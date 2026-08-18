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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ResourceIdentifierTest {
	private static final Class<?> CLASS = ResourceIdentifierTest.class;

	/**
	 * Bigger than the threshold up to which a file is hashed completely, which is
	 * 5 segments of 256 kB.
	 */
	private static final int BIG = 1024 * 1024 * 2;

	/**
	 * An offset inside the gap between the first and the second segment of a file of
	 * {@link #BIG} bytes: the first segment ends at 256 kB, the second one starts at
	 * (BIG - 256 kB) / 4 = 448 kB.
	 */
	private static final int UNREAD_OFFSET = 1024 * 300;

	@TempDir
	private Path tempDir;

	public void testResourceIdentifier(String test, String uri, String expected) {
		String actual = ResourceIdentifier.getResourceIdentifier(uri);
		assertEquals(expected, actual, test);
	}

	public static File getTestFile(String testFile) {
		return FileUtils.toFile(CLASS.getResource(testFile));
	}

	@Test
	public void testResourceIdentifiers() {
		//test with file
		File file = getTestFile("/net/pms/parsers/video-h264-aac.mp4");
		String filePath = file.getAbsolutePath();
		testResourceIdentifier("file: " + filePath, file.getAbsolutePath(), "bd153798588a561");
		//test with file url
		try {
			String fileUrl = file.toURI().toURL().toString();
			testResourceIdentifier("file url: " + fileUrl, fileUrl, "bd153798588a561");
		} catch (MalformedURLException ex) {
			// Can't happen
		}
		//test with url
		testResourceIdentifier("url", "http://test.me", "60cb8b08c493ec5b");
		//test with text
		testResourceIdentifier("text", "something", "97a313603bc96153");
		//test with null
		testResourceIdentifier("null", null, null);
	}

	@Test
	public void test2GBFileIdentifierPerformance() throws Exception {
		File tempFile = File.createTempFile("ums-1gb-", ".bin");
		tempFile.deleteOnExit();
		long targetSize = Integer.MAX_VALUE;

		try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
			raf.setLength(targetSize);
		}

		for (int i = 0; i < 5; i++) {
			long startNs = System.nanoTime();
			String ruid = ResourceIdentifier.getResourceIdentifier(tempFile.getAbsolutePath());
			long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
			System.out.println("ResourceIdentifier 2GiB hash: " + ruid + " in " + elapsedMs + " ms");

			assertNotNull(ruid);
			assertFalse(ruid.isEmpty());
		}

	}

	@Test
	public void test4GBFileIdentifierPerformance() throws Exception {
		File tempFile = File.createTempFile("ums-4gb-", ".bin");
		tempFile.deleteOnExit();
		long targetSize = 4L * 1024 * 1024 * 1024; // 4 GiB

		try (RandomAccessFile raf = new RandomAccessFile(tempFile, "rw")) {
			raf.setLength(targetSize);
		}

		for (int i = 0; i < 5; i++) {
			long startNs = System.nanoTime();
			String ruid = ResourceIdentifier.getResourceIdentifier(tempFile.getAbsolutePath());
			long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
			System.out.println("ResourceIdentifier 4GiB hash: " + ruid + " in " + elapsedMs + " ms");

			assertNotNull(ruid);
			assertFalse(ruid.isEmpty());
		}
	}

	private File write(String name, byte[] content) throws IOException {
		Path path = tempDir.resolve(name);
		Files.write(path, content);
		return path.toFile();
	}

	private static byte[] content(int size, byte fill) {
		byte[] bytes = new byte[size];
		Arrays.fill(bytes, fill);
		return bytes;
	}

	private static String ruid(File file) {
		return ResourceIdentifier.getResourceIdentifier(file.getAbsolutePath());
	}

	@Test
	public void testSmallFileUsesWholeContent() throws IOException {
		byte[] content = content(1000, (byte) 7);
		File a = write("a.bin", content);
		content[500] = (byte) 9;
		File b = write("b.bin", content);

		assertNotNull(ruid(a));
		assertEquals(ruid(a), ruid(a));
		assertNotEquals(ruid(a), ruid(b), "a changed byte must change the identifier of a small file");
	}

	@Test
	public void testBigFileUsesSizeAndSegments() throws IOException {
		String expected = ruid(write("original.bin", content(BIG, (byte) 7)));
		assertNotNull(expected);

		byte[] changedHead = content(BIG, (byte) 7);
		changedHead[0] = (byte) 9;
		assertNotEquals(expected, ruid(write("head.bin", changedHead)), "a changed first byte must change the identifier");

		byte[] changedTail = content(BIG, (byte) 7);
		changedTail[BIG - 1] = (byte) 9;
		assertNotEquals(expected, ruid(write("tail.bin", changedTail)), "a changed last byte must change the identifier");

		assertNotEquals(expected, ruid(write("size.bin", content(BIG + 1, (byte) 7))), "a changed size must change the identifier");

		byte[] changedInside = content(BIG, (byte) 7);
		changedInside[BIG / 2] = (byte) 9;
		assertNotEquals(expected, ruid(write("inside.bin", changedInside)), "a changed byte inside a segment must change the identifier");

		// documents the trade-off: a big file is not read completely
		byte[] changedGap = content(BIG, (byte) 7);
		changedGap[UNREAD_OFFSET] = (byte) 9;
		assertEquals(expected, ruid(write("gap.bin", changedGap)), "bytes between two segments are not part of the identifier");
	}

	@Test
	public void testEmptyFileHasIdentifier() throws IOException {
		File empty = write("empty.bin", new byte[0]);
		assertNotNull(ruid(empty));
		assertEquals(ruid(empty), ruid(empty));
	}
}
