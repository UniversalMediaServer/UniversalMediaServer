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
package net.pms.network.mediaserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.nettyserver.NettyServer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HttpMediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	protected final int port;

	protected String hostname;
	protected InetAddress iafinal;
	protected NetworkInterface networkInterface;

	public HttpMediaServer(int port) {
		this.port = port;
		hostname = CONFIGURATION.getServerHostname();
	}

	public String getURL() {
		return "http://" + hostname + ":" + port;
	}

	public String getHost() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	public boolean start() throws IOException {
		hostname = CONFIGURATION.getServerHostname();
		InetSocketAddress address;

		if (StringUtils.isNotBlank(hostname)) {
			LOGGER.info("Using forced address " + hostname);
			InetAddress tempIA = InetAddress.getByName(hostname);

			if (tempIA != null && networkInterface != null && networkInterface.equals(NetworkInterface.getByInetAddress(tempIA))) {
				address = new InetSocketAddress(tempIA, port);
			} else {
				address = new InetSocketAddress(hostname, port);
			}
		} else if (isAddressFromInterfaceFound(CONFIGURATION.getNetworkInterface())) { // XXX sets iafinal and networkInterface
			LOGGER.info("Using address {} found on network interface: {}", iafinal, networkInterface.toString().trim().replace('\n', ' '));
			address = new InetSocketAddress(iafinal, port);
		} else {
			LOGGER.info("Using localhost address");
			address = new InetSocketAddress(port);
		}
		LOGGER.info("Created socket: {}", address);
		return true;
	}

	// avoid a NPE when a) switching HTTP Engine versions and b) restarting the HTTP server
	// by cleaning up based on what's in use (not null) rather than the config state, which
	// might be inconsistent.
	//
	// NOTE: there's little in the way of cleanup to do here as PMS.reset() discards the old
	// server and creates a new one
	public void stop() {
	}

	public NetworkInterface getNetworkInterface()  {
		return networkInterface;
	}

	protected InetSocketAddress getSocketAddress() throws IOException {
		hostname = CONFIGURATION.getServerHostname();
		InetSocketAddress address;

		if (StringUtils.isNotBlank(hostname)) {
			LOGGER.info("Using forced address " + hostname);
			InetAddress tempIA = InetAddress.getByName(hostname);

			if (tempIA != null && networkInterface != null && networkInterface.equals(NetworkInterface.getByInetAddress(tempIA))) {
				address = new InetSocketAddress(tempIA, port);
			} else {
				address = new InetSocketAddress(hostname, port);
			}
		} else if (isAddressFromInterfaceFound(CONFIGURATION.getNetworkInterface())) { // XXX sets iafinal and networkInterface
			LOGGER.info("Using address {} found on network interface: {}", iafinal, networkInterface.toString().trim().replace('\n', ' '));
			address = new InetSocketAddress(iafinal, port);
		} else {
			LOGGER.info("Using localhost address");
			address = new InetSocketAddress(port);
		}

		LOGGER.info("Created socket: {}", address);
		return address;
	}

	// XXX this sets iafinal and networkInterface
	private boolean isAddressFromInterfaceFound(String networkInterfaceName) {
		NetworkConfiguration.InterfaceAssociation ia = StringUtils.isNotEmpty(networkInterfaceName) ?
			NetworkConfiguration.getInstance().getAddressForNetworkInterfaceName(networkInterfaceName) :
			null;

		if (ia == null) {
			ia = NetworkConfiguration.getInstance().getDefaultNetworkInterfaceAddress();
		}

		if (ia != null) {
			iafinal = ia.getAddr();
			networkInterface = ia.getIface();
		}

		return ia != null;
	}

}
