package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import java.io.IOException;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.pms.store.StoreResource;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.support.contentdirectory.ContentDirectoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class UpdateObjectFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateObjectFactory.class.getName());
	private static final String QUOTED_COMMA_PLACEHOLDER = "XXX1122334455XXX";

	public static IUpdateObjectHandler getUpdateObjectHandler(StoreResource objectResource, String currentTagValue, String newTagValue)
		throws ContentDirectoryException {

		NodeList currentTagNode = getXmlNode(currentTagValue);
		NodeList newTagNode = getXmlNode(newTagValue);

		if (currentTagNode == null && newTagNode == null) {
			throw new ContentDirectoryException(703, "UpdateObject() failed because no newTagValue was supplied");
		}
		if (currentTagNode != null && newTagNode != null && !getNodeName(currentTagNode).equalsIgnoreCase(getNodeName(newTagNode))) {
			throw new ContentDirectoryException(703, "UpdateObject() failed because newTagValue node name doesn't match currentTagValue node name");
		}

		if ("upnp:rating".equalsIgnoreCase(getNodeName(currentTagNode)) || "upnp:rating".equalsIgnoreCase(getNodeName(newTagNode))) {
			return new UpnpRatingHandler(objectResource, currentTagNode, newTagNode);
		}

		LOGGER.warn("NO handler found for tag pair values : '{}' AND '{}'", currentTagValue, newTagValue);
		return null;
	}

	private static String getNodeName(NodeList tagNode) {
		if (tagNode == null) {
			return null;
		}
		if (tagNode.item(0) != null) {
			return tagNode.item(0).getNodeName();
		} else {
			return null;
		}
	}

	public static NodeList getXmlNode(String xml) {
		if (StringUtils.isAllBlank(xml)) {
			return null;
		}

		int countTags = (xml.length() - xml.replaceAll("</", "").length()) / 2;
		if (countTags > 1) {
			StringBuilder sb = new StringBuilder();
			sb.append("<r>").append(xml).append("</r>");
			xml = sb.toString();
		}

		XPath xpath = XPathFactory.newInstance().newXPath();

		InputSource is = new InputSource(new StringReader(xml));
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.parse(is);
			NodeList node = (NodeList) xpath.evaluate("//*", doc, XPathConstants.NODESET);

			if (countTags > 1) {
				return (NodeList) node.item(0);
			}
			return node;
		} catch (SAXException | IOException | XPathExpressionException | ParserConfigurationException e) {
			LOGGER.warn("cannot extract error message", e);
			return null;
		}
	}

	/**
	 * Split CSV list around "," while handling escaped \, comma.
	 */
	public static String[] getFragments(String tagValue) {
		if (tagValue == null) {
			//handle null string (unique add/remove)
			return new String[]{""};
		}

		tagValue = tagValue.replaceAll("\\\\,", QUOTED_COMMA_PLACEHOLDER);

		String[] fragments = tagValue.split(",", -1);
		for (int i = 0; i < fragments.length; i++) {
			fragments[i] = fragments[i].replaceAll(QUOTED_COMMA_PLACEHOLDER, ",");
			fragments[i] = fragments[i].replaceAll("\\\\\\\\", "\\\\");
			fragments[i] = StringUtils.trim(fragments[i]);
		}
		return fragments;
	}

}
