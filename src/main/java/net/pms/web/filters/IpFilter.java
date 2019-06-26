package net.pms.web.filters;

import java.io.IOException;
import java.net.InetAddress;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.web.resources.ResourceUtil;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class IpFilter implements ContainerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(IpFilter.class);

	@Context 
	private ChannelHandlerContext chc;

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		InetAddress addr = ResourceUtil.getAddress(chc);
		if (!PMS.getConfiguration().getIpFiltering().allowed(addr) || !PMS.isReady()) {
			LOGGER.info("Denying external request");
			requestContext.abortWith(Response.status(Status.FORBIDDEN).build());
		}
	}
}
