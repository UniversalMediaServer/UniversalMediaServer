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
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.pms.Messages;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.formats.Format;
import net.pms.gui.GuiManager;
import net.pms.image.ImageInfo;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides methods for creating and maintaining the database where
 * media information is stored. Scanning media and interpreting the data is
 * intensive, so the database is used to cache scanned information to be reused
 * later.
 */
public class MediaTableFiles extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableFiles.class);
	public static final String TABLE_NAME = "FILES";
	public static final String COL_ID = "ID";
	public static final String COL_THUMBID = "THUMBID";
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_FORMAT_TYPE = TABLE_NAME + ".FORMAT_TYPE";
	public static final String TABLE_COL_FILENAME = TABLE_NAME + ".FILENAME";
	public static final String TABLE_COL_MODIFIED = TABLE_NAME + ".MODIFIED";
	public static final String TABLE_COL_WIDTH = TABLE_NAME + ".WIDTH";
	public static final String TABLE_COL_HEIGHT = TABLE_NAME + ".HEIGHT";
	public static final String TABLE_COL_THUMBID = TABLE_NAME + "." + COL_THUMBID;
	public static final String TABLE_COL_STEREOSCOPY = TABLE_NAME + ".STEREOSCOPY";

	public static final String REFERENCE_TABLE_COL_ID = TABLE_NAME + "(" + COL_ID + ")";

	public static final String SQL_LEFT_JOIN_TABLE_FILES_STATUS = "LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON " + TABLE_COL_FILENAME + " = " + MediaTableFilesStatus.TABLE_COL_FILENAME + " ";
	public static final String SQL_LEFT_JOIN_TABLE_THUMBNAILS = "LEFT JOIN " + MediaTableThumbnails.TABLE_NAME + " ON " + TABLE_COL_THUMBID + " = " + MediaTableThumbnails.TABLE_COL_ID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA = "LEFT JOIN " + MediaTableVideoMetadata.TABLE_NAME + " ON " + TABLE_COL_ID + " = " + MediaTableVideoMetadata.TABLE_COL_FILEID + " ";

	private static final String INSERT_COLUMNS = "FILENAME, MODIFIED, FORMAT_TYPE, DURATION, BITRATE, WIDTH, HEIGHT, MEDIA_SIZE, CODECV, FRAMERATE, ASPECTRATIODVD, ASPECTRATIOCONTAINER, " +
		"ASPECTRATIOVIDEOTRACK, REFRAMES, AVCLEVEL, IMAGEINFO, CONTAINER, MUXINGMODE, FRAMERATEMODE, STEREOSCOPY, MATRIXCOEFFICIENTS, TITLECONTAINER, TITLEVIDEOTRACK, VIDEOTRACKCOUNT, " +
		"IMAGECOUNT, BITDEPTH, HDRFORMAT, PIXELASPECTRATIO, SCANTYPE, SCANORDER";

	private static final String SQL_GET_ID_FILENAME = "SELECT " + TABLE_COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILENAME + " = ? LIMIT 1";
	private static final String SQL_GET_ID_FILENAME_MODIFIED = "SELECT " + TABLE_COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILENAME + " = ? AND " + TABLE_COL_MODIFIED + " = ? LIMIT 1";
	private static final String SQL_GET_ALL_FILENAME_MODIFIED = "SELECT * FROM " + TABLE_NAME + " " + SQL_LEFT_JOIN_TABLE_THUMBNAILS + " WHERE " + TABLE_COL_FILENAME + " = ? AND " + TABLE_COL_MODIFIED + " = ? LIMIT 1";
	private static final String SQL_GET_INSERT_COLUMNS_BY_FILENAME = "SELECT " + TABLE_COL_ID + ", " + INSERT_COLUMNS + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILENAME + " = ? LIMIT 1";
	private static final String SQL_INSERT_COLUMNS = "INSERT INTO " + TABLE_NAME + " (" + INSERT_COLUMNS + ")" + createDefaultValueForInsertStatement(INSERT_COLUMNS);

	public static final String NONAME = "###";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 *
	 * Version notes:
	 * - 18: Introduced ALBUMARTIST field
	 * - 19: Introduced EXTERNALFILE and CHARSET fields
	 *       Released in versions 8.0.0-a1 and a2
	 * - 20: No db changes, bumped version because a parsing bug was fixed
	 *       Released in version 8.0.0-b1
	 * - 21: No db changes, bumped version because a parsing bug was fixed
	 *       Released in version 8.0.0
	 * - 22: No db changes, bumped version because h2database was reverted
	 *       to 1.4.196 because 1.4.197 broke audio metadata being
	 *       inserted/updated
	 * - 23: Store aspect ratios as strings again
	 * - 24: Added VERSION column to FILES table which keeps track of which
	 *       API metadata version is saved for that file
	 * - 25: Renamed columns to avoid reserved SQL and H2 keywords (part of
	 *       updating H2Database to v2)
	 * - 26: No db changes, improved filename parsing
	 * - 27: Added many columns for TMDB information
	 * - 28: No db changes, clear database metadata to populate new columns
	 * - 29-30: No db changes, improved filename parsing
	 * - 31: Redo the changes from version 27 because the versioning got muddled
	 * - 32: Added an index for the Media Library Movies folder that includes duration
	 * - 33: Added HDRFORMAT column
	 */
	private static final int TABLE_VERSION = 34;

	// Database column sizes
	private static final int SIZE_CODECV = 32;
	private static final int SIZE_FRAMERATE = 32;
	private static final int SIZE_AVCLEVEL = 3;
	private static final int SIZE_CONTAINER = 32;
	private static final int SIZE_MATRIX_COEFFICIENTS = 16;
	private static final int SIZE_MUXINGMODE = 32;
	private static final int SIZE_FRAMERATEMODE = 16;

	/*
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
				// Moving sql from DLNAMediaDatabase to this class.
				version = 24;
			}
			if (version < TABLE_VERSION) {
				upgradeTable(connection, version);
			} else if (version > TABLE_VERSION) {
				LOGGER.warn(LOG_TABLE_NEWER_VERSION, DATABASE_NAME, TABLE_NAME);
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
	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		boolean force = false;
		try {
			for (int version = currentVersion; version < TABLE_VERSION; version++) {
				LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
				switch (version) {
					case 23 -> {
						try (Statement statement = connection.createStatement()) {
							/*
							* Since the last release, 10.12.0, we fixed some bugs with TV episode filename parsing
							* so here we clear any cached data for non-episodes.
							*/
							StringBuilder sb = new StringBuilder();
							sb
								.append("UPDATE ")
									.append(TABLE_NAME).append(" ")
								.append("SET ")
									.append("IMDBID = NULL, ")
									.append("MEDIA_YEAR = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL ")
								.append("WHERE ")
									.append("NOT ISTVEPISODE");
							statement.execute(sb.toString());

						}
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 24 -> {
						//Rename sql reserved words
						LOGGER.trace("Deleting index TYPE_ISTV");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV");
						LOGGER.trace("Deleting index TYPE_ISTV_SIMPLENAME");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_SIMPLENAME");
						LOGGER.trace("Deleting index TYPE_ISTV_NAME");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_NAME");
						LOGGER.trace("Deleting index TYPE_ISTV_NAME_SEASON");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_NAME_SEASON");
						LOGGER.trace("Deleting index TYPE_ISTV_YEAR_STEREOSCOPY");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_ISTV_YEAR_STEREOSCOPY");
						LOGGER.trace("Deleting index TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_WIDTH_HEIGHT");
						LOGGER.trace("Deleting index TYPE_MODIFIED");
						executeUpdate(connection, "DROP INDEX IF EXISTS TYPE_MODIFIED");
						if (isColumnExist(connection, TABLE_NAME, "TYPE")) {
							LOGGER.trace("Renaming column name TYPE to FORMAT_TYPE");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `TYPE` RENAME TO FORMAT_TYPE");
						}
						if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
							LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
						}
						if (isColumnExist(connection, TABLE_NAME, "SIZE")) {
							LOGGER.trace("Renaming column name SIZE to MEDIA_SIZE");
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `SIZE` RENAME TO MEDIA_SIZE");
						}
						LOGGER.trace("Creating index FORMAT_TYPE");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE on " + TABLE_NAME + " (FORMAT_TYPE)");
						LOGGER.trace("Creating index FORMAT_TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_WIDTH_HEIGHT on " + TABLE_NAME + " (FORMAT_TYPE, WIDTH, HEIGHT)");
						LOGGER.trace("Creating index FORMAT_TYPE_MODIFIED");
						executeUpdate(connection, "CREATE INDEX FORMAT_TYPE_MODIFIED on " + TABLE_NAME + " (FORMAT_TYPE, MODIFIED)");
					}
					case 25 -> {
						try (Statement statement = connection.createStatement()) {
							/*
							* Since the last release, 10.15.0, we fixed some bugs with TV episode and sport
							* filename parsing so here we clear any cached data for non-episodes.
							*/
							StringBuilder sb = new StringBuilder();
							sb
								.append("UPDATE ")
									.append(TABLE_NAME).append(" ")
								.append("SET ")
									.append("IMDBID = NULL, ")
									.append("MEDIA_YEAR = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL ")
								.append("WHERE ")
									.append("NOT ISTVEPISODE");
							statement.execute(sb.toString());
						}

						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 26 -> {
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 27 -> {
						// This version was for testing, left here to not break tester dbs
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 28 -> {
						// This didn't work and was fixed in the next version
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 29 -> {
						try (Statement statement = connection.createStatement()) {
							/*
							* Since the last release, 10.21.0.1, we fixed some bugs with miniseries
							* filename parsing so here we clear any cached data for potential miniseries.
							*/
							StringBuilder sb = new StringBuilder();
							sb
								.append("UPDATE ")
									.append(TABLE_NAME).append(" ")
								.append("SET ")
									.append("IMDBID = NULL, ")
									.append("MEDIA_YEAR = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL ")
								.append("WHERE ")
									.append("FILENAME REGEXP '[0-9]of[0-9]'");
							statement.execute(sb.toString());
						}
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 30 -> {
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 31 -> {
						executeUpdate(connection, "DROP INDEX IF EXISTS FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY");
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 32 -> {
						//ensure MediaTableVideoMetadatas is created
						MediaTableVideoMetadata.checkTable(connection);

						//drop all metadata indexes
						String[] indexes = {
							"IDX_FILENAME_MODIFIED_IMDBID",
							"FILENAME_MODIFIED_VERSION_IMDBID",
							"FORMAT_TYPE_ISTV",
							"FORMAT_TYPE_ISTV_NAME",
							"FORMAT_TYPE_ISTV_SIMPLENAME",
							"FORMAT_TYPE_ISTV_NAME_SEASON",
							"FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY",
							"FORMAT_TYPE_ISTV_YEAR_DURATION_STEREOSCOPY"
						};
						for (String index : indexes)  {
							executeUpdate(connection, "DROP INDEX IF EXISTS " + index);
						}

						//set old json datas to be rescanned
						if (isColumnExist(connection, TABLE_NAME, "VERSION")) {
							String[] badJsonColumns = {"CREDITS", "EXTERNALIDS", "PRODUCTIONCOMPANIES", "PRODUCTIONCOUNTRIES"};
							for (String badJsonColumn : badJsonColumns) {
								if (isColumnExist(connection, TABLE_NAME, badJsonColumn)) {
									executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET IMDBID = NULL WHERE RIGHT(" + badJsonColumn + ", 1) = ','");
								}
							}
						}

						//move datas
						String[] columns = {"IMDBID", "MEDIA_YEAR", "MOVIEORSHOWNAME", "MOVIEORSHOWNAMESIMPLE", "TVSEASON", "TVEPISODENUMBER", "TVEPISODENAME", "ISTVEPISODE", "EXTRAINFORMATION", "VERSION",
							"BUDGET", "CREDITS", "EXTERNALIDS", "HOMEPAGE", "IMAGES", "ORIGINALLANGUAGE", "ORIGINALTITLE", "PRODUCTIONCOMPANIES", "PRODUCTIONCOUNTRIES", "REVENUE"};
						if (isColumnExist(connection, TABLE_NAME, "MOVIEORSHOWNAMESIMPLE")) {
							StringBuilder sb = new StringBuilder();
							StringBuilder sbselect = new StringBuilder();
							sb.append("INSERT INTO ").append(MediaTableVideoMetadata.TABLE_NAME);
							sb.append(" (FILEID");
							sbselect.append("ID");
							for (String column : columns)  {
								if (isColumnExist(connection, TABLE_NAME, column)) {
									sb.append(", ");
									//version was refactored
									if ("VERSION".equals(column)) {
										sb.append("API_VERSION");
									} else {
										sb.append(column);
									}
									sbselect.append(", ").append(column);
								}
							}
							sb.append(") ");
							sb.append("SELECT ").append(sbselect.toString()).append(" FROM ").append(TABLE_NAME);
							sb.append(" WHERE MOVIEORSHOWNAMESIMPLE IS NOT NULL");
							sb.append(" AND ID NOT IN (SELECT FILEID FROM ").append(MediaTableVideoMetadata.TABLE_NAME).append(")");
							executeUpdate(connection, sb.toString());
						}

						//delete old columns
						for (String column : columns)  {
							executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN IF EXISTS " + column);
						}

						executeUpdate(connection, "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_" + COL_THUMBID + "_IDX ON " + TABLE_NAME + "(" + COL_THUMBID + ")");
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 33 -> {
						//Rename sql reserved words
						LOGGER.trace("Adding HDRFORMAT column");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS HDRFORMAT VARCHAR");
					}
					default -> {
						// Do the dumb way
						force = true;
					}
				}
				if (force) {
					break;
				}
			}
			try {
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			} catch (SQLException e) {
				LOGGER.error("Failed setting the table version of the {} for {}", TABLE_NAME, e.getMessage());
				throw new SQLException(e);
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_UPGRADING_TABLE_FAILED, DATABASE_NAME, TABLE_NAME, se.getMessage());
			LOGGER.trace("", se);
			force = true;
		}
		if (force) {
			LOGGER.debug("Database will be (re)initialized");
			try {
				//delete rows to cascade delete
				LOGGER.error("Deleting data from table: {}", TABLE_NAME);
				executeUpdate(connection, "DELETE FROM " + TABLE_NAME);
				//remove table and constraints
				MediaDatabase.dropCascadeConstraint(connection, TABLE_NAME);
				MediaDatabase.dropTable(connection, TABLE_NAME);
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				//put back constaints
				executeUpdate(connection, "ALTER TABLE IF EXISTS " + MediaTableAudiotracks.TABLE_NAME + " ADD CONSTRAINT IF NOT EXISTS " + MediaTableAudiotracks.TABLE_NAME + "_" + MediaTableAudiotracks.COL_FILEID + "_FK FOREIGN KEY(" + MediaTableAudiotracks.COL_FILEID + ") REFERENCES " + TABLE_NAME + "(" + COL_ID + ") ON DELETE CASCADE");
				executeUpdate(connection, "ALTER TABLE IF EXISTS " + MediaTableSubtracks.TABLE_NAME + " ADD CONSTRAINT IF NOT EXISTS " + MediaTableSubtracks.TABLE_NAME + "_" + MediaTableSubtracks.COL_FILEID + "_FK FOREIGN KEY(" + MediaTableSubtracks.COL_FILEID + ") REFERENCES " + TABLE_NAME + "(" + COL_ID + ") ON DELETE CASCADE");
				executeUpdate(connection, "ALTER TABLE IF EXISTS " + MediaTableChapters.TABLE_NAME + " ADD CONSTRAINT IF NOT EXISTS " + MediaTableChapters.TABLE_NAME + "_" + MediaTableChapters.COL_FILEID + "_FK FOREIGN KEY(" + MediaTableChapters.COL_FILEID + ") REFERENCES " + TABLE_NAME + "(" + COL_ID + ") ON DELETE CASCADE");
				executeUpdate(connection, "ALTER TABLE IF EXISTS " + MediaTableVideoMetadata.TABLE_NAME + " ADD CONSTRAINT IF NOT EXISTS " + MediaTableVideoMetadata.TABLE_NAME + "_" + MediaTableVideoMetadata.COL_FILEID + "_FK FOREIGN KEY(" + MediaTableVideoMetadata.COL_FILEID + ") REFERENCES " + TABLE_NAME + "(" + COL_ID + ") ON DELETE CASCADE");
			} catch (SQLException se) {
				LOGGER.error("SQL error while (re)initializing tables: {}", se.getMessage());
				LOGGER.trace("", se);
			}
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			StringBuilder sb = new StringBuilder();
			sb.append("CREATE TABLE " + TABLE_NAME + " (");
			sb.append("  ID                      INTEGER          AUTO_INCREMENT PRIMARY KEY");
			sb.append(", THUMBID                 BIGINT");
			sb.append(", FILENAME                VARCHAR(1024)    NOT NULL UNIQUE");
			sb.append(", MODIFIED                TIMESTAMP        NOT NULL");
			sb.append(", FORMAT_TYPE             INTEGER");
			//all columns here are not file related but media related
			sb.append(", DURATION                DOUBLE PRECISION");
			sb.append(", BITRATE                 INTEGER");
			sb.append(", WIDTH                   INTEGER");
			sb.append(", HEIGHT                  INTEGER");
			sb.append(", MEDIA_SIZE              NUMERIC");
			sb.append(", CODECV                  VARCHAR(").append(SIZE_CODECV).append(')');
			sb.append(", FRAMERATE               VARCHAR(").append(SIZE_FRAMERATE).append(')');
			sb.append(", ASPECTRATIODVD          VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", ASPECTRATIOCONTAINER    VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", ASPECTRATIOVIDEOTRACK   VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", REFRAMES                TINYINT");
			sb.append(", AVCLEVEL                VARCHAR(").append(SIZE_AVCLEVEL).append(')');
			sb.append(", IMAGEINFO               OTHER");
			sb.append(", CONTAINER               VARCHAR(").append(SIZE_CONTAINER).append(')');
			sb.append(", MUXINGMODE              VARCHAR(").append(SIZE_MUXINGMODE).append(')');
			sb.append(", FRAMERATEMODE           VARCHAR(").append(SIZE_FRAMERATEMODE).append(')');
			sb.append(", STEREOSCOPY             VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", MATRIXCOEFFICIENTS      VARCHAR(").append(SIZE_MATRIX_COEFFICIENTS).append(')');
			sb.append(", TITLECONTAINER          VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", TITLEVIDEOTRACK         VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", VIDEOTRACKCOUNT         INTEGER");
			sb.append(", IMAGECOUNT              INTEGER");
			sb.append(", BITDEPTH                INTEGER");
			sb.append(", HDRFORMAT               VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", PIXELASPECTRATIO        VARCHAR(").append(SIZE_MAX).append(')');
			sb.append(", SCANTYPE                OTHER");
			sb.append(", SCANORDER               OTHER");
			sb.append(")");
			LOGGER.trace("Creating table FILES with:\n\n{}\n", sb.toString());

			statement.execute(sb.toString());

			LOGGER.trace("Creating index IDX_FILE");
			statement.execute("CREATE UNIQUE INDEX IDX_FILE ON " + TABLE_NAME + " (FILENAME, MODIFIED)");

			LOGGER.trace("Creating index FORMAT_TYPE");
			statement.execute("CREATE INDEX FORMAT_TYPE on " + TABLE_NAME + " (FORMAT_TYPE)");

			LOGGER.trace("Creating index FORMAT_TYPE_WIDTH_HEIGHT");
			statement.execute("CREATE INDEX FORMAT_TYPE_WIDTH_HEIGHT on " + TABLE_NAME + " (FORMAT_TYPE, WIDTH, HEIGHT)");

			LOGGER.trace("Creating index FORMAT_TYPE_MODIFIED");
			statement.execute("CREATE INDEX FORMAT_TYPE_MODIFIED on " + TABLE_NAME + " (FORMAT_TYPE, MODIFIED)");

			LOGGER.trace("Creating index on " + TABLE_COL_THUMBID);
			statement.execute("CREATE INDEX " + TABLE_NAME + "_" + COL_THUMBID + "_IDX ON " + TABLE_NAME + "(" + COL_THUMBID + ")");
		}
	}

	/**
	 * Checks whether a row representing a {@link DLNAMediaInfo} instance for
	 * the given media exists in the database.
	 *
	 * @param connection the db connection
	 * @param filename the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return {@code true} if the data exists for this media, {@code false}
	 *         otherwise.
	 */
	public static boolean isDataExists(final Connection connection, String filename, long modified) {
		return getFileId(connection, filename, modified) != null;
	}

	/**
	 * Gets the file Id for the given media in the database.
	 *
	 * @param connection the db connection
	 * @param filename the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return the file id if the data exists for this media, -1 otherwise.
	 */
	public static Long getFileId(final Connection connection, String filename, long modified) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ID_FILENAME_MODIFIED)) {
				statement.setString(1, filename);
				statement.setTimestamp(2, new Timestamp(modified));
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getLong(1);
					}
				}
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "checking if data exists", TABLE_NAME, filename, se.getMessage());
			LOGGER.trace("", se);
		}
		return null;
	}

	/**
	 * Gets the file Id for the given media in the database.
	 *
	 * @param connection the db connection
	 * @param filename the full path of the media.
	 * @return the file id if the data exists for this media, -1 otherwise.
	 */
	public static Long getFileId(final Connection connection, String filename) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ID_FILENAME)) {
				statement.setString(1, filename);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getLong(1);
					}
				}
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "checking if data exists", TABLE_NAME, filename, se.getMessage());
			LOGGER.trace("", se);
		}
		return null;
	}

	/**
	 * Gets a row of {@link MediaDatabase} from the database and returns it
	 * as a {@link DLNAMediaInfo} instance, along with thumbnails, status and tracks.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return The {@link DLNAMediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public static DLNAMediaInfo getData(final Connection connection, String name, long modified) throws IOException, SQLException {
		DLNAMediaInfo media = null;
		try (
			PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILENAME_MODIFIED);
		) {
			stmt.setString(1, name);
			stmt.setTimestamp(2, new Timestamp(modified));
			try (
				ResultSet rs = stmt.executeQuery();
				PreparedStatement status = connection.prepareStatement("SELECT * FROM " + MediaTableFilesStatus.TABLE_NAME + " WHERE " + MediaTableFilesStatus.TABLE_COL_FILENAME + " = ? LIMIT 1");
			) {
				if (rs.next()) {
					media = new DLNAMediaInfo();
					int id = rs.getInt("ID");
					media.setDuration(toDouble(rs, "DURATION"));
					media.setBitrate(rs.getInt("BITRATE"));
					media.setWidth(rs.getInt("WIDTH"));
					media.setHeight(rs.getInt("HEIGHT"));
					media.setSize(rs.getLong("MEDIA_SIZE"));
					media.setCodecV(rs.getString("CODECV"));
					media.setFrameRate(rs.getString("FRAMERATE"));
					media.setAspectRatioDvdIso(rs.getString("ASPECTRATIODVD"));
					media.setAspectRatioContainer(rs.getString("ASPECTRATIOCONTAINER"));
					media.setAspectRatioVideoTrack(rs.getString("ASPECTRATIOVIDEOTRACK"));
					media.setReferenceFrameCount(rs.getByte("REFRAMES"));
					media.setAvcLevel(rs.getString("AVCLEVEL"));
					media.setImageInfo((ImageInfo) rs.getObject("IMAGEINFO"));
					try {
						media.setThumb((DLNAThumbnail) rs.getObject("THUMBNAIL"));
					} catch (SQLException se) {
						//thumb will be recreated on next thumb request
					}
					media.setContainer(rs.getString("CONTAINER"));
					media.setMuxingMode(rs.getString("MUXINGMODE"));
					media.setFrameRateMode(rs.getString("FRAMERATEMODE"));
					media.setStereoscopy(rs.getString("STEREOSCOPY"));
					media.setMatrixCoefficients(rs.getString("MATRIXCOEFFICIENTS"));
					media.setFileTitleFromMetadata(rs.getString("TITLECONTAINER"));
					media.setVideoTrackTitleFromMetadata(rs.getString("TITLEVIDEOTRACK"));
					media.setVideoTrackCount(rs.getInt("VIDEOTRACKCOUNT"));
					media.setImageCount(rs.getInt("IMAGECOUNT"));
					media.setVideoBitDepth(rs.getInt("BITDEPTH"));
					media.setVideoHDRFormat(rs.getString("HDRFORMAT"));
					media.setPixelAspectRatio(rs.getString("PIXELASPECTRATIO"));
					media.setScanType((DLNAMediaInfo.ScanType) rs.getObject("SCANTYPE"));
					media.setScanOrder((DLNAMediaInfo.ScanOrder) rs.getObject("SCANORDER"));

					media.setAudioTracks(MediaTableAudiotracks.getAudioTracks(connection, id));
					media.setSubtitlesTracks(MediaTableSubtracks.getSubtitleTracks(connection, id));
					media.setChapters(MediaTableChapters.getChapters(connection, id));
					media.setVideoMetadata(MediaTableVideoMetadata.getVideoMetadataByFileId(connection, id));
					media.setMediaparsed(true);

					status.setString(1, name);
					try (ResultSet elements = status.executeQuery()) {
						if (elements.next()) {
							media.setPlaybackCount(elements.getInt("PLAYCOUNT"));
							media.setLastPlaybackTime(elements.getString("DATELASTPLAY"));
							media.setLastPlaybackPosition(elements.getDouble("LASTPLAYBACKPOSITION"));
						}
					}
				}
			}
		}
		return media;
	}

	/**
	 * Gets a row of {@link MediaDatabase} from the database and returns it
	 * as a {@link DLNAMediaInfo} instance.
	 * This is the same as getData above, but is a much smaller query because it
	 * does not fetch thumbnails, status and tracks, and does not require a
	 * modified value to be passed, which means we can avoid touching the filesystem
	 * in the caller.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @return The {@link DLNAMediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public static DLNAMediaInfo getFileMetadata(final Connection connection, String name) throws IOException, SQLException {
		Long id = getFileId(connection, name);
		if (id != null) {
			DLNAMediaInfo media = new DLNAMediaInfo();
			media.setVideoMetadata(MediaTableVideoMetadata.getVideoMetadataByFileId(connection, id));
			media.setMediaparsed(true);
			return media;
		}
		return null;
	}

	/**
	 * Inserts or updates a database row representing an {@link DLNAMediaInfo}
	 * instance. If the row already exists, it will be updated with the
	 * information given in {@code media}. If it doesn't exist, a new will row
	 * be created using the same information.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @param type the integer constant from {@link Format} indicating the type
	 *            of media.
	 * @param media the {@link DLNAMediaInfo} row to update.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertOrUpdateData(final Connection connection, String name, long modified, int type, DLNAMediaInfo media) throws SQLException {
		try {
			long fileId = -1;
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_INSERT_COLUMNS_BY_FILENAME,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, name);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						fileId = rs.getLong("ID");
						rs.updateTimestamp("MODIFIED", new Timestamp(modified));
						rs.updateInt("FORMAT_TYPE", type);
						if (media != null) {
							if (media.getDuration() != null) {
								rs.updateDouble("DURATION", media.getDurationInSeconds());
							} else {
								rs.updateNull("DURATION");
							}

							if (type != Format.IMAGE) {
								if (media.getBitrate() == 0) {
									LOGGER.debug("Could not parse the bitrate for: " + name);
								}
								rs.updateInt("BITRATE", media.getBitrate());
							} else {
								rs.updateInt("BITRATE", 0);
							}
							rs.updateInt("WIDTH", media.getWidth());
							rs.updateInt("HEIGHT", media.getHeight());
							rs.updateLong("MEDIA_SIZE", media.getSize());
							rs.updateString("CODECV", StringUtils.left(media.getCodecV(), SIZE_CODECV));
							rs.updateString("FRAMERATE", StringUtils.left(media.getFrameRate(), SIZE_FRAMERATE));
							rs.updateString("ASPECTRATIODVD", StringUtils.left(media.getAspectRatioDvdIso(), SIZE_MAX));
							rs.updateString("ASPECTRATIOCONTAINER", StringUtils.left(media.getAspectRatioContainer(), SIZE_MAX));
							rs.updateString("ASPECTRATIOVIDEOTRACK", StringUtils.left(media.getAspectRatioVideoTrack(), SIZE_MAX));
							rs.updateByte("REFRAMES", media.getReferenceFrameCount());
							rs.updateString("AVCLEVEL", StringUtils.left(media.getAvcLevel(), SIZE_AVCLEVEL));
							updateSerialized(rs, media.getImageInfo(), "IMAGEINFO");
							if (media.getImageInfo() != null) {
								rs.updateObject("IMAGEINFO", media.getImageInfo());
							} else {
								rs.updateNull("IMAGEINFO");
							}
							rs.updateString("CONTAINER", StringUtils.left(media.getContainer(), SIZE_CONTAINER));
							rs.updateString("MUXINGMODE", StringUtils.left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
							rs.updateString("FRAMERATEMODE", StringUtils.left(media.getFrameRateMode(), SIZE_FRAMERATEMODE));
							rs.updateString("STEREOSCOPY", StringUtils.left(media.getStereoscopy(), SIZE_MAX));
							rs.updateString("MATRIXCOEFFICIENTS", StringUtils.left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
							rs.updateString("TITLECONTAINER", StringUtils.left(media.getFileTitleFromMetadata(), SIZE_MAX));
							rs.updateString("TITLEVIDEOTRACK", StringUtils.left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
							rs.updateInt("VIDEOTRACKCOUNT", media.getVideoTrackCount());
							rs.updateInt("IMAGECOUNT", media.getImageCount());
							rs.updateInt("BITDEPTH", media.getVideoBitDepth());
							rs.updateString("HDRFORMAT", StringUtils.left(media.getVideoHDRFormat(), SIZE_MAX));
							rs.updateString("PIXELASPECTRATIO", StringUtils.left(media.getPixelAspectRatio(), SIZE_MAX));
							updateSerialized(rs, media.getScanType(), "SCANTYPE");
							updateSerialized(rs, media.getScanOrder(), "SCANORDER");
						}
						rs.updateRow();
					}
				}
			}

			if (fileId < 0) {
				// No fileId means it didn't exist
				try (
					PreparedStatement ps = connection.prepareStatement(SQL_INSERT_COLUMNS,
						Statement.RETURN_GENERATED_KEYS
					)
				) {
					int databaseColumnIterator = 0;

					ps.setString(++databaseColumnIterator, name);
					ps.setTimestamp(++databaseColumnIterator, new Timestamp(modified));
					ps.setInt(++databaseColumnIterator, type);
					if (media != null) {
						if (media.getDuration() != null) {
							ps.setDouble(++databaseColumnIterator, media.getDurationInSeconds());
						} else {
							ps.setNull(++databaseColumnIterator, Types.DOUBLE);
						}

						int databaseBitrate = 0;
						if (type != Format.IMAGE) {
							databaseBitrate = media.getBitrate();
							if (databaseBitrate == 0) {
								LOGGER.debug("Could not parse the bitrate for: " + name);
							}
						}
						ps.setInt(++databaseColumnIterator, databaseBitrate);

						ps.setInt(++databaseColumnIterator, media.getWidth());
						ps.setInt(++databaseColumnIterator, media.getHeight());
						ps.setLong(++databaseColumnIterator, media.getSize());
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getCodecV(), SIZE_CODECV));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getFrameRate(), SIZE_FRAMERATE));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getAspectRatioDvdIso(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getAspectRatioContainer(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getAspectRatioVideoTrack(), SIZE_MAX));
						ps.setByte(++databaseColumnIterator, media.getReferenceFrameCount());
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getAvcLevel(), SIZE_AVCLEVEL));
						if (media.getImageInfo() != null) {
							ps.setObject(++databaseColumnIterator, media.getImageInfo());
						} else {
							ps.setNull(++databaseColumnIterator, Types.OTHER);
						}
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getContainer(), SIZE_CONTAINER));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getFrameRateMode(), SIZE_FRAMERATEMODE));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getStereoscopy(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getFileTitleFromMetadata(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
						ps.setInt(++databaseColumnIterator, media.getVideoTrackCount());
						ps.setInt(++databaseColumnIterator, media.getImageCount());
						ps.setInt(++databaseColumnIterator, media.getVideoBitDepth());
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getVideoHDRFormat(), SIZE_MAX));
						ps.setString(++databaseColumnIterator, StringUtils.left(media.getPixelAspectRatio(), SIZE_MAX));
						insertSerialized(ps, media.getScanType(), ++databaseColumnIterator);
						insertSerialized(ps, media.getScanOrder(), ++databaseColumnIterator);
					} else {
						ps.setString(++databaseColumnIterator, null);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setLong(++databaseColumnIterator, 0);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setByte(++databaseColumnIterator, (byte) -1);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.OTHER);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setInt(++databaseColumnIterator, 0);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.VARCHAR);
						ps.setNull(++databaseColumnIterator, Types.OTHER);
						ps.setNull(++databaseColumnIterator, Types.OTHER);
					}
					ps.executeUpdate();
					try (ResultSet rs = ps.getGeneratedKeys()) {
						if (rs.next()) {
							fileId = rs.getLong(1);
						}
					}
				}
			}

			if (media != null && fileId > -1) {
				MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, media, false);
				MediaTableAudiotracks.insertOrUpdateAudioTracks(connection, fileId, media);
				MediaTableSubtracks.insertOrUpdateSubtitleTracks(connection, fileId, media);
				MediaTableChapters.insertOrUpdateChapters(connection, fileId, media);
			}
		} catch (SQLException se) {
			if (se.getErrorCode() == 23505) {
				throw new SQLException(String.format(
					"Duplicate key while adding \"%s\" to the cache: %s",
					name,
					se.getMessage()
				), se);
			}
			throw se;
		} finally {
			if (media != null && media.getThumb() != null) {
				MediaTableThumbnails.setThumbnail(connection, media.getThumb(), name, -1, false);
			}
		}
	}

	/**
	 * Removes a single media file from the database.
	 *
	 * @param connection the db connection
	 * @param pathToFile the full path to the file to remove.
	 * @param removeStatus whether to remove file status entry. WARNING: this
	 *                     is user data and is NOT recoverable like the rest.
	 */
	public static void removeMediaEntry(final Connection connection, String pathToFile, boolean removeStatus) {
		try {
			removeMedia(connection, pathToFile, false, removeStatus);
		} catch (SQLException e) {
			LOGGER.error(
				"An error occurred while trying to remove \"{}\" from the database: {}",
				pathToFile,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes all media files in a folder from the database.
	 *
	 * @param connection the db connection
	 * @param pathToFolder the full path to the folder whose children should be
	 *            removed.
	 */
	public static void removeMediaEntriesInFolder(final Connection connection, String pathToFolder) {
		try {
			removeMedia(connection, sqlLikeEscape(pathToFolder) + "%", true, true);
		} catch (SQLException e) {
			LOGGER.error(
				"An error occurred while trying to remove files matching \"{}\" from the database: {}",
				pathToFolder,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes row(s) in our other tables representing matching media. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see TableTables#sqlLikeEscape(String)
	 *
	 * @param connection the db connection
	 * @param filename the filename(s) to remove.
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @param removeStatus whether to remove file status entry. WARNING: this
	 *                     is user data and is NOT recoverable like the rest.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void removeMedia(final Connection connection, String filename, boolean useLike, boolean removeStatus) throws SQLException {
		if (StringUtils.isEmpty(filename)) {
			return;
		}

		deleteRowsInFilesTable(connection, filename, useLike);
		if (removeStatus) {
			MediaTableFilesStatus.remove(connection, filename, useLike);
		}
	}

	/**
	 * Deletes a row or rows in the FILES table. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see TableTables#sqlLikeEscape(String)
	 *
	 * @param connection the db connection
	 * @param filename the filename to delete
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void deleteRowsInFilesTable(final Connection connection, String filename, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(filename)) {
			return;
		}

		LOGGER.trace("Deleting rows from " + TABLE_NAME + " table where the filename is \"{}\"", filename);
		try (Statement statement = connection.createStatement()) {
			int rows;
			if (useLike) {
				rows = statement.executeUpdate("DELETE FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILENAME + " LIKE " + sqlQuote(filename));
			} else {
				rows = statement.executeUpdate("DELETE FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILENAME + " = " + sqlQuote(filename));
			}
			LOGGER.trace("Deleted {} rows from " + TABLE_NAME, rows);
		}
	}

	/**
	 * Deletes a row or rows in the given {@code tableName} for the given {@code condition}. If {@code useLike} is
	 * {@code true}, the {@code condition} must be properly escaped.
	 *
	 * @see TableTables#sqlLikeEscape(String)
	 *
	 * @param connection the db connection
	 * @param tableName the table name in which a row or rows will be deleted
	 * @param column the column where the {@code condition} will be queried
	 * @param condition the condition for which rows will be deleted
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void deleteRowsInTable(final Connection connection, String tableName, String column, String condition, boolean useLike) throws SQLException {
		if (StringUtils.isEmpty(condition)) {
			return;
		}

		LOGGER.trace("Deleting rows from \"{}\" table for given column \"{}\" and condition \"{}\"", tableName, column, condition);
		try (Statement statement = connection.createStatement()) {
			int rows;
			if (useLike) {
				rows = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + column + " LIKE " + sqlQuote(condition));
			} else {
				rows = statement.executeUpdate("DELETE FROM " + tableName + " WHERE " + column + " = " + sqlQuote(condition));
			}
			LOGGER.trace("Deleted {} rows from " + tableName, rows);
		}
	}

	public static void updateThumbnailId(final Connection connection, String fullPathToFile, int thumbId) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement("UPDATE " + TABLE_NAME + " SET THUMBID = ? WHERE " + TABLE_COL_FILENAME + " = ?"
				);
			) {
				ps.setInt(1, thumbId);
				ps.setString(2, fullPathToFile);
				ps.executeUpdate();
				LOGGER.trace("THUMBID updated to {} for {}", thumbId, fullPathToFile);
			}
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static List<String> getStrings(final Connection connection, String sql) {
		List<String> list = new ArrayList<>();
		Set<String> set = new HashSet<>();
		try {
			try (
				PreparedStatement ps = connection.prepareStatement((sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with")) ? sql : ("SELECT FILENAME FROM " + TABLE_NAME + " WHERE " + sql));
				ResultSet rs = ps.executeQuery()
			) {
				while (rs.next()) {
					String str = rs.getString(1);
					if (StringUtils.isBlank(str)) {
						set.add(NONAME);
					} else {
						set.add(str);
					}
				}
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		}
		list.addAll(set);
		return list;
	}

	public static synchronized void cleanup(final Connection connection) {
		try {
			/*
			 * Cleanup of FILES table
			 *
			 * Removes entries that are not on the hard drive anymore, and
			 * ones that are no longer shared.
			 */
			int dbCount = 0;
			try (
				PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + TABLE_NAME);
				ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					dbCount = rs.getInt(1);
				}
			}
			GuiManager.setStatusLine(Messages.getString("CleaningUpDatabase") + " 0%");

			if (dbCount > 0) {
				try (
					PreparedStatement ps = connection.prepareStatement("SELECT " + TABLE_COL_FILENAME + ", " + TABLE_COL_MODIFIED + ", " + TABLE_COL_ID + " FROM " + TABLE_NAME, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
					ResultSet rs = ps.executeQuery()) {
					List<File> sharedFolders = SharedContentConfiguration.getSharedFolders();
					boolean isFileStillShared = false;
					int oldpercent = 0;
					int i = 0;
					while (rs.next()) {
						String filename = rs.getString("FILENAME");
						long modified = rs.getTimestamp("MODIFIED").getTime();
						File file = new File(filename);
						if (!file.exists() || file.lastModified() != modified) {
							LOGGER.trace("Removing the file {} from our database because it is no longer on the hard drive", filename);
							rs.deleteRow();
						} else {
							// the file exists on the hard drive, but now check if we are still sharing it
							for (File folder : sharedFolders) {
								if (filename.contains(folder.getAbsolutePath())) {
									isFileStillShared = true;
									break;
								}
							}

							if (!isFileStillShared) {
								LOGGER.trace("Removing the file {} from our database because it is no longer shared", filename);
								rs.deleteRow();
							}
						}

						i++;
						int newpercent = i * 100 / dbCount;
						if (newpercent > oldpercent) {
							GuiManager.setStatusLine(Messages.getString("CleaningUpDatabase") + newpercent + "%");
							oldpercent = newpercent;
						}
					}
				}
				GuiManager.setStatusLine(null);
			}

			/*
			 * Cleanup of THUMBNAILS table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			try (
				PreparedStatement ps = connection.prepareStatement(
					"DELETE FROM " + MediaTableThumbnails.TABLE_NAME + " " +
					"WHERE NOT EXISTS (" +
						"SELECT " + TABLE_COL_ID + " FROM " + TABLE_NAME + " " +
						"WHERE " + TABLE_COL_THUMBID + " = " + MediaTableThumbnails.TABLE_COL_ID + " " +
						"LIMIT 1" +
					") AND NOT EXISTS (" +
						"SELECT " + MediaTableTVSeries.TABLE_COL_ID + " FROM " + MediaTableTVSeries.TABLE_NAME + " " +
						"WHERE " + MediaTableTVSeries.TABLE_COL_THUMBID + " = " + MediaTableThumbnails.TABLE_COL_ID + " " +
						"LIMIT 1" +
					");"
			)) {
				ps.execute();
			}

			/*
			 * Cleanup of FILES_STATUS table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			try (
				PreparedStatement ps = connection.prepareStatement(
					"DELETE FROM " + MediaTableFilesStatus.TABLE_NAME + " " +
					"WHERE NOT EXISTS (" +
						"SELECT " + TABLE_COL_ID + " FROM " + TABLE_NAME + " " +
						"WHERE " + TABLE_COL_FILENAME + " = " + MediaTableFilesStatus.TABLE_COL_FILENAME +
					");"
			)) {
				ps.execute();
			}

			/*
			 * Cleanup of TV_SERIES table
			 *
			 * Removes entries that are not referenced by any rows in the FILES table.
			 */
			try (
				PreparedStatement ps = connection.prepareStatement(
					"DELETE FROM " + MediaTableTVSeries.TABLE_NAME + " " +
					"WHERE NOT EXISTS (" +
						"SELECT " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAMESIMPLE + " FROM " + MediaTableVideoMetadata.TABLE_NAME + " " +
						"WHERE " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAMESIMPLE + " = " + MediaTableTVSeries.TABLE_COL_SIMPLIFIEDTITLE + " " +
						"LIMIT 1" +
					");"
			)) {
				ps.execute();
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
		} finally {
			GuiManager.setStatusLine(null);
		}
	}

	public static List<File> getFiles(final Connection connection, String sql) {
		List<File> list = new ArrayList<>();
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(
					sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with") ? sql : ("SELECT FILENAME, MODIFIED FROM " + TABLE_NAME + " WHERE " + sql)
				);
				ResultSet rs = ps.executeQuery();
			) {
				while (rs.next()) {
					String filename = rs.getString("FILENAME");
					long modified = rs.getTimestamp("MODIFIED").getTime();
					File file = new File(filename);
					if (file.exists() && file.lastModified() == modified) {
						list.add(file);
					}
				}
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
			return null;
		}
		return list;
	}

	/**
	 * @param connection the db connection
	 * @param title
	 * @return a thumbnail based on title.
	 */
	public static DLNAThumbnail getThumbnailByTitle(final Connection connection, final String title) {
		boolean trace = LOGGER.isTraceEnabled();

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);

		try {
			String query = "SELECT " + MediaTableThumbnails.TABLE_COL_THUMBNAIL + " " +
				"FROM " + TABLE_NAME + " " +
				SQL_LEFT_JOIN_TABLE_THUMBNAILS +
				SQL_LEFT_JOIN_TABLE_VIDEO_METADATA +
				"WHERE " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAMESIMPLE + " = " + sqlQuote(simplifiedTitle) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query)
			) {
				if (resultSet.next()) {
					return (DLNAThumbnail) resultSet.getObject("THUMBNAIL");
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", title, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}
}
