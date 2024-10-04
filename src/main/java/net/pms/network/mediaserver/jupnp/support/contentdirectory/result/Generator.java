/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result;

import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dlna.DLNA;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.pv.PV;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.sec.SEC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import org.jupnp.model.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Generator {

	private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);
	private static final String XMLNS_NAMESPACE_URI = "http://www.w3.org/2000/xmlns/";

	/**
	 * Generates an XML representation of the content model.
	 *
	 * @param content The content model.
	 * @return An XML representation.
	 * @throws Exception
	 */
	public String generate(Result content) {
		try {
			return documentToString(buildDOM(content), true);
		} catch (ParserConfigurationException | TransformerException ex) {
			LOGGER.trace("\n" + ex);
		}
		return "";
	}

	protected String documentToString(Document document, boolean omitProlog) throws TransformerException {
		TransformerFactory transFactory = TransformerFactory.newInstance();

		Transformer transformer = transFactory.newTransformer();

		if (omitProlog) {
			// TODO: UPNP VIOLATION: Terratec Noxon Webradio fails when DIDL content has a prolog
			// No XML prolog! This is allowed because it is UTF-8 encoded and required
			// because broken devices will stumble on SOAP messages that contain (even
			// encoded) XML prologs within a message body.
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		}

		// Again, Android 2.2 fails hard if you try this.
		//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		StringWriter out = new StringWriter();
		transformer.transform(new DOMSource(document), new StreamResult(out));
		return out.toString();
	}

	protected Document buildDOM(Result content) throws ParserConfigurationException {

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);

		Document d = factory.newDocumentBuilder().newDocument();

		generateRoot(content, d);

		return d;
	}

	protected void generateRoot(Result content, Document descriptor) {
		Element rootElement = descriptor.createElementNS(Result.NAMESPACE_URI, "DIDL-Lite");
		descriptor.appendChild(rootElement);

		rootElement.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns:upnp", UPNP.NAMESPACE_URI);
		rootElement.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns:dc", DC.NAMESPACE_URI);
		rootElement.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns:dlna", DLNA.NAMESPACE_URI);
		rootElement.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns:sec", SEC.NAMESPACE_URI);
		rootElement.setAttributeNS(XMLNS_NAMESPACE_URI, "xmlns:pv", PV.NAMESPACE_URI);

		for (Container container : content.getContainers()) {
			if (container == null) {
				continue;
			}
			generateContainer(descriptor, rootElement, container);
		}

		for (Item item : content.getItems()) {
			if (item == null) {
				continue;
			}
			generateItem(descriptor, rootElement, item);
		}

		generateDescriptions(descriptor, rootElement, content);
	}

	protected void generateContainer(Document descriptor, Element parent, Container container) {

		if (container.getUpnpClassName() == null) {
			throw new RuntimeException("Missing 'upnp:class' element for container: " + container.getId());
		}

		Element containerElement = XMLUtil.appendNewElement(descriptor, parent, "container");

		appendAttributes(descriptor, containerElement, container);
		appendProperties(descriptor, containerElement, container, DC.NAMESPACE.class, DC.NAMESPACE_URI);
		appendProperties(descriptor, containerElement, container, UPNP.NAMESPACE.class, UPNP.NAMESPACE_URI);
		appendProperties(descriptor, containerElement, container, DIDL_LITE.NAMESPACE.class, DIDL_LITE.NAMESPACE_URI);
	}

	protected void generateItem(Document descriptor, Element parent, Item item) {
		Element itemElement = XMLUtil.appendNewElement(descriptor, parent, "item");

		appendAttributes(descriptor, itemElement, item);
		appendProperties(descriptor, itemElement, item, DC.NAMESPACE.class, DC.NAMESPACE_URI);
		appendProperties(descriptor, itemElement, item, UPNP.NAMESPACE.class, UPNP.NAMESPACE_URI);
		appendProperties(descriptor, itemElement, item, SEC.NAMESPACE.class, SEC.NAMESPACE_URI);
		appendProperties(descriptor, itemElement, item, DIDL_LITE.NAMESPACE.class, DIDL_LITE.NAMESPACE_URI);
	}

	protected void generateDescriptions(Document descriptor, Element parent, Result content) {
		for (Property<?> property : content.getDescriptions()) {
			Element element = descriptor.createElementNS(DIDL_LITE.NAMESPACE_URI, property.getQualifiedName());
			appendAttributes(descriptor, element, property);
			element.setTextContent(property.toString());
			parent.appendChild(element);
		}
	}

	protected void appendAttributes(Document descriptor, Element parent, Property property) {
		for (Property<?> attributeProperty : property.getDependentProperties().get()) {
			String namespaceURI = attributeProperty.getNamespaceURI();
			Attr attr;
			if (namespaceURI != null) {
				attr = descriptor.createAttributeNS(namespaceURI, attributeProperty.getQualifiedName());
			} else {
				attr = descriptor.createAttribute(attributeProperty.getQualifiedName());
			}
			attr.setTextContent(attributeProperty.toString());
			if (namespaceURI != null) {
				String pre = descriptor.lookupPrefix(namespaceURI);
				attr.setPrefix(pre);
				parent.setAttributeNodeNS(attr);
			} else {
				parent.setAttributeNode(attr);
			}
		}
	}

	protected void appendProperties(Document descriptor, Element parent, BaseObject object,
			Class<?> namespace,
			String namespaceURI) {
		for (Property<?> property : object.getProperties().getPropertiesInstanceOf(namespace)) {
			Element element = descriptor.createElementNS(namespaceURI, property.getQualifiedName());
			String pre = descriptor.lookupPrefix(namespaceURI);
			element.setPrefix(pre);
			appendAttributes(descriptor, element, property);
			element.setTextContent(property.toString());
			parent.appendChild(element);
		}
	}

}
