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

import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.io.FileUtils;
import org.h2.engine.Constants;
import org.h2.tools.ConvertTraceFile;
import org.h2.tools.Upgrade;
import org.h2.util.Profiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseEmbedded {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEmbedded.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Profiler PROFILER = new Profiler();
	private static boolean collecting = false;

	public static String getJdbcUrl(String name) {
		startCollectingIfNeeded();
		String dbDir = getDbDir();
		String url = Constants.START_URL + dbDir + File.separator + name + ";DB_CLOSE_ON_EXIT=FALSE";
		LOGGER.info("Using database engine version {}.{}.{}", Constants.VERSION_MAJOR, Constants.VERSION_MINOR, Constants.BUILD_ID);
		int cacheSize = CONFIGURATION.getDatabaseMediaCacheSize();
		if (cacheSize < 0) {
			//never set, try to set to 10% of JVM memory if > 500MB
			long maxMemKb = Runtime.getRuntime().maxMemory() / 1024;
			if (maxMemKb > 500000) {
				cacheSize = cacheSize / 10;
			} else {
				cacheSize = 0;
			}
			CONFIGURATION.setDatabaseMediaCacheSize(cacheSize);
		}
		if (cacheSize > 0) {
			cacheSize = (cacheSize * 1000);
			url += ";CACHE_SIZE=" + cacheSize;
		}

		if (CONFIGURATION.getDatabaseLogging()) {
			url += ";TRACE_LEVEL_FILE=3";
			LOGGER.info("Database logging is enabled");
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Database logging is disabled");
		}

		if (CONFIGURATION.isDatabaseMediaUseMemoryIndexes()) {
			url += ";DEFAULT_TABLE_TYPE=MEMORY";
			LOGGER.info("Database indexes in memory is enabled");
		}

		LOGGER.debug("Using \"{}\" database URL: {}", name, url);
		LOGGER.info("Using \"{}\" database located at: \"{}\"", name, dbDir);

		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			LOGGER.error(null, e);
		}
		return url;
	}

	private static String getDbDir() {
		File profileDirectory = new File(CONFIGURATION.getProfileDirectory());
		return new File(PMS.isRunningTests() || profileDirectory.isDirectory() ? CONFIGURATION.getProfileDirectory() : null, "database").getAbsolutePath();
	}

	public static String getDbUser() {
		return "sa";
	}

	public static String getDbPassword() {
		return null;
	}

	public static void close(HikariDataSource ds) {
		logProfilerIfNeeded();
		try (Statement stmt = ds.getConnection().createStatement()) {
			stmt.execute("SHUTDOWN COMPACT");
		} catch (SQLException e1) {
			LOGGER.error("compacting DB ", e1);
		}
	}

	public static boolean openFailed(String dbName, SQLException se) {
		String dbDir = getDbDir();
		final File dbFile = new File(dbDir + File.separator + dbName + ".data.db");
		final File dbDirectory = new File(dbDir);
		if (se.getErrorCode() == 50000 && se.getMessage().contains("format 1 is smaller than the supported format 2")) {
			LOGGER.info("The database need a migration to h2 format 2");
			migrateDatabaseVersion2(true, dbName);
			return true;
		} else if (dbFile.exists() || (se.getErrorCode() == 90048)) { // Cache is corrupt or a wrong version, so delete it
			FileUtils.deleteQuietly(dbDirectory);
			if (!dbDirectory.exists()) {
				LOGGER.info("The database has been deleted because it was corrupt or had the wrong version");
				return true;
			} else {
				Database.showMessageDialog("DamagedCacheCantBeDeleted", dbDir);
				LOGGER.error("Damaged cache can't be deleted. Stop the program and delete the folder \"" + dbDir + "\" manually");
				return false;
			}
		} else {
			LOGGER.debug("Database connection error, retrying in 10 seconds");
			Database.sleep(10000);
			return true;
		}
	}

	/**
	 * Migrate the h2 database from version 1.4.197 to version 2.
	 *
	 */
	private static void migrateDatabaseVersion2(boolean deleteBackup, String dbName) {
		LOGGER.info("Migrating database to v{}", Constants.VERSION);
		if (!net.pms.PMS.isHeadless() && PMS.get().getFrame() != null) {
			try {
				PMS.get().getFrame().setStatusLine("Migrating database to v" + Constants.VERSION);
			} catch (NullPointerException e) {
				LOGGER.debug("Failed to set status, probably because GUI is not initialized yet. Error was {}", e);
			}
		}
		String dbDir = getDbDir();
		String oldUrl = Constants.START_URL + dbDir + File.separator + dbName;
		Properties prprts = new Properties();
		prprts.setProperty("user", getDbUser());
		prprts.setProperty("password", getDbPassword());
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
	 * Delete old database lock file used in previous versions.
	 * @param dbName
	 */
	public static void deleteDatabaseLock(String dbName) {
		String dbDir = getDbDir();
		final File dbLockFile = new File(dbDir + File.separator + dbName + ".lock.db");
		if (dbLockFile.exists()) {
			dbLockFile.delete();
		}
	}

	public static String getDatabaseFilename(String dbName) {
		String dbDir = getDbDir();
		if (dbName == null || dbDir == null) {
			return null;
		}
		return dbDir + File.separator + dbName;
	}

	/**
	 * Create the database report.Use an automatic H2database profiling tool
	 * to make a report at the end of the logging file converted to the
	 * "logging_report.txt" in the database directory.
	 *
	 * @param dbName
	 */
	public static void createDatabaseReport(String dbName) {
		try {
			String dbFilename = getDatabaseFilename(dbName);
			ConvertTraceFile.main("-traceFile", dbFilename + ".trace.db",
					"-script", dbFilename + "_logging_report.txt");
		} catch (SQLException ex) {
		}
	}

	public static void startCollectingIfNeeded() {
		if (CONFIGURATION.getDatabaseLogging() && !collecting) {
			collecting = true;
			PROFILER.startCollecting();
		}
	}

	public static void logProfilerIfNeeded() {
		if (CONFIGURATION.getDatabaseLogging() && collecting) {
			collecting = false;
			LOGGER.trace("-------------------------------------------------------------");
			LOGGER.trace(PROFILER.getTop(5));
			LOGGER.trace("-------------------------------------------------------------");
		}
	}

}
