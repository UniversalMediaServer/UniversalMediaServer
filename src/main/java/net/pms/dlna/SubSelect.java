package net.pms.dlna;

import java.io.IOException;
import net.pms.Messages;
import net.pms.dlna.virtual.VirtualFolder;

public class SubSelect extends VirtualFolder {
	public SubSelect() {
		super(Messages.getString("PMS.133"), null);
	}

	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		try {
			return DLNAThumbnailInputStream.toThumbnailInputStream(downloadAndSend(thumbnailIcon, true));
		} catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}
}
