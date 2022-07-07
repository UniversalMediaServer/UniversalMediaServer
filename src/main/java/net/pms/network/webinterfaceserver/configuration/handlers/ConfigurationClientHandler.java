/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import net.pms.network.webinterfaceserver.handlers.ThumbHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationClientHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbHandler.class);

	public static final String BASE_PATH = "/configuration";
	public static final ArrayList<String> ROUTES = new ArrayList<>(Arrays.asList(
		"/about",
		"/accounts",
		"/player",
		"/settings"
	));

	private final WebInterfaceServerHttpServer parent;

	public ConfigurationClientHandler(WebInterfaceServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(exchange)) {
				throw new IOException("Access denied");
			}
			LOGGER.debug("Handling web configuration server file request \"{}\"", exchange.getRequestURI());
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			var api = new ApiHelper(exchange, BASE_PATH);
			String endpoint = api.getEndpoint();
			if ("/".equals(endpoint) || "".equals(endpoint)) {
				endpoint = "/index.html";
			}

			if (!parent.getResources().write("react-app/" + endpoint, exchange)) {
				// The resource manager can't found or send the file, we need to send a response.
				LOGGER.trace("ConfigurationClientHandler request not available : {}", api.getEndpoint());
				WebInterfaceServerUtil.respond(exchange, "<html><body>404 - File Not Found: " + exchange.getRequestURI().getPath() + "</body></html>", 404, "text/html");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConfigurationClientHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static boolean handleApp(WebInterfaceServerHttpServer parent, HttpExchange exchange) throws IOException {
		if (ROUTES.contains(exchange.getRequestURI().getPath())) {
			if (!parent.getResources().write("react-app/index.html", exchange)) {
				// The resource manager can't found or send the file, we need to send a response.
				LOGGER.trace("ConfigurationClientHandler request not available : /index.html");
				WebInterfaceServerUtil.respond(exchange, "<html><body>404 - File Not Found: " + exchange.getRequestURI().getPath() + "</body></html>", 404, "text/html");
			}
			return true;
		}
		return false;
	}

}
