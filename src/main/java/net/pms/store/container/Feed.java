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
import java.io.IOException;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Feed extends StoreContainer {
	private static final Logger LOGGER = LoggerFactory.getLogger(Feed.class);
	private static final int REFRESH_INTERVAL = 60 * 60 * 1000; // 1 hour
	private static final Map<String, String> FEED_TITLES_CACHE = Collections.synchronizedMap(new HashMap<>());
	private static final Map<String, String> FEED_URLS_CACHE = Collections.synchronizedMap(new HashMap<>());

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
		this.url = getFeedUrl(url);
	}

	public void parse() throws Exception {
		SyndFeedInput input = new SyndFeedInput();
		byte[] b = HTTPResource.downloadAndSendBinary(url);
		if (b != null) {
			SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(b)));
			setName(feed.getTitle());
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
	public synchronized void doRefreshChildren() {
		try {
			getChildren().clear();
			parse();
		} catch (Exception e) {
			LOGGER.error("Error in parsing stream: " + url, e);
		}
		sortChildrenIfNeeded();
	}

	/**
	 * @param url feed URL
	 * @return a feed title from its URL
	 * @throws Exception
	 */
	public static String getFeedTitle(String url) throws Exception {
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

	/**
	 * @param url webpage URL
	 * @return RSS feed URL
	 * @throws IOException
	 */
	private static String getFeedUrlFromWebpage(String url) throws IOException {
		// Check cache first
		String feedUrl = FEED_URLS_CACHE.get(url);
		if (feedUrl != null) {
			return feedUrl;
		}

		Document doc = Jsoup.connect(url).get();
		feedUrl = doc.select("link[type=application/rss+xml]").first().attr("href");
		LOGGER.trace("Parsed feed URL {} from webpage {}", feedUrl, url);

		if (StringUtils.isNotBlank(feedUrl)) {
			FEED_URLS_CACHE.put(url, feedUrl);
			return feedUrl;
		}

		return null;
	}

	/**
	 * Transforms URLs from YouTube into their channel RSS feeds.
	 */
	private static String getYouTubeChannelFeedUrl(String url) throws IOException {
		/**
		 * The newer "handle URL" does not contain the URL we want, so we
		 * parse the webpage contents to get it
		 */
		if (url.contains("youtube.com/@")) {
			return getFeedUrlFromWebpage(url);
		}

		if (url.contains("youtube.com/channel/")) {
			return url.replaceAll("youtube.com/channel/", "youtube.com/feeds/videos.xml?channel_id=");
		}

		return url;
	}

	/**
	 * Performs any known transformations to the incoming URL.
	 * For now it only handles YouTube but it could grow.
	 *
	 * @return a transformed URL or the original one
	 */
	public static String getFeedUrl(String url) {
		try {
			if (url.contains("youtube.com")) {
				return getYouTubeChannelFeedUrl(url);
			}
		} catch (IOException e) {
			LOGGER.debug("{}", e);
		}

		return url;
	}
}
