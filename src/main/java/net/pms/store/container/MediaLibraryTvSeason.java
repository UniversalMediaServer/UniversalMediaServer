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
package net.pms.store.container;

import net.pms.media.video.metadata.ApiSeason;
import net.pms.media.video.metadata.TvSeriesMetadata;
import net.pms.renderers.Renderer;
import org.apache.commons.lang3.StringUtils;

/**
 * @author SurfaceS
 */
public class MediaLibraryTvSeason extends MediaLibraryFolder {

	private final Integer tvSeasonId;
	private TvSeriesMetadata tvSeriesMetadata;
	private String seasonName = "";

	public MediaLibraryTvSeason(Renderer renderer, String i18nName, String tvSeason, String[] sql, int[] expectedOutput) {
		super(renderer, i18nName, sql, expectedOutput, tvSeason);
		this.tvSeasonId = getInteger(tvSeason);
	}

	@Override
	public String getName() {
		return super.getName().concat(getSeasonName());
	}

	/**
	 * Returns the localized display name.
	 *
	 * @return The localized display name.
	 */
	@Override
	public String getLocalizedDisplayName(String lang) {
		return super.getLocalizedDisplayName(lang).concat(getSeasonName());
	}

	private TvSeriesMetadata getTvSeriesMetadata() {
		if (tvSeriesMetadata == null && tvSeasonId != null && getParent() instanceof MediaLibraryTvSeries mediaLibraryTvSeries) {
			tvSeriesMetadata = mediaLibraryTvSeries.getTvSeriesMetadata();
		}
		return tvSeriesMetadata;
	}

	/**
	 * Get the season name.
	 *
	 * Will be possible to localize it when/if we store localized data.
	 *
	 * @return the season name or empty string.
	 */
	private synchronized String getSeasonName() {
		if (seasonName.isEmpty() && getTvSeriesMetadata() != null && tvSeriesMetadata.getSeasons() != null && tvSeasonId != null) {
			for (ApiSeason season : tvSeriesMetadata.getSeasons()) {
				if (season.getSeasonNumber() == tvSeasonId) {
					if (!StringUtils.isBlank(season.getName()) &&
							!season.getName().equalsIgnoreCase("Season " + season.getSeasonNumber())) {
						seasonName = ": " + season.getName();
					}
					break;
				}
			}
		}
		return seasonName;
	}

	private static Integer getInteger(String value) {
		try {
			return Integer.valueOf(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

}
