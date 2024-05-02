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
package net.pms.configuration;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import net.pms.util.StringUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

public abstract class BaseConfiguration {
	private static final StringUtil.LaxUnicodeUnescaper LAX_UNICODE_UNESCAPER = new StringUtil.LaxUnicodeUnescaper();
	public static final String EMPTY_LIST_VALUE = "None";

	protected Configuration configuration;
	protected ConfigurationReader configurationReader;

	protected BaseConfiguration(Configuration configuration, ConfigurationReader configurationReader) {
		this.configuration = configuration;
		this.configurationReader = configurationReader;
	}

	protected BaseConfiguration(boolean logOverrides) {
		configuration = createPropertiesConfiguration();
		configurationReader = new ConfigurationReader(configuration, logOverrides);
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public ConfigurationReader getConfigurationReader() {
		return configurationReader;
	}

	public int getInt(String key, int def) {
		return configurationReader.getInt(key, def);
	}

	public long getLong(String key, long def) {
		return configurationReader.getLong(key, def);
	}

	public double getDouble(String key, double def) {
		return configurationReader.getDouble(key, def);
	}

	public boolean getBoolean(String key, boolean def) {
		return configurationReader.getBoolean(key, def);
	}

	public final String getString(String key, String def) {
		return configurationReader.getNonBlankConfigurationString(key, def);
	}

	public List<String> getStringList(String key, String def) {
		List<String> result = configurationReader.getStringList(key, def);
		if (result.size() == 1 && result.get(0).equalsIgnoreCase(EMPTY_LIST_VALUE)) {
			return new ArrayList<>();
		}
		return result;
	}

	public void setStringList(String key, List<String> value) {
		StringBuilder result = new StringBuilder();
		for (String element : value) {
			if (!result.toString().isEmpty()) {
				result.append(", ");
			}
			result.append(element);
		}
		if (result.toString().isEmpty()) {
			result.append(EMPTY_LIST_VALUE);
		}
		configuration.setProperty(key, result.toString());
	}

	public static PropertiesConfiguration createPropertiesConfiguration() {
		PropertiesConfiguration conf = new PropertiesConfiguration();
		conf.setListDelimiter((char) 0);
		// Treat backslashes in the conf as literal while also supporting double-backslash syntax, i.e.
		// ensure that typical raw regex strings (and unescaped Windows file paths) are read correctly.
		conf.setIOFactory(new PropertiesConfiguration.DefaultIOFactory() {
			@Override
			public PropertiesConfiguration.PropertiesReader createPropertiesReader(final Reader in, final char delimiter) {
				return new PropertiesConfiguration.PropertiesReader(in, delimiter) {
					@Override
					protected void parseProperty(final String line) {
						// Decode any backslashed unicode escapes, e.g. '\u005c', from the
						// ISO 8859-1 (aka Latin 1) encoded java Properties file, then
						// unescape any double-backslashes, then escape all backslashes before parsing
						super.parseProperty(LAX_UNICODE_UNESCAPER.translate(line).replace("\\\\", "\\").replace("\\", "\\\\"));
					}
				};
			}
		});
		return conf;
	}


	/**
	 * Converts the MEncoder's quality settings format to FFmpeg's.
	 *
	 * @param mpegSettings
	 * @return The FFmpeg format.
	 */
	protected String convertMencoderSettingToFFmpegFormat(String mpegSettings) {
		String[] mpegSettingsArray = mpegSettings.split(":");
		String[] pairArray;
		StringBuilder returnString = new StringBuilder();
		for (String pair : mpegSettingsArray) {
			pairArray = pair.split("=");
			switch (pairArray[0]) {
				case "keyint" -> returnString.append("-g ").append(pairArray[1]).append(' ');
				case "vqscale" -> returnString.append("-q:v ").append(pairArray[1]).append(' ');
				case "vqmin" -> returnString.append("-qmin ").append(pairArray[1]).append(' ');
				case "vqmax" -> returnString.append("-qmax ").append(pairArray[1]).append(' ');
				default -> {
					//setting not yet handled
				}
			}
		}

		return returnString.toString();
	}

}
