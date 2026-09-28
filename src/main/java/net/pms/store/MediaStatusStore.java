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
import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.pms.Messages;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.gui.GuiManager;
import net.pms.media.MediaStatus;

public class MediaStatusStore {

	private static final Map<Integer, Map<String, MediaStatus>> STORE = new HashMap<>();
	private static final Map<StatusKey, StatusLoadLock> LOAD_LOCKS = new HashMap<>();

	private record StatusKey(int userId, String filename) { }

	private static final class StatusLoadLock {
		private int users;
	}


	private MediaStatusStore() {
		//should not be instantiated
	}

	public static MediaStatus getMediaStatus(int userId, String filename) {
		return getMediaStatus(userId, filename, () -> loadMediaStatus(userId, filename));
	}

	static MediaStatus getMediaStatus(int userId, String filename, Supplier<MediaStatus> loader) {
		MediaStatus stored = getStoredMediaStatus(userId, filename);
		if (stored != null) {
			return stored;
		}
		StatusKey key = new StatusKey(userId, filename);
		StatusLoadLock lock;
		synchronized (LOAD_LOCKS) {
			lock = LOAD_LOCKS.computeIfAbsent(key, ignored -> new StatusLoadLock());
			lock.users++;
		}
		try {
			synchronized (lock) {
				stored = getStoredMediaStatus(userId, filename);
				if (stored != null) {
					return stored;
				}
				MediaStatus loaded = loader.get();
				if (loaded == null) {
					loaded = new MediaStatus();
				}
				synchronized (STORE) {
					MediaStatus known = getStoredMediaStatusLocked(userId, filename);
					if (known != null) {
						return known;
					}
					STORE.computeIfAbsent(userId, id -> new HashMap<>()).put(filename, loaded);
					return loaded;
				}
			}
		} finally {
			synchronized (LOAD_LOCKS) {
				if (--lock.users == 0) {
					LOAD_LOCKS.remove(key);
				}
			}
		}
	}

	private static MediaStatus loadMediaStatus(int userId, String filename) {
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			return connection == null ? null : MediaTableFilesStatus.getMediaStatus(connection, filename, userId);
		} finally {
			MediaDatabase.close(connection);
		}
	}

	private static MediaStatus getStoredMediaStatus(int userId, String filename) {
		synchronized (STORE) {
			return getStoredMediaStatusLocked(userId, filename);
		}
	}

	private static MediaStatus getStoredMediaStatusLocked(int userId, String filename) {
		Map<String, MediaStatus> userStatus = STORE.get(userId);
		return userStatus != null ? userStatus.get(filename) : null;
	}

	/**
	 * Checks if {@code filename} is registered as fully played.
	 *
	 * @param filename the full path to the file whose status to retrieve.
	 * @return {@code true} if {@code filename} is fully played,
	 *         {@code false} otherwise.
	 */
	public static boolean isFullyPlayed(String filename, int userId) {
		MediaStatus mediaStatus = getMediaStatus(userId, filename);
		return mediaStatus.isFullyPlayed();
	}

	/**
	 * Sets the fully played status of the given {@code filename} both in
	 * the memory cache and in the database.
	 *
	 * @param filename the full path to the file in question.
	 * @param isFullyPlayed {@code true} if {@code fullPathToFile} is fully
	 *            played, {@code false} otherwise.
	 * @param lastPlaybackPosition how many seconds were played
	 */
	public static void setFullyPlayed(String filename, int userId, boolean isFullyPlayed, Double lastPlaybackPosition) {
		//update store
		List<SharedContent> sharedContents = SharedContentConfiguration.getSharedContentArray();
		for (SharedContent sc : sharedContents) {
			if (sc instanceof FolderContent folder) {
				File folderFile = folder.getFile();
				if (folderFile != null && filename.startsWith(folderFile.getAbsolutePath())) {
					if (!folder.isMonitored()) {
						return;
					}
				}
			}
		}
		MediaStatus mediaStatus = getMediaStatus(userId, filename);
		mediaStatus.setFullyPlayed(isFullyPlayed);
		if (lastPlaybackPosition != null) {
			mediaStatus.setLastPlaybackPosition(lastPlaybackPosition);
			mediaStatus.setPlaybackCount(mediaStatus.getPlaybackCount() + 1);
		}
		//update db
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFilesStatus.setFullyPlayed(connection, filename, userId, isFullyPlayed);
				if (lastPlaybackPosition != null) {
					MediaTableFilesStatus.setLastPlayed(connection, filename, userId, lastPlaybackPosition);
				}
			}
			MediaStoreIds.incrementUpdateIdForFilename(connection, filename);
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Sets the last played position of the given {@code filename} both in
	 * the memory cache and in the database.
	 *
	 * @param filename the full path to the file in question.
	 * @param lastPlaybackPosition how many seconds were played
	 */
	public static void setLastPlayed(String filename, int userId, Double lastPlaybackPosition) {
		if (lastPlaybackPosition != null) {
			//update store
			MediaStatus mediaStatus = getMediaStatus(userId, filename);
			mediaStatus.setLastPlaybackPosition(lastPlaybackPosition);
			mediaStatus.setPlaybackCount(mediaStatus.getPlaybackCount() + 1);
			//update db
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					MediaTableFilesStatus.setLastPlayed(connection, filename, userId, lastPlaybackPosition);
				}
				MediaStoreIds.incrementUpdateIdForFilename(connection, filename);
			} finally {
				MediaDatabase.close(connection);
			}
		}
	}

	public static void setBookmark(final String filename, final int userId, final int bookmark) {
		//update store
		MediaStatus mediaStatus = getMediaStatus(userId, filename);
		mediaStatus.setBookmark(bookmark);
		//update db
		Connection connection = null;
		try {
			connection = MediaDatabase.getConnectionIfAvailable();
			if (connection != null) {
				MediaTableFilesStatus.setBookmark(connection, filename, userId, bookmark);
			}
		} finally {
			MediaDatabase.close(connection);
		}
	}

	/**
	 * Sets whether each file within the folder is fully played.
	 *
	 * @param connection the db connection
	 * @param fullPathToFolder the full path to the folder.
	 * @param isFullyPlayed whether to mark the folder content as fully played
	 *            or not fully played.
	 */
	public static void setDirectoryFullyPlayed(final Connection connection, final String fullPathToFolder, final int userId, final boolean isFullyPlayed) {
		String statusLineString = isFullyPlayed ? Messages.getString("MarkContentsFullyPlayed") : Messages.getString("MarkContentsUnplayed");
		GuiManager.setStatusLine(statusLineString + ": " + fullPathToFolder);

		try {
			for (String fullPathToFile : MediaTableFiles.getFilenamesInFolder(connection, fullPathToFolder)) {
				setFullyPlayed(fullPathToFile, userId, isFullyPlayed, null);
			}
		} finally {
			GuiManager.setStatusLine(null);
		}

		MediaStoreIds.incrementSystemUpdateId();
	}

	public static boolean removeMediaEntriesInFolder(String pathToFolder) {
		boolean removed = false;
		synchronized (STORE) {
			for (int userId : STORE.keySet()) {
				if (STORE.get(userId) != null) {
					Iterator<String> filenames = STORE.get(userId).keySet().iterator();
					while (filenames.hasNext()) {
						if (filenames.next().startsWith(pathToFolder)) {
							filenames.remove();
							removed = true;
						}
					}
				}
			}
		}
		return removed;
	}

	public static boolean removeMediaEntry(String filename) {
		boolean removed = false;
		synchronized (STORE) {
			for (int userId : STORE.keySet()) {
				if (STORE.get(userId) != null && STORE.get(userId).remove(filename) != null) {
					removed = true;
				}
			}
		}
		return removed;
	}

	public static void clear(int userId) {
		synchronized (STORE) {
			if (STORE.containsKey(userId) && STORE.get(userId) != null) {
				STORE.get(userId).clear();
			}
		}
	}

	public static void clear() {
		synchronized (STORE) {
			STORE.clear();
		}
	}

}
