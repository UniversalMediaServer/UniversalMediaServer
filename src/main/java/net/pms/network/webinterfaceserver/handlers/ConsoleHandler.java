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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.service.LibraryScanner;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import net.pms.util.DbgPacker;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final WebInterfaceServerHttpServer parent;

	public ConsoleHandler(final WebInterfaceServerHttpServer parent) {
		this.parent = parent;
		// Make sure logs are available right away
		getLogs(false);
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
			mustacheVars.put("logs", getLogs(true));
			if (CONFIGURATION.getUseCache()) {
				int pos = t.getRequestURI().getPath().indexOf("scan/");
				if (pos != -1) {
					String scanCommand = t.getRequestURI().getPath().substring(pos + "scan/".length());
					if (scanCommand.equals("start")) {
						if (!LibraryScanner.isScanLibraryRunning()) {
							LibraryScanner.scanLibrary();
						}
					} else if (scanCommand.equals("stop") && LibraryScanner.isScanLibraryRunning()) {
						LibraryScanner.stopScanLibrary();
					}
				}
				if (LibraryScanner.isScanLibraryRunning()) {
					mustacheVars.put("scanLibrary", "<b>Scan in progress!</b><br>You can <b><a href=\"/console/scan/stop\">stop it</a></b><br>");
				} else {
					mustacheVars.put("scanLibrary", "<b>Scan stopped!</b><br>You can <b><a href=\"/console/scan/start\">Start it</a></b><br>");
				}
			}
			mustacheVars.put("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));
			String response = parent.getResources().getTemplate("console.html").execute(mustacheVars);
			WebInterfaceServerUtil.respond(t, response, 200, "text/html");
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
			WebInterfaceServerUtil.respond(t, "", 500, "text/html");
			throw e;
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
