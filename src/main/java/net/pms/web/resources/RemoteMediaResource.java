package net.pms.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DVDISOTitle;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.StandardPlayerId;
import net.pms.remote.RemoteUtil;
import net.pms.util.FileUtil;
import net.pms.web.model.Roots;

@Singleton
@Path("media")
public class RemoteMediaResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteMediaResource.class);

	private RendererConfiguration renderer;

	private Roots roots;

	@Inject
	public RemoteMediaResource(Roots roots) {
		this.roots = roots;
	}

	@GET
	@PathParam("{id:.*}")
	public Response handle(@PathParam("id") String id, @Context SecurityContext securityContext,
			@Context HttpServletRequest t) throws InterruptedException, IOException {
		RootFolder root = roots.getRoot(ResourceUtil.getUserName(securityContext), t);
		if (root == null) {
			throw new NotFoundException("Unknown root");
		}

		Enumeration<String> headers = t.getHeaderNames();
		while (headers.hasMoreElements()) {
			String h1 = headers.nextElement();
			LOGGER.debug("key " + h1 + "=" + t.getHeader(h1));
		}
		id = RemoteUtil.strip(id);
		RendererConfiguration defaultRenderer = renderer;
		if (renderer == null) {
			defaultRenderer = root.getDefaultRenderer();
		}
		DLNAResource resource = root.getDLNAResource(id, defaultRenderer);
		if (resource == null) {
			// another error
			LOGGER.debug("media unkonwn");
			throw new NotFoundException("Bad id");
		}
		if (!resource.isCodeValid(resource)) {
			LOGGER.debug("coded object with invalid code");
			throw new IOException("Bad code");
		}
		DLNAMediaSubtitle sid = null;
		String mimeType = root.getDefaultRenderer().getMimeType(resource.mimeType(), resource.getMedia());
		// DLNAResource dlna = res.get(0);
		WebRender renderer = (WebRender) defaultRenderer;
		DLNAMediaInfo media = resource.getMedia();
		if (media == null) {
			media = new DLNAMediaInfo();
			resource.setMedia(media);
		}
		if (mimeType.equals(FormatConfiguration.MIMETYPE_AUTO) && media.getMimeType() != null) {
			mimeType = media.getMimeType();
		}
		ResponseBuilder response = Response.ok();
		resource.setDefaultRenderer(defaultRenderer);
		if (resource.getFormat().isVideo()) {
			if (!RemoteUtil.directmime(mimeType) || RemoteUtil.transMp4(mimeType, media)) {
				mimeType = renderer != null ? renderer.getVideoMimeType() : RemoteUtil.transMime();
				if (FileUtil.isUrl(resource.getSystemName())) {
					resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_WEB_VIDEO, false, false));
				} else if (!(resource instanceof DVDISOTitle)) {
					resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_VIDEO, false, false));
				}
				// code = 206;
			}
			if (PMS.getConfiguration().getWebSubs() && resource.getMediaSubtitle() != null
					&& resource.getMediaSubtitle().isExternal()) {
				// fetched on the side
				sid = resource.getMediaSubtitle();
				resource.setMediaSubtitle(null);
			}
		}

		if (!RemoteUtil.directmime(mimeType) && resource.getFormat().isAudio()) {
			resource.setPlayer(PlayerFactory.getPlayer(StandardPlayerId.FFMPEG_AUDIO, false, false));
			response.status(206);
		}

		media.setMimeType(mimeType);
		Range.Byte range = ResourceUtil.parseRange(t, resource.length());
		LOGGER.debug("Sending {} with mime type {} to {}", resource, mimeType, renderer);
		InputStream in = resource.getInputStream(range, root.getDefaultRenderer());
		if (range.getEnd() == 0) {
			// For web resources actual length may be unknown until we open the stream
			range.setEnd(resource.length());
		}
		response.header("Content-Type", mimeType);
		response.header("Accept-Ranges", "bytes");
		long end = range.getEnd();
		long start = range.getStart();
		String rStr = start + "-" + end + "/*";
		response.header("Content-Range", "bytes " + rStr);
		if (start != 0) {
			response.status(206);
		}

		response.header("Server", PMS.get().getServerName());
		response.header("Connection", "keep-alive");
		if (renderer != null) {
			renderer.start(resource);
		}
		if (sid != null) {
			resource.setMediaSubtitle(sid);
		}

		response.entity(new StreamingRendererOutput(in, renderer));

		return response.build();
	}
}
