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
package net.pms.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.transport.spi.InitializationException;
import org.fourthline.cling.transport.spi.NetworkAddressFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.Platform;


public class UPnPListener extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(UPnPListener.class);

	private final PmsConfiguration configuration = PMS.getConfiguration();
	private final NetworkInterface multicastInterface;
	private final InetSocketAddress multicastAddress;
	private final MulticastSocket multicastSocket;
	private final Object closeLock = new Object();
	private final NetworkAddressFactory networkAddressFactory;
	private final List<InetAddress> interfaceAddresses = new ArrayList<>();

	public UPnPListener(NetworkAddressFactory networkAddressFactory, NetworkInterface networkInterface) {
		super("UPnP-Listener " + networkInterface.getDisplayName());
		this.multicastInterface = networkInterface;
		this.networkAddressFactory = networkAddressFactory;

		try {
			LOGGER.trace("Creating listening multicast socket for network interface \"{}\" on port {}", multicastInterface.getDisplayName(), UPNPHelper.UPNP_PORT);
			multicastAddress = new InetSocketAddress(UPNPHelper.IPV4_UPNP_HOST, UPNPHelper.UPNP_PORT);

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
				multicastSocket = new MulticastSocket(new InetSocketAddress(interfaceAddresses[0], UPNPHelper.UPNP_PORT));
			} else {
				multicastSocket = new MulticastSocket(multicastAddress);
			}

			multicastSocket.setReuseAddress(true);
			// Keep a backlog of incoming datagrams if we aren't fast enough
			multicastSocket.setReceiveBufferSize(32768);

			LOGGER.debug("Joining multicast group \"{}\" on network interface \"{}\"", multicastAddress, multicastInterface.getDisplayName());
			multicastSocket.joinGroup(multicastAddress, multicastInterface);

			interfaceAddresses.addAll(Arrays.asList(NetworkConfiguration.getInstance().getRelevantInterfaceAddresses(multicastInterface)));
		} catch (IOException e) {
			throw new InitializationException("Could not initialize " + getClass().getSimpleName() + ": " + e);
		}

	}

	@Override
	public void run() {
		int M_SEARCH = 1, NOTIFY = 2;
		InetAddress lastAddress = null;
		int lastPacketType = 0;

        LOGGER.trace("Entering multicast listening loop, listening for UDP datagrams on: {}", multicastSocket.getLocalAddress().getHostAddress());
		try {
			while (true) {
				byte[] buf = new byte[1024];
				DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);

				multicastSocket.receive(receivePacket);

                InetAddress receivedOnLocalAddress =
                    networkAddressFactory.getLocalAddress(
                        multicastInterface,
                        multicastAddress.getAddress() instanceof Inet6Address,
                        receivePacket.getAddress()
                    );

                // Filter packets for this interface only
                if (receivedOnLocalAddress == null || !interfaceAddresses.contains(receivedOnLocalAddress)) {
                	continue;
                }

				String s = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

				InetAddress address = receivePacket.getAddress();
				int packetType = s.startsWith("M-SEARCH") ? M_SEARCH : s.startsWith("NOTIFY") ? NOTIFY : 0;

				boolean redundant = address.equals(lastAddress) && packetType == lastPacketType;

				if (packetType == M_SEARCH) {
					if (configuration.getIpFiltering().allowed(address)) {
						String remoteAddr = address.getHostAddress();
						int remotePort = receivePacket.getPort();
						if (!redundant) {
							LOGGER.trace("Receiving a M-SEARCH from [" + remoteAddr + ":" + remotePort + "]: " + s);
						}

						if (StringUtils.indexOf(s, "urn:schemas-upnp-org:service:ContentDirectory:1") > 0) {
							UPNPHelper.sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:service:ContentDirectory:1");
						}

						if (StringUtils.indexOf(s, "upnp:rootdevice") > 0) {
							UPNPHelper.sendDiscover(remoteAddr, remotePort, "upnp:rootdevice");
						}

						if (
							StringUtils.indexOf(s, "urn:schemas-upnp-org:device:MediaServer:1") > 0 ||
							StringUtils.indexOf(s, "ssdp:all") > 0
						) {
							UPNPHelper.sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:device:MediaServer:1");
						}

						if (StringUtils.indexOf(s, PMS.get().usn()) > 0) {
							UPNPHelper.sendDiscover(remoteAddr, remotePort, PMS.get().usn());
						}
					}
				// Don't log redundant notify messages
				} else if (packetType == NOTIFY && !redundant && LOGGER.isTraceEnabled()) {
					LOGGER.trace("Receiving a NOTIFY from [{}:{}]", address.getHostAddress(), receivePacket.getPort());
				}
				lastAddress = address;
				lastPacketType = packetType;
			}
		} catch (SocketException e) {
			LOGGER.trace(
				"Socket closed, terminating multicast listening thread for network interface \"{}\"",
				multicastInterface.getDisplayName()
			);
		} catch (IOException e) {
			LOGGER.error(
				"UPnP network exception, terminating multicast listening thread for network interface \"{}\": {}",
				multicastInterface.getDisplayName(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		} finally {
			closeSocket();
		}
	}

	@Override
	public void interrupt() {
		super.interrupt();
		closeSocket();
	}

	public void closeSocket() {
		synchronized (closeLock) {
			if (multicastSocket != null && !multicastSocket.isClosed()) {
				// Clean up the multicast socket nicely
				LOGGER.trace("Closing listening multicast socket on network interface \"{}\"", multicastInterface.getDisplayName());
				try {
					InetAddress upnpAddress = UPNPHelper.getUPNPAddress();
					multicastSocket.leaveGroup(upnpAddress);
				} catch (IOException e) {
					LOGGER.trace("Exception while leaving UPnP group: ", e.getMessage());
					LOGGER.trace("", e);
				}

				multicastSocket.close();
			}
		}
	}
}
