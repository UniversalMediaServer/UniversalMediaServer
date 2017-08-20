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

import java.io.IOException;
import java.io.InputStream;

public class FeedItem extends DLNAResource {
	@Override
	protected String getThumbnailURL(DLNAImageProfile profile) {
		if (thumbURL == null) {
			return null;
		}
		return super.getThumbnailURL(profile);
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		return DLNAThumbnailInputStream.toThumbnailInputStream(downloadAndSend(thumbURL, true));
	}

	private String title;
	private String itemURL;
	private String thumbURL;
	private long length;

	public FeedItem(String title, String itemURL, String thumbURL, DLNAMediaInfo media, int type) {
		super(type);
		this.title = title;
		this.itemURL = itemURL;
		this.thumbURL = thumbURL;
		this.setMedia(media);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		InputStream i = downloadAndSend(itemURL, true);
		if (i != null) {
			length = i.available();
		}
		return i;
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public boolean isFolder() {
		return false;
	}

	@Override
	public long length() {
		return length;
	}

	// XXX unused
	@Deprecated
	public long lastModified() {
		return 0;
	}

	@Override
	public void discoverChildren() {
	}

	@Override
	public String getSystemName() {
		return itemURL;
	}

	public void parse(String content) {
	}

	@Override
	public boolean isValid() {
		resolveFormat();
		return getFormat() != null;
	}
}
