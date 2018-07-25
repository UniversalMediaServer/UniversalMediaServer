package net.pms.remote;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.MediaType;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.util.SubtitleUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
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
			vars.put(pos + "Attr", next != null ? (" title=\"" + StringEscapeUtils.escapeHtml3(next.resumeName()) + "\"") : " disabled");
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
		DLNAResource resource = root.getDLNAResource(id, renderer);
		if (resource == null) {
			LOGGER.debug("Bad web play id: " + id);
			throw new IOException("Bad Id");
		}
		if (!resource.isCodeValid(resource)) {
			LOGGER.debug("coded object with invalid code");
			throw new IOException("Bad code");
		}
		if (resource instanceof VirtualVideoAction) {
			// for VVA we just call the enable fun directly
			// waste of resource to play dummy video
			if (((VirtualVideoAction) resource).enable()) {
				renderer.notify(renderer.INFO, resource.getName() + " enabled");
			} else {
				renderer.notify(renderer.INFO, resource.getName() + " disabled");
			}
			return returnPage();
		}

		Format format =  resource.getFormat();
		DLNAMediaInfo media = resource.getMedia();
		MediaType mediaType = media == null ? MediaType.UNKNOWN : media.getMediaType();
		String query = t.getRequestURI().getQuery();
		boolean forceFlash = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "flash"));
		boolean forcehtml5 = StringUtils.isNotEmpty(RemoteUtil.getQueryVars(query, "html5"));
		boolean flowplayer = mediaType == MediaType.VIDEO && (forceFlash || (!forcehtml5 && configuration.getWebFlash()));

		// hack here to ensure we got a root folder to use for recently played etc.
		root.getDefaultRenderer().setRootFolder(root);
		String id1 = URLEncoder.encode(id, "UTF-8");
		String name = StringEscapeUtils.escapeHtml3(resource.resumeName());
		String mimeType = root.getDefaultRenderer().getMimeType(resource.mimeType(), media);
		String auto = "autoplay";
		@SuppressWarnings("unused")
		String coverImage = "";

		if (mediaType == MediaType.VIDEO) {
			if (mimeType.equals(FormatConfiguration.MIMETYPE_AUTO)) {
				if (media != null && media.getMimeType() != null) {
					mimeType = media.getMimeType();
				}
			}
			if (!flowplayer) {
				if (!RemoteUtil.directmime(mimeType) || RemoteUtil.transMp4(mimeType, media) || resource.isResume()) {
					WebRender render = (WebRender) resource.getDefaultRenderer();
					mimeType = render != null ? render.getVideoMimeType() : RemoteUtil.transMime();
				}
			}
		}
		vars.put("isVideo", mediaType == MediaType.VIDEO);
		vars.put("name", name);
		vars.put("id1", id1);
		vars.put("autoContinue", configuration.getWebAutoCont(format));
		if (configuration.isDynamicPls()) {
			if (resource.getParent() instanceof Playlist) {
				vars.put("plsOp", "del");
				vars.put("plsSign", "-");
				vars.put("plsAttr", RemoteUtil.getMsgString("Web.4", t));
			} else {
				vars.put("plsOp", "add");
				vars.put("plsSign", "+");
				vars.put("plsAttr", RemoteUtil.getMsgString("Web.5", t));
			}
		}
		addNextByType(resource, vars);
		if (mediaType == MediaType.IMAGE) {
			// do this like this to simplify the code
			// skip all player crap since img tag works well
			int delay = configuration.getWebImgSlideDelay() * 1000;
			if (delay > 0 && configuration.getWebAutoCont(format)) {
				vars.put("delay", delay);
			}
		} else {
			vars.put("mediaType", mediaType.getString());
			vars.put("auto", auto);
			vars.put("mime", mimeType);
			if (flowplayer) {
				if (
					RemoteUtil.directmime(mimeType) &&
					!RemoteUtil.transMp4(mimeType, media) &&
					!resource.isResume() &&
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

		if (mediaType == MediaType.VIDEO && configuration.getWebSubs()) {
			// only if subs are requested as <track> tags
			// otherwise we'll transcode them in
			boolean isFFmpegFontConfig = configuration.isFFmpegFontConfig();
			if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer subs
				configuration.setFFmpegFontConfig(false);
			}
			OutputParams params = new OutputParams(configuration);
			params.sid = resource.getMediaSubtitle();
			Player.setAudioAndSubs(resource.getName(), media, params);
			if (params.sid != null && params.sid.getType().isText()) {
				try {
					File subFile = SubtitleUtils.getSubtitles(resource, media, params, configuration, SubtitleType.WEBVTT);
					LOGGER.debug("Adding subtitles file \"{}\"", subFile);
					if (subFile != null) {
						vars.put("sub", parent.getResources().add(subFile));
					}
				} catch (Exception e) {
					LOGGER.error(
						"An error occured while resolving subtitles file for \"{}\": {}",
						resource.getName(),
						e.getMessage()
					);
					LOGGER.trace("", e);
				}
			}

			configuration.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
		}

		return parent.getResources().getTemplate(
			mediaType == MediaType.IMAGE ? "image.html" : flowplayer ? "flow.html" : "play.html"
		).execute(vars);
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
		}
	}
}
