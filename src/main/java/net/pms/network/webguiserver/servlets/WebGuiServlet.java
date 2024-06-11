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

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import net.pms.network.webguiserver.GuiHttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "WebGuiServlet", urlPatterns = {"/"})
public class WebGuiServlet extends GuiHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebGuiServlet.class);

	public static final String BASE_PATH = "/";
	public static final String ABOUT_BASE_PATH = BASE_PATH + "about";
	public static final String ACCOUNTS_BASE_PATH = BASE_PATH + "accounts";
	public static final String ACTIONS_BASE_PATH = BASE_PATH + "actions";
	public static final String LOGS_BASE_PATH = BASE_PATH + "logs";
	public static final String PLAYER_BASE_PATH = BASE_PATH + "player";
	public static final String SETTINGS_BASE_PATH = BASE_PATH + "settings";
	public static final String SHARED_BASE_PATH = BASE_PATH + "shared";

	private static final List<String> ROUTES = List.of(
			ABOUT_BASE_PATH,
			ACCOUNTS_BASE_PATH,
			ACTIONS_BASE_PATH,
			LOGS_BASE_PATH,
			PLAYER_BASE_PATH,
			SETTINGS_BASE_PATH,
			SHARED_BASE_PATH
	);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uri = req.getRequestURI();
			if (uri == null) {
				uri = "/index.html";
			} else if (uri.startsWith("/static/")) {
				resp.setHeader("Cache-Control", "public, max-age=604800");
			} else {
				uri = uri.toLowerCase();
			}
			if (uri.equals(BASE_PATH) || ROUTES.contains(uri) || uri.startsWith(PLAYER_BASE_PATH)) {
				uri = "/index.html";
			}
			if (!writeAsync(req, resp, uri.substring(1))) {
				// The resource manager can't found or send the file, we need to send a response.
				LOGGER.trace("WebGuiServlet request not available : {}", req.getRequestURI());
				respond(req, resp, "<html><body>404 - File Not Found: " + req.getRequestURI() + "</body></html>", 404, "text/html");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in WebGuiServlet: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
