package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.store.utils.WebStreamMetadata;

public class MediaTableWebResource extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableWebResource.class);
	protected static final String TABLE_NAME = "WEB_RESOURCE";

	private static final String COL_URL = "URL";
	private static final String COL_LOGO_URL = "LOGO_URL";
	private static final String COL_GENRE = "GENRE";
	private static final String COL_SAMPLE_RATE = "SAMPLE_RATE";
	private static final String COL_BITRATE = "BITRATE";
	private static final String COL_TYPE = "TYPE";
	private static final String COL_CONTENT_TYPE = "CONTENT_TYPE";

	private static final String SQL_MERGE_RESOURCE = MERGE_INTO + TABLE_NAME + "(" + COL_URL + COMMA + COL_LOGO_URL + COMMA + COL_CONTENT_TYPE +
		COMMA + COL_GENRE + COMMA + COL_BITRATE + COMMA + COL_SAMPLE_RATE + COMMA + COL_TYPE + ")" + VALUES + " ( ?, ?, ?, ?, ?, ?, ?) ";

	private static final String SQL_MERGE_RESOURCE_WITHOUT_LOGO = MERGE_INTO + TABLE_NAME + "(" + COL_URL + COMMA + COL_CONTENT_TYPE +
		COMMA + COL_GENRE + COMMA + COL_BITRATE + COMMA + COL_SAMPLE_RATE + COMMA + COL_TYPE + ")" + VALUES + " ( ?, ?, ?, ?, ?, ?) ";

	private static final String SQL_DELETE_ALL = DELETE_FROM + TABLE_NAME;
	private static final String SQL_DELETE_URL = DELETE_FROM + TABLE_NAME + WHERE + COL_URL + EQUAL + PARAMETER;

	private static final String SQL_GET_ALL_BY_URL = SELECT_ALL + FROM + TABLE_NAME + WHERE + COL_URL + EQUAL + PARAMETER;

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

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


	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_URL                     + VARCHAR               + PRIMARY_KEY            + COMMA +
				COL_LOGO_URL                + VARCHAR                                        + COMMA +
				COL_CONTENT_TYPE            + VARCHAR                                        + COMMA +
				COL_GENRE                   + VARCHAR                                        + COMMA +
				COL_BITRATE                 + INTEGER                                        + COMMA +
				COL_SAMPLE_RATE             + INTEGER                                        + COMMA +
				COL_TYPE                    + INTEGER                                        +
			")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_GENRE + ")"
		);
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				default -> {
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
				}
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

	/**
	 * Can be used to clear the cache which means to clear all entries.
	 */
	public static void deleteAllEntries() {
		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			Statement stmt = connection.createStatement();
		) {
			LOGGER.debug("deleting all WEB_RESOURCE entries ...");
			stmt.execute(SQL_DELETE_ALL);
		} catch (Exception e) {
			LOGGER.error("cannot delete web resource entries.", e);
		}
	}

	/**
	 * Deletes one entry
	 */
	public static void deleteByUrl(String url) {
		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_URL);
		) {
			updateStatment.setString(1, url);
			updateStatment.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("cannot delete web resource entry for given URL {} ", url, e);
		}
	}

	public static void insertOrUpdateWebResource(WebStreamMetadata meta) {
		if (StringUtils.isAllBlank(meta.LOGO_URL())) {
			insertOrUpdateWebResourceWithoutLogo(meta);
		} else {
			insertOrUpdateWebResourceWithLogo(meta);
		}
	}

	public static void insertOrUpdateWebResourceWithLogo(WebStreamMetadata meta) {
		if (meta == null) {
			LOGGER.trace("no metadata ... do not store any data.");
			return;
		}
		LOGGER.trace("adding WebResourceMeta to database : {}", meta.toString());

		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			PreparedStatement mergeStatement = connection.prepareStatement(SQL_MERGE_RESOURCE, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			mergeStatement.setString(1, meta.URL());
			if (meta.LOGO_URL() != null) {
				mergeStatement.setString(2, meta.LOGO_URL());
			} else {
				mergeStatement.setNull(2, Types.VARCHAR);
			}
			if (meta.CONTENT_TYPE() != null) {
				mergeStatement.setString(3, meta.CONTENT_TYPE());
			} else {
				mergeStatement.setNull(3, Types.VARCHAR);
			}
			if (meta.GENRE() != null) {
				mergeStatement.setString(4, meta.GENRE());
			} else {
				mergeStatement.setNull(4, Types.VARCHAR);
			}
			if (meta.BITRATE() != null) {
				mergeStatement.setInt(5, meta.BITRATE());
			} else {
				mergeStatement.setNull(5, Types.INTEGER);
			}
			if (meta.SAMPLE_RATE() != null) {
				mergeStatement.setInt(6, meta.SAMPLE_RATE());
			} else {
				mergeStatement.setNull(6, Types.INTEGER);
			}
			mergeStatement.setInt(7, meta.TYPE());
			mergeStatement.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("cannot merge web resource {} ", meta, e);
		}
	}

	public static void insertOrUpdateWebResourceWithoutLogo(WebStreamMetadata meta) {
		if (meta == null) {
			LOGGER.trace("no metadata ... do not store any data.");
			return;
		}
		LOGGER.trace("adding WebResourceMeta to database (without logo) : {}", meta.toString());

		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			PreparedStatement mergeStatement = connection.prepareStatement(SQL_MERGE_RESOURCE_WITHOUT_LOGO, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			mergeStatement.setString(1, meta.URL());
			if (meta.CONTENT_TYPE() != null) {
				mergeStatement.setString(2, meta.CONTENT_TYPE());
			} else {
				mergeStatement.setNull(2, Types.VARCHAR);
			}
			if (meta.GENRE() != null) {
				mergeStatement.setString(3, meta.GENRE());
			} else {
				mergeStatement.setNull(3, Types.VARCHAR);
			}
			if (meta.BITRATE() != null) {
				mergeStatement.setInt(4, meta.BITRATE());
			} else {
				mergeStatement.setNull(4, Types.INTEGER);
			}
			if (meta.SAMPLE_RATE() != null) {
				mergeStatement.setInt(5, meta.SAMPLE_RATE());
			} else {
				mergeStatement.setNull(5, Types.INTEGER);
			}
			mergeStatement.setInt(6, meta.TYPE());
			mergeStatement.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("cannot merge web resource (without logo) {} ", meta, e);
		}
	}

	public static WebStreamMetadata getWebStreamMetadata(String url) {
		if (StringUtils.isAllBlank(url)) {
			LOGGER.trace("given URL is null");
			return null;
		}
		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_BY_URL)) {
			stmt.setString(1, url);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					WebStreamMetadata meta = new WebStreamMetadata(
						rs.getString(COL_URL),
						rs.getString(COL_LOGO_URL),
						rs.getString(COL_GENRE),
						rs.getString(COL_CONTENT_TYPE),
						rs.getInt(COL_SAMPLE_RATE),
						rs.getInt(COL_BITRATE),
						rs.getInt(COL_TYPE));
					return meta;
				} else {
					LOGGER.trace("no record found for {}", url);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", url, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

}
