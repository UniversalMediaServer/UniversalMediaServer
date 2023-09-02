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
package net.pms.library;

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.sharedcontent.ApertureContent;
import net.pms.configuration.sharedcontent.FeedAudioContent;
import net.pms.configuration.sharedcontent.FeedImageContent;
import net.pms.configuration.sharedcontent.FeedVideoContent;
import net.pms.configuration.sharedcontent.FolderContent;
import net.pms.configuration.sharedcontent.IPhotoContent;
import net.pms.configuration.sharedcontent.ITunesContent;
import net.pms.configuration.sharedcontent.SharedContent;
import net.pms.configuration.sharedcontent.SharedContentConfiguration;
import net.pms.configuration.sharedcontent.SharedContentWithPath;
import net.pms.configuration.sharedcontent.StreamAudioContent;
import net.pms.configuration.sharedcontent.StreamContent;
import net.pms.configuration.sharedcontent.StreamVideoContent;
import net.pms.configuration.sharedcontent.VirtualFolderContent;
import net.pms.iam.AccountService;
import net.pms.library.container.ApertureLibraries;
import net.pms.library.container.AudiosFeed;
import net.pms.library.container.CodeEnter;
import net.pms.library.container.CueFolder;
import net.pms.library.container.DVDISOFile;
import net.pms.library.container.DynamicPlaylist;
import net.pms.library.container.FolderLimit;
import net.pms.library.container.IPhotoLibrary;
import net.pms.library.container.ITunesLibrary;
import net.pms.library.container.ImagesFeed;
import net.pms.library.container.MediaLibrary;
import net.pms.library.container.MediaMonitor;
import net.pms.library.container.Playlist;
import net.pms.library.container.PlaylistFolder;
import net.pms.library.container.RarredFile;
import net.pms.library.container.RealFolder;
import net.pms.library.container.SearchFolder;
import net.pms.library.container.ServerSettingsFolder;
import net.pms.library.container.SevenZipFile;
import net.pms.library.container.UnattachedFolder;
import net.pms.library.container.UserVirtualFolder;
import net.pms.library.container.VideosFeed;
import net.pms.library.container.VirtualFolder;
import net.pms.library.container.VirtualFolderDbId;
import net.pms.library.container.ZippedFile;
import net.pms.library.item.RealFile;
import net.pms.library.item.WebAudioStream;
import net.pms.library.item.WebVideoStream;
import net.pms.library.utils.IOList;
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.renderers.Renderer;
import net.pms.util.FileUtil;
import net.pms.util.SimpleThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RootFolder extends LibraryContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(RootFolder.class);

	// A temp folder for non-xmb items
	private final UnattachedFolder tempFolder;
	private final MediaLibrary mediaLibrary;
	private DynamicPlaylist dynamicPls;
	private FolderLimit lim;
	private MediaMonitor mon;

	/**
	 * List of children objects backuped when discoverChildren.
	 */
	private final List<LibraryResource> backupChildren = new ArrayList<>();

	public RootFolder(Renderer renderer) {
		super(renderer, "root", null);
		tempFolder = new UnattachedFolder(renderer, "Temp");
		mediaLibrary = new MediaLibrary(renderer);
		setIndexId(0);
		addVirtualMyMusicFolder();
	}

	public UnattachedFolder getTemp() {
		return tempFolder;
	}

	public Playlist getDynamicPls() {
		if (dynamicPls == null) {
			dynamicPls = new DynamicPlaylist(renderer, Messages.getString("DynamicPlaylist"),
					renderer.getUmsConfiguration().getDynamicPlsSavePath(),
					(renderer.getUmsConfiguration().isDynamicPlsAutoSave() ? IOList.AUTOSAVE : 0) | IOList.PERMANENT);
		}
		return dynamicPls;
	}

	/**
	 * Returns the MediaLibrary.
	 *
	 * @return The current {@link MediaLibrary}.
	 */
	public MediaLibrary getLibrary() {
		return mediaLibrary;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public synchronized void discoverChildren() {
		if (isDiscovered()) {
			return;
		}

		//clear childrens but keep copy until discovered
		backupChildren.clear();
		for (LibraryResource libraryResource : getChildren()) {
			backupChildren.add(libraryResource);
		}
		getChildren().clear();

		if (renderer.getUmsConfiguration().isShowMediaLibraryFolder() && mediaLibrary.isEnabled()) {
			if (backupChildren.contains(mediaLibrary)) {
				addChildInternal(mediaLibrary, false);
				backupChildren.remove(mediaLibrary);
			} else {
				addChild(mediaLibrary);
			}
		}

		if (mon != null) {
			mon.clearChildren();
		}
		if (renderer.getUmsConfiguration().getUseCache()) {
			List<File> foldersMonitored = SharedContentConfiguration.getMonitoredFolders();
			if (!foldersMonitored.isEmpty()) {
				File[] dirs = foldersMonitored.toArray(File[]::new);
				mon = new MediaMonitor(renderer, dirs);
			}
		}

		if (renderer.getUmsConfiguration().getFolderLimit() &&
				renderer.isLimitFolders()) {
			lim = new FolderLimit(renderer);
			addChild(lim);
		}

		if (renderer.getUmsConfiguration().isDynamicPls()) {
			if (dynamicPls != null && backupChildren.contains(dynamicPls)) {
				addChildInternal(dynamicPls, false);
				backupChildren.remove(dynamicPls);
			} else {
				addChild(getDynamicPls());
			}

			if (!renderer.getUmsConfiguration().isHideSavedPlaylistFolder()) {
				File plsdir = new File(renderer.getUmsConfiguration().getDynamicPlsSavePath());
				LibraryResource plsResource = findResourceFromFile(backupChildren, plsdir);
				if (plsResource != null) {
					addChildInternal(plsResource, false);
					backupChildren.remove(plsResource);
				} else {
					addChild(new RealFolder(renderer, plsdir, Messages.getString("SavedPlaylists")));
				}
			}
		}

		setSharedContents();

		if (renderer.getUmsConfiguration().isShowServerSettingsFolder()) {
			LibraryResource serverSettingsFolder = findVirtualFolderInResources(backupChildren, Messages.getString("ServerSettings"));
			if (serverSettingsFolder != null) {
				addChildInternal(serverSettingsFolder, false);
				backupChildren.remove(serverSettingsFolder);
			} else {
				serverSettingsFolder = ServerSettingsFolder.getServerSettingsFolder(renderer);
				if (serverSettingsFolder != null) {
					addChild(serverSettingsFolder);
				}
			}
		}

		//now remove old children that was not added from globalids
		clearBackupChildren();
		setDiscovered(true);
	}

	private LibraryContainer findVirtualFolderInResources(List<LibraryResource> resources, String name) {
		if (name == null) {
			return null;
		}
		for (LibraryResource resource : resources) {
			if (resource instanceof LibraryContainer virtualFolder && name.equals(virtualFolder.getName())) {
				return virtualFolder;
			}
		}
		return null;
	}

	public void setFolderLim(LibraryResource r) {
		if (lim != null) {
			lim.setStart(r);
		}
	}

	private void setSharedContents() {
		List<LibraryResource> realSystemFileResources = new ArrayList<>();
		List<SharedContent> sharedContents = SharedContentConfiguration.getSharedContentArray();
		boolean useExternalContent = CONFIGURATION.getExternalNetwork() && renderer.getUmsConfiguration().getExternalNetwork();
		for (SharedContent sharedContent : sharedContents) {
			if (sharedContent.isActive() && sharedContent.isGroupAllowed(renderer.getAccountGroupId())) {
				if (sharedContent instanceof ITunesContent iTunesContent) {
					setITunesContent(iTunesContent);
				} else if (sharedContent instanceof ApertureContent) {
					setApertureContent();
				} else if (sharedContent instanceof IPhotoContent) {
					setIPhotoContent();
				} else if (sharedContent instanceof FolderContent folder) {
					if (folder.getFile() != null) {
						LibraryResource realSystemFileResource = setFolderContent(folder);
						if (realSystemFileResource instanceof RealFolder || realSystemFileResource instanceof RealFile) {
							realSystemFileResources.add(realSystemFileResource);
						}
					}
				} else if (sharedContent instanceof VirtualFolderContent virtualFolder) {
					setVirtualFolderContent(virtualFolder);
				} else if (useExternalContent && sharedContent instanceof SharedContentWithPath sharedContentWithPath && sharedContentWithPath.isExternalContent()) {
					setExternalContent(sharedContentWithPath);
				}
			}
		}

		if (renderer.getUmsConfiguration().getSearchFolder()) {
			SearchFolder sf = new SearchFolder(renderer, Messages.getString("SearchDiscFolders"), new FileSearch(realSystemFileResources));
			addChild(sf);
		}
	}

	private LibraryResource setFolderContent(FolderContent folderContent) {
		if (folderContent.getFile() != null) {
			LibraryResource realSystemFileResource = findResourceFromFile(backupChildren, folderContent.getFile());
			if (realSystemFileResource != null) {
				addChildInternal(realSystemFileResource, false);
				backupChildren.remove(realSystemFileResource);
			} else {
				realSystemFileResource = createResourceFromFile(folderContent.getFile());
				addChild(realSystemFileResource, true, true);
			}
			return realSystemFileResource;
		}
		return null;
	}

	private void setVirtualFolderContent(VirtualFolderContent virtualFolderContent) {
		//TODO : find a way to use backupChildren avoiding reconstruction
		LibraryContainer parent = getSharedContentParent(virtualFolderContent.getParent());
		parent.addChild(new VirtualFolder(renderer, virtualFolderContent));
	}

	private void setITunesContent(ITunesContent iTunesContent) {
		int osType = Platform.getOSType();
		if (osType == Platform.MAC || osType == Platform.WINDOWS) {
			LibraryResource iTunesRes = findVirtualFolderInResources(backupChildren, "iTunes Library");
			if (iTunesRes != null) {
				addChildInternal(iTunesRes, false);
				backupChildren.remove(iTunesRes);
			} else {
				iTunesRes = ITunesLibrary.getiTunesFolder(renderer, iTunesContent.getPath());
				if (iTunesRes != null) {
					addChild(iTunesRes);
				}
			}
		}
	}

	private void setApertureContent() {
		int osType = Platform.getOSType();
		if (osType == Platform.MAC) {
			LibraryResource apertureRes = findVirtualFolderInResources(backupChildren, "Aperture libraries");
			if (apertureRes != null) {
				addChildInternal(apertureRes, false);
				backupChildren.remove(apertureRes);
			} else {
				apertureRes = ApertureLibraries.getApertureFolder(renderer);
				if (apertureRes != null) {
					addChild(apertureRes);
				}
			}
		}
	}

	private void setIPhotoContent() {
		int osType = Platform.getOSType();
		if (osType == Platform.MAC) {
			LibraryResource iPhotoRes = findVirtualFolderInResources(backupChildren, "iPhoto Library");
			if (iPhotoRes != null) {
				addChildInternal(iPhotoRes, false);
				backupChildren.remove(iPhotoRes);
			} else {
				iPhotoRes = IPhotoLibrary.getiPhotoFolder(renderer);
				if (iPhotoRes != null) {
					addChild(iPhotoRes);
				}
			}
		}
	}

	private void setExternalContent(SharedContentWithPath sharedContent) {
		LibraryContainer parent = getSharedContentParent(sharedContent.getParent());
		// Handle web playlists stream
		if (sharedContent instanceof StreamContent streamContent) {
			LibraryResource playlist = PlaylistFolder.getPlaylist(renderer, streamContent.getName(), streamContent.getUri(), streamContent.getFormat());
			if (playlist != null) {
				parent.addChild(playlist);
				return;
			}
		}
		if (sharedContent instanceof FeedAudioContent feedAudioContent) {
			parent.addChild(new AudiosFeed(renderer, feedAudioContent.getUri()));
		} else if (sharedContent instanceof FeedImageContent feedImageContent) {
			parent.addChild(new ImagesFeed(renderer, feedImageContent.getUri()));
		} else if (sharedContent instanceof FeedVideoContent feedVideoContent) {
			parent.addChild(new VideosFeed(renderer, feedVideoContent.getUri()));
		} else if (sharedContent instanceof StreamAudioContent streamAudioContent) {
			parent.addChild(new WebAudioStream(renderer, streamAudioContent.getName(), streamAudioContent.getUri(), streamAudioContent.getThumbnail()));
		} else if (sharedContent instanceof StreamVideoContent streamVideoContent) {
			parent.addChild(new WebVideoStream(renderer, streamVideoContent.getName(), streamVideoContent.getUri(), streamVideoContent.getThumbnail()));
		}
		setLastModified(1);
	}

	/**
	 * Clear all resources in children.
	 */
	public void clearBackupChildren() {
		Iterator<LibraryResource> resources = backupChildren.iterator();
		while (resources.hasNext()) {
			LibraryResource resource = resources.next();
			if (resource instanceof LibraryContainer container) {
				container.clearChildren();
			}
			resources.remove();
			renderer.getGlobalRepo().delete(resource);
		}
		backupChildren.clear();
	}

	public void reset() {
		if (isDiscovered()) {
			setDiscovered(false);
			discoverChildren();
		}
	}

	public void stopPlaying(LibraryResource res) {
		if (mon != null) {
			mon.stopped(res);
		}
	}

	// Returns the LibraryResource pointed to by the uri if it exists
	// or else a new Temp item (or null)
	public LibraryResource getValidResource(String uri, String name) {
		LOGGER.debug("Validating URI \"{}\"", uri);
		String objectId = parseObjectId(uri);
		if (objectId != null) {
			if (objectId.startsWith("Temp$")) {
				int index = tempFolder.indexOf(objectId);
				return index > -1 ? tempFolder.getChildren().get(index) : tempFolder.recreate(objectId, name);
			}
			return getLibraryResource(objectId);
		}
		return tempFolder.add(uri, name);
	}

	public synchronized LibraryResource getLibraryResource(String objectId) {
		// this method returns exactly ONE (1) LibraryResource
		// it's used when someone requests playback of mediaInfo. The mediaInfo must
		// have been discovered by someone first (unless it's a Temp item)

		if (objectId.startsWith("$LogIn/")) {
			String loginstring = StringUtils.substringAfter(objectId, "/");
			Integer userId = UserVirtualFolder.decrypt(loginstring);
			if (userId != null) {
				renderer.setAccount(AccountService.getAccountByUserId(userId));
				reset();
				discoverChildren();
				UmsContentDirectoryService.bumpSystemUpdateId();
			}
			return this;
		}

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			return getTemp().get(objectId);
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		String[] ids = objectId.split("\\.");
		if (objectId.equals("0")) {
			return this;
		} else {
			// only allow the last one here
			return renderer.getGlobalRepo().get(ids[ids.length - 1]);
		}
	}

	/**
	 * First thing it does it searches for an item matching the given objectID.
	 * If children is false, then it returns the found object as the only object
	 * in the list.
	 *
	 * TODO: (botijo) This function does a lot more than this!
	 *
	 * @param objectId ID to search for.
	 * @param children State if you want all the children in the returned list.
	 * @param start
	 * @param count
	 * @param renderer Renderer for which to do the actions.
	 * @return List of LibraryResource items.
	 * @throws IOException
	 */
	public synchronized List<LibraryResource> getLibraryResources(String objectId, boolean children, int start, int count) throws IOException {
		return getLibraryResources(objectId, children, start, count, null);
	}

	public synchronized List<LibraryResource> getLibraryResources(String objectId, boolean returnChildren, int start, int count,
			String searchStr) {
		ArrayList<LibraryResource> resources = new ArrayList<>();

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			List<LibraryResource> items = getTemp().asList(objectId);
			return items != null ? items : resources;
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		LibraryResource resource = null;
		String[] ids = objectId.split("\\.");
		if (objectId.equals("0")) {
			resource = this;
		} else {
			if (objectId.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
				try {
					resource = DbIdResourceLocator.locateResource(renderer, objectId);
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			} else {
				resource = renderer.getGlobalRepo().get(ids[ids.length - 1]);
			}
		}

		if (resource == null) {
			// nothing in the cache do a traditional search
			resource = search(ids);
			// resource = search(objectId, count, searchStr);
		}

		if (resource != null) {
			if (!(resource instanceof CodeEnter) && !isCodeValid(resource)) {
				LOGGER.debug("code is not valid any longer");
				return resources;
			}

			if (!isRendererAllowed()) {
				LOGGER.debug("renderer does not have access to this ressource");
				return resources;
			}

			if (!returnChildren) {
				resources.add(resource);
				if (resource instanceof LibraryContainer libraryContainer) {
					libraryContainer.refreshChildrenIfNeeded(searchStr);
				}
			} else {
				if (resource instanceof LibraryContainer libraryContainer) {
					libraryContainer.discover(count, true, searchStr);

					if (count == 0) {
						count = libraryContainer.getChildren().size();
					}

					if (count > 0) {
						String systemName = libraryContainer.getSystemName();
						ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(count);

						int nParallelThreads = 3;
						if (libraryContainer instanceof DVDISOFile) {
							// Some DVD drives die with 3 parallel threads
							nParallelThreads = 1;
						}

						ThreadPoolExecutor tpe = new ThreadPoolExecutor(Math.min(count, nParallelThreads), count, 20, TimeUnit.SECONDS, queue,
								new SimpleThreadFactory("LibraryResource resolver thread", true));

						if (shouldDoAudioTrackSorting(libraryContainer)) {
							sortChildrenWithAudioElements(libraryContainer);
						}
						for (int i = start; i < start + count && i < libraryContainer.getChildren().size(); i++) {
							final LibraryResource child = libraryContainer.getChildren().get(i);
							if (child != null) {
								tpe.execute(child);
								resources.add(child);
							} else {
								LOGGER.warn("null child at index {} in {}", i, systemName);
							}
						}

						try {
							tpe.shutdown();
							tpe.awaitTermination(20, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
							LOGGER.error("error while shutting down thread pool executor for " + systemName, e);
							Thread.currentThread().interrupt();
						}

						LOGGER.trace("End of analysis for " + systemName);
					}
				}
			}
		}

		return resources;
	}

	private LibraryResource search(String[] searchIds) {
		LibraryResource resource;
		for (String searchId : searchIds) {
			if (searchId.equals("0")) {
				resource = this;
			} else {
				resource = renderer.getGlobalRepo().get(searchId);
			}

			if (resource == null) {
				LOGGER.debug("Bad id {} found in path", searchId);
				return null;
			}

			if (resource instanceof LibraryContainer libraryContainer) {
				libraryContainer.discover(0, false, null);
			}
		}

		return renderer.getGlobalRepo().get(searchIds[searchIds.length - 1]);
	}

	public void fileRemoved(String filename) {
		File file = new File(filename);
		for (LibraryResource libraryResource : renderer.getGlobalRepo().findSystemFileResources(file)) {
			libraryResource.getParent().removeChild(libraryResource);
			libraryResource.getParent().notifyRefresh();
		}
	}

	public void fileAdded(String filename) {
		File file = new File(filename);
		File parentFile = file.getParentFile();
		for (LibraryResource libraryResource : renderer.getGlobalRepo().findSystemFileResources(parentFile)) {
			if (libraryResource instanceof VirtualFolder virtualFolder) {
				virtualFolder.addFile(file);
			}
		}
	}

	private LibraryResource findResourceFromFile(List<LibraryResource> resources, File file) {
		if (file == null || file.isHidden() || !(file.isFile() || file.isDirectory())) {
			return null;
		}
		if (file.isDirectory()) {
			if (file.getName().toUpperCase(Locale.ROOT).equals("VIDEO_TS")) {
				return findResourceFromFile(resources, file, DVDISOFile.class);
			} else {
				return findResourceFromFile(resources, file, RealFolder.class);
			}
		}
		String lcFilename = file.getName().toLowerCase();
		if (renderer.getUmsConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".zip") || lcFilename.endsWith(".cbz"))) {
			return findResourceFromFile(resources, file, ZippedFile.class);
		} else if (renderer.getUmsConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".rar") || lcFilename.endsWith(".cbr"))) {
			return findResourceFromFile(resources, file, RarredFile.class);
		} else if (renderer.getUmsConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".tar") ||
				lcFilename.endsWith(".gzip") ||
				lcFilename.endsWith(".gz") ||
				lcFilename.endsWith(".7z"))) {
			return findResourceFromFile(resources, file, SevenZipFile.class);
		} else if (lcFilename.endsWith(".iso") ||
				lcFilename.endsWith(".img")) {
			return findResourceFromFile(resources, file, DVDISOFile.class);
		} else if (lcFilename.endsWith(".m3u") ||
				lcFilename.endsWith(".m3u8") ||
				lcFilename.endsWith(".pls")) {
			return findResourceFromFile(resources, file, PlaylistFolder.class);
		} else if (lcFilename.endsWith(".cue")) {
			return findResourceFromFile(resources, file, CueFolder.class);
		} else if (lcFilename.endsWith(".ups")) {
			return findResourceFromFile(resources, file, Playlist.class);
		} else {
			return findResourceFromFile(resources, file, RealFile.class);
		}
	}

	private LibraryResource findResourceFromFile(List<LibraryResource> resources, File file, Class<?> resourceClass) {
		if (file == null || file.isHidden() || !(file.isFile() || file.isDirectory())) {
			return null;
		}
		for (LibraryResource resource : resources) {
			if (resourceClass.isInstance(resource) && resource instanceof SystemFileResource systemFileResource && file.equals(systemFileResource.getSystemFile())) {
				return resource;
			}
		}
		return null;
	}

	public LibraryResource createResourceFromFile(File file) {
		if (file == null || file.isHidden() || !(file.isFile() || file.isDirectory())) {
			return null;
		}
		String lcFilename = file.getName().toLowerCase();
		if (renderer.getUmsConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".zip") || lcFilename.endsWith(".cbz"))) {
			return new ZippedFile(renderer, file);
		} else if (renderer.getUmsConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".rar") || lcFilename.endsWith(".cbr"))) {
			return new RarredFile(renderer, file);
		} else if (renderer.getUmsConfiguration().isArchiveBrowsing() && (lcFilename.endsWith(".tar") ||
				lcFilename.endsWith(".gzip") ||
				lcFilename.endsWith(".gz") ||
				lcFilename.endsWith(".7z"))) {
			return new SevenZipFile(renderer, file);
		} else if (lcFilename.endsWith(".iso") ||
				lcFilename.endsWith(".img") || (file.isDirectory() &&
				file.getName().toUpperCase(Locale.ROOT).equals("VIDEO_TS"))) {
			return new DVDISOFile(renderer, file);
		} else if (lcFilename.endsWith(".m3u") ||
				lcFilename.endsWith(".m3u8") ||
				lcFilename.endsWith(".pls") ||
				lcFilename.endsWith(".cue") ||
				lcFilename.endsWith(".ups")) {
			LibraryContainer d = PlaylistFolder.getPlaylist(renderer, lcFilename, file.getAbsolutePath(), 0);
			if (d != null) {
				return d;
			}
		} else {
			ArrayList<String> ignoredFolderNames = renderer.getUmsConfiguration().getIgnoredFolderNames();

			/* Optionally ignore empty directories */
			if (file.isDirectory() && renderer.getUmsConfiguration().isHideEmptyFolders() && !FileUtil.isFolderRelevant(file, renderer.getUmsConfiguration())) {
				LOGGER.debug("Ignoring empty/non-relevant directory: " + file.getName());
			} else if (file.isDirectory() && !ignoredFolderNames.isEmpty() && ignoredFolderNames.contains(file.getName())) {
				LOGGER.debug("Ignoring {} because it is in the ignored folders list", file.getName());
			} else {
				// Otherwise add the file
				if (file.isDirectory()) {
					return new RealFolder(renderer, file);
				} else {
					RealFile rf = new RealFile(renderer, file);
					if (rf.length() == 0) {
						LOGGER.debug("Ignoring {} because it seems corrupted when the length of the file is 0", file.getName());
						return null;
					}
					return rf;
				}
			}
		}
		return null;
	}

	/**
	 * TODO: move that under the media library as it should (like tv series)
	 */
	private void addVirtualMyMusicFolder() {
		DbIdTypeAndIdent myAlbums = new DbIdTypeAndIdent(DbIdMediaType.TYPE_MYMUSIC_ALBUM, null);
		VirtualFolderDbId myMusicFolder = new VirtualFolderDbId(renderer, Messages.getString("MyAlbums"), myAlbums, "");
		if (PMS.getConfiguration().displayAudioLikesInRootFolder()) {
			if (!getChildren().contains(myMusicFolder)) {
				myMusicFolder.setFakeParentId("0");
				addChild(myMusicFolder, true, false);
				LOGGER.debug("adding My Music folder to root");
			}
		} else {
			if (mediaLibrary.isEnabled() &&
					mediaLibrary.getAudioFolder() != null &&
					mediaLibrary.getAudioFolder().getChildren() != null &&
					!mediaLibrary.getAudioFolder().getChildren().contains(myMusicFolder)) {
				myMusicFolder.setFakeParentId(mediaLibrary.getAudioFolder().getId());
				mediaLibrary.getAudioFolder().addChild(myMusicFolder, true, false);
				LOGGER.debug("adding My Music folder to 'Audio' folder");
			} else {
				LOGGER.debug("couldn't add 'My Music' folder because the media library is not initialized.");
			}
		}
	}

	@Override
	public String toString() {
		return "RootFolder[" + getChildren() + "]";
	}

	/**
	 * Check if all audio child elements belong to the same album. Here the
	 * Album string is matched. Another more strict alternative implementation
	 * could match the MBID record id (not implemented).
	 *
	 * @param resource Folder containing child objects of any kind
	 *
	 * @return TRUE, if AudioTrackSorting is not disabled, all audio child
	 * objects belong to the same album and the majority of files are audio.
	 */
	private static boolean shouldDoAudioTrackSorting(LibraryContainer resource) {
		if (!PMS.getConfiguration().isSortAudioTracksByAlbumPosition()) {
			LOGGER.trace("shouldDoAudioTrackSorting : {}", PMS.getConfiguration().isSortAudioTracksByAlbumPosition());
			return false;
		}

		String album = null;
		String mbReleaseId = null;
		int numberOfAudioFiles = 0;
		int numberOfOtherFiles = 0;

		boolean audioExists = false;
		for (LibraryResource res : resource.getChildren()) {
			if (res instanceof LibraryItem item && item.getFormat() != null && item.getFormat().isAudio()) {
				if (res.getMediaInfo() == null || !res.getMediaInfo().hasAudioMetadata()) {
					LOGGER.warn("Audio resource has no AudioMetadata : {}", res.getDisplayName());
					continue;
				}
				MediaAudioMetadata metadata = res.getMediaInfo().getAudioMetadata();
				numberOfAudioFiles++;
				if (album == null) {
					audioExists = true;
					album = metadata.getAlbum() != null ? metadata.getAlbum() : "";
					mbReleaseId = metadata.getMbidRecord();
					if (StringUtils.isAllBlank(album) && StringUtils.isAllBlank(mbReleaseId)) {
						return false;
					}
				} else {
					if (mbReleaseId != null && !StringUtils.isAllBlank(mbReleaseId)) {
						// First check musicbrainz ReleaseID
						if (!mbReleaseId.equals(metadata.getMbidRecord())) {
							return false;
						}
					} else if (!album.equals(metadata.getAlbum())) {
						return false;
					}
				}
			} else {
				numberOfOtherFiles++;
			}
		}
		return audioExists && (numberOfAudioFiles > numberOfOtherFiles);
	}

	private static void sortChildrenWithAudioElements(LibraryContainer resource) {
		Collections.sort(resource.getChildren(), (LibraryResource o1, LibraryResource o2) -> {
			if (getDiscNum(o1) == null || getDiscNum(o2) == null || getDiscNum(o1).equals(getDiscNum(o2))) {
				if (o1 instanceof LibraryItem item1 && item1.getFormat() != null && item1.getFormat().isAudio()) {
					if (o2 instanceof LibraryItem item2 && item2.getFormat() != null && item2.getFormat().isAudio()) {
						return getTrackNum(o1).compareTo(getTrackNum(o2));
					} else {
						return o1.getDisplayNameBase().compareTo(o2.getDisplayNameBase());
					}
				} else {
					return o1.getDisplayNameBase().compareTo(o2.getDisplayNameBase());
				}
			} else {
				return getDiscNum(o1).compareTo(getDiscNum(o2));
			}
		});
	}

	private static Integer getTrackNum(LibraryResource res) {
		if (res != null && res.getMediaInfo() != null && res.getMediaInfo().hasAudioMetadata()) {
			return res.getMediaInfo().getAudioMetadata().getTrack();
		}
		return 0;
	}

	private static Integer getDiscNum(LibraryResource res) {
		if (res != null && res.getMediaInfo() != null && res.getMediaInfo().hasAudioMetadata()) {
			return res.getMediaInfo().getAudioMetadata().getDisc();
		}
		return 0;
	}

}
