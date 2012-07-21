/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008-2012  A.Brochard
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
package net.pms.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;

/*
 * Utility methods common to classes that wrap config (property) files e.g. PmsConfiguration and RendererConfiguration.
 */

class ConfigurationUtil { /* package-private */
	/**
	 * Return the <code>String</code> value for a given configuration key if the
	 * value is non-blank (i.e. not null, not an empty string, not all whitespace).
	 * Otherwise return the supplied default value.
	 * The value is returned with leading and trailing whitespace removed in both cases.
	 * @param configuration The configuration to look up the key in.
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */

	// package-private
	static String getNonBlankConfigurationString(Configuration configuration, String key, String def) {
		String value = configuration.getString(key);

		if (StringUtils.isNotBlank(value)) {
			return value.trim();
		} else if (def != null) {
			return def.trim();
		} else {
			return def;
		}
	}
}
