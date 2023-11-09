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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.database.MediaDatabase;
import net.pms.store.MediaScanner;
import net.pms.store.MediaStoreIds;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlaylistManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistManager.class.getName());

	private final List<Path> availablePlaylists = new ArrayList<>();
	private final List<String> playlistsNames = new ArrayList<>();
	private final List<PlaylistIdentVO> serverAccessiblePlaylists = new ArrayList<>();
	private final MediaDatabase db = PMS.get().getMediaDatabase();
	private boolean serviceDisabled = true;

	public PlaylistManager() {
		checkPlaylistDirectoryConfiguration();
	}

	private void checkPlaylistDirectoryConfiguration() {
		LOGGER.trace("Playlist directory is set to : " + PMS.getConfiguration().getManagedPlaylistFolder());
		if (StringUtils.isAllBlank(PMS.getConfiguration().getManagedPlaylistFolder())) {
			LOGGER.info("Playlist directory not set. Playlist management is disabled.");
			return;
		}

		availablePlaylists.clear();
		playlistsNames.clear();
		serverAccessiblePlaylists.clear();

		try {
			Path dir = Paths.get(PMS.getConfiguration().getManagedPlaylistFolder());
			if (dir.toFile().isDirectory()) {
				try (var dirStream = Files.newDirectoryStream(dir, path -> isValidPlaylist(path.toString()))) {
					dirStream.forEach(this::addPlaylist);
				}
				serviceDisabled = false;
			} else {
				LOGGER.debug("Invalid playlist directory. Playlist management is disabled.");
			}
		} catch (IOException e) {
			LOGGER.warn("Error while scanning Playlist files from disc.", e);
		}
	}

	public boolean isServiceEnabled() {
		return !serviceDisabled;
	}

	/**
	 * Returns discovered playlists name and ID from folder
	 * PMS.getConfiguration().getManagedPlaylistFolder() if playlist is cached
	 * in UMS database.
	 */
	public List<PlaylistIdentVO> getServerAccessiblePlaylists() {
		return serverAccessiblePlaylists;
	}

	/**
	 * Returns all discovered playlists name from folder
	 * PMS.getConfiguration().getManagedPlaylistFolder()
	 */
	public List<String> getAvailablePlaylistNames() {
		return playlistsNames;
	}

	private void addPlaylist(Path p) {
		availablePlaylists.add(p);
		String name = p.getName(p.getNameCount() - 1).toString();
		name = name.substring(0, name.indexOf('.'));
		playlistsNames.add(name.toLowerCase());
		discoverInLocalDatabase(p, name);
	}

	/**
	 * Discovers playlist in local database. If playlist is found it is marked
	 * as server accessible and can later be browsed by objectid "$DBID$PLAYLIST$" + databaseId.
	 */
	private void discoverInLocalDatabase(Path p, String name) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("discover databaseid for file : " + p.toFile().getAbsolutePath());
		}
		Integer databaseId = getDatabaseId(p.toFile().getAbsolutePath());
		if (databaseId != null) {
			serverAccessiblePlaylists.add(new PlaylistIdentVO(name, databaseId));
		}
	}

	private Integer getDatabaseId(String absolutePath) {
		try (Connection connection = db.getConnection()) {
			String sql = "select ID from FILES as F where (filename = ?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setString(1, absolutePath);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getInt(1);
					}
				}
			}
		} catch (SQLException e) {
			LOGGER.debug("exception while reading playlist id", e);
		}
		return null;
	}

	private Path getPlaylistPathFromName(String playlistName) {
		String baseName = FilenameUtils.getBaseName(playlistName);
		if (playlistsNames.indexOf(baseName.toLowerCase()) > -1) {
			return availablePlaylists.get(playlistsNames.indexOf(baseName.toLowerCase()));
		}
		throw new RuntimeException(Messages.getString("UnknownPlaylistName") + " : " + playlistName);
	}

	public List<String> addSongToPlaylist(Integer audiotrackID, String playlistName) throws SQLException, IOException {
		Path playlistPath = getPlaylistPathFromName(playlistName);
		String filenameToAdd = getFilenameFromId(audiotrackID);

		if (StringUtils.isAllBlank(filenameToAdd)) {
			throw new RuntimeException(Messages.getString("UnknownAudiotrackId") + " : " + audiotrackID);
		}
		if (playlistName == null) {
			throw new RuntimeException(Messages.getString("PlaylistNameNotProvided"));
		}

		String relativeSongPath = calculateRelativeSongPath(Paths.get(filenameToAdd), playlistPath);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);
		if (isSongAlreadyInPlaylist(filenameToAdd, relativeSongPath, playlistEntries)) {
			LOGGER.trace("song already in playlist " + relativeSongPath);
			throw new RuntimeException(Messages.getString("SongAlreadyInPlaylist") + ". ID : " + audiotrackID);
		} else {
			playlistEntries.add(relativeSongPath);
			writePlaylistToDisk(playlistEntries, playlistPath);
			try (Connection c = MediaDatabase.getConnectionIfAvailable()) {
				MediaStoreIds.incrementUpdateIdForFilename(c, playlistPath.toString());
			}
		}
		return playlistEntries;
	}

	private String getFilenameFromId(Integer audiotrackId) throws SQLException {
		try (Connection connection = db.getConnection()) {
			String sql = "select FILENAME from FILES as F join AUDIO_METADATA as A on F.ID = A.FILEID where (audiotrack_id = ?)";
			try (PreparedStatement ps = connection.prepareStatement(sql)) {
				ps.setInt(1, audiotrackId);
				try (ResultSet rs = ps.executeQuery()) {
					if (rs.next()) {
						return rs.getString(1);
					}
				}
			}
			throw new RuntimeException(Messages.getString("UnknownAudiotrackId") + " : " + audiotrackId);
		}
	}

	private boolean isSongAlreadyInPlaylist(String absoluteSongPath, String relativeSongPath, List<String> playlistEntries) {
		return playlistEntries.contains(relativeSongPath) || playlistEntries.contains(absoluteSongPath);
	}

	public List<String> removeSongFromPlaylist(Integer audiotrackID, String playlistName) throws SQLException, IOException {
		Path playlistPath = getPlaylistPathFromName(playlistName);
		String filenameToRemove = getFilenameFromId(audiotrackID);
		String relativePath = calculateRelativeSongPath(Paths.get(filenameToRemove), playlistPath);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);

		if (playlistEntries.remove(filenameToRemove) || playlistEntries.remove(relativePath)) {
			writePlaylistToDisk(playlistEntries, playlistPath);
			try (Connection c = MediaDatabase.getConnectionIfAvailable()) {
				MediaStoreIds.incrementUpdateIdForFilename(c, playlistPath.toString());
			}
		} else {
			throw new RuntimeException(Messages.getString("SongNotInPlaylist") + " : " + audiotrackID);
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

	public void createPlaylist(String playlistName) throws IOException {
		if (StringUtils.isAllBlank(playlistName)) {
			throw new RuntimeException(Messages.getString("NoPlaylistNameProvided"));
		}
		if (!FilenameUtils.getBaseName(playlistName).equals(playlistName)) {
			throw new RuntimeException(Messages.getString("DoNotProvidePathOrFileExtensions"));
		}
		if (playlistsNames.contains(playlistName)) {
			throw new RuntimeException(Messages.getString("PlaylistAlreadyExists"));
		}

		String absoluteNewFilename = FilenameUtils.concat(PMS.getConfiguration().getManagedPlaylistFolder(), playlistName + ".m3u8");
		File newPlaylist = new File(absoluteNewFilename);
		if (newPlaylist.exists()) {
			throw new RuntimeException(Messages.getString("PlaylistAlreadyExists"));
		}

		createNewEmptyPlaylistFile(newPlaylist);
		checkPlaylistDirectoryConfiguration();
		MediaScanner.scanFileOrFolder(PMS.getConfiguration().getManagedPlaylistFolder());
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
