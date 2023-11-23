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
import net.pms.renderers.Renderer;
import net.pms.store.container.MediaLibrary;
import net.pms.store.container.VirtualFolderDbId;
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
	private VirtualFolderDbId personFolder;

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
	public VirtualFolderDbId getPersonFolder() {
		return personFolder;
	}

	protected final void reset(List<StoreResource> backupChildren) {
		if (CONFIGURATION.useNextcpApi()) {
			setAudioLikesFolder(backupChildren);
			setMbidFolder();
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

	private void setMbidFolder() {
		if (mbidFolder == null) {
			mbidFolder = new VirtualFolderDbId(renderer, "BrowseByMusicBrainzAlbum", new DbIdTypeAndIdent(DbIdMediaType.TYPE_MUSICBRAINZ_RECORDID, null));
		}
		addChildToMediaLibraryAudioFolder(mbidFolder);
	}

	private void setPersonFolder() {
		if (personFolder == null) {
			personFolder = new VirtualFolderDbId(renderer, "BrowseByPerson", new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, null));
		}
		personFolder = new VirtualFolderDbId(renderer, "BrowseByPerson", new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON, null));
	}

}
