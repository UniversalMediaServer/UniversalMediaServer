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
package net.pms.network.mediaserver.jupnp.transport.impl;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import net.pms.PMS;
import net.pms.network.mediaserver.javahttpserver.RequestHandler;
import net.pms.network.mediaserver.jupnp.UmsUpnpServiceConfiguration;
import org.jupnp.model.message.Connection;
import org.jupnp.transport.Router;
import org.jupnp.transport.impl.HttpExchangeUpnpStream;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on the built-in SUN JDK HttpServer.
 */
public class JdkHttpServerStreamServer implements StreamServer<UmsStreamServerConfiguration> {

	//base the logger inside org.jupnp.transport.spi.StreamServer to reflect old behavior
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamServer.class);

	final protected UmsStreamServerConfiguration configuration;
	protected HttpServer server;

	public JdkHttpServerStreamServer(UmsStreamServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	synchronized public void init(InetAddress bindAddress, Router router) throws InitializationException {
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

			server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
			server.createContext("/", new RequestHttpHandler(router));

			LOGGER.info("Created server (for receiving TCP streams) on: {}", server.getAddress());

		} catch (IOException ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
		}
	}

	@Override
	synchronized public int getPort() {
		return server.getAddress().getPort();
	}

	@Override
	public UmsStreamServerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	synchronized public void run() {
		LOGGER.debug("Starting StreamServer...");
		// Starts a new thread but inherits the properties of the calling thread
		server.start();
	}

	@Override
	synchronized public void stop() {
		LOGGER.debug("Stopping StreamServer...");
		if (server != null) {
			server.stop(1);
		}
	}

	protected class RequestHttpHandler implements HttpHandler {

		private final Router router;
		private final RequestHandler requestHandler;

		public RequestHttpHandler(Router router) {
			this.router = router;
			if (router.getConfiguration() instanceof UmsUpnpServiceConfiguration &&
				((UmsUpnpServiceConfiguration) router.getConfiguration()).useOwnHttpServer()) {
				requestHandler = new RequestHandler();
			} else {
				requestHandler = null;
			}
		}

		// This is executed in the request receiving thread!
		@Override
		public void handle(final HttpExchange httpExchange) throws IOException {
			//check inetAddress allowed
			if (!PMS.getConfiguration().getIpFiltering().allowed(httpExchange.getRemoteAddress().getAddress())) {
				LOGGER.trace("Ip Filtering denying address: {}", httpExchange.getRemoteAddress().getAddress().getHostAddress());
				httpExchange.close();
				return;
			}
			if (requestHandler != null) {
				String uri = httpExchange.getRequestURI().getPath();
				//check if we want to let JUPnP handle the uri or not.

				//JUPnP use it's own uri formatter ("/dev/<udn>/", "/svc/<udn>/")
				//if not pass it to RequestV3
				if (!uri.startsWith("/dev") && !uri.startsWith("/svc")) {
					requestHandler.handle(httpExchange);
					return;
				}
				//lastly we want UMS to respond it's own devices desc service.
				if (uri.startsWith("/dev/" + PMS.get().udn())) {
					requestHandler.handle(httpExchange);
					return;
				}
			}
			// Here everything is handle by JUPnP
			// And we pass control to the service, which will (hopefully) start a new thread immediately so we can
			// continue the receiving thread ASAP
			LOGGER.debug("Received HTTP exchange: {} {}", httpExchange.getRequestMethod(), httpExchange.getRequestURI());
			router.received(
					new HttpExchangeUpnpStream(router.getProtocolFactory(), httpExchange) {
				@Override
				protected Connection createConnection() {
					return new HttpServerConnection(httpExchange);
				}
			}
			);
		}
	}

	protected class HttpServerConnection implements Connection {

		protected HttpExchange exchange;

		public HttpServerConnection(HttpExchange exchange) {
			this.exchange = exchange;
		}

		@Override
		public boolean isOpen() {
			LOGGER.trace("Can't check client connection, socket access impossible on JDK webserver!");
			return true;
		}

		@Override
		public InetAddress getRemoteAddress() {
			return exchange.getRemoteAddress() != null ? exchange.getRemoteAddress().getAddress() : null;
		}

		@Override
		public InetAddress getLocalAddress() {
			return exchange.getLocalAddress() != null ? exchange.getLocalAddress().getAddress() : null;
		}
	}
}
