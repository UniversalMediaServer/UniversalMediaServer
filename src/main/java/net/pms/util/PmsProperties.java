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
			StringReader reader = new StringReader(utf);
			properties.clear();
			properties.load(reader);
			reader.close();
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
		InputStream inputStream = getClass().getResourceAsStream(filename);

		try {
			properties.load(inputStream);
		} finally {
			inputStream.close();
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
