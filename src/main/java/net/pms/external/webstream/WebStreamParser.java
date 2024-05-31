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

import java.net.http.HttpHeaders;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.WebStreamMetadata;
import net.pms.util.FileUtil;

public class WebStreamParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStreamParser.class.getName());

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebStreamParser() {
	}

	public static WebStreamMetadata getWebStreamMetadata(String url) {
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
			type = f == null ? Format.VIDEO : f.getType();
		}
		if (type == Format.VIDEO || type == Format.AUDIO) {
			WebStreamMetadata wsm = new WebStreamMetadata(url, type);
			addAudioFormat(wsm, headHeaders);
			addAudioFormat(wsm, inputStreamHeaders);
			return wsm;
		}
		return null;
	}

	/*
	 * Extracts audio information from ice or icecast protocol.
	 */
	private static void addAudioFormat(WebStreamMetadata streamMeta, HttpHeaders headers) {
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

	private static Integer parseIntValue(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int getTypeFrom(HttpHeaders headers, int currentType) {
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
