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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JEditorPane;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class StringUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);
	private static final int[] MULTIPLIER = new int[] {3600, 60, 1};
	public static final String SEC_TIME_FORMAT = "%02d:%02d:%02.0f";
	public static final String DURATION_TIME_FORMAT = "%02d:%02d:%05.2f";
	public static final String DLNA_DURATION_FORMAT = "%01d:%02d:%06.3f";
	public static final String NEWLINE_CHARACTER = System.getProperty("line.separator");
	public static final long KIBI = 1L << 10;
	public static final long MEBI = 1L << 20;
	public static final long GIBI = 1L << 30;
	public static final long TEBI = 1L << 40;
	public static final long PEBI = 1L << 50;
	public static final long EXBI = 1L << 60;
	public static final long KILO = 1000L;
	public static final long MEGA = 1000000L;
	public static final long GIGA = 1000000000L;
	public static final long TERA = 1000000000000L;
	public static final long PETA = 1000000000000000L;
	public static final long EXA  = 1000000000000000000L;

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
		sb.append(' ');
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
	 * @param d the time in seconds.
	 * @param timeFormat Format string e.g. "%02d:%02d:%02f" or use the
	 *            predefined constants {@link #SEC_TIME_FORMAT},
	 *            {@link #DURATION_TIME_FORMAT}.
	 *
	 * @return The converted {@link String}.
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
	 * Converts a duration in seconds to the DIDL-Lite specified duration
	 * format.
	 *
	 * @param duration the duration in seconds.
	 * @return The formatted duration.
	 */
	public static String formatDLNADuration(double duration) {
		double seconds;
		int hours;
		int minutes;
		if (duration < 0) {
			seconds = 0.0;
			hours = 0;
			minutes = 0;
		} else {
			seconds = duration % 60;
			hours = (int) (duration / 3600);
			minutes = ((int) (duration / 60)) % 60;
		}
		if (hours > 99999) {
			// As per DLNA standard
			hours = 99999;
		}
		StringBuilder sb = new StringBuilder();
		try (Formatter formatter = new Formatter(sb, Locale.ROOT)) {
			formatter.format(DLNA_DURATION_FORMAT, hours, minutes, seconds);
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
			int i = t.indexOf('.');
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
	 * Returns the first four digit number that can look like a year from the
	 * specified {@link CharSequence}.
	 *
	 * @param date the {@link CharSequence} from which to extract the year.
	 * @return The extracted year or {@code -1} if no valid year is found.
	 */
	public static int getYear(CharSequence date) {
		if (isBlank(date)) {
			return -1;
		}
		Pattern pattern = Pattern.compile("\\b\\d{4}\\b");
		Matcher matcher = pattern.matcher(date);
		while (matcher.find()) {
			int result = Integer.parseInt(matcher.group());
			if (result > 1600 && result < 2100) {
				return result;
			}
		}
		return -1;
	}

	/**
	 * Extracts the first four digit number that can look like a year from both
	 * {@link CharSequence}s and compares them.
	 *
	 * @param firstDate the first {@link CharSequence} containing a year.
	 * @param secondDate the second {@link CharSequence} containing a year.
	 * @return {@code true} if a year can be extracted from both
	 *         {@link CharSequence}s and they have the same numerical value,
	 *         {@code false} otherwise.
	 */
	public static boolean isSameYear(CharSequence firstDate, CharSequence secondDate) {
		int first = getYear(firstDate);
		if (first < 0) {
			return false;
		}
		int second = getYear(secondDate);
		if (second < 0) {
			return false;
		}
		return first == second;
	}

	/**
	 * Compares the specified {@link String}s for equality. Either
	 * {@link String} can be {@code null}, and they are considered equal if both
	 * are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @return {@code true} if the two {@link String}s are equal, {@code false}
	 *         otherwise.
	 * @throws IllegalArgumentException if an invalid combination of parameters
	 *             is specified.
	 * @throws IndexOutOfBoundsException if {@code fromIdx} or {@code toIdx} is
	 *             positive and outside the bounds of {@code first} or
	 *             {@code second}.
	 */
	public static boolean isEqual(
		String first,
		String second
	) {
		return isEqual(first, second, false, false, false, null, false, 0, -1, -1);
	}

	/**
	 * Compares the specified {@link String}s for equality. Either
	 * {@link String} can be {@code null}, and they are considered equal if both
	 * are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 * @throws IllegalArgumentException if an invalid combination of parameters
	 *             is specified.
	 * @throws IndexOutOfBoundsException if {@code fromIdx} or {@code toIdx} is
	 *             positive and outside the bounds of {@code first} or
	 *             {@code second}.
	 */
	public static boolean isEqual(
		String first,
		String second,
		boolean blankIsNull
	) {
		return isEqual(first, second, blankIsNull, false, false, null, false, 0, -1, -1);
	}

	/**
	 * Compares the specified {@link String}s for equality according to the
	 * rules set by the parameters. Either {@link String} can be {@code null},
	 * and they are considered equal if both are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @param trim {@code true} to {@link String#trim()} both {@link String}s
	 *            before comparison, {@code false} otherwise.
	 * @param ignoreCase {@code true} to convert both {@link String}s to
	 *            lower-case using the specified {@link Locale} before
	 *            comparison, {@code false} otherwise.
	 * @param locale the {@link Locale} to use when converting both
	 *            {@link String}s to lower-case if {@code ignoreCase} is
	 *            {@code true}. Ignored if {@code ignoreCase} is {@code false}.
	 *            If {@code null}, {@link Locale#ROOT} will be used.
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 */
	public static boolean isEqual(
		String first,
		String second,
		boolean blankIsNull,
		boolean trim,
		boolean ignoreCase,
		Locale locale
	) {
		return isEqual(first, second, blankIsNull, trim, ignoreCase, locale, false, 0, -1, -1);
	}

	/**
	 * Compares the specified {@link String}s for equality according to the
	 * rules set by the parameters. Either {@link String} can be {@code null},
	 * and they are considered equal if both are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @param trim {@code true} to {@link String#trim()} both {@link String}s
	 *            before comparison, {@code false} otherwise.
	 * @param ignoreCase {@code true} to convert both {@link String}s to
	 *            lower-case using the specified {@link Locale} before
	 *            comparison, {@code false} otherwise.
	 * @param locale the {@link Locale} to use when converting both
	 *            {@link String}s to lower-case if {@code ignoreCase} is
	 *            {@code true}. Ignored if {@code ignoreCase} is {@code false}.
	 *            If {@code null}, {@link Locale#ROOT} will be used.
	 * @param shortest {@code true} to only compare the length of the shortest
	 *            of the two {@link String}s.
	 * @param minLength the minimum length to compare if {@code shortest} is
	 *            true. If this is zero, an empty {@link String} will equal any
	 *            {@link String}.
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 */
	public static boolean isEqual(
		String first,
		String second,
		boolean blankIsNull,
		boolean trim,
		boolean ignoreCase,
		Locale locale,
		boolean shortest,
		int minLength
	) {
		return isEqual(first, second, blankIsNull, trim, ignoreCase, locale, shortest, minLength, -1, -1);
	}

	/**
	 * Compares the specified {@link String}s for equality according to the
	 * rules set by the parameters. Either {@link String} can be {@code null},
	 * and they are considered equal if both are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @param ignoreCase {@code true} to convert both {@link String}s to
	 *            lower-case using the specified {@link Locale} before
	 *            comparison, {@code false} otherwise.
	 * @param locale the {@link Locale} to use when converting both
	 *            {@link String}s to lower-case if {@code ignoreCase} is
	 *            {@code true}. Ignored if {@code ignoreCase} is {@code false}.
	 *            If {@code null}, {@link Locale#ROOT} will be used.
	 * @param fromIdx compare only from the character of this index.
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 * @throws IllegalArgumentException {@code toIdx} is positive and is smaller
	 *             than {@code fromIdx}.
	 * @throws IndexOutOfBoundsException if {@code fromIdx} or {@code toIdx} is
	 *             positive and outside the bounds of {@code first} or
	 *             {@code second}.
	 */
	public static boolean isEqualFrom(
		String first,
		String second,
		boolean blankIsNull,
		boolean ignoreCase,
		Locale locale,
		int fromIdx
	) {
		return isEqual(first, second, blankIsNull, false, ignoreCase, locale, false, 0, fromIdx, -1);
	}

	/**
	 * Compares the specified {@link String}s for equality according to the
	 * rules set by the parameters. Either {@link String} can be {@code null},
	 * and they are considered equal if both are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @param ignoreCase {@code true} to convert both {@link String}s to
	 *            lower-case using the specified {@link Locale} before
	 *            comparison, {@code false} otherwise.
	 * @param locale the {@link Locale} to use when converting both
	 *            {@link String}s to lower-case if {@code ignoreCase} is
	 *            {@code true}. Ignored if {@code ignoreCase} is {@code false}.
	 *            If {@code null}, {@link Locale#ROOT} will be used.
	 * @param toIdx compare only to (not including) the character of this index.
	 *            To compare to the end of the {@link String}, use {@code -1} or
	 *            the index position after the last character (the same as the
	 *            length).
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 * @throws IllegalArgumentException {@code toIdx} is positive and is smaller
	 *             than {@code fromIdx}.
	 * @throws IndexOutOfBoundsException if {@code fromIdx} or {@code toIdx} is
	 *             positive and outside the bounds of {@code first} or
	 *             {@code second}.
	 */
	public static boolean isEqualTo(
		String first,
		String second,
		boolean blankIsNull,
		boolean ignoreCase,
		Locale locale,
		int toIdx
	) {
		return isEqual(first, second, blankIsNull, false, ignoreCase, locale, false, 0, -1, toIdx);
	}


	/**
	 * Compares the specified {@link String}s for equality according to the
	 * rules set by the parameters. Either {@link String} can be {@code null},
	 * and they are considered equal if both are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @param ignoreCase {@code true} to convert both {@link String}s to
	 *            lower-case using the specified {@link Locale} before
	 *            comparison, {@code false} otherwise.
	 * @param locale the {@link Locale} to use when converting both
	 *            {@link String}s to lower-case if {@code ignoreCase} is
	 *            {@code true}. Ignored if {@code ignoreCase} is {@code false}.
	 *            If {@code null}, {@link Locale#ROOT} will be used.
	 * @param fromIdx compare only from the character of this index.
	 * @param toIdx compare only to (not including) the character of this index.
	 *            To compare to the end of the {@link String}, use {@code -1} or
	 *            the index position after the last character (the same as the
	 *            length).
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 * @throws IllegalArgumentException {@code toIdx} is positive and is smaller
	 *             than {@code fromIdx}.
	 * @throws IndexOutOfBoundsException if {@code fromIdx} or {@code toIdx} is
	 *             positive and outside the bounds of {@code first} or
	 *             {@code second}.
	 */
	public static boolean isEqual(
		String first,
		String second,
		boolean blankIsNull,
		boolean ignoreCase,
		Locale locale,
		int fromIdx,
		int toIdx
	) {
		return isEqual(first, second, blankIsNull, false, ignoreCase, locale, false, 0, fromIdx, toIdx);
	}

	/**
	 * Compares the specified {@link String}s for equality according to the
	 * rules set by the parameters. Either {@link String} can be {@code null},
	 * and they are considered equal if both are {@code null}.
	 *
	 * @param first the first {@link String} to compare.
	 * @param second the second {@link String} to compare.
	 * @param blankIsNull if {@code true} a blank {@link String} will be equal
	 *            to any other blank {@link String} or {@code null}.
	 * @param trim {@code true} to {@link String#trim()} both {@link String}s
	 *            before comparison, {@code false} otherwise. Cannot be used
	 *            together with {@code fromIdx} or {@code toIdx}.
	 * @param ignoreCase {@code true} to convert both {@link String}s to
	 *            lower-case using the specified {@link Locale} before
	 *            comparison, {@code false} otherwise.
	 * @param locale the {@link Locale} to use when converting both
	 *            {@link String}s to lower-case if {@code ignoreCase} is
	 *            {@code true}. Ignored if {@code ignoreCase} is {@code false}.
	 *            If {@code null}, {@link Locale#ROOT} will be used.
	 * @param shortest {@code true} to only compare the length of the shortest
	 *            of the two {@link String}s. Cannot be used together with
	 *            {@code fromIdx} or {@code toIdx}.
	 * @param minLength the minimum length to compare if {@code shortest} is
	 *            true. If this is zero, an empty {@link String} will equal any
	 *            {@link String}.
	 * @param fromIdx compare only from the character of this index. Cannot be
	 *            used together with {@code trim} or {@code shortest}.
	 * @param toIdx compare only to (not including) the character of this index.
	 *            To compare to the end of the {@link String}, use {@code -1} or
	 *            the index position after the last character (the same as the
	 *            length). Cannot be used together with {@code trim} or
	 *            {@code shortest}.
	 * @return {@code true} if the two {@link String}s are equal according to
	 *         the rules set by the parameters, {@code false} otherwise.
	 * @throws IllegalArgumentException if an invalid combination of parameters
	 *             is specified.
	 * @throws IndexOutOfBoundsException if {@code fromIdx} or {@code toIdx} is
	 *             positive and outside the bounds of {@code first} or
	 *             {@code second}.
	 */
	protected static boolean isEqual(
		String first,
		String second,
		boolean blankIsNull,
		boolean trim,
		boolean ignoreCase,
		Locale locale,
		boolean shortest,
		int minLength,
		int fromIdx,
		int toIdx
	) {
		if ((trim || shortest) && (fromIdx >= 0 || toIdx >= 0)) {
			throw new IllegalArgumentException("trim or shortest and index range can't be used together");
		}
		if (blankIsNull) {
			if (first == null) {
				first = "";
			}
			if (second == null) {
				second = "";
			}
		} else {
			if (first == null || second == null) {
				return first == null && second == null;
			}
		}
		// No null after this point

		if (trim) {
			first = first.trim();
			second = second.trim();
		}

		if (ignoreCase) {
			if (locale == null) {
				locale = Locale.ROOT;
			}
			first = first.toLowerCase(locale);
			second = second.toLowerCase(locale);
		}

		if (shortest) {
			if (first.length() != second.length()) {
				int shortestIdx = Math.max(Math.min(first.length(), second.length()), minLength);
				first = first.substring(0, Math.min(shortestIdx, first.length()));
				second = second.substring(0, Math.min(shortestIdx, second.length()));
			}
		} else if (fromIdx >= 0 || toIdx >= 0) {
			if (fromIdx == toIdx) {
				return true;
			}
			if (fromIdx > toIdx && toIdx >= 0) {
				throw new IllegalArgumentException("fromIdx (" + fromIdx + ") > toIdx (" + toIdx + ")");
			}
			if (fromIdx >= first.length() || fromIdx >= second.length()) {
				throw new IndexOutOfBoundsException(
					"fromIdx=" + fromIdx + ", first length=" + first.length() + ", second length=" + second.length()
				);
			}
			if (toIdx > first.length() || toIdx > second.length()) {
				throw new IndexOutOfBoundsException(
					"toIdx=" + fromIdx + ", first length=" + first.length() + ", second length=" + second.length()
				);
			}
			if (fromIdx < 0) {
				fromIdx = 0;
			}
			if (toIdx < 0) {
				first = first.substring(fromIdx);
				second = second.substring(fromIdx);
			} else {
				first = first.substring(fromIdx, toIdx);
				second = second.substring(fromIdx, toIdx);
			}
		}

		if (blankIsNull && (isBlank(first) || isBlank(second))) {
			return isBlank(first) && isBlank(second);
		}

		return first.equals(second);
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
	 * Fill a string in a unicode safe way.
	 *
	 * @param subString The <code>String</code> to be filled with
	 * @param count The number of times to repeat the <code>String</code>
	 * @return The filled string
	 */
	public static String fillString(String subString, int count) {
		StringBuilder sb = new StringBuilder(subString.length() * count);
		for (int i = 0; i < count; i++) {
			sb.append(subString);
		}
		return sb.toString();
	}

	/**
	 * Fill a string in a unicode safe way provided that the char array contains
	 * a valid unicode sequence.
	 *
	 * @param chars The <code>char[]</code> to be filled with
	 * @param count The number of times to repeat the <code>char[]</code>
	 * @return The filled string
	 */
	public static String fillString(char[] chars, int count) {
		StringBuilder sb = new StringBuilder(chars.length * count);
		for (int i = 0; i < count; i++) {
			sb.append(chars);
		}
		return sb.toString();
	}

	/**
	 * Fill a string in a unicode safe way. 8 bit (&lt; 256) code points
	 * equals ISO 8859-1 codes.
	 *
	 * @param codePoint The unicode code point to be filled with
	 * @param count The number of times to repeat the unicode code point
	 * @return The filled string
	 */
	public static String fillString(int codePoint, int count) {
		return fillString(Character.toChars(codePoint), count);
	}

	/**
	 * Returns the <code>body</code> of a HTML {@link String} formatted by
	 * {@link HTMLEditorKit} as typically used by {@link JEditorPane} and
	 * {@link JTextPane} stripped for tags, newline, indentation and with
	 * <code>&lt;br&gt;</code> tags converted to newline.<br>
	 * <br>
	 * <strong>Note: This is not a universal or sophisticated HTML stripping
	 * method, but is purpose built for these circumstances.</strong>
	 *
	 * @param html the HTML formatted text as described above
	 * @return The "deHTMLified" text
	 */
	public static String stripHTML(String html) {
		Pattern pattern = Pattern.compile("<body>(.*)</body>", Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
		Matcher matcher = pattern.matcher(html);
		if (matcher.find()) {
			return matcher.group(1).replaceAll("\n    ", "").trim().replaceAll("(?i)<br>", "\n").replaceAll("<.*?>", "");
		}
		throw new IllegalArgumentException("HTML text not as expected, must have <body> section");
	}

	/**
	 * Convenience method to check if a {@link String} is not <code>null</code>
	 * and contains anything other than whitespace.
	 *
	 * @param s the {@link String} to evaluate
	 * @return The verdict
	 */
	public static boolean hasValue(String s) {
		return s != null && !s.trim().isEmpty();
	}

	/**
	 * Escapes {@link org.apache.lucene} special characters with backslash.
	 *
	 * @param s the {@link String} to evaluate.
	 * @return The converted String.
	 */
	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	public static String luceneEscape(final String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
				case '+':
				case '-':
				case '&':
				case '|':
				case '!':
				case '(':
				case ')':
				case '{':
				case '}':
				case '[':
				case ']':
				case '^':
				case '\"':
				case '~':
				case '*':
				case '?':
				case ':':
				case '\\':
				case '/':
					sb.append("\\");
				default:
					sb.append(ch);
			}
		}

		return sb.toString();
	}

	/**
	 * Escapes special characters with backslashes for FFmpeg subtitles.
	 *
	 * @param s the {@link String} to evaluate.
	 * @return The converted String.
	 */
	public static String ffmpegEscape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			switch (ch) {
				case '\'':
					sb.append("\\\\\\'");
					break;
				case ':':
					sb.append("\\\\:");
					break;
				case '\\':
					sb.append("/");
					break;
				case ']':
				case '[':
				case ',':
				case ';':
					sb.append("\\");
				default:
					sb.append(ch);
			}
		}

		return sb.toString();
	}

	/**
	 * Formats a XML string to be easier to read with newlines and indentations.
	 *
	 * @param xml the {@link String} to "prettify".
	 * @param indentWidth the width of one indentation in number of characters.
	 * @return The "prettified" {@link String}.
	 * @throws SAXException If a parsing error occurs.
	 * @throws ParserConfigurationException If a parsing error occurs.
	 * @throws XPathExpressionException If a parsing error occurs.
	 * @throws TransformerException If a parsing error occurs.
	 */
	public static String prettifyXML(
		String xml,
		int indentWidth
	) throws SAXException, ParserConfigurationException, XPathExpressionException, TransformerException {
		try {
			// Turn XML string into a document
			Document xmlDocument =
				DocumentBuilderFactory.newInstance().
				newDocumentBuilder().
				parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

			// Remove whitespaces outside tags
			xmlDocument.normalize();
			XPath xPath = XPathFactory.newInstance().newXPath();
			NodeList nodeList = (NodeList) xPath.evaluate(
				"//text()[normalize-space()='']",
				xmlDocument,
				XPathConstants.NODESET
			);

			for (int i = 0; i < nodeList.getLength(); ++i) {
				Node node = nodeList.item(i);
				node.getParentNode().removeChild(node);
			}

			// Setup pretty print options
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute("indent-number", indentWidth);
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// Return pretty print XML string
			StringWriter stringWriter = new StringWriter();
			transformer.transform(new DOMSource(xmlDocument), new StreamResult(stringWriter));
			return stringWriter.toString();
		} catch (IOException e) {
			LOGGER.warn("Failed to read XML document, returning the source document: {}", e.getMessage());
			LOGGER.trace("", e);
			return xml;
		}
	}

	/**
	 * Creates a "readable" string by combining the strings in {@code strings}
	 * while inserting "{@code ,}" and "{@code and}" as appropriate. The
	 * resulting {@link String} is in the form
	 * "{@code element 1, element2 and element3}".
	 *
	 * @param strings the {@link Collection} of {@link String} to combine.
	 * @return The combined "readable" {@link String}.
	 */
	public static String createReadableCombinedString(Collection<String> strings) {
		return createReadableCombinedString(strings, null, null);
	}

	/**
	 * Creates a "readable" string by combining the strings in {@code strings}
	 * while inserting {@code separator} and {@code lastSeparator} as
	 * appropriate. The resulting {@link String} is in the form
	 * "{@code element 1<separator> element2 <lastSeparator> element3}".
	 *
	 * @param strings the {@link Collection} of {@link String} to combine.
	 * @param separator the "normal" separator used everywhere except between
	 *            the last two elements.
	 * @param lastSeparator the separator used between the last two elements.
	 * @return The combined "readable" {@link String}.
	 */
	public static String createReadableCombinedString(Collection<String> strings, String separator, String lastSeparator) {
		if (strings == null || strings.isEmpty()) {
			return "";
		}
		return createReadableCombinedString(strings.toArray(new String[strings.size()]), separator, lastSeparator);
	}

	/**
	 * Creates a "readable" string by combining the strings in {@code strings}
	 * while inserting "{@code ,}" and "{@code and}" as appropriate. The
	 * resulting {@link String} is in the form
	 * "{@code element 1, element2 and element3}".
	 *
	 * @param strings the array of {@link String} to combine.
	 * @return The combined "readable" {@link String}.
	 */
	public static String createReadableCombinedString(String[] strings) {
		return createReadableCombinedString(strings, null, null);
	}

	/**
	 * Creates a "readable" string by combining the strings in {@code strings}
	 * while inserting {@code separator} and {@code lastSeparator} as
	 * appropriate. The resulting {@link String} is in the form
	 * "{@code element 1<separator> element2 <lastSeparator> element3}".
	 *
	 * @param strings the array of {@link String} to combine.
	 * @param separator the "normal" separator used everywhere except between
	 *            the last two elements.
	 * @param lastSeparator the separator used between the last two elements.
	 * @return The combined "readable" {@link String}.
	 */
	public static String createReadableCombinedString(String[] strings, String separator, String lastSeparator) {
		if (strings == null || strings.length == 0) {
			return "";
		}
		if (separator == null) {
			separator = ", ";
		} else {
			separator += " ";
		}
		if (lastSeparator == null) {
			lastSeparator = " and ";
		} else if (!isBlank(lastSeparator)) {
			if (!lastSeparator.substring(0, 1).equals(" ")) {
				lastSeparator = " " + lastSeparator;
			}
			if (!lastSeparator.substring(lastSeparator.length() - 1).equals(" ")) {
				lastSeparator += " ";
			}
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < strings.length; i++) {
			if (i > 0) {
				if (i == strings.length) {
					sb.append(lastSeparator);
				} else {
					sb.append(separator);
				}
			}
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	/**
	 * Formats bytes into a rounded {@link String} representation in either
	 * binary/power of 2 or SI notation using {@link Locale#ROOT}.
	 *
	 * @param bytes the value to format.
	 * @param binary whether the representation should be binary/power of 2 or
	 *            SI/metric.
	 * @return The formatted byte value and unit.
	 */
	public static String formatBytes(long bytes, boolean binary) {
		return formatBytes(bytes, binary, Locale.ROOT);
	}

	/**
	 * Formats bytes into a rounded {@link String} representation in either
	 * binary/power of 2 or SI notation.
	 *
	 * @param bytes the value to format.
	 * @param binary whether the representation should be binary/power of 2 or
	 *            SI/metric.
	 * @param locale the {@link Locale} to use when formatting.
	 * @return The formatted byte value and unit.
	 */
	public static String formatBytes(long bytes, boolean binary, Locale locale) {
		if ((binary && bytes < 1L << 10) || bytes < KILO) {
			return String.format("%d %s", bytes, bytes == 1L ? "byte" : "bytes");
		}

		long divisor;
		String unit;
		if ((binary && bytes < MEBI) || bytes < MEGA) { // kibi/kilo
			divisor = binary ? KIBI : KILO;
			unit = binary ? "KiB" : "kB";
		} else if ((binary && bytes < GIBI) || bytes < GIGA) { // mebi/mega
			divisor = binary ? MEBI : MEGA;
			unit = binary ? "MiB" : "MB";
		} else if ((binary && bytes < TEBI) || bytes < TERA) { // gibi/giga
			divisor = binary ? GIBI : GIGA;
			unit = binary ? "GiB" : "GB";
		} else if ((binary && bytes < PEBI) || bytes < PETA) { // tebi/tera
			divisor = binary ? TEBI : TERA;
			unit = binary ? "TiB" : "TB";
		} else if ((binary && bytes < EXBI) || bytes < EXA) { // pebi/peta
			divisor = binary ? PEBI : PETA;
			unit = binary ? "PiB" : "PB";
		} else { // exbi/exa
			divisor = binary ? EXBI : EXA;
			unit = binary ? "EiB" : "EB";
		}
		if (bytes % divisor == 0) {
			return String.format(locale, "%d %s", bytes / divisor, unit);
		}
		return String.format(locale, "%.1f %s", (double) bytes / divisor, unit);
	}

	/**
	 * An enum representing letter cases.
	 */
	public static enum LetterCase {

		/** Upper-case, uppercase, capital or majuscule */
		UPPER,

		/** Lower-case, lowercase or minuscule */
		LOWER
	}
}
