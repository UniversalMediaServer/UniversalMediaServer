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

import com.sun.net.httpserver.HttpExchange;
import java.net.InetAddress;
import net.pms.PMS;
import net.pms.network.HTTPResource;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("restriction")
public class WebInterfaceServerUtil {
	public static final String MIME_TRANS = HTTPResource.OGG_TYPEMIME;

	/**
	 * This class is not meant to be instantiated.
	 */
	private WebInterfaceServerUtil() {
	}

	public static boolean deny(HttpExchange t) {
		return deny(t.getRemoteAddress().getAddress());
	}

	public static boolean deny(InetAddress inetAddress) {
		return !PMS.getConfiguration().getIpFiltering().allowed(inetAddress) || !PMS.isReady();
	}

	private static final int WIDTH = 0;
	private static final int HEIGHT = 1;

	private static final int DEFAULT_WIDTH = 720;
	private static final int DEFAULT_HEIGHT = 404;

	private static int getHW(int cfgVal, int id, int def) {
		if (cfgVal != 0) {
			// if we have a value cfg return that
			return cfgVal;
		}
		String s = PMS.getConfiguration().getWebSize();
		if (StringUtils.isEmpty(s)) {
			// no size string return default
			return def;
		}
		String[] tmp = s.split("x", 2);
		if (tmp.length < 2) {
			// bad format resort to default
			return def;
		}
		try {
			// pick whatever we got
			return Integer.parseInt(tmp[id]);
		} catch (NumberFormatException e) {
			// bad format (again) resort to default
			return def;
		}
	}

	public static int getHeight() {
		return getHW(PMS.getConfiguration().getWebHeight(), HEIGHT, DEFAULT_HEIGHT);
	}

	public static int getWidth() {
		return getHW(PMS.getConfiguration().getWebWidth(), WIDTH, DEFAULT_WIDTH);
	}

	public static String transMime() {
		return MIME_TRANS;
	}
}
