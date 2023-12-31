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

import java.io.File;
import java.util.*;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class that implements common getters for the various types stored in renderer confs and UMS.conf.
 */
public class ConfigurationReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationReader.class);
	private final Map<String, Object> logMap = new HashMap<>();
	private final Configuration configuration;
	private boolean logOverrides;
	private Configuration dConf;
	private String dTag;

	ConfigurationReader(Configuration configuration) {
		this(configuration, false); // don't log by default: just provide the getters
	}

	ConfigurationReader(Configuration configuration, boolean logOverrides) {
		this.configuration = configuration;
		this.logOverrides = logOverrides;
		dConf = (configuration instanceof CompositeConfiguration) ?
			((CompositeConfiguration) configuration).getConfiguration(0) : null;
		File f = dConf != null ? ((PropertiesConfiguration) dConf).getFile() : null;
		dTag = f != null ? ("[" + f.getName() + "] ") : null;
	}

	// quote strings
	private String quote(Object value) {
		if (value instanceof String s) {
			return String.format("\"%s\"", s.replace("\"", "\\\""));
		} else {
			return String.valueOf(value);
		}
	}

	// log configuration settings that override the previous value,
	// where the previous value is initialised to the default
	// value
	private <T> void log(String key, T value, T def) {
		boolean initialised = false;

		if (!logOverrides) {
			return;
		}

		// 1) if a record for this key doesn't exist, initialise it with the default value
		if (!logMap.containsKey(key)) {
			logMap.put(key, def);
			initialised = true;
		}

		// 2) now check if the value has changed
		T oldValue = (T) logMap.get(key);

		if (ObjectUtils.notEqual(oldValue, value)) {
			logMap.put(key, value);

			// Do an independent lookup to determine if the value's source was the device conf,
			// and if so log it as a device override by explicitly identifying the source.
			String src = (dConf != null && value != null && value.equals(dConf.getProperty(key))) ? dTag : "";
			if (initialised) {
				LOGGER.debug("{}Reading {}: {} (default: {})", src, key, quote(value), quote(oldValue));
			} else {
				LOGGER.debug("{}Reading {}: {} (previous: {}, default: {})", src, key, quote(value), quote(oldValue), quote(def));
			}
		}
	}

	/**
	 * Return the <code>int</code> value for a given configuration key. First, the key
	 * is looked up in the current configuration settings. If it exists and contains a
	 * valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 *
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	int getInt(String key, int def) {
		int value;

		try {
			value = configuration.getInt(key, def);
		} catch (ConversionException e) {
			value = def;
		}

		log(key, value, def);
		return value;
	}

	/**
	 * Return the <code>long</code> value for a given configuration key. First, the key
	 * is looked up in the current configuration settings. If it exists and contains a
	 * valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 *
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	long getLong(String key, long def) {
		long value;

		try {
			value = configuration.getLong(key, def);
		} catch (ConversionException e) {
			value = def;
		}

		log(key, value, def);
		return value;
	}

	/**
	 * Return the <code>double</code> value for a given configuration key. First, the key
	 * is looked up in the current configuration settings. If it exists and contains a
	 * valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 *
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	double getDouble(String key, double def) {
		double value;

		try {
			value = configuration.getDouble(key, def);
		} catch (ConversionException e) {
			value = def;
		}

		log(key, value, def);
		return value;
	}

	/**
	 * Return the <code>boolean</code> value for a given configuration key. First, the
	 * key is looked up in the current configuration settings. If it exists and contains
	 * a valid value, that value is returned. If the key contains an invalid value or
	 * cannot be found, the specified default value is returned.
	 *
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	boolean getBoolean(String key, boolean def) {
		boolean value;

		try {
			value = configuration.getBoolean(key, def);
		} catch (ConversionException e) {
			value = def;
		}

		log(key, value, def);
		return value;
	}

	/**
	 * Return the <code>String</code> value for a given configuration key if the
	 * value is non-blank (i.e. not null, not an empty string, not all whitespace).
	 * Otherwise return the supplied default value.
	 * The value is returned with leading and trailing whitespace removed in both cases.
	 *
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	String getString(String key, String def) {
		return getNonBlankConfigurationString(key, def);
	}

	/**
	 * Return a <code>List</code> of <code>String</code> values for a given configuration
	 * key. First, the key is looked up in the current configuration settings. If it
	 * exists and contains a valid value, that value is returned. If the key contains an
	 * invalid value or cannot be found, a list with the specified default values is
	 * returned.
	 *
	 * @param key The key to look up.
	 * @param def The default list of strings to return when
	 * no matching value is found.
	 * If list items in a comma-separated have surrounding whitespace,
	 * the whitespace is ignored e.g. <code>"foo, bar, baz"</code> becomes
	 * <code>[ "foo", "bar" , "baz" ]</code>.
	 * @return The list of value strings configured for the key.
	 */
	List<String> getStringList(String key, String def) {
		return Arrays.asList(getString(key, def != null ? def : "").split("\\s*,\\s*"));
	}

	/**
	 * Return the <code>Object</code> value for a custom configuration key.
	 * Returns an object if the key exists, or <code>null</code> otherwise.
	 *
	 * @param key The key to look up.
	 * @return The value configured for the key or null if no value is set.
	 */
	Object getCustomProperty(String property) {
		Object value = configuration.getProperty(property);
		log(property, value, null);
		return value;
	}

	/**
	 * Return the <code>String</code> value for a given configuration key if the
	 * value is non-blank (i.e. not null, not an empty string, not all whitespace).
	 * Otherwise return the supplied default value.
	 * The value is returned with leading and trailing whitespace removed in both cases.
	 *
	 * @param configuration The configuration to look up the key in.
	 * @param key The key to look up.
	 * @param def The default value to return when no value is set for the key.
	 * @return The value configured for the key.
	 */
	String getNonBlankConfigurationString(String key, String def) {
		String value;
		String s = configuration.getString(key);

		if (StringUtils.isNotBlank(s)) {
			value = s.trim();
		} else if (def != null) {
			value = def.trim();
		} else {
			value = null;
		}

		log(key, value, def);
		return value;
	}

	/**
	 * Return the <code>String</code> value for a given, possibly-blank
	 * (i.e. empty or all whitespace) configuration key.
	 * If the value is not defined, the supplied default value is returned.
	 * The value is returned with leading and trailing whitespace removed in both cases.
	 *
	 * @param configuration The configuration to look up the key in.
	 * @param key The key to look up.
	 * @param def The default value to return when no value is defined for the key.
	 * @return The value configured for the key.
	 */
	String getPossiblyBlankConfigurationString(String key, String def) {
		String value;
		String s = configuration.getString(key, def);

		if (s != null) {
			value = s.trim();
		} else {
			value = s;
		}

		log(key, value, def);
		return value;
	}

	public boolean getLogOverrides() {
		return logOverrides;
	}

	public void setLogOverrides(boolean logOverrides) {
		this.logOverrides = logOverrides;
	}
}
