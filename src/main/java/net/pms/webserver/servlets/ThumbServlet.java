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
import java.io.OutputStream;
import java.net.URI;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageFormat;
import net.pms.network.HTTPResource;
import net.pms.util.FullyPlayed;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThumbServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbServlet.class);

	public ThumbServlet(WebServerServlets parent) {
		super(parent);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			URI uri = URI.create(request.getRequestURI());
			String id = RemoteUtil.getId("thumb/", uri);
			LOGGER.trace("web thumb req " + id);
			if (id.contains("logo")) {
				RemoteUtil.sendLogo(response);
				return;
			}
			RootFolder root = parent.getRoot(request, response);
			if (root == null) {
				LOGGER.debug("root not found");
				response.sendError(401, "Unknown root");
				return;
			}

			final DLNAResource r = root.getDLNAResource(id, root.getDefaultRenderer());
			if (r == null) {
				// another error
				LOGGER.debug("media unkonwn");
				response.sendError(400, "Bad id");
				return;
			}

			DLNAThumbnailInputStream in;
			if (!CONFIGURATION.isShowCodeThumbs() && !r.isCodeValid(r)) {
				// we shouldn't show the thumbs for coded objects
				// unless the code is entered
				in = r.getGenericThumbnailInputStream(null);
			} else {
				r.checkThumbnail();
				in = r.fetchThumbnailInputStream();
				if (in == null) {
					// if r is null for some reason, default to generic thumb
					in = r.getGenericThumbnailInputStream(null);
				}
			}

			BufferedImageFilterChain filterChain = null;
			if (
				(
					r instanceof RealFile &&
					FullyPlayed.isFullyPlayedFileMark(((RealFile) r).getFile())
				) ||
				(
					r instanceof MediaLibraryFolder &&
					((MediaLibraryFolder) r).isTVSeries() &&
					FullyPlayed.isFullyPlayedTVSeriesMark(((MediaLibraryFolder) r).getName())
				)
			) {
				filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
			}
			filterChain = r.addFlagFilters(filterChain);
			if (filterChain != null) {
				in = in.transcode(in.getDLNAImageProfile(), false, filterChain);
			}
			response.setContentType(ImageFormat.PNG.equals(in.getFormat()) ? HTTPResource.PNG_TYPEMIME : HTTPResource.JPEG_TYPEMIME);
			response.addHeader("Accept-Ranges", "bytes");
			response.addHeader("Connection", "keep-alive");
			response.setContentLengthLong(in.getSize());

			OutputStream os = response.getOutputStream();
			LOGGER.trace("Web thumbnail: Input is {} output is {}", in, os);
			RemoteUtil.dumpDirect(in, os);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ThumbServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
