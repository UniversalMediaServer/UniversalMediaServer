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
package net.pms.webserver;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.UUID;
import java.net.UnknownHostException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebServerServlets extends WebServer {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebServerServlets.class);

	public WebServerServlets() throws IOException {
	}

	public RootFolder getRoot(HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		String user = RemoteUtil.userName(request);
		return getRoot(user, false, request, response);
	}

	public RootFolder getRoot(String user, HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		return getRoot(user, false, request, response);
	}

	public RootFolder getRoot(String user, boolean create, HttpServletRequest request, HttpServletResponse response) throws InterruptedException {
		String cookie = HttpServletHelper.getCookie("UMS", request);
		RootFolder root;
		synchronized (roots) {
			root = roots.get(cookie);
			if (root == null) {
				// Double-check for cookie errors
				WebRender valid = RemoteUtil.matchRenderer(user, request);
				if (valid != null) {
					// A browser of the same type and user is already connected
					// at
					// this ip but for some reason we didn't get a cookie match.
					RootFolder validRoot = valid.getRootFolder();
					// Do a reverse lookup to see if it's been registered
					for (Map.Entry<String, RootFolder> entry : roots.entrySet()) {
						if (entry.getValue() == validRoot) {
							// Found
							root = validRoot;
							cookie = entry.getKey();
							LOGGER.debug("Allowing browser connection without cookie match: {}: {}", valid.getRendererName(),
								request.getRemoteAddr());
							break;
						}
					}
				}
			}

			if (!create || (root != null)) {
				response.addHeader("Set-Cookie", "UMS=" + cookie + ";Path=/;SameSite=Strict");
				return root;
			}

			root = new RootFolder();
			try {
				WebRender render = new WebRender(user);
				root.setDefaultRenderer(render);
				render.setRootFolder(root);
				render.associateIP(InetAddress.getByName(request.getRemoteAddr()));
				render.associatePort(request.getRemotePort());
				if (CONFIGURATION.useWebSubLang()) {
					render.setSubLang(StringUtils.join(RemoteUtil.getLangs(request), ","));
				}
				render.setBrowserInfo(HttpServletHelper.getCookie("UMSINFO", request), request.getHeader("User-agent"));
				PMS.get().setRendererFound(render);
			} catch (ConfigurationException | UnknownHostException e) {
				root.setDefaultRenderer(RendererConfiguration.getDefaultConf());
			}

			root.discoverChildren();
			cookie = UUID.randomUUID().toString();
			response.addHeader("Set-Cookie", "UMS=" + cookie + ";Path=/;SameSite=Strict");
			roots.put(cookie, root);
		}
		return root;
	}
}
