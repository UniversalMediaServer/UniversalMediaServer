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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class MediaTableSubtitleHashCache extends MediaTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableTablesVersions.class);
    public static final String TABLE_NAME = "SUBTITLE_HASH_CACHE";
    public static final String COL_FILE_NAME = "FILE_NAME";
    public static final String COL_HASH = "HASH";
    private static final int TABLE_VERSION = 1;

    protected static void checkTable(final Connection connection) throws SQLException {
        if (!tableExists(connection, TABLE_NAME)) {
            createTable(connection);
            MediaTableTablesVersions.setTableVersion(connection,TABLE_NAME,TABLE_VERSION);
        }
    }

    protected static final void createTable(final Connection connection) throws SQLException {
        LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
        execute(connection,
                "CREATE TABLE " + TABLE_NAME + "(" +
                        "ID IDENTITY PRIMARY KEY, " +
                        COL_FILE_NAME+" VARCHAR(255) NOT NULL, " +
                        COL_HASH+" VARCHAR(50) NOT NULL" +
                        ")"
        );
    }
}
