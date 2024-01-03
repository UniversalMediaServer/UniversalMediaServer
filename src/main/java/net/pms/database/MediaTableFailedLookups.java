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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import net.pms.util.APIUtils;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableFailedLookups extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableFailedLookups.class);
	protected static final String TABLE_NAME = "FAILED_LOOKUPS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 3;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_LASTATTEMPT = "LASTATTEMPT";
	private static final String COL_FAILUREDETAILS = "FAILUREDETAILS";
	private static final String COL_FILENAME = "FILENAME";
	private static final String COL_VERSION = "VERSION";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_LASTATTEMPT = TABLE_NAME + "." + COL_LASTATTEMPT;
	private static final String TABLE_COL_FAILUREDETAILS = TABLE_NAME + "." + COL_FAILUREDETAILS;
	private static final String TABLE_COL_FILENAME = TABLE_NAME + "." + COL_FILENAME;
	private static final String TABLE_COL_VERSION = TABLE_NAME + "." + COL_VERSION;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_LASTATTEMPT = SELECT + TABLE_COL_LASTATTEMPT + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_LASTATTEMPT_VERSION = SELECT + TABLE_COL_LASTATTEMPT + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_VERSION + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_FILENAME = SELECT + TABLE_COL_FILENAME + COMMA + TABLE_COL_FAILUREDETAILS + COMMA + TABLE_COL_VERSION + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_DELETE_FILENAME = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_FILENAME_LIKE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + PARAMETER;

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
					LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB,
						DATABASE_NAME,
						TABLE_NAME,
						DATABASE.getDatabaseFilename()
					);
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
					try (Statement statement = connection.createStatement()) {
						if (!isColumnExist(connection, TABLE_NAME, COL_VERSION)) {
							statement.execute(ALTER_TABLE + TABLE_NAME + ADD + COL_VERSION + VARCHAR);
							statement.execute(CREATE_INDEX + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_VERSION + ON + TABLE_NAME + " (" + COL_FILENAME + COMMA + COL_VERSION + ")");
						}
					} catch (SQLException e) {
						LOGGER.error(LOG_UPGRADING_TABLE_FAILED, DATABASE_NAME, TABLE_NAME, e.getMessage());
						LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
						throw new SQLException(e);
					}
				}
				case 2 -> {
					//rename indexes
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "FAILED_" + COL_FILENAME + IDX_MARKER + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_VERSION + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_VERSION + IDX_MARKER);
				}
				default -> throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}

		try {
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} catch (SQLException e) {
			LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                 + IDENTITY                   + PRIMARY_KEY                  + COMMA +
				COL_FILENAME           + VARCHAR_1024               + NOT_NULL                     + COMMA +
				COL_FAILUREDETAILS     + VARCHAR_20000              + NOT_NULL                     + COMMA +
				COL_VERSION            + VARCHAR_1024               + NOT_NULL                     + COMMA +
				COL_LASTATTEMPT        + TIMESTAMP_WITH_TIME_ZONE   + DEFAULT + CURRENT_TIMESTAMP  +
			")",
			CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER + ON + TABLE_NAME + "(" + COL_FILENAME + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_VERSION + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FILENAME + COMMA + COL_VERSION + ")"
		);
	}

	/**
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param isVideo whether this is a video, otherwise it's a TV series
	 * @return whether a lookup for this file has failed recently
	 */
	public static boolean hasLookupFailedRecently(final Connection connection, final String fullPathToFile, final boolean isVideo) {
		boolean removeAfter = false;
		String latestVersion = null;
		if (CONFIGURATION.getExternalNetwork()) {
			if (isVideo) {
				latestVersion = APIUtils.getApiDataVideoVersion();
			} else {
				latestVersion = APIUtils.getApiDataSeriesVersion();
			}
		}
		String sql;
		if (latestVersion != null) {
			sql = SQL_GET_LASTATTEMPT_VERSION;
		} else {
			sql = SQL_GET_LASTATTEMPT;
		}

		try (PreparedStatement selectStatement = connection.prepareStatement(sql)) {
			selectStatement.setString(1, fullPathToFile);
			if (latestVersion != null) {
				selectStatement.setString(2, latestVersion);
			}
			try (ResultSet rs = selectStatement.executeQuery()) {
				if (rs.next()) {
					LOGGER.trace("We have failed a lookup for {} so let's see if it was recent", fullPathToFile);

					OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
					OffsetDateTime lastAttempt = rs.getObject(COL_LASTATTEMPT, OffsetDateTime.class);
					if (lastAttempt.plusWeeks(1).isAfter(now)) {
						// The last attempt happened in the last week
						return true;
					} else {
						// The last attempt happened over a week ago, let's remove it so it can be tried again
						removeAfter = true;
						return false;
					}
				} else {
					LOGGER.trace("We have no failed lookups stored for {}", fullPathToFile);
					return false;
				}
			}
		} catch (Exception e) {
			LOGGER.error(
				LOG_ERROR_WHILE_IN_FOR,
				DATABASE_NAME,
				"writing",
				TABLE_NAME,
				fullPathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		} finally {
			if (removeAfter) {
				remove(connection, fullPathToFile, false);
			}
		}

		return false;
	}

	/**
	 * Sets a new row.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param failureDetails the response the API server returned, or a client-side message
	 * @param isVideo
	 */
	public static void set(final Connection connection, final String fullPathToFile, final String failureDetails, final boolean isVideo) {
		String latestVersion;
		if (isVideo) {
			latestVersion = APIUtils.getApiDataVideoVersion();
		} else {
			latestVersion = APIUtils.getApiDataSeriesVersion();
		}

		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_FILENAME, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
				LOGGER.trace("Searching for file/series in " + TABLE_NAME + " with \"{}\" before update", statement);
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						result.updateString(COL_FAILUREDETAILS, left(failureDetails, 20000));
						result.updateString(COL_VERSION, left(latestVersion, 1024));
						result.updateRow();
					} else {
						result.moveToInsertRow();
						result.updateString(COL_FILENAME, left(fullPathToFile, 1024));
						result.updateString(COL_FAILUREDETAILS, left(failureDetails, 20000));
						result.updateString(COL_VERSION, left(latestVersion, 1024));
						result.insertRow();
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		} catch (Exception e) {
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes an entry or entries based on its FILENAME.
	 * If {@code useLike} is {@code true} {@code filename} must be properly escaped.
	 *
	 * @param connection the db connection
	 * @see Tables#sqlLikeEscape(String)
	 *
	 * @param filename the filename to remove
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 */
	public static void remove(final Connection connection, final String filename, boolean useLike) {
		try {
			String sql = useLike ? SQL_DELETE_FILENAME_LIKE : SQL_DELETE_FILENAME;
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, filename);
				int rows = statement.executeUpdate();
				LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for filename \"{}\"", rows, filename);
			}
		} catch (SQLException e) {
			LOGGER.error(
				LOG_ERROR_WHILE_IN_FOR,
				DATABASE_NAME,
				"removing entries",
				TABLE_NAME,
				filename,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

}
