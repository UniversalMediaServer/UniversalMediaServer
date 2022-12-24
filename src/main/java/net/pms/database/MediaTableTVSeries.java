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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import net.pms.dlna.DLNAThumbnail;
import net.pms.media.metadata.TvSeriesMetadata;
import net.pms.media.metadata.VideoMetadataLocalized;
import net.pms.util.APIUtils;
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
	 */
	private static final int TABLE_VERSION = 9;

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
	private static final String COL_THUMBNAIL = "THUMBNAIL";
	private static final String COL_TMDBID = "TMDBID";
	private static final String COL_SIMPLIFIEDTITLE = "SIMPLIFIEDTITLE";
	private static final String COL_STARTYEAR = "STARTYEAR";
	private static final String COL_TITLE = "TITLE";

	/**
	 * COLUMNS with table name
	 */
	public static final String TABLE_COL_ID = TABLE_NAME + "." + COL_ID;
	public static final String TABLE_COL_IMAGES = TABLE_NAME + "." + COL_IMAGES;
	public static final String TABLE_COL_IMDBID = TABLE_NAME + "." + COL_IMDBID;
	public static final String TABLE_COL_SIMPLIFIEDTITLE = TABLE_NAME + "." + COL_SIMPLIFIEDTITLE;
	public static final String TABLE_COL_STARTYEAR = TABLE_NAME + "." + COL_STARTYEAR;
	public static final String TABLE_COL_TITLE = TABLE_NAME + "." + COL_TITLE;
	public static final String TABLE_COL_THUMBID = TABLE_NAME + "." + COL_THUMBID;

	private static final String SQL_LEFT_JOIN_TABLE_THUMBNAILS = "LEFT JOIN " + MediaTableThumbnails.TABLE_NAME + " ON " + TABLE_COL_THUMBID + " = " + MediaTableThumbnails.TABLE_COL_ID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_GENRES = "LEFT JOIN " + MediaTableVideoMetadataGenres.TABLE_NAME + " ON " + TABLE_COL_ID + " = " + MediaTableVideoMetadataGenres.TABLE_COL_TVSERIESID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_IMDB_RATING = "LEFT JOIN " + MediaTableVideoMetadataIMDbRating.TABLE_NAME + " ON " + TABLE_COL_ID + " = " + MediaTableVideoMetadataIMDbRating.TABLE_COL_TVSERIESID + " ";
	public static final String SQL_LEFT_JOIN_TABLE_VIDEO_METADATA_RATED = "LEFT JOIN " + MediaTableVideoMetadataRated.TABLE_NAME + " ON " + TABLE_COL_ID + " = " + MediaTableVideoMetadataRated.TABLE_COL_TVSERIESID + " ";

	private static final String SQL_GET_BY_ID = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_ID + " = ? LIMIT 1";
	private static final String SQL_GET_BY_SIMPLIFIEDTITLE = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
	private static final String SQL_GET_ID_BY_SIMPLIFIEDTITLE = "SELECT " + TABLE_COL_ID + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
	private static final String SQL_GET_TITLE_BY_IMDBID = "SELECT " + TABLE_COL_TITLE + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_IMDBID + " = ? LIMIT 1";
	private static final String SQL_GET_TITLE_BY_IMDBID_API_VERSION = "SELECT " + TABLE_COL_TITLE + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_IMDBID + " = ? AND " + COL_API_VERSION + " = ? LIMIT 1";
	private static final String SQL_GET_IMAGES_BY_SIMPLIFIEDTITLE = "SELECT " + TABLE_COL_IMAGES + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
	private static final String SQL_GET_THUMBNAIL_BY_SIMPLIFIEDTITLE = "SELECT " + TABLE_COL_THUMBID + ", " + TABLE_COL_ID + ", " + MediaTableThumbnails.TABLE_COL_THUMBNAIL + " FROM " + TABLE_NAME + " " + SQL_LEFT_JOIN_TABLE_THUMBNAILS + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
	private static final String SQL_GET_STARTYEAR_BY_SIMPLIFIEDTITLE = "SELECT " + TABLE_COL_STARTYEAR + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
	private static final String SQL_GET_TITLE_BY_SIMPLIFIEDTITLE = "SELECT " + TABLE_COL_TITLE + " FROM " + TABLE_NAME + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
	private static final String SQL_GET_ISFULLYPLAYED = "SELECT " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " FROM " + MediaTableFiles.TABLE_NAME + " " + MediaTableFiles.SQL_LEFT_JOIN_TABLE_FILES_STATUS + MediaTableFiles.SQL_LEFT_JOIN_TABLE_VIDEO_METADATA + "WHERE " + MediaTableFiles.TABLE_COL_FORMAT_TYPE + " = 4 AND " + MediaTableVideoMetadata.TABLE_COL_MOVIEORSHOWNAME + " = ? AND " + MediaTableVideoMetadata.TABLE_COL_ISTVEPISODE + " AND " + MediaTableFilesStatus.TABLE_COL_ISFULLYPLAYED + " IS NOT TRUE LIMIT 1";
	private static final String SQL_UPDATE_THUMBID = "UPDATE " + TABLE_NAME + " SET " + COL_THUMBID + " = ? WHERE " + TABLE_COL_ID + " = ?";
	private static final String SQL_UPDATE_IMDBID_TMDBID_NULL = "UPDATE " + TABLE_NAME + " SET " + COL_IMDBID + " = null, " + COL_TMDBID + " = null WHERE " + TABLE_COL_ID + " = ?";
	private static final String SQL_INSERT_TITLE = "INSERT INTO " + TABLE_NAME + " (" + COL_SIMPLIFIEDTITLE + ", " + COL_TITLE + ") VALUES (?, ?)";

	public static final String REFERENCE_TABLE_COL_ID = TABLE_NAME + "(" + COL_ID + ")";

	/**
	 * Used by child tables
	 */
	public static final String CHILD_ID = "TVSERIESID";

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
							statement.execute("ALTER TABLE " + TABLE_NAME + " ADD VERSION VARCHAR");
							statement.execute("CREATE INDEX IMDBID_VERSION ON " + TABLE_NAME + "(IMDBID, VERSION)");
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
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ALTER COLUMN `YEAR` RENAME TO MEDIA_YEAR");
					}
				}
				case 3 -> {
					LOGGER.trace("Adding TMDB columns");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS CREATEDBY VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS CREDITS VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS EXTERNALIDS VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS FIRSTAIRDATE VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS HOMEPAGE VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS IMAGES VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS INPRODUCTION VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS LANGUAGES VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS LASTAIRDATE VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS NETWORKS VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS NUMBEROFEPISODES DOUBLE PRECISION");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS NUMBEROFSEASONS DOUBLE PRECISION");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS ORIGINCOUNTRY VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS ORIGINALLANGUAGE VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS ORIGINALTITLE VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS PRODUCTIONCOMPANIES VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS PRODUCTIONCOUNTRIES VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS SEASONS VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS SERIESTYPE VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS SPOKENLANGUAGES VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS STATUS VARCHAR");
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS TAGLINE VARCHAR");
				}
				case 4 -> {
					// This version was for testing, left here to not break tester dbs
				}
				case 5 -> {
					if (isColumnExist(connection, TABLE_NAME, "INPRODUCTION")) {
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " DROP COLUMN INPRODUCTION");
						executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN INPRODUCTION BOOLEAN");
					}
				}
				case 6 -> {
					executeUpdate(connection, "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_" + COL_THUMBID + "_IDX ON " + TABLE_NAME + "(" + COL_THUMBID + ")");

					//set old json datas to be rescanned
					if (isColumnExist(connection, TABLE_NAME, "VERSION")) {
						String[] badJsonColumns = {"LANGUAGES", "ORIGINCOUNTRY"};
						for (String badJsonColumn : badJsonColumns) {
							if (isColumnExist(connection, TABLE_NAME, badJsonColumn)) {
								executeUpdate(connection, "UPDATE " + TABLE_NAME + " SET " + COL_IMDBID + " = NULL WHERE RIGHT(" + badJsonColumn + ", 1) = ','");
							}
						}
					}
				}
				case 7 -> {
					//remove old index
					executeUpdate(connection, "DROP INDEX IF EXISTS IMDBID_VERSION");
					//change VERSION to API_VERSION
					if (!isColumnExist(connection, TABLE_NAME, COL_API_VERSION)) {
						executeUpdate(connection, "ALTER TABLE IF EXISTS " + TABLE_NAME + " ALTER COLUMN IF EXISTS VERSION RENAME TO " + COL_API_VERSION);
					}
					//add tmdb id
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS " + COL_TMDBID + " BIGINT");
					executeUpdate(connection, "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_" + COL_TMDBID + "_IDX ON " + TABLE_NAME + "(" + COL_TMDBID + ")");
					executeUpdate(connection, "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_" + COL_TMDBID + "_" + COL_IMDBID + "_" + COL_API_VERSION + "_IDX ON " + TABLE_NAME + "(" + COL_TMDBID + ", " + COL_IMDBID + ", " + COL_API_VERSION + ")");

					//uniformizing indexes name
					executeUpdate(connection, "ALTER INDEX IF EXISTS IMDBID_IDX RENAME TO " + TABLE_NAME + "_" + COL_IMDBID + "_IDX");
					executeUpdate(connection, "ALTER INDEX IF EXISTS TITLE_IDX RENAME TO " + TABLE_NAME + "_" + COL_TITLE + "_IDX");
					executeUpdate(connection, "ALTER INDEX IF EXISTS SIMPLIFIEDTITLE_IDX RENAME TO " + TABLE_NAME + "_" + COL_SIMPLIFIEDTITLE + "_IDX");
					//change PLOT to OVERVIEW
					if (!isColumnExist(connection, TABLE_NAME, COL_OVERVIEW)) {
						executeUpdate(connection, "ALTER TABLE IF EXISTS " + TABLE_NAME + " ALTER COLUMN IF EXISTS PLOT RENAME TO " + COL_OVERVIEW);
					}
				}
				case 8 -> {
					executeUpdate(connection, "ALTER TABLE " + TABLE_NAME + " ADD COLUMN IF NOT EXISTS " + COL_MODIFIED + " BIGINT");
					if (!isColumnExist(connection, TABLE_NAME, COL_TMDBID)) {
						executeUpdate(connection, "ALTER TABLE IF EXISTS " + TABLE_NAME + " ALTER COLUMN IF EXISTS TMDB_ID RENAME TO " + COL_TMDBID);
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
		LOGGER.debug(LOG_CREATING_TABLE, DATABASE_NAME, TABLE_NAME);
		execute(connection,
			"CREATE TABLE " + TABLE_NAME + "(" +
				COL_ID + "              IDENTITY           PRIMARY KEY , " +
				COL_MODIFIED + "        BIGINT                         , " +
				COL_IMDBID + "          VARCHAR(1024)                  , " +
				COL_TMDBID + "          BIGINT                         , " +
				COL_THUMBID + "         BIGINT                         , " +
				COL_OVERVIEW + "        VARCHAR(20000)                 , " +
				COL_TITLE + "           VARCHAR(1024)      NOT NULL    , " +
				COL_SIMPLIFIEDTITLE + " VARCHAR(1024)      NOT NULL    , " +
				"STARTYEAR              VARCHAR(1024)                  , " +
				"ENDYEAR                VARCHAR(1024)                  , " +
				"TOTALSEASONS           DOUBLE PRECISION               , " +
				COL_API_VERSION + "     VARCHAR(1024)                  , " +
				"VOTES                  VARCHAR(1024)                  , " +
				"CREATEDBY              VARCHAR                        , " +
				"CREDITS                VARCHAR                        , " +
				"EXTERNALIDS            VARCHAR                        , " +
				"FIRSTAIRDATE           VARCHAR                        , " +
				"HOMEPAGE               VARCHAR                        , " +
				COL_IMAGES + "          VARCHAR                        , " +
				"INPRODUCTION           BOOLEAN                        , " +
				"LANGUAGES              VARCHAR                        , " +
				"LASTAIRDATE            VARCHAR                        , " +
				"NETWORKS               VARCHAR                        , " +
				"NUMBEROFEPISODES       DOUBLE PRECISION               , " +
				"NUMBEROFSEASONS        DOUBLE PRECISION               , " +
				"ORIGINCOUNTRY          VARCHAR                        , " +
				"ORIGINALLANGUAGE       VARCHAR                        , " +
				"ORIGINALTITLE          VARCHAR                        , " +
				"PRODUCTIONCOMPANIES    VARCHAR                        , " +
				"PRODUCTIONCOUNTRIES    VARCHAR                        , " +
				"SEASONS                VARCHAR                        , " +
				"SERIESTYPE             VARCHAR                        , " +
				"SPOKENLANGUAGES        VARCHAR                        , " +
				"STATUS                 VARCHAR                        , " +
				"TAGLINE                VARCHAR                          " +
			")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_IMDBID + "_IDX ON " + TABLE_NAME + "(" + COL_IMDBID + ")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_TMDBID + "_IDX ON " + TABLE_NAME + "(" + COL_TMDBID + ")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_TMDBID + "_" + COL_IMDBID + "_" + COL_API_VERSION + "_IDX ON " + TABLE_NAME + "(" + COL_TMDBID + ", " + COL_IMDBID + ", " + COL_API_VERSION + ")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_TITLE + "_IDX ON " + TABLE_NAME + "(" + COL_TITLE + ")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_SIMPLIFIEDTITLE + "_IDX ON " + TABLE_NAME + "(" + COL_SIMPLIFIEDTITLE + ")",
			"CREATE INDEX " + TABLE_NAME + "_" + COL_THUMBID + "_IDX ON " + TABLE_NAME + "(" + COL_THUMBID + ")"
		);
	}

	/**
	 * Sets a new entry if not found and returns the row ID.
	 *
	 * @param connection the db connection
	 * @param title the title of the series
	 * @return the new row ID
	 */
	public static Long set(final Connection connection, final String title) {
		if (StringUtils.isBlank(title)) {
			LOGGER.debug("Attempted to set TV series info with no series title");
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_BY_SIMPLIFIEDTITLE, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
				selectStatement.setString(1, simplifiedTitle);
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
						try (PreparedStatement insertStatement = connection.prepareStatement(SQL_INSERT_TITLE, Statement.RETURN_GENERATED_KEYS)) {
							insertStatement.setString(1, simplifiedTitle);
							insertStatement.setString(2, title);
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
		if (tvSeriesId == null || tvSeriesId < 0) {
			return;
		}
		if (seriesMetadata == null) {
			LOGGER.warn("Couldn't write API data for \"{}\" to the database because there is no media information");
			return;
		}
		String title = seriesMetadata.getTitle();
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(
					SQL_GET_BY_ID,
					ResultSet.TYPE_FORWARD_ONLY,
					ResultSet.CONCUR_UPDATABLE
				)
			) {
				ps.setLong(1, tvSeriesId);
				LOGGER.trace("Inserting API metadata for " + simplifiedTitle);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						rs.updateString(COL_IMDBID, seriesMetadata.getIMDbID());
						rs.updateString(COL_TITLE, title);
						rs.updateString(COL_API_VERSION, seriesMetadata.getApiVersion());
						rs.updateLong(COL_MODIFIED, System.currentTimeMillis());
						if (seriesMetadata.getCreatedBy() != null) {
							rs.updateString("CREATEDBY", GSON.toJson(seriesMetadata.getCreatedBy()));
						}
						if (seriesMetadata.getCredits() != null) {
							rs.updateString("CREDITS", GSON.toJson(seriesMetadata.getCredits()));
						}
						rs.updateString("ENDYEAR", seriesMetadata.getEndYear());
						if (seriesMetadata.getExternalIDs() != null) {
							rs.updateString("EXTERNALIDS", GSON.toJson(seriesMetadata.getExternalIDs()));
						}
						rs.updateString("FIRSTAIRDATE", seriesMetadata.getFirstAirDate());
						rs.updateString("HOMEPAGE", seriesMetadata.getHomepage());
						if (seriesMetadata.getImages() != null) {
							rs.updateString(COL_IMAGES, GSON.toJson(seriesMetadata.getImages()));
						}
						if (seriesMetadata.isInProduction() != null) {
							rs.updateBoolean("INPRODUCTION", seriesMetadata.isInProduction());
						}
						if (seriesMetadata.getLanguages() != null) {
							rs.updateString("LANGUAGES", GSON.toJson(seriesMetadata.getLanguages()));
						}
						rs.updateString("LASTAIRDATE", seriesMetadata.getLastAirDate());
						if (seriesMetadata.getNetworks() != null) {
							rs.updateString("NETWORKS", GSON.toJson(seriesMetadata.getNetworks()));
						}
						if (seriesMetadata.getNumberOfEpisodes() != null) {
							rs.updateDouble("NUMBEROFEPISODES", seriesMetadata.getNumberOfEpisodes());
						}
						if (seriesMetadata.getNumberOfSeasons() != null) {
							rs.updateDouble("NUMBEROFSEASONS", seriesMetadata.getNumberOfSeasons());
						}
						if (seriesMetadata.getOriginCountry() != null) {
							rs.updateString("ORIGINCOUNTRY", GSON.toJson(seriesMetadata.getOriginCountry()));
						}
						rs.updateString("ORIGINALLANGUAGE", seriesMetadata.getOriginalLanguage());
						rs.updateString("ORIGINALTITLE", seriesMetadata.getOriginalTitle());
						rs.updateString(COL_OVERVIEW, seriesMetadata.getOverview());
						if (seriesMetadata.getProductionCompanies() != null) {
							rs.updateString("PRODUCTIONCOMPANIES", GSON.toJson(seriesMetadata.getProductionCompanies()));
						}
						if (seriesMetadata.getProductionCountries() != null) {
							rs.updateString("PRODUCTIONCOUNTRIES", GSON.toJson(seriesMetadata.getProductionCountries()));
						}
						if (seriesMetadata.getSeasons() != null) {
							rs.updateString("SEASONS", GSON.toJson(seriesMetadata.getSeasons()));
						}
						rs.updateString("SERIESTYPE", seriesMetadata.getSeriesType());
						if (seriesMetadata.getSpokenLanguages() != null) {
							rs.updateString("SPOKENLANGUAGES", GSON.toJson(seriesMetadata.getSpokenLanguages()));
						}
						rs.updateString(COL_STARTYEAR, seriesMetadata.getStartYear());
						rs.updateString("STATUS", seriesMetadata.getStatus());
						rs.updateString("TAGLINE", seriesMetadata.getTagline());
						if (seriesMetadata.getTmdbId() != null) {
							rs.updateLong(COL_TMDBID, seriesMetadata.getTmdbId());
						}
						if (seriesMetadata.getTotalSeasons() != null) {
							rs.updateDouble("TOTALSEASONS", seriesMetadata.getTotalSeasons());
						}
						rs.updateString("VOTES", seriesMetadata.getVotes());
						rs.updateRow();
					} else {
						LOGGER.debug("Couldn't find \"{}\" in the database when trying to store data from our API", title);
					}
				}
			}
			MediaTableVideoMetadataActors.set(connection, null, seriesMetadata.getActors(), tvSeriesId);
			MediaTableVideoMetadataAwards.set(connection, null, seriesMetadata.getAwards(), tvSeriesId);
			MediaTableVideoMetadataCountries.set(connection, null, seriesMetadata.getCountries(), tvSeriesId);
			MediaTableVideoMetadataDirectors.set(connection, null, seriesMetadata.getDirectors(), tvSeriesId);
			MediaTableVideoMetadataGenres.set(connection, null, seriesMetadata.getGenres(), tvSeriesId);
			MediaTableVideoMetadataPosters.set(connection, null, seriesMetadata.getPoster(), tvSeriesId);
			MediaTableVideoMetadataProduction.set(connection, null, seriesMetadata.getProduction(), tvSeriesId);
			MediaTableVideoMetadataRated.set(connection, null, seriesMetadata.getRated(), tvSeriesId);
			MediaTableVideoMetadataIMDbRating.set(connection, null, seriesMetadata.getRating(), tvSeriesId);
			MediaTableVideoMetadataRatings.set(connection, null, seriesMetadata.getRatings(), tvSeriesId);
			MediaTableVideoMetadataReleased.set(connection, null, seriesMetadata.getReleased(), tvSeriesId);
			connection.commit();
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "inserting API data to TV series entry", simplifiedTitle, TABLE_NAME, e.getMessage());
		}
	}

	public static TvSeriesMetadata getTvSeriesMetadata(final Connection connection, final String title) {
		if (connection == null || title == null) {
			return null;
		}
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		boolean trace = LOGGER.isTraceEnabled();
		try {
			try (PreparedStatement selectStatement = connection.prepareStatement(SQL_GET_BY_SIMPLIFIEDTITLE)) {
				selectStatement.setString(1, simplifiedTitle);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						Long tvSeriesId = rs.getLong(COL_ID);
						TvSeriesMetadata metadata = new TvSeriesMetadata();
						metadata.setActors(MediaTableVideoMetadataActors.getActorsForTvSerie(connection, tvSeriesId));
						metadata.setApiVersion(rs.getString(COL_API_VERSION));
						metadata.setAwards(MediaTableVideoMetadataAwards.getValueForTvSerie(connection, tvSeriesId));
						metadata.setCountries(MediaTableVideoMetadataCountries.getCountriesForTvSerie(connection, tvSeriesId));
						metadata.setCreatedBy(rs.getString("CREATEDBY"));
						metadata.setCredits(rs.getString("CREDITS"));
						metadata.setDirectors(MediaTableVideoMetadataDirectors.getDirectorsForTvSerie(connection, tvSeriesId));
						metadata.setEndYear(rs.getString("ENDYEAR"));
						metadata.setExternalIDs(rs.getString("EXTERNALIDS"));
						metadata.setFirstAirDate(rs.getString("FIRSTAIRDATE"));
						metadata.setGenres(MediaTableVideoMetadataGenres.getGenresForTvSerie(connection, tvSeriesId));
						metadata.setHomepage(rs.getString("HOMEPAGE"));
						metadata.setImages(rs.getString(COL_IMAGES));
						metadata.setIMDbID(rs.getString(COL_IMDBID));
						metadata.setInProduction(rs.getBoolean("INPRODUCTION"));
						metadata.setLanguages(rs.getString("LANGUAGES"));
						metadata.setLastAirDate(rs.getString("LASTAIRDATE"));
						metadata.setNetworks(rs.getString("NETWORKS"));
						metadata.setNumberOfEpisodes(rs.getDouble("NUMBEROFEPISODES"));
						metadata.setNumberOfSeasons(rs.getDouble("NUMBEROFSEASONS"));
						metadata.setOriginalLanguage(rs.getString("ORIGINALLANGUAGE"));
						metadata.setOriginalTitle(rs.getString("ORIGINALTITLE"));
						metadata.setOriginCountry(rs.getString("ORIGINCOUNTRY"));
						metadata.setOverview(rs.getString(COL_OVERVIEW));
						metadata.setPoster(MediaTableVideoMetadataPosters.getValueForTvSerie(connection, tvSeriesId));
						metadata.setProduction(MediaTableVideoMetadataProduction.getValueForTvSerie(connection, tvSeriesId));
						metadata.setProductionCompanies(rs.getString("PRODUCTIONCOMPANIES"));
						metadata.setProductionCountries(rs.getString("PRODUCTIONCOUNTRIES"));
						metadata.setRated(MediaTableVideoMetadataRated.getValueForTvSerie(connection, tvSeriesId));
						metadata.setRating(MediaTableVideoMetadataIMDbRating.getValueForTvSerie(connection, tvSeriesId));
						metadata.setRatings(MediaTableVideoMetadataRatings.getRatingsForTvSerie(connection, tvSeriesId));
						metadata.setReleased(MediaTableVideoMetadataReleased.getValueForTvSerie(connection, tvSeriesId));
						metadata.setSeasons(rs.getString("SEASONS"));
						metadata.setSeriesType(rs.getString("SERIESTYPE"));
						metadata.setSimplifiedTitle(rs.getString("SIMPLIFIEDTITLE"));
						metadata.setSpokenLanguages(rs.getString("SPOKENLANGUAGES"));
						metadata.setStartYear(rs.getString("STARTYEAR"));
						metadata.setStatus(rs.getString("STATUS"));
						metadata.setTagline(rs.getString("TAGLINE"));
						metadata.setTitle(rs.getString(COL_TITLE));
						metadata.setTmdbId(rs.getLong(COL_TMDBID));
						metadata.setTotalSeasons(rs.getDouble("TOTALSEASONS"));
						metadata.setVotes(rs.getString("VOTES"));
						return metadata;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error("Database error in " + TABLE_NAME + " for \"{}\": {}", title, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Get TV series title by IMDb ID.
	 * If we have the latest version number from the
	 * API, narrow the result to that version.
	 * @param connection the db connection
	 * @param imdbID
	 * @return the title or null
	 */
	public static String getTitleByIMDbID(final Connection connection, final String imdbID) {
		String sql;
		String latestVersion = null;
		if (CONFIGURATION.getExternalNetwork()) {
			latestVersion = APIUtils.getApiDataSeriesVersion();
		}
		if (latestVersion != null) {
			sql = SQL_GET_TITLE_BY_IMDBID_API_VERSION;
		} else {
			sql = SQL_GET_TITLE_BY_IMDBID;
		}

		try {
			try (PreparedStatement statement = connection.prepareStatement(sql)) {
				statement.setString(1, imdbID);
				if (latestVersion != null) {
					statement.setString(2, latestVersion);
				}
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getString(COL_TITLE);
					} else {
						LOGGER.trace("Did not find title by IMDb ID using query: {}", statement.toString());
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
	 * Returns the images based on title.
	 *
	 * @param connection the db connection
	 * @param title
	 * @return
	 */
	public static String getImagesByTitle(final Connection connection, final String title) {
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_IMAGES_BY_SIMPLIFIEDTITLE)) {
				statement.setString(1, simplifiedTitle);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getString(COL_IMAGES);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading images from title", title, TABLE_NAME, e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Returns a row id based on title.
	 *
	 * @param connection the db connection
	 * @param title
	 * @return
	 */
	public static Long getIdByTitle(final Connection connection, final String title) {
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ID_BY_SIMPLIFIEDTITLE)) {
				statement.setString(1, simplifiedTitle);
				try (ResultSet resultSet = statement.executeQuery()) {
					if (resultSet.next()) {
						return resultSet.getLong(COL_ID);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_VAR_IN, DATABASE_NAME, "reading id from title", title, TABLE_NAME, e.getMessage());
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
		String simplifiedTitle = FileUtil.getSimplifiedShowName(title);
		Integer thumbnailId = null;
		Integer tvSeriesId = null;

		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_THUMBNAIL_BY_SIMPLIFIEDTITLE)) {
			statement.setString(1, simplifiedTitle);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					thumbnailId = resultSet.getInt(COL_THUMBID);
					tvSeriesId = resultSet.getInt(COL_ID);
					return (DLNAThumbnail) resultSet.getObject(COL_THUMBNAIL);
				}
			}
		} catch (Exception e) {
			LOGGER.debug("Cached thumbnail for TV series {} seems to be from a previous version, regenerating", title);
			LOGGER.trace("", e);

			// Regenerate the thumbnail from a stored poster if it exists
			Object[] posterInfo = MediaTableVideoMetadataPosters.getByTVSeriesName(connection, title);
			if (posterInfo == null) {
				// this should never happen, since the only way to have a TV series thumbnail is from an API poster
				LOGGER.debug("No poster URI was found locally for {}, removing API information for TV series", title);
				if (thumbnailId != null) {
					MediaTableThumbnails.removeById(connection, thumbnailId);
					unsetApiIdsForId(connection, tvSeriesId);
				}
				return null;
			}

			String posterURL = (String) posterInfo[0];
			Long tvSeriesDatabaseId = (Long) posterInfo[1];
			DLNAThumbnail thumbnail = APIUtils.getThumbnailFromUri(posterURL);
			if (thumbnail != null) {
				MediaTableThumbnails.setThumbnail(connection, thumbnail, null, tvSeriesDatabaseId, true);
			}
			return thumbnail;
		}

		return null;
	}

	public static String getStartYearBySimplifiedTitle(final Connection connection, final String simplifiedTitle) {
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_STARTYEAR_BY_SIMPLIFIEDTITLE)) {
			statement.setString(1, simplifiedTitle);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					return resultSet.getString("STARTYEAR");
				}
			}
		} catch (SQLException ex) {
			LOGGER.error(
				LOG_ERROR_WHILE_IN_FOR,
				DATABASE_NAME,
				"reading",
				TABLE_NAME,
				simplifiedTitle,
				ex.getMessage()
			);
			LOGGER.trace("", ex);
		}
		return null;
	}

	public static void updateThumbnailId(final Connection connection, long id, int thumbId) {
		try {
			try (
				PreparedStatement ps = connection.prepareStatement(SQL_UPDATE_THUMBID);
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
	public static JsonObject getTvSeriesMetadataAsJsonObject(final Connection connection, final String simplifiedTitle, final String lang) {
		if (connection == null || simplifiedTitle == null) {
			return null;
		}
		boolean trace = LOGGER.isTraceEnabled();

		try {
			String sql = "SELECT * FROM " + TABLE_NAME + " WHERE " + TABLE_COL_SIMPLIFIEDTITLE + " = ? LIMIT 1";
			try (PreparedStatement selectStatement = connection.prepareStatement(sql)) {
				selectStatement.setString(1, simplifiedTitle);
				if (trace) {
					LOGGER.trace("Searching " + TABLE_NAME + " with \"{}\"", selectStatement);
				}
				try (ResultSet rs = selectStatement.executeQuery()) {
					if (rs.next()) {
						JsonObject result = new JsonObject();
						long id = rs.getLong("ID");
						String imdbID = rs.getString(COL_IMDBID);
						long tmdbId = rs.getLong(COL_TMDBID);
						result.addProperty("imdbID", imdbID);
						result.addProperty("overview", rs.getString(COL_OVERVIEW));
						result.addProperty("startYear", rs.getString("STARTYEAR"));
						result.addProperty("endYear", rs.getString("ENDYEAR"));
						result.addProperty("votes", rs.getString("VOTES"));
						result.addProperty("title", rs.getString(COL_TITLE));
						result.addProperty("totalSeasons", rs.getDouble("TOTALSEASONS"));
						result.addProperty("createdBy", rs.getString("CREATEDBY"));
						addJsonElementToJsonObjectIfExists(result, "credits", rs.getString("CREDITS"));
						addJsonElementToJsonObjectIfExists(result, "externalIDs", rs.getString("EXTERNALIDS"));
						result.addProperty("firstAirDate", rs.getString("FIRSTAIRDATE"));
						result.addProperty("homepage", rs.getString("HOMEPAGE"));
						addJsonElementToJsonObjectIfExists(result, "images", rs.getString(COL_IMAGES));
						result.addProperty("inProduction", rs.getBoolean("INPRODUCTION"));
						result.addProperty("homepage", rs.getString("HOMEPAGE"));
						addJsonElementToJsonObjectIfExists(result, "languages", rs.getString("LANGUAGES"));
						result.addProperty("lastAirDate", rs.getString("LASTAIRDATE"));
						result.addProperty("mediaType", "tv");
						addJsonElementToJsonObjectIfExists(result, "networks", rs.getString("NETWORKS"));
						result.addProperty("numberOfEpisodes", rs.getDouble("NUMBEROFEPISODES"));
						result.addProperty("numberOfSeasons", rs.getDouble("NUMBEROFSEASONS"));
						result.addProperty("originCountry", rs.getString("ORIGINCOUNTRY"));
						result.addProperty("originalLanguage", rs.getString("ORIGINALLANGUAGE"));
						result.addProperty("originalTitle", rs.getString("ORIGINALTITLE"));
						addJsonElementToJsonObjectIfExists(result, "productionCompanies", rs.getString("PRODUCTIONCOMPANIES"));
						addJsonElementToJsonObjectIfExists(result, "productionCountries", rs.getString("PRODUCTIONCOUNTRIES"));
						addJsonElementToJsonObjectIfExists(result, "seasons", rs.getString("SEASONS"));
						result.addProperty("seriesType", rs.getString("SERIESTYPE"));
						addJsonElementToJsonObjectIfExists(result, "spokenLanguages", rs.getString("SPOKENLANGUAGES"));
						result.addProperty("status", rs.getString("STATUS"));
						result.addProperty("tagline", rs.getString("TAGLINE"));
						result.addProperty("tmdbID", tmdbId);
						result.add("actors", MediaTableVideoMetadataActors.getJsonArrayForTvSerie(connection, id));
						result.addProperty("award", MediaTableVideoMetadataAwards.getValueForTvSerie(connection, id));
						result.add("countries", MediaTableVideoMetadataCountries.getJsonArrayForTvSerie(connection, id));
						result.add("directors", MediaTableVideoMetadataDirectors.getJsonArrayForTvSerie(connection, id));
						result.add("genres", MediaTableVideoMetadataGenres.getJsonArrayForTvSerie(connection, id));
						result.addProperty("poster", MediaTableVideoMetadataPosters.getValueForTvSerie(connection, id));
						result.addProperty("production", MediaTableVideoMetadataProduction.getValueForTvSerie(connection, id));
						result.addProperty("rated", MediaTableVideoMetadataRated.getValueForTvSerie(connection, id));
						result.addProperty("rating", MediaTableVideoMetadataIMDbRating.getValueForTvSerie(connection, id));
						result.add("ratings", MediaTableVideoMetadataRatings.getJsonArrayForTvSerie(connection, id));
						result.addProperty("released", MediaTableVideoMetadataReleased.getValueForTvSerie(connection, id));
						if (lang != null && !"en-us".equalsIgnoreCase(lang)) {
							VideoMetadataLocalized loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(connection, id, true, lang, imdbID, "tv", tmdbId, null, null);
							if (loc != null) {
								loc.localizeJsonObject(result);
								//store tmdbID if it was not before
								if (tmdbId == 0 && loc.getTmdbID() != null) {
									updateTmdbId(connection, id, loc.getTmdbID());
									result.remove("tmdbID");
									result.addProperty("tmdbID", loc.getTmdbID());
								}
							}
						}
						return result;
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "reading API results", TABLE_NAME, simplifiedTitle, e.getMessage());
			LOGGER.debug("", e);
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

	private static void updateTmdbId(final Connection connection, final long tvSeriesId, final long tmdbId) {
		try (Statement statement = connection.createStatement()) {
			statement.executeUpdate(
				"UPDATE " + TABLE_NAME + " SET " + COL_TMDBID + " = " + tmdbId +
				" WHERE " + COL_ID + " = " + tvSeriesId
			);
		} catch (SQLException e) {
			LOGGER.error("Failed to update TMDB ID for \"{}\" to \"{}\": {}", tvSeriesId, tmdbId, e.getMessage());
			LOGGER.trace("", e);
		}
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
		try (PreparedStatement statement = connection.prepareStatement(SQL_GET_TITLE_BY_SIMPLIFIEDTITLE)) {
			statement.setString(1, simplifiedTitle);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					String str = resultSet.getString(1);
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
	public static void unsetApiIdsForId(final Connection connection, final Integer id) {
		try {
			try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE_IMDBID_TMDBID_NULL)) {
				statement.setInt(1, id);
				int row = statement.executeUpdate();
				LOGGER.trace("Removed IMDb ID and TMDB ID from {} in " + TABLE_NAME + " for ID \"{}\"", row, id);
			}
		} catch (SQLException e) {
			LOGGER.error(LOG_ERROR_WHILE_IN_FOR, DATABASE_NAME, "removing entry", TABLE_NAME, id, e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static Boolean isFullyPlayed(final Connection connection, final String title) {
		try {
			/*
			 * If there is one file for this TV series where ISFULLYPLAYED is
			 * not true, then this series is not fully played, otherwise it is.
			 *
			 * This backwards logic is used for performance since we only have
			 * to check one row instead of all rows.
			 */
			try (PreparedStatement statement = connection.prepareStatement(SQL_GET_ISFULLYPLAYED)) {
				statement.setString(1, title);
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

}
