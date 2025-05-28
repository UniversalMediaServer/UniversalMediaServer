/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.network.webguiserver.servlets;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.server.ServerContainer;
import net.pms.network.webguiserver.GuiHttpServlet;
import net.pms.network.webguiserver.WebSocketEndpoint;

@WebServlet(name = "WebSocketServlet", urlPatterns = {"/v1/api/ws"}, displayName = "WebSocket Api Servlet")
public class WebSocketServlet extends GuiHttpServlet {

	@Override
	public void init() throws ServletException {
		try {
			ServerContainer container = (ServerContainer) getServletContext().getAttribute(ServerContainer.class.getName());
			container.setDefaultMaxTextMessageBufferSize(128 * 1024);
			container.addEndpoint(WebSocketEndpoint.class);
		} catch (DeploymentException e) {
			throw new ServletException(e);
		}
	}

}
