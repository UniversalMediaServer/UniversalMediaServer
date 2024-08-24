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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import net.pms.dlna.DLNAThumbnail;
import net.pms.dlna.DLNAThumbnailFixer;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Thumbnails table.
 *
 * It does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author SubJunk & Nadahar
 * @since 7.1.1
 */
public final class MediaTableThumbnails extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableThumbnails.class);
	public static final String TABLE_NAME = "THUMBNAILS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	protected static final String COL_THUMBNAIL = "THUMBNAIL";
	private static final String COL_ID = "ID";
	private static final String COL_MD5 = "MD5";
	private static final String COL_MODIFIED = "MODIFIED";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	private static final String TABLE_COL_MD5 = TABLE_NAME + "." + COL_MD5;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_MD5 = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_MD5 + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_INSERT_ID_MD5 = INSERT_INTO + TABLE_NAME + " (" + COL_THUMBNAIL + COMMA + COL_MODIFIED + COMMA + COL_MD5 + ") VALUES (" + PARAMETER + COMMA + PARAMETER + COMMA + PARAMETER + ")";

	private static final String SQL_DELETE_ID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_CLEANUP = DELETE_FROM + TABLE_NAME + WHERE +
		NOT + EXISTS + "(" + SELECT + MediaTableTVSeries.TABLE_COL_THUMBID + FROM + MediaTableTVSeries.TABLE_NAME + WHERE + MediaTableTVSeries.TABLE_COL_THUMBID + EQUAL + TABLE_COL_ID + ")" +
		AND + NOT + EXISTS + "(" + SELECT + MediaTableFiles.TABLE_COL_THUMBID + FROM + MediaTableFiles.TABLE_NAME + WHERE + MediaTableFiles.TABLE_COL_THUMBID + EQUAL + TABLE_COL_ID + ")";

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
			if (version != null) {
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
				}
			} else {
				LOGGER.warn(LOG_TABLE_UNKNOWN_VERSION_RECREATE, DATABASE_NAME, TABLE_NAME);
				dropTable(connection, TABLE_NAME);
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	/**
	 * This method <strong>MUST</strong> be updated if the table definition are
	 * altered. The changes for each version in the form of
	 * <code>ALTER TABLE</code> must be implemented here.
	 *
	 * @param connection the {@link Connection} to use
	 * @param currentVersion the version to upgrade <strong>from</strong>
	 *
	 * @throws SQLException
	 */
	@SuppressFBWarnings("IIL_PREPARE_STATEMENT_IN_LOOP")
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				default ->
					throw new IllegalStateException(
							getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                + IDENTITY                       + COMMA +
				COL_THUMBNAIL         + OTHER      + NOT_NULL          + COMMA +
				COL_MODIFIED          + TIMESTAMP                      + COMMA +
				COL_MD5               + VARCHAR    + UNIQUE_NOT_NULL   +
			")"
		);
	}

	/**
	 * Attempts to find a thumbnail in this table by MD5 hash.
	 *
	 * If not found, it writes the new thumbnail to this table. Finally, it
	 * returns the ID from this table as the THUMBID.
	 *
	 * @param connection the db connection
	 * @param thumbnail
	 */
	public static synchronized Long setThumbnail(final Connection connection, final DLNAThumbnail thumbnail) {
		String md5Hash = DigestUtils.md5Hex(thumbnail.getBytes(false));
		Long existingId = getThumbnailId(connection, md5Hash);
		if (existingId != null) {
			return existingId;
		}
		try (PreparedStatement insertStatement = connection.prepareStatement(SQL_INSERT_ID_MD5, Statement.RETURN_GENERATED_KEYS)) {
			insertStatement.setObject(1, thumbnail);
			insertStatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
			insertStatement.setString(3, md5Hash);
			insertStatement.executeUpdate();
			return getThumbnailId(connection, md5Hash);
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "writing md5", md5Hash, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Attempts to find a thumbnail id in this table by MD5 hash.
	 *
	 * @param connection the db connection
	 * @param md5Hash
	 */
	private static Long getThumbnailId(final Connection connection, final String md5Hash) {
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ID_MD5)) {
			statement.setString(1, md5Hash);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong(COL_ID);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "searching md5", md5Hash, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	public static DLNAThumbnail getThumbnail(final Connection connection, final Long id) {
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
			statement.setLong(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					DLNAThumbnail thumbnail = null;
					try {
						return ((DLNAThumbnail) resultSet.getObject(COL_THUMBNAIL));
					} catch (SQLException ex) {
						try {
							thumbnail = DLNAThumbnailFixer.fixDLNAThumbnail(resultSet.getBinaryStream(COL_THUMBNAIL));
							if (thumbnail != null) {
								//update thumbnail object
								resultSet.updateObject(COL_THUMBNAIL, thumbnail);
								resultSet.updateRow();
							}
						} catch (IOException ex1) {
							LOGGER.error("Error in DLNAThumbnail deserialization for id \"{}\": {}", id, ex.getMessage());
							LOGGER.info("", ex);
						}
					}
					return thumbnail;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for id \"{}\": {}", id, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Remove entries not in FILES table or TV_SERIES table.
	 *
	 * @param connection
	 */
	public static void cleanup(final Connection connection) {
		try (PreparedStatement statement = connection.prepareStatement(SQL_CLEANUP)) {
			int rows = statement.executeUpdate();
			LOGGER.trace("Removed {} entries in \"{}\"", rows, TABLE_NAME);
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN, DATABASE_NAME, "removing entries", TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
