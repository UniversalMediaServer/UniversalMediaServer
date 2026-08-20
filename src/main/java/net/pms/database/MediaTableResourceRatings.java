package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.pms.store.DbIdMediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the ResourceRatings table. It holds
 * the user rating (0 - 5 stars) of any kind of store resource : items as well
 * as containers. Ratings are global, there is one rating per resource.
 *
 * Rows are keyed on a resource key, which is a stable identity of the resource
 * independent of its position in the store tree, see
 * StoreResource.getRatingKey().
 */
public final class MediaTableResourceRatings extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableResourceRatings.class);
	public static final String TABLE_NAME = "RESOURCE_RATINGS";

	/**
	 * Table version must be increased every time a change is done to the table definition. Table upgrade SQL must also be added to
	 * upgradeTable(Connection, int)
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * The rating that represents a "like". Used by the My Albums folder.
	 */
	public static final int RATING_LIKED = 5;

	/**
	 * The rating that represents a "dislike".
	 */
	public static final int RATING_DISLIKED = 0;

	/**
	 * OBJECT_TYPE stored for album containers, whatever container class was used
	 * to browse the album, see GETALBUMOBJECTTYPE().
	 */
	public static final String MUSIC_ALBUM_OBJECT_TYPE = "MusicAlbumFolder";

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_RESOURCE_KEY = "RESOURCE_KEY";
	private static final String COL_OBJECT_TYPE = "OBJECT_TYPE";
	private static final String COL_RATING = "RATING";
	private static final String COL_MODIFIED = "MODIFIED";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_RESOURCE_KEY = TABLE_NAME + "." + COL_RESOURCE_KEY;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_KEY = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_RESOURCE_KEY + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_RATING_KEY = SELECT + COL_RATING + FROM + TABLE_NAME + WHERE + TABLE_COL_RESOURCE_KEY + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ALL_ROWS = SELECT + COL_RESOURCE_KEY + COMMA + COL_OBJECT_TYPE + COMMA + COL_RATING + FROM + TABLE_NAME + WHERE + COL_RATING + IS_NOT_NULL;
	private static final String SQL_DELETE_KEY = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_RESOURCE_KEY + EQUAL + PARAMETER;

	/**
	 * Checks and creates or upgrades the table as needed.
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

	private static void upgradeTable(final Connection connection, final Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> migrateAlbumLikes(connection);
				default -> throw new IllegalStateException(
					getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
				);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	/**
	 * Copies the album likes of the legacy like tables into this table, so the My Albums folder keeps its content after the upgrade.
	 */
	public static void migrateAlbumLikes(final Connection connection) throws SQLException {
		migrateAlbumLikes(connection, MediaTableMusicBrainzReleaseLike.TABLE_NAME, "MBID_RELEASE", DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID);
		migrateAlbumLikes(connection, MediaTableDiscogsReleaseLike.TABLE_NAME, "DISCOGS_RELEASE_ID", DbIdMediaType.TYPE_DISCOGS_RELEASEID);
	}

	private static void migrateAlbumLikes(final Connection connection, final String likeTable, final String likeColumn, final DbIdMediaType type) throws SQLException {
		if (!tableExists(connection, likeTable)) {
			//nothing to migrate, for example after a cache reset
			return;
		}
		String key = sqlQuote(type.toString()) + " || CAST(" + likeColumn + " AS VARCHAR)";
		String sql = INSERT_INTO + TABLE_NAME + "(" + COL_RESOURCE_KEY + COMMA + COL_OBJECT_TYPE + COMMA + COL_RATING + COMMA + COL_MODIFIED + ") " +
			SELECT + key + COMMA + sqlQuote(MUSIC_ALBUM_OBJECT_TYPE) + COMMA + RATING_LIKED + COMMA + "CURRENT_TIMESTAMP" +
			FROM + likeTable +
			WHERE + likeColumn + IS_NOT_NULL +
			AND + "NOT " + EXISTS + "(" + SELECT + "1" + FROM + TABLE_NAME + WHERE + TABLE_COL_RESOURCE_KEY + EQUAL + key + ")";
		executeUpdate(connection, sql);
		LOGGER.info("Database \"{}\" migrated the album likes of \"{}\" into \"{}\"", DATABASE_NAME, likeTable, TABLE_NAME);
	}

	/*
	 * a restore brings back the state of the backup, so what it does not name goes away
	 */
	public static int deleteRatingsNotIn(final Connection connection, final Collection<String> keys) throws SQLException {
		if (keys.isEmpty()) {
			//an empty backup must not wipe the table
			LOGGER.warn("Database \"{}\" kept the ratings of \"{}\", the restore knows no keys at all", DATABASE_NAME, TABLE_NAME);
			return 0;
		}
		String tempTable = TABLE_NAME + "_RESTORE_KEYS";
		executeUpdate(connection, "CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS " + tempTable + "(" + COL_RESOURCE_KEY + VARCHAR_1024 + PRIMARY_KEY + ")");
		try {
			executeUpdate(connection, DELETE_FROM + tempTable);
			try (PreparedStatement insert = connection.prepareStatement(INSERT_INTO + tempTable + "(" + COL_RESOURCE_KEY + ") VALUES (?)")) {
				Set<String> distinct = new HashSet<>(keys);
				for (String key : distinct) {
					insert.setString(1, key);
					insert.addBatch();
				}
				insert.executeBatch();
			}
			int deleted;
			try (PreparedStatement delete = connection.prepareStatement(
					DELETE_FROM + TABLE_NAME + WHERE + COL_RESOURCE_KEY + " NOT IN (" + SELECT + COL_RESOURCE_KEY + FROM + tempTable + ")")) {
				deleted = delete.executeUpdate();
			}
			LOGGER.info("Database \"{}\" deleted {} ratings of \"{}\" that the backup does not contain", DATABASE_NAME, deleted, TABLE_NAME);
			return deleted;
		} finally {
			executeUpdate(connection, "DROP TABLE IF EXISTS " + tempTable);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID            + IDENTITY      + PRIMARY_KEY + COMMA +
				COL_RESOURCE_KEY  + VARCHAR_1024  + NOT_NULL    + COMMA +
				COL_OBJECT_TYPE   + VARCHAR                     + COMMA +
				COL_RATING        + INTEGER                     + COMMA +
				COL_MODIFIED      + TIMESTAMP                   +
			")",
			CREATE_UNIQUE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_RESOURCE_KEY + IDX_MARKER + ON + TABLE_NAME + "(" + COL_RESOURCE_KEY + ")"
		);
	}

	/**
	 * Gets the rating of a resource.
	 *
	 * @return the rating (0 - 5 stars) or NULL if the resource is not rated
	 */
	public static Integer getRating(final Connection connection, final String resourceKey) {
		if (connection == null || resourceKey == null) {
			return null;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_RATING_KEY)) {
			statement.setString(1, resourceKey);
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					return toInteger(rs, COL_RATING);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting rating", TABLE_NAME, resourceKey, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Sets the rating of a resource. A NULL rating removes the rating.
	 * @param rating the rating (0 - 5 stars) or NULL to remove it
	 */
	public static void setRating(final Connection connection, final String resourceKey, final String objectType, final Integer rating) {
		if (connection == null || resourceKey == null) {
			return;
		}
		if (rating == null) {
			deleteRating(connection, resourceKey);
			return;
		}
		final String storedObjectType = getAlbumObjectType(resourceKey, objectType);
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL_KEY, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setString(1, resourceKey);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					result.updateString(COL_OBJECT_TYPE, storedObjectType);
					updateInteger(result, COL_RATING, rating);
					result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
					result.updateRow();
				} else {
					result.moveToInsertRow();
					result.updateString(COL_RESOURCE_KEY, resourceKey);
					result.updateString(COL_OBJECT_TYPE, storedObjectType);
					updateInteger(result, COL_RATING, rating);
					result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
					result.insertRow();
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN_FOR, DATABASE_NAME, "writing rating", rating, TABLE_NAME, resourceKey, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Check, if we have an music album.
	 *
	 * @return the object type to store
	 */
	private static String getAlbumObjectType(final String resourceKey, final String objectType) {
		if (resourceKey.startsWith(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID.toString()) ||
			resourceKey.startsWith(DbIdMediaType.TYPE_DISCOGS_RELEASEID.toString()) ||
			resourceKey.startsWith(DbIdMediaType.TYPE_ALBUM.toString())) {
			return MUSIC_ALBUM_OBJECT_TYPE;
		}
		return objectType;
	}

	/**
	 * Removes the rating of a resource.
	 *
	 * @param resourceKey the resource key, see StoreResource.getRatingKey()
	 */
	public static void deleteRating(final Connection connection, final String resourceKey) {
		if (connection == null || resourceKey == null) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE_KEY)) {
			statement.setString(1, resourceKey);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "deleting rating", TABLE_NAME, resourceKey, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Tells whether an album is liked, that means rated with RATING_LIKED.
	 *
	 * @param connection the db connection
	 * @param type the album type, either a MusicBrainz or a Discogs release
	 * @param ident the release id
	 * @return true if the album is liked
	 */
	public static boolean isAlbumLiked(final Connection connection, final DbIdMediaType type, final String ident) {
		Integer rating = getRating(connection, type.getResourceKey(ident));
		return rating != null && rating == RATING_LIKED;
	}

	/**
	 * Likes or unlikes an album. Unliking removes the rating instead of storing a
	 * dislike, to keep the behaviour of the legacy like tables.
	 *
	 * @param connection the db connection
	 * @param type the album type, either a MusicBrainz or a Discogs release
	 * @param ident the release id
	 * @param liked true to like the album, false to remove the like
	 */
	public static void setAlbumLiked(final Connection connection, final DbIdMediaType type, final String ident, final boolean liked) {
		setRating(connection, type.getResourceKey(ident), MUSIC_ALBUM_OBJECT_TYPE, liked ? RATING_LIKED : null);
	}

	/**
	 * Returns every stored rating. Used to backup ratings.
	 *
	 * @return the list of stored ratings, never NULL
	 */
	public static List<ResourceRating> getAllRatings(final Connection connection) {
		List<ResourceRating> result = new ArrayList<>();
		if (connection == null) {
			return result;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL_ROWS)) {
			try (ResultSet rs = statement.executeQuery()) {
				while (rs.next()) {
					result.add(new ResourceRating(
						rs.getString(COL_RESOURCE_KEY),
						rs.getString(COL_OBJECT_TYPE),
						toInteger(rs, COL_RATING)
					));
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting ratings", TABLE_NAME, "all", e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	/**
	 * A stored rating row.
	 */
	public record ResourceRating(String resourceKey, String objectType, Integer rating) {
	}

}
