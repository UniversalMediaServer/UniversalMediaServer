package net.pms.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.pms.util.RelativeMediaPath.Relative;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RelativeMediaPathTest {

	@TempDir
	private File tempDir;

	@Test
	public void testRelativizeUsesTheDeepestSharedFolder() {
		File shallow = new File(tempDir, "media");
		File deep = new File(shallow, "music");
		List<File> roots = List.of(shallow, deep);

		Relative relative = RelativeMediaPath.relativize(new File(deep, "ABBA/Gold/01.flac").getAbsolutePath(), roots);

		assertNotNull(relative);
		assertEquals(1, relative.rootIndex());
		assertEquals("ABBA/Gold/01.flac", relative.path());
	}

	@Test
	public void testRelativizeSharedFolderItself() {
		File root = new File(tempDir, "music");

		Relative relative = RelativeMediaPath.relativize(root.getAbsolutePath() + File.separator, List.of(root));

		assertNotNull(relative);
		assertEquals(RelativeMediaPath.ROOT_PATH, relative.path());
	}

	@Test
	public void testRelativizeOutsideOfEverySharedFolder() {
		assertNull(RelativeMediaPath.relativize(new File(tempDir, "elsewhere/01.flac").getAbsolutePath(), List.of(new File(tempDir, "music"))));
		assertNull(RelativeMediaPath.relativize("http://example.com/stream.mp3", List.of(new File(tempDir, "music"))));
		assertNull(RelativeMediaPath.relativize(new File(tempDir, "musicvideos/01.mkv").getAbsolutePath(), List.of(new File(tempDir, "music"))));
	}

	@Test
	public void testResolveBelowAnotherMountPoint() throws IOException {
		//the library was shared here at backup time, and is shared there now
		File here = createTree("mnt/music");
		File there = createTree("export/music");

		List<String> resolved = RelativeMediaPath.resolve(here.getAbsolutePath(), "ABBA/Gold/01.flac", List.of(there));

		assertEquals(1, resolved.size());
		assertEquals(ProcessUtil.getSystemPathName(new File(there, "ABBA/Gold/01.flac").getAbsolutePath()), resolved.get(0));
	}

	@Test
	public void testResolvePrefersTheRecordedSharedFolder() throws IOException {
		File recorded = createTree("mnt/music");
		File other = createTree("backup/music");

		List<String> resolved = RelativeMediaPath.resolve(recorded.getAbsolutePath(), "ABBA/Gold", List.of(other, recorded));

		assertEquals(1, resolved.size());
		assertEquals(ProcessUtil.getSystemPathName(new File(recorded, "ABBA/Gold").getAbsolutePath()), resolved.get(0));
	}

	@Test
	public void testResolvePrefersASharedFolderOfTheSameName() throws IOException {
		File recorded = new File(tempDir, "mnt/music");
		File sameName = createTree("export/music");
		File otherName = createTree("export/copy");

		List<String> resolved = RelativeMediaPath.resolve(recorded.getAbsolutePath(), "ABBA/Gold/01.flac", List.of(otherName, sameName));

		assertEquals(1, resolved.size());
		assertEquals(ProcessUtil.getSystemPathName(new File(sameName, "ABBA/Gold/01.flac").getAbsolutePath()), resolved.get(0));
	}

	@Test
	public void testResolveReturnsEverySharedFolderOfTheWinningTier() throws IOException {
		File one = createTree("export/one");
		File two = createTree("export/two");

		List<String> resolved = RelativeMediaPath.resolve(new File(tempDir, "mnt/music").getAbsolutePath(), "ABBA/Gold/01.flac", List.of(one, two));

		assertEquals(2, resolved.size());
	}

	@Test
	public void testResolveSkipsWhatDoesNotExist() throws IOException {
		File root = createTree("export/music");

		assertTrue(RelativeMediaPath.resolve(root.getAbsolutePath(), "ABBA/Gold/02.flac", List.of(root)).isEmpty());
		assertTrue(RelativeMediaPath.resolve(root.getAbsolutePath(), "ABBA/Gold/01.flac", new ArrayList<>()).isEmpty());
	}

	@Test
	public void testResolveSharedFolderItself() throws IOException {
		File root = createTree("export/music");

		List<String> resolved = RelativeMediaPath.resolve(null, RelativeMediaPath.ROOT_PATH, List.of(root));

		assertEquals(1, resolved.size());
		assertEquals(ProcessUtil.getSystemPathName(root.getAbsolutePath()), resolved.get(0));
	}

	@Test
	public void testIsFileSystemPath() {
		assertTrue(RelativeMediaPath.isFileSystemPath(tempDir.getAbsolutePath()));
		assertTrue(!RelativeMediaPath.isFileSystemPath("http://example.com/stream.mp3"));
		assertTrue(!RelativeMediaPath.isFileSystemPath("$DBID$MUSICBRAINZ_RECORDID$1234"));
		assertTrue(!RelativeMediaPath.isFileSystemPath(null));
		assertTrue(!RelativeMediaPath.isFileSystemPath(""));
	}

	/**
	 * Creates a shared folder holding ABBA/Gold/01.flac.
	 *
	 * @return the shared folder
	 */
	private File createTree(String sharedFolder) throws IOException {
		File root = new File(tempDir, sharedFolder);
		File album = new File(root, "ABBA/Gold");
		Files.createDirectories(album.toPath());
		Files.createFile(new File(album, "01.flac").toPath());
		return root;
	}

}
