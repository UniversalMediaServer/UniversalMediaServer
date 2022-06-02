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
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationClientHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbHandler.class);

	private final WebInterfaceServerHttpServer parent;

	public ConfigurationClientHandler(WebInterfaceServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			LOGGER.debug("Handling web player server file request \"{}\"", t.getRequestURI());
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(t, "");
			}
			String basePath = "/configuration/";
			String path = t.getRequestURI().getPath();
			String relativePath = path.substring(basePath.length());
			String response = null;
			String mime = null;
			int status = 200;
			if (StringUtils.isEmpty(relativePath)) {
				relativePath = "/index.html";
			}

			if (parent.getResources().write("react-app/" + relativePath, t)) {
				// The resource manager found and sent the file, all done.
				return;
			} else {
				status = 404;
			}

			if (status == 404 && response == null) {
				response = "<html><body>404 - File Not Found: " + path + "</body></html>";
				mime = "text/html";
			}

			WebInterfaceServerUtil.respond(t, response, status, mime);
		} catch (IOException e) {
			throw e;
		} catch (MustacheException e) {
			// Nothing should get here, this is just to avoid crashing the
			// thread
			LOGGER.error("Unexpected error in ConfigurationClientHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
