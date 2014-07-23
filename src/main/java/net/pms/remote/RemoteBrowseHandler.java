package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import net.pms.Messages;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.util.UMSUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private final static String CRLF = "\r\n";
	private RemoteWeb parent;

	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String getSearchStr(String query) {
		for (String p : query.split("&")) {
			String[] pair = p.split("=");
			if (pair[0].equalsIgnoreCase("str")) {
				if (pair.length > 1 && StringUtils.isNotEmpty(pair[1])) {
					return pair[1];
				}
			}
		}
		return null;
	}

	private String mkBrowsePage(String id, HttpExchange t) throws IOException {
		String user = RemoteUtil.userName(t);
		RootFolder root = parent.getRoot(user, true, t);
		String vars = t.getRequestURI().getQuery();
		String search = null;
		if (StringUtils.isNotEmpty(vars)) {
			search = getSearchStr(vars);
		}
		List<DLNAResource> res = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		if (StringUtils.isNotEmpty(search)) {
			UMSUtils.postSearch(res, search);
		}

		// Media browser HTML
		StringBuilder sb          = new StringBuilder();
		StringBuilder foldersHtml = new StringBuilder();
		StringBuilder mediaHtml   = new StringBuilder();

		boolean showFolders = false;
		boolean hasFile     = false;

		sb.append("<!DOCTYPE html>").append(CRLF);
			sb.append("<head>").append(CRLF);
				// this special (simple) script performs a reload
				// if we have been sent back here after a VVA
				sb.append("<script>if(typeof window.refresh!='undefined' && window.refresh){").append(CRLF);
				sb.append("window.refresh=false;window.location.reload();}</script>").append(CRLF);
				sb.append("<meta charset=\"utf-8\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/reset.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/web.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/web-narrow.css\" type=\"text/css\" media=\"screen and (max-width: 1080px)\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/web-wide.css\" type=\"text/css\" media=\"screen and (min-width: 1081px)\">").append(CRLF);
				sb.append("<link rel=\"icon\" href=\"/files/favicon.ico\" type=\"image/x-icon\">").append(CRLF);
				sb.append("<script src=\"/files/jquery.min.js\"></script>");
				sb.append("<script src=\"/files/jquery.ums.js\"></script>");
				sb.append("<script src=\"/bump/bump.js\"></script>");
				// simple prompt script for search folders
				sb.append("<script>function searchFun(url) {");
		        sb.append("var str=prompt(\"Enter search string:\");").append(CRLF);
				sb.append("if(str!=null){ window.location.replace(url+'?str='+str)}").append(CRLF);
				sb.append("return false;");
				sb.append("}</script>").append(CRLF);
				// script ends here
				sb.append("<title>Universal Media Server</title>").append(CRLF);
			sb.append("</head>").append(CRLF);
			sb.append("<body id=\"ContentPage\">").append(CRLF);
				sb.append("<div id=\"Container\">");
					for (DLNAResource r : res) {
						String newId = r.getResourceId();
						String idForWeb = URLEncoder.encode(newId, "UTF-8");
						String thumb = "/thumb/" + idForWeb;
						String name = StringEscapeUtils.escapeHtml(r.resumeName());

						if (r.isFolder()) {
							// The resource is a folder
							foldersHtml.append("<li>");
							String p = "/browse/" + idForWeb;
								if (r.getClass().getName().contains("SearchFolder")) {
									// search folder add a prompt
									// NOTE!!!
									// Yes doing getClass.getname is REALLY BAD, but this
									// is to make legacy plugins utilize this function as well
									foldersHtml.append("<a href=\"javascript:void(0);\" onclick=\"searchFun('").append(p).append("');\" title=\"").append(name).append("\">");
								} else {
									foldersHtml.append("<a href=\"/browse/").append(idForWeb).append("\" oncontextmenu=\"searchFun('").append(p).append("');\"title=\"").append(name).append("\">");
								}
								foldersHtml.append("<span>").append(name).append("</span>");
								foldersHtml.append("</a>").append(CRLF);
							foldersHtml.append("</li>").append(CRLF);
							showFolders = true;
						} else {
							// The resource is a media file
							mediaHtml.append("<li>");
								if (WebRender.supports(r)) {
									mediaHtml.append("<a href=\"/play/").append(idForWeb);
									mediaHtml.append("\" title=\"").append(name).append("\">");
									mediaHtml.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">");
									mediaHtml.append("<span>").append(name).append("</span>");
								} else if (upnpControl) {
									// Include it as a web-disabled item so it can be thrown via upnp
									mediaHtml.append("<a class=\"webdisabled\" href=\"javascript:alert('This item not playable via browser but can be sent to other renderers.')\"");
									mediaHtml.append("\" title=\"").append(name).append(" ** NOT PLAYABLE IN BROWSER - for other renderers only **\">");
									mediaHtml.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">");
									mediaHtml.append("<span>").append(name).append("</span>");
								}
								mediaHtml.append("</a>").append(CRLF);
								if (upnpControl) {
									mediaHtml.append("<a id=\"bumpicon\" href=\"javascript:bump.start('//")
										.append(parent.getAddress()).append("','/play/").append(idForWeb).append("','")
										.append(name.replace("'", "\\'")).append("')\" title=\"").append("Play on another renderer").append("\"></a>").append(CRLF);
								} else {
									mediaHtml.append("<a id=\"bumpicon\" class=\"icondisabled\" href=\"javascript:alert('").append("No upnp-controllable renderers suitable for receiving pushed media are available. Refresh this page if a new renderer may have recently connected.")
										.append("')\" title=\"No other renderers available\"></a>").append(CRLF);
								}
							mediaHtml.append("</li>").append(CRLF);

							hasFile = true;
						}
					}

					sb.append("<div id=\"Menu\">");
						sb.append("<a href=\"/browse/0\" id=\"HomeButton\"></a>");
						// Display the search form if the folder is populated
						if (hasFile) {
							sb.append("<form id=\"SearchForm\" method=\"get\">");
								sb.append("<input type=\"text\" id=\"SearchInput\" name=\"str\">");
								sb.append("<input type=\"submit\" id=\"SearchSubmit\" title=\"Search\" value=\"&nbsp;\">");
							sb.append("</form>");
						}
						sb.append("<a href=\"/doc\" id=\"DocButton\" title=\"Documentation\"></a>");
					sb.append("</div>");

					String noFoldersCSS = "";
					if (!showFolders) {
						noFoldersCSS = " class=\"noFolders\"";
					}
					sb.append("<div id=\"FoldersContainer\"").append(noFoldersCSS).append("><div><ul id=\"Folders\">").append(foldersHtml).append("</ul></div></div>");

					if (mediaHtml.length() > 0) {
						sb.append("<ul id=\"Media\"").append(noFoldersCSS).append(">").append(mediaHtml).append("</ul>");
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
		Headers hdr = t.getResponseHeaders();
		hdr.add("Content-Type", "text/html");
		writePage(response, t);
	}
}
