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

import com.google.gson.*;
import java.io.EOFException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.h2.jdbc.JdbcSQLDataException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.util.APIUtils;
import net.pms.util.FileUtil;
import net.pms.util.UnknownFormatException;
import net.pms.util.UriFileRetriever;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class MediaTableTVSeries extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableTVSeries.class);
	public static final String TABLE_NAME = "TV_SERIES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 6;

	/**
	 * The columns we added from TMDB in V11
	 */
	private static final String TMDB_COLUMNS = "CREATEDBY, CREDITS, EXTERNALIDS, FIRSTAIRDATE, HOMEPAGE, IMAGES, INPRODUCTION, LANGUAGES, LASTAIRDATE, NETWORKS, NUMBEROFEPISODES, NUMBEROFSEASONS, ORIGINCOUNTRY, ORIGINALLANGUAGE, ORIGINALTITLE, PRODUCTIONCOMPANIES, PRODUCTIONCOUNTRIES, SEASONS, SERIESTYPE, SPOKENLANGUAGES, STATUS, TAGLINE";
	private static final String TMDB_COLUMNS_PLACEHOLDERS = "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?";

	private static final Gson GSON = new Gson();

	private static final UriFileRetriever URI_FILE_RETRIEVER = new UriFileRetriever();

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
				case 1:
					try (Statement statement = connection.createStatement()) {
						if (!isColumnExist(connection, TABLE_NAME, "VERSION")) {
							statement.execute("ALTER TABLE " + TABLE_NAME + " ADD VERSION VARCHAR2");
							statement.execute("CREATE INDEX IMDBID_VERSION ON " + TABLE_NAME + "(IMDBID, VERSION)");
						}
					} catch (SQLException e) {
						LOGGER.error("Failed upgrading database table {} for {}", TABLE_NAME, e.getMessage());
						LOGGER.error("Please use the 'Reset the cache' button on the 'Navigation Settings' tab, close UMS and start it again.");
						throw new SQLException(e);
					}
					break;
				case 2:
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
					}
					break;
				case 3:
					LOGGER.trace("Adding TMDB columns");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS CREATEDBY VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS CREDITS VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS EXTERNALIDS VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS FIRSTAIRDATE VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS HOMEPAGE VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS IMAGES VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS INPRODUCTION VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS LANGUAGES VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS LASTAIRDATE VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS NETWORKS VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS NUMBEROFEPISODES DOUBLE");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS NUMBEROFSEASONS DOUBLE");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS ORIGINCOUNTRY VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS ORIGINALLANGUAGE VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS ORIGINALTITLE VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS PRODUCTIONCOMPANIES VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS PRODUCTIONCOUNTRIES VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS SEASONS VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS SERIESTYPE VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS SPOKENLANGUAGES VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS STATUS VARCHAR2");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS TAGLINE VARCHAR2");
					break;
				case 4:
					// This version was for testing, left here to not break tester dbs
					break;
				case 5:
					if (isColumnExist(connection, TABLE_NAME, "INPRODUCTION")) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN INPRODUCTION");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN INPRODUCTION BOOLEAN");
					}
					break;
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
			throw new SQLException(e);
		}
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				"ID					IDENTITY			PRIMARY KEY	, " +
				"ENDYEAR			VARCHAR2(1024)					, " +
				"IMDBID				VARCHAR2(1024)					, " +
				"THUMBID			BIGINT							, " +
				"PLOT				VARCHAR2(20000)					, " +
				"STARTYEAR			VARCHAR2(1024)					, " +
				"TITLE				VARCHAR2(1024)		NOT NULL	, " +
				"SIMPLIFIEDTITLE    VARCHAR2(1024)		NOT NULL	, " +
				"TOTALSEASONS		DOUBLE							, " +
				"VERSION			VARCHAR2(1024)					, " +
				"VOTES				VARCHAR2(1024)					, " +
				"CREATEDBY VARCHAR2, " +
				"CREDITS VARCHAR2, " +
				"EXTERNALIDS VARCHAR2, " +
				"FIRSTAIRDATE VARCHAR2, " +
				"HOMEPAGE VARCHAR2, " +
				"IMAGES VARCHAR2, " +
				"INPRODUCTION BOOLEAN, " +
				"LANGUAGES VARCHAR2, " +
				"LASTAIRDATE VARCHAR2, " +
				"NETWORKS VARCHAR2, " +
				"NUMBEROFEPISODES DOUBLE, " +
				"NUMBEROFSEASONS DOUBLE, " +
				"ORIGINCOUNTRY VARCHAR2, " +
				"ORIGINALLANGUAGE VARCHAR2, " +
				"ORIGINALTITLE VARCHAR2, " +
				"PRODUCTIONCOMPANIES VARCHAR2, " +
				"PRODUCTIONCOUNTRIES VARCHAR2, " +
				"SEASONS VARCHAR2, " +
				"SERIESTYPE VARCHAR2, " +
				"SPOKENLANGUAGES VARCHAR2, " +
				"STATUS VARCHAR2, " +
				"TAGLINE VARCHAR2" +
			")",
			"CREATE INDEX IMDBID_IDX ON " + TABLE_NAME + "(IMDBID)",
			"CREATE INDEX TITLE_IDX ON " + TABLE_NAME + "(TITLE)",
			"CREATE INDEX SIMPLIFIEDTITLE_IDX ON " + TABLE_NAME + "(SIMPLIFIEDTITLE)",
			"CREATE INDEX IMDBID_VERSION ON " + TABLE_NAME + "(IMDBID, VERSION)"
		);
	}

	/**
	 * Sets a new entry and returns the row ID.
	 *
	 * @param connection the db connection
	 * @param tvSeries data about this series from the API
	 * @param seriesName the name of the series, for when we don't have API data yet
	 * @return the new row ID
	 */
	public static long set(final Connection connection, final JsonObject tvSeries, final String seriesName) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;
		String condition;
		String simplifiedTitle;

		if (seriesName != null) {
			simplifiedTitle = FileUtil.getSimplifiedShowName(seriesName);
			condition = "SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle);
		} else {
			String title = APIUtils.getStringOrNull(tvSeries, "title");
			if (isNotBlank(title)) {
				simplifiedTitle = FileUtil.getSimplifiedShowName(title);
				condition = "IMDBID = " + sqlQuote(APIUtils.getStringOrNull(tvSeries, "imdbID"));
			} else {
				LOGGER.debug("Attempted to set TV series info with no series title: {}", (tvSeries != null ? tvSeries.toString() : "Nothing provided"));
				return -1;
			}
		}

		try {
			query = "SELECT * FROM " + TABLE_NAME + " WHERE " + condition + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching in " + TABLE_NAME + " with \"{}\" before set", query);
			}

			try (
				PreparedStatement selectStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
				ResultSet result = selectStatement.executeQuery()
			) {
				if (result.next()) {
					if (trace) {
						LOGGER.trace("Found entry in " + TABLE_NAME);
					}
					return result.getLong("ID");
				} else {
					if (trace) {
						LOGGER.trace("Entry \"{}\" not found in " + TABLE_NAME + ", inserting", simplifiedTitle);
					}

					String insertQuery;
					if (seriesName != null) {
						insertQuery = "INSERT INTO " + TABLE_NAME + " (SIMPLIFIEDTITLE, TITLE) VALUES (?, ?)";
					} else {
						insertQuery = "INSERT INTO " + TABLE_NAME + " (" +
							"ENDYEAR, IMDBID, PLOT, SIMPLIFIEDTITLE, STARTYEAR, TITLE, TOTALSEASONS, VOTES, VERSION, " + TMDB_COLUMNS +
						") VALUES (" +
							"?, ?, ?, ?, ?, ?, ?, ?, ?, " + TMDB_COLUMNS_PLACEHOLDERS +
						")";
					}
					try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
						if (seriesName != null) {
							insertStatement.setString(1, simplifiedTitle);
							insertStatement.setString(2, seriesName);
						} else {
							insertStatement.setString(1, APIUtils.getStringOrNull(tvSeries, "endYear"));
							insertStatement.setString(2, APIUtils.getStringOrNull(tvSeries, "imdbID"));
							insertStatement.setString(3, APIUtils.getStringOrNull(tvSeries, "plot"));
							insertStatement.setString(4, simplifiedTitle);
							insertStatement.setString(5, APIUtils.getStringOrNull(tvSeries, "startYear"));
							insertStatement.setString(6, APIUtils.getStringOrNull(tvSeries, "title"));

							if (tvSeries.has("totalSeasons")) {
								insertStatement.setDouble(7, tvSeries.get("totalSeasons").getAsDouble());
							} else {
								insertStatement.setDouble(7, 0.0);
							}

							insertStatement.setString(8, APIUtils.getStringOrNull(tvSeries, "votes"));
							insertStatement.setString(9, APIUtils.getApiDataSeriesVersion());

							// TMDB data, since v11
							if (tvSeries.has("createdBy")) {
								insertStatement.setString(10, tvSeries.get("createdBy").toString());
							}
							if (tvSeries.has("credits")) {
								insertStatement.setString(11, tvSeries.get("credits").toString());
							}
							if (tvSeries.has("externalIDs")) {
								insertStatement.setString(12, tvSeries.get("externalIDs").toString());
							}
							insertStatement.setString(13, APIUtils.getStringOrNull(tvSeries, "firstAirDate"));
							insertStatement.setString(14, APIUtils.getStringOrNull(tvSeries, "homepage"));
							if (tvSeries.has("images")) {
								insertStatement.setString(15, tvSeries.get("images").toString());
							}
							if (tvSeries.has("inProduction")) {
								insertStatement.setBoolean(16, tvSeries.get("inProduction").getAsBoolean());
							} else {
								insertStatement.setBoolean(16, false);
							}
							if (tvSeries.has("languages")) {
								insertStatement.setString(17, tvSeries.get("languages").toString());
							}
							insertStatement.setString(18, APIUtils.getStringOrNull(tvSeries, "lastAirDate"));
							if (tvSeries.has("networks")) {
								insertStatement.setString(19, tvSeries.get("networks").toString());
							}
							if (tvSeries.has("numberOfEpisodes")) {
								insertStatement.setDouble(20, tvSeries.get("numberOfEpisodes").getAsDouble());
							} else {
								insertStatement.setNull(20, Types.DOUBLE);
							}
							if (tvSeries.has("numberOfSeasons")) {
								insertStatement.setDouble(21, tvSeries.get("numberOfSeasons").getAsDouble());
							} else {
								insertStatement.setNull(21, Types.DOUBLE);
							}
							if (tvSeries.has("originCountry")) {
								insertStatement.setString(22, tvSeries.get("originCountry").toString());
							}
							insertStatement.setString(23, APIUtils.getStringOrNull(tvSeries, "originalLanguage"));
							insertStatement.setString(24, APIUtils.getStringOrNull(tvSeries, "originalTitle"));
							if (tvSeries.has("productionCompanies")) {
								insertStatement.setString(25, tvSeries.get("productionCompanies").toString());
							}
							if (tvSeries.has("productionCountries")) {
								insertStatement.setString(26, tvSeries.get("productionCountries").toString());
							}
							if (tvSeries.has("seasons")) {
								insertStatement.setString(27, tvSeries.get("seasons").toString());
							}
							insertStatement.setString(28, APIUtils.getStringOrNull(tvSeries, "seriesType"));
							if (tvSeries.has("spokenLanguages")) {
								insertStatement.setString(29, tvSeries.get("spokenLanguages").toString());
							}
							insertStatement.setString(30, APIUtils.getStringOrNull(tvSeries, "status"));
							insertStatement.setString(31, APIUtils.getStringOrNull(tvSeries, "tagline"));
						}
						insertStatement.executeUpdate();

						try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
							if (generatedKeys.next()) {
								return generatedKeys.getLong(1);
							} else {
								LOGGER.debug("Generated key not returned in " + TABLE_NAME);
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing", TABLE_NAME, "tv Series", e.getMessage());
			LOGGER.trace("", e);
		}

		return -1;
	}

	/**
	 * Get TV series by IMDb ID.
	 * If we have the latest version number from the
	 * API, narrow the result to that version.
	 * @param connection the db connection
	 * @param imdbID
	 * @return
	 */
	public static HashMap<String, Object> getByIMDbID(final Connection connection, final String imdbID) {
		boolean trace = LOGGER.isTraceEnabled();
		StringBuilder sql = new StringBuilder();
		sql.append("SELECT * FROM ").append(TABLE_NAME).append(" WHERE IMDBID = ").append(sqlQuote(imdbID)).append(" ");
		String latestVersion = APIUtils.getApiDataSeriesVersion();
		if (latestVersion != null && CONFIGURATION.getExternalNetwork()) {
			sql.append("AND VERSION = ").append(sqlQuote(latestVersion)).append(" ");
		}
		sql.append("LIMIT 1");

		try {
			if (trace) {
				LOGGER.trace("Searching {} with \"{}\"", TABLE_NAME, sql);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql.toString())
			) {
				if (resultSet.next()) {
					return convertSingleResultSetToList(resultSet);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading tv series from imdbID", imdbID, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Returns a row based on title.
	 *
	 * @param connection the db connection
	 * @param title
	 * @return
	 */
	public static HashMap<String, Object> getByTitle(final Connection connection, final String title) {
		boolean trace = LOGGER.isTraceEnabled();

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);

		try {
			StringBuilder sql = new StringBuilder();
			sql.append("SELECT * FROM ").append(TABLE_NAME).append(" WHERE SIMPLIFIEDTITLE = ").append(sqlQuote(simplifiedTitle)).append(" LIMIT 1");

			if (trace) {
				LOGGER.trace("Searching {} with \"{}\"", TABLE_NAME, sql);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql.toString())
			) {
				if (resultSet.next()) {
					return convertSingleResultSetToList(resultSet);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading tv series from title", title, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * @param connection the db connection
	 * @param title
	 * @return a thumbnail based on title.
	 */
	public static DLNAThumbnail getThumbnailByTitle(final Connection connection, final String title) {
		boolean trace = LOGGER.isTraceEnabled();

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		Integer thumbnailId = null;
		Integer tvSeriesId = null;

		try {
			String sql = "SELECT " + MediaTableThumbnails.TABLE_NAME + ".ID AS ThumbnailId, " + TABLE_NAME + ".ID as TVSeriesId, THUMBNAIL " +
				"FROM " + TABLE_NAME + " " +
				"LEFT JOIN " + MediaTableThumbnails.TABLE_NAME + " ON " + TABLE_NAME + ".THUMBID = " + MediaTableThumbnails.TABLE_NAME + ".ID " +
				"WHERE SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", sql);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)
			) {
				if (resultSet.next()) {
					thumbnailId = resultSet.getInt("ThumbnailId");
					tvSeriesId = resultSet.getInt("TVSeriesId");
					return (DLNAThumbnail) resultSet.getObject("THUMBNAIL");
				}
			} catch (JdbcSQLDataException e) {
				LOGGER.debug("Cached thumbnail for TV series {} seems to be from a previous version, regenerating", title);
				LOGGER.trace("", e);

				// Regenerate the thumbnail from a stored poster if it exists
				Object[] posterInfo = MediaTableVideoMetadataPosters.getByTVSeriesName(connection, title);
				if (posterInfo == null) {
					// this should never happen, since the only way to have a TV series thumbnail is from an API poster
					LOGGER.debug("No poster URI was found locally for {}, removing API information for TV series", title);
					if (thumbnailId != null) {
						MediaTableThumbnails.removeById(connection, thumbnailId);
						removeImdbIdById(connection, tvSeriesId);
					}
					return null;
				}

				String posterURL = (String) posterInfo[0];
				Long tvSeriesDatabaseId = (Long) posterInfo[1];
				try {
					byte[] image = URI_FILE_RETRIEVER.get(posterURL);
					DLNAThumbnail thumbnail = (DLNAThumbnail) DLNAThumbnail.toThumbnail(image, 640, 480, ScaleType.MAX, ImageFormat.JPEG, false);
					MediaTableThumbnails.setThumbnail(connection, thumbnail, null, tvSeriesDatabaseId, true);
					return thumbnail;
				} catch (EOFException e2) {
					LOGGER.debug(
						"Error reading \"{}\" thumbnail from posters table: Unexpected end of stream, probably corrupt or read error.",
						posterURL
					);
				} catch (UnknownFormatException e2) {
					LOGGER.debug("Could not read \"{}\" thumbnail from posters table: {}", posterURL, e2.getMessage());
				} catch (IOException e2) {
					LOGGER.error("Error reading \"{}\" thumbnail from posters table: {}", posterURL, e2.getMessage());
					LOGGER.trace("", e2);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading tv series thumbnail from title", title, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	public static void updateThumbnailId(final Connection connection, long id, int thumbId) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(
					"UPDATE " + TABLE_NAME + " SET THUMBID = ? WHERE ID = ?"
				);
			) {
				ps.setInt(1, thumbId);
				ps.setLong(2, id);
				ps.executeUpdate();
				LOGGER.trace("TV series THUMBID updated to {} for {}", thumbId, id);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "updating cached thumbnail", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * @param connection the db connection
	 * @param simplifiedTitle
	 * @return all data across all tables for a video file, if it has an IMDb ID stored.
	 */
	public static List<HashMap<String, Object>> getAPIResultsBySimplifiedTitleIncludingExternalTables(final Connection connection, final String simplifiedTitle) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			String sql = "SELECT * " +
				"FROM " + TABLE_NAME + " " +
				"LEFT JOIN " + MediaTableVideoMetadataActors.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataActors.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataAwards.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataAwards.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataCountries.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataCountries.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataDirectors.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataDirectors.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataGenres.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataGenres.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataProduction.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataProduction.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataPosters.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataPosters.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataRated.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataRated.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataRatings.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataRatings.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + MediaTableVideoMetadataReleased.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + MediaTableVideoMetadataReleased.TABLE_NAME + ".TVSERIESID " +
				"WHERE SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle) + " and IMDBID != ''";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", sql);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)
			) {
				return convertResultSetToList(resultSet);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "reading API results", TABLE_NAME, simplifiedTitle, e.getMessage());
			LOGGER.debug("", e);
		}

		return null;
	}

	/**
	 * Returns a similar TV series name from the database.
	 *
	 * @param connection the db connection
	 * @param title
	 * @return
	 */
	public static String getSimilarTVSeriesName(final Connection connection, String title) {
		if (title == null) {
			return title;
		}

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		simplifiedTitle = StringEscapeUtils.escapeSql(simplifiedTitle);

		ArrayList<String> titleList = MediaTableFiles.getStrings(connection, "SELECT TITLE FROM " + TABLE_NAME + " WHERE SIMPLIFIEDTITLE='" + simplifiedTitle + "' LIMIT 1");
		if (!titleList.isEmpty()) {
			return titleList.get(0);
		}

		return null;
	}

	/**
	 * Updates an existing row with information from our API.
	 *
	 * @param connection the db connection
	 * @param tvSeries
	 */
	public static void insertAPIMetadata(final Connection connection, final JsonObject tvSeries) {
		if (tvSeries == null) {
			LOGGER.warn("Couldn't write API data for \"{}\" to the database because there is no media information");
			return;
		}
		String title = APIUtils.getStringOrNull(tvSeries, "title");
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);

		try (
			PreparedStatement ps = connection.prepareStatement(
				"SELECT * " +
				"FROM " + MediaTableTVSeries.TABLE_NAME + " " +
				"WHERE SIMPLIFIEDTITLE = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)
		) {
			ps.setString(1, simplifiedTitle);
			LOGGER.trace("Inserting API metadata for " + simplifiedTitle + ": " + tvSeries.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					String json;
					rs.updateString("ENDYEAR", APIUtils.getStringOrNull(tvSeries, "endYear"));
					rs.updateString("IMDBID", APIUtils.getStringOrNull(tvSeries, "imdbID"));
					rs.updateString("PLOT", APIUtils.getStringOrNull(tvSeries, "plot"));
					rs.updateString("STARTYEAR", APIUtils.getStringOrNull(tvSeries, "startYear"));
					rs.updateString("TITLE", title);
					if (tvSeries.get("totalSeasons") != null) {
						rs.updateDouble("TOTALSEASONS", tvSeries.get("totalSeasons").getAsDouble());
					}
					rs.updateString("VERSION", APIUtils.getApiDataSeriesVersion());
					rs.updateString("VOTES", APIUtils.getStringOrNull(tvSeries, "votes"));

					// TMDB columns added in V11
					if (tvSeries.get("createdBy") != null) {
						json = GSON.toJson(tvSeries.get("createdBy"));
						rs.updateString("CREATEDBY", json);
					}
					if (tvSeries.get("credits") != null) {
						json = GSON.toJson(tvSeries.get("credits"));
						rs.updateString("CREDITS", json);
					}
					if (tvSeries.get("externalIDs") != null) {
						json = GSON.toJson(tvSeries.get("externalIDs"));
						rs.updateString("EXTERNALIDS", json);
					}
					rs.updateString("FIRSTAIRDATE", APIUtils.getStringOrNull(tvSeries, "firstAirDate"));
					rs.updateString("HOMEPAGE", APIUtils.getStringOrNull(tvSeries, "homepage"));
					if (tvSeries.has("images")) {
						rs.updateString("IMAGES", tvSeries.get("images").toString());
					}
					if (tvSeries.has("inProduction")) {
						rs.updateBoolean("INPRODUCTION", tvSeries.get("inProduction").getAsBoolean());
					}
					if (tvSeries.has("languages")) {
						rs.updateString("LANGUAGES", tvSeries.get("languages").toString());
					}
					rs.updateString("LASTAIRDATE", APIUtils.getStringOrNull(tvSeries, "lastAirDate"));
					if (tvSeries.has("networks")) {
						rs.updateString("NETWORKS", tvSeries.get("networks").toString());
					}
					if (tvSeries.has("numberOfEpisodes")) {
						rs.updateDouble("NUMBEROFEPISODES", tvSeries.get("numberOfEpisodes").getAsDouble());
					}
					if (tvSeries.has("numberOfSeasons")) {
						rs.updateDouble("NUMBEROFSEASONS", tvSeries.get("numberOfSeasons").getAsDouble());
					}
					if (tvSeries.has("originCountry")) {
						rs.updateString("ORIGINCOUNTRY", tvSeries.get("originCountry").toString());
					}
					rs.updateString("ORIGINALLANGUAGE", APIUtils.getStringOrNull(tvSeries, "originalLanguage"));
					rs.updateString("ORIGINALTITLE", APIUtils.getStringOrNull(tvSeries, "originalTitle"));
					if (tvSeries.has("productionCompanies")) {
						rs.updateString("PRODUCTIONCOMPANIES", tvSeries.get("productionCompanies").toString());
					}
					if (tvSeries.has("productionCountries")) {
						rs.updateString("PRODUCTIONCOUNTRIES", tvSeries.get("productionCountries").toString());
					}
					if (tvSeries.has("seasons")) {
						rs.updateString("SEASONS", tvSeries.get("seasons").toString());
					}
					rs.updateString("SERIESTYPE", APIUtils.getStringOrNull(tvSeries, "seriesType"));
					if (tvSeries.has("spokenLanguages")) {
						rs.updateString("SPOKENLANGUAGES", tvSeries.get("spokenLanguages").toString());
					}
					rs.updateString("STATUS", APIUtils.getStringOrNull(tvSeries, "status"));
					rs.updateString("TAGLINE", APIUtils.getStringOrNull(tvSeries, "tagline"));
					rs.updateRow();
				} else {
					LOGGER.debug("Couldn't find \"{}\" in the database when trying to store data from our API", title);
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "inserting API data to TV series entry", simplifiedTitle, TABLE_NAME, e.getMessage());
		}
	}

	/**
	 * Removes an entry or entries by IMDb ID.
	 *
	 * @param connection the db connection
	 * @param imdbID the IMDb ID to remove
	 */
	public static void removeByImdbId(final Connection connection, final String imdbID) {
		try {
			String query = "DELETE FROM " + TABLE_NAME + " WHERE IMDBID = " + sqlQuote(imdbID);
			try (Statement statement = connection.createStatement()) {
				int rows = statement.executeUpdate(query);
				LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for imdbID \"{}\"", rows, imdbID);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entries", TABLE_NAME, imdbID, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes an entry by ID.
	 *
	 * @param connection the db connection
	 * @param id the ID to remove
	 */
	public static void removeImdbIdById(final Connection connection, final Integer id) {
		try {
			String query = "UPDATE " + TABLE_NAME + " SET IMDBID = null WHERE ID = " + id;
			try (Statement statement = connection.createStatement()) {
				int row = statement.executeUpdate(query);
				LOGGER.trace("Removed IMDb ID from {} in " + TABLE_NAME + " for ID \"{}\"", row, id);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entry", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static Boolean isFullyPlayed(final Connection connection, final String title) {
		boolean trace = LOGGER.isTraceEnabled();
		Boolean result = true;

		try {
			/*
			 * If there is one file for this TV series where ISFULLYPLAYED is
			 * not true, then this series is not fully played, otherwise it is.
			 *
			 * This backwards logic is used for performance since we only have
			 * to check one row instead of all rows.
			 */
			String sql = "SELECT FILES.MOVIEORSHOWNAME " +
				"FROM FILES " +
					"LEFT JOIN " + MediaTableFilesStatus.TABLE_NAME + " ON " +
					"FILES.FILENAME = " + MediaTableFilesStatus.TABLE_NAME + ".FILENAME " +
				"WHERE " +
					"FILES.FORMAT_TYPE = 4 AND " +
					"FILES.MOVIEORSHOWNAME = " + sqlQuote(title) + " AND " +
					"FILES.ISTVEPISODE AND " +
					MediaTableFilesStatus.TABLE_NAME + ".ISFULLYPLAYED IS NOT TRUE " +
				"LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", sql);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(sql)
			) {
				if (resultSet.next()) {
					result = false;
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "looking up TV series status", TABLE_NAME, title, e.getMessage());
			LOGGER.trace("", e);
		}

		return result;
	}

}
