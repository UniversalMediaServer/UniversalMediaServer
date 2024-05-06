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
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entries in this table are "liked" albums. An album is identified by a
 * musicBrainz releaseID.
 */
public class MediaTableMusicBrainzReleaseLike extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableMusicBrainzReleaseLike.class);
	public static final String TABLE_NAME = "MUSIC_BRAINZ_RELEASE_LIKE";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_MBID_RELEASE = "MBID_RELEASE";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_MBID_RELEASE = TABLE_NAME + "." + COL_MBID_RELEASE;

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		if (!tableExists(connection, TABLE_NAME)) {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection, CREATE_TABLE + TABLE_NAME + "(" + COL_MBID_RELEASE + UUID_TYPE + PRIMARY_KEY + ")");
	}

}
