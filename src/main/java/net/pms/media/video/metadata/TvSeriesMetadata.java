/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; apiVersion 2 of the License only.
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
import net.pms.store.ThumbnailSource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the TV series metadata of media.
 */
public class TvSeriesMetadata {
	private static final Logger LOGGER = LoggerFactory.getLogger(TvSeriesMetadata.class);
	private static final Gson GSON = new Gson();
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * Metadata gathered from either the filename or our API.
	 */
	private ApiStringArray actors;
	private String awards;
	private ApiStringArray countries;
	private ApiPersonCreditedArray createdBy;
	private ApiCredits credits;
	private ApiStringArray directors;
	private Integer endYear;
	private ApiExternalIDs externalIDs;
	private LocalDate firstAirDate;
	private ApiStringArray genres;
	private String homepage;
	private ApiImages images;
	private String imdbID;
	private Boolean inProduction;
	private ApiStringArray languages;
	private LocalDate lastAirDate;
	private ApiNetworkArray networks;
	private Double numberOfEpisodes;
	private Double numberOfSeasons;
	private String originalLanguage;
	private String originalTitle;
	private ApiStringArray originCountry;
	private String overview;
	private String poster;
	private ApiProductionCompanyArray productionCompanies;
	private ApiCountryArray productionCountries;
	private String rated;
	private Double rating;
	private ApiRatingSourceArray ratings;
	private LocalDate released;
	private ApiSeasonArray seasons;
	private String seriesType;
	private ApiLanguageArray spokenLanguages;
	private Integer startYear;
	private String status;
	private String tagline;
	private String title;
	private Long tmdbId;
	private Double totalSeasons;
	private String apiVersion;
	private String votes;
	private Long thumbnailId;
	private ThumbnailSource thumbnailSource = ThumbnailSource.UNKNOWN;
	private Long tvSeriesId;
	private Map<String, VideoMetadataLocalized> translations;

	public Long getTvSeriesId() {
		return tvSeriesId;
	}

	public void setTvSeriesId(Long value) {
		tvSeriesId = value;
	}

	public ApiStringArray getActors() {
		return actors;
	}

	public void setActors(ApiStringArray value) {
		this.actors = value;
	}

	public String getApiVersion() {
		return apiVersion;
	}

	public void setApiVersion(String value) {
		this.apiVersion = value;
	}

	public String getAwards() {
		return awards;
	}

	public void setAwards(String value) {
		this.awards = value;
	}

	public ApiStringArray getCountries() {
		return countries;
	}

	public void setCountries(ApiStringArray value) {
		this.countries = value;
	}

	public ApiPersonCreditedArray getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(ApiPersonCreditedArray value) {
		this.createdBy = value;
	}

	public void setCreatedBy(String value) {
		try {
			this.createdBy = GSON.fromJson(value, ApiPersonCreditedArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing created by: {}", e.getMessage());
			this.createdBy = null;
		}
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

	public Integer getEndYear() {
		return endYear;
	}

	public void setEndYear(Integer value) {
		this.endYear = value;
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

	public LocalDate getFirstAirDate() {
		return firstAirDate;
	}

	public void setFirstAirDate(LocalDate value) {
		this.firstAirDate = value;
	}

	public void setFirstAirDate(String value) {
		LocalDate localDate = null;
		if (value != null) {
			try {
				localDate = LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
			} catch (DateTimeParseException | IllegalArgumentException | NullPointerException e) {
				LOGGER.trace("String \"{}\" cannot converts to LocalDate", value);
			}
		}
		this.firstAirDate = localDate;
	}

	public ApiStringArray getGenres() {
		return genres;
	}

	public void setGenres(ApiStringArray value) {
		this.genres = value;
	}

	public String getHomepage() {
		return homepage;
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

	public String getIMDbID() {
		return imdbID;
	}

	public void setIMDbID(String value) {
		this.imdbID = value;
	}

	public Boolean isInProduction() {
		return inProduction;
	}

	public void setInProduction(Boolean value) {
		this.inProduction = value;
	}

	public ApiStringArray getLanguages() {
		return languages;
	}

	public void setLanguages(ApiStringArray value) {
		this.languages = value;
	}

	public void setLanguages(String value) {
		try {
			this.languages = GSON.fromJson(value, ApiStringArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing languages: {}", e.getMessage());
			this.languages = null;
		}
	}

	public LocalDate getLastAirDate() {
		return lastAirDate;
	}

	public void setLastAirDate(LocalDate value) {
		this.lastAirDate = value;
	}

	public void setLastAirDate(String value) {
		LocalDate localDate = null;
		if (value != null) {
			try {
				localDate = LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
			} catch (DateTimeParseException | IllegalArgumentException | NullPointerException e) {
				LOGGER.trace("String \"{}\" cannot converts to LocalDate", value);
			}
		}
		this.lastAirDate = localDate;
	}

	public ApiNetworkArray getNetworks() {
		return networks;
	}

	public void setNetworks(ApiNetworkArray value) {
		this.networks = value;
	}

	public void setNetworks(String value) {
		try {
			this.networks = GSON.fromJson(value, ApiNetworkArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing networks: {}", e.getMessage());
			this.networks = null;
		}
	}

	public Double getNumberOfEpisodes() {
		return numberOfEpisodes;
	}

	public void setNumberOfEpisodes(Double value) {
		this.numberOfEpisodes = value;
	}

	public Double getNumberOfSeasons() {
		return numberOfSeasons;
	}

	public void setNumberOfSeasons(Double value) {
		this.numberOfSeasons = value;
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

	public ApiStringArray getOriginCountry() {
		return originCountry;
	}

	public void setOriginCountry(ApiStringArray value) {
		this.originCountry = value;
	}

	public void setOriginCountry(String value) {
		try {
			this.originCountry = GSON.fromJson(value, ApiStringArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing origin country: {}", e.getMessage());
			this.originCountry = null;
		}
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String value) {
		this.overview = value;
	}

	public String getPoster() {
		return poster;
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

	public ApiSeasonArray getSeasons() {
		return seasons;
	}

	public void setSeasons(ApiSeasonArray value) {
		this.seasons = value;
	}

	public void setSeasons(String value) {
		try {
			this.seasons = GSON.fromJson(value, ApiSeasonArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing seasons: {}", e.getMessage());
			this.seasons = null;
		}
	}

	public String getSeriesType() {
		return seriesType;
	}

	public void setSeriesType(String value) {
		this.seriesType = value;
	}

	public ApiLanguageArray getSpokenLanguages() {
		return spokenLanguages;
	}

	public void setSpokenLanguages(ApiLanguageArray value) {
		this.spokenLanguages = value;
	}

	public void setSpokenLanguages(String value) {
		try {
			this.spokenLanguages = GSON.fromJson(value, ApiLanguageArray.class);
		} catch (JsonSyntaxException e) {
			LOGGER.error("Error in parsing spoken languages: {}", e.getMessage());
			this.spokenLanguages = null;
		}
	}

	public Integer getStartYear() {
		return startYear;
	}

	public void setStartYear(Integer value) {
		this.startYear = value;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String value) {
		this.status = value;
	}

	public String getTagline() {
		return tagline;
	}

	public void setTagline(String value) {
		this.tagline = value;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String value) {
		this.title = value;
	}

	public Long getTmdbId() {
		return tmdbId;
	}

	public void setTmdbId(Long value) {
		this.tmdbId = value;
	}

	public Double getTotalSeasons() {
		return totalSeasons;
	}

	public void setTotalSeasons(Double value) {
		this.totalSeasons = value;
	}

	public Long getThumbnailId() {
		return thumbnailId;
	}

	public void setThumbnailId(Long value) {
		this.thumbnailId = value;
	}

	public ThumbnailSource getThumbnailSource() {
		return thumbnailSource;
	}

	public void setThumbnailSource(ThumbnailSource value) {
		this.thumbnailSource = value;
	}

	public void setThumbnailSource(String value) {
		this.thumbnailSource = ThumbnailSource.valueOfName(value);
	}

	public String getVotes() {
		return votes;
	}

	public void setVotes(String value) {
		this.votes = value;
	}

	public void setTranslations(Map<String, VideoMetadataLocalized> value) {
		this.translations = value;
	}

	public void ensureHavingTranslation(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		if (lang != null && !"en-us".equals(lang) && !hasTranslation(lang) && tvSeriesId != null && tvSeriesId > -1) {
			VideoMetadataLocalized loc = MediaTableVideoMetadataLocalized.getVideoMetadataLocalized(tvSeriesId, true, lang, imdbID, "tv", tmdbId, null, null);
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

	public void update(TvSeriesMetadata tvSeriesMetadata) {
		setActors(tvSeriesMetadata.getActors());
		setApiVersion(tvSeriesMetadata.getApiVersion());
		setAwards(tvSeriesMetadata.getAwards());
		setCountries(tvSeriesMetadata.getCountries());
		setCreatedBy(tvSeriesMetadata.getCreatedBy());
		setCredits(tvSeriesMetadata.getCredits());
		setDirectors(tvSeriesMetadata.getDirectors());
		setEndYear(tvSeriesMetadata.getEndYear());
		setExternalIDs(tvSeriesMetadata.getExternalIDs());
		setFirstAirDate(tvSeriesMetadata.getFirstAirDate());
		setGenres(tvSeriesMetadata.getGenres());
		setHomepage(tvSeriesMetadata.getHomepage());
		setIMDbID(tvSeriesMetadata.getIMDbID());
		setImages(tvSeriesMetadata.getImages());
		setLanguages(tvSeriesMetadata.getLanguages());
		setLastAirDate(tvSeriesMetadata.getLastAirDate());
		setNetworks(tvSeriesMetadata.getNetworks());
		setNumberOfEpisodes(tvSeriesMetadata.getNumberOfEpisodes());
		setNumberOfSeasons(tvSeriesMetadata.getNumberOfSeasons());
		setOriginCountry(tvSeriesMetadata.getOriginCountry());
		setOriginalLanguage(tvSeriesMetadata.getOriginalLanguage());
		setOriginalTitle(tvSeriesMetadata.getOriginalTitle());
		setOverview(tvSeriesMetadata.getOverview());
		setPoster(tvSeriesMetadata.getPoster());
		setProductionCompanies(tvSeriesMetadata.getProductionCompanies());
		setProductionCountries(tvSeriesMetadata.getProductionCountries());
		setRated(tvSeriesMetadata.getRated());
		setRating(tvSeriesMetadata.getRating());
		setRatings(tvSeriesMetadata.getRatings());
		setSeasons(tvSeriesMetadata.getSeasons());
		setSeriesType(tvSeriesMetadata.getSeriesType());
		setSpokenLanguages(tvSeriesMetadata.getSpokenLanguages());
		setStartYear(tvSeriesMetadata.getStartYear());
		setStatus(tvSeriesMetadata.getStatus());
		setTagline(tvSeriesMetadata.getTagline());
		setThumbnailId(tvSeriesMetadata.getThumbnailId());
		setThumbnailSource(tvSeriesMetadata.getThumbnailSource());
		setTitle(tvSeriesMetadata.getTitle());
		setTmdbId(tvSeriesMetadata.getTmdbId());
		setTotalSeasons(tvSeriesMetadata.getTotalSeasons());
		setTranslations(null);
		setVotes(tvSeriesMetadata.getVotes());
	}

	public JsonObject asJsonObject(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		ensureHavingTranslation(lang);
		JsonObject result = new JsonObject();
		result.add("actors", GSON.toJsonTree(actors));
		result.addProperty("awards", awards);
		result.add("countries", GSON.toJsonTree(countries));
		result.add("directors", GSON.toJsonTree(directors));
		result.addProperty("endYear", endYear);
		result.add("externalIDs", GSON.toJsonTree(externalIDs));
		if (firstAirDate != null) {
			result.addProperty("firstAirDate", firstAirDate.toString());
		}
		result.add("genres", GSON.toJsonTree(genres));
		result.addProperty("homepage", getHomepage(lang));
		result.add("images", GSON.toJsonTree(images));
		result.addProperty("imdbID", imdbID);
		result.addProperty("inProduction", inProduction);
		result.add("languages", GSON.toJsonTree(languages));
		if (lastAirDate != null) {
			result.addProperty("lastAirDate", lastAirDate.toString());
		}
		result.addProperty("mediaType", "tv");
		result.addProperty("numberOfEpisodes", numberOfEpisodes);
		result.addProperty("numberOfSeasons", numberOfSeasons);
		result.add("originCountry", GSON.toJsonTree(originCountry));
		if (originalTitle != null && !originalTitle.equals(getTitle(lang))) {
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
		result.add("seasons", GSON.toJsonTree(seasons));
		result.addProperty("seriesType", seriesType);
		result.add("spokenLanguages", GSON.toJsonTree(spokenLanguages));
		result.addProperty("startYear", startYear);
		result.addProperty("status", status);
		result.addProperty("tagline", getTagline(lang));
		result.addProperty("tmdbID", tmdbId);
		result.addProperty("title", getTitle(lang));
		result.addProperty("totalSeasons", totalSeasons);
		result.addProperty("votes", votes);
		return result;
	}

}
