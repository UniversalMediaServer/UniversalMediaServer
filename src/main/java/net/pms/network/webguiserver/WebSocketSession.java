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
import java.net.InetSocketAddress;
import net.pms.iam.Account;
import net.pms.iam.AuthService;

/**
 * @author Surf@ceS
 */
public class WebSocketSession {

	private final Session session;
	private final String remoteAddress;
	private final boolean isLocalhost;
	private Account account;

	public WebSocketSession(Session session) {
		this.session = session;
		if (session.getUserProperties().containsKey("jakarta.websocket.endpoint.remoteAddress")) {
			InetSocketAddress address = (InetSocketAddress) session.getUserProperties().get("jakarta.websocket.endpoint.remoteAddress");
			this.remoteAddress = address.getHostString();
			this.isLocalhost = address.getAddress().isAnyLocalAddress() || address.getAddress().isLoopbackAddress();
		} else {
			this.remoteAddress = "";
			this.isLocalhost = false;
		}
	}

	public String getId() {
		return session.getId();
	}

	public boolean isUserId(int id) {
		return account != null && account.getUser().getId() == id;
	}

	public boolean havePermission(int permission) {
		return account != null && account.havePermission(permission);
	}

	public void sendText(String text) throws IOException {
		this.session.getBasicRemote().sendText(text);
	}

	public void setToken(String token) {
		account = AuthService.getAccountLoggedIn(token, remoteAddress, isLocalhost);
	}

	public void setPlayerUuid(String uuid) {
		//to implement
	}

}
