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
import java.util.ArrayList;
import java.util.List;
import net.pms.external.umsapi.APIUtils;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.media.video.metadata.VideoMetadataLocalized;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing the Video Metadatas table. It
 * does everything from creating, checking and upgrading the table to performing
 * lookups, updates and inserts. All operations involving this table shall be
 * done with this class.
 */
public class MediaTableVideoMetadata extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadata.class);
	private static final Gson GSON = new Gson();
	public static final String TABLE_NAME = "VIDEO_METADATA";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 *
	 * Version notes:
	 * - 5: FILEID as BIGINT
	 * - 6: removed MOVIEORSHOWNAME
	 *		renamed MOVIEORSHOWNAMESIMPLE for TITLE
	 *		renamed TVEPISODENAME for TITLE
	 *		added TVSERIESID
	 */
	private static final int TABLE_VERSION = 6;

	/**
	 * COLUMNS NAMES
	 */
	private static final String COL_API_VERSION = "API_VERSION";
	private static final String COL_EXTRAINFORMATION = "EXTRAINFORMATION";
	protected static final String COL_FILEID = MediaTableFiles.CHILD_ID;
	private static final String COL_IMDBID = "IMDBID";
	private static final String COL_TMDBID = "TMDBID";
	private static final String COL_TMDBTVID = "TMDBTVID";
	private static final String COL_ISTVEPISODE = "ISTVEPISODE";
	private static final String COL_MEDIA_YEAR = "MEDIA_YEAR";
	private static final String COL_MODIFIED = "MODIFIED";
	private static final String COL_TITLE = "TITLE";
	private static final String COL_TVSERIESID = MediaTableTVSeries.CHILD_ID;
	private static final String COL_TVSEASON = "TVSEASON";
	private static final String COL_TVEPISODENUMBER = "TVEPISODENUMBER";
	private static final String BASIC_COLUMNS = COL_IMDBID + ", " + COL_MEDIA_YEAR + ", " + COL_TITLE + ", " + COL_TVSERIESID + ", " + COL_EXTRAINFORMATION + ", " + COL_ISTVEPISODE + ", " + COL_TVSEASON + ", " + COL_TVEPISODENUMBER;
	/**
	 * The columns we added from TMDB in V11
	 */
	private static final String COL_BUDGET = "BUDGET";
	private static final String COL_CREDITS = "CREDITS";
	private static final String COL_EXTERNALIDS = "EXTERNALIDS";
	private static final String COL_HOMEPAGE = "HOMEPAGE";
	private static final String COL_IMAGES = "IMAGES";
	private static final String COL_ORIGINALLANGUAGE = "ORIGINALLANGUAGE";
	private static final String COL_ORIGINALTITLE = "ORIGINALTITLE";
	private static final String COL_OVERVIEW = "OVERVIEW";
	private static final String COL_PRODUCTIONCOMPANIES = "PRODUCTIONCOMPANIES";
	private static final String COL_PRODUCTIONCOUNTRIES = "PRODUCTIONCOUNTRIES";
	private static final String COL_REVENUE = "REVENUE";
	private static final String COL_TAGLINE = "TAGLINE";
	private static final String COL_VOTES = "VOTES";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_API_VERSION = TABLE_NAME + "." + COL_API_VERSION;
	public static final String TABLE_COL_EXTRAINFORMATION = TABLE_NAME + "." + COL_EXTRAINFORMATION;
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_IMDBID = TABLE_NAME + "." + COL_IMDBID;
	public static final String TABLE_COL_TMDBID = TABLE_NAME + "." + COL_TMDBID;
	public static final String TABLE_COL_ISTVEPISODE = TABLE_NAME + "." + COL_ISTVEPISODE;
	public static final String TABLE_COL_MEDIA_YEAR = TABLE_NAME + "." + COL_MEDIA_YEAR;
	public static final String TABLE_COL_TITLE = TABLE_NAME + "." + COL_TITLE;
	public static final String TABLE_COL_TVSERIESID = TABLE_NAME + "." + COL_TVSERIESID;
	public static final String TABLE_COL_TVEPISODENUMBER = TABLE_NAME + "." + COL_TVEPISODENUMBER;
	public static final String TABLE_COL_TVSEASON = TABLE_NAME + "." + COL_TVSEASON;

	/**
	 * SQL Jointures
	 */
	public static final String SQL_LEFT_JOIN_TABLE_TV_SERIES = LEFT_JOIN + MediaTableTVSeries.TABLE_NAME + ON + TABLE_COL_TVSERIESID + EQUAL + MediaTableTVSeries.TABLE_COL_ID;
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES = LEFT_JOIN + MediaTableVideoMetadataGenres.TABLE_NAME + ON + TABLE_COL_FILEID + EQUAL + MediaTableVideoMetadataGenres.TABLE_COL_FILEID;
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_IMDB_RATING = LEFT_JOIN + MediaTableVideoMetadataIMDbRating.TABLE_NAME + ON + TABLE_COL_FILEID + EQUAL + MediaTableVideoMetadataIMDbRating.TABLE_COL_FILEID;
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_RATED = LEFT_JOIN + MediaTableVideoMetadataRated.TABLE_NAME + ON + TABLE_COL_FILEID + EQUAL + MediaTableVideoMetadataRated.TABLE_COL_FILEID;

	/**
	 * SQL References
	 */
	public static final String REFERENCE_TABLE_COL_FILE_ID = TABLE_NAME + "(" + COL_FILEID + ")";

	/**
	 * SQL Queries
	 */
	private static final String SQL_GET_VIDEO_METADATA_BY_FILEID = SELECT + COL_FILEID + COMMA + BASIC_COLUMNS + FROM + TABLE_NAME + WHERE + COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_VIDEO_ALL_METADATA_BY_FILEID = SELECT_ALL + FROM + TABLE_NAME + WHERE + COL_FILEID + EQUAL + PARAMETER;
	private static final String SQL_GET_VIDEO_METADATA_BY_FILEID_WITH_IMDBID_OR_TMDBID_EXIST = SELECT_ALL + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + "(" + TABLE_COL_IMDBID + IS_NOT_NULL + OR + TABLE_COL_TMDBID + IS_NOT_NULL + ")" + LIMIT_1;
	private static final String SQL_GET_API_METADATA_EXIST = SELECT + TABLE_COL_FILEID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_API_METADATA_IMDBID_OR_TMDBID_EXIST = SELECT + TABLE_COL_FILEID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + "(" + TABLE_COL_IMDBID + IS_NOT_NULL + OR + TABLE_COL_TMDBID + IS_NOT_NULL + ")" + LIMIT_1;
	private static final String SQL_GET_API_METADATA_API_VERSION_IMDBID_OR_TMDBID_EXIST = SELECT + TABLE_COL_FILEID + FROM + TABLE_NAME + WHERE + TABLE_COL_FILEID + EQUAL + PARAMETER + AND + "(" + TABLE_COL_IMDBID + IS_NOT_NULL + OR + TABLE_COL_TMDBID + IS_NOT_NULL + ")" + AND + TABLE_COL_API_VERSION + EQUAL + PARAMETER + LIMIT_1;
	private static final String SQL_GET_FILENAME_TVSERIESID = SELECT + MediaTableFiles.TABLE_COL_FILENAME + FROM + MediaTableFiles.TABLE_NAME + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA + WHERE + TABLE_COL_TVSERIESID + EQUAL + PARAMETER;
	private static final String SQL_UPDATE_TITLE = UPDATE + TABLE_NAME + SET + COL_TITLE + EQUAL + PARAMETER + WHERE + COL_TITLE + EQUAL + PARAMETER;

	/**
	 * Database column sizes
	 */
	private static final int SIZE_IMDBID = 16;
	private static final int SIZE_YEAR = 4;
	private static final int SIZE_TVSEASON = 4;
	private static final int SIZE_TVEPISODENUMBER = 8;

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

	private static void upgradeTable(Connection connection, Integer currentVersion) throws SQLException {
		LOGGER.info(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, currentVersion, TABLE_VERSION);
		for (int version = currentVersion; version < TABLE_VERSION; version++) {
			LOGGER.trace(LOG_UPGRADING_TABLE, DATABASE_NAME, TABLE_NAME, version, version + 1);
			switch (version) {
				case 1 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_OVERVIEW + " CLOB");
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_TAGLINE + VARCHAR);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_VOTES + VARCHAR);
				}
				case 2 -> {
					//add tmdb id
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_TMDBID + BIGINT);
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_TMDBTVID + BIGINT);
					executeUpdate(connection, "DROP INDEX " + IF_EXISTS + TABLE_NAME + "_IMDBID_API_VERSION_IDX");
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + "_" + COL_TMDBID  + "_" + COL_IMDBID  + "_" + COL_API_VERSION + "_IDX" + ON + TABLE_NAME + "(" + COL_TMDBID + ", " + COL_IMDBID + ", " + COL_API_VERSION + ")");
					//change PLOT to OVERVIEW
					if (!isColumnExist(connection, TABLE_NAME, COL_OVERVIEW)) {
						executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "PLOT" + RENAME_TO + COL_OVERVIEW);
					}
				}
				case 3 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_MODIFIED + BIGINT);
				}
				case 4 -> {
					executeUpdate(connection, ALTER_TABLE + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + COL_FILEID + BIGINT);
				}
				case 5 -> {
					executeUpdate(connection, DROP_INDEX + IF_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + "BASIC_COLUMNS" + IDX_MARKER);
					executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ADD + COLUMN + IF_NOT_EXISTS + COL_TVSERIESID + BIGINT);
					executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + ALTER_COLUMN + IF_EXISTS + "TVEPISODENAME" + RENAME_TO + COL_TITLE);
					if (isColumnExist(connection, TABLE_NAME, "MOVIEORSHOWNAME")) {
						//fill movie titles
						executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_TITLE + EQUAL + "MOVIEORSHOWNAME" + WHERE + TABLE_COL_ISTVEPISODE + EQUAL + FALSE);
						//fill tvSeriesId
						if (tableExists(connection, MediaTableTVSeries.TABLE_NAME)) {
							executeUpdate(connection, UPDATE + TABLE_NAME + SET + COL_TVSERIESID + EQUAL + "(" + SELECT + MediaTableTVSeries.TABLE_COL_ID + FROM  + MediaTableTVSeries.TABLE_NAME + WHERE + MediaTableTVSeries.TABLE_COL_TITLE + EQUAL + TABLE_NAME + ".MOVIEORSHOWNAME)");
						}
						executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "MOVIEORSHOWNAME");
					}
					executeUpdate(connection, ALTER_TABLE + IF_EXISTS + TABLE_NAME + DROP + COLUMN + IF_EXISTS + "MOVIEORSHOWNAMESIMPLE");
					//restore new index
					executeUpdate(connection, CREATE_INDEX + IF_NOT_EXISTS + TABLE_NAME + CONSTRAINT_SEPARATOR + "BASIC_COLUMNS" + IDX_MARKER + ON + TABLE_NAME + "(" + BASIC_COLUMNS + ")");
					//clear failed lookups
					if (tableExists(connection, MediaTableFailedLookups.TABLE_NAME)) {
						executeUpdate(connection, DELETE_FROM + MediaTableFailedLookups.TABLE_NAME);
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
			LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.info(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			CREATE_TABLE + TABLE_NAME + " (" +
				COL_FILEID                  + BIGINT                             + PRIMARY_KEY + COMMA +
				COL_MODIFIED                + BIGINT                                           + COMMA +
				COL_IMDBID                  + VARCHAR + "(" + SIZE_IMDBID + ")"                + COMMA +
				COL_TMDBID                  + BIGINT                                           + COMMA +
				COL_TMDBTVID                + BIGINT                                           + COMMA +
				COL_MEDIA_YEAR              + VARCHAR + "(" + SIZE_YEAR + ")"                  + COMMA +
				COL_TITLE                   + VARCHAR                                          + COMMA +
				COL_TVSERIESID              + BIGINT                                           + COMMA +
				COL_TVSEASON                + VARCHAR + "(" + SIZE_TVSEASON + ")"              + COMMA +
				COL_TVEPISODENUMBER         + VARCHAR + "(" + SIZE_TVEPISODENUMBER + ")"       + COMMA +
				COL_ISTVEPISODE             + BOOLEAN                                          + COMMA +
				COL_EXTRAINFORMATION        + VARCHAR_SIZE_MAX                                 + COMMA +
				COL_API_VERSION             + VARCHAR_16                                       + COMMA +
				COL_BUDGET                  + BIGINT                                           + COMMA +
				COL_CREDITS                 + CLOB                                             + COMMA +
				COL_EXTERNALIDS             + CLOB                                             + COMMA +
				COL_HOMEPAGE                + VARCHAR                                          + COMMA +
				COL_IMAGES                  + CLOB                                             + COMMA +
				COL_ORIGINALLANGUAGE        + VARCHAR                                          + COMMA +
				COL_ORIGINALTITLE           + VARCHAR                                          + COMMA +
				COL_OVERVIEW                + CLOB                                             + COMMA +
				COL_PRODUCTIONCOMPANIES     + CLOB                                             + COMMA +
				COL_PRODUCTIONCOUNTRIES     + CLOB                                             + COMMA +
				COL_REVENUE                 + BIGINT                                           + COMMA +
				COL_TAGLINE                 + VARCHAR                                          + COMMA +
				COL_VOTES                   + VARCHAR                                          + COMMA +
				CONSTRAINT + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_FILEID + FK_MARKER + FOREIGN_KEY + "(" + COL_FILEID + ")" + REFERENCES + MediaTableFiles.REFERENCE_TABLE_COL_ID + ON_DELETE_CASCADE +
			")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + "BASIC_COLUMNS" + IDX_MARKER + ON + TABLE_NAME + "(" + BASIC_COLUMNS + ")",
			CREATE_INDEX + TABLE_NAME + CONSTRAINT_SEPARATOR + COL_TMDBID  + CONSTRAINT_SEPARATOR + COL_IMDBID  + CONSTRAINT_SEPARATOR + COL_API_VERSION + IDX_MARKER + ON + TABLE_NAME + "(" + COL_TMDBID + COMMA + COL_IMDBID + COMMA + COL_API_VERSION + ")"
		);
	}

	/**
	 * Updates an existing row with information either extracted from the filename
	 * or from our API.
	 *
	 * @param connection the db connection
	 * @param fileId the file id from FILES table.
	 * @param media the {@link MediaInfo} VideoMetadata row to update.
	 * @param apiExtendedMetadata JsonObject from metadata
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertOrUpdateVideoMetadata(final Connection connection, final Long fileId, final MediaInfo media, final boolean fromApi) throws SQLException {
		if (connection == null || fileId == null || media == null || !media.hasVideoMetadata()) {
			return;
		}
		MediaVideoMetadata videoMetadata = media.getVideoMetadata();
		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				fromApi ? SQL_GET_VIDEO_ALL_METADATA_BY_FILEID : SQL_GET_VIDEO_METADATA_BY_FILEID,
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
		) {
			updateStatement.setLong(1, fileId);
			try (ResultSet rs = updateStatement.executeQuery()) {
				boolean isCreatingNewRecord = !rs.next();
				if (isCreatingNewRecord) {
					rs.moveToInsertRow();
					rs.updateLong(COL_FILEID, fileId);
				}
				videoMetadata.setFileId(fileId);
				rs.updateString(COL_IMDBID, StringUtils.left(videoMetadata.getIMDbID(), SIZE_IMDBID));
				rs.updateString(COL_MEDIA_YEAR, StringUtils.left(videoMetadata.getYear(), SIZE_YEAR));
				rs.updateString(COL_TITLE, StringUtils.left(videoMetadata.getTitle(), SIZE_MAX));
				rs.updateString(COL_EXTRAINFORMATION, StringUtils.left(videoMetadata.getExtraInformation(), SIZE_MAX));
				rs.updateBoolean(COL_ISTVEPISODE, videoMetadata.isTvEpisode());
				updateLong(rs, COL_TVSERIESID, videoMetadata.getTvSeriesId());
				rs.updateString(COL_TVSEASON, StringUtils.left(videoMetadata.getTvSeason(), SIZE_TVSEASON));
				rs.updateString(COL_TVEPISODENUMBER, StringUtils.left(videoMetadata.getTvEpisodeNumber(), SIZE_TVEPISODENUMBER));
				if (fromApi) {
					rs.updateString(COL_API_VERSION, StringUtils.left(APIUtils.getApiDataVideoVersion(), SIZE_IMDBID));
					rs.updateLong(COL_MODIFIED, System.currentTimeMillis());
					updateLong(rs, COL_BUDGET, videoMetadata.getBudget());
					if (videoMetadata.getCredits() != null) {
						rs.updateString(COL_CREDITS, GSON.toJson(videoMetadata.getCredits()));
					} else {
						rs.updateNull(COL_CREDITS);
					}
					if (videoMetadata.getExternalIDs() != null) {
						rs.updateString(COL_EXTERNALIDS, GSON.toJson(videoMetadata.getExternalIDs()));
					} else {
						rs.updateNull(COL_EXTERNALIDS);
					}
					rs.updateString(COL_HOMEPAGE, videoMetadata.getHomepage());
					if (videoMetadata.getImages() != null) {
						rs.updateString(COL_IMAGES, GSON.toJson(videoMetadata.getImages()));
					} else {
						rs.updateNull(COL_IMAGES);
					}
					if (videoMetadata.getOriginalLanguage() != null) {
						rs.updateString(COL_ORIGINALLANGUAGE, GSON.toJson(videoMetadata.getOriginalLanguage()));
					} else {
						rs.updateNull(COL_ORIGINALLANGUAGE);
					}
					rs.updateString(COL_ORIGINALTITLE, GSON.toJson(videoMetadata.getOriginalTitle()));
					rs.updateString(COL_OVERVIEW, videoMetadata.getOverview());
					if (videoMetadata.getProductionCompanies() != null) {
						rs.updateString(COL_PRODUCTIONCOMPANIES, GSON.toJson(videoMetadata.getProductionCompanies()));
					} else {
						rs.updateNull(COL_PRODUCTIONCOMPANIES);
					}
					if (videoMetadata.getProductionCountries() != null) {
						rs.updateString(COL_PRODUCTIONCOUNTRIES, GSON.toJson(videoMetadata.getProductionCountries()));
					} else {
						rs.updateNull(COL_PRODUCTIONCOUNTRIES);
					}
					updateLong(rs, COL_REVENUE, videoMetadata.getRevenue());
					rs.updateString(COL_TAGLINE, videoMetadata.getTagline());
					updateLong(rs, COL_TMDBID, videoMetadata.getTmdbId());
					updateLong(rs, COL_TMDBTVID, videoMetadata.getTmdbTvId());
					rs.updateString(COL_VOTES, videoMetadata.getVotes());
				}
				if (isCreatingNewRecord) {
					rs.insertRow();
				} else {
					rs.updateRow();
				}
			}
		}
		if (fromApi) {
			MediaTableVideoMetadataActors.set(connection, fileId, videoMetadata.getActors(), null);
			MediaTableVideoMetadataAwards.set(connection, fileId, videoMetadata.getAwards(), null);
			MediaTableVideoMetadataCountries.set(connection, fileId, videoMetadata.getCountries(), null);
			MediaTableVideoMetadataDirectors.set(connection, fileId, videoMetadata.getDirectors(), null);
			MediaTableVideoMetadataGenres.set(connection, fileId, videoMetadata.getGenres(), null);
			MediaTableVideoMetadataPosters.set(connection, fileId, videoMetadata.getPoster(), null);
			MediaTableVideoMetadataProduction.set(connection, fileId, videoMetadata.getProduction(), null);
			MediaTableVideoMetadataRated.set(connection, fileId, videoMetadata.getRated(), null);
			MediaTableVideoMetadataIMDbRating.set(connection, fileId, videoMetadata.getRating(), null);
			MediaTableVideoMetadataRatings.set(connection, fileId, videoMetadata.getRatings(), null);
			MediaTableVideoMetadataReleased.set(connection, fileId, videoMetadata.getReleased(), null);
		}
		connection.commit();
	}

	/**
	 * Updates an existing row with information either extracted from the filename
	 * or from our API.
	 *
	 * @param connection the db connection
	 * @param path the full path of the media.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @param media the {@link MediaInfo} row to update.
	 * @param apiExtendedMetadata JsonObject from metadata
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertVideoMetadata(final Connection connection, String path, long modified, MediaInfo media) throws SQLException {
		if (StringUtils.isBlank(path)) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because the media cannot be identified", path);
			return;
		}
		if (media == null || !media.hasVideoMetadata()) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because there is no media information", path);
			return;
		}

		Long fileId = MediaTableFiles.getFileId(connection, path, modified);

		if (fileId == null) {
			LOGGER.trace("Couldn't find \"{}\" in the database when trying to store metadata", path);
		} else {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_API_METADATA_EXIST)) {
				selectStatement.setLong(1, fileId);
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						LOGGER.trace("\"{}\" already exists in the database", path);
						return;
					}
				}
			}
			media.getVideoMetadata().setFileId(fileId);
			insertOrUpdateVideoMetadata(connection, fileId, media, false);
		}
	}

	public static MediaVideoMetadata getVideoMetadataByFilename(final Connection connection, final String filename) {
		if (connection == null || StringUtils.isBlank(filename)) {
			return null;
		}
		Long id = MediaTableFiles.getFileId(connection, filename);
		if (id != null) {
			return getVideoMetadataByFileId(connection, id);
		}
		return null;
	}

	public static MediaVideoMetadata getVideoMetadataByFileId(final Connection connection, final long fileId) {
		if (connection == null || fileId < 0) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_VIDEO_METADATA_BY_FILEID_WITH_IMDBID_OR_TMDBID_EXIST)) {
				selectStatement.setLong(1, fileId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						MediaVideoMetadata metadata = new MediaVideoMetadata();
						metadata.setFileId(rs.getLong(COL_FILEID));
						metadata.setApiVersion(rs.getString(COL_API_VERSION));
						metadata.setIMDbID(rs.getString(COL_IMDBID));
						metadata.setYear(rs.getString(COL_MEDIA_YEAR));
						metadata.setTitle(rs.getString(COL_TITLE));
						metadata.setExtraInformation(rs.getString(COL_EXTRAINFORMATION));
						metadata.setIsTvEpisode(rs.getBoolean(COL_ISTVEPISODE));
						metadata.setTvSeriesId(toLong(rs, COL_TVSERIESID));
						metadata.setActors(MediaTableVideoMetadataActors.getActorsForFile(connection, fileId));
						metadata.setAwards(MediaTableVideoMetadataAwards.getValueForFile(connection, fileId));
						metadata.setBudget(rs.getLong(COL_BUDGET));
						metadata.setCredits(rs.getString(COL_CREDITS));
						metadata.setCountries(MediaTableVideoMetadataCountries.getCountriesForFile(connection, fileId));
						metadata.setDirectors(MediaTableVideoMetadataDirectors.getDirectorsForFile(connection, fileId));
						metadata.setExternalIDs(rs.getString(COL_EXTERNALIDS));
						metadata.setGenres(MediaTableVideoMetadataGenres.getGenresForFile(connection, fileId));
						metadata.setHomepage(rs.getString(COL_HOMEPAGE));
						metadata.setImages(rs.getString(COL_IMAGES));
						metadata.setOriginalLanguage(rs.getString(COL_ORIGINALLANGUAGE));
						metadata.setOriginalTitle(rs.getString(COL_ORIGINALTITLE));
						metadata.setOverview(rs.getString(COL_OVERVIEW));
						metadata.setPoster(MediaTableVideoMetadataPosters.getValueForFile(connection, fileId));
						metadata.setProduction(MediaTableVideoMetadataProduction.getValueForFile(connection, fileId));
						metadata.setProductionCompanies(rs.getString(COL_PRODUCTIONCOMPANIES));
						metadata.setProductionCountries(rs.getString(COL_PRODUCTIONCOUNTRIES));
						metadata.setRated(MediaTableVideoMetadataRated.getValueForFile(connection, fileId));
						metadata.setRating(MediaTableVideoMetadataIMDbRating.getValueForFile(connection, fileId));
						metadata.setRatings(MediaTableVideoMetadataRatings.getRatingsForFile(connection, fileId));
						metadata.setReleased(MediaTableVideoMetadataReleased.getValueForFile(connection, fileId));
						metadata.setRevenue(rs.getLong(COL_REVENUE));
						if (metadata.isTvEpisode() && metadata.getTvSeriesId() != null) {
							metadata.setSeriesMetadata(MediaTableTVSeries.getTvSeriesMetadata(connection, metadata.getTvSeriesId()));
						}
						metadata.setTvSeason(rs.getString(COL_TVSEASON));
						metadata.setTvEpisodeNumber(rs.getString(COL_TVEPISODENUMBER));
						metadata.setTagline(rs.getString(COL_TAGLINE));
						metadata.setTmdbId(rs.getLong(COL_TMDBID));
						metadata.setTmdbTvId(rs.getLong(COL_TMDBTVID));
						metadata.setVotes(rs.getString(COL_VOTES));
						metadata.setTranslations(MediaTableVideoMetadataLocalized.getAllVideoMetadataLocalized(connection, fileId, false));
						//ensure we have the default translation
						metadata.ensureHavingTranslation(null);
						return metadata;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	public static VideoMetadataLocalized getVideoMetadataLocalized(final Connection connection, final long fileId) {
		if (connection == null || fileId < 0) {
			return null;
		}
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_VIDEO_METADATA_BY_FILEID_WITH_IMDBID_OR_TMDBID_EXIST)) {
				selectStatement.setLong(1, fileId);
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						VideoMetadataLocalized metadata = new VideoMetadataLocalized();
						metadata.setHomepage(rs.getString(COL_HOMEPAGE));
						metadata.setOverview(rs.getString(COL_OVERVIEW));
						metadata.setTagline(rs.getString(COL_TAGLINE));
						metadata.setTitle(rs.getString(COL_TITLE));
						return metadata;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}


	/**
	 * Checks whether the latest data from our API has been written to the
	 * database for this video.
	 * If we could not fetch the latest version from the API, it will check
	 * whether any version exists in the database.
	 *
	 * @param connection the db connection
	 * @param name the full path of the video.
	 * @param modified the current {@code lastModified} value of the media file.
	 * @return whether the latest API metadata exists for this video.
	 */
	public static boolean doesLatestApiMetadataExist(final Connection connection, String name, long modified) {
		Long id = MediaTableFiles.getFileId(connection, name, modified);
		if (id == null) {
			return true;
		}
		return doesLatestApiMetadataExist(connection, id);
	}

	public static boolean doesLatestApiMetadataExist(final Connection connection, final Long fileId) {
		String sql;
		String latestVersion = null;
		if (CONFIGURATION.getExternalNetwork()) {
			latestVersion = APIUtils.getApiDataVideoVersion();
		}

		if (latestVersion != null) {
			sql = SQL_GET_API_METADATA_API_VERSION_IMDBID_OR_TMDBID_EXIST;
		} else {
			sql = SQL_GET_API_METADATA_IMDBID_OR_TMDBID_EXIST;
		}

		try {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setLong(1, fileId.intValue());
				if (latestVersion != null) {
					statement.setString(2, latestVersion);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return true;
					}
				}
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "checking if API metadata exists for", TABLE_NAME, fileId, se.getMessage());
			LOGGER.trace("", se);
		}

		return false;
	}

	public static List<String> getTvEpisodesFilesByTvSeriesId(final Connection connection, final Long tvSeriesId) {
		List<String> result = new ArrayList<>();
		if (connection == null || tvSeriesId == null || tvSeriesId < 0) {
			return result;
		}
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_FILENAME_TVSERIESID)) {
			statement.setLong(1, tvSeriesId);
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					result.add(resultSet.getString(1));
				}
			}
		} catch (SQLException se) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "getTvEpisodesFilesByTvSeriesId", TABLE_NAME, tvSeriesId, se.getMessage());
			LOGGER.trace("", se);
		}
		return result;
	}

	/**
	 * Updates the name of a movie or TV series for existing entries in the database.
	 *
	 * @param connection the db connection
	 * @param oldName the existing movie or show name.
	 * @param newName the new movie or show name.
	 */
	public static void updateMovieOrShowName(final Connection connection, String oldName, String newName) {
		if (StringUtils.isEmpty(newName)) {
			return;
		}
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_TITLE)) {
				statement.setString(1,  StringUtils.left(newName, SIZE_MAX));
				statement.setString(2,  StringUtils.left(oldName, SIZE_MAX));
				statement.execute();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Failed to update MOVIEORSHOWNAME from \"{}\" to \"{}\": {}",
				oldName,
				newName,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

}
