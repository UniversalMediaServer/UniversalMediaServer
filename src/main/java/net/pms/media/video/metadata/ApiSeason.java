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

import com.google.gson.annotations.SerializedName;

public class ApiSeason {
	@SerializedName("air_date")
	private String airDate;
	@SerializedName("episode_count")
	private int episodeCount;
	private int id;
	private String name;
	private String overview;
	@SerializedName("poster_path")
	private String posterPath;
	@SerializedName("season_number")
	private int seasonNumber;

	public String getAirDate() {
		return airDate;
	}

	public void setAirDate(String value) {
		this.airDate = value;
	}

	public int getEpisodeCount() {
		return episodeCount;
	}

	public void setEpisodeCount(int value) {
		this.episodeCount = value;
	}

	public int getId() {
		return id;
	}

	public void setId(int value) {
		this.id = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getOverview() {
		return overview;
	}

	public void setOverview(String value) {
		this.overview = value;
	}


	public String getPosterPath() {
		return posterPath;
	}

	public void setPosterPath(String value) {
		this.posterPath = value;
	}

	public int getSeasonNumber() {
		return seasonNumber;
	}

	public void setSeasonNumber(int value) {
		this.seasonNumber = value;
	}
}
