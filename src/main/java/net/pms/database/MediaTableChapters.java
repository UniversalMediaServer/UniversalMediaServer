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
import net.pms.dlna.DLNAMediaChapter;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
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
	private static final int SIZE_LANG = 3;

	public static final String TABLE_NAME = "CHAPTERS";
	public static final String COL_FILEID = "FILEID";
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;

	private static final String SQL_GET_ALL_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ?";

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
				case 1 -> {
					try {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " RENAME CONSTRAINT PKCHAP TO " + TABLE_NAME + "_PK");
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
			"CREATE TABLE " + TABLE_NAME + " (" +
				"ID             INTEGER                             NOT NULL            , " +
				"FILEID         BIGINT                              NOT NULL            , " +
				"LANG           VARCHAR(" + SIZE_LANG + ")                              , " +
				"TITLE          VARCHAR(" + SIZE_MAX + ")                               , " +
				"START_TIME     DOUBLE PRECISION                                        , " +
				"END_TIME       DOUBLE PRECISION                                        , " +
				"THUMBNAIL      OTHER                                                   , " +
				"CONSTRAINT " + TABLE_NAME + "_PK PRIMARY KEY (FILEID, ID, LANG)        , " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY(" + COL_FILEID + ") REFERENCES " + MediaTableFiles.REFERENCE_TABLE_COL_ID + " ON DELETE CASCADE" +
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
						rs.updateString("TITLE", StringUtils.left(chapter.getTitle(), SIZE_MAX));
						rs.updateDouble("START_TIME", chapter.getStart());
						rs.updateDouble("END_TIME", chapter.getEnd());
						rs.updateObject("THUMBNAIL", chapter.getThumbnail());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						int databaseColumnIterator = 0;
						insertStatement.setLong(++databaseColumnIterator, fileId);
						insertStatement.setInt(++databaseColumnIterator, chapter.getId());
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(chapter.getLang(), SIZE_LANG));
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(chapter.getTitle(), SIZE_MAX));
						insertStatement.setDouble(++databaseColumnIterator, chapter.getStart());
						insertStatement.setDouble(++databaseColumnIterator, chapter.getEnd());
						insertStatement.setObject(++databaseColumnIterator, chapter.getThumbnail());
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	protected static List<DLNAMediaChapter> getChapters(Connection connection, long fileId) {
		List<DLNAMediaChapter> result = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					DLNAMediaChapter chapter = new DLNAMediaChapter();
					chapter.setId(elements.getInt("ID"));
					chapter.setLang(elements.getString("LANG"));
					chapter.setTitle(elements.getString("TITLE"));
					chapter.setStart(elements.getDouble("START_TIME"));
					chapter.setEnd(elements.getDouble("END_TIME"));
					chapter.setThumbnail((DLNAThumbnail) elements.getObject("THUMBNAIL"));
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
