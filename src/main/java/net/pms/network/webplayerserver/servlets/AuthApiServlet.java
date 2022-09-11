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
package net.pms.network.webplayerserver.servlets;

import com.google.gson.Gson;
import net.pms.network.webguiserver.servlets.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.sql.Connection;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.database.UserDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.iam.UsernamePassword;
import net.pms.network.webplayerserver.PlayerHttpServlet;
import net.pms.network.webplayerserver.WebPlayerServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
@WebServlet(name = "AuthApiServlet", urlPatterns = {"/v1/api/auth"}, displayName = "Auth Api Servlet")
public class AuthApiServlet extends PlayerHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiServlet.class);
	private static final Gson GSON = new Gson();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/session" -> {
					JsonObject jObject = new JsonObject();
					jObject.add("authenticate", new JsonPrimitive(AuthService.isEnabled()));
					Account account = AuthService.getAccountLoggedIn(req);
					if (account != null && account.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
						jObject.add("account", AccountApiServlet.accountToJsonObject(account));
					}
					WebPlayerServletHelper.respond(req, resp, jObject.toString(), 200, "application/json");
				}
				default -> {
					LOGGER.trace("AuthApiServlet request not available : {}", path);
					WebPlayerServletHelper.respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AuthApiServlet: {}", e.getMessage());
			WebPlayerServletHelper.respond(req, resp, null, 500, "application/json");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			switch (path) {
				case "/login" -> {
					String loginDetails = WebPlayerServletHelper.getBodyAsString(req);
					UsernamePassword data = GSON.fromJson(loginDetails, UsernamePassword.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						Account account = AccountService.getAccountByUsername(connection, data.getUsername());
						if (account != null) {
							LOGGER.info("Got user from db: {}", account.getUsername());
							AccountService.checkUserUnlock(connection, account.getUser());
							if (AccountService.isUserLocked(account.getUser())) {
								WebPlayerServletHelper.respond(req, resp, "{\"retrycount\": \"0\", \"lockeduntil\": \"" + (account.getUser().getLoginFailedTime() + AccountService.LOGIN_FAIL_LOCK_TIME) + "\"}", 401, "application/json");
							} else if (AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
								if (account.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
									AccountService.setUserLogged(connection, account.getUser());
									String token = AuthService.signJwt(account.getUser().getId(), req.getRemoteAddr());
									JsonObject jObject = new JsonObject();
									jObject.add("token", new JsonPrimitive(token));
									JsonElement jElement = GSON.toJsonTree(account);
									JsonObject jAccount = jElement.getAsJsonObject();
									jAccount.getAsJsonObject("user").remove("password");
									jObject.add("account", jAccount);
									WebPlayerServletHelper.respond(req, resp, jObject.toString(), 200, "application/json");
								} else {
									WebPlayerServletHelper.respondUnauthorized(req, resp);
								}
							} else {
								AccountService.setUserLoginFailed(connection, account.getUser());
								WebPlayerServletHelper.respond(req, resp, "{\"retrycount\": \"" + (AccountService.MAX_LOGIN_FAIL_BEFORE_LOCK - account.getUser().getLoginFailedCount()) + "\", \"lockeduntil\": \"0\"}", 401, "application/json");
							}
						} else {
							WebPlayerServletHelper.respondUnauthorized(req, resp);
						}
						UserDatabase.close(connection);
					} else {
						LOGGER.error("User database not available");
						WebPlayerServletHelper.respondInternalServerError(req, resp);
					}
				}
				case "/refresh" -> {
					Account account = AuthService.getAccountLoggedIn(req);
					if (account != null && account.havePermission(Permissions.WEB_PLAYER_BROWSE)) {
						String token = AuthService.signJwt(account.getUser().getId(), req.getRemoteAddr());
						WebPlayerServletHelper.respond(req, resp, "{\"token\": \"" + token + "\"}", 200, "application/json");
					} else {
						WebPlayerServletHelper.respondUnauthorized(req, resp);
					}
				}
				default -> {
					LOGGER.trace("AccountApiServlet request not available : {}", path);
					WebPlayerServletHelper.respondNotFound(req, resp);
				}
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AccountApiServlet: {}", e.getMessage());
			WebPlayerServletHelper.respondInternalServerError(req, resp);
		}
	}

}
