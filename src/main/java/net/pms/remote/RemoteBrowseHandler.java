package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import net.pms.Messages;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private final static String CRLF = "\r\n";
	private RemoteWeb parent;

	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String mkBrowsePage(String id, HttpExchange t) throws IOException {
		String user = RemoteUtil.userName(t);
		RootFolder root = parent.getRoot(user, true, t);
		List<DLNAResource> res = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), null);

		// Media browser HTML
		StringBuilder sb          = new StringBuilder();
		StringBuilder foldersHtml = new StringBuilder();
		StringBuilder mediaHtml   = new StringBuilder();

		sb.append("<!DOCTYPE html>").append(CRLF);
			sb.append("<head>").append(CRLF);
				sb.append("<meta charset=\"utf-8\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/reset.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/web.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"icon\" href=\"/files/favicon.ico\" type=\"image/x-icon\">").append(CRLF);
				sb.append("<script src=\"/files/jquery.min.js\"></script>");
				sb.append("<script src=\"/files/jquery.ums.js\"></script>");
				sb.append("<title>Universal Media Server</title>").append(CRLF);
			sb.append("</head>").append(CRLF);
			sb.append("<body id=\"ContentPage\">").append(CRLF);
				sb.append("<div id=\"Container\">");
					sb.append("<div id=\"Menu\">");
						sb.append("<a href=\"/browse/0\" id=\"HomeButton\"></a>");
					sb.append("</div>");
					for (DLNAResource r : res) {
						LOGGER.debug("add res  "+r);
						String newId = r.getResourceId();
						String idForWeb = URLEncoder.encode(newId, "UTF-8");
						String thumb = "/thumb/" + idForWeb;
						String name = StringEscapeUtils.escapeHtml(r.resumeName());

						if (r.isFolder()) {
							// Do not display the transcode folder in the web interface
							if (!name.equals(Messages.getString("TranscodeVirtualFolder.0"))) {
								// The resource is a folder
								foldersHtml.append("<li>");
									foldersHtml.append("<a href=\"/browse/").append(idForWeb).append("\" title=\"").append(name).append("\">");
										foldersHtml.append("<span>").append(name).append("</span>");
									foldersHtml.append("</a>").append(CRLF);
								foldersHtml.append("</li>").append(CRLF);
							}
						} else {
							// The resource is a media file
							mediaHtml.append("<li>");
								mediaHtml.append("<a href=\"/play/").append(idForWeb).append("\" title=\"").append(name).append("\">");
									mediaHtml.append("<img src=\"").append(thumb).append("\" alt=\"").append(name).append("\">");
									mediaHtml.append("<span>").append(name).append("</span>");
								mediaHtml.append("</a>").append(CRLF);
							mediaHtml.append("</li>").append(CRLF);
						}
					}
					sb.append("<div id=\"FoldersContainer\"><div><ul id=\"Folders\">").append(foldersHtml).append("</ul></div></div>");
					if (mediaHtml.length() > 0) {
						sb.append("<ul id=\"Media\">").append(mediaHtml).append("</ul>");
					}
				sb.append("</div>");
			sb.append("</body>");
		sb.append("</html>");

		return sb.toString();
	}

	private void writePage(String response, HttpExchange t) throws IOException {
		LOGGER.debug("Write page " + response);
		t.sendResponseHeaders(200, response.length());
		try (OutputStream os = t.getResponseBody()) {
			os.write(response.getBytes());
		}
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("Got a browse request " + t.getRequestURI());
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String id = RemoteUtil.getId("browse/", t);
		LOGGER.debug("Found id " + id);
		String response = mkBrowsePage(id, t);
		writePage(response, t);
	}
}
