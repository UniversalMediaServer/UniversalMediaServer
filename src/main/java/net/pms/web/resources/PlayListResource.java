package net.pms.web.resources;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.WebRender;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.web.services.RootService;

@Singleton
@Path("playlist")
public class PlayListResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

	private RootService roots;

	@Inject
	public PlayListResource(RootService roots) {
		this.roots = roots;
	}

	private static String returnPage() {
		// special page to return
		return "<html><head><script>window.refresh=true;history.back()</script></head></html>";
	}

	@GET
	@Path("{id}/{op}")
	public Response get(@PathParam("id") String id, @PathParam("op") String op, @Context HttpServletRequest request,
		@Context SecurityContext securityContext) throws InterruptedException {

		DLNAResource r = PMS.getGlobalRepo().get(id);
		if (r != null) {
			RootFolder root = roots.getRoot(ResourceUtil.getUserName(securityContext), request);
			if (root == null) {
				LOGGER.debug("root not found");
				throw new NotFoundException("Unknown root");
			}
			WebRender renderer = (WebRender) root.getDefaultRenderer();
			if (op.equals("add")) {
				PMS.get().getDynamicPls().add(r);
				renderer.notify(renderer.OK, "Added '" + r.getDisplayName() + "' to dynamic playlist");
			} else if (op.equals("del") && (r.getParent() instanceof Playlist)) {
				((Playlist) r.getParent()).remove(r);
				renderer.notify(renderer.INFO, "Removed '" + r.getDisplayName() + "' from playlist");
			}
		}
		return Response.ok(returnPage(), MediaType.TEXT_HTML).build();
	}
}
