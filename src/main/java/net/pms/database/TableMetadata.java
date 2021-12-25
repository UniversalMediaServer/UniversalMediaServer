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

public class TableMetadata extends TableHelper {

	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableMetadata.class);
	public static final String TABLE_NAME = "METADATA";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

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
				Integer version = TableTablesVersions.getTableVersion(connection, TABLE_NAME);
				if (version == null) {
					// Moving sql from DLNAMediaDatabase to this class.
					version = 1;
				}
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn("Database table \"" + TABLE_NAME + "\" is from a newer version of UMS.");
				}
			} else {
				createTable(connection);
				TableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		TABLE_LOCK.writeLock().lock();
		try {
			for (int version = currentVersion; version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
				switch (version) {
					case 1:
						// From version 1 to 2, we stopped using KEY and VALUE, and instead use M_KEY and M_VALUE
						executeUpdate(connection, "DROP INDEX IF EXISTS IDX_KEY");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `KEY` RENAME TO M_KEY");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `VALUE` RENAME TO M_VALUE");
						executeUpdate(connection, "CREATE UNIQUE INDEX IDX_M_KEY ON " + TABLE_NAME + "(M_KEY)");
						break;
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "\" is missing table upgrade commands from version " +
							version + " to " + TABLE_VERSION
						);
				}
			}
			try {
				TableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			} catch (SQLException e) {
				LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
				throw new SQLException(e);
			}
		} catch (SQLException e) {
			LOGGER.error("Failed upgrading database table {} for {}", TABLE_NAME, e.getMessage());
			throw new SQLException(e);
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		executeUpdate(connection, "CREATE TABLE " + TABLE_NAME + " (M_KEY VARCHAR2(255) NOT NULL, M_VALUE VARCHAR2(255) NOT NULL)");
		executeUpdate(connection, "CREATE UNIQUE INDEX IDX_M_KEY ON " + TABLE_NAME + "(M_KEY)");
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

	/**
	 * Gets a value from the METADATA table
	 * @param key
	 * @return value
	 */
	public static String getMetadataValue(String key) {
		String value = null;

		try (Connection conn = DATABASE.getConnection()) {
			TABLE_LOCK.readLock().lock();
			try (
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("SELECT M_VALUE FROM " + TABLE_NAME + " WHERE M_KEY = '" + key + "'")
			) {
				if (rs.next()) {
					value = rs.getString(1);
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (Exception se) {
			LOGGER.error(null, se);
		}

		return value;
	}

	/**
	 * Sets or updates a value in the METADATA table.
	 *
	 * @param key
	 * @param value
	 */
	public static void setOrUpdateMetadataValue(final String key, final String value) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;

		try (Connection connection = DATABASE.getConnection()) {
			query = "SELECT * FROM " + TABLE_NAME + " WHERE M_KEY = " + sqlQuote(key) + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching for value in METADATA with \"{}\" before update", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery(query)) {
					boolean isCreatingNewRecord = false;

					if (!result.next()) {
						isCreatingNewRecord = true;
						result.moveToInsertRow();
					}

					result.updateString("M_KEY", key);
					result.updateString("M_VALUE", value);

					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				} finally {
					connection.commit();
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing value to " + TABLE_NAME + ": {}",
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}
}
