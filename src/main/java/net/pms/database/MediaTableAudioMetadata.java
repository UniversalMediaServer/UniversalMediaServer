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
import java.util.UUID;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Audio Metadatas table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableAudioMetadata extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableAudioMetadata.class);
	public static final String TABLE_NAME = "AUDIO_METADATA";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	public static final String COL_FILEID = "FILEID";
	private static final String COL_DISC = "DISC";
	public static final String COL_COMPOSER = "COMPOSER";
	public static final String COL_CONDUCTOR = "CONDUCTOR";
	private static final String COL_ALBUM = "ALBUM";
	private static final String COL_ARTIST = "ARTIST";
	private static final String COL_ALBUMARTIST = "ALBUMARTIST";
	private static final String COL_GENRE = "GENRE";
	private static final String COL_MEDIA_YEAR = "MEDIA_YEAR";
	private static final String COL_TRACK = "TRACK";
	private static final String COL_SONGNAME = "SONGNAME";
	private static final String COL_MBID_RECORD = "MBID_RECORD";
	private static final String COL_MBID_TRACK = "MBID_TRACK";
	private static final String COL_AUDIOTRACK_ID = "AUDIOTRACK_ID";
	//this is a user param / rating as well when it's changed by the user
	private static final String COL_RATING = "RATING";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_MBID_RECORD = TABLE_NAME + "." + COL_MBID_RECORD;
	public static final String TABLE_COL_MBID_TRACK = TABLE_NAME + "." + COL_MBID_TRACK;
	public static final String TABLE_COL_MEDIA_YEAR = TABLE_NAME + "." + COL_MEDIA_YEAR;
	public static final String TABLE_COL_GENRE = TABLE_NAME + "." + COL_GENRE;
	public static final String TABLE_COL_ALBUM = TABLE_NAME + "." + COL_ALBUM;
	public static final String TABLE_COL_TRACK = TABLE_NAME + "." + COL_TRACK;
	public static final String TABLE_COL_ALBUMARTIST = TABLE_NAME + "." + COL_ALBUMARTIST;
	public static final String TABLE_COL_ARTIST = TABLE_NAME + "." + COL_ARTIST;
	public static final String TABLE_COL_COMPOSER = TABLE_NAME + "." + COL_COMPOSER;
	public static final String TABLE_COL_CONDUCTOR = TABLE_NAME + "." + COL_CONDUCTOR;

	/**
	 * SQL References
	 */
	public static final String REFERENCE_TABLE_COL_FILE_ID = TABLE_NAME + "(" + COL_FILEID + ")";

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_AUDIO_METADATA_BY_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + COL_FILEID + EQUAL + PARAMETER + LIMIT_1;

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
				COL_FILEID              + INTEGER                        + PRIMARY_KEY       + COMMA +
				COL_AUDIOTRACK_ID       + INTEGER                        + AUTO_INCREMENT    + COMMA +
				COL_ALBUM               + VARCHAR_SIZE_MAX                                   + COMMA +
				COL_ARTIST              + VARCHAR_SIZE_MAX                                   + COMMA +
				COL_ALBUMARTIST         + VARCHAR_SIZE_MAX                                   + COMMA +
				COL_SONGNAME            + VARCHAR_SIZE_MAX                                   + COMMA +
				COL_GENRE               + VARCHAR_SIZE_MAX                                   + COMMA +
				COL_MEDIA_YEAR          + INTEGER                                            + COMMA +
				COL_MBID_RECORD         + UUID_TYPE                                          + COMMA +
				COL_MBID_TRACK          + UUID_TYPE                                          + COMMA +
				COL_TRACK               + INTEGER                                            + COMMA +
				COL_DISC                + INTEGER                                            + COMMA +
				COL_RATING              + INTEGER                                            + COMMA +
				COL_COMPOSER            + VARCHAR_1024                                       + COMMA +
				COL_CONDUCTOR           + VARCHAR_1024                                       + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST + IDX_MARKER + ON + TABLE_NAME + " (" + COL_ARTIST + ASC + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ALBUMARTIST + IDX_MARKER + ON + TABLE_NAME + " (" + COL_ALBUMARTIST + ASC + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ALBUM + IDX_MARKER + ON + TABLE_NAME + " (" + COL_ALBUM + ASC + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + " (" + COL_GENRE + ASC + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_RATING + IDX_MARKER + ON + TABLE_NAME + " (" + COL_RATING + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_AUDIOTRACK_ID + IDX_MARKER + ON + TABLE_NAME + " (" + COL_AUDIOTRACK_ID + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_COMPOSER + IDX_MARKER + ON + TABLE_NAME + " (" + COL_COMPOSER + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_CONDUCTOR + IDX_MARKER + ON + TABLE_NAME + " (" + COL_CONDUCTOR + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MEDIA_YEAR + IDX_MARKER + ON + TABLE_NAME + " (" + COL_MEDIA_YEAR + ")"
		);
	}

	/**
	 * Updates an existing row with information from audioMetadata metadata.
	 *
	 * @param connection the db connection
	 * @param fileId the file id from FILES table.
	 * @param media the {@link MediaInfo} AudioMetadata row to update.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertOrUpdateAudioMetadata(final Connection connection, final Long fileId, final MediaInfo media) throws SQLException {
		if (connection == null || fileId == null || media == null || !media.hasAudioMetadata()) {
			return;
		}
		MediaAudioMetadata audioMetadata = media.getAudioMetadata();
		try (
			PreparedStatement updateStatement = connection.prepareStatement(SQL_GET_AUDIO_METADATA_BY_FILEID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			updateStatement.setLong(1, fileId);
			try (ResultSet rs = updateStatement.executeQuery()) {
				boolean isCreatingNewRecord = !rs.next();
				if (isCreatingNewRecord) {
					rs.moveToInsertRow();
					rs.updateLong(COL_FILEID, fileId);
				}
				updateAudioMetadata(rs, audioMetadata);
				if (isCreatingNewRecord) {
					rs.insertRow();
				} else {
					rs.updateRow();
				}
			}
		}
	}

	public static FilenameIdVO getFilenameIdForAudiotrackId(Connection connection, Integer audiotrackId) {
		if (audiotrackId == null) {
			throw new RuntimeException("audiotrackId shall not be empty.");
		}

		String sql = String.format(
			"Select f.id, filename from FILES as f left outer join %s as a on F.ID = A.FILEID where a.AUDIOTRACK_ID = ?",
			MediaTableAudioMetadata.TABLE_NAME);
		try (PreparedStatement ps = connection.prepareStatement(sql);) {
			ps.setInt(1, audiotrackId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return new FilenameIdVO(rs.getInt(1), rs.getString(2));
			} else {
				throw new RuntimeException("audiotrackId not found : " + audiotrackId);
			}
		} catch (SQLException e) {
			throw new RuntimeException("cannot handle request", e);
		}
	}

	public static void updateRatingByAudiotrackId(Connection connection, int ratingInStars, Integer audiotracksId) throws SQLException {
		String sql;
		sql = String.format("UPDATE %s set rating = ? where AUDIOTRACK_ID = ?", TABLE_NAME);
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, ratingInStars);
			if (audiotracksId == null) {
				ps.setNull(2, Types.INTEGER);
			} else {
				ps.setInt(2, audiotracksId);
			}
			ps.executeUpdate();
			connection.commit();
		}
	}

	public static void updateRatingByMusicbrainzId(Connection connection, int ratingInStars, String musicBrainzTrackId)
		throws SQLException {
		String sql;
		sql = String.format("UPDATE %s set rating = ? where MBID_TRACK = ?", TABLE_NAME);
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setInt(1, ratingInStars);
			ps.setString(2, musicBrainzTrackId);
			ps.executeUpdate();
			connection.commit();
		}
	}

	public static MediaAudioMetadata getAudioMetadataByFileId(final Connection connection, final long fileId) {
		if (connection == null || fileId < 0) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_AUDIO_METADATA_BY_FILEID)) {
				selectStatement.setLong(1, fileId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						return resultSetToAudioMetadata(rs);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	private static void updateAudioMetadata(ResultSet result, MediaAudioMetadata audioMetadata) throws SQLException {
		//make sure mbid are uuids
		if (StringUtils.isEmpty(audioMetadata.getMbidRecord())) {
			result.updateNull(COL_MBID_RECORD);
		} else {
			try {
				UUID mbidRecord = UUID.fromString(StringUtils.trimToEmpty(audioMetadata.getMbidRecord()));
				result.updateObject(COL_MBID_RECORD, mbidRecord);
			} catch (IllegalArgumentException e) {
				LOGGER.trace("UUID {} not well formatted, store null value", audioMetadata.getMbidRecord());
				result.updateNull(COL_MBID_RECORD);
			}
		}
		if (StringUtils.isEmpty(audioMetadata.getMbidTrack())) {
			result.updateNull(COL_MBID_TRACK);
		} else {
			try {
				UUID mbidTrack = UUID.fromString(StringUtils.trimToEmpty(audioMetadata.getMbidTrack()));
				result.updateObject(COL_MBID_TRACK, mbidTrack);
			} catch (IllegalArgumentException e) {
				LOGGER.trace("UUID {} not well formatted, store null value", audioMetadata.getMbidTrack());
				result.updateNull(COL_MBID_TRACK);
			}
		}
		updateString(result, COL_ALBUM, audioMetadata.getAlbum(), SIZE_MAX);
		updateString(result, COL_ARTIST, audioMetadata.getArtist(), SIZE_MAX);
		updateString(result, COL_COMPOSER, audioMetadata.getComposer(), SIZE_1024);
		updateString(result, COL_CONDUCTOR, audioMetadata.getConductor(), SIZE_1024);
		updateString(result, COL_ALBUMARTIST, audioMetadata.getAlbumArtist(), SIZE_MAX);
		updateString(result, COL_SONGNAME, audioMetadata.getSongname(), SIZE_MAX);
		updateString(result, COL_GENRE, audioMetadata.getGenre(), SIZE_MAX);
		result.updateInt(COL_MEDIA_YEAR, audioMetadata.getYear());
		result.updateInt(COL_TRACK, audioMetadata.getTrack());
		result.updateInt(COL_DISC, audioMetadata.getDisc());
		updateInteger(result, COL_RATING, audioMetadata.getRating());
	}

	private static MediaAudioMetadata resultSetToAudioMetadata(ResultSet resultset) throws SQLException {
		MediaAudioMetadata audioMetadata = new MediaAudioMetadata();
		audioMetadata.setAlbum(resultset.getString(COL_ALBUM));
		audioMetadata.setArtist(resultset.getString(COL_ARTIST));
		audioMetadata.setAlbumArtist(resultset.getString(COL_ALBUMARTIST));
		audioMetadata.setSongname(resultset.getString(COL_SONGNAME));
		audioMetadata.setGenre(resultset.getString(COL_GENRE));
		audioMetadata.setYear(resultset.getInt(COL_MEDIA_YEAR));
		audioMetadata.setTrack(resultset.getInt(COL_TRACK));
		audioMetadata.setDisc(resultset.getInt(COL_DISC));
		audioMetadata.setRating(resultset.getInt(COL_RATING));
		audioMetadata.setAudiotrackId(resultset.getInt(COL_AUDIOTRACK_ID));
		audioMetadata.setMbidRecord(resultset.getString(COL_MBID_RECORD));
		audioMetadata.setMbidTrack(resultset.getString(COL_MBID_TRACK));
		audioMetadata.setComposer(resultset.getString(COL_COMPOSER));
		audioMetadata.setConductor(resultset.getString(COL_CONDUCTOR));
		return audioMetadata;
	}

}
