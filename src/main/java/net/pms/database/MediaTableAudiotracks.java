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
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import org.apache.commons.lang3.StringUtils;
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
	private static final int TABLE_VERSION = 13;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	public static final String COL_FILEID = MediaTableFiles.CHILD_ID;
	private static final String COL_LANG = "LANG";
	private static final String COL_STREAMID = "STREAMID";
	private static final String COL_DEFAULT_FLAG = "DEFAULT_FLAG";
	private static final String COL_FORCED_FLAG = "FORCED_FLAG";
	private static final String COL_OPTIONALID = "OPTIONALID";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_CODEC = "CODEC";
	private static final String COL_VIDEODELAY = "VIDEODELAY";
	private static final String COL_MUXINGMODE = "MUXINGMODE";
	private static final String COL_BITRATE = "BITRATE";
	private static final String COL_SAMPLERATE = "SAMPLERATE";
	private static final String COL_BITDEPTH = "BITDEPTH";
	private static final String COL_NRAUDIOCHANNELS = "NRAUDIOCHANNELS";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_FILEID_ID = SQL_GET_ALL_FILEID + AND + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_FILEID_ID_GREATER_OR_EQUAL = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + TABLE_COL_ID + GREATER_OR_EQUAL_THAN + PARAMETER;

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
				case 1, 2 -> {
					//removed
				}
				case 3 -> {
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + "IDXYEAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "`YEAR`");
				}
				case 4, 5, 6, 7 -> {
					//removed
				}
				case 8 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + RENAME + CONSTRAINT + IF_EXISTS + "PKAUDIO TO " + TABLE_NAME + PK_MARKER);
				}
				case 9, 10 -> {
					//removed
				}
				case 11 -> {
					//remove indexes
					String[] indexes = {"IDX_MBID", "IDXALBUM", "IDXARTIST", "IDXALBUMARTIST", "IDXGENRE", "IDXRATING", "IDX_LIKE_SONG",
						"IDX_AUDIOTRACK_ID", "IDX_COMPOSER", "IDX_CONDUCTOR", "IDX_AUDIO_YEAR"
					};
					for (String index : indexes)  {
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + index);
					}
					//set data type to int
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_NRAUDIOCHANNELS + SET  + "DATA TYPE" + INTEGER);
				}
				case 12 -> {
					//set audio metadata to the MediaTableAudioMetadata table
					if (isColumnExist(connection, TABLE_NAME, "CONDUCTOR")) {
						executeUpdate(connection,
							INSERT_INTO + MediaTableAudioMetadata.TABLE_NAME +
								" (FILEID, ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, MEDIA_YEAR, MBID_RECORD," +
								"MBID_TRACK, TRACK, DISC, RATING, COMPOSER, CONDUCTOR) " +
							SELECT + "FILEID, ALBUM, ARTIST, ALBUMARTIST, SONGNAME, GENRE, MEDIA_YEAR, MBID_RECORD," +
								"MBID_TRACK, TRACK, DISC, RATING, COMPOSER, CONDUCTOR" +
							FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL_0
						);
						executeUpdate(connection,
							DELETE_FROM + MediaTableAudioMetadata.TABLE_NAME + WHERE +
								"ALBUM" + EQUAL + EMPTY_STRING + AND +
								"ARTIST" + EQUAL + EMPTY_STRING + AND +
								"SONGNAME" + EQUAL + EMPTY_STRING + AND +
								"GENRE" + EQUAL + EMPTY_STRING + AND +
								"COMPOSER" + EQUAL + EMPTY_STRING + AND +
								"CONDUCTOR" + EQUAL + EMPTY_STRING + AND +
								"MEDIA_YEAR" + EQUAL_0 + AND +
								"TRACK" + EQUAL_0 + AND +
								"MBID_RECORD" + IS_NULL + AND +
								"MBID_TRACK" + IS_NULL
						);
					}
					String[] columns = {"ALBUM", "ARTIST", "ALBUMARTIST", "SONGNAME", "GENRE", "MEDIA_YEAR", "MBID_RECORD",
						"MBID_TRACK", "TRACK", "DISC", "LIKE_SONG", "LIKESONG", "RATING", "COMPOSER", "CONDUCTOR"
					};
					//delete old columns
					for (String column : columns)  {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + column);
					}
					//delete old index
					for (String column : columns)  {
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + column + IDX_MARKER);
					}
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "DELAY" + RENAME_TO + COL_VIDEODELAY);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "CODECA" + RENAME_TO + COL_CODEC);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "BITSPERSAMPLE" + RENAME_TO + COL_BITDEPTH);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_SAMPLERATE + INTEGER);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_STREAMID + INTEGER);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_OPTIONALID + BIGINT);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_DEFAULT_FLAG + BOOLEAN + DEFAULT + FALSE);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_FORCED_FLAG + BOOLEAN + DEFAULT + FALSE);
					//put back data from SAMPLEFREQ if exists
					if (isColumnExist(connection, TABLE_NAME, "SAMPLEFREQ")) {
						executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_SAMPLERATE + EQUAL + "CAST(SAMPLEFREQ AS INT)");
					}
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "SAMPLEFREQ");
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
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_ID                  + INTEGER                         + NOT_NULL         + COMMA +
				COL_FILEID              + BIGINT                          + NOT_NULL         + COMMA +
				COL_LANG                + VARCHAR + "(" + SIZE_LANG + ")"                    + COMMA +
				COL_STREAMID            + INTEGER                                            + COMMA +
				COL_OPTIONALID          + BIGINT                                             + COMMA +
				COL_DEFAULT_FLAG        + BOOLEAN                         + DEFAULT + FALSE  + COMMA +
				COL_FORCED_FLAG         + BOOLEAN                         + DEFAULT + FALSE  + COMMA +
				COL_TITLE               + VARCHAR + "(" + SIZE_MAX + ")"                     + COMMA +
				COL_NRAUDIOCHANNELS     + INTEGER                                            + COMMA +
				COL_SAMPLERATE          + INTEGER                                            + COMMA +
				COL_CODEC               + VARCHAR_32                                         + COMMA +
				COL_BITDEPTH            + INTEGER                                            + COMMA +
				COL_VIDEODELAY          + INTEGER                                            + COMMA +
				COL_MUXINGMODE          + VARCHAR_32                                         + COMMA +
				COL_BITRATE             + INTEGER                                            + COMMA +
				CONSTRAINT + TABLE_NAME + PK_MARKER + PRIMARY_KEY + "(" + COL_FILEID + COMMA + COL_ID + COMMA + COL_LANG + ")" + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")"
		);
	}

	protected static void insertOrUpdateAudioTracks(Connection connection, long fileId, MediaInfo media) throws SQLException {
		if (connection == null || fileId < 0 || media == null) {
			return;
		}

		int trackCount = media.getAudioTrackCount();
		try (
			PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_BY_FILEID_ID_GREATER_OR_EQUAL);
		) {
			updateStatment.setLong(1, fileId);
			updateStatment.setInt(2, trackCount);
			updateStatment.executeUpdate();
		}

		if (trackCount == 0) {
			return;
		}

		try (
			PreparedStatement updateStatment = connection.prepareStatement(SQL_GET_ALL_FILEID_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			for (MediaAudio audioTrack : media.getAudioTracks()) {
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
					updateAudioTrack(result, audioTrack);
					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				}
			}
		}
	}

	private static void updateAudioTrack(ResultSet result, MediaAudio audioTrack) throws SQLException {
		updateInteger(result, COL_STREAMID, audioTrack.getStreamOrder());
		result.updateBoolean(COL_DEFAULT_FLAG, audioTrack.isDefault());
		result.updateBoolean(COL_FORCED_FLAG, audioTrack.isForced());
		if (audioTrack.getLang() == null) {
			result.updateString(COL_LANG, "");
		} else {
			updateString(result, COL_LANG, audioTrack.getLang(), SIZE_LANG);
		}
		result.updateString(COL_TITLE, StringUtils.left(audioTrack.getTitle(), SIZE_MAX));
		result.updateString(COL_CODEC, StringUtils.left(audioTrack.getCodec(), 32));
		updateLong(result, COL_OPTIONALID, audioTrack.getOptionalId());
		result.updateString(COL_MUXINGMODE, StringUtils.left(StringUtils.trimToEmpty(audioTrack.getMuxingMode()), 32));
		result.updateInt(COL_SAMPLERATE, audioTrack.getSampleRate());
		result.updateInt(COL_BITDEPTH, audioTrack.getBitDepth());
		result.updateInt(COL_NRAUDIOCHANNELS, audioTrack.getNumberOfChannels());
		result.updateInt(COL_VIDEODELAY, audioTrack.getVideoDelay());
		result.updateInt(COL_BITRATE, audioTrack.getBitRate());
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
					MediaAudio audio = getAudioTrack(elements);
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

	private static MediaAudio getAudioTrack(ResultSet resultset) throws SQLException {
		MediaAudio audio = new MediaAudio();
		audio.setId(resultset.getInt(COL_ID));
		audio.setLang(resultset.getString(COL_LANG));
		audio.setStreamOrder(toInteger(resultset, COL_STREAMID));
		audio.setDefault(resultset.getBoolean(COL_DEFAULT_FLAG));
		audio.setForced(resultset.getBoolean(COL_FORCED_FLAG));
		audio.setOptionalId(toLong(resultset, COL_OPTIONALID));
		audio.setTitle(resultset.getString(COL_TITLE));
		audio.setNumberOfChannels(resultset.getInt(COL_NRAUDIOCHANNELS));
		audio.setSampleRate(resultset.getInt(COL_SAMPLERATE));
		audio.setCodec(resultset.getString(COL_CODEC));
		audio.setBitDepth(resultset.getInt(COL_BITDEPTH));
		audio.setVideoDelay(resultset.getInt(COL_VIDEODELAY));
		audio.setMuxingMode(resultset.getString(COL_MUXINGMODE));
		audio.setBitRate(resultset.getInt(COL_BITRATE));
		return audio;
	}

}
