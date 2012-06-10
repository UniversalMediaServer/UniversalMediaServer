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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.pms.PMS;
import net.pms.configuration.MapFileConfiguration;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.formats.FormatFactory;
import net.pms.network.HTTPResource;
import net.pms.util.NaturalComparator;

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
	private List<File> discoverable;

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
		setLastmodified(0);
	}

	public MapFile(MapFileConfiguration conf) {
		setConf(conf);
		setLastmodified(0);
	}

	private boolean isFileRelevant(File f) {
		String fileName = f.getName().toLowerCase();
		return (PMS.getConfiguration().isArchiveBrowsing() && (fileName.endsWith(".zip") || fileName.endsWith(".cbz")
			|| fileName.endsWith(".rar") || fileName.endsWith(".cbr")))
			|| fileName.endsWith(".iso") || fileName.endsWith(".img")
			|| fileName.endsWith(".m3u") || fileName.endsWith(".m3u8") || fileName.endsWith(".pls") || fileName.endsWith(".cue");
	}

	private boolean isFolderRelevant(File f) {
		boolean excludeNonRelevantFolder = true;
		if (f.isDirectory() && PMS.getConfiguration().isHideEmptyFolders()) {
			File children[] = f.listFiles();
			for (File child : children) {
				if (child.isFile()) {
					if (FormatFactory.getAssociatedExtension(child.getName()) != null || isFileRelevant(child)) {
						excludeNonRelevantFolder = false;
						break;
					}
				} else {
					if (isFolderRelevant(child)) {
						excludeNonRelevantFolder = false;
						break;
					}
				}
			}
		}

		return !excludeNonRelevantFolder;
	}

	private void manageFile(File f) {
		if (f.isFile() || f.isDirectory()) {
			String lcFilename = f.getName().toLowerCase();

			if (!f.isHidden()) {
				if (PMS.getConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".zip") || lcFilename.endsWith(".cbz"))) {
					addChild(new ZippedFile(f));
				} else if (PMS.getConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".rar") || lcFilename.endsWith(".cbr"))) {
					addChild(new RarredFile(f));
				} else if ((lcFilename.endsWith(".iso") || lcFilename.endsWith(".img")) || (f.isDirectory() && f.getName().toUpperCase().equals("VIDEO_TS"))) {
					addChild(new DVDISOFile(f));
				} else if (lcFilename.endsWith(".m3u") || lcFilename.endsWith(".m3u8") || lcFilename.endsWith(".pls")) {
					addChild(new PlaylistFolder(f));
				} else if (lcFilename.endsWith(".cue")) {
					addChild(new CueFolder(f));
				} else {
					/* Optionally ignore empty directories */
					if (f.isDirectory() && PMS.getConfiguration().isHideEmptyFolders() && !isFolderRelevant(f)) {
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
		List<File> out = new ArrayList<File>();
		for (File file : this.conf.getFiles()) {
			if (file != null && file.isDirectory() && file.canRead()) {
				out.addAll(Arrays.asList(file.listFiles()));
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
		while ((getChildren().size() - currentChildrenCount) < count || count == -1) {
			if (vfolder < getConf().getChildren().size()) {
				addChild(new MapFile(getConf().getChildren().get(vfolder)));
				++vfolder;
			} else {
				if (discoverable.isEmpty()) {
					break;
				}
				manageFile(discoverable.remove(0));
			}
		}
		return discoverable.isEmpty();
	}

	@Override
	public void discoverChildren() {
		super.discoverChildren();

		if (discoverable == null) {
			discoverable = new ArrayList<File>();
		} else {
			return;
		}

		List<File> files = getFileList();

		switch (PMS.getConfiguration().getSortMethod()) {
			case 4: // Locale-sensitive natural sort
				Collections.sort(files, new Comparator<File>() {
					public int compare(File f1, File f2) {
						return NaturalComparator.compareNatural(collator, f1.getName(), f2.getName());
					}
				});
				break;
			case 3: // Case-insensitive ASCIIbetical sort
				Collections.sort(files, new Comparator<File>() {

					public int compare(File f1, File f2) {
						return f1.getName().compareToIgnoreCase(f2.getName());
					}
				});
				break;
			case 2: // Sort by modified date, oldest first
				Collections.sort(files, new Comparator<File>() {

					public int compare(File f1, File f2) {
						return Long.valueOf(f1.lastModified()).compareTo(Long.valueOf(f2.lastModified()));
					}
				});
				break;
			case 1: // Sort by modified date, newest first
				Collections.sort(files, new Comparator<File>() {

					public int compare(File f1, File f2) {
						return Long.valueOf(f2.lastModified()).compareTo(Long.valueOf(f1.lastModified()));
					}
				});
				break;
			default: // Locale-sensitive A-Z
				Collections.sort(files, new Comparator<File>() {

					public int compare(File f1, File f2) {
						return collator.compare(f1.getName(), f2.getName());
					}
				});
				break;
		}

		for (File f : files) {
			if (f.isDirectory()) {
				discoverable.add(f); // manageFile(f);
			}
		}

		for (File f : files) {
			if (f.isFile()) {
				discoverable.add(f); // manageFile(f);
			}
		}
	}

	@Override
	public boolean isRefreshNeeded() {
		long lastModif = 0;
		for (File f : this.getConf().getFiles()) {
			if (f != null) {
				lastModif = Math.max(lastModif, f.lastModified());
			}
		}
		return getLastRefreshTime() < lastModif;
	}

	@Override
	public void doRefreshChildren() {
		List<File> files = getFileList();
		List<File> addedFiles = new ArrayList<File>();
		List<DLNAResource> removedFiles = new ArrayList<DLNAResource>();

		for (DLNAResource d : getChildren()) {
			boolean isNeedMatching = !(d.getClass() == MapFile.class || (d instanceof VirtualFolder && !(d instanceof DVDISOFile)));
			if (isNeedMatching && !foundInList(files, d)) {
				removedFiles.add(d);
			}
		}

		for (File f : files) {
			if (!f.isHidden() && (f.isDirectory() || FormatFactory.getAssociatedExtension(f.getName()) != null)) {
				addedFiles.add(f);
			}
		}

		for (DLNAResource f : removedFiles) {
			LOGGER.debug("File automatically removed: " + f.getName());
		}

		for (File f : addedFiles) {
			LOGGER.debug("File automatically added: " + f.getName());
		}

		TranscodeVirtualFolder vf = getTranscodeFolder(false);

		for (DLNAResource f : removedFiles) {
			getChildren().remove(f);
			if (vf != null) {
				for (int j = vf.getChildren().size() - 1; j >= 0; j--) {
					if (vf.getChildren().get(j).getName().equals(f.getName())) {
						vf.getChildren().remove(j);
					}
				}
			}
		}

		for (File f : addedFiles) {
			manageFile(f);
		}

		for (MapFileConfiguration f : this.getConf().getChildren()) {
			addChild(new MapFile(f));
		}
	}

	private boolean foundInList(List<File> files, DLNAResource d) {
		for (File f: files) {
			if (!f.isHidden() && isNameMatch(f, d) && (isRealFolder(d) || isSameLastModified(f, d))) {
				files.remove(f);
				return true;
			}
		}
		return false;
	}

	private boolean isSameLastModified(File f, DLNAResource d) {
		return d.getLastmodified() == f.lastModified();
	}

	private boolean isRealFolder(DLNAResource d) {
		return d instanceof RealFile && d.isFolder();
	}

	private boolean isNameMatch(File file, DLNAResource resource) {
		return (resource.getName().equals(file.getName()) || isDVDIsoMatch(file, resource));
	}

	private boolean isDVDIsoMatch(File file, DLNAResource resource) {
		return (resource instanceof DVDISOFile) &&
			resource.getName().startsWith(DVDISOFile.PREFIX) &&
			resource.getName().substring(DVDISOFile.PREFIX.length()).equals(file.getName());
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
		return this.getConf().getName();
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
		return "MapFile [name=" + getName() + ", id=" + getResourceId() + ", ext=" + getExt() + ", children=" + getChildren() + "]";
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
}
