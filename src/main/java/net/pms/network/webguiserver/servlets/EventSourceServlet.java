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
package net.pms.network.webguiserver.servlets;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import net.pms.iam.Account;
import net.pms.iam.AuthService;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.EventSourceClient;
import net.pms.network.webguiserver.EventSourceServer;
import net.pms.network.webguiserver.GuiHttpServlet;

@WebServlet(name = "EventSourceServlet", urlPatterns = {"/v1/api/sse"}, displayName = "Sse Api Servlet")
public class EventSourceServlet extends GuiHttpServlet {

	private int heartBeatPeriod = EventSourceClient.DEFAULT_HEART_BEAT_PERIOD;

	@Override
	public void init() throws ServletException {
		String heartBeatPeriodParam = getServletConfig().getInitParameter("heartBeatPeriod");
		if (heartBeatPeriodParam != null) {
			heartBeatPeriod = Integer.parseInt(heartBeatPeriodParam);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if (hasHeaderValue(request, "Accept", "text/event-stream")) {
			Account account = AuthService.getAccountLoggedIn(request);
			if (account != null && account.getUser().getId() > 0) {
				String sseType = null;
				URI referer = getRequestReferer(request);
				if (referer != null) {
					sseType = referer.getPath();
				}
				response.setHeader("Server", MediaServer.getServerName());
				response.setHeader("Connection", "close");
				response.setHeader("Cache-Control", "no-transform");
				response.setHeader("Charset", "UTF-8");
				response.setContentType("text/event-stream");
				response.setStatus(HttpServletResponse.SC_OK);
				response.flushBuffer();
				AsyncContext async = request.startAsync();
				async.setTimeout(0);
				EventSourceClient client = new EventSourceClient(async, heartBeatPeriod);
				EventSourceServer.addServerSentEventsFor(account.getUser().getId(), client, sseType);
			} else {
				respondForbidden(request, response);
			}
		} else {
			respondBadRequest(request, response);
		}
	}

}
