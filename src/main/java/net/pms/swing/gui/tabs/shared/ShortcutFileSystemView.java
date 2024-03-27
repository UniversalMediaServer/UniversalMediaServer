/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.swing.gui.tabs.shared;

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
