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

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import static net.pms.dlna.DLNAResource.Temp;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.model.meta.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to handle the UPnP traffic that makes UMS discoverable by
 * other clients.
 * See http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.0.pdf
 * and http://upnp.org/specs/arch/UPnP-arch-DeviceArchitecture-v1.1-AnnexA.pdf
 * for the specifications.
 */
public class UPNPHelper extends UPNPControl {
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

	private static final String[] NT_LIST = {
		"upnp:rootdevice",
		"urn:schemas-upnp-org:device:MediaServer:1",
		"urn:schemas-upnp-org:service:ContentDirectory:1",
		"urn:schemas-upnp-org:service:ConnectionManager:1",
		PMS.get().usn(),
		"urn:microsoft.com:service:X_MS_MediaReceiverRegistrar:1"
	};

	private static final String[] ST_LIST = {
		"urn:schemas-upnp-org:device:MediaRenderer:1",
		"urn:schemas-upnp-org:device:Basic:1"
	};

	// The listener.
	private static Thread listenerThread;

	// The alive thread.
	private static Thread aliveThread;

	private static final PmsConfiguration configuration = PMS.getConfiguration();

	private static final UPNPHelper instance = new UPNPHelper();
	private static PlayerControlHandler httpControlHandler;

	/**
	 * This utility class is not meant to be instantiated.
	 */
	private UPNPHelper() {
		rendererMap = new DeviceMap<>(DeviceConfiguration.class);
	}

	public static UPNPHelper getInstance() {
		return instance;
	}

	@Override
	public void init() {
		if (configuration.isUpnpEnabled()) {
			super.init();
		}
		getHttpControlHandler();
	}

	public static PlayerControlHandler getHttpControlHandler() {
		if (
			httpControlHandler == null &&
			PMS.get().getWebServer() != null &&
			!"false".equals(configuration.getBumpAddress().toLowerCase())
		) {
			httpControlHandler = new PlayerControlHandler(PMS.get().getWebInterface());
			LOGGER.debug("Attached http player control handler to web server");
		}
		return httpControlHandler;
	}

	private static String lastSearch = null;

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
		discovery.append("CACHE-CONTROL: max-age=1800").append(CRLF);
		discovery.append("DATE: ").append(sdf.format(new Date(System.currentTimeMillis()))).append(" GMT").append(CRLF);
		discovery.append("LOCATION: http://").append(serverHost).append(':').append(serverPort).append("/description/fetch").append(CRLF);
		discovery.append("SERVER: ").append(PMS.get().getServerName()).append(CRLF);
		discovery.append("ST: ").append(st).append(CRLF);
		discovery.append("EXT: ").append(CRLF);
		discovery.append("USN: ").append(usn).append(st).append(CRLF);
		discovery.append("Content-Length: 0").append(CRLF).append(CRLF);

		String msg = discovery.toString();

		if (LOGGER.isTraceEnabled()) {
			if (st.equals(lastSearch)) {
				LOGGER.trace("Resending last discovery [" + host + ":" + port + "]");
			} else {
				LOGGER.trace("Sending discovery [" + host + ":" + port + "]: " + StringUtils.replace(msg, CRLF, "<CRLF>"));
			}
		}

		sendReply(host, port, msg);

		for (String ST: ST_LIST) {
			discovery = new StringBuilder();
			discovery.append("M-SEARCH * HTTP/1.1").append(CRLF);
			discovery.append("ST: ").append(ST).append(CRLF);
			discovery.append("HOST: ").append(IPV4_UPNP_HOST).append(':').append(UPNP_PORT).append(CRLF);
			discovery.append("MX: 3").append(CRLF);
			discovery.append("MAN: \"ssdp:discover\"").append(CRLF).append(CRLF);
			msg = discovery.toString();
			sendReply(host, port, msg);
		}

		lastSearch = st;
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
	 * Send alive.
	 */
	public static void sendAlive() {
		LOGGER.debug("Sending ALIVE...");
		MulticastSocket multicastSocket = null;

		try {
			multicastSocket = getNewMulticastSocket();
			InetAddress upnpAddress = getUPNPAddress();
			multicastSocket.joinGroup(upnpAddress);

			for (String NT: NT_LIST) {
				sendMessage(multicastSocket, NT, ALIVE);
			}
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

	static boolean multicastLog = true;

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

		MulticastSocket ssdpSocket = new MulticastSocket(configuration.getUpnpPort());
		ssdpSocket.setReuseAddress(true);
		ssdpSocket.setTimeToLive(32);

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
				}
			}
		}
		if (multicastLog) {
			LOGGER.trace("Sending message from multicast socket on network interface: {}", ssdpSocket.getNetworkInterface());
			LOGGER.trace("Multicast socket is on interface address: {} and local port {}", ssdpSocket.getInterface(), ssdpSocket.getLocalPort());
			LOGGER.trace("Socket Timeout: {}", ssdpSocket.getSoTimeout());
			LOGGER.trace("Socket TTL: {}", ssdpSocket.getTimeToLive());
			multicastLog = false;
		}
		return ssdpSocket;
	}

	/**
	 * Send the UPnP BYEBYE message.
	 */
	public static void sendByeBye() {
		LOGGER.debug("Sending BYEBYE...");

		MulticastSocket multicastSocket = null;

		try {
			multicastSocket = getNewMulticastSocket();
			InetAddress upnpAddress = getUPNPAddress();
			multicastSocket.joinGroup(upnpAddress);

			for (String NT: NT_LIST) {
				sendMessage(multicastSocket, NT, BYEBYE, true);
			}
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
	public static void sleep(int delay) {
		try {
			Thread.sleep(delay);
		} catch (InterruptedException e) { }
	}

	/**
	 * Send the provided message to the socket three times.
	 *
	 * @see #sendMessage(java.net.DatagramSocket, java.lang.String, java.lang.String, boolean)
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
		String msg = buildMsg(nt, message);
		Random rand = new Random();

		// LOGGER.trace( "Sending this SSDP packet: " + CRLF + StringUtils.replace(msg, CRLF, "<CRLF>")));

		InetAddress upnpAddress = getUPNPAddress();
		DatagramPacket ssdpPacket = new DatagramPacket(msg.getBytes(), msg.length(), upnpAddress, UPNP_PORT);

		/**
		 * Requirement [7.2.4.1]: UPnP endpoints (devices and control points) should
		 * wait a random amount of time, between 0 and 100 milliseconds after acquiring
		 * a new IP address, before sending advertisements or initiating searches on a
		 * new IP interface.
		 */
		sleep(rand.nextInt(101));
		socket.send(ssdpPacket);

		// Send the message three times as recommended by the standard
		if (!sendOnce) {
			sleep(100);
			socket.send(ssdpPacket);
			sleep(100);
			socket.send(ssdpPacket);
		}
	}

	private static int ALIVE_delay = 10000;

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
				while (true) {
					sleep(ALIVE_delay);
					sendAlive();

					// If getAliveDelay is 0, there is no custom alive delay
					if (configuration.getAliveDelay() == 0) {
						if (PMS.get().getFoundRenderers().size() > 0) {
							ALIVE_delay = 30000;
						} else {
							ALIVE_delay = 10000;
						}
					} else {
						ALIVE_delay = configuration.getAliveDelay();
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
							LOGGER.warn("Finally, acquiring port {} was successful!", configuration.getUpnpPort());
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
								LOGGER.trace("Setting multicast network interface: {}", ni);
							} else if (PMS.get().getServer().getNetworkInterface() != null) {
								multicastSocket.setNetworkInterface(PMS.get().getServer().getNetworkInterface());
								LOGGER.trace("Setting multicast network interface: {}", PMS.get().getServer().getNetworkInterface());
							}
						} catch (SocketException e) {
							// Not setting the network interface will work just fine on Mac OS X.
						}

						multicastSocket.setTimeToLive(4);
						multicastSocket.setReuseAddress(true);
						InetAddress upnpAddress = getUPNPAddress();
						multicastSocket.joinGroup(upnpAddress);

						final int M_SEARCH = 1;
						final int NOTIFY = 2;
						InetAddress lastAddress = null;
						int lastPacketType = 0;

						while (true) {
							byte[] buf = new byte[1024];
							DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
							multicastSocket.receive(receivePacket);

							String s = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);

							InetAddress address = receivePacket.getAddress();
							int packetType = s.startsWith("M-SEARCH") ? M_SEARCH : s.startsWith("NOTIFY") ? NOTIFY : 0;

							boolean redundant = address.equals(lastAddress) && packetType == lastPacketType;

							if (packetType == M_SEARCH) {
								if (configuration.getIpFiltering().allowed(address)) {
									String remoteAddr = address.getHostAddress();
									int remotePort = receivePacket.getPort();
									if (!redundant && LOGGER.isTraceEnabled()) {
										LOGGER.trace("Received a M-SEARCH from [{}:{}]: {}", remoteAddr, remotePort, s);
									}

									if (StringUtils.indexOf(s, "urn:schemas-upnp-org:service:ContentDirectory:1") > 0) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:service:ContentDirectory:1");
									}

									if (StringUtils.indexOf(s, "upnp:rootdevice") > 0) {
										sendDiscover(remoteAddr, remotePort, "upnp:rootdevice");
									}

									if (
										StringUtils.indexOf(s, "urn:schemas-upnp-org:device:MediaServer:1") > 0 ||
										StringUtils.indexOf(s, "ssdp:all") > 0
									) {
										sendDiscover(remoteAddr, remotePort, "urn:schemas-upnp-org:device:MediaServer:1");
									}

									if (StringUtils.indexOf(s, PMS.get().usn()) > 0) {
										sendDiscover(remoteAddr, remotePort, PMS.get().usn());
									}
								}
							// Don't log redundant notify messages
							} else if (packetType == NOTIFY && !redundant && LOGGER.isTraceEnabled()) {
								LOGGER.trace("Received a NOTIFY from [{}:{}]", address.getHostAddress(), receivePacket.getPort());
							}
							lastAddress = address;
							lastPacketType = packetType;
						}
					} catch (BindException e) {
						if (!bindErrorReported) {
							LOGGER.error("Unable to bind to " + configuration.getUpnpPort()
							+ ", which means that UMS will not automatically appear on your renderer! "
							+ "This usually means that another program occupies the port. Please "
							+ "stop the other program and free up the port. "
							+ "UMS will keep trying to bind to it...[{}]", e.getMessage());
						}

						bindErrorReported = true;
						sleep(5000);
					} catch (IOException e) {
						LOGGER.error("UPnP network exception: {}", e.getMessage());
						LOGGER.trace("", e);
						sleep(1000);
					} finally {
						if (multicastSocket != null) {
							// Clean up the multicast socket nicely
							try {
								InetAddress upnpAddress = getUPNPAddress();
								multicastSocket.leaveGroup(upnpAddress);
							} catch (IOException e) {
								LOGGER.trace("Final UPnP network exception: {}", e.getMessage());
								LOGGER.trace("", e);
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
		instance.shutdown();
		if (listenerThread != null) {
			listenerThread.interrupt();
		}
		if (aliveThread != null) {
			aliveThread.interrupt();
		}
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
		sb.append("HOST: ").append(IPV4_UPNP_HOST).append(':').append(UPNP_PORT).append(CRLF);
		sb.append("NT: ").append(nt).append(CRLF);
		sb.append("NTS: ").append(message).append(CRLF);

		if (message.equals(ALIVE)) {
			sb.append("LOCATION: http://").append(PMS.get().getServer().getHost()).append(':').append(PMS.get().getServer().getPort()).append("/description/fetch").append(CRLF);
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
	 * Gets the UPnP address.
	 *
	 * @return the UPnP address
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	private static InetAddress getUPNPAddress() throws IOException {
		return InetAddress.getByName(IPV4_UPNP_HOST);
	}

	public void addRenderer(DeviceConfiguration d) {
		if (d.uuid != null) {
			rendererMap.put(d.uuid, "0", d);
		}
	}

	public void removeRenderer(RendererConfiguration d) {
		if (d.uuid != null) {
			rendererMap.remove(d.uuid);
		}
	}

	public static boolean activate(String uuid) {
		if (! rendererMap.containsKey(uuid)) {
			LOGGER.debug("Activating upnp service for {}", uuid);
			return getInstance().addRenderer(uuid);
		}
		return true;
	}

	@Override
	protected boolean isBlocked(String uuid) {
		int mode = DeviceConfiguration.getDeviceUpnpMode(uuid, true);
		if (mode != RendererConfiguration.ALLOW) {
			LOGGER.debug("Upnp service is {} for {}", RendererConfiguration.getUpnpModeString(mode), uuid);
			return true;
		}
		return false;
	}

	@Override
	protected Renderer rendererFound(Device d, String uuid) {
		// Create or retrieve an instance
		try {
			InetAddress socket = InetAddress.getByName(getURL(d).getHost());
			DeviceConfiguration r = (DeviceConfiguration) RendererConfiguration.getRendererConfigurationBySocketAddress(socket);
			RendererConfiguration ref = configuration.isRendererForceDefault() ?
				null : RendererConfiguration.getRendererConfigurationByUPNPDetails(getDeviceDetailsString(d));

			if (r != null && ! r.isUpnpAllowed()) {
				LOGGER.debug("Upnp service is {} for \"{}\"", r.getUpnpModeString(), r);
				return null;
			} else if (r == null && ref != null && ! ref.isUpnpAllowed()) {
				LOGGER.debug("Upnp service is {} for {} devices", ref.getUpnpModeString(), ref);
				return null;
			}

			// FIXME: when UpnpDetailsSearch is missing from the conf a upnp-advertising
			// renderer could register twice if the http server sees it first

			boolean distinct = r != null && StringUtils.isNotBlank(r.getUUID()) && ! uuid.equals(r.getUUID());

			if (! distinct && r != null && (r.matchUPNPDetails(getDeviceDetailsString(d)) || ! r.loaded)) {
				// Already seen by the http server
				if (
					ref != null &&
					!ref.getUpnpDetailsString().equals(r.getUpnpDetailsString()) &&
					ref.getLoadingPriority() >= r.getLoadingPriority()
				) {
					// The upnp-matched reference conf is different from the previous
					// http-matched conf and has equal or higher priority, so update.
					LOGGER.debug("Switching to preferred renderer: " + ref);
					r.inherit(ref);
				}

				// Update if we have a custom configuration for this uuid
				r.setUUID(uuid);

				// Make sure it's mapped
				rendererMap.put(uuid, "0", r);
				r.details = getDeviceDetails(d);
				// Update gui
				PMS.get().updateRenderer(r);
				LOGGER.debug("Found upnp service for \"{}\" with dlna details: {}", r, r.details);
			} else {
				// It's brand new
				r = (DeviceConfiguration) rendererMap.get(uuid, "0");
				if (ref != null) {
					r.inherit(ref);
				} else {
					// It's unrecognized: temporarily assign the default renderer but mark it as unloaded
					// so actual recognition can happen later once the http server receives a request.
					// This is to allow initiation of upnp playback before http recognition has occurred.
					r.inherit(r.getDefaultConf());
					r.loaded = false;
					LOGGER.debug("Marking upnp renderer \"{}\" at {} as unrecognized", r, socket);
				}
				if (r.associateIP(socket)) {
					r.details = getDeviceDetails(d);
					PMS.get().setRendererFound(r);
					LOGGER.debug("New renderer found: \"{}\" with dlna details: {}", r, r.details);
				}
			}
			return r;
		} catch (Exception e) {
			LOGGER.debug("Error initializing device " + getFriendlyName(d) + ": " + e);
			e.printStackTrace();
		}
		return null;
	}

	public static InetAddress getAddress(String uuid) {
		try {
			return InetAddress.getByName(getURL(getDevice(uuid)).getHost());
		} catch (Exception e) {
		}
		return null;
	}

	public static boolean hasRenderer(int type) {
		for (Map<String, Renderer> item : (Collection<Map<String, Renderer>>) rendererMap.values()) {
			Renderer r = item.get("0");
			if ((r.controls & type) != 0) {
				return true;
			}
		}
		return false;
	}

	public static List<RendererConfiguration> getRenderers(int type) {
		ArrayList<RendererConfiguration> renderers = new ArrayList<>();
		for (Map<String, Renderer> item : (Collection<Map<String, Renderer>>) rendererMap.values()) {
			Renderer r = item.get("0");
			if (r.active && (r.controls & type) != 0) {
				renderers.add((RendererConfiguration) r);
			}
		}
		return renderers;
	}

	@Override
	protected void rendererReady(String uuid) {
		RendererConfiguration r = RendererConfiguration.getRendererConfigurationByUUID(uuid);
		if(r != null) {
			r.getPlayer();
		}
	}

	public static void play(String uri, String name, DeviceConfiguration r) {
		DLNAResource d = DLNAResource.getValidResource(uri, name, r);
		if (d != null) {
			play(d, r);
		}
	}

	public static void play(DLNAResource d, DeviceConfiguration r) {
		DLNAResource d1 = d.getParent() == null ? Temp.add(d) : d;
		if (d1 != null) {
			Device dev = getDevice(r.getUUID());
			String id = r.getInstanceID();
			setAVTransportURI(dev, id, d1.getURL(""), r.isPushMetadata() ? d1.getDidlString(r) : null);
			play(dev, id);
		}
	}

	// A logical player to manage upnp playback
	public static class Player extends BasicPlayer.Logical {
		protected Device dev;
		protected String uuid;
		protected String instanceID;
		protected Map<String, String> data;
		protected String lasturi;
		private boolean ignoreUpnpDuration;

		public Player(DeviceConfiguration renderer) {
			super(renderer);
			uuid = renderer.getUUID();
			instanceID = renderer.getInstanceID();
			dev = getDevice(uuid);
			data = rendererMap.get(uuid, instanceID).connect(this);
			lasturi = null;
			ignoreUpnpDuration = false;
			LOGGER.debug("Created upnp player for " + renderer.getRendererName());
			refresh();
		}

		@Override
		public void setURI(String uri, String metadata) {
			Playlist.Item item = resolveURI(uri, metadata);
			if (item != null) {
				if (item.name != null) {
					state.name = item.name;
				}
				UPNPControl.setAVTransportURI(dev, instanceID, item.uri, renderer.isPushMetadata() ? item.metadata : null);
			}
		}

		@Override
		public void play() {
			UPNPControl.play(dev, instanceID);
		}

		@Override
		public void pause() {
			UPNPControl.pause(dev, instanceID);
		}

		@Override
		public void stop() {
			UPNPControl.stop(dev, instanceID);
		}

		@Override
		public void forward() {
			UPNPControl.seek(dev, instanceID, REL_TIME, jump(60));
		}

		@Override
		public void rewind() {
			UPNPControl.seek(dev, instanceID, REL_TIME, jump(-60));
		}

		@Override
		public void mute() {
			UPNPControl.setMute(dev, instanceID, !state.mute);
		}

		@Override
		public void setVolume(int volume) {
			UPNPControl.setVolume(dev, instanceID, volume * maxVol / 100);
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			if (renderer.isUpnpConnected()) {
				refresh();
			} else if (state.playback != STOPPED) {
				reset();
			}
		}

		public void refresh() {
			String s = data.get("TransportState");
			state.playback = "STOPPED".equals(s) ? STOPPED :
				"PLAYING".equals(s) ? PLAYING :
				"PAUSED_PLAYBACK".equals(s) ? PAUSED: -1;
			state.mute = !"0".equals(data.get("Mute"));
			s = data.get("Volume");
			state.volume = s == null ? 0 : (Integer.valueOf(s) * 100 / maxVol);
			state.position = data.get("RelTime");
			if (!ignoreUpnpDuration) {
				state.duration = data.get("CurrentMediaDuration");
			}
			state.uri = data.get("AVTransportURI");
			state.metadata = data.get("AVTransportURIMetaData");

			// update playlist only if uri has changed
			if (!StringUtils.isBlank(state.uri) && !state.uri.equals(lasturi)) {
				playlist.set(state.uri, null, state.metadata);
			}
			lasturi = state.uri;
			alert();
		}

		@Override
		public void start() {
			DLNAResource d = renderer.getPlayingRes();
			state.name = d.getDisplayName();
			if (d.getMedia() != null) {
				String duration = d.getMedia().getDurationString();
				ignoreUpnpDuration = !StringUtil.isZeroTime(duration);
				if (ignoreUpnpDuration) {
					state.duration = StringUtil.shortTime(d.getMedia().getDurationString(), 4);
				}
			}
		}

		@Override
		public void close() {
			rendererMap.get(uuid, instanceID).disconnect(this);
			super.close();
		}

		public String jump(double seconds) {
			double t = StringUtil.convertStringToTime(state.position) + seconds;
			return t > 0 ? StringUtil.convertTimeToString(t, "%02d:%02d:%02.0f") : "00:00:00";
		}
	}

	public static String unescape(String s) throws UnsupportedEncodingException {
		return StringEscapeUtils.unescapeXml(StringEscapeUtils.unescapeHtml4(URLDecoder.decode(s, "UTF-8")));
	}
}
