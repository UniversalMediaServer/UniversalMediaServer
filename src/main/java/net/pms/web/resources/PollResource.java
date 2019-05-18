package net.pms.web.resources;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.web.model.Roots;

@Singleton
@Path("poll")
public class PollResource {

	private Roots roots;

	@Inject
	public PollResource(Roots roots) {
		this.roots = roots;
	}

	@GET
	public Response handle(@Context SecurityContext context, @Context HttpServletRequest request) throws Exception {
		RootFolder root = roots.getRoot(ResourceUtil.getUserName(context), request);
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		String json = renderer.getPushData();
		return Response.ok(json, MediaType.TEXT_PLAIN_TYPE).build();
	}
}
