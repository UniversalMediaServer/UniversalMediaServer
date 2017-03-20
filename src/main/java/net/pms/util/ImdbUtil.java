package net.pms.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;

public class ImdbUtil {
	private static final String HASH_REG = "_os([^_]+)_";
	private static final String IMDB_REG = "_imdb([^_]+)_";

	public static String cleanName(String str) {
		return str.replaceAll(IMDB_REG, "").replaceAll(HASH_REG, "");
	}

	public static String extractOSHash(File file) {
		return extract(file, HASH_REG);
	}

	public static String extractImdb(File file) {
		String ret = extract(file, IMDB_REG);
		// Opensubtitles requires IMDb ID to be a number only
		if (!StringUtils.isEmpty(ret) && ret.startsWith("tt") && ret.length() > 2) {
			ret = ret.substring(2);
		}
		return ret;
	}

	private static String extract(File f, String reg) {
		String fName = f.getAbsolutePath();
		Pattern re = Pattern.compile(reg);
		Matcher m = re.matcher(fName);
		String ret = "";
		while (m.find()) {
			ret = m.group(1);
		}
		return ret;
	}

	public static String ensureTT(String s) {
		return (s.startsWith("tt") ? s : "tt" + s);
	}
}
