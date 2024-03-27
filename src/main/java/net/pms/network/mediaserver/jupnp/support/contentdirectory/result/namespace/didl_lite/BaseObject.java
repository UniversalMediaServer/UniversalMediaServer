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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite;

import com.google.common.primitives.UnsignedInteger;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.WriteStatusValue;

/**
 * This is the root class of the entire ContentDirectory service class
 * hierarchy.
 *
 * It shall not be instantiated.
 *
 * No object shall be created or otherwise exist in a ContentDirectory service
 * whose upnp:class property has the value “object”.
 *
 * The object class defines properties that are common to both individual media
 * items and logical collections of these items.
 */
public abstract class BaseObject extends Property {

	protected BaseObject(String qualifiedName) {
		super(null, qualifiedName);
	}

	protected BaseObject(String qualifiedName, BaseObject other) {
		this(qualifiedName,
				other.getId(),
				other.getParentID(),
				other.getTitle(),
				other.getCreator(),
				other.isRestricted(),
				WriteStatusValue.valueOf(other.getWriteStatus()),
				other.getUpnpClass(),
				null,
				other.getProperties().get(),
				other.getDescriptions()
		);
	}

	protected BaseObject(String qualifiedName, String id, String parentID, String title, String creator, boolean restricted, WriteStatusValue writeStatus, UPNP.Class upnpClass, List<Res> resources, List<Property<?>> properties, List<Desc> descriptions) {
		super(null, qualifiedName);
		this.properties.set(properties);
		setId(id);
		setParentID(parentID);
		setRestricted(restricted);
		setTitle(title);
		setUpnpClass(upnpClass);
		setCreator(creator);
		setWriteStatus(writeStatus);
		if (resources != null) {
			setResources(resources);
		}
		if (descriptions != null) {
			setDescriptions(descriptions);
		}
	}

	public String getId() {
		return dependentProperties.getValue(DIDL_LITE.Id.class);
	}

	public final BaseObject setId(String id) {
		dependentProperties.set(new DIDL_LITE.Id(id));
		return this;
	}

	public String getParentID() {
		return dependentProperties.getValue(DIDL_LITE.ParentID.class);
	}

	public final BaseObject setParentID(String parentID) {
		dependentProperties.set(new DIDL_LITE.ParentID(parentID));
		return this;
	}

	/**
	 * The REQUIRED @restricted property indicates whether the object is
	 * modifiable.
	 *
	 * If set to “1”, the ability to modify a given object is confined to the
	 * ContentDirectory service. Control point metadata write access is
	 * disabled.
	 *
	 * If set to “0”, a control point can modify the object’s metadata and/or
	 * modify the object’s children.
	 *
	 * Default Value: N/A – The property is REQUIRED.
	 *
	 * @since ContentDirectory v1
	 */
	public boolean isRestricted() {
		return dependentProperties.getValue(DIDL_LITE.Restricted.class);
	}

	/**
	 * The REQUIRED @restricted property indicates whether the object is
	 * modifiable.
	 *
	 * If set to “1”, the ability to modify a given object is confined to the
	 * ContentDirectory service. Control point metadata write access is
	 * disabled.
	 *
	 * If set to “0”, a control point can modify the object’s metadata and/or
	 * modify the object’s children.
	 *
	 * Default Value: N/A – The property is REQUIRED.
	 *
	 * @since ContentDirectory v1
	 */
	public final BaseObject setRestricted(boolean restricted) {
		dependentProperties.set(new DIDL_LITE.Restricted(restricted));
		return this;
	}

	public String getTitle() {
		return properties.getValue(DC.Title.class);
	}

	/**
	 * The dc:title property is a REQUIRED property and indicates a friendly
	 * name for the object.
	 *
	 * @since ContentDirectory v1
	 */
	public final BaseObject setTitle(String title) {
		properties.set(new DC.Title(title));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getUpnpClassName() {
		return properties.getValue(UPNP.Class.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public UPNP.Class getUpnpClass() {
		return properties.get(UPNP.Class.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final BaseObject setUpnpClass(UPNP.Class upnpClass) {
		properties.set(upnpClass);
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getCreator() {
		return properties.getValue(DC.Creator.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final BaseObject setCreator(String creator) {
		properties.set(new DC.Creator(creator));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Res getFirstResource() {
		return properties.get(Res.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public List<Res> getResources() {
		return properties.getAll(Res.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final BaseObject setResources(List<Res> resources) {
		properties.remove(Res.class);
		for (Res resource : resources) {
			properties.add(resource);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public BaseObject setResources(Res[] resources) {
		properties.remove(Res.class);
		properties.addAll(resources);
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public BaseObject addResource(Res resource) {
		return addProperty(resource);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public List<Desc> getDescriptions() {
		return properties.getAll(Desc.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final BaseObject setDescriptions(List<Desc> descriptions) {
		properties.remove(Desc.class);
		for (Desc description : descriptions) {
			properties.add(description);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public BaseObject setDescriptions(Desc[] descriptions) {
		properties.remove(Desc.class);
		properties.addAll(descriptions);
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public BaseObject addDescription(Desc description) {
		return addProperty(description);
	}

	public BaseObject addProperty(Property<?> property) {
		properties.add(property);
		return this;
	}

	/**
	 * The upnp:writeStatus property controls the modifiability of the resources
	 * of a given object.
	 *
	 * The ability for a control point to change the value of the
	 * upnp:writeStatus property is implementation dependent.
	 *
	 * @since ContentDirectory v1
	 */
	public String getWriteStatus() {
		return properties.getValue(UPNP.WriteStatus.class);
	}

	/**
	 * The upnp:writeStatus property controls the modifiability of the resources
	 * of a given object.
	 *
	 * The ability for a control point to change the value of the
	 * upnp:writeStatus property is implementation dependent.
	 *
	 * @since ContentDirectory v1
	 */
	public final BaseObject setWriteStatus(WriteStatusValue writeStatus) {
		properties.set(new UPNP.WriteStatus(writeStatus));
		return this;
	}

	/**
	 * The upnp:writeStatus property controls the modifiability of the resources
	 * of a given object.
	 *
	 * The ability for a control point to change the value of the
	 * upnp:writeStatus property is implementation dependent.
	 *
	 * @since ContentDirectory v1
	 */
	public BaseObject setWriteStatus(String writeStatus) {
		properties.set(new UPNP.WriteStatus(WriteStatusValue.valueOf(writeStatus)));
		return this;
	}

	/**
	 * @since ContentDirectory v3
	 */
	public UnsignedInteger getObjectUpdateID() {
		return properties.getValue(UPNP.ObjectUpdateID.class);
	}

	/**
	 * @since ContentDirectory v3
	 */
	public final void setObjectUpdateID(UnsignedInteger objectUpdateID) {
		properties.set(new UPNP.ObjectUpdateID(objectUpdateID));
	}

	/**
	 * Check required properties.
	 */
	public boolean isValid() {
		return dependentProperties.hasProperty(DIDL_LITE.Id.class) &&
			dependentProperties.hasProperty(DIDL_LITE.ParentID.class) &&
			dependentProperties.hasProperty(DIDL_LITE.Restricted.class) &&
			properties.hasProperty(DC.Title.class) &&
			properties.hasProperty(UPNP.Class.class);
	}

}
