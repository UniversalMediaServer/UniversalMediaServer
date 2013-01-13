package net.pms.xmlwise;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.IOException;
import java.io.StringReader;
import java.io.File;

/**
 * Xmlwise convenience methods for loading xml documents and render them into
 * XmlElement trees.
 *
 * @author Christoffer Lerno
 */
public class Xmlwise {
	private Xmlwise() {
	}

	/**
	 * Loads an XML document ignoring DTD-validation.
	 *
	 * @param file the file to read from.
	 * @return an XML document.
	 * @throws IOException if we fail to load the file.
	 * @throws XmlParseException if there is a problem parsing the xml in the file.
	 */
	public static Document loadDocument(File file) throws IOException, XmlParseException {
		return loadDocument(file, false, false);
	}

	/**
	 * Loads an XML document.
	 *
	 * @param file the file to read from.
	 * @param validate if we should validate the document or not.
	 * @param loadExternalDTD true to allow loading of external dtds.
	 * @return an XML document.
	 * @throws IOException if we fail to load the file.
	 * @throws XmlParseException if there is a problem parsing the xml in the file.
	 */
	public static Document loadDocument(File file, boolean validate, boolean loadExternalDTD) throws IOException, XmlParseException {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",
				loadExternalDTD);
			documentBuilderFactory.setValidating(validate);
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			return builder.parse(file);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new XmlParseException(e);
		}
	}

	/**
	 * Creates a DOM Document from the specified XML string, ignoring DTD-validation.
	 *
	 * @param xml a valid XML document, ie the String can't be null or empty
	 * @param validate if we should validate the document or not.
	 * @param loadExternalDTD true to allow loading of external dtds.
	 * @return the <code>Document</code> object for the specified string.
	 * @throws XmlParseException if we fail to parse the XML.
	 */
	public static Document createDocument(String xml, boolean validate, boolean loadExternalDTD) throws XmlParseException {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd",
				loadExternalDTD);
			documentBuilderFactory.setValidating(validate);
			DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
			return builder.parse(new InputSource(new StringReader(xml)));
		} catch (Exception e) {
			throw new XmlParseException(e);
		}
	}

	/**
	 * Creates a DOM Document from the specified XML string, ignoring DTD-validation.
	 *
	 * @param xml a valid XML document, ie the String can't be null or empty
	 * @return the <code>Document</code> object for the specified string.
	 * @throws XmlParseException if we fail to parse the XML.
	 */
	public static Document createDocument(String xml) throws XmlParseException {
		return createDocument(xml, false, false);
	}

	/**
	 * Escapes a string to be used in an xml document.
	 * <p>
	 * The following replacements are made:
	 * <p>
	 * <table>
	 * <tr><td>&amp;</td><td>&amp;amp;</td></tr>
	 * <tr><td>&lt;</td><td>&amp;lt;</td></tr>
	 * <tr><td>&gt;</td><td>&amp;gt;</td></tr>
	 * <tr><td>&quot;</td><td>&amp;quot;</td></tr>
	 * <tr><td>'</td><td>&amp;apos;</td></tr>
	 * </table>
	 *
	 * @param stringToEscape the string to escape.
	 * @return an escaped string suitable for use in an xml document.
	 */
	public static String escapeXML(String stringToEscape) {
		stringToEscape = stringToEscape.replace("&", "&amp;");
		stringToEscape = stringToEscape.replace("<", "&lt;");
		stringToEscape = stringToEscape.replace(">", "&gt;");
		stringToEscape = stringToEscape.replace("\"", "&quot;");
		stringToEscape = stringToEscape.replace("'", "&apos;");

		return stringToEscape;
	}

	/**
	 * Loads a document from file and transforms it into an XmlElement tree.
	 *
	 * @param file the file to load.
	 * @return an XmlElement tree rendered from the file.
	 * @throws XmlParseException if parsing the file failed for some reason.
	 * @throws IOException if there were any problems reading from the file.
	 */
	public static XmlElement loadXml(File file) throws XmlParseException, IOException {
		return new XmlElement(loadDocument(file).getDocumentElement());
	}

	/**
	 * Loads a document from file and transforms it into an XmlElement tree.
	 *
	 * @param filename the path to the file.
	 * @return an XmlElement tree rendered from the file.
	 * @throws XmlParseException if parsing the file failed for some reason.
	 * @throws IOException if there were any problems reading from the file.
	 */
	public static XmlElement loadXml(String filename) throws XmlParseException, IOException {
		return loadXml(new File(filename));
	}

	/**
	 * Creates a document from a string and transforms it into an XmlElement tree.
	 *
	 * @param xml the xml as a string.
	 * @return an XmlElement tree rendered from the file.
	 * @throws XmlParseException if parsing the xml failed to validate for some reason.
	 */
	public static XmlElement createXml(String xml) throws XmlParseException {
		return new XmlElement(createDocument(xml).getDocumentElement());
	}
}
