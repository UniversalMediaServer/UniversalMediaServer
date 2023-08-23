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
import java.sql.Statement;
import java.sql.Timestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.sql.PreparedStatement;
import java.util.HashSet;
import java.util.Set;
import net.pms.Messages;
import net.pms.dlna.MediaMonitor;
import net.pms.gui.GuiManager;
import net.pms.media.MediaStatus;

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
	private static final int TABLE_VERSION = 13;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_ID = "ID";
	private static final String COL_BOOKMARK = "BOOKMARK";
	private static final String COL_FILENAME = "FILENAME";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_ISFULLYPLAYED = "ISFULLYPLAYED";
	private static final String COL_PLAYCOUNT = "PLAYCOUNT";
	private static final String COL_DATELASTPLAY = "DATELASTPLAY";
	private static final String COL_LASTPLAYBACKPOSITION = "LASTPLAYBACKPOSITION";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_BOOKMARK = TABLE_NAME + "." + COL_BOOKMARK;
	public static final String TABLE_COL_FILENAME = TABLE_NAME + "." + COL_FILENAME;
	public static final String TABLE_COL_ISFULLYPLAYED = TABLE_NAME + "." + COL_ISFULLYPLAYED;
	public static final String TABLE_COL_PLAYCOUNT = TABLE_NAME + "." + COL_PLAYCOUNT;
	public static final String TABLE_COL_DATELASTPLAY = TABLE_NAME + "." + COL_DATELASTPLAY;

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ALL = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_BOOKMARK = SELECT + TABLE_COL_BOOKMARK + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ISFULLYPLAYED = SELECT + TABLE_COL_ISFULLYPLAYED + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_DELETE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_LIKE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + PARAMETER;

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
					try (Statement statement = connection.createStatement()) {
						statement.execute(ALTER_TABLE + TABLE_NAME + ADD + COL_FILENAME + VARCHAR_1024);
						statement.execute(ALTER_TABLE + TABLE_NAME + ADD + CONSTRAINT + "FILES_FILENAME_UNIQUE UNIQUE(FILENAME)");

						Set<String> fileStatusEntries = new HashSet<>();
						try (PreparedStatement stmt = connection.prepareStatement("SELECT " + MediaTableFiles.TABLE_COL_ID + " AS FILES_ID, " + MediaTableFiles.TABLE_NAME + ".FILENAME AS FILES_FILENAME FROM " + MediaTableFiles.TABLE_NAME + " LEFT JOIN " + TABLE_NAME + " ON " + MediaTableFiles.TABLE_NAME + ".ID = " + TABLE_NAME + ".FILEID");
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

						statement.execute(DELETE_FROM + TABLE_NAME + WHERE + COL_FILENAME + IS_NULL);
						statement.execute(ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + COL_FILENAME + SET + "NOT NULL");
						statement.execute("DROP INDEX IF EXISTS FILEID_IDX");
						statement.execute(ALTER_TABLE + TABLE_NAME + " DROP COLUMN FILEID");
						statement.execute(CREATE_INDEX + COL_FILENAME + "_IDX" + ON + TABLE_NAME + "(" + COL_FILENAME + ")");
					}
					version = 2;
				}
				case 2, 3 -> {
					// From version 2 to 3, we added an index for the ISFULLYPLAYED column
					// From version 3 to 4, we make sure the previous index was created correctly
					try (Statement statement = connection.createStatement()) {
						statement.execute("DROP INDEX IF EXISTS ISFULLYPLAYED_IDX");
						statement.execute("CREATE INDEX ISFULLYPLAYED_IDX ON " + TABLE_NAME + "(ISFULLYPLAYED)");
					}
					version = 4;
				}
				case 4, 5, 6, 7 -> {
					// From version 7 to 8, we undo our referential integrity attempt that kept going wrong
					String sql;
					ResultSet rs = connection.getMetaData().getTables(null, "INFORMATION_SCHEMA", "TABLE_CONSTRAINTS", null);
					if (rs.next()) {
						sql = "SELECT CONSTRAINT_NAME " +
								"FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
								"WHERE TABLE_NAME = '" + TABLE_NAME + "' AND CONSTRAINT_TYPE = 'FOREIGN KEY' OR CONSTRAINT_TYPE = 'REFERENTIAL'";
					} else {
						sql = "SELECT CONSTRAINT_NAME " +
								"FROM INFORMATION_SCHEMA.CONSTRAINTS " +
								"WHERE TABLE_NAME = '" + TABLE_NAME + "' AND CONSTRAINT_TYPE = 'REFERENTIAL'";
					}

					PreparedStatement stmt = connection.prepareStatement(sql);
					rs = stmt.executeQuery();

					while (rs.next()) {
						try (Statement statement = connection.createStatement()) {
							statement.execute(ALTER_TABLE + TABLE_NAME + " DROP CONSTRAINT IF EXISTS " + rs.getString("CONSTRAINT_NAME"));
						}
					}

					stmt = connection.prepareStatement(sql);
					rs = stmt.executeQuery();

					if (rs.next()) {
						throw new SQLException("The upgrade from v7 to v8 failed to remove the old constraints");
					}
					close(rs);
					close(stmt);

					version = 8;
				}
				case 8 -> {
					try (Statement statement = connection.createStatement()) {
						statement.execute(ALTER_TABLE + TABLE_NAME + " ADD BOOKMARK INTEGER DEFAULT 0");
					}
					version = 9;
				}
				case 9 -> {
					try (Statement statement = connection.createStatement()) {
						statement.execute(ALTER_TABLE + TABLE_NAME + " ADD DATELASTPLAY  TIMESTAMP");
						statement.execute(ALTER_TABLE + TABLE_NAME + " ADD PLAYCOUNT     INTEGER DEFAULT 0");
					}
					version = 10;
				}
				case 10, 11 -> {
					try (Statement statement = connection.createStatement()) {
						if (!isColumnExist(connection, TABLE_NAME, COL_LASTPLAYBACKPOSITION)) {
							statement.execute(ALTER_TABLE + TABLE_NAME + " ADD LASTPLAYBACKPOSITION DOUBLE PRECISION DEFAULT 0.0");
						}
					} catch (SQLException e) {
						LOGGER.error(LOG_UPGRADING_TABLE_FAILED, DATABASE_NAME, TABLE_NAME, e.getMessage());
						LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
						throw new SQLException(e);
					}
					version = 12;
				}
				case 12 -> {
					//rename indexes COL_FILENAME + "_IDX"
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_FILENAME + IDX_MARKER + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER);
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_ISFULLYPLAYED + IDX_MARKER + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ISFULLYPLAYED + IDX_MARKER);
					version = 13;
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
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                     + IDENTITY              + PRIMARY_KEY      + COMMA +
				COL_FILENAME               + VARCHAR_1024          + NOT_NULL         + COMMA +
				COL_MODIFIED               + TIMESTAMP                                + COMMA +
				COL_ISFULLYPLAYED          + BOOLEAN               + DEFAULT + FALSE  + COMMA +
				COL_BOOKMARK               + INTEGER               + DEFAULT_0        + COMMA +
				COL_DATELASTPLAY           + TIMESTAMP                                + COMMA +
				COL_PLAYCOUNT              + INTEGER               + DEFAULT_0        + COMMA +
				COL_LASTPLAYBACKPOSITION   + DOUBLE_PRECISION      + DEFAULT + "0.0"  +
			")",
			CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + IDX_MARKER + ON + TABLE_NAME + "(" + COL_FILENAME + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_ISFULLYPLAYED + IDX_MARKER + ON + TABLE_NAME + "(" + COL_ISFULLYPLAYED + ")"
		);
	}

	/**
	 * Sets whether the file has been fully played.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param isFullyPlayed
	 */
	public static void setFullyPlayed(final Connection connection, final String fullPathToFile, final boolean isFullyPlayed) {
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
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
	public static void setLastPlayed(final Connection connection, final String fullPathToFile, final Double lastPlaybackPosition) {
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
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

	/**
	 * Sets whether each file within the folder is fully played.
	 *
	 * @param connection the db connection
	 * @param fullPathToFolder the full path to the folder.
	 * @param isFullyPlayed whether to mark the folder content as fully played
	 *            or not fully played.
	 */
	public static void setDirectoryFullyPlayed(final Connection connection, final String fullPathToFolder, final boolean isFullyPlayed) {
		String statusLineString = isFullyPlayed ? Messages.getString("MarkContentsFullyPlayed") : Messages.getString("MarkContentsUnplayed");
		GuiManager.setStatusLine(statusLineString + ": " + fullPathToFolder);

		try {
			for (String fullPathToFile : MediaTableFiles.getFilenamesInFolder(connection, fullPathToFolder)) {
				MediaMonitor.setFullyPlayed(fullPathToFile, isFullyPlayed, null);
			}
		} finally {
			GuiManager.setStatusLine(null);
		}
	}

	/**
	 * Removes an entry or entries based on its FILENAME. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see Tables#sqlLikeEscape(String)
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

	public static Boolean isFullyPlayed(final Connection connection, final String fullPathToFile) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ISFULLYPLAYED)) {
				statement.setString(1, fullPathToFile);
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

	public static int getBookmark(final Connection connection, final String fullPathToFile) {
		boolean trace = LOGGER.isTraceEnabled();
		int result = 0;

		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_BOOKMARK)) {
				statement.setString(1, fullPathToFile);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", statement);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						result = resultSet.getInt(COL_BOOKMARK);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "looking up file bookmark", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
		return result;
	}

	public static void setBookmark(final Connection connection, final String fullPathToFile, final int bookmark) {
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ALL, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				statement.setString(1, fullPathToFile);
				if (trace) {
					LOGGER.trace("Searching for file in {} with \"{}\" before setBookmark", TABLE_NAME, statement);
				}
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
						result.updateInt(COL_BOOKMARK, bookmark);
						result.updateRow();
						if (trace) {
							LOGGER.trace("Updating existing bookmark in {}: \"{}\" ", TABLE_NAME, bookmark);
						}
					} else {
						result.moveToInsertRow();
						result.updateString(COL_FILENAME, fullPathToFile);
						result.updateTimestamp(COL_MODIFIED, new Timestamp(System.currentTimeMillis()));
						result.updateBoolean(COL_ISFULLYPLAYED, false);
						result.updateInt(COL_BOOKMARK, bookmark);
						result.insertRow();
						if (trace) {
							LOGGER.trace("Inserting bookmark in {}: \"{}\" ", TABLE_NAME, bookmark);
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN_FOR, DATABASE_NAME, "writing bookmark", bookmark, TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Gets a row of {@link MediaDatabase} from the database and returns it
	 * as a {@link MediaStatus} instance.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @return The {@link MediaStatus} instance matching {@code name}.
	 */
	public static MediaStatus getData(final Connection connection, String fullPathToFile) {
		try (
			PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL);
		) {
			stmt.setString(1, fullPathToFile);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					MediaStatus media = new MediaStatus();
					media.setPlaybackCount(rs.getInt(COL_PLAYCOUNT));
					media.setLastPlaybackTime(rs.getString(COL_DATELASTPLAY));
					media.setLastPlaybackPosition(rs.getDouble(COL_LASTPLAYBACKPOSITION));
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting data", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

}
