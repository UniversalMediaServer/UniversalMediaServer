/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum DbIdMediaType {

	//@formatter:off
	TYPE_AUDIO("FID$", "object.item.audioItem"),
	TYPE_FOLDER("FOLDER$", "object.container.storageFolder"),
	TYPE_ALBUM("ALBUM$", "object.container.album.musicAlbum"),
	TYPE_MUSICBRAINZ_RECORDID("MUSICBRAINZALBUM$", "object.container.album.musicAlbum"),
	TYPE_PERSON("PERSON$", "object.container.person.musicArtist"),
	TYPE_PERSON_ALBUM_FILES("PERSON_ALBUM_FILES$", "object.container.storageFolder"),
	TYPE_PERSON_ALBUM("PERSON_ALBUM$", "object.container.storageFolder"),
	TYPE_PERSON_ALL_FILES("PERSON_ALL_FILES$", "object.container.storageFolder"),
	TYPE_PLAYLIST("PLAYLIST$", "object.container.playlistContainer"),
	TYPE_VIDEO("VIDEO$", "object.item.videoItem"),
	TYPE_IMAGE("IMAGE$", "object.item.imageItem");
	//@formatter:on//@formatter:on//@formatter:on//@formatter:on

	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdMediaType.class);
	public final static String GENERAL_PREFIX = "$DBID$";
	public static final String SPLIT_CHARS = "___";
	public final String dbidPrefix;
	public final String uclass;

	DbIdMediaType(String dbidPrefix, String uclass) {
		this.dbidPrefix = dbidPrefix;
		this.uclass = uclass;
	}

	public static DbIdTypeAndIdent getTypeIdentByDbid(String id) {
		String strType = id.substring(DbIdMediaType.GENERAL_PREFIX.length());
		for (DbIdMediaType type : values()) {
			if (strType.startsWith(type.dbidPrefix)) {
				String ident = strType.substring(type.dbidPrefix.length());
				try {
					return new DbIdTypeAndIdent(type, URLDecoder.decode(ident, StandardCharsets.UTF_8.toString()));
				} catch (UnsupportedEncodingException e) {
					LOGGER.warn("decode error", e);
					return new DbIdTypeAndIdent(type, ident);
				}
			}
		}
		throw new RuntimeException("Unknown DBID type : " + id);
	}
}

