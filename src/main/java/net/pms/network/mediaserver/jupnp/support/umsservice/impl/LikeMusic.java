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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.jupnp.model.types.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableMusicBrainzReleaseLike;
import net.pms.database.MediaTableDiscogsReleaseLike;
import net.pms.network.mediaserver.jupnp.support.umsservice.UmsExtendedServicesException;

public class LikeMusic {

	private static final Logger LOGGER = LoggerFactory.getLogger(LikeMusic.class.getName());
	public static final String PATH_MATCH = "like";
	private final String backupFilenameMusicBrainz;
	private final String backupFilenameDiscogs;

	public LikeMusic() {
		String dir = FilenameUtils.concat(UmsConfiguration.getProfileDirectory(), "database_backup");
		backupFilenameMusicBrainz = FilenameUtils.concat(dir, "MUSIC_BRAINZ_RELEASE_LIKE");
		backupFilenameDiscogs = FilenameUtils.concat(dir, "DISCOGS_RELEASE_LIKE");
	}

	private boolean baseDbRequest(String sql, String key) throws UmsExtendedServicesException {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				throw new RuntimeException("cannot acquire database connection.");
			}
			return isCountGreaterZero(sql, connection, key);
		} catch (SQLException e) {
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Like album : " + e.getMessage());
		}
	}

	public boolean isAlbumLiked(String musicBrainzReleaseId) throws UmsExtendedServicesException {
		String sql = "SELECT COUNT(*) FROM " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " WHERE " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + " = ?";
		return baseDbRequest(sql, musicBrainzReleaseId);
	}

	public void likeAlbum(String musicBrainzReleaseId) throws UmsExtendedServicesException {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				LOGGER.warn("likeAlbum action failed because database connection is null");
				return;
			}
			String sql = "MERGE INTO " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " KEY (MBID_RELEASE) values (?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, musicBrainzReleaseId);
				ps.executeUpdate();
			}
			LOGGER.debug("album liked with musicBrainzReleaseId {}", musicBrainzReleaseId);
		} catch (SQLException e) {
			LOGGER.warn("like album failed : ", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Like album : " + e.getMessage());
		}
	}

	public void dislikeAlbum(String musicBrainzReleaseId) throws UmsExtendedServicesException {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection == null) {
				LOGGER.warn("dislikeAlbum action failed because database connection is null");
				return;
			}
			String sql = "DELETE FROM " + MediaTableMusicBrainzReleaseLike.TABLE_NAME + " WHERE " + MediaTableMusicBrainzReleaseLike.TABLE_COL_MBID_RELEASE + " = ?";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, musicBrainzReleaseId);
				ps.executeUpdate();
			}
			LOGGER.debug("disliked album with musicBrainzReleaseId {}", musicBrainzReleaseId);
		} catch (SQLException e) {
			LOGGER.warn("dislike album failed : ", e);
			throw new UmsExtendedServicesException(ErrorCode.ACTION_FAILED, "Dislike album : " + e.getMessage());
		}
	}

	private boolean isCountGreaterZero(String sql, Connection connection, String key) {
		try (PreparedStatement ps = connection.prepareStatement(sql);) {
			ps.setString(1, key);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getLong(1) > 0;
			}
		} catch (SQLException e) {
			throw new RuntimeException("LikeMusic failed. Cannot handle request, because of an SQLException", e);
		}
		return false;
	}

	public void backupLikedAlbums() throws SQLException {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			Script.process(connection, backupFilenameMusicBrainz, "", "TABLE " + MediaTableMusicBrainzReleaseLike.TABLE_NAME);
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			Script.process(connection, backupFilenameDiscogs, "", "TABLE " + MediaTableDiscogsReleaseLike.TABLE_NAME);
		}
	}

	public void restoreLikedAlbums() throws SQLException, FileNotFoundException {
		restoreMusicBrainzLikedAlbums();
		restoreDiscogsLikedAlbums();
	}

	private void restoreMusicBrainzLikedAlbums() throws SQLException, FileNotFoundException {
		File backupFile = new File(backupFilenameMusicBrainz);
		if (backupFile.exists() && backupFile.isFile()) {
			try (Connection connection = MediaDatabase.getConnectionIfAvailable(); Statement stmt = connection.createStatement()) {
				String sql;
				sql = "DROP TABLE " + MediaTableMusicBrainzReleaseLike.TABLE_NAME;
				stmt.execute(sql);
				try {
					RunScript.execute(connection, new FileReader(backupFilenameMusicBrainz));
				} catch (FileNotFoundException | SQLException e) {
					LOGGER.error("restoring MUSIC_BRAINZ_RELEASE_LIKE table : failed");
					throw new RuntimeException("restoring MUSIC_BRAINZ_RELEASE_LIKE table failed", e);
				}
				connection.commit();
				LOGGER.trace("restoring MUSIC_BRAINZ_RELEASE_LIKE table : success");
			}
		} else {
			if (!StringUtils.isEmpty(backupFilenameMusicBrainz)) {
				LOGGER.trace("LikeMusik: Backup file doesn't exist : " + backupFilenameMusicBrainz);
				throw new RuntimeException("Backup file doesn't exist : " + backupFilenameMusicBrainz);
			} else {
				throw new RuntimeException("Backup filename not set !");
			}
		}
	}

	private void restoreDiscogsLikedAlbums() throws SQLException, FileNotFoundException {
		File backupFile = new File(backupFilenameDiscogs);
		if (backupFile.exists() && backupFile.isFile()) {
			try (Connection connection = MediaDatabase.getConnectionIfAvailable(); Statement stmt = connection.createStatement()) {
				String sql;
				sql = "DROP TABLE " + MediaTableDiscogsReleaseLike.TABLE_NAME;
				stmt.execute(sql);
				try {
					RunScript.execute(connection, new FileReader(backupFilenameDiscogs));
				} catch (FileNotFoundException | SQLException e) {
					LOGGER.error("restoring DISCOGS_RELEASE_LIKE table : failed");
					throw new RuntimeException("restoring DISCOGS_RELEASE_LIKE table failed", e);
				}
				connection.commit();
				LOGGER.trace("restoring DISCOGS_RELEASE_LIKE table : success");
			}
		} else {
			if (!StringUtils.isEmpty(backupFilenameDiscogs)) {
				LOGGER.trace("LikeMusik: Backup file doesn't exist : " + backupFilenameDiscogs);
				throw new RuntimeException("Backup file doesn't exist : " + backupFilenameDiscogs);
			} else {
				throw new RuntimeException("Backup filename not set !");
			}
		}
	}
}
