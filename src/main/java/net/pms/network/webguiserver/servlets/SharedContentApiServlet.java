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
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.sun.jna.Platform;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.Group;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.store.MediaStatusStore;
import net.pms.store.container.Feed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
@WebServlet(name = "SharedContentApiServlet", urlPatterns = {"/v1/api/shared"}, displayName = "Shared Content Api Servlet")
public class SharedContentApiServlet extends GuiHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(SharedContentApiServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				Account account = AuthService.getAccountLoggedIn(req.getHeader("Authorization"), req.getRemoteAddr(), req.getRemoteAddr().equals(req.getLocalAddr()));
				if (account == null) {
					respondUnauthorized(req, resp);
					return;
				}
				if (!account.havePermission(Permissions.SETTINGS_VIEW | Permissions.SETTINGS_MODIFY)) {
					respondForbidden(req, resp);
					return;
				}
				JsonObject jsonResponse = new JsonObject();

				// immutable data
				jsonResponse.add("shared_content", SharedContentConfiguration.getAsJsonArray());
				jsonResponse.addProperty("show_itunes_library", Platform.isMac() || Platform.isWindows());
				jsonResponse.addProperty("show_iphoto_library", Platform.isMac());
				jsonResponse.addProperty("show_aperture_library", Platform.isMac());
				JsonArray jGroups = new JsonArray();
				for (Group group : AccountService.getAllGroups()) {
					jGroups.add(groupToJsonObject(group));
				}
				jsonResponse.add("groups", jGroups);
				respond(req, resp, jsonResponse.toString(), 200, "application/json");
			} else {
				LOGGER.trace("SharedContentApiServlet request not available : {}", path);
				respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			respondInternalServerError(req, resp);
		} catch (IOException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in SharedContentApiServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/" -> {
					Account account = AuthService.getAccountLoggedIn(req);
					if (account == null) {
						respondUnauthorized(req, resp);
						return;
					}
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						respondForbidden(req, resp);
						return;
					}
					// Here we possibly received some updates to shared content values
					JsonObject data = getJsonObjectFromBody(req);
					if (data.has("shared_content") && data.get("shared_content").isJsonArray()) {
						SharedContentConfiguration.setFromJsonArray(data.get("shared_content").getAsJsonArray());
					}
					respond(req, resp, "{}", 200, "application/json");
				}
				case "/directories" -> {
					//only logged users for security concerns
					Account account = AuthService.getAccountLoggedIn(req);
					if (account == null) {
						respondUnauthorized(req, resp);
						return;
					}
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject post = getJsonObjectFromBody(req);
					String directoryResponse = getDirectoryResponse(post);
					if (directoryResponse == null) {
						respondNotFound(req, resp, "Directory does not exist");
						return;
					}
					respond(req, resp, directoryResponse, 200, "application/json");
				}
				case "/web-content-name" -> {
					//only logged users for security concerns
					Account account = AuthService.getAccountLoggedIn(req);
					if (account == null) {
						respondUnauthorized(req, resp);
						return;
					}
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject request = getJsonObjectFromBody(req);
					if (request.has("source")) {
						String webContentName;
						try {
							String uri = Feed.getFeedUrl(request.get("source").getAsString());
							webContentName = Feed.getFeedTitle(uri);
						} catch (Exception e) {
							webContentName = "";
						}
						respond(req, resp, "{\"name\": \"" + webContentName + "\"}", 200, "application/json");
					} else {
						respondBadRequest(req, resp);
					}
				}
				case "/mark-directory" -> {
					//only logged users for security concerns
					Account account = AuthService.getAccountLoggedIn(req);
					if (account == null) {
						respondUnauthorized(req, resp);
						return;
					}
					if (!account.havePermission(Permissions.SETTINGS_VIEW)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject request = getJsonObjectFromBody(req);
					String directory = request.get("directory").getAsString();
					boolean isPlayed = request.get("isPlayed").getAsBoolean();
					int userId = account.getUser().getId();
					if (userId == Integer.MAX_VALUE) {
						userId = 0;
					}
					Connection connection = null;
					try {
						connection = MediaDatabase.getConnectionIfAvailable();
						if (connection != null) {
							MediaStatusStore.setDirectoryFullyPlayed(connection, directory, userId, isPlayed);
						}
					} finally {
						MediaDatabase.close(connection);
					}
					respond(req, resp, "{}", 200, "application/json");
				}
				default -> {
					LOGGER.trace("SettingsApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.trace("", e);
			respondInternalServerError(req, resp);
		} catch (IOException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in SettingsApiServlet.doPost(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
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
				(File file) -> file.isDirectory() && !file.isHidden() && file.canRead() && !file.getName().startsWith(".")
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

	private static JsonObject groupToJsonObject(Group group) {
		JsonObject jObject = GSON.toJsonTree(group).getAsJsonObject();
		jObject.remove("permissions");
		return jObject;
	}

}
