/*
 * Universal Media Server, for streaming any medias to DLNA
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.util.FileUtil;

public final class TableTVSeries extends Tables {
	/**
	 * TABLE_LOCK is used to synchronize database access on table level.
	 * H2 calls are thread safe, but the database's multithreading support is
	 * described as experimental. This lock therefore used in addition to SQL
	 * transaction locks. All access to this table must be guarded with this
	 * lock. The lock allows parallel reads.
	 */
	private static final ReadWriteLock TABLE_LOCK = new ReentrantReadWriteLock();
	private static final Logger LOGGER = LoggerFactory.getLogger(TableTVSeries.class);
	public static final String TABLE_NAME = "TV_SERIES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

	// No instantiation
	private TableTVSeries() {
	}

	/**
	 * Sets a new entry and returns the row ID.
	 *
	 * @param tvSeries data about this series from the API
	 * @param seriesName the name of the series, for when we don't have API data yet
	 * @return the new row ID
	 */
	public static long set(final HashMap tvSeries, final String seriesName) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;
		String condition;
		String simplifiedTitle;

		if (seriesName != null) {
			simplifiedTitle = FileUtil.getSimplifiedShowName(seriesName);
			condition = "SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle);
		} else {
			simplifiedTitle = FileUtil.getSimplifiedShowName((String) tvSeries.get("title"));
			condition = "IMDBID = " + sqlQuote((String) tvSeries.get("imdbID"));
		}

		try (Connection connection = database.getConnection()) {
			query = "SELECT * FROM " + TABLE_NAME + " WHERE " + condition + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching in " + TABLE_NAME + " with \"{}\" before set", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement selectStatement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				connection.setAutoCommit(false);
				try (ResultSet result = selectStatement.executeQuery()) {
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
								"ENDYEAR, IMDBID, PLOT, SIMPLIFIEDTITLE, STARTYEAR, TITLE, TOTALSEASONS, VOTES, YEAR" +
							") VALUES (" +
								"?, ?, ?, ?, ?, ?, ?, ?, ?" +
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
								insertStatement.setDouble(7, (Double) tvSeries.get("totalSeasons"));
								insertStatement.setString(8, (String) tvSeries.get("votes"));
								insertStatement.setString(9, (String) tvSeries.get("year"));
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
				} finally {
					connection.commit();
				}
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while writing to " + TABLE_NAME,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}

		return -1;
	}

	public static HashMap<String, Object> getByIMDbID(final String imdbID) {
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = database.getConnection()) {
			String query = "SELECT * FROM " + TABLE_NAME + " WHERE IMDBID = " + sqlQuote(imdbID) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						return convertSingleResultSetToList(resultSet);
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", imdbID, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Returns a row based on title.
	 *
	 * @param title
	 * @return
	 */
	public static HashMap<String, Object> getByTitle(final String title) {
		boolean trace = LOGGER.isTraceEnabled();

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);

		try (Connection connection = database.getConnection()) {
			String query = "SELECT * FROM " + TABLE_NAME + " WHERE SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle) + " LIMIT 1";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					if (resultSet.next()) {
						return convertSingleResultSetToList(resultSet);
					}
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", title, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * @param simplifiedTitle
	 * @return all data across all tables for a video file, if it has an IMDb ID stored.
	 */
	public static List<HashMap<String, Object>> getAPIResultsBySimplifiedTitleIncludingExternalTables(final String simplifiedTitle) {
		boolean trace = LOGGER.isTraceEnabled();

		try (Connection connection = database.getConnection()) {
			String query = "SELECT * " +
				"FROM " + TABLE_NAME + " " +
				"LEFT JOIN " + TableVideoMetadataActors.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataActors.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataAwards.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataAwards.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataCountries.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataCountries.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataDirectors.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataDirectors.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataGenres.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataGenres.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataProduction.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataProduction.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataPosters.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataPosters.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataRated.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataRated.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataRatings.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataRatings.TABLE_NAME + ".TVSERIESID " +
				"LEFT JOIN " + TableVideoMetadataReleased.TABLE_NAME + " ON " + TABLE_NAME + ".ID = " + TableVideoMetadataReleased.TABLE_NAME + ".TVSERIESID " +
				"WHERE SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle) + " and IMDBID != ''";

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			TABLE_LOCK.readLock().lock();
			try (Statement statement = connection.createStatement()) {
				try (ResultSet resultSet = statement.executeQuery(query)) {
					return convertResultSetToList(resultSet);
				}
			} finally {
				TABLE_LOCK.readLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", simplifiedTitle, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Returns a similar TV series name from the database.
	 *
	 * @param title
	 * @return
	 */
	public static String getSimilarTVSeriesName(String title) {
		if (title == null) {
			return title;
		}

		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		simplifiedTitle = StringEscapeUtils.escapeSql(simplifiedTitle);

		ArrayList<String> titleList = PMS.get().getDatabase().getStrings("SELECT TITLE FROM " + TableTVSeries.TABLE_NAME + " WHERE SIMPLIFIEDTITLE='" + simplifiedTitle + "' LIMIT 1");
		if (titleList.size() > 0) {
			return titleList.get(0);
		}

		return null;
	}

	/**
	 * Updates an existing row with information from our API.
	 *
	 * @param tvSeries
	 */
	public static void insertAPIMetadata(final HashMap tvSeries) {
		if (tvSeries == null) {
			LOGGER.warn("Couldn't write API data for \"{}\" to the database because there is no media information");
			return;
		}
		String simplifiedTitle = FileUtil.getSimplifiedShowName((String) tvSeries.get("title"));

		try (Connection connection = database.getConnection()) {
			TABLE_LOCK.writeLock().lock();
			connection.setAutoCommit(false);
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"* " +
				"FROM " + TableTVSeries.TABLE_NAME + " " +
				"WHERE " +
					"SIMPLIFIEDTITLE = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, simplifiedTitle);
				LOGGER.trace("Inserting API metadata for " + simplifiedTitle + ": " + tvSeries.toString());
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						rs.updateString("ENDYEAR", (String) tvSeries.get("endYear"));
						rs.updateString("IMDBID", (String) tvSeries.get("imdbID"));
						rs.updateString("PLOT", (String) tvSeries.get("plot"));
						rs.updateString("STARTYEAR", (String) tvSeries.get("startYear"));
						rs.updateString("TITLE", (String) tvSeries.get("title"));
						rs.updateDouble("TOTALSEASONS", (Double) tvSeries.get("totalSeasons"));
						rs.updateString("VOTES", (String) tvSeries.get("votes"));
						rs.updateString("YEAR", (String) tvSeries.get("year"));
						rs.updateRow();
					} else {
						LOGGER.debug("Couldn't find \"{}\" in the database when trying to store data from our API", (String) tvSeries.get("title"));
						return;
					}
				} finally {
					TABLE_LOCK.writeLock().unlock();
				}
			}
			connection.commit();
		} catch (Exception e) {
			LOGGER.debug("Error when attempting to insert API data to TV series entry: \"{}\"", e.getMessage());
		}
	}

	/**
	 * Removes an entry or entries.
	 *
	 * @param imdbID the IMDb ID to remove
	 */
	public static void remove(final String imdbID) {
		try (Connection connection = database.getConnection()) {
			String query = "DELETE FROM " + TABLE_NAME + " WHERE IMDBID = " + sqlQuote(imdbID);
			TABLE_LOCK.writeLock().lock();
			try (Statement statement = connection.createStatement()) {
				int rows = statement.executeUpdate(query);
				LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for imdbID \"{}\"", rows, imdbID);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Database error while removing entries from " + TABLE_NAME + " for \"{}\": {}",
				imdbID,
				e.getMessage()
			);
			LOGGER.trace("", e);
		}
	}

	/**
	 * Checks and creates or upgrades the table as needed.
	 *
	 * @param connection the {@link Connection} to use
	 *
	 * @throws SQLException
	 */
	protected static void checkTable(final Connection connection) throws SQLException {
		TABLE_LOCK.writeLock().lock();
		try {
			if (tableExists(connection, TABLE_NAME)) {
				Integer version = getTableVersion(connection, TABLE_NAME);
				if (version != null) {
					if (version < TABLE_VERSION) {
//						upgradeTable(connection, version);
					} else if (version > TABLE_VERSION) {
						LOGGER.warn(
							"Database table \"" + TABLE_NAME +
							"\" is from a newer version of UMS. If you experience problems, you could try to move, rename or delete database file \"" +
							database.getDatabaseFilename() +
							"\" before starting UMS"
						);
					}
				} else {
					LOGGER.warn("Database table \"{}\" has an unknown version and cannot be used. Dropping and recreating table", TABLE_NAME);
					dropTable(connection, TABLE_NAME);
					createTable(connection);
					setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
				}
			} else {
				createTable(connection);
				setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
			}
		} finally {
			TABLE_LOCK.writeLock().unlock();
		}
	}

	public static void updateThumbnailId(long id, int thumbId) {
		try (Connection conn = database.getConnection()) {
			TABLE_LOCK.writeLock().lock();
			try (
				PreparedStatement ps = conn.prepareStatement(
					"UPDATE " + TABLE_NAME + " SET THUMBID = ? WHERE ID = ?"
				);
			) {
				ps.setInt(1, thumbId);
				ps.executeUpdate();
				LOGGER.trace("THUMBID updated to {} for {}", thumbId, id);
			} finally {
				TABLE_LOCK.writeLock().unlock();
			}
		} catch (SQLException se) {
			LOGGER.error("Error updating cached thumbnail for \"{}\": {}", se.getMessage());
			LOGGER.trace("", se);
		}
	}

	/**
	 * Must be called from inside a table lock
	 */
	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID       IDENTITY PRIMARY KEY, " +
					"ENDYEAR    VARCHAR2(1024), " +
					"IMDBID    VARCHAR2(1024), " +
					"THUMBID    BIGINT, " +
					"PLOT    VARCHAR2(20000), " +
					"STARTYEAR    VARCHAR2(1024), " +
					"TITLE    VARCHAR2(1024) NOT NULL, " +
					"SIMPLIFIEDTITLE    VARCHAR2(1024) NOT NULL, " +
					"TOTALSEASONS    DOUBLE, " +
					"VOTES    VARCHAR2(1024), " +
					"YEAR    VARCHAR2(1024) " +
				")"
			);
			statement.execute("CREATE INDEX IMDBID_IDX ON " + TABLE_NAME + "(IMDBID)");
			statement.execute("CREATE INDEX TITLE_IDX ON " + TABLE_NAME + "(TITLE)");
			statement.execute("CREATE INDEX SIMPLIFIEDTITLE_IDX ON " + TABLE_NAME + "(SIMPLIFIEDTITLE)");
		}
	}
}
