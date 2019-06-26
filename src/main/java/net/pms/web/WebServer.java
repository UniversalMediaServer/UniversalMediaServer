package net.pms.web;

import org.jboss.resteasy.plugins.guice.ModuleProcessor;
import org.jboss.resteasy.plugins.server.netty.NettyJaxrsServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.remote.RemoteUtil.ResourceManager;

public class WebServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);

	static final String APPLICATION_PATH = "/api";

	static final String CONTEXT_ROOT = "/";

	private NettyJaxrsServer netty;

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
	
	public NettyJaxrsServer getServer() {
		return netty;
	}

	public void start() throws Exception {
		PmsConfiguration configuration = injector.getInstance(PmsConfiguration.class);

		netty = new NettyJaxrsServer();
		netty.setRootResourcePath("");
		netty.setSecurityDomain(null);
		
		
		if (configuration.getWebHttps()) {
			// todo
		} else {
			netty.setPort(port);
		}

		netty.start();

		ModuleProcessor processor = new ModuleProcessor(netty.getDeployment().getRegistry(), netty.getDeployment().getProviderFactory());
		processor.processInjector(injector);
	}

	public void stop() throws Exception {
		if (netty != null) {
			netty.stop();
		}
	}

	public String getAddress() {
		return PMS.get().getServer().getHost() + ":" + port;
	}

	public ResourceManager getResources() {
		return injector.getInstance(ResourceManager.class);
	}

	public String getUrl() {
		if (netty != null) {
			return (injector.getInstance(PmsConfiguration.class).getWebHttps() ? "https://" : "http://")
					+ PMS.get().getServer().getHost() + ":" + port;
		}
		return null;
	}
}
