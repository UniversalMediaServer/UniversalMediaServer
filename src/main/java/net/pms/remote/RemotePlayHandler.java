package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
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

	public RemotePlayHandler(RemoteWeb parent) {
		this.parent = parent;
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
		if (r.getFormat().isAudio()) {
			mediaType = "audio";
			String thumb = "/thumb/" + id1;
			coverImage = "<img class=\"cover\" src=\"" + thumb + "\" alt=\"\"><br>";
		}
		if (r.getFormat().isVideo()) {
			mediaType = "video";
			if (!RemoteUtil.directmime(mime)) {
				mime = RemoteUtil.MIME_TRANS;
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
					sb.append("<script src=\"/files/flowplayer.min.js\"></script>").append(CRLF);
					sb.append("<link rel=\"stylesheet\" href=\"/files/functional.css\">").append(CRLF);
				}
			sb.append("</head>").append(CRLF);
			sb.append("<body id=\"ContentPage\">").append(CRLF);
				sb.append("<div id=\"Container\">").append(CRLF);
					sb.append("<div id=\"Menu\">").append(CRLF);
						sb.append("<a href=\"/browse/0\" id=\"HomeButton\"></a>").append(CRLF);
					sb.append("</div>").append(CRLF);
					sb.append(coverImage).append(CRLF);
					if (flowplayer) {
						sb.append("<div id=\"VideoContainer\">").append(CRLF);
						sb.append("<div class=\"flowplayer no-time no-volume no-mute\" data-ratio=\"0.5625\" data-embed=\"false\" data-flashfit=\"true\">").append(CRLF);
					}
					sb.append("<").append(mediaType);
					if (flowplayer) {
						sb.append(" autoplay>");
					} else {
						sb.append(" width=\"720\" height=\"404\" controls autoplay>").append(CRLF);
					}
					//sb.append("<source src=\"/media/").append(URLEncoder.encode(id1, "UTF-8")).append("\" type=\"").append(mime).append("\">");
					sb.append("<source src=\"/fmedia/").append(URLEncoder.encode(id1, "UTF-8")).append("\" type=\"video/x-flv\">");
					if (flowplayer) {
						PmsConfiguration configuration = PMS.getConfiguration();
						boolean isFFmpegFontConfig = configuration.isFFmpegFontConfig();
						if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
							configuration.setFFmpegFontConfig(false);
						}

						OutputParams p = new OutputParams(configuration);
						Player.setAudioAndSubs(r.getName(), r.getMedia(), p);
						try {
							File subFile = FFMpegVideo.getSubtitles(r, r.getMedia(), p, configuration);
							subFile = SubtitleUtils.convertSubripToWebVTT(subFile);
							LOGGER.debug("subFile " + subFile);
							if (subFile != null) {
								sb.append("<track src=\"/subs/").append(subFile.getAbsolutePath()).append("\">");
							}
						} catch (Exception e) {
							LOGGER.debug("error when doing sub file " + e);
						}
						
						configuration.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
					}
					sb.append("</").append(mediaType).append(">").append(CRLF);
					if (flowplayer) {
						sb.append("</div>").append(CRLF);
						sb.append("</div>").append(CRLF);
					}
					sb.append("<a href=\"/raw/").append(rawId).append("\" target=\"_blank\" id=\"DownloadLink\" title=\"Download this video\"></a>").append(CRLF);
				sb.append("</div>").append(CRLF);
			sb.append("</body>").append(CRLF);
		sb.append("</html>").append(CRLF);

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
