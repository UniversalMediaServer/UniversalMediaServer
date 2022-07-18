package net.pms.database.syntax;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostgresTypes implements DbTypes {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostgresTypes.class);

	public String getAutoIncVariableType() {
		return " SERIAL ";
	}

	@Override
	public String rename(String oldColumn, String newColumn) {
		return String.format(" RENAME COLUMN %s TO %s ", oldColumn, newColumn);
	}

	@Override
	public boolean analyseExists(ResultSet rs) throws SQLException {
		if (rs.next()) {
			return rs.getBoolean(1);
		}
		return false;
	}

	@Override
	public String getDouble() {
		return " double precision ";
	}

	@Override
	public String getObjectType() {
		return " bytea ";
	}

	@Override
	public void updateSerialized(ResultSet rs, Object x, String columnLabel) throws SQLException {
		if (x == null) {
			rs.updateNull(columnLabel);
		} else {
			byte[] obj = getBytesFromObject(x);
			ByteArrayInputStream bais = new ByteArrayInputStream(obj);
			rs.updateBinaryStream(columnLabel, bais, obj.length);
		}
	}

	@Override
	public void insertSerialized(PreparedStatement ps, Object x, int parameterIndex) throws SQLException {
		byte[] obj = getBytesFromObject(x);
		ByteArrayInputStream bais = new ByteArrayInputStream(obj);
		if (obj.length > 0) {
			ps.setBinaryStream(parameterIndex, bais, obj.length);
		} else {
			ps.setNull(parameterIndex, Types.BLOB);
		}
	}

	private byte[] getBytesFromObject(Object o) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(o);
			oos.close();
			return baos.toByteArray();
		} catch (IOException e) {
			LOGGER.warn("serialization failed", e);
		}
		return new byte[0];
	}

	@Override
	public <T> T getObject(Class<T> cls, ResultSet rs, String column) throws SQLException {
		byte[] obj = rs.getBytes(column);
		if (obj == null) {
			return null;
		}
		try {
			ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(obj));
			return (T) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			LOGGER.warn("deserialization failed", e);
		}
		return null;
	}

	@Override
	public boolean tableExists(Connection connection, String tableName, String tableSchema) throws SQLException {
		LOGGER.trace("Checking if database table \"{}\" in schema \"{}\" exists", tableName, tableSchema);

		String sql = " SELECT EXISTS ( " + "SELECT FROM INFORMATION_SCHEMA.TABLES " + "WHERE TABLE_SCHEMA = ? " + "AND  TABLE_NAME = ?" +
			"); ";

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, tableSchema.toLowerCase());
			statement.setString(2, tableName.toLowerCase());
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
		String sql = " SELECT EXISTS ( " + "SELECT * FROM INFORMATION_SCHEMA.COLUMNS " + "WHERE TABLE_NAME = ? " +
			"AND COLUMN_NAME = ? ); ";

		try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setString(1, table.toLowerCase());
			statement.setString(2, column.toLowerCase());
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
		return " SMALLINT ";
	}

	@Override
	public String getIdentity() {
		return " SERIAL ";
	}

	@Override
	public String getBlob() {
		return " BYTEA ";
	}

	@Override
	public void updateBinary(ResultSet rs, byte[] data, String columnLabel) throws SQLException {
		if (data != null) {
			rs.updateBinaryStream("COVER", new ByteArrayInputStream(data), data.length);
		} else {
			rs.updateNull("COVER");
		}
	}

	@Override
	public void cleanupMetadataTable(Connection connection, String table) {
		// TODO
	}

	@Override
	public void mergeLikedAlbum(Connection connection, String content) throws SQLException {
		String sql = "insert into music_brainz_release_like (MBID_RELEASE) VALUES ( ? ) ON CONFLICT DO NOTHING";
		PreparedStatement ps = connection.prepareStatement(sql);
		ps.setObject(1, UUID.fromString(content));
		ps.executeUpdate();
	}

	@Override
	public void appendProperty(StringBuilder sb, String op, String val, String field) {
		if ("=".equals(op)) {
			sb.append(String.format(" %s = '%s' ", field, val));
		} else if ("contains".equals(op)) {
			sb.append(String.format("LOWER(%s) LIKE '%%%s%%'", field, val.toLowerCase()));
		} else {
			throw new RuntimeException("unknown or unimplemented operator : " + op);
		}
		sb.append("");
	}
}
