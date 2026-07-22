package net.pms.store.container.audioaddict;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictPlayWindow;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;

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
					return n;
				}
				// Current track finished: close it and advance the server session.
				closeCurrent(true);
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
		AudioAddictTrackDto track = currentTrack;
		if (track == null) {
			return null;
		}
		if (track.artist != null && !track.artist.isBlank()) {
			return track.artist + " - " + track.title;
		}
		return track.title;
	}

	/**
	 * @return the track currently playing for the given playlist id, or {@code null} if that
	 * playlist is not being streamed right now. Used by the UmsExtendedServices UPnP action so a
	 * control point can display the live playlist track.
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
			BufferedInputStream in = new BufferedInputStream(URI.create(next.contentUrl).toURL().openStream(), 64 * 1024);
			currentTrack = next;
			CURRENT_TRACKS.put(playlistId, next);
			trackNumber++;
			// Strip the original ID3v2 tag (it carries large cover art that desyncs strict decoders at
			// concatenated track boundaries) and prepend a tiny synthetic one.
			stripId3v2(in);
			byte[] id3 = buildId3v2(next.artist, next.title);
			current = new SequenceInputStream(new ByteArrayInputStream(id3), in);
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
	 */
	private void stripId3v2(BufferedInputStream in) throws IOException {
		in.mark(16);
		byte[] header = in.readNBytes(10);
		if (header.length == 10 && header[0] == 'I' && header[1] == 'D' && header[2] == '3') {
			int size = ((header[6] & 0x7F) << 21) | ((header[7] & 0x7F) << 14) | ((header[8] & 0x7F) << 7) | (header[9] & 0x7F);
			in.skipNBytes(size);
			LOGGER.debug("{} : playlist {} - track #{} stripped ID3v2 ({} bytes)", network.displayName, playlistId, trackNumber, size);
		} else {
			in.reset();
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

	@Override
	public void close() throws IOException {
		LOGGER.debug("{} : playlist {} - stream closed by consumer after {} tracks", network.displayName, playlistId, trackNumber);
		closeCurrent(false);
		buffer.clear();
		finished = true;
	}
}
