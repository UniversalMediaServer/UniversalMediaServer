package net.pms.dlna;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.FormatFactory;

public class ATZFolder extends VirtualFolder {
	private static final Logger LOGGER = LoggerFactory.getLogger(ATZFolder.class);
	private List<File> files;

	public ATZFolder(String letter, List<File> files) {
		super(letter, null);
		this.files = files;
	}
	
	/*private boolean isFolderRelevant(File f) {
		boolean isRelevant = false;

		if (f.isDirectory() && PMS.getConfiguration().isHideEmptyFolders()) {
			File[] children = f.listFiles();

			// listFiles() returns null if "this abstract pathname does not denote a directory, or if an I/O error occurs".
			// in this case (since we've already confirmed that it's a directory), this seems to mean the directory is non-readable
			// http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15135
			// http://stackoverflow.com/questions/3228147/retrieving-the-underlying-error-when-file-listfiles-return-null
			if (children == null) {
				LOGGER.warn("Can't list files in non-readable directory: {}", f.getAbsolutePath());
			} else {
				for (File child : children) {
					if (child.isFile()) {
						if (FormatFactory.getAssociatedExtension(child.getName()) != null || isFileRelevant(child)) {
							isRelevant = true;
							break;
						}
					} else {
						if (isFolderRelevant(child)) {
							isRelevant = true;
							break;
						}
					}
				}
			}
		}

		return isRelevant;
	}*/
	
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
			/* Optionally ignore empty directories */
			/*if (f.isDirectory() && PMS.getConfiguration().isHideEmptyFolders() && !isFolderRelevant(f)) {
				LOGGER.debug("Ignoring empty/non-relevant directory: " + f.getName());
			} else { // Otherwise add the file*/
				addChild(new RealFile(f));
			//}
		}
		
	}
	
	public void discoverChildren() {
		for (File f : files) {
			if (f.isHidden()) {
				continue;
			}
			addFile(f);
		}
	}
	
	

}
