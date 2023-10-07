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
import net.pms.store.MediaStoreIds;
import net.pms.store.ThumbnailSource;
import net.pms.store.ThumbnailStore;
import net.pms.util.FileUtil;
import net.pms.util.ImdbUtil;
import net.pms.util.SimpleThreadFactory;
import net.ums.tmdbapi.TMDbClient;
import net.ums.tmdbapi.endpoint.find.FindExternalSource;
import net.ums.tmdbapi.endpoint.movie.MovieIdAppendToResponse;
import net.ums.tmdbapi.endpoint.movie.MovieIdEndpoint;
import net.ums.tmdbapi.endpoint.search.SearchMovieEndpoint;
import net.ums.tmdbapi.endpoint.search.SearchTvEndpoint;
import net.ums.tmdbapi.endpoint.tv.TvIdAppendToResponse;
import net.ums.tmdbapi.endpoint.tv.episode.TvEpisodeAppendToResponse;
import net.ums.tmdbapi.schema.IntegerIdNameSchema;
import net.ums.tmdbapi.schema.collection.CollectionDetailsSchema;
import net.ums.tmdbapi.schema.configuration.ConfigurationSchema;
import net.ums.tmdbapi.schema.country.CountrySimpleSchema;
import net.ums.tmdbapi.schema.find.FindSchema;
import net.ums.tmdbapi.schema.movie.MovieDetailsSchema;
import net.ums.tmdbapi.schema.movie.MovieReleaseDateSchema;
import net.ums.tmdbapi.schema.movie.MovieReleaseDatesResultSchema;
import net.ums.tmdbapi.schema.movie.MovieReleaseDatesSchema;
import net.ums.tmdbapi.schema.movie.MovieShortResultsSchema;
import net.ums.tmdbapi.schema.movie.MovieShortSchema;
import net.ums.tmdbapi.schema.movie.MovieTypedSchema;
import net.ums.tmdbapi.schema.person.PersonJobCreditSchema;
import net.ums.tmdbapi.schema.person.PersonRoleCreditSchema;
import net.ums.tmdbapi.schema.tv.TvAlternativeTitleSchema;
import net.ums.tmdbapi.schema.tv.TvContentRatingSchema;
import net.ums.tmdbapi.schema.tv.TvContentRatingsSchema;
import net.ums.tmdbapi.schema.tv.TvDetailsSchema;
import net.ums.tmdbapi.schema.tv.TvSimpleResultsSchema;
import net.ums.tmdbapi.schema.tv.TvSimpleSchema;
import net.ums.tmdbapi.schema.tv.TvTypedSchema;
import net.ums.tmdbapi.schema.tv.episode.TvEpisodeDetailsSchema;
import net.ums.tmdbapi.schema.tv.episode.TvEpisodeTypedSchema;
import net.ums.tmdbapi.schema.tv.season.TvSeasonDetailsSchema;
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
		if (StringUtils.isBlank(CONFIGURATION.getTmdbApiKey())) {
			return false;
		}
		if (!CONFIGURATION.getTmdbApiKey().equals(CLIENT.getApiKey())) {
			CLIENT.setApiKey(CONFIGURATION.getTmdbApiKey());
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
		Runnable r = () -> {
			// wait until the realtime lock is released before starting
			PMS.REALTIME_LOCK.lock();
			PMS.REALTIME_LOCK.unlock();

			if (!CONFIGURATION.getExternalNetwork()) {
				LOGGER.trace("Not doing background TMDB lookup because external network is disabled");
				return;
			}

			if (!MediaDatabase.isAvailable()) {
				LOGGER.trace("Not doing background TMDB lookup because database is closed");
				return;
			}

			if (!CONFIGURATION.isUseInfoFromTMDB()) {
				LOGGER.trace("Not doing background TMDB lookup because isUseInfoFromTMDB is disabled");
				//fallback to UMS API.
				APIUtils.backgroundLookupAndAddMetadata(file, mediaInfo);
				return;
			}

			if (!isReady()) {
				LOGGER.trace("Not doing background TMDB lookup because api key");
				//fallback to UMS API.
				APIUtils.backgroundLookupAndAddMetadata(file, mediaInfo);
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
				GuiManager.setSecondaryStatusLine(Messages.getString("GettingTMDBInfoFor") + " " + file.getName());
				connection.setAutoCommit(false);
				if (mediaInfo.hasVideoMetadata() && mediaInfo.getVideoMetadata().isTVEpisode()) {
					lookupAndAddTvEpisodeMetadata(connection, file, mediaInfo);
				} else {
					lookupAndAddMovieMetadata(connection, file, mediaInfo);
				}
				exitLookupAndAddMetadata(connection);
			} catch (SQLException ex) {
				LOGGER.trace("Error in TMDB parsing:", ex);
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
				LOGGER.error("Error in commit in TMDB.backgroundLookupAndAddMetadata: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		}
		GuiManager.setSecondaryStatusLine(null);
	}

	private static void lookupAndAddMovieMetadata(Connection connection, final File file, final MediaInfo mediaInfo) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();

		String year = videoMetadata.getYear();
		String titleFromFilename = videoMetadata.getMovieOrShowName();
		MovieDetailsSchema movieDetails;

		try {
			String imdbID = ImdbUtil.extractImdbId(file.toPath(), false);
			if (StringUtils.isBlank(titleFromFilename)) {
				titleFromFilename = FileUtil.getFileNameWithoutExtension(file.getName());
			}
			// Remove the year from the title before lookup if it exists
			String yearRegex = StringUtils.isNotBlank(year) ? year : "(?:19|20)\\d{2}";
			int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + yearRegex + "\\)"), titleFromFilename);
			if (yearIndex > -1) {
				titleFromFilename = titleFromFilename.substring(0, yearIndex);
			}
			movieDetails = getMovieInfo(titleFromFilename, year, imdbID);

			if (movieDetails == null) {
				LOGGER.trace("Failed lookup for " + file.getName());
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

		//advertise queue size (only when a new real lookup is done to not flood)
		LOGGER.info("TMDB: {} background task in queue", BACKGROUND_EXECUTOR.getQueue().size());
	}

	private static void setMovieMetadata(Connection connection, final Long fileId, final MediaInfo mediaInfo, MovieDetailsSchema movieDetails) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();
		String titleFromFilename = videoMetadata.getMovieOrShowName();

		// Now that we are happy with the TMDB data, let's make some clearer variables
		String title = StringUtils.isNotBlank(movieDetails.getTitle()) ? movieDetails.getTitle() : titleFromFilename;
		String titleSimplified = FileUtil.getSimplifiedShowName(title);
		String yearFromTMDB = StringUtils.isNotBlank(movieDetails.getReleaseDate()) ? movieDetails.getReleaseDate().substring(0, 4) : "";

		videoMetadata.setMovieOrShowName(title);
		videoMetadata.setSimplifiedMovieOrShowName(titleSimplified);
		videoMetadata.setYear(yearFromTMDB);

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
		//videoMetadata.setRated(getStringOrNull(movieDetails, "rated"));
		//Release Dates -> US
		videoMetadata.setRating(movieDetails.getVoteAverage().toString());
		videoMetadata.setReleased(movieDetails.getReleaseDate());
		videoMetadata.setRevenue(movieDetails.getRevenue());
		videoMetadata.setTagline(movieDetails.getTagline());
		videoMetadata.setVotes(movieDetails.getVoteCount().toString());
		mediaInfo.setVideoMetadata(videoMetadata);

		LOGGER.trace("setting movie metadata for " + title);
		//store unlocalized data first
		MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, mediaInfo, true);
		//now localize data if needed
		String lang = CONFIGURATION.getLanguageRawString();
		if (lang != null && !"en-us".equalsIgnoreCase(lang)) {
			VideoMetadataLocalized loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(connection, fileId, false, lang, videoMetadata.getIMDbID(), "movie", videoMetadata.getTmdbId(), null, null);
			if (loc != null) {
				loc.localizeMediaVideoMetadata(videoMetadata);
			}
		}
		//now check the thumbnail localized
		if (!StringUtils.isBlank(videoMetadata.getPoster())) {
			mediaInfo.waitMediaParsing(5);
			mediaInfo.setParsing(true);
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(videoMetadata.getPoster());
			if (thumbnail != null) {
				Long thumbnailId = ThumbnailStore.getId(thumbnail);
				mediaInfo.setThumbnailSource(ThumbnailSource.TMDB_LOC);
				mediaInfo.setThumbnailId(thumbnailId);
				MediaTableFiles.updateThumbnailId(connection, fileId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
			}
			mediaInfo.setParsing(false);
		}
		//let store know that we change media metadata
		MediaStoreIds.incrementUpdateIdForFileId(connection, fileId);
	}

	private static void lookupAndAddTvEpisodeMetadata(Connection connection, final File file, final MediaInfo mediaInfo) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();

		String titleFromFilename = videoMetadata.getMovieOrShowName();
		Long tvSeasonFromFilename = getLong(videoMetadata.getTVSeason());
		Long tvEpisodeNumberFromFilename = getLong(videoMetadata.getTVEpisodeNumber());
		String tvSeriesStartYear = videoMetadata.getTVSeriesStartYear();

		TvEpisodeDetailsSchema tvEpisodeDetails;
		// unset tvSeriesStartYear if it is NOT in the title because it must have come from TMDB earlier and will mess up the matching logic
		// todo: use better matching logic
		if (StringUtils.isNotBlank(tvSeriesStartYear)) {
			int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + tvSeriesStartYear + "\\)"), titleFromFilename);
			if (yearIndex == -1) {
				tvSeriesStartYear = null;
			}
		}
		String imdbId = ImdbUtil.extractImdbId(file.toPath(), false);
		if (StringUtils.isBlank(titleFromFilename)) {
			titleFromFilename = FileUtil.getFileNameWithoutExtension(file.getName());
		}
		// Remove the year from the title before lookup if it exists
		String yearRegex = StringUtils.isNotBlank(tvSeriesStartYear) ? tvSeriesStartYear : "(?:19|20)\\d{2}";
		int yearIndex = FileUtil.indexOf(Pattern.compile("\\s\\(" + yearRegex + "\\)"), titleFromFilename);
		if (yearIndex > -1) {
			titleFromFilename = titleFromFilename.substring(0, yearIndex);
		}

		Long tvShowId;
		try {
			String simplifiedTitle = FileUtil.getSimplifiedShowName(titleFromFilename);
			tvShowId = MediaTableTVSeries.getTmdbIdByTitle(connection, simplifiedTitle);
			if (tvShowId == null) {
				//not found in database
				String failedLookupKey = simplifiedTitle;
				if (imdbId != null) {
					LOGGER.trace("Failed lookup for " + file.getName());
					failedLookupKey += imdbId;
				}
				if (MediaTableFailedLookups.hasLookupFailedRecently(connection, failedLookupKey, false)) {
					return;
				}

				//search for a tv show
				TvDetailsSchema tvDetails = getTvShowFromEpisode(titleFromFilename, tvSeriesStartYear, imdbId);
				if (tvDetails == null) {
					LOGGER.trace("Failed lookup for " + file.getName());
					LOGGER.trace("Did not find matching series for the episode in TMDB for {}", file.getName());
					MediaTableFailedLookups.set(connection, failedLookupKey, "tvShow not found", false);
					MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "tvShow not found", true);
					return;
				}
				tvShowId = tvDetails.getId();
				//attempt to enhance TV series data
				setTvShowMetadata(connection, titleFromFilename, videoMetadata, tvDetails);
			}
			videoMetadata.setTmdbTvId(tvShowId);

			if (tvSeasonFromFilename == null) {
				LOGGER.trace("Failed lookup for " + file.getName());
				MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "seasonNumber missing", true);
				return;
			} else if (tvEpisodeNumberFromFilename == null) {
				LOGGER.trace("Failed lookup for " + file.getName());
				MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "episodeNumber missing", true);
				return;
			}
			tvEpisodeDetails = getTvEpisodeInfo(tvShowId, tvSeasonFromFilename, tvEpisodeNumberFromFilename);
			if (tvEpisodeDetails == null) {
				LOGGER.trace("Failed lookup for " + file.getName());
				MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "tvEpisode not found", true);
				return;
			} else {
				LOGGER.trace("Found an TMDB match for " + file.getName());
			}
		} catch (IOException ex) {
			// this likely means a transient error so don't store the failure, to allow retries
			LOGGER.debug("Likely transient error", ex);
			return;
		}
		// At this point, this is the episode title if it is an episode
		Long tvSeasonFromTMDB = tvEpisodeDetails.getSeasonNumber();
		Long tvEpisodeNumberFromTMDB = tvEpisodeDetails.getEpisodeNumber();

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
		if (!tvSeasonFromFilename.equals(tvSeasonFromTMDB) ||
				!tvEpisodeNumberFromFilename.equals(tvEpisodeNumberFromTMDB)) {
			LOGGER.debug("TMDB data was different to our parsed data, not storing it.");
			MediaTableFailedLookups.set(connection, file.getAbsolutePath(), "Data mismatch", true);

			LOGGER.trace("Filename data: " + mediaInfo);
			LOGGER.trace("TMDB data: " + tvEpisodeDetails);
			return;
		}

		LOGGER.trace("TMDB data matches filename data for " + file.getName());
		Long fileId = MediaTableFiles.getFileId(connection, file.getAbsolutePath(), file.lastModified());
		setTvEpisodeMetadata(connection, fileId, mediaInfo, tvEpisodeDetails);
		//advertise queue size (only when a new real lookup is done to not flood)
		LOGGER.info("TMDB: {} background task in queue", BACKGROUND_EXECUTOR.getQueue().size());
	}

	private static void setTvEpisodeMetadata(Connection connection, final Long fileId, final MediaInfo mediaInfo, final TvEpisodeDetailsSchema tvEpisodeDetails) throws SQLException {
		MediaVideoMetadata videoMetadata = mediaInfo.hasVideoMetadata() ? mediaInfo.getVideoMetadata() : new MediaVideoMetadata();
		String titleFromFilename = videoMetadata.getMovieOrShowName();

		// At this point, this is the episode title if it is an episode
		String tvEpisodeTitleFromTMDB = tvEpisodeDetails.getName();
		Long tvSeasonFromTMDB = tvEpisodeDetails.getSeasonNumber();
		Long tvEpisodeNumberFromTMDB = tvEpisodeDetails.getEpisodeNumber();

		// Now that we are happy with the TMDB data, let's make some clearer variables
		String titleFromDatabase = MediaTableTVSeries.getTitleByTmdbId(connection, videoMetadata.getTmdbTvId());
		String title = StringUtils.isBlank(titleFromDatabase) ? titleFromFilename : titleFromDatabase;
		String titleSimplified = FileUtil.getSimplifiedShowName(title);

		videoMetadata.setMovieOrShowName(title);
		videoMetadata.setSimplifiedMovieOrShowName(titleSimplified);
		videoMetadata.setYear(tvEpisodeDetails.getAirDate() != null ? tvEpisodeDetails.getAirDate().substring(0, 4) : null);

		videoMetadata.setTmdbId(tvEpisodeDetails.getId());
		videoMetadata.setIMDbID(tvEpisodeDetails.getExternalIds() != null ? tvEpisodeDetails.getExternalIds().getImdbId() : null);

		// Set the poster as the thumbnail
		String posterFromTMDB = getStillUrl(tvEpisodeDetails.getStillPath());
		if (posterFromTMDB != null) {
			videoMetadata.setPoster(posterFromTMDB);
		}
		videoMetadata.setTVSeason(tvSeasonFromTMDB.toString());
		videoMetadata.setTVEpisodeNumber(tvEpisodeNumberFromTMDB.toString());
		if (StringUtils.isNotBlank(tvEpisodeTitleFromTMDB)) {
			LOGGER.trace("Setting episode name from TMDB: " + tvEpisodeTitleFromTMDB);
			videoMetadata.setTVEpisodeName(tvEpisodeTitleFromTMDB);
		}
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
		mediaInfo.setVideoMetadata(videoMetadata);

		LOGGER.trace("setting tv episode metadata for " + title + " " + tvSeasonFromTMDB + "-" + tvEpisodeNumberFromTMDB);
		//store unlocalized data first
		MediaTableVideoMetadata.insertOrUpdateVideoMetadata(connection, fileId, mediaInfo, true);
		//now localize data if needed
		String lang = CONFIGURATION.getLanguageRawString();
		if (lang != null && !"en-us".equalsIgnoreCase(lang)) {
			VideoMetadataLocalized loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(connection, fileId, false, lang, videoMetadata.getIMDbID(), "tv_episode", videoMetadata.getTmdbTvId(), videoMetadata.getTVSeason(), videoMetadata.getTVEpisodeNumber());
			if (loc != null) {
				loc.localizeMediaVideoMetadata(videoMetadata);
			}
		}
		//now check the thumbnail localized
		if (!StringUtils.isBlank(videoMetadata.getPoster())) {
			mediaInfo.waitMediaParsing(5);
			mediaInfo.setParsing(true);
			DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(videoMetadata.getPoster());
			if (thumbnail != null) {
				Long thumbnailId = ThumbnailStore.getId(thumbnail);
				mediaInfo.setThumbnailSource(ThumbnailSource.TMDB_LOC);
				mediaInfo.setThumbnailId(thumbnailId);
				MediaTableFiles.updateThumbnailId(connection, fileId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
			}
			mediaInfo.setParsing(false);
		}
		//let store know that we change media metadata
		MediaStoreIds.incrementUpdateIdForFileId(connection, fileId);
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
	 */
	private static void setTvShowMetadata(final Connection connection, String titleFromFilename, MediaVideoMetadata videoMetadata, TvDetailsSchema tvDetails) {
		String title;
		String titleSimplified;
		String titleSimplifiedFromFilename = FileUtil.getSimplifiedShowName(titleFromFilename);

		/*
		 * Get the TV series metadata from our database, or from TMDB if it's not
		 * in our database yet, and persist it to our database.
		 */
		TvSeriesMetadata tvSeriesMetadata = MediaTableTVSeries.getTvSeriesMetadataFromTmdbId(connection, tvDetails.getId());

		if (tvSeriesMetadata != null) {
			LOGGER.trace("TV series with TMDB data already found in database {}", tvSeriesMetadata.getTitle());
		} else {

			/*
			 * This either means there is no entry in the TV Series table for this series, or
			 * there is but it only contains filename info - not Metadata yet.
			 */
			LOGGER.trace("TMDB metadata for TV series {} (TMDB ID: {}) does not already exist in the database", titleFromFilename, tvDetails.getId());

			title = tvDetails.getName();
			titleSimplified = FileUtil.getSimplifiedShowName(title);

			/*
			 * Now we have a TMDB result for the TV series, we need to see whether
			 * to insert it or update existing data, so we attempt to find or
			 * create an entry based on the title.
			 */
			Long tvSeriesId = MediaTableTVSeries.set(connection, title);
			if (tvSeriesId == null) {
				LOGGER.debug("tvSeriesDatabaseId was not set, something went wrong");
				return;
			}

			//create the TvSeriesMetadata
			tvSeriesMetadata = new TvSeriesMetadata();
			if (tvDetails.getCredits() != null && tvDetails.getCredits().getCast() != null) {
				tvSeriesMetadata.setActors(getActors(tvDetails.getCredits().getCast()));
			}
			tvSeriesMetadata.setCountries(getApiStringArrayFromStringList(tvDetails.getOriginCountry()));
			tvSeriesMetadata.setCreatedBy(GSON.toJson(tvDetails.getCreatedBy()));
			if (tvDetails.getCredits() != null) {
				tvSeriesMetadata.setCredits("[" + GSON.toJson(tvDetails.getCredits()) + "]");
			}
			if (Boolean.FALSE.equals(tvDetails.getInProduction())) {
				tvSeriesMetadata.setEndYear(tvDetails.getLastAirDate());
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
			tvSeriesMetadata.setReleased(tvDetails.getFirstAirDate());
			tvSeriesMetadata.setRated(getUsContentRating(tvDetails.getContentRatings()));
			tvSeriesMetadata.setRating(tvDetails.getVoteAverage().toString());
			if (tvDetails.getSeasons() != null) {
				tvSeriesMetadata.setSeasons(GSON.toJson(tvDetails.getSeasons()));
			}
			tvSeriesMetadata.setSeriesType(tvDetails.getType());
			if (tvDetails.getSpokenLanguages() != null) {
				tvSeriesMetadata.setSpokenLanguages(GSON.toJson(tvDetails.getSpokenLanguages()));
			}
			tvSeriesMetadata.setStartYear(tvDetails.getFirstAirDate());
			tvSeriesMetadata.setStatus(tvDetails.getStatus());
			tvSeriesMetadata.setTagline(tvDetails.getTagline());
			tvSeriesMetadata.setTmdbId(tvDetails.getId());
			tvSeriesMetadata.setTitle(tvDetails.getName());
			tvSeriesMetadata.setTotalSeasons(tvDetails.getNumberOfSeasons().doubleValue());
			tvSeriesMetadata.setVotes(tvDetails.getVoteCount().toString());

			//store unlocalized data first
			MediaTableTVSeries.updateAPIMetadata(connection, tvSeriesMetadata, tvSeriesId);

			//now localize data if needed
			String lang = CONFIGURATION.getLanguageRawString();
			if (lang != null && !"en-us".equalsIgnoreCase(lang)) {
				VideoMetadataLocalized loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(connection, tvSeriesId, true, lang, tvSeriesMetadata.getIMDbID(), "tv", tvSeriesMetadata.getTmdbId(), null, null);
				if (loc != null) {
					loc.localizeTvSeriesMetadata(tvSeriesMetadata);
				}
			}

			//now check the thumbnail localized
			if (!StringUtils.isBlank(tvSeriesMetadata.getPoster())) {
				DLNAThumbnail thumbnail = JavaHttpClient.getThumbnail(tvSeriesMetadata.getPoster());
				if (thumbnail != null) {
					Long thumbnailId = ThumbnailStore.getIdForTvSerie(thumbnail, tvSeriesId, ThumbnailSource.TMDB);
					tvSeriesMetadata.setThumbnailSource(ThumbnailSource.TMDB);
					tvSeriesMetadata.setThumbnailId(thumbnailId);
					MediaTableTVSeries.updateThumbnailId(connection, tvSeriesId, thumbnailId, ThumbnailSource.TMDB_LOC.toString());
				}
			}

			// Replace any close-but-not-exact titles in the FILES table
			if (titleFromFilename != null &&
					titleSimplifiedFromFilename != null &&
					!title.equals(titleFromFilename) &&
					titleSimplified.equals(titleSimplifiedFromFilename)) {
				LOGGER.trace("Converting rows in FILES table with the show name " + titleFromFilename + " to " + title);
				MediaTableVideoMetadata.updateMovieOrShowName(connection, titleFromFilename, title);
			}
		}
		//update MediaVideoMetadata
		if (videoMetadata != null) {
			videoMetadata.setTmdbTvId(tvDetails.getId());
			videoMetadata.setMovieOrShowName(tvSeriesMetadata.getTitle());
			videoMetadata.setSeriesMetadata(tvSeriesMetadata);
			videoMetadata.setTVSeriesStartYear(tvSeriesMetadata.getStartYear());
		}
	}

	private static TvDetailsSchema getMatchingTvShow(Long tvShowId, String titleSimplified, Integer year) throws IOException {
		TvDetailsSchema tvDetails = getTvShowInfo(tvShowId);
		if (year != null) {
			//check year
			Integer firstAirDate = getYear(tvDetails.getFirstAirDate());
			Integer lastAirDate = getYear(tvDetails.getLastAirDate());
			if (firstAirDate == null) {
				firstAirDate = 0;
			}
			if (lastAirDate == null) {
				lastAirDate = 9999;
			}
			if (year < firstAirDate || year > lastAirDate) {
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

	private static TvDetailsSchema getTvShowFromEpisode(String title, String year, String imdbId) throws IOException {
		Integer yearInt = getInteger(year);
		String titleSimplified = FileUtil.getSimplifiedShowName(title);
		List<Long> tvShowIds = new ArrayList<>();
		if (imdbId != null) {
			FindSchema findResult = CLIENT.find(imdbId, FindExternalSource.IMDB_ID).getResults();
			//look into episode results
			if (!findResult.getTvEpisodeResults().isEmpty()) {
				for (TvEpisodeTypedSchema tvEpisodeTyped : findResult.getTvEpisodeResults()) {
					Long tvShowId = tvEpisodeTyped.getShowId();
					if (tvShowId != null && !tvShowIds.contains(tvShowId)) {
						TvDetailsSchema tvDetails = getMatchingTvShow(tvShowId, titleSimplified, yearInt);
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
						TvDetailsSchema tvDetails = getMatchingTvShow(tvShowId, titleSimplified, yearInt);
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
		if (tvSimpleResultsSchema.getTotalResults() > 0) {
			for (TvSimpleSchema tvSimple : tvSimpleResultsSchema.getResults()) {
				Long tvShowId = tvSimple.getId();
				if (tvShowId != null && !tvShowIds.contains(tvShowId)) {
					TvDetailsSchema tvDetails = getMatchingTvShow(tvShowId, titleSimplified, yearInt);
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

	public static boolean setTvShowForEpisode(final MediaInfo mediaInfo, final long tvShowId) {
		Connection connection = null;
		try {
			Long episode = getLong(mediaInfo.getVideoMetadata().getTVEpisodeNumber());
			Long season = getLong(mediaInfo.getVideoMetadata().getTVSeason());
			if (season == null || episode == null) {
				return false;
			}
			TvDetailsSchema tvDetails = getTvShowInfo(tvShowId);
			TvEpisodeDetailsSchema tvEpisodeDetails = getTvEpisodeInfo(tvShowId, season, episode);
			if (tvDetails != null && tvEpisodeDetails != null && mediaInfo.getFileId() != null) {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection == null) {
					return false;
				}
				setTvShowMetadata(connection, null, mediaInfo.getVideoMetadata(), tvDetails);
				mediaInfo.getVideoMetadata().setSimplifiedMovieOrShowName(mediaInfo.getVideoMetadata().getSeriesMetadata().getSimplifiedTitle());
				setTvEpisodeMetadata(connection, mediaInfo.getFileId(), mediaInfo, tvEpisodeDetails);
				return true;
			}
		} catch (IOException | SQLException ex) {
			LOGGER.trace("Error in setMovieMetadata:", ex);
		} finally {
			MediaDatabase.close(connection);
		}
		return false;
	}

	public static List<TvSimpleSchema> getTvShowsFromEpisode(String title, String year, String lang) throws IOException {
		Integer yearInt = getInteger(year);
		SearchTvEndpoint searchTvEndpoint = CLIENT.search(title).forTvShow();
		if (yearInt != null && yearInt > 0) {
			searchTvEndpoint.setFirstAirDateYear(yearInt);
		}
		if (lang != null) {
			searchTvEndpoint.setLanguage(lang);
		}
		TvSimpleResultsSchema tvSimpleResultsSchema = searchTvEndpoint.getResults();
		return tvSimpleResultsSchema.getResults();
	}

	public static List<MovieShortSchema> getMovies(String title, String year, String lang) throws IOException {
		Integer yearInt = getInteger(year);
		SearchMovieEndpoint searchMovieEndpoint = CLIENT.search(title).forMovie();
		if (yearInt != null && yearInt != 0) {
			searchMovieEndpoint.setYear(yearInt);
		}
		if (lang != null) {
			searchMovieEndpoint.setLanguage(lang);
		}
		MovieShortResultsSchema movieShortResults = searchMovieEndpoint.getResults();
		return movieShortResults.getResults();
	}

	public static boolean setMovieMetadata(final MediaInfo mediaInfo, final Long tmdbId) {
		Connection connection = null;
		try {
			MovieDetailsSchema movieDetails = getMovieInfo(tmdbId, null);
			if (movieDetails != null && mediaInfo.getFileId() != null) {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection == null) {
					return false;
				}
				setMovieMetadata(connection, mediaInfo.getFileId(), mediaInfo, movieDetails);
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
			final String season,
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

	private static VideoMetadataLocalized getTvSeasonMetadataLocalized(final Long tmdbId, final String season, final String language) {
		Long seasonNumber = getLong(season);
		if (seasonNumber == null) {
			return null;
		}
		TvSeasonDetailsSchema tvSeasonDetailsSchema = CLIENT.tvSeason(tmdbId, seasonNumber)
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

	private static VideoMetadataLocalized getTvEpisodeMetadataLocalized(final Long tmdbId, final String season, final String episode, final String language) {
		Long seasonNumber = getLong(season);
		if (seasonNumber == null) {
			return null;
		}
		Long episodeNumber = getLong(episode);
		if (episodeNumber == null) {
			return null;
		}
		TvEpisodeDetailsSchema tvEpisodeDetailsSchema = CLIENT.tvEpisode(tmdbId, seasonNumber, episodeNumber)
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
			long seasonNumber,
			long episodeNumber
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
			String year,
			String imdbID
	) throws IOException {
		Integer yearInt = getInteger(year);
		if (imdbID != null) {
			FindSchema findResult = CLIENT.find(imdbID, FindExternalSource.IMDB_ID).getResults();
			if (!findResult.getMovieResults().isEmpty()) {
				for (MovieTypedSchema movieTyped : findResult.getMovieResults()) {
					Long tmdbId = movieTyped.getId();
					MovieDetailsSchema movieDetails = getMovieInfo(tmdbId, null);
					if (yearInt != null && yearInt != 0) {
						//check for year
						if (getReleaseYears(movieDetails.getReleaseDates()).contains(yearInt)) {
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
		if (yearInt != null && yearInt != 0) {
			searchMovieEndpoint.setYear(yearInt);
		}
		MovieShortResultsSchema movieShortResults = searchMovieEndpoint.getResults();
		if (movieShortResults.getTotalResults() > 0) {
			for (MovieShortSchema movieShort : movieShortResults.getResults()) {
				Long tmdbId = movieShort.getId();
				MovieDetailsSchema movieDetails = getMovieInfo(tmdbId, null);
				if (yearInt != null && yearInt != 0) {
					//check for year
					if (getReleaseYears(movieDetails.getReleaseDates()).contains(yearInt)) {
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

	private static Integer getInteger(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
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
			ConfigurationSchema configurationSchema = CLIENT.configuration().getConfiguration();
			tmdbImageBaseURL = configurationSchema.getImages().getBaseUrl();
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
