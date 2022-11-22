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

public class ApiPerson {
	private int gender;
	private int id;
	private String name;
	@SerializedName("profile_path")
	private String profilePath;

	public int getGender() {
		return gender;
	}

	public void setGender(int value) {
		this.gender = value;
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

	public String getProfilePath() {
		return profilePath;
	}

	public void setProfilePath(String value) {
		this.profilePath = value;
	}

}
