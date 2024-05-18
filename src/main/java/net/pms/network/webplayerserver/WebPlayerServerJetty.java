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
package net.pms.network.webplayerserver;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.servlets.AboutApiServlet;
import net.pms.network.webguiserver.servlets.I18nApiServlet;
import net.pms.network.webguiserver.servlets.PlayerApiServlet;
import net.pms.network.webplayerserver.servlets.PlayerAuthApiServlet;
import net.pms.network.webplayerserver.servlets.WebPlayerServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

public class WebPlayerServerJetty extends WebPlayerServer {

	private Server server;

	public WebPlayerServerJetty() throws IOException {
		this(DEFAULT_PORT);
	}

	public WebPlayerServerJetty(int port) throws IOException {
		if (port < 0) {
			port = CONFIGURATION.getWebPlayerServerPort();
		}

		// Setup the socket address
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("webplayer-server");
		server = new Server(threadPool);
		ServerConnector connector = new ServerConnector(server, new HttpConnectionFactory(), new HTTP2CServerConnectionFactory());
		connector.setHost("0.0.0.0");
		connector.setPort(port);
		//let some time for pausing from media renderer (2 hours)
		connector.setIdleTimeout(2 * 60 * 60 * 1000);
		server.addConnector(connector);
		ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
		addServlet(servletHandler, AboutApiServlet.class);
		addServlet(servletHandler, I18nApiServlet.class);
		addServlet(servletHandler, PlayerApiServlet.class);
		addServlet(servletHandler, PlayerAuthApiServlet.class);
		addServlet(servletHandler, WebPlayerServlet.class);
		server.setHandler(servletHandler);
		try {
			start();
		} catch (Exception e) {
			LOGGER.error("Failed to start web player server ({}) : {}", address, e.getMessage());
		}
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public int getPort() {
		NetworkConnector connector = getNetworkConnector();
		return connector != null ? connector.getLocalPort() : 0;
	}

	@Override
	public String getAddress() {
		if (server != null) {
			return MediaServer.getHost() + ":" + getPort();
		}
		return null;
	}

	@Override
	public String getUrl() {
		if (server != null) {
			return (isSecure() ? "https://" : "http://") + getAddress();
		}
		return null;
	}

	@Override
	public boolean isSecure() {
		NetworkConnector connector = getNetworkConnector();
		if (connector != null) {
			String protocol = connector.getDefaultConnectionFactory().getProtocol();
			return (protocol.startsWith("SSL-") || protocol.equals("SSL"));
		}
		return false;
	}

	public final synchronized void start() throws Exception {
		if (server != null && !server.isStarted() && !server.isStarting()) {
			LOGGER.info("Starting Jetty server {}", Server.getVersion());
			try {
				server.start();
			} catch (Exception e) {
				LOGGER.error("Couldn't start Jetty server", e);
				throw e;
			}
		}
	}

	/**
	 * Stop the current server.
	 */
	@Override
	public synchronized void stop() {
		if (server != null && !server.isStopped() && !server.isStopping()) {
			LOGGER.info("Stopping Jetty server...");
			try {
				server.stop();
			} catch (Exception e) {
				LOGGER.error("Couldn't stop Jetty server", e);
			}
		}
	}

	private NetworkConnector getNetworkConnector() {
		for (Connector connector : server.getConnectors()) {
			if (connector instanceof NetworkConnector networkConnector) {
				return networkConnector;
			}
		}
		return null;
	}

	public static WebPlayerServerJetty createServer(int port) throws IOException {
		LOGGER.debug("Using Jetty as web player server");
		return new WebPlayerServerJetty(port);
	}

	private static void addServlet(ServletContextHandler servletHandler, Class<? extends HttpServlet> clazz) {
		WebServlet webServlet = clazz.getAnnotation(WebServlet.class);
		String name = webServlet.name();
		if (name == null) {
			name = clazz.getSimpleName();
		}
		String[] urlPatterns = webServlet.urlPatterns();
		if (urlPatterns.length == 0) {
			urlPatterns = webServlet.value();
		}
		if (urlPatterns.length > 0) {
			for (String urlPattern : urlPatterns) {
				if (!"/".equals(urlPattern) && !urlPattern.endsWith("*")) {
					if (!urlPattern.endsWith("/")) {
						urlPattern += "/";
					}
					urlPattern += "*";
				}
				ServletHolder holder = new ServletHolder(name, clazz);
				servletHandler.addServlet(holder, urlPattern);
			}
		} else {
			LOGGER.debug("Servlet '{}' does not include any pattern.", clazz.getSimpleName());
		}
	}

}
