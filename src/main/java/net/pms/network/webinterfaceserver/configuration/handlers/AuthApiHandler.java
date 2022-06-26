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
package net.pms.network.webinterfaceserver.configuration.handlers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
import net.pms.network.webinterfaceserver.configuration.ApiHelper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public class AuthApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiHandler.class);

	public static final String BASE_PATH = "/v1/api/auth";

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
				if (api.post("/login")) {
					String loginDetails = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
					UsernamePassword data = gson.fromJson(loginDetails, UsernamePassword.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						Account account = AccountService.getAccountByUsername(connection, data.getUsername());
						if (account != null) {
							LOGGER.info("Got user from db: {}", account.getUsername());
							AccountService.checkUserUnlock(connection, account.getUser());
							if (AccountService.isUserLocked(account.getUser())) {
								WebInterfaceServerUtil.respond(exchange, "{\"retrycount\": \"0\", \"lockeduntil\": \"" + (account.getUser().getLoginFailedTime() + AccountService.LOGIN_FAIL_LOCK_TIME) + "\"}", 401, "application/json");
							} else if (AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
								AccountService.setUserLogged(connection, account.getUser());
								String token = AuthService.signJwt(account.getUser().getId(), api.getRemoteHostString());
//should be replaced to handle only sse
								if (WebInterfaceServer.getAccountByUserId(account.getUser().getId()) == null) {
									WebInterfaceServer.setAccount(account);
								}
								JsonObject jObject = new JsonObject();
								jObject.add("token", new JsonPrimitive(token));
								JsonElement jElement = gson.toJsonTree(account);
								JsonObject jAccount = jElement.getAsJsonObject();
								jAccount.getAsJsonObject("user").remove("password");
								jObject.add("account", jAccount);
								WebInterfaceServerUtil.respond(exchange, jObject.toString(), 200, "application/json");
							} else {
								AccountService.setUserLoginFailed(connection, account.getUser());
								WebInterfaceServerUtil.respond(exchange, "{\"retrycount\": \"" + (AccountService.MAX_LOGIN_FAIL_BEFORE_LOCK - account.getUser().getLoginFailedCount()) + "\", \"lockeduntil\": \"0\"}", 401, "application/json");
							}
						} else {
							WebInterfaceServerUtil.respond(exchange, null, 401, "application/json");
						}
						UserDatabase.close(connection);
					} else {
						LOGGER.error("User database not available");
						WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
					}
				} else if (api.post("/refresh")) {
					Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
					if (account != null) {
						String token = AuthService.signJwt(account.getUser().getId(), api.getRemoteHostString());
						WebInterfaceServerUtil.respond(exchange, "{\"token\": \"" + token + "\"}", 200, "application/json");
					} else {
						WebInterfaceServerUtil.respond(exchange, null, 401, "application/json");
					}
				} else if (api.get("/session")) {
					JsonObject jObject = new JsonObject();
					Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString());
					if (account != null) {
						jObject.add("noAdminFound", new JsonPrimitive(false));
						jObject.add("account", AccountApiHandler.accountToJsonObject(account));
					}
					if (!jObject.has("noAdminFound")) {
						Connection connection = UserDatabase.getConnectionIfAvailable();
						if (connection != null) {
							jObject.add("noAdminFound", new JsonPrimitive(AccountService.hasNoAdmin(connection)));
							UserDatabase.close(connection);
						} else {
							LOGGER.error("User database not available");
							WebInterfaceServerUtil.respond(exchange, "{\"error\": \"User database not available\"}", 500, "application/json");
							return;
						}
					}
					WebInterfaceServerUtil.respond(exchange, jObject.toString(), 200, "application/json");
				} else if (api.post("/create")) {
					//create the first admin user
					String loginDetails = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
					UsernamePassword data = gson.fromJson(loginDetails, UsernamePassword.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						//for security, always check if no admin account is already in db
						if (AccountService.hasNoAdmin(connection)) {
							AccountService.createUser(connection, data.getUsername(), data.getPassword(), 1);
							//now login and check created user
							Account account = AccountService.getAccountByUsername(connection, data.getUsername());
							if (account != null && AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
								AccountService.setUserLogged(connection, account.getUser());
								if (AccountService.getAccountByUserId(account.getUser().getId()) == null) {
									WebInterfaceServer.setAccount(account);
								}
								JsonObject jObject = new JsonObject();
								jObject.add("noAdminFound", new JsonPrimitive(false));
								String token = AuthService.signJwt(account.getUser().getId(), api.getRemoteHostString());
								jObject.add("token", new JsonPrimitive(token));
								jObject.add("account", AccountApiHandler.accountToJsonObject(account));
								WebInterfaceServerUtil.respond(exchange, jObject.toString(), 200, "application/json");
							} else {
								LOGGER.error("Error in admin user creation");
								WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
							}
						} else {
							LOGGER.error("An admin user is already in database");
							WebInterfaceServerUtil.respond(exchange, null, 403, "application/json");
						}
						UserDatabase.close(connection);
					} else {
						LOGGER.error("User database not available");
						WebInterfaceServerUtil.respond(exchange, "{\"error\": \"User database not available\"}", 500, "application/json");
					}
				} else {
					LOGGER.trace("AuthApiHandler request not available : {}", api.getEndpoint());
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in AuthApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, null, 500, "application/json");
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in AuthApiHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
