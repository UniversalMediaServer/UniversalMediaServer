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

import com.samskivert.mustache.MustacheException;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebServerServlet extends HttpServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebServerServlet.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	protected final WebServerServlets parent;

	public WebServerServlet(final WebServerServlets parent) {
		this.parent = parent;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		URI uri = URI.create(request.getRequestURI());
		if (uri.getPath().contains("favicon")) {
			RemoteUtil.sendLogo(response);
			return;
		}
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("serverName", CONFIGURATION.getServerDisplayName());
		try {
			Template template = parent.getResources().getTemplate("start.html");
			if (template != null) {
				String responseString = template.execute(vars);
				RemoteUtil.respond(response, responseString, 200, "text/html");
			} else {
				response.sendError(404, "Web template \"start.html\" not found");
			}
		} catch (MustacheException e) {
			LOGGER.error("An error occurred while generating a HTTP response: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			InetAddress inetAddress = InetAddress.getByName(request.getRemoteAddr());
			if (RemoteUtil.deny(inetAddress)) {
				LOGGER.debug("Web Server: Access denied to {}", inetAddress);
				response.sendError(403, "Access denied");
				return;
			}
			super.service(request, response);
		} catch (IOException e) {
			throw e;
		}
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		//by default same as doGet
		doGet(request, response);
	}
}
