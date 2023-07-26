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
import java.util.ArrayList;
import java.util.List;
import net.pms.dlna.DLNAThumbnail;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.MediaInfo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Chapters releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableChapters extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableChapters.class);
	protected static final String TABLE_NAME = "CHAPTERS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	protected static final String COL_FILEID = "FILEID";
	private static final String COL_LANG = "LANG";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_START_TIME = "START_TIME";
	private static final String COL_END_TIME = "END_TIME";
	private static final String COL_THUMBNAIL = "THUMBNAIL";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	private static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	private static final String TABLE_COL_LANG = TABLE_NAME + "." + COL_LANG;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_BY_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_BY_FILEID_ID_LANG = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + TABLE_COL_ID + EQUAL + PARAMETER + AND + TABLE_COL_LANG + EQUAL + PARAMETER;

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

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					try {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + RENAME + CONSTRAINT + "PKCHAP TO " + TABLE_NAME + PK_MARKER);
					} catch (SQLException e) {
						//PKCHAP not found, nothing to update.
					}
				}
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

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_ID              + INTEGER                           + NOT_NULL          + COMMA +
				COL_FILEID          + BIGINT                            + NOT_NULL          + COMMA +
				COL_LANG            + VARCHAR_SIZE_LANG                                     + COMMA +
				COL_TITLE           + VARCHAR_SIZE_MAX                                      + COMMA +
				COL_START_TIME      + DOUBLE_PRECISION                                      + COMMA +
				COL_END_TIME        + DOUBLE_PRECISION                                      + COMMA +
				COL_THUMBNAIL       + OTHER                                                 + COMMA +
				CONSTRAINT + TABLE_NAME + PK_MARKER + PRIMARY_KEY +  "(" + COL_FILEID + COMMA + COL_ID + COMMA + COL_LANG + ")" + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")"
		);
	}

	protected static void insertOrUpdateChapters(Connection connection, long fileId, MediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || !media.hasChapters()) {
			return;
		}

		try (
			PreparedStatement updateStatement = connection.prepareStatement(SQL_GET_ALL_BY_FILEID_ID_LANG, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			for (MediaChapter chapter : media.getChapters()) {
				updateStatement.clearParameters();
				updateStatement.setLong(1, fileId);
				updateStatement.setInt(2, chapter.getId());
				updateStatement.setString(3, chapter.getLang());
				try (ResultSet result = updateStatement.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						result.moveToInsertRow();
						result.updateLong(COL_FILEID, fileId);
						result.updateInt(COL_ID, chapter.getId());
						result.updateString(COL_LANG, chapter.getLang());
					}
					result.updateString(COL_TITLE, StringUtils.left(chapter.getTitle(), SIZE_MAX));
					result.updateDouble(COL_START_TIME, chapter.getStart());
					result.updateDouble(COL_END_TIME, chapter.getEnd());
					result.updateObject(COL_THUMBNAIL, chapter.getThumbnail());
					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				}
			}
		}
	}

	protected static List<MediaChapter> getChapters(Connection connection, long fileId) {
		List<MediaChapter> result = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_BY_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					MediaChapter chapter = new MediaChapter();
					chapter.setId(elements.getInt(COL_ID));
					chapter.setLang(elements.getString(COL_LANG));
					chapter.setTitle(elements.getString(COL_TITLE));
					chapter.setStart(elements.getDouble(COL_START_TIME));
					chapter.setEnd(elements.getDouble(COL_END_TIME));
					chapter.setThumbnail((DLNAThumbnail) elements.getObject(COL_THUMBNAIL));
					LOGGER.trace("Adding chapter from the database: {}", chapter.toString());
					result.add(chapter);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

}
