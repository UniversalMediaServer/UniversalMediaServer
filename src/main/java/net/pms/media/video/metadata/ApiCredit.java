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

public class ApiCredit extends ApiPersonCredited {
	private boolean adult;
	private String character;
	private String department;
	private String job;
	@SerializedName("known_for_department")
	private String knownForDepartment;
	private int order;
	@SerializedName("original_name")
	private String originalName;
	private Double popularity;

	public boolean isAdult() {
		return adult;
	}

	public void setIsAdult(boolean value) {
		this.adult = value;
	}

	public String getCharacter() {
		return character;
	}

	public void setCharacter(String value) {
		this.character = value;
	}

	public String getDepartment() {
		return department;
	}

	public void setDepartment(String value) {
		this.department = value;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String value) {
		this.job = value;
	}

	public String getKnownForDepartment() {
		return knownForDepartment;
	}

	public void setKnownForDepartment(String value) {
		this.knownForDepartment = value;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int value) {
		this.order = value;
	}

	public String getOriginalName() {
		return originalName;
	}

	public void setOriginalName(String value) {
		this.originalName = value;
	}

	public Double getPopularity() {
		return popularity;
	}

	public void setPopularity(Double value) {
		this.popularity = value;
	}

}
