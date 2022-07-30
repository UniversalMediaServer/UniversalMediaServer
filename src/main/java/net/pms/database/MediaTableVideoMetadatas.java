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
import java.sql.Types;
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
public class MediaTableVideoMetadatas extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadatas.class);
	private static final Gson GSON = new Gson();
	public static final String TABLE_NAME = "VIDEO_METADATAS";
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
	private static final String TMDB_COLUMNS = "BUDGET, CREDITS, EXTERNALIDS, HOMEPAGE, IMAGES, ORIGINALLANGUAGE, ORIGINALTITLE, PRODUCTIONCOMPANIES, PRODUCTIONCOUNTRIES, REVENUE";

	/**
	 * COLUMNS with table name
	 */
	public static final String API_VERSION = TABLE_NAME + "." + COL_API_VERSION;
	public static final String EXTRAINFORMATION = TABLE_NAME + "." + COL_EXTRAINFORMATION;
	public static final String FILEID = TABLE_NAME + "." + COL_FILEID;
	public static final String IMDBID = TABLE_NAME + "." + COL_IMDBID;
	public static final String ISTVEPISODE = TABLE_NAME + "." + COL_ISTVEPISODE;
	public static final String MEDIA_YEAR = TABLE_NAME + "." + COL_MEDIA_YEAR;
	public static final String MOVIEORSHOWNAME = TABLE_NAME + "." + COL_MOVIEORSHOWNAME;
	public static final String MOVIEORSHOWNAMESIMPLE = TABLE_NAME + "." + COL_MOVIEORSHOWNAMESIMPLE;
	public static final String TVEPISODENAME = TABLE_NAME + ".TVEPISODENAME";
	public static final String TVEPISODENUMBER = TABLE_NAME + "." + COL_TVEPISODENUMBER;
	public static final String TVSEASON = TABLE_NAME + "." + COL_TVSEASON;
	public static final String SQL_LEFT_JOIN_TABLE_FILES = "LEFT JOIN " + TABLE_NAME + " ON " + MediaTableFiles.ID + " = " + FILEID + " ";
	private static final String SQL_GET_VIDEO_METADATA_BY_FILEID = "SELECT " + BASIC_COLUMNS + " FROM " + TABLE_NAME + " WHERE " + COL_FILEID + " = ?";
	private static final String SQL_GET_API_METADATA_EXIST = "SELECT " + FILEID + " FROM " + TABLE_NAME + " " + " WHERE " + FILEID + " = ? LIMIT 1";
	private static final String SQL_GET_API_METADATA_IMDBID_EXIST = "SELECT " + FILEID + " FROM " + TABLE_NAME + " " + " WHERE " + FILEID + " = ? AND " + IMDBID + " IS NOT NULL LIMIT 1";
	private static final String SQL_GET_API_METADATA_API_IMDBID_VERSION_EXIST = "SELECT " + FILEID + " FROM " + TABLE_NAME + " WHERE " + FILEID + " = ? AND " + IMDBID + " IS NOT NULL AND " + API_VERSION + " = ? LIMIT 1";

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
				default:
					throw new IllegalStateException(
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
				"FILEID                  INTEGER                              PRIMARY KEY , " +
				"IMDBID                  VARCHAR(" + SIZE_IMDBID + ")                     , " +
				"MEDIA_YEAR              VARCHAR(" + SIZE_YEAR + ")                       , " +
				"MOVIEORSHOWNAME         VARCHAR(" + SIZE_MAX + ")                        , " +
				"MOVIEORSHOWNAMESIMPLE   VARCHAR(" + SIZE_MAX + ")                        , " +
				"TVSEASON                VARCHAR(" + SIZE_TVSEASON + ")                   , " +
				"TVEPISODENUMBER         VARCHAR(" + SIZE_TVEPISODENUMBER + ")            , " +
				"TVEPISODENAME           VARCHAR(" + SIZE_MAX + ")                        , " +
				"ISTVEPISODE             BOOLEAN                                          , " +
				"EXTRAINFORMATION        VARCHAR(" + SIZE_MAX + ")                        , " +
				"API_VERSION             VARCHAR(" + SIZE_IMDBID + ")                     , " +
				"BUDGET                  INTEGER                                          , " +
				"CREDITS                 CLOB                                             , " +
				"EXTERNALIDS             CLOB                                             , " +
				"HOMEPAGE                VARCHAR                                          , " +
				"IMAGES                  CLOB                                             , " +
				"ORIGINALLANGUAGE        VARCHAR                                          , " +
				"ORIGINALTITLE           VARCHAR                                          , " +
				"PRODUCTIONCOMPANIES     CLOB                                             , " +
				"PRODUCTIONCOUNTRIES     CLOB                                             , " +
				"REVENUE                 INTEGER                                          , " +
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
		String columns = "FILEID, " + BASIC_COLUMNS;
		if (apiExtendedMetadata != null) {
			columns += ", API_VERSION, " + TMDB_COLUMNS;
		}
		DLNAMediaVideoMetadata videoMetadata = media.getVideoMetadata();
		try (
			PreparedStatement updateStatement = connection.prepareStatement(
				"SELECT " + columns + " " +
				"FROM " + TABLE_NAME + " " +
				"WHERE " +
					"FILEID = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			);
			PreparedStatement insertStatement = connection.prepareStatement(
				"INSERT INTO " + TABLE_NAME + " (" + columns +	")" +
				createDefaultValueForInsertStatement(columns)
			);
		) {
			updateStatement.setLong(1, fileId);
			try (ResultSet rs = updateStatement.executeQuery()) {
				if (rs.next()) {
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
							rs.updateInt("BUDGET", apiExtendedMetadata.get("budget").getAsInt());
						}
						if (apiExtendedMetadata.has("credits")) {
							rs.updateString("CREDITS", apiExtendedMetadata.get("credits").toString());
						}
						if (apiExtendedMetadata.has("externalIDs")) {
							rs.updateString("EXTERNALIDS", apiExtendedMetadata.get("externalIDs").toString());
						}
						rs.updateString("HOMEPAGE", APIUtils.getStringOrNull(apiExtendedMetadata, "homepage"));
						if (apiExtendedMetadata.has("images")) {
							rs.updateString("IMAGES", apiExtendedMetadata.get("images").toString());
						}
						rs.updateString("ORIGINALLANGUAGE", APIUtils.getStringOrNull(apiExtendedMetadata, "originalLanguage"));
						rs.updateString("ORIGINALTITLE", APIUtils.getStringOrNull(apiExtendedMetadata, "originalTitle"));
						if (apiExtendedMetadata.has("productionCompanies")) {
							rs.updateString("PRODUCTIONCOMPANIES", apiExtendedMetadata.get("productionCompanies").toString());
						}
						if (apiExtendedMetadata.has("productionCountries")) {
							rs.updateString("PRODUCTIONCOUNTRIES", apiExtendedMetadata.get("productionCountries").toString());
						}
						if (apiExtendedMetadata.has("revenue")) {
							rs.updateInt("REVENUE", apiExtendedMetadata.get("revenue").getAsInt());
						}
					}
					rs.updateRow();
				} else {
					insertStatement.clearParameters();
					int databaseColumnIterator = 0;
					insertStatement.setLong(++databaseColumnIterator, fileId);
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getIMDbID(), SIZE_IMDBID));
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getYear(), SIZE_YEAR));
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getMovieOrShowName(), SIZE_MAX));
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getSimplifiedMovieOrShowName(), SIZE_MAX));
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getExtraInformation(), SIZE_MAX));
					insertStatement.setBoolean(++databaseColumnIterator, videoMetadata.isTVEpisode());
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getTVSeason(), SIZE_TVSEASON));
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getTVEpisodeNumber(), SIZE_TVEPISODENUMBER));
					insertStatement.setString(++databaseColumnIterator, StringUtils.left(videoMetadata.getTVEpisodeName(), SIZE_MAX));
					if (apiExtendedMetadata != null) {
						insertStatement.setString(++databaseColumnIterator, StringUtils.left(APIUtils.getApiDataVideoVersion(), SIZE_IMDBID));
						if (apiExtendedMetadata.has("budget")) {
							insertStatement.setInt(++databaseColumnIterator, apiExtendedMetadata.get("budget").getAsInt());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.INTEGER);
						}
						if (apiExtendedMetadata.has("credits")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("credits").toString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("externalIDs")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("externalIDs").toString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("homepage")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("homepage").getAsString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("images")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("images").toString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("originalLanguage")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("originalLanguage").getAsString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("originalTitle")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("originalTitle").getAsString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("productionCompanies")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("productionCompanies").toString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("productionCountries")) {
							insertStatement.setString(++databaseColumnIterator, apiExtendedMetadata.get("productionCountries").toString());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.VARCHAR);
						}
						if (apiExtendedMetadata.has("revenue")) {
							insertStatement.setInt(++databaseColumnIterator, apiExtendedMetadata.get("revenue").getAsInt());
						} else {
							insertStatement.setNull(++databaseColumnIterator, Types.INTEGER);
						}
					}
					insertStatement.executeUpdate();
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
		String sql = "SELECT * FROM " + TABLE_NAME + "WHERE " + FILEID + " = ? and " + IMDBID + " IS NOT NULL LIMIT 1";
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(sql)) {
				selectStatement.setLong(1, fileId);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						JsonObject result = new JsonObject();
						result.addProperty("imdbID", rs.getString(COL_IMDBID));
						addJsonElementToJsonObjectIfExists(result, "credits", rs.getString("CREDITS"));
						addJsonElementToJsonObjectIfExists(result, "externalIDs", rs.getString("EXTERNALIDS"));
						result.addProperty("homepage", rs.getString("HOMEPAGE"));
						addJsonElementToJsonObjectIfExists(result, "images", rs.getString("IMAGES"));
						result.add("actors", MediaTableVideoMetadataActors.getJsonArrayForFile(connection, fileId));
						result.addProperty("award", MediaTableVideoMetadataAwards.getValueForFile(connection, fileId));
						result.add("countries", MediaTableVideoMetadataCountries.getJsonArrayForFile(connection, fileId));
						result.add("directors", MediaTableVideoMetadataDirectors.getJsonArrayForFile(connection, fileId));
						result.add("genres", MediaTableVideoMetadataGenres.getJsonArrayForFile(connection, fileId));
						result.addProperty("imdbRating", MediaTableVideoMetadataIMDbRating.getValueForFile(connection, fileId));
						result.addProperty("poster", MediaTableVideoMetadataIMDbRating.getValueForFile(connection, fileId));
						result.addProperty("production", MediaTableVideoMetadataProduction.getValueForFile(connection, fileId));
						result.addProperty("rated", MediaTableVideoMetadataRated.getValueForFile(connection, fileId));
						result.add("rating", MediaTableVideoMetadataRatings.getJsonArrayForFile(connection, fileId));
						result.addProperty("released", MediaTableVideoMetadataReleased.getValueForFile(connection, fileId));
						//TODO add TV serie images
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
			updateRowsInTable(connection, oldName, newName, "MOVIEORSHOWNAME", SIZE_MAX, true);
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
