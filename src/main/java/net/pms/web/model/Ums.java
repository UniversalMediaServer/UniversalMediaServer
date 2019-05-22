package net.pms.web.model;

import java.util.Map;

public class Ums {

	private boolean upnpAllowed;

	private boolean upnpControl;

	private Map<String, String> messages;

	private String title;

	public Ums(boolean upnpAllowed, boolean upnpControl, String title, Map<String, String> messages) {
		this.upnpAllowed = upnpAllowed;
		this.upnpControl = upnpControl;
		this.title = title;
		this.messages = messages;
	}

	public boolean isUpnpAllowed() {
		return upnpAllowed;
	}

	public boolean isUpnpControl() {
		return upnpControl;
	}

	public Map<String, String> getMessages() {
		return messages;
	}

	public String getTitle() {
		return title;
	}
}
