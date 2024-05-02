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
 * A musicTrack instance represents music audio content (as opposed to, for
 * example, a news broadcast or an audio book).
 *
 * It typically has at least one res property. This class is derived from the
 * audioItem class and inherits the properties defined by that class.
 */
public class MusicTrack extends AudioItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_AUDIOITEM_MUSICTRACK_TYPE);

	public MusicTrack() {
		setUpnpClass(CLASS);
	}

	public MusicTrack(Item other) {
		super(other);
	}

	public MusicTrack(String id, Container parent, String title, String creator, String album, String artist, Res... resource) {
		this(id, parent.getId(), title, creator, album, artist, resource);
	}

	public MusicTrack(String id, Container parent, String title, String creator, String album, UPNP.Artist artist, Res... resource) {
		this(id, parent.getId(), title, creator, album, artist, resource);
	}

	public MusicTrack(String id, String parentID, String title, String creator, String album, String artist, Res... resource) {
		this(id, parentID, title, creator, album, artist == null ? null : new UPNP.Artist(artist), resource);
	}

	public MusicTrack(String id, String parentID, String title, String creator, String album, UPNP.Artist artist, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
		if (album != null) {
			setAlbum(album);
		}
		if (artist != null) {
			properties.add(artist);
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
	public MusicTrack addArtist(UPNP.Artist artist) {
		properties.add(artist);
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicTrack setArtists(UPNP.Artist[] artists) {
		properties.remove(UPNP.Artist.class);
		for (UPNP.Artist artist : artists) {
			properties.add(artist);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getAlbum() {
		return properties.getValue(UPNP.Album.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public final MusicTrack setAlbum(String album) {
		properties.set(new UPNP.Album(album));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public Integer getOriginalTrackNumber() {
		return properties.getValue(UPNP.OriginalTrackNumber.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicTrack setOriginalTrackNumber(Integer number) {
		properties.set(new UPNP.OriginalTrackNumber(number));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstPlaylist() {
		return properties.getValue(UPNP.Playlist.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getPlaylists() {
		List<String> list = properties.getValues(UPNP.Playlist.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicTrack setPlaylists(String[] playlists) {
		properties.remove(UPNP.Playlist.class);
		for (String playlist : playlists) {
			properties.add(new UPNP.Playlist(playlist));
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
	public MusicTrack setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public MusicTrack setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
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
	public MusicTrack setContributors(String[] contributors) {
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
	public MusicTrack setDate(String date) {
		properties.set(new DC.Date(date));
		return this;
	}

}
