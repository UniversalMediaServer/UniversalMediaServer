package net.pms.network.mediaserver.jupnp.support.umsservice.impl;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Comparator;
import java.util.Properties;
import java.util.stream.Stream;
import net.pms.PMS;
import net.pms.TestHelper;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableResourceRatings;
import net.pms.media.MediaInfo;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreResourceRatings;
import net.pms.store.item.RealFile;
import net.pms.util.ProcessUtil;
import net.pms.util.ResourceIdentifier;
import org.apache.commons.configuration2.ex.ConfigurationException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RatingBackupManagerTest {

	@TempDir
	private File tempDir;

	@BeforeEach
	public final void setUp() throws ConfigurationException, InterruptedException {
		TestHelper.setLoggingOff();
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	/*
	 * a backup that cannot be written must not report success
	 */
	@Test
	public void testFailedWriteIsReported() throws Exception {
		MediaDatabase.init();
		File backupDir = new File(UmsConfiguration.getProfileDirectory(), "database_backup");
		assertTrue(backupDir.exists() || backupDir.mkdirs());
		File blocker = new File(backupDir, "ratings_backup");
		Files.deleteIfExists(blocker.toPath());
		// A directory in place of the file prevents the atomic replacement.
		assertTrue(blocker.mkdir());
		try {
			assertThrows(UncheckedIOException.class, RatingBackupManager::backupRatings);
			assertTrue(blocker.isDirectory());
			try (var files = Files.list(backupDir.toPath())) {
				assertTrue(files.noneMatch(file -> file.getFileName().toString().startsWith("ratings_backup-")));
			}
		} finally {
			Files.delete(blocker.toPath());
		}
	}

	@Test
	public void testPendingBackupExportsTheLatestRatingAndRuid() throws Exception {
		MediaDatabase.init();
		File track = new File(tempDir, "rated.flac");
		try (var source = getClass().getResourceAsStream("/net/pms/parsers/audio-flac24.flac")) {
			Files.copy(source, track.toPath());
		}
		String filename = key(track);
		String originalRuid = ResourceIdentifier.getResourceIdentifier(filename);
		assertNotNull(originalRuid);
		RealFile resource = new RealFile(null, track);
		MediaInfo media = new MediaInfo();
		media.setResourceId(originalRuid);
		media.setAudioMetadata(new MediaAudioMetadata());
		resource.setMediaInfo(media);
		boolean originalTagSetting = PMS.getConfiguration().isAudioUpdateTag();
		try (Connection connection = MediaDatabase.get().getConnection()) {
			addFile(connection, filename, originalRuid);
			MediaTableResourceRatings.setRating(connection, filename, "RealFile", 1);
			RatingBackupManager.backupRatings();
			assertEquals("1", readBackup().getProperty("content:RealFile:" + originalRuid));

			PMS.getConfiguration().setAudioUpdateTag(true);
			resource.setRating(3);
			resource.setRating(5);

			String newRuid = media.getResourceId();
			assertNotNull(newRuid);
			assertNotEquals(originalRuid, newRuid);
			assertEquals(ResourceIdentifier.getResourceIdentifier(filename), newRuid);
			assertEquals(newRuid, MediaTableFiles.getResourceUidForFilename(connection, filename));
			assertEquals("1", readBackup().getProperty("resource:RealFile:" + filename));

			RatingBackupManager.flushPendingBackup();
			Properties backup = readBackup();
			assertEquals("5", backup.getProperty("content:RealFile:" + newRuid));
			assertEquals("5", backup.getProperty("resource:RealFile:" + filename));
			assertNull(backup.getProperty("content:RealFile:" + originalRuid));
			assertEquals("1", readBackup("ratings_backup.1").getProperty("content:RealFile:" + originalRuid));

			resource.setRating(null);
			RatingBackupManager.flushPendingBackup();
			assertNull(readBackup().getProperty("resource:RealFile:" + filename));
			assertNull(readBackup().getProperty("content:RealFile:" + newRuid));
		} finally {
			PMS.getConfiguration().setAudioUpdateTag(originalTagSetting);
			try (Connection connection = MediaDatabase.get().getConnection()) {
				MediaTableResourceRatings.setRating(connection, filename, "RealFile", null);
				removeFile(connection, filename);
			}
			StoreResourceRatings.clearCache();
			RatingBackupManager.flushPendingBackup();
		}
	}

	private static Properties readBackup() throws IOException {
		return readBackup("ratings_backup");
	}

	private static Properties readBackup(String filename) throws IOException {
		Properties backup = new Properties();
		Path path = Path.of(UmsConfiguration.getProfileDirectory(), "database_backup", filename);
		try (var input = Files.newInputStream(path)) {
			backup.load(input);
		}
		return backup;
	}

	@Test
	public void testKeepsTheSevenLatestBackups() throws Exception {
		MediaDatabase.init();
		Path backupDir = Path.of(UmsConfiguration.getProfileDirectory(), "database_backup");
		String objectType = "WebStream";
		String prefix = "https://example.org/backup-generation-";
		try (Connection connection = MediaDatabase.get().getConnection()) {
			for (int generation = 0; generation < 8; generation++) {
				String key = prefix + generation;
				MediaTableResourceRatings.setRating(connection, key, objectType, 5);
				RatingBackupManager.backupRatings();
				MediaTableResourceRatings.setRating(connection, key, objectType, null);
			}
			assertEquals("5", readBackup().getProperty("resource:" + objectType + ":" + prefix + 7));
			for (int generation = 1; generation <= 6; generation++) {
				Properties backup = readBackup("ratings_backup." + generation);
				assertEquals("5", backup.getProperty("resource:" + objectType + ":" + prefix + (7 - generation)));
				assertNull(backup.getProperty("resource:" + objectType + ":" + prefix + 0));
			}
			try (var files = Files.list(backupDir)) {
				assertEquals(7, files.filter(file -> file.getFileName().toString().startsWith("ratings_backup")).count());
			}
		}
	}

	/**
	 * The media library was shared as "mnt/music" when the ratings were backed up and is shared as "export/music" when they
	 * are restored : the ratings of a file and of a folder have to end up on the new paths.
	 */
	@Test
	public void testRatingsAreRestoredBelowAnotherMountPoint() throws Exception {
		MediaDatabase.init();
		SharedContentArray previouslyShared = SharedContentConfiguration.getSharedContentArray();
		try (Connection connection = MediaDatabase.get().getConnection()) {
			File oldRoot = createTree("mnt/music");
			File oldTrack = new File(oldRoot, "ABBA/Gold/01.flac");
			File oldAlbum = new File(oldRoot, "ABBA/Gold");
			share(oldRoot);
			MediaTableResourceRatings.setRating(connection, key(oldTrack), "RealFile", 5);
			MediaTableResourceRatings.setRating(connection, key(oldAlbum), "RealFolder", 4);

			RatingBackupManager.backupRatings();

			//the library is mounted somewhere else, and the ratings are gone
			MediaTableResourceRatings.setRating(connection, key(oldTrack), "RealFile", null);
			MediaTableResourceRatings.setRating(connection, key(oldAlbum), "RealFolder", null);
			deleteRecursively(oldRoot);
			File newRoot = createTree("export/music");
			share(newRoot);

			long updateIdBeforeRestore = MediaStoreIds.getSystemUpdateId().getValue();
			RatingBackupManager.restoreRating();

			assertNotEquals(updateIdBeforeRestore, MediaStoreIds.getSystemUpdateId().getValue());
			assertEquals(Integer.valueOf(5), MediaTableResourceRatings.getRating(connection, key(new File(newRoot, "ABBA/Gold/01.flac"))));
			assertEquals(Integer.valueOf(4), MediaTableResourceRatings.getRating(connection, key(new File(newRoot, "ABBA/Gold"))));
			//the path of the old mount point is gone and must not be restored
			assertNull(MediaTableResourceRatings.getRating(connection, key(oldTrack)));
			assertNull(MediaTableResourceRatings.getRating(connection, key(oldAlbum)));
		} finally {
			SharedContentConfiguration.updateSharedContent(previouslyShared, false);
		}
	}

	/**
	 * An unchanged setup has to be restored exactly as it was stored, whatever else is shared.
	 */
	@Test
	public void testRatingsAreRestoredOnTheirOwnPath() throws Exception {
		MediaDatabase.init();
		SharedContentArray previouslyShared = SharedContentConfiguration.getSharedContentArray();
		try (Connection connection = MediaDatabase.get().getConnection()) {
			File root = createTree("mnt/music");
			File copy = createTree("backup/music");
			File track = new File(root, "ABBA/Gold/01.flac");
			share(root, copy);
			MediaTableResourceRatings.setRating(connection, key(track), "RealFile", 3);

			RatingBackupManager.backupRatings();
			MediaTableResourceRatings.setRating(connection, key(track), "RealFile", null);
			RatingBackupManager.restoreRating();

			assertEquals(Integer.valueOf(3), MediaTableResourceRatings.getRating(connection, key(track)));
			//the copy holds the same relative path, but the stored path still exists
			assertNull(MediaTableResourceRatings.getRating(connection, key(new File(copy, "ABBA/Gold/01.flac"))));
		} finally {
			SharedContentConfiguration.updateSharedContent(previouslyShared, false);
		}
	}

	/**
	 * The file was moved inside the library: match by RUID.
	 */
	@Test
	public void testRatingIsRestoredAfterTheFileWasMoved() throws Exception {
		MediaDatabase.init();
		SharedContentArray previouslyShared = SharedContentConfiguration.getSharedContentArray();
		String resourceUid = "ruid-testRatingIsRestoredAfterTheFileWasMoved";
		File root = createTree("mnt/music");
		File oldTrack = new File(root, "ABBA/Gold/01.flac");
		File newTrack = new File(root, "ABBA/Gold Remastered/01.flac");
		try (Connection connection = MediaDatabase.get().getConnection()) {
			share(root);
			addFile(connection, key(oldTrack), resourceUid);
			MediaTableResourceRatings.setRating(connection, key(oldTrack), "RealFile", 2);

			RatingBackupManager.backupRatings();

			//the file was moved and rescanned, so the database knows it on its new
			//path only, and the rating of the old path is gone
			MediaTableResourceRatings.setRating(connection, key(oldTrack), "RealFile", null);
			Files.createDirectories(newTrack.getParentFile().toPath());
			Files.move(oldTrack.toPath(), newTrack.toPath());
			removeFile(connection, key(oldTrack));
			addFile(connection, key(newTrack), resourceUid);

			RatingBackupManager.restoreRating();

			assertEquals(Integer.valueOf(2), MediaTableResourceRatings.getRating(connection, key(newTrack)));
			assertNull(MediaTableResourceRatings.getRating(connection, key(oldTrack)));
		} finally {
			try (Connection connection = MediaDatabase.get().getConnection()) {
				removeFile(connection, key(oldTrack));
				removeFile(connection, key(newTrack));
			}
			SharedContentConfiguration.updateSharedContent(previouslyShared, false);
		}
	}

	private static void addFile(Connection connection, String filename, String resourceUid) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("INSERT INTO FILES (FILENAME, MODIFIED, FORMAT_TYPE, RUID) VALUES (?, ?, ?, ?)")) {
			statement.setString(1, filename);
			statement.setTimestamp(2, new Timestamp(1000L));
			statement.setInt(3, 1);
			statement.setString(4, resourceUid);
			statement.executeUpdate();
		}
	}

	private static void removeFile(Connection connection, String filename) throws SQLException {
		try (PreparedStatement statement = connection.prepareStatement("DELETE FROM FILES WHERE FILENAME = ?")) {
			statement.setString(1, filename);
			statement.executeUpdate();
		}
	}

	private static void share(File... folders) {
		SharedContentArray values = new SharedContentArray();
		for (File folder : folders) {
			values.add(new FolderContent(folder, false, false));
		}
		SharedContentConfiguration.updateSharedContent(values, false);
	}

	private static String key(File file) {
		return ProcessUtil.getSystemPathName(file.getAbsolutePath());
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
		File track = new File(album, "01.flac");
		if (!track.exists()) {
			Files.createFile(track.toPath());
		}
		return root;
	}

	private static void deleteRecursively(File folder) throws IOException {
		try (Stream<java.nio.file.Path> paths = Files.walk(folder.toPath())) {
			paths.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
		}
	}

}
