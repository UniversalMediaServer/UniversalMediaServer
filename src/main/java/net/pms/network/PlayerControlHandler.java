package net.pms.network;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;

public class PlayerControlHandler implements HttpHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlayerControlHandler.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	private int port;
	private HashMap<String,UPNPHelper.Player> players;
	private String jsonState = "\"state\":{\"playback\":%d,\"mute\":\"%s\",\"volume\":%d,\"position\":\"%s\",\"duration\":\"%s\",\"uri\":\"%s\"}";

	public PlayerControlHandler(HttpServer server) {
		if (server == null) {
			server = createServer(9009);
		}
		server.createContext("/bump", this);
		port = server.getAddress().getPort();
		players = new HashMap();
	}

	@Override
	public void handle(HttpExchange x) throws IOException {

		String[] p = x.getRequestURI().getPath().split("/");
		String response = "";
		String mime = "text/html";
		boolean log = true;
		ArrayList<String> json = new ArrayList<>();

		UPNPHelper.Player player = p.length > 3 ? getPlayer(p[3]) : null;

		if (player != null) {
			if (p[2].equals("status")) {
				// limit status updates to one per second
				UPNPHelper.sleep(1000);
				log = false;
			} else if (p[2].equals("play")) {
				Map<String,String> q = parseQuery(x);
				player.pressPlay(q.get("uri"), q.get("title"));
			} else if (p[2].equals("stop")) {
				player.stop();
			} else if (p[2].equals("prev")) {
				player.prev();
			} else if (p[2].equals("next")) {
				player.next();
			} else if (p[2].equals("fwd")) {
				player.forward();
			} else if (p[2].equals("rew")) {
				player.rewind();
			} else if (p[2].equals("mute")) {
				// TODO
			} else if (p[2].equals("setvolume")) {
				// TODO
			} else if (p[2].equals("seturi")) {
				Map<String,String> q = parseQuery(x);
				player.setURI(q.get("uri"), q.get("title"));
			}
			json.add(getPlayerState(player));
			json.add(getPlaylist(player));
		} else if (p.length == 2) {
			response = read(configuration.getWebFile("bump.html"))
				.replace("127.0.0.1", PMS.get().getServer().getHost())
				.replace("9001", String.valueOf(port));
		} else if (p[2].equals("bump.js")) {
			response = read(configuration.getWebFile("bump.js"));
			mime = "text/javascript";
		} else if (p[2].equals("renderers")) {
			json.add(getRenderers());
		}

		if (json.size() > 0) {
			response = "{" + StringUtils.join(json, ",") + "}";
		}

		if (log) {
			LOGGER.debug("Received http player control request from " + x.getRemoteAddress().getAddress() + ": "+ x.getRequestURI());
		}

		Headers headers = x.getResponseHeaders();
		headers.add("Content-Type", mime);
		// w/o this client may receive response status 0 and no content
		headers.add("Access-Control-Allow-Origin", "*");

		byte[] bytes = response.getBytes();
		x.sendResponseHeaders(200, bytes.length);
		OutputStream o = x.getResponseBody();
		o.write(bytes);
		o.close();
	}

	public String getAddress() {
		return PMS.get().getServer().getHost() + ":" + port;
	}

	public UPNPHelper.Player getPlayer(String address) {
		UPNPHelper.Player player = players.get(address);
		if (player == null) {
			try {
				InetAddress socket = InetAddress.getByName(address);
				RendererConfiguration r = RendererConfiguration.getRendererConfigurationBySocketAddress(socket);
				player = r.getPlayer();
				players.put(address, player);
			} catch (Exception e) {
				LOGGER.debug("Error retrieving player: " + e);
			}
		}
		return player;
	}

	public String getPlayerState(UPNPHelper.Player player) {
		if (player != null) {
			UPNPHelper.Player.State state = player.getState();
			return String.format(jsonState, state.playback, state.mute, state.volume, state.position, state.duration, state.uri/*, state.metadata*/);
		}
		return "";
	}

	public String getRenderers() {
		ArrayList<String> json = new ArrayList();
		String bumpAddress = configuration.getBumpAddress();
		for (RendererConfiguration r : RendererConfiguration.getConnectedControlPlayers()) {
			String address = r.getAddress().toString().substring(1);
			json.add(String.format("[\"%s\",%d,\"%s\"]", r, address.equals(bumpAddress) ? 1 : 0, address));
		}
		return "\"renderers\":[" + StringUtils.join(json, ",") + "]";
	}

	public String getPlaylist(UPNPHelper.Player player) {
		ArrayList<String> json = new ArrayList();
		UPNPHelper.Player.Playlist playlist = player.playlist;
		UPNPHelper.Player.Playlist.Item selected = (UPNPHelper.Player.Playlist.Item)playlist.getSelectedItem();
		int i;
		for (i=0; i < playlist.getSize(); i++) {
			UPNPHelper.Player.Playlist.Item item = (UPNPHelper.Player.Playlist.Item)playlist.getElementAt(i);
			json.add(String.format("[\"%s\",%d,\"%s\"]",
				item.toString().replace("\"","\\\\\""), item == selected ? 1 : 0, "$i$" + i));
		}
		return "\"playlist\":[" + StringUtils.join(json, ",") + "]";
	}

	private static String read(String resource) {
		try {
			return IOUtils.toString(PlayerControlHandler.class.getResourceAsStream("/resources/web/" + resource), "UTF-8");
		} catch (IOException e) {
			LOGGER.debug("Error reading resource: " + e);
		}
		return null;
	}

	private static String read(File f) {
		try {
			return FileUtils.readFileToString(f, Charset.forName("UTF-8"));
		} catch (IOException e) {
			LOGGER.debug("Error reading file: " + e);
		}
		return null;
	}

	public static Map<String,String> parseQuery(HttpExchange x) {
		Map<String,String> vars = new LinkedHashMap<String,String>();
		try {
			String[] q = x.getRequestURI().getRawQuery().split("&|=");
			int i;
			for (i=0; i < q.length; i+=2) {
				vars.put(URLDecoder.decode(q[i], "UTF-8"), URLDecoder.decode(q[i+1], "UTF-8"));
			}
		} catch (Exception e) {
			LOGGER.debug("Error parsing query string '" + x.getRequestURI().getQuery() + "' :" + e);
		}
		return vars;
	}

	// For standalone service, if required
	private static HttpServer createServer(int socket) {
		HttpServer server = null;
		try {
			server = HttpServer.create(new InetSocketAddress(socket), 0);
			server.start();
		} catch (IOException e) {
			LOGGER.debug("Error creating bump server: " + e);
		}
		return server;
	}
}
