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
package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import net.pms.media.metadata.VideoMetadataLocalized;
import net.pms.util.APIUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableVideoMetadataLocalized extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadataLocalized.class);
	public static final String TABLE_NAME = "VIDEO_METADATA_LOCALIZED";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * COLUMNS
	 */
	private static final String COL_LANGUAGE = "LANGUAGE";
	private static final String COL_ID = "ID";
	private static final String COL_FILEID = "FILEID";
	private static final String COL_TVSERIESID = MediaTableTVSeries.CHILD_ID;
	private static final String COL_HOMEPAGE = "HOMEPAGE";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_OVERVIEW = "OVERVIEW";
	private static final String COL_POSTER = "POSTER";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_TAGLINE = "TAGLINE";

	private static final String TABLE_COL_LANGUAGE = TABLE_NAME + "." + COL_LANGUAGE;
	private static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	private static final String TABLE_COL_TVSERIESID = TABLE_NAME + "." + COL_TVSERIESID;

	private static final String SQL_GET_ALL_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ?";
	private static final String SQL_GET_ALL_TVSERIESID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_TVSERIESID + " = ?";
	private static final String SQL_GET_ALL_LANGUAGE_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_LANGUAGE + " = ? AND " + TABLE_COL_FILEID + " = ?";
	private static final String SQL_GET_ALL_LANGUAGE_TVSERIESID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_LANGUAGE + " = ? AND " + TABLE_COL_TVSERIESID + " = ?";

	private static final int SIZE_LANGUAGE = 5;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (tableExists(connection, TABLE_NAME)) {
			Integer version = MediaTableTablesVersions.getTableVersion(connection, TABLE_NAME);
			if (version != null) {
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
				}
			} else {
				LOGGER.warn(LOG_TABLE_UNKNOWN_VERSION_RECREATE, DATABASE_NAME, TABLE_NAME);
				dropTable(connection, TABLE_NAME);
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	/**
	 * This method <strong>MUST</strong> be updated if the table definition are
	 * altered. The changes for each version in the form of
	 * <code>ALTER TABLE</code> must be implemented here.
	 *
	 * @param connection the {@link Connection} to use
	 * @param currentVersion the version to upgrade <strong>from</strong>
	 *
	 * @throws SQLException
	 */
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS " + COL_MODIFIED + " BIGINT");
				}
				default -> {
					throw new IllegalStateException(getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION));
				}
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				COL_ID + "         IDENTITY                        PRIMARY KEY , " +
				COL_LANGUAGE + "   VARCHAR(" + SIZE_LANGUAGE + ")  NOT NULL    , " +
				COL_FILEID + "     INTEGER                                     , " +
				COL_TVSERIESID + " INTEGER                                     , " +
				COL_MODIFIED + "   BIGINT                                      , " +
				COL_HOMEPAGE + "   VARCHAR                                     , " +
				COL_OVERVIEW + "   CLOB                                        , " +
				COL_POSTER + "     VARCHAR                                     , " +
				COL_TAGLINE + "    VARCHAR                                     , " +
				COL_TITLE + "      VARCHAR                                     , " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY (" + COL_FILEID + ") REFERENCES " + MediaTableVideoMetadata.REFERENCE_TABLE_COL_FILE_ID + " ON DELETE CASCADE, " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_TVSERIESID + "_FK FOREIGN KEY (" + COL_TVSERIESID + ") REFERENCES " + MediaTableTVSeries.REFERENCE_TABLE_COL_ID + " ON DELETE CASCADE " +
			")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_LANGUAGE + "_" + COL_FILEID + "_" + COL_TVSERIESID + "_IDX ON " + TABLE_NAME + "(" + COL_LANGUAGE + "," + COL_FILEID + "," + COL_TVSERIESID + ")"
		);
	}

	private static void set(final Connection connection, final Long id, final boolean fromTvSeries, final VideoMetadataLocalized metadata, final String language) {
		if (id == null || id < 0 || StringUtils.isBlank(language)) {
			return;
		}
		try (PreparedStatement ps = connection.prepareStatement(
					fromTvSeries ? SQL_GET_ALL_LANGUAGE_TVSERIESID : SQL_GET_ALL_LANGUAGE_FILEID,
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_UPDATABLE)
		) {
			ps.setString(1, language);
			ps.setLong(2, id);
			try (ResultSet rs = ps.executeQuery()) {
				boolean isCreatingNewRecord = !rs.next();
				if (isCreatingNewRecord) {
					rs.moveToInsertRow();
					rs.updateString(COL_LANGUAGE, StringUtils.left(language, SIZE_LANGUAGE));
					rs.updateLong(fromTvSeries ? COL_TVSERIESID : COL_FILEID, id);
				}
				rs.updateLong(COL_MODIFIED, System.currentTimeMillis());
				rs.updateString(COL_HOMEPAGE, metadata == null ? null : metadata.getHomepage());
				rs.updateString(COL_OVERVIEW, metadata == null ? null : metadata.getOverview());
				rs.updateString(COL_POSTER, metadata == null ? null : metadata.getPoster());
				rs.updateString(COL_TAGLINE, metadata == null ? null : metadata.getTagline());
				rs.updateString(COL_TITLE, metadata == null ? null : metadata.getTitle());
				if (isCreatingNewRecord) {
					rs.insertRow();
				} else {
					rs.updateRow();
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static Map<String, VideoMetadataLocalized> getAllVideoMetadataLocalized(final Connection connection, final Long id, final boolean fromTvSeries) {
		Map<String, VideoMetadataLocalized> result = new HashMap<>();
		try {
			try (PreparedStatement ps = connection.prepareStatement(fromTvSeries ? SQL_GET_ALL_TVSERIESID : SQL_GET_ALL_FILEID)) {
				ps.setLong(1, id);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						VideoMetadataLocalized metadata = new VideoMetadataLocalized();
						metadata.setHomepage(rs.getString(COL_HOMEPAGE));
						metadata.setOverview(rs.getString(COL_OVERVIEW));
						metadata.setPoster(rs.getString(COL_POSTER));
						metadata.setTagline(rs.getString(COL_TAGLINE));
						metadata.setTitle(rs.getString(COL_TITLE));
						result.put(rs.getString(COL_LANGUAGE), metadata);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", id, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static VideoMetadataLocalized getVideoMetadataLocalized(
		final Connection connection,
		final Long id,
		final boolean fromTvSeries,
		final String language,
		final String imdbId,
		final String mediaType,
		final Long tmdbId,
		final String season,
		final String episode
	) {
		if (connection == null || id == null || id < 0 || StringUtils.isBlank(language)) {
			return null;
		}
		try (PreparedStatement ps = connection.prepareStatement(fromTvSeries ? SQL_GET_ALL_LANGUAGE_TVSERIESID : SQL_GET_ALL_LANGUAGE_FILEID)) {
			ps.setString(1, language);
			ps.setLong(2, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.first()) {
					VideoMetadataLocalized result = new VideoMetadataLocalized();
					result.setHomepage(rs.getString(COL_HOMEPAGE));
					result.setOverview(rs.getString(COL_OVERVIEW));
					result.setPoster(rs.getString(COL_POSTER));
					result.setTagline(rs.getString(COL_TAGLINE));
					result.setTitle(rs.getString(COL_TITLE));
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", id, e.getMessage());
			LOGGER.trace("", e);
		}
		//here we now we do not have the language in db, let search it.
		VideoMetadataLocalized result = APIUtils.getVideoMetadataLocalizedFromImdb(language, mediaType, imdbId, tmdbId, season, episode);
		set(connection, id, fromTvSeries, result, language);
		return result;
	}
}
