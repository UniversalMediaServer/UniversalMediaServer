package net.pms.util;

import java.util.StringTokenizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StringUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(StringUtil.class);
	
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
	 * Converts time to string .
	 * @param d time in double.
	 * @param inSeconds true if result have to be rounded in seconds (format 00:00:00), 
	 * false if result in milliseconds (format 00:00:00.000).
	 * 
	 * @return Converted String.
	 */
	 public static String convertTimeToString(double d, boolean inSeconds) {
		 String out;
		 int s = 0;
		 double	ms = 0;
		 
		 if (inSeconds) {
			 s = (int) (d % 60);
		 } else  {
			 ms = d % 60;
		 }
		 
		 int h = (int) (d / 3600);
		 int m = ((int) (d / 60)) % 60;
		 
		 if (inSeconds) {
			 out = String.format("%02d:%02d:%02d", h, m, s);
		 } else  {
			 out = String.format("%02d:%02d:%02.3f", h, m, ms);
		 }
		 
		 return out;
	 }

	/**
	* Converts string in time format to double.
	* @param time in string time format 00:00:00.000.
	* @return Time in double.
	*/
	 public static Double convertStringToTime(String time) {
		 if (time == null) {
			 return null;
		 }

		 StringTokenizer st = new StringTokenizer(time, ":");

		 try {
			 int h = Integer.parseInt(st.nextToken());
			 int m = Integer.parseInt(st.nextToken());
			 double s = Double.parseDouble(st.nextToken());
			 return h * 3600 + m * 60 + s;
		 } catch (NumberFormatException nfe) {
			 LOGGER.debug("Failed to convert \"" + time + "\"");
		 }

		 return null;
	 }
}
