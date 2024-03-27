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
package net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite;

import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Property;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.XmlNamespace;

@SuppressWarnings({ "checkstyle:TypeName" })
public class DIDL_LITE {

	public static final String NAMESPACE_URI = "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/";

	@SuppressWarnings({ "checkstyle:InterfaceIsType" })
	public interface NAMESPACE extends XmlNamespace {
	}

	/**
	 * The allowed read-only @childContainerCount property is only applicable to
	 * container objects.
	 *
	 * It reflects the number of direct-children container objects contained in
	 * the container object. This property can be used to derive the number of
	 * direct-children item objects contained in the container object by
	 * subtracting the value of this property from the value of the @childCount
	 * property, if supported, of the container object.
	 *
	 * unsignedInt
	 */
	public static class ChildContainerCount extends Property<Long> implements NAMESPACE {

		public ChildContainerCount() {
			this(null);
		}

		public ChildContainerCount(Long value) {
			super(value, "childContainerCount");
		}
	}

	/**
	 * The read-only @childCount property is only applicable to container
	 * objects.
	 *
	 * It reflects the number of direct children contained in the container
	 * object.
	 *
	 * unsignedInt
	 */
	public static class ChildCount extends Property<Long> implements NAMESPACE {

		public ChildCount() {
			this(null);
		}

		public ChildCount(Long value) {
			super(value, "childCount");
		}
	}

	/**
	 * The read-only @id property is a required property that shall provide a
	 * unique identity for the object with respect to all of the objects within
	 * the ContentDirectory service.
	 *
	 * For all objects that support tracking of changes (i.e those that expose
	 * the upnp:objectUpdateID or upnp:containerUpdateID properties), as long as
	 * the ServiceResetToken remains constant, the ContentDirectory service
	 * shall ensure the persistence of these object’s @id property values. If
	 * the ContentDirectory service cannot ensure the persistence of these
	 * object’s @id property values, then it shall invoke the Service Reset
	 * Procedure.
	 *
	 * For all objects, regardless of whether they support tracking of changes
	 * or not, as long as the ServiceResetToken remains constant, the
	 * ContentDirectory service shall ensure the object ID’s uniqueness; that
	 * is: if an object is created with the same @id property as a previously
	 * deleted object, the service is making the claim that these two objects
	 * are the same. If the ContentDirectory service cannot ensure the
	 * uniqueness of an object’s @id property value, then it shall invoke the
	 * Service Reset procedure.
	 *
	 * For all objects that do not support tracking of changes, as long as the
	 * ServiceResetToken remains constant, the ContentDirectory service is
	 * recommended to ensure the persistence of these object’s @id property
	 * values. If the ContentDirectory service cannot ensure the persistence of
	 * these object’s @id property values, then it should invoke the Service
	 * Reset Procedure.
	 */
	public static class Id extends Property<String> implements NAMESPACE {

		public Id() {
			this(null);
		}

		public Id(String value) {
			super(value, "id");
		}
	}

	/**
	 * The read-only @parentID property is a required property of an item or
	 * container object.
	 *
	 * The @parentID property shall be set and always remain equal to the @id
	 * property of the object’s parent, which shall be a container. The
	 *
	 * @parentID property of the ContentDirectory service root container shall
	 * be set to the reserved value of -1. The @parentID property of any other
	 * ContentDirectory service object shall not take this value.
	 */
	public static class ParentID extends Property<String> implements NAMESPACE {

		public ParentID() {
			this(null);
		}

		public ParentID(String value) {
			super(value, "parentID");
		}
	}

	/**
	 * The read-only @refID property is only applicable to item objects.
	 *
	 * The presence of this property indicates that the item is actually
	 * referencing another existing item (reference item). The @refID property
	 * shall be set and always remain equal to the @id property of the item that
	 * is referenced.
	 */
	public static class RefID extends Property<String> implements NAMESPACE {

		public RefID() {
			this(null);
		}

		public RefID(String value) {
			super(value, "refID");
		}
	}

	/**
	 * The read-only required @restricted property indicates whether the object
	 * is modifiable.
	 *
	 * If set to “1”, the ability to modify or delete a given object is confined
	 * to the ContentDirectory service implementation. Therefore, a control
	 * point cannot add, modify or delete metadata from a restricted object.
	 * Additionally, control points are not able to add, modify or delete any
	 * children of a restricted container. However, the @restricted property
	 * does not propagate to descendant objects. Note however, that metadata of
	 * a restricted object can still change due to internal ContentDirectory
	 * service implementation manipulations.
	 *
	 * If set to “0”, a control point can modify the object’s metadata and add,
	 * delete, or modify the object’s children.
	 */
	public static class Restricted extends Property<Boolean> implements NAMESPACE {

		public Restricted() {
			this(null);
		}

		public Restricted(Boolean value) {
			super(value, "restricted");
		}
	}

	/**
	 * The read-only @searchable property is only applicable to container
	 * objects.
	 *
	 * When “1” (true), the ability to perform a Search() action under a
	 * container is enabled, otherwise a Search() action under that container
	 * will return no results, even when child containers have their @searchable
	 * property set to “1”.
	 *
	 * Default Value: “0”.
	 */
	public static class Searchable extends Property<Boolean> implements NAMESPACE {

		public Searchable() {
			this(null);
		}

		public Searchable(Boolean value) {
			super(value, "searchable");
		}
	}

	/**
	 * The @neverPlayable property indicates whether an item or container will
	 * ever have normal playable content.
	 *
	 * A value of “1” indicates that the associated item or container will never
	 * have normal playable content. Furthermore, for a container, the complete
	 * subtree underneath the container will also never have normal playable
	 * content.
	 *
	 * A value of “0” indicates that the item or subtree may contain playable
	 * content.
	 *
	 * The value of this property shall be static.
	 *
	 * Default Value: “0”.
	 */
	public static class NeverPlayable extends Property<Boolean> implements NAMESPACE {

		public NeverPlayable() {
			this(null);
		}

		public NeverPlayable(Boolean value) {
			super(value, "neverPlayable");
		}
	}

	public static final String OBJECT_TYPE = "object";
	public static final String OBJECT_CONTAINER_TYPE = OBJECT_TYPE + ".container";
	public static final String OBJECT_CONTAINER_ALBUM_TYPE = OBJECT_CONTAINER_TYPE + ".album";
	public static final String OBJECT_CONTAINER_ALBUM_MUSICALBUM_TYPE = OBJECT_CONTAINER_ALBUM_TYPE + ".musicAlbum";
	public static final String OBJECT_CONTAINER_ALBUM_PHOTOALBUM_TYPE = OBJECT_CONTAINER_ALBUM_TYPE + ".photoAlbum";
	public static final String OBJECT_CONTAINER_GENRE_TYPE = OBJECT_CONTAINER_TYPE + ".genre";
	public static final String OBJECT_CONTAINER_GENRE_MOVIEGENRE_TYPE = OBJECT_CONTAINER_GENRE_TYPE + ".movieGenre";
	public static final String OBJECT_CONTAINER_GENRE_MUSICGENRE_TYPE = OBJECT_CONTAINER_GENRE_TYPE + ".musicGenre";
	public static final String OBJECT_CONTAINER_PERSON_TYPE = OBJECT_CONTAINER_TYPE + ".person";
	public static final String OBJECT_CONTAINER_PERSON_MUSICARTIST_TYPE = OBJECT_CONTAINER_PERSON_TYPE + ".musicArtist";
	public static final String OBJECT_CONTAINER_PLAYLISTCONTAINER_TYPE = OBJECT_CONTAINER_TYPE + ".playlistContainer";
	public static final String OBJECT_CONTAINER_STORAGEFOLDER_TYPE = OBJECT_CONTAINER_TYPE + ".storageFolder";
	public static final String OBJECT_CONTAINER_STORAGESYSTEM_TYPE = OBJECT_CONTAINER_TYPE + ".storageSystem";
	public static final String OBJECT_CONTAINER_STORAGEVOLUME_TYPE = OBJECT_CONTAINER_TYPE + ".storageVolume";
	public static final String OBJECT_ITEM_TYPE = OBJECT_TYPE + ".item";
	public static final String OBJECT_ITEM_AUDIOITEM_TYPE = OBJECT_ITEM_TYPE + ".audioItem";
	public static final String OBJECT_ITEM_AUDIOITEM_AUDIOBOOK_TYPE = OBJECT_ITEM_AUDIOITEM_TYPE + ".audioBook";
	public static final String OBJECT_ITEM_AUDIOITEM_AUDIOBROADCAST_TYPE = OBJECT_ITEM_AUDIOITEM_TYPE + ".audioBroadcast";
	public static final String OBJECT_ITEM_AUDIOITEM_MUSICTRACK_TYPE = OBJECT_ITEM_AUDIOITEM_TYPE + ".musicTrack";
	public static final String OBJECT_ITEM_BOOKMARKITEM_TYPE = OBJECT_ITEM_TYPE + ".bookmarkItem";
	public static final String OBJECT_ITEM_IMAGEITEM_TYPE = OBJECT_ITEM_TYPE + ".imageItem";
	public static final String OBJECT_ITEM_IMAGEITEM_PHOTO_TYPE = OBJECT_ITEM_IMAGEITEM_TYPE + ".photo";
	public static final String OBJECT_ITEM_PLAYLISTITEM_TYPE = OBJECT_ITEM_TYPE + ".playlistItem";
	public static final String OBJECT_ITEM_TEXTITEM_TYPE = OBJECT_ITEM_TYPE + ".textItem";
	public static final String OBJECT_ITEM_VIDEOITEM_TYPE = OBJECT_ITEM_TYPE + ".videoItem";
	public static final String OBJECT_ITEM_VIDEOITEM_MOVIE_TYPE = OBJECT_ITEM_VIDEOITEM_TYPE + ".movie";
	public static final String OBJECT_ITEM_VIDEOITEM_MUSICVIDEOCLIP_TYPE = OBJECT_ITEM_VIDEOITEM_TYPE + ".musicVideoClip";
	public static final String OBJECT_ITEM_VIDEOITEM_VIDEOBROADCAST_TYPE = OBJECT_ITEM_VIDEOITEM_TYPE + ".videoBroadcast";
}
