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

import java.io.IOException;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.network.webplayerserver.servlets.PlayerApiServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class WebPlayerServer {
	protected static final Logger LOGGER = LoggerFactory.getLogger(WebPlayerServer.class);
	protected static final PmsConfiguration CONFIGURATION = PMS.getConfiguration();
    //Should be CONFIGURATION.getPlayerServerPort()
	public static final int DEFAULT_PORT = 9003;

	public abstract Object getServer();
	public abstract int getPort();
	public abstract String getAddress();
	public abstract String getUrl();
	public abstract boolean isSecure();
	public abstract void stop();

	public static void resetAllRenderers() {
		PlayerApiServlet.resetAllRenderers();
	}

	public static void deleteAllRenderers() {
		PlayerApiServlet.deleteAllRenderers();
	}

	public static WebPlayerServer createServer(int port) throws IOException {
		return WebPlayerServerHttpServer.createServer(port);
	}
}
