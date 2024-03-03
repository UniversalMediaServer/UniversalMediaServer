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
import jakarta.servlet.AsyncContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.iam.User;
import net.pms.network.NetworkDeviceFilter;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.RendererItem;
import net.pms.renderers.RendererFilter;
import net.pms.renderers.RendererUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "RenderersApiServlet", urlPatterns = {"/v1/api/renderers"}, displayName = "Renderers Api Servlet")
public class RenderersApiServlet extends GuiHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(RenderersApiServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.add("renderers", RendererItem.getRenderersAsJsonArray());
				jsonResponse.addProperty("renderersBlockedByDefault", RendererFilter.getBlockedByDefault());
				jsonResponse.addProperty("networkDevicesBlockedByDefault", NetworkDeviceFilter.getBlockedByDefault());
				JsonArray jUsers = new JsonArray();
				for (User user : AccountService.getAllUsers()) {
					JsonObject jUser = new JsonObject();
					jUser.addProperty("value", user.getId());
					jUser.addProperty("label", user.getDisplayName());
					jUsers.add(jUser);
				}
				jsonResponse.add("users", jUsers);
				jsonResponse.addProperty("currentTime", System.currentTimeMillis());
				respond(req, resp, jsonResponse.toString(), 200, "application/json");
			} else if (path.equals("/devices")) {
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.addProperty("isLocalhost", isLocalhost(req));
				jsonResponse.add("networkDevices", NetworkDeviceFilter.getNetworkDevicesAsJsonArray());
				jsonResponse.addProperty("networkDevicesBlockedByDefault", NetworkDeviceFilter.getBlockedByDefault());
				jsonResponse.addProperty("currentTime", System.currentTimeMillis());
				respond(req, resp, jsonResponse.toString(), 200, "application/json");
			} else if (path.startsWith("/icon/")) {
				RendererItem renderer = null;
				String[] splitted = path.split("/");
				if (splitted.length == 4) {
					String rId = splitted[2];
					try {
						renderer = RendererItem.getRenderer(Integer.parseInt(rId));
					} catch (NumberFormatException e) {
					}
				}
				if (renderer == null || !getRendererIcon(req, resp, renderer.getIcon())) {
					respondNotFound(req, resp);
				}
			} else {
				LOGGER.trace("RenderersApiServlet request not available : {}", path);
				respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in RenderersApiServlet: {}", e.getMessage());
			respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Account account = AuthService.getAccountLoggedIn(req);
			if (account == null) {
				respondForbidden(req, resp);
				return;
			}
			var path = req.getPathInfo();
			switch (path) {
				case "/infos" -> {
					JsonObject post = getJsonObjectFromBody(req);
					JsonObject rendererInfos = getRendererInfos(post);
					if (rendererInfos == null) {
						respondBadRequest(req, resp);
						return;
					}
					respond(req, resp, rendererInfos.toString(), 200, "application/json");
				}
				case "/control" -> {
					if (!account.havePermission(Permissions.DEVICES_CONTROL)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject post = getJsonObjectFromBody(req);
					if (RendererItem.remoteControlRenderer(post)) {
						respond(req, resp, "{}", 200, "application/json");
					} else {
						respondBadRequest(req, resp);
					}
				}
				case "/browse" -> {
					if (!account.havePermission(Permissions.DEVICES_CONTROL)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject post = getJsonObjectFromBody(req);
					JsonObject datas = RendererItem.getRemoteControlBrowse(post);
					if (datas != null) {
						respond(req, resp, datas.toString(), 200, "application/json");
					} else {
						respondBadRequest(req, resp);
					}
				}
				case "/renderers" -> {
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject data = getJsonObjectFromBody(req);
					if (data != null && data.has("rule")) {
						String uuid = data.get("rule").getAsString();
						if (data.has("isAllowed")) {
							boolean isAllowed = data.get("isAllowed").getAsBoolean();
							if ("DEFAULT".equals(uuid)) {
								RendererFilter.setBlockedByDefault(!isAllowed);
							} else {
								RendererFilter.setAllowed(uuid, isAllowed);
							}
						}
						if (data.has("userId")) {
							int userId = data.get("userId").getAsInt();
							RendererUser.setRendererUser(uuid, userId);
						}
					}
					respond(req, resp, "{}", 200, "application/json");
				}
				case "/devices" -> {
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						respondForbidden(req, resp);
						return;
					}
					JsonObject data = getJsonObjectFromBody(req);
					if (data != null && data.has("rule")) {
						String rule = data.get("rule").getAsString();
						Boolean isAllowed = data.get("isAllowed").getAsBoolean();
						if ("DEFAULT".equals(rule)) {
							NetworkDeviceFilter.setBlockedByDefault(!isAllowed);
						} else {
							NetworkDeviceFilter.setAllowed(rule, isAllowed);
						}
						respond(req, resp, "{}", 200, "application/json");
					} else {
						respondBadRequest(req, resp);
					}
				}
				case "/reset" -> {
					if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
						respondForbidden(req, resp);
						return;
					}
					CONFIGURATION.setNetworkDevicesFilter("");
					CONFIGURATION.setNetworkDevicesBlockedByDefault(false);
					NetworkDeviceFilter.reset();
					CONFIGURATION.setRenderersFilter("");
					CONFIGURATION.setRenderersBlockedByDefault(false);
					RendererFilter.reset();
					respond(req, resp, "{}", 200, "application/json");
				}
				default -> {
					LOGGER.trace("RenderersApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in RenderersApiServlet: {}", e.getMessage());
			respondInternalServerError(req, resp);
		}
	}

	private static boolean getRendererIcon(HttpServletRequest req, HttpServletResponse resp, String icon) {
		if (icon != null) {
			try {
				InputStream is = null;
				String mime = getMimeType(icon);
				if (icon.matches(".*\\S+://.*")) {
					try {
						URL url = URI.create(icon).toURL();
						is = url.openStream();
						mime = getMimeType(url.getPath());
					} catch (IOException e) {
						LOGGER.debug("Unable to read icon url \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
						icon = RendererConfiguration.UNKNOWN_ICON;
					}
				}

				/**
				 * Check for a custom icon file first
				 *
				 * The file can be a) the name of a file in the renderers
				 * directory b) a path relative to the UMS working directory or
				 * c) an absolute path. If no file is found, the built-in
				 * resource (if any) is used instead.
				 *
				 * The File constructor does the right thing for the relative
				 * and absolute path cases, so we only need to detect the bare
				 * filename case.
				 *
				 * RendererIcon = foo.png // e.g. $UMS/renderers/foo.png
				 * RendererIcon = images/foo.png // e.g. $UMS/images/foo.png
				 * RendererIcon = /path/to/foo.png
				 */
				if (is == null) {
					File f = RendererConfigurations.getRenderersIconFile(icon);
					if (f.isFile() && f.exists()) {
						is = new FileInputStream(f);
						mime = getMimeType(f.getName());
					}
				}

				if (is == null) {
					is = RenderersApiServlet.class.getResourceAsStream("/resources/images/clients/" + icon);
					mime = getMimeType(icon);
				}

				if (is == null) {
					is = RenderersApiServlet.class.getResourceAsStream("/renderers/" + icon);
					mime = getMimeType(icon);
				}

				if (is == null) {
					LOGGER.debug("Unable to read icon \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
					is = RenderersApiServlet.class.getResourceAsStream("/resources/images/clients/" + RendererConfiguration.UNKNOWN_ICON);
					mime = getMimeType(RendererConfiguration.UNKNOWN_ICON);
				}

				if (is != null) {
					AsyncContext async = req.startAsync();
					if (resp.getContentType() == null && mime != null) {
						resp.setContentType(mime);
					}
					resp.setContentLength(is.available());
					resp.setStatus(200);
					copyStreamAsync(is, resp.getOutputStream(), async);
					return true;
				}
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
		LOGGER.debug("Failed to load icon: " + icon);
		return false;
	}

	private static JsonObject getRendererInfos(JsonObject data) {
		if (data != null && data.has("id")) {
			int rId = data.get("id").getAsInt();
			RendererItem renderer = RendererItem.getRenderer(rId);
			if (renderer != null) {
				return renderer.getInfos();
			}
		}
		return null;
	}

}
