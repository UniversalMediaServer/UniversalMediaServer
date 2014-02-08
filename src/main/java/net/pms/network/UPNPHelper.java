/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */
package net.pms.network;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to handle the UPnP traffic that makes PMS discoverable by
 * other clients.
 * See http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.0.pdf
 * and http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1-AnnexA.pdf
 * for the specifications.
 */
public class UPNPHelper {
	// Logger instance to write messages to the logs.
	private static final Logger LOGGER = LoggerFactory.getLogger(UPNPHelper.class);

	// Carriage return and line feed.
	private static final String CRLF = "\r\n";

	// The Constant ALIVE.
	private static final String ALIVE = "ssdp:alive";

	/**
	 * IPv4 Multicast channel reserved for SSDP by Internet Assigned Numbers Authority (IANA).
	 * MUST be 239.255.255.250.
	 */
	private static final String IPV4_UPNP_HOST = "239.255.255.250";

	/**
	 * Multicast channel reserved for SSDP by Internet Assigned Numbers Authority (IANA).
	 * MUST be 1900.
	 */
	private static final int UPNP_PORT = 1900;

	// The Constant BYEBYE.
	private static final String BYEBYE = "ssdp:byebye";

	// The listener.
	private static Thread listenerThread;

	// The alive thread.
	private static Thread aliveThread;

	private static final PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * This utility class is not meant to be instantiated.
	 */
	private UPNPHelper() { }

	/**
	 * Send UPnP discovery search message to discover devices of interest on
	 * the network.
	 *
	 * @param host The multicast channel
	 * @param port The multicast port
	 * @param st The search target string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void sendDiscover(String host, int port, String st) throws IOException {
		String usn = PMS.get().usn();
		String serverHost = PMS.get().getServer().getHost();
		int serverPort = PMS.get().getServer().getPort();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);

		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		if (st.equals(usn)) {
			usn = "";
		} else {
			usn += "::";
		}

		StringBuilder discovery = new StringBuilder();

		discovery.append("HTTP/1.1 200 OK").append(CRLF);
		discovery.append("CACHE-CONTROL: max-age=1200").append(CRLF);
		discovery.append("DATE: ").append(sdf.format(new Date(System.currentTimeMillis()))).append(" GMT").append(CRLF);
		discovery.append("LOCATION: http://").append(serverHost).append(":").append(serverPort).append("/description/fetch").append(CRLF);
		discovery.append("SERVER: ").append(PMS.get().getServerName()).append(CRLF);
		discovery.append("ST: ").append(st).append(CRLF);
		discovery.append("EXT: ").append(CRLF);
		discovery.append("USN: ").append(usn).append(st).append(CRLF);
		discovery.append("Content-Length: 0").append(CRLF).append(CRLF);

		sendReply(host, port, discovery.toString());
	}

	/**
	 * Send reply.
	 *
	 * @param host the host
	 * @param port the port
	 * @param msg the msg
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void sendReply(String host, int port, String msg) {
		try (DatagramSocket datagramSocket = new DatagramSocket()) {
			InetAddress inetAddr = InetAddress.getByName(host);
			DatagramPacket dgmPacket = new DatagramPacket(msg.getBytes(), msg.length(), inetAddr, port);

			LOGGER.trace("Sending this reply [" + host + ":" + port + "]: " + StringUtils.replace(msg, CRLF, "<CRLF>"));

			datagramSocket.send(dgmPacket);
		} catch (Exception e) {
			LOGGER.info(e.getMessage());
			LOGGER.debug("Error sending reply", e);
		}
	}

	/**
	 * Send alive.
	 */
	public static void sendAlive() {
		LOGGER.debug("Sending ALIVE...");
		MulticastSocket multicastSocket = null;

		try {
			multicastSocket = getNewMulticastSocket();
			InetAddress upnpAddress = getUPNPAddress();
			multicastSocket.joinGroup(upnpAddress);

			sendMessage(multicastSocket, "upnp:rootdevice", ALIVE);
			sendMessage(multicastSocket, PMS.get().usn(), ALIVE);
			sendMessage(multicastSocket, "urn:schemas-upnp-org:device:MediaServer:1", ALIVE);
			sendMessage(multicastSocket, "urn:schemas-upnp-org:service:ContentDirectory:1", ALIVE);
			sendMessage(multicastSocket, "urn:schemas-upnp-org:service:ConnectionManager:1", ALIVE);
		} catch (IOException e) {
			LOGGER.debug("Error sending ALIVE message", e);
		} finally {
			if (multicastSocket != null) {
				// Clean up the multicast socket nicely
				try {
					InetAddress upnpAddress = getUPNPAddress();
					multicastSocket.leaveGroup(upnpAddress);
				} catch (IOException e) {
				}

				multicastSocket.disconnect();
				multicastSocket.close();
			}
		}
	}

	/**
	 * Gets the new multicast socket.
	 *
	 * @return the new multicast socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static MulticastSocket getNewMulticastSocket() throws IOException {
		NetworkInterface networkInterface = NetworkConfiguration.getInstance().getNetworkInterfaceByServerName();

		if (networkInterface == null) {
			try {
				networkInterface = PMS.get().getServer().getNetworkInterface();
			} catch (NullPointerException e) {
				LOGGER.debug("Couldn't get server network interface. Trying again in 5 seconds.");

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e2) { }

				try {
					networkInterface = PMS.get().getServer().getNetworkInterface();
				} catch (NullPointerException e3) {
					LOGGER.debug("Couldn't get server network interface.");
				}
			}
		}

		if (networkInterface == null) {
			throw new IOException("No usable network interface found for UPnP multicast");
		}

		List<InetAddress> usableAddresses = new ArrayList<>();
		List<InetAddress> networkInterfaceAddresses = Collections.list(networkInterface.getInetAddresses());

		for (InetAddress inetAddress : networkInterfaceAddresses) {
			if (inetAddress != null && inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
				usableAddresses.add(inetAddress);
			}
		}

		if (usableAddresses.isEmpty()) {
			throw new IOException("No usable addresses found for UPnP multicast");
		}

		InetSocketAddress localAddress = new InetSocketAddress(usableAddresses.get(0), 0);
		MulticastSocket ssdpSocket = new MulticastSocket(localAddress);
		ssdpSocket.setReuseAddress(true);

		LOGGER.trace("Sending message from multicast socket on network interface: " + ssdpSocket.getNetworkInterface());
		LOGGER.trace("Multicast socket is on interface: " + ssdpSocket.getInterface());
		ssdpSocket.setTimeToLive(32);
		LOGGER.trace("Socket Timeout: " + ssdpSocket.getSoTimeout());
		LOGGER.trace("Socket TTL: " + ssdpSocket.getTimeToLive());
		return ssdpSocket;
	}

	/**
	 * Send the UPnP BYEBYE message.
	 */
	public static void sendByeBye() {
		LOGGER.info("Sending BYEBYE...");

		MulticastSocket multicastSocket = null;

		try {
			multicastSocket = getNewMulticastSocket();
			InetAddress upnpAddress = getUPNPAddress();
			multicastSocket.joinGroup(upnpAddress);
	
			sendMessage(multicastSocket, "upnp:rootdevice", BYEBYE);
			sendMessage(multicastSocket, "urn:schemas-upnp-org:device:MediaServer:1", BYEBYE);
			sendMessage(multicastSocket, "urn:schemas-upnp-org:service:ContentDirectory:1", BYEBYE);
			sendMessage(multicastSocket, "urn:schemas-upnp-org:service:ConnectionManager:1", BYEBYE);
		} catch (IOException e) {
			LOGGER.debug("Error sending BYEBYE message", e);
		} finally {
			if (multicastSocket != null) {
				// Clean up the multicast socket nicely
				try {
					InetAddress upnpAddress = getUPNPAddress();
					multicastSocket.leaveGroup(upnpAddress);
				} catch (IOException e) {
				}

				multicastSocket.disconnect();
				multicastSocket.close();
			}
		}
	}

	/**
	 * Utility method to call {@link Thread#sleep(long)} without having to
	 * catch the InterruptedException.
	 *
	 * @param delay the delay
	 */
	private static void sleep(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) { }
	}

	/**
	 * Send the provided message to the socket.
	 *
	 * @param socket the socket
	 * @param nt the nt
	 * @param message the message
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void sendMessage(DatagramSocket socket, String nt, String message) throws IOException {
		String msg = buildMsg(nt, message);
		//Random rand = new Random();

		// LOGGER.trace( "Sending this SSDP packet: " + CRLF + StringUtils.replace(msg, CRLF, "<CRLF>")));

		InetAddress upnpAddress = getUPNPAddress();
		DatagramPacket ssdpPacket = new DatagramPacket(msg.getBytes(), msg.length(), upnpAddress, UPNP_PORT);
		socket.send(ssdpPacket);

		// XXX Why is it necessary to sleep for this random time? What would happen when random equals 0?
		//sleep(rand.nextInt(1800 / 2));

		// XXX Why send the same packet twice?
		//socket.send(ssdpPacket);

		// XXX Why is it necessary to sleep for this random time (again)?
		//sleep(rand.nextInt(1800 / 2));
	}

	/**
	 * Starts up two threads: one to broadcast UPnP ALIVE messages and another
	 * to listen for responses. 
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void listen() throws IOException {
		Runnable rAlive = new Runnable() {
			@Override
			public void run() {
				int delay = 10000;

				while (true) {
					sleep(delay);
					sendAlive();

					// The first delay for sending an ALIVE message is 10 seconds,
					// the second delay is for 20 seconds. From then on, all other
					// delays are for 180 seconds.
					switch (delay) {
					case 10000:
						delay = 20000;
						break;
					case 20000:
						delay = 180000;
						break;
					default:
						break;
					}
				}
			}
		};

		aliveThread = new Thread(rAlive, "UPNP-AliveMessageSender");
		aliveThread.start();

		Runnable r = new Runnable() {
			@Override
			public void run() {
				boolean bindErrorReported = false;

				while (true) {
					MulticastSocket multicastSocket = null;

					try {
						// Use configurable source port as per http://code.google.com/p/ps3mediaserver/issues/detail?id=1166
						multicastSocket = new MulticastSocket(configuration.getUpnpPort());

						if (bindErrorReported) {
							LOGGER.warn("Finally, acquiring port " + configuration.getUpnpPort() + " was successful!");
						}

						NetworkInterface ni = NetworkConfiguration.getInstance().getNetworkInterfaceByServerName();

						try {
							/**
							 * Setting the network interface will throw a SocketException on Mac OS X
							 * with Java 1.6.0_45 or higher, but if we don't do it some Windows
							 * configurations will not listen at all.
							 */
							if (ni != null) {
									multicastSocket.setNetworkInterface(ni);
							} else if (PMS.get().getServer().getNetworkInterface() != null) {
									multicastSocket.setNetworkInterface(PMS.get().getServer().getNetworkInterface());
									LOGGER.trace("Setting multicast network interface: " + PMS.get().getServer().getNetworkInterface());
							}
						} catch (SocketException e) {
							// Not setting the network interface will work just fine on Mac OS X.
						}

						multicastSocket.setTimeToLive(4);
						multicastSocket.setReuseAddress(true);
						InetAddress upnpAddress = getUPNPAddress();
						multicastSocket.joinGroup(upnpAddress);

						while (true) {
							byte[] buf = new byte[1024];
							DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
							multicastSocket.receive(receivePacket);

							String s = new String(receivePacket.getData());

							InetAddress address = receivePacket.getAddress();

							if (s.startsWith("M-SEARCH")) {
								String remoteAddr = address.getHostAddress();
								int remotePort = receivePacket.getPort();

								if (configuration.getIpFiltering().allowed(address)) {
									LOGGER.trace("Receiving a M-SEARCH from [" + remoteAddr + ":" + remotePort + "]");

									if (StringUtils.indexOf(s, "urn:schemas-upnp-org:service:ContentDirectory:1") > 0) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:service:ContentDirectory:1");
									}

									if (StringUtils.indexOf(s, "upnp:rootdevice") > 0) {
										sendDiscover(remoteAddr, remotePort, "upnp:rootdevice");
									}

									if (StringUtils.indexOf(s, "urn:schemas-upnp-org:device:MediaServer:1") > 0) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:device:MediaServer:1");
									}

									if (StringUtils.indexOf(s, "ssdp:all") > 0) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:device:MediaServer:1");
									}

									if (StringUtils.indexOf(s, PMS.get().usn()) > 0) {
										sendDiscover(remoteAddr, remotePort, PMS.get().usn());
									}
								}
							} else if (s.startsWith("NOTIFY")) {
								String remoteAddr = address.getHostAddress();
								int remotePort = receivePacket.getPort();

								LOGGER.trace("Receiving a NOTIFY from [" + remoteAddr + ":" + remotePort + "]");
							}
						}
					} catch (BindException e) {
						if (!bindErrorReported) {
							LOGGER.error("Unable to bind to " + configuration.getUpnpPort()
							+ ", which means that PMS will not automatically appear on your renderer! "
							+ "This usually means that another program occupies the port. Please "
							+ "stop the other program and free up the port. "
							+ "PMS will keep trying to bind to it...[" + e.getMessage() + "]");
						}

						bindErrorReported = true;
						sleep(5000);
					} catch (IOException e) {
						LOGGER.error("UPNP network exception", e);
						sleep(1000);
					} finally {
						if (multicastSocket != null) {
							// Clean up the multicast socket nicely
							try {
								InetAddress upnpAddress = getUPNPAddress();
								multicastSocket.leaveGroup(upnpAddress);
							} catch (IOException e) {
							}

							multicastSocket.disconnect();
							multicastSocket.close();
						}
					}
				}
			}
		};

		listenerThread = new Thread(r, "UPNPHelper");
		listenerThread.start();
	}

	/**
	 * Shut down the threads that send ALIVE messages and listen to responses.
	 */
	public static void shutDownListener() {
		listenerThread.interrupt();
		aliveThread.interrupt();
	}

	/**
	 * Builds a UPnP message string based on a message.
	 *
	 * @param nt the nt
	 * @param message the message
	 * @return the string
	 */
	private static String buildMsg(String nt, String message) {
		StringBuilder sb = new StringBuilder();

		sb.append("NOTIFY * HTTP/1.1").append(CRLF);
		sb.append("HOST: ").append(IPV4_UPNP_HOST).append(":").append(UPNP_PORT).append(CRLF);
		sb.append("NT: ").append(nt).append(CRLF);
		sb.append("NTS: ").append(message).append(CRLF);

		if (message.equals(ALIVE)) {
			sb.append("LOCATION: http://").append(PMS.get().getServer().getHost()).append(":").append(PMS.get().getServer().getPort()).append("/description/fetch").append(CRLF);
		}

		sb.append("USN: ").append(PMS.get().usn());

		if (!nt.equals(PMS.get().usn())) {
			sb.append("::").append(nt);
		}

		sb.append(CRLF);

		if (message.equals(ALIVE)) {
			sb.append("CACHE-CONTROL: max-age=1800").append(CRLF);
		}

		if (message.equals(ALIVE)) {
			sb.append("SERVER: ").append(PMS.get().getServerName()).append(CRLF);
		}

		sb.append(CRLF);
		return sb.toString();
	}

	/**
	 * Gets the uPNP address.
	 *
	 * @return the uPNP address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static InetAddress getUPNPAddress() throws IOException {
		return InetAddress.getByName(IPV4_UPNP_HOST);
	}
}
