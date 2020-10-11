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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import net.pms.network.HTTPResourceAuthenticator;
import net.pms.util.FileUtil;

public class WebStream extends DLNAResource {
	@Override
	public boolean isValid() {
		resolveFormat();
		return getFormat() != null;
	}

	private String url;
	private String fluxName;
	private String thumbURL;

	public WebStream(String fluxName, String url, String thumbURL, int type) {
		super(type);

		try {
			URL tmpUrl = new URL(url);
			tmpUrl = HTTPResourceAuthenticator.concatenateUserInfo(tmpUrl);
			this.url = tmpUrl.toString();
		} catch (MalformedURLException e) {
			this.url = url;
		}

		try {
			URL tmpUrl = new URL(thumbURL);
			tmpUrl = HTTPResourceAuthenticator.concatenateUserInfo(tmpUrl);
			this.thumbURL = tmpUrl.toString();
		} catch (MalformedURLException e) {
			this.thumbURL = thumbURL;
		}

		this.fluxName = fluxName;
	}

	@Override
	public String write() {
		return fluxName + ">" + url + ">" + thumbURL + ">" + getSpecificType();
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (thumbURL != null) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(
				FileUtil.isUrl(thumbURL) ? downloadAndSend(thumbURL, true) : new FileInputStream(thumbURL)
			);
		} else {
			DLNAThumbnailInputStream inputStream = super.getThumbnailInputStream();
			return inputStream;
		}
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public long length() {
		return DLNAMediaInfo.TRANS_SIZE;
	}

	@Override
	public String getName() {
		return getFluxName();
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public String getSystemName() {
		return getUrl();
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
	 * @return the fluxName
	 * @since 1.50
	 */
	protected String getFluxName() {
		return fluxName;
	}

	/**
	 * @param fluxName the fluxName to set
	 * @since 1.50
	 */
	protected void setFluxName(String fluxName) {
		this.fluxName = fluxName;
	}

	/**
	 * @return the thumbURL
	 * @since 1.50
	 */
	protected String getThumbURL() {
		return thumbURL;
	}

	/**
	 * @param thumbURL the thumbURL to set
	 * @since 1.50
	 */
	protected void setThumbURL(String thumbURL) {
		this.thumbURL = thumbURL;
	}

	@Override
	public boolean isSubSelectable() {
		return true;
	}
}
