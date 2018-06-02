package net.pms.network;


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
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.util.BasicPlayer;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;

public class ChromecastMgr implements ServiceListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastMgr.class);
	private JmDNS mDNS;
	private ArrayList<ChromeDevice> chromeCasts;
	private RendererConfiguration renderer;

	public ChromecastMgr(JmDNS mDNS) throws IOException {
		this.mDNS = mDNS;
		renderer = RendererConfiguration.getRendererConfigurationByName("Chromecast");
		mDNS.addServiceListener(ChromeCast.SERVICE_TYPE, this);
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
		ServiceInfo serviceInfo = mDNS.getServiceInfo(ChromeCast.SERVICE_TYPE, event.getInfo().getName());
		ChromeCast chromeCast = new ChromeCast(serviceInfo.getInet4Addresses()[0].getHostAddress(), serviceInfo.getPort());
		chromeCast.setName(event.getInfo().getName());
		chromeCast.setAppsURL(serviceInfo.getURLs().length == 0 ? null : serviceInfo.getURLs()[0]);
		chromeCast.setApplication(serviceInfo.getApplication());

		try {
			chromeCast.connect();
			chromeCasts.add(new ChromeDevice(chromeCast, renderer, InetAddress.getByName(chromeCast.getAddress())));
		} catch (IOException | GeneralSecurityException | ConfigurationException e) {
			LOGGER.error("Chromecast registration failed with the following error: {}", e);
			LOGGER.trace("", e);
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
			name = renderer.getConfName();
		}
		ArrayList<ChromeDevice> devices = new ArrayList<>();
		for (ChromeDevice device : chromeCasts) {
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
	}

	static class ChromeDevice extends DeviceConfiguration {
		public ChromeCast chromeCast;

		public ChromeDevice(
			ChromeCast chromeCast,
			RendererConfiguration renderer,
			InetAddress inetAddress
		) throws ConfigurationException {
			super(renderer, inetAddress);
			this.chromeCast = chromeCast;
			uuid = chromeCast.getAddress();
			controls = UPNPControl.ANY;
			active = true;
			associateIP(inetAddress);
			PMS.get().setRendererFound(this);
		}

		@Override
		public String getRendererName() {
			try {
				if (StringUtils.isNotEmpty(chromeCast.getName())) {
					return chromeCast.getName();
				}
			} catch (Exception e) {
				LOGGER.debug("Failed to find name for Chromecast \"{}\"", chromeCast);
				LOGGER.trace("", e);
			}
			return getConfName();
		}

		@Override
		public BasicPlayer getPlayer() {
			if (player == null) {
				player = new ChromecastPlayer(this, chromeCast);
				((ChromecastPlayer)player).startPoll();
			}
			return player;
		}

		@Override
		public InetAddress getAddress() {
			try {
				return InetAddress.getByName(chromeCast.getAddress());
			} catch (Exception e) {
				LOGGER.debug("Failed to find address for Chromecast \"{}\"", chromeCast);
				LOGGER.trace("", e);
				return null;
			}
		}
	}
}
