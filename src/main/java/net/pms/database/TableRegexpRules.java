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
import java.sql.SQLException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Audiotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class TableRegexpRules extends TableHelper {

	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableRegexpRules.class);
	public static final String TABLE_NAME = "REGEXP_RULES";

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
						if (isColumnExist(connection, TABLE_NAME, "RULE")) {
							LOGGER.trace("Renaming column name RULE to REGEXP_RULE");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `RULE` RENAME TO REGEXP_RULE");
						}
						if (isColumnExist(connection, TABLE_NAME, "ORDR")) {
							LOGGER.trace("Renaming column name ORDR to REGEXP_ORDER");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `ORDR` RENAME TO REGEXP_ORDER");
						}
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
				LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
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
		executeUpdate(connection, "CREATE TABLE " + TABLE_NAME + " ( ID VARCHAR2(255) PRIMARY KEY, REGEXP_RULE VARCHAR2(255), REGEXP_ORDER NUMERIC );");
		executeUpdate(connection, "INSERT INTO " + TABLE_NAME + " VALUES ( '###', '(?i)^\\W.+', 0 );");
		executeUpdate(connection, "INSERT INTO " + TABLE_NAME + " VALUES ( '0-9', '(?i)^\\d.+', 1 );");
		String[] chars = Messages.getString("DLNAMediaDatabase.1").split(",");
		for (int i = 0; i < chars.length; i++) {
			// Create regexp rules for characters with a sort order based on the property value
			executeUpdate(connection, "INSERT INTO " + TABLE_NAME + " VALUES ( '" + chars[i] + "', '(?i)^" + chars[i] + ".+', " + (i + 2) + " );");
		}
	}

}
