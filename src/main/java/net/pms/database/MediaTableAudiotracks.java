/*
 * Universal Media Server, for streaming any medias to DLNA
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
import java.sql.Statement;
import java.sql.Types;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;
import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Audiotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableAudiotracks extends MediaTable {

	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableAudiotracks.class);
	public static final String TABLE_NAME = "AUDIOTRACKS";
	private static final String MBID_RECORD = "MBID_RECORD";
	private static final String MBID_TRACK = "MBID_TRACK";
	private static final String DISC = "DISC";
	private static final int SIZE_LANG = 3;
	private static final int SIZE_GENRE = 64;
	private static final int SIZE_MUXINGMODE = 32;
	private static final int SIZE_SAMPLEFREQ = 16;
	private static final int SIZE_CODECA = 32;

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 4;

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
				Integer version = MediaTableTablesVersions.getTableVersion(connection, TABLE_NAME);
				if (version == null) {
					version = 1;
				}
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn("Database table \"" + TABLE_NAME + "\" is from a newer version of UMS.");
				}
			} else {
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		TABLE_LOCK.writeLock().lock();
		try {
			for (int version = currentVersion; version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
				switch (version) {
					case 1:
						if (!isColumnExist(connection, TABLE_NAME, MBID_RECORD)) {
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + MBID_RECORD + " UUID");
						}
						if (!isColumnExist(connection, TABLE_NAME, MBID_TRACK)) {
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + MBID_TRACK + " UUID");
						}
						break;
					case 2:
						if (!isColumnExist(connection, TABLE_NAME, DISC)) {
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + DISC + " INT");
						}
						break;
					case 3:
						if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
							LOGGER.trace("Deleting index IDXYEAR");
							executeUpdate(connection, "DROP INDEX IF EXISTS IDXYEAR");
							LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
							LOGGER.trace("Creating index IDX_AUDIO_YEAR");
							executeUpdate(connection, "CREATE INDEX IDX_AUDIO_YEAR on AUDIOTRACKS (MEDIA_YEAR asc);");
						}
						break;
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "\" is missing table upgrade commands from version " +
							version + " to " + TABLE_VERSION
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
			sb.append(", MEDIA_YEAR        INT");
			sb.append(", TRACK             INT");
			sb.append(", DISC              INT");
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

			LOGGER.trace("Creating index IDX_AUDIO_YEAR");
			executeUpdate(connection, "CREATE INDEX IDX_AUDIO_YEAR on AUDIOTRACKS (MEDIA_YEAR asc);");
		}
	}

	protected static void insertOrUpdateAudioTracks(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getAudioTrackCount() < 1) {
			return;
		}

		String columns = "FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, BITSPERSAMPLE, " +
			"ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, MEDIA_YEAR, TRACK, DELAY, MUXINGMODE, BITRATE, MBID_RECORD, MBID_TRACK, DISC";

		TABLE_LOCK.writeLock().lock();
		try (
			PreparedStatement updateStatment = connection.prepareStatement("SELECT " +
					"FILEID, ID, MBID_RECORD, MBID_TRACK, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, " +
					"BITSPERSAMPLE, ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, MEDIA_YEAR, TRACK, DISC, " +
					"DELAY, MUXINGMODE, BITRATE " +
				"FROM " + MediaTableAudiotracks.TABLE_NAME + " " +
				"WHERE " +
					"FILEID = ? AND ID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + MediaTableAudiotracks.TABLE_NAME + " (" + columns + ")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (DLNAMediaAudio audioTrack : media.getAudioTracksList()) {
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, audioTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						//make sure mbid are uuids
						try {
							UUID mbidRecord = UUID.fromString(trimToEmpty(audioTrack.getMbidRecord()));
							rs.updateObject("MBID_RECORD", mbidRecord);
						} catch (IllegalArgumentException e) {
							LOGGER.trace("UUID not well formated, store null value");
							rs.updateNull("MBID_RECORD");
						}
						try {
							UUID mbidTrack = UUID.fromString(trimToEmpty(audioTrack.getMbidTrack()));
							rs.updateObject("MBID_TRACK", mbidTrack);
						} catch (IllegalArgumentException e) {
							LOGGER.trace("UUID not well formated, store null value");
							rs.updateNull("MBID_TRACK");
						}
						rs.updateString("LANG", left(audioTrack.getLang(), SIZE_LANG));
						rs.updateString("TITLE", left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_MAX));
						rs.updateInt("NRAUDIOCHANNELS", audioTrack.getAudioProperties().getNumberOfChannels());
						rs.updateString("SAMPLEFREQ", left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						rs.updateString("CODECA", left(audioTrack.getCodecA(), SIZE_CODECA));
						rs.updateInt("BITSPERSAMPLE", audioTrack.getBitsperSample());
						rs.updateString("ALBUM", left(trimToEmpty(audioTrack.getAlbum()), SIZE_MAX));
						rs.updateString("ARTIST", left(trimToEmpty(audioTrack.getArtist()), SIZE_MAX));

						//Special case for album artist. If it's empty, we want to insert NULL (for quicker retrieval)
						String albumartist = left(trimToEmpty(audioTrack.getAlbumArtist()), SIZE_MAX);
						if (albumartist.isEmpty()) {
							rs.updateNull("ALBUMARTIST");
						} else {
							rs.updateString("ALBUMARTIST", albumartist);
						}

						rs.updateString("SONGNAME", left(trimToEmpty(audioTrack.getSongname()), SIZE_MAX));
						rs.updateString("GENRE", left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
						rs.updateInt("MEDIA_YEAR", audioTrack.getYear());
						rs.updateInt("TRACK", audioTrack.getTrack());
						rs.updateInt("DISC", audioTrack.getDisc());
						rs.updateInt("DELAY", audioTrack.getAudioProperties().getAudioDelay());
						rs.updateString("MUXINGMODE", left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
						rs.updateInt("BITRATE", audioTrack.getBitRate());
						rs.updateRow();
					} else {
						insertStatement.clearParameters();
						insertStatement.setLong(1, fileId);
						insertStatement.setInt(2, audioTrack.getId());
						insertStatement.setString(3, left(audioTrack.getLang(), SIZE_LANG));
						insertStatement.setString(4, left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_MAX));
						insertStatement.setInt(5, audioTrack.getAudioProperties().getNumberOfChannels());
						insertStatement.setString(6, left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
						insertStatement.setString(7, left(audioTrack.getCodecA(), SIZE_CODECA));
						insertStatement.setInt(8, audioTrack.getBitsperSample());
						insertStatement.setString(9, left(trimToEmpty(audioTrack.getAlbum()), SIZE_MAX));
						insertStatement.setString(10, left(trimToEmpty(audioTrack.getArtist()), SIZE_MAX));

						//Special case for album artist. If it's empty, we want to insert NULL (for quicker retrieval)
						String albumartist = left(trimToEmpty(audioTrack.getAlbumArtist()), SIZE_MAX);
						if (albumartist.isEmpty()) {
							insertStatement.setNull(11, Types.VARCHAR);
						} else {
							insertStatement.setString(11, albumartist);
						}

						insertStatement.setString(12, left(trimToEmpty(audioTrack.getSongname()), SIZE_MAX));
						insertStatement.setString(13, left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
						insertStatement.setInt(14, audioTrack.getYear());
						insertStatement.setInt(15, audioTrack.getTrack());
						insertStatement.setInt(16, audioTrack.getAudioProperties().getAudioDelay());
						insertStatement.setString(17, left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
						insertStatement.setInt(18, audioTrack.getBitRate());
						try {
							UUID mbidRecord = UUID.fromString(trimToEmpty(audioTrack.getMbidRecord()));
							insertStatement.setObject(19, mbidRecord);
						} catch (IllegalArgumentException e) {
							LOGGER.trace("UUID not well formated, store null value");
							insertStatement.setNull(19, Types.OTHER);
						}
						try {
							UUID mbidTrack = UUID.fromString(trimToEmpty(audioTrack.getMbidTrack()));
							insertStatement.setObject(20, mbidTrack);
						} catch (IllegalArgumentException e) {
							LOGGER.trace("UUID not well formated, store null value");
							insertStatement.setNull(20, Types.OTHER);
						}
						insertStatement.setInt(21, audioTrack.getDisc());
						insertStatement.executeUpdate();
					}
				}
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}
}
