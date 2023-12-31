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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.gui.GuiManager;
import net.pms.util.UMSUtils;
import org.apache.commons.io.FileUtils;
import org.h2.engine.Constants;
import org.h2.tools.ConvertTraceFile;
import org.h2.tools.Upgrade;
import org.h2.util.Profiler;
import org.h2.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseEmbedded {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEmbedded.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Profiler PROFILER = new Profiler();
	private static boolean collecting = false;

	/**
	 * This class is not meant to be instantiated.
	 */
	private DatabaseEmbedded() {
	}

	public static String getJdbcUrl(String name) {
		startCollectingIfNeeded();
		String dbDir = getDbDir();
		String url = Constants.START_URL + dbDir + File.separator + name + ";DB_CLOSE_ON_EXIT=FALSE";
		LOGGER.info("Using database engine version {}.{}.{}", Constants.VERSION_MAJOR, Constants.VERSION_MINOR, Constants.BUILD_ID);
		int cacheSize = CONFIGURATION.getDatabaseMediaCacheSize();
		if (cacheSize < 0) {
			//no value set, use 64 MB per GB
			cacheSize = Utils.scaleForAvailableMemory(65536);
		}
		LOGGER.info("Database may use {} MB for caching", Math.round((cacheSize / 1024)));
		url += ";CACHE_SIZE=" + cacheSize;

		if (CONFIGURATION.isDatabaseMediaUseCacheSoft()) {
			LOGGER.info("Database use soft cache");
			url += ";CACHE_TYPE=SOFT_" + Constants.CACHE_TYPE_DEFAULT;
		} else {
			url += ";CACHE_TYPE=" + Constants.CACHE_TYPE_DEFAULT;
		}

		if (CONFIGURATION.isDatabaseMediaUseMemoryIndexes()) {
			url += ";DEFAULT_TABLE_TYPE=MEMORY";
			LOGGER.info("Database indexes in memory is enabled");
		} else {
			url += ";DEFAULT_TABLE_TYPE=CACHED";
		}

		if (CONFIGURATION.getDatabaseLogging()) {
			url += ";TRACE_LEVEL_FILE=3";
			LOGGER.info("Database logging is enabled");
		} else if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Database logging is disabled");
		}

		LOGGER.debug("Using \"{}\" database URL: {}", name, url);
		LOGGER.info("Using \"{}\" database located at: \"{}\"", name, dbDir);

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

	public static void shutdown(Connection connection) {
		logProfilerIfNeeded();
		try (Statement stmt = connection.createStatement()) {
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
			UMSUtils.sleep(10000);
			return true;
		}
	}

	/**
	 * Migrate the h2 database from version 1.4.197 to version 2.
	 *
	 */
	private static void migrateDatabaseVersion2(boolean deleteBackup, String dbName) {
		LOGGER.info("Migrating database to v{}", Constants.VERSION);
		GuiManager.setStatusLine("Migrating database to v" + Constants.VERSION);
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
			LOGGER.trace("Failed to create trace database logging report");
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

	public static void checkTableStorageType(final Connection connection, String dbName) {
		try {
			boolean memoryStorage = CONFIGURATION.isDatabaseMediaUseMemoryIndexes();
			String askedStorageType = memoryStorage ? "MEMORY" : "CACHED";
			boolean upgradeTables = false;
			try (ResultSet rs = connection.getMetaData().getTables(null, "PUBLIC", "%", new String[] {"BASE TABLE"})) {
				while (rs.next()) {
					String tableName = rs.getString("TABLE_NAME");
					try (PreparedStatement statement = connection.prepareStatement("SELECT STORAGE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ? LIMIT 1")) {
						statement.setString(1, tableName);
						try (ResultSet rs2 = statement.executeQuery()) {
							if (rs2.first()) {
								String storageType = rs2.getString("STORAGE_TYPE");
								if (!askedStorageType.equals(storageType)) {
									upgradeTables = true;
									break;
								}
							}
						}
					}
				}
			}
			if (upgradeTables) {
				LOGGER.info("Database will update to {} table storage type", askedStorageType);
				try (Statement stBackup = connection.createStatement()) {
					boolean hasError = false;
					ResultSet backup = stBackup.executeQuery("SCRIPT NOPASSWORDS NOSETTINGS NOVERSION DROP");
					try (Statement stRestore = connection.createStatement()) {
						while (backup.next()) {
							String sql = backup.getString(1);
							if (!memoryStorage && sql.startsWith("CREATE MEMORY TABLE")) {
								sql = sql.replace("CREATE MEMORY TABLE", "CREATE CACHED TABLE");
							} else if (memoryStorage && sql.startsWith("CREATE CACHED TABLE")) {
								sql = sql.replace("CREATE CACHED TABLE", "CREATE MEMORY TABLE");
							}
							try {
								stRestore.execute(sql);
							} catch (SQLException e) {
								hasError = true;
							}
						}
					}
					if (hasError) {
						//dump to disk
						LOGGER.info("Database update error, backup file will be created");
						String dbFilename = getDatabaseFilename(dbName);
						String backupFilename = dbFilename + ".script.sql";
						File backupFile = new File(backupFilename);
						try (BufferedWriter fileStream = new BufferedWriter(new FileWriter(backupFile))) {
							backup.beforeFirst(); //first is user
							while (backup.next()) {
								String sql = backup.getString(1);
								if (!memoryStorage && sql.startsWith("CREATE MEMORY TABLE")) {
									sql = sql.replace("CREATE MEMORY TABLE", "CREATE CACHED TABLE");
								} else if (memoryStorage && sql.startsWith("CREATE CACHED TABLE")) {
									sql = sql.replace("CREATE CACHED TABLE", "CREATE MEMORY TABLE");
								}
								fileStream.write(sql);
								fileStream.newLine();
							}
							fileStream.flush();
						} catch (IOException ex) {
						}
					} else {
						LOGGER.info("Database updated successfully");
					}
				}
			}
		} catch (SQLException ex) {
			LOGGER.error("Database error on memory indexes change", ex);
		}
	}
}
