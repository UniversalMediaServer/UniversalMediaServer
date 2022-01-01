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

import java.sql.*;
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.dlna.RootFolder;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class MediaDatabase extends Database {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaDatabase.class);
	private static final ReadWriteLock DATABASE_LOCK = new ReentrantReadWriteLock(true);
	public static final String DATABASE_NAME = "medias";
	/**
	 * Pointer to the instanciated MediaDatabase.
	 */
	private static MediaDatabase instance = null;
	private static boolean tablesChecked = false;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 */
	public MediaDatabase() {
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
		RootFolder rootFolder = PMS.get().getRootFolder(null);
		if (rootFolder != null) {
			rootFolder.stopScan();
		}
	}

	/**
	 * Checks all child tables for their existence and version and creates or
	 * upgrades as needed.Access to this method is serialized.
	 *
	 * @param force do the check even if it has already happened
	 * @throws SQLException
	 */
	public final void checkTables(boolean force) throws SQLException {
		synchronized (DATABASE_LOCK) {
			if (tablesChecked && !force) {
				LOGGER.debug("Database tables have already been checked, aborting check");
			} else {
				LOGGER.debug("Starting check of database tables");
				try (Connection connection = getConnection()) {
					//Tables Versions (need to be first)
					MediaTableTablesVersions.checkTable(connection);

					// Files and metadata
					MediaTableMetadata.checkTable(connection);
					MediaTableFiles.checkTable(connection);
					MediaTableSubtracks.checkTable(connection);
					MediaTableRegexpRules.checkTable(connection);

					MediaTableMusicBrainzReleases.checkTable(connection);
					MediaTableCoverArtArchive.checkTable(connection);
					MediaTableFilesStatus.checkTable(connection);
					MediaTableThumbnails.checkTable(connection);

					MediaTableTVSeries.checkTable(connection);
					MediaTableFailedLookups.checkTable(connection);

					// Video metadata tables
					MediaTableVideoMetadataActors.checkTable(connection);
					MediaTableVideoMetadataAwards.checkTable(connection);
					MediaTableVideoMetadataCountries.checkTable(connection);
					MediaTableVideoMetadataDirectors.checkTable(connection);
					MediaTableVideoMetadataIMDbRating.checkTable(connection);
					MediaTableVideoMetadataGenres.checkTable(connection);
					MediaTableVideoMetadataPosters.checkTable(connection);
					MediaTableVideoMetadataProduction.checkTable(connection);
					MediaTableVideoMetadataRated.checkTable(connection);
					MediaTableVideoMetadataRatings.checkTable(connection);
					MediaTableVideoMetadataReleased.checkTable(connection);

					// Audio Metadata
					MediaTableAudiotracks.checkTable(connection);
				}
				tablesChecked = true;
			}
		}
	}

	/**
	 * Re-initializes all child tables except files status.
	 *
	 * @throws SQLException
	 */
	public final void reInitTablesExceptFilesStatus() throws SQLException {
		LOGGER.debug("Re-initializing tables");
		try (Connection connection = getConnection()) {
			dropAllTablesExceptFilesStatus(connection);
		}
		checkTables(true);
	}

	public static synchronized void dropAllTables(Connection connection) {
		dropAllTablesExceptFilesStatus(connection);
		dropTableAndConstraint(connection, MediaTableRegexpRules.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableFilesStatus.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableMetadata.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableFiles.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableTablesVersions.TABLE_NAME);
	}

	public static synchronized void dropAllTablesExceptFilesStatus(Connection connection) {
		dropTableAndConstraint(connection, MediaTableMusicBrainzReleases.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableCoverArtArchive.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableThumbnails.TABLE_NAME);

		dropTableAndConstraint(connection, MediaTableTVSeries.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableFailedLookups.TABLE_NAME);

		// Video metadata tables
		dropTableAndConstraint(connection, MediaTableVideoMetadataActors.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataAwards.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataCountries.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataDirectors.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataIMDbRating.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataGenres.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataPosters.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataProduction.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataRated.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataRatings.TABLE_NAME);
		dropTableAndConstraint(connection, MediaTableVideoMetadataReleased.TABLE_NAME);

		// Audio Metadata
		dropTableAndConstraint(connection, MediaTableAudiotracks.TABLE_NAME);
	}

	/**
	 * Returns the MediaDatabase instance.
	 * Will create the database instance as needed.
	 *
	 * @return {@link net.pms.database.MediaDatabase}
	 */
	public static MediaDatabase get() {
		synchronized (DATABASE_LOCK) {
			if (instance == null) {
				instance = new MediaDatabase();
			}
			return instance;
		}
	}

	/**
	 * Initialize the MediaDatabase instance.
	 * Will initialize the database instance as needed.
	 */
	public static void init() {
		synchronized (DATABASE_LOCK) {
			get().init(false);
		}
	}

	/**
	 * Initialize the MediaDatabase instance.
	 * Will initialize the database instance as needed.
	 * Will check all tables.
	 */
	public static void initForce() {
		synchronized (DATABASE_LOCK) {
			get().init(true);
		}
	}

	/**
	 * Check the MediaDatabase instance.
	 *
	 * @return <code>true</code> if the MediaDatabase is instanciated
	 * , <code>false</code> otherwise
	 */
	public static boolean isInstanciated() {
		return instance != null;
	}

	/**
	 * Check the MediaDatabase instance availability.
	 *
	 * @return {@code true } if the MediaDatabase is instanciated and opened
	 * , <code>false</code> otherwise
	 */
	public static boolean isAvailable() {
		return isInstanciated() && instance.isOpened();
	}

	/**
	 * Get a MediaDatabase connection.
	 * Will not try to init the database.
	 * Give a connection only if the database status is OPENED.
	 *
	 * Prevent for init or giving a connection on db closing
	 *
	 * @return A {@link java.sql.Connection} if the MediaDatabase is available, <code>null</code> otherwise
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
	 * Create the database report.Use an automatic H2database profiling tool
	 * to make a report at the end of the logging file converted to the
	 * "logging_report.txt" in the database directory.
	 * @throws java.sql.SQLException
	 */
	public static void resetCache() throws SQLException {
		synchronized (DATABASE_LOCK) {
			if (instance != null) {
				instance.reInitTablesExceptFilesStatus();
			}
		}
	}

	/**
	 * Create the database report.
	 * Use an automatic H2database profiling tool to make a report at the end of the logging file
	 * converted to the "logging_report.txt" in the database directory.
	 */
	public static void createReport() {
		if (instance != null) {
			instance.createDatabaseReport();
		}
	}

	/**
	 * Shutdown the MediaDatabase database.
	 */
	public static void shutdown() {
		synchronized (DATABASE_LOCK) {
			if (instance != null) {
				instance.close();
			}
		}
	}

}
