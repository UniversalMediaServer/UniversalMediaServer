package net.pms.web.filters;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		String credentials = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		if (credentials != null) {
			int space = credentials.indexOf(' ');
			if (space > 0) {
				String method = credentials.substring(0, space);
				if ("basic".equalsIgnoreCase(method)) {
					credentials = credentials.substring(space + 1);
					credentials = new String(Base64.getDecoder().decode(credentials), StandardCharsets.ISO_8859_1);
					int i = credentials.indexOf(':');
					if (i > 0) {
						String user = credentials.substring(0, i);
						String pwd = credentials.substring(i + 1);

						LOGGER.debug("authenticate " + user);
						if (PMS.verifyCred("web", PMS.getCredTag("web", user), user, pwd)) {
							return;
						}
					}
				}
			}

		}
		requestContext.abortWith(Response.status(Status.UNAUTHORIZED).header(HttpHeaders.WWW_AUTHENTICATE, "basic realm=\"ums\"").build());
	}
}
