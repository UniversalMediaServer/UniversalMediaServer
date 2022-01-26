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

import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.cling.transport.impl.JdkHttpServerStreamServer;
import net.pms.network.mediaserver.cling.transport.impl.NettyStreamServer;
import net.pms.network.mediaserver.cling.transport.impl.UmsStreamServerConfiguration;
import org.jupnp.transport.spi.NetworkAddressFactory;
import org.jupnp.transport.spi.StreamServer;

public class UmsServerUpnpServiceConfiguration extends UmsNoServerUpnpServiceConfiguration {

	@Override
	public StreamServer createStreamServer(NetworkAddressFactory networkAddressFactory) {
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
		return null;
	}

}
