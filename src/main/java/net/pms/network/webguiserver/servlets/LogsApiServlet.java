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
import com.google.gson.JsonObject;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;
import net.pms.PMS;
import net.pms.gui.GuiManager;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.util.DbgPacker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "LogsApiServlet", urlPatterns = {"/v1/api/logs"}, displayName = "Logs Api Servlet")
public class LogsApiServlet extends GuiHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogsApiServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		//don't serve if user have no config write perm as log contain critical infos that can elevated user
		//or remove them from log then we can serve users with config read only
		Account account = AuthService.getAccountLoggedIn(req);
		if (account == null) {
			respondUnauthorized(req, resp);
			return;
		}
		if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
			respondForbidden(req, resp);
			return;
		}
		try {
			var path = req.getPathInfo() != null ? req.getPathInfo() : "/";
			switch (path) {
				case "/" -> {
					JsonObject result = new JsonObject();
					result.addProperty("rootLogLevel", CONFIGURATION.getRootLogLevel());
					result.addProperty("guiLogLevel", CONFIGURATION.getLoggingFilterLogsTab().toString());
					result.addProperty("traceMode", PMS.getTraceMode());
					result.addProperty("hasMoreLogLines", GuiManager.hasMoreLogLines());
					JsonArray logLines = getLogLines();
					if (logLines != null) {
						result.add("logs", logLines);
					} else {
						result.add("logs", new JsonArray());
					}
					//do not log the logs !!!
					respond(req, resp, result.toString(), 200, "application/json", false);
				}
				case "/packer" -> {
					JsonArray itemsArray = new JsonArray();
					for (File file : PMS.get().debugPacker().getFiles()) {
						JsonObject fileObject = new JsonObject();
						fileObject.addProperty("name", file.getName());
						fileObject.addProperty("path", file.getAbsolutePath());
						fileObject.addProperty("exists", file.exists());
						itemsArray.add(fileObject);
					}
					respond(req, resp, itemsArray.toString(), 200, "application/json", false);
				}
				default -> {
					LOGGER.trace("LogsApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in LogsApiServlet: {}", e.getMessage());
			respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		//don't serve if user have no config write as log contain critical infos that can elevated user
		//or remove them from log then we can serve users with config read only
		Account account = AuthService.getAccountLoggedIn(req);
		if (account == null) {
			respondUnauthorized(req, resp);
			return;
		}
		if (!account.havePermission(Permissions.SETTINGS_MODIFY)) {
			respondForbidden(req, resp);
			return;
		}
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/packer" -> {
					JsonObject data = getJsonObjectFromBody(req);
					if (!data.has("items")) {
						respondBadRequest(req, resp);
						return;
					}
					List<String> items = new ArrayList<>();
					for (JsonElement item : data.getAsJsonArray("items")) {
						items.add(item.getAsString());
					}

					//check the file exists in the dbgPacker items to prevent hack (get other file from disk)
					List<File> files = new ArrayList<>();
					for (File file : PMS.get().debugPacker().getFiles()) {
						if (items.contains(file.getAbsolutePath())) {
							files.add(file);
						}
					}
					if (!files.isEmpty()) {
						resp.setContentType("application/zip");
						resp.setHeader("Accept-Ranges", "bytes");
						resp.setHeader("Server", MediaServer.getServerName());
						resp.setHeader("Connection", "keep-alive");
						resp.setHeader("Transfer-Encoding", "chunked");
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
						resp.setHeader("Content-Disposition", "attachment; filename=\"ums_dbg_" + dateFormat.format(new Date()) + ".zip\"");
						AsyncContext async = req.startAsync();
						try (ZipOutputStream zos = new ZipOutputStream(async.getResponse().getOutputStream())) {
							for (File file : files) {
								LOGGER.debug("Packing {}", file.getAbsolutePath());
								DbgPacker.writeToZip(zos, file);
							}
						} catch (Exception e) {
							LOGGER.debug("Error packing zip file: {}", e.getLocalizedMessage());
						}
						async.complete();
					} else {
						respondBadRequest(req, resp);
					}
				}
				default -> {
					LOGGER.trace("LogsApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in LogsApiServlet: {}", e.getMessage());
			respondInternalServerError(req, resp);
		}
	}

	private static JsonArray getLogLines() {
		String[] logLines = GuiManager.getLogLines();
		return getJsonArrayFromStringArray(logLines);
	}

}
