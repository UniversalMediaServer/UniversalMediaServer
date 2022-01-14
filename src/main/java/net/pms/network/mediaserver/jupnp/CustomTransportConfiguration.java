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
package net.pms.network.mediaserver.jupnp;

import java.util.concurrent.ExecutorService;
import org.jupnp.transport.TransportConfiguration;
import org.jupnp.transport.impl.jetty.StreamClientConfigurationImpl;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamClientConfiguration;
import org.jupnp.transport.spi.StreamServer;

/**
 * TransportConfiguration with a StreamClient but not StreamServer. UMS will
 * handle http server, jupnp the datagram and RemoteDevices
 */
public class CustomTransportConfiguration implements TransportConfiguration {

	@Override
	public StreamClient createStreamClient(final ExecutorService executorService, final StreamClientConfiguration configuration) {
		//we have a dependency on apache httpclient, so use it until we can use java 11+
		StreamClientConfigurationImpl clientConfiguration = new StreamClientConfigurationImpl(
				executorService,
				30
		);
		return new ApacheStreamClientImpl(clientConfiguration);
	}

	@Override
	public StreamServer createStreamServer(final int listenerPort) {
		return new EmptyStreamServerImpl(
				new StreamServerConfigurationImpl(
						listenerPort
				)
		);
	}

}
