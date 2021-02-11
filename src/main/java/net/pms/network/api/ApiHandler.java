package net.pms.network.api;

import org.jboss.netty.handler.codec.http.HttpMethod;
import net.pms.PMS;

public class ApiHandler {

	public ApiHandler() {
	}

	/**
	 * Rescan library
	 * @param method
	 * @param response
	 * @param substring
	 */
	public void handleApiRequest(HttpMethod method, StringBuilder response, String substring) {
		switch (substring) {
			case "rescan": rescanLibrary();
			break;
		}
	}

	/**
	 * rescan library
	 */
	private void rescanLibrary() {
		PMS.get().getRootFolder(null).scan();
	}

}
