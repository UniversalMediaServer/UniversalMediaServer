package net.pms.store.utils;

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
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.util.FileUtil;
import net.pms.util.HttpUtil;

public class WebStreamMetadataCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStreamMetadataCollector.class.getName());
	private static WebStreamMetadataCollector instance = new WebStreamMetadataCollector();

	private Executor webStreamExecutor = Executors.newSingleThreadExecutor();

	private RadioBrowser radioBrowser = null;

	private WebStreamMetadataCollector() {
		Optional<String> endpoint;
		try {
			endpoint = new EndpointDiscovery("UMS/" + PMS.getVersion()).discover();
			radioBrowser = new RadioBrowser(
				ConnectionParams.builder().apiUrl(endpoint.get()).userAgent("UMS/" + PMS.getVersion()).timeout(5000).build());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static WebStreamMetadataCollector getInstance() {
		return instance;
	}

	public void collectMetadata(String url) {
		webStreamExecutor.execute(getExecutionForUrl(url, null));
	}

	public void collectMetadata(String url, String radioBrowserUUID) {
		webStreamExecutor.execute(getExecutionForUrl(url, radioBrowserUUID));
	}

	private Runnable getExecutionForUrl(String url, String radioBrowserUUID) {
		return new Runnable() {

			@Override
			public void run() {
				if (StringUtils.isAllBlank(radioBrowserUUID)) {
					int type = 0;
					String ext = "." + FileUtil.getUrlExtension(url);
					HttpHeaders headHeaders = HttpUtil.getHeaders(url);
					HttpHeaders inputStreamHeaders = HttpUtil.getHeadersFromInputStreamRequest(url);
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
						StreamMeta sm = new StreamMeta();
						addAudioFormat(sm, headHeaders);
						addAudioFormat(sm, inputStreamHeaders);
						WebStreamMetadata wsm = new WebStreamMetadata(url, sm.logo, sm.genre, sm.contentType, sm.sample, sm.bitrate, type);
						MediaTableWebResource.insertOrUpdateWebResource(wsm);
					}
				} else {
					try {
						UUID uuid = UUID.fromString(radioBrowserUUID);
						Optional<Station> optStation = radioBrowser.getStationByUUID(uuid);
						Station station = optStation.get();
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
	private void addAudioFormat(StreamMeta streamMeta, HttpHeaders headers) {
		if (headers == null) {
			LOGGER.trace("web audio stream without header info.");
			return;
		}
		Optional<String> value = null;
		value = headers.firstValue("icy-br");
		if (value.isPresent()) {
			streamMeta.bitrate = parseIntValue(value) != null ? parseIntValue(value) : null;
		}
		value = headers.firstValue("icy-sr");
		if (value.isPresent()) {
			streamMeta.sample = parseIntValue(value) != null ? parseIntValue(value) : null;
		}

		value = headers.firstValue("icy-genre");
		if (value.isPresent()) {
			streamMeta.genre = value.get();
		}

		value = headers.firstValue("content-type");
		if (value.isPresent()) {
			streamMeta.contentType = value.get();
		}

		value = headers.firstValue("icy-logo");
		if (value.isPresent()) {
			streamMeta.logo = value.get();
		}
	}

	private Integer parseIntValue(Optional<String> value) {
		try {
			return Integer.parseInt(value.get());
		} catch (Exception e) {
			return null;
		}
	}

	private int getTypeFrom(HttpHeaders headers, int currentType) {
		if (headers.firstValue("content-type").isEmpty()) {
			LOGGER.trace("web server has no content type set ...");
			return currentType;
		} else {
			String contentType = headers.firstValue("content-type").get();
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
