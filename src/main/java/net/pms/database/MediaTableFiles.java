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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.pms.Messages;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.formats.Format;
import net.pms.gui.GuiManager;
import net.pms.image.ImageInfo;
import net.pms.media.MediaInfo;
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
	 * - 34: Added HDRFORMAT column
	 * - 35: Added HDRFORMATCOMPATIBILITY column
	 */
	private static final int TABLE_VERSION = 36;

	/**
	 * COLUMNS NAMES
	 */
	public static final String COL_ID = "ID";
	public static final String COL_THUMBID = "THUMBID";
	private static final String COL_DURATION = "DURATION";
	private static final String COL_FORMAT_TYPE = "FORMAT_TYPE";
	private static final String COL_FILENAME = "FILENAME";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_WIDTH = "WIDTH";
	private static final String COL_HEIGHT = "HEIGHT";
	private static final String COL_STEREOSCOPY = "STEREOSCOPY";
	private static final String COL_BITRATE = "BITRATE";
	private static final String COL_MEDIA_SIZE = "MEDIA_SIZE";
	private static final String COL_CODECV = "CODECV";
	private static final String COL_FRAMERATE = "FRAMERATE";
	private static final String COL_ASPECTRATIODVD = "ASPECTRATIODVD";
	private static final String COL_ASPECTRATIOCONTAINER = "ASPECTRATIOCONTAINER";
	private static final String COL_ASPECTRATIOVIDEOTRACK = "ASPECTRATIOVIDEOTRACK";
	private static final String COL_REFRAMES = "REFRAMES";
	private static final String COL_AVCLEVEL = "AVCLEVEL";
	private static final String COL_IMAGEINFO = "IMAGEINFO";
	private static final String COL_CONTAINER = "CONTAINER";
	private static final String COL_MUXINGMODE = "MUXINGMODE";
	private static final String COL_FRAMERATEMODE = "FRAMERATEMODE";
	private static final String COL_MATRIXCOEFFICIENTS = "MATRIXCOEFFICIENTS";
	private static final String COL_TITLECONTAINER = "TITLECONTAINER";
	private static final String COL_TITLEVIDEOTRACK = "TITLEVIDEOTRACK";
	private static final String COL_VIDEOTRACKCOUNT = "VIDEOTRACKCOUNT";
	private static final String COL_IMAGECOUNT = "IMAGECOUNT";
	private static final String COL_BITDEPTH = "BITDEPTH";
	private static final String COL_HDRFORMAT = "HDRFORMAT";
	private static final String COL_HDRFORMATCOMPATIBILITY = "HDRFORMATCOMPATIBILITY";
	private static final String COL_PIXELASPECTRATIO = "PIXELASPECTRATIO";
	private static final String COL_SCANTYPE = "SCANTYPE";
	private static final String COL_SCANORDER = "SCANORDER";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_FORMAT_TYPE = TABLE_NAME + "." + COL_FORMAT_TYPE;
	public static final String TABLE_COL_FILENAME = TABLE_NAME + "." + COL_FILENAME;
	public static final String TABLE_COL_MODIFIED = TABLE_NAME + "." + COL_MODIFIED;
	public static final String TABLE_COL_WIDTH = TABLE_NAME + "." + COL_WIDTH;
	public static final String TABLE_COL_HEIGHT = TABLE_NAME + "." + COL_HEIGHT;
	public static final String TABLE_COL_THUMBID = TABLE_NAME + "." + COL_THUMBID;
	public static final String TABLE_COL_STEREOSCOPY = TABLE_NAME + "." + COL_STEREOSCOPY;

	/**
	 * SQL Jointures
	 */
	public static final String SQL_LEFT_JOIN_TABLE_FILES_STATUS = LEFT_JOIN + MediaTableFilesStatus.TABLE_NAME + ON + TABLE_COL_FILENAME + EQUAL + MediaTableFilesStatus.TABLE_COL_FILENAME + " ";
	public static final String SQL_LEFT_JOIN_TABLE_THUMBNAILS = LEFT_JOIN + MediaTableThumbnails.TABLE_NAME + ON + TABLE_COL_THUMBID + EQUAL + MediaTableThumbnails.TABLE_COL_ID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA = LEFT_JOIN + MediaTableVideoMetadata.TABLE_NAME + ON + TABLE_COL_ID + EQUAL + MediaTableVideoMetadata.TABLE_COL_FILEID + " ";

	/**
	 * SQL References
	 */
	public static final String REFERENCE_TABLE_COL_ID = TABLE_NAME + "(" + COL_ID + ")";

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_ROW_COUNT = SELECT + "COUNT(*)" + FROM + TABLE_NAME;
	private static final String SQL_GET_FILENAME_MODIFIED_ID = SELECT + TABLE_COL_FILENAME + COMMA + TABLE_COL_MODIFIED + COMMA + TABLE_COL_ID + FROM + TABLE_NAME;
	private static final String SQL_GET_ALL_BY_FILENAME = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ALL_FILENAME_MODIFIED = SELECT_ALL + FROM + TABLE_NAME + SQL_LEFT_JOIN_TABLE_THUMBNAILS + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_MODIFIED + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_FILENAME_LIKE = SELECT + TABLE_COL_FILENAME + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + PARAMETER;
	private static final String SQL_GET_ID_FILENAME = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_FILENAME_MODIFIED = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_MODIFIED + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_UPDATE_THUMBID_BY_FILENAME = UPDATE + TABLE_NAME + SET + COL_THUMBID + EQUAL + PARAMETER + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_FILENAME = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_FILENAME_LIKE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + PARAMETER;
	private static final String SQL_GET_THUMBNAIL_BY_TITLE = SELECT + MediaTableThumbnails.TABLE_COL_THUMBNAIL + FROM + TABLE_NAME + SQL_LEFT_JOIN_TABLE_THUMBNAILS + SQL_LEFT_JOIN_TABLE_VIDEO_METADATA + WHERE + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAMESIMPLE + EQUAL + PARAMETER + LIMIT_1;

	public static final String NONAME = "###";

	/**
	 * Database column sizes
	 */
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
								.append(UPDATE)
									.append(TABLE_NAME)
								.append(SET)
									.append("IMDBID = NULL, ")
									.append("MEDIA_YEAR = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL")
								.append(WHERE)
									.append(NOT).append("ISTVEPISODE");
							statement.execute(sb.toString());

						}
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 24 -> {
						//Rename sql reserved words
						LOGGER.trace("Deleting index TYPE_ISTV");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_ISTV");
						LOGGER.trace("Deleting index TYPE_ISTV_SIMPLENAME");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_ISTV_SIMPLENAME");
						LOGGER.trace("Deleting index TYPE_ISTV_NAME");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_ISTV_NAME");
						LOGGER.trace("Deleting index TYPE_ISTV_NAME_SEASON");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_ISTV_NAME_SEASON");
						LOGGER.trace("Deleting index TYPE_ISTV_YEAR_STEREOSCOPY");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_ISTV_YEAR_STEREOSCOPY");
						LOGGER.trace("Deleting index TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_WIDTH_HEIGHT");
						LOGGER.trace("Deleting index TYPE_MODIFIED");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "TYPE_MODIFIED");
						if (isColumnExist(connection, TABLE_NAME, "TYPE")) {
							LOGGER.trace("Renaming column name TYPE to FORMAT_TYPE");
							executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`TYPE` " + RENAME_TO + COL_FORMAT_TYPE);
						}
						if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
							LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
							executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`YEAR` " + RENAME_TO + "MEDIA_YEAR");
						}
						if (isColumnExist(connection, TABLE_NAME, "SIZE")) {
							LOGGER.trace("Renaming column name SIZE to MEDIA_SIZE");
							executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + "`SIZE` " + RENAME_TO + COL_MEDIA_SIZE);
						}
						LOGGER.trace("Creating index FORMAT_TYPE");
						executeUpdate(connection, CREATE_INDEX + "FORMAT_TYPE on " + TABLE_NAME + " (FORMAT_TYPE)");
						LOGGER.trace("Creating index FORMAT_TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, CREATE_INDEX + "FORMAT_TYPE_WIDTH_HEIGHT on " + TABLE_NAME + " (FORMAT_TYPE, WIDTH, HEIGHT)");
						LOGGER.trace("Creating index FORMAT_TYPE_MODIFIED");
						executeUpdate(connection, CREATE_INDEX + "FORMAT_TYPE_MODIFIED on " + TABLE_NAME + " (FORMAT_TYPE, MODIFIED)");
					}
					case 25 -> {
						try (Statement statement = connection.createStatement()) {
							/*
							* Since the last release, 10.15.0, we fixed some bugs with TV episode and sport
							* filename parsing so here we clear any cached data for non-episodes.
							*/
							StringBuilder sb = new StringBuilder();
							sb
								.append(UPDATE)
									.append(TABLE_NAME)
								.append(SET)
									.append("IMDBID = NULL, ")
									.append("MEDIA_YEAR = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL")
								.append(WHERE)
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
								.append(UPDATE)
									.append(TABLE_NAME)
								.append(SET)
									.append("IMDBID = NULL, ")
									.append("MEDIA_YEAR = NULL, ")
									.append("MOVIEORSHOWNAME = NULL, ")
									.append("MOVIEORSHOWNAMESIMPLE = NULL, ")
									.append("TVSEASON = NULL, ")
									.append("TVEPISODENUMBER = NULL, ")
									.append("TVEPISODENAME = NULL, ")
									.append("ISTVEPISODE = NULL, ")
									.append("EXTRAINFORMATION = NULL")
								.append(WHERE)
									.append("FILENAME REGEXP '[0-9]of[0-9]'");
							statement.execute(sb.toString());
						}
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 30 -> {
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 31 -> {
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "FORMAT_TYPE_ISTV_YEAR_STEREOSCOPY");
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
							executeUpdate(connection, DROP_INDEX + IF_EXISTS + "" + index);
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
							sb.append(INSERT_INTO).append(MediaTableVideoMetadata.TABLE_NAME);
							sb.append(" (FILEID");
							sbselect.append(COL_ID);
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
							sb.append(SELECT).append(sbselect.toString()).append(FROM).append(TABLE_NAME);
							sb.append(" WHERE MOVIEORSHOWNAMESIMPLE IS NOT NULL");
							sb.append(" AND ID NOT IN (SELECT FILEID FROM ").append(MediaTableVideoMetadata.TABLE_NAME).append(")");
							executeUpdate(connection, sb.toString());
						}

						//delete old columns
						for (String column : columns)  {
							executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " DROP COLUMN IF EXISTS " + column);
						}

						executeUpdate(connection, CREATE_INDEX + "IF NOT EXISTS " + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_THUMBID + "_IDX ON " + TABLE_NAME + "(" + COL_THUMBID + ")");
						LOGGER.trace(LOG_UPGRADED_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, version);
					}
					case 33 -> {
						LOGGER.trace("Adding HDRFORMAT column");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " ADD COLUMN IF NOT EXISTS HDRFORMAT VARCHAR");
					}
					case 34 -> {
						LOGGER.trace("Adding HDRFORMATCOMPATIBILITY column");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " ADD COLUMN IF NOT EXISTS HDRFORMATCOMPATIBILITY VARCHAR");
					}
					case 35 -> {
						//rename indexes
						executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IDX_FILE" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER);
						executeUpdate(connection, ALTER_INDEX + IF_EXISTS + COL_FORMAT_TYPE + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + IDX_MARKER);
						executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "FORMAT_TYPE_WIDTH_HEIGHT" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + COL_WIDTH + CONSTRAINT_SEPARATOR + COL_HEIGHT + IDX_MARKER);
						executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "FORMAT_TYPE_MODIFIED" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER);
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
				executeUpdate(connection, DELETE_FROM + TABLE_NAME);
				//remove table and constraints
				MediaDatabase.dropCascadeConstraint(connection, TABLE_NAME);
				MediaDatabase.dropTable(connection, TABLE_NAME);
				createTable(connection);
				MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				//put back constraints
				executeUpdate(connection, ALTER_TABLE + IF_EXISTS + MediaTableAudiotracks.TABLE_NAME + ADD + CONSTRAINT + IF_NOT_EXISTS + MediaTableAudiotracks.TABLE_NAME + CONSTRAINT_SEPARATOR + MediaTableAudiotracks.COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + MediaTableAudiotracks.COL_FILEID + ")" + REFERENCES + TABLE_NAME + "(" + COL_ID + ")" + ON_DELETE_CASCADE);
				executeUpdate(connection, ALTER_TABLE + IF_EXISTS + MediaTableSubtracks.TABLE_NAME + ADD + CONSTRAINT + IF_NOT_EXISTS + MediaTableSubtracks.TABLE_NAME + CONSTRAINT_SEPARATOR + MediaTableSubtracks.COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + MediaTableSubtracks.COL_FILEID + ")" + REFERENCES + TABLE_NAME + "(" + COL_ID + ")" + ON_DELETE_CASCADE);
				executeUpdate(connection, ALTER_TABLE + IF_EXISTS + MediaTableChapters.TABLE_NAME + ADD + CONSTRAINT + IF_NOT_EXISTS + MediaTableChapters.TABLE_NAME + CONSTRAINT_SEPARATOR + MediaTableChapters.COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + MediaTableChapters.COL_FILEID + ")" + REFERENCES + TABLE_NAME + "(" + COL_ID + ")" + ON_DELETE_CASCADE);
				executeUpdate(connection, ALTER_TABLE + IF_EXISTS + MediaTableVideoMetadata.TABLE_NAME + ADD + CONSTRAINT + IF_NOT_EXISTS + MediaTableVideoMetadata.TABLE_NAME + CONSTRAINT_SEPARATOR + MediaTableVideoMetadata.COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + MediaTableVideoMetadata.COL_FILEID + ")" + REFERENCES + TABLE_NAME + "(" + COL_ID + ")" + ON_DELETE_CASCADE);
			} catch (SQLException se) {
				LOGGER.error("SQL error while (re)initializing tables: {}", se.getMessage());
				LOGGER.trace("", se);
			}
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_ID                      + INTEGER         + AUTO_INCREMENT + PRIMARY_KEY + COMMA +
				COL_THUMBID                 + BIGINT                                         + COMMA +
				COL_FILENAME                + VARCHAR_1024    + NOT_NULL + " " + UNIQUE      + COMMA +
				COL_MODIFIED                + TIMESTAMP       + NOT_NULL                     + COMMA +
				COL_FORMAT_TYPE             + INTEGER                                        + COMMA +
				//all columns here are not file related but media related
				COL_DURATION                + DOUBLE_PRECISION                               + COMMA +
				COL_BITRATE                 + INTEGER                                         + COMMA +
				COL_WIDTH                   + INTEGER                                         + COMMA +
				COL_HEIGHT                  + INTEGER                                         + COMMA +
				COL_MEDIA_SIZE              + NUMERIC                                         + COMMA +
				COL_CODECV                  + VARCHAR_32                                      + COMMA +
				COL_FRAMERATE               + VARCHAR_32                                      + COMMA +
				COL_ASPECTRATIODVD          + VARCHAR_SIZE_MAX                                + COMMA +
				COL_ASPECTRATIOCONTAINER    + VARCHAR_SIZE_MAX                                + COMMA +
				COL_ASPECTRATIOVIDEOTRACK   + VARCHAR_SIZE_MAX                                + COMMA +
				COL_REFRAMES                + TINYINT                                         + COMMA +
				COL_AVCLEVEL                + VARCHAR_3                                       + COMMA +
				COL_IMAGEINFO               + OTHER                                           + COMMA +
				COL_CONTAINER               + VARCHAR_32                                      + COMMA +
				COL_MUXINGMODE              + VARCHAR_32                                      + COMMA +
				COL_FRAMERATEMODE           + VARCHAR_16                                      + COMMA +
				COL_STEREOSCOPY             + VARCHAR_SIZE_MAX                                + COMMA +
				COL_MATRIXCOEFFICIENTS      + VARCHAR_16                                      + COMMA +
				COL_TITLECONTAINER          + VARCHAR_SIZE_MAX                                + COMMA +
				COL_TITLEVIDEOTRACK         + VARCHAR_SIZE_MAX                                + COMMA +
				COL_VIDEOTRACKCOUNT         + INTEGER                                         + COMMA +
				COL_IMAGECOUNT              + INTEGER                                         + COMMA +
				COL_BITDEPTH                + INTEGER                                         + COMMA +
				COL_HDRFORMAT               + VARCHAR_SIZE_MAX                                + COMMA +
				COL_HDRFORMATCOMPATIBILITY  + VARCHAR_SIZE_MAX                                + COMMA +
				COL_PIXELASPECTRATIO        + VARCHAR_SIZE_MAX                                + COMMA +
				COL_SCANTYPE                + OTHER                                           + COMMA +
				COL_SCANORDER               + OTHER                                           +
			")"
		);

		LOGGER.trace("Creating index on " + COL_FILENAME + COMMA + COL_MODIFIED);
		execute(connection, CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FILENAME + COMMA + COL_MODIFIED + ")");

		LOGGER.trace("Creating index on " + COL_FORMAT_TYPE);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FORMAT_TYPE + ")");

		LOGGER.trace("Creating index on " + COL_FORMAT_TYPE + COMMA + COL_WIDTH + COMMA + COL_HEIGHT);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + COL_WIDTH + CONSTRAINT_SEPARATOR + COL_HEIGHT + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FORMAT_TYPE + COMMA + COL_WIDTH + COMMA + COL_HEIGHT + ")");

		LOGGER.trace("Creating index on " + COL_FORMAT_TYPE + COMMA + COL_MODIFIED);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FORMAT_TYPE + COMMA + COL_MODIFIED + ")");

		LOGGER.trace("Creating index on " + COL_THUMBID);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_THUMBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_THUMBID + ")");
	}

	/**
	 * Checks whether a row representing a {@link MediaInfo} instance for
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
	 * as a {@link MediaInfo} instance, along with thumbnails, status and tracks.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return The {@link MediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public static MediaInfo getData(final Connection connection, String name, long modified) throws IOException, SQLException {
		MediaInfo media = null;
		try (
			PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILENAME_MODIFIED);
		) {
			stmt.setString(1, name);
			stmt.setTimestamp(2, new Timestamp(modified));
			try (
				ResultSet rs = stmt.executeQuery();
			) {
				if (rs.next()) {
					media = new MediaInfo();
					int id = rs.getInt(COL_ID);
					media.setDuration(toDouble(rs, COL_DURATION));
					media.setBitrate(rs.getInt(COL_BITRATE));
					media.setWidth(rs.getInt(COL_WIDTH));
					media.setHeight(rs.getInt(COL_HEIGHT));
					media.setSize(rs.getLong(COL_MEDIA_SIZE));
					media.setCodecV(rs.getString(COL_CODECV));
					media.setFrameRate(rs.getString(COL_FRAMERATE));
					media.setAspectRatioDvdIso(rs.getString(COL_ASPECTRATIODVD));
					media.setAspectRatioContainer(rs.getString(COL_ASPECTRATIOCONTAINER));
					media.setAspectRatioVideoTrack(rs.getString(COL_ASPECTRATIOVIDEOTRACK));
					media.setReferenceFrameCount(rs.getByte(COL_REFRAMES));
					media.setAvcLevel(rs.getString(COL_AVCLEVEL));
					media.setImageInfo((ImageInfo) rs.getObject(COL_IMAGEINFO));
					try {
						media.setThumb((DLNAThumbnail) rs.getObject(MediaTableThumbnails.COL_THUMBNAIL));
					} catch (SQLException se) {
						//thumb will be recreated on next thumb request
					}
					media.setContainer(rs.getString(COL_CONTAINER));
					media.setMuxingMode(rs.getString(COL_MUXINGMODE));
					media.setFrameRateMode(rs.getString(COL_FRAMERATEMODE));
					media.setStereoscopy(rs.getString(COL_STEREOSCOPY));
					media.setMatrixCoefficients(rs.getString(COL_MATRIXCOEFFICIENTS));
					media.setFileTitleFromMetadata(rs.getString(COL_TITLECONTAINER));
					media.setVideoTrackTitleFromMetadata(rs.getString(COL_TITLEVIDEOTRACK));
					media.setVideoTrackCount(rs.getInt(COL_VIDEOTRACKCOUNT));
					media.setImageCount(rs.getInt(COL_IMAGECOUNT));
					media.setVideoBitDepth(rs.getInt(COL_BITDEPTH));
					media.setVideoHDRFormat(rs.getString(COL_HDRFORMAT));
					media.setVideoHDRFormatCompatibility(rs.getString(COL_HDRFORMATCOMPATIBILITY));
					media.setPixelAspectRatio(rs.getString(COL_PIXELASPECTRATIO));
					// TODO : store this as string
					media.setScanType((DLNAMediaInfo.ScanType) rs.getObject(COL_SCANTYPE));
					media.setScanOrder((DLNAMediaInfo.ScanOrder) rs.getObject(COL_SCANORDER));

					media.setAudioTracks(MediaTableAudiotracks.getAudioTracks(connection, id));
					media.setSubtitlesTracks(MediaTableSubtracks.getSubtitleTracks(connection, id));
					media.setChapters(MediaTableChapters.getChapters(connection, id));
					media.setVideoMetadata(MediaTableVideoMetadata.getVideoMetadataByFileId(connection, id));
					media.setMediaparsed(true);
				}
			}
		}
		return media;
	}

	/**
	 * Gets a row of {@link MediaDatabase} from the database and returns it
	 * as a {@link MediaInfo} instance.
	 * This is the same as getData above, but is a much smaller query because it
	 * does not fetch thumbnails, status and tracks, and does not require a
	 * modified value to be passed, which means we can avoid touching the filesystem
	 * in the caller.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @return The {@link MediaInfo} instance matching
	 *         {@code name} and {@code modified}.
	 * @throws SQLException if an SQL error occurs during the operation.
	 * @throws IOException if an IO error occurs during the operation.
	 */
	public static MediaInfo getFileMetadata(final Connection connection, String name) throws IOException, SQLException {
		Long id = getFileId(connection, name);
		if (id != null) {
			MediaInfo media = new MediaInfo();
			media.setVideoMetadata(MediaTableVideoMetadata.getVideoMetadataByFileId(connection, id));
			media.setMediaparsed(true);
			return media;
		}
		return null;
	}

	/**
	 * Inserts or updates a database row representing an {@link MediaInfo}
	 * instance. If the row already exists, it will be updated with the
	 * information given in {@code media}. If it doesn't exist, a new will row
	 * be created using the same information.
	 *
	 * @param connection the db connection
	 * @param name the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @param type the integer constant from {@link Format} indicating the type
	 *            of media.
	 * @param media the {@link MediaInfo} row to update.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertOrUpdateData(final Connection connection, String name, long modified, int type, MediaInfo media) throws SQLException {
		try {
			long fileId = -1;
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_ALL_BY_FILENAME,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, name);
				try (ResultSet result = ps.executeQuery()) {
					boolean isCreatingNewRecord = !result.next();
					if (isCreatingNewRecord) {
						result.moveToInsertRow();
						result.updateString(COL_FILENAME, name);
					} else {
						fileId = result.getLong(COL_ID);
					}
					result.updateTimestamp(COL_MODIFIED, new Timestamp(modified));
					result.updateInt(COL_FORMAT_TYPE, type);
					if (media != null) {
						if (media.getDuration() != null) {
							result.updateDouble(COL_DURATION, media.getDurationInSeconds());
						} else {
							result.updateNull(COL_DURATION);
						}

						if (type != Format.IMAGE) {
							if (media.getBitrate() == 0) {
								LOGGER.debug("Could not parse the bitrate for: " + name);
							}
							result.updateInt(COL_BITRATE, media.getBitrate());
						} else {
							result.updateInt(COL_BITRATE, 0);
						}
						result.updateInt(COL_WIDTH, media.getWidth());
						result.updateInt(COL_HEIGHT, media.getHeight());
						result.updateLong(COL_MEDIA_SIZE, media.getSize());
						result.updateString(COL_CODECV, StringUtils.left(media.getCodecV(), SIZE_CODECV));
						result.updateString(COL_FRAMERATE, StringUtils.left(media.getFrameRate(), SIZE_FRAMERATE));
						result.updateString(COL_ASPECTRATIODVD, StringUtils.left(media.getAspectRatioDvdIso(), SIZE_MAX));
						result.updateString(COL_ASPECTRATIOCONTAINER, StringUtils.left(media.getAspectRatioContainer(), SIZE_MAX));
						result.updateString(COL_ASPECTRATIOVIDEOTRACK, StringUtils.left(media.getAspectRatioVideoTrack(), SIZE_MAX));
						result.updateByte(COL_REFRAMES, media.getReferenceFrameCount());
						result.updateString(COL_AVCLEVEL, StringUtils.left(media.getAvcLevel(), SIZE_AVCLEVEL));
						updateSerialized(result, media.getImageInfo(), COL_IMAGEINFO);
						if (media.getImageInfo() != null) {
							result.updateObject(COL_IMAGEINFO, media.getImageInfo());
						} else {
							result.updateNull(COL_IMAGEINFO);
						}
						result.updateString(COL_CONTAINER, StringUtils.left(media.getContainer(), SIZE_CONTAINER));
						result.updateString(COL_MUXINGMODE, StringUtils.left(media.getMuxingModeAudio(), SIZE_MUXINGMODE));
						result.updateString(COL_FRAMERATEMODE, StringUtils.left(media.getFrameRateMode(), SIZE_FRAMERATEMODE));
						result.updateString(COL_STEREOSCOPY, StringUtils.left(media.getStereoscopy(), SIZE_MAX));
						result.updateString(COL_MATRIXCOEFFICIENTS, StringUtils.left(media.getMatrixCoefficients(), SIZE_MATRIX_COEFFICIENTS));
						result.updateString(COL_TITLECONTAINER, StringUtils.left(media.getFileTitleFromMetadata(), SIZE_MAX));
						result.updateString(COL_TITLEVIDEOTRACK, StringUtils.left(media.getVideoTrackTitleFromMetadata(), SIZE_MAX));
						result.updateInt(COL_VIDEOTRACKCOUNT, media.getVideoTrackCount());
						result.updateInt(COL_IMAGECOUNT, media.getImageCount());
						result.updateInt(COL_BITDEPTH, media.getVideoBitDepth());
						result.updateString(COL_HDRFORMAT, StringUtils.left(media.getVideoHDRFormat(), SIZE_MAX));
						result.updateString(COL_HDRFORMATCOMPATIBILITY, StringUtils.left(media.getVideoHDRFormatCompatibility(), SIZE_MAX));
						result.updateString(COL_PIXELASPECTRATIO, StringUtils.left(media.getPixelAspectRatio(), SIZE_MAX));
						updateSerialized(result, media.getScanType(), COL_SCANTYPE);
						updateSerialized(result, media.getScanOrder(), COL_SCANORDER);
					}
					if (isCreatingNewRecord) {
						result.insertRow();
						fileId = getFileId(connection, name);
					} else {
						result.updateRow();
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
		try (PreparedStatement ps = connection.prepareStatement(useLike ? SQL_DELETE_BY_FILENAME_LIKE : SQL_DELETE_BY_FILENAME,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
			ps.setString(1, filename);
			int rows = ps.executeUpdate();
			LOGGER.trace("Deleted {} rows from " + TABLE_NAME, rows);
		}
	}

	public static void updateThumbnailId(final Connection connection, String fullPathToFile, int thumbId) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_THUMBID_BY_FILENAME);
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
		Set<String> set = new LinkedHashSet<>();
		try {
			try (
				PreparedStatement ps = connection.prepareStatement((sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with")) ? sql : ("SELECT FILENAME FROM " + TABLE_NAME + WHERE + sql));
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
				PreparedStatement ps = connection.prepareStatement(SQL_GET_ROW_COUNT);
				ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					dbCount = rs.getInt(1);
				}
			}

			if (dbCount > 0) {
				GuiManager.setStatusLine(Messages.getString("CleaningUpDatabase") + " 0%");
				try (
					PreparedStatement ps = connection.prepareStatement(SQL_GET_FILENAME_MODIFIED_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
					ResultSet rs = ps.executeQuery()) {
					List<File> sharedFolders = SharedContentConfiguration.getSharedFolders();
					int oldpercent = 0;
					int i = 0;
					while (rs.next()) {
						String filename = rs.getString(COL_FILENAME);
						long modified = rs.getTimestamp(COL_MODIFIED).getTime();
						File file = new File(filename);
						if (!file.exists() || file.lastModified() != modified) {
							LOGGER.trace("Removing the file {} from our database because it is no longer on the hard drive", filename);
							rs.deleteRow();
						} else {
							// the file exists on the hard drive, but now check if we are still sharing it
							boolean isFileStillShared = false;
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
					DELETE_FROM + MediaTableThumbnails.TABLE_NAME +
					WHERE + NOT + EXISTS + "(" +
						SELECT + TABLE_COL_ID + FROM + TABLE_NAME +
						WHERE + TABLE_COL_THUMBID + EQUAL + MediaTableThumbnails.TABLE_COL_ID +
						LIMIT_1 +
					")" + AND + NOT + EXISTS + "(" +
						SELECT + MediaTableTVSeries.TABLE_COL_ID + FROM + MediaTableTVSeries.TABLE_NAME +
						WHERE + MediaTableTVSeries.TABLE_COL_THUMBID + EQUAL + MediaTableThumbnails.TABLE_COL_ID +
						LIMIT_1 +
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
					DELETE_FROM + MediaTableFilesStatus.TABLE_NAME +
					WHERE + NOT + EXISTS + "(" +
						SELECT + TABLE_COL_ID + FROM + TABLE_NAME +
						WHERE + TABLE_COL_FILENAME + EQUAL + MediaTableFilesStatus.TABLE_COL_FILENAME +
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
					DELETE_FROM + MediaTableTVSeries.TABLE_NAME +
					WHERE + NOT + EXISTS + "(" +
						SELECT + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAMESIMPLE + FROM + MediaTableVideoMetadata.TABLE_NAME +
						WHERE + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAMESIMPLE + EQUAL + MediaTableTVSeries.TABLE_COL_SIMPLIFIEDTITLE +
						LIMIT_1 +
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

	protected static List<String> getFilenamesInFolder(final Connection connection, final String fullPathToFolder) {
		List<String> result = new ArrayList<>();
		if (StringUtils.isBlank(fullPathToFolder)) {
			return result;
		}
		try (PreparedStatement ps = connection.prepareStatement(SQL_GET_FILENAME_LIKE)) {
			ps.setString(1, fullPathToFolder + "%");
			try (
				ResultSet rs = ps.executeQuery();
			) {
				while (rs.next()) {
					result.add(rs.getString(COL_FILENAME));
				}
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
		}
		return result;
	}

	/**
	 * @param connection the db connection
	 * @param title
	 * @return a thumbnail based on title.
	 */
	public static DLNAThumbnail getThumbnailByTitle(final Connection connection, final String title) {
		boolean trace = LOGGER.isTraceEnabled();

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		try (PreparedStatement ps = connection.prepareStatement(SQL_GET_THUMBNAIL_BY_TITLE)) {
			ps.setString(1, simplifiedTitle);
			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", ps);
			}
			try (
				ResultSet rs = ps.executeQuery();
			) {
				if (rs.next()) {
					return (DLNAThumbnail) rs.getObject(MediaTableThumbnails.COL_THUMBNAIL);
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", title, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	//TODO : review this
	public static List<File> getFiles(final Connection connection, String sql) {
		List<File> list = new ArrayList<>();
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(
					sql.toLowerCase().startsWith("select") || sql.toLowerCase().startsWith("with") ? sql : ("SELECT FILENAME, MODIFIED FROM " + TABLE_NAME + WHERE + sql)
				);
				ResultSet rs = ps.executeQuery();
			) {
				while (rs.next()) {
					String filename = rs.getString(COL_FILENAME);
					long modified = rs.getTimestamp(COL_MODIFIED).getTime();
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

}
