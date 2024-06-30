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
package net.pms.media.video.metadata;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaTableVideoMetadataLocalized;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the metadata of video media.
 */
public class MediaVideoMetadata {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaVideoMetadata.class);
	private static final Gson GSON = new Gson();
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * Metadata gathered from either the filename or our API.
	 */
	private Long fileId;
	private String imdbID;
	private Integer year;
	private String title;
	private Long tvSeriesId;
	private Integer tvSeason;
	private String tvEpisodeNumber;
	private String extraInformation;
	private boolean isTvEpisode;
	/**
	 * Metadata gathered from our API.
	 */
	private ApiStringArray actors;
	private String awards;
	private Long budget;
	private ApiStringArray countries;
	private ApiCredits credits;
	private ApiStringArray directors;
	private ApiExternalIDs externalIDs;
	private ApiStringArray genres;
	private String homepage;
	private ApiImages images;
	private String originalLanguage;
	private String originalTitle;
	private String overview;
	private String poster;
	private ApiProductionCompanyArray productionCompanies;
	private ApiCountryArray productionCountries;
	private String rated;
	private Double rating;
	private ApiRatingSourceArray ratings;
	private LocalDate released;
	private Long revenue;
	private TvSeriesMetadata seriesMetadata;
	private String tagline;
	private Long tmdbId;
	private Long tmdbTvId;
	private String version;
	private String votes;
	private Map<String, VideoMetadataLocalized> translations;

	public Long getFileId() {
		return fileId;
	}

	public void setFileId(Long value) {
		this.fileId = value;
	}

	public String getIMDbID() {
		return imdbID;
	}

	public void setIMDbID(String value) {
		this.imdbID = value;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(Integer value) {
		this.year = value;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	public Long getTvSeriesId() {
		return tvSeriesId;
	}

	public void setTvSeriesId(Long value) {
		this.tvSeriesId = value;
	}

	public Integer getTvSeason() {
		return tvSeason;
	}

	public void setTvSeason(Integer value) {
		this.tvSeason = value;
	}

	public String getTvEpisodeNumber() {
		return tvEpisodeNumber;
	}

	public String getTvEpisodeNumberUnpadded() {
		if (StringUtils.isNotBlank(tvEpisodeNumber) && tvEpisodeNumber.length() > 1 && tvEpisodeNumber.startsWith("0")) {
			return tvEpisodeNumber.substring(1);
		}
		return tvEpisodeNumber;
	}

	public Integer getFirstTvEpisodeNumber() {
		if (tvEpisodeNumber != null) {
			String firstEpisode = tvEpisodeNumber;
			if (firstEpisode.contains("-")) {
				firstEpisode = firstEpisode.substring(0, firstEpisode.indexOf("-"));
			}
			Integer result = null;
			if (firstEpisode != null) {
				try {
					result = Integer.valueOf(firstEpisode);
				} catch (NumberFormatException e) {
					//nothing to do
				}
			}
			return result;
		}
		return null;
	}

	public void setTvEpisodeNumber(String value) {
		this.tvEpisodeNumber = value;
	}

	public String getTvEpisodeName() {
		if (isTvEpisode) {
			return getTitle(null);
		}
		return null;
	}

	public String getTvEpisodeName(String lang) {
		if (isTvEpisode) {
			return getTitle(lang);
		}
		return null;
	}

	public boolean isTvEpisode() {
		return isTvEpisode;
	}

	public void setIsTvEpisode(boolean value) {
		this.isTvEpisode = value;
	}

	public Integer getTvSeriesStartYear() {
		if (isTvEpisode && seriesMetadata != null) {
			return seriesMetadata.getStartYear();
		}
		return null;
	}

	public String getTvSeriesTitle() {
		if (isTvEpisode && seriesMetadata != null) {
			return seriesMetadata.getTitle(null);
		}
		return null;
	}

	public String getTvSeriesTitle(String lang) {
		if (isTvEpisode && seriesMetadata != null) {
			return seriesMetadata.getTitle(lang);
		}
		return null;
	}

	/**
	 * Any extra information like movie edition or whether it is a
	 * sample video.
	 *
	 * Example: "(Director's Cut) (Sample)"
	 * @return
	 */
	public String getExtraInformation() {
		return extraInformation;
	}

	/*
	 * Any extra information like movie edition or whether it is a
	 * sample video.
	 *
	 * Example: "(Director's Cut) (Sample)"
	 */
	public void setExtraInformation(String value) {
		this.extraInformation = value;
	}

	public ApiStringArray getActors() {
		return actors;
	}

	public void setActors(ApiStringArray value) {
		this.actors = value;
	}

	public void setActors(String value) {
		try {
			this.actors = GSON.fromJson(value, ApiStringArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing actors: {}", e.getMessage());
			this.actors = null;
		}
	}

	public String getAwards() {
		return awards;
	}

	public void setAwards(String value) {
		this.awards = value;
	}

	public Long getBudget() {
		return budget;
	}

	public void setBudget(Long value) {
		this.budget = value;
	}

	public ApiStringArray getCountries() {
		return countries;
	}

	public void setCountries(ApiStringArray value) {
		this.countries = value;
	}

	public ApiCredits getCredits() {
		return credits;
	}

	public void setCredits(ApiCredits value) {
		this.credits = value;
	}

	public void setCredits(String value) {
		try {
			this.credits = GSON.fromJson(value, ApiCredits.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing credits: {}", e.getMessage());
			this.credits = null;
		}
	}

	public ApiStringArray getDirectors() {
		return directors;
	}

	public void setDirectors(ApiStringArray value) {
		this.directors = value;
	}

	public void setDirectors(String value) {
		try {
			this.directors = GSON.fromJson(value, ApiStringArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing directors: {}", e.getMessage());
			this.directors = null;
		}
	}

	public ApiExternalIDs getExternalIDs() {
		return externalIDs;
	}

	public void setExternalIDs(ApiExternalIDs value) {
		this.externalIDs = value;
	}

	public void setExternalIDs(String value) {
		try {
			this.externalIDs = GSON.fromJson(value, ApiExternalIDs.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing ExternalIDs: {}", e.getMessage());
			this.externalIDs = null;
		}
	}

	public ApiStringArray getGenres() {
		return genres;
	}

	public void setGenres(ApiStringArray value) {
		this.genres = value;
	}

	public void setGenres(String value) {
		try {
			this.genres = GSON.fromJson(value, ApiStringArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing genres: {}", e.getMessage());
			this.genres = null;
		}
	}

	public String getHomepage() {
		return getHomepage(null);
	}

	public void setHomepage(String value) {
		this.homepage = value;
	}

	public ApiImages getImages() {
		return images;
	}

	public void setImages(ApiImages value) {
		this.images = value;
	}

	public void setImages(String value) {
		try {
			this.images = GSON.fromJson(value, ApiImages.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing Images: {}", e.getMessage());
			this.images = null;
		}
	}

	public String getOriginalLanguage() {
		return originalLanguage;
	}

	public void setOriginalLanguage(String value) {
		this.originalLanguage = value;
	}

	public String getOriginalTitle() {
		return originalTitle;
	}

	public void setOriginalTitle(String value) {
		this.originalTitle = value;
	}

	public String getOverview() {
		return getOverview(null);
	}

	public void setOverview(String value) {
		this.overview = value;
	}

	public String getPoster() {
		return getPoster(null);
	}

	public void setPoster(String value) {
		this.poster = value;
	}

	public ApiProductionCompanyArray getProductionCompanies() {
		return productionCompanies;
	}

	public void setProductionCompanies(ApiProductionCompanyArray value) {
		this.productionCompanies = value;
	}

	public void setProductionCompanies(String value) {
		try {
			this.productionCompanies = GSON.fromJson(value, ApiProductionCompanyArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing production companies: {}", e.getMessage());
			this.productionCompanies = null;
		}
	}

	public ApiCountryArray getProductionCountries() {
		return productionCountries;
	}

	public void setProductionCountries(ApiCountryArray value) {
		this.productionCountries = value;
	}

	public void setProductionCountries(String value) {
		try {
			this.productionCountries = GSON.fromJson(value, ApiCountryArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing production countries: {}", e.getMessage());
			this.productionCountries = null;
		}
	}

	public String getRated() {
		return rated;
	}

	public void setRated(String value) {
		this.rated = value;
	}

	public Double getRating() {
		return rating;
	}

	public void setRating(Double value) {
		this.rating = value;
	}

	public ApiRatingSourceArray getRatings() {
		return ratings;
	}

	public void setRatings(ApiRatingSourceArray value) {
		this.ratings = value;
	}

	public void setRatings(String value) {
		try {
			this.ratings = GSON.fromJson(value, ApiRatingSourceArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing ratings: {}", e.getMessage());
			this.ratings = null;
		}
	}

	public LocalDate getReleased() {
		return released;
	}

	public void setReleased(LocalDate value) {
		this.released = value;
	}

	public void setReleased(String value) {
		LocalDate localDate = null;
		if (value != null) {
			try {
				localDate = LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
			} catch (DateTimeParseException | IllegalArgumentException | NullPointerException e) {
				LOGGER.trace("String \"{}\" cannot converts to LocalDate", value);
			}
		}
		this.released = localDate;
	}

	public Long getRevenue() {
		return revenue;
	}

	public void setRevenue(Long value) {
		this.revenue = value;
	}

	public TvSeriesMetadata getSeriesMetadata() {
		return seriesMetadata;
	}

	public void setSeriesMetadata(TvSeriesMetadata value) {
		this.seriesMetadata = value;
	}

	public String getTagline() {
		return getTagline(null);
	}

	public void setTagline(String value) {
		this.tagline = value;
	}

	public Long getTmdbId() {
		return tmdbId;
	}

	public void setTmdbId(Long value) {
		this.tmdbId = value;
	}

	public Long getTmdbTvId() {
		return tmdbTvId;
	}

	public void setTmdbTvId(Long value) {
		this.tmdbTvId = value;
	}

	public String getVotes() {
		return votes;
	}

	public void setVotes(String value) {
		this.votes = value;
	}

	public String getVersion() {
		return version;
	}

	public void setApiVersion(String value) {
		this.version = value;
	}

	public String getMovieOrShowName() {
		if (isTvEpisode && seriesMetadata != null) {
			return seriesMetadata.getTitle();
		}
		return title;
	}

	public String getMovieOrShowName(String lang) {
		if (isTvEpisode && seriesMetadata != null) {
			return seriesMetadata.getTitle(lang);
		}
		return getTitle(lang);
	}

	public Integer getMovieOrShowYear() {
		if (isTvEpisode && seriesMetadata != null) {
			return seriesMetadata.getStartYear();
		}
		return year;
	}

	public void setTranslations(Map<String, VideoMetadataLocalized> value) {
		this.translations = value;
	}

	public void ensureHavingTranslation(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		if (lang != null && !"en-us".equals(lang) && !hasTranslation(lang) && fileId != null && fileId > -1) {
			VideoMetadataLocalized loc;
			if (isTvEpisode) {
				loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(fileId, false, lang, imdbID, "tv_episode", tmdbTvId, tvSeason, tvEpisodeNumber);
			} else {
				loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(fileId, false, lang, imdbID, "movie", tmdbId, null, null);
			}
			if (loc != null) {
				addTranslation(lang, loc);
			}
		}
	}

	private void addTranslation(String lang, VideoMetadataLocalized value) {
		if (lang == null || value == null) {
			return;
		}
		if (this.translations == null)  {
			this.translations = new HashMap<>();
		}
		this.translations.put(lang.toLowerCase(), value);
	}

	private boolean hasTranslation(String lang) {
		return this.translations != null && this.translations.containsKey(lang.toLowerCase());
	}

	private VideoMetadataLocalized getTranslation(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		if (lang != null && hasTranslation(lang)) {
			return this.translations.get(lang);
		}
		return null;
	}

	public String getHomepage(String lang) {
		VideoMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getHomepage())) {
			return translation.getHomepage();
		}
		return homepage;
	}

	public String getOverview(String lang) {
		VideoMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getOverview())) {
			return translation.getOverview();
		}
		return overview;
	}

	public String getPoster(String lang) {
		VideoMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getPoster())) {
			return translation.getPoster();
		}
		return poster;
	}

	public String getTagline(String lang) {
		VideoMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getTagline())) {
			return translation.getTagline();
		}
		return tagline;
	}

	public String getTitle(String lang) {
		VideoMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getTitle())) {
			return translation.getTitle();
		}
		return title;
	}

	public JsonObject asJsonObject(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		ensureHavingTranslation(lang);
		JsonObject result = new JsonObject();
		result.addProperty("imdbID", imdbID);
		result.add("actors", GSON.toJsonTree(actors));
		result.addProperty("awards", awards);
		result.add("countries", GSON.toJsonTree(countries));
		result.add("directors", GSON.toJsonTree(directors));
		result.add("externalIDs", GSON.toJsonTree(externalIDs));
		result.add("genres", GSON.toJsonTree(genres));
		result.addProperty("homepage", getHomepage(lang));
		result.add("images", GSON.toJsonTree(images));
		result.addProperty("mediaType", isTvEpisode ? "tv_episode" : "movie");
		if (!isTvEpisode && originalTitle != null && !originalTitle.equals(getMovieOrShowName(lang))) {
			result.addProperty("originalLanguage", originalLanguage);
			result.addProperty("originalTitle", originalTitle);
		}
		result.addProperty("overview", getOverview(lang));
		result.addProperty("poster", getPoster(lang));
		result.addProperty("rated", rated);
		result.addProperty("rating", rating);
		result.add("ratings", GSON.toJsonTree(ratings));
		if (released != null) {
			result.addProperty("released", released.toString());
		}
		result.addProperty("tagline", getTagline(lang));
		result.addProperty("tmdbID", tmdbId);
		result.addProperty("tmdbTvID", tmdbTvId);
		result.addProperty("tvEpisode", tvEpisodeNumber);
		result.addProperty("tvSeason", tvSeason);
		result.addProperty("votes", votes);
		if (isTvEpisode && seriesMetadata != null) {
			result.add("seriesImages", GSON.toJsonTree(seriesMetadata.getImages()));
		}
		return result;
	}

}
