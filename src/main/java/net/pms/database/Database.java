/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.h2.engine.Constants;
import org.h2.tools.ConvertTraceFile;
import org.h2.tools.Upgrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.database.syntax.DbTypes;
import net.pms.database.syntax.H2dbTypes;
import net.pms.database.syntax.PostgresTypes;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public abstract class Database extends DatabaseHelper {
	private static Logger logger;

	private String url;
	private String dbDir;
	private String dbName;
	private String dbUser;
	private String dbPassword;

	// External connection pool. (at the moment used for postgresql)
	private HikariDataSource ds = null;

	protected DatabaseStatus status;

	protected DbTypes dbTypes;

	public DbTypes getDbType() {
		return dbTypes;
	}

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
		logger = LoggerFactory.getLogger(Database.class);

		PmsConfiguration pmsConfig = PMS.getConfiguration();
		HikariConfig hikariConfig = new HikariConfig();
		hikariConfig.setUsername(pmsConfig.getDatabaseUser());
		hikariConfig.setPassword(pmsConfig.getDatabasePassword());
		hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
		hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
		hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

		logger.info("using {} database backend ", PMS.getConfiguration().getDatabaseBackend());

		if (isH2dbBackend()) {
			h2dbInit(name, user, password);
			String h2url = pmsConfig.getDatabaseUrl() + ";CACHE_SIZE=131072";
			hikariConfig.setJdbcUrl(h2url);
			logger.info("database url : {} ", h2url);
			dbTypes = new H2dbTypes();
		} else if (isPostgresBackend()) {
			hikariConfig.setJdbcUrl(pmsConfig.getDatabaseUrl());
			dbTypes = new PostgresTypes();
			if (!StringUtils.isAllBlank(pmsConfig.getDatabaseSocketFactory())) {
				hikariConfig.addDataSourceProperty("socketFactory", pmsConfig.getDatabaseSocketFactory());
				hikariConfig.addDataSourceProperty("socketFactoryArg", pmsConfig.getDatabaseSocketFactoryArg());
				hikariConfig.addDataSourceProperty("sslMode", "disable");
				logger.info("database socketFactory : {} ", pmsConfig.getDatabaseSocketFactory());
				logger.info("database socketFactoryArg : {} ", pmsConfig.getDatabaseSocketFactoryArg());
				logger.info("database url : {} ", pmsConfig.getDatabaseUrl());
			}
		} else {
			logger.warn("unknown database");
			dbTypes = null;
		}

		ds = new HikariDataSource(hikariConfig);
	}

	/**
	 * Delivers a database connection from JDBC driver instead of a pooled connection.
	 *
	 * @return
	 * @throws SQLException
	 */
	public Connection getStandaloneConnection() throws SQLException {
		PmsConfiguration c = PMS.getConfiguration();
		Properties props = new Properties();
		props.setProperty("user", c.getDatabaseUser());
		props.setProperty("password", c.getDatabasePassword());
		props.setProperty("ssl", "false");

		if (isPostgresBackend() && !StringUtils.isAllBlank(c.getDatabaseSocketFactory())) {
			logger.info("adding socket factory class and arg's");
			props.setProperty("socketFactory", c.getDatabaseSocketFactory());
			props.setProperty("socketFactoryArg", c.getDatabaseSocketFactoryArg());
			props.setProperty("sslMode", "disable");
		}

		Connection conn = DriverManager.getConnection(c.getDatabaseUrl(), props);
		return conn;
	}

	public boolean isH2dbBackend() {
		return "h2db".equalsIgnoreCase(PMS.getConfiguration().getDatabaseBackend());
	}

	public boolean isPostgresBackend() {
		return "pg".equalsIgnoreCase(PMS.getConfiguration().getDatabaseBackend());
	}

	private void h2dbInit(String name, String user, String password) {
		status = DatabaseStatus.CLOSED;
		dbName = name;
		dbUser = user;
		dbPassword = password;
		File profileDirectory = new File(CONFIGURATION.getProfileDirectory());
		dbDir = new File(PMS.isRunningTests() || profileDirectory.isDirectory() ? CONFIGURATION.getProfileDirectory() : null, "database").getAbsolutePath();
		url = Constants.START_URL + dbDir + File.separator + dbName + ";DB_CLOSE_ON_EXIT=FALSE";
		logger.info("Using database engine version {}.{}.{}", Constants.VERSION_MAJOR, Constants.VERSION_MINOR, Constants.BUILD_ID);

		if (CONFIGURATION.getDatabaseLogging()) {
			url += ";TRACE_LEVEL_FILE=3";
			logger.info("Database logging is enabled");
		} else if (logger.isDebugEnabled()) {
			logger.debug("Database logging is disabled");
		}

		logger.debug("Using \"{}\" database URL: {}", dbName, url);
		logger.info("Using \"{}\" database located at: \"{}\"", dbName, dbDir);

		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			logger.error(null, e);
		}
	}

	public Database(String name) {
		this(name, PMS.getConfiguration().getDatabaseUser(), PMS.getConfiguration().getDatabasePassword());
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
		return ds.getConnection();
	}

	public int getActiveConnections() throws SQLException {
		return 0;
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
		deleteDatabaseLock();
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
			if (isH2dbBackend()) {
				conn = handleOpenExceptionH2db(conn, needRetry, se);
			}
			logger.error("db open", se);
		} finally {
			close(conn);
		}
	}

	private Connection handleOpenExceptionH2db(Connection conn, boolean needRetry, SQLException se) {
		status = DatabaseStatus.OPENFAILED;
		final File dbFile = new File(dbDir + File.separator + dbName + ".data.db");
		final File dbDirectory = new File(dbDir);
		if (se.getErrorCode() == 50000 && se.getMessage().contains("format 1 is smaller than the supported format 2")) {
			logger.info("The database need a migration to h2 format 2");
			migrateDatabaseVersion2(true);
			needRetry = true;
		} else if (dbFile.exists() || (se.getErrorCode() == 90048)) { // Cache is corrupt or a wrong version, so delete it
			FileUtils.deleteQuietly(dbDirectory);
			if (!dbDirectory.exists()) {
				logger.info("The database has been deleted because it was corrupt or had the wrong version");
				needRetry = true;
			} else {
				showMessageDialog("DamagedCacheCantBeDeleted", dbDir);
				logger.error("Damaged cache can't be deleted. Stop the program and delete the folder \"" + dbDir + "\" manually");
			}
		} else {
			logger.debug("Database connection error, retrying in 10 seconds");
			sleep(10000);
			needRetry = true;
		}
		if (needRetry) {
			try {
				conn = getConnection();
				status = DatabaseStatus.OPENED;
			} catch (SQLException se2) {
				showMessageDialog("TheLocalCacheCouldNotStarted", dbDir);
				logger.debug("", se2);
			}
		}
		return conn;
	}

	abstract void onOpening(boolean force);

	abstract void onOpeningFail(boolean force);

	private void showMessageDialog(String message, String dbDir) {
		if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
			try {
				JOptionPane.showMessageDialog(
					SwingUtilities.getWindowAncestor((Component) PMS.get().getFrame()),
					String.format(Messages.getString(message), dbDir),
					Messages.getString("Error"),
					JOptionPane.ERROR_MESSAGE
				);
			} catch (NullPointerException e1) {
				logger.debug("Failed to show database connection error message, probably because GUI is not initialized yet. Error was {}", e1);
			}
		}
	}

	public void close() {
		status = DatabaseStatus.CLOSING;
		try {
			Thread.sleep(50);
			int activeConnections = getActiveConnections();
			int maxAttempts = 10;
			int curAttempts = 1;
			//allow threads to finish
			while (activeConnections > 0 && curAttempts <= maxAttempts) {
				logger.trace("Database shutdown waiting 500 ms ({}/{}) for {} connections to close", curAttempts, maxAttempts, activeConnections);
				Thread.sleep(500);
				activeConnections = getActiveConnections();
				curAttempts++;
			}
			if (activeConnections > 0) {
				logger.debug("Database shutdown will kill remaining connections ({}), db errors may occurs", activeConnections);
			}
		} catch (SQLException e) {
			logger.error("Waiting DB connections", e);
		} catch (InterruptedException e) {
			logger.debug("Interrupted while shutting down database..");
			logger.trace("", e);
		}

		try (Statement stmt = getConnection().createStatement()) {
			stmt.execute("SHUTDOWN COMPACT");
		} catch (SQLException e1) {
			logger.error("compacting DB ", e1);
		}
		status = DatabaseStatus.CLOSED;
	}

	/**
	 * Migrate the h2 database from version 1.4.197 to version 2.
	 *
	 */
	private void migrateDatabaseVersion2(boolean deleteBackup) {
		logger.info("Migrating database to v{}", Constants.VERSION);
		if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
			try {
				PMS.get().getFrame().setStatusLine("Migrating database to v" + Constants.VERSION);
			} catch (NullPointerException e) {
				logger.debug("Failed to set status, probably because GUI is not initialized yet. Error was {}", e);
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
			logger.info("The database successfully migrated to version {}", Constants.VERSION);
		} catch (Exception e) {
			logger.error(
				"Database migration failed: {}",
				e.getMessage()
			);
			logger.trace("", e);
		}
	}

	/**
	 * Delete old database lock file used in previous versions.
	 */
	private void deleteDatabaseLock() {
		final File dbLockFile = new File(dbDir + File.separator + dbName + ".lock.db");
		if (dbLockFile.exists()) {
			dbLockFile.delete();
		}
	}

	/**
	 * Create the database report.
	 * Use an automatic H2database profiling tool to make a report at the end of the logging file
	 * converted to the "logging_report.txt" in the database directory.
	 */
	public void createDatabaseReport() {
		try {
			if (isH2dbBackend()) {
				ConvertTraceFile.main("-traceFile", getDatabaseFilename() + ".trace.db",
					"-script", getDatabaseFilename() + "_logging_report.txt");
			}
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
