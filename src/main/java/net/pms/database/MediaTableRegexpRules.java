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

import java.sql.Connection;
import java.sql.SQLException;
import net.pms.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Audiotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableRegexpRules extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableRegexpRules.class);
	public static final String TABLE_NAME = "REGEXP_RULES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_REGEXP_RULE = "REGEXP_RULE";
	private static final String COL_REGEXP_ORDER = "REGEXP_ORDER";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_REGEXP_ORDER = TABLE_NAME + "." + COL_REGEXP_ORDER;
	public static final String TABLE_COL_REGEXP_RULE = TABLE_NAME + "." + COL_REGEXP_RULE;

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
			if (version == null) {
				// Moving sql from DLNAMediaDatabase to this class.
				version = 1;
			}
			if (version < TABLE_VERSION) {
				upgradeTable(connection, version);
			} else if (version > TABLE_VERSION) {
				LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);

			switch (version) {
				case 1 -> {
					if (isColumnExist(connection, TABLE_NAME, "RULE")) {
						LOGGER.trace("Renaming column name RULE to REGEXP_RULE");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`RULE`" + RENAME_TO + COL_REGEXP_RULE);
					}
					if (isColumnExist(connection, TABLE_NAME, "ORDR")) {
						LOGGER.trace("Renaming column name ORDR to REGEXP_ORDER");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`ORDR`" + RENAME_TO + COL_REGEXP_ORDER);
					}
				}
				default -> throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		try {
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} catch (SQLException e) {
			LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
			LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " ( " +
				COL_ID                + VARCHAR_SIZE_MAX     + PRIMARY_KEY + COMMA +
				COL_REGEXP_RULE       + VARCHAR_SIZE_MAX                   + COMMA +
				COL_REGEXP_ORDER      + NUMERIC                            +
			")",
			INSERT_INTO + TABLE_NAME + " VALUES ( '###', '(?i)^\\W.+', 0 )",
			INSERT_INTO + TABLE_NAME + " VALUES ( '0-9', '(?i)^\\d.+', 1 )"
		);
		String[] chars = Messages.getString("Alphabet").split(",");
		for (int i = 0; i < chars.length; i++) {
			// Create regexp rules for characters with a sort order based on the property value
			executeUpdate(connection, INSERT_INTO + TABLE_NAME + " VALUES ( '" + chars[i] + "', '(?i)^" + chars[i] + ".+', " + (i + 2) + " );");
		}
	}

}
