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

import java.io.IOException;
import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.sfuhrm.radiobrowser4j.ConnectionParams;
import de.sfuhrm.radiobrowser4j.EndpointDiscovery;
import de.sfuhrm.radiobrowser4j.RadioBrowser;
import de.sfuhrm.radiobrowser4j.Station;
import net.pms.PMS;
import net.pms.database.MediaTableWebResource;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.util.FileUtil;

public class WebStreamMetadataCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStreamMetadataCollector.class.getName());
	private static final WebStreamMetadataCollector INSTANCE = new WebStreamMetadataCollector();

	private final RadioBrowser radioBrowser;
	private final Executor webStreamExecutor = Executors.newSingleThreadExecutor();

	private WebStreamMetadataCollector() {
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
		} else {
			radioBrowser = null;
		}
	}

	public static WebStreamMetadataCollector getInstance() {
		return INSTANCE;
	}

	public void collectMetadata(String url) {
		webStreamExecutor.execute(getExecutionForUrl(url, null));
	}

	public void collectMetadata(String url, String radioBrowserUUID) {
		webStreamExecutor.execute(getExecutionForUrl(url, radioBrowserUUID));
	}

	private Runnable getExecutionForUrl(String url, String radioBrowserUUID) {
		return () -> {
			if (StringUtils.isAllBlank(radioBrowserUUID)) {
				int type = 0;
				String ext = "." + FileUtil.getUrlExtension(url);
				HttpHeaders headHeaders = JavaHttpClient.getHeaders(url);
				HttpHeaders inputStreamHeaders = JavaHttpClient.getHeadersFromInputStreamRequest(url);
				if (FileUtil.getUrlExtension(url) == null) {
					LOGGER.debug("URL has no file extension. Analysing internet resource type from content-type HEADER : {}", url);
					type = getTypeFrom(headHeaders, type);
					if (type == 0) {
						type = getTypeFrom(inputStreamHeaders, type);
						if (type == 0) {
							LOGGER.warn("couldn't determine stream content type for {}", url);
						}
					}
				} else {
					LOGGER.debug("analysing internet resource type from file extension of given URL.");
					Format f = FormatFactory.getAssociatedFormat(ext);
					int defaultContent = (type != 0 && type != Format.UNKNOWN) ? type : Format.VIDEO;
					type = f == null ? defaultContent : f.getType();
				}
				if (type == Format.VIDEO || type == Format.AUDIO) {
					WebStreamMetadata wsm = new WebStreamMetadata(url, type);
					addAudioFormat(wsm, headHeaders);
					addAudioFormat(wsm, inputStreamHeaders);
					MediaTableWebResource.insertOrUpdateWebResource(wsm);
				}
			} else if (radioBrowser != null) {
				try {
					UUID uuid = UUID.fromString(radioBrowserUUID);
					Station station = radioBrowser.getStationByUUID(uuid).orElseThrow();
					Format f = FormatFactory.getAssociatedFormat("." + station.getCodec());
					WebStreamMetadata wsm = new WebStreamMetadata(
							url,
							station.getFavicon(),
							getGenres(station.getTagList()),
							f != null ? f.mimeType() : null,
							null, // sample rate not available over API
							station.getBitrate(),
							Format.AUDIO);
					MediaTableWebResource.insertOrUpdateWebResource(wsm);
				} catch (Exception e) {
					LOGGER.debug("cannot read radio browser metadata for uuid {}", radioBrowserUUID, e);
				}
			}
		};
	}

	/**
	 * Splits tag list " / " separator.
	 *
	 * @param tags
	 * @return
	 */
	protected String getGenres(List<String> tags) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tags.size(); i++) {
			sb.append(tags.get(i));
			if (i < tags.size() - 1) {
				sb.append(" / ");
			}
		}
		return sb.toString();
	}

	/*
	 * Extracts audio information from ice or icecast protocol.
	 */
	private void addAudioFormat(WebStreamMetadata streamMeta, HttpHeaders headers) {
		if (headers == null) {
			LOGGER.trace("web audio stream without header info.");
			return;
		}
		Optional<String> value = headers.firstValue("icy-br");
		if (value.isPresent()) {
			streamMeta.setBitrate(parseIntValue(value.get()));
		}
		value = headers.firstValue("icy-sr");
		if (value.isPresent()) {
			streamMeta.setSampleRate(parseIntValue(value.get()));
		}

		value = headers.firstValue("icy-genre");
		if (value.isPresent()) {
			streamMeta.setGenre(value.get());
		}

		value = headers.firstValue("content-type");
		if (value.isPresent()) {
			streamMeta.setContentType(value.get());
		}

		value = headers.firstValue("icy-logo");
		if (value.isPresent()) {
			streamMeta.setLogoUrl(value.get());
		}
	}

	private Integer parseIntValue(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private int getTypeFrom(HttpHeaders headers, int currentType) {
		String contentType = headers.firstValue("content-type").orElse("");
		if (contentType.isEmpty()) {
			LOGGER.trace("web server has no content type set ...");
			return currentType;
		} else {
			if (contentType.startsWith("audio")) {
				return Format.AUDIO;
			} else if (contentType.startsWith("video")) {
				return Format.VIDEO;
			} else if (contentType.startsWith("image")) {
				return Format.IMAGE;
			} else {
				LOGGER.trace("web server has no content type set ...");
				return currentType;
			}
		}
	}

}
