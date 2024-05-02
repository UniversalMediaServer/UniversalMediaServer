/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.mediaserver.javahttpserver;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.InetAddress;
import net.pms.network.NetworkDeviceFilter;
import net.pms.network.mediaserver.MediaServer;

public class RequestHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		InetAddress ia = exchange.getRemoteAddress().getAddress();

		// Filter if required
		if (!NetworkDeviceFilter.isAllowed(ia)) {
			exchange.close();
			return;
		}

		String uri = exchange.getRequestURI().getPath();
		if (uri.startsWith("/ums/")) {
			new MediaServerHandler().handle(exchange);
		} else if (uri.startsWith("/api/")) {
			new NextcpApiHandler().handle(exchange);
		} else if (uri.startsWith("/dev/")) {
			//This is the contendirectory service that can (should) be handled directly by JUPnP
			new ContentDirectoryHandler().handle(exchange);
		} else {
			sendErrorResponse(exchange, 404);
		}
	}

	private static void sendErrorResponse(final HttpExchange exchange, int code) throws IOException {
		exchange.getResponseHeaders().set("Server", MediaServer.getServerName());
		exchange.sendResponseHeaders(code, 0);
	}

}
