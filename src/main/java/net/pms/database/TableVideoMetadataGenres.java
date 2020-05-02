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
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	 * Sets a new genre entry for a file.
	 *
	 * @param fullPathToFile
	 * @param genres
	 * @param tvSeriesID
	 */
	public static void set(final String fullPathToFile, final HashSet genres, final int tvSeriesID) {
		if (genres.isEmpty()) {
			return;
		}

		TABLE_LOCK.writeLock().lock();
		try (Connection connection = database.getConnection()) {
			Iterator<String> i = genres.iterator();
			while (i.hasNext()) {
				String genre = i.next();
				PreparedStatement insertStatement = connection.prepareStatement(
					"INSERT INTO " + TABLE_NAME + " (" +
						"TVSERIESID, FILENAME, GENRE" +
					") VALUES (" +
						"?, ?, ?" +
					")",
					Statement.RETURN_GENERATED_KEYS
				);
				insertStatement.clearParameters();
				insertStatement.setInt(1, tvSeriesID);
				insertStatement.setString(2, left(fullPathToFile, 255));
				insertStatement.setString(3, left(genre, 255));

				insertStatement.executeUpdate();
				try (ResultSet rs = insertStatement.getGeneratedKeys()) {
					if (rs.next()) {
						LOGGER.trace("Set new entry successfully in " + TABLE_NAME + " with \"{}\" and \"{}\"", fullPathToFile, genre);
					}
				}
			}
		} catch (SQLException e) {
			if (e.getErrorCode() == 23505) {
				// This isn't optimal but it won't cause problems so just log it
				LOGGER.trace("Attempted to set genres when they were already there for \"{}\": {}", e);
			} else {
				LOGGER.error(
					"Database error while writing genres to " + TABLE_NAME + " for \"{}\": {}",
					fullPathToFile,
					e.getMessage()
				);
				LOGGER.trace("", e);
			}
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
					"ID       IDENTITY PRIMARY KEY, " +
					"TVSERIESID   INT, " +
					"FILENAME VARCHAR2(1024), " +
					"GENRE    VARCHAR2(1024)        NOT NULL" +
				")"
			);

			statement.execute("CREATE UNIQUE INDEX FILENAME_GENRE_IDX ON " + TABLE_NAME + "(FILENAME, GENRE)");
		}
	}
}
