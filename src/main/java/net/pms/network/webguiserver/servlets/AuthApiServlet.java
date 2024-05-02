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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import net.pms.database.UserDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.UsernamePassword;
import net.pms.network.webguiserver.GuiHttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
@WebServlet(name = "AuthApiServlet", urlPatterns = {"/v1/api/auth"}, displayName = "Auth Api Servlet")
public class AuthApiServlet extends GuiHttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiServlet.class);

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/session" -> {
					JsonObject jObject = new JsonObject();
					Account account = AuthService.getAccountLoggedIn(req);
					jObject.add("authenticate", new JsonPrimitive(AuthService.isEnabled()));
					jObject.add("player", new JsonPrimitive(false));
					if (account != null) {
						jObject.add("noAdminFound", new JsonPrimitive(false));
						jObject.add("account", AccountApiServlet.accountToJsonObject(account));
					}
					if (!jObject.has("noAdminFound")) {
						Connection connection = UserDatabase.getConnectionIfAvailable();
						if (connection != null) {
							jObject.add("noAdminFound", new JsonPrimitive(AccountService.hasNoAdmin(connection)));
							UserDatabase.close(connection);
						} else {
							LOGGER.error("User database not available");
							respondInternalServerError(req, resp, "User database not available");
							return;
						}
					}
					respond(req, resp, jObject.toString(), 200, "application/json");
				}
				case "/disable" -> {
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection == null) {
						LOGGER.error("User database not available");
						respondInternalServerError(req, resp, "User database not available");
					} else {
						if (!AccountService.hasNoAdmin(connection)) {
							LOGGER.error("An admin user is already in database");
							respondForbidden(req, resp);
						} else {
							AuthService.setEnabled(false);
							respond(req, resp, "", 200, "application/json");
						}
					}
				}
				default -> {
					LOGGER.trace("AuthApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}

			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AuthApiServlet: {}", e.getMessage());
			respond(req, resp, null, 500, "application/json");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/login" -> {
					String loginDetails = getBodyAsString(req);
					UsernamePassword data = GSON.fromJson(loginDetails, UsernamePassword.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						Account account = AccountService.getAccountByUsername(connection, data.getUsername());
						if (account != null) {
							LOGGER.info("Got user from db: {}", account.getUsername());
							AccountService.checkUserUnlock(connection, account.getUser());
							if (AccountService.isUserLocked(account.getUser())) {
								respond(req, resp, "{\"retrycount\": \"0\", \"lockeduntil\": \"" + (account.getUser().getLoginFailedTime() + AccountService.LOGIN_FAIL_LOCK_TIME) + "\"}", 401, "application/json");
							} else if (AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
								AccountService.setUserLogged(connection, account.getUser());
								String token = AuthService.signJwt(account.getUser().getId(), req.getRemoteAddr());
								JsonObject jObject = new JsonObject();
								jObject.add("token", new JsonPrimitive(token));
								JsonElement jElement = GSON.toJsonTree(account);
								JsonObject jAccount = jElement.getAsJsonObject();
								jAccount.getAsJsonObject("user").remove("password");
								jObject.add("account", jAccount);
								respond(req, resp, jObject.toString(), 200, "application/json");
							} else {
								AccountService.setUserLoginFailed(connection, account.getUser());
								respond(req, resp, "{\"retrycount\": \"" + (AccountService.MAX_LOGIN_FAIL_BEFORE_LOCK - account.getUser().getLoginFailedCount()) + "\", \"lockeduntil\": \"0\"}", 401, "application/json");
							}
						} else {
							respondUnauthorized(req, resp);
						}
						UserDatabase.close(connection);
					} else {
						LOGGER.error("User database not available");
						respondInternalServerError(req, resp);
					}
				}
				case "/refresh" -> {
					Account account = AuthService.getAccountLoggedIn(req);
					if (account != null) {
						String token = AuthService.signJwt(account.getUser().getId(), req.getRemoteAddr());
						respond(req, resp, "{\"token\": \"" + token + "\"}", 200, "application/json");
					} else {
						respondUnauthorized(req, resp);
					}
				}
				case "/create" -> {
					//create the first admin user
					String loginDetails = getBodyAsString(req);
					UsernamePassword data = GSON.fromJson(loginDetails, UsernamePassword.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						//for security, always check if no admin account is already in db
						if (AccountService.hasNoAdmin(connection)) {
							AccountService.createUser(connection, data.getUsername(), data.getPassword(), 1);
							//now login and check created user
							Account account = AccountService.getAccountByUsername(connection, data.getUsername());
							if (account != null && AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
								AccountService.setUserLogged(connection, account.getUser());
								JsonObject jObject = new JsonObject();
								jObject.add("noAdminFound", new JsonPrimitive(false));
								String token = AuthService.signJwt(account.getUser().getId(), req.getRemoteAddr());
								jObject.add("token", new JsonPrimitive(token));
								jObject.add("account", AccountApiServlet.accountToJsonObject(account));
								respond(req, resp, jObject.toString(), 200, "application/json");
							} else {
								LOGGER.error("Error in admin user creation");
								respondInternalServerError(req, resp);
							}
						} else {
							LOGGER.error("An admin user is already in database");
							respondForbidden(req, resp);
						}
						UserDatabase.close(connection);
					} else {
						LOGGER.error("User database not available");
						respondInternalServerError(req, resp, "User database not available");
					}
				}
				default -> {
					LOGGER.trace("AccountApiServlet request not available : {}", path);
					respondNotFound(req, resp);
				}

			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AccountApiServlet: {}", e.getMessage());
			respondInternalServerError(req, resp);
		}
	}

}
