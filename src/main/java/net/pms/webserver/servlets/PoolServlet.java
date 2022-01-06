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
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PoolServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(PoolServlet.class);

	public PoolServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			RootFolder root = parent.getRoot(request, response);
			if (root == null) {
				LOGGER.debug("root not found");
				response.sendError(401, "Unknown root");
				return;
			}
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			String json = renderer.getPushData();
			RemoteUtil.respond(response, json, "application/json");
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// This can happen if a browser is left open and our cookie changed, or something like that
			// I'm just leaving this note here as a clue for the next person who encounters this
			LOGGER.error("Unexpected error on web interface. Please try closing any tabs or windows that contain UMS and try again");
			LOGGER.debug("", e);
		}
	}

}
