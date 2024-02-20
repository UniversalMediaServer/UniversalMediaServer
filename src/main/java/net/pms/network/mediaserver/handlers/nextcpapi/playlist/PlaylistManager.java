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
package net.pms.network.mediaserver.handlers.nextcpapi.playlist;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jupnp.support.contentdirectory.DIDLParser;
import org.jupnp.support.model.DIDLContent;
import org.jupnp.support.model.item.PlaylistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.configuration.RendererConfigurations;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.CreateObjectResult;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Parser;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.Result;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.result.namespace.didl_lite.item.Item;
import net.pms.renderers.Renderer;
import net.pms.store.MediaStoreIds;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.container.PlaylistFolder;

public class PlaylistManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistManager.class.getName());

	// migration step towards UPnP handling
	private final Renderer renderer = RendererConfigurations.getDefaultRenderer();


	private Path getPlaylistPathFromObjectId(StoreContainer playlistFolder) {
		File pl = new File(playlistFolder.getSystemName());
		if (pl != null) {
			return pl.toPath();
		}
		throw new RuntimeException("cannot resolve PATH of playlist");
	}

	private PlaylistFolder getPlaylistContainer(String playlistObjectId) {
		StoreResource sr = renderer.getMediaStore().getResource(playlistObjectId);
		if (sr instanceof PlaylistFolder sc) {
			return sc;
		}
		return null;
	}

	private StoreContainer getStoreContainer(String storeContainerId) {
		StoreResource sr = renderer.getMediaStore().getResource(storeContainerId);
		if (sr instanceof StoreContainer sc) {
			return sc;
		}
		throw new RuntimeException("unknown parent container.");
	}

	public String addSongToPlaylist(String songObjectId, String playlistObjectId) throws SQLException, IOException {
		PlaylistFolder playlistFolder = getPlaylistContainer(playlistObjectId);
		Path playlistPath = getPlaylistPathFromObjectId(playlistFolder);
		String filenameToAdd = getFilenameFromSongObjectId(songObjectId);
		String relativeSongPath = calculateRelativeSongPath(Paths.get(filenameToAdd), playlistPath);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);
		if (isSongAlreadyInPlaylist(filenameToAdd, relativeSongPath, playlistEntries)) {
			LOGGER.trace("song already in playlist " + relativeSongPath);
			throw new RuntimeException(Messages.getString("SongAlreadyInPlaylist") + ". ID : " + songObjectId);
		} else {
			playlistEntries.add(relativeSongPath);
			writePlaylistToDisk(playlistEntries, playlistPath);
			StoreResource newPlaylistEntry = renderer.getMediaStore().createResourceFromFile(new File(filenameToAdd));
			playlistFolder.addChild(newPlaylistEntry);
			MediaStoreIds.incrementUpdateIdForFilename(playlistPath.toString());
			return newPlaylistEntry.getId();
		}
	}

	private String getFilenameFromSongObjectId(String songObjectId) throws SQLException {
		StoreResource sr = renderer.getMediaStore().getResource(songObjectId);
		if (sr != null) {
			return sr.getFileName();
		}
		throw new RuntimeException("Unknown soung objectId : " + songObjectId);
	}

	private boolean isSongAlreadyInPlaylist(String absoluteSongPath, String relativeSongPath, List<String> playlistEntries) {
		return playlistEntries.contains(relativeSongPath) || playlistEntries.contains(absoluteSongPath);
	}

	public List<String> removeSongFromPlaylist(String songObjectId) throws SQLException, IOException {
		StoreResource sr = renderer.getMediaStore().getResource(songObjectId);
		if (sr != null) {
			return removeSongFromPlaylist(songObjectId, sr.getParentId());
		}
		LOGGER.warn("songObjectId not found.");
		return null;
	}

	public List<String> removeSongFromPlaylist(String songObjectId, String playlistObjectId) throws SQLException, IOException {
		Path playlistPath = getPlaylistPathFromObjectId(getPlaylistContainer(playlistObjectId));
		String filenameToRemove = getFilenameFromSongObjectId(songObjectId);
		String relativePath = calculateRelativeSongPath(Paths.get(filenameToRemove), playlistPath);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);

		if (playlistEntries.remove(filenameToRemove) || playlistEntries.remove(relativePath)) {
			writePlaylistToDisk(playlistEntries, playlistPath);
			MediaStoreIds.incrementUpdateIdForFilename(playlistPath.toString());
		} else {
			throw new RuntimeException(Messages.getString("SongNotInPlaylist") + " : " + songObjectId);
		}
		return playlistEntries;
	}

	private List<String> readCurrentPlaylist(Path playlistFile) {
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

	private String calculateRelativeSongPath(Path absoluteSongPath, Path absolutePlaylistFile) {
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

	private String removeCommonParentPathFromSongFile(Path absoluteSongPath, Path commonParent) {
		return absoluteSongPath.toString().substring(commonParent.toString().length() + 1);
	}

	private Path findFirstCommonParentFolder(Path absoluteSongPath, Path absolutePlaylistFile, StringBuilder sb) {
		Path commonRoot = absolutePlaylistFile.getParent();
		do {
			sb.append("..");
			sb.append(File.separator);
			commonRoot = commonRoot.getParent();
		} while (!absoluteSongPath.toString().startsWith(commonRoot.toString()));
		return commonRoot;
	}

	private String removeSameParentPathFromSongPath(Path absoluteSongPath, Path absolutePlaylistFile) {
		return absoluteSongPath.toString().substring(absolutePlaylistFile.getParent().toString().length());
	}

	private boolean isSongInSubfolderOfPlaylist(Path absoluteSongPath, Path absolutePlaylistFile) {
		return absoluteSongPath.toString().startsWith(absolutePlaylistFile.getParent().toString());
	}

	private void writePlaylistToDisk(List<String> lines, Path playlistFile) throws IOException {
		Files.write(playlistFile, lines);
	}

	public CreateObjectResult createPlaylist(String parentContainerId, String elements) throws Exception {
		CreateObjectResult createResult = new CreateObjectResult();
		Parser parser = new Parser();
		Result modelItemToAdd = parser.parse(elements);
		Item itemToCreate = modelItemToAdd.getItems().get(0);
		if (itemToCreate != null) {
			if ("object.item.playlistItem".equalsIgnoreCase(itemToCreate.getUpnpClassName())) {
				String playlistName = modelItemToAdd.getItems().get(0).getTitle();
				if (StringUtils.isAllBlank(playlistName)) {
					throw new RuntimeException(Messages.getString("NoPlaylistNameProvided"));
				}
				String fullPath = FilenameUtils.concat(getStoreContainer(parentContainerId).getFileName(), playlistName);
				File newPlaylist = new File(fullPath);
				if (newPlaylist.exists()) {
					throw new RuntimeException(Messages.getString("PlaylistAlreadyExists"));
				}

				createNewEmptyPlaylistFile(newPlaylist);
				StoreResource newResource = renderer.getMediaStore().createResourceFromFile(newPlaylist);
				getStoreContainer(parentContainerId).addChild(newResource);


				PlaylistItem pi = new PlaylistItem();
				pi.setTitle(playlistName);
				pi.setParentID(newResource.getParentId());
				pi.setId(newResource.getId());

				DIDLParser didlParser = new DIDLParser();
				DIDLContent content = new DIDLContent();
				content.addItem(pi);
				String xml = didlParser.generate(content);
				createResult.setResult(xml);
				createResult.setObjectID(newResource.getId());
			} else {
				LOGGER.error("cannot create item of type {}", itemToCreate.getUpnpClassName());
			}
		} else {
			LOGGER.error("no item to create.");
		}
		return createResult;
	}

	private void createNewEmptyPlaylistFile(File newPlaylist) throws IOException {
		if (!newPlaylist.createNewFile()) {
			throw new RuntimeException(Messages.getString("PlaylistCanNotBeCreated"));
		}
		try (PrintWriter pw = new PrintWriter(newPlaylist)) {
			pw.println("#EXTM3U");
			pw.println();
		}
	}

	private boolean isValidPlaylist(String filename) {
		return (
			filename.endsWith(".m3u") ||
			filename.endsWith(".m3u8") ||
			filename.endsWith(".pls")
		);
	}

}
