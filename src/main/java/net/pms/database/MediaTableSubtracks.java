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
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.formats.v2.SubtitleType;
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
	public static final String COL_FILEID = "FILEID";
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;

	private static final String SQL_GET_ALL_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ?";
	private static final String SQL_DELETE_EXTERNALFILE = "DELETE FROM " + TABLE_NAME + " WHERE EXTERNALFILE = ?";

	private static final int SIZE_LANG = 3;
	private static final int SIZE_EXTERNALFILE = 1000;

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 3;

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
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `TYPE` RENAME TO FORMAT_TYPE");
					}
				}
				case 2 -> {
					try {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " RENAME CONSTRAINT PKSUB TO " + TABLE_NAME + "_PK");
					} catch (SQLException e) {
						//PKSUB not found, nothing to update.
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
			"CREATE TABLE " + TABLE_NAME + " (" +
				"ID             INTEGER                             NOT NULL            , " +
				"FILEID         BIGINT                              NOT NULL            , " +
				"LANG           VARCHAR(" + SIZE_LANG + ")                              , " +
				"TITLE          VARCHAR(" + SIZE_MAX + ")                               , " +
				"FORMAT_TYPE    INTEGER                                                 , " +
				"EXTERNALFILE   VARCHAR(" + SIZE_EXTERNALFILE + ")  NOT NULL default '' , " +
				"CHARSET        VARCHAR(" + SIZE_MAX + ")                               , " +
				"CONSTRAINT " + TABLE_NAME + "_PK PRIMARY KEY (FILEID, ID, EXTERNALFILE), " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY(" + COL_FILEID + ") REFERENCES " + MediaTableFiles.REFERENCE_TABLE_COL_ID + " ON DELETE CASCADE" +
			")"
		);
	}

	protected static void insertOrUpdateSubtitleTracks(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getSubTrackCount() < 1) {
			return;
		}

		String columns = "FILEID, ID, LANG, TITLE, FORMAT_TYPE, EXTERNALFILE, CHARSET ";

		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				"SELECT " +
					"FILEID, ID, LANG, TITLE, FORMAT_TYPE, EXTERNALFILE, CHARSET " +
				"FROM " + TABLE_NAME + " " +
				"WHERE " +
					"FILEID = ? AND ID = ? AND EXTERNALFILE = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO " + TABLE_NAME + " (" + columns +	")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (DLNAMediaSubtitle subtitleTrack : media.getSubtitlesTracks()) {
				updateStatement.setLong(1, fileId);
				updateStatement.setInt(2, subtitleTrack.getId());
				if (subtitleTrack.getExternalFile() != null) {
					updateStatement.setString(3, subtitleTrack.getExternalFile().getPath());
				} else {
					updateStatement.setString(3, "");
				}
				try (ResultSet rs = updateStatement.executeQuery()) {
					if (rs.next()) {
						rs.updateString("LANG", StringUtils.left(subtitleTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", StringUtils.left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("FORMAT_TYPE", subtitleTrack.getType().getStableIndex());
						if (subtitleTrack.getExternalFile() != null) {
							rs.updateString("EXTERNALFILE", StringUtils.left(subtitleTrack.getExternalFile().getPath(), SIZE_EXTERNALFILE));
						} else {
							rs.updateString("EXTERNALFILE", "");
						}
						rs.updateString("CHARSET", StringUtils.left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, subtitleTrack.getId());
						insertStatement.setString(3, StringUtils.left(subtitleTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, StringUtils.left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						insertStatement.setInt(5, subtitleTrack.getType().getStableIndex());
						if (subtitleTrack.getExternalFile() != null) {
							insertStatement.setString(6, StringUtils.left(subtitleTrack.getExternalFile().getPath(), SIZE_EXTERNALFILE));
						} else {
							insertStatement.setString(6, "");
						}
						insertStatement.setString(7, StringUtils.left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	protected static List<DLNAMediaSubtitle> getSubtitleTracks(Connection connection, long fileId) {
		List<DLNAMediaSubtitle> result = new ArrayList<>();
		List<String> externalFileReferencesToRemove = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					String fileName = elements.getString("EXTERNALFILE");
					File externalFile = StringUtils.isNotBlank(fileName) ? new File(fileName) : null;
					if (externalFile != null && !externalFile.exists()) {
						externalFileReferencesToRemove.add(externalFile.getPath());
						continue;
					}
					DLNAMediaSubtitle sub = new DLNAMediaSubtitle();
					sub.setId(elements.getInt("ID"));
					sub.setLang(elements.getString("LANG"));
					sub.setSubtitlesTrackTitleFromMetadata(elements.getString("TITLE"));
					sub.setType(SubtitleType.valueOfStableIndex(elements.getInt("FORMAT_TYPE")));
					sub.setExternalFileOnly(externalFile);
					sub.setSubCharacterSet(elements.getString("CHARSET"));
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
