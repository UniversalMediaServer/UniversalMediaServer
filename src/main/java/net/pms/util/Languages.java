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
package net.pms.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a utility class for translation between
 * {@link java.util.Locale}'s
 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> and
 * UMS' language files. See <a href="http://r12a.github.io/apps/subtags/">here
 * for subtag lookup</a>. If UMS languages are removed or added, this class
 * needs to be updated. The class is immutable.
 *
 * To add a new language, the following must be done:
 * <ul>
 * <li>Add the BCP47 code to {@link #UMS_BCP47_CODES}</li>
 * <li>Add a new label called "{@code Language.<BCP47 tag>}" to
 * {@code messages.properties}</li>
 * <li>Add the language to UMS.conf</li>
 * <li>Modify {@link #localeToLanguageTag(Locale)} to handle the language</li>
 * <li>Modify {@link #languageTagToUMSLanguageTag(String)} to handle the
 * language</li>
 * <li>Add the language at crowdin</li>
 * <li>Pull crowdin translations containing the new language so that the
 * language file is committed</li>
 * </ul>
 *
 * @since 5.2.3
 * @author Nadahar
 */
public final class Languages {
	/**
	 * Not to be instantiated.
	 */
	private Languages() {
	}

	/**
	 * Defines the minimum translation percentage a language can have and still
	 * be included in the list over language choices.
	 */
	private static final int MINIMUM_TRANSLATE_PCT = 20;

	/**
	 * Defines the minimum translation percentage a language can have to be the
	 * recommended/default language.
	 */
	private static final int RECOMMENDED_TRANSLATE_PCT = 90;

	/**
	 * Defines the minimum approved translation percentage a language can have
	 * to be the recommended/default language.
	 */
	private static final int RECOMMENDED_APPROVED_PCT = 85;

	private static final Logger LOGGER = LoggerFactory.getLogger(Languages.class);

	/**
	 * If the below list is changed, methods
	 * {@link #localeToLanguageTag(Locale)} and
	 * {@link #languageTagToUMSLanguageTag(String)} must be updated
	 * correspondingly.
	 */
	private static final String[] UMS_BCP47_CODES = {
		"af",      // Afrikaans
		"ar",      // Arabic
		"bn",      // Bengali (Bangladesh)
		"pt-BR",   // Brazilian Portuguese
		"bg",      // Bulgarian
		"ca",      // Catalan, Valencian
		"zh-Hans", // Chinese, Han (Simplified variant)
		"zh-Hant", // Chinese, Han (Traditional variant)
		"hr",      // Croatian
		"cs",      // Czech
		"da",      // Danish
		"nl",      // Dutch, Flemish
		"en-GB",   // English, United Kingdom
		"en-US",   // English, United States
		"et",      // Estonian
		"fi",      // Finnish
		"fr",      // French
		"de",      // German
		"el",      // Greek, Modern
		"iw",      // Hebrew (Java prefers the deprecated "iw" to "he")
		"hu",      // Hungarian
		"is",      // Icelandic
		"it",      // Italian
		"ja",      // Japanese
		"ko",      // Korean
		"no",      // Norwegian
		"fa",      // Persian (Farsi)
		"pl",      // Polish
		"pt",      // Portuguese
		"ro",      // Romanian, Moldavian, Moldovan
		"ru",      // Russian
		"sr",      // Serbian (Cyrillic)
		"sk",      // Slovak
		"sl",      // Slovenian
		"es",      // Spanish, Castilian
		"sv",      // Swedish
		"th",      // Thai
		"tr",      // Turkish
		"uk",      // Ukrainian
		"vi",      // Vietnamese
	};

	/**
	 * This map is also used as a synchronization object for
	 * {@link #TRANSLATIONS_STATISTICS}, {@link #lastpreferredLocale} and
	 * {@link #SORTED_LANGUAGES}
	 */
	private static final HashMap<String, TranslationStatistics> TRANSLATIONS_STATISTICS = new HashMap<>(
		(int) Math.round(UMS_BCP47_CODES.length * 1.34)
	);
	private static final List<LanguageEntry> SORTED_LANGUAGES = new ArrayList<>();
	private static Locale lastpreferredLocale = null;

	public static class TranslationStatistics {
		protected String name;
		protected int phrases;
		protected int phrasesApproved;
		protected int phrasesTranslated;
		protected int words;
		protected int wordsApproved;
		protected int wordsTranslated;
		protected int approved;
		protected int translated;
	}

	/*
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	private static class LanguageEntry implements Comparable<LanguageEntry> {
		protected String tag;
		protected String name;
		protected String defaultname;
		protected String country;
		protected Locale locale = null;
		protected int coveragePercent;
		protected int approvedPercent;

		@Override
		public int compareTo(LanguageEntry entry) {
			int result = this.name.compareTo(entry.name);
			if (result != 0) {
				return result;
			}
			result = this.tag.compareTo(entry.tag);
			if (result != 0) {
				return result;
			}
			result = entry.coveragePercent - this.coveragePercent;

			return result;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + approvedPercent;
			result = prime * result + coveragePercent;
			result = prime * result + ((locale == null) ? 0 : locale.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((defaultname == null) ? 0 : defaultname.hashCode());
			result = prime * result + ((country == null) ? 0 : country.hashCode());
			result = prime * result + ((tag == null) ? 0 : tag.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (!(obj instanceof LanguageEntry)) {
				return false;
			}
			LanguageEntry other = (LanguageEntry) obj;
			return (
				approvedPercent == other.approvedPercent &&
				coveragePercent == other.coveragePercent &&
				(locale == null ? other.locale == null : locale.equals(other.locale)) &&
				(name == null ? other.name == null : name.equals(other.name)) &&
				(defaultname == null ? other.defaultname == null : defaultname.equals(other.defaultname)) &&
				(country == null ? other.country == null : country.equals(other.country)) &&
				(tag == null ? other.tag == null : tag.equals(other.tag))
			);
		}

	}

	private static class LanguageEntryCoverageComparator implements Comparator<LanguageEntry>, Serializable {
		private static final long serialVersionUID = 1974719326731763265L;

		@Override
		public int compare(LanguageEntry o1, LanguageEntry o2) {
			// Descending
			return o2.coveragePercent - o1.coveragePercent;
		}
	}

	private static String localeToLanguageTag(Locale locale) {
		/*
		 * This might seem redundant, but a language can also contain a
		 * country/region and a variant. Stating that e.g language "ar" should
		 * return "ar" means that "messages_ar.properties" will be used for any
		 * country/region and variant of Arabic. This should be true until UMS
		 * contains multiple dialects of Arabic, in which case different codes
		 * would have to be returned for the different dialects.
		 */

		if (locale == null) {
			return null;
		}
		String languageTag = locale.getLanguage();
		if (languageTag != null && !languageTag.isEmpty()) {
			switch (languageTag) {
				case "en" -> {
					if (locale.getCountry().equalsIgnoreCase("GB")) {
						return "en-GB";
					}
					return "en-US";
				}
				case "pt" -> {
					if (locale.getCountry().equalsIgnoreCase("BR")) {
						return "pt-BR";
					}
					return "pt";
				}
				case "nb", "nn" -> {
					return "no";
				}
				case "cmn", "zh" -> {
					if (locale.getScript().equalsIgnoreCase("Hans")) {
						return "zh-Hans";
					} else if (locale.getCountry().equalsIgnoreCase("CN") || locale.getCountry().equalsIgnoreCase("SG")) {
						return "zh-Hans";
					} else {
						return "zh-Hant";
					}
				}
				default -> {
					return languageTag;
				}
			}
		}
		return null;
	}

	private static String languageTagToUMSLanguageTag(String languageTag) {
		/*
		 * Performs the same conversion as localeToLanguageTag() but from a
		 * language tag instead of a Locale.
		 */
		if (languageTag == null) {
			return null;
		} else if (languageTag.isEmpty()) {
			return "";
		}
		switch (languageTag.toLowerCase(Locale.US)) {
			case "en-gb" -> {
				return "en-GB";
			}
			case "pt-br" -> {
				return "pt-BR";
			}
			case "cmn-cn", "cmn-sg", "cmn-hans", "zh-cn", "zh-sg", "zh-hans" -> {
				return "zh-Hans";
			}
			default -> {
				if (languageTag.indexOf('-') > 0) {
					languageTag = languageTag.substring(0, languageTag.indexOf('-'));
				}
				if (languageTag.equalsIgnoreCase("nb") || languageTag.equalsIgnoreCase("nn")) {
					return "no";
				} else if (languageTag.equalsIgnoreCase("cmn") || languageTag.equalsIgnoreCase("zh")) {
					return "zh-Hant";
				} else if (languageTag.equalsIgnoreCase("en")) {
					return "en-US";
				} else {
					return languageTag.toLowerCase(Locale.US);
				}
			}
		}
	}

	private static void populateTranslationsStatistics() {
		if (TRANSLATIONS_STATISTICS.size() < 1) {
			try (
				BufferedReader reader = new BufferedReader(new InputStreamReader(
					Languages.class.getResourceAsStream("/resources/languages.properties"),
					StandardCharsets.UTF_8
				))
			) {
				Pattern pattern = Pattern.compile("^\\s*(?!#)\\b([^\\.=][^=]+[^\\.=])=(.*[^\\s])\\s*$");
				String line;
				while ((line = reader.readLine()) != null) {
					Matcher matcher = pattern.matcher(line);
					if (matcher.find()) {
						try {
							String[] path = matcher.group(1).split("\\.");
							TranslationStatistics translationStatistics;
							if (TRANSLATIONS_STATISTICS.containsKey(path[0])) {
								translationStatistics = TRANSLATIONS_STATISTICS.get(path[0]);
							} else {
								translationStatistics = new TranslationStatistics();
								TRANSLATIONS_STATISTICS.put(path[0], translationStatistics);
							}
							if (path.length < 2) {
								LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
							} else if (path[1].equalsIgnoreCase("name")) {
								translationStatistics.name = matcher.group(2);
							} else if (path[1].equalsIgnoreCase("phrases")) {
								if (path.length < 3) {
									translationStatistics.phrases = Integer.parseInt(matcher.group(2));
								} else {
									switch (path[2].toLowerCase(Locale.US)) {
										case "approved":
											translationStatistics.phrasesApproved = Integer.parseInt(matcher.group(2));
											break;
										case "translated":
											translationStatistics.phrasesTranslated = Integer.parseInt(matcher.group(2));
											break;
										default:
											LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
									}
								}
							} else if (path[1].equalsIgnoreCase("words")) {
								if (path.length < 3) {
									translationStatistics.words = Integer.parseInt(matcher.group(2));
								} else {
									switch (path[2].toLowerCase(Locale.US)) {
										case "approved":
											translationStatistics.wordsApproved = Integer.parseInt(matcher.group(2));
											break;
										case "translated":
											translationStatistics.wordsTranslated = Integer.parseInt(matcher.group(2));
											break;
										default:
											LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
									}
								}
							} else if (path[1].equalsIgnoreCase("progress")) {
								if (path.length < 3) {
									LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
								} else {
									switch (path[2].toLowerCase(Locale.US)) {
										case "approved":
											translationStatistics.approved = Integer.parseInt(matcher.group(2));
											break;
										case "translated":
											translationStatistics.translated = Integer.parseInt(matcher.group(2));
											break;
										default:
											LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
									}
								}
							} else {
								LOGGER.debug("Failed to parse translation statistics line \"{}\": Illegal qualifier", line);
							}
						} catch (NumberFormatException e) {
							LOGGER.debug("Failed to parse translation statistics line \"{}\": ", line, e.getMessage());
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error("Error reading translations statistics: {}", e.getMessage());
				LOGGER.trace("", e);
				TRANSLATIONS_STATISTICS.clear();
			}
		}
	}

	/**
	 * This method must be called in a context synchronized on
	 * {@link #TRANSLATIONS_STATISTICS}.
	 */
	private static LanguageEntry getSortedLanguageByTag(String tag) {
		for (LanguageEntry entry : SORTED_LANGUAGES) {
			if (entry.tag.equalsIgnoreCase(tag)) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * This method must be called in a context synchronized on
	 * {@link #TRANSLATIONS_STATISTICS}.
	 */
	private static LanguageEntry getSortedLanguageByLocale(Locale locale) {
		for (LanguageEntry entry : SORTED_LANGUAGES) {
			if (entry.locale.equals(locale)) {
				return entry;
			}
		}

		// No exact match found, try to match only by language and country
		for (LanguageEntry entry : SORTED_LANGUAGES) {
			if (entry.locale.getCountry().equals(locale.getCountry()) && entry.locale.getLanguage().equals(locale.getLanguage())) {
				return entry;
			}
		}

		// No match found on language and country, try to match only by language
		for (LanguageEntry entry : SORTED_LANGUAGES) {
			if (entry.locale.getLanguage().equals(locale.getLanguage())) {
				return entry;
			}
		}

		// No match found on language, try a last desperate match only by
		// country
		if (!locale.getCountry().isEmpty()) {
			for (LanguageEntry entry : SORTED_LANGUAGES) {
				if (entry.locale.getCountry().equals(locale.getCountry())) {
					return entry;
				}
			}
		}

		// No match found, return null
		return null;
	}

	/**
	 * Returns whether the given {@link LanguageEntry} qualifies for being
	 * recommended/default choice. English languages is always recommended, as
	 * there's no way for us to calculate coverage for them since only the
	 * strings that deviate from US-English is translated.
	 *
	 * @param language the {@link LanguageEntry} to evaluate
	 * @return The result
	 */
	private static boolean isRecommended(LanguageEntry language) {
		return language.tag.startsWith("en") || language.coveragePercent >= RECOMMENDED_TRANSLATE_PCT ||
			language.approvedPercent >= RECOMMENDED_APPROVED_PCT;
	}

	/**
	 * Returns whether the given {@link TranslationStatistics} qualifies for
	 * being recommended/default choice. English languages cannot be evaluated
	 * by this method and should always be considered recommended.
	 *
	 * @param languageStatistics the {@link TranslationStatistics} to evaluate
	 * @return The result
	 */

	private static boolean isRecommended(TranslationStatistics languageStatistics) {
		return languageStatistics.translated >= RECOMMENDED_TRANSLATE_PCT || languageStatistics.approved >= RECOMMENDED_APPROVED_PCT;

	}

	/**
	 * This method must be called in a context synchronized on
	 * {@link #TRANSLATIONS_STATISTICS}.
	 * <p>
	 * The sorting places the default/recommended choice on top of the list, and
	 * then tried to place other relevant choices close to the top in descending
	 * order by relevance. The rest of the list is alphabetical by the
	 * preferred/currently selected language's language names. The sorting is
	 * done following these rules:
	 * <ul>
	 * <li>The base language (en-US) and the language closest matching
	 * <code>preferredLocale</code> is looked up. If the closest matching
	 * language has a coverage greater or equal to
	 * {@link #RECOMMENDED_TRANSLATE_PCT} or an approval greater or equal to
	 * {@link #RECOMMENDED_APPROVED_PCT} it will be placed on top. If not, the
	 * base language will be placed on top. Whichever of these is not placed on
	 * top is placed second. If a closely matching language cannot be found,
	 * only the base language will be placed on top.</li>
	 * <li>A search for related languages is performed. Related is defined by
	 * either having the same language code (e.g "en") or the same country code
	 * as <code>preferredLocale</code>. Related languages are then sorted
	 * descending by coverage and put after that or those language(s) placed on
	 * top.</li>
	 * <li>The rest of the languages are listed alphabetically based on their
	 * localized (from currently chosen language) names.
	 * </ul>
	 *
	 * If the localized language name differs from the English language name,
	 * the English language name is shown in parenthesis. This is to help in
	 * case the localized names are incomprehensible to the user.
	 */
	private static void createSortedList(Locale preferredLocale) {
		if (preferredLocale == null) {
			throw new IllegalArgumentException("preferredLocale cannot be null");
		}
		if (lastpreferredLocale == null || !lastpreferredLocale.equals(preferredLocale)) {
			// Populate
			lastpreferredLocale = preferredLocale;
			SORTED_LANGUAGES.clear();
			populateTranslationsStatistics();
			for (String tag : UMS_BCP47_CODES) {
				LanguageEntry entry = new LanguageEntry();
				entry.tag = tag;
				entry.name = Messages.getString("Language." + tag, preferredLocale);
				entry.defaultname = Messages.getRootString("Language." + tag);
				entry.locale = Locale.forLanguageTag(tag);
				if (tag.equals("en-US")) {
					entry.coveragePercent = 100;
					entry.approvedPercent = 100;
				} else {
					TranslationStatistics stats = TRANSLATIONS_STATISTICS.get(tag);
					if (stats != null) {
						if (entry.locale.getLanguage().equals("en") && stats.wordsTranslated > 0) {
							/*
							 * Special case for English language variants that
							 * only overrides the strings that differ from US
							 * English. We cannot find coverage for these
							 */
							entry.coveragePercent = 100;
							entry.approvedPercent = 100;
						} else {
							entry.coveragePercent = stats.translated;
							entry.approvedPercent = stats.approved;
						}
					} else {
						entry.coveragePercent = 0;
						entry.approvedPercent = 0;
						LOGGER.debug("Warning: Could not find language statistics for {}", entry.name);
					}
				}
				String country = entry.locale.getCountry();
				if ("".equals(country)) {
					country = switch (entry.locale.getLanguage()) {
						case "ca" -> "es";
						case "cs" -> "CZ";
						case "da" -> "DK";
						case "el" -> "GR";
						case "fa" -> "IR";
						case "he" -> "IL";
						case "ja" -> "JP";
						case "ko" -> "KR";
						case "uk" -> "UA";
						case "zh" -> "cn";
						default -> entry.locale.getLanguage();
					};
				}
				entry.country = country;
				if (entry.coveragePercent >= MINIMUM_TRANSLATE_PCT) {
					SORTED_LANGUAGES.add(entry);
				}
			}

			// Sort
			Collections.sort(SORTED_LANGUAGES);

			// Put US English first
			LanguageEntry baseLanguage = getSortedLanguageByTag("en-US");
			if (baseLanguage == null) {
				throw new IllegalStateException("Languages.createSortedList encountered an impossible situation");
			}
			if (SORTED_LANGUAGES.remove(baseLanguage)) {
				SORTED_LANGUAGES.add(0, baseLanguage);
			}

			// Put matched language first or second depending on coverage
			LanguageEntry preferredLanguage = getSortedLanguageByLocale(preferredLocale);
			if (preferredLanguage != null && !preferredLanguage.tag.equals("en-US")) {
				if (SORTED_LANGUAGES.remove(preferredLanguage) && isRecommended(preferredLanguage)) {
					SORTED_LANGUAGES.add(0, preferredLanguage);
				} else {
					/*
					 * This could constitute a bug if
					 * SORTED_LANGUAGES.remove(entry) returned false, but that
					 * should be impossible
					 */
					SORTED_LANGUAGES.add(1, preferredLanguage);
				}
			}

			// Put related language(s) close to top
			List<LanguageEntry> relatedLanguages = new ArrayList<>();
			for (LanguageEntry entry : SORTED_LANGUAGES) {
				if (
					entry != baseLanguage &&
					entry != preferredLanguage &&
					(!preferredLocale.getCountry().isEmpty() &&
					preferredLocale.getCountry().equals(entry.locale.getCountry()) ||
					!preferredLocale.getLanguage().isEmpty() &&
					preferredLocale.getLanguage().equals(entry.locale.getLanguage()))
				) {
					relatedLanguages.add(entry);
				}
			}
			if (!relatedLanguages.isEmpty()) {
				SORTED_LANGUAGES.removeAll(relatedLanguages);
				Collections.sort(relatedLanguages, new LanguageEntryCoverageComparator());
				SORTED_LANGUAGES.addAll(preferredLanguage == null || preferredLanguage.equals(baseLanguage) ? 1 : 2, relatedLanguages);
			}
		}
	}

	/**
	 * Reads translations statistics from resource file
	 * <code>languages.properties</code> and returns them in a {@link HashMap}
	 * with language tags as keys. Results are cached for subsequent reads.
	 * <p>
	 * <strong>The returned {@link HashMap} is never <code>null</code> and must
	 * always be synchronized on itself during read or write</strong>
	 *
	 * @return The resulting {@link HashMap}
	 */
	public static Map<String, TranslationStatistics> getTranslationsStatistics() {
		synchronized (TRANSLATIONS_STATISTICS) {
			populateTranslationsStatistics();
			return TRANSLATIONS_STATISTICS;
		}
	}

	/**
	 * Returns whether the given language has a translation percentage that
	 * doesn't qualify it as being recommended/default choice. English languages
	 * are always considered recommended since we can't calculate their
	 * coverage.
	 *
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return <code>True</code> if a warning should be given for that language
	 */
	public static boolean warnCoverage(String languageTag) {
		if (languageTag.startsWith("en")) {
			return false;
		}
		synchronized (TRANSLATIONS_STATISTICS) {
			populateTranslationsStatistics();
			TranslationStatistics stats = TRANSLATIONS_STATISTICS.get(languageTag);
			if (stats == null) {
				return true;
			}
			return !isRecommended(stats);
		}
	}

	/**
	 * Returns the percentage of strings that is translation for the given
	 * language. English languages always return 100% since we have no way to
	 * calculate their coverage due to the fact that only those strings that
	 * differ from US-English is translated.
	 *
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return The percentage
	 */
	public static int getLanguageCoverage(String languageTag) {
		if (languageTag.startsWith("en")) {
			return 100;
		}
		synchronized (TRANSLATIONS_STATISTICS) {
			populateTranslationsStatistics();
			TranslationStatistics stats = TRANSLATIONS_STATISTICS.get(languageTag);
			if (stats == null) {
				return 0;
			}
			return stats.translated;
		}
	}

	/**
	 * Verifies if a given
	 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag is supported by UMS.
	 *
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return The result.
	 */
	public static boolean isValid(String languageTag) {
		if (languageTag != null && !languageTag.isEmpty()) {
			for (String code : UMS_BCP47_CODES) {
				if (code.equalsIgnoreCase(languageTag)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Verifies if a given {@link java.util.Locale} is supported by UMS.
	 *
	 * @param locale The {@link java.util.Locale}.
	 * @return The result.
	 */
	public static boolean isValid(Locale locale) {
		return isValid(localeToLanguageTag(locale));
	}

	/**
	 * Verifies if a given
	 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag is or can be converted into a language tag supported by UMS.
	 *
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return The result.
	 */
	public static boolean isCompatible(String languageTag) {
		return isValid(languageTagToUMSLanguageTag(languageTag));
	}

	/**
	 * Returns a correctly capitalized
	 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag if the language tag is supported by UMS, or returns null.
	 *
	 * @param languageTag The IEFT BCP 47 compatible language tag.
	 * @return The IEFT BCP 47 formatted language tag.
	 */
	public static String toLanguageTag(String languageTag) {
		if (languageTag != null && !languageTag.isEmpty()) {
			languageTag = languageTagToUMSLanguageTag(languageTag);
			for (String tag : UMS_BCP47_CODES) {
				if (tag.equalsIgnoreCase(languageTag)) {
					return tag;
				}
			}
		}
		return null;
	}

	/**
	 * Returns a correctly capitalized
	 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag if the language tag is supported by UMS, or returns null.
	 *
	 * @param locale The {@link java.util.Locale}.
	 * @return The IEFT BCP 47 formatted language tag.
	 */
	public static String toLanguageTag(Locale locale) {
		if (locale != null) {
			return toLanguageTag(localeToLanguageTag(locale));
		}
		return null;
	}

	/**
	 * Returns a UMS supported {@link java.util.Locale} from the given
	 * <code>Local</code> if it can be found (<code>en</code> is translated to
	 * <code>en-US</code>, <code>zh</code> to <code>zh-Hant</code> etc.).
	 * Returns <code>null</code> if a valid <code>Locale</code> cannot be found.
	 *
	 * @param locale Source {@link java.util.Locale}.
	 * @return Resulting {@link java.util.Locale}.
	 */
	public static Locale toLocale(Locale locale) {
		if (locale != null) {
			String tag = localeToLanguageTag(locale);
			if (tag != null && isValid(tag)) {
				return Locale.forLanguageTag(tag);
			}
		}
		return null;
	}

	/**
	 * Returns a UMS supported {@link Locale} from the given
	 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * if it can be found ({@code en} is translated to {@code en-US}, {@code zh}
	 * to {@code zh-Hant} etc.). Returns {@code null} if a valid {@link Locale}
	 * cannot be found.
	 *
	 * @param languageTag the IEFT BCP 47 code to convert.
	 * @return The resulting {@link Locale}.
	 */
	public static Locale toLocale(String languageTag) {
		if (languageTag != null) {
			String tag = languageTagToUMSLanguageTag(languageTag);
			if (isValid(tag)) {
				return Locale.forLanguageTag(tag);
			}
		}
		return null;
	}

	/**
	 * Returns a sorted string array of UMS supported language tags. The sorting
	 * will match that returned by {@link #getLanguageNames(Locale)} for the
	 * same {@code preferredLocale} for easy use with {@link JComboBox}. For
	 * sorting details see {@link #createSortedList(Locale)}.
	 *
	 * @param preferredLocale the {@link Locale} to be seen as preferred when
	 *            sorting the array.
	 * @return The sorted string array of language tags.
	 */
	public static String[] getLanguageTags(Locale preferredLocale) {
		synchronized (TRANSLATIONS_STATISTICS) {
			createSortedList(preferredLocale);
			String[] tags = new String[SORTED_LANGUAGES.size()];
			for (int i = 0; i < SORTED_LANGUAGES.size(); i++) {
				tags[i] = SORTED_LANGUAGES.get(i).tag;
			}

			return tags;
		}
	}

	/**
	 * Returns a sorted string array of localized UMS supported language names
	 * with coverage/translation percentage in parenthesis. The sorting will
	 * match that returned by {@link #getLanguageTags(Locale)} for the same
	 * {@code preferredLocale} for easy use with {@link JComboBox}. For sorting
	 * details see {@link #createSortedList(Locale)}.
	 *
	 * @param preferredLocale the {@link Locale} to be seen as preferred when
	 *            sorting the array, and used when localizing language names.
	 * @return The sorted string array of localized language names.
	 */
	public static String[] getLanguageNames(Locale preferredLocale) {
		synchronized (TRANSLATIONS_STATISTICS) {
			createSortedList(preferredLocale);
			String[] languages = new String[SORTED_LANGUAGES.size()];
			for (int i = 0; i < SORTED_LANGUAGES.size(); i++) {
				LanguageEntry entry = SORTED_LANGUAGES.get(i);
				String name = entry.name;
				if (!entry.name.equals(entry.defaultname)) {
					name += String.format(" (%s)", entry.defaultname);
				}
				if (!entry.locale.getLanguage().equals("en")) {
					/*
					 * Only show coverage on non-English languages as we can't
					 * calculate if for English because they only override
					 * what's different from US English.
					 */
					name += String.format(" (%d%%)", entry.coveragePercent);
				}
				languages[i] = name;
			}

			return languages;
		}
	}

	/**
	 * Returns a sorted jsonned string of localized UMS supported languages.
	 *
	 * @param locale the language to be seen as preferred when
	 *            sorting the array, and used when localizing language names.
	 * @return The sorted jsonned string of localized languages.
	 */
	public static JsonArray getLanguagesAsJsonArray(Locale locale) {
		Locale preferredLocale = toLocale(locale);
		if (preferredLocale == null) {
			preferredLocale = PMS.getLocale();
		}
		synchronized (TRANSLATIONS_STATISTICS) {
			createSortedList(preferredLocale);
			JsonArray jsonArray = new JsonArray();
			for (int i = 0; i < SORTED_LANGUAGES.size(); i++) {
				JsonObject objectGroup = new JsonObject();
				LanguageEntry entry = SORTED_LANGUAGES.get(i);
				objectGroup.addProperty("id", entry.tag);
				objectGroup.addProperty("name", entry.name);
				objectGroup.addProperty("defaultname", entry.defaultname);
				objectGroup.addProperty("country", entry.country);
				objectGroup.addProperty("coverage", entry.coveragePercent);
				jsonArray.add(objectGroup);
			}
			return jsonArray;
		}
	}

	public static boolean getLanguageIsRtl(Locale locale) {
		return switch (locale.getLanguage()) {
			case "ar" -> true; //arab
			case "fa" -> true; //persian
			default -> false;
		};
	}

}
