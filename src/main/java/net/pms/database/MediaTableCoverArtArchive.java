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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	protected static final String TABLE_NAME = "COVER_ART_ARCHIVE";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_MBID = "MBID";
	private static final String COL_COVER = "COVER";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_MBID = TABLE_NAME + "." + COL_MBID;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_BY_MBID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_MBID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_COVER_MODIFIED_BY_MBID = SELECT + COL_COVER + COMMA + COL_MODIFIED + FROM + TABLE_NAME + WHERE + TABLE_COL_MBID + EQUAL + PARAMETER + LIMIT_1;

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
				case 1 -> {
					//rename indexes
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_MBID + IDX_MARKER + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MBID + IDX_MARKER);
				}
				default -> {
					getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, TABLE_VERSION);
					throw new IllegalStateException(
							getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
				}
			}
			//case 1: Alter table to version 2
					}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID           + IDENTITY          + PRIMARY_KEY + COMMA +
				COL_MODIFIED     + TIMESTAMP                       + COMMA +
				COL_MBID         + VARCHAR_36                      + COMMA +
				COL_COVER        + BLOB                            +
			")",
			CREATE_INDEX + TABLE_NAME + COL_MBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_MBID + ")"
		);
	}

	/**
	 * A type class for returning results from Cover Art Archive database
	 * lookup.
	 */
	public static class CoverArtArchiveResult {
		private final boolean found;
		private final Timestamp modified;
		private final byte[] cover;

		public CoverArtArchiveResult(final boolean found, final Timestamp modified, final byte[] cover) {
			this.found = found;
			this.modified = modified;
			this.cover = cover;
		}

		public boolean isFound() {
			return found;
		}

		public long getModifiedTime() {
			return modified.getTime();
		}

		public boolean hasCoverBytes() {
			return cover != null;
		}

		public byte[] getCoverBytes() {
			return cover;
		}
	}

	/**
	 * Stores the cover {@link Blob} with the given mBID in the database
	 *
	 * @param mBID the MBID (releaseId) to store
	 * @param cover the cover as a {@link Blob}
	 */
	public static void writeMBID(final String mBID, InputStream cover) {
		if (StringUtils.isBlank(mBID)) {
			return;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL_BY_MBID, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, mBID);
				if (trace) {
					LOGGER.trace("Searching for Cover Art Archive cover with \"{}\" before update", statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						if (trace) {
							LOGGER.trace("Inserting new cover for MBID \"{}\"", mBID);
						}
						result.moveToInsertRow();
						result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
						result.updateString(COL_MBID, mBID);
						if (cover != null) {
							result.updateBinaryStream(COL_COVER, cover);
						}
						result.insertRow();
					} else if (cover != null || result.getBlob(COL_COVER) == null) {
						if (trace) {
							LOGGER.trace("Updating cover for MBID \"{}\"", mBID);
						}
						result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
						if (cover != null) {
							result.updateBinaryStream(COL_COVER, cover);
						} else {
							result.updateNull(COL_COVER);
						}
						result.updateRow();
					} else if (trace) {
						LOGGER.trace("Leaving row {} alone since previous information seems better", result.getInt(COL_ID));
					}
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
	 * @param mBID the MBID {@link String} to search with
	 *
	 * @return The result of the search, never <code>null</code>
	 */
	public static CoverArtArchiveResult findMBID(final String mBID) {
		if (StringUtils.isBlank(mBID)) {
			return new CoverArtArchiveResult(false, null, null);
		}
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_COVER_MODIFIED_BY_MBID, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, mBID);
				if (trace) {
					LOGGER.trace("Searching for Cover Art Archive cover with \"{}\"", statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						return new CoverArtArchiveResult(true, result.getTimestamp(COL_MODIFIED), result.getBytes(COL_COVER));
					}
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
		}
		return new CoverArtArchiveResult(false, null, null);
	}

	/**
	 * Checks if cover exists for given musicBrainz releseId.
	 *
	 * @param mbReleaseId
	 * @return
	 */
	public static boolean hasCover(String mbReleaseId) {
		if (StringUtils.isBlank(mbReleaseId)) {
			return false;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_COVER_MODIFIED_BY_MBID, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, mbReleaseId);
				try (ResultSet result = statement.executeQuery()) {
					return result.next();
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("", e);
		}
		return false;
	}
}
