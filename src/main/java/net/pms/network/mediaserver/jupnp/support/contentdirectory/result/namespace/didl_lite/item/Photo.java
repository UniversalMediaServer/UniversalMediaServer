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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item;

import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A photo instance represents a photo object (as opposed to, for example, an
 * icon).
 *
 * It typically has at least one res property. This class is derived from the
 * imageItem class and inherits the properties defined by that class.
 */
public class Photo extends ImageItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_IMAGEITEM_PHOTO_TYPE);

	public Photo() {
		setUpnpClass(CLASS);
	}

	public Photo(Item other) {
		super(other);
	}

	public Photo(String id, Container parent, String title, String creator, String album, Res... resource) {
		this(id, parent.getId(), title, creator, album, resource);
	}

	public Photo(String id, String parentID, String title, String creator, String album, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
		if (album != null) {
			setAlbum(album);
		}
	}

	public String getAlbum() {
		return properties.getValue(UPNP.Album.class);
	}

	public final Photo setAlbum(String album) {
		properties.set(new UPNP.Album(album));
		return this;
	}

}
