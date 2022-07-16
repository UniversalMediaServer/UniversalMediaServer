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
package net.pms.network.webinterfaceserver.configuration.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import net.pms.PMS;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionsApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ActionsApiHandler.class);

	public static final String BASE_PATH = "/v1/api/actions";

	private final Gson gson = new Gson();

	/**
	 * Handle API calls.
	 *
	 * @param exchange
	 * @throws java.io.IOException
	 */
	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(exchange)) {
				exchange.close();
				return;
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(exchange, "");
			}
			var api = new ApiHelper(exchange, BASE_PATH);
			try {
				if (api.post("/")) {
					Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString(), api.isFromLocalhost());
					if (account != null) {
						String reqBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
						HashMap<String, String> data = gson.fromJson(reqBody, HashMap.class);
						String operation = data.get("operation");
						if (operation != null) {
							switch (operation) {
								case "Server.Restart":
									if (account.havePermission(Permissions.SERVER_RESTART)) {
										PMS.get().resetMediaServer();
										WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
									} else {
										WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Forbidden\"}", 403, "application/json");
									}
									break;
								default:
									WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Operation not configured\"}", 400, "application/json");
							}
						} else {
							WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Bad Request\"}", 400, "application/json");
						}
					} else {
						WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Unauthorized\"}", 401, "application/json");
					}
				} else {
					LOGGER.trace("ActionsApiHandler request not available : {}", api.getEndpoint());
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in ActionsApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
			}
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ActionsApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
