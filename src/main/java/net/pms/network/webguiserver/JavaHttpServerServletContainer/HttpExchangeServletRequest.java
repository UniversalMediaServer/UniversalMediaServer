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
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.HttpUtils;
import javax.servlet.http.Part;
import net.pms.PMS;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class HttpExchangeServletRequest implements HttpServletRequest {

	private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	private final HttpExchange exchange;
	private final HttpServlet servlet;

	private final Map<String, String[]> parameters = new HashMap<>();
	private final List<String> attributesName = new ArrayList<>();
	private String contentType;
	private String characterEncoding;
	private ServletInputStream servletInputStream;
	private Cookie[] cookies;
	private List<Locale> locales;

	public HttpExchangeServletRequest(HttpServlet servlet, HttpExchange exchange) {
		this.servlet = servlet;
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
	public void removeAttribute(String string) {
		attributesName.remove(string);
		exchange.setAttribute(string, null);
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
		String servpath = getServletContext().getContext(getRequestURI()).getContextPath();
		String path = exchange.getRequestURI().getPath();
		return path.replace(servpath, "");
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
		return servlet.getServletContext().getContextPath();
	}

	@Override
	public String getQueryString() {
		return exchange.getRequestURI().getQuery();
	}

	@Override
	public String getRequestURI() {
		return exchange.getRequestURI().getPath();
	}

	@Override
	public StringBuffer getRequestURL() {
		String url = exchange.getRequestURI().toString();
		if (exchange.getRequestURI().getQuery() != null) {
			url = url.substring(0, url.lastIndexOf("?"));
		}
		return new StringBuffer(url);
	}

	@Override
	public String getServletPath() {
		return getServletContext().getContext(getRequestURI()).getContextPath();
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
	public String getRemoteUser() {
		return exchange.getPrincipal().getUsername();
	}

	@Override
	public String getServerName() {
		String hostHeader = getHeader("Host");
		if (!StringUtils.isEmpty(hostHeader)) {
			String[] hostSplit = hostHeader.split(":");
			return hostSplit[0];
		}
		return exchange.getRequestURI().getHost();
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
	public String getAuthType() {
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
	public Locale getLocale() {
		if (locales == null) {
			parseLocales();
		}
		return locales.get(0);
	}

	@Override
	public Enumeration<Locale> getLocales() {
		if (locales == null) {
			parseLocales();
		}
		return Collections.enumeration(locales);
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
	@Deprecated
	public String getRealPath(String string) {
		return getServletContext().getRealPath(string);
	}

	@Override
	public int getRemotePort() {
		return exchange.getRemoteAddress().getPort();
	}

	@Override
	public String getLocalName() {
		return exchange.getRequestURI().getHost();
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
	public ServletContext getServletContext() {
		return servlet.getServletConfig().getServletContext();
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

	public void parseLocales() {
		String languagesHeader = getHeader("Accept-language");
		if (StringUtils.isEmpty(languagesHeader)) {
			locales = new ArrayList<>();
			locales.add(PMS.getLocale());
			return;
		}
		HashMap<Locale, Float> result = new HashMap<>();
		String[] languages = languagesHeader.split(",");
		for (String language : languages) {
			String[] split = language.split(";");
			Locale locale = Locale.forLanguageTag(split[0]);
			float factor = 1;
			if (split.length > 1 && split[1].contains("q=")) {
				try {
					factor = Float.parseFloat(split[1].substring(2));
				} catch (NumberFormatException e) {
				}
			}
			result.put(locale, factor);
		}
		Comparator<Entry<Locale, Float>> valueComparator = (Entry<Locale, Float> e1, Entry<Locale, Float> e2) -> {
			return e2.getValue().compareTo(e1.getValue());
		};
		List<Entry<Locale, Float>> listOfEntries = new ArrayList<>(result.entrySet());
		Collections.sort(listOfEntries, valueComparator);
		locales = new ArrayList<>();
		locales.add(PMS.getLocale());
		for (Entry<Locale, Float> entry : listOfEntries) {
			locales.add(entry.getKey());
		}
	}
}
