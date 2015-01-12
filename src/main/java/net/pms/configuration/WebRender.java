/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012  UMS developers.
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
		super(null);
		this.name = name;
		ip = "";
		port = 0;
		ua = "";
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
		} else if (ua.contains("playstation 4")) {
			rendererName += "PlayStation 4";
		} else if (ua.contains("xbox one")) {
			rendererName += "Xbox One";
		} else {
			rendererName += Messages.getString("PMS.142");
		}

		return rendererName;
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
		if (ua.contains("playstation 4")) {
			return "ps4.png";
		}
		if (ua.contains("xbox one")) {
			return "xbox-one.png";
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

	@Override
	public boolean isLimitFolders() {
		// no folder limit on the web clients
		return false;
	}
}
