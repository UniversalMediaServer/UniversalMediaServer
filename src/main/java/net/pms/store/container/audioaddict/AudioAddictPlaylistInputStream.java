package net.pms.store.container.audioaddict;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictPlayWindow;
import net.pms.external.audioaddict.AudioAddictService;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.external.audioaddict.Platform;

/**
 * Continuous audio stream of a curated playlist. It walks the playlist play session
 * (play window -&gt; stream track -&gt; mark played -&gt; next window) and concatenates the track
 * MP3s into a single stream. When {@code loop} is set it restarts a fresh session after the
 * last track and plays forever; otherwise the stream ends after the last track.
 */
public class AudioAddictPlaylistInputStream extends InputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictPlaylistInputStream.class.getName());
	// Guard against tight loops when the API keeps returning only already served tracks.
	private static final int MAX_EMPTY_WINDOWS = 3;
	// TEMP diagnostics: MP3 frame-header lookup tables.
	private static final int[] MP3_SAMPLE_RATE_MPEG1 = {44100, 48000, 32000, 0};
	private static final int[] MP3_SAMPLE_RATE_MPEG2 = {22050, 24000, 16000, 0};
	private static final int[] MP3_SAMPLE_RATE_MPEG25 = {11025, 12000, 8000, 0};
	private static final int[] MP3_BITRATE_MPEG1_L3 = {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0};

	private final Platform network;
	private final int playlistId;
	private final boolean loop;

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
	 * @return the currently playing track as "Artist - Title" for ICY metadata, or {@code null}
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

	private boolean openNextTrack() {
		AudioAddictTrackDto next = nextTrack();
		if (next == null) {
			finished = true;
			return false;
		}
		try {
			BufferedInputStream in = new BufferedInputStream(URI.create(next.contentUrl).toURL().openStream(), 512 * 1024);
			currentTrack = next;
			trackNumber++;
			LOGGER.debug("{} : playlist {} - playing track #{} (id={}): {} - {}", network.displayName, playlistId, trackNumber,
				next.id, next.artist, next.title);
			logMp3Diagnostics(in, next);
			current = in;
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
						// Replay from the start. AudioAddict tracks progress per member, so this
						// only keeps looping while the server still returns not-yet-served tracks.
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
	 * TEMP diagnostics: non-destructively peek (mark/reset) at the start of the track and log
	 * whether it carries an ID3v2 tag (size) and the first MP3 frame header (version/layer/sample
	 * rate/bitrate). Confirms tag cruft at the boundaries and whether the sample rate is uniform.
	 */
	private void logMp3Diagnostics(BufferedInputStream in, AudioAddictTrackDto t) {
		try {
			in.mark(400_000);
			byte[] b = in.readNBytes(262_144);
			in.reset();
			boolean hasId3 = b.length >= 10 && b[0] == 'I' && b[1] == 'D' && b[2] == '3';
			int id3Size = 0;
			int scanFrom = 0;
			if (hasId3) {
				id3Size = ((b[6] & 0x7F) << 21) | ((b[7] & 0x7F) << 14) | ((b[8] & 0x7F) << 7) | (b[9] & 0x7F);
				scanFrom = 10 + id3Size;
			}
			int f = -1;
			for (int i = Math.max(0, scanFrom); i + 3 < b.length; i++) {
				if ((b[i] & 0xFF) == 0xFF && (b[i + 1] & 0xE0) == 0xE0) {
					f = i;
					break;
				}
			}
			if (f < 0) {
				LOGGER.info("MP3-DIAG: track #{} (id={}) ID3v2={} (size={}), no frame header within {} peeked bytes",
					trackNumber, t.id, hasId3, id3Size, b.length);
				return;
			}
			int h1 = b[f + 1] & 0xFF;
			int h2 = b[f + 2] & 0xFF;
			int h3 = b[f + 3] & 0xFF;
			int ver = (h1 >> 3) & 0x3;
			int layer = (h1 >> 1) & 0x3;
			int brIdx = (h2 >> 4) & 0xF;
			int srIdx = (h2 >> 2) & 0x3;
			int chMode = (h3 >> 6) & 0x3;
			int sr = ver == 3 ? MP3_SAMPLE_RATE_MPEG1[srIdx] : ver == 2 ? MP3_SAMPLE_RATE_MPEG2[srIdx] : MP3_SAMPLE_RATE_MPEG25[srIdx];
			int br = (ver == 3 && layer == 1) ? MP3_BITRATE_MPEG1_L3[brIdx] : -1;
			String verStr = ver == 3 ? "MPEG1" : ver == 2 ? "MPEG2" : ver == 0 ? "MPEG2.5" : "reserved";
			LOGGER.info("MP3-DIAG: track #{} (id={}) ID3v2={} (size={}), {} layer-bits={} (1=III) {}Hz {}kbps chMode={} (3=mono) frameAt={}",
				trackNumber, t.id, hasId3, id3Size, verStr, layer, sr, br, chMode, f);
		} catch (IOException e) {
			LOGGER.info("MP3-DIAG: track #{} (id={}) peek failed: {}", trackNumber, t.id, e.toString());
		}
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
