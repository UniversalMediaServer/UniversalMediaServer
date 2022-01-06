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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.network.HTTPResource;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteFileServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteFileServlet.class);

	public RemoteFileServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			URI uri = URI.create(request.getRequestURI());
			LOGGER.debug("Handling web interface file request \"{}\"", uri);

			String path = uri.getPath();
			String responseString = null;
			String mime = null;
			int status = 200;

			if (path.contains("crossdomain.xml")) {
				responseString = "<?xml version=\"1.0\"?>" +
					"<!-- http://www.bitsontherun.com/crossdomain.xml -->" +
					"<cross-domain-policy>" +
					"<allow-access-from domain=\"*\" />" +
					"</cross-domain-policy>";
				mime = "text/xml";

			} else if (path.startsWith("/files/log/")) {
				String filename = path.substring(11);
				if (filename.equals("info")) {
					String log = PMS.get().getFrame().getLog();
					log = log.replace("\n", "<br>");
					String fullLink = "<br><a href=\"/files/log/full\">Full log</a><br><br>";
					String x = fullLink + log;
					if (StringUtils.isNotEmpty(log)) {
						x = x + fullLink;
					}
					responseString = "<html><title>UMS LOG</title><body>" + x + "</body></html>";
				} else {
					File file = parent.getResources().getFile(filename);
					if (file != null) {
						filename = file.getName();
						HashMap<String, Object> vars = new HashMap<>();
						vars.put("title", filename);
						vars.put("brush", filename.endsWith("debug.log") ? "debug_log" : filename.endsWith(".log") ? "log" : "conf");
						vars.put("log", RemoteUtil.read(file).replace("<", "&lt;"));
						responseString = parent.getResources().getTemplate("util/log.html").execute(vars);
					} else {
						status = 404;
					}
				}
				mime = "text/html";
			} else if (path.startsWith("/files/proxy")) {
				String url = uri.getQuery();
				if (url != null) {
					url = url.substring(2);
				}

				InputStream in;
				CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
				if (cookieManager == null) {
					cookieManager = new CookieManager();
					CookieHandler.setDefault(cookieManager);
				}

				switch (request.getMethod()) {
					case "POST":
						byte[] buf = new byte[4096];
						int n;
						String str = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
						URLConnection conn = new URL(url).openConnection();
						((HttpURLConnection) conn).setRequestMethod("POST");
						conn.setRequestProperty("Content-type", request.getHeader("Content-type"));
						conn.setRequestProperty("Content-Length", String.valueOf(str.length()));
						conn.setDoOutput(true);
						OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
						writer.write(str);
						writer.flush();
						in = conn.getInputStream();
						ByteArrayOutputStream bytes = new ByteArrayOutputStream();
						while ((n = in.read(buf)) > -1) {
							bytes.write(buf, 0, n);
						}
						in = new ByteArrayInputStream(bytes.toByteArray());
						if (LOGGER.isDebugEnabled()) {
							List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
							for (HttpCookie cookie : cookies) {
								LOGGER.debug("Domain: {}, Cookie: {}", cookie.getDomain(), cookie);
							}
						}
						break;
					case "OPTIONS":
						in = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
						break;
					default:
						in = HTTPResource.downloadAndSend(url, false);
						if (LOGGER.isDebugEnabled()) {
							List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
							for (HttpCookie cookie : cookies) {
								LOGGER.debug("Domain: {}, Cookie: {}", cookie.getDomain(), cookie);
							}
						}
						break;
				}
				response.setContentType("text/plain");
				response.addHeader("Access-Control-Allow-Origin", "*");
				response.addHeader("Access-Control-Allow-Headers", "User-Agent");
				response.addHeader("Access-Control-Allow-Headers", "Content-Type");
				RemoteUtil.dumpDirect(in, response);
				return;
			} else if (parent.getResources().write(path.substring(7), response)) {
				// The resource manager found and sent the file, all done.
				return;

			} else {
				status = 404;
			}

			if (status == 404 && responseString == null) {
				responseString = "<html><body>404 - File Not Found: " + path + "</body></html>";
				mime = "text/html";
			}
			RemoteUtil.respond(response, responseString, status, mime);
		} catch (IOException e) {
			throw e;
		} catch (MustacheException e) {
			// Nothing should get here, this is just to avoid crashing the
			// thread
			LOGGER.error("Unexpected error in RemoteFileHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
