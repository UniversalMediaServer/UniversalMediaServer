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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.io.IOException;
import java.sql.Connection;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.database.UserDatabase;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.UsernamePassword;
import net.pms.network.webguiserver.ApiHelper;
import net.pms.network.webguiserver.ServletHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
@WebServlet({"/v1/api/auth"})
public class AuthApiServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiServlet.class);
	private static final Gson GSON = new Gson();

	public static final String BASE_PATH = "/v1/api/auth";

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
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var api = new ApiHelper(req, BASE_PATH);
			if (api.get("/session")) {
				JsonObject jObject = new JsonObject();
				Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString(), api.isFromLocalhost());
				jObject.add("authenticate", new JsonPrimitive(AuthService.isEnabled()));
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
						ServletHelper.respond(req, resp, "{\"error\": \"User database not available\"}", 500, "application/json");
						return;
					}
				}
				ServletHelper.respond(req, resp, jObject.toString(), 200, "application/json");
			} else if (api.get("/disable")) {
				Connection connection = UserDatabase.getConnectionIfAvailable();
				if (connection == null) {
					LOGGER.error("User database not available");
					ServletHelper.respond(req, resp, "{\"error\": \"User database not available\"}", 500, "application/json");
				} else {
					if (!AccountService.hasNoAdmin(connection)) {
						LOGGER.error("An admin user is already in database");
						ServletHelper.respond(req, resp, null, 403, "application/json");
					} else {
						AuthService.setEnabled(false);
						ServletHelper.respond(req, resp, "", 200, "application/json");
					}
				}
			} else {
				LOGGER.trace("AuthApiServlet request not available : {}", api.getEndpoint());
				ServletHelper.respond(req, resp, null, 404, "application/json");
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AuthApiServlet: {}", e.getMessage());
			ServletHelper.respond(req, resp, null, 500, "application/json");
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var api = new ApiHelper(req, BASE_PATH);
			if (api.post("/login")) {
				String loginDetails = ServletHelper.getPostString(req);
				UsernamePassword data = GSON.fromJson(loginDetails, UsernamePassword.class);
				Connection connection = UserDatabase.getConnectionIfAvailable();
				if (connection != null) {
					Account account = AccountService.getAccountByUsername(connection, data.getUsername());
					if (account != null) {
						LOGGER.info("Got user from db: {}", account.getUsername());
						AccountService.checkUserUnlock(connection, account.getUser());
						if (AccountService.isUserLocked(account.getUser())) {
							ServletHelper.respond(req, resp, "{\"retrycount\": \"0\", \"lockeduntil\": \"" + (account.getUser().getLoginFailedTime() + AccountService.LOGIN_FAIL_LOCK_TIME) + "\"}", 401, "application/json");
						} else if (AccountService.validatePassword(data.getPassword(), account.getUser().getPassword())) {
							AccountService.setUserLogged(connection, account.getUser());
							String token = AuthService.signJwt(account.getUser().getId(), api.getRemoteHostString());
							JsonObject jObject = new JsonObject();
							jObject.add("token", new JsonPrimitive(token));
							JsonElement jElement = GSON.toJsonTree(account);
							JsonObject jAccount = jElement.getAsJsonObject();
							jAccount.getAsJsonObject("user").remove("password");
							jObject.add("account", jAccount);
							ServletHelper.respond(req, resp, jObject.toString(), 200, "application/json");
						} else {
							AccountService.setUserLoginFailed(connection, account.getUser());
							ServletHelper.respond(req, resp, "{\"retrycount\": \"" + (AccountService.MAX_LOGIN_FAIL_BEFORE_LOCK - account.getUser().getLoginFailedCount()) + "\", \"lockeduntil\": \"0\"}", 401, "application/json");
						}
					} else {
						ServletHelper.respond(req, resp, null, 401, "application/json");
					}
					UserDatabase.close(connection);
				} else {
					LOGGER.error("User database not available");
					ServletHelper.respond(req, resp, null, 500, "application/json");
				}
			} else if (api.post("/refresh")) {
				Account account = AuthService.getAccountLoggedIn(api.getAuthorization(), api.getRemoteHostString(), api.isFromLocalhost());
				if (account != null) {
					String token = AuthService.signJwt(account.getUser().getId(), api.getRemoteHostString());
					ServletHelper.respond(req, resp, "{\"token\": \"" + token + "\"}", 200, "application/json");
				} else {
					ServletHelper.respond(req, resp, null, 401, "application/json");
				}
			} else if (api.post("/create")) {
				//create the first admin user
				String loginDetails = ServletHelper.getPostString(req);
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
							String token = AuthService.signJwt(account.getUser().getId(), api.getRemoteHostString());
							jObject.add("token", new JsonPrimitive(token));
							jObject.add("account", AccountApiServlet.accountToJsonObject(account));
							ServletHelper.respond(req, resp, jObject.toString(), 200, "application/json");
						} else {
							LOGGER.error("Error in admin user creation");
							ServletHelper.respond(req, resp, null, 500, "application/json");
						}
					} else {
						LOGGER.error("An admin user is already in database");
						ServletHelper.respond(req, resp, null, 403, "application/json");
					}
					UserDatabase.close(connection);
				} else {
					LOGGER.error("User database not available");
					ServletHelper.respond(req, resp, "{\"error\": \"User database not available\"}", 500, "application/json");
				}
		} else {
				LOGGER.trace("AccountApiServlet request not available : {}", api.getEndpoint());
				ServletHelper.respond(req, resp, null, 404, "application/json");
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in AccountApiServlet: {}", e.getMessage());
			ServletHelper.respond(req, resp, null, 500, "application/json");
		}
	}

}
