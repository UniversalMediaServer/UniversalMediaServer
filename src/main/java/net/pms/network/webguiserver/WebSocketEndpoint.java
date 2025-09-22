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

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
@ServerEndpoint("/v1/api/ws")
public class WebSocketEndpoint {

	private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEndpoint.class);

	@OnOpen
	public void onOpen(Session session) {
		LOGGER.debug("WebSocket connection opened: " + session.getId());
		WebSocketDispatcher.add(session);
	}

	@OnMessage
	public void onMessage(Session session, String message) {
		if ("ping".equals(message)) {
			try {
				session.getBasicRemote().sendText("pong");
			} catch (IOException e) {
				LOGGER.debug("Error sending message: " + e.getMessage());
			}
		} else {
			WebSocketDispatcher.onMessage(session, message);
		}
	}

	@OnClose
	public void onClose(Session session) {
		LOGGER.debug("WebSocket connection closed: " + session.getId());
		WebSocketDispatcher.remove(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		LOGGER.debug("WebSocket error: {}", throwable.getMessage());
		LOGGER.trace("Error details: {}", throwable);
	}

}
