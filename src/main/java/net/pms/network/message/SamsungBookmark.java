package net.pms.network.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

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
		return String.format("objectId: %s\ncategoryType: %s\nposSecond: %d\nrId: %s",objectId, categoryType, this.posSecond, this.rId);
	}
}
