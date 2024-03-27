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
package net.pms;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class Messages provides a mechanism to localize the text messages found in
 * UMS. It is based on {@link ResourceBundle}.
 *
 */
public class Messages {
	private static final String BUNDLE_NAME = "resources.i18n.messages";

	private static ReadWriteLock resourceBundleLock = new ReentrantReadWriteLock();
	private static ResourceBundle resourceBundle;
	private static final ResourceBundle ROOT_RESOURCE_BUNDLE;
	private static final Logger LOGGER = LoggerFactory.getLogger(Messages.class);

	static {
		/*
		 * This is called when the first call to any of the static class
		 * methods are done. PMS.setLocale() will call setLocaleBundle() to
		 * access the correct resource bundle, but we need something in the
		 * mean time if this is invoked before PM.setLocale() has been called.
		 * This can happen if any code calls getString() before configuration
		 * has been loaded, in which case the default locale will be used.
		 */
		resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
		ROOT_RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT, new ResourceBundle.Control() {
			@Override
			public List<Locale> getCandidateLocales(String name, Locale locale) {
				return Collections.singletonList(Locale.ROOT);
			}
		});

	}

	private Messages() {
	}

	/**
	 * Creates a resource bundle based on the given <code>Local</code> and
	 * keeps this for use by any calls to {@link #getString(String)}. If
	 * no matching <code>ResourceBundle</code> can be found, one is chosen
	 * from a number of candidates according to
	 * <a href="https://docs.oracle.com/javase/7/docs/api/java/util/ResourceBundle.html#default_behavior">
	 * ResourceBundle default behavior</a>.
	 *
	 * @param locale the <code>Locale</code> from which the
	 * <code>ResourceBundle</code> is selected.
	 */

	public static void setLocaleBundle(Locale locale) {
		if (locale == null) {
			throw new IllegalArgumentException("locale cannot be null");
		}
		resourceBundleLock.writeLock().lock();
		try {
			if (isRootEnglish(locale)) {
				resourceBundle = ROOT_RESOURCE_BUNDLE;
			} else {
				resourceBundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
			}
		} finally {
			resourceBundleLock.writeLock().unlock();
		}
	}

	/**
	 * Returns the locale-specific string associated with the key.
	 *
	 * @param key
	 *            Keys in UMS follow the format "group.x". group states where
	 *            this key is likely to be used. For example, StatusTab refers
	 *            to the status tab in the UMS GUI. "x" can be anything.
	 * @return Descriptive string if key is found or a copy of the key string if
	 *         it is not.
	 */
	@Nonnull
	public static String getString(String key) {
		resourceBundleLock.readLock().lock();
		try {
			return getString(key, resourceBundle);
		} finally {
			resourceBundleLock.readLock().unlock();
		}
	}

	/**
	 * Returns the locale-specific GUI string associated with the key.
	 */
	@Nonnull
	public static String getGuiString(String key) {
		return getString(key);
	}

	/**
	 * Returns the locale-specific setting string associated with the key.
	 */
	@Nonnull
	public static String getConfigurationString(String key) {
		return getString(key);
	}

	public static String getStringsAsJson() {
		resourceBundleLock.readLock().lock();
		try {
			Enumeration<String> i18nKeys = resourceBundle.getKeys();
			JsonObject jsonObject = new JsonObject();
			while (i18nKeys.hasMoreElements()) {
				String key = i18nKeys.nextElement();
				String value = resourceBundle.getString(key);
				jsonObject.addProperty(key, value);
			}
			return jsonObject.toString();
		} catch (Exception e) {
			LOGGER.debug("Failed to parse translations to JSON: {} ", e);
		} finally {
			resourceBundleLock.readLock().unlock();
		}
		return null;
	}

	public static JsonObject getStringsAsJsonObject(Locale locale) {
		ResourceBundle rb = null;
		if (locale != null && !isRootEnglish(locale)) {
			rb = ResourceBundle.getBundle(BUNDLE_NAME, locale);
		}
		if (rb == null) {
			rb = ROOT_RESOURCE_BUNDLE;
		}
		JsonObject jsonObject = new JsonObject();
		try {
			Enumeration<String> i18nKeys = rb.getKeys();
			while (i18nKeys.hasMoreElements()) {
				String key = i18nKeys.nextElement();
				String value = rb.getString(key);
				jsonObject.addProperty(key, value);
			}
		} catch (Exception e) {
			LOGGER.debug("Failed to parse translations to JSON: {} ", e);
		}
		return jsonObject;
	}

	@Nonnull
	public static String getString(String key, Locale locale) {
		if (locale == null) {
			return getString(key);
		}
		// Selecting base bundle (en-US) for all English variants but British
		if (isRootEnglish(locale)) {
			return getRootString(key);
		}
		ResourceBundle rb = ResourceBundle.getBundle(BUNDLE_NAME, locale);
		if (rb == null) {
			rb = ROOT_RESOURCE_BUNDLE;
		}
		return getString(key, rb);
	}

	/**
	 * Returns the string from the root language file (messages.properties)
	 * regardless of default <code>Locale</code>. Java will otherwise choose
	 * a <code>Locale</code> for a "similar" language or the default
	 * <code>Locale</code> if the requested locale can't be found. The root
	 * <code>Locale</code> is only chosen as a last resort. See
	 * <a href="https://docs.oracle.com/javase/7/docs/api/java/util/ResourceBundle.html#default_behavior">
	 * ResourceBundle default behavior</a> for more information about the
	 * selection process.<br><br>
	 *
	 * @param key the key for the desired string
	 * @return the string for the given key from root (neutral)
	 */
	@Nonnull
	public static String getRootString(String key) {
		return getString(key, ROOT_RESOURCE_BUNDLE);
	}

	@Nonnull
	private static String getString(String key, ResourceBundle rb) {
		try {
			return rb.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}

	/**
	 * Checks if the given <code>Locale</code> should use the root language
	 * file (messages.properties) which is en-US. Currently that is all variants
	 * of English but British English.
	 * @param locale the <code>Locale</code> to check
	 * @return The result
	 */
	private static boolean isRootEnglish(Locale locale) {
		return locale.getLanguage().toLowerCase(Locale.ENGLISH).equals("en") && !locale.getCountry().equals("GB");
	}
}
