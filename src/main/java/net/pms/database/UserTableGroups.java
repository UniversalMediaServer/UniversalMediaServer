/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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
import net.pms.iam.AccountService;
import net.pms.iam.Group;
import net.pms.iam.Permissions;
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
	private static final int TABLE_VERSION = 3;

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
				case 1 -> {
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN IF EXISTS NAME RENAME TO DISPLAY_NAME");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
				}
				case 2 -> {
					dropTableAndConstraint(connection, "PERMISSIONS");
					UserTableTablesVersions.removeTableVersion(connection, "PERMISSIONS");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD IF NOT EXISTS PERMISSIONS INTEGER DEFAULT 0");
					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET PERMISSIONS = " + Permissions.ALL + " WHERE ID = 1");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
				}
				default -> throw new IllegalStateException(
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
				"ID                 INTEGER         PRIMARY KEY AUTO_INCREMENT, " +
				"DISPLAY_NAME       VARCHAR2(255)   UNIQUE                    , " +
				"PERMISSIONS        INTEGER         DEFAULT 0                   " +
			")"
		);
		// create an initial group for admin in the table
		addGroup(connection, AccountService.DEFAULT_ADMIN_GROUP, Permissions.ALL);
	}

	public static void addGroup(Connection connection, String displayName, int permissions) {
		if (connection == null || displayName == null || "".equals(displayName)) {
			return;
		}
		String query = "INSERT INTO " + TABLE_NAME + " (DISPLAY_NAME, PERMISSIONS) VALUES (" + sqlQuote(displayName) + ", " + permissions + ")";
		try {
			execute(connection, query);
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "inserting value", sqlEscape(displayName), TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static boolean updateGroupName(Connection connection, int id, String name) {
		if (connection == null || id < 1 || name == null || "".equals(name)) {
			return false;
		}
		String query = "UPDATE " + TABLE_NAME + " SET DISPLAY_NAME = " + sqlQuote(name) + " WHERE ID = " + id;
		try {
			execute(connection, query);
			return true;
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "updating value", sqlEscape(name), TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
			return false;
		}
	}

	public static boolean updateGroupPermissions(Connection connection, int id, int permissions) {
		if (connection == null || id < 1) {
			return false;
		}
		String query = "UPDATE " + TABLE_NAME + " SET PERMISSIONS = " + permissions + " WHERE ID = " + id;
		try {
			execute(connection, query);
			return true;
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "updating permissions", permissions, TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
			return false;
		}
	}

	public static boolean removeGroup(Connection connection, int id) {
		//group id < 2 to prevent admin group (1) to be deleted
		if (connection == null || id < 2) {
			return false;
		}
		String query = "DELETE FROM " + TABLE_NAME + " WHERE ID = " + id;
		try {
			execute(connection, query);
			return true;
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "deleting value", id, TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
			return false;
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
					result.setDisplayName(resultSet.getString("DISPLAY_NAME"));
					result.setPermissions(resultSet.getInt("PERMISSIONS"));
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding group: " + e);
		}
		result.setId(0);
		result.setDisplayName("");
		result.setPermissions(0);
		return result;
	}

	public static List<Group> getAllGroups(final Connection connection) {
		List<Group> result = new ArrayList<>();
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME;
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					Group group = new Group();
					group.setId(resultSet.getInt("ID"));
					group.setDisplayName(resultSet.getString("DISPLAY_NAME"));
					group.setPermissions(resultSet.getInt("PERMISSIONS"));
					result.add(group);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error listing groups: " + e);
		}
		return result;
	}

}
