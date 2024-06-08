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
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A genre instance represents an unordered collection of objects that all
 * belong to the same genre.
 *
 * It may have a res property for playback of all items of the genre, or not. In
 * the first case, rendering the genre has the semantics of rendering each
 * object in the collection, in some order. In the latter case, a control point
 * needs to separately initiate rendering for each child object. A genre
 * container can contain objects of class person, album, audioItem, videoItem or
 * sub-genre containers of the same class (for example, Rock contains
 * Alternative Rock). The classes of objects a genre container may actually
 * contain is device-dependent. This class is derived from the container class
 * and inherits the properties defined by that class.
 */
public class GenreContainer extends Container {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_CONTAINER_GENRE_TYPE);

	public GenreContainer() {
		setUpnpClass(CLASS);
	}

	public GenreContainer(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public GenreContainer(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public GenreContainer(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, CLASS, childCount);
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
	public GenreContainer setLongDescription(String description) {
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
	public GenreContainer setDescription(String description) {
		properties.set(new DC.Description(description));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public UPNP.Genre getGenre() {
		return properties.get(UPNP.Genre.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public GenreContainer setGenre(UPNP.Genre genre) {
		properties.set(genre);
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public GenreContainer setGenre(String genre) {
		properties.set(new UPNP.Genre(genre));
		return this;
	}

}
