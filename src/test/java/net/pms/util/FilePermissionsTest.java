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
package net.pms.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class FilePermissionsTest {
	private final Class<?> CLASS = FilePermissionsTest.class;

	@Test
	public void throwsIllegalArgumentExceptionIfFileIsNull() throws FileNotFoundException {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			File file = null;
			new FilePermissions(file);
		}, "IllegalArgumentException was expected");
		assertEquals("file cannot be null", thrown.getMessage());
	}

	public void throwsIllegalArgumentExceptionIfPathIsNull() throws FileNotFoundException {
		IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
			Path path = null;
			new FilePermissions(path);
		}, "IllegalArgumentException was expected");
		assertEquals("File argument cannot be null", thrown.getMessage());
	}

	@Test
	public void throwsFileNotFoundExceptionIfFileDoesNotExist() throws FileNotFoundException {
		assertThrows(FileNotFoundException.class, () -> {
			new FilePermissions(new File("No such file"));
		}, "FileNotFoundException was expected");
	}

	@Test
	public void throwsFileNotFoundExceptionIfPathDoesNotExist() throws FileNotFoundException {
		assertThrows(FileNotFoundException.class, () -> {
			new FilePermissions(Paths.get("No such file"));
		}, "FileNotFoundException was expected");
	}

	@Test
	public void testFilePermissions() throws URISyntaxException, IOException {
		FilePermissions permissions = new FilePermissions(new File(""));
		assertTrue(permissions.isFolder(), "CurrentFolderIsFolder");
		assertTrue(permissions.isReadable(), "CurrentFolderIsReadable");
		assertTrue(permissions.isBrowsable(), "CurrentFolderIsBrowsable");

		permissions = new FilePermissions(Paths.get("").toAbsolutePath());
		assertTrue(permissions.isFolder(), "CurrentFolderIsFolder");
		assertTrue(permissions.isReadable(), "CurrentFolderIsReadable");
		assertTrue(permissions.isBrowsable(), "CurrentFolderIsBrowsable");

		permissions = new FilePermissions(FileUtils.toFile(CLASS.getResource("prettified_filenames_metadata.json")));
		assertTrue(permissions.isReadable(), "FileIsReadable");
		assertTrue(permissions.isWritable(), "FileIsWritable");
		assertFalse(permissions.isFolder(), "FileIsNotFolder");
		assertFalse(permissions.isBrowsable(), "FileIsNotBrowsable");

		permissions = new FilePermissions(permissions.getFile().getParentFile());
		assertTrue(permissions.isFolder(), "ParentIsFolder");
		assertTrue(permissions.isBrowsable(), "ParentIsBrowsable");

		permissions = new FilePermissions(Paths.get(CLASS.getResource("prettified_filenames_metadata.json").toURI()));
		assertTrue(permissions.isReadable(), "FileIsReadable");
		assertTrue(permissions.isWritable(), "FileIsWritable");
		assertFalse(permissions.isFolder(), "FileIsNotFolder");
		assertFalse(permissions.isBrowsable(), "FileIsNotBrowsable");

		permissions = new FilePermissions(permissions.getPath().getParent());
		assertTrue(permissions.isFolder(), "ParentIsFolder");
		assertTrue(permissions.isBrowsable(), "ParentIsBrowsable");

		File file = new File(System.getProperty("java.io.tmpdir"), String.format("UMS_temp_writable_file_%d.tmp", new Random().nextInt(10000)));
		if (file.createNewFile()) {
			try {
				assertTrue(new FilePermissions(file).isReadable(), "TempFileIsReadable");
				assertTrue(new FilePermissions(file).isWritable(), "TempFileIsWritable");
				assertFalse(new FilePermissions(file).isFolder(), "TempFileIsNotFolder");
				assertFalse(new FilePermissions(file).isBrowsable(), "TempFileIsNotBrowsable");
			} finally {
				file.delete();
			}
		}

		Path path = Paths.get(System.getProperty("java.io.tmpdir"), String.format("UMS_temp_writable_file_%d.tmp", new Random().nextInt(10000)));
		Files.createFile(path);
		try {
			assertTrue(new FilePermissions(path).isReadable(), "TempFileIsReadable");
			assertTrue(new FilePermissions(path).isWritable(), "TempFileIsWritable");
			assertFalse(new FilePermissions(path).isFolder(), "TempFileIsNotFolder");
			assertFalse(new FilePermissions(path).isBrowsable(), "TempFileIsNotBrowsable");
		} finally {
			Files.delete(path);
		}
	}

}
