package net.pms.network;


import ch.qos.logback.classic.Level;
import net.pms.PMS;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.RendererConfiguration;
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
		DeviceConfiguration d;
		ChromeCast cc = new ChromeCast(jmDNS, event.getInfo().getName());
		try {
			// this is a bit stupid, but first fetch the conf from the conf file
			// then make special render obj to override the UPNP functions
		 	RendererConfiguration r = RendererConfiguration.getRendererConfigurationByName("Chromecast");
			d = new DeviceConfiguration(r);
			cc.connect();
			d.associateIP(InetAddress.getByName(cc.getAddress()));
			UPNPHelper.getInstance().mapRender(cc.getAddress(), d, UPNPControl.ANY);
		} catch (Exception e) {
			LOGGER.debug("Chromecast failed " + e);
			return;
		}
		ChromecastPlayer p = new ChromecastPlayer(d, cc);
		d.setPlayer(p);
		PMS.get().setRendererFound(d);
		p.startPoll();
	}

	@Override
	public void serviceRemoved(ServiceEvent event) {
	}

	@Override
	public void serviceResolved(ServiceEvent event) {
	}
}
