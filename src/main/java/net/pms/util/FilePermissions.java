/*
 * Universal Media Server, for streaming any medias to DLNA
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

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.sun.jna.Platform;

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
			read = Boolean.valueOf(Files.isReadable(path));
		}
		if (write == null && checkWrite) {
			write = Boolean.valueOf(Files.isWritable(path));
		}
		if (execute == null && checkExecute) {
			execute = Boolean.valueOf(Files.isExecutable(path) || (Platform.isLinux() && FileUtil.isAdmin()));
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
		return read.booleanValue();
	}

	/**
	 * @return Whether the file or folder is writable in the current context.
	 */
	public synchronized boolean isWritable() {
		checkPermissions(false, true, false);
		return write.booleanValue();
	}

	/**
	 * @return Whether the file is executable in the current context, or if
	 * folder listing is permitted in the current context if it's a folder.
	 */
	public synchronized boolean isExecutable() {
		checkPermissions(false, false, true);
		return execute.booleanValue();
	}

	/**
	 * @return Whether the listing of the folder's content is permitted.
	 * For this to be <code>true</code> {@link #isFolder()}, {@link #isReadable()}
	 * and {@link #isExecutable()} must be true.
	 */
	public synchronized boolean isBrowsable() {
		checkPermissions(true, false, true);
		return folder && read.booleanValue() && execute.booleanValue();
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
	}

	@Override
	public synchronized String toString() {
		checkPermissions(true, true, true);
		StringBuilder sb = new StringBuilder();
		sb.append(folder ? "d" : "-");
		if (read == null) {
			sb.append("?");
		} else {
			sb.append(read.booleanValue() ? "r" : "-");
		}
		if (write == null) {
			sb.append("?");
		} else {
			sb.append(write.booleanValue() ? "w" : "-");
		}
		if (execute == null) {
			sb.append("?");
		} else {
			sb.append(execute.booleanValue() ? "x" : "-");
		}
		return sb.toString();
	}
}
