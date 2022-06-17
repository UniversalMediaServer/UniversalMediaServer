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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.pms.iam.AccountService;
import net.pms.iam.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserTableGroups extends UserTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserTableGroups.class);
	public static final String TABLE_NAME = "GROUPS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (tableExists(connection, TABLE_NAME)) {
			Integer version = UserTableTablesVersions.getTableVersion(connection, TABLE_NAME);
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
				UserTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} else {
			createTable(connection);
			UserTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
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
				default:
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		UserTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				"ID					INT				PRIMARY KEY AUTO_INCREMENT, " +
				"NAME				VARCHAR2(255)	UNIQUE" +
			")"
		);
		// create an initial group for admin in the table
		addGroup(connection, AccountService.DEFAULT_ADMIN_GROUP);
	}

	public static void addGroup(Connection connection, String name) {
		if (connection == null || name == null || "".equals(name)) {
			return;
		}
		String query = "INSERT INTO " + TABLE_NAME + " SET NAME = " + sqlQuote(name);
		try {
			execute(connection, query);
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "inserting value", sqlEscape(name), TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static void updateGroupName(Connection connection, int id, String name) {
		if (connection == null || id < 1 || name == null || "".equals(name)) {
			return;
		}
		String query = "UPDATE " + TABLE_NAME + " SET NAME = " + sqlQuote(name) + " WHERE GROUP_ID = " + id;
		try {
			execute(connection, query);
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "updating value", sqlEscape(name), TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static void removeGroup(Connection connection, int id) {
		//group id < 2 to prevent admin group (1) to be deleted
		if (connection == null || id < 2) {
			return;
		}
		String query = "DELETE FROM " + TABLE_NAME + " WHERE ID = " + id;
		try {
			execute(connection, query);
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "deleting value", id, TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static Group getGroupById(final Connection connection, final int id) {
		Group result = new Group();
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME + " " +
					"WHERE ID='" + id + "' " +
					"LIMIT 1";
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					result.setId(resultSet.getInt("ID"));
					result.setName(resultSet.getString("NAME"));
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding group: " + e);
		}
		result.setId(0);
		result.setName("");
		return result;
	}

}
