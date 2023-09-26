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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.configuration.sharedcontent.SharedContentListener;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.gui.GuiManager;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.MediaScannerDevice;
import net.pms.store.container.DVDISOFile;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.container.RealFolder;
import net.pms.store.container.VirtualFolder;
import net.pms.store.item.RealFile;
import net.pms.util.FileUtil;
import net.pms.util.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaScanner extends StoreContainer implements SharedContentListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaScanner.class);
	private static final String ENTRY_CREATE = StandardWatchEventKinds.ENTRY_CREATE.name();
	private static final String ENTRY_DELETE = StandardWatchEventKinds.ENTRY_DELETE.name();
	private static final String ENTRY_MODIFY = StandardWatchEventKinds.ENTRY_MODIFY.name();
	private static final Object DEFAULT_FOLDERS_LOCK = new Object();
	private static final List<FileWatcher.Watch> MEDIA_FILEWATCHERS = new ArrayList<>();
	private static final List<String> SHARED_FOLDERS = new ArrayList<>();
	private static final MediaScanner INSTANCE = new MediaScanner();

	@GuardedBy("DEFAULT_FOLDERS_LOCK")
	private static List<String> defaultFolders = null;
	private static Thread scannerThread;
	private static boolean running;

	private MediaScanner() {
		super(MediaScannerDevice.getRenderer(), "scanner", null);
	}

	private void startScan() {
		if (running) {
			throw new IllegalStateException("Can't scan when scan in progress");
		}
		setRunning(true);
		reset();
		GuiManager.setMediaScanStatus(true);

		discoverChildren();

		LOGGER.debug("Starting scan of: {}", this.getName());
		if (running) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					scan(this);
					// Running might have been set false during scan
					if (running) {
						MediaTableFiles.cleanup(connection);
					}
				}
			} finally {
				MediaDatabase.close(connection);
			}
			setRunning(false);
		}
		reset();
		GuiManager.setMediaScanStatus(false);
		GuiManager.setStatusLine(null);
	}

	/**
	 * Starts partial rescan
	 *
	 * @param filename This is the partial root of the scan. If a file is given,
	 * the parent folder will be scanned.
	 */
	private void scanFolder(String filename) {
		if (isInSharedFolders(filename) || isInDefaultFolders(filename)) {
			LOGGER.debug("rescanning file or folder : " + filename);

			File file = new File(filename);
			if (file.isFile()) {
				file = file.getParentFile();
			}
			RealFolder dir = new RealFolder(renderer, file);
			dir.doRefreshChildren();
			scan(dir);
		} else {
			LOGGER.warn("given file or folder doesn't share same base path as this server : " + filename);
		}
	}

	private void scan(StoreContainer resource) {
		if (running) {
			for (StoreResource child : resource.getChildren()) {
				// wait until the realtime lock is released before starting
				PMS.REALTIME_LOCK.lock();
				PMS.REALTIME_LOCK.unlock();

				if (running && child instanceof StoreContainer storeContainer && storeContainer.allowScan()) {

					// Display and log which folder is being scanned
					if (storeContainer instanceof RealFolder) {
						String childName = child.getName();
						LOGGER.debug("Scanning folder: " + childName);
						GuiManager.setStatusLine(Messages.getString("ScanningFolder") + " " + childName);
					}

					if (storeContainer.isDiscovered()) {
						storeContainer.refreshChildren();
					} else {
						// ugly hack
						if (storeContainer instanceof DVDISOFile || storeContainer instanceof PlaylistFolder) {
							storeContainer.syncResolve();
						}

						storeContainer.discoverChildren();
						if (child instanceof VirtualFolder virtualFolder) {
							virtualFolder.analyzeChildren(-1, false);
						}
						storeContainer.setDiscovered(true);
					}

					int count = storeContainer.getChildren().size();
					if (count != 0) {
						scan(storeContainer);
						storeContainer.getChildren().clear();
					}
				} else if (!running) {
					break;
				}
			}
		} else {
			GuiManager.setStatusLine(null);
		}
	}

	private void reset() {
		getChildren().clear();
		setDiscovered(false);
	}

	@Override
	public void discoverChildren() {
		if (isDiscovered()) {
			return;
		}
		List<SharedContent> sharedContents = SharedContentConfiguration.getSharedContentArray();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent instanceof FolderContent folder && folder.getFile() != null && folder.isActive()) {
				StoreResource realSystemFileResource = renderer.getMediaStore().createResourceFromFile(folder.getFile());
				addChild(realSystemFileResource, true, false);
			}
		}
	}

	@Override
	public synchronized void updateSharedContent() {
		setMediaFileWatchers();
		setSharedFolders();
	}

	@Override
	public String getName() {
		return "scanner";
	}

	public static void init() {
		SharedContentConfiguration.addListener(INSTANCE);
	}

	public static boolean isMediaScanRunning() {
		return scannerThread != null && scannerThread.isAlive();
	}

	public static void startMediaScan() {
		if (isMediaScanRunning()) {
			LOGGER.info("Cannot start media scanner: A scan is already in progress");
		} else {
			Runnable scan = () -> {
				try {
					if (INSTANCE.getDefaultRenderer() != null) {
						LOGGER.info("Media scan started");
						long start = System.currentTimeMillis();
						INSTANCE.startScan();
						LOGGER.info("Media scan completed in {} seconds", ((System.currentTimeMillis() - start) / 1000));
						LOGGER.info("Database analyze started");
						start = System.currentTimeMillis();
						MediaDatabase.analyzeDb();
						LOGGER.info("Database analyze completed in {} seconds", ((System.currentTimeMillis() - start) / 1000));
					}
				} catch (Exception e) {
					LOGGER.error("Unhandled exception during media scan: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			};
			scannerThread = new Thread(scan, "Media Scanner");
			scannerThread.setPriority(Thread.MIN_PRIORITY);
			scannerThread.start();
			GuiManager.setMediaScanStatus(true);
		}
	}

	public static void stopMediaScan() {
		if (isMediaScanRunning()) {
			setRunning(false);
			GuiManager.setMediaScanStatus(false);
		}
	}

	public static void scanFileOrFolder(String filename) {
		if (!isMediaScanRunning()) {
			Runnable scan = () -> {
				if (INSTANCE.getDefaultRenderer() != null) {
					INSTANCE.scanFolder(filename);
				}
			};
			Thread scanThread = new Thread(scan, "rescanFileOrFolder");
			scanThread.start();
		}
	}

	private static void setRunning(boolean value) {
		running = value;
	}

	private static synchronized boolean isInSharedFolders(String filename) {
		return hasSameBasePath(SHARED_FOLDERS, filename);
	}

	private static boolean isInDefaultFolders(String filename) {
		return hasSameBasePath(getDefaultFolders(), filename);
	}

	private static boolean hasSameBasePath(List<String> dirs, String filename) {
		for (String path : dirs) {
			if (filename.startsWith(path)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Enumerates and sets the default shared folders if none is configured.
	 *
	 * Note: This is a getter and a setter in one.
	 *
	 * @return The default shared folders.
	 */
	@Nonnull
	private static List<String> getDefaultFolders() {
		synchronized (DEFAULT_FOLDERS_LOCK) {
			if (defaultFolders == null) {
				// Lazy initialization
				List<String> result = new ArrayList<>();
				for (Path path : PlatformUtils.INSTANCE.getDefaultFolders()) {
					result.add(path.toString());
				}
				defaultFolders = Collections.unmodifiableList(result);
			}
			return defaultFolders;
		}
	}

	/**
	 * Advise renderer for added file.
	 *
	 * @param filename the file added
	 */
	private static void addFileEntry(String filename) {
		LOGGER.trace("File {} was created on the hard drive", filename);
		if (parseFileEntry(new File(filename))) {
			for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
				renderer.getMediaStore().fileAdded(filename);
			}
		}
	}

	/**
	 * Parses a file so it gets parsed and added to the database along the way.
	 *
	 * @param file the file to parse
	 */
	private static boolean parseFileEntry(File file) {
		if (!SystemFilesHelper.isPotentialMediaFile(file.getAbsolutePath())) {
			LOGGER.trace("Not parsing file that can't be media");
			return false;
		}

		if (!file.exists()) {
			LOGGER.trace("Not parsing file that no longer exists");
			return false;
		}

		if (FileUtil.isLocked(file)) {
			LOGGER.debug("File will not be parsed because it is open in another process");
			return false;
		}

		// TODO: Can this use UnattachedFolder and add instead?
		Renderer renderer = MediaScannerDevice.getRenderer();
		RealFile rf = new RealFile(renderer, file);
		rf.setParent(new StoreContainer(renderer, null, null));
		rf.resolveFormat();
		rf.syncResolve();

		if (rf.isValid()) {
			LOGGER.info("New file {} was detected and added to the media store", file.getName());
			MediaStoreIds.incrementSystemUpdateId();

			/*
			 * Something about this process causes Java to hold onto the
			 * file, which prevents things happening to it on the filesystem
			 * until the garbage collector runs.
			 * Some sources say it is a symptom of the nio namespace itself
			 * and the fix is to use older syntax, and others say other things,
			 * but until we have a real fix for it we ask Java to collect the
			 * garbage. It might not do it, but usually it does, which is better
			 * than what we had before.
			 */
			if (FileUtil.isLocked(file)) {
				System.gc();
				System.runFinalization();
			}

			return true;
		} else {
			LOGGER.trace("File {} was not recognized as valid media so was not added to the media store", file.getName());
		}
		return false;
	}

	private static void addFolderEntry(String filename) {
		LOGGER.trace("Folder {} was created on the hard drive", filename);
		for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
			renderer.getMediaStore().fileAdded(filename);
		}
		File[] files = new File(filename).listFiles();
		if (files != null) {
			LOGGER.trace("Crawling {}", filename);
			for (File file : files) {
				if (file.isFile()) {
					LOGGER.trace("File {} found in {}", file.getName(), filename);
					parseFileEntry(file);
				}
			}
		} else {
			LOGGER.trace("Folder {} is empty", filename);
		}
	}

	private static void removeFolderEntry(String filename) {
		LOGGER.trace("Folder {} was deleted or moved on the hard drive, removing all files within it from the database", filename);
		//folder may be empty
		for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
			renderer.getMediaStore().fileRemoved(filename);
		}
		if (MediaInfoStore.removeMediaEntriesInFolder(filename)) {
			MediaStoreIds.incrementSystemUpdateId();
		}
	}

	private static void removeFileEntry(String filename) {
		LOGGER.info("File {} was deleted or moved on the hard drive, removing it from the database", filename);
		if (MediaInfoStore.removeMediaEntry(filename)) {
			for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
				renderer.getMediaStore().fileRemoved(filename);
			}
			MediaStoreIds.incrementSystemUpdateId();
		}
	}

	/**
	 * Adds and removes files from the database when they are created, modified
	 * or deleted on the hard drive.
	 */
	private static final FileWatcher.Listener MEDIA_RESCANNER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> {
		if ((ENTRY_DELETE.equals(event) || ENTRY_CREATE.equals(event) || ENTRY_MODIFY.equals(event))) {
			/**
			 * If a new directory is created with files, the listener may not
			 * give us information about those new files, as it wasn't listening
			 * when they were created, so make sure we parse them.
			 */
			if (isDir) {
				if (ENTRY_CREATE.equals(event)) {
					addFolderEntry(filename);
				} else if (ENTRY_DELETE.equals(event)) {
					removeFolderEntry(filename);
				}
			} else {
				if (ENTRY_CREATE.equals(event)) {
					addFileEntry(filename);
				} else if (ENTRY_DELETE.equals(event)) {
					removeFileEntry(filename);
				} else {
					parseFileEntry(new File(filename));
				}
			}
		}
	};

	private static void setSharedFolders() {
		SHARED_FOLDERS.clear();
		for (File file : SharedContentConfiguration.getSharedFolders()) {
			if (file.exists()) {
				SHARED_FOLDERS.add(file.getAbsolutePath());
			}
		}
	}

	private static void setMediaFileWatchers() {
		for (FileWatcher.Watch watcher : MEDIA_FILEWATCHERS) {
			FileWatcher.remove(watcher);
		}
		MEDIA_FILEWATCHERS.clear();
		for (File file : SharedContentConfiguration.getMonitoredFolders()) {
			if (file.exists()) {
				if (!file.isDirectory()) {
					LOGGER.trace("Skip adding a FileWatcher for non-folder \"{}\"", file);
				} else {
					LOGGER.trace("Creating FileWatcher for " + file.toString());
					try {
						FileWatcher.Watch watcher = new FileWatcher.Watch(file.toString() + File.separator + "**", MEDIA_RESCANNER);
						MEDIA_FILEWATCHERS.add(watcher);
						FileWatcher.add(watcher);
					} catch (Exception e) {
						LOGGER.warn("File watcher access denied for directory {}", file.toString());
					}
				}
			} else {
				LOGGER.trace("Skip adding a FileWatcher for non-existent \"{}\"", file);
			}
		}
	}

}
