/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.network;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpAdminServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpAdminServer.class);
	private final HttpServerSentEventsServlet seeServer;
	private final Server server;
	public HttpAdminServer() {
		seeServer = new HttpServerSentEventsServlet();
		server = new Server(9999);
		ServletHandler servletHandler = new ServletHandler();
		server.setHandler(servletHandler);
		ServletHolder servletHolder = new ServletHolder(HttpServerSentEventsServlet.class);
		servletHandler.addServletWithMapping(servletHolder, "/admin/async");
		try {
			server.start();
			server.join();
		} catch (Exception e) {
			LOGGER.trace("", e);
		}
	}

	public void sendEvent(String event, String data) {
		seeServer.broadcast(event, data);
	}
}
