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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class FilePermissionsTest {
	private final Class<?> CLASS = FilePermissionsTest.class;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void throwsIllegalArgumentExceptionIfFileIsNull() throws FileNotFoundException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("File argument cannot be null");
		File file = null;
		new FilePermissions(file);
	}

	public void throwsIllegalArgumentExceptionIfPathIsNull() throws FileNotFoundException {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("File argument cannot be null");
		Path path = null;
		new FilePermissions(path);
	}

	@Test
	public void throwsFileNotFoundExceptionIfFileDoesNotExist() throws FileNotFoundException {
		exception.expect(FileNotFoundException.class);
		new FilePermissions(new File("No such file"));
	}

	@Test
	public void throwsFileNotFoundExceptionIfPathDoesNotExist() throws FileNotFoundException {
		exception.expect(FileNotFoundException.class);
		new FilePermissions(Paths.get("No such file"));
	}

	@Test
	public void testFilePermissions() throws URISyntaxException, IOException {
		FilePermissions permissions = new FilePermissions(new File(""));
		assertTrue("CurrentFolderIsFolder", permissions.isFolder());
		assertTrue("CurrentFolderIsReadable", permissions.isReadable());
		assertTrue("CurrentFolderIsBrowsable", permissions.isBrowsable());

		permissions = new FilePermissions(Paths.get("").toAbsolutePath());
		assertTrue("CurrentFolderIsFolder", permissions.isFolder());
		assertTrue("CurrentFolderIsReadable", permissions.isReadable());
		assertTrue("CurrentFolderIsBrowsable", permissions.isBrowsable());

		permissions = new FilePermissions(FileUtils.toFile(CLASS.getResource("english-utf8-with-bom.srt")));
		assertTrue("FileIsReadable", permissions.isReadable());
		assertTrue("FileIsWritable", permissions.isWritable());
		assertFalse("FileIsNotFolder", permissions.isFolder());
		assertFalse("FileIsNotBrowsable", permissions.isBrowsable());

		permissions = new FilePermissions(permissions.getFile().getParentFile());
		assertTrue("ParentIsFolder", permissions.isFolder());
		assertTrue("ParentIsBrowsable", permissions.isBrowsable());

		permissions = new FilePermissions(Paths.get(CLASS.getResource("english-utf8-with-bom.srt").toURI()));
		assertTrue("FileIsReadable", permissions.isReadable());
		assertTrue("FileIsWritable", permissions.isWritable());
		assertFalse("FileIsNotFolder", permissions.isFolder());
		assertFalse("FileIsNotBrowsable", permissions.isBrowsable());

		permissions = new FilePermissions(permissions.getPath().getParent());
		assertTrue("ParentIsFolder", permissions.isFolder());
		assertTrue("ParentIsBrowsable", permissions.isBrowsable());

		File file = new File(System.getProperty("java.io.tmpdir"), String.format("UMS_temp_writable_file_%d.tmp", new Random().nextInt(10000)));
		if (file.createNewFile()) {
			try {
				assertTrue("TempFileIsReadable", new FilePermissions(file).isReadable());
				assertTrue("TempFileIsWritable", new FilePermissions(file).isWritable());
				assertFalse("TempFileIsNotFolder", new FilePermissions(file).isFolder());
				assertFalse("TempFileIsNotBrowsable", new FilePermissions(file).isBrowsable());
			} finally {
				file.delete();
			}
		}

		Path path = Paths.get(System.getProperty("java.io.tmpdir"), String.format("UMS_temp_writable_file_%d.tmp", new Random().nextInt(10000)));
		Files.createFile(path);
		try {
			assertTrue("TempFileIsReadable", new FilePermissions(path).isReadable());
			assertTrue("TempFileIsWritable", new FilePermissions(path).isWritable());
			assertFalse("TempFileIsNotFolder", new FilePermissions(path).isFolder());
			assertFalse("TempFileIsNotBrowsable", new FilePermissions(path).isBrowsable());
		} finally {
			Files.delete(path);
		}
	}

}
