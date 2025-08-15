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
import net.pms.external.tmdb.TMDB;
import net.pms.media.video.metadata.ApiSeason;
import net.pms.media.video.metadata.TvSeasonMetadataLocalized;
import net.pms.media.video.metadata.VideoMetadataLocalized;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableTvSeasonMetadataLocalized extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableTvSeasonMetadataLocalized.class);
	public static final String TABLE_NAME = "TV_SEASON_METADATA_LOCALIZED";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 *
	 * Version notes:
	 * - 1: creation
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_LANGUAGE = "LANGUAGE";
	private static final String COL_ID = "ID";
	private static final String COL_TVSERIESID = MediaTableTVSeries.CHILD_ID;
	private static final String COL_TVSEASON = "TVSEASON";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_OVERVIEW = "OVERVIEW";
	private static final String COL_POSTER = "POSTER";
	private static final String COL_TITLE = "TITLE";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_LANGUAGE = TABLE_NAME + "." + COL_LANGUAGE;
	private static final String TABLE_COL_TVSERIESID = TABLE_NAME + "." + COL_TVSERIESID;
	private static final String TABLE_COL_TVSEASON = TABLE_NAME + "." + COL_TVSEASON;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_TVSERIESID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_TVSERIESID + EQUAL + PARAMETER + AND + TABLE_COL_TVSEASON + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_LANGUAGE_TVSERIESID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_LANGUAGE + EQUAL + PARAMETER + AND + TABLE_COL_TVSERIESID + EQUAL + PARAMETER + AND + TABLE_COL_TVSEASON + EQUAL + PARAMETER;
	private static final String SQL_DELETE_TVSERIESID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_TVSERIESID + EQUAL + PARAMETER;

	/**
	 * Database column sizes
	 */
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
				default -> {
					throw new IllegalStateException(getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION));
				}
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID            + IDENTITY          + PRIMARY_KEY + COMMA +
				COL_LANGUAGE      + VARCHAR_5         + NOT_NULL    + COMMA +
				COL_TVSERIESID    + BIGINT            + NOT_NULL    + COMMA +
				COL_TVSEASON      + INTEGER           + NOT_NULL    + COMMA +
				COL_MODIFIED      + BIGINT                          + COMMA +
				COL_OVERVIEW      + CLOB                            + COMMA +
				COL_POSTER        + VARCHAR                         + COMMA +
				COL_TITLE         + VARCHAR                         + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TVSERIESID + FK_MARKER + FOREIGN_KEY + "(" + COL_TVSERIESID + ")" + REFERENCES + MediaTableTVSeries.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")",
			CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_LANGUAGE + CONSTRAINT_SEPARATOR + COL_TVSERIESID + CONSTRAINT_SEPARATOR + COL_TVSEASON + IDX_MARKER + ON + TABLE_NAME + "(" + COL_LANGUAGE + COMMA + COL_TVSERIESID + COMMA + COL_TVSEASON + ")"
		);
	}

	private static void set(final Connection connection, final Long tvSeriesId, final Integer seasonNumber, final TvSeasonMetadataLocalized metadata, final String language) {
		if (tvSeriesId == null || tvSeriesId < 0 || seasonNumber == null || StringUtils.isBlank(language)) {
			return;
		}
		try (PreparedStatement ps = connection.prepareStatement(
					SQL_GET_ALL_LANGUAGE_TVSERIESID,
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_UPDATABLE)
		) {
			ps.setString(1, language);
			ps.setLong(2, tvSeriesId);
			ps.setInt(3, seasonNumber);
			try (ResultSet rs = ps.executeQuery()) {
				boolean isCreatingNewRecord = !rs.next();
				if (isCreatingNewRecord) {
					rs.moveToInsertRow();
					rs.updateString(COL_LANGUAGE, StringUtils.left(language, SIZE_LANGUAGE));
					rs.updateLong(COL_TVSERIESID, tvSeriesId);
					rs.updateInt(COL_TVSEASON, seasonNumber);
				}
				rs.updateLong(COL_MODIFIED, System.currentTimeMillis());
				rs.updateString(COL_OVERVIEW, metadata == null ? null : metadata.getOverview());
				rs.updateString(COL_POSTER, metadata == null ? null : metadata.getPoster());
				rs.updateString(COL_TITLE, metadata == null ? null : metadata.getName());
				if (isCreatingNewRecord) {
					rs.insertRow();
				} else {
					rs.updateRow();
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing", TABLE_NAME, tvSeriesId, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static Map<String, TvSeasonMetadataLocalized> getAllTvSeasonMetadataLocalized(final Connection connection, final Long tvSeriesId, final Integer seasonNumber) {
		Map<String, TvSeasonMetadataLocalized> result = new HashMap<>();
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_ALL_TVSERIESID)) {
				ps.setLong(1, tvSeriesId);
				ps.setInt(2, seasonNumber);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						TvSeasonMetadataLocalized metadata = new TvSeasonMetadataLocalized();
						metadata.setOverview(rs.getString(COL_OVERVIEW));
						metadata.setPoster(rs.getString(COL_POSTER));
						metadata.setName(rs.getString(COL_TITLE));
						result.put(rs.getString(COL_LANGUAGE), metadata);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tvSeriesId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static TvSeasonMetadataLocalized getTvSeasonMetadataLocalized(
		final Long tvSeriesId,
		final String language,
		final Long tmdbId,
		final ApiSeason apiSeason
	) {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			if (connection != null) {
				return getTvSeasonMetadataLocalized(connection, tvSeriesId, language, tmdbId, apiSeason);
			}
		} catch (Exception e) {
			LOGGER.error("Error while getting metadata for web interface");
			LOGGER.debug("", e);
		}
		return null;
	}

	private static TvSeasonMetadataLocalized getTvSeasonMetadataLocalized(
		final Connection connection,
		final Long tvSeriesId,
		final String language,
		final Long tmdbId,
		final ApiSeason apiSeason
	) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0 || apiSeason == null || StringUtils.isBlank(language)) {
			return null;
		}
		try (PreparedStatement ps = connection.prepareStatement(SQL_GET_ALL_LANGUAGE_TVSERIESID)) {
			ps.setString(1, language);
			ps.setLong(2, tvSeriesId);
			ps.setInt(3, apiSeason.getSeasonNumber());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.first()) {
					TvSeasonMetadataLocalized result = new TvSeasonMetadataLocalized();
					result.setOverview(rs.getString(COL_OVERVIEW));
					result.setPoster(rs.getString(COL_POSTER));
					result.setName(rs.getString(COL_TITLE));
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tvSeriesId, e.getMessage());
			LOGGER.trace("", e);
		}
		//here we now we do not have the language in db, let search it.
		LOGGER.trace("Looking for localized metadata for TVSeries \"{}\": saison {}", tvSeriesId, apiSeason.getSeasonNumber());
		TvSeasonMetadataLocalized result = new TvSeasonMetadataLocalized();
		VideoMetadataLocalized loc = TMDB.getVideoMetadataLocalized(language, "tv_season", null, tmdbId, apiSeason.getSeasonNumber(), null);
		//remove not translated fields from base data
		if (loc != null) {
			if (loc.getOverview() != null && !loc.getOverview().equals(apiSeason.getOverview())) {
				result.setOverview(loc.getOverview());
			}
			if (loc.getTitle() != null && !loc.getTitle().equals(apiSeason.getName())) {
				result.setName(loc.getTitle());
			}
			result.setPoster(loc.getPoster());
		}
		set(connection, tvSeriesId, apiSeason.getSeasonNumber(), result, language);
		return result;
	}

	public static void clearTvSeasonMetadataLocalized(final Connection connection, final Long tvSeriesId) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return;
		}
		try (PreparedStatement ps = connection.prepareStatement(SQL_DELETE_TVSERIESID)) {
			ps.setLong(1, tvSeriesId);
			ps.execute();
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for deleting \"{}\": {}", tvSeriesId, e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
