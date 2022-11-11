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

import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.RendererItem;
import net.pms.network.webguiserver.WebGuiServletHelper;
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
				WebGuiServletHelper.respond(req, resp, jsonResponse.toString(), 200, "application/json");
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
					WebGuiServletHelper.respondNotFound(req, resp);
				}
			} else {
				LOGGER.trace("RenderersApiServlet request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in RenderersApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			Account account = AuthService.getAccountLoggedIn(req);
			if (account == null) {
				WebGuiServletHelper.respondForbidden(req, resp);
				return;
			}
			var path = req.getPathInfo();
			switch (path) {
				case "/infos" -> {
					JsonObject post = WebGuiServletHelper.getJsonObjectFromBody(req);
					JsonObject rendererInfos = getRendererInfos(post);
					if (rendererInfos == null) {
						WebGuiServletHelper.respondBadRequest(req, resp);
						return;
					}
					WebGuiServletHelper.respond(req, resp, rendererInfos.toString(), 200, "application/json");
				}
				case "/control" -> {
					if (!account.havePermission(Permissions.DEVICES_CONTROL)) {
						WebGuiServletHelper.respondForbidden(req, resp);
						return;
					}
					JsonObject post = WebGuiServletHelper.getJsonObjectFromBody(req);
					if (RendererItem.remoteControlRenderer(post)) {
						WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
					} else {
						WebGuiServletHelper.respondBadRequest(req, resp);
					}
				}
				case "/browse" -> {
					if (!account.havePermission(Permissions.DEVICES_CONTROL)) {
						WebGuiServletHelper.respondForbidden(req, resp);
						return;
					}
					JsonObject post = WebGuiServletHelper.getJsonObjectFromBody(req);
					JsonObject datas = RendererItem.getRemoteControlBrowse(post);
					if (datas != null) {
						WebGuiServletHelper.respond(req, resp, datas.toString(), 200, "application/json");
					} else {
						WebGuiServletHelper.respondBadRequest(req, resp);
					}
				}
				default -> {
					LOGGER.trace("RenderersApiServlet request not available : {}", path);
					WebGuiServletHelper.respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in RenderersApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	private static boolean getRendererIcon(HttpServletRequest req, HttpServletResponse resp, String icon) {
		if (icon != null) {
			try {
				InputStream is = null;
				String mime = WebGuiServletHelper.getMimeType(icon);
				if (icon.matches(".*\\S+://.*")) {
					try {
						URL url = new URL(icon);
						is = url.openStream();
						mime = WebGuiServletHelper.getMimeType(url.getPath());
					} catch (IOException e) {
						LOGGER.debug("Unable to read icon url \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
						icon = RendererConfiguration.UNKNOWN_ICON;
					}
				}

				/**
				 * Check for a custom icon file first
				 *
				 * The file can be a) the name of a file in the renderers directory b) a path relative
				 * to the PMS working directory or c) an absolute path. If no file is found,
				 * the built-in resource (if any) is used instead.
				 *
				 * The File constructor does the right thing for the relative and absolute path cases,
				 * so we only need to detect the bare filename case.
				 *
				 * RendererIcon = foo.png // e.g. $PMS/renderers/foo.png
				 * RendererIcon = images/foo.png // e.g. $PMS/images/foo.png
				 * RendererIcon = /path/to/foo.png
				 */

				if (is == null) {
					File f = RendererConfigurations.getRenderersIconFile(icon);
					if (f.isFile() && f.exists()) {
						is = new FileInputStream(f);
						mime = WebGuiServletHelper.getMimeType(f.getName());
					}
				}

				if (is == null) {
					is = RenderersApiServlet.class.getResourceAsStream("/resources/images/clients/" + icon);
					mime = WebGuiServletHelper.getMimeType(icon);
				}

				if (is == null) {
					is = RenderersApiServlet.class.getResourceAsStream("/renderers/" + icon);
					mime = WebGuiServletHelper.getMimeType(icon);
				}

				if (is == null) {
					LOGGER.debug("Unable to read icon \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
					is = RenderersApiServlet.class.getResourceAsStream("/resources/images/clients/" + RendererConfiguration.UNKNOWN_ICON);
					mime = WebGuiServletHelper.getMimeType(RendererConfiguration.UNKNOWN_ICON);
				}

				if (is != null) {
					AsyncContext async = req.startAsync();
					if (resp.getContentType() == null && mime != null) {
						resp.setContentType(mime);
					}
					resp.setContentLength(is.available());
					resp.setStatus(200);
					WebGuiServletHelper.copyStreamAsync(is, resp.getOutputStream(), async);
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
