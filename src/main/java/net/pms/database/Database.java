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
import org.h2.tools.ConvertTraceFile;
import org.h2.tools.Upgrade;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public abstract class Database extends DatabaseHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(Database.class);

	private String url;
	private String dbDir;
	private String dbName;
	private String dbUser;
	private String dbPassword;
	private final JdbcConnectionPool cp;

	protected DatabaseStatus status;

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
		status = DatabaseStatus.CLOSED;
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

		LOGGER.debug("Using \"{}\" database URL: {}", dbName, url);
		LOGGER.info("Using \"{}\" database located at: \"{}\"", dbName, dbDir);

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

	public int getActiveConnections() throws SQLException {
		return cp.getActiveConnections();
	}

	public boolean isOpened() {
		return status == DatabaseStatus.OPENED;
	}

	/**
	 * Initialized the database for use, performing checks and creating a new
	 * database if necessary.
	 *
	 * @param force whether to recreate the database regardless of necessity.
	 */
	@SuppressFBWarnings("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
	public synchronized void init(boolean force) {
		open();
		switch (status) {
			case OPENED:
				onOpening(force);
				break;
			case OPENFAILED:
				onOpeningFail(force);
				break;
		}
	}

	private void open() {
		status = DatabaseStatus.OPENING;
		Connection conn = null;
		boolean needRetry = false;
		try {
			conn = getConnection();
			status = DatabaseStatus.OPENED;
		} catch (SQLException se) {
			status = DatabaseStatus.OPENFAILED;
			final File dbFile = new File(dbDir + File.separator + dbName + ".data.db");
			final File dbDirectory = new File(dbDir);
			if (se.getErrorCode() == 50000 && se.getMessage().contains("format 1 is smaller than the supported format 2")) {
				LOGGER.info("The database need a migration to h2 format 2");
				migrateDatabaseVersion2(true);
				needRetry = true;
			} else if (dbFile.exists() || (se.getErrorCode() == 90048)) { // Cache is corrupt or a wrong version, so delete it
				FileUtils.deleteQuietly(dbDirectory);
				if (!dbDirectory.exists()) {
					LOGGER.info("The database has been deleted because it was corrupt or had the wrong version");
					needRetry = true;
				} else {
					showMessageDialog("DLNAMediaDatabase.5", dbDir);
					LOGGER.error("Damaged cache can't be deleted. Stop the program and delete the folder \"" + dbDir + "\" manually");
				}
			} else {
				LOGGER.debug("Database connection error, retrying in 10 seconds");
				sleep(10000);
				needRetry = true;
			}
			if (needRetry) {
				try {
					conn = getConnection();
					status = DatabaseStatus.OPENED;
				} catch (SQLException se2) {
					showMessageDialog("DLNAMediaDatabase.ConnectionError", dbDir);
					LOGGER.debug("", se2);
				}
			}
		} finally {
			close(conn);
		}
	}

	abstract void onOpening(boolean force);

	abstract void onOpeningFail(boolean force);

	private void showMessageDialog(String message, String dbDir) {
		if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
			try {
				JOptionPane.showMessageDialog(
					SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
					String.format(Messages.getString(message), dbDir),
					Messages.getString("Dialog.Error"),
					JOptionPane.ERROR_MESSAGE
				);
			} catch (NullPointerException e1) {
				LOGGER.debug("Failed to show database connection error message, probably because GUI is not initialized yet. Error was {}", e1);
			}
		}
	}

	public void close() {
		status = DatabaseStatus.CLOSING;
		try {
			Thread.sleep(500);
			while (getActiveConnections() > 0) {
				Thread.sleep(500);
			}
		} catch (SQLException e) {
			LOGGER.error("Waiting DB connections", e);
		} catch (InterruptedException e) {
			LOGGER.debug("Interrupted while shutting down..");
			LOGGER.trace("", e);
		}

		try (Statement stmt = getConnection().createStatement()) {
			stmt.execute("SHUTDOWN COMPACT");
		} catch (SQLException e1) {
			LOGGER.error("compacting DB ", e1);
		}
		status = DatabaseStatus.CLOSED;
	}

	/**
	 * Migrate the h2 database from version 1.4.197 to version 2.
	 *
	 */
	private void migrateDatabaseVersion2(boolean deleteBackup) {
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
			if (deleteBackup) {
				final File dbBakFile = new File(dbDir + File.separator + dbName + ".mv.db.bak");
				if (dbBakFile.exists()) {
					dbBakFile.delete();
				}
			}
			LOGGER.info("The database successfully migrated to version {}", Constants.VERSION);
		} catch (Exception e) {
			LOGGER.error(
				"Database migration failed: {}",
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Create the database report.
	 * Use an automatic H2database profiling tool to make a report at the end of the logging file
	 * converted to the "logging_report.txt" in the database directory.
	 */
	public void createDatabaseReport() {
		try {
			ConvertTraceFile.main("-traceFile", getDatabaseFilename() + ".trace.db",
				"-script", getDatabaseFilename() + "_logging_report.txt");
		} catch (SQLException e) {}
	}

	/**
	 * Utility method to call {@link Thread#sleep(long)} without having to catch
	 * the InterruptedException.
	 *
	 * @param delay the delay
	 */
	public static void sleep(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) {
		}
	}

	public enum DatabaseStatus {
		OPENING,
		OPENED,
		OPENFAILED,
		CLOSING,
		CLOSED
	}
}
