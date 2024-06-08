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

import com.google.common.primitives.UnsignedInteger;
import java.util.List;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.StorageMediumValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A videoItem instance represents content intended for viewing (as a
 * combination of video and audio).
 *
 * It typically has at least one res property. This class is derived from the
 * item class and inherits the properties defined by that class.
 */
public class Movie extends VideoItem {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_VIDEOITEM_MOVIE_TYPE);

	public Movie() {
		setUpnpClass(CLASS);
	}

	public Movie(Item other) {
		super(other);
	}

	public Movie(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public Movie(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, resource);
		setUpnpClass(CLASS);
	}

	public String getStorageMedium() {
		return properties.getValue(UPNP.StorageMedium.class);
	}

	public Movie setStorageMedium(String storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	public Movie setStorageMedium(StorageMediumValue storageMedium) {
		properties.set(new UPNP.StorageMedium(storageMedium));
		return this;
	}

	public Integer getDVDRegionCode() {
		return properties.getValue(UPNP.DVDRegionCode.class);
	}

	public Movie setDVDRegionCode(Integer dvdRegionCode) {
		properties.set(new UPNP.DVDRegionCode(dvdRegionCode));
		return this;
	}

	public String getChannelName() {
		return properties.getValue(UPNP.ChannelName.class);
	}

	public Movie setChannelName(String channelName) {
		properties.set(new UPNP.ChannelName(channelName));
		return this;
	}

	public String getFirstScheduledStartTime() {
		return properties.getValue(UPNP.ScheduledStartTime.class);
	}

	public String[] getScheduledStartTimes() {
		List<String> list = properties.getValues(UPNP.ScheduledStartTime.class);
		return list.toArray(String[]::new);
	}

	public Movie setScheduledStartTimes(String[] strings) {
		properties.remove(UPNP.ScheduledStartTime.class);
		for (String s : strings) {
			properties.add(new UPNP.ScheduledStartTime(s));
		}
		return this;
	}

	public String getFirstScheduledEndTime() {
		return properties.getValue(UPNP.ScheduledEndTime.class);
	}

	public String[] getScheduledEndTimes() {
		List<String> list = properties.getValues(UPNP.ScheduledEndTime.class);
		return list.toArray(String[]::new);
	}

	public Movie setScheduledEndTimes(String[] strings) {
		properties.remove(UPNP.ScheduledEndTime.class);
		for (String s : strings) {
			properties.add(new UPNP.ScheduledEndTime(s));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getProgramTitle() {
		return properties.getValue(UPNP.ProgramTitle.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setProgramTitle(String programTitle) {
		properties.set(new UPNP.ProgramTitle(programTitle));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getSeriesTitle() {
		return properties.getValue(UPNP.SeriesTitle.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setSeriesTitle(String seriesTitle) {
		properties.set(new UPNP.SeriesTitle(seriesTitle));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public UnsignedInteger getEpisodeCount() {
		return properties.getValue(UPNP.EpisodeCount.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setEpisodeCount(UnsignedInteger episodeCount) {
		properties.set(new UPNP.EpisodeCount(episodeCount));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setEpisodeCount(int episodeCount) {
		return setEpisodeCount(UnsignedInteger.asUnsigned(episodeCount));
	}

	/**
	 * @since ContentDirectory v2
	 */
	public UnsignedInteger getEpisodeNumber() {
		return properties.getValue(UPNP.EpisodeNumber.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setEpisodeNumber(UnsignedInteger episodeNumber) {
		properties.set(new UPNP.EpisodeNumber(episodeNumber));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setEpisodeNumber(int episodeNumber) {
		return setEpisodeNumber(UnsignedInteger.asUnsigned(episodeNumber));
	}

	/**
	 * @since ContentDirectory v4
	 */
	public UnsignedInteger getEpisodeSeason() {
		return properties.getValue(UPNP.EpisodeSeason.class);
	}

	/**
	 * @since ContentDirectory v4
	 */
	public VideoItem setEpisodeSeason(UnsignedInteger episodeSeason) {
		properties.set(new UPNP.EpisodeSeason(episodeSeason));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setEpisodeSeason(int episodeSeason) {
		return setEpisodeSeason(UnsignedInteger.asUnsigned(episodeSeason));
	}

}
