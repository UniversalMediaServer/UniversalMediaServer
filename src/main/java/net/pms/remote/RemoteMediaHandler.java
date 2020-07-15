package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.*;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.StandardPlayerId;
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
			//Restrict access to user
			if (RemoteUtil.deny(httpExchange)) {
				throw new IOException("Access denied");
			}

			//Get "root" folder for user
			final RootFolder root = parent.getRoot(RemoteUtil.userName(httpExchange), httpExchange);
			if (root == null) {
				throw new IOException("Unknown root");
			}

			//Get requested headers
			Headers h = httpExchange.getRequestHeaders();
			for (String h1 : h.keySet()) {
				LOGGER.debug("key " + h1 + "=" + h.get(h1));
			}

			//Return id of media pr. user
			final String id = RemoteUtil.strip(RemoteUtil.getId(path, httpExchange));

			//Get resource or generate if missing
			Map.Entry<DLNAResource,Long> enry = this.parent.mediaResources.computeIfAbsent(id, s -> {
				//Get the renderer
				RendererConfiguration defaultRenderer = renderer;
				if (renderer == null) {
					defaultRenderer = root.getDefaultRenderer();
				}

				//Get the DLNA resource
				DLNAResource resource = root.getDLNAResource(id, defaultRenderer);
				if (resource == null) {
					// another error
					LOGGER.debug("media unknown");
					return null;
					//throw new IOException("Bad id");
				}
				if (!resource.isCodeValid(resource)) {
					LOGGER.debug("coded object with invalid code");
					return null;
					//throw new IOException("Bad code");
				}

				DLNAMediaSubtitle sid = null;
				String mimeType = defaultRenderer.getMimeType(resource);
				//DLNAResource dlna = res.get(0);
				WebRender renderer = (WebRender)defaultRenderer;
				DLNAMediaInfo media = resource.getMedia();

				if (media == null) {
					media = new DLNAMediaInfo();
					resource.setMedia(media);
				}

				if (mimeType.equals(FormatConfiguration.MIMETYPE_AUTO) && media.getMimeType() != null) {
					mimeType = media.getMimeType();
				}

				//int code = 200;
				resource.setDefaultRenderer(defaultRenderer);

				//Input type configurations
				if (resource.getFormat().isVideo()) {
					if (flash) {
						mimeType = "video/flash";
					} else if (!RemoteUtil.directmime(mimeType) || RemoteUtil.transMp4(mimeType, media)) {
						mimeType = renderer != null ? renderer.getVideoMimeType() : RemoteUtil.transMime();
						if (FileUtil.isUrl(resource.getSystemName())) {
							resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_WEB_VIDEO, false, false));
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

				if (!RemoteUtil.directmime(mimeType) && resource.getFormat().isAudio()) {
					resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_AUDIO, false, false));
					//code = 206;
				}

				media.setMimeType(mimeType);

				if (renderer != null) {
					renderer.start(resource);
				}
				if (sid != null) {
					resource.setMediaSubtitle(sid);
				}

				long sec = System.currentTimeMillis() / 1000L;
				return new SimpleEntry<DLNAResource,Long>(resource, sec);
			});

			if (enry == null) {
				throw new IOException();
			}

			DLNAResource res = enry.getKey();

			int code = 200;

			//Get mimeType and renderer
			String mimeType = res.getMedia().getMimeType();
			WebRender renderer = (WebRender)res.getDefaultRenderer();

			//Get input stream starting from specified offset given by Range header
			Range.Byte range = RemoteUtil.parseRange(httpExchange.getRequestHeaders(), res.length());
			LOGGER.debug("Sending {} with mime type {} to {}", res, mimeType, renderer);

			InputStream in = res.getInputStream(range, res.getDefaultRenderer()); //Note: MUST BE res.getDefaultRenderer(), its perceived as live which only works in chrome

			//This should never be 0 unless the client specifies it, why is this here?
			if(range.getEnd() == 0) {
				// For web resources actual length may be unknown until we open the stream
				range.setEnd(res.length());
			}

			//Get default response headers
			Headers headers = httpExchange.getResponseHeaders();
			headers.add("Content-Type", mimeType);
			headers.add("Accept-Ranges", "bytes");
			headers.add("Server", PMS.get().getServerName());

			//Specify range
			long end = range.getEnd();
			long start = range.getStart();

			if (end == res.length()) {
				LOGGER.debug("Browser wants data stream!");
				String rStr = start + "-" + end + "/*";
				headers.add("Content-Range", "bytes " + rStr);
				headers.add("Connection", "keep-alive");

				httpExchange.sendResponseHeaders(code, 0);
				OutputStream os = httpExchange.getResponseBody();
				RemoteUtil.dump(in, os , null, false);
			} else {
				//Note: We're NEVER allowed to change the length, so we need to supply some absurd length
				//res.length() is too big, break safari
				//in.available() is not static, break safari
				//2912060230L seems to work, but safari scans the file for its length, 
				//meaning its gonna find how far the transcoder has gone, and leave it there, thus not working.
				//"*", break safari
				String length = "*";
				LOGGER.debug("Browser wants byte range! {}-{}/{}",start,end,length);

				code = 206;
				if (start > end || end > in.available()) {
					code = 416;
					end = end > in.available() ? in.available() : end;
					start = end < start ? end : start;
				}

				String rStr = start + "-" + end + "/" + length;
				headers.add("Content-Range", "bytes " + rStr);

				int reqlength = (int)(end-start)+1;
				headers.add("Content-Length", "" + reqlength);

				httpExchange.sendResponseHeaders(code, 0);
				OutputStream os = httpExchange.getResponseBody();
				RemoteUtil.dumpLimit(in, os, null, reqlength);
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteMediaHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
