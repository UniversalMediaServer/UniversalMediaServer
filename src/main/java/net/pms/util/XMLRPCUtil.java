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

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.awt.List;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * A utility class for reading and writing {@code XML-RPC} using the Streaming
 * API for XML (StAX).
 *
 * @author Nadahar
 */
public class XMLRPCUtil {

	/** A static {@link XMLOutputFactory} for creating XML writers */
	public static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newFactory();

	/** A static {@link XMLInputFactory} for creating XML readers */
	public static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newFactory();

	/**
	 * Not to be instantiated.
	 */
	private XMLRPCUtil() {
	}

	/**
	 * Creates a new {@link XMLStreamReader} for the specified
	 * {@link InputStream} using UTF-8 character encoding.
	 *
	 * @param inputStream the {@link InputStream}.
	 * @return The new {@link XMLStreamReader}.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static XMLStreamReader createReader(InputStream inputStream) throws XMLStreamException {
		return INPUT_FACTORY.createXMLStreamReader(inputStream, "UTF-8");
	}

	/**
	 * Creates a new {@link XMLStreamWriter} that will write to the specified
	 * {@link OutputStream} using UTF-8 character encoding.
	 *
	 * @param outputStream the {@link OutputStream}.
	 * @return The new {@link XMLStreamWriter}.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static XMLStreamWriter createWriter(OutputStream outputStream) throws XMLStreamException {
		return OUTPUT_FACTORY.createXMLStreamWriter(outputStream, "UTF-8");
	}

	/**
	 * Parses an {@code XML-RPC} {@code <methodResponse>} element into a
	 * {@link Params} structure using the specified {@link XMLStreamReader}.
	 *
	 * @param reader the {@link XMLStreamReader} whose content to parse.
	 * @return The new {@link Params} structure containing the parsed content of
	 *         the {@code <methodResponse>} element.
	 * @throws XMLRPCException If a parsing error occurs during the operation.
	 * @throws XMLStreamException If a stream error occurs during the operation.
	 */
	public static Params readMethodResponse(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
		Params result = null;
		boolean methodResponse = false;
		while (reader.hasNext()) {
			int eventType = reader.next();
			switch (eventType) {
				case XMLStreamReader.START_ELEMENT:
					String elementName = reader.getLocalName();
					switch (elementName) {
						case "methodResponse":
							if (reader.isStartElement()) {
								methodResponse = true;
							}
							break;
						case "params":
							if (!methodResponse) {
								throw new XMLRPCException("XML-RPC: XML document isn't a <methodResponse>");
							}
							result = Params.read(reader);
							break;
						default:
							throw new XMLRPCException("XML-RPC: Unexpected <methodResponse> property \"" + elementName + "\"");
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					if (!"methodResponse".equals(reader.getLocalName())) {
						throw new XMLRPCException("XML-RPC: Invalid <methodResponse>");
					}
					return result;
				default:
					// Ignore
			}
		}
		throw new XMLRPCException("XML-RPC: Premature end of stream");
	}

	/**
	 * Parses an {@code XML-RPC} {@code <param>} element into a {@link Value}
	 * using the specified {@link XMLStreamReader}. This method is called via
	 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
	 * shouldn't normally be called directly.
	 *
	 * @param reader the {@link XMLStreamReader} whose content to parse.
	 * @return The new {@link Value} containing the parsed content of the
	 *         {@code <param>} element.
	 * @throws XMLRPCException If a parsing error occurs during the operation.
	 * @throws XMLStreamException If a stream error occurs during the operation.
	 */
	public static Value<?> readParam(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
		if (!"param".equals(reader.getLocalName())) {
			throw new XMLRPCException("XML-RPC: Cursor isn't at a <param> element");
		}
		Value<?> result = null;
		while (reader.hasNext()) {
			int eventType = reader.next();
			switch (eventType) {
				case XMLStreamReader.START_ELEMENT:
					String elementName = reader.getLocalName();
					switch (elementName) {
						case "value":
							result = Value.read(reader);
							break;
						default:
							throw new XMLRPCException("XML-RPC: Unexpected <param> property \"" + elementName + "\"");
					}
					break;
				case XMLStreamReader.END_ELEMENT:
					if (!"param".equals(reader.getLocalName())) {
						throw new XMLRPCException("XML-RPC: Invalid <param> element");
					}
					return result;
				default:
					// Ignore
			}
		}
		throw new XMLRPCException("XML-RPC: Premature end of stream");
	}

	/**
	 * Parses an {@code XML-RPC} {@code "name"} element into a {@link String}
	 * using the specified {@link XMLStreamReader}. This method is called via
	 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
	 * shouldn't normally be called directly.
	 *
	 * @param reader the {@link XMLStreamReader} whose content to parse.
	 * @return The new {@link String} containing the parsed content of the
	 *         {@code "name"} element.
	 * @throws XMLRPCException If a parsing error occurs during the operation.
	 * @throws XMLStreamException If a stream error occurs during the operation.
	 */
	public static String readName(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
		if (!"name".equals(reader.getLocalName())) {
			throw new XMLRPCException("XML-RPC: Cursor isn't at a name element");
		}
		String name = readCharacters(reader);
		if (reader.getEventType() != XMLStreamReader.END_ELEMENT || !"name".equals(reader.getLocalName())) {
			throw new XMLRPCException("XML-RPC: Invalid name element");
		}
		return name;
	}

	/**
	 * Reads all the {@link XMLStreamConstants#CHARACTERS} or
	 * {@link XMLStreamConstants#CDATA} found until the next tag into a
	 * {@link String} using the specified {@link XMLStreamReader}. This method
	 * is called via {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as
	 * needed and shouldn't normally be called directly.
	 *
	 * @param reader the {@link XMLStreamReader} whose content to read.
	 * @return The new {@link String} containing the content.
	 * @throws XMLRPCException If a parsing error occurs during the operation.
	 * @throws XMLStreamException If a stream error occurs during the operation.
	 */
	public static String readCharacters(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
		StringBuilder result = new StringBuilder();
		while (reader.hasNext()) {
			int eventType = reader.next();
			switch (eventType) {
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.CDATA:
					result.append(reader.getText());
					break;
				case XMLStreamReader.END_ELEMENT:
					return result.toString();
				default:
					// Ignore
			}
		}
		throw new XMLRPCException("XML-RPC: Premature end of stream");
	}

	/**
	 * Writes the specified {@link Params} structure as an {@code XML-RPC}
	 * {@code <methodCall>} with the specified method name to the specified
	 * {@link XMLStreamWriter}. The document encoding is set to {@code "utf-8"}
	 * and the XML version to {@code "1.0"}.
	 *
	 * @param writer the {@link XMLStreamWriter} to write to.
	 * @param methodName the method name.
	 * @param params the {@link Params} structure.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static void writeMethod(XMLStreamWriter writer, String methodName, Params params) throws XMLStreamException {
		writeMethodStart(writer, methodName);
		if (params == null) {
			writer.writeEmptyElement("params");
		} else {
			params.write(writer);
		}
		writeMethodEnd(writer);
	}

	/**
	 * Writes an {@code XML-RCP} {@code <methodCall>} "start block". The
	 * document encoding is set to {@code "utf-8"} and the XML version to
	 * {@code "1.0"}.
	 *
	 * @param writer the {@link XMLStreamWriter} to write to.
	 * @param methodName the method name.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static void writeMethodStart(XMLStreamWriter writer, String methodName) throws XMLStreamException {
		writer.writeStartDocument("utf-8", "1.0");
		writer.writeStartElement("methodCall");
		writeTextElement(writer, "methodName", methodName);
	}

	/**
	 * Writes an {@code XML-RCP} {@code <methodCall>} end element and closes any
	 * open tags.
	 *
	 * @param writer the {@link XMLStreamWriter} to write to.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static void writeMethodEnd(XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEndElement();
		writer.writeEndDocument();
	}

	/**
	 * A convenience method to close {@code n} tags.
	 *
	 * @param writer the {@link XMLStreamWriter} to write to.
	 * @param n the number of tags to close.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static void writeEndElements(XMLStreamWriter writer, int n) throws XMLStreamException {
		if (n < 1) {
			throw new IllegalArgumentException("elements must be 1 or greater");
		}
		for (int i = 0; i < n; i++) {
			writer.writeEndElement();
		}
	}

	/**
	 * Writes an {@code XML-RCP} {@code <boolean>} with the specified value.
	 *
	 * @param writer the {@link XMLStreamWriter} to write to.
	 * @param value the {@link Boolean} value.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static void writeBooleanValue(XMLStreamWriter writer, Boolean value) throws XMLStreamException {
		writer.writeStartElement("boolean");
		writer.writeCharacters(value != null && value.booleanValue() ? "1" : "0");
		writer.writeEndElement();
	}

	/**
	 * Writes an XML tag without attributes using the specified name and tag.
	 *
	 * @param writer the {@link XMLStreamWriter} to write to.
	 * @param elementName the tag name.
	 * @param text the tag text content.
	 * @throws XMLStreamException If an error occurs during the operation.
	 */
	public static void writeTextElement(XMLStreamWriter writer, String elementName, String text) throws XMLStreamException {
		if (isBlank(text)) {
			writer.writeEmptyElement(elementName);
		} else {
			writer.writeStartElement(elementName);
			writer.writeCharacters(text);
			writer.writeEndElement();
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC} {@code <params>}
	 * element. It implements the {@link List} interface for easy manipulation.
	 * All elements be of type {@link Value}. All contained {@link Value}s will
	 * be written inside {@code <param>} tags when {@link #write()} is called.
	 *
	 * @author Nadahar
	 */
	public static class Params extends ArrayList<Value<?>> {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an empty instance with an initial capacity of ten.
		 */
		public Params() {
			super();
		}

		/**
		 * Constructs an instance containing the elements of the specified
		 * {@link Collection}, in the order they are returned by the
		 * {@link Collection}'s {@link Iterator}.
		 *
		 * @param collection the {@link Collection} whose elements are to be
		 *            placed into this instance.
		 * @throws NullPointerException If the specified collection is
		 *             {@code null}.
		 */
		public Params(Collection<? extends Value<?>> collection) {
			super(collection);
		}

		/**
		 * Constructs an empty instance with the specified initial capacity.
		 *
		 * @param initialCapacity the initial capacity of the instance.
		 * @throws IllegalArgumentException If the specified initial capacity is
		 *             negative.
		 */
		public Params(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Writes this {@code XML-RPC} {@code <params>} element with all
		 * children to the specified {@link XMLStreamWriter}. All children will
		 * be written inside {@code <param>} tags.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			writer.writeStartElement("params");
			for (Value<?> value : this) {
				if (value == null) {
					writer.writeEmptyElement("param");
				} else {
					writer.writeStartElement("param");
					value.write(writer);
					writer.writeEndElement();
				}
			}
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append("{\n");
			for (Value<?> value : this) {
				sb.append("  ").append(value).append("\n");
			}
			sb.append("}");
			return sb.toString();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <params>} element into a {@link Params}
		 * structure using the specified {@link XMLStreamReader}. This method is
		 * called by {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as
		 * needed and shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link Params} structure containing the parsed content of
		 *         the {@code <params>} element.
		 * @throws XMLRPCException If a parsing error occurs during the operation.
		 * @throws XMLStreamException If a stream error occurs during the operation.
		 */
		public static Params read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!"params".equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at a <params> element");
			}
			Params result = new Params();
			while (reader.hasNext()) {
				int eventType = reader.next();
				switch (eventType) {
					case XMLStreamReader.START_ELEMENT:
						String elementName = reader.getLocalName();
						switch (elementName) {
							case "param":
								result.add(readParam(reader));
								break;
							default:
								throw new XMLRPCException("XML-RPC: Unexpected <params> property \"" + elementName + "\"");
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if (!"params".equals(reader.getLocalName())) {
							throw new XMLRPCException("XML-RPC: Invalid <params> element");
						}
						return result;
					default:
						// Ignore
				}
			}
			throw new XMLRPCException("XML-RPC: Premature end of stream");
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC} {@code <member>}
	 * element. A {@link Member} consists of a {@link XMLRPCTypes} type, a name
	 * and a {@link Value}.
	 *
	 * @param <E> the {@link Value} implementation.
	 * @param <T> the {@link Value} type.
	 *
	 * @author Nadahar
	 */
	public static class Member<E extends Value<T>, T> {
		private final XMLRPCTypes type;
		private final String name;
		private final E value;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param type the {@link XMLRPCTypes} type to use. Must correspond to
		 *            the {@code value} type.
		 * @param name the member name.
		 * @param value the member {@link Value}.
		 */
		protected Member(XMLRPCTypes type, String name, E value) {
			this.type = type;
			this.name = name;
			this.value = value;
		}

		/**
		 * @return The name.
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return The {@link Value}.
		 */
		public E getValueInstance() {
			return value;
		}

		/**
		 * @return The value of the {@link Value}.
		 */
		public T getValue() {
			return value == null ? null : value.getValue();
		}

		/**
		 * @return The {@link XMLRPCTypes} type.
		 */
		public XMLRPCTypes getType() {
			return type;
		}

		/**
		 * Writes this {@code XML-RPC} {@code <member>} element to the specified
		 * {@link XMLStreamWriter}.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			writer.writeStartElement("member");
			writeTextElement(writer, "name", name);
			if (value == null) {
				writer.writeEmptyElement("value");
			} else {
				value.write(writer);
			}
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append("[name=\"").append(name).append("\", value=");
			if (value == null) {
				sb.append("null");
				return sb.toString();
			}
			if (value.getValue() instanceof String) {
				sb.append("\"").append(value.getValue()).append("\"");
			} else {
				sb.append(value.getValue());
			}
			sb.append("]");
			return sb.toString();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <member>} element into a
		 * {@link Member} using the specified {@link XMLStreamReader}. This
		 * method is called via
		 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
		 * shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link Member} containing the parsed content of the
		 *         {@code <member>} element.
		 * @throws XMLRPCException If a parsing error occurs during the
		 *             operation.
		 * @throws XMLStreamException If a stream error occurs during the
		 *             operation.
		 */
		public static Member<? extends Value<?>, ?> read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!"member".equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at a <member> element");
			}
			String memberName = null;
			Member<? extends Value<?>, ?> result = null;
			while (reader.hasNext()) {
				int eventType = reader.next();
				switch (eventType) {
					case XMLStreamReader.START_ELEMENT:
						String elementName = reader.getLocalName();
						switch (elementName) {
							case "name":
								memberName = readName(reader);
								break;
							case "value":
								Value<?> value = Value.read(reader);
								if (value == null) {
									result = new Member<Value<Object>, Object>(null, memberName, null);
								} else {
									switch (value.getType()) {
										case ARRAY:
											result = new MemberArray(memberName, (ValueArray) value);
											break;
										case BASE64:
											result = new MemberBase64(memberName, (ValueBase64) value);
											break;
										case BOOLEAN:
											result = new MemberBoolean(memberName, (ValueBoolean) value);
											break;
										case DATETIME_ISO8601:
											result = new MemberDateTime(memberName, (ValueDateTime) value);
											break;
										case DOUBLE:
											result = new MemberDouble(memberName, (ValueDouble) value);
											break;
										case INT:
											result = new MemberInt(memberName, (ValueInt) value);
											break;
										case STRING:
											result = new MemberString(memberName, (ValueString) value);
											break;
										case STRUCT:
											result = new MemberStruct(memberName, (ValueStruct) value);
											break;
										default:
											throw new AssertionError("Unimplemented <member> type \"" + value.getType() + "\"");
									}
								}
								break;
							default:
								throw new XMLRPCException("XML-RPC: Unexpected <member> property \"" + elementName + "\"");
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if (!"member".equals(reader.getLocalName())) {
							throw new XMLRPCException("XML-RPC: Invalid <member> element");
						}
						return result;
					default:
						// Ignore
				}
			}
			throw new XMLRPCException("XML-RPC: Premature end of stream");
		}

		/**
		 * Extracts the {@link String} value from the {@link Member} with the
		 * specified name from the specified {@link Map} where the keys are
		 * {@link Member} names and the values are {@link Member}s.
		 * {@link Struct} is an example of such a {@link Map}.
		 * <p>
		 * If a {@link Member} with the specified name doesn't exist,
		 * {@code null} is returned.
		 * <p>
		 * This will return the string representation of the matching
		 * {@link Member} regardless of the {@link XMLRPCTypes}, which might
		 * give unexpected results for types like {@link XMLRPCTypes#ARRAY} and
		 * {@link XMLRPCTypes#STRUCT}.
		 *
		 * @param members the {@link Map} of {@link Member} name and
		 *            {@link Member} pairs.
		 * @param memberName the name of the {@link Member} whose string
		 *            representation to get.
		 * @return The resulting {@link String} or {@code null} if the string
		 *         value is {@code null} or the {@link Member} doesn't exist in
		 *         the specified {@link Map}.
		 */
		public static String getString(Map<String, ? extends Member<?, ?>> members, String memberName) {
			Member<?, ?> member = members.get(memberName);
			return member == null ? null : member.getValue().toString();
		}

		/**
		 * Extracts the {@code int} value from the {@link Member} with the
		 * specified name from the specified {@link Map} where the keys are
		 * {@link Member} names and the values are {@link Member}s.
		 * {@link Struct} is an example of such a {@link Map}.
		 * <p>
		 * If a {@link Member} with the specified name doesn't exist or the
		 * {@link Member}'s value can't be parsed to an {@code int}, {@code -1}
		 * is returned.
		 *
		 * @param members the {@link Map} of {@link Member} name and
		 *            {@link Member} pairs.
		 * @param memberName the name of the {@link Member} whose integer
		 *            representation to get.
		 * @return The resulting {@code int} or {@code -1} if the value can't be
		 *         parsed to an {@code int} or the {@link Member} doesn't exist
		 *         in the specified {@link Map}.
		 */
		public static int getInt(Map<String, ? extends Member<?, ?>> members, String memberName) {
			Member<?, ?> member = members.get(memberName);
			if (member == null || member.getValue() == null) {
				return -1;
			}
			if (member.getValue() instanceof Number) {
				return ((Number) member.getValue()).intValue();
			}
			try {
				return Integer.parseInt(member.getValue().toString());
			} catch (NumberFormatException e) {
				return -1;
			}
		}

		/**
		 * Extracts the {@code double} value from the {@link Member} with the
		 * specified name from the specified {@link Map} where the keys are
		 * {@link Member} names and the values are {@link Member}s.
		 * {@link Struct} is an example of such a {@link Map}.
		 * <p>
		 * If a {@link Member} with the specified name doesn't exist or the
		 * {@link Member}'s value can't be parsed to a {@code double},
		 * {@link Double#NaN} is returned.
		 *
		 * @param members the {@link Map} of {@link Member} name and
		 *            {@link Member} pairs.
		 * @param memberName the name of the {@link Member} whose double
		 *            representation to get.
		 * @return The resulting {@code double} or {@link Double#NaN} if the
		 *         value can't be parsed to a {@code double} or the
		 *         {@link Member} doesn't exist in the specified {@link Map}.
		 */
		public static double getDouble(Map<String, ? extends Member<?, ?>> members, String memberName) {
			Member<?, ?> member = members.get(memberName);
			if (member == null || member.getValue() == null) {
				return Double.NaN;
			}
			if (member.getValue() instanceof Number) {
				return ((Number) member.getValue()).doubleValue();
			}
			try {
				return Double.parseDouble(member.getValue().toString());
			} catch (NumberFormatException e) {
				return Double.NaN;
			}
		}

		/**
		 * Extracts the {@code boolean} value from the {@link Member} with the
		 * specified name from the specified {@link Map} where the keys are
		 * {@link Member} names and the values are {@link Member}s.
		 * {@link Struct} is an example of such a {@link Map}.
		 * <p>
		 * If a {@link Member} with the specified name doesn't exist or the
		 * {@link Member}'s value can't be parsed to a {@code boolean},
		 * {@code false} is returned.
		 *
		 * @param members the {@link Map} of {@link Member} name and
		 *            {@link Member} pairs.
		 * @param memberName the name of the {@link Member} whose boolean
		 *            representation to get.
		 * @return The resulting {@code boolean} or {@code false} if the value
		 *         can't be parsed to a {@code boolean} or the {@link Member}
		 *         doesn't exist in the specified {@link Map}.
		 */
		public static boolean getBoolean(Map<String, ? extends Member<?, ?>> members, String memberName) {
			Member<?, ?> member = members.get(memberName);
			if (member == null || member.getValue() == null) {
				return false;
			}
			return StringUtil.parseBoolean(member.getValue(), true);
		}
	}

	/**
	 * A predefined {@link Member} of type {@link Array}.
	 */
	public static class MemberArray extends Member<Value<Array>, Array> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link Array}.
		 */
		public MemberArray(String name, Value<Array> value) {
			super(XMLRPCTypes.ARRAY, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Array}.
		 */
		public MemberArray(String name, Array value) {
			super(XMLRPCTypes.ARRAY, name, new ValueArray(value));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link Base64}.
	 */
	public static class MemberBase64 extends Member<Value<Base64>, Base64> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link Base64}.
		 */
		public MemberBase64(String name, Value<Base64> value) {
			super(XMLRPCTypes.BASE64, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Base64}.
		 */
		public MemberBase64(String name, Base64 value) {
			super(XMLRPCTypes.BASE64, name, new ValueBase64(value));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link Boolean}.
	 */
	public static class MemberBoolean extends Member<Value<Boolean>, Boolean> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link Boolean}.
		 */
		public MemberBoolean(String name, Value<Boolean> value) {
			super(XMLRPCTypes.BOOLEAN, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Boolean}.
		 */
		public MemberBoolean(String name, Boolean value) {
			super(XMLRPCTypes.BOOLEAN, name, new ValueBoolean(value));
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@code boolean}.
		 */
		public MemberBoolean(String name, boolean value) {
			super(XMLRPCTypes.BOOLEAN, name, new ValueBoolean(value));
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Object} to be parsed into a {@link Boolean}
		 *            using {@link StringUtil#parseBoolean(Object, boolean)}.
		 */
		public MemberBoolean(String name, Object value) {
			super(XMLRPCTypes.BOOLEAN, name, new ValueBoolean(value));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link DateTime}.
	 */
	public static class MemberDateTime extends Member<Value<DateTime>, DateTime> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link DateTime}.
		 */
		public MemberDateTime(String name, Value<DateTime> value) {
			super(XMLRPCTypes.DATETIME_ISO8601, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link DateTime}.
		 */
		public MemberDateTime(String name, DateTime value) {
			super(XMLRPCTypes.DATETIME_ISO8601, name, new ValueDateTime(value));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link Double}.
	 */
	public static class MemberDouble extends Member<Value<Double>, Double> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link Double}.
		 */
		public MemberDouble(String name, Value<Double> value) {
			super(XMLRPCTypes.DOUBLE, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Double}.
		 */
		public MemberDouble(String name, Double value) {
			super(XMLRPCTypes.DOUBLE, name, new ValueDouble(value));
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@code double}.
		 */
		public MemberDouble(String name, double value) {
			super(XMLRPCTypes.DOUBLE, name, new ValueDouble(value));
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Number}.
		 */
		public MemberDouble(String name, Number value) {
			super(XMLRPCTypes.DOUBLE, name, new ValueDouble(value));
		}

		/**
		 * Creates a new instance with the specified name by parsing
		 * {@code value} into a {@link Double}.
		 * <p>
		 * <b>Note:</b> A {@link NumberFormatException} will be thrown if
		 * {@code value} doesn't contain a valid {@link Double} value.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link String} to parse to a {@link Double}.
		 * @throws NumberFormatException If the string doesn't contain a
		 *             parsable number.
		 */
		public MemberDouble(String name, String value) {
			super(XMLRPCTypes.DOUBLE, name, new ValueDouble(Double.valueOf(value)));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link Integer}.
	 */
	public static class MemberInt extends Member<Value<Integer>, Integer> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link Integer}.
		 */
		public MemberInt(String name, Value<Integer> value) {
			super(XMLRPCTypes.INT, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Integer}.
		 */
		public MemberInt(String name, Integer value) {
			super(XMLRPCTypes.INT, name, new ValueInt(value));
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@code int}.
		 */
		public MemberInt(String name, int value) {
			super(XMLRPCTypes.INT, name, new ValueInt(value));
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Number}.
		 */
		public MemberInt(String name, Number value) {
			super(XMLRPCTypes.INT, name, new ValueInt(value));
		}

		/**
		 * Creates a new instance with the specified name by parsing
		 * {@code value} into a {@link Integer}.
		 * <p>
		 * <b>Note:</b> A {@link NumberFormatException} will be thrown if
		 * {@code value} doesn't contain a valid {@link Integer} value.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link String} to parse to a {@link Integer}.
		 * @throws NumberFormatException If the string doesn't contain a
		 *             parsable number.
		 */
		public MemberInt(String name, String value) {
			super(XMLRPCTypes.INT, name, new ValueInt(Integer.valueOf(value)));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link String}.
	 */
	public static class MemberString extends Member<Value<String>, String> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link String}.
		 */
		public MemberString(String name, Value<String> value) {
			super(XMLRPCTypes.STRING, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link String}.
		 */
		public MemberString(String name, String value) {
			super(XMLRPCTypes.STRING, name, new ValueString(value));
		}
	}

	/**
	 * A predefined {@link Member} of type {@link Struct}.
	 */
	public static class MemberStruct extends Member<Value<Struct>, Struct> {

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Value} of type {@link Struct}.
		 */
		public MemberStruct(String name, Value<Struct> value) {
			super(XMLRPCTypes.STRUCT, name, value);
		}

		/**
		 * Creates a new instance with the specified values.
		 *
		 * @param name the {@link Member} name.
		 * @param value the {@link Struct}.
		 */
		public MemberStruct(String name, Struct value) {
			super(XMLRPCTypes.STRUCT, name, new ValueStruct(value));
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC} {@code <value>}
	 * element. A {@link Value} consists of a {@link XMLRPCTypes} type and a
	 * {@link Value} of the corresponding type.
	 *
	 * @param <T> the {@link Value} type.
	 *
	 * @author Nadahar
	 */
	public static class Value<T> {
		private final XMLRPCTypes type;
		private final T value;

		/**
		 * Creates a new instance using the specified parameters.
		 *
		 * @param type the {@link XMLRPCTypes} type to use. Must correspond to
		 *            the {@code value} type.
		 * @param value the member {@link Value}.
		 */
		public Value(XMLRPCTypes type, T value) {
			this.type = type;
			this.value = value;
		}

		/**
		 * @return The {@link XMLRPCTypes} type.
		 */
		public XMLRPCTypes getType() {
			return type;
		}

		/**
		 * @return The value.
		 */
		public T getValue() {
			return value;
		}

		/**
		 * Writes this {@code XML-RPC} {@code <value>} element to the specified
		 * {@link XMLStreamWriter}.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			if (type == null) {
				writer.writeEmptyElement("value");
				return;
			}
			if (value == null) {
				writer.writeStartElement("value");
				writer.writeEmptyElement(type.toString());
				writer.writeEndElement();
				return;
			}
			writer.writeStartElement("value");
			switch (type) {
				case ARRAY:
					((Array) value).write(writer);
					break;
				case BASE64:
					((Base64) value).write(writer);
					break;
				case BOOLEAN:
					writeBooleanValue(writer, (Boolean) value);
					break;
				case DATETIME_ISO8601:
					((DateTime) value).write(writer);
					break;
				case DOUBLE:
				case INT:
				case STRING:
					writeTextElement(writer, type.toString(), value.toString());
					break;
				case STRUCT:
					((Struct) value).write(writer);
					break;
				default:
					throw new AssertionError("Unimplemented <value> type " + type.name());

			}
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if (!"Value".equals(getClass().getSimpleName())) {
				sb.append(getClass().getSimpleName()).append("[");
			} else if (type != null) {
				switch (type) {
					case ARRAY:
						sb.append("Array value[");
						break;
					case BASE64:
						sb.append("Base64 value[");
						break;
					case BOOLEAN:
						sb.append("Boolean value[");
						break;
					case DATETIME_ISO8601:
						sb.append("Date value[");
						break;
					case DOUBLE:
						sb.append("Double value[");
						break;
					case INT:
						sb.append("Int value[");
						break;
					case STRING:
						sb.append("String value[");
						break;
					case STRUCT:
						sb.append("Struct value[");
						break;
					default:
						sb.append(getClass().getSimpleName()).append("[");
				}
			} else {
				sb.append("Untyped value[");
			}

			if (type == XMLRPCTypes.BASE64) {
				if (value == null) {
					sb.append("null");
				} else if (value instanceof Base64) {
					if (((Base64) value).getSize() > 30) {
						sb.append(((Base64) value).getSize()).append(" bytes");
					} else {
						sb.append("\"").append(new String(((Base64) value).getBytes(), StandardCharsets.ISO_8859_1)).append("\"");
					}
				} else {
					sb.append("Invalid Base64 value");
				}
			} else if (type == XMLRPCTypes.STRING) {
				if (value == null) {
					sb.append("null");
				} else {
					sb.append("\"").append(value).append("\"");
				}
			} else {
				sb.append(value);
			}
			sb.append("]");
			return sb.toString();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <value>} element into a
		 * {@link Value} using the specified {@link XMLStreamReader}. This
		 * method is called via
		 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
		 * shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link Value} containing the parsed content of the
		 *         {@code <value>} element.
		 * @throws XMLRPCException If a parsing error occurs during the
		 *             operation.
		 * @throws XMLStreamException If a stream error occurs during the
		 *             operation.
		 */
		public static Value<?> read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!"value".equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at a <value> element");
			}
			XMLRPCTypes type = null;
			Value<?> result = null;
			String stringValue;
			while (reader.hasNext()) {
				int eventType = reader.next();
				switch (eventType) {
					case XMLStreamReader.START_ELEMENT:
						if (type == null) {
							type = XMLRPCTypes.typeOf(reader.getLocalName());
							switch (type) {
								case ARRAY:
									result = new ValueArray(Array.read(reader));
									break;
								case BASE64:
									result = new ValueBase64(Base64.read(reader));
									break;
								case BOOLEAN:
									result = new ValueBoolean(readCharacters(reader));
									break;
								case DATETIME_ISO8601:
									result = new ValueDateTime(DateTime.read(reader));
									break;
								case DOUBLE:
									stringValue = readCharacters(reader);
									try {
										result = isBlank(stringValue) ?
											new ValueDouble(null) :
											new ValueDouble(Double.valueOf(stringValue));
									} catch (NumberFormatException e) {
										throw new XMLRPCException("XML-RPC: Invalid <double> value \"" + stringValue + "\"", e);
									}
									break;
								case INT:
									stringValue = readCharacters(reader);
									try {
										result = isBlank(stringValue) ?
											new ValueInt(null) :
											new ValueInt(Integer.valueOf(stringValue));
									} catch (NumberFormatException e) {
										throw new XMLRPCException("XML-RPC: Invalid <int> value \"" + stringValue + "\"", e);
									}
									break;
								case STRUCT:
									result = new ValueStruct(Struct.read(reader));
									break;
								case STRING:
								default:
									result = new ValueString(readCharacters(reader));
									break;
							}
						} else {
							throw new XMLRPCException("XML-RPC: Unexpected <value> property \"" + reader.getLocalName() + "\"");
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						String elementName = reader.getLocalName();
						if (type != null && type.toString().equals(elementName)) {
							break;
						} else if ("value".equals(elementName)) {
							return result;
						} else {
							throw new XMLRPCException("XML-RPC: Invalid <value> element");
						}
					default:
						// Ignore
				}
			}
			throw new XMLRPCException("XML-RPC: Premature end of stream");
		}
	}

	/**
	 * A predefined {@link Value} of type {@link Array}.
	 */
	public static class ValueArray extends Value<Array> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link Array}.
		 */
		public ValueArray(Array value) {
			super(XMLRPCTypes.ARRAY, value);
		}
	}

	/**
	 * A predefined {@link Value} of type {@link Base64}.
	 */
	public static class ValueBase64 extends Value<Base64> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link Base64}.
		 */
		public ValueBase64(Base64 value) {
			super(XMLRPCTypes.BASE64, value);
		}

		/**
		 * Creates a new instance by parsing the specified {@link String} as a
		 * {@code base64} encoded string.
		 *
		 * @param base64 the {@code base64} encoded {@link String}.
		 * @throws IllegalArgumentException If {@code base64} doesn't conform to
		 *             the lexical value space defined in XML Schema Part 2:
		 *             Datatypes for {@code xsd:base64Binary}.
		 */
		public ValueBase64(String base64) {
			super(XMLRPCTypes.BASE64, new Base64(base64));
		}

		/**
		 * Creates a new instance containing the specified bytes. The byte array
		 * is copied.
		 *
		 * @param value the byte array.
		 */
		public ValueBase64(byte[] value) {
			super(XMLRPCTypes.BASE64, new Base64(value));
		}

		/**
		 * Creates a new instance containing the specified bytes.
		 * <p>
		 * <b>Note:</b> Make sure that the byte array isn't modified if
		 * {@code copy} is {@code false}. Not copying the byte array can save
		 * memory and increase performance for large byte arrays.
		 *
		 * @param value the byte array.
		 * @param copy if {@code true} the byte array is copied, if
		 *            {@code false} the {@code value} instance becomes the
		 *            underlying buffer of this {@link ValueBase64}.
		 */
		public ValueBase64(byte[] value, boolean copy) {
			super(XMLRPCTypes.BASE64, new Base64(value, copy));
		}
	}

	/**
	 * A predefined {@link Value} of type {@link Boolean}.
	 */
	public static class ValueBoolean extends Value<Boolean> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link Boolean}.
		 */
		public ValueBoolean(Boolean value) {
			super(XMLRPCTypes.BOOLEAN, value);
		}

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@code boolean} value.
		 */
		public ValueBoolean(boolean value) {
			super(XMLRPCTypes.BOOLEAN, Boolean.valueOf(value));
		}

		/**
		 * Creates a new instance by parsing the specified {@code int} using
		 * {@link StringUtil#parseBoolean(int, boolean)}.
		 *
		 * @param value the {@code int} value.
		 */
		public ValueBoolean(int value) {
			super(XMLRPCTypes.BOOLEAN, StringUtil.parseBoolean(value, true));
		}

		/**
		 * Creates a new instance by parsing the specified {@link Object} using
		 * {@link StringUtil#parseBoolean(Object, boolean)}.
		 *
		 * @param value the {@link Object}.
		 */
		public ValueBoolean(Object value) {
			super(XMLRPCTypes.BOOLEAN, StringUtil.parseBoolean(value, true));
		}
	}

	/**
	 * A predefined {@link Value} of type {@link DateTime}.
	 */
	public static class ValueDateTime extends Value<DateTime> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link DateTime}.
		 */
		public ValueDateTime(DateTime value) {
			super(XMLRPCTypes.DATETIME_ISO8601, value);
		}

		/**
		 * Creates a new instance by parsing the specified {@link String} as an
		 * {@code ISO 8601} encoded string.
		 *
		 * @param iso8601 the {@code ISO 8601} encoded string.
		 * @throws IllegalArgumentException If {@code iso8601} doesn't conform
		 *             to the lexical value space defined in XML Schema Part 2:
		 *             Datatypes for {@code xsd:dateTime}.
		 */
		public ValueDateTime(String iso8601) {
			super(XMLRPCTypes.DATETIME_ISO8601, new DateTime(iso8601));
		}

		/**
		 * Creates a new instance with using the specified value.
		 *
		 * @param value the {@link Calendar}.
		 */
		public ValueDateTime(Calendar value) {
			super(XMLRPCTypes.DATETIME_ISO8601, new DateTime(value));
		}

		/**
		 * Creates a new instance with using the specified value.
		 *
		 * @param value the {@link Date}.
		 */
		public ValueDateTime(Date value) {
			super(XMLRPCTypes.DATETIME_ISO8601, new DateTime(value));
		}
	}

	/**
	 * A predefined {@link Value} of type {@link Double}.
	 */
	public static class ValueDouble extends Value<Double> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link Double}.
		 */
		public ValueDouble(Double value) {
			super(XMLRPCTypes.DOUBLE, value);
		}

		/**
		 * Creates a new instance using the specified value.
		 *
		 * @param value the {@code double} value.
		 */
		public ValueDouble(double value) {
			super(XMLRPCTypes.DOUBLE, Double.valueOf(value));
		}

		/**
		 * Creates a new instance by converting the specified {@link Number} to
		 * a {@link Double}.
		 *
		 * @param value the {@link Number}.
		 */
		public ValueDouble(Number value) {
			super(XMLRPCTypes.DOUBLE, value == null ? null : Double.valueOf(value.doubleValue()));
		}
	}

	/**
	 * A predefined {@link Value} of type {@link Integer}.
	 */
	public static class ValueInt extends Value<Integer> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link Integer}.
		 */
		public ValueInt(Integer value) {
			super(XMLRPCTypes.INT, value);
		}

		/**
		 * Creates a new instance using the specified value.
		 *
		 * @param value the {@code int} value.
		 */
		public ValueInt(int value) {
			super(XMLRPCTypes.INT, Integer.valueOf(value));
		}

		/**
		 * Creates a new instance by converting the specified {@link Number} to
		 * an {@link Integer}.
		 *
		 * @param value the {@link Number}.
		 */
		public ValueInt(Number value) {
			super(XMLRPCTypes.INT, value == null ? null : Integer.valueOf(value.intValue()));
		}
	}

	/**
	 * A predefined {@link Value} of type {@link String}.
	 */
	public static class ValueString extends Value<String> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link String}.
		 */
		public ValueString(String value) {
			super(XMLRPCTypes.STRING, value);
		}
	}

	/**
	 * A predefined {@link Value} of type {@link Struct}.
	 */
	public static class ValueStruct extends Value<Struct> {

		/**
		 * Creates a new instance with the specified value.
		 *
		 * @param value the {@link String}.
		 */
		public ValueStruct(Struct value) {
			super(XMLRPCTypes.STRUCT, value);
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC} {@code <array>}
	 * element. An {@link Array} consists of any number of {@link Value}
	 * elements.
	 *
	 * @author Nadahar
	 */
	public static class Array extends ArrayList<Value<?>> {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an empty instance with an initial capacity of ten.
		 */
		public Array() {
			super();
		}

		/**
		 * Constructs an instance containing the elements of the specified
		 * {@link Collection}, in the order they are returned by the
		 * {@link Collection}'s {@link Iterator}.
		 *
		 * @param collection the {@link Collection} whose elements are to be
		 *            placed into this instance.
		 * @throws NullPointerException If the specified collection is
		 *             {@code null}.
		 */
		public Array(Collection<? extends Value<?>> collection) {
			super(collection);
		}

		/**
		 * Constructs an empty instance with the specified initial capacity.
		 *
		 * @param initialCapacity the initial capacity of the instance.
		 * @throws IllegalArgumentException If the specified initial capacity is
		 *             negative.
		 */
		public Array(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Writes this {@code XML-RPC} {@code <array>} element to the specified
		 * {@link XMLStreamWriter}.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			writer.writeStartElement("array");
			if (isEmpty()) {
				writer.writeEmptyElement("data");
			} else {
				writer.writeStartElement("data");
				for (Value<?> value : this) {
					if (value == null) {
						writer.writeEmptyElement("value");
					} else {
						value.write(writer);
					}
				}
				writer.writeEndElement();
			}
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append("{\n");
			for (Value<?> value : this) {
				sb.append("  ").append(value).append("\n");
			}
			sb.append("}");
			return sb.toString();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <array>} element into an
		 * {@link Array} using the specified {@link XMLStreamReader}. This
		 * method is called via
		 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
		 * shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link Array} containing the parsed content of the
		 *         {@code <array>} element.
		 * @throws XMLRPCException If a parsing error occurs during the
		 *             operation.
		 * @throws XMLStreamException If a stream error occurs during the
		 *             operation.
		 */
		public static Array read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!"array".equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at an <array> element");
			}
			boolean data = false;
			String elementName;
			Array result = new Array();
			while (reader.hasNext()) {
				int eventType = reader.next();
				switch (eventType) {
					case XMLStreamReader.START_ELEMENT:
						elementName = reader.getLocalName();
						if (!data && "data".equals(elementName)) {
							data = true;
						} else if (data && "value".equals(elementName)) {
							result.add(Value.read(reader));
						} else {
							throw new XMLRPCException("XML-RPC: Unexpected <array> property \"" + elementName + "\"");
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						elementName = reader.getLocalName();
						if (data && "data".equals(elementName)) {
							data = false;
						} else if (!data && "array".equals(elementName)) {
							return result;
						} else {
							throw new XMLRPCException("XML-RPC: Invalid <array> element");
						}
					default:
						// Ignore
				}
			}
			throw new XMLRPCException("XML-RPC: Premature end of stream");
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC} {@code <base64>}
	 * element. A {@link Base64} consists of a byte array represented as a
	 * {@code base64} encoded string.
	 *
	 * @author Nadahar
	 */
	public static class Base64 {

		private byte[] buffer;

		/**
		 * Creates a new instance by parsing the specified {@link String} as a
		 * {@code base64} encoded string.
		 *
		 * @param base64String the {@code base64} encoded {@link String}.
		 * @throws IllegalArgumentException If {@code base64String} doesn't
		 *             conform to the lexical value space defined in XML Schema
		 *             Part 2: Datatypes for {@code xsd:base64Binary}.
		 */
		public Base64(String base64String) {
			if (isBlank(base64String)) {
				buffer = null;
			} else {
				try {
					buffer = DatatypeConverter.parseBase64Binary(base64String);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid base64 string \"" + base64String + "\"", e);
				}
			}
		}

		/**
		 * Creates a new instance using the specified bytes. The byte array is
		 * copied.
		 *
		 * @param bytes the byte array.
		 */
		public Base64(byte[] bytes) {
			this(bytes, true);
		}

		/**
		 * Creates a new instance using the specified bytes.
		 * <p>
		 * <b>Note:</b> Make sure that the byte array isn't modified if
		 * {@code copy} is {@code false}. Not copying the byte array can save
		 * memory and increase performance for large byte arrays.
		 *
		 * @param bytes the byte array.
		 * @param copy if {@code true} the byte array is copied, if
		 *            {@code false} the {@code value} instance becomes the
		 *            underlying buffer of this {@link ValueBase64}.
		 */
		@SuppressFBWarnings("EI_EXPOSE_REP2")
		public Base64(byte[] bytes, boolean copy) {
			if (bytes == null) {
				buffer = null;
			} else if (copy) {
				buffer = new byte[bytes.length];
				System.arraycopy(bytes, 0, buffer, 0, bytes.length);
			} else {
				buffer = bytes;
			}
		}

		/**
		 * @return The number of bytes in the underlying byte buffer.
		 */
		public int getSize() {
			return buffer == null ? 0 : buffer.length;
		}

		/**
		 * @return A copy of the byte array of this {@link Base64} instance or
		 *         {@code null}.
		 */
		public byte[] getBytes() {
			return getBytes(true);
		}

		/**
		 * Returns the bytes of this {@link Base64} instance either as a copy or
		 * as the same instance as held by this instance.
		 * <p>
		 * <b>Note:</b> Make sure that the byte array isn't modified if
		 * {@code copy} is {@code false}. Not copying the byte array can save
		 * memory and increase performance for large byte arrays.
		 *
		 * @param copy if {@code true} the returned byte array is a copy, if
		 *            {@code false} it's the actual byte array held by this
		 *            instance.
		 * @return The byte array of this {@link Base64} instance or
		 *         {@code null}.
		 */
		@SuppressFBWarnings("EI_EXPOSE_REP")
		public byte[] getBytes(boolean copy) {
			if (buffer == null) {
				return null;
			}
			if (copy) {
				byte[] result = new byte[buffer.length];
				System.arraycopy(buffer, 0, result, 0, buffer.length);
				return result;
			}
			return buffer;
		}

		/**
		 * @return The {@code base64} encoded {@link String} representing this
		 *         {@link Base64} instance or {@code null}.
		 */
		public String getBase64String() {
			return buffer == null ? null : DatatypeConverter.printBase64Binary(buffer);
		}

		/**
		 * Writes this {@code XML-RPC} {@code <base64>} element to the specified
		 * {@link XMLStreamWriter}.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			if (buffer == null) {
				writer.writeEmptyElement(XMLRPCTypes.BASE64.toString());
				return;
			}
			writer.writeStartElement(XMLRPCTypes.BASE64.toString());
			writer.writeCharacters(getBase64String());
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			return buffer == null ? "" : getBase64String();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <base64>} element into a
		 * {@link Base64} using the specified {@link XMLStreamReader}. This
		 * method is called via
		 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
		 * shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link Base64} containing the parsed content of the
		 *         {@code <base64>} element.
		 * @throws XMLRPCException If a parsing error occurs during the
		 *             operation.
		 * @throws XMLStreamException If a stream error occurs during the
		 *             operation.
		 */
		public static Base64 read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!XMLRPCTypes.BASE64.toString().equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at a <base64> element");
			}
			try {
				return new Base64(readCharacters(reader));
			} catch (IllegalArgumentException e) {
				throw new XMLRPCException("XML-RPC: Invalid <base64> element", e);
			}
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC}
	 * {@code <dateTime.iso8601>} element. A {@link DateTime} consists of a
	 * {@link Calendar} instance represented as an {@code ISO 8601} encoded
	 * string.
	 *
	 * @author Nadahar
	 */
	public static class DateTime {

		private Calendar calendar;

		/**
		 * Creates a new instance by parsing the specified {@link String} as an
		 * {@code ISO 8601} encoded string.
		 *
		 * @param iso8601String the {@code ISO 8601} encoded {@link String}.
		 * @throws IllegalArgumentException If {@code iso8601String} doesn't
		 *             conform to the lexical value space defined in XML Schema
		 *             Part 2: Datatypes for {@code xsd:dateTime}.
		 */
		public DateTime(String iso8601String) {
			if (isBlank(iso8601String)) {
				calendar = null;
			} else {
				try {
					calendar = DatatypeConverter.parseDateTime(iso8601String);
				} catch (IllegalArgumentException e) {
					throw new IllegalArgumentException("Invalid ISO 8601 string \"" + iso8601String + "\"", e);
				}
			}
		}

		/**
		 * Creates a new instance using the specified {@link Calendar} instance.
		 *
		 * @param calendar the {@link Calendar} to use.
		 */
		public DateTime(Calendar calendar) {
			this.calendar = calendar == null ? null : (Calendar) calendar.clone();
		}

		/**
		 * Creates a new instance by converting the specified {@link Date}
		 * instance.
		 *
		 * @param date the {@link Date} to use.
		 */
		public DateTime(Date date) {
			if (date == null) {
				calendar = null;
			} else {
				calendar = Calendar.getInstance();
				calendar.setTime(date);
			}
		}

		/**
		 * @return A copy of the underlying {@link Calendar} instance or
		 *         {@code null};
		 */
		public Calendar getCalendar() {
			return calendar == null ? null : (Calendar) calendar.clone();
		}

		/**
		 * @return A {@link Date} instance representing the underlying
		 *         {@code Calendar}'s time value (millisecond offset from the
		 *         {@code Epoch}).
		 */
		public Date getDate() {
			return calendar == null ? null : calendar.getTime();
		}

		/**
		 * @return The {@code ISO 8601} encoded {@link String} representing this
		 *         {@link DateTime} instance or {@code null}.
		 */
		public String getISO8610String() {
			return calendar == null ? null : DatatypeConverter.printDateTime(calendar);
		}

		/**
		 * Writes this {@code XML-RPC} {@code <dateTime.iso8601>} element to the
		 * specified {@link XMLStreamWriter}.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			if (calendar == null) {
				writer.writeEmptyElement(XMLRPCTypes.DATETIME_ISO8601.toString());
				return;
			}
			writer.writeStartElement(XMLRPCTypes.DATETIME_ISO8601.toString());
			writer.writeCharacters(getISO8610String());
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			return calendar == null ? "" : getISO8610String();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <dateTime.iso8601>} element into a
		 * {@link DateTime} using the specified {@link XMLStreamReader}. This
		 * method is called via
		 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
		 * shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link DateTime} containing the parsed content of the
		 *         {@code <dateTime.iso8601>} element.
		 * @throws XMLRPCException If a parsing error occurs during the
		 *             operation.
		 * @throws XMLStreamException If a stream error occurs during the
		 *             operation.
		 */
		public static DateTime read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!XMLRPCTypes.DATETIME_ISO8601.toString().equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at a <dateTime.iso8601> element");
			}
			try {
				return new DateTime(readCharacters(reader));
			} catch (IllegalArgumentException e) {
				throw new XMLRPCException("XML-RPC: Invalid <dateTime.iso8601> element", e);
			}
		}
	}

	/**
	 * This class is used to represent an {@code XML-RPC} {@code <struct>}
	 * element. A {@link Struct} consists of any number of {@link Member}
	 * elements.
	 *
	 * @author Nadahar
	 */
	public static class Struct extends HashMap<String, Member<? extends Value<?>, ?>> {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs an empty {@link Struct} with the default initial capacity
		 * (16) and the default load factor (0.75).
		 */
		public Struct() {
			super();
		}

		/**
		 * Constructs an empty {@link Struct} with the specified initial
		 * capacity and the default load factor (0.75).
		 *
		 * @param initialCapacity the initial capacity.
		 * @throws IllegalArgumentException If the initial capacity is negative.
		 */
		public Struct(int initialCapacity) {
			super(initialCapacity);
		}

		/**
		 * Constructs an empty {@link Struct} with the specified initial
		 * capacity and load factor.
		 *
		 * @param initialCapacity the initial capacity.
		 * @param loadFactor the load factor.
		 * @throws IllegalArgumentException If the initial capacity is negative
		 *             or the load factor is non-positive.
		 */
		public Struct(int initialCapacity, float loadFactor) {
			super(initialCapacity, loadFactor);
		}

		/**
		 * Constructs a new {@link Struct}> with the same mappings as the
		 * specified {@link Map}.  The {@link Struct} is created with
		 * default load factor (0.75) and an initial capacity sufficient to
		 * hold the mappings in the specified {@link Map}.
		 *
		 * @param   map the map whose mappings are to be placed in this {@link Struct}.
		 * @throws  NullPointerException If the {@code map} is {@code null}.
		 */
		public Struct(Map<? extends String, ? extends Member<? extends Value<?>, ?>> map) {
			super(map);
		}

		/**
		 * Puts the specified {@link Member} into this {@link Struct} using
		 * {@link Member#getName()} as the key. If the {@link Struct} previously
		 * contained a mapping for the name, the old value is replaced.
		 *
		 * @param member the {@link Member} to be set in the {@link Struct}.
		 * @return The previous value associated with the name, or {@code null}
		 *         if there was no mapping for this name or if the
		 *         {@link Struct} previously associated {@code null} with the
		 *         name.
		 */
		public Member<? extends Value<?>, ?> put(Member<? extends Value<?>, ?> member) {
			return put(member == null ? null : member.getName(), member);
		}

		/**
		 * Writes this {@code XML-RPC} {@code <struct>} element to the specified
		 * {@link XMLStreamWriter}.
		 *
		 * @param writer the {@link XMLStreamWriter} to write to.
		 * @throws XMLStreamException If an error occurs during the operation.
		 */
		public void write(XMLStreamWriter writer) throws XMLStreamException {
			if (isEmpty()) {
				writer.writeEmptyElement("struct");
				return;
			}
			writer.writeStartElement("struct");
			for (Member<? extends Value<?>, ?> member : values()) {
				member.write(writer);
			}
			writer.writeEndElement();
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder(getClass().getSimpleName());
			sb.append("{\n");
			for (Member<? extends Value<?>, ?> member : values()) {
				sb.append("  ").append(member).append("\n");
			}
			sb.append("}");
			return sb.toString();
		}

		/**
		 * Parses an {@code XML-RPC} {@code <struct>} element into a
		 * {@link Struct} using the specified {@link XMLStreamReader}. This
		 * method is called via
		 * {@link XMLRPCUtil#readMethodResponse(XMLStreamReader)} as needed and
		 * shouldn't normally be called directly.
		 *
		 * @param reader the {@link XMLStreamReader} whose content to parse.
		 * @return The new {@link Struct} containing the parsed content of the
		 *         {@code <struct>} element.
		 * @throws XMLRPCException If a parsing error occurs during the
		 *             operation.
		 * @throws XMLStreamException If a stream error occurs during the
		 *             operation.
		 */
		public static Struct read(XMLStreamReader reader) throws XMLRPCException, XMLStreamException {
			if (!"struct".equals(reader.getLocalName())) {
				throw new XMLRPCException("XML-RPC: Cursor isn't at a <struct> element");
			}
			Struct result = new Struct();
			while (reader.hasNext()) {
				int eventType = reader.next();
				switch (eventType) {
					case XMLStreamReader.START_ELEMENT:
						String elementName = reader.getLocalName();
						switch (elementName) {
							case "member":
								Member<? extends Value<?>, ?> member = Member.read(reader);
								if (member == null) {
									throw new XMLRPCException("XML-RPC: Member is null");
								}
								if (isBlank(member.getName())) {
									throw new XMLRPCException("XML-RPC: Member has no name");
								}
								result.put(member);
								break;
							default:
								throw new XMLRPCException("XML-RPC: Unexpected <struct> property \"" + elementName + "\"");
						}
						break;
					case XMLStreamReader.END_ELEMENT:
						if (!"struct".equals(reader.getLocalName())) {
							throw new XMLRPCException("XML-RPC: Invalid <struct> element");
						}
						return result;
					default:
						// Ignore
				}
			}
			throw new XMLRPCException("XML-RPC: Premature end of stream");
		}
	}

	/**
	 * This {@code enum} is used to represent the {@code XML-RPC} types.
	 */
	public static enum XMLRPCTypes {

		/** The {@code XML-RPC} {@code <array>} type */
		ARRAY,

		/** The {@code XML-RPC} {@code <base64>} type */
		BASE64,

		/** The {@code XML-RPC} {@code <boolean>} type */
		BOOLEAN,

		/** The {@code XML-RPC} {@code <dateTime.iso8601>} type */
		DATETIME_ISO8601,

		/** The {@code XML-RPC} {@code <double>} type */
		DOUBLE,

		/** The {@code XML-RPC} {@code <int>} or {@code <i4>} type */
		INT,

		/** The {@code XML-RPC} {@code <string>} type */
		STRING,

		/** The {@code XML-RPC} {@code <struct>} type */
		STRUCT;

		@Override
		public String toString() {
			switch (this) {
				case ARRAY:
					return "array";
				case BASE64:
					return "base64";
				case BOOLEAN:
					return "boolean";
				case DATETIME_ISO8601:
					return "dateTime.iso8601";
				case DOUBLE:
					return "double";
				case INT:
					return "int";
				case STRING:
					return "string";
				case STRUCT:
					return "struct";
				default:
					throw new AssertionError("Unimplemented XMLRPCType " + name());
			}
		};

		/**
		 * Parses the specified {@link String} into the corresponding
		 * {@link XMLRPCTypes} value.
		 *
		 * @param value the {@link String} to parse.
		 * @return The corresponding {@link XMLRPCTypes} value.
		 * @throws XMLRPCException If {@code value} isn't a valid
		 *             {@code XML-RPC} type.
		 */
		public static XMLRPCTypes typeOf(String value) throws XMLRPCException {
			if (isBlank(value)) {
				throw new XMLRPCException("XML-RPC: Blank type");
			}
			switch (value.toLowerCase(Locale.ROOT)) {
				case "array":
					return ARRAY;
				case "base64":
					return BASE64;
				case "boolean":
					return BOOLEAN;
				case "dateTime.iso8601":
					return DATETIME_ISO8601;
				case "double":
					return DOUBLE;
				case "i4":
				case "int":
					return INT;
				case "string":
					return STRING;
				case "struct":
					return STRUCT;
				default:
					throw new XMLRPCException("XML-RPC: Unknown type \"" + value + "\"");
			}
		}
	}

	/**
	 * Signals that a problem of some sort has occurred while processing
	 * {@code XML-RPC} data.
	 *
	 * @author Nadahar
	 */
	public static class XMLRPCException extends IOException {
		private static final long serialVersionUID = 1L;

		/**
		 * Creates a new instance with the specified detail message and cause.
		 *
		 * @param message the detail message.
		 * @param cause the {@link Throwable} causing this
		 *            {@link XMLRPCException} if any.
		 */
		public XMLRPCException(String message, Throwable cause) {
			super(message, cause);
		}

		/**
		 * Creates a new instance with the specified detail message.
		 *
		 * @param message the detail message.
		 */
		public XMLRPCException(String message) {
			super(message);
		}
	}
}
