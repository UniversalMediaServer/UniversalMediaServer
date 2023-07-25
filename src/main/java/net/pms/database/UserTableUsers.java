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
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.iam.AccountService;
import net.pms.iam.User;
import org.apache.commons.lang3.StringUtils;

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
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_USERNAME = "USERNAME";
	private static final String COL_PASSWORD = "PASSWORD";
	private static final String COL_DISPLAY_NAME = "DISPLAY_NAME";
	private static final String COL_GROUP_ID = "GROUP_ID";
	private static final String COL_LAST_LOGIN_TIME = "LAST_LOGIN_TIME";
	private static final String COL_LOGIN_FAIL_TIME = "LOGIN_FAIL_TIME";
	private static final String COL_LOGIN_FAIL_COUNT = "LOGIN_FAIL_COUNT";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	private static final String TABLE_COL_USERNAME = TABLE_NAME + "." + COL_USERNAME;
	private static final String TABLE_COL_GROUP_ID = TABLE_NAME + "." + COL_GROUP_ID;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL = SELECT_ALL + FROM + TABLE_NAME;
	private static final String SQL_GET_BY_ID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_BY_USERNAME = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_USERNAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ADMIN = SELECT + COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_GROUP_ID + EQUAL + "1" + LIMIT_1;
	private static final String SQL_INSERT_USER = INSERT_INTO + TABLE_NAME + " (" + COL_USERNAME + ", " + COL_PASSWORD + ", " + COL_DISPLAY_NAME + ", " + COL_GROUP_ID + ") " + "VALUES(" + PARAMETER + ", " + PARAMETER + ", " + PARAMETER + ", " + PARAMETER +  ")";
	private static final String SQL_UPDATE_USER = UPDATE + TABLE_NAME + SET + COL_DISPLAY_NAME + EQUAL + PARAMETER + ", " + COL_GROUP_ID + EQUAL + PARAMETER + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_LOGIN = UPDATE + TABLE_NAME + SET + COL_USERNAME + EQUAL + PARAMETER + ", " + COL_PASSWORD + EQUAL + PARAMETER + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_LOGIN_TIME = UPDATE + TABLE_NAME + SET + COL_LAST_LOGIN_TIME + EQUAL + PARAMETER + ", " + COL_LOGIN_FAIL_TIME + EQUAL + "'0', " + COL_LOGIN_FAIL_COUNT + EQUAL + "'0'" + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_LOGIN_FAIL = UPDATE + TABLE_NAME + SET + COL_LOGIN_FAIL_TIME + EQUAL + PARAMETER + ", " + COL_LOGIN_FAIL_COUNT + EQUAL + COL_LOGIN_FAIL_COUNT + "+1" + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_RESET_FAIL_COUNT = UPDATE + TABLE_NAME + SET + COL_LOGIN_FAIL_COUNT + EQUAL + "'0'" + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_DELETE_ID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;

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
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + IF_NOT_EXISTS + COL_DISPLAY_NAME + VARCHAR_255);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + IF_NOT_EXISTS + COL_GROUP_ID + INTEGER + DEFAULT_0);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + IF_NOT_EXISTS + COL_LAST_LOGIN_TIME + BIGINT + DEFAULT_0);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + IF_NOT_EXISTS + COL_LOGIN_FAIL_TIME + BIGINT + DEFAULT_0);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + IF_NOT_EXISTS + COL_LOGIN_FAIL_COUNT + INTEGER + DEFAULT_0);
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_DISPLAY_NAME + EQUAL + "'" + AccountService.DEFAULT_ADMIN_GROUP + "'" + WHERE + COL_ID + EQUAL + "0");
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_GROUP_ID + EQUAL + "1" + WHERE + COL_ID + EQUAL + "1");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
				}
				case 2 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_GROUP_ID + SET + DEFAULT_0);
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_GROUP_ID + EQUAL + "1" + WHERE + COL_ID + EQUAL + "1");
					LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
				}
				case 3 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "NAME" + RENAME_TO + COL_DISPLAY_NAME);
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_DISPLAY_NAME + EQUAL + "'" + AccountService.DEFAULT_ADMIN_GROUP + "'" + WHERE + COL_ID + EQUAL + "1");
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
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                 + INTEGER        + PRIMARY_KEY + AUTO_INCREMENT + COMMA +
				COL_USERNAME           + VARCHAR_255    + UNIQUE                       + COMMA +
				COL_PASSWORD           + VARCHAR_255    + NOT_NULL                     + COMMA +
				COL_DISPLAY_NAME       + VARCHAR_255                                   + COMMA +
				COL_GROUP_ID           + INTEGER        + DEFAULT_0                    + COMMA +
				COL_LAST_LOGIN_TIME    + BIGINT         + DEFAULT_0                    + COMMA +
				COL_LOGIN_FAIL_TIME    + BIGINT         + DEFAULT_0                    + COMMA +
				COL_LOGIN_FAIL_COUNT   + INTEGER        + DEFAULT_0                    +
			")"
		);
	}

	public static void addUser(final Connection connection, final String username, final String password, final String displayName, final int groupId) {
		if (connection == null || username == null || "".equals(username) || password == null || "".equals(password)) {
			return;
		}
		LOGGER.info("Creating user: {}", username);
		try (PreparedStatement insertStatement = connection.prepareStatement(SQL_INSERT_USER, Statement.RETURN_GENERATED_KEYS)) {
			insertStatement.clearParameters();
			insertStatement.setString(1, StringUtils.left(username, 255));
			insertStatement.setString(2, StringUtils.left(password, 255));
			insertStatement.setString(3, StringUtils.left(displayName, 255));
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
		try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE_ID)) {
			statement.setInt(1, id);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error deleteUser:{}", e.getMessage());
		}
	}

	public static boolean updateUser(final Connection connection, final int id, final String displayName, final int groupId) {
		if (connection == null || displayName == null) {
			return false;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_USER)) {
			statement.setString(1, StringUtils.left(displayName, 255));
			statement.setInt(2, groupId);
			statement.setInt(3, id);
			statement.executeUpdate();
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
		try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_LOGIN)) {
			statement.setString(1, StringUtils.left(username, 255));
			statement.setString(2, StringUtils.left(password, 255));
			statement.setInt(3, id);
			statement.executeUpdate();
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
		try (PreparedStatement statement = connection.prepareStatement(SQL_RESET_FAIL_COUNT)) {
			statement.setInt(1, id);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error resetLoginFailCount:{}", e.getMessage());
		}
	}

	public static void setLoginTime(final Connection connection, final int id, final long time) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_LOGIN_TIME)) {
			statement.setLong(1, time);
			statement.setInt(2, id);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error setLoginTime:{}", e.getMessage());
		}
	}

	public static void setLoginFailed(final Connection connection, final int id, final long time) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_LOGIN_FAIL)) {
			statement.setLong(1, time);
			statement.setInt(2, id);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error setLoginFailed:{}", e.getMessage());
		}
	}

	public static User getUserByUsername(final Connection connection, final String username) {
		LOGGER.info("Finding user: {} ", username);
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_BY_USERNAME)) {
			statement.setString(1, StringUtils.left(username, 255));
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSetToUser(resultSet);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return null;
	}

	public static User getUserByUserId(final Connection connection, final int id) {
		LOGGER.info("Finding user id: {} ", id);
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_BY_ID)) {
			statement.setInt(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSetToUser(resultSet);
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
		try (
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_GET_ALL);
		) {
			while (resultSet.next()) {
				result.add(resultSetToUser(resultSet));
			}
		} catch (SQLException e) {
			LOGGER.error("Error finding user: " + e);
		}
		return result;
	}

	public static boolean hasNoAdmin(final Connection connection) {
		try (
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery(SQL_GET_ADMIN);
		) {
			return !resultSet.next();
		} catch (SQLException e) {
			LOGGER.error("Error finding admin: " + e);
		}
		return true;
	}

	private static User resultSetToUser(ResultSet resultSet) throws SQLException {
		User result = new User();
		result.setId(resultSet.getInt(COL_ID));
		result.setUsername(resultSet.getString(COL_USERNAME));
		result.setPassword(resultSet.getString(COL_PASSWORD));
		result.setDisplayName(resultSet.getString(COL_DISPLAY_NAME));
		result.setGroupId(resultSet.getInt(COL_GROUP_ID));
		result.setLastLoginTime(resultSet.getLong(COL_LAST_LOGIN_TIME));
		result.setLoginFailedTime(resultSet.getLong(COL_LOGIN_FAIL_TIME));
		result.setLoginFailedCount(resultSet.getInt(COL_LOGIN_FAIL_COUNT));
		return result;
	}

}
