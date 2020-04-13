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
import net.pms.Messages;
import net.pms.PMS;

public final class TableVideoMetadataGenres extends Tables {
	/**
	 * TABLE_LOCK is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableVideoMetadataGenres.class);
	public static final String TABLE_NAME = "VIDEO_METADATA_GENRES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	// No instantiation
	private TableVideoMetadataGenres() {
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
			String query = "SELECT ISFULLYPLAYED FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(fullPathToFile) + " LIMIT 1";

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

	public static int getBookmark(final String fullPathToFile) {
		boolean trace = LOGGER.isTraceEnabled();
		int result = 0;
		
		try (Connection connection = database.getConnection()) {
			String query = "SELECT BOOKMARK FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(fullPathToFile) + " LIMIT 1";
			
			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}
			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						result = resultSet.getInt("BOOKMARK");
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error while looking up file bookmark in " + TABLE_NAME + " for \"{}\": {}", fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static void setBookmark(final String fullPathToFile, final int bookmark) {
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
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						result.updateInt("BOOKMARK", bookmark);
						result.updateRow();
					} else {
						result.moveToInsertRow();
						result.updateString("FILENAME", fullPathToFile);
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						result.updateBoolean("ISFULLYPLAYED", false);
						result.updateInt("BOOKMARK", bookmark);
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
				"Database error while writing bookmark \"{}\" to " + TABLE_NAME + " for \"{}\": {}",
				bookmark,
				fullPathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
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
		TABLE_LOCK.writeLock().lock();
		try {
			if (tableExists(connection, TABLE_NAME)) {
				Integer version = getTableVersion(connection, TABLE_NAME);
				if (version != null) {
					if (version < TABLE_VERSION) {
//						upgradeTable(connection, version);
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
					createVideoMetadataGenresTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createVideoMetadataGenresTable(connection);
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createVideoMetadataGenresTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"id            IDENTITY PRIMARY KEY, " +
					"genre         VARCHAR2(1024)        NOT NULL " +
				")"
			);

			statement.execute("CREATE UNIQUE INDEX FILENAME_IDX ON " + TABLE_NAME + "(FILENAME)");
			statement.execute("CREATE INDEX ISFULLYPLAYED_IDX ON " + TABLE_NAME + "(ISFULLYPLAYED)");
		}
	}
}
