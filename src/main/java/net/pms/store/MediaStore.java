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

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
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
import net.pms.media.audio.metadata.MediaAudioMetadata;
import net.pms.renderers.Renderer;
import net.pms.store.container.ApertureLibraries;
import net.pms.store.container.AudiosFeed;
import net.pms.store.container.CodeEnter;
import net.pms.store.container.CueFolder;
import net.pms.store.container.DVDISOFile;
import net.pms.store.container.DynamicPlaylist;
import net.pms.store.container.FolderLimit;
import net.pms.store.container.IPhotoLibrary;
import net.pms.store.container.ITunesLibrary;
import net.pms.store.container.ImagesFeed;
import net.pms.store.container.MediaLibrary;
import net.pms.store.container.MediaMonitor;
import net.pms.store.container.Playlist;
import net.pms.store.container.PlaylistFolder;
import net.pms.store.container.RarredFile;
import net.pms.store.container.RealFolder;
import net.pms.store.container.SearchFolder;
import net.pms.store.container.ServerSettingsFolder;
import net.pms.store.container.SevenZipFile;
import net.pms.store.container.UnattachedFolder;
import net.pms.store.container.UserVirtualFolder;
import net.pms.store.container.VideosFeed;
import net.pms.store.container.VirtualFolder;
import net.pms.store.container.VirtualFolderDbId;
import net.pms.store.container.ZippedFile;
import net.pms.store.item.RealFile;
import net.pms.store.item.WebAudioStream;
import net.pms.store.item.WebVideoStream;
import net.pms.store.utils.IOList;
import net.pms.util.FileUtil;
import net.pms.util.SimpleThreadFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MediaStore extends StoreContainer {

	private static final Logger LOGGER = LoggerFactory.getLogger(MediaStore.class);

	private final Map<Long, WeakReference<StoreResource>> weakResources = new HashMap<>();
	// A temp folder for non-xmb items
	private final UnattachedFolder tempFolder;
	private final MediaLibrary mediaLibrary;
	private VirtualFolderDbId audioLikesFolder;
	private VirtualFolderDbId nonVisibleDbIdFolder;
	private DynamicPlaylist dynamicPls;
	private FolderLimit lim;
	private MediaMonitor mon;

	/**
	 * List of children objects backuped when discoverChildren.
	 */
	private final List<StoreResource> backupChildren = new ArrayList<>();

	public MediaStore(Renderer renderer) {
		super(renderer, "root", null);
		tempFolder = new UnattachedFolder(renderer, "Temp");
		mediaLibrary = new MediaLibrary(renderer);
		setLongId(0);
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
	public MediaLibrary getMediaLibrary() {
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
		LOGGER.debug("Discovering the root folder for " + renderer.getRendererName());

		//clear childrens but keep copy until discovered
		backupChildren.clear();
		for (StoreResource storeResource : getChildren()) {
			backupChildren.add(storeResource);
		}
		getChildren().clear();

		if (renderer.getUmsConfiguration().isShowMediaLibraryFolder()) {
			if (backupChildren.contains(mediaLibrary)) {
				addChildInternal(mediaLibrary, false);
				backupChildren.remove(mediaLibrary);
			} else {
				addChild(mediaLibrary);
			}
		}

		setAudioLikesFolder();
		setDbidFolder();

		if (mon != null) {
			mon.clearChildren();
		}
		List<File> foldersMonitored = SharedContentConfiguration.getMonitoredFolders();
		if (!foldersMonitored.isEmpty()) {
			File[] dirs = foldersMonitored.toArray(File[]::new);
			mon = new MediaMonitor(renderer, dirs);
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
				StoreResource plsResource = findResourceFromFile(backupChildren, plsdir);
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
			StoreResource serverSettingsFolder = findSystemNameInResources(backupChildren, "ServerSettings");
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

	private StoreContainer findSystemNameInResources(List<StoreResource> resources, String systemName) {
		if (systemName == null) {
			return null;
		}
		for (StoreResource resource : resources) {
			if (resource instanceof StoreContainer container && systemName.equals(container.getSystemName())) {
				return container;
			}
		}
		return null;
	}

	public void setFolderLim(StoreResource r) {
		if (lim != null) {
			lim.setStart(r);
		}
	}

	private void setSharedContents() {
		List<StoreResource> realSystemFileResources = new ArrayList<>();
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
						StoreResource realSystemFileResource = setFolderContent(folder);
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
			SearchFolder sf = new SearchFolder(renderer, "SearchDiscFolders", new FileSearch(realSystemFileResources));
			addChild(sf);
		}
	}

	private StoreResource setFolderContent(FolderContent folderContent) {
		if (folderContent.getFile() != null) {
			StoreResource realSystemFileResource = findResourceFromFile(backupChildren, folderContent.getFile());
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
		StoreContainer parent = getSharedContentParent(virtualFolderContent.getParent());
		parent.addChild(new VirtualFolder(renderer, virtualFolderContent));
	}

	private void setITunesContent(ITunesContent iTunesContent) {
		int osType = Platform.getOSType();
		if (osType == Platform.MAC || osType == Platform.WINDOWS) {
			StoreResource iTunesRes = findSystemNameInResources(backupChildren, "ItunesLibrary");
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
			StoreResource apertureRes = findSystemNameInResources(backupChildren, "ApertureLibrary");
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
			StoreResource iPhotoRes = findSystemNameInResources(backupChildren, "IphotoLibrary");
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
		StoreContainer parent = getSharedContentParent(sharedContent.getParent());
		// Handle web playlists stream
		if (sharedContent instanceof StreamContent streamContent) {
			StoreResource playlist = PlaylistFolder.getPlaylist(renderer, streamContent.getName(), streamContent.getUri(), streamContent.getFormat());
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
		Iterator<StoreResource> backupResources = backupChildren.iterator();
		while (backupResources.hasNext()) {
			StoreResource resource = backupResources.next();
			if (resource instanceof StoreContainer container) {
				container.clearChildren();
			}
			backupResources.remove();
			deleteWeakResource(resource);
		}
		backupChildren.clear();
	}

	public void reset() {
		if (isDiscovered()) {
			setDiscovered(false);
			discoverChildren();
		}
	}

	public void stopPlaying(StoreResource res) {
		if (mon != null) {
			mon.stopped(res);
		}
	}

	// Returns the LibraryResource pointed to by the uri if it exists
	// or else a new Temp item (or null)
	public StoreResource getValidResource(String uri, String name) {
		LOGGER.debug("Validating URI \"{}\"", uri);
		String objectId = parseObjectId(uri);
		if (objectId != null) {
			if (objectId.startsWith("Temp$")) {
				int index = tempFolder.indexOf(objectId);
				return index > -1 ? tempFolder.getChildren().get(index) : tempFolder.recreate(objectId, name);
			}
			return getResource(objectId);
		}
		return tempFolder.add(uri, name);
	}

	public synchronized StoreResource getResource(String objectId) {
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
			}
			return this;
		}

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			return getTemp().get(objectId);
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		if (objectId.equals("0")) {
			return this;
		}
		if (objectId.startsWith(DbIdMediaType.GENERAL_PREFIX)) {
			try {
				// this is direct acceded resource.
				// as we don't know what was it's parent, let find one or fail.
				DbIdTypeAndIdent typeAndIdent = DbIdMediaType.getTypeIdentByDbid(objectId);
				StoreResource dbid = DbIdResourceLocator.getLibraryResourceByDbTypeIdent(renderer, typeAndIdent);
				return dbid;
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}
		// only allow the last one here
		String[] ids = objectId.split("\\.");
		StoreResource resource = getWeakResource(ids[ids.length - 1]);
		if (resource instanceof VirtualFolderDbId virtualFolderDbId) {
			resource = DbIdResourceLocator.locateResource(renderer, virtualFolderDbId);
		}
		return resource;
	}

	private StoreResource getWeakResource(String objectId) {
		Long id = parseIndex(objectId);
		if (id == null) {
			return null;
		}
		if (weakResources.containsKey(id) && weakResources.get(id).get() != null) {
			return weakResources.get(id).get();
		} else {
			// object id not founded, try recreate
			return recreateResource(id);
		}
	}

	/**
	 * Try to recreate the item tree if possible.
	 *
	 * @param id
	 * @return
	 */
	private StoreResource recreateResource(long id) {
		List<MediaStoreId> libraryIds = MediaStoreIds.getMediaStoreResourceTree(id);
		if (!libraryIds.isEmpty()) {
			for (MediaStoreId libraryId : libraryIds) {
				if (weakResources.containsKey(libraryId.getId()) && weakResources.get(libraryId.getId()).get() != null) {
					StoreResource resource = weakResources.get(libraryId.getId()).get();
					if (resource instanceof StoreContainer container) {
						container.discoverChildren();
					}
				}
			}
			//now that parent folders are discovered, try to get the resource
			if (weakResources.containsKey(id) && weakResources.get(id).get() != null) {
				return weakResources.get(id).get();
			}
		}
		return null;
	}

	public synchronized boolean weakResourceExists(String objectId) {
		Long id = parseIndex(objectId);
		return (id != null && weakResources.containsKey(id) && weakResources.get(id).get() != null);
	}

	public boolean addWeakResource(StoreResource resource) {
		Long id = MediaStoreIds.getMediaStoreResourceId(resource);
		if (id != null) {
			weakResources.put(id, new WeakReference<>(resource));
			return true;
		}
		return false;
	}

	public void replaceWeakResource(StoreResource a, StoreResource b) {
		Long id = parseIndex(a.getId());
		if (id != null && weakResources.containsKey(id)) {
			weakResources.get(id).clear();
			weakResources.put(id, new WeakReference<>(b));
		}
	}

	public synchronized void deleteWeakResource(StoreResource resource) {
		Long id = parseIndex(resource.getId());
		if (id != null && weakResources.containsKey(id)) {
			weakResources.get(id).clear();
			weakResources.remove(id);
		}
	}

	public synchronized void clearWeakResources() {
		weakResources.clear();
	}

	public synchronized List<StoreResource> findSystemFileResources(File file) {
		List<StoreResource> systemFileResources = new ArrayList<>();
		for (WeakReference<StoreResource> resource : weakResources.values()) {
			if (resource.get() instanceof SystemFileResource systemFileResource &&
					file.equals(systemFileResource.getSystemFile()) &&
					systemFileResource instanceof StoreResource storeResource) {
				systemFileResources.add(storeResource);
			}
		}
		return systemFileResources;
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
	public synchronized List<StoreResource> getResources(String objectId, boolean children, int start, int count) throws IOException {
		return getResources(objectId, children, start, count, null);
	}

	public synchronized List<StoreResource> getResources(String objectId, boolean returnChildren, int start, int count,
		String searchStr) {
		return getResources(objectId, returnChildren, start, count, searchStr, null);
	}

	public synchronized List<StoreResource> getResources(String objectId, boolean returnChildren, int start, int count,
			String searchStr, String lang) {
		ArrayList<StoreResource> resources = new ArrayList<>();

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp")) {
			List<StoreResource> items = getTemp().asList(objectId);
			return items != null ? items : resources;
		}

		StoreResource resource = getResource(objectId);

		if (resource == null) {
			// nothing in the cache do a traditional search
			// Now strip off the filename
			objectId = StringUtils.substringBefore(objectId, "/");
			String[] ids = objectId.split("\\.");
			resource = search(ids, lang);
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
				if (resource instanceof StoreContainer storeContainer) {
					storeContainer.refreshChildrenIfNeeded(searchStr, lang);
				}
			} else {
				if (resource instanceof StoreContainer storeContainer) {
					storeContainer.discover(count, true, searchStr, lang);

					if (count == 0) {
						count = storeContainer.getChildren().size();
					}

					if (count > 0) {
						String systemName = storeContainer.getSystemName();
						LOGGER.trace("Start of analysis for " + systemName);
						ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(count);

						int nParallelThreads = 3;
						if (storeContainer instanceof DVDISOFile) {
							// Some DVD drives die with 3 parallel threads
							nParallelThreads = 1;
						}

						ThreadPoolExecutor tpe = new ThreadPoolExecutor(Math.min(count, nParallelThreads), count, 20, TimeUnit.SECONDS, queue,
								new SimpleThreadFactory("LibraryResource resolver thread", true));

						if (shouldDoAudioTrackSorting(storeContainer)) {
							sortChildrenWithAudioElements(storeContainer);
						}
						for (int i = start; i < start + count && i < storeContainer.getChildren().size(); i++) {
							final StoreResource child = storeContainer.getChildren().get(i);
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

	private StoreResource search(String[] searchIds, String lang) {
		StoreResource resource;
		for (String searchId : searchIds) {
			if (searchId.equals("0")) {
				resource = this;
			} else {
				resource = getWeakResource(searchId);
			}

			if (resource == null) {
				LOGGER.debug("Bad id {} found in path", searchId);
				return null;
			}

			if (resource instanceof StoreContainer storeContainer) {
				storeContainer.discover(0, false, null, lang);
			}
		}

		return getWeakResource(searchIds[searchIds.length - 1]);
	}

	public void fileRemoved(String filename) {
		File file = new File(filename);
		for (StoreResource storeResource : findSystemFileResources(file)) {
			storeResource.getParent().removeChild(storeResource);
			storeResource.getParent().notifyRefresh();
		}
	}

	public void fileAdded(String filename) {
		File file = new File(filename);
		File parentFile = file.getParentFile();
		for (StoreResource storeResource : findSystemFileResources(parentFile)) {
			if (storeResource instanceof VirtualFolder virtualFolder) {
				virtualFolder.addFile(file);
			}
		}
	}

	private StoreResource findResourceFromFile(List<StoreResource> resources, File file) {
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

	private StoreResource findResourceFromFile(List<StoreResource> resources, File file, Class<?> resourceClass) {
		if (file == null || file.isHidden() || !(file.isFile() || file.isDirectory())) {
			return null;
		}
		for (StoreResource resource : resources) {
			if (resourceClass.isInstance(resource) && resource instanceof SystemFileResource systemFileResource && file.equals(systemFileResource.getSystemFile())) {
				return resource;
			}
		}
		return null;
	}

	public StoreResource createResourceFromFile(File file) {
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
			StoreContainer d = PlaylistFolder.getPlaylist(renderer, lcFilename, file.getAbsolutePath(), 0);
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

	public VirtualFolderDbId getAudioLikesFolder() {
		return audioLikesFolder;
	}

	/**
	 * TODO: move that under the media library as it should (like tv series)
	 */
	private void setAudioLikesFolder() {
		if (CONFIGURATION.useNextcpApi()) {
			if (audioLikesFolder == null) {
				audioLikesFolder = new VirtualFolderDbId(renderer, "MyAlbums", new DbIdTypeAndIdent(DbIdMediaType.TYPE_MYMUSIC_ALBUM, null));
			}
			if (PMS.getConfiguration().displayAudioLikesInRootFolder()) {
				if (backupChildren.contains(audioLikesFolder)) {
					addChildInternal(audioLikesFolder, false);
				} else {
					addChild(audioLikesFolder);
				}
				LOGGER.debug("adding My Albums folder to the root of MediaStore");
			} else if (renderer.getUmsConfiguration().isShowMediaLibraryFolder() &&
					mediaLibrary.getAudioFolder() != null &&
					mediaLibrary.getAudioFolder().getChildren() != null &&
					!mediaLibrary.getAudioFolder().getChildren().contains(audioLikesFolder)) {
				mediaLibrary.getAudioFolder().addChild(audioLikesFolder);
				LOGGER.debug("adding My Albums folder to the 'Audio' folder of MediaLibrary");
			}
			if (backupChildren.contains(audioLikesFolder)) {
				backupChildren.remove(audioLikesFolder);
			}
		}
	}

	/**
	 * Root of DBID elements
	 * @return
	 */
	public VirtualFolderDbId getDbIdFolder() {
		return nonVisibleDbIdFolder;
	}

	private void setDbidFolder() {
		nonVisibleDbIdFolder = new VirtualFolderDbId(renderer, "DBID", new DbIdTypeAndIdent(DbIdMediaType.TYPE_FOLDER, null));
		mediaLibrary.addChildInternal(nonVisibleDbIdFolder);
	}

	@Override
	public String toString() {
		return "MediaStore[" + getChildren() + "]";
	}

	private static Long parseIndex(String id) {
		try {
			// Id strings may have optional tags beginning with $ appended, e.g. '1234$Temp'
			return Long.valueOf(StringUtils.substringBefore(id, "$"));
		} catch (NumberFormatException e) {
			return null;
		}
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
	private static boolean shouldDoAudioTrackSorting(StoreContainer resource) {
		if (!PMS.getConfiguration().isSortAudioTracksByAlbumPosition()) {
			LOGGER.trace("shouldDoAudioTrackSorting : {}", PMS.getConfiguration().isSortAudioTracksByAlbumPosition());
			return false;
		}

		String album = null;
		String mbReleaseId = null;
		int numberOfAudioFiles = 0;
		int numberOfOtherFiles = 0;

		boolean audioExists = false;
		for (StoreResource res : resource.getChildren()) {
			if (res instanceof StoreItem item && item.getFormat() != null && item.getFormat().isAudio()) {
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

	private static void sortChildrenWithAudioElements(StoreContainer resource) {
		Collections.sort(resource.getChildren(), (StoreResource o1, StoreResource o2) -> {
			if (getDiscNum(o1) == null || getDiscNum(o2) == null || getDiscNum(o1).equals(getDiscNum(o2))) {
				if (o1 instanceof StoreItem item1 && item1.getFormat() != null && item1.getFormat().isAudio()) {
					if (o2 instanceof StoreItem item2 && item2.getFormat() != null && item2.getFormat().isAudio()) {
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

	private static Integer getTrackNum(StoreResource res) {
		if (res != null && res.getMediaInfo() != null && res.getMediaInfo().hasAudioMetadata()) {
			return res.getMediaInfo().getAudioMetadata().getTrack();
		}
		return 0;
	}

	private static Integer getDiscNum(StoreResource res) {
		if (res != null && res.getMediaInfo() != null && res.getMediaInfo().hasAudioMetadata()) {
			return res.getMediaInfo().getAudioMetadata().getDisc();
		}
		return 0;
	}

}