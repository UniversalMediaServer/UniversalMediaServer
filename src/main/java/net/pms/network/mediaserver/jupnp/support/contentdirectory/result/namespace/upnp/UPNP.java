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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp;

import com.google.common.primitives.UnsignedInteger;
import java.net.URI;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DayOfWeekValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DaylightSavingValue;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dlna.DLNA;

public class UPNP {

	public static final String NAMESPACE_URI = "urn:schemas-upnp-org:metadata-1-0/upnp/";

	@SuppressWarnings({ "checkstyle:InterfaceIsType" })
	public interface NAMESPACE {
	}

	/**
	 * The upnp:actor property indicates the name of an actor performing in
	 * (part of) the content.
	 *
	 * Multi-Valued
	 */
	public static class Actor extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:actor@role property indicates the role of the actor in the
		 * work.
		 */
		public static class Role extends Property<String> implements NAMESPACE {

			public Role() {
			}

			public Role(String value) {
				super(value, "role");
			}
		}

		public Actor() {
		}

		public Actor(String value) {
			super(value, "actor");
		}

		public Actor(String value, String role) {
			super(value, "actor");
			setRole(role);
		}

		public String getRole() {
			return dependentProperties.getValue(Actor.Role.class);
		}

		public final Actor setRole(String value) {
			dependentProperties.set(new Actor.Role(value));
			return this;
		}
	}

	/**
	 * The upnp:album property indicates the title of the album to which the
	 * content item belongs.
	 *
	 * Multi-Valued
	 */
	public static class Album extends Property<String> implements NAMESPACE {

		public Album() {
		}

		public Album(String value) {
			super(value, "album");
		}
	}

	/**
	 * The upnp:albumArtURI property contains a reference to album art.
	 *
	 * The value MUST be a properly escaped URI as described in [RFC 2396].
	 *
	 * Multi-Valued
	 */
	public static class AlbumArtURI extends Property<URI> implements NAMESPACE {

		public AlbumArtURI() {
			this(null);
		}

		public AlbumArtURI(URI value) {
			super(value, "albumArtURI");
		}

		public String getProfileID() {
			return dependentProperties.getValue(DLNA.ProfileID.class);
		}

		public final AlbumArtURI setProfileID(String value) {
			dependentProperties.set(new DLNA.ProfileID(value));
			return this;
		}
	}

	/**
	 * The upnp:artist property indicates the name of an artist.
	 *
	 * Multi-Valued
	 */
	public static class Artist extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:artist@role property indicates the role of the artist in the
		 * work.
		 */
		public static class Role extends Property<String> implements NAMESPACE {

			public Role() {
			}

			public Role(String value) {
				super(value, "role");
			}
		}

		public Artist() {
		}

		public Artist(String value) {
			super(value, "artist");
		}

		public Artist(String value, String role) {
			super(value, "artist");
			setRole(role);
		}

		public String getRole() {
			return dependentProperties.getValue(Artist.Role.class);
		}

		public final Artist setRole(String value) {
			dependentProperties.set(new Artist.Role(value));
			return this;
		}
	}

	/**
	 * The upnp:artistDiscographyURI property contains a reference to the
	 * artist’s discography.
	 *
	 * The value MUST be a properly escaped URI as described in [RFC 2396].
	 */
	public static class ArtistDiscographyURI extends Property<URI> implements NAMESPACE {

		public ArtistDiscographyURI() {
			this(null);
		}

		public ArtistDiscographyURI(URI value) {
			super(value, "artistDiscographyURI");
		}
	}

	/**
	 * The upnp:author property indicates the name of an author contributing to
	 * the content (for example, the writer of a text book).
	 *
	 * Multi-Valued
	 */
	public static class Author extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:author@role property indicates the role of the author in the
		 * work.
		 */
		public static class Role extends Property<String> implements NAMESPACE {

			public Role() {
			}

			public Role(String value) {
				super(value, "role");
			}
		}

		public Author() {
		}

		public Author(String value) {
			super(value, "author");
		}

		public Author(String value, String role) {
			super(value, "author");
			setRole(role);
		}

		public String getRole() {
			return dependentProperties.getValue(Author.Role.class);
		}

		public final Author setRole(String value) {
			dependentProperties.set(new Author.Role(value));
			return this;
		}
	}

	/**
	 * The read/write upnp:bookmarkID property contains the object ID of a
	 * bookmark item that is associated with this content item and that marks a
	 * specific location within its content.
	 */
	public static class BookmarkID extends Property<String> implements NAMESPACE {

		public BookmarkID() {
		}

		public BookmarkID(String value) {
			super(value, "bookmarkID");
		}
	}

	/**
	 * The read/write upnp:bookmarkedObjectID property contains the object ID of
	 * the content item that is bookmarked by this bookmark.
	 */
	public static class BookmarkedObjectID extends Property<String> implements NAMESPACE {

		public BookmarkedObjectID() {
		}

		public BookmarkedObjectID(String value) {
			super(value, "bookmarkedObjectID");
		}
	}

	/**
	 * The upnp:channelNr property contains the number of the associated
	 * broadcast channel.
	 *
	 * This is typically used for live content or recorded content. If there
	 * exists a upnp:channelID property with its dependent property
	 * upnp:channelID@type property set to “DIGITAL”, then the upnp:channelNr
	 * property shall be set equal to the major channel number from that
	 * upnp:channelID property. Else, if there exists a upnp:channelID property
	 * with its dependent upnp:channelID@type property set to “ANALOG”, then the
	 * upnp:channelNr property shall be set equal to the value of that
	 * upnp:channelID property. Else, the upnp:channelNr property shall not
	 * exist.
	 */
	public static class ChannelNr extends Property<Integer> implements NAMESPACE {

		public ChannelNr() {
			this(null);
		}

		public ChannelNr(Integer value) {
			super(value, "channelNr");
		}
	}

	/**
	 * The upnp:callSign property contains the broadcast station call sign of
	 * the associated broadcast channel.
	 *
	 * This is typically used for live content or recorded content.
	 *
	 * Example: “KGW”.
	 *
	 * If the upnp:callSign property is supported and upnp:class =
	 * “object.item.audioItem.audioBroadcast” then the upnp:radioCallSign
	 * property shall also be supported and shall be set equal to the value of
	 * the upnp:callSign property.
	 */
	public static class CallSign extends Property<String> implements NAMESPACE {

		public CallSign() {
			this(null);
		}

		public CallSign(String value) {
			super(value, "callSign");
		}
	}

	/**
	 * The upnp:channelName property contains the user-friendly name of the
	 * associated broadcast channel.
	 *
	 * This is typically used for live or recorded content.
	 */
	public static class ChannelName extends Property<String> implements NAMESPACE {

		public ChannelName() {
			this(null);
		}

		public ChannelName(String value) {
			super(value, "channelName");
		}
	}

	/**
	 * The upnp:class property is a required property and it indicates the class
	 * of the object.
	 *
	 * Default Value: N/A – The property is required
	 */
	public static class Class extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:class@name property indicates a friendly name for the class
		 * of the object.
		 *
		 * This should not be used for class-based searches as it is not
		 * guaranteed to be unique or consistent across content items of the
		 * same class.
		 */
		public static class Name extends Property<String> implements NAMESPACE {

			public Name() {
			}

			public Name(String value) {
				super(value, "name");
			}
		}

		public Class() {
		}

		public Class(String value) {
			super(value, "class");
		}

		public Class(String value, String name) {
			super(value, "class");
			setName(name);
		}

		public String getName() {
			return dependentProperties.getValue(Class.Name.class);
		}

		public final Class setName(String value) {
			dependentProperties.set(new Class.Name(value));
			return this;
		}
	}

	/**
	 * The read-only upnp:containerUpdateID property is an allowed property for
	 * all container objects (that is: objects that are derived from the
	 * container class) that contains the value of the SystemUpdateID state
	 * variable generated by the most recent Container Modification for that
	 * container.
	 *
	 * If implemented, the value of the upnp:containerUpdateID property shall be
	 * preserved even while off-line except when the Service Reset Procedure is
	 * invoked.
	 */
	public static class ContainerUpdateID extends Property<UnsignedInteger> implements NAMESPACE {

		public ContainerUpdateID() {
			this(null);
		}

		public ContainerUpdateID(UnsignedInteger value) {
			super(value, "containerUpdateID");
		}
	}

	/**
	 * The read-only upnp:createClass property is only applicable to container
	 * objects.
	 *
	 * It contains a class that can be created within the container object.
	 *
	 * Multi-Valued
	 */
	public static class CreateClass extends Property<String> implements NAMESPACE {

		/**
		 * The read-only upnp:createClass@name property indicates a friendly
		 * name for the class.
		 */
		public static class Name extends Property<String> implements NAMESPACE {

			public Name() {
			}

			public Name(String value) {
				super(value, "name");
			}
		}

		/**
		 * The read-only upnp:createClass@includeDerived property is a required
		 * property of the associated upnp:createClass property and indicates
		 * whether the class specified shall also include derived classes.
		 *
		 * When set to “1”, derived classes shall be included. When set to “0”,
		 * derived classes shall be excluded.
		 *
		 * The property is required when the upnp:createClass property is
		 * present.
		 */
		public static class IncludeDerived extends Property<Boolean> implements NAMESPACE {

			public IncludeDerived() {
			}

			public IncludeDerived(Boolean value) {
				super(value, "createClass");
			}
		}

		public CreateClass() {
		}

		public CreateClass(String value, Boolean includeDerived) {
			super(value, "createClass");
			setIncludeDerived(includeDerived);
		}

		public CreateClass(String value, String name, Boolean includeDerived) {
			super(value, "createClass");
			setName(name);
			setIncludeDerived(includeDerived);
		}

		public String getName() {
			return dependentProperties.getValue(CreateClass.Name.class);
		}

		public final CreateClass setName(String value) {
			dependentProperties.set(new CreateClass.Name(value));
			return this;
		}

		public Boolean getIncludeDerived() {
			return dependentProperties.getValue(CreateClass.IncludeDerived.class);
		}

		public final CreateClass setIncludeDerived(Boolean value) {
			dependentProperties.set(new SearchClass.IncludeDerived(value));
			return this;
		}
	}

	/**
	 * The read/write upnp:deviceUDN property contains the UDN of the device
	 * whose state information is captured in the values of the
	 * upnp:stateVariableCollection properties within the same bookmark item.
	 */
	public static class DeviceUDN extends Property<String> implements NAMESPACE {

		/**
		 * The read/write upnp:deviceUDN@serviceType property contains the
		 * service type of the device whose UDN is stored in the associated
		 * upnp:deviceUDN property.
		 *
		 * Note that the service type includes the name and version number, such
		 * as “AVTransport:1”.
		 *
		 * The upnp:deviceUDN@serviceType property is required if the
		 * upnp:deviceUDN property is specified.
		 */
		public static class ServiceType extends Property<String> implements NAMESPACE {

			public ServiceType() {
			}

			public ServiceType(String value) {
				super(value, "serviceType");
			}
		}

		/**
		 * The read/write upnp:deviceUDN@serviceId property contains the
		 * serviceId of the device whose UDN is stored in the associated
		 * upnp:deviceUDN property.
		 *
		 * The upnp:deviceUDN@serviceId property is required if the
		 * upnp:deviceUDN property is specified.
		 */
		public static class ServiceId extends Property<String> implements NAMESPACE {

			public ServiceId() {
			}

			public ServiceId(String value) {
				super(value, "serviceId");
			}
		}

		public DeviceUDN() {
		}

		public DeviceUDN(String value, String serviceType, String serviceId) {
			super(value, "deviceUDN");
			setServiceType(serviceType);
			setServiceId(serviceId);
		}

		public String getServiceType() {
			return dependentProperties.getValue(DeviceUDN.ServiceType.class);
		}

		public final DeviceUDN setServiceType(String value) {
			dependentProperties.set(new DeviceUDN.ServiceType(value));
			return this;
		}

		public String getServiceId() {
			return dependentProperties.getValue(DeviceUDN.ServiceId.class);
		}

		public final DeviceUDN setServiceId(String value) {
			dependentProperties.set(new DeviceUDN.ServiceId(value));
			return this;
		}
	}

	/**
	 * The upnp:director property indicates the name of a director of the
	 * content (for example, a movie).
	 */
	public static class Director extends Property<String> implements NAMESPACE {

		public Director() {
		}

		public Director(String value) {
			super(value, "director");
		}
	}

	/**
	 * The upnp:DVDRegionCode property contains the region code of the DVD disc.
	 */
	public static class DVDRegionCode extends Property<Integer> implements NAMESPACE {

		public DVDRegionCode() {
			this(null);
		}

		public DVDRegionCode(Integer value) {
			super(value, "DVDRegionCode");
		}
	}

	/**
	 * The upnp:episodeCount property contains the total number of episodes in
	 * the series to which this content belongs.
	 */
	public static class EpisodeCount extends Property<UnsignedInteger> implements NAMESPACE {

		public EpisodeCount() {
		}

		public EpisodeCount(UnsignedInteger value) {
			super(value, "episodeCount");
		}
	}

	/**
	 * The upnp:episodeSeason property indicates the season of the episode.
	 *
	 * Example: 1 indicates season one.
	 *
	 * Note: the dc:date property can be used to convey the year of the first
	 * broadcast of the episode.
	 */
	public static class EpisodeSeason extends Property<UnsignedInteger> implements NAMESPACE {

		public EpisodeSeason() {
		}

		public EpisodeSeason(UnsignedInteger value) {
			super(value, "episodeSeason");
		}
	}

	/**
	 * The upnp:episodeNumber property contains the episode number of this
	 * recorded content within the series to which this content belongs.
	 */
	public static class EpisodeNumber extends Property<UnsignedInteger> implements NAMESPACE {

		public EpisodeNumber() {
		}

		public EpisodeNumber(UnsignedInteger value) {
			super(value, "episodeNumber");
		}
	}

	/**
	 * The upnp:genre property indicates the genre to which an object belongs.
	 */
	public static class Genre extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:genre@id property identifies the genre scheme which defines
		 * the set of names used in the upnp:genre and upnp:genre@extended
		 * property.
		 *
		 * The format of the upnp:genre@id is:
		 * <ICANN registered domain> “_” <genre_scheme_id>. Example:
		 * “epg.com_GenreSet1” The upnp:genre@id property is required if the
		 * upnp:genre@extended property is specified.
		 *
		 * Default Value: N/A – This property is required when the
		 * upnp:genre@extended property is present.
		 */
		public static class Id extends Property<String> implements NAMESPACE {

			public Id() {
			}

			public Id(String value) {
				super(value, "id");
			}
		}

		/**
		 * The upnp:genre@extended property shall be a CSV list of genre names,
		 * which are individually displayable strings, representing increasingly
		 * precise (sub)genre names.
		 *
		 * The list shall be ordered with the most general genre first. The
		 * first entry in the list shall be equal to the value of the upnp:genre
		 * property.
		 *
		 * Example: “Sports,Basketball,NBA”
		 */
		public static class Extended extends Property<String> implements NAMESPACE {

			public Extended() {
			}

			public Extended(String value) {
				super(value, "extended");
			}
		}

		public Genre() {
		}

		public Genre(String value) {
			super(value, null);
		}

		public Genre(String value, String id, String extended) {
			super(value, null);
			setId(id);
			setExtended(extended);
		}

		public String getId() {
			return dependentProperties.getValue(Genre.Id.class);
		}

		public final Genre setId(String value) {
			dependentProperties.set(new Genre.Id(value));
			return this;
		}

		public String getExtended() {
			return dependentProperties.getValue(Genre.Extended.class);
		}

		public final Genre setExtended(String value) {
			dependentProperties.set(new Genre.Extended(value));
			return this;
		}
	}

	/**
	 * The upnp:icon property contains a URI to some icon that a control point
	 * can use in its UI to display the content.
	 *
	 * for example, a CNN logo for a Tuner channel.
	 *
	 * It is recommended that the same format be used as is used for the icon
	 * element in the UPnP device description document schema (PNG). The value
	 * shall be a properly escaped URI as described in [40].
	 */
	public static class Icon extends Property<URI> implements NAMESPACE {

		public Icon() {
			this(null);
		}

		public Icon(URI value) {
			super(value, "icon");
		}
	}

	/**
	 * The upnp:lastPlaybackPosition property conbtains the time offset within
	 * the content where the last playback was suspended.
	 *
	 * The format of the upnp:lastPlaybackPosition property shall comply with
	 * the duration syntax as defined in Annex E.
	 *
	 * The criteria for determining the time offset in the content where the
	 * playback of the content has been suspended, is device dependent.
	 */
	public static class LastPlaybackPosition extends Property<String> implements NAMESPACE {

		public LastPlaybackPosition() {
			this(null);
		}

		public LastPlaybackPosition(String value) {
			super(value, "lastPlaybackPosition");
		}
	}

	/**
	 * The upnp:lastPlaybackTime property contains the date&ime of the last
	 * playback.
	 *
	 * The format of the upnp:lastPlaybackTime property shall comply with the
	 * date-time syntax as defined in Annex E.
	 *
	 * The criteria for determining when the content has been played last, is
	 * device dependent.
	 */
	public static class LastPlaybackTime extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:lastPlaybackTime@daylightSaving property indicates whether
		 * the time value used in the upnp:lastPlaybackTime property is
		 * expressed using as a reference either Daylight Saving Time or
		 * Standard Time.
		 *
		 * This property is only applicable when the time value in the
		 * upnp:lastPlaybackTime property is expressed in local time. Whenever
		 * the time value in the upnp:lastPlaybackTime property is expressed in
		 * absolute time, the upnp:lastPlaybackTime@daylightSaving property
		 * shall not be present on output and shall be ignored on input.
		 */
		public static class DaylightSaving extends Property<String> implements NAMESPACE {

			public DaylightSaving() {
			}

			public DaylightSaving(String value) {
				super(value, "daylightSaving");
			}
		}

		public LastPlaybackTime() {
			this(null);
		}

		public LastPlaybackTime(String value) {
			super(value, "lastPlaybackTime");
		}

		public LastPlaybackTime(String value, DaylightSavingValue daylightSaving) {
			super(value, "lastPlaybackTime");
			setDaylightSaving(daylightSaving);
		}

		public String getDaylightSaving() {
			return dependentProperties.getValue(Genre.Extended.class);
		}

		public final LastPlaybackTime setDaylightSaving(String value) {
			dependentProperties.set(new Genre.Extended(value));
			return this;
		}

		public final LastPlaybackTime setDaylightSaving(DaylightSavingValue value) {
			dependentProperties.set(new LastPlaybackTime.DaylightSaving(value.toString()));
			return this;
		}
	}

	/**
	 * The upnp:longDescription property contains a few lines of description of
	 * the content item (longer than the dc:description property).
	 */
	public static class LongDescription extends Property<String> implements NAMESPACE {

		public LongDescription() {
			this(null);
		}

		public LongDescription(String value) {
			super(value, "longDescription");
		}
	}

	/**
	 * The upnp:lyricsURI property contains a reference to lyrics of the song or
	 * of the whole album.
	 *
	 *
	 * The value MUST be a properly escaped URI as described in [RFC 2396].
	 */
	public static class LyricsURI extends Property<URI> implements NAMESPACE {

		public LyricsURI() {
			this(null);
		}

		public LyricsURI(URI value) {
			super(value, "lyricsURI");
		}
	}

	/**
	 * The read-only upnp:objectUpdateID property is an allowed property that
	 * contains the value the SystemUpdateID state variable that was generated
	 * when the object experienced its latest Object Modification.
	 *
	 * In other words, the upnp:objectUpdateID property represents a
	 * last-modified timestamp for the object relative to the SystemUpdateID
	 * state variable. If implemented, the upnp:objectUpdateID property shall be
	 * preserved even while off-line except when the Service Reset Procedure is
	 * invoked.
	 */
	public static class ObjectUpdateID extends Property<UnsignedInteger> implements NAMESPACE {

		public ObjectUpdateID() {
			this(null);
		}

		public ObjectUpdateID(UnsignedInteger value) {
			super(value, "objectUpdateID");
		}
	}

	/**
	 * The upnp:originalTrackNumber property contains the original track number
	 * on an audio CD or other medium.
	 */
	public static class OriginalTrackNumber extends Property<Integer> implements NAMESPACE {

		public OriginalTrackNumber() {
			this(null);
		}

		public OriginalTrackNumber(Integer value) {
			super(value, "originalTrackNumber");
		}
	}

	/**
	 * The read-only upnp:payPerView property indicates whether the object
	 * represents pay-per-view content.
	 *
	 * When set to “1”, the object is a pay-per-view object.
	 *
	 * When set to “0”, the object is not a pay-per-view object.
	 */
	public static class PayPerView extends Property<Boolean> implements NAMESPACE {

		public PayPerView() {
			this(null);
		}

		public PayPerView(Boolean value) {
			super(value, "payPerView");
		}
	}

	/**
	 * The read-only upnp:playbackCount property contains the number of times
	 * the content has been played.
	 *
	 * The special value -1 means that the content has been played but the count
	 * is unknown.
	 *
	 * The criteria for determining whether the content has been played, is
	 * device dependent.
	 */
	public static class PlaybackCount extends Property<Integer> implements NAMESPACE {

		public PlaybackCount() {
			this(null);
		}

		public PlaybackCount(Integer value) {
			super(value, "playbackCount");
		}
	}

	/**
	 * The upnp:playlist property indicates the name of a playlist (the dc:title
	 * of a playlistItem) to which the content item belongs.
	 *
	 * Multi-Valued
	 */
	public static class Playlist extends Property<String> implements NAMESPACE {

		public Playlist() {
		}

		public Playlist(String value) {
			super(value, "playlist");
		}
	}

	/**
	 * The read-only upnp:price property contains the price for a broadcast,
	 * series, program, movie, etc.
	 *
	 * Multi-Valued
	 */
	public static class Price extends Property<Float> implements NAMESPACE {

		/**
		 * The read-only upnp:price@currency property indicates the unit of
		 * currency used for the upnp:price property.
		 *
		 * The allowed values for this property shall adhere to ISO 4217, “Type
		 * Currency Code List”.
		 *
		 * The upnp:price@currency property is required if the upnp:price
		 * property is specified.
		 */
		public static class Currency extends Property<String> implements NAMESPACE {

			public Currency() {
			}

			public Currency(String value) {
				super(value, "currency");
			}
		}

		public Price() {
		}

		public Price(Float value, String currency) {
			super(value, "price");
			setCurrency(currency);
		}

		public String getCurrency() {
			return dependentProperties.getValue(Price.Currency.class);
		}

		public final Price setCurrency(String value) {
			dependentProperties.set(new Price.Currency(value));
			return this;
		}
	}

	/**
	 * The upnp:producer property indicates the name of a producer of the
	 * content (for example, a movie or a CD).
	 */
	public static class Producer extends Property<String> implements NAMESPACE {

		public Producer() {
		}

		public Producer(String value) {
			super(value, "producer");
		}
	}

	/**
	 * The upnp:programTitle property contains the name of the program.
	 *
	 * This is most likely obtained from a database that contains
	 * program-related information, such as an Electronic Program Guide.
	 *
	 * Example: “Friends Series Finale”.
	 *
	 * Note: To be precise, this is different from the dc:title property which
	 * indicates a friendly name for the ContentDirectory service object.
	 * However, in many cases, the dc:title property will be set to the same
	 * value as the upnp:programTitle property.
	 */
	public static class ProgramTitle extends Property<String> implements NAMESPACE {

		public ProgramTitle() {
		}

		public ProgramTitle(String value) {
			super(value, "programTitle");
		}
	}

	/**
	 * The upnp:radioBand property contains the radio station frequency band.
	 */
	public static class RadioBand extends Property<String> implements NAMESPACE {

		public RadioBand() {
			this(null);
		}

		public RadioBand(String value) {
			super(value, "radioBand");
		}
	}

	/**
	 * The upnp:radioCallSign property contains a radio station call sign.
	 *
	 * for example, “KSJO”.
	 */
	public static class RadioCallSign extends Property<String> implements NAMESPACE {

		public RadioCallSign() {
			this(null);
		}

		public RadioCallSign(String value) {
			super(value, "radioCallSign");
		}
	}

	/**
	 * The upnp:radioStationID property contains some identification.
	 *
	 * for example, “107.7”, broadcast frequency of the radio station.
	 */
	public static class RadioStationID extends Property<String> implements NAMESPACE {

		public RadioStationID() {
			this(null);
		}

		public RadioStationID(String value) {
			super(value, "radioStationID");
		}
	}

	/**
	 * The upnp:rating property contains the viewer rating value of the content
	 * of this item expressed in the rating system indicated by the
	 * upnp:rating@type property.
	 *
	 * The format and semantics of the upnp:rating property are identical to
	 * those of the srs:matchedRating property, defined in the
	 * ScheduledRecording service specification. Refer to the ScheduledRecording
	 * service specification [SRS] for details.
	 *
	 * Multi-Valued
	 */
	public static class Rating extends Property<String> implements NAMESPACE {

		/**
		 * The upnp:rating@type property indicates the rating system used in the
		 * upnp:rating property.
		 *
		 * The format and allowed values of the upnp:rating@type property are
		 * identical to those of the srs:matchedRating@type property, defined in
		 * the ScheduledRecording service specification.
		 *
		 * The upnp:rating@type property is highly RECOMMENDED if the
		 * upnp:rating property is specified.
		 */
		public static class Type extends Property<String> implements NAMESPACE {

			public Type() {
			}

			public Type(String value) {
				super(value, "type");
			}
		}

		/**
		 * The upnp:rating@advice property indicates the advice that can be
		 * included in the rating system used in the upnp:rating property.
		 *
		 * The upnp:rating@advice property is highly recommended if the
		 * upnp:rating property is specified and the referenced rating system
		 * gives valid advice.
		 */
		public static class Advice extends Property<String> implements NAMESPACE {

			public Advice() {
			}

			public Advice(String value) {
				super(value, "advice");
			}
		}

		/**
		 * The upnp:rating@equivalentAge property indicates equivalent age of
		 * the rating system used in the upnp:rating property (as indicated by
		 * the upnp:rating@type property).
		 *
		 * The equivalent age is the minimum age of the underlying rating system
		 * which allows the user to view the content. The format and allowed
		 * values of the upnp:rating@equivalentAge property are identical to
		 * those of the srs:matchedRating@equivalentAge property, defined in the
		 * ScheduledRecording service specification. Refer to the
		 * ScheduledRecording service specification [25] for details.
		 *
		 * The upnp:rating@equivalentAge property is highly recommended if the
		 * upnp:rating property is specified and the rating of referenced rating
		 * system can be translated to a minimum age. If the rating system does
		 * not specify equivalent age values, or the specific rating value does
		 * not specify an equivalent age, then this property is not allowed.
		 */
		public static class EquivalentAge extends Property<String> implements NAMESPACE {

			public EquivalentAge() {
			}

			public EquivalentAge(String value) {
				super(value, "equivalentAge");
			}
		}

		public Rating() {
		}

		public Rating(String value) {
			super(value, "rating");
		}

		public Rating(String value, String type) {
			super(value, "rating");
			setType(type);

		}

		public String getType() {
			return dependentProperties.getValue(Rating.Type.class);
		}

		public final Rating setType(String value) {
			dependentProperties.set(new Rating.Type(value));
			return this;
		}

		public String getAdvice() {
			return dependentProperties.getValue(Rating.Advice.class);
		}

		public final Rating setAdvice(String value) {
			dependentProperties.set(new Rating.Advice(value));
			return this;
		}

		public String getEquivalentAge() {
			return dependentProperties.getValue(Rating.EquivalentAge.class);
		}

		public final Rating setEquivalentAge(String value) {
			dependentProperties.set(new Rating.EquivalentAge(value));
			return this;
		}
	}

	/**
	 * When the upnp:recordable property is set to “1”, the content represented
	 * by this object can potentially be used for recording purposes.
	 *
	 * If the object is not self-contained (such as an object of class other
	 * than “object.item.epgItem”), other information might be needed to set up
	 * the recording. When set to “0”, the content represented by this object is
	 * not accessible for recording due to various reasons, such as hardware
	 * limitations.
	 *
	 * Default Value: “1”.
	 */
	public static class Recordable extends Property<Boolean> implements NAMESPACE {

		public Recordable() {
			this(null);
		}

		public Recordable(Boolean value) {
			super(value, "recordable");
		}
	}

	/**
	 * The upnp:recordedDayOfWeek property contains the day of the week when the
	 * recording started.
	 */
	public static class RecordedDayOfWeek extends Property<String> implements NAMESPACE {

		public RecordedDayOfWeek() {
		}

		public RecordedDayOfWeek(String value) {
			super(value, "recordedDayOfWeek");
		}

		public RecordedDayOfWeek(DayOfWeekValue value) {
			super(value.toString(), "recordedDayOfWeek");
		}
	}

	/**
	 * The upnp:region property contains some identification of the region,
	 * associated with the source of the object.
	 *
	 * for example, “US”, “Latin America”, “Seattle”.
	 */
	public static class Region extends Property<String> implements NAMESPACE {

		public Region() {
		}

		public Region(String value) {
			super(value, "region");
		}
	}

	/**
	 * The upnp:scheduledStartTime property is used to indicate the start time
	 * of a scheduled program, intended for use by tuners.
	 *
	 * The format MUST be compliant to [ISO 8601] and SHOULD be compliant to
	 * [RFC 3339].
	 *
	 * Multi-Valued
	 */
	public static class ScheduledEndTime extends Property<String> implements NAMESPACE {

		public ScheduledEndTime() {
			this(null);
		}

		public ScheduledEndTime(String value) {
			super(value, "scheduledEndTime");
		}
	}

	/**
	 * The upnp:scheduledStartTime property is used to indicate the start time
	 * of a scheduled program, intended for use by tuners.
	 *
	 * The format MUST be compliant to [ISO 8601] and SHOULD be compliant to
	 * [RFC 3339].
	 *
	 * Multi-Valued
	 */
	public static class ScheduledStartTime extends Property<String> implements NAMESPACE {

		public ScheduledStartTime() {
			this(null);
		}

		public ScheduledStartTime(String value) {
			super(value, "scheduledStartTime");
		}
	}

	/**
	 * The upnp:scheduledStopTime property is used to indicate the start time of
	 * a scheduled program, intended for use by tuners.
	 *
	 * The format MUST be compliant to [ISO 8601] and SHOULD be compliant to
	 * [RFC 3339].
	 *
	 * Multi-Valued
	 */
	public static class ScheduledStopTime extends Property<String> implements NAMESPACE {

		public ScheduledStopTime() {
			this(null);
		}

		public ScheduledStopTime(String value) {
			super(value, "scheduledStopTime");
		}
	}

	/**
	 * The read-only upnp:searchClass property is only applicable to container
	 * objects.
	 *
	 * It contains a class for which the container object can be searched.
	 *
	 * Multi-Valued
	 */
	public static class SearchClass extends Property<String> implements NAMESPACE {

		/**
		 * The read-only upnp:searchClass@name property indicates a friendly
		 * name for the class.
		 */
		public static class Name extends Property<String> implements NAMESPACE {

			public Name() {
			}

			public Name(String value) {
				super(value, "name");
			}
		}

		/**
		 * The read-only upnp:searchClass@includeDerived property is a required
		 * property of the associated upnp:searchClass property and indicates
		 * whether the class specified shall also include derived classes.
		 *
		 * When set to “1”, derived classes shall be included. When set to “0”,
		 * derived classes shall be excluded.
		 *
		 * The property is required when the upnp:searchClass property is
		 * present.
		 */
		public static class IncludeDerived extends Property<Boolean> implements NAMESPACE {

			public IncludeDerived() {
			}

			public IncludeDerived(Boolean value) {
				super(value, "includeDerived");
			}
		}

		public SearchClass() {
		}

		public SearchClass(String value, Boolean includeDerived) {
			super(value, "searchClass");
			setIncludeDerived(includeDerived);
		}

		public SearchClass(String value, String name, Boolean includeDerived) {
			super(value, "searchClass");
			setName(name);
			setIncludeDerived(includeDerived);
		}

		public String getName() {
			return dependentProperties.getValue(SearchClass.Name.class);
		}

		public final SearchClass setName(String value) {
			dependentProperties.set(new SearchClass.Name(value));
			return this;
		}

		public Boolean getIncludeDerived() {
			return dependentProperties.getValue(SearchClass.IncludeDerived.class);
		}

		public final SearchClass setIncludeDerived(Boolean value) {
			dependentProperties.set(new SearchClass.IncludeDerived(value));
			return this;
		}
	}

	/**
	 * The upnp:seriesTitle property contains the name of the series.
	 *
	 * This is most likely obtained from a database that contains
	 * program-related information, such as an Electronic Program Guide.
	 */
	public static class SeriesTitle extends Property<String> implements NAMESPACE {

		public SeriesTitle() {
		}

		public SeriesTitle(String value) {
			super(value, "seriesTitle");
		}
	}

	/**
	 * The read-only upnp:signalLocked property indicates whether the signal
	 * strength is sufficiently strong to enable the hardware to lock onto the
	 * signal at the current target frequency.
	 *
	 * When set to “1”, the signal strength is high enough for the hardware to
	 * lock onto it. When set to “0”, the signal strength is too low for the
	 * hardware to lock onto it. A change in the value of this property does not
	 * result in a change in the SystemUpdateID state variable or the
	 * corresponding ContainerUpdateIDs state variable. Therefore, a change to
	 * the this property does not constitute an object modification.
	 */
	public static class SignalLocked extends Property<Boolean> implements NAMESPACE {

		public SignalLocked() {
			this(null);
		}

		public SignalLocked(Boolean value) {
			super(value, "signalLocked");
		}
	}

	/**
	 * The read-only upnp:signalStrength property contains the relative strength
	 * of the signal that is used to retrieve the content for the item.
	 *
	 * A value of 0 indicates “no signal detected”. A value of 100 indicates
	 * “best possible” signal strength. A value of -1 indicates that the signal
	 * strength is currently unknown. Values less than -1 or greater than 100
	 * are reserved for future use and shall be treated as -1. A change in the
	 * value of this property does not result in a change in the SystemUpdateID
	 * state variable or the corresponding ContainerUpdateIDs state variable.
	 * Therefore, a change to the this property does not constitute an object
	 * modification.
	 */
	public static class SignalStrength extends Property<Integer> implements NAMESPACE {

		public SignalStrength() {
			this(null);
		}

		public SignalStrength(Integer value) {
			super(value, "signalStrength");
		}
	}

	/**
	 * The read-only upnp:storageMedium property indicates the type of storage
	 * medium used for the content.
	 *
	 * Potentially useful for user-interface purposes.
	 */
	public static class StorageMedium extends Property<String> implements NAMESPACE {

		public StorageMedium() {
			this(StorageMediumValue.UNKNOWN);
		}

		public StorageMedium(String value) {
			super(value, "storageMedium");
		}

		public StorageMedium(StorageMediumValue value) {
			super(value.toString(), "storageMedium");
		}
	}

	/**
	 * The read-only upnp:srsRecordScheduleID property contains the value of the
	 * srs:@id property of the srs:recordSchedule object that was used to create
	 * this recorded content.
	 */
	public static class SrsRecordScheduleID extends Property<String> implements NAMESPACE {

		public SrsRecordScheduleID() {
			this(StorageMediumValue.UNKNOWN);
		}

		public SrsRecordScheduleID(String value) {
			super(value, "srsRecordScheduleID");
		}

		public SrsRecordScheduleID(StorageMediumValue value) {
			super(value.toString(), "srsRecordScheduleID");
		}
	}

	/**
	 * The read/write upnp:stateVariableCollection property holds a
	 * stateVariableValuePairs XML Document which encapsulates the collected
	 * state variables and their values.
	 *
	 * Multi-Valued
	 */
	public static class StateVariableCollection extends Property<String> implements NAMESPACE {

		/**
		 * The read/write upnp:stateVariableCollection@serviceName property
		 * identifies from which service the state variables were retrieved.
		 *
		 * The upnp:stateVariableCollection@serviceName property is required if
		 * the upnp:stateVariableCollection property is specified.
		 */
		public static class ServiceName extends Property<String> implements NAMESPACE {

			public ServiceName() {
			}

			public ServiceName(String value) {
				super(value, "serviceName");
			}
		}

		/**
		 * The read/write upnp:stateVariableCollection@rcsInstanceType property
		 * specifies whether the RenderingControl service instance is pre-mix or
		 * post-mix.
		 *
		 * It shall be specified if the state variable collection originates
		 * from a RenderingControl service.
		 *
		 * Allowed values for upnp:stateVariableCollection@rcsInstanceType are
		 * “pre-mix” and “post-mix”
		 */
		public static class RcsInstanceType extends Property<String> implements NAMESPACE {

			public RcsInstanceType() {
			}

			public RcsInstanceType(String value) {
				super(value, "rcsInstanceType");
			}
		}

		public StateVariableCollection(String value, String serviceName) {
			super(value, "stateVariableCollection");
			setServiceName(serviceName);

		}

		public StateVariableCollection(String value, String serviceName, String rcsInstanceType) {
			this(value, serviceName);
			setRcsInstanceType(rcsInstanceType);
		}

		public String getServiceName() {
			return dependentProperties.getValue(StateVariableCollection.ServiceName.class);
		}

		public final StateVariableCollection setServiceName(String serviceName) {
			dependentProperties.set(new StateVariableCollection.ServiceName(serviceName));
			return this;
		}

		public String getRcsInstanceType() {
			return dependentProperties.getValue(StateVariableCollection.RcsInstanceType.class);
		}

		public final StateVariableCollection setRcsInstanceType(String rcsInstanceType) {
			dependentProperties.set(new StateVariableCollection.RcsInstanceType(rcsInstanceType));
			return this;
		}
	}

	/**
	 * The upnp:storageFree property contains the total free capacity, in bytes,
	 * of the storage represented by the container.
	 *
	 * Value -1 is reserved to indicate that the capacity is unknown.
	 */
	public static class StorageFree extends Property<Long> implements NAMESPACE {

		public StorageFree() {
			this(null);
		}

		public StorageFree(Long value) {
			super(value, "storageFree");
		}
	}

	/**
	 * The upnp:storageMaxPartition property contains the largest amount of
	 * space, in bytes, available for storing a single resource in the
	 * container.
	 *
	 * Value -1 is reserved to indicate that the amount of space is unknown.
	 */
	public static class StorageMaxPartition extends Property<Long> implements NAMESPACE {

		public StorageMaxPartition() {
			this(null);
		}

		public StorageMaxPartition(Long value) {
			super(value, "storageMaxPartition");
		}
	}

	/**
	 * The upnp:storageTotal property contains the total capacity, in bytes, of
	 * the storage represented by the container.
	 *
	 * Value -1 is reserved to indicate that the capacity is unknown.
	 */
	public static class StorageTotal extends Property<Long> implements NAMESPACE {

		public StorageTotal() {
			this(null);
		}

		public StorageTotal(Long value) {
			super(value, "storageTotal");
		}
	}

	/**
	 * The upnp:storageUsed property contains the combined space, in bytes, used
	 * by all the objects held in the storage represented by the container.
	 *
	 * Value -1 is reserved to indicate that the space is unknown.
	 */
	public static class StorageUsed extends Property<Long> implements NAMESPACE {

		public StorageUsed() {
			this(null);
		}

		public StorageUsed(Long value) {
			super(value, "storageUsed");
		}
	}

	/**
	 * The upnp:toc property contains the table of contents of the object.
	 */
	public static class Toc extends Property<String> implements NAMESPACE {

		public Toc() {
		}

		public Toc(String value) {
			super(value, "toc");
		}
	}

	/**
	 * The read-only upnp:totalDeletedChildCount property is an allowed property
	 * that contains the total number of child objects that have been deleted
	 * from a container object since the last initialization.
	 *
	 * When a container object is first created, the value of it
	 * upnp:totalDeletedChildCount property shall be initialized to zero (“0”).
	 * Every time an object is deleted, the upnp:totalDeletedChildCount property
	 * of the object’s parent container shall be incremented by one (“1”).
	 *
	 * If implemented, the current value the upnp:totalDeletedChildCount
	 * property shall be persisted even while off-line.
	 */
	public static class TotalDeletedChildCount extends Property<UnsignedInteger> implements NAMESPACE {

		public TotalDeletedChildCount() {
			this(null);
		}

		public TotalDeletedChildCount(UnsignedInteger value) {
			super(value, "totalDeletedChildCount");
		}
	}

	/**
	 * The read-only upnp:tuned property indicates whether a hardware resource
	 * is currently tuned to retrieve the content represented by this item.
	 *
	 * When set to “1”, there is a hardware resource currently tuned to this
	 * item. When set to “0”, there is no hardware resource currently tuned.
	 */
	public static class Tuned extends Property<Boolean> implements NAMESPACE {

		public Tuned() {
			this(null);
		}

		public Tuned(Boolean value) {
			super(value, "tuned");
		}
	}

	/**
	 * The upnp:userAnnotation property is a general-purpose property where a
	 * user can annotate an object with some user-specific information.
	 */
	public static class UserAnnotation extends Property<String> implements NAMESPACE {

		public UserAnnotation() {
			this(null);
		}

		public UserAnnotation(String value) {
			super(value, "userAnnotation");
		}
	}

	/**
	 * The upnp:writeStatus property controls the modifiability of the resources
	 * of a given object.
	 *
	 * The ability for a control point to change the value of the
	 * upnp:writeStatus property is implementation dependent.
	 *
	 * Default Value: “UNKNOWN”.
	 */
	public static class WriteStatus extends Property<String> implements NAMESPACE {

		public WriteStatus() {
			this(WriteStatusValue.UNKNOWN);
		}

		public WriteStatus(WriteStatusValue value) {
			super(value.toString(), "writeStatus");
		}

	}

}
