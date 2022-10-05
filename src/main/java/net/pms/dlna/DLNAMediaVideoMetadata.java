/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import org.apache.commons.lang3.StringUtils;

/**
 * This class keeps track of the chapter properties of media.
 */
public class DLNAMediaVideoMetadata {
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

}
