package net.pms.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Audiotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class TableAudiotracks extends Tables {
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableAudiotracks.class);
	public static final String TABLE_NAME = "AUDIOTRACKS";
	private static final String MBID_RECORD = "MBID_RECORD";
	private static final String MBID_TRACK = "MBID_TRACK";
	private static final int SIZE_LANG = 3;
	private static final int SIZE_GENRE = 64;
	private static final int SIZE_MUXINGMODE = 32;
	private static final int SIZE_MAX = 255;
	private static final int SIZE_MBID = 36;
	private static final int SIZE_SAMPLEFREQ = 16;
	private static final int SIZE_CODECA = 32;

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
		TABLE_LOCK.writeLock().lock();
		try {
			if (tableExists(connection, TABLE_NAME)) {
				Integer version = getTableVersion(connection, TABLE_NAME);
				if (version != null) {
					if (version < TABLE_VERSION) {
						upgradeTable(connection, version);
					} else if (version > TABLE_VERSION) {
						LOGGER.warn("Database table \"" + TABLE_NAME + "\" is from a newer version of UMS.");
					}
				} else {
					// Moving sql from DLNAMediaDatabase to this class.
					upgradeTable(connection, null);
				}
			} else {
				createTable(connection);
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		TABLE_LOCK.writeLock().lock();
		try {
			if (currentVersion == null || currentVersion == 1) {
				try (Statement statement = connection.createStatement()) {
					if (!isColumnExist(connection, TABLE_NAME, MBID_RECORD)) {
						statement.execute("ALTER TABLE " + TABLE_NAME + " ADD " + MBID_RECORD + " UUID");
					}
					if (!isColumnExist(connection, TABLE_NAME, MBID_TRACK)) {
						statement.execute("ALTER TABLE " + TABLE_NAME + " ADD " + MBID_TRACK + " UUID");
					}
				} catch (SQLException e) {
					LOGGER.error("Failed upgrading database table {} for {}", TABLE_NAME, e.getMessage());
					LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
					throw new SQLException(e);
				}
			}

			try {
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			} catch (SQLException e) {
				LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
				throw new SQLException(e);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " + TABLE_NAME + " (");
			sb.append("  ID                INT              NOT NULL");
			sb.append(", FILEID            BIGINT           NOT NULL");
			sb.append(", MBID_RECORD       UUID");
			sb.append(", MBID_TRACK        UUID");
			sb.append(", LANG              VARCHAR2(").append(SIZE_LANG).append(')');
			sb.append(", TITLE             VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", NRAUDIOCHANNELS   NUMERIC");
			sb.append(", SAMPLEFREQ        VARCHAR2(").append(SIZE_SAMPLEFREQ).append(')');
			sb.append(", CODECA            VARCHAR2(").append(SIZE_CODECA).append(')');
			sb.append(", BITSPERSAMPLE     INT");
			sb.append(", ALBUM             VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", ARTIST            VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", ALBUMARTIST       VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", SONGNAME          VARCHAR2(").append(SIZE_MAX).append(')');
			sb.append(", GENRE             VARCHAR2(").append(SIZE_GENRE).append(')');
			sb.append(", YEAR              INT");
			sb.append(", TRACK             INT");
			sb.append(", DELAY             INT");
			sb.append(", MUXINGMODE        VARCHAR2(").append(SIZE_MUXINGMODE).append(')');
			sb.append(", BITRATE           INT");
			sb.append(", constraint PKAUDIO primary key (FILEID, ID)");
			sb.append(", FOREIGN KEY(FILEID)");
			sb.append("    REFERENCES FILES(ID)");
			sb.append("    ON DELETE CASCADE");
			sb.append(')');

			statement.execute(sb.toString());

			LOGGER.trace("Creating index IDXARTIST");
			executeUpdate(connection, "CREATE INDEX IDXARTIST on AUDIOTRACKS (ARTIST asc);");

			LOGGER.trace("Creating index IDXALBUMARTIST");
			executeUpdate(connection, "CREATE INDEX IDXALBUMARTIST on AUDIOTRACKS (ALBUMARTIST asc);");

			LOGGER.trace("Creating index IDXALBUM");
			executeUpdate(connection, "CREATE INDEX IDXALBUM on AUDIOTRACKS (ALBUM asc);");

			LOGGER.trace("Creating index IDXGENRE");
			executeUpdate(connection, "CREATE INDEX IDXGENRE on AUDIOTRACKS (GENRE asc);");

			LOGGER.trace("Creating index IDXYEAR");
			executeUpdate(connection, "CREATE INDEX IDXYEAR on AUDIOTRACKS (YEAR asc);");
		}
	}

	private static void executeUpdate(Connection conn, String sql) throws SQLException {
		if (conn != null) {
			try (Statement stmt = conn.createStatement()) {
				stmt.executeUpdate(sql);
			}
		}
	}

	/**
	 * Drops (deletes) the current table. Use with caution, there is no undo.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static final void dropTable(final Connection connection) throws SQLException {
		LOGGER.debug("Dropping database table if it exists \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
		}
	}
}
