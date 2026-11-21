package net.pms.store.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.pms.store.SystemFilesHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VirtualFolderThumbnailTest {

	@Test
	void additionalFolderCoversAreOptIn(@TempDir Path directory) throws IOException {
		File cover = Files.createFile(directory.resolve("Cover.JPG")).toFile();
		Files.createFile(directory.resolve("cover.jpg.bak"));
		Files.createFile(directory.resolve("cover.extra.png"));
		Files.createDirectory(directory.resolve("cover.gif"));
		assertNull(SystemFilesHelper.getFolderThumbnail(directory.toFile(), List.of()));
		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(directory.toFile(), List.of("cover")));

		Files.delete(cover.toPath());
		assertNull(SystemFilesHelper.getFolderThumbnail(directory.toFile(), List.of("cover")));
	}

	@Test
	void prefersFolderThenConfiguredNamesThenAlbumArt(@TempDir Path directory) throws IOException {
		File albumArt = Files.createFile(directory.resolve("AlbumArtSmall.png")).toFile();
		File cover = Files.createFile(directory.resolve("cover.png")).toFile();
		File front = Files.createFile(directory.resolve("front.jpg")).toFile();
		File folder = Files.createFile(directory.resolve("folder.jpg")).toFile();
		List<String> names = List.of("front", "cover");

		assertEquals(folder, SystemFilesHelper.getFolderThumbnail(directory.toFile(), names));
		Files.delete(folder.toPath());
		assertEquals(front, SystemFilesHelper.getFolderThumbnail(directory.toFile(), names));
		assertEquals(cover, SystemFilesHelper.getFolderThumbnail(directory.toFile(), List.of("cover", "front")));
		assertEquals(albumArt, SystemFilesHelper.getFolderThumbnail(directory.toFile(), List.of()));
		Files.delete(front.toPath());
		Files.delete(cover.toPath());
		assertEquals(albumArt, SystemFilesHelper.getFolderThumbnail(directory.toFile(), names));
	}

	@Test
	void preservesOrderAndUnrelatedImages() {
		File video = new File("media/movie.mkv");
		File cover = new File("media/movie.mkv.cover.png");
		File poster = new File("media/movie.jpg");
		File photo = new File("media/holiday.jpg");
		File otherFolder = new File("other/movie.jpg");
		List<File> files = new ArrayList<>(List.of(photo, cover, video, poster, otherFolder));
		VirtualFolder.removeFileThumbnails(files, new HashSet<>(List.of(cover, poster, photo, otherFolder)), Set.of(video));
		assertEquals(List.of(photo, video, otherFolder), files);
	}

	@Test
	void preservesDuplicateBehaviorForOverlappingFolders() {
		File video = new File("media/movie.mkv");
		File audio = new File("media/movie.mp3");
		File image = new File("media/movie.jpg");
		List<File> files = new ArrayList<>(List.of(image, video, image, audio));
		VirtualFolder.removeFileThumbnails(files, new HashSet<>(Set.of(image)), Set.of(video, audio));
		assertEquals(List.of(video, image, audio), files);
	}

	@Test
	void leavesFilesWithoutMatchesUnchanged() {
		File video = new File("media/movie.mkv");
		File photo = new File("media/holiday.jpg");
		List<File> original = List.of(video, photo);
		for (Set<File> images : List.of(Set.<File>of(), Set.of(photo))) {
			for (Set<File> media : List.of(Set.<File>of(), Set.of(video))) {
				List<File> files = new ArrayList<>(original);
				VirtualFolder.removeFileThumbnails(files, new HashSet<>(images), media);
				assertEquals(original, files);
			}
		}
	}

	@Test
	void matchesPreviousAlgorithmForMixedAndRepeatedPaths() {
		Random random = new Random(16);
		for (int round = 0; round < 100; round++) {
			List<File> files = new ArrayList<>();
			Set<File> images = new HashSet<>();
			Set<File> media = new HashSet<>();
			for (int i = 0; i < 200; i++) {
				String path = "folder" + random.nextInt(3) + "/file" + random.nextInt(20);
				boolean image = random.nextBoolean();
				File file = new File(path + (image ? (random.nextBoolean() ? ".jpg" : ".mkv.cover.png") : ".mkv"));
				files.add(file);
				(image ? images : media).add(file);
			}
			List<File> expected = new ArrayList<>(files);
			Set<File> expectedImages = new HashSet<>(images);
			removePreviously(expected, expectedImages, media);
			VirtualFolder.removeFileThumbnails(files, images, media);
			assertEquals(expected, files, "round " + round);
			assertEquals(expectedImages, images, "remaining images in round " + round);
		}
	}

	private static void removePreviously(List<File> files, Set<File> images, Set<File> media) {
		for (File video : media) {
			Set<File> candidates = SystemFilesHelper.getPotentialFileThumbnails(video, false);
			Iterator<File> iterator = images.iterator();
			while (iterator.hasNext()) {
				File image = iterator.next();
				if (candidates.contains(image)) {
					iterator.remove();
					files.remove(image);
				}
			}
		}
	}
}
