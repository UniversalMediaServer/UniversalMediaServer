package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePlayHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemotePlayHandler.class);
	private final static String CRLF = "\r\n";
	private RemoteWeb parent;

	public RemotePlayHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String mkPage(String id, HttpExchange t) throws IOException {
		LOGGER.debug("make play page " + id);
		RootFolder root = parent.getRoot(t.getPrincipal().getUsername());
		if (root == null) {
			throw new IOException("Unknown root");
		}
		String id1 = id;
		StringBuilder sb = new StringBuilder();
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
		sb.append("<!DOCTYPE html>");
		sb.append(CRLF);
		sb.append("<head>");
		sb.append("<link rel=\"stylesheet\" href=\"");
		sb.append("/file/web.css");
		sb.append("\">");
		sb.append("</head>");
		sb.append("<body>");
		String mime = root.getDefaultRenderer().getMimeType(res.get(0).mimeType());
		String mediaType = "";
		if (res.get(0).getFormat().isAudio()) {
			mediaType = "audio";
			String thumb = "/thumb/" + id1;
			sb.append("<img class=\"cover\" src=\"").append(thumb).append("\" alt=\"\" /><br>");
		}

		if (res.get(0).getFormat().isVideo()) {
			mediaType = "video";
			mime = "video/ogg";
		}

		sb.append("<").append(mediaType).append(" width=\"720\" height=\"404\" controls=\"controls\" autoplay=\"autoplay\"");
		sb.append(" src=\"/media/").append(id1).append("\" type=\"").append(mime).append("\">");
		sb.append("Your browser doesn't appear to support the HTML5 video tag");
		sb.append("</").append(mediaType).append("><br><br>");
		String rawId = id;
		sb.append("<a href=\"/raw/").append(rawId).append("\" target=\"_blank\">Download</a>");
		sb.append(CRLF);
		sb.append("</body></html>");
		return sb.toString();
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("got a play request " + t.getRequestURI());
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String id;
		id = RemoteUtil.getId("play/", t);
		String response = mkPage(id, t);
		LOGGER.debug("play page " + response);
		t.sendResponseHeaders(200, response.length());
		try (OutputStream os = t.getResponseBody()) {
			os.write(response.getBytes());
		}
	}
}
