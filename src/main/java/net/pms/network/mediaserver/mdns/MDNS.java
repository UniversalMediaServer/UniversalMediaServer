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
package net.pms.network.mediaserver.mdns;

import java.io.IOException;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.mediaserver.mdns.chromecast.ChromecastServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MDNS {
	private static final Logger LOGGER = LoggerFactory.getLogger(MDNS.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static JmDNS jmDNS;

	/**
	 * This class is not meant to be instantiated.
	 */
	private MDNS() {
	}

	public static boolean start(InetAddress inetAddress) {
		if (jmDNS == null && CONFIGURATION.useChromecastExt()) {
			if (RendererConfigurations.getRendererConfigurationByName("Chromecast") != null) {
				try {
					LOGGER.info("Starting mDNS service (JmDNS version {}) on host {}", JmDNS.VERSION, inetAddress.getHostAddress());
					jmDNS = JmDNS.create(inetAddress);
					ChromecastServiceListener.addService(jmDNS);
					LOGGER.info("mDNS Chromecast service started");
				} catch (IOException e) {
					LOGGER.info("Can't create mDNS Chromecast service: {}", e.getMessage());
					LOGGER.trace("The error was: " + e);
					return false;
				}
			} else {
				LOGGER.info("No Chromecast renderer found. Please enable one and restart.");
			}
		}
		return true;
	}

	public static void stop() {
		if (jmDNS != null) {
			String hostname = "";
			try {
				hostname = " on host " + jmDNS.getInetAddress().getHostAddress();
			} catch (IOException ex) {
			}
			LOGGER.info("Stopping mDNS service{}", hostname);
			try {
				jmDNS.close();
				LOGGER.info("mDNS service stopped");
			} catch (IOException e) {
				LOGGER.debug("mDNS service failed to stop with error: {}", e);
			} finally {
				jmDNS = null;
			}
		}
	}

}
