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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DoubleRecordFilter {

	private Map<MusicBrainzAlbum, HashSet<String>> mbidGenres = new HashMap<>();
	private final Set<MusicBrainzAlbum> albumObjects = new HashSet<>();

	public void addAlbum(MusicBrainzAlbum album) {
		extractGenres(album);
		albumObjects.add(album);
	}

	private void extractGenres(MusicBrainzAlbum album) {
		HashSet<String> genres = getGenres(album);
		if (album.getGenre() != null) {
			String[] splitGenre = album.getGenre().split("/");
			for (String genre : splitGenre) {
				genres.add(genre.trim());
			}
			updatGenre(genres, album);
		}
	}

	private void updatGenre(HashSet<String> genres, MusicBrainzAlbum album) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (String genre : genres) {
			sb.append(genre);
			i++;
			if (genres.size() < i) {
				sb.append(" /");
			}
		}
		album.setGenre(sb.toString());
	}

	private HashSet<String> getGenres(MusicBrainzAlbum album) {
		if (mbidGenres.get(album) == null) {
			mbidGenres.put(album, new HashSet<>());
		}
		return mbidGenres.get(album);
	}

	/**
	 * Adds song genres to current musicbrainz-release genere field.
	 *
	 * @param album
	 */
	private void addGenres(MusicBrainzAlbum album) {
	}

	public Set<MusicBrainzAlbum> getUniqueAlbumSet() {
		return albumObjects;
	}
}
