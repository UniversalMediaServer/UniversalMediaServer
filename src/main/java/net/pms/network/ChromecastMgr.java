package net.pms.network;


import ch.qos.logback.classic.Level;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.util.BasicPlayer;
import org.apache.commons.configuration.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import su.litvak.chromecast.api.v2.ChromeCast;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;

public class ChromecastMgr implements ServiceListener {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChromecastMgr.class);
	private JmDNS jmDNS;

	public ChromecastMgr() throws IOException {
		jmDNS = JmDNS.create();
		jmDNS.addServiceListener(ChromeCast.SERVICE_TYPE, this);
		ch.qos.logback.classic.Logger l = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("su.litvak.chromecast.api.v2");
		l.setLevel(Level.OFF);
	}

	public void stop() throws IOException{
		jmDNS.close();
	}

	@Override
	public void serviceAdded(ServiceEvent event) {
		if (event.getInfo() == null) {
			LOGGER.debug("Bad Chromcast event " + event.toString());
			return;
		}
		LOGGER.debug("Found chromecast " + event.getInfo().getName());
		ChromeCast cc = new ChromeCast(jmDNS, event.getInfo().getName());
		try {
			cc.connect();
			new ChromeDevice(cc, InetAddress.getByName(cc.getAddress()));
		} catch (Exception e) {
			LOGGER.debug("Chromecast failed " + e);
			return;
		}
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
	}

	static class ChromeDevice extends DeviceConfiguration {
		public ChromeCast api;

		public ChromeDevice(ChromeCast cc, InetAddress ia) throws ConfigurationException {
			super(RendererConfiguration.getRendererConfigurationByName("Chromecast"), ia);
			api = cc;
			uuid = cc.getAddress();
			controls = UPNPControl.ANY;
			active = true;
			UPNPHelper.getInstance().addRenderer(this);
			associateIP(ia);
			PMS.get().setRendererFound(this);
		}

		@Override
		public String getRendererName() {
			try {
				return ((ChromecastPlayer)player).getName();
			} catch (Exception e) {
				return getConfName();
			}
		}

		@Override
		public BasicPlayer getPlayer() {
			if (player == null) {
				player = new ChromecastPlayer(this, api);
				((ChromecastPlayer)player).startPoll();
			}
			return player;
		}
	}
}
