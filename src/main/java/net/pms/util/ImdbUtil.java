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

	public static String extractOSHashFromFileName(File file) {
		return extract(file, HASH_REG);
	}

	public static String extractImdbIdFromFileName(File file) {
		String ret = extract(file, IMDB_REG);
		// Opensubtitles requires IMDb ID to be a number only
		if (!StringUtils.isEmpty(ret) && ret.startsWith("tt") && ret.length() > 2) {
			ret = ret.substring(2);
		}
		return ret;
	}

	private static String extract(File file, String regex) {
		String fileName = file.getAbsolutePath();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(fileName);
		String result = "";
		while (matcher.find()) {
			result = matcher.group(1);
		}
		return result;
	}

	public static String ensureTT(String s) {
		return (s.startsWith("tt") ? s : "tt" + s);
	}
}
