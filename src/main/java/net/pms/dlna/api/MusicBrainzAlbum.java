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
package net.pms.dlna.api;

import java.util.Objects;

public class MusicBrainzAlbum {
	private final String mbReleaseid;
	private final String album;
	private final String artist;
	private final Integer year;
	private String genre;

	public MusicBrainzAlbum(String mbReleaseid, String album, String artist, Integer year, String genre) {
		this.mbReleaseid = mbReleaseid;
		this.album = album;
		this.artist = artist;
		this.year = year;
		this.genre = genre;
	}

	public String getMbReleaseid() {
		return mbReleaseid;
	}

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	public int getYear() {
		return year;
	}

	public String getGenre() {
		return genre;
	}

	public void setGenre(String genre) {
		this.genre = genre;
	}

	@Override
	public int hashCode() {
		return Objects.hash(mbReleaseid);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MusicBrainzAlbum other = (MusicBrainzAlbum) obj;
		return Objects.equals(mbReleaseid, other.mbReleaseid);
	}

}
