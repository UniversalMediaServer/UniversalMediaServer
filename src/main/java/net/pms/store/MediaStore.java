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
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
import net.pms.store.container.UmsPlaylist;
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
	/**
	 * An AtomicInteger to prevent heavy IO tasks from causing browsing to be less
	 * responsive.
	 * <p>
	 * When a MediaStore ask for resource (needs to run in realtime), it should
	 * increment this AtomicInteger for the duration of their operation.
	 * When a task has a lower priority, it should use waitWorkers() to wait for
	 * any realtime task to finish.
	 */
	private static final AtomicInteger WORKERS = new AtomicInteger(0);
	private static final String TEMP_TAG = "$Temp$";

	private final Map<Long, WeakReference<StoreResource>> weakResources = new HashMap<>();
	private final Map<Long, Object> idLocks = new HashMap<>();
	// A temp folder for non-xmb items
	private final UnattachedFolder tempFolder;
	private final MediaLibrary mediaLibrary;
	private final DbIdLibrary dbIdLibrary;

	private DynamicPlaylist dynamicPls;
	private FolderLimit lim;
	private MediaMonitor mon;

	/**
	 * List of children objects backuped when discoverChildren.
	 */
	private final List<StoreResource> backupChildren = new ArrayList<>();

	public MediaStore(Renderer renderer) {
		super(renderer, "root", null);
		tempFolder = new UnattachedFolder(renderer, TEMP_TAG);
		mediaLibrary = new MediaLibrary(renderer);
		dbIdLibrary = new DbIdLibrary(renderer);
		setLongId(0);
	}

	public UnattachedFolder getTemp() {
		return tempFolder;
	}

	public UmsPlaylist getDynamicPls() {
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

	/**
	 * Returns the DbIdLibrary.
	 *
	 * @return The current {@link DbIdLibrary}.
	 */
	public DbIdLibrary getDbIdLibrary() {
		return dbIdLibrary;
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

		dbIdLibrary.reset(backupChildren);

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
				realSystemFileResource = createResourceFromFile(folderContent.getFile(), true);
				if (realSystemFileResource != null) {
					addChild(realSystemFileResource, true, true);
				} else {
					LOGGER.trace("createResourceFromFile has failed for {}", folderContent.getFile());
				}
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
			StoreResource playlist = PlaylistManager.getPlaylist(renderer, streamContent.getName(), streamContent.getUri(), streamContent.getFormat());
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
	private void clearBackupChildren() {
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
			if (objectId.startsWith(TEMP_TAG)) {
				int index = tempFolder.indexOf(objectId);
				return index > -1 ? tempFolder.getChildren().get(index) : tempFolder.recreate(objectId, name);
			}
			return getResource(objectId);
		}
		return tempFolder.add(uri, name);
	}

	public StoreResource getResource(String objectId) {
		// this method returns exactly ONE (1) LibraryResource
		// it's used when someone requests playback of mediaInfo. The mediaInfo must
		// have been discovered by someone first (unless it's a Temp item)
		try {
			WORKERS.incrementAndGet();
			if (StringUtils.isEmpty(objectId)) {
				return null;
			}
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
			if (objectId.startsWith(TEMP_TAG)) {
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
					return DbIdResourceLocator.getLibraryResourceByDbTypeIdent(renderer, typeAndIdent);
				} catch (Exception e) {
					LOGGER.error("", e);
				}
			}
			// only allow the last one here
			String[] ids = objectId.split("\\.");
			return getWeakResource(ids[ids.length - 1]);
		} finally {
			WORKERS.decrementAndGet();
		}
	}

	private Object getIdLock(Long id) {
		synchronized (idLocks) {
			if (idLocks.containsKey(id)) {
				return idLocks.get(id);
			}
			Object idLock = new Object();
			idLocks.put(id, idLock);
			return idLock;
		}
	}

	private StoreResource getWeakResource(String objectId) {
		Long id = parseIndex(objectId);
		if (id == null) {
			return null;
		}
		Object idLock = getIdLock(id);
		synchronized (idLock) {
			StoreResource res = getWeakResource(id);
			if (res != null) {
				return res;
			} else {
				// object id not found, try recreate
				return recreateResource(id);
			}
		}
	}

	/**
	 * Try to recreate the item tree if possible.
	 *
	 * @param id
	 * @return
	 */
	private StoreResource recreateResource(long id) {
		LOGGER.trace("try recreating resource with id '{}'", id);
		List<MediaStoreId> libraryIds = MediaStoreIds.getMediaStoreResourceTree(id);
		if (!libraryIds.isEmpty()) {
			for (MediaStoreId libraryId : libraryIds) {
				StoreResource parent = getWeakResource(libraryId.getId());
				if (parent instanceof StoreContainer container) {
					container.discoverChildren();
				}
			}
			//now that parent folders are discovered, try to get the resource
			StoreResource resource = getWeakResource(id);
			if (resource != null) {
				LOGGER.trace("resource with id '{}' recreacted succefully", id);
				return resource;
			} else {
				LOGGER.trace("resource with id '{}' is no longer available in the store tree", id);
			}
		} else {
			LOGGER.trace("resource with id '{}' was not found in database", id);
		}
		return null;
	}

	public boolean weakResourceExists(String objectId) {
		Long id = parseIndex(objectId);
		return getWeakResource(id) != null;
	}

	private StoreResource getWeakResource(Long id) {
		synchronized (weakResources) {
			if (id != null && weakResources.containsKey(id) && weakResources.get(id).get() != null) {
				return weakResources.get(id).get();
			}
		}
		return null;
	}

	private boolean addWeakResource(Long id, StoreResource resource) {
		if (id != null) {
			synchronized (weakResources) {
				weakResources.put(id, new WeakReference<>(resource));
			}
			return true;
		}
		return false;
	}

	public boolean addWeakResource(StoreResource resource) {
		Long id = MediaStoreIds.getMediaStoreResourceId(resource);
		return addWeakResource(id, resource);
	}

	public void replaceWeakResource(StoreResource a, StoreResource b) {
		Long id = parseIndex(a.getId());
		synchronized (weakResources) {
			if (id != null && weakResources.containsKey(id)) {
				weakResources.get(id).clear();
				weakResources.put(id, new WeakReference<>(b));
			}
		}
	}

	public void deleteWeakResource(StoreResource resource) {
		Long id = parseIndex(resource.getId());
		synchronized (weakResources) {
			if (id != null && weakResources.containsKey(id)) {
				weakResources.get(id).clear();
				weakResources.remove(id);
			}
		}
	}

	public void clearWeakResources() {
		synchronized (weakResources) {
			weakResources.clear();
		}
	}

	public List<StoreResource> findSystemFileResources(File file) {
		List<StoreResource> systemFileResources = new ArrayList<>();
		synchronized (weakResources) {
			for (WeakReference<StoreResource> resource : weakResources.values()) {
				if (resource.get() instanceof SystemFileResource systemFileResource &&
						file.equals(systemFileResource.getSystemFile()) &&
						systemFileResource instanceof StoreResource storeResource) {
					systemFileResources.add(storeResource);
				}
			}
		}
		return systemFileResources;
	}

	/**
	 * First thing it does it searches for an item matching the given objectID.
	 * If children is false, then it returns the found object as the only object
	 * in the list.
	 *
	 * @param objectId ID to search for.
	 * @param returnChildren State if you want all the children in the returned list.
	 * @return List of LibraryResource items.
	 * @throws IOException
	 */
	public List<StoreResource> getResources(String objectId, boolean returnChildren) {
		try {
			WORKERS.incrementAndGet();
			ArrayList<StoreResource> resources = new ArrayList<>();
			if (StringUtils.isEmpty(objectId)) {
				return resources;
			}

			// Get/create/reconstruct it if it's a Temp item
			if (objectId.startsWith(TEMP_TAG)) {
				List<StoreResource> items = getTemp().asList(objectId);
				return items != null ? items : resources;
			}

			StoreResource resource = getResource(objectId);

			if (resource == null) {
				// nothing in the cache do a traditional search
				// Now strip off the filename
				objectId = StringUtils.substringBefore(objectId, "/");
				String[] ids = objectId.split("\\.");
				resource = search(ids);
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
						if (!storeContainer.isDiscovered()) {
							storeContainer.discover(false);
						} else {
							storeContainer.refreshChildrenIfNeeded();
						}
					}
				} else {
					if (resource instanceof StoreContainer storeContainer) {
						storeContainer.discover(true);

						int count = storeContainer.getChildren().size();
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
							for (int i = 0; i < storeContainer.getChildren().size(); i++) {
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
		} finally {
			WORKERS.decrementAndGet();
		}
	}

	private StoreResource search(String[] searchIds) {
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
				storeContainer.discover(false);
			}
		}

		return getWeakResource(searchIds[searchIds.length - 1]);
	}

	public void fileRemoved(File file) {
		for (StoreResource storeResource : findSystemFileResources(file)) {
			storeResource.getParent().removeChild(storeResource);
			storeResource.getParent().notifyRefresh();
		}
	}

	public void fileAdded(File file) {
		File parentFile = file.getParentFile();
		for (StoreResource storeResource : findSystemFileResources(parentFile)) {
			if (storeResource instanceof VirtualFolder virtualFolder) {
				virtualFolder.doRefreshChildren();
			}
		}
	}

	private StoreResource findResourceFromFile(List<StoreResource> resources, File file) {
		if (file == null || file.isHidden() || !file.canRead() || !(file.isFile() || file.isDirectory())) {
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
			return findResourceFromFile(resources, file, UmsPlaylist.class);
		} else {
			return findResourceFromFile(resources, file, RealFile.class);
		}
	}

	private StoreResource findResourceFromFile(List<StoreResource> resources, File file, Class<?> resourceClass) {
		if (file == null || file.isHidden() || !file.canRead() || !(file.isFile() || file.isDirectory())) {
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
		return createResourceFromFile(file, false);
	}

	public StoreResource createResourceFromFile(File file, boolean allowHidden) {
		if (file == null) {
			LOGGER.trace("createResourceFromFile return null as file is null.");
			return null;
		} else if (!allowHidden && file.isHidden()) {
			LOGGER.trace("createResourceFromFile return null as {} is hidden.", file.toString());
			return null;
		} else if (!file.canRead()) {
			LOGGER.trace("createResourceFromFile return null as {} is unreadable.", file.toString());
			return null;
		} else if (!(file.isFile() || file.isDirectory())) {
			LOGGER.trace("createResourceFromFile return null as {} is neither a file or a directory.", file.toString());
			return null;
		}

		String lcFilename = file.getName();
		if (lcFilename == null) {
			lcFilename = "";
		} else {
			lcFilename = lcFilename.toLowerCase();
		}
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
				lcFilename.toUpperCase(Locale.ROOT).equals("VIDEO_TS"))) {
			return new DVDISOFile(renderer, file);
		} else if (lcFilename.endsWith(".m3u") ||
				lcFilename.endsWith(".m3u8") ||
				lcFilename.endsWith(".pls") ||
				lcFilename.endsWith(".cue") ||
				lcFilename.endsWith(".ups")) {
			StoreContainer d = PlaylistManager.getPlaylist(renderer, file.getName(), file.getAbsolutePath(), 0);
			if (d == null) {
				LOGGER.trace("createResourceFromFile return null as {} is PlaylistFolder fail.", file.toString());
			}
			return d;
		} else {
			List<String> ignoredFolderNames = renderer.getUmsConfiguration().getIgnoredFolderNames();

			/* Optionally ignore empty directories */
			if (file.isDirectory() && renderer.getUmsConfiguration().isHideEmptyFolders() && !FileUtil.isFolderRelevant(file, renderer.getUmsConfiguration())) {
				LOGGER.debug("Ignoring empty/non-relevant directory: " + file.toString());
				return null;
			} else if (file.isDirectory() && !"".equals(lcFilename) && !ignoredFolderNames.isEmpty() && ignoredFolderNames.contains(file.getName())) {
				LOGGER.debug("Ignoring {} because it is in the ignored folders list", file.toString());
				return null;
			} else {
				// Otherwise add the file
				if (file.isDirectory()) {
					return new RealFolder(renderer, file);
				} else {
					RealFile rf = new RealFile(renderer, file);
					if (rf.length() == 0) {
						LOGGER.debug("Ignoring {} because it seems corrupted when the length of the file is 0", file.toString());
						return null;
					}
					return rf;
				}
			}
		}
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

	public static void waitWorkers() throws InterruptedException {
		while (WORKERS.get() > 0) {
			Thread.sleep(100);
		}
	}

}
