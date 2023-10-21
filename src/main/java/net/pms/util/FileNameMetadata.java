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
package net.pms.util;

public class FileNameMetadata {

	private final String movieOrShowName;
	private final Integer year;
	private final String extraInformation;
	private final Integer tvSeasonNumber;
	private final String tvEpisodeName;
	private final String tvEpisodeNumber;

	public FileNameMetadata() {
		this(null, null, null, null, null, null);
	}

	public FileNameMetadata(String movieOrShowName, String year, String extraInformation, String tvSeason, String tvEpisodeNumbers, String tvEpisodeName) {
		this.movieOrShowName = movieOrShowName;
		this.year = FileUtil.getYearFromYearString(year);
		this.extraInformation = extraInformation;
		this.tvSeasonNumber = FileUtil.getIntegerFromString(tvSeason);
		this.tvEpisodeNumber = tvEpisodeNumbers;
		this.tvEpisodeName = tvEpisodeName;
	}

	public String getMovieOrShowName() {
		return movieOrShowName;
	}

	public Integer getYear() {
		return year;
	}

	public String getExtraInformation() {
		return extraInformation;
	}

	public boolean isTvEpisode() {
		return tvSeasonNumber != null;
	}

	public Integer getTvSeasonNumber() {
		return tvSeasonNumber;
	}

	public String getTvEpisodeNumber() {
		return tvEpisodeNumber;
	}

	public String getTvEpisodeName() {
		return tvEpisodeName;
	}

}
