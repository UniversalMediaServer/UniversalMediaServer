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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.ApiHelper;
import net.pms.network.webguiserver.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet({"/v1/api/actions"})
public class ActionsApiServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionsApiServlet.class);

	public static final String BASE_PATH = "/v1/api/actions";

	private final Gson gson = new Gson();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (ServletHelper.deny(req)) {
			throw new IOException("Access denied");
		}
		if (LOGGER.isTraceEnabled()) {
			ServletHelper.logHttpServletRequest(req, "");
		}
		super.service(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		var api = new ApiHelper(req, BASE_PATH);
		try {
			if (api.post("/")) {
				Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString(), api.isFromLocalhost());
				if (account != null) {
					JsonObject data = ServletHelper.getJsonObjectFromPost(req);
					String operation = data.has("operation") ? data.get("operation").getAsString() : null;
					if (operation != null) {
						switch (operation) {
							case "Server.Restart":
								if (account.havePermission(Permissions.SERVER_RESTART)) {
									PMS.get().resetMediaServer();
									ServletHelper.respond(req, resp, "{}", 200, "application/json");
								} else {
									ServletHelper.respond(req, resp, "{\"error\": \"Forbidden\"}", 403, "application/json");
								}
								break;
							case "Server.ResetCache":
								MediaDatabase.initForce();
								try {
									MediaDatabase.resetCache();
								} catch (SQLException e) {
									LOGGER.debug("Error when re-initializing after manual cache reset:", e);
								}
								ServletHelper.respond(req, resp, "{}", 200, "application/json");
								break;
							default:
								ServletHelper.respond(req, resp, "{\"error\": \"Operation not configured\"}", 400, "application/json");
						}
					} else {
						ServletHelper.respond(req, resp, "{\"error\": \"Bad Request\"}", 400, "application/json");
					}
				} else {
					ServletHelper.respond(req, resp, "{\"error\": \"Unauthorized\"}", 401, "application/json");
				}
			} else {
				LOGGER.trace("ActionsApiHandler request not available : {}", api.getEndpoint());
				ServletHelper.respond(req, resp, null, 404, "application/json");
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in ActionsApiHandler: {}", e.getMessage());
			ServletHelper.respond(req, resp, "Internal server error", 500, "application/json");
		}
	}

}
