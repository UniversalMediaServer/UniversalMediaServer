/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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
	private static final int TABLE_VERSION = 5;

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
					LOGGER.trace("Clearing database metadata to populate new columns");
					try (Statement statement = connection.createStatement()) {
						StringBuilder sb = new StringBuilder();
						sb
							.append("UPDATE ")
								.append(TABLE_NAME)
							.append(" SET ")
								.append("IMDBID = NULL");
						statement.execute(sb.toString());
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
				"MEDIA_YEAR			VARCHAR2(1024)					, " +
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
	public static long set(final Connection connection, final HashMap tvSeries, final String seriesName) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;
		String condition;
		String simplifiedTitle;

		if (seriesName != null) {
			simplifiedTitle = FileUtil.getSimplifiedShowName(seriesName);
			condition = "SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle);
		} else if (isNotBlank((String) tvSeries.get("title"))) {
			simplifiedTitle = FileUtil.getSimplifiedShowName((String) tvSeries.get("title"));
			condition = "IMDBID = " + sqlQuote((String) tvSeries.get("imdbID"));
		} else {
			LOGGER.debug("Attempted to set TV series info with no series title: {}", (!tvSeries.isEmpty() ? tvSeries.toString() : "Nothing provided"));
			return -1;
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
							"ENDYEAR, IMDBID, PLOT, SIMPLIFIEDTITLE, STARTYEAR, TITLE, TOTALSEASONS, VOTES, MEDIA_YEAR, VERSION, " + TMDB_COLUMNS +
						") VALUES (" +
							"?, ?, ?, ?, ?, ?, ?, ?, ?, ?, " + TMDB_COLUMNS_PLACEHOLDERS +
						")";
					}
					try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {
						if (seriesName != null) {
							insertStatement.setString(1, simplifiedTitle);
							insertStatement.setString(2, seriesName);
						} else {
							insertStatement.setString(1, (String) tvSeries.get("endYear"));
							insertStatement.setString(2, (String) tvSeries.get("imdbID"));
							insertStatement.setString(3, (String) tvSeries.get("plot"));
							insertStatement.setString(4, simplifiedTitle);
							insertStatement.setString(5, (String) tvSeries.get("startYear"));
							insertStatement.setString(6, (String) tvSeries.get("title"));

							if (tvSeries.get("totalSeasons") != null) {
								insertStatement.setDouble(7, (Double) tvSeries.get("totalSeasons"));
							} else {
								insertStatement.setDouble(7, 0.0);
							}

							insertStatement.setString(8, (String) tvSeries.get("votes"));
							insertStatement.setString(9, (String) tvSeries.get("year"));
							insertStatement.setString(10, APIUtils.getApiDataSeriesVersion());

							// TMDB data, since v11
							if (tvSeries.get("createdBy") != null) {
								insertStatement.setString(11, StringUtils.join(tvSeries.get("createdBy"), ","));
							}
							insertStatement.setString(12, (String) tvSeries.get("credits"));
							insertStatement.setString(13, (String) tvSeries.get("externalIDs"));
							insertStatement.setString(14, (String) tvSeries.get("firstAirDate"));
							insertStatement.setString(15, (String) tvSeries.get("homepage"));
							insertStatement.setString(16, (String) tvSeries.get("images"));
							insertStatement.setBoolean(17, (Boolean) tvSeries.get("inProduction"));
							insertStatement.setString(18, (String) tvSeries.get("languages"));
							insertStatement.setString(19, (String) tvSeries.get("lastAirDate"));
							insertStatement.setString(20, (String) tvSeries.get("networks"));
							insertStatement.setDouble(21, (Double) tvSeries.get("numberOfEpisodes"));
							insertStatement.setDouble(22, (Double) tvSeries.get("numberOfSeasons"));
							insertStatement.setString(23, (String) tvSeries.get("originCountry"));
							insertStatement.setString(24, (String) tvSeries.get("originalLanguage"));
							insertStatement.setString(25, (String) tvSeries.get("originalTitle"));
							insertStatement.setString(26, (String) tvSeries.get("productionCompanies"));
							insertStatement.setString(27, (String) tvSeries.get("productionCountries"));
							insertStatement.setString(28, (String) tvSeries.get("seasons"));
							insertStatement.setString(29, (String) tvSeries.get("seriesType"));
							insertStatement.setString(30, (String) tvSeries.get("spokenLanguages"));
							insertStatement.setString(31, (String) tvSeries.get("status"));
							insertStatement.setString(32, (String) tvSeries.get("tagline"));
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
		} catch (Exception e) {
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
	public static void insertAPIMetadata(final Connection connection, final HashMap tvSeries) {
		if (tvSeries == null) {
			LOGGER.warn("Couldn't write API data for \"{}\" to the database because there is no media information");
			return;
		}
		String simplifiedTitle = FileUtil.getSimplifiedShowName((String) tvSeries.get("title"));

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
					rs.updateString("ENDYEAR", (String) tvSeries.get("endYear"));
					rs.updateString("IMDBID", (String) tvSeries.get("imdbID"));
					rs.updateString("PLOT", (String) tvSeries.get("plot"));
					rs.updateString("STARTYEAR", (String) tvSeries.get("startYear"));
					rs.updateString("TITLE", (String) tvSeries.get("title"));
					if (tvSeries.get("totalSeasons") != null) {
						rs.updateDouble("TOTALSEASONS", (Double) tvSeries.get("totalSeasons"));
					}
					rs.updateString("VERSION", APIUtils.getApiDataSeriesVersion());
					rs.updateString("VOTES", (String) tvSeries.get("votes"));
					rs.updateString("MEDIA_YEAR", (String) tvSeries.get("year"));

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
					rs.updateString("FIRSTAIRDATE", (String) tvSeries.get("firstAirDate"));
					rs.updateString("HOMEPAGE", (String) tvSeries.get("homepage"));
					if (tvSeries.get("images") != null) {
						json = GSON.toJson(tvSeries.get("images"));
						rs.updateString("IMAGES", json);
					}
					if (tvSeries.get("inProduction") != null) {
						rs.updateBoolean("INPRODUCTION", (Boolean) tvSeries.get("inProduction"));
					}
					if (tvSeries.get("languages") != null) {
						rs.updateString("LANGUAGES", StringUtils.join(tvSeries.get("languages"), ","));
					}
					rs.updateString("LASTAIRDATE", (String) tvSeries.get("lastAirDate"));
					if (tvSeries.get("networks") != null) {
						json = GSON.toJson(tvSeries.get("networks"));
						rs.updateString("NETWORKS", json);
					}
					if (tvSeries.get("numberOfEpisodes") != null) {
						rs.updateDouble("NUMBEROFEPISODES", (Double) tvSeries.get("numberOfEpisodes"));
					}
					if (tvSeries.get("numberOfSeasons") != null) {
						rs.updateDouble("NUMBEROFSEASONS", (Double) tvSeries.get("numberOfSeasons"));
					}
					if (tvSeries.get("originCountry") != null) {
						rs.updateString("ORIGINCOUNTRY", StringUtils.join(tvSeries.get("originCountry"), ","));
					}
					rs.updateString("ORIGINALLANGUAGE", (String) tvSeries.get("originalLanguage"));
					rs.updateString("ORIGINALTITLE", (String) tvSeries.get("originalTitle"));
					if (tvSeries.get("productionCompanies") != null) {
						json = GSON.toJson(tvSeries.get("productionCompanies"));
						rs.updateString("PRODUCTIONCOMPANIES", json);
					}
					if (tvSeries.get("productionCountries") != null) {
						json = GSON.toJson(tvSeries.get("productionCountries"));
						rs.updateString("PRODUCTIONCOUNTRIES", json);
					}
					if (tvSeries.get("seasons") != null) {
						json = GSON.toJson(tvSeries.get("seasons"));
						rs.updateString("SEASONS", json);
					}
					rs.updateString("SERIESTYPE", (String) tvSeries.get("seriesType"));
					if (tvSeries.get("spokenLanguages") != null) {
						json = GSON.toJson(tvSeries.get("spokenLanguages"));
						rs.updateString("SPOKENLANGUAGES", json);
					}
					rs.updateString("STATUS", (String) tvSeries.get("status"));
					rs.updateString("TAGLINE", (String) tvSeries.get("tagline"));
					rs.updateRow();
				} else {
					LOGGER.debug("Couldn't find \"{}\" in the database when trying to store data from our API", (String) tvSeries.get("title"));
					return;
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
