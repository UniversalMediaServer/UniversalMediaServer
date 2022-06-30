/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.RootFolder;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebInterfaceServer implements WebInterfaceServerInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebInterfaceServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected static final int DEFAULT_PORT = CONFIGURATION.getWebInterfaceServerPort();
	protected static final Map<Integer, ArrayList<ServerSentEvents>> SSE_INSTANCES = new HashMap<>();

	protected final Map<String, RootFolder> roots;
	protected final WebInterfaceServerUtil.ResourceManager resources;

	private WebInterfaceServerInterface webServer;

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

	public static boolean hasServerSentEvents() {
		synchronized (SSE_INSTANCES) {
			return !SSE_INSTANCES.isEmpty();
		}
	}

	public static void addServerSentEventsFor(int id, ServerSentEvents sse) {
		if (id > 0) {
			synchronized (SSE_INSTANCES) {
				if (!SSE_INSTANCES.containsKey(id)) {
					SSE_INSTANCES.put(id, new ArrayList<>());
				}
				SSE_INSTANCES.get(id).add(sse);
			}
		}
	}

	/**
	 * Broadcast a message to all Server Sent Events Streams
	 * @param message
	 */
	public static void broadcastMessage(String message) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<Map.Entry<Integer, ArrayList<ServerSentEvents>>> ssesIterator = SSE_INSTANCES.entrySet().iterator(); ssesIterator.hasNext();) {
				Map.Entry<Integer, ArrayList<ServerSentEvents>> entry = ssesIterator.next();
				for (Iterator<ServerSentEvents> sseIterator = entry.getValue().iterator(); sseIterator.hasNext();) {
					ServerSentEvents sse = sseIterator.next();
					if (!sse.isOpened()) {
						sseIterator.remove();
					} else {
						sse.sendMessage(message);
					}
				}
				if (entry.getValue().isEmpty()) {
					ssesIterator.remove();
				}
			}
		}
	}

	/**
	 * Broadcast a message to all Server Sent Events Streams if account
	 * have the requested permission.
	 * @param message
	 * @param permission
	 */
	public static void broadcastMessage(String message, String permission) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<Map.Entry<Integer, ArrayList<ServerSentEvents>>> ssesIterator = SSE_INSTANCES.entrySet().iterator(); ssesIterator.hasNext();) {
				Map.Entry<Integer, ArrayList<ServerSentEvents>> entry = ssesIterator.next();
				Account account = AccountService.getAccountByUserId(entry.getKey());
				if (account.havePermission(permission)) {
					for (Iterator<ServerSentEvents> sseIterator = entry.getValue().iterator(); sseIterator.hasNext();) {
						ServerSentEvents sse = sseIterator.next();
						if (!sse.isOpened()) {
							sseIterator.remove();
						} else {
							sse.sendMessage(message);
						}
					}
				}
				if (entry.getValue().isEmpty()) {
					ssesIterator.remove();
				}
			}
		}
	}

	/**
	 * Broadcast a message to all Server Sent Events Streams to a specific
	 * account.
	 * @param message
	 * @param id
	 */
	public static void broadcastMessage(String message, int id) {
		synchronized (SSE_INSTANCES) {
			if (SSE_INSTANCES.containsKey(id)) {
				for (Iterator<ServerSentEvents> sseIterator = SSE_INSTANCES.get(id).iterator(); sseIterator.hasNext();) {
					ServerSentEvents sse = sseIterator.next();
					if (!sse.isOpened()) {
						sseIterator.remove();
					} else {
						sse.sendMessage(message);
					}
				}
				if (SSE_INSTANCES.get(id).isEmpty()) {
					SSE_INSTANCES.remove(id);
				}
			}
		}
	}

}
