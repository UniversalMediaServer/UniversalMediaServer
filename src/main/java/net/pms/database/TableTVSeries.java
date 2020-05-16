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
	public static int set(final HashMap tvSeries, final String seriesName) {
		boolean trace = LOGGER.isTraceEnabled();
		String query;
		String condition;
		String simplifiedTitle;

		if (seriesName != null) {
			LOGGER.info("6 " + seriesName);
			simplifiedTitle = FileUtil.getSimplifiedShowName(seriesName);
			condition = "SIMPLIFIEDTITLE = " + sqlQuote(simplifiedTitle);
		} else {
			LOGGER.info("7 " + (String) tvSeries.get("title"));
			simplifiedTitle = FileUtil.getSimplifiedShowName((String) tvSeries.get("title"));
			condition = "IMDBID = " + sqlQuote((String) tvSeries.get("imdbID"));
		}

		try (Connection connection = database.getConnection()) {
			query = "SELECT * FROM " + TABLE_NAME + " WHERE " + condition + " LIMIT 1";
			if (trace) {
				LOGGER.trace("Searching in " + TABLE_NAME + " with \"{}\" before set", query);
			}

			TABLE_LOCK.writeLock().lock();
			try (PreparedStatement statement = connection.prepareStatement(query, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE, Statement.RETURN_GENERATED_KEYS)) {
				connection.setAutoCommit(false);
				try (ResultSet result = statement.executeQuery()) {
					if (result.next()) {
						if (trace) {
							LOGGER.trace("Found entry in " + TABLE_NAME);
						}
						return result.getInt("ID");
					} else {
						if (trace) {
							LOGGER.trace("Entry \"{}\" not found in " + TABLE_NAME + ", inserting", simplifiedTitle);
						}
						result.moveToInsertRow();

						if (seriesName != null) {
							result.updateString("SIMPLIFIEDTITLE", simplifiedTitle);
							result.updateString("TITLE", seriesName);
								LOGGER.info("8 ");
						} else {
							result.updateString("AWARDS", (String) tvSeries.get("awards"));
							result.updateString("COUNTRY", (String) tvSeries.get("country"));
							result.updateString("ENDYEAR", (String) tvSeries.get("endYear"));
							result.updateString("IMDBID", (String) tvSeries.get("imdbID"));
							result.updateString("METASCORE", (String) tvSeries.get("metascore"));
							result.updateString("PLOT", (String) tvSeries.get("plot"));
							result.updateString("POSTER", (String) tvSeries.get("poster"));
							result.updateString("RATED", (String) tvSeries.get("rated"));
							result.updateDouble("RATING", (Double) tvSeries.get("rating"));
							result.updateString("SIMPLIFIEDTITLE", simplifiedTitle);
							result.updateString("STARTYEAR", (String) tvSeries.get("startYear"));
							result.updateString("TITLE", (String) tvSeries.get("title"));
							result.updateDouble("TOTALSEASONS", (Double) tvSeries.get("totalSeasons"));
							result.updateString("VOTES", (String) tvSeries.get("votes"));
							result.updateString("YEAR", (String) tvSeries.get("year"));
								LOGGER.info("9 ");
						}
						result.insertRow();
								LOGGER.info("10 ");
						try (ResultSet rs = statement.getGeneratedKeys()) {
								LOGGER.info("11 ");
							if (rs.next()) {
																LOGGER.info("13 " + rs.getInt(1));
LOGGER.info("12 " + rs.getInt("ID"));
								return rs.getInt("ID");
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
						LOGGER.info("55 found " + title);
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
		String titleSimplified = FileUtil.getSimplifiedShowName((String) tvSeries.get("title"));

		try (Connection connection = database.getConnection()) {
			connection.setAutoCommit(false);
			try (PreparedStatement ps = connection.prepareStatement(
				"SELECT " +
					"* " +
				"FROM " + TableTVSeries.TABLE_NAME + " " +
				"WHERE " +
					"TITLESIMPLIFIED = ?",
				ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE
			)) {
				ps.setString(1, titleSimplified);
				TABLE_LOCK.writeLock().lock();
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						rs.updateString("AWARDS", (String) tvSeries.get("awards"));
						rs.updateString("COUNTRY", (String) tvSeries.get("country"));
						rs.updateString("ENDYEAR", (String) tvSeries.get("endYear"));
						rs.updateString("IMDBID", (String) tvSeries.get("imdbid"));
						rs.updateString("METASCORE", (String) tvSeries.get("metascore"));
						rs.updateString("PLOT", (String) tvSeries.get("plot"));
						rs.updateString("POSTER", (String) tvSeries.get("poster"));
						rs.updateString("RATED", (String) tvSeries.get("rated"));
						rs.updateDouble("RATING", (Double) tvSeries.get("rating"));
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

	/**
	 * Must be called from inside a table lock
	 */
	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug("Creating database table: \"{}\"", TABLE_NAME);
		try (Statement statement = connection.createStatement()) {
			statement.execute(
				"CREATE TABLE " + TABLE_NAME + "(" +
					"ID       IDENTITY PRIMARY KEY, " +
					"AWARDS   VARCHAR2(1024), " +
					"COUNTRY    VARCHAR2(1024), " +
					"ENDYEAR    VARCHAR2(1024), " +
					"IMDBID    VARCHAR2(1024), " +
					"METASCORE    VARCHAR2(1024), " +
					"PLOT    VARCHAR2(20000), " +
					"POSTER    VARCHAR2(1024), " +
					"RATED    VARCHAR2(1024), " +
					"RATING    DOUBLE, " +
					"STARTYEAR    VARCHAR2(1024), " +
					"TITLE    VARCHAR2(1024) NOT NULL, " +
					"SIMPLIFIEDTITLE    VARCHAR2(1024) NOT NULL, " +
					"TOTALSEASONS    DOUBLE, " +
					"VOTES    VARCHAR2(1024), " +
					"YEAR    VARCHAR2(1024) " +
				")"
			);
			// we also recieve the following from the API series request:
			// actors: { type: Array },
			// directors: { type: Array },
			// genres: { type: Array },
			// rating: { type: Number },
			// ratings: { type: Array, required: true },
			statement.execute("CREATE INDEX IMDBID_IDX ON " + TABLE_NAME + "(IMDBID)");
			statement.execute("CREATE INDEX TITLE_IDX ON " + TABLE_NAME + "(TITLE)");
			statement.execute("CREATE INDEX SIMPLIFIEDTITLE_IDX ON " + TABLE_NAME + "(SIMPLIFIEDTITLE)");
		}
	}
}
