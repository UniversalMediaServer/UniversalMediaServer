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
package net.pms.store.container;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStatusStore;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.item.RealFile;
import net.pms.store.item.VirtualVideoActionLocalized;
import net.pms.util.FileUtil;
import net.pms.util.FullyPlayedAction;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaMonitor extends LocalizedStoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);

	private final ReentrantReadWriteLock fullyPlayedEntriesLock = new ReentrantReadWriteLock();
	private final HashMap<String, Boolean> fullyPlayedEntries = new HashMap<>();
	private final File[] dirs;

	public MediaMonitor(Renderer renderer, File[] dirs) {
		super(renderer, "Unused", "images/store/folder.png");
		this.dirs = new File[dirs.length];
		System.arraycopy(dirs, 0, this.dirs, 0, dirs.length);
	}

	public void scanDir(File[] files, final StoreContainer res) {
		if (files != null) {
			final StoreContainer mm = this;
			res.addChild(new VirtualVideoActionLocalized(renderer, "MarkAllAsPlayed", true, null) {
				@Override
				public boolean enable() {
					for (StoreResource r : res.getChildren()) {
						if (!(r instanceof RealFile)) {
							continue;
						}
						RealFile rf = (RealFile) r;
						MediaStatusStore.setFullyPlayed(rf.getFile().getAbsolutePath(), renderer.getAccountUserId(), true, null);
					}
					mm.setDiscovered(false);
					mm.getChildren().clear();
					MediaStoreIds.incrementSystemUpdateId();
					return true;
				}
			});

			Set<String> fullyPlayedPaths = null;
			if (renderer.getUmsConfiguration().isHideEmptyFolders()) {
				fullyPlayedPaths = new HashSet<>();
				fullyPlayedEntriesLock.readLock().lock();
				try {
					for (Entry<String, Boolean> entry : fullyPlayedEntries.entrySet()) {
						if (Boolean.TRUE.equals(entry.getValue())) {
							fullyPlayedPaths.add(entry.getKey());
						}
					}
				} finally {
					fullyPlayedEntriesLock.readLock().unlock();
				}
			}
			for (File fileEntry : files) {
				if (fileEntry.isFile()) {
					if (MediaStatusStore.isFullyPlayed(fileEntry.getAbsolutePath(), renderer.getAccountUserId())) {
						continue;
					}
					res.addChild(new RealFile(renderer, fileEntry));
				}
				if (fileEntry.isDirectory()) {
					boolean add = !renderer.getUmsConfiguration().isHideEmptyFolders();
					if (!add) {
						add = FileUtil.isFolderRelevant(fileEntry, renderer.getUmsConfiguration(), fullyPlayedPaths);
					}
					if (add) {
						res.addChild(new MonitorEntry(renderer, fileEntry, this));
					}
				}
			}
		}
	}

	@Override
	public void discoverChildren() {
		if (dirs != null) {
			for (File f : dirs) {
				scanDir(f.listFiles(), this);
			}
		}
	}

	@Override
	public boolean isRefreshNeeded() {
		return true;
	}

	/**
	 * Performs certain actions after a video or audio file is stopped, if the
	 * file is within a monitored directory. These actions include: - If the
	 * file is fully played: - Marking the file as fully played in the database
	 * - Updating the systemUpdateID to indicate to the client there is updated
	 * content - If the file is not fully played: - Update the last played date
	 * for the file in the database
	 *
	 * @param resource
	 */
	public void stopped(StoreResource resource) {
		if (!(resource instanceof RealFile)) {
			return;
		}

		final RealFile realFile = (RealFile) resource;
		String fullPathToFile = realFile.getFile().getAbsolutePath();

		boolean isMonitored = false;
		List<File> foldersMonitored = SharedContentConfiguration.getMonitoredFolders();
		if (!foldersMonitored.isEmpty()) {
			for (File folderMonitored : foldersMonitored) {
				if (fullPathToFile.contains(folderMonitored.getAbsolutePath())) {
					isMonitored = true;
					break;
				}
			}
		}

		if (!isMonitored) {
			LOGGER.trace("File {} is not within a monitored directory, so not calculating fully played status", realFile.getName());
			return;
		}

		// The total video duration in seconds
		double fileDuration = 0;
		if (realFile.getMediaInfo() != null && (realFile.getMediaInfo().isAudio() || realFile.getMediaInfo().isVideo())) {
			fileDuration = realFile.getMediaInfo().getDurationInSeconds();
		}

		long now = System.currentTimeMillis();

		/**
		 * Time since the file started playing. This is not a great way to get
		 * this value because if the video is paused, it will no longer be
		 * accurate.
		 */
		long lastStartSystemTime = realFile.getLastStartSystemTime();
		long lastStartSystemTimeUser = realFile.getLastStartSystemTimeUser();
		double lastStartPosition = realFile.getLastStartPosition();

		int minimumPlayTime = CONFIGURATION.getMinimumWatchedPlayTimeSeconds();
		FullyPlayedAction fullyPlayedAction = CONFIGURATION.getFullyPlayedAction();
		double triggerPlayTime = fileDuration * CONFIGURATION.getResumeBackFactor();

		// First, set the total amount of real time that has passed since the video was started
		double elapsed = 0D;
		if (lastStartSystemTimeUser > 0D) {
			elapsed = (now - lastStartSystemTimeUser) / 1000D;
		}

		// Add the start offset requested by the renderer
		if (lastStartPosition > 0D) {
			elapsed += lastStartPosition;
		}

		boolean logTrace = LOGGER.isTraceEnabled() && !fullyPlayedAction.equals(FullyPlayedAction.NO_ACTION);
		if (logTrace) {
			LOGGER.trace("Fully Played feature logging:");
			LOGGER.trace("   duration: " + fileDuration);
			LOGGER.trace("   getLastStartPosition: " + lastStartPosition);
			LOGGER.trace("   currentTime: " + now);
			LOGGER.trace("   getLastStartSystemTime: " + lastStartSystemTime);
			LOGGER.trace("   getLastStartSystemTimeUser: " + lastStartSystemTimeUser);
			LOGGER.trace("   minimum play time: " + minimumPlayTime);
			LOGGER.trace("   triggered fully played time: " + triggerPlayTime);
			LOGGER.trace("   elapsed: " + elapsed);
		}

		int userId = renderer.getAccountUserId();
		/**
		 * Only mark the file as fully played if more than 92% (default) of the
		 * duration has elapsed since it started playing.
		 */
		if (
			fileDuration == 0 ||
			elapsed > minimumPlayTime &&
			elapsed >= triggerPlayTime
		) {
			LOGGER.trace("final decision: fully played");
			StoreResource fileParent = realFile.getParent();
			if (fileParent == null) {
				LOGGER.trace("fileParent is null for {}", fullPathToFile);
			} else if (MediaStatusStore.isFullyPlayed(fullPathToFile, userId)) {
				LOGGER.trace("{} already marked as fully played", fullPathToFile);
			} else {
				/*
				 * Set to fully played even if it will be deleted or moved, because
				 * the entry will be cleaned up later in those cases.
				 */
				MediaStatusStore.setFullyPlayed(fullPathToFile, userId, true, elapsed);
				setDiscovered(false);
				getChildren().clear();

				File playedFile = new File(fullPathToFile);

				if (fullyPlayedAction == FullyPlayedAction.MOVE_FOLDER || fullyPlayedAction == FullyPlayedAction.MOVE_FOLDER_AND_MARK) {
					String oldDirectory = FileUtil.appendPathSeparator(playedFile.getAbsoluteFile().getParent());
					String newDirectory = FileUtil.appendPathSeparator(CONFIGURATION.getFullyPlayedOutputDirectory());
					if (!StringUtils.isBlank(newDirectory) && !newDirectory.equals(oldDirectory)) {
						// Move the video to a different folder
						String newFilename = playedFile.getName();
						String newPathToFile = newDirectory + newFilename;
						try {
							fileWillMove(fullPathToFile, newPathToFile);
							Files.move(Paths.get(playedFile.getAbsolutePath()), Paths.get(newPathToFile), StandardCopyOption.REPLACE_EXISTING);
							LOGGER.debug("Moved {} because it has been fully played", newFilename);
						} catch (IOException e) {
							LOGGER.debug("Moving {} failed, trying again in 3 seconds: {}", newFilename, e.getMessage());
							try {
								Thread.sleep(3000);
								Files.move(Paths.get(playedFile.getAbsolutePath()), Paths.get(newPathToFile), StandardCopyOption.REPLACE_EXISTING);
								LOGGER.debug("Moved {} because it has been fully played", newFilename);
							} catch (InterruptedException e2) {
								LOGGER.debug(
										"Abandoning moving of {} because the thread was interrupted, probably due to program shutdown: {}",
										newFilename,
										e2.getMessage()
								);
								fileMoveFail(newPathToFile);
								Thread.currentThread().interrupt();
							} catch (IOException e3) {
								LOGGER.debug("Moving {} failed a second time: {}", newFilename, e3.getMessage());
							}
						}
					} else if (StringUtils.isBlank(newDirectory)) {
						LOGGER.warn(
								"Failed to move \"{}\" after being fully played because the folder to move to isn't configured",
								playedFile.getName()
						);
					} else {
						LOGGER.trace(
								"Not moving \"{}\" after being fully played since it's already in the target folder \"{}\"",
								playedFile.getName(),
								newDirectory
						);
					}
				} else if (fullyPlayedAction == FullyPlayedAction.MOVE_TRASH) {
					try {
						PlatformUtils.INSTANCE.moveToTrash(playedFile);
					} catch (IOException e) {
						LOGGER.warn(
								"Failed to move file \"{}\" to recycler/trash after it has been fully played: {}",
								playedFile.getAbsoluteFile(),
								e.getMessage()
						);
						LOGGER.trace("", e);
					}
				}

				/**
				 * Here we bump the systemUpdateID because a file has either
				 * been removed or its thumbnail changed.
				 */
				if (fullyPlayedAction != FullyPlayedAction.NO_ACTION) {
					notifyRefresh();
				}

				LOGGER.info("{} marked as fully played", playedFile.getName());
			}
		} else {
			MediaStatusStore.setLastPlayed(fullPathToFile, userId, elapsed);
			LOGGER.trace("final decision: not fully played");
		}
	}

	private static void fileWillMove(String fullPathToFile, String fullPathToNewFile) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFilesStatus.createCopyOnFileMoved(connection, fullPathToFile, fullPathToNewFile);
			}
		} finally {
			MediaDatabase.close(connection);
		}
	}

	private static void fileMoveFail(String fullPathToNewFile) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFilesStatus.remove(connection, fullPathToNewFile, false);
			}
		} finally {
			MediaDatabase.close(connection);
		}
	}

	@Override
	public void doRefreshChildren() {
		if (isDiscovered()) {
			notifyRefresh();
		}
	}

}
