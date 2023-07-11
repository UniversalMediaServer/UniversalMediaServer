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
package net.pms.network.mediaserver.handlers.message;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "X_SetBookmark")
public class SamsungBookmark {

	@XmlElement(name = "ObjectID")
	private String objectId;

	@XmlElement(name = "PosSecond")
	private int posSecond;

	@XmlElement(name = "CategoryType")
	private String categoryType;

	@XmlElement(name = "RID")
	private String rId;

	public String getObjectId() {
		return this.objectId;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public int getPosSecond() {
		return this.posSecond;
	}

	public void setPosSecond(int posSecond) {
		this.posSecond = posSecond;
	}

	public String getCategoryType() {
		return this.categoryType;
	}

	public void setCategoryType(String categoryType) {
		this.categoryType = categoryType;
	}

	public String getrId() {
		return this.rId;
	}

	public void setrId(String rId) {
		this.rId = rId;
	}

	@Override
	public String toString() {
		return "SamsungBookmark{" +
				"objectId='" + objectId + "'" +
				", posSecond=" + posSecond +
				", categoryType='" + categoryType + "'" +
				", rId='" + rId + "'" +
				'}';
	}
}
