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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.LibraryScanner;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.util.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleHandler.class);

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		try {
			InetAddress ia = exchange.getRemoteAddress().getAddress();
			if (WebInterfaceServerUtil.deny(ia)) {
				exchange.close();
				return;
			}
			exchange.getResponseHeaders().set("Server", PMS.get().getServerName());
			String command = "";
			int pos = exchange.getRequestURI().getPath().indexOf("console/");
			if (pos != -1) {
				command = exchange.getRequestURI().getPath().substring(pos + "console/".length());
			}
			WebInterfaceServerUtil.respond(exchange, servePage(command), 200, "text/html");
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ConsoleHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public static String servePage(String command) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><meta charset=\"utf-8\"><title>").append(PropertiesUtil.getProjectProperties().get("project.name")).append(" HTML Console</title></head><body>");

		PmsConfiguration configuration = PMS.getConfiguration();

		if (command.equals("scan") && configuration.getUseCache()) {
			if (!LibraryScanner.isScanLibraryRunning()) {
				LibraryScanner.scanLibrary();
			}
			if (LibraryScanner.isScanLibraryRunning()) {
				sb.append("<p style=\"margin: 0 auto; text-align: center;\"><b>Scan in progress! you can also <a href=\"stop\">stop it</a></b></p><br>");
			}
		}

		if (command.equals("stop") && configuration.getUseCache() && LibraryScanner.isScanLibraryRunning()) {
			LibraryScanner.stopScanLibrary();
			sb.append("<p style=\"margin: 0 auto; text-align: center;\"><b>Scan stopped!</b></p><br>");
		}

		sb.append("<p style=\"margin: 0 auto; text-align: center;\"><img src='/images/logo.png'><br>").append(PropertiesUtil.getProjectProperties().get("project.name")).append(" HTML console<br><br>Menu:<br>");
		sb.append("<a href=\"home\">Home</a><br>");
		sb.append("<a href=\"scan\">Scan folders</a><br>");
		sb.append("</p></body></html>");
		return sb.toString();
	}
}
