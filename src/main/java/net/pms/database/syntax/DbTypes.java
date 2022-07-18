package net.pms.database.syntax;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DbTypes {

	String getAutoIncVariableType();

	String getDouble();

	String getSmallInt();

	boolean analyseExists(ResultSet rs) throws SQLException;

	<T> T getObject(Class<T> cls, ResultSet rs, String column) throws SQLException;

	String rename(String oldColumn, String newColumn);

	String getObjectType();

	void updateSerialized(ResultSet rs, Object x, String columnLabel) throws SQLException;

	void updateBinary(ResultSet rs, byte[] data, String columnLabel) throws SQLException;

	void insertSerialized(PreparedStatement ps, Object x, int parameterIndex) throws SQLException;

	boolean tableExists(final Connection connection, final String tableName, final String tableSchema) throws SQLException;

	boolean isColumnExist(Connection connection, String table, String column) throws SQLException;

	String getIdentity();

	String getBlob();

	void cleanupMetadataTable(Connection connection, String table);

	void mergeLikedAlbum(Connection connection, String content) throws SQLException;
}
