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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */
package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaLang;


/**
 * This class provides a list of languages mapped to {@code ISO 639} language
 * codes and some methods to verify which language matches which ISO code.
 * <p>
 * {@code ISO 639 codes} updated <b>2018-02-01</b>.
 */
@SuppressWarnings({
	"checkstyle:MemberName",
	"checkstyle:MethodLength",
	"checkstyle:MethodName",
	"checkstyle:ParameterName",
})
public final class Iso639 {

	/**
	 * ISO code alias for the language set in the preferences
	 */
	private static final String LOCAL_ALIAS = "loc";

	/**
	 * {@link HashMap} that contains the lower-case English language name as the
	 * key with the corresponding {@link Iso639Entry} as the value.
	 */
	private static HashMap<String, Iso639Entry> links = new HashMap<>();

	/**
	 * {@link List} that contains all known ISO language codes.
	 */
	private static final List<String> CODES;

	private static final Map<String, String> COMMON_MISSPELLINGS;

	static {
		// Make sure everything is initialized before it is retrieved.
		initLinks();
		CODES = Collections.unmodifiableList(initCodes());
		COMMON_MISSPELLINGS = Collections.unmodifiableMap(buildMisspellings());
	}

	/**
	 * Not to be instantiated.
	 */
	private Iso639() {
	}

	/**
	 * Returns whether the code is a valid/known {@code ISO 639} code or not.
	 *
	 * @param code the {@code ISO 639} language code.
	 * @return {@code true} if the code is known, {@code false} otherwise.
	 */
	public static boolean codeIsValid(String code) {
		return isBlank(code) ? false : CODES.contains(code.trim().toLowerCase(Locale.ROOT));
	}

	/**
	 * Determines if the specified {@link String} is either a valid
	 * {@code ISO 639} code or an English {@code ISO 639} language name.
	 *
	 * @param code the {@code ISO 639} code or English language name.
	 * @return {@code true} if a match is found, {@code false} otherwise.
	 */
	public static boolean isValid(String code) {
		if (isBlank(code)) {
			return false;
		}
		code = code.trim().toLowerCase(Locale.ROOT);
		return CODES.contains(code) || links.containsKey(code);
	}

	/**
	 * Gets the {@link Iso639Entry} for an English {@code ISO 639} language name
	 * or an {@code ISO 639} code, or {@code null} if no match can be found.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @return The matching {@link Iso639Entry} or {@code null}.
	 */
	public static Iso639Entry get(String code) {
		return get(code, false);
	}

	/**
	 * Gets the {@link Iso639Entry} for an English {@code ISO 639} language name
	 * or an {@code ISO 639} code, or {@code null} if no match can be found. Can
	 * optionally also search {@code code} for the English language name.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @param containsName if {@code true}, a search for the English language
	 *            name will also be performed.
	 * @return The matching {@link Iso639Entry} or {@code null}.
	 */
	public static Iso639Entry get(String code, boolean containsName) {
		if (isBlank(code)) {
			return null;
		}

		code = code.trim().toLowerCase(Locale.ROOT);
		if (LOCAL_ALIAS.equals(code)) {
			code = normalize(code);
			if (isBlank(code)) {
				code = "eng"; // Fall back to English
			}
		}

		for (Iso639Entry entry : links.values()) {
			if (entry.matches(code)) {
				return entry;
			}
		}

		String tmpCode = COMMON_MISSPELLINGS.get(code);
		if (tmpCode != null) {
			code = tmpCode;
		}

		Iso639Entry result = links.get(code);
		if (result != null) {
			return result;
		}

		if (containsName && code.length() > 2) {
			// Do a search for a match for the language name in "code"
			for (Entry<String, String> misspelling : COMMON_MISSPELLINGS.entrySet()) {
				if (code.contains(misspelling.getKey())) {
					code = code.replace(misspelling.getKey(), misspelling.getValue());
				}
			}

			for (Entry<String, Iso639Entry> entry : links.entrySet()) {
				if (code.contains(entry.getKey())) {
					return entry.getValue();
				}
			}
		}

		return null;
	}

	/**
	 * Gets the first defined English {@code ISO 639} language name for an
	 * English {@code ISO 639} language name or an {@code ISO 639} code, or
	 * {@code null} if no match can be found.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @return The {@code ISO 639} English language name or {@code null}.
	 */
	public static String getFirstName(String code) {
		return getFirstName(code, false);
	}

	/**
	 * Gets the first defined English {@code ISO 639} language name for an
	 * English {@code ISO 639} language name or an {@code ISO 639} code, or
	 * {@code null} if no match can be found. Can optionally also search
	 * {@code code} for the English language name.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @param containsName if {@code true}, a search for the English language
	 *            name will also be performed.
	 * @return The {@code ISO 639} English language name or {@code null}.
	 */
	public static String getFirstName(String code, boolean containsName) {
		Iso639Entry entry = get(code, containsName);
		return entry == null ? null : entry.getFirstName();
	}

	/**
	 * Gets the array of English {@code ISO 639} language names for an English
	 * {@code ISO 639} language name or an {@code ISO 639} code, or {@code null}
	 * if no match can be found.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @return The array of {@code ISO 639} English language names or
	 *         {@code null}.
	 */
	public static String[] getNames(String code) {
		return getNames(code, false);
	}

	/**
	 * Gets the array of English {@code ISO 639} language names for an English
	 * {@code ISO 639} language name or an {@code ISO 639} code, or {@code null}
	 * if no match can be found. Can optionally also search {@code code} for the
	 * English language name.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @param containsName if {@code true}, a search for the English language
	 *            name will also be performed.
	 * @return The array of {@code ISO 639} English language names or
	 *         {@code null}.
	 */
	public static String[] getNames(String code, boolean containsName) {
		Iso639Entry entry = get(code, containsName);
		return entry == null ? null : entry.getNames();
	}

	/**
	 * Gets the shortest possible (as per {@link Locale} specification)
	 * {@code ISO 639} (two or three letter) code for an English {@code ISO 639}
	 * language name or an {@code ISO 639} code, or {@code null} if no match can
	 * be found.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @return The {@code ISO 639-1} (two letter), {@code ISO 639-2} (three
	 *         letter) code or {@code null}.
	 */
	public static String getISOCode(String code) {
		return getISOCode(code, false);
	}

	/**
	 * Gets the shortest possible (as per {@link Locale} specification)
	 * {@code ISO 639} (two or three letter) code for an English {@code ISO 639}
	 * language name or an {@code ISO 639} code, or {@code null} if no match can
	 * be found. Can optionally also search {@code code} for the English
	 * language name.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @param containsName if {@code true}, a search for the English language
	 *            name will also be performed.
	 * @return The {@code ISO 639-1} (two letter), {@code ISO 639-2} (three
	 *         letter) code or {@code null}.
	 */
	public static String getISOCode(String code, boolean containsName) {
		Iso639Entry entry = get(code, containsName);
		return entry == null ? null : isNotBlank(entry.iso639_1) ? entry.iso639_1 : entry.iso639_2;
	}

	/**
	 * Gets the {@code ISO 639-2} (three letter) code for an English
	 * {@code ISO 639} language name or an {@code ISO 639} code, or {@code null}
	 * if no match can be found.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @return The {@code ISO 639-2} (three letter) code or {@code null}.
	 */
	public static String getISO639_2Code(String code) {
		return getISO639_2Code(code, false);
	}

	/**
	 * Gets the {@code ISO 639-2} (three letter) code for an English
	 * {@code ISO 639} language name or an {@code ISO 639} code, or {@code null}
	 * if no match can be found. Can optionally also search {@code code} for the
	 * English language name.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @param containsName if {@code true}, a search for the English language
	 *            name will also be performed.
	 * @return The {@code ISO 639-2} (three letter) code or {@code null}.
	 */
	public static String getISO639_2Code(String code, boolean containsName) {
		Iso639Entry entry = get(code, containsName);
		return entry == null ? null : entry.iso639_2;
	}

	/**
	 * Returns the code, except when the alias "{@code loc}" is used. In that
	 * case the ISO 639 code of the preferred language in the UMS settings is
	 * returned.
	 *
	 * @param isoCode an {@code ISO 639} code, or "{@code loc}".
	 * @return The code.
	 */
	private static String normalize(String isoCode) {
		if (LOCAL_ALIAS.equals(isoCode)) {
			String tag = PMS.getLocale().toLanguageTag();
			int idx = tag.indexOf('-');
			return idx > 0 ? tag.substring(0, idx) : tag;
		}
		return isoCode;
	}

	/**
	 * Verifies that a {@code ISO 639} English language name is matching an
	 * {@code ISO 639} code. Returns {@code true} if a match can be made,
	 * {@code false} otherwise.
	 *
	 * @param language the full language name.
	 * @param code the {@code ISO 639} code. If "{@code loc}" is specified, the
	 *            ISO code of the preferred language is used instead.
	 * @return {@code true} if they match, {@code false} otherwise.
	 */
	public static boolean isCodeMatching(String language, String code) {
		if (isBlank(language) || isBlank(code)) {
			return false;
		}

		code = normalize(code.trim().toLowerCase(Locale.ROOT));
		Iso639Entry entry = links.get(language.trim().toLowerCase(Locale.ROOT));
		if (entry == null) {
			return false;
		}

		return entry.matches(code);
	}

	/**
	 * Verifies that two {@code ISO 639} codes match the same language. Returns
	 * {@code true} if a match can be made, {@code false} otherwise. If the
	 * alias "{@code loc}" is used as a code, it will be replaced by the
	 * {@code ISO 639} code of the preferred language from the UMS settings.
	 *
	 * @param code1 The first {@code ISO 639} code.
	 * @param code2 The second {@code ISO 639} code.
	 * @return {@code true} if both match, {@code false} otherwise.
	 */
	public static boolean isCodesMatching(String code1, String code2) {
		if (isBlank(code1) || isBlank(code2)) {
			return false;
		}

		code1 = normalize(code1.trim().toLowerCase(Locale.ROOT));
		code2 = normalize(code2.trim().toLowerCase(Locale.ROOT));

		for (Iso639Entry entry : links.values()) {
			if (entry.matches(code1) && entry.matches(code2)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Registers a language with one or more names, a {@code ISO 639-1} code and
	 * a {@code ISO 639-2} code.
	 *
	 * @param name the semicolon-separated {@link String} of English language
	 *            names.
	 * @param iso639_1 the {@code ISO 639-1} (two letter) code.
	 * @param iso639_2 the ISO 639-2 (three letter) code.
	 */
	private static void registerLanguage(String name, String iso639_1, String iso639_2) {
		registerLanguage(name, iso639_1, iso639_2, null);
	}

	/**
	 * Registers a language with one or more names, a {@code ISO 639-1} code and
	 * two {@code ISO 639-2} codes.
	 *
	 * @param name the semicolon-separated {@link String} of English language
	 *            names.
	 * @param iso639_1 the {@code ISO 639-1} (two letter) code.
	 * @param iso639_2 the bibliographic ISO 639-2 (three letter) code.
	 * @param altIso639_2 the terminology ISO 639-2 (three letter) code.
	 */
	private static void registerLanguage(String name, String iso639_1, String iso639_2, String altIso639_2) {
		String[] names = name.split("\\s*;\\s*");
		if (names.length == 0) {
			throw new IllegalArgumentException("name cannot be blank");
		}
		String[] keys = new String[names.length];
		for (int i = 0; i < names.length; i++) {
			keys[i] = names[i].replaceAll("\\s*\\([^\\)]*\\)\\s*", "").toLowerCase(Locale.ROOT);
		}
		Iso639Entry entry = new Iso639Entry(names, iso639_1, iso639_2, altIso639_2);
		for (int i = 0; i < keys.length; i++) {
			links.put(keys[i], entry);
		}
	}

	/**
	 * Initializes the list of {@code ISO 639} codes.
	 */
	private static ArrayList<String> initCodes() {
		ArrayList<String> result = new ArrayList<>();
		for (Iso639Entry entry : links.values()) {
			if (isNotBlank(entry.iso639_1)) {
				result.add(entry.iso639_1);
			}
			result.add(entry.iso639_2);
			if (isNotBlank(entry.altIso639_2)) {
				result.add(entry.altIso639_2);
			}
		}
		return result;
	}

	/**
	 * Initializes the {@link HashMap} containing languages and their codes.
	 */
	private static void initLinks() {
		registerLanguage("Abkhazian", "ab", "abk");
		registerLanguage("Achinese", null, "ace");
		registerLanguage("Acoli", null, "ach");
		registerLanguage("Adangme", null, "ada");
		registerLanguage("Adyghe;Adygei", null, "ady");
		registerLanguage("Afar", "aa", "aar");
		registerLanguage("Afrihili", null, "afh");
		registerLanguage("Afrikaans", "af", "afr");
		registerLanguage("Afro-Asiatic languages", null, "afa");
		registerLanguage("Ainu;Ainu (Japan)", null, "ain");
		registerLanguage("Akan", "ak", "aka");
		registerLanguage("Akkadian", null, "akk");
		registerLanguage("Albanian", "sq", "alb", "sqi");
		registerLanguage("Alemannic;Alsatian;Swiss German", null, "gsw");
		registerLanguage("Aleut", null, "ale");
		registerLanguage("Algonquian;Algonquian languages", null, "alg");
		registerLanguage("Altaic;Altaic languages", null, "tut");
		registerLanguage("Amharic", "am", "amh");
		registerLanguage("Ancient Greek (to 1453)", null, "grc");
		registerLanguage("Angika", null, "anp");
		registerLanguage("Apache;Apache languages", null, "apa");
		registerLanguage("Arabic", "ar", "ara");
		registerLanguage("Aragonese", "an", "arg");
		registerLanguage("Arapaho", null, "arp");
		registerLanguage("Arawak", null, "arw");
		registerLanguage("Armenian", "hy", "arm", "hye");
		registerLanguage("Aromanian;Arumanian;Macedo-Romanian", null, "rup");
		registerLanguage("Artificial languages", null, "art");
		registerLanguage("Assamese", "as", "asm");
		registerLanguage("Asturian;Asturleonese;Bable;Leonese", null, "ast");
		registerLanguage("Athapascan;Athapascan languages", null, "ath");
		registerLanguage("Australian;Australian languages", null, "aus");
		registerLanguage("Austronesian;Austronesian languages", null, "map");
		registerLanguage("Avaric", "av", "ava");
		registerLanguage("Avestan", "ae", "ave");
		registerLanguage("Awadhi", null, "awa");
		registerLanguage("Aymara", "ay", "aym");
		registerLanguage("Azerbaijani", "az", "aze");
		registerLanguage("Balinese", null, "ban");
		registerLanguage("Baltic;Baltic languages", null, "bat");
		registerLanguage("Baluchi", null, "bal");
		registerLanguage("Bambara", "bm", "bam");
		registerLanguage("Bamileke;Bamileke languages", null, "bai");
		registerLanguage("Banda;Banda languages", null, "bad");
		registerLanguage("Bantu;Bantu languages", null, "bnt");
		registerLanguage("Basa (Cameroon)", null, "bas");
		registerLanguage("Bashkir", "ba", "bak");
		registerLanguage("Basque", "eu", "baq", "eus");
		registerLanguage("Batak;Batak languages", null, "btk");
		registerLanguage("Bedawiyet;Beja", null, "bej");
		registerLanguage("Belarusian", "be", "bel");
		registerLanguage("Bemba (Zambia)", null, "bem");
		registerLanguage("Bengali", "bn", "ben");
		registerLanguage("Berber;Berber languages", null, "ber");
		registerLanguage("Bhojpuri", null, "bho");
		registerLanguage("Bihari;Bihari languages", "bh", "bih");
		registerLanguage("Bikol", null, "bik");
		registerLanguage("Bilin;Blin", null, "byn");
		registerLanguage("Bini;Edo", null, "bin");
		registerLanguage("Bislama", "bi", "bis");
		registerLanguage("Bliss;Blissymbolics;Blissymbols", null, "zbl");
		registerLanguage("Bosnian", "bs", "bos");
		registerLanguage("Braj", null, "bra");
		registerLanguage("Breton", "br", "bre");
		registerLanguage("Buginese", null, "bug");
		registerLanguage("Bulgarian", "bg", "bul");
		registerLanguage("Buriat", null, "bua");
		registerLanguage("Burmese", "my", "bur", "mya");
		registerLanguage("Caddo", null, "cad");
		registerLanguage("Castilian;Spanish", "es", "spa");
		registerLanguage("Catalan;Valencian", "ca", "cat");
		registerLanguage("Caucasian;Caucasian languages", null, "cau");
		registerLanguage("Cebuano", null, "ceb");
		registerLanguage("Celtic;Celtic languages", null, "cel");
		registerLanguage("Central American Indian languages", null, "cai");
		registerLanguage("Central Khmer;Khmer", "km", "khm");
		registerLanguage("Chagatai", null, "chg");
		registerLanguage("Chamic;Chamic languages", null, "cmc");
		registerLanguage("Chamorro", "ch", "cha");
		registerLanguage("Chechen", "ce", "che");
		registerLanguage("Cherokee", null, "chr");
		registerLanguage("Chewa;Chichewa;Nyanja", "ny", "nya");
		registerLanguage("Cheyenne", null, "chy");
		registerLanguage("Chibcha", null, "chb");
		registerLanguage("Chinese", "zh", "chi", "zho");
		registerLanguage("Chinook jargon", null, "chn");
		registerLanguage("Chipewyan;Dene Suline", null, "chp");
		registerLanguage("Choctaw", null, "cho");
		registerLanguage("Chuang;Zhuang", "za", "zha");
		registerLanguage("Church Slavic;Church Slavonic;Old Bulgarian;Old Church Slavonic;Old Slavonic", "cu", "chu");
		registerLanguage("Chuukese", null, "chk");
		registerLanguage("Chuvash", "cv", "chv");
		registerLanguage("Classical Nepal Bhasa;Classical Newari;Old Newari", null, "nwc");
		registerLanguage("Classical Syriac", null, "syc");
		registerLanguage("Cook Islands Maori;Rarotongan", null, "rar");
		registerLanguage("Coptic", null, "cop");
		registerLanguage("Cornish", "kw", "cor");
		registerLanguage("Corsican", "co", "cos");
		registerLanguage("Cree", "cr", "cre");
		registerLanguage("Creek", null, "mus");
		registerLanguage("Creoles and pidgins", null, "crp");
		registerLanguage("Creoles and pidgins, English based", null, "cpe");
		registerLanguage("Creoles and pidgins, French-based", null, "cpf");
		registerLanguage("Creoles and pidgins, Portuguese-based", null, "cpp");
		registerLanguage("Crimean Tatar;Crimean Turkish", null, "crh");
		registerLanguage("Croatian", "hr", "hrv");
		registerLanguage("Cushitic;Cushitic languages", null, "cus");
		registerLanguage("Czech", "cs", "cze", "ces");
		registerLanguage("Dakota", null, "dak");
		registerLanguage("Danish", "da", "dan");
		registerLanguage("Dargwa", null, "dar");
		registerLanguage("Delaware", null, "del");
		registerLanguage("Dhivehi;Divehi;Maldivian", "dv", "div");
		registerLanguage("Dholuo;Luo (Kenya and Tanzania)", null, "luo");
		registerLanguage("Dimili;Dimli (macrolanguage);Kirdki;Kirmanjki (macrolanguage);Zaza;Zazaki", null, "zza");
		registerLanguage("Dinka", null, "din");
		registerLanguage("Dogri (macrolanguage)", null, "doi");
		registerLanguage("Dogrib", null, "dgr");
		registerLanguage("Dravidian;Dravidian languages", null, "dra");
		registerLanguage("Duala", null, "dua");
		registerLanguage("Dutch;Flemish", "nl", "dut", "nld");
		registerLanguage("Dyula", null, "dyu");
		registerLanguage("Dzongkha", "dz", "dzo");
		registerLanguage("Eastern Frisian", null, "frs");
		registerLanguage("Efik", null, "efi");
		registerLanguage("Egyptian (Ancient)", null, "egy");
		registerLanguage("Ekajuk", null, "eka");
		registerLanguage("Elamite", null, "elx");
		registerLanguage("English", "en", "eng");
		registerLanguage("Erzya", null, "myv");
		registerLanguage("Esperanto", "eo", "epo");
		registerLanguage("Estonian", "et", "est");
		registerLanguage("Ewe", "ee", "ewe");
		registerLanguage("Ewondo", null, "ewo");
		registerLanguage("Fang (Equatorial Guinea)", null, "fan");
		registerLanguage("Fanti", null, "fat");
		registerLanguage("Faroese", "fo", "fao");
		registerLanguage("Fijian", "fj", "fij");
		registerLanguage("Filipino;Pilipino", null, "fil");
		registerLanguage("Finnish", "fi", "fin");
		registerLanguage("Finno-Ugrian languages", null, "fiu");
		registerLanguage("Fon", null, "fon");
		registerLanguage("French", "fr", "fre", "fra");
		registerLanguage("Friulian", null, "fur");
		registerLanguage("Fulah", "ff", "ful");
		registerLanguage("Ga", null, "gaa");
		registerLanguage("Gaelic;Scottish Gaelic", "gd", "gla");
		registerLanguage("Galibi Carib", null, "car");
		registerLanguage("Galician", "gl", "glg");
		registerLanguage("Ganda", "lg", "lug");
		registerLanguage("Gayo", null, "gay");
		registerLanguage("Gbaya (Central African Republic)", null, "gba");
		registerLanguage("Geez", null, "gez");
		registerLanguage("Georgian", "ka", "geo", "kat");
		registerLanguage("German", "de", "ger", "deu");
		registerLanguage("Germanic;Germanic languages", null, "gem");
		registerLanguage("Gikuyu;Kikuyu", "ki", "kik");
		registerLanguage("Gilbertese", null, "gil");
		registerLanguage("Gondi", null, "gon");
		registerLanguage("Gorontalo", null, "gor");
		registerLanguage("Gothic", null, "got");
		registerLanguage("Grebo", null, "grb");
		registerLanguage("Greek;Modern Greek (1453-)", "el", "gre", "ell");
		registerLanguage("Greenlandic;Kalaallisut", "kl", "kal");
		registerLanguage("Guarani", "gn", "grn");
		registerLanguage("Gujarati", "gu", "guj");
		registerLanguage("Gwichʼin", null, "gwi");
		registerLanguage("Haida", null, "hai");
		registerLanguage("Haitian;Haitian Creole", "ht", "hat");
		registerLanguage("Hausa", "ha", "hau");
		registerLanguage("Hawaiian", null, "haw");
		registerLanguage("Hebrew", "he", "heb");
		registerLanguage("Herero", "hz", "her");
		registerLanguage("Hiligaynon", null, "hil");
		registerLanguage("Himachali languages;Western Pahari languages", null, "him");
		registerLanguage("Hindi", "hi", "hin");
		registerLanguage("Hiri Motu", "ho", "hmo");
		registerLanguage("Hittite", null, "hit");
		registerLanguage("Hmong;Mong", null, "hmn");
		registerLanguage("Hungarian", "hu", "hun");
		registerLanguage("Hupa", null, "hup");
		registerLanguage("Iban", null, "iba");
		registerLanguage("Icelandic", "is", "ice", "isl");
		registerLanguage("Ido", "io", "ido");
		registerLanguage("Igbo", "ig", "ibo");
		registerLanguage("Ijo;Ijo languages", null, "ijo");
		registerLanguage("Iloko", null, "ilo");
		registerLanguage("Imperial Aramaic (700-300 BCE);Official Aramaic (700-300 BCE)", null, "arc");
		registerLanguage("Inari Sami", null, "smn");
		registerLanguage("Indic;Indic languages", null, "inc");
		registerLanguage("Indo-European languages", null, "ine");
		registerLanguage("Indonesian", "id", "ind");
		registerLanguage("Ingush", null, "inh");
		registerLanguage("Interlingua (International Auxiliary Language Association)", "ia", "ina");
		registerLanguage("Interlingue;Occidental", "ie", "ile");
		registerLanguage("Inuktitut", "iu", "iku");
		registerLanguage("Inupiaq", "ik", "ipk");
		registerLanguage("Iranian;Iranian languages", null, "ira");
		registerLanguage("Irish", "ga", "gle");
		registerLanguage("Iroquoian;Iroquoian languages", null, "iro");
		registerLanguage("Italian", "it", "ita");
		registerLanguage("Japanese", "ja", "jpn");
		registerLanguage("Javanese", "jv", "jav");
		registerLanguage("Jingpho;Kachin", null, "kac");
		registerLanguage("Judeo-Arabic", null, "jrb");
		registerLanguage("Judeo-Persian", null, "jpr");
		registerLanguage("Kabardian", null, "kbd");
		registerLanguage("Kabyle", null, "kab");
		registerLanguage("Kalmyk;Oirat", null, "xal");
		registerLanguage("Kamba (Kenya)", null, "kam");
		registerLanguage("Kannada", "kn", "kan");
		registerLanguage("Kanuri", "kr", "kau");
		registerLanguage("Kapampangan;Pampanga", null, "pam");
		registerLanguage("Karachay-Balkar", null, "krc");
		registerLanguage("Kara-Kalpak", null, "kaa");
		registerLanguage("Karelian", null, "krl");
		registerLanguage("Karen;Karen languages", null, "kar");
		registerLanguage("Kashmiri", "ks", "kas");
		registerLanguage("Kashubian", null, "csb");
		registerLanguage("Kawi", null, "kaw");
		registerLanguage("Kazakh", "kk", "kaz");
		registerLanguage("Khasi", null, "kha");
		registerLanguage("Khoisan;Khoisan languages", null, "khi");
		registerLanguage("Khotanese;Sakan", null, "kho");
		registerLanguage("Kimbundu", null, "kmb");
		registerLanguage("Kinyarwanda", "rw", "kin");
		registerLanguage("Kirghiz;Kyrgyz", "ky", "kir");
		registerLanguage("Klingon;tlhIngan Hol", null, "tlh");
		registerLanguage("Komi", "kv", "kom");
		registerLanguage("Kongo", "kg", "kon");
		registerLanguage("Konkani (macrolanguage)", null, "kok");
		registerLanguage("Korean", "ko", "kor");
		registerLanguage("Kosraean", null, "kos");
		registerLanguage("Kpelle", null, "kpe");
		registerLanguage("Kru;Kru languages", null, "kro");
		registerLanguage("Kuanyama;Kwanyama", "kj", "kua");
		registerLanguage("Kumyk", null, "kum");
		registerLanguage("Kurdish", "ku", "kur");
		registerLanguage("Kurukh", null, "kru");
		registerLanguage("Kutenai", null, "kut");
		registerLanguage("Ladino", null, "lad");
		registerLanguage("Lahnda", null, "lah");
		registerLanguage("Lamba", null, "lam");
		registerLanguage("Land Dayak languages", null, "day");
		registerLanguage("Lao", "lo", "lao");
		registerLanguage("Latin", "la", "lat");
		registerLanguage("Latvian", "lv", "lav");
		registerLanguage("Letzeburgesch;Luxembourgish", "lb", "ltz");
		registerLanguage("Lezghian", null, "lez");
		registerLanguage("Limburgan;Limburger;Limburgish", "li", "lim");
		registerLanguage("Lingala", "ln", "lin");
		registerLanguage("Lithuanian", "lt", "lit");
		registerLanguage("Lojban", null, "jbo");
		registerLanguage("Low German;Low Saxon", null, "nds");
		registerLanguage("Lower Sorbian", null, "dsb");
		registerLanguage("Lozi", null, "loz");
		registerLanguage("Luba-Katanga", "lu", "lub");
		registerLanguage("Luba-Lulua", null, "lua");
		registerLanguage("Luiseno", null, "lui");
		registerLanguage("Lule Sami", null, "smj");
		registerLanguage("Lunda", null, "lun");
		registerLanguage("Lushai", null, "lus");
		registerLanguage("Macedonian", "mk", "mac", "mkd");
		registerLanguage("Madurese", null, "mad");
		registerLanguage("Magahi", null, "mag");
		registerLanguage("Maithili", null, "mai");
		registerLanguage("Makasar", null, "mak");
		registerLanguage("Malagasy", "mg", "mlg");
		registerLanguage("Malay (macrolanguage)", "ms", "may", "msa");
		registerLanguage("Malayalam", "ml", "mal");
		registerLanguage("Maltese", "mt", "mlt");
		registerLanguage("Manchu", null, "mnc");
		registerLanguage("Mandar", null, "mdr");
		registerLanguage("Manding;Mandingo", null, "man");
		registerLanguage("Manipuri", null, "mni");
		registerLanguage("Manobo;Manobo languages", null, "mno");
		registerLanguage("Manx", "gv", "glv");
		registerLanguage("Maori", "mi", "mao", "mri");
		registerLanguage("Mapuche;Mapudungun", null, "arn");
		registerLanguage("Marathi", "mr", "mar");
		registerLanguage("Mari (Russia)", null, "chm");
		registerLanguage("Marshallese", "mh", "mah");
		registerLanguage("Marwari", null, "mwr");
		registerLanguage("Masai", null, "mas");
		registerLanguage("Mayan;Mayan languages", null, "myn");
		registerLanguage("Mende (Sierra Leone)", null, "men");
		registerLanguage("Micmac;Mi'kmaq", null, "mic");
		registerLanguage("Middle Dutch (ca. 1050-1350)", null, "dum");
		registerLanguage("Middle English (1100-1500)", null, "enm");
		registerLanguage("Middle French (ca. 1400-1600)", null, "frm");
		registerLanguage("Middle High German (ca. 1050-1500)", null, "gmh");
		registerLanguage("Middle Irish (900-1200)", null, "mga");
		registerLanguage("Minangkabau", null, "min");
		registerLanguage("Mirandese", null, "mwl");
		registerLanguage("Mohawk", null, "moh");
		registerLanguage("Moksha", null, "mdf");
		registerLanguage("Moldavian;Moldovan;Romanian", "ro", "rum", "ron");
		registerLanguage("Mongo", null, "lol");
		registerLanguage("Mongolian", "mn", "mon");
		registerLanguage("Mon-Khmer languages", null, "mkh");
		registerLanguage("Montenegrin", null, "cnr");
		registerLanguage("Mossi", null, "mos");
		registerLanguage("Multiple languages", null, "mul");
		registerLanguage("Munda;Munda languages", null, "mun");
		registerLanguage("Nahuatl;Nahuatl languages", null, "nah");
		registerLanguage("Nauru", "na", "nau");
		registerLanguage("Navaho;Navajo", "nv", "nav");
		registerLanguage("Ndonga", "ng", "ndo");
		registerLanguage("Neapolitan", null, "nap");
		registerLanguage("Nepal Bhasa;Newari", null, "new");
		registerLanguage("Nepali (macrolanguage)", "ne", "nep");
		registerLanguage("Nias", null, "nia");
		registerLanguage("Niger-Kordofanian languages", null, "nic");
		registerLanguage("Nilo-Saharan languages", null, "ssa");
		registerLanguage("Niuean", null, "niu");
		registerLanguage("N'Ko", null, "nqo");
		registerLanguage("No linguistic content;Not applicable", null, "zxx");
		registerLanguage("Nogai", null, "nog");
		registerLanguage("North American Indian languages", null, "nai");
		registerLanguage("North Ndebele", "nd", "nde");
		registerLanguage("Northern Frisian", null, "frr");
		registerLanguage("Northern Sami", "se", "sme");
		registerLanguage("Northern Sotho;Pedi;Sepedi", null, "nso");
		registerLanguage("Norwegian Bokmål", "nb", "nob");
		registerLanguage("Norwegian Nynorsk", "nn", "nno");
		registerLanguage("Norwegian", "no", "nor");
		registerLanguage("Nubian;Nubian languages", null, "nub");
		registerLanguage("Nuosu;Sichuan Yi", "ii", "iii");
		registerLanguage("Nyamwezi", null, "nym");
		registerLanguage("Nyankole", null, "nyn");
		registerLanguage("Nyoro", null, "nyo");
		registerLanguage("Nzima", null, "nzi");
		registerLanguage("Occitan (post 1500)", "oc", "oci");
		registerLanguage("Ojibwa", "oj", "oji");
		registerLanguage("Old English (ca. 450-1100)", null, "ang");
		registerLanguage("Old French (842-ca. 1400)", null, "fro");
		registerLanguage("Old High German (ca. 750-1050)", null, "goh");
		registerLanguage("Old Irish (to 900)", null, "sga");
		registerLanguage("Old Norse", null, "non");
		registerLanguage("Old Occitan (to 1500);Old Provençal (to 1500)", null, "pro");
		registerLanguage("Old Persian (ca. 600-400 B.C.)", null, "peo");
		registerLanguage("Oriya (macrolanguage)", "or", "ori");
		registerLanguage("Oromo", "om", "orm");
		registerLanguage("Osage", null, "osa");
		registerLanguage("Ossetian;Ossetic", "os", "oss");
		registerLanguage("Otomian;Otomian languages", null, "oto");
		registerLanguage("Ottoman Turkish (1500-1928)", null, "ota");
		registerLanguage("Pahlavi", null, "pal");
		registerLanguage("Palauan", null, "pau");
		registerLanguage("Pali", "pi", "pli");
		registerLanguage("Pangasinan", null, "pag");
		registerLanguage("Panjabi;Punjabi", "pa", "pan");
		registerLanguage("Papiamento", null, "pap");
		registerLanguage("Papuan;Papuan languages", null, "paa");
		registerLanguage("Pashto;Pushto", "ps", "pus");
		registerLanguage("Persian", "fa", "per", "fas");
		registerLanguage("Philippine;Philippine languages", null, "phi");
		registerLanguage("Phoenician", null, "phn");
		registerLanguage("Pohnpeian", null, "pon");
		registerLanguage("Polish", "pl", "pol");
		registerLanguage("Portuguese", "pt", "por");
		registerLanguage("Prakrit;Prakrit languages", null, "pra");
		registerLanguage("Quechua", "qu", "que");
		registerLanguage("Rajasthani", null, "raj");
		registerLanguage("Rapanui", null, "rap");
		registerLanguage("Reserved for local use", null, "qaa-qtz");
		registerLanguage("Romance;Romance languages", null, "roa");
		registerLanguage("Romansh", "rm", "roh");
		registerLanguage("Romany", null, "rom");
		registerLanguage("Rundi", "rn", "run");
		registerLanguage("Russian", "ru", "rus");
		registerLanguage("Salishan;Salishan languages", null, "sal");
		registerLanguage("Samaritan Aramaic", null, "sam");
		registerLanguage("Sami;Sami languages", null, "smi");
		registerLanguage("Samoan", "sm", "smo");
		registerLanguage("Sandawe", null, "sad");
		registerLanguage("Sango", "sg", "sag");
		registerLanguage("Sanskrit", "sa", "san");
		registerLanguage("Santali", null, "sat");
		registerLanguage("Sardinian", "sc", "srd");
		registerLanguage("Sasak", null, "sas");
		registerLanguage("Scots", null, "sco");
		registerLanguage("Selkup", null, "sel");
		registerLanguage("Semitic languages", null, "sem");
		registerLanguage("Serbian", "sr", "srp");
		registerLanguage("Serer", null, "srr");
		registerLanguage("Shan", null, "shn");
		registerLanguage("Shona", "sn", "sna");
		registerLanguage("Sicilian", null, "scn");
		registerLanguage("Sidamo", null, "sid");
		registerLanguage("Sign;Sign Languages", null, "sgn");
		registerLanguage("Siksika", null, "bla");
		registerLanguage("Sindhi", "sd", "snd");
		registerLanguage("Sinhala;Sinhalese", "si", "sin");
		registerLanguage("Sino-Tibetan languages", null, "sit");
		registerLanguage("Siouan;Siouan languages", null, "sio");
		registerLanguage("Skolt Sami", null, "sms");
		registerLanguage("Slave (Athapascan)", null, "den");
		registerLanguage("Slavic languages", null, "sla");
		registerLanguage("Slovak", "sk", "slo", "slk");
		registerLanguage("Slovenian", "sl", "slv");
		registerLanguage("Sogdian", null, "sog");
		registerLanguage("Somali", "so", "som");
		registerLanguage("Songhai languages", null, "son");
		registerLanguage("Soninke", null, "snk");
		registerLanguage("Sorbian languages", null, "wen");
		registerLanguage("South American Indian languages", null, "sai");
		registerLanguage("South Ndebele", "nr", "nbl");
		registerLanguage("Southern Altai", null, "alt");
		registerLanguage("Southern Sami", null, "sma");
		registerLanguage("Southern Sotho", "st", "sot");
		registerLanguage("Sranan Tongo", null, "srn");
		registerLanguage("Standard Moroccan Tamazight", null, "zgh");
		registerLanguage("Sukuma", null, "suk");
		registerLanguage("Sumerian", null, "sux");
		registerLanguage("Sundanese", "su", "sun");
		registerLanguage("Susu", null, "sus");
		registerLanguage("Swahili (macrolanguage)", "sw", "swa");
		registerLanguage("Swati", "ss", "ssw");
		registerLanguage("Swedish", "sv", "swe");
		registerLanguage("Syriac", null, "syr");
		registerLanguage("Tagalog", "tl", "tgl");
		registerLanguage("Tahitian", "ty", "tah");
		registerLanguage("Tai;Tai languages", null, "tai");
		registerLanguage("Tajik", "tg", "tgk");
		registerLanguage("Tamashek", null, "tmh");
		registerLanguage("Tamil", "ta", "tam");
		registerLanguage("Tatar", "tt", "tat");
		registerLanguage("Telugu", "te", "tel");
		registerLanguage("Tereno", null, "ter");
		registerLanguage("Tetum", null, "tet");
		registerLanguage("Thai", "th", "tha");
		registerLanguage("Tibetan", "bo", "tib", "bod");
		registerLanguage("Tigre", null, "tig");
		registerLanguage("Tigrinya", "ti", "tir");
		registerLanguage("Timne", null, "tem");
		registerLanguage("Tiv", null, "tiv");
		registerLanguage("Tlingit", null, "tli");
		registerLanguage("Tok Pisin", null, "tpi");
		registerLanguage("Tokelau", null, "tkl");
		registerLanguage("Tonga (Nyasa)", null, "tog");
		registerLanguage("Tonga (Tonga Islands)", "to", "ton");
		registerLanguage("Tsimshian", null, "tsi");
		registerLanguage("Tsonga", "ts", "tso");
		registerLanguage("Tswana", "tn", "tsn");
		registerLanguage("Tumbuka", null, "tum");
		registerLanguage("Tupi languages", null, "tup");
		registerLanguage("Turkish", "tr", "tur");
		registerLanguage("Turkmen", "tk", "tuk");
		registerLanguage("Tuvalu", null, "tvl");
		registerLanguage("Tuvinian", null, "tyv");
		registerLanguage("Twi", "tw", "twi");
		registerLanguage("Udmurt", null, "udm");
		registerLanguage("Ugaritic", null, "uga");
		registerLanguage("Uighur;Uyghur", "ug", "uig");
		registerLanguage("Ukrainian", "uk", "ukr");
		registerLanguage("Umbundu", null, "umb");
		registerLanguage("Uncoded languages", null, "mis");
		registerLanguage("Undetermined", null, DLNAMediaLang.UND);
		registerLanguage("Upper Sorbian", null, "hsb");
		registerLanguage("Urdu", "ur", "urd");
		registerLanguage("Uzbek", "uz", "uzb");
		registerLanguage("Vai", null, "vai");
		registerLanguage("Venda", "ve", "ven");
		registerLanguage("Vietnamese", "vi", "vie");
		registerLanguage("Volapük", "vo", "vol");
		registerLanguage("Votic", null, "vot");
		registerLanguage("Wakashan languages", null, "wak");
		registerLanguage("Walloon", "wa", "wln");
		registerLanguage("Waray (Philippines)", null, "war");
		registerLanguage("Washo", null, "was");
		registerLanguage("Welsh", "cy", "wel", "cym");
		registerLanguage("Western Frisian", "fy", "fry");
		registerLanguage("Wolaitta;Wolaytta", null, "wal");
		registerLanguage("Wolof", "wo", "wol");
		registerLanguage("Xhosa", "xh", "xho");
		registerLanguage("Yakut", null, "sah");
		registerLanguage("Yao", null, "yao");
		registerLanguage("Yapese", null, "yap");
		registerLanguage("Yiddish", "yi", "yid");
		registerLanguage("Yoruba", "yo", "yor");
		registerLanguage("Yupik;Yupik languages", null, "ypk");
		registerLanguage("Zande;Zande languages", null, "znd");
		registerLanguage("Zapotec", null, "zap");
		registerLanguage("Zenaga", null, "zen");
		registerLanguage("Zulu", "zu", "zul");
		registerLanguage("Zuni", null, "zun");
	}

	private static HashMap<String, String> buildMisspellings() {
		HashMap<String, String> misspellings = new HashMap<String, String>();
		misspellings.put("ameircan", "american");
		misspellings.put("artifical", "artificial");
		misspellings.put("brasillian", "brazilian");
		misspellings.put("carrib", "carib");
		misspellings.put("centeral", "central");
		misspellings.put("chineese", "chinese");
		misspellings.put("curch", "church");
		misspellings.put("dravadian", "dravidian");
		misspellings.put("enlish", "english");
		misspellings.put("euorpean", "european");
		misspellings.put("farsi", "persian");
		misspellings.put("hawaian", "hawaiian");
		misspellings.put("hebrwe", "hebrew");
		misspellings.put("japaneese", "japanese");
		misspellings.put("javaneese", "javanese");
		misspellings.put("laguage", "language");
		misspellings.put("madureese", "madurese");
		misspellings.put("malteese", "maltese");
		misspellings.put("maltesian", "maltese");
		misspellings.put("miscelaneous", "miscellaneous");
		misspellings.put("miscellanious", "miscellaneous");
		misspellings.put("miscellanous", "miscellaneous");
		misspellings.put("northen", "northern");
		misspellings.put("norweigan", "norwegian");
		misspellings.put("ottaman", "ottoman");
		misspellings.put("philipine", "philippine");
		misspellings.put("phonecian", "phoenician");
		misspellings.put("portugese", "portuguese");
		misspellings.put("rusian", "russian");
		misspellings.put("sinhaleese", "sinhalese");
		misspellings.put("sourth", "south");
		misspellings.put("spainish", "spanish");
		misspellings.put("sweedish", "swedish");
		misspellings.put("ukranian", "ukrainian");
		misspellings.put("vietnameese", "vietnamese");

		return misspellings;
	}

	/**
	 * A representation of one {@code ISO 639} language.
	 */
	public static class Iso639Entry {
		private final String[] names;
		private final String iso639_1;
		private final String iso639_2;
		private final String altIso639_2;

		/**
		 * Creates a new instance.
		 *
		 * @param names the array of English language names.
		 * @param iso639_1 the ISO 639-1 (two letter) code.
		 * @param iso639_2 the bibliographic ISO 639-2 (three letter) code.
		 * @param altIso639_2 the terminology ISO 639-2 (three letter) code.
		 */
		public Iso639Entry(String[] names, String iso639_1, String iso639_2, String altIso639_2) {
			if (names.length == 0 || isBlank(names[0])) {
				throw new IllegalArgumentException("names cannot be empty");
			}
			this.names = new String[names.length];
			System.arraycopy(names, 0, this.names, 0, names.length);
			this.iso639_1 = iso639_1;
			this.iso639_2 = iso639_2;
			this.altIso639_2 = altIso639_2;
		}

		/**
		 * @return The first registered English language name.
		 */
		public String getFirstName() {
			return names[0];
		}

		/**
		 * @return The English language names.
		 */
		public String[] getNames() {
			String[] result = new String[names.length];
			System.arraycopy(names, 0, result, 0, names.length);
			return result;
		}

		/**
		 * @return The ISO 639-1 (two letter) code.
		 */
		public String getIso639_1() {
			return iso639_1;
		}

		/**
		 * @return The bibliographic ISO 639-2 (three letter) code.
		 */
		public String getIso639_2() {
			return iso639_2;
		}

		/**
		 * Verifies if the specified lower-case {@code ISO 639} code matches any
		 * of the {@code ISO 639} codes for this instance.
		 *
		 * @param code the lower-case ISO 639 (two or three letter) code.
		 * @return {@code true} if a match is found, {@code false} otherwise.
		 */
		public boolean matches(String code) {
			if (code == null) {
				return false;
			}
			return code.equals(iso639_1) || code.equals(iso639_2) || code.equals(altIso639_2);
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append(" [");
			if (names.length > 1) {
				sb.append("Names=").append(StringUtil.createReadableCombinedString(names, true));
			} else {
				sb.append("Name=").append("\"").append(names[0]).append("\"");
			}
			if (isNotBlank(iso639_1)) {
				sb.append(", 639-1=").append(iso639_1);
			}
			sb.append(", 639-2=").append(iso639_2);
			if (isNotBlank(altIso639_2)) {
				sb.append(", alternative 639-2=").append(altIso639_2);
			}
			sb.append("]");
			return sb.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((altIso639_2 == null) ? 0 : altIso639_2.hashCode());
			result = prime * result + ((iso639_1 == null) ? 0 : iso639_1.hashCode());
			result = prime * result + ((iso639_2 == null) ? 0 : iso639_2.hashCode());
			result = prime * result + Arrays.hashCode(names);
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
			if (!(obj instanceof Iso639Entry)) {
				return false;
			}
			Iso639Entry other = (Iso639Entry) obj;
			if (altIso639_2 == null) {
				if (other.altIso639_2 != null) {
					return false;
				}
			} else if (!altIso639_2.equals(other.altIso639_2)) {
				return false;
			}
			if (iso639_1 == null) {
				if (other.iso639_1 != null) {
					return false;
				}
			} else if (!iso639_1.equals(other.iso639_1)) {
				return false;
			}
			if (iso639_2 == null) {
				if (other.iso639_2 != null) {
					return false;
				}
			} else if (!iso639_2.equals(other.iso639_2)) {
				return false;
			}
			if (!Arrays.equals(names, other.names)) {
				return false;
			}
			return true;
		}
	}
}
