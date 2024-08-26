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
package net.pms.external.radiobrowser;

import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.EndpointDiscovery;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.Station;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.store.MediaStore;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.SimpleThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RadioBrowser4j {

	private static final Logger LOGGER = LoggerFactory.getLogger(RadioBrowser.class.getName());
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	// Minimum number of threads in pool
	private static final ThreadPoolExecutor BACKGROUND_EXECUTOR = new ThreadPoolExecutor(
			0,
			5, // Maximum number of threads in pool
			30, TimeUnit.SECONDS, // Number of seconds before an idle thread is terminated
			// The queue holding the tasks waiting to be processed
			new LinkedBlockingQueue<>(),
			// The ThreadFactory
			new SimpleThreadFactory("Lookup RadioBrowser Metadata background worker", "Lookup RadioBrowser Metadata background workers group", Thread.NORM_PRIORITY - 1)
	);

	private static RadioBrowser radioBrowser;

	/**
	 * This class is not meant to be instantiated.
	 */
	private RadioBrowser4j() {
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

	private static boolean isRadioBrowserExists() {
		if (radioBrowser == null) {
			Optional<String> endpoint = Optional.empty();
			try {
				endpoint = new EndpointDiscovery("UMS/" + PMS.getVersion()).discover();
			} catch (IOException e) {
				LOGGER.debug("IO problem while endpoint discovery.");
			}
			if (endpoint.isPresent()) {
				ConnectionParams cx = ConnectionParams
						.builder()
						.apiUrl(endpoint.get())
						.userAgent("UMS/" + PMS.getVersion())
						.timeout(5000)
						.build();
				radioBrowser = new RadioBrowser(cx);
			}
		}
		return radioBrowser != null;
	}

	public static boolean getWebStreamMetadata(MediaInfo mediaInfo, String url, String radioBrowserUUID) {
		if (!CONFIGURATION.getExternalNetwork() || StringUtils.isAllBlank(radioBrowserUUID)) {
			return false;
		}
		if (isRadioBrowserExists()) {
			try {
				UUID uuid = UUID.fromString(radioBrowserUUID);
				Station station = radioBrowser.getStationByUUID(uuid).orElseThrow();
				String genre = getGenres(station.getTagList());
				Format f = FormatFactory.getAssociatedFormat("." + station.getCodec());
				if (!mediaInfo.hasAudio()) {
					mediaInfo.addAudioTrack(new MediaAudio());
				}
				if (f != null) {
					mediaInfo.setMimeType(f.mimeType());
				}
				if (station.getBitrate() != null) {
					mediaInfo.setBitRate(station.getBitrate());
					mediaInfo.getDefaultAudioTrack().setBitRate(station.getBitrate());
				}
				if (genre != null) {
					mediaInfo.getAudioMetadata().setGenre(genre);
				}
				if (StringUtils.isNotBlank(station.getFavicon()) && !ThumbnailSource.RADIOBROWSER.equals(mediaInfo.getThumbnailSource())) {
					DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(station.getFavicon());
					if (thumbnail != null) {
						Long fileId = MediaTableFiles.getFileId(url);
						if (fileId != null) {
							Long thumbnailId = ThumbnailStore.getId(thumbnail, fileId, ThumbnailSource.RADIOBROWSER);
							mediaInfo.setThumbnailId(thumbnailId);
							mediaInfo.setThumbnailSource(ThumbnailSource.RADIOBROWSER);
						}
					}
				}
				return true;
			} catch (Exception e) {
				LOGGER.debug("cannot read radio browser metadata for uuid {}", radioBrowserUUID, e);
			}
		}
		return false;
	}

	/**
	 * Splits tag list " / " separator.
	 *
	 * @param tags
	 * @return
	 */
	protected static String getGenres(List<String> tags) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tags.size(); i++) {
			sb.append(tags.get(i));
			if (i < tags.size() - 1) {
				sb.append(" / ");
			}
		}
		return sb.toString();
	}

	/**
	 * Enhances existing MediaInfo attached to this WebStream.
	 *
	 * @param uri WebStream uri
	 * @param mediaInfo MediaInfo
	 */
	public static void backgroundLookupAndAddMetadata(final String uri, final String radioBrowserUUID, final MediaInfo mediaInfo) {
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
			try {
				// wait until MediaStore Workers release before starting
				MediaStore.waitWorkers();
				//ensure mediaInfo is not parsing
				mediaInfo.waitMediaParsing(10);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return;
			}
			if (!StringUtils.isBlank(radioBrowserUUID) && RadioBrowser4j.getWebStreamMetadata(mediaInfo, uri, radioBrowserUUID)) {
				MediaTableFiles.insertOrUpdateData(uri, 0, Format.AUDIO, mediaInfo);
			}
		};
		LOGGER.trace("Queuing background WebStream lookup for {}", uri);
		BACKGROUND_EXECUTOR.execute(r);
	}

}
