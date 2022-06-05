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

package net.pms.network.mediaserver.javahttpserver;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.LoginDetails;
import net.pms.database.UserDatabase;

/**
 * This class handles calls to the internal API.
 */
public class LoginApiHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ApiHandler.class);

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
			try {
				if (exchange.getRequestMethod().equals("POST")) {
					Boolean isFirstLogin = false;
					String loginDetails = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
					LoginDetails data = gson.fromJson(loginDetails, LoginDetails.class);
					Connection connection = null;
					connection = UserDatabase.getConnectionIfAvailable();
					LoginDetails dbUser = UserService.getUserByUsername(connection, data.getUsername());
					if (dbUser != null) {
						LOGGER.info("Got user from db: {}", dbUser.getUsername());
						// A real implementation will fetch the user from H2, then securely check password match
						if (UserService.validatePassword(data.getPassword(), dbUser.getPassword())) {
							// If correct, sign a JWT and return it
							try {
								Algorithm algorithm = Algorithm.HMAC256("secret");
								String token = JWT.create()
									.withIssuer("UMS")
									.withClaim("username", dbUser.getUsername())
									.withArrayClaim("roles", new String[]{"admin"})
									.sign(algorithm);
								
								if (data.getPassword().equals("initialpassword")) {
									isFirstLogin = true;
								}
								WebInterfaceServerUtil.respond(exchange, "{\"token\": \"" + token +"\", \"firstLogin\": \""+ isFirstLogin + "\"}", 200, "application/json");
							} catch (JWTCreationException exception){
								//Invalid Signing configuration / Couldn't convert Claims.
								exchange.sendResponseHeaders(500, 0); //Internal Server Error
								LOGGER.error("Error signing JWT: {}", exception.getMessage());
							}
						} else {
							WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
						}
					} else {
						WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
					}
				}
			} catch (RuntimeException e) {
				LOGGER.error("RuntimeException: {}", e.getMessage());
				exchange.sendResponseHeaders(500, 0); //Internal Server Error
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
