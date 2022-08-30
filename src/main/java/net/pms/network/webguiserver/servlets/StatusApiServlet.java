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

import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.configuration.RendererConfiguration;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.RendererStatus;
import net.pms.network.webguiserver.WebGuiServletHelper;
import static net.pms.network.webguiserver.WebGuiServletHelper.copyStreamAsync;
import static net.pms.network.webguiserver.WebGuiServletHelper.getMimeType;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "StatusApiServlet", urlPatterns = {"/v1/api/status"}, displayName = "Status Api Servlet")
public class StatusApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(StatusApiServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.addProperty("app", PropertiesUtil.getProjectProperties().get("project.name"));
				jsonResponse.add("renderers", RendererStatus.getRenderersAsJsonArray());
				WebGuiServletHelper.respond(req, resp, jsonResponse.toString(), 200, "application/json");
			} else if (path.startsWith("/icon/")) {
				RendererStatus renderer = null;
				String[] splitted = path.split("/");
				if (splitted.length == 4) {
					String rId = splitted[2];
					String filename = splitted[3];
					try {
						renderer = RendererStatus.getRenderer(Long.parseLong(rId));
					} catch (NumberFormatException e) {
					}
				}
				if (renderer == null || !getRendererIcon(req, resp, renderer.getIcon())) {
					WebGuiServletHelper.respondNotFound(req, resp);
				}
			} else {
				LOGGER.trace("AboutApiServlet request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AboutApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	public static boolean getRendererIcon(HttpServletRequest req, HttpServletResponse resp, String icon) {
		if (icon != null) {
			try {
				InputStream is = null;

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

				String mime = getMimeType(icon);
				File f = new File(icon);

				if (!f.isAbsolute() && f.getParent() == null) {
					// filename, try profile renderers dir
					f = new File(RendererConfiguration.getProfileRenderersDir(), icon);
					if (f.isFile()) {
						is = new FileInputStream(f);
						mime = getMimeType(f.getName());
					} else {
						//try renderers dir
						f = new File(RendererConfiguration.getRenderersDir(), icon);
						if (f.isFile()) {
							is = new FileInputStream(f);
							mime = getMimeType(f.getName());
						}
					}
				}

				if (is == null) {
					is = StatusApiServlet.class.getResourceAsStream("/resources/images/clients/" + icon);
					mime = getMimeType(icon);
				}

				if (is == null) {
					is = StatusApiServlet.class.getResourceAsStream("/renderers/" + icon);
					mime = getMimeType(icon);
				}

				if (is == null) {
					LOGGER.debug("Unable to read icon \"{}\", using \"{}\" instead.", icon, RendererConfiguration.UNKNOWN_ICON);
					is = StatusApiServlet.class.getResourceAsStream("/resources/images/clients/" + RendererConfiguration.UNKNOWN_ICON);
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

}
