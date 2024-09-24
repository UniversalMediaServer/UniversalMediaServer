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

import com.google.gson.Gson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.external.umsapi.APIUtils;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.media.video.metadata.VideoMetadataLocalized;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableTVSeries extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableTVSeries.class);
	private static final Gson GSON = new Gson();
	public static final String TABLE_NAME = "TV_SERIES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 *
	 * Version notes:
	 * - 10: added thumbnail source
	 * - 11: removed SIMPLIFIEDTITLE
	 * - 12: ensure STARTYEAR and ENDYEAR are year, then convert to INTEGER
	 *		ensure FIRSTAIRDATE and LASTAIRDATE are date, then convert to DATE
	 *		set back 1 to 1 values to table (POSTER, RATED, RATING, RELEASEDATE)
	 *		use CLOB for very large string
	 *		remove unused metadata tables PRODUCTION and 1 to 1 values tables
	 */
	private static final int TABLE_VERSION = 12;

	/**
	 * COLUMNS
	 */
	public static final String COL_API_VERSION = "API_VERSION";
	public static final String COL_ID = "ID";
	private static final String COL_IMAGES = "IMAGES";
	private static final String COL_IMDBID = "IMDBID";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_OVERVIEW = "OVERVIEW";
	private static final String COL_THUMBID = "THUMBID";
	private static final String COL_THUMB_SRC = "THUMB_SRC";
	private static final String COL_TMDBID = "TMDBID";
	private static final String COL_STARTYEAR = "STARTYEAR";
	private static final String COL_ENDYEAR = "ENDYEAR";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_CREATEDBY = "CREATEDBY";
	private static final String COL_TOTALSEASONS = "TOTALSEASONS";
	private static final String COL_VOTES = "VOTES";
	private static final String COL_CREDITS = "CREDITS";
	private static final String COL_EXTERNALIDS = "EXTERNALIDS";
	private static final String COL_FIRSTAIRDATE = "FIRSTAIRDATE";
	private static final String COL_HOMEPAGE = "HOMEPAGE";
	private static final String COL_INPRODUCTION = "INPRODUCTION";
	private static final String COL_LANGUAGES = "LANGUAGES";
	private static final String COL_LASTAIRDATE = "LASTAIRDATE";
	private static final String COL_NETWORKS = "NETWORKS";
	private static final String COL_NUMBEROFEPISODES = "NUMBEROFEPISODES";
	private static final String COL_NUMBEROFSEASONS = "NUMBEROFSEASONS";
	private static final String COL_ORIGINCOUNTRY = "ORIGINCOUNTRY";
	private static final String COL_ORIGINALLANGUAGE = "ORIGINALLANGUAGE";
	private static final String COL_ORIGINALTITLE = "ORIGINALTITLE";
	private static final String COL_POSTER = "POSTER";
	private static final String COL_PRODUCTIONCOMPANIES = "PRODUCTIONCOMPANIES";
	private static final String COL_PRODUCTIONCOUNTRIES = "PRODUCTIONCOUNTRIES";
	public static final String COL_RATED = "RATED";
	private static final String COL_RATING = "RATING";
	private static final String COL_SEASONS = "SEASONS";
	private static final String COL_SERIESTYPE = "SERIESTYPE";
	private static final String COL_SPOKENLANGUAGES = "SPOKENLANGUAGES";
	private static final String COL_STATUS = "STATUS";
	private static final String COL_TAGLINE = "TAGLINE";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_FIRSTAIRDATE = TABLE_NAME + "." + COL_FIRSTAIRDATE;
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_IMAGES = TABLE_NAME + "." + COL_IMAGES;
	public static final String TABLE_COL_IMDBID = TABLE_NAME + "." + COL_IMDBID;
	private static final String TABLE_COL_POSTER = TABLE_NAME + "." + COL_POSTER;
	public static final String TABLE_COL_RATED = TABLE_NAME + "." + COL_RATED;
	public static final String TABLE_COL_RATING = TABLE_NAME + "." + COL_RATING;
	public static final String TABLE_COL_STARTYEAR = TABLE_NAME + "." + COL_STARTYEAR;
	public static final String TABLE_COL_TITLE = TABLE_NAME + "." + COL_TITLE;
	public static final String TABLE_COL_THUMBID = TABLE_NAME + "." + COL_THUMBID;
	private static final String TABLE_COL_TMDBID = TABLE_NAME + "." + COL_TMDBID;

	/**
	 * SQL Jointures
	 */
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES = LEFT_JOIN + MediaTableVideoMetadataGenres.TABLE_NAME + ON + TABLE_COL_ID + EQUAL + MediaTableVideoMetadataGenres.TABLE_COL_TVSERIESID;

	/**
	 * SQL References
	 */
	public static final String REFERENCE_TABLE_COL_ID = TABLE_NAME + "(" + COL_ID + ")";

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_BY_ID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_BY_TITLE = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_TITLE + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_BY_TITLE_YEAR = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_TITLE + EQUAL + PARAMETER + AND + TABLE_COL_STARTYEAR + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_BY_IMDBID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_IMDBID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_BY_TMDBID = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_TMDBID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_BY_IMDBID = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_IMDBID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_BY_IMDBID_API_VERSION = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + TABLE_COL_IMDBID + EQUAL + PARAMETER + AND + COL_API_VERSION + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_BY_SIMPLIFIEDTITLE = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + "REGEXP_REPLACE(LOWER(" + COL_TITLE + "), '[^a-z0-9]', '')" + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_BY_SIMPLIFIEDTITLE_YEAR = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + "REGEXP_REPLACE(LOWER(" + COL_TITLE + "), '[^a-z0-9]', '')" + EQUAL + PARAMETER + AND + TABLE_COL_STARTYEAR + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_BY_ORIGINALTITLE = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + "REGEXP_REPLACE(LOWER(" + COL_ORIGINALTITLE + "), '[^a-z0-9]', '')" + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_ID_BY_ORIGINALTITLE_YEAR = SELECT + TABLE_COL_ID + FROM + TABLE_NAME + WHERE + "REGEXP_REPLACE(LOWER(" + COL_ORIGINALTITLE + "), '[^a-z0-9]', '')" + EQUAL + PARAMETER + AND + TABLE_COL_STARTYEAR + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_TITLE_BY_ID = SELECT + TABLE_COL_TITLE + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_TMDBID_BY_ID = SELECT + TABLE_COL_TMDBID + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_IMAGES_BY_ID = SELECT + TABLE_COL_IMAGES + FROM + TABLE_NAME + WHERE + TABLE_COL_ID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_UPDATE_THUMBID = UPDATE + TABLE_NAME + SET + COL_THUMBID + EQUAL + PARAMETER + COMMA + COL_THUMB_SRC + EQUAL + PARAMETER + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_THUMB_SRC_LOC = UPDATE + TABLE_NAME + SET + COL_THUMB_SRC + EQUAL + PARAMETER + WHERE + COL_THUMB_SRC + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_TMDBID = UPDATE + TABLE_NAME + SET + COL_TMDBID + EQUAL + PARAMETER + WHERE + COL_ID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_IMDBID_TMDBID_NULL = UPDATE + TABLE_NAME + SET + COL_IMDBID + EQUAL + NULL + ", " + COL_TMDBID + EQUAL + NULL + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
	private static final String SQL_GET_PARTIALLY_PLAYED = SELECT + MediaTableVideoMetadata.TABLE_COL_TITLE + FROM + MediaTableFiles.TABLE_NAME + MediaTableFiles.SQL_LEFT_JOIN_TABLE_FILES_STATUS + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA + WHERE + MediaTableFiles.TABLE_COL_FORMAT_TYPE + EQUAL + "4" + AND + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE + AND + MediaTableVideoMetadata.TABLE_COL_TITLE + EQUAL + PARAMETER + AND + MediaTableFilesStatus.TABLE_COL_USERID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_NOT_FULLYPLAYED = SELECT + MediaTableVideoMetadata.TABLE_COL_TITLE + FROM + MediaTableFiles.TABLE_NAME + MediaTableFiles.SQL_LEFT_JOIN_TABLE_FILES_STATUS + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA + WHERE + MediaTableFiles.TABLE_COL_FORMAT_TYPE + EQUAL + "4" + AND + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE + AND + MediaTableVideoMetadata.TABLE_COL_TITLE + EQUAL + PARAMETER + AND + MediaTableFilesStatus.TABLE_COL_ISFULLYPLAYED + IS_NOT_TRUE + AND + MediaTableFilesStatus.TABLE_COL_USERID + EQUAL + PARAMETER + LIMIT_1;

	/**
	 * Used by child tables
	 */
	public static final String CHILD_ID = "TVSERIESID";
	public static final String FIRSTAIRDATE_FORMATED = "FORMATDATETIME(" + TABLE_COL_FIRSTAIRDATE + ", 'yyyy')";
	public static final String FLOOR_RATING = "CAST(FLOOR(" + TABLE_COL_RATING + ") AS INT)";

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
	private static void upgradeTable(final Connection connection, final int currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					try (Statement statement = connection.createStatement()) {
						if (!isColumnExist(connection, TABLE_NAME, "VERSION")) {
							statement.execute(ALTER_TABLE + TABLE_NAME + ADD + "VERSION VARCHAR");
							statement.execute(CREATE_INDEX + "IMDBID_VERSION " + ON + TABLE_NAME + "(IMDBID, VERSION)");
						}
					} catch (SQLException e) {
						LOGGER.error("Failed upgrading database table {} for {}", TABLE_NAME, e.getMessage());
						LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
						throw new SQLException(e);
					}
				}
				case 2 -> {
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME +  ALTER_COLUMN + "`YEAR`" + RENAME_TO + "MEDIA_YEAR");
					}
				}
				case 3 -> {
					LOGGER.trace("Adding TMDB columns");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "CREATEDBY VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "CREDITS VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "EXTERNALIDS VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "FIRSTAIRDATE VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "HOMEPAGE VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "IMAGES VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "INPRODUCTION VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "LANGUAGES VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "LASTAIRDATE VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "NETWORKS VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "NUMBEROFEPISODES DOUBLE PRECISION");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "NUMBEROFSEASONS DOUBLE PRECISION");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "ORIGINCOUNTRY VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "ORIGINALLANGUAGE VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "ORIGINALTITLE VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "PRODUCTIONCOMPANIES VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "PRODUCTIONCOUNTRIES VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "SEASONS VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "SERIESTYPE VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "SPOKENLANGUAGES VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "STATUS VARCHAR");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + "TAGLINE VARCHAR");
				}
				case 4 -> {
					// This version was for testing, left here to not break tester dbs
				}
				case 5 -> {
					if (isColumnExist(connection, TABLE_NAME, COL_INPRODUCTION)) {
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + " DROP COLUMN " + COL_INPRODUCTION);
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + COL_INPRODUCTION + " BOOLEAN");
					}
				}
				case 6 -> {
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + "_" + COL_THUMBID + "_IDX " + ON + TABLE_NAME + "(" + COL_THUMBID + ")");

					//set old json datas to be rescanned
					if (isColumnExist(connection, TABLE_NAME, "VERSION")) {
						String[] badJsonColumns = {COL_LANGUAGES, COL_ORIGINCOUNTRY};
						for (String badJsonColumn : badJsonColumns) {
							if (isColumnExist(connection, TABLE_NAME, badJsonColumn)) {
								executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_IMDBID + EQUAL + NULL + WHERE + "RIGHT(" + badJsonColumn + ", 1) = ','");
							}
						}
					}
				}
				case 7 -> {
					//remove old index
					executeUpdate(connection, "DROP INDEX IF EXISTS IMDBID_VERSION");
					//change VERSION to API_VERSION
					if (!isColumnExist(connection, TABLE_NAME, COL_API_VERSION)) {
						executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "VERSION" + RENAME_TO + COL_API_VERSION);
					}
					//add tmdb id
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_TMDBID + " BIGINT");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + "_" + COL_TMDBID + "_IDX " + ON + TABLE_NAME + "(" + COL_TMDBID + ")");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + "_" + COL_TMDBID + "_" + COL_IMDBID + "_" + COL_API_VERSION + "_IDX " + ON + TABLE_NAME + "(" + COL_TMDBID + ", " + COL_IMDBID + ", " + COL_API_VERSION + ")");

					//uniformizing indexes name
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "IMDBID_IDX" + RENAME_TO + TABLE_NAME + "_" + COL_IMDBID + "_IDX");
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "TITLE_IDX" + RENAME_TO + TABLE_NAME + "_" + COL_TITLE + "_IDX");
					executeUpdate(connection, ALTER_INDEX + IF_EXISTS + "SIMPLIFIEDTITLE_IDX" + RENAME_TO + TABLE_NAME + "_SIMPLIFIEDTITLE_IDX");
					//change PLOT to OVERVIEW
					if (!isColumnExist(connection, TABLE_NAME, COL_OVERVIEW)) {
						executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME +  ALTER_COLUMN + IF_EXISTS + "PLOT" + RENAME_TO + COL_OVERVIEW);
					}
				}
				case 8 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_MODIFIED + BIGINT);
					if (!isColumnExist(connection, TABLE_NAME, COL_TMDBID)) {
						executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME +  ALTER_COLUMN + IF_EXISTS + "TMDB_ID" + RENAME_TO + COL_TMDBID);
					}
				}
				case 9 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_THUMB_SRC + VARCHAR_32);
				}
				case 10 -> {
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + TABLE_NAME + TABLE_NAME + CONSTRAINT_SEPARATOR + "SIMPLIFIEDTITLE" + IDX_MARKER);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "SIMPLIFIEDTITLE");
					executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_TMDBID + EQUAL + NULL + WHERE + COL_TMDBID + EQUAL_0);
				}
				case 11 -> {
					//add poster data if any then remove table VIDEO_METADATA_POSTERS
					executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_POSTER + VARCHAR);
					if (tableExists(connection, "VIDEO_METADATA_POSTERS")) {
						executeUpdate(connection, UPDATE + TABLE_NAME + SET + TABLE_COL_POSTER + EQUAL + "(SELECT VIDEO_METADATA_POSTERS." + COL_POSTER + FROM + "VIDEO_METADATA_POSTERS" + WHERE + "VIDEO_METADATA_POSTERS." + CHILD_ID + EQUAL + TABLE_COL_ID + LIMIT_1 + ")");
						executeUpdate(connection, DROP_TABLE + "VIDEO_METADATA_POSTERS");
						MediaTableTablesVersions.removeTableVersion(connection, "VIDEO_METADATA_POSTERS");
					}
					//remove unused table VIDEO_METADATA_PRODUCTION
					if (tableExists(connection, "VIDEO_METADATA_PRODUCTION")) {
						executeUpdate(connection, DROP_TABLE + "VIDEO_METADATA_PRODUCTION");
						MediaTableTablesVersions.removeTableVersion(connection, "VIDEO_METADATA_PRODUCTION");
					}
					//add rated value if any
					executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_RATED + VARCHAR);
					if (tableExists(connection, "VIDEO_METADATA_RATED")) {
						executeUpdate(connection, UPDATE + TABLE_NAME + SET + TABLE_COL_RATED + EQUAL + "(SELECT VIDEO_METADATA_RATED." + COL_RATED + FROM + "VIDEO_METADATA_RATED" + WHERE + "VIDEO_METADATA_RATED." + CHILD_ID + EQUAL + TABLE_COL_ID + LIMIT_1 + ")");
						executeUpdate(connection, DROP_TABLE + "VIDEO_METADATA_RATED");
						MediaTableTablesVersions.removeTableVersion(connection, "VIDEO_METADATA_RATED");
					}
					//add rating if any then remove table VIDEO_METADATA_IMDB_RATING
					executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_RATING + DOUBLE_PRECISION);
					if (tableExists(connection, "VIDEO_METADATA_IMDB_RATING")) {
						String prepSql = UPDATE + TABLE_NAME + SET + COL_RATING + EQUAL + PARAMETER + WHERE + COL_ID + EQUAL + PARAMETER;
						try (
							PreparedStatement ps = connection.prepareStatement(SELECT_ALL + FROM + "VIDEO_METADATA_IMDB_RATING" + WHERE + CHILD_ID + IS_NOT_NULL);
							ResultSet rs = ps.executeQuery()
						) {
							while (rs.next()) {
								Long fileId = rs.getLong(CHILD_ID);
								String ratingStr = rs.getString("IMDBRATING");
								try {
									Double rating = Double.valueOf(ratingStr);
									if (rating > 0) {
										try (PreparedStatement udpdateStatement = connection.prepareStatement(prepSql)) {
											udpdateStatement.setDouble(1, rating);
											udpdateStatement.setLong(2, fileId);
											udpdateStatement.execute();
										}
									}
								} catch (NullPointerException | NumberFormatException e) {
									//nothing to do
								}
							}
						}
						//remove the VIDEO_METADATA_IMDB_RATING table
						executeUpdate(connection, DROP_TABLE + "VIDEO_METADATA_IMDB_RATING");
						MediaTableTablesVersions.removeTableVersion(connection, "VIDEO_METADATA_IMDB_RATING");
					}
					//add release date to first air date if any then remove table VIDEO_METADATA_RELEASED
					if (tableExists(connection, "VIDEO_METADATA_RELEASED")) {
						String prepSql = UPDATE + TABLE_NAME + SET + COL_FIRSTAIRDATE + EQUAL + PARAMETER + WHERE + TABLE_COL_ID + EQUAL + PARAMETER;
						try (
							PreparedStatement ps = connection.prepareStatement(SELECT + TABLE_COL_ID + COMMA + TABLE_COL_FIRSTAIRDATE + COMMA + "VIDEO_METADATA_RELEASED.RELEASEDATE" + FROM + TABLE_NAME + JOIN + "VIDEO_METADATA_RELEASED" + ON + TABLE_COL_ID + EQUAL + "VIDEO_METADATA_RELEASED.TVSERIESID");
							ResultSet rs = ps.executeQuery()
						) {
							while (rs.next()) {
								Long tvSeriesId = rs.getLong(COL_ID);
								String firstAirDate = rs.getString(COL_FIRSTAIRDATE);
								String releaseDate = rs.getString("RELEASEDATE");
								if (firstAirDate == null && releaseDate != null) {
									try (PreparedStatement udpdateStatement = connection.prepareStatement(prepSql)) {
										udpdateStatement.setString(1, StringUtils.left(StringUtils.trimToEmpty(releaseDate), 10));
										udpdateStatement.setLong(2, tvSeriesId);
										udpdateStatement.execute();
									} catch (IllegalArgumentException e) {
										//nothing to do.
									}
								}
							}
						}
						executeUpdate(connection, DROP_TABLE + "VIDEO_METADATA_RELEASED");
						MediaTableTablesVersions.removeTableVersion(connection, "VIDEO_METADATA_RELEASED");
					}
					//use CLOB for very large string
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_CREDITS + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_EXTERNALIDS + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_IMAGES + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_NETWORKS + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_OVERVIEW + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_PRODUCTIONCOMPANIES + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_PRODUCTIONCOUNTRIES + CLOB);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_SEASONS + CLOB);
					//ensure STARTYEAR and ENDYEAR are year, then convert STARTYEAR and ENDYEAR to Integer
					for (String column : new String[] {COL_STARTYEAR, COL_ENDYEAR}) {
						try (
							PreparedStatement ps = connection.prepareStatement(SELECT_ALL + FROM + TABLE_NAME + WHERE + column + IS_NOT_NULL,
								ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
							ResultSet rs = ps.executeQuery()
						) {
							while (rs.next()) {
								String yearStr = rs.getString(column);
								Integer year = FileUtil.getYearFromYearString(yearStr);
								if (year != null) {
									rs.updateString(column, year.toString());
								} else {
									rs.updateNull(column);
								}
								rs.updateRow();
							}
						}
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + column + INTEGER);
					}
					//ensure FIRSTAIRDATE and LASTAIRDATE are date, then convert to DATE
					for (String column : new String[] {COL_FIRSTAIRDATE, COL_LASTAIRDATE}) {
						try (
							PreparedStatement ps = connection.prepareStatement(SELECT_ALL + FROM + TABLE_NAME + WHERE + column + IS_NOT_NULL,
								ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
							ResultSet rs = ps.executeQuery()
						) {
							while (rs.next()) {
								String dateStr = rs.getString(column);
								if (dateStr != null && dateStr.length() > 10) {
									dateStr = dateStr.substring(0, 10);
								}
								LocalDate localDate = null;
								try {
									localDate = LocalDate.parse(dateStr);
								} catch (DateTimeParseException e) {
									//nothing to do
								}
								if (localDate != null) {
									rs.updateString(column, localDate.toString());
								} else {
									rs.updateNull(column);
								}
								rs.updateRow();
							}
						}
						executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + column + DATE);
					}
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
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + "(" +
				COL_ID                     + IDENTITY     + PRIMARY_KEY  + COMMA +
				COL_MODIFIED               + BIGINT                      + COMMA +
				COL_IMDBID                 + VARCHAR_1024                + COMMA +
				COL_TMDBID                 + BIGINT                      + COMMA +
				COL_THUMBID                + BIGINT                      + COMMA +
				COL_THUMB_SRC              + VARCHAR_32                  + COMMA +
				COL_TITLE                  + VARCHAR_1024 + NOT_NULL     + COMMA +
				COL_STARTYEAR              + INTEGER                     + COMMA +
				COL_ENDYEAR                + INTEGER                     + COMMA +
				COL_TOTALSEASONS           + DOUBLE_PRECISION            + COMMA +
				COL_API_VERSION            + VARCHAR_1024                + COMMA +
				COL_VOTES                  + VARCHAR_1024                + COMMA +
				COL_CREATEDBY              + VARCHAR                     + COMMA +
				COL_CREDITS                + CLOB                        + COMMA +
				COL_EXTERNALIDS            + CLOB                        + COMMA +
				COL_FIRSTAIRDATE           + DATE                        + COMMA +
				COL_HOMEPAGE               + VARCHAR                     + COMMA +
				COL_IMAGES                 + CLOB                        + COMMA +
				COL_INPRODUCTION           + BOOLEAN                     + COMMA +
				COL_LANGUAGES              + VARCHAR                     + COMMA +
				COL_LASTAIRDATE            + DATE                        + COMMA +
				COL_NETWORKS               + CLOB                        + COMMA +
				COL_NUMBEROFEPISODES       + DOUBLE_PRECISION            + COMMA +
				COL_NUMBEROFSEASONS        + DOUBLE_PRECISION            + COMMA +
				COL_ORIGINCOUNTRY          + VARCHAR                     + COMMA +
				COL_ORIGINALLANGUAGE       + VARCHAR                     + COMMA +
				COL_ORIGINALTITLE          + VARCHAR                     + COMMA +
				COL_OVERVIEW               + CLOB                        + COMMA +
				COL_POSTER                 + VARCHAR                     + COMMA +
				COL_PRODUCTIONCOMPANIES    + CLOB                        + COMMA +
				COL_PRODUCTIONCOUNTRIES    + CLOB                        + COMMA +
				COL_RATING                 + DOUBLE_PRECISION            + COMMA +
				COL_RATED                  + VARCHAR                     + COMMA +
				COL_SEASONS                + CLOB                        + COMMA +
				COL_SERIESTYPE             + VARCHAR                     + COMMA +
				COL_SPOKENLANGUAGES        + VARCHAR                     + COMMA +
				COL_STATUS                 + VARCHAR                     + COMMA +
				COL_TAGLINE                + VARCHAR                             +
			")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_IMDBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_IMDBID + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TMDBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_TMDBID + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TMDBID + CONSTRAINT_SEPARATOR + COL_IMDBID + CONSTRAINT_SEPARATOR + COL_API_VERSION + IDX_MARKER + ON + TABLE_NAME + "(" + COL_TMDBID + COMMA + COL_IMDBID + COMMA + COL_API_VERSION + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TITLE + IDX_MARKER + ON + TABLE_NAME + "(" + COL_TITLE + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_THUMBID + IDX_MARKER + ON + TABLE_NAME + "(" + COL_THUMBID + ")"
		);
	}

	/**
	 * Sets a new entry if not found and returns the row ID.
	 *
	 * @param connection the db connection
	 * @param title the title of the series
	 * @return the new row ID
	 */
	public static Long set(final Connection connection, final String title, final Integer startYear) {
		if (StringUtils.isBlank(title)) {
			LOGGER.debug("Attempted to set TV series info with no series title");
			return null;
		}
		String sql = SQL_GET_BY_TITLE;
		if (startYear != null) {
			sql = SQL_GET_BY_TITLE_YEAR;
		}

		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				selectStatement.setString(1, title);
				if (startYear != null) {
					selectStatement.setInt(2, startYear);
				}
				try (ResultSet result = selectStatement.executeQuery()) {
					if (result.next()) {
						if (trace) {
							LOGGER.trace("Found entry in " + TABLE_NAME);
						}
						return result.getLong("ID");
					} else {
						if (trace) {
							LOGGER.trace("Entry \"{}\" not found in " + TABLE_NAME + ", inserting", title);
						}
						result.moveToInsertRow();
						result.updateString(COL_TITLE, title);
						if (startYear != null) {
							result.updateInt(COL_STARTYEAR, startYear);
						}
						result.insertRow();
						return set(connection, title, startYear);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing", TABLE_NAME, "tv Series", e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Updates an existing row with information from our API.
	 *
	 * @param connection the db connection
	 * @param seriesMetadata
	 * @param tvSeriesId
	 */
	public static void updateAPIMetadata(final Connection connection, final TvSeriesMetadata seriesMetadata, final Long tvSeriesId) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return;
		}
		if (seriesMetadata == null) {
			LOGGER.warn("Couldn't write API data for \"{}\" to the database because there is no media information");
			return;
		}
		String title = seriesMetadata.getTitle();
		try {
			try (PreparedStatement ps = connection.prepareStatement(SQL_GET_BY_ID, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)) {
				ps.setLong(1, tvSeriesId);
				LOGGER.trace("Inserting API metadata for TVSeries " + title);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						rs.updateString(COL_IMDBID, seriesMetadata.getIMDbID());
						rs.updateString(COL_TITLE, title);
						rs.updateString(COL_API_VERSION, seriesMetadata.getApiVersion());
						rs.updateLong(COL_MODIFIED, System.currentTimeMillis());
						if (seriesMetadata.getCreatedBy() != null) {
							rs.updateString(COL_CREATEDBY, GSON.toJson(seriesMetadata.getCreatedBy()));
						}
						if (seriesMetadata.getCredits() != null) {
							rs.updateString(COL_CREDITS, GSON.toJson(seriesMetadata.getCredits()));
						}
						updateInteger(rs, COL_ENDYEAR, seriesMetadata.getEndYear());
						if (seriesMetadata.getExternalIDs() != null) {
							rs.updateString(COL_EXTERNALIDS, GSON.toJson(seriesMetadata.getExternalIDs()));
						}
						updateDate(rs, COL_FIRSTAIRDATE, seriesMetadata.getFirstAirDate());
						rs.updateString(COL_HOMEPAGE, seriesMetadata.getHomepage());
						if (seriesMetadata.getImages() != null) {
							rs.updateString(COL_IMAGES, GSON.toJson(seriesMetadata.getImages()));
						}
						if (seriesMetadata.isInProduction() != null) {
							rs.updateBoolean(COL_INPRODUCTION, seriesMetadata.isInProduction());
						}
						if (seriesMetadata.getLanguages() != null) {
							rs.updateString(COL_LANGUAGES, GSON.toJson(seriesMetadata.getLanguages()));
						}
						updateDate(rs, COL_LASTAIRDATE, seriesMetadata.getLastAirDate());
						if (seriesMetadata.getNetworks() != null) {
							rs.updateString(COL_NETWORKS, GSON.toJson(seriesMetadata.getNetworks()));
						}
						updateDouble(rs, COL_NUMBEROFEPISODES, seriesMetadata.getNumberOfEpisodes());
						updateDouble(rs, COL_NUMBEROFSEASONS, seriesMetadata.getNumberOfSeasons());
						if (seriesMetadata.getOriginCountry() != null) {
							rs.updateString(COL_ORIGINCOUNTRY, GSON.toJson(seriesMetadata.getOriginCountry()));
						}
						rs.updateString(COL_ORIGINALLANGUAGE, seriesMetadata.getOriginalLanguage());
						rs.updateString(COL_ORIGINALTITLE, seriesMetadata.getOriginalTitle());
						rs.updateString(COL_OVERVIEW, seriesMetadata.getOverview());
						rs.updateString(COL_POSTER, seriesMetadata.getPoster());
						if (seriesMetadata.getProductionCompanies() != null) {
							rs.updateString(COL_PRODUCTIONCOMPANIES, GSON.toJson(seriesMetadata.getProductionCompanies()));
						}
						if (seriesMetadata.getProductionCountries() != null) {
							rs.updateString(COL_PRODUCTIONCOUNTRIES, GSON.toJson(seriesMetadata.getProductionCountries()));
						}
						rs.updateString(COL_RATED, seriesMetadata.getRated());
						updateDouble(rs, COL_RATING, seriesMetadata.getRating());
						if (seriesMetadata.getSeasons() != null) {
							rs.updateString(COL_SEASONS, GSON.toJson(seriesMetadata.getSeasons()));
						}
						rs.updateString(COL_SERIESTYPE, seriesMetadata.getSeriesType());
						if (seriesMetadata.getSpokenLanguages() != null) {
							rs.updateString(COL_SPOKENLANGUAGES, GSON.toJson(seriesMetadata.getSpokenLanguages()));
						}
						updateInteger(rs, COL_STARTYEAR, seriesMetadata.getStartYear());
						rs.updateString(COL_STATUS, seriesMetadata.getStatus());
						rs.updateString(COL_TAGLINE, seriesMetadata.getTagline());
						updateLong(rs, COL_TMDBID, seriesMetadata.getTmdbId());
						if (seriesMetadata.getTotalSeasons() != null) {
							rs.updateDouble(COL_TOTALSEASONS, seriesMetadata.getTotalSeasons());
						}
						updateLong(rs, COL_THUMBID, seriesMetadata.getThumbnailId());
						if (seriesMetadata.getThumbnailSource() != null) {
							updateString(rs, COL_THUMB_SRC, seriesMetadata.getThumbnailSource().toString(), 32);
						}
						rs.updateString(COL_VOTES, seriesMetadata.getVotes());
						rs.updateRow();
						connection.commit();
					} else {
						LOGGER.debug("Couldn't find \"{}\" in the database when trying to store data from our API", title);
						return;
					}
				}
			}
			MediaTableVideoMetadataActors.set(connection, null, seriesMetadata.getActors(), tvSeriesId);
			MediaTableVideoMetadataAwards.set(connection, null, seriesMetadata.getAwards(), tvSeriesId);
			MediaTableVideoMetadataCountries.set(connection, null, seriesMetadata.getCountries(), tvSeriesId);
			MediaTableVideoMetadataDirectors.set(connection, null, seriesMetadata.getDirectors(), tvSeriesId);
			MediaTableVideoMetadataGenres.set(connection, null, seriesMetadata.getGenres(), tvSeriesId);
			MediaTableVideoMetadataRatings.set(connection, null, seriesMetadata.getRatings(), tvSeriesId);
			connection.commit();
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "inserting API data to TV series entry", title, TABLE_NAME, e.getMessage());
		}
	}

	public static TvSeriesMetadata getTvSeriesMetadata(final Connection connection, final Long tvSeriesId) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_BY_ID)) {
				selectStatement.setLong(1, tvSeriesId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						return getTvSeriesMetadata(connection, rs);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tvSeriesId, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	public static TvSeriesMetadata getTvSeriesMetadataFromImdbId(final Connection connection, final String imdbId) {
		if (connection == null || imdbId == null) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_BY_IMDBID)) {
				selectStatement.setString(1, imdbId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						return getTvSeriesMetadata(connection, rs);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", imdbId, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	public static TvSeriesMetadata getTvSeriesMetadataFromTmdbId(final Connection connection, final Long tmdbId) {
		if (connection == null || tmdbId == null) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_BY_TMDBID)) {
				selectStatement.setLong(1, tmdbId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						return getTvSeriesMetadata(connection, rs);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tmdbId, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	private static TvSeriesMetadata getTvSeriesMetadata(final Connection connection, final ResultSet resultSet) throws SQLException {
		Long tvSeriesId = resultSet.getLong(COL_ID);
		TvSeriesMetadata metadata = new TvSeriesMetadata();
		metadata.setTvSeriesId(tvSeriesId);
		metadata.setActors(MediaTableVideoMetadataActors.getActorsForTvSeries(connection, tvSeriesId));
		metadata.setApiVersion(resultSet.getString(COL_API_VERSION));
		metadata.setAwards(MediaTableVideoMetadataAwards.getValueForTvSeries(connection, tvSeriesId));
		metadata.setCountries(MediaTableVideoMetadataCountries.getCountriesForTvSeries(connection, tvSeriesId));
		metadata.setCreatedBy(resultSet.getString(COL_CREATEDBY));
		metadata.setCredits(resultSet.getString(COL_CREDITS));
		metadata.setDirectors(MediaTableVideoMetadataDirectors.getDirectorsForTvSeries(connection, tvSeriesId));
		metadata.setEndYear(toInteger(resultSet, COL_ENDYEAR));
		metadata.setExternalIDs(resultSet.getString(COL_EXTERNALIDS));
		metadata.setFirstAirDate(getLocalDate(resultSet, COL_FIRSTAIRDATE));
		metadata.setGenres(MediaTableVideoMetadataGenres.getGenresForTvSeries(connection, tvSeriesId));
		metadata.setHomepage(resultSet.getString(COL_HOMEPAGE));
		metadata.setImages(resultSet.getString(COL_IMAGES));
		metadata.setIMDbID(resultSet.getString(COL_IMDBID));
		metadata.setInProduction(resultSet.getBoolean(COL_INPRODUCTION));
		metadata.setLanguages(resultSet.getString(COL_LANGUAGES));
		metadata.setLastAirDate(getLocalDate(resultSet, COL_LASTAIRDATE));
		metadata.setNetworks(resultSet.getString(COL_NETWORKS));
		metadata.setNumberOfEpisodes(toDouble(resultSet, COL_NUMBEROFEPISODES));
		metadata.setNumberOfSeasons(toDouble(resultSet, COL_NUMBEROFSEASONS));
		metadata.setOriginalLanguage(resultSet.getString(COL_ORIGINALLANGUAGE));
		metadata.setOriginalTitle(resultSet.getString(COL_ORIGINALTITLE));
		metadata.setOriginCountry(resultSet.getString(COL_ORIGINCOUNTRY));
		metadata.setOverview(resultSet.getString(COL_OVERVIEW));
		metadata.setPoster(resultSet.getString(COL_POSTER));
		metadata.setProductionCompanies(resultSet.getString(COL_PRODUCTIONCOMPANIES));
		metadata.setProductionCountries(resultSet.getString(COL_PRODUCTIONCOUNTRIES));
		metadata.setRated(resultSet.getString(COL_RATED));
		metadata.setRating(toDouble(resultSet, COL_RATING));
		metadata.setRatings(MediaTableVideoMetadataRatings.getRatingsForTvSeries(connection, tvSeriesId));
		metadata.setSeasons(resultSet.getString(COL_SEASONS));
		metadata.setSeriesType(resultSet.getString(COL_SERIESTYPE));
		metadata.setSpokenLanguages(resultSet.getString(COL_SPOKENLANGUAGES));
		metadata.setStartYear(toInteger(resultSet, COL_STARTYEAR));
		metadata.setStatus(resultSet.getString(COL_STATUS));
		metadata.setTagline(resultSet.getString(COL_TAGLINE));
		metadata.setTitle(resultSet.getString(COL_TITLE));
		metadata.setTmdbId(toLong(resultSet, COL_TMDBID));
		metadata.setTotalSeasons(toDouble(resultSet, COL_TOTALSEASONS));
		metadata.setThumbnailId(toLong(resultSet, COL_THUMBID));
		metadata.setThumbnailSource(resultSet.getString(COL_THUMB_SRC));
		metadata.setVotes(resultSet.getString(COL_VOTES));
		metadata.setTranslations(MediaTableVideoMetadataLocalized.getAllVideoMetadataLocalized(connection, tvSeriesId, true));
		//ensure we have the default translation
		metadata.ensureHavingTranslation(null);
		//get localized thumb if thumb was not localized
		if (metadata.getPoster(null) != null &&
			!metadata.getThumbnailSource().equals(ThumbnailSource.TMDB_LOC)
			) {
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(metadata.getPoster(null));
			if (thumbnail != null) {
				Long thumbnailId = ThumbnailStore.getIdForTvSeries(thumbnail, tvSeriesId, ThumbnailSource.TMDB_LOC);
				metadata.setThumbnailSource(ThumbnailSource.TMDB_LOC);
				metadata.setThumbnailId(thumbnailId);
			}
		}
		return metadata;
	}

	/**
	 * Get TMDB ID by TV series title.
	 * @param connection the db connection
	 * @param tmdbId
	 * @return the title or null
	 */
	public static Long getTmdbIdByTitle(final Connection connection, final String title, final Integer startYear) {
		if (connection == null || StringUtils.isBlank(title)) {
			return null;
		}
		Long id = getIdBySimilarTitle(connection, title, startYear);
		if (id != null) {
			try {
				try (PreparedStatement statement = connection.prepareStatement(SQL_GET_TMDBID_BY_ID)) {
					statement.setLong(1, id);
					try (ResultSet resultSet = statement.executeQuery()) {
						if (resultSet.next()) {
							return toLong(resultSet, COL_TMDBID);
						}
					}
				}
			} catch (SQLException e) {
				LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading Tmdb Id from title", title, TABLE_NAME, e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return null;
	}

	/**
	 * Get TV series id by IMDb ID.
	 * If we have the latest version number from the
	 * API, narrow the result to that version.
	 * @param connection the db connection
	 * @param imdbID
	 * @return the tvSeriesId or null
	 */
	public static Long getIdByIMDbID(final Connection connection, final String imdbID) {
		String sql;
		String latestVersion = null;
		if (CONFIGURATION.getExternalNetwork()) {
			latestVersion = APIUtils.getApiDataSeriesVersion();
		}
		if (latestVersion != null) {
			sql = SQL_GET_ID_BY_IMDBID_API_VERSION;
		} else {
			sql = SQL_GET_ID_BY_IMDBID;
		}

		try {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, imdbID);
				if (latestVersion != null) {
					statement.setString(2, latestVersion);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return toLong(resultSet, COL_ID);
					} else {
						LOGGER.trace("Did not find tvSeriesId by IMDb ID using query: {}", statement.toString());
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading tv series from imdbID", imdbID, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Returns the images based on tvSeriesId.
	 *
	 * @param connection the db connection
	 * @param tvSeriesId
	 * @return
	 */
	public static String getImages(final Connection connection, final Long tvSeriesId) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return null;
		}
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_IMAGES_BY_ID)) {
				statement.setLong(1, tvSeriesId);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getString(COL_IMAGES);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading images from id", tvSeriesId, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Returns a row id based on title.
	 *
	 * Will look on title, then original title, then on localized titles
	 * @param connection the db connection
	 * @param title
	 * @return tvSeriesId
	 */
	public static Long getIdBySimilarTitle(final Connection connection, final String title, final Integer startYear) {
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		Long tvSeriesId = getIdBySimplifiedTitle(connection, simplifiedTitle, startYear);
		if (tvSeriesId == null) {
			tvSeriesId = getIdByOriginalTitle(connection, title, startYear);
		}
		if (tvSeriesId == null) {
			tvSeriesId = MediaTableVideoMetadataLocalized.getTvSeriesIdFromTitle(connection, simplifiedTitle);
		}
		return tvSeriesId;
	}

	/**
	 * Returns a row id based on title.
	 *
	 * @param connection the db connection
	 * @param simplifiedTitle
	 * @return tvSeriesId
	 */
	private static Long getIdBySimplifiedTitle(final Connection connection, final String simplifiedTitle, final Integer startYear) {
		String sql = SQL_GET_ID_BY_SIMPLIFIEDTITLE;
		if (startYear != null) {
			sql = SQL_GET_ID_BY_SIMPLIFIEDTITLE_YEAR;
		}

		try {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, simplifiedTitle);
				if (startYear != null) {
					statement.setInt(2, startYear);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getLong(COL_ID);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading id from title", simplifiedTitle, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Returns a row id based on original title.
	 *
	 * @param connection the db connection
	 * @param simplifiedTitle
	 * @return tvSeriesId
	 */
	private static Long getIdByOriginalTitle(final Connection connection, final String simplifiedTitle, final Integer startYear) {
		String sql = SQL_GET_ID_BY_ORIGINALTITLE;
		if (startYear != null) {
			sql = SQL_GET_ID_BY_ORIGINALTITLE_YEAR;
		}

		try (PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, simplifiedTitle);
			if (startYear != null) {
				statement.setInt(2, startYear);
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getLong(COL_ID);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading id from original title", simplifiedTitle, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	public static VideoMetadataLocalized getTvSeriesMetadataUnLocalized(final Connection connection, final Long tvSeriesId) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return null;
		}
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_BY_ID)) {
				selectStatement.setLong(1, tvSeriesId);
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						VideoMetadataLocalized result = new VideoMetadataLocalized();
						result.setHomepage(rs.getString(COL_HOMEPAGE));
						result.setOverview(rs.getString(COL_OVERVIEW));
						result.setTagline(rs.getString(COL_TAGLINE));
						result.setTitle(rs.getString(COL_TITLE));
						return result;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", tvSeriesId, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	public static void updateThumbnailId(final Connection connection, Long id, Long thumbId, String thumbnailSource) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_THUMBID);
			) {
				ps.setLong(1, thumbId);
				ps.setString(2, thumbnailSource);
				ps.setLong(3, id);
				ps.executeUpdate();
				LOGGER.trace("TV series THUMBID updated to {} for {}", thumbId, id);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "updating cached thumbnail", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
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

	private static void updateTmdbId(final Connection connection, final long tvSeriesId, final long tmdbId) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_TMDBID);
			) {
				ps.setLong(1, tmdbId);
				ps.setLong(2, tvSeriesId);
				ps.executeUpdate();
			}
		} catch (SQLException e) {
			LOGGER.error("Failed to update TMDB ID for \"{}\" to \"{}\": {}", tvSeriesId, tmdbId, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Returns a TV series name from the database.
	 *
	 * @param connection the db connection
	 * @param tvSeriesId
	 * @return
	 */
	public static String getTitleFromId(final Connection connection, Long tvSeriesId) {
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return null;
		}

		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_TITLE_BY_ID)) {
			statement.setLong(1, tvSeriesId);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					String str = resultSet.getString(COL_TITLE);
					return StringUtils.isBlank(str) ? MediaTableFiles.NONAME : str;
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "reading", TABLE_NAME, "SimilarTVSeriesName", e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Unset ImdbId and TmdbId for ID.
	 *
	 * @param connection the db connection
	 * @param id the ID to unset
	 */
	public static void unsetApiIdsForId(final Connection connection, final Long id) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_IMDBID_TMDBID_NULL)) {
				statement.setLong(1, id);
				int row = statement.executeUpdate();
				LOGGER.trace("Removed IMDb ID and TMDB ID from {} in " + TABLE_NAME + " for ID \"{}\"", row, id);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entry", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static Boolean isFullyPlayed(final Connection connection, final String title, final int userId) {
		try {
			/*
			 * If we don't have entry for this series, then this series is
			 * not fully played.
			 */
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_PARTIALLY_PLAYED)) {
				statement.setString(1, title);
				statement.setInt(2, userId);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", statement);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (!resultSet.next()) {
						return false;
					}
				}
			}

			/*
			 * If there is one file for this TV series where ISFULLYPLAYED is
			 * not true, then this series is not fully played, otherwise it is.
			 *
			 * This backwards logic is used for performance since we only have
			 * to check one row instead of all rows.
			 */
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_NOT_FULLYPLAYED)) {
				statement.setString(1, title);
				statement.setInt(2, userId);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", statement);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return false;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "looking up TV series status", TABLE_NAME, title, e.getMessage());
			LOGGER.trace("", e);
		}

		return true;
	}

	public static synchronized void cleanup(final Connection connection) {
		/*
		 * Cleanup of TV_SERIES table
		 *
		 * Removes entries that are not referenced by any rows in the VIDEO_METADATA table.
		 */
		try (
			PreparedStatement ps = connection.prepareStatement(
				DELETE_FROM + TABLE_NAME +
				WHERE + NOT + EXISTS + "(" +
					SELECT + MediaTableVideoMetadata.TABLE_COL_TVSERIESID + FROM + MediaTableVideoMetadata.TABLE_NAME +
					WHERE + MediaTableVideoMetadata.TABLE_COL_TVSERIESID + EQUAL + MediaTableTVSeries.TABLE_COL_ID +
					LIMIT_1 +
				");"
		)) {
			ps.execute();
		} catch (SQLException se) {
			LOGGER.error(null, se);
		}
	}

}
