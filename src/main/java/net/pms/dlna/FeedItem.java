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
package net.pms.dlna;

import java.io.IOException;
import java.io.InputStream;
import net.pms.media.MediaInfo;

public class FeedItem extends DLNAResource {
	private final String title;
	private final String itemURL;
	private final String thumbURL;

	private long length;

	public FeedItem(String title, String itemURL, String thumbURL, MediaInfo media, int type) {
		super(type);
		this.title = title;
		this.itemURL = itemURL;
		this.thumbURL = thumbURL;
		this.setMedia(media);
	}

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

	@Override
	public void discoverChildren() {
	}

	@Override
	public String getSystemName() {
		return itemURL;
	}

	@Override
	public boolean isValid() {
		resolveFormat();
		return getFormat() != null;
	}
}
