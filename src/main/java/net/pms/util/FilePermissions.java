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
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object that encapsulates file permissions for a {@link File} or
 * {@link Path} object. If there are insufficient permission to read the
 * object's permissions, all permissions will return {@code false} (even though
 * some of them might be true) and no {@link Exception} will be thrown. This is
 * due to limitations in the underlying methods.
 *
 * @author Nadahar
 */
@ThreadSafe
public class FilePermissions {
	private static final Logger LOGGER = LoggerFactory.getLogger(FilePermissions.class);
	private final Path path;
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	@GuardedBy("lock")
	private String lastCause = null;
	@GuardedBy("lock")
	private boolean readChecked = false;
	@GuardedBy("lock")
	private boolean writeChecked = false;
	@GuardedBy("lock")
	private boolean executeChecked = false;

	@GuardedBy("lock")
	private final EnumSet<FileFlag> flags = EnumSet.noneOf(FileFlag.class);

	/**
	 * Creates a new instance representing the file permissions of the specified
	 * {@link File}. The implementation is lazy; no permission is being
	 * evaluated until its value is requested and only the requested permission
	 * is evaluated. Evaluated values are cached, so the check is only performed
	 * once unless {@link #refresh()} is called in which case all cached values
	 * are cleared.
	 *
	 * @param file the {@link File} for which to retrieve permissions.
	 * @throws FileNotFoundException If {@code file} doesn't exist.
	 */
	public FilePermissions(File file) throws FileNotFoundException {
		if (file == null) {
			throw new IllegalArgumentException("file cannot be null");
		}
		/*
		 *  Go via .getAbsoluteFile() to work around a bug where new File("")
		 * (current folder) will report false to isDirectory().
		 */
		file = file.getAbsoluteFile();
		if (!file.exists()) {
			throw new FileNotFoundException("File \"" + file + "\" not found");
		}
		path = file.toPath();

		lock.writeLock().lock();
		try {
			if (file.isDirectory()) {
				flags.add(FileFlag.FOLDER);
			} else if (file.isFile()) {
				flags.add(FileFlag.FILE);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Creates a new instance representing the file permissions of the specified
	 * {@link Path}. The implementation is lazy; no permission is being
	 * evaluated until its value is requested and only the requested permission
	 * is evaluated. Evaluated values are cached, so the check is only performed
	 * once unless {@link #refresh()} is called in which case all cached values
	 * are cleared.
	 *
	 * @param path the {@link Path} for which to retrieve permissions.
	 * @param options Any {@link LinkOption}s to use when resolving {@code path}
	 *            .
	 * @throws FileNotFoundException If {@code path} doesn't exist.
	 */
	public FilePermissions(Path path, LinkOption... options) throws FileNotFoundException {
		if (path == null) {
			throw new IllegalArgumentException("Path argument cannot be null");
		}
		if (!Files.exists(path)) {
			throw new FileNotFoundException("File \"" + path + "\" not found");
		}
		this.path = path;
		lock.writeLock().lock();
		try {
			if (Files.isDirectory(path, options)) {
				flags.add(FileFlag.FOLDER);
			} else if (Files.isRegularFile(path, options)) {
				flags.add(FileFlag.FILE);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	private void checkPermissions(boolean checkRead, boolean checkWrite, boolean checkExecute) {
		lock.readLock().lock();
		try {
			if (
				(!checkRead || readChecked) &&
				(!checkWrite || writeChecked) &&
				(!checkExecute || executeChecked)
			) {
				return;
			}
		} finally {
			lock.readLock().unlock();
		}
		lock.writeLock().lock();
		try {
			boolean checkBrowse =
				checkRead && !readChecked && (checkExecute || executeChecked) ||
				checkExecute && !executeChecked && (checkRead || readChecked);
			if (!readChecked && checkRead) {
				try {
					path.getFileSystem().provider().checkAccess(path, AccessMode.READ);
					flags.add(FileFlag.READ);
				} catch (AccessDeniedException e) {
					if (path.toString().equals(e.getMessage())) {
						lastCause = "Insufficient permission to read permissions";
					} else if ("Permissions does not allow requested access".equals(e.getMessage())) {
						lastCause = "Permissions do not allow read access";
					} else {
						lastCause = e.getMessage();
					}
				} catch (IOException e) {
					lastCause = e.getMessage();
				}
				readChecked = true;
			}
			if (!writeChecked && checkWrite) {
				try {
					path.getFileSystem().provider().checkAccess(path, AccessMode.WRITE);
					flags.add(FileFlag.WRITE);
				} catch (AccessDeniedException e) {
					if (e.getMessage().endsWith("Permissions does not allow requested access")) {
						lastCause = "Permissions do not allow write access";
					} else {
						lastCause = e.getMessage();
					}
				} catch (FileSystemException e) {
					// A workaround for https://bugs.openjdk.java.net/browse/JDK-8034057
					// and similar bugs, if we can't determine it the NIO way, fall
					// back to actually testing it.
					LOGGER.debug(
						"Couldn't determine write permissions for \"{}\", falling back to write testing. The error was: {}",
						path.toString(),
						e.getMessage()
					);
					if (isFolder()) {
						if (testFolderWritable()) {
							flags.add(FileFlag.WRITE);
						}
					} else {
						if (testFileWritable(path.toFile())) {
							flags.add(FileFlag.WRITE);
						}
					}
				} catch (IOException e) {
					lastCause = e.getMessage();
				}
				writeChecked = true;
			}
			if (!executeChecked && checkExecute) {
				// To conform to the fact that on Linux root always implicit
				// execute permission regardless of explicit permissions
				if (Platform.isLinux() && FileUtil.isAdmin()) {
					flags.add(FileFlag.EXECUTE);
				} else {
					try {
						path.getFileSystem().provider().checkAccess(path, AccessMode.EXECUTE);
						flags.add(FileFlag.EXECUTE);
					} catch (AccessDeniedException e) {
						if (e.getMessage().endsWith("Permissions does not allow requested access")) {
							lastCause = "Permissions do not allow execute access";
						} else {
							lastCause = e.getMessage();
						}
					} catch (IOException e) {
						lastCause = e.getMessage();
					}
				}
				executeChecked = true;
			}
			if (checkBrowse &&
				flags.contains(FileFlag.FOLDER) &&
				flags.contains(FileFlag.READ) &&
				flags.contains(FileFlag.EXECUTE)
			) {
				flags.add(FileFlag.BROWSE);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is a folder, {@code false}
	 *         otherwise.
	 */
	public boolean isFolder() {
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.FOLDER);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is a regular file, {@code false}
	 *         otherwise.
	 */
	public boolean isFile() {
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.FILE);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is readable, {@code false}
	 *         otherwise.
	 */
	public boolean isReadable() {
		checkPermissions(true, false, false);
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.READ);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is writable, {@code false}
	 *         otherwise.
	 */
	public boolean isWritable() {
		checkPermissions(false, true, false);
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.WRITE);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is a file and is executable or is
	 *         a folder and is browsable, {@code false} otherwise.
	 */
	public boolean isExecutable() {
		checkPermissions(false, false, true);
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.EXECUTE);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is a file and is readable and
	 *         executable, {@code false} otherwise.
	 */
	public boolean isExecutableFile() {
		checkPermissions(true, false, true);
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.FILE) && flags.contains(FileFlag.READ) && flags.contains(FileFlag.EXECUTE);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return {@code true} if the file object is a folder and listing of the
	 *         its content is permitted, {@code false} otherwise.
	 */
	public boolean isBrowsable() {
		checkPermissions(true, false, true);
		lock.readLock().lock();
		try {
			return flags.contains(FileFlag.BROWSE);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * Makes sure that the specified file permissions are evaluated and returns
	 * an {@link EnumSet} containing the permissions and properties of the file
	 * object. {@code checkFlag} isn't a filter but a way to make sure that only
	 * the permissions that are needed are evaluated, so that any permissions
	 * that has previously been evaluated will be included even if they aren't
	 * specified.
	 * <p>
	 * If no {@code checkFlag} is specified, all permissions will be evaluated.
	 *
	 * @param checkFlag the {@link FileFlag}s indicating which permissions to check.
	 * @return An {@link EnumSet} containing the permissions for the file object
	 *         that are present and have been evaluated. There is no way to
	 *         differentiate between a permission that isn't evaluated and one
	 *         that isn't granted, in both cases it won't be in the returned
	 *         {@link EnumSet}.
	 */
	@Nonnull
	public EnumSet<FileFlag> getFlags(FileFlag... checkFlag) {
		boolean checkRead = checkFlag.length == 0;
		boolean checkWrite = checkFlag.length == 0;
		boolean checkExecute = checkFlag.length == 0;
		for (FileFlag flag : checkFlag) {
			switch (flag) {
				case BROWSE:
					checkRead = true;
					checkExecute = true;
					break;
				case EXECUTE:
					checkExecute = true;
					break;
				case FILE:
					if (!isFile()) {
						return EnumSet.noneOf(FileFlag.class);
					}
					break;
				case FOLDER:
					if (!isFolder()) {
						return EnumSet.noneOf(FileFlag.class);
					}
					break;
				case READ:
					checkRead = true;
					break;
				case WRITE:
					checkWrite = true;
					break;
				default:
					throw new AssertionError("Flag " + flag.name() + "isn't implemented", null);
			}
		}
		checkPermissions(checkRead, checkWrite, checkExecute);
		lock.readLock().lock();
		try {
			return flags.clone();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return The object this {@link FilePermissions} instance represents as a
	 *         {@link File}.
	 */
	public File getFile() {
		return path.toFile();
	}

	/**
	 * @return The object this {@link FilePermissions} instance represents as a
	 *         {@link Path}.
	 */
	public Path getPath() {
		return path;
	}

	/**
	 * Re-reads file or folder permissions in case they have changed.
	 */
	public void refresh() {
		lock.writeLock().lock();
		try {
			flags.clear();
			readChecked = false;
			writeChecked = false;
			executeChecked = false;
			lastCause = null;
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return The {@link String} containing the last cause why a permission
	 *         isn't available of {@code null} if none exists.
	 */
	@Nullable
	public String getLastCause() {
		lock.readLock().lock();
		try {
			return lastCause;
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public String toString() {
		return toString(false);
	}

	/**
	 * Returns a {@link String} representation of this {@link FilePermissions}
	 * instance in POSIX notation ({@code "drwx"}).
	 *
	 * @param debug if {@code true}, will show the current state without first
	 *            evaluating unresolved permissions. Unknown permissions will be
	 *            shown as {@code "?"}.
	 * @return The POSIX {@link String} representation.
	 */
	public String toString(boolean debug) {
		if (!debug) {
			checkPermissions(true, true, true);
		}
		StringBuilder sb = new StringBuilder();
		lock.readLock().lock();
		try {
			sb.append(flags.contains(FileFlag.FOLDER) ? "d" : "-");
			if (!readChecked) {
				sb.append('?');
			} else {
				sb.append(flags.contains(FileFlag.READ) ? "r" : "-");
			}
			if (!writeChecked) {
				sb.append('?');
			} else {
				sb.append(flags.contains(FileFlag.WRITE) ? "w" : "-");
			}
			if (!executeChecked) {
				sb.append('?');
			} else {
				sb.append(flags.contains(FileFlag.EXECUTE) ? "x" : "-");
			}
		} finally {
			lock.readLock().unlock();
		}
		return sb.toString();
	}

	private boolean testFolderWritable() {
		if (!isFolder()) {
			throw new IllegalStateException("testFolderWriteable can only be called on a folder");
		}
		boolean isWritable = false;

		File file = new File(
			this.path.toFile(),
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
					LOGGER.error("Couldn't delete temporary test file: \"{}\"", file.getAbsolutePath());
					file.deleteOnExit();
				}
			}
		} catch (IOException e) {
			lastCause = e.getMessage();
		}

		return isWritable;
	}

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
						file.deleteOnExit();
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

	/**
	 * Flags used to represent a file object's permission or property.
	 *
	 * @author Nadahar
	 */
	public enum FileFlag {

		/** Read permission */
		READ,

		/** Write permission */
		WRITE,

		/** Execute permission */
		EXECUTE,

		/** Browse permission */
		BROWSE,

		/** Is a file */
		FILE,

		/** Is a folder */
		FOLDER;
	}
}
