package net.pms.store.container.audioaddict;

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

	private final Platform network;
	private final int playlistId;
	private final boolean loop;

	private final Deque<AudioAddictTrackDto> buffer = new ArrayDeque<>();
	private final Set<Long> servedIds = new HashSet<>();
	private InputStream current;
	private AudioAddictTrackDto currentTrack;
	private boolean servedThisPass;
	private boolean finished;

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
			current = URI.create(next.contentUrl).toURL().openStream();
			currentTrack = next;
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
						servedIds.clear();
						servedThisPass = false;
						emptyWindows = 0;
						continue;
					}
					return null;
				}
			}
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
			AudioAddictService.get().markPlaylistTrackPlayed(network, playlistId, currentTrack.id);
		}
		currentTrack = null;
	}

	@Override
	public void close() throws IOException {
		closeCurrent(false);
		buffer.clear();
		finished = true;
	}
}
