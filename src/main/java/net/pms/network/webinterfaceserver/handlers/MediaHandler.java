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
import java.io.InputStream;
import java.io.OutputStream;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.dlna.*;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.encoders.HlsHelper;
import net.pms.encoders.StandardEngineId;
import net.pms.media.MediaInfo;
import net.pms.media.chapter.MediaChapter;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServerInterface;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.WebRender;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaHandler.class);

	private final WebInterfaceServerHttpServerInterface parent;
	private final String path;
	private final Renderer renderer;
	private final boolean flash;

	public MediaHandler(WebInterfaceServerHttpServerInterface parent) {
		this(parent, "media/", null, false);
	}

	public MediaHandler(WebInterfaceServerHttpServerInterface parent, boolean flash) {
		this(parent, "fmedia/", null, flash);
	}

	public MediaHandler(WebInterfaceServerHttpServerInterface parent, String path, Renderer renderer) {
		this(parent, path, renderer, false);
	}

	public MediaHandler(WebInterfaceServerHttpServerInterface parent, String path, Renderer renderer, boolean flash) {
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
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageReceived(httpExchange, "");
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
			if (id.contains("/hls/")) {
				//clean for hls
				id = id.substring(0, id.indexOf("/hls/"));
			}
			if (id.endsWith("/chapters.json") || id.endsWith("/chapters.vtt")) {
				//clean for chapters
				id = id.substring(0, id.lastIndexOf("/chapters"));
			}
			Renderer defaultRenderer = renderer;
			if (renderer == null) {
				defaultRenderer = root.getDefaultRenderer();
			}
			DLNAResource resource = root.getDLNAResource(id, defaultRenderer);
			if (resource == null) {
				// another error
				LOGGER.debug("media unkonwn");
				throw new IOException("Bad id");
			}
			if (!resource.isCodeValid(resource)) {
				LOGGER.debug("coded object with invalid code");
				throw new IOException("Bad code");
			}
			MediaSubtitle sid = null;
			String mimeType = root.getDefaultRenderer().getMimeType(resource);
			//DLNAResource dlna = res.get(0);
			WebRender render = (WebRender) defaultRenderer;
			MediaInfo media = resource.getMedia();
			if (media == null) {
				media = new MediaInfo();
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
							resource.setEngine(EngineFactory.getEngine(StandardEngineId.YOUTUBE_DL, false, false));
						} else {
							resource.setEngine(EngineFactory.getEngine(StandardEngineId.FFMPEG_WEB_VIDEO, false, false));
						}
					} else if (!(resource instanceof DVDISOTitle)) {
						resource.setEngine(EngineFactory.getEngine(StandardEngineId.FFMPEG_VIDEO, false, false));
					}
					//code = 206;
				}
				if (
					PMS.getConfiguration().getWebPlayerSubs() &&
					resource.getMediaSubtitle() != null &&
					resource.getMediaSubtitle().isExternal()
				) {
					// fetched on the side
					sid = resource.getMediaSubtitle();
					resource.setMediaSubtitle(null);
				}
			}

			if (!WebInterfaceServerUtil.directmime(mimeType) && resource.getFormat().isAudio()) {
				resource.setEngine(EngineFactory.getEngine(StandardEngineId.FFMPEG_AUDIO, false, false));
				code = 206;
			}
			if (resource.getFormat().isVideo() && render != null && HTTPResource.HLS_TYPEMIME.equals(render.getVideoMimeType())) {
				String uri = WebInterfaceServerUtil.getId(path, httpExchange);
				Headers headers = httpExchange.getResponseHeaders();
				headers.add("Server", PMS.get().getServerName());
				if (uri.endsWith("/chapters.vtt")) {
					String response = MediaChapter.getWebVtt(resource);
					WebInterfaceServerUtil.respond(httpExchange, response, 200, HTTPResource.WEBVTT_TYPEMIME);
				} else if (uri.endsWith("/chapters.json")) {
					String response = MediaChapter.getHls(resource);
					WebInterfaceServerUtil.respond(httpExchange, response, 200, HTTPResource.JSON_TYPEMIME);
				} else if (uri.contains("/hls/")) {
					if (uri.endsWith(".m3u8")) {
						String rendition = uri.substring(uri.indexOf("/hls/") + 5);
						rendition = rendition.replace(".m3u8", "");
						String response = HlsHelper.getHLSm3u8ForRendition(resource, root.getDefaultRenderer(), "/media/", rendition);
						WebInterfaceServerUtil.respond(httpExchange, response, 200, HTTPResource.HLS_TYPEMIME);
					} else {
						//we need to stream
						InputStream in = HlsHelper.getInputStream(uri, resource, defaultRenderer);
						if (in != null) {
							headers.add("Connection", "keep-alive");
							if (uri.endsWith(".ts")) {
								headers.add("Content-Type", HTTPResource.MPEGTS_BYTESTREAM_TYPEMIME);
							} else if (uri.endsWith(".vtt")) {
								headers.add("Content-Type", HTTPResource.WEBVTT_TYPEMIME);
							}
							OutputStream os = httpExchange.getResponseBody();
							httpExchange.sendResponseHeaders(200, 0); //chunked
							render.start(resource);
							if (LOGGER.isTraceEnabled()) {
								WebInterfaceServerUtil.logMessageSent(httpExchange, null, in);
							}
							WebInterfaceServerUtil.dump(in, os);
						} else {
							httpExchange.sendResponseHeaders(500, -1);
						}
					}
				} else {
					String response = HlsHelper.getHLSm3u8(resource, root.getDefaultRenderer(), "/media/");
					WebInterfaceServerUtil.respond(httpExchange, response, 200, HTTPResource.HLS_TYPEMIME);
				}
				return;
			}
			media.setMimeType(mimeType);
			ByteRange range = WebInterfaceServerUtil.parseRange(httpExchange.getRequestHeaders(), resource.length());
			LOGGER.debug("Sending {} with mime type {} to {}", resource, mimeType, renderer);
			InputStream in = resource.getInputStream(range, root.getDefaultRenderer());
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
			if (LOGGER.isTraceEnabled()) {
				WebInterfaceServerUtil.logMessageSent(httpExchange, null, in);
			}
			OutputStream os = httpExchange.getResponseBody();
			if (render != null) {
				render.start(resource);
			}
			if (sid != null) {
				resource.setMediaSubtitle(sid);
			}
			WebInterfaceServerUtil.dump(in, os);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in MediaHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

}
