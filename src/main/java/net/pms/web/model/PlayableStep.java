package net.pms.web.model;

public class PlayableStep {

	private String attr;

	private String id;

	public PlayableStep(String id, String attr) {
		this.id = id;
		this.attr = attr;
	}

	public String getAttr() {
		return attr;
	}

	public String getId() {
		return id;
	}
}
