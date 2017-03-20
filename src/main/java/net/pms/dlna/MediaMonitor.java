package net.pms.dlna;

import com.sun.jna.Platform;
import com.sun.jna.platform.FileUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
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
	private static Set<String> fullyPlayedEntries;
	private File[] dirs;
	private PmsConfiguration config;

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("VirtualFolder.2"), "images/thumbnail-folder-256.png");
		this.dirs = dirs;
		fullyPlayedEntries = new HashSet<>();
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
						fullyPlayedEntries.add(entry.trim());
					}
				}
			}
			dumpFile();
		} catch (IOException e) {
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
						fullyPlayedEntries.add(rf.getFile().getAbsolutePath());
					}
					mm.setDiscovered(false);
					mm.getChildren().clear();
					try {
						dumpFile();
					} catch (IOException e) {
					}
					return true;
				}
			});

			for (File f : files) {
				if (f.isFile()) {
					if (isFullyPlayed(f.getAbsolutePath())) {
						continue;
					}
					res.addChild(new RealFile(f));
				}
				if (f.isDirectory()) {
					boolean add = true;
					if (config.isHideEmptyFolders()) {
						add = FileUtil.isFolderRelevant(f, config, fullyPlayedEntries);
					}
					if (add) {
						res.addChild(new MonitorEntry(f, this));
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

	public void stopped(DLNAResource res) {
		if (!(res instanceof RealFile)) {
			return;
		}

		RealFile rf = (RealFile) res;

		// The total video duration in seconds
		double fileDuration = 0;
		if (res.getMedia() != null && (res.getMedia().isAudio() || res.getMedia().isVideo())) {
			fileDuration = res.getMedia().getDurationInSeconds();
		}

		/**
		 * Time since the file started playing.
		 * This is not a great way to get this value because if the
		 * video is paused, it will no longer be accurate.
		 */
		double elapsed;
		if (res.getLastStartPosition() == 0) {
			elapsed = (System.currentTimeMillis() - res.getStartTime()) / 1000;
		} else {
			elapsed = (System.currentTimeMillis() - res.getLastStartSystemTime()) / 1000;
			elapsed += res.getLastStartPosition();
		}

		FullyPlayedAction fullyPlayedAction = configuration.getFullyPlayedAction();

		if (!fullyPlayedAction.equals(FullyPlayedAction.NO_ACTION)) {
			LOGGER.trace("Fully Played feature logging:");
			LOGGER.trace("   duration: " + fileDuration);
			LOGGER.trace("   getLastStartPosition: " + res.getLastStartPosition());
			LOGGER.trace("   getStartTime: " + res.getStartTime());
			LOGGER.trace("   getLastStartSystemTime: " + res.getLastStartSystemTime());
			LOGGER.trace("   elapsed: " + elapsed);
			LOGGER.trace("   minimum play time needed: " + (fileDuration * configuration.getResumeBackFactor()));
		}

		/**
		 * Only mark the file as fully played if more than 92% (default) of
		 * the duration has elapsed since it started playing.
		 */
		if (
			(
				res.getMedia() != null &&
				res.getMedia().isImage()
			) ||
			(
				fileDuration > configuration.getMinimumWatchedPlayTimeSeconds() &&
				elapsed >= (fileDuration * configuration.getResumeBackFactor())
			)
		) {
			DLNAResource tmp = res.getParent();
			if (tmp != null) {
				boolean isMonitored = false;
				File[] foldersMonitored = PMS.get().getSharedFoldersArray(true);
				if (foldersMonitored != null && foldersMonitored.length > 0) {
					for (File folderMonitored : foldersMonitored) {
						if (rf.getFile().getAbsolutePath().contains(folderMonitored.getAbsolutePath())) {
							isMonitored = true;
						}
					}
				}

				if (isMonitored) {
					// Prevent duplicates from being added
					if (isFullyPlayed(rf.getFile().getAbsolutePath())) {
						return;
					}

					fullyPlayedEntries.add(rf.getFile().getAbsolutePath());
					setDiscovered(false);
					getChildren().clear();

					try {
						dumpFile();
					} catch (IOException e) {
						LOGGER.debug("An error occurred when dumping monitor file: " + e);
					}

					File playedFile = new File(rf.getFile().getAbsolutePath());

					if (fullyPlayedAction == FullyPlayedAction.MOVE_FOLDER) {
						// Move the video to a different folder
						String newDirectory = FileUtil.appendPathSeparator(configuration.getFullyPlayedOutputDirectory());

						if (playedFile.renameTo(new File(newDirectory + playedFile.getName()))) {
							LOGGER.debug("Moved {} because it has been fully played", playedFile.getName());
						} else {
							LOGGER.debug("Moving {} failed, trying again in 3 seconds", playedFile.getName());

							try {
								Thread.sleep(3000);

								if (playedFile.renameTo(new File(newDirectory + playedFile.getName()))) {
									LOGGER.debug("Moved {} because it has been fully played", playedFile.getName());
								} else {
									LOGGER.info("Failed to move {}", playedFile.getName());
								}
							} catch (InterruptedException e) {
								LOGGER.warn("Abandoning moving of {} because the thread was interrupted, probably due to UMS shutdown", e.getMessage());
								LOGGER.trace("", e);
								Thread.currentThread().interrupt();
							}
						}
					} else if (fullyPlayedAction == FullyPlayedAction.MOVE_TRASH) {
						try {
							if (Platform.isLinux()) {
								FreedesktopTrash.moveToTrash(playedFile);
							} else {
								FileUtils.getInstance().moveToTrash(Arrays.array(playedFile));
							}
						} catch (IOException | FileUtil.InvalidFileSystemException e) {
							LOGGER.warn("Failed to move file \"{}\" to recycler/trash after it has been fully played: {}", playedFile.getAbsoluteFile(), e.getMessage());
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

	public static boolean isFullyPlayed(String str) {
		return fullyPlayedEntries != null && fullyPlayedEntries.contains(str);
	}

	/**
	 * Populates UMS.mon with a list of completely played media.
	 *
	 * @throws IOException
	 */
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
			for (String str : fullyPlayedEntries) {
				if (sb.indexOf(str) == -1) {
					sb.append("entry=");
					sb.append(str);
					sb.append("\n");
				}
			}
			out.write(sb.toString());
			out.flush();
		}
	}

	@Override
	public void doRefreshChildren() {
		setUpdateId(this.getIntId());
	}
}
