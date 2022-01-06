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
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayListServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayListServlet.class);
	private static final String RETURN_PAGE = "<html><head><script>window.refresh=true;history.back()</script></head></html>";

	public PlayListServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			URI uri = URI.create(request.getRequestURI());
			String p = uri.getPath();
			String[] tmp = p.split("/");
			if (tmp.length < 3) {
				response.sendError(400, "Bad request");
				return;
			}
			String op = tmp[tmp.length - 2];
			String id = tmp[tmp.length - 1];
			DLNAResource r = PMS.getGlobalRepo().get(id);
			if (r != null) {
				RootFolder root = parent.getRoot(request, response);
				if (root == null) {
					LOGGER.debug("root not found");
					response.sendError(401, "Unknown root");
					return;
				}
				WebRender renderer = (WebRender) root.getDefaultRenderer();
				if (op.equals("add")) {
					PMS.get().getDynamicPls().add(r);
					synchronized (renderer) {
						renderer.notify(RendererConfiguration.OK, "Added '" + r.getDisplayName() + "' to dynamic playlist");
					}
				} else if (op.equals("del") && (r.getParent() instanceof Playlist)) {
					((Playlist) r.getParent()).remove(r);
					synchronized (renderer) {
						renderer.notify(RendererConfiguration.INFO, "Removed '" + r.getDisplayName() + "' from playlist");
					}
				}
			}
			RemoteUtil.respondHtml(response, RETURN_PAGE);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in PlayListServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
