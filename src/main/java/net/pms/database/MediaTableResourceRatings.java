package net.pms.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
	private static final int TABLE_VERSION = 1;

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
				default -> throw new IllegalStateException(
					getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
				);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
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
	 *
	 * @param resourceKey the resource key, see StoreResource.getRatingKey()
	 * @param objectType the resource class simple name, stored for diagnostics only
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
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL_KEY, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
			statement.setString(1, resourceKey);
			try (ResultSet result = statement.executeQuery()) {
				if (result.next()) {
					result.updateString(COL_OBJECT_TYPE, objectType);
					updateInteger(result, COL_RATING, rating);
					result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
					result.updateRow();
				} else {
					result.moveToInsertRow();
					result.updateString(COL_RESOURCE_KEY, resourceKey);
					result.updateString(COL_OBJECT_TYPE, objectType);
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
