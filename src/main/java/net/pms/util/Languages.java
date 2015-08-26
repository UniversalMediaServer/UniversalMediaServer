/*
 * Universal Media Server, for streaming any medias to DLNA
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

import java.util.Locale;
import org.apache.commons.lang.WordUtils;

/**
 * This class is a utility class for translation between {@link java.util.Locale}'s
 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> and
 * UMS' language files. See <a href="http://r12a.github.io/apps/subtags/">here
 * for subtag lookup</a>. If UMS languages are removed or added, this class needs
 * to be updated. The class is immutable.
 *
 * @author Nadahar
 * @since 5.2.3
 */

public final class Languages {

	private final static String[] UMS_BCP47_CODES = {
		"ar",      // Arabic
		"bg",      // Bulgarian
		"ca",      // Catalan, Valencian
		"zh-Hans", // Chinese, Han (Simplified variant)
		"zh-Hant", // Chinese, Han (Traditional variant)
		"cs",      // Czech
		"da",      // Danish
		"nl",      // Dutch, Flemish
		"en-GB",   // English, United Kingdom
		"en-US",   // English, United States
		"fi",      // Finnish
		"fr",      // French
		"de",      // German
		"el",      // Modern Greek
		"iw",      // Hebrew (Java prefers the deprecated "iw" to "he")
		"is",      // Icelandic
		"it",      // Italian
		"ja",      // Japanese
		"ko",      // Korean
		"no",      // Norwegian
		"pl",      // Polish
		"pt",      // Portuguese
		"pt-BR",   // Brazilian Portuguese
		"ro",      // Romanian, Moldavian, Moldovan
		"ru",      // Russian
		"sl",      // Slovenian
		"es",      // Spanish, Castilian
		"sv",      // Swedish
		"tr",      // Turkish
	};

	private final static String[] UMS_LANGUAGES = new String[UMS_BCP47_CODES.length];

	static {
		for (int i = 0; i < UMS_BCP47_CODES.length; i++) {
			UMS_LANGUAGES[i] = WordUtils.capitalize(Locale.forLanguageTag(UMS_BCP47_CODES[i]).getDisplayName(Locale.getDefault()));
		}
	}

	private static String localeToLanguageCode(Locale locale) {
		/* This might seem redundant, but a language can also contain a
		 * country/region and a variant. Stating that e.g language
		 * "ar" should return "ar" means that "messages_ar.properties"
		 * will be used for any country/region and variant of Arabic.
		 * This should be true until UMS contains multiple dialects of Arabic,
		 * in which case different codes would have to be returned for the
		 * different dialects.
		 */

		if (locale == null) {
			return null;
		}
		String languageCode = locale.getLanguage();
		if (languageCode != null && !languageCode.isEmpty()) {
			switch (languageCode) {
				case "en":
					if (locale.getCountry().equalsIgnoreCase("GB")) {
						return "en-GB";
					} else {
						return "en-US";
					}
				case "pt":
					if (locale.getCountry().equalsIgnoreCase("BR")) {
						return "pt-BR";
					} else {
						return "pt";
					}
				case "cmn":
				case "zh":
					if (locale.getScript().equalsIgnoreCase("Hans")) {
						return "zh-Hans";
					} else if (locale.getCountry().equalsIgnoreCase("CN") || locale.getCountry().equalsIgnoreCase("SG")) {
						return "zh-Hans";
					} else {
						return "zh-Hant";
					}
				default:
					return languageCode;
			}
		} else {
			return null;
		}
	}

	/**
	 * Verifies if a given <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag is supported by UMS.
	 * @param languageCode The language tag in IEFT BCP 47 format.
	 * @return The result.
	 */
	public static boolean isValid(String languageCode) {
		if (languageCode != null && !languageCode.isEmpty()) {
			for (String code : UMS_BCP47_CODES) {
				if (code.equalsIgnoreCase(languageCode)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Verifies if a given {@link java.util.Locale} is supported by UMS.
	 * @param locale The {@link java.util.Locale}.
	 * @return The result.
	 */
	public static boolean isValid(Locale locale) {
		return isValid(localeToLanguageCode(locale));
	}

	/** Returns a correctly capitalized <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 *  language tag if the language tag is supported by UMS, or returns null.
	 * @param languageCode The IEFT BCP 47 compatible language tag.
	 * @return The IEFT BCP 47 formatted language tag.
	 */
	public static String toLanguageCode(String languageCode) {
		if (languageCode != null && !languageCode.isEmpty()) {
			for (String code : UMS_BCP47_CODES) {
				if (code.equalsIgnoreCase(languageCode)) {
					return code;
				}
			}
		}
		return null;
	}

	/** Returns a correctly capitalized <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 *  language tag if the language tag is supported by UMS, or returns null.
	 * @param locale The {@link java.util.Locale}.
	 * @return The IEFT BCP 47 formatted language tag.
	 */
	public static String toLanguageCode(Locale locale) {
		if (locale != null) {
			return toLanguageCode(localeToLanguageCode(locale));
		}
		return null;
	}

	/**
	 * Returns a UMS supported {@link java.util.Locale} from the given
	 * <code>Local</code> if it exists (<code>en</code> is translated to
	 * <code>en-US</code>, <code>zh</code> to <code>zh-Hant</code> etc.).
	 * Returns <code>null</code> if a valid <code>Locale</code> cannot be found.
	 * @param locale Source {@link java.util.Locale}.
	 * @return Resulting {@link java.util.Locale}.
	 */
	public static Locale toLocale(Locale locale) {
		if (locale != null) {
			String code = localeToLanguageCode(locale);
			if (code != null && isValid(code)) {
				return Locale.forLanguageTag(code);
			}
		}
		return null;
	}

	public static String[] getLanguageTags() {
		return UMS_BCP47_CODES;
	}

	public static String[] getLanguageNames() {
		return UMS_LANGUAGES;
	}
}
