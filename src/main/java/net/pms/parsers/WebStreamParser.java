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
package net.pms.parsers;

import java.net.http.HttpHeaders;
import net.pms.database.MediaTableFiles;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebStreamParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebStreamParser.class.getName());

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebStreamParser() {
	}

	public static void parse(MediaInfo mediaInfo, String url, int type) {
		//ensure mediaInfo is not already parsing or is parsed
		mediaInfo.waitMediaParsing(5);
		if (mediaInfo.isMediaParsed()) {
			return;
		}
		mediaInfo.setParsing(true);
		mediaInfo.resetParser();
		HttpHeaders headHeaders = JavaHttpClient.getHeaders(url);
		String contentType = headHeaders.firstValue("content-type").orElse(null);
		if (contentType != null) {
			mediaInfo.setMimeType(contentType);
		}
		if (type == Format.AUDIO) {
			HttpHeaders getHeaders = JavaHttpClient.getHeadersFromInputStreamRequest(url);
			addAudioIcyInfos(mediaInfo, url, headHeaders);
			addAudioIcyInfos(mediaInfo, url, getHeaders);
		}
		mediaInfo.setParsing(false);
		FFmpegParser.parseUrl(mediaInfo, url);
		mediaInfo.setMediaParser("WEBSTREAM");
	}

	public static int getWebStreamType(String url, int defaultType) {
		int type = getTypeFromUrl(url, defaultType);
		if (type == 0 || type == Format.UNKNOWN) {
			LOGGER.debug("Analyzing internet resource type from content-type HEADER : {}", url);
			HttpHeaders headHeaders = JavaHttpClient.getHeaders(url);
			type = getTypeFromHttpHeaders(headHeaders, 0);
			if (type == 0) {
				HttpHeaders getHeaders = JavaHttpClient.getHeadersFromInputStreamRequest(url);
				type = getTypeFromHttpHeaders(getHeaders, 0);
			}
			if (type == 0) {
				LOGGER.debug("Couldn't determine stream content type from content-type HEADER for {}", url);
			} else {
				LOGGER.debug("Stream content type set to {} for {}", Format.getStringType(type), url);
			}
		}
		if (type == 0 || type == Format.UNKNOWN) {
			type = defaultType;
			LOGGER.debug("Stream content type set to default {} for {}", Format.getStringType(type), url);
		}
		return type;
	}

	/*
	 * Extracts audio information from ice or icecast protocol.
	 */
	private static void addAudioIcyInfos(MediaInfo mediaInfo, String url, HttpHeaders headers) {
		if (headers == null) {
			LOGGER.trace("web audio stream without header info.");
			return;
		}
		if (!mediaInfo.hasAudioMetadata()) {
			mediaInfo.setAudioMetadata(new MediaAudioMetadata());
		}
		Integer bitrate = parseIntValue(headers.firstValue("icy-br").orElse(null));
		if (bitrate != null) {
			mediaInfo.setBitRate(bitrate);
			if (mediaInfo.hasAudio()) {
				mediaInfo.getDefaultAudioTrack().setBitRate(bitrate);
			}
		}
		Integer sampleRate = parseIntValue(headers.firstValue("icy-sr").orElse(null));
		if (sampleRate != null && mediaInfo.hasAudio()) {
			mediaInfo.getDefaultAudioTrack().setSampleRate(sampleRate);
		}
		String genre = headers.firstValue("icy-genre").orElse(null);
		if (genre != null) {
			if (!mediaInfo.hasAudioMetadata()) {
				mediaInfo.setAudioMetadata(new MediaAudioMetadata());
			}
			mediaInfo.getAudioMetadata().setGenre(genre);
		}
		String logo = headers.firstValue("icy-logo").orElse(null);
		if (StringUtils.isNotBlank(logo) &&
				!ThumbnailSource.WEBSTREAM.equals(mediaInfo.getThumbnailSource()) &&
				!ThumbnailSource.RADIOBROWSER.equals(mediaInfo.getThumbnailSource())) {
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(logo);
			if (thumbnail != null) {
				Long fileId = MediaTableFiles.getFileId(url);
				if (fileId != null) {
					Long thumbnailId = ThumbnailStore.getId(thumbnail, fileId, ThumbnailSource.WEBSTREAM);
					mediaInfo.setThumbnailId(thumbnailId);
					mediaInfo.setThumbnailSource(ThumbnailSource.WEBSTREAM);
				}
			}
		}
	}

	private static Integer parseIntValue(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static int getTypeFromUrl(String url, int defaultType) {
		int type = 0;
		String ext = FileUtil.getUrlExtension(url);
		if (ext != null) {
			ext = "." + ext;
			LOGGER.debug("Analyzing internet resource type from file extension of given URL.");
			Format f = FormatFactory.getAssociatedFormat(ext);
			if (f != null) {
				type = f.getType();
				if (type == Format.PLAYLIST && !url.endsWith(ext)) {
					// If the filename continues past the "extension" (i.e. has
					// a query string) it's
					// likely not a nested playlist but a media item, for
					// instance Twitch TV media urls:
					// 'http://video10.iad02.hls.twitch.tv/.../index-live.m3u8?token=id=235...'
					type = defaultType;
				}
				LOGGER.debug("Stream content type set to {} for {}", Format.getStringType(type), url);
			} else {
				LOGGER.debug("Couldn't determine stream content type from file extension for {}", url);
			}
		}
		return type;
	}

	private static int getTypeFromHttpHeaders(HttpHeaders headers, int currentType) {
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
