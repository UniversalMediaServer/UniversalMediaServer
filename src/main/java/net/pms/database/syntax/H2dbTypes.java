package net.pms.database.syntax;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.h2.tools.RunScript;
import org.h2.tools.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableTVSeries;

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

		String sql = " SELECT * FROM INFORMATION_SCHEMA.TABLES " + "WHERE TABLE_SCHEMA = ? " + "AND  TABLE_NAME = ? ";

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
		String sql = " SELECT * FROM INFORMATION_SCHEMA.COLUMNS " + "WHERE TABLE_NAME = ? " + "AND COLUMN_NAME = ? ";

		try (PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
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

	@Override
	public void cleanupMetadataTable(Connection connection, String table) {
		String sql = "DELETE FROM " + table + " " + "WHERE NOT EXISTS (" + "SELECT FILENAME FROM FILES " + "WHERE FILES.FILENAME = " +
			table + ".FILENAME " + "LIMIT 1" + ") AND NOT EXISTS (" + "SELECT ID FROM " + MediaTableTVSeries.TABLE_NAME + " " + "WHERE " +
			MediaTableTVSeries.TABLE_NAME + ".ID = " + table + ".TVSERIESID " + "LIMIT 1" + ");";
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.execute();
		} catch (SQLException e) {
			LOGGER.error("cleanup failed.", e);
		}
	}

	@Override
	public void mergeLikedAlbum(Connection connection, String content) throws SQLException {
		String sql = "MERGE INTO MUSIC_BRAINZ_RELEASE_LIKE KEY (MBID_RELEASE) values (?)";
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

	@Override
	public void backupLikedAlbums(MediaDatabase db, String backupFilename) throws SQLException {
		try (Connection connection = db.getConnection()) {
			Script.process(connection, backupFilename, "", "TABLE MUSIC_BRAINZ_RELEASE_LIKE");
		}
	}

	@Override
	public void restoreLikedAlbums(MediaDatabase db, String backupFilename) throws SQLException {
		File backupFile = new File(backupFilename);
		if (backupFile.exists() && backupFile.isFile()) {
			try (Connection connection = db.getConnection(); Statement stmt = connection.createStatement()) {
				String sql;
				sql = "DROP TABLE MUSIC_BRAINZ_RELEASE_LIKE";
				stmt.execute(sql);
				try {
					RunScript.execute(connection, new FileReader(backupFilename));
				} catch (Exception e) {
					LOGGER.error("restoring MUSIC_BRAINZ_RELEASE_LIKE table : failed");
					throw new RuntimeException("restoring MUSIC_BRAINZ_RELEASE_LIKE table failed", e);
				}
				connection.commit();
				LOGGER.trace("restoring MUSIC_BRAINZ_RELEASE_LIKE table : success");
			}
		} else {
			if (!StringUtils.isEmpty(backupFilename)) {
				LOGGER.trace("Backup file doesn't exist : " + backupFilename);
				throw new RuntimeException("Backup file doesn't exist : " + backupFilename);
			} else {
				throw new RuntimeException("Backup filename not set !");
			}
		}
	}

	@Override
	public String createTable() {
		return "CREATE MEMORY TABLE ";
	}
}
