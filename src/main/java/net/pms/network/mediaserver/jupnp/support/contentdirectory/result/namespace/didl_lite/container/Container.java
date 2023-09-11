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

import com.google.common.primitives.UnsignedInteger;
import java.util.ArrayList;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Desc;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.WriteStatusValue;

/**
 * This is a derived class of object used to represent a collection (container)
 * of individual content objects and other collections of objects (nested
 * containers).
 *
 * The XML expression of any instance of a class that is derived from container
 * is the <container> element. This class is derived from the object class and
 * inherits the properties defined by that class.
 */
public class Container extends BaseObject {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_CONTAINER_TYPE);

	protected List<Container> containers = new ArrayList<>();
	protected List<Item> items = new ArrayList<>();

	public Container() {
		super("container");
		setUpnpClass(CLASS);
	}

	public Container(Container other) {
		super("container", other);
		setUpnpClass(CLASS);
		setChildCount(other.getChildCount());
		setSearchable(other.isSearchable());
		setCreateClasses(other.getCreateClasses());
		setSearchClasses(other.getSearchClasses());
	}

	public Container(String id, String parentID, String title, String creator, boolean restricted, WriteStatusValue writeStatus, UPNP.Class upnpClass, List<Res> resources, List<Property<?>> properties, List<Desc> descriptions) {
		super("container", id, parentID, title, creator, restricted, writeStatus, upnpClass, resources, properties, descriptions);
	}

	public Container(String id, String parentID, String title, String creator, boolean restricted, WriteStatusValue writeStatus, UPNP.Class upnpClass, List<Res> resources, List<Property<?>> properties, List<Desc> descriptions, Long childCount, boolean searchable, List<UPNP.CreateClass> createClasses, List<UPNP.SearchClass> searchClasses, List<Item> items) {
		this(id, parentID, title, creator, restricted, writeStatus, upnpClass, resources, properties, descriptions);
		setChildCount(childCount);
		setSearchable(searchable);
		setCreateClasses(createClasses);
		setSearchClasses(searchClasses);
		this.items = items;
	}

	public Container(String id, Container parent, String title, String creator, UPNP.Class upnpClass, Long childCount) {
		this(id, parent.getId(), title, creator, true, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), childCount, false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public Container(String id, String parentID, String title, String creator, UPNP.Class upnpClass, Long childCount) {
		this(id, parentID, title, creator, true, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), childCount, false, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
	}

	public Container(String id, Container parent, String title, String creator, UPNP.Class upnpClass, Long childCount, boolean searchable, List<UPNP.CreateClass> createClasses, List<UPNP.SearchClass> searchClasses, List<Item> items) {
		this(id, parent.getId(), title, creator, true, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), childCount, searchable, createClasses, searchClasses, items);
	}

	public Container(String id, String parentID, String title, String creator, UPNP.Class upnpClass, Long childCount, boolean searchable, List<UPNP.CreateClass> createClasses, List<UPNP.SearchClass> searchClasses, List<Item> items) {
		this(id, parentID, title, creator, true, null, upnpClass, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), childCount, searchable, createClasses, searchClasses, items);
	}

	public Long getChildCount() {
		return dependentProperties.getValue(DIDL_LITE.ChildCount.class);
	}

	/**
	 * The @childCount property is only applicable to container objects. It
	 * reflects the number of direct children contained in the container object.
	 *
	 * @param childCount
	 */
	public final void setChildCount(Long childCount) {
		dependentProperties.set(new DIDL_LITE.ChildCount(childCount));
	}

	/**
	 * @since ContentDirectory v4
	 */
	public Long getChildContainerCount() {
		return dependentProperties.getValue(DIDL_LITE.ChildContainerCount.class);
	}

	/**
	 * @since ContentDirectory v4
	 */
	public final void setChildContainerCount(Long childContainerCount) {
		dependentProperties.set(new DIDL_LITE.ChildContainerCount(childContainerCount));
	}

	public UPNP.CreateClass getFirstCreateClass() {
		return properties.get(UPNP.CreateClass.class);
	}

	public List<UPNP.CreateClass> getCreateClasses() {
		return properties.getAll(UPNP.CreateClass.class);
	}

	public final Container setCreateClasses(List<UPNP.CreateClass> createClasses) {
		properties.remove(UPNP.CreateClass.class);
		for (UPNP.CreateClass createClass : createClasses) {
			properties.add(createClass);
		}
		return this;
	}

	public UPNP.SearchClass getFirstSearchClass() {
		return properties.get(UPNP.SearchClass.class);
	}

	public List<UPNP.SearchClass> getSearchClasses() {
		return properties.getAll(UPNP.SearchClass.class);
	}

	public final Container setSearchClasses(List<UPNP.SearchClass> searchClasses) {
		properties.remove(UPNP.SearchClass.class);
		for (UPNP.SearchClass searchClass : searchClasses) {
			properties.add(searchClass);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public boolean isSearchable() {
		return dependentProperties.getValue(DIDL_LITE.Searchable.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final void setSearchable(boolean searchable) {
		dependentProperties.set(new DIDL_LITE.Searchable(searchable));
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
	 * @since ContentDirectory v3
	 */
	public UnsignedInteger getContainerUpdateID() {
		return properties.getValue(UPNP.ContainerUpdateID.class);
	}

	/**
	 * @since ContentDirectory v3
	 */
	public final void setContainerUpdateID(UnsignedInteger objectUpdateID) {
		properties.set(new UPNP.ContainerUpdateID(objectUpdateID));
	}

	/**
	 * @since ContentDirectory v3
	 */
	public UnsignedInteger getTotalDeletedChildCount() {
		return properties.getValue(UPNP.TotalDeletedChildCount.class);
	}

	/**
	 * @since ContentDirectory v3
	 */
	public final void setTotalDeletedChildCount(UnsignedInteger totalDeletedChildCount) {
		properties.set(new UPNP.TotalDeletedChildCount(totalDeletedChildCount));
	}

}
