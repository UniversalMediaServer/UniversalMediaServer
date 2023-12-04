package net.pms.store.container;

import java.io.IOException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.DbIdTypeAndIdent;

public class MusicBrainzPersonFolder extends VirtualFolderDbIdNamed {

	private static final Logger LOGGER = LoggerFactory.getLogger(MusicBrainzPersonFolder.class);

	private DbIdTypeAndIdent tiAllFilesFolder;
	private DbIdTypeAndIdent tiAlbumFolder;

	@Override
	public boolean isDiscovered() {
		return false;
	}

	public MusicBrainzPersonFolder(Renderer renderer, String personName, DbIdTypeAndIdent typeIdent) {
		super(renderer, personName, typeIdent);
		if (StringUtils.isAllBlank(typeIdent.ident)) {
			LOGGER.debug("preson name is blanc");
		}
		tiAllFilesFolder = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALL_FILES, typeIdent.ident);
		tiAlbumFolder = new DbIdTypeAndIdent(DbIdMediaType.TYPE_PERSON_ALBUM, typeIdent.ident);
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
		if (StringUtils.isAllBlank(tiAllFilesFolder.ident)) {
			return null;
		}
		return (VirtualFolderDbId) DbIdResourceLocator.getLibraryResourceByDbTypeIdent(renderer, tiAllFilesFolder);
	}

	public VirtualFolderDbId getAlbumFolder() {
		if (StringUtils.isAllBlank(tiAlbumFolder.ident)) {
			return null;
		}
		return (VirtualFolderDbId) DbIdResourceLocator.getLibraryResourceByDbTypeIdent(renderer, tiAlbumFolder);
	}
}