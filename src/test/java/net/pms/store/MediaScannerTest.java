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

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import javax.imageio.ImageIO;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContentArray;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.configuration.sharedcontent.SharedContentListener;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import net.pms.database.MediaDatabase;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MediaScannerTest {
	@TempDir
	Path temp;

	@BeforeAll
	static void initialize() throws Exception {
		PMS.get();
		PMS.setConfiguration(new UmsConfiguration(false));
		RendererConfigurations.loadRendererConfigurations();
	}

	@Test
	void collectsOnlyEnabledLocalFolders() {
		FolderContent active = new FolderContent(temp.resolve("active").toFile(), false, false);
		FolderContent disabled = new FolderContent(temp.resolve("disabled").toFile());
		disabled.setActive(false);
		VirtualFolderContent excluded = new VirtualFolderContent(null, "excluded", List.of(active), false);
		VirtualFolderContent hidden = new VirtualFolderContent("hidden", List.of(active));
		hidden.setActive(false);
		assertTrue(MediaScanner.getScanFolders(List.of(disabled, excluded, hidden, new FolderContent(null))).isEmpty());
		VirtualFolderContent nested = new VirtualFolderContent("nested", List.of(active));
		assertEquals(Set.of(temp.resolve("active")), MediaScanner.getScanFolders(List.of(active, nested)));
	}

	@Test
	void newlySharedFolderIsScannedRecursivelyInBackground() throws Exception {
		PMS.get();
		MediaDatabase.init();
		SharedContentArray original = SharedContentConfiguration.getSharedContentArray();
		MediaScanner scanner = (MediaScanner) field(MediaScanner.class, "INSTANCE").get(null);
		Object originalFolders = field(MediaScanner.class, "scanFolders").get(scanner);
		ExecutorService executor = (ExecutorService) field(MediaScanner.class, "PARTIAL_SCAN_EXECUTOR").get(null);
		ReentrantLock lock = (ReentrantLock) field(MediaScanner.class, "SCANNER_LOCK").get(null);
		@SuppressWarnings("unchecked")
		List<SharedContentListener> listeners = (List<SharedContentListener>) field(SharedContentConfiguration.class, "LISTENERS").get(null);
		try {
			SharedContentArray contents = new SharedContentArray();
			Path oldFolder = Files.createDirectory(temp.resolve("old"));
			contents.add(new FolderContent(oldFolder.toFile(), false));
			SharedContentConfiguration.updateSharedContent(contents, false);
			field(MediaScanner.class, "scanFolders").set(scanner, null);
			SharedContentConfiguration.addListener(scanner);
			awaitScans(executor);
			File oldImage = image(oldFolder.resolve("old.png"));
			assertFalse(isCached(oldImage), "Listener initialization must leave startup scanning to PMS");

			Path added = Files.createDirectory(temp.resolve("added"));
			File nestedImage = image(Files.createDirectories(added.resolve("nested/deep")).resolve("new.png"));
			// Simulate an existing scan holding the shared scanner state. Updating
			// configuration must return before that scan finishes.
			Thread previousScannerThread = (Thread) field(MediaScanner.class, "scannerThread").get(null);
			lock.lock();
			try {
				field(MediaScanner.class, "scannerThread").set(null, Thread.currentThread());
				assertTrue(MediaScanner.isMediaScanRunning());
				contents.add(new FolderContent(added.toFile(), false));
				SharedContentConfiguration.updateSharedContent(contents, false);
				assertFalse(isCached(nestedImage));
			} finally {
				field(MediaScanner.class, "scannerThread").set(null, previousScannerThread);
				lock.unlock();
			}
			awaitScans(executor);
			assertTrue(isCached(nestedImage), "New shares must cache nested files without browsing or restarting");
			assertFalse(isCached(oldImage), "Adding a share must not rescan existing shares");

			File laterImage = image(added.resolve("later.png"));
			// Duplicate paths, ordering and metadata changes must not scan again.
			contents.add(0, new FolderContent(added.resolve(".").toFile(), false, false));
			SharedContentConfiguration.updateSharedContent(contents, false);
			awaitScans(executor);
			assertFalse(isCached(laterImage));

			Path disabledFolder = Files.createDirectory(temp.resolve("disabled"));
			File disabledImage = image(disabledFolder.resolve("disabled.png"));
			FolderContent disabled = new FolderContent(disabledFolder.toFile(), false);
			disabled.setActive(false);
			contents.add(new VirtualFolderContent("nested", List.of(disabled)));
			SharedContentConfiguration.updateSharedContent(contents, false);
			awaitScans(executor);
			assertFalse(isCached(disabledImage));
			// Edit a copy as the UI does, rather than mutating stored configuration.
			contents = SharedContentConfiguration.getSharedContentArray();
			VirtualFolderContent virtual = (VirtualFolderContent) contents.get(contents.size() - 1);
			virtual.getChilds().get(0).setActive(true);
			SharedContentConfiguration.updateSharedContent(contents, false);
			awaitScans(executor);
			assertTrue(isCached(disabledImage), "Enabling a nested share should precache it");

			Path removed = Files.createDirectory(temp.resolve("removed"));
			File removedImage = image(removed.resolve("removed.png"));
			Path retainedChild = Files.createDirectory(removed.resolve("child"));
			File retainedImage = image(retainedChild.resolve("retained.png"));
			lock.lock();
			try {
				FolderContent content = new FolderContent(removed.toFile(), false);
				contents.add(content);
				contents.add(new FolderContent(retainedChild.toFile(), false));
				SharedContentConfiguration.updateSharedContent(contents, false);
				contents.remove(content);
				SharedContentConfiguration.updateSharedContent(contents, false);
			} finally {
				lock.unlock();
			}
			awaitScans(executor);
			assertFalse(isCached(removedImage), "Removed shares must be rechecked when queued scans run");
			assertTrue(isCached(retainedImage), "Removing a queued parent must not lose a still-shared child");
		} finally {
			synchronized (listeners) {
				listeners.remove(scanner);
			}
			SharedContentConfiguration.updateSharedContent(original, false);
			field(MediaScanner.class, "scanFolders").set(scanner, originalFolders);
		}
	}

	private File image(Path path) throws Exception {
		File file = path.toFile();
		assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", file));
		return file;
	}

	private boolean isCached(File file) throws Exception {
		try (Connection connection = MediaDatabase.getConnectionIfAvailable();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM FILES WHERE FILENAME = ?")) {
			statement.setString(1, file.getAbsolutePath());
			try (ResultSet result = statement.executeQuery()) {
				result.next();
				return result.getInt(1) > 0;
			}
		}
	}

	private void awaitScans(ExecutorService executor) throws Exception {
		executor.submit(() -> {}).get(30, TimeUnit.SECONDS);
	}

	private static Field field(Class<?> type, String name) throws Exception {
		Field field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}
}
