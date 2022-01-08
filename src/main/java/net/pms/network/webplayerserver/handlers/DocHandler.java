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
package net.pms.network.webplayerserver.handlers;

import com.samskivert.mustache.MustacheException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.newgui.DbgPacker;
import net.pms.network.webplayerserver.WebPlayerServerUtil;
import net.pms.network.webplayerserver.WebPlayerServerHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	@SuppressWarnings("unused")
	private static final String CRLF = "\r\n";

	private final WebPlayerServerHttpServer parent;

	public DocHandler(final WebPlayerServerHttpServer parent) {
		this.parent = parent;
		// Make sure logs are available right away
		getLogs(false);
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			LOGGER.debug("root req " + t.getRequestURI());
			if (WebPlayerServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			if (t.getRequestURI().getPath().contains("favicon")) {
				WebPlayerServerUtil.sendLogo(t);
				return;
			}

			HashMap<String, Object> vars = new HashMap<>();
			vars.put("logs", getLogs(true));
			if (CONFIGURATION.getUseCache()) {
				vars.put("cache",
					"http://" + PMS.get().getServer().getHost() + ":" + PMS.get().getServer().getPort() + "/console/home");
			}

			String response = parent.getResources().getTemplate("doc.html").execute(vars);
			WebPlayerServerUtil.respond(t, response, 200, "text/html");
		} catch (IOException e) {
			throw e;
		} catch (MustacheException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in DocHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private ArrayList<HashMap<String, String>> getLogs(final boolean asList) {
		Set<File> files = new DbgPacker().getItems();
		if (!asList) {
			return null;
		}
		ArrayList<HashMap<String, String>> logs = new ArrayList<>();
		for (File f : files) {
			if (f.exists()) {
				String id = String.valueOf(parent.getResources().add(f));
				if (asList) {
					HashMap<String, String> item = new HashMap<>();
					item.put("filename", f.getName());
					item.put("id", id);
					logs.add(item);
				}
			}
		}
		return logs;
	}
}
