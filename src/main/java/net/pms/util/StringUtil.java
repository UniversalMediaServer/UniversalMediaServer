package net.pms.util;

import java.io.IOException;
import java.io.Writer;
import java.util.Formatter;
import java.util.Locale;
import static org.apache.commons.lang3.StringUtils.isBlank;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

public class StringUtil {
	private static final int[] MULTIPLIER = new int[] {3600, 60, 1};
	public static final String SEC_TIME_FORMAT = "%02d:%02d:%02.0f";
	public static final String DURATION_TIME_FORMAT = "%02d:%02d:%05.2f";

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
	 * Does basic transformations between characters and their HTML representation with ampersands.
	 *
	 * @param s String to be encoded
	 * @return Encoded String
	 */
	public static String encodeXML(String s) {
		s = s.replace("&", "&amp;");
		s = s.replace("<", "&lt;");
		s = s.replace(">", "&gt;");
		s = s.replace("\"", "&quot;");
		s = s.replace("'", "&apos;");
		s = s.replace("&", "&amp;");

		return s;
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
	 * Removes leading zeros up to the nth char of an hh:mm:ss time string.
	 *
	 * @param t time string.
	 * @param n position to stop checking
	 *
	 * @return the Shortened String.
	 */
	public static String shortTime(String t, int n) {
		n = n < 8 ? n : 8;
		if (!isBlank(t)) {
			int i = t.indexOf(".");
			// Throw out the decimal portion, if any
			if (i > -1) {
				t = t.substring(0, i);
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
}
