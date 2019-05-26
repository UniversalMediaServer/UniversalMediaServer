package net.pms.web.resources;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.network.UPNPHelper;
import net.pms.util.BasicPlayer.Logical;
import net.pms.util.StringUtil;

@Singleton
@Path("bump")
public class PlayerControlResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerControlResource.class);

	private int port;

	private String protocol;

	private HashMap<String, Logical> players;

	private HashMap<InetAddress, Logical> selectedPlayers;

	private String bumpAddress;

	private RendererConfiguration defaultRenderer;

	private String jsonState = "\"state\":{\"playback\":%d,\"mute\":\"%s\",\"volume\":%d,\"position\":\"%s\",\"duration\":\"%s\",\"uri\":\"%s\"}";

	@SuppressWarnings("unused")
	private File bumpjs, skindir;

	@Inject
	public PlayerControlResource(PmsConfiguration configuration) {
		port = configuration.getServerPort();
		protocol = configuration.getWebHttps() ? "https://" : "http://";
		players = new HashMap<>();
		selectedPlayers = new HashMap<>();
		String basepath = configuration.getWebPath().getPath();
		bumpjs = new File(FilenameUtils.concat(basepath, configuration.getBumpJS("bump/bump.js")));
		skindir = new File(FilenameUtils.concat(basepath, configuration.getBumpSkinDir("bump/skin")));
		bumpAddress = configuration.getBumpAddress();
		defaultRenderer = null;
	}

	@GET
	public Response bump() throws IOException {
		return Response.ok(
			ResourceUtil.read("bump/bump.html").replace("http://127.0.0.1:9001", protocol + PMS.get().getServer().getHost() + ":" + port),
			MediaType.TEXT_HTML).build();
	}

	@GET
	@Path("{path:.*}")
	public Response handle(@PathParam("path") String path, @QueryParam("uri") String uri, @QueryParam("title") String title,
		@QueryParam("vol") String vol, @Context SecurityContext context, @Context HttpServletRequest request) throws IOException {

		InetAddress address = ResourceUtil.getAddress(request);
		String[] p = path.split("/");

		String response = "";
		String mime = "text/html";
		boolean log = true;
		ArrayList<String> json = new ArrayList<>();

		String uuid = p.length > 2 ? p[2] : null;
		Logical player = uuid != null ? getPlayer(uuid) : null;

		if (player != null) {
			switch (p[1]) {
				case "status":
					// limit status updates to one per second
					UPNPHelper.sleep(1000);
					log = false;
					break;
				case "play":
					player.pressPlay(translate(uri), title);
					break;
				case "stop":
					player.pressStop();
					break;
				case "prev":
					player.prev();
					break;
				case "next":
					player.next();
					break;
				case "fwd":
					player.forward();
					break;
				case "rew":
					player.rewind();
					break;
				case "mute":
					player.mute();
					break;
				case "setvolume":
					player.setVolume(Integer.parseInt(vol));
					break;
				case "add":
					player.add(-1, translate(uri), title, null, true);
					break;
				case "remove":
					player.remove(translate(uri));
					break;
				case "clear":
					player.clear();
					break;
				case "seturi":
					player.setURI(translate(uri), title);
					break;
			}
			json.add(getPlayerState(player));
			json.add(getPlaylist(player));
			selectedPlayers.put(address, player);
		} else if (p[0].equals("renderers")) {
			json.add(getRenderers(address));
		} else if (p[0].startsWith("skin.")) {
			return Response.ok(new File(skindir, p[1].substring(5))).build();
		} else if (p[0].equals("bump.js")) {
			response = getBumpJS();
			mime = "text/javascript";
		} else {
			Format format = FormatFactory.getAssociatedFormat(p[0]);
			return Response.ok(ResourceUtil.open("bump/" + p[0]), format != null ? format.mimeType() : null).build();
		}

		if (json.size() > 0) {
			if (player != null) {
				json.add("\"uuid\":\"" + uuid + "\"");
			}
			response = "{" + StringUtils.join(json, ",") + "}";
		}

		if (log) {
			LOGGER.debug("Received http player control request from {}: {}", address, request.getRequestURI());
		}

		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		return Response.ok(bytes, mime).header("Access-Control-Allow-Origin", "*").build();
	}

	public Logical getPlayer(String uuid) {
		Logical player = players.get(uuid);
		if (player == null) {
			try {
				RendererConfiguration renderer = RendererConfiguration.getRendererConfigurationByUUID(uuid);
				if (renderer != null) {
					player = (Logical) renderer.getPlayer();
					players.put(uuid, player);
				}
			} catch (Exception e) {
				LOGGER.debug("Error retrieving player {}: {}", uuid, e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return player;
	}

	public String getPlayerState(Logical player) {
		if (player != null) {
			Logical.State state = player.getState();
			return String.format(jsonState, state.playback, state.mute, state.volume, StringUtil.shortTime(state.position, 4),
				StringUtil.shortTime(state.duration, 4),
				state.uri/* , state.metadata */);
		}
		return "";
	}

	public RendererConfiguration getDefaultRenderer() {
		if (defaultRenderer == null && bumpAddress != null) {
			try {
				InetAddress ia = InetAddress.getByName(bumpAddress);
				defaultRenderer = RendererConfiguration.getRendererConfigurationBySocketAddress(ia);
			} catch (UnknownHostException e) {
			}
		}
		return (defaultRenderer != null && !defaultRenderer.isOffline()) ? defaultRenderer : null;
	}

	public String getRenderers(InetAddress client) {
		Logical player = selectedPlayers.get(client);
		RendererConfiguration selected = player != null ? player.renderer : getDefaultRenderer();
		ArrayList<String> json = new ArrayList<>();
		for (RendererConfiguration r : RendererConfiguration.getConnectedControlPlayers()) {
			json.add(String.format("[\"%s\",%d,\"%s\"]", (r instanceof WebRender) ? r.uuid : r, r == selected ? 1 : 0, r.uuid));
		}
		return "\"renderers\":[" + StringUtils.join(json, ",") + "]";
	}

	public String getPlaylist(Logical player) {
		ArrayList<String> json = new ArrayList<>();
		Logical.Playlist playlist = player.playlist;
		playlist.validate();
		Logical.Playlist.Item selected = (Logical.Playlist.Item) playlist.getSelectedItem();
		int i;
		for (i = 0; i < playlist.getSize(); i++) {
			Logical.Playlist.Item item = (Logical.Playlist.Item) playlist.getElementAt(i);
			json.add(String.format("[\"%s\",%d,\"%s\"]", item.toString().replace("\"", "\\\""), item == selected ? 1 : 0, "$i$" + i));
		}
		return "\"playlist\":[" + StringUtils.join(json, ",") + "]";
	}

	public String getBumpJS() throws IOException {
		return ResourceUtil.read("bump/bump.js") + "\nvar bumpskin = function() {\n" + ResourceUtil.read("bump/skin/skin.js") + "\n}";
	}

	public static String translate(String uri) {
		return uri.startsWith("/play/") ? (PMS.get().getServer().getURL() + "/get/" + uri.substring(6).replace("%24", "$")) : uri;
	}

	@SuppressWarnings("unused")
	private static String getId(String uri) {
		return uri.startsWith("/play/") ? uri.substring(6) : "";
	}
}
