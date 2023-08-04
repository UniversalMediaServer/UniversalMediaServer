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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.sql.Statement;
import net.pms.dlna.DLNAThumbnail;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * This class is responsible for managing the Thumbnails table. It
 * does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author SubJunk & Nadahar
 * @since 7.1.1
 */
public final class MediaTableThumbnails extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableThumbnails.class);
	public static final String TABLE_NAME = "THUMBNAILS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable()}
	 */
	private static final int TABLE_VERSION = 1;

	/**
	 * COLUMNS NAMES
	 */
	protected static final String COL_THUMBNAIL = "THUMBNAIL";
	private static final String COL_ID = "ID";
	private static final String COL_MD5 = "MD5";
	private static final String COL_MODIFIED = "MODIFIED";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_THUMBNAIL = TABLE_NAME + "." + COL_THUMBNAIL;
	private static final String TABLE_COL_MD5 = TABLE_NAME + "." + COL_MD5;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ID_MD5 = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_MD5 + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_INSERT_ID_MD5 = INSERT_INTO + TABLE_NAME + " (" + COL_THUMBNAIL + COMMA + COL_MODIFIED + COMMA + COL_MD5 + ") VALUES (" + PARAMETER + COMMA + PARAMETER + COMMA + PARAMETER + ")";
	private static final String SQL_DELETE_ID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;

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
	@SuppressFBWarnings("IIL_PREPARE_STATEMENT_IN_LOOP")
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					//nothing to do
				}
				default -> throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                + IDENTITY                       + COMMA +
				COL_THUMBNAIL         + OTHER      + NOT_NULL          + COMMA +
				COL_MODIFIED          + TIMESTAMP                      + COMMA +
				COL_MD5               + VARCHAR    + UNIQUE_NOT_NULL   +
			")"
		);
	}

	/**
	 * Attempts to find a thumbnail in this table by MD5 hash. If not found,
	 * it writes the new thumbnail to this table.
	 * Finally, it writes the ID from this table as the THUMBID in the FILES
	 * table.
	 *
	 * @param thumbnail
	 * @param fullPathToFile
	 * @param tvSeriesID
	 */
	public static void setThumbnail(final DLNAThumbnail thumbnail, final String fullPathToFile, final long tvSeriesID) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				//handle autocommit
				boolean currentAutoCommit = connection.getAutoCommit();
				if (currentAutoCommit) {
					connection.setAutoCommit(false);
				}
				setThumbnail(connection, thumbnail, fullPathToFile, tvSeriesID, false);
				if (currentAutoCommit) {
					connection.commit();
					connection.setAutoCommit(true);
				}
			}
		} catch (SQLException e) {
			LOGGER.trace("", e);
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Attempts to find a thumbnail in this table by MD5 hash. If not found,
	 * it writes the new thumbnail to this table.
	 * Finally, it writes the ID from this table as the THUMBID in the FILES
	 * or TVSERIES table.
	 *
	 * @param connection the db connection
	 * @param thumbnail
	 * @param fullPathToFile
	 * @param tvSeriesID
	 * @param forceNew whether to use a new thumbnail and remove any existing match
	 *                 introduced to fix unrecoverable serialization
	 */
	public static void setThumbnail(final Connection connection, final DLNAThumbnail thumbnail, final String fullPathToFile, final long tvSeriesID, final boolean forceNew) {
		if (fullPathToFile == null && tvSeriesID == -1) {
			LOGGER.trace("Either fullPathToFile or tvSeriesID are required for setThumbnail, returning early");
			return;
		}

		String md5Hash = DigestUtils.md5Hex(thumbnail.getBytes(false));

		try {
			Integer existingId = null;
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ID_MD5)) {
				statement.setString(1, md5Hash);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						existingId = resultSet.getInt(COL_ID);
						if (!forceNew) {
							if (fullPathToFile != null) {
								LOGGER.trace("Found existing thumbnail with ID {} in {}, setting the THUMBID in the FILES table", existingId, TABLE_NAME);
								MediaTableFiles.updateThumbnailId(connection, fullPathToFile, existingId);
							} else {
								LOGGER.trace("Found existing thumbnail with ID {} in {}, setting the THUMBID in the {} table", existingId, TABLE_NAME, MediaTableTVSeries.TABLE_NAME);
								MediaTableTVSeries.updateThumbnailId(connection, tvSeriesID, existingId);
							}
						}
					}
				}

				if (existingId == null || forceNew) {
					if (existingId == null) {
						LOGGER.trace("Thumbnail \"{}\" not found in {}", md5Hash, TABLE_NAME);
					} else {
						LOGGER.trace("Forcing new thumbnail \"{}\" in {}, deleting thumbnail with ID {}", md5Hash, TABLE_NAME, existingId);
						removeById(connection, existingId);
					}

					try (PreparedStatement insertStatement = connection.prepareStatement(SQL_INSERT_ID_MD5, Statement.RETURN_GENERATED_KEYS)) {
						insertStatement.setObject(1, thumbnail);
						insertStatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
						insertStatement.setString(3, md5Hash);
						insertStatement.executeUpdate();

						try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
							if (generatedKeys.next()) {
								if (fullPathToFile != null) {
									LOGGER.trace("Inserting new thumbnail with ID {}, setting the THUMBID in the FILES table", generatedKeys.getInt(1));
									MediaTableFiles.updateThumbnailId(connection, fullPathToFile, generatedKeys.getInt(1));
								} else {
									LOGGER.trace("Inserting new thumbnail with ID {} in {}, setting the THUMBID in the {} table", generatedKeys.getInt(1), TABLE_NAME, MediaTableTVSeries.TABLE_NAME);
									MediaTableTVSeries.updateThumbnailId(connection, tvSeriesID, generatedKeys.getInt(1));
								}
							} else {
								LOGGER.trace("Generated key not returned in " + TABLE_NAME);
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN_FOR, DATABASE_NAME, "writing md5", md5Hash, TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes an entry or entries based on its ID.
	 *
	 * @param connection the db connection
	 * @param id the ID to remove
	 */
	public static void removeById(final Connection connection, final Integer id) {
		try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE_ID)) {
			statement.setInt(1, id);
			int rows = statement.executeUpdate();
			LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for ID \"{}\"", rows, id);
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entries", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
