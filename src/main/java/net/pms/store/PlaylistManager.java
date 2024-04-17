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
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import net.pms.Messages;
import net.pms.database.MediaTableWebResource;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.utils.WebStreamMetadataCollector;
import net.pms.util.FileUtil;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistManager.class.getName());

	// This class is not instantiable
	private PlaylistManager() {
	}

	private static Path getPlaylistPathFromObjectId(StoreContainer playlistFolder) {
		return new File(playlistFolder.getSystemName()).toPath();
	}

	public static String addSongToPlaylist(StoreResource songResource, PlaylistFolder playlistFolder) throws Exception {
		Path playlistPath = getPlaylistPathFromObjectId(playlistFolder);
		String filenameToAdd = songResource.getFileName();
		String relativeSongPath;
		if (FileUtil.isUrl(filenameToAdd)) {
			relativeSongPath = filenameToAdd;
		} else {
			relativeSongPath = calculateRelativeSongPath(Paths.get(filenameToAdd), playlistPath);
		}
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);
		if (isSongAlreadyInPlaylist(filenameToAdd, relativeSongPath, playlistEntries)) {
			LOGGER.trace("song already in playlist " + relativeSongPath);
			return null;
		} else {
			playlistEntries.add(relativeSongPath);
			writePlaylistToDisk(playlistEntries, playlistPath);
			StoreResource newPlaylistEntry = playlistFolder.getDefaultRenderer().getMediaStore().createResourceFromFile(new File(filenameToAdd));
			playlistFolder.addChild(newPlaylistEntry);
			MediaStoreIds.incrementUpdateIdForFilename(playlistPath.toString());
			if (FileUtil.isUrl(relativeSongPath)) {
				WebStreamMetadataCollector.getInstance().collectMetadata(relativeSongPath);
			}
			return newPlaylistEntry.getId();
		}
	}

	private static boolean isSongAlreadyInPlaylist(String absoluteSongPath, String relativeSongPath, List<String> playlistEntries) {
		return playlistEntries.contains(relativeSongPath) || playlistEntries.contains(absoluteSongPath);
	}

	public static boolean removeSongFromPlaylist(StoreResource songResource, PlaylistFolder playlistFolder) throws IOException {
		Path playlistPath = getPlaylistPathFromObjectId(playlistFolder);
		String filenameToRemove = songResource.getFileName();

		String relativePath;
		if (FileUtil.isUrl(filenameToRemove)) {
			relativePath = filenameToRemove;
		} else {
			relativePath = calculateRelativeSongPath(Paths.get(filenameToRemove), playlistPath);
		}
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);

		if (playlistEntries.remove(filenameToRemove) || playlistEntries.remove(relativePath)) {
			writePlaylistToDisk(playlistEntries, playlistPath);
			MediaStoreIds.incrementUpdateIdForFilename(playlistPath.toString());
			if (FileUtil.isUrl(filenameToRemove)) {
				LOGGER.debug("deleting WebResource from database {}", filenameToRemove);
				MediaTableWebResource.deleteByUrl(filenameToRemove);
			}
			return true;
		} else {
			LOGGER.debug("deleting failed because song resource was not found in playlist. resource URL absolute / relative : {} / {}", filenameToRemove, relativePath);
		}
		return false;
	}

	private static List<String> readCurrentPlaylist(Path playlistFile) {
		if (!Files.exists(playlistFile)) {
			throw new RuntimeException("Playlist does not exists: " + playlistFile.toString());
		}

		List<String> lines = new ArrayList<>();
		try {
			lines = Files.readAllLines(playlistFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOGGER.error("readCurrentPlaylist", e);
		}
		return lines;
	}

	protected static String calculateRelativeSongPath(Path absoluteSongPath, Path absolutePlaylistFile) {
		StringBuilder sb = new StringBuilder();

		if (isSongInSubfolderOfPlaylist(absoluteSongPath, absolutePlaylistFile)) {
			String relativePath = removeSameParentPathFromSongPath(absoluteSongPath, absolutePlaylistFile);
			sb.append(".");
			if (!relativePath.startsWith(File.separator)) {
				sb.append(File.separator);
			}
			sb.append(relativePath);
		} else {
			Path commonParent = findFirstCommonParentFolder(absoluteSongPath, absolutePlaylistFile, sb);
			String relativePath = removeCommonParentPathFromSongFile(absoluteSongPath, commonParent);
			sb.append(relativePath);
			return sb.toString();
		}
		return sb.toString();
	}

	private static String removeCommonParentPathFromSongFile(Path absoluteSongPath, Path commonParent) {
		return absoluteSongPath.toString().substring(commonParent.toString().length() + 1);
	}

	private static Path findFirstCommonParentFolder(Path absoluteSongPath, Path absolutePlaylistFile, StringBuilder sb) {
		Path commonRoot = absolutePlaylistFile.getParent();
		do {
			sb.append("..");
			sb.append(File.separator);
			commonRoot = commonRoot.getParent();
		} while (!absoluteSongPath.toString().startsWith(commonRoot.toString()));
		return commonRoot;
	}

	private static String removeSameParentPathFromSongPath(Path absoluteSongPath, Path absolutePlaylistFile) {
		return absoluteSongPath.toString().substring(absolutePlaylistFile.getParent().toString().length());
	}

	private static boolean isSongInSubfolderOfPlaylist(Path absoluteSongPath, Path absolutePlaylistFile) {
		return absoluteSongPath.toString().startsWith(absolutePlaylistFile.getParent().toString());
	}

	private static void writePlaylistToDisk(List<String> lines, Path playlistFile) throws IOException {
		Files.write(playlistFile, lines);
	}

	public static boolean deletePlaylistFromDisk(PlaylistFolder playlistFolder) throws Exception {
		List<String> playlistEntries = readCurrentPlaylist(playlistFolder.getPlaylistfile().toPath());

		playlistFolder.getParent().removeChild(playlistFolder);
		if (playlistFolder.getPlaylistfile() != null) {
			if (playlistFolder.getPlaylistfile().delete()) {
				for (String entry : playlistEntries) {
					if (FileUtil.isUrl(entry)) {
						LOGGER.debug("deleting WebResource from database {}", entry);
						MediaTableWebResource.deleteByUrl(entry);
					}
				}
				return true;
			} else {
				LOGGER.debug("deleting playlist failed. File permissions set? Location : {}", playlistFolder.getPlaylistfile().getAbsolutePath());
			}
		} else {
			LOGGER.debug("deleting playlist failed because it was not found. Location : {}", playlistFolder.getPlaylistfile().getAbsolutePath());
		}
		return false;
	}

	public static StoreResource createPlaylist(StoreContainer storeContainer, String playlistName) throws Exception {
		LOGGER.trace("creating playlist {} for parentcontainer {}", playlistName, storeContainer.getId());
		if (StringUtils.isAllBlank(playlistName)) {
			LOGGER.error(Messages.getString("NoPlaylistNameProvided"));
			return null;
		}
		if (!isValidPlaylist(playlistName)) {
			LOGGER.error("Playlist extension must end with '.pls', '.m3u' or '.m3u8'");
			return null;
		}
		String playlistFullPath = FilenameUtils.concat(storeContainer.getFileName(), playlistName);
		File newPlaylist = new File(playlistFullPath);
		if (newPlaylist.exists()) {
			LOGGER.error(Messages.getString("PlaylistAlreadyExists"));
			return null;
		}

		StoreResource newResource = null;
		if (createNewEmptyPlaylistFile(newPlaylist)) {
			LOGGER.trace("empty playlist created.");
			newResource = storeContainer.getDefaultRenderer().getMediaStore().createResourceFromFile(newPlaylist);
			storeContainer.addChild(newResource);
			LOGGER.trace("empty playlist has new ID of {}", newResource.getId());
		}
		return newResource;
	}

	private static boolean createNewEmptyPlaylistFile(File newPlaylist) throws IOException {
		if (newPlaylist.createNewFile()) {
			try (PrintWriter pw = new PrintWriter(newPlaylist)) {
				pw.println("#EXTM3U");
				pw.println();
			}
			return true;
		}
		LOGGER.error(Messages.getString("PlaylistCanNotBeCreated"));
		return false;
	}

	private static boolean isValidPlaylist(String filename) {
		return (filename.endsWith(".m3u") ||
				filename.endsWith(".m3u8") ||
				filename.endsWith(".pls"));
	}

}
