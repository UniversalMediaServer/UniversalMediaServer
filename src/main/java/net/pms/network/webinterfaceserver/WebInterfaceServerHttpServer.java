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
package net.pms.network.webinterfaceserver;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webinterfaceserver.handlers.BrowseHandler;
import net.pms.network.webinterfaceserver.handlers.ConsoleHandler;
import net.pms.network.webinterfaceserver.handlers.ControlHandler;
import net.pms.network.webinterfaceserver.handlers.DocHandler;
import net.pms.network.webinterfaceserver.handlers.EventStreamHandler;
import net.pms.network.webinterfaceserver.handlers.FileHandler;
import net.pms.network.webinterfaceserver.handlers.MediaHandler;
import net.pms.network.webinterfaceserver.handlers.PlayHandler;
import net.pms.network.webinterfaceserver.handlers.PollHandler;
import net.pms.network.webinterfaceserver.handlers.RawHandler;
import net.pms.network.webinterfaceserver.handlers.StartHandler;
import net.pms.network.webinterfaceserver.handlers.ThumbHandler;
import net.pms.util.FileUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WebInterfaceServerHttpServer extends WebInterfaceServer implements WebInterfaceServerInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebInterfaceServerHttpServer.class);
	private KeyStore keyStore;
	private KeyManagerFactory keyManagerFactory;
	private TrustManagerFactory trustManagerFactory;
	private HttpServer server;
	private SSLContext sslContext;

	public WebInterfaceServerHttpServer() throws IOException {
		this(DEFAULT_PORT);
	}

	public WebInterfaceServerHttpServer(int port) throws IOException {
		if (port <= 0) {
			port = DEFAULT_PORT;
		}

		// Setup the socket address
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

		// Initialize the HTTP(S) server
		if (CONFIGURATION.getWebHttps()) {
			try {
				server = httpsServer(address);
			} catch (IOException e) {
				LOGGER.error("Failed to start web interface server on HTTPS: {}", e.getMessage());
				LOGGER.trace("", e);
				if (e.getMessage().contains("UMS.jks")) {
					LOGGER.info(
							"To enable HTTPS please generate a self-signed keystore file " +
							"called \"UMS.jks\" with password \"umsums\" using the java " +
							"'keytool' commandline utility, and place it in the profile folder"
					);
				}
			} catch (GeneralSecurityException e) {
				LOGGER.error("Failed to start web interface server on HTTPS due to a security error: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		} else {
			server = HttpServer.create(address, 0);
		}

		if (server != null) {
			int threads = CONFIGURATION.getWebThreads();

			// Add context handlers
			addCtx("/", new StartHandler(this));
			addCtx("/browse", new BrowseHandler(this));
			PlayHandler playHandler = new PlayHandler(this);
			addCtx("/play", playHandler);
			addCtx("/playstatus", playHandler);
			addCtx("/playlist", playHandler);
			addCtx("/m3u8", playHandler);
			addCtx("/media", new MediaHandler(this));
			addCtx("/fmedia", new MediaHandler(this, true));
			addCtx("/thumb", new ThumbHandler(this));
			addCtx("/raw", new RawHandler(this));
			addCtx("/files", new FileHandler(this));
			addCtx("/doc", new DocHandler(this));
			addCtx("/poll", new PollHandler(this));
			addCtx("/event-stream", new EventStreamHandler(this));
			addCtx("/bump", new ControlHandler(this));
			addCtx("/console", new ConsoleHandler(this));

			server.setExecutor(Executors.newFixedThreadPool(threads));
			server.start();
		}
	}

	private HttpServer httpsServer(InetSocketAddress address) throws IOException, GeneralSecurityException {
		// Initialize the keystore
		char[] password = "umsums".toCharArray();
		keyStore = KeyStore.getInstance("JKS");
		try (FileInputStream fis = new FileInputStream(FileUtil.appendPathSeparator(CONFIGURATION.getProfileDirectory()) + "UMS.jks")) {
			keyStore.load(fis, password);
		}

		// Setup the key manager factory
		keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
		keyManagerFactory.init(keyStore, password);

		// Setup the trust manager factory
		trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
		trustManagerFactory.init(keyStore);

		HttpsServer httpsServer = HttpsServer.create(address, 0);
		sslContext = SSLContext.getInstance("TLS");
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

	public RootFolder getRoot(String user, HttpExchange t) throws InterruptedException {
		return getRoot(user, false, t);
	}

	public RootFolder getRoot(String user, boolean create, HttpExchange t) throws InterruptedException {
		String cookie = WebInterfaceServerUtil.getCookie("UMS", t);
		boolean setCookie = false;
		RootFolder root = null;
		synchronized (roots) {
			if (cookie != null) {
				root = roots.get(cookie);
			}
			if (root == null) {
				// Double-check for cookie errors
				WebRender valid = WebInterfaceServerUtil.matchRenderer(user, t);
				if (valid != null) {
					// A browser of the same type and user is already connected
					// at
					// this ip but for some reason we didn't get a cookie match.
					RootFolder validRoot = valid.getRootFolder();
					// Do a reverse lookup to see if it's been registered
					for (Map.Entry<String, RootFolder> entry : roots.entrySet()) {
						if (entry.getValue() == validRoot) {
							// Found
							root = validRoot;
							cookie = entry.getKey();
							setCookie = true;
							LOGGER.debug("Allowing browser connection without cookie match: {}: {}", valid.getRendererName(),
									t.getRemoteAddress().getAddress());
							break;
						}
					}
				}
			}

			if (!create || (root != null)) {
				if (setCookie) {
					t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/;SameSite=Strict");
				}
				return root;
			}

			LOGGER.debug("Browser connection {} have no uuid", t.getRemoteAddress().getAddress());
			root = new RootFolder();
			try {
				WebRender render = new WebRender(user);
				root.setDefaultRenderer(render);
				render.setRootFolder(root);
				render.associateIP(t.getRemoteAddress().getAddress());
				render.associatePort(t.getRemoteAddress().getPort());
				if (CONFIGURATION.useWebSubLang()) {
					render.setSubLang(StringUtils.join(WebInterfaceServerUtil.getLangs(t), ","));
				}
				render.setBrowserInfo(WebInterfaceServerUtil.getCookie("UMSINFO", t), t.getRequestHeaders().getFirst("User-agent"));
				PMS.get().setRendererFound(render);
			} catch (ConfigurationException e) {
				root.setDefaultRenderer(RendererConfiguration.getDefaultConf());
			}

			root.discoverChildren();
			cookie = UUID.randomUUID().toString();
			LOGGER.debug("Browser connection set uuid {} to {}", cookie, t.getRemoteAddress().getAddress());
			t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/;SameSite=Strict");
			roots.put(cookie, root);
		}
		return root;
	}

	private void addCtx(String path, HttpHandler h) {
		HttpContext ctx = server.createContext(path, h);
		if (CONFIGURATION.isWebAuthenticate()) {
			ctx.setAuthenticator(new BasicAuthenticator(CONFIGURATION.getServerName()) {
				@Override
				public boolean checkCredentials(String user, String pwd) {
					LOGGER.debug("authenticate " + user);
					return PMS.verifyCred("web", PMS.getCredTag("web", user), user, pwd);
				}
			});
		}
	}

	@Override
	public HttpServer getServer() {
		return server;
	}

	@Override
	public int getPort() {
		return server.getAddress().getPort();
	}

	@Override
	public String getAddress() {
		return MediaServer.getHost() + ":" + getPort();
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

	public static void associate(HttpExchange t, WebRender webRenderer) {
		webRenderer.associateIP(t.getRemoteAddress().getAddress());
		webRenderer.associatePort(t.getRemoteAddress().getPort());
	}

}
