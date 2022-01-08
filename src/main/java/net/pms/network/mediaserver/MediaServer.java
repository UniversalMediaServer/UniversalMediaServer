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
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.nettyserver.NettyServer;
import net.pms.network.mediaserver.socketchannelserver.SocketChannelServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static HttpMediaServer httpMediaServer;
	private static boolean isStarted = false;

	public static void init() {
		UPNPHelper.getInstance().init();
	}

	public static boolean start() {
		if (!isStarted) {
			//first start the http server
			try {
				if (CONFIGURATION.isHTTPEngineV2()) {
					httpMediaServer = new NettyServer(CONFIGURATION.getServerPort());
					isStarted = httpMediaServer.start();
				} else {
					httpMediaServer = new SocketChannelServer(CONFIGURATION.getServerPort());
					isStarted = httpMediaServer.start();
				}
			} catch (IOException ex) {
				LOGGER.error("FATAL ERROR: Unable to bind on port: " + CONFIGURATION.getServerPort() + ", because: " + ex.getMessage());
				LOGGER.info("Maybe another process is running or the hostname is wrong.");
				isStarted = false;
				stop();
			}
			if (isStarted) {
				//then advise upnp
				try {
					UPNPHelper.getInstance().createMulticastSocket();
					UPNPHelper.sendAlive();
					UPNPHelper.sendByeBye();
					LOGGER.trace("Waiting 250 milliseconds...");
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
					}
					UPNPHelper.sendAlive();
					LOGGER.trace("Waiting 250 milliseconds...");
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
					}
					UPNPHelper.listen();
				} catch (IOException ex) {
					LOGGER.error("FATAL ERROR: Unable to start upnp service, because: " + ex.getMessage());
					isStarted = false;
					stop();
				}
			}
		} else {
			LOGGER.error("try to start the media server, but it's already started");
		}
		return isStarted;
	}

	public static void stop() {
		UPNPHelper.shutDownListener();
		UPNPHelper.sendByeBye();
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
