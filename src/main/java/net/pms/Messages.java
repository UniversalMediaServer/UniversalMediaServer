package net.pms;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**Class Messages provides a mechanism to localise the text messages found in PMS. It is based
 * on {@link ResourceBundle}.
 */
public class Messages {
	private static final String BUNDLE_NAME = "net.pms.messages";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private Messages() {
	}

	/**Returns the locale-specific string associated with the key.
	 * @param key Keys in PMS follow the format "group.x". group states where this key is likely to be
	 * used. For example, NetworkTab refers to the network configuration tab in the PMS GUI. x is just
	 * a number. 
	 * @return Descriptive string if key is found or a copy of the key string if it is not.
	 */
	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
