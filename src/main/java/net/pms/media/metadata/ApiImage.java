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

import com.google.gson.annotations.SerializedName;

public class ApiImage {
	@SerializedName("aspect_ratio")
	private double aspectRatio;
	private int height;
	@SerializedName("iso_639_1")
	private String iso639Part1;
	@SerializedName("file_path")
	private String filePath;
	@SerializedName("vote_average")
	private double voteAverage;
	@SerializedName("vote_count")
	private int voteCount;
	private int width;

	public double getAspectRatio() {
		return aspectRatio;
	}

	public String getIso639Part1() {
		return iso639Part1;
	}

	public String getFilePath() {
		return filePath;
	}

	public int getHeigth() {
		return height;
	}

	public double getVoteAverage() {
		return voteAverage;
	}

	public int getVoteCount() {
		return voteCount;
	}

	public int getWidth() {
		return width;
	}
}