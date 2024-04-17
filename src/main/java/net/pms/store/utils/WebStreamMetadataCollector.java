package net.pms.store.utils;

import java.net.http.HttpHeaders;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaTableWebResource;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.util.FileUtil;
import net.pms.util.HttpUtil;

public class WebStreamMetadataCollector {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStreamMetadataCollector.class.getName());
	private static WebStreamMetadataCollector instance = new WebStreamMetadataCollector();

	private Executor webStreamExecutor = Executors.newSingleThreadExecutor();

	private WebStreamMetadataCollector() {
	}

	public static WebStreamMetadataCollector getInstance() {
		return instance;
	}

	public void collectMetadata(String url) {
		webStreamExecutor.execute(getExecutionForUrl(url));
	}

	private Runnable getExecutionForUrl(String url) {
		return new Runnable() {

			@Override
			public void run() {
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
			}
		};
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
