/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
import net.pms.dlna.DLNAMediaChapter;
import net.pms.dlna.DLNAMediaInfo;
import static org.apache.commons.lang3.StringUtils.left;
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
	private static final int SIZE_LANG = 3;

	public static final String TABLE_NAME = "CHAPTERS";

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

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
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
			"CREATE MEMORY TABLE " + TABLE_NAME + " (" +
				"ID             INT                                 NOT NULL            , " +
				"FILEID         BIGINT                              NOT NULL            , " +
				"LANG           VARCHAR2(" + SIZE_LANG + ")                             , " +
				"TITLE          VARCHAR2(" + SIZE_MAX + ")                              , " +
				"START_TIME     DOUBLE                                                  , " +
				"END_TIME       DOUBLE                                                  , " +
				"THUMBNAIL		OTHER	                    		            		, " +
				"CONSTRAINT PKCHAP PRIMARY KEY (FILEID, ID, LANG)                       , " +
				"FOREIGN KEY(FILEID) REFERENCES FILES(ID) ON DELETE CASCADE" +
			")"
		);
	}

	protected static void insertOrUpdateChapters(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || !media.hasChapters()) {
			return;
		}

		String columns = "FILEID, ID, LANG, TITLE, START_TIME, END_TIME, THUMBNAIL ";

		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				"SELECT " + columns +
				"FROM " + TABLE_NAME + " " +
				"WHERE " +
					"FILEID = ? AND ID = ? AND LANG = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO " + TABLE_NAME + " (" + columns +	")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (DLNAMediaChapter chapter : media.getChapters()) {
				updateStatement.setLong(1, fileId);
				updateStatement.setInt(2, chapter.getId());
				updateStatement.setString(3, chapter.getLang());
				try (ResultSet rs = updateStatement.executeQuery()) {
					if (rs.next()) {
						rs.updateString("TITLE", left(chapter.getTitle(), SIZE_MAX));
						rs.updateDouble("START_TIME", chapter.getStart());
						rs.updateDouble("END_TIME", chapter.getEnd());
						rs.updateObject("THUMBNAIL", chapter.getThumbnail());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, chapter.getId());
						insertStatement.setString(3, left(chapter.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(chapter.getTitle(), SIZE_MAX));
						insertStatement.setDouble(5, chapter.getStart());
						insertStatement.setDouble(6, chapter.getEnd());
						insertStatement.setObject(7, chapter.getThumbnail());
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}
}
