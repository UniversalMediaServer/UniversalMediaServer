/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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
package net.pms.webserver.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.webserver.WebServerServlets;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayersStatusServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayersStatusServlet.class);

	public PlayersStatusServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			String json = IOUtils.toString(request.getReader());
			LOGGER.trace("got player status: " + json);
			RootFolder root = parent.getRoot(request, response);
			if (root == null) {
				LOGGER.debug("root not found");
				response.sendError(401, "Unknown root");
				return;
			}
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			((WebRender.WebPlayer) renderer.getPlayer()).setData(json);
			response.setContentType("text/html");
			response.setStatus(200);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in PlayersStatusServlet.doPost(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
