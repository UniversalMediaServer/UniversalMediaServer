package net.pms.xmlwise;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
/**
 * Plist xml handling (serialization and deserialization)
 * <p>
 * <em>The xml plist dtd can be found at http://www.apple.com/DTDs/PropertyList-1.0.dtd</em>
 * <p>
 * The plist spec handles 8 types of objects: booleans, real, integers, dates, binary data,
 * strings, arrays (lists) and dictionaries (maps).
 * <p>
 * The java Plist lib handles converting xml plists to a nested {@code Map<String, Object>}
 * that can be trivially read from java. It also provides a simple way to convert a nested
 * {@code Map<String, Object>} into an xml plist representation.
 * <p>
 * The following mapping will be done when converting from plist to <tt>Map</tt>:
 * <pre>
 * true/false -> Boolean
 * real -> Double
 * integer -> Integer/Long (depends on size, values exceeding an int will be rendered as longs)
 * data -> byte[]
 * string -> String
 * array -> List
 * dict -> Map
 * </pre>
 * <p>
 * When converting from Map -> plist the conversion is as follows:
 * <pre>
 * Boolean -> true/false
 * Float/Double -> real
 * Byte/Short/Integer/Long -> integer
 * byte[] -> data
 * List -> array
 * Map -> dict
 * </pre> 
 *
 * @author Christoffer Lerno
 */
public final class Plist {
	private static final Logger LOGGER = LoggerFactory.getLogger(Plist.class);

	/**
	 * Singleton instance.
	 */
	private final static Plist PLIST = new Plist();

	/**
	 * All element types possible for a plist.
	 */
	private static enum ElementType {
		INTEGER,
		STRING,
		REAL,
		DATA,
		DATE,
		DICT,
		ARRAY,
		TRUE,
		FALSE,}
	private static final String BASE64_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
	private static final char[] BASE64_CHARS = BASE64_STRING.toCharArray();
	private final DateFormat m_dateFormat;
	private final Map<Class<?>, ElementType> m_simpleTypes;

	/**
	 * Convert a nested {@code map<String, Object>} as a plist xml string
	 * using the default mapping.
	 *
	 * @param data the nested data to store as a plist.
	 * @return the resulting xml as a string.
	 */
	public static String toXml(Map<String, Object> data) {
		StringBuilder builder = new StringBuilder(
			"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
			+ "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
			+ "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
			+ "<plist version=\"1.0\">");
		builder.append(PLIST.objectToXml(data).toXml());
		return builder.append("</plist>").toString();
	}

	/**
	 * Store a nested {@code map<String, Object>} as a plist using the default mapping.
	 *
	 * @param data the nested data to store as a plist.
	 * @param filename the destination file to store the data to.
	 * @throws IOException if there was an IO error saving the file.
	 */
	public static void store(Map<String, Object> data, String filename) throws IOException {
		store(data, new File(filename));
	}

	/**
	 * Store a nested {@code map<String, Object>} as a plist using the default mapping.
	 *
	 * @param data the nested data to store as a plist.
	 * @param file the destination File to store the data to.
	 * @throws IOException if there was an IO error saving the file.
	 */
	public static void store(Map<String, Object> data, File file) throws IOException {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(file);
			stream.write(toXml(data).getBytes());
		} finally {
			silentlyClose(stream);
		}
	}

	/**
	 * Utility method to close a closeable.
	 *
	 * @param closeable or null.
	 */
	static void silentlyClose(Closeable closeable) {
		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException e) {
			LOGGER.debug("Caught exception", e);
		}
	}

	/**
	 * Create a nested {@code map<String, Object>} from a plist xml string using the default mapping.
	 *
	 * @param xml the plist xml data as a string.
	 * @return the resulting map as read from the plist data.
	 * @throws XmlParseException if the plist could not be properly parsed.
	 */
	public static Map<String, Object> fromXml(String xml) throws XmlParseException {
		return PLIST.parse(Xmlwise.createXml(xml));
	}

	/**
	 * Create a nested {@code map<String, Object>} from a plist xml file using the default mapping.
	 *
	 * @param file the File containing the the plist xml.
	 * @return the resulting map as read from the plist data.
	 * @throws XmlParseException if the plist could not be properly parsed.
	 * @throws IOException if there was an issue reading the plist file.
	 */
	public static Map<String, Object> load(File file) throws XmlParseException, IOException {
		return PLIST.parse(Xmlwise.loadXml(file));
	}

	/**
	 * Create a nested {@code map<String, Object>} from a plist xml file using the default mapping.
	 *
	 * @param filename the file containing the the plist xml.
	 * @return the resulting map as read from the plist data.
	 * @throws XmlParseException if the plist could not be properly parsed.
	 * @throws IOException if there was an issue reading the plist file.
	 */
	public static Map<String, Object> load(String filename) throws XmlParseException, IOException {
		return load(new File(filename));
	}

	/**
	 * Create a plist handler.
	 */
	Plist() {
		m_dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		m_dateFormat.setTimeZone(TimeZone.getTimeZone("Z"));
		m_simpleTypes = new HashMap<Class<?>, ElementType>();
		m_simpleTypes.put(Integer.class, ElementType.INTEGER);
		m_simpleTypes.put(Byte.class, ElementType.INTEGER);
		m_simpleTypes.put(Short.class, ElementType.INTEGER);
		m_simpleTypes.put(Short.class, ElementType.INTEGER);
		m_simpleTypes.put(Long.class, ElementType.INTEGER);
		m_simpleTypes.put(String.class, ElementType.STRING);
		m_simpleTypes.put(Float.class, ElementType.REAL);
		m_simpleTypes.put(Double.class, ElementType.REAL);
		m_simpleTypes.put(byte[].class, ElementType.DATA);
		m_simpleTypes.put(Boolean.class, ElementType.TRUE);
		m_simpleTypes.put(Date.class, ElementType.DATE);
	}

	/**
	 * Convert an object to its plist representation.
	 *
	 * @param o the object to convert, must be Integer, Double, String, Date, Boolean, byte[],
	 * Map or List.
	 * @return an <tt>XmlElement</tt> containing the serialized version of the object.
	 */
	XmlElement objectToXml(Object o) {
		ElementType type = m_simpleTypes.get(o.getClass());
		if (type != null) {
			switch (type) {
				case REAL:
					return new XmlElement("real", o.toString());
				case INTEGER:
					return new XmlElement("integer", o.toString());
				case TRUE:
					return new XmlElement(((Boolean) o) ? "true" : "false");
				case DATE:
					return new XmlElement("date", m_dateFormat.format((Date) o));
				case STRING:
					return new XmlElement("string", (String) o);
				case DATA:
					return new XmlElement("data", base64encode((byte[]) o));
			}
		}
		if (o instanceof Map) {
			return toXmlDict((Map<String, Object>) o);
		} else if (o instanceof List) {
			return toXmlArray((List<?>) o);
		} else {
			throw new RuntimeException("Cannot use " + o.getClass() + " in plist.");
		}
	}

	/**
	 * Convert a list to its plist representation.
	 *
	 * @param list the list to convert.
	 * @return an <tt>XmlElement</tt> representing the list.
	 */
	private XmlElement toXmlArray(List<?> list) {
		XmlElement array = new XmlElement("array");
		for (Object o : list) {
			array.add(objectToXml(o));
		}
		return array;
	}

	/**
	 * Convert a map to its plist representation.
	 *
	 * @param map the map to convert, assumed to have string keys.
	 * @return an <tt>XmlElement</tt> representing the map.
	 */
	private XmlElement toXmlDict(Map<String, Object> map) {
		XmlElement dict = new XmlElement("dict");
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			dict.add(new XmlElement("key", entry.getKey()));
			dict.add(objectToXml(entry.getValue()));
		}
		return dict;
	}

	/**
	 * Parses a plist top element into a map dictionary containing all the data
	 * in the plist.
	 *
	 * @param element the top plist element.
	 * @return the resulting data tree structure.
	 * @throws XmlParseException if there was any error parsing the xml.
	 */
	Map<String, Object> parse(XmlElement element) throws XmlParseException {
		if (!"plist".equalsIgnoreCase(element.getName())) {
			throw new XmlParseException("Expected plist top element, was: " + element.getName());
		}

		// Assure that the top element is a dict and the single child element.
		if (element.size() != 1) {
			throw new XmlParseException("Expected single 'dict' child element.");
		}
		element.getUnique("dict");

		return (Map<String, Object>) parseElement(element.getUnique("dict"));
	}

	/**
	 * Parses a (non-top) xml element.
	 *
	 * @param element the element to parse.
	 * @return the resulting object.
	 * @throws XmlParseException if there was some error in the xml.
	 */
	private Object parseElement(XmlElement element) throws XmlParseException {
		try {
			return parseElementRaw(element);
		} catch (Exception e) {
			throw new XmlParseException("Failed to parse: " + element.toXml(), e);
		}
	}

	/**
	 * Parses a (non-top) xml element.
	 *
	 * @param element the element to parse.
	 * @return the resulting object.
	 * @throws Exception if there was some error parsing the xml.
	 */
	private Object parseElementRaw(XmlElement element) throws Exception {
		ElementType type = ElementType.valueOf(element.getName().toUpperCase());
		switch (type) {
			case INTEGER:
				return parseInt(element.getValue());
			case REAL:
				return parseDouble(element.getValue());
			case STRING:
				return element.getValue();
			case DATE:
				return m_dateFormat.parse(element.getValue());
			case DATA:
				return base64decode(element.getValue());
			case ARRAY:
				return parseArray(element);
			case TRUE:
				return Boolean.TRUE;
			case FALSE:
				return Boolean.FALSE;
			case DICT:
				return parseDict(element);
			default:
				throw new RuntimeException("Unexpected type: " + element.getName());
		}
	}

	/**
	 * Parses a string into a Long or Integer depending on size.
	 *
	 * @param value the value as a string.
	 * @return the long value of this string is the value doesn't fit in an integer,
	 * otherwise the int value of the string.
	 */
	private Number parseInt(String value) {
		Long l = Long.valueOf(value);
		if (l.intValue() == l) {
			return l.intValue();
		}
		return l;
	}

	/**
	 * Parses a string into a Double
	 *
	 * @param value the value as a string.
	 * @return the Double value of this string.
	 */
	private Number parseDouble(String value) {
		if (value.equals("NaN")) {
			return Double.NaN;
		}
		if (value.equals("-inf")) {
			return Double.NEGATIVE_INFINITY;
		}
		if (value.equals("inf")) {
			return Double.POSITIVE_INFINITY;
		}

		return Double.valueOf(value);
	}

	/**
	 * Parse a list of xml elements as a plist dict.
	 *
	 * @param elements the elements to parse.
	 * @return the dict deserialized as a map.
	 * @throws Exception if there are any problems deserializing the map.
	 */
	private Map<String, Object> parseDict(List<XmlElement> elements) throws Exception {
		Iterator<XmlElement> element = elements.iterator();
		HashMap<String, Object> dict = new HashMap<String, Object>();
		while (element.hasNext()) {
			XmlElement key = element.next();
			if (!"key".equals(key.getName())) {
				throw new Exception("Expected key but was " + key.getName());
			}
			Object o = parseElementRaw(element.next());
			dict.put(key.getValue(), o);
		}
		return dict;
	}

	/**
	 * Parse a list of xml elements as a plist array.
	 *
	 * @param elements the elements to parse.
	 * @return the array deserialized as a list.
	 * @throws Exception if there are any problems deserializing the list.
	 */
	private List<Object> parseArray(List<XmlElement> elements) throws Exception {
		ArrayList<Object> list = new ArrayList<Object>(elements.size());
		for (XmlElement element : elements) {
			list.add(parseElementRaw(element));
		}
		return list;
	}

	/**
	 * Encode an array of bytes to a string using base64 encoding.
	 *
	 * @param bytes the bytes to convert.
	 * @return the base64 representation of the bytes.
	 */
	static String base64encode(byte[] bytes) {
		StringBuilder builder = new StringBuilder(((bytes.length + 2) / 3) * 4);
		for (int i = 0; i < bytes.length; i += 3) {
			byte b0 = bytes[i];
			byte b1 = i < bytes.length - 1 ? bytes[i + 1] : 0;
			byte b2 = i < bytes.length - 2 ? bytes[i + 2] : 0;
			builder.append(BASE64_CHARS[(b0 & 0xFF) >> 2]);
			builder.append(BASE64_CHARS[((b0 & 0x03) << 4) | ((b1 & 0xF0) >> 4)]);
			builder.append(i < bytes.length - 1 ? BASE64_CHARS[((b1 & 0x0F) << 2) | ((b2 & 0xC0) >> 6)] : "=");
			builder.append(i < bytes.length - 2 ? BASE64_CHARS[b2 & 0x3F] : "=");
		}
		return builder.toString();
	}

	/**
	 * Converts a string to a byte array assuming the string uses base64-encoding.
	 *
	 * @param base64 the string to convert.
	 * @return the resulting byte array.
	 */
	static byte[] base64decode(String base64) {
		base64 = base64.trim();
		int endTrim = base64.endsWith("==") ? 2 : base64.endsWith("=") ? 1 : 0;
		int length = (base64.length() / 4) * 3 - endTrim;
		base64 = base64.replace('=', 'A');
		byte[] result = new byte[length];
		int stringLength = base64.length();
		int index = 0;
		for (int i = 0; i < stringLength; i += 4) {
			int i0 = BASE64_STRING.indexOf(base64.charAt(i));
			int i1 = BASE64_STRING.indexOf(base64.charAt(i + 1));
			int i2 = BASE64_STRING.indexOf(base64.charAt(i + 2));
			int i3 = BASE64_STRING.indexOf(base64.charAt(i + 3));
			byte b0 = (byte) ((i0 << 2) | (i1 >> 4));
			byte b1 = (byte) ((i1 << 4) | (i2 >> 2));
			byte b2 = (byte) ((i2 << 6) | i3);
			result[index++] = b0;
			if (index < length) {
				result[index++] = b1;
				if (index < length) {
					result[index++] = b2;
				}
			}
		}
		return result;
	}
}
