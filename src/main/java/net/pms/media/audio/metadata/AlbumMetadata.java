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
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdTypeAndIdent;

public class AlbumMetadata {
	private final String mbRecordId;
	private final Long discogsReleaseId;
	private final String album;
	private final String artist;
	private final String year;
	private String genre;

	public AlbumMetadata(String mbRecordId, Long discogsReleaseId, String album, String artist, String year, String genre) {
		this.mbRecordId = mbRecordId;
		this.discogsReleaseId = discogsReleaseId;
		this.album = album;
		this.artist = artist;
		this.year = year;
		this.genre = genre;

		if (mbRecordId == null && discogsReleaseId == null) {
			throw new IllegalArgumentException("At least one of mbRecordId or discogsRecordId must be provided");
		}
	}

	public DbIdTypeAndIdent getTypeIdent() {
		// First we try MusicBrainz ID. If not present, we will use Discogs ID.
		if (mbRecordId != null) {
			return new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, mbRecordId);
		} else if (discogsReleaseId != null) {
			return new DbIdTypeAndIdent(DbIdMediaType.TYPE_DISCOGS_RELEASEID, discogsReleaseId.toString());
		}
		throw new IllegalArgumentException("At least one of mbRecordId or discogsRecordId must be provided");
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

	public String getDiscogsRecordId() {
		if (discogsReleaseId == null) {
			return null;
		}
		return discogsReleaseId.toString();
	}

	@Override
	public int hashCode() {
		return Objects.hash(discogsReleaseId, mbRecordId);
	}

	@Override
	public boolean equals(Object obj) {
		if (getClass() != obj.getClass()) {
			return false;
		}

		AlbumMetadata other = (AlbumMetadata) obj;

		// First check MusicBrainz ID. If both have it, compare it. If not, check Discogs ID.
		if (mbRecordId != null && other.mbRecordId != null) {
			return Objects.equals(mbRecordId, other.mbRecordId);
		} else if (discogsReleaseId != null && other.discogsReleaseId != null) {
			return Objects.equals(discogsReleaseId, other.discogsReleaseId);
		}

		return false;
	}

	@Override
	public String toString() {
		return "MusicBrainzAlbum [mbRecordId=" + mbRecordId + ", discogsRecordId=" + discogsReleaseId + ", album=" + album + ", artist=" +
			artist + ", year=" + year + ", genre=" + genre + "]";
	}

}
