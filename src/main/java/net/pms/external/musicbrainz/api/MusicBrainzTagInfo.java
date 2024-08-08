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
package net.pms.external.musicbrainz.api;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

/**
 * This class is a container to hold information used by
 * {@link CoverArtArchiveUtil} to look up covers.
 */
public class MusicBrainzTagInfo {

	/**
	 * The album name
	 */
	public final String album;

	/**
	 * The artist name
	 */
	public final String artist;

	/**
	 * The song title
	 */
	public final String title;

	/**
	 * The release year
	 */
	public final String year;

	/**
	 * The MusicBrainz artist ID
	 */
	public final String artistId;

	/**
	 * The MusicBrainz track ID
	 */
	public final String trackId;

	/**
	 * Creates a new instance based on the specified {@link Tag}.
	 *
	 * @param tag the {@link Tag} to get the information from.
	 */
	public MusicBrainzTagInfo(Tag tag) {
		if (MusicBrainzUtil.tagSupportsFieldKey(tag, FieldKey.ALBUM)) {
			album = tag.getFirst(FieldKey.ALBUM);
		} else {
			album = null;
		}

		if (MusicBrainzUtil.tagSupportsFieldKey(tag, FieldKey.ARTIST)) {
			artist = tag.getFirst(FieldKey.ARTIST);
		} else {
			artist = null;
		}

		if (MusicBrainzUtil.tagSupportsFieldKey(tag, FieldKey.TITLE)) {
			title = tag.getFirst(FieldKey.TITLE);
		} else {
			title = null;
		}

		if (MusicBrainzUtil.tagSupportsFieldKey(tag, FieldKey.YEAR)) {
			year = tag.getFirst(FieldKey.YEAR);
		} else {
			year = null;
		}

		if (MusicBrainzUtil.tagSupportsFieldKey(tag, FieldKey.MUSICBRAINZ_ARTISTID)) {
			artistId = tag.getFirst(FieldKey.MUSICBRAINZ_ARTISTID);
		} else {
			artistId = null;
		}

		if (MusicBrainzUtil.tagSupportsFieldKey(tag, FieldKey.MUSICBRAINZ_TRACK_ID)) {
			trackId = tag.getFirst(FieldKey.MUSICBRAINZ_TRACK_ID);
		} else {
			trackId = null;
		}
	}

	/**
	 * @return {@code true} if this {@link MusicBrainzTagInfo} has any
	 * information, {@code false} if it is "blank".
	 */
	public boolean hasInfo() {
		return isNotBlank(album) ||
				isNotBlank(artist) ||
				isNotBlank(title) ||
				isNotBlank(year) ||
				isNotBlank(artistId) ||
				isNotBlank(trackId);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		if (isNotBlank(artist)) {
			result.append(artist);
		}
		if (isNotBlank(artistId)) {
			if (result.length() > 0) {
				result.append(" (").append(artistId).append(')');
			} else {
				result.append(artistId);
			}
		}
		if (result.length() > 0 &&
				(isNotBlank(title) ||
				isNotBlank(album) ||
				isNotBlank(trackId))) {
			result.append(" - ");
		}
		if (isNotBlank(album)) {
			result.append(album);
			if (isNotBlank(title) || isNotBlank(trackId)) {
				result.append(": ");
			}
		}
		if (isNotBlank(title)) {
			result.append(title);
			if (isNotBlank(trackId)) {
				result.append(" (").append(trackId).append(')');
			}
		} else if (isNotBlank(trackId)) {
			result.append(trackId);
		}
		if (isNotBlank(year)) {
			if (result.length() > 0) {
				result.append(" (").append(year).append(')');
			} else {
				result.append(year);
			}
		}
		return result.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((album == null) ? 0 : album.hashCode());
		result = prime * result + ((artist == null) ? 0 : artist.hashCode());
		result = prime * result + ((artistId == null) ? 0 : artistId.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
		result = prime * result + ((trackId == null) ? 0 : trackId.hashCode());
		result = prime * result + ((year == null) ? 0 : year.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MusicBrainzTagInfo)) {
			return false;
		}
		MusicBrainzTagInfo other = (MusicBrainzTagInfo) obj;
		if (album == null) {
			if (other.album != null) {
				return false;
			}
		} else if (!album.equals(other.album)) {
			return false;
		}
		if (artist == null) {
			if (other.artist != null) {
				return false;
			}
		} else if (!artist.equals(other.artist)) {
			return false;
		}
		if (artistId == null) {
			if (other.artistId != null) {
				return false;
			}
		} else if (!artistId.equals(other.artistId)) {
			return false;
		}
		if (title == null) {
			if (other.title != null) {
				return false;
			}
		} else if (!title.equals(other.title)) {
			return false;
		}
		if (trackId == null) {
			if (other.trackId != null) {
				return false;
			}
		} else if (!trackId.equals(other.trackId)) {
			return false;
		}
		if (year == null) {
			if (other.year != null) {
				return false;
			}
		} else if (!year.equals(other.year)) {
			return false;
		}
		return true;
	}

}
