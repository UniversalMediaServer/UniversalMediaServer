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
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;
import net.pms.dlna.Feed;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.util.FullyPlayedAction;
import org.apache.commons.configuration.Configuration;
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
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final JsonObject WEB_SETTINGS_WITH_DEFAULTS = getWebSettingsWithDefaults();

	private static final JsonArray AUDIO_COVER_SUPPLIERS = PmsConfiguration.getAudioCoverSuppliersAsJsonArray();
	private static final JsonArray FFMPEG_LOGLEVEL = PmsConfiguration.getFfmpegLoglevels();
	private static final JsonArray FULLY_PLAYED_ACTIONS = PmsConfiguration.getFullyPlayedActionsAsJsonArray();
	private static final JsonArray SERVER_ENGINES = MediaServer.getServerEnginesAsJsonArray();
	private static final JsonArray SORT_METHODS = PmsConfiguration.getSortMethodsAsJsonArray();
	private static final JsonArray SUBTITLES_CODEPAGES = PmsConfiguration.getSubtitlesCodepageArray();
	private static final JsonArray SUBTITLES_DEPTH = PmsConfiguration.getSubtitlesDepthArray();
	private static final JsonArray SUBTITLES_INFO_LEVELS = PmsConfiguration.getSubtitlesInfoLevelsAsJsonArray();
	private static final JsonArray TRANSCODING_ENGINES_PURPOSES = PmsConfiguration.getEnginesPurposesAsJsonArray();

	private static final List<String> VALID_EMPTY_KEYS = List.of(
		"alternate_thumb_folder",
		"hostname",
		"ip_filter",
		"network_interface",
		"port",
		"renderer_default",
		"web_port",
		"web_gui_port"
	);
	private static final List<String> SELECT_KEYS = List.of("server_engine", "audio_thumbnails_method", "sort_method");
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
				for (String key : ARRAY_KEYS) {
					if (configurationAsJson.has(key) && configurationAsJson.get(key).isJsonPrimitive()) {
						JsonPrimitive value = configurationAsJson.get(key).getAsJsonPrimitive();
						JsonArray array = new JsonArray();
						array.add(value);
						configurationAsJson.add(key, array);
					}
				}
				configurationAsJson.add("shared_web_content", PmsConfiguration.getAllSharedWebContentAsJsonArray());
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
					Configuration configuration = CONFIGURATION.getRawConfiguration();
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
							if (key.equals("shared_web_content")) {
								if (configurationSetting.getValue() instanceof JsonArray array) {
									CONFIGURATION.writeWebConfigurationFile(array);
								}
							} else {
								LOGGER.trace("The key {} is not allowed", key);
							}
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
				case "/web-content-name" -> {
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
					JsonObject request = WebGuiServletHelper.getJsonObjectFromBody(req);
					if (request.has("source")) {
						String webContentName;
						try {
							webContentName = Feed.getFeedTitle(request.get("source").getAsString());
						} catch (Exception e) {
							webContentName = "";
						}
						WebGuiServletHelper.respond(req, resp, "{\"name\": \"" + webContentName + "\"}", 200, "application/json");
					} else {
						WebGuiServletHelper.respondBadRequest(req, resp);
					}
				}
				case "/mark-directory" -> {
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
					JsonObject request = WebGuiServletHelper.getJsonObjectFromBody(req);

					String directory = request.get("directory").getAsString();
					Boolean isPlayed = request.get("isPlayed").getAsBoolean();
					Connection connection = null;
					try {
						connection = MediaDatabase.getConnectionIfAvailable();
						if (connection != null) {
							MediaTableFilesStatus.setDirectoryFullyPlayed(connection, directory, isPlayed);
						}
					} finally {
						MediaDatabase.close(connection);
					}
					WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
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

	private static JsonObject getWebSettingsWithDefaults() {
		// populate WEB_SETTINGS_WITH_DEFAULTS with all defaults
		JsonObject jObj = new JsonObject();
		jObj.addProperty("alternate_subtitles_folder", "");
		jObj.addProperty("alternate_thumb_folder", "");
		jObj.addProperty("append_profile_name", false);
		jObj.addProperty("atz_limit", 10000);
		jObj.addProperty("audio_channels", "6");
		jObj.addProperty("audio_embed_dts_in_pcm", false);
		jObj.addProperty("audio_bitrate", "448");
		jObj.addProperty("audio_remux_ac3", true);
		jObj.addProperty("audio_resample", true);
		jObj.addProperty("audio_subtitles_languages", "");
		jObj.addProperty("audio_thumbnails_method", "1");
		jObj.addProperty("audio_use_pcm", false);
		jObj.addProperty("auto_update", true);
		jObj.addProperty("autoload_external_subtitles", true);
		jObj.addProperty("automatic_maximum_bitrate", true);
		jObj.addProperty("chapter_interval", 5);
		jObj.addProperty("chapter_support", false);
		jObj.addProperty("chromecast_extension", false);
		jObj.addProperty("disable_subtitles", false);
		jObj.addProperty("disable_transcode_for_extensions", "");
		jObj.addProperty("enable_archive_browsing", false);
		jObj.addProperty("encoded_audio_passthrough", false);
		JsonArray transcodingEngines = PmsConfiguration.getAllEnginesAsJsonArray();
		jObj.add("engines", transcodingEngines);
		jObj.add("engines_priority", transcodingEngines);
		jObj.addProperty("force_transcode_for_extensions", "");
		jObj.addProperty("fully_played_action", String.valueOf(FullyPlayedAction.MARK.getValue()));
		jObj.addProperty("fully_played_output_directory", "");
		jObj.addProperty("gpu_acceleration", false);
		jObj.addProperty("external_network", true);
		jObj.addProperty("ffmpeg_fontconfig", false);
		jObj.addProperty("ffmpeg_gpu_decoding_acceleration_method", "none");
		jObj.addProperty("ffmpeg_gpu_decoding_acceleration_thread_number", 1);
		jObj.addProperty("ffmpeg_logging_level", "fatal");
		jObj.addProperty("ffmpeg_mencoder_problematic_subtitles", true);
		jObj.addProperty("ffmpeg_multithreading", "");
		jObj.addProperty("ffmpeg_mux_tsmuxer_compatible", false);
		jObj.addProperty("fmpeg_sox", true);
		jObj.add("folders", new JsonArray());
		jObj.add("folders_monitored", new JsonArray());
		jObj.addProperty("force_external_subtitles", true);
		jObj.addProperty("forced_subtitle_language", "");
		jObj.addProperty("forced_subtitle_tags", "forced");
		jObj.addProperty("generate_thumbnails", true);
		jObj.addProperty("hide_empty_folders", false);
		jObj.addProperty("hide_enginenames", true);
		jObj.addProperty("hide_extensions", true);
		jObj.addProperty("hostname", "");
		jObj.addProperty("ignore_the_word_a_and_the", true);
		jObj.addProperty("ip_filter", "");
		jObj.addProperty("language", "en-US");
		jObj.addProperty("live_subtitles_keep", false);
		jObj.addProperty("live_subtitles_limit", 20);
		jObj.addProperty("mencoder_ass", true);
		jObj.addProperty("mencoder_codec_specific_script", "");
		jObj.addProperty("mencoder_custom_options", "");
		jObj.addProperty("mencoder_fontconfig", true);
		jObj.addProperty("mencoder_forcefps", false);
		jObj.addProperty("mencoder_intelligent_sync", true);
		jObj.addProperty("mencoder_mt", "");
		jObj.addProperty("mencoder_mux_compatible", false);
		jObj.addProperty("mencoder_noass_outline", 1);
		jObj.addProperty("mencoder_nooutofsync", false);
		jObj.addProperty("mencoder_overscan_compensation_height", "0");
		jObj.addProperty("mencoder_overscan_compensation_width", "0");
		jObj.addProperty("mencoder_remux_mpeg2", true);
		jObj.addProperty("mencoder_scaler", false);
		jObj.addProperty("mencoder_scalex", "0");
		jObj.addProperty("mencoder_scaley", "0");
		jObj.addProperty("mencoder_skip_loop_filter", false);
		jObj.addProperty("mencoder_subfribidi", false);
		jObj.addProperty("mencoder_yadif", false);
		jObj.addProperty("maximum_video_buffer_size", 200);
		jObj.addProperty("maximum_bitrate", 90);
		jObj.addProperty("minimized", false);
		jObj.addProperty("tsmuxer_forcefps", true);
		jObj.addProperty("tsmuxer_mux_all_audiotracks", false);
		jObj.addProperty("mpeg2_main_settings", "Automatic (Wired)");
		jObj.addProperty("network_interface", "");
		int numberOfCpuCores = Runtime.getRuntime().availableProcessors();
		if (numberOfCpuCores < 1) {
			numberOfCpuCores = 1;
		}
		jObj.addProperty("number_of_cpu_cores", numberOfCpuCores);
		jObj.addProperty("port", 5001);
		jObj.addProperty("prettify_filenames", false);
		jObj.addProperty("renderer_default", "");
		jObj.addProperty("renderer_force_default", false);
		jObj.addProperty("resume", true);
		JsonArray allRenderers = new JsonArray();
		allRenderers.add("All renderers");
		jObj.add("selected_renderers", allRenderers);
		jObj.addProperty("server_engine", "0");
		jObj.addProperty("server_name", "Universal Media Server");
		jObj.addProperty("show_media_library_folder", true);
		jObj.addProperty("show_recently_played_folder", true);
		jObj.addProperty("show_server_settings_folder", false);
		jObj.addProperty("show_splash_screen", true);
		jObj.addProperty("show_transcode_folder", true);
		jObj.addProperty("sort_method", "4");
		jObj.addProperty("subs_info_level", "basic");
		jObj.addProperty("subtitles_codepage", "");
		jObj.addProperty("subtitles_color", "0xFFFFFFFF");
		jObj.addProperty("subtitles_font", "");
		jObj.addProperty("subtitles_ass_margin", 10);
		jObj.addProperty("subtitles_ass_scale", 1.4);
		jObj.addProperty("subtitles_ass_shadow", 1);
		jObj.addProperty("thumbnail_seek_position", 4);
		jObj.addProperty("upnp_enable", true);
		jObj.addProperty("use_embedded_subtitles_style", true);
		jObj.addProperty("use_cache", true);
		jObj.addProperty("use_imdb_info", true);
		jObj.addProperty("use_symlinks_target_file", true);
		jObj.addProperty("vlc_audio_sync_enabled", false);
		jObj.addProperty("vlc_use_experimental_codecs", false);
		jObj.addProperty("web_enable", true);
		jObj.addProperty("web_gui_on_start", true);
		jObj.addProperty("web_gui_port", 9002);
		jObj.addProperty("web_player_auth", false);
		jObj.addProperty("web_player_controls", true);
		jObj.addProperty("web_player_download", true);
		jObj.addProperty("web_port", 9001);
		jObj.addProperty("x264_constant_rate_factor", "Automatic (Wired)");
		jObj.addProperty("3d_subtitles_depth", "0");
		return jObj;
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
			Configuration configuration = CONFIGURATION.getRawConfiguration();
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
}
