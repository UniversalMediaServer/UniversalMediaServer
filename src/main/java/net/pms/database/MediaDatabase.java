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
public class MediaDatabase extends Database implements Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaDatabase.class);
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
	public MediaDatabase() {
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

}
