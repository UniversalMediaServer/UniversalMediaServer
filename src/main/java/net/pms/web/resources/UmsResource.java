
package net.pms.web.resources;

import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.jboss.netty.channel.ChannelHandlerContext;
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
	public Ums get(@Context HttpHeaders headers, @Context ChannelHandlerContext chc) {
		Map<String, String> messages = Messages.getMessagesForLocale(ResourceUtil.getFirstSupportedLanguage(headers));
		boolean upnpAllowed = ResourceUtil.bumpAllowed(chc);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		return new Ums(upnpAllowed, upnpControl, configuration.getServerDisplayName(), messages);
	}
}
