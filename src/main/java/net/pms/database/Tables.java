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
import net.pms.PMS;
import net.pms.dlna.DLNAMediaDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the super class of all database table classes. It has the
 * responsibility to check or create the <code>TABLES<code> table, and to call
 * <code>checkTable</code> for each database table implementation.
 *
 * This class also has some utility methods that's likely to be useful to most
 * child classes.
 *
 * @author Nadahar
 */
public class Tables {
	private static final Logger LOGGER = LoggerFactory.getLogger(Tables.class);
	private static final Object checkTablesLock = new Object();
	protected static final DLNAMediaDatabase database = PMS.get().getDatabase();
	private static boolean tablesChecked = false;
	private static final String EscapeCharacter = "\\";

	// No instantiation
	protected Tables() {
	}

	/**
	 * Checks all child tables for their existence and version and creates or
	 * upgrades as needed. Access to this method is serialized.
	 *
	 * @throws SQLException
	 */
	public final static void checkTables() throws SQLException {
		synchronized (checkTablesLock) {
			if (tablesChecked) {
				LOGGER.debug("Database tables have already been checked, aborting check");
			} else {
				LOGGER.debug("Starting check of database tables");
				try (Connection connection = database.getConnection()) {
					if (!tableExists(connection, "TABLES")) {
						createTablesTable(connection);
					}

					TableMusicBrainzReleases.checkTable(connection);
					TableCoverArtArchive.checkTable(connection);
				}
				tablesChecked = true;
			}
		}
	}

	/**
	 * Checks if a named table exists
	 *
	 * @param connection the {@link Connection} to use while performing the check
	 * @param tableName the name of the table to check for existence
	 * @param tableSchema the table schema for the table to check for existence
	 *
	 * @return <code>true</code> if a table with the given name in the given
	 *         schema exists, <code>false</code> otherwise
	 *
	 * @throws SQLException
	 */
	protected final static boolean tableExists(final Connection connection, final String tableName, final String tableSchema) throws SQLException {
		LOGGER.trace("Checking if database table \"{}\" in schema \"{}\" exists", tableName, tableSchema);

		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT * FROM INFORMATION_SCHEMA.TABLES " +
                 "WHERE TABLE_SCHEMA = ? "+
                 "AND  TABLE_NAME = ?"
		)) {
			statement.setString(1, tableSchema);
			statement.setString(2, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					LOGGER.trace("Database table \"{}\" found", tableName);
					return true;
				} else {
					LOGGER.trace("Database table \"{}\" not found", tableName);
					return false;
				}
			}
		}
	}

	/**
	 * Checks if a named table exists in table schema <code>PUBLIC</code>
	 *
	 * @param connection the {@link Connection} to use while performing the check
	 * @param tableName the name of the table to check for existence
	 *
	 * @return <code>true</code> if a table with the given name in schema
	 * <code>PUBLIC</code> exists, <code>false</code> otherwise
	 *
	 * @throws SQLException
	 */
	protected final static boolean tableExists(final Connection connection, final String tableName) throws SQLException {
		return tableExists(connection, tableName, "PUBLIC");
	}

	/**
	 * Gets the version of a named table from the <code>TABLES</code> table
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table for which to find the version
	 *
	 * @return The version number if found or <code>null</code> if the table
	 *         isn't listed in <code>TABLES</code>
	 *
	 * @throws SQLException
	 */
	protected final static Integer getTableVersion(final Connection connection, final String tableName) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT VERSION FROM TABLES " +
				"WHERE NAME = ?"
		)) {
			statement.setString(1, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Table version for database table \"{}\" is {}", tableName, result.getInt("VERSION"));
					}
					return result.getInt("VERSION");
				} else {
					LOGGER.trace("Table version for database table \"{}\" not found", tableName);
					return null;
				}
			}
		}
	}

	/**
	 * Sets the version of a named table in the <code>TABLES</code> table.
	 * Creates a row for the given table name if needed.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table for which to set the version
	 * @param version the version number to set
	 *
	 * @throws SQLException
	 */
	protected final static void setTableVersion(final Connection connection, final String tableName, final int version) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT VERSION FROM TABLES WHERE NAME = ?"
		)) {
			statement.setString(1, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					int currentVersion = result.getInt("VERSION");
					if (version != currentVersion) {
						try (PreparedStatement updateStatement = connection.prepareStatement(
							"UPDATE TABLES SET VERSION = ? WHERE NAME = ?"
						)) {
							LOGGER.trace("Updating table version for database table \"{}\" from {} to {}", tableName, currentVersion, version);
							updateStatement.setInt(1, version);
							updateStatement.setString(2, tableName);
							updateStatement.executeUpdate();
						}
					} else {
						LOGGER.trace("Table version for database table \"{}\" is already {}, aborting set", tableName, version);
					}
				} else {
					try (PreparedStatement insertStatement = connection.prepareStatement(
						"INSERT INTO TABLES VALUES(?, ?)"
					)) {
						LOGGER.trace("Setting table version for database table \"{}\" to {}", tableName, version);
						insertStatement.setString(1, tableName);
						insertStatement.setInt(2, version);
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	/**
	 * Drops (deletes) the named table. Use with caution, there is no undo.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table to delete
	 *
	 * @throws SQLException
	 */
	protected final static void dropTable(final Connection connection, final String tableName) throws SQLException {
		LOGGER.debug("Dropping database table \"{}\"", tableName);
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP TABLE " + tableName);
		}
	}

	private final static void createTablesTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table \"TABLES\"");
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE TABLES(NAME VARCHAR(50) PRIMARY KEY, VERSION INT NOT NULL)");
		}
	}

	/**
	 * Convenience method for handling SQL null values in <code>WHERE</code> or
	 * <code>HAVING</code> statements. SQL doesn't see null as a value, and
	 * thus <code>=</code> is illegal for <code>null</code>.
	 * Instead, <code>IS NULL</code> must be used.
	 *
	 * Please note that the like-escaping is not applied, as that must be done
	 * before any wildcards are added.
	 *
	 * @param s the {@link String} to compare to.
	 * @param quote whether the result should be single quoted for use as a SQL
	 *        string or not.
	 * @param like whether <code>LIKE</code> should be used instead of <code>=</code>. This implies quote.
	 * @return The SQL formatted string including the <code>=</code>,
	 * <code>LIKE</code> or <code>IS</code> operator.
	 */
	public final static String sqlNullIfBlank(final String s, boolean quote, boolean like) {
		if (s == null || s.trim().isEmpty()) {
			return " IS NULL ";
		} else if (like) {
			return " LIKE " + sqlQuote(s);
		} else if (quote) {
			return " = " + sqlQuote(s);
		} else {
			return " = " + s;
		}
	}

	/**
	 * Surrounds the argument with single quotes {@link String} and escapes any
	 * existing single quotes.
	 *
	 * @param s the {@link String} to quote.
	 * @return The quoted {@link String}.
	 */
	public final static String sqlQuote(final String s) {
		return s == null ? null : "'" + s.replace("'", "''") + "'";
	}

	/**
	 * Escapes the argument with the default H2 escape character for the
	 * escape character itself and the two wildcard characters <code>%</code>
	 * and <code>_<code>. This escaping is only valid when using,
	 *  <code>LIKE</code>, not when using <code>=</code>.
	 *
	 * TODO: Escaping should be generalized so that any escape character could
	 *       be used and that the class would set the correct escape character
	 *       when opening the database.
	 *
	 * @param the {@link String} to be SQL escaped.
	 * @return The escaped {@link String}.
	 */
	public final static String sqlLikeEscape(final String s) {
		return s == null ? null : s.
			replace(EscapeCharacter, EscapeCharacter + EscapeCharacter).
			replace("%", EscapeCharacter + "%").
			replace("_", EscapeCharacter + "_");
	}
}
