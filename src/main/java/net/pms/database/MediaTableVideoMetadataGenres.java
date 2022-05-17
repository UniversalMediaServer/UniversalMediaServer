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
import java.util.HashSet;
import java.util.Iterator;
import static org.apache.commons.lang3.StringUtils.left;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MediaTableVideoMetadataGenres extends MediaTable {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaTableVideoMetadataGenres.class);
	public static final String TABLE_NAME = "VIDEO_METADATA_GENRES";

	/**
	 * Table version must be increased every time a change is done to the table
	 * definition. Table upgrade SQL must also be added to
	 * {@link #upgradeTable(Connection, int)}
	 */
	private static final int TABLE_VERSION = 1;

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
				default:
					throw new IllegalStateException(
						getMessage(LOG_UPGRADING_TABLE_MISSING, DATABASE_NAME, TABLE_NAME, version, TABLE_VERSION)
					);
			}
		}
		MediaTableTablesVersions.setTableVersion(connection, TABLE_NAME, TABLE_VERSION);
	}

	private static void createTable(final Connection connection) throws SQLException {
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				"ID				IDENTITY			PRIMARY KEY, " +
				"TVSERIESID		INT					DEFAULT -1, " +
				"FILENAME		VARCHAR2(1024)		DEFAULT '', " +
				"GENRE			VARCHAR2(1024)		NOT NULL" +
			")",
			"CREATE UNIQUE INDEX FILENAME_GENRE_TVSERIESID_IDX ON " + TABLE_NAME + "(FILENAME, GENRE, TVSERIESID)"
		);
	}

	/**
	 * @param connection the db connection
	 * @param tvSeriesTitle
	 * @return all data in this table for a TV series, if it has an IMDb ID stored.
	 */
	public static HashSet getByTVSeriesName(final Connection connection, final String tvSeriesTitle) {
		boolean trace = LOGGER.isTraceEnabled();

		try {
			String query = "SELECT GENRE FROM " + TABLE_NAME + " " +
				"LEFT JOIN " + MediaTableTVSeries.TABLE_NAME + " ON " + TABLE_NAME + ".TVSERIESID = " + MediaTableTVSeries.TABLE_NAME + ".ID " +
				"WHERE " + MediaTableTVSeries.TABLE_NAME + ".TITLE = " + sqlQuote(tvSeriesTitle);

			if (trace) {
				LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", query);
			}

			try (
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery(query)
			) {
				return convertResultSetToHashSet(resultSet);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "reading genres", TABLE_NAME, tvSeriesTitle, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Sets a new row if it doesn't already exist.
	 *
	 * @param connection the db connection
	 * @param fullPathToFile
	 * @param genres
	 * @param tvSeriesID
	 */
	public static void set(final Connection connection, final String fullPathToFile, final HashSet genres, final long tvSeriesID) {
		if (genres == null || genres.isEmpty()) {
			return;
		}

		try {
			Iterator<String> i = genres.iterator();
			while (i.hasNext()) {
				String genre = i.next();

				try (
					PreparedStatement ps = connection.prepareStatement(
						"SELECT " +
							"ID " +
						"FROM " + TABLE_NAME + " " +
						"WHERE " +
							"TVSERIESID = ? AND " +
							"FILENAME = ? AND " +
							"GENRE = ? " +
						"LIMIT 1"
					)
				) {
					ps.setLong(1, tvSeriesID);
					ps.setString(2, left(fullPathToFile, 255));
					ps.setString(3, left(genre, 255));
					try (ResultSet rs = ps.executeQuery()) {
						if (rs.next()) {
							LOGGER.trace("Record already exists {} {} {}", tvSeriesID, fullPathToFile, genre);
						} else {
							try (
								PreparedStatement insertStatement = connection.prepareStatement(
									"INSERT INTO " + TABLE_NAME + " (" +
										"TVSERIESID, FILENAME, GENRE" +
									") VALUES (" +
										"?, ?, ?" +
									")",
									Statement.RETURN_GENERATED_KEYS
								)
							) {
								insertStatement.clearParameters();
								insertStatement.setLong(1, tvSeriesID);
								insertStatement.setString(2, left(fullPathToFile, 255));
								insertStatement.setString(3, left(genre, 255));

								insertStatement.executeUpdate();
								try (ResultSet rs2 = insertStatement.getGeneratedKeys()) {
									if (rs2.next()) {
										LOGGER.trace("Set new entry successfully in " + TABLE_NAME + " with \"{}\", \"{}\" and \"{}\"", fullPathToFile, tvSeriesID, genre);
									}
								}
							}
						}
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "writing genres", TABLE_NAME, fullPathToFile, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Removes an entry or entries based on its FILENAME. If {@code useLike} is
	 * {@code true}, {@code filename} must be properly escaped.
	 *
	 * @see Tables#sqlLikeEscape(String)
	 *
	 * @param connection the db connection
	 * @param filename the filename to remove
	 * @param useLike {@code true} if {@code LIKE} should be used as the compare
	 *            operator, {@code false} if {@code =} should be used.
	 */
	public static void remove(final Connection connection, final String filename, boolean useLike) {
		String query = "DELETE FROM " + TABLE_NAME + " WHERE FILENAME " +	(useLike ? "LIKE " : "= ") + sqlQuote(filename);
		try (Statement statement = connection.createStatement()) {
			int rows = statement.executeUpdate(query);
			LOGGER.trace("Removed entries {} in " + TABLE_NAME + " for filename \"{}\"", rows, filename);
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entries", TABLE_NAME, filename, e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
