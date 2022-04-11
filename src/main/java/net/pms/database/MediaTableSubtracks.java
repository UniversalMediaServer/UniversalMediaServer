/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import static org.apache.commons.lang3.StringUtils.left;
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

	private static final int SIZE_LANG = 3;
	private static final int SIZE_EXTERNALFILE = 1000;

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

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
				case 1:
					if (isColumnExist(connection, TABLE_NAME, "TYPE")) {
						LOGGER.trace("Renaming column name TYPE to FORMAT_TYPE");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `TYPE` RENAME TO FORMAT_TYPE");
					}
					break;
				default:
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
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
			"CREATE TABLE SUBTRACKS (" +
				"ID             INT									NOT NULL			, " +
				"FILEID         BIGINT								NOT NULL			, " +
				"LANG           VARCHAR2(" + SIZE_LANG + ")								, " +
				"TITLE          VARCHAR2(" + SIZE_MAX + ")								, " +
				"FORMAT_TYPE    INT														, " +
				"EXTERNALFILE   VARCHAR2(" + SIZE_EXTERNALFILE + ")	NOT NULL default ''	, " +
				"CHARSET        VARCHAR2(" + SIZE_MAX + ")								, " +
				"CONSTRAINT PKSUB PRIMARY KEY (FILEID, ID, EXTERNALFILE)				, " +
				"FOREIGN KEY(FILEID) REFERENCES FILES(ID) ON DELETE CASCADE" +
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
				"INSERT INTO SUBTRACKS (" + columns +	")" +
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
						rs.updateString("LANG", left(subtitleTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("FORMAT_TYPE", subtitleTrack.getType().getStableIndex());
						if (subtitleTrack.getExternalFile() != null) {
							rs.updateString("EXTERNALFILE", left(subtitleTrack.getExternalFile().getPath(), SIZE_EXTERNALFILE));
						} else {
							rs.updateString("EXTERNALFILE", "");
						}
						rs.updateString("CHARSET", left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, subtitleTrack.getId());
						insertStatement.setString(3, left(subtitleTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(subtitleTrack.getSubtitlesTrackTitleFromMetadata(), SIZE_MAX));
						insertStatement.setInt(5, subtitleTrack.getType().getStableIndex());
						if (subtitleTrack.getExternalFile() != null) {
							insertStatement.setString(6, left(subtitleTrack.getExternalFile().getPath(), SIZE_EXTERNALFILE));
						} else {
							insertStatement.setString(6, "");
						}
						insertStatement.setString(7, left(subtitleTrack.getSubCharacterSet(), SIZE_MAX));
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}
}
