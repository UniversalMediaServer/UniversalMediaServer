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
	private static final String encoding = StandardCharsets.UTF_8.name();
	private static Object instanceLock = new Object();
	private static CoverUtil instance = null;

	/**
	 * Do not instantiate this class, use {@link #get()}
	 */
	protected CoverUtil() {
	}

	public static CoverUtil get() {
		CoverSupplier supplier = CoverSupplier.COVER_ART_ARCHIVE ; //TODO Temp hardcode
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

	protected String urlEncode(String url) {
		try {
			return URLEncoder.encode(url, encoding);
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("UTF-8 is unsupported :O", e);
			return "";
		}
	}

	protected boolean hasValue(String s) {
		return s != null && !s.trim().isEmpty();
	}

	public final byte[] getThumbnail(Tag tag) {
		if (!PMS.getConfiguration().getExternalNetwork()) {
			LOGGER.trace("Can't download cover since external network is disabled");
			return null;
		}
		return doGetThumbnail(tag);
	}

	abstract public byte[] doGetThumbnail(Tag tag);

}
