/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is free software; you can redistribute it and/or
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

import java.util.List;
import javax.servlet.http.HttpServletRequest;

/**
 * Helpers for HTTP methods and paths.
 */
public class ApiHelper {
	private final HttpServletRequest req;
	private final String basePath;

	public ApiHelper(HttpServletRequest req, String path) {
		this.req = req;
		this.basePath = path;
	}

	public String getEndpoint() {
		String endpoint = "/";
		int pos = req.getRequestURI().getPath().indexOf(basePath);
		if (pos != -1) {
			endpoint = req.getRequestURI().getPath().substring(pos + basePath.length());
		}
		return endpoint;
	}

	/**
	 * @param path the specified path
	 * @return whether this was a GET request for the specified path.
	 */
	public Boolean get(String path) {
		return req.getRequestMethod().equals("GET") && getEndpoint().equals(path);
	}

	/**
	 * @param path the specified path
	 * @return whether this was a POST request for the specified path.
	 */
	public Boolean post(String path) {
		return req.getRequestMethod().equals("POST") && getEndpoint().equals(path);
	}

	/**
	 * @return the Remote Host String (IP).
	 */
	public String getRemoteHostString() {
		return req.getRemoteAddress().getHostString();
	}

	/**
	 * @return the Request Authorization Headers.
	 */
	public List<String> getAuthorization() {
		return req.getRequestHeaders().get("Authorization");
	}

	/**
	 * @return true if the Remote Host is the server.
	 */
	public boolean isFromLocalhost() {
		return req.getRemoteAddress().getHostName().equals(exchange.getLocalAddress().getHostName());
	}

}
