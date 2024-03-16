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
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletConnection;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.MappingMatch;
import jakarta.servlet.http.Part;
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
import java.util.StringTokenizer;
import net.pms.PMS;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class HttpExchangeServletRequest implements HttpServletRequest {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	private final HttpExchange exchange;
	private final HttpServlet servlet;

	private final Map<String, String[]> parameters = new HashMap<>();
	private final List<String> attributesName = new ArrayList<>();
	private final HttpExchangeServletResponse servletResponse;

	private String contentType;
	private String characterEncoding;
	private ServletInputStream servletInputStream;
	private Cookie[] cookies;
	private List<Locale> locales;
	private String pathInfo;
	private String servletPath;
	private HttpExchangeAsyncContext asyncContext;

	public HttpExchangeServletRequest(HttpServlet servlet, HttpExchange exchange) {
		this.servlet = servlet;
		this.exchange = exchange;
		parseContentType();
		parseQueryParameters();
		if (contentType != null && contentType.equals("application/x-www-form-urlencoded")) {
			parsePostParameters();
		}
		servletResponse = new HttpExchangeServletResponse(exchange);
	}

	@Override
	public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String changeSessionId() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public AsyncContext getAsyncContext() {
		if (isAsyncStarted()) {
			return asyncContext;
		}
		throw new IllegalStateException("This request has not been put into asynchronous mode.");
	}

	@Override
	public Object getAttribute(String name) {
		return exchange.getAttribute(name);
	}

	@Override
	public Enumeration<String> getAttributeNames() {
		return Collections.enumeration(attributesName);
	}

	@Override
	public String getAuthType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
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
	public String getContextPath() {
		return servlet.getServletContext().getContextPath();
	}

	@Override
	public Cookie[] getCookies() {
		if (cookies == null) {
			parseCookies();
		}
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
	public DispatcherType getDispatcherType() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getHeader(String name) {
		return exchange.getRequestHeaders().getFirst(name);
	}

	@Override
	public Enumeration<String> getHeaders(String name) {
		List<String> headers = exchange.getRequestHeaders().get(name);
		if (headers != null) {
			return Collections.enumeration(headers);
		}
		return Collections.enumeration(new ArrayList<>());
	}

	@Override
	public Enumeration<String> getHeaderNames() {
		return Collections.enumeration(exchange.getRequestHeaders().keySet());
	}

	@Override
	public HttpServletMapping getHttpServletMapping() {
		return new HttpServletMapping() {
			@Override
			public String getMatchValue() {
				return "";
			}

			@Override
			public String getPattern() {
				return "";
			}

			@Override
			public String getServletName() {
				return servlet.getServletName();
			}

			@Override
			public MappingMatch getMappingMatch() {
				return null;
			}

			@Override
			public String toString() {
				return "MappingImpl{" + "matchValue=" + getMatchValue() + ", pattern=" + getPattern() + ", servletName=" +
						getServletName() + ", mappingMatch=" + getMappingMatch() + "} HttpServletRequest {" +
						HttpExchangeServletRequest.this.toString() + '}';
			}

		};
	}

	@Override
	public ServletInputStream getInputStream() throws IOException {
		if (servletInputStream == null) {
			servletInputStream = new HttpExchangeServletInputStream(exchange.getRequestBody());
		}
		return servletInputStream;
	}

	@Override
	public int getIntHeader(String string) {
		if (exchange.getRequestHeaders().containsKey(string)) {
			return Integer.parseInt(exchange.getRequestHeaders().getFirst(string));
		}
		return -1;
	}

	@Override
	public String getLocalAddr() {
		return exchange.getLocalAddress().getAddress().getHostAddress();
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
	public String getLocalName() {
		return exchange.getRequestURI().getHost();
	}

	@Override
	public int getLocalPort() {
		return exchange.getLocalAddress().getPort();
	}

	@Override
	public String getMethod() {
		return exchange.getRequestMethod();
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
	public String[] getParameterValues(String string) {
		return parameters.get(string);
	}

	@Override
	public Part getPart(String string) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<Part> getParts() throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getPathInfo() {
		if (pathInfo == null) {
			String servpath = getServletContext().getContextPath();
			String path = exchange.getRequestURI().getPath();
			if ("/".equals(servpath)) {
				pathInfo = path;
			} else {
				String pInfo = path.replaceFirst(servpath, "");
				if (!pInfo.isEmpty() && !pInfo.startsWith("/")) {
					pInfo = "/" + pInfo;
				}
				pathInfo = pInfo;
			}
		}
		return pathInfo.isEmpty() ? null : pathInfo;
	}

	@Override
	public String getPathTranslated() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getProtocol() {
		return exchange.getProtocol();
	}

	@Override
	public String getProtocolRequestId() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getQueryString() {
		return exchange.getRequestURI().getQuery();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
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
	public int getRemotePort() {
		return exchange.getRemoteAddress().getPort();
	}

	@Override
	public String getRemoteUser() {
		return exchange.getPrincipal().getUsername();
	}

	@Override
	public RequestDispatcher getRequestDispatcher(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getRequestId() {
		throw new UnsupportedOperationException("Not supported yet.");
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
	public String getRequestedSessionId() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getServerPort() {
		return exchange.getLocalAddress().getPort();
	}

	@Override
	public ServletContext getServletContext() {
		return servlet.getServletConfig().getServletContext();
	}

	@Override
	public String getScheme() {
		return exchange.getRequestURI().getScheme();
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
	public ServletConnection getServletConnection() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getServletPath() {
		if (servletPath == null) {
			String contextPath = getServletContext().getContext(getRequestURI()).getContextPath();
			if ("/*".equals(contextPath)) {
				servletPath = "";
			} else {
				servletPath = contextPath;
			}
		}
		return servletPath;
	}

	public HttpExchangeServletResponse getServletResponse() {
		return servletResponse;
	}

	@Override
	public HttpSession getSession(boolean bln) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Principal getUserPrincipal() {
		return exchange.getPrincipal();
	}

	@Override
	public boolean isAsyncStarted() {
		return asyncContext != null;
	}

	@Override
	public boolean isAsyncSupported() {
		return true;
	}

	@Override
	public boolean isRequestedSessionIdValid() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isRequestedSessionIdFromCookie() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isRequestedSessionIdFromURL() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isSecure() {
		return exchange.getRequestURI().getScheme().equals("https");
	}

	@Override
	public boolean isUserInRole(String string) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void login(String string, String string1) throws ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void logout() throws ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void removeAttribute(String string) {
		attributesName.remove(string);
		exchange.setAttribute(string, null);
	}

	@Override
	public void setAttribute(String name, Object o) {
		if (!attributesName.contains(name)) {
			attributesName.add(name);
		}
		exchange.setAttribute(name, o);
	}

	@Override
	public void setCharacterEncoding(String string) throws UnsupportedEncodingException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public AsyncContext startAsync() throws IllegalStateException {
		return startAsync(this, servletResponse);
	}

	@Override
	public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
		if (!isAsyncSupported()) {
			throw new IllegalStateException("This request does not support asynchronous operations.");
		}
		if (servletResponse.isCommitted()) {
			throw new IllegalStateException("The response has already been closed.");
		}
		if (asyncContext == null) {
			asyncContext = new HttpExchangeAsyncContext((HttpExchangeServletRequest) servletRequest, (HttpExchangeServletResponse) servletResponse);
		}
		return asyncContext;
	}

	@Override
	public <T extends HttpUpgradeHandler> T upgrade(Class<T> type) throws IOException, ServletException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private void parseQueryParameters() {
		try {
			parameters.putAll(parseQueryString(exchange.getRequestURI().getQuery()));
		} catch (IllegalArgumentException e) {
		}
	}

	private void parsePostParameters() {
		try {
			String postData = IOUtils.toString(exchange.getRequestBody(), StandardCharsets.UTF_8);
			parameters.putAll(parseQueryString(postData));
		} catch (IOException | IllegalArgumentException e) {
		}
	}

	private void parseCookies() {
		List<Cookie> cookiesList = new ArrayList<>();
		Enumeration<String> cookiesStr = getHeaders("Cookie");
		while (cookiesStr.hasMoreElements()) {
			String cookieStr = cookiesStr.nextElement();
			List<HttpCookie> httpCookies = HttpCookie.parse(cookieStr);
			for (HttpCookie httpCookie : httpCookies) {
				Cookie cookie = new Cookie(httpCookie.getName(), httpCookie.getValue());
				if (httpCookie.getDomain() != null) {
					cookie.setDomain(httpCookie.getDomain());
				}
				cookie.setHttpOnly(httpCookie.isHttpOnly());
				if (httpCookie.getMaxAge() > -1) {
					cookie.setMaxAge((int) httpCookie.getMaxAge());
				}
				if (httpCookie.getPath() != null) {
					cookie.setPath(httpCookie.getPath());
				}
				cookie.setSecure(httpCookie.getSecure());
				cookiesList.add(cookie);
			}
		}
		cookies = cookiesList.toArray(Cookie[]::new);
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

	private void parseLocales() {
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

	private static Map<String, String[]> parseQueryString(String s) {
		String[] valArray;
		if (s == null) {
			throw new IllegalArgumentException();
		}

		Map<String, String[]> ht = new HashMap<>();
		StringBuilder sb = new StringBuilder();
		StringTokenizer st = new StringTokenizer(s, "&");
		while (st.hasMoreTokens()) {
			String pair = st.nextToken();
			int pos = pair.indexOf('=');
			if (pos == -1) {
				// XXX
				// should give more detail about the illegal argument
				throw new IllegalArgumentException();
			}
			String key = parseName(pair.substring(0, pos), sb);
			String val = parseName(pair.substring(pos + 1, pair.length()), sb);
			if (ht.containsKey(key)) {
				String[] oldVals = ht.get(key);
				valArray = new String[oldVals.length + 1];
				System.arraycopy(oldVals, 0, valArray, 0, oldVals.length);
				valArray[oldVals.length] = val;
			} else {
				valArray = new String[1];
				valArray[0] = val;
			}
			ht.put(key, valArray);
		}

		return ht;
	}

	/*
	 * Parse a name in the query string.
	 */
	private static String parseName(String s, StringBuilder sb) {
		sb.setLength(0);
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
				case '+' ->
					sb.append(' ');
				case '%' -> {
					try {
						sb.append((char) Integer.parseInt(s.substring(i + 1, i + 3), 16));
						i += 2;
					} catch (NumberFormatException e) {
						// XXX
						// need to be more specific about illegal arg
						throw new IllegalArgumentException();
					} catch (StringIndexOutOfBoundsException e) {
						String rest = s.substring(i);
						sb.append(rest);
						if (rest.length() == 2) {
							i++;
						}
					}
				}
				default ->
					sb.append(c);
			}
		}
		return sb.toString();
	}

}
