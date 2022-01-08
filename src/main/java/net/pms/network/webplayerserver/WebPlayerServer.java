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
package net.pms.network.webplayerserver;

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

public abstract class WebPlayerServer implements WebPlayerServerInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayerServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected static final int DEFAULT_PORT = CONFIGURATION.getWebPlayerServerPort();
	private WebPlayerServerInterface webServer;
	protected final Map<String, RootFolder> roots;
	protected final WebPlayerServerUtil.ResourceManager resources;

	public WebPlayerServer() throws IOException {
		roots = new HashMap<>();
		// Add "classpaths" for resolving web resources
		resources = AccessController.doPrivileged((PrivilegedAction<WebPlayerServerUtil.ResourceManager>) () -> new WebPlayerServerUtil.ResourceManager(
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

	public WebPlayerServerUtil.ResourceManager getResources() {
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

	public static WebPlayerServer createServer(int port) throws IOException {
		LOGGER.debug("Using httpserver as web server");
		return new WebPlayerServerHttpServer(port);
	}
}
