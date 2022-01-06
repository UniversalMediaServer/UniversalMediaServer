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
package net.pms.webserver;

import net.pms.webserver.servlets.*;
import java.io.File;
import java.io.IOException;
import net.pms.PMS;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerJetty extends WebServerServlets implements WebServerInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebServerJetty.class);
	private Server server;
	private boolean isSecure;

	public WebServerJetty(int port) throws IOException {
		if (port <= 0) {
			port = DEFAULT_PORT;
		}

		Log.setLog(new StdErrLog());
		int maxThreads = CONFIGURATION.getWebThreads();
		QueuedThreadPool threadPool = new QueuedThreadPool(maxThreads);
		server = new Server(threadPool);

		// Initialize the HTTP(S) server
		if (CONFIGURATION.getWebHttps()) {
			HttpConfiguration httpsConfiguration = new HttpConfiguration();
			httpsConfiguration.addCustomizer(new SecureRequestCustomizer());
			File keystoreFile = new File("UMS.jks");
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setKeyStorePath(keystoreFile.getAbsolutePath());
			sslContextFactory.setKeyStorePassword("umsums");
			sslContextFactory.setKeyManagerPassword("umsums");
			ServerConnector sslConnector = new ServerConnector(server,
				new SslConnectionFactory(sslContextFactory, "http/1.1"),
				new HttpConnectionFactory(httpsConfiguration));
			sslConnector.setPort(port);
			server.setConnectors(new Connector[] {sslConnector});
			isSecure = true;
		} else {
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(port);
			server.setConnectors(new Connector[] {connector});
			isSecure = false;
		}

		ServletHandler servletHandler = new ServletHandler();
		server.setHandler(servletHandler);

		servletHandler.addServletWithMapping(new ServletHolder("base", new WebServerServlet(this)), "/*");
		servletHandler.addServletWithMapping(new ServletHolder("browse", new BrowseServlet(this)), "/browse/*");
		servletHandler.addServletWithMapping(new ServletHolder("doc", new DocServlet(this)), "/doc/*");
		servletHandler.addServletWithMapping(new ServletHolder("files", new RemoteFileServlet(this)), "/files/*");
		servletHandler.addServletWithMapping(new ServletHolder("fmedia", new MediaServlet(this, true)), "/fmedia/*");
		servletHandler.addServletWithMapping(new ServletHolder("m3u8", new M3u8Servlet(this)), "/m3u8/*");
		servletHandler.addServletWithMapping(new ServletHolder("media", new MediaServlet(this)), "/media/*");
		servletHandler.addServletWithMapping(new ServletHolder("play", new PlayServlet(this)), "/play/*");
		servletHandler.addServletWithMapping(new ServletHolder("playerstatus", new PlayersStatusServlet(this)), "/playerstatus/*");
		servletHandler.addServletWithMapping(new ServletHolder("playlist", new PlayListServlet(this)), "/playlist/*");
		servletHandler.addServletWithMapping(new ServletHolder("poll", new PoolServlet(this)), "/poll/*");
		servletHandler.addServletWithMapping(new ServletHolder("raw", new RawServlet(this)), "/raw/*");
		servletHandler.addServletWithMapping(new ServletHolder("thumb", new ThumbServlet(this)), "/thumb/*");

		if (server != null) {
			try {
				LOGGER.info("Starting jetty web server {}", Server.getVersion());
				server.start();
				LOGGER.info("Web server started");
				//server.join();
			} catch (Exception ex) {
			}
		}
	}

	@Override
	public boolean isSecure() {
		return isSecure;
	}

	@Override
	public Server getServer() {
		return server;
	}

	@Override
	public int getPort() {
		return server.getURI().getPort();
	}

	@Override
	public String getAddress() {
		return PMS.get().getServer().getHost() + ":" + getPort();
	}

	@Override
	public String getUrl() {
		if (server != null) {
			return (isSecure ? "https://" : "http://") + getAddress();
		}
		return null;
	}

	@Override
	public boolean setPlayerControlService() {
		if (server != null) {
			Handler handler = server.getHandler();
			if (handler instanceof ServletHandler) {
				((ServletHandler) handler).addServletWithMapping(new ServletHolder("bump", new PlayerControlServlet(this)), "/bump/*");
				return true;
			}
		}
		return false;
	}
}
