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
package net.pms.network.webguiserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import javax.servlet.AsyncContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebGuiServletHelper {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebGuiServletHelper.class);
	private static final Gson GSON = new Gson();
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String HTTPSERVER_REQUEST_BEGIN =  "============================= GUI HTTPSERVER REQUEST BEGIN ================================";
	private static final String HTTPSERVER_REQUEST_END =    "============================= GUI HTTPSERVER REQUEST END ==================================";
	private static final String HTTPSERVER_RESPONSE_BEGIN = "============================= GUI HTTPSERVER RESPONSE BEGIN ===============================";
	private static final String HTTPSERVER_RESPONSE_END =   "============================= GUI HTTPSERVER RESPONSE END =================================";
	private static final ClassLoader CLASS_LOADER = new URLClassLoader(new URL[] {getUrl("file:" + CONFIGURATION.getWebPath() + "/react-client/")});

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebGuiServletHelper() {
	}

	private static URL getUrl(String url) {
		try {
			return new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public static boolean deny(ServletRequest req) {
		try {
			InetAddress inetAddress = InetAddress.getByName(req.getRemoteAddr());
			return !CONFIGURATION.getIpFiltering().allowed(inetAddress) || !PMS.isReady();
		} catch (UnknownHostException ex) {
			return true;
		}
	}

	public static void logHttpServletRequest(HttpServletRequest req, String content) {
		StringBuilder header = new StringBuilder();
		header.append(req.getMethod());
		header.append(" ").append(req.getRequestURI());
		if (header.length() > 0) {
			header.append(" ");
		}
		header.append(req.getProtocol());
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

		String formattedContent = StringUtils.isNotEmpty(content) ? "\nCONTENT:\n" + content + "\n" : "";
		String remoteHost = req.getRemoteHost();
		LOGGER.trace(
				"Received a request from {}:\n{}\n{}{}{}",
				remoteHost,
				HTTPSERVER_REQUEST_BEGIN,
				header,
				formattedContent,
				HTTPSERVER_REQUEST_END
		);
	}

	public static void logHttpServletResponse(HttpServletRequest req, HttpServletResponse resp, String response, InputStream iStream) {
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
		String remoteHost = req.getRemoteAddr();
		if (StringUtils.isNotEmpty(response)) {
			LOGGER.trace(
				"Response sent to {}:\n{}\n{}\n{}\nCONTENT:\n{}{}",
				remoteHost,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				header,
				response,
				HTTPSERVER_RESPONSE_END
			);
		} else if (iStream != null) {
			LOGGER.trace(
				"Transfer response sent to {}:\n{}\n{} ({})\n{}{}",
				remoteHost,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				chunked ? "chunked" : "non-chunked",
				header,
				HTTPSERVER_RESPONSE_END
			);
		} else {
			LOGGER.trace(
				"Empty response sent to {}:\n{}\n{}\n{}{}",
				remoteHost,
				HTTPSERVER_RESPONSE_BEGIN,
				responseCode,
				header,
				HTTPSERVER_RESPONSE_END
			);
		}
	}

	/**
	 * Write the given resource as an HttpServletResponse body.
	 * @param req
	 * @param resp
	 * @param filename
	 * @return
	 * @throws java.io.IOException
	*/
	public static boolean write(HttpServletRequest req, HttpServletResponse resp, String filename) throws IOException {
		InputStream stream = CLASS_LOADER.getResourceAsStream(filename);
		if (stream != null) {
			if (resp.getContentType() == null) {
				String mime = getMimeType(filename);
				if (mime != null) {
					resp.setContentType(mime);
				}
			}
			// Note: available() isn't officially guaranteed to return the full
			// stream length but effectively seems to do so in our context.
			resp.setContentLength(stream.available());
			resp.setStatus(200);
			logHttpServletResponse(req, resp, null, stream);
			copyStream(stream, resp.getOutputStream());
			return true;
		}
		return false;
	}

	/**
	 * Write the given resource as an HttpServletResponse body.
	 * @param req
	 * @param resp
	 * @param filename
	 * @return
	 * @throws java.io.IOException
	*/
	public static boolean writeAsync(HttpServletRequest req, HttpServletResponse resp, String filename) throws IOException {
		InputStream stream = CLASS_LOADER.getResourceAsStream(filename);
		if (stream != null) {
			AsyncContext async = req.startAsync();
			if (resp.getContentType() == null) {
				String mime = getMimeType(filename);
				if (mime != null) {
					resp.setContentType(mime);
				}
			}
			// Note: available() isn't officially guaranteed to return the full
			// stream length but effectively seems to do so in our context.
			resp.setContentLength(stream.available());
			resp.setStatus(200);
			logHttpServletResponse(req, resp, null, stream);
			copyStreamAsync(stream, resp.getOutputStream(), async);
			return true;
		}
		return false;
	}

	public static void copyStream(final InputStream in, final OutputStream os) {
		byte[] buffer = new byte[32 * 1024];
		int bytes;
		int sendBytes = 0;

		try {
			while ((bytes = in.read(buffer)) != -1) {
				sendBytes += bytes;
				os.write(buffer, 0, bytes);
				os.flush();
			}
			LOGGER.trace("Sending stream finished after: " + sendBytes + " bytes.");
		} catch (IOException e) {
			LOGGER.trace("Sending stream with premature end: " + sendBytes + " bytes. Reason: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch (IOException e) {
			}
		}

		try {
			os.close();
		} catch (IOException e) {
		}
	}

	public static void copyStreamAsync(final InputStream in, final OutputStream os, final AsyncContext context) {
		Runnable r = () -> {
			copyStream(in, os);
			context.complete();
		};
		context.start(r);
	}

	public static void respond(HttpServletRequest req, HttpServletResponse resp, String response, int status, String mime) {
		respond(req, resp, response, status, mime, true);
	}

	public static void respond(HttpServletRequest req, HttpServletResponse resp, String response, int status, String mime, boolean logBody) {
		if (response != null) {
			if (mime != null) {
				resp.setContentType(mime);
			}
			byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
			try (OutputStream os = resp.getOutputStream()) {
				resp.setContentLength(bytes.length);
				resp.setStatus(status);
				if (LOGGER.isTraceEnabled()) {
					logHttpServletResponse(req, resp, logBody ? response : "Not logged", null);
				}
				os.write(bytes);
			} catch (Exception e) {
				LOGGER.debug("Error sending response: " + e);
			}
		} else {
			resp.setContentLength(0);
			resp.setStatus(status);
			if (LOGGER.isTraceEnabled()) {
				logHttpServletResponse(req, resp, null, null);
			}
		}
	}

	public static void respondBadRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondBadRequest(req, resp, null);
	}

	public static void respondBadRequest(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, 400, msg);
	}

	public static void respondUnauthorized(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondUnauthorized(req, resp, null);
	}

	public static void respondUnauthorized(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, 401, msg);
	}

	public static void respondForbidden(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondForbidden(req, resp, null);
	}

	public static void respondForbidden(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, 403, msg);
	}

	public static void respondNotFound(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondNotFound(req, resp, null);
	}

	public static void respondNotFound(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, 404, msg);
	}

	public static void respondInternalServerError(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		respondInternalServerError(req, resp, null);
	}

	public static void respondInternalServerError(HttpServletRequest req, HttpServletResponse resp, String msg) throws IOException {
		respondError(req, resp, 500, msg);
	}

	private static void respondError(HttpServletRequest req, HttpServletResponse resp, int status, String msg) throws IOException {
		if (LOGGER.isTraceEnabled()) {
			resp.setStatus(status);
			logHttpServletResponse(req, resp, msg, null);
		}
		if (msg != null) {
			resp.sendError(status, msg);
		} else {
			resp.sendError(status);
		}
	}

	public static String getBodyAsString(HttpServletRequest req) {
		try {
			return IOUtils.toString(req.getInputStream(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return null;
		}
	}

	public static JsonObject getJsonObjectFromBody(HttpServletRequest req) {
		String reqBody = getBodyAsString(req);
		return jsonObjectFromString(reqBody);
	}

	public static JsonArray getJsonArrayFromStringArray(String[] array) {
		return GSON.toJsonTree(array).getAsJsonArray();
	}

	private static JsonObject jsonObjectFromString(String str) {
		JsonObject jObject = null;
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem != null && jElem.isJsonObject()) {
				jObject = jElem.getAsJsonObject();
			}
		} catch (JsonSyntaxException je) {
		}
		return jObject;
	}

	public static JsonArray getJsonArrayFromBody(HttpServletRequest req) {
		String reqBody = getBodyAsString(req);
		return jsonArrayFromString(reqBody);
	}

	private static JsonArray jsonArrayFromString(String str) {
		JsonArray jArray = null;
		try {
			JsonElement jElem = GSON.fromJson(str, JsonElement.class);
			if (jElem != null && jElem.isJsonArray()) {
				jArray = jElem.getAsJsonArray();
			}
		} catch (JsonSyntaxException je) {
			LOGGER.error("", je);
		}
		return jArray;
	}

	public static InetAddress getInetAddress(String host) {
		try {
			return InetAddress.getByName(host);
		} catch (UnknownHostException ex) {
		}
		return null;
	}

	public static Cookie getCookie(HttpServletRequest req, String name) {
		Cookie[] cookies = req.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equalsIgnoreCase(name)) {
					return cookie;
				}
			}
		}
		return null;
	}

	public static String getLangs(HttpServletRequest req) {
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

	public static String getMimeType(String file) {
		return file.endsWith(".html") ? "text/html" :
		file.endsWith(".css") ? "text/css" :
		file.endsWith(".js") ? "text/javascript" :
		file.endsWith(".ttf") ? "font/truetype" :
		URLConnection.guessContentTypeFromName(file);
	}
}
