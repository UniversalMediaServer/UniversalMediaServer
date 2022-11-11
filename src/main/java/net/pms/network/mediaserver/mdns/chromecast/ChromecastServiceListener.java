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
package net.pms.network.mediaserver.mdns.chromecast;

import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.net.InetAddress;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.RendererConfigurations;
import net.pms.renderers.devices.ChromecastDevice;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;

public class ChromecastServiceListener implements ServiceListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastServiceListener.class);

	private final RendererConfiguration rendererConf;

	private ArrayList<ChromecastDevice> chromeCasts;

	public ChromecastServiceListener() throws IOException {
		rendererConf = RendererConfigurations.getRendererConfigurationByName("Chromecast");
		chromeCasts = new ArrayList<>();
		if (!PMS.getConfiguration().isChromecastDbg()) {
			ch.qos.logback.classic.Logger logger =
				(ch.qos.logback.classic.Logger) LoggerFactory.getLogger("su.litvak.chromecast.api.v2");
			logger.setLevel(Level.OFF);
		}
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		if (event.getInfo() == null) {
			LOGGER.debug("Bad Chromcast event: {}", event.toString());
			return;
		}

		LOGGER.debug("Found chromecast: {}", event.getInfo().getName());
		ServiceInfo serviceInfo = event.getDNS().getServiceInfo(ChromeCast.SERVICE_TYPE, event.getInfo().getName());
		ChromeCast chromeCast = new ChromeCast(serviceInfo.getInet4Addresses()[0].getHostAddress(), serviceInfo.getPort());
		chromeCast.setName(event.getInfo().getName());
		chromeCast.setAppsURL(serviceInfo.getURLs().length == 0 ? null : serviceInfo.getURLs()[0]);
		chromeCast.setApplication(serviceInfo.getApplication());

		try {
			chromeCast.connect();
			ChromecastDevice chromecastDevice = new ChromecastDevice(chromeCast, rendererConf, InetAddress.getByName(chromeCast.getAddress()));
			PMS.get().setRendererFound(chromecastDevice);
			chromeCasts.add(chromecastDevice);
		} catch (IOException | GeneralSecurityException | ConfigurationException e) {
			LOGGER.error("Chromecast registration failed with the following error: {}", e);
			LOGGER.trace("", e);
		} catch (InterruptedException e) {
			LOGGER.info("Chromecast registration was interrupted");
		}
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
		if (event.getInfo() == null) {
			// silent
			return;
		}
		String name = event.getInfo().getName();
		if (StringUtils.isEmpty(name)) {
			name = rendererConf.getConfName();
		}
		ArrayList<ChromecastDevice> devices = new ArrayList<>();
		for (ChromecastDevice device : chromeCasts) {
			if (name.equals(device.getRendererName())) {
				// Make the icon grey and delete after 5 seconds
				device.delete(5000);
				LOGGER.debug("Chromecast \"{}\" is gone.", name);
				continue;
			}
			devices.add(device);
		}
		chromeCasts = devices;
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
		//ChromeCast service does not resolve.
	}

	public static void addService(JmDNS mDNS) throws IOException {
		mDNS.addServiceListener(ChromeCast.SERVICE_TYPE, new ChromecastServiceListener());
	}

}
