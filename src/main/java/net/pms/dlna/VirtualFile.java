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
package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.renderers.Renderer;
import net.pms.util.FileUtil;
import net.pms.util.UMSUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualFile extends MediaResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(VirtualFile.class);

	/**
	 * An array of {@link String}s that defines the lower-case representation of
	 * the file extensions that are eligible for evaluation as file or folder
	 * thumbnails.
	 */
	public static final Set<String> THUMBNAIL_EXTENSIONS = Set.of("jpeg", "jpg", "png");

	/**
	 * An array of {@link String}s that defines the file extensions that are
	 * never media so we should not attempt to parse.
	 */
	public static final Set<String> EXTENSIONS_DENYLIST = Set.of("!qB", "!ut", "1", "dmg", "exe");

	private final String forcedName;
	private final List<File> files;
	private final List<VirtualFolderContent> virtualFolders;

	private List<File> discoverable;
	private List<File> emptyFoldersToRescan;

	protected String name;
	private boolean addToMediaLibrary = true;
	private ArrayList<RealFile> searchList;
	private File potentialCover;

	public VirtualFile(Renderer renderer) {
		super(renderer);
		setLastModified(0);
		files = new ArrayList<>();
		virtualFolders = new ArrayList<>();
		forcedName = null;
	}

	public VirtualFile(Renderer renderer, VirtualFolderContent virtualFolder) {
		super(renderer);
		name = virtualFolder.getName();
		addToMediaLibrary = virtualFolder.isAddToMediaLibrary();
		files = virtualFolder.getFiles();
		virtualFolders = virtualFolder.getVirtualFolders();
		setLastModified(0);
		forcedName = null;
	}

	public VirtualFile(Renderer renderer, VirtualFile virtualFile, List<File> list, String forcedName) {
		super(renderer);
		addToMediaLibrary = virtualFile.isAddToMediaLibrary();
		files = virtualFile.getFiles();
		virtualFolders = new ArrayList<>();
		setLastModified(0);
		this.discoverable = list;
		this.forcedName = forcedName;
	}

	public List<File> getFiles() {
		return files;
	}

	private void manageFile(File f, boolean isAddGlobally) {
		if (f.isFile() || f.isDirectory()) {
			String lcFilename = f.getName().toLowerCase();

			if (!f.isHidden()) {
				if (configuration.isArchiveBrowsing() && (lcFilename.endsWith(".zip") || lcFilename.endsWith(".cbz"))) {
					addChild(new ZippedFile(defaultRenderer, f), true, isAddGlobally);
				} else if (configuration.isArchiveBrowsing() && (lcFilename.endsWith(".rar") || lcFilename.endsWith(".cbr"))) {
					addChild(new RarredFile(defaultRenderer, f), true, isAddGlobally);
				} else if (
					configuration.isArchiveBrowsing() && (
						lcFilename.endsWith(".tar") ||
						lcFilename.endsWith(".gzip") ||
						lcFilename.endsWith(".gz") ||
						lcFilename.endsWith(".7z")
					)
				) {
					addChild(new SevenZipFile(defaultRenderer, f), true, isAddGlobally);
				} else if (
					lcFilename.endsWith(".iso") ||
					lcFilename.endsWith(".img") || (
						f.isDirectory() &&
						f.getName().toUpperCase(Locale.ROOT).equals("VIDEO_TS")
					)
				) {
					addChild(new DVDISOFile(defaultRenderer, f), true, isAddGlobally);
				} else if (
					lcFilename.endsWith(".m3u") ||
					lcFilename.endsWith(".m3u8") ||
					lcFilename.endsWith(".pls") ||
					lcFilename.endsWith(".cue") ||
					lcFilename.endsWith(".ups")
				) {
					MediaResource d = PlaylistFolder.getPlaylist(defaultRenderer, lcFilename, f.getAbsolutePath(), 0);
					if (d != null) {
						addChild(d, true, isAddGlobally);
					}
				} else {
					ArrayList<String> ignoredFolderNames = configuration.getIgnoredFolderNames();

					/* Optionally ignore empty directories */
					if (f.isDirectory() && configuration.isHideEmptyFolders() && !FileUtil.isFolderRelevant(f, configuration)) {
						LOGGER.debug("Ignoring empty/non-relevant directory: " + f.getName());
						// Keep track of the fact that we have empty folders, so when we're asked if we should refresh,
						// we can re-scan the folders in this list to see if they contain something relevant
						if (emptyFoldersToRescan == null) {
							emptyFoldersToRescan = new ArrayList<>();
						}
						if (!emptyFoldersToRescan.contains(f)) {
							emptyFoldersToRescan.add(f);
						}
					} else if (f.isDirectory() && !ignoredFolderNames.isEmpty() && ignoredFolderNames.contains(f.getName())) {
						LOGGER.debug("Ignoring {} because it is in the ignored folders list", f.getName());
					} else {
						// Otherwise add the file
						RealFile rf = new RealFile(defaultRenderer, f);
						if (rf.length() == 0  && !rf.isFolder()) {
							LOGGER.debug("Ignoring {} because it seems corrupted when the length of the file is 0", f.getName());
							return;
						}

						//we need to propagate the flag in order to make all hierarchy stay outside the media library if needed
						rf.setAddToMediaLibrary(addToMediaLibrary);
						if (searchList != null) {
							searchList.add(rf);
						}
						addChild(rf, true, isAddGlobally);
					}
				}
			}
		}
	}

	private File getPath() {
		if (this instanceof RealFile realFile) {
			return realFile.getFile();
		}
		return null;
	}

	private List<File> getFilesListForDirectories() {
		List<File> out = new ArrayList<>();
		ArrayList<String> ignoredDirectoryNames = configuration.getIgnoredFolderNames();
		String directoryName;
		for (File directory : getFiles()) {
			directoryName = directory == null || directory.getName() == null ? "unnamed" : directory.getName();
			if (directory == null || !directory.isDirectory()) {
				LOGGER.trace("Ignoring {} because it is not a valid directory", directoryName);
				continue;
			}

			// Skip if ignored
			if (!ignoredDirectoryNames.isEmpty() && ignoredDirectoryNames.contains(directoryName)) {
				LOGGER.debug("Ignoring {} because it is in the ignored directories list", directoryName);
				continue;
			}

			if (directory.canRead()) {
				File[] listFiles = directory.listFiles((File parentDirectory, String file) -> {
					// Accept any directory
					Path path = Paths.get(parentDirectory + File.separator + file);
					if (Files.isDirectory(path)) {
						return true;
					}

					// We want to find only media files
					return isPotentialMediaFile(file);
				});

				if (listFiles == null) {
					LOGGER.warn("Can't read files from directory: {}", directory.getAbsolutePath());
				} else {
					out.addAll(Arrays.asList(listFiles));
				}
			} else {
				LOGGER.warn("Can't read directory: {}", directory.getAbsolutePath());
			}
		}

		return out;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public boolean analyzeChildren(int count) {
		return analyzeChildren(count, true);
	}

	@Override
	public boolean analyzeChildren(int count, boolean isAddGlobally) {
		int currentChildrenCount = getChildren().size();
		int vfolder = 0;
		FileSearch fs = null;
		if (!discoverable.isEmpty() && configuration.getSearchInFolder()) {
			searchList = new ArrayList<>();
			fs = new FileSearch(searchList);
			addChild(new SearchFolder(defaultRenderer, fs));
		}
		while (((getChildren().size() - currentChildrenCount) < count) || (count == -1)) {
			if (vfolder < virtualFolders.size()) {
				VirtualFolderContent virtualFolder = virtualFolders.get(vfolder);
				MediaResource parent = getSharedContentParent(virtualFolder.getParent());
				parent.addChild(new VirtualFile(defaultRenderer, virtualFolder), true, isAddGlobally);
				++vfolder;
			} else {
				if (discoverable.isEmpty()) {
					break;
				}
				manageFile(discoverable.remove(0), isAddGlobally);
			}
		}
		if (fs != null) {
			fs.update(searchList);
		}
		return discoverable.isEmpty();
	}

	@Override
	public void discoverChildren() {
		discoverChildren(null, true);
	}

	@Override
	public void discoverChildren(String str) {
		discoverChildren(str, true);
	}

	public void discoverChildren(String str, boolean isAddGlobally) {
		if (discoverable == null) {
			discoverable = new ArrayList<>();
		} else {
			return;
		}

		int sm = configuration.getSortMethod(getPath());

		List<File> childrenFiles = getFilesListForDirectories();

		// Build a map of all files and their corresponding formats
		Set<File> images = new HashSet<>();
		Set<File> audioVideo = new HashSet<>();
		Iterator<File> iterator = childrenFiles.iterator();
		while (iterator.hasNext()) {
			File file = iterator.next();
			if (file.isFile()) {
				if (isPotentialThumbnail(file.getName())) {
					if (isFolderThumbnail(file, false)) {
						potentialCover = file;
						iterator.remove();
					} else {
						images.add(file);
					}
				} else {
					Format format = FormatFactory.getAssociatedFormat(file.getAbsolutePath());
					if (format != null && (format.isAudio() || format.isVideo())) {
						audioVideo.add(file);
					}
				}
			}
		}

		// Remove cover/thumbnails from file list
		if (!images.isEmpty() && !audioVideo.isEmpty()) {
			Set<File> potentialMatches;
			for (File audioVideoFile : audioVideo) {
				potentialMatches = getPotentialFileThumbnails(audioVideoFile, false);
				iterator = images.iterator();
				while (iterator.hasNext()) {
					File imageFile = iterator.next();
					if (potentialMatches.contains(imageFile)) {
						iterator.remove();
						childrenFiles.remove(imageFile);
					}
				}
			}
		}

		// ATZ handling
		if (childrenFiles.size() > configuration.getATZLimit() && StringUtils.isEmpty(forcedName)) {
			/*
			 * Too many files to display at once, add A-Z folders
			 * instead and let the filters begin
			 *
			 * Note: If we done this at the level directly above we don't do it again
			 * since all files start with the same letter then
			 */
			Map<String, List<File>> map = new TreeMap<>();
			for (File f : childrenFiles) {
				if ((!f.isFile() && !f.isDirectory()) || f.isHidden()) {
					// skip these
					continue;
				}
				if (f.isDirectory() && configuration.isHideEmptyFolders() && !FileUtil.isFolderRelevant(f, configuration)) {
					LOGGER.debug("Ignoring empty/non-relevant directory: " + f.getName());
					// Keep track of the fact that we have empty folders, so when we're asked if we should refresh,
					// we can re-scan the folders in this list to see if they contain something relevant
					if (emptyFoldersToRescan == null) {
						emptyFoldersToRescan = new ArrayList<>();
					}
					if (!emptyFoldersToRescan.contains(f)) {
						emptyFoldersToRescan.add(f);
					}
					continue;
				}

				String filenameToSort = FileUtil.renameForSorting(f.getName(), false, f.getAbsolutePath());

				char c = filenameToSort.toUpperCase().charAt(0);

				if (!(c >= 'A' && c <= 'Z')) {
					// "other char"
					c = '#';
				}
				List<File> l = map.get(String.valueOf(c));
				if (l == null) {
					// new letter
					l = new ArrayList<>();
				}
				l.add(f);
				map.put(String.valueOf(c), l);
			}

			for (Entry<String, List<File>> entry : map.entrySet()) {
				// loop over all letters, this avoids adding
				// empty letters
				UMSUtils.sortFiles(entry.getValue(), sm);
				VirtualFile mf = new VirtualFile(defaultRenderer, this, entry.getValue(), entry.getKey());
				addChild(mf, true, isAddGlobally);
			}
			return;
		}

		UMSUtils.sortFiles(childrenFiles, (sm == UMSUtils.SORT_RANDOM ? UMSUtils.SORT_LOC_NAT : sm));

		for (File f : childrenFiles) {
			if (f.isDirectory()) {
				discoverable.add(f); // manageFile(f);
			}
		}

		// For random sorting, we only randomize file entries
		if (sm == UMSUtils.SORT_RANDOM) {
			UMSUtils.sortFiles(childrenFiles, sm);
		}

		for (File f : childrenFiles) {
			if (f.isFile()) {
				discoverable.add(f); // manageFile(f);
			}
		}
	}

	public void doRefreshChildren(String str, boolean isAddGlobally) {
		getChildren().clear();
		emptyFoldersToRescan = null; // Since we're re-scanning, reset this list so it can be built again
		discoverable = null;
		discoverChildren(str, isAddGlobally);
		analyzeChildren(-1, isAddGlobally);
	}

	/**
	 * @return the potentialCover
	 * @since 1.50
	 */
	public File getPotentialCover() {
		return potentialCover;
	}

	/**
	 * @param potentialCover the potentialCover to set
	 * @since 1.50
	 */
	public void setPotentialCover(File potentialCover) {
		this.potentialCover = potentialCover;
	}

	@Override
	public boolean isRefreshNeeded() {
		long modified = 0;

		for (File f : getFiles()) {
			if (f != null) {
				modified = Math.max(modified, f.lastModified());
			}
		}

		// Check if any of our previously empty folders now have content
		boolean emptyFolderNowNotEmpty = false;
		if (emptyFoldersToRescan != null) {
			for (File emptyFile : emptyFoldersToRescan) {
				if (FileUtil.isFolderRelevant(emptyFile, configuration)) {
					emptyFolderNowNotEmpty = true;
					break;
				}
			}
		}
		return
			getLastRefreshTime() < modified ||
			configuration.getSortMethod(getPath()) == UMSUtils.SORT_RANDOM ||
			emptyFolderNowNotEmpty;
	}

	@Override
	public void doRefreshChildren() {
		doRefreshChildren(null, true);
	}

	@Override
	public void doRefreshChildren(String str) {
		doRefreshChildren(str, true);
	}

	@Override
	public boolean isSearched() {
		return (getParent() instanceof SearchFolder);
	}

	public void setAddToMediaLibrary(boolean value) {
		addToMediaLibrary = value;
	}

	@Override
	public boolean isAddToMediaLibrary() {
		return addToMediaLibrary;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public String getName() {
		if (StringUtils.isEmpty(forcedName)) {
			return name;
		}
		return forcedName;
	}

	@Override
	public boolean isFolder() {
		return true;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public boolean allowScan() {
		return isFolder();
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [id=").append(getId());
		result.append(", name=").append(getName());
		result.append(", format=").append(getFormat());
		result.append(", discovered=").append(isDiscovered());
		if (getMediaAudio() != null) {
			result.append(", selected audio=[").append(getMediaAudio()).append("]");
		}
		if (getMediaSubtitle() != null) {
			result.append(", selected subtitles=[").append(getMediaSubtitle()).append("]");
		}
		if (getEngine() != null) {
			result.append(", player=").append(getEngine());
		}
		if (getChildren() != null && !getChildren().isEmpty()) {
			result.append(", children=").append(getChildren());
		}
		result.append(']');
		return result.toString();
	}

	/**
	 * Returns the first {@link File} in a specified folder that is considered a
	 * "folder thumbnail" by naming convention.
	 *
	 * @param folder the folder to search for a folder thumbnail.
	 * @return The first "folder thumbnail" file in {@code folder} or
	 *         {@code null} if none was found.
	 */
	public static File getFolderThumbnail(File folder) {
		if (folder == null || !folder.isDirectory()) {
			return null;
		}
		try (DirectoryStream<Path> folderThumbnails = Files.newDirectoryStream(folder.toPath(), (Path entry) -> {
				Path fileNamePath = entry.getFileName();
				if (fileNamePath == null) {
					return false;
				}
				String fileName = fileNamePath.toString().toLowerCase(Locale.ROOT);
				if (fileName.startsWith("folder.") || fileName.contains("albumart")) {
					return isPotentialThumbnail(fileName);
				}
				return false;
			})) {
			for (Path folderThumbnail : folderThumbnails) {
				// We don't have any rule to prioritize between them; return the first
				return folderThumbnail.toFile();
			}
		} catch (IOException e) {
			LOGGER.warn("An error occurred while trying to browse folder \"{}\": {}", folder.getAbsolutePath(), e.getMessage());
			LOGGER.trace("", e);
		}
		return null;
	}

	/**
	 * Returns whether or not the specified {@link File} is considered a
	 * "folder thumbnail" by naming convention.
	 *
	 * @param file the {@link File} to evaluate.
	 * @param evaluateExtension if {@code true} the file extension will also be
	 *            evaluated in addition to the file name, if {@code false} only
	 *            the file name will be evaluated.
	 * @return {@code true} if {@code file} name matches the naming convention
	 *         for folder thumbnails, {@code false} otherwise.
	 */
	public static boolean isFolderThumbnail(File file, boolean evaluateExtension) {
		if (file == null || !file.isFile()) {
			return false;
		}

		String fileName = file.getName();
		if (StringUtils.isBlank(fileName)) {
			return false;
		}
		if (evaluateExtension && !isPotentialThumbnail(fileName)) {
			return false;
		}
		fileName = fileName.toLowerCase(Locale.ROOT);
		return fileName.startsWith("folder.") || fileName.contains("albumart");
	}

	/**
	 * Returns the potential thumbnail resources for the specified audio or
	 * video file as a {@link Set} of {@link File}s.
	 *
	 * @param audioVideoFile the {@link File} for which to enumerate potential
	 *            thumbnail files.
	 * @param existingOnly if {@code true}, files will only be added to the
	 *            returned {@link Set} if they {@link File#exists()}.
	 * @return The {@link Set} of {@link File}s.
	 */
	public static Set<File> getPotentialFileThumbnails(
		File audioVideoFile,
		boolean existingOnly
	) {
		File file;
		Set<File> potentialMatches = new HashSet<>(THUMBNAIL_EXTENSIONS.size() * 2);
		for (String extension : THUMBNAIL_EXTENSIONS) {
			file = FileUtil.replaceExtension(audioVideoFile, extension, false, true);
			if (!existingOnly || file.exists()) {
				potentialMatches.add(file);
			}
			file = new File(audioVideoFile.toString() + ".cover." + extension);
			if (!existingOnly || file.exists()) {
				potentialMatches.add(file);
			}
		}
		return potentialMatches;
	}

	/**
	 * Returns whether or not the specified {@link File} has an extension that
	 * makes it a possible thumbnail file that should be considered a thumbnail
	 * resource belonging to another file or folder.
	 *
	 * @param file the {@link File} to evaluate.
	 * @return {@code true} if {@code file} has one of the predefined
	 *         {@link VirtualFile#THUMBNAIL_EXTENSIONS} extensions, {@code false}
	 *         otherwise.
	 */
	public static boolean isPotentialThumbnail(File file) {
		return file != null && file.isFile() && isPotentialThumbnail(file.getName());
	}

	/**
	 * Returns whether or not {@code fileName} has an extension that makes it a
	 * possible thumbnail file that should be considered a thumbnail resource
	 * belonging to another file or folder.
	 *
	 * @param fileName the file name to evaluate.
	 * @return {@code true} if {@code fileName} has one of the predefined
	 *         {@link VirtualFile#THUMBNAIL_EXTENSIONS} extensions, {@code false}
	 *         otherwise.
	 */
	public static boolean isPotentialThumbnail(String fileName) {
		return THUMBNAIL_EXTENSIONS.contains(FileUtil.getExtension(fileName));
	}

	/**
	 * Returns whether {@code fileName} has an extension that is not on our
	 * list of extensions that can't be media files.
	 *
	 * @param fileName the file name to evaluate.
	 * @return {@code true} if {@code fileName} has not the one of the predefined
	 *         {@link VirtualFile#EXTENSIONS_DENYLIST} extensions, {@code false}
	 *         otherwise.
	 */
	public static boolean isPotentialMediaFile(String fileName) {
		String ext = FileUtil.getExtension(fileName);
		if (ext == null) {
			return false;
		}
		return !EXTENSIONS_DENYLIST.contains(ext);
	}

}
