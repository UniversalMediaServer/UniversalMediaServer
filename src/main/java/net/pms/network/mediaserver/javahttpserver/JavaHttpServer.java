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

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.pms.network.mediaserver.HttpMediaServer;
import net.pms.util.SimpleThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHttpServer extends HttpMediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(JavaHttpServer.class);

	private HttpServer server;

	public JavaHttpServer(InetAddress inetAddress, int port) {
		super(inetAddress, port);
	}

	@Override
	public synchronized boolean start() throws IOException {
		LOGGER.info("Starting HTTP server (JDK HttpServer) on host {} and port {}", hostname, port);
		InetSocketAddress address = new InetSocketAddress(serverInetAddress, port);
		server = HttpServer.create(address, 0);
		if (server != null) {
			hostname = server.getAddress().getAddress().getHostAddress();
			localPort = server.getAddress().getPort();
			server.createContext("/", new RequestHandler());
			server.createContext("/api", new ApiHandler());
			ExecutorService executorService = Executors.newCachedThreadPool(
				new SimpleThreadFactory("HTTPv3 Request Handler", "HttpServer Requests group")
			);
			server.setExecutor(executorService);
			server.start();
			LOGGER.info("HTTP server started on host {} and port {}", hostname, localPort);
			return true;
		}
		return false;
	}

	@Override
	public synchronized void stop() {
		LOGGER.info("Stopping HTTP server (JDK HttpServer) on host {} and port {}...", hostname, localPort);
		if (server != null) {
			server.stop(0);
			server = null;
		}
		LOGGER.info("HTTP server stopped");
	}

}
