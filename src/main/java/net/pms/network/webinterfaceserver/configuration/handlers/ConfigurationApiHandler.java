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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import net.pms.util.Languages;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public class ConfigurationApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationApiHandler.class);

	public static final String[] VALID_KEYS = {
		"alternate_thumb_folder",
		"append_profile_name",
		"audio_thumbnails_method",
		"auto_update",
		"audio_bitrate",
		"audio_channels",
		"audio_embed_dts_in_pcm",
		"audio_remux_ac3",
		"audio_use_pcm",
		"automatic_maximum_bitrate",
		"chapter_interval",
		"chapter_support",
		"disable_subtitles",
		"disable_transcode_for_extensions",
		"encoded_audio_passthrough",
		"external_network",
		"force_transcode_for_extensions",
		"gpu_acceleration",
		"generate_thumbnails",
		"hostname",
		"ip_filter",
		"language",
		"maximum_bitrate",
		"mencoder_remux_mpeg2",
		"maximum_bitrate",
		"maximum_video_buffer_size",
		"minimized",
		"mpeg2_main_settings",
		"network_interface",
		"number_of_cpu_cores",
		"port",
		"renderer_default",
		"renderer_force_default",
		"selected_renderers",
		"server_engine",
		"server_name",
		"show_splash_screen",
		"thumbnail_seek_position",
		"x264_constant_rate_factor"
	};

	public static final String BASE_PATH = "/configuration-api";

	private final Gson gson = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();

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
			var api = new ApiHelper(exchange, BASE_PATH);
			/**
			 * API endpoints
			 */
			// this is called by the web interface settings React app on page load
			if (api.get("/settings")) {
				Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
				if (account == null) {
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Unauthorized\"}", 401, "application/json");
					return;
				}
				if (!account.havePermission(Permissions.SETTINGS_VIEW)) {
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
					return;
				}
				String configurationAsJsonString = pmsConfiguration.getConfigurationAsJsonString();

				JsonObject jsonResponse = new JsonObject();

				jsonResponse.add("languages", Languages.getLanguagesAsJsonArray());
				jsonResponse.add("networkInterfaces", NetworkConfiguration.getNetworkInterfacesAsJsonArray());
				jsonResponse.add("serverEngines", MediaServer.getServerEnginesAsJsonArray());
				jsonResponse.add("allRendererNames", RendererConfiguration.getAllRendererNamesAsJsonArray());
				jsonResponse.add("enabledRendererNames", RendererConfiguration.getEnabledRendererNamesAsJsonArray());
				jsonResponse.add("audioCoverSuppliers", PmsConfiguration.getAudioCoverSuppliersAsJsonArray());
				jsonResponse.add("sortMethods", PmsConfiguration.getSortMethodsAsJsonArray());

				JsonObject configurationAsJson = JsonParser.parseString(configurationAsJsonString).getAsJsonObject();
				jsonResponse.add("userSettings", configurationAsJson);

				WebInterfaceServerUtil.respond(exchange, jsonResponse.toString(), 200, "application/json");
			} else if (api.post("/settings")) {
				Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
				if (account == null) {
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Unauthorized\"}", 401, "application/json");
					return;
				}
				if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
					return;
				}
				// Here we possibly received some updates to config values
				String configToSave = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
				HashMap<String, ?> data = gson.fromJson(configToSave, HashMap.class);
				for (Map.Entry configurationSetting : data.entrySet()) {
					String key = (String) configurationSetting.getKey();
					if (!Arrays.asList(VALID_KEYS).contains(key)) {
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
					} else if (configurationSetting.getValue() instanceof Long) {
						LOGGER.trace("Saving key {} and Integer value {}", key, configurationSetting.getValue());
						configuration.setProperty(key, (Long) configurationSetting.getValue());
					} else if (configurationSetting.getValue() instanceof ArrayList) {
						ArrayList<String> incomingArrayList = (ArrayList<String>) configurationSetting.getValue();
						LOGGER.trace("Saving key {} and ArrayList value {}", key, configurationSetting.getValue());
						String arrayAsCommaDelimitedString = "";
						for (int i = 0; i < incomingArrayList.size(); i++) {
							if (i != 0) {
								arrayAsCommaDelimitedString += ",";
							}
							arrayAsCommaDelimitedString += incomingArrayList.get(i);
						}
						configuration.setProperty(key, arrayAsCommaDelimitedString);
					} else {
						LOGGER.trace("Invalid value passed from client: {}, {} of type {}", key, configurationSetting.getValue(), configurationSetting.getValue().getClass().getSimpleName());
					}
				}
				WebInterfaceServerUtil.respond(exchange, null, 200, "application/json");
			} else if (api.get("/i18n")) {
				String i18nAsJson = Messages.getStringsAsJson();
				WebInterfaceServerUtil.respond(exchange, i18nAsJson, 200, "application/json");
			} else if (api.get("/directories")) {
				String directoryResponse = getDirectoryResponse(exchange);
				if (directoryResponse == null) {
					WebInterfaceServerUtil.respond(exchange, "Directory does not exist", 404, "application/json");
					return;
				}
				WebInterfaceServerUtil.respond(exchange, directoryResponse, 200, "application/json");
			} else {
				LOGGER.trace("ConfigurationApiHandler request not available : {}", api.getEndpoint());
				WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConfigurationApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private String getDirectoryResponse(HttpExchange exchange) {
		JsonObject jsonResponse = new JsonObject();

		Map<String, String> queryParameters = parseQueryString(exchange.getRequestURI().getQuery());
		String requestedDirectory = queryParameters.get("path");
		if (StringUtils.isEmpty(requestedDirectory)) {
			// todo: support all OS'
			requestedDirectory = System.getProperty("user.home");
		}

		File requestedDirectoryFile = new File(requestedDirectory);
		if (!requestedDirectoryFile.exists()) {
			return null;
		}
		File[] directories = requestedDirectoryFile.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return file.isDirectory() && !file.isHidden() && !file.getName().startsWith(".");
			}
		});
		Arrays.sort(directories);
		JsonArray jsonArray = new JsonArray();
		for (File file : directories) {
			JsonObject directoryGroup = new JsonObject();
			String value = file.toString();
			String label = file.getName();
			directoryGroup.addProperty("label", label);
			directoryGroup.addProperty("value", value);
			jsonArray.add(directoryGroup);
		}
		jsonResponse.add("parents", jsonArray);

		jsonArray = new JsonArray();
		JsonObject directoryGroup = new JsonObject();
		directoryGroup.addProperty("label", requestedDirectoryFile.getName());
		directoryGroup.addProperty("value", requestedDirectoryFile.toString());
		jsonArray.add(directoryGroup);

		while (requestedDirectoryFile.getParentFile() != null && requestedDirectoryFile.getParentFile().isDirectory()) {
			directoryGroup = new JsonObject();
			requestedDirectoryFile = requestedDirectoryFile.getParentFile();
			String name = requestedDirectoryFile.getName();
			if (StringUtils.isEmpty(name) && requestedDirectoryFile.toString().equals("/")) {
				name = "/";
			}
			directoryGroup.addProperty("label", name);
			directoryGroup.addProperty("value", requestedDirectoryFile.toString());
			jsonArray.add(directoryGroup);
		}
		jsonResponse.add("children", jsonArray);

		return jsonResponse.toString();
	}

	/**
	 * @see https://stackoverflow.com/a/41610845/2049714
	 */
	private static Map<String, String> parseQueryString(String qs) {
		Map<String, String> result = new HashMap<>();
		if (qs == null) {
			return result;
		}

		int last = 0, next, l = qs.length();
		while (last < l) {
			next = qs.indexOf('&', last);
			if (next == -1) {
				next = l;
			}

			if (next > last) {
				int eqPos = qs.indexOf('=', last);
				try {
					if (eqPos < 0 || eqPos > next) {
						result.put(URLDecoder.decode(qs.substring(last, next), "utf-8"), "");
					} else {
						result.put(URLDecoder.decode(qs.substring(last, eqPos), "utf-8"), URLDecoder.decode(qs.substring(eqPos + 1, next), "utf-8"));
					}
				} catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e); // will never happen, utf-8 support is mandatory for java
				}
			}
			last = next + 1;
		}
		return result;
	}
}
