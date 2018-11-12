package net.pms.remote;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private RemoteWeb parent;
	private static PmsConfiguration configuration = PMS.getConfiguration();

	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String mkBrowsePage(String id, HttpExchange t) throws IOException {
		LOGGER.debug("Make browse page " + id);
		String user = RemoteUtil.userName(t);
		RootFolder root = parent.getRoot(user, true, t);
		String search = RemoteUtil.getQueryVars(t.getRequestURI().getQuery(), "str");

		List<DLNAResource> resources = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
		boolean upnpAllowed = RemoteUtil.bumpAllowed(t);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		if (!resources.isEmpty() &&
			resources.get(0).getParent() != null &&
			(resources.get(0).getParent() instanceof CodeEnter)) {
			// this is a code folder the search string is  entered code
			CodeEnter ce = (CodeEnter) resources.get(0).getParent();
			ce.setEnteredCode(search);
			if(!ce.validCode(ce)) {
				// invalid code throw error
				throw new IOException("Auth error");
			}
			DLNAResource real = ce.getResource();
			if (!real.isFolder()) {
				// no folder   -> redirect
				Headers hdr = t.getResponseHeaders();
				hdr.add("Location", "/play/" + real.getId());
				RemoteUtil.respond(t, "", 302, "text/html");
				// return null here to avoid multiple responses
				return null;
			}
			// redirect to ourself
			Headers hdr = t.getResponseHeaders();
			hdr.add("Location", "/browse/" + real.getResourceId());
			RemoteUtil.respond(t, "", 302, "text/html");
			return null;
		}
		if (StringUtils.isNotEmpty(search) && !(resources instanceof CodeEnter)) {
			UMSUtils.postSearch(resources, search);
		}

		boolean hasFile = false;

		ArrayList<String> folders = new ArrayList<>();
		ArrayList<HashMap<String, String>> media = new ArrayList<>();
		StringBuilder sb = new StringBuilder();

		sb.setLength(0);
		sb.append("<a href=\"javascript:history.back()\" title=\"").append(RemoteUtil.getMsgString("Web.10", t)).append("\">");
		sb.append("<span>").append(RemoteUtil.getMsgString("Web.10", t)).append("</span>");
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
						.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">")
						.append("</a>");
				item.put("thumb", sb.toString());
				sb.setLength(0);
				sb.append("<a href=\"#\" onclick=\"umsAjax('/play/").append(idForWeb)
						.append("', true);return false;\" title=\"").append(name).append("\">")
						.append("<span class=\"caption\">").append(name).append("</span>")
						.append("</a>");
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
				String txt = RemoteUtil.getMsgString("Web.8", t);
				if (code) {
					txt = RemoteUtil.getMsgString("Web.9", t);
				}
				if (resource.getClass().getName().contains("SearchFolder") || code) {
					// search folder add a prompt
					// NOTE!!!
					// Yes doing getClass.getname is REALLY BAD, but this
					// is to make legacy plugins utilize this function as well
					sb.append("<a href=\"javascript:void(0);\" onclick=\"searchFun('").append(p).append("','")
					   .append(txt).append("');\" title=\"").append(name).append("\">");
				} else {
					sb.append("<a href=\"").append(p).append("\" oncontextmenu=\"searchFun('").append(p)
					  .append("','").append(txt).append("');\" title=\"").append(name).append("\">");
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
						sb.append("<a class=\"bumpIcon\" href=\"javascript:bump.start('//")
							.append(parent.getAddress()).append("','/play/").append(idForWeb).append("','")
							.append(name.replace("'", "\\'")).append("')\" title=\"")
							.append(RemoteUtil.getMsgString("Web.1", t)).append("\"></a>");
					} else {
						sb.append("<a class=\"bumpIcon icondisabled\" href=\"javascript:notify('warn','")
						   .append(RemoteUtil.getMsgString("Web.2", t))
						   .append("')\" title=\"").append(RemoteUtil.getMsgString("Web.3", t)).append("\"></a>");
					}
					if (resource.getParent() instanceof Playlist) {
						sb.append("\n<a class=\"playlist_del\" href=\"#\" onclick=\"umsAjax('/playlist/del/")
							.append(idForWeb).append("', true);return false;\" title=\"")
						    .append(RemoteUtil.getMsgString("Web.4", t)).append("\"></a>");
					} else {
						sb.append("\n<a class=\"playlist_add\" href=\"#\" onclick=\"umsAjax('/playlist/add/")
							.append(idForWeb).append("', false);return false;\" title=\"")
						    .append(RemoteUtil.getMsgString("Web.5", t)).append("\"></a>");
					}
				} else {
					// ensure that we got a string
					sb.append("");
				}

				item.put("bump", sb.toString());
				sb.setLength(0);

				if (WebRender.supports(resource) || resource.isResume() || resource.getType() == Format.IMAGE) {
					sb.append("<a href=\"/play/").append(idForWeb)
						.append("\" title=\"").append(name).append("\">")
						.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">")
						.append("</a>");
					item.put("thumb", sb.toString());
					sb.setLength(0);
					sb.append("<a href=\"/play/").append(idForWeb)
						.append("\" title=\"").append(name).append("\">")
						.append("<span class=\"caption\">").append(name).append("</span>")
						.append("</a>");
					item.put("caption", sb.toString());
				} else if (upnpControl && upnpAllowed) {
					// Include it as a web-disabled item so it can be thrown via upnp
					sb.append("<a class=\"webdisabled\" href=\"javascript:notify('warn','")
						.append(RemoteUtil.getMsgString("Web.6", t)).append("')\"")
						.append(" title=\"").append(name).append(' ').append(RemoteUtil.getMsgString("Web.7", t)).append("\">")
						.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">")
						.append("</a>");
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
		vars.put("name", id.equals("0") ? configuration.getServerDisplayName() :
			StringEscapeUtils.escapeHtml(root.getDLNAResource(id, null).getDisplayName()));
		vars.put("hasFile", hasFile);
		vars.put("folders", folders);
		vars.put("media", media);
		if (configuration.useWebControl()) {
			vars.put("push", true);
		}

		return parent.getResources().getTemplate("browse.html").execute(vars);
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		try {
			if (RemoteUtil.deny(t)) {
				throw new IOException("Access denied");
			}
			String id = RemoteUtil.getId("browse/", t);
			LOGGER.debug("Got a browse request found id " + id);
			String response = mkBrowsePage(id, t);
			LOGGER.trace("Browse page:\n{}", response);
			RemoteUtil.respond(t, response, 200, "text/html");
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			// Nothing should get here, this is just to avoid crashing the thread
			LOGGER.error("Unexpected error in RemoteBrowseHandler.handle(): {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}
}
