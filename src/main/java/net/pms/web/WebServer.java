package net.pms.web;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.EnumSet;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.servlet.DispatcherType;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.FilterDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.remote.RemoteUtil.ResourceManager;
import net.pms.util.FileUtil;

public class WebServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

	static final String APPLICATION_PATH = "/api";

	static final String CONTEXT_ROOT = "/";

	private Server server;

	private int port;

	private Injector injector;

	public WebServer(int port, Module... modules) {
		this.port = port;
		injector = Guice.createInjector(new WebModule());
		injector.getAllBindings();
		injector.createChildInjector().getAllBindings();
	}

	public WebServer(int port) {
		this(port, new WebModule());
	}

	public void start() throws Exception {
		PmsConfiguration configuration = injector.getInstance(PmsConfiguration.class);
		if (configuration.getWebHttps()) {
			server = new Server();
			server.addConnector(createServerConnector(configuration));
		} else {
			server = new Server(port);
		}

		ServletContextHandler servletHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		servletHandler.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));

		servletHandler.addServlet(DefaultServlet.class, "/");
		servletHandler.setContextPath("/");
		servletHandler.setBaseResource(Resource.newClassPathResource("/resources/web"));
		FilterHolder fh = new FilterHolder(FilterDispatcher.class);
		servletHandler.addFilter(fh, "/*", EnumSet.of(DispatcherType.REQUEST));

		server.setHandler(servletHandler);
		server.start();
	}

	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	public String getAddress() {
		return PMS.get().getServer().getHost() + ":" + port;
	}

	public ResourceManager getResources() {
		return injector.getInstance(ResourceManager.class);
	}

	public String getUrl() {
		if (server != null) {
			return (injector.getInstance(PmsConfiguration.class).getWebHttps() ? "https://" : "http://")
					+ PMS.get().getServer().getHost() + ":" + port;
		}
		return null;
	}

	private ServerConnector createServerConnector(PmsConfiguration configuration) throws Exception {
		try {
			// Initialize the keystore
			char[] password = "umsums".toCharArray();
			KeyStore keyStore = KeyStore.getInstance("JKS");
			try (FileInputStream fis = new FileInputStream(
					FileUtil.appendPathSeparator(configuration.getProfileDirectory()) + "DMS.jks")) {
				keyStore.load(fis, password);
			}

			// Setup the key manager factory
			KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
			keyManagerFactory.init(keyStore, password);

			// Setup the trust manager factory
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
			trustManagerFactory.init(keyStore);

			// SSL Context Factory
			SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
			sslContextFactory.setKeyStore(keyStore);
			sslContextFactory.setKeyManagerPassword(new String(password));
			sslContextFactory.setTrustStore(keyStore);
			sslContextFactory.setTrustStorePassword(new String(password));
			sslContextFactory.setExcludeCipherSuites("SSL_RSA_WITH_DES_CBC_SHA", "SSL_DHE_RSA_WITH_DES_CBC_SHA",
					"SSL_DHE_DSS_WITH_DES_CBC_SHA", "SSL_RSA_EXPORT_WITH_RC4_40_MD5",
					"SSL_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA",
					"SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA");

			HttpConfiguration http_config = new HttpConfiguration();
			http_config.setSecureScheme("https");
			http_config.setSecurePort(port);

			// SSL HTTP Configuration
			HttpConfiguration https_config = new HttpConfiguration(http_config);
			https_config.addCustomizer(new SecureRequestCustomizer());

			// SSL Connector
			return new ServerConnector(server,
					new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
					new HttpConnectionFactory(https_config));
		} catch (IOException e) {
			LOGGER.error("Failed to start WEB interface on HTTPS: {}", e.getMessage());
			LOGGER.trace("", e);
			if (e.getMessage().contains("UMS.jks")) {
				LOGGER.info("To enable HTTPS please generate a self-signed keystore file "
						+ "called \"DMS.jks\" with password \"dmsdms\" using the java "
						+ "'keytool' commandline utility, and place it in the profile folder");
			}
			throw e;
		} catch (GeneralSecurityException e) {
			LOGGER.error("Failed to start WEB interface on HTTPS due to a security error: {}", e.getMessage());
			LOGGER.trace("", e);
			throw e;
		}
	}

	public Server getServer() {
		return server;
	}

	public static void main(String[] args) throws Exception {
		WebServer ws = new WebServer(8080);
		ws.start();
		ws.server.join();
	}
}
