/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.network.webinterfaceserver.handlers;

import com.sun.net.httpserver.*;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.configuration.WebRender;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.UPNPHelper;
import net.pms.util.BasicPlayer.Logical;
import net.pms.util.StringUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServerUtil;
import net.pms.network.webinterfaceserver.WebInterfaceServer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ControlHandler implements HttpHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ControlHandler.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	private static final String JSON_STATE = "\"state\":{\"playback\":%d,\"mute\":\"%s\",\"volume\":%d,\"position\":\"%s\",\"duration\":\"%s\",\"uri\":\"%s\"}";

	private final WebInterfaceServer parent;
	private final HashMap<String, Logical> players;
	private final HashMap<InetAddress, Logical> selectedPlayers;
	private final String bumpAddress;
	@SuppressWarnings(value = "unused")
	private final File bumpjs;
	private final File skindir;

	private RendererConfiguration defaultRenderer;

	public ControlHandler(WebInterfaceServer parent) {
		this.parent = parent;
		players = new HashMap<>();
		selectedPlayers = new HashMap<>();
		String basepath = CONFIGURATION.getWebPath().getPath();
		bumpjs = new File(FilenameUtils.concat(basepath, CONFIGURATION.getBumpJS("bump/bump.js")));
		skindir = new File(FilenameUtils.concat(basepath, CONFIGURATION.getBumpSkinDir("bump/skin")));
		bumpAddress = CONFIGURATION.getBumpAddress();
		defaultRenderer = null;
	}

	@Override
	public void handle(HttpExchange httpExchange) throws IOException {

		if (WebInterfaceServerUtil.deny(httpExchange) && !WebInterfaceServerUtil.bumpAllowed(httpExchange)) {
			LOGGER.debug("Denying {}", httpExchange);
			throw new IOException("Denied");
		}

		String[] p = httpExchange.getRequestURI().getPath().split("/");
		Map<String, String> query = parseQuery(httpExchange);

		String response = "";
		String mime = "text/html";
		boolean log = true;
		ArrayList<String> json = new ArrayList<>();

		String uuid = p.length > 3 ? p[3] : null;
		Logical player = uuid != null ? getPlayer(uuid) : null;

		if (player != null) {
			switch (p[2]) {
				case "status":
					// limit status updates to one per second
					UPNPHelper.sleep(1000);
					log = false;
					break;
				case "play":
					player.pressPlay(translate(query.get("uri")), query.get("title"));
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
					player.setVolume(Integer.parseInt(query.get("vol")));
					break;
				case "add":
					player.add(-1, translate(query.get("uri")), query.get("title"), null, true);
					break;
				case "remove":
					player.remove(translate(query.get("uri")));
					break;
				case "clear":
					player.clear();
					break;
				case "seturi":
					player.setURI(translate(query.get("uri")), query.get("title"));
					break;
			}
			json.add(getPlayerState(player));
			json.add(getPlaylist(player));
			selectedPlayers.put(httpExchange.getRemoteAddress().getAddress(), player);
		} else if (p.length == 2) {
			response = parent.getResources().read("bump/bump.html")
				.replace("http://127.0.0.1:9001", parent.getUrl());
		} else if (p[2].equals("bump.js")) {
			response = getBumpJS();
			mime = "text/javascript";
		} else if (p[2].equals("renderers")) {
			json.add(getRenderers(httpExchange.getRemoteAddress().getAddress()));
		} else if (p[2].startsWith("skin.")) {
			WebInterfaceServerUtil.dumpFile(new File(skindir, p[2].substring(5)), httpExchange);
			return;
		}

		if (!json.isEmpty()) {
			if (player != null) {
				json.add("\"uuid\":\"" + uuid + "\"");
			}
			response = "{" + StringUtils.join(json, ",") + "}";
		}

		if (log) {
			LOGGER.debug("Received http player control request from {}: {}", httpExchange.getRemoteAddress().getAddress(), httpExchange.getRequestURI());
		}

		Headers headers = httpExchange.getResponseHeaders();
		headers.add("Content-Type", mime);
		// w/o this client may receive response status 0 and no content
		headers.add("Access-Control-Allow-Origin", "*");

		byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
		httpExchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream o = httpExchange.getResponseBody()) {
			o.write(bytes);
		}
	}

	public static Map<String, String> parseQuery(HttpExchange x) {
		Map<String, String> vars = new LinkedHashMap<>();
		String raw = x.getRequestURI().getRawQuery();
		if (!StringUtils.isBlank(raw)) {
			try {
				String[] q = raw.split("&|=");
				for (int i = 0; i < q.length; i += 2) {
					vars.put(URLDecoder.decode(q[i], "UTF-8"), UPNPHelper.unescape(URLDecoder.decode(q[i + 1], "UTF-8")));
				}
			} catch (UnsupportedEncodingException e) {
				LOGGER.debug("Error parsing query string '" + x.getRequestURI().getQuery() + "' :" + e);
			}
		}
		return vars;
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
			return String.format(JSON_STATE, state.playback, state.mute, state.volume, StringUtil.shortTime(state.position, 4), StringUtil.shortTime(state.duration, 4), state.uri/*, state.metadata*/);
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
			json.add(String.format("[\"%s\",%d,\"%s\"]",
				item.toString().replace("\"", "\\\""), item == selected ? 1 : 0, "$i$" + i));
		}
		return "\"playlist\":[" + StringUtils.join(json, ",") + "]";
	}

	public String getBumpJS() {
		WebInterfaceServerUtil.ResourceManager resources = parent.getResources();
		return resources.read("bump/bump.js") +
			"\nvar bumpskin = function() {\n" +
			resources.read("bump/skin/skin.js") +
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
