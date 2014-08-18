package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteRawHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRawHandler.class);
	private RemoteWeb parent;

	public RemoteRawHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("got a raw request " + t.getRequestURI());
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
		if (root == null) {
			throw new IOException("Unknown root");
		}
		String id;
		id = RemoteUtil.strip(RemoteUtil.getId("raw/", t));
		LOGGER.debug("raw id " + id);
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
		if (res.size() != 1) {
			// another error
			LOGGER.debug("media unkonwn");
			throw new IOException("Bad id");
		}
		DLNAResource dlna = res.get(0);
		long len = dlna.length();
		dlna.setPlayer(null);
		Range range = RemoteUtil.parseRange(t.getRequestHeaders(), len);
		Range.Byte rb = range.asByteRange();
		InputStream in = dlna.getInputStream(range, root.getDefaultRenderer());
		String mime = root.getDefaultRenderer().getMimeType(dlna.mimeType());
		Headers hdr = t.getResponseHeaders();
		LOGGER.debug("dumping media " + mime + " " + dlna);
		hdr.add("Content-Type", mime);
		hdr.add("Accept-Ranges", "bytes");
		hdr.add("Server", PMS.get().getServerName());
		hdr.add("Connection", "keep-alive");
		hdr.add("Transfer-Encoding", "chunked");
		if (in.available() != len) {
			hdr.add("Content-Range", "bytes " + rb.getStart() + "-" + in.available() + "/" + len);
			t.sendResponseHeaders(206, in.available());
		} else {
			t.sendResponseHeaders(200, 0);
		}
		OutputStream os = new BufferedOutputStream(t.getResponseBody(), 512 * 1024);
		LOGGER.debug("start raw dump");
		RemoteUtil.dump(in, os);
	}
}
