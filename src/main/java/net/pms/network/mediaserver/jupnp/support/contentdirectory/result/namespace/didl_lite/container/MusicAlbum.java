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
 * A musicAlbum instance is an album container that contains items of class
 * musicTrack or sub-album containers of class musicAlbum.
 *
 * It can be used to model, for example, an audio-CD. This class is derived from
 * the album class and inherits the properties defined by that class.
 */
public class MusicAlbum extends Album {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_CONTAINER_ALBUM_MUSICALBUM_TYPE);

	public MusicAlbum() {
		setUpnpClass(CLASS);
	}

	public MusicAlbum(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public MusicAlbum(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public MusicAlbum(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, childCount);
		setUpnpClass(CLASS);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public UPNP.Artist getFirstArtist() {
		return properties.get(UPNP.Artist.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public List<UPNP.Artist> getArtists() {
		return properties.getAll(UPNP.Artist.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicAlbum setArtists(UPNP.Artist[] artists) {
		properties.remove(UPNP.Artist.class);
		for (UPNP.Artist artist : artists) {
			properties.add(artist);
		}
		return this;
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
	public MusicAlbum setGenres(String[] genres) {
		properties.remove(UPNP.Genre.class);
		for (String genre : genres) {
			properties.add(new UPNP.Genre(genre));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstProducer() {
		return properties.getValue(UPNP.Producer.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getProducers() {
		List<String> list = properties.getValues(UPNP.Producer.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicAlbum setProducers(String[] producers) {
		properties.remove(UPNP.Producer.class);
		for (String producer : producers) {
			properties.add(new UPNP.Producer(producer));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public URI getFirstAlbumArtURI() {
		return properties.getValue(UPNP.AlbumArtURI.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public URI[] getAlbumArtURIs() {
		List<URI> list = properties.getValues(UPNP.AlbumArtURI.class);
		return list.toArray(URI[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicAlbum setAlbumArtURIs(URI[] uris) {
		properties.remove(UPNP.AlbumArtURI.class);
		for (URI uri : uris) {
			properties.add(new UPNP.AlbumArtURI(uri));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getToc() {
		return properties.getValue(UPNP.Toc.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicAlbum setToc(String toc) {
		properties.set(new UPNP.Toc(toc));
		return this;
	}

}
