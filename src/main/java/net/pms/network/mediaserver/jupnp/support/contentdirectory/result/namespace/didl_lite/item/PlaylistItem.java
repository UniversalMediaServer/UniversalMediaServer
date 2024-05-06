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
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.StorageMediumValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A playlistItem instance represents a playable sequence of resources.
 *
 * It is different from musicAlbum in the sense that a playlistItem MAY contain
 * a mix of audio, video and images and is typically created by a user, while an
 * album is typically a fixed published sequence of songs (for example, an audio
 * CD). A playlistItem is REQUIRED to have a res property for playback of the
 * whole sequence. This res property is a reference to a playlist file authored
 * outside of the ContentDirectory service (for example, an external M3U file).
 * Rendering the playlistItem has the semantics defined by the playlist’s
 * resource (for example, ordering, transition effects, etc.).
 *
 * This class is derived from the item class and inherits the properties defined
 * by that class.
 */
public class PlaylistItem extends Item {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_PLAYLISTITEM_TYPE);

	public PlaylistItem() {
		setUpnpClass(CLASS);
	}

	public PlaylistItem(Item other) {
		super(other);
	}

	public PlaylistItem(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public PlaylistItem(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, CLASS);
		if (resource != null) {
			setResources(resource);
		}
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
	public PlaylistItem setArtists(UPNP.Artist[] artists) {
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
	public PlaylistItem setGenres(String[] genres) {
		properties.remove(UPNP.Genre.class);
		for (String genre : genres) {
			properties.add(new UPNP.Genre(genre));
		}
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
	public PlaylistItem setLongDescription(String description) {
		properties.set(new UPNP.LongDescription(description));
		return this;
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
	public PlaylistItem setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public PlaylistItem setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
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
	public PlaylistItem setDescription(String description) {
		properties.set(new DC.Description(description));
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
	public PlaylistItem setDate(String date) {
		properties.set(new DC.Date(date));
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
	public PlaylistItem setLanguage(String language) {
		properties.set(new DC.Language(language));
		return this;
	}

}
