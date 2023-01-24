/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationConverter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.jalokim.propertiestojson.util.PropertiesToJsonConverter;

/**
 * This class handles calls to the internal API.
 */
@WebServlet(name = "SettingsApiServlet", urlPatterns = {"/v1/api/settings"}, displayName = "Settings Api Servlet")
public class SettingsApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettingsApiServlet.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final JsonObject WEB_SETTINGS_WITH_DEFAULTS = UmsConfiguration.getWebSettingsWithDefaults();

	private static final JsonArray AUDIO_COVER_SUPPLIERS = UmsConfiguration.getAudioCoverSuppliersAsJsonArray();
	private static final JsonArray FFMPEG_LOGLEVEL = UmsConfiguration.getFfmpegLoglevels();
	private static final JsonArray FULLY_PLAYED_ACTIONS = UmsConfiguration.getFullyPlayedActionsAsJsonArray();
	private static final JsonArray SERVER_ENGINES = MediaServer.getServerEnginesAsJsonArray();
	private static final JsonArray SORT_METHODS = UmsConfiguration.getSortMethodsAsJsonArray();
	private static final JsonArray SUBTITLES_CODEPAGES = UmsConfiguration.getSubtitlesCodepageArray();
	private static final JsonArray SUBTITLES_DEPTH = UmsConfiguration.getSubtitlesDepthArray();
	private static final JsonArray SUBTITLES_INFO_LEVELS = UmsConfiguration.getSubtitlesInfoLevelsAsJsonArray();
	private static final JsonArray TRANSCODING_ENGINES_PURPOSES = UmsConfiguration.getEnginesPurposesAsJsonArray();
	private static final JsonArray GPU_ENCODING_H264_ACCELERATION_METHODS = UmsConfiguration.getFFmpegAvailableGPUH264EncodingAccelerationMethodsArray();
	private static final JsonArray GPU_ENCODING_H265_ACCELERATION_METHODS = UmsConfiguration.getFFmpegAvailableGPUH265EncodingAccelerationMethodsArray();

	private static final List<String> VALID_EMPTY_KEYS = List.of(
		"alternate_thumb_folder",
		"hostname",
		"ip_filter",
		"network_interface",
		"port",
		"renderer_default",
		"web_gui_port",
		"web_player_port"
	);
	private static final List<String> SELECT_KEYS = List.of("server_engine", "audio_thumbnails_method", "sort_method", "ffmpeg_avisynth_output_format_index_3d", "ffmpeg_avisynth_conversion_algorithm_index_2d_to_3d", "ffmpeg_avisynth_horizontal_resize_resolution");
	private static final List<String> ARRAY_KEYS = List.of("folders", "folders_monitored");

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			// this is called by the web interface settings React app on page load
			if (path.equals("/")) {
				Account account = AuthService.getAccountLoggedIn(req.getHeader("Authorization"), req.getRemoteAddr(), req.getRemoteAddr().equals(req.getLocalAddr()));
				if (account == null) {
					WebGuiServletHelper.respondUnauthorized(req, resp);
					return;
				}
				if (!account.havePermission(Permissions.SETTINGS_VIEW | Permissions.SETTINGS_MODIFY)) {
					WebGuiServletHelper.respondForbidden(req, resp);
					return;
				}
				JsonObject jsonResponse = new JsonObject();

				// immutable data
				jsonResponse.add("userSettingsDefaults", WEB_SETTINGS_WITH_DEFAULTS);
				jsonResponse.add("serverEngines", SERVER_ENGINES);
				jsonResponse.add("audioCoverSuppliers", AUDIO_COVER_SUPPLIERS);
				jsonResponse.add("sortMethods", SORT_METHODS);
				jsonResponse.add("subtitlesInfoLevels", SUBTITLES_INFO_LEVELS);
				jsonResponse.add("transcodingEnginesPurposes", TRANSCODING_ENGINES_PURPOSES);
				jsonResponse.add("subtitlesCodepages", SUBTITLES_CODEPAGES);
				jsonResponse.add("subtitlesDepth", SUBTITLES_DEPTH);
				jsonResponse.add("ffmpegLoglevels", FFMPEG_LOGLEVEL);
				jsonResponse.add("fullyPlayedActions", FULLY_PLAYED_ACTIONS);
				jsonResponse.add("networkInterfaces", NetworkConfiguration.getNetworkInterfacesAsJsonArray());
				jsonResponse.add("allRendererNames", RendererConfigurations.getAllRendererNamesAsJsonArray());
				jsonResponse.add("enabledRendererNames", RendererConfigurations.getEnabledRendererNamesAsJsonArray());
				jsonResponse.add("transcodingEngines", UmsConfiguration.getAllEnginesAsJsonObject());
				jsonResponse.add("gpuEncodingH264AccelerationMethods", GPU_ENCODING_H264_ACCELERATION_METHODS);
				jsonResponse.add("gpuEncodingH265AccelerationMethods", GPU_ENCODING_H265_ACCELERATION_METHODS);

				String configurationAsJsonString = getConfigurationAsJsonString();
				JsonObject configurationAsJson = JsonParser.parseString(configurationAsJsonString).getAsJsonObject();

				//select need string, not number
				for (String key : SELECT_KEYS) {
					if (configurationAsJson.has(key) && configurationAsJson.get(key).isJsonPrimitive()) {
						String value = configurationAsJson.get(key).getAsString();
						configurationAsJson.add(key, new JsonPrimitive(value));
					}
				}
				for (String key : ARRAY_KEYS) {
					if (configurationAsJson.has(key) && configurationAsJson.get(key).isJsonPrimitive()) {
						JsonPrimitive value = configurationAsJson.get(key).getAsJsonPrimitive();
						JsonArray array = new JsonArray();
						array.add(value);
						configurationAsJson.add(key, array);
					}
				}
				jsonResponse.add("userSettings", configurationAsJson);

				WebGuiServletHelper.respond(req, resp, jsonResponse.toString(), 200, "application/json");
			} else {
				LOGGER.trace("SettingsApiServlet request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			WebGuiServletHelper.respondInternalServerError(req, resp);
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in SettingsApiServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/" -> {
					Configuration configuration = CONFIGURATION.getConfiguration();
					Account account = AuthService.getAccountLoggedIn(req);
					if (account == null) {
						WebGuiServletHelper.respondUnauthorized(req, resp);
						return;
					}
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						WebGuiServletHelper.respondForbidden(req, resp);
						return;
					}
					// Here we possibly received some updates to config values
					JsonObject data = WebGuiServletHelper.getJsonObjectFromBody(req);
					for (Entry<String, JsonElement> configurationSetting : data.entrySet()) {
						String key = configurationSetting.getKey();
						if (!WEB_SETTINGS_WITH_DEFAULTS.has(key)) {
							LOGGER.trace("The key {} is not allowed", key);
							continue;
						}
						if (configurationSetting.getValue() instanceof JsonPrimitive element) {
							if (element.isBoolean()) {
								LOGGER.trace("Saving key {} and Boolean value {}", key, element);
								configuration.setProperty(key, element.getAsBoolean());
							} else if (element.isNumber()) {
								LOGGER.trace("Saving key {} and Number value {}", key, element);
								configuration.setProperty(key, element.getAsNumber());
							} else if (element.isString()) {
								LOGGER.trace("Saving key {} and String value {}", key, element);
								configuration.setProperty(key, element.getAsString());
							} else {
								LOGGER.trace("Invalid value passed from client: {}, {} of type {}", key, configurationSetting.getValue(), configurationSetting.getValue().getClass().getSimpleName());
							}
						} else if (configurationSetting.getValue() instanceof JsonArray element) {
							//assume only ArrayList<String> as before
							StringBuilder arrayAsCommaDelimitedString = new StringBuilder();
							for (int i = 0; i < element.size(); i++) {
								if (i != 0) {
									arrayAsCommaDelimitedString.append(",");
								}
								arrayAsCommaDelimitedString.append(element.get(i).getAsString());
							}
							configuration.setProperty(key, arrayAsCommaDelimitedString.toString());
						} else if (configurationSetting.getValue() instanceof JsonNull) {
							configuration.setProperty(key, "");
						} else {
							LOGGER.trace("Invalid value passed from client: {}, {} of type {}", key, configurationSetting.getValue(), configurationSetting.getValue().getClass().getSimpleName());
						}
					}
					WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
				}
				case "/directories" -> {
					//only logged users for security concerns
					Account account = AuthService.getAccountLoggedIn(req);
					if (account == null) {
						WebGuiServletHelper.respondUnauthorized(req, resp);
						return;
					}
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						WebGuiServletHelper.respondForbidden(req, resp);
						return;
					}
					JsonObject post = WebGuiServletHelper.getJsonObjectFromBody(req);
					String directoryResponse = getDirectoryResponse(post);
					if (directoryResponse == null) {
						WebGuiServletHelper.respondNotFound(req, resp, "Directory does not exist");
						return;
					}
					WebGuiServletHelper.respond(req, resp, directoryResponse, 200, "application/json");
				}
				default -> {
					LOGGER.trace("SettingsApiServlet request not available : {}", path);
					WebGuiServletHelper.respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			WebGuiServletHelper.respondInternalServerError(req, resp);
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in SettingsApiServlet.doPost(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static boolean haveKey(String key) {
		return WEB_SETTINGS_WITH_DEFAULTS.has(key);
	}

	public static boolean acceptEmptyValueForKey(String key) {
		return VALID_EMPTY_KEYS.contains(key);
	}

	private static String getDirectoryResponse(JsonObject data) {
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
		jsonResponse.add("children", jsonArray);
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
		jsonResponse.add("children", jsonArray);
		jsonResponse.add("parents", new JsonArray());
		jsonResponse.add("separator", new JsonPrimitive(File.separator));
		return jsonResponse.toString();
	}

	public static String getConfigurationUpdate(String key) {
		if (haveKey(key)) {
			JsonObject datas = new JsonObject();
			datas.addProperty("action", "set_configuration_changed");
			Configuration configuration = CONFIGURATION.getConfiguration();
			JsonObject userConfiguration = new JsonObject();
			if (configuration.containsKey(key)) {
				String strValue = Objects.toString(configuration.getProperty(key));
				if (StringUtils.isNotEmpty(strValue) || SettingsApiServlet.acceptEmptyValueForKey(key)) {
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

	/**
	 * Note: This is not guaranteed to contain ALL settings,
	 * only the ones the user has changed from defaults. To
	 * get the whole picture it needs to be combined with
	 * the defaults.
	 *
	 * Note: We do not save the configuration as JSON at
	 * any point, this is just a convenience method for
	 * our REST API.
	 *
	 * @return the user settings as a JSON string.
	 */
	public String getConfigurationAsJsonString() {
		Properties configurationAsProperties = ConfigurationConverter.getProperties(CONFIGURATION.getConfiguration());

		Map<String, String> propsAsStringMap = new HashMap<>();
		configurationAsProperties.forEach((key, value) -> {
			String strKey = Objects.toString(key);
			if (SettingsApiServlet.haveKey(strKey)) {
				String strValue = Objects.toString(value);
				//do not add non acceptable empty key then it back to default
				if (StringUtils.isNotEmpty(strValue) || SettingsApiServlet.acceptEmptyValueForKey(strKey)) {
					//escape "\" char with "\\" otherwise json will fail
					propsAsStringMap.put(strKey, strValue.replace("\\", "\\\\"));
				}
			}
		});

		return new PropertiesToJsonConverter().convertToJson(propsAsStringMap);
	}

}
