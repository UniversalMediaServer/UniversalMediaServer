/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
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
package net.pms.network.webplayerserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.servlet.ServletException;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.httpserverservletcontainer.HttpServerServletContainer;
import net.pms.network.webguiserver.servlets.AboutApiServlet;
import net.pms.network.webguiserver.servlets.I18nApiServlet;
import net.pms.network.webguiserver.servlets.PlayerApiServlet;
import net.pms.network.webguiserver.servlets.WebPlayerServlet;
import net.pms.network.webplayerserver.servlets.PlayerAuthApiServlet;

@SuppressWarnings("restriction")
public class WebPlayerServerHttpServer extends WebPlayerServer {

	private HttpServer server;

	public WebPlayerServerHttpServer() throws IOException {
		this(DEFAULT_PORT);
	}

	public WebPlayerServerHttpServer(int port) throws IOException {
		if (port < 0) {
			port = DEFAULT_PORT;
		}

		// Setup the socket address
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

		try {
			server = HttpServer.create(address, 0);
		} catch (IOException e) {
			LOGGER.error("Failed to start web graphical user interface server : {}", e.getMessage());
		}

		if (server != null) {
			int threads = CONFIGURATION.getWebThreads();
			HttpServerServletContainer container = new HttpServerServletContainer(server, "file:" + CONFIGURATION.getWebPath() + "/react-client/");
			try {
				container.createServlet(AboutApiServlet.class);
				container.createServlet(I18nApiServlet.class);
				container.createServlet(PlayerApiServlet.class);
				container.createServlet(PlayerAuthApiServlet.class);
				container.createServlet(WebPlayerServlet.class);
			} catch (ServletException ex) {
				LOGGER.error(ex.getMessage());
			}
			server.setExecutor(Executors.newFixedThreadPool(threads));
			server.start();
		}
	}

	@Override
	public HttpServer getServer() {
		return server;
	}

	@Override
	public int getPort() {
		return server.getAddress().getPort();
	}

	@Override
	public String getAddress() {
		return MediaServer.getHost() + ":" + getPort();
	}

	@Override
	public String getUrl() {
		if (server != null) {
			return (isSecure() ? "https://" : "http://") + getAddress();
		}
		return null;
	}

	@Override
	public boolean isSecure() {
		return server instanceof HttpsServer;
	}

	/**
	 * Stop the current server.
	 * Once stopped, a {@code HttpServer} cannot be re-used.
	 */
	@Override
	public void stop() {
		if (server != null) {
			server.stop(0);
		}
	}

	public static WebPlayerServerHttpServer createServer(int port) throws IOException {
		LOGGER.debug("Using httpserver as gui server");
		return new WebPlayerServerHttpServer(port);
	}
}