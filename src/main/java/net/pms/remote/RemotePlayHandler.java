package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.FileTranscodeVirtualFolder;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.Player;
import net.pms.external.ExternalFactory;
import net.pms.external.URLResolver.URLResult;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.util.FileUtil;
import net.pms.util.SubtitleUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemotePlayHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemotePlayHandler.class);
	private RemoteWeb parent;
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	public RemotePlayHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String returnPage() {
		// special page to return
		return "<html><head><script>window.refresh=true;history.back()</script></head></html>";
	}

	private void addNextByType(DLNAResource d, HashMap<String, Object> vars) {
		List<DLNAResource> children = d.getParent().getChildren();
		boolean looping = configuration.getWebAutoLoop(d.getFormat());
		int type = d.getType();
		int size = children.size();
		int mod = looping ? size : 9999;
		int self = children.indexOf(d);
		for (int step = -1; step < 2; step += 2) {
			int i = self;
			int offset = (step < 0 && looping) ? size : 0;
			DLNAResource next = null;
			while (true) {
				i = (offset + i + step) % mod;
				if (i >= size || i < 0 || i == self) {
					break; // Not found
				}
				next = children.get(i);
				if (next.getType() == type && !next.isFolder()) {
					break; // Found
				}
				next = null;
			}
			String pos = step > 0 ? "next" : "prev";
			vars.put(pos + "Id", next != null ? next.getResourceId() : null);
			vars.put(pos + "Attr", next != null ? (" title=\"" + StringEscapeUtils.escapeHtml(next.resumeName()) + "\"") : " disabled");
		}
	}

	private String mkPage(String id, HttpExchange t) throws IOException {
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("serverName", configuration.getServerDisplayName());

		LOGGER.debug("Make play page " + id);
		RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
		if (root == null) {
			LOGGER.debug("root not found");
			throw new IOException("Unknown root");
		}
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		renderer.setBrowserInfo(RemoteUtil.getCookie("UMSINFO", t), t.getRequestHeaders().getFirst("User-agent"));
		//List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, renderer);
		DLNAResource r = root.getDLNAResource(id, renderer);
		if (r == null) {
			LOGGER.debug("Bad web play id: " + id);
			throw new IOException("Bad Id");
		}
		if (!r.isCodeValid(r)) {
			LOGGER.debug("coded object with invalid code");
			throw new IOException("Bad code");
		}
		if (r instanceof VirtualVideoAction) {
			// for VVA we just call the enable fun directly
			// waste of resource to play dummy video
			if (((VirtualVideoAction) r).enable()) {
				renderer.notify(renderer.INFO, r.getName() + " enabled");
			} else {
				renderer.notify(renderer.INFO, r.getName() + " disabled");
			}
			return returnPage();
		}

		Format format =  r.getFormat();
		boolean isImage = format.isImage();
		boolean isVideo = format.isVideo();
		boolean isAudio = format.isAudio();
		String query = t.getRequestURI().getQuery();
		boolean forceFlash = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "flash"));
		boolean forcehtml5 = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "html5"));
		boolean flowplayer = isVideo && (forceFlash || (!forcehtml5 && configuration.getWebFlash()));

		// hack here to ensure we got a root folder to use for recently played etc.
		root.getDefaultRenderer().setRootFolder(root);
		String id1 = URLEncoder.encode(id, "UTF-8");
		String name = StringEscapeUtils.escapeHtml(r.resumeName());
		String mime = root.getDefaultRenderer().getMimeType(r.mimeType(), r.getMedia());
		String mediaType = isVideo ? "video" : isAudio ? "audio" : isImage ? "image" : "";
		String auto = "autoplay";
		@SuppressWarnings("unused")
		String coverImage = "";

		if (isVideo) {
			if (mime.equals(FormatConfiguration.MIMETYPE_AUTO)) {
				if (r.getMedia() != null && r.getMedia().getMimeType() != null) {
					mime = r.getMedia().getMimeType();
				}
			}
			if (!flowplayer) {
				if (!RemoteUtil.directmime(mime) || RemoteUtil.transMp4(mime, r.getMedia()) || r.isResume()) {
					WebRender render = (WebRender) r.getDefaultRenderer();
					mime = render != null ? render.getVideoMimeType() : RemoteUtil.transMime();
				}
			}
		}
		vars.put("isVideo", isVideo);
		vars.put("name", name);
		vars.put("id1", id1);
		vars.put("autoContinue", configuration.getWebAutoCont(format));
		if (configuration.isDynamicPls()) {
			if (r.getParent() instanceof Playlist) {
				vars.put("plsOp", "del");
				vars.put("plsSign", "-");
				vars.put("plsAttr", RemoteUtil.getMsgString("Web.4", t));
			} else {
				vars.put("plsOp", "add");
				vars.put("plsSign", "+");
				vars.put("plsAttr", RemoteUtil.getMsgString("Web.5", t));
			}
		}
		addNextByType(r, vars);
		if (isImage) {
			// do this like this to simplify the code
			// skip all player crap since img tag works well
			int delay = configuration.getWebImgSlideDelay() * 1000;
			if (delay > 0 && configuration.getWebAutoCont(format)) {
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
		if (configuration.useWebControl()) {
			vars.put("push", true);
		}

		if (isVideo && configuration.getWebSubs()) {
			// only if subs are requested as <track> tags
			// otherwise we'll transcode them in
			boolean isFFmpegFontConfig = configuration.isFFmpegFontConfig();
			if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
				configuration.setFFmpegFontConfig(false);
			}
			OutputParams p = new OutputParams(configuration);
			p.sid = r.getMediaSubtitle();
			Player.setAudioAndSubs(r.getName(), r.getMedia(), p);
			if (p.sid != null && p.sid.getType().isText()) {
				try {
					File subFile = SubtitleUtils.getSubtitles(r, r.getMedia(), p, configuration, SubtitleType.WEBVTT);
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

		return parent.getResources().getTemplate(isImage ? "image.html" : flowplayer ? "flow.html" : "play.html").execute(vars);
	}

	private String mkM3u8(DLNAResource dlna) {
		if (dlna != null) {
			return makeM3u8(dlna.isFolder() ? dlna.getChildren() : Arrays.asList(new DLNAResource[]{dlna}));
		}
		return null;
	}

	private String makeM3u8(List<DLNAResource> resources) {
		ArrayList<Map<String, Object>> items = new ArrayList<>();
		double targetDuration = 1;
		for (DLNAResource dlna : resources) {
			if (dlna != null && !dlna.isFolder() && !dlna.isResume() && !(dlna instanceof VirtualVideoAction)) {
				double duration = dlna.getMedia() != null ? dlna.getMedia().getDurationInSeconds() : -1;
				if (duration > targetDuration) {
					targetDuration = duration + 1;
				}
				String url = null;

				if (! FileUtil.isUrl(dlna.getSystemName())) {
					// Get the resource url
					boolean isTranscodeFolderItem = dlna.isNoName() && (dlna.getParent() instanceof FileTranscodeVirtualFolder);
					// If the resource is not a transcode folder item, tag its url for forced streaming
					url = dlna.getURL(isTranscodeFolderItem  ? "" : RendererConfiguration.NOTRANSCODE, true, false);

				} else {
					// It's a WEB.conf item or plugin-provided url, make sure it's resolved
					url = dlna.getSystemName();
					if (!dlna.isURLResolved()) {
						URLResult r = ExternalFactory.resolveURL(url);
						if (r != null && StringUtils.isNotEmpty(r.url)) {
							url = r.url;
						}
					}
				}

				Map<String, Object> item = new HashMap<>();
				item.put("duration", duration != 0 ? duration : -1);
				item.put("title", dlna.resumeName());
				item.put("url", url);
				items.add(item);
			}
		}

		if (!items.isEmpty()) {
			HashMap<String, Object> vars = new HashMap<>();
			vars.put("targetDuration", targetDuration != 0 ? targetDuration : -1);
			vars.put("items", items);
			return parent.getResources().getTemplate("play.m3u8").execute(vars);
		}
		return null;
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String p = t.getRequestURI().getPath();
		if (p.contains("/play/")) {
			LOGGER.debug("got a play request " + t.getRequestURI());
			String id = RemoteUtil.getId("play/", t);
			String response = mkPage(id, t);
//			LOGGER.trace("play page " + response);
			RemoteUtil.respond(t, response, 200, "text/html");
		} else if (p.contains("/playerstatus/")) {
			String json = IOUtils.toString(t.getRequestBody(), "UTF-8");
			LOGGER.trace("got player status: " + json);
			RemoteUtil.respond(t, "", 200, "text/html");

			RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
			if (root == null) {
				LOGGER.debug("root not found");
				throw new IOException("Unknown root");
			}
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			((WebRender.WebPlayer)renderer.getPlayer()).setData(json);
		}  else if (p.contains("/playlist/")) {
			String[] tmp = p.split("/");
			// sanity
			if (tmp.length < 3) {
				throw new IOException("Bad request");
			}
			String op = tmp[tmp.length - 2];
			String id = tmp[tmp.length - 1];
			DLNAResource r = PMS.getGlobalRepo().get(id);
 			if (r != null) {
				RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
				if (root == null) {
					LOGGER.debug("root not found");
					throw new IOException("Unknown root");
				}
				WebRender renderer = (WebRender) root.getDefaultRenderer();
 				if (op.equals("add")) {
 					PMS.get().getDynamicPls().add(r);
					renderer.notify(renderer.OK, "Added '" + r.getDisplayName() + "' to dynamic playlist");
				} else if (op.equals("del") && (r.getParent() instanceof Playlist)) {
					((Playlist)r.getParent()).remove(r);
					renderer.notify(renderer.INFO, "Removed '" + r.getDisplayName() + "' from playlist");
				}
			}
			RemoteUtil.respond(t, returnPage(), 200, "text/html");
		}  else if (p.contains("/m3u8/")) {
			String id = StringUtils.substringBefore(StringUtils.substringAfter(p, "/m3u8/"), ".m3u8");
			String response = mkM3u8(PMS.getGlobalRepo().get(id));
			if (response != null) {
				LOGGER.debug("sending m3u8:\n" + response);
				RemoteUtil.respond(t, response, 200, "application/x-mpegURL");
			} else {
				RemoteUtil.respond(t, "<html><body>404 - File Not Found: " + p + "</body></html>", 404, "text/html");
			}

		}
	}
}
