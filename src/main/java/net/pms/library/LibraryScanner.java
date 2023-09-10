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
package net.pms.library;

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
import net.pms.library.container.DVDISOFile;
import net.pms.library.container.PlaylistFolder;
import net.pms.library.container.RealFolder;
import net.pms.library.container.VirtualFolder;
import net.pms.library.item.RealFile;
import net.pms.media.MediaInfoStore;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.ConnectedRenderers;
import net.pms.renderers.Renderer;
import net.pms.renderers.devices.LibraryScannerDevice;
import net.pms.util.FileUtil;
import net.pms.util.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibraryScanner extends LibraryContainer implements SharedContentListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryScanner.class);
	private static final String ENTRY_CREATE = StandardWatchEventKinds.ENTRY_CREATE.name();
	private static final String ENTRY_DELETE = StandardWatchEventKinds.ENTRY_DELETE.name();
	private static final String ENTRY_MODIFY = StandardWatchEventKinds.ENTRY_MODIFY.name();
	private static final Object DEFAULT_FOLDERS_LOCK = new Object();
	private static final List<FileWatcher.Watch> LIBRARY_WATCHERS = new ArrayList<>();
	private static final List<String> SHARED_FOLDERS = new ArrayList<>();
	private static final LibraryScanner INSTANCE = new LibraryScanner();

	@GuardedBy("DEFAULT_FOLDERS_LOCK")
	private static List<String> defaultFolders = null;
	private static Thread scannerThread;
	private static boolean running;

	private LibraryScanner() {
		super(LibraryScannerDevice.getLibraryScannerDevice(), "scanner", null);
	}

	public void startScan() {
		if (!CONFIGURATION.getUseCache()) {
			throw new IllegalStateException("Can't scan when cache is disabled");
		}
		if (running) {
			throw new IllegalStateException("Can't scan when scan in progress");
		}
		running = true;
		reset();
		GuiManager.setScanLibraryStatus(true, true);

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
			running = false;
		}
		reset();
		GuiManager.setScanLibraryStatus(CONFIGURATION.getUseCache(), false);
		GuiManager.setStatusLine(null);
	}

	/**
	 * Starts partial rescan
	 *
	 * @param filename This is the partial root of the scan. If a file is given,
	 * the parent folder will be scanned.
	 */
	public void scanFolder(String filename) {
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

	private void reset() {
		getChildren().clear();
		setDiscovered(false);
	}

	private void scan(LibraryContainer resource) {
		if (running) {
			for (LibraryResource child : resource.getChildren()) {
				// wait until the realtime lock is released before starting
				PMS.REALTIME_LOCK.lock();
				PMS.REALTIME_LOCK.unlock();

				if (running && child instanceof LibraryContainer libraryContainer && libraryContainer.allowScan()) {

					// Display and log which folder is being scanned
					if (libraryContainer instanceof RealFolder) {
						String childName = child.getName();
						LOGGER.debug("Scanning folder: " + childName);
						GuiManager.setStatusLine(Messages.getString("ScanningFolder") + " " + childName);
					}

					if (libraryContainer.isDiscovered()) {
						libraryContainer.refreshChildren();
					} else {
						// ugly hack
						if (libraryContainer instanceof DVDISOFile || libraryContainer instanceof PlaylistFolder) {
							libraryContainer.syncResolve();
						}

						libraryContainer.discoverChildren();
						if (child instanceof VirtualFolder virtualFolder) {
							virtualFolder.analyzeChildren(-1, false);
						}
						libraryContainer.setDiscovered(true);
					}

					int count = libraryContainer.getChildren().size();
					if (count != 0) {
						scan(libraryContainer);
						libraryContainer.getChildren().clear();
					}
				} else if (!running) {
					break;
				}
			}
		} else {
			GuiManager.setStatusLine(null);
		}
	}

	@Override
	public void discoverChildren() {
		if (isDiscovered()) {
			return;
		}
		List<SharedContent> sharedContents = SharedContentConfiguration.getSharedContentArray();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent instanceof FolderContent folder && folder.getFile() != null && folder.isActive()) {
				LibraryResource realSystemFileResource = renderer.getRootFolder().createResourceFromFile(folder.getFile());
				addChild(realSystemFileResource, true, false);
			}
		}
	}

	@Override
	public synchronized void updateSharedContent() {
		setLibraryFileWatchers();
		setSharedFolders();
	}

	@Override
	public String getName() {
		return "scanner";
	}

	public static void init() {
		SharedContentConfiguration.addListener(INSTANCE);
	}

	public static boolean isScanLibraryRunning() {
		return scannerThread != null && scannerThread.isAlive();
	}

	public static void scanLibrary() {
		if (isScanLibraryRunning()) {
			LOGGER.info("Cannot start library scanner: A scan is already in progress");
		} else {
			Runnable scan = () -> {
				try {
					if (INSTANCE.getDefaultRenderer() != null) {
						LOGGER.info("Library scan started");
						long start = System.currentTimeMillis();
						INSTANCE.startScan();
						LOGGER.info("Library scan completed in {} seconds", ((System.currentTimeMillis() - start) / 1000));
						LOGGER.info("Database analyze started");
						start = System.currentTimeMillis();
						MediaDatabase.analyzeDb();
						LOGGER.info("Database analyze completed in {} seconds", ((System.currentTimeMillis() - start) / 1000));
					}
				} catch (Exception e) {
					LOGGER.error("Unhandled exception during library scan: {}", e.getMessage());
					LOGGER.trace("", e);
				}
			};
			scannerThread = new Thread(scan, "Library Scanner");
			scannerThread.setPriority(Thread.MIN_PRIORITY);
			scannerThread.start();
			GuiManager.setScanLibraryStatus(true, true);
		}
	}

	public static void scanFileOrFolder(String filename) {
		if (!isScanLibraryRunning()) {
			Runnable scan = () -> {
				if (INSTANCE.getDefaultRenderer() != null) {
					INSTANCE.scanFolder(filename);
				}
			};
			Thread scanThread = new Thread(scan, "rescanLibraryFileOrFolder");
			scanThread.start();
		}
	}

	public static void stopScanLibrary() {
		if (isScanLibraryRunning()) {
			running = false;
			GuiManager.setScanLibraryStatus(CONFIGURATION.getUseCache(), false);
		}
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
	public static final void addFileEntry(String filename) {
		LOGGER.trace("File {} was created on the hard drive", filename);
		if (parseFileEntry(new File(filename))) {
			for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
				renderer.getRootFolder().fileAdded(filename);
			}
		}
	}

	/**
	 * Parses a file so it gets parsed and added to the database along the way.
	 *
	 * @param file the file to parse
	 */
	public static final boolean parseFileEntry(File file) {
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
		RealFile rf = new RealFile(LibraryScannerDevice.getLibraryScannerDevice(), file);
		rf.setParent(new LibraryContainer(LibraryScannerDevice.getLibraryScannerDevice(), null, null));
		rf.resolveFormat();
		rf.syncResolve();

		if (rf.isValid()) {
			LOGGER.info("New file {} was detected and added to the Media Library", file.getName());
			UmsContentDirectoryService.bumpSystemUpdateId();

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
			LOGGER.trace("File {} was not recognized as valid media so was not added to the database", file.getName());
		}
		return false;
	}

	private static void addFolderEntry(String filename) {
		LOGGER.trace("Folder {} was created on the hard drive", filename);
		for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
			renderer.getRootFolder().fileAdded(filename);
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
			renderer.getRootFolder().fileRemoved(filename);
		}
		if (MediaInfoStore.removeMediaEntriesInFolder(filename)) {
			UmsContentDirectoryService.bumpSystemUpdateId();
		}
	}

	private static void removeFileEntry(String filename) {
		LOGGER.info("File {} was deleted or moved on the hard drive, removing it from the database", filename);
		if (MediaInfoStore.removeMediaEntry(filename)) {
			for (Renderer renderer : ConnectedRenderers.getConnectedRenderers()) {
				renderer.getRootFolder().fileRemoved(filename);
			}
			UmsContentDirectoryService.bumpSystemUpdateId();
		}
	}

	/**
	 * Adds and removes files from the database when they are created, modified
	 * or deleted on the hard drive.
	 */
	private static final FileWatcher.Listener LIBRARY_RESCANNER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> {
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

	private static void setLibraryFileWatchers() {
		for (FileWatcher.Watch watcher : LIBRARY_WATCHERS) {
			FileWatcher.remove(watcher);
		}
		LIBRARY_WATCHERS.clear();
		for (File file : SharedContentConfiguration.getMonitoredFolders()) {
			if (file.exists()) {
				if (!file.isDirectory()) {
					LOGGER.trace("Skip adding a FileWatcher for non-folder \"{}\"", file);
				} else {
					LOGGER.trace("Creating FileWatcher for " + file.toString());
					try {
						FileWatcher.Watch watcher = new FileWatcher.Watch(file.toString() + File.separator + "**", LIBRARY_RESCANNER);
						LIBRARY_WATCHERS.add(watcher);
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
