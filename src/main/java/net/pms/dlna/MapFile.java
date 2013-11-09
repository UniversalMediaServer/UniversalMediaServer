/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.dlna;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.Collator;
import java.util.*;
import net.pms.PMS;
import net.pms.configuration.MapFileConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.FormatFactory;
import net.pms.network.HTTPResource;
import net.pms.util.NaturalComparator;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public class MapFile extends DLNAResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapFile.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private List<File> discoverable;
	private String forcedName;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public File potentialCover;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected MapFileConfiguration conf;

	private static final Collator collator;

	static {
		collator = Collator.getInstance();
		collator.setStrength(Collator.PRIMARY);
	}

	public MapFile() {
		setConf(new MapFileConfiguration());
		setLastModified(0);
		forcedName = null;
	}

	public MapFile(MapFileConfiguration conf) {
		setConf(conf);
		setLastModified(0);
		forcedName = null;
	}

	public MapFile(MapFileConfiguration conf, List<File> list) {
		setConf(conf);
		setLastModified(0);
		this.discoverable = list;
		forcedName = null;
	}

	private boolean isFileRelevant(File f) {
		String fileName = f.getName().toLowerCase();
		return (configuration.isArchiveBrowsing() && (fileName.endsWith(".zip") || fileName.endsWith(".cbz")
			|| fileName.endsWith(".rar") || fileName.endsWith(".cbr")))
			|| fileName.endsWith(".iso") || fileName.endsWith(".img")
			|| fileName.endsWith(".m3u") || fileName.endsWith(".m3u8") || fileName.endsWith(".pls") || fileName.endsWith(".cue");
	}

	private boolean isFolderRelevant(File f) {
		boolean isRelevant = false;

		if (f.isDirectory() && configuration.isHideEmptyFolders()) {
			File[] children = f.listFiles();

			// listFiles() returns null if "this abstract pathname does not denote a directory, or if an I/O error occurs".
			// in this case (since we've already confirmed that it's a directory), this seems to mean the directory is non-readable
			// http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15135
			// http://stackoverflow.com/questions/3228147/retrieving-the-underlying-error-when-file-listfiles-return-null
			if (children == null) {
				LOGGER.warn("Can't list files in non-readable directory: {}", f.getAbsolutePath());
			} else {
				for (File child : children) {
					if (child.isFile()) {
						if (FormatFactory.getAssociatedFormat(child.getName()) != null || isFileRelevant(child)) {
							isRelevant = true;
							break;
						}
					} else {
						if (isFolderRelevant(child)) {
							isRelevant = true;
							break;
						}
					}
				}
			}
		}
		return isRelevant;
	}

	private void manageFile(File f, String str) {
		if (f.isFile() || f.isDirectory()) {
			String lcFilename = f.getName().toLowerCase();
			if (str != null && !lcFilename.contains(str)) {
				// this is not searched for
				return;
			}

			if (!f.isHidden()) {
				if (configuration.isArchiveBrowsing() && (lcFilename.endsWith(".zip") || lcFilename.endsWith(".cbz"))) {
					addChild(new ZippedFile(f));
				} else if (configuration.isArchiveBrowsing() && (lcFilename.endsWith(".rar") || lcFilename.endsWith(".cbr"))) {
					addChild(new RarredFile(f));
				} else if (configuration.isArchiveBrowsing() && (lcFilename.endsWith(".tar") || lcFilename.endsWith(".gzip") || lcFilename.endsWith(".gz") || lcFilename.endsWith(".7z"))) {
					addChild(new SevenZipFile(f));
				} else if ((lcFilename.endsWith(".iso") || lcFilename.endsWith(".img")) || (f.isDirectory() && f.getName().toUpperCase().equals("VIDEO_TS"))) {
					addChild(new DVDISOFile(f));
				} else if (lcFilename.endsWith(".m3u") || lcFilename.endsWith(".m3u8") || lcFilename.endsWith(".pls")) {
					addChild(new PlaylistFolder(f));
				} else if (lcFilename.endsWith(".cue")) {
					addChild(new CueFolder(f));
				} else {
					/* Optionally ignore empty directories */
					if (f.isDirectory() && configuration.isHideEmptyFolders() && !isFolderRelevant(f)) {
						LOGGER.debug("Ignoring empty/non-relevant directory: " + f.getName());
					} else { // Otherwise add the file
						addChild(new RealFile(f));
					}
				}
			}

			// FIXME this causes folder thumbnails to take precedence over file thumbnails
			if (f.isFile()) {
				if (lcFilename.equals("folder.jpg") || lcFilename.equals("folder.png") || (lcFilename.contains("albumart") && lcFilename.endsWith(".jpg"))) {
					setPotentialCover(f);
				}
			}
		}
	}

	private List<File> getFileList() {
		List<File> out = new ArrayList<>();

		for (File file : this.conf.getFiles()) {
			if (file != null && file.isDirectory()) {
				if (file.canRead()) {
					File[] files = file.listFiles();

					if (files == null) {
						LOGGER.warn("Can't read files from directory: {}", file.getAbsolutePath());
					} else {
						out.addAll(Arrays.asList(files));
					}
				} else {
					LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
				}
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
		int currentChildrenCount = getChildren().size();
		int vfolder = 0;
		while (((getChildren().size() - currentChildrenCount) < count) || (count == -1)) {
			if (vfolder < getConf().getChildren().size()) {
				addChild(new MapFile(getConf().getChildren().get(vfolder)));
				++vfolder;
			} else {
				if (discoverable.isEmpty()) {
					break;
				}
				manageFile(discoverable.remove(0), null);
			}
		}
		return discoverable.isEmpty();
	}

	private String renameForSorting(String filename) {
		if (configuration.isPrettifyFilenames()) {
			// This chunk makes anime sort properly
			int squareBracketIndex;
			if (filename.substring(0, 1).matches("\\[")) {
				filename = filename.replaceAll("_", " ");
				squareBracketIndex = filename.indexOf("]");
				if (squareBracketIndex != -1) {
					filename = filename.substring(squareBracketIndex + 1);
					if (filename.substring(0, 1).matches("\\s")) {
						filename = filename.substring(1);
					}
				}
			}
		}

		if (configuration.isIgnoreTheWordThe()) {
			// Remove "The" from the beginning of files
			filename = filename.replaceAll("^(?i)The[ .]", "");
		}

		return filename;
	}

	private void sort(List<File> files) {
		switch (configuration.getSortMethod()) {
			case 4: // Locale-sensitive natural sort
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String filename1ToSort = renameForSorting(f1.getName());
						String filename2ToSort = renameForSorting(f2.getName());

						return NaturalComparator.compareNatural(collator, filename1ToSort, filename2ToSort);
					}
				});
				break;
			case 3: // Case-insensitive ASCIIbetical sort
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String filename1ToSort = renameForSorting(f1.getName());
						String filename2ToSort = renameForSorting(f2.getName());

						return filename1ToSort.compareToIgnoreCase(filename2ToSort);
					}
				});
				break;
			case 2: // Sort by modified date, oldest first
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(Long.valueOf(f2.lastModified()));
					}
				});
				break;
			case 1: // Sort by modified date, newest first
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						return Long.valueOf(f2.lastModified()).compareTo(Long.valueOf(f1.lastModified()));
					}
				});
				break;
			default: // Locale-sensitive A-Z
				Collections.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String filename1ToSort = renameForSorting(f1.getName());
						String filename2ToSort = renameForSorting(f2.getName());

						return collator.compare(filename1ToSort, filename2ToSort);
					}
				});
				break;
		}
	}

	@Override
	public void discoverChildren() {
		discoverChildren(null);
	}

	@Override
	public void discoverChildren(String str) {
		//super.discoverChildren(str);
		if (str != null) {
			str = str.toLowerCase();
		}

		if (discoverable == null) {
			discoverable = new ArrayList<>();
		} else {
			return;
		}

		List<File> files = getFileList();

		// ATZ handling
		if (files.size() > configuration.getATZLimit() && StringUtils.isEmpty(forcedName)) {
			/*
			 * Too many files to display at once, add A-Z folders
			 * instead and let the filters begin
			 *
			 * Note: If we done this at the level directly above we don't do it again
			 * since all files start with the same letter then
			 */
			TreeMap<String, ArrayList<File>> map = new TreeMap<>();
			for (File f : files) {
				if ((!f.isFile() && !f.isDirectory()) || f.isHidden()) {
					// skip these
					continue;
				}
				if (f.isDirectory() && configuration.isHideEmptyFolders() && !isFolderRelevant(f)) {
					LOGGER.debug("Ignoring empty/non-relevant directory: " + f.getName());
					continue;
				}

				String filenameToSort = renameForSorting(f.getName());

				char c = filenameToSort.toUpperCase().charAt(0);

				if (!(c >= 'A' && c <= 'Z')) {
					// "other char"
					c = '#';
				}
				ArrayList<File> l = map.get(String.valueOf(c));
				if (l == null) {
					// new letter
					l = new ArrayList<>();
				}
				l.add(f);
				map.put(String.valueOf(c), l);
			}

			for (String letter : map.keySet()) {
				// loop over all letters, this avoids adding
				// empty letters
				ArrayList<File> l = map.get(letter);
				sort(l);
				MapFile mf = new MapFile(getConf(), l);
				mf.forcedName = letter;
				addChild(mf);
			}
			return;
		}
		
		sort(files);
		
		for (File f : files) {
			if (f.isDirectory()) {
				if (str == null || f.getName().toLowerCase().contains(str)) {
					discoverable.add(f); // manageFile(f);
				}
			}
		}

		// For random sorting, we only randomize file entries
		if (configuration.getSortMethod() == 5) {
			Collections.shuffle(files);
		}

		for (File f : files) {
			if (f.isFile()) {
				if (str == null || f.getName().toLowerCase().contains(str)) {
					discoverable.add(f); // manageFile(f);
				}
			}
		}
	}

	@Override
	public boolean isRefreshNeeded() {
		long modified = 0;

		for (File f : this.getConf().getFiles()) {
			if (f != null) {
				modified = Math.max(modified, f.lastModified());
			}
		}

		return getLastRefreshTime() < modified;
	}

	@Override
	public void doRefreshChildren() {
		doRefreshChildren(null);
	}

	@Override
	public void doRefreshChildren(String str) {
		List<File> files = getFileList();
		List<File> addedFiles = new ArrayList<>();
		List<DLNAResource> removedFiles = new ArrayList<>();

		for (DLNAResource d : getChildren()) {
			boolean isNeedMatching = !(d.getClass() == MapFile.class || (d instanceof VirtualFolder && !(d instanceof DVDISOFile)));
			boolean found = foundInList(files, d);

			if (isNeedMatching && !found) {
				removedFiles.add(d);
			} else if (str != null && found) {
				String s = d.getName().toLowerCase();
				if (!s.contains(str)) {
					// new search, this doesn't match
					removedFiles.add(d);
				}
			}
		}

		for (File f : files) {
			if (!f.isHidden() && (f.isDirectory() || FormatFactory.getAssociatedFormat(f.getName()) != null)) {
				addedFiles.add(f);
			}
		}

		for (DLNAResource f : removedFiles) {
			LOGGER.debug("File automatically removed: " + f.getName());
		}

		for (File f : addedFiles) {
			LOGGER.debug("File automatically added: " + f.getName());
		}

		// false: don't create the folder if it doesn't exist i.e. find the folder
		TranscodeVirtualFolder transcodeFolder = getTranscodeFolder(false);

		for (DLNAResource f : removedFiles) {
			getChildren().remove(f);

			if (transcodeFolder != null) {
				for (int j = transcodeFolder.getChildren().size() - 1; j >= 0; j--) {
					if (transcodeFolder.getChildren().get(j).getName().equals(f.getName())) {
						transcodeFolder.getChildren().remove(j);
					}
				}
			}
		}

		for (File f : addedFiles) {
			manageFile(f, str);
		}

		for (MapFileConfiguration f : this.getConf().getChildren()) {
			addChild(new MapFile(f));
		}
	}

	private boolean foundInList(List<File> files, DLNAResource dlna) {
		for (Iterator<File> it = files.iterator(); it.hasNext();) {
			File file = it.next();
			if (!file.isHidden() && isNameMatch(dlna, file) && (isRealFolder(dlna) || isSameLastModified(dlna, file))) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	private boolean isSameLastModified(DLNAResource dlna, File file) {
		return dlna.getLastModified() == file.lastModified();
	}

	private boolean isRealFolder(DLNAResource dlna) {
		return dlna instanceof RealFile && dlna.isFolder();
	}

	private boolean isNameMatch(DLNAResource dlna, File file) {
		return (dlna.getName().equals(file.getName()) || isDVDIsoMatch(dlna, file));
	}

	private boolean isDVDIsoMatch(DLNAResource dlna, File file) {
		if (dlna instanceof DVDISOFile) {
			DVDISOFile dvdISOFile = (DVDISOFile) dlna;
			return dvdISOFile.getFilename().equals(file.getName());
		} else {
			return false;
		}
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	@Override
	public String getThumbnailContentType() {
		String thumbnailIcon = this.getConf().getThumbnailIcon();
		if (thumbnailIcon != null && thumbnailIcon.toLowerCase().endsWith(".png")) {
			return HTTPResource.PNG_TYPEMIME;
		}
		return super.getThumbnailContentType();
	}

	@Override
	public InputStream getThumbnailInputStream() throws IOException {
		return this.getConf().getThumbnailIcon() != null
			? getResourceInputStream(this.getConf().getThumbnailIcon())
			: super.getThumbnailInputStream();
	}

	@Override
	public long length() {
		return 0;
	}

	@Override
	public String getName() {
		if (StringUtils.isEmpty(forcedName)) {
			return this.getConf().getName();
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

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "MapFile [name=" + getName() + ", id=" + getResourceId() + ", format=" + getFormat() + ", children=" + getChildren() + "]";
	}

	/**
	 * @return the conf
	 * @since 1.50
	 */
	protected MapFileConfiguration getConf() {
		return conf;
	}

	/**
	 * @param conf the conf to set
	 * @since 1.50
	 */
	protected void setConf(MapFileConfiguration conf) {
		this.conf = conf;
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
	public boolean isSearched() {
		return true;
	}
}
