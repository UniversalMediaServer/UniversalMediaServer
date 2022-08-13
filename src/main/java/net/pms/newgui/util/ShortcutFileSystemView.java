package net.pms.newgui.util;

import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileSystemView;
import net.pms.util.FileUtil;

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
		if ("Win32ShellFolder2".equals(file.getClass().getName()) && file.isDirectory() && FileUtil.isSymbolicLink(file)) {
			return true;
		}
		return super.isFileSystem(file);
	}

	@Override
	public File createNewFolder(File file) throws IOException {
		return getFileSystemView().createNewFolder(file);
	}
}
