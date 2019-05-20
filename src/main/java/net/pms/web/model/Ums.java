package net.pms.web.model;

import java.util.Map;

public class Ums {

	private boolean upnpAllowed;

	private boolean upnpControl;

	private Map<String, String> messages;

	public Ums(boolean upnpAllowed, boolean upnpControl, Map<String,String> messages) {
		this.upnpAllowed = upnpAllowed;
		this.upnpControl = upnpControl;
		this.messages = messages;
	}

	public boolean isUpnpAllowed() {
		return upnpAllowed;
	}

	public boolean isUpnpControl() {
		return upnpControl;
	}

	public Map<String,String> getMessages() {
		return messages;
	}
}

