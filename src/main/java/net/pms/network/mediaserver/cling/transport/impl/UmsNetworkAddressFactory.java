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
package net.pms.network.mediaserver.cling.transport.impl;

import java.net.InetAddress;
import java.net.UnknownHostException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.NetworkConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.transport.impl.NetworkAddressFactoryImpl;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NoNetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NetworkAddressFactory that use ums config to find the upnp httpserver ip /
 * port.
 */
public class UmsNetworkAddressFactory extends NetworkAddressFactoryImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(UmsNetworkAddressFactory.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * Defaults to an configuration port.
	 */
	public UmsNetworkAddressFactory() throws InitializationException {
		this(CONFIGURATION.getServerPort());
	}

	public UmsNetworkAddressFactory(int streamListenPort) throws InitializationException {
		networkInterfaces.clear();
		bindAddresses.clear();
		String hostname = CONFIGURATION.getServerHostname();
		if (StringUtils.isNotBlank(hostname)) {
			InetAddress hostnameInetAddress = null;
			try {
				hostnameInetAddress = InetAddress.getByName(hostname);
			} catch (UnknownHostException ex) {
			}
			if (hostnameInetAddress != null) {
				LOGGER.info("Using forced address " + hostname);
				useAddresses.add(hostnameInetAddress.getHostAddress());
			}
		}
		if (useAddresses.isEmpty()) {
			String networkInterfaceName = CONFIGURATION.getNetworkInterface();
			NetworkConfiguration.InterfaceAssociation ia = null;
			if (StringUtils.isNotEmpty(networkInterfaceName)) {
				ia = NetworkConfiguration.getInstance().getAddressForNetworkInterfaceName(networkInterfaceName);
			}
			if (ia == null) {
				ia = NetworkConfiguration.getInstance().getDefaultNetworkInterfaceAddress();
			}
			if (ia != null) {
				useAddresses.add(ia.getAddr().getHostAddress());
				useInterfaces.add(ia.getIface().getName());
			} else if (StringUtils.isNotEmpty(networkInterfaceName)) {
				useInterfaces.add(networkInterfaceName);
			}
		}

		super.discoverNetworkInterfaces();
		super.discoverBindAddresses();

		if ((networkInterfaces.isEmpty() || bindAddresses.isEmpty())) {
			LOGGER.warn("No usable network interface or addresses found");
			if (super.requiresNetworkInterface()) {
				throw new NoNetworkException("Could not discover any usable network interfaces and/or addresses");
			}
		}

		this.streamListenPort = streamListenPort;
	}
}
