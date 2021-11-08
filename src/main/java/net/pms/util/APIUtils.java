/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.database.TableFailedLookups;
import net.pms.database.TableTVSeries;
import net.pms.database.TableThumbnails;
import net.pms.database.TableVideoMetadataActors;
import net.pms.database.TableVideoMetadataAwards;
import net.pms.database.TableVideoMetadataCountries;
import net.pms.database.TableVideoMetadataDirectors;
import net.pms.database.TableVideoMetadataGenres;
import net.pms.database.TableVideoMetadataIMDbRating;
import net.pms.database.TableVideoMetadataPosters;
import net.pms.database.TableVideoMetadataProduction;
import net.pms.database.TableVideoMetadataRated;
import net.pms.database.TableVideoMetadataRatings;
import net.pms.database.TableVideoMetadataReleased;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAThumbnail;
import net.pms.image.ImageFormat;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.newgui.IFrame;
import net.pms.util.OpenSubtitle.OpenSubtitlesBackgroundWorkerThreadFactory;

/**
 * This class contains utility methods for API to get the Metadata info.
 */
public class APIUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(APIUtils.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static IFrame frame = PMS.get().getFrame();
	private static final String VERBOSE_UA = "Universal Media Server " + PMS.getVersion();

	// Minimum number of threads in pool
	private static final ThreadPoolExecutor BACKGROUND_EXECUTOR = new ThreadPoolExecutor(0,
		5, // Maximum number of threads in pool
		30, // Number of seconds before an idle thread is terminated

		// The queue holding the tasks waiting to be processed
		TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(),
			new OpenSubtitlesBackgroundWorkerThreadFactory() // The ThreadFactory
	);

	private static final UriFileRetriever URI_FILE_RETRIEVER = new UriFileRetriever();
	private static Gson gson = new Gson();

	// Do not instantiate
	private APIUtils() {
	}

	/**
	 * Enhances existing metadata attached to this media by querying our API.
	 *
	 * @param file
	 * @param media
	 */
	public static void backgroundLookupAndAddMetadata(final File file, final DLNAMediaInfo media) {
		Runnable r = () -> {
			if (PMS.get().getDatabase().isAPIMetadataExists(file.getAbsolutePath(), file.lastModified())) {
				LOGGER.trace("Metadata already exists for {}", file.getName());
				return;
			}

			if (TableFailedLookups.hasLookupFailedRecently(file.getAbsolutePath())) {
				return;
			}

			frame.setSecondaryStatusLine(Messages.getString("StatusBar.GettingAPIInfoFor") + " " + file.getName());
			HashMap<?, ?> metadataFromAPI;
			try {
				String yearFromFilename            = media.getYear();
				String titleFromFilename           = media.getMovieOrShowName();
				String titleSimplifiedFromFilename = FileUtil.getSimplifiedShowName(titleFromFilename);
				String tvSeasonFromFilename        = media.getTVSeason();
				String tvEpisodeNumberFromFilename = media.getTVEpisodeNumber();
				Boolean isTVEpisodeBasedOnFilename = media.isTVEpisode();

				try {
					if (isTVEpisodeBasedOnFilename) {
						metadataFromAPI = getAPIMetadata(file, titleFromFilename, yearFromFilename, tvSeasonFromFilename, media.getTVEpisodeNumberUnpadded());
					} else {
						metadataFromAPI = getAPIMetadata(file, titleFromFilename, yearFromFilename, null, null);
					}

					if (metadataFromAPI == null || metadataFromAPI.containsKey("statusCode")) {
						LOGGER.trace("Failed lookup for " + file.getName());
						TableFailedLookups.set(file.getAbsolutePath(), (metadataFromAPI != null ? (String) metadataFromAPI.get("serverResponse") : ""));

						// File lookup failed, but before we return, attempt to enhance TV series data
						if (isTVEpisodeBasedOnFilename) {
							setTVSeriesInfo(null, titleFromFilename, yearFromFilename, titleSimplifiedFromFilename, file);
						}

						return;
					} else {
						LOGGER.trace("Found an API match for " + file.getName());
					}
				} catch (IOException ex) {
					// this likely means a transient error so don't store the failure, to allow retries
					LOGGER.debug("Likely transient error", ex);
					return;
				}

				String typeFromAPI = (String) metadataFromAPI.get("type");
				String yearFromAPI = (String) metadataFromAPI.get("year");
				boolean isTVEpisodeFromAPI = isNotBlank(typeFromAPI) && typeFromAPI.equals("episode");

				// At this point, this is the episode title if it is an episode
				String titleFromAPI = null;
				String tvEpisodeTitleFromAPI = null;
				if (isTVEpisodeFromAPI) {
					tvEpisodeTitleFromAPI = (String) metadataFromAPI.get("title");
				} else {
					titleFromAPI = (String) metadataFromAPI.get("title");
				}

				String tvSeasonFromAPI = (String) metadataFromAPI.get("season");
				String tvEpisodeNumberFromAPI = (String) metadataFromAPI.get("episode");
				if (tvEpisodeNumberFromAPI != null && tvEpisodeNumberFromAPI.length() == 1) {
					tvEpisodeNumberFromAPI = "0" + tvEpisodeNumberFromAPI;
				}
				String seriesIMDbIDFromAPI = (String) metadataFromAPI.get("seriesIMDbID");

				/**
				 * Only continue if the API returned a result that agrees with our filename.
				 * Specifically, fail early if:
				 * - the filename and API do not agree about it being a TV episode
				 * - for TV episodes, the season and episode number must exist and match, and
				 *   must have a series IMDb ID.
				 * - for movies, if we got a year from the filename, the API must agree
				 */
				if (
					(isTVEpisodeBasedOnFilename && !isTVEpisodeFromAPI) ||
					(!isTVEpisodeBasedOnFilename && isTVEpisodeFromAPI) ||
					(
						isTVEpisodeBasedOnFilename &&
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
						!isTVEpisodeBasedOnFilename &&
						(
							isNotBlank(yearFromFilename) &&
							isNotBlank(yearFromAPI) &&
							!yearFromFilename.equals(yearFromAPI)
						)
					)
				) {
					LOGGER.debug("API data was different to our parsed data, not storing it.");
					TableFailedLookups.set(file.getAbsolutePath(), "Data mismatch");

					LOGGER.trace("Filename data: " + media);
					LOGGER.trace("API data: " + metadataFromAPI);
					return;
				}

				LOGGER.trace("API data matches filename data for " + file.getName());

				// Now that we are happy with the API data, let's make some clearer variables
				boolean isTVEpisode    = isTVEpisodeBasedOnFilename;
				String title = null;
				String tvEpisodeNumber = tvEpisodeNumberFromAPI;
				String tvEpisodeTitle  = tvEpisodeTitleFromAPI;
				String tvSeason        = tvSeasonFromAPI;
				String year            = yearFromAPI;

				if (isTVEpisodeBasedOnFilename) {
					String titleFromDatabase = setTVSeriesInfo(seriesIMDbIDFromAPI, titleFromFilename, yearFromFilename, titleSimplifiedFromFilename, file);
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

				media.setMovieOrShowName(title);
				media.setSimplifiedMovieOrShowName(titleSimplified);
				media.setYear(year);

				media.setIMDbID((String) metadataFromAPI.get("imdbID"));

				// Set the poster as the thumbnail
				if (metadataFromAPI.get("poster") != null) {
					try {
						byte[] image = URI_FILE_RETRIEVER.get((String) metadataFromAPI.get("poster"));
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
					media.setTVSeason(tvSeason);
					media.setTVEpisodeNumber(tvEpisodeNumber);
					if (isNotBlank(tvEpisodeTitle)) {
						LOGGER.trace("Setting episode name from api: " + tvEpisodeTitle);
						media.setTVEpisodeName(tvEpisodeTitle);
					}

					media.setIsTVEpisode(true);
				}

				if (CONFIGURATION.getUseCache()) {
					LOGGER.trace("setting metadata for " + file.getName());
					PMS.get().getDatabase().insertVideoMetadata(file.getAbsolutePath(), file.lastModified(), media);

					if (media.getThumb() != null) {
						TableThumbnails.setThumbnail(media.getThumb(), file.getAbsolutePath(), -1);
					}

					if (metadataFromAPI.get("actors") != null) {
						TableVideoMetadataActors.set(file.getAbsolutePath(), new HashSet<Object>((ArrayList<?>) metadataFromAPI.get("actors")), -1);
					}
					TableVideoMetadataAwards.set(file.getAbsolutePath(), (String) metadataFromAPI.get("awards"), -1);
					TableVideoMetadataCountries.set(file.getAbsolutePath(), (String) metadataFromAPI.get("country"), -1);
					if (metadataFromAPI.get("directors") != null) {
						TableVideoMetadataDirectors.set(file.getAbsolutePath(), new HashSet<Object>((ArrayList<?>) metadataFromAPI.get("directors")), -1);
					}
					if (metadataFromAPI.get("rating") != null && (Double) metadataFromAPI.get("rating") != 0.0) {
						TableVideoMetadataIMDbRating.set(file.getAbsolutePath(), Double.toString((Double) metadataFromAPI.get("rating")), -1);
					}
					if (metadataFromAPI.get("genres") != null) {
						TableVideoMetadataGenres.set(file.getAbsolutePath(), new HashSet<Object>((ArrayList<?>) metadataFromAPI.get("genres")), -1);
					}
					TableVideoMetadataPosters.set(file.getAbsolutePath(), (String) metadataFromAPI.get("poster"), -1);
					TableVideoMetadataProduction.set(file.getAbsolutePath(), (String) metadataFromAPI.get("production"), -1);
					TableVideoMetadataRated.set(file.getAbsolutePath(), (String) metadataFromAPI.get("rated"), -1);
					if (metadataFromAPI.get("ratings") != null) {
						TableVideoMetadataRatings.set(file.getAbsolutePath(), new HashSet<Object>((ArrayList<?>) metadataFromAPI.get("ratings")), -1);
					}
					TableVideoMetadataReleased.set(file.getAbsolutePath(), (String) metadataFromAPI.get("released"), -1);
				}
			} catch (SQLException ex) {
				LOGGER.trace("Error in API parsing:", ex);
			} finally {
				frame.setSecondaryStatusLine(null);
			}
		};
		BACKGROUND_EXECUTOR.execute(r);
	}

	/**
	 * Performs a database lookup for the TV series, and an API lookup if it
	 * does not already exist with API data.
	 * Also writes the poster from the API to the thumbnail in the database.
	 * Also standardizes the series name across the episode records in the
	 * FILES table.
	 *
	 * @param isTVEpisodeBasedOnFilename
	 * @param seriesIMDbIDFromAPI
	 * @param titleFromFilename
	 * @param yearFromFilename
	 * @param title
	 * @return the title of the series.
	 */
	private static String setTVSeriesInfo(String seriesIMDbIDFromAPI, String titleFromFilename, String yearFromFilename, String titleSimplifiedFromFilename, File file) {
		long tvSeriesDatabaseId;
		String title;
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
			HashMap<String, Object> seriesMetadataFromDatabase;
			if (seriesIMDbIDFromAPI != null) {
				seriesMetadataFromDatabase = TableTVSeries.getByIMDbID(seriesIMDbIDFromAPI);
			} else {
				seriesMetadataFromDatabase = null;
			}

			if (seriesMetadataFromDatabase != null) {
				LOGGER.trace("TV series with API data already found in database {}", seriesMetadataFromDatabase.get("TITLE"));
				return (String) seriesMetadataFromDatabase.get("TITLE");
			} else {
				/*
				 * This either means there is no entry in the TV Series table for this series, or
				 * there is but it only contains filename info - not API yet.
				 */
				LOGGER.trace("API metadata for TV series {} ({}) does not already exist in the database", titleFromFilename, seriesIMDbIDFromAPI);

				// Start by checking if we have already failed this lookup recently
				if (TableFailedLookups.hasLookupFailedRecently(failedLookupKey)) {
					return null;
				}

				HashMap<String, Object> seriesMetadataFromAPI = getTVSeriesInfo(titleFromFilename, seriesIMDbIDFromAPI, yearFromFilename);
				if (seriesMetadataFromAPI == null || seriesMetadataFromAPI.containsKey("statusCode")) {
					if (seriesMetadataFromAPI != null && seriesMetadataFromAPI.containsKey("statusCode") && seriesMetadataFromAPI.get("statusCode") == "500") {
						LOGGER.debug("Got a 500 error while looking for TV series with title {} and IMDb API {}", titleFromFilename, seriesIMDbIDFromAPI);
					}
					LOGGER.trace("Did not find matching series for the episode in our API for {}", file.getName());
					// Return now because the API data is wrong if we have an episode but no series in the API
					return null;
				}

				title = (String) seriesMetadataFromAPI.get("title");
				if (isNotBlank(yearFromFilename)) {
					title += " (" + yearFromFilename + ")";
				}
				titleSimplified = FileUtil.getSimplifiedShowName(title);
				String typeFromAPI = (String) seriesMetadataFromAPI.get("type");
				boolean isSeriesFromAPI = isNotBlank(typeFromAPI) && typeFromAPI.equals("series");

				boolean isAPIDataValid = true;
				String validationFailedPrepend = "not storing the series API lookup result because ";
				// Only continue if the simplified titles match
				if (!titleSimplified.equalsIgnoreCase(titleSimplifiedFromFilename)) {
					isAPIDataValid = false;
					LOGGER.debug(validationFailedPrepend + "file and API TV series titles do not match. {} vs {}", titleSimplified, titleSimplifiedFromFilename);
					TableFailedLookups.set(titleSimplifiedFromFilename, "Title mismatch - expected " + titleSimplifiedFromFilename + " but got " + titleSimplified);
				} else if (!isSeriesFromAPI) {
					isAPIDataValid = false;
					LOGGER.debug(validationFailedPrepend + "we received a non-series from API");
					TableFailedLookups.set(titleSimplifiedFromFilename, "Type mismatch - expected series but got " + typeFromAPI);
				}

				if (!isAPIDataValid) {
					return null;
				}

				/*
				 * Now we have an API result for the TV series, we need to see whether
				 * to insert it or update existing data, so we attempt to find an entry
				 * based on the title.
				 */
				seriesMetadataFromDatabase = TableTVSeries.getByTitle(title);

				// Restore the year appended to the title if it is in the filename
				int yearIndex = indexOf(Pattern.compile("\\s\\((?:19|20)\\d{2}\\)"), (String) seriesMetadataFromAPI.get("title"));
				if (isNotBlank(yearFromFilename) && yearIndex == -1) {
					String titleFromAPI = seriesMetadataFromAPI.get("title") + " (" + yearFromFilename + ")";
					seriesMetadataFromAPI.replace("title", titleFromAPI);
				}

				if (seriesMetadataFromDatabase == null) {
					LOGGER.trace("No title match, so let's make a new entry for {}", seriesMetadataFromAPI.get("title"));
					tvSeriesDatabaseId = TableTVSeries.set(seriesMetadataFromAPI, null);
				} else {
					LOGGER.trace("There is an existing entry, so let's fill it in with API data for {}", seriesMetadataFromDatabase.get("TITLE"));
					tvSeriesDatabaseId = (long) seriesMetadataFromDatabase.get("ID");
					TableTVSeries.insertAPIMetadata(seriesMetadataFromAPI);
				}

				if (tvSeriesDatabaseId == -1) {
					LOGGER.debug("tvSeriesDatabaseId was not set, something went wrong");
					return null;
				}

				// Now we insert the TV series data into the other tables
				HashSet<?> actorsFromAPI = new HashSet<Object>((ArrayList<?>) seriesMetadataFromAPI.get("actors"));
				if (!actorsFromAPI.isEmpty()) {
					TableVideoMetadataActors.set("", actorsFromAPI, tvSeriesDatabaseId);
				}
				TableVideoMetadataAwards.set("", (String) seriesMetadataFromAPI.get("awards"), tvSeriesDatabaseId);
				TableVideoMetadataCountries.set("", (String) seriesMetadataFromAPI.get("country"), tvSeriesDatabaseId);
				HashSet<?> directorsFromAPI = new HashSet<Object>((ArrayList<?>) seriesMetadataFromAPI.get("directors"));
				if (!directorsFromAPI.isEmpty()) {
					TableVideoMetadataDirectors.set("", directorsFromAPI, tvSeriesDatabaseId);
				}
				HashSet<?> genresFromAPI = new HashSet<Object>((ArrayList<?>) seriesMetadataFromAPI.get("genres"));
				if (!genresFromAPI.isEmpty()) {
					TableVideoMetadataGenres.set("", genresFromAPI, tvSeriesDatabaseId);
				}
				TableVideoMetadataProduction.set("", (String) seriesMetadataFromAPI.get("production"), tvSeriesDatabaseId);

				// Set the poster as the thumbnail
				if (seriesMetadataFromAPI.get("poster") != null) {
					try {
						byte[] image = URI_FILE_RETRIEVER.get((String) seriesMetadataFromAPI.get("poster"));
						TableThumbnails.setThumbnail(DLNAThumbnail.toThumbnail(image, 640, 480, ScaleType.MAX, ImageFormat.JPEG, false), null, tvSeriesDatabaseId);
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

				TableVideoMetadataPosters.set("", (String) seriesMetadataFromAPI.get("poster"), tvSeriesDatabaseId);
				TableVideoMetadataRated.set("", (String) seriesMetadataFromAPI.get("rated"), tvSeriesDatabaseId);
				if (seriesMetadataFromAPI.get("rating") != null && (Double) seriesMetadataFromAPI.get("rating") != 0.0) {
					TableVideoMetadataIMDbRating.set("", Double.toString((Double) seriesMetadataFromAPI.get("rating")), tvSeriesDatabaseId);
				}
				HashSet<?> ratingsFromAPI = new HashSet<Object>((ArrayList<?>) seriesMetadataFromAPI.get("ratings"));
				if (!ratingsFromAPI.isEmpty()) {
					TableVideoMetadataRatings.set("", ratingsFromAPI, tvSeriesDatabaseId);
				}
				TableVideoMetadataReleased.set("", (String) seriesMetadataFromAPI.get("released"), tvSeriesDatabaseId);

				// Replace any close-but-not-exact titles in the FILES table
				if (
					titleFromFilename != null &&
					titleSimplifiedFromFilename != null &&
					!title.equals(titleFromFilename) &&
					titleSimplified.equals(titleSimplifiedFromFilename)
				) {
					LOGGER.trace("Converting rows in FILES table with the show name " + titleFromFilename + " to " + title);
					PMS.get().getDatabase().updateMovieOrShowName(titleFromFilename, title);
				}
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
	public static HashMap<?, ?> getAPIMetadata(File file, String movieOrTVSeriesTitle, String year, String season, String episode) throws IOException {
		Path path = null;
		String apiResult = null;

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

		String mediaType = isBlank(episode) ? "movie" : "episode";

		// Remove the year from the title before lookup if it exists
		int yearIndex = indexOf(Pattern.compile("\\s\\((?:19|20)\\d{2}\\)"), movieOrTVSeriesTitle);
		if (yearIndex > -1) {
			movieOrTVSeriesTitle = movieOrTVSeriesTitle.substring(0, yearIndex);
		}

		apiResult = getInfoFromAllExtractedData(movieOrTVSeriesTitle, false, year, season, episode, imdbID, osdbHash, filebytesize);

		String notFoundMessage = "Metadata not found on OpenSubtitles";
		if (apiResult == null || Objects.equals(notFoundMessage, apiResult)) {
			LOGGER.trace("no result for " + movieOrTVSeriesTitle + ", received: " + apiResult);
			return null;
		}

		HashMap<?, ?> data = new HashMap<Object, Object>();

		try {
			data = gson.fromJson(apiResult, data.getClass());
		} catch (JsonSyntaxException e) {
			LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, e);
		}

		if (data.isEmpty()) {
			return null;
		}

		return data;
	}

	/**
	 * Initiates a series of API lookups, from most to least desirable, until
	 * one succeeds.
	 *
	 * @param formattedName the name to use in the name search
	 * @param imdbID
	 * @param year
	 * @return The API result or null
	 * @throws IOException If an I/O error occurs during the operation.
	 */
	public static HashMap<String, Object> getTVSeriesInfo(String formattedName, String imdbID, String year) throws IOException {
		String apiResult = null;

		// Remove the year from the title if it exists
		int yearIndex = indexOf(Pattern.compile("\\s\\((?:19|20)\\d{2}\\)"), formattedName);
		if (yearIndex > -1) {
			formattedName = formattedName.substring(0, yearIndex);
		}

		apiResult = getInfoFromAllExtractedData(formattedName, true, year, null, null, imdbID, null, 0L);

		HashMap<String, Object> data = new HashMap<String, Object>();
		try {
			data = gson.fromJson(apiResult, data.getClass());
		} catch (JsonSyntaxException e) {
			LOGGER.debug("API Result was not JSON. Received: {}, full stack: {}", apiResult, e);
		}

		if (data != null && data.isEmpty()) {
			return null;
		}

		return data;
	}

	/**
	 * Attempt to return information from our API about the file based on
	 * all data we have extracted about it.
	 *
	 * @param title title or filename
	 * @param isSeries whether we are looking for a TV series (not a video itself)
	 * @param year
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
		String endpoint = isSeries ? "seriestitle" : "video";
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
			connection.setUseCaches(false);
			connection.setDefaultUseCaches(false);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Content-length", "0");
			connection.setRequestProperty("User-Agent", VERBOSE_UA);
			connection.connect();

			int status = connection.getResponseCode();
			String response;

			switch (status) {
				case 200:
				case 201:
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
					break;
				default:
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
					response = "{ statusCode: \"" + status + "\", serverResponse: " + gson.toJson(errorMessage) + " }";
			}

			return response;
		} catch (Exception e) {
			LOGGER.debug("Error while parsing JSON response: {}", e);
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
}
