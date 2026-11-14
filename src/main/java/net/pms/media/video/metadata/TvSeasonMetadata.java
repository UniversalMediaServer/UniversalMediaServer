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

import com.google.gson.JsonObject;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaTableTvSeasonMetadataLocalized;
import net.pms.external.tmdb.TMDB;
import net.pms.store.container.MediaLibraryTvSeries;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of the TV season metadata of media.
 */
public class TvSeasonMetadata {

	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final Long tvSeriesId;
	private final Long tmdbTvId;
	private final ApiSeason apiSeason;

	private final BackgroundTranslations<TvSeasonMetadataLocalized> translations;

	public TvSeasonMetadata(Long tvSeriesId, Long tmdbTvId, ApiSeason apiSeason) {
		this.tvSeriesId = tvSeriesId;
		this.tmdbTvId = tmdbTvId;
		this.apiSeason = apiSeason;
		//season folders are children of the series container
		this.translations = new BackgroundTranslations<>(
			() -> tvSeriesId != null && tvSeriesId > -1 ?
				new TranslationStoreRefresh.Target(null, MediaLibraryTvSeries.getSystemName(tvSeriesId)) : null);
	}

	public Long getTvSeriesId() {
		return tvSeriesId;
	}

	public String getAirDate() {
		return apiSeason.getAirDate();
	}

	public int getEpisodeCount() {
		return apiSeason.getEpisodeCount();
	}

	public int getTmdbId() {
		return apiSeason.getId();
	}

	public String getName(String lang) {
		TvSeasonMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getName())) {
			return translation.getName();
		}
		return apiSeason.getName();
	}

	public String getOverview(String lang) {
		TvSeasonMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getOverview())) {
			return translation.getOverview();
		}
		return apiSeason.getOverview();
	}

	public String getPoster(String lang) {
		TvSeasonMetadataLocalized translation = getTranslation(lang);
		if (translation != null && StringUtils.isNotBlank(translation.getPoster())) {
			return translation.getPoster();
		}
		return TMDB.getPosterUrl(getPosterPath());
	}

	public String getPosterPath() {
		return apiSeason.getPosterPath();
	}

	public int getSeasonNumber() {
		return apiSeason.getSeasonNumber();
	}

	public void ensureHavingTranslation(String lang) {
		String language = CONFIGURATION.getTranslationLanguage(lang);
		if (language != null && !"en-us".equals(language) && tmdbTvId != null && tmdbTvId > -1) {
			translations.request(language, () -> MediaTableTvSeasonMetadataLocalized.getTvSeasonMetadataLocalized(tvSeriesId, language, tmdbTvId, apiSeason));
		}
	}

	public long getTranslationVersion() {
		return translations.version();
	}

	private TvSeasonMetadataLocalized getTranslation(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		return translations.get(lang);
	}

	public JsonObject asJsonObject(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		ensureHavingTranslation(lang);
		JsonObject result = new JsonObject();
		result.addProperty("airDate", getAirDate());
		result.addProperty("episodeCount", getEpisodeCount());
		result.addProperty("title", getName(lang));
		result.addProperty("overview", getOverview(lang));
		result.addProperty("poster", getPoster(lang));
		result.addProperty("tvSeason", getSeasonNumber());
		result.addProperty("tmdbID", getTmdbId());
		result.addProperty("tmdbTvID", tmdbTvId);
		return result;
	}

}
