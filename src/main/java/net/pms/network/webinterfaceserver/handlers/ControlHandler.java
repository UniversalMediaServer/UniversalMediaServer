/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.webinterfaceserver.handlers;

import com.sun.net.httpserver.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webinterfaceserver.WebInterfaceServerHttpServerInterface;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.WebRender;
import net.pms.renderers.devices.players.LogicalPlayer;
import net.pms.renderers.devices.players.PlayerState;
import net.pms.renderers.devices.players.Playlist;
import net.pms.renderers.devices.players.PlaylistItem;
import net.pms.util.PropertiesUtil;
import net.pms.util.StringUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ControlHandler.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String JSON_STATE = "\"state\":{\"playback\":%d,\"mute\":\"%s\",\"volume\":%d,\"position\":\"%s\",\"duration\":\"%s\",\"uri\":\"%s\"}";

	private final WebInterfaceServerHttpServerInterface parent;
	private final HashMap<String, LogicalPlayer> players;
	private final HashMap<InetAddress, LogicalPlayer> selectedPlayers;
	private final String bumpAddress;
	@SuppressWarnings(value = "unused")
	private final File bumpjs;
	private final File skindir;

	private Renderer defaultRenderer;

	public ControlHandler(WebInterfaceServerHttpServerInterface parent) {
		this.parent = parent;
		players = new HashMap<>();
		selectedPlayers = new HashMap<>();
		String basepath = CONFIGURATION.getWebPath().getPath();
		bumpjs = new File(FilenameUtils.concat(basepath, CONFIGURATION.getBumpJS("util/bump/bump.js")));
		skindir = new File(FilenameUtils.concat(basepath, CONFIGURATION.getBumpSkinDir("util/bump/skin")));
		bumpAddress = CONFIGURATION.getBumpAddress();
		defaultRenderer = null;
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {
		if (WebInterfaceServerUtil.deny(httpExchange) && !WebInterfaceServerUtil.bumpAllowed(httpExchange)) {
			LOGGER.debug("Denying {}", httpExchange);
			throw new IOException("Denied");
		}
		if (LOGGER.isTraceEnabled()) {
			WebInterfaceServerUtil.logMessageReceived(httpExchange, "");
		}

		String[] p = httpExchange.getRequestURI().getPath().split("/");
		Map<String, String> query = parseQuery(httpExchange);

		String response = "";
		String mime = "text/html";
		boolean log = true;
		ArrayList<String> json = new ArrayList<>();

		String uuid = p.length > 3 ? p[3] : null;
		LogicalPlayer player = uuid != null ? getPlayer(uuid) : null;

		Headers headers = httpExchange.getResponseHeaders();
		if (player != null) {
			switch (p[2]) {
				case "status" -> {
					// limit status updates to one per second
					UMSUtils.sleep(1000);
					log = false;
				}
				case "play" -> player.pressPlay(translate(query.get("uri")), query.get("title"));
				case "stop" -> player.pressStop();
				case "prev" -> player.prev();
				case "next" -> player.next();
				case "fwd" -> player.forward();
				case "rew" -> player.rewind();
				case "mute" -> player.mute();
				case "setvolume" -> player.setVolume(Integer.parseInt(query.get("vol")));
				case "add" -> player.add(-1, translate(query.get("uri")), query.get("title"), null, true);
				case "remove" -> player.remove(translate(query.get("uri")));
				case "clear" -> player.clear();
				case "seturi" -> player.setURI(translate(query.get("uri")), query.get("title"));
				default -> {
					//nothing to do
				}
			}
			json.add(getPlayerState(player));
			json.add(getPlaylist(player));
			selectedPlayers.put(httpExchange.getRemoteAddress().getAddress(), player);
		} else if (p.length == 2) {
			HashMap<String, Object> mustacheVars = new HashMap<>();
			mustacheVars.put("serverUrl", parent.getUrl());
			mustacheVars.put("umsversion", PropertiesUtil.getProjectProperties().get("project.version"));
			response =  parent.getResources().getTemplate("util/bump/bump.html").execute(mustacheVars);
		} else if (p[2].equals("bump.js")) {
			response = getBumpJS();
			mime = "text/javascript";
			headers.add("Cache-Control", "public, max-age=604800");
		} else if (p[2].equals("renderers")) {
			json.add(getRenderers(httpExchange.getRemoteAddress().getAddress()));
		} else if (p[2].startsWith("skin.")) {
			WebInterfaceServerUtil.dumpFile(new File(skindir, p[2].substring(5)), httpExchange);
			return;
		}

		if (!json.isEmpty()) {
			mime = "application/json";
			if (player != null) {
				json.add("\"uuid\":\"" + uuid + "\"");
			}
			response = "{" + StringUtils.join(json, ",") + "}";
		}

		if (log) {
			LOGGER.debug("Received http player control request from {}: {}", httpExchange.getRemoteAddress().getAddress(), httpExchange.getRequestURI());
		}

		// w/o this client may receive response status 0 and no content
		headers.add("Access-Control-Allow-Origin", "*");

		WebInterfaceServerUtil.respond(httpExchange, response, 200, mime);
	}

	public static Map<String, String> parseQuery(HttpExchange x) {
		Map<String, String> vars = new LinkedHashMap<>();
		String raw = x.getRequestURI().getRawQuery();
		if (!StringUtils.isBlank(raw)) {
			try {
				String[] q = raw.split("&|=");
				for (int i = 0; i < q.length; i += 2) {
					vars.put(URLDecoder.decode(q[i], "UTF-8"), UMSUtils.unescape(URLDecoder.decode(q[i + 1], "UTF-8")));
				}
			} catch (UnsupportedEncodingException e) {
				LOGGER.debug("Error parsing query string '" + x.getRequestURI().getQuery() + "' :" + e);
			}
		}
		return vars;
	}

	public LogicalPlayer getPlayer(String uuid) {
		LogicalPlayer player = players.get(uuid);
		if (player == null) {
			try {
				Renderer renderer = ConnectedRenderers.getRendererByUUID(uuid);
				if (renderer != null) {
					player = (LogicalPlayer) renderer.getPlayer();
					players.put(uuid, player);
				}
			} catch (Exception e) {
				LOGGER.debug("Error retrieving player {}: {}", uuid, e.getMessage());
				LOGGER.trace("", e);
			}
		}
		return player;
	}

	public String getPlayerState(LogicalPlayer player) {
		if (player != null) {
			PlayerState state = player.getState();
			return String.format(JSON_STATE, state.getPlayback(), state.isMuted(), state.getVolume(), StringUtil.shortTime(state.getPosition(), 4), StringUtil.shortTime(state.getDuration(), 4), state.getUri()/*, state.metadata*/);
		}
		return "";
	}

	public Renderer getDefaultRenderer() {
		if (defaultRenderer == null && bumpAddress != null) {
			try {
				InetAddress ia = InetAddress.getByName(bumpAddress);
				defaultRenderer = ConnectedRenderers.getRendererBySocketAddress(ia);
			} catch (UnknownHostException e) {
				//do nothing
			}
		}
		return (defaultRenderer != null && !defaultRenderer.isOffline()) ? defaultRenderer : null;
	}

	/**
	 * Used only by old web interface.
	 * To be removed.
	 * @param client
	 * @return
	 * @deprecated
	 */
	@Deprecated
	public String getRenderers(InetAddress client) {
		LogicalPlayer player = selectedPlayers.get(client);
		Renderer selected = player != null ? player.getRenderer() : getDefaultRenderer();
		ArrayList<String> json = new ArrayList<>();
		for (Renderer r : ConnectedRenderers.getConnectedControlPlayers()) {
			json.add(String.format("[\"%s\",%d,\"%s\"]", (r instanceof WebRender) ? r.getUUID() : r, r == selected ? 1 : 0, r.getUUID()));
		}
		return "\"renderers\":[" + StringUtils.join(json, ",") + "]";
	}

	public String getPlaylist(LogicalPlayer player) {
		ArrayList<String> json = new ArrayList<>();
		Playlist playlist = player.getPlaylist();
		playlist.validate();
		PlaylistItem selected = (PlaylistItem) playlist.getSelectedItem();
		int i;
		for (i = 0; i < playlist.getSize(); i++) {
			PlaylistItem item = (PlaylistItem) playlist.getElementAt(i);
			json.add(String.format("[\"%s\",%d,\"%s\"]",
				item.toString().replace("\"", "\\\""), item == selected ? 1 : 0, "$i$" + i));
		}
		return "\"playlist\":[" + StringUtils.join(json, ",") + "]";
	}

	public String getBumpJS() {
		WebInterfaceServerUtil.ResourceManager resources = parent.getResources();
		return resources.read("util/bump/bump.js") +
			"\nvar bumpskin = function() {\n" +
			resources.read("util/bump/skin/skin.js") +
			"\n}";
	}

	public static String translate(String uri) {
		return uri.startsWith("/play/") ?
			(MediaServer.getURL() + "/get/" + uri.substring(6).replace("%24", "$")) : uri;
	}

	@SuppressWarnings("unused")
	private static String getId(String uri) {
		return uri.startsWith("/play/") ? uri.substring(6) : "";
	}
}
