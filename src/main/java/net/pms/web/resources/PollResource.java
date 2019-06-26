package net.pms.web.resources;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.jboss.netty.channel.ChannelHandlerContext;
import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.web.services.RootService;

@Singleton
@Path("poll")
public class PollResource {

	private RootService roots;

	@Inject
	public PollResource(RootService roots) {
		this.roots = roots;
	}

	@GET
	public Response handle(@Context SecurityContext context, @Context HttpHeaders headers, @Context ChannelHandlerContext chc) throws Exception {
		RootFolder root = roots.getRoot(ResourceUtil.getUserName(context), headers, chc);
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		String json = renderer.getPushData();
		return Response.ok(json, MediaType.TEXT_PLAIN_TYPE).build();
	}
}
