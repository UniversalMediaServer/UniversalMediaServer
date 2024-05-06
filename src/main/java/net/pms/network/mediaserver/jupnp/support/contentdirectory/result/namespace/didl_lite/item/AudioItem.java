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

import java.net.URI;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * An audioItem instance represents content that is intended for listening.
 *
 * Movies, TV broadcasts, etc., that also contain an audio track are excluded
 * from this definition; those objects are classified under videoItem.
 *
 * It typically has at least one res property.
 *
 * This class is derived from the item class and inherits the properties defined
 * by that class.
 */
public class AudioItem extends Item {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_AUDIOITEM_TYPE);

	public AudioItem() {
		setUpnpClass(CLASS);
	}

	public AudioItem(Item other) {
		super(other);
	}

	public AudioItem(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public AudioItem(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, CLASS);
		if (resource != null) {
			setResources(resource);
		}
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstGenre() {
		return properties.getValue(UPNP.Genre.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getGenres() {
		List<String> list = properties.getValues(UPNP.Genre.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public AudioItem setGenres(String[] genres) {
		properties.remove(UPNP.Genre.class);
		for (String genre : genres) {
			properties.add(new UPNP.Genre(genre));
		}
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
	public AudioItem setDescription(String description) {
		properties.set(new DC.Description(description));
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
	public AudioItem setLongDescription(String description) {
		properties.set(new UPNP.LongDescription(description));
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
	public AudioItem setPublishers(String[] publishers) {
		properties.remove(DC.Publisher.class);
		for (String publisher : publishers) {
			properties.add(new DC.Publisher(publisher));
		}
		return this;
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
	public AudioItem setLanguage(String language) {
		properties.set(new DC.Language(language));
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
	public AudioItem setRelations(URI[] relations) {
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
	public AudioItem setRights(String[] rights) {
		properties.remove(DC.Rights.class);
		for (String right : rights) {
			properties.add(new DC.Rights(right));
		}
		return this;
	}
}
