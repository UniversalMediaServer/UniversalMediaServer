package net.pms.xmlwise;

import org.w3c.dom.Element;

import org.w3c.dom.NamedNodeMap;

import java.util.Map;
import java.util.HashMap;

/**
 * This is a hash map containing all attributes of a single
 * element.
 * <p>
 * Aside from the hash map methods, it also has convenience
 * methods for extracting integers, booleans and doubles.
 *
 * @author Christoffer Lerno
 */
@SuppressWarnings({"serial"})
public class XmlElementAttributes extends HashMap<String, String> {
	/**
	 * Creates an empty element attribute map.
	 */
	XmlElementAttributes() {
	}

	/**
	 * Creates an object given an Element object.
	 *
	 * @param element the element to read from.
	 */
	public XmlElementAttributes(Element element) {
		super(element.getAttributes().getLength());
		NamedNodeMap map = element.getAttributes();
		int attributesLength = map.getLength();
		for (int i = 0; i < attributesLength; i++) {
			put(map.item(i).getNodeName(), map.item(i).getNodeValue());
		}
	}

	/**
	 * Get an integer attribute.
	 *
	 * @param attribute the name of the attribute.
	 * @return the integer value of the attribute.
	 * @throws XmlParseException if we fail to parse this attribute as an int, or the attribute is missing.
	 */
	public int getInt(String attribute) throws XmlParseException {
		String value = get(attribute);
		if (value == null) {
			throw new XmlParseException("Could not find attribute " + attribute);
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new XmlParseException("Failed to parse int attribute " + attribute, e);
		}
	}

	/**
	 * Get a double attribute.
	 *
	 * @param attribute the name of the attribute.
	 * @return the double value of the attribute.
	 * @throws XmlParseException if we fail to parse this attribute as an double, or the attribute is missing.
	 */
	public double getDouble(String attribute) throws XmlParseException {
		String value = get(attribute);
		if (value == null) {
			throw new XmlParseException("Could not find attribute " + attribute);
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new XmlParseException("Failed to parse double attribute " + attribute, e);
		}
	}

	/**
	 * Get an boolean attribute.
	 * <p>
	 * "true", "yes" and "y" are all interpreted as true. (Case-independent)
	 * <p>
	 * "false", "no" and "no" are all interpreted at false. (Case-independent)
	 *
	 * @param attribute the name of the attribute.
	 * @return the boolean value of the attribute.
	 * @throws XmlParseException if the attribute value does match true or false as defined, or the attribute is missing.
	 */
	public boolean getBoolean(String attribute) throws XmlParseException {
		String value = get(attribute);
		if (value == null) {
			throw new XmlParseException("Could not find attribute " + attribute);
		}
		value = value.toLowerCase();
		if ("true".equals(value) || "yes".equals(value) || "y".equals(value)) {
			return true;
		}
		if ("false".equals(value) || "no".equals(value) || "n".equals(value)) {
			return false;
		}
		throw new XmlParseException("Attribute " + attribute + " did not have boolean value (was: " + value + ')');
	}

	/**
	 * Renders the content of the attributes as Xml. Does not do proper XML-escaping.
	 *
	 * @return this attribute suitable for xml, in the format " attribute1='value1' attribute2='value2' ..."
	 */
	public String toXml() {
		StringBuilder builder = new StringBuilder(10 * size());
		for (Map.Entry<String, String> entry : entrySet()) {
			builder.append(' ').append(entry.getKey()).append("=").append("'");
			builder.append(Xmlwise.escapeXML(entry.getValue())).append("'");
		}
		return builder.toString();
	}
}
