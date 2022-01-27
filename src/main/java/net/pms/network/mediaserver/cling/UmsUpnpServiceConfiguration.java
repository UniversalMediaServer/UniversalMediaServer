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
package net.pms.network.mediaserver.cling;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.cling.transport.impl.JdkHttpURLConnectionStreamClient;
import net.pms.network.mediaserver.cling.transport.impl.EmptyStreamServer;
import net.pms.network.mediaserver.cling.transport.impl.JdkHttpURLConnectionStreamClientConfiguration;
import net.pms.network.mediaserver.cling.transport.impl.UmsNetworkAddressFactory;
import org.fourthline.cling.DefaultUpnpServiceConfiguration;
import org.fourthline.cling.model.ServerClientTokens;
import org.fourthline.cling.model.message.UpnpHeaders;
import org.fourthline.cling.model.message.header.UpnpHeader;
import org.fourthline.cling.model.meta.RemoteDeviceIdentity;
import org.fourthline.cling.transport.impl.StreamServerConfigurationImpl;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.fourthline.cling.transport.spi.StreamClient;
import org.fourthline.cling.transport.spi.StreamServer;

public class UmsUpnpServiceConfiguration extends DefaultUpnpServiceConfiguration {

	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final UpnpHeaders umsHeaders;

	public UmsUpnpServiceConfiguration() {
		super();
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

	@Override
	public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
		return new EmptyStreamServer(
				new StreamServerConfigurationImpl(
						networkAddressFactory.getStreamListenPort()
				)
		);
	}

	@Override
	public NetworkAddressFactory createNetworkAddressFactory() {
		return new UmsNetworkAddressFactory();
	}
}
