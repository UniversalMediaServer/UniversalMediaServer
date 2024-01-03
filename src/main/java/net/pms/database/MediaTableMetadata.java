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
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaTableMetadata extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableMetadata.class);
	public static final String TABLE_NAME = "METADATA";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 3;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_M_KEY = "M_KEY";
	private static final String COL_M_VALUE = "M_VALUE";

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL = SELECT_ALL + FROM + TABLE_NAME + WHERE + COL_M_KEY + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_M_VALUE = SELECT + COL_M_VALUE + FROM + TABLE_NAME + WHERE + COL_M_KEY + EQUAL + PARAMETER + LIMIT_1;

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
			if (version == null) {
				version = 1;
			}
			if (version < TABLE_VERSION) {
				upgradeTable(connection, version);
			} else if (version > TABLE_VERSION) {
				LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		try {
			for (int version = currentVersion; version < TABLE_VERSION; version++) {
				LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
				switch (version) {
					case 1 -> {
						// From version 1 to 2, we stopped using KEY and VALUE, and instead use M_KEY and M_VALUE
						executeUpdate(connection, "DROP INDEX IF EXISTS IDX_KEY");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "`KEY`" + RENAME_TO + COL_M_KEY);
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "`VALUE`" + RENAME_TO + COL_M_VALUE);
						executeUpdate(connection, CREATE_UNIQUE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_M_KEY + IDX_MARKER + ON + TABLE_NAME + "(" + COL_M_KEY + ")");
					}
					case 2 -> {
						executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_" + COL_M_KEY + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_M_KEY + IDX_MARKER);
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
		} catch (SQLException e) {
			LOGGER.error(LOG_UPGRADING_TABLE_FAILED, DATABASE_NAME, TABLE_NAME, e.getMessage());
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_M_KEY       + VARCHAR_SIZE_MAX      + NOT_NULL  + COMMA +
				COL_M_VALUE     + VARCHAR_SIZE_MAX      + NOT_NULL  +
			")",
			CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_M_KEY + IDX_MARKER + ON + TABLE_NAME + "(" + COL_M_KEY + ")"
		);
	}

	/**
	 * Gets a value from the METADATA table
	 * @param connection the db connection
	 * @param key
	 * @return value
	 */
	public static String getMetadataValue(final Connection connection, String key) {
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_M_VALUE)) {
			statement.setString(1, key);
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					return rs.getString(1);
				}
			}
		} catch (Exception se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading value", key, TABLE_NAME, se.getMessage());
			LOGGER.error(null, se);
		}
		return null;
	}

	/**
	 * Sets or updates a value in the METADATA table.
	 *
	 * @param connection the db connection
	 * @param key
	 * @param value
	 */
	public static void setOrUpdateMetadataValue(final Connection connection, final String key, final String value) {
		boolean trace = LOGGER.isTraceEnabled();

		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setString(1, key);
			if (trace) {
				LOGGER.trace("Searching for value in METADATA with \"{}\" before update", statement);
			}

			try (ResultSet result = statement.executeQuery()) {
				boolean isCreatingNewRecord = !result.next();

				if (isCreatingNewRecord) {
					result.moveToInsertRow();
				}

				result.updateString(COL_M_KEY, key);
				result.updateString(COL_M_VALUE, value);

				if (isCreatingNewRecord) {
					result.insertRow();
				} else {
					result.updateRow();
				}
			} catch (JdbcSQLIntegrityConstraintViolationException e) {
				/**
				 * Allow the database to recover from a unique index violation.
				 * Not sure how the database has allowed itself to get into that
				 * in the first place but that seems out of our control - my
				 * assumption being that h2database should not allow a unique index
				 * to be applied to non-unique data.
				 *
				 * @see https://github.com/UniversalMediaServer/UniversalMediaServer/issues/3901
				 */
				LOGGER.debug("Attempting to recover from error: {}", e.getMessage());
				LOGGER.trace("", e);

				executeUpdate(connection, "DROP INDEX IF EXISTS IDX_M_KEY");
				String query = DELETE_FROM + TABLE_NAME + WHERE + COL_M_KEY + EQUAL + sqlQuote(key);
				try {
					execute(connection, query);
					LOGGER.debug("Recovery seems successful, recreating unique index");
				} catch (SQLException se) {
					LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "deleting value", key, TABLE_NAME, se.getMessage());
					LOGGER.trace("", se);
				}
				executeUpdate(connection, CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_M_KEY + IDX_MARKER + ON + TABLE_NAME + "(" + COL_M_KEY + ")");
			} catch (Exception e) {
				LOGGER.error("Error while writing metadata: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		} catch (Exception e2) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_FOR_IN, DATABASE_NAME, "writing value", key, value, TABLE_NAME, e2.getMessage());
			LOGGER.trace("", e2);
		}
	}
}
