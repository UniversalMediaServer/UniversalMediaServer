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

import com.google.common.base.CharMatcher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHelper.class);
	private static final String ESCAPE_CHARACTER = "\\";
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	protected static final String LOG_CREATING_TABLE = "Creating database \"{}\" table: \"{}\"";
	protected static final String LOG_UPGRADING_TABLE = "Upgrading database \"{}\" table \"{}\" from version {} to {}";
	protected static final String LOG_UPGRADED_TABLE = "Updated database \"{}\" table \"{}\" from version {} to {}";
	protected static final String LOG_UPGRADING_TABLE_FAILED = "Failed upgrading database \"{}\" table {} for {}";
	protected static final String LOG_UPGRADING_TABLE_MISSING = "Database \"{}\" table \"{}\" is missing table upgrade commands from version {} to {}";

	protected static final String LOG_TABLE_NEWER_VERSION = "Database \"{}\" table \"{}\" is from a newer version of UMS.";
	protected static final String LOG_TABLE_NEWER_VERSION_DELETEDB = "Database \"{}\" table \"{}\" is from a newer version of UMS. If you experience problems, you could try to move, rename or delete database file \"{}\" before starting UMS";
	protected static final String LOG_TABLE_UNKNOWN_VERSION_RECREATE = "Database \"{}\" table \"{}\" has an unknown version and cannot be used. Dropping and recreating table";

	protected static final String LOG_CONNECTION_GET_ERROR = "Database \"{}\" error while getting connection: {}";
	protected static final String LOG_ERROR_WHILE_IN = "Database \"{}\" error while {} in \"{}\": {}";
	protected static final String LOG_ERROR_WHILE_VAR_IN = "Database \"{}\" error while {} \"{}\" in \"{}\": {}";
	protected static final String LOG_ERROR_WHILE_IN_FOR = "Database \"{}\" error while {} in \"{}\" for \"{}\": {}";
	protected static final String LOG_ERROR_WHILE_VAR_IN_FOR = "Database \"{}\" error while {} \"{}\" in \"{}\" for \"{}\": {}";

	// Generic constant for the maximum string size: 255 chars
	protected static final int SIZE_MAX = 255;

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
	protected static final boolean tableExists(final Connection connection, final String tableName, final String tableSchema) throws SQLException {
		LOGGER.trace("Checking if database table \"{}\" in schema \"{}\" exists", tableName, tableSchema);

		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT * FROM INFORMATION_SCHEMA.TABLES " +
			"WHERE TABLE_SCHEMA = ? " +
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
	protected static final boolean tableExists(final Connection connection, final String tableName) throws SQLException {
		return tableExists(connection, tableName, "PUBLIC");
	}

	/**
	 * Drops (deletes) the named table. Use with caution, there is no undo.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table to delete
	 *
	 * @throws SQLException
	 */
	protected static final void dropTable(final Connection connection, final String tableName) throws SQLException {
		LOGGER.debug("Dropping database table if it exists \"{}\"", tableName);
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP TABLE IF EXISTS " + tableName);
		}
	}

	/**
	 * Drops (deletes) the named table and its constaints. Use with caution, there is no undo.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table to delete
	 */
	protected static final void dropTableAndConstraint(final Connection connection, final String tableName) {
		LOGGER.debug("Dropping database table if it exists \"{}\"", tableName);
		try {
			if (tableExists(connection, tableName)) {
				dropReferentialsConstraint(connection, tableName);
				executeUpdate(connection, "DROP TABLE IF EXISTS " + tableName + " CASCADE");
			}
		} catch (SQLException e) {
			LOGGER.error("error during dropping table\"" + tableName + "\":" + e.getMessage(), e);
		}
	}

	/**
	 * Drops referentials constraints on the named table.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table to delete
	 */
	protected static final void dropReferentialsConstraint(final Connection connection, final String tableName) {
		LOGGER.debug("Dropping table \"{}\" constraints if it exists", tableName);
		try {
			String sql;
			ResultSet rs = connection.getMetaData().getTables(null, "INFORMATION_SCHEMA", "TABLE_CONSTRAINTS", null);
			if (rs.next()) {
				sql = "SELECT CONSTRAINT_NAME " +
					"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
					"WHERE TABLE_NAME = '" + tableName + "' AND CONSTRAINT_TYPE = 'FOREIGN KEY' OR CONSTRAINT_TYPE = 'REFERENTIAL'";
			} else {
				sql = "SELECT CONSTRAINT_NAME " +
					"FROM INFORMATION_SCHEMA.CONSTRAINTS " +
					"WHERE TABLE_NAME = '" + tableName + "' AND CONSTRAINT_TYPE = 'REFERENTIAL'";
			}
			PreparedStatement stmt = connection.prepareStatement(sql);
			rs = stmt.executeQuery();

			while (rs.next()) {
				try (Statement statement = connection.createStatement()) {
					statement.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + rs.getString("CONSTRAINT_NAME"));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("error during dropping table\"" + tableName + "\" constraints:" + e.getMessage(), e);
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
	public static final String sqlNullIfBlank(final String s, boolean quote, boolean like) {
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
	 * Surrounds the argument with single quotes and escapes any existing single
	 * quotes.
	 *
	 * @see #sqlEscape(String)
	 *
	 * @param s the {@link String} to escape and quote.
	 * @return The escaped and quoted {@code s}.
	 */
	public static final String sqlQuote(final String s) {
		return s == null ? null : "'" + s.replace("'", "''") + "'";
	}

	/**
	 * Escapes any existing single quotes in the argument but doesn't quote it.
	 *
	 * @see #sqlQuote(String)
	 *
	 * @param s the {@link String} to escape.
	 * @return The escaped {@code s}.
	 */
	public static String sqlEscape(final String s) {
		return s == null ? null : s.replace("'", "''");
	}

	/**
	 * Escapes the argument with the default H2 escape character for the
	 * escape character itself and the two wildcard characters <code>%</code>
	 * and <code>_</code>. This escaping is only valid when using,
	 * <code>LIKE</code>, not when using <code>=</code>.
	 *
	 * TODO: Escaping should be generalized so that any escape character could
	 *       be used and that the class would set the correct escape character
	 *       when opening the database.
	 *
	 * @param s the {@link String} to be SQL escaped.
	 * @return The escaped {@link String}.
	 */
	public static final String sqlLikeEscape(final String s) {
		return s == null ? null : s.
			replace(ESCAPE_CHARACTER, ESCAPE_CHARACTER + ESCAPE_CHARACTER).
			replace("%", ESCAPE_CHARACTER + "%").
			replace("_", ESCAPE_CHARACTER + "_");
	}

	/**
	 * @see https://stackoverflow.com/a/10213258/2049714
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static List<HashMap<String, Object>> convertResultSetToList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		List<HashMap<String, Object>> list = new ArrayList<>();

		while (rs.next()) {
			HashMap<String, Object> row = new HashMap<>(columns);
			for (int i = 1; i <= columns; ++i) {
				row.put(md.getColumnName(i), rs.getObject(i));
			}
			list.add(row);
		}

		return list;
	}

	/**
	 * @param rs
	 * @return the rows of the first column of a result set
	 * @throws SQLException
	 */
	public static HashSet<String> convertResultSetToHashSet(ResultSet rs) throws SQLException {
		HashSet<String> list = new HashSet<>();

		while (rs.next()) {
			list.add(rs.getString(1));
		}

		return list;
	}

	/**
	 * @see https://stackoverflow.com/a/10213258/2049714
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static HashMap<String, Object> convertSingleResultSetToList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		HashMap<String, Object> row = new HashMap<>(columns);
		for (int i = 1; i <= columns; ++i) {
			row.put(md.getColumnName(i), rs.getObject(i));
		}
		return row;
	}

	/**
	 * Check if the column name exists in the database.
	 *
	 * Must be called from inside a table lock.
	 *
	 * @param connection the {@link Connection} to use.
	 * @param table The table name where the column name should exist.
	 * @param column The name of the column.
	 *
	 * @return <code>true</code> if the column name exists in
	 * the database <code>false</code> otherwise.
	 *
	 * @throws SQLException
	 */
	protected static boolean isColumnExist(Connection connection, String table, String column) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement(
			"SELECT * FROM INFORMATION_SCHEMA.COLUMNS " +
			"WHERE TABLE_NAME = ? " +
			"AND COLUMN_NAME = ?"
		)) {
			statement.setString(1, table);
			statement.setString(2, column);
			try (ResultSet result = statement.executeQuery()) {
				if (result.first()) {
					LOGGER.trace("Column \"{}\" found in table \"{}\"", column, table);
					return true;
				} else {
					LOGGER.trace("Column \"{}\" not found in table \"{}\"", column, table);
					return false;
				}
			}
		}
	}

	protected static void executeUpdate(Connection conn, String sql) throws SQLException {
		if (conn != null) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sql);
			}
		}
	}

	protected static void executeUpdate(Statement stmt, String sql) throws SQLException {
		stmt.executeUpdate(sql);
	}

	protected static void execute(Connection conn, String... sqls) throws SQLException {
		if (conn != null) {
			try (Statement stmt = conn.createStatement()) {
				for (String sql : sqls) {
					stmt.execute(sql);
				}
			}
		}
	}

	protected static void updateSerialized(ResultSet rs, Object x, String columnLabel) throws SQLException {
		if (x != null) {
			rs.updateObject(columnLabel, x);
		} else {
			rs.updateNull(columnLabel);
		}
	}

	protected static void insertSerialized(PreparedStatement ps, Object x, int parameterIndex) throws SQLException {
		if (x != null) {
			ps.setObject(parameterIndex, x);
		} else {
			ps.setNull(parameterIndex, Types.OTHER);
		}
	}
	/**
	 * Returns the VALUES {@link String} for the SQL request.
	 * It fills the {@link String} with {@code " VALUES (?,?,?, ...)"}.<p>
	 * The number of the "?" is calculated from the columns and not need to be hardcoded which
	 * often causes mistakes when columns are deleted or added.<p>
	 * Possible implementation:
	 * <blockquote><pre>
	 * String columns = "FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS";
	 * PreparedStatement insertStatement = connection.prepareStatement(
	 *    "INSERT INTO AUDIOTRACKS (" + columns + ")" +
	 *    createDefaultValueForInsertStatement(columns)
	 * );
	 * </pre></blockquote><p
	 *
	 * @param columns the SQL parameters string
	 * @return The " VALUES (?,?,?, ...)" string
	 *
	 */
	protected static String createDefaultValueForInsertStatement(String columns) {
		int count = CharMatcher.is(',').countIn(columns);
		StringBuilder sb = new StringBuilder();
		sb.append(" VALUES (").append(StringUtils.repeat("?,", count)).append("?)");
		return sb.toString();
	}

	protected static Double toDouble(ResultSet rs, String column) throws SQLException {
		Object obj = rs.getObject(column);
		if (obj instanceof Double) {
			return (Double) obj;
		}
		return null;
	}

	public static void close(ResultSet rs) {
		try {
			if (rs != null) {
				rs.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	public static void close(Statement ps) {
		try {
			if (ps != null) {
				ps.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	public static void close(Connection conn) {
		try {
			if (conn != null) {
				conn.close();
			}
		} catch (SQLException e) {
			LOGGER.error("error during closing:" + e.getMessage(), e);
		}
	}

	protected static String getMessage(String pattern, Object... arguments) {
		int i = 0;
		while (pattern.contains("{}")) {
			pattern = pattern.replaceFirst(Pattern.quote("{}"), "{" + i++ + "}");
		}
		return MessageFormat.format(pattern, arguments);
	}
}
