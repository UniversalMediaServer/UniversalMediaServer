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
package net.pms.network.mediaserver.jupnp.transport.impl.jetty;

import java.util.concurrent.ExecutorService;
import net.pms.network.mediaserver.jupnp.transport.impl.JakartaServletStreamServerConfigurationImpl;
import net.pms.network.mediaserver.jupnp.transport.impl.JakartaServletStreamServerImpl;
import net.pms.network.mediaserver.jupnp.transport.impl.jetty.ee10.JettyServletContainer;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;

/**
 * Implementation of {@link TransportConfiguration} for Jetty Servlet EE10 HTTP
 * components.
 *
 * @author Victor Toni - initial contribution
 * @author Surf@ceS - adapt to Jetty Servlet EE10
 */
public class JettyTransportConfiguration implements TransportConfiguration {

	public static final TransportConfiguration INSTANCE = new JettyTransportConfiguration();

	@Override
	public StreamClient createStreamClient(final ExecutorService executorService,
			final StreamClientConfiguration configuration) {
		StreamClientConfigurationImpl clientConfiguration = new StreamClientConfigurationImpl(executorService,
				configuration.getTimeoutSeconds(), configuration.getLogWarningSeconds(),
				configuration.getRetryAfterSeconds(), configuration.getRetryIterations());

		return new JettyStreamClientImpl(clientConfiguration);
	}

	@Override
	public StreamServer createStreamServer(final int listenerPort) {
		return new JakartaServletStreamServerImpl(
				new JakartaServletStreamServerConfigurationImpl(JettyServletContainer.INSTANCE, listenerPort));
	}
}
