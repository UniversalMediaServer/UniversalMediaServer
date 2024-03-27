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
package net.pms.network.mediaserver;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * This is the core stream server request.
 *
 * It serve media stream, thumbnails and subtitles for media items.
 *
 * Requests format is : MediaServer url /ums/{requested}/{renderer
 * uuid}/{id}/{optional things} example :
 * http://192.168.0.1:5001/ums/media/3d9c333b-95ae-4692-ac20-031c1ff279fa/124/file.ts
 */
public class MediaServerRequest {

	public static final String PATH_SEPARATOR = "/";

	public static final String UMS = "ums";
	public static final String UMS_PATH = UMS + PATH_SEPARATOR;
	public static final String BASE_PATH = PATH_SEPARATOR + UMS_PATH;
	public static final String MEDIA = "media";
	public static final String MEDIA_PATH = MEDIA + PATH_SEPARATOR;
	public static final String THUMBNAIL = "thumbnail";
	public static final String THUMBNAIL_PATH = THUMBNAIL + PATH_SEPARATOR;
	public static final String SUBTITLES = "subtitles";
	public static final String SUBTITLES_PATH = SUBTITLES + PATH_SEPARATOR;

	private final MediaServerRequestType requestType;
	private final String uuid;
	private final String resourceId;
	private final String optionalPath;

	public MediaServerRequest(String path) {
		int pos = path.indexOf(BASE_PATH);
		if (pos != -1) {
			path = path.substring(pos + BASE_PATH.length());
		} else if (path.startsWith(UMS_PATH)) {
			path = path.substring(UMS_PATH.length());
		} else if (path.startsWith(PATH_SEPARATOR)) {
			path = path.substring(1);
		}
		/**
		 * part 1 : action part 2 : renderer uuid part 3 : resource id part 4 :
		 * optional
		 */
		String[] requestData = path.split(PATH_SEPARATOR, 4);
		if (requestData.length < 3) {
			//Bad Request
			uuid = null;
			requestType = MediaServerRequestType.BAD_REQUEST;
			resourceId = null;
			optionalPath = null;
		} else {
			requestType = switch (requestData[0]) {
				case MEDIA ->
					MediaServerRequestType.MEDIA;
				case THUMBNAIL ->
					MediaServerRequestType.THUMBNAIL;
				case SUBTITLES ->
					MediaServerRequestType.SUBTITLES;
				default ->
					MediaServerRequestType.BAD_REQUEST;
			};
			uuid = requestData[1];
			resourceId = URLDecoder.decode(requestData[2], StandardCharsets.UTF_8);
			optionalPath = requestData[3];
		}
	}

	public String getUuid() {
		return uuid;
	}

	public MediaServerRequestType getRequestType() {
		return requestType;
	}

	public boolean isBadRequest() {
		return requestType == MediaServerRequestType.BAD_REQUEST;
	}

	public boolean isMediaRequest() {
		return requestType == MediaServerRequestType.MEDIA;
	}

	public boolean isThumbnailRequest() {
		return requestType == MediaServerRequestType.THUMBNAIL;
	}

	public boolean isSubtitlesRequest() {
		return requestType == MediaServerRequestType.SUBTITLES;
	}

	public String getResourceId() {
		return resourceId;
	}

	public String getOptionalPath() {
		return optionalPath;
	}

	private static StringBuilder getMediaServerURL() {
		return new StringBuilder(MediaServer.getURL());
	}

	public static StringBuilder getServerMediaURL(String uuid, String id) {
		return getMediaServerURL().append(getMediaURL(uuid, id));
	}

	public static StringBuilder getServerSubtitlesURL(String uuid, String id) {
		return getMediaServerURL().append(getSubtitlesURL(uuid, id));
	}

	public static StringBuilder getServerThumbnailURL(String uuid, String id) {
		return getMediaServerURL().append(getThumbnailURL(uuid, id));
	}

	private static StringBuilder getBaseURL() {
		return new StringBuilder(BASE_PATH);
	}

	private static StringBuilder getBaseURL(String endpoint) {
		return getBaseURL().append(endpoint);
	}

	private static StringBuilder getBaseURL(String endpoint, String uuid) {
		return getBaseURL(endpoint).append(uuid).append(PATH_SEPARATOR);
	}

	private static StringBuilder getBaseURL(String endpoint, String uuid, String id) {
		return getBaseURL(endpoint, uuid).append(id).append(PATH_SEPARATOR);
	}

	public static StringBuilder getMediaURL() {
		return getBaseURL(MEDIA_PATH);
	}

	public static StringBuilder getMediaURL(String uuid) {
		return getBaseURL(MEDIA_PATH, uuid);
	}

	private static StringBuilder getMediaURL(String uuid, String id) {
		return getBaseURL(MEDIA_PATH, uuid, id);
	}

	private static StringBuilder getSubtitlesURL(String uuid, String id) {
		return getBaseURL(SUBTITLES_PATH, uuid, id);
	}

	private static StringBuilder getThumbnailURL(String uuid, String id) {
		return getBaseURL(THUMBNAIL_PATH, uuid, id);
	}

}
