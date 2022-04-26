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

import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(StartHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	@SuppressWarnings("unused")
	private static final String CRLF = "\r\n";

	private final WebInterfaceServerHttpServer parent;

	public StartHandler(WebInterfaceServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			LOGGER.debug("root req " + t.getRequestURI());
			if (WebInterfaceServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			if (t.getRequestURI().getPath().contains("favicon")) {
				WebInterfaceServerUtil.sendLogo(t);
				return;
			}

			HashMap<String, Object> vars = new HashMap<>();
			vars.put("serverName", CONFIGURATION.getServerDisplayName());

			try {
				Template template = parent.getResources().getTemplate("start.html");
				if (template != null) {
					String response = template.execute(vars);
					WebInterfaceServerUtil.respond(t, response, 200, "text/html");
				} else {
					throw new IOException("Web template \"start.html\" not found");
				}
			} catch (MustacheException e) {
				LOGGER.error("An error occurred while generating a HTTP response: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in StartHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
