package net.pms.network;


import ch.qos.logback.classic.Level;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
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
	private JmDNS ChromeCasts;
	private ArrayList<ChromeDevice> chromes;
	private RendererConfiguration ccr;

	public ChromecastMgr(JmDNS j) throws IOException {
		this.ChromeCasts = j;
		ccr = RendererConfiguration.getRendererConfigurationByName("Chromecast");
		ChromeCasts.addServiceListener(ChromeCast.SERVICE_TYPE, this);
		chromes = new ArrayList<>();
		if (!PMS.getConfiguration().isChromecastDbg()) {
			ch.qos.logback.classic.Logger l = (ch.qos.logback.classic.Logger)
											   LoggerFactory.getLogger("su.litvak.chromecast.api.v2");
			l.setLevel(Level.OFF);
		}
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		if (event.getInfo() == null) {
			LOGGER.debug("Bad Chromcast event " + event.toString());
			return;
		}
		LOGGER.debug("Found chromecast " + event.getInfo().getName());
		ChromeCast cc = new ChromeCast(ChromeCasts, event.getInfo().getName());
		try {
			cc.connect();
			chromes.add(new ChromeDevice(cc, ccr, InetAddress.getByName(cc.getAddress())));
		} catch (Exception e) {
			LOGGER.debug("Chromecast failed " + e);
			return;
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
			name = ccr.getConfName();
		}
		ArrayList<ChromeDevice> devs = new ArrayList<>();
		for (ChromeDevice d : chromes) {
			if (name.equals(d.getRendererName())) {
				// Make the icon grey and delete after 5 seconds
				d.delete(5000);
				LOGGER.debug("Chromecast " + name + " is gone.");
				continue;
			}
			devs.add(d);
		}
		chromes = devs;
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
	}

	static class ChromeDevice extends DeviceConfiguration {
		public ChromeCast api;

		public ChromeDevice(ChromeCast cc, RendererConfiguration r, InetAddress ia) throws ConfigurationException {
			super(r, ia);
			api = cc;
			uuid = cc.getAddress();
			controls = UPNPControl.ANY;
			active = true;
			associateIP(ia);
			PMS.get().setRendererFound(this);
		}

		@Override
		public String getRendererName() {
			try {
				if (StringUtils.isNotEmpty(api.getName())) {
					return api.getName();
				}
			} catch (Exception e) {
			}
			return getConfName();
		}

		@Override
		public BasicPlayer getPlayer() {
			if (player == null) {
				player = new ChromecastPlayer(this, api);
				((ChromecastPlayer)player).startPoll();
			}
			return player;
		}

		@Override
		public InetAddress getAddress() {
			try {
				return InetAddress.getByName(api.getAddress());
			} catch (Exception e) {
				return null;
			}
		}
	}
}
