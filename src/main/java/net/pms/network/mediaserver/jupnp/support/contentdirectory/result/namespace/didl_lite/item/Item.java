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

import java.util.ArrayList;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Desc;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.WriteStatusValue;

/**
 * Item is a first-level class derived directly from object.
 *
 * @see An item most often represents a single piece of AV data, such as a CD
 * track, a movie or an audio file.
 */
public class Item extends BaseObject {

	public Item() {
		super("item");
	}

	public Item(Item other) {
		super("item", other);
		setRefID(other.getRefID());
	}

	public Item(String id, String parentID, String title, String creator, boolean restricted, WriteStatusValue writeStatus, UPNP.Class upnpClass, List<Res> resources, List<Property<?>> properties, List<Desc> descriptions) {
		super("item", id, parentID, title, creator, restricted, writeStatus, upnpClass, resources, properties, descriptions);
	}

	public Item(String id, String parentID, String title, String creator, boolean restricted, WriteStatusValue writeStatus, UPNP.Class upnpClass, List<Res> resources, List<Property<?>> properties, List<Desc> descriptions, String refID) {
		this(id, parentID, title, creator, restricted, writeStatus, upnpClass, resources, properties, descriptions);
		setRefID(refID);
	}

	public Item(String id, Container parent, String title, String creator, UPNP.Class upnpClass) {
		this(id, parent.getId(), title, creator, false, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public Item(String id, Container parent, String title, String creator, UPNP.Class upnpClass, String refID) {
		this(id, parent.getId(), title, creator, false, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), refID);
	}

	public Item(String id, String parentID, String title, String creator, UPNP.Class upnpClass) {
		this(id, parentID, title, creator, false, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public Item(String id, String parentID, String title, String creator, UPNP.Class upnpClass, String refID) {
		this(id, parentID, title, creator, false, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), refID);
	}

	public String getRefID() {
		return dependentProperties.getValue(DIDL_LITE.RefID.class);
	}

	public final void setRefID(String refID) {
		dependentProperties.set(new DIDL_LITE.RefID(refID));
	}

	public String getFirstBookmarkID() {
		return properties.getValue(UPNP.BookmarkID.class);
	}

	/**
	 * The upnp:bookmarkID property contains the object ID of a bookmark item
	 * that is associated with this content item and that marks a specific
	 * location within its content.
	 *
	 * @since ContentDirectory v2
	 */
	public String[] getBookmarkID() {
		List<String> list = properties.getValues(UPNP.BookmarkID.class);
		return list.toArray(String[]::new);
	}

	/**
	 * The upnp:bookmarkID property contains the object ID of a bookmark item
	 * that is associated with this content item and that marks a specific
	 * location within its content.
	 *
	 * @since ContentDirectory v2
	 */
	public Item setBookmarkID(String[] bookmarkIDs) {
		properties.remove(UPNP.BookmarkID.class);
		for (String bookmarkID : bookmarkIDs) {
			properties.add(new UPNP.BookmarkID(bookmarkID));
		}
		return this;
	}

}
