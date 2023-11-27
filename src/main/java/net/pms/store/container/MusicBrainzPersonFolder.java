package net.pms.store.container;

import java.io.IOException;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdMediaType;
import net.pms.store.DbIdResourceLocator;
import net.pms.store.DbIdTypeAndIdent;

public class MusicBrainzPersonFolder extends VirtualFolderDbIdNamed {

	private DbIdTypeAndIdent tiAllFilesFolder;
	private DbIdTypeAndIdent tiAlbumFolder;

	public MusicBrainzPersonFolder(Renderer renderer, String personName, DbIdTypeAndIdent typeIdent) {
		super(renderer, personName, typeIdent);
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
		return (VirtualFolderDbId) DbIdResourceLocator.getLibraryResourceByDbTypeIdent(renderer, tiAllFilesFolder);
	}

	public VirtualFolderDbId getAlbumFolder() {
		return (VirtualFolderDbId) DbIdResourceLocator.getLibraryResourceByDbTypeIdent(renderer, tiAlbumFolder);
	}
}