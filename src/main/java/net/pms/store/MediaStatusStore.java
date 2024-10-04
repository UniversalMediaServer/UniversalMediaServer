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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.pms.Messages;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableFilesStatus;
import net.pms.gui.GuiManager;
import net.pms.media.MediaStatus;

public class MediaStatusStore {

	private static final Map<Integer, Map<String, MediaStatus>> STORE = new HashMap<>();

	private MediaStatusStore() {
		//should not be instantiated
	}

	public static MediaStatus getMediaStatus(int userId, String filename) {
		synchronized (STORE) {
			if (STORE.containsKey(userId) && STORE.get(userId) != null && STORE.get(userId).containsKey(filename)) {
				return STORE.get(userId).get(filename);
			}
			MediaStatus mediaStatus = null;
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null) {
					mediaStatus = MediaTableFilesStatus.getMediaStatus(connection, filename, userId);
				}
			} finally {
				MediaDatabase.close(connection);
			}
			if (mediaStatus == null) {
				mediaStatus = new MediaStatus();
			}
			if (!STORE.containsKey(userId)) {
				STORE.put(userId, new HashMap<>());
			}
			STORE.get(userId).put(filename, mediaStatus);
			return mediaStatus;
		}
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
