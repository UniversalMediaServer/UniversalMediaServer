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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.FileTranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.external.ExternalFactory;
import net.pms.external.URLResolver.URLResult;
import net.pms.util.FileUtil;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class M3u8Servlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(M3u8Servlet.class);

	public M3u8Servlet(WebServerServlets parent) {
		super(parent);
	}

	private String makeM3u8(DLNAResource dlna) {
		if (dlna != null) {
			return makeM3u8(dlna.isFolder() ? dlna.getChildren() : Arrays.asList(new DLNAResource[]{dlna}));
		}
		return null;
	}

	private String makeM3u8(List<DLNAResource> resources) {
		ArrayList<Map<String, Object>> items = new ArrayList<>();
		double targetDuration = 1;
		for (DLNAResource dlna : resources) {
			if (dlna != null && !dlna.isFolder() && !dlna.isResume() && !(dlna instanceof VirtualVideoAction)) {
				double duration = dlna.getMedia() != null ? dlna.getMedia().getDurationInSeconds() : -1;
				if (duration > targetDuration) {
					targetDuration = duration + 1;
				}
				String url;

				if (!FileUtil.isUrl(dlna.getSystemName())) {
					// Get the resource url
					boolean isTranscodeFolderItem = dlna.isNoName() && (dlna.getParent() instanceof FileTranscodeVirtualFolder);
					// If the resource is not a transcode folder item, tag its url for forced streaming
					url = dlna.getURL(isTranscodeFolderItem  ? "" : RendererConfiguration.NOTRANSCODE, true, false);

				} else {
					// It's a WEB.conf item or plugin-provided url, make sure it's resolved
					url = dlna.getSystemName();
					if (!dlna.isURLResolved()) {
						URLResult r = ExternalFactory.resolveURL(url);
						if (r != null && StringUtils.isNotEmpty(r.url)) {
							url = r.url;
						}
					}
				}

				Map<String, Object> item = new HashMap<>();
				item.put("duration", duration != 0 ? duration : -1);
				item.put("title", dlna.resumeName());
				item.put("url", url);
				items.add(item);
			}
		}

		if (!items.isEmpty()) {
			HashMap<String, Object> vars = new HashMap<>();
			vars.put("targetDuration", targetDuration != 0 ? targetDuration : -1);
			vars.put("items", items);
			return parent.getResources().getTemplate("play.m3u8").execute(vars);
		}
		return null;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			URI uri = URI.create(request.getRequestURI());
			String p = uri.getPath();
			String id = StringUtils.substringBefore(StringUtils.substringAfter(p, "/m3u8/"), ".m3u8");
			String responseString = makeM3u8(PMS.getGlobalRepo().get(id));
			if (responseString != null) {
				LOGGER.debug("sending m3u8:\n" + responseString);
				RemoteUtil.respond(response, responseString, "application/x-mpegURL");
			} else {
				response.sendError(404, "<html><body>404 - File Not Found: " + p + "</body></html>");
			}
		} catch (IOException e) {
			throw e;
		}
	}
}
