/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.network.webguiserver.JavaHttpServerServletContainer;

import com.sun.net.httpserver.HttpExchange;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpCookie;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.HttpUtils;
import javax.servlet.http.Part;
import org.apache.commons.io.IOUtils;

public class HttpExchangeServletRequest implements HttpServletRequest {

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	private final HttpExchange exchange;

	private final Map<String, String[]> parameters = new HashMap<>();
	private final List<String> attributesName = new ArrayList<>();
	private String contentType;
	private String characterEncoding;
	private ServletInputStream servletInputStream;
	private Cookie[] cookies;

	public HttpExchangeServletRequest(HttpExchange exchange) {
		this.exchange = exchange;
		parseContentType();
		parseCookies();
		parseQueryParameters();
		if (contentType != null && contentType.equals("application/x-www-form-urlencoded")) {
			parsePostParameters();
		}
	}

	@Override
	public String getHeader(String name) {
		return exchange.getRequestHeaders().getFirst(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		return Collections.enumeration(exchange.getRequestHeaders().get(name));
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(exchange.getRequestHeaders().keySet());
	}

	@Override
	public Object getAttribute(String name) {
		return exchange.getAttribute(name);
	}

	@Override
	public void setAttribute(String name, Object o) {
		if (!attributesName.contains(name)) {
			attributesName.add(name);
		}
		exchange.setAttribute(name, o);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributesName);
	}

	@Override
	public String getMethod() {
		return exchange.getRequestMethod();
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (servletInputStream == null) {
			servletInputStream = new HttpExchangeServletInputStream(exchange.getRequestBody());
		}
		return servletInputStream;
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}

	@Override
	public String getPathInfo() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
		//return exchange.getRequestURI().getPath();
	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name) != null && parameters.get(name).length > 0 ? parameters.get(name)[0] : null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		return parameters;
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(parameters.keySet());
	}

	@Override
	public String getAuthType() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Cookie[] getCookies() {
		return cookies;
	}

	@Override
	public long getDateHeader(String string) {
		String dateStr = getHeader(string);
		if (dateStr != null) {
			try {
				return DATE_FORMAT.parse(dateStr).getTime();
			} catch (ParseException e) {
				throw new IllegalArgumentException();
			}
		}
		return -1L;
	}

	@Override
	public int getIntHeader(String string) {
		if (exchange.getRequestHeaders().containsKey(string)) {
			return Integer.parseInt(exchange.getRequestHeaders().getFirst(string));
		}
		return -1;
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getContextPath() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getQueryString() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getRemoteUser() {
		return exchange.getPrincipal().getUsername();
	}

	@Override
	public boolean isUserInRole(String string) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Principal getUserPrincipal() {
		return exchange.getPrincipal();
	}

	@Override
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getRequestURI() {
		return exchange.getRequestURI().getPath();
	}

	@Override
	public StringBuffer getRequestURL() {

		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getServletPath() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public HttpSession getSession(boolean bln) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isRequestedSessionIdFromUrl() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public void login(String string, String string1) throws ServletException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Part getPart(String string) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> type) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public void setCharacterEncoding(String string) throws UnsupportedEncodingException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public int getContentLength() {
		String cLength = getHeader("Content-Length");
		if (cLength != null) {
			try {
				return Integer.parseInt(cLength);
			} catch (NumberFormatException e) {
			}
		}
		return -1;
	}

	@Override
	public long getContentLengthLong() {
		String cLength = getHeader("Content-Length");
		if (cLength != null) {
			try {
				return Long.parseLong(cLength);
			} catch (NumberFormatException e) {
			}
		}
		return -1L;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public String[] getParameterValues(String string) {
		return parameters.get(string);
	}

	@Override
	public String getProtocol() {
		return exchange.getProtocol();
	}

	@Override
	public String getScheme() {
		return exchange.getRequestURI().getScheme();
	}

	@Override
	public String getServerName() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public int getServerPort() {
		return exchange.getLocalAddress().getPort();
	}

	@Override
	public String getRemoteAddr() {
		return exchange.getRemoteAddress().getAddress().getHostAddress();
	}

	@Override
	public String getRemoteHost() {
		return exchange.getRemoteAddress().getHostString();
	}

	@Override
	public void removeAttribute(String string) {
		attributesName.remove(string);
		exchange.setAttribute(string, null);
	}

	@Override
	public Locale getLocale() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public Enumeration<Locale> getLocales() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isSecure() {
		return exchange.getRequestURI().getScheme().equals("https");
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String string) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getRealPath(String string) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public int getRemotePort() {
		return exchange.getRemoteAddress().getPort();
	}

	@Override
	public String getLocalName() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String getLocalAddr() {
		return exchange.getLocalAddress().getAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		return exchange.getLocalAddress().getPort();
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public AsyncContext startAsync(ServletRequest sr, ServletResponse sr1) throws IllegalStateException {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isAsyncStarted() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isAsyncSupported() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public AsyncContext getAsyncContext() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public DispatcherType getDispatcherType() {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	private void parseQueryParameters() {
		try {
			parameters.putAll(HttpUtils.parseQueryString(exchange.getRequestURI().getQuery()));
		} catch (IllegalArgumentException e) {
		}
	}

	private void parsePostParameters() {
		try {
			String postData = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
			parameters.putAll(HttpUtils.parseQueryString(postData));
		} catch (IOException | IllegalArgumentException e) {
		}
	}

	private void parseCookies() {
		Enumeration<String> cookiesStr = getHeaders("Cookie");
		while (cookiesStr.hasMoreElements()) {
			String cookieStr = cookiesStr.nextElement();
			HttpCookie.parse(cookieStr);

		}
	}

	private void parseContentType() {
		String cType = getHeader("Content-Type");
		if (cType != null) {
			cType = cType.toLowerCase();
			if (cType.contains(";")) {
				String[] cTypeParts = cType.split(";");
				for (String cTypePart : cTypeParts) {
					if (cTypePart.trim().startsWith("charset") && cTypePart.contains("=")) {
						cTypePart = cTypePart.substring(cTypePart.indexOf("=") + 1);
						characterEncoding = cTypePart.trim();
					} else {
						contentType = cTypePart.trim();
					}
				}
			} else if (cType.startsWith("charset") && cType.contains("=")) {
				cType = cType.substring(cType.indexOf("=") + 1);
				characterEncoding = cType.trim();
			} else {
				contentType = cType.trim();
			}
		}
	}
}
