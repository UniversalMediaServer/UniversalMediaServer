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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaTableTablesVersions extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableTablesVersions.class);
	protected static final String TABLE_NAME = "TABLES_VERSIONS";
	private static final String TABLE_NAME_OLD = "TABLES";

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_TABLE_NAME = "TABLE_NAME";
	private static final String COL_TABLE_VERSION = "TABLE_VERSION";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_TABLE_NAME = TABLE_NAME + "." + COL_TABLE_NAME;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_BY_TABLE_NAME = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_TABLE_NAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_VERSION_BY_TABLE_NAME = SELECT + COL_TABLE_VERSION + FROM + TABLE_NAME + WHERE + TABLE_COL_TABLE_NAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_DELETE_BY_TABLE_NAME = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_TABLE_NAME + EQUAL + PARAMETER;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (!tableExists(connection, TABLE_NAME)) {
			//check if old table name
			if (!tableExists(connection, TABLE_NAME_OLD)) {
				createTable(connection);
			} else {
				//change to new table name, remove name and version columns (reserved sql)
				LOGGER.trace("Changing table name from \"{}\" to \"{}\"", TABLE_NAME_OLD, TABLE_NAME);
				executeUpdate(connection, ALTER_TABLE + TABLE_NAME_OLD + ALTER_COLUMN + "`NAME`" + RENAME_TO + COL_TABLE_NAME);
				executeUpdate(connection, ALTER_TABLE + TABLE_NAME_OLD + ALTER_COLUMN + "`VERSION`" + RENAME_TO + COL_TABLE_VERSION);
				executeUpdate(connection, ALTER_TABLE + TABLE_NAME_OLD + RENAME_TO + TABLE_NAME);
			}
		}
	}

	protected static final void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_TABLE_NAME         + VARCHAR_50     + PRIMARY_KEY + COMMA +
				COL_TABLE_VERSION      + INTEGER        + NOT_NULL    +
			")"
		);
	}

	/**
	 * Gets the version of a named table from the <code>TABLES</code> table
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table for which to find the version
	 *
	 * @return The version number if found or <code>null</code> if the table
	 *         isn't listed in <code>TABLES</code>
	 *
	 * @throws SQLException
	 */
	protected static final Integer getTableVersion(final Connection connection, final String tableName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_VERSION_BY_TABLE_NAME)) {
			statement.setString(1, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Table version for database table \"{}\" is {}", tableName, result.getInt(1));
					}
					return result.getInt(1);
				} else {
					LOGGER.trace("Table version for database table \"{}\" not found", tableName);
					return null;
				}
			}
		}
	}

	/**
	 * Sets the version of a named table in the <code>TABLES</code> table.
	 * Creates a row for the given table name if needed.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table for which to set the version
	 * @param version the version number to set
	 *
	 * @throws SQLException
	 */
	protected static final void setTableVersion(final Connection connection, final String tableName, final int version) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL_BY_TABLE_NAME, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setString(1, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (!result.next()) {
					LOGGER.trace("Setting table version for database table \"{}\" to {}", tableName, version);
					result.moveToInsertRow();
					result.updateString(COL_TABLE_NAME, tableName);
					result.updateInt(COL_TABLE_VERSION, version);
					result.insertRow();
				} else if (result.getInt(COL_TABLE_VERSION) == version) {
					LOGGER.trace("Table version for database table \"{}\" is already {}, aborting set", tableName, version);
				} else {
					int currentVersion = result.getInt(COL_TABLE_VERSION);
					LOGGER.trace("Updating table version for database table \"{}\" from {} to {}", tableName, currentVersion, version);
					result.updateInt(COL_TABLE_VERSION, version);
					result.updateRow();
				}
			}
		}
	}

	protected static final void removeTableVersion(final Connection connection, final String tableName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE_BY_TABLE_NAME)) {
			statement.setString(1, tableName);
			statement.executeUpdate();
		}
	}

}
