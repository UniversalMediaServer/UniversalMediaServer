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
import java.sql.Statement;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableVideoMetadataPosters extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadataPosters.class);
	public static final String TABLE_NAME = "VIDEO_METADATA_POSTERS";
	private static final String COL_ID = "ID";
	private static final String COL_FILEID = "FILEID";
	private static final String COL_TVSERIESID = MediaTableTVSeries.CHILD_ID;
	private static final String COL_POSTER = "POSTER";
	private static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	private static final String TABLE_COL_TVSERIESID = TABLE_NAME + "." + COL_TVSERIESID;
	private static final String TABLE_COL_POSTER = TABLE_NAME + "." + COL_POSTER;

	public static final String SQL_LEFT_JOIN_TABLE_TV_SERIES = "LEFT JOIN " + MediaTableTVSeries.TABLE_NAME + " ON " + TABLE_COL_TVSERIESID + " = " + MediaTableTVSeries.TABLE_COL_ID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_FILES = "LEFT JOIN " + MediaTableFiles.TABLE_NAME + " ON " + TABLE_COL_FILEID + " = " + MediaTableFiles.TABLE_COL_ID + " ";

	private static final String SQL_GET_POSTER_FILEID = "SELECT " + TABLE_COL_POSTER + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ? LIMIT 1";
	private static final String SQL_GET_POSTER_TVSERIESID = "SELECT " + TABLE_COL_POSTER + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_TVSERIESID + " = ? LIMIT 1";
	private static final String SQL_GET_TVSERIESID_EXISTS = "SELECT " + COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_TVSERIESID + " = ? AND " + TABLE_COL_POSTER + " = ? LIMIT 1";
	private static final String SQL_GET_FILEID_EXISTS = "SELECT " + COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ? AND " + TABLE_COL_POSTER + " = ? LIMIT 1";
	private static final String SQL_INSERT_TVSERIESID = "INSERT INTO " + TABLE_NAME + " (" + COL_TVSERIESID + ", " + COL_POSTER + ") VALUES (?, ?)";
	private static final String SQL_INSERT_FILEID = "INSERT INTO " + TABLE_NAME + " (" + COL_FILEID + ", " + COL_POSTER + ") VALUES (?, ?)";
	private static final String SQL_GET_POSTER_TV_SERIES_TITLE = "SELECT " + TABLE_COL_POSTER + ", " + TABLE_COL_TVSERIESID + " FROM " + TABLE_NAME + " " + SQL_LEFT_JOIN_TABLE_TV_SERIES + "WHERE " + MediaTableTVSeries.TABLE_COL_TITLE + " = ? LIMIT 1";
	private static final String SQL_GET_POSTER_FILE_FILENAME = "SELECT " + TABLE_COL_POSTER + " FROM " + TABLE_NAME + " " + SQL_LEFT_JOIN_TABLE_FILES + "WHERE " + MediaTableFiles.TABLE_COL_FILENAME + " = ? LIMIT 1";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

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
					//index with all columns ??
					executeUpdate(connection, "DROP INDEX IF EXISTS FILENAME_POSTER_TVSERIESID_IDX");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS " + COL_FILEID + " INTEGER");
					if (isColumnExist(connection, TABLE_NAME, "FILENAME")) {
						executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET " + COL_FILEID + "=(SELECT " + MediaTableFiles.TABLE_COL_ID + " FROM " + MediaTableFiles.TABLE_NAME + " WHERE " + MediaTableFiles.TABLE_COL_FILENAME + " = " + TABLE_NAME + ".FILENAME) WHERE " + TABLE_NAME + ".FILENAME != ''");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN IF EXISTS FILENAME");
					}
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN IF EXISTS " + COL_TVSERIESID + " DROP DEFAULT");

					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET " + COL_FILEID + " = NULL WHERE " + TABLE_COL_FILEID + " = -1");
					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET " + COL_TVSERIESID + " = NULL WHERE " + TABLE_COL_TVSERIESID + " = -1");

					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY (" + COL_FILEID + ") REFERENCES " + MediaTableVideoMetadata.TABLE_NAME + "(" + MediaTableVideoMetadata.COL_FILEID + ") ON DELETE CASCADE");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT " + TABLE_NAME + "_" + COL_TVSERIESID + "_FK FOREIGN KEY (" + COL_TVSERIESID + ") REFERENCES " + MediaTableTVSeries.TABLE_NAME + "(" + MediaTableTVSeries.COL_ID + ") ON DELETE CASCADE");
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
				COL_ID + "          IDENTITY            PRIMARY KEY , " +
				COL_TVSERIESID + "  INTEGER                         , " +
				COL_FILEID + "      INTEGER                         , " +
				COL_POSTER + "      VARCHAR(1024)       NOT NULL    , " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY (" + COL_FILEID + ") REFERENCES " + MediaTableVideoMetadata.REFERENCE_TABLE_COL_FILE_ID + " ON DELETE CASCADE, " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_TVSERIESID + "_FK FOREIGN KEY (" + COL_TVSERIESID + ") REFERENCES " + MediaTableTVSeries.REFERENCE_TABLE_COL_ID + " ON DELETE CASCADE " +
			")"
		);
	}

	/**
	 * @param connection the db connection
	 * @param tvSeriesTitle
	 * @return the poster for the TV series, or null.
	 */
	public static Object[] getByTVSeriesName(final Connection connection, final String tvSeriesTitle) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_POSTER_TV_SERIES_TITLE)) {
				ps.setString(1, tvSeriesTitle);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", ps);
				}
				try (ResultSet resultSet = ps.executeQuery()) {
					if (resultSet.next()) {
						return new Object[] {
							resultSet.getString(1),
							resultSet.getLong(2)
						};
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "reading poster", TABLE_NAME, tvSeriesTitle, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * @param connection the db connection
	 * @param filename
	 * @return the poster for the file, or null.
	 */
	public static String getByFilename(final Connection connection, final String filename) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_POSTER_FILE_FILENAME)) {
				ps.setString(1, filename);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", ps);
				}
				try (ResultSet resultSet = ps.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getString(1);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "reading poster", TABLE_NAME, filename, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Sets a new row if it doesn't already exist.
	 *
	 * @param connection the db connection
	 * @param fileId
	 * @param poster
	 * @param tvSeriesID
	 */
	public static void set(final Connection connection, final Long fileId, final String poster, final Long tvSeriesID) {
		if (StringUtils.isBlank(poster)) {
			return;
		}
		final String sqlSelect;
		final String sqlInsert;
		final int id;
		if (tvSeriesID != null) {
			sqlSelect = SQL_GET_TVSERIESID_EXISTS;
			sqlInsert = SQL_INSERT_TVSERIESID;
			id = tvSeriesID.intValue();
		} else if (fileId != null) {
			sqlSelect = SQL_GET_FILEID_EXISTS;
			sqlInsert = SQL_INSERT_FILEID;
			id = fileId.intValue();
		} else {
			return;
		}

		try (PreparedStatement ps = connection.prepareStatement(sqlSelect)) {
			ps.setInt(1, id);
			ps.setString(2, StringUtils.left(poster, 1024));
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					LOGGER.trace("Record already exists {} {} {}", tvSeriesID, fileId, poster);
				} else {
					try (PreparedStatement insertStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
						insertStatement.clearParameters();
						insertStatement.setInt(1, id);
						insertStatement.setString(2, StringUtils.left(poster, 1024));

						insertStatement.executeUpdate();
						try (ResultSet rs2 = insertStatement.getGeneratedKeys()) {
							if (rs2.next()) {
								LOGGER.trace("Set new entry successfully in " + TABLE_NAME + " with \"{}\", \"{}\" and \"{}\"", fileId, tvSeriesID, poster);
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing", TABLE_NAME, fileId, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static String getValueForFile(final Connection connection, final Long fileId) {
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_POSTER_FILEID)) {
				ps.setLong(1, fileId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString(1);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	public static String getValueForTvSerie(final Connection connection, final Long tvSerieId) {
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_POSTER_TVSERIESID)) {
				ps.setLong(1, tvSerieId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString(1);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tvSerieId, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

}
