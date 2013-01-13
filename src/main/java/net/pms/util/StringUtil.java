package net.pms.util;

public class StringUtil {
	/**Appends "&lt;<u>tag</u> " to the StringBuilder. This is a typical HTML/DIDL/XML tag opening.
	 * @param sb String to append the tag beginning to.
	 * @param tag String that represents the tag
	 */
	public static void openTag(StringBuilder sb, String tag) {
		sb.append("&lt;");
		sb.append(tag);
	}

	/**Appends the closing symbol &gt; to the StringBuilder. This is a typical HTML/DIDL/XML tag closing.
	 * @param sb String to append the ending character of a tag.
	 */
	public static void endTag(StringBuilder sb) {
		sb.append("&gt;");
	}

	/**Appends "&lt;/<u>tag</u>&gt;" to the StringBuilder. This is a typical closing HTML/DIDL/XML tag.
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

	/**Does basic transformations between characters and their HTML representation with ampersands.
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

	/**Converts an URL string to it more canonical form
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

}
