package net.pms.external.audioaddict;

public enum StreamListQuality {

	AAC_64("Good 64k (AAC)", "premium_medium"), AAC_128("Excellent (128k AAC)", "premium"), MP3_320("Excellent (320k MP3)", "premium_high");

	public String displayName = "";
	public String path = "";

	StreamListQuality(String displayName, String path) {
		this.displayName = displayName;
		this.path = path;
	}
}
