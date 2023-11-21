package net.pms.store.container;

import java.io.IOException;
import net.pms.database.MediaTableCoverArtArchive;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.renderers.Renderer;
import net.pms.store.DbIdTypeAndIdent;

public class PersonFolder extends VirtualFolderDbIdNamed {

	public PersonFolder(Renderer renderer, String personName, DbIdTypeAndIdent typeIdent) {
		super(renderer, personName, typeIdent);
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		MediaTableCoverArtArchive.CoverArtArchiveResult res = MediaTableCoverArtArchive.findMBID(getMediaIdent());
		if (res.isFound()) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(res.getCoverBytes());
		}
		return super.getThumbnailInputStream();
	}
}
