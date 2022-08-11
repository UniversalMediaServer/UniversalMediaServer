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

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceManager extends URLClassLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);

	public ResourceManager(String... urls) {
		super(new URL[]{}, null);
		try {
			for (String url : urls) {
				super.addURL(new URL(url));
			}
		} catch (MalformedURLException e) {
			LOGGER.debug("Error adding resource url: " + e);
		}
	}

	public InputStream getInputStream(String filename) {
		return getResourceAsStream(filename);
	}

	@Override
	public URL getResource(String name) {
		URL url = super.getResource(name);
		if (url != null) {
			LOGGER.debug("Using resource: " + url);
		}
		return url;
	}

}
