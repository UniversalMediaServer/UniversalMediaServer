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
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HttpExchangeServletResponse implements HttpServletResponse, AutoCloseable {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
	private final HttpExchange exchange;

	private String contentType;
	private String characterEncoding;
	private int bufferSize = 32 * 1024;
	private int status = HttpServletResponse.SC_OK;
	private long contentLength = -1L;
	private Locale locale = Locale.US;
	private boolean committed = false;
	private HttpExchangeServletOutputStream servletOutputStream;
	private PrintWriter printWriter;

	public HttpExchangeServletResponse(HttpExchange exchange) {
		this.exchange = exchange;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		if (servletOutputStream == null) {
			if (!isCommitted()) {
				exchange.sendResponseHeaders(status, contentLength > -1 ? contentLength : 0);
				committed = true;
			}
			servletOutputStream = new HttpExchangeServletOutputStream(exchange.getResponseBody());
		}
		return servletOutputStream;
	}

	@Override
	public void setStatus(int status) {
		this.status = status;
	}

	@Override
	public void sendError(int sc, String msg) throws IOException {
		if (isCommitted()) {
			throw new IllegalStateException("The response has already been committed.");
		}
		status = sc;
		if (msg != null) {
			exchange.sendResponseHeaders(status, 0);
			committed = true;
			getWriter().write(msg);
		} else {
			exchange.sendResponseHeaders(status, -1);
			committed = true;
		}
	}

	@Override
	public void sendError(int sc) throws IOException {
		sendError(sc, null);
	}

	@Override
	public void sendRedirect(String location) throws IOException {
		setHeader("Location", location);
		sendError(HttpServletResponse.SC_MOVED_TEMPORARILY, null);
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		if (printWriter == null) {
			printWriter = new PrintWriter(getOutputStream());
		}
		return printWriter;
	}

	@Override
	public void setCharacterEncoding(String charset) {
		characterEncoding = charset;
		refreshContentType();
	}

	@Override
	public void setContentLength(int len) {
		setContentLengthLong(len);
	}

	@Override
	public void setContentLengthLong(long len) {
		if (len > 0) {
			contentLength = len;
			setHeader("Content-Length", Long.toString(contentLength));
			exchange.getResponseHeaders().remove("Transfer-Encoding");
		} else {
			contentLength = -1L;
			exchange.getResponseHeaders().remove("Content-Length");
		}
	}

	@Override
	public void setContentType(String type) {
		contentType = type;
		refreshContentType();
	}

	@Override
	public void setBufferSize(int size) {
		bufferSize = size;
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public void flushBuffer() throws IOException {

	}

	@Override
	public void addCookie(Cookie cookie) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean containsHeader(String name) {
		return exchange.getResponseHeaders().containsKey(name);
	}

	@Override
	public String encodeURL(String url) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public String encodeRedirectURL(String url) {
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	@Deprecated
	public String encodeUrl(String url) {
		return encodeURL(url);
	}

	@Override
	@Deprecated
	public String encodeRedirectUrl(String url) {
		return encodeRedirectURL(url);
	}

	@Override
	public void setDateHeader(String name, long date) {
		String dateStr = DATE_FORMAT.format(new Date(date));
		setHeader(name, dateStr);
	}

	@Override
	public void addDateHeader(String name, long date) {
		String dateStr = DATE_FORMAT.format(new Date(date));
		addHeader(name, dateStr);
	}

	@Override
	public void setHeader(String name, String value) {
		exchange.getResponseHeaders().set(name, value);
		if (name.equalsIgnoreCase("Transfer-Encoding") && value.contains("chunked")) {
			contentLength = 0;
		}
	}

	@Override
	public void addHeader(String name, String value) {
		exchange.getResponseHeaders().add(name, value);
	}

	@Override
	public void setIntHeader(String name, int value) {
		setHeader(name, Integer.toString(value));
	}

	@Override
	public void addIntHeader(String name, int value) {
		addHeader(name, Integer.toString(value));
	}

	@Override
	@Deprecated
	public void setStatus(int sc, String sm) {
		try {
			sendError(sc, sm);
		} catch (IOException ex) {
		}
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String getHeader(String name) {
		return exchange.getResponseHeaders().getFirst(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		return exchange.getResponseHeaders().get(name);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return exchange.getResponseHeaders().keySet();
	}

	@Override
	public String getCharacterEncoding() {
		return characterEncoding;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public void resetBuffer() {
		if (isCommitted()) {
			throw new IllegalStateException("Response has been committed.");
		}
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public boolean isCommitted() {
		return committed;
	}

	@Override
	public void reset() {
		if (isCommitted()) {
			throw new IllegalStateException("Response has been committed.");
		}
		throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
	}

	@Override
	public void setLocale(Locale loc) {
		if (isCommitted() || characterEncoding != null) {
			return;
		}
		locale = loc;
		/* TODO update charEnc
		if (characterEncoding == null) {
		}
		*/
		setHeader("Content-Language", locale.getCountry() + "-" + locale.getLanguage());
	}

	@Override
	public Locale getLocale() {
		return locale;
	}

	private void refreshContentType() {
		String cType = contentType;
		if (characterEncoding != null) {
			if (cType != null) {
				cType += "; ";
			}
			cType += "charset=" + characterEncoding;
		}
		if (cType == null) {
			exchange.getResponseHeaders().remove("Content-Type");
		} else {
			setHeader("Content-Type", cType);
		}
	}

	@Override
	public void close() {
		if (!isCommitted()) {
			try {
				exchange.sendResponseHeaders(status, contentLength);
			} catch (IOException ex) {
			} finally {
				committed = true;
			}
		}
		if (printWriter != null) {
			printWriter.flush();
		}
		if (servletOutputStream != null) {
			try {
				servletOutputStream.flush();
			} catch (IOException ex) {
			}
			try {
				servletOutputStream.close();
			} catch (IOException ex) {
			}
		}
	}

	@Override
	@Deprecated
	protected void finalize() throws Throwable {
		try {
			close();
		} finally {
			super.finalize();
		}
	}

}
