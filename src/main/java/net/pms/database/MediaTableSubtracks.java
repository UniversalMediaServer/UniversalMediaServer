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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.pms.formats.v2.SubtitleType;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Audiotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableSubtracks extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableSubtracks.class);
	public static final String TABLE_NAME = "SUBTRACKS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 4;

	/**
	 * COLUMNS
	 */
	private static final String COL_ID = "ID";
	public static final String COL_FILEID = "FILEID";
	private static final String COL_LANG = "LANG";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_FORMAT_TYPE = "FORMAT_TYPE";
	private static final String COL_EXTERNALFILE = "EXTERNALFILE";
	private static final String COL_CHARSET = "CHARSET";
	private static final String COL_DEFAULT_FLAG = "DEFAULT_FLAG";
	private static final String COL_FORCED_FLAG = "FORCED_FLAG";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	private static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	private static final String TABLE_COL_EXTERNALFILE = TABLE_NAME + "." + COL_EXTERNALFILE;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_FILEID_ID_EXTERNALFILE = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + TABLE_COL_ID + EQUAL + PARAMETER + AND + TABLE_COL_EXTERNALFILE + EQUAL + PARAMETER;
	private static final String SQL_DELETE_EXTERNALFILE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_EXTERNALFILE + EQUAL + PARAMETER;

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
					if (isColumnExist(connection, TABLE_NAME, "TYPE")) {
						LOGGER.trace("Renaming column name TYPE to FORMAT_TYPE");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`TYPE`" + RENAME_TO + COL_FORMAT_TYPE);
					}
				}
				case 2 -> {
					try {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + RENAME + CONSTRAINT + "PKSUB TO " + TABLE_NAME + PK_MARKER);
					} catch (SQLException e) {
						//PKSUB not found, nothing to update.
					}
				}
				case 3 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_DEFAULT_FLAG + BOOLEAN);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_FORCED_FLAG + BOOLEAN);
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
				COL_ID               + INTEGER               + NOT_NULL                      + COMMA +
				COL_FILEID           + BIGINT                + NOT_NULL                      + COMMA +
				COL_LANG             + VARCHAR_SIZE_LANG                                     + COMMA +
				COL_TITLE            + VARCHAR_SIZE_MAX                                      + COMMA +
				COL_FORMAT_TYPE      + INTEGER                                               + COMMA +
				COL_EXTERNALFILE     + VARCHAR_1000          + NOT_NULL + DEFAULT + "''"     + COMMA +
				COL_CHARSET          + VARCHAR_SIZE_MAX                                      + COMMA +
				COL_DEFAULT_FLAG     + BOOLEAN                                               + COMMA +
				COL_FORCED_FLAG      + BOOLEAN                                               + COMMA +
				CONSTRAINT + TABLE_NAME + PK_MARKER + PRIMARY_KEY + "(" + COL_FILEID + COMMA + COL_ID + COMMA + COL_EXTERNALFILE + ")" + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + PK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")"
		);
	}

	protected static void insertOrUpdateSubtitleTracks(Connection connection, long fileId, MediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getSubTrackCount() < 1) {
			return;
		}

		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				SQL_GET_ALL_FILEID_ID_EXTERNALFILE,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
		) {
			for (MediaSubtitle subtitleTrack : media.getSubtitlesTracks()) {
				String externalFile;
				if (subtitleTrack.getExternalFile() != null) {
					externalFile = subtitleTrack.getExternalFile().getPath();
				} else {
					externalFile = "";
				}
				updateStatement.setLong(1, fileId);
				updateStatement.setInt(2, subtitleTrack.getId());
				updateStatement.setString(3, externalFile);
				try (ResultSet rs = updateStatement.executeQuery()) {
					boolean isCreatingNewRecord = !rs.next();
					if (isCreatingNewRecord) {
						rs.moveToInsertRow();
						rs.updateLong(COL_FILEID, fileId);
						rs.updateInt(COL_ID, subtitleTrack.getId());
						rs.updateString(COL_EXTERNALFILE, externalFile);
					}
					rs.updateString(COL_LANG, StringUtils.left(subtitleTrack.getLang(), SIZE_LANG));
					rs.updateString(COL_TITLE, StringUtils.left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
					rs.updateInt(COL_FORMAT_TYPE, subtitleTrack.getType().getStableIndex());
					rs.updateString(COL_CHARSET, StringUtils.left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
					rs.updateBoolean(COL_DEFAULT_FLAG, subtitleTrack.isDefault());
					rs.updateBoolean(COL_FORCED_FLAG, subtitleTrack.isForced());
					if (isCreatingNewRecord) {
						rs.insertRow();
					} else {
						rs.updateRow();
					}
				}
			}
		}
	}

	protected static List<MediaSubtitle> getSubtitleTracks(Connection connection, long fileId) {
		List<MediaSubtitle> result = new ArrayList<>();
		List<String> externalFileReferencesToRemove = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					String fileName = elements.getString(COL_EXTERNALFILE);
					File externalFile = StringUtils.isNotBlank(fileName) ? new File(fileName) : null;
					if (externalFile != null && !externalFile.exists()) {
						externalFileReferencesToRemove.add(externalFile.getPath());
						continue;
					}
					MediaSubtitle sub = new MediaSubtitle();
					sub.setId(elements.getInt(COL_ID));
					sub.setLang(elements.getString(COL_LANG));
					sub.setSubtitlesTrackTitleFromMetadata(elements.getString(COL_TITLE));
					sub.setType(SubtitleType.valueOfStableIndex(elements.getInt(COL_FORMAT_TYPE)));
					sub.setExternalFileOnly(externalFile);
					sub.setSubCharacterSet(elements.getString(COL_CHARSET));
					sub.setDefault(elements.getBoolean(COL_DEFAULT_FLAG));
					sub.setForced(elements.getBoolean(COL_FORCED_FLAG));
					LOGGER.trace("Adding subtitles from the database: {}", sub.toString());
					result.add(sub);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		} finally {
			if (!externalFileReferencesToRemove.isEmpty()) {
				for (String externalFileReferenceToRemove : externalFileReferencesToRemove) {
					LOGGER.trace("Deleting cached external subtitles from database because the file \"{}\" doesn't exist", externalFileReferenceToRemove);
					try (
						PreparedStatement ps = connection.prepareStatement(SQL_DELETE_EXTERNALFILE);
					) {
						ps.setString(1, sqlQuote(externalFileReferenceToRemove));
						ps.executeUpdate();
					} catch (SQLException se) {
						LOGGER.error("Error deleting cached external subtitles: {}", se.getMessage());
						LOGGER.trace("", se);
					}
				}
			}
		}

		return result;
	}
}
