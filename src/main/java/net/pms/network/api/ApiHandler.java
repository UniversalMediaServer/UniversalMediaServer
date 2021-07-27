package net.pms.network.api;

import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;
import org.apache.commons.codec.digest.DigestUtils;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaDatabase;
import net.pms.dlna.RootFolder;

/**
 * This class handles calls to the internal API.
 */
public class ApiHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

	private DLNAMediaDatabase database = PMS.get().getDatabase();

	public ApiHandler() {
	}

	/**
	 * Handle API calls
	 *
	 * @param method HTTP method
	 * @param content body content
	 * @param output Response to request
	 * @param event The request
	 * @param uri
	 */
	public void handleApiRequest(HttpMethod method, String content, HttpResponse output, String uri, MessageEvent event) {
		output.headers().set(HttpHeaders.Names.CONTENT_LENGTH, "0");
		output.setStatus(HttpResponseStatus.NO_CONTENT);

		String serverApiKey = PMS.getConfiguration().getApiKey();
		if (serverApiKey.length() < 12) {
			LOGGER.warn("Weak server API key configured. UMS.conf api_key should have at least 12 digests.");
			output.setStatus(HttpResponseStatus.SERVICE_UNAVAILABLE);
			return;
		}

		try {
			if (validApiKeyPresent(serverApiKey, extractApiKey(event))) {
				switch (uri) {
					case "rescan":
						rescanLibrary();
						break;
					case "rescanFileOrFolder":
						RootFolder.rescanLibraryFileOrFolder(content);
						break;
				}
			} else {
				LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf api_key value.");
				output.setStatus(HttpResponseStatus.UNAUTHORIZED);
			}
		} catch (RuntimeException e) {
			LOGGER.error("comparing api key failed: " + e.getMessage());
		}
	}

	/**
	 * checks if the given api-key equals to the provided api key.
	 *
	 * @param serverApiKey
	 * 		server API key
	 * @param givenApiKey
	 *		given API key from client
	 * @return
	 * 		TRUE if keys match.
	 */
	private boolean validApiKeyPresent(String serverApiKey, String givenApiKey) {
		boolean result = true;
		try {
			byte[] givenApiKeyHash = DigestUtils.sha256(givenApiKey.getBytes("UTF-8"));
			byte[] serverApiKeyHash = DigestUtils.sha256(serverApiKey.getBytes("UTF-8"));
			int pos = 0;
			for (byte b : serverApiKeyHash) {
				result = result && (b == givenApiKeyHash[pos++]);
			}
			LOGGER.debug("validApiKeyPresent : " + result);
			return result;
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("cannot hash api key", e);
			return false;
		} catch (RuntimeException e) {
			LOGGER.error("cannot hash api key", e);
			return false;
		}
	}

	/**
	 * Extracts API key from header.
	 * @param event
	 * @return
	 */
	private String extractApiKey(MessageEvent event) {
		HttpRequest request = (HttpRequest) event.getMessage();
		for (Entry<String, String> entry : request.headers().entries()) {
			if (entry.getKey().equalsIgnoreCase("api-key")) {
				return entry.getValue();
			}
		}
		throw new RuntimeException("no 'api-key' provided in header.");
	}

	/**
	 * rescan library
	 */
	private void rescanLibrary() {
		if (!database.isScanLibraryRunning()) {
			database.scanLibrary();
		} else {
			LOGGER.warn("library scan already in progress");
		}
	}
}
