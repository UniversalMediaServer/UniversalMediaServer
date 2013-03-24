/**
 * Based on http://transoceanic.blogspot.cz/2011/12/java-read-key-from-windows-registry.html
 */
package net.pms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
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
			String parsed = reader.getResult().substring(reader.getResult().indexOf("REG_SZ") + 6).trim();

			if (parsed.length() > 1) {
				return parsed;
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

				while ((line = br.readLine()) != null){
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

