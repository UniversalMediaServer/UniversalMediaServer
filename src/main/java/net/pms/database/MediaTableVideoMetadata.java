/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.database;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaVideoMetadata;
import net.pms.util.APIUtils;
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
	 * COLUMNS NAMES
	 */
	private static final String COL_API_VERSION = "API_VERSION";
	private static final String COL_EXTRAINFORMATION = "EXTRAINFORMATION";
	public static final String COL_FILEID = "FILEID";
	private static final String COL_IMDBID = "IMDBID";
	private static final String COL_ISTVEPISODE = "ISTVEPISODE";
	private static final String COL_MEDIA_YEAR = "MEDIA_YEAR";
	private static final String COL_MOVIEORSHOWNAME = "MOVIEORSHOWNAME";
	private static final String COL_MOVIEORSHOWNAMESIMPLE = "MOVIEORSHOWNAMESIMPLE";
	private static final String COL_TVSEASON = "TVSEASON";
	private static final String COL_TVEPISODENAME = "TVEPISODENAME";
	private static final String COL_TVEPISODENUMBER = "TVEPISODENUMBER";
	private static final String BASIC_COLUMNS = COL_IMDBID + ", " + COL_MEDIA_YEAR + ", " + COL_MOVIEORSHOWNAME + ", " + COL_MOVIEORSHOWNAMESIMPLE + ", " + COL_EXTRAINFORMATION + ", " + COL_ISTVEPISODE + ", " + COL_TVSEASON + ", " + COL_TVEPISODENUMBER + ", " + COL_TVEPISODENAME;
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
	private static final String COL_PRODUCTIONCOMPANIES = "PRODUCTIONCOMPANIES";
	private static final String COL_PRODUCTIONCOUNTRIES = "PRODUCTIONCOUNTRIES";
	private static final String COL_REVENUE = "REVENUE";

	/**
	 * COLUMNS with table name
	 */
	private static final String TABLE_COL_API_VERSION = TABLE_NAME + "." + COL_API_VERSION;
	public static final String TABLE_COL_EXTRAINFORMATION = TABLE_NAME + "." + COL_EXTRAINFORMATION;
	public static final String TABLE_COL_FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String TABLE_COL_IMDBID = TABLE_NAME + "." + COL_IMDBID;
	public static final String TABLE_COL_ISTVEPISODE = TABLE_NAME + "." + COL_ISTVEPISODE;
	public static final String TABLE_COL_MEDIA_YEAR = TABLE_NAME + "." + COL_MEDIA_YEAR;
	public static final String TABLE_COL_MOVIEORSHOWNAME = TABLE_NAME + "." + COL_MOVIEORSHOWNAME;
	public static final String TABLE_COL_MOVIEORSHOWNAMESIMPLE = TABLE_NAME + "." + COL_MOVIEORSHOWNAMESIMPLE;
	public static final String TABLE_COL_TVEPISODENAME = TABLE_NAME + ".TVEPISODENAME";
	public static final String TABLE_COL_TVEPISODENUMBER = TABLE_NAME + "." + COL_TVEPISODENUMBER;
	public static final String TABLE_COL_TVSEASON = TABLE_NAME + "." + COL_TVSEASON;
	private static final String SQL_GET_VIDEO_METADATA_BY_FILEID = "SELECT " + COL_FILEID + ", " + BASIC_COLUMNS + " FROM " + TABLE_NAME + " WHERE " + COL_FILEID + " = ?";
	private static final String SQL_GET_VIDEO_ALL_METADATA_BY_FILEID = "SELECT * FROM " + TABLE_NAME + " WHERE " + COL_FILEID + " = ?";
	private static final String SQL_GET_VIDEO_METADATA_BY_FILEID_IMDBID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ? and " + TABLE_COL_IMDBID + " IS NOT NULL LIMIT 1";
	private static final String SQL_GET_API_METADATA_EXIST = "SELECT " + TABLE_COL_FILEID + " FROM " + TABLE_NAME + " " + " WHERE " + TABLE_COL_FILEID + " = ? LIMIT 1";
	private static final String SQL_GET_API_METADATA_IMDBID_EXIST = "SELECT " + TABLE_COL_FILEID + " FROM " + TABLE_NAME + " " + " WHERE " + TABLE_COL_FILEID + " = ? AND " + TABLE_COL_IMDBID + " IS NOT NULL LIMIT 1";
	private static final String SQL_GET_API_METADATA_API_IMDBID_VERSION_EXIST = "SELECT " + TABLE_COL_FILEID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_FILEID + " = ? AND " + TABLE_COL_IMDBID + " IS NOT NULL AND " + TABLE_COL_API_VERSION + " = ? LIMIT 1";

	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES = "LEFT JOIN " + MediaTableVideoMetadataGenres.TABLE_NAME + " ON " + TABLE_COL_FILEID + " = " + MediaTableVideoMetadataGenres.TABLE_COL_FILEID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_IMDB_RATING = "LEFT JOIN " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + " ON " + TABLE_COL_FILEID + " = " + MediaTableVideoMetadataIMDbRating.TABLE_COL_FILEID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_RATED = "LEFT JOIN " + MediaTableVideoMetadataRated.TABLE_NAME + " ON " + TABLE_COL_FILEID + " = " + MediaTableVideoMetadataRated.TABLE_COL_FILEID + " ";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;
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
				default -> throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
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
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + " (" +
				COL_FILEID + "                INTEGER                              PRIMARY KEY , " +
				COL_IMDBID + "                VARCHAR(" + SIZE_IMDBID + ")                     , " +
				COL_MEDIA_YEAR + "            VARCHAR(" + SIZE_YEAR + ")                       , " +
				COL_MOVIEORSHOWNAME + "       VARCHAR(" + SIZE_MAX + ")                        , " +
				COL_MOVIEORSHOWNAMESIMPLE + " VARCHAR(" + SIZE_MAX + ")                        , " +
				COL_TVSEASON + "              VARCHAR(" + SIZE_TVSEASON + ")                   , " +
				COL_TVEPISODENUMBER + "       VARCHAR(" + SIZE_TVEPISODENUMBER + ")            , " +
				COL_TVEPISODENAME + "         VARCHAR(" + SIZE_MAX + ")                        , " +
				COL_ISTVEPISODE + "           BOOLEAN                                          , " +
				COL_EXTRAINFORMATION + "      VARCHAR(" + SIZE_MAX + ")                        , " +
				COL_API_VERSION + "           VARCHAR(" + SIZE_IMDBID + ")                     , " +
				COL_BUDGET + "                BIGINT                                           , " +
				COL_CREDITS + "               CLOB                                             , " +
				COL_EXTERNALIDS + "           CLOB                                             , " +
				COL_HOMEPAGE + "              VARCHAR                                          , " +
				COL_IMAGES + "                CLOB                                             , " +
				COL_ORIGINALLANGUAGE + "      VARCHAR                                          , " +
				COL_ORIGINALTITLE + "         VARCHAR                                          , " +
				COL_PRODUCTIONCOMPANIES + "   CLOB                                             , " +
				COL_PRODUCTIONCOUNTRIES + "   CLOB                                             , " +
				COL_REVENUE + "               BIGINT                                           , " +
				"CONSTRAINT " + TABLE_NAME + "_" + COL_FILEID + "_FK FOREIGN KEY(" + COL_FILEID + ") REFERENCES " + MediaTableFiles.TABLE_NAME + "(" + MediaTableFiles.COL_ID + ") ON DELETE CASCADE" +
			")"
		);
		execute(connection, "CREATE INDEX " + TABLE_NAME + "_BASIC_COLUMNS_IDX ON " + TABLE_NAME + "(" + BASIC_COLUMNS + ")");
		execute(connection, "CREATE INDEX " + TABLE_NAME + "_IMDBID_API_VERSION_IDX ON " + TABLE_NAME + "(" + COL_IMDBID + ", " + COL_API_VERSION + ")");
	}

	/**
	 * Updates an existing row with information either extracted from the filename
	 * or from our API.
	 *
	 * @param connection the db connection
	 * @param fileId the file id from FILES table.
	 * @param media the {@link DLNAMediaInfo} VideoMetadata row to update.
	 * @param apiExtendedMetadata JsonObject from metadata
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertOrUpdateVideoMetadata(final Connection connection, final Long fileId, final DLNAMediaInfo media, final JsonObject apiExtendedMetadata) throws SQLException {
		if (connection == null || fileId == null || media == null || !media.hasVideoMetadata()) {
			return;
		}
		DLNAMediaVideoMetadata videoMetadata = media.getVideoMetadata();
		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				apiExtendedMetadata != null ? SQL_GET_VIDEO_ALL_METADATA_BY_FILEID : SQL_GET_VIDEO_METADATA_BY_FILEID,
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
				rs.updateString(COL_IMDBID, StringUtils.left(videoMetadata.getIMDbID(), SIZE_IMDBID));
				rs.updateString(COL_MEDIA_YEAR, StringUtils.left(videoMetadata.getYear(), SIZE_YEAR));
				rs.updateString(COL_MOVIEORSHOWNAME, StringUtils.left(videoMetadata.getMovieOrShowName(), SIZE_MAX));
				rs.updateString(COL_MOVIEORSHOWNAMESIMPLE, StringUtils.left(videoMetadata.getSimplifiedMovieOrShowName(), SIZE_MAX));
				rs.updateString(COL_EXTRAINFORMATION, StringUtils.left(videoMetadata.getExtraInformation(), SIZE_MAX));
				rs.updateBoolean(COL_ISTVEPISODE, videoMetadata.isTVEpisode());
				rs.updateString(COL_TVSEASON, StringUtils.left(videoMetadata.getTVSeason(), SIZE_TVSEASON));
				rs.updateString(COL_TVEPISODENUMBER, StringUtils.left(videoMetadata.getTVEpisodeNumber(), SIZE_TVEPISODENUMBER));
				rs.updateString(COL_TVEPISODENAME, StringUtils.left(videoMetadata.getTVEpisodeName(), SIZE_MAX));
				if (apiExtendedMetadata != null) {
					rs.updateString(COL_API_VERSION, StringUtils.left(APIUtils.getApiDataVideoVersion(), SIZE_IMDBID));
					if (apiExtendedMetadata.has("budget")) {
						rs.updateLong(COL_BUDGET, apiExtendedMetadata.get("budget").getAsLong());
					} else {
						rs.updateNull(COL_BUDGET);
					}
					if (apiExtendedMetadata.has("credits")) {
						rs.updateString(COL_CREDITS, apiExtendedMetadata.get("credits").toString());
					} else {
						rs.updateNull(COL_CREDITS);
					}
					if (apiExtendedMetadata.has("externalIDs")) {
						rs.updateString(COL_EXTERNALIDS, apiExtendedMetadata.get("externalIDs").toString());
					} else {
						rs.updateNull(COL_EXTERNALIDS);
					}
					rs.updateString(COL_HOMEPAGE, APIUtils.getStringOrNull(apiExtendedMetadata, "homepage"));
					if (apiExtendedMetadata.has("images")) {
						rs.updateString(COL_IMAGES, apiExtendedMetadata.get("images").toString());
					} else {
						rs.updateNull(COL_IMAGES);
					}
					rs.updateString(COL_ORIGINALLANGUAGE, APIUtils.getStringOrNull(apiExtendedMetadata, "originalLanguage"));
					rs.updateString(COL_ORIGINALTITLE, APIUtils.getStringOrNull(apiExtendedMetadata, "originalTitle"));
					if (apiExtendedMetadata.has("productionCompanies")) {
						rs.updateString(COL_PRODUCTIONCOMPANIES, apiExtendedMetadata.get("productionCompanies").toString());
					} else {
						rs.updateNull(COL_PRODUCTIONCOMPANIES);
					}
					if (apiExtendedMetadata.has("productionCountries")) {
						rs.updateString(COL_PRODUCTIONCOUNTRIES, apiExtendedMetadata.get("productionCountries").toString());
					} else {
						rs.updateNull(COL_PRODUCTIONCOUNTRIES);
					}
					if (apiExtendedMetadata.has("revenue")) {
						rs.updateLong(COL_REVENUE, apiExtendedMetadata.get("revenue").getAsLong());
					} else {
						rs.updateNull(COL_REVENUE);
					}
				}
				if (isCreatingNewRecord) {
					rs.insertRow();
				} else {
					rs.updateRow();
				}
			}
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
	 * @param media the {@link DLNAMediaInfo} row to update.
	 * @param apiExtendedMetadata JsonObject from metadata
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	public static void insertVideoMetadata(final Connection connection, String path, long modified, DLNAMediaInfo media, final JsonObject apiExtendedMetadata) throws SQLException {
		if (StringUtils.isBlank(path)) {
			LOGGER.warn("Couldn't write metadata for \"{}\" to the database because the media cannot be identified", path);
			return;
		}
		if (media == null) {
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
			insertOrUpdateVideoMetadata(connection, fileId, media, apiExtendedMetadata);
		}
	}

	public static DLNAMediaVideoMetadata getVideoMetadataByFileId(Connection connection, long fileId) {
		if (connection == null || fileId < 0) {
			return null;
		}
		try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_VIDEO_METADATA_BY_FILEID)) {
			selectStatement.setLong(1, fileId);
			try (ResultSet rs = selectStatement.executeQuery()) {
				if (rs.next()) {
					DLNAMediaVideoMetadata videoMetadata = new DLNAMediaVideoMetadata();
					videoMetadata.setIMDbID(rs.getString(COL_IMDBID));
					videoMetadata.setYear(rs.getString(COL_MEDIA_YEAR));
					videoMetadata.setMovieOrShowName(rs.getString(COL_MOVIEORSHOWNAME));
					videoMetadata.setSimplifiedMovieOrShowName(rs.getString(COL_MOVIEORSHOWNAMESIMPLE));
					videoMetadata.setExtraInformation(rs.getString(COL_EXTRAINFORMATION));

					if (rs.getBoolean(COL_ISTVEPISODE)) {
						videoMetadata.setTVSeason(rs.getString(COL_TVSEASON));
						videoMetadata.setTVEpisodeNumber(rs.getString(COL_TVEPISODENUMBER));
						videoMetadata.setTVEpisodeName(rs.getString(COL_TVEPISODENAME));
						videoMetadata.setIsTVEpisode(true);
						// Fields from TV Series table
						videoMetadata.setTVSeriesStartYear(MediaTableTVSeries.getStartYearBySimplifiedTitle(connection, videoMetadata.getSimplifiedMovieOrShowName()));
					} else {
						videoMetadata.setIsTVEpisode(false);
					}
				}
			}
		} catch (SQLException ex) {
			LOGGER.error(
				LOG_ERROR_WHILE_IN_FOR,
				DATABASE_NAME,
				"reading",
				TABLE_NAME,
				fileId,
				ex.getMessage()
			);
			LOGGER.trace("", ex);
		}
		return null;
	}

	/**
	 * @param connection the db connection
	 * @param path the full path of the media.
	 * @return all data across all tables for a video file, if it has an IMDb ID stored.
	 */
	public static JsonObject getVideoMetadataAsJsonObject(final Connection connection, final String path) {
		Long fileId = MediaTableFiles.getFileId(connection, path);
		return getVideoMetadataAsJsonObject(connection, fileId);
	}

	public static JsonObject getVideoMetadataAsJsonObject(final Connection connection, final Long fileId) {
		if (connection == null || fileId == null) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_VIDEO_METADATA_BY_FILEID_IMDBID)) {
				selectStatement.setLong(1, fileId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						JsonObject result = new JsonObject();
						result.addProperty("imdbID", rs.getString(COL_IMDBID));
						addJsonElementToJsonObjectIfExists(result, "credits", rs.getString(COL_CREDITS));
						addJsonElementToJsonObjectIfExists(result, "externalIDs", rs.getString(COL_EXTERNALIDS));
						result.addProperty("homepage", rs.getString(COL_HOMEPAGE));
						addJsonElementToJsonObjectIfExists(result, "images", rs.getString(COL_IMAGES));
						result.add("actors", MediaTableVideoMetadataActors.getJsonArrayForFile(connection, fileId));
						result.addProperty("award", MediaTableVideoMetadataAwards.getValueForFile(connection, fileId));
						result.add("countries", MediaTableVideoMetadataCountries.getJsonArrayForFile(connection, fileId));
						result.add("directors", MediaTableVideoMetadataDirectors.getJsonArrayForFile(connection, fileId));
						result.add("genres", MediaTableVideoMetadataGenres.getJsonArrayForFile(connection, fileId));
						result.addProperty("imdbRating", MediaTableVideoMetadataIMDbRating.getValueForFile(connection, fileId));
						result.addProperty("poster", MediaTableVideoMetadataPosters.getValueForFile(connection, fileId));
						result.addProperty("production", MediaTableVideoMetadataProduction.getValueForFile(connection, fileId));
						result.addProperty("rated", MediaTableVideoMetadataRated.getValueForFile(connection, fileId));
						result.add("ratings", MediaTableVideoMetadataRatings.getJsonArrayForFile(connection, fileId));
						result.addProperty("released", MediaTableVideoMetadataReleased.getValueForFile(connection, fileId));
						if (rs.getBoolean(COL_ISTVEPISODE)) {
							String showName = rs.getString(COL_MOVIEORSHOWNAME);
							if (StringUtils.isNotBlank(showName)) {
								addJsonElementToJsonObjectIfExists(result, "seriesImages", MediaTableTVSeries.getImagesByTitle(connection, showName));
							}
						}
						return result;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", fileId, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	private static void addJsonElementToJsonObjectIfExists(final JsonObject dest, final String property, final String jsonString) {
		if (StringUtils.isEmpty(jsonString)) {
			return;
		}
		try {
			JsonElement element = GSON.fromJson(jsonString, JsonElement.class);
			dest.add(property, element);
		} catch (JsonSyntaxException e) {
		}
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
			sql = SQL_GET_API_METADATA_API_IMDBID_VERSION_EXIST;
		} else {
			sql = SQL_GET_API_METADATA_IMDBID_EXIST;
		}

		try {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setInt(1, fileId.intValue());
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


	/**
	 * Updates the name of a movie or TV series for existing entries in the database.
	 *
	 * @param connection the db connection
	 * @param oldName the existing movie or show name.
	 * @param newName the new movie or show name.
	 */
	public static void updateMovieOrShowName(final Connection connection, String oldName, String newName) {
		try {
			updateRowsInTable(connection, oldName, newName, COL_MOVIEORSHOWNAME, SIZE_MAX, true);
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

	/**
	 * Updates a row or rows in the table.
	 *
	 * @param connection the db connection
	 * @param oldValue the value to match, can be {@code null}.
	 * @param newValue the value to store, can be {@code null}.
	 * @param columnName the column to update.
	 * @param size the maximum size of the data if {@code isString} is
	 *            {@code true}.
	 * @param isString whether or not the value is a SQL char/string and should
	 *            be quoted and length limited.
	 * @throws SQLException if an SQL error occurs during the operation.
	 */
	private static void updateRowsInTable(final Connection connection, String oldValue, String newValue, String columnName, int size, boolean isString) throws SQLException {
		if (isString && size < 1) {
			throw new IllegalArgumentException("size must be positive");
		}
		if (StringUtils.isEmpty(columnName)) {
			LOGGER.error("Couldn't update rows in " + TABLE_NAME + " table because columnName is blank");
			return;
		}

		// Sanitize values
		oldValue = isString ? sqlQuote(oldValue) : sqlEscape(oldValue);
		newValue = isString ? sqlQuote(newValue) : sqlEscape(newValue);

		LOGGER.trace(
			"Updating rows in " + TABLE_NAME + " table from \"{}\" to \"{}\" in column {}",
			oldValue,
			newValue,
			columnName
		);
		try (Statement statement = connection.createStatement()) {
			int rows = statement.executeUpdate(
				"UPDATE " + TABLE_NAME + " SET " +
					columnName +  " = " + (newValue == null ? "NULL" : (isString ? StringUtils.left(newValue, size) : newValue)) +
				" WHERE " +
					columnName + (oldValue == null ? " IS NULL" : " = " + (isString ? StringUtils.left(oldValue, size) : oldValue))
			);
			LOGGER.trace("Updated {} rows in " + TABLE_NAME + " table", rows);
		}
	}

}
