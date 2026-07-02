package net.pms.store.container.audioaddict;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.audioaddict.AudioAddictTrackDto;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.item.WebAudioStream;
import net.pms.util.ByteRange;
import net.pms.util.Range;

/**
 * A seekable AudioAddict media file serves as a normal. UMS reports the real length and
 * forwards Range requests to the origin, so the renderer can seek/probe and play it through to the
 * end and read the embedded ID3 title and cover art itself.
 * <p>
 * If the length cannot be determined, it falls back to the radio-style serving so playback still
 * works without ranges.
 */
public class AudioAddictFileStream extends WebAudioStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioAddictFileStream.class);
	private static final int TIMEOUT_MS = 15000;

	private volatile long cachedLength = -1;

	public AudioAddictFileStream(Renderer renderer, String fluxName, String url, String thumbURL) {
		super(renderer, fluxName, url, thumbURL);
	}

	/**
	 * Builds a playable file item from an AudioAddict track DTO, prefixing the title with the
	 * air-time label when present.
	 */
	public static AudioAddictFileStream from(Renderer renderer, AudioAddictTrackDto track) {
		MediaInfo mi = new MediaInfo();
		mi.setMimeType("audio/mpeg");
		mi.setMediaParser("STATIC");
		if (track.artist != null || track.genres != null || track.album != null) {
			MediaAudioMetadata md = new MediaAudioMetadata();
			if (track.artist != null) {
				md.setArtist(track.artist);
			}
			if (track.genres != null) {
				md.setGenre(track.genres);
			}
			if (track.album != null) {
				md.setAlbum(track.album);
			}
			mi.setAudioMetadata(md);
		}

		String title = track.startLabel != null ? (track.startLabel + "  " + track.title) : track.title;
		AudioAddictFileStream sr = new AudioAddictFileStream(renderer, title, track.contentUrl, track.albumArt);
		sr.setMediaInfo(mi);
		return sr;
	}

	@Override
	public long length() {
		if (cachedLength == -1) {
			synchronized (this) {
				if (cachedLength == -1) {
					long probed = probeLength();
					cachedLength = probed > 0 ? probed : TRANS_SIZE;
				}
			}
		}
		return cachedLength;
	}

	/**
	 * The event is a real file. We want to probe when actually streaming. It therefore returns false
	 * until a probe has actually run and failed (cachedLength == TRANS_SIZE), in which case we fall
	 * back to radio-style serving so playback still works without ranges.
	 */
	@Override
	public boolean isUnboundedLiveStream() {
		return cachedLength == TRANS_SIZE;
	}

	/**
	 * Opens the origin stream, forwarding the requested byte range so the renderer can seek without
	 * UMS having to download-and-discard behaviour).
	 */
	@Override
	public InputStream getInputStream(Range range) throws IOException {
		long low = (range instanceof ByteRange byteRange) ? byteRange.getStartOrZero() : 0;
		long high = (range instanceof ByteRange byteRange && range.isEndLimitAvailable()) ? (long) byteRange.getEnd() : -1;
		HttpURLConnection connection = (HttpURLConnection) URI.create(getUrl()).toURL().openConnection();
		connection.setConnectTimeout(TIMEOUT_MS);
		connection.setReadTimeout(TIMEOUT_MS);
		if (low > 0 || high >= 0) {
			connection.setRequestProperty("Range", "bytes=" + low + "-" + (high >= 0 ? high : ""));
		}
		LOGGER.debug("AudioAddict event '{}' : opening origin range bytes={}-{}", getName(), low, high >= 0 ? high : "");
		return connection.getInputStream();
	}

	private long probeLength() {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) URI.create(getUrl()).toURL().openConnection();
			connection.setRequestProperty("Range", "bytes=0-1");
			connection.setConnectTimeout(TIMEOUT_MS);
			connection.setReadTimeout(TIMEOUT_MS);
			int code = connection.getResponseCode();
			long total = -1;
			if (code == HttpURLConnection.HTTP_PARTIAL) {
				String contentRange = connection.getHeaderField("Content-Range"); // e.g. "bytes 0-1/123456"
				int slash = contentRange != null ? contentRange.indexOf('/') : -1;
				if (slash >= 0) {
					String totalStr = contentRange.substring(slash + 1).trim();
					if (!"*".equals(totalStr)) {
						total = Long.parseLong(totalStr);
					}
				}
			} else if (code == HttpURLConnection.HTTP_OK) {
				total = connection.getContentLengthLong();
			}
			LOGGER.debug("AudioAddict event '{}' : probed length = {} bytes (HTTP {})", getName(), total, code);
			return total;
		} catch (IOException | NumberFormatException e) {
			LOGGER.warn("AudioAddict event '{}' : length probe failed, falling back to unbounded serving", getName(), e);
			return -1;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
