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
package net.pms.newgui;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Vector;
import javax.swing.Icon;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO This bug was fixed in Java 6u18 and is no longer relevant. It also only applied for Windows XP. This class can be deleted.
/**
 * Fallback implementation of a FileSystemView.
 * <p>
 * Intended usage:<br>
 * If the standard JFileChooser cannot open due to an exception, as described in <a
 * href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6544857">http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6544857</a>
 * <p>
 * Example:
 *
 * <pre>
 *   File currentDir = ...;
 *   JFrame parentFrame = ...;
 *   JFileChooser chooser;
 *   try {
 *       chooser = new JFileChooser(currentDir);
 *   } catch (Exception e) {
 *       chooser = new JFileChooser(currentDir, new RestrictedFileSystemView());
 *   }
 *   int returnValue = chooser.showOpenDialog(parentFrame);
 * </pre>
 *
 * This FileSystemView only provides basic functionality (and probably a poor look & feel), but it can be a life saver
 * if otherwise no dialog pops up in your application.
 * <p>
 * The implementation does <strong>not</strong> use <code>sun.awt.shell.*</code> classes.
 *
 */
public class RestrictedFileSystemView extends FileSystemView {
	private static final Logger LOGGER = LoggerFactory.getLogger(RestrictedFileSystemView.class);
	private static final String NEW_FOLDER_STRING = UIManager.getString("FileChooser.other.newFolder");
	private File defaultDirectory;

	public RestrictedFileSystemView() {
		this(null);
	}

	public RestrictedFileSystemView(File defaultDirectory) {
		this.defaultDirectory = defaultDirectory;
	}

	/**
	 * Determines if the given file is a root in the navigatable tree(s).
	 *
	 * @param f a <code>File</code> object representing a directory
	 * @return <code>true</code> if <code>f</code> is a root in the navigatable tree.
	 * @see #isFileSystemRoot
	 */
	@Override
	public boolean isRoot(File f) {
		if (f == null || !f.isAbsolute()) {
			return false;
		}

		File[] roots = getRoots();
		for (File root : roots) {
			if (root.equals(f)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the file (directory) can be visited. Returns false if the directory cannot be traversed.
	 *
	 * @param f the <code>File</code>
	 * @return <code>true</code> if the file/directory can be traversed, otherwise <code>false</code>
	 * @see JFileChooser#isTraversable
	 * @see FileView#isTraversable
	 */
	@Override
	public Boolean isTraversable(File f) {
		return f.isDirectory();
	}

	/**
	 * Name of a file, directory, or folder as it would be displayed in a system file browser
	 *
	 * @param f a <code>File</code> object
	 * @return the file name as it would be displayed by a native file chooser
	 * @see JFileChooser#getName
	 */
	@Override
	public String getSystemDisplayName(File f) {
		String name = null;
		if (f != null) {
			if (isRoot(f)) {
				name = f.getAbsolutePath();
			} else {
				name = f.getName();
			}
		}
		return name;
	}

	/**
	 * Type description for a file, directory, or folder as it would be displayed in a system file browser.
	 *
	 * @param f a <code>File</code> object
	 * @return the file type description as it would be displayed by a native file chooser or null if no native
	 * information is available.
	 * @see JFileChooser#getTypeDescription
	 */
	@Override
	public String getSystemTypeDescription(File f) {
		return null;
	}

	/**
	 * Icon for a file, directory, or folder as it would be displayed in a system file browser.
	 *
	 * @param f a <code>File</code> object
	 * @return an icon as it would be displayed by a native file chooser, null if not available
	 * @see JFileChooser#getIcon
	 */
	@Override
	public Icon getSystemIcon(File f) {
		if (f != null) {
			return UIManager.getIcon(f.isDirectory() ? "FileView.directoryIcon" : "FileView.fileIcon");
		} else {
			return null;
		}
	}

	/**
	 * @param folder a <code>File</code> object representing a directory
	 * @param file a <code>File</code> object
	 * @return <code>true</code> if <code>folder</code> is a directory and contains
	 * <code>file</code>.
	 */
	@Override
	public boolean isParent(File folder, File file) {
		if (folder == null || file == null) {
			return false;
		} else {
			return folder.equals(file.getParentFile());
		}
	}

	/**
	 * @param parent a <code>File</code> object representing a directory
	 * @param fileName a name of a file or folder which exists in <code>parent</code>
	 * @return a File object. This is normally constructed with <code>new
	 * File(parent, fileName)</code>.
	 */
	@Override
	public File getChild(File parent, String fileName) {
		return createFileObject(parent, fileName);
	}

	/**
	 * Checks if <code>f</code> represents a real directory or file as opposed to a special folder such as
	 * <code>"Desktop"</code>. Used by UI classes to decide if a folder is selectable when doing directory choosing.
	 *
	 * @param f a <code>File</code> object
	 * @return <code>true</code> if <code>f</code> is a real file or directory.
	 */
	@Override
	public boolean isFileSystem(File f) {
		return true;
	}

	/**
	 * Returns whether a file is hidden or not.
	 */
	@Override
	public boolean isHiddenFile(File f) {
		return f.isHidden();
	}

	/**
	 * Is dir the root of a tree in the file system, such as a drive or partition.
	 *
	 * @param dir a <code>File</code> object representing a directory
	 * @return <code>true</code> if <code>f</code> is a root of a filesystem
	 * @see #isRoot
	 */
	@Override
	public boolean isFileSystemRoot(File dir) {
		return isRoot(dir);
	}

	/**
	 * Used by UI classes to decide whether to display a special icon for drives or partitions, e.g. a "hard disk" icon.
	 *
	 * The default implementation has no way of knowing, so always returns false.
	 *
	 * @param dir a directory
	 * @return <code>false</code> always
	 */
	@Override
	public boolean isDrive(File dir) {
		return false;
	}

	/**
	 * Used by UI classes to decide whether to display a special icon for a floppy disk. Implies isDrive(dir).
	 *
	 * The default implementation has no way of knowing, so always returns false.
	 *
	 * @param dir a directory
	 * @return <code>false</code> always
	 */
	@Override
	public boolean isFloppyDrive(File dir) {
		return false;
	}

	/**
	 * Used by UI classes to decide whether to display a special icon for a computer node, e.g. "My Computer" or a
	 * network server.
	 *
	 * The default implementation has no way of knowing, so always returns false.
	 *
	 * @param dir a directory
	 * @return <code>false</code> always
	 */
	@Override
	public boolean isComputerNode(File dir) {
		return false;
	}

	/**
	 * Returns all root partitions on this system. For example, on Windows, this would be the "Desktop" folder, while on
	 * DOS this would be the A: through Z: drives.
	 */
	@Override
	public File[] getRoots() {
		return File.listRoots();
	}

	// Providing default implementations for the remaining methods
	// because most OS file systems will likely be able to use this
	// code. If a given OS can't, override these methods in its
	// implementation.
	@Override
	public File getHomeDirectory() {
		return createFileObject(System.getProperty("user.home"));
	}

	/**
	 * Return the user's default starting directory for the file chooser.
	 *
	 * @return a <code>File</code> object representing the default starting folder
	 */
	@Override
	public File getDefaultDirectory() {
		if (defaultDirectory == null) {
			try {
				File tempFile = File.createTempFile("filesystemview", "restricted");
				tempFile.deleteOnExit();
				defaultDirectory = tempFile.getParentFile();
			} catch (IOException e) {
				LOGGER.debug("Caught exception", e);
			}
		}
		return defaultDirectory;
	}

	/**
	 * Returns a File object constructed in dir from the given filename.
	 */
	@Override
	public File createFileObject(File dir, String filename) {
		if (dir == null) {
			return new File(filename);
		} else {
			return new File(dir, filename);
		}
	}

	/**
	 * Returns a File object constructed from the given path string.
	 */
	@Override
	public File createFileObject(String path) {
		File f = new File(path);
		if (isFileSystemRoot(f)) {
			f = createFileSystemRoot(f);
		}
		return f;
	}

	/**
	 * Gets the list of shown (i.e. not hidden) files.
	 */
	@Override
	public File[] getFiles(File dir, boolean useFileHiding) {
		Vector<File> files = new Vector<>();

		// add all files in dir
		File[] names;
		names = dir.listFiles();
		File f;

		int nameCount = (names == null) ? 0 : names.length;
		for (int i = 0; i < nameCount; i++) {
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
			f = names[i];
			if (!useFileHiding || !isHiddenFile(f)) {
				files.addElement(f);
			}
		}

		return files.toArray(new File[0]);
	}

	/**
	 * Returns the parent directory of <code>dir</code>.
	 *
	 * @param dir the <code>File</code> being queried
	 * @return the parent directory of <code>dir</code>, or <code>null</code> if <code>dir</code> is
	 * <code>null</code>
	 */
	@Override
	public File getParentDirectory(File dir) {
		if (dir != null && dir.exists()) {
			File psf = dir.getParentFile();
			if (psf != null) {
				if (isFileSystem(psf)) {
					File f = psf;
					if (!f.exists()) {
						// This could be a node under "Network Neighborhood".
						File ppsf = psf.getParentFile();
						if (ppsf == null || !isFileSystem(ppsf)) {
							// We're mostly after the exists() override for windows below.
							f = createFileSystemRoot(f);
						}
					}
					return f;
				} else {
					return psf;
				}
			}
		}
		return null;
	}

	/**
	 * Creates a new <code>File</code> object for <code>f</code> with correct behavior for a file system root
	 * directory.
	 *
	 * @param f a <code>File</code> object representing a file system root directory, for example "/" on Unix or "C:\"
	 * on Windows.
	 * @return a new <code>File</code> object
	 */
	@Override
	protected File createFileSystemRoot(File f) {
		return new FileSystemRoot(f);
	}

	static class FileSystemRoot extends File {
		private static final long serialVersionUID = -807847319198119832L;

		public FileSystemRoot(File f) {
			super(f, "");
		}

		public FileSystemRoot(String s) {
			super(s);
		}

		@Override
		public boolean isDirectory() {
			return true;
		}

		@Override
		public String getName() {
			return getPath();
		}
	}

	@Override
	public File createNewFolder(File containingDir) throws IOException {
		if (containingDir == null) {
			throw new IOException("Containing directory is null:");
		}
		File newFolder;
		newFolder = createFileObject(containingDir, NEW_FOLDER_STRING);
		int i = 2;
		while (newFolder.exists() && (i < 100)) {
			newFolder = createFileObject(containingDir, MessageFormat.format(NEW_FOLDER_STRING,
				new Object[]{i}));
			i++;
		}

		if (newFolder.exists()) {
			throw new IOException("Directory already exists:" + newFolder.getAbsolutePath());
		} else {
			if (!newFolder.mkdirs()) {
				LOGGER.debug("Could not create directory \"" + newFolder.getAbsolutePath() + "\"");
			}
		}

		return newFolder;
	}
}
