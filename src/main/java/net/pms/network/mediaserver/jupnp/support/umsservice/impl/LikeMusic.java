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
package net.pms.network.mediaserver.jupnp.support.umsservice.impl;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.types.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableResourceRatings;
import net.pms.network.mediaserver.jupnp.support.umsservice.UmsExtendedServicesException;
import net.pms.store.DbIdMediaType;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreResourceRatings;

public class LikeMusic {

	private static final Logger LOGGER = LoggerFactory.getLogger(LikeMusic.class.getName());
	public static final String PATH_MATCH = "like";
	public boolean isAlbumLikedMB(String musicBrainzReleaseId) throws UmsExtendedServicesException {
		return isAlbumLiked(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, musicBrainzReleaseId);
	}

	public boolean isAlbumLikedDiscogs(Long discogsReleaseId) throws UmsExtendedServicesException {
		if (discogsReleaseId == null) {
			return false;
		}
		return isAlbumLiked(DbIdMediaType.TYPE_DISCOGS_RELEASEID, discogsReleaseId.toString());
	}

	public void likeAlbumMB(String musicBrainzReleaseId) throws UmsExtendedServicesException {
		setAlbumLiked(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, musicBrainzReleaseId, true);
	}

	public void likeAlbumDiscogs(Long discogsReleaseId) throws UmsExtendedServicesException {
		if (discogsReleaseId == null) {
			return;
		}
		setAlbumLiked(DbIdMediaType.TYPE_DISCOGS_RELEASEID, discogsReleaseId.toString(), true);
	}

	public void dislikeAlbumMB(String musicBrainzReleaseId) throws UmsExtendedServicesException {
		setAlbumLiked(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, musicBrainzReleaseId, false);
	}

	public void dislikeAlbumDiscogs(Long discogsReleaseId) throws UmsExtendedServicesException {
		if (discogsReleaseId == null) {
			return;
		}
		setAlbumLiked(DbIdMediaType.TYPE_DISCOGS_RELEASEID, discogsReleaseId.toString(), false);
	}

	/**
	 * Album likes are stored as a rating of 5 stars on the album container in
	 * RESOURCE_RATINGS, the same place the generic UpdateObject rating path
	 * writes to. Both entry points therefore agree, and the My Albums folder has
	 * a single source.
	 */
	private boolean isAlbumLiked(DbIdMediaType type, String ident) throws UmsExtendedServicesException {
		if (StringUtils.isBlank(ident)) {
			return false;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Like album : cannot acquire database connection.");
			}
			return MediaTableResourceRatings.isAlbumLiked(connection, type, ident);
		} catch (SQLException e) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Like album : " + e.getMessage());
		}
	}

	private void setAlbumLiked(DbIdMediaType type, String ident, boolean liked) throws UmsExtendedServicesException {
		if (StringUtils.isBlank(ident)) {
			LOGGER.warn("{} album failed because no release id was given", liked ? "like" : "dislike");
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				LOGGER.warn("{} album action failed because database connection is null", liked ? "like" : "dislike");
				return;
			}
			MediaTableResourceRatings.setAlbumLiked(connection, type, ident, liked);
			StoreResourceRatings.clearCache();
			MediaStoreIds.incrementUpdateIdForFilename(connection, type.getResourceKey(ident));
			LOGGER.debug("{} album with {}{}", liked ? "liked" : "disliked", type, ident);
		} catch (SQLException e) {
			LOGGER.warn("{} album failed : ", liked ? "like" : "dislike", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Like album : " + e.getMessage());
		}
	}

}