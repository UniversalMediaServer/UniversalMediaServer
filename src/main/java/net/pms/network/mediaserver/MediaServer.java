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
import net.pms.network.mediaserver.javahttpserver.JavaHttpServer;
import net.pms.network.mediaserver.jupnp.UmsUpnpService;
import net.pms.network.mediaserver.mdns.MDNS;
import net.pms.network.mediaserver.nettyserver.NettyServer;
import net.pms.network.mediaserver.socketchannelserver.SocketChannelServer;
import net.pms.network.mediaserver.socketssdpserver.SocketSSDPServer;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.model.message.header.DeviceTypeHeader;
import org.jupnp.model.types.DeviceType;
import org.jupnp.transport.RouterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final Map<Integer, String> VERSIONS = Stream.of(new Object[][] {
			{1, "Sockets"},
			{2, "Netty"},
			{3, "Java"},
			{4, "JUPnP (Netty)"},
			{5, "JUPnP (Java)"},
		}).collect(Collectors.toMap(data -> (Integer) data[0], data -> (String) data[1]));
	public static final int DEFAULT_VERSION = 2;

	public static UmsUpnpService upnpService;
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
			try {
				switch (engineVersion) {
					case 1:
						httpMediaServer = new SocketChannelServer(inetAddress, port);
						isStarted = httpMediaServer.start();
						break;
					case 2:
						httpMediaServer = new NettyServer(inetAddress, port);
						isStarted = httpMediaServer.start();
						break;
					case 3:
						httpMediaServer = new JavaHttpServer(inetAddress, port);
						isStarted = httpMediaServer.start();
						break;
					default:
						//we will handle requests via JUPnP
						isStarted = true;
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
					LOGGER.debug("Starting UPnP (JUPnP) services.");
					switch (engineVersion) {
						case 4:
						case 5:
							upnpService = new UmsUpnpService(true);
							upnpService.startup();
							break;
						default:
							upnpService = new UmsUpnpService(false);
							upnpService.startup();
							break;
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
		} else {
			LOGGER.debug("try to start the media server, but it's already started");
		}
		return isStarted;
	}

	public static synchronized void stop() {
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
		NetworkConfiguration.forgetConfiguration();
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
