package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.encoders.WebPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteMediaHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMediaHandler.class);
	private RemoteWeb parent;
	private String path;
	private RendererConfiguration render;

	public RemoteMediaHandler(RemoteWeb parent) {
		this(parent, "media/", null);
	}

	public RemoteMediaHandler(RemoteWeb parent, String path, RendererConfiguration render) {
		this.parent = parent;
		this.path = path;
		this.render = render;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		RootFolder root = parent.getRoot(t.getPrincipal().getUsername());
		if (root == null) {
			throw new IOException("Unknown root");
		}
		String id = RemoteUtil.getId(path, t);
		id = RemoteUtil.strip(id);
		RendererConfiguration r = render;
		if (render == null) {
			r = root.getDefaultRenderer();
		}
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, r);
		if (res.size() != 1) {
			// another error
			LOGGER.debug("media unkonwn");
			throw new IOException("Bad id");
		}
		long len = res.get(0).length();
		Range range = RemoteUtil.parseRange(t.getRequestHeaders(), len);
		String mime = root.getDefaultRenderer().getMimeType(res.get(0).mimeType());
        DLNAResource dlna = res.get(0);
		if (dlna.getFormat().isVideo()) {
            if (!RemoteUtil.directmime(mime) || (dlna.getMediaSubtitle() != null)) {
			    mime = "video/ogg";
			    dlna.setPlayer(new WebPlayer());
            }
		}
		LOGGER.debug("dumping media " + mime + " " + res);
		InputStream in = dlna.getInputStream(range, root.getDefaultRenderer());
		Headers hdr = t.getResponseHeaders();
		hdr.add("Content-Type", mime);
		hdr.add("Accept-Ranges", "bytes");
		hdr.add("Server", PMS.get().getServerName());
		hdr.add("Connection", "keep-alive");
		t.sendResponseHeaders(200, 0);
		OutputStream os = t.getResponseBody();
		RemoteUtil.dump(in, os);
	}
}
