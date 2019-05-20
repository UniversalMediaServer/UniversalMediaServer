package net.pms.web.resources;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.remote.RemoteUtil;
import net.pms.remote.RemoteUtil.ResourceManager;
import net.pms.util.SubtitleUtils;
import net.pms.web.model.RootService;
import net.pms.web.model.TemplateService;

@Singleton
@Path("play")
public class PlayResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

	@Context
	private SecurityContext securityContext;

	private PmsConfiguration configuration;

	private RootService roots;
	
	private TemplateService templates;

	private ResourceManager resources;

	@Inject
	public PlayResource(RootService roots, ResourceManager resources, TemplateService templates, PmsConfiguration configuration) {
		this.configuration = configuration;
		this.roots = roots;
		this.resources = resources;
		this.templates = templates;
	}

	@GET
	@Path("{id:.*}")
	public Response play(@PathParam("id") String id, @Context HttpServletRequest request)
			throws IOException, InterruptedException {
		LOGGER.debug("got a play request {}", id);
		return mkPage(id, request);
	}

	private static String returnPage() {
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
			vars.put(pos + "Attr", next != null ? (" title=\"" + StringEscapeUtils.escapeHtml(next.resumeName()) + "\"")
					: " disabled");
		}
	}

	private Response mkPage(String id, HttpServletRequest t) throws IOException, InterruptedException {
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("serverName", configuration.getServerDisplayName());

		LOGGER.debug("Make play page " + id);
		RootFolder root = roots.getRoot(ResourceUtil.getUserName(securityContext), t);
		if (root == null) {
			LOGGER.debug("root not found");
			throw new NotFoundException("Unknown root");
		}
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		renderer.setBrowserInfo(ResourceUtil.getCookie(t, "UMSINFO"), t.getHeader("User-agent"));
		// List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, renderer);
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
			return Response.ok(returnPage(), MediaType.TEXT_HTML).build();
		}

		Format format = r.getFormat();
		boolean isImage = format.isImage();
		boolean isVideo = format.isVideo();
		boolean isAudio = format.isAudio();
		String query = t.getQueryString();
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
				vars.put("plsAttr", ResourceUtil.getMsgString("Web.4", t));
			} else {
				vars.put("plsOp", "add");
				vars.put("plsSign", "+");
				vars.put("plsAttr", ResourceUtil.getMsgString("Web.5", t));
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
				if (RemoteUtil.directmime(mime) && !RemoteUtil.transMp4(mime, r.getMedia()) && !r.isResume()
						&& !forceFlash) {
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
			Player.setAudioAndSubs(r, p);
			if (p.sid != null && p.sid.getType().isText()) {
				try {
					File subFile = SubtitleUtils.getSubtitles(r, r.getMedia(), p, configuration, SubtitleType.WEBVTT);
					LOGGER.debug("subFile " + subFile);
					if (subFile != null) {
						vars.put("sub", resources.add(subFile));
					}
				} catch (Exception e) {
					LOGGER.debug("error when doing sub file " + e);
				}
			}

			configuration.setFFmpegFontConfig(isFFmpegFontConfig); // return back original fontconfig value
		}

		return Response.ok(
				templates.getTemplate(isImage ? "image.html" : flowplayer ? "flow.html" : "play.html").execute(vars),
				MediaType.TEXT_HTML).build();
	}
}
