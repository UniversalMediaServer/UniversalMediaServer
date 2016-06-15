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

public class RemoteMediaHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMediaHandler.class);
	private RemoteWeb parent;
	private String path;
	private RendererConfiguration render;
	private boolean flash;

	public RemoteMediaHandler(RemoteWeb parent) {
		this(parent, "media/", null);
	}

	public RemoteMediaHandler(RemoteWeb parent, boolean flash) {
		this(parent, "fmedia/", null);
		this.flash = flash;
	}

	public RemoteMediaHandler(RemoteWeb parent, String path, RendererConfiguration render) {
		this.flash = false;
		this.parent = parent;
		this.path = path;
		this.render = render;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
		if (root == null) {
			throw new IOException("Unknown root");
		}
		Headers h = t.getRequestHeaders();
		for (String h1 : h.keySet()) {
			LOGGER.debug("key " + h1 + "=" + h.get(h1));
		}
		String id = RemoteUtil.getId(path, t);
		id = RemoteUtil.strip(id);
		RendererConfiguration r = render;
		if (render == null) {
			r = root.getDefaultRenderer();
		}
		DLNAResource dlna = root.getDLNAResource(id, r);
		if (dlna == null) {
			// another error
			LOGGER.debug("media unkonwn");
			throw new IOException("Bad id");
		}
		if (!dlna.isCodeValid(dlna)) {
			LOGGER.debug("coded object with invalid code");
			throw new IOException("Bad code");
		}
		DLNAMediaSubtitle sid = null;
		String mime = root.getDefaultRenderer().getMimeType(dlna.mimeType(), dlna.getMedia());
		//DLNAResource dlna = res.get(0);
		WebRender render = (WebRender) r;
		DLNAMediaInfo m = dlna.getMedia();
		if (m == null) {
			m = new DLNAMediaInfo();
			dlna.setMedia(m);
		}
		if (mime.equals(FormatConfiguration.MIMETYPE_AUTO) && m.getMimeType() != null) {
			mime = m.getMimeType();
		}
		int code = 200;
		dlna.setDefaultRenderer(r);
		if (dlna.getFormat().isVideo()) {
			if (flash) {
				mime = "video/flash";
			} else if (!RemoteUtil.directmime(mime) || RemoteUtil.transMp4(mime, m)) {
				mime = render != null ? render.getVideoMimeType() : RemoteUtil.transMime();
				if (FileUtil.isUrl(dlna.getSystemName())) {
					dlna.setPlayer(new FFmpegWebVideo());
				} else {
					dlna.setPlayer(new FFMpegVideo());
				}
				//code = 206;
			}
			if (
				PMS.getConfiguration().getWebSubs() &&
				dlna.getMediaSubtitle() != null &&
				dlna.getMediaSubtitle().isExternal()
			) {
				// fetched on the side
				sid = dlna.getMediaSubtitle();
				dlna.setMediaSubtitle(null);
			}
		}

		if (!RemoteUtil.directmime(mime) && dlna.getFormat().isAudio()) {
			dlna.setPlayer(new FFmpegAudio());
			code = 206;
		}

		m.setMimeType(mime);
		Range.Byte range = RemoteUtil.parseRange(t.getRequestHeaders(), dlna.length());
		LOGGER.debug("dumping media " + mime + " " + dlna);
		InputStream in = dlna.getInputStream(range, root.getDefaultRenderer());
		if(range.getEnd() == 0) {
			// For web resources actual length may be unknown until we open the stream
			range.setEnd(dlna.length());
		}
		Headers hdr = t.getResponseHeaders();
		hdr.add("Content-Type", mime);
		hdr.add("Accept-Ranges", "bytes");
		if (range != null) {
			long end = range.getEnd();
			long start = range.getStart();
			String rStr = start + "-" + end + "/*" ;
			hdr.add("Content-Range", "bytes " + rStr);
			if (start != 0) {
				code = 206;
			}

		}
		hdr.add("Server", PMS.get().getServerName());
		hdr.add("Connection", "keep-alive");
		t.sendResponseHeaders(code, 0);
		OutputStream os = t.getResponseBody();
		render.start(dlna);
		if (sid != null) {
			dlna.setMediaSubtitle(sid);
		}
		RemoteUtil.dump(in, os, render);
	}
}
