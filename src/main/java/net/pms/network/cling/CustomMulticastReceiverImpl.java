/*
 * Universal Media Server, for streaming any medias to DLNA
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
package net.pms.network.cling;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.logging.Logger;
import net.pms.network.NetworkConfiguration;
import org.fourthline.cling.transport.Router;
import org.fourthline.cling.transport.impl.MulticastReceiverConfigurationImpl;
import org.fourthline.cling.transport.impl.MulticastReceiverImpl;
import org.fourthline.cling.transport.spi.DatagramProcessor;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.slf4j.LoggerFactory;
import com.sun.jna.Platform;

public class CustomMulticastReceiverImpl extends MulticastReceiverImpl {

	private static Logger log = Logger.getLogger(CustomMulticastReceiverImpl.class.getName());
	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(CustomMulticastReceiverImpl.class);

	public CustomMulticastReceiverImpl(MulticastReceiverConfigurationImpl configuration) {
		super(configuration);
	}

	@Override
	synchronized public void init(NetworkInterface networkInterface, Router router, NetworkAddressFactory networkAddressFactory,
		DatagramProcessor datagramProcessor) throws InitializationException {

		this.router = router;
		this.networkAddressFactory = networkAddressFactory;
		this.datagramProcessor = datagramProcessor;
		this.multicastInterface = networkInterface;

		try {

			log.info("Creating socket (for receiving multicast datagrams) on port: " + configuration.getPort());
			multicastAddress = new InetSocketAddress(configuration.getGroup(), configuration.getPort());

			// Trying to bind according to http://stackoverflow.com/a/5484657/363791
			// (interface address for Windows, multicast address for others)
			if (Platform.isWindows()) {
				InetAddress[] interfaceAddresses = NetworkConfiguration.getInstance().getRelevantInterfaceAddresses(multicastInterface);
				if (interfaceAddresses == null) {
					throw new InitializationException(
						"Found no relevant addresses to bind multicast socket to for interface \"" + multicastInterface.getDisplayName() + "\""
					);
				}
				/*
				 * XXX - This is a bug and won't have the desired effect on
				 * interfaces with multiple "relevant" addresses. That
				 * situation is probably relatively rare, and as we can only
				 * bind to one address per socket a proper solution would
				 * mean to spawn one thread per interface/address combination.
				 */
				if (interfaceAddresses.length > 1) {
					StringBuilder stringBuilder = new StringBuilder();
					for (int i = 1; i < interfaceAddresses.length; i++) {
						if (i == 1) {
							stringBuilder.append(interfaceAddresses[i].getHostAddress());
						} else {
							stringBuilder.append(", ").append(interfaceAddresses[i].getHostAddress());
						}
					}
					LOGGER.warn(
						"Ignoring address" + (interfaceAddresses.length > 2 ? "es :" : " :") +
						"{} for network interface \"{}\"",
						stringBuilder,
						multicastInterface.getDisplayName()
					);
				}
				socket = new MulticastSocket(new InetSocketAddress(interfaceAddresses[0], configuration.getPort()));
			} else {
				socket = new MulticastSocket(multicastAddress);
			}

			socket.setReuseAddress(true);
			// Keep a backlog of incoming datagrams if we aren't fast enough
			socket.setReceiveBufferSize(32768);

			log.info("Joining multicast group: " + multicastAddress + " on network interface: " + multicastInterface.getDisplayName());
			socket.joinGroup(multicastAddress, multicastInterface);

		} catch (Exception ex) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + ex);
		}
	}

}
