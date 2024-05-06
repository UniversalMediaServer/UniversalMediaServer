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

import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.StorageMediumValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A playlistContainer instance represents a collection of objects.
 *
 * It is different from a musicAlbum container in the sense that a
 * playlistContainer instance may contain a mix of audio, video and images and
 * is typically created by a user, while an album container typically holds a
 * fixed published sequence of songs (for example, an audio CD). A
 * playlistContainer instance may have a res property for playback of the whole
 * playlist or not. This res property may be a dynamically created playlist
 * resource, as described in subclase D.10.2, or a reference to a playlist file
 * authored outside of the ContentDirectory service (for example, an external
 * M3U file). This is device-dependent. In any case, rendering the playlist has
 * the semantics defined by the playlist resource (for example, ordering,
 * transition effects, etc.). If the playlistContainer instance has no res
 * property, a control point needs to separately initiate rendering for each
 * child object, typically in the order the children are received from a
 * Browse() action. This class is derived from the container class and inherits
 * the properties defined by that class.
 */
public class PlaylistContainer extends Container {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_CONTAINER_PLAYLISTCONTAINER_TYPE);

	public PlaylistContainer() {
		setUpnpClass(CLASS);
	}

	public PlaylistContainer(Container other) {
		super(other);
		setUpnpClass(CLASS);
	}

	public PlaylistContainer(String id, Container parent, String title, String creator, Long childCount) {
		this(id, parent.getId(), title, creator, childCount);
	}

	public PlaylistContainer(String id, String parentID, String title, String creator, Long childCount) {
		super(id, parentID, title, creator, CLASS, childCount);
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
	public PlaylistContainer setArtists(UPNP.Artist[] artists) {
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
	public PlaylistContainer setGenres(String[] genres) {
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
	public PlaylistContainer setLongDescription(String description) {
		properties.set(new UPNP.LongDescription(description));
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
	public PlaylistContainer setProducers(String[] producers) {
		properties.remove(UPNP.Producer.class);
		for (String producer : producers) {
			properties.add(new UPNP.Producer(producer));
		}
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
	public PlaylistContainer setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public PlaylistContainer setStorageMedium(StorageMediumValue storageMedium) {
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
	public PlaylistContainer setDescription(String description) {
		properties.set(new DC.Description(description));
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
	public PlaylistContainer setContributors(String[] contributors) {
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
	public PlaylistContainer setDate(String date) {
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
	public PlaylistContainer setLanguage(String language) {
		properties.set(new DC.Language(language));
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
	public PlaylistContainer setRights(String[] rights) {
		properties.remove(DC.Rights.class);
		for (String right : rights) {
			properties.add(new DC.Rights(right));
		}
		return this;
	}

}
