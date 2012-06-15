package net.pms.dlna;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.swing.JComponent;

import net.pms.dlna.virtual.VirtualFolder;
import net.pms.external.AdditionalResourceFolderListener;
import net.pms.util.OpenSubtitle;

public class SubSelect extends VirtualFolder {
	public SubSelect() {
		super("Select Subtitle", null);
	}

	public InputStream getThumbnailInputStream() {
		try {
			return downloadAndSend(thumbnailIcon, true);
		} catch (Exception e) {
			return super.getThumbnailInputStream();
		}
	}
}
