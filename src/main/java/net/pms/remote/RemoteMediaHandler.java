package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.*;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.FFmpegAudio;
import net.pms.encoders.FFmpegWebVideo;
import net.pms.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RemoteMediaHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMediaHandler.class);
	private RemoteWeb parent;
	private String path;
	private RendererConfiguration renderer;
	private boolean flash;

	public RemoteMediaHandler(RemoteWeb parent) {
		this(parent, "media/", null);
	}

	public RemoteMediaHandler(RemoteWeb parent, boolean flash) {
		this(parent, "fmedia/", null);
		this.flash = flash;
	}

	public RemoteMediaHandler(RemoteWeb parent, String path, RendererConfiguration renderer) {
		this.flash = false;
		this.parent = parent;
		this.path = path;
		this.renderer = renderer;
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		try {
			if (RemoteUtil.deny(httpExchange)) {
				throw new IOException("Access denied");
			}
			RootFolder root = parent.getRoot(RemoteUtil.userName(httpExchange), httpExchange);
			if (root == null) {
				throw new IOException("Unknown root");
			}
			Headers h = httpExchange.getRequestHeaders();
			for (String h1 : h.keySet()) {
				LOGGER.debug("key " + h1 + "=" + h.get(h1));
			}
			String id = RemoteUtil.getId(path, httpExchange);
			id = RemoteUtil.strip(id);
			RendererConfiguration defaultRenderer = renderer;
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
			DLNAMediaSubtitle sid = null;
			String mimeType = root.getDefaultRenderer().getMimeType(resource.mimeType(), resource.getMedia());
			//DLNAResource dlna = res.get(0);
			WebRender renderer = (WebRender) defaultRenderer;
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
				} else if (!RemoteUtil.directmime(mimeType) || RemoteUtil.transMp4(mimeType, media)) {
					mimeType = renderer != null ? renderer.getVideoMimeType() : RemoteUtil.transMime();
					if (FileUtil.isUrl(resource.getSystemName())) {
						resource.setPlayer(new FFmpegWebVideo());
					} else if (!(resource instanceof DVDISOTitle)) {
						resource.setPlayer(new FFMpegVideo());
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

			if (!RemoteUtil.directmime(mimeType) && resource.getFormat().isAudio()) {
				resource.setPlayer(new FFmpegAudio());
				code = 206;
			}

			media.setMimeType(mimeType);
			Range.Byte range = RemoteUtil.parseRange(httpExchange.getRequestHeaders(), resource.length());
			LOGGER.debug("Sending {} with mime type {} to {}", resource, mimeType, renderer);
			InputStream in = resource.getInputStream(range, root.getDefaultRenderer());
			if(range.getEnd() == 0) {
				// For web resources actual length may be unknown until we open the stream
				range.setEnd(resource.length());
			}
			Headers headers = httpExchange.getResponseHeaders();
			headers.add("Content-Type", mimeType);
			headers.add("Accept-Ranges", "bytes");
			long end = range.getEnd();
			long start = range.getStart();
			String rStr = start + "-" + end + "/*" ;
			headers.add("Content-Range", "bytes " + rStr);
			if (start != 0) {
				code = 206;
			}

			headers.add("Server", PMS.get().getServerName());
			headers.add("Connection", "keep-alive");
			httpExchange.sendResponseHeaders(code, 0);
			OutputStream os = httpExchange.getResponseBody();
			if (renderer != null) {
				renderer.start(resource);
			}
			if (sid != null) {
				resource.setMediaSubtitle(sid);
			}
			RemoteUtil.dump(in, os, renderer);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteMediaHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
