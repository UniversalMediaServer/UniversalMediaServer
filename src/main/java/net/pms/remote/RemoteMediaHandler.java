package net.pms.remote;

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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RemoteMediaHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMediaHandler.class);
	private final static String CRLF = "\r\n";
	
	private RemoteWeb parent;
	
	public RemoteMediaHandler(RemoteWeb parent) {
		this.parent = parent;
	}
	
	private Range nullRange() {
		return Range.create(0, 0, 0.0, 0.0);
	}
	
	private Range parseRange(Headers hdr, long len) {
		if (hdr == null) {
			return nullRange();
		}
		List<String> r = hdr.get("Range");
		LOGGER.debug("get range "+r);
		if (r == null) { // no range
			return nullRange();
		}
		// assume only one
		String range = r.get(0);
		LOGGER.debug("range str "+range);
		String[] tmp = range.split("=")[1].split("-");
        long start = Long.parseLong(tmp[0]);
        long end = tmp.length == 1 ?  len : Long.parseLong(tmp[1]);
        LOGGER.debug("start "+start+" end "+end);
        return Range.create(start, end, 0.0, 0.0);
	}
	
	@Override
	public void handle(HttpExchange t) throws IOException {
		RootFolder root = parent.getRoot(t.getPrincipal().getUsername());
		if(root == null) {
			throw new IOException("Unknown root");
		}
		String id = RemoteUtil.getId("media/", t);
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
		if (res.size() != 1) {
			// another error
			LOGGER.debug("media unkonwn");
			throw new IOException("Bad id");
		}
		long len = res.get(0).length();
		Range range = parseRange(t.getRequestHeaders(), len);
		Range.Byte rb = range.asByteRange();
		String mime = root.getDefaultRenderer().getMimeType(res.get(0).mimeType());
		LOGGER.debug("dumping media "+mime+" "+res);
		InputStream in = res.get(0).getInputStream(range, root.getDefaultRenderer());
		Headers hdr = t.getResponseHeaders();
		hdr.add("Content-Type" , mime);
		hdr.add("Accept-Ranges", "bytes");
		hdr.add("Server", PMS.get().getServerName());
		hdr.add("Connection", "keep-alive");
		if(in.available() != len) {
			hdr.add("Content-Range", "bytes " + rb.getStart() + "-"  + in.available() + "/" + len);
			t.sendResponseHeaders(206, in.available());
		}
		else {
			t.sendResponseHeaders(200, len);
		}
		OutputStream os = new BufferedOutputStream(t.getResponseBody(),512*1024);
		LOGGER.debug("input is "+in+" out "+os);
		RemoteUtil.dump(in,os);	
	}		
}
