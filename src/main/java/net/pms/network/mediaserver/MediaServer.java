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

import net.pms.network.NetworkConfiguration;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.cling.UmsNoServerUpnpService;
import net.pms.network.mediaserver.javahttpserver.JavaHttpServer;
import net.pms.network.mediaserver.mdns.MDNS;
import net.pms.network.mediaserver.nettyserver.NettyServer;
import net.pms.network.mediaserver.socketchannelserver.SocketChannelServer;
import net.pms.network.mediaserver.socketssdpserver.SocketSSDPServer;
import org.apache.commons.lang3.StringUtils;
import org.fourthline.cling.UpnpService;
import org.fourthline.cling.model.message.header.DeviceTypeHeader;
import org.fourthline.cling.model.types.DeviceType;
import org.fourthline.cling.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final Map<Integer, String> VERSIONS = Stream.of(new Object[][] {
			{1, "Sockets"},
			{2, "Netty"},
			{3, "Java"},
			{4, "Netty + Cling"},
			{5, "Java + Cling"},
		}).collect(Collectors.toMap(data -> (Integer) data[0], data -> (String) data[1]));
	public static final int DEFAULT_VERSION = 2;

	public static UpnpService upnpService;
	private static HttpMediaServer httpMediaServer;
	private static boolean isStarted = false;
	protected static int port = CONFIGURATION.getServerPort();
	protected static String hostname;
	protected static InetAddress inetAddress;
	protected static NetworkInterface networkInterface;

	private static boolean init() {
		//get config ip port
		port = CONFIGURATION.getServerPort();
		//get config network interface
		NetworkConfiguration.forgetConfiguration();
		inetAddress = null;
		hostname = null;
		networkInterface = null;
		InetAddress tmpInetAddress = null;
		if (StringUtils.isNotEmpty(CONFIGURATION.getNetworkInterface())) {
			LOGGER.info("Using forced network interface: {}" + CONFIGURATION.getNetworkInterface());
			NetworkConfiguration.InterfaceAssociation ia = NetworkConfiguration.getInstance().getAddressForNetworkInterfaceName(CONFIGURATION.getNetworkInterface());
			if (ia != null) {
				networkInterface = ia.getIface();
				tmpInetAddress = ia.getAddr();
			} else {
				LOGGER.error("Forced network interface {} not found on this system", CONFIGURATION.getNetworkInterface().trim().replace('\n', ' '));
				LOGGER.info("Ignoring to network interface config value");
			}
		}
		hostname = CONFIGURATION.getServerHostname();
		//look for ip on forced address
		if (StringUtils.isNotBlank(hostname)) {
			LOGGER.info("Using forced address: {}", hostname);
			try {
				inetAddress = InetAddress.getByName(hostname);
			} catch (UnknownHostException ex) {
				hostname = null;
				LOGGER.error("Forced address {} is unknowned on this system", hostname);
				LOGGER.info("Falling back to default config values");
			}
			if (inetAddress != null) {
				hostname = inetAddress.getHostAddress();
				try {
					NetworkInterface tmpNetworkInterface = NetworkInterface.getByInetAddress(inetAddress);
					if (!networkInterface.equals(tmpNetworkInterface)) {
						LOGGER.error("Forced address {} not found on network interface {}", hostname, networkInterface.toString().trim().replace('\n', ' '));
						LOGGER.info("Ignoring to forced network interface");
						networkInterface = tmpNetworkInterface;
					}
				} catch (SocketException ex) {
				}
			}
		}
		//look for ip on forced network interface
		if (inetAddress == null && networkInterface != null) {
			if (tmpInetAddress != null) {
				inetAddress = tmpInetAddress;
				hostname = inetAddress.getHostAddress();
			} else {
				LOGGER.error("Forced network interface {} don't have any IP address assigned", networkInterface.toString().trim().replace('\n', ' '));
			}
		}
		//look for ip on default network interface
		if (inetAddress == null) {
			NetworkConfiguration.InterfaceAssociation ia = NetworkConfiguration.getInstance().getDefaultNetworkInterfaceAddress();
			if (ia != null) {
				networkInterface = ia.getIface();
				inetAddress = ia.getAddr();
				hostname = inetAddress.getHostAddress();
			} else {
				LOGGER.info("No default network interface found on this system");
			}
		}
		//look for localhost
		if (inetAddress == null) {
			try {
				networkInterface = null;
				inetAddress = InetAddress.getLocalHost();
				hostname = inetAddress.getHostAddress();
				LOGGER.info("Using localhost address");
			} catch (UnknownHostException ex) {
				LOGGER.error("FATAL ERROR: no IP address found on this system");
				return false;
			}
		}
		return true;
	}

	public static synchronized boolean start() {
		if (!isStarted) {
			//always refresh the network as it can been changed on configuration
			if (!init()) {
				isStarted = false;
				return isStarted;
			}
			//start the http service (for upnp and others)
			int engineVersion = CONFIGURATION.getServerEngine();
			if (engineVersion == 0 || !VERSIONS.containsKey(engineVersion)) {
				engineVersion = DEFAULT_VERSION;
			}
			//clean UpnpService if not reflect the conf
			//only true after a reset
			if (upnpService != null) {
				if (CONFIGURATION.isUpnpEnabled()) {
					switch (engineVersion) {
						case 4:
						case 5:
							if (!(upnpService instanceof UmsNoServerUpnpService)) {
								upnpService.shutdown();
								upnpService = null;
							} else {
								//ensure UMS devide is registered
								((UmsNoServerUpnpService) upnpService).addMediaServerDevice();
							}
							break;
						default:
							if (!(upnpService instanceof UmsNoServerUpnpService)) {
								upnpService.shutdown();
								upnpService = null;
							} else {
								//ensure UMS devide is NOT registered
								((UmsNoServerUpnpService) upnpService).removeMediaServerDevice();
							}
							break;
					}
				} else {
					LOGGER.debug("Stopping all UPnP (Cling) services.");
					upnpService.shutdown();
					upnpService = null;
				}
			}
			try {
				switch (engineVersion) {
					case 1:
						httpMediaServer = new SocketChannelServer(inetAddress, port);
						isStarted = httpMediaServer.start();
						break;
					case 2:
					case 4:
						httpMediaServer = new NettyServer(inetAddress, port);
						isStarted = httpMediaServer.start();
						break;
					case 3:
					case 5:
						httpMediaServer = new JavaHttpServer(inetAddress, port);
						isStarted = httpMediaServer.start();
						break;
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
					LOGGER.debug("Starting UPnP (Cling) services.");
					switch (engineVersion) {
						case 4:
						case 5:
							upnpService = new UmsNoServerUpnpService(UPNPHelper.getInstance(), true);
							break;
						default:
							upnpService = new UmsNoServerUpnpService(UPNPHelper.getInstance(), false);
							break;
					}
				} else {
					//come back from reset, let restart the sockets
					try {
						LOGGER.debug("Enabling UPnP (Cling) network services");
						upnpService.getRouter().enable();
					} catch (RouterException ex) {
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
					LOGGER.debug("UPnP (Cling) services are online, listening for media renderers");
				}

				//then start SSDP service if cling does not
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
		} else {
			LOGGER.debug("try to start the media server, but it's already started");
		}
		return isStarted;
	}

	public static synchronized void stop() {
		MDNS.stop();
		SocketSSDPServer.stop();
		if (upnpService != null) {
			//just disable the network router and keep the upnp registry
			LOGGER.debug("Disabling UPnP (Cling) network services");
			try {
				upnpService.getRouter().disable();
				LOGGER.debug("UPnP network services disabled");
			} catch (RouterException ex) {
			}
		}
		if (httpMediaServer != null) {
			httpMediaServer.stop();
			httpMediaServer = null;
		}
		NetworkConfiguration.forgetConfiguration();
		isStarted = false;
	}

	public static synchronized void shutdown() {
		MDNS.stop();
		SocketSSDPServer.stop();
		if (upnpService != null) {
			LOGGER.debug("Shutting down UPnP (Cling) service");
			upnpService.shutdown();
			upnpService = null;
			LOGGER.debug("UPnP service stopped");
		}
		if (httpMediaServer != null) {
			httpMediaServer.stop();
			httpMediaServer = null;
		}
		isStarted = false;
	}

	public static boolean isStarted() {
		return isStarted;
	}

	public static void setPort(int localPort) {
		port = localPort;
	}

	public static String getURL() {
		return getProtocol() + "://" + getHost() + ":" + getPort();
	}

	public static String getProtocol() {
		return httpMediaServer != null && httpMediaServer.isHTTPS() ? "https" : "http";
	}

	public static String getHost() {
		return hostname != null ? hostname : CONFIGURATION.getServerHostname();
	}

	public static int getPort() {
		return port;
	}

	public static NetworkInterface getNetworkInterface() {
		return networkInterface;
	}

}
