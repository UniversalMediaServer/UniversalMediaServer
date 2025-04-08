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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.ToNumberPolicy;
import jakarta.websocket.Session;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.pms.iam.Permissions;
import net.pms.network.webguiserver.servlets.SettingsApiServlet;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class WebSocketDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketDispatcher.class);
	private static final Map<String, WebSocketSession> WS_SESSIONS = Collections.synchronizedMap(new HashMap<>());
	private static final List<String> ABOUT_SESSIONS = Collections.synchronizedList(new LinkedList<>());
	private static final List<String> ACCOUNT_SESSIONS = Collections.synchronizedList(new LinkedList<>());
	private static final List<String> HOME_SESSIONS = Collections.synchronizedList(new LinkedList<>());
	private static final List<String> LOGS_SESSIONS = Collections.synchronizedList(new LinkedList<>());
	private static final List<String> SETTINGS_SESSIONS = Collections.synchronizedList(new LinkedList<>());
	private static final List<String> SHARED_SESSIONS = Collections.synchronizedList(new LinkedList<>());
	private static final Gson GSON = new GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create();
	private static final String ACTION_STRING = "action";
	private static final String DATA_STRING = "data";

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebSocketDispatcher() {
	}

	public static void add(Session session) {
		WS_SESSIONS.put(session.getId(), new WebSocketSession(session));
	}

	public static void remove(Session session) {
		unsubscribe(session.getId());
		WS_SESSIONS.remove(session.getId());
	}

	public static void onMessage(Session session, String message) {
		//filter non json messages and handle client request when we will implement
		JsonObject jsonMessage = jsonObjectFromString(message);
		if (jsonMessage == null || !jsonMessage.has(ACTION_STRING) || !jsonMessage.get(ACTION_STRING).isJsonPrimitive()) {
			LOGGER.debug("Error onMessage session '{}' with message '{}'", session.getId(), message);
			return;
		}
		String action = jsonMessage.get(ACTION_STRING).getAsString();
		JsonElement data = jsonMessage.get(DATA_STRING);
		switch (action) {
			case "subscribe" ->
				subscribeHandler(session.getId(), data);
			case "token" ->
				tokenHandler(session.getId(), data);
			case "uuid" ->
				uuidHandler(session.getId(), data);
			default ->
				LOGGER.debug("Unhandled action '{}' for session '{}' with message '{}'", action, session.getId(), message);
		}
	}

	public static void broadcastMessage(String message) {
		for (WebSocketSession session : WS_SESSIONS.values()) {
			try {
				session.sendText(message);
			} catch (IOException e) {
				LOGGER.debug("Error broadcasting to '{}' with message : {}", session.getId(), message);
				LOGGER.trace("", e);
			}
		}
	}

	/**
	 * Broadcast a message to sessions that have account with the requested
	 * permission.
	 *
	 * @param message
	 * @param permission
	 */
	public static void broadcastMessageWithPermission(String message, int permission) {
		for (WebSocketSession session : WS_SESSIONS.values()) {
			if (session.havePermission(permission)) {
				try {
					session.sendText(message);
				} catch (IOException e) {
					LOGGER.debug("Error broadcasting to '{}' with message : {}", session.getId(), message);
					LOGGER.trace("", e);
				}
			}
		}
	}

	/**
	 * Broadcast a message to sessions that have a specific account.
	 *
	 * @param message
	 * @param id
	 */
	public static void broadcastMessageForUser(String message, int id) {
		for (WebSocketSession session : WS_SESSIONS.values()) {
			if (session.isUserId(id)) {
				try {
					session.sendText(message);
				} catch (IOException e) {
					LOGGER.debug("Error broadcasting to '{}' with message : {}", session.getId(), message);
					LOGGER.trace("", e);
				}
			}
		}
	}

	/**
	 * Broadcast a message to About page sessions
	 *
	 * @param message
	 */
	public static void broadcastAboutMessage(String message) {
		broadcastMessage(ABOUT_SESSIONS, message);
	}

	/**
	 * Broadcast a message to Home page sessions
	 *
	 * @param message
	 */
	public static void broadcastHomeMessage(String message) {
		broadcastMessage(HOME_SESSIONS, message);
	}

	/**
	 * Broadcast a message to Logs page sessions
	 *
	 * @param message
	 */
	public static void broadcastLogsMessage(String message) {
		broadcastMessage(LOGS_SESSIONS, message);
	}

	/**
	 * Broadcast a message to ServerSettings page sessions
	 *
	 * @param message
	 */
	public static void broadcastSettingsMessage(String message) {
		broadcastMessage(SETTINGS_SESSIONS, message);
	}

	/**
	 * Broadcast a message to SharedContent page sessions
	 *
	 * @param message
	 */
	public static void broadcastSharedMessage(String message) {
		broadcastMessage(SHARED_SESSIONS, message);
	}

	public static boolean hasSession() {
		return !WS_SESSIONS.isEmpty();
	}

	public static boolean hasAboutSession() {
		return !ABOUT_SESSIONS.isEmpty();
	}

	public static boolean hasHomeSession() {
		return !HOME_SESSIONS.isEmpty();
	}

	public static boolean hasLogsSession() {
		return !LOGS_SESSIONS.isEmpty();
	}

	public static boolean hasSettingsSession() {
		return !SETTINGS_SESSIONS.isEmpty();
	}

	public static boolean hasSharedSession() {
		return !SHARED_SESSIONS.isEmpty();
	}

	public static void setRefreshSessions(List<Integer> ids) {
		if (hasSession()) {
			for (int id : ids) {
				setRefreshSession(id);
			}
		}
	}

	public static void setRefreshSession(int id) {
		broadcastMessageForUser("{\"action\":\"refresh_session\"}", id);
	}

	public static void setUpdateAccounts() {
		broadcastMessage("{\"action\":\"update_accounts\"}");
	}

	public static void setReloadable(boolean value) {
		broadcastMessageWithPermission("{\"action\":\"set_reloadable\",\"data\":" + (value ? "true" : "false") + "}", Permissions.SETTINGS_VIEW);
	}

	public static void setConfigurationChanged(String key) {
		if (hasSession() && SettingsApiServlet.haveKey(key)) {
			broadcastMessageWithPermission(SettingsApiServlet.getConfigurationUpdate(key), Permissions.SETTINGS_VIEW);
		}
	}

	public static void setMemoryUsage(int maxMemory, int usedMemory, int dbCacheMemory, int bufferMemory) {
		if (hasAboutSession()) {
			String json = "{\"action\":\"update_memory\",\"data\":{\"max\":" + maxMemory + ",\"used\":" + usedMemory + ",\"dbcache\":" + dbCacheMemory + ",\"buffer\":" + bufferMemory + "}}";
			broadcastAboutMessage(json);
		}
	}

	public static void setMediaScanStatus(boolean running) {
		broadcastMessageWithPermission("{\"action\":\"set_media_scan_status\",\"data\":" + (running ? "true" : "false") + "}", Permissions.SETTINGS_VIEW);
	}

	public static void setStatusLine(String line) {
		JsonObject result = new JsonObject();
		result.addProperty(ACTION_STRING, "set_status_line");
		result.addProperty(DATA_STRING, line);
		broadcastMessageWithPermission(result.toString(), Permissions.SETTINGS_VIEW);
	}

	public static void appendLog(String msg) {
		if (hasLogsSession()) {
			JsonObject result = new JsonObject();
			result.addProperty(ACTION_STRING, "log_line");
			result.addProperty(DATA_STRING, msg);
			broadcastLogsMessage(result.toString());
		}
	}

	public static void notifyAll(String msgId, String message, String title, String color, boolean autoClose) {
		if (hasSession()) {
			JsonObject jsonMessage = new JsonObject();
			jsonMessage.addProperty(ACTION_STRING, "notify");
			JsonObject data = new JsonObject();
			if (!StringUtils.isEmpty(msgId) && !StringUtils.isBlank(msgId)) {
				data.addProperty("id", msgId);
			}
			if (!StringUtils.isEmpty(message)) {
				data.addProperty("message", message);
			}
			if (!StringUtils.isEmpty(title)) {
				data.addProperty("title", title);
			}
			if (!StringUtils.isEmpty(color)) {
				data.addProperty("color", color);
			}
			data.addProperty("autoClose", autoClose);
			jsonMessage.add(DATA_STRING, data);
			broadcastMessage(jsonMessage.toString());
		}
	}

	/**
	 * Broadcast a message to sessions in the list.
	 *
	 * @param message
	 */
	private static void broadcastMessage(List<String> list, String message) {
		for (Iterator<String> idIterator = list.iterator(); idIterator.hasNext();) {
			String id = idIterator.next();
			WebSocketSession session = WS_SESSIONS.get(id);
			if (session == null) {
				idIterator.remove();
			} else {
				try {
					session.sendText(message);
				} catch (IOException e) {
					LOGGER.debug("Error broadcasting to '{}' with message : {}", id, message);
					LOGGER.trace("", e);
				}
			}
		}
	}

	private static void unsubscribe(String id) {
		ABOUT_SESSIONS.remove(id);
		ACCOUNT_SESSIONS.remove(id);
		HOME_SESSIONS.remove(id);
		LOGS_SESSIONS.remove(id);
		SETTINGS_SESSIONS.remove(id);
		SHARED_SESSIONS.remove(id);
	}

	private static void subscribeHandler(String id, JsonElement data) {
		unsubscribe(id);
		if (data != null && data.isJsonPrimitive()) {
			String subscribing = data.getAsString();
			switch (subscribing) {
				case "" ->
					LOGGER.debug("unsubscribing for session '{}'", id);
				case "About" ->
					ABOUT_SESSIONS.add(id);
				case "Accounts" ->
					ACCOUNT_SESSIONS.add(id);
				case "Home" ->
					HOME_SESSIONS.add(id);
				case "Logs" ->
					LOGS_SESSIONS.add(id);
				case "ServerSettings" ->
					SETTINGS_SESSIONS.add(id);
				case "SharedContent" ->
					SHARED_SESSIONS.add(id);
				default ->
					LOGGER.debug("Unhandled subscribing '{}' for session '{}'", subscribing, id);
			}
		}
	}

	private static void tokenHandler(String id, JsonElement data) {
		WebSocketSession session = WS_SESSIONS.get(id);
		if (session == null) {
			return;
		}
		String token = null;
		if (data != null && data.isJsonPrimitive()) {
			token = data.getAsString();
		}
		session.setToken(token);
	}

	private static void uuidHandler(String id, JsonElement data) {
		WebSocketSession session = WS_SESSIONS.get(id);
		if (session == null) {
			return;
		}
		String uuid = null;
		if (data != null && data.isJsonPrimitive()) {
			uuid = data.getAsString();
		}
		session.setPlayerUuid(uuid);
	}

	private static JsonObject jsonObjectFromString(String str) {
		JsonObject jObject = null;
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem != null && jElem.isJsonObject()) {
				jObject = jElem.getAsJsonObject();
			}
		} catch (JsonSyntaxException je) {
		}
		return jObject;
	}

}
