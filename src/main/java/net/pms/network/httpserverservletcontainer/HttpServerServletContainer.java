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

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A small servlet container interface for com.sun.net.httpserver.HttpServer
 * Implement only members that we needs
 */
public class HttpServerServletContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerServletContainer.class);
	private final URLClassLoader classLoader;
	private final HttpServer server;

	public HttpServerServletContainer(HttpServer server, String... urls) {
		this.server = server;
		ArrayList<URL> cleanedUrls = new ArrayList<>();
		try {
			for (String url : urls) {
				URL currentUrl = new URL(url);
				cleanedUrls.add(currentUrl);
			}
		} catch (MalformedURLException e) {
			LOGGER.debug("Error adding resource url: " + e);
		}
		classLoader = new URLClassLoader(cleanedUrls.toArray(URL[]::new), null);
	}

	public <T extends HttpServlet> void createServlet(Class<T> clazz) throws ServletException {
		WebServlet webServlet = clazz.getAnnotation(WebServlet.class);
		String[] urlPatterns = webServlet.urlPatterns();
		if (urlPatterns.length == 0) {
			urlPatterns = webServlet.value();
		}
		if (urlPatterns.length > 0) {
			try {
				Constructor<?> cons = clazz.getConstructor();
				for (String urlPattern : urlPatterns) {
					HttpServlet servlet = (HttpServlet) cons.newInstance();
					HttpContext httpContext = server.createContext(urlPattern);
					HttpHandlerServletConfig config = new HttpHandlerServletConfig(servlet, httpContext, classLoader);
					servlet.init(config);
					HttpHandlerServlet handler = new HttpHandlerServlet(servlet);
					httpContext.setHandler(handler);
				}
			} catch (NoSuchMethodException | InstantiationException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				throw new ServletException("Servlet failed to instantiate.", ex);
			}
		} else {
			throw new ServletException("Servlet does not include any pattern.");
		}
	}

}
