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

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.fourthline.cling.model.meta.Device;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import static net.pms.dlna.DLNAResource.Temp;
import net.pms.util.BasicPlayer;
import net.pms.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Helper class to handle the UPnP traffic that makes PMS discoverable by
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
		rendererMap = new DeviceMap<RendererConfiguration>(RendererConfiguration.class);
	}

	public static UPNPHelper getInstance() {
		return instance;
	}

	@Override
	public void init() {
		super.init();
		getHttpControlHandler();
	}

	public static PlayerControlHandler getHttpControlHandler() {
		if (httpControlHandler == null && PMS.get().getWebServer() != null &&
				! "false".equals(configuration.getBumpAddress().toLowerCase())) {
			httpControlHandler = new PlayerControlHandler(PMS.get().getWebServer());
			LOGGER.debug("Attached http player control handler to web server");
		}
		return httpControlHandler;
	}

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
	public static void sleep(int delay) {
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

							String s = new String(receivePacket.getData(), 0, receivePacket.getLength());

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
		instance.shutdown();
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

	@Override
	protected Renderer rendererFound(Device d, String uuid) {
		// Create or retrieve an instance
		try {
			InetAddress socket = InetAddress.getByName(getURL(d).getHost());
			RendererConfiguration r = RendererConfiguration.getRendererConfigurationBySocketAddress(socket);

			// FIXME: when UpnpDetailsSearch is missing from the conf a upnp-advertising
			// renderer could register twice if the http server sees it first
			if (r != null && r.matchUPNPDetails(getDeviceDetailsString(d))) {
				// Already seen by the http server, make sure it's mapped
				rendererMap.put(uuid, "0", r);
				// update gui
				PMS.get().updateRenderer(r);
				LOGGER.debug("Found upnp service for " + r.getRendererName() + ": " + getDeviceDetails(d));
			} else {
				// It's brand new
				r = (RendererConfiguration)rendererMap.get(uuid, "0");
				RendererConfiguration ref = RendererConfiguration.getRendererConfigurationByUPNPDetails(getDeviceDetailsString(d));
				if (ref != null) {
					r.init(ref.getFile());
				}
				if (r.associateIP(socket)) {
					PMS.get().setRendererFound(r);
					LOGGER.debug("New renderer found: " + r.getRendererName() + ": " + getDeviceDetails(d));
				}
			}
			return r;
		} catch(Exception e) {
			LOGGER.debug("Error initializing device " + getFriendlyName(d) + ": " + e);
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
		for (Map<String,Renderer> item : (Collection<Map<String,Renderer>>)rendererMap.values()) {
			Renderer r = (Renderer)item.get("0");
			if ((r.controls & type) != 0) {
				return true;
			}
		}
		return false;
	}

	public static List<RendererConfiguration> getRenderers(int type) {
		ArrayList<RendererConfiguration> renderers = new ArrayList<>();
		for (Map<String,Renderer> item : (Collection<Map<String,Renderer>>)rendererMap.values()) {
			Renderer r = (Renderer)item.get("0");
			if ((r.controls & type) != 0) {
				renderers.add((RendererConfiguration)r);
			}
		}
		return renderers;
	}

	@Override
	protected void rendererReady(String uuid) {
	}

	public static void play(String uri, String name, RendererConfiguration r) {
		DLNAResource d = DLNAResource.getValidResource(uri, name, r);
		if (d != null) {
			play(d, r);
		}
	}

	public static void play(DLNAResource d, RendererConfiguration r) {
		DLNAResource d1 = d.getParent() == null ? Temp.add(d) : d;
		if(d1 != null) {
			Device dev = getDevice(r.getUUID());
			String id = r.getInstanceID();
			setAVTransportURI(dev, id, d1.getURL(""), d1.getDidlString(r));
			play(dev, id);
		}
	}

	// A player state-machine to manage upnp playback

	public static class Player implements BasicPlayer {
		private Device dev;
		private String uuid;
		private String instanceID;
		public RendererConfiguration renderer;
		private Map<String,String> data;
		private LinkedHashSet<ActionListener> listeners;
		private BasicPlayer.State state;
		public Playlist playlist;
		String lasturi;


		public Player(RendererConfiguration renderer) {
			uuid = renderer.getUUID();
			instanceID = renderer.getInstanceID();
			this.renderer = renderer;
			dev = getDevice(uuid);
			data = rendererMap.get(uuid, instanceID).connect(this);
			state = new State();
			playlist = new Playlist();
			listeners = new LinkedHashSet();
			lasturi = null;
			LOGGER.debug("Created upnp player for " + renderer.getRendererName());
			refresh();
		}

		@Override
		public void setURI(String uri, String metadata) {
			Playlist.Item item;
			if (uri != null) {
				if (metadata != null && metadata.startsWith("<DIDL")) {
					// If it looks real assume it's valid
				} else if ((item = playlist.get(uri)) != null) {
					// We've played it before
					metadata = item.metadata;
				} else {
					// It's new to us, find or create the resource as required.
					// Note: here metadata (if any) is actually the resource name
					LOGGER.debug("Validating uri " + uri);
					DLNAResource d = DLNAResource.getValidResource(uri, metadata, renderer);
					if (d != null) {
						uri = d.getURL("", true);
						metadata = d.getDidlString(renderer);
					}
				}
				UPNPControl.setAVTransportURI(dev, instanceID, uri, metadata);
			}
		}

		@Override
		public void pressPlay(String uri, String metadata) {
			if (state.playback == -1) {
				// unknown state, we assume it's stopped
				state.playback = STOPPED;
			}
			if (state.playback == PLAYING) {
				pause();
			} else {
				if (state.playback == STOPPED) {
					Playlist.Item item = playlist.resolve(uri);
					if (item != null) {
						uri = item.uri;
						metadata = item.metadata;
					}
					if (uri != null && ! uri.equals(state.uri)) {
						setURI(uri, metadata);
					}
				}
				play();
			}
		}

		@Override
		public void add(int index, String uri, String name, String metadata, boolean select) {
			if (! StringUtils.isBlank(uri)) {
				playlist.add(index, uri, name, metadata, select);
			}
		}

		@Override
		public void remove(String uri) {
			if (! StringUtils.isBlank(uri)) {
				playlist.remove(uri);
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
		public void next() {
			step(1);
		}

		@Override
		public void prev() {
			step(-1);
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
			UPNPControl.setMute(dev, instanceID, ! state.mute);
		}

		public void setVolume(int volume) {
			UPNPControl.setVolume(dev, instanceID, volume);
		}

		@Override
		public void connect(ActionListener listener) {
			listeners.add(listener);
		}

		@Override
		public void disconnect(ActionListener listener) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				close();
			}
		}

		@Override
		public void actionPerformed(final ActionEvent e) {
			refresh();
		}

		@Override
		public void refresh() {
			String s = data.get("TransportState");
			state.playback = "STOPPED".equals(s) ? BasicPlayer.STOPPED :
				"PLAYING".equals(s) ? BasicPlayer.PLAYING :
				"PAUSED_PLAYBACK".equals(s) ? BasicPlayer.PAUSED: -1;
			state.mute = "0".equals(data.get("Mute")) ? false : true;
			s = data.get("Volume");
			state.volume = s == null ? 0 : Integer.valueOf(s);
			state.position = data.get("RelTime");
			state.duration = data.get("CurrentMediaDuration");
			state.uri = data.get("AVTransportURI");
			state.metadata = data.get("AVTransportURIMetaData");
			// update playlist only if uri has changed
			if (! StringUtils.isBlank(state.uri) && ! state.uri.equals(lasturi)) {
				playlist.set(state.uri, null, state.metadata);
			}
			lasturi = state.uri;
			alert();
		}

		public void alert() {
			for (ActionListener l : listeners) {
				l.actionPerformed(new ActionEvent(this, 0, null));
			}
		}

		@Override
		public BasicPlayer.State getState() {
			return state;
		}

		@Override
		public int getControls() {
			return renderer.controls;
		}

		@Override
		public void close() {
			listeners.clear();
			rendererMap.get(uuid, instanceID).disconnect(this);
			renderer.setPlayer(null);
		}

		public DefaultComboBoxModel getPlaylist() {
			return playlist;
		}

		public String jump(double seconds) {
			double t = StringUtil.convertStringToTime(state.position) + seconds;
			return t > 0 ? StringUtil.convertTimeToString(t, "%02d:%02d:%02.0f") : "00:00:00";
		}

		public void step(int n) {
			if (state.playback != STOPPED) {
				stop();
			}
			playlist.step(n);
			state.playback = STOPPED;
			pressPlay(null, null);
		}

		public static class Playlist extends DefaultComboBoxModel {

			public Item get(String uri) {
				int index = getIndexOf(new Item(uri, null, null));
				if (index > -1) {
					return (Item)getElementAt(index);
				}
				return null;
			}

			public Item resolve(String uri) {
				Item item = null;
				try {
					Object selected = getSelectedItem();
					Item selectedItem = selected instanceof Item ? (Item)selected : null;
					String selectedName = selectedItem != null ? selectedItem.name : null;
					// See if we have a matching item for the "uri", which could be:
					item = (Item) (
						// An alias for the currently selected item
						StringUtils.isBlank(uri) || uri.equals(selectedName) ? selectedItem :
						// An item index, e.g. '$i$4'
						uri.startsWith("$i$") ? getElementAt(Integer.valueOf(uri.substring(3))) :
						// Or an actual uri
						get(uri));
				} catch (Exception e) {
				}
				return item;
			}

			public void set(String uri, String name, String metadata) {
				add(0, uri, name, metadata, true);
			}

			public void add(final int index, final String uri, final String name, final String metadata, final boolean select) {
				if (! StringUtils.isBlank(uri)) {
					// TODO: check headless mode (should work according to https://java.net/bugzilla/show_bug.cgi?id=2568)
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							Item item = resolve(uri);
							if (item == null) {
								item = new Item(uri, name,  metadata);
								insertElementAt(item, index > -1 ? index : getSize());
							}
							if (select) {
								setSelectedItem(item);
							}
						}
					});
				}
			}

			public void remove(final String uri) {
				if (! StringUtils.isBlank(uri)) {
					// TODO: check headless mode
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							Item item = resolve(uri);
							if (item != null) {
								removeElement(item);
							}
						}
					});
				}
			}

			public void step(int n) {
				int i = (getIndexOf(getSelectedItem()) + getSize() + n) % getSize();
				setSelectedItem(getElementAt(i));
			}

			public static class Item {

				public String name, uri, metadata;
				static final Matcher dctitle = Pattern.compile("<dc:title>(.+)</dc:title>").matcher("");

				public Item(String uri, String name, String metadata) {
					this.uri = uri;
					this.name = name;
					this.metadata = metadata;
				}

				@Override
				public String toString() {
					if (StringUtils.isBlank(name)) {
						name = (! StringUtils.isEmpty(metadata) && dctitle.reset(unescape(metadata)).find()) ?
							dctitle.group(1) :
							new File(StringUtils.substringBefore(unescape(uri), "?")).getName();
					}
					return name;
				}

				@Override
				public boolean equals(Object other) {
					return other == null ? false :
						other == this ? true :
						other instanceof Item ? ((Item)other).uri.equals(uri) :
						other.toString().equals(uri);
				}

				@Override
				public int hashCode() {
					return uri.hashCode();
				}
			}
		}
	}

	public static String unescape(String s) {
		return StringEscapeUtils.unescapeXml(StringEscapeUtils.unescapeHtml4(URLDecoder.decode(s)));
	}
}

