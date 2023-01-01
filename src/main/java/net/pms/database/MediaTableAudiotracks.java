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

import static org.apache.commons.lang3.StringUtils.left;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAMediaAudio;
import net.pms.dlna.DLNAMediaInfo;

/**
 * This class is responsible for managing the Audiotracks releases table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableAudiotracks extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableAudiotracks.class);
	public static final String TABLE_NAME = "AUDIOTRACKS";
	/**
	 * COLUMNS NAMES
	 */
	public static final String COL_FILEID = "FILEID";
	private static final String COL_MBID_RECORD = "MBID_RECORD";
	private static final String COL_MBID_TRACK = "MBID_TRACK";
	private static final String COL_LIKE_SONG = "LIKESONG";
	private static final String COL_DISC = "DISC";
	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_ALBUM = TABLE_NAME + ".ALBUM";
	public static final String TABLE_COL_ALBUMARTIST = TABLE_NAME + ".ALBUMARTIST";
	public static final String TABLE_COL_ARTIST = TABLE_NAME + ".ARTIST";
	public static final String TABLE_COL_GENRE = TABLE_NAME + ".GENRE";
	public static final String TABLE_COL_MBID_RECORD = TABLE_NAME + "." + COL_MBID_RECORD;
	public static final String TABLE_COL_MBID_TRACK = TABLE_NAME + "." + COL_MBID_TRACK;
	public static final String TABLE_COL_MEDIA_YEAR = TABLE_NAME + ".MEDIA_YEAR";
	public static final String TABLE_COL_TRACK = TABLE_NAME + ".TRACK";

	private static final String SQL_GET_ALL_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ?";

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
	private static final int TABLE_VERSION = 9;

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
				LOGGER.warn(LOG_TABLE_NEWER_VERSION, DATABASE_NAME, TABLE_NAME);
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
					if (!isColumnExist(connection, TABLE_NAME, COL_MBID_RECORD)) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + COL_MBID_RECORD + " UUID");
					}
					if (!isColumnExist(connection, TABLE_NAME, COL_MBID_TRACK)) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + COL_MBID_TRACK + " UUID");
					}
				}
				case 2 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_DISC)) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + COL_DISC + " INT");
					}
				}
				case 3 -> {
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Deleting index IDXYEAR");
						executeUpdate(connection, "DROP INDEX IF EXISTS IDXYEAR");
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
						LOGGER.trace("Creating index IDX_AUDIO_YEAR");
						executeUpdate(connection, "CREATE INDEX IDX_AUDIO_YEAR on " + TABLE_NAME + " (MEDIA_YEAR asc);");
					}
				}
				case 4 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_LIKE_SONG)) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD " + COL_LIKE_SONG + " BOOLEAN");
						LOGGER.trace("Adding " + COL_LIKE_SONG + " to table " + TABLE_NAME);
						executeUpdate(connection, "CREATE INDEX IDX_LIKE_SONG ON " + TABLE_NAME + " (" + COL_LIKE_SONG + ");");
						LOGGER.trace("Indexing column " + COL_LIKE_SONG + " on table " + TABLE_NAME);
					}
				}
				case 5 -> {
					executeUpdate(connection, "CREATE INDEX IDX_MBID ON " + TABLE_NAME + " (MBID_TRACK);");
					LOGGER.trace("Indexing column MBID_TRACK on table " + TABLE_NAME);
				}
				case 6 -> {
					if (!isColumnExist(connection, TABLE_NAME, "RATING")) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD RATING INT");
						LOGGER.trace("added column RATING on table " + TABLE_NAME);
						executeUpdate(connection, "CREATE INDEX IDX_RATING ON " + TABLE_NAME + " (RATING);");
						LOGGER.trace("Indexing column RATING on table " + TABLE_NAME);
					}
				}
				case 7 -> {
					connection.setAutoCommit(false);
					try (
						Statement stmt =  DATABASE.getConnection().createStatement()) {
						stmt.execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN AUDIOTRACK_ID INTEGER AUTO_INCREMENT");
						stmt.execute("UPDATE " + TABLE_NAME + " SET AUDIOTRACK_ID = ROWNUM()");
						stmt.execute("SET @mv = SELECT MAX(AUDIOTRACK_ID) FROM " + TABLE_NAME + " + 1");
						stmt.execute("ALTER TABLE " + TABLE_NAME + " ALTER COLUMN AUDIOTRACK_ID RESTART WITH @mv");
						connection.commit();
						connection.setAutoCommit(true);
					} catch (Exception e) {
						LOGGER.warn("Upgrade failed", e);
					}
				}
				case 8 -> {
					try {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " RENAME CONSTRAINT PKAUDIO TO " + TABLE_NAME + "_PK");
					} catch (SQLException e) {
						//PKAUDIO not found, nothing to update.
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
		try (Statement statement = connection.createStatement()) {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " + TABLE_NAME + " (");
			sb.append("  ID                INTEGER          NOT NULL");
			sb.append(", FILEID            BIGINT           NOT NULL");
			sb.append(", MBID_RECORD       UUID");
			sb.append(", MBID_TRACK        UUID");
			sb.append(", LANG              VARCHAR(").append(SIZE_LANG).append(')');
			sb.append(", TITLE             VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", NRAUDIOCHANNELS   NUMERIC");
			sb.append(", SAMPLEFREQ        VARCHAR(").append(SIZE_SAMPLEFREQ).append(')');
			sb.append(", CODECA            VARCHAR(").append(SIZE_CODECA).append(')');
			sb.append(", BITSPERSAMPLE     INTEGER");
			sb.append(", ALBUM             VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", ARTIST            VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", ALBUMARTIST       VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", SONGNAME          VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", GENRE             VARCHAR(").append(SIZE_GENRE).append(')');
			sb.append(", MEDIA_YEAR        INTEGER");
			sb.append(", TRACK             INTEGER");
			sb.append(", DISC              INTEGER");
			sb.append(", DELAY             INTEGER");
			sb.append(", MUXINGMODE        VARCHAR(").append(SIZE_MUXINGMODE).append(')');
			sb.append(", BITRATE           INTEGER");
			sb.append(", LIKE_SONG         BOOLEAN");
			sb.append(", RATING            INTEGER");
			sb.append(", AUDIOTRACK_ID     INTEGER          AUTO_INCREMENT");
			sb.append(", CONSTRAINT " + TABLE_NAME + "_PK PRIMARY KEY (FILEID, ID)");
			sb.append(", CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY(" + COL_FILEID + ") REFERENCES " + MediaTableFiles.REFERENCE_TABLE_COL_ID + " ON DELETE CASCADE");
			sb.append(')');

			executeUpdate(statement, sb.toString());

			LOGGER.trace("Creating index IDXARTIST");
			executeUpdate(statement, "CREATE INDEX IDXARTIST on " + TABLE_NAME + " (ARTIST asc);");

			LOGGER.trace("Creating index IDXALBUMARTIST");
			executeUpdate(statement, "CREATE INDEX IDXALBUMARTIST on " + TABLE_NAME + " (ALBUMARTIST asc);");

			LOGGER.trace("Creating index IDXALBUM");
			executeUpdate(statement, "CREATE INDEX IDXALBUM on " + TABLE_NAME + " (ALBUM asc);");

			LOGGER.trace("Creating index IDXGENRE");
			executeUpdate(statement, "CREATE INDEX IDXGENRE on " + TABLE_NAME + " (GENRE asc);");

			LOGGER.trace("Creating index IDX_AUDIO_YEAR");
			executeUpdate(statement, "CREATE INDEX IDX_AUDIO_YEAR on " + TABLE_NAME + " (MEDIA_YEAR asc);");

			LOGGER.trace("Creating index IDX_RATING");
			statement.execute("CREATE INDEX IDX_RATING on " + TABLE_NAME + " (RATING)");

			LOGGER.trace("Creating index IDX_LIKE_SONG");
			statement.execute("CREATE INDEX IDX_LIKE_SONG on " + TABLE_NAME + " (LIKE_SONG)");

			LOGGER.trace("Creating index IDX_AUDIOTRACK_ID");
			statement.execute("CREATE INDEX IDX_AUDIOTRACK_ID on " + TABLE_NAME + " (AUDIOTRACK_ID)");
		}
	}

	protected static void insertOrUpdateAudioTracks(Connection connection, long fileId, DLNAMediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getAudioTrackCount() < 1) {
			return;
		}

		String columns = "FILEID, ID, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, BITSPERSAMPLE, " +
			"ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, MEDIA_YEAR, TRACK, DELAY, MUXINGMODE, BITRATE, MBID_RECORD, MBID_TRACK, DISC, RATING";

		try (
			PreparedStatement updateStatment = connection.prepareStatement(
				"SELECT " +
					"FILEID, ID, MBID_RECORD, MBID_TRACK, LANG, TITLE, NRAUDIOCHANNELS, SAMPLEFREQ, CODECA, " +
					"BITSPERSAMPLE, ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, MEDIA_YEAR, TRACK, DISC, " +
					"DELAY, MUXINGMODE, BITRATE, RATING " +
				"FROM " + TABLE_NAME + " " +
				"WHERE " +
					"FILEID = ? AND ID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO " + TABLE_NAME + " (" + columns + ")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			for (DLNAMediaAudio audioTrack : media.getAudioTracksList()) {
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, audioTrack.getId());
				try (ResultSet rs = updateStatment.executeQuery()) {
					if (rs.next()) {
						//make sure mbid are uuids
						if (StringUtils.isEmpty(audioTrack.getMbidRecord())) {
							rs.updateNull("MBID_RECORD");
						} else {
							try {
								UUID mbidRecord = UUID.fromString(trimToEmpty(audioTrack.getMbidRecord()));
								rs.updateObject("MBID_RECORD", mbidRecord);
							} catch (IllegalArgumentException e) {
								LOGGER.trace("UUID {} not well formatted, store null value", audioTrack.getMbidRecord());
								rs.updateNull("MBID_RECORD");
							}
						}
						if (StringUtils.isEmpty(audioTrack.getMbidTrack())) {
							rs.updateNull("MBID_TRACK");
						} else {
							try {
								UUID mbidTrack = UUID.fromString(trimToEmpty(audioTrack.getMbidTrack()));
								rs.updateObject("MBID_TRACK", mbidTrack);
							} catch (IllegalArgumentException e) {
								LOGGER.trace("UUID {} not well formatted, store null value", audioTrack.getMbidTrack());
								rs.updateNull("MBID_TRACK");
							}
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
						if (audioTrack.getRating() == null) {
							rs.updateNull("RATING");
						} else {
							rs.updateInt("RATING", audioTrack.getRating());
						}
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
						if (StringUtils.isEmpty(audioTrack.getMbidRecord())) {
							insertStatement.setNull(19, Types.OTHER);
						} else {
							try {
								UUID mbidRecord = UUID.fromString(trimToEmpty(audioTrack.getMbidRecord()));
								insertStatement.setObject(19, mbidRecord);
							} catch (IllegalArgumentException e) {
								LOGGER.trace("UUID not well formated, store null value");
								insertStatement.setNull(19, Types.OTHER);
							}
						}
						if (StringUtils.isEmpty(audioTrack.getMbidTrack())) {
							insertStatement.setNull(20, Types.OTHER);
						} else {
							try {
								UUID mbidTrack = UUID.fromString(trimToEmpty(audioTrack.getMbidTrack()));
								insertStatement.setObject(20, mbidTrack);
							} catch (IllegalArgumentException e) {
								LOGGER.trace("UUID not well formated, store null value");
								insertStatement.setNull(20, Types.OTHER);
							}
						}
						insertStatement.setInt(21, audioTrack.getDisc());
						if (audioTrack.getRating() == null) {
							insertStatement.setNull(22, Types.INTEGER);
						} else {
							insertStatement.setInt(22, audioTrack.getRating());
						}
						insertStatement.executeUpdate();
					}
				}
			}
		}
	}

	protected static List<DLNAMediaAudio> getAudioTracks(Connection connection, long fileId) {
		List<DLNAMediaAudio> result = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					DLNAMediaAudio audio = new DLNAMediaAudio();
					audio.setId(elements.getInt("ID"));
					audio.setLang(elements.getString("LANG"));
					audio.setAudioTrackTitleFromMetadata(elements.getString("TITLE"));
					audio.getAudioProperties().setNumberOfChannels(elements.getInt("NRAUDIOCHANNELS"));
					audio.setSampleFrequency(elements.getString("SAMPLEFREQ"));
					audio.setCodecA(elements.getString("CODECA"));
					audio.setBitsperSample(elements.getInt("BITSPERSAMPLE"));
					audio.setAlbum(elements.getString("ALBUM"));
					audio.setArtist(elements.getString("ARTIST"));
					audio.setAlbumArtist(elements.getString("ALBUMARTIST"));
					audio.setSongname(elements.getString("SONGNAME"));
					audio.setGenre(elements.getString("GENRE"));
					audio.setYear(elements.getInt("MEDIA_YEAR"));
					audio.setTrack(elements.getInt("TRACK"));
					audio.setDisc(elements.getInt("DISC"));
					audio.getAudioProperties().setAudioDelay(elements.getInt("DELAY"));
					audio.setMuxingModeAudio(elements.getString("MUXINGMODE"));
					audio.setBitRate(elements.getInt("BITRATE"));
					audio.setRating(elements.getInt("RATING"));
					audio.setAudiotrackId(elements.getInt("AUDIOTRACK_ID"));
					audio.setMbidRecord(elements.getString("MBID_RECORD"));
					audio.setMbidTrack(elements.getString("MBID_TRACK"));
					LOGGER.trace("Adding audio from the database: {}", audio.toString());
					result.add(audio);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}
}
