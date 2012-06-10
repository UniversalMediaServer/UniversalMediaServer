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
package net.pms.dlna.virtual;

import java.io.IOException;
import java.io.InputStream;

import net.pms.dlna.DLNAResource;
import net.pms.network.HTTPResource;

/**
 * Represents a container (folder). This is widely used by the UPNP ContentBrowser service. Child objects are expected in this folder.
 */
public class VirtualFolder extends DLNAResource {
	protected String name;
	protected String thumbnailIcon;
	protected String thumbnailContentType;

	/**Constructor for this class. The constructor does not add any child to the container. This is the only
	 * chance to set the name of this container.
	 * @param name String to be shown in the ContentBrowser service 
	 * @param thumbnailIcon Represents a thumbnail to be shown. The String represents an absolute
	 *        path. Use null if none is available or desired.
	 * @see #addChild(DLNAResource)
	 */
	public VirtualFolder(String name, String thumbnailIcon) {
		this.name = name;
		this.thumbnailIcon = thumbnailIcon;
		if (thumbnailIcon != null && thumbnailIcon.toLowerCase().endsWith(".png")) {
			thumbnailContentType = HTTPResource.PNG_TYPEMIME;
		} else {
			thumbnailContentType = HTTPResource.JPEG_TYPEMIME;
		}
	}

	/**Because a container cannot be streamed, this function always returns null.
	 * @return null 
	 * @see net.pms.dlna.DLNAResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	/**Returns a string representing the container. This string is used in
	 * the UPNP ContentBrowser service.
	 * @see net.pms.dlna.DLNAResource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**Returns true in this case, as this is a folder.
	 * @return true
	 * @see net.pms.dlna.DLNAResource#isFolder()
	 */
	@Override
	public boolean isFolder() {
		return true;
	}

	/**Returns zero as this is a folder (container).
	 * @see net.pms.dlna.DLNAResource#length()
	 */
	@Override
	public long length() {
		return 0;
	}

	/**Containers are likely not to be modified, so this one returns zero.
	 * TODO: (botijo) When is this used then? Is this a prototype?
	 * @return Zero
	 */
	public long lastModified() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	/**Returns a {@link InputStream} that represents the thumbnail used.
	 * @see net.pms.dlna.DLNAResource#getThumbnailInputStream()
	 */
	@Override
	public InputStream getThumbnailInputStream() {
		return getResourceInputStream(thumbnailIcon);
	}

	/**Returns the thumbnailContentType associated to the thumbnail associated to this container.
	 * @see net.pms.dlna.DLNAResource#getThumbnailContentType()
	 * @see #thumbnailContentType
	 */
	@Override
	public String getThumbnailContentType() {
		return thumbnailContentType;
	}

	/**Returns true, as a container is always a valid item to add to another container.
	 * @see net.pms.dlna.DLNAResource#isValid()
	 */
	@Override
	public boolean isValid() {
		return true;
	}

	public void setThumbnail(String thumbnailIcon) {
		this.thumbnailIcon = thumbnailIcon;
		
		if (thumbnailIcon != null && thumbnailIcon.toLowerCase().endsWith(".png")) {
			thumbnailContentType = HTTPResource.PNG_TYPEMIME;
		} else {
			thumbnailContentType = HTTPResource.JPEG_TYPEMIME;
		}
		
	}
}
