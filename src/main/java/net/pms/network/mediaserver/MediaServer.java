/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.gui.GuiManager;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.configuration.NetworkInterfaceAssociation;
import net.pms.network.mediaserver.javahttpserver.JavaHttpServer;
import net.pms.network.mediaserver.jupnp.UmsUpnpService;
import net.pms.network.mediaserver.mdns.MDNS;
import net.pms.network.mediaserver.nettyserver.NettyServer;
import net.pms.network.mediaserver.socketchannelserver.SocketChannelServer;
import net.pms.network.mediaserver.socketssdpserver.SocketSSDPServer;
import org.jupnp.model.message.header.DeviceTypeHeader;
import org.jupnp.model.types.DeviceType;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final Map<Integer, String> VERSIONS = Map.of(
		1, "Sockets",
		2, "Netty",
		3, "Java",
		4, "JUPnP (Netty)",
		5, "JUPnP (Java)"
	);

	public static final int DEFAULT_VERSION = 4;

	public static UmsUpnpService upnpService;
	private static HttpMediaServer httpMediaServer;
	private static boolean isStarted = false;
	private static ServerStatus status = ServerStatus.STOPPED;
	protected static int port = CONFIGURATION.getMediaServerPort();
	protected static String hostname;
	protected static InetAddress inetAddress;
	protected static NetworkInterface networkInterface;

	private static boolean init() {
		//get config ip port
		port = CONFIGURATION.getMediaServerPort();
		NetworkInterfaceAssociation ia = NetworkConfiguration.getNetworkInterfaceAssociationFromConfig();
		if (ia != null) {
			inetAddress = ia.getAddr();
			hostname = inetAddress.getHostAddress();
			networkInterface = ia.getIface();
			return true;
		}
		return false;
	}

	public static synchronized boolean start() {
		while (status == ServerStatus.STOPPING) {
			//wait while stopping
			try {
				Thread.sleep(100);
			} catch (InterruptedException ex) {
				LOGGER.info("Starting media server interrupted.");
				return false;
			}
		}
		if (!isStarted && (status == ServerStatus.STOPPED || status == ServerStatus.WAITING)) {
			status = ServerStatus.STARTING;
			if (!init()) {
				//network not available
				LOGGER.info("Network not available with the config values.");
				LOGGER.info("Waiting network scanner discovering.");
				isStarted = false;
				setWaiting();
				return isStarted;
			}
			//start the http service (for upnp and others)
			int engineVersion = CONFIGURATION.getServerEngine();
			if (engineVersion == 0 || !VERSIONS.containsKey(engineVersion)) {
				engineVersion = DEFAULT_VERSION;
			}
			try {
				switch (engineVersion) {
					case 1 -> {
						httpMediaServer = new SocketChannelServer(inetAddress, port);
						isStarted = httpMediaServer.start();
					}
					case 2 -> {
						httpMediaServer = new NettyServer(inetAddress, port);
						isStarted = httpMediaServer.start();
					}
					case 3 -> {
						httpMediaServer = new JavaHttpServer(inetAddress, port);
						isStarted = httpMediaServer.start();
					}
					default -> {
						//we will handle requests via JUPnP
						isStarted = true;
					}
				}
			} catch (IOException ex) {
				LOGGER.error("FATAL ERROR: Unable to bind on port: " + port + ", because: " + ex.getMessage());
				LOGGER.info("Maybe another process is running or the hostname is wrong.");
				isStarted = false;
				stop();
			}
			//start the upnp service
			if (isStarted && CONFIGURATION.isUpnpEnabled()) {
				if (upnpService == null) {
					LOGGER.debug("Starting UPnP (JUPnP) services.");
					switch (engineVersion) {
						case 4, 5 -> {
							upnpService = new UmsUpnpService(true);
							upnpService.startup();
						}
						default -> {
							upnpService = new UmsUpnpService(false);
							upnpService.startup();
						}
					}
				}
				try {
					isStarted = upnpService.getRouter().isEnabled();
				} catch (RouterException ex) {
					isStarted = false;
				}
				if (!isStarted) {
					LOGGER.error("FATAL ERROR: Unable to start upnp service");
				} else {
					for (DeviceType t : UPNPHelper.MEDIA_RENDERER_TYPES) {
						upnpService.getControlPoint().search(new DeviceTypeHeader(t));
					}
					LOGGER.debug("UPnP (JUPnP) services are online, listening for media renderers");
				}

				//then start SSDP service if JUPnP does not
				if (isStarted && upnpService.getRegistry().getLocalDevices().isEmpty()) {
					isStarted = SocketSSDPServer.start(networkInterface);
					if (!isStarted) {
						LOGGER.error("FATAL ERROR: Unable to start socket ssdp service");
						stop();
					}
				}
			}
			//start mDNS service
			if (isStarted) {
				isStarted = MDNS.start(inetAddress);
				if (!isStarted) {
					LOGGER.error("FATAL ERROR: Unable to start mDNS service");
					stop();
				}
			}
			status = isStarted ? ServerStatus.STARTED : ServerStatus.STOPPED;
		} else {
			LOGGER.debug("try to start the media server, but it's already started");
		}
		GuiManager.updateServerStatus();
		return isStarted;
	}

	public static synchronized void stop() {
		status = ServerStatus.STOPPING;
		MDNS.stop();
		SocketSSDPServer.stop();
		if (upnpService != null) {
			LOGGER.debug("Shutting down UPnP (JUPnP) service");
			upnpService.shutdown();
			upnpService = null;
			LOGGER.debug("UPnP service stopped");
		}
		if (httpMediaServer != null) {
			httpMediaServer.stop();
			httpMediaServer = null;
		}
		status = ServerStatus.STOPPED;
		isStarted = false;
		GuiManager.updateServerStatus();
	}

	public static boolean isStarted() {
		return status == ServerStatus.STARTED;
	}

	public static void setPort(int localPort) {
		port = localPort;
	}

	public static String getURL() {
		return getProtocol() + "://" + getAddress();
	}

	public static String getProtocol() {
		return httpMediaServer != null && httpMediaServer.isHTTPS() ? "https" : "http";
	}

	public static String getAddress() {
		return getHost() + ":" + getPort();
	}

	public static String getHost() {
		if (hostname != null) {
			return hostname;
		} else if (CONFIGURATION.getServerHostname() != null) {
			return CONFIGURATION.getServerHostname();
		} else {
			return "localhost";
		}
	}

	public static int getPort() {
		return port;
	}

	public static NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

	public static InetAddress getInetAddress() {
		return inetAddress;
	}

	private static void setWaiting() {
		status = ServerStatus.WAITING;
		//check a last time if network scanner has not fire before this set.
		startIfPossible();
	}

	public static void checkNetworkConfiguration() {
		if (status == ServerStatus.WAITING) {
			startIfPossible();
		} else if (status == ServerStatus.STARTED) {
			resetIfNeeded();
		}
	}

	private static void startIfPossible() {
		NetworkInterfaceAssociation ia = NetworkConfiguration.getNetworkInterfaceAssociationFromConfig();
		if (ia != null) {
			LOGGER.info("Starting the media server as network interface association was founded");
			start();
		}
	}

	public static void resetIfNeeded() {
		NetworkInterfaceAssociation ia = NetworkConfiguration.getNetworkInterfaceAssociationFromConfig();
		if (ia == null || ia.getAddr() != inetAddress || ia.getIface() != networkInterface) {
			//reset the server, network have changed
			//ia is null will fail into WAITING
			LOGGER.info("Restarting the media server as network configuration has changed");
			stop();
			start();
		}
	}

	private enum ServerStatus { STARTING, STARTED, STOPPING, STOPPED, WAITING }

	/**
	 * @return available server engines as a JSON array
	 */
	public static synchronized JsonArray getServerEnginesAsJsonArray() {
		JsonArray jsonArray = new JsonArray();

		JsonObject defaultOption = new JsonObject();
		defaultOption.addProperty("value", "0");
		defaultOption.addProperty("label", "i18n@Default");
		jsonArray.add(defaultOption);

		for (Entry<Integer, String> upnpEngineVersion : VERSIONS.entrySet()) {
			JsonObject version = new JsonObject();
			version.addProperty("value", upnpEngineVersion.getKey().toString());
			version.addProperty("label", upnpEngineVersion.getValue());
			jsonArray.add(version);
		}

		return jsonArray;
	}
}
