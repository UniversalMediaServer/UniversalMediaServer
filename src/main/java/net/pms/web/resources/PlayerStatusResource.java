package net.pms.web.resources;

import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.configuration.WebRender;
import net.pms.dlna.RootFolder;
import net.pms.web.services.RootService;

@Singleton
@Path("playerstatus")
public class PlayerStatusResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

	private RootService roots;

	@Inject
	public PlayerStatusResource(RootService roots) {
		this.roots = roots;
	}

	@GET
	public Response getPlayerStatus(@Context SecurityContext context, @Context HttpServletRequest request)
			throws InterruptedException, IOException {

		String json;
		try (InputStream in = request.getInputStream()) {
			json = IOUtils.toString(request.getInputStream(), "UTF-8");
		}
		LOGGER.trace("got player status: " + json);

		RootFolder root = roots.getRoot(ResourceUtil.getUserName(context), request);
		if (root == null) {
			LOGGER.debug("root not found");
			throw new IOException("Unknown root");
		}
		WebRender renderer = (WebRender) root.getDefaultRenderer();
		((WebRender.WebPlayer) renderer.getPlayer()).setData(json);

		return Response.ok(json, MediaType.TEXT_HTML).build();
	}
}
