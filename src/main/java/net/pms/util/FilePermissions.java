/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.util;

import com.sun.jna.Platform;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object that encapsulates file permissions for a <code>File</code> object.
 * If there are insufficient permission to read the <code>File</code> object's
 * permissions, all permissions will return false (even though some of them
 * might be true) and no <code>Exception</code> will be thrown.
 * This is due to limitations in the underlying methods.
 *
 * @author Nadahar
 * @threadsafe
 */
public class FilePermissions {
	private final File file;
	private final Path path;
	private Boolean read = null;
	private Boolean write = null;
	private Boolean execute = null;
	private final boolean folder;
	private String lastCause = null;
	private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissions.class);

	public FilePermissions(File file) throws FileNotFoundException {
		if (file == null) {
			throw new IllegalArgumentException("File argument cannot be null");
		}
		/* Go via .getAbsoluteFile() to work around a bug where new File("")
		 * (current folder) will report false to isDirectory().
		 */
		this.file = file.getAbsoluteFile();
		if (!this.file.exists()) {
			throw new FileNotFoundException("File \"" + this.file.getAbsolutePath() + "\" not found");
		}
		path = this.file.toPath();
		folder = this.file.isDirectory();
	}

	public FilePermissions(Path path) throws FileNotFoundException {
		if (path == null) {
			throw new IllegalArgumentException("Path argument cannot be null");
		}
		this.path = path;
		if (!Files.exists(this.path)) {
			throw new FileNotFoundException("File \"" + this.path + "\" not found");
		}
		file = path.toFile();
		folder = Files.isDirectory(this.path);
	}

	/**
	 * Must always be called in a synchronized context
	 */
	private void checkPermissions(boolean checkRead, boolean checkWrite, boolean checkExecute) {

		if (read == null && checkRead) {
			try {
				path.getFileSystem().provider().checkAccess(path, AccessMode.READ);
				read = true;
			} catch (AccessDeniedException e) {
				if (path.toString().equals(e.getMessage())) {
					lastCause = "Insufficient permission to read permissions";
				} else if ("Permissions does not allow requested access".equals(e.getMessage())) {
					lastCause = "Permissions don't allow read access";
				} else {
					lastCause = e.getMessage();
				}
				read = false;
			} catch (IOException e) {
				lastCause = e.getMessage();
				read = false;
			}
		}
		if (write == null && checkWrite) {
			try {
				path.getFileSystem().provider().checkAccess(path, AccessMode.WRITE);
				write = true;
			} catch (AccessDeniedException e) {
				if (e.getMessage().endsWith("Permissions does not allow requested access")) {
					lastCause = "Permissions don't allow write access";
				} else {
					lastCause = e.getMessage();
				}
				write = false;
			} catch (FileSystemException e) {
				// A workaround for https://bugs.openjdk.java.net/browse/JDK-8034057
				// and similar bugs, if we can't determine it the nio way, fall
				// back to actually testing it.
				LOGGER.trace(
					"Couldn't determine write permissions for \"{}\", falling back to write testing. The error was: {}",
					path.toString(),
					e.getMessage()
				);
				if (folder) {
					write = testFolderWritable();
				} else {
					write = testFileWritable(file);
				}
			} catch (IOException e) {
				lastCause = e.getMessage();
				write = false;
			}
		}
		if (execute == null && checkExecute) {
			// To conform to the fact that on Linux root always implicit
			// execute permission regardless of explicit permissions
			if (Platform.isLinux() && FileUtil.isAdmin()) {
				execute = true;
			} else {
				try {
					path.getFileSystem().provider().checkAccess(path, AccessMode.EXECUTE);
					execute = true;
				} catch (AccessDeniedException e) {
					if (e.getMessage().endsWith("Permissions does not allow requested access")) {
						lastCause = "Permissions don't allow execute access";
					} else {
						lastCause = e.getMessage();
					}
					execute = false;
				} catch (IOException e) {
					lastCause = e.getMessage();
					execute = false;
				}
			}
		}
	}

	/**
	 * @return Whether the <code>File</code> object this <code>FilePermission</code>
	 * object represents is a folder.
	 */
	public boolean isFolder() {
		return folder;
	}

	/**
	 * @return Whether the file or folder is readable in the current context.
	 */
	public synchronized boolean isReadable() {
		checkPermissions(true, false, false);
		return read;
	}

	/**
	 * @return Whether the file or folder is writable in the current context.
	 */
	public synchronized boolean isWritable() {
		checkPermissions(false, true, false);
		return write;
	}

	/**
	 * @return Whether the file is executable in the current context, or if
	 * folder listing is permitted in the current context if it's a folder.
	 */
	public synchronized boolean isExecutable() {
		checkPermissions(false, false, true);
		return execute;
	}

	/**
	 * @return Whether the listing of the folder's content is permitted.
	 * For this to be <code>true</code> {@link #isFolder()}, {@link #isReadable()}
	 * and {@link #isExecutable()} must be true.
	 */
	public synchronized boolean isBrowsable() {
		checkPermissions(true, false, true);
		return folder && read && execute;
	}

	/**
	 * @return The <code>File</code> object this <code>FilePermission</code>
	 * object represents.
	 */
	public File getFile() {
		return file;
	}

	/**
	 * @return The <code>Path</code> object this <code>FilePermission</code>
	 * object represents.
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * Re-reads file or folder permissions in case they have changed.
	 */
	public synchronized void refresh() {
		read = null;
		write = null;
		execute = null;
		lastCause = null;
	}

	public synchronized String getLastCause() {
		return lastCause;
	}

	@Override
	public synchronized String toString() {
		checkPermissions(true, true, true);
		StringBuilder sb = new StringBuilder();
		sb.append(folder ? "d" : "-");
		if (read == null) {
			sb.append('?');
		} else {
			sb.append(read ? "r" : "-");
		}
		if (write == null) {
			sb.append('?');
		} else {
			sb.append(write ? "w" : "-");
		}
		if (execute == null) {
			sb.append('?');
		} else {
			sb.append(execute ? "x" : "-");
		}
		return sb.toString();
	}

	/**
	 * Must always be called in a synchronized context
	 */
	private boolean testFolderWritable() {
		if (!folder) {
			throw new IllegalStateException("Can only be called on a folder");
		}
		boolean isWritable = false;

		File file = new File(
			this.file,
			String.format(
				"UMS_folder_write_test_%d_%d.tmp",
				System.currentTimeMillis(),
				Thread.currentThread().getId()
			)
		);

		try {
			if (file.createNewFile()) {
				if (testFileWritable(file)) {
					isWritable = true;
				}

				if (!file.delete()) {
					LOGGER.warn("Can't delete temporary test file: {}", file.getAbsolutePath());
				}
			}
		} catch (IOException e) {
			lastCause = e.getMessage();
		}

		return isWritable;
	}

	/**
	 * Must always be called in a synchronized context
	 */
	private boolean testFileWritable(File file) {
		file = file.getAbsoluteFile();
		if (file.isDirectory()) {
			throw new IllegalStateException("Can't be called on a folder");
		}

		boolean isWritable = false;
		boolean fileAlreadyExists = file.isFile(); // i.e. exists and is a File

		if (fileAlreadyExists || !file.exists()) {
			try {
				// fileAlreadyExists: open for append: make sure the open
				// doesn't clobber the file
				new FileOutputStream(file, true).close();
				isWritable = true;

				if (!fileAlreadyExists) { // a new file has been "touched"; try to remove it
					if (!file.delete()) {
						LOGGER.warn("Can't delete temporary test file: {}", file.getAbsolutePath());
					}
				}
			} catch (IOException e) {
				lastCause = e.getMessage();
			}
		} else {
			lastCause = file.getAbsolutePath() + " isn't a file";
		}

		return isWritable;
	}
}
