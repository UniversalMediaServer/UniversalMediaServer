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
import net.pms.renderers.Renderer;
import net.pms.store.container.MusicBrainzAlbumFolder;
import net.pms.store.container.MusicBrainzPersonFolder;
import net.pms.store.container.VirtualFolderDbId;
import net.pms.store.container.VirtualFolderDbIdNamed;
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
			if (resource instanceof MusicBrainzPersonFolder personFolder) {
				return personFolder;
			}
		}
		LOGGER.info("Person '{}' not found in database.", typeIdent.ident);
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

	public static MusicBrainzAlbumFolder getLibraryResourceMusicBrainzAlbum(Renderer renderer, DbIdTypeAndIdent musicBrainzType) {
		List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(musicBrainzType.toString(), MusicBrainzAlbumFolder.class);
		for (Long id : ids) {
			StoreResource resource = renderer.getMediaStore().getResource(id.toString());
			if (resource instanceof  MusicBrainzAlbumFolder musicBrainzAlbumFolder) {
				return musicBrainzAlbumFolder;
			}
		}
		LOGGER.info("{} not found as MusicBrainzID in database.", musicBrainzType);
		return null;
	}

	/**
	 * Locates resources. We need to remember the path for each DBID resource.
	 * @param renderer
	 * @param typeIdent
	 * @return
	 */
	public static StoreResource getLibraryResourceByDbTypeIdent(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		switch (typeIdent.type) {
			case TYPE_MUSICBRAINZ_RECORDID -> {
				if (StringUtils.isAllBlank(typeIdent.ident)) {
					return renderer.getMediaStore().getDbIdLibrary().getMbidFolder();
				}
				MusicBrainzAlbumFolder album = getLibraryResourceMusicBrainzAlbum(renderer, typeIdent);
				if (album == null) {
					album = new MusicBrainzAlbumFolder(renderer, typeIdent.ident, typeIdent);
					renderer.getMediaStore().getDbIdLibrary().getMbidFolder().addChild(album);
				}
				return album;
			}
			case TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR -> {
				if (StringUtils.isAllBlank(typeIdent.ident)) {
					return renderer.getMediaStore().getDbIdLibrary().getPersonFolder();
				}
				MusicBrainzPersonFolder person = getLibraryResourcePersonFolder(renderer, typeIdent);
				if (person == null) {
					person = new MusicBrainzPersonFolder(renderer, typeIdent.ident, typeIdent);
					renderer.getMediaStore().getDbIdLibrary().getMbidFolder().addChild(person);
				}
				return person;
			}
			case TYPE_PERSON_ALBUM -> {
				VirtualFolderDbId folder = getLibraryResourcePersonFolder(renderer, typeIdent);
				if (folder == null) {
					folder = new VirtualFolderDbIdNamed(renderer, "all albums by " + typeIdent.ident, typeIdent);
				}
				DbIdTypeAndIdent parentTypeIdent = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeIdent.ident);
				VirtualFolderDbId parent = getLibraryResourcePersonFolder(renderer, parentTypeIdent);
				parent.addChild(folder);
				return folder;
			}
			case TYPE_PERSON_ALL_FILES -> {
				VirtualFolderDbId folder = getLibraryResourcePersonFolder(renderer, typeIdent);
				if (folder == null) {
					folder = new VirtualFolderDbIdNamed(renderer, "all files by " + typeIdent.ident, typeIdent);
				}
				DbIdTypeAndIdent parentTypeIdent = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, typeIdent.ident);
				VirtualFolderDbId parent = getLibraryResourcePersonFolder(renderer, parentTypeIdent);
				parent.addChild(folder);
				return folder;
			}
			case TYPE_PERSON_ALBUM_FILES -> {
				VirtualFolderDbId folder = new VirtualFolderDbId(renderer, "Album" + typeIdent.ident, typeIdent);
				VirtualFolderDbId parent = getLibraryResourcePersonFolder(renderer, typeIdent);
				folder.setParent(parent);
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
