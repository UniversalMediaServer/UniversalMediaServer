package net.pms.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RemotePlayHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemotePlayHandler.class);
	private final static String CRLF = "\r\n";

	private RemoteWeb parent;
	
	public RemotePlayHandler(RemoteWeb parent) {
		this.parent = parent;
	}
	
	private String mkPage(String id,HttpExchange t) throws IOException {
		LOGGER.debug("make play page "+id);
		RootFolder root = parent.getRoot(t.getPrincipal().getUsername());
		if(root == null) {
			throw new IOException("Unknown root");
		}
		String id1 = id;
		/*int pos = id.lastIndexOf(".");
		if(pos != -1)
			id1 = id.substring(0, pos);*/
		StringBuilder sb=new StringBuilder();
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
//		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:og=\"http://opengraphprotocol.org/schema/\">");
		sb.append("<html>");
		sb.append(CRLF);
		sb.append("<head>");
		sb.append("<link rel=\"stylesheet\" href=\"");
		sb.append("http://swesub.nu/css/style.css");
		sb.append("\">");
		/*sb.append("<script type='text/javascript' ");
		sb.append("src=\"/files/jwplayer.js\">");
		//sb.append("src=\"http://www.longtailvideo.com/jwplayer/jwplayer.js\">");
		sb.append("</script>");*/
		sb.append("</head>");
		sb.append("<body>");
		/*String x="<script type=\"text/javascript\" src=\"/jwplayer/jwplayer.js\"></script>"+
		"<video class=\"jwplayer\" src=\"";
		sb.append(x+"/media/"+id1+"\"></video>");*/
/*		sb.append("<script type='text/javascript'>");
		sb.append(CRLF);
		sb.append("jwplayer('container').setup({");
		sb.append("'flashplayer': 'player.swf',");
		//sb.append("'flashplayer': 'http://player.longtailvideo.com/player.swf',");
		sb.append(CRLF);
		sb.append("'file': '/media/" + id1 + "',");
		sb.append(CRLF);
		sb.append("'controlbar': 'bottom',");
		//sb.append("'autostart' : true");
		sb.append("'height': 270,");
		sb.append(CRLF);
		sb.append("'width': 480");
		sb.append(CRLF);
		sb.append(")});");
		sb.append(CRLF);
		sb.append("</script>");
	    sb.append(CRLF);
	    sb.append("<div id='container'>Loading player....</div><br>");*/
	    //sb.append("<br><ul><li onclick='jwplayer().play()'>Start the player</li></ul><br>");
		String mime = root.getDefaultRenderer().getMimeType(res.get(0).mimeType());
		String mediaType="";
		if(res.get(0).getFormat().isAudio()) {
			mediaType="audio";
			String thumb = "/thumb/" + id1;
			sb.append("<img class=\"cover\" src=\"" + thumb + "\" alt=\"\" /><br>");
		}
		if(res.get(0).getFormat().isVideo()) {
			mediaType="video";
		}
		sb.append("<" + mediaType + " width=\"320\" height=\"240\" autoplay=\"autoplay\" controls=\"controls\"");
		sb.append(" src=\"/media/" + id1 + "\" type=\"" + mime + "\">");
		sb.append("Your browser doesn't appear to support the HTML5 video tag");
		sb.append("</" + mediaType +"><br>");
		List<DLNAResource> res1 = root.getDLNAResources(id, false, 0, 0, RendererConfiguration.getDefaultConf());
		String rawId = id + "." + res1.get(0).getFormat().getMatchedId();
		sb.append("<a href=\"/raw/" + rawId + "\" target=\"_blank\">Download</a>");
		sb.append(CRLF);
		sb.append("</body></html>");
		return sb.toString();
	}

	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("got a play equest "+t.getRequestURI());
		String id = "0";
		id = RemoteUtil.getId("play/", t);
		String response = mkPage(id,t);
		LOGGER.debug("play page "+response);
		t.sendResponseHeaders(200, response.length());
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
