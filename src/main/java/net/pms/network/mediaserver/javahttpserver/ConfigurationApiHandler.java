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
package net.pms.network.mediaserver.javahttpserver;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;

/**
 * This class handles calls to the internal API.
 */
public class ConfigurationApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

	private final Gson gson = new Gson();

	private final String[] validKeys = {"append_profile_name", "server_name"};

	/**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			PmsConfiguration pmsConfiguration = PMS.get().getConfiguration();
			Configuration configuration = pmsConfiguration.getRawConfiguration();
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			if (WebInterfaceServerUtil.deny(ia)) {
				exchange.close();
				return;
			}

			/**
			 * Helpers for HTTP methods and paths.
			 */
			var api = new Object(){
				private String getEndpoint() {
					String endpoint = "";
					int pos = exchange.getRequestURI().getPath().indexOf("configuration-api");
					if (pos != -1) {
						endpoint = exchange.getRequestURI().getPath().substring(pos + "configuration-api".length());
					}
					return endpoint;
				}
				/**
				 * @return whether this was a GET request for the specified path.
				 */
				public Boolean get(String path) {
					return exchange.getRequestMethod().equals("GET") && getEndpoint().equals(path);
				}
				/**
				 * @return whether this was a POST request for the specified path.
				 */
				public Boolean post(String path) {
					return exchange.getRequestMethod().equals("POST") && getEndpoint().equals(path);
				}
			};

			/**
			 * API endpoints
			 */
			// this is called by the web interface settings React app on page load
			if (api.get("/settings")) {
				// this would be implemented higher in the stack, and on all the protected endpoints
				final List<String> authHeader = exchange.getRequestHeaders().get("Authorization");
				// this is a dumb check, representing that we would verify the JWT has been signed by this UMS server
				// for now it just matches the token sent by the dummy login endpoint
				if (authHeader != null & authHeader.get(0).equals("Bearer XXYYXX")) {
					String configurationAsJson = pmsConfiguration.getConfigurationAsJson();
					WebInterfaceServerUtil.respond(exchange, configurationAsJson, 200, "application/json");
				} else {
					WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
				}
			}
			if (api.post("/settings")) {
				// Here we possibly received some updates to config values
				String configToSave = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
				HashMap<String, ?> data = gson.fromJson(configToSave, HashMap.class);
				Iterator iterator = data.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry configurationSetting = (Map.Entry) iterator.next();
					String key = (String) configurationSetting.getKey();
					if (!Arrays.asList(validKeys).contains(key)) {
						LOGGER.trace("The key {} is not allowed", key);
						continue;
					}

					if (configurationSetting.getValue() instanceof String) {
						LOGGER.trace("Saving key {} and String value {}", key, configurationSetting.getValue());
						configuration.setProperty(key, (String) configurationSetting.getValue());
					} else if (configurationSetting.getValue() instanceof Boolean) {
						LOGGER.trace("Saving key {} and Boolean value {}", key, configurationSetting.getValue());
						configuration.setProperty(key, (Boolean) configurationSetting.getValue());
					} else if (configurationSetting.getValue() instanceof Integer) {
						LOGGER.trace("Saving key {} and Integer value {}", key, configurationSetting.getValue());
						configuration.setProperty(key, (Integer) configurationSetting.getValue());
					} else {
						LOGGER.trace("Invalid value passed from client: {}, {}", key, configurationSetting.getValue());
					}
				}
				WebInterfaceServerUtil.respond(exchange, null, 200, "application/json");
			}
			if (api.get("/i18n")) {
				String i18nAsJson = Messages.getStringsAsJson();
				WebInterfaceServerUtil.respond(exchange, i18nAsJson, 200, "application/json");
			}
		} catch (RuntimeException e) {
			exchange.sendResponseHeaders(500, 0); //Internal Server Error
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
