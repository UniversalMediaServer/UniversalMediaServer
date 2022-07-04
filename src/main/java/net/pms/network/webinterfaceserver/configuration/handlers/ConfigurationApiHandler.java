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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;

/**
 * This class handles calls to the internal API.
 */
public class ConfigurationApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationApiHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
	private static final JsonArray SERVER_ENGINES = MediaServer.getServerEnginesAsJsonArray();
	private static final JsonArray AUDIO_COVER_SUPPLIERS = PmsConfiguration.getAudioCoverSuppliersAsJsonArray();
	private static final JsonArray SORT_METHODS = PmsConfiguration.getSortMethodsAsJsonArray();
	private static final JsonArray SUBTITLES_INFO_LEVELS = PmsConfiguration.getSubtitlesInfoLevelsAsJsonArray();
	private static final JsonArray TRANSCODING_ENGINES_PURPOSES = PmsConfiguration.getEnginesPurposesAsJsonArray();
	private static final JsonObject WEB_SETTINGS_WITH_DEFAULTS = getWebSettingsWithDefaults();

	private static final List<String> VALID_EMPTY_KEYS = List.of(
		"alternate_thumb_folder",
		"hostname",
		"ip_filter",
		"network_interface",
		"port",
		"renderer_default"
	);
	private static final List<String> SELECT_KEYS = List.of("server_engine", "audio_thumbnails_method", "sort_method");
	public static final String BASE_PATH = "/configuration-api";

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

				//immutable datas
				jsonResponse.add("userSettingsDefaults", WEB_SETTINGS_WITH_DEFAULTS);
				jsonResponse.add("serverEngines", SERVER_ENGINES);
				jsonResponse.add("audioCoverSuppliers", AUDIO_COVER_SUPPLIERS);
				jsonResponse.add("sortMethods", SORT_METHODS);
				jsonResponse.add("subtitlesInfoLevels", SUBTITLES_INFO_LEVELS);
				jsonResponse.add("transcodingEnginesPurposes", TRANSCODING_ENGINES_PURPOSES);

				jsonResponse.add("languages", Languages.getLanguagesAsJsonArray());
				jsonResponse.add("networkInterfaces", NetworkConfiguration.getNetworkInterfacesAsJsonArray());
				jsonResponse.add("allRendererNames", RendererConfiguration.getAllRendererNamesAsJsonArray());
				jsonResponse.add("enabledRendererNames", RendererConfiguration.getEnabledRendererNamesAsJsonArray());
				jsonResponse.add("transcodingEngines", PmsConfiguration.getAllEnginesAsJsonObject());

				String configurationAsJsonString = CONFIGURATION.getConfigurationAsJsonString();
				JsonObject configurationAsJson = JsonParser.parseString(configurationAsJsonString).getAsJsonObject();

				//select need string, not number
				for (String key : SELECT_KEYS) {
					if (configurationAsJson.has(key) && configurationAsJson.get(key).isJsonPrimitive()) {
						String value = configurationAsJson.get(key).getAsString();
						configurationAsJson.add(key, new JsonPrimitive(value));
					}
				}
				jsonResponse.add("userSettings", configurationAsJson);

				WebInterfaceServerUtil.respond(exchange, jsonResponse.toString(), 200, "application/json");
			} else if (api.post("/settings")) {
				Configuration configuration = CONFIGURATION.getRawConfiguration();
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
				HashMap<String, ?> data = GSON.fromJson(configToSave, HashMap.class);
				for (Map.Entry configurationSetting : data.entrySet()) {
					String key = (String) configurationSetting.getKey();
					if (!WEB_SETTINGS_WITH_DEFAULTS.has(key)) {
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
			} else if (api.post("/i18n")) {
				JsonObject post = WebInterfaceServerUtil.getJsonObjectFromPost(exchange);
				if (post == null || !post.has("language") || !post.get("language").isJsonPrimitive()) {
					WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
					return;
				}
				Locale locale = Languages.toLocale(post.get("language").getAsString());
				if (locale == null) {
					locale = PMS.getLocale();
				}
				JsonObject i18n = new JsonObject();
				i18n.add("i18n", Messages.getStringsAsJsonObject(locale));
				i18n.add("languages", Languages.getLanguagesWithCountry(locale));
				i18n.add("isRtl", new JsonPrimitive(Languages.getLanguageIsRtl(locale)));
				WebInterfaceServerUtil.respond(exchange, i18n.toString(), 200, "application/json");
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

	public static boolean haveKey(String key) {
		return WEB_SETTINGS_WITH_DEFAULTS.has(key);
	}

	public static boolean acceptEmptyValueForKey(String key) {
		return VALID_EMPTY_KEYS.contains(key);
	}

	private static JsonObject getWebSettingsWithDefaults() {
		// populate WEB_SETTINGS_WITH_DEFAULTS with all defaults
		JsonObject jObj = new JsonObject();
		jObj.addProperty("alternate_subtitles_folder", "");
		jObj.addProperty("alternate_thumb_folder", "");
		jObj.addProperty("append_profile_name", false);
		jObj.addProperty("audio_channels", "6");
		jObj.addProperty("audio_embed_dts_in_pcm", false);
		jObj.addProperty("audio_bitrate", "448");
		jObj.addProperty("audio_remux_ac3", true);
		jObj.addProperty("audio_subtitles_languages", "");
		jObj.addProperty("audio_thumbnails_method", "1");
		jObj.addProperty("audio_use_pcm", false);
		jObj.addProperty("auto_update", true);
		jObj.addProperty("autoload_external_subtitles", true);
		jObj.addProperty("automatic_maximum_bitrate", true);
		jObj.addProperty("chapter_interval", 5);
		jObj.addProperty("chapter_support", false);
		jObj.addProperty("disable_subtitles", false);
		jObj.addProperty("disable_transcode_for_extensions", "");
		jObj.addProperty("encoded_audio_passthrough", false);
		JsonArray transcodingEngines = PmsConfiguration.getAllEnginesAsJsonArray();
		jObj.add("engines", transcodingEngines);
		jObj.add("engines_priority", transcodingEngines);
		jObj.addProperty("force_transcode_for_extensions", "");
		jObj.addProperty("gpu_acceleration", false);
		jObj.addProperty("external_network", true);
		jObj.addProperty("force_external_subtitles", true);
		jObj.addProperty("forced_subtitle_language", "");
		jObj.addProperty("forced_subtitle_tags", "forced");
		jObj.addProperty("generate_thumbnails", true);
		jObj.addProperty("hide_enginenames", true);
		jObj.addProperty("hide_extensions", true);
		jObj.addProperty("hostname", "");
		jObj.addProperty("ignore_the_word_a_and_the", true);
		jObj.addProperty("ip_filter", "");
		jObj.addProperty("language", "en-US");
		jObj.addProperty("live_subtitles_keep", false);
		jObj.addProperty("live_subtitles_limit", 20);
		jObj.addProperty("mencoder_remux_mpeg2", true);
		jObj.addProperty("maximum_video_buffer_size", 200);
		jObj.addProperty("maximum_bitrate", "90");
		jObj.addProperty("mencoder_noass_outline", 1);
		jObj.addProperty("mencoder_subfribidi", false);
		jObj.addProperty("minimized", false);
		jObj.addProperty("mpeg2_main_settings", "Automatic (Wired)");
		jObj.addProperty("network_interface", "");
		int numberOfCpuCores = Runtime.getRuntime().availableProcessors();
		if (numberOfCpuCores < 1) {
			numberOfCpuCores = 1;
		}
		jObj.addProperty("number_of_cpu_cores", numberOfCpuCores);
		jObj.addProperty("port", "");
		jObj.addProperty("prettify_filenames", false);
		jObj.addProperty("renderer_default", "");
		jObj.addProperty("renderer_force_default", false);
		JsonArray allRenderers = new JsonArray();
		allRenderers.add("All renderers");
		jObj.add("selected_renderers", allRenderers);
		jObj.addProperty("server_engine", "0");
		jObj.addProperty("server_name", "Universal Media Server");
		jObj.addProperty("show_splash_screen", true);
		jObj.addProperty("sort_method", "4");
		jObj.addProperty("subs_info_level", "basic");
		jObj.addProperty("subtitles_codepage", "");
		jObj.addProperty("subtitles_color", "0xFFFFFFFF");
		jObj.addProperty("subtitles_font", "");
		jObj.addProperty("subtitles_ass_margin", 10);
		jObj.addProperty("subtitles_ass_scale", 1.4);
		jObj.addProperty("subtitles_ass_shadow", 1);
		jObj.addProperty("thumbnail_seek_position", "4");
		jObj.addProperty("use_embedded_subtitles_style", true);
		jObj.addProperty("use_cache", true);
		jObj.addProperty("use_imdb_info", true);
		jObj.addProperty("x264_constant_rate_factor", "Automatic (Wired)");
		jObj.addProperty("3d_subtitles_depth", "0");
		return jObj;
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

	public static String getConfigurationUpdate(String key) {
		if (haveKey(key)) {
			JsonObject datas = new JsonObject();
			datas.addProperty("action", "set_configuration_changed");
			Configuration configuration = CONFIGURATION.getRawConfiguration();
			JsonObject userConfiguration = new JsonObject();
			if (configuration.containsKey(key)) {
				String strValue = Objects.toString(configuration.getProperty(key));
				if (StringUtils.isNotEmpty(strValue) || ConfigurationApiHandler.acceptEmptyValueForKey(key)) {
					//escape "\" char with "\\" otherwise json will fail
					Map<String, String> propsAsStringMap = new HashMap<>();
					propsAsStringMap.put(key, strValue.replace("\\", "\\\\"));
					String configurationAsJsonString = new PropertiesToJsonConverter().convertToJson(propsAsStringMap);
					JsonObject configurationAsJson = JsonParser.parseString(configurationAsJsonString).getAsJsonObject();
					//select need string, not number
					if (SELECT_KEYS.contains(key)) {
						String value = configurationAsJson.get(key).getAsString();
						userConfiguration.add(key, new JsonPrimitive(value));
					} else {
						userConfiguration.add(key, configurationAsJson.get(key));
					}
				} else {
					//back to default value
					userConfiguration.add(key, WEB_SETTINGS_WITH_DEFAULTS.get(key));
				}
			} else {
				//back to default value
				userConfiguration.add(key, WEB_SETTINGS_WITH_DEFAULTS.get(key));
			}
			datas.add("value", userConfiguration);
			return datas.toString();
		}
		return "";
	}
}
