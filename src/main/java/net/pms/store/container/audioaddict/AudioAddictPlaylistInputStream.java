package net.pms.store.container.audioaddict;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictPlayWindow;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;
import net.pms.store.NowPlayingInfo;

/**
 * Continuous audio stream of a curated playlist. It walks the playlist play session and concatenates the track
 * MP3s into a single stream. When "loop" is set it restarts a fresh session after the
 * last track and plays forever; otherwise the stream ends after the last track.
 */
public class AudioAddictPlaylistInputStream extends InputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictPlaylistInputStream.class.getName());

	private static final int MAX_EMPTY_WINDOWS = 3;

	private final Platform network;
	private final int playlistId;
	private final boolean loop;

	// Current track per playlist id, so a control point can look up "now playing" for a playlist it
	// is not itself streaming. Last-writer-wins if the same playlist is streamed by several clients.
	private static final Map<Integer, AudioAddictTrackDto> CURRENT_TRACKS = new ConcurrentHashMap<>();

	private final Deque<AudioAddictTrackDto> buffer = new ArrayDeque<>();
	private final Set<Long> servedIds = new HashSet<>();
	private InputStream current;
	private AudioAddictTrackDto currentTrack;
	private boolean servedThisPass;
	private boolean finished;
	private int trackNumber;

	/*
	 * What is audible follows the clock, not the byte stream: the tracks are handed out as fast as the
	 * consumer takes them, but they are listened to one after the other, starting with the first byte.
	 * Every track is therefore filed with the playing time at which it begins, and the announcement
	 * picks the one that time has reached.
	 */
	private final Deque<ScheduledTrack> schedule = new ArrayDeque<>();
	private long streamStartedAt;
	private long scheduledMs;
	private AudioAddictTrackDto announcedTrack;

	/*
	 * The service is told every minute that this stream is still being listened to, the same way the
	 * official player does it. The token out of the content url identifies the stream; without the
	 * ping our playback is invisible to the side that counts who is listening.
	 */
	private static final long STREAMING_PING_SECONDS = 60;
	private static final ScheduledExecutorService PING_SERVICE = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread thread = new Thread(r, "audioaddict-streaming-ping");
		thread.setDaemon(true);
		return thread;
	});

	private volatile String audioToken;
	private ScheduledFuture<?> ping;

	/*
	 * The bytes go out at the speed they are listened to.
	 */
	private static final long BURST_MS = 4000;
	private static final long MAX_SLEEP_MS = 250;
	// 320 kbit/s, the premium quality, for a track that does not say how long it is.
	private static final int FALLBACK_BYTES_PER_SECOND = 40000;

	private long pacingStartedAt;
	private double deliveredMs;
	private double msPerByte;

	public AudioAddictPlaylistInputStream(Platform network, int playlistId, boolean loop) {
		this.network = network;
		this.playlistId = playlistId;
		this.loop = loop;
	}

	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		int n = read(one, 0, 1);
		return n == -1 ? -1 : (one[0] & 0xFF);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		while (true) {
			if (current != null) {
				int n = current.read(b, off, len);
				if (n != -1) {
					pace(n);
					return n;
				}
				closeCurrent(false);
			}
			if (finished || !openNextTrack()) {
				return -1;
			}
		}
	}

	/**
	 * @return the currently playing track as "Artist - Title" for ICY metadata, or NULL
	 * when no track is currently open (treated as "unchanged" by the metadata layer).
	 */
	public String getStreamTitle() {
		NowPlayingInfo info = getNowPlaying();
		return info == null ? null : info.streamTitle;
	}

	/**
	 * @return the track this very stream instance is playing, by the clock.
	 */
	public NowPlayingInfo getNowPlaying() {
		AudioAddictTrackDto track = audibleTrack();
		return track == null ? null : NowPlayingInfo.of(track.artist, track.title, track.albumArt);
	}

	/**
	 * Files a track with the playing time it starts at. An unknown length makes it start where the
	 * previous one did, so the next track takes over at once instead of the schedule standing still.
	 */
	private synchronized void scheduleTrack(AudioAddictTrackDto track) {
		if (streamStartedAt == 0) {
			streamStartedAt = System.currentTimeMillis();
		}
		schedule.addLast(new ScheduledTrack(track, scheduledMs));
		scheduledMs += Math.max(0, track.length) * 1000L;
	}

	/**
	 * @return the filed track the playing time has reached, NULL before the first byte went out
	 */
	private synchronized AudioAddictTrackDto audibleTrack() {
		if (streamStartedAt == 0 || schedule.isEmpty()) {
			return announcedTrack;
		}
		long elapsed = System.currentTimeMillis() - streamStartedAt;
		// What the clock has left behind has been listened to, so that is when the service hears of it.
		while (schedule.size() > 1) {
			ScheduledTrack next = schedule.stream().skip(1).findFirst().orElse(null);
			if (next == null || next.fromMs() > elapsed) {
				break;
			}
			markPlayed(schedule.removeFirst().track());
		}
		AudioAddictTrackDto audible = schedule.getFirst().track();
		if (audible != announcedTrack) {
			announcedTrack = audible;
			CURRENT_TRACKS.put(playlistId, audible);
			LOGGER.debug("{} : playlist {} - now audible: {} - {}", network.displayName, playlistId, audible.artist,
				audible.title);
		}
		return announcedTrack;
	}

	/**
	 * A track and the playing time of the stream at which it begins.
	 */
	private record ScheduledTrack(AudioAddictTrackDto track, long fromMs) {
	}

	/**
	 * @return the track currently playing for the given playlist id.
	 */
	public static AudioAddictTrackDto getCurrentTrack(int playlistId) {
		return CURRENT_TRACKS.get(playlistId);
	}

	private boolean openNextTrack() {
		AudioAddictTrackDto next = nextTrack();
		if (next == null) {
			finished = true;
			return false;
		}
		try {
			URLConnection connection = URI.create(next.contentUrl).toURL().openConnection();
			BufferedInputStream in = new BufferedInputStream(connection.getInputStream(), 64 * 1024);
			currentTrack = next;
			scheduleTrack(next);
			startStreamingPing(next.contentUrl);
			trackNumber++;
			// Strip the original ID3v2 tag (it carries large cover art that desyncs strict decoders at
			// concatenated track boundaries) and prepend a tiny synthetic one.
			int stripped = stripId3v2(in);
			byte[] id3 = buildId3v2(next.artist, next.title);
			current = new SequenceInputStream(new ByteArrayInputStream(id3), in);
			setPace(next, connection.getContentLengthLong() - stripped + id3.length);
			LOGGER.debug("{} : playlist {} - playing track #{} (id={}): {} - {} (prepended {} byte ID3v2)", network.displayName,
				playlistId, trackNumber, next.id, next.artist, next.title, id3.length);
			return true;
		} catch (IOException e) {
			LOGGER.warn("{} : cannot open playlist track {}", network.displayName, next.id, e);
			// Skip the broken track and try the next one.
			return openNextTrack();
		}
	}

	private AudioAddictTrackDto nextTrack() {
		int emptyWindows = 0;
		while (true) {
			while (!buffer.isEmpty()) {
				AudioAddictTrackDto t = buffer.poll();
				if (t != null && servedIds.add(t.id)) {
					servedThisPass = true;
					return t;
				}
			}
			AudioAddictPlayWindow window = AudioAddictService.get().playPlaylist(network, playlistId);
			LOGGER.debug("{} : playlist {} - fetched window: {} tracks, remaining={}, lastTracks={}", network.displayName,
				playlistId, window.tracks.size(), window.remainingTracks, window.lastTracks);
			boolean added = false;
			for (AudioAddictTrackDto t : window.tracks) {
				if (!servedIds.contains(t.id)) {
					buffer.add(t);
					added = true;
				}
			}
			if (!added) {
				emptyWindows++;
				boolean ended = window.lastTracks || window.remainingTracks <= 0 || emptyWindows >= MAX_EMPTY_WINDOWS;
				if (ended) {
					if (loop && servedThisPass) {
						LOGGER.debug("{} : playlist {} - looping, replaying from start after {} tracks", network.displayName,
							playlistId, trackNumber);
						servedIds.clear();
						servedThisPass = false;
						emptyWindows = 0;
						continue;
					}
					LOGGER.debug("{} : playlist {} - no more tracks, ending after {} tracks (lastTracks={}, remaining={})",
						network.displayName, playlistId, trackNumber, window.lastTracks, window.remainingTracks);
					return null;
				}
			}
		}
	}

	/**
	 * Skips a leading ID3v2 tag so the rest of the stream begins at the first MP3 audio frame.
	 *
	 * @return how many bytes of tag were skipped, so they do not count as playing time
	 */
	private int stripId3v2(BufferedInputStream in) throws IOException {
		in.mark(16);
		byte[] header = in.readNBytes(10);
		if (header.length == 10 && header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
			int size = ((header[6] & 0x7F) << 21) | ((header[7] & 0x7F) << 14) | ((header[8] & 0x7F) << 7) | (header[9] & 0x7F);
			in.skipNBytes(size);
			LOGGER.debug("{} : playlist {} - track #{} stripped ID3v2 ({} bytes)", network.displayName, playlistId, trackNumber, size);
			return size + 10;
		}
		in.reset();
		return 0;
	}

	private void setPace(AudioAddictTrackDto track, long audioBytes) {
		msPerByte = track.length > 0 && audioBytes > 0 ?
			track.length * 1000.0 / audioBytes :
			1000.0 / FALLBACK_BYTES_PER_SECOND;
		LOGGER.debug("{} : playlist {} - track #{} paced at {} kbit/s ({} s, {} bytes)", network.displayName, playlistId,
			trackNumber, Math.round(8 / msPerByte), track.length, audioBytes);
	}

	/**
	 * Holds the stream back to the speed of listening.
	 */
	private void pace(int bytes) {
		if (pacingStartedAt == 0) {
			pacingStartedAt = System.currentTimeMillis();
		}
		deliveredMs += bytes * msPerByte;
		long due = pacingStartedAt + (long) deliveredMs - BURST_MS;
		long wait;
		while ((wait = due - System.currentTimeMillis()) > 0) {
			try {
				Thread.sleep(Math.min(wait, MAX_SLEEP_MS));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/**
	 * Builds a minimal ID3v2.4 tag holding just the track title "TIT2" and, when present,
	 * the artist "TPE1", UTF-8 encoded and without any cover art.
	 */
	private static byte[] buildId3v2(String artist, String title) {
		byte[] titleFrame = textFrame("TIT2", title);
		byte[] artistFrame = (artist != null && !artist.isBlank()) ? textFrame("TPE1", artist) : new byte[0];
		int bodySize = titleFrame.length + artistFrame.length;
		byte[] tag = new byte[10 + bodySize];
		tag[0] = 'I';
		tag[1] = 'D';
		tag[2] = '3';
		tag[3] = 0x04; // version 2.4.0
		tag[4] = 0x00; // revision
		tag[5] = 0x00; // flags
		writeSyncsafe(tag, 6, bodySize);
		System.arraycopy(titleFrame, 0, tag, 10, titleFrame.length);
		System.arraycopy(artistFrame, 0, tag, 10 + titleFrame.length, artistFrame.length);
		return tag;
	}

	/**
	 * Builds a single ID3v2.4 text information frame: 4-char id, syncsafe size, no flags, then a
	 * UTF-8 encoding byte ({@code 0x03}) followed by the UTF-8 text.
	 */
	private static byte[] textFrame(String id, String text) {
		byte[] textBytes = (text != null ? text : "").getBytes(StandardCharsets.UTF_8);
		int bodySize = 1 + textBytes.length; // encoding byte + text
		byte[] frame = new byte[10 + bodySize];
		frame[0] = (byte) id.charAt(0);
		frame[1] = (byte) id.charAt(1);
		frame[2] = (byte) id.charAt(2);
		frame[3] = (byte) id.charAt(3);
		writeSyncsafe(frame, 4, bodySize);
		frame[8] = 0x00; // flags
		frame[9] = 0x00;
		frame[10] = 0x03; // text encoding: UTF-8
		System.arraycopy(textBytes, 0, frame, 11, textBytes.length);
		return frame;
	}

	/**
	 * Writes a 28-bit syncsafe integer (7 bits per byte) into 4 bytes starting at {@code offset}.
	 */
	private static void writeSyncsafe(byte[] buf, int offset, int value) {
		buf[offset] = (byte) ((value >> 21) & 0x7F);
		buf[offset + 1] = (byte) ((value >> 14) & 0x7F);
		buf[offset + 2] = (byte) ((value >> 7) & 0x7F);
		buf[offset + 3] = (byte) (value & 0x7F);
	}

	/**
	 * Reports a track as played, the way the official player does it.
	 */
	private void markPlayed(AudioAddictTrackDto track) {
		if (track == null) {
			return;
		}
		LOGGER.debug("{} : playlist {} - listened through {} - {} (id={}), marking played", network.displayName, playlistId,
			track.artist, track.title, track.id);
		AudioAddictService.get().markPlaylistTrackPlayed(network, playlistId, track.id);
	}

	private void closeCurrent(boolean markPlayed) {
		if (current != null) {
			try {
				current.close();
			} catch (IOException e) {
				// ignore
			}
			current = null;
		}
		if (markPlayed && currentTrack != null) {
			LOGGER.debug("{} : playlist {} - finished track #{} (id={}), marking played", network.displayName, playlistId,
				trackNumber, currentTrack.id);
			AudioAddictService.get().markPlaylistTrackPlayed(network, playlistId, currentTrack.id);
		}
		currentTrack = null;
	}

	/**
	 * Reports this stream to the service once a minute, for as long as it is open. The token changes
	 * with every track, the schedule does not.
	 */
	private synchronized void startStreamingPing(String contentUrl) {
		audioToken = audioTokenOf(contentUrl);
		if (ping != null || audioToken == null) {
			return;
		}
		ping = PING_SERVICE.scheduleAtFixedRate(() -> {
			String token = audioToken;
			if (token != null) {
				AudioAddictService.get().pingStreaming(network, token);
			}
			audibleTrack();
		}, STREAMING_PING_SECONDS, STREAMING_PING_SECONDS, TimeUnit.SECONDS);
	}

	private synchronized void stopStreamingPing() {
		if (ping != null) {
			ping.cancel(false);
			ping = null;
		}
		audioToken = null;
	}

	/**
	 * @return the "audio_token" of a content url, NULL when it carries none
	 */
	private static String audioTokenOf(String contentUrl) {
		if (contentUrl == null) {
			return null;
		}
		String token = StringUtils.substringAfter(contentUrl, "audio_token=");
		token = StringUtils.substringBefore(token, "&");
		return StringUtils.trimToNull(token);
	}

	@Override
	public void close() throws IOException {
		LOGGER.debug("{} : playlist {} - stream closed by consumer after {} tracks", network.displayName, playlistId, trackNumber);
		stopStreamingPing();
		closeCurrent(false);
		buffer.clear();
		finished = true;
	}
}
