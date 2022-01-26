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

import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.javahttpserver.JavaHttpServer;
import net.pms.network.mediaserver.nettyserver.NettyServer;
import net.pms.network.mediaserver.socketchannelserver.SocketChannelServer;
import net.pms.network.mediaserver.socketssdpserver.SocketSSDPServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final Map<Integer, String> VERSIONS = Stream.of(new Object[][] {
			{1, "Sockets"},
			{2, "Netty"},
			{3, "Java"},
		}).collect(Collectors.toMap(data -> (Integer) data[0], data -> (String) data[1]));
	public static final int DEFAULT_VERSION = 2;

	private static HttpMediaServer httpMediaServer;
	private static boolean isStarted = false;

	public static void init() {
		UPNPHelper.getInstance().init();
	}

	public static boolean start() {
		if (!isStarted) {
			int engineVersion = CONFIGURATION.getServerEngine();
			if (engineVersion == 0 || !VERSIONS.containsKey(engineVersion)) {
				engineVersion = DEFAULT_VERSION;
			}
			//first start the http server
			try {
				switch (engineVersion) {
					case 1:
						httpMediaServer = new SocketChannelServer(CONFIGURATION.getServerPort());
						isStarted = httpMediaServer.start();
						break;
					case 2:
						httpMediaServer = new NettyServer(CONFIGURATION.getServerPort());
						isStarted = httpMediaServer.start();
						break;
					case 3:
						httpMediaServer = new JavaHttpServer(CONFIGURATION.getServerPort());
						isStarted = httpMediaServer.start();
						break;
				}
			} catch (IOException ex) {
				LOGGER.error("FATAL ERROR: Unable to bind on port: " + CONFIGURATION.getServerPort() + ", because: " + ex.getMessage());
				LOGGER.info("Maybe another process is running or the hostname is wrong.");
				isStarted = false;
				stop();
			}
			if (isStarted) {
				//then advise upnp
				isStarted = SocketSSDPServer.start();
				if (!isStarted) {
					LOGGER.error("FATAL ERROR: Unable to start ssdp service");
					isStarted = false;
					stop();
				}
			}
		} else {
			LOGGER.info("try to start the media server, but it's already started");
		}
		return isStarted;
	}

	public static void stop() {
		SocketSSDPServer.stop();
		if (httpMediaServer != null) {
			httpMediaServer.stop();
		}
		httpMediaServer = null;
		isStarted = false;
	}

	public static boolean isStarted() {
		return isStarted;
	}

	public static String getURL() {
		return httpMediaServer != null ? httpMediaServer.getURL() : "http://" + getHost() + ":" + getPort();
	}

	public static String getHost() {
		return httpMediaServer != null ? httpMediaServer.getHost() : CONFIGURATION.getServerHostname();
	}

	public static int getPort() {
		return httpMediaServer != null ? httpMediaServer.getPort() : CONFIGURATION.getServerPort();
	}

	public static NetworkInterface getNetworkInterface() {
		return httpMediaServer != null ? httpMediaServer.getNetworkInterface() : null;
	}

}
