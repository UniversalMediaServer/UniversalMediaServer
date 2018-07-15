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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.h2.api.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNABinaryThumbnail;
import net.pms.dlna.DLNAThumbnail;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This class is responsible for managing the Cover Art Archive table. It
 * does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author Nadahar
 */

public final class TableCoverArtArchive extends Tables{

	/**
	 * tableLock is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock tableLock = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableCoverArtArchive.class);
	private static final String TABLE_NAME = "COVER_ART_ARCHIVE";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 2;

	// No instantiation
	private TableCoverArtArchive() {
	}

	/**
	 * A type class for returning results from Cover Art Archive database
	 * lookup.
	 */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public static class CoverArtArchiveEntry {

		public boolean found;
		public Timestamp modified;
		public byte[] cover;
		public DLNABinaryThumbnail thumbnail;

		@SuppressFBWarnings("EI_EXPOSE_REP2")
		public CoverArtArchiveEntry(boolean found, Timestamp modified, byte[] cover, DLNABinaryThumbnail thumbnail) {
			this.found = found;
			this.modified = modified;
			this.cover = cover;
			this.thumbnail = thumbnail;
		}
	}

	private static String contructMBIDWhere(final String mBID) {
		return " WHERE MBID" + sqlNullIfBlank(mBID, true, false);
	}

	/**
	 * Stores the cover {@link Blob} with the given mBID in the database
	 *
	 * @param mBID the MBID to store.
	 * @param cover the cover as a byte array.
	 * @param thumbnail the {@link DLNABinaryThumbnail}.
	 */
	public static void writeMBID(final String mBID, final byte[] cover, DLNABinaryThumbnail thumbnail) {
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = database.getConnection()) {
			String query = "SELECT * FROM " + TABLE_NAME + contructMBIDWhere(mBID);
			if (trace) {
				LOGGER.trace("Searching for Cover Art Archive cover with \"{}\" before update", query);
			}

			tableLock.writeLock().lock();
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
							if (thumbnail == null) {
								result.updateNull("THUMBNAIL");
							} else {
								result.updateObject("THUMBNAIL", thumbnail);
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
						if (thumbnail != null) {
							result.updateObject("THUMBNAIL", thumbnail);
						}
						result.insertRow();
					}
				} finally {
					connection.commit();
				}
			} finally {
				tableLock.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error while writing Cover Art Archive cover for MBID \"{}\": {}", mBID, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Looks up cover in the table based on the given MBID. Never returns
	 * <code>null</code>
	 *
	 * @param mBID the MBID {@link String} to search with
	 *
	 * @return The result of the search, never <code>null</code>
	 */
	public static CoverArtArchiveEntry findMBID(final String mBID) {
		boolean trace = LOGGER.isTraceEnabled();
		CoverArtArchiveEntry result;

		try (Connection connection = database.getConnection()) {
			String query = "SELECT COVER, MODIFIED, THUMBNAIL FROM " + TABLE_NAME + contructMBIDWhere(mBID);

			if (trace) {
				LOGGER.trace("Searching for cover with \"{}\"", query);
			}

			tableLock.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						DLNABinaryThumbnail thumbnail;
						try {
							thumbnail = (DLNABinaryThumbnail) resultSet.getObject("THUMBNAIL");
						} catch (Exception e) {
							thumbnail = null;
							if (trace) {
								LOGGER.trace(
									"Deserialization failed for MBID \"{}\", returning null: {}",
									mBID,
									e.getMessage()
								);
							}
						}
						result = new CoverArtArchiveEntry(
							true,
							resultSet.getTimestamp("MODIFIED"),
							resultSet.getBytes("COVER"),
							thumbnail
						);
					} else {
						result = new CoverArtArchiveEntry(false, null, null, null);
					}
				}
			} finally {
				tableLock.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while looking up Cover Art Archive cover for MBID \"{}\": {}",
				mBID,
				e.getMessage()
			);
			LOGGER.trace("", e);
			result = new CoverArtArchiveEntry(false, null, null, null);
		}

		return result;
	}

	/**
	 * Looks up thumbnail with the specified {@code MBID}.
	 *
	 * @param mBID the MBID to look up.
	 *
	 * @return The {@link DLNAThumbnail} or {@code null}.
	 * @throws SQLException If a SQL error occurs during the operation.
	 */
	public static DLNAThumbnail getThumbnail(String mBID) throws SQLException {
		if (mBID == null) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = database.getConnection()) {
			String query = "SELECT THUMBNAIL FROM " + TABLE_NAME + " WHERE MBID = " + sqlQuote(mBID);

			if (trace) {
				LOGGER.trace("Looking up thumbnail for MBID \"{}\" with \"{}\"", mBID, query);
			}

			tableLock.readLock().lock();
			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query);
			) {
				if (resultSet.next()) {
					try {
						return (DLNABinaryThumbnail) resultSet.getObject("THUMBNAIL");
					} catch (Exception e) {
						if (trace) {
							LOGGER.trace(
								"Deserialization failed for MBID \"{}\", returning null: {}",
								mBID,
								e.getMessage()
							);
						}
						return null;
					}
				}
				if (trace) {
					LOGGER.trace("No thumbnail found for MBID \"{}\"", mBID);
				}
				return null;
			} finally {
				tableLock.readLock().unlock();
			}
		}
	}

	/**
	 * Looks up thumbnail with the specified {@code MBID}.
	 *
	 * @param mBID the MBID to look up.
	 * @param thumbnail the {@link DLNABinaryThumbnail} to store.
	 *
	 * @throws SQLException If a SQL error occurs during the operation.
	 * @throws IllegalArgumentException If {@code mBID} is {@code null}.
	 */
	public static void updateThumbnail(String mBID, DLNABinaryThumbnail thumbnail) throws SQLException {
		if (mBID == null) {
			throw new IllegalArgumentException("mBID cannot be null");
		}
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = database.getConnection()) {
			String query = "SELECT * FROM " + TABLE_NAME + " WHERE MBID = " + sqlQuote(mBID);
			if (trace) {
				LOGGER.trace("Searching for Cover Art Archive cover with \"{}\" before update", query);
			}

			tableLock.writeLock().lock();
			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				try (ResultSet result = statement.executeQuery(query)) {
					if (result.next()) {
						if (thumbnail == null) {
							result.updateNull("THUMBNAIL");
						} else {
							result.updateObject("THUMBNAIL", thumbnail);
						}
						result.updateRow();
					} else {
						throw new SQLException("Row for MBID \"" + mBID + "\" not found", "02000", ErrorCode.NO_DATA_AVAILABLE);
					}
				}
			} finally {
				tableLock.writeLock().unlock();
			}
		}
	}

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		tableLock.writeLock().lock();
		try {
			if (tableExists(connection, TABLE_NAME)) {
				Integer version = getTableVersion(connection, TABLE_NAME);
				if (version != null) {
					if (version < TABLE_VERSION) {
						upgradeTable(connection, version);
					} else if (version > TABLE_VERSION) {
						throw new SQLException(
							"Database table \"" + TABLE_NAME +
							"\" is from a newer version of UMS. Please move, rename or delete database file \"" +
							database.getDatabaseFilename() +
							"\" before starting UMS"
						);
					}
				} else {
					LOGGER.warn("Database table \"{}\" has an unknown version and cannot be used. Dropping and recreating table", TABLE_NAME);
					dropTable(connection, TABLE_NAME);
					createCoverArtArchiveTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createCoverArtArchiveTable(connection);
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			tableLock.writeLock().unlock();
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
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		tableLock.writeLock().lock();
		try {
			for (int version = currentVersion;version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
				switch (version) {
					case 1:
						// Version 2 adds field THUMBNAIL.
						Statement statement = connection.createStatement();
						statement.executeUpdate("ALTER TABLE " + TABLE_NAME + " ADD COLUMN THUMBNAIL OTHER");
						break;
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "is missing table upgrade commands from version " +
							version + " to " + TABLE_VERSION
						);
				}
			}
			setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} finally {
			tableLock.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createCoverArtArchiveTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID IDENTITY PRIMARY KEY, " +
					"MODIFIED DATETIME, " +
					"MBID VARCHAR(36), " +
					"COVER BLOB, " +
					"THUMBNAIL OTHER" +
				")");
			statement.execute("CREATE INDEX MBID_IDX ON " + TABLE_NAME + "(MBID)");
		}
	}
}
