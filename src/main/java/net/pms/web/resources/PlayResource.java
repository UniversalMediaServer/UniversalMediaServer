package net.pms.web.resources;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.OutputParams;
import net.pms.remote.RemoteUtil;
import net.pms.remote.RemoteUtil.ResourceManager;
import net.pms.util.SubtitleUtils;
import net.pms.web.model.PlayableMedia;
import net.pms.web.model.PlayableStep;
import net.pms.web.services.RootService;

@Singleton
@Path("play")
@Produces(MediaType.APPLICATION_JSON)
public class PlayResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

	@Context
	private SecurityContext securityContext;

	private PmsConfiguration configuration;

	private RootService roots;

	private ResourceManager resources;

	@Inject
	public PlayResource(RootService roots, ResourceManager resources, PmsConfiguration configuration) {
		this.configuration = configuration;
		this.roots = roots;
		this.resources = resources;
	}

	@GET
	@Path("{id}")
	public PlayableMedia play(@PathParam("id") String id, @Context HttpHeaders headers, @Context ChannelHandlerContext chc, @Context UriInfo uriInfo)
		throws UnsupportedEncodingException, InterruptedException {
		LOGGER.debug("Make play page " + id);

		RootFolder root = roots.getRoot(ResourceUtil.getUserName(securityContext), headers, chc);
		if (root == null) {
			LOGGER.debug("root not found");
			throw new NotFoundException("Unknown root");
		}

		WebRender renderer = (WebRender) root.getDefaultRenderer();
		renderer.setBrowserInfo(headers.getHeaderString("UMSINFO"), headers.getHeaderString(HttpHeaders.USER_AGENT));
		// List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0,
		// renderer);
		DLNAResource r = root.getDLNAResource(id, renderer);
		if (r == null) {
			LOGGER.debug("Bad web play id: " + id);
			throw new NotFoundException("Bad Id");
		}
		if (!r.isCodeValid(r)) {
			LOGGER.debug("coded object with invalid code");
			throw new NotFoundException("Bad code");
		}

		//
		// if (r instanceof VirtualVideoAction) {
		// // for VVA we just call the enable fun directly
		// // waste of resource to play dummy video
		// if (((VirtualVideoAction) r).enable()) {
		// renderer.notify(renderer.INFO, r.getName() + " enabled");
		// } else {
		// renderer.notify(renderer.INFO, r.getName() + " disabled");
		// }
		// return Response.ok(returnPage(), MediaType.TEXT_HTML).build();
		// }

		boolean inPlaylist = false;
		boolean push = false;
		boolean src = false;
		Integer sub = null;
		Integer height = null;
		Integer width = null;
		Integer delay = null;

		Format format = r.getFormat();
		boolean isImage = format.isImage();
		boolean isVideo = format.isVideo();
		boolean isAudio = format.isAudio();
		
		boolean forceFlash = StringUtils.isNotEmpty(uriInfo.getQueryParameters().getFirst("flash"));
		boolean forcehtml5 = StringUtils.isNotEmpty(uriInfo.getQueryParameters().getFirst("html5"));
		boolean flowplayer = isVideo && (forceFlash || (!forcehtml5 && configuration.getWebFlash()));
		boolean autoContinue = configuration.getWebAutoCont(format);

		// hack here to ensure we got a root folder to use for recently played
		// etc.
		root.getDefaultRenderer().setRootFolder(root);
		String name = r.resumeName();
		String mime = root.getDefaultRenderer().getMimeType(r.mimeType(), r.getMedia());
		String mediaType = isVideo ? "video" : isAudio ? "audio" : isImage ? "image" : "";
		boolean autoplay = true;
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
		if (configuration.isDynamicPls()) {
			inPlaylist = r.getParent() instanceof Playlist;
		}
		PlayableStep[] steps = getNextByType(r);
		if (isImage) {
			// do this like this to simplify the code
			// skip all player crap since img tag works well
			delay = configuration.getWebImgSlideDelay() > 0 && configuration.getWebAutoCont(format)
				? configuration.getWebImgSlideDelay() * 1000
				: 0;
		} else {
			autoplay = false;
			if (flowplayer) {
				if (RemoteUtil.directmime(mime) && !RemoteUtil.transMp4(mime, r.getMedia()) && !r.isResume() && !forceFlash) {
					src = true;
				}
			} else {
				width = renderer.getVideoWidth();
				height = renderer.getVideoHeight();
			}
		}
		if (configuration.useWebControl()) {
			push = true;
		}

		if (isVideo && configuration.getWebSubs()) {
			// only if subs are requested as <track> tags
			// otherwise we'll transcode them in
			boolean isFFmpegFontConfig = configuration.isFFmpegFontConfig();
			if (isFFmpegFontConfig) { // do not apply fontconfig to flowplayer
										 // subs
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
						sub = resources.add(subFile);
					}
				} catch (Exception e) {
					LOGGER.debug("error when doing sub file " + e);
				}
			}

			configuration.setFFmpegFontConfig(isFFmpegFontConfig); // return
																	 // back
																	 // original
																	 // fontconfig
																	 // value
		}

		return new PlayableMedia(URLEncoder.encode(id, "UTF-8"), name, mediaType, autoplay, mime, sub, height, width, inPlaylist, steps,
			delay, push, src, autoContinue);
	}

	// private static String returnPage() {
	// // special page to return
	// return
	// "<html><head><script>window.refresh=true;history.back()</script></head></html>";
	// }

	private PlayableStep[] getNextByType(DLNAResource d) {
		List<DLNAResource> children = d.getParent().getChildren();
		boolean looping = configuration.getWebAutoLoop(d.getFormat());
		int type = d.getType();
		int size = children.size();
		int mod = looping ? size : 9999;
		int self = children.indexOf(d);

		PlayableStep[] steps = new PlayableStep[2];
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

			steps[step > 0 ? 1 : 0] = new PlayableStep(next != null ? next.getResourceId() : null, next != null ? next.resumeName() : null);
		}
		return steps;
	}
}
