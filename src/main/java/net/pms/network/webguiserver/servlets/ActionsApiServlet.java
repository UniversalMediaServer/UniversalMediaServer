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
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.store.MediaScanner;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "ActionsApiServlet", urlPatterns = {"/v1/api/actions"}, displayName = "Actions Api Servlet")
public class ActionsApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionsApiServlet.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getServletPath();
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
									if (!MediaScanner.isMediaScanRunning()) {
										MediaScanner.startMediaScan();
									}
									WebGuiServletHelper.respond(req, resp, "{}", 200, "application/json");
								} else {
									WebGuiServletHelper.respondForbidden(req, resp);
								}
							}
							case "Server.ScanAllSharedFoldersCancel" -> {
								if (account.havePermission(Permissions.SETTINGS_MODIFY)) {
									if (MediaScanner.isMediaScanRunning()) {
										MediaScanner.stopMediaScan();
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
