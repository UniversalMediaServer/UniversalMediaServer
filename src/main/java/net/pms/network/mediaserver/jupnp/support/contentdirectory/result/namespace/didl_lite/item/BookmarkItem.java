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

import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A bookmarkItem instance represents a piece of data that can be used to
 * recover previous state information of a AVTransport and a RenderingControl
 * service instance.
 *
 * A bookmarkItem instance can be located in any container but all bookmark
 * items in the ContentDirectory service shall be accessible within one of the
 * defined bookmark subtrees.
 *
 * This class is derived from the item class and inherits the properties defined
 * by that class.
 *
 * @since ContentDirectory v2
 */
public class BookmarkItem extends Item {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_BOOKMARKITEM_TYPE);

	public BookmarkItem() {
		setUpnpClass(CLASS);
	}

	public BookmarkItem(Item other) {
		super(other);
	}

	public BookmarkItem(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public BookmarkItem(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, CLASS);
		if (resource != null) {
			setResources(resource);
		}
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getBookmarkedObjectID() {
		return properties.getValue(UPNP.BookmarkedObjectID.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public final BookmarkItem setBookmarkedObjectID(String bookmarkedObjectID) {
		properties.set(new UPNP.BookmarkedObjectID(bookmarkedObjectID));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public boolean isNeverPlayable() {
		return dependentProperties.getValue(DIDL_LITE.NeverPlayable.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public final void setNeverPlayable(boolean neverPlayable) {
		dependentProperties.set(new DIDL_LITE.NeverPlayable(neverPlayable));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public UPNP.DeviceUDN getDeviceUDN() {
		return properties.get(UPNP.DeviceUDN.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public final BookmarkItem setDeviceUDN(UPNP.DeviceUDN deviceUDN) {
		properties.set(deviceUDN);
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public final BookmarkItem setDeviceUDN(String deviceUDN, String serviceType, String serviceId) {
		return setDeviceUDN(new UPNP.DeviceUDN(deviceUDN, serviceType, serviceId));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getDate() {
		return properties.getValue(DC.Date.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public final BookmarkItem setDate(String date) {
		properties.set(new DC.Date(date));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public UPNP.StateVariableCollection getFirstStateVariableCollections() {
		return properties.get(UPNP.StateVariableCollection.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public List<UPNP.StateVariableCollection> getStateVariableCollections() {
		return properties.getAll(UPNP.StateVariableCollection.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public BookmarkItem setStateVariableCollections(UPNP.StateVariableCollection[] stateVariableCollections) {
		properties.remove(UPNP.StateVariableCollection.class);
		for (UPNP.StateVariableCollection stateVariableCollection : stateVariableCollections) {
			properties.add(stateVariableCollection);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public BookmarkItem addStateVariableCollection(UPNP.StateVariableCollection stateVariableCollection) {
		properties.add(stateVariableCollection);
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public BookmarkItem addStateVariableCollection(String value, String serviceName) {
		return addStateVariableCollection(new UPNP.StateVariableCollection(value, serviceName));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public BookmarkItem addStateVariableCollection(String value, String serviceName, String rcsInstanceType) {
		return addStateVariableCollection(new UPNP.StateVariableCollection(value, serviceName, rcsInstanceType));
	}

	/**
	 * Check required properties.
	 */
	@Override
	public boolean isValid() {
		return super.isValid() &&
				properties.hasProperty(UPNP.BookmarkedObjectID.class) &&
				properties.hasProperty(UPNP.DeviceUDN.class) &&
				properties.hasProperty(UPNP.StateVariableCollection.class);
	}
}
