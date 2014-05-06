/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.dlna;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Content;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public class Feed extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(Feed.class);
	private static final int REFRESH_INTERVAL = 60 * 60 * 1000; // 1 hour

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String name;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String url;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String tempItemTitle;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String tempItemLink;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String tempFeedLink;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String tempCategory;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String tempItemThumbURL;

	@Override
	protected void resolveOnce() {
		try {
			parse();
		} catch (Exception e) {
			LOGGER.error("Error in parsing stream: " + url, e);
		}
	}

	public Feed(String name, String url, int type) {
		super(type);
		this.url = url;
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public void parse() throws Exception {
		SyndFeedInput input = new SyndFeedInput();
		byte b[] = downloadAndSendBinary(url);
		if (b != null) {
			SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(b)));
			name = feed.getTitle();
			if (feed.getCategories() != null && feed.getCategories().size() > 0) {
				SyndCategory category = (SyndCategory) feed.getCategories().get(0);
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
							if (subelt instanceof Element) {
								parseElement((Element) subelt, false);
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

	@SuppressWarnings("unchecked")
	private void parseElement(Element elt, boolean parseLink) {
		if ("content".equals(elt.getName()) && "media".equals(elt.getNamespacePrefix())) {
			if (parseLink) {
				tempItemLink = elt.getAttribute("url").getValue();
			}
			List<Content> subElts = elt.getContent();
			for (Content subelt : subElts) {
				if (subelt instanceof Element) {
					parseElement((Element) subelt, false);
				}
			}
		}
		if ("thumbnail".equals(elt.getName()) && "media".equals(elt.getNamespacePrefix())
				&& tempItemThumbURL == null) {
			tempItemThumbURL = elt.getAttribute("url").getValue();
		}
		if ("image".equals(elt.getName()) && "exInfo".equals(elt.getNamespacePrefix())
				&& tempItemThumbURL == null) {
			tempItemThumbURL = elt.getValue();
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public long length() {
		return 0;
	}

	// XXX unused
	@Deprecated
	public long lastModified() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return url;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	protected void manageItem() {
		FeedItem fi = new FeedItem(tempItemTitle, tempItemLink, tempItemThumbURL, null, getSpecificType());
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
	 * @return the url
	 * @since 1.50
	 */
	protected String getUrl() {
		return url;
	}

	/**
	 * @param url the url to set
	 * @since 1.50
	 */
	protected void setUrl(String url) {
		this.url = url;
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
}
