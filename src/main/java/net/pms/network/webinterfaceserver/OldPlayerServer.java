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
package net.pms.network.webinterfaceserver;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.UUID;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.dlna.RootFolder;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webinterfaceserver.handlers.BrowseHandler;
import net.pms.network.webinterfaceserver.handlers.ConsoleHandler;
import net.pms.network.webinterfaceserver.handlers.ControlHandler;
import net.pms.network.webinterfaceserver.handlers.DocHandler;
import net.pms.network.webinterfaceserver.handlers.EventStreamHandler;
import net.pms.network.webinterfaceserver.handlers.FileHandler;
import net.pms.network.webinterfaceserver.handlers.MediaHandler;
import net.pms.network.webinterfaceserver.handlers.PlayHandler;
import net.pms.network.webinterfaceserver.handlers.PollHandler;
import net.pms.network.webinterfaceserver.handlers.RawHandler;
import net.pms.network.webinterfaceserver.handlers.StartHandler;
import net.pms.network.webinterfaceserver.handlers.ThumbHandler;
import net.pms.renderers.devices.WebRender;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldPlayerServer extends WebInterfaceServer implements WebInterfaceServerHttpServerInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(OldPlayerServer.class);
	private static OldPlayerServer instance;
	private final HttpServer server;

	private OldPlayerServer(HttpServer server) throws IOException {
		super();
		this.server = server;
		// Add context handlers
		addCtx(server, "/oldplayer", new StartHandler(this));
		addCtx(server, "/browse", new BrowseHandler(this));
		PlayHandler playHandler = new PlayHandler(this);
		addCtx(server, "/play/", playHandler);
		addCtx(server, "/playerstatus/", playHandler);
		addCtx(server, "/playlist/", playHandler);
		addCtx(server, "/m3u8/", playHandler);
		addCtx(server, "/media/", new MediaHandler(this));
		addCtx(server, "/fmedia/", new MediaHandler(this, true));
		addCtx(server, "/thumb/", new ThumbHandler(this));
		addCtx(server, "/raw/", new RawHandler(this));
		addCtx(server, "/files/", new FileHandler(this));
		addCtx(server, "/doc", new DocHandler(this));
		addCtx(server, "/poll", new PollHandler(this));
		addCtx(server, "/event-stream", new EventStreamHandler(this));
		addCtx(server, "/bump", new ControlHandler(this));
		addCtx(server, "/console", new ConsoleHandler(this));
	}

	@Override
	public void stop() {
		// Do nothing.
	}

	@Override
	public RootFolder getRoot(String user, HttpExchange t) throws InterruptedException {
		return getRoot(user, false, t);
	}

	@Override
	public RootFolder getRoot(String user, boolean create, HttpExchange t) throws InterruptedException {
		String cookie = WebInterfaceServerUtil.getCookie("UMS", t);
		boolean setCookie = false;
		RootFolder root = null;
		synchronized (roots) {
			if (cookie != null) {
				root = roots.get(cookie);
			}
			if (root == null) {
				// Double-check for cookie errors
				WebRender valid = WebInterfaceServerUtil.matchRenderer(user, t);
				if (valid != null) {
					// A browser of the same type and user is already connected
					// at
					// this ip but for some reason we didn't get a cookie match.
					RootFolder validRoot = valid.getRootFolder();
					// Do a reverse lookup to see if it's been registered
					for (Entry<String, RootFolder> entry : roots.entrySet()) {
						if (entry.getValue() == validRoot) {
							// Found
							root = validRoot;
							cookie = entry.getKey();
							setCookie = true;
							LOGGER.debug("Allowing browser connection without cookie match: {}: {}", valid.getRendererName(),
									t.getRemoteAddress().getAddress());
							break;
						}
					}
				}
			}

			if (!create || (root != null)) {
				if (setCookie) {
					t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/;SameSite=Strict");
				}
				return root;
			}

			LOGGER.debug("Browser connection {} have no uuid", t.getRemoteAddress().getAddress());
			root = new RootFolder();
			try {
				WebRender render = new WebRender(user);
				root.setDefaultRenderer(render);
				render.setRootFolder(root);
				render.associateIP(t.getRemoteAddress().getAddress());
				render.associatePort(t.getRemoteAddress().getPort());
				if (CONFIGURATION.useWebPlayerSubLang()) {
					render.setSubLang(StringUtils.join(WebInterfaceServerUtil.getLangs(t), ","));
				}
				render.setBrowserInfo(WebInterfaceServerUtil.getCookie("UMSINFO", t), t.getRequestHeaders().getFirst("User-agent"));
				PMS.get().setRendererFound(render);
			} catch (ConfigurationException e) {
				root.setDefaultRenderer(RendererConfigurations.getDefaultRenderer());
			}

			root.discoverChildren();
			cookie = UUID.randomUUID().toString();
			LOGGER.debug("Browser connection set uuid {} to {}", cookie, t.getRemoteAddress().getAddress());
			t.getResponseHeaders().add("Set-Cookie", "UMS=" + cookie + ";Path=/;SameSite=Strict");
			roots.put(cookie, root);
		}
		return root;
	}

	public static void resetRenderers() {
		if (instance != null) {
			instance.resetAllRenderers();
		}
	}

	public static void deleteRenderers() {
		if (instance != null) {
			instance.deleteAllRenderers();
		}
	}

	public static void plug(HttpServer server) {
		try {
			instance = new OldPlayerServer(server);
		} catch (IOException ex) {
			LOGGER.debug("Failed to plug the old player");
		}
	}

	private static void addCtx(HttpServer server, String path, HttpHandler h) {
		HttpContext ctx = server.createContext(path, h);
		if (CONFIGURATION.isWebAuthenticate()) {
			ctx.setAuthenticator(new BasicAuthenticator(CONFIGURATION.getServerName()) {
				@Override
				public boolean checkCredentials(String user, String pwd) {
					LOGGER.debug("authenticate " + user);
					return PMS.verifyCred("web", PMS.getCredTag("web", user), user, pwd);
				}
			});
		}
	}

	@Override
	public int getPort() {
		return server.getAddress().getPort();
	}

	@Override
	public String getAddress() {
		return MediaServer.getHost() + ":" + getPort();
	}

}
