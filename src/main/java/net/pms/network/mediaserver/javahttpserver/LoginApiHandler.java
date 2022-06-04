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

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.LoginDetails;
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
			PmsConfiguration pmsConfiguration = PMS.get().getConfiguration();
			Configuration configuration = pmsConfiguration.getRawConfiguration();
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			try {
				if (exchange.getRequestMethod().equals("POST")) {
					String loginDetails = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
					LoginDetails data = gson.fromJson(loginDetails, LoginDetails.class);

					// A real implementation will fetch the user from H2, then securely check password match
					if (data.getUsername().equals("ums_user") && data.getPassword().equals("pass")) {
						// If correct, sign a JWT and return it
						WebInterfaceServerUtil.respond(exchange, "{\"token\": \"XXYYXX\"}", 200, "application/json");
					} else {
						WebInterfaceServerUtil.respond(exchange, "Unauthorized", 401, "application/json");
					}
				}
				
			} catch (RuntimeException e) {
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
