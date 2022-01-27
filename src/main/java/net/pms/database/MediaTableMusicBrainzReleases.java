/*
 * Universal Media Server, for streaming any media to DLNA
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

import static org.apache.commons.lang3.StringUtils.left;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import net.pms.util.CoverArtArchiveUtil.CoverArtArchiveTagInfo;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
	private static final int TABLE_VERSION = 3;


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
					Statement statement = connection.createStatement();
					statement.executeUpdate(
						"ALTER TABLE " + TABLE_NAME + " ALTER COLUMN ARTIST VARCHAR(1000)"
					);
					statement.executeUpdate(
						"ALTER TABLE " + TABLE_NAME + " ALTER COLUMN ALBUM VARCHAR(1000)"
					);
					statement.executeUpdate(
						"ALTER TABLE " + TABLE_NAME + " ALTER COLUMN TITLE VARCHAR(1000)"
					);
					statement.executeUpdate(
						"ALTER TABLE " + TABLE_NAME + " ALTER COLUMN MEDIA_YEAR VARCHAR(20)"
					);
					break;
				case 2:
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
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
				"ID				IDENTITY		PRIMARY KEY	, " +
				"MODIFIED		DATETIME					, " +
				"MBID			VARCHAR(36)					, " +
				"ARTIST			VARCHAR(1000)				, " +
				"ALBUM			VARCHAR(1000)				, " +
				"TITLE			VARCHAR(1000)				, " +
				"MEDIA_YEAR		VARCHAR(20)					, " +
				"ARTIST_ID		VARCHAR(36)					, " +
				"TRACK_ID		VARCHAR(36)					  " +
			")",
			"CREATE INDEX ARTIST_IDX ON " + TABLE_NAME + "(ARTIST)",
			"CREATE INDEX ARTIST_ID_IDX ON " + TABLE_NAME + "(ARTIST_ID)"
		);
	}

	/**
	 * A type class for returning results from MusicBrainzReleases database
	 * lookup.
	 */
	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	public static class MusicBrainzReleasesResult {

		public boolean found = false;
		public Timestamp modified = null;
		public String mBID = null;

		public MusicBrainzReleasesResult() {
		}

		@SuppressFBWarnings("EI_EXPOSE_REP2")
		public MusicBrainzReleasesResult(final boolean found, final Timestamp modified, final String mBID) {
			this.found = found;
			this.modified = modified;
			this.mBID = mBID;
		}
	}

	private static String constructTagWhere(final CoverArtArchiveTagInfo tagInfo, final boolean includeAll) {
		StringBuilder where = new StringBuilder(" WHERE ");
		final String and = " AND ";
		boolean added = false;

		if (includeAll || StringUtil.hasValue(tagInfo.album)) {
			where.append("ALBUM").append(sqlNullIfBlank(tagInfo.album, true, false));
			added = true;
		}
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
					!StringUtil.hasValue(tagInfo.album) || !(
						StringUtil.hasValue(tagInfo.artist) ||
						StringUtil.hasValue(tagInfo.artistId)
					)
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
						!StringUtil.hasValue(tagInfo.album) || !(
							StringUtil.hasValue(tagInfo.artist) ||
							StringUtil.hasValue(tagInfo.artistId)
						)
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

			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery(query)) {
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
								"     Album     \"{}\"\n" +
								"     Title     \"{}\"\n" +
								"     Year      \"{}\"\n" +
								"     Artist ID \"{}\"\n" +
								"     Track ID  \"{}\"\n",
								mBID, tagInfo.artist, tagInfo.album,
								tagInfo.title, tagInfo.year,
								tagInfo.artistId, tagInfo.trackId
							);
						}

						result.moveToInsertRow();
						result.updateTimestamp("MODIFIED", new Timestamp(System.currentTimeMillis()));
						if (StringUtil.hasValue(mBID)) {
							result.updateString("MBID", mBID);
						}
						if (StringUtil.hasValue(tagInfo.album)) {
							result.updateString("ALBUM", left(tagInfo.album, 1000));
						}
						if (StringUtil.hasValue(tagInfo.artist)) {
							result.updateString("ARTIST", left(tagInfo.artist, 1000));
						}
						if (StringUtil.hasValue(tagInfo.title)) {
							result.updateString("TITLE", left(tagInfo.title, 1000));
						}
						if (StringUtil.hasValue(tagInfo.year)) {
							result.updateString("MEDIA_YEAR", left(tagInfo.year, 20));
						}
						if (StringUtil.hasValue(tagInfo.artistId)) {
							result.updateString("ARTIST_ID", tagInfo.artistId);
						}
						if (StringUtil.hasValue(tagInfo.trackId)) {
							result.updateString("TRACK_ID", tagInfo.trackId);
						}
						result.insertRow();
					}
				} finally {
					connection.commit();
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

}
