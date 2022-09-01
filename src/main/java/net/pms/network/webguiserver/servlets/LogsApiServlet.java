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
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.gui.GuiManager;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "LogsApiServlet", urlPatterns = {"/v1/api/logs"}, displayName = "Logs Api Servlet")
public class LogsApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(LogsApiServlet.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				JsonObject result = new JsonObject();
				result.addProperty("consoleLogLevel", CONFIGURATION.getLoggingFilterConsole().toString());
				result.addProperty("guiLogLevel", CONFIGURATION.getLoggingFilterLogsTab().toString());
				result.addProperty("hasMoreLogLines", GuiManager.hasMoreLogLines());
				JsonArray logLines = getLogLines();
				if (logLines != null) {
					result.add("logs", logLines);
				}
				WebGuiServletHelper.respond(req, resp, result.toString(), 200, "application/json");
			} else {
				LOGGER.trace("LogsApiServlet request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in LogsApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
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

	private static JsonArray getLogLines() {
		String[] logLines = GuiManager.getLogLines();
		return WebGuiServletHelper.getJsonArrayFromStringArray(logLines);
	}

}
