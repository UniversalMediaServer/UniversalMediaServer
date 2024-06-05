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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.pms.media.WebStreamMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaTableWebResource extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableWebResource.class);
	protected static final String TABLE_NAME = "WEB_RESOURCE";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_URL = "URL";
	private static final String COL_LOGO_URL = "LOGO_URL";
	private static final String COL_GENRE = "GENRE";
	private static final String COL_SAMPLE_RATE = "SAMPLE_RATE";
	private static final String COL_BITRATE = "BITRATE";
	private static final String COL_FORMAT_TYPE = "FORMAT_TYPE";
	private static final String COL_CONTENT_TYPE = "CONTENT_TYPE";
	private static final String COL_THUMBID = "THUMBID";
	private static final String COL_THUMB_SRC = "THUMB_SRC";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_URL = TABLE_NAME + "." + COL_URL;
	public static final String TABLE_COL_THUMBID = TABLE_NAME + "." + COL_THUMBID;

	/**
	 * SQL Queries
	 */
	private static final String SQL_DELETE_URL = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_URL + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_BY_URL = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_URL + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_THUMBID_BY_URL = UPDATE + TABLE_NAME + SET + COL_THUMBID + EQUAL + PARAMETER + COMMA + COL_THUMB_SRC + EQUAL + PARAMETER + WHERE + TABLE_COL_URL + EQUAL + PARAMETER;

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
				COL_URL             + VARCHAR + PRIMARY_KEY + COMMA +
				COL_LOGO_URL        + VARCHAR               + COMMA +
				COL_CONTENT_TYPE    + VARCHAR               + COMMA +
				COL_GENRE           + VARCHAR               + COMMA +
				COL_BITRATE         + INTEGER               + COMMA +
				COL_SAMPLE_RATE     + INTEGER               + COMMA +
				COL_THUMBID         + BIGINT                + COMMA +
				COL_THUMB_SRC       + VARCHAR_32            + COMMA +
				COL_FORMAT_TYPE     + INTEGER               +
				")",
				CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_GENRE + ")",
				CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_THUMBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_THUMBID + ")"
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
	 * Deletes one entry
	 */
	public static void deleteByUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			deleteByUrl(connection, url);
		} catch (Exception e) {
			LOGGER.error("cannot delete web resource entry for given URL {} ", url, e);
		}
	}

	/**
	 * Deletes one entry
	 */
	public static void deleteByUrl(final Connection connection, String url) {
		if (connection == null || StringUtils.isBlank(url)) {
			return;
		}
		try (PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_URL)) {
			updateStatment.setString(1, url);
			updateStatment.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("cannot delete web resource entry for given URL {} ", url, e);
		}
	}

	public static void insertOrUpdateWebResource(WebStreamMetadata meta) {
		if (meta == null) {
			LOGGER.warn("Couldn't write WebResource metadata to the database because there is no metadata information.");
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			insertOrUpdateWebResource(connection, meta);
		} catch (Exception e) {
			LOGGER.error("An error occurred while trying to insert WebResource metadata to the database: {}", e.getMessage());
		}
	}

	public static void insertOrUpdateWebResource(final Connection connection, final WebStreamMetadata meta) {
		if (connection == null || meta == null) {
			return;
		}
		String url = meta.getUrl();
		if (StringUtils.isBlank(url)) {
			LOGGER.warn("Couldn't write WebResource metadata to the database because there is no url.");
			return;
		}
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_ALL_BY_URL, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
				ps.setString(1, meta.getUrl());
				try (ResultSet result = ps.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						LOGGER.trace("Inserting WebResource metadata: {}", meta.toString());
						result.moveToInsertRow();
						result.updateString(COL_URL, meta.getUrl());
					} else {
						LOGGER.trace("Updating WebResource metadata: {}", meta.toString());
					}
					//update only if logo url is set
					if (meta.getLogoUrl() != null) {
						result.updateString(COL_LOGO_URL, meta.getLogoUrl());
						updateLong(result, COL_THUMBID, meta.getThumbnailId());
						updateString(result, COL_THUMB_SRC, meta.getContentType(), 32);
					}
					updateString(result, COL_CONTENT_TYPE, meta.getContentType(), SIZE_MAX);
					updateString(result, COL_GENRE, meta.getGenre(), SIZE_MAX);
					updateInteger(result, COL_BITRATE, meta.getBitrate());
					updateInteger(result, COL_SAMPLE_RATE, meta.getSampleRate());
					updateInteger(result, COL_FORMAT_TYPE, meta.getType());
					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "inserting WebResource metadata entry", url, TABLE_NAME, e.getMessage());
		}
	}

	public static WebStreamMetadata getWebStreamMetadata(String url) {
		if (StringUtils.isBlank(url)) {
			LOGGER.trace("cannot get web resource metadata for empty URL");
			return null;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			return getWebStreamMetadata(connection, url);
		} catch (Exception e) {
			LOGGER.error("cannot get web resource metadata for given URL {} ", url, e);
		}
		return null;
	}

	public static WebStreamMetadata getWebStreamMetadata(final Connection connection, String url) {
		if (StringUtils.isAllBlank(url)) {
			LOGGER.trace("cannot get web resource metadata for empty URL");
			return null;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_BY_URL)) {
			stmt.setString(1, url);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return new WebStreamMetadata(
							rs.getString(COL_URL),
							rs.getString(COL_LOGO_URL),
							rs.getString(COL_GENRE),
							rs.getString(COL_CONTENT_TYPE),
							rs.getInt(COL_SAMPLE_RATE),
							rs.getInt(COL_BITRATE),
							rs.getLong(COL_THUMBID),
							rs.getString(COL_THUMB_SRC),
							rs.getInt(COL_FORMAT_TYPE)
					);
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

	public static void updateThumbnailId(final Connection connection, String url, Long thumbId, String thumbnailSource) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_THUMBID_BY_URL);
			) {
				ps.setLong(1, thumbId);
				ps.setString(2, thumbnailSource);
				ps.setString(3, url);
				ps.executeUpdate();
				LOGGER.trace("THUMBID updated to {} for {}", thumbId, url);
			}
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", url, se.getMessage());
			LOGGER.trace("", se);
		}
	}

}
