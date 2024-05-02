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

import java.sql.*;
import net.pms.swing.Splash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class UserDatabase extends Database {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserDatabase.class);
	public static final String DATABASE_NAME = "USERS";
	/**
	 * Pointer to the instantiated UserDatabase.
	 */
	private static UserDatabase instance = null;
	private static boolean tablesChecked = false;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 */
	public UserDatabase() {
		super(DATABASE_NAME);
	}

	@Override
	void onOpening(boolean force) {
		try {
			checkTables(force);
		} catch (SQLException se) {
			LOGGER.error("Error checking tables: " + se.getMessage());
			LOGGER.trace("", se);
			status = DatabaseStatus.CLOSED;
		}
	}

	@Override
	void onOpeningFail(boolean force) {
	}

	/**
	 * Checks all child tables for their existence and version and creates or
	 * upgrades as needed.Access to this method is serialized.
	 *
	 * @param force do the check even if it has already happened
	 * @throws SQLException
	 */
	public final synchronized void checkTables(boolean force) throws SQLException {
		if (tablesChecked && !force) {
			LOGGER.debug("Database tables have already been checked, aborting check");
		} else {
			LOGGER.debug("Starting check of database tables");
			try (Connection connection = getConnection()) {
				Splash.setStatusMessage("UpgradingUserDb");
				UserTableTablesVersions.checkTable(connection);
				UserTableGroups.checkTable(connection);
				UserTableUsers.checkTable(connection);
			}
			tablesChecked = true;
		}
	}

	/**
	 * Returns the UserDatabase instance.
	 * Will create the database instance as needed.
	 *
	 * @return {@link net.pms.database.UserDatabase}
	 */
	public static synchronized UserDatabase get() {
		if (instance == null) {
			instance = new UserDatabase();
		}
		return instance;
	}

	/**
	 * Initialize the UserDatabase instance.
	 * Will initialize the database instance as needed.
	 */
	public static synchronized void init() {
		get().init(false);
	}

	/**
	 * Initialize the UserDatabase instance.
	 * Will initialize the database instance as needed.
	 * Will check all tables.
	 */
	public static synchronized void initForce() {
		get().init(true);
	}

	/**
	 * Check the UserDatabase instance.
	 *
	 * @return <code>true</code> if the UserDatabase is instantiated
	 * , <code>false</code> otherwise
	 */
	public static boolean isInstantiated() {
		return instance != null;
	}

	/**
	 * Check the UserDatabase instance availability.
	 *
	 * @return {@code true } if the UserDatabase is instantiated and opened
	 * , <code>false</code> otherwise
	 */
	public static boolean isAvailable() {
		return isInstantiated() && instance.isOpened();
	}

	/**
	 * Get a UserDatabase connection.
	 * Will not try to init the database.
	 * Give a connection only if the database status is OPENED.
	 *
	 * Prevent for init or giving a connection on db closing
	 *
	 * @return A {@link java.sql.Connection} if the UserDatabase is available, <code>null</code> otherwise
	 */
	public static Connection getConnectionIfAvailable() {
		if (isAvailable()) {
			try {
				return instance.getConnection();
			} catch (SQLException ex) {}
		}
		return null;
	}

	/**
	 * Shutdown the UserDatabase database.
	 */
	public static synchronized void shutdown() {
		if (instance != null) {
			instance.close();
		}
	}

	public static synchronized void createDatabaseReportIfNeeded() {
		if (instance != null && instance.isEmbedded()) {
			instance.createDatabaseReport();
		}
	}
}
