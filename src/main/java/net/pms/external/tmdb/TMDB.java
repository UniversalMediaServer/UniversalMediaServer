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
package net.pms.external.tmdb;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.universalmediaserver.tmdbapi.TMDbClient;
import com.universalmediaserver.tmdbapi.endpoint.find.FindExternalSource;
import com.universalmediaserver.tmdbapi.endpoint.movie.MovieIdAppendToResponse;
import com.universalmediaserver.tmdbapi.endpoint.movie.MovieIdEndpoint;
import com.universalmediaserver.tmdbapi.endpoint.search.SearchMovieEndpoint;
import com.universalmediaserver.tmdbapi.endpoint.search.SearchTvEndpoint;
import com.universalmediaserver.tmdbapi.endpoint.tv.TvIdAppendToResponse;
import com.universalmediaserver.tmdbapi.endpoint.tv.episode.TvEpisodeAppendToResponse;
import com.universalmediaserver.tmdbapi.schema.IntegerIdNameSchema;
import com.universalmediaserver.tmdbapi.schema.collection.CollectionDetailsSchema;
import com.universalmediaserver.tmdbapi.schema.configuration.ConfigurationSchema;
import com.universalmediaserver.tmdbapi.schema.country.CountrySimpleSchema;
import com.universalmediaserver.tmdbapi.schema.find.FindSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieDetailsSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieReleaseDateSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieReleaseDatesResultSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieReleaseDatesSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieShortResultsSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieShortSchema;
import com.universalmediaserver.tmdbapi.schema.movie.MovieTypedSchema;
import com.universalmediaserver.tmdbapi.schema.person.PersonJobCreditSchema;
import com.universalmediaserver.tmdbapi.schema.person.PersonRoleCreditSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvAlternativeTitleSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvContentRatingSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvContentRatingsSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvDetailsSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvSimpleResultsSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvSimpleSchema;
import com.universalmediaserver.tmdbapi.schema.tv.TvTypedSchema;
import com.universalmediaserver.tmdbapi.schema.tv.episode.TvEpisodeDetailsSchema;
import com.universalmediaserver.tmdbapi.schema.tv.episode.TvEpisodeTypedSchema;
import com.universalmediaserver.tmdbapi.schema.tv.season.TvSeasonDetailsSchema;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
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
import net.pms.database.MediaTableTVSeries;
import net.pms.database.MediaTableVideoMetadata;
import net.pms.database.MediaTableVideoMetadataLocalized;
import net.pms.dlna.DLNAThumbnail;
import net.pms.external.JavaHttpClient;
import net.pms.external.umsapi.APIUtils;
import net.pms.gui.GuiManager;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.ApiStringArray;
import net.pms.media.video.metadata.MediaVideoMetadata;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.media.video.metadata.VideoMetadataLocalized;
import net.pms.store.MediaInfoStore;
import net.pms.store.MediaStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.FileNameMetadata;
import net.pms.util.FileUtil;
import net.pms.util.ImdbUtil;
import net.pms.util.SimpleThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains utility methods for TMDB to get the Metadata info.
 *
 * @author SurfaceS
 */
public class TMDB {

	private static final Logger LOGGER = LoggerFactory.getLogger(TMDB.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final TMDbClient CLIENT = new TMDbClient();
	private static final Gson GSON = new Gson();

	// Minimum number of threads in pool
	private static final ThreadPoolExecutor BACKGROUND_EXECUTOR = new ThreadPoolExecutor(
			0,
			5, // Maximum number of threads in pool
			30, TimeUnit.SECONDS, // Number of seconds before an idle thread is terminated
			// The queue holding the tasks waiting to be processed
			new LinkedBlockingQueue<>(),
			// The ThreadFactory
			new SimpleThreadFactory("Lookup TMDB Metadata background worker", "Lookup TMDB Metadata background workers group", Thread.NORM_PRIORITY - 1)
	);

	static {
		Runtime.getRuntime().addShutdownHook(new Thread("TMDB Utils Executor Shutdown Hook") {
			@Override
			public void run() {
				BACKGROUND_EXECUTOR.shutdownNow();
			}
		});
	}

	private static String tmdbImageBaseURL;

	/**
	 * This class is not meant to be instantiated.
	 */
	private TMDB() {
	}

	public static boolean isReady() {
		if (!CONFIGURATION.isUseInfoFromTMDB() || StringUtils.isBlank(CONFIGURATION.getTmdbApiKey())) {
			return false;
		}
		if (!CONFIGURATION.getTmdbApiKey().equals(CLIENT.getApiKey())) {
			CLIENT.setApiKey(CONFIGURATION.getTmdbApiKey());
		}
		return true;
	}

	private static boolean shouldLookupAndAddMetadata(final File file, final MediaInfo mediaInfo) {
		if (BACKGROUND_EXECUTOR.isShutdown()) {
			LOGGER.trace("Not doing background API lookup because background executor is shutdown");
			return false;
		}

		if (!CONFIGURATION.getExternalNetwork()) {
			LOGGER.trace("Not doing background TMDB lookup because external network is disabled");
			return false;
		}

		if (!MediaDatabase.isAvailable()) {
			LOGGER.trace("Not doing background TMDB lookup because database is closed");
			return false;
		}

		if (!CONFIGURATION.isUseInfoFromTMDB()) {
			LOGGER.trace("Not doing background TMDB lookup because isUseInfoFromTMDB is disabled");
			//fallback to UMS API.
			APIUtils.backgroundLookupAndAddMetadata(file, mediaInfo);
			return false;
		}

		if (!isReady()) {
			LOGGER.trace("Not doing background TMDB lookup because no/bad api key found");
			//fallback to UMS API.
			APIUtils.backgroundLookupAndAddMetadata(file, mediaInfo);
			return false;
		}
		return true;
	}

	/**
	 * Enhances existing MediaInfo attached to this media by querying TMDB.
	 *
	 * @param file
	 * @param mediaInfo MediaInfo
	 */
	public static void backgroundLookupAndAddMetadata(final File file, final MediaInfo mediaInfo) {
		if (!shouldLookupAndAddMetadata(file, mediaInfo)) {
			return;
		}
		//do not try a lookup if already queued on last 5 minutes
		long elapsed = System.currentTimeMillis() - mediaInfo.getLastExternalLookup();
		if (elapsed < 300000) {
			return;
		}
		mediaInfo.setLastExternalLookup(System.currentTimeMillis());
		Runnable r = () -> {
			try {
				// wait until MediaStore Workers release before starting
				MediaStore.waitWorkers();
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				return;
			}

			if (!shouldLookupAndAddMetadata(file, mediaInfo)) {
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
				GuiManager.setSecondaryStatusLine(Messages.getString("GettingTMDBInfoFor") + " " + file.getName());
				connection.setAutoCommit(false);
				if (mediaInfo.hasVideoMetadata() && mediaInfo.getVideoMetadata().isTvEpisode()) {
					lookupAndAddTvEpisodeMetadata(connection, file, mediaInfo);
				} else {
					lookupAndAddMovieMetadata(connection, file, mediaInfo);
				}
				exitLookupAndAddMetadata(connection);
			} catch (SQLException ex) {
				LOGGER.trace("Error in TMDB parsing:", ex);
			}
		};
		LOGGER.trace("Queuing background TMDB lookup for {}", file.getName());
		BACKGROUND_EXECUTOR.execute(r);
	}

	private static void exitLookupAndAddMetadata(Connection connection) {
		if (connection != null) {
			try {
				connection.commit();
				connection.setAutoCommit(true);
			} catch (SQLException e) {
				LOGGER.error("Error in commit in TMDB.backgroundLookupAndAddMetadata: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}
		GuiManager.setSecondaryStatusLine(null);
	}

	private static void lookupAndAddMovieMetadata(Connection connection, final File file, final MediaInfo mediaInfo) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();

		Integer year = videoMetadata.getYear();
		String titleFromFilename = videoMetadata.getTitle();
		MovieDetailsSchema movieDetails;

		try {
			String imdbID = ImdbUtil.extractImdbId(file.toPath(), false);
			if (StringUtils.isBlank(titleFromFilename)) {
				titleFromFilename = FileUtil.getFileNameWithoutExtension(file.getName());
			}
			// Remove the year from the title before lookup if it exists
			String yearRegex = year != null ? year.toString() : "(?:19|20)\\d{2}";
			int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + yearRegex + "\\)"), titleFromFilename);
			if (yearIndex > -1) {
				titleFromFilename = titleFromFilename.substring(0, yearIndex);
			}
			movieDetails = getMovieInfo(titleFromFilename, year, imdbID);

			if (movieDetails == null) {
				LOGGER.trace("Failed TMDB lookup for " + file.getName());
				MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "", true);
				return;
			} else {
				LOGGER.trace("Found a TMDB match for " + file.getName());
			}
		} catch (IOException ex) {
			// this likely means a transient error so don't store the failure, to allow retries
			LOGGER.debug("Likely transient error", ex);
			return;
		}
		LOGGER.trace("TMDB data matches filename data for " + file.getName());
		Long fileId = MediaTableFiles.getFileId(connection, file.getAbsolutePath(), file.lastModified());
		setMovieMetadata(connection, fileId, mediaInfo, movieDetails);

		//let store know that we change media metadata
		MediaStoreIds.incrementUpdateIdForFilename(connection, file.getAbsolutePath());
		//advertise queue size (only when a new real lookup is done to not flood)
		LOGGER.info("TMDB: {} background task in queue", BACKGROUND_EXECUTOR.getQueue().size());
	}

	private static void setMovieMetadata(Connection connection, final Long fileId, final MediaInfo mediaInfo, MovieDetailsSchema movieDetails) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();
		String titleFromFilename = videoMetadata.getTitle();

		// Now that we are happy with the TMDB data, let's make some clearer variables
		String title = StringUtils.isNotBlank(movieDetails.getTitle()) ? movieDetails.getTitle() : titleFromFilename;

		videoMetadata.setFileId(fileId);
		videoMetadata.setTitle(title);
		videoMetadata.setYear(FileUtil.getYearFromYearString(movieDetails.getReleaseDate()));

		videoMetadata.setIMDbID(movieDetails.getImdbId());
		videoMetadata.setTmdbId(movieDetails.getId());

		// Set the poster as the thumbnail
		String posterFromTMDB = getPosterUrl(movieDetails.getPosterPath());
		if (posterFromTMDB != null) {
			videoMetadata.setPoster(posterFromTMDB);
		}
		if (movieDetails.getCredits() != null && movieDetails.getCredits().getCast() != null) {
			videoMetadata.setActors(getActors(movieDetails.getCredits().getCast()));
		}
		videoMetadata.setBudget(movieDetails.getBudget());
		if (movieDetails.getProductionCountries() != null) {
			videoMetadata.setCountries(getCountries(movieDetails.getProductionCountries()));
		}
		if (movieDetails.getCredits() != null) {
			videoMetadata.setCredits("[" + GSON.toJson(movieDetails.getCredits()) + "]");
		}
		if (movieDetails.getCredits() != null && movieDetails.getCredits().getCrew() != null) {
			videoMetadata.setDirectors(getDirectors(movieDetails.getCredits().getCrew()));
		}
		if (movieDetails.getExternalIds() != null) {
			videoMetadata.setExternalIDs("[" + GSON.toJson(movieDetails.getExternalIds()) + "]");
		}
		videoMetadata.setGenres(getApiStringArrayFromList(movieDetails.getGenres()));
		videoMetadata.setHomepage(movieDetails.getHomepage());
		if (movieDetails.getImages() != null) {
			videoMetadata.setImages("[" + GSON.toJson(movieDetails.getImages()) + "]");
		}
		videoMetadata.setOriginalLanguage(movieDetails.getOriginalLanguage());
		videoMetadata.setOriginalTitle(movieDetails.getOriginalTitle());
		videoMetadata.setOverview(movieDetails.getOverview());
		if (movieDetails.getProductionCompanies() != null) {
			videoMetadata.setProductionCompanies(GSON.toJson(movieDetails.getProductionCompanies()));
		}
		if (movieDetails.getProductionCountries() != null) {
			videoMetadata.setProductionCountries(GSON.toJson(movieDetails.getProductionCountries()));
		}
		if (movieDetails.getReleaseDates() != null) {
			videoMetadata.setRated(getUsCertification(movieDetails.getReleaseDates()));
		}
		videoMetadata.setRating(movieDetails.getVoteAverage());
		videoMetadata.setReleased(movieDetails.getReleaseDate());
		videoMetadata.setRevenue(movieDetails.getRevenue());
		videoMetadata.setTagline(movieDetails.getTagline());
		videoMetadata.setVotes(movieDetails.getVoteCount().toString());
		//clear old localized values
		videoMetadata.setTranslations(null);
		mediaInfo.setVideoMetadata(videoMetadata);
		MediaTableVideoMetadataLocalized.clearVideoMetadataLocalized(connection, fileId, false);
		LOGGER.trace("setting movie metadata for " + title);
		MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, mediaInfo, true);

		//ensure we have the default translation
		videoMetadata.ensureHavingTranslation(null);

		//now check the thumbnail localized
		if (!StringUtils.isBlank(videoMetadata.getPoster(null))) {
			mediaInfo.waitMediaParsing(5);
			mediaInfo.setParsing(true);
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(videoMetadata.getPoster(null));
			if (thumbnail != null) {
				Long thumbnailId = ThumbnailStore.getId(thumbnail);
				mediaInfo.setThumbnailSource(ThumbnailSource.TMDB_LOC);
				mediaInfo.setThumbnailId(thumbnailId);
				MediaTableFiles.updateThumbnailId(connection, fileId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
			}
			mediaInfo.setParsing(false);
		}
	}

	private static void lookupAndAddTvEpisodeMetadata(Connection connection, final File file, final MediaInfo mediaInfo) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();
		if (videoMetadata.getTvSeriesId() == null) {
			FileNameMetadata metadataFromFilename = FileUtil.getFileNameMetadata(file.getName(), file.getAbsolutePath());
			String titleFromFilename = metadataFromFilename.getMovieOrShowName();
			Integer startYearFromFilename = metadataFromFilename.getYear();
			Long tvSeriesId = MediaTableTVSeries.getIdBySimilarTitle(connection, titleFromFilename, startYearFromFilename);
			if (tvSeriesId == null) {
				// Creates a minimal TV series row with just the title, that
				// might be enhanced later by the API
				tvSeriesId = MediaTableTVSeries.set(connection, titleFromFilename, startYearFromFilename);
			}
			videoMetadata.setTvSeriesId(tvSeriesId);
		}
		if (videoMetadata.getSeriesMetadata() == null) {
			TvSeriesMetadata tvSeriesMetadata = MediaInfoStore.getTvSeriesMetadata(videoMetadata.getTvSeriesId());
			videoMetadata.setSeriesMetadata(tvSeriesMetadata);
		}

		// first check if tv series is found
		Long tvShowId = videoMetadata.getSeriesMetadata().getTmdbId();
		if (tvShowId == null) {
			String showNameFromFilename = videoMetadata.getMovieOrShowName();
			Integer tvSeriesStartYear = videoMetadata.getTvSeriesStartYear();

			// unset tvSeriesStartYear if it is NOT in the title because it must have come from TMDB earlier and will mess up the matching logic
			// todo: use better matching logic
			if (tvSeriesStartYear != null) {
				int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + tvSeriesStartYear + "\\)"), showNameFromFilename);
				if (yearIndex == -1) {
					tvSeriesStartYear = null;
				}
			}
			String imdbId = ImdbUtil.extractImdbId(file.toPath(), false);
			if (StringUtils.isBlank(showNameFromFilename)) {
				showNameFromFilename = FileUtil.getFileNameWithoutExtension(file.getName());
			}
			// Remove the year from the title before lookup if it exists
			String yearRegex = tvSeriesStartYear != null ? tvSeriesStartYear.toString() : "(?:19|20)\\d{2}";
			int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + yearRegex + "\\)"), showNameFromFilename);
			if (yearIndex > -1) {
				showNameFromFilename = showNameFromFilename.substring(0, yearIndex);
			}

			try {
				tvShowId = MediaTableTVSeries.getTmdbIdByTitle(connection, showNameFromFilename, tvSeriesStartYear);
				if (tvShowId == null) {
					//not found in database
					String failedLookupKey = showNameFromFilename;
					if (imdbId != null) {
						LOGGER.trace("Failed lookup for " + file.getName());
						failedLookupKey += imdbId;
					}
					if (MediaTableFailedLookups.hasLookupFailedRecently(connection, failedLookupKey, false)) {
						return;
					}

					//search for a tv show
					TvDetailsSchema tvDetails = getTvShowFromEpisode(showNameFromFilename, tvSeriesStartYear, imdbId);
					if (tvDetails == null) {
						LOGGER.trace("Failed lookup for " + file.getName());
						LOGGER.trace("Did not find matching series for the episode in TMDB for {}", file.getName());
						MediaTableFailedLookups.set(connection, failedLookupKey, "tvShow not found", false);
						MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "tvShow not found", true);
						return;
					}
					tvShowId = tvDetails.getId();
					//attempt to enhance TV series data
					insertTvShowMetadata(connection, showNameFromFilename, videoMetadata, tvDetails);
				} else {
					TvSeriesMetadata tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadataFromTmdbId(connection, tvShowId);
					videoMetadata.setTvSeriesId(tvSeriesMetadata.getTvSeriesId());
					videoMetadata.setTmdbTvId(tvShowId);
					videoMetadata.setSeriesMetadata(MediaInfoStore.getTvSeriesMetadata(tvSeriesMetadata.getTvSeriesId()));
				}
			} catch (IOException ex) {
				// this likely means a transient error so don't store the failure, to allow retries
				LOGGER.debug("Likely transient error", ex);
				return;
			}
		} else if (videoMetadata.getTmdbTvId() == null) {
			videoMetadata.setTmdbTvId(tvShowId);
		}
		//now look for the episode
		Integer tvEpisodeNumberFromFilename = videoMetadata.getFirstTvEpisodeNumber();
		if (videoMetadata.getTvSeason() == null) {
			LOGGER.trace("Failed lookup for " + file.getName());
			MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "season number missing", true);
			return;
		} else if (tvEpisodeNumberFromFilename == null) {
			LOGGER.trace("Failed lookup for " + file.getName());
			MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "episodeNumber missing", true);
			return;
		}
		TvEpisodeDetailsSchema tvEpisodeDetails;
		try {
			tvEpisodeDetails = getTvEpisodeInfo(tvShowId, videoMetadata.getTvSeason(), tvEpisodeNumberFromFilename);
		} catch (IOException ex) {
			// this likely means a transient error so don't store the failure, to allow retries
			LOGGER.debug("Likely transient error", ex);
			return;
		}
		if (tvEpisodeDetails == null) {
			LOGGER.trace("Failed lookup for " + file.getName());
			MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "tvEpisode not found", true);
			return;
		} else {
			LOGGER.trace("Found an TMDB match for " + file.getName());
		}
		// At this point, this is the episode title if it is an episode
		Integer tvSeasonFromTMDB = tvEpisodeDetails.getSeasonNumber() != null ? tvEpisodeDetails.getSeasonNumber().intValue() : null;
		Integer tvEpisodeNumberFromTMDB = tvEpisodeDetails.getEpisodeNumber() != null ? tvEpisodeDetails.getEpisodeNumber().intValue() : null;

		/**
		 * Only continue if the TMDB returned a result that agrees with our
		 * filename.
		 *
		 * Specifically, fail early if:
		 *
		 * - the filename and TMDB do not agree about it being a TV episode
		 *
		 * - the season and episode number must exist and match, and must have a
		 * series TMDB ID.
		 */
		if (!videoMetadata.getTvSeason().equals(tvSeasonFromTMDB) ||
				!tvEpisodeNumberFromFilename.equals(tvEpisodeNumberFromTMDB)) {
			LOGGER.debug("TMDB data was different to our parsed data, not storing it.");
			MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "Data mismatch", true);

			LOGGER.trace("Filename data: " + mediaInfo);
			LOGGER.trace("TMDB data: " + tvEpisodeDetails);
			return;
		}
		mediaInfo.setVideoMetadata(videoMetadata);
		LOGGER.trace("TMDB data matches filename data for " + file.getName());
		Long fileId = MediaTableFiles.getFileId(connection, file.getAbsolutePath(), file.lastModified());
		setTvEpisodeMetadata(connection, fileId, mediaInfo, tvEpisodeDetails);

		//let store know that we change media metadata
		MediaStoreIds.incrementUpdateIdForFilename(connection, file.getAbsolutePath());
		//advertise queue size (only when a new real lookup is done to not flood)
		LOGGER.info("TMDB: {} background task in queue", BACKGROUND_EXECUTOR.getQueue().size());
	}

	private static void setTvEpisodeMetadata(Connection connection, final Long fileId, final MediaInfo mediaInfo, final TvEpisodeDetailsSchema tvEpisodeDetails) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();

		// At this point, this is the episode title if it is an episode
		String tvEpisodeTitleFromTMDB = tvEpisodeDetails.getName();
		Integer tvSeasonFromTMDB = tvEpisodeDetails.getSeasonNumber().intValue();
		Integer tvEpisodeNumberFromTMDB = tvEpisodeDetails.getEpisodeNumber().intValue();

		// Now that we are happy with the TMDB data, let's make some clearer variables
		if (StringUtils.isNotBlank(tvEpisodeTitleFromTMDB)) {
			LOGGER.trace("Setting episode name from TMDB: " + tvEpisodeTitleFromTMDB);
			videoMetadata.setTitle(tvEpisodeTitleFromTMDB);
		}
		videoMetadata.setYear(FileUtil.getYearFromYearString(tvEpisodeDetails.getAirDate()));

		videoMetadata.setTmdbId(tvEpisodeDetails.getId());
		videoMetadata.setIMDbID(tvEpisodeDetails.getExternalIds() != null ? tvEpisodeDetails.getExternalIds().getImdbId() : null);

		// Set the poster as the thumbnail
		String posterFromTMDB = getStillUrl(tvEpisodeDetails.getStillPath());
		if (posterFromTMDB != null) {
			videoMetadata.setPoster(posterFromTMDB);
		}
		videoMetadata.setTvSeason(tvSeasonFromTMDB);
		videoMetadata.setTvEpisodeNumber(tvEpisodeNumberFromTMDB.toString());
		if (tvEpisodeDetails.getCredits() != null && tvEpisodeDetails.getCredits().getCast() != null) {
			videoMetadata.setActors(getActors(tvEpisodeDetails.getCredits().getCast()));
		}
		if (tvEpisodeDetails.getCredits() != null) {
			videoMetadata.setCredits("[" + GSON.toJson(tvEpisodeDetails.getCredits()) + "]");
		}
		if (tvEpisodeDetails.getCredits() != null && tvEpisodeDetails.getCredits().getCrew() != null) {
			videoMetadata.setDirectors(getDirectors(tvEpisodeDetails.getCredits().getCrew()));
		}
		videoMetadata.setExternalIDs("[" + GSON.toJson(tvEpisodeDetails.getExternalIds()) + "]");
		if (tvEpisodeDetails.getImages() != null) {
			videoMetadata.setImages("[" + GSON.toJson(tvEpisodeDetails.getImages()) + "]");
		}
		videoMetadata.setOverview(tvEpisodeDetails.getOverview());
		videoMetadata.setVotes(tvEpisodeDetails.getVoteAverage().toString());
		//clear old localized values
		videoMetadata.setTranslations(null);
		mediaInfo.setVideoMetadata(videoMetadata);
		MediaTableVideoMetadataLocalized.clearVideoMetadataLocalized(connection, fileId, false);
		LOGGER.trace("setting tv episode metadata for " + videoMetadata.getTvSeriesTitle() + " " + tvSeasonFromTMDB + "-" + tvEpisodeNumberFromTMDB);
		MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, mediaInfo, true);

		//ensure we have the default translation
		videoMetadata.ensureHavingTranslation(null);

		//now check the thumbnail localized
		if (!StringUtils.isBlank(videoMetadata.getPoster(null))) {
			mediaInfo.waitMediaParsing(5);
			mediaInfo.setParsing(true);
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(videoMetadata.getPoster(null));
			if (thumbnail != null) {
				Long thumbnailId = ThumbnailStore.getId(thumbnail);
				mediaInfo.setThumbnailSource(ThumbnailSource.TMDB_LOC);
				mediaInfo.setThumbnailId(thumbnailId);
				MediaTableFiles.updateThumbnailId(connection, fileId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
			}
			mediaInfo.setParsing(false);
		}
	}

	/**
	 * Performs a database lookup for the TV series, and a TMDB lookup if it
	 * does not already exist with TMDB data.
	 *
	 * Also writes the poster from TMDB to the thumbnail in the database.
	 *
	 * Also standardizes the series name across the episode records in the FILES
	 * table.
	 *
	 * @param connection
	 * @param tvDetails
	 * @param titleFromFilename
	 * @param videoMetadata
	 *
	 * @return the db TvSeriesId.
	 */
	private static TvSeriesMetadata setTvShowMetadata(final Connection connection, TvDetailsSchema tvDetails) {
		if (tvDetails == null) {
			return null;
		}
		String title = tvDetails.getName();
		Integer startYear = getYear(tvDetails.getFirstAirDate());

		/*
		 * Now we have a TMDB result for the TV series, we need to see whether
		 * to insert it or update existing data, so we attempt to find or
		 * create an entry based on the title.
		 */
		Long tvSeriesId = MediaTableTVSeries.set(connection, title, startYear);
		if (tvSeriesId == null) {
			LOGGER.debug("tvSeriesDatabaseId was not set, something went wrong");
			return null;
		}

		//create the TvSeriesMetadata
		TvSeriesMetadata tvSeriesMetadata = new TvSeriesMetadata();
		tvSeriesMetadata.setTvSeriesId(tvSeriesId);
		if (tvDetails.getCredits() != null && tvDetails.getCredits().getCast() != null) {
			tvSeriesMetadata.setActors(getActors(tvDetails.getCredits().getCast()));
		}
		tvSeriesMetadata.setCountries(getApiStringArrayFromStringList(tvDetails.getOriginCountry()));
		tvSeriesMetadata.setCreatedBy(GSON.toJson(tvDetails.getCreatedBy()));
		if (tvDetails.getCredits() != null) {
			tvSeriesMetadata.setCredits("[" + GSON.toJson(tvDetails.getCredits()) + "]");
		}
		if (Boolean.FALSE.equals(tvDetails.getInProduction())) {
			Integer endYear = getYear(tvDetails.getLastAirDate());
			tvSeriesMetadata.setEndYear(endYear);
		}
		if (tvDetails.getExternalIds() != null) {
			tvSeriesMetadata.setExternalIDs("[" + GSON.toJson(tvDetails.getExternalIds()) + "]");
		}
		tvSeriesMetadata.setFirstAirDate(tvDetails.getFirstAirDate());
		tvSeriesMetadata.setGenres(getApiStringArrayFromList(tvDetails.getGenres()));
		tvSeriesMetadata.setHomepage(tvDetails.getHomepage());
		if (tvDetails.getImages() != null) {
			tvSeriesMetadata.setImages("[" + GSON.toJson(tvDetails.getImages()) + "]");
		}
		tvSeriesMetadata.setIMDbID(tvDetails.getExternalIds() != null ? tvDetails.getExternalIds().getImdbId() : null);
		tvSeriesMetadata.setInProduction(tvDetails.getInProduction());
		tvSeriesMetadata.setLanguages(getApiStringArrayFromStringList(tvDetails.getLanguages()));
		tvSeriesMetadata.setLastAirDate(tvDetails.getLastAirDate());
		if (tvDetails.getNetworks() != null) {
			tvSeriesMetadata.setNetworks(GSON.toJson(tvDetails.getNetworks()));
		}
		tvSeriesMetadata.setNumberOfEpisodes(tvDetails.getNumberOfEpisodes().doubleValue());
		tvSeriesMetadata.setNumberOfSeasons(tvDetails.getNumberOfSeasons().doubleValue());
		tvSeriesMetadata.setOriginCountry(getApiStringArrayFromStringList(tvDetails.getOriginCountry()));
		tvSeriesMetadata.setOriginalLanguage(tvDetails.getOriginalLanguage());
		tvSeriesMetadata.setOriginalTitle(tvDetails.getOriginalName());
		tvSeriesMetadata.setOverview(tvDetails.getOverview());
		String posterFromTMDB = getPosterUrl(tvDetails.getPosterPath());
		tvSeriesMetadata.setPoster(posterFromTMDB);
		if (tvDetails.getProductionCompanies() != null) {
			tvSeriesMetadata.setProductionCompanies(GSON.toJson(tvDetails.getProductionCompanies()));
		}
		if (tvDetails.getProductionCountries() != null) {
			tvSeriesMetadata.setProductionCountries(GSON.toJson(tvDetails.getProductionCountries()));
		}
		tvSeriesMetadata.setRated(getUsContentRating(tvDetails.getContentRatings()));
		tvSeriesMetadata.setRating(tvDetails.getVoteAverage());
		if (tvDetails.getSeasons() != null) {
			tvSeriesMetadata.setSeasons(GSON.toJson(tvDetails.getSeasons()));
		}
		tvSeriesMetadata.setSeriesType(tvDetails.getType());
		if (tvDetails.getSpokenLanguages() != null) {
			tvSeriesMetadata.setSpokenLanguages(GSON.toJson(tvDetails.getSpokenLanguages()));
		}
		tvSeriesMetadata.setStartYear(startYear);
		tvSeriesMetadata.setStatus(tvDetails.getStatus());
		tvSeriesMetadata.setTagline(tvDetails.getTagline());
		tvSeriesMetadata.setTmdbId(tvDetails.getId());
		tvSeriesMetadata.setTitle(tvDetails.getName());
		tvSeriesMetadata.setTotalSeasons(tvDetails.getNumberOfSeasons().doubleValue());
		tvSeriesMetadata.setVotes(tvDetails.getVoteCount().toString());
		//clear old localized values
		tvSeriesMetadata.setTranslations(null);
		MediaTableVideoMetadataLocalized.clearVideoMetadataLocalized(connection, tvSeriesId, true);
		MediaInfoStore.updateTvSeriesMetadata(tvSeriesMetadata, tvSeriesId);

		//ensure we have the default translation
		tvSeriesMetadata.ensureHavingTranslation(null);

		//now check the thumbnail localized
		if (!StringUtils.isBlank(tvSeriesMetadata.getPoster(null))) {
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(tvSeriesMetadata.getPoster(null));
			if (thumbnail != null) {
				Long thumbnailId = ThumbnailStore.getIdForTvSeries(thumbnail, tvSeriesId, ThumbnailSource.TMDB_LOC);
				tvSeriesMetadata.setThumbnailSource(ThumbnailSource.TMDB_LOC);
				tvSeriesMetadata.setThumbnailId(thumbnailId);
				MediaTableTVSeries.updateThumbnailId(connection, tvSeriesId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
			}
		}
		return tvSeriesMetadata;
	}

	/**
	 * Performs a database lookup for the TV series, and a TMDB lookup if it
	 * does not already exist with TMDB data.
	 *
	 * Also writes the poster from TMDB to the thumbnail in the database.
	 *
	 * Also standardizes the series name across the episode records in the FILES
	 * table.
	 *
	 * @param connection
	 * @param tvDetails
	 * @param titleFromFilename
	 * @param videoMetadata
	 *
	 * @return the db TvSeriesId.
	 */
	private static Long insertTvShowMetadata(final Connection connection, String titleFromFilename, MediaVideoMetadata videoMetadata, TvDetailsSchema tvDetails) {
		Long tvSeriesId = null;

		/*
		 * Get the TV series metadata from our database, or from TMDB if it's not
		 * in our database yet, and persist it to our database.
		 */
		TvSeriesMetadata tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadataFromTmdbId(connection, tvDetails.getId());
		if (tvSeriesMetadata != null && tvSeriesMetadata.getTvSeriesId() != null) {
			tvSeriesId = tvSeriesMetadata.getTvSeriesId();
			LOGGER.trace("TV series with TMDB data already found in database {}", tvSeriesMetadata.getTitle());
		} else {
			/*
			 * This either means there is no entry in the TV Series table for this series, or
			 * there is but it only contains filename info - not Metadata yet.
			 */
			LOGGER.trace("TMDB metadata for TV series {} (TMDB ID: {}) does not already exist in the database", titleFromFilename, tvDetails.getId());

			tvSeriesMetadata = setTvShowMetadata(connection, tvDetails);
			if (tvSeriesMetadata != null) {
				tvSeriesId = tvSeriesMetadata.getTvSeriesId();
				String title = tvSeriesMetadata.getTitle();
				String titleSimplified = FileUtil.getSimplifiedShowName(title);
				String titleSimplifiedFromFilename = FileUtil.getSimplifiedShowName(titleFromFilename);
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
			}
		}
		//update MediaVideoMetadata
		if (videoMetadata != null && tvSeriesId != null) {
			videoMetadata.setTvSeriesId(tvSeriesId);
			videoMetadata.setTmdbTvId(tvDetails.getId());
			videoMetadata.setSeriesMetadata(MediaInfoStore.getTvSeriesMetadata(tvSeriesId));
		}
		return tvSeriesId;
	}

	private static TvDetailsSchema getMatchingTvShow(Long tvShowId, String titleSimplified, Integer year) throws IOException {
		TvDetailsSchema tvDetails = getTvShowInfo(tvShowId);
		if (year != null) {
			//check year
			Integer firstAirYear = getYear(tvDetails.getFirstAirDate());
			Integer lastAirYear = getYear(tvDetails.getLastAirDate());
			if (firstAirYear == null) {
				firstAirYear = 0;
			}
			if (lastAirYear == null) {
				lastAirYear = 9999;
			}
			if (year < firstAirYear || year > lastAirYear) {
				return null;
			}
		}
		String nameSimplified = FileUtil.getSimplifiedShowName(tvDetails.getName());
		if (titleSimplified.equals(nameSimplified)) {
			return tvDetails;
		}
		//check alternative titles
		if (tvDetails.getAlternativeTitles() != null && !tvDetails.getAlternativeTitles().getResults().isEmpty()) {
			for (TvAlternativeTitleSchema tvAlternativeTitle : tvDetails.getAlternativeTitles().getResults()) {
				nameSimplified = FileUtil.getSimplifiedShowName(tvAlternativeTitle.getTitle());
				if (titleSimplified.equals(nameSimplified)) {
					return tvDetails;
				}
			}
		}
		return null;
	}

	private static TvDetailsSchema getTvShowFromEpisode(String title, Integer year, String imdbId) throws IOException {
		String titleSimplified = FileUtil.getSimplifiedShowName(title);
		List<Long> tvShowIds = new ArrayList<>();
		if (imdbId != null) {
			FindSchema findResult = CLIENT.find(imdbId, FindExternalSource.IMDB_ID).getResults();
			//look into episode results
			if (!findResult.getTvEpisodeResults().isEmpty()) {
				for (TvEpisodeTypedSchema tvEpisodeTyped : findResult.getTvEpisodeResults()) {
					Long tvShowId = tvEpisodeTyped.getShowId();
					if (tvShowId != null && !tvShowIds.contains(tvShowId)) {
						TvDetailsSchema tvDetails = getMatchingTvShow(tvShowId, titleSimplified, year);
						if (tvDetails != null) {
							return tvDetails;
						} else {
							tvShowIds.add(tvShowId);
						}
					}
				}
			}
			//look into series results
			if (!findResult.getTvResults().isEmpty()) {
				for (TvTypedSchema tvTyped : findResult.getTvResults()) {
					Long tvShowId = tvTyped.getId();
					if (tvShowId != null && !tvShowIds.contains(tvShowId)) {
						TvDetailsSchema tvDetails = getMatchingTvShow(tvShowId, titleSimplified, year);
						if (tvDetails != null) {
							return tvDetails;
						} else {
							tvShowIds.add(tvShowId);
						}
					}
				}
			}
		}
		SearchTvEndpoint searchTvEndpoint = CLIENT.search(title).forTvShow();
		TvSimpleResultsSchema tvSimpleResultsSchema = searchTvEndpoint.getResults();
		if (tvSimpleResultsSchema != null && tvSimpleResultsSchema.getTotalResults() > 0) {
			for (TvSimpleSchema tvSimple : tvSimpleResultsSchema.getResults()) {
				Long tvShowId = tvSimple.getId();
				if (tvShowId != null && !tvShowIds.contains(tvShowId)) {
					TvDetailsSchema tvDetails = getMatchingTvShow(tvShowId, titleSimplified, year);
					if (tvDetails != null) {
						return tvDetails;
					} else {
						tvShowIds.add(tvShowId);
					}
				}
			}
		}
		return null;
	}

	public static boolean updateTvShowForEpisode(final MediaInfo mediaInfo, final long tvShowId) {
		Integer episode = mediaInfo.getVideoMetadata().getFirstTvEpisodeNumber();
		Integer season = mediaInfo.getVideoMetadata().getTvSeason();
		if (season == null || episode == null) {
			return false;
		}
		try {
			TvDetailsSchema tvDetails = getTvShowInfo(tvShowId);
			return updateTvShowForEpisode(mediaInfo, tvShowId, season, episode, tvDetails);
		} catch (IOException ex) {
			LOGGER.trace("Error in updateTvShowForEpisode:", ex);
		}
		return false;
	}

	private static boolean updateTvShowForEpisode(final MediaInfo mediaInfo, final long tvShowId, final int season, final int episode, final TvDetailsSchema tvDetails) {
		Connection connection = null;
		try {
			TvEpisodeDetailsSchema tvEpisodeDetails = getTvEpisodeInfo(tvShowId, season, episode);
			if (tvDetails != null && tvEpisodeDetails != null && mediaInfo.getFileId() != null) {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection == null) {
					return false;
				}
				insertTvShowMetadata(connection, null, mediaInfo.getVideoMetadata(), tvDetails);
				setTvEpisodeMetadata(connection, mediaInfo.getFileId(), mediaInfo, tvEpisodeDetails);
				//let store know that we change media metadata
				String filename = MediaTableFiles.getFilenameById(connection, mediaInfo.getFileId());
				if (filename != null) {
					MediaStoreIds.incrementUpdateIdForFilename(connection, filename);
				}
				return true;
			}
		} catch (IOException | SQLException ex) {
			LOGGER.trace("Error in setMovieMetadata:", ex);
		} finally {
			MediaDatabase.close(connection);
		}
		return false;
	}

	public static boolean updateTvShowMetadata(final Long tvSeriesId, final long tvShowId) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection == null) {
				return false;
			}
			TvDetailsSchema tvDetails = getTvShowInfo(tvShowId);
			TvSeriesMetadata tvSeriesMetadata = setTvShowMetadata(connection, tvDetails);
			Long newTvSeriesId = tvSeriesMetadata.getTvSeriesId();
			//update related tv episodes
			MediaInfoStore.updateTvEpisodesTvSeriesId(tvSeriesId, newTvSeriesId);
			return true;
		} catch (IOException ex) {
			LOGGER.trace("Error in updateTvShowMetadata:", ex);
		} finally {
			MediaDatabase.close(connection);
		}
		return false;
	}

	public static JsonArray getTvShowsFromEpisode(String title, Integer year, String lang, Long currentId) throws IOException {
		JsonArray result = new JsonArray();
		SearchTvEndpoint searchTvEndpoint = CLIENT.search(title).forTvShow();
		if (year != null && year > 0) {
			searchTvEndpoint.setFirstAirDateYear(year);
		}
		if (lang != null) {
			searchTvEndpoint.setLanguage(lang);
		}
		TvSimpleResultsSchema tvSimpleResultsSchema = searchTvEndpoint.getResults();
		for (TvSimpleSchema tvShow : tvSimpleResultsSchema.getResults()) {
			JsonObject tvShowObject = new JsonObject();
			tvShowObject.addProperty("id", tvShow.getId());
			tvShowObject.addProperty("title", tvShow.getName());
			tvShowObject.addProperty("poster", TMDB.getStillUrl(tvShow.getPosterPath()));
			tvShowObject.addProperty("overview", tvShow.getOverview());
			tvShowObject.addProperty("year", tvShow.getFirstAirDate());
			tvShowObject.addProperty("original_language", tvShow.getOriginalLanguage());
			tvShowObject.addProperty("original_title", tvShow.getOriginalName());
			tvShowObject.addProperty("selected", tvShow.getId().equals(currentId));
			result.add(tvShowObject);
		}
		return result;
	}

	public static JsonArray getMovies(String title, Integer year, String lang, Long currentId) throws IOException {
		JsonArray result = new JsonArray();
		SearchMovieEndpoint searchMovieEndpoint = CLIENT.search(title).forMovie();
		if (year != null && year != 0) {
			searchMovieEndpoint.setYear(year);
		}
		if (lang != null) {
			searchMovieEndpoint.setLanguage(lang);
		}
		MovieShortResultsSchema movieShortResults = searchMovieEndpoint.getResults();
		for (MovieShortSchema movie : movieShortResults.getResults()) {
			JsonObject movieObject = new JsonObject();
			movieObject.addProperty("id", movie.getId());
			movieObject.addProperty("title", movie.getTitle());
			movieObject.addProperty("poster", TMDB.getPosterUrl(movie.getPosterPath()));
			movieObject.addProperty("overview", movie.getOverview());
			movieObject.addProperty("year", movie.getReleaseDate());
			movieObject.addProperty("original_language", movie.getOriginalLanguage());
			movieObject.addProperty("original_title", movie.getOriginalTitle());
			movieObject.addProperty("selected", movie.getId().equals(currentId));
			result.add(movieObject);
		}
		return result;
	}

	public static boolean updateMovieMetadata(final MediaInfo mediaInfo, final Long tmdbId) {
		Connection connection = null;
		try {
			MovieDetailsSchema movieDetails = getMovieInfo(tmdbId, null);
			if (movieDetails != null && mediaInfo.getFileId() != null) {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection == null) {
					return false;
				}
				setMovieMetadata(connection, mediaInfo.getFileId(), mediaInfo, movieDetails);
				//let store know that we change media metadata
				String filename = MediaTableFiles.getFilenameById(connection, mediaInfo.getFileId());
				if (filename != null) {
					MediaStoreIds.incrementUpdateIdForFilename(connection, filename);
				}
				return true;
			}
		} catch (IOException | SQLException ex) {
			LOGGER.trace("Error in setMovieMetadata:", ex);
		} finally {
			MediaDatabase.close(connection);
		}
		return false;
	}

	/**
	 * Attempt to return translated infos from TMDB on the language asked.
	 *
	 * @param language the asked language.
	 * @param imdbId media imdb id.
	 * @param tmdbType media tmdb type ("movie", "tv", "tv_season",
	 * "tv_episode").
	 * @param tmdbId media tmdb id.
	 * @param season media tv series season.
	 * @param episode media tv series episode.
	 * @return the VideoMetadataLocalized for the specific language.
	 */
	public static synchronized VideoMetadataLocalized getVideoMetadataLocalized(
			final String language,
			final String mediaType,
			final String imdbId,
			final Long tmdbId,
			final Integer season,
			final String episode
	) {
		if (!CONFIGURATION.getExternalNetwork()) {
			return null;
		}
		if (!isReady()) {
			//fallback to UMS API
			return APIUtils.getVideoMetadataLocalizedFromImdb(language, mediaType, imdbId, tmdbId, season, episode);
		}
		if (StringUtils.isNotBlank(language) && StringUtils.isNotBlank(mediaType) && tmdbId != null && tmdbId > 0) {
			return switch (mediaType) {
				case "collection" ->
					getCollectionMetadataLocalized(tmdbId, language);
				case "movie" ->
					getMovieMetadataLocalized(tmdbId, language);
				case "tv" ->
					getTvShowMetadataLocalized(tmdbId, language);
				case "tv_season" ->
					getTvSeasonMetadataLocalized(tmdbId, season, language);
				case "tv_episode" ->
					getTvEpisodeMetadataLocalized(tmdbId, season, episode, language);
				default ->
					null;
			};
		}
		return null;
	}

	private static VideoMetadataLocalized getCollectionMetadataLocalized(final Long tmdbId, final String language) {
		CollectionDetailsSchema collectionDetailsSchema = CLIENT.collection(tmdbId)
				.setLanguage(language)
				.getDetails();
		VideoMetadataLocalized metadata = new VideoMetadataLocalized();
		metadata.setOverview(collectionDetailsSchema.getOverview());
		if (StringUtils.isNotBlank(collectionDetailsSchema.getPosterPath())) {
			String poster = getPosterUrl(collectionDetailsSchema.getPosterPath());
			metadata.setPoster(poster);
		}
		metadata.setTitle(collectionDetailsSchema.getName());
		metadata.setTmdbID(collectionDetailsSchema.getId());
		return metadata;
	}

	private static VideoMetadataLocalized getMovieMetadataLocalized(final Long tmdbId, final String language) {
		MovieDetailsSchema movieDetailsSchema = CLIENT.movie(tmdbId)
				.setLanguage(language)
				.getDetails();
		VideoMetadataLocalized metadata = new VideoMetadataLocalized();
		metadata.setHomepage(movieDetailsSchema.getHomepage());
		metadata.setOverview(movieDetailsSchema.getOverview());
		if (StringUtils.isNotBlank(movieDetailsSchema.getPosterPath())) {
			String poster = getPosterUrl(movieDetailsSchema.getPosterPath());
			metadata.setPoster(poster);
		}
		metadata.setTagline(movieDetailsSchema.getTagline());
		metadata.setTitle(movieDetailsSchema.getTitle());
		metadata.setTmdbID(movieDetailsSchema.getId());
		return metadata;
	}

	private static VideoMetadataLocalized getTvShowMetadataLocalized(final Long tmdbId, final String language) {
		TvDetailsSchema tvDetailsSchema = CLIENT.tv(tmdbId)
				.setLanguage(language)
				.getDetails();
		VideoMetadataLocalized metadata = new VideoMetadataLocalized();
		metadata.setHomepage(tvDetailsSchema.getHomepage());
		metadata.setOverview(tvDetailsSchema.getOverview());
		metadata.setPoster(tvDetailsSchema.getOverview());
		if (StringUtils.isNotBlank(tvDetailsSchema.getPosterPath())) {
			String poster = getPosterUrl(tvDetailsSchema.getPosterPath());
			metadata.setPoster(poster);
		}
		metadata.setTagline(tvDetailsSchema.getTagline());
		metadata.setTitle(tvDetailsSchema.getName());
		metadata.setTmdbID(tvDetailsSchema.getId());
		return metadata;
	}

	private static VideoMetadataLocalized getTvSeasonMetadataLocalized(final Long tmdbId, final Integer season, final String language) {
		if (season == null) {
			return null;
		}
		TvSeasonDetailsSchema tvSeasonDetailsSchema = CLIENT.tvSeason(tmdbId, season)
				.setLanguage(language)
				.getDetails();
		VideoMetadataLocalized metadata = new VideoMetadataLocalized();
		metadata.setOverview(tvSeasonDetailsSchema.getOverview());
		if (StringUtils.isNotBlank(tvSeasonDetailsSchema.getPosterPath())) {
			String poster = getPosterUrl(tvSeasonDetailsSchema.getPosterPath());
			metadata.setPoster(poster);
		}
		metadata.setTitle(tvSeasonDetailsSchema.getName());
		metadata.setTmdbID(tmdbId);
		return metadata;
	}

	private static VideoMetadataLocalized getTvEpisodeMetadataLocalized(final Long tmdbId, final Integer season, final String episode, final String language) {
		if (season == null) {
			return null;
		}
		Long episodeNumber = getLong(episode);
		if (episodeNumber == null) {
			return null;
		}
		TvEpisodeDetailsSchema tvEpisodeDetailsSchema = CLIENT.tvEpisode(tmdbId, season, episodeNumber)
				.setLanguage(language)
				.getDetails();
		VideoMetadataLocalized metadata = new VideoMetadataLocalized();
		metadata.setOverview(tvEpisodeDetailsSchema.getOverview());
		if (StringUtils.isNotBlank(tvEpisodeDetailsSchema.getStillPath())) {
			String poster = getStillUrl(tvEpisodeDetailsSchema.getStillPath());
			metadata.setPoster(poster);
		}
		metadata.setTitle(tvEpisodeDetailsSchema.getName());
		metadata.setTmdbID(tvEpisodeDetailsSchema.getId());
		return metadata;
	}

	private static TvEpisodeDetailsSchema getTvEpisodeInfo(
			long tvShowId,
			int seasonNumber,
			int episodeNumber
	) throws IOException {
		return CLIENT.tvEpisode(tvShowId, seasonNumber, episodeNumber)
				.appendToResponse(TvEpisodeAppendToResponse.CREDITS)
				.appendToResponse(TvEpisodeAppendToResponse.EXTERNAL_IDS)
				.appendToResponse(TvEpisodeAppendToResponse.IMAGES)
				.appendToResponse(TvEpisodeAppendToResponse.TRANSLATIONS)
				.getDetails();
	}

	private static TvDetailsSchema getTvShowInfo(
			long tvId
	) throws IOException {
		return CLIENT.tv(tvId)
				.appendToResponse(TvIdAppendToResponse.ALTERNATIVE_TITLES)
				.appendToResponse(TvIdAppendToResponse.CONTENT_RATINGS)
				.appendToResponse(TvIdAppendToResponse.CREDITS)
				.appendToResponse(TvIdAppendToResponse.EXTERNAL_IDS)
				.appendToResponse(TvIdAppendToResponse.IMAGES)
				.appendToResponse(TvIdAppendToResponse.TRANSLATIONS)
				.getDetails();
	}

	private static MovieDetailsSchema getMovieInfo(
			String title,
			Integer year,
			String imdbID
	) throws IOException {
		if (imdbID != null) {
			FindSchema findResult = CLIENT.find(imdbID, FindExternalSource.IMDB_ID).getResults();
			if (!findResult.getMovieResults().isEmpty()) {
				for (MovieTypedSchema movieTyped : findResult.getMovieResults()) {
					Long tmdbId = movieTyped.getId();
					MovieDetailsSchema movieDetails = getMovieInfo(tmdbId, null);
					if (year != null && year != 0) {
						//check for year
						if (getReleaseYears(movieDetails.getReleaseDates()).contains(year)) {
							return movieDetails;
						}
					} else {
						//return first result
						return movieDetails;
					}
				}
			}
		}
		SearchMovieEndpoint searchMovieEndpoint = CLIENT.search(title).forMovie();
		if (year != null && year != 0) {
			searchMovieEndpoint.setYear(year);
		}
		MovieShortResultsSchema movieShortResults = searchMovieEndpoint.getResults();
		if (movieShortResults != null && movieShortResults.getTotalResults() > 0) {
			for (MovieShortSchema movieShort : movieShortResults.getResults()) {
				Long tmdbId = movieShort.getId();
				MovieDetailsSchema movieDetails = getMovieInfo(tmdbId, null);
				if (year != null && year != 0) {
					//check for year
					if (getReleaseYears(movieDetails.getReleaseDates()).contains(year)) {
						return movieDetails;
					}
				} else {
					//return first result
					return movieDetails;
				}
			}
		}
		return null;
	}

	private static MovieDetailsSchema getMovieInfo(
			long tmdbId,
			String language
	) throws IOException {
		MovieIdEndpoint movieIdEndpoint = CLIENT.movie(tmdbId)
				.appendToResponse(MovieIdAppendToResponse.ALTERNATIVE_TITLES)
				.appendToResponse(MovieIdAppendToResponse.CREDITS)
				.appendToResponse(MovieIdAppendToResponse.EXTERNAL_IDS)
				.appendToResponse(MovieIdAppendToResponse.IMAGES)
				.appendToResponse(MovieIdAppendToResponse.RELEASE_DATES)
				.appendToResponse(MovieIdAppendToResponse.REVIEWS)
				.appendToResponse(MovieIdAppendToResponse.TRANSLATIONS);
		if (language != null) {
			movieIdEndpoint.setLanguage(language);
		}
		return movieIdEndpoint.getDetails();
	}

	/**
	 * Return five actors ordered
	 *
	 * @param cast list
	 * @return
	 */
	private static ApiStringArray getActors(List<? extends PersonRoleCreditSchema> cast) {
		//search for actors
		Collections.sort(cast, (role1, role2) -> role1.getOrder().intValue() - role2.getOrder().intValue());
		ApiStringArray result = new ApiStringArray();
		for (PersonRoleCreditSchema person : cast) {
			result.add(person.getName());
			if (result.size() > 4) {
				break;
			}
		}
		return result;
	}

	/**
	 * Return US certification
	 *
	 * @param movieReleaseDates list
	 * @return
	 */
	private static String getUsCertification(MovieReleaseDatesSchema movieReleaseDates) {
		if (movieReleaseDates == null || movieReleaseDates.getResults() == null || movieReleaseDates.getResults().isEmpty()) {
			return null;
		}
		for (MovieReleaseDatesResultSchema countryReleaseDate : movieReleaseDates.getResults()) {
			if ("US".equals(countryReleaseDate.getIso3166Part1())) {
				for (MovieReleaseDateSchema movieReleaseDate : countryReleaseDate.getReleaseDates()) {
					if (movieReleaseDate.getCertification() != null) {
						return movieReleaseDate.getCertification();
					}
				}
			}
		}
		return null;
	}

	/**
	 * Return US certification
	 *
	 * @param tvContentRatings list
	 * @return
	 */
	private static String getUsContentRating(TvContentRatingsSchema tvContentRatings) {
		if (tvContentRatings == null || tvContentRatings.getResults() == null || tvContentRatings.getResults().isEmpty()) {
			return null;
		}
		for (TvContentRatingSchema tvContentRating : tvContentRatings.getResults()) {
			if ("US".equals(tvContentRating.getIso3166Part1())) {
				if (tvContentRating.getRating() != null) {
					return tvContentRating.getRating();
				}
			}
		}
		return null;
	}

	/**
	 * Return countries
	 *
	 * @param countries list
	 * @return
	 */
	private static ApiStringArray getCountries(List<CountrySimpleSchema> countries) {
		ApiStringArray result = new ApiStringArray();
		for (CountrySimpleSchema country : countries) {
			result.add(country.getIso3166Part1());
		}
		return result;
	}

	/**
	 * Return directors
	 *
	 * @param countries list
	 * @return directors list
	 */
	private static ApiStringArray getDirectors(List<PersonJobCreditSchema> crew) {
		//search for directors
		ApiStringArray result = new ApiStringArray();
		for (PersonJobCreditSchema person : crew) {
			if ("Directing".equals(person.getDepartment()) && "Director".equals(person.getJob())) {
				result.add(person.getName());
			}
		}
		return result;
	}

	/**
	 * Return Release Years
	 *
	 * @param movieReleaseDates movieReleaseDates
	 * @return release years list
	 */
	private static List<Integer> getReleaseYears(MovieReleaseDatesSchema movieReleaseDates) {
		if (movieReleaseDates == null || movieReleaseDates.getResults() == null || movieReleaseDates.getResults().isEmpty()) {
			return Collections.emptyList();
		}
		List<Integer> result = new ArrayList<>();
		for (MovieReleaseDatesResultSchema countryReleaseDate : movieReleaseDates.getResults()) {
			for (MovieReleaseDateSchema movieReleaseDate : countryReleaseDate.getReleaseDates()) {
				Integer year = getYear(movieReleaseDate.getReleaseDate());
				if (year != null && !result.contains(year)) {
					result.add(year);
				}
			}
		}
		return result;
	}

	private static Long getLong(String value) {
		try {
			return Long.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Integer getYear(String value) {
		if (value == null || value.length() < 4) {
			return null;
		}
		try {
			return Integer.valueOf(value.substring(0, 4));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String getTmdbImageBaseURL() {
		if (tmdbImageBaseURL == null && isReady()) {
			try {
				ConfigurationSchema configurationSchema = CLIENT.configuration().getConfiguration();
				tmdbImageBaseURL = configurationSchema.getImages().getBaseUrl();
			} catch (Exception e) {
				//let use APIUtils
			}
		}
		if (tmdbImageBaseURL == null) {
			//fallback to UMS API
			return APIUtils.getApiImageBaseURL();
		}
		return tmdbImageBaseURL;
	}

	/**
	 * @param posterPath this is a "poster_path" from TMDB
	 * @return a full URL to an image
	 */
	public static String getPosterUrl(String posterPath) {
		if (posterPath != null) {
			return getTmdbImageBaseURL() + "original" + posterPath;
		}

		return null;
	}

	/**
	 * @param imagePath this is a "still_path" from TMDB
	 * @return a full URL to an image
	 */
	public static String getStillUrl(String stillPath) {
		if (stillPath != null) {
			return getTmdbImageBaseURL() + "original" + stillPath;
		}

		return null;
	}

	private static ApiStringArray getApiStringArrayFromList(final List<IntegerIdNameSchema> element) {
		if (element == null) {
			return null;
		}
		ApiStringArray result = new ApiStringArray();
		Iterator<IntegerIdNameSchema> i = element.iterator();
		while (i.hasNext()) {
			String str = i.next().getName();
			result.add(str);
		}
		return result;
	}

	private static ApiStringArray getApiStringArrayFromStringList(final List<String> element) {
		if (element == null) {
			return null;
		}
		ApiStringArray result = new ApiStringArray();
		Iterator<String> i = element.iterator();
		while (i.hasNext()) {
			result.add(i.next());
		}
		return result;
	}

}
