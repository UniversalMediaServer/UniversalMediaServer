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

import com.google.gson.JsonArray;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.pms.media.metadata.ApiStringArray;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableVideoMetadataDirectors extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadataDirectors.class);
	public static final String TABLE_NAME = "VIDEO_METADATA_DIRECTORS";
	private static final String COL_ID = "ID";
	private static final String COL_FILEID = "FILEID";
	private static final String COL_TVSERIESID = MediaTableTVSeries.CHILD_ID;
	private static final String COL_DIRECTOR = "DIRECTOR";
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	private static final String TABLE_COL_TVSERIESID = TABLE_NAME + "." + COL_TVSERIESID;
	public static final String TABLE_COL_DIRECTOR = TABLE_NAME + "." + COL_DIRECTOR;
	private static final String SQL_GET_DIRECTOR_FILEID = "SELECT " + TABLE_COL_DIRECTOR + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ?";
	private static final String SQL_GET_DIRECTOR_TVSERIESID = "SELECT " + TABLE_COL_DIRECTOR + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_TVSERIESID + " = ?";
	private static final String SQL_GET_TVSERIESID_EXISTS = "SELECT " + COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_TVSERIESID + " = ? AND " + TABLE_COL_DIRECTOR + " = ? LIMIT 1";
	private static final String SQL_GET_FILEID_EXISTS = "SELECT " + COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ? AND " + TABLE_COL_DIRECTOR + " = ? LIMIT 1";
	private static final String SQL_INSERT_TVSERIESID = "INSERT INTO " + TABLE_NAME + " (" + COL_TVSERIESID + ", " + COL_DIRECTOR + ") VALUES (?, ?)";
	private static final String SQL_INSERT_FILEID = "INSERT INTO " + TABLE_NAME + " (" + COL_FILEID + ", " + COL_DIRECTOR + ") VALUES (?, ?)";

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
					executeUpdate(connection, "DROP INDEX IF EXISTS FILENAME_DIRECTOR_TVSERIESID_IDX");
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
				COL_DIRECTOR + "    VARCHAR(1024)       NOT NULL    , " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY (" + COL_FILEID + ") REFERENCES " + MediaTableVideoMetadata.REFERENCE_TABLE_COL_FILE_ID + " ON DELETE CASCADE, " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_TVSERIESID + "_FK FOREIGN KEY (" + COL_TVSERIESID + ") REFERENCES " + MediaTableTVSeries.REFERENCE_TABLE_COL_ID + " ON DELETE CASCADE " +
			")"
		);
	}

	/**
	 * Sets a new row if it doesn't already exist.
	 *
	 * @param connection the db connection
	 * @param fileId
	 * @param directors
	 * @param tvSeriesID
	 */
	public static void set(final Connection connection, final Long fileId, final ApiStringArray directors, final Long tvSeriesID) {
		if (directors == null || !directors.isEmpty()) {
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

		try {
			for (String director : directors) {
				try (PreparedStatement ps = connection.prepareStatement(sqlSelect)) {
					ps.setInt(1, id);
					ps.setString(2, StringUtils.left(director, 1024));
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							LOGGER.trace("Record already exists {} {} {}", tvSeriesID, fileId, director);
						} else {
							try (PreparedStatement insertStatement = connection.prepareStatement(sqlInsert, Statement.RETURN_GENERATED_KEYS)) {
								insertStatement.clearParameters();
								insertStatement.setLong(1, id);
								insertStatement.setString(2, StringUtils.left(director, 1024));

								insertStatement.executeUpdate();
								try (ResultSet rs2 = insertStatement.getGeneratedKeys()) {
									if (rs2.next()) {
										LOGGER.trace("Set new entry successfully in " + TABLE_NAME + " with \"{}\", \"{}\" and \"{}\"", fileId, tvSeriesID, director);
									}
								}
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

	public static ApiStringArray getDirectorsForFile(final Connection connection, final long fileId) {
		ApiStringArray result = new ApiStringArray();
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_DIRECTOR_FILEID)) {
				ps.setLong(1, fileId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						result.add(rs.getString(1));
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static JsonArray getJsonArrayForFile(final Connection connection, final long fileId) {
		JsonArray result = new JsonArray();
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_DIRECTOR_FILEID)) {
				ps.setLong(1, fileId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						result.add(rs.getString(1));
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static ApiStringArray getDirectorsForTvSerie(final Connection connection, final long tvSerieId) {
		ApiStringArray result = new ApiStringArray();
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_DIRECTOR_TVSERIESID)) {
				ps.setLong(1, tvSerieId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						result.add(rs.getString(1));
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for TV serie ID \"{}\": {}", tvSerieId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static JsonArray getJsonArrayForTvSerie(final Connection connection, final long tvSerieId) {
		JsonArray result = new JsonArray();
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_DIRECTOR_TVSERIESID)) {
				ps.setLong(1, tvSerieId);
				try (ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						result.add(rs.getString(1));
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tvSerieId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

}
