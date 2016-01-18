package net.pms.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import net.pms.PMS;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public abstract class CoverUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(CoverUtil.class);
	protected static final String encoding = StandardCharsets.UTF_8.name();
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
		for (int i = 0; i < list.getLength(); i++) {
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
	 * Convenience method to check if a {@link String} is not <code>null</code>
	 * and contains anything other than whitespace.
	 * @param s the {@link String} to evaluate
	 * @return The verdict
	 */
	protected boolean hasValue(String s) {
		return s != null && !s.trim().isEmpty();
	}

	/**
	 * Gets a thumbnail from the configured cover utility based on a {@link Tag}
	 * @param tag the {@link tag} to use while searching for a cover
	 * @return The thumbnail or <code>null</code> if none was found
	 */
	public final byte[] getThumbnail(Tag tag) {
		if (!PMS.getConfiguration().getExternalNetwork()) {
			LOGGER.trace("Can't download cover since external network is disabled");
			return null;
		}
		return doGetThumbnail(tag);
	}

	abstract protected byte[] doGetThumbnail(Tag tag);

}
