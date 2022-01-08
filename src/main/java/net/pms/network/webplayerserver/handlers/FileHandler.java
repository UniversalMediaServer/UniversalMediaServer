/*
 * Universal Media Server, for streaming any media to DLNA
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
package net.pms.network.webplayerserver.handlers;

import com.samskivert.mustache.MustacheException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import net.pms.PMS;
import net.pms.network.HTTPResource;
import net.pms.network.webplayerserver.WebPlayerServerUtil;
import net.pms.network.webplayerserver.WebPlayerServerHttpServer;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbHandler.class);

	private final WebPlayerServerHttpServer parent;

	public FileHandler(WebPlayerServerHttpServer parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			LOGGER.debug("Handling web player server file request \"{}\"", t.getRequestURI());

			String path = t.getRequestURI().getPath();
			String response = null;
			String mime = null;
			int status = 200;

			if (path.contains("crossdomain.xml")) {
				response = "<?xml version=\"1.0\"?>" +
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
					response = "<html><title>UMS LOG</title><body>" + x + "</body></html>";
				} else {
					File file = parent.getResources().getFile(filename);
					if (file != null) {
						filename = file.getName();
						HashMap<String, Object> vars = new HashMap<>();
						vars.put("title", filename);
						vars.put("brush", filename.endsWith("debug.log") ? "debug_log" : filename.endsWith(".log") ? "log" : "conf");
						vars.put("log", WebPlayerServerUtil.read(file).replace("<", "&lt;"));
						response = parent.getResources().getTemplate("util/log.html").execute(vars);
					} else {
						status = 404;
					}
				}
				mime = "text/html";
			} else if (path.startsWith("/files/proxy")) {
				String url = t.getRequestURI().getQuery();
				if (url != null) {
					url = url.substring(2);
				}

				InputStream in;
				CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
				if (cookieManager == null) {
					cookieManager = new CookieManager();
					CookieHandler.setDefault(cookieManager);
				}

				switch (t.getRequestMethod()) {
					case "POST":
						ByteArrayOutputStream bytes = new ByteArrayOutputStream();
						byte[] buf = new byte[4096];
						int n;
						while ((n = t.getRequestBody().read(buf)) > -1) {
							bytes.write(buf, 0, n);
						}
						String str = bytes.toString("utf-8");
						URLConnection conn = new URL(url).openConnection();
						((HttpURLConnection) conn).setRequestMethod("POST");
						conn.setRequestProperty("Content-type", t.getRequestHeaders().getFirst("Content-type"));
						conn.setRequestProperty("Content-Length", String.valueOf(str.length()));
						conn.setDoOutput(true);
						OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
						writer.write(str);
						writer.flush();
						in = conn.getInputStream();
						bytes = new ByteArrayOutputStream();
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
				Headers hdr = t.getResponseHeaders();
				hdr.add("Content-Type", "text/plain");
				hdr.add("Access-Control-Allow-Origin", "*");
				hdr.add("Access-Control-Allow-Headers", "User-Agent");
				hdr.add("Access-Control-Allow-Headers", "Content-Type");
				t.sendResponseHeaders(200, in.available());

				OutputStream os = t.getResponseBody();
				LOGGER.trace("input is {} output is {}", in, os);
				WebPlayerServerUtil.dump(in, os);
				return;
			} else if (parent.getResources().write(path.substring(7), t)) {
				// The resource manager found and sent the file, all done.
				return;

			} else {
				status = 404;
			}

			if (status == 404 && response == null) {
				response = "<html><body>404 - File Not Found: " + path + "</body></html>";
				mime = "text/html";
			}

			WebPlayerServerUtil.respond(t, response, status, mime);
		} catch (IOException e) {
			throw e;
		} catch (MustacheException e) {
			// Nothing should get here, this is just to avoid crashing the
			// thread
			LOGGER.error("Unexpected error in FileHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
