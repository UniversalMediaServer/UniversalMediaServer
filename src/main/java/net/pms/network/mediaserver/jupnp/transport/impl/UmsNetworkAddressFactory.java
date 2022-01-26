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
package net.pms.network.mediaserver.jupnp.transport.impl;

import net.pms.network.mediaserver.MediaServer;
import org.jupnp.transport.impl.NetworkAddressFactoryImpl;
import org.jupnp.transport.spi.InitializationException;
import org.jupnp.transport.spi.NoNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetworkAddressFactory that use MediaServer config to find the upnp interface, ip and port.
 */
public class UmsNetworkAddressFactory extends NetworkAddressFactoryImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(UmsNetworkAddressFactory.class);

	public UmsNetworkAddressFactory() throws InitializationException {
		networkInterfaces.clear();
		bindAddresses.clear();
		useAddresses.add(MediaServer.getHost());
		useInterfaces.add(MediaServer.getNetworkInterface().getName());
		super.discoverNetworkInterfaces();
		super.discoverBindAddresses();

		if ((networkInterfaces.isEmpty() || bindAddresses.isEmpty())) {
			LOGGER.warn("No usable network interface or addresses found");
			if (super.requiresNetworkInterface()) {
				throw new NoNetworkException("Could not discover any usable network interfaces and/or addresses");
			}
		}

		this.streamListenPort = MediaServer.getPort();
	}
}
