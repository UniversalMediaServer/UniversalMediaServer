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
package net.pms.network.webinterfaceserver.handlers;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.*;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.StandardPlayerId;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final WebInterfaceServerHttpServer parent;
	private final String path;
	private final RendererConfiguration renderer;
	private final boolean flash;

	public MediaHandler(WebInterfaceServerHttpServer parent) {
		this(parent, "media/", null, false);
	}

	public MediaHandler(WebInterfaceServerHttpServer parent, boolean flash) {
		this(parent, "fmedia/", null, flash);
	}

	public MediaHandler(WebInterfaceServerHttpServer parent, String path, RendererConfiguration renderer) {
		this(parent, path, renderer, false);
	}

	public MediaHandler(WebInterfaceServerHttpServer parent, String path, RendererConfiguration renderer, boolean flash) {
		this.parent = parent;
		this.path = path;
		this.renderer = renderer;
		this.flash = flash;
	}
	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			if (WebInterfaceServerUtil.deny(httpExchange)) {
				throw new IOException("Access denied");
			}
			RootFolder root = parent.getRoot(WebInterfaceServerUtil.userName(httpExchange), httpExchange);
			if (root == null) {
				throw new IOException("Unknown root");
			}
			Headers h = httpExchange.getRequestHeaders();
			for (String h1 : h.keySet()) {
				LOGGER.debug("key " + h1 + "=" + h.get(h1));
			}
			String id = WebInterfaceServerUtil.getId(path, httpExchange);
			id = WebInterfaceServerUtil.strip(id);
			RendererConfiguration defaultRenderer = renderer;
			if (renderer == null) {
				defaultRenderer = root.getDefaultRenderer();
			}
			DLNAResource resource = root.getDLNAResource(id, defaultRenderer);
			if (resource == null) {
				// another error
				LOGGER.debug("media unknown");
				throw new IOException("Bad id");
			}
			if (!resource.isCodeValid(resource)) {
				LOGGER.debug("coded object with invalid code");
				throw new IOException("Bad code");
			}
			DLNAMediaSubtitle sid = null;
			String mimeType = root.getDefaultRenderer().getMimeType(resource);
			//DLNAResource dlna = res.get(0);
			WebRender render = (WebRender) defaultRenderer;
			DLNAMediaInfo media = resource.getMedia();
			if (media == null) {
				media = new DLNAMediaInfo();
				resource.setMedia(media);
			}
			if (mimeType.equals(FormatConfiguration.MIMETYPE_AUTO) && media.getMimeType() != null) {
				mimeType = media.getMimeType();
			}
			int code = 200;
			resource.setDefaultRenderer(defaultRenderer);
			if (resource.getFormat().isVideo()) {
				if (flash) {
					mimeType = "video/flash";
				} else if (!WebInterfaceServerUtil.directmime(mimeType) || WebInterfaceServerUtil.transMp4(mimeType, media)) {
					mimeType = render != null ? render.getVideoMimeType() : WebInterfaceServerUtil.transMime();
					// TODO: Use normal engine priorities instead of the following hacks
					if (FileUtil.isUrl(resource.getSystemName())) {
						if (FFmpegWebVideo.isYouTubeURL(resource.getSystemName())) {
							resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.YOUTUBE_DL, false, false));
						} else {
							resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_WEB_VIDEO, false, false));
						}
					} else if (!(resource instanceof DVDISOTitle)) {
						resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_VIDEO, false, false));
					}
					//code = 206;
				}
				if (
					PMS.getConfiguration().getWebSubs() &&
					resource.getMediaSubtitle() != null &&
					resource.getMediaSubtitle().isExternal()
				) {
					// fetched on the side
					sid = resource.getMediaSubtitle();
					resource.setMediaSubtitle(null);
				}
			}

			if (!WebInterfaceServerUtil.directmime(mimeType) && resource.getFormat().isAudio()) {
				resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_AUDIO, false, false));
				code = 206;
			}

			media.setMimeType(mimeType);
			Range.Byte range = WebInterfaceServerUtil.parseRange(httpExchange.getRequestHeaders(), resource.length());
			LOGGER.debug("Sending {} with mime type {} to {}", resource, mimeType, renderer);
			if (render == null || !HTTPResource.HLS_TYPEMIME.equals(render.getVideoMimeType())) {
				if (range.getEnd() == 0) {
					// For web resources actual length may be unknown until we open the stream
					range.setEnd(resource.length());
				}
				Headers headers = httpExchange.getResponseHeaders();
				headers.add("Content-Type", mimeType);
				headers.add("Accept-Ranges", "bytes");
				long end = range.getEnd();
				long start = range.getStart();
				String rStr = start + "-" + end + "/*";
				headers.add("Content-Range", "bytes " + rStr);
				if (start != 0) {
					code = 206;
				}
				headers.add("Server", PMS.get().getServerName());
				headers.add("Connection", "keep-alive");
				httpExchange.sendResponseHeaders(code, 0);
				if (render != null) {
					render.start(resource);
				}
				if (sid != null) {
					resource.setMediaSubtitle(sid);
				}
				InputStream in = resource.getInputStream(range, root.getDefaultRenderer());
				OutputStream os = httpExchange.getResponseBody();
				WebInterfaceServerUtil.dump(in, os);
			} else {
				render.start(resource);
				if (sid != null) {
					resource.setMediaSubtitle(sid);
				}
				String playlistPath = FileUtil.appendPathSeparator(CONFIGURATION.getTempFolder().getAbsolutePath()) + "webhls-" + id + "-playlist.m3u8";
				File playlist = new File(playlistPath);

				StringBuilder resultStringBuilder = new StringBuilder();
				try (BufferedReader br = new BufferedReader(new FileReader(playlist))) {
					String line;
					while ((line = br.readLine()) != null) {
						resultStringBuilder.append(line).append("\n");
					}
				}
				String response = resultStringBuilder.toString();
				WebInterfaceServerUtil.respond(httpExchange, response, 200, HTTPResource.HLS_TYPEMIME);
			}
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in MediaHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
