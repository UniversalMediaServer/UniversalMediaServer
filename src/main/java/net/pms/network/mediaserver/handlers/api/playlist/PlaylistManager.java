package net.pms.network.mediaserver.handlers.api.playlist;

import java.io.File;
import java.io.IOException;
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
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.database.MediaDatabase;

public class PlaylistManager {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistManager.class.getName());

	private List<Path> availablePlaylists = new ArrayList<>();
	private List<String> playlistsNames = new ArrayList<>();
	private boolean serviceDisabled = true;
	private MediaDatabase db = PMS.get().getMediaDatabase();

	public PlaylistManager() {
		checkPlaylistDirectoryConfiguration();
	}

	private void checkPlaylistDirectoryConfiguration() {
		LOG.trace("Playlist directory is set to : " + PMS.getConfiguration().getManagedPlaylistFolder());
		if (StringUtils.isAllBlank(PMS.getConfiguration().getManagedPlaylistFolder())) {
			LOG.info("Playlist directory not set. Playlist management is disabled.");
			return;
		}

		try {
			Path dir = Paths.get(PMS.getConfiguration().getManagedPlaylistFolder());
			if (dir.toFile().isDirectory()) {
				Files.newDirectoryStream(dir, path -> isValidPlaylist(path.toString())).forEach(p -> addPlaylist(p));
				serviceDisabled = false;
				return;
			} else {
				LOG.debug("Invalid playlist directory. Playlist management is disabled.");
			}
		} catch (Exception e) {
			LOG.warn("Error while scanning Playlist files from disc.", e);
		}
	}

	public boolean isServiceEnabled() {
		return !serviceDisabled;
	}

	public List<String> getAvailablePlaylistNames() {
		return playlistsNames;
	}

	private void addPlaylist(Path p) {
		availablePlaylists.add(p);
		String name = p.getName(p.getNameCount() - 1).toString();
		name = name.substring(0, name.indexOf('.'));
		playlistsNames.add(name.toLowerCase());
	}

	private Path getPlaylistPathFromName(String playlistName) {
		String baseName = FilenameUtils.getBaseName(playlistName);
		if (playlistsNames.indexOf(baseName.toLowerCase()) > -1) {
			return availablePlaylists.get(playlistsNames.indexOf(baseName.toLowerCase()));
		}
		throw new RuntimeException(Messages.getString("Api.Playlist.PlaylistNotManaged") + " : " + playlistName);
	}

	public List<String> addSongToPlaylist(Integer audiotrackID, String playlistName) throws SQLException, IOException {
		Path playlistPath = getPlaylistPathFromName(playlistName);
		String filenameToAdd = getFilenameFromId(audiotrackID);

		if (StringUtils.isAllBlank(filenameToAdd)) {
			throw new RuntimeException(Messages.getString("Api.Playlist.AudiotrackIdUnknown") + " : " + audiotrackID);
		}
		if (playlistName == null) {
			throw new RuntimeException(Messages.getString("Api.Playlist.PlaylistNotProvided"));
		}

		String relativeSongPath = calculateRelativeSongPath(Paths.get(filenameToAdd), playlistPath);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);
		if (isSongAlreadyInPlaylist(filenameToAdd, relativeSongPath, playlistEntries)) {
			LOG.trace("song already in playlist " + relativeSongPath);
			throw new RuntimeException(Messages.getString("Api.Playlist.SongAlredyInPlaylist") + ". ID : " + audiotrackID);
		} else {
			playlistEntries.add(relativeSongPath);
			writePlaylistToDisk(playlistEntries, playlistPath);
		}
		return playlistEntries;
	}

	private String getFilenameFromId(Integer audiotrackId) throws SQLException {
		try (Connection connection = db.getConnection()) {
			String sql = "select FILENAME from FILES as F join AUDIOTRACKS as A on F.ID = A.FILEID where (audiotrack_id = ?)";
			PreparedStatement ps = connection.prepareStatement(sql);
			ps.setInt(1, audiotrackId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getString(1);
			}
			throw new RuntimeException(Messages.getString("Api.Playlist.AudiotrackIdUnknown") + " : " + audiotrackId);
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
		} else {
			throw new RuntimeException(Messages.getString("Api.Playlist.SongNotInPlaylist") + " : " + audiotrackID);
		}
		return playlistEntries;
	}

	private List<String> readCurrentPlaylist(Path playlistFile) {
		if (!Files.exists(playlistFile)) {
			throw new RuntimeException("Playlist does not exists: " + playlistFile.toString());
		}

		List<String> lines = new ArrayList<String>();
		try {
			lines = Files.readAllLines(playlistFile, StandardCharsets.UTF_8);
		} catch (IOException e) {
			LOG.error("readCurrentPlaylist", e);
		}
		return lines;
	}

	/**
	 *
	 * @param event
	 * @param absolutePlaylistFile
	 * @return
	 */
	private String calculateRelativeSongPath(Path absoluteSongPath, Path absolutePlaylistFile) {
		StringBuilder sb = new StringBuilder();

		if (absoluteSongPath.toString().startsWith(absolutePlaylistFile.getParent().toString())) {
			String relativePath = absoluteSongPath.toString().substring(absolutePlaylistFile.getParent().toString().length());
			sb.append(".");
			if (!relativePath.startsWith(File.separator)) {
				sb.append(File.separator);
			}
			sb.append(relativePath);
		} else {
			Path commonRoot = absolutePlaylistFile.getParent();
			do {
				sb.append("..");
				sb.append(File.separator);
				commonRoot = commonRoot.getParent();
			} while (!absoluteSongPath.toString().startsWith(commonRoot.toString()));

			String relativePath = absoluteSongPath.toString().substring(commonRoot.toString().length() + 1);
			sb.append(relativePath);
			return sb.toString();
		}
		return sb.toString();
	}

	private void writePlaylistToDisk(List<String> lines, Path playlistFile) throws IOException {
		Files.write(playlistFile, lines);
	}

	public boolean isValidPlaylist(String filename) {
		if (filename.endsWith(".m3u")) {
			return true;
		}
		if (filename.endsWith(".m3u8")) {
			return true;
		}
		if (filename.endsWith(".pls")) {
			return true;
		}
		return false;
	}
}
