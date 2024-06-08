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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container;

import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A photoAlbum instance is an album container that contains items of class
 * photo or sub-album containers of class photoAlbum.
 *
 * This class is derived from the album class and inherits the properties
 * defined by that class.
 */
public class PhotoAlbum extends Album {

	private static final UPNP.Class CLASS = new UPNP.Class("object.container.album.photoAlbum");

	public PhotoAlbum() {
		setUpnpClass(CLASS);
	}

	public PhotoAlbum(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public PhotoAlbum(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public PhotoAlbum(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, childCount);
		setUpnpClass(CLASS);
	}

}
