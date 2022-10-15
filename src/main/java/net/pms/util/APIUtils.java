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

package net.pms.util;

import static net.pms.util.FileUtil.indexOf;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFailedLookups;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableMetadata;
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableThumbnails;
import net.pms.database.MediaTableVideoMetadataActors;
import net.pms.database.MediaTableVideoMetadataAwards;
import net.pms.database.MediaTableVideoMetadataCountries;
import net.pms.database.MediaTableVideoMetadataDirectors;
import net.pms.database.MediaTableVideoMetadataGenres;
import net.pms.database.MediaTableVideoMetadataIMDbRating;
import net.pms.database.MediaTableVideoMetadataPosters;
import net.pms.database.MediaTableVideoMetadataProduction;
import net.pms.database.MediaTableVideoMetadataRated;
import net.pms.database.MediaTableVideoMetadataRatings;
import net.pms.database.MediaTableVideoMetadataReleased;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaVideoMetadata;
import net.pms.dlna.DLNAThumbnail;
import net.pms.gui.GuiManager;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.util.OpenSubtitle.OpenSubtitlesBackgroundWorkerThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains utility methods for API to get the Metadata info.
 */
public class APIUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUtils.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String VERBOSE_UA = "Universal Media Server " + PMS.getVersion();

	/**
	 * This class is not meant to be instantiated.
	 */
	private APIUtils() {
	}

	// Minimum number of threads in pool
	private static final ThreadPoolExecutor BACKGROUND_EXECUTOR = new ThreadPoolExecutor(0,
		5, // Maximum number of threads in pool
		30, // Number of seconds before an idle thread is terminated

		// The queue holding the tasks waiting to be processed
		TimeUnit.SECONDS, new LinkedBlockingQueue<>(),
			new OpenSubtitlesBackgroundWorkerThreadFactory() // The ThreadFactory
	);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("Api Utils Executor Shutdown Hook") {
			@Override
			public void run() {
				BACKGROUND_EXECUTOR.shutdownNow();
			}
		});
	}

	private static final UriFileRetriever URI_FILE_RETRIEVER = new UriFileRetriever();
	private static final Gson GSON = new Gson();

	/**
	 * These versions are returned to us from the API server. The versions are
	 * bumped when that endpoint has received a fix that warrants the client
	 * re-fetching its results.
	 */
	private static String apiDataVideoVersion = null;
	private static String apiDataSeriesVersion = null;

	/**
	 * These versions are used to manually invalidate API data. They should be
	 * bumped when we want to re-fetch valid API data, for example if we fixed
	 * a bug that caused some data to not be stored properly.
	 * The values will be appended to the versions above on startup.
	 */
	private static final String API_DATA_VIDEO_VERSION_LOCAL = "1";
	private static final String API_DATA_SERIES_VERSION_LOCAL = "1";

	/**
	 * The base URL for all images from TMDB
	 */
	private static String apiImageBaseURL = null;

	public static String getApiDataVideoVersion() {
		if (apiDataVideoVersion == null) {
			setApiMetadataVersions();
		}

		return apiDataVideoVersion;
	}

	public static String getApiDataSeriesVersion() {
		if (apiDataSeriesVersion == null) {
			setApiMetadataVersions();
		}

		return apiDataSeriesVersion;
	}

	/**
	 * Populates the apiDataSeriesVersion and apiDataVideoVersion
	 * variables, preferably from the API, but falling back to
	 * the local database, and appended with our local values.
	 * For example:
	 * A value of "3-2" means the remote version is 3 and the local
	 * version is 2.
	 */
	public static void setApiMetadataVersions() {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			JsonObject jsonData = null;

			if (CONFIGURATION.getExternalNetwork()) {
				URL domain = new URL("https://api.universalmediaserver.com");
				URL url = new URL(domain, "/api/subversions");
				String apiResult = getJson(url);

				try {
					JsonElement element = GSON.fromJson(apiResult, JsonElement.class);
					if (element.isJsonObject()) {
						jsonData = element.getAsJsonObject();
					}
				} catch (JsonSyntaxException e) {
					LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, e);
				}
			}

			if (jsonData == null || !jsonData.has("series") || !jsonData.has("video") || jsonData.has("statusCode")) {
				if (jsonData != null && jsonData.has("statusCode") && "500".equals(jsonData.get("statusCode").getAsString())) {
					LOGGER.debug("Got a 500 error while looking for metadata subversions");
				}
				LOGGER.trace("Did not get metadata subversions, will attempt to use the database version");
				if (connection != null) {
					apiDataSeriesVersion = MediaTableMetadata.getMetadataValue(connection, "SERIES_VERSION") + "-" + API_DATA_SERIES_VERSION_LOCAL;
					apiDataVideoVersion = MediaTableMetadata.getMetadataValue(connection, "VIDEO_VERSION") + "-" + API_DATA_VIDEO_VERSION_LOCAL;
				}
				if (apiDataSeriesVersion == null) {
					LOGGER.trace("API versions could not be fetched from the API or the local database");
				}
				return;
			}

			apiDataSeriesVersion = jsonData.get("series").getAsString();
			apiDataVideoVersion = jsonData.get("video").getAsString();

			// Persist the values to the database to be used as fallbacks
			if (connection != null && apiDataSeriesVersion != null) {
				MediaTableMetadata.setOrUpdateMetadataValue(connection, "SERIES_VERSION", apiDataSeriesVersion);
				MediaTableMetadata.setOrUpdateMetadataValue(connection, "VIDEO_VERSION", apiDataVideoVersion);
			}

			apiDataSeriesVersion += "-" + API_DATA_SERIES_VERSION_LOCAL;
			apiDataVideoVersion += "-" + API_DATA_VIDEO_VERSION_LOCAL;
		} catch (IOException e) {
			LOGGER.trace("Error while setting API metadata versions", e);
		} finally {
			MediaDatabase.close(connection);
		}
	}

	public static String getApiImageBaseURL() {
		if (apiImageBaseURL == null) {
			setApiImageBaseURL();
		}

		return apiImageBaseURL;
	}

	/**
	 * Populates the apiImageBaseURL variable, preferably from the API,
	 * but falling back to the local database.
	 */
	public static void setApiImageBaseURL() {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			JsonObject jsonData = null;

			if (CONFIGURATION.getExternalNetwork()) {
				URL domain = new URL("https://api.universalmediaserver.com");
				URL url = new URL(domain, "/api/configuration");
				String apiResult = getJson(url);

				try {
					JsonElement element = GSON.fromJson(apiResult, JsonElement.class);
					if (element.isJsonObject()) {
						jsonData = element.getAsJsonObject();
					}
				} catch (JsonSyntaxException e) {
					LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, e);
				}
			}

			if (jsonData == null || !jsonData.has("imageBaseURL") || jsonData.has("statusCode")) {
				if (jsonData != null && jsonData.has("statusCode") && "500".equals(jsonData.get("statusCode").getAsString())) {
					LOGGER.debug("Got a 500 error while looking for imageBaseURL");
				}
				LOGGER.trace("Did not get imageBaseURL, will attempt to use the database version");
				if (connection != null) {
					apiImageBaseURL = MediaTableMetadata.getMetadataValue(connection, "IMAGE_BASE_URL");
				}
				if (apiImageBaseURL == null) {
					LOGGER.trace("imageBaseURL could not be fetched from the API or the local database");
				}
				return;
			}

			apiImageBaseURL = jsonData.get("imageBaseURL").getAsString();

			// Persist the values to the database to be used as fallbacks
			if (connection != null && apiImageBaseURL != null) {
				MediaTableMetadata.setOrUpdateMetadataValue(connection, "IMAGE_BASE_URL", apiImageBaseURL);
			}
		} catch (IOException e) {
			LOGGER.trace("Error while setting imageBaseURL", e);
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Enhances existing metadata attached to this media by querying our API.
	 *
	 * @param file
	 * @param media
	 */
	public static void backgroundLookupAndAddMetadata(final File file, final DLNAMediaInfo media) {
		Runnable r = () -> {
			// wait until the realtime lock is released before starting
			PMS.REALTIME_LOCK.lock();
			PMS.REALTIME_LOCK.unlock();

			if (!CONFIGURATION.getExternalNetwork()) {
				LOGGER.trace("Not doing background API lookup because external network is disabled");
				return;
			}

			if (!CONFIGURATION.isUseInfoFromIMDb()) {
				LOGGER.trace("Not doing background API lookup because isUseInfoFromIMDb is disabled");
				return;
			}

			if (!CONFIGURATION.getUseCache()) {
				LOGGER.trace("Not doing background API lookup because cache/database is disabled");
				return;
			}

			if (!MediaDatabase.isAvailable()) {
				LOGGER.trace("Database is closed");
				return;
			}

			try (Connection connection = MediaDatabase.getConnectionIfAvailable()) {
				if (connection == null) {
					return;
				}

				if (MediaTableVideoMetadata.doesLatestApiMetadataExist(connection, file.getAbsolutePath(), file.lastModified())) {
					LOGGER.trace("The latest metadata already exists for {}", file.getName());
					return;
				}

				if (MediaTableFailedLookups.hasLookupFailedRecently(connection, file.getAbsolutePath(), true)) {
					return;
				}

				GuiManager.setSecondaryStatusLine(Messages.getString("GettingApiInfoFor") + " " + file.getName());
				connection.setAutoCommit(false);
				JsonObject metadataFromAPI;

				DLNAMediaVideoMetadata videoMetadata = media.hasVideoMetadata() ? media.getVideoMetadata() : new DLNAMediaVideoMetadata();

				String year                        = videoMetadata.getYear();
				String titleFromFilename           = videoMetadata.getMovieOrShowName();
				String titleSimplifiedFromFilename = FileUtil.getSimplifiedShowName(titleFromFilename);
				String tvSeasonFromFilename        = videoMetadata.getTVSeason();
				String tvEpisodeNumberFromFilename = videoMetadata.getTVEpisodeNumber();
				String tvSeriesStartYear           = videoMetadata.getTVSeriesStartYear();

				// unset tvSeriesStartYear if it is NOT in the title because it must have come from the API earlier and will mess up the matching logic
				// todo: use better matching logic
				if (isNotBlank(tvSeriesStartYear)) {
					int yearIndex = indexOf(Pattern.compile("\\s\\(" + tvSeriesStartYear + "\\)"), titleFromFilename);
					if (yearIndex == -1) {
						tvSeriesStartYear = null;
					}
				}

				Boolean isTVEpisode = videoMetadata.isTVEpisode();

				try {
					if (isTVEpisode) {
						metadataFromAPI = getAPIMetadata(file, titleFromFilename, tvSeriesStartYear, tvSeasonFromFilename, videoMetadata.getTVEpisodeNumberUnpadded());
					} else {
						metadataFromAPI = getAPIMetadata(file, titleFromFilename, year, null, null);
					}

					if (metadataFromAPI == null || metadataFromAPI.has("statusCode")) {
						LOGGER.trace("Failed lookup for " + file.getName());
						MediaTableFailedLookups.set(connection, file.getAbsolutePath(), (metadataFromAPI != null ? metadataFromAPI.get("serverResponse").getAsString() : ""), true);

						// File lookup failed, but before we return, attempt to enhance TV series data
						if (isTVEpisode) {
							setTVSeriesInfo(connection, null, titleFromFilename, tvSeriesStartYear, titleSimplifiedFromFilename, file, media);
						}

						exitLookupAndAddMetadata(connection);
						return;
					} else {
						LOGGER.trace("Found an API match for " + file.getName());
					}
				} catch (IOException ex) {
					// this likely means a transient error so don't store the failure, to allow retries
					LOGGER.debug("Likely transient error", ex);
					exitLookupAndAddMetadata(connection);
					return;
				}

				String typeFromAPI = metadataFromAPI.has("type") ? metadataFromAPI.get("type").getAsString() : "";
				String yearFromAPI = metadataFromAPI.has("year") ? metadataFromAPI.get("year").getAsString() : "";
				boolean isTVEpisodeFromAPI = isNotBlank(typeFromAPI) && typeFromAPI.equals("episode");

				// At this point, this is the episode title if it is an episode
				String titleFromAPI = null;
				String tvEpisodeTitleFromAPI = null;
				if (isTVEpisodeFromAPI) {
					tvEpisodeTitleFromAPI = getStringOrNull(metadataFromAPI, "title");
				} else {
					titleFromAPI = getStringOrNull(metadataFromAPI, "title");
				}

				String tvSeasonFromAPI = getStringOrNull(metadataFromAPI, "season");
				String tvEpisodeNumberFromAPI = getStringOrNull(metadataFromAPI, "episode");
				if (tvEpisodeNumberFromAPI != null && tvEpisodeNumberFromAPI.length() == 1) {
					tvEpisodeNumberFromAPI = "0" + tvEpisodeNumberFromAPI;
				}
				String seriesIMDbIDFromAPI = getStringOrNull(metadataFromAPI, "seriesIMDbID");

				/**
				 * Only continue if the API returned a result that agrees with our filename.
				 * Specifically, fail early if:
				 * - the filename and API do not agree about it being a TV episode
				 * - for TV episodes, the season and episode number must exist and match, and
				 *   must have a series IMDb ID.
				 * - for movies, if we got a year from the filename, the API must agree
				 */
				if (
					(isTVEpisode && !isTVEpisodeFromAPI) ||
					(!isTVEpisode && isTVEpisodeFromAPI) ||
					(
						isTVEpisode &&
						(
							isBlank(tvSeasonFromFilename) ||
							isBlank(tvSeasonFromAPI) ||
							!tvSeasonFromFilename.equals(tvSeasonFromAPI) ||
							isBlank(tvEpisodeNumberFromFilename) ||
							isBlank(tvEpisodeNumberFromAPI) ||
							!tvEpisodeNumberFromFilename.startsWith(tvEpisodeNumberFromAPI) ||
							isBlank(seriesIMDbIDFromAPI)
						)
					) ||
					(
						!isTVEpisode &&
						(
							isNotBlank(year) &&
							isNotBlank(yearFromAPI) &&
							!year.equals(yearFromAPI)
						)
					)
				) {
					LOGGER.debug("API data was different to our parsed data, not storing it.");
					MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "Data mismatch", true);

					LOGGER.trace("Filename data: " + media);
					LOGGER.trace("API data: " + metadataFromAPI);

					// Before we return, attempt to enhance TV series data
					if (isTVEpisode) {
						setTVSeriesInfo(connection, null, titleFromFilename, tvSeriesStartYear, titleSimplifiedFromFilename, file, media);
					}

					exitLookupAndAddMetadata(connection);
					return;
				}

				LOGGER.trace("API data matches filename data for " + file.getName());

				// Now that we are happy with the API data, let's make some clearer variables
				String title = null;
				String tvEpisodeNumber = tvEpisodeNumberFromAPI;
				String tvEpisodeTitle  = tvEpisodeTitleFromAPI;
				String tvSeason        = tvSeasonFromAPI;
				year                   = yearFromAPI;

				if (isTVEpisode) {
					String titleFromDatabase = setTVSeriesInfo(connection, seriesIMDbIDFromAPI, titleFromFilename, tvSeriesStartYear, titleSimplifiedFromFilename, file, media);
					if (titleFromDatabase != null) {
						title = titleFromDatabase;
					}
				} else {
					title = isNotBlank(titleFromAPI) ? titleFromAPI : titleFromFilename;
				}

				if (isBlank(title)) {
					title = titleFromFilename;
				}
				String titleSimplified = FileUtil.getSimplifiedShowName(title);

				videoMetadata.setMovieOrShowName(title);
				videoMetadata.setSimplifiedMovieOrShowName(titleSimplified);
				videoMetadata.setYear(year);

				videoMetadata.setIMDbID(getStringOrNull(metadataFromAPI, "imdbID"));

				// Set the poster as the thumbnail
				String posterFromApi = getPosterUrlFromApiInfo(
					getStringOrNull(metadataFromAPI, "poster"),
					getStringOrNull(metadataFromAPI, "posterRelativePath")
				);
				if (posterFromApi != null) {
					try {
						byte[] image = URI_FILE_RETRIEVER.get(posterFromApi);
						media.setThumb(DLNAThumbnail.toThumbnail(image, 640, 480, ScaleType.MAX, ImageFormat.JPEG, false));
					} catch (EOFException e) {
						LOGGER.debug(
							"Error reading \"{}\" thumbnail from API: Unexpected end of stream, probably corrupt or read error.",
							file.getName()
						);
					} catch (UnknownFormatException e) {
						LOGGER.debug("Could not read \"{}\" thumbnail from API: {}", file.getName(), e.getMessage());
					} catch (IOException e) {
						LOGGER.error("Error reading \"{}\" thumbnail from API: {}", file.getName(), e.getMessage());
						LOGGER.trace("", e);
					}
				}

				// unused metadata from our api
//					media.setTagline((String) metadataFromAPI.get("tagline"));
//					media.setTrivia((String) metadataFromAPI.get("trivia"));
//					media.setVotes((String) metadataFromAPI.get("votes"));
//					media.setBoxOffice((String) metadataFromAPI.get("boxoffice"));
//					media.setGoofs((String) metadataFromAPI.get("goofs"));

				if (isTVEpisode) {
					videoMetadata.setTVSeason(tvSeason);
					videoMetadata.setTVEpisodeNumber(tvEpisodeNumber);
					if (isNotBlank(tvEpisodeTitle)) {
						LOGGER.trace("Setting episode name from api: " + tvEpisodeTitle);
						videoMetadata.setTVEpisodeName(tvEpisodeTitle);
					}

					videoMetadata.setIsTVEpisode(true);
				}
				media.setVideoMetadata(videoMetadata);

				LOGGER.trace("setting metadata for " + file.getName());
				Long fileId = MediaTableFiles.getFileId(connection, file.getAbsolutePath(), file.lastModified());
				MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, media, metadataFromAPI);

				if (media.getThumb() != null) {
					MediaTableThumbnails.setThumbnail(connection, media.getThumb(), file.getAbsolutePath(), -1, false);
				}

				if (metadataFromAPI.has("actors")) {
					//metadataFromAPI.has("poster") ? metadataFromAPI.get("poster").getAsString() : null
					MediaTableVideoMetadataActors.set(connection, fileId, metadataFromAPI.get("actors"), null);
				}
				MediaTableVideoMetadataAwards.set(connection, fileId, getStringOrNull(metadataFromAPI, "awards"), null);
				MediaTableVideoMetadataCountries.set(connection, fileId, metadataFromAPI.get("country"), null);
				if (metadataFromAPI.has("directors")) {
					MediaTableVideoMetadataDirectors.set(connection, fileId, metadataFromAPI.get("directors"), null);
				}
				if (metadataFromAPI.has("rating")  && metadataFromAPI.get("rating").isJsonPrimitive()) {
					Double rating = metadataFromAPI.get("rating").getAsDouble();
					if (rating != 0) {
						MediaTableVideoMetadataIMDbRating.set(connection, fileId, Double.toString(rating), null);
					}
				}
				if (metadataFromAPI.has("genres")) {
					MediaTableVideoMetadataGenres.set(connection, fileId, metadataFromAPI.get("genres"), null);
				}
				if (posterFromApi != null) {
					MediaTableVideoMetadataPosters.set(connection, fileId, posterFromApi, null);
				}
				MediaTableVideoMetadataProduction.set(connection, fileId, getStringOrNull(metadataFromAPI, "production"), null);
				MediaTableVideoMetadataRated.set(connection, fileId, getStringOrNull(metadataFromAPI, "rated"), null);
				if (metadataFromAPI.get("ratings") != null) {
					MediaTableVideoMetadataRatings.set(connection, fileId, metadataFromAPI.get("ratings"), null);
				}
				MediaTableVideoMetadataReleased.set(connection, fileId, getStringOrNull(metadataFromAPI, "released"), null);
				exitLookupAndAddMetadata(connection);
			} catch (SQLException ex) {
				LOGGER.trace("Error in API parsing:", ex);
			}
		};
		BACKGROUND_EXECUTOR.execute(r);
	}

	private static void exitLookupAndAddMetadata(Connection connection) {
		if (connection != null) {
			try {
				connection.commit();
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				LOGGER.error("Error in commit in APIUtils.backgroundLookupAndAdd: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}
		GuiManager.setSecondaryStatusLine(null);
	}

	/**
	 * @param jsonObject the json object to look at.
	 * @param memberName the member name to look for.
	 * @return the value or null if not exists.
	 */
	public static String getStringOrNull(JsonObject jsonObject, String memberName) {
		return jsonObject != null && jsonObject.has(memberName) && !jsonObject.get(memberName).isJsonNull() ? jsonObject.get(memberName).getAsString() : null;
	}

	/**
	 * Performs a database lookup for the TV series, and an API lookup if it
	 * does not already exist with API data.
	 * Also writes the poster from the API to the thumbnail in the database.
	 * Also standardizes the series name across the episode records in the
	 * FILES table.
	 *
	 * @param connection
	 * @param seriesIMDbIDFromAPI
	 * @param titleFromFilename
	 * @param startYear
	 * @param titleSimplifiedFromFilename
	 * @param file
	 * @param media
	 * @return the title of the series.
	 */
	private static String setTVSeriesInfo(final Connection connection, String seriesIMDbIDFromAPI, String titleFromFilename, String startYear, String titleSimplifiedFromFilename, File file, DLNAMediaInfo media) {
		String title = null;
		String titleSimplified;

		String failedLookupKey = titleSimplifiedFromFilename;
		if (seriesIMDbIDFromAPI != null) {
			failedLookupKey += seriesIMDbIDFromAPI;
		}

		/*
		 * Get the TV series title from our database, or from our API if it's not
		 * in our database yet, and persist it to our database.
		 */
		try {
			if (seriesIMDbIDFromAPI != null) {
				title = MediaTableTVSeries.getTitleByIMDbID(connection, seriesIMDbIDFromAPI);
			}

			if (title != null) {
				LOGGER.trace("TV series with API data already found in database {}", title);
				return title;
			}

			/*
			 * This either means there is no entry in the TV Series table for this series, or
			 * there is but it only contains filename info - not API yet.
			 */
			LOGGER.trace("API metadata for TV series {} (IMDb ID: {}) does not already exist in the database", titleFromFilename, seriesIMDbIDFromAPI);

			// Start by checking if we have already failed this lookup recently
			if (MediaTableFailedLookups.hasLookupFailedRecently(connection, failedLookupKey, false)) {
				return null;
			}

			JsonObject seriesMetadataFromAPI = getTVSeriesInfo(titleFromFilename, seriesIMDbIDFromAPI, startYear);
			if (seriesMetadataFromAPI == null || seriesMetadataFromAPI.has("statusCode")) {
				if (seriesMetadataFromAPI != null && seriesMetadataFromAPI.has("statusCode") && "500".equals(seriesMetadataFromAPI.get("statusCode").getAsString())) {
					LOGGER.debug("Got a 500 error while looking for TV series with title {} and IMDb API {}", titleFromFilename, seriesIMDbIDFromAPI);
				} else {
					LOGGER.trace("Did not find matching series for the episode in our API for {}", file.getName());
					MediaTableFailedLookups.set(connection, titleSimplifiedFromFilename, "No API result - expected ", false);
				}
				return null;
			}

			title = getStringOrNull(seriesMetadataFromAPI, "title");
			if (isNotBlank(startYear)) {
				title += " (" + startYear + ")";
			}
			titleSimplified = FileUtil.getSimplifiedShowName(title);
			String typeFromAPI = getStringOrNull(seriesMetadataFromAPI, "type");
			boolean isSeriesFromAPI = isNotBlank(typeFromAPI) && "series".equals(typeFromAPI);

			boolean isAPIDataValid = true;
			String validationFailedPrepend = "not storing the series API lookup result because ";
			// Only continue if the simplified titles match
			if (!titleSimplified.equalsIgnoreCase(titleSimplifiedFromFilename)) {
				isAPIDataValid = false;
				LOGGER.debug(validationFailedPrepend + "file and API TV series titles do not match. {} vs {}", titleSimplified, titleSimplifiedFromFilename);
				MediaTableFailedLookups.set(connection, titleSimplifiedFromFilename, "Title mismatch - expected " + titleSimplifiedFromFilename + " but got " + titleSimplified, false);
			} else if (!isSeriesFromAPI) {
				isAPIDataValid = false;
				LOGGER.debug(validationFailedPrepend + "we received a non-series from API");
				MediaTableFailedLookups.set(connection, titleSimplifiedFromFilename, "Type mismatch - expected series but got " + typeFromAPI, false);
			}

			if (!isAPIDataValid) {
				return null;
			}

			/*
			 * Now we have an API result for the TV series, we need to see whether
			 * to insert it or update existing data, so we attempt to find an entry
			 * based on the title.
			 */
			Long tvSeriesId = MediaTableTVSeries.getIdByTitle(connection, title);

			// Restore the startYear appended to the title if it is in the filename
			if (isNotBlank(startYear)) {
				String titleFromAPI = getStringOrNull(seriesMetadataFromAPI, "title") + " (" + startYear + ")";
				seriesMetadataFromAPI.remove("title");
				seriesMetadataFromAPI.addProperty("title", titleFromAPI);
			}

			if (tvSeriesId == null) {
				LOGGER.trace("No title match, so let's make a new entry for {}", title);
				tvSeriesId = MediaTableTVSeries.set(connection, seriesMetadataFromAPI, null);
			} else {
				LOGGER.trace("There is an existing entry, so let's fill it in with API data for {}", title);
				MediaTableTVSeries.insertAPIMetadata(connection, seriesMetadataFromAPI);
			}

			if (tvSeriesId == null) {
				LOGGER.debug("tvSeriesDatabaseId was not set, something went wrong");
				return null;
			}

			// Now we insert the TV series data into the other tables
			if (seriesMetadataFromAPI.has("actors")) {
				MediaTableVideoMetadataActors.set(connection, null, seriesMetadataFromAPI.get("actors"), tvSeriesId);
			}
			MediaTableVideoMetadataAwards.set(connection, null, getStringOrNull(seriesMetadataFromAPI, "awards"), tvSeriesId);
			MediaTableVideoMetadataCountries.set(connection, null, seriesMetadataFromAPI.get("country"), tvSeriesId);
			if (seriesMetadataFromAPI.has("directors")) {
				MediaTableVideoMetadataDirectors.set(connection, null, seriesMetadataFromAPI.get("directors"), tvSeriesId);
			}
			if (seriesMetadataFromAPI.has("genres")) {
				MediaTableVideoMetadataGenres.set(connection, null, seriesMetadataFromAPI.get("genres"), tvSeriesId);
			}
			MediaTableVideoMetadataProduction.set(connection, null, getStringOrNull(seriesMetadataFromAPI, "production"), tvSeriesId);

			String posterFromApi = getPosterUrlFromApiInfo(
				getStringOrNull(seriesMetadataFromAPI, "poster"),
				getStringOrNull(seriesMetadataFromAPI, "posterRelativePath")
			);
			if (posterFromApi != null) {
				try {
					byte[] image = URI_FILE_RETRIEVER.get(posterFromApi);
					MediaTableThumbnails.setThumbnail(connection, DLNAThumbnail.toThumbnail(image, 640, 480, ScaleType.MAX, ImageFormat.JPEG, false), null, tvSeriesId, false);
				} catch (EOFException e) {
					LOGGER.debug(
						"Error reading \"{}\" thumbnail from API: Unexpected end of stream, probably corrupt or read error.",
						file.getName()
					);
				} catch (UnknownFormatException e) {
					LOGGER.debug("Could not read \"{}\" thumbnail from API: {}", file.getName(), e.getMessage());
				} catch (IOException e) {
					LOGGER.error("Error reading \"{}\" thumbnail from API: {}", file.getName(), e.getMessage());
					LOGGER.trace("", e);
				}
				MediaTableVideoMetadataPosters.set(connection, null, posterFromApi, tvSeriesId);
			}

			MediaTableVideoMetadataRated.set(connection, null, getStringOrNull(seriesMetadataFromAPI, "rated"), tvSeriesId);
			if (seriesMetadataFromAPI.has("rating")  && seriesMetadataFromAPI.get("rating").isJsonPrimitive()) {
				Double rating = seriesMetadataFromAPI.get("rating").getAsDouble();
				if (rating != 0) {
					MediaTableVideoMetadataIMDbRating.set(connection, null, Double.toString(rating), tvSeriesId);
				}
			}
			if (seriesMetadataFromAPI.get("ratings") != null) {
				MediaTableVideoMetadataRatings.set(connection, null, seriesMetadataFromAPI.get("ratings"), tvSeriesId);
			}
			MediaTableVideoMetadataReleased.set(connection, null, getStringOrNull(seriesMetadataFromAPI, "released"), tvSeriesId);

			// Replace any close-but-not-exact titles in the FILES table
			if (
				titleFromFilename != null &&
				titleSimplifiedFromFilename != null &&
				!title.equals(titleFromFilename) &&
				titleSimplified.equals(titleSimplifiedFromFilename)
			) {
				LOGGER.trace("Converting rows in FILES table with the show name " + titleFromFilename + " to " + title);
				MediaTableVideoMetadata.updateMovieOrShowName(connection, titleFromFilename, title);
			}

			return title;
		} catch (IOException e) {
			LOGGER.error("Error getting \"{}\" TV series info from API: {}", file.getName(), e.getMessage());
			LOGGER.trace("", e);
		}

		return null;
	}

	/**
	 * Attempts to get metadata about a file from our API.
	 *
	 * @param file the {@link File} to lookup.
	 * @param movieOrTVSeriesTitle the title of the movie or TV series
	 * @param year optional year to include with title lookups
	 * @param season
	 * @param episode
	 * @return The parameter {@link String}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static JsonObject getAPIMetadata(File file, String movieOrTVSeriesTitle, String year, String season, String episode) throws IOException {
		Path path;
		String apiResult;

		String imdbID = null;
		String osdbHash = null;
		long filebytesize = 0L;

		if (file != null) {
			path = file.toPath();
			osdbHash = OpenSubtitle.getHash(path);
			if (isBlank(osdbHash)) {
				LOGGER.trace("OSDb hash was blank for " + path);
			}
			filebytesize = file.length();

			imdbID = ImdbUtil.extractImdbId(path, false);
		}

		// Remove the year from the title before lookup if it exists
		String yearRegex = "(?:19|20)\\d{2}";
		if (isNotBlank(year)) {
			yearRegex = year;
		}
		int yearIndex = indexOf(Pattern.compile("\\s\\(" + yearRegex + "\\)"), movieOrTVSeriesTitle);
		if (yearIndex > -1) {
			movieOrTVSeriesTitle = movieOrTVSeriesTitle.substring(0, yearIndex);
		}

		apiResult = getInfoFromAllExtractedData(movieOrTVSeriesTitle, false, year, season, episode, imdbID, osdbHash, filebytesize);

		String notFoundMessage = "Metadata not found on OpenSubtitles";
		if (apiResult == null || Objects.equals(notFoundMessage, apiResult)) {
			LOGGER.trace("no result for " + movieOrTVSeriesTitle + ", received: " + apiResult);
			return null;
		}

		JsonObject data = null;

		try {
			data = GSON.fromJson(apiResult, JsonObject.class);
		} catch (JsonSyntaxException e) {
			LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, e);
		}

		return data;
	}

	/**
	 * Initiates a series of API lookups, from most to least desirable, until
	 * one succeeds.
	 *
	 * @param formattedName the name to use in the name search
	 * @param imdbID
	 * @param startYear
	 * @return The API result or null
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static JsonObject getTVSeriesInfo(String formattedName, String imdbID, String startYear) throws IOException {
		String apiResult;

		// Remove the startYear from the title if it exists
		String startYearRegex = "(?:19|20)\\d{2}";
		if (isNotBlank(startYear)) {
			startYearRegex = startYear;
		}
		int startYearIndex = indexOf(Pattern.compile("\\s\\(" + startYearRegex + "\\)"), formattedName);
		if (startYearIndex > -1) {
			formattedName = formattedName.substring(0, startYearIndex);
		}

		apiResult = getInfoFromAllExtractedData(formattedName, true, startYear, null, null, imdbID, null, 0L);

		JsonObject data = null;
		try {
			data = GSON.fromJson(apiResult, JsonObject.class);
		} catch (JsonSyntaxException e) {
			LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, e);
		}

		return data;
	}

	/**
	 * Attempt to return information from our API about the file based on
	 * all data we have extracted about it.
	 *
	 * @param title title or filename
	 * @param isSeries whether we are looking for a TV series (not a video itself)
	 * @param year for movies this is the release year, for TV episodes or series this
	 *             is the year of the first episode release.
	 * @param season
	 * @param episode
	 * @param imdbID
	 * @param osdbHash
	 * @param filebytesize
	 *
	 * @return a string array including the IMDb ID, episode title, season number,
	 *         episode number relative to the season, and the show name, or null
	 *         if we couldn't find it.
	 *
	 * @throws IOException
	 */
	private static String getInfoFromAllExtractedData(
		String title,
		boolean isSeries,
		String year,
		String season,
		String episode,
		String imdbID,
		String osdbHash,
		long filebytesize
	) throws IOException {
		URL domain = new URL("https://api.universalmediaserver.com");
		String endpoint = isSeries ? "series/v2" : "video/v2";
		ArrayList<String> getParameters = new ArrayList<>();
		if (isNotBlank(title)) {
			title = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
			getParameters.add("title=" + title);
		}
		if (isNotBlank(year)) {
			getParameters.add("year=" + year);
		}
		if (isNotBlank(season)) {
			getParameters.add("season=" + season);
		}
		if (isNotBlank(episode)) {
			getParameters.add("episode=" + episode);
		}
		if (isNotBlank(imdbID)) {
			getParameters.add("imdbID=" + imdbID);
		}
		if (isNotBlank(osdbHash)) {
			getParameters.add("osdbHash=" + osdbHash);
		}
		if (filebytesize != 0L) {
			getParameters.add("filebytesize=" + filebytesize);
		}
		String getParametersJoined = StringUtils.join(getParameters, "&");
		URL url = new URL(domain, "/api/media/" + endpoint + "?" + getParametersJoined);

		LOGGER.trace("Getting API data from: {}", url);

		return getJson(url);
	}

	private static String getJson(URL url) throws IOException {
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setAllowUserInteraction(false);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-length", "0");
			connection.setRequestProperty("User-Agent", VERBOSE_UA);
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
			connection.connect();

			int status = connection.getResponseCode();
			String response;

			switch (status) {
				case 200, 201 -> {
					StringBuilder sb = new StringBuilder();
					try (
							InputStreamReader instream = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
							BufferedReader br = new BufferedReader(instream)
							) {
						String line;
						while ((line = br.readLine()) != null) {
							sb.append(line.trim()).append("\n");
						}
					} catch (Exception e) {
						LOGGER.info("API lookup error for {}, {}", connection.getURL(), e.getMessage());
					}
					LOGGER.debug("API URL was {}", connection.getURL());
					response = sb.toString().trim();
				}
				default -> {
					StringBuilder errorMessage = new StringBuilder();
					if (connection.getErrorStream() != null) {
						try (
								InputStreamReader instream = new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8);
								BufferedReader br = new BufferedReader(instream)
								) {
							String line;
							while ((line = br.readLine()) != null) {
								errorMessage.append(line.trim()).append("\n");
							}
						} catch (Exception e) {
							LOGGER.info("API lookup error for {}, {}", connection.getURL(), e.getMessage());
						}
					}

					LOGGER.debug("API status was {} for {}, {}", status, errorMessage, connection.getURL());
					response = "{ statusCode: \"" + status + "\", serverResponse: " + GSON.toJson(errorMessage) + " }";
				}
			}

			return response;
		} catch (IOException e) {
			LOGGER.debug("Error with HttpURLConnection: {}", e);
		} finally {
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception ex) {
					LOGGER.debug("Error while disconnecting connection: {}", ex);
				}
			}
		}
		return null;
	}

	/**
	 * @param posterFromApi a full URL of a poster from OMDb
	 * @param posterRelativePathFromApi this is either a "poster_path" or "still_path" from TMDB
	 * @return a full URL to a poster or meaningful screenshot
	 */
	private static String getPosterUrlFromApiInfo(String posterFromApi, String posterRelativePathFromApi) {
		if (posterRelativePathFromApi != null) {
			return getApiImageBaseURL() + "w500" + posterRelativePathFromApi;
		}

		return posterFromApi;
	}

}
