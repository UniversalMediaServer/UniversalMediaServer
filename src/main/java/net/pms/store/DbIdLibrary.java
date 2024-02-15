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

import java.util.List;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.database.MediaTableMusicBrainzReleases;
import net.pms.media.audio.metadata.MusicBrainzAlbum;
import net.pms.renderers.Renderer;
import net.pms.store.container.MediaLibrary;
import net.pms.store.container.MusicBrainzAlbumFolder;
import net.pms.store.container.MusicBrainzPersonFolder;
import net.pms.store.container.VirtualFolderDbId;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Surf@ceS
 */
public class DbIdLibrary {

	private static final Logger LOGGER = LoggerFactory.getLogger(DbIdLibrary.class);
	private static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private final Renderer renderer;
	private VirtualFolderDbId audioLikesFolder;
	private VirtualFolderDbId mbidFolder;
	private VirtualFolderDbId personArtistFolder;
	private VirtualFolderDbId personAlbumArtistFolder;
	private VirtualFolderDbId personComposerFolder;
	private VirtualFolderDbId personConductorFolder;

	public DbIdLibrary(Renderer renderer) {
		this.renderer = renderer;
	}

	public VirtualFolderDbId getAudioLikesFolder() {
		return audioLikesFolder;
	}

	/**
	 * MusicBrainz album folder
	 * @return
	 */
	public VirtualFolderDbId getMbidFolder() {
		return mbidFolder;
	}

	/**
	 * Person root folder
	 */
	public VirtualFolderDbId getPersonArtistFolder() {
		return personArtistFolder;
	}

	public VirtualFolderDbId getPersonAlbumArtistFolder() {
		return personAlbumArtistFolder;
	}

	public VirtualFolderDbId getPersonConductorFolder() {
		return personConductorFolder;
	}

	public VirtualFolderDbId getPersonComposerFolder() {
		return personComposerFolder;
	}

	protected final void reset(List<StoreResource> backupChildren) {
		if (CONFIGURATION.useNextcpApi()) {
			setAudioLikesFolder(backupChildren);
			setPersonFolder();
		}
	}

	private void addChildToMediaLibraryAudioFolder(VirtualFolderDbId dbIdFolder) {
		if (renderer.getUmsConfiguration().isShowMediaLibraryFolder()) {
			MediaLibrary mediaLibrary = renderer.getMediaStore().getMediaLibrary();
			if (mediaLibrary != null &&
					mediaLibrary.getAudioFolder() != null &&
					mediaLibrary.getAudioFolder().getChildren() != null &&
					!mediaLibrary.getAudioFolder().getChildren().contains(dbIdFolder)) {
				mediaLibrary.getAudioFolder().addChild(dbIdFolder);
				LOGGER.debug("adding '{}' folder to the 'Audio' folder of MediaLibrary", dbIdFolder.getDisplayName());
			}
		}
	}

	private void setAudioLikesFolder(List<StoreResource> backupChildren) {
		if (audioLikesFolder == null) {
			audioLikesFolder = new VirtualFolderDbId(renderer, "MyAlbums", new DbIdTypeAndIdent(DbIdMediaType.TYPE_MYMUSIC_ALBUM, null));
		}
		if (PMS.getConfiguration().displayAudioLikesInRootFolder()) {
			if (backupChildren.contains(audioLikesFolder)) {
				renderer.getMediaStore().addChildInternal(audioLikesFolder, false);
			} else {
				renderer.getMediaStore().addChild(audioLikesFolder);
			}
			LOGGER.debug("adding My Albums folder to the root of MediaStore");
		} else {
			addChildToMediaLibraryAudioFolder(audioLikesFolder);
		}
		if (backupChildren.contains(audioLikesFolder)) {
			backupChildren.remove(audioLikesFolder);
		}
	}

	private void setPersonFolder() {
		if (personArtistFolder == null) {
			personArtistFolder = new VirtualFolderDbId(renderer, "BrowseByArtist", new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, null));
		}
		addChildToMediaLibraryAudioFolder(personArtistFolder);

		if (personComposerFolder == null) {
			personComposerFolder = new VirtualFolderDbId(renderer, "BrowseByComposer", new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_COMPOSER, null));
		}
		addChildToMediaLibraryAudioFolder(personComposerFolder);

		if (personConductorFolder == null) {
			personConductorFolder = new VirtualFolderDbId(renderer, "BrowseByConductor", new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_CONDUCTOR, null));
		}
		addChildToMediaLibraryAudioFolder(personConductorFolder);

		if (personAlbumArtistFolder == null) {
			personAlbumArtistFolder = new VirtualFolderDbId(renderer, "BrowseByAlbumArtist", new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUMARTIST, null));
		}
		addChildToMediaLibraryAudioFolder(personAlbumArtistFolder);
	}

	/**
	 * Adds a person folder to the library
	 *
	 * @param renderer
	 * @param typeIdent Can be composer, conductor or regular person.
	 * @return
	 */
	public static MusicBrainzPersonFolder addLibraryResourcePerson(Renderer renderer, DbIdTypeAndIdent typeIdent) {
		if (StringUtils.isAllBlank(typeIdent.ident)) {
			LOGGER.warn("no person name given.");
			return null;
		}

		MusicBrainzPersonFolder personFolder = new MusicBrainzPersonFolder(renderer, typeIdent.ident, typeIdent);
		personFolder.getPersonFolder(renderer).addChild(personFolder);
		personFolder.discoverChildren();
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
			MusicBrainzAlbumFolder mbFolder = DbIdResourceLocator.getLibraryResourceMusicBrainzFolder(renderer, typeIdent);
			if (mbFolder == null) {
				LOGGER.debug("musicBrainz album not in database : {} ", typeIdent.toString());
				MusicBrainzAlbum persistentAlbum = MediaTableMusicBrainzReleases.getMusicBrainzAlbum(album.getMbReleaseid());
				if (persistentAlbum == null) {
					MediaTableMusicBrainzReleases.storeMusicBrainzAlbum(album);
				}
				mbFolder = new MusicBrainzAlbumFolder(renderer, album);

				// Lookup person's album folder as parent
				DbIdTypeAndIdent tiPerson = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, album.getArtist());
				MusicBrainzPersonFolder person = DbIdResourceLocator.getLibraryResourcePersonFolder(renderer, tiPerson);
				if (person == null) {
					person = DbIdLibrary.addLibraryResourcePerson(renderer, tiPerson);
				}
				person.getAlbumFolder().addChild(mbFolder);
			}
			return mbFolder;
		} catch (Exception e) {
			LOGGER.error("cannot add MusicBrainzAlbumFolder.", e);
			return null;
		}
	}
}
