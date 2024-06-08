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
package net.pms.external.musicbrainz.coverart;

/**
 * The entity (vocabulary of MusicBrainz) a cover art belongs to.
 *
 * @author schnatterer
 */
public enum CoverArtArchiveEntity {

	/**
	 * The basic entity that has a cover art.
	 */
	RELEASE("release/"),
	/**
	 * A group of releases (where each can have its own art). A release group
	 * does not have its own cover art but Cover Art Archive maps one of the
	 * releases' cover art to the release group.
	 */
	RELEASE_GROUP("release-group/");

	/**
	 * The API URL parameter that is used for querying this entity.
	 */
	private final String urlParam;

	private CoverArtArchiveEntity(String urlParam) {
		this.urlParam = urlParam;
	}

	/**
	 * @return the API URL parameter that is used for querying this entity
	 */
	public String getUrlParam() {
		return urlParam;
	}

}
