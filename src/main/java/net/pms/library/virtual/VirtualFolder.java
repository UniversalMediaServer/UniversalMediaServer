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
package net.pms.library.virtual;

import java.io.IOException;
import java.io.InputStream;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.library.LibraryResource;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import org.apache.commons.lang3.StringUtils;

/**
 * Represents a container (folder). This is widely used by the UPNP ContentBrowser service. Child objects are expected in this folder.
 */
public class VirtualFolder extends LibraryResource {
	protected String name;
	protected String thumbnailIcon;

	/**
	 * Constructor for this class. The constructor does not add any child to
	 * the container. This is the only chance to set the name of this container.
	 *
	 * @param name String to be shown in the ContentBrowser service
	 * @param thumbnailIcon Represents a thumbnail to be shown. The String
	 *                      represents an absolute path. Use null if none is
	 *                      available or desired.
	 * @see #addChild(LibraryResource)
	 */
	public VirtualFolder(Renderer renderer, String name, String thumbnailIcon) {
		super(renderer);
		this.name = name;
		this.thumbnailIcon = thumbnailIcon;
	}

	/**
	 * Because a container cannot be streamed, this function always returns null.
	 *
	 * @return null
	 * @throws java.io.IOException
	 * @see net.pms.dlna.MediaResource#getInputStream()
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	/**
	 * Returns a string representing the container. This string is used in
	 * the UPNP ContentBrowser service.
	 *
	 * @see net.pms.dlna.MediaResource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns true in this case, as this is a folder.
	 *
	 * @return true
	 * @see net.pms.dlna.MediaResource#isFolder()
	 */
	@Override
	public boolean isFolder() {
		return true;
	}

	/**
	 * Returns zero as this is a folder (container).
	 *
	 * @return 0
	 * @see net.pms.dlna.MediaResource#length()
	 */
	@Override
	public long length() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	/**
	 * Returns a {@link InputStream} that represents the thumbnail used.
	 * @throws IOException
	 *
	 * @see LibraryResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (StringUtils.isEmpty(thumbnailIcon)) {
			try {
				return super.getThumbnailInputStream();
			} catch (IOException e) {
				return null;
			}
		}
		return DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.getResourceInputStream(thumbnailIcon));
	}

	/**
	 * Returns true, as a container is always a valid item to add to another
	 * container.
	 *
	 * @see net.pms.dlna.MediaResource#isValid()
	 */
	@Override
	public boolean isValid() {
		return true;
	}

	public void setThumbnail(String thumbnailIcon) {
		this.thumbnailIcon = thumbnailIcon;
	}
}
