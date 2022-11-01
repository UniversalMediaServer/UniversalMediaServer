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
package net.pms.network.httpserverservletcontainer;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class HttpHandlerServlet implements HttpHandler {

	private final HttpServlet servlet;

	public HttpHandlerServlet(HttpServlet servlet) {
		this.servlet = servlet;
	}

	@Override
	public void handle(final HttpExchange exchange) throws IOException {
		HttpExchangeServletRequest req = new HttpExchangeServletRequest(servlet, exchange);
		try {
			servlet.service(req, req.getServletResponse());
		} catch (ServletException e) {
			throw new IOException(e);
		}
	}
}
