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
package net.pms.store.container;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import net.pms.store.StoreContainer;
import net.pms.store.item.FeedItem;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Content;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Feed extends StoreContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(Feed.class);
	private static final int REFRESH_INTERVAL = 60 * 60 * 1000; // 1 hour
	private static final Map<String, String> FEED_TITLES_CACHE = Collections.synchronizedMap(new HashMap<>());

	private final String url;
	private final int childSpecificType;

	private String tempItemTitle;
	private String tempItemLink;
	private String tempFeedLink;
	private String tempCategory;
	private String tempItemThumbURL;

	public Feed(Renderer renderer, String name, String url, int type) {
		super(renderer, name, null);
		childSpecificType = type;
		this.url = url;
	}

	public void parse() throws Exception {
		SyndFeedInput input = new SyndFeedInput();
		byte[] b = HTTPResource.downloadAndSendBinary(url);
		if (b != null) {
			SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(b)));
			name = feed.getTitle();
			if (feed.getCategories() != null && !feed.getCategories().isEmpty()) {
				SyndCategory category = feed.getCategories().get(0);
				tempCategory = category.getName();
			}
			List<SyndEntry> entries = feed.getEntries();
			for (SyndEntry entry : entries) {
				tempItemTitle = entry.getTitle();
				tempItemLink = entry.getLink();
				tempFeedLink = entry.getUri();
				tempItemThumbURL = null;

				ArrayList<Element> elements = (ArrayList<Element>) entry.getForeignMarkup();
				for (Element elt : elements) {
					if ("group".equals(elt.getName()) && "media".equals(elt.getNamespacePrefix())) {
						List<Content> subElts = elt.getContent();
						for (Content subelt : subElts) {
							if (subelt instanceof Element element) {
								parseElement(element, false);
							}
						}
					}
					parseElement(elt, true);
				}
				List<SyndEnclosure> enclosures = entry.getEnclosures();
				for (SyndEnclosure enc : enclosures) {
					if (StringUtils.isNotBlank(enc.getUrl())) {
						tempItemLink = enc.getUrl();
					}
				}
				manageItem();
			}
		}
		setLastModified(System.currentTimeMillis());
	}

	private void parseElement(Element elt, boolean parseLink) {
		if ("content".equals(elt.getName()) && "media".equals(elt.getNamespacePrefix())) {
			if (parseLink) {
				tempItemLink = elt.getAttribute("url").getValue();
			}
			List<Content> subElts = elt.getContent();
			for (Content subelt : subElts) {
				if (subelt instanceof Element element) {
					parseElement(element, false);
				}
			}
		}
		if ("thumbnail".equals(elt.getName()) && "media".equals(elt.getNamespacePrefix()) &&
				tempItemThumbURL == null) {
			tempItemThumbURL = elt.getAttribute("url").getValue();
		}
		if ("image".equals(elt.getName()) && "exInfo".equals(elt.getNamespacePrefix()) &&
				tempItemThumbURL == null) {
			tempItemThumbURL = elt.getValue();
		}
	}

	/**
	 * @return the url
	 * @since 1.50
	 */
	protected String getUrl() {
		return url;
	}

	/**
	 * @return the tempItemTitle
	 * @since 1.50
	 */
	protected String getTempItemTitle() {
		return tempItemTitle;
	}

	/**
	 * @param tempItemTitle the tempItemTitle to set
	 * @since 1.50
	 */
	protected void setTempItemTitle(String tempItemTitle) {
		this.tempItemTitle = tempItemTitle;
	}

	/**
	 * @return the tempItemLink
	 * @since 1.50
	 */
	protected String getTempItemLink() {
		return tempItemLink;
	}

	/**
	 * @param tempItemLink the tempItemLink to set
	 * @since 1.50
	 */
	protected void setTempItemLink(String tempItemLink) {
		this.tempItemLink = tempItemLink;
	}

	/**
	 * @return the tempFeedLink
	 * @since 1.50
	 */
	protected String getTempFeedLink() {
		return tempFeedLink;
	}

	/**
	 * @param tempFeedLink the tempFeedLink to set
	 * @since 1.50
	 */
	protected void setTempFeedLink(String tempFeedLink) {
		this.tempFeedLink = tempFeedLink;
	}

	/**
	 * @return the tempCategory
	 * @since 1.50
	 */
	protected String getTempCategory() {
		return tempCategory;
	}

	/**
	 * @param tempCategory the tempCategory to set
	 * @since 1.50
	 */
	protected void setTempCategory(String tempCategory) {
		this.tempCategory = tempCategory;
	}

	/**
	 * @return the tempItemThumbURL
	 * @since 1.50
	 */
	protected String getTempItemThumbURL() {
		return tempItemThumbURL;
	}

	/**
	 * @param tempItemThumbURL the tempItemThumbURL to set
	 * @since 1.50
	 */
	protected void setTempItemThumbURL(String tempItemThumbURL) {
		this.tempItemThumbURL = tempItemThumbURL;
	}

	/**
	 * @param name the name to set
	 * @since 1.50
	 */
	protected void setName(String name) {
		this.name = name;
	}

	@Override
	protected void resolveOnce() {
		try {
			parse();
		} catch (Exception e) {
			LOGGER.error("Error in parsing stream: " + url, e);
		}
	}

	@Override
	public String getSystemName() {
		return url;
	}

	protected void manageItem() {
		FeedItem fi = new FeedItem(renderer, tempItemTitle, tempItemLink, tempItemThumbURL, null, childSpecificType);
		addChild(fi);
	}

	@Override
	public boolean isRefreshNeeded() {
		return (System.currentTimeMillis() - getLastModified() > REFRESH_INTERVAL);
	}

	@Override
	public void doRefreshChildren() {
		try {
			getChildren().clear();
			parse();
		} catch (Exception e) {
			LOGGER.error("Error in parsing stream: " + url, e);
		}
	}

	/**
	 * @param url feed URL
	 * @return a feed title from its URL
	 * @throws Exception
	 */
	public static String getFeedTitle(String url) throws Exception {
		// Convert YouTube channel URIs to their feed URIs
		if (url.contains("youtube.com/channel/")) {
			url = url.replaceAll("youtube.com/channel/", "youtube.com/feeds/videos.xml?channel_id=");
		}

		// Check cache first
		String feedTitle = FEED_TITLES_CACHE.get(url);
		if (feedTitle != null) {
			return feedTitle;
		}

		SyndFeedInput input = new SyndFeedInput();
		byte[] b = HTTPResource.downloadAndSendBinary(url);
		if (b != null) {
			SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(b)));
			feedTitle = feed.getTitle();
			if (StringUtils.isNotBlank(feedTitle)) {
				FEED_TITLES_CACHE.put(url, feedTitle);
				return feedTitle;
			}
		}

		return null;
	}

}
