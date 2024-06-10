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
import net.pms.database.MediaTableContainerFiles;
import net.pms.database.MediaTableFiles;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.renderers.Renderer;
import net.pms.store.container.CueFolder;
import net.pms.store.container.UmsPlaylist;
import net.pms.store.container.PlaylistFolder;
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

	public static String addEntryToPlaylist(StoreResource entry, PlaylistFolder playlistFolder) throws Exception {
		Path playlistPath = getPlaylistPathFromObjectId(playlistFolder);
		String filenameToAdd = entry.getFileName();
		String relativeEntryPath;
		if (FileUtil.isUrl(filenameToAdd)) {
			relativeEntryPath = filenameToAdd;
		} else {
			relativeEntryPath = calculateRelativeEntryPath(Paths.get(filenameToAdd), playlistPath);
		}
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);
		if (isEntryAlreadyInPlaylist(filenameToAdd, relativeEntryPath, playlistEntries)) {
			LOGGER.trace("entry already in playlist " + relativeEntryPath);
			return null;
		} else {
			playlistEntries.add(relativeEntryPath);
			writePlaylistToDisk(playlistEntries, playlistPath);
			StoreResource newPlaylistEntry = playlistFolder.getDefaultRenderer().getMediaStore().createResourceFromFile(new File(filenameToAdd));
			playlistFolder.addChild(newPlaylistEntry);
			MediaStoreIds.incrementUpdateIdForFilename(playlistPath.toString());
			return newPlaylistEntry.getId();
		}
	}

	private static boolean isEntryAlreadyInPlaylist(String absoluteEntryPath, String relativeEntryPath, List<String> playlistEntries) {
		return playlistEntries.contains(relativeEntryPath) || playlistEntries.contains(absoluteEntryPath);
	}

	public static boolean removeEntryFromPlaylist(StoreResource entry, PlaylistFolder playlistFolder) throws IOException {
		Path playlistPath = getPlaylistPathFromObjectId(playlistFolder);
		String filenameToRemove = entry.getFileName();

		String relativePath;
		if (FileUtil.isUrl(filenameToRemove)) {
			relativePath = filenameToRemove;
		} else {
			relativePath = calculateRelativeEntryPath(Paths.get(filenameToRemove), playlistPath);
		}
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);

		if (playlistEntries.remove(filenameToRemove) || playlistEntries.remove(relativePath)) {
			writePlaylistToDisk(playlistEntries, playlistPath);
			MediaStoreIds.incrementUpdateIdForFilename(playlistPath.toString());
			Long containerId = MediaTableFiles.getFileId(playlistPath.toString());
			Long entryId = MediaTableFiles.getFileId(filenameToRemove);
			MediaTableContainerFiles.deleteContainerEntry(containerId, entryId);
			return true;
		} else {
			LOGGER.debug("deleting failed because entry was not found in playlist. entry URL absolute / relative : {} / {}", filenameToRemove, relativePath);
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

	protected static String calculateRelativeEntryPath(Path absoluteEntryPath, Path absolutePlaylistPath) {
		StringBuilder sb = new StringBuilder();

		if (isEntryInSubfolderOfPlaylist(absoluteEntryPath, absolutePlaylistPath)) {
			String relativePath = removeSameParentPathFromEntryPath(absoluteEntryPath, absolutePlaylistPath);
			sb.append(".");
			if (!relativePath.startsWith(File.separator)) {
				sb.append(File.separator);
			}
			sb.append(relativePath);
		} else {
			Path commonParent = findFirstCommonParentFolder(absoluteEntryPath, absolutePlaylistPath, sb);
			String relativePath = removeCommonParentPathFromEntryPath(absoluteEntryPath, commonParent);
			sb.append(relativePath);
			return sb.toString();
		}
		return sb.toString();
	}

	private static String removeCommonParentPathFromEntryPath(Path absoluteEntryPath, Path commonParent) {
		return absoluteEntryPath.toString().substring(commonParent.toString().length() + 1);
	}

	private static Path findFirstCommonParentFolder(Path absoluteEntryPath, Path absolutePlaylistPath, StringBuilder sb) {
		Path commonRoot = absolutePlaylistPath.getParent();
		do {
			sb.append("..");
			sb.append(File.separator);
			commonRoot = commonRoot.getParent();
		} while (!absoluteEntryPath.toString().startsWith(commonRoot.toString()));
		return commonRoot;
	}

	private static String removeSameParentPathFromEntryPath(Path absoluteEntryPath, Path absolutePlaylistPath) {
		return absoluteEntryPath.toString().substring(absolutePlaylistPath.getParent().toString().length());
	}

	private static boolean isEntryInSubfolderOfPlaylist(Path absoluteEntryPath, Path absolutePlaylistPath) {
		return absoluteEntryPath.toString().startsWith(absolutePlaylistPath.getParent().toString());
	}

	private static void writePlaylistToDisk(List<String> lines, Path playlistFile) throws IOException {
		Files.write(playlistFile, lines);
	}

	public static boolean deletePlaylistFromDisk(PlaylistFolder playlistFolder) throws Exception {
		playlistFolder.getParent().removeChild(playlistFolder);
		if (playlistFolder.getPlaylistfile() != null) {
			if (playlistFolder.getPlaylistfile().delete()) {
				MediaTableFiles.removeEntry(playlistFolder.getPlaylistfile().getAbsolutePath());
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

	public static StoreContainer getPlaylist(Renderer renderer, String name, String uri, int type) {
		String ext = FileUtil.getUrlExtension(uri);
		if (StringUtils.isBlank(ext)) {
			return null;
		}
		Format f = FormatFactory.getAssociatedFormat("." + ext);
		if (f != null && f.getType() == Format.PLAYLIST) {
			switch (f.getMatchedExtension()) {
				case "m3u", "m3u8", "pls" -> {
					return new PlaylistFolder(renderer, name, uri, type);
				}
				case "cue" -> {
					return FileUtil.isUrl(uri) ? null : new CueFolder(renderer, new File(uri));
				}
				case "ups" -> {
					return new UmsPlaylist(renderer, name, uri);
				}
				default -> {
					//nothing to do
				}
			}
		}
		return null;
	}

}
