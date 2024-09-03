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
import java.sql.Timestamp;
import java.util.UUID;
import net.pms.external.musicbrainz.coverart.CoverArtArchiveUtil.CoverArtArchiveTagInfo;
import net.pms.media.audio.metadata.MusicBrainzAlbum;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the MusicBrainz releases table.
 *
 * It does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author Nadahar
 */
public final class MediaTableMusicBrainzReleases extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableMusicBrainzReleases.class);
	public static final String TABLE_NAME = "MUSIC_BRAINZ_RELEASES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 5;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_MBID = "MBID";
	private static final String COL_ARTIST = "ARTIST";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_MEDIA_YEAR = "MEDIA_YEAR";
	private static final String COL_GENRE = "GENRE";
	private static final String COL_ARTIST_ID = "ARTIST_ID";
	private static final String COL_RATING = "RATING";

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_MBID = SELECT_ALL + FROM + TABLE_NAME + WHERE + COL_MBID + EQUAL + PARAMETER;

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
			if (version != null) {
				if (version < TABLE_VERSION) {
					upgradeTable(connection, version);
				} else if (version > TABLE_VERSION) {
					LOGGER.warn(LOG_TABLE_NEWER_VERSION_DELETEDB, DATABASE_NAME, TABLE_NAME, DATABASE.getDatabaseFilename());
				}
			} else {
				LOGGER.warn(LOG_TABLE_UNKNOWN_VERSION_RECREATE, DATABASE_NAME, TABLE_NAME);
				dropTable(connection, TABLE_NAME);
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} else {
			createTable(connection);
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		}
	}

	/**
	 * This method <strong>MUST</strong> be updated if the table definition are
	 * altered. The changes for each version in the form of
	 * <code>ALTER TABLE</code> must be implemented here.
	 *
	 * @param connection the {@link Connection} to use
	 * @param currentVersion the version to upgrade <strong>from</strong>
	 *
	 * @throws SQLException
	 */
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					// Version 2 increases the size of ARTIST; ALBUM, TITLE and YEAR.
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_ARTIST + VARCHAR_1000);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_TITLE + VARCHAR_1000);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "`YEAR`" + VARCHAR_20);
				}
				case 2 -> {
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "`YEAR`" + RENAME_TO + COL_MEDIA_YEAR);
					}
				}
				case 3 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_MBID + UUID_TYPE);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_ARTIST_ID + UUID_TYPE);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_GENRE + VARCHAR_1000);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_RATING + INTEGER);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "TRACK_ID");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "ALBUM");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_MBID + ")");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_GENRE + ")");
				}
				case 4 -> {
					//fix: append the table name to the index name to avoid collisions with indexes of other tables
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + COL_ARTIST + IDX_MARKER);
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + COL_ARTIST_ID + IDX_MARKER);
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + COL_MBID + IDX_MARKER);
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + COL_GENRE + IDX_MARKER);
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ARTIST + ")");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST_ID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ARTIST_ID + ")");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_MBID + ")");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_GENRE + ")");
				}
				default -> {
					throw new IllegalStateException(
							getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
				}
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
				CREATE_TABLE + TABLE_NAME + "(" +
					COL_ID               + IDENTITY             + COMMA +
					COL_MODIFIED         + TIMESTAMP            + COMMA +
					COL_MBID             + UUID_TYPE            + COMMA +
					COL_ARTIST           + VARCHAR_1000         + COMMA +
					COL_TITLE            + VARCHAR_1000         + COMMA +
					COL_MEDIA_YEAR       + VARCHAR_20           + COMMA +
					COL_GENRE            + VARCHAR_1000         + COMMA +
					COL_ARTIST_ID        + UUID_TYPE            + COMMA +
					COL_RATING           + INTEGER              +
				")",
				CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ARTIST + ")",
				CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ARTIST_ID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ARTIST_ID + ")",
				CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_MBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_MBID + ")",
				CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_GENRE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_GENRE + ")"
		);
	}

	private static String constructTagWhere(final CoverArtArchiveTagInfo tagInfo, final boolean includeAll) {
		StringBuilder where = new StringBuilder(WHERE);
		boolean added = false;

		if (includeAll || StringUtil.hasValue(tagInfo.artistId)) {
			where.append(COL_ARTIST_ID).append(sqlNullIfBlank(tagInfo.artistId, true, false));
			added = true;
		}
		if (includeAll || (!StringUtil.hasValue(tagInfo.artistId) && StringUtil.hasValue(tagInfo.artist))) {
			if (added) {
				where.append(AND);
			}
			where.append(COL_ARTIST).append(sqlNullIfBlank(tagInfo.artist, true, false));
			added = true;
		}

		if (includeAll || (StringUtil.hasValue(tagInfo.title) && (StringUtil.hasValue(tagInfo.artist) ||
				StringUtil.hasValue(tagInfo.artistId)))) {
			if (added) {
				where.append(AND);
			}
			where.append(COL_TITLE).append(sqlNullIfBlank(tagInfo.title, true, false));
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.year)) {
			if (added) {
				where.append(AND);
			}
			where.append(COL_MEDIA_YEAR).append(sqlNullIfBlank(tagInfo.year, true, false));
		}

		return where.toString();
	}

	/**
	 * Stores the MBID with information from this {@link Tag} in the database
	 *
	 * @param connection the db connection
	 * @param mBID the MBID to store
	 * @param tagInfo the {@link Tag} who's information should be associated
	 *                with the given MBID
	 */
	public static void writeMBID(final Connection connection, final String mBID, final CoverArtArchiveTagInfo tagInfo) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			String query = SELECT_ALL + FROM + TABLE_NAME + constructTagWhere(tagInfo, true) + LIMIT_1;
			if (trace) {
				LOGGER.trace("Searching for release MBID with \"{}\" before update", query);
			}

			try (
				Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet result = statement.executeQuery(query)
			) {
				if (result.next()) {
					if (StringUtil.hasValue(mBID) || !StringUtil.hasValue(result.getString(COL_MBID))) {
						if (trace) {
							LOGGER.trace("Updating row {} to MBID \"{}\"", result.getInt(COL_ID), mBID);
						}
						result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
						if (StringUtil.hasValue(mBID)) {
							result.updateString(COL_MBID, mBID);
						} else {
							result.updateNull(COL_MBID);
						}
						result.updateRow();
					} else if (trace) {
						LOGGER.trace("Leaving row {} alone since previous information seems better", result.getInt("ID"));
					}
				} else {
					if (trace) {
						LOGGER.trace(
								"Inserting new row for MBID '{}':\n\tArtist    \"{}\"\n\tTitle     \"{}\"\n\tYear      \"{}\"\n\tArtist ID \"{}\"\n\tTrack ID  \"{}\"\n",
								mBID, tagInfo.artist,
								tagInfo.title, tagInfo.year,
								tagInfo.artistId, tagInfo.trackId
						);
					}

					result.moveToInsertRow();
					result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
					if (StringUtil.hasValue(mBID)) {
						result.updateString(COL_MBID, mBID);
					}
					if (StringUtil.hasValue(tagInfo.artist)) {
						result.updateString(COL_ARTIST, StringUtils.left(tagInfo.artist, 1000));
					}
					if (StringUtil.hasValue(tagInfo.title)) {
						result.updateString(COL_TITLE, StringUtils.left(tagInfo.title, 1000));
					}
					if (StringUtil.hasValue(tagInfo.year)) {
						result.updateString(COL_MEDIA_YEAR, StringUtils.left(tagInfo.year, 20));
					}
					if (StringUtil.hasValue(tagInfo.artistId)) {
						result.updateString(COL_ARTIST_ID, tagInfo.artistId);
					}
					result.insertRow();
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN_FOR, DATABASE_NAME, "writing Music Brainz ID", mBID, TABLE_NAME, tagInfo, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Looks up MBID in the table based on the given {@link Tag}. Never returns
	 * <code>null</code>
	 *
	 * @param connection the db connection
	 * @param tagInfo the {@link Tag} for whose values should be used in the search
	 *
	 * @return The result of the search, never <code>null</code>
	 */
	public static MusicBrainzReleasesResult findMBID(final Connection connection, final CoverArtArchiveTagInfo tagInfo) {
		boolean trace = LOGGER.isTraceEnabled();
		MusicBrainzReleasesResult result;

		try {
			String query = SELECT + COL_MBID + COMMA + COL_MODIFIED + FROM + TABLE_NAME + constructTagWhere(tagInfo, false) + LIMIT_1;

			if (trace) {
				LOGGER.trace("Searching for release MBID with \"{}\"", query);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query)
			) {
				if (resultSet.next()) {
					result = new MusicBrainzReleasesResult(true, resultSet.getTimestamp(COL_MODIFIED), resultSet.getString(COL_MBID));
				} else {
					result = new MusicBrainzReleasesResult(false, null, null);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "looking up Music Brainz ID", TABLE_NAME, tagInfo, e.getMessage());
			LOGGER.trace("", e);
			result = new MusicBrainzReleasesResult();
		}

		return result;
	}

	public static MusicBrainzAlbum getMusicBrainzAlbum(String mbid) {
		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			PreparedStatement updateStatement = connection.prepareStatement(SQL_GET_MBID, ResultSet.TYPE_FORWARD_ONLY);
		) {
			UUID mbidRecord = UUID.fromString(StringUtils.trimToEmpty(mbid));
			updateStatement.setObject(1, mbidRecord);
			try (ResultSet rs = updateStatement.executeQuery()) {
				if (rs.next()) {
					return new MusicBrainzAlbum(mbid, rs.getString(COL_TITLE), rs.getString(COL_ARTIST), rs.getString(COL_MEDIA_YEAR),
							rs.getString(COL_GENRE));
				} else {
					LOGGER.debug("mbid not found in database : " + mbid);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("getMusicBrainzAlbum : Database error in " + TABLE_NAME + " for \"{}\": {}", mbid, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	public static void storeMusicBrainzAlbum(MusicBrainzAlbum album) {
		try (
			Connection connection = MediaDatabase.getConnectionIfAvailable();
			PreparedStatement updateStatement = connection.prepareStatement(SQL_GET_MBID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
		) {
			UUID mbidRecord = UUID.fromString(StringUtils.trimToEmpty(album.getMbReleaseid()));
			updateStatement.setObject(1, mbidRecord);
			try (ResultSet rs = updateStatement.executeQuery()) {
				boolean isCreatingNewRecord = !rs.next();
				if (isCreatingNewRecord) {
					rs.moveToInsertRow();
				}
				updateAudioMetadata(rs, album);
				if (isCreatingNewRecord) {
					rs.insertRow();
				} else {
					rs.updateRow();
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", album, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static void updateAudioMetadata(ResultSet result, MusicBrainzAlbum album) throws SQLException {
		try {
			UUID mbidRecord = UUID.fromString(StringUtils.trimToEmpty(album.getMbReleaseid()));
			result.updateObject(COL_MBID, mbidRecord);
		} catch (IllegalArgumentException e) {
			LOGGER.error("invalid UUID. Cannot add album information.", e);
			return;
		}
		updateString(result, COL_ARTIST, album.getArtist(), 1000);
		updateString(result, COL_TITLE, album.getAlbum(), 1000);
		updateString(result, COL_MEDIA_YEAR, album.getYear(), 1000);
		updateString(result, COL_GENRE, album.getGenre(), 1000);
		// ArtistID for future reference
	}

	/**
	 * A type class for returning results from MusicBrainzReleases database
	 * lookup.
	 */
	public static class MusicBrainzReleasesResult {

		private final boolean found;
		private final Timestamp modified;
		private final String mBID;

		public MusicBrainzReleasesResult() {
			this(false, null, null);
		}

		public MusicBrainzReleasesResult(final boolean found, final Timestamp modified, final String mBID) {
			this.found = found;
			this.modified = modified;
			this.mBID = mBID;
		}

		public boolean isFound() {
			return found;
		}

		public long getModifiedTime() {
			return modified.getTime();
		}

		public boolean hasMusicBrainzId() {
			return StringUtils.isNotBlank(mBID);
		}

		public String getMusicBrainzId() {
			return mBID;
		}
	}

}
