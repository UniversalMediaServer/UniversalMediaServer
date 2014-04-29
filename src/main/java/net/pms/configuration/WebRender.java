package net.pms.configuration;

import java.net.InetAddress;
import net.pms.Messages;
import net.pms.PMS;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

public class WebRender extends RendererConfiguration {
	private String name;
	private String ip;
	private int port;
	private String ua;
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public WebRender(String name) throws ConfigurationException {
		super(null, null);
		this.name = name;
		ip = "";
		port = 0;
		ua = "";
		loaded = true;
	}

	@Override
	public String getRendererName() {
		String rendererName = "";
		if (configuration.isWebAuthenticate()) {
			rendererName = name + "@";
		}

		if (ua.contains("chrome")) {
			rendererName += "Chrome";
		} else  if (ua.contains("msie")) {
			rendererName += "Internet Explorer";
		} else if (ua.contains("firefox")) {
			rendererName += "Firefox";
		} else if (ua.contains("safari")) {
			rendererName += "Safari";
		} else {
			rendererName += Messages.getString("PMS.142");
		}

		return rendererName;
	}

	@Override
	public boolean associateIP(InetAddress sa) {
		ip = sa.getHostAddress();
		return super.associateIP(sa);
	}

	public void associatePort(int port) {
		this.port = port;
	}

	public void setUA(String ua) {
		this.ua = ua.toLowerCase();
	}

	@Override
	public String getRendererIcon() {
		if (StringUtils.isEmpty(ua)) {
			return super.getRendererIcon();
		}
		if (ua.contains("chrome")) {
			return "chrome.png";
		}
		if (ua.contains("msie")) {
			return "internetexplorer.png";
		}
		if (ua.contains("firefox")) {
			return "firefox.png";
		}
		return super.getRendererIcon();
	}

	@Override
	public String toString() {
		return getRendererName();
	}

	@Override
	public boolean isMediaParserV2ThumbnailGeneration() {
		return false;
	}
}
