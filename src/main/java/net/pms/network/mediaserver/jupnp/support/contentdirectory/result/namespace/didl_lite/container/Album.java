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

import java.net.URI;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.StorageMediumValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * An album instance represents an ordered collection of objects.
 *
 * It may have a res property for playback of the whole album instance.When it
 * does, rendering the album instance renders all of the objects sequentially.
 * When it does not, a control point needs to separately initiate rendering for
 * each child object. This class is derived from the container class and
 * inherits the properties defined by that class.
 */
public class Album extends Container {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_CONTAINER_ALBUM_TYPE);

	public Album() {
		setUpnpClass(CLASS);
	}

	public Album(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public Album(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public Album(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, CLASS, childCount);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getStorageMedium() {
		return properties.getValue(UPNP.StorageMedium.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getLongDescription() {
		return properties.getValue(UPNP.LongDescription.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setLongDescription(String description) {
		properties.set(new UPNP.LongDescription(description));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getDescription() {
		return properties.getValue(DC.Description.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setDescription(String description) {
		properties.set(new DC.Description(description));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstPublisher() {
		return properties.getValue(DC.Publisher.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getPublishers() {
		List<String> list = properties.getValues(DC.Publisher.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setPublishers(String[] publishers) {
		properties.remove(DC.Publisher.class);
		for (String publisher : publishers) {
			properties.add(new DC.Publisher(publisher));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstContributor() {
		return properties.getValue(DC.Contributor.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getContributors() {
		List<String> list = properties.getValues(DC.Contributor.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setContributors(String[] contributors) {
		properties.remove(DC.Contributor.class);
		for (String contributor : contributors) {
			properties.add(new DC.Contributor(contributor));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getDate() {
		return properties.getValue(DC.Date.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setDate(String date) {
		properties.set(new DC.Date(date));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public URI getFirstRelation() {
		return properties.getValue(DC.Relation.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public URI[] getRelations() {
		List<URI> list = properties.getValues(DC.Relation.class);
		return list.toArray(URI[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setRelations(URI[] relations) {
		properties.remove(DC.Relation.class);
		for (URI relation : relations) {
			properties.add(new DC.Relation(relation));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstRights() {
		return properties.getValue(DC.Rights.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getRights() {
		List<String> list = properties.getValues(DC.Rights.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Album setRights(String[] rights) {
		properties.remove(DC.Rights.class);
		for (String right : rights) {
			properties.add(new DC.Rights(right));
		}
		return this;
	}

}
