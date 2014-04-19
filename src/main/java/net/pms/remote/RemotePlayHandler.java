package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.Player;
import net.pms.formats.v2.SubtitleUtils;
import net.pms.io.OutputParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePlayHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemotePlayHandler.class);
	private final static String CRLF = "\r\n";
	private RemoteWeb parent;
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public RemotePlayHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private void endPage(StringBuilder sb) {
		sb.append("</div>").append(CRLF);
		sb.append("</div>").append(CRLF);
		sb.append("</body>").append(CRLF);
		sb.append("</html>").append(CRLF);
	}

	private String mkPage(String id, HttpExchange t) throws IOException {
		boolean flowplayer = true;

		LOGGER.debug("make play page " + id);
		RootFolder root = parent.getRoot(RemoteUtil.userName(t));
		if (root == null) {
			throw new IOException("Unknown root");
		}
		String id1 = id;
		List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
		String rawId = id;

		DLNAResource r = res.get(0);
		String mime = root.getDefaultRenderer().getMimeType(r.mimeType());
		String mediaType = "";
		String coverImage = "";
		if(r.getFormat().isImage()) {
			flowplayer = false;
			coverImage = "<img src=\"/raw/" + rawId + "\" alt=\"\"><br>";
		}
		if (r.getFormat().isAudio()) {
			mediaType = "audio";
			String thumb = "/thumb/" + id1;
			coverImage = "<img src=\"" + thumb + "\" alt=\"\"><br>";
			flowplayer = false;
		}
		if (r.getFormat().isVideo()) {
			mediaType = "video";
			if(mime.equals(FormatConfiguration.MIMETYPE_AUTO)) {
				if (r.getMedia() != null && r.getMedia().getMimeType() != null) {
					mime = r.getMedia().getMimeType();
				}
			}
		}

		// Media player HTML
		StringBuilder sb = new StringBuilder();
		sb.append("<!DOCTYPE html>").append(CRLF);
			sb.append("<head>").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/reset.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"stylesheet\" href=\"/files/web.css\" type=\"text/css\" media=\"screen\">").append(CRLF);
				sb.append("<link rel=\"icon\" href=\"/files/favicon.ico\" type=\"image/x-icon\">").append(CRLF);
				sb.append("<title>Universal Media Server</title>").append(CRLF);
				if (flowplayer) {
					sb.append("<script src=\"/files/jquery.min.js\"></script>").append(CRLF);
					//sb.append("<script src=\"/files/flowplayer.min.js\"></script>").append(CRLF);
					sb.append("<script src=\"//releases.flowplayer.org/5.4.6/flowplayer.min.js\"></script>").append(CRLF);
					sb.append("<link rel=\"stylesheet\" href=\"/files/functional.css\">").append(CRLF);
				}
			sb.append("</head>").append(CRLF);
			sb.append("<body id=\"ContentPage\">").append(CRLF);
				sb.append("<div id=\"Container\">").append(CRLF);
					sb.append("<div id=\"Menu\">").append(CRLF);
						sb.append("<a href=\"/browse/0\" id=\"HomeButton\"></a>").append(CRLF);
					sb.append("</div>").append(CRLF);
					sb.append("<div id=\"VideoContainer\">").append(CRLF);
					// for video this gives just an empty line
					sb.append(coverImage).append(CRLF);
					if(r.getFormat().isImage()) {
						// do this like this to simplify the code
						// skip all player crap since img tag works well
						endPage(sb);
						return sb.toString();
					}

					if (flowplayer) {
						sb.append("<div class=\"flowplayer no-time no-volume no-mute\" data-ratio=\"0.5625\" data-embed=\"false\" data-flashfit=\"true\">").append(CRLF);
					}
					sb.append("<").append(mediaType);
					if (flowplayer) {
						sb.append(" controls autoplay>").append(CRLF);
						if(RemoteUtil.directmime(mime) && !transMp4(mime, r.getMedia())) {
							sb.append("<source src=\"/media/").append(URLEncoder.encode(id1, "UTF-8")).
							   append("\" type=\"").append(mime).append("\">").append(CRLF);
						}
						sb.append("<source src=\"/fmedia/").append(URLEncoder.encode(id1, "UTF-8")).
					 	   append("\" type=\"video/flash\">");
					} else {
						sb.append(" width=\"720\" height=\"404\" controls autoplay>").append(CRLF);
						sb.append("<source src=\"/media/").append(URLEncoder.encode(id1, "UTF-8")).append("\" type=\"").append(mime).append("\">");
					}
					sb.append(CRLF);

					if (flowplayer) {
						boolean isFFmpegFontConfig = configuration.isFFmpegFontConfig();
						if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
							configuration.setFFmpegFontConfig(false);
						}

						OutputParams p = new OutputParams(configuration);
						p.aid = r.getMediaAudio();
						p.sid = r.getMediaSubtitle();
						p.header = r.getHeaders();
						configuration.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
					}
					sb.append("</").append(mediaType).append(">").append(CRLF);

					if (flowplayer) {
						sb.append("</div>").append(CRLF);
					}
		sb.append("</div>").append(CRLF);
		sb.append("<a href=\"/raw/").append(rawId).append("\" target=\"_blank\" id=\"DownloadLink\" title=\"Download this video\"></a>").append(CRLF);
		endPage(sb);

		return sb.toString();
	}

	private boolean transMp4(String mime, DLNAMediaInfo media) {
		LOGGER.debug("mp4 profile "+media.getH264Profile());
		return mime.equals("video/mp4") && (configuration.isWebMp4Trans() ||
			   								media.getAvcAsInt() >= 40);
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
