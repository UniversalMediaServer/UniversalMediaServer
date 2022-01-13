package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.dlna.LibraryScanner;
import net.pms.dlna.RootFolder;
import org.apache.commons.io.IOUtils;

/**
 * This class handles calls to the internal API.
 */
public class ApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

	/**
	 * Handle API calls.
	 *
	 * @param t
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			String serverApiKey = PMS.getConfiguration().getApiKey();
			String clientApiKey = t.getRequestHeaders().getFirst("api-key");
			String call = "";
			int pos = t.getRequestURI().getPath().indexOf("api/");
			if (pos != -1) {
				call = t.getRequestURI().getPath().substring(pos + "api/".length());
			}
			try {
				if (serverApiKey.length() < 12) {
					LOGGER.warn("Weak server API key configured. UMS.conf api_key should have at least 12 digests.");
					t.sendResponseHeaders(503, 0); //Service Unavailable
				} else if (clientApiKey == null) {
					LOGGER.error("no 'api-key' provided in header.");
					t.sendResponseHeaders(403, 0); //Forbidden
				} else if (validApiKeyPresent(serverApiKey, clientApiKey)) {
					switch (call) {
						case "rescan":
							rescanLibrary();
							break;
						case "rescanFileOrFolder":
							String filename = IOUtils.toString(t.getRequestBody(), StandardCharsets.UTF_8);
							RootFolder.rescanLibraryFileOrFolder(filename);
							break;
					}
					t.sendResponseHeaders(204, 0); //No Content
				} else {
					LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf api_key value.");
					t.sendResponseHeaders(401, 0); //Unauthorized
				}
			} catch (RuntimeException e) {
				LOGGER.error("comparing api key failed: " + e.getMessage());
				t.sendResponseHeaders(500, 0); //Internal Server Error
			}

		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteBrowseHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
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
	private static boolean validApiKeyPresent(String serverApiKey, String givenApiKey) {
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
		} catch (UnsupportedEncodingException | RuntimeException e) {
			LOGGER.error("cannot hash api key", e);
			return false;
		}
	}

	/**
	 * rescan library
	 */
	private static void rescanLibrary() {
		if (!LibraryScanner.isScanLibraryRunning()) {
			LibraryScanner.scanLibrary();
		} else {
			LOGGER.warn("library scan already in progress");
		}
	}
}
