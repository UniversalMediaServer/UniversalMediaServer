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

public class ApiNetwork {
	private int id;
	@SerializedName("logo_path")
	private String logoPath;
	private String name;
	@SerializedName("origin_country")
	private String originCountry;

	public int getId() {
		return id;
	}

	public void setId(int value) {
		this.id = value;
	}

	public String getLogoPath() {
		return logoPath;
	}

	public void setLogoPath(String value) {
		this.logoPath = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getOriginCountry() {
		return originCountry;
	}

	public void setOriginCountry(String value) {
		this.originCountry = value;
	}

}
