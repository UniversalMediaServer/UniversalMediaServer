/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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
package net.pms.network.webinterfaceserver.handlers;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import net.pms.database.UserDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.UsernamePassword;
import net.pms.network.webinterfaceserver.WebInterfaceServer;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserApiHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserApiHandler.class);

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
			/**
			 * Helpers for HTTP methods and paths.
			 */
			var api = new Object() {
				private String getEndpoint() {
					String endpoint = "";
					int pos = exchange.getRequestURI().getPath().indexOf("/v1/api/user");
					if (pos != -1) {
						endpoint = exchange.getRequestURI().getPath().substring(pos + "/v1/api/user".length());
					}
					return endpoint;
				}

				/**
				 * @return whether this was a GET request for the specified
				 * path.
				 */
				public Boolean get(String path) {
					return exchange.getRequestMethod().equals("GET") && getEndpoint().equals(path);
				}

				/**
				 * @return whether this was a POST request for the specified
				 * path.
				 */
				public Boolean post(String path) {
					return exchange.getRequestMethod().equals("POST") && getEndpoint().equals(path);
				}
			};

			try {
				if (api.post("/changepassword")) {
					if (!AuthService.isLoggedIn(exchange.getRequestHeaders().get("Authorization"))) {
						WebInterfaceServerUtil.respond(exchange, null, 401, "application/json");
					}
					String reqBody = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
					UsernamePassword data = gson.fromJson(reqBody, UsernamePassword.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						int loggedInUserId = AuthService.getUserIdFromJWT(exchange.getRequestHeaders().get("Authorization"));
						Account account = WebInterfaceServer.getAccountByUserId(loggedInUserId);
						/* check change password for others ?
						Account account = AccountService.getAccountByUsername(connection, data.getUsername());
						if (!account.getUsername().equals(loggedInUsername)) {
						}
						*/
						if (AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
							AccountService.updatePassword(connection, data.getNewPassword(), account.getUser());
							WebInterfaceServerUtil.respond(exchange, "{}", 200, "application/json");
						} else {
							WebInterfaceServerUtil.respond(exchange, "{\"error\": \"Current password is not correct\"}", 400, "application/json");
						}
					} else {
						LOGGER.error("User database not available");
						WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
					}
				} else {
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in UserApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in UserApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
