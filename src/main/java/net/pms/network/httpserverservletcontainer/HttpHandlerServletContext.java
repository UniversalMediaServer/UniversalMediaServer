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
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.SessionTrackingMode;
import jakarta.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

public class HttpHandlerServletContext implements ServletContext {

	private final ClassLoader classLoader;
	private final HttpContext context;
	private final String name;

	public HttpHandlerServletContext(HttpContext context, String name, ClassLoader classLoader) {
		this.context = context;
		this.name = name;
		this.classLoader = classLoader;
	}

	@Override
	public String getContextPath() {
		return context.getPath();
	}

	@Override
	public ServletContext getContext(String uripath) {
		//this is only needed when servlet added to this
		return this;
	}

	@Override
	public int getMajorVersion() {
		return 6;
	}

	@Override
	public int getMinorVersion() {
		return 0;
	}

	@Override
	public int getEffectiveMajorVersion() {
		return 6;
	}

	@Override
	public int getEffectiveMinorVersion() {
		return 0;
	}

	@Override
	public String getMimeType(String file) {
		return file.endsWith(".html") ? "text/html" :
				file.endsWith(".css") ? "text/css" :
				file.endsWith(".js") ? "text/javascript" :
				file.endsWith(".ttf") ? "font/truetype" :
				URLConnection.guessContentTypeFromName(file);
	}

	@Override
	public Set<String> getResourcePaths(String path) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public URL getResource(String path) throws MalformedURLException {
		return classLoader.getResource(path);
	}

	@Override
	public InputStream getResourceAsStream(String path) {
		return classLoader.getResourceAsStream(path);
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String path) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RequestDispatcher getNamedDispatcher(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void log(String msg) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void log(String message, Throwable throwable) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getRealPath(String path) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getServerInfo() {
		return "Java HttpServer Servlet Container/1.0";
	}

	@Override
	public String getInitParameter(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Enumeration<String> getInitParameterNames() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean setInitParameter(String name, String value) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Object getAttribute(String name) {
		return context.getAttributes().get(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(context.getAttributes().keySet());
	}

	@Override
	public void setAttribute(String name, Object object) {
		context.getAttributes().put(name, object);
	}

	@Override
	public void removeAttribute(String name) {
		context.getAttributes().remove(name);
	}

	@Override
	public String getServletContextName() {
		return name;
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, String className) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ServletRegistration.Dynamic addJspFile(String servletName, String jspFile) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ServletRegistration getServletRegistration(String servletName) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Map<String, ? extends ServletRegistration> getServletRegistrations() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, String className) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public FilterRegistration getFilterRegistration(String filterName) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public SessionCookieConfig getSessionCookieConfig() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void addListener(String className) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T extends EventListener> void addListener(T t) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void addListener(Class<? extends EventListener> listenerClass) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
			throw new ServletException(e);
		}
	}

	@Override
	public JspConfigDescriptor getJspConfigDescriptor() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public void declareRoles(String... roleNames) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getVirtualServerName() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getSessionTimeout() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setSessionTimeout(int sessionTimeout) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getRequestCharacterEncoding() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setRequestCharacterEncoding(String encoding) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getResponseCharacterEncoding() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void setResponseCharacterEncoding(String encoding) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
