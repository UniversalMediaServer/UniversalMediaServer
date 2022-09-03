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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.iam.AccountService;
import net.pms.iam.User;
import static org.apache.commons.lang3.StringUtils.left;

public final class UserTableUsers extends UserTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserTableUsers.class);
	public static final String TABLE_NAME = "USERS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 4;

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
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD NAME VARCHAR2(255)");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD GROUP_ID INT DEFAULT 0");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD LAST_LOGIN_TIME BIGINT DEFAULT 0");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD LOGIN_FAIL_TIME BIGINT DEFAULT 0");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD LOGIN_FAIL_COUNT INT DEFAULT 0");
					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET NAME='" + AccountService.DEFAULT_ADMIN_GROUP + "' WHERE ID=0");
					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET GROUP_ID=1 WHERE ID=1");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					break;
				case 2:
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN IF EXISTS GROUP_ID SET DEFAULT 0");
					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET GROUP_ID=1 WHERE ID=1");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					break;
				case 3:
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN IF EXISTS NAME RENAME TO DISPLAY_NAME");
					executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET DISPLAY_NAME='" + AccountService.DEFAULT_ADMIN_GROUP + "' WHERE ID=1");					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
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
				"ID					INT				PRIMARY KEY AUTO_INCREMENT, " +
				"USERNAME			VARCHAR2(255)	UNIQUE, " +
				"PASSWORD			VARCHAR2(255)	NOT NULL, " +
				"DISPLAY_NAME		VARCHAR2(255), " +
				"GROUP_ID			INT				DEFAULT 0, " +
				"LAST_LOGIN_TIME	BIGINT			DEFAULT 0, " +
				"LOGIN_FAIL_TIME	BIGINT			DEFAULT 0, " +
				"LOGIN_FAIL_COUNT	INT				DEFAULT 0" +
			")"
		);
	}

	public static void addUser(final Connection connection, final String username, final String password, final String displayName, final int groupId) {
		if (connection == null || username == null || "".equals(username) || password == null || "".equals(password)) {
			return;
		}
		LOGGER.info("Creating user: {}", username);
		try (PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO " + UserTableUsers.TABLE_NAME + "(USERNAME, PASSWORD, DISPLAY_NAME, GROUP_ID) " + "VALUES(?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS)) {
			insertStatement.clearParameters();
			insertStatement.setString(1, left(username, 255));
			insertStatement.setString(2, left(password, 255));
			insertStatement.setString(3, left(displayName, 255));
			insertStatement.setInt(4, groupId);
			insertStatement.executeUpdate();
			try (ResultSet rs = insertStatement.getGeneratedKeys()) {
				if (rs.next()) {
					LOGGER.info("Created user successfully in " + UserTableUsers.TABLE_NAME);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("ERROR createUser" + e);
		}
	}

	public static void deleteUser(final Connection connection, int id) {
		if (connection == null || id < 1) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			String sql = "DELETE " + TABLE_NAME + " " +
					"WHERE ID='" + id + "'";
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LOGGER.error("Error deleteUser:{}", e.getMessage());
		}
	}

	public static boolean updateUser(final Connection connection, final int id, final String displayName, final int groupId) {
		if (connection == null || displayName == null) {
			return false;
		}
		try (Statement statement = connection.createStatement()) {
			String sql = "UPDATE " + TABLE_NAME + " " +
					"SET DISPLAY_NAME = " + sqlQuote(displayName) + ", " +
					"GROUP_ID = " + groupId + " " +
					"WHERE ID='" + id + "'";
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			LOGGER.error("Error updateLogin:{}", e.getMessage());
			return false;
		}
	}

	public static boolean updateLogin(final Connection connection, final int id, final String username, final String password) {
		if (connection == null || username == null || "".equals(username) || password == null || "".equals(password)) {
			return false;
		}
		try (Statement statement = connection.createStatement()) {
			String sql = "UPDATE " + TABLE_NAME + " " +
					"SET USERNAME = " + sqlQuote(username) + ", " +
					"PASSWORD = " + sqlQuote(password) + " " +
					"WHERE ID='" + id + "'";
			statement.executeUpdate(sql);
			return true;
		} catch (SQLException e) {
			LOGGER.error("Error updateLogin:{}", e.getMessage());
			return false;
		}
	}

	public static void resetLoginFailCount(final Connection connection, final int id) {
		if (connection == null) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			String sql = "UPDATE " + TABLE_NAME + " " +
					"SET LOGIN_FAIL_COUNT='0' " +
					"WHERE ID='" + id + "'";
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LOGGER.error("Error resetLoginFailCount:{}", e.getMessage());
		}
	}

	public static void setLoginTime(final Connection connection, final int id, final long time) {
		if (connection == null) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			String sql = "UPDATE " + TABLE_NAME + " " +
				"SET LAST_LOGIN_TIME='" + time + "', " +
				"LOGIN_FAIL_TIME='0', LOGIN_FAIL_COUNT='0' " +
				"WHERE ID='" + id + "'";
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LOGGER.error("Error setLoginTime:{}", e.getMessage());
		}
	}

	public static void setLoginFailed(final Connection connection, final int id, final long time) {
		if (connection == null) {
			return;
		}
		try (Statement statement = connection.createStatement()) {
			String sql = "UPDATE " + UserTableUsers.TABLE_NAME + " " +
					"SET LOGIN_FAIL_TIME='" + time + "', " +
					"LOGIN_FAIL_COUNT=LOGIN_FAIL_COUNT+1 " +
					"WHERE ID='" + id + "'";
			statement.executeUpdate(sql);
		} catch (SQLException e) {
			LOGGER.error("Error setLoginFailed:{}", e.getMessage());
		}
	}

	public static User getUserByUsername(final Connection connection, final String username) {
		User result;
		LOGGER.info("Finding user: {} ", sqlEscape(username));
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME + " " +
					"WHERE USERNAME=" + sqlQuote(username) + " " +
					"LIMIT 1";
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					result = resultSetToUser(resultSet);
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return null;
	}

	public static User getUserByUserId(final Connection connection, final int id) {
		User result;
		LOGGER.info("Finding user id: {} ", id);
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME + " " +
					"WHERE ID=" + id + " " +
					"LIMIT 1";
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					result = resultSetToUser(resultSet);
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return null;
	}

	public static List<User> getAllUsers(final Connection connection) {
		List<User> result = new ArrayList<>();
		LOGGER.info("Listing all users");
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME;
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				while (resultSet.next()) {
					result.add(resultSetToUser(resultSet));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return result;
	}

	private static User resultSetToUser(ResultSet resultSet) throws SQLException {
		User result = new User();
		result.setId(resultSet.getInt("ID"));
		result.setUsername(resultSet.getString("USERNAME"));
		result.setPassword(resultSet.getString("PASSWORD"));
		result.setDisplayName(resultSet.getString("DISPLAY_NAME"));
		result.setGroupId(resultSet.getInt("GROUP_ID"));
		result.setLastLoginTime(resultSet.getLong("LAST_LOGIN_TIME"));
		result.setLoginFailedTime(resultSet.getLong("LOGIN_FAIL_TIME"));
		result.setLoginFailedCount(resultSet.getInt("LOGIN_FAIL_COUNT"));
		return result;
	}

	public static boolean hasNoAdmin(final Connection connection) {
		try {
			String sql = "SELECT * " +
					"FROM " + TABLE_NAME + " " +
					"WHERE GROUP_ID = 1 " +
					"LIMIT 1";
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql);
			) {
				return !resultSet.next();
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return true;
	}
}
