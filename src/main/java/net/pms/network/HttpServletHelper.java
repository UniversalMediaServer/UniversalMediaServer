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
package net.pms.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.servlets.StartStopListener;
import net.pms.util.StringUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Surf@ceS
 */
public abstract class HttpServletHelper extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpServletHelper.class);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String LOG_START = "============================= ";
	private static final String LOG_REQUEST_BEGIN = " REQUEST BEGIN ================================";
	private static final String LOG_REQUEST_END = " REQUEST END ==================================";
	private static final String LOG_RESPONSE_BEGIN = " RESPONSE BEGIN ===============================";
	private static final String LOG_RESPONSE_END = " RESPONSE END =================================";

	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	/**
	 * This class is not meant to be instantiated directly.
	 */
	protected HttpServletHelper() {
	}

	protected static InetAddress getInetAddress(ServletRequest req) {
		try {
			return InetAddress.getByName(req.getRemoteAddr());
		} catch (UnknownHostException ex) {
			return null;
		}
	}

	protected static boolean isLocalhost(ServletRequest req) {
		try {
			InetAddress inetAddress = InetAddress.getByName(req.getRemoteAddr());
			return inetAddress.isLoopbackAddress();
		} catch (UnknownHostException ex) {
			return false;
		}
	}

	protected static boolean deny(ServletRequest req) {
		try {
			InetAddress inetAddress = InetAddress.getByName(req.getRemoteAddr());
			return !NetworkDeviceFilter.isAllowed(inetAddress) || !PMS.isReady();
		} catch (UnknownHostException ex) {
			return true;
		}
	}

	protected static boolean isHttp10(ServletRequest req) {
		return "HTTP/1.0".equals(req.getProtocol());
	}

	public static void logHttpServletRequest(HttpServletRequest req, String content) {
		logHttpServletRequest(req, content, req.getRemoteHost());
	}

	protected static void logHttpServletRequest(HttpServletRequest req, String content, String remoteHost) {
		StringBuilder header = new StringBuilder();
		header.append(req.getMethod());
		header.append(" ").append(req.getRequestURI());
		String query = req.getQueryString();
		if (StringUtils.isNotBlank(query)) {
			header.append("?").append(query);
		}
		header.append(" ").append(req.getProtocol());
		header.append("\n\n");
		header.append("HEADER:\n");
		Enumeration<String> headerNames = req.getHeaderNames();
		if (headerNames != null) {
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				Enumeration<String> headers = req.getHeaders(headerName);
				while (headers.hasMoreElements()) {
					String headerValue = headers.nextElement();
					header.append("  ").append(headerName).append(": ").append(headerValue).append("\n");
				}
			}
		}

		String formattedContent = getFormattedContent(content, req.getContentType());
		String servletName = req.getHttpServletMapping().getServletName();
		LOGGER.trace("Received a request from {}:\n{}{}{}\n{}{}{}{}{}",
				remoteHost,
				LOG_START,
				servletName,
				LOG_REQUEST_BEGIN,
				header,
				formattedContent,
				LOG_START,
				servletName,
				LOG_REQUEST_END
		);
	}

	public static void logHttpServletResponse(HttpServletRequest req, HttpServletResponse resp, String content, boolean isStream) {
		logHttpServletResponse(req, resp, content, isStream, req.getRemoteAddr());
	}

	protected static void logHttpServletResponse(HttpServletRequest req, HttpServletResponse resp, String content, boolean isStream, String remoteHost) {
		boolean chunked = false;
		StringBuilder header = new StringBuilder();
		for (String headerName : resp.getHeaderNames()) {
			for (String headerValue : resp.getHeaders(headerName)) {
				header.append("  ").append(headerName).append(": ").append(headerValue).append("\n");
				if (!chunked && headerValue.equals("chunked") && headerName.equalsIgnoreCase("transfer-encoding")) {
					chunked = true;
				}
			}
		}

		if (header.length() > 0) {
			header.insert(0, "\nHEADER:\n");
		}

		String responseCode = req.getProtocol() + " " + resp.getStatus();
		String servletName = req.getHttpServletMapping().getServletName();
		if ("HEAD".equalsIgnoreCase(req.getMethod())) {
			LOGGER.trace(
					"HEAD only response sent to {}:\n{}{}{}\n{}\n{}{}{}{}",
					remoteHost,
					LOG_START,
					servletName,
					LOG_RESPONSE_BEGIN,
					responseCode,
					header,
					LOG_START,
					servletName,
					LOG_RESPONSE_END
			);
		} else if (StringUtils.isNotEmpty(content)) {
			String formattedContent = getFormattedContent(content, resp.getContentType());
			LOGGER.trace("Response sent to {}:\n{}{}{}\n{}\n{}{}{}{}{}",
					remoteHost,
					LOG_START,
					servletName,
					LOG_RESPONSE_BEGIN,
					responseCode,
					header,
					formattedContent,
					LOG_START,
					servletName,
					LOG_RESPONSE_END
			);
		} else if (isStream) {
			LOGGER.trace("Transfer response sent to {}:\n{}{}{}\n{} ({})\n{}{}{}{}",
					remoteHost,
					LOG_START,
					servletName,
					LOG_RESPONSE_BEGIN,
					responseCode,
					chunked ? "chunked" : "non-chunked",
					header,
					LOG_START,
					servletName,
					LOG_RESPONSE_END
			);
		} else {
			LOGGER.trace("Empty response sent to {}:\n{}{}{}\n{}\n{}{}{}{}",
					remoteHost,
					LOG_START,
					servletName,
					LOG_RESPONSE_BEGIN,
					responseCode,
					header,
					LOG_START,
					servletName,
					LOG_RESPONSE_END
			);
		}
	}

	private static String getFormattedContent(String content, String contentType) {
		if (StringUtils.isEmpty(content)) {
			return "";
		}
		if (content.endsWith("\n")) {
			content = content.substring(0, content.lastIndexOf("\n"));
		}
		String formattedContent = null;
		if (contentType != null) {
			contentType = contentType.toLowerCase();
			if (contentType.startsWith("text/xml")) {
				try {
					formattedContent = StringUtil.prettifyXML(content, null, 4);
				} catch (SAXException | ParserConfigurationException | XPathExpressionException | TransformerException ex) {
					//nothing to do
				}
			} else if (contentType.startsWith("application/json")) {
				try {
					formattedContent = GSON.toJson(GSON.fromJson(content, Object.class));
				} catch (JsonSyntaxException ex) {
					//nothing to do
				}
			}
		}
		if (formattedContent == null) {
			formattedContent = "    " + content.replace("\n", "\n    ");
		}
		formattedContent = "\nCONTENT:\n" + formattedContent + "\n";
		return formattedContent;
	}

	protected static void copyStream(final InputStream in, final OutputStream os, final AsyncContext context, final UmsAsyncListener umsAsyncListener, final StartStopListener startStopListener) {
		byte[] buffer = new byte[32 * 1024];
		int bytes;
		long sendBytes = 0;

		try {
			if (startStopListener != null) {
				startStopListener.start();
			}
			while ((bytes = in.read(buffer)) != -1) {
				os.write(buffer, 0, bytes);
				sendBytes += bytes;
				os.flush();
				if (umsAsyncListener != null) {
					umsAsyncListener.setBytesSent(sendBytes);
				}
			}
			LOGGER.trace("Sending stream finished after: " + sendBytes + " bytes.");
		} catch (IOException e) {
			String reason = e.getMessage();
			if (reason == null && e.getCause() != null) {
				reason = e.getCause().getMessage();
			}
			LOGGER.debug("Sending stream with premature end: " + sendBytes + " bytes. Reason: " + reason);
			if (umsAsyncListener != null) {
				umsAsyncListener.onPrematureEnd(reason);
			}
			if (startStopListener != null) {
				startStopListener.stop();
			}
		} finally {
			try {
				in.close();
			} catch (IOException e) {
				//do not care
			}
		}

		try {
			os.close();
		} catch (IOException e) {
			//do not care
		}
		context.complete();
	}

	protected static void copyStreamAsync(final InputStream in, final OutputStream os, final AsyncContext context, final StartStopListener startStopListener) {
		UmsAsyncListener umsAsyncListener = new UmsAsyncListener(System.currentTimeMillis(), 0);
		context.addListener(umsAsyncListener);
		if (startStopListener != null) {
			context.setTimeout(0);
			context.addListener(startStopListener);
		}
		Runnable r = () -> copyStream(in, os, context, umsAsyncListener, startStopListener);
		context.start(r);
	}

	protected static void copyStreamAsync(final InputStream in, final OutputStream os, final AsyncContext context) {
		copyStreamAsync(in, os, context, null);
	}

	protected static void respond(HttpServletRequest req, HttpServletResponse resp, String response, int status, String mime) {
		respond(req, resp, response, status, mime, true);
	}

	protected static void respond(HttpServletRequest req, HttpServletResponse resp, String response, int status, String mime, boolean logBody) {
		resp.setHeader("Server", MediaServer.getServerName());
		if (response != null) {
			if (mime != null) {
				resp.setContentType(mime);
			}
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			resp.setContentLength(bytes.length);
			resp.setStatus(status);
			if (!req.getMethod().equalsIgnoreCase("head")) {
				try (OutputStream os = resp.getOutputStream()) {
					if (LOGGER.isTraceEnabled()) {
						logHttpServletResponse(req, resp, logBody ? response : "Not logged", false);
					}
					os.write(bytes);
				} catch (Exception e) {
					LOGGER.debug("Error sending response: " + e);
				}
			} else if (LOGGER.isTraceEnabled()) {
				logHttpServletResponse(req, resp, "HEAD request", false);
			}
		} else {
			resp.setContentLength(0);
			resp.setStatus(status);
			if (LOGGER.isTraceEnabled()) {
				logHttpServletResponse(req, resp, null, false);
			}
		}
	}

	protected static void respondNotModified(HttpServletRequest req, HttpServletResponse resp) {
		respond(req, resp, null, HttpServletResponse.SC_NOT_MODIFIED, null);
	}

	protected static void respondBadRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondBadRequest(req, resp, null);
	}

	protected static void respondBadRequest(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_BAD_REQUEST, msg);
	}

	protected static void respondUnauthorized(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondUnauthorized(req, resp, null);
	}

	protected static void respondUnauthorized(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_UNAUTHORIZED, msg);
	}

	protected static void respondForbidden(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondForbidden(req, resp, null);
	}

	protected static void respondForbidden(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_FORBIDDEN, msg);
	}

	protected static void respondNotFound(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondNotFound(req, resp, null);
	}

	protected static void respondNotFound(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_NOT_FOUND, msg);
	}

	protected static void respondNotAllowed(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondNotAllowed(req, resp, null);
	}

	protected static void respondNotAllowed(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_METHOD_NOT_ALLOWED, msg);
	}

	protected static void respondUnsupportedMediaType(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondUnsupportedMediaType(req, resp, null);
	}

	protected static void respondUnsupportedMediaType(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, msg);
	}

	protected static void respondInternalServerError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondInternalServerError(req, resp, null);
	}

	protected static void respondInternalServerError(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
	}

	protected static void respondServiceUnavailable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondServiceUnavailable(req, resp, null);
	}

	protected static void respondServiceUnavailable(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, msg);
	}

	protected static void respondError(HttpServletRequest req, HttpServletResponse resp, int status, String msg) throws IOException {
		resp.setHeader("Server", MediaServer.getServerName());
		if (LOGGER.isTraceEnabled()) {
			resp.setStatus(status);
			logHttpServletResponse(req, resp, msg, false);
		}
		if (msg != null) {
			resp.sendError(status, msg);
		} else {
			resp.sendError(status);
		}
	}

	protected static String getBodyAsString(HttpServletRequest req) {
		try {
			return IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return null;
		}
	}

	public static URI getRequestReferer(HttpServletRequest req) {
		String referer = req.getHeader("Referer");
		if (referer != null) {
			try {
				return new URI(referer);
			} catch (URISyntaxException x) {
				//bad referer url
			}
		}
		return null;
	}

	public static String getRequestLanguages(HttpServletRequest req) {
		StringBuilder result = new StringBuilder();
		Enumeration<Locale> locales = req.getLocales();
		while (locales.hasMoreElements()) {
			Locale locale = locales.nextElement();
			if (!result.isEmpty()) {
				result.append(",");
			} else {
				result.append(locale.getLanguage());
			}
		}
		return result.toString();
	}

	public static boolean hasHeaderValue(HttpServletRequest request, String header, String value) {
		Enumeration<String> acceptValues = request.getHeaders(header);
		while (acceptValues.hasMoreElements()) {
			if (acceptValues.nextElement().equals(value)) {
				return true;
			}
		}
		return false;
	}

}
