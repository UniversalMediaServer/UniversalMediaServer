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

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Pattern;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);
	private static final int[] MULTIPLIER = new int[] {3600, 60, 1};
	public static final String SEC_TIME_FORMAT = "%02d:%02d:%02.0f";
	public static final String DURATION_TIME_FORMAT = "%02d:%02d:%05.2f";
	public static final String NEWLINE_CHARACTER = System.getProperty("line.separator");

	/**
	 * Appends "&lt;<u>tag</u> " to the StringBuilder. This is a typical HTML/DIDL/XML tag opening.
	 *
	 * @param sb String to append the tag beginning to.
	 * @param tag String that represents the tag
	 */
	public static void openTag(StringBuilder sb, String tag) {
		sb.append("&lt;");
		sb.append(tag);
	}

	/**
	 * Appends the closing symbol &gt; to the StringBuilder. This is a typical HTML/DIDL/XML tag closing.
	 *
	 * @param sb String to append the ending character of a tag.
	 */
	public static void endTag(StringBuilder sb) {
		sb.append("&gt;");
	}

	/**
	 * Appends "&lt;/<u>tag</u>&gt;" to the StringBuilder. This is a typical closing HTML/DIDL/XML tag.
	 *
	 * @param sb
	 * @param tag
	 */
	public static void closeTag(StringBuilder sb, String tag) {
		sb.append("&lt;/");
		sb.append(tag);
		sb.append("&gt;");
	}

	public static void addAttribute(StringBuilder sb, String attribute, Object value) {
		sb.append(" ");
		sb.append(attribute);
		sb.append("=\"");
		sb.append(value);
		sb.append("\"");
	}

	public static void addXMLTagAndAttribute(StringBuilder sb, String tag, Object value) {
		sb.append("&lt;");
		sb.append(tag);
		sb.append("&gt;");
		sb.append(value);
		sb.append("&lt;/");
		sb.append(tag);
		sb.append("&gt;");
	}

	/**
	 * Does double transformations between &<> characters and their XML representation with ampersands.
	 *
	 * @param s String to be encoded
	 * @return Encoded String
	 */
	public static String encodeXML(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		/* Skip encoding/escaping ' and " for compatibility with some renderers
		 * This might need to be made into a renderer option if some renderers require them to be encoded
		 * s = s.replace("\"", "&quot;");
		 * s = s.replace("'", "&apos;");
		 */

		// The second encoding/escaping of & is not a bug, it's what effectively adds the second layer of encoding/escaping
		s = s.replace("&", "&amp;");
		return s;
	}

	/**
	 * Removes xml character representations.
	 *
	 * @param s String to be cleaned
	 * @return Encoded String
	 */
	public static String unEncodeXML(String s) {
		// Note: ampersand substitution must be first in order to undo double transformations
		// TODO: support ' and " if/when required, see encodeXML() above
		return s.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">");
	}

	/**
	 * Converts a URL string to a more canonical form
	 *
	 * @param url String to be converted
	 * @return Converted String.
	 */
	public static String convertURLToFileName(String url) {
		url = url.replace('/', '\u00b5');
		url = url.replace('\\', '\u00b5');
		url = url.replace(':', '\u00b5');
		url = url.replace('?', '\u00b5');
		url = url.replace('*', '\u00b5');
		url = url.replace('|', '\u00b5');
		url = url.replace('<', '\u00b5');
		url = url.replace('>', '\u00b5');
		return url;
	}

	/**
	 * Parse as double, or if it's not just one number, handles {hour}:{minute}:{seconds}
	 *
	 * @param time
	 * @return
	 */
	public static double convertStringToTime(String time) throws IllegalArgumentException {
		if (isBlank(time)) {
			throw new IllegalArgumentException("time String should not be blank.");
		}

		try {
			return Double.parseDouble(time);
		} catch (NumberFormatException e) {
			String[] arguments = time.split(":");
			double sum = 0;
			int i = 0;
			for (String argument : arguments) {
				sum += Double.parseDouble(argument.replace(",", ".")) * MULTIPLIER[i];
				i++;
			}

			return sum;
		}
	}

	/**
	 * Converts time to string.
	 *
	 * @param d time in double.
	 * @param timeFormat Format string e.g. "%02d:%02d:%02f" or use predefined constants
	 * SEC_TIME_FORMAT, DURATION_TIME_FORMAT.
	 *
	 * @return Converted String.
	 */
	public static String convertTimeToString(double d, String timeFormat) {
		StringBuilder sb = new StringBuilder();
		try (Formatter formatter = new Formatter(sb, Locale.US)) {
			double s = d % 60;
			int h = (int) (d / 3600);
			int m = ((int) (d / 60)) % 60;
			formatter.format(timeFormat, h, m, s);
		}

		return sb.toString();
	}

	/**
	 * Removes leading zeros up to the nth char of an hh:mm:ss time string,
	 * normalizing it first if necessary.
	 *
	 * @param t time string.
	 * @param n position to stop checking
	 *
	 * @return the Shortened String.
	 */
	public static String shortTime(String t, int n) {
		n = n < 8 ? n : 8;
		if (!isBlank(t)) {
			if (t.startsWith("NOT_IMPLEMENTED")) {
				return t.length() > 15 ? t.substring(15) : " ";
			}
			int i = t.indexOf(".");
			// Throw out the decimal portion, if any
			if (i > -1) {
				t = t.substring(0, i);
			}
			int l = t.length();
			// Normalize if necessary
			if (l < 8) {
				t = "00:00:00".substring(0, 8 - l) + t;
			} else if (l > 8) {
				t = t.substring(l - 8);
			}
			for (i = 0; i < n; i++) {
				if (t.charAt(i) != "00:00:00".charAt(i)) {
					break;
				}
			}
			return t.substring(i);
		}
		return "00:00:00".substring(n);
	}

	public static boolean isZeroTime(String t) {
		return isBlank(t) || "00:00:00.000".contains(t);
	}

	/**
	 * A unicode unescaper that translates unicode escapes, e.g. '\u005c', while leaving
	 * intact any  sequences that can't be interpreted as escaped unicode.
	 */
	public static class LaxUnicodeUnescaper extends UnicodeUnescaper {
		@Override
		public int translate(CharSequence input, int index, Writer out) throws IOException {
			try {
				return super.translate(input, index, out);
			} catch (IllegalArgumentException e) {
				// Leave it alone and continue
			}
			return 0;
		}
	}

	/**
	 * Interprets color strings of these forms:
	 *    integer: r,g,b[,a]          - e.g. '125,184,47' or '125,184,47,128'
	 *    hex: #[aa]rrggbb            - e.g. '#04DCF9' or '#8004DCF9'
	 *    java.awt.Color named color  - e.g. 'blue' or 'LIGHT_GRAY'
	 */
	public static Color parseColor(String colorString) {
		try {
			colorString = colorString.trim();
			if (colorString.contains(",")) {
				// Integer r,g,b[,a]
				String[] colorElements = colorString.split("\\s*,\\s*");
				int r = Integer.parseInt(colorElements[0]);
				int g = Integer.parseInt(colorElements[1]);
				int b = Integer.parseInt(colorElements[2]);
				int a = colorElements.length > 3 ? Integer.parseInt(colorElements[3]) : 255;
				return new Color(r, g, b, a);

			} else if (colorString.charAt(0) == '#') {
				// Hex #[aa]rrggbb
				long argb = Long.parseLong(colorString.substring(1), 16);
				return new Color((int)argb, colorString.length() > 8);

			} else {
				// java.awt.Color named color
				return (Color) Color.class.getField(colorString).get(null);
			}
		} catch (Exception e) {
		}
		LOGGER.warn("Unknown color '{}'. Color string must be rgb (integer R,G,B[,A] or hex #[AA]RRGGBB) or a standard java.awt.Color name", colorString);
		return null;
	}

	/**
	 * Returns the argument string surrounded with quotes if it contains a space,
	 * otherwise returns the string as is.
	 *
	 * @param arg The argument string
	 * @return The string, optionally in quotes.
	 */
	public static String quoteArg(String arg) {
		if (arg != null && arg.indexOf(' ') > -1) {
			return "\"" + arg + "\"";
		}

		return arg;
	}

	/**
	 * This method is written for matching renderer HTTP user agent and UPnP
	 * details, but can be used for anything that want to use the same matching
	 * rules. The rules are: <ul>
	 * <li>Matching is case sensitive unless the search string starts with
	 * <code>(?i)</code></li>
	 * <li>Normal regex rules apply, but dot matches all including newline</li>
	 * <li>If leading or trailing spaces are to be included, the regex must be
	 * double quoted</li>
	 * <li>Double quotes are stripped only if they exist in the first and last
	 * position of the regex, flags excluded
	 * </ul>
	 * Examples of valid syntax:
	 * <pre>
	 * Case sensitive search term
	 * (?i)caSE inSensitive search term
	 * "Double quoted search term with trailing spaces   "
	 * (?i)"caSE inSensitive double quoted search term"
	 * </pre>
	 * @param searchTerm the regex string following the above rules
	 * @param content the string to search
	 * @return The result of the regex search
	 */
	public static boolean matchConfigurationRegEx(String searchTerm, String content) {
		if (content == null) {
			throw new IllegalArgumentException("content cannot be null");
		}
		if (searchTerm == null) {
			return false;
		}

		// Strip case insensitivity flag
		boolean caseInsensitive = false;
		if (searchTerm.startsWith("(?i)")) {
			caseInsensitive = true;
			searchTerm = searchTerm.substring(4);
		}

		// Strip double quotes
		if (searchTerm.startsWith("\"") && searchTerm.endsWith("\"")) {
			searchTerm = searchTerm.substring(1, searchTerm.length() - 1);
		}

		if (StringUtils.isBlank(searchTerm)) {
			return false;
		}

		Pattern pattern = Pattern.compile(searchTerm, caseInsensitive ? Pattern.DOTALL + Pattern.CASE_INSENSITIVE : Pattern.DOTALL);
		return pattern.matcher(content).find();
	}

}
