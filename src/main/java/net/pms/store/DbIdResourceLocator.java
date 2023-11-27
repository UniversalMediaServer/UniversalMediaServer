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
package net.pms.store;

import java.io.File;
import java.util.List;
import java.util.Optional;
import net.pms.database.MediaTableMusicBrainzReleases;
import net.pms.media.audio.metadata.MusicBrainzAlbum;
import net.pms.renderers.Renderer;
import net.pms.store.container.MediaLibraryFolder;
import net.pms.store.container.MusicBrainzAlbumFolder;
import net.pms.store.container.MusicBrainzPersonFolder;
import net.pms.store.container.VirtualFolderDbId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class resolves DLNA objects identified by databaseID's.
 */
public class DbIdResourceLocator {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdResourceLocator.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private DbIdResourceLocator() {
	}

	/**
	 * Adds a person folder to the library
	 *
	 * @param renderer
	 * @param typeIdent
	 * @return
	 */
	private static MusicBrainzPersonFolder addLibraryResourcePerson(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		MusicBrainzPersonFolder personFolder = new MusicBrainzPersonFolder(renderer, typeIdent.ident, typeIdent);

		DbIdTypeAndIdent tiAllFilesFolder = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALL_FILES, typeIdent.ident);
		DbIdTypeAndIdent tiAlbumFolder = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, typeIdent.ident);

		VirtualFolderDbId allFiles = new VirtualFolderDbId(renderer, "All songs by " + typeIdent.ident, tiAllFilesFolder);
		VirtualFolderDbId byAlbum = new VirtualFolderDbId(renderer, "Albums performed by  " + typeIdent.ident, tiAlbumFolder);

		renderer.getMediaStore().getDbIdLibrary().getPersonFolder().addChild(personFolder);

		personFolder.addChild(allFiles);
		personFolder.addChild(byAlbum);
		return personFolder;
	}

	/**
	 * Add a musicBrainz folder to the library.
	 *
	 * @param renderer
	 * @param typeIdent
	 * @param album
	 * @return
	 */
	public static MusicBrainzAlbumFolder addLibraryResourceMusicBrainzAlbum(Renderer renderer, MusicBrainzAlbum album) {
		try {
			DbIdTypeAndIdent typeIdent = new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, album.getMbReleaseid());
			MusicBrainzAlbumFolder mbFolder = getLibraryResourceMusicBrainzFolder(renderer, typeIdent);
			if (mbFolder == null) {
				LOGGER.debug("musicBrainz album not in database : {} ", typeIdent.toString());
				MusicBrainzAlbum persistentAlbum = MediaTableMusicBrainzReleases.getMusicBrainzAlbum(album.getMbReleaseid());
				if (persistentAlbum == null) {
					MediaTableMusicBrainzReleases.storeMusicBrainzAlbum(album);
				}
				mbFolder = new MusicBrainzAlbumFolder(renderer, album);

				// Lookup person's album folder as parent
				DbIdTypeAndIdent tiPersonAlbum = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, album.getArtist());
				StoreContainer personAlbum = (StoreContainer) getLibraryResourceByDbTypeIdent(renderer, tiPersonAlbum);
				if (personAlbum == null) {
					DbIdTypeAndIdent tiPerson = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, album.getArtist());
					MusicBrainzPersonFolder person = getLibraryResourcePersonFolder(renderer, tiPerson);
					if (person == null) {
						person = addLibraryResourcePerson(renderer, new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, album.getArtist()));
					}
					person.getAlbumFolder().addChild(mbFolder);
				} else {
					personAlbum.addChild(mbFolder);
				}
			}
			return mbFolder;
		} catch (Exception e) {
			LOGGER.error("cannot add MusicBrainzAlbumFolder.", e);
			return null;
		}
	}

	/**
	 * discover regular albums from media library
	 * @param renderer
	 * @param realFileName
	 * @return
	 */
	public static StoreResource getAlbumFromMediaLibrary(Renderer renderer, String realFileName) {
		// Regular albums can be discovered in the media library
		MediaLibraryFolder album = renderer.getMediaStore().getMediaLibrary().getAlbumFolder();
		if (!album.isDiscovered()) {
			album.discoverChildren();
		}
		Optional<StoreResource> optional = album.getChildren().stream().filter(
			sr -> realFileName.equals(sr.getDisplayName())).findFirst();
		if (optional.isPresent()) {
			StoreResource sr = optional.get();
			return sr;
		} else {
			LOGGER.error("album cannot be located in media library : " + realFileName);
		}
		return null;
	}

	public static StoreResource getLibraryResourceRealFile(Renderer renderer, String realFileName) {
		if (renderer.hasShareAccess(new File(realFileName))) {
			List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(realFileName, "RealFile", "RealFolder");
			for (Long id : ids) {
				StoreResource resource = renderer.getMediaStore().getResource(id.toString());
				if (resource != null) {
					return resource;
				}
			}
			LOGGER.error("{} not found as RealFile in database.", realFileName);
		}
		return null;
	}

	public static StoreResource getLibraryResourcePlaylist(Renderer renderer, String realFileName) {
		if (renderer.hasShareAccess(new File(realFileName))) {
			List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(realFileName, "PlaylistFolder", "RealFolder");
			for (Long id : ids) {
				StoreResource resource = renderer.getMediaStore().getResource(id.toString());
				if (resource != null) {
					return resource;
				}
			}
			LOGGER.error("{} not found as PlaylistFolder in database.", realFileName);
		}
		return null;
	}

	public static MusicBrainzPersonFolder getLibraryResourcePersonFolder(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(typeIdent.toString(), MusicBrainzPersonFolder.class);
		for (Long id : ids) {
			StoreResource resource = renderer.getMediaStore().getResource(id.toString());
			if (resource != null) {
				return (MusicBrainzPersonFolder) resource;
			}
		}
		LOGGER.debug("Person '{}' not found in database.", typeIdent.toString());
		return null;
	}

	public static MusicBrainzAlbumFolder getLibraryResourceMusicBrainzFolder(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(typeIdent.toString(), MusicBrainzAlbumFolder.class);
		for (Long id : ids) {
			StoreResource resource = renderer.getMediaStore().getResource(id.toString());
			if (resource != null) {
				return (MusicBrainzAlbumFolder) resource;
			}
		}
		LOGGER.debug("MusicBrainz album '{}' not found in database.", typeIdent.toString());
		return null;
	}

	public static VirtualFolderDbId getLibraryResourceVirtualFolder(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(typeIdent.toString(), VirtualFolderDbId.class);
		for (Long id : ids) {
			StoreResource resource = renderer.getMediaStore().getResource(id.toString());
			if (resource != null) {
				return (VirtualFolderDbId) resource;
			}
		}
		LOGGER.debug("VirtualFolderDbId '{}' not found in database.", typeIdent.toString());
		return null;
	}

	public static StoreResource getLibraryResourceFolder(Renderer renderer, String realFolderName) {
		if (renderer.hasShareAccess(new File(realFolderName))) {
			List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(realFolderName, "RealFolder", "RealFolder");
			for (Long id : ids) {
				StoreResource resource = renderer.getMediaStore().getResource(id.toString());
				if (resource != null) {
					return resource;
				}
			}
			LOGGER.info("{} not found as RealFolder in database.", realFolderName);
		}
		return null;
	}

	/**
	 * Locates resources. We need to remember the path for each DBID resource.
	 * @param renderer
	 * @param typeIdent
	 * @return
	 */
	public static StoreResource getLibraryResourceByDbTypeIdent(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		LOGGER.debug("getLibraryResourceByDbTypeIdent : {}", typeIdent.toString());
		switch (typeIdent.type) {
			case TYPE_MUSICBRAINZ_RECORDID -> {
				if (StringUtils.isAllBlank(typeIdent.ident)) {
					return null;
				}
				return getLibraryResourceMusicBrainzFolder(renderer, typeIdent);
			}
			case TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR -> {
				if (StringUtils.isAllBlank(typeIdent.ident)) {
					return renderer.getMediaStore().getDbIdLibrary().getPersonFolder();
				}
				MusicBrainzPersonFolder personFolder = DbIdResourceLocator.getLibraryResourcePersonFolder(renderer, typeIdent);
				if (personFolder == null) {
					personFolder = addLibraryResourcePerson(renderer, typeIdent);
				}
				return personFolder;
			}
			case TYPE_PERSON_ALBUM, TYPE_PERSON_ALL_FILES -> {
				// those two folders are of type `VirtualFolderDbId`
				VirtualFolderDbId folder = getLibraryResourceVirtualFolder(renderer, typeIdent);
				if (folder == null) {
					LOGGER.debug("person album path not in database : {} " + typeIdent.toString());
				}
				return folder;
			}
			case TYPE_PERSON_ALBUM_FILES -> {
				MusicBrainzPersonFolder folder = getLibraryResourcePersonFolder(renderer, typeIdent);
				if (folder == null) {
					LOGGER.debug("person album path not in database : {} " + typeIdent.toString());
				}
				return folder;
			}
			case TYPE_MYMUSIC_ALBUM -> {
				LOGGER.trace("Returning 'my music' folder");
				return renderer.getMediaStore().getDbIdLibrary().getAudioLikesFolder();
			}
			default -> {
				LOGGER.error("implement type : " + typeIdent.type);
				return null;
			}
		}
	}
}
