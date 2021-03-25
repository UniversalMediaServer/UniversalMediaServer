package net.pms.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.filechooser.FileSystemView;

public class ShortcutFileSystemView extends FileSystemView {
	/**
	 * We override this to allow the "Open" button to be enabled for symlink
	 * directories.
	 *
	 * @param file
	 * @return whether the File is part of the real file system
	 */
	@Override
	public boolean isFileSystem(File file) {
		if (file.isDirectory() && Files.isSymbolicLink(file.toPath())) {
			return true;
		}
		return super.isFileSystem(file);
	}

	@Override
	public File createNewFolder(File file) throws IOException {
		return getFileSystemView().createNewFolder(file);
	}
}
