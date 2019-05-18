package net.pms.web.resources;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.dlna.CodeEnter;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.Playlist;
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.formats.Format;
import net.pms.util.UMSUtils;
import net.pms.web.model.Roots;
import net.pms.web.model.TemplateService;

@Singleton
@Path("browse")
public class BrowseResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BrowseResource.class);

	private static PmsConfiguration configuration = PMS.getConfiguration();
	
	@Context
	private SecurityContext securityContext;
	
	private Roots roots;

	private TemplateService templates;
	
	@Inject
	public BrowseResource(Roots roots, TemplateService templates) {
		this.roots = roots;
		this.templates = templates;
	}

	@GET
	@Path("{path:.*}")
	public Response browse(@PathParam("path") String id, @HeaderParam("Accept-Language") String acceptLanguageHeader,
			@QueryParam("str") String searchTerm, @Context HttpServletRequest request) throws Exception {
		try {
			String language = ResourceUtil.getFirstSupportedLanguage(acceptLanguageHeader);
			// if (RemoteUtil.deny(t)) {
			// throw new IOException("Access denied");
			// }
			LOGGER.debug("Got a browse request found id " + id);
			return mkBrowsePage(id, searchTerm, language, request);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteBrowseHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
			throw e;
		}
	}

	private Response mkBrowsePage(String id, String search, String language, HttpServletRequest request) throws IOException, InterruptedException {
		LOGGER.debug("Make browse page " + id);
		String user = ResourceUtil.getUserName(securityContext);
		RootFolder root = roots.getRoot(user, true, request);

		List<DLNAResource> resources = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
		boolean upnpAllowed = ResourceUtil.bumpAllowed(request);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		if (!resources.isEmpty() && resources.get(0).getParent() != null
				&& (resources.get(0).getParent() instanceof CodeEnter)) {
			// this is a code folder the search string is entered code
			CodeEnter ce = (CodeEnter) resources.get(0).getParent();
			ce.setEnteredCode(search);
			if (!ce.validCode(ce)) {
				// invalid code throw error
				throw new IOException("Auth error");
			}
			DLNAResource real = ce.getResource();
			if (!real.isFolder()) {
				// no folder -> redirect
				return Response.temporaryRedirect(URI.create("/play/" + real.getId())).build();
			}
			// redirect to ourself
			return Response.temporaryRedirect(URI.create("/browse/" + real.getResourceId())).build();
		}
		if (StringUtils.isNotEmpty(search) && !(resources instanceof CodeEnter)) {
			UMSUtils.postSearch(resources, search);
		}

		boolean hasFile = false;

		ArrayList<String> folders = new ArrayList<>();
		ArrayList<HashMap<String, String>> media = new ArrayList<>();
		StringBuilder sb = new StringBuilder();

		String backUri = "javascript:history.back()";
		if (!resources.isEmpty() && resources.get(0).getParent() != null
				&& resources.get(0).getParent().getParent() != null
				&& resources.get(0).getParent().getParent().isFolder()) {
			String newId = resources.get(0).getParent().getParent().getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8");
			backUri = "/browse/" + idForWeb;
		}

		sb.setLength(0);
		sb.append("<a href=\"").append(backUri).append("\" title=\"").append(ResourceUtil.getMsgString("Web.10", language))
		.append("\">");
		sb.append("<span>").append(ResourceUtil.getMsgString("Web.10", language)).append("</span>");
		sb.append("</a>");
		folders.add(sb.toString());

		// Generate innerHtml snippets for folders and media items
		for (DLNAResource resource : resources) {
			String newId = resource.getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8");
			String thumb = "/thumb/" + idForWeb;
			String name = StringEscapeUtils.escapeHtml(resource.resumeName());

			if (resource instanceof VirtualVideoAction) {
				// Let's take the VVA real early
				sb.setLength(0);
				HashMap<String, String> item = new HashMap<>();
				sb.append("<a href=\"#\" onclick=\"umsAjax('/play/").append(idForWeb)
				.append("', true);return false;\" title=\"").append(name).append("\">")
				.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name)
				.append("\">").append("</a>");
				item.put("thumb", sb.toString());
				sb.setLength(0);
				sb.append("<a href=\"#\" onclick=\"umsAjax('/play/").append(idForWeb)
				.append("', true);return false;\" title=\"").append(name).append("\">")
				.append("<span class=\"caption\">").append(name).append("</span>").append("</a>");
				item.put("caption", sb.toString());
				item.put("bump", "<span class=\"floatRight\"></span>");
				media.add(item);
				hasFile = true;
				continue;
			}

			if (resource.isFolder()) {
				sb.setLength(0);
				// The resource is a folder
				String p = "/browse/" + idForWeb;
				boolean code = (resource instanceof CodeEnter);
				String txt = ResourceUtil.getMsgString("Web.8", language);
				if (code) {
					txt = ResourceUtil.getMsgString("Web.9", language);
				}
				if (resource.getClass().getName().contains("SearchFolder") || code) {
					// search folder add a prompt
					// NOTE!!!
					// Yes doing getClass.getname is REALLY BAD, but this
					// is to make legacy plugins utilize this function as well
					sb.append("<a href=\"javascript:void(0);\" onclick=\"searchFun('").append(p).append("','")
					.append(txt).append("');\" title=\"").append(name).append("\">");
				} else {
					sb.append("<a href=\"").append(p).append("\" oncontextmenu=\"searchFun('").append(p).append("','")
					.append(txt).append("');\" title=\"").append(name).append("\">");
				}
				sb.append("<span>").append(name).append("</span>");
				sb.append("</a>");
				folders.add(sb.toString());
			} else {
				// The resource is a media file
				sb.setLength(0);
				HashMap<String, String> item = new HashMap<>();
				if (upnpAllowed) {
					if (upnpControl) {
						sb.append("<a class=\"bumpIcon\" href=\"javascript:bump.start('//").append(getAddress(request))
						.append("','/play/").append(idForWeb).append("','").append(name.replace("'", "\\'"))
						.append("')\" title=\"").append(ResourceUtil.getMsgString("Web.1", language)).append("\"></a>");
					} else {
						sb.append("<a class=\"bumpIcon icondisabled\" href=\"javascript:notify('warn','")
						.append(ResourceUtil.getMsgString("Web.2", language)).append("')\" title=\"")
						.append(ResourceUtil.getMsgString("Web.3", language)).append("\"></a>");
					}
					if (resource.getParent() instanceof Playlist) {
						sb.append("\n<a class=\"playlist_del\" href=\"#\" onclick=\"umsAjax('/playlist/del/")
						.append(idForWeb).append("', true);return false;\" title=\"")
						.append(ResourceUtil.getMsgString("Web.4", language)).append("\"></a>");
					} else {
						sb.append("\n<a class=\"playlist_add\" href=\"#\" onclick=\"umsAjax('/playlist/add/")
						.append(idForWeb).append("', false);return false;\" title=\"")
						.append(ResourceUtil.getMsgString("Web.5", language)).append("\"></a>");
					}
				} else {
					// ensure that we got a string
					sb.append("");
				}

				item.put("bump", sb.toString());
				sb.setLength(0);

				if (WebRender.supports(resource) || resource.isResume() || resource.getType() == Format.IMAGE) {
					sb.append("<a href=\"/play/").append(idForWeb).append("\" title=\"").append(name).append("\">")
					.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name)
					.append("\">").append("</a>");
					item.put("thumb", sb.toString());
					sb.setLength(0);
					sb.append("<a href=\"/play/").append(idForWeb).append("\" title=\"").append(name).append("\">")
					.append("<span class=\"caption\">").append(name).append("</span>").append("</a>");
					item.put("caption", sb.toString());
				} else if (upnpControl && upnpAllowed) {
					// Include it as a web-disabled item so it can be thrown via upnp
					sb.append("<a class=\"webdisabled\" href=\"javascript:notify('warn','")
					.append(ResourceUtil.getMsgString("Web.6", language)).append("')\"").append(" title=\"").append(name)
					.append(' ').append(ResourceUtil.getMsgString("Web.7", language)).append("\">")
					.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name)
					.append("\">").append("</a>");
					item.put("thumb", sb.toString());
					sb.setLength(0);
					sb.append("<span class=\"webdisabled caption\">").append(name).append("</span>");
					item.put("caption", sb.toString());
				}
				media.add(item);
				hasFile = true;
			}
		}

		HashMap<String, Object> vars = new HashMap<>();
		vars.put("name", id.equals("0") ? configuration.getServerDisplayName()
				: StringEscapeUtils.escapeHtml(root.getDLNAResource(id, null).getDisplayName()));
		vars.put("hasFile", hasFile);
		vars.put("folders", folders);
		vars.put("media", media);
		if (configuration.useWebControl()) {
			vars.put("push", true);
		}

		return Response.ok(templates.getTemplate("browse.html").execute(vars), MediaType.TEXT_HTML_TYPE).build();
	}
	
	private String getAddress(HttpServletRequest req) {
		URI uri = URI.create(req.getRequestURL().toString());
		return uri.getHost() + uri.getPort();
	}
}
