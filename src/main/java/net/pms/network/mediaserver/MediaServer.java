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
package net.pms.network.mediaserver;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.UUID;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.gui.GuiManager;
import net.pms.network.configuration.NetworkConfiguration;
import net.pms.network.configuration.NetworkInterfaceAssociation;
import net.pms.network.mediaserver.jupnp.UmsUpnpService;
import net.pms.network.mediaserver.mdns.MDNS;
import net.pms.renderers.JUPnPDeviceHelper;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static boolean isStarted = false;
	private static ServerStatus status = ServerStatus.STOPPED;
	private static int port = CONFIGURATION.getMediaServerPort();
	private static String hostname;
	private static InetAddress inetAddress;
	private static NetworkInterface networkInterface;
	/**
	 * User friendly name for the server.
	 */
	private static String serverName;
	/**
	 * Universally Unique Identifier used in the UPnP mediaServer.
	 */
	private static String uuid;
	public static UmsUpnpService upnpService;

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
				Thread.currentThread().interrupt();
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
			//start the upnp service
			if (CONFIGURATION.isUpnpEnabled()) {
				if (upnpService == null) {
					LOGGER.debug("Starting UPnP (JUPnP) services.");
					upnpService = new UmsUpnpService(true);
					upnpService.startup();
				}
				try {
					isStarted = upnpService.getRouter().isEnabled();
				} catch (RouterException ex) {
					isStarted = false;
				}
				if (!isStarted) {
					LOGGER.error("FATAL ERROR: Unable to start upnp service");
				} else {
					upnpService.sendAlive();
					JUPnPDeviceHelper.searchMediaRendererDevices();
					LOGGER.debug("UPnP (JUPnP) services are online, listening for media renderers");
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
		if (upnpService != null) {
			LOGGER.debug("Shutting down UPnP (JUPnP) service");
			upnpService.shutdown();
			upnpService = null;
			LOGGER.debug("UPnP service stopped");
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
		return "http://" + getAddress();
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
			LOGGER.info("Starting the media server as network interface association was found");
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
	 * Returns the user friendly name of the UMS server.
	 *
	 * @return {@link String} with the user friendly name.
	 */
	public static String getServerName() {
		if (serverName == null) {
			StringBuilder sb = new StringBuilder();
			sb.append(System.getProperty("os.name").replace(" ", "_"));
			sb.append('-');
			sb.append(System.getProperty("os.arch").replace(" ", "_"));
			sb.append('-');
			sb.append(System.getProperty("os.version").replace(" ", "_"));
			sb.append(", UPnP/1.0 DLNADOC/1.50, UMS/").append(PMS.getVersion());
			serverName = sb.toString();
		}
		return serverName;
	}

	/**
	 * Get unique device name from {@link #uuid}.
	 *
	 * @return {@link String} with an unique device name.
	 */
	public static String getUniqueDeviceName() {
		return "uuid:" + getUuid();
	}

	/**
	 * Get saved server {@link #uuid} or creates a new random one.
	 * <p>
	 * These are used to uniquely identify the server to renderers (i.e.
	 * renderers treat multiple servers with the same UUID as the same server).
	 *
	 * @return {@link String} with an Universally Unique Identifier.
	 */
	// XXX don't use the MAC address to seed the UUID as it breaks multiple profiles
	public static synchronized String getUuid() {
		if (uuid == null) {
			// Retrieve UUID from configuration
			uuid = CONFIGURATION.getUuid();

			if (uuid == null) {
				uuid = UUID.randomUUID().toString();
				LOGGER.info("Generated new random UUID: {}", uuid);

				// save the newly-generated UUID
				CONFIGURATION.setUuid(uuid);
				CONFIGURATION.saveConfiguration();
			}

			LOGGER.info("Using the following UUID configured in UMS.conf: {}", uuid);
		}

		return uuid;
	}

}
