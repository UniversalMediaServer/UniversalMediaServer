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
import net.pms.PMS;
import net.pms.dlna.DLNAThumbnail;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * This class is responsible for managing the Thumbnails table. It
 * does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author SubJunk & Nadahar
 * @since 7.1.1
 */
public final class TableThumbnails extends Tables {
	/**
	 * TABLE_LOCK is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableThumbnails.class);
	public static final String TABLE_NAME = "THUMBNAILS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 1;

	// No instantiation
	private TableThumbnails() {
	}

	/**
	 * Attempts to find a thumbnail in this table by MD5 hash. If not found,
	 * it writes the new thumbnail to this table.
	 * Finally, it writes the ID from this table as the THUMBID in the FILES
	 * table.
	 *
	 * @param thumbnail
	 * @param fullPathToFile
	 */
	public static void setThumbnail(final DLNAThumbnail thumbnail, final String fullPathToFile) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;
		String md5Hash = DigestUtils.md5Hex(thumbnail.getBytes(false));

		try (Connection connection = database.getConnection()) {
			query = "SELECT * FROM " + TABLE_NAME + " WHERE MD5 = " + sqlQuote(md5Hash) + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching for thumbnail in " + TABLE_NAME + " with \"{}\" before update", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, Statement.RETURN_GENERATED_KEYS)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						if (trace) {
							LOGGER.trace("Found existing file entry with ID {} in " + TABLE_NAME + ", setting the THUMBID in the FILES table", result.getInt("ID"));

							// Write the existing thumbnail ID to the FILES table
							PMS.get().getDatabase().updateThumbnailId(fullPathToFile, result.getInt("ID"));
						}
					} else {
						if (trace) {
							LOGGER.trace(
								"File entry \"{}\" not found in " + TABLE_NAME,
								md5Hash
							);
						}
						result.moveToInsertRow();
						result.updateString("MD5", md5Hash);
						result.updateObject("THUMBNAIL", thumbnail);
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						result.insertRow();

						try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
							if (generatedKeys.next()) {
								// Write the new thumbnail ID to the FILES table
								if (trace) {
									LOGGER.trace("Inserting new thumbnail with ID {}, setting the THUMBID in the FILES table", generatedKeys.getInt(1));
								}
								PMS.get().getDatabase().updateThumbnailId(fullPathToFile, generatedKeys.getInt(1));
							}
						}
					}
				} finally {
					connection.commit();
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing \"{}\" to " + TABLE_NAME + ": {}",
				md5Hash,
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
							database.getDatabaseFilename() +
							"\" before starting UMS"
						);
					}
				} else {
					LOGGER.warn("Database table \"{}\" has an unknown version and cannot be used. Dropping and recreating table", TABLE_NAME);
					dropTable(connection, TABLE_NAME);
					createThumbnailsTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createThumbnailsTable(connection);
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
						version = 2;
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
	private static void createThumbnailsTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID          IDENTITY PRIMARY KEY, " +
					"THUMBNAIL   OTHER         NOT NULL, " +
					"MODIFIED    DATETIME, " +
					"MD5         VARCHAR UNIQUE NOT NULL" +
				")"
			);

			statement.execute("CREATE UNIQUE INDEX MD5_IDX ON " + TABLE_NAME + "(MD5)");
		}
	}
}
