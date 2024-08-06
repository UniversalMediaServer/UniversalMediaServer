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
package net.pms.store;

public enum ThumbnailSource {

	UNKNOWN,
	MPLAYER_SEEK,
	FFMPEG_SEEK,
	TMDB,
	TMDB_LOC,
	MUSICBRAINZ,
	WEBSTREAM,
	RADIOBROWSER,
	EMBEDDED;

	public static ThumbnailSource valueOfName(String value) {
		try {
			return valueOf(ThumbnailSource.class, value);
		} catch (IllegalArgumentException | NullPointerException e) {
			//bad string
		}
		return UNKNOWN;
	}

}
