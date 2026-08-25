/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.store.item;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.media.MediaInfo;
import net.pms.external.radiobrowser.RadioBrowser4j;
import net.pms.network.HTTPResourceAuthenticator;
import net.pms.renderers.Renderer;
import net.pms.store.MediaInfoStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreItem;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.store.container.PlaylistFolder;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStream extends StoreItem {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStream.class);

	// Probing a stream is an HTTP round trip, so a browse hands it over here instead of waiting for it.
	private static final ExecutorService RESOLVE_EXECUTOR = Executors.newFixedThreadPool(4, r -> {
		Thread thread = new Thread(r, "web-stream-resolve");
		thread.setDaemon(true);
		return thread;
	});

	// Urls with a probe in flight, so the same stream is not probed by several browses at once.
	private static final Set<String> RESOLVING = ConcurrentHashMap.newKeySet();

	private String url;
	private String fluxName;
	private String thumbURL;
	private final Map<String, String> directives;
	private volatile String lastStreamError;

	public WebStream(Renderer renderer, String fluxName, String url, String thumbURL, int type, Map<String, String> directives) {
		super(renderer, type);

		if (url != null) {
			try {
				URL tmpUrl = URI.create(url).toURL();
				tmpUrl = HTTPResourceAuthenticator.concatenateUserInfo(tmpUrl);
				this.url = tmpUrl.toString();
			} catch (IllegalArgumentException | MalformedURLException e) {
				this.url = url;
			}
		}

		if (thumbURL != null) {
			try {
				URL tmpUrl = URI.create(thumbURL).toURL();
				tmpUrl = HTTPResourceAuthenticator.concatenateUserInfo(tmpUrl);
				this.thumbURL = tmpUrl.toString();
			} catch (IllegalArgumentException | MalformedURLException e) {
				this.thumbURL = thumbURL;
			}
		}

		this.fluxName = fluxName;
		this.directives = directives;
	}

	@Override
	public String write() {
		return fluxName + ">" + url + ">" + thumbURL + ">" + getSpecificType();
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		DLNAThumbnailInputStream result = null;
		if (getMediaInfo() != null && getMediaInfo().getThumbnail() != null) {
			result = getMediaInfo().getThumbnailInputStream();
		}
		if (result == null && thumbURL != null) {
			result = FileUtil.isUrl(thumbURL) ? ThumbnailStore.getThumbnailInputStreamForUrl(thumbURL) : ThumbnailStore.getThumbnailInputStreamForFile(new File(thumbURL));
		}
		return result != null ? result : super.getThumbnailInputStream();
	}

	@Override
	public InputStream getInputStream() {
		try {
			InputStream input = new URL(url).openStream();
			lastStreamError = null;
			return input;
		} catch (IOException e) {
			LOGGER.error("cannot read input stream from {}", url, e);
			lastStreamError = e.getMessage();
		}
		return null;
	}

	public String getLastStreamError() {
		return lastStreamError;
	}

	/**
	 * A radio stream has neither a size nor a duration, a hosted file has both.
	 */
	@Override
	public boolean isUnboundedLiveStream() {
		MediaInfo media = getMediaInfo();
		if (media == null || !media.isMediaParsed()) {
			return true;
		}
		return media.getSize() <= 0 && media.getDurationInSeconds() <= 0;
	}

	@Override
	public boolean isValid() {
		resolveFormat();
		return getFormat() != null;
	}

	@Override
	public long length() {
		return TRANS_SIZE;
	}

	@Override
	public String getName() {
		return getFluxName();
	}

	@Override
	public String getSystemName() {
		return getUrl();
	}

	/**
	 * @return the url
	 * @since 1.50
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 * @since 1.50
	 */
	protected void setUrl(String url) {
		this.url = url;
	}

	/**
	 * @return the fluxName
	 * @since 1.50
	 */
	protected String getFluxName() {
		return fluxName;
	}

	/**
	 * @param fluxName the fluxName to set
	 * @since 1.50
	 */
	protected void setFluxName(String fluxName) {
		this.fluxName = fluxName;
	}

	/**
	 * @return the thumbURL
	 * @since 1.50
	 */
	protected String getThumbURL() {
		return thumbURL;
	}

	/**
	 * @param thumbURL the thumbURL to set
	 * @since 1.50
	 */
	protected void setThumbURL(String thumbURL) {
		this.thumbURL = thumbURL;
	}

	@Override
	public boolean isSubSelectable() {
		return true;
	}

	/**
	 * Puts the stream away from the request that asked for it.
	 */
	public void resolveInBackground() {
		if (!RESOLVING.add(url)) {
			return;
		}
		RESOLVE_EXECUTOR.execute(() -> {
			try {
				setMediaInfo(MediaInfoStore.getWebStreamMediaInfo(url, getSpecificType()));
				MediaStoreIds.incrementUpdateId(getLongId());
			} catch (Exception e) {
				LOGGER.debug("Could not resolve the web stream \"{}\": {}", url, e.getMessage());
				LOGGER.trace("", e);
			} finally {
				RESOLVING.remove(url);
			}
		});
	}

	@Override
	public synchronized void resolve() {
		if (url == null) {
			LOGGER.error("WebStream points to a null url.");
			return;
		}

		if (getMediaInfo() == null || !getMediaInfo().isMediaParsed()) {
			setMediaInfo(MediaInfoStore.getWebStreamMediaInfo(url, getSpecificType()));
		}
		if (directives != null && directives.containsKey(PlaylistFolder.DIRECTIVE_RADIOBROWSERUUID)) {
			// Attempt to enhance the metadata via RADIOBROWSER API.
			RadioBrowser4j.backgroundLookupAndAddMetadata(url, directives.get(PlaylistFolder.DIRECTIVE_RADIOBROWSERUUID), mediaInfo);
		}
		if (directives != null && directives.containsKey(PlaylistFolder.DIRECTIVE_ALBUMART_URI)) {
			ThumbnailStore.enqueueThumbnailUpdate(directives.get(PlaylistFolder.DIRECTIVE_ALBUMART_URI), getFileName(), ThumbnailSource.PLAYLIST);
		}
		applyRatingDirective();
	}

	/**
	 * A web stream has no file to hold its rating.
	 */
	private void applyRatingDirective() {
		if (directives == null || !directives.containsKey(PlaylistFolder.DIRECTIVE_RATING) || getRating() != null) {
			return;
		}
		String value = directives.get(PlaylistFolder.DIRECTIVE_RATING).trim();
		try {
			setRating(Integer.valueOf(value));
		} catch (NumberFormatException e) {
			LOGGER.debug("Ignoring the rating directive \"{}\" of {} because it is not a number", value, url);
		}
	}

}
