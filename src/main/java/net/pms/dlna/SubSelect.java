package net.pms.dlna;


import java.io.InputStream;

import net.pms.dlna.virtual.VirtualFolder;
import net.pms.Messages;

public class SubSelect extends VirtualFolder {
	
	public SubSelect() {
		super(Messages.getString("PMS.200"), null);
	}

	public InputStream getThumbnailInputStream() {
		try {
			return downloadAndSend(thumbnailIcon, true);
		} catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}
}
