package net.pms.dlna;

import com.sun.jna.platform.FileUtils;
import com.sun.jna.Platform;
import java.io.*;
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
import org.apache.commons.lang.StringUtils;
import org.fest.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaMonitor extends VirtualFolder {
	private static Set<String> watchedEntries;
	private File[] dirs;
	private PmsConfiguration config;
	private static final FileUtils FILE_UTILS = FileUtils.getInstance();
	private static final Logger LOGGER = LoggerFactory.getLogger(MediaMonitor.class);

	public MediaMonitor(File[] dirs) {
		super(Messages.getString("VirtualFolder.2"), "images/thumbnail-folder-256.png");
		this.dirs = dirs;
		watchedEntries = new HashSet<>();
		config = PMS.getConfiguration();
		parseMonitorFile();
	}

	private File monitorFile() {
		return new File(config.getDataFile("UMS.mon"));
	}

	private void parseMonitorFile() {
		File f = monitorFile();
		if (!f.exists()) {
			return;
		}
		try {
			try (BufferedReader in = new BufferedReader(new FileReader(f))) {
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
						watchedEntries.add(entry.trim());
					}
				}
			}
			dumpFile();
		} catch (IOException e) {
		}
	}

	public void scanDir(File[] files, final DLNAResource res) {
		final DLNAResource mm = this;
		res.addChild(new VirtualVideoAction(Messages.getString("PMS.150"), true) {
			@Override
			public boolean enable() {
				for (DLNAResource r : res.getChildren()) {
					if (!(r instanceof RealFile)) {
						continue;
					}
					RealFile rf = (RealFile) r;
					watchedEntries.add(rf.getFile().getAbsolutePath());
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
				if (isWatched(f.getAbsolutePath())) {
					continue;
				}
				res.addChild(new RealFile(f));
			}
			if (f.isDirectory()) {
				boolean add = true;
				if (config.isHideEmptyFolders()) {
					add = FileUtil.isFolderRelevant(f, config, watchedEntries);
				}
				if (add) {
					res.addChild(new MonitorEntry(f, this));
				}
			}
		}
	}

	@Override
	public void discoverChildren() {
		for (File f : dirs) {
			scanDir(f.listFiles(), this);
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
			fileDuration = res.getMedia().getDuration();
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

		int fullyPlayedAction = configuration.getFullyPlayedAction();

		LOGGER.trace("Fully Played feature logging:");
		LOGGER.trace("   duration: " + fileDuration);
		LOGGER.trace("   getLastStartPosition: " + res.getLastStartPosition());
		LOGGER.trace("   getStartTime: " + res.getStartTime());
		LOGGER.trace("   getLastStartSystemTime: " + res.getLastStartSystemTime());
		LOGGER.trace("   elapsed: " + elapsed);
		LOGGER.trace("   minimum play time needed: " + (fileDuration * configuration.getResumeBackFactor()));

		/**
		 * Only mark the video as watched if more than 92% (default) of
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
					if (isWatched(rf.getFile().getAbsolutePath())) {
						return;
					}

					watchedEntries.add(rf.getFile().getAbsolutePath());
					setDiscovered(false);
					getChildren().clear();

					try {
						dumpFile();
					} catch (IOException e) {
						LOGGER.debug("An error occurred when dumping monitor file: " + e);
					}

					File watchedFile = new File(rf.getFile().getAbsolutePath());

					if (fullyPlayedAction == 3) {
						// Move the video to a different folder
						String newDirectory = configuration.getFullyPlayedOutputDirectory();
						if (!newDirectory.endsWith("\\")) {
							newDirectory += "\\\\";
						}

						try {
							if (watchedFile.renameTo(new File(newDirectory + watchedFile.getName()))) {
								LOGGER.debug("Moved {} because it has been fully played", watchedFile.getName());
							} else {
								LOGGER.debug("Moving {} failed, trying again in 3 seconds", watchedFile.getName());

								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
									LOGGER.error(null, e);
								}

								if (watchedFile.renameTo(new File(newDirectory + watchedFile.getName()))) {
									LOGGER.debug("Moved {} because it has been fully played", watchedFile.getName());
								} else {
									LOGGER.info("Failed to move {}", watchedFile.getName());
								}
							}
						} catch (Exception e) {
							LOGGER.info("Failed to move {} because {}", watchedFile.getName(), e.getMessage());
						}
					} else if (fullyPlayedAction == 4) {
						try {
							if (Platform.isLinux()) {
								FreedesktopTrash.moveToTrash(watchedFile);
							} else {
								FILE_UTILS.moveToTrash(Arrays.array(watchedFile));
							}
						} catch (IOException | FileUtil.InvalidFileSystemException e) {
							LOGGER.error(String.format("Failed to send the file %s to the trash after it has been fully played. Please enable the functionality on your operating system.", watchedFile.getAbsoluteFile()));
							LOGGER.trace("The error was", e);
						}
					}
				}
			}
			LOGGER.trace("   final decision: fully played");
		} else {
			LOGGER.trace("   final decision: not fully played");
		}
	}

	public static boolean isWatched(String str) {
		return watchedEntries != null && watchedEntries.contains(str);
	}

	/**
	 * Populates UMS.mon with a list of completely watched videos.
	 *
	 * @throws IOException 
	 */
	private void dumpFile() throws IOException {
		File f = monitorFile();
		Date now = new Date();
		try (FileWriter out = new FileWriter(f)) {
			StringBuilder sb = new StringBuilder();
			sb.append("######\n");
			sb.append("## NOTE!!!!!\n");
			sb.append("## This file is auto generated\n");
			sb.append("## Edit with EXTREME care\n");
			sb.append("## Generated: ");
			sb.append(now.toString());
			sb.append("\n");
			for (String str : watchedEntries) {
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
}
