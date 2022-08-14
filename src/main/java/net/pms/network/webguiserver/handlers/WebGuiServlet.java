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
package net.pms.network.webguiserver.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.network.webguiserver.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet({"/"})
public class WebGuiServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebGuiServlet.class);

	public static final String BASE_PATH = "/";
	public static final ArrayList<String> ROUTES = new ArrayList<>(Arrays.asList(
		"/about",
		"/accounts",
		"/player",
		"/settings"
	));

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (ServletHelper.deny(req)) {
			throw new IOException("Access denied");
		}
		LOGGER.debug("Handling web gui server file request \"{}\"", req.getRequestURI());
		if (LOGGER.isTraceEnabled()) {
			ServletHelper.logHttpServletRequest(req, "");
		}
		super.service(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			String uri = req.getRequestURI() != null ? req.getRequestURI().toLowerCase() : "/index.html";
			if (uri.equals("") || uri.equals(BASE_PATH) || ROUTES.contains(uri)) {
				uri = "/index.html";
			}
			if (uri.startsWith("/static")) {
				resp.setHeader("Cache-Control", "public, max-age=604800");
			}
			if (!ServletHelper.write(req, resp, "react-app" + uri)) {
				// The resource manager can't found or send the file, we need to send a response.
				LOGGER.trace("WebGuiServlet request not available : {}", req.getRequestURI());
				ServletHelper.respond(req, resp, "<html><body>404 - File Not Found: " + req.getRequestURI() + "</body></html>", 404, "text/html");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConfigurationClientHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
