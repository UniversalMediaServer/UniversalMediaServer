package net.pms.external.audioaddict;

public class RadioNetworkQuality {

	public String key;
	public String displayName;

	public RadioNetworkQuality() {
	}

	public RadioNetworkQuality(String key, String displayName) {
		this.key = key;
		this.displayName = displayName;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("RadioNetworkQuality [");
		sb.append("key=").append(this.key).append(", ");
		sb.append("displayName=").append(this.displayName).append(", ");
		sb.append("]");
		return sb.toString();
	}

}