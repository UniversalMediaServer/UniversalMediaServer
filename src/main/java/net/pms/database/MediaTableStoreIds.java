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

import com.google.common.primitives.UnsignedInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.pms.store.MediaStoreId;
import net.pms.store.StoreResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Containers/Items ids.
 */
public class MediaTableStoreIds extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableStoreIds.class);
	public static final String TABLE_NAME = "STORE_IDS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 2;

	/**
	 * COLUMNS
	 */
	private static final String COL_ID = "ID";
	private static final String COL_PARENT_ID = "PARENT_ID";
	private static final String COL_NAME = "NAME";
	private static final String COL_OBJECT_TYPE = "OBJECT_TYPE";
	private static final String COL_UPDATE_ID = "UPDATE_ID";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	private static final String TABLE_COL_PARENT_ID = TABLE_NAME + "." + COL_PARENT_ID;
	private static final String TABLE_COL_NAME = TABLE_NAME + "." + COL_NAME;
	private static final String TABLE_COL_OBJECT_TYPE = TABLE_NAME + "." + COL_OBJECT_TYPE;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL_ID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_PARENTID_NAME = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_PARENT_ID + EQUAL + PARAMETER + AND + TABLE_COL_NAME + EQUAL + PARAMETER;
	private static final String SQL_GET_ID_NAME = SELECT + COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_NAME + EQUAL + PARAMETER;
	private static final String SQL_GET_NAME_ID = SELECT + COL_NAME + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_GET_ID_TYPE = SELECT + COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_OBJECT_TYPE + EQUAL + PARAMETER;
	private static final String SQL_GET_ID_NAME_TYPE = SQL_GET_ID_NAME + AND + TABLE_COL_OBJECT_TYPE + EQUAL + PARAMETER;
	private static final String SQL_GET_ID_NAME_TYPE_PARENTTYPE = SQL_GET_ID_NAME_TYPE + AND + TABLE_COL_PARENT_ID + IN + "(" + SQL_GET_ID_TYPE + ")";
	private static final String SQL_UPDATE_UPDATEID_ID = UPDATE + TABLE_NAME + SET + COL_UPDATE_ID + EQUAL + PARAMETER + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;

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
		ensureSystemId(connection);
	}

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					LOGGER.trace("Creating index " + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + COL_OBJECT_TYPE + COL_PARENT_ID + IDX_MARKER);
					executeUpdate(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + COL_OBJECT_TYPE + COL_PARENT_ID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_NAME + ", " + COL_OBJECT_TYPE + ", " + COL_PARENT_ID + ")");
					LOGGER.trace("Creating index " + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + COL_OBJECT_TYPE + IDX_MARKER);
					executeUpdate(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + COL_OBJECT_TYPE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_NAME + ", " + COL_OBJECT_TYPE + ")");
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
					COL_ID +              BIGINT + " GENERATED BY DEFAULT AS IDENTITY(START WITH 100)" + PRIMARY_KEY + COMMA +
					COL_PARENT_ID +       BIGINT           + NOT_NULL + COMMA +
					COL_NAME +            VARCHAR          + NOT_NULL + COMMA +
					COL_OBJECT_TYPE +     VARCHAR          + NOT_NULL + COMMA +
					COL_UPDATE_ID +       BIGINT                              +
				")",
				CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_PARENT_ID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_PARENT_ID + ")",
				CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + IDX_MARKER + ON + TABLE_NAME + "(" + COL_NAME + ")",
				CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + COL_OBJECT_TYPE + COL_PARENT_ID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_NAME + ", " + COL_OBJECT_TYPE + ", " + COL_PARENT_ID + ")",
				CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_NAME + COL_OBJECT_TYPE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_NAME + ", " + COL_OBJECT_TYPE + ")"
		);
		ensureSystemId(connection);
	}

	/**
	 * Get an id for a StoreResource.
	 *
	 * Usefull for the MediaStore to use the old known id if any.
	 *
	 * @param connection
	 * @param resource
	 * @return the StoreId stored or created
	 */
	public static MediaStoreId getResourceMediaStoreId(Connection connection, StoreResource resource) {
		if (connection == null || resource == null || resource.getParent() == null || resource.getParent().getLongId() == null) {
			return null;
		}
		long parentId = resource.getParent().getLongId();
		String name = resource.getSystemName();

		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_PARENTID_NAME, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
			stmt.setLong(1, parentId);
			stmt.setString(2, name);
			try (ResultSet elements = stmt.executeQuery()) {
				if (elements.next()) {
					return getMediaStoreId(connection, elements.getLong(COL_ID));
				} else {
					elements.moveToInsertRow();
					elements.updateLong(COL_PARENT_ID, parentId);
					elements.updateString(COL_NAME, name);
					elements.updateString(COL_OBJECT_TYPE, resource.getClass().getSimpleName());
					elements.updateLong(COL_UPDATE_ID, 0);
					elements.insertRow();
					return getResourceMediaStoreId(connection, resource);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", resource.getDisplayName(), e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Retreive the last known object from id.
	 *
	 * Usefull when a renderer ask an object that need to be recreated.
	 *
	 * @param connection
	 * @param id
	 * @return the last known object
	 */
	public static MediaStoreId getMediaStoreId(Connection connection, long id) {
		if (connection == null) {
			return null;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_ID)) {
			stmt.setLong(1, id);
			try (ResultSet elements = stmt.executeQuery()) {
				if (elements.next()) {
					MediaStoreId result = new MediaStoreId();
					result.setId(elements.getLong(COL_ID));
					result.setParentId(elements.getLong(COL_PARENT_ID));
					result.setName(elements.getString(COL_NAME));
					result.setObjectType(elements.getString(COL_OBJECT_TYPE));
					result.setUpdateId(elements.getLong(COL_UPDATE_ID));
					return result;
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", id, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	public static void setMediaStoreUpdateId(Connection connection, long id, long updateId) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_UPDATE_UPDATEID_ID)) {
			stmt.setLong(1, updateId);
			stmt.setLong(2, id);
			stmt.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static String getMediaStoreNameForId(Connection connection, String id) {
		if (connection == null) {
			return null;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_NAME_ID)) {
			stmt.setLong(1, Long.parseLong(id));
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					return elements.getString(COL_NAME);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for id \"{}\": {}", id, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	public static List<Long> getMediaStoreIdsForName(Connection connection, String name) {
		List<Long> result = new ArrayList<>();
		if (connection == null) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ID_NAME)) {
			stmt.setString(1, name);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					result.add(elements.getLong(COL_ID));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for name \"{}\": {}", name, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static List<Long> getMediaStoreIdsForName(Connection connection, String name, String objectType) {
		List<Long> result = new ArrayList<>();
		if (connection == null) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ID_NAME_TYPE)) {
			stmt.setString(1, name);
			stmt.setString(2, objectType);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					result.add(elements.getLong(COL_ID));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for name \"{}\" with type \"{}\": {}", name, objectType, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static List<Long> getMediaStoreIdsForName(Connection connection, String name, String objectType, String parentType) {
		List<Long> result = new ArrayList<>();
		if (connection == null) {
			return result;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ID_NAME_TYPE_PARENTTYPE)) {
			stmt.setString(1, name);
			stmt.setString(2, objectType);
			stmt.setString(3, parentType);
			try (ResultSet elements = stmt.executeQuery()) {
				while (elements.next()) {
					result.add(elements.getLong(COL_ID));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for name \"{}\" with type \"{}\" and parent type \"{}\": {}", name, objectType, parentType, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	private static void ensureSystemId(Connection connection) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
			stmt.setLong(1, -1L);
			try (ResultSet elements = stmt.executeQuery()) {
				if (!elements.next()) {
					UnsignedInteger systemUpdateId = getPreviousSystemUpdateId(connection);
					elements.moveToInsertRow();
					elements.updateLong(COL_ID, -1);
					elements.updateLong(COL_PARENT_ID, -1);
					elements.updateString(COL_NAME, "System");
					elements.updateString(COL_OBJECT_TYPE, "");
					elements.updateLong(COL_UPDATE_ID, systemUpdateId.longValue());
					elements.insertRow();
				}
			}
			stmt.clearParameters();
			stmt.setLong(1, 0);
			try (ResultSet elements = stmt.executeQuery()) {
				if (!elements.next()) {
					UnsignedInteger systemUpdateId = getPreviousSystemUpdateId(connection);
					elements.moveToInsertRow();
					elements.updateLong(COL_ID, 0);
					elements.updateLong(COL_PARENT_ID, -1);
					elements.updateString(COL_NAME, "Root");
					elements.updateString(COL_OBJECT_TYPE, "MediaStore");
					elements.updateLong(COL_UPDATE_ID, systemUpdateId.longValue());
					elements.insertRow();
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", "System id", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static UnsignedInteger getPreviousSystemUpdateId(Connection connection) {
		UnsignedInteger systemUpdateId;
		String systemUpdateIdFromDb = MediaTableMetadata.getMetadataValue(connection, "SystemUpdateID");
		if (systemUpdateIdFromDb != null) {
			try {
				systemUpdateId = UnsignedInteger.valueOf(systemUpdateIdFromDb);
			} catch (NumberFormatException ex) {
				LOGGER.debug("" + ex);
				systemUpdateId = UnsignedInteger.ZERO;
			}
		} else {
			systemUpdateId = UnsignedInteger.ZERO;
		}
		return systemUpdateId;
	}

}
