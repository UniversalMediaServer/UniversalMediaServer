/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.webguiserver;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.servlets.SettingsApiServlet;
import net.pms.network.webguiserver.servlets.WebGuiServlet;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Surf@ceS
 */
public class EventSourceServer {

	private static final Map<Integer, ArrayList<EventSourceClient>> SSE_INSTANCES = new HashMap<>();
	private static final List<EventSourceClient> SSE_ABOUT_INSTANCES = new ArrayList<>();
	private static final List<EventSourceClient> SSE_HOME_INSTANCES = new ArrayList<>();
	private static final List<EventSourceClient> SSE_LOGS_INSTANCES = new ArrayList<>();
	private static final List<EventSourceClient> SSE_SETTINGS_INSTANCES = new ArrayList<>();
	private static final List<EventSourceClient> SSE_SHARED_INSTANCES = new ArrayList<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	private EventSourceServer() {
	}

	public static void addServerSentEventsFor(int id, EventSourceClient sse, String sseType) {
		if (id > 0) {
			synchronized (SSE_INSTANCES) {
				if (!SSE_INSTANCES.containsKey(id)) {
					SSE_INSTANCES.put(id, new ArrayList<>());
				}
				SSE_INSTANCES.get(id).add(sse);
				if (sseType != null) {
					switch (sseType) {
						case WebGuiServlet.BASE_PATH -> {
							synchronized (SSE_HOME_INSTANCES) {
								SSE_HOME_INSTANCES.add(sse);
							}
						}
						case WebGuiServlet.LOGS_BASE_PATH -> {
							synchronized (SSE_LOGS_INSTANCES) {
								SSE_LOGS_INSTANCES.add(sse);
							}
						}
						case WebGuiServlet.SETTINGS_BASE_PATH -> {
							synchronized (SSE_SETTINGS_INSTANCES) {
								SSE_SETTINGS_INSTANCES.add(sse);
							}
						}
						case WebGuiServlet.SHARED_BASE_PATH -> {
							synchronized (SSE_SHARED_INSTANCES) {
								SSE_SHARED_INSTANCES.add(sse);
							}
						}
						case WebGuiServlet.ABOUT_BASE_PATH -> {
							synchronized (SSE_ABOUT_INSTANCES) {
								SSE_ABOUT_INSTANCES.add(sse);
							}
						}
						default -> {
							//nothing to do
						}
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
		synchronized (SSE_HOME_INSTANCES) {
			return !SSE_HOME_INSTANCES.isEmpty();
		}
	}

	public static boolean hasAboutServerSentEvents() {
		synchronized (SSE_ABOUT_INSTANCES) {
			return !SSE_ABOUT_INSTANCES.isEmpty();
		}
	}

	public static boolean hasLogsServerSentEvents() {
		synchronized (SSE_LOGS_INSTANCES) {
			return !SSE_LOGS_INSTANCES.isEmpty();
		}
	}

	/**
	 * Broadcast a message to all Server Sent Events Streams
	 *
	 * @param message
	 */
	public static void broadcastMessage(String message) {
		broadcastMessage(message, true);
	}

	/**
	 * Broadcast a message to settings page Server Sent Events Streams
	 *
	 * @param message
	 */
	public static void broadcastSettingsMessage(String message) {
		synchronized (SSE_SETTINGS_INSTANCES) {
			for (Iterator<EventSourceClient> sseIterator = SSE_SETTINGS_INSTANCES.iterator(); sseIterator.hasNext();) {
				EventSourceClient sse = sseIterator.next();
				if (sse.isClosed()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to settings page Server Sent Events Streams
	 *
	 * @param message
	 */
	public static void broadcastSharedMessage(String message) {
		synchronized (SSE_SHARED_INSTANCES) {
			for (Iterator<EventSourceClient> sseIterator = SSE_SHARED_INSTANCES.iterator(); sseIterator.hasNext();) {
				EventSourceClient sse = sseIterator.next();
				if (sse.isClosed()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to about page Server Sent Events Streams
	 *
	 * @param message
	 */
	public static void broadcastAboutMessage(String message) {
		synchronized (SSE_ABOUT_INSTANCES) {
			for (Iterator<EventSourceClient> sseIterator = SSE_ABOUT_INSTANCES.iterator(); sseIterator.hasNext();) {
				EventSourceClient sse = sseIterator.next();
				if (sse.isClosed()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to home page Server Sent Events Streams
	 *
	 * @param message
	 */
	public static void broadcastHomeMessage(String message) {
		synchronized (SSE_HOME_INSTANCES) {
			for (Iterator<EventSourceClient> sseIterator = SSE_HOME_INSTANCES.iterator(); sseIterator.hasNext();) {
				EventSourceClient sse = sseIterator.next();
				if (sse.isClosed()) {
					sseIterator.remove();
				} else {
					sse.sendMessage(message);
				}
			}
		}
	}

	/**
	 * Broadcast a message to logs page Server Sent Events Streams
	 *
	 * @param message
	 */
	public static void broadcastLogsMessage(String message) {
		synchronized (SSE_LOGS_INSTANCES) {
			for (Iterator<EventSourceClient> sseIterator = SSE_LOGS_INSTANCES.iterator(); sseIterator.hasNext();) {
				EventSourceClient sse = sseIterator.next();
				if (sse.isClosed()) {
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
			for (Iterator<Map.Entry<Integer, ArrayList<EventSourceClient>>> ssesIterator = SSE_INSTANCES.entrySet().iterator(); ssesIterator.hasNext();) {
				Map.Entry<Integer, ArrayList<EventSourceClient>> entry = ssesIterator.next();
				for (Iterator<EventSourceClient> sseIterator = entry.getValue().iterator(); sseIterator.hasNext();) {
					EventSourceClient sse = sseIterator.next();
					if (sse.isClosed()) {
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
	 * Broadcast a message to all Server Sent Events Streams if account have the
	 * requested permission.
	 *
	 * @param message
	 * @param permission
	 */
	public static void broadcastMessageWithPermission(String message, int permission) {
		synchronized (SSE_INSTANCES) {
			for (Iterator<Map.Entry<Integer, ArrayList<EventSourceClient>>> ssesIterator = SSE_INSTANCES.entrySet().iterator(); ssesIterator.hasNext();) {
				Map.Entry<Integer, ArrayList<EventSourceClient>> entry = ssesIterator.next();
				Account account = AccountService.getAccountByUserId(entry.getKey());
				if (account.havePermission(permission)) {
					for (Iterator<EventSourceClient> sseIterator = entry.getValue().iterator(); sseIterator.hasNext();) {
						EventSourceClient sse = sseIterator.next();
						if (sse.isClosed()) {
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
	 *
	 * @param message
	 * @param id
	 */
	public static void broadcastMessage(String message, int id) {
		synchronized (SSE_INSTANCES) {
			if (SSE_INSTANCES.containsKey(id)) {
				for (Iterator<EventSourceClient> sseIterator = SSE_INSTANCES.get(id).iterator(); sseIterator.hasNext();) {
					EventSourceClient sse = sseIterator.next();
					if (sse.isClosed()) {
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

	public static void setMemoryUsage(int maxMemory, int usedMemory, int dbCacheMemory, int bufferMemory) {
		if (hasAboutServerSentEvents()) {
			String json = "{\"action\":\"update_memory\",\"max\":" + maxMemory + ",\"used\":" + usedMemory + ",\"dbcache\":" + dbCacheMemory + ",\"buffer\":" + bufferMemory + "}";
			broadcastAboutMessage(json);
		}
	}

	public static void appendLog(String msg) {
		if (hasLogsServerSentEvents()) {
			JsonObject result = new JsonObject();
			result.addProperty("action", "log_line");
			result.addProperty("value", msg);
			broadcastLogsMessage(result.toString());
		}
	}

	public static void setMediaScanStatus(boolean running) {
		broadcastSettingsMessage("{\"action\":\"set_media_scan_status\",\"running\":" + (running ? "true" : "false") + "}");
	}

	public static void setStatusLine(String line) {
		JsonObject result = new JsonObject();
		result.addProperty("action", "set_status_line");
		result.addProperty("value", line);
		broadcastMessage(result.toString());
	}

}
