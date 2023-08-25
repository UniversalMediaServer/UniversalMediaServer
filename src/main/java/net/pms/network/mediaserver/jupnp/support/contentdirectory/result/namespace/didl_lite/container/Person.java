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

import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A person instance represents an unordered collection of objects associated
 * with a person.
 *
 * It may have a res property for playback of all items belonging to the person
 * container. A person container can contain objects of class album, item, or
 * playlist. The classes of objects a person container may actually contain is
 * device-dependent. This class is derived from the container class and inherits
 * the properties defined by that class.
 */
public class Person extends Container {

	private static final UPNP.Class CLASS = new UPNP.Class("object.container.person");

	public Person() {
		setUpnpClass(CLASS);
	}

	public Person(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public Person(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public Person(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, CLASS, childCount);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getLanguage() {
		return properties.getValue(DC.Language.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Person setLanguage(String language) {
		properties.set(new DC.Language(language));
		return this;
	}

}
