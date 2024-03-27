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
 * A storageFolder instance represents a collection of objects stored on some
 * storage medium.
 *
 * The storageFolder container may be writable, indicating whether new items can
 * be created as children of the storageFolder container or whether existing
 * child items can be removed. If the parent container is not writable, then the
 * storageFolder container itself cannot be writable. A storageFolder container
 * may contain other objects, except a storageSystem container or a
 * storageVolume container. A storageFolder container shall either be a child of
 * the root container or a child of another storageSystem container, a
 * storageVolume container or a storageFolder container. This class is derived
 * from the container class and inherits the properties defined by that class.
 */
public class StorageFolder extends Container {

	private static final UPNP.Class CLASS = new UPNP.Class("object.container.storageFolder");

	public StorageFolder() {
		setUpnpClass(CLASS);
	}

	public StorageFolder(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public StorageFolder(String id, Container parent, String title, String creator, Long childCount,
			Long storageUsed) {
		this(id, parent.getId(), title, creator, childCount, storageUsed);
	}

	public StorageFolder(String id, String parentID, String title, String creator, Long childCount,
			Long storageUsed) {
		super(id, parentID, title, creator, CLASS, childCount);
		if (storageUsed != null) {
			setStorageUsed(storageUsed);
		}
	}

	public Long getStorageUsed() {
		return properties.getValue(UPNP.StorageUsed.class);
	}

	public final StorageFolder setStorageUsed(Long l) {
		properties.set(new UPNP.StorageUsed(l));
		return this;
	}

	/**
	 * Check required properties.
	 */
	@Override
	public boolean isValid() {
		return super.isValid() &&
			properties.hasProperty(UPNP.StorageUsed.class);
	}
}
