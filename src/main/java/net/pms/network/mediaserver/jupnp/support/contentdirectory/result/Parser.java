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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.dc.DC;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.BaseObject;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.DIDL_LITE;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Desc;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.Res;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Album;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Container;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.GenreContainer;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MovieGenre;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MusicAlbum;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MusicArtist;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.MusicGenre;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.Person;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.PhotoAlbum;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.PlaylistContainer;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.StorageFolder;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.StorageSystem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.container.StorageVolume;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.AudioBook;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.AudioBroadcast;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.AudioItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.BookmarkItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.ImageItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Movie;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.MusicTrack;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.MusicVideoClip;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Photo;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.PlaylistItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.TextItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.VideoBroadcast;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.VideoItem;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.UPNP;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.upnp.WriteStatusValue;
import org.jupnp.model.types.Datatype;
import org.jupnp.model.types.InvalidValueException;
import org.jupnp.support.model.ProtocolInfo;
import org.jupnp.util.io.IO;
import org.jupnp.xml.SAXParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Parser extends SAXParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(Parser.class);

	/**
	 * Uses the current thread's context classloader to read and unmarshall the
	 * given resource.
	 *
	 * @param resource The resource on the classpath.
	 * @return The unmarshalled DIDL content model.
	 * @throws Exception
	 */
	public Result parseResource(String resource) throws Exception {
		try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
			return parse(IO.readLines(is));
		}
	}

	/**
	 * Reads and unmarshalls an XML representation into a DIDL content model.
	 *
	 * @param xml The XML representation.
	 * @return A DIDL content model.
	 * @throws Exception
	 */
	public Result parse(String xml) throws Exception {

		if (xml == null || xml.length() == 0) {
			throw new RuntimeException("Null or empty XML");
		}

		Result content = new Result();
		createRootHandler(content, this);

		LOGGER.trace("Parsing DIDL XML content");
		parse(new InputSource(new StringReader(xml)));
		return content;
	}

	protected ResultHandler createRootHandler(Result instance, SAXParser parser) {
		return new ResultHandler(instance, parser);
	}

	protected ContainerHandler createContainerHandler(Container instance, SAXParser.Handler<?> parent) {
		return new ContainerHandler(instance, parent);
	}

	protected ItemHandler createItemHandler(Item instance, SAXParser.Handler<?> parent) {
		return new ItemHandler(instance, parent);
	}

	protected ResHandler createResHandler(Res instance, SAXParser.Handler<?> parent) {
		return new ResHandler(instance, parent);
	}

	protected DescHandler createDescHandler(Desc instance, SAXParser.Handler<?> parent) {
		return new DescHandler(instance, parent);
	}

	protected Container createContainer(Attributes attributes) {
		Container container = new Container();

		container.setId(attributes.getValue("id"));
		container.setParentID(attributes.getValue("parentID"));

		if ((attributes.getValue("childCount") != null)) {
			container.setChildCount(Long.valueOf(attributes.getValue("childCount")));
		}

		try {
			Boolean value = (Boolean) Datatype.Builtin.BOOLEAN.getDatatype().valueOf(
					attributes.getValue("restricted")
			);
			if (value != null) {
				container.setRestricted(value);
			}

			value = (Boolean) Datatype.Builtin.BOOLEAN.getDatatype().valueOf(
					attributes.getValue("searchable")
			);
			if (value != null) {
				container.setSearchable(value);
			}
		} catch (InvalidValueException ex) {
			// Ignore
		}

		return container;
	}

	protected Item createItem(Attributes attributes) {
		Item item = new Item();
		item.setId(attributes.getValue("id"));
		item.setParentID(attributes.getValue("parentID"));

		try {
			Boolean value = (Boolean) Datatype.Builtin.BOOLEAN.getDatatype().valueOf(
					attributes.getValue("restricted")
			);
			if (value != null) {
				item.setRestricted(value);
			}

		} catch (InvalidValueException ex) {
			// Ignore
		}

		if ((attributes.getValue("refID") != null)) {
			item.setRefID(attributes.getValue("refID"));
		}

		return item;
	}

	protected Res createResource(Attributes attributes) {
		Res res = new Res();

		if (attributes.getValue("importUri") != null) {
			res.setImportUri(URI.create(attributes.getValue("importUri")));
		}

		try {
			res.setProtocolInfo(
					new ProtocolInfo(attributes.getValue("protocolInfo"))
			);
		} catch (InvalidValueException ex) {
			LOGGER.warn("In DIDL content, invalid resource protocol info: " + ex.getMessage());
			return null;
		}

		if (attributes.getValue("size") != null) {
			res.setSize(UnsignedLong.valueOf(attributes.getValue("size")));
		}

		if (attributes.getValue("duration") != null) {
			res.setDuration(attributes.getValue("duration"));
		}

		if (attributes.getValue("bitrate") != null) {
			res.setBitrate(UnsignedInteger.valueOf(attributes.getValue("bitrate")));
		}

		if (attributes.getValue("sampleFrequency") != null) {
			res.setSampleFrequency(UnsignedInteger.valueOf(attributes.getValue("sampleFrequency")));
		}

		if (attributes.getValue("bitsPerSample") != null) {
			res.setBitsPerSample(UnsignedInteger.valueOf(attributes.getValue("bitsPerSample")));
		}

		if (attributes.getValue("nrAudioChannels") != null) {
			res.setNrAudioChannels(UnsignedInteger.valueOf(attributes.getValue("nrAudioChannels")));
		}

		if (attributes.getValue("colorDepth") != null) {
			res.setColorDepth(UnsignedInteger.valueOf(attributes.getValue("colorDepth")));
		}

		if (attributes.getValue("protection") != null) {
			res.setProtection(attributes.getValue("protection"));
		}

		if (attributes.getValue("resolution") != null) {
			res.setResolution(attributes.getValue("resolution"));
		}

		return res;
	}

	protected Desc createDescription(Attributes attributes) {
		Desc desc = new Desc();

		desc.setId(attributes.getValue("id"));

		if ((attributes.getValue("type") != null)) {
			desc.setType(attributes.getValue("type"));
		}

		if ((attributes.getValue("nameSpace") != null)) {
			desc.setNameSpace(attributes.getValue("nameSpace"));
		}

		return desc;
	}

	protected Item enhanceItem(Item item) {
		String genericType = item.getUpnpClassName();

		if (null == genericType) {
			return item;
		} else {
			return switch (genericType) {
				case DIDL_LITE.OBJECT_ITEM_TYPE ->
					item;
				case DIDL_LITE.OBJECT_ITEM_AUDIOITEM_TYPE ->
					(AudioItem) item;
				case DIDL_LITE.OBJECT_ITEM_AUDIOITEM_MUSICTRACK_TYPE ->
					(MusicTrack) item;
				case DIDL_LITE.OBJECT_ITEM_AUDIOITEM_AUDIOBOOK_TYPE ->
					(AudioBook) item;
				case DIDL_LITE.OBJECT_ITEM_AUDIOITEM_AUDIOBROADCAST_TYPE ->
					(AudioBroadcast) item;
				case DIDL_LITE.OBJECT_ITEM_BOOKMARKITEM_TYPE ->
					(BookmarkItem) item;
				case DIDL_LITE.OBJECT_ITEM_IMAGEITEM_TYPE ->
					(ImageItem) item;
				case DIDL_LITE.OBJECT_ITEM_IMAGEITEM_PHOTO_TYPE ->
					(Photo) item;
				case DIDL_LITE.OBJECT_ITEM_PLAYLISTITEM_TYPE ->
					(PlaylistItem) item;
				case DIDL_LITE.OBJECT_ITEM_TEXTITEM_TYPE ->
					(TextItem) item;
				case DIDL_LITE.OBJECT_ITEM_VIDEOITEM_TYPE ->
					(VideoItem) item;
				case DIDL_LITE.OBJECT_ITEM_VIDEOITEM_MOVIE_TYPE ->
					(Movie) item;
				case DIDL_LITE.OBJECT_ITEM_VIDEOITEM_MUSICVIDEOCLIP_TYPE ->
					(MusicVideoClip) item;
				case DIDL_LITE.OBJECT_ITEM_VIDEOITEM_VIDEOBROADCAST_TYPE ->
					(VideoBroadcast) item;
				default ->
					item;
			};
		}
	}

	protected Container enhanceContainer(Container container) {
		String genericType = container.getUpnpClassName();
		if (genericType == null) {
			return container;
		} else {
			return switch (genericType) {
				case DIDL_LITE.OBJECT_CONTAINER_TYPE ->
					container;
				case DIDL_LITE.OBJECT_CONTAINER_ALBUM_TYPE ->
					(Album) container;
				case DIDL_LITE.OBJECT_CONTAINER_ALBUM_MUSICALBUM_TYPE ->
					(MusicAlbum) container;
				case DIDL_LITE.OBJECT_CONTAINER_ALBUM_PHOTOALBUM_TYPE ->
					(PhotoAlbum) container;
				case DIDL_LITE.OBJECT_CONTAINER_GENRE_TYPE ->
					(GenreContainer) container;
				case DIDL_LITE.OBJECT_CONTAINER_GENRE_MOVIEGENRE_TYPE ->
					(MovieGenre) container;
				case DIDL_LITE.OBJECT_CONTAINER_GENRE_MUSICGENRE_TYPE ->
					(MusicGenre) container;
				case DIDL_LITE.OBJECT_CONTAINER_PERSON_TYPE ->
					(Person) container;
				case DIDL_LITE.OBJECT_CONTAINER_PERSON_MUSICARTIST_TYPE ->
					(MusicArtist) container;
				case DIDL_LITE.OBJECT_CONTAINER_PLAYLISTCONTAINER_TYPE ->
					(PlaylistContainer) container;
				case DIDL_LITE.OBJECT_CONTAINER_STORAGEFOLDER_TYPE ->
					(StorageFolder) container;
				case DIDL_LITE.OBJECT_CONTAINER_STORAGESYSTEM_TYPE ->
					(StorageSystem) container;
				case DIDL_LITE.OBJECT_CONTAINER_STORAGEVOLUME_TYPE ->
					(StorageVolume) container;
				default ->
					container;
			};
		}
	}

	/* ############################################################################################# */
	public abstract class DIDLObjectHandler<I extends BaseObject> extends SAXParser.Handler<I> {

		protected DIDLObjectHandler(I instance, SAXParser.Handler<?> parent) {
			super(instance, parent);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			if (localName == null) {
				return;
			}
			if (DC.NAMESPACE_URI.equals(uri)) {
				switch (localName) {
					case "title" ->
						getInstance().setTitle(getCharacters());
					case "creator" ->
						getInstance().setCreator(getCharacters());
					case "description" ->
						getInstance().getProperties().add(new DC.Description(getCharacters()));
					case "publisher" ->
						getInstance().getProperties().add(new DC.Publisher(getCharacters()));
					case "contributor" ->
						getInstance().getProperties().add(new DC.Contributor(getCharacters()));
					case "date" ->
						getInstance().getProperties().add(new DC.Date(getCharacters()));
					case "language" ->
						getInstance().getProperties().add(new DC.Language(getCharacters()));
					case "rights" ->
						getInstance().getProperties().add(new DC.Rights(getCharacters()));
					case "relation" ->
						getInstance().getProperties().add(new DC.Relation(URI.create(getCharacters())));
					default -> {
						//nothing to do
					}
				}
			} else if (UPNP.NAMESPACE_URI.equals(uri)) {
				switch (localName) {
					case "writeStatus" -> {
						try {
							getInstance().setWriteStatus(
									WriteStatusValue.valueOf(getCharacters())
							);
						} catch (Exception ex) {
							LOGGER.info("Ignoring invalid writeStatus value: " + getCharacters());
						}
					}
					case "class" ->
						getInstance().setUpnpClass(
								new UPNP.Class(
										getCharacters(),
										getAttributes().getValue("name")
								)
						);
					case "artist" ->
						getInstance().getProperties().add(
								new UPNP.Artist(
										getCharacters(), getAttributes().getValue("role")
								)
						);
					case "actor" ->
						getInstance().getProperties().add(
								new UPNP.Actor(
										getCharacters(), getAttributes().getValue("role")
								)
						);
					case "author" ->
						getInstance().getProperties().add(
								new UPNP.Author(
										getCharacters(), getAttributes().getValue("role")
								)
						);
					case "producer" ->
						getInstance().getProperties().add(
								new UPNP.Producer(getCharacters())
						);
					case "director" ->
						getInstance().getProperties().add(
								new UPNP.Director(getCharacters())
						);
					case "longDescription" -> {
						getInstance().getProperties().add(
								new UPNP.LongDescription(getCharacters())
						);
					}
					case "storageUsed" ->
						getInstance().getProperties().add(
								new UPNP.StorageUsed(Long.valueOf(getCharacters()))
						);
					case "storageTotal" ->
						getInstance().getProperties().add(
								new UPNP.StorageTotal(Long.valueOf(getCharacters()))
						);
					case "storageFree" ->
						getInstance().getProperties().add(
								new UPNP.StorageFree(Long.valueOf(getCharacters()))
						);
					case "storageMaxPartition" ->
						getInstance().getProperties().add(
								new UPNP.StorageMaxPartition(Long.valueOf(getCharacters()))
						);
					case "storageMedium" ->
						getInstance().getProperties().add(
								new UPNP.StorageMedium(getCharacters())
						);
					case "genre" ->
						getInstance().getProperties().add(
								new UPNP.Genre(getCharacters())
						);
					case "album" ->
						getInstance().getProperties().add(
								new UPNP.Album(getCharacters())
						);
					case "playlist" ->
						getInstance().getProperties().add(
								new UPNP.Playlist(getCharacters())
						);
					case "region" ->
						getInstance().getProperties().add(
								new UPNP.Region(getCharacters())
						);
					case "rating" ->
						getInstance().getProperties().add(
								new UPNP.Rating(getCharacters())
						);
					case "toc" ->
						getInstance().getProperties().add(
								new UPNP.Toc(getCharacters())
						);
					case "albumArtURI" -> {
						UPNP.AlbumArtURI albumArtURI = new UPNP.AlbumArtURI(URI.create(getCharacters()));
						Attributes albumArtURIAttributes = getAttributes();
						for (int i = 0; i < albumArtURIAttributes.getLength(); i++) {
							if ("profileID".equals(albumArtURIAttributes.getLocalName(i))) {
								albumArtURI.setProfileID(albumArtURIAttributes.getValue(i));
							}
						}
						getInstance().getProperties().add(albumArtURI);
					}
					case "artistDiscographyURI" ->
						getInstance().getProperties().add(
								new UPNP.ArtistDiscographyURI(URI.create(getCharacters()))
						);
					case "lyricsURI" ->
						getInstance().getProperties().add(
								new UPNP.LyricsURI(URI.create(getCharacters()))
						);
					case "icon" ->
						getInstance().getProperties().add(
								new UPNP.Icon(URI.create(getCharacters()))
						);
					case "radioCallSign" ->
						getInstance().getProperties().add(
								new UPNP.RadioCallSign(getCharacters())
						);
					case "radioStationID" ->
						getInstance().getProperties().add(
								new UPNP.RadioStationID(getCharacters())
						);
					case "radioBand" ->
						getInstance().getProperties().add(
								new UPNP.RadioBand(getCharacters())
						);
					case "channelNr" ->
						getInstance().getProperties().add(
								new UPNP.ChannelNr(Integer.valueOf(getCharacters()))
						);
					case "channelName" ->
						getInstance().getProperties().add(
								new UPNP.ChannelName(getCharacters())
						);
					case "scheduledStartTime" ->
						getInstance().getProperties().add(
								new UPNP.ScheduledStartTime(getCharacters())
						);
					case "scheduledEndTime" ->
						getInstance().getProperties().add(
								new UPNP.ScheduledEndTime(getCharacters())
						);
					case "DVDRegionCode" ->
						getInstance().getProperties().add(
								new UPNP.DVDRegionCode(Integer.valueOf(getCharacters()))
						);
					case "originalTrackNumber" ->
						getInstance().getProperties().add(
								new UPNP.OriginalTrackNumber(Integer.valueOf(getCharacters()))
						);
					case "userAnnotation" ->
						getInstance().getProperties().add(
								new UPNP.UserAnnotation(getCharacters())
						);
					default -> {
						//nothing to do
						LOGGER.warn("In DIDL content, missing 'upnp:{}' element with value: {}", localName, getCharacters());
					}
				}
			}
		}
	}

	public class ResultHandler extends SAXParser.Handler<Result> {

		ResultHandler(Result instance, SAXParser parser) {
			super(instance, parser);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);

			if (!Result.NAMESPACE_URI.equals(uri)) {
				return;
			}

			switch (localName) {
				case "container" -> {
					Container container = createContainer(attributes);
					getInstance().addContainer(container);
					createContainerHandler(container, this);
				}
				case "item" -> {
					Item item = createItem(attributes);
					getInstance().addItem(item);
					createItemHandler(item, this);
				}
				default -> {
					//nothing to do
				}
			}
		}

		@Override
		protected boolean isLastElement(String uri, String localName, String qName) {
			return Result.NAMESPACE_URI.equals(uri) && "DIDL-Lite".equals(localName);
		}
	}

	public class ContainerHandler extends DIDLObjectHandler<Container> {

		public ContainerHandler(Container instance, SAXParser.Handler<?> parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);

			if (!Result.NAMESPACE_URI.equals(uri)) {
				return;
			}

			switch (localName) {
				case "desc" -> {
					Desc desc = createDescription(attributes);
					if (desc != null) {
						getInstance().addDescription(desc);
						createDescHandler(desc, this);
					}
				}
				case "res" -> {
					Res res = createResource(attributes);
					if (res != null) {
						getInstance().addResource(res);
						createResHandler(res, this);
					}
				}
				default -> {
					// We do NOT support recursive container embedded in container! The schema allows it
					// but the spec doesn't:
					//
					// Section 2.8.3: Incremental navigation i.e. the full hierarchy is never returned
					// in one call since this is likely to flood the resources available to the control
					// point (memory, network bandwidth, etc.).
				}
			}
		}

		@Override
		protected boolean isLastElement(String uri, String localName, String qName) {
			if (Result.NAMESPACE_URI.equals(uri) && "container".equals(localName)) {
				if (getInstance().getTitle() == null) {
					LOGGER.warn("In DIDL content, missing 'dc:title' element for container: " + getInstance().getId());
				}
				if (getInstance().getUpnpClassName() == null) {
					LOGGER.warn("In DIDL content, missing 'upnp:class' element for container: " + getInstance().getId());
				}
				return true;
			}
			return false;
		}
	}

	public class ItemHandler extends DIDLObjectHandler<Item> {

		public ItemHandler(Item instance, SAXParser.Handler<?> parent) {
			super(instance, parent);
		}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);

			if (!Result.NAMESPACE_URI.equals(uri)) {
				return;
			}

			if (localName.equals("res")) {

				Res res = createResource(attributes);
				if (res != null) {
					getInstance().addResource(res);
					createResHandler(res, this);
				}

			} else if (localName.equals("desc")) {

				Desc desc = createDescription(attributes);
				if (desc != null) {
					getInstance().addDescription(desc);
					createDescHandler(desc, this);
				}
			}
		}

		@Override
		protected boolean isLastElement(String uri, String localName, String qName) {
			if (Result.NAMESPACE_URI.equals(uri) && "item".equals(localName)) {
				if (getInstance().getTitle() == null) {
					LOGGER.warn("In DIDL content, missing 'dc:title' element for item: " + getInstance().getId());
				}
				if (getInstance().getUpnpClassName() == null) {
					LOGGER.warn("In DIDL content, missing 'upnp:class' element for item: " + getInstance().getId());
				}
				return true;
			}
			return false;
		}
	}

	protected class ResHandler extends SAXParser.Handler<Res> {

		public ResHandler(Res instance, SAXParser.Handler<?> parent) {
			super(instance, parent);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
		}

		@Override
		protected boolean isLastElement(String uri, String localName, String qName) {
			return Result.NAMESPACE_URI.equals(uri) && "res".equals(localName);
		}
	}

	/**
	 * Extracts an <code>org.w3c.Document</code> from the nested elements in the
	 * {@code <desc>} element.
	 * <p>
	 * The root element of this document is a wrapper in the namespace
	 * {@link org.jupnp.support.model.DIDLContent#DESC_WRAPPER_NAMESPACE_URI}.
	 * </p>
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public class DescHandler extends SAXParser.Handler<Desc> {

		public DescHandler(Desc instance, SAXParser.Handler<?> parent) {
			super(instance, parent);
		}

		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
		}

		@Override
		protected boolean isLastElement(String uri, String localName, String qName) {
			return Result.NAMESPACE_URI.equals(uri) && "desc".equals(localName);
		}

	}

}
