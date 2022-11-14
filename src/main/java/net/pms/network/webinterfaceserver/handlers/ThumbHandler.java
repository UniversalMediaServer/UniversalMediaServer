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
package net.pms.network.webinterfaceserver.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.DbIdMediaType;
import net.pms.dlna.DbIdResourceLocator;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.MediaLibraryFolder;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageFormat;
import net.pms.network.HTTPResource;
import net.pms.util.FullyPlayed;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServerInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThumbHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ThumbHandler.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final WebInterfaceServerHttpServerInterface parent;


	public ThumbHandler(WebInterfaceServerHttpServerInterface parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(t, "");
			}
			String id = WebInterfaceServerUtil.getId("thumb/", t);
			LOGGER.trace("web thumb req " + id);
			if (id.contains("logo")) {
				WebInterfaceServerUtil.sendLogo(t);
				return;
			}

			RootFolder root = parent.getRoot(WebInterfaceServerUtil.userName(t), t);
			if (root == null) {
				LOGGER.debug("weird root in thumb req");
				throw new IOException("Unknown root");
			}

			DLNAResource r = null;
			if (id.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
				try {
					r = DbIdResourceLocator.locateResource(id, root.getDefaultRenderer()); // id.substring(0, id.indexOf('/'))
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				r = root.getDLNAResource(id, root.getDefaultRenderer());
			}

			if (r == null) {
				// another error
				LOGGER.debug("media unknown");
				throw new IOException("Bad id");
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
			Headers hdr = t.getResponseHeaders();
			hdr.add("Content-Type", ImageFormat.PNG.equals(in.getFormat()) ? HTTPResource.PNG_TYPEMIME : HTTPResource.JPEG_TYPEMIME);
			hdr.add("Accept-Ranges", "bytes");
			hdr.add("Connection", "keep-alive");
			t.sendResponseHeaders(200, in.getSize());
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageSent(t, null, in);
			}
			OutputStream os = t.getResponseBody();
			LOGGER.trace("Web thumbnail: Input is {} output is {}", in, os);
			WebInterfaceServerUtil.dump(in, os);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in ThumbHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
