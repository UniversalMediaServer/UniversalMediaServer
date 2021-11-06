package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import net.pms.Messages;
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
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.util.SubtitleUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RemotePlayHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemotePlayHandler.class);
	private RemoteWeb parent;
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();

	public RemotePlayHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private static String returnPage() {
		// special page to return
		return "<html><head><script>window.refresh=true;history.back()</script></head></html>";
	}

	private static void addNextByType(DLNAResource d, HashMap<String, Object> vars) {
		List<DLNAResource> children = d.getParent().getChildren();
		boolean looping = CONFIGURATION.getWebAutoLoop(d.getFormat());
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

	private String mkPage(String id, HttpExchange t) throws IOException, InterruptedException {
		HashMap<String, Object> mustacheVars = new HashMap<>();
		mustacheVars.put("serverName", CONFIGURATION.getServerDisplayName());

		LOGGER.debug("Make play page " + id);
		RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
		if (root == null) {
			LOGGER.debug("root not found");
			throw new IOException("Unknown root");
		}
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		renderer.setBrowserInfo(RemoteUtil.getCookie("UMSINFO", t), t.getRequestHeaders().getFirst("User-agent"));
		//List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, renderer);
		DLNAResource rootResource = root.getDLNAResource(id, renderer);
		if (rootResource == null) {
			LOGGER.debug("Bad web play id: " + id);
			throw new IOException("Bad Id");
		}
		if (!rootResource.isCodeValid(rootResource)) {
			LOGGER.debug("coded object with invalid code");
			throw new IOException("Bad code");
		}
		if (rootResource instanceof VirtualVideoAction) {
			// for VVA we just call the enable fun directly
			// waste of resource to play dummy video
			if (((VirtualVideoAction) rootResource).enable()) {
				renderer.notify(renderer.INFO, rootResource.getName() + " enabled");
			} else {
				renderer.notify(renderer.INFO, rootResource.getName() + " disabled");
			}
			return returnPage();
		}

		String name = StringEscapeUtils.escapeHtml(rootResource.resumeName());
		String id1 = URLEncoder.encode(id, "UTF-8");
		mustacheVars.put("poster", "/thumb/" + id1);
		ArrayList<String> folders = new ArrayList<>();
		ArrayList<String> breadcrumbs = new ArrayList<>();
		StringBuilder backLinkHTML = new StringBuilder();
		Boolean isShowBreadcrumbs = false;

		if (
			rootResource.getParent() != null &&
			rootResource.getParent().isFolder()
		) {
			DLNAResource thisResourceFromResources = rootResource;

			breadcrumbs.add("<li class=\"active\">" + name + "</li>");
			while (thisResourceFromResources.getParent() != null && thisResourceFromResources.getParent().isFolder()) {
				thisResourceFromResources = thisResourceFromResources.getParent();
				String ancestorName = thisResourceFromResources.getDisplayName().equals("root") ? Messages.getString("Web.Home") : thisResourceFromResources.getDisplayName();
				ancestorName = StringEscapeUtils.escapeHtml(ancestorName);
				String ancestorID = thisResourceFromResources.getResourceId();
				String ancestorIDForWeb = URLEncoder.encode(ancestorID, "UTF-8");
				String ancestorUri = "/browse/" + ancestorIDForWeb;
				breadcrumbs.add(0, "<li><a href=\"" + ancestorUri + "\">" + ancestorName + "</a></li>");
				isShowBreadcrumbs = true;
			}

			DLNAResource parentFromResources = rootResource.getParent();
			String parentID = parentFromResources.getResourceId();
			String parentIDForWeb = URLEncoder.encode(parentID, "UTF-8");
			String backUri = "/browse/" + parentIDForWeb;
			backLinkHTML.append("<a href=\"").append(backUri).append("\" title=\"").append(RemoteUtil.getMsgString("Web.10", t)).append("\">");
			backLinkHTML.append("<span><i class=\"fa fa-angle-left\"></i> ").append(RemoteUtil.getMsgString("Web.10", t)).append("</span>");
			backLinkHTML.append("</a>");
			folders.add(backLinkHTML.toString());
		}
		mustacheVars.put("isShowBreadcrumbs", isShowBreadcrumbs);
		mustacheVars.put("breadcrumbs", breadcrumbs);
		mustacheVars.put("folders", folders);

		Format format = rootResource.getFormat();
		boolean isImage = format.isImage();
		boolean isVideo = format.isVideo();
		boolean isAudio = format.isAudio();
		String query = t.getRequestURI().getQuery();
		boolean forceFlash = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "flash"));
		boolean forcehtml5 = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "html5"));
		boolean flowplayer = isVideo && (forceFlash || (!forcehtml5 && CONFIGURATION.getWebFlash()));

		// hack here to ensure we got a root folder to use for recently played etc.
		root.getDefaultRenderer().setRootFolder(root);
		String mime = root.getDefaultRenderer().getMimeType(rootResource);
		String mediaType = isVideo ? "video" : isAudio ? "audio" : isImage ? "image" : "";
		String auto = "autoplay";
		@SuppressWarnings("unused")
		String coverImage = "";

		mustacheVars.put("isVideoWithAPIData", false);
		mustacheVars.put("javascriptVarsScript", "");
		if (isVideo) {
			if (CONFIGURATION.getUseCache()) {
				String apiMetadataAsJavaScriptVars = RemoteUtil.getAPIMetadataAsJavaScriptVars(rootResource, t, false, root);
				if (apiMetadataAsJavaScriptVars != null) {
					mustacheVars.put("javascriptVarsScript", apiMetadataAsJavaScriptVars);
					mustacheVars.put("isVideoWithAPIData", true);
				}
			}

			if (mime.equals(FormatConfiguration.MIMETYPE_AUTO)) {
				if (rootResource.getMedia() != null && rootResource.getMedia().getMimeType() != null) {
					mime = rootResource.getMedia().getMimeType();
				}
			}
			if (!flowplayer) {
				if (!RemoteUtil.directmime(mime) || RemoteUtil.transMp4(mime, rootResource.getMedia()) || rootResource.isResume()) {
					WebRender render = (WebRender) rootResource.getDefaultRenderer();
					mime = render != null ? render.getVideoMimeType() : RemoteUtil.transMime();
				}
			}
		}
		mustacheVars.put("isVideo", isVideo);

		// Controls whether to use the browser's native audio player
		mustacheVars.put("isNativeAudio", false);
		if (
			isAudio &&
			// Audio types that are natively supported by all major browsers:
			mime.equals(HTTPResource.AUDIO_MP3_TYPEMIME)
		) {
			mustacheVars.put("isNativeAudio", true);
		}

		mustacheVars.put("name", name);
		mustacheVars.put("id1", id1);
		mustacheVars.put("autoContinue", CONFIGURATION.getWebAutoCont(format));
		if (CONFIGURATION.isDynamicPls()) {
			if (rootResource.getParent() instanceof Playlist) {
				mustacheVars.put("plsOp", "del");
				mustacheVars.put("plsSign", "-");
				mustacheVars.put("plsAttr", RemoteUtil.getMsgString("Web.4", t));
			} else {
				mustacheVars.put("plsOp", "add");
				mustacheVars.put("plsSign", "+");
				mustacheVars.put("plsAttr", RemoteUtil.getMsgString("Web.5", t));
			}
		}
		addNextByType(rootResource, mustacheVars);
		if (isImage) {
			// do this like this to simplify the code
			// skip all player crap since img tag works well
			int delay = CONFIGURATION.getWebImgSlideDelay() * 1000;
			if (delay > 0 && CONFIGURATION.getWebAutoCont(format)) {
				mustacheVars.put("delay", delay);
			}
		} else {
			mustacheVars.put("mediaType", mediaType);
			mustacheVars.put("auto", auto);
			mustacheVars.put("mime", mime);
			if (flowplayer) {
				if (
					RemoteUtil.directmime(mime) &&
					!RemoteUtil.transMp4(mime, rootResource.getMedia()) &&
					!rootResource.isResume() &&
					!forceFlash
				) {
					mustacheVars.put("src", true);
				}
			} else {
				mustacheVars.put("width", renderer.getVideoWidth());
				mustacheVars.put("height", renderer.getVideoHeight());
			}
		}
		if (CONFIGURATION.useWebControl()) {
			mustacheVars.put("push", true);
		}

		if (isVideo && CONFIGURATION.getWebSubs()) {
			// only if subs are requested as <track> tags
			// otherwise we'll transcode them in
			boolean isFFmpegFontConfig = CONFIGURATION.isFFmpegFontConfig();
			if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
				CONFIGURATION.setFFmpegFontConfig(false);
			}
			OutputParams p = new OutputParams(CONFIGURATION);
			p.setSid(rootResource.getMediaSubtitle());
			Player.setAudioAndSubs(rootResource, p);
			if (p.getSid() != null && p.getSid().getType().isText()) {
				try {
					File subFile = SubtitleUtils.getSubtitles(rootResource, rootResource.getMedia(), p, CONFIGURATION, SubtitleType.WEBVTT);
					LOGGER.debug("subFile " + subFile);
					if (subFile != null) {
						mustacheVars.put("sub", parent.getResources().add(subFile));
					}
				} catch (Exception e) {
					LOGGER.debug("error when doing sub file " + e);
				}
			}

			CONFIGURATION.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
		}

		return parent.getResources().getTemplate(isImage ? "image.html" : flowplayer ? "flow.html" : "play.html").execute(mustacheVars);
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

				if (!FileUtil.isUrl(dlna.getSystemName())) {
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
		try {
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			String p = t.getRequestURI().getPath();
			if (p.contains("/play/")) {
				LOGGER.debug("got a play request " + t.getRequestURI());
				String id = RemoteUtil.getId("play/", t);
				String response = mkPage(id, t);
				//LOGGER.trace("play page " + response);
				RemoteUtil.respond(t, response, 200, "text/html");
			} else if (p.contains("/playerstatus/")) {
				String json = IOUtils.toString(t.getRequestBody(), StandardCharsets.UTF_8);
				LOGGER.trace("got player status: " + json);
				RemoteUtil.respond(t, "", 200, "text/html");

				RootFolder root = parent.getRoot(RemoteUtil.userName(t), t);
				if (root == null) {
					LOGGER.debug("root not found");
					throw new IOException("Unknown root");
				}
				WebRender renderer = (WebRender) root.getDefaultRenderer();
				((WebRender.WebPlayer) renderer.getPlayer()).setData(json);
			} else if (p.contains("/playlist/")) {
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
						renderer.notify(RendererConfiguration.OK, "Added '" + r.getDisplayName() + "' to dynamic playlist");
					} else if (op.equals("del") && (r.getParent() instanceof Playlist)) {
						((Playlist) r.getParent()).remove(r);
						renderer.notify(RendererConfiguration.INFO, "Removed '" + r.getDisplayName() + "' from playlist");
					}
				}
				RemoteUtil.respond(t, returnPage(), 200, "text/html");
			} else if (p.contains("/m3u8/")) {
				String id = StringUtils.substringBefore(StringUtils.substringAfter(p, "/m3u8/"), ".m3u8");
				String response = mkM3u8(PMS.getGlobalRepo().get(id));
				if (response != null) {
					LOGGER.debug("sending m3u8:\n" + response);
					RemoteUtil.respond(t, response, 200, "application/x-mpegURL");
				} else {
					RemoteUtil.respond(t, "<html><body>404 - File Not Found: " + p + "</body></html>", 404, "text/html");
				}
			}
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemotePlayHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
