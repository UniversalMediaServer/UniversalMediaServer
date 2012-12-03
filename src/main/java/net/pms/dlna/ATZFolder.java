package net.pms.dlna;

import java.io.File;
import java.util.List;
import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;

public class ATZFolder extends VirtualFolder {
	private List<File> files;

	public ATZFolder(String letter, List<File> files) {
		super(letter, null);
		this.files = files;
	}

	private void addFile(File f) {
		String str = f.getName();
		if (PMS.getConfiguration().isArchiveBrowsing() && (str.endsWith(".zip") || str.endsWith(".cbz"))) {
			addChild(new ZippedFile(f));
		} else if (PMS.getConfiguration().isArchiveBrowsing() && (str.endsWith(".rar") || str.endsWith(".cbr"))) {
			addChild(new RarredFile(f));
		} else if (PMS.getConfiguration().isArchiveBrowsing() && (str.endsWith(".tar") || str.endsWith(".gzip") || str.endsWith(".gz") || str.endsWith(".7z"))) {
			addChild(new SevenZipFile(f));
		} else if ((str.endsWith(".iso") || str.endsWith(".img")) || (f.isDirectory() && f.getName().toUpperCase().equals("VIDEO_TS"))) {
			addChild(new DVDISOFile(f));
		} else if (str.endsWith(".m3u") || str.endsWith(".m3u8") || str.endsWith(".pls")) {
			addChild(new PlaylistFolder(f));
		} else if (str.endsWith(".cue")) {
			addChild(new CueFolder(f));
		} else {
			addChild(new RealFile(f));
		}
	}

	@Override
	public void discoverChildren() {
		for (File f : files) {
			if (f.isHidden()) {
				continue;
			}
			addFile(f);
		}
	}
}
