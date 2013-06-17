package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class StringUtil {
	private static final int[] MULTIPLIER = new int[] {1, 60, 3600, 24*3600};
	public static final String ASS_TIME_FORMAT = "%01d:%02d:%02.2f";
	public static final String SRT_TIME_FORMAT = "%02d:%02d:%02.3f";
	public static final String SEC_TIME_FORMAT = "%02d:%02d:%02d";
	
	/**
	 * Appends "&lt;<u>tag</u> " to the StringBuilder. This is a typical HTML/DIDL/XML tag opening.
	 * @param sb String to append the tag beginning to.
	 * @param tag String that represents the tag
	 */
	public static void openTag(StringBuilder sb, String tag) {
		sb.append("&lt;");
		sb.append(tag);
	}

	/**
	 * Appends the closing symbol &gt; to the StringBuilder. This is a typical HTML/DIDL/XML tag closing.
	 * @param sb String to append the ending character of a tag.
	 */
	public static void endTag(StringBuilder sb) {
		sb.append("&gt;");
	}

	/**
	 * Appends "&lt;/<u>tag</u>&gt;" to the StringBuilder. This is a typical closing HTML/DIDL/XML tag.
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
			String[] arrs = time.split(":");
			double value, sum = 0;
			for (int i = 0; i < arrs.length; i++) {
				String tmp = arrs[arrs.length - i - 1];
				value = Double.parseDouble(tmp.replace(",", "."));
				sum += value * MULTIPLIER[i];
			}
			return sum;
		}

	}

	/**
	 * Converts time to string.
	 *
	 * @param d time in double.
	 * @param timeFormat Format string e.g. "%02d:%02d:%02d" or use predefined constants
	 * ASS_TIME_FORMAT, SRT_TIME_FORMAT, SEC_TIME_FORMAT.
	 *
	 * @return Converted String.
	 */
	public static String convertTimeToString(double d, String timeFormat) {
		double s = d % 60;
		int h = (int) (d / 3600);
		int m = ((int) (d / 60)) % 60;

		if (timeFormat.equals(SRT_TIME_FORMAT)) {
			return String.format(timeFormat, h, m, s).replaceAll("\\.", ",");
		}

		return String.format(timeFormat, h, m, s);
	}
}
