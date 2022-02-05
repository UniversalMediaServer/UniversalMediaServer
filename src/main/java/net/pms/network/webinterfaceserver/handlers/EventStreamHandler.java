/*
 * Universal Media Server, for streaming any media to DLNA
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
package net.pms.network.webinterfaceserver.handlers;

import net.pms.network.webinterfaceserver.ServerSentEvents;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import net.pms.PMS;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventStreamHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(EventStreamHandler.class);

	private final WebInterfaceServerHttpServer parent;

	public EventStreamHandler(WebInterfaceServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			RootFolder root = parent.getRoot(WebInterfaceServerUtil.userName(t), t);
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			Headers hdr = t.getResponseHeaders();
			hdr.add("Server", PMS.get().getServerName());
			hdr.add("Content-Type", "text/event-stream");
			hdr.add("Connection", "keep-alive");
			hdr.add("Charset", "UTF-8");
			t.sendResponseHeaders(200, 0);
			ServerSentEvents sse = new ServerSentEvents(t.getResponseBody(), WebInterfaceServerUtil.getFirstSupportedLanguage(t));
			renderer.addServerSentEvents(sse);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// This can happen if a browser is left open and our cookie changed, or something like that
			// I'm just leaving this note here as a clue for the next person who encounters this
			LOGGER.error("Unexpected error on web player server. Please try closing any tabs or windows that contain UMS and try again");
			LOGGER.debug("", e);
		}
	}
}
