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
import java.sql.Timestamp;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Set;
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
public final class TableFilesStatus extends Tables {
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
	private static final int TABLE_VERSION = 8;

	// No instantiation
	private TableFilesStatus() {
	}

	/**
	 * Sets whether the file has been fully played.
	 *
	 * @param fullPathToFile
	 * @param isFullyPlayed
	 */
	public static void setFullyPlayed(final String fullPathToFile, final boolean isFullyPlayed) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;

		try (Connection connection = database.getConnection()) {
			query = "SELECT * FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(fullPathToFile) + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching for file in " + TABLE_NAME + " with \"{}\" before update", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery(query)) {
					if (result.next()) {
						if (result.getBoolean("ISFULLYPLAYED") == isFullyPlayed) {
							if (trace) {
								LOGGER.trace("Found file entry in " + TABLE_NAME + " and it already has ISFULLYPLAYED set to {}", result.getBoolean("ISFULLYPLAYED"));
							}
						} else {
							if (trace) {
								LOGGER.trace(
									"Found file entry \"{}\" in " + TABLE_NAME + "; setting ISFULLYPLAYED to {}",
									fullPathToFile,
									isFullyPlayed
								);
							}
							result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
							result.updateBoolean("ISFULLYPLAYED", isFullyPlayed);
							result.updateRow();
						}
					} else {
						if (trace) {
							LOGGER.trace(
								"File entry \"{}\" not found in " + TABLE_NAME + ", inserting new row with ISFULLYPLAYED set to {}",
								fullPathToFile,
								isFullyPlayed
							);
						}
						result.moveToInsertRow();
						result.updateString("FILENAME", fullPathToFile);
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						result.updateBoolean("ISFULLYPLAYED", isFullyPlayed);
						result.insertRow();
					}
				} finally {
					connection.commit();
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing status \"{}\" to " + TABLE_NAME + " for \"{}\": {}",
				isFullyPlayed,
				fullPathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Sets whether each file within the folder is fully played.
	 *
	 * @param fullPathToFolder the full path to the folder.
	 * @param isFullyPlayed whether to mark the folder content as fully played
	 *            or not fully played.
	 */
	public static void setDirectoryFullyPlayed(final String fullPathToFolder, final boolean isFullyPlayed) {
		boolean trace = LOGGER.isTraceEnabled();
		String pathWithWildcard = sqlLikeEscape(fullPathToFolder) + "%";
		String statusLineString = isFullyPlayed ? Messages.getString("FoldTab.75") : Messages.getString("FoldTab.76");
		PMS.get().getFrame().setStatusLine(statusLineString + ": " + fullPathToFolder);

		try (Connection connection = database.getConnection()) {
			String query = "SELECT ID, FILENAME FROM FILES WHERE FILENAME LIKE " + sqlQuote(pathWithWildcard);
			if (trace) {
				LOGGER.trace("Searching for file in " + TABLE_NAME + " with \"{}\" before update", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet result = statement.executeQuery(query)) {
					while (result.next()) {
						setFullyPlayed(result.getString("FILENAME"), isFullyPlayed);
					}
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing status \"{}\" to " + TABLE_NAME + " for \"{}\": {}",
				isFullyPlayed,
				pathWithWildcard,
				e.getMessage()
			);
			LOGGER.trace("", e);
		} finally {
			PMS.get().getFrame().setStatusLine(null);
		}
	}

	/**
	 * Removes an entry or entries based on its FILENAME. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see Tables#sqlLikeEscape(String)
	 *
	 * @param filename the filename to remove
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 */
	public static void remove(final String filename, boolean useLike) {
		try (Connection connection = database.getConnection()) {
			String query =
				"DELETE FROM " + TABLE_NAME + " WHERE FILENAME " +
				(useLike ? "LIKE " : "= ") + sqlQuote(filename);
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows = statement.executeUpdate(query);
				LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for filename \"{}\"", rows, filename);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while removing entries from " + TABLE_NAME + " for \"{}\": {}",
				filename,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	public static Boolean isFullyPlayed(final String fullPathToFile) {
		boolean trace = LOGGER.isTraceEnabled();
		Boolean result = null;

		try (Connection connection = database.getConnection()) {
			String query = "SELECT ISFULLYPLAYED FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(fullPathToFile);

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						result = resultSet.getBoolean("ISFULLYPLAYED");
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error while looking up file status in " + TABLE_NAME + " for \"{}\": {}", fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
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
						LOGGER.warn(
							"Database table \"" + TABLE_NAME +
							"\" is from a newer version of UMS. If you experience problems, you could try to move, rename or delete database file \"" +
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
	@SuppressFBWarnings("IIL_PREPARE_STATEMENT_IN_LOOP")
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		TABLE_LOCK.writeLock().lock();
		try {
			for (int version = currentVersion;version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
				switch (version) {
					case 1:
						// From version 1 to 2, we stopped using FILEID and instead use FILENAME directly
						try (Statement statement = connection.createStatement()) {
							statement.execute("ALTER TABLE " + TABLE_NAME + " ADD FILENAME VARCHAR2(1024)");
							statement.execute("ALTER TABLE " + TABLE_NAME + " ADD CONSTRAINT FILES_FILENAME_UNIQUE UNIQUE(FILENAME)");

							Set<String> fileStatusEntries = new HashSet<>();
							PreparedStatement stmt = connection.prepareStatement("SELECT FILES.ID AS FILES_ID, FILES.FILENAME AS FILES_FILENAME FROM FILES LEFT JOIN " + TABLE_NAME + " ON FILES.ID = " + TABLE_NAME + ".FILEID");
							ResultSet rs = stmt.executeQuery();
							String filename;
							while (rs.next()) {
								filename = rs.getString("FILES_FILENAME");

								// Ensure we don't attempt add the same filename twice
								if (!fileStatusEntries.contains(filename)) {
									fileStatusEntries.add(filename);
									String query = "UPDATE " + TABLE_NAME + " SET FILENAME=" + sqlQuote(filename) + " WHERE FILEID=" + rs.getInt("FILES_ID");
									Statement statement2 = connection.createStatement();
									statement2.execute(query);
									LOGGER.info("Updating fully played entry for " + filename);
								}
							}
							stmt.close();
							rs.close();

							statement.execute("DELETE FROM " + TABLE_NAME + " WHERE FILENAME IS NULL");
							statement.execute("ALTER TABLE " + TABLE_NAME + " ALTER COLUMN FILENAME SET NOT NULL");
							statement.execute("DROP INDEX IF EXISTS FILEID_IDX");
							statement.execute("ALTER TABLE " + TABLE_NAME + " DROP COLUMN FILEID");
							statement.execute("CREATE INDEX FILENAME_IDX ON " + TABLE_NAME + "(FILENAME)");
						}
						version = 2;
						break;
					case 2:
					case 3:
						// From version 2 to 3, we added an index for the ISFULLYPLAYED column
						// From version 3 to 4, we make sure the previous index was created correctly
						try (Statement statement = connection.createStatement()) {
							statement.execute("DROP INDEX IF EXISTS ISFULLYPLAYED_IDX");
							statement.execute("CREATE INDEX ISFULLYPLAYED_IDX ON " + TABLE_NAME + "(ISFULLYPLAYED)");
						}
						version = 4;
						break;
					case 4:
					case 5:
					case 6:
					case 7:
						// From version 7 to 8, we undo our referential integrity attempt that kept going wrong
						PreparedStatement stmt = connection.prepareStatement(
							"SELECT constraint_name " +
							"FROM information_schema.constraints " +
							"WHERE TABLE_NAME = '" + TABLE_NAME + "' AND constraint_type = 'REFERENTIAL'"
						);
						ResultSet rs = stmt.executeQuery();

						while (rs.next()) {
							try (Statement statement = connection.createStatement()) {
								statement.execute("ALTER TABLE " + TABLE_NAME + " DROP CONSTRAINT IF EXISTS " + rs.getString("constraint_name"));
							}
						}

						stmt = connection.prepareStatement(
							"SELECT constraint_name " +
							"FROM information_schema.constraints " +
							"WHERE TABLE_NAME = '" + TABLE_NAME + "' AND constraint_type = 'REFERENTIAL'"
						);
						rs = stmt.executeQuery();

						while (rs.next()) {
							throw new SQLException("The upgrade from v7 to v8 failed to remove the old constraints");
						}

						stmt.close();
						rs.close();

						version = 8;
						break;
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "\" is missing table upgrade commands from version " +
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
					"FILENAME      VARCHAR2(1024)        NOT NULL, " +
					"MODIFIED      DATETIME, " +
					"ISFULLYPLAYED BOOLEAN DEFAULT false" +
				")"
			);

			statement.execute("CREATE UNIQUE INDEX FILENAME_IDX ON " + TABLE_NAME + "(FILENAME)");
			statement.execute("CREATE INDEX ISFULLYPLAYED_IDX ON " + TABLE_NAME + "(ISFULLYPLAYED)");
		}
	}
}
