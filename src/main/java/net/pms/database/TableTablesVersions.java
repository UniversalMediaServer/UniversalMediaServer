/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableTablesVersions extends TableHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(TableTablesVersions.class);
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	protected static final String TABLE_NAME = "TABLES_VERSIONS";
	protected static final String TABLE_NAME_OLD = "TABLES";

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		TABLE_LOCK.writeLock().lock();
		try {
			if (!tableExists(connection, TABLE_NAME)) {
				//check if old table name
				if (!tableExists(connection, TABLE_NAME_OLD)) {
					createTable(connection);
				} else {
					//change to new table name, remove name and version columns (reserved sql)
					LOGGER.trace("Changing table name from \"{}\" to \"{}\"", TABLE_NAME_OLD, TABLE_NAME);
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME_OLD + " ALTER COLUMN `NAME` RENAME TO TABLE_NAME");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME_OLD + " ALTER COLUMN `VERSION` RENAME TO TABLE_VERSION");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME_OLD + " RENAME TO " + TABLE_NAME);
				}
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	protected static final void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table \"" + TABLE_NAME + "\"");
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE " + TABLE_NAME + "(TABLE_NAME VARCHAR(50) PRIMARY KEY, TABLE_VERSION INT NOT NULL)");
		}
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
		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT TABLE_VERSION FROM " + TABLE_NAME + " " +
				"WHERE TABLE_NAME = ?"
		)) {
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
		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT TABLE_VERSION FROM " + TABLE_NAME + " WHERE TABLE_NAME = ?"
		)) {
			statement.setString(1, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					int currentVersion = result.getInt("TABLE_VERSION");
					if (version != currentVersion) {
						try (PreparedStatement updateStatement = connection.prepareStatement(
							"UPDATE " + TABLE_NAME + " SET TABLE_VERSION = ? WHERE TABLE_NAME = ?"
						)) {
							LOGGER.trace("Updating table version for database table \"{}\" from {} to {}", tableName, currentVersion, version);
							updateStatement.setInt(1, version);
							updateStatement.setString(2, tableName);
							updateStatement.executeUpdate();
						}
					} else {
						LOGGER.trace("Table version for database table \"{}\" is already {}, aborting set", tableName, version);
					}
				} else {
					try (PreparedStatement insertStatement = connection.prepareStatement(
						"INSERT INTO " + TABLE_NAME + " VALUES(?, ?)"
					)) {
						LOGGER.trace("Setting table version for database table \"{}\" to {}", tableName, version);
						insertStatement.setString(1, tableName);
						insertStatement.setInt(2, version);
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}
}
