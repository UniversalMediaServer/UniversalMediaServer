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

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.ServletException;
import net.pms.network.httpserverservletcontainer.HttpServerServletContainer;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.servlets.AboutApiServlet;
import net.pms.network.webguiserver.servlets.I18nApiServlet;
import net.pms.network.webguiserver.servlets.PlayerApiServlet;
import net.pms.network.webinterfaceserver.OldPlayerServer;
import net.pms.network.webplayerserver.servlets.PlayerAuthApiServlet;
import net.pms.network.webplayerserver.servlets.WebPlayerServlet;
import net.pms.util.FileUtil;

@SuppressWarnings("restriction")
public class WebPlayerServerHttpServer extends WebPlayerServer {

	private HttpServer server;

	public WebPlayerServerHttpServer() throws IOException {
		this(DEFAULT_PORT);
	}

	public WebPlayerServerHttpServer(int port) throws IOException {
		if (port < 0) {
			port = DEFAULT_PORT;
		}

		// Setup the socket address
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);
		// Initialize the HTTP(S) server
		if (CONFIGURATION.getWebPlayerHttps()) {
			try {
				server = httpsServer(address);
			} catch (IOException e) {
				LOGGER.error("Failed to start web player server ({}) on HTTPS: {}", address, e.getMessage());
				LOGGER.trace("", e);
				if (e.getMessage().contains("UMS.jks")) {
					LOGGER.info(
							"To enable HTTPS please generate a self-signed keystore file " +
							"called \"UMS.jks\" with password \"umsums\" using the java " +
							"'keytool' commandline utility, and place it in the profile folder"
					);
				}
			} catch (GeneralSecurityException e) {
				LOGGER.error("Failed to start web player server ({}) on HTTPS due to a security error: {}", address, e.getMessage());
				LOGGER.trace("", e);
			}
		} else {
			try {
				server = HttpServer.create(address, 0);
			} catch (IOException e) {
				LOGGER.error("Failed to start web player server ({}) : {}", address, e.getMessage());
			}
		}

		if (server != null) {
			int threads = CONFIGURATION.getWebThreads();
			HttpServerServletContainer container = new HttpServerServletContainer(server, "file:" + CONFIGURATION.getWebPath() + "/react-client/");
			try {
				container.createServlet(AboutApiServlet.class);
				container.createServlet(I18nApiServlet.class);
				container.createServlet(PlayerApiServlet.class);
				container.createServlet(PlayerAuthApiServlet.class);
				container.createServlet(WebPlayerServlet.class);
			} catch (ServletException ex) {
				LOGGER.error(ex.getMessage());
			}
			OldPlayerServer.plug(server);
			server.setExecutor(Executors.newFixedThreadPool(threads));
			server.start();
		}
	}

	@Override
	public HttpServer getServer() {
		return server;
	}

	@Override
	public int getPort() {
		if (server != null) {
			return server.getAddress().getPort();
		}
		return 0;
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
		return server instanceof HttpsServer;
	}

	/**
	 * Stop the current server.
	 * Once stopped, a {@code HttpServer} cannot be re-used.
	 */
	@Override
	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	private static HttpServer httpsServer(InetSocketAddress address) throws IOException, GeneralSecurityException {
		// Initialize the keystore
		char[] password = "umsums".toCharArray();
		KeyStore keyStore = KeyStore.getInstance("JKS");
		try (FileInputStream fis = new FileInputStream(FileUtil.appendPathSeparator(CONFIGURATION.getProfileDirectory()) + "UMS.jks")) {
			keyStore.load(fis, password);
		}

		// Setup the key manager factory
		KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, password);

		// Setup the trust manager factory
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(keyStore);

		HttpsServer httpsServer = HttpsServer.create(address, 0);
		SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

		httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {

			@Override
			public void configure(HttpsParameters params) {
				try {
					// initialise the SSL context
					SSLContext c = SSLContext.getDefault();
					SSLEngine engine = c.createSSLEngine();
					params.setNeedClientAuth(true);
					params.setCipherSuites(engine.getEnabledCipherSuites());
					params.setProtocols(engine.getEnabledProtocols());

					// get the default parameters
					SSLParameters defaultSSLParameters = c.getDefaultSSLParameters();
					params.setSSLParameters(defaultSSLParameters);
				} catch (NoSuchAlgorithmException e) {
					LOGGER.debug("https configure error  " + e);
				}
			}
		});
		return httpsServer;
	}

	public static WebPlayerServerHttpServer createServer(int port) throws IOException {
		LOGGER.debug("Using httpserver as gui server");
		return new WebPlayerServerHttpServer(port);
	}
}
