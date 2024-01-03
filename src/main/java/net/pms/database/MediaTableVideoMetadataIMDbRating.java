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

public final class MediaTableVideoMetadataIMDbRating extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadataIMDbRating.class);
	public static final String TABLE_NAME = "VIDEO_METADATA_IMDB_RATING";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_FILEID = "FILEID";
	private static final String COL_TVSERIESID = MediaTableTVSeries.CHILD_ID;
	private static final String COL_IMDBRATING = "IMDBRATING";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_TVSERIESID = TABLE_NAME + "." + COL_TVSERIESID;
	public static final String TABLE_COL_IMDBRATING = TABLE_NAME + "." + COL_IMDBRATING;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_IMDBRATING_FILEID = SELECT + TABLE_COL_IMDBRATING + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_IMDBRATING_TVSERIESID = SELECT + TABLE_COL_IMDBRATING + FROM + TABLE_NAME + WHERE + TABLE_COL_TVSERIESID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_TVSERIESID_EXISTS = SELECT + COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_TVSERIESID + EQUAL + PARAMETER + AND + TABLE_COL_IMDBRATING + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_FILEID_EXISTS = SELECT + COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + TABLE_COL_IMDBRATING + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_INSERT_TVSERIESID = INSERT_INTO + TABLE_NAME + " (" + COL_TVSERIESID + COMMA + COL_IMDBRATING + ") VALUES (" + PARAMETER + COMMA + PARAMETER + ")";
	private static final String SQL_INSERT_FILEID = INSERT_INTO + TABLE_NAME + " (" + COL_FILEID + COMMA + COL_IMDBRATING + ") VALUES (" + PARAMETER + COMMA + PARAMETER + ")";

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
					executeUpdate(connection, "DROP INDEX IF EXISTS FILENAME_IMDBRATING_TVSERIESID_IDX");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " ADD COLUMN IF NOT EXISTS " + COL_FILEID + " INTEGER");
					if (isColumnExist(connection, TABLE_NAME, "FILENAME")) {
						executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_FILEID + "=(SELECT " + MediaTableFiles.TABLE_COL_ID + FROM + MediaTableFiles.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_FILENAME + " = " + TABLE_NAME + ".FILENAME) WHERE " + TABLE_NAME + ".FILENAME != ''");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " DROP COLUMN IF EXISTS FILENAME");
					}
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " ALTER COLUMN IF EXISTS " + COL_TVSERIESID + " DROP DEFAULT");

					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_TVSERIESID + " = NULL WHERE " + TABLE_COL_TVSERIESID + " = -1");
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_FILEID + " = NULL WHERE " + TABLE_COL_TVSERIESID + " IS NOT NULL");
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_FILEID + " = NULL WHERE " + TABLE_COL_FILEID + " = -1");
					//api settvseriesinfo had both filled filename and tvserieId ending in multiple row for nothing.
					executeUpdate(connection,
					"DELETE FROM " + TABLE_NAME + " WHERE EXISTS (SELECT " + COL_ID + FROM + TABLE_NAME + " T2 WHERE T2." + COL_TVSERIESID + " = " + TABLE_COL_TVSERIESID + " AND " +
					"T2." + COL_FILEID + " IS NULL AND T2." + COL_ID + " != " + TABLE_NAME + "." + COL_ID + " AND T2." + COL_IMDBRATING + " = " + TABLE_COL_IMDBRATING + " AND " +
					TABLE_NAME + "." + COL_ID + " != (SELECT MIN(T3." + COL_ID + ") FROM " + TABLE_NAME + " T3 WHERE T3." + COL_TVSERIESID + " = T2." + COL_TVSERIESID + "))");

					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " ADD CONSTRAINT " + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableVideoMetadata.TABLE_NAME + "(" + MediaTableVideoMetadata.COL_FILEID + ") ON DELETE CASCADE");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " ADD CONSTRAINT " + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TVSERIESID + FK_MARKER + FOREIGN_KEY + "(" + COL_TVSERIESID + ")" + REFERENCES + MediaTableTVSeries.TABLE_NAME + "(" + MediaTableTVSeries.COL_ID + ") ON DELETE CASCADE");
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
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID            + IDENTITY          + PRIMARY_KEY + COMMA +
				COL_TVSERIESID    + INTEGER                         + COMMA +
				COL_FILEID        + INTEGER                         + COMMA +
				COL_IMDBRATING    + VARCHAR_1024      + NOT_NULL    + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableVideoMetadata.REFERENCE_TABLE_COL_FILE_ID + ON_DELETE_CASCADE + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TVSERIESID + FK_MARKER + FOREIGN_KEY + "(" + COL_TVSERIESID + ")" + REFERENCES + MediaTableTVSeries.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")"
		);
	}

	/**
	 * Sets a new row if it doesn't already exist.
	 *
	 * @param connection the db connection
	 * @param fileId
	 * @param imdbRating
	 * @param tvSeriesID
	 */
	public static void set(final Connection connection, final Long fileId, final String imdbRating, final Long tvSeriesID) {
		if (StringUtils.isBlank(imdbRating)) {
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
			ps.setString(2, StringUtils.left(imdbRating, 1024));
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					LOGGER.trace("Record already exists {} {} {}", tvSeriesID, fileId, imdbRating);
				} else {
					try (PreparedStatement insertStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
						insertStatement.clearParameters();
						insertStatement.setInt(1, id);
						insertStatement.setString(2, StringUtils.left(imdbRating, 1024));

						insertStatement.executeUpdate();
						try (ResultSet rs2 = insertStatement.getGeneratedKeys()) {
							if (rs2.next()) {
								LOGGER.trace("Set new entry successfully in " + TABLE_NAME + " with \"{}\", \"{}\" and \"{}\"", fileId, tvSeriesID, imdbRating);
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
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_IMDBRATING_FILEID)) {
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
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_IMDBRATING_TVSERIESID)) {
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
