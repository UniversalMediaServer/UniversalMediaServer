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
package net.pms.platform.linux;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import net.pms.util.FileUtil;

/**
 * A simple type holding mount point information for Unix file systems.
 *
 * @author Nadahar
 */
public class UnixMountPoint {
	private final String device;
	private final String folder;

	public UnixMountPoint(String device, String folder) {
		this.device = device;
		this.folder = folder;
	}

	public String getDevice() {
		return device;
	}

	public String getFolder() {
		return folder;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof UnixMountPoint)) {
			return false;
		}

		return
			device.equals(((UnixMountPoint) obj).device) &&
			folder.equals(((UnixMountPoint) obj).folder);
	}

	@Override
	public int hashCode() {
		return device.hashCode() + folder.hashCode();
	}

	@Override
	public String toString() {
		return String.format("Device: \"%s\", folder: \"%s\"", device, folder);
	}

	/**
	 * Finds the {@link UnixMountPoint} for a {@link java.nio.file.Path} given
	 * that the file resides on a Unix file system.
	 *
	 * @param path the {@link java.nio.file.Path} for which to find the Unix
	 *            mount point.
	 * @return The {@link UnixMountPoint} for the given path.
	 *
	 * @throws InvalidFileSystemException
	 */
	public static UnixMountPoint getMountPoint(Path path) throws FileUtil.InvalidFileSystemException {
		FileStore store;
		try {
			store = Files.getFileStore(path);
		} catch (IOException e) {
			throw new FileUtil.InvalidFileSystemException(
				String.format("Could not get Unix mount point for file \"%s\": %s", path.toAbsolutePath(), e.getMessage()),
				e
			);
		}

		try {
			Field entryField = store.getClass().getSuperclass().getDeclaredField("entry");
			Field nameField = entryField.getType().getDeclaredField("name");
			Field dirField = entryField.getType().getDeclaredField("dir");
			entryField.setAccessible(true);
			nameField.setAccessible(true);
			dirField.setAccessible(true);
			String device = new String((byte[]) nameField.get(entryField.get(store)), StandardCharsets.UTF_8);
			String folder = new String((byte[]) dirField.get(entryField.get(store)), StandardCharsets.UTF_8);
			return new UnixMountPoint(device, folder);
		} catch (NoSuchFieldException e) {
			throw new FileUtil.InvalidFileSystemException(String.format("File \"%s\" is not on a Unix file system", path.isAbsolute()), e);
		} catch (SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new FileUtil.InvalidFileSystemException(
				String.format(
					"An error occurred while trying to find mount point for file \"%s\": %s",
					path.toAbsolutePath(),
					e.getMessage()
				),
				e
			);
		}
	}

	/**
	 * Finds the {@link UnixMountPoint} for a {@link java.io.File} given that
	 * the file resides on a Unix file system.
	 *
	 * @param file the {@link java.io.File} for which to find the Unix mount
	 *            point.
	 * @return The {@link UnixMountPoint} for the given path.
	 *
	 * @throws InvalidFileSystemException
	 */
	public static UnixMountPoint getMountPoint(File file) throws FileUtil.InvalidFileSystemException {
		return getMountPoint(file.toPath());
	}

}
