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
package net.pms.network.mediaserver.socketssdpserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.NetworkConfiguration;
import net.pms.network.mediaserver.UPNPHelper;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server to handle the UPnP traffic that makes UMS discoverable by other
 * clients. See http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.0.pdf
 * and http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1-AnnexA.pdf
 * for the specifications.
 */
public class SocketSSDPServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SocketSSDPServer.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final int ALIVE_DELAY = CONFIGURATION.getAliveDelay() != 0 ? CONFIGURATION.getAliveDelay() : 30000;

	/**
	 * IPv4 Multicast channel reserved for SSDP by Internet Assigned Numbers
	 * Authority (IANA). MUST be 239.255.255.250.
	 */
	private static final String IPV4_UPNP_HOST = "239.255.255.250";

	/**
	 * Multicast channel reserved for SSDP by Internet Assigned Numbers
	 * Authority (IANA). MUST be 1900.
	 */
	private static final int UPNP_PORT = 1900;

	// Carriage return and line feed.
	private static final String CRLF = "\r\n";
	// The Constant ALIVE.
	private static final String ALIVE = "ssdp:alive";
	// The Constant BYEBYE.
	private static final String BYEBYE = "ssdp:byebye";
	private static final String UUID = "uuid:";

	private static final String[] NT_LIST = {
		"upnp:rootdevice",
		"urn:schemas-upnp-org:device:MediaServer:1",
		"urn:schemas-upnp-org:service:ContentDirectory:1",
		"urn:schemas-upnp-org:service:ConnectionManager:1",
		PMS.get().usn(),
		"urn:microsoft.com:service:X_MS_MediaReceiverRegistrar:1"
	};

	// The listener.
	private static Thread listenerThread;

	// The alive thread.
	private static Thread aliveThread;

	private static String lastSearch = null;

	private static MulticastSocket multicastSocket;
	private static SocketAddress socketAddress;
	private static NetworkInterface networkInterface;

	public static boolean start() {
		try {
			createMulticastSocket();
			sendAlive();
			sendByeBye();
			LOGGER.trace("Waiting 250 milliseconds...");
			UMSUtils.sleep(250);
			sendAlive();
			LOGGER.trace("Waiting 250 milliseconds...");
			UMSUtils.sleep(250);
			listen();
		} catch (IOException ex) {
			stop();
			return false;
		}
		return true;
	}

	public static void stop() {
		shutDownListener();
		sendByeBye();
		destroyMulticastSocket();
	}

	/**
	 * Create the multicast socket and its socket address used for
	 * sending/receiving the multicast messages.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void createMulticastSocket() throws IOException {
		networkInterface = getNetworkInterface();
		multicastSocket = getNewMulticastSocket();
		socketAddress = new InetSocketAddress(getIPv4MulticastAddress(), UPNP_PORT);
		multicastSocket.setTimeToLive(4);
		multicastSocket.setReuseAddress(true);
		multicastSocket.joinGroup(socketAddress, networkInterface);
	}

	/**
	 * Gets the new multicast socket.
	 *
	 * @return the new multicast socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static NetworkInterface getNetworkInterface() throws IOException {
		NetworkInterface result = null;
		try {
			result = NetworkConfiguration.getInstance().getNetworkInterfaceByServerName();
		} catch (SocketException | UnknownHostException e) {
		}

		if (result == null) {
			try {
				result = MediaServer.getNetworkInterface();
			} catch (NullPointerException e) {
				LOGGER.debug("Couldn't get server network interface. Trying again in 5 seconds.");

				try {
					Thread.sleep(5000);
				} catch (InterruptedException e2) {
				}

				try {
					result = MediaServer.getNetworkInterface();
				} catch (NullPointerException e3) {
					LOGGER.debug("Couldn't get server network interface.");
				}
			}
		}
		if (result == null) {
			throw new IOException("No usable network interface found for UPnP multicast");
		}
		return result;
	}

	/**
	 * Gets the new multicast socket.
	 *
	 * @return the new multicast socket
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static MulticastSocket getNewMulticastSocket() throws IOException {
		// Use configurable source port as per
		// http://code.google.com/p/ps3mediaserver/issues/detail?id=1166
		// XXX this should not be configurable because it breaks the standard
		MulticastSocket ssdpSocket = null;
		try {
			ssdpSocket = new MulticastSocket(CONFIGURATION.getUpnpPort());
		} catch (IOException e) {
			LOGGER.error("Unable to bind multicast socket to port: " + CONFIGURATION.getUpnpPort() +
				", which means that UMS will not automatically appear on your renderer! " +
				"This usually means that another program occupies the port. Please " +
				"stop the UMS and the other program to free up the port and start the UMS again.");
			throw new IOException(e);
		}

		ssdpSocket.setReuseAddress(true);
		// In the UPnP standard is written:
		// To limit network congestion, the time-to-live (TTL) of each IP packet for each multicast message SHOULD default to 2 and
		// SHOULD be configurable. When the TTL is greater than 1, it is possible for multicast messages to traverse multiple routers;
		// therefore control points and devices using non-AutoIP addresses MUST send an IGMP Join message so that routers will forward
		// multicast messages to them (this is not necessary when using an Auto-IP address, since packets with Auto-IP addresses will not be
		// forwarded by routers).
		ssdpSocket.setTimeToLive(2);

		try {
			LOGGER.trace("Setting SSDP network interface: {}", networkInterface);
			ssdpSocket.setNetworkInterface(networkInterface);
		} catch (SocketException ex) {
			LOGGER.warn("Setting SSDP network interface failed: {}", ex);
			NetworkInterface confIntf = NetworkConfiguration.getInstance().getNetworkInterfaceByServerName();
			if (confIntf != null) {
				LOGGER.trace("Setting SSDP network interface from configuration: {}", confIntf);
				try {
					ssdpSocket.setNetworkInterface(confIntf);
				} catch (SocketException ex2) {
					LOGGER.warn("Setting SSDP network interface from configuration failed: {}", ex2);
					throw new IOException(ex2);
				}
			}
		}

		LOGGER.trace("Created multicast socket on network interface: {}", ssdpSocket.getNetworkInterface());
		LOGGER.trace("Socket local port: {}", ssdpSocket.getLocalPort());
		LOGGER.trace("Socket Timeout: {}", ssdpSocket.getSoTimeout());
		LOGGER.trace("Socket TTL: {}", ssdpSocket.getTimeToLive());
		return ssdpSocket;
	}

	/**
	 * Gets the IPv4 multicast channel address.
	 *
	 * @return the UPnP address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static InetAddress getIPv4MulticastAddress() throws IOException {
		return InetAddress.getByName(IPV4_UPNP_HOST);
	}

	private static void destroyMulticastSocket() {
		if (multicastSocket != null) {
			if (socketAddress != null) {
				try {
					multicastSocket.leaveGroup(socketAddress, networkInterface);
				} catch (IOException ex) {
				}
				socketAddress = null;
				networkInterface = null;
			}
			multicastSocket.close();
		}
	}

	/**
	 * Send alive.
	 */
	public static void sendAlive() {
		LOGGER.debug("Sending ALIVE...");
		for (String nt : NT_LIST) {
			try {
				sendMessage(multicastSocket, nt, ALIVE);
			} catch (IOException e) {
				LOGGER.trace("Error when sending the ALIVE message: {}", e);
			}
		}
	}

	/**
	 * Send the UPnP BYEBYE message.
	 */
	public static void sendByeBye() {
		if (multicastSocket == null || multicastSocket.isClosed()) {
			LOGGER.trace("Multicast socket closed when sending the BYEBYE message");
		}
		LOGGER.debug("Sending BYEBYE...");
		for (String nt : NT_LIST) {
			try {
				sendMessage(multicastSocket, nt, BYEBYE, true);
			} catch (IOException e) {
				LOGGER.trace("Error when sending the BYEBYE message: {}", e);
			}
		}
	}

	/**
	 * Send the provided message to the socket two times.
	 *
	 * @see #sendMessage(java.net.DatagramSocket, java.lang.String,
	 *      java.lang.String, boolean)
	 * @param socket the socket
	 * @param nt the nt
	 * @param message the message
	 * @throws IOException
	 */
	private static void sendMessage(DatagramSocket socket, String nt, String message) throws IOException {
		sendMessage(socket, nt, message, false);
	}

	/**
	 * Send the provided message to the socket.
	 *
	 * @param socket the socket
	 * @param nt the nt
	 * @param message the message
	 * @param sendOnce send the message only once
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void sendMessage(DatagramSocket socket, String nt, String message, boolean sendOnce) throws IOException {
		String msg = buildMessage(nt, message);
		Random rand = new Random();

		// LOGGER.trace( "Sending this SSDP packet: " + CRLF +
		// StringUtils.replace(msg, CRLF, "<CRLF>")));

		DatagramPacket ssdpPacket = new DatagramPacket(msg.getBytes(), msg.length(), socketAddress);

		/**
		 * Requirement [7.2.4.1]: UPnP endpoints (devices and control points)
		 * should wait a random amount of time, between 0 and 100 milliseconds
		 * after acquiring a new IP address, before sending advertisements or
		 * initiating searches on a new IP interface.
		 */
		UMSUtils.sleep(rand.nextInt(101));
		socket.send(ssdpPacket);

		// Repeat the message as recommended by the standard
		if (!sendOnce) {
			UMSUtils.sleep(100);
			socket.send(ssdpPacket);
		}
	}

	/**
	 * Builds a UPnP message string based on a message.
	 *
	 * @param nt the nt
	 * @param message the message
	 * @return the string
	 */
	private static String buildMessage(String nt, String message) {
		StringBuilder sb = new StringBuilder();

		sb.append("NOTIFY * HTTP/1.1").append(CRLF);
		sb.append("HOST: ").append(IPV4_UPNP_HOST).append(':').append(UPNP_PORT).append(CRLF);
		sb.append("NT: ").append(nt).append(CRLF);
		sb.append("NTS: ").append(message).append(CRLF);

		if (message.equals(ALIVE)) {
			sb
				.append("LOCATION: ")
				.append(MediaServer.getURL())
				.append("/description/fetch")
				.append(CRLF);
		}

		sb.append("USN: ").append(PMS.get().usn());

		if (!nt.equals(PMS.get().usn())) {
			sb.append("::").append(nt);
		}

		sb.append(CRLF);

		if (message.equals(ALIVE)) {
			sb.append("CACHE-CONTROL: max-age=1800").append(CRLF);
			sb.append("SERVER: ").append(PMS.get().getServerName()).append(CRLF);
		}

		// Sony devices like PS3 and PS4 need this extra linebreak
		sb.append(CRLF);
		return sb.toString();
	}

	/**
	 * Send UPnP discovery search message to discover devices of interest on the
	 * network.
	 *
	 * @param host The multicast channel
	 * @param port The multicast port
	 * @param st The search target string
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void sendDiscover(String host, int port, String searchTarget) throws IOException {
		String usn = PMS.get().usn();
		String serverURL = MediaServer.getURL();
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);

		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));

		if (searchTarget.equals(usn)) {
			usn = "";
		} else {
			usn += "::";
		}

		StringBuilder discovery = new StringBuilder();

		discovery.append("HTTP/1.1 200 OK").append(CRLF);
		discovery.append("CACHE-CONTROL: max-age=1800").append(CRLF);
		discovery.append("DATE: ").append(sdf.format(new Date(System.currentTimeMillis()))).append(" GMT").append(CRLF);
		discovery.append("LOCATION: ").append(serverURL).append("/description/fetch").append(CRLF);
		discovery.append("SERVER: ").append(PMS.get().getServerName()).append(CRLF);
		discovery.append("ST: ").append(searchTarget).append(CRLF);
		discovery.append("EXT: ").append(CRLF);
		discovery.append("USN: ").append(usn).append(searchTarget).append(CRLF);
		discovery.append("Content-Length: 0").append(CRLF).append(CRLF);

		String msg = discovery.toString();

		if (LOGGER.isTraceEnabled()) {
			if (searchTarget.equals(lastSearch)) {
				LOGGER.trace("Resending last discovery [" + host + ":" + port + "]");
			} else {
				LOGGER.trace("Sending discovery [" + host + ":" + port + "]: " + StringUtils.replace(msg, CRLF, "<CRLF>"));
			}
		}

		sendReply(host, port, msg);
		lastSearch = searchTarget;
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
			datagramSocket.send(dgmPacket);
		} catch (Exception e) {
			LOGGER.info(e.getMessage());
			LOGGER.debug("Error sending reply", e);
		}
	}


	/**
	 * Check if the request was send from NOT ignored device.
	 *
	 * @param request The message to check.
	 * @return True when requesting device is NOT on the list of ignored
	 *         devices, false otherwise.
	 */
	private static boolean isNotIgnoredDevice(String request) {
		String uuid;
		int uuidPosition = request.indexOf(UUID);
		if (uuidPosition != -1) {
			String temp = request.substring(uuidPosition);
			// get only the line of message containing UUID
			temp = temp.substring(0, temp.indexOf(CRLF));
			if (temp.indexOf(':') == temp.lastIndexOf(':')) {
				uuid = temp; // there are no additional informations in the line
			} else {
				uuid = temp.substring(0, temp.indexOf(':', UUID.length()));
			}
		} else {
			return true;
		}
		return UPNPHelper.isNotIgnoredDevice(uuid);
	}

	/**
	 * Starts up two threads: one to broadcast UPnP ALIVE messages and another
	 * to listen for responses.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static void listen() throws IOException {
		Runnable rAlive = () -> {
			while (true) {
				UMSUtils.sleep(ALIVE_DELAY);
				sendAlive();
			}
		};

		aliveThread = new Thread(rAlive, "UPNP-AliveMessageSender");
		aliveThread.start();

		Runnable r = () -> {
			while (true) {
				try {
					final int mSearch = 1;
					final int notify = 2;
					InetAddress lastAddress = null;
					int lastPacketType = 0;
					long lastValidPacketReceivedTime = System.currentTimeMillis();

					while (true) {
						byte[] buf = new byte[1024];
						DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
						multicastSocket.receive(receivePacket);

						String s = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

						InetAddress address = receivePacket.getAddress();
						int packetType = s.startsWith("M-SEARCH") ? mSearch : s.startsWith("NOTIFY") ? notify : 0;

						long currentTime = System.currentTimeMillis();
						/*
						 * Do not respond to a message if it: - Is from the
						 * same address as the last message, and - Is the
						 * same packet type as the last message, and - Has
						 * happened within 10 seconds of the last valid
						 * message
						 */
						boolean redundant = address.equals(lastAddress) && packetType == lastPacketType &&
							currentTime < (lastValidPacketReceivedTime + 10 * 1000);
						// Is the request from our own server, i.e.
						// self-originating?
						boolean isSelf = address.getHostAddress().equals(MediaServer.getHost()) && s.contains("UMS/");

						if (CONFIGURATION.getIpFiltering().allowed(address) && !isSelf && isNotIgnoredDevice(s)) {
							String remoteAddr = address.getHostAddress();
							int remotePort = receivePacket.getPort();
							if (!redundant) {
								if (packetType == mSearch || packetType == notify) {
									if (LOGGER.isTraceEnabled()) {
										String requestType = "";
										if (packetType == mSearch) {
											requestType = "M-SEARCH";
										} else if (packetType == notify) {
											requestType = "NOTIFY";
										}
										LOGGER.trace("Received a {} from [{}:{}]: {}", requestType, remoteAddr, remotePort, s);
									}

									if (StringUtils.indexOf(s, "urn:schemas-upnp-org:service:ContentDirectory:1") > 0) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:service:ContentDirectory:1");
									}

									if (StringUtils.indexOf(s, "upnp:rootdevice") > 0) {
										sendDiscover(remoteAddr, remotePort, "upnp:rootdevice");
									}

									if (StringUtils.indexOf(s, "urn:schemas-upnp-org:device:MediaServer:1") > 0 ||
											StringUtils.indexOf(s, "ssdp:all") > 0) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:device:MediaServer:1");
									}

									if (StringUtils.indexOf(s, PMS.get().usn()) > 0) {
										sendDiscover(remoteAddr, remotePort, PMS.get().usn());
									}
								} else {
									LOGGER.trace("Received an unrecognized request from [{}:{}]: {}", remoteAddr, remotePort, s);
								}
								lastValidPacketReceivedTime = System.currentTimeMillis();
							}
						}
						lastAddress = address;
						lastPacketType = packetType;
					}
				} catch (IOException e) {
					LOGGER.error("UPnP network exception: {}", e.getMessage());
					LOGGER.trace("", e);
					UMSUtils.sleep(1000);
				} finally {
					if (multicastSocket != null) {
					// Clean up the multicast socket nicely
						try {
							multicastSocket.leaveGroup(socketAddress, networkInterface);
						} catch (IOException e) {
							LOGGER.trace("Final UPnP network exception: {}", e.getMessage());
							LOGGER.trace("", e);
						}

						multicastSocket.disconnect();
						multicastSocket.close();
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
	private static void shutDownListener() {
		if (listenerThread != null) {
			listenerThread.interrupt();
		}
		if (aliveThread != null) {
			aliveThread.interrupt();
		}
	}

}
