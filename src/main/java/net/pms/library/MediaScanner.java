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
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.RendererConfigurations;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.gui.GuiManager;
import net.pms.library.virtual.VirtualFile;
import net.pms.media.MediaInfoStore;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.devices.MediaScannerDevice;
import net.pms.util.FileUtil;
import net.pms.util.FileWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaScanner extends LibraryResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaScanner.class);
	private static final Object DEFAULT_FOLDERS_LOCK = new Object();
	private static final List<FileWatcher.Watch> LIBRARY_WATCHERS = new ArrayList<>();

	@GuardedBy("DEFAULT_FOLDERS_LOCK")
	private static List<Path> defaultFolders = null;

	private boolean running;

	public MediaScanner() {
		super(MediaScannerDevice.getMediaScannerDevice());
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

	public void stopScan() {
		if (running) {
			running = false;
			GuiManager.setScanLibraryStatus(CONFIGURATION.getUseCache(), false);
		}
	}

	/**
	 * Starts partial rescan
	 *
	 * @param filename This is the partial root of the scan. If a file is given,
	 * the parent folder will be scanned.
	 */
	public void scanFileOrFolder(String filename) {
		if (hasSameBasePathFromFiles(SharedContentConfiguration.getSharedFolders(), filename) ||
			hasSameBasePath(getDefaultFolders(), filename)) {
			LOGGER.debug("rescanning file or folder : " + filename);

			File file = new File(filename);
			if (file.isFile()) {
				file = file.getParentFile();
			}
			LibraryResource dir = new RealFile(renderer, file);
			dir.doRefreshChildren();
			scan(dir);
		} else {
			LOGGER.warn("given file or folder doesn't share same base path as this server : " + filename);
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
				addChild(new RealFile(renderer, folder.getFile()), true, false);
			}
		}
	}

	@Override
	public String getName() {
		return "scanner";
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	private void reset() {
		getChildren().clear();
		setDiscovered(false);
	}

	private void scan(LibraryResource resource) {
		if (running) {
			for (LibraryResource child : resource.getChildren()) {
				// wait until the realtime lock is released before starting
				PMS.REALTIME_LOCK.lock();
				PMS.REALTIME_LOCK.unlock();

				if (running && child.allowScan()) {

					// Display and log which folder is being scanned
					if (child instanceof RealFile) {
						String childName = child.getName();
						LOGGER.debug("Scanning folder: " + childName);
						GuiManager.setStatusLine(Messages.getString("ScanningFolder") + " " + childName);
					}

					if (child.isDiscovered()) {
						child.refreshChildren();
					} else {
						if (child instanceof DVDISOFile || child instanceof DVDISOTitle || child instanceof PlaylistFolder) { // ugly hack
							child.syncResolve();
						}
						child.discoverChildren();
						child.analyzeChildren(-1, false);
						child.setDiscovered(true);
					}

					int count = child.getChildren().size();

					if (count == 0) {
						continue;
					}

					scan(child);
					child.getChildren().clear();
				} else if (!running) {
					break;
				}
			}
		} else {
			GuiManager.setStatusLine(null);
		}
	}

	private static boolean hasSameBasePath(List<Path> dirs, String filename) {
		for (Path path : dirs) {
			if (filename.startsWith(path.toString())) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasSameBasePathFromFiles(List<File> dirs, String filename) {
		for (File file : dirs) {
			if (filename.startsWith(file.getAbsolutePath())) {
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
	private static List<Path> getDefaultFolders() {
		synchronized (DEFAULT_FOLDERS_LOCK) {
			if (defaultFolders == null) {
				// Lazy initialization
				defaultFolders = Collections.unmodifiableList(PlatformUtils.INSTANCE.getDefaultFolders());
			}
			return defaultFolders;
		}
	}

	/**
	 * Parses a file so it gets parsed and added to the database
	 * along the way.
	 *
	 * @param file the file to parse
	 */
	public static final void addMediaEntry(File file) {
		if (!VirtualFile.isPotentialMediaFile(file.getAbsolutePath())) {
			LOGGER.trace("Not parsing file that can't be media");
			return;
		}

		if (!file.exists()) {
			LOGGER.trace("Not parsing file that no longer exists");
			return;
		}

		if (FileUtil.isLocked(file)) {
			LOGGER.debug("File will not be parsed because it is open in another process");
			return;
		}

		// TODO: Can this use UnattachedFolder and add instead?
		RealFile rf = new RealFile(RendererConfigurations.getDefaultRenderer(), file);
		rf.setParent(rf);
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
			System.gc();
			System.runFinalization();
		} else {
			LOGGER.trace("File {} was not recognized as valid media so was not added to the database", file.getName());
		}
	}

	private static void addMediaEntriesInFolder(String filename) {
		LOGGER.trace("Folder {} was created on the hard drive", filename);
		File[] files = new File(filename).listFiles();
		if (files != null) {
			LOGGER.trace("Crawling {}", filename);
			for (File file : files) {
				if (file.isFile()) {
					LOGGER.trace("File {} found in {}", file.getName(), filename);
					addMediaEntry(file);
				}
			}
		} else {
			LOGGER.trace("Folder {} is empty", filename);
		}
	}

	private static void removeMediaEntriesInFolder(String filename) {
		LOGGER.trace("Folder {} was deleted or moved on the hard drive, removing all files within it from the database", filename);
		if (MediaInfoStore.removeMediaEntriesInFolder(filename)) {
			UmsContentDirectoryService.bumpSystemUpdateId();
		}
	}

	private static void removeMediaEntry(String filename) {
		LOGGER.trace("File {} was deleted or moved on the hard drive, removing it from the database", filename);
		if (MediaInfoStore.removeMediaEntry(filename)) {
			UmsContentDirectoryService.bumpSystemUpdateId();
		}
	}

	private static void fileScanner(String filename, String event, boolean isDir) {
		if (("ENTRY_DELETE".equals(event) || "ENTRY_CREATE".equals(event) || "ENTRY_MODIFY".equals(event))) {
			/**
			 * If a new directory is created with files, the listener may not
			 * give us information about those new files, as it wasn't listening
			 * when they were created, so make sure we parse them.
			 */
			if (isDir) {
				if ("ENTRY_CREATE".equals(event)) {
					addMediaEntriesInFolder(filename);
				} else if ("ENTRY_DELETE".equals(event)) {
					removeMediaEntriesInFolder(filename);
				}
			} else {
				if ("ENTRY_DELETE".equals(event)) {
					removeMediaEntry(filename);
				} else {
					LOGGER.trace("File {} was created on the hard drive", filename);
					File file = new File(filename);
					addMediaEntry(file);
				}
			}
		}
	}

	public static void setLibraryFileWatchers() {
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

	/**
	 * Adds and removes files from the database when they are created, modified or
	 * deleted on the hard drive.
	 */
	private static final FileWatcher.Listener LIBRARY_RESCANNER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> fileScanner(filename, event, isDir);

}
