package net.pms.web.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Range;
import net.pms.dlna.RootFolder;
import net.pms.encoders.ImagePlayer;
import net.pms.image.Image;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil.ScaleType;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.remote.RemoteUtil;
import net.pms.web.model.RootService;

@Singleton
@Path("raw")
public class RawResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(RawResource.class);

	private RootService roots;

	@Inject
	public RawResource(RootService roots) {
		this.roots = roots;
	}

	@GET
	@Path("{path:.*}")
	public Response handle(@PathParam("path") String id, @Context SecurityContext context,
			@Context HttpServletRequest request) throws Exception {
		try {
			LOGGER.debug("got a raw request {}", id);
			RootFolder root = roots.getRoot(ResourceUtil.getUserName(context), request);
			if (root == null) {
				throw new IOException("Unknown root");
			}
			id = RemoteUtil.strip(id);
			LOGGER.debug("raw id " + id);
			List<DLNAResource> res = root.getDLNAResources(id, false, 0, 0, root.getDefaultRenderer());
			if (res.size() != 1) {
				// another error
				LOGGER.debug("media unkonwn");
				throw new IOException("Bad id");
			}
			DLNAResource dlna = res.get(0);
			long len;
			String mime = null;
			InputStream in;
			Range.Byte range;
			if (dlna.getMedia() != null && dlna.getMedia().isImage() && dlna.getMedia().getImageInfo() != null) {
				boolean supported = false;
				ImageInfo imageInfo = dlna.getMedia().getImageInfo();
				if (root.getDefaultRenderer() instanceof WebRender) {
					WebRender renderer = (WebRender) root.getDefaultRenderer();
					supported = renderer.isImageFormatSupported(imageInfo.getFormat());
				}
				mime = dlna.getFormat() != null ? dlna.getFormat().mimeType()
						: root.getDefaultRenderer().getMimeType(dlna.mimeType(), dlna.getMedia());

				len = supported && imageInfo.getSize() != ImageInfo.SIZE_UNKNOWN ? imageInfo.getSize() : dlna.length();
				range = new Range.Byte(0l, len);
				if (supported) {
					in = dlna.getInputStream();
				} else {
					InputStream imageInputStream;
					if (dlna.getPlayer() instanceof ImagePlayer) {
						ProcessWrapper transcodeProcess = dlna.getPlayer().launchTranscode(dlna, dlna.getMedia(),
								new OutputParams(PMS.getConfiguration()));
						imageInputStream = transcodeProcess != null ? transcodeProcess.getInputStream(0) : null;
					} else {
						imageInputStream = dlna.getInputStream();
					}
					Image image = Image.toImage(imageInputStream, 3840, 2400, ScaleType.MAX, ImageFormat.JPEG, false);
					len = image == null ? 0 : image.getBytes(false).length;
					in = image == null ? null : new ByteArrayInputStream(image.getBytes(false));
				}
			} else {
				len = dlna.length();
				dlna.setPlayer(null);
				range = ResourceUtil.parseRange(request, len);
				in = dlna.getInputStream(range, root.getDefaultRenderer());
				if (len == 0) {
					// For web resources actual length may be unknown until we open the stream
					len = dlna.length();
				}
				mime = root.getDefaultRenderer().getMimeType(dlna.mimeType(), dlna.getMedia());
			}
			ResponseBuilder response = Response.ok();
			LOGGER.debug("Sending media \"{}\" with mime type \"{}\"", dlna, mime);
			response.header("Content-Type", mime);
			response.header("Accept-Ranges", "bytes");
			response.header("Server", PMS.get().getServerName());
			response.header("Connection", "keep-alive");
			response.header("Transfer-Encoding", "chunked");
			if (in != null && in.available() != len) {
				response.header("Content-Range", "bytes " + range.getStart() + "-" + in.available() + "/" + len);
				response.status(206);
			}
			LOGGER.debug("start raw dump");
			return response.entity(in).build();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteRawHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
			throw e;
		}

	}
}
