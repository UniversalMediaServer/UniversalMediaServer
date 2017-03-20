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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaLang;

/**
 * This class provides a list of languages mapped to ISO 639 language codes
 * and some methods to verify which language matches which ISO code.
 *
 * Made immutable in UMS version 5.2.3
 */
public final class Iso639 {
	/**
	 * ISO code alias for the language set in the preferences
	 */
	private static final String LOCAL_ALIAS = "loc";

	/**
	 * Hashmap that contains full language names and their ISO codes.
	 */
	private static HashMap<String, String[]> links = new HashMap<>();

	/**
	 * List that contains all known language names.
	 */
	private static ArrayList<String> languages = new ArrayList<>();

	/**
	 * List that contains all known ISO language codes.
	 */
	private static ArrayList<String> codes = new ArrayList<>();

	static {
		// Make sure everything is initialized before it is retrieved.
		initLinks();
		initLanguages();
		initCodes();
	}

	/**
	 * Returns the full language name for a given ISO language code. Will return
	 * null when the language name cannot be determined.
	 *
	 * @param code
	 *            The ISO language code.
	 * @return The full language name.
	 */
	public static String getLanguage(String code) {
		if (code == null) {
			return null;
		}

		String lang = null;
		Iterator<Entry<String, String[]>> iterator = links.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, String[]> entry = iterator.next();

			for (String c : entry.getValue()) {
				if (code.equalsIgnoreCase(c)) {
					return entry.getKey();
				}
			}
		}

		return lang;
	}

	/**
	 * Returns whether the code is a valid/known code or not.
	 *
	 * @param code the ISO language code.
	 */
	public static boolean codeIsValid(String code) {
		if (code != null && !code.isEmpty()) {
			for (String s : codes) {
				if (s.equalsIgnoreCase(code)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the ISO 639_2 code for an ISO code. Will return null if no
	 * match can be found.
	 *
	 * @param code The ISO code.
	 * @return The ISO 639_2 code.
	 */
	public static String getISO639_2Code(String code) {
		if (code == null) {
			return null;
		}

		Iterator<Entry<String, String[]>> iterator = links.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, String[]> entry = iterator.next();
			for (String c : entry.getValue()) {
				if (code.equalsIgnoreCase(c)) {
					return entry.getValue()[entry.getValue().length - 1].toLowerCase();
				}
			}
		}

		return null;
	}

	/**
	 *
	 * Returns the shortest possible ISO 639 code for an ISO code (as per
	 * {@link java.util.Locale} specification). Will return null if no
	 * match can be found.
	 *
	 * @param code the ISO code.
	 * @return The ISO 639-1 or 639-2 code.
	 */
	public static String getISOCode(String code) {
		if (code == null) {
			return null;
		}
		Iterator<Entry<String, String[]>> iterator = links.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, String[]> entry = iterator.next();
			for (String c : entry.getValue()) {
				if (code.equalsIgnoreCase(c)) {
					return entry.getValue()[0];
				}
			}
		}

		return null;
	}

	/**
	 * Returns the ISO code, except when the alias "loc" is used. In that case
	 * the ISO code of the preferred language in the UMS settings is returned.
	 *
	 * @param isoCode An ISO code, or <code>"loc"</code>.
	 * @return The code.
	 */
	private static String normalize(String isoCode) {
		if (LOCAL_ALIAS.equals(isoCode)) {
			return PMS.getConfiguration().getLanguageTag();
		} else {
			return isoCode;
		}
	}

	/**
	 * Verifies that a full language name is matching an ISO code. Returns true
	 * if a match can be made, false otherwise.
	 *
	 * @param language
	 *            The full language name.
	 * @param code
	 *            The ISO code. If "loc" is given, the ISO code of the preferred
	 *            language is used instead.
	 * @return True if both match, false otherwise.
	 */
	public static boolean isCodeMatching(String language, String code) {
		if (language == null || code == null) {
			return false;
		}

		String isoCode = normalize(code);
		String codes[] = links.get(language);

		if (codes != null) {
			for (String c : codes) {
				if (c.equalsIgnoreCase(isoCode)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Verifies that two ISO codes match the same language. Returns true if a
	 * match can be made, false otherwise. The alias "loc" can be used as a code,
	 * it will be replaced by the ISO code of the preferred language from the
	 * PMS settings.
	 *
	 * @param code1 The first ISO code.
	 * @param code2 The second ISO code.
	 * @return True if both match, false otherwise.
	 */
	public static boolean isCodesMatching(String code1, String code2) {
		if (code1 == null || code2 == null) {
			return false;
		}

		String isoCode1 = normalize(code1);
		String isoCode2 = normalize(code2);

		Iterator<Entry<String, String[]>> iterator = links.entrySet().iterator();

		while (iterator.hasNext()) {
			Entry<String, String[]> entry = iterator.next();

			for (String c : entry.getValue()) {
				if (isoCode1.equalsIgnoreCase(c)) {
					for (String c2 : entry.getValue()) {
						if (isoCode2.equalsIgnoreCase(c2)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Add a full language name and up to three language codes to the mapping.
	 * The language codes can be null.
	 *
	 * @param language The full language name.
	 * @param iso6391 The first ISO code
	 * @param iso6392 The second ISO code
	 * @param iso6392bis The third ISO code
	 */
	private static void putCode(String language, String iso6391, String iso6392, String iso6392bis) {
		ArrayList<String> codeArray = new ArrayList<>();

		if (iso6391 != null) {
			codeArray.add(iso6391);
		}

		if (iso6392 != null) {
			codeArray.add(iso6392);
		}

		if (iso6392bis != null) {
			codeArray.add(iso6392bis);
		}

		String[] newCodes = new String[codeArray.size()];
		codeArray.toArray(newCodes);
		links.put(language, newCodes);
	}

	/**
	 * Initialize the list of language strings.
	 */
	private static void initLanguages() {
		Iterator<String> iterator = links.keySet().iterator();
		while (iterator.hasNext()) {
			languages.add(iterator.next());
		}
	}

	/**
	 * Initialize the list of language codes.
	 */
	private static void initCodes() {
		codes = new ArrayList<>();
		Iterator<String[]> iterator = links.values().iterator();

		while (iterator.hasNext()) {
			codes.addAll(Arrays.asList(iterator.next()));
		}
	}

	/**
	 * Initialize the hashmap containing languages and their codes.
	 */
	private static void initLinks() {
		putCode("Abkhazian", "ab", "abk", "abk");
		putCode("Achinese", null, "ace", "ace");
		putCode("Acoli", null, "ach", "ach");
		putCode("Adangme", null, "ada", "ada");
		putCode("Afar", "aa", "aar", "aar");
		putCode("Afrihili", null, "afh", "afh");
		putCode("Afrikaans", "af", "afr", "afr");
		putCode("Afro-Asiatic (Other)", null, "afa", "afa");
		putCode("Akan", null, "aka", "aka");
		putCode("Akkadian", null, "akk", "akk");
		putCode("Albanian", "sq", "sqi", "alb");
		putCode("Aleut", null, "ale", "ale");
		putCode("Algonquian languages", null, "alg", "alg");
		putCode("Altaic (Other)", null, "tut", "tut");
		putCode("Amharic", "am", "amh", "amh");
		putCode("Apache languages", null, "apa", "apa");
		putCode("Arabic", "ar", "ara", "ara");
		putCode("Aramaic", null, "arc", "arc");
		putCode("Arapaho", null, "arp", "arp");
		putCode("Araucanian", null, "arn", "arn");
		putCode("Arawak", null, "arw", "arw");
		putCode("Armenian", "hy", "hye", "arm");
		putCode("Artificial (Other)", null, "art", "art");
		putCode("Assamese", "as", "asm", "asm");
		putCode("Athapascan languages", null, "ath", "ath");
		putCode("Australian languages", null, "aus", "aus");
		putCode("Austronesian (Other)", null, "map", "map");
		putCode("Avaric", null, "ava", "ava");
		putCode("Avestan", "ae", "ave", "ave");
		putCode("Awadhi", null, "awa", "awa");
		putCode("Aymara", "ay", "aym", "aym");
		putCode("Azerbaijani", "az", "aze", "aze");
		putCode("Balinese", null, "ban", "ban");
		putCode("Baltic (Other)", null, "bat", "bat");
		putCode("Baluchi", null, "bal", "bal");
		putCode("Bambara", null, "bam", "bam");
		putCode("Bamileke languages", null, "bai", "bai");
		putCode("Banda", null, "bad", "bad");
		putCode("Bantu (Other)", null, "bnt", "bnt");
		putCode("Basa", null, "bas", "bas");
		putCode("Bashkir", "ba", "bak", "bak");
		putCode("Basque", "eu", "eus", "baq");
		putCode("Batak (Indonesia)", null, "btk", "btk");
		putCode("Beja", null, "bej", "bej");
		putCode("Belarusian", "be", "bel", "bel");
		putCode("Bemba", null, "bem", "bem");
		putCode("Bengali", "bn", "ben", "ben");
		putCode("Berber (Other)", null, "ber", "ber");
		putCode("Bhojpuri", null, "bho", "bho");
		putCode("Bihari", "bh", "bih", "bih");
		putCode("Bikol", null, "bik", "bik");
		putCode("Bini", null, "bin", "bin");
		putCode("Bislama", "bi", "bis", "bis");
		putCode("Bosnian", "bs", "bos", "bos");
		putCode("Brazilian Portuguse", null, "pob", "pob");
		putCode("Braj", null, "bra", "bra");
		putCode("Breton", "br", "bre", "bre");
		putCode("Buginese", null, "bug", "bug");
		putCode("Bulgarian", "bg", "bul", "bul");
		putCode("Buriat", null, "bua", "bua");
		putCode("Burmese", "my", "mya", "bur");
		putCode("Caddo", null, "cad", "cad");
		putCode("Carib", null, "car", "car");
		putCode("Catalan", "ca", "cat", "cat");
		putCode("Caucasian (Other)", null, "cau", "cau");
		putCode("Cebuano", null, "ceb", "ceb");
		putCode("Celtic (Other)", null, "cel", "cel");
		putCode("Central American Indian (Other)", null, "cai", "cai");
		putCode("Chagatai", null, "chg", "chg");
		putCode("Chamic languages", null, "cmc", "cmc");
		putCode("Chamorro", "ch", "cha", "cha");
		putCode("Chechen", "ce", "che", "che");
		putCode("Cherokee", null, "chr", "chr");
		putCode("Cheyenne", null, "chy", "chy");
		putCode("Chibcha", null, "chb", "chb");
		putCode("Chichewa; Nyanja", "ny", "nya", "nya");
		putCode("Chinese", "zh", "zho", "chi");
		putCode("Chinook jargon", null, "chn", "chn");
		putCode("Chipewyan", null, "chp", "chp");
		putCode("Choctaw", null, "cho", "cho");
		putCode("Church Slavic", "cu", "chu", "chu");
		putCode("Chuukese", null, "chk", "chk");
		putCode("Chuvash", "cv", "chv", "chv");
		putCode("Coptic", null, "cop", "cop");
		putCode("Cornish", "kw", "cor", "cor");
		putCode("Corsican", "co", "cos", "cos");
		putCode("Cree", null, "cre", "cre");
		putCode("Creek", null, "mus", "mus");
		putCode("Creoles and pidgins (Other)", null, "crp", "crp");
		putCode("Creoles and pidgins, English-based (Other)", null, "cpe", "cpe");
		putCode("Creoles and pidgins, French-based (Other)", null, "cpf", "cpf");
		putCode("Creoles and pidgins, Portuguese-based (Other)", null, "cpp", "cpp");
		putCode("Croatian", "hr", "hrv", "scr");
		putCode("Cushitic (Other)", null, "cus", "cus");
		putCode("Czech", "cs", "ces", "cze");
		putCode("Dakota", null, "dak", "dak");
		putCode("Danish", "da", "dan", "dan");
		putCode("Dayak", null, "day", "day");
		putCode("Delaware", null, "del", "del");
		putCode("Dinka", null, "din", "din");
		putCode("Divehi", null, "div", "div");
		putCode("Dogri", null, "doi", "doi");
		putCode("Dogrib", null, "dgr", "dgr");
		putCode("Dravidian (Other)", null, "dra", "dra");
		putCode("Duala", null, "dua", "dua");
		putCode("Dutch", "nl", "nld", "dut");
		putCode("Dutch, Middle (ca. 1050-1350)", null, "dum", "dum");
		putCode("Dyula", null, "dyu", "dyu");
		putCode("Dzongkha", "dz", "dzo", "dzo");
		putCode("Efik", null, "efi", "efi");
		putCode("Egyptian (Ancient)", null, "egy", "egy");
		putCode("Ekajuk", null, "eka", "eka");
		putCode("Elamite", null, "elx", "elx");
		putCode("English", "en", "eng", "eng");
		putCode("English, Middle (1100-1500)", null, "enm", "enm");
		putCode("English, Old (ca.450-1100)", null, "ang", "ang");
		putCode("Esperanto", "eo", "epo", "epo");
		putCode("Estonian", "et", "est", "est");
		putCode("Ewe", null, "ewe", "ewe");
		putCode("Ewondo", null, "ewo", "ewo");
		putCode("Fang", null, "fan", "fan");
		putCode("Fanti", null, "fat", "fat");
		putCode("Faroese", "fo", "fao", "fao");
		putCode("Fijian", "fj", "fij", "fij");
		putCode("Finnish", "fi", "fin", "fin");
		putCode("Finno-Ugrian (Other)", null, "fiu", "fiu");
		putCode("Fon", null, "fon", "fon");
		putCode("French", "fr", "fra", "fre");
		putCode("French, Middle (ca.1400-1600)", null, "frm", "frm");
		putCode("French, Old (842-ca.1400)", null, "fro", "fro");
		putCode("Frisian", "fy", "fry", "fry");
		putCode("Friulian", null, "fur", "fur");
		putCode("Fulah", null, "ful", "ful");
		putCode("Ga", null, "gaa", "gaa");
		putCode("Gaelic (Scots)", "gd", "gla", "gla");
		putCode("Gallegan", "gl", "glg", "glg");
		putCode("Ganda", null, "lug", "lug");
		putCode("Gayo", null, "gay", "gay");
		putCode("Gbaya", null, "gba", "gba");
		putCode("Geez", null, "gez", "gez");
		putCode("Georgian", "ka", "kat", "geo");
		putCode("German", "de", "deu", "ger");
		putCode("German, Low; Saxon, Low; Low German; Low Saxon", null, "nds", "nds");
		putCode("German, Middle High (ca.1050-1500)", null, "gmh", "gmh");
		putCode("German, Old High (ca.750-1050)", null, "goh", "goh");
		putCode("Germanic (Other)", null, "gem", "gem");
		putCode("Gilbertese", null, "gil", "gil");
		putCode("Gondi", null, "gon", "gon");
		putCode("Gorontalo", null, "gor", "gor");
		putCode("Gothic", null, "got", "got");
		putCode("Grebo", null, "grb", "grb");
		putCode("Greek, Ancient (to 1453)", null, "grc", "grc");
		putCode("Greek, Modern (1453-)", "el", "ell", "gre");
		putCode("Guarani", "gn", "grn", "grn");
		putCode("Gujarati", "gu", "guj", "guj");
		putCode("Gwich-in", null, "gwi", "gwi");
		putCode("Haida", null, "hai", "hai");
		putCode("Hausa", "ha", "hau", "hau");
		putCode("Hawaiian", null, "haw", "haw");
		putCode("Hebrew", "he", "heb", "heb");
		putCode("Herero", "hz", "her", "her");
		putCode("Hiligaynon", null, "hil", "hil");
		putCode("Himachali", null, "him", "him");
		putCode("Hindi", "hi", "hin", "hin");
		putCode("Hiri Motu", "ho", "hmo", "hmo");
		putCode("Hittite", null, "hit", "hit");
		putCode("Hmong", null, "hmn", "hmn");
		putCode("Hungarian", "hu", "hun", "hun");
		putCode("Hupa", null, "hup", "hup");
		putCode("Iban", null, "iba", "iba");
		putCode("Icelandic", "is", "isl", "ice");
		putCode("Igbo", null, "ibo", "ibo");
		putCode("Ijo", null, "ijo", "ijo");
		putCode("Iloko", null, "ilo", "ilo");
		putCode("Indic (Other)", null, "inc", "inc");
		putCode("Indo-European (Other)", null, "ine", "ine");
		putCode("Indonesian", "id", "ind", "ind");
		putCode("Interlingua (International Auxiliary Language Association)", "ia", "ina", "ina");
		putCode("Interlingue", "ie", "ile", "ile");
		putCode("Inuktitut", "iu", "iku", "iku");
		putCode("Inupiaq", "ik", "ipk", "ipk");
		putCode("Iranian (Other)", null, "ira", "ira");
		putCode("Irish", "ga", "gle", "gle");
		putCode("Irish, Middle (900-1200)", null, "mga", "mga");
		putCode("Irish, Old (to 900)", null, "sga", "sga");
		putCode("Iroquoian languages", null, "iro", "iro");
		putCode("Italian", "it", "ita", "ita");
		putCode("Japanese", "ja", "jpn", "jpn");
		putCode("Javanese", "jw", "jaw", "jav");
		putCode("Judeo-Arabic", null, "jrb", "jrb");
		putCode("Judeo-Persian", null, "jpr", "jpr");
		putCode("Kabyle", null, "kab", "kab");
		putCode("Kachin", null, "kac", "kac");
		putCode("Kalaallisut", "kl", "kal", "kal");
		putCode("Kamba", null, "kam", "kam");
		putCode("Kannada", "kn", "kan", "kan");
		putCode("Kanuri", null, "kau", "kau");
		putCode("Kara-Kalpak", null, "kaa", "kaa");
		putCode("Karen", null, "kar", "kar");
		putCode("Kashmiri", "ks", "kas", "kas");
		putCode("Kawi", null, "kaw", "kaw");
		putCode("Kazakh", "kk", "kaz", "kaz");
		putCode("Khasi", null, "kha", "kha");
		putCode("Khmer", "km", "khm", "khm");
		putCode("Khoisan (Other)", null, "khi", "khi");
		putCode("Khotanese", null, "kho", "kho");
		putCode("Kikuyu", "ki", "kik", "kik");
		putCode("Kimbundu", null, "kmb", "kmb");
		putCode("Kinyarwanda", "rw", "kin", "kin");
		putCode("Kirghiz", "ky", "kir", "kir");
		putCode("Komi", "kv", "kom", "kom");
		putCode("Kongo", null, "kon", "kon");
		putCode("Konkani", null, "kok", "kok");
		putCode("Korean", "ko", "kor", "kor");
		putCode("Kosraean", null, "kos", "kos");
		putCode("Kpelle", null, "kpe", "kpe");
		putCode("Kru", null, "kro", "kro");
		putCode("Kuanyama", "kj", "kua", "kua");
		putCode("Kumyk", null, "kum", "kum");
		putCode("Kurdish", "ku", "kur", "kur");
		putCode("Kurukh", null, "kru", "kru");
		putCode("Kutenai", null, "kut", "kut");
		putCode("Ladino", null, "lad", "lad");
		putCode("Lahnda", null, "lah", "lah");
		putCode("Lamba", null, "lam", "lam");
		putCode("Lao", "lo", "lao", "lao");
		putCode("Latin", "la", "lat", "lat");
		putCode("Latvian", "lv", "lav", "lav");
		putCode("Letzeburgesch", "lb", "ltz", "ltz");
		putCode("Lezghian", null, "lez", "lez");
		putCode("Lingala", "ln", "lin", "lin");
		putCode("Lithuanian", "lt", "lit", "lit");
		putCode("Low German; Low Saxon; German, Low; Saxon, Low", null, "nds", "nds");
		putCode("Low Saxon; Low German; Saxon, Low; German, Low", null, "nds", "nds");
		putCode("Lozi", null, "loz", "loz");
		putCode("Luba-Katanga", null, "lub", "lub");
		putCode("Luba-Lulua", null, "lua", "lua");
		putCode("Luiseno", null, "lui", "lui");
		putCode("Lunda", null, "lun", "lun");
		putCode("Luo (Kenya and Tanzania)", null, "luo", "luo");
		putCode("Lushai", null, "lus", "lus");
		putCode("Macedonian", "mk", "mkd", "mac");
		putCode("Madurese", null, "mad", "mad");
		putCode("Magahi", null, "mag", "mag");
		putCode("Maithili", null, "mai", "mai");
		putCode("Makasar", null, "mak", "mak");
		putCode("Malagasy", "mg", "mlg", "mlg");
		putCode("Malay", "ms", "msa", "may");
		putCode("Malayalam", "ml", "mal", "mal");
		putCode("Maltese", "mt", "mlt", "mlt");
		putCode("Manchu", null, "mnc", "mnc");
		putCode("Mandar", null, "mdr", "mdr");
		putCode("Mandingo", null, "man", "man");
		putCode("Manipuri", null, "mni", "mni");
		putCode("Manobo languages", null, "mno", "mno");
		putCode("Manx", "gv", "glv", "glv");
		putCode("Maori", "mi", "mri", "mao");
		putCode("Marathi", "mr", "mar", "mar");
		putCode("Mari", null, "chm", "chm");
		putCode("Marshall", "mh", "mah", "mah");
		putCode("Marwari", null, "mwr", "mwr");
		putCode("Masai", null, "mas", "mas");
		putCode("Mayan languages", null, "myn", "myn");
		putCode("Mende", null, "men", "men");
		putCode("Micmac", null, "mic", "mic");
		putCode("Minangkabau", null, "min", "min");
		putCode("Miscellaneous languages", null, "mis", "mis");
		putCode("Mohawk", null, "moh", "moh");
		putCode("Moldavian", "mo", "mol", "mol");
		putCode("Mon-Khmer (Other)", null, "mkh", "mkh");
		putCode("Mongo", null, "lol", "lol");
		putCode("Mongolian", "mn", "mon", "mon");
		putCode("Mossi", null, "mos", "mos");
		putCode("Munda languages", null, "mun", "mun");
		putCode("Nahuatl", null, "nah", "nah");
		putCode("Nauru", "na", "nau", "nau");
		putCode("Navajo", "nv", "nav", "nav");
		putCode("Ndebele, North", "nd", "nde", "nde");
		putCode("Ndebele, South", "nr", "nbl", "nbl");
		putCode("Ndonga", "ng", "ndo", "ndo");
		putCode("Nepali", "ne", "nep", "nep");
		putCode("Newari", null, "new", "new");
		putCode("Nias", null, "nia", "nia");
		putCode("Niger-Kordofanian (Other)", null, "nic", "nic");
		putCode("Nilo-Saharan (Other)", null, "ssa", "ssa");
		putCode("Niuean", null, "niu", "niu");
		putCode("Norse, Old", null, "non", "non");
		putCode("North American Indian(Other)", null, "nai", "nai");
		putCode("Northern Sami", "se", "sme", "sme");
		putCode("Norwegian", "no", "nor", "nor");
		putCode("Norwegian Bokmal", "nb", "nob", "nob");
		putCode("Norwegian Nynorsk", "nn", "nno", "nno");
		putCode("Nubian languages", null, "nub", "nub");
		putCode("Nyamwezi", null, "nym", "nym");
		putCode("Nyanja; Chichewa", "ny", "nya", "nya");
		putCode("Nyankole", null, "nyn", "nyn");
		putCode("Nyoro", null, "nyo", "nyo");
		putCode("Nzima", null, "nzi", "nzi");
		putCode("Occitan (post 1500); Provencal", "oc", "oci", "oci");
		putCode("Ojibwa", null, "oji", "oji");
		putCode("Oriya", "or", "ori", "ori");
		putCode("Oromo", "om", "orm", "orm");
		putCode("Osage", null, "osa", "osa");
		putCode("Ossetian; Ossetic", "os", "oss", "oss");
		putCode("Otomian languages", null, "oto", "oto");
		putCode("Pahlavi", null, "pal", "pal");
		putCode("Palauan", null, "pau", "pau");
		putCode("Pali", "pi", "pli", "pli");
		putCode("Pampanga", null, "pam", "pam");
		putCode("Pangasinan", null, "pag", "pag");
		putCode("Panjabi", "pa", "pan", "pan");
		putCode("Papiamento", null, "pap", "pap");
		putCode("Papuan (Other)", null, "paa", "paa");
		putCode("Persian", "fa", "fas", "per");
		putCode("Persian, Old (ca.600-400 B.C.)", null, "peo", "peo");
		putCode("Philippine (Other)", null, "phi", "phi");
		putCode("Phoenician", null, "phn", "phn");
		putCode("Pohnpeian", null, "pon", "pon");
		putCode("Polish", "pl", "pol", "pol");
		putCode("Portuguese", "pt", "por", "por");
		putCode("Prakrit languages", null, "pra", "pra");
		putCode("Provencal; Occitan (post 1500)", "oc", "oci", "oci");
		putCode("Provencal, Old (to 1500)", null, "pro", "pro");
		putCode("Pushto", "ps", "pus", "pus");
		putCode("Quechua", "qu", "que", "que");
		putCode("Raeto-Romance", "rm", "roh", "roh");
		putCode("Rajasthani", null, "raj", "raj");
		putCode("Rapanui", null, "rap", "rap");
		putCode("Rarotongan", null, "rar", "rar");
		putCode("Romance (Other)", null, "roa", "roa");
		putCode("Romanian", "ro", "ron", "rum");
		putCode("Romany", null, "rom", "rom");
		putCode("Rundi", "rn", "run", "run");
		putCode("Russian", "ru", "rus", "rus");
		putCode("Salishan languages", null, "sal", "sal");
		putCode("Samaritan Aramaic", null, "sam", "sam");
		putCode("Sami languages (Other)", null, "smi", "smi");
		putCode("Samoan", "sm", "smo", "smo");
		putCode("Sandawe", null, "sad", "sad");
		putCode("Sango", "sg", "sag", "sag");
		putCode("Sanskrit", "sa", "san", "san");
		putCode("Santali", null, "sat", "sat");
		putCode("Sardinian", "sc", "srd", "srd");
		putCode("Sasak", null, "sas", "sas");
		putCode("Saxon, Low; German, Low; Low Saxon; Low German", null, "nds", "nds");
		putCode("Scots", null, "sco", "sco");
		putCode("Selkup", null, "sel", "sel");
		putCode("Semitic (Other)", null, "sem", "sem");
		putCode("Serbian", "sr", "srp", "scc");
		putCode("Serer", null, "srr", "srr");
		putCode("Shan", null, "shn", "shn");
		putCode("Shona", "sn", "sna", "sna");
		putCode("Sidamo", null, "sid", "sid");
		putCode("Sign languages", null, "sgn", "sgn");
		putCode("Siksika", null, "bla", "bla");
		putCode("Sindhi", "sd", "snd", "snd");
		putCode("Sinhalese", "si", "sin", "sin");
		putCode("Sino-Tibetan (Other)", null, "sit", "sit");
		putCode("Siouan languages", null, "sio", "sio");
		putCode("Slave (Athapascan)", null, "den", "den");
		putCode("Slavic (Other)", null, "sla", "sla");
		putCode("Slovak", "sk", "slk", "slo");
		putCode("Slovenian", "sl", "slv", "slv");
		putCode("Sogdian", null, "sog", "sog");
		putCode("Somali", "so", "som", "som");
		putCode("Songhai", null, "son", "son");
		putCode("Soninke", null, "snk", "snk");
		putCode("Sorbian languages", null, "wen", "wen");
		putCode("Sotho, Northern", null, "nso", "nso");
		putCode("Sotho, Southern", "st", "sot", "sot");
		putCode("South American Indian (Other)", null, "sai", "sai");
		putCode("Spanish", "es", "spa", "spa");
		putCode("Sukuma", null, "suk", "suk");
		putCode("Sumerian", null, "sux", "sux");
		putCode("Sundanese", "su", "sun", "sun");
		putCode("Susu", null, "sus", "sus");
		putCode("Swahili", "sw", "swa", "swa");
		putCode("Swati", "ss", "ssw", "ssw");
		putCode("Swedish", "sv", "swe", "swe");
		putCode("Syriac", null, "syr", "syr");
		putCode("Tagalog", "tl", "tgl", "tgl");
		putCode("Tahitian", "ty", "tah", "tah");
		putCode("Tai (Other)", null, "tai", "tai");
		putCode("Tajik", "tg", "tgk", "tgk");
		putCode("Tamashek", null, "tmh", "tmh");
		putCode("Tamil", "ta", "tam", "tam");
		putCode("Tatar", "tt", "tat", "tat");
		putCode("Telugu", "te", "tel", "tel");
		putCode("Tereno", null, "ter", "ter");
		putCode("Tetum", null, "tet", "tet");
		putCode("Thai", "th", "tha", "tha");
		putCode("Tibetan", "bo", "bod", "tib");
		putCode("Tigre", null, "tig", "tig");
		putCode("Tigrinya", "ti", "tir", "tir");
		putCode("Timne", null, "tem", "tem");
		putCode("Tiv", null, "tiv", "tiv");
		putCode("Tlingit", null, "tli", "tli");
		putCode("Tok Pisin", null, "tpi", "tpi");
		putCode("Tokelau", null, "tkl", "tkl");
		putCode("Tonga (Nyasa)", null, "tog", "tog");
		putCode("Tonga (Tonga Islands)", "to", "ton", "ton");
		putCode("Tsimshian", null, "tsi", "tsi");
		putCode("Tsonga", "ts", "tso", "tso");
		putCode("Tswana", "tn", "tsn", "tsn");
		putCode("Tumbuka", null, "tum", "tum");
		putCode("Turkish", "tr", "tur", "tur");
		putCode("Turkish, Ottoman (1500-1928)", null, "ota", "ota");
		putCode("Turkmen", "tk", "tuk", "tuk");
		putCode("Tuvalu", null, "tvl", "tvl");
		putCode("Tuvinian", null, "tyv", "tyv");
		putCode("Twi", "tw", "twi", "twi");
		putCode("Ugaritic", null, "uga", "uga");
		putCode("Uighur", "ug", "uig", "uig");
		putCode("Ukrainian", "uk", "ukr", "ukr");
		putCode("Umbundu", null, "umb", "umb");
		putCode("Undetermined", null, DLNAMediaLang.UND, DLNAMediaLang.UND);
		putCode("Urdu", "ur", "urd", "urd");
		putCode("Uzbek", "uz", "uzb", "uzb");
		putCode("Vai", null, "vai", "vai");
		putCode("Venda", null, "ven", "ven");
		putCode("Vietnamese", "vi", "vie", "vie");
		putCode("Volapuk", "vo", "vol", "vol");
		putCode("Votic", null, "vot", "vot");
		putCode("Wakashan languages", null, "wak", "wak");
		putCode("Walamo", null, "wal", "wal");
		putCode("Waray", null, "war", "war");
		putCode("Washo", null, "was", "was");
		putCode("Welsh", "cy", "cym", "wel");
		putCode("Wolof", "wo", "wol", "wol");
		putCode("Xhosa", "xh", "xho", "xho");
		putCode("Yakut", null, "sah", "sah");
		putCode("Yao", null, "yao", "yao");
		putCode("Yapese", null, "yap", "yap");
		putCode("Yiddish", "yi", "yid", "yid");
		putCode("Yoruba", "yo", "yor", "yor");
		putCode("Yupik languages", null, "ypk", "ypk");
		putCode("Zande", null, "znd", "znd");
		putCode("Zapotec", null, "zap", "zap");
		putCode("Zenaga", null, "zen", "zen");
		putCode("Zhuang", "za", "zha", "zha");
		putCode("Zulu", "zu", "zul", "zul");
		putCode("Zuni", null, "zun", "zun");
	}
}
