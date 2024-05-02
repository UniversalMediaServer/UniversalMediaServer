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
package net.pms.media.audio.metadata;

import java.util.Objects;

public class MusicBrainzAlbum {
	private final String mbRecordId;
	private final String album;
	private final String artist;
	private final String year;
	private String genre;

	public MusicBrainzAlbum(String mbRecordId, String album, String artist, String year, String genre) {
		this.mbRecordId = mbRecordId;
		this.album = album;
		this.artist = artist;
		this.year = year;
		this.genre = genre;
	}

	public String getMbReleaseid() {
		return mbRecordId;
	}

	public String getAlbum() {
		return album;
	}

	public String getArtist() {
		return artist;
	}

	public String getYear() {
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
		return Objects.hash(mbRecordId);
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
		return Objects.equals(mbRecordId, other.mbRecordId);
	}

	@Override
	public String toString() {
		return "MusicBrainzAlbum [mbRecordId=" + mbRecordId + ", album=" + album + ", artist=" + artist + ", year=" + year + ", genre=" +
			genre + "]";
	}

}
