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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.util.CoverArtArchiveUtil.CoverArtArchiveTagInfo;
import net.pms.util.StringUtil;
import org.jaudiotagger.tag.Tag;
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

public final class TableMusicBrainzReleases extends Tables {

	/**
	 * tableLock is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock tableLock = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableMusicBrainzReleases.class);
	private static final String TABLE_NAME = "MUSIC_BRAINZ_RELEASES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 2;

	// No instantiation
	private TableMusicBrainzReleases() {
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
		final String AND = " AND ";
		boolean added = false;

		if (includeAll || StringUtil.hasValue(tagInfo.album)) {
			where.append("ALBUM").append(sqlNullIfBlank(tagInfo.album, true, false));
			added = true;
		}
		if (includeAll || StringUtil.hasValue(tagInfo.artistId)) {
			if (added) {
				where.append(AND);
			}
			where.append("ARTIST_ID").append(sqlNullIfBlank(tagInfo.artistId, true, false));
			added = true;
		}
		if (includeAll || (!StringUtil.hasValue(tagInfo.artistId) && StringUtil.hasValue(tagInfo.artist))) {
			if (added) {
				where.append(AND);
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
				where.append(AND);
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
				where.append(AND);
			}
			where.append("TITLE").append(sqlNullIfBlank(tagInfo.title, true, false));
			added = true;
		}

		if (StringUtil.hasValue(tagInfo.year)) {
			if (added) {
				where.append(AND);
			}
			where.append("YEAR").append(sqlNullIfBlank(tagInfo.year, true, false));
			added = true;
		}

		return where.toString();
	}

	/**
	 * Stores the MBID with information from this {@link Tag} in the database
	 *
	 * @param mBID the MBID to store
	 * @param tag the {@link Tag} who's information should be associated with
	 *        the given MBID
	 */
	public static void writeMBID(final String mBID, final CoverArtArchiveTagInfo tagInfo) {
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = database.getConnection()) {
			String query = "SELECT * FROM " + TABLE_NAME + constructTagWhere(tagInfo, true);
			if (trace) {
				LOGGER.trace("Searching for release MBID with \"{}\" before update", query);
			}

			tableLock.writeLock().lock();
			try (Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)){
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery(query)){
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
							result.updateString("YEAR", left(tagInfo.year, 20));
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
			} finally {
				tableLock.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing Music Brainz ID \"{}\" for \"{}\": {}",
				mBID,
				tagInfo,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Looks up MBID in the table based on the given {@link Tag}. Never returns
	 * <code>null</code>
	 *
	 * @param tag the {@link Tag} for whose values should be used in the search
	 *
	 * @return The result of the search, never <code>null</code>
	 */
	public static MusicBrainzReleasesResult findMBID(final CoverArtArchiveTagInfo tagInfo) {
		boolean trace = LOGGER.isTraceEnabled();
		MusicBrainzReleasesResult result;

		try (Connection connection = database.getConnection()) {
			String query = "SELECT MBID, MODIFIED FROM " + TABLE_NAME + constructTagWhere(tagInfo, false);

			if (trace) {
				LOGGER.trace("Searching for release MBID with \"{}\"", query);
			}

			tableLock.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						result = new MusicBrainzReleasesResult(true, resultSet.getTimestamp("MODIFIED"), resultSet.getString("MBID"));
					} else {
						result = new MusicBrainzReleasesResult(false, null, null);
					}
				}
			} finally {
				tableLock.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error while looking up Music Brainz ID for \"{}\": {}", tagInfo, e.getMessage());
			LOGGER.trace("", e);
			result = new MusicBrainzReleasesResult();
		}

		return result;
	}

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		tableLock.writeLock().lock();
		try {
			if (tableExists(connection, TABLE_NAME)) {
				Integer version = getTableVersion(connection, TABLE_NAME);
				if (version != null) {
					if (version < TABLE_VERSION) {
						upgradeTable(connection, version);
					} else if (version > TABLE_VERSION) {
						LOGGER.warn(
							"Database table \"" + TABLE_NAME +
							"\" is from a newer version of UMS. If you experience problems, you could try to move, rename or delete database file \"" +
							database.getDatabaseFilename() +
							"\" before starting UMS"
						);
					}
				} else {
					LOGGER.warn("Database table \"{}\" has an unknown version and cannot be used. Dropping and recreating table", TABLE_NAME);
					dropTable(connection, TABLE_NAME);
					createMusicBrainzReleasesTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createMusicBrainzReleasesTable(connection);
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			tableLock.writeLock().unlock();
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
		LOGGER.info("Upgrading database table \"{}\" from version {} to {}", TABLE_NAME, currentVersion, TABLE_VERSION);
		tableLock.writeLock().lock();
		try {
			for (int version = currentVersion;version < TABLE_VERSION; version++) {
				LOGGER.trace("Upgrading table {} from version {} to {}", TABLE_NAME, version, version + 1);
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
							"ALTER TABLE " + TABLE_NAME + " ALTER COLUMN YEAR VARCHAR(20)"
						);
						break;
					default:
						throw new IllegalStateException(
							"Table \"" + TABLE_NAME + "is missing table upgrade commands from version " +
							version + " to " + TABLE_VERSION
						);
				}
			}
			setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} finally {
			tableLock.writeLock().unlock();
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createMusicBrainzReleasesTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID IDENTITY PRIMARY KEY, " +
					"MODIFIED DATETIME, " +
					"MBID VARCHAR(36), " +
					"ARTIST VARCHAR(1000), " +
					"ALBUM VARCHAR(1000), " +
					"TITLE VARCHAR(1000), " +
					"YEAR VARCHAR(20), " +
					"ARTIST_ID VARCHAR(36), " +
					"TRACK_ID VARCHAR(36)" +
				")");
			statement.execute("CREATE INDEX ARTIST_IDX ON " + TABLE_NAME + "(ARTIST)");
			statement.execute("CREATE INDEX ARTIST_ID_IDX ON " + TABLE_NAME + "(ARTIST_ID)");
		}
	}
}
