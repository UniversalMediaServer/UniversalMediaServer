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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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
			return matcher.group(1).replaceAll("\n    ", "").trim().replaceAll("(?i)<br>", "\n").replaceAll("<.*?>","");
		} else {
			throw new IllegalArgumentException("HTML text not as expected, must have <body> section");
		}
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
	 * Escapes {@link org.apache.lucene} special characters with backslash
	 *
	 * @param s the {@link String} to evaluate
	 * @return The converted String
	 */
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
	 * Escapes special characters with backslashes for FFmpeg subtitles
	 *
	 * @param s the {@link String} to evaluate
	 * @return The converted String
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

	public static String prettifyXML(String xml, int indentWidth) {
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
		} catch (SAXException | IOException | ParserConfigurationException | XPathExpressionException | TransformerException e) {
			LOGGER.warn("Failed to prettify XML document, returning the source document: {}", e.getMessage());
			LOGGER.trace("", e);
			return xml;
		}
	}
}
