package net.pms.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
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
	
	private String mkPage(String id) {
		LOGGER.debug("make play page "+id);
		String id1 = id;
		int pos = id.lastIndexOf(".");
		if(pos != -1)
			id1 = id.substring(0, pos);
		StringBuilder sb=new StringBuilder();
		sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:og=\"http://opengraphprotocol.org/schema/\">");
		sb.append(CRLF);
		sb.append("<head>");
		/*sb.append("<script type=\"text/javascript\" ");
		//sb.append("src=\"/files/jwplayer.js\">");
		sb.append("src=\"http://www.longtailvideo.com/jwplayer/jwplayer.js\">");
		sb.append("</script>");*/
		sb.append("</head>");
		sb.append("<body>");
		/*sb.append("<script type=\"text/javascript\">");
		sb.append(CRLF);
		sb.append("jwplayer('container').setup({");
		//sb.append("'flashplayer': '/files/player.swf',");
		sb.append("'flashplayer': 'http://player.longtailvideo.com/player.swf',");
		sb.append(CRLF);
		sb.append("'file': '/media/" + id1 + "',");
		sb.append(CRLF);
		sb.append("'image': '/thumb/"+ id1 +"',");
		sb.append("'id': 'playerID',");
		sb.append("'autostart' : true");
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
		sb.append("<video id=\"video\" width=\"320\" height=\"240\" controls=\"controls\"");
		sb.append(" src=\"/media/" + id1 +"\" type=\"video/mp4\">");
		sb.append("Your browser doesn't appear to support the HTML5 video tag");
		sb.append("</video><br>");
		sb.append("<a href=\"/raw/" + id + "\" target=\"_blank\">Download</a>");
		sb.append(CRLF);
		sb.append("</body></html>");
		return sb.toString();
	}

	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("got a play equest "+t.getRequestURI());
		String id = "0";
		id = RemoteUtil.getId("play/", t);
		String response = mkPage(id);
		LOGGER.debug("play page "+response);
		t.sendResponseHeaders(200, response.length());
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
}
