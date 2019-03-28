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

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;

public class XmlUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(XmlUtils.class);
	private static PmsConfiguration configuration = PMS.getConfiguration();

	/**
	 * Returns a new {@code DocumentBuilderFactory} instance with XML External Entity (XXE) processing disabled.
	 *
	 * @return the new {@code DocumentBuilderFactory} instance with XXE processing disabled
	 * @throws ParserConfigurationException if an error occurred while disabling XXE processing
	 * @see DocumentBuilderFactory
	 */
	public static DocumentBuilderFactory xxeDisabledDocumentBuilderFactory() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		if (!configuration.disableExternalEntities()) {
			String FEATURE = null;
			try {
				FEATURE = "http://apache.org/xml/features/disallow-doctype-decl";
				dbf.setFeature(FEATURE, true);    
				FEATURE = "http://xml.org/sax/features/external-general-entities";
				dbf.setFeature(FEATURE, false);   
				FEATURE = "http://xml.org/sax/features/external-parameter-entities";
				dbf.setFeature(FEATURE, false);
				FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
				dbf.setFeature(FEATURE, false);
			} catch (ParserConfigurationException e) {
				LOGGER.info("ParserConfigurationException was thrown. The feature '{}' is probably not supported by the XML processor.", FEATURE);
			}

			dbf.setXIncludeAware(false);
			dbf.setExpandEntityReferences(false);
		}
		
		return dbf;
	}

	/**
	 * Returns a new {@code TransformerFactory} instance with XML External Entity (XXE) processing disabled.
	 *
	 * @return the new {@code TransformerFactory} instance with XXE processing disabled
	 * @see TransformerFactory
	 */
	public static TransformerFactory xxeDisabledTransformerFactory() {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		if (!configuration.disableExternalEntities()) {
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		}
		
		return transformerFactory;
	}

	/**
	 * Returns a new {@code XMLInputFactory} instance with XML External Entity (XXE) processing disabled.
	 *
	 * @return the new {@code XMLInputFactory} instance with XXE processing disabled
	 * @see XMLInputFactory
	 */
	public static XMLInputFactory xxeDisabledXMLInputFactory() {
		XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
		if (!configuration.disableExternalEntities()) {
			xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
		}
		
		return xmlInputFactory;
	}
}
