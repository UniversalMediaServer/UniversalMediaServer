package net.pms.configuration;

import java.net.InetAddress;
import net.pms.Messages;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;

public class WebRender extends RendererConfiguration {
	private String name;
	private String ip;
	private int port;
	private String ua;

	public WebRender(String name) throws ConfigurationException {
		super(null);
		this.name = name;
		ip = "";
		port = 0;
		ua = "";
	}

	@Override
	public String getRendererName() {
		String ipStr;
		if (StringUtils.isNotEmpty(ip)) {
			ipStr = "\n(" + ip + (port != 0 ? ":" + port : "") + ")";
		} else {
			ipStr = "";
		}
		return name + "@\n" + Messages.getString("PMS.142") + ipStr;
	}

	@Override
	public void associateIP(InetAddress sa) {
		super.associateIP(sa);
		ip = sa.getHostAddress();
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
}
