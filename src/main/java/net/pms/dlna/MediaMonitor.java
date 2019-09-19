package net.pms.dlna;

import com.sun.jna.Platform;
import com.sun.jna.platform.FileUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.database.TableFilesStatus;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.util.FileUtil;
import net.pms.util.FreedesktopTrash;
import net.pms.util.FullyPlayedAction;
import org.apache.commons.lang.StringUtils;
import org.fest.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaMonitor extends VirtualFolder {
	private static final ReentrantReadWriteLock fullyPlayedEntriesLock = new ReentrantReadWriteLock();
	private static final HashMap<String, Boolean> fullyPlayedEntries = new HashMap<>();
	private File[] dirs;
	private PmsConfiguration config;

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("VirtualFolder.2"), "images/thumbnail-folder-256.png");
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
						MediaMonitor.setFullyPlayed(entry, true);
					}
				}
			}
			dumpFile(); //TODO: Remove this and delete the file when 7.0.0 beta is over
		} catch (IOException e) {
			LOGGER.error("Error reading monitor file \"{}\": {}", f.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
	}

	public void scanDir(File[] files, final DLNAResource res) {
		if (files != null) {
			final DLNAResource mm = this;
			res.addChild(new VirtualVideoAction(Messages.getString("PMS.150"), true) {
				@Override
				public boolean enable() {
					for (DLNAResource r : res.getChildren()) {
						if (!(r instanceof RealFile)) {
							continue;
						}
						RealFile rf = (RealFile) r;
						MediaMonitor.setFullyPlayed(rf.getFile().getAbsolutePath(), true);
					}
					mm.setDiscovered(false);
					mm.getChildren().clear();
					return true;
				}
			});

			Set<String> fullyPlayedPaths = null;
			if (config.isHideEmptyFolders()) {
				fullyPlayedPaths = new HashSet<>();
				fullyPlayedEntriesLock.readLock().lock();
				try {
					for (Entry<String, Boolean> entry : fullyPlayedEntries.entrySet()) {
						if (entry.getValue()) {
							fullyPlayedPaths.add(entry.getKey());
						}
					}
				} finally {
					fullyPlayedEntriesLock.readLock().unlock();
				}
			}
			for (File fileEntry : files) {
				if (fileEntry.isFile()) {
					if (isFullyPlayed(fileEntry.getAbsolutePath())) {
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

	public void stopped(DLNAResource resource) {
		if (!(resource instanceof RealFile)) {
			return;
		}

		final RealFile realFile = (RealFile) resource;

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
			if (fileParent != null) {
				boolean isMonitored = false;
				List<Path> foldersMonitored = configuration.getMonitoredFolders();
				if (foldersMonitored != null && !foldersMonitored.isEmpty()) {
					for (Path folderMonitored : foldersMonitored) {
						if (realFile.getFile().getAbsolutePath().contains(folderMonitored.toAbsolutePath().toString())) {
							isMonitored = true;
						}
					}
				}

				if (isMonitored && !isFullyPlayed(realFile.getFile().getAbsolutePath())) {
					if (fullyPlayedAction != FullyPlayedAction.MOVE_FOLDER && fullyPlayedAction != FullyPlayedAction.MOVE_TRASH) {
						setFullyPlayed(realFile.getFile().getAbsolutePath(), true);
						if (realFile.getMedia() != null) {
							realFile.getMedia().setThumbready(false);
						}
					}

					setDiscovered(false);
					getChildren().clear();

					File playedFile = new File(realFile.getFile().getAbsolutePath());

					if (fullyPlayedAction == FullyPlayedAction.MOVE_FOLDER) {
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
								setFullyPlayed(newDirectory + playedFile.getName(), true);
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
							if (Platform.isLinux()) {
								FreedesktopTrash.moveToTrash(playedFile);
							} else {
								FileUtils.getInstance().moveToTrash(Arrays.array(playedFile));
							}
						} catch (IOException | FileUtil.InvalidFileSystemException e) {
							LOGGER.warn(
								"Failed to move file \"{}\" to recycler/trash after it has been fully played: {}",
								playedFile.getAbsoluteFile(),
								e.getMessage()
							);
							LOGGER.trace("", e);
						}
					}
					LOGGER.info("{} marked as fully played", playedFile.getName());
				}
			}
		} else {
			LOGGER.trace("   final decision: not fully played");
		}
	}

	/**
	 * Checks if {@code fullPathToFile} is registered as fully played. The check
	 * will first check the memory cache, and if not found there check the
	 * database and insert an entry in the memory cache.
	 *
	 * @param fullPathToFile the full path to the file whose status to retrieve.
	 * @return {@code true} if {@code fullPathToFile} is fully played,
	 *         {@code false} otherwise.
	 */
	public static boolean isFullyPlayed(String fullPathToFile) {
		fullyPlayedEntriesLock.readLock().lock();
		Boolean fullyPlayed;
		try {
			fullyPlayed = fullyPlayedEntries.get(fullPathToFile);
		} finally {
			fullyPlayedEntriesLock.readLock().unlock();
		}
		if (fullyPlayed != null) {
			return fullyPlayed;
		}

		// The status isn't cached, add it
		fullyPlayedEntriesLock.writeLock().lock();
		try {
			// It could have been added between the locks, check again
			fullyPlayed = fullyPlayedEntries.get(fullPathToFile);
			if (fullyPlayed != null) {
				return fullyPlayed;
			}

			// Add the entry to the cache
			fullyPlayed = TableFilesStatus.isFullyPlayed(fullPathToFile);
			if (fullyPlayed == null) {
				fullyPlayed = false;
			}
			fullyPlayedEntries.put(fullPathToFile, fullyPlayed);
			return fullyPlayed;
		} finally {
			fullyPlayedEntriesLock.writeLock().unlock();
		}
	}

	/**
	 * Sets the fully played status of the given {@code fullPathToFile} both in
	 * the memory cache and in the database.
	 *
	 * @param fullPathToFile the full path to the file in question.
	 * @param isFullyPlayed {@code true} if {@code fullPathToFile} is fully
	 *            played, {@code false} otherwise.
	 */
	public static void setFullyPlayed(String fullPathToFile, boolean isFullyPlayed) {
		fullyPlayedEntriesLock.writeLock().lock();
		try {
			fullyPlayedEntries.put(fullPathToFile, isFullyPlayed);
			TableFilesStatus.setFullyPlayed(fullPathToFile, isFullyPlayed);
		} finally {
			fullyPlayedEntriesLock.writeLock().unlock();
		}
	}

	/**
	 * Populates UMS.mon with a list of completely played media.
	 *
	 * @deprecated Should be removed when 7.0.0 is out of beta.
	 * @throws IOException
	 */
	@Deprecated
	private void dumpFile() throws IOException {
		File f = monitorFile();
		Date now = new Date();
		try (OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			sb.append("## Generated: ");
			sb.append(now.toString());
			sb.append("\n");
//			for (String str : fullyPlayedEntries) {
//				if (sb.indexOf(str) == -1) {
//					sb.append("entry=");
//					sb.append(str);
//					sb.append("\n");
//				}
//			}
			out.write(sb.toString());
			out.flush();
		}
	}

	@Override
	public void doRefreshChildren() {
		setUpdateId(this.getIntId());
	}
}