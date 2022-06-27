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
import com.google.gson.JsonPrimitive;
import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public class ConfigurationApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationApiHandler.class);

	private static JsonObject WEB_SETTINGS_WITH_DEFAULTS = new JsonObject();

	public static synchronized JsonObject getWebSettingsWithDefaults() {
		if (WEB_SETTINGS_WITH_DEFAULTS.size() != 0) {
			return WEB_SETTINGS_WITH_DEFAULTS;
		}

		// populate WEB_SETTINGS_WITH_DEFAULTS with all defaults
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("alternate_thumb_folder", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("append_profile_name", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("audio_channels", "6");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("audio_embed_dts_in_pcm", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("audio_bitrate", "448");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("audio_remux_ac3", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("audio_use_pcm", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("audio_thumbnails_method", "1");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("auto_update", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("automatic_maximum_bitrate", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("chapter_interval", 5);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("chapter_support", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("disable_subtitles", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("disable_transcode_for_extensions", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("encoded_audio_passthrough", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("force_transcode_for_extensions", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("gpu_acceleration", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("external_network", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("generate_thumbnails", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("hide_enginenames", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("hide_extensions", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("hostname", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("ignore_the_word_a_and_the", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("ip_filter", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("language", "en-US");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("mencoder_remux_mpeg2", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("maximum_video_buffer_size", 200);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("maximum_bitrate", "90");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("minimized", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("mpeg2_main_settings", "Automatic (Wired)");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("network_interface", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("number_of_cpu_cores", "1");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("port", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("prettify_filenames", false);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("renderer_default", "");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("renderer_force_default", false);
		JsonArray allRenderers = new JsonArray();
		allRenderers.add("All renderers");
		WEB_SETTINGS_WITH_DEFAULTS.add("selected_renderers", allRenderers);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("server_engine", "0");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("server_name", "Universal Media Server");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("show_splash_screen", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("sort_method", "4");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("subs_info_level", "basic");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("thumbnail_seek_position", "4");
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("use_cache", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("use_imdb_info", true);
		WEB_SETTINGS_WITH_DEFAULTS.addProperty("x264_constant_rate_factor", "Automatic (Wired)");

		return WEB_SETTINGS_WITH_DEFAULTS;
	}

	public static final String[] VALID_EMPTY_KEYS = {
		"alternate_thumb_folder",
		"hostname",
		"ip_filter",
		"network_interface",
		"port",
		"renderer_default"
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
				JsonObject jsonResponse = new JsonObject();

				jsonResponse.add("languages", Languages.getLanguagesAsJsonArray());
				jsonResponse.add("networkInterfaces", NetworkConfiguration.getNetworkInterfacesAsJsonArray());
				jsonResponse.add("serverEngines", MediaServer.getServerEnginesAsJsonArray());
				jsonResponse.add("allRendererNames", RendererConfiguration.getAllRendererNamesAsJsonArray());
				jsonResponse.add("enabledRendererNames", RendererConfiguration.getEnabledRendererNamesAsJsonArray());
				jsonResponse.add("audioCoverSuppliers", PmsConfiguration.getAudioCoverSuppliersAsJsonArray());
				jsonResponse.add("sortMethods", PmsConfiguration.getSortMethodsAsJsonArray());
				jsonResponse.add("subtitlesInfoLevels", PmsConfiguration.getSubtitlesInfoLevelsAsJsonArray());
				int numberOfCpuCores = Runtime.getRuntime().availableProcessors();
				if (numberOfCpuCores < 1) {
					numberOfCpuCores = 1;
				}
				jsonResponse.add("numberOfCpuCores", new JsonPrimitive(numberOfCpuCores));

				String configurationAsJsonString = pmsConfiguration.getConfigurationAsJsonString();
				JsonObject configurationAsJson = JsonParser.parseString(configurationAsJsonString).getAsJsonObject();
				//back to default value if empty
				List<String> validEmptyKeys = Arrays.asList(VALID_EMPTY_KEYS);
				for (String key : getWebSettingsWithDefaults().keySet()) {
					if (!validEmptyKeys.contains(key) && configurationAsJson.has(key) && configurationAsJson.get(key).isJsonPrimitive() && "".equals(configurationAsJson.get(key).getAsString())) {
						configurationAsJson.remove(key);
					}
				}
				//select need string, not number
				String[] needConvertToString = {"server_engine", "audio_thumbnails_method", "sort_method"};
				for (String key : needConvertToString) {
					if (configurationAsJson.has(key) && configurationAsJson.get(key).isJsonPrimitive()) {
						String value = configurationAsJson.get(key).getAsString();
						configurationAsJson.add(key, new JsonPrimitive(value));
					}
				}
				jsonResponse.add("userSettings", configurationAsJson);
				jsonResponse.add("userSettingsDefaults", getWebSettingsWithDefaults());

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
				String configToSave = WebInterfaceServerUtil.getPostString(exchange);
				HashMap<String, ?> data = gson.fromJson(configToSave, HashMap.class);
				for (Map.Entry configurationSetting : data.entrySet()) {
					String key = (String) configurationSetting.getKey();
					if (getWebSettingsWithDefaults().has(key)) {
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
			} else if (api.post("/directories")) {
				//only logged users for security concerns
				Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
				if (account == null) {
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Unauthorized\"}", 401, "application/json");
					return;
				}
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
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConfigurationApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private static String getDirectoryResponse(HttpExchange exchange) {
		JsonObject data = WebInterfaceServerUtil.getJsonObjectFromPost(exchange);
		String requestedDirectory;
		if (data != null && data.has("path")) {
			requestedDirectory = data.get("path").getAsString();
		} else {
			requestedDirectory = "";
		}
		return getDirectoryResponse(requestedDirectory);
	}

	private static String getDirectoryResponse(String path) {
		if (StringUtils.isEmpty(path)) {
			// todo: support all OS'
			path = System.getProperty("user.home");
		}
		if ("roots".equals(path)) {
			return getRootsDirectoryResponse();
		}
		List<File> roots = Arrays.asList(File.listRoots());
		JsonObject jsonResponse = new JsonObject();
		File requestedDirectoryFile = new File(path);
		if (!requestedDirectoryFile.exists()) {
			return null;
		}
		File[] directories = requestedDirectoryFile.listFiles(
			(File file) -> file.isDirectory() && !file.isHidden() && !file.getName().startsWith(".")
		);
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
		jsonResponse.add("childrens", jsonArray);
		String name = requestedDirectoryFile.getName();
		if (StringUtils.isEmpty(name) && roots.contains(requestedDirectoryFile)) {
			name = requestedDirectoryFile.toString().replace("\\", "");
		}
		jsonArray = new JsonArray();
		JsonObject directoryGroup = new JsonObject();
		directoryGroup.addProperty("label", name);
		directoryGroup.addProperty("value", requestedDirectoryFile.toString());
		jsonArray.add(directoryGroup);
		while (requestedDirectoryFile.getParentFile() != null && requestedDirectoryFile.getParentFile().isDirectory()) {
			directoryGroup = new JsonObject();
			requestedDirectoryFile = requestedDirectoryFile.getParentFile();
			name = requestedDirectoryFile.getName();
			if (StringUtils.isEmpty(name) && roots.contains(requestedDirectoryFile)) {
				name = requestedDirectoryFile.toString().replace("\\", "");
			}
			directoryGroup.addProperty("label", name);
			directoryGroup.addProperty("value", requestedDirectoryFile.toString());
			jsonArray.add(directoryGroup);
		}
		jsonResponse.add("parents", jsonArray);
		jsonResponse.add("separator", new JsonPrimitive(File.separator));
		return jsonResponse.toString();
	}

	private static String getRootsDirectoryResponse() {
		JsonObject jsonResponse = new JsonObject();
		JsonArray jsonArray = new JsonArray();
		for (File file : File.listRoots()) {
			JsonObject directoryGroup = new JsonObject();
			directoryGroup.addProperty("label", file.toString().replace("\\", ""));
			directoryGroup.addProperty("value", file.toString());
			jsonArray.add(directoryGroup);
		}
		jsonResponse.add("childrens", jsonArray);
		jsonResponse.add("parents", new JsonArray());
		jsonResponse.add("separator", new JsonPrimitive(File.separator));
		return jsonResponse.toString();
	}
}
