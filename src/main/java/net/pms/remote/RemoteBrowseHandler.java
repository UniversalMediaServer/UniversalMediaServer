package net.pms.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.util.PropertiesUtil;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpPrincipal;

public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private final static String CRLF = "\r\n";
	
	private RemoteWeb parent;
	
	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}
	
	private String mkBrowsePage(String id,HttpExchange t) throws IOException {
		HttpPrincipal p = t.getPrincipal();
		RootFolder root = parent.getRoot(p.getUsername(), true);
    	List<DLNAResource> res = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), null);
		StringBuilder sb = new StringBuilder();
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:og=\"http://opengraphprotocol.org/schema/\">");
		sb.append(CRLF);
		sb.append("<link rel=\"stylesheet\" href=\"");
		sb.append("http://swesub.nu/css/style.css");
		sb.append("\">");
		sb.append(CRLF);
		sb.append("<head>");
		sb.append(CRLF);
		sb.append("<meta charset=\"utf-8\">");
		sb.append(CRLF);
		sb.append("<title>");
		sb.append(PropertiesUtil.getProjectProperties().get("project.name") + " " + PMS.getVersion());
		sb.append("</title></head><body>");
		sb.append(CRLF);
		sb.append("<div class=\"subtitles cover left\">");
		sb.append("<ul>");
		sb.append(CRLF);
		for (DLNAResource r : res) {
			String newId = r.getResourceId();
			String thumb = "/thumb/" + newId;
			String path = "/browse/";
			if (!r.isFolder()) {
				path = "/play/";
				//newId = newId + "." + r.getFormat().getMatchedId();
			}
			sb.append("<li>");
			sb.append("<a href=\"" + path + newId + "\"");
			sb.append(" title=\"" + r.getDisplayName() + "\">");
			sb.append("<img class=\"cover\" src=\"" + thumb + "\" alt=\"\" />");
			sb.append("<br><span class=\"ep\">");
			sb.append(r.getDisplayName());
			sb.append("</span>");
			sb.append("</a></li>");
			sb.append(CRLF);
		}
		sb.append("</ul></div></body></html>");
		sb.append(CRLF);
		return sb.toString();
	}
	
	private void writePage(String response, HttpExchange t) throws IOException {
		LOGGER.debug("write page "+response);
        t.sendResponseHeaders(200, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
	}
	
    public void handle(HttpExchange t) throws IOException {
    	LOGGER.debug("got a browse request "+t.getRequestURI());
    	if(RemoteUtil.deny(t)) {
    		throw new IOException("Access denied");
    	}
    	String id = RemoteUtil.getId("browse/", t);
    	LOGGER.debug("found id "+id);
    	String response = mkBrowsePage(id,t);
    	writePage(response,t);
    }
}
