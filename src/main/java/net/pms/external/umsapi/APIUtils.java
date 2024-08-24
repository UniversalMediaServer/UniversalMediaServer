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
package net.pms.external.umsapi;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.BufferedReader;
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
import java.util.Iterator;
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
import net.pms.database.MediaTableVideoMetadata;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.gui.GuiManager;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.ApiRatingSource;
import net.pms.media.video.metadata.ApiRatingSourceArray;
import net.pms.media.video.metadata.ApiStringArray;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.media.video.metadata.VideoMetadataLocalized;
import net.pms.store.MediaInfoStore;
import net.pms.store.MediaStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.FileUtil;
import net.pms.util.ImdbUtil;
import net.pms.util.SimpleThreadFactory;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains utility methods for API to get the Metadata info.
 */
public class APIUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUtils.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String VERBOSE_UA = "Universal Media Server " + PMS.getVersion();
	private static final String API_URL = "https://api.universalmediaserver.com";

	/**
	 * This class is not meant to be instantiated.
	 */
	private APIUtils() {
	}

	// Minimum number of threads in pool
	private static final ThreadPoolExecutor BACKGROUND_EXECUTOR = new ThreadPoolExecutor(
			0,
			5, // Maximum number of threads in pool
			30, TimeUnit.SECONDS, // Number of seconds before an idle thread is terminated
			// The queue holding the tasks waiting to be processed
			new LinkedBlockingQueue<>(),
			// The ThreadFactory
			new SimpleThreadFactory("Lookup Metadata background worker", "Lookup Metadata background workers group", Thread.NORM_PRIORITY - 1)
	);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("Api Utils Executor Shutdown Hook") {
			@Override
			public void run() {
				BACKGROUND_EXECUTOR.shutdownNow();
			}
		});
	}

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
				URL url = FileUtil.urlFrom(API_URL, "/api/subversions");
				String apiResult = getJson(url);

				try {
					JsonElement element = GSON.fromJson(apiResult, JsonElement.class);
					if (element != null && element.isJsonObject()) {
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
				URL url = FileUtil.urlFrom(API_URL, "/api/configuration");
				String apiResult = getJson(url);

				try {
					JsonElement element = GSON.fromJson(apiResult, JsonElement.class);
					if (element != null && element.isJsonObject()) {
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

	private static boolean shouldLookupAndAddMetadata() {
		if (BACKGROUND_EXECUTOR.isShutdown()) {
			LOGGER.trace("Not doing background API lookup because background executor is shutdown");
			return false;
		}

		if (!CONFIGURATION.getExternalNetwork()) {
			LOGGER.trace("Not doing background API lookup because external network is disabled");
			return false;
		}

		if (!CONFIGURATION.isUseInfoFromIMDb()) {
			LOGGER.trace("Not doing background API lookup because isUseInfoFromIMDb is disabled");
			return false;
		}

		if (!MediaDatabase.isAvailable()) {
			LOGGER.trace("Not doing background API lookup because database is closed");
			return false;
		}
		return true;
	}

	/**
	 * Enhances existing seriesMetadata attached to this media by querying our API.
	 *
	 * @param file
	 * @param media
	 */
	public static void backgroundLookupAndAddMetadata(final File file, final MediaInfo media) {
		if (!shouldLookupAndAddMetadata()) {
			return;
		}
		Runnable r = () -> {
			try {
				// wait until MediaStore Workers release before starting
				MediaStore.waitWorkers();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return;
			}

			if (!shouldLookupAndAddMetadata()) {
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
					LOGGER.trace("Lookup recently failed for {}", file.getName());
					return;
				}

				GuiManager.setSecondaryStatusLine(Messages.getString("GettingApiInfoFor") + " " + file.getName());
				connection.setAutoCommit(false);
				JsonObject metadataFromAPI;

				MediaVideoMetadata videoMetadata = media.hasVideoMetadata() ? media.getVideoMetadata() : new MediaVideoMetadata();

				Integer year                       = videoMetadata.getYear();
				String titleFromFilename           = videoMetadata.getMovieOrShowName();
				String titleSimplifiedFromFilename = FileUtil.getSimplifiedShowName(titleFromFilename);
				Integer tvSeasonFromFilename       = videoMetadata.getTvSeason();
				String tvEpisodeNumberFromFilename = videoMetadata.getTvEpisodeNumber();
				Integer tvSeriesStartYear          = videoMetadata.getTvSeriesStartYear();

				// unset tvSeriesStartYear if it is NOT in the title because it must have come from the API earlier and will mess up the matching logic
				// todo: use better matching logic
				if (tvSeriesStartYear != null) {
					int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + tvSeriesStartYear + "\\)"), titleFromFilename);
					if (yearIndex == -1) {
						tvSeriesStartYear = null;
					}
				}

				boolean isTVEpisode = videoMetadata.isTvEpisode();

				try {
					if (isTVEpisode) {
						metadataFromAPI = getAPIMetadata(file, titleFromFilename, tvSeriesStartYear, tvSeasonFromFilename, videoMetadata.getTvEpisodeNumberUnpadded());
					} else {
						metadataFromAPI = getAPIMetadata(file, titleFromFilename, year, null, null);
					}

					if (metadataFromAPI == null || metadataFromAPI.has("statusCode")) {
						LOGGER.trace("Failed lookup for " + file.getName());
						MediaTableFailedLookups.set(connection, file.getAbsolutePath(), (metadataFromAPI != null ? metadataFromAPI.get("serverResponse").getAsString() : ""), true);

						// File lookup failed, but before we return, attempt to enhance TV series data
						if (isTVEpisode) {
							setTVSeriesInfo(connection, titleFromFilename, tvSeriesStartYear, titleSimplifiedFromFilename, file, videoMetadata, null, null);
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
				String titleFromAPI = getStringOrNull(metadataFromAPI, "title");

				Integer tvSeasonFromAPI = FileUtil.getIntegerFromString(getStringOrNull(metadataFromAPI, "season"));
				String tvEpisodeNumberFromAPI = getStringOrNull(metadataFromAPI, "episode");
				if (tvEpisodeNumberFromAPI != null && tvEpisodeNumberFromAPI.length() == 1) {
					tvEpisodeNumberFromAPI = "0" + tvEpisodeNumberFromAPI;
				}
				String seriesIMDbIDFromAPI = getStringOrNull(metadataFromAPI, "seriesIMDbID");
				Long tmdbTvIDFromAPI = getLongOrNull(metadataFromAPI, "tmdbTvID");

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
							tvSeasonFromFilename == null ||
							tvSeasonFromAPI == null ||
							!tvSeasonFromFilename.equals(tvSeasonFromAPI) ||
							isBlank(tvEpisodeNumberFromFilename) ||
							isBlank(tvEpisodeNumberFromAPI) ||
							!tvEpisodeNumberFromFilename.startsWith(tvEpisodeNumberFromAPI) ||
							(isBlank(seriesIMDbIDFromAPI) && tmdbTvIDFromAPI == null)
						)
					) ||
					(
						!isTVEpisode &&
						(
							year != null &&
							isNotBlank(yearFromAPI) &&
							!year.toString().equals(yearFromAPI)
						)
					)
				) {
					LOGGER.debug("API data was different to our parsed data, not storing it.");
					MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "Data mismatch", true);

					LOGGER.trace("Filename data: " + media);
					LOGGER.trace("API data: " + metadataFromAPI);

					// Before we return, attempt to enhance TV series data
					if (isTVEpisode) {
						setTVSeriesInfo(connection, titleFromFilename, tvSeriesStartYear, titleSimplifiedFromFilename, file, videoMetadata, null, null);
					}

					exitLookupAndAddMetadata(connection);
					return;
				}

				LOGGER.trace("API data matches filename data for " + file.getName());

				if (isTVEpisode) {
					Long tvSeriesId = setTVSeriesInfo(connection, titleFromFilename, tvSeriesStartYear, titleSimplifiedFromFilename, file, videoMetadata, seriesIMDbIDFromAPI, tmdbTvIDFromAPI);
					if (tvSeriesId != null && !tvSeriesId.equals(videoMetadata.getTvSeriesId())) {
						LOGGER.trace("Replacing TvSeriesId from {} to {}", videoMetadata.getTvSeriesId(), videoMetadata);
						videoMetadata.setTvSeriesId(tvSeriesId);
					}
					if (isNotBlank(titleFromAPI)) {
						LOGGER.trace("Setting episode name from api: " + titleFromAPI);
						videoMetadata.setTitle(titleFromAPI);
					}
					videoMetadata.setTvSeason(tvSeasonFromAPI);
					videoMetadata.setTvEpisodeNumber(tvEpisodeNumberFromAPI);
					videoMetadata.setIsTvEpisode(true);
				} else {
					String title = isNotBlank(titleFromAPI) ? titleFromAPI : titleFromFilename;
					videoMetadata.setTitle(title);
				}

				year = FileUtil.getYearFromYearString(yearFromAPI);
				videoMetadata.setYear(year);

				if (metadataFromAPI.has("tmdbID")) {
					videoMetadata.setTmdbId(metadataFromAPI.get("tmdbID").getAsLong());
				}
				videoMetadata.setIMDbID(getStringOrNull(metadataFromAPI, "imdbID"));
				if (videoMetadata.getIMDbID() == null && videoMetadata.getTmdbId() == null) {
					LOGGER.debug("API data does not contain IMDb/tmdb ID, storing the API data but do not retry now.");
					MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "IMDbID/tmdbId missing", true);
				}

				// Set the poster as the thumbnail
				String posterFromApi = getPosterUrlFromApiInfo(
					getStringOrNull(metadataFromAPI, "poster"),
					getStringOrNull(metadataFromAPI, "posterRelativePath")
				);
				if (posterFromApi != null) {
					videoMetadata.setPoster(posterFromApi);
					media.waitMediaParsing(5);
					media.setParsing(true);
					DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(posterFromApi);
					if (thumbnail != null) {
						Long thumbnailId = ThumbnailStore.getId(thumbnail);
						media.setThumbnailSource(ThumbnailSource.TMDB);
						media.setThumbnailId(thumbnailId);
					}
					media.setParsing(false);
				}
				if (metadataFromAPI.has("actors")) {
					videoMetadata.setActors(getApiStringArrayFromJsonElement(metadataFromAPI.get("actors")));
				}
				videoMetadata.setAwards(getStringOrNull(metadataFromAPI, "awards"));
				videoMetadata.setBudget(getLongOrNull(metadataFromAPI, "budget"));
				videoMetadata.setCountries(getCountriesFromJsonElement(metadataFromAPI.get("country")));
				if (metadataFromAPI.has("credits")) {
					videoMetadata.setCredits(metadataFromAPI.get("credits").toString());
				}
				if (metadataFromAPI.has("directors")) {
					videoMetadata.setDirectors(metadataFromAPI.get("directors").toString());
				}
				if (metadataFromAPI.has("externalIDs")) {
					videoMetadata.setExternalIDs(metadataFromAPI.get("externalIDs").toString());
				}
				if (metadataFromAPI.has("genres")) {
					videoMetadata.setGenres(metadataFromAPI.get("genres").toString());
				}
				videoMetadata.setHomepage(getStringOrNull(metadataFromAPI, "homepage"));
				if (metadataFromAPI.has("images")) {
					videoMetadata.setImages(metadataFromAPI.get("images").toString());
				}
				if (metadataFromAPI.has("originalLanguage")) {
					videoMetadata.setOriginalLanguage(metadataFromAPI.get("originalLanguage").toString());
				}
				videoMetadata.setOriginalTitle(getStringOrNull(metadataFromAPI, "originalTitle"));
				videoMetadata.setOverview(getStringOrNull(metadataFromAPI, "plot"));
				if (metadataFromAPI.has("productionCompanies")) {
					videoMetadata.setProductionCompanies(metadataFromAPI.get("productionCompanies").toString());
				}
				if (metadataFromAPI.has("productionCountries")) {
					videoMetadata.setProductionCountries(metadataFromAPI.get("productionCountries").toString());
				}
				videoMetadata.setRated(getStringOrNull(metadataFromAPI, "rated"));
				if (metadataFromAPI.has("rating")  && metadataFromAPI.get("rating").isJsonPrimitive()) {
					Double rating = metadataFromAPI.get("rating").getAsDouble();
					if (rating != 0) {
						videoMetadata.setRating(rating);
					}
				}
				if (metadataFromAPI.get("ratings") != null) {
					videoMetadata.setRatings(metadataFromAPI.get("ratings").toString());
				}
				videoMetadata.setReleased(getStringOrNull(metadataFromAPI, "released"));
				videoMetadata.setRevenue(getLongOrNull(metadataFromAPI, "revenue"));
				videoMetadata.setTagline(getStringOrNull(metadataFromAPI, "tagline"));
				videoMetadata.setVotes(getStringOrNull(metadataFromAPI, "votes"));
				videoMetadata.setApiVersion(getApiDataVideoVersion());
				media.setVideoMetadata(videoMetadata);

				LOGGER.trace("Setting database metadata for " + file.getName());
				Long fileId = MediaTableFiles.getFileId(connection, file.getAbsolutePath(), file.lastModified());
				MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, media, true);

				//let store know that we change media metadata
				MediaStoreIds.incrementUpdateIdForFilename(connection, file.getAbsolutePath());
				exitLookupAndAddMetadata(connection);
			} catch (SQLException ex) {
				LOGGER.trace("Error in API parsing:", ex);
			}
		};
		LOGGER.trace("Queuing background API lookup for {}", file.getName());
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
	 * @param jsonObject the json object to look at.
	 * @param memberName the member name to look for.
	 * @return the value or null if not exists.
	 */
	public static Long getLongOrNull(JsonObject jsonObject, String memberName) {
		return jsonObject != null && jsonObject.has(memberName) && !jsonObject.get(memberName).isJsonNull() ? jsonObject.get(memberName).getAsLong() : null;
	}

	/**
	 * Performs a database lookup for the TV series, and an API lookup if it
	 * does not already exist with API data.
	 * Also writes the poster from the API to the thumbnail in the database.
	 * Also standardizes the series name across the episode records in the
	 * FILES table.
	 *
	 * @param connection
	 * @param titleFromFilename
	 * @param startYear
	 * @param titleSimplifiedFromFilename
	 * @param file
	 * @param videoMetadata
	 * @param seriesIMDbIDFromAPI
	 * @param tmdbTvIDFromAPI
	 * @return the tvSeriesId of the series.
	 */
	private static Long setTVSeriesInfo(final Connection connection, String titleFromFilename, Integer startYear, String titleSimplifiedFromFilename, File file, MediaVideoMetadata videoMetadata, String seriesIMDbIDFromAPI, Long tmdbTvIDFromAPI) {
		/*
		 * Get the TV series metadata from our database, or from our API if it's not
		 * in our database yet, and persist it to our database.
		 */
		try {
			TvSeriesMetadata tvSeriesMetadata = null;
			if (tmdbTvIDFromAPI != null) {
				tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadataFromTmdbId(connection, tmdbTvIDFromAPI);
			}
			if (tvSeriesMetadata == null && seriesIMDbIDFromAPI != null) {
				tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadataFromImdbId(connection, seriesIMDbIDFromAPI);
			}

			if (tvSeriesMetadata != null) {
				LOGGER.trace("TV series with API data already found in database {}", tvSeriesMetadata.getTitle());
			} else {
				/*
				 * This either means there is no entry in the TV Series table for this series, or
				 * there is but it only contains filename info - not API yet.
				 */
				LOGGER.trace("API metadata for TV series {} (IMDb ID: {}, tmdbId: {}) does not already exist in the database", titleFromFilename, seriesIMDbIDFromAPI, tmdbTvIDFromAPI);

				String failedLookupKey = titleSimplifiedFromFilename;
				if (seriesIMDbIDFromAPI != null) {
					failedLookupKey += seriesIMDbIDFromAPI;
				}
				if (tmdbTvIDFromAPI != null) {
					failedLookupKey += tmdbTvIDFromAPI;
				}
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

				String title = getStringOrNull(seriesMetadataFromAPI, "title");
				if (startYear != null) {
					title += " (" + startYear + ")";
				}
				String titleSimplified = FileUtil.getSimplifiedShowName(title);
				String typeFromAPI = getStringOrNull(seriesMetadataFromAPI, "type");
				boolean isSeriesFromAPI = isNotBlank(typeFromAPI) && "series".equals(typeFromAPI);

				String validationFailedPrepend = "not storing the series API lookup result because ";
				// Only continue if the simplified titles match or we sent the IMDb ID to the API
				if (seriesIMDbIDFromAPI == null && !titleSimplified.equalsIgnoreCase(titleSimplifiedFromFilename)) {
					LOGGER.debug(validationFailedPrepend + "file and API TV series titles do not match. {} vs {}", titleSimplified, titleSimplifiedFromFilename);
					MediaTableFailedLookups.set(connection, titleSimplifiedFromFilename, "Title mismatch - expected " + titleSimplifiedFromFilename + " but got " + titleSimplified, false);
					return null;
				} else if (!isSeriesFromAPI) {
					LOGGER.debug(validationFailedPrepend + "we received a non-series from API");
					MediaTableFailedLookups.set(connection, titleSimplifiedFromFilename, "Type mismatch - expected series but got " + typeFromAPI, false);
					return null;
				}

				/*
				 * Now we have an API result for the TV series, we need to see whether
				 * to insert it or update existing data, so we attempt to find or
				 * create an entry based on the title.
				 */
				Long tvSeriesId = MediaTableTVSeries.set(connection, title, startYear);
				if (tvSeriesId == null) {
					LOGGER.debug("tvSeriesDatabaseId was not set, something went wrong");
					return null;
				}

				// Restore the startYear appended to the title if it is in the filename
				if (startYear != null) {
					String titleFromAPI = getStringOrNull(seriesMetadataFromAPI, "title") + " (" + startYear + ")";
					seriesMetadataFromAPI.remove("title");
					seriesMetadataFromAPI.addProperty("title", titleFromAPI);
				}
				String posterFromApi = getPosterUrlFromApiInfo(
					getStringOrNull(seriesMetadataFromAPI, "poster"),
					getStringOrNull(seriesMetadataFromAPI, "posterRelativePath")
				);

				//create the TvSeriesMetadata
				tvSeriesMetadata = new TvSeriesMetadata();
				tvSeriesMetadata.setActors(getApiStringArrayFromJsonElement(seriesMetadataFromAPI.get("actors")));
				tvSeriesMetadata.setApiVersion(getApiDataSeriesVersion());
				tvSeriesMetadata.setAwards(getStringOrNull(seriesMetadataFromAPI, "awards"));
				tvSeriesMetadata.setCountries(getCountriesFromJsonElement(seriesMetadataFromAPI.get("country")));
				if (seriesMetadataFromAPI.has("createdBy")) {
					tvSeriesMetadata.setCreatedBy(seriesMetadataFromAPI.get("createdBy").toString());
				}
				if (seriesMetadataFromAPI.has("credits")) {
					tvSeriesMetadata.setCredits(seriesMetadataFromAPI.get("credits").toString());
				}
				tvSeriesMetadata.setDirectors(getApiStringArrayFromJsonElement(seriesMetadataFromAPI.get("directors")));
				String endYearStr = getStringOrNull(seriesMetadataFromAPI, "endYear");
				Integer endYear = FileUtil.getYearFromYearString(endYearStr);
				tvSeriesMetadata.setEndYear(endYear);
				if (seriesMetadataFromAPI.has("externalIDs")) {
					tvSeriesMetadata.setExternalIDs(seriesMetadataFromAPI.get("externalIDs").toString());
				}
				tvSeriesMetadata.setFirstAirDate(getStringOrNull(seriesMetadataFromAPI, "firstAirDate"));
				tvSeriesMetadata.setGenres(getApiStringArrayFromJsonElement(seriesMetadataFromAPI.get("genres")));
				tvSeriesMetadata.setHomepage(getStringOrNull(seriesMetadataFromAPI, "homepage"));
				if (seriesMetadataFromAPI.has("images")) {
					tvSeriesMetadata.setImages(seriesMetadataFromAPI.get("images").toString());
				}
				tvSeriesMetadata.setIMDbID(getStringOrNull(seriesMetadataFromAPI, "imdbID"));
				if (seriesMetadataFromAPI.has("inProduction")) {
					tvSeriesMetadata.setInProduction(seriesMetadataFromAPI.get("inProduction").getAsBoolean());
				}
				tvSeriesMetadata.setLanguages(getApiStringArrayFromJsonElement(seriesMetadataFromAPI.get("languages")));
				tvSeriesMetadata.setLastAirDate(getStringOrNull(seriesMetadataFromAPI, "lastAirDate"));
				if (seriesMetadataFromAPI.has("networks")) {
					tvSeriesMetadata.setNetworks(seriesMetadataFromAPI.get("networks").toString());
				}
				if (seriesMetadataFromAPI.has("numberOfEpisodes")  && seriesMetadataFromAPI.get("numberOfEpisodes").isJsonPrimitive()) {
					tvSeriesMetadata.setNumberOfEpisodes(seriesMetadataFromAPI.get("numberOfEpisodes").getAsDouble());
				}
				if (seriesMetadataFromAPI.has("numberOfSeasons")  && seriesMetadataFromAPI.get("numberOfSeasons").isJsonPrimitive()) {
					tvSeriesMetadata.setNumberOfSeasons(seriesMetadataFromAPI.get("numberOfSeasons").getAsDouble());
				}
				tvSeriesMetadata.setOriginCountry(getApiStringArrayFromJsonElement(seriesMetadataFromAPI.get("originCountry")));
				tvSeriesMetadata.setOriginalLanguage(getStringOrNull(seriesMetadataFromAPI, "originalLanguage"));
				tvSeriesMetadata.setOriginalTitle(getStringOrNull(seriesMetadataFromAPI, "originalTitle"));

				tvSeriesMetadata.setOverview(getStringOrNull(seriesMetadataFromAPI, "plot"));
				tvSeriesMetadata.setPoster(posterFromApi);
				if (seriesMetadataFromAPI.has("productionCompanies")) {
					tvSeriesMetadata.setProductionCompanies(seriesMetadataFromAPI.get("productionCompanies").toString());
				}
				if (seriesMetadataFromAPI.has("productionCountries")) {
					tvSeriesMetadata.setProductionCountries(seriesMetadataFromAPI.get("productionCountries").toString());
				}
				tvSeriesMetadata.setRated(getStringOrNull(seriesMetadataFromAPI, "rated"));
				if (seriesMetadataFromAPI.has("rating")  && seriesMetadataFromAPI.get("rating").isJsonPrimitive()) {
					Double rating = seriesMetadataFromAPI.get("rating").getAsDouble();
					if (rating != 0) {
						tvSeriesMetadata.setRating(rating);
					}
				}
				tvSeriesMetadata.setRatings(getApiRatingSourceArrayFromJsonElement(seriesMetadataFromAPI.get("ratings")));
				if (tvSeriesMetadata.getFirstAirDate() == null) {
					tvSeriesMetadata.setFirstAirDate(getStringOrNull(seriesMetadataFromAPI, "released"));
				}
				if (seriesMetadataFromAPI.has("seasons")) {
					tvSeriesMetadata.setSeasons(seriesMetadataFromAPI.get("seasons").toString());
				}
				tvSeriesMetadata.setSeriesType(getStringOrNull(seriesMetadataFromAPI, "seriesType"));
				if (seriesMetadataFromAPI.has("spokenLanguages")) {
					tvSeriesMetadata.setSpokenLanguages(seriesMetadataFromAPI.get("spokenLanguages").toString());
				}
				String startYearStr = getStringOrNull(seriesMetadataFromAPI, "startYear");
				startYear = FileUtil.getYearFromYearString(startYearStr);
				tvSeriesMetadata.setStartYear(startYear);
				tvSeriesMetadata.setStatus(getStringOrNull(seriesMetadataFromAPI, "status"));
				tvSeriesMetadata.setTagline(getStringOrNull(seriesMetadataFromAPI, "tagline"));
				if (seriesMetadataFromAPI.has("tmdbID")) {
					tvSeriesMetadata.setTmdbId(seriesMetadataFromAPI.get("tmdbID").getAsLong());
				}
				tvSeriesMetadata.setTitle(getStringOrNull(seriesMetadataFromAPI, "title"));
				if (seriesMetadataFromAPI.has("totalSeasons")  && seriesMetadataFromAPI.get("totalSeasons").isJsonPrimitive()) {
					tvSeriesMetadata.setTotalSeasons(seriesMetadataFromAPI.get("totalSeasons").getAsDouble());
				}
				tvSeriesMetadata.setVotes(getStringOrNull(seriesMetadataFromAPI, "votes"));

				//Create/Update Thumbnail
				if (posterFromApi != null) {
					DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(posterFromApi);
					if (thumbnail != null) {
						Long thumbnailId = ThumbnailStore.getIdForTvSeries(thumbnail, tvSeriesId, ThumbnailSource.TMDB);
						tvSeriesMetadata.setThumbnailSource(ThumbnailSource.TMDB);
						tvSeriesMetadata.setThumbnailId(thumbnailId);
					}
				}

				MediaInfoStore.updateTvSeriesMetadata(tvSeriesMetadata, tvSeriesId);
				tvSeriesMetadata.setTvSeriesId(tvSeriesId);
			}
			//update MediaVideoMetadata
			if (videoMetadata != null) {
				LOGGER.trace("Setting Episode TvSeriesMetadata {}", tvSeriesMetadata.getTitle());
				if (tvSeriesMetadata.getTvSeriesId() != null) {
					videoMetadata.setSeriesMetadata(MediaInfoStore.getTvSeriesMetadata(videoMetadata.getTvSeriesId()));
					if (!tvSeriesMetadata.getTvSeriesId().equals(videoMetadata.getTvSeriesId())) {
						LOGGER.trace("Replacing Episode TvSeriesId from {} to {}", videoMetadata.getTvSeriesId(), tvSeriesMetadata.getTvSeriesId());
						videoMetadata.setTvSeriesId(tvSeriesMetadata.getTvSeriesId());
					}
				} else {
					videoMetadata.setSeriesMetadata(tvSeriesMetadata);
				}
				if (!tvSeriesMetadata.getTmdbId().equals(videoMetadata.getTmdbTvId())) {
					LOGGER.trace("Replacing Episode TmdbTvId from {} to {}", videoMetadata.getTmdbTvId(), tvSeriesMetadata.getTmdbId());
					videoMetadata.setTmdbTvId(tvSeriesMetadata.getTmdbId());
				}
			}
			return tvSeriesMetadata.getTvSeriesId();
		} catch (IOException e) {
			LOGGER.error("Error getting \"{}\" TV series info from API: {}", file.getName(), e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Attempts to get seriesMetadata about a file from our API.
	 *
	 * @param file the {@link File} to lookup.
	 * @param movieOrTVSeriesTitle the title of the movie or TV series
	 * @param year optional year to include with title lookups
	 * @param season
	 * @param episode
	 * @return The parameter {@link String}.
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static JsonObject getAPIMetadata(File file, String movieOrTVSeriesTitle, Integer year, Integer season, String episode) throws IOException {
		Path path;
		String apiResult;
		String imdbID = null;
		if (file != null) {
			path = file.toPath();
			imdbID = ImdbUtil.extractImdbId(path, false);
			if (isBlank(movieOrTVSeriesTitle)) {
				movieOrTVSeriesTitle = FileUtil.getFileNameWithoutExtension(file.getName());
			}
		}

		// Remove the year from the title before lookup if it exists
		String yearRegex = "(?:19|20)\\d{2}";
		if (year != null) {
			yearRegex = year.toString();
		}
		int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + yearRegex + "\\)"), movieOrTVSeriesTitle);
		if (yearIndex > -1) {
			movieOrTVSeriesTitle = movieOrTVSeriesTitle.substring(0, yearIndex);
		}

		apiResult = getInfoFromAllExtractedData(movieOrTVSeriesTitle, false, year, season, episode, imdbID);

		String notFoundPartialMessage = "Metadata not found";
		if (apiResult == null || StringUtils.contains(apiResult, notFoundPartialMessage)) {
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
	public static JsonObject getTVSeriesInfo(String formattedName, String imdbID, Integer startYear) throws IOException {
		String apiResult;

		// Remove the startYear from the title if it exists
		String startYearRegex = "(?:19|20)\\d{2}";
		if (startYear != null) {
			startYearRegex = startYear.toString();
		}
		int startYearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + startYearRegex + "\\)"), formattedName);
		if (startYearIndex > -1) {
			formattedName = formattedName.substring(0, startYearIndex);
		}

		apiResult = getInfoFromAllExtractedData(formattedName, true, startYear, null, null, imdbID);

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
		Integer year,
		Integer season,
		String episode,
		String imdbID
	) throws IOException {
		String endpoint = isSeries ? "series/v2" : "video/v2";
		ArrayList<String> getParameters = new ArrayList<>();
		if (isNotBlank(title)) {
			title = URLEncoder.encode(title, StandardCharsets.UTF_8.toString());
			getParameters.add("title=" + title);
		}
		if (year != null) {
			getParameters.add("year=" + year.toString());
		}
		if (season != null) {
			getParameters.add("season=" + season.toString());
		}
		if (isNotBlank(episode)) {
			getParameters.add("episode=" + episode);
		}
		if (isNotBlank(imdbID)) {
			getParameters.add("imdbID=" + imdbID);
		}
		if (!"en-US".equals(CONFIGURATION.getLanguageTag())) {
			getParameters.add("language=" + CONFIGURATION.getLanguageTag());
		}
		String getParametersJoined = StringUtils.join(getParameters, "&");
		URL url = FileUtil.urlFrom(API_URL, "/api/media/" + endpoint + "?" + getParametersJoined);

		if (isSeries) {
			LOGGER.trace("Getting API data for series from: {}", url);
		} else {
			LOGGER.trace("Getting API data for video from: {}", url);
		}

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

	/**
	 * fix country field from api is string with ',' instead of real array
	 * @return ApiStringArray
	 * FIXME : api should send a real array and field should be refactored to countries
	 */
	private static ApiStringArray getCountriesFromJsonElement(final JsonElement countries) {
		if (countries == null) {
			return null;
		}
		ApiStringArray result = new ApiStringArray();
		if (countries.isJsonArray()) {
			Iterator<JsonElement> i = countries.getAsJsonArray().iterator();
			while (i.hasNext()) {
				String country = i.next().getAsString();
				result.add(country);
			}
		} else if (countries.isJsonPrimitive()) {
			String malformedCountries = countries.getAsString();
			String[] countriesSplitted = malformedCountries.split(",");
			for (String country : countriesSplitted) {
				result.add(country.trim());
			}
		}
		return result;
	}

	private static ApiStringArray getApiStringArrayFromJsonElement(final JsonElement element) {
		if (element == null || !element.isJsonArray()) {
			return null;
		}
		ApiStringArray result = new ApiStringArray();
		Iterator<JsonElement> i = element.getAsJsonArray().iterator();
		while (i.hasNext()) {
			String country = i.next().getAsString();
			result.add(country);
		}
		return result;
	}

	private static ApiRatingSourceArray getApiRatingSourceArrayFromJsonElement(final JsonElement element) {
		if (element == null || !element.isJsonArray()) {
			return null;
		}
		ApiRatingSourceArray result = new ApiRatingSourceArray();
		Iterator<JsonElement> i = element.getAsJsonArray().iterator();
		while (i.hasNext()) {
			JsonObject rating = i.next().getAsJsonObject();
			if (rating.has("Source")) {
				String source = rating.get("Source").getAsString();
				String value = rating.has("Value") ? rating.get("Value").getAsString() : null;
				ApiRatingSource ratingSource = new ApiRatingSource();
				ratingSource.setSource(source);
				ratingSource.setValue(value);
				result.add(ratingSource);
			}
		}
		return result;
	}

	/**
	 * Attempt to return translated infos from our API about the imdbId
	 * on the language asked.
	 *
	 * @param language the asked language.
	 * @param imdbId media imdb id.
	 * @param tmdbType media tmdb type ("movie", "tv", "tv_season", "tv_episode").
	 * @param tmdbId media tmdb id.
	 * @param season media tv series season.
	 * @param episode media tv series episode.
	 * @return the VideoMetadataLocalized for the specific language.
	 */
	public static synchronized VideoMetadataLocalized getVideoMetadataLocalizedFromImdb(
		final String language,
		final String mediaType,
		final String imdbId,
		final Long tmdbId,
		final Integer season,
		final String episode
	) {
		VideoMetadataLocalized metadata = null;
		if (isNotBlank(language) && isNotBlank(mediaType) &&
			(isNotBlank(imdbId) || (tmdbId != null && tmdbId > 0)) &&
			CONFIGURATION.getExternalNetwork()
		) {
			String apiResult = null;
			try {
				ArrayList<String> getParameters = new ArrayList<>();
				getParameters.add("language=" + URLEncoder.encode(language, StandardCharsets.UTF_8.toString()));
				getParameters.add("mediaType=" + URLEncoder.encode(mediaType, StandardCharsets.UTF_8.toString()));
				if (isNotBlank(imdbId)) {
					getParameters.add("imdbID=" + URLEncoder.encode(imdbId, StandardCharsets.UTF_8.toString()));
				}
				if (tmdbId != null && tmdbId > 0) {
					getParameters.add("tmdbId=" + tmdbId);
				}
				if (season != null && isNotBlank(episode)) {
					getParameters.add("season=" + URLEncoder.encode(season.toString(), StandardCharsets.UTF_8.toString()));
					getParameters.add("episode=" + URLEncoder.encode(episode, StandardCharsets.UTF_8.toString()));
				}
				String getParametersJoined = StringUtils.join(getParameters, "&");
				URL url = FileUtil.urlFrom(API_URL, "/api/media/localize?" + getParametersJoined);
				LOGGER.trace("Getting API data from: {}", url);
				apiResult = getJson(url);
				JsonObject localizedMetadataFromAPI = GSON.fromJson(apiResult, JsonObject.class);
				if (localizedMetadataFromAPI == null || localizedMetadataFromAPI.has("statusCode")) {
					if (localizedMetadataFromAPI != null && localizedMetadataFromAPI.has("statusCode") && "500".equals(localizedMetadataFromAPI.get("statusCode").getAsString())) {
						LOGGER.debug("Got a 500 error while looking for localized metadata with language {} and url {}", language, url);
					} else {
						LOGGER.trace("Did not find matching localized metadata with language {} and url {}", language, url);
					}
					return null;
				}
				metadata = new VideoMetadataLocalized();
				if (localizedMetadataFromAPI.has("homepage") && localizedMetadataFromAPI.get("homepage").isJsonPrimitive()) {
					metadata.setHomepage(localizedMetadataFromAPI.get("homepage").getAsString());
				}
				if (localizedMetadataFromAPI.has("overview") && localizedMetadataFromAPI.get("overview").isJsonPrimitive()) {
					metadata.setOverview(localizedMetadataFromAPI.get("overview").getAsString());
				}
				if (localizedMetadataFromAPI.has("posterRelativePath") && localizedMetadataFromAPI.get("posterRelativePath").isJsonPrimitive()) {
					metadata.setPoster(getPosterUrlFromApiInfo(null, localizedMetadataFromAPI.get("posterRelativePath").getAsString()));
				}
				if (localizedMetadataFromAPI.has("tagline") && localizedMetadataFromAPI.get("tagline").isJsonPrimitive()) {
					metadata.setTagline(localizedMetadataFromAPI.get("tagline").getAsString());
				}
				if (localizedMetadataFromAPI.has("title") && localizedMetadataFromAPI.get("title").isJsonPrimitive()) {
					metadata.setTitle(localizedMetadataFromAPI.get("title").getAsString());
				}
				if (localizedMetadataFromAPI.has("tmdbID") && localizedMetadataFromAPI.get("tmdbID").isJsonPrimitive()) {
					metadata.setTmdbID(localizedMetadataFromAPI.get("tmdbID").getAsLong());
				}
			} catch (JsonSyntaxException ex) {
				LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, ex);
			} catch (IOException ex) {
				LOGGER.trace("Error while getting Localized metadata with language {}", language);
			}
		}
		return metadata;
	}

}
