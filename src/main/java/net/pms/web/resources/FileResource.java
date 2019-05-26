package net.pms.web.resources;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.util.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.network.HTTPResource;
import net.pms.remote.RemoteUtil;
import net.pms.remote.RemoteUtil.ResourceManager;
import net.pms.web.services.TemplateService;

@Singleton
@Path("files")
public class FileResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(FileResource.class);

	private TemplateService templates;

	private ResourceManager resourceManager;

	@Inject
	public FileResource(ResourceManager resourceManager, TemplateService templates) {
		this.resourceManager = resourceManager;
		this.templates = templates;
	}

	@GET
	@Path("crossdomain.xml")
	public Response getCrossdomain() {
		return Response.ok(resourceManager.getResourceAsStream("/net/pms/web/resources/crossdomain.xml"), MediaType.APPLICATION_XML_TYPE)
			.build();
	}

	@GET
	@Path("log/info")
	public Response getInfoLog() {
		String log = PMS.get().getFrame().getLog();
		log = log.replaceAll("\n", "<br>");
		String fullLink = "<br><a href=\"/files/log/full\">Full log</a><br><br>";
		String x = fullLink + log;
		if (StringUtils.isNotEmpty(log)) {
			x = x + fullLink;
		}
		return Response.ok("<html><title>UMS LOG</title><body>" + x + "</body></html>", MediaType.TEXT_HTML_TYPE).build();
	}

	@GET
	@Path("log/{path:.*}")
	public Response getAbritraryLog(@PathParam("path") String filename) {
		File file = resourceManager.getFile(filename);
		if (file != null) {
			filename = file.getName();
			HashMap<String, Object> vars = new HashMap<>();
			vars.put("title", filename);
			vars.put("brush", filename.endsWith("debug.log") ? "debug_log" : filename.endsWith(".log") ? "log" : "conf");
			vars.put("log", RemoteUtil.read(file).replace("<", "&lt;"));
			return Response.ok(templates.getTemplate("log.html").execute(vars), MediaType.TEXT_HTML).build();
		} else {
			throw new NotFoundException("Unable to find " + filename);
		}
	}

	@POST
	@Path("proxy")
	public Response postProxy(@Context HttpHeaders headers, @Context HttpServletRequest request, InputStream body) throws IOException {
		String url = request.getQueryString();
		CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
		if (cookieManager == null) {
			cookieManager = new CookieManager();
			CookieHandler.setDefault(cookieManager);
		}

		String str = IO.toString(body);

		URLConnection conn = new URL(url).openConnection();
		((HttpURLConnection) conn).setRequestMethod("POST");
		conn.setRequestProperty("Content-type", headers.getHeaderString("Content-type"));
		conn.setRequestProperty("Content-Length", String.valueOf(str.length()));
		conn.setDoOutput(true);
		OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
		writer.write(str);
		writer.flush();

		if (LOGGER.isDebugEnabled()) {
			List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
			for (HttpCookie cookie : cookies) {
				LOGGER.debug("Domain: {}, Cookie: {}", cookie.getDomain(), cookie);
			}
		}

		return createResponse(conn.getInputStream());
	}

	@OPTIONS
	@Path("proxy")
	public Response proxyOptions() throws UnsupportedEncodingException {
		CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
		if (cookieManager == null) {
			cookieManager = new CookieManager();
			CookieHandler.setDefault(cookieManager);
		}

		return createResponse(new ByteArrayInputStream("".getBytes("utf-8")));
	}

	@GET
	@Path("proxy")
	public Response proxyGet(@Context HttpServletRequest request) throws IOException {
		String url = request.getQueryString();
		CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
		if (cookieManager == null) {
			cookieManager = new CookieManager();
			CookieHandler.setDefault(cookieManager);
		}

		InputStream in = HTTPResource.downloadAndSend(url, false);

		if (LOGGER.isDebugEnabled()) {
			List<HttpCookie> cookies = cookieManager.getCookieStore().getCookies();
			for (HttpCookie cookie : cookies) {
				LOGGER.debug("Domain: {}, Cookie: {}", cookie.getDomain(), cookie);
			}
		}

		return createResponse(in);
	}

	@GET
	@Path("{path:.*}")
	public Response resource(@PathParam("path") String filename) {
		File file = resourceManager.getFile(filename);
		// TODO
		if (file != null) {
			return Response.ok(file).build();
		}

		InputStream stream = resourceManager.getResourceAsStream(filename);
		if (stream != null) {
			return Response.ok(stream).build();
		}
		throw new NotFoundException();
	}

	private Response createResponse(InputStream in) {
		LOGGER.trace("input is {}", in);
		return Response.ok(in, MediaType.TEXT_PLAIN).header("Access-Control-Allow-Origin", "*")
			.header("Access-Control-Allow-Headers", "User-Agent").header("Access-Control-Allow-Headers", "Content-Type").build();
	}
}
