package net.pms.web.resources;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.dlna.RealFile;
import net.pms.dlna.RootFolder;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageFormat;
import net.pms.network.HTTPResource;
import net.pms.util.FullyPlayed;
import net.pms.web.model.Roots;

@Singleton
@Path("thumb")
public class ThumbResource {

	private final static Logger LOGGER = LoggerFactory.getLogger(ThumbResource.class);

	private Roots roots;

	private PmsConfiguration configuration;

	@Inject
	public ThumbResource(Roots roots, PmsConfiguration configuration) {
		this.roots = roots;
		this.configuration = configuration;
	}

	@GET
	@Path("logo")
	public Response getLogo() throws IOException {
		LOGGER.trace("web thumb req logo");
		return ResourceUtil.logoResponse();
	}

	@GET
	@Path("{id}")
	public Response handle(@PathParam("id") String id, @Context SecurityContext context,
			@Context HttpServletRequest request) throws Exception {
		try {
			LOGGER.trace("web thumb req " + id);
			RootFolder root = roots.getRoot(ResourceUtil.getUserName(context), request);
			if (root == null) {
				LOGGER.debug("weird root in thumb req");
				throw new NotFoundException("Unknown root");
			}
			final DLNAResource r = root.getDLNAResource(id, root.getDefaultRenderer());
			if (r == null) {
				// another error
				LOGGER.debug("media unknown");
				throw new NotFoundException("Bad id");
			}
			DLNAThumbnailInputStream in;
			if (!configuration.isShowCodeThumbs() && !r.isCodeValid(r)) {
				// we shouldn't show the thumbs for coded objects
				// unless the code is entered
				in = r.getGenericThumbnailInputStream(null);
			} else {
				r.checkThumbnail();
				in = r.fetchThumbnailInputStream();
			}
			BufferedImageFilterChain filterChain = null;
			if (r instanceof RealFile && FullyPlayed.isFullyPlayedMark(((RealFile) r).getFile())) {
				filterChain = new BufferedImageFilterChain(FullyPlayed.getOverlayFilter());
			}
			filterChain = r.addFlagFilters(filterChain);
			if (filterChain != null) {
				in = in.transcode(in.getDLNAImageProfile(), false, filterChain);
			}
			ResponseBuilder response = Response.ok();
			response.header(HttpHeaders.CONTENT_TYPE,
					ImageFormat.PNG.equals(in.getFormat()) ? HTTPResource.PNG_TYPEMIME : HTTPResource.JPEG_TYPEMIME);
			response.header("Accept-Ranges", "bytes");
			response.header("Connection", "keep-alive");
			response.header(HttpHeaders.CONTENT_LENGTH, in.getSize());
			LOGGER.trace("Web thumbnail: Input is {}", in);
			return response.entity(in).build();
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteThumbHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
			throw e;
		}
	}
}
