/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network.webguiserver.servlets;

import com.google.gson.JsonObject;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.network.webguiserver.ServerSentEvents;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "SseApiServlet", urlPatterns = {"/v1/api/sse"}, displayName = "Sse Api Servlet")
public class SseApiServlet extends GuiHttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(SseApiServlet.class);
	private static final Map<Integer, ArrayList<ServerSentEvents>> SSE_INSTANCES = new HashMap<>();
	private static final List<ServerSentEvents> SSE_ABOUT_INSTANCES = new ArrayList<>();
	private static final List<ServerSentEvents> SSE_HOME_INSTANCES = new ArrayList<>();
	private static final List<ServerSentEvents> SSE_LOGS_INSTANCES = new ArrayList<>();
	private static final List<ServerSentEvents> SSE_SETTINGS_INSTANCES = new ArrayList<>();

	public static final String BASE_PATH = "/v1/api/sse";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path == null || path.equals("/")) {
				Account account = AuthService.getAccountLoggedIn(req);
				if (account != null && account.getUser().getId() > 0) {
					String sseType = null;
					URI referer = WebGuiServletHelper.getRequestReferer(req);
					if (referer != null) {
						sseType = referer.getPath();
					}
					resp.setHeader("Server", PMS.get().getServerName());
					resp.setHeader("Connection", "keep-alive");
					resp.setHeader("Cache-Control", "no-transform");
					resp.setHeader("Charset", "UTF-8");
					resp.setContentType("text/event-stream");
					AsyncContext async = req.startAsync();
					ServerSentEvents sse = new ServerSentEvents(async);
					addServerSentEventsFor(account.getUser().getId(), sse, sseType);
				} else {
					WebGuiServletHelper.respond(req, resp, "{\"error\": \"Forbidden\"}", 403, "application/json");
				}
			} else {
				WebGuiServletHelper.respond(req, resp, "{}", 404, "application/json");
			}
		} catch (RuntimeException e) {
			LOGGER.error("RuntimeException in SseApiServlet: {}", e.getMessage());
			WebGuiServletHelper.respond(req, resp, null, 500, "application/json");
		}
	}

	private static void addServerSentEventsFor(int id, ServerSentEvents sse, String sseType) {
		if (id > 0) {
			synchronized (SSE_INSTANCES) {
				if (!SSE_INSTANCES.containsKey(id)) {
					SSE_INSTANCES.put(id, new ArrayList<>());
				}
				SSE_INSTANCES.get(id).add(sse);
				if (sseType != null) {
					switch (sseType) {
						case WebGuiServlet.BASE_PATH -> SSE_HOME_INSTANCES.add(sse);
						case WebGuiServlet.LOGS_BASE_PATH -> SSE_LOGS_INSTANCES.add(sse);
						case WebGuiServlet.SETTINGS_BASE_PATH -> SSE_SETTINGS_INSTANCES.add(sse);
						case WebGuiServlet.ABOUT_BASE_PATH -> SSE_ABOUT_INSTANCES.add(sse);
					}
				}
			}
		}
	}

	private static boolean hasServerSentEvents() {
		synchronized (SSE_INSTANCES) {
			return !SSE_INSTANCES.isEmpty();
		}
	}

	public static boolean hasHomeServerSentEvents() {
		synchronized (SSE_INSTANCES) {
			return !SSE_HOME_INSTANCES.isEmpty();
		}
	}

	public static boolean hasLogsServerSentEvents() {
		synchronized (SSE_INSTANCES) {
			return !SSE_LOGS_INSTANCES.isEmpty();
		}
	}

	/**
	 * Broadcast a message to all Server Sent Events Streams
	 * @param message
	 */
	public static void broadcastMessage(String message) {
		broadcastMessage(message, true);
	}

	/**
	 * Broadcast a message to settings page Server Sent Events Streams
	 * @param message
	 */
	public static void broadcastSettingsMessage(String message) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<ServerSentEvents> sseIterator = SSE_SETTINGS_INSTANCES.iterator(); sseIterator.hasNext();) {
				ServerSentEvents sse = sseIterator.next();
				if (!sse.isOpened()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to about page Server Sent Events Streams
	 * @param message
	 */
	public static void broadcastAboutMessage(String message) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<ServerSentEvents> sseIterator = SSE_ABOUT_INSTANCES.iterator(); sseIterator.hasNext();) {
				ServerSentEvents sse = sseIterator.next();
				if (!sse.isOpened()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to home page Server Sent Events Streams
	 * @param message
	 */
	public static void broadcastHomeMessage(String message) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<ServerSentEvents> sseIterator = SSE_HOME_INSTANCES.iterator(); sseIterator.hasNext();) {
				ServerSentEvents sse = sseIterator.next();
				if (!sse.isOpened()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to logs page Server Sent Events Streams
	 * @param message
	 */
	public static void broadcastLogsMessage(String message) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<ServerSentEvents> sseIterator = SSE_LOGS_INSTANCES.iterator(); sseIterator.hasNext();) {
				ServerSentEvents sse = sseIterator.next();
				if (!sse.isOpened()) {
					sseIterator.remove();
				} else {
					//never log a log message
					sse.sendMessage(message, false);
				}
			}
		}
	}

	public static void broadcastMessage(String message, boolean log) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<Entry<Integer, ArrayList<ServerSentEvents>>> ssesIterator = SSE_INSTANCES.entrySet().iterator(); ssesIterator.hasNext();) {
				Entry<Integer, ArrayList<ServerSentEvents>> entry = ssesIterator.next();
				for (Iterator<ServerSentEvents> sseIterator = entry.getValue().iterator(); sseIterator.hasNext();) {
					ServerSentEvents sse = sseIterator.next();
					if (!sse.isOpened()) {
						sseIterator.remove();
					} else {
						sse.sendMessage(message, log);
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
	public static void broadcastMessageWithPermission(String message, int permission) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<Entry<Integer, ArrayList<ServerSentEvents>>> ssesIterator = SSE_INSTANCES.entrySet().iterator(); ssesIterator.hasNext();) {
				Entry<Integer, ArrayList<ServerSentEvents>> entry = ssesIterator.next();
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

	public static void notify(String id, String message, String title, String color, boolean autoClose) {
		if (hasServerSentEvents()) {
			JsonObject datas = new JsonObject();
			datas.addProperty("action", "notify");
			if (!StringUtils.isEmpty(id) && !StringUtils.isBlank(id)) {
				datas.addProperty("id", id);
			}
			if (!StringUtils.isEmpty(message)) {
				datas.addProperty("message", message);
			}
			if (!StringUtils.isEmpty(title)) {
				datas.addProperty("title", title);
			}
			if (!StringUtils.isEmpty(color)) {
				datas.addProperty("color", color);
			}
			datas.addProperty("autoClose", autoClose);
			broadcastMessage(datas.toString());
		}
	}

	public static void setRefreshSessions(List<Integer> ids) {
		if (hasServerSentEvents()) {
			for (int id : ids) {
				setRefreshSession(id);
			}
		}
	}

	public static void setRefreshSession(int id) {
		broadcastMessage("{\"action\":\"refresh_session\"}", id);
	}

	public static void setUpdateAccounts() {
		broadcastMessage("{\"action\":\"update_accounts\"}");
	}

	public static void setReloadable(boolean value) {
		broadcastMessage("{\"action\":\"set_reloadable\",\"value\":" + (value ? "true" : "false") + "}");
	}

	public static void setConfigurationChanged(String key) {
		if (hasServerSentEvents() && SettingsApiServlet.haveKey(key)) {
			broadcastMessageWithPermission(SettingsApiServlet.getConfigurationUpdate(key), Permissions.SETTINGS_VIEW);
		}
	}

	public static void setMemoryUsage(int maxMemory, int usedMemory, int bufferMemory) {
		String json = "{\"action\":\"update_memory\",\"max\":" + maxMemory + ",\"used\":" + usedMemory + ",\"buffer\":" + bufferMemory + "}";
		broadcastAboutMessage(json);
	}

	public static void appendLog(String msg) {
		if (hasLogsServerSentEvents()) {
			JsonObject result = new JsonObject();
			result.addProperty("action", "log_line");
			result.addProperty("value", msg);
			broadcastLogsMessage(result.toString());
		}
	}

	public static void setScanLibraryStatus(boolean enabled, boolean running) {
		broadcastSettingsMessage("{\"action\":\"set_scanlibrary_status\",\"enabled\":" + (enabled ? "true" : "false") + ",\"running\":" + (running ? "true" : "false") + "}");
	}
}
