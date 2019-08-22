/*
 * Universal Media Server, for streaming any media to DLNA
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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComboBox;
import net.pms.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a utility class for translation between {@link java.util.Locale}'s
 * <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> and
 * UMS' language files. See <a href="http://r12a.github.io/apps/subtags/">here
 * for subtag lookup</a>. If UMS languages are removed or added, this class needs
 * to be updated. The class is immutable.
 *
 * To add a new language, the following must be done:
 * <ul>
 * <li>Add the BCP47 code to {@link #UMS_BCP47_CODES}</li>
 * <li>Add a new label called "{@code Language.<BCP47 tag>}" to {@code messages.properties}</li>
 * <li>Add the language to UMS.conf</li>
 * <li>Modify {@link #localeToLanguageTag(Locale)} to handle the language</li>
 * <li>Modify {@link #languageTagToUMSLanguageTag(String)} to handle the language</li>
 * <li>Add the language at crowdin</li>
 * <li>Pull crowdin translations containing the new language so that the language file is committed</li>
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
	 * Defines the minimum translation percentage a language can have to be
	 * the recommended/default language.
	 */
	private static final int RECOMMENDED_TRANSLATE_PCT = 90;

	/**
	 * Defines the minimum approved translation percentage a language can have
	 * to be the recommended/default language.
	 */
	private static final int RECOMMENDED_APPROVED_PCT = 85;

	private static final Logger LOGGER = LoggerFactory.getLogger(Languages.class);

	/**
	 * If the below list is changed, methods {@link #localeToLanguageTag(Locale)} and
	 * {@link #languageTagToUMSLanguageTag(String)} must be updated correspondingly.
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
	 * This map is also used as a synchronization object for {@link #translationsStatistics},
	 * {@link #lastpreferredLocale} and {@link #sortedLanguages}
	 */
	private static HashMap<String, TranslationStatistics> translationsStatistics = new HashMap<>(
		(int) Math.round(UMS_BCP47_CODES.length * 1.34)
	);
	private static Locale lastpreferredLocale = null;
	private static List<LanguageEntry> sortedLanguages = new ArrayList<>();

	@SuppressWarnings("unused")
	private static class TranslationStatistics {
		public String name;
		public int phrases;
		public int phrasesApproved;
		public int phrasesTranslated;
		public int words;
		public int wordsApproved;
		public int wordsTranslated;
		public int approved;
		public int translated;
	}

	/*
	 * Note: this class has a natural ordering that is inconsistent with equals.
	 */
	private static class LanguageEntry implements Comparable<LanguageEntry> {
		public String tag;
		public String name;
		public Locale locale = null;
		public int coveragePercent;
		public int approvedPercent;

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
			if (approvedPercent != other.approvedPercent) {
				return false;
			}
			if (coveragePercent != other.coveragePercent) {
				return false;
			}
			if (locale == null) {
				if (other.locale != null) {
					return false;
				}
			} else if (!locale.equals(other.locale)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (tag == null) {
				if (other.tag != null) {
					return false;
				}
			} else if (!tag.equals(other.tag)) {
				return false;
			}
			return true;
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
		String languageTag = locale.getLanguage();
		if (languageTag != null && !languageTag.isEmpty()) {
			switch (languageTag) {
				case "en":
					if (locale.getCountry().equalsIgnoreCase("GB")) {
						return "en-GB";
					}
					return "en-US";
				case "pt":
					if (locale.getCountry().equalsIgnoreCase("BR")) {
						return "pt-BR";
					}
					return "pt";
				case "nb":
				case "nn":
					return "no";
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
					return languageTag;
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
			case "en-gb":
				return "en-GB";
			case "pt-br":
				return "pt-BR";
			case "cmn-cn":
			case "cmn-sg":
			case "cmn-hans":
			case "zh-cn":
			case "zh-sg":
			case "zh-hans":
				return "zh-Hans";
			default:
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

	@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
	private static void populateTranslationsStatistics() {
		if (translationsStatistics.size() < 1) {
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
							if (translationsStatistics.containsKey(path[0])) {
								translationStatistics = translationsStatistics.get(path[0]);
							} else {
								translationStatistics = new TranslationStatistics();
								translationsStatistics.put(path[0], translationStatistics);
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
				translationsStatistics.clear();
			}
		}
	}

	/**
	 * This method must be called in a context synchronized on {@link #translationsStatistics}.
	 */
	private static LanguageEntry getSortedLanguageByTag(String tag) {
		for (LanguageEntry entry : sortedLanguages) {
			if (entry.tag.equalsIgnoreCase(tag)) {
				return entry;
			}
		}
		return null;
	}

	/**
	 * This method must be called in a context synchronized on {@link #translationsStatistics}.
	 */
	private static LanguageEntry getSortedLanguageByLocale(Locale locale) {
		for (LanguageEntry entry : sortedLanguages) {
			if (entry.locale.equals(locale)) {
				return entry;
			}
		}

		// No exact match found, try to match only by language and country
		for (LanguageEntry entry : sortedLanguages) {
			if (entry.locale.getCountry().equals(locale.getCountry()) && entry.locale.getLanguage().equals(locale.getLanguage())) {
				return entry;
			}
		}

		// No match found on language and country, try to match only by language
		for (LanguageEntry entry : sortedLanguages) {
			if (entry.locale.getLanguage().equals(locale.getLanguage())) {
				return entry;
			}
		}

		// No match found on language, try a last desperate match only by country
		if (!locale.getCountry().isEmpty()) {
			for (LanguageEntry entry : sortedLanguages) {
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
	 * recommended/default choice. English languages is always recommended,
	 * as there's no way for us to calculate coverage for them since only the
	 * strings that deviate from US-English is translated.
	 * @param language the {@link LanguageEntry} to evaluate
	 * @return The result
	 */
	private static boolean isRecommended(LanguageEntry language) {
		return
			language.tag.startsWith("en") ||
			language.coveragePercent >= RECOMMENDED_TRANSLATE_PCT ||
			language.approvedPercent >= RECOMMENDED_APPROVED_PCT;
	}

	/**
	 * Returns whether the given {@link TranslationStatistics} qualifies for
	 * being recommended/default choice. English languages cannot be evaluated
	 * by this method and should always be considered recommended.
	 * @param languageStatistics the {@link TranslationStatistics} to evaluate
	 * @return The result
	 */

	private static boolean isRecommended(TranslationStatistics languageStatistics) {
		return languageStatistics.translated >= RECOMMENDED_TRANSLATE_PCT || languageStatistics.approved >= RECOMMENDED_APPROVED_PCT;

	}

	/**
	 * This method must be called in a context synchronized on {@link #translationsStatistics}.
	 * <p>
	 * The sorting places the default/recommended choice on top of the list,
	 * and then tried to place other relevant choices close to the top in
	 * descending order by relevance. The rest of the list is alphabetical
	 * by the preferred/currently selected language's language names.
	 * The sorting is done following these rules:
	 * <ul>
	 *   <li>The base language (en-US) and the language closest matching
	 *       <code>preferredLocale</code> is looked up. If the closest matching
	 *       language has a coverage greater or equal to {@link #RECOMMENDED_TRANSLATE_PCT}
	 *       or an approval greater or equal to {@link #RECOMMENDED_APPROVED_PCT} it
	 *       will be placed on top. If not, the base language will be placed on
	 *       top. Whichever of these is not placed on top is placed second. If
	 *       a closely matching language cannot be found, only the base language
	 *       will be placed on top.</li>
	 *   <li>A search for related languages is performed. Related is defined by
	 *       either having the same language code (e.g "en") or the same country
	 *       code as <code>preferredLocale</code>. Related languages are then
	 *       sorted descending by coverage and put after that or those
	 *       language(s) placed on top.</li>
	 *   <li>The rest of the languages are listed alphabetically based on their
	 *       localized (from currently chosen language) names.
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
			sortedLanguages.clear();
			populateTranslationsStatistics();
			for (String tag : UMS_BCP47_CODES) {
				LanguageEntry entry = new LanguageEntry();
				entry.tag = tag;
				entry.name = Messages.getString("Language." + tag, preferredLocale);
				if (!entry.name.equals(Messages.getRootString("Language." + tag))) {
					entry.name += " (" + Messages.getRootString("Language." + tag) + ")";
				}
				entry.locale = Locale.forLanguageTag(tag);
				if (tag.equals("en-US")) {
					entry.coveragePercent = 100;
					entry.approvedPercent = 100;
				} else {
					TranslationStatistics stats = translationsStatistics.get(tag);
					if (stats != null) {
						if (entry.locale.getLanguage().equals("en") && stats.wordsTranslated > 0) {
							/* Special case for English language variants that only
							 * overrides the strings that differ from US English.
							 * We cannot find coverage for these */
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

				if (entry.coveragePercent >= MINIMUM_TRANSLATE_PCT) {
					sortedLanguages.add(entry);
				}
			}

			// Sort
			Collections.sort(sortedLanguages);

			// Put US English first
			LanguageEntry baseLanguage = getSortedLanguageByTag("en-US");
			if (baseLanguage == null) {
				throw new IllegalStateException("Languages.createSortedList encountered an impossible situation");
			}
			if (sortedLanguages.remove(baseLanguage)) {
				sortedLanguages.add(0, baseLanguage);
			}

			// Put matched language first or second depending on coverage
			LanguageEntry preferredLanguage = getSortedLanguageByLocale(preferredLocale);
			if (preferredLanguage != null && !preferredLanguage.tag.equals("en-US")) {
				if (
					sortedLanguages.remove(preferredLanguage) && isRecommended(preferredLanguage)
				) {
					sortedLanguages.add(0, preferredLanguage);
				} else {
					/* This could constitute a bug if sortedLanguages.remove(entry)
					 * returned false, but that should be impossible */
					sortedLanguages.add(1, preferredLanguage);
				}
			}

			// Put related language(s) close to top
			List<LanguageEntry> relatedLanguages = new ArrayList<>();
			for (LanguageEntry entry : sortedLanguages) {
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
			if (relatedLanguages.size() > 0) {
				sortedLanguages.removeAll(relatedLanguages);
				Collections.sort(relatedLanguages, new LanguageEntryCoverageComparator());
				sortedLanguages.addAll(preferredLanguage == null || preferredLanguage.equals(baseLanguage) ? 1 : 2, relatedLanguages);
			}
		}
	}

	/**
	 * Reads translations statistics from resource file <code>languages.properties</code>
	 * and returns them in a {@link HashMap} with language tags as keys.
	 * Results are cached for subsequent reads.
	 * <p>
	 * <strong>The returned {@link HashMap} is never <code>null</code> and must
	 * always be synchronized on itself during read or write</strong>
	 * @return The resulting {@link HashMap}
	 */
	public static HashMap<String, TranslationStatistics> getTranslationsStatistics() {
		synchronized (translationsStatistics) {
			populateTranslationsStatistics();
			return translationsStatistics;
		}
	}

	/**
	 * Returns whether the given language has a translation percentage that
	 * doesn't qualify it as being recommended/default choice. English
	 * languages are always considered recommended since we can't calculate
	 * their coverage.
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return <code>True</code> if a warning should be given for that language
	 */
	public static boolean warnCoverage(String languageTag) {
		if (languageTag.startsWith("en")) {
			return false;
		}
		synchronized (translationsStatistics) {
			populateTranslationsStatistics();
			TranslationStatistics stats = translationsStatistics.get(languageTag);
			if (stats == null) {
				return true;
			}
			return !isRecommended(stats);
		}
	}

	/**
	 * Returns the percentage of strings that is translation for the given
	 * language. English languages always return 100% since we have no way
	 * to calculate their coverage due to the fact that only those strings
	 * that differ from US-English is translated.
	 *
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return The percentage
	 */
	public static int getLanguageCoverage(String languageTag) {
		if (languageTag.startsWith("en")) {
			return 100;
		}
		synchronized (translationsStatistics) {
			populateTranslationsStatistics();
			TranslationStatistics stats = translationsStatistics.get(languageTag);
			if (stats == null) {
				return 0;
			}
			return stats.translated;
		}
	}

	/**
	 * Verifies if a given <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
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
	 * Verifies if a given <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
	 * language tag is or can be converted into a language tag supported by UMS.
	 *
	 * @param languageTag The language tag in IEFT BCP 47 format.
	 * @return The result.
	 */
	public static boolean isCompatible(String languageTag) {
		return isValid(languageTagToUMSLanguageTag(languageTag));
	}

	/**
	 * Returns a correctly capitalized <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
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
	 * Returns a correctly capitalized <a href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a>
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
	 * Returns a UMS supported {@link Locale} from the given <a
	 * href="https://en.wikipedia.org/wiki/IETF_language_tag">IEFT BCP 47</a> if
	 * it can be found ({@code en} is translated to {@code en-US}, {@code zh} to
	 * {@code zh-Hant} etc.). Returns {@code null} if a valid {@link Locale}
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
		synchronized (translationsStatistics) {
			createSortedList(preferredLocale);
			String[] tags = new String[sortedLanguages.size()];
			for (int i = 0; i < sortedLanguages.size(); i++) {
				tags[i] = sortedLanguages.get(i).tag;
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
		synchronized (translationsStatistics) {
			createSortedList(preferredLocale);
			String[] languages = new String[sortedLanguages.size()];
			for (int i = 0; i < sortedLanguages.size(); i++) {
				LanguageEntry entry = sortedLanguages.get(i);
				if (entry.locale.getLanguage().equals("en")) {
					languages[i] = entry.name;
				} else {
					/* Only show coverage on non-English languages as we can't
					 * calculate if for English because they only override
					 * what's different from US English.*/
					languages[i] = entry.name + String.format(" (%d%%)", entry.coveragePercent);
				}
			}

			return languages;
		}
	}
}
