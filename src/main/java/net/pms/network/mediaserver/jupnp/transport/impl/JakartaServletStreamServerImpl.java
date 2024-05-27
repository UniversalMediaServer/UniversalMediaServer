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

import jakarta.servlet.Servlet;
import java.io.IOException;
import java.net.InetAddress;
import net.pms.network.mediaserver.jupnp.transport.async.JakartaAsyncServlet;
import org.jupnp.transport.Router;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.StreamServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet stream server implementation.
 *
 * @author Christian Bauer - Initial contribution to work with Servlet 3.0
 * @author Surf@ceS - Added support for JakartaEE 9+ Servlet
 */
public class JakartaServletStreamServerImpl implements StreamServer<JakartaServletStreamServerConfigurationImpl> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JakartaServletStreamServerImpl.class);

	protected final JakartaServletStreamServerConfigurationImpl configuration;
	protected int localPort;

	public JakartaServletStreamServerImpl(JakartaServletStreamServerConfigurationImpl configuration) {
		this.configuration = configuration;
	}

	@Override
	public JakartaServletStreamServerConfigurationImpl getConfiguration() {
		return configuration;
	}

	@Override
	public synchronized void init(InetAddress bindAddress, final Router router) throws InitializationException {
		try {
			LOGGER.debug("Setting executor service on servlet container adapter");
			configuration.getServletContainerAdapter().setExecutorService(router.getConfiguration().getStreamServerExecutorService());

			LOGGER.debug("Adding connector: {}:{}", bindAddress, configuration.getListenPort());
			localPort = configuration.getServletContainerAdapter().addConnector(bindAddress.getHostAddress(), configuration.getListenPort());

			String contextPath = router.getConfiguration().getNamespace().getBasePath().getPath();

			Servlet servlet = createAsyncServlet(router);
			configuration.getServletContainerAdapter().registerServlet(contextPath, servlet);
		} catch (IOException e) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName(), e);
		}
	}

	@Override
	public synchronized int getPort() {
		return this.localPort;
	}

	@Override
	public synchronized void stop() {
		configuration.getServletContainerAdapter().stopIfRunning();
	}

	@Override
	public void run() {
		configuration.getServletContainerAdapter().startIfNotRunning();
	}

	protected Servlet createAsyncServlet(final Router router) {
		return new JakartaAsyncServlet(router, configuration);
	}

}
