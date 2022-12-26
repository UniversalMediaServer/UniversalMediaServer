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

import java.io.*;
import java.nio.charset.StandardCharsets;
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
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFilesStatus;
import net.pms.database.MediaTableTVSeries;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.platform.PlatformUtils;
import net.pms.util.FileUtil;
import net.pms.util.FullyPlayedAction;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaMonitor extends VirtualFolder {
	private static final ReentrantReadWriteLock FULLY_PLAYED_ENTRIES_LOCK = new ReentrantReadWriteLock();
	private static final HashMap<String, Boolean> FULLY_PLAYED_ENTRIES = new HashMap<>();
	private final File[] dirs;
	private final UmsConfiguration config;

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("Unused"), "images/thumbnail-folder-256.png");
		this.dirs = new File[dirs.length];
		System.arraycopy(dirs, 0, this.dirs, 0, dirs.length);
		config = PMS.getConfiguration();
		parseMonitorFile();
	}

	/**
	 * The UTF-8 encoded file containing fully played entries.
	 * @return The file
	 */
	private File monitorFile() {
		return new File(config.getDataFile("UMS.mon"));
	}

	private void parseMonitorFile() {
		File f = monitorFile();
		if (!f.exists()) {
			return;
		}
		try {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
				String str;

				while ((str = in.readLine()) != null) {
					if (StringUtils.isEmpty(str)) {
						continue;
					}
					str = str.trim();
					if (str.startsWith("#")) {
						continue;
					}
					if (str.startsWith("entry=")) {
						String entry = str.substring(6);
						if (!new File(entry.trim()).exists()) {
							continue;
						}

						entry = entry.trim();
						setFullyPlayed(entry, true, null);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.error("Error reading monitor file \"{}\": {}", f.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public void scanDir(File[] files, final DLNAResource res) {
		if (files != null) {
			final DLNAResource mm = this;
			res.addChild(new VirtualVideoAction(Messages.getString("MarkAllAsPlayed"), true, null) {
				@Override
				public boolean enable() {
					for (DLNAResource r : res.getChildren()) {
						if (!(r instanceof RealFile)) {
							continue;
						}
						RealFile rf = (RealFile) r;
						setFullyPlayed(rf.getFile().getAbsolutePath(), true, null);
					}
					mm.setDiscovered(false);
					mm.getChildren().clear();
					return true;
				}
			});

			Set<String> fullyPlayedPaths = null;
			if (config.isHideEmptyFolders()) {
				fullyPlayedPaths = new HashSet<>();
				FULLY_PLAYED_ENTRIES_LOCK.readLock().lock();
				try {
					for (Entry<String, Boolean> entry : FULLY_PLAYED_ENTRIES.entrySet()) {
						if (entry.getValue()) {
							fullyPlayedPaths.add(entry.getKey());
						}
					}
				} finally {
					FULLY_PLAYED_ENTRIES_LOCK.readLock().unlock();
				}
			}
			for (File fileEntry : files) {
				if (fileEntry.isFile()) {
					if (isFullyPlayed(fileEntry.getAbsolutePath(), true)) {
						continue;
					}
					res.addChild(new RealFile(fileEntry));
				}
				if (fileEntry.isDirectory()) {
					boolean add = !config.isHideEmptyFolders();
					if (!add) {
						add = FileUtil.isFolderRelevant(fileEntry, config, fullyPlayedPaths);
					}
					if (add) {
						res.addChild(new MonitorEntry(fileEntry, this));
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
	 * Performs certain actions after a video or audio file is stopped, if the file is
	 * within a monitored directory.
	 * These actions include:
	 * - If the file is fully played:
	 *   - Marking the file as fully played in the database
	 *   - Updating the systemUpdateID to indicate to the client there is updated content
	 * - If the file is not fully played:
	 *   - Update the last played date for the file in the database
	 *
	 * @param resource
	 */
	public void stopped(DLNAResource resource) {
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
		if (realFile.getMedia() != null && (realFile.getMedia().isAudio() || realFile.getMedia().isVideo())) {
			fileDuration = realFile.getMedia().getDurationInSeconds();
		}

		/**
		 * Time since the file started playing. This is not a great way to get
		 * this value because if the video is paused, it will no longer be
		 * accurate.
		 */
		double elapsed;
		if (realFile.getLastStartPosition() == 0) {
			elapsed = (double) (System.currentTimeMillis() - realFile.getStartTime()) / 1000;
		} else {
			elapsed = (System.currentTimeMillis() - realFile.getLastStartSystemTime()) / 1000;
			elapsed += realFile.getLastStartPosition();
		}

		FullyPlayedAction fullyPlayedAction = configuration.getFullyPlayedAction();

		if (LOGGER.isTraceEnabled() && !fullyPlayedAction.equals(FullyPlayedAction.NO_ACTION)) {
			LOGGER.trace("Fully Played feature logging:");
			LOGGER.trace("   duration: " + fileDuration);
			LOGGER.trace("   getLastStartPosition: " + realFile.getLastStartPosition());
			LOGGER.trace("   getStartTime: " + realFile.getStartTime());
			LOGGER.trace("   getLastStartSystemTime: " + realFile.getLastStartSystemTime());
			LOGGER.trace("   elapsed: " + elapsed);
			LOGGER.trace("   minimum play time needed: " + (fileDuration * configuration.getResumeBackFactor()));
		}

		/**
		 * Only mark the file as fully played if more than 92% (default) of
		 * the duration has elapsed since it started playing.
		 */
		if (
			fileDuration == 0 ||
			elapsed > configuration.getMinimumWatchedPlayTimeSeconds() &&
			elapsed >= (fileDuration * configuration.getResumeBackFactor())
		) {
			DLNAResource fileParent = realFile.getParent();
			if (fileParent != null && !isFullyPlayed(fullPathToFile, true)) {
				/*
				 * Set to fully played even if it will be deleted or moved, because
				 * the entry will be cleaned up later in those cases.
				 */
				setFullyPlayed(fullPathToFile, true, elapsed);
				setDiscovered(false);
				getChildren().clear();

				File playedFile = new File(fullPathToFile);

				if (fullyPlayedAction == FullyPlayedAction.MOVE_FOLDER || fullyPlayedAction == FullyPlayedAction.MOVE_FOLDER_AND_MARK) {
					String oldDirectory = FileUtil.appendPathSeparator(playedFile.getAbsoluteFile().getParent());
					String newDirectory = FileUtil.appendPathSeparator(configuration.getFullyPlayedOutputDirectory());
					if (!StringUtils.isBlank(newDirectory) && !newDirectory.equals(oldDirectory)) {
						// Move the video to a different folder
						boolean moved = false;
						File newFile = null;

						try {
							Files.move(Paths.get(playedFile.getAbsolutePath()), Paths.get(newDirectory + playedFile.getName()), StandardCopyOption.REPLACE_EXISTING);
							LOGGER.debug("Moved {} because it has been fully played", playedFile.getName());
							newFile = new File(newDirectory + playedFile.getName());
							moved = true;
						} catch (IOException e) {
							LOGGER.debug("Moving {} failed, trying again in 3 seconds: {}", playedFile.getName(), e.getMessage());

							try {
								Thread.sleep(3000);
								Files.move(Paths.get(playedFile.getAbsolutePath()), Paths.get(newDirectory + playedFile.getName()), StandardCopyOption.REPLACE_EXISTING);
								LOGGER.debug("Moved {} because it has been fully played", playedFile.getName());
								newFile = new File(newDirectory + playedFile.getName());
								moved = true;
							} catch (InterruptedException e2) {
								LOGGER.debug(
									"Abandoning moving of {} because the thread was interrupted, probably due to program shutdown: {}",
									playedFile.getName(),
									e2.getMessage()
								);
								Thread.currentThread().interrupt();
							} catch (IOException e3) {
								LOGGER.debug("Moving {} failed a second time: {}", playedFile.getName(), e3.getMessage());
							}
						}

						if (moved) {
							RootFolder.parseFileForDatabase(newFile);
							setFullyPlayed(newDirectory + playedFile.getName(), true, elapsed);
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
				 * Here we bump the systemUpdateID because a file has
				 * either been removed or its thumbnail changed.
				 */
				if (fullyPlayedAction != FullyPlayedAction.NO_ACTION) {
					notifyRefresh();
				}

				LOGGER.info("{} marked as fully played", playedFile.getName());
			}
		} else {
			setLastPlayed(fullPathToFile, elapsed);
			LOGGER.trace("final decision: not fully played");
		}
	}

	/**
	 * Checks if {@code fullPathToFile} is registered as fully played.The check
	 * will first check the memory cache, and if not found there check the
	 * database and insert an entry in the memory cache.
	 *
	 * @param fullPathToFile the full path to the file whose status to retrieve.
	 * @param isFileOrTVSeries whether this is a file or TV series
	 * @return {@code true} if {@code fullPathToFile} is fully played,
	 *         {@code false} otherwise.
	 */
	public static boolean isFullyPlayed(String fullPathToFile, boolean isFileOrTVSeries) {
		FULLY_PLAYED_ENTRIES_LOCK.readLock().lock();
		Boolean fullyPlayed;
		try {
			fullyPlayed = FULLY_PLAYED_ENTRIES.get(fullPathToFile);
		} finally {
			FULLY_PLAYED_ENTRIES_LOCK.readLock().unlock();
		}
		if (fullyPlayed != null) {
			return fullyPlayed;
		}

		// The status isn't cached, add it
		FULLY_PLAYED_ENTRIES_LOCK.writeLock().lock();
		try {
			// It could have been added between the locks, check again
			fullyPlayed = FULLY_PLAYED_ENTRIES.get(fullPathToFile);
			if (fullyPlayed != null) {
				return fullyPlayed;
			}

			// Add the entry to the cache
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					if (isFileOrTVSeries) {
						fullyPlayed = MediaTableFilesStatus.isFullyPlayed(connection, fullPathToFile);
					} else {
						fullyPlayed = MediaTableTVSeries.isFullyPlayed(connection, fullPathToFile);
					}
				}
			} finally {
				MediaDatabase.close(connection);
			}
			if (fullyPlayed == null) {
				fullyPlayed = false;
			}
			FULLY_PLAYED_ENTRIES.put(fullPathToFile, fullyPlayed);
			return fullyPlayed;
		} finally {
			FULLY_PLAYED_ENTRIES_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Sets the fully played status of the given {@code fullPathToFile} both in
	 * the memory cache and in the database.
	 *
	 * @param fullPathToFile the full path to the file in question.
	 * @param isFullyPlayed {@code true} if {@code fullPathToFile} is fully
	 *            played, {@code false} otherwise.
	 * @param lastPlaybackPosition how many seconds were played
	 */
	public static void setFullyPlayed(String fullPathToFile, boolean isFullyPlayed, Double lastPlaybackPosition) {
		FULLY_PLAYED_ENTRIES_LOCK.writeLock().lock();
		Connection connection = null;
		try {
			FULLY_PLAYED_ENTRIES.put(fullPathToFile, isFullyPlayed);
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFilesStatus.setFullyPlayed(connection, fullPathToFile, isFullyPlayed);
				if (lastPlaybackPosition != null) {
					MediaTableFilesStatus.setLastPlayed(connection, fullPathToFile, lastPlaybackPosition);
				}
			}
		} finally {
			MediaDatabase.close(connection);
			FULLY_PLAYED_ENTRIES_LOCK.writeLock().unlock();
		}
	}

	/**
	 * Sets the last played position of the given {@code fullPathToFile} both in
	 * the database.
	 *
	 * @param fullPathToFile the full path to the file in question.
	 * @param lastPlaybackPosition how many seconds were played
	 */
	public static void setLastPlayed(String fullPathToFile, Double lastPlaybackPosition) {
		if (lastPlaybackPosition != null) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaTableFilesStatus.setLastPlayed(connection, fullPathToFile, lastPlaybackPosition);
				}
			} finally {
				MediaDatabase.close(connection);
			}
		}
	}

	@Override
	public void doRefreshChildren() {
		if (isDiscovered()) {
			notifyRefresh();
		}
	}
}
