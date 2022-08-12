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
package net.pms.network.webguiserver;

import net.pms.network.webinterfaceserver.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.webguiserver.JavaHttpServerServletContainer.HttpHandlerServlet;
import net.pms.network.webguiserver.handlers.AboutApiServlet;
import net.pms.network.webguiserver.handlers.AccountApiServlet;
import net.pms.network.webguiserver.handlers.ActionsApiServlet;
import net.pms.network.webguiserver.handlers.AuthApiServlet;
import net.pms.network.webguiserver.handlers.ConfigurationApiServlet;
import net.pms.network.webguiserver.handlers.WebGuiServlet;
import net.pms.network.webguiserver.handlers.SseApiServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class WebGuiServerHttpServer implements WebInterfaceServerInterface {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebGuiServerHttpServer.class);
	private static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
	public static final int DEFAULT_PORT = 9002; //CONFIGURATION.getGuiServerPort();
	private HttpServer server;

	public WebGuiServerHttpServer() throws IOException {
		this(DEFAULT_PORT);
	}

	public WebGuiServerHttpServer(int port) throws IOException {
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

			addServlet(new WebGuiServlet());
			//configuration v1 api handlers
			addServlet(new AboutApiServlet());
			addServlet(new AccountApiServlet());
			addServlet(new ActionsApiServlet());
			addServlet(new AuthApiServlet());
			addServlet(new ConfigurationApiServlet());
			addServlet(new SseApiServlet());

			server.setExecutor(Executors.newFixedThreadPool(threads));
			server.start();
		}
	}

	private void addServlet(HttpServlet servlet) {
		WebServlet webServlet = servlet.getClass().getAnnotation(WebServlet.class);
		String[] urlPatterns = webServlet.urlPatterns();
		if (urlPatterns.length == 0) {
			urlPatterns = webServlet.value();
		}
		if (urlPatterns.length > 0) {
			HttpHandlerServlet handler = new HttpHandlerServlet(servlet);
			for (String urlPattern : urlPatterns) {
				server.createContext(urlPattern, handler);
			}
		} else {
			LOGGER.error("Class {} does not include any pattern.", servlet.getClass().getName());
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

	public static WebGuiServerHttpServer createServer(int port) throws IOException {
		LOGGER.debug("Using httpserver as gui server");
		return new WebGuiServerHttpServer(port);
	}
}
