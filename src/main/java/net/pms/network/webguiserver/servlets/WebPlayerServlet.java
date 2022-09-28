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

import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "WebPlayerServlet", urlPatterns = {"/"}, displayName = "Player Servlet")
public class WebPlayerServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayerServlet.class);

	private static final String BASE_PATH = "/";
	private static final String ABOUT_BASE_PATH = BASE_PATH + "about";
	private static final String PLAYER_BASE_PATH = BASE_PATH + "player";

	private static final List<String> ROUTES = List.of(
		ABOUT_BASE_PATH,
		PLAYER_BASE_PATH
	);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uri = req.getServletPath() != null ? req.getServletPath().toLowerCase() : "/index.html";
			if (uri.equals(BASE_PATH) || ROUTES.contains(uri)) {
				uri = "/index.html";
			}
			if (uri.startsWith("/static/")) {
				resp.setHeader("Cache-Control", "public, max-age=604800");
			}
			if (!WebGuiServletHelper.writeAsync(req, resp, uri.substring(1))) {
				// The resource manager can't found or send the file, we need to send a response.
				LOGGER.trace("WebPlayerServlet request not available : {}", req.getRequestURI());
				WebGuiServletHelper.respond(req, resp, "<html><body>404 - File Not Found: " + req.getRequestURI() + "</body></html>", 404, "text/html");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in WebPlayerServlet: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
