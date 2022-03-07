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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.dlna.DLNAThumbnail;
import net.pms.util.APIUtils;
import net.pms.util.FileUtil;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public final class MediaTableTVSeries extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableTVSeries.class);
	public static final String TABLE_NAME = "TV_SERIES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 3;

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
					version++;
					break;
				case 2:
					if (isColumnExist(connection, TABLE_NAME, "YEAR")) {
						LOGGER.trace("Renaming column name YEAR to MEDIA_YEAR");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
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
				"MEDIA_YEAR			VARCHAR2(1024)					  " +
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
							"ENDYEAR, IMDBID, PLOT, SIMPLIFIEDTITLE, STARTYEAR, TITLE, TOTALSEASONS, VOTES, MEDIA_YEAR, VERSION" +
						") VALUES (" +
							"?, ?, ?, ?, ?, ?, ?, ?, ?, ?" +
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
			sql.append("SELECT * FROM ").append(TABLE_NAME).append(" WHERE SIMPLIFIEDTITLE = ").append(sqlQuote(simplifiedTitle)).append(" ");
			String latestVersion = APIUtils.getApiDataSeriesVersion();
			if (latestVersion != null && CONFIGURATION.getExternalNetwork()) {
				sql.append("AND VERSION = ").append(sqlQuote(latestVersion)).append(" ");
			}
			sql.append("LIMIT 1");

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

		try {
			String sql = "SELECT THUMBNAIL " +
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
					return (DLNAThumbnail) resultSet.getObject("THUMBNAIL");
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
			LOGGER.trace("", e);
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
	 * Removes an entry or entries.
	 *
	 * @param connection the db connection
	 * @param imdbID the IMDb ID to remove
	 */
	public static void remove(final Connection connection, final String imdbID) {
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
