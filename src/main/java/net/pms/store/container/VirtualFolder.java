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
package net.pms.store.container;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.renderers.Renderer;
import net.pms.store.FileSearch;
import net.pms.store.StoreContainer;
import net.pms.store.StoreResource;
import net.pms.store.SystemFileResource;
import net.pms.store.SystemFilesHelper;
import net.pms.store.item.RealFile;
import net.pms.store.utils.StoreResourceSorter;
import net.pms.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VirtualFolder extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(VirtualFolder.class);

	private final String forcedName;
	private final List<File> files;
	private final List<VirtualFolderContent> virtualFolders;

	private List<File> discoverable;
	private List<File> emptyFoldersToRescan;

	private boolean addToMediaLibrary = true;
	private ArrayList<StoreResource> searchList;
	private File potentialCover;

	public VirtualFolder(Renderer renderer) {
		super(renderer, null, null);
		setChildrenSorted(true);
		this.files = new ArrayList<>();
		this.virtualFolders = new ArrayList<>();
		this.forcedName = null;
		setLastModified(0);
	}

	public VirtualFolder(Renderer renderer, VirtualFolderContent virtualFolder) {
		super(renderer, virtualFolder.getName(), null);
		setChildrenSorted(true);
		this.addToMediaLibrary = virtualFolder.isAddToMediaLibrary();
		this.files = virtualFolder.getFiles();
		this.virtualFolders = virtualFolder.getVirtualFolders();
		this.forcedName = null;
		setLastModified(0);
	}

	/**
	 * A to Z limit VirtualFolder
	 */
	public VirtualFolder(Renderer renderer, VirtualFolder virtualFile, List<File> files, String forcedName) {
		super(renderer, null, null);
		setChildrenSorted(true);
		this.addToMediaLibrary = virtualFile.isAddToMediaLibrary();
		this.files = virtualFile.getFiles();
		this.virtualFolders = new ArrayList<>();
		this.discoverable = files;
		this.forcedName = forcedName;
		setLastModified(0);
		analyzeChildren();
	}

	public List<File> getFiles() {
		return files;
	}

	public void addFile(File file) {
		files.add(file);
	}

	private void manageFile(File f) {
		StoreResource res = renderer.getMediaStore().createResourceFromFile(f);
		if (res != null) {
			if (res instanceof RealFile realfile) {
				//we need to propagate the flag in order to make all hierarchy stay outside the media library if needed
				realfile.setAddToMediaLibrary(addToMediaLibrary);
				if (searchList != null) {
					searchList.add(realfile);
				}
			}
			if (res instanceof RealFolder realFolder) {
				//we need to propagate the flag in order to make all hierarchy stay outside the media library if needed
				realFolder.setAddToMediaLibrary(addToMediaLibrary);
				if (searchList != null) {
					searchList.add(realFolder);
				}
			}
			addChild(res, true, true);
		} else if (f.isDirectory() && renderer.getUmsConfiguration().isHideEmptyFolders() && !FileUtil.isFolderRelevant(f, renderer.getUmsConfiguration())) {
			// Keep track of the fact that we have empty folders, so when we're asked if we should refresh,
			// we can re-scan the folders in this list to see if they contain something relevant
			if (emptyFoldersToRescan == null) {
				emptyFoldersToRescan = new ArrayList<>();
			}
			if (!emptyFoldersToRescan.contains(f)) {
				emptyFoldersToRescan.add(f);
			}
		}
	}

	private File getPath() {
		if (this instanceof SystemFileResource systemFileResource) {
			return systemFileResource.getSystemFile();
		}
		return null;
	}

	private List<File> getFilesListForDirectories() {
		List<File> out = new ArrayList<>();
		List<String> ignoredDirectoryNames = renderer.getUmsConfiguration().getIgnoredFolderNames();
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
					Path path = Paths.get(parentDirectory + File.separator + file);
					// Reject any non readable
					if (!Files.isReadable(path)) {
						return false;
					}
					// Accept any directory
					if (Files.isDirectory(path)) {
						// Skip if ignored
						if (!ignoredDirectoryNames.isEmpty() && ignoredDirectoryNames.contains(file)) {
							LOGGER.debug("Ignoring {} because it is in the ignored directories list", file);
							return false;
						}
						return true;
					}

					// We want to find only media files
					return SystemFilesHelper.isPotentialMediaFile(file);
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

	private boolean analyzeChildren() {
		FileSearch fs = null;
		if (!discoverable.isEmpty() && renderer.getUmsConfiguration().getSearchInFolder()) {
			searchList = new ArrayList<>();
			fs = new FileSearch(searchList);
			addChild(new SearchFolder(renderer, fs));
		}
		for (VirtualFolderContent virtualFolder : virtualFolders) {
			StoreContainer parent = getSharedContentParent(virtualFolder.getParent());
			parent.addChild(new VirtualFolder(renderer, virtualFolder), true, true);
		}
		while (!discoverable.isEmpty()) {
			manageFile(discoverable.remove(0));
		}
		if (fs != null) {
			fs.update(searchList);
		}
		return discoverable.isEmpty();
	}

	@Override
	public synchronized void discoverChildren() {
		if (discoverable == null) {
			discoverable = new ArrayList<>();
		} else {
			return;
		}

		getChildren().clear();
		List<File> childrenFiles = getFilesListForDirectories();

		// Build a map of all files and their corresponding formats
		Set<File> images = new HashSet<>();
		Set<File> audioVideo = new HashSet<>();
		Iterator<File> iterator = childrenFiles.iterator();
		while (iterator.hasNext()) {
			File file = iterator.next();
			if (file.isFile()) {
				if (SystemFilesHelper.isPotentialThumbnail(file.getName())) {
					if (SystemFilesHelper.isFolderThumbnail(file, false)) {
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
				potentialMatches = SystemFilesHelper.getPotentialFileThumbnails(audioVideoFile, false);
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
		if (childrenFiles.size() > renderer.getUmsConfiguration().getATZLimit() && StringUtils.isEmpty(forcedName)) {
			/*
			 * Too many files to display at once, add A-Z folders
			 * instead and let the filters begin
			 *
			 * Note: If we done this at the level directly above we don't do it again
			 * since all files start with the same letter then
			 */
			Map<String, List<File>> map = new TreeMap<>();
			for (File f : childrenFiles) {
				if ((!f.isFile() && !f.isDirectory()) || f.isHidden() || !f.canRead()) {
					// skip these
					continue;
				}
				if (f.isDirectory() && renderer.getUmsConfiguration().isHideEmptyFolders() && !FileUtil.isFolderRelevant(f, renderer.getUmsConfiguration())) {
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

			if (map.size() > 1) {
				for (Entry<String, List<File>> entry : map.entrySet()) {
					// loop over all letters, this avoids adding
					// empty letters
					VirtualFolder mf = new VirtualFolder(renderer, this, entry.getValue(), entry.getKey());
					addChild(mf, true, true);
				}
				return;
			}
		}

		for (File f : childrenFiles) {
			if (f.isDirectory()) {
				discoverable.add(f);
			}
		}

		for (File f : childrenFiles) {
			if (f.isFile()) {
				discoverable.add(f);
			}
		}
		setDiscovered(analyzeChildren());
		sortChildrenIfNeeded();
		setLastRefreshTime(System.currentTimeMillis());
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
				if (FileUtil.isFolderRelevant(emptyFile, renderer.getUmsConfiguration())) {
					emptyFolderNowNotEmpty = true;
					break;
				}
			}
		}
		return getLastRefreshTime() < modified ||
				renderer.getUmsConfiguration().getSortMethod(getPath()) == StoreResourceSorter.SORT_RANDOM ||
				emptyFolderNowNotEmpty;
	}

	@Override
	public synchronized void doRefreshChildren() {
		emptyFoldersToRescan = null; // Since we're re-scanning, reset this list so it can be built again
		discoverable = null;
		discoverChildren();
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
	public String getName() {
		if (StringUtils.isEmpty(forcedName)) {
			return super.getName();
		}
		return forcedName;
	}

	@Override
	public boolean allowScan() {
		return true;
	}

}
