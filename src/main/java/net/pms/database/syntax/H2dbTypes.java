package net.pms.database.syntax;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H2dbTypes implements DbTypes {

	private static final Logger LOGGER = LoggerFactory.getLogger(H2dbTypes.class);

	public String getAutoIncVariableType() {
		return " INT AUTO_INCREMENT ";
	}

	@Override
	public String rename(String oldColumn, String newColumn) {
		return String.format(" ALTER COLUMN `%s` RENAME TO %s ", oldColumn, newColumn);
	}

	@Override
	public boolean analyseExists(ResultSet rs) throws SQLException {
		return rs.next();
	}

	@Override
	public String getDouble() {
		return " DOUBLE ";
	}

	@Override
	public String getObjectType() {
		return " other ";
	}

	@Override
	public void updateSerialized(ResultSet rs, Object x, String columnLabel) throws SQLException {
		if (x != null) {
			rs.updateObject(columnLabel, x);
		} else {
			rs.updateNull(columnLabel);
		}
	}

	@Override
	public void insertSerialized(PreparedStatement ps, Object x, int parameterIndex) throws SQLException {
		if (x != null) {
			ps.setObject(parameterIndex, x);
		} else {
			ps.setNull(parameterIndex, Types.OTHER);
		}
	}

	@Override
	public <T> T getObject(Class<T> cls, ResultSet rs, String column) throws SQLException {
		return (T) rs.getObject(column);
	}

	@Override
	public boolean tableExists(Connection connection, String tableName, String tableSchema) throws SQLException {
		LOGGER.trace("Checking if database table \"{}\" in schema \"{}\" exists", tableName, tableSchema);

		String sql = " SELECT * FROM INFORMATION_SCHEMA.TABLES " +
			"WHERE TABLE_SCHEMA = ? " +
			"AND  TABLE_NAME = ? ";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, tableSchema);
			statement.setString(2, tableName);
			try (ResultSet result = statement.executeQuery()) {
				if (analyseExists(result)) {
					LOGGER.trace("Database table \"{}\" found", tableName);
					return true;
				} else {
					LOGGER.trace("Database table \"{}\" not found", tableName);
					return false;
				}
			}
		}
	}

	@Override
	public boolean isColumnExist(Connection connection, String table, String column) throws SQLException {
		String sql = " SELECT * FROM INFORMATION_SCHEMA.COLUMNS " +
			"WHERE TABLE_NAME = ? " +
			"AND COLUMN_NAME = ? ";

		try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE
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

	@Override
	public String getSmallInt() {
		return " TINYINT ";
	}

	@Override
	public String getIdentity() {
		return " IDENTITY ";
	}

	@Override
	public String getBlob() {
		return " BLOB ";
	}

	@Override
	public void updateBinary(ResultSet rs, byte[] data, String columnLabel) throws SQLException {
		if (data != null) {
			rs.updateBinaryStream("COVER", new ByteArrayInputStream(data));
		} else {
			rs.updateNull("COVER");
		}
	}
}
