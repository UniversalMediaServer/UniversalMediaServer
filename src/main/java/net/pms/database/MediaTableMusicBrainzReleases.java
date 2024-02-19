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
 * This class is responsible for managing the MusicBrainz releases table. It
 * does everything from creating, checking and upgrading the table to
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
	private static final int TABLE_VERSION = 4;
	private static final String SQL_GET_MBID = "SELECT * FROM " + TABLE_NAME + " WHERE MBID = ?";

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
				case 1:
					// Version 2 increases the size of ARTIST; ALBUM, TITLE and YEAR.
					try (Statement statement = connection.createStatement()) {
						statement.executeUpdate(
							ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "ARTIST" + VARCHAR_1000
						);
						statement.executeUpdate(
							ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "TITLE" + VARCHAR_1000
						);
						statement.executeUpdate(
							ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "MEDIA_YEAR" + VARCHAR_20
						);
					}
					break;
				case 2:
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`YEAR`" + RENAME_TO + "MEDIA_YEAR");
					}
					break;
				case 3:
					if (isColumnExist(connection, TABLE_NAME, "MBID")) {
						LOGGER.trace("alter datatype of MBID column from VARCHAR to UUID");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN MBID SET DATA TYPE UUID");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN ARTIST_ID SET DATA TYPE UUID");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN GENRE VARCHAR(1000)");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN RATING INTEGER");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN TRACK_ID");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN ALBUM");
						executeUpdate(connection, "CREATE INDEX MBID_IDX ON " + TABLE_NAME + "(MBID)");
						executeUpdate(connection, "CREATE INDEX GENRE_IDX ON " + TABLE_NAME + "(GENRE)");
					}
					break;
				default:
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				"ID             IDENTITY         PRIMARY KEY , " +
				"MODIFIED       TIMESTAMP                    , " +
				"MBID           UUID                         , " +
				"ARTIST         VARCHAR(1000)                , " +
				"TITLE          VARCHAR(1000)                , " +
				"MEDIA_YEAR     VARCHAR(20)                  , " +
				"GENRE          VARCHAR(1000)                , " +
				"ARTIST_ID      UUID                         , " +
				"RATING         INTEGER                        " +
			")",
			"CREATE INDEX ARTIST_IDX ON " + TABLE_NAME + "(ARTIST)",
			"CREATE INDEX ARTIST_ID_IDX ON " + TABLE_NAME + "(ARTIST_ID)",
			"CREATE INDEX MBID_IDX ON " + TABLE_NAME + "(MBID)",
			"CREATE INDEX GENRE_IDX ON " + TABLE_NAME + "(GENRE)"
		);
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

	private static String constructTagWhere(final CoverArtArchiveTagInfo tagInfo, final boolean includeAll) {
		StringBuilder where = new StringBuilder(" WHERE ");
		final String and = " AND ";
		boolean added = false;

		if (includeAll || StringUtil.hasValue(tagInfo.artistId)) {
			if (added) {
				where.append(and);
			}
			where.append("ARTIST_ID").append(sqlNullIfBlank(tagInfo.artistId, true, false));
			added = true;
		}
		if (includeAll || (!StringUtil.hasValue(tagInfo.artistId) && StringUtil.hasValue(tagInfo.artist))) {
			if (added) {
				where.append(and);
			}
			where.append("ARTIST").append(sqlNullIfBlank(tagInfo.artist, true, false));
			added = true;
		}

		if (
			includeAll || (
				StringUtil.hasValue(tagInfo.trackId) && (
					StringUtil.hasValue(tagInfo.artist) ||
					StringUtil.hasValue(tagInfo.artistId)
				)
			)
		) {
			if (added) {
				where.append(and);
			}
			where.append("TRACK_ID").append(sqlNullIfBlank(tagInfo.trackId, true, false));
			added = true;
		}
		if (
			includeAll || (
				!StringUtil.hasValue(tagInfo.trackId) && (
					StringUtil.hasValue(tagInfo.title) && (
						StringUtil.hasValue(tagInfo.artist) ||
						StringUtil.hasValue(tagInfo.artistId)
					)
				)
			)
		) {
			if (added) {
				where.append(and);
			}
			where.append("TITLE").append(sqlNullIfBlank(tagInfo.title, true, false));
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.year)) {
			if (added) {
				where.append(and);
			}
			where.append("MEDIA_YEAR").append(sqlNullIfBlank(tagInfo.year, true, false));
		}

		return where.toString();
	}

	/**
	 * Stores the MBID with information from this {@link Tag} in the database
	 *
	 * @param connection the db connection
	 * @param mBID the MBID to store
	 * @param tagInfo the {@link Tag} who's information should be associated with
	 *        the given MBID
	 */
	public static void writeMBID(final Connection connection, final String mBID, final CoverArtArchiveTagInfo tagInfo) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			String query = "SELECT * FROM " + TABLE_NAME + constructTagWhere(tagInfo, true) + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching for release MBID with \"{}\" before update", query);
			}

			try (
				Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet result = statement.executeQuery(query)
			) {
				if (result.next()) {
					if (StringUtil.hasValue(mBID) || !StringUtil.hasValue(result.getString("MBID"))) {
						if (trace) {
							LOGGER.trace("Updating row {} to MBID \"{}\"", result.getInt("ID"), mBID);
						}
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						if (StringUtil.hasValue(mBID)) {
							result.updateString("MBID", mBID);
						} else {
							result.updateNull("MBID");
						}
						result.updateRow();
					} else if (trace) {
						LOGGER.trace("Leaving row {} alone since previous information seems better", result.getInt("ID"));
					}
				} else {
					if (trace) {
						LOGGER.trace(
							"Inserting new row for MBID \"{}\":\n" +
							"     Artist    \"{}\"\n" +
							"     Title     \"{}\"\n" +
							"     Year      \"{}\"\n" +
							"     Artist ID \"{}\"\n" +
							"     Track ID  \"{}\"\n",
							mBID, tagInfo.artist,
							tagInfo.title, tagInfo.year,
							tagInfo.artistId, tagInfo.trackId
						);
					}

					result.moveToInsertRow();
					result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
					if (StringUtil.hasValue(mBID)) {
						result.updateString("MBID", mBID);
					}
					if (StringUtil.hasValue(tagInfo.artist)) {
						result.updateString("ARTIST", StringUtils.left(tagInfo.artist, 1000));
					}
					if (StringUtil.hasValue(tagInfo.title)) {
						result.updateString("TITLE", StringUtils.left(tagInfo.title, 1000));
					}
					if (StringUtil.hasValue(tagInfo.year)) {
						result.updateString("MEDIA_YEAR", StringUtils.left(tagInfo.year, 20));
					}
					if (StringUtil.hasValue(tagInfo.artistId)) {
						result.updateString("ARTIST_ID", tagInfo.artistId);
					}
					if (StringUtil.hasValue(tagInfo.trackId)) {
						result.updateString("TRACK_ID", tagInfo.trackId);
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
			String query = "SELECT MBID, MODIFIED FROM " + TABLE_NAME + constructTagWhere(tagInfo, false) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching for release MBID with \"{}\"", query);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query)
			) {
				if (resultSet.next()) {
					result = new MusicBrainzReleasesResult(true, resultSet.getTimestamp("MODIFIED"), resultSet.getString("MBID"));
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
					MusicBrainzAlbum album = new MusicBrainzAlbum(mbid, rs.getString("TITLE"), rs.getString("ARTIST"), rs.getString("MEDIA_YEAR"),
						rs.getString("GENRE"));
					return album;
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
			result.updateObject("MBID", mbidRecord);
		} catch (IllegalArgumentException e) {
			LOGGER.error("invalid UUID. Cannot add album information.", e);
			return;
		}
		updateString(result, "ARTIST", album.getArtist(), 1000);
		updateString(result, "TITLE", album.getAlbum(), 1000);
		updateString(result, "MEDIA_YEAR", album.getYear(), 1000);
		updateString(result, "GENRE", album.getGenre(), 1000);
		// ArtistID for future reference
	}
}
