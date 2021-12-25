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

import java.sql.*;
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.newgui.SharedContentTab;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class MediasDatabase extends Database implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediasDatabase.class);
	private static final ReadWriteLock DATABASE_LOCK = new ReentrantReadWriteLock(true);

	private Thread scanner;

	private static boolean tablesChecked = false;

	/**
	 * Initializes the database connection pool for the current profile.
	 *
	 * Will create the "UMS-tests" profile directory and put the database
	 * in there if it doesn't exist, in order to prevent overwriting
	 * real databases.
	 */
	public MediasDatabase() {
		super("medias");
	}

	/**
	 * Initialized the database for use, performing checks and creating a new
	 * database if necessary.
	 *
	 * @param force whether to recreate the database regardless of necessity.
	 */
	@Override
	public synchronized void init(boolean force) {
		super.init(force);
		try {
			checkTables(true);
		} catch (SQLException se) {
			LOGGER.error("Error checking tables: " + se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public boolean isScanLibraryRunning() {
		return scanner != null && scanner.isAlive();
	}

	public void scanLibrary() {
		if (isScanLibraryRunning()) {
			LOGGER.info("Cannot start library scanner: A scan is already in progress");
		} else {
			scanner = new Thread(this, "Library Scanner");
			scanner.setPriority(Thread.MIN_PRIORITY);
			scanner.start();
			SharedContentTab.setScanLibraryBusy();
		}
	}

	public void stopScanLibrary() {
		if (isScanLibraryRunning()) {
			PMS.get().getRootFolder(null).stopScan();
		}
	}

	@Override
	public void run() {
		try {
			PMS.get().getRootFolder(null).scan();
		} catch (Exception e) {
			LOGGER.error("Unhandled exception during library scan: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	//table related functions
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
					MediasTableTablesVersions.checkTable(connection);

					// Files and metadata
					MediasTableMetadata.checkTable(connection);
					MediasTableFiles.checkTable(connection);
					MediasTableSubtracks.checkTable(connection);
					MediasTableRegexpRules.checkTable(connection);

					MediasTableMusicBrainzReleases.checkTable(connection);
					MediasTableCoverArtArchive.checkTable(connection);
					MediasTableFilesStatus.checkTable(connection);
					MediasTableThumbnails.checkTable(connection);

					MediasTableTVSeries.checkTable(connection);
					MediasTableFailedLookups.checkTable(connection);

					// Video metadata tables
					MediasTableVideoMetadataActors.checkTable(connection);
					MediasTableVideoMetadataAwards.checkTable(connection);
					MediasTableVideoMetadataCountries.checkTable(connection);
					MediasTableVideoMetadataDirectors.checkTable(connection);
					MediasTableVideoMetadataIMDbRating.checkTable(connection);
					MediasTableVideoMetadataGenres.checkTable(connection);
					MediasTableVideoMetadataPosters.checkTable(connection);
					MediasTableVideoMetadataProduction.checkTable(connection);
					MediasTableVideoMetadataRated.checkTable(connection);
					MediasTableVideoMetadataRatings.checkTable(connection);
					MediasTableVideoMetadataReleased.checkTable(connection);

					// Audio Metadata
					MediasTableAudiotracks.checkTable(connection);
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
		dropTableAndConstraint(connection, MediasTableRegexpRules.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableFilesStatus.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableMetadata.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableFiles.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableTablesVersions.TABLE_NAME);
	}

	public static synchronized void dropAllTablesExceptFilesStatus(Connection connection) {
		dropTableAndConstraint(connection, MediasTableMusicBrainzReleases.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableCoverArtArchive.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableThumbnails.TABLE_NAME);

		dropTableAndConstraint(connection, MediasTableTVSeries.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableFailedLookups.TABLE_NAME);

		// Video metadata tables
		dropTableAndConstraint(connection, MediasTableVideoMetadataActors.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataAwards.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataCountries.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataDirectors.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataIMDbRating.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataGenres.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataPosters.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataProduction.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataRated.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataRatings.TABLE_NAME);
		dropTableAndConstraint(connection, MediasTableVideoMetadataReleased.TABLE_NAME);

		// Audio Metadata
		dropTableAndConstraint(connection, MediasTableAudiotracks.TABLE_NAME);
	}

}
