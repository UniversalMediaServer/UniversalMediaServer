package net.pms.store;

import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * What a continuous stream is playing right now. Where this comes from differs per stream type.
 */
public class NowPlayingInfo {

	/** Set when the source separates the two; NULL for a source that only knows one string. */
	public final String artist;
	public final String title;
	public final String artUrl;
	/** The unsplit line, as ICY carries it. Always set. */
	public final String streamTitle;

	private NowPlayingInfo(String artist, String title, String artUrl, String streamTitle) {
		this.artist = artist;
		this.title = title;
		this.artUrl = artUrl;
		this.streamTitle = streamTitle;
	}

	/**
	 * For a source that only knows a single unstructured line, such as ICY.
	 */
	public static NowPlayingInfo ofStreamTitle(String streamTitle) {
		return StringUtils.isBlank(streamTitle) ? null : new NowPlayingInfo(null, null, null, streamTitle);
	}

	/**
	 * For a source that knows the parts, such as the AudioAddict API.
	 */
	public static NowPlayingInfo of(String artist, String title, String artUrl) {
		if (StringUtils.isAllBlank(artist, title)) {
			return null;
		}
		String line = StringUtils.isNotBlank(artist) && StringUtils.isNotBlank(title) ? artist + " - " + title
			: StringUtils.defaultIfBlank(title, artist);
		return new NowPlayingInfo(StringUtils.trimToNull(artist), StringUtils.trimToNull(title),
			StringUtils.trimToNull(artUrl), line);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof NowPlayingInfo o)) {
			return false;
		}
		return Objects.equals(artist, o.artist) && Objects.equals(title, o.title) &&
			Objects.equals(artUrl, o.artUrl) && Objects.equals(streamTitle, o.streamTitle);
	}

	@Override
	public int hashCode() {
		return Objects.hash(artist, title, artUrl, streamTitle);
	}

	@Override
	public String toString() {
		return "NowPlayingInfo [" + streamTitle + (artUrl != null ? ", art=" + artUrl : "") + "]";
	}
}
