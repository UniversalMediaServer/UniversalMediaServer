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
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A musicArtist instance is a person instance, where the person associated with
 * the container is a music artist.
 *
 * A musicArtist container can contain objects of class musicAlbum, musicTrack
 * or musicVideoClip. The classes of objects a musicArtist container may
 * actually contain is device-dependent. This class is derived from the person
 * class and inherits the properties defined by that class.
 */
public class MusicArtist extends Person {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_CONTAINER_PERSON_MUSICARTIST_TYPE);

	public MusicArtist() {
		setUpnpClass(CLASS);
	}

	public MusicArtist(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public MusicArtist(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public MusicArtist(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, childCount);
		setUpnpClass(CLASS);
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
	public MusicArtist setGenres(String[] genres) {
		properties.remove(UPNP.Genre.class);
		for (String genre : genres) {
			properties.add(new UPNP.Genre(genre));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public URI getArtistDiscographyURI() {
		return properties.getValue(UPNP.ArtistDiscographyURI.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicArtist setArtistDiscographyURI(URI uri) {
		properties.set(new UPNP.ArtistDiscographyURI(uri));
		return this;
	}

}
