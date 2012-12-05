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
package net.pms;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Class Messages provides a mechanism to localize the text messages found in
 * PMS. It is based on {@link ResourceBundle}.
 */
public class Messages {
	private static final String BUNDLE_NAME = "resources.i18n.messages";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	/**
	 * Returns the locale-specific string associated with the key.
	 * 
	 * @param key
	 *            Keys in PMS follow the format "group.x". group states where
	 *            this key is likely to be used. For example, NetworkTab refers
	 *            to the network configuration tab in the PMS GUI. x is just a
	 *            number.
	 * @return Descriptive string if key is found or a copy of the key string if
	 *         it is not.
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
