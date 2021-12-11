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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.util.APIUtils;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TableFailedLookups extends Tables {
	/**
	 * TABLE_LOCK is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableFailedLookups.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final String TABLE_NAME = "FAILED_LOOKUPS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

	// No instantiation
	private TableFailedLookups() {
	}

	/**
	 * @param fullPathToFile
	 * @param isVideo whether this is a video, otherwise it's a TV series
	 * @return whether a lookup for this file has failed recently
	 */
	public static boolean hasLookupFailedRecently(final String fullPathToFile, final boolean isVideo) {
		boolean removeAfter = false;
		String latestVersion = null;
		if (isVideo) {
			latestVersion = APIUtils.getApiDataVideoVersion();
		} else {
			latestVersion = APIUtils.getApiDataSeriesVersion();
		}
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT LASTATTEMPT FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(fullPathToFile) + " ");
		if (latestVersion != null && CONFIGURATION.getExternalNetwork()) {
			sql.append(" AND VERSION = " + sqlQuote(latestVersion) + " ");
		}
		sql.append("LIMIT 1");

		TABLE_LOCK.readLock().lock();
		try (
			Connection connection = DATABASE.getConnection();
			PreparedStatement selectStatement = connection.prepareStatement(sql.toString());
			ResultSet rs = selectStatement.executeQuery()
		) {
			if (rs.next()) {
				LOGGER.trace("We have failed a lookup for {} so let's see if it was recent", fullPathToFile);

				OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
				OffsetDateTime lastAttempt = rs.getObject("LASTATTEMPT", OffsetDateTime.class);
				if (lastAttempt.plusWeeks(1).isAfter(now)) {
					// The last attempt happened in the last week
					return true;
				} else {
					// The last attempt happened over a week ago, let's remove it so it can be tried again
					removeAfter = true;
					return false;
				}
			} else {
				LOGGER.trace("We have no failed lookups stored for {}", fullPathToFile);
				return false;
			}
		} catch (Exception e) {
			LOGGER.error(
				"Database error while writing to " + TABLE_NAME + " for \"{}\": {}",
				fullPathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		} finally {
			TABLE_LOCK.readLock().unlock();
			if (removeAfter) {
				remove(fullPathToFile, false);
			}
		}

		return false;
	}

	/**
	 * Sets a new row.
	 *
	 * @param fullPathToFile
	 * @param failureDetails the response the API server returned, or a client-side message
	 */
	public static void set(final String fullPathToFile, final String failureDetails, final boolean isVideo) {
		boolean trace = LOGGER.isTraceEnabled();
		String latestVersion = null;
		if (isVideo) {
			latestVersion = APIUtils.getApiDataVideoVersion();
		} else {
			latestVersion = APIUtils.getApiDataSeriesVersion();
		}

		TABLE_LOCK.writeLock().lock();
		try (Connection connection = DATABASE.getConnection()) {
			String query = "SELECT FILENAME, FAILUREDETAILS, VERSION FROM " + TABLE_NAME + " WHERE FILENAME = " + sqlQuote(fullPathToFile) + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching for file/series in " + TABLE_NAME + " with \"{}\" before update", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery(query)) {
					if (result.next()) {
						result.updateString("FAILUREDETAILS", left(failureDetails, 20000));
						result.updateString("VERSION", left(latestVersion, 1024));
						result.updateRow();
					} else {
						result.moveToInsertRow();
						result.updateString("FILENAME", left(fullPathToFile, 1024));
						result.updateString("FAILUREDETAILS", left(failureDetails, 20000));
						result.updateString("VERSION", left(latestVersion, 1024));
						result.insertRow();
					}
				} finally {
					connection.commit();
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			if (e.getErrorCode() != 23505) {
				LOGGER.error(
					"Database error while writing to " + TABLE_NAME + " for \"{}\": {}",
					fullPathToFile,
					e.getMessage()
				);
				LOGGER.trace("", e);
			}
		} catch (Exception e) {
			LOGGER.trace("", e);
		} finally {
			TABLE_LOCK.writeLock().unlock();
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
		try (Connection connection = DATABASE.getConnection()) {
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
							DATABASE.getDatabaseFilename() +
							"\" before starting UMS"
						);
					}
				} else {
					LOGGER.warn("Database table \"{}\" has an unknown version and cannot be used. Dropping and recreating table", TABLE_NAME);
					dropTable(connection, TABLE_NAME);
					createTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createTable(connection);
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
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		TABLE_LOCK.writeLock().lock();
		try {
			for (int version = currentVersion; version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
				switch (version) {
					case 1:
						try (Statement statement = connection.createStatement()) {
							if (!isColumnExist(connection, TABLE_NAME, "VERSION")) {
								statement.execute("ALTER TABLE " + TABLE_NAME + " ADD VERSION VARCHAR2");
								statement.execute("CREATE INDEX FILENAME_VERSION on FILES (FILENAME, VERSION)");
							}
						} catch (SQLException e) {
							LOGGER.error("Failed upgrading database table {} for {}", TABLE_NAME, e.getMessage());
							LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
							throw new SQLException(e);
						}
						version++;
						break;
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "\" is missing table upgrade commands from version " +
							version + " to " + TABLE_VERSION
						);
				}
			}

			try {
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			} catch (SQLException e) {
				LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
				throw new SQLException(e);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID               IDENTITY                   PRIMARY KEY, " +
					"FILENAME         VARCHAR2(1024)             NOT NULL, " +
					"FAILUREDETAILS   VARCHAR2(20000)            NOT NULL, " +
					"VERSION          VARCHAR2(1024)             NOT NULL, " +
					"LASTATTEMPT      TIMESTAMP WITH TIME ZONE   DEFAULT CURRENT_TIMESTAMP" +
				")"
			);

			statement.execute("CREATE UNIQUE INDEX FAILED_FILENAME_IDX ON " + TABLE_NAME + "(FILENAME)");
			statement.execute("CREATE INDEX FILENAME_VERSION on FILES (FILENAME, VERSION)");
		}
	}

	/**
	 * Drops (deletes) the current table. Use with caution, there is no undo.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static final void dropTable(final Connection connection) throws SQLException {
		LOGGER.debug("Dropping database table if it exists \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
		}
	}
}
