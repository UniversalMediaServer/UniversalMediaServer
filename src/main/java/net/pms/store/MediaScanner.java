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
import java.util.concurrent.locks.ReentrantLock;
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
import net.pms.util.FileUtil;
import net.pms.util.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaScanner implements SharedContentListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaScanner.class);
	private static final String ENTRY_CREATE = StandardWatchEventKinds.ENTRY_CREATE.name();
	private static final String ENTRY_DELETE = StandardWatchEventKinds.ENTRY_DELETE.name();
	private static final String ENTRY_MODIFY = StandardWatchEventKinds.ENTRY_MODIFY.name();
	private static final Object DEFAULT_FOLDERS_LOCK = new Object();
	private static final ReentrantLock SCANNER_LOCK = new ReentrantLock();
	private static final List<FileWatcher.Watch> MEDIA_FILEWATCHERS = new ArrayList<>();
	private static final List<String> SHARED_FOLDERS = new ArrayList<>();
	private static final Renderer RENDERER = MediaScannerDevice.getRenderer();
	private static final MediaScanner INSTANCE = new MediaScanner();
	private static final List<String> FILES_PARSING = Collections.synchronizedList(new ArrayList<>());

	@GuardedBy("DEFAULT_FOLDERS_LOCK")
	private static List<String> defaultFolders = null;
	private static Thread scannerThread;
	private static boolean running;

	private MediaScanner() {
	}

	@Override
	public synchronized void updateSharedContent() {
		setMediaFileWatchers();
		setSharedFolders();
	}

	public static void init() {
		SharedContentConfiguration.addListener(INSTANCE);
	}

	private static void startScan() {
		if (running) {
			throw new IllegalStateException("Can't scan when scan in progress");
		}
		setRunning(true);
		reset();
		GuiManager.setMediaScanStatus(true);

		setSharedContent();

		LOGGER.debug("Starting media scanner");
		if (running) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					scan(RENDERER.getMediaStore());
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

	private static void scan(StoreContainer resource) {
		if (running) {
			for (StoreResource child : resource.getChildren()) {
				try {
					// wait until the MediaStore workers release before starting
					MediaStore.waitWorkers();
				} catch (InterruptedException ex) {
					running = false;
					Thread.currentThread().interrupt();
				}
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

	private static void reset() {
		RENDERER.getMediaStore().getChildren().clear();
		RENDERER.getMediaStore().setDiscovered(false);
	}

	private static void setSharedContent() {
		if (RENDERER.getMediaStore().isDiscovered()) {
			return;
		}
		List<SharedContent> sharedContents = SharedContentConfiguration.getSharedContentArray();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent instanceof FolderContent folder && folder.getFile() != null && folder.isActive()) {
				StoreResource realSystemFileResource = RENDERER.getMediaStore().createResourceFromFile(folder.getFile(), true);
				if (realSystemFileResource != null) {
					RENDERER.getMediaStore().addChild(realSystemFileResource);
				} else {
					LOGGER.trace("createResourceFromFile has failed for {}", folder.getFile());
				}
			}
		}
	}

	public static boolean isMediaScanRunning() {
		return scannerThread != null && scannerThread.isAlive();
	}

	public static void startMediaScan() {
		if (isMediaScanRunning()) {
			LOGGER.info("Cannot start media scanner: A scan is already in progress");
		} else if (PMS.isRunningTests()) {
			LOGGER.debug("Skipping media scanner because UMS is being run by a test");
		} else {
			Runnable scan = () -> {
				try {
					if (RENDERER != null) {
						LOGGER.info("Media scan started");
						long start = System.currentTimeMillis();
						try {
							SCANNER_LOCK.lock();
							startScan();
						} finally {
							SCANNER_LOCK.unlock();
						}
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

	/**
	 * only used by nextcpapi
	 */
	public static void backgroundScanFileOrFolder(String filename) {
		if (!isMediaScanRunning()) {
			Runnable scan = () -> {
				if (RENDERER != null) {
					scanFileOrFolder(filename);
				}
			};
			Thread scanThread = new Thread(scan, "scanFileOrFolder");
			scanThread.start();
		}
	}

	/**
	 * Starts partial rescan.
	 *
	 * @param filename This is the partial root of the scan. If a file is given,
	 * the parent folder will be scanned.
	 */
	private static void scanFileOrFolder(String filename) {
		try {
			SCANNER_LOCK.lock();
			reset();
			internalScanFileOrFolder(filename);
			reset();
		} finally {
			SCANNER_LOCK.unlock();
		}
	}

	/**
	 * Starts partial rescan.
	 *
	 * @param filename This is the partial root of the scan. If a file is given,
	 * the parent folder will be scanned.
	 */
	private static void internalScanFileOrFolder(String filename) {
		if (isInSharedFolders(filename) || isInDefaultFolders(filename)) {
			LOGGER.debug("Scanning file or folder : " + filename);

			File file = new File(filename);
			if (!file.exists()) {
				LOGGER.debug("Not scanning file or folder that no longer exists: " + filename);
				return;
			}
			if (file.isFile()) {
				file = file.getParentFile();
				LOGGER.debug("Scanning folder \"{}\" for file \"{}\"", file.getAbsolutePath(), filename);
			} else {
				LOGGER.debug("Scanning folder \"{}\"", file.getAbsolutePath());
			}
			List<StoreResource> systemFileResources = RENDERER.getMediaStore().findSystemFileResources(file);
			if (systemFileResources.isEmpty()) {
				//not yet discovered or root path outside shared folders ?
				String parent = file.getParentFile().getAbsolutePath();
				if (isInSharedFolders(parent)) {
					internalScanFileOrFolder(parent);
					systemFileResources = RENDERER.getMediaStore().findSystemFileResources(file);
				}
			}
			if (!systemFileResources.isEmpty()) {
				//if it is still empty, it mean the tree is no more accessible
				for (StoreResource storeResource : systemFileResources) {
					if (storeResource instanceof StoreContainer storeContainer) {
						storeContainer.discoverChildren();
						storeContainer.setDiscovered(true);
					}
				}
			} else {
				LOGGER.warn("Given folder was not found in store : " + file.getAbsolutePath());
			}
		} else {
			LOGGER.warn("Given file or folder doesn't share same base path as this server : " + filename);
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
	 * Threaded parses a file so it gets parsed and added to the database along the way.
	 *
	 * @param file the file to parse
	 */
	private static void parseFileEntry(File file, boolean advise) {
		String filename = file.getAbsolutePath();
		synchronized (FILES_PARSING) {
			if (FILES_PARSING.contains(filename)) {
				//parsing of this file is already in progress
				return;
			} else {
				FILES_PARSING.add(filename);
			}
		}
		Runnable r = () -> {
			try {
				if (advise) {
					LOGGER.debug("File {} was created on the hard drive", filename);
				}
				long currentSize = file.length();
				//wait 500 ms
				Thread.sleep(500);
				//Check if size changed (copying, downloading)
				while (file.exists() && (currentSize != file.length() || FileUtil.isLocked(file))) {
					//loop until file size is not changing anymore and file is unlocked.
					LOGGER.trace("Waiting file {} is fully written", filename);
					currentSize = file.length();
					Thread.sleep(500);
				}
				//here the file should be fully written, deleted or moved.
				synchronized (FILES_PARSING) {
					FILES_PARSING.remove(filename);
				}
				if (file.exists()) {
					LOGGER.debug("Analyzing file {}", filename);
					if (parseFileEntry(file) && advise) {
						//Advise renderers for added file.
						for (Renderer connectedRenderer : ConnectedRenderers.getConnectedRenderers()) {
							connectedRenderer.getMediaStore().fileAdded(file);
						}
					}
				} else {
					LOGGER.debug("File {} does not more exists", filename);
				}
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		};
		new Thread(r, "MediaScanner File Parser").start();
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

		if (!isInSharedFolders(file.getAbsolutePath())) {
			LOGGER.debug("File will not be parsed because it is not in a shared folder");
			return false;
		}

		StoreResource rf = RENDERER.getMediaStore().createResourceFromFile(file);
		if (rf != null) {
			if (rf instanceof StoreItem storeItem) {
				storeItem.resolveFormat();
			}
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
				}

				return true;
			}
		} else {
			LOGGER.trace("File {} was not recognized as valid media so was not added to the media store", file.getName());
		}
		return false;
	}

	private static void addFolderEntry(File directory) {
		LOGGER.trace("Folder {} was created on the hard drive", directory);
		if (RENDERER.getUmsConfiguration().getIgnoredFolderNames().contains(directory.getName())) {
			LOGGER.debug("Ignoring {} because it is in the ignored folders list", directory.getName());
			return;
		}
		for (Renderer connectedRenderer : ConnectedRenderers.getConnectedRenderers()) {
			connectedRenderer.getMediaStore().fileAdded(directory);
		}
		File[] files = directory.listFiles();
		if (files != null) {
			LOGGER.trace("Crawling {}", directory.getName());
			for (File file : files) {
				if (file.isFile()) {
					LOGGER.trace("File {} found in {}", file.getName(), directory.getName());
					parseFileEntry(file);
				}
			}
		} else {
			LOGGER.trace("Folder {} is empty", directory.getName());
		}
	}

	private static void removeFolderEntry(String filename) {
		LOGGER.trace("Folder {} was deleted or moved on the hard drive, removing all files within it from the database", filename);
		//folder may be empty
		File folder = new File(filename);
		for (Renderer connectedRenderer : ConnectedRenderers.getConnectedRenderers()) {
			connectedRenderer.getMediaStore().fileRemoved(folder);
		}
		if (MediaInfoStore.removeMediaEntriesInFolder(filename)) {
			MediaStoreIds.incrementSystemUpdateId();
		}
	}

	private static void removeFileEntry(String filename) {
		LOGGER.info("File {} was deleted or moved on the hard drive, removing it from the database", filename);
		if (MediaInfoStore.removeMediaEntry(filename)) {
			File file = new File(filename);
			for (Renderer connectedRenderer : ConnectedRenderers.getConnectedRenderers()) {
				connectedRenderer.getMediaStore().fileRemoved(file);
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
					addFolderEntry(new File(filename));
				} else if (ENTRY_DELETE.equals(event)) {
					removeFolderEntry(filename);
				}
			} else {
				if (ENTRY_CREATE.equals(event)) {
					parseFileEntry(new File(filename), true);
				} else if (ENTRY_DELETE.equals(event)) {
					removeFileEntry(filename);
				} else {
					parseFileEntry(new File(filename), false);
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
		List<String> ignoredFolderNames = RENDERER.getUmsConfiguration().getIgnoredFolderNames();
		for (File file : SharedContentConfiguration.getMonitoredFolders()) {
			if (file.exists()) {
				if (!file.isDirectory()) {
					LOGGER.trace("Skip adding a FileWatcher for non-folder \"{}\"", file);
				} else if (ignoredFolderNames.contains(file.getName())) {
					LOGGER.debug("Skip adding a FileWatcher for \"{}\" because it is in the ignored folders list", file.getName());
				} else {
					LOGGER.trace("Creating FileWatcher for " + file.toString());
					try {
						FileWatcher.Watch watcher = new FileWatcher.Watch(file.toString() + File.separator + "**", MEDIA_RESCANNER);
						watcher.setIgnoredFolderNames(ignoredFolderNames);
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
