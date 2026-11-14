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
package net.pms.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SystemFilesHelperTest {

	@TempDir
	Path tempDir;

	private File create(String name) throws IOException {
		return Files.createFile(tempDir.resolve(name)).toFile();
	}

	@Test
	void findsAlternativeCoverName() throws IOException {
		File cover = create("cover.jpg");
		create("track01.mp3");

		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder", "cover")));
	}

	@Test
	void doesNotFindNameThatIsNotConfigured() throws IOException {
		create("cover.jpg");

		assertNull(SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder")));
	}

	@Test
	void configuredOrderDecidesWhichFileWins() throws IOException {
		File folder = create("folder.png");
		File cover = create("cover.jpg");

		assertEquals(folder, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder", "cover")));
		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("cover", "folder")));
	}

	@Test
	void namesAreMatchedCaseInsensitively() throws IOException {
		File cover = create("Cover.JPG");

		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder", "cover")));
		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("  COVER  ")));
	}

	@Test
	void onlyImageExtensionsAreCandidates() throws IOException {
		create("cover.txt");
		create("cover");

		assertNull(SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("cover")));
	}

	@Test
	void extensionlessAlbumArtFileDoesNotThrow() throws IOException {
		create("albumart");

		assertNull(SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("cover")));
		assertFalse(SystemFilesHelper.isPotentialThumbnail("albumart"));
	}

	@Test
	void nameMustMatchTheWholeBaseName() throws IOException {
		create("discover.jpg");
		create("covers.jpg");

		assertNull(SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("cover")));
	}

	@Test
	void albumArtRuleStillAppliesAfterConfiguredNames() throws IOException {
		File albumArt = create("AlbumArtSmall.jpg");

		assertEquals(albumArt, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder", "cover")));

		File cover = create("cover.jpg");
		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder", "cover")));
	}

	@Test
	void blankNamesAreIgnored() throws IOException {
		create(".jpg");

		assertNull(SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("", "  ")));
	}

	@Test
	void sameNameWithDifferentExtensionsIsDeterministic() throws IOException {
		File jpg = create("folder.jpg");
		create("folder.png");

		assertEquals(jpg, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), List.of("folder")));
	}

	@Test
	void missingOrNonDirectoryReturnsNull() throws IOException {
		File file = create("cover.jpg");

		assertNull(SystemFilesHelper.getFolderThumbnail(null, List.of("cover")));
		assertNull(SystemFilesHelper.getFolderThumbnail(file, List.of("cover")));
		assertNull(SystemFilesHelper.getFolderThumbnail(tempDir.resolve("missing").toFile(), List.of("cover")));
	}

	@Test
	void singleFilenameOverloadIsUnchanged() throws IOException {
		File playlistThumb = create("my playlist.png");
		create("cover.jpg");

		assertEquals(playlistThumb, SystemFilesHelper.getFolderThumbnail(tempDir.toFile(), "my playlist"));
	}

	@Test
	void isFolderThumbnailUsesTheSameNames() throws IOException {
		File cover = create("cover.jpg");
		File folder = create("folder.jpg");
		File other = create("track.jpg");

		List<String> names = List.of("folder", "cover");
		assertTrue(SystemFilesHelper.isFolderThumbnail(cover, true, names));
		assertTrue(SystemFilesHelper.isFolderThumbnail(folder, true, names));
		assertFalse(SystemFilesHelper.isFolderThumbnail(other, true, names));
		assertFalse(SystemFilesHelper.isFolderThumbnail(cover, true, List.of("folder")));
	}

	@Test
	void isFolderThumbnailChecksExtensionOnlyWhenAsked() throws IOException {
		File cover = create("cover.txt");

		assertTrue(SystemFilesHelper.isFolderThumbnail(cover, false, List.of("cover")));
		assertFalse(SystemFilesHelper.isFolderThumbnail(cover, true, List.of("cover")));
	}

	@Test
	void isFolderThumbnailRejectsDirectoriesAndNull() throws IOException {
		File dir = Files.createDirectory(tempDir.resolve("cover.jpg")).toFile();

		assertFalse(SystemFilesHelper.isFolderThumbnail(dir, false, List.of("cover")));
		assertFalse(SystemFilesHelper.isFolderThumbnail(null, false, List.of("cover")));
	}

	@Test
	void priorityFollowsConfiguredOrder() {
		List<String> names = List.of("cover", "folder");

		assertEquals(0, SystemFilesHelper.getFolderThumbnailPriority("cover.jpg", names));
		assertEquals(1, SystemFilesHelper.getFolderThumbnailPriority("Folder.PNG", names));
		assertEquals(2, SystemFilesHelper.getFolderThumbnailPriority("AlbumArt_{ABC}.jpg", names));
		assertEquals(-1, SystemFilesHelper.getFolderThumbnailPriority("track01.jpg", names));
	}
}
