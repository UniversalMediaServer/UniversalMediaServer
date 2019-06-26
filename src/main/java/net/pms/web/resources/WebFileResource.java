package net.pms.web.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;

@Named
@Singleton
@Path("/")
public class WebFileResource {

	private final String root;

	@Inject
	public WebFileResource(@Named("ums.web.root") String root) {
		if (root.endsWith("/")) {
			root = root.substring(0, root.length() - 1);
		}
		if (root.startsWith("/")) {
			root = root.substring(1);
		}
		this.root = root;
	}

	@GET
	@Path("{path:.*}")
	public Response getRootFile(@PathParam("path") String path) {
		if ("".equals(path)) {
			return serve("index.html");
		}
		return serve(path);
	}

	private Response serve(String path) {
		final StringBuilder sb = new StringBuilder("/").append(root).append("/");
		sb.append(path);
// TODO mime type
		return Response.ok(new StreamingOutput() {

			public void write(OutputStream output) throws IOException, WebApplicationException {
				InputStream in = null;
				try {
					in = getClass().getResourceAsStream(sb.toString());
					if (in == null) {
						throw new NotFoundException();
					}
					IOUtils.copy(in, output);
				} finally {
					IOUtils.closeQuietly(in);
				}
				/// write
			}
		}).build();
	}
}
