/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webinterfaceserver.configuration.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import net.pms.PMS;
import net.pms.network.webinterfaceserver.UserServerSentEvents;
import net.pms.network.webinterfaceserver.WebInterfaceServer;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SseApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(SseApiHandler.class);

	public static final String BASE_PATH = "/v1/api/sse";
	/**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(exchange)) {
				exchange.close();
				return;
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			/**
			 * Helpers for HTTP methods and paths.
			 */
			var api = new Object() {
				private String getEndpoint() {
					String endpoint = "";
					int pos = exchange.getRequestURI().getPath().indexOf("/v1/api/sse");
					if (pos != -1) {
						endpoint = exchange.getRequestURI().getPath().substring(pos + "/v1/api/sse".length());
					}
					return endpoint;
				}

				/**
				 * @return whether this was a GET request for the specified
				 * path.
				 */
				public Boolean get(String path) {
					return exchange.getRequestMethod().equals("GET") && getEndpoint().equals(path);
				}

				/**
				 * @return whether this was a POST request for the specified
				 * path.
				 */
				public Boolean post(String path) {
					return exchange.getRequestMethod().equals("POST") && getEndpoint().equals(path);
				}
			};
			try {
				if (api.get("/")) {
					int userId = WebInterfaceServerUtil.getValidUserIdFromPayload(exchange.getRequestURI().getQuery(), exchange.getRemoteAddress().getHostName());
					if (userId > 0) {
						Headers hdr = exchange.getResponseHeaders();
						hdr.add("Server", PMS.get().getServerName());
						hdr.add("Content-Type", "text/event-stream");
						hdr.add("Connection", "keep-alive");
						hdr.add("Charset", "UTF-8");
						exchange.sendResponseHeaders(200, 0);
						UserServerSentEvents sse = new UserServerSentEvents(exchange.getResponseBody(), WebInterfaceServerUtil.getFirstSupportedLanguage(exchange), userId);
						WebInterfaceServer.addServerSentEventsFor(userId, sse);
					} else {
						WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
					}
				} else {
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in SseApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
			}
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in SseApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
