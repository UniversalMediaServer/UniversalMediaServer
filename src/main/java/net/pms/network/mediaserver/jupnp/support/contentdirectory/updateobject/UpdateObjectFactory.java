package net.pms.network.mediaserver.jupnp.support.contentdirectory.updateobject;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
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

		if ("upnp:albumArtURI".equalsIgnoreCase(getNodeName(currentTagNode)) || "upnp:albumArtURI".equalsIgnoreCase(getNodeName(newTagNode))) {
			return new AlbumArtUriHandler(objectResource, currentTagNode, newTagNode);
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
	 * Split CSV list around "," while handling escaped \, comma and commas
	 * inside XML element content (e.g. base64 data URIs).
	 */
	public static String[] getFragments(String tagValue) {
		if (tagValue == null) {
			return new String[]{""};
		}

		List<String> fragments = new ArrayList<>();
		int depth = 0;
		int start = 0;
		boolean inDataUri = StringUtils.trim(tagValue).startsWith("data:");

		for (int i = 0; i < tagValue.length(); i++) {
			char c = tagValue.charAt(i);
			if (c == '<') {
				if (i + 1 < tagValue.length() && tagValue.charAt(i + 1) == '/') {
					depth--;
				} else {
					depth++;
				}
			} else if (c == ',' && depth == 0) {
				if (i > 0 && tagValue.charAt(i - 1) == '\\') {
					continue;
				}
				// Skip the comma that separates mediatype from data in a data: URI
				if (inDataUri) {
					inDataUri = false;
					continue;
				}
				fragments.add(unescape(StringUtils.trim(tagValue.substring(start, i))));
				start = i + 1;
				// Check if the next fragment starts with "data:"
				inDataUri = StringUtils.trim(tagValue.substring(start)).startsWith("data:");
			}
		}
		fragments.add(unescape(StringUtils.trim(tagValue.substring(start))));
		return fragments.toArray(new String[0]);
	}

	private static String unescape(String value) {
		return value.replace("\\,", ",").replace("\\\\", "\\");
	}

}
