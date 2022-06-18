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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserTablePermissions extends UserTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserTablePermissions.class);
	public static final String TABLE_NAME = "PERMISSIONS";

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
				case 1:
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN ALLOW");
					executeUpdate(connection, "DELETE FROM " + TABLE_NAME + " WHERE ID=0");
					executeUpdate(connection, "DELETE FROM " + TABLE_NAME + " WHERE ID=1");
					executeUpdate(connection, "INSERT INTO " + TABLE_NAME + " (GROUP_ID, NAME) VALUES (1, '*')");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					break;
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
				"GROUP_ID			INT, " +
				"NAME				VARCHAR2(255)" +
			")"
		);
		// allow full access to admin group (1)
		execute(connection,
				"INSERT INTO " + TABLE_NAME + " " +
				"(GROUP_ID, NAME) " +
				"VALUES (1, '*')" +
			")"
		);
	}

	public static void insert(Connection connection, int groupId, String name) {
		//group id < 2 to prevent no group (0) and admin group (1) permissions to be changed
		if (connection == null || groupId < 2 || name == null || "".equals(name)) {
			return;
		}
		String query = "SELECT * FROM " + TABLE_NAME + " WHERE GROUP_ID = " + groupId + " AND NAME = " + sqlQuote(name) + " LIMIT 1";
		LOGGER.trace("Searching for value in {} with \"{}\" before insert", TABLE_NAME, query);
		try (
			Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ResultSet result = statement.executeQuery(query)
		) {
			if (!result.next()) {
				result.moveToInsertRow();
				result.updateInt("GROUP_ID", groupId);
				result.updateString("NAME", sqlEscape(name));
				result.insertRow();
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "writing value", sqlQuote(name), TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static void remove(Connection connection, int groupId, String name) {
		//group id < 2 to prevent no group (0) and admin group (1) permissions to be changed
		if (connection == null || groupId < 2 || name == null || "".equals(name)) {
			return;
		}
		String query = "DELETE FROM " + TABLE_NAME + " WHERE GROUP_ID = " + groupId + " AND NAME = " + sqlQuote(name);
		try {
			execute(connection, query);
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "deleting value", sqlQuote(name), TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static boolean removeGroup(Connection connection, int groupId) {
		//group id < 2 to prevent admin group (1) permissions to be removed
		if (connection == null || groupId < 2) {
			return false;
		}
		String query = "DELETE FROM " + TABLE_NAME + " WHERE GROUP_ID = " + groupId;
		try {
			execute(connection, query);
			return true;
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "deleting group id", groupId, TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
			return false;
		}
	}

	public static boolean updateGroup(Connection connection, int groupId, List<String> permissions) {
		//group id < 2 to prevent admin group (1) permissions to be removed
		if (connection == null || groupId < 2) {
			return false;
		}
		if (removeGroup(connection, groupId)) {
			for (String permission : permissions) {
				insert(connection, groupId, permission);
			}
			return true;
		}
		return false;
	}

	public static List<String> getPermissionsForGroupId(final Connection connection, final int groupId) {
		List<String> result = new ArrayList<>();
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME + " " +
					"WHERE GROUP_ID" + "='" + groupId + "' ";
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					result.add(resultSet.getString("NAME"));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error getPermissions: " + e);
		}
		return result;
	}
}
