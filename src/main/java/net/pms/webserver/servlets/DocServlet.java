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
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.newgui.DbgPacker;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(DocServlet.class);

	public DocServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			InetAddress inetAddress = InetAddress.getByName(request.getRemoteAddr());
			URI uri = URI.create(request.getRequestURI());
			LOGGER.debug("root req " + uri);
			if (RemoteUtil.deny(inetAddress)) {
				throw new IOException("Access denied");
			}
			if (uri.getPath().contains("favicon")) {
				RemoteUtil.sendLogo(response);
				return;
			}

			HashMap<String, Object> vars = new HashMap<>();
			vars.put("logs", getLogs(true));
			if (CONFIGURATION.getUseCache()) {
				vars.put("cache",
					"http://" + PMS.get().getServer().getHost() + ":" + PMS.get().getServer().getPort() + "/console/home");
			}

			String responseString = parent.getResources().getTemplate("doc.html").execute(vars);
			RemoteUtil.respondHtml(response, responseString);
		} catch (IOException e) {
			throw e;
		} catch (MustacheException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in DocServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	private ArrayList<HashMap<String, String>> getLogs(boolean asList) {
		Set<File> files = new DbgPacker().getItems();
		if (!asList) {
			return null;
		}
		ArrayList<HashMap<String, String>> logs = new ArrayList<>();
		for (File f : files) {
			if (f.exists()) {
				String id = String.valueOf(parent.getResources().add(f));
				if (asList) {
					HashMap<String, String> item = new HashMap<>();
					item.put("filename", f.getName());
					item.put("id", id);
					logs.add(item);
				}
			}
		}
		return logs;
	}

}
