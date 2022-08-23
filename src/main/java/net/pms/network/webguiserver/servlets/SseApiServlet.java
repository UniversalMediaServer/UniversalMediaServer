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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.iam.Account;
import net.pms.iam.AccountService;
import net.pms.iam.AuthService;
import net.pms.network.webguiserver.WebGuiServletHelper;
import net.pms.network.webguiserver.ServerSentEvents;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebServlet(name = "SseApiServlet", urlPatterns = {"/v1/api/sse"}, displayName = "Sse Api Servlet")
public class SseApiServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(SseApiServlet.class);
	private static final Map<Integer, ArrayList<ServerSentEvents>> SSE_INSTANCES = new HashMap<>();

	public static final String BASE_PATH = "/v1/api/sse";

	private static final Thread UPDATE_MEMORY_USAGE_THREAD = new Thread(() -> {
		while (true) {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				return;
			}
			updateMemoryUsage();
		}
	}, "SSE Api Memory Usage Updater");

	public SseApiServlet() {
		startMemoryThread();
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		if (WebGuiServletHelper.deny(req)) {
			throw new IOException("Access denied");
		}
		if (LOGGER.isTraceEnabled()) {
			WebGuiServletHelper.logHttpServletRequest(req, "");
		}
		super.service(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		try {
			var path = req.getPathInfo();
			if (path.equals("/")) {
				Account account = AuthService.getAccountLoggedIn(req);
				if (account != null && account.getUser().getId() > 0) {
					resp.setHeader("Server", PMS.get().getServerName());
					resp.setHeader("Connection", "keep-alive");
					resp.setHeader("Cache-Control", "no-transform");
					resp.setHeader("Charset", "UTF-8");
					resp.setContentType("text/event-stream");
					resp.setContentLength(0);
					ServerSentEvents sse = new ServerSentEvents(resp.getOutputStream());
					addServerSentEventsFor(account.getUser().getId(), sse);
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

	//let start a thread to update memory usage
	private static void startMemoryThread() {
		if (!UPDATE_MEMORY_USAGE_THREAD.isAlive()) {
			UPDATE_MEMORY_USAGE_THREAD.start();
		}
	}

	private static void updateMemoryUsage() {
		if (hasServerSentEvents()) {
			final long max = Runtime.getRuntime().maxMemory() / 1048576;
			final long used = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
			long buffer = 0;
			List<RendererConfiguration> foundRenderers = PMS.get().getFoundRenderers();
			synchronized (foundRenderers) {
				for (RendererConfiguration r : PMS.get().getFoundRenderers()) {
					buffer += (r.getBuffer());
				}
			}
			String json = "{\"action\":\"update_memory\",\"max\":" + (int) max + ",\"used\":" + (int) used + ",\"buffer\":" + (int) buffer + "}";
			broadcastMessage(json);
		}
	}

	private static void addServerSentEventsFor(int id, ServerSentEvents sse) {
		if (id > 0) {
			synchronized (SSE_INSTANCES) {
				if (!SSE_INSTANCES.containsKey(id)) {
					SSE_INSTANCES.put(id, new ArrayList<>());
				}
				SSE_INSTANCES.get(id).add(sse);
			}
		}
	}

	public static boolean hasServerSentEvents() {
		synchronized (SSE_INSTANCES) {
			return !SSE_INSTANCES.isEmpty();
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
		if (ConfigurationApiServlet.haveKey(key)) { //hasServerSentEvents() &&
			broadcastMessage(ConfigurationApiServlet.getConfigurationUpdate(key), "settings_view");
		}
	}
}
