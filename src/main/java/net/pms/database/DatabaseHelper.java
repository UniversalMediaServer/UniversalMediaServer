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
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class DatabaseHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHelper.class);
	private static final String ESCAPE_CHARACTER = "\\";
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

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
	protected static final String LOG_ERROR_WHILE_VAR_FOR_IN = "Database \"{}\" error while {} \"{}\" for \"{}\" in \"{}\": {}";
	protected static final String LOG_ERROR_WHILE_IN_FOR = "Database \"{}\" error while {} in \"{}\" for \"{}\": {}";
	protected static final String LOG_ERROR_WHILE_VAR_IN_FOR = "Database \"{}\" error while {} \"{}\" in \"{}\" for \"{}\": {}";

	// Generic constant for the maximum string size: 255 chars
	protected static final int SIZE_1024 = 1024;
	protected static final int SIZE_MAX = 255;
	protected static final int SIZE_LANG = 3;

	/**
	 * SQL DATA TYPES
	 */
	/**
	 * Possible values: -9223372036854775808 to 9223372036854775807.
	 * Mapped to java.lang.Long.
	 */
	protected static final String BIGINT = " BIGINT";

	/**
	 * BINARY LARGE OBJECT is intended for very large binary values such as files or images.
	 * Unlike when using BINARY VARYING, large objects are not kept fully in-memory; instead, they are streamed.
	 * Mapped to java.sql.Blob.
	 */
	protected static final String BLOB = " BLOB";

	/**
	 * Possible values: TRUE, FALSE, and UNKNOWN (NULL).
	 * Mapped to java.lang.Boolean.
	 */
	protected static final String BOOLEAN = " BOOLEAN";

	/**
	 * CHARACTER LARGE OBJECT is intended for very large Unicode character string values.
	 * Unlike when using CHARACTER VARYING, large CHARACTER LARGE OBJECT values are not kept fully in-memory;
	 * instead, they are streamed.
	 * Mapped to java.sql.Clob.
	 */
	protected static final String CLOB = " CLOB";

	/**
	 * A double precision floating point number.
	 * Should not be used to represent currency values, because of rounding problems.
	 * If precision value is specified for FLOAT type name, it should be from 25 to 53.
	 * Mapped to java.lang.Double.
	 */
	protected static final String DOUBLE_PRECISION = " DOUBLE PRECISION";

	/**
	 * Identity column is a column generated with a sequence.
	 * This column is implicitly the primary key column of this table.
	 * Same to BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY
	 * Mapped to java.lang.Long.
	 */
	protected static final String IDENTITY = " IDENTITY";

	/**
	 * Possible values: -2147483648 to 2147483647.
	 * Mapped to java.lang.Integer.
	 * INTEGER | INT
	 */
	protected static final String INTEGER = " INTEGER";

	/**
	 * This type allows storing serialized Java objects.
	 * Internally, a byte array with serialized form is used.
	 * Mapped to java.lang.Object (or any subclass).
	 * OTHER | OBJECT | JAVA_OBJECT
	 */
	protected static final String OTHER = " OTHER";

	/**
	 * Data type with fixed decimal precision and scale.
	 * Mapped to java.math.BigDecimal.
	 * NUMERIC | DECIMAL | DEC
	 */
	protected static final String NUMERIC = " NUMERIC";

	/**
	 * The timestamp data type.
	 * The proleptic Gregorian calendar is used.
	 * If fractional seconds precision is specified it should be from 0 to 9, 6 is default.
	 * Mapped to java.sql.Timestamp.
	 * java.time.LocalDateTime is also supported and recommended.
	 */
	protected static final String TIMESTAMP = " TIMESTAMP";

	/**
	 * Possible values are: -128 to 127.
	 * In JDBC this data type is mapped to java.lang.Integer.
	 * java.lang.Byte is also supported.
	 */
	protected static final String TINYINT = " TINYINT";

	/**
	 * The timestamp data type.
	 * The timestamp with time zone data type.
	 * The proleptic Gregorian calendar is used.
	 * If fractional seconds precision is specified it should be from 0 to 9, 6 is default.
	 * Mapped to java.time.OffsetDateTime.
	 * java.time.ZonedDateTime and java.time.Instant are also supported.
	 */
	protected static final String TIMESTAMP_WITH_TIME_ZONE = TIMESTAMP + " WITH TIME ZONE";

	/**
	 * Universally unique identifier.
	 * This is a 128 bit value.
	 * Please note that using an index on randomly generated data will result on poor performance once there are millions of rows in a table.
	 * The reason is that the cache behavior is very bad with randomly distributed data.
	 * This is a problem for any database system.
	 * ResultSet.getObject will return a java.util.UUID.
	 */
	protected static final String UUID_TYPE = " UUID";

	/**
	 * A Unicode String.
	 * The allowed length is from 1 to 1,000,000,000 characters.
	 * The length is a size constraint; only the actual data is persisted.
	 * Length, if any, should be specified in characters.
	 * Mapped to java.lang.String.
	 */
	protected static final String VARCHAR = " VARCHAR";
	protected static final String VARCHAR_SIZE_MAX = VARCHAR + "(" + SIZE_MAX + ")";
	protected static final String VARCHAR_SIZE_LANG = VARCHAR + "(" + SIZE_LANG + ")";
	protected static final String VARCHAR_3 = VARCHAR_SIZE_LANG;
	protected static final String VARCHAR_5 = VARCHAR + "(5)";
	protected static final String VARCHAR_16 = VARCHAR + "(16)";
	protected static final String VARCHAR_20 = VARCHAR + "(20)";
	protected static final String VARCHAR_32 = VARCHAR + "(32)";
	protected static final String VARCHAR_36 = VARCHAR + "(36)";
	protected static final String VARCHAR_50 = VARCHAR + "(50)";
	protected static final String VARCHAR_255 = VARCHAR_SIZE_MAX;
	protected static final String VARCHAR_1000 = VARCHAR + "(1000)";
	protected static final String VARCHAR_1024 = VARCHAR + "(" + SIZE_1024 + ")";
	protected static final String VARCHAR_20000 = VARCHAR + "(20000)";

	protected static final String AUTO_INCREMENT = " AUTO_INCREMENT";
	protected static final String CONSTRAINT_SEPARATOR = "_";
	protected static final String PRIMARY_KEY = " PRIMARY KEY";
	protected static final String FOREIGN_KEY = " FOREIGN KEY";
	protected static final String DEFAULT = " DEFAULT ";
	protected static final String DEFAULT_0 = DEFAULT + "0";
	protected static final String COMMA = ", ";
	protected static final String IDX_MARKER = "_IDX";
	protected static final String FK_MARKER = "_FK";
	protected static final String PK_MARKER = "_PK";

	//values
	protected static final String CURRENT_TIMESTAMP = "CURRENT_TIMESTAMP";
	protected static final String FALSE = "FALSE";
	protected static final String NULL = "NULL";
	protected static final String TRUE = "TRUE";
	protected static final String PARAMETER = "?";

	/**
	 * SQL COMMANDS
	 */
	protected static final String CREATE = "CREATE ";
	protected static final String INSERT = "INSERT ";
	protected static final String SELECT = "SELECT ";
	protected static final String UPDATE = "UPDATE ";

	protected static final String ADD = " ADD ";
	protected static final String DROP = " DROP ";
	protected static final String ALTER = "ALTER ";
	protected static final String AND = " AND ";
	protected static final String ASC = " ASC";

	protected static final String COLUMN = "COLUMN ";
	protected static final String CONSTRAINT = "CONSTRAINT ";
	protected static final String DELETE_FROM = "DELETE FROM ";
	protected static final String EQUAL = " = ";
	protected static final String EXISTS = "EXISTS ";
	protected static final String FROM = " FROM ";
	protected static final String IF = "IF ";
	protected static final String INDEX = "INDEX ";
	protected static final String NOT = "NOT ";
	protected static final String IS = " IS ";
	protected static final String LEFT_JOIN = " LEFT JOIN ";
	protected static final String LIMIT = " LIMIT ";
	protected static final String LIMIT_1 = LIMIT + "1";
	protected static final String LIKE = " LIKE ";
	protected static final String ON = " ON ";
	protected static final String ON_DELETE_CASCADE = ON + "DELETE CASCADE";
	protected static final String OR = " OR ";
	protected static final String REFERENCES = " REFERENCES ";
	protected static final String RENAME = " RENAME ";

	protected static final String INSERT_INTO = "INSERT INTO ";

	protected static final String SET = " SET ";
	protected static final String TABLE = "TABLE ";
	protected static final String UNIQUE = "UNIQUE ";
	protected static final String WHERE = " WHERE ";

	protected static final String ALTER_COLUMN = " " + ALTER + COLUMN;
	protected static final String ALTER_INDEX = ALTER + INDEX;
	protected static final String ALTER_TABLE = ALTER + TABLE;
	protected static final String CREATE_TABLE = CREATE + TABLE;
	protected static final String CREATE_INDEX = CREATE + INDEX;
	protected static final String CREATE_UNIQUE_INDEX = CREATE + UNIQUE + INDEX;
	protected static final String DROP_INDEX = "DROP " + INDEX;
	protected static final String NOT_NULL = " NOT NULL";
	protected static final String IF_EXISTS = IF + EXISTS;
	protected static final String IF_NOT_EXISTS = IF + NOT + EXISTS;
	protected static final String IS_NOT_NULL = IS + NOT + NULL;
	protected static final String IS_NOT_TRUE = IS + NOT + TRUE;
	protected static final String IS_NULL = IS + NULL;
	protected static final String RENAME_TO = RENAME + "TO ";
	protected static final String SELECT_ALL = SELECT + "*";
	protected static final String UNIQUE_NOT_NULL = " UNIQUE NOT NULL";

	/**
	 * This class is not meant to be instantiated.
	 */
	protected DatabaseHelper() {
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
	 * Drops (deletes) the named table and its constraints. Use with caution, there is no undo.
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
			try (PreparedStatement stmt = connection.prepareStatement(sql)) {
				rs = stmt.executeQuery();

				while (rs.next()) {
					try (Statement statement = connection.createStatement()) {
						statement.execute("ALTER TABLE " + tableName + " DROP CONSTRAINT IF EXISTS " + rs.getString("CONSTRAINT_NAME"));
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("error during dropping table\"" + tableName + "\" constraints:" + e.getMessage(), e);
		}
	}

	/**
	 * Drops referentials constraints on the named table.
	 *
	 * @param connection the {@link Connection} to use
	 * @param tableName the name of the table to delete
	 */
	protected static final void dropCascadeConstraint(final Connection connection, final String tableName) {
		LOGGER.debug("Dropping table \"{}\" constraints if it exists", tableName);
		try {
			String sql;
			ResultSet rs = connection.getMetaData().getTables(null, "INFORMATION_SCHEMA", "TABLE_CONSTRAINTS", null);
			if (rs.next()) {
				sql = "SELECT DISTINCT TC.CONSTRAINT_NAME, TC.TABLE_NAME " +
					"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS AS TC INNER JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS AS RC ON TC.CONSTRAINT_NAME = CCU.CONSTRAINT_NAME INNER JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE AS CCU ON RC.CONSTRAINT_NAME = CCU.CONSTRAINT_NAME " +
					"WHERE CCU.TABLE_NAME = '" + tableName + "'";
			} else {
				return;
			}
			try (PreparedStatement stmt = connection.prepareStatement(sql)) {
				rs = stmt.executeQuery();

				while (rs.next()) {
					try (Statement statement = connection.createStatement()) {
						statement.execute("ALTER TABLE IF EXISTS " + rs.getString("TABLE_NAME") + " DROP CONSTRAINT IF EXISTS " + rs.getString("CONSTRAINT_NAME"));
					}
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
	public static Set<String> convertResultSetToHashSet(ResultSet rs) throws SQLException {
		Set<String> list = new HashSet<>();

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
	public static Map<String, Object> convertSingleResultSetToList(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int columns = md.getColumnCount();
		Map<String, Object> row = new HashMap<>(columns);
		for (int i = 1; i <= columns; ++i) {
			row.put(md.getColumnName(i), rs.getObject(i));
		}
		return row;
	}

	/**
	 * Check if the column name exists in the database.
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
		ResultSet result = connection.getMetaData().getColumns(null, null, table, column);
		if (result.first()) {
			LOGGER.trace("Column \"{}\" found in table \"{}\"", column, table);
			return true;
		} else {
			LOGGER.trace("Column \"{}\" not found in table \"{}\"", column, table);
			return false;
		}
	}

	protected static void executeUpdate(Connection conn, String sql) throws SQLException {
		if (conn != null) {
			try (Statement stmt = conn.createStatement()) {
				LOGGER.trace("Execute Update with SQL \"{}\"", sql);
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
		if (obj instanceof Double value) {
			return value;
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
