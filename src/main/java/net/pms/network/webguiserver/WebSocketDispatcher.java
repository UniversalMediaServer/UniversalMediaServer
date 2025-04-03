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

import jakarta.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class WebSocketDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketDispatcher.class);
	private static final List<Session> WS_SESSIONS = new ArrayList<>();

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebSocketDispatcher() {
	}

	public static synchronized void add(Session session) {
		WS_SESSIONS.add(session);
	}

	public static synchronized void remove(Session session) {
		WS_SESSIONS.add(session);
	}

	public static void onMessage(Session session, String message) {
		//filter non json messages and handle client request when we will implement
	}

	public static void broadcastMessage(String message) {
		synchronized (WS_SESSIONS) {
			for (Session session : WS_SESSIONS) {
				try {
					session.getBasicRemote().sendText(message);
				} catch (IOException e) {
					LOGGER.debug("Error broadcasting message: " + e.getMessage());
				}
			}
		}
	}

}
