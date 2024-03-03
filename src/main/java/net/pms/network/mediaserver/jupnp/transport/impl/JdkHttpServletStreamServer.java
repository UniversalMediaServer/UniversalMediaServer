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
package net.pms.network.mediaserver.jupnp.transport.impl;

import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import net.pms.network.httpserverservletcontainer.HttpServerServletContainer;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.servlets.MediaServerServlet;
import net.pms.network.mediaserver.servlets.NextcpApiServlet;
import net.pms.network.mediaserver.servlets.UPnPServerServlet;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation based on the built-in SUN JDK HttpServer.
 *
 * @author Surf@ceS
 */
public class JdkHttpServletStreamServer implements StreamServer<UmsStreamServerConfiguration> {

	//base the logger inside org.jupnp.transport.spi.StreamServer to reflect old behavior
	private static final Logger LOGGER = LoggerFactory.getLogger(StreamServer.class);

	protected final UmsStreamServerConfiguration configuration;
	protected HttpServer server;

	public JdkHttpServletStreamServer(UmsStreamServerConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public synchronized void init(InetAddress bindAddress, Router router) throws InitializationException {
		try {
			InetSocketAddress socketAddress = new InetSocketAddress(bindAddress, configuration.getListenPort());

			server = HttpServer.create(socketAddress, configuration.getTcpConnectionBacklog());
			HttpServerServletContainer container = new HttpServerServletContainer(server, "");
			try {
				container.createServlet(MediaServerServlet.class);
				container.createServlet(NextcpApiServlet.class);
				container.createServlet(UPnPServerServlet.class, Router.class, router);
			} catch (ServletException ex) {
				LOGGER.error(ex.getMessage());
			}
			server.setExecutor(router.getConfiguration().getStreamServerExecutorService());
			LOGGER.info("Created server (for receiving TCP streams) on: {}", server.getAddress());
		} catch (IOException ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex.toString(), ex);
		}
	}

	@Override
	public synchronized int getPort() {
		return server.getAddress().getPort();
	}

	@Override
	public UmsStreamServerConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public synchronized void run() {
		LOGGER.debug("Starting StreamServer...");
		// Starts a new thread but inherits the properties of the calling thread
		server.start();
		LOGGER.info("Started StreamServer on: {}", server.getAddress());
		if (configuration.getUpdateMediaServerPort()) {
			MediaServer.setPort(getPort());
		}
	}

	@Override
	public synchronized void stop() {
		LOGGER.debug("Stopping StreamServer...");
		if (server != null) {
			server.stop(0);
		}
	}

}