/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.util.HashMap;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocHandler.class);

	private final WebInterfaceServerHttpServer parent;

	public DocHandler(final WebInterfaceServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			LOGGER.debug("root req " + t.getRequestURI());
			if (WebInterfaceServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(t, "");
			}
			if (t.getRequestURI().getPath().contains("favicon")) {
				WebInterfaceServerUtil.sendLogo(t);
				return;
			}

			HashMap<String, Object> mustacheVars = new HashMap<>();
			mustacheVars.put("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));
			String response = parent.getResources().getTemplate("doc.html").execute(mustacheVars);
			WebInterfaceServerUtil.respond(t, response, 200, "text/html");
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in DocHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
			WebInterfaceServerUtil.respond(t, "", 500, "text/html");
			throw e;
		}
	}
}
