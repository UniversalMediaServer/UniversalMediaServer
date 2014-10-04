package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.FFMpegVideo;
import net.pms.encoders.Player;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
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

	private DLNAResource findNext(int start, int inc, List<DLNAResource> list) {
		int nxtPos = start;
		while ((nxtPos < list.size()) && (nxtPos >= 0)) {
			// if we're not last/first in list just pick next/prev from child list
			DLNAResource n = list.get(nxtPos);
			if (!n.isFolder()) {
				return n;
			}
			nxtPos += inc;
		}
		return null;
	}

	private String mkPage(String id, HttpExchange t) throws IOException {
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("serverName", configuration.getServerName());
		boolean flowplayer = true;

		LOGGER.debug("make play page " + id);
		RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
		if (root == null) {
			LOGGER.debug("root not found");
			throw new IOException("Unknown root");
		}
		WebRender renderer = (WebRender)root.getDefaultRenderer();
		renderer.setBrowserInfo(RemoteUtil.getCookie("UMSINFO", t), t.getRequestHeaders().getFirst("User-agent"));
		//List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, renderer);
		DLNAResource r = root.getDLNAResource(id, renderer);
		if (r == null) {
			LOGGER.debug("Bad id in web if "+id);
			throw new IOException("Bad Id");
		}
		if(!r.isCodeValid(r)) {
			LOGGER.debug("coded object with invalid code");
			throw new IOException("Bad code");
		}
		String auto = "autoplay";
		String query = t.getRequestURI().getQuery();
		boolean forceFlash = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "flash"));
		// next/prev handling
		String dir = RemoteUtil.getQueryVars(query, "nxt");
		if(StringUtils.isNotEmpty(dir)) {
			// if the "nxt" field is set we should calculate the next media
			// 1st fetch or own index in the child list
			List<DLNAResource> children = r.getParent().getChildren();
			int i = children.indexOf(r);
			DLNAResource n = null;
			int inc;
			int loopPos;
			if (dir.equals("next")) {
				inc = 1;
				loopPos = 0;
			} else {
				inc = -1;
				loopPos = children.size() - 1;
			}
			n = findNext(i + inc, inc, children);
			if (n == null && configuration.getWebAutoLoop(r.getFormat())) {
				// we were last/first so if we loop pick first/last in list
				n = findNext(loopPos, inc, children);
			}
			if (n != null) {
				// all done, change the id
				id = n.getResourceId();
				r = n;
			} else {
				// trick here to stop continuing if loop is off
				auto = "";
			}
		}
		String id1 = URLEncoder.encode(id, "UTF-8");
		String rawId = id;

		// hack here to ensure we got a root folder to use for recently played etc.
		root.getDefaultRenderer().setRootFolder(root);
		String name = StringEscapeUtils.escapeHtml(r.resumeName());
		String mime = root.getDefaultRenderer().getMimeType(r.mimeType());
		String mediaType = "";
		String coverImage = "";
		if (r instanceof VirtualVideoAction) {
			// for VVA we just call the enable fun directly
			// waste of resource to play dummy video
			((VirtualVideoAction) r).enable();
			// special page to return
			return "<html><head><script>window.refresh=true;history.back()</script></head></html>";
		}
		if (r.getFormat().isImage()) {
			flowplayer = false;
			coverImage = "<img src=\"/raw/" + rawId + "\" alt=\"\"><br>";
		}
		if (r.getFormat().isAudio()) {
			mediaType = "audio";
			String thumb = "/thumb/" + id1;
			coverImage = "<img height=256 width=256 src=\"" + thumb + "\" alt=\"\"><br><h2>" + name + "</h2><br>";
			flowplayer = false;
		}
		if (r.getFormat().isVideo()) {
			mediaType = "video";
			if (mime.equals(FormatConfiguration.MIMETYPE_AUTO)) {
				if (r.getMedia() != null && r.getMedia().getMimeType() != null) {
					mime = r.getMedia().getMimeType();
				}
			}
			if (!configuration.getWebFlash() && !forceFlash)  {
				if(!RemoteUtil.directmime(mime) || RemoteUtil.transMp4(mime, r.getMedia()) || r.isResume()) {
					WebRender render = (WebRender)r.getDefaultRenderer();
					mime = render != null ? render.getVideoMimeType() : RemoteUtil.transMime();
					flowplayer = false;
				}
			}
		}
		vars.put("name", name);
		vars.put("id1", id1);
		vars.put("coverImage", coverImage);
		vars.put("autocontinue", configuration.getWebAutoCont(r.getFormat()));
		boolean isImage = r.getFormat().isImage();
		if (isImage) {
			// do this like this to simplify the code
			// skip all player crap since img tag works well
			int delay = configuration.getWebImgSlideDelay() * 1000;
			if (delay > 0 && configuration.getWebAutoCont(r.getFormat())) {
				vars.put("delay", delay);
			}
		} else {
			vars.put("mediaType", mediaType);
			vars.put("auto", auto);
			vars.put("mime", mime);
			if (flowplayer) {
				if (
					RemoteUtil.directmime(mime) &&
					!RemoteUtil.transMp4(mime, r.getMedia()) &&
					!r.isResume() &&
					!forceFlash
				) {
					vars.put("src", true);
				}
			} else {
				vars.put("width", renderer.getVideoWidth());
				vars.put("height", renderer.getVideoHeight());
			}
		}

		if (configuration.getWebSubs() && r.getFormat().isVideo()) {
			// only if subs are requested as <track> tags
			// otherwise we'll transcode them in
			boolean isFFmpegFontConfig = configuration.isFFmpegFontConfig();
			if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
				configuration.setFFmpegFontConfig(false);
			}
			OutputParams p = new OutputParams(configuration);
			p.sid = r.getMediaSubtitle();
			Player.setAudioAndSubs(r.getName(), r.getMedia(), p);
			if (p.sid !=null && p.sid.getType().isText()) {
				try {
					File subFile = FFMpegVideo.getSubtitles(r, r.getMedia(), p, configuration, SubtitleType.WEBVTT);
					LOGGER.debug("subFile " + subFile);
					if (subFile != null) {
						vars.put("sub", parent.getResources().add(subFile));
					}
				} catch (Exception e) {
					LOGGER.debug("error when doing sub file " + e);
				}
			}

			configuration.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
		}

		return parent.getResources().getTemplate(isImage ? "image.html" :flowplayer ? "flow.html" : "play.html").execute(vars);
	}

	private boolean transMp4(String mime, DLNAMediaInfo media) {
		LOGGER.debug("mp4 profile " + media.getH264Profile());
		return mime.equals("video/mp4") && (configuration.isWebMp4Trans() || media.getAvcAsInt() >= 40);
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("got a play request " + t.getRequestURI());
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String id = RemoteUtil.getId("play/", t);
		String response = mkPage(id, t);
		LOGGER.debug("play page " + response);
		RemoteUtil.respond(t, response, 200, "text/html");
	}
}
