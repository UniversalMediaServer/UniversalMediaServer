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
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import net.pms.PMS;
import net.pms.network.NetworkDeviceFilter;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.handlers.nextcpapi.AbstractNextcpApiHandler;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponse;
import net.pms.network.mediaserver.handlers.nextcpapi.NextcpApiResponseHandler;
import org.apache.commons.lang3.StringUtils;

/**
 * This class handles calls to the Nextcp API.
 */
public class NextcpApiHandler extends AbstractNextcpApiHandler implements HttpHandler {

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
			if (!NetworkDeviceFilter.isAllowed(ia)) {
				exchange.close();
				return;
			}
			String serverApiKey = PMS.getConfiguration().getNextcpApiKey();
			String clientApiKey = exchange.getRequestHeaders().getFirst("api-key");
			try {
				if (serverApiKey.length() < 12) {
					LOGGER.warn("Weak nextcp API key configured. UMS.conf nextcp_api_key should have at least 12 digests.");
					exchange.sendResponseHeaders(503, 0); //Service Unavailable
				} else if (clientApiKey == null) {
					LOGGER.error("no 'api-key' provided in header.");
					exchange.sendResponseHeaders(403, 0); //Forbidden
				} else if (validApiKeyPresent(serverApiKey, clientApiKey)) {
					String uri = "";
					String handler = "";
					String call = "";
					int pos = exchange.getRequestURI().getPath().indexOf("api/");
					if (pos != -1) {
						uri = exchange.getRequestURI().getPath().substring(pos + "api/".length());
					}
					pos = uri.indexOf("/");
					if (pos != -1) {
						call = uri.substring(pos + 1);
						handler = uri.substring(0, pos);
					}
					if (!StringUtils.isAllBlank(handler)) {
						NextcpApiResponseHandler responseHandler = getApiResponseHandler(handler);
						String body = null;
						if (exchange.getRequestHeaders().containsKey("Content-Length")) {
							int contentLength = 0;
							try {
								contentLength = Integer.parseInt(exchange.getRequestHeaders().getFirst("Content-Length"));
							} catch (NumberFormatException e) {
							}
							if (contentLength > 0) {
								byte[] data = new byte[contentLength];
								exchange.getRequestBody().read(data);
								body = new String(data, StandardCharsets.UTF_8);
							} else {
								body = "";
							}
						}
						NextcpApiResponse response = responseHandler.handleRequest(call, body);
						sendResponse(exchange, response);
					} else {
						LOGGER.warn("Invalid API call. Unknown path : " + uri);
						exchange.sendResponseHeaders(404, 0);
					}
				} else {
					LOGGER.warn("Invalid given API key. Request header key 'api-key' must match UMS.conf nextcp_api_key value.");
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
			LOGGER.error("Unexpected error in NextcpApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static void sendResponse(final HttpExchange exchange, NextcpApiResponse response) throws IOException {
		try (exchange) {
			exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
			if (response.getConnection() != null) {
				exchange.getResponseHeaders().set("Connection", response.getConnection());
			}
			if (response.getContentType() != null) {
				exchange.getResponseHeaders().set("Content-Type", response.getContentType());
			}
			if (response.getStatusCode() == null) {
				response.setStatusCode(200);
			}
			if (StringUtils.isAllBlank(response.getResponse())) {
				exchange.sendResponseHeaders(response.getStatusCode(), 0);
				return;
			}
			// A response message was constructed; convert it to data ready to be sent.
			byte[] responseData = response.getResponse().getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(response.getStatusCode(), responseData.length);
			// HEAD requests only require headers to be set, no need to set contents.
			if (!"HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
				// Not a HEAD request, so set the contents of the response.
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(responseData);
					os.flush();
				}
			}
		}
	}

}
