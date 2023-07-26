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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.pms.media.audio.MediaAudio;
import net.pms.media.MediaInfo;
import org.apache.commons.lang.StringUtils;
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
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableAudiotracks.class);
	public static final String TABLE_NAME = "AUDIOTRACKS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 12;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	public static final String COL_FILEID = "FILEID";
	private static final String COL_MBID_RECORD = "MBID_RECORD";
	private static final String COL_MBID_TRACK = "MBID_TRACK";
	private static final String COL_LIKE_SONG = "LIKESONG";
	private static final String COL_DISC = "DISC";
	public static final String COL_COMPOSER = "COMPOSER";
	public static final String COL_CONDUCTOR = "CONDUCTOR";
	private static final String COL_ALBUM = "ALBUM";
	private static final String COL_ARTIST = "ARTIST";
	private static final String COL_ALBUMARTIST = "ALBUMARTIST";
	private static final String COL_GENRE = "GENRE";
	private static final String COL_MEDIA_YEAR = "MEDIA_YEAR";
	private static final String COL_TRACK = "TRACK";
	private static final String COL_LANG = "LANG";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_NRAUDIOCHANNELS = "NRAUDIOCHANNELS";
	private static final String COL_SAMPLEFREQ = "SAMPLEFREQ";
	private static final String COL_CODECA = "CODECA";
	private static final String COL_BITSPERSAMPLE = "BITSPERSAMPLE";
	private static final String COL_SONGNAME = "SONGNAME";
	private static final String COL_DELAY = "DELAY";
	private static final String COL_MUXINGMODE = "MUXINGMODE";
	private static final String COL_BITRATE = "BITRATE";
	private static final String COL_RATING = "RATING";
	private static final String COL_AUDIOTRACK_ID = "AUDIOTRACK_ID";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_ALBUM = TABLE_NAME + "." + COL_ALBUM;
	public static final String TABLE_COL_ALBUMARTIST = TABLE_NAME + "." + COL_ALBUMARTIST;
	public static final String TABLE_COL_ARTIST = TABLE_NAME + "." + COL_ARTIST;
	public static final String TABLE_COL_COMPOSER = TABLE_NAME + "." + COL_COMPOSER;
	public static final String TABLE_COL_CONDUCTOR = TABLE_NAME + "." + COL_CONDUCTOR;
	public static final String TABLE_COL_GENRE = TABLE_NAME + "." + COL_GENRE;
	public static final String TABLE_COL_MBID_RECORD = TABLE_NAME + "." + COL_MBID_RECORD;
	public static final String TABLE_COL_MBID_TRACK = TABLE_NAME + "." + COL_MBID_TRACK;
	public static final String TABLE_COL_MEDIA_YEAR = TABLE_NAME + "." + COL_MEDIA_YEAR;
	public static final String TABLE_COL_TRACK = TABLE_NAME + "." + COL_TRACK;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_FILEID_ID = SQL_GET_ALL_FILEID + AND + TABLE_COL_ID + EQUAL + PARAMETER;

	/**
	 * Database column sizes
	 */
	private static final int SIZE_GENRE = 64;
	private static final int SIZE_COMPOSER = 1024;
	private static final int SIZE_MUXINGMODE = 32;
	private static final int SIZE_SAMPLEFREQ = 16;
	private static final int SIZE_CODECA = 32;

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
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_MBID_RECORD + " UUID");
					}
					if (!isColumnExist(connection, TABLE_NAME, COL_MBID_TRACK)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_MBID_TRACK + " UUID");
					}
				}
				case 2 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_DISC)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_DISC + INTEGER);
					}
				}
				case 3 -> {
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Deleting index IDXYEAR");
						executeUpdate(connection, "DROP INDEX IF EXISTS IDXYEAR");
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`YEAR`" + RENAME_TO + COL_MEDIA_YEAR);
						LOGGER.trace("Creating index on " + COL_MEDIA_YEAR);
						executeUpdate(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MEDIA_YEAR + IDX_MARKER + ON + TABLE_NAME + " (" + COL_MEDIA_YEAR + ASC + ")");
					}
				}
				case 4 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_LIKE_SONG)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_LIKE_SONG + BOOLEAN);
						LOGGER.trace("Adding " + COL_LIKE_SONG + " to table " + TABLE_NAME);
						executeUpdate(connection, CREATE_INDEX + "IDX_LIKE_SONG" + ON + TABLE_NAME + " (" + COL_LIKE_SONG + ");");
						LOGGER.trace("Indexing column " + COL_LIKE_SONG + " on table " + TABLE_NAME);
					}
				}
				case 5 -> {
					executeUpdate(connection, CREATE_INDEX + "IDX_MBID ON " + TABLE_NAME + " (MBID_TRACK);");
					LOGGER.trace("Indexing column MBID_TRACK on table " + TABLE_NAME);
				}
				case 6 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_RATING)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_RATING + INTEGER);
						LOGGER.trace("added column RATING on table " + TABLE_NAME);
						executeUpdate(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_RATING + IDX_MARKER + ON + TABLE_NAME + "(" + COL_RATING + ")");
						LOGGER.trace("Indexing column RATING on table " + TABLE_NAME);
					}
				}
				case 7 -> {
					connection.setAutoCommit(false);
					try (
						Statement stmt =  DATABASE.getConnection().createStatement()) {
						stmt.execute(ALTER_TABLE + TABLE_NAME + ADD + COLUMN + COL_AUDIOTRACK_ID + INTEGER + AUTO_INCREMENT);
						stmt.execute(UPDATE + TABLE_NAME + SET + COL_AUDIOTRACK_ID + EQUAL + "ROWNUM()");
						stmt.execute("SET @mv = SELECT MAX(AUDIOTRACK_ID)" + FROM + TABLE_NAME + " + 1");
						stmt.execute(ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + COL_AUDIOTRACK_ID + " RESTART WITH @mv");
						connection.commit();
						connection.setAutoCommit(true);
					} catch (Exception e) {
						LOGGER.warn("Upgrade failed", e);
					}
				}
				case 8 -> {
					try {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + RENAME + CONSTRAINT + "PKAUDIO TO " + TABLE_NAME + PK_MARKER);
					} catch (SQLException e) {
						//PKAUDIO not found, nothing to update.
					}
				}
				case 9 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_COMPOSER)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_COMPOSER + VARCHAR + "(" + SIZE_COMPOSER + ")");
						LOGGER.trace("Adding " + COL_COMPOSER + " to table " + TABLE_NAME);
						executeUpdate(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_COMPOSER + IDX_MARKER + ON + TABLE_NAME + " (" + COL_COMPOSER + ");");
						LOGGER.trace("Indexing column " + COL_COMPOSER + " on table " + TABLE_NAME);
					}
				}
				case 10 -> {
					if (!isColumnExist(connection, TABLE_NAME, COL_CONDUCTOR)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_CONDUCTOR + VARCHAR + "(" + SIZE_COMPOSER + ")");
						LOGGER.trace("Adding " + COL_CONDUCTOR + " to table " + TABLE_NAME);
						executeUpdate(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_CONDUCTOR + IDX_MARKER + ON + TABLE_NAME + " (" + COL_CONDUCTOR + ");");
						LOGGER.trace("Indexing column " + COL_CONDUCTOR + " on table " + TABLE_NAME);
					}
				}
				case 11 -> {
					//rename indexes
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDXALBUM" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ALBUM + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDXALBUMARTIST" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ALBUMARTIST + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDXARTIST" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDXGENRE" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDXRATING" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_RATING + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_LIKE_SONG" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_LIKE_SONG + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_AUDIOTRACK_ID" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_AUDIOTRACK_ID + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_COMPOSER" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_COMPOSER + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_CONDUCTOR" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_CONDUCTOR + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_AUDIO_YEAR" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MEDIA_YEAR + IDX_MARKER);
					//set data type to int
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_NRAUDIOCHANNELS + SET  + "DATA TYPE" + INTEGER);
					//remove index
					executeUpdate(connection, "DROP INDEX IF EXISTS IDX_MBID");
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
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_AUDIOTRACK_ID       + INTEGER                           + AUTO_INCREMENT + COMMA +
				COL_ID                  + INTEGER                           + NOT_NULL       + COMMA +
				COL_FILEID              + BIGINT                            + NOT_NULL       + COMMA +
				COL_MBID_RECORD         + UUID_TYPE                                          + COMMA +
				COL_MBID_TRACK          + UUID_TYPE                                          + COMMA +
				COL_LANG                + VARCHAR + "(" + SIZE_LANG + ")"                    + COMMA +
				COL_TITLE               + VARCHAR + "(" + SIZE_MAX + ")"                     + COMMA +
				COL_NRAUDIOCHANNELS     + INTEGER                                            + COMMA +
				COL_SAMPLEFREQ          + VARCHAR + "(" + SIZE_SAMPLEFREQ + ")"              + COMMA +
				COL_CODECA              + VARCHAR + "(" + SIZE_CODECA + ")"                  + COMMA +
				COL_BITSPERSAMPLE       + INTEGER                                            + COMMA +
				COL_ALBUM               + VARCHAR + "(" + SIZE_MAX + ")"                     + COMMA +
				COL_ARTIST              + VARCHAR + "(" + SIZE_MAX + ")"                     + COMMA +
				COL_ALBUMARTIST         + VARCHAR + "(" + SIZE_MAX + ")"                     + COMMA +
				COL_SONGNAME            + VARCHAR + "(" + SIZE_MAX + ")"                     + COMMA +
				COL_GENRE               + VARCHAR + "(" + SIZE_GENRE + ")"                   + COMMA +
				COL_MEDIA_YEAR          + INTEGER                                            + COMMA +
				COL_TRACK               + INTEGER                                            + COMMA +
				COL_DISC                + INTEGER                                            + COMMA +
				COL_DELAY               + INTEGER                                            + COMMA +
				COL_MUXINGMODE          + VARCHAR + "(" + SIZE_MUXINGMODE + ")"              + COMMA +
				COL_BITRATE             + INTEGER                                            + COMMA +
				COL_LIKE_SONG           + BOOLEAN                                            + COMMA +
				COL_RATING              + INTEGER                                            + COMMA +
				COL_COMPOSER            + VARCHAR + "(" + SIZE_COMPOSER + ")"                + COMMA +
				COL_CONDUCTOR           + VARCHAR + "(" + SIZE_COMPOSER + ")"                + COMMA +
				CONSTRAINT + TABLE_NAME + PK_MARKER + PRIMARY_KEY + "(" + COL_FILEID + COMMA + COL_ID + COMMA + COL_LANG + ")" + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")"
		);

		LOGGER.trace("Creating index on " + COL_ARTIST);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST + IDX_MARKER + ON + TABLE_NAME + " (" + COL_ARTIST + ASC + ")");

		LOGGER.trace("Creating index on " + COL_ALBUMARTIST);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ALBUMARTIST + IDX_MARKER + ON + TABLE_NAME + " (" + COL_ALBUMARTIST + ASC + ")");

		LOGGER.trace("Creating index on " + COL_ALBUM);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ALBUM + IDX_MARKER + ON + TABLE_NAME + " (" + COL_ALBUM + ASC + ")");

		LOGGER.trace("Creating index on " + COL_GENRE);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + " (" + COL_GENRE + ASC + ")");

		LOGGER.trace("Creating index on " + COL_RATING);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_RATING + IDX_MARKER + ON + TABLE_NAME + " (" + COL_RATING + ")");

		LOGGER.trace("Creating index on " + COL_LIKE_SONG);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_LIKE_SONG + IDX_MARKER + ON + TABLE_NAME + " (" + COL_LIKE_SONG + ")");

		LOGGER.trace("Creating index on " + COL_AUDIOTRACK_ID);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_AUDIOTRACK_ID + IDX_MARKER + ON + TABLE_NAME + " (" + COL_AUDIOTRACK_ID + ")");

		LOGGER.trace("Creating index on " + COL_COMPOSER);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_COMPOSER + IDX_MARKER + ON + TABLE_NAME + " (" + COL_COMPOSER + ")");

		LOGGER.trace("Creating index on " + COL_CONDUCTOR);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_CONDUCTOR + IDX_MARKER + ON + TABLE_NAME + " (" + COL_CONDUCTOR + ")");

		LOGGER.trace("Creating index on " + COL_MEDIA_YEAR);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MEDIA_YEAR + IDX_MARKER + ON + TABLE_NAME + " (" + COL_MEDIA_YEAR + ")");
	}

	protected static void insertOrUpdateAudioTracks(Connection connection, long fileId, MediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null || media.getAudioTrackCount() < 1) {
			return;
		}

		try (
			PreparedStatement updateStatment = connection.prepareStatement(SQL_GET_ALL_FILEID_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			for (MediaAudio audioTrack : media.getAudioTracksList()) {
				updateStatment.clearParameters();
				updateStatment.setLong(1, fileId);
				updateStatment.setInt(2, audioTrack.getId());
				try (ResultSet result = updateStatment.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						result.moveToInsertRow();
						result.updateLong(COL_FILEID, fileId);
						result.updateInt(COL_ID, audioTrack.getId());
					}
					//make sure mbid are uuids
					if (StringUtils.isEmpty(audioTrack.getMbidRecord())) {
						result.updateNull(COL_MBID_RECORD);
					} else {
						try {
							UUID mbidRecord = UUID.fromString(trimToEmpty(audioTrack.getMbidRecord()));
							result.updateObject(COL_MBID_RECORD, mbidRecord);
						} catch (IllegalArgumentException e) {
							LOGGER.trace("UUID {} not well formatted, store null value", audioTrack.getMbidRecord());
							result.updateNull(COL_MBID_RECORD);
						}
					}
					if (StringUtils.isEmpty(audioTrack.getMbidTrack())) {
						result.updateNull(COL_MBID_TRACK);
					} else {
						try {
							UUID mbidTrack = UUID.fromString(trimToEmpty(audioTrack.getMbidTrack()));
							result.updateObject(COL_MBID_TRACK, mbidTrack);
						} catch (IllegalArgumentException e) {
							LOGGER.trace("UUID {} not well formatted, store null value", audioTrack.getMbidTrack());
							result.updateNull(COL_MBID_TRACK);
						}
					}
					result.updateString(COL_LANG, left(audioTrack.getLang(), SIZE_LANG));
					result.updateString(COL_TITLE, left(audioTrack.getAudioTrackTitleFromMetadata(), SIZE_MAX));
					result.updateInt(COL_NRAUDIOCHANNELS, audioTrack.getAudioProperties().getNumberOfChannels());
					result.updateString(COL_SAMPLEFREQ, left(audioTrack.getSampleFrequency(), SIZE_SAMPLEFREQ));
					result.updateString(COL_CODECA, left(audioTrack.getCodecA(), SIZE_CODECA));
					result.updateInt(COL_BITSPERSAMPLE, audioTrack.getBitsperSample());
					result.updateString(COL_ALBUM, left(trimToEmpty(audioTrack.getAlbum()), SIZE_MAX));
					result.updateString(COL_ARTIST, left(trimToEmpty(audioTrack.getArtist()), SIZE_MAX));
					if (audioTrack.getComposer() == null) {
						result.updateNull(COL_COMPOSER);
					} else {
						result.updateString(COL_COMPOSER, left(trimToEmpty(audioTrack.getComposer()), SIZE_COMPOSER));
					}
					if (audioTrack.getConductor() == null) {
						result.updateNull(COL_CONDUCTOR);
					} else {
						result.updateString(COL_CONDUCTOR, left(trimToEmpty(audioTrack.getConductor()), SIZE_COMPOSER));
					}

					//Special case for album artist. If it's empty, we want to insert NULL (for quicker retrieval)
					String albumartist = left(trimToEmpty(audioTrack.getAlbumArtist()), SIZE_MAX);
					if (albumartist.isEmpty()) {
						result.updateNull(COL_ALBUMARTIST);
					} else {
						result.updateString(COL_ALBUMARTIST, albumartist);
					}

					result.updateString(COL_SONGNAME, left(trimToEmpty(audioTrack.getSongname()), SIZE_MAX));
					result.updateString(COL_GENRE, left(trimToEmpty(audioTrack.getGenre()), SIZE_GENRE));
					result.updateInt(COL_MEDIA_YEAR, audioTrack.getYear());
					result.updateInt(COL_TRACK, audioTrack.getTrack());
					result.updateInt(COL_DISC, audioTrack.getDisc());
					result.updateInt(COL_DELAY, audioTrack.getAudioProperties().getAudioDelay());
					result.updateString(COL_MUXINGMODE, left(trimToEmpty(audioTrack.getMuxingModeAudio()), SIZE_MUXINGMODE));
					result.updateInt(COL_BITRATE, audioTrack.getBitRate());
					if (audioTrack.getRating() == null) {
						result.updateNull(COL_RATING);
					} else {
						result.updateInt(COL_RATING, audioTrack.getRating());
					}
					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				}
			}
		}
	}

	protected static List<MediaAudio> getAudioTracks(Connection connection, long fileId) {
		List<MediaAudio> result = new ArrayList<>();
		if (connection == null || fileId < 0) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					MediaAudio audio = new MediaAudio();
					audio.setId(elements.getInt(COL_ID));
					audio.setLang(elements.getString(COL_LANG));
					audio.setAudioTrackTitleFromMetadata(elements.getString(COL_TITLE));
					audio.getAudioProperties().setNumberOfChannels(elements.getInt(COL_NRAUDIOCHANNELS));
					audio.setSampleFrequency(elements.getString(COL_SAMPLEFREQ));
					audio.setCodecA(elements.getString(COL_CODECA));
					audio.setBitsperSample(elements.getInt(COL_BITSPERSAMPLE));
					audio.setAlbum(elements.getString(COL_ALBUM));
					audio.setArtist(elements.getString(COL_ARTIST));
					audio.setAlbumArtist(elements.getString(COL_ALBUMARTIST));
					audio.setSongname(elements.getString(COL_SONGNAME));
					audio.setGenre(elements.getString(COL_GENRE));
					audio.setYear(elements.getInt(COL_MEDIA_YEAR));
					audio.setTrack(elements.getInt(COL_TRACK));
					audio.setDisc(elements.getInt(COL_DISC));
					audio.getAudioProperties().setAudioDelay(elements.getInt(COL_DELAY));
					audio.setMuxingModeAudio(elements.getString(COL_MUXINGMODE));
					audio.setBitRate(elements.getInt(COL_BITRATE));
					audio.setRating(elements.getInt(COL_RATING));
					audio.setAudiotrackId(elements.getInt(COL_AUDIOTRACK_ID));
					audio.setMbidRecord(elements.getString(COL_MBID_RECORD));
					audio.setMbidTrack(elements.getString(COL_MBID_TRACK));
					audio.setComposer(elements.getString(COL_COMPOSER));
					audio.setConductor(elements.getString(COL_CONDUCTOR));
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
