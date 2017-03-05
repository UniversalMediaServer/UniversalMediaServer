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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.Messages;
import net.pms.PMS;

/**
 * This class is responsible for managing the FilesStatus table. It
 * does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author SubJunk & Nadahar
 * @since 7.0.0
 */
public final class TableFilesStatus extends Tables{
	/**
	 * TABLE_LOCK is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableFilesStatus.class);
	public static final String TABLE_NAME = "FILES_STATUS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 1;

	// No instantiation
	private TableFilesStatus() {
	}

	/**
	 * A type class for returning results from FilesStatus database
	 * lookup.
	 */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public static class FilesStatusResult {
		public boolean found = false;
		public boolean isFullyPlayed = false;

		public FilesStatusResult() {
		}

		@SuppressFBWarnings("EI_EXPOSE_REP2")
		public FilesStatusResult(final boolean found, final boolean isFullyPlayed) {
			this.found = found;
			this.isFullyPlayed = isFullyPlayed;
		}
	}

	/**
	 * Sets whether the file has been fully played.
	 *
	 * @param fullPathToFile
	 * @param isFullyPlayed
	 * @param fileId
	 */
	public static void setFullyPlayed(final String fullPathToFile, final boolean isFullyPlayed, int fileId) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;

		try (Connection connection = database.getConnection()) {
			if (fileId == 0) {
				query = "SELECT ID FROM FILES WHERE FILENAME = " + sqlQuote(fullPathToFile);
				if (trace) {
					LOGGER.trace("Looking up file id for file \"{}\" with \"{}\" before update", fullPathToFile, query);
				}

				TABLE_LOCK.readLock().lock();
				try (Statement statement = connection.createStatement()) {
					try (ResultSet result = statement.executeQuery(query)) {
						if (result.next()) {
							fileId = result.getInt("ID");
						}
					}
				} finally {
					TABLE_LOCK.readLock().unlock();
				}
			}

			// If the entry exists in the FILES table
			if (fileId != 0) {
				query = "SELECT ISFULLYPLAYED FROM " + TABLE_NAME + " WHERE FILEID = " + fileId;
				if (trace) {
					LOGGER.trace("Searching for file with \"{}\" before update", query);
				}

				TABLE_LOCK.writeLock().lock();
				try (Statement statement = connection.createStatement()) {
					try (ResultSet result = statement.executeQuery(query)) {
						if (result.next()) {
							if (result.getBoolean("ISFULLYPLAYED") == isFullyPlayed) {
								if (trace) {
									LOGGER.trace("Found file entry and it already has ISFULLYPLAYED set to {}", result.getBoolean("ISFULLYPLAYED"));
								}
							} else {
								query = "UPDATE " + TABLE_NAME + " SET MODIFIED=CURRENT_TIMESTAMP, ISFULLYPLAYED=" + isFullyPlayed + " WHERE FILEID=" + fileId;
								if (trace) {
									LOGGER.trace("Found the file entry so we will update it with \"{}\"", query);
								}

								Statement statement2 = connection.createStatement();
								statement2.execute(query);
							}
						} else {
							query = "INSERT INTO " + TABLE_NAME + " (FILEID, MODIFIED, ISFULLYPLAYED) VALUES (" + fileId + ", CURRENT_TIMESTAMP, " + isFullyPlayed + ")";
							if (trace) {
								LOGGER.trace("Did not find the file entry so we will insert it with \"{}\"", query);
							}

							Statement statement2 = connection.createStatement();
							statement2.execute(query);
						}
					}
				} finally {
					TABLE_LOCK.writeLock().unlock();
				}
			} else {
				if (trace) {
					LOGGER.trace("Found no match in FILES for \"{}\"", fullPathToFile);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing file status \"{}\" for \"{}\": {}",
				isFullyPlayed,
				fullPathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Sets whether each file within the directory is fully played.
	 *
	 * @param fullPath      the full and escaped path to the directory
	 * @param statusPath    a more readable version of fullPath
	 * @param isFullyPlayed whether to mark fully played or unplayed
	 */
	public static void setDirectoryFullyPlayed(final String fullPath, final String statusPath, final boolean isFullyPlayed) {
		boolean trace = LOGGER.isTraceEnabled();
		String statusLineString = isFullyPlayed ? Messages.getString("FoldTab.75") : Messages.getString("FoldTab.76");
		PMS.get().getFrame().setStatusLine(statusLineString + ": " + statusPath);

		try (Connection connection = database.getConnection()) {
			String query = "SELECT ID, FILENAME FROM FILES WHERE FILENAME LIKE " + sqlQuote(fullPath);
			if (trace) {
				LOGGER.trace("Searching for file with \"{}\" before update", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet result = statement.executeQuery(query)) {
					while (result.next()) {
						setFullyPlayed(result.getString("FILENAME"), isFullyPlayed, result.getInt("ID"));
					}
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing file status \"{}\" for \"{}\": {}",
				isFullyPlayed,
				fullPath,
				e.getMessage()
			);
			LOGGER.trace("", e);
		} finally {
			PMS.get().getFrame().setStatusLine(null);
		}
	}

	/**
	 * Removes an entry based on its FILEID (which is the ID of its
	 * corresponding entry in the FILES table.
	 *
	 * @param fileid the fileid to remove
	 */
	public static void removeEntryFromFileId(final String fileid) {
		try (Connection connection = database.getConnection()) {
			String query = "DELETE FROM " + TABLE_NAME + " WHERE FILEID = " + fileid;
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet result = statement.executeQuery(query)) {
					if (result.next()) {
						LOGGER.trace("Removed entry in FILES_STATUS for fileid \"{}\"", fileid);
					} else {
						LOGGER.trace("Could not find entry in FILES_STATUS for fileid \"{}\"", fileid);
					}
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while removing entry from file status for \"{}\": {}",
				fileid,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	public static FilesStatusResult isFullyPlayed(final String fullPathToFile) {
		boolean trace = LOGGER.isTraceEnabled();
		FilesStatusResult result;

		try (Connection connection = database.getConnection()) {
			String query = "SELECT ISFULLYPLAYED FROM FILES JOIN " + TABLE_NAME + " ON FILES.ID = " + TABLE_NAME + ".FILEID WHERE FILENAME = " + sqlQuote(fullPathToFile);

			if (trace) {
				LOGGER.trace("Searching for file status with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						result = new FilesStatusResult(true, resultSet.getBoolean("ISFULLYPLAYED"));
					} else {
						result = new FilesStatusResult(false, false);
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error while looking up file status for \"{}\": {}", fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
			result = new FilesStatusResult();
		}

		return result;
	}

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		TABLE_LOCK.writeLock().lock();
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
					createFilesStatusTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createFilesStatusTable(connection);
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
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
	@SuppressWarnings("unused")
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		TABLE_LOCK.writeLock().lock();
		try {
			for (int version = currentVersion;version < TABLE_VERSION; version++) {
				switch (version) {
					//case 1: Alter table to version 2
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "is missing table upgrade commands from version " +
							version + " to " + TABLE_VERSION
						);
				}
			}
			setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createFilesStatusTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID            IDENTITY PRIMARY KEY, " +
					"FILEID        INT      NOT NULL UNIQUE, " +
					"MODIFIED      DATETIME, " +
					"ISFULLYPLAYED BOOLEAN  DEFAULT false" +
				")"
			);

			statement.execute("CREATE INDEX FILEID_IDX ON " + TABLE_NAME + "(FILEID)");
		}
	}
}
