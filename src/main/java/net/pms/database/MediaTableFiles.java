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
import java.util.Objects;
import java.util.Set;
import net.pms.Messages;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.gui.GuiManager;
import net.pms.image.ImageInfo;
import net.pms.media.MediaInfo;
import net.pms.store.MediaStoreIds;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
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
	 * - 36: rename indexes
	 * - 37: remove video infos
	 * - 38: added Mime type
	 * - 39: typo on column name
	 * - 40: added thumbnail source
	 * - 41: ID as BIGINT
	 * - 42: ID as IDENTITY
	 */
	private static final int TABLE_VERSION = 42;

	/**
	 * COLUMNS NAMES
	 */
	public static final String COL_ID = "ID";
	public static final String COL_THUMBID = "THUMBID";
	private static final String COL_THUMB_SRC = "THUMB_SRC";
	private static final String COL_FORMAT_TYPE = "FORMAT_TYPE";
	public static final String COL_FILENAME = "FILENAME";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_PARSER = "PARSER";
	private static final String COL_MEDIA_SIZE = "MEDIA_SIZE";
	private static final String COL_CONTAINER = "CONTAINER";
	private static final String COL_MIMETYPE = "MIMETYPE";
	private static final String COL_TITLECONTAINER = "TITLECONTAINER";
	private static final String COL_DURATION = "DURATION";
	private static final String COL_BITRATE = "BITRATE";
	private static final String COL_FRAMERATE = "FRAMERATE";

	private static final String COL_ASPECTRATIODVD = "ASPECTRATIODVD";
	private static final String COL_IMAGEINFO = "IMAGEINFO";
	private static final String COL_IMAGECOUNT = "IMAGECOUNT";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_FORMAT_TYPE = TABLE_NAME + "." + COL_FORMAT_TYPE;
	public static final String TABLE_COL_FILENAME = TABLE_NAME + "." + COL_FILENAME;
	public static final String TABLE_COL_MODIFIED = TABLE_NAME + "." + COL_MODIFIED;
	public static final String TABLE_COL_THUMBID = TABLE_NAME + "." + COL_THUMBID;
	public static final String TABLE_COL_DURATION = TABLE_NAME + "." + COL_DURATION;

	/**
	 * SQL Jointures
	 */
	public static final String SQL_LEFT_JOIN_TABLE_FILES_STATUS = LEFT_JOIN + MediaTableFilesStatus.TABLE_NAME + ON + TABLE_COL_FILENAME + EQUAL + MediaTableFilesStatus.TABLE_COL_FILENAME;
	public static final String SQL_LEFT_JOIN_TABLE_THUMBNAILS = LEFT_JOIN + MediaTableThumbnails.TABLE_NAME + ON + TABLE_COL_THUMBID + EQUAL + MediaTableThumbnails.TABLE_COL_ID;
	public static final String SQL_LEFT_JOIN_TABLE_AUDIO_METADATA = LEFT_JOIN + MediaTableAudioMetadata.TABLE_NAME + ON + TABLE_COL_ID + EQUAL + MediaTableAudioMetadata.TABLE_COL_FILEID;
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA = LEFT_JOIN + MediaTableVideoMetadata.TABLE_NAME + ON + TABLE_COL_ID + EQUAL + MediaTableVideoMetadata.TABLE_COL_FILEID;

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
	private static final String SQL_GET_FILENAME_BY_ID = SELECT + TABLE_COL_FILENAME + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_GET_FILENAME_LIKE = SELECT + TABLE_COL_FILENAME + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + LIKE_STARTING_WITH_PARAMETER;
	private static final String SQL_GET_ID_FILENAME = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_FORMAT_TYPE_BY_FILENAME = SELECT + TABLE_COL_FORMAT_TYPE + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_FILENAME_MODIFIED = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER + AND + TABLE_COL_MODIFIED + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_UPDATE_THUMBID_BY_ID = UPDATE + TABLE_NAME + SET + COL_THUMBID + EQUAL + PARAMETER + COMMA + COL_THUMB_SRC + EQUAL + PARAMETER + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_THUMB_SRC_LOC = UPDATE + TABLE_NAME + SET + COL_THUMB_SRC + EQUAL + PARAMETER + WHERE + COL_THUMB_SRC + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_ID = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_FILENAME = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + EQUAL + PARAMETER;
	private static final String SQL_DELETE_BY_FILENAME_LIKE = DELETE_FROM + TABLE_NAME + WHERE + TABLE_COL_FILENAME + LIKE + LIKE_STARTING_WITH_PARAMETER;
	private static final String SQL_GET_THUMBNAIL_BY_TITLE = SELECT + TABLE_COL_THUMBID + FROM + TABLE_NAME + SQL_LEFT_JOIN_TABLE_VIDEO_METADATA + WHERE + MediaTableVideoMetadata.TABLE_COL_TITLE + EQUAL + PARAMETER + LIMIT_1;

	/**
	 * Used by child tables
	 */
	public static final String CHILD_ID = "FILEID";

	public static final String NONAME = "###";

	/**
	 * Database column sizes
	 */
	private static final int SIZE_CONTAINER = 32;

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
						executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "FORMAT_TYPE_MODIFIED" + RENAME_TO + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER);
					}
					case 36 -> {
						//remove indexes
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + "FORMAT_TYPE_WIDTH_HEIGHT");
						executeUpdate(connection, DROP_INDEX + IF_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + "WIDTH" + CONSTRAINT_SEPARATOR + "HEIGHT" + IDX_MARKER);
						String[] columns = {COL_FRAMERATE, "ASPECTRATIOCONTAINER", "ASPECTRATIOVIDEOTRACK", "REFRAMES", "AVCLEVEL", "WIDTH",
							"HEIGHT", "CODECV", "FRAMERATEMODE", "MATRIXCOEFFICIENTS", "TITLEVIDEOTRACK", "VIDEOTRACKCOUNT", "STEREOSCOPY",
							"BITDEPTH", "HDRFORMAT", "HDRFORMATCOMPATIBILITY", "PIXELASPECTRATIO", "SCANTYPE", "SCANORDER", "MUXINGMODE"
						};
						//delete old columns
						for (String column : columns)  {
							executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + column);
						}
						LOGGER.trace("Adding back FRAMERATE column");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_FRAMERATE + DOUBLE_PRECISION);
						LOGGER.trace("Adding PARSER column");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_PARSER + VARCHAR_32);
						//check all cascade constaints
						ensureCascadeConstraint(connection, MediaTableAudioMetadata.TABLE_NAME, MediaTableAudioMetadata.COL_FILEID, TABLE_NAME, COL_ID);
						ensureCascadeConstraint(connection, MediaTableAudiotracks.TABLE_NAME, MediaTableAudiotracks.COL_FILEID, TABLE_NAME, COL_ID);
						ensureCascadeConstraint(connection, MediaTableChapters.TABLE_NAME, MediaTableChapters.COL_FILEID, TABLE_NAME, COL_ID);
						ensureCascadeConstraint(connection, MediaTableSubtracks.TABLE_NAME, MediaTableSubtracks.COL_FILEID, TABLE_NAME, COL_ID);
						ensureCascadeConstraint(connection, MediaTableVideotracks.TABLE_NAME, MediaTableVideotracks.COL_FILEID, TABLE_NAME, COL_ID);
						ensureCascadeConstraint(connection, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.COL_FILEID, TABLE_NAME, COL_ID);
					}
					case 37 -> {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_MIMETYPE + VARCHAR_32);
					}
					case 38 -> {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "COL_MIMETYPE" + RENAME_TO + COL_MIMETYPE);
					}
					case 39 -> {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_THUMB_SRC + VARCHAR_32);
					}
					case 40 -> {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_ID + BIGINT);
					}
					case 41 -> {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_ID + IDENTITY);
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + COL_ID + " RESTART WITH (SELECT MAX(ID) + 1 FROM FILES)");
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
				ensureCascadeConstraint(connection, MediaTableAudioMetadata.TABLE_NAME, MediaTableAudioMetadata.COL_FILEID, TABLE_NAME, COL_ID);
				ensureCascadeConstraint(connection, MediaTableAudiotracks.TABLE_NAME, MediaTableAudiotracks.COL_FILEID, TABLE_NAME, COL_ID);
				ensureCascadeConstraint(connection, MediaTableChapters.TABLE_NAME, MediaTableChapters.COL_FILEID, TABLE_NAME, COL_ID);
				ensureCascadeConstraint(connection, MediaTableSubtracks.TABLE_NAME, MediaTableSubtracks.COL_FILEID, TABLE_NAME, COL_ID);
				ensureCascadeConstraint(connection, MediaTableVideotracks.TABLE_NAME, MediaTableVideotracks.COL_FILEID, TABLE_NAME, COL_ID);
				ensureCascadeConstraint(connection, MediaTableVideoMetadata.TABLE_NAME, MediaTableVideoMetadata.COL_FILEID, TABLE_NAME, COL_ID);
			} catch (SQLException se) {
				LOGGER.error("SQL error while (re)initializing tables: {}", se.getMessage());
				LOGGER.trace("", se);
			}
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_ID                      + IDENTITY                                       + COMMA +
				COL_THUMBID                 + BIGINT                                         + COMMA +
				COL_THUMB_SRC               + VARCHAR_32                                     + COMMA +
				COL_FILENAME                + VARCHAR_1024    + NOT_NULL + " " + UNIQUE      + COMMA +
				COL_MODIFIED                + TIMESTAMP       + NOT_NULL                     + COMMA +
				COL_PARSER                  + VARCHAR_32                                     + COMMA +
				COL_FORMAT_TYPE             + INTEGER                                        + COMMA +
				COL_MEDIA_SIZE              + NUMERIC                                        + COMMA +
				COL_CONTAINER               + VARCHAR_32                                     + COMMA +
				COL_MIMETYPE                + VARCHAR_32                                     + COMMA +
				COL_TITLECONTAINER          + VARCHAR_SIZE_MAX                               + COMMA +
				COL_DURATION                + DOUBLE_PRECISION                               + COMMA +
				COL_BITRATE                 + INTEGER                                        + COMMA +
				COL_FRAMERATE               + DOUBLE_PRECISION                               + COMMA +
				//all columns here are not file (container) related but media related
				COL_ASPECTRATIODVD          + VARCHAR_SIZE_MAX                               + COMMA +
				COL_IMAGECOUNT              + INTEGER                                        + COMMA +
				COL_IMAGEINFO               + OTHER                                          +
			")"
		);

		LOGGER.trace("Creating index on " + COL_FILENAME + COMMA + COL_MODIFIED);
		execute(connection, CREATE_UNIQUE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILENAME + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FILENAME + COMMA + COL_MODIFIED + ")");

		LOGGER.trace("Creating index on " + COL_FORMAT_TYPE);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FORMAT_TYPE + ")");

		LOGGER.trace("Creating index on " + COL_FORMAT_TYPE + COMMA + COL_MODIFIED);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FORMAT_TYPE + CONSTRAINT_SEPARATOR + COL_MODIFIED + IDX_MARKER + ON + TABLE_NAME + " (" + COL_FORMAT_TYPE + COMMA + COL_MODIFIED + ")");

		LOGGER.trace("Creating index on " + COL_THUMBID);
		execute(connection, CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_THUMBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_THUMBID + ")");
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
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting fileId for {}", TABLE_NAME, filename, se.getMessage());
			LOGGER.trace("", se);
		}
		return null;
	}

	/**
	 * Gets the file Id for the given media in the database.
	 *
	 * @param filename the full path of the media.
	 * @return the file id if the data exists for this media, null otherwise.
	 */
	public static Long getFileId(String filename) {
		if (StringUtils.isBlank(filename)) {
			return null;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			return getFileId(connection, filename);
		} catch (Exception e) {
			LOGGER.error("cannot get fileId for {} ", filename, e);
		}
		return null;
	}

	/**
	 * Gets the file Id for the given media in the database.
	 *
	 * @param connection the db connection
	 * @param filename the full path of the media.
	 * @return the file id if the data exists for this media, null otherwise.
	 */
	protected static Long getFileId(final Connection connection, String filename) {
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
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting fileId for {}", TABLE_NAME, filename, se.getMessage());
			LOGGER.trace("", se);
		}
		return null;
	}

	/**
	 * Gets the format type for the given media in the database.
	 *
	 * @param filename the full path of the media.
	 * @return the file type if the data exists for this media, null otherwise.
	 */
	public static Integer getFormatType(String filename) {
		if (StringUtils.isBlank(filename)) {
			return null;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			return getFormatType(connection, filename);
		} catch (Exception e) {
			LOGGER.error("cannot get format type for {} ", filename, e);
		}
		return null;
	}

	/**
	 * Gets the format type for the given media in the database.
	 *
	 * @param connection the db connection
	 * @param filename the full path of the media.
	 * @return the file type if the data exists for this media, null otherwise.
	 */
	private static Integer getFormatType(final Connection connection, String filename) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_FORMAT_TYPE_BY_FILENAME)) {
				statement.setString(1, filename);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getInt(1);
					}
				}
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getting format type for {}", TABLE_NAME, filename, se.getMessage());
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
	public static MediaInfo getMediaInfo(final Connection connection, String filename, long modified) throws IOException, SQLException {
		MediaInfo media = null;
		try (
			PreparedStatement stmt = connection.prepareStatement(SQL_GET_ALL_FILENAME_MODIFIED);
		) {
			stmt.setString(1, filename);
			stmt.setTimestamp(2, new Timestamp(modified));
			try (
				ResultSet rs = stmt.executeQuery();
			) {
				if (rs.next()) {
					media = new MediaInfo();
					long fileId = rs.getLong(COL_ID);
					media.setFileId(fileId);
					media.setMediaParser(rs.getString(COL_PARSER));
					media.setSize(rs.getLong(COL_MEDIA_SIZE));
					media.setContainer(rs.getString(COL_CONTAINER));
					media.setMimeType(rs.getString(COL_MIMETYPE));
					media.setTitle(rs.getString(COL_TITLECONTAINER));
					media.setDuration(toDouble(rs, COL_DURATION));
					media.setBitRate(rs.getInt(COL_BITRATE));
					media.setFrameRate(toDouble(rs, COL_FRAMERATE));
					media.setThumbnailId(toLong(rs, COL_THUMBID));
					media.setThumbnailSource(rs.getString(COL_THUMB_SRC));
					//not media related
					media.setAspectRatioDvdIso(rs.getString(COL_ASPECTRATIODVD));
					media.setImageInfo((ImageInfo) rs.getObject(COL_IMAGEINFO));
					media.setImageCount(rs.getInt(COL_IMAGECOUNT));

					media.setAudioTracks(MediaTableAudiotracks.getAudioTracks(connection, fileId));
					media.setVideoTracks(MediaTableVideotracks.getVideoTracks(connection, fileId));
					media.setSubtitlesTracks(MediaTableSubtracks.getSubtitleTracks(connection, fileId));
					media.setChapters(MediaTableChapters.getChapters(connection, fileId));
					media.setAudioMetadata(MediaTableAudioMetadata.getAudioMetadataByFileId(connection, fileId));
					media.setVideoMetadata(MediaTableVideoMetadata.getVideoMetadataByFileId(connection, fileId));
					//get localized thumb if thumb was not localized
					if (media.getVideoMetadata() != null &&
						media.getVideoMetadata().getPoster() != null &&
						!media.getThumbnailSource().equals(ThumbnailSource.TMDB_LOC)
						) {
						DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(media.getVideoMetadata().getPoster());
						if (thumbnail != null) {
							Long thumbnailId = ThumbnailStore.getId(thumbnail);
							if (!Objects.equals(thumbnailId, media.getThumbnailId())) {
								media.setThumbnailId(thumbnailId);
								MediaStoreIds.incrementUpdateIdForFilename(connection, filename);
							}
							media.setThumbnailSource(ThumbnailSource.TMDB_LOC);
							updateThumbnailId(connection, fileId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
						}
					}
				}
			}
		}
		return media;
	}

	/**
	 * Stores the file in the database if it doesn't already exist.
	 *
	 * @param filename the full path to the file.
	 * @param formatType the type constant defined in {@link Format}.
	 * @return The file ID.
	 */
	public static Long getOrInsertFileId(String filename, Long modified, int type) {
		if (StringUtils.isBlank(filename)) {
			return null;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			return getOrInsertFileId(connection, filename, modified, type);
		} catch (Exception e) {
			LOGGER.error("cannot get fileId for {} ", filename, e);
		}
		return null;
	}

	private static Long getOrInsertFileId(final Connection connection, String filename, long modified, int type) {
		if (connection == null || StringUtils.isBlank(filename)) {
			return null;
		}
		Long fileId = getFileId(connection, filename, modified);
		if (fileId != null) {
			return fileId;
		}
		try {
			return insertOrUpdateData(connection, filename, modified, type, null);
		} catch (SQLException ex) {
			return null;
		}
	}

	public static Long insertOrUpdateData(String name, long modified, int type, MediaInfo media) {
		if (StringUtils.isBlank(name)) {
			return null;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			return insertOrUpdateData(connection, name, modified, type, media);
		} catch (Exception e) {
			LOGGER.error("cannot store data for {} ", name, e);
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
	 *             of media.
	 * @param media the {@link MediaInfo} row to update.
	 * @return The file ID.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static Long insertOrUpdateData(final Connection connection, String name, long modified, int type, MediaInfo media) throws SQLException {
		Long fileId = null;
		try {
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
						updateString(result, COL_PARSER, media.getMediaParser(), SIZE_MAX);
						updateLong(result, COL_THUMBID, media.getThumbnailId());
						if (media.getThumbnailSource() != null) {
							updateString(result, COL_THUMB_SRC, media.getThumbnailSource().toString(), 32);
						}
						result.updateLong(COL_MEDIA_SIZE, media.getSize());
						updateString(result, COL_CONTAINER, media.getContainer(), SIZE_CONTAINER);
						updateString(result, COL_MIMETYPE, media.getMimeType(), 32);
						updateString(result, COL_TITLECONTAINER, media.getTitle(), SIZE_MAX);
						updateDouble(result, COL_DURATION, media.getDurationInSeconds());
						updateInteger(result, COL_BITRATE, media.getBitRate());
						updateDouble(result, COL_FRAMERATE, media.getFrameRate());
						//not media related
						result.updateInt(COL_IMAGECOUNT, media.getImageCount());
						updateString(result, COL_ASPECTRATIODVD, media.getAspectRatioDvdIso(), SIZE_MAX);
						updateObject(result, COL_IMAGEINFO, media.getImageInfo());
					}
					if (isCreatingNewRecord) {
						result.insertRow();
						fileId = getFileId(connection, name);
					} else {
						result.updateRow();
					}
				}
			}

			if (media != null && fileId != null) {
				media.setFileId(fileId);
				MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, media, false);
				MediaTableVideotracks.insertOrUpdateVideoTracks(connection, fileId, media);
				MediaTableAudiotracks.insertOrUpdateAudioTracks(connection, fileId, media);
				MediaTableAudioMetadata.insertOrUpdateAudioMetadata(connection, fileId, media);
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
			if (fileId != null) {
				//let store know that we change media metadata
				MediaStoreIds.incrementUpdateIdForFilename(connection, name);
			}
		}
		return fileId;
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
			removeMedia(connection, pathToFolder, true, true);
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

	/**
	 * Removes a single media file from the database.
	 *
	 * It will remove all the related contained entries.
	 * @param filename the filename.
	 */
	public static void removeEntry(String filename) {
		if (StringUtils.isBlank(filename)) {
			return;
		}
		try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
			removeEntry(connection, filename);
		} catch (Exception e) {
			LOGGER.error("cannot remove entry {} ", filename, e);
		}
	}

	/**
	 * Removes a single media file from the database.
	 *
	 * It will remove all the related contained entries.
	 * @param connection the db connection
	 * @param filename the filename.
	 */
	private static void removeEntry(final Connection connection, String filename) {
		if (connection == null || StringUtils.isBlank(filename)) {
			return;
		}
		Long fileId = getFileId(connection, filename);
		if (fileId != null) {
			removeEntry(connection, fileId);
		}
	}

	/**
	 * Removes a single media file from the database.
	 *
	 * It will remove all the related contained entries.
	 * @param connection the db connection
	 * @param fileId the file Id.
	 */
	protected static void removeEntry(final Connection connection, long fileId) {
		if (connection == null) {
			return;
		}
		//get actual contained entries.
		List<Long> entries = MediaTableContainerFiles.getContainerFileIds(connection, fileId);
		//remove relation.
		MediaTableContainerFiles.deleteContainer(connection, fileId);
		//delete the entry if not anymore related to a container
		for (Long entryId : entries) {
			if (!MediaTableContainerFiles.isInContainer(connection, entryId)) {
				removeEntry(connection, entryId);
			}
		}
		//remove the itself relation if any
		MediaTableContainerFiles.deleteEntry(connection, fileId);
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_DELETE_BY_ID);
			) {
				ps.setLong(1, fileId);
				ps.executeUpdate();
			}
		} catch (SQLException se) {
			LOGGER.error("An error occurred while trying to remove \"{}\" from the database: {}", fileId, se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static void updateThumbnailId(final Connection connection, long fileId, Long thumbId, String thumbnailSource) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_THUMBID_BY_ID);
			) {
				ps.setLong(1, thumbId);
				ps.setString(2, thumbnailSource);
				ps.setLong(3, fileId);
				ps.executeUpdate();
				LOGGER.trace("THUMBID updated to {} for {}", thumbId, fileId);
			}
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", se.getMessage());
			LOGGER.trace("", se);
		}
	}

	public static void resetLocalizedThumbnail(final Connection connection) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_THUMB_SRC_LOC);
			) {
				ps.setString(1, ThumbnailSource.TMDB.toString());
				ps.setString(2, ThumbnailSource.TMDB_LOC.toString());
				ps.executeUpdate();
				LOGGER.trace("Thumbnail source updated from {} to {}", ThumbnailSource.TMDB_LOC.toString(), ThumbnailSource.TMDB.toString());
			}
		} catch (SQLException se) {
			LOGGER.error("Error updating thumbnail source: {}", se.getMessage());
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
					List<Long> removedIds = new ArrayList<>();
					int oldpercent = 0;
					int i = 0;
					while (rs.next()) {
						String filename = rs.getString(COL_FILENAME);
						Long id = toLong(rs, COL_ID);
						if (Boolean.FALSE.equals(MediaTableContainerFiles.isInContainer(connection, id))) {
							if (!FileUtil.isUrl(filename)) {
								//this is a real file not in container
								long modified = rs.getTimestamp(COL_MODIFIED).getTime();
								File file = new File(filename);
								if (!file.exists() || file.lastModified() != modified) {
									LOGGER.trace("Removing the file {} from our database because it is no longer on the hard drive", filename);
									rs.deleteRow();
									removedIds.add(id);
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
										removedIds.add(id);
									}
								}
							} else {
								//check for url shared content
							}
						}
						i++;
						int newpercent = i * 100 / dbCount;
						if (newpercent > oldpercent) {
							GuiManager.setStatusLine(Messages.getString("CleaningUpDatabase") + newpercent + "%");
							oldpercent = newpercent;
						}
					}
					//clean relations
					for (Long id : removedIds) {
						removeEntry(connection, id);
					}
				}
				GuiManager.setStatusLine(null);
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
			 * Removes entries that are not referenced by any rows in the VIDEO_METADATA table.
			 */
			MediaTableTVSeries.cleanup(connection);

			/*
			 * Cleanup of THUMBNAILS table
			 *
			 * Removes entries that are not referenced by any rows in the FILES
			 * table or TV_SERIES table.
			 */
			MediaTableThumbnails.cleanup(connection);

		} catch (SQLException se) {
			LOGGER.error(null, se);
		} finally {
			GuiManager.setStatusLine(null);
		}
	}

	public static String getFilenameById(final Connection connection, final Long id) {
		if (id == null) {
			return null;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_FILENAME_BY_ID)) {
			statement.setLong(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getString(1);
				}
			}
		} catch (SQLException se) {
			LOGGER.error(null, se);
		}
		return null;
	}

	public static List<String> getFilenamesInFolder(final Connection connection, final String fullPathToFolder) {
		List<String> result = new ArrayList<>();
		if (StringUtils.isBlank(fullPathToFolder)) {
			return result;
		}
		try (PreparedStatement ps = connection.prepareStatement(SQL_GET_FILENAME_LIKE)) {
			ps.setString(1, fullPathToFolder);
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

		try (PreparedStatement ps = connection.prepareStatement(SQL_GET_THUMBNAIL_BY_TITLE)) {
			ps.setString(1, title);
			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", ps);
			}
			try (
				ResultSet rs = ps.executeQuery();
			) {
				if (rs.next()) {
					return ThumbnailStore.getThumbnail(rs.getLong(TABLE_COL_THUMBID));
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
		String psSql = sql.toUpperCase().startsWith(SELECT) || sql.toUpperCase().startsWith(WITH) ? sql : (SELECT + TABLE_COL_FILENAME + COMMA + TABLE_COL_MODIFIED + FROM + TABLE_NAME + WHERE + sql);
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(psSql);
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
			LOGGER.trace("Error get files with sql: {}", psSql);
			LOGGER.error(null, se);
			return list;
		}
		return list;
	}

}
