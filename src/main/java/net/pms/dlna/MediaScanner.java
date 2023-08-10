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
package net.pms.dlna;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.gui.GuiManager;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.devices.MediaScannerDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaScanner extends MediaResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaScanner.class);
	private static final Object DEFAULT_FOLDERS_LOCK = new Object();

	@GuardedBy("defaultFoldersLock")
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
			MediaResource dir = new RealFile(defaultRenderer, file);
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
				addChild(new RealFile(defaultRenderer, folder.getFile()), true, false);
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

	private void scan(MediaResource resource) {
		if (running) {
			for (MediaResource child : resource.getChildren()) {
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

}
