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

/**
 * Based on http://transoceanic.blogspot.cz/2011/12/java-read-key-from-windows-registry.html
 */
package net.pms.util;

import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WindowsRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsRegistry.class);

	/**
	 * @param location path in the registry
	 * @param key registry key
	 * @return registry value or null if not found
	 */
	public static String readRegistry(String location, String key) {
		try {
			// Run reg query, then read output with StreamReader (internal class)
			String query = "reg query " + '"' + location + "\" /v \"" + key + '"';
			Process process = Runtime.getRuntime().exec(query);

			StreamReader reader = new StreamReader(process.getInputStream(), System.getProperty("file.encoding"));
			reader.start();
			process.waitFor();
			reader.join();

			// Parse out the value
			if (reader.getResult().length() > 6) {
				String parsed = reader.getResult().substring(reader.getResult().indexOf("REG_SZ") + 6).trim();

				if (parsed.length() > 1) {
					return parsed;
				}
			}
		} catch (IOException | InterruptedException e) {}

		return null;
	}

	static class StreamReader extends Thread {
		private InputStream inputStream;
		private String charsetName;
		private StringBuffer result = new StringBuffer();

		public StreamReader(InputStream is, String charsetName) {
			this.inputStream = is;
			this.charsetName = charsetName;
		}

		@Override
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, charsetName));
				String line = null;

				while ((line = br.readLine()) != null) {
					result.append(line);
				}
			} catch (UnsupportedEncodingException e) {
				LOGGER.error(null, e);
			} catch (IOException e1) {
				LOGGER.error(null, e1);
			}
		}

		public String getResult() {
			return result.toString();
		}
	}
}
