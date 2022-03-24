package net.pms.network.mediaserver.handlers.api.playlist;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;

public class PlaylistManager {

	private static final Logger LOG = LoggerFactory.getLogger(PlaylistManager.class.getName());

	private List<Path> availablePlaylists = new ArrayList<>();
	private List<String> playlistsNames = new ArrayList<>();
	private final int uuidCharLen = 36;
	private boolean serviceDisabled = true;

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
		throw new RuntimeException("Playlist not managed : " + playlistName);
	}

	public List<String> addSongToPlaylist(String musicBrainzId, String playlistName) {
		Path playlistPath = getPlaylistPathFromName(playlistName);
		String songIndex = getBestSongFromSongList(getIndexedSongFromMBID(musicBrainzId));

		if (songIndex == null) {
			LOG.error("song not found " + musicBrainzId);
			throw new RuntimeException("song was not found for MBID : " + musicBrainzId);
		}
		if (playlistName == null) {
			LOG.error("playlist not found " + playlistName);
			throw new RuntimeException("playlist was not found. name : " + playlistName);
		}

		String entry = calculateRelativeSongPath(Paths.get(songIndex), playlistPath);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);
		if (isSongAlreadyInPlaylist(songIndex, entry, playlistEntries, playlistPath)) {
			LOG.debug("song already in playlist " + entry);
			throw new RuntimeException("Song already exists in playlist");
		} else {
			playlistEntries.add(entry);
			writePlaylistToDisk(playlistEntries, playlistPath);
		}
		return playlistEntries;
	}

	/**
	 * Checks if path is already in playlist or a song with the same musicBraint
	 * trackid.
	 *
	 * @param entry
	 * @param playlistEntries
	 * @return
	 */
	private boolean isSongAlreadyInPlaylist(String absolutePath, String entry, List<String> playlistEntries, Path playlistPath) {
		if (playlistEntries.contains(entry)) {
			return true;
		}
		Set<String> mbIdSet = new HashSet<String>();

		String entryId = selectMusicBrainzIDFromPath(absolutePath);
		if (entryId == null) {
			throw new RuntimeException("MusicBrainzID dissapeared for file : " + absolutePath);
		}
		String base = FilenameUtils.getFullPath(playlistPath.toFile().getAbsolutePath());
		for (String aPath : playlistEntries) {
			String absoluteFile = FilenameUtils.concat(base, aPath);
			mbIdSet.add(selectMusicBrainzIDFromPath(absoluteFile));
		}
		if (mbIdSet.contains(entryId)) {
			return true;
		}

		return false;
	}

	/**
	 * Reads the musicBrainzID for the absolute path
	 *
	 * @param absolutePath
	 * @return
	 */
	private String selectMusicBrainzIDFromPath(String absolutePath) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getBestSong(String musicBrainzId) {
		return getBestSongFromSongList(getIndexedSongFromMBID(musicBrainzId));
	}

	private String getBestSongFromSongList(List<String> songs) {
		if (songs.size() == 0) {
			return null;
		}
		if (songs.size() == 1) {
			return songs.get(0);
		}
		// TODO analyse filetype and bitrate ...
		return songs.get(0);
	}

	public void checkValidUUID(String musicBrainzID) {
		if ("00000000-0000-0000-0000-000000000000".equals(musicBrainzID) || musicBrainzID == null ||
			musicBrainzID.length() != uuidCharLen) {
			throw new RuntimeException(String.format("Invalid UUID : %s", musicBrainzID));
		}
	}

	/**
	 *
	 * @param musicBrainzId
	 * @return
	 */
	private List<String> getIndexedSongFromMBID(String musicBrainzId) {
		checkValidUUID(musicBrainzId);
		List<String> songIndexList = getSongByMusicBrainzId(musicBrainzId);
		if (songIndexList.size() == 0) {
			LOG.info("musicBrainzID not found in internal table : " + musicBrainzId);
		}
		return songIndexList;
	}

	/**
	 *
	 * @param musicBrainzId
	 * @return List with absolute file path
	 */
	private List<String> getSongByMusicBrainzId(String musicBrainzId) {
		return null;
	}

	public List<String> removeSongFromPlaylist(String musicBrainzId, String playlistName) {
		int numRemoved = 0;
		Path playlistPath = getPlaylistPathFromName(playlistName);
		List<String> songIndexList = getIndexedSongFromMBID(musicBrainzId);
		List<String> playlistEntries = readCurrentPlaylist(playlistPath);

		for (String filePath : songIndexList) {
			String entry = calculateRelativeSongPath(Paths.get(filePath), playlistPath);
			if (playlistEntries.remove(entry)) {
				numRemoved++;
			} else {
				LOG.debug("Song is not in playlist : " + entry != null ? entry : "NULL");
			}
		}
		if (numRemoved == 0) {
			throw new RuntimeException("Cannot remove song. Song is not in playlist : ");
		} else {
			writePlaylistToDisk(playlistEntries, playlistPath);
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

	private void writePlaylistToDisk(List<String> lines, Path playlistFile) {
		try {
			Files.write(playlistFile, lines);
		} catch (IOException e) {
			LOG.warn("cannot write internal media renderer playlist file.", e);
		}
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
