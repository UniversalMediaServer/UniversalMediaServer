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
package net.pms.media.metadata;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class keeps track of the metadata of media.
 */
public class MediaVideoMetadata {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaVideoMetadata.class);
	private static final Gson GSON = new Gson();

	/**
	 * Metadata gathered from either the filename or our API.
	 */
	private String imdbID;
	private String year;
	private String tvShowName;
	private String simplifiedTvShowName;
	private String tvSeason;
	private String tvEpisodeNumber;
	private String tvEpisodeName;
	private String tvSeriesStartYear;
	private String extraInformation;
	private boolean isTVEpisode;
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
	private String production;
	private ApiProductionCompanyArray productionCompanies;
	private ApiCountryArray productionCountries;
	private String rated;
	private String rating;
	private ApiRatingSourceArray ratings;
	private String released;
	private Long revenue;
	private TvSeriesMetadata seriesMetadata;
	private String tagline;
	private Long tmdbId;
	private Long tmdbTvId;
	private String version;
	private String votes;

	public String getIMDbID() {
		return imdbID;
	}

	public void setIMDbID(String value) {
		this.imdbID = value;
	}

	public String getYear() {
		return year;
	}

	public void setYear(String value) {
		this.year = value;
	}

	public String getTVSeriesStartYear() {
		return tvSeriesStartYear;
	}

	public void setTVSeriesStartYear(String value) {
		this.tvSeriesStartYear = value;
	}

	public String getMovieOrShowName() {
		return tvShowName;
	}

	public void setMovieOrShowName(String value) {
		this.tvShowName = value;
	}

	public String getSimplifiedMovieOrShowName() {
		return simplifiedTvShowName;
	}

	public void setSimplifiedMovieOrShowName(String value) {
		this.simplifiedTvShowName = value;
	}

	public String getTVSeason() {
		return tvSeason;
	}

	public void setTVSeason(String value) {
		this.tvSeason = value;
	}

	public String getTVEpisodeNumber() {
		return tvEpisodeNumber;
	}

	public String getTVEpisodeNumberUnpadded() {
		if (StringUtils.isNotBlank(tvEpisodeNumber) && tvEpisodeNumber.length() > 1 && tvEpisodeNumber.startsWith("0")) {
			return tvEpisodeNumber.substring(1);
		}
		return tvEpisodeNumber;
	}

	public void setTVEpisodeNumber(String value) {
		this.tvEpisodeNumber = value;
	}

	public String getTVEpisodeName() {
		return tvEpisodeName;
	}

	public void setTVEpisodeName(String value) {
		this.tvEpisodeName = value;
	}

	public boolean isTVEpisode() {
		return isTVEpisode;
	}

	public void setIsTVEpisode(boolean value) {
		this.isTVEpisode = value;
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
		return tagline;
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

}