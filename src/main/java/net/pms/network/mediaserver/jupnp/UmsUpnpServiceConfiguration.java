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

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.jupnp.transport.impl.DummyStreamServer;
import net.pms.network.mediaserver.jupnp.transport.impl.JdkHttpServerStreamServer;
import net.pms.network.mediaserver.jupnp.transport.impl.JdkHttpURLConnectionStreamClient;
import net.pms.network.mediaserver.jupnp.transport.impl.JdkHttpURLConnectionStreamClientConfiguration;
import net.pms.network.mediaserver.jupnp.transport.impl.NettyStreamServer;
import net.pms.network.mediaserver.jupnp.transport.impl.UmsNetworkAddressFactory;
import net.pms.network.mediaserver.jupnp.transport.impl.UmsStreamServerConfiguration;
import org.jupnp.DefaultUpnpServiceConfiguration;
import org.jupnp.model.ServerClientTokens;
import org.jupnp.model.message.UpnpHeaders;
import org.jupnp.model.message.header.UpnpHeader;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamClient;
import org.jupnp.transport.spi.StreamServer;

public class UmsUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final UpnpHeaders umsHeaders;
	private boolean ownHttpServer = false;

	public UmsUpnpServiceConfiguration(boolean ownHttpServer) {
		super();
		this.ownHttpServer = ownHttpServer;
		umsHeaders = new UpnpHeaders();
		umsHeaders.add(UpnpHeader.Type.USER_AGENT.getHttpName(), "UMS/" + PMS.getVersion() + " " + new ServerClientTokens());
	}

	@Override
	public UpnpHeaders getDescriptorRetrievalHeaders(RemoteDeviceIdentity identity) {
		return umsHeaders;
	}

	@Override
	public int getAliveIntervalMillis() {
		return CONFIGURATION.getAliveDelay() != 0 ? CONFIGURATION.getAliveDelay() : 30000;
	}

	@Override
	public StreamClient createStreamClient() {
		return new JdkHttpURLConnectionStreamClient(
				new JdkHttpURLConnectionStreamClientConfiguration(
						getSyncProtocolExecutorService()
				)
		);
	}

	public boolean useOwnHttpServer() {
		return ownHttpServer;
	}

	public void setOwnHttpServer(boolean ownHttpServer) {
		this.ownHttpServer = ownHttpServer;
	}

	@Override
	public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
		if (ownHttpServer) {
			int engineVersion = CONFIGURATION.getServerEngine();
			if (engineVersion == 0 || !MediaServer.VERSIONS.containsKey(engineVersion)) {
				engineVersion = MediaServer.DEFAULT_VERSION;
			}
			switch (engineVersion) {
				case 2:
					return new NettyStreamServer(
							new UmsStreamServerConfiguration(
									networkAddressFactory.getStreamListenPort()
							)
					);
				case 3:
					return new JdkHttpServerStreamServer(
							new UmsStreamServerConfiguration(
									networkAddressFactory.getStreamListenPort()
							)
					);
			}
		}
		return new DummyStreamServer(
				new UmsStreamServerConfiguration(
						networkAddressFactory.getStreamListenPort()
				)
		);
	}

	@Override
	public NetworkAddressFactory createNetworkAddressFactory() {
		return new UmsNetworkAddressFactory();
	}

}
