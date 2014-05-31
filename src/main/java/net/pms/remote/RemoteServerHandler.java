package net.pms.remote;


import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

public class RemoteServerHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteServerHandler.class);
	private final static String CRLF = "\r\n";
	private RemoteWeb parent;

	public RemoteServerHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String jsonkeyVal(String key,String val) {
		// we need some special escapes here
		key = StringEscapeUtils.escapeJava(key);
		val = StringEscapeUtils.escapeJava(val);
		return "\""+key+"\":\""+val+"\"";
	}

	private void writePage(String response, HttpExchange t) throws IOException {
		LOGGER.debug("Write page " + response);
		t.sendResponseHeaders(200, response.length());
		try (OutputStream os = t.getResponseBody()) {
			os.write(response.getBytes());
		}
	}

	private void playMedia(String id, HttpExchange t, RootFolder root) throws IOException {
		RendererConfiguration render = RendererConfiguration.getDefaultConf();
		//RendererConfiguration render =  root.getDefaultRenderer();
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, render);
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
		InputStream in = dlna.getInputStream(range, render);
		Headers hdr = t.getResponseHeaders();
		hdr.add("Accept-Ranges", "bytes");
		hdr.add("Server", PMS.get().getServerName());
		hdr.add("Connection", "keep-alive");
		hdr.add("Content-Type", dlna.mimeType());
		t.sendResponseHeaders(200, 0);
		OutputStream os = new BufferedOutputStream(t.getResponseBody(), 512 * 1024);
		LOGGER.debug("start raw dump");
		//RemoteUtil.dump(in, os);
		RemoteUtil.dumpNoThread(in, os, null);
	}


	public void handle(HttpExchange t) throws IOException {
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String id = RemoteUtil.getId("srv/", t);
		LOGGER.debug("Found id " + id);

		if(id.startsWith("thumb/")) {
			RemoteUtil.fetchAndSendThumb(t, parent,  id.substring(6));
			return;
		}
		String user = RemoteUtil.userName(t);
		RootFolder root = parent.getRoot(user, true, t);
		if(id.startsWith("media/")) {
			playMedia(id.substring(6),t, root);
			return;
		}
		List<DLNAResource> res = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), null);

		StringBuilder sb          = new StringBuilder();
		StringBuilder folders = new StringBuilder();
		StringBuilder media   = new StringBuilder();

		for (DLNAResource r : res) {
			LOGGER.debug("add res  "+r);
			String newId = r.getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8");
			String thumb = "thumb/" + idForWeb;
			String name = StringEscapeUtils.escapeHtml(r.resumeName());

			if (r.isFolder()) {
				// Do not display the transcode folder in the remote servers
				if (name.equals(Messages.getString("TranscodeVirtualFolder.0"))) {
					continue;
				}
				// The resource is a folder
				String json = jsonkeyVal("name", name)+","+jsonkeyVal("id", idForWeb)+
						"," + jsonkeyVal("thumb", thumb);
				folders.append("{" + json + "},");
			} else {
				// The resource is a media file
				String fmtCls = r.getFormat().getClass().getCanonicalName();
				String json = jsonkeyVal("name", name)+","+jsonkeyVal("id",idForWeb)+","+
							  jsonkeyVal("thumb",thumb)+","+jsonkeyVal("fmt",fmtCls);
				media.append("{" + json + "},");
			}
		}
		sb.append("{\"folders\":["+folders.toString()+"],");
		sb.append("\"media\":["+media.toString()+"]}");
		writePage(sb.toString(), t);
	}

}
