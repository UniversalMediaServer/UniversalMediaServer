package net.pms.web.model;

public class Media {
	private boolean disabled;

	private String id;

	private String name;

	private boolean playlist;

	private boolean vva;

	public Media(String id, String name, boolean disabled, boolean playlist, boolean vva) {
		this.id = id;
		this.name = name;
		this.disabled = disabled;
		this.playlist = playlist;
		this.vva = vva;
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public boolean isPlaylist() {
		return playlist;
	}

	public boolean isVva() {
		return vva;
	}
}
