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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaDatabase;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is the superclass of all cover utility implementations.
 * Cover utilities are responsible for getting media covers based
 * on information given by the caller.
 *
 * @author Nadahar
 */

public abstract class CoverUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoverUtil.class);
	protected static final String encoding = StandardCharsets.UTF_8.name();
	protected static final DLNAMediaDatabase database = PMS.get().getDatabase();
	private static Object instanceLock = new Object();
	private static CoverUtil instance = null;

	/**
	 * Do not instantiate this class, use {@link #get()}
	 */
	protected CoverUtil() {
	}

	/**
	 * Factory method that gets an instance of correct type according to
	 * configuration, or <code>null</code> if no cover utility is configured.
	 *
	 * @return The {@link CoverUtil} instance.
	 */

	public static CoverUtil get() {
		CoverSupplier supplier = PMS.getConfiguration().getAudioThumbnailMethod();
		synchronized (instanceLock) {
			switch (supplier.toInt()) {
				case CoverSupplier.COVER_ART_ARCHIVE_INT:
					if (instance == null || !(instance instanceof CoverArtArchiveUtil)) {
						instance = new CoverArtArchiveUtil();
					}
					break;
				default:
					instance = null;
					break;
			}
			return instance;
		}
	}

	/**
	 * Convenience method to find the first child {@link Element} of the given
	 * name.
	 *
	 * @param element the {@link Element} to search
	 * @param name the name of the child {@link Element}
	 * @return The found {@link Element} or null if not found
	 */
	protected Element getChildElement(Element element, String name) {
		NodeList list = element.getElementsByTagName(name);
		int listLength = list.getLength();
		for (int i = 0; i < listLength; i++) {
			Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(name) && node instanceof Element) {
				return (Element) node;
			}
		}
		return null;
	}

	/**
	 * Convenience method to URL encode a string with {@link #encoding} without
	 * handling the hypothetical {@link UnsupportedEncodingException}
	 * @param url {@link String} to encode
	 * @return The encoded {@link String}
	 */
	protected String urlEncode(String url) {
		try {
			return URLEncoder.encode(url, encoding);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("UTF-8 is unsupported :O", e);
			return "";
		}
	}

	/**
	 * Gets a thumbnail from the configured cover utility based on a {@link Tag}
	 * @param tag the {@link tag} to use while searching for a cover
	 * @return The thumbnail or <code>null</code> if none was found
	 */
	public final byte[] getThumbnail(Tag tag) {
		boolean externalNetwork = PMS.getConfiguration().getExternalNetwork();
		return doGetThumbnail(tag, externalNetwork);
	}

	abstract protected byte[] doGetThumbnail(Tag tag, boolean externalNetwork);

}
