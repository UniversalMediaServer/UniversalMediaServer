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
import com.sun.jna.Platform;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.platform.PlatformUtils;
import net.pms.service.LibraryScanner;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "ActionsApiServlet", urlPatterns = {"/v1/api/actions"}, displayName = "Actions Api Servlet")
public class ActionsApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionsApiServlet.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				boolean canShutdownComputer = (Platform.isLinux() || Platform.isMac()) ? PlatformUtils.INSTANCE.isAdmin() : true;
				JsonObject jsonResponse = new JsonObject();
				jsonResponse.addProperty("canShutdownComputer", canShutdownComputer);
				WebGuiServletHelper.respond(req, resp, jsonResponse.toString(), 200, "application/json");
			} else {
				LOGGER.trace("ActionsApiServlet request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in ActionsApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path == null || path.equals("/")) {
				Account account = AuthService.getAccountLoggedIn(req);
				if (account != null) {
					JsonObject data = WebGuiServletHelper.getJsonObjectFromBody(req);
					String operation = data.has("operation") ? data.get("operation").getAsString() : null;
					if (operation != null) {
						switch (operation) {
							case "Server.Restart" -> {
								if (account.havePermission(Permissions.SERVER_RESTART)) {
									PMS.get().resetMediaServer();
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Computer.Shutdown" -> {
								if (account.havePermission(Permissions.COMPUTER_SHUTDOWN)) {
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
									PMS.get().shutdownComputer();
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Server.ResetCache" -> {
								if (account.havePermission(Permissions.SETTINGS_MODIFY)) {
									MediaDatabase.initForce();
									try {
										MediaDatabase.resetCache();
									} catch (SQLException e) {
										LOGGER.debug("Error when re-initializing after manual cache reset:", e);
									}
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Process.Reboot" -> {
								if (account.havePermission(Permissions.APPLICATION_RESTART | Permissions.APPLICATION_SHUTDOWN)) {
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
									ProcessUtil.reboot();
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Process.Reboot.Trace" -> {
								if (account.havePermission(Permissions.APPLICATION_RESTART | Permissions.APPLICATION_SHUTDOWN)) {
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
									ProcessUtil.reboot("trace");
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Process.Exit" -> {
								if (account.havePermission(Permissions.APPLICATION_SHUTDOWN)) {
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
									PMS.quit();
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Server.ScanAllSharedFolders" -> {
								if (account.havePermission(Permissions.SETTINGS_MODIFY)) {
									if (CONFIGURATION.getUseCache() && !LibraryScanner.isScanLibraryRunning()) {
										LibraryScanner.scanLibrary();
									}
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Server.ScanAllSharedFoldersCancel" -> {
								if (account.havePermission(Permissions.SETTINGS_MODIFY)) {
									if (LibraryScanner.isScanLibraryRunning()) {
										LibraryScanner.stopScanLibrary();
									}
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							default -> WebGuiServletHelper.respondBadRequest(req, resp, "Operation not configured");
						}
					} else {
						WebGuiServletHelper.respondBadRequest(req, resp);
					}
				} else {
					WebGuiServletHelper.respondUnauthorized(req, resp);
				}
			} else {
				LOGGER.trace("ActionsApiHandler request not available : {}", path);
				WebGuiServletHelper.respondNotFound(req, resp);
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in ActionsApiHandler: {}", e.getMessage());
			WebGuiServletHelper.respondInternalServerError(req, resp);
		}
	}

}
