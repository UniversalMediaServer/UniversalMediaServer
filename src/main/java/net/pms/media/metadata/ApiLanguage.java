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

public class ApiLanguage {
	@SerializedName("english_name")
	private String englishName;
	@SerializedName("iso_639_1")
	private String iso639Part1;
	private String name;

	public String getEnglishName() {
		return englishName;
	}

	public void setEnglishName(String value) {
		this.englishName = value;
	}

	public String getIso639Part1() {
		return iso639Part1;
	}

	public void setIso639Part1(String value) {
		this.iso639Part1 = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String value) {
		this.name = value;
	}

}
