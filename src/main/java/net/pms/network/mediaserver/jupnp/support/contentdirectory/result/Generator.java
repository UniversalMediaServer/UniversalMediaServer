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
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.sec.SEC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import org.jupnp.model.XMLUtil;
import org.jupnp.support.model.DescMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Generator {

	private static final Logger LOGGER = LoggerFactory.getLogger(Generator.class);

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

		// rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:didl", Result.NAMESPACE_URI);
		rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:upnp", UPNP.NAMESPACE_URI);
		rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:dc", DC.NAMESPACE_URI);
		rootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:sec", SEC.NAMESPACE_URI);

		for (Container container : content.getContainers()) {
			if (container == null) {
				continue;
			}
			generateContainer(container, descriptor, rootElement);
		}

		for (Item item : content.getItems()) {
			if (item == null) {
				continue;
			}
			generateItem(item, descriptor, rootElement);
		}

		for (DescMeta<?> descMeta : content.getDescMetadata()) {
			if (descMeta == null) {
				continue;
			}
			generateDescMetadata(descMeta, descriptor, rootElement);
		}
	}

	protected void generateContainer(Container container, Document descriptor, Element parent) {

		if (container.getUpnpClassName() == null) {
			throw new RuntimeException("Missing 'upnp:class' element for container: " + container.getId());
		}

		Element containerElement = XMLUtil.appendNewElement(descriptor, parent, "container");

		appendAttributes(descriptor, containerElement, container);
		appendProperties(descriptor, containerElement, container, "upnp:", UPNP.NAMESPACE.class, UPNP.NAMESPACE_URI);
		appendProperties(descriptor, containerElement, container, "dc:", DC.NAMESPACE.class, DC.NAMESPACE_URI);
		appendProperties(descriptor, containerElement, container, "", DIDL_LITE.NAMESPACE.class, DIDL_LITE.NAMESPACE_URI);

		for (DescMeta<?> descMeta : container.getDescMetadata()) {
			if (descMeta == null) {
				continue;
			}
			generateDescMetadata(descMeta, descriptor, containerElement);
		}
	}

	protected void generateItem(Item item, Document descriptor, Element parent) {
		Element itemElement = XMLUtil.appendNewElement(descriptor, parent, "item");

		appendAttributes(descriptor, itemElement, item);

		appendProperties(descriptor, itemElement, item, "upnp:", UPNP.NAMESPACE.class, UPNP.NAMESPACE_URI);
		appendProperties(descriptor, itemElement, item, "dc:", DC.NAMESPACE.class, DC.NAMESPACE_URI);
		appendProperties(descriptor, itemElement, item, "sec:", SEC.NAMESPACE.class, SEC.NAMESPACE_URI);
		appendProperties(descriptor, itemElement, item, "", DIDL_LITE.NAMESPACE.class, DIDL_LITE.NAMESPACE_URI);

		for (DescMeta<?> descMeta : item.getDescMetadata()) {
			if (descMeta == null) {
				continue;
			}
			generateDescMetadata(descMeta, descriptor, itemElement);
		}
	}

	protected void generateResource(Res resource, Document descriptor, Element parent) {

		if (resource.getValue() == null) {
			throw new RuntimeException("Missing resource URI value" + resource);
		}
		if (resource.getProtocolInfo() == null) {
			throw new RuntimeException("Missing resource protocol info: " + resource);
		}

		Element resourceElement = XMLUtil.appendNewElement(descriptor, parent, "res", resource.getValue());
		resourceElement.setAttribute("protocolInfo", resource.getProtocolInfo().toString());
		if (resource.getImportUri() != null) {
			resourceElement.setAttribute("importUri", resource.getImportUri().toString());
		}
		if (resource.getSize() != null) {
			resourceElement.setAttribute("size", resource.getSize().toString());
		}
		if (resource.getDuration() != null) {
			resourceElement.setAttribute("duration", resource.getDuration());
		}
		if (resource.getBitrate() != null) {
			resourceElement.setAttribute("bitrate", resource.getBitrate().toString());
		}
		if (resource.getSampleFrequency() != null) {
			resourceElement.setAttribute("sampleFrequency", resource.getSampleFrequency().toString());
		}
		if (resource.getBitsPerSample() != null) {
			resourceElement.setAttribute("bitsPerSample", resource.getBitsPerSample().toString());
		}
		if (resource.getNrAudioChannels() != null) {
			resourceElement.setAttribute("nrAudioChannels", resource.getNrAudioChannels().toString());
		}
		if (resource.getColorDepth() != null) {
			resourceElement.setAttribute("colorDepth", resource.getColorDepth().toString());
		}
		if (resource.getProtection() != null) {
			resourceElement.setAttribute("protection", resource.getProtection());
		}
		if (resource.getResolution() != null) {
			resourceElement.setAttribute("resolution", resource.getResolution());
		}
	}

	protected void generateDescMetadata(DescMeta<?> descMeta, Document descriptor, Element parent) {

		if (descMeta.getId() == null) {
			throw new RuntimeException("Missing id of description metadata: " + descMeta);
		}
		if (descMeta.getNameSpace() == null) {
			throw new RuntimeException("Missing namespace of description metadata: " + descMeta);
		}

		Element descElement = XMLUtil.appendNewElement(descriptor, parent, "desc");
		descElement.setAttribute("id", descMeta.getId());
		descElement.setAttribute("nameSpace", descMeta.getNameSpace().toString());
		if (descMeta.getType() != null) {
			descElement.setAttribute("type", descMeta.getType());
		}
		populateDescMetadata(descElement, descMeta);
	}

	/**
	 * Expects an <code>org.w3c.Document</code> as metadata, copies nodes of the
	 * document into the DIDL content.
	 * <p>
	 * This method will ignore the content and log a warning if it's of the
	 * wrong type. If you override
	 * {@link #createDescMetaHandler(org.jupnp.support.model.DescMeta, org.jupnp.xml.SAXParser.Handler)},
	 * you most likely also want to override this method.
	 * </p>
	 *
	 * @param descElement The DIDL content {@code <desc>} element wrapping the
	 * final metadata.
	 * @param descMeta The metadata with a <code>org.w3c.Document</code>
	 * payload.
	 */
	protected void populateDescMetadata(Element descElement, DescMeta<?> descMeta) {
		if (descMeta.getMetadata() instanceof Document doc) {
			NodeList nl = doc.getDocumentElement().getChildNodes();
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				if (n.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}

				Node clone = descElement.getOwnerDocument().importNode(n, true);
				descElement.appendChild(clone);
			}
		} else {
			LOGGER.warn("Unknown desc metadata content, please override populateDescMetadata(): " + descMeta.getMetadata());
		}
	}

	protected void appendAttributes(Document descriptor, Element parent, Property object) {
		for (Property<?> property : object.getDependentProperties().get()) {
			parent.setAttributeNS(
				property.getNamespaceURI(),
				property.getQualifiedName(),
				property.toString()
			);
		}
	}

	protected void appendProperties(Document descriptor, Element parent, BaseObject object, String prefix,
			Class<?> namespace,
			String namespaceURI) {
		for (Property<?> property : object.getProperties().getPropertiesInstanceOf(namespace)) {
			Element el = descriptor.createElementNS(namespaceURI, prefix + property.getQualifiedName());
			parent.appendChild(el);
			property.setOnElement(el);
		}
	}

}
