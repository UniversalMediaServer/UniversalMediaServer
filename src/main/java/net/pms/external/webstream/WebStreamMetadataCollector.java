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
package net.pms.external.webstream;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableWebResource;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.media.MediaInfo;
import net.pms.media.WebStreamMetadata;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.SimpleThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStreamMetadataCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStreamMetadataCollector.class.getName());

	// Minimum number of threads in pool
	private static final ThreadPoolExecutor BACKGROUND_EXECUTOR = new ThreadPoolExecutor(
			0,
			5, // Maximum number of threads in pool
			30, TimeUnit.SECONDS, // Number of seconds before an idle thread is terminated
			// The queue holding the tasks waiting to be processed
			new LinkedBlockingQueue<>(),
			// The ThreadFactory
			new SimpleThreadFactory("Lookup WebStream Metadata background worker", "Lookup WebStream Metadata background workers group", Thread.NORM_PRIORITY - 1)
	);

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebStreamMetadataCollector() {
	}

	private static boolean shouldLookupAndAddMetadata() {
		if (BACKGROUND_EXECUTOR.isShutdown()) {
			LOGGER.trace("Not doing background WebStream lookup because background executor is shutdown");
			return false;
		}

		if (!MediaDatabase.isAvailable()) {
			LOGGER.trace("Not doing background WebStream lookup because database is closed");
			return false;
		}

		return true;
	}

	/**
	 * Enhances existing MediaInfo attached to this WebStream.
	 *
	 * @param uri WebStream uri
	 * @param mediaInfo MediaInfo
	 */
	public static void backgroundLookupAndAddMetadata(final String uri, final Map<String, String> directives, final MediaInfo mediaInfo) {
		if (!shouldLookupAndAddMetadata()) {
			return;
		}
		//do not try a lookup if already queued on last 5 minutes
		long elapsed = System.currentTimeMillis() - mediaInfo.getLastExternalLookup();
		if (elapsed < 300000) {
			return;
		}
		mediaInfo.setLastExternalLookup(System.currentTimeMillis());
		Runnable r = () -> {
			//get the default
			WebStreamMetadata webStreamMetadata = WebStreamParser.getWebStreamMetadata(uri);
			if (webStreamMetadata != null && directives.containsKey("RADIOBROWSERUUID")) {
				WebStreamMetadata radioStreamMetadata = RadioBrowser4j.getWebStreamMetadata(uri, directives.get("RADIOBROWSERUUID"));
				if (radioStreamMetadata != null) {
					if (radioStreamMetadata.getGenre() != null) {
						webStreamMetadata.setGenre(radioStreamMetadata.getGenre());
					}
					if (StringUtils.isBlank(radioStreamMetadata.getLogoUrl())) {
						DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(radioStreamMetadata.getLogoUrl());
						if (thumbnail != null) {
							Long thumbnailId = ThumbnailStore.getIdForWebStream(thumbnail, uri, ThumbnailSource.RADIOBROWSER);
							webStreamMetadata.setThumbnailSource(ThumbnailSource.RADIOBROWSER);
							webStreamMetadata.setThumbnailId(thumbnailId);
						}
						webStreamMetadata.setLogoUrl(radioStreamMetadata.getLogoUrl());
					}
					if (radioStreamMetadata.getContentType() != null) {
						webStreamMetadata.setContentType(radioStreamMetadata.getContentType());
					}
					if (radioStreamMetadata.getBitrate() != null) {
						webStreamMetadata.setBitrate(radioStreamMetadata.getBitrate());
					}
				}
			}
			if (webStreamMetadata != null) {
				if (webStreamMetadata.getThumbnailId() == null && !StringUtils.isBlank(webStreamMetadata.getLogoUrl())) {
					DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(webStreamMetadata.getLogoUrl());
					if (thumbnail != null) {
						Long thumbnailId = ThumbnailStore.getIdForWebStream(thumbnail, uri, ThumbnailSource.WEBSTREAM);
						webStreamMetadata.setThumbnailSource(ThumbnailSource.WEBSTREAM);
						webStreamMetadata.setThumbnailId(thumbnailId);
					}
				}
				MediaTableWebResource.insertOrUpdateWebResource(webStreamMetadata);
				webStreamMetadata.fillMediaInfo(mediaInfo);
			}
		};
		LOGGER.trace("Queuing background WebStream lookup for {}", uri);
		BACKGROUND_EXECUTOR.execute(r);
	}

}
