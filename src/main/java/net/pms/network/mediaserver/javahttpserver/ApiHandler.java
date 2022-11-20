/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.service.LibraryScanner;
import net.pms.dlna.RootFolder;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import org.apache.commons.io.IOUtils;

/**
 * This class handles calls to the internal API.
 */
public class ApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

	/**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			if (WebInterfaceServerUtil.deny(ia)) {
				exchange.close();
				return;
			}
			String serverApiKey = PMS.getConfiguration().getApiKey();
			String clientApiKey = exchange.getRequestHeaders().getFirst("api-key");
			String call = "";
			int pos = exchange.getRequestURI().getPath().indexOf("api/");
			if (pos != -1) {
				call = exchange.getRequestURI().getPath().substring(pos + "api/".length());
			}
			try {
				if (serverApiKey.length() < 12) {
					LOGGER.warn("Weak server API key configured. UMS.conf api_key should have at least 12 digests.");
					exchange.sendResponseHeaders(503, 0); //Service Unavailable
				} else if (clientApiKey == null) {
					LOGGER.error("no 'api-key' provided in header.");
					exchange.sendResponseHeaders(403, 0); //Forbidden
				} else if (validApiKeyPresent(serverApiKey, clientApiKey)) {
					switch (call) {
						case "rescan":
							rescanLibrary();
							break;
						case "rescanFileOrFolder":
							String filename = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
							RootFolder.rescanLibraryFileOrFolder(filename);
							break;
					}
					exchange.sendResponseHeaders(204, 0); //No Content
				} else {
					LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf api_key value.");
					exchange.sendResponseHeaders(401, 0); //Unauthorized
				}
			} catch (RuntimeException e) {
				LOGGER.error("comparing api key failed: " + e.getMessage());
				exchange.sendResponseHeaders(500, 0); //Internal Server Error
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
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
			byte[] givenApiKeyHash = DigestUtils.sha256(givenApiKey.getBytes(StandardCharsets.UTF_8));
			byte[] serverApiKeyHash = DigestUtils.sha256(serverApiKey.getBytes(StandardCharsets.UTF_8));
			int pos = 0;
			for (byte b : serverApiKeyHash) {
				result = result && (b == givenApiKeyHash[pos++]);
			}
			LOGGER.debug("validApiKeyPresent : " + result);
			return result;
		} catch (RuntimeException e) {
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
