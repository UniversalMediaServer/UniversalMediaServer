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
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import net.pms.external.umsapi.APIUtils;
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
	private static final int TABLE_VERSION = 5;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_LASTATTEMPT = "LASTATTEMPT";
	private static final String COL_NEXTATTEMPTINTERVALMINUTES = "NEXTATTEMPTINTERVALMINUTES";
	private static final String COL_FAILUREDETAILS = "FAILUREDETAILS";
	private static final String COL_FILENAME = "FILENAME";
	private static final String COL_VERSION = "VERSION";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_LASTATTEMPT = TABLE_NAME + "." + COL_LASTATTEMPT;
	private static final String TABLE_COL_FAILUREDETAILS = TABLE_NAME + "." + COL_FAILUREDETAILS;
	private static final String TABLE_COL_FILENAME = TABLE_NAME + "." + COL_FILENAME;
	private static final String TABLE_COL_NEXTATTEMPTINTERVALMINUTES = TABLE_NAME + "." + COL_NEXTATTEMPTINTERVALMINUTES;
	private static final String TABLE_COL_VERSION = TABLE_NAME + "." + COL_VERSION;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_LASTATTEMPT = SELECT + TABLE_COL_LASTATTEMPT + COMMA + TABLE_COL_NEXTATTEMPTINTERVALMINUTES + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_LASTATTEMPT_VERSION = SELECT + TABLE_COL_LASTATTEMPT + COMMA + TABLE_COL_NEXTATTEMPTINTERVALMINUTES + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_VERSION + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_FILENAME = SELECT + TABLE_COL_FILENAME + COMMA + TABLE_COL_FAILUREDETAILS + COMMA + TABLE_COL_VERSION + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_DELETE_FILENAME = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_FILENAME_LIKE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + LIKE_STARTING_WITH_PARAMETER;
	private static final String SQL_UPDATE_NEXTATTEMPTINTERVALMINUTES = UPDATE + TABLE_NAME + SET + TABLE_COL_NEXTATTEMPTINTERVALMINUTES + EQUAL + PARAMETER + COMMA + TABLE_COL_LASTATTEMPT + EQUAL + PARAMETER + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;

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
							statement.execute(CREATE_INDEX + IF_NOT_EXISTS + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_VERSION + ON + TABLE_NAME + " (" + COL_FILENAME + COMMA + COL_VERSION + ")");
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
				case 3 -> {
					// unused
				}
				case 4 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_NEXTATTEMPTINTERVALMINUTES + INTEGER);
				}
				default ->
					throw new IllegalStateException(
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
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                         + IDENTITY                 + PRIMARY_KEY    + COMMA +
				COL_FILENAME                   + VARCHAR_1024             + NOT_NULL       + COMMA +
				COL_FAILUREDETAILS             + VARCHAR_20000            + NOT_NULL       + COMMA +
				COL_VERSION                    + VARCHAR_1024             + NOT_NULL       + COMMA +
				COL_LASTATTEMPT                + TIMESTAMP_WITH_TIME_ZONE + DEFAULT + CURRENT_TIMESTAMP + COMMA +
				COL_NEXTATTEMPTINTERVALMINUTES + INTEGER                  + DEFAULT + "0" +
			")",
			CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER + ON + TABLE_NAME + "(" + COL_FILENAME + ")",
			CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_VERSION + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FILENAME + COMMA + COL_VERSION + ")"
		);
	}

	/**
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param isVideo whether this is a video, otherwise it's a TV series
	 * @return whether a lookup for this file has failed recently
	 */
	public static boolean hasLookupFailedRecently(final Connection connection, final String fullPathToFile, final boolean isVideo) {
		boolean incrementAfter = false;
		Integer nextStepOff = null;
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
					OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
					OffsetDateTime lastAttempt = rs.getObject(COL_LASTATTEMPT, OffsetDateTime.class);
					Integer thisAttemptIntervalMinutes = rs.getInt(COL_NEXTATTEMPTINTERVALMINUTES);

					List<Integer> stepoffSteps = new ArrayList<>();
					stepoffSteps.add(5);
					stepoffSteps.add(30);
					stepoffSteps.add(60);
					stepoffSteps.add(120);
					stepoffSteps.add(240);
					stepoffSteps.add(480);
					stepoffSteps.add(1440); // 24 hours
					stepoffSteps.add(2880); // 48 hours
					stepoffSteps.add(4320); // 3 days
					stepoffSteps.add(5760); // 4 days
					stepoffSteps.add(10080); // 7 days

					if (thisAttemptIntervalMinutes == null || thisAttemptIntervalMinutes == 0) {
						nextStepOff = stepoffSteps.getFirst();
					} else if (stepoffSteps.getLast() == thisAttemptIntervalMinutes) {
						nextStepOff = stepoffSteps.getLast();
					} else {
						int nextIndex = stepoffSteps.indexOf(thisAttemptIntervalMinutes) + 1;
						nextStepOff = stepoffSteps.get(nextIndex);
					}
					// do gradual stepoff
					if (lastAttempt.plusMinutes(nextStepOff).isAfter(now)) {
						// The last attempt happened inside of the next stepoff time
						return true;
					} else {
						// The last attempt happened outside of the next stepoff time, let's remove it so it can be tried again
						incrementAfter = true;
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
			if (incrementAfter) {
				setAttemptInterval(connection, fullPathToFile, nextStepOff);
			}
		}

		return false;
	}

	/**
	 * Sets a new row.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param failureDetails the response the API server returned, or a
	 * client-side message
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
						updateString(result, COL_FAILUREDETAILS, failureDetails, 20000);
						updateString(result, COL_VERSION, latestVersion, 1024);
						result.updateRow();
					} else {
						result.moveToInsertRow();
						updateString(result, COL_FILENAME, fullPathToFile, 1024);
						updateString(result, COL_FAILUREDETAILS, failureDetails, 20000);
						updateString(result, COL_VERSION, latestVersion, 1024);
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
	 *
	 * If {@code useLike} is {@code true} {@code filename} must be properly
	 * escaped.
	 *
	 * @param connection the db connection
	 *
	 * @param filename the filename to remove
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 * operator, {@code false} if {@code =} should be used.
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

	/**
	 * Sets a new last attempt interval for an entry or entries based on filename.
	 *
	 * If {@code useLike} is {@code true} {@code filename} must be properly
	 * escaped.
	 *
	 * @param connection the db connection
	 * @param filename the filename to remove
	 */
	public static void setAttemptInterval(final Connection connection, final String filename, final Integer nextStepOff) {
		try {
			OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
			String sql = SQL_UPDATE_NEXTATTEMPTINTERVALMINUTES;
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setInt(1, nextStepOff);
				statement.setObject(2, now, JDBCType.TIMESTAMP_WITH_TIMEZONE);
				statement.setString(3, filename);
				statement.executeUpdate();
			}
		} catch (SQLException e) {
			LOGGER.error(
				LOG_ERROR_WHILE_IN_FOR,
				DATABASE_NAME,
				"incrementing stepoff",
				TABLE_NAME,
				filename,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}
}
