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

import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.SocketException;
import net.pms.PMS;
import org.jupnp.model.UnsupportedDataException;
import org.jupnp.transport.impl.MulticastReceiverConfigurationImpl;
import org.jupnp.transport.impl.MulticastReceiverImpl;
import org.jupnp.transport.spi.MulticastReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UmsMulticastReceiver extends MulticastReceiverImpl {
	private static final Logger LOGGER = LoggerFactory.getLogger(MulticastReceiver.class);

	public UmsMulticastReceiver(MulticastReceiverConfigurationImpl configuration) {
		super(configuration);
	}

	@Override
	public void run() {
		LOGGER.debug("Entering blocking receiving loop, listening for UDP datagrams on: " + socket.getLocalAddress());
		while (true) {

			try {
				byte[] buf = new byte[getConfiguration().getMaxDatagramBytes()];
				DatagramPacket datagram = new DatagramPacket(buf, buf.length);

				socket.receive(datagram);

				//check inetAddress allowed
				if (!PMS.getConfiguration().getIpFiltering().allowed(datagram.getAddress())) {
					LOGGER.trace("Ip Filtering denying address: {}", datagram.getAddress().getHostAddress());
					continue;
				}

				InetAddress receivedOnLocalAddress =
						networkAddressFactory.getLocalAddress(
								multicastInterface,
								multicastAddress.getAddress() instanceof Inet6Address,
								datagram.getAddress()
						);

				LOGGER.debug(
						"UDP datagram received from: " + datagram.getAddress().getHostAddress() +
						":" + datagram.getPort() +
						" on local interface: " + multicastInterface.getDisplayName() +
						" and address: " + receivedOnLocalAddress.getHostAddress()
				);

				router.received(datagramProcessor.read(receivedOnLocalAddress, datagram));

			} catch (SocketException ex) {
				LOGGER.debug("Socket closed");
				break;
			} catch (UnsupportedDataException ex) {
				LOGGER.info("Could not read datagram: " + ex.getMessage());
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		}
		try {
			if (!socket.isClosed()) {
				LOGGER.debug("Closing multicast socket");
				socket.close();
			}
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
}
