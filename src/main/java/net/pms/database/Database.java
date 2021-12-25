/*
 * Universal Media Server, for streaming any medias to DLNA
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

import net.pms.dlna.*;
import java.awt.Component;
import java.io.File;
import java.sql.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.pms.Messages;
import net.pms.PMS;
import org.apache.commons.io.FileUtils;
import org.h2.engine.Constants;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Properties;
import org.h2.tools.Upgrade;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class Database extends DatabaseHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

	private String url;
	private String dbDir;
	private String dbName;
	private String dbUser;
	private String dbPassword;
	private final JdbcConnectionPool cp;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 *
	 * @param name the database name
	 * @param user the database user
	 * @param password the database password
	 */
	public Database(String name, String user, String password) {
		dbName = name;
		dbUser = user;
		dbPassword = password;
		File profileDirectory = new File(CONFIGURATION.getProfileDirectory());
		dbDir = new File(PMS.isRunningTests() || profileDirectory.isDirectory() ? CONFIGURATION.getProfileDirectory() : null, "database").getAbsolutePath();
		url = Constants.START_URL + dbDir + File.separator + dbName + ";DB_CLOSE_ON_EXIT=FALSE";
		LOGGER.info("Using database engine version {}.{}.{}", Constants.VERSION_MAJOR, Constants.VERSION_MINOR, Constants.BUILD_ID);

		if (CONFIGURATION.getDatabaseLogging()) {
			url += ";TRACE_LEVEL_FILE=3";
			LOGGER.info("Database logging is enabled");
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Database logging is disabled");
		}

		LOGGER.debug("Using database URL: {}", url);
		LOGGER.info("Using database located at: \"{}\"", dbDir);

		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.error(null, e);
		}

		JdbcDataSource ds = new JdbcDataSource();
		ds.setURL(url);
		ds.setUser(dbUser);
		ds.setPassword(dbPassword);
		cp = JdbcConnectionPool.create(ds);
	}

	public Database(String name) {
		this(name, "sa", "");
	}

	/**
	 * Gets the name of the database file
	 *
	 * @return The filename
	 */
	public String getDatabaseFilename() {
		if (dbName == null || dbDir == null) {
			return null;
		}
		return dbDir + File.separator + dbName;
	}

	/**
	 * Gets the database path
	 *
	 * @return The database path
	 */
	public String getDatabasePath() {
		if (dbDir == null) {
			return null;
		}

		return dbDir;
	}

	/**
	 * Gets a new connection from the connection pool if one is available. If
	 * not waits for a free slot until timeout.<br>
	 * <br>
	 * <strong>Important: Every connection must be closed after use</strong>
	 *
	 * @return the new connection
	 * @throws SQLException
	 */
	public Connection getConnection() throws SQLException {
		return cp.getConnection();
	}

	/**
	 * Initialized the database for use, performing checks and creating a new
	 * database if necessary.
	 *
	 * @param force whether to recreate the database regardless of necessity.
	 */
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
	public synchronized void init(boolean force) {
		Connection conn = null;
		try {
			conn = getConnection();
		} catch (SQLException se) {
			final File dbFile = new File(dbDir + File.separator + dbName + ".data.db");
			final File dbDirectory = new File(dbDir);
			if (se.getErrorCode() == 90030 && se.getCause().getMessage().contains("format 2 is larger than the supported format 1")) {
				LOGGER.info("The current database was migrated to h2 format 2");
				//check if the backup file is still present
				File dbNewFile = new File(dbDir + File.separator + dbName + ".mv.db");
				if (dbNewFile.exists()) {
					//delete the new file
					LOGGER.trace("Deleting the current db file \"{}\"", dbNewFile.getName());
					dbNewFile.delete();
					LOGGER.info("The database has been deleted because it had the wrong version");
				}
				File dbBakFile = new File(dbDir + File.separator + dbName + ".mv.db.bak");
				if (dbBakFile.exists()) {
					LOGGER.info("Revert back to the backup database not migrated");
					LOGGER.trace("Renaming backup db file \"{}\" to \"{}\"", dbBakFile.getName(), dbNewFile.getName());
					dbBakFile.renameTo(dbNewFile);
					try {
						LOGGER.trace("Try to open the backup db file");
						conn = getConnection();
					} catch (SQLException ex) {
						LOGGER.info("The backup database failed to open");
						if (dbNewFile.exists()) {
							LOGGER.trace("Renaming current db file \"{}\" to \"{}\"", dbNewFile.getName(), dbBakFile.getName());
							dbNewFile.renameTo(dbBakFile);
						}
					}
				} else if (dbNewFile.exists()) {
					//delete the new file
					dbNewFile.delete();
					LOGGER.info("The database has been deleted because it had the wrong version");
				}
			} else if (se.getErrorCode() == 50000 && se.getMessage().contains("format 1 is smaller than the supported format 2")) {
				LOGGER.info("The database need a migration to h2 format 2");
				migrateDatabaseVersion2();
			} else if (dbFile.exists() || (se.getErrorCode() == 90048)) { // Cache is corrupt or a wrong version, so delete it
				FileUtils.deleteQuietly(dbDirectory);
				if (!dbDirectory.exists()) {
					LOGGER.info("The database has been deleted because it was corrupt or had the wrong version");
				} else {
					if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
						JOptionPane.showMessageDialog(
							SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
							String.format(Messages.getString("DLNAMediaDatabase.5"), dbDir),
							Messages.getString("Dialog.Error"),
							JOptionPane.ERROR_MESSAGE
						);
					}
					LOGGER.error("Damaged cache can't be deleted. Stop the program and delete the folder \"" + dbDir + "\" manually");
					PMS.get().getRootFolder(null).stopScan();
					CONFIGURATION.setUseCache(false);
				}
			} else {
				LOGGER.debug("Database connection error, retrying in 10 seconds");
				try {
					Thread.sleep(10000);
					conn = getConnection();
				} catch (InterruptedException | SQLException se2) {
					if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
						try {
							JOptionPane.showMessageDialog(
								SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
								String.format(Messages.getString("DLNAMediaDatabase.ConnectionError"), dbDir),
								Messages.getString("Dialog.Error"),
								JOptionPane.ERROR_MESSAGE
							);
						} catch (NullPointerException e) {
							LOGGER.debug("Failed to show database connection error message, probably because GUI is not initialized yet. Error was {}", e);
						}
					}
					LOGGER.debug("", se2);
					RootFolder rootFolder = PMS.get().getRootFolder(null);
					if (rootFolder != null) {
						rootFolder.stopScan();
					}
				}
			}
		} finally {
			close(conn);
		}
	}

	/**
	 * Migrate the h2 database from version 1.4.197 to version 2.
	 *
	 */
	private void migrateDatabaseVersion2() {
		LOGGER.info("Migrating database to v{}", Constants.VERSION);
		if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
			try {
				PMS.get().getFrame().setStatusLine("Migrating database to v" + Constants.VERSION);
			} catch (NullPointerException e) {
				LOGGER.debug("Failed to set status, probably because GUI is not initialized yet. Error was {}", e);
			}
		}
		String oldUrl = Constants.START_URL + dbDir + File.separator + dbName;
		Properties prprts = new Properties();
		prprts.setProperty("user", dbUser);
		prprts.setProperty("password", dbPassword);
		try {
			Upgrade.upgrade(oldUrl, prprts, 197);
			LOGGER.info("The database successfully migrated to version {}", Constants.VERSION);
		} catch (Exception e) {
			LOGGER.error(
				"Database migration failed: {}",
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}
}
