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

import net.pms.network.mediaserver.MediaServer;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.spi.InitializationException;

/**
 * NetworkAddressFactory that use MediaServer config to find the upnp interface, ip and port.
 */
public class UmsNetworkAddressFactory extends NetworkAddressFactoryImpl {

	public UmsNetworkAddressFactory() throws InitializationException {
		this(MediaServer.getPort(), DEFAULT_MULTICAST_RESPONSE_LISTEN_PORT);
	}

	public UmsNetworkAddressFactory(int streamListenPort, int multicastResponsePort) throws InitializationException {
		this.streamListenPort = streamListenPort;
		this.multicastResponsePort = multicastResponsePort;
	}

	@Override
	protected void discoverNetworkInterfaces() throws InitializationException {
		synchronized (networkInterfaces) {
			networkInterfaces.add(MediaServer.getNetworkInterface());
		}
	}

	@Override
	protected void discoverBindAddresses() throws InitializationException {
		synchronized (bindAddresses) {
			bindAddresses.add(MediaServer.getInetAddress());
		}
	}
}
