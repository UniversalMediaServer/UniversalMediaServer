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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebInitParam;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import org.apache.commons.lang3.StringUtils;

public class HttpHandlerServletConfig implements ServletConfig {

	private final HttpHandlerServletContext context;
	private final String servletName;
	private final HashMap<String, String> initParameters;

	public HttpHandlerServletConfig(HttpServlet servlet, HttpContext httpContext, ClassLoader classLoader) {
		WebServlet webServlet = servlet.getClass().getAnnotation(WebServlet.class);
		if (StringUtils.isBlank(webServlet.name())) {
			servletName = servlet.getClass().getSimpleName();
		} else {
			servletName = webServlet.name();
		}
		initParameters = new HashMap<>();
		for (WebInitParam initParam : webServlet.initParams()) {
			initParameters.put(initParam.name(), initParam.value());
		}
		context = new HttpHandlerServletContext(httpContext, servletName, classLoader);
	}

	@Override
	public String getServletName() {
		return servletName;
	}

	@Override
	public ServletContext getServletContext() {
		return context;
	}

	@Override
	public String getInitParameter(String name) {
		return initParameters.get(name);
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		return Collections.enumeration(initParameters.keySet());
	}

}
