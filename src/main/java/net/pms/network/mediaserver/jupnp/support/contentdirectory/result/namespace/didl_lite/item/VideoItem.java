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
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DayOfWeekValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;

/**
 * A videoItem instance represents content intended for viewing (as a
 * combination of video and audio).
 *
 * It typically has at least one res property. This class is derived from the
 * item class and inherits the properties defined by that class.
 */
public class VideoItem extends Item {

	private static final UPNP.Class CLASS = new UPNP.Class(DIDL_LITE.OBJECT_ITEM_VIDEOITEM_TYPE);

	public VideoItem() {
		setUpnpClass(CLASS);
	}

	public VideoItem(Item other) {
		super(other);
	}

	public VideoItem(String id, Container parent, String title, String creator, Res... resource) {
		this(id, parent.getId(), title, creator, resource);
	}

	public VideoItem(String id, String parentID, String title, String creator, Res... resource) {
		super(id, parentID, title, creator, CLASS);
		if (resource != null) {
			setResources(resource);
		}
	}

	public String getFirstGenre() {
		return properties.getValue(UPNP.Genre.class);
	}

	public String[] getGenres() {
		List<String> list = properties.getValues(UPNP.Genre.class);
		return list.toArray(String[]::new);
	}

	public VideoItem setGenres(String[] genres) {
		properties.remove(UPNP.Genre.class);
		for (String genre : genres) {
			properties.add(new UPNP.Genre(genre));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public VideoItem addGenre(String genre) {
		properties.add(new UPNP.Genre(genre));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem addGenre(String genre, String id) {
		properties.add(new UPNP.Genre(genre, id, null));
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
	public VideoItem setLongDescription(String description) {
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
	public VideoItem setProducers(String[] producers) {
		properties.remove(UPNP.Producer.class);
		for (String producer : producers) {
			properties.add(new UPNP.Producer(producer));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getRating() {
		return properties.getValue(UPNP.Rating.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public VideoItem setRating(String rating) {
		properties.set(new UPNP.Rating(rating));
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public UPNP.Actor getFirstActor() {
		return properties.get(UPNP.Actor.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public UPNP.Actor[] getActors() {
		List<UPNP.Actor> list = properties.getAll(UPNP.Actor.class);
		return list.toArray(UPNP.Actor[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public VideoItem setActors(UPNP.Actor[] actors) {
		properties.remove(UPNP.Actor.class);
		for (UPNP.Actor actor : actors) {
			properties.add(actor);
		}
		return this;
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String getFirstDirector() {
		return properties.getValue(UPNP.Director.class);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public String[] getDirectors() {
		List<String> list = properties.getValues(UPNP.Director.class);
		return list.toArray(String[]::new);
	}

	/**
	 * @since ContentDirectory v1
	 */
	public VideoItem setDirectors(String[] directors) {
		properties.remove(UPNP.Director.class);
		for (String director : directors) {
			properties.add(new UPNP.Director(director));
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
	public VideoItem setDescription(String description) {
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
	public VideoItem setPublishers(String[] publishers) {
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
	public VideoItem setLanguage(String language) {
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
	public VideoItem setRelations(URI[] relations) {
		properties.remove(DC.Relation.class);
		for (URI relation : relations) {
			properties.add(new DC.Relation(relation));
		}
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public Integer getPlaybackCount() {
		return properties.getValue(UPNP.PlaybackCount.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setPlaybackCount(Integer playbackCount) {
		properties.set(new UPNP.PlaybackCount(playbackCount));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getLastPlaybackTime() {
		return properties.getValue(UPNP.LastPlaybackTime.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setLastPlaybackTime(String lastPlaybackTime) {
		properties.set(new UPNP.LastPlaybackTime(lastPlaybackTime));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getLastPlaybackPosition() {
		return properties.getValue(UPNP.LastPlaybackPosition.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setLastPlaybackPosition(String lastPlaybackPosition) {
		properties.set(new UPNP.LastPlaybackPosition(lastPlaybackPosition));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getRecordedDayOfWeek() {
		return properties.getValue(UPNP.RecordedDayOfWeek.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setRecordedDayOfWeek(DayOfWeekValue recordedDayOfWeek) {
		properties.set(new UPNP.RecordedDayOfWeek(recordedDayOfWeek));
		return this;
	}

	/**
	 * @since ContentDirectory v2
	 */
	public String getSrsRecordScheduleID() {
		return properties.getValue(UPNP.SrsRecordScheduleID.class);
	}

	/**
	 * @since ContentDirectory v2
	 */
	public VideoItem setSrsRecordScheduleID(String srsRecordScheduleID) {
		properties.set(new UPNP.SrsRecordScheduleID(srsRecordScheduleID));
		return this;
	}

}
