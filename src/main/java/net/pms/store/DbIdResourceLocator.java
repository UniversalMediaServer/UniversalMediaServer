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
import net.pms.store.container.PersonFolder;
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

	public static PersonFolder getLibraryResourcePersonFolder(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		List<Long> ids = MediaStoreIds.getMediaStoreIdsForName(typeIdent.toString(), PersonFolder.class);
		for (Long id : ids) {
			StoreResource resource = renderer.getMediaStore().getResource(id.toString());
			if (resource != null) {
				return (PersonFolder) resource;
			}
		}
		LOGGER.error("Person '{}' not found in database.", typeIdent.ident);
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
			LOGGER.error("{} not found as RealFolder in database.", realFolderName);
		}
		return null;
	}

	public static StoreResource getLibraryResourceByDbTypeIdent(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		switch (typeIdent.type) {
			case TYPE_MUSICBRAINZ_RECORDID -> {
				VirtualFolderDbId container = renderer.getMediaStore().getMbidFolder();
				container.discoverChildren();
				return container;
			}
			case TYPE_PERSON, TYPE_PERSON_COMPOSER, TYPE_PERSON_CONDUCTOR -> {
				if (StringUtils.isAllBlank(typeIdent.ident)) {
					return renderer.getMediaStore().getPersonFolder();
				}
				PersonFolder personFolder = getLibraryResourcePersonFolder(renderer, typeIdent);
				if (personFolder == null) {
					personFolder = new PersonFolder(renderer, typeIdent.ident, typeIdent);
					renderer.getMediaStore().getPersonFolder().addChild(personFolder);
				}
				return personFolder;
			}
			case TYPE_PERSON_ALBUM, TYPE_PERSON_ALL_FILES -> {
				LOGGER.error("TYPE_PERSON_ALBUM, TYPE_PERSON_ALL_FILES are not stored in database.");
				return null;
			}
			case TYPE_MYMUSIC_ALBUM -> {
				return renderer.getMediaStore().getAudioLikesFolder();
			}
			default -> {
				LOGGER.error("implement type : " + typeIdent.type);
				return null;
			}
		}
	}
}
