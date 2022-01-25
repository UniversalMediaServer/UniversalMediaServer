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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class is responsible for managing the Cover Art Archive table. It
 * does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author Nadahar
 */

public final class MediaTableCoverArtArchive extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableCoverArtArchive.class);
	public static final String TABLE_NAME = "COVER_ART_ARCHIVE";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (tableExists(connection, TABLE_NAME)) {
			Integer version = MediaTableTablesVersions.getTableVersion(connection, TABLE_NAME);
			if (version != null) {
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
				}
			} else {
				LOGGER.warn(LOG_TABLE_UNKNOWN_VERSION_RECREATE, DATABASE_NAME, TABLE_NAME);
				dropTable(connection, TABLE_NAME);
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	/**
	 * This method <strong>MUST</strong> be updated if the table definition are
	 * altered. The changes for each version in the form of
	 * <code>ALTER TABLE</code> must be implemented here.
	 *
	 * @param connection the {@link Connection} to use
	 * @param currentVersion the version to upgrade <strong>from</strong>
	 *
	 * @throws SQLException
	 */
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				//case 1: Alter table to version 2
				default:
					getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, TABLE_VERSION);
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				"ID				IDENTITY		PRIMARY KEY, " +
				"MODIFIED		DATETIME, " +
				"MBID			VARCHAR(36), " +
				"COVER			BLOB" +
			")",
			"CREATE INDEX MBID_IDX ON " + TABLE_NAME + "(MBID)"
		);
	}

	/**
	 * A type class for returning results from Cover Art Archive database
	 * lookup.
	 */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public static class CoverArtArchiveResult {

		public boolean found = false;
		public Timestamp modified = null;
		public byte[] cover = null;

		@SuppressFBWarnings("EI_EXPOSE_REP2")
		public CoverArtArchiveResult(final boolean found, final Timestamp modified, final byte[] cover) {
			this.found = found;
			this.modified = modified;
			this.cover = cover;
		}
	}

	private static String contructMBIDWhere(final String mBID) {
		return " WHERE MBID" + sqlNullIfBlank(mBID, true, false);
	}

	/**
	 * Stores the cover {@link Blob} with the given mBID in the database
	 *
	 * @param connection the db connection
	 * @param mBID the MBID to store
	 * @param cover the cover as a {@link Blob}
	 */
	public static void writeMBID(final Connection connection, final String mBID, final byte[] cover) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			String query = "SELECT * FROM " + TABLE_NAME + contructMBIDWhere(mBID) + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching for Cover Art Archive cover with \"{}\" before update", query);
			}

			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery(query)) {
					if (result.next()) {
						if (cover != null || result.getBlob("COVER") == null) {
							if (trace) {
								LOGGER.trace("Updating cover for MBID \"{}\"", mBID);
							}
							result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
							if (cover != null) {
								result.updateBytes("COVER", cover);
							} else {
								result.updateNull("COVER");
							}
							result.updateRow();
						} else if (trace) {
							LOGGER.trace("Leaving row {} alone since previous information seems better", result.getInt("ID"));
						}
					} else {
						if (trace) {
							LOGGER.trace("Inserting new cover for MBID \"{}\"", mBID);
						}

						result.moveToInsertRow();
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						result.updateString("MBID", mBID);
						if (cover != null) {
							result.updateBytes("COVER", cover);
						}
						result.insertRow();
					}
				} finally {
					connection.commit();
				}
			}
		} catch (SQLException e) {
			LOGGER.error(
				LOG_ERROR_WHILE_VAR_IN,
				DATABASE_NAME,
				"writing Cover Art Archive cover for MBID",
				mBID,
				TABLE_NAME,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Looks up cover in the table based on the given MBID.
	 * Never returns <code>null</code>
	 *
	 * @param connection the db connection
	 * @param mBID the MBID {@link String} to search with
	 *
	 * @return The result of the search, never <code>null</code>
	 */
	public static CoverArtArchiveResult findMBID(final Connection connection, final String mBID) {
		boolean trace = LOGGER.isTraceEnabled();
		CoverArtArchiveResult result;

		try {
			String query = "SELECT COVER, MODIFIED FROM " + TABLE_NAME + contructMBIDWhere(mBID) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching for cover with \"{}\"", query);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query)
			 ) {
				if (resultSet.next()) {
					result = new CoverArtArchiveResult(true, resultSet.getTimestamp("MODIFIED"), resultSet.getBytes("COVER"));
				} else {
					result = new CoverArtArchiveResult(false, null, null);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(
				LOG_ERROR_WHILE_VAR_IN,
				DATABASE_NAME,
				"looking up Cover Art Archive cover for MBID",
				mBID,
				TABLE_NAME,
				e.getMessage()
			);
			LOGGER.trace("", e);
			result = new CoverArtArchiveResult(false, null, null);
		}

		return result;
	}
}
