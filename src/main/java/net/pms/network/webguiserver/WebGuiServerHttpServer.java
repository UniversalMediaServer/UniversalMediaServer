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
package net.pms.network.webguiserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.servlet.ServletException;
import net.pms.network.httpserverservletcontainer.HttpServerServletContainer;
import net.pms.network.webguiserver.servlets.AboutApiServlet;
import net.pms.network.webguiserver.servlets.AccountApiServlet;
import net.pms.network.webguiserver.servlets.ActionsApiServlet;
import net.pms.network.webguiserver.servlets.AuthApiServlet;
import net.pms.network.webguiserver.servlets.I18nApiServlet;
import net.pms.network.webguiserver.servlets.LogsApiServlet;
import net.pms.network.webguiserver.servlets.PlayerApiServlet;
import net.pms.network.webguiserver.servlets.RenderersApiServlet;
import net.pms.network.webguiserver.servlets.SettingsApiServlet;
import net.pms.network.webguiserver.servlets.SharedContentApiServlet;
import net.pms.network.webguiserver.servlets.SseApiServlet;
import net.pms.network.webguiserver.servlets.WebGuiServlet;

@SuppressWarnings("restriction")
public class WebGuiServerHttpServer extends WebGuiServer {

	private HttpServer server;

	public WebGuiServerHttpServer() throws IOException {
		this(-1);
	}

	public WebGuiServerHttpServer(int port) throws IOException {
		if (port < 0) {
			port = CONFIGURATION.getWebGuiServerPort();
		}

		// Setup the socket address
		InetSocketAddress address = new InetSocketAddress(InetAddress.getByName("0.0.0.0"), port);

		try {
			server = HttpServer.create(address, 0);
		} catch (IOException e) {
			LOGGER.error("Failed to start web graphical user interface server ({}) : {}", address, e.getMessage());
		}

		if (server != null) {
			int threads = CONFIGURATION.getWebThreads();
			HttpServerServletContainer container = new HttpServerServletContainer(server, "file:" + CONFIGURATION.getWebPath() + "/react-client/");
			try {
				container.createServlet(WebGuiServlet.class);
				container.createServlet(AboutApiServlet.class);
				container.createServlet(AccountApiServlet.class);
				container.createServlet(ActionsApiServlet.class);
				container.createServlet(AuthApiServlet.class);
				container.createServlet(I18nApiServlet.class);
				container.createServlet(LogsApiServlet.class);
				container.createServlet(PlayerApiServlet.class);
				container.createServlet(RenderersApiServlet.class);
				container.createServlet(SettingsApiServlet.class);
				container.createServlet(SharedContentApiServlet.class);
				container.createServlet(SseApiServlet.class);
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
		return "localhost:" + getPort();
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

	public static WebGuiServerHttpServer createServer(int port) throws IOException {
		LOGGER.debug("Using httpserver as gui server");
		return new WebGuiServerHttpServer(port);
	}
}
