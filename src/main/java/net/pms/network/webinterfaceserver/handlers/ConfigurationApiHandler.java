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
package net.pms.network.webinterfaceserver.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.Languages;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public class ConfigurationApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationApiHandler.class);

	private final Gson gson = new Gson();

	private final String[] validKeys = {
		"append_profile_name",
		"language",
		"server_name"
	};

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
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			/**
			 * Helpers for HTTP methods and paths.
			 */
			var api = new Object() {
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
				if (!AuthService.isLoggedIn(exchange.getRequestHeaders().get("Authorization"))) {
					WebInterfaceServerUtil.respond(exchange, null, 401, "application/json");
					return;
				}
				String configurationAsJsonString = pmsConfiguration.getConfigurationAsJsonString();
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.add("languages", Languages.getLanguagesAsJsonArray());
				JsonObject configurationAsJson = JsonParser.parseString(configurationAsJsonString).getAsJsonObject();
				jsonResponse.add("userSettings", configurationAsJson);
				WebInterfaceServerUtil.respond(exchange, jsonResponse.toString(), 200, "application/json");
			} else if (api.post("/settings")) {
				if (!AuthService.isLoggedIn(exchange.getRequestHeaders().get("Authorization"))) {
					WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
					return;
				}
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
			} else if (api.get("/i18n")) {
				if (!AuthService.isLoggedIn(exchange.getRequestHeaders().get("Authorization"))) {
					WebInterfaceServerUtil.respond(exchange, null, 401, "application/json");
					return;
				}
				String i18nAsJson = Messages.getStringsAsJson();
				WebInterfaceServerUtil.respond(exchange, i18nAsJson, 200, "application/json");
			} else {
				WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
