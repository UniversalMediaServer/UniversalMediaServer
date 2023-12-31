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
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.RootFolder;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class MediaDatabase extends Database {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaDatabase.class);
	public static final String DATABASE_NAME = "medias";
	/**
	 * Pointer to the instantiated MediaDatabase.
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
	public final void onOpening(boolean force) {
		try {
			checkTables(force);
		} catch (SQLException se) {
			LOGGER.error("Error checking tables: " + se.getMessage());
			LOGGER.trace("", se);
			status = DatabaseStatus.CLOSED;
		}
	}

	@Override
	public final void onOpeningFail(boolean force) {
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
	public final synchronized void checkTables(boolean force) throws SQLException {
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
				MediaTableVideoMetadata.checkTable(connection);
				MediaTableSubtracks.checkTable(connection);
				MediaTableChapters.checkTable(connection);
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
				MediaTableVideoMetadataLocalized.checkTable(connection);

				// Audio Metadata
				MediaTableAudiotracks.checkTable(connection);
				MediaTableMusicBrainzReleaseLike.checkTable(connection);
			}
			tablesChecked = true;
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
		dropTableAndConstraint(connection, MediaTableChapters.TABLE_NAME);

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
		dropTableAndConstraint(connection, MediaTableVideoMetadataLocalized.TABLE_NAME);

		// Audio Metadata
		dropTableAndConstraint(connection, MediaTableAudiotracks.TABLE_NAME);
	}

	/**
	 * Returns the MediaDatabase instance.
	 * Will create the database instance as needed.
	 *
	 * @return {@link net.pms.database.MediaDatabase}
	 */
	public static synchronized MediaDatabase get() {
		if (instance == null) {
			instance = new MediaDatabase();
		}
		return instance;
	}

	/**
	 * Initialize the MediaDatabase instance.
	 * Will initialize the database instance as needed.
	 */
	public static synchronized void init() {
		get().init(false);
	}

	/**
	 * Initialize the MediaDatabase instance.
	 * Will initialize the database instance as needed.
	 * Will check all tables.
	 */
	public static synchronized void initForce() {
		get().init(true);
	}

	/**
	 * Check the MediaDatabase instance.
	 *
	 * @return <code>true</code> if the MediaDatabase is instantiated
	 * , <code>false</code> otherwise
	 */
	public static boolean isInstantiated() {
		return instance != null;
	}

	/**
	 * Check the MediaDatabase instance availability.
	 *
	 * @return {@code true } if the MediaDatabase is instantiated and opened
	 * , <code>false</code> otherwise
	 */
	public static boolean isAvailable() {
		return isInstantiated() && instance.isOpened();
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
	 * Reset the media database cache.
	 * Recreate all tables related to media cache except files status.
	 * @throws java.sql.SQLException
	 */
	public static synchronized void resetCache() throws SQLException {
		if (instance != null) {
			instance.reInitTablesExceptFilesStatus();
		}
	}

	/**
	 * Shutdown the MediaDatabase database.
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
