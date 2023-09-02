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
 * A musicVideoClip instance represents video content that is a clip supporting
 * a song (as opposed to, for example, a continuous TV broadcast or a movie).
 *
 * It typically has at least one res property. This class is derived from the
 * videoItem class and inherits the properties defined by that class.
 */
public class MusicVideoClip extends VideoItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_VIDEOITEM_MUSICVIDEOCLIP_TYPE);

	public MusicVideoClip() {
		setUpnpClass(CLASS);
	}

	public MusicVideoClip(Item other) {
		super(other);
	}

	public MusicVideoClip(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public MusicVideoClip(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
	}

	public UPNP.Artist getFirstArtist() {
		return properties.get(UPNP.Artist.class);
	}

	public List<UPNP.Artist> getArtists() {
		return properties.getAll(UPNP.Artist.class);
	}

	public MusicVideoClip setArtists(UPNP.Artist[] artists) {
		properties.remove(UPNP.Artist.class);
		for (UPNP.Artist artist : artists) {
			properties.add(artist);
		}
		return this;
	}

	public String getStorageMedium() {
		return properties.getValue(UPNP.StorageMedium.class);
	}

	public MusicVideoClip setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	public MusicVideoClip setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	public String getAlbum() {
		return properties.getValue(UPNP.Album.class);
	}

	public MusicVideoClip setAlbum(String album) {
		properties.set(new UPNP.Album(album));
		return this;
	}

	public String getFirstScheduledStartTime() {
		return properties.getValue(UPNP.ScheduledStartTime.class);
	}

	public String[] getScheduledStartTimes() {
		List<String> list = properties.getValues(UPNP.ScheduledStartTime.class);
		return list.toArray(String[]::new);
	}

	public MusicVideoClip setScheduledStartTimes(String[] strings) {
		properties.remove(UPNP.ScheduledStartTime.class);
		for (String s : strings) {
			properties.add(new UPNP.ScheduledStartTime(s));
		}
		return this;
	}

	public String getFirstScheduledStopTime() {
		return properties.getValue(UPNP.ScheduledStopTime.class);
	}

	public String[] getScheduledStopTimes() {
		List<String> list = properties.getValues(UPNP.ScheduledStopTime.class);
		return list.toArray(String[]::new);
	}

	public MusicVideoClip setScheduledStopTimes(String[] strings) {
		properties.remove(UPNP.ScheduledStopTime.class);
		for (String s : strings) {
			properties.add(new UPNP.ScheduledStopTime(s));
		}
		return this;
	}

	public String getFirstContributor() {
		return properties.getValue(DC.Contributor.class);
	}

	public String[] getContributors() {
		List<String> list = properties.getValues(DC.Contributor.class);
		return list.toArray(String[]::new);
	}

	public MusicVideoClip setContributors(String[] contributors) {
		properties.remove(DC.Contributor.class);
		for (String contributor : contributors) {
			properties.add(new DC.Contributor(contributor));
		}
		return this;
	}

	public String getDate() {
		return properties.getValue(DC.Date.class);
	}

	public MusicVideoClip setDate(String date) {
		properties.set(new DC.Date(date));
		return this;
	}

}
