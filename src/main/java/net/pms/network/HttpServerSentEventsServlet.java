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
package net.pms.network;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServerSentEventsServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerSentEventsServlet.class);
	private static final String SSE_CONTENT_TYPE = "text/event-stream";
	private static final String SSE_ENCODING = "UTF-8";
	private final List<AsyncContext> clients = new CopyOnWriteArrayList<>();

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		AsyncContext asyncContext = request.startAsync(request, response);
		asyncContext.setTimeout(-1);
		asyncContext.getResponse().setContentType(SSE_CONTENT_TYPE);
		asyncContext.getResponse().setCharacterEncoding(SSE_ENCODING);
		asyncContext.getResponse().flushBuffer();
		clients.add(asyncContext);
	}

	public void broadcast(String event, String data) {
		broadcast(getMessage(event, data, null, null));
	}

	public void broadcast(String message) {
		for (AsyncContext client : clients) {
			try {
				send(client, message);
			} catch (IOException e) {
				// This client seems disconnected.
				clients.remove(client);
			}
		}
	}

	/**
	 * Send an SSE message
	 * @param asyncContext the client AsyncContext
	 * @param message the SSE AsyncContext
	 * @throws IOException
	 */
	public static void send(final AsyncContext asyncContext, String message) throws IOException {
		final HttpServletResponse res = (HttpServletResponse) asyncContext.getResponse();
		final ServletOutputStream outputStream = res.getOutputStream();
		outputStream.print(message);
		outputStream.flush();
	}

	/***
	 * Get a well formated SSE message
	 * @param event	A string identifying the type of event described.
	 * @param data The data field for the message.
	 * @param id The event ID to set the EventSource object's last event ID value.
	 * @param retry The reconnection time.
	 * @return Stringified message
	*/
	public static String getMessage(String event, String data, Integer retry, String id) {
		StringBuilder sb = new StringBuilder();
		if (event != null) {
			sb.append("event: ").append(event.replace("\n", "")).append('\n');
		}
		if (data != null) {
			for (String s : data.split("\n")) {
				sb.append("data: ").append(s).append('\n');
			}
		}
		if (retry != null) {
			sb.append("retry: ").append(retry).append('\n');
		}
		if (id != null) {
			sb.append("id: ").append(id.replace("\n", "")).append('\n');
		}
		sb.append('\n');
		return sb.toString();
	}

}
