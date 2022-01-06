/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.webserver.servlets;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.pms.Messages;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.network.HTTPResource;
import net.pms.util.SubtitleUtils;
import net.pms.webserver.HttpServletHelper;
import net.pms.webserver.RemoteUtil;
import net.pms.webserver.WebServerServlets;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayServlet extends WebServerServlet {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlayServlet.class);
	private static final String RETURN_PAGE = "<html><head><script>window.refresh=true;history.back()</script></head></html>";

	public PlayServlet(WebServerServlets parent) {
		super(parent);
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

	private String mkPage(String id, HttpServletRequest request, HttpServletResponse response) throws IOException, InterruptedException {
		HashMap<String, Object> mustacheVars = new HashMap<>();
		String user = RemoteUtil.userName(request);
		mustacheVars.put("serverName", CONFIGURATION.getServerDisplayName());

		LOGGER.debug("Make play page " + id);
		String language = RemoteUtil.getFirstSupportedLanguage(request);
		RootFolder root = parent.getRoot(user, request, response);
		if (root == null) {
			LOGGER.debug("root not found");
			response.sendError(401, "Unknown root");
			return null;
		}
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		renderer.setBrowserInfo(HttpServletHelper.getCookie("UMSINFO", request), request.getHeader("User-agent"));
		//List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, renderer);
		DLNAResource rootResource = root.getDLNAResource(id, renderer);
		if (rootResource == null) {
			LOGGER.debug("Bad web play id: " + id);
			response.sendError(404, "Bad web play id: " + id);
			return null;
		}
		if (!rootResource.isCodeValid(rootResource)) {
			LOGGER.debug("coded object with invalid code");
			response.sendError(403, "coded object with invalid code");
			return null;
		}
		if (rootResource instanceof VirtualVideoAction) {
			// for VVA we just call the enable fun directly
			// waste of resource to play dummy video
			synchronized (renderer) {
				if (((VirtualVideoAction) rootResource).enable()) {
					renderer.notify(RendererConfiguration.INFO, rootResource.getName() + " enabled");
				} else {
					renderer.notify(RendererConfiguration.INFO, rootResource.getName() + " disabled");
				}
			}
			return RETURN_PAGE;
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
			backLinkHTML.append("<a href=\"").append(backUri).append("\" title=\"").append(RemoteUtil.getMsgString("Web.10", language)).append("\">");
			backLinkHTML.append("<span><i class=\"fa fa-angle-left\"></i> ").append(RemoteUtil.getMsgString("Web.10", language)).append("</span>");
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
		boolean forceFlash = StringUtils.isNotEmpty(request.getParameter("flash"));
		boolean forcehtml5 = StringUtils.isNotEmpty(request.getParameter("html5"));
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
				String apiMetadataAsJavaScriptVars = RemoteUtil.getAPIMetadataAsJavaScriptVars(rootResource, language, false, root);
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
				mustacheVars.put("plsAttr", RemoteUtil.getMsgString("Web.4", language));
			} else {
				mustacheVars.put("plsOp", "add");
				mustacheVars.put("plsSign", "+");
				mustacheVars.put("plsAttr", RemoteUtil.getMsgString("Web.5", language));
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
				} catch (IOException e) {
					LOGGER.debug("error when doing sub file " + e);
				}
			}

			CONFIGURATION.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
		}

		return parent.getResources().getTemplate(isImage ? "image.html" : flowplayer ? "flow.html" : "play.html").execute(mustacheVars);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		try {
			URI uri = URI.create(request.getRequestURI());
			LOGGER.debug("got a play request " + uri);
			String id = RemoteUtil.getId("play/", uri);
			String responseString = mkPage(id, request, response);
			RemoteUtil.respondHtml(response, responseString);
		} catch (IOException e) {
			throw e;
		} catch (InterruptedException e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in PlayServlet.doGet(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
