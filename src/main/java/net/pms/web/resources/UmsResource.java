
package net.pms.web.resources;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import net.pms.Messages;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.web.model.Ums;

@Singleton
@Path("/ums")
@Produces(MediaType.APPLICATION_JSON)
public class UmsResource {

	private PmsConfiguration configuration;

	@Inject
	public UmsResource(PmsConfiguration configuration) {
		this.configuration = configuration;
	}

	@GET
	public Ums get(@Context HttpServletRequest request) {
		Map<String, String> messages = Messages
			.getMessagesForLocale(ResourceUtil.getFirstSupportedLanguage(request.getHeader(HttpHeaders.ACCEPT_LANGUAGE)));
		boolean upnpAllowed = ResourceUtil.bumpAllowed(request);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		return new Ums(upnpAllowed, upnpControl, configuration.getServerDisplayName(), messages);
	}
}
