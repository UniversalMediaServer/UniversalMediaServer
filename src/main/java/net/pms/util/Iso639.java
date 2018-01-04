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
 */
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

		if (containsName) {
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
	 * Gets the English {@code ISO 639} language name for an English
	 * {@code ISO 639} language name or an {@code ISO 639} code, or {@code null}
	 * if no match can be found.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @return The {@code ISO 639} English language name or {@code null}.
	 */
	public static String getName(String code) {
		return getName(code, false);
	}

	/**
	 * Gets the English {@code ISO 639} language name for an English
	 * {@code ISO 639} language name or an {@code ISO 639} code, or {@code null}
	 * if no match can be found. Can optionally also search {@code code} for the
	 * English language name.
	 *
	 * @param code the {@code ISO 639} two or three letter code or English
	 *            language name to find.
	 * @param containsName if {@code true}, a search for the English language
	 *            name will also be performed.
	 * @return The {@code ISO 639} English language name or {@code null}.
	 */
	public static String getName(String code, boolean containsName) {
		Iso639Entry entry = get(code, containsName);
		return entry == null ? null : entry.name;
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
	 * Registers a language with a name, a {@code ISO 639-1} code and a
	 * {@code ISO 639-2} code.
	 *
	 * @param name the English language name.
	 * @param iso639_1 the {@code ISO 639-1} (two letter) code.
	 * @param iso639_2 the ISO 639-2 (three letter) code.
	 */
	private static void registerLanguage(String name, String iso639_1, String iso639_2) {
		registerLanguage(name, iso639_1, iso639_2, null);
	}

	/**
	 * Registers a language with a name, a {@code ISO 639-1} code and two
	 * {@code ISO 639-2} codes.
	 *
	 * @param name the English language name.
	 * @param iso639_1 the {@code ISO 639-1} (two letter) code.
	 * @param iso639_2 the bibliographic ISO 639-2 (three letter) code.
	 * @param altIso639_2 the terminology ISO 639-2 (three letter) code.
	 */
	private static void registerLanguage(String name, String iso639_1, String iso639_2, String altIso639_2) {
		String key = name.replaceAll("\\s*\\([^\\)]*\\)\\s*", "").toLowerCase(Locale.ROOT);
		links.put(key, new Iso639Entry(name, iso639_1, iso639_2, altIso639_2));
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
		registerLanguage("Afar", "aa", "aar");
		registerLanguage("Afrihili", null, "afh");
		registerLanguage("Afrikaans", "af", "afr");
		registerLanguage("Afro-Asiatic (Other)", null, "afa");
		registerLanguage("Akan", null, "aka");
		registerLanguage("Akkadian", null, "akk");
		registerLanguage("Albanian", "sq", "alb", "sqi");
		registerLanguage("Aleut", null, "ale");
		registerLanguage("Algonquian languages", null, "alg");
		registerLanguage("Altaic (Other)", null, "tut");
		registerLanguage("Amharic", "am", "amh");
		registerLanguage("Apache languages", null, "apa");
		registerLanguage("Arabic", "ar", "ara");
		registerLanguage("Aramaic", null, "arc");
		registerLanguage("Arapaho", null, "arp");
		registerLanguage("Araucanian", null, "arn");
		registerLanguage("Arawak", null, "arw");
		registerLanguage("Armenian", "hy", "arm", "hye");
		registerLanguage("Artificial (Other)", null, "art");
		registerLanguage("Assamese", "as", "asm");
		registerLanguage("Athapascan languages", null, "ath");
		registerLanguage("Australian languages", null, "aus");
		registerLanguage("Austronesian (Other)", null, "map");
		registerLanguage("Avaric", null, "ava");
		registerLanguage("Avestan", "ae", "ave");
		registerLanguage("Awadhi", null, "awa");
		registerLanguage("Aymara", "ay", "aym");
		registerLanguage("Azerbaijani", "az", "aze");
		registerLanguage("Balinese", null, "ban");
		registerLanguage("Baltic (Other)", null, "bat");
		registerLanguage("Baluchi", null, "bal");
		registerLanguage("Bambara", null, "bam");
		registerLanguage("Bamileke languages", null, "bai");
		registerLanguage("Banda", null, "bad");
		registerLanguage("Bantu (Other)", null, "bnt");
		registerLanguage("Basa", null, "bas");
		registerLanguage("Bashkir", "ba", "bak");
		registerLanguage("Basque", "eu", "baq", "eus");
		registerLanguage("Batak (Indonesia)", null, "btk");
		registerLanguage("Beja", null, "bej");
		registerLanguage("Belarusian", "be", "bel");
		registerLanguage("Bemba", null, "bem");
		registerLanguage("Bengali", "bn", "ben");
		registerLanguage("Berber (Other)", null, "ber");
		registerLanguage("Bhojpuri", null, "bho");
		registerLanguage("Bihari", "bh", "bih");
		registerLanguage("Bikol", null, "bik");
		registerLanguage("Bini", null, "bin");
		registerLanguage("Bislama", "bi", "bis");
		registerLanguage("Bosnian", "bs", "bos");
		registerLanguage("Brazilian Portuguese", null, "pob");
		registerLanguage("Braj", null, "bra");
		registerLanguage("Breton", "br", "bre");
		registerLanguage("Buginese", null, "bug");
		registerLanguage("Bulgarian", "bg", "bul");
		registerLanguage("Buriat", null, "bua");
		registerLanguage("Burmese", "my", "bur", "mya");
		registerLanguage("Caddo", null, "cad");
		registerLanguage("Carib", null, "car");
		registerLanguage("Catalan", "ca", "cat");
		registerLanguage("Caucasian (Other)", null, "cau");
		registerLanguage("Cebuano", null, "ceb");
		registerLanguage("Celtic (Other)", null, "cel");
		registerLanguage("Central American Indian (Other)", null, "cai");
		registerLanguage("Chagatai", null, "chg");
		registerLanguage("Chamic languages", null, "cmc");
		registerLanguage("Chamorro", "ch", "cha");
		registerLanguage("Chechen", "ce", "che");
		registerLanguage("Cherokee", null, "chr");
		registerLanguage("Cheyenne", null, "chy");
		registerLanguage("Chibcha", null, "chb");
		registerLanguage("Chichewa; Nyanja", "ny", "nya");
		registerLanguage("Chinese", "zh", "chi", "zho");
		registerLanguage("Chinook jargon", null, "chn");
		registerLanguage("Chipewyan", null, "chp");
		registerLanguage("Choctaw", null, "cho");
		registerLanguage("Church Slavic", "cu", "chu");
		registerLanguage("Chuukese", null, "chk");
		registerLanguage("Chuvash", "cv", "chv");
		registerLanguage("Coptic", null, "cop");
		registerLanguage("Cornish", "kw", "cor");
		registerLanguage("Corsican", "co", "cos");
		registerLanguage("Cree", null, "cre");
		registerLanguage("Creek", null, "mus");
		registerLanguage("Creoles and pidgins (Other)", null, "crp");
		registerLanguage("Creoles and pidgins, English-based (Other)", null, "cpe");
		registerLanguage("Creoles and pidgins, French-based (Other)", null, "cpf");
		registerLanguage("Creoles and pidgins, Portuguese-based (Other)", null, "cpp");
		registerLanguage("Croatian", "hr", "hrv");
		registerLanguage("Cushitic (Other)", null, "cus");
		registerLanguage("Czech", "cs", "cze", "ces");
		registerLanguage("Dakota", null, "dak");
		registerLanguage("Danish", "da", "dan");
		registerLanguage("Dayak", null, "day");
		registerLanguage("Delaware", null, "del");
		registerLanguage("Dinka", null, "din");
		registerLanguage("Divehi", null, "div");
		registerLanguage("Dogri", null, "doi");
		registerLanguage("Dogrib", null, "dgr");
		registerLanguage("Dravidian (Other)", null, "dra");
		registerLanguage("Duala", null, "dua");
		registerLanguage("Dutch", "nl", "dut", "nld");
		registerLanguage("Dutch, Middle (ca. 1050-1350)", null, "dum");
		registerLanguage("Dyula", null, "dyu");
		registerLanguage("Dzongkha", "dz", "dzo");
		registerLanguage("Efik", null, "efi");
		registerLanguage("Egyptian (Ancient)", null, "egy");
		registerLanguage("Ekajuk", null, "eka");
		registerLanguage("Elamite", null, "elx");
		registerLanguage("English", "en", "eng");
		registerLanguage("English, Middle (1100-1500)", null, "enm");
		registerLanguage("English, Old (ca.450-1100)", null, "ang");
		registerLanguage("Esperanto", "eo", "epo");
		registerLanguage("Estonian", "et", "est");
		registerLanguage("Ewe", null, "ewe");
		registerLanguage("Ewondo", null, "ewo");
		registerLanguage("Fang", null, "fan");
		registerLanguage("Fanti", null, "fat");
		registerLanguage("Faroese", "fo", "fao");
		registerLanguage("Fijian", "fj", "fij");
		registerLanguage("Finnish", "fi", "fin");
		registerLanguage("Finno-Ugrian (Other)", null, "fiu");
		registerLanguage("Fon", null, "fon");
		registerLanguage("French", "fr", "fre", "fra");
		registerLanguage("French, Middle (ca.1400-1600)", null, "frm");
		registerLanguage("French, Old (842-ca.1400)", null, "fro");
		registerLanguage("Frisian", "fy", "fry");
		registerLanguage("Friulian", null, "fur");
		registerLanguage("Fulah", null, "ful");
		registerLanguage("Ga", null, "gaa");
		registerLanguage("Gaelic (Scots)", "gd", "gla");
		registerLanguage("Gallegan", "gl", "glg");
		registerLanguage("Ganda", null, "lug");
		registerLanguage("Gayo", null, "gay");
		registerLanguage("Gbaya", null, "gba");
		registerLanguage("Geez", null, "gez");
		registerLanguage("Georgian", "ka", "geo", "kat");
		registerLanguage("German", "de", "ger", "deu");
		registerLanguage("German, Low; Saxon, Low; Low German; Low Saxon", null, "nds");
		registerLanguage("German, Middle High (ca.1050-1500)", null, "gmh");
		registerLanguage("German, Old High (ca.750-1050)", null, "goh");
		registerLanguage("Germanic (Other)", null, "gem");
		registerLanguage("Gilbertese", null, "gil");
		registerLanguage("Gondi", null, "gon");
		registerLanguage("Gorontalo", null, "gor");
		registerLanguage("Gothic", null, "got");
		registerLanguage("Grebo", null, "grb");
		registerLanguage("Greek, Ancient (to 1453)", null, "grc");
		registerLanguage("Greek", "el", "gre", "ell");
		registerLanguage("Guarani", "gn", "grn");
		registerLanguage("Gujarati", "gu", "guj");
		registerLanguage("Gwich-in", null, "gwi");
		registerLanguage("Haida", null, "hai");
		registerLanguage("Hausa", "ha", "hau");
		registerLanguage("Hawaiian", null, "haw");
		registerLanguage("Hebrew", "he", "heb");
		registerLanguage("Herero", "hz", "her");
		registerLanguage("Hiligaynon", null, "hil");
		registerLanguage("Himachali", null, "him");
		registerLanguage("Hindi", "hi", "hin");
		registerLanguage("Hiri Motu", "ho", "hmo");
		registerLanguage("Hittite", null, "hit");
		registerLanguage("Hmong", null, "hmn");
		registerLanguage("Hungarian", "hu", "hun");
		registerLanguage("Hupa", null, "hup");
		registerLanguage("Iban", null, "iba");
		registerLanguage("Icelandic", "is", "ice", "isl");
		registerLanguage("Igbo", null, "ibo");
		registerLanguage("Ijo", null, "ijo");
		registerLanguage("Iloko", null, "ilo");
		registerLanguage("Indic (Other)", null, "inc");
		registerLanguage("Indo-European (Other)", null, "ine");
		registerLanguage("Indonesian", "id", "ind");
		registerLanguage("Interlingua (International Auxiliary Language Association)", "ia", "ina");
		registerLanguage("Interlingue", "ie", "ile");
		registerLanguage("Inuktitut", "iu", "iku");
		registerLanguage("Inupiaq", "ik", "ipk");
		registerLanguage("Iranian (Other)", null, "ira");
		registerLanguage("Irish", "ga", "gle");
		registerLanguage("Irish, Middle (900-1200)", null, "mga");
		registerLanguage("Irish, Old (to 900)", null, "sga");
		registerLanguage("Iroquoian languages", null, "iro");
		registerLanguage("Italian", "it", "ita");
		registerLanguage("Japanese", "ja", "jpn");
		registerLanguage("Javanese", "jw", "jav", "jaw");
		registerLanguage("Judeo-Arabic", null, "jrb");
		registerLanguage("Judeo-Persian", null, "jpr");
		registerLanguage("Kabyle", null, "kab");
		registerLanguage("Kachin", null, "kac");
		registerLanguage("Kalaallisut", "kl", "kal");
		registerLanguage("Kamba", null, "kam");
		registerLanguage("Kannada", "kn", "kan");
		registerLanguage("Kanuri", null, "kau");
		registerLanguage("Kara-Kalpak", null, "kaa");
		registerLanguage("Karen", null, "kar");
		registerLanguage("Kashmiri", "ks", "kas");
		registerLanguage("Kawi", null, "kaw");
		registerLanguage("Kazakh", "kk", "kaz");
		registerLanguage("Khasi", null, "kha");
		registerLanguage("Khmer", "km", "khm");
		registerLanguage("Khoisan (Other)", null, "khi");
		registerLanguage("Khotanese", null, "kho");
		registerLanguage("Kikuyu", "ki", "kik");
		registerLanguage("Kimbundu", null, "kmb");
		registerLanguage("Kinyarwanda", "rw", "kin");
		registerLanguage("Kirghiz", "ky", "kir");
		registerLanguage("Komi", "kv", "kom");
		registerLanguage("Kongo", null, "kon");
		registerLanguage("Konkani", null, "kok");
		registerLanguage("Korean", "ko", "kor");
		registerLanguage("Kosraean", null, "kos");
		registerLanguage("Kpelle", null, "kpe");
		registerLanguage("Kru", null, "kro");
		registerLanguage("Kuanyama", "kj", "kua");
		registerLanguage("Kumyk", null, "kum");
		registerLanguage("Kurdish", "ku", "kur");
		registerLanguage("Kurukh", null, "kru");
		registerLanguage("Kutenai", null, "kut");
		registerLanguage("Ladino", null, "lad");
		registerLanguage("Lahnda", null, "lah");
		registerLanguage("Lamba", null, "lam");
		registerLanguage("Lao", "lo", "lao");
		registerLanguage("Latin", "la", "lat");
		registerLanguage("Latvian", "lv", "lav");
		registerLanguage("Letzeburgesch", "lb", "ltz");
		registerLanguage("Lezghian", null, "lez");
		registerLanguage("Lingala", "ln", "lin");
		registerLanguage("Lithuanian", "lt", "lit");
		registerLanguage("Low German; Low Saxon; German, Low; Saxon, Low", null, "nds");
		registerLanguage("Low Saxon; Low German; Saxon, Low; German, Low", null, "nds");
		registerLanguage("Lozi", null, "loz");
		registerLanguage("Luba-Katanga", null, "lub");
		registerLanguage("Luba-Lulua", null, "lua");
		registerLanguage("Luiseno", null, "lui");
		registerLanguage("Lunda", null, "lun");
		registerLanguage("Luo (Kenya and Tanzania)", null, "luo");
		registerLanguage("Lushai", null, "lus");
		registerLanguage("Macedonian", "mk", "mac", "mkd");
		registerLanguage("Madurese", null, "mad");
		registerLanguage("Magahi", null, "mag");
		registerLanguage("Maithili", null, "mai");
		registerLanguage("Makasar", null, "mak");
		registerLanguage("Malagasy", "mg", "mlg");
		registerLanguage("Malay", "ms", "may", "msa");
		registerLanguage("Malayalam", "ml", "mal");
		registerLanguage("Maltese", "mt", "mlt");
		registerLanguage("Manchu", null, "mnc");
		registerLanguage("Mandar", null, "mdr");
		registerLanguage("Mandarin", null, "mdr");
		registerLanguage("Mandingo", null, "man");
		registerLanguage("Manipuri", null, "mni");
		registerLanguage("Manobo languages", null, "mno");
		registerLanguage("Manx", "gv", "glv");
		registerLanguage("Maori", "mi", "mao", "mri");
		registerLanguage("Marathi", "mr", "mar");
		registerLanguage("Mari", null, "chm");
		registerLanguage("Marshall", "mh", "mah");
		registerLanguage("Marwari", null, "mwr");
		registerLanguage("Masai", null, "mas");
		registerLanguage("Mayan languages", null, "myn");
		registerLanguage("Mende", null, "men");
		registerLanguage("Micmac", null, "mic");
		registerLanguage("Minangkabau", null, "min");
		registerLanguage("Miscellaneous languages", null, "mis");
		registerLanguage("Mohawk", null, "moh");
		registerLanguage("Moldavian", "mo", "mol");
		registerLanguage("Mon-Khmer (Other)", null, "mkh");
		registerLanguage("Mongo", null, "lol");
		registerLanguage("Mongolian", "mn", "mon");
		registerLanguage("Mossi", null, "mos");
		registerLanguage("Munda languages", null, "mun");
		registerLanguage("Nahuatl", null, "nah");
		registerLanguage("Nauru", "na", "nau");
		registerLanguage("Navajo", "nv", "nav");
		registerLanguage("Ndebele, North", "nd", "nde");
		registerLanguage("Ndebele, South", "nr", "nbl");
		registerLanguage("Ndonga", "ng", "ndo");
		registerLanguage("Nepali", "ne", "nep");
		registerLanguage("Newari", null, "new");
		registerLanguage("Nias", null, "nia");
		registerLanguage("Niger-Kordofanian (Other)", null, "nic");
		registerLanguage("Nilo-Saharan (Other)", null, "ssa");
		registerLanguage("Niuean", null, "niu");
		registerLanguage("Norse, Old", null, "non");
		registerLanguage("North American Indian(Other)", null, "nai");
		registerLanguage("Northern Sami", "se", "sme");
		registerLanguage("Norwegian", "no", "nor");
		registerLanguage("Norwegian Bokmal", "nb", "nob");
		registerLanguage("Norwegian Nynorsk", "nn", "nno");
		registerLanguage("Nubian languages", null, "nub");
		registerLanguage("Nyamwezi", null, "nym");
		registerLanguage("Nyanja; Chichewa", "ny", "nya");
		registerLanguage("Nyankole", null, "nyn");
		registerLanguage("Nyoro", null, "nyo");
		registerLanguage("Nzima", null, "nzi");
		registerLanguage("Occitan (post 1500); Provencal", "oc", "oci");
		registerLanguage("Ojibwa", null, "oji");
		registerLanguage("Oriya", "or", "ori");
		registerLanguage("Oromo", "om", "orm");
		registerLanguage("Osage", null, "osa");
		registerLanguage("Ossetian; Ossetic", "os", "oss");
		registerLanguage("Otomian languages", null, "oto");
		registerLanguage("Pahlavi", null, "pal");
		registerLanguage("Palauan", null, "pau");
		registerLanguage("Pali", "pi", "pli");
		registerLanguage("Pampanga", null, "pam");
		registerLanguage("Pangasinan", null, "pag");
		registerLanguage("Panjabi", "pa", "pan");
		registerLanguage("Papiamento", null, "pap");
		registerLanguage("Papuan (Other)", null, "paa");
		registerLanguage("Persian", "fa", "per", "fas");
		registerLanguage("Persian, Old (ca.600-400 B.C.)", null, "peo");
		registerLanguage("Philippine (Other)", null, "phi");
		registerLanguage("Phoenician", null, "phn");
		registerLanguage("Pohnpeian", null, "pon");
		registerLanguage("Polish", "pl", "pol");
		registerLanguage("Portuguese", "pt", "por");
		registerLanguage("Prakrit languages", null, "pra");
		registerLanguage("Provencal; Occitan (post 1500)", "oc", "oci");
		registerLanguage("Provencal, Old (to 1500)", null, "pro");
		registerLanguage("Pushto", "ps", "pus");
		registerLanguage("Quechua", "qu", "que");
		registerLanguage("Raeto-Romance", "rm", "roh");
		registerLanguage("Rajasthani", null, "raj");
		registerLanguage("Rapanui", null, "rap");
		registerLanguage("Rarotongan", null, "rar");
		registerLanguage("Romance (Other)", null, "roa");
		registerLanguage("Romanian", "ro", "rum", "ron");
		registerLanguage("Romany", null, "rom");
		registerLanguage("Rundi", "rn", "run");
		registerLanguage("Russian", "ru", "rus");
		registerLanguage("Salishan languages", null, "sal");
		registerLanguage("Samaritan Aramaic", null, "sam");
		registerLanguage("Sami languages (Other)", null, "smi");
		registerLanguage("Samoan", "sm", "smo");
		registerLanguage("Sandawe", null, "sad");
		registerLanguage("Sango", "sg", "sag");
		registerLanguage("Sanskrit", "sa", "san");
		registerLanguage("Santali", null, "sat");
		registerLanguage("Sardinian", "sc", "srd");
		registerLanguage("Sasak", null, "sas");
		registerLanguage("Saxon, Low; German, Low; Low Saxon; Low German", null, "nds");
		registerLanguage("Scots", null, "sco");
		registerLanguage("Selkup", null, "sel");
		registerLanguage("Semitic (Other)", null, "sem");
		registerLanguage("Serbian", "sr", "scc", "srp");
		registerLanguage("Serer", null, "srr");
		registerLanguage("Shan", null, "shn");
		registerLanguage("Shona", "sn", "sna");
		registerLanguage("Sidamo", null, "sid");
		registerLanguage("Sign languages", null, "sgn");
		registerLanguage("Siksika", null, "bla");
		registerLanguage("Sindhi", "sd", "snd");
		registerLanguage("Sinhalese", "si", "sin");
		registerLanguage("Sino-Tibetan (Other)", null, "sit");
		registerLanguage("Siouan languages", null, "sio");
		registerLanguage("Slave (Athapascan)", null, "den");
		registerLanguage("Slavic (Other)", null, "sla");
		registerLanguage("Slovak", "sk", "slo", "slk");
		registerLanguage("Slovenian", "sl", "slv");
		registerLanguage("Sogdian", null, "sog");
		registerLanguage("Somali", "so", "som");
		registerLanguage("Songhai", null, "son");
		registerLanguage("Soninke", null, "snk");
		registerLanguage("Sorbian languages", null, "wen");
		registerLanguage("Sotho, Northern", null, "nso");
		registerLanguage("Sotho, Southern", "st", "sot");
		registerLanguage("South American Indian (Other)", null, "sai");
		registerLanguage("Spanish", "es", "spa");
		registerLanguage("Sukuma", null, "suk");
		registerLanguage("Sumerian", null, "sux");
		registerLanguage("Sundanese", "su", "sun");
		registerLanguage("Susu", null, "sus");
		registerLanguage("Swahili", "sw", "swa");
		registerLanguage("Swati", "ss", "ssw");
		registerLanguage("Swedish", "sv", "swe");
		registerLanguage("Syriac", null, "syr");
		registerLanguage("Tagalog", "tl", "tgl");
		registerLanguage("Tahitian", "ty", "tah");
		registerLanguage("Tai (Other)", null, "tai");
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
		registerLanguage("Turkish", "tr", "tur");
		registerLanguage("Turkish, Ottoman (1500-1928)", null, "ota");
		registerLanguage("Turkmen", "tk", "tuk");
		registerLanguage("Tuvalu", null, "tvl");
		registerLanguage("Tuvinian", null, "tyv");
		registerLanguage("Twi", "tw", "twi");
		registerLanguage("Ugaritic", null, "uga");
		registerLanguage("Uighur", "ug", "uig");
		registerLanguage("Ukrainian", "uk", "ukr");
		registerLanguage("Umbundu", null, "umb");
		registerLanguage("Undetermined", null, DLNAMediaLang.UND, DLNAMediaLang.UND);
		registerLanguage("Urdu", "ur", "urd");
		registerLanguage("Uzbek", "uz", "uzb");
		registerLanguage("Vai", null, "vai");
		registerLanguage("Venda", null, "ven");
		registerLanguage("Vietnamese", "vi", "vie");
		registerLanguage("Volapuk", "vo", "vol");
		registerLanguage("Votic", null, "vot");
		registerLanguage("Wakashan languages", null, "wak");
		registerLanguage("Walamo", null, "wal");
		registerLanguage("Waray", null, "war");
		registerLanguage("Washo", null, "was");
		registerLanguage("Welsh", "cy", "wel", "cym");
		registerLanguage("Wolof", "wo", "wol");
		registerLanguage("Xhosa", "xh", "xho");
		registerLanguage("Yakut", null, "sah");
		registerLanguage("Yao", null, "yao");
		registerLanguage("Yapese", null, "yap");
		registerLanguage("Yiddish", "yi", "yid");
		registerLanguage("Yoruba", "yo", "yor");
		registerLanguage("Yupik languages", null, "ypk");
		registerLanguage("Zande", null, "znd");
		registerLanguage("Zapotec", null, "zap");
		registerLanguage("Zenaga", null, "zen");
		registerLanguage("Zhuang", "za", "zha");
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
		private final String name;
		private final String iso639_1;
		private final String iso639_2;
		private final String altIso639_2;

		/**
		 * Creates a new instance.
		 *
		 * @param name the English language name.
		 * @param iso639_1 the ISO 639-1 (two letter) code.
		 * @param iso639_2 the bibliographic ISO 639-2 (three letter) code.
		 * @param altIso639_2 the terminology ISO 639-2 (three letter) code.
		 */
		public Iso639Entry(String name, String iso639_1, String iso639_2, String altIso639_2) {
			if (isBlank(name)) {
				throw new IllegalArgumentException("name cannot be null");
			}
			this.name = name;
			this.iso639_1 = iso639_1;
			this.iso639_2 = iso639_2;
			this.altIso639_2 = altIso639_2;
		}

		/**
		 * @return The English language name.
		 */
		public String getName() {
			return name;
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
	}
}
