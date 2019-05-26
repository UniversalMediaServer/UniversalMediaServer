package net.pms.web.resources;

import java.io.IOException;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.samskivert.mustache.MustacheException;
import net.pms.configuration.PmsConfiguration;
import net.pms.web.services.TemplateService;

@Singleton
@Path("/")
public class StartResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(StartResource.class);

	private TemplateService templates;

	private PmsConfiguration configuration;

	@Inject
	public StartResource(TemplateService templates, PmsConfiguration configuration) {
		this.templates = templates;
		this.configuration = configuration;
	}

	@GET
	public Response start() throws IOException {
		HashMap<String, Object> vars = new HashMap<>();
		vars.put("serverName", configuration.getServerDisplayName());

		try {
			String start = templates.getTemplate("start.html").execute(vars);
			return Response.ok(start, MediaType.TEXT_HTML).build();
		} catch (MustacheException e) {
			LOGGER.error("An error occurred while generating a HTTP response: {}", e.getMessage());
			LOGGER.trace("", e);
			throw e;
		}
	}

	@GET
	@Path("favicon")
	public Response favicon() {
		return Response.ok(getClass().getResourceAsStream("/web/favicon.ico"), "image/x-icon").build();
	}
}
