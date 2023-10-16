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

import com.google.gson.JsonArray;
import java.io.File;
import java.io.IOException;
import net.pms.external.umsapi.APIUtils;
import net.pms.media.MediaInfo;
import net.pms.media.video.metadata.VideoMetadataLocalized;

/**
 * This class contains utility methods for TMDB to get the Metadata info.
 *
 * @author SurfaceS
 */
public class TMDB {

	/**
	 * This class is not meant to be instantiated.
	 */
	private TMDB() {
	}

	public static boolean isReady() {
		return false;
	}

	/**
	 * Enhances existing MediaInfo attached to this media by querying TMDB.
	 *
	 * @param file
	 * @param mediaInfo MediaInfo
	 */
	public static void backgroundLookupAndAddMetadata(final File file, final MediaInfo mediaInfo) {
		//fallback to UMS API
		APIUtils.backgroundLookupAndAddMetadata(file, mediaInfo);
	}

	public static boolean updateTvShowForEpisode(final MediaInfo mediaInfo, final long tvShowId) {
		return false;
	}

	public static boolean updateTvShowMetadata(final Long tvSeriesId, final long tvShowId) {
		return false;
	}

	public static boolean updateMovieMetadata(final MediaInfo mediaInfo, final Long tmdbId) {
		return false;
	}

	public static JsonArray getTvShowsFromEpisode(String title, String year, String lang, Long currentId) throws IOException {
		return new JsonArray();
	}

	public static JsonArray getMovies(String title, String year, String lang, Long currentId) throws IOException {
		return new JsonArray();
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
		//fallback to UMS API
		return APIUtils.getVideoMetadataLocalizedFromImdb(language, mediaType, imdbId, tmdbId, season, episode);
	}

	public static String getTmdbImageBaseURL() {
		//fallback to UMS API
		return APIUtils.getApiImageBaseURL();
	}

}
