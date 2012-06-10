/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
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
package net.pms.network;

import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.HashMap;
import java.util.StringTokenizer;


/**
 * This class knows how to obtain authentication for a network connection
 * from URLs that look like "http://username:password@http://www.domain.com/feed".
 * It stores user and password information per hostname.
 */
public class HTTPResourceAuthenticator extends Authenticator {

	// Lookup table for user and password information per hostname
	static HashMap<String, String> siteinfo = new HashMap<String, String>();

	/**
	 * Called when password authentication is needed.
	 * @return Returns the {@link PasswordAuthentication} collected from the site
	 * information, or null if none is provided.
	 */
	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		URL requrl = getRequestingURL();

		if (siteinfo.get(requrl.getHost()) != null) {
			String userinfo = siteinfo.get(requrl.getHost());
			StringTokenizer st = new StringTokenizer(userinfo, ":");
			String user = null;
			String pass = null;

			if (st.hasMoreTokens()) {
				user = st.nextToken();
			}

			if (st.hasMoreTokens()) {
				pass = st.nextToken();
			}

			if (pass != null) {
				// Send user information for the URL.
				PasswordAuthentication pwAuth = new PasswordAuthentication(user, pass.toCharArray());
	
				return pwAuth;
			} else {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Concatenate stored user and password information to the provided URL.
	 * If this results in an invalid URL, or if the original already contains
	 * user information, the original is returned exactly like it was.
	 * @param url The URL for the HTTP resource.
	 * @return The URL with user information.
	 */
	public static URL concatenateUserInfo(URL url) {
		if (url.getUserInfo() == null && siteinfo.get(url.getHost()) != null) {
			String userinfo = siteinfo.get(url.getHost());
			String urlStr = url.toString();
			String protocol = url.getProtocol() + "://";

			// Insert the username and password into the URL.
			urlStr = urlStr.replace(protocol, protocol + userinfo + "@");

			try {
				return new URL(urlStr);
			} catch (MalformedURLException e) {
				// Not a valid URL, fall back to the original.
				return url;
			}
		} else {
			// The URL either already has user information in it, or there is
			// no user information stored for this particular hostname.
			return url;
		}
	}

	/**
	 * Examine a URL and if it contains user information, store the username
	 * and password for that host.
	 * @param url The URL to examine.
	 */
	public static void addURL(URL url) {
		if (url.getUserInfo() != null) {
			siteinfo.put(url.getHost(), url.getUserInfo());
		}
	}
}
