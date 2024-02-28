package net.pms.store.container;

import java.io.IOException;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdTypeAndIdent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents one person, having two folders to navigate into. One is the "all files" folder and one is the "albums" related to
 * that person in regard if it's role.
 *
 * Currently known roles are ALBUM_ARTIST, COMPOSER and CONDUCTOR.
 */
public class MusicBrainzPersonFolder extends VirtualFolderDbIdNamed {

	private static final Logger LOGGER = LoggerFactory.getLogger(MusicBrainzPersonFolder.class);

	private VirtualFolderDbId allFiles = null;
	private VirtualFolderDbId albumFiles = null;

	public MusicBrainzPersonFolder(Renderer renderer, String personName, DbIdTypeAndIdent typeIdent) {
		super(renderer, personName, typeIdent);
		if (StringUtils.isAllBlank(typeIdent.ident)) {
			LOGGER.debug("person name is blanc");
		} else {
			initChilds();
		}
	}

	@Override
	public void discoverChildren() {
		getChildren().clear();
		initChilds();
	}

	private void initChilds() {
		String encodedIdent = getIdent(getMediaType(), getMediaIdent());
		DbIdTypeAndIdent tiAllFilesFolder = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALL_FILES, encodedIdent);
		DbIdTypeAndIdent tiAlbumFolder = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, encodedIdent);
		allFiles = new VirtualFolderDbId(renderer, "AllAudioTracks", tiAllFilesFolder);
		albumFiles = new VirtualFolderDbId(renderer, "ByAlbum_lowercase", tiAlbumFolder);
		addChild(allFiles);
		addChild(albumFiles);
	}

	/*
	 * TYPE_PERSON_ALL_FILES and TYPE_PERSON_ALBUM folder are used for all person role types. To distinguish between the person roles
	 * encode the role type in the name field and revert later.
	 */
	private static String getIdent(DbIdMediaType type, String person) {
		if (type ==  null) {
			throw new RuntimeException("DbidMediaType is NULL");
		}
		switch (type) {
			case TYPE_PERSON_COMPOSER -> {
				return String.format("%s%s", DbIdMediaType.PERSON_COMPOSER_PREFIX, person);
			}
			case TYPE_PERSON_CONDUCTOR -> {
				return String.format("%s%s", DbIdMediaType.PERSON_CONDUCTOR_PREFIX, person);
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				return String.format("%s%s", DbIdMediaType.PERSON_ALBUMARTIST_PREFIX, person);
			}
			case TYPE_PERSON -> {
				return person;
			}
			default -> {
				throw new RuntimeException("Unknown DbidMediaType " + type.toString());
			}
		}
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		MediaTableCoverArtArchive.CoverArtArchiveResult res = MediaTableCoverArtArchive.findMBID(getMediaIdent());
		if (res.isFound()) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(res.getCoverBytes());
		}
		return super.getThumbnailInputStream();
	}

	public VirtualFolderDbId getAllFilesFolder() {

		return allFiles;
	}

	public VirtualFolderDbId getAlbumFolder() {
		return albumFiles;
	}

	public VirtualFolderDbId getPersonFolder(Renderer renderer) {
		switch (getMediaType()) {
			case TYPE_PERSON_COMPOSER -> {
				return renderer.getMediaStore().getDbIdLibrary().getPersonComposerFolder();
			}
			case TYPE_PERSON_CONDUCTOR -> {
				return renderer.getMediaStore().getDbIdLibrary().getPersonConductorFolder();
			}
			case TYPE_PERSON_ALBUMARTIST -> {
				return renderer.getMediaStore().getDbIdLibrary().getPersonAlbumArtistFolder();
			}
			case TYPE_PERSON -> {
				return renderer.getMediaStore().getDbIdLibrary().getPersonArtistFolder();
			}
			default -> {
				throw new RuntimeException("Unknown DbidMediaType " + getMediaType());
			}
		}
	}
}