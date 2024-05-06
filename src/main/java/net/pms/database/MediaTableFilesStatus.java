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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import net.pms.media.MediaStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the FilesStatus table. It
 * does everything from creating, checking and upgrading the table to
 * performing lookups, updates and inserts. All operations involving this table
 * shall be done with this class.
 *
 * @author SubJunk & Nadahar
 * @since 7.0.0
 */
public final class MediaTableFilesStatus extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableFilesStatus.class);
	public static final String TABLE_NAME = "FILES_STATUS";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 14;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_USERID = "USERID";
	private static final String COL_FILENAME = "FILENAME";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_BOOKMARK = "BOOKMARK";
	private static final String COL_ISFULLYPLAYED = "ISFULLYPLAYED";
	private static final String COL_PLAYCOUNT = "PLAYCOUNT";
	private static final String COL_DATELASTPLAY = "DATELASTPLAY";
	private static final String COL_LASTPLAYBACKPOSITION = "LASTPLAYBACKPOSITION";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_FILENAME = TABLE_NAME + "." + COL_FILENAME;
	public static final String TABLE_COL_USERID = TABLE_NAME + "." + COL_USERID;
	public static final String TABLE_COL_ISFULLYPLAYED = TABLE_NAME + "." + COL_ISFULLYPLAYED;
	public static final String TABLE_COL_PLAYCOUNT = TABLE_NAME + "." + COL_PLAYCOUNT;
	public static final String TABLE_COL_DATELASTPLAY = TABLE_NAME + "." + COL_DATELASTPLAY;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_USERID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ISFULLYPLAYED = SELECT + TABLE_COL_ISFULLYPLAYED + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_USERID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_MOVED = SELECT + PARAMETER + COMMA + COL_USERID + COMMA + COL_BOOKMARK + COMMA + COL_ISFULLYPLAYED + COMMA + COL_PLAYCOUNT + COMMA + COL_DATELASTPLAY + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_GET_USER = SELECT + TABLE_COL_FILENAME + COMMA + PARAMETER + COMMA + COL_BOOKMARK + COMMA + COL_ISFULLYPLAYED + COMMA + COL_PLAYCOUNT + COMMA + COL_DATELASTPLAY + FROM + TABLE_NAME + WHERE + TABLE_COL_USERID + EQUAL + PARAMETER;
	private static final String SQL_DELETE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_LIKE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + LIKE_STARTING_WITH_PARAMETER;
	private static final String SQL_DELETE_USER = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_USERID + EQUAL + PARAMETER;
	private static final String SQL_INSERT_MOVED = INSERT_INTO + TABLE_NAME + "(" + COL_FILENAME + COMMA + COL_USERID + COMMA + COL_BOOKMARK + COMMA + COL_ISFULLYPLAYED + COMMA + COL_PLAYCOUNT + COMMA + COL_DATELASTPLAY + ") " + SQL_GET_MOVED;
	private static final String SQL_INSERT_USERCOPY = INSERT_INTO + TABLE_NAME + "(" + COL_FILENAME + COMMA + COL_USERID + COMMA + COL_BOOKMARK + COMMA + COL_ISFULLYPLAYED + COMMA + COL_PLAYCOUNT + COMMA + COL_DATELASTPLAY + ") " + SQL_GET_USER;

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
					// From version 1 to 2, we stopped using FILEID and instead use FILENAME directly
					if (isColumnExist(connection, TABLE_NAME, "FILEID")) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COL_FILENAME + VARCHAR_1024);
						Set<String> fileStatusEntries = new HashSet<>();
						try (PreparedStatement stmt = connection.prepareStatement(SELECT + MediaTableFiles.TABLE_COL_ID + AS + "FILES_ID" + COMMA + TABLE_COL_FILENAME + AS + "FILES_FILENAME" + FROM + MediaTableFiles.TABLE_NAME + LEFT_JOIN + TABLE_NAME + ON + MediaTableFiles.TABLE_COL_ID + EQUAL + TABLE_NAME + ".FILEID");
									ResultSet rs = stmt.executeQuery()) {
							String filename;
							while (rs.next()) {
								filename = rs.getString("FILES_FILENAME");

								// Ensure we don't attempt add the same filename twice
								if (!fileStatusEntries.contains(filename)) {
									fileStatusEntries.add(filename);
									String query = UPDATE + TABLE_NAME + SET + COL_FILENAME + EQUAL + sqlQuote(filename) + WHERE + "FILEID" + EQUAL + rs.getInt("FILES_ID");
									try (Statement statement2 = connection.createStatement()) {
										statement2.execute(query);
									}
									LOGGER.info("Updating fully played entry for " + filename);
								}
							}
						}
					}
					executeUpdate(connection, DELETE_FROM + TABLE_NAME + WHERE + COL_FILENAME + IS_NULL);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_FILENAME + SET + NOT_NULL);
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + "FILEID" + IDX_MARKER);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "FILEID");
					executeUpdate(connection, CREATE_UNIQUE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER + ON + TABLE_NAME + "(" + COL_FILENAME + ")");
				}
				case 2 -> {
					// From version 2 to 3, we added an index for the ISFULLYPLAYED column
					// see 3
				}
				case 3 -> {
					// From version 3 to 4, we make sure the previous index was created correctly
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + COL_ISFULLYPLAYED + IDX_MARKER);
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ISFULLYPLAYED + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ISFULLYPLAYED + ")");
				}
				case 4, 5, 6 -> {
					// superseed
				}
				case 7 -> {
					// From version 7 to 8, we undo our referential integrity attempt that kept going wrong
					dropReferentialsConstraint(connection, TABLE_NAME);
					//check if was deleted or errored
					if (dropReferentialsConstraint(connection, TABLE_NAME)) {
						throw new SQLException("The upgrade from v7 to v8 failed to remove the old constraints");
					}
				}
				case 8 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_BOOKMARK + INTEGER + DEFAULT_0);
				}
				case 9 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_DATELASTPLAY + TIMESTAMP);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_PLAYCOUNT + INTEGER + DEFAULT_0);
				}
				case 10 -> {
					// superseed
				}
				case 11 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_LASTPLAYBACKPOSITION + DOUBLE_PRECISION + DEFAULT_0D);
				}
				case 12 -> {
					//rename indexes COL_FILENAME + "_IDX"
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_FILENAME + IDX_MARKER + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_ISFULLYPLAYED + IDX_MARKER + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ISFULLYPLAYED + IDX_MARKER);
				}
				case 13 -> {
					//add userid + index
					dropUniqueConstraint(connection, TABLE_NAME, COL_FILENAME);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_USERID + INTEGER + DEFAULT_0);
					executeUpdate(connection, CREATE_UNIQUE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_USERID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_FILENAME + COMMA + COL_USERID + ")");
				}
				default -> throw new IllegalStateException(
					getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
				);
			}
		}

		try {
			MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
		} catch (SQLException e) {
			LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                     + IDENTITY              + PRIMARY_KEY      + COMMA +
				COL_FILENAME               + VARCHAR_1024          + NOT_NULL         + COMMA +
				COL_USERID                 + INTEGER               + DEFAULT_0        + COMMA +
				COL_MODIFIED               + TIMESTAMP                                + COMMA +
				COL_ISFULLYPLAYED          + BOOLEAN               + DEFAULT + FALSE  + COMMA +
				COL_BOOKMARK               + INTEGER               + DEFAULT_0        + COMMA +
				COL_DATELASTPLAY           + TIMESTAMP                                + COMMA +
				COL_PLAYCOUNT              + INTEGER               + DEFAULT_0        + COMMA +
				COL_LASTPLAYBACKPOSITION   + DOUBLE_PRECISION      + DEFAULT_0D       +
			")",
			CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_USERID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_FILENAME + COMMA + COL_USERID + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ISFULLYPLAYED + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ISFULLYPLAYED + ")"
		);
	}

	/**
	 * Gets a row of {@link MediaDatabase} from the database and returns it
	 * as a {@link MediaStatus} instance.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @return The {@link MediaStatus} instance matching {@code name}.
	 */
	public static MediaStatus getMediaStatus(final Connection connection, final String fullPathToFile, final int userId) {
		try (
			PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL);
		) {
			statement.setString(1, fullPathToFile);
			statement.setInt(2, userId);
			try (ResultSet rs = statement.executeQuery()) {
				if (rs.next()) {
					MediaStatus media = new MediaStatus();
					media.setFullyPlayed(rs.getBoolean(COL_ISFULLYPLAYED));
					media.setPlaybackCount(rs.getInt(COL_PLAYCOUNT));
					media.setLastPlaybackTime(rs.getString(COL_DATELASTPLAY));
					media.setLastPlaybackPosition(rs.getDouble(COL_LASTPLAYBACKPOSITION));
					media.setBookmark(rs.getInt(COL_BOOKMARK));
					return media;
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting data", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Sets whether the file has been fully played.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param isFullyPlayed
	 */
	public static void setFullyPlayed(final Connection connection, final String fullPathToFile, final int userId, final boolean isFullyPlayed) {
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
				statement.setInt(2, userId);
				if (trace) {
					LOGGER.trace("Searching for file in " + TABLE_NAME + " with \"{}\" before setFullyPlayed", statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						if (result.getBoolean(COL_ISFULLYPLAYED) == isFullyPlayed) {
							if (trace) {
								LOGGER.trace("Found file entry in " + TABLE_NAME + " and it already has ISFULLYPLAYED set to {}", result.getBoolean(COL_ISFULLYPLAYED));
							}
						} else {
							if (trace) {
								LOGGER.trace(
									"Found file entry \"{}\" in " + TABLE_NAME + "; setting ISFULLYPLAYED to {}",
									fullPathToFile,
									isFullyPlayed
								);
							}
							result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
							result.updateBoolean(COL_ISFULLYPLAYED, isFullyPlayed);
							result.updateRow();
						}
					} else {
						if (trace) {
							LOGGER.trace(
								"File entry \"{}\" not found in " + TABLE_NAME + ", inserting new row with ISFULLYPLAYED set to {}",
								fullPathToFile,
								isFullyPlayed
							);
						}
						result.moveToInsertRow();
						result.updateString(COL_FILENAME, fullPathToFile);
						result.updateInt(COL_USERID, userId);
						result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
						result.updateBoolean(COL_ISFULLYPLAYED, isFullyPlayed);
						result.insertRow();
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN_FOR, DATABASE_NAME, "writing status", isFullyPlayed, TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Sets the last played date and increments the play count.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param lastPlaybackPosition how many seconds were played
	 */
	public static void setLastPlayed(final Connection connection, final String fullPathToFile, final int userId, final Double lastPlaybackPosition) {
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
				statement.setInt(2, userId);
				if (trace) {
					LOGGER.trace("Searching for file in " + TABLE_NAME + " with \"{}\" before setLastPlayed", statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					int playCount;
					boolean isCreatingNewRecord = !result.next();

					if (isCreatingNewRecord) {
						playCount = 1;
						result.moveToInsertRow();
						result.updateString(COL_FILENAME, fullPathToFile);
						result.updateInt(COL_USERID, userId);
					} else {
						playCount = result.getInt(COL_PLAYCOUNT) + 1;
					}

					result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
					result.updateTimestamp(COL_DATELASTPLAY, new Timestamp(System.currentTimeMillis()));
					result.updateInt(COL_PLAYCOUNT, playCount);
					if (lastPlaybackPosition != null) {
						result.updateDouble(COL_LASTPLAYBACKPOSITION, lastPlaybackPosition);
					}

					if (isCreatingNewRecord) {
						result.insertRow();
					} else {
						result.updateRow();
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing last played date", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static void setBookmark(final Connection connection, final String fullPathToFile, final int userId, final int bookmark) {
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
				statement.setInt(2, userId);
				if (trace) {
					LOGGER.trace("Searching for file in {} with \"{}\" before setBookmark", TABLE_NAME, statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						result.moveToInsertRow();
						result.updateString(COL_FILENAME, fullPathToFile);
						result.updateInt(COL_USERID, userId);
						result.updateBoolean(COL_ISFULLYPLAYED, false);
					}
					result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
					result.updateInt(COL_BOOKMARK, bookmark);
					if (isCreatingNewRecord) {
						if (trace) {
							LOGGER.trace("Inserting bookmark in {}: \"{}\" ", TABLE_NAME, bookmark);
						}
						result.insertRow();
					} else {
						if (trace) {
							LOGGER.trace("Updating existing bookmark in {}: \"{}\" ", TABLE_NAME, bookmark);
						}
						result.updateRow();
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN_FOR, DATABASE_NAME, "writing bookmark", bookmark, TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes an entry or entries based on its FILENAME. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @param connection the db connection
	 * @param filename the filename to remove
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 */
	public static void remove(final Connection connection, final String filename, boolean useLike) {
		try {
			String sql = useLike ? SQL_DELETE_LIKE : SQL_DELETE;
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, filename);
				int rows = statement.executeUpdate();
				LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for filename \"{}\"", rows, filename);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entries", TABLE_NAME, filename, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Create a copy of entries from the old filename to the new filename.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile the old filename
	 * @param fullPathToNewFile the new filename
	 */
	public static void createCopyOnFileMoved(final Connection connection, final String fullPathToFile, final String fullPathToNewFile) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT_MOVED)) {
				statement.setString(1, fullPathToNewFile);
				statement.setString(2, fullPathToFile);
				int rows = statement.executeUpdate();
				LOGGER.trace("Copied entries {} in " + TABLE_NAME + " for filename \"{}\"", rows, fullPathToNewFile);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "Copying entries", TABLE_NAME, fullPathToNewFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	//used only from test class
	protected static Boolean isFullyPlayed(final Connection connection, final String fullPathToFile, final int userId) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ISFULLYPLAYED)) {
				statement.setString(1, fullPathToFile);
				statement.setInt(2, userId);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						return result.getBoolean(COL_ISFULLYPLAYED);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "looking up file status", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	public static void deleteUser(final Connection connection, int userId) {
		if (connection == null || userId < 1) {
			return;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE_USER)) {
			statement.setInt(1, userId);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error deleteUser:{}", e.getMessage());
		}
	}

	public static void copyUserEntries(final Connection connection, final int userId, final int userIdDest) {
		if (connection == null || userId < 0 || userIdDest < 1) {
			return;
		}
		//first ensure empty user entries (avoid duplicate)
		deleteUser(connection, userIdDest);
		try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT_USERCOPY)) {
			statement.setInt(1, userIdDest);
			statement.setInt(2, userId);
			statement.executeUpdate();
		} catch (SQLException e) {
			LOGGER.error("Error copyUserEntries:{}", e.getMessage());
		}
	}

}
