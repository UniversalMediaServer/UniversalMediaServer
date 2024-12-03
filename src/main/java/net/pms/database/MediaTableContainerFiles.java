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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for creating and maintaining the database where
 * container information is stored.
 *
 * it keep relation between MediaTableFiles entries.
 */
public class MediaTableContainerFiles extends MediaTable {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableContainerFiles.class);
	protected static final String TABLE_NAME = "CONTAINER_FILES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition.
	 *
	 * Table upgrade SQL must also be added to {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_CONTAINER_ID = "ID";
	private static final String COL_FILEID = MediaTableFiles.CHILD_ID;

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_CONTAINER_ID = TABLE_NAME + "." + COL_CONTAINER_ID;
	private static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;

	/**
	 * SQL Queries
	 */
	private static final String SQL_DELETE_ID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_CONTAINER_ID + EQUAL + PARAMETER;
	private static final String SQL_DELETE_ID_FILEID = SQL_DELETE_ID + AND + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_DELETE_FILEID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_BY_ID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_CONTAINER_ID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_BY_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_ALL_BY_ID_FILEID = SQL_GET_ALL_BY_ID + AND + TABLE_COL_FILEID + EQUAL + PARAMETER;

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

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
				CREATE_TABLE + TABLE_NAME + " (" +
					COL_CONTAINER_ID  + BIGINT    + COMMA +
					COL_FILEID        + BIGINT            +
				")",
				CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_CONTAINER_ID + CONSTRAINT_SEPARATOR + COL_FILEID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_CONTAINER_ID + COMMA + COL_FILEID + ")"
		);
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

	/**
	 * Deletes container entries
	 */
	protected static void deleteContainer(final Connection connection, long containerId) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_ID)) {
			updateStatment.setLong(1, containerId);
			updateStatment.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("Cannot delete container entries for {} ", containerId, e);
		}
	}

	/**
	 * Deletes one container entry
	 */
	public static void deleteContainerEntry(final Long containerId, final Long entryId) {
		if (containerId == null || entryId == null) {
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			deleteContainerEntry(connection, containerId, entryId);
		} catch (Exception e) {
			LOGGER.error("Cannot delete container entry {} for {} ", entryId, containerId, e);
		}
	}

	/**
	 * Deletes one container entry
	 */
	private static void deleteContainerEntry(final Connection connection, final long containerId, final long entryId) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_ID_FILEID)) {
			updateStatment.setLong(1, containerId);
			updateStatment.setLong(2, entryId);
			updateStatment.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("Cannot delete container entry {} for {} ", entryId, containerId, e);
		}
		//clean MediaTableFiles if not more related to a container
		if (Boolean.FALSE.equals(MediaTableContainerFiles.isInContainer(connection, entryId))) {
			MediaTableFiles.removeEntry(connection, entryId);
		}
	}

	/**
	 * Deletes relation with that entry
	 */
	protected static void deleteEntry(final Connection connection, final long entryId) {
		if (connection == null) {
			return;
		}
		try (PreparedStatement updateStatment = connection.prepareStatement(SQL_DELETE_FILEID)) {
			updateStatment.setLong(1, entryId);
			updateStatment.executeUpdate();
		} catch (Exception e) {
			LOGGER.error("Cannot delete entry {}", entryId, e);
		}
	}

	public static void addContainerEntry(final Long containerId, final Long entryId) {
		if (containerId == null || entryId == null) {
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			addContainerEntry(connection, containerId, entryId);
		} catch (Exception e) {
			LOGGER.error("Cannot add container entry {} for {} ", entryId, containerId, e);
		}
	}

	private static void addContainerEntry(final Connection connection, final long containerId, final long entryId) {
		if (connection == null) {
			return;
		}
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_ALL_BY_ID_FILEID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
				ps.setLong(1, containerId);
				ps.setLong(2, entryId);
				try (ResultSet result = ps.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						LOGGER.debug("Adding container entry {} for {} ", entryId, containerId);
						result.moveToInsertRow();
						result.updateLong(COL_CONTAINER_ID, containerId);
						result.updateLong(COL_FILEID, entryId);
						result.insertRow();
					} else {
						LOGGER.trace("Container entry {} for {} already exists", entryId, containerId);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Cannot add container entry {} for {} ", entryId, containerId, e);
		}
	}

	protected static List<Long> getContainerFileIds(final Connection connection, Long containerId) {
		if (connection == null || containerId == null) {
			return List.of();
		}
		List<Long> result = new ArrayList<>();
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_BY_ID)) {
			stmt.setLong(1, containerId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) {
					result.add(toLong(rs, COL_FILEID));
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", containerId, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	protected static Boolean isInContainer(final Connection connection, Long fileId) {
		if (connection == null || fileId == null) {
			return null;
		}
		try (PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_BY_FILEID)) {
			stmt.setLong(1, fileId);
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return false;
	}

}
