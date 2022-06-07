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
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import net.pms.database.UserDatabase;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.LoginDetails;
import net.pms.util.UserService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles calls to the internal API.
 */
public class AuthApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthApiHandler.class);

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
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			if (WebInterfaceServerUtil.deny(ia)) {
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
					String endpoint = "/";
					int pos = exchange.getRequestURI().getPath().indexOf("/v1/api/auth");
					if (pos != -1) {
						endpoint = exchange.getRequestURI().getPath().substring(pos + "/v1/api/auth".length());
					}
					return endpoint;
				}
				/**
				 * @return whether this was a GET request for the specified path.
				 */
				public Boolean get(String path) {
					return exchange.getRequestMethod().equals("GET") && getEndpoint().equals(path);
				}
				/**
				 * @return whether this was a POST request for the specified path.
				 */
				public Boolean post(String path) {
					return exchange.getRequestMethod().equals("POST") && getEndpoint().equals(path);
				}
			};
			try {
				if (api.post("/login")) {
					Boolean isFirstLogin = false;
					String loginDetails = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
					LoginDetails data = gson.fromJson(loginDetails, LoginDetails.class);
					Connection connection = UserDatabase.getConnectionIfAvailable();
					if (connection != null) {
						LoginDetails dbUser = UserService.getUserByUsername(connection, data.getUsername());
						if (dbUser != null) {
							LOGGER.info("Got user from db: {}", dbUser.getUsername());
							if (UserService.validatePassword(data.getPassword(), dbUser.getPassword())) {
								String token = AuthService.signJwt(dbUser.getUsername());
								if (data.getPassword().equals("initialpassword")) {
									isFirstLogin = true;
								}
								WebInterfaceServerUtil.respond(exchange, "{\"token\": \"" + token + "\", \"firstLogin\": \"" + isFirstLogin + "\"}", 200, "application/json");
							} else {
								WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
							}
						} else {
							WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
						}
					} else {
						LOGGER.error("User database not available");
						WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
					}
				} else if (api.post("/refresh")) {
					if (!AuthService.isLoggedIn(exchange.getRequestHeaders().get("Authorization"))) {
						WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
					}
					String loggedInUsername = AuthService.getUsernameFromJWT(exchange.getRequestHeaders().get("Authorization"));
					String token = AuthService.signJwt(loggedInUsername);
					WebInterfaceServerUtil.respond(exchange, "{\"token\": \"" + token + "\"}", 200, "application/json");
				} else {
					WebInterfaceServerUtil.respond(exchange, null, 404, "application/json");
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException in AuthApiHandler: {}", e.getMessage());
				WebInterfaceServerUtil.respond(exchange, "Internal server error", 500, "application/json");
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
