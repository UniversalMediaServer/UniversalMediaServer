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
		execute(connection, "CREATE MEMORY TABLE " + TABLE_NAME + "(" + "MBID_RELEASE	UUID  PRIMARY KEY" + ")");
	}

}
