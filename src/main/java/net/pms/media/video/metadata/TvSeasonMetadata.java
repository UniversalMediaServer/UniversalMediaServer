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
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaTableTvSeasonMetadataLocalized;
import net.pms.external.tmdb.TMDB;
import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of the TV season metadata of media.
 */
public class TvSeasonMetadata {

	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final Long tvSeriesId;
	private final Long tmdbTvId;
	private final ApiSeason apiSeason;

	private Map<String, TvSeasonMetadataLocalized> translations;

	public TvSeasonMetadata(Long tvSeriesId, Long tmdbTvId, ApiSeason apiSeason) {
		this.tvSeriesId = tvSeriesId;
		this.tmdbTvId = tmdbTvId;
		this.apiSeason = apiSeason;
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
		lang = CONFIGURATION.getTranslationLanguage(lang);
		if (lang != null && !"en-us".equals(lang) && !hasTranslation(lang) && tmdbTvId != null && tmdbTvId > -1) {
			TvSeasonMetadataLocalized loc = MediaTableTvSeasonMetadataLocalized.getTvSeasonMetadataLocalized(tvSeriesId, lang, tmdbTvId, apiSeason);
			if (loc != null) {
				addTranslation(lang, loc);
			}
		}
	}

	private void addTranslation(String lang, TvSeasonMetadataLocalized value) {
		if (lang == null || value == null) {
			return;
		}
		if (this.translations == null) {
			this.translations = new HashMap<>();
		}
		this.translations.put(lang.toLowerCase(), value);
	}

	private boolean hasTranslation(String lang) {
		return this.translations != null && this.translations.containsKey(lang.toLowerCase());
	}

	private TvSeasonMetadataLocalized getTranslation(String lang) {
		lang = CONFIGURATION.getTranslationLanguage(lang);
		if (lang != null && hasTranslation(lang)) {
			return this.translations.get(lang);
		}
		return null;
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
