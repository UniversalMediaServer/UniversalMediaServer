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
package net.pms.network.webinterfaceserver;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.RootFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebInterfaceServer implements WebInterfaceServerInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebInterfaceServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected static final int DEFAULT_PORT = CONFIGURATION.getWebinterfaceServerPort();
	private WebInterfaceServerInterface webServer;
	protected final Map<String, RootFolder> roots;
	protected final WebInterfaceServerUtil.ResourceManager resources;

	public WebInterfaceServer() throws IOException {
		roots = new HashMap<>();
		// Add "classpaths" for resolving web resources
		resources = AccessController.doPrivileged((PrivilegedAction<WebInterfaceServerUtil.ResourceManager>) () -> new WebInterfaceServerUtil.ResourceManager(
				"file:" + CONFIGURATION.getProfileDirectory() + "/web/",
				"jar:file:" + CONFIGURATION.getProfileDirectory() + "/web.zip!/",
				"file:" + CONFIGURATION.getWebPath() + "/"
		));
	}

	public String getTag(String user) {
		String tag = PMS.getCredTag("web", user);
		if (tag == null) {
			return user;
		}
		return tag;
	}

	public WebInterfaceServerUtil.ResourceManager getResources() {
		return resources;
	}

	@Override
	public Object getServer() {
		return webServer.getServer();
	}

	@Override
	public int getPort() {
		return webServer.getPort();
	}

	@Override
	public String getAddress() {
		return webServer.getAddress();
	}

	@Override
	public String getUrl() {
		return webServer.getUrl();
	}

	@Override
	public boolean isSecure() {
		return webServer.isSecure();
	}

	public static WebInterfaceServer createServer(int port) throws IOException {
		LOGGER.debug("Using httpserver as web interface server");
		return new WebInterfaceServerHttpServer(port);
	}
}
