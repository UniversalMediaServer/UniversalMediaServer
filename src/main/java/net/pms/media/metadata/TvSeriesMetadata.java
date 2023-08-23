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
package net.pms.media.metadata;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the TV series metadata of media.
 */
public class TvSeriesMetadata {
	private static final Logger LOGGER = LoggerFactory.getLogger(TvSeriesMetadata.class);
	private static final Gson GSON = new Gson();

	/**
	 * Metadata gathered from either the filename or our API.
	 */
	private ApiStringArray actors;
	private String awards;
	private ApiStringArray countries;
	private ApiPersonCreditedArray createdBy;
	private ApiCredits credits;
	private ApiStringArray directors;
	private String endYear;
	private ApiExternalIDs externalIDs;
	private String firstAirDate;
	private ApiStringArray genres;
	private String homepage;
	private ApiImages images;
	private String imdbID;
	private Boolean inProduction;
	private ApiStringArray languages;
	private String lastAirDate;
	private ApiNetworkArray networks;
	private Double numberOfEpisodes;
	private Double numberOfSeasons;
	private String originalLanguage;
	private String originalTitle;
	private ApiStringArray originCountry;
	private String overview;
	private String poster;
	private String production;
	private ApiProductionCompanyArray productionCompanies;
	private ApiCountryArray productionCountries;
	private String rated;
	private String rating;
	private ApiRatingSourceArray ratings;
	private String released;
	private ApiSeasonArray seasons;
	private String seriesType;
	private String simplifiedTitle;
	private ApiLanguageArray spokenLanguages;
	private String startYear;
	private String status;
	private String tagline;
	private String title;
	private Long tmdbId;
	private Double totalSeasons;
	private String apiVersion;
	private String votes;

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

	public String getEndYear() {
		return endYear;
	}

	public void setEndYear(String value) {
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

	public String getFirstAirDate() {
		return firstAirDate;
	}

	public void setFirstAirDate(String value) {
		this.firstAirDate = value;
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

	public String getLastAirDate() {
		return lastAirDate;
	}

	public void setLastAirDate(String value) {
		this.lastAirDate = value;
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

	public String getPoster() {
		return poster;
	}

	public void setPoster(String value) {
		this.poster = value;
	}

	public String getProduction() {
		return production;
	}

	public void setProduction(String value) {
		this.production = value;
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

	public String getOverview() {
		return overview;
	}

	public void setOverview(String value) {
		this.overview = value;
	}

	public String getRated() {
		return rated;
	}

	public void setRated(String value) {
		this.rated = value;
	}

	public String getRating() {
		return rating;
	}

	public void setRating(String value) {
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

	public String getReleased() {
		return released;
	}

	public void setReleased(String value) {
		this.released = value;
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

	public String getSimplifiedTitle() {
		return simplifiedTitle;
	}

	public void setSimplifiedTitle(String value) {
		this.simplifiedTitle = value;
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

	public String getStartYear() {
		return startYear;
	}

	public void setStartYear(String value) {
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

	public String getVotes() {
		return votes;
	}

	public void setVotes(String value) {
		this.votes = value;
	}

}