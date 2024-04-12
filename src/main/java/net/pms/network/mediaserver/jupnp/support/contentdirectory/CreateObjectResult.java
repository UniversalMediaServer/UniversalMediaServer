package net.pms.network.mediaserver.jupnp.support.contentdirectory;


public class CreateObjectResult {

	protected String result;
	protected String objectID;

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getObjectID() {
		return objectID;
	}

	public void setObjectID(String objectID) {
		this.objectID = objectID;
	}

	@Override
	public String toString() {
		return "CreateObjectResult [result=" + result + ", objectID=" + objectID + "]";
	}

}
