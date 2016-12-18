/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
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

package net.pms.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Properties;

/**
 * Convenience wrapper around the Java Properties class.
 *
 * @author Tim Cox (mail@tcox.org)
 */
public class PmsProperties {
	private final Properties properties = new Properties();
	private static final String ENCODING = "UTF-8";

	public void loadFromByteArray(byte[] data) throws IOException {
		try {
			String utf = new String(data, ENCODING);
			try (StringReader reader = new StringReader(utf)) {
				properties.clear();
				properties.load(reader);
			}
		} catch (UnsupportedEncodingException e) {
			throw new IOException("Could not decode " + ENCODING);
		}
	}

	/**
	 * Initialize from a properties file.
	 * @param filename The properties file.
	 * @throws IOException
	 */
	public void loadFromResourceFile(String filename) throws IOException {
		try (InputStream inputStream = getClass().getResourceAsStream(filename)) {
			properties.load(inputStream);
		}
	}

	public void clear() {
		properties.clear();
	}

	public String get(String key) {
		Object obj = properties.get(key);
		if (obj != null) {
			return trimAndRemoveQuotes("" + obj);
		} else {
			return "";
		}
	}

	private static String trimAndRemoveQuotes(String in) {
		in = in.trim();
		if (in.startsWith("\"")) {
			in = in.substring(1);
		}
		if (in.endsWith("\"")) {
			in = in.substring(0, in.length() - 1);
		}
		return in;
	}

	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}
}
