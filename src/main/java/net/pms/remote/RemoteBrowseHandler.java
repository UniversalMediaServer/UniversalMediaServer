package net.pms.remote;

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
import net.pms.dlna.RootFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.util.UMSUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteBrowseHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(RemoteBrowseHandler.class);
	private RemoteWeb parent;
	private static PmsConfiguration configuration = PMS.getConfiguration();

	public RemoteBrowseHandler(RemoteWeb parent) {
		this.parent = parent;
	}

	private String mkBrowsePage(String id, HttpExchange t) throws IOException {
		String user = RemoteUtil.userName(t);
		RootFolder root = parent.getRoot(user, true, t);
		String search = RemoteUtil.getQueryVars(t.getRequestURI().getQuery(), "str");

		List<DLNAResource> res = root.getDLNAResources(id, true, 0, 0, root.getDefaultRenderer(), search);
		boolean upnpAllowed = RemoteUtil.bumpAllowed(PMS.getConfiguration().getBumpAllowedIps(), t);
		boolean upnpControl = RendererConfiguration.hasConnectedControlPlayers();
		if (StringUtils.isNotEmpty(search) && !(res instanceof CodeEnter)) {
			UMSUtils.postSearch(res, search);
		}

		boolean showFolders = false;
		boolean hasFile     = false;

		ArrayList<String> folders = new ArrayList<>();
		ArrayList<HashMap<String, String>> media = new ArrayList<>();
		StringBuilder sb = new StringBuilder();

		// Generate innerHtml snippets for folders and media items
		for (DLNAResource r : res) {
			String newId = r.getResourceId();
			String idForWeb = URLEncoder.encode(newId, "UTF-8");
			String thumb = "/thumb/" + idForWeb;
			String name = StringEscapeUtils.escapeHtml(r.resumeName());

			if (r.isFolder()) {
				sb.setLength(0);
				// The resource is a folder
				String p = "/browse/" + idForWeb;
				if (r.getClass().getName().contains("SearchFolder")) {
					// search folder add a prompt
					// NOTE!!!
					// Yes doing getClass.getname is REALLY BAD, but this
					// is to make legacy plugins utilize this function as well
					sb.append("<a href=\"javascript:void(0);\" onclick=\"searchFun('").append(p).append("');\" title=\"").append(name).append("\">");
				} else {
					sb.append("<a href=\"/browse/").append(idForWeb).append("\" oncontextmenu=\"searchFun('").append(p).append("');\" title=\"").append(name).append("\">");
				}
				sb.append("<span>").append(name).append("</span>");
				sb.append("</a>");
				folders.add(sb.toString());
				showFolders = true;
			} else {
				// The resource is a media file
				sb.setLength(0);
				HashMap<String, String> item = new HashMap<>();
				if (upnpAllowed && !(r instanceof VirtualVideoAction)) {
					// VVAs aren't bumpable
					if (upnpControl) {
						sb.append("<a class=\"bumpIcon\" href=\"javascript:bump.start('//")
							.append(parent.getAddress()).append("','/play/").append(idForWeb).append("','")
							.append(name.replace("'", "\\'")).append("')\" title=\"Play on another renderer\"></a>");
					} else {
						sb.append("<a class=\"bumpIcon icondisabled\" href=\"javascript:alert('").append("No upnp-controllable renderers suitable for receiving pushed media are available. Refresh this page if a new renderer may have recently connected.")
							.append("')\" title=\"No other renderers available\"></a>");
					}
					if (!PMS.get().isInDynPls(r)) {
						sb.append("\n<a class=\"playlist_add\" href=\"/playlist/add/")
								.append(idForWeb).append("\" title=\"Add to playlist\"></a>");
					} else {
						sb.append("\n<a class=\"playlist_del\" href=\"/playlist/del/")
								.append(idForWeb).append("\" title=\"Remove from playlist\"></a>");
					}
				} else {
					// ensure that we got a string
					sb.append("");
				}

				item.put("bump", sb.toString());
				sb.setLength(0);

				if (WebRender.supports(r)) {
					sb.append("<a href=\"/play/").append(idForWeb)
						.append("\" title=\"").append(name).append("\" id=\"").append(idForWeb).append("\">")
						.append("<img class=\"thumb\" src=\"").append(thumb).append("\" alt=\"").append(name).append("\">")
						.append("</a>");
					item.put("thumb", sb.toString());
					sb.setLength(0);
					sb.append("<a href=\"/play/").append(idForWeb)
						.append("\" title=\"").append(name).append("\" id=\"").append(idForWeb).append("\">")
						.append("<span class=\"caption\">").append(name).append("</span>")
						.append("</a>");
					item.put("caption", sb.toString());
				} else if (upnpControl && upnpAllowed) {
					// Include it as a web-disabled item so it can be thrown via upnp
					sb.append("<a class=\"webdisabled\" href=\"javascript:alert('This item not playable via browser but can be sent to other renderers.')\"")
						.append(" title=\"").append(name).append(" (NOT PLAYABLE IN BROWSER)\">")
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
		vars.put("name", id.equals("0") ? configuration.getServerName() :
			StringEscapeUtils.escapeHtml(root.getDLNAResource(id, null).getDisplayName()));
		vars.put("hasFile", hasFile);
		vars.put("noFoldersCSS", showFolders ? "" : " class=\"noFolders\"");
		vars.put("folders", folders);
		vars.put("media", media);

		return parent.getResources().getTemplate("browse.html").execute(vars);
	}

	@Override
	public void handle(HttpExchange t) throws IOException {
		LOGGER.debug("Got a browse request " + t.getRequestURI());
		if (RemoteUtil.deny(t)) {
			throw new IOException("Access denied");
		}
		String id = RemoteUtil.getId("browse/", t);
		LOGGER.debug("Found id " + id);
		String response = mkBrowsePage(id, t);
		LOGGER.debug("Write page " + response);
		RemoteUtil.respond(t, response, 200, "text/html");
	}
}
