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
package net.pms.network.mediaserver.handlers;

import net.pms.network.mediaserver.MediaServer;

/**
 * This is the core stream server.
 *
 * It serve media stream, thumbnails and subtitles for media items.
 *
 * Requests format is :
 * MediaServer url /ums/{renderer uuid}/{requested}/{id}/{optional things}
 * example : http://192.168.0.1:5001/ums/3d9c333b-95ae-4692-ac20-031c1ff279fa/media/124/file.ts
 */
public class MediaStreamHandler {

	public static final String BASE_PATH = "/ums/";
	public static final String MEDIA = "media";
	public static final String MEDIA_PATH = MEDIA + "/";
	public static final String THUMBNAIL = "thumbnail";
	public static final String THUMBNAIL_PATH = THUMBNAIL + "/";
	public static final String SUBTITLES = "subtitles";
	public static final String SUBTITLES_PATH = SUBTITLES + "/";

	public static StringBuilder getServerBaseURL() {
		return new StringBuilder(MediaServer.getURL()).append(MediaStreamHandler.BASE_PATH);
	}

	public static StringBuilder getServerBaseURL(String uuid) {
		return getServerBaseURL().append(uuid).append("/");
	}

	public static StringBuilder getServerMediaURL(String uuid, String id) {
		return getServerBaseURL(uuid).append(MEDIA_PATH).append(id).append("/");
	}

	public static StringBuilder getServerSubtitlesURL(String uuid, String id) {
		return getServerBaseURL(uuid).append(SUBTITLES_PATH).append(id).append("/");
	}

	public static StringBuilder getServerThumbnailURL(String uuid, String id) {
		return getServerBaseURL(uuid).append(THUMBNAIL_PATH).append(id).append("/");
	}

	public static StringBuilder getBaseURL() {
		return new StringBuilder(MediaStreamHandler.BASE_PATH);
	}

	public static StringBuilder getBaseURL(String uuid) {
		return getBaseURL().append(uuid).append("/");
	}

	public static StringBuilder getMediaURL(String uuid) {
		return getBaseURL(uuid).append(MEDIA_PATH);
	}

	public static StringBuilder getMediaURL(String uuid, String id) {
		return getMediaURL(uuid).append(id).append("/");
	}

	public static StringBuilder getSubtitlesURL(String uuid, String id) {
		return getBaseURL(uuid).append(SUBTITLES_PATH).append(id).append("/");
	}

	public static StringBuilder getThumbnailURL(String uuid, String id) {
		return getBaseURL(uuid).append(THUMBNAIL_PATH).append(id).append("/");
	}

}
