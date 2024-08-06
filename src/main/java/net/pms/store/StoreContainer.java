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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import net.pms.PMS;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.encoders.TranscodingSettings;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import net.pms.store.container.CodeEnter;
import net.pms.store.container.FileTranscodeVirtualFolder;
import net.pms.store.container.LocalizedStoreContainer;
import net.pms.store.container.TranscodeVirtualFolder;
import net.pms.store.item.VirtualVideoAction;
import net.pms.store.item.VirtualVideoActionLocalized;
import net.pms.store.utils.StoreResourceSorter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a container (folder). This is widely used by the UPNP
 * ContentBrowser service. Child objects are expected in this folder.
 */
public class StoreContainer extends StoreResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResource.class);
	private static final int DEPTH_WARNING_LIMIT = 7;

	protected String name;
	protected String thumbnailIcon;
	private boolean isChildrenSorted = false;

	private boolean allChildrenAreContainers = true;
	private boolean discovered = false;

	/**
	 * List of children objects associated with this StoreResource.
	 *
	 * This is only valid when the StoreResource is of the container type.
	 */
	private final List<StoreResource> children = new ArrayList<>();

	/**
	 * The numerical ID (1-based index) assigned to the last child of this
	 * folder. The next child is assigned this ID + 1.
	 */
	private int lastChildId = 0;
	private StoreContainer dynamicPls;

	/**
	 * Constructor for this class. The constructor does not add any child to the
	 * container. This is the only chance to set the name of this container.
	 *
	 * @param name String to be shown in the ContentBrowser service
	 * @param thumbnailIcon Represents a thumbnail to be shown. The String
	 * represents an absolute path. Use null if none is available or desired.
	 * @see #addChild(LibraryResource)
	 */
	public StoreContainer(Renderer renderer, String name, String thumbnailIcon) {
		super(renderer);
		this.name = name;
		this.thumbnailIcon = thumbnailIcon;
	}

	/**
	 * Adds a new LibraryResource to the child list. Only useful if this object
	 * is of the container type.
	 *
	 * @param child LibraryResource to add to a container type.
	 */
	public void addChild(StoreResource child) {
		addChild(child, true, true);
	}

	public void addChild(StoreResource child, boolean isNew, boolean isAddGlobally) {
		// child may be null (spotted - via mediaStore.addChild() - in a
		// misbehaving plugin
		if (child == null) {
			LOGGER.error("A plugin has attempted to add a null child to \"{}\"", getName());
			LOGGER.debug("Error info:", new NullPointerException("Invalid store resource"));
			return;
		}

		child.setParent(this);
		if (isAddGlobally && renderer.getUmsConfiguration().useCode() && !PMS.get().masterCodeValid()) {
			String code = PMS.get().codeDb().getCode(child);
			if (StringUtils.isNotEmpty(code)) {
				StoreResource cobj = child.isCoded();
				if (cobj == null || !((CodeEnter) cobj).getCode().equals(code)) {
					LOGGER.debug("Resource {} is coded add code folder", child);
					CodeEnter ce = new CodeEnter(child);
					ce.setParent(this);
					ce.setCode(code);
					addChildInternal(ce);
					return;
				}
			}
		}

		if (child instanceof StoreItem storeItem) {
			addChildItem(storeItem, isNew, isAddGlobally);
		} else if (child instanceof StoreContainer storeContainer) {
			addChildContainer(storeContainer, isNew, isAddGlobally);
		}

	}

	private void addChildItem(StoreItem item, boolean isNew, boolean isAddGlobally) {
		try {
			if (item.isValid()) {
				if (isAddGlobally && item.getFormat() != null) {
					// Do not add unsupported mediaInfo formats to the list
					if (renderer != null && !renderer.supportsFormat(item.getFormat())) {
						LOGGER.trace("Ignoring file \"{}\" because it is not supported by renderer \"{}\"", item.getName(),
								renderer.getRendererName());
						children.remove(item);
						return;
					}

					// Hide watched videos depending user preference
					if (item.isHideFullyPlayed()) {
						LOGGER.trace("Ignoring video file \"{}\" because it has been watched", item.getName());
						return;
					}
				}

				LOGGER.trace("{} child \"{}\" with class \"{}\"", isNew ? "Adding new" : "Updating", item.getName(),
						item.getClass().getSimpleName());

				if (allChildrenAreContainers && !item.isFolder()) {
					allChildrenAreContainers = false;
				}

				item.setResumeHash(Math.abs(item.getSystemName().hashCode() + hashCode()));

				StoreItem resumeRes = null;

				ResumeObj resumeObject = ResumeObj.create(item);
				if (isAddGlobally &&
						resumeObject != null &&
						!renderer.disableUmsResume() &&
						!renderer.isSamsung()) {
					resumeRes = item.clone();
					resumeRes.setResume(resumeObject);
					resumeRes.setResumeHash(item.resumeHash());
				}

				if (isAddGlobally && item.getFormat() != null) {
					// Determine transcoding possibilities if either
					// - the format is known to be transcodable
					// - we have mediaInfo info (via parserV2, playback info, or a
					// plugin)
					if (item.getFormat().transcodable() || item.getMediaInfo() != null) {
						if (item.getMediaInfo() == null) {
							item.setMediaInfo(new MediaInfo());
						}

						// Try to determine a engine to use for transcoding.
						TranscodingSettings transcodingSettings = null;

						// First, try to match an engine from recently played
						// folder or based on the name of the LibraryResource
						// or its parent. If the name ends in "[unique engine
						// id]", that engine is preferred.
						String currentName = getName();

						if (renderer.getUmsConfiguration().isShowRecentlyPlayedFolder()) {
							transcodingSettings = item.getTranscodingSettings();
						} else {
							for (TranscodingSettings tSettings : TranscodingSettings.getTranscodingsSettings(item)) {
								String end = "[" + tSettings.getEngine().getEngineId().toString() + "]";

								if (currentName.endsWith(end)) {
									truncateDisplayName(end);
									transcodingSettings = tSettings;
									LOGGER.trace("Selecting engine based on name end");
									break;
								} else if (getParent() != null && getParent().getName().endsWith(end)) {
									getParent().truncateDisplayName(end);
									transcodingSettings = tSettings;
									LOGGER.trace("Selecting engine based on parent name end");
									break;
								}
							}
						}

						// If no preferred engine could be determined from the name,
						// try to match a engine based on mediaInfo information and format.
						if (transcodingSettings == null) {
							transcodingSettings = item.resolveTranscodingSettings();
						}
						item.setTranscodingSettings(transcodingSettings);

						if (resumeRes != null) {
							resumeRes.setTranscodingSettings(transcodingSettings);
							resumeRes.setMediaSubtitle(item.getMediaSubtitle());
						}

						if (!allChildrenAreContainers) {
							// Should the item be added to the #--TRANSCODE--# folder?
							if ((item.getFormat().isVideo() || item.getFormat().isAudio()) && item.isTranscodeFolderAvailable()) {
								TranscodeVirtualFolder transcodeFolder = getTranscodeFolder();
								if (transcodeFolder != null) {
									FileTranscodeVirtualFolder fileTranscodeFolder = new FileTranscodeVirtualFolder(renderer, item);

									LOGGER.trace("Adding \"{}\" to transcode folder for engine: \"{}\"", item.getName(),
											transcodingSettings);
									transcodeFolder.addChildInternal(fileTranscodeFolder);
								}
							}

							if (renderer.getUmsConfiguration().isDynamicPls() && !item.isFolder() && renderer != null &&
									!renderer.isNoDynPlsFolder()) {
								addDynamicPls(item);
							}
						} else if (!item.getFormat().isCompatible(item, renderer) && !item.isFolder()) {
							LOGGER.trace("Ignoring file \"{}\" because it is not compatible with renderer \"{}\"", item.getName(),
									renderer.getRendererName());
							children.remove(item);
							return;
						}
					}

					if (resumeRes != null && resumeRes.getMediaInfo() != null) {
						resumeRes.getMediaInfo().setThumbnailId(null);
						resumeRes.getMediaInfo().setThumbnailSource(ThumbnailSource.UNKNOWN);
						resumeRes.getMediaInfo().setMimeType(HTTPResource.VIDEO_TRANSCODE);
					}

					/**
					 * Secondary format is currently only used to provide 24-bit
					 * FLAC to PS3 by sending it as a fake video. This can be
					 * made more reusable with a renderer config setting like
					 * Mux24BitFlacToVideo if we ever have another purpose for
					 * it, which I doubt we will have.
					 */
					if (item.getFormat().getSecondaryFormat() != null && item.getMediaInfo() != null && renderer != null &&
							renderer.supportsFormat(item.getFormat().getSecondaryFormat()) && renderer.isPS3()) {
						StoreItem newChild = item.clone();
						newChild.setFormat(newChild.getFormat().getSecondaryFormat());
						LOGGER.trace("Detected secondary format \"{}\" for \"{}\"", newChild.getFormat().toString(), newChild.getName());
						newChild.setPrimaryResource(item);
						item.setSecondaryResource(newChild);

						if (!newChild.getFormat().isCompatible(newChild, renderer)) {
							TranscodingSettings transcodingSettings = TranscodingSettings.getBestTranscodingSettings(newChild);
							newChild.setTranscodingSettings(transcodingSettings);
							LOGGER.trace("Secondary format \"{}\" will use engine \"{}\" for \"{}\"", newChild.getFormat().toString(),
									transcodingSettings == null ? "null" : transcodingSettings.toString(), newChild.getName());
						}

						if (item.getMediaInfo() != null && item.getMediaInfo().isSecondaryFormatValid()) {
							addChild(newChild);
							LOGGER.trace("Adding secondary format \"{}\" for \"{}\"", newChild.getFormat().toString(), newChild.getName());
						} else {
							LOGGER.trace("Ignoring secondary format \"{}\" for \"{}\": invalid format", newChild.getFormat().toString(),
									newChild.getName());
						}
					}
				}

				if (isNew) {
					addChildInternal(item, isAddGlobally);
				} else {
					LOGGER.trace("Details on media being imported :" + item);
				}

				if (resumeRes != null) {
					addChildInternal(resumeRes);
				}
			}
		} catch (Throwable t) {
			LOGGER.debug("Error adding child {}: {}", item.getName(), t);

			item.setParent(null);
			children.remove(item);
		}
	}

	private void addChildContainer(StoreContainer container, boolean isNew, boolean isAddGlobally) {
		try {
			if (container.isValid()) {
				LOGGER.trace("{} child \"{}\" with class \"{}\"", isNew ? "Adding new" : "Updating", container.getName(),
						container.getClass().getSimpleName());

				if (isNew) {
					addChildInternal(container, isAddGlobally);
				}

			}
		} catch (Throwable t) {
			LOGGER.debug("Error adding child {}: {}", container.getName(), t);

			container.setParent(null);
			children.remove(container);
		}
	}

	public boolean allowScan() {
		return false;
	}

	/**
	 * Returns the list of children for this resource.
	 *
	 * @return List of children objects.
	 */
	public List<StoreResource> getChildren() {
		return children;
	}

	/**
	 * @return Number of children objects. This might be used in the DLDI
	 * response, as some renderers might not have enough memory to hold the list
	 * for all children.
	 */
	public int childrenCount() {
		return children.size();
	}

	/**
	 * Clear all resources in children.
	 */
	public void clearChildren() {
		Iterator<StoreResource> resources = children.iterator();
		while (resources.hasNext()) {
			StoreResource resource = resources.next();
			if (resource instanceof StoreContainer container) {
				container.clearChildren();
			}
			resources.remove();
			renderer.getMediaStore().deleteWeakResource(resource);
		}
	}

	/**
	 * Returns the numerical ID of the last child added.
	 *
	 * @return The ID.
	 * @since 1.80.0
	 */
	protected int getLastChildId() {
		return lastChildId;
	}

	/**
	 * Sets the numerical ID of the last child added.
	 *
	 * @param lastChildId The ID to set.
	 * @since 1.80.0
	 */
	protected void setLastChildId(int lastChildId) {
		this.lastChildId = lastChildId;
	}

	/**
	 * (Re)sets the given LibraryResource as follows: - if it's already one of
	 * our children, renew it - or if we have another child with the same name,
	 * replace it - otherwise add it as a new child.
	 *
	 * @param child the LibraryResource to update
	 */
	public void updateChild(StoreResource child) {
		StoreResource found = children.contains(child) ? child : searchByName(child.getName());
		if (found != null) {
			if (child != found) {
				// Replace
				child.setParent(this);
				renderer.getMediaStore().replaceWeakResource(found, child);
				children.set(children.indexOf(found), child);
			}
			// Renew
			addChild(child, false, true);
		} else {
			// Not found, it's new
			addChild(child, true, true);
		}
	}

	public void removeChild(StoreResource child) {
		if (children.contains(child)) {
			if (child instanceof StoreContainer storeContainer) {
				storeContainer.clearChildren();
			}
			children.remove(child);
		}
		renderer.getMediaStore().deleteWeakResource(child);
	}

	/**
	 * Adds the supplied MediaResource in the internal list of child nodes, and
	 * sets the parent to the current node. Avoids the side-effects associated
	 * with the {@link #addChild(MediaResource)} method.
	 *
	 * @param child the LibraryResource to add to this node's list of children
	 */
	protected synchronized void addChildInternal(StoreResource child) {
		addChildInternal(child, true);
	}

	/**
	 * Adds the supplied MediaResource in the internal list of child nodes, and
	 * sets the parent to the current node. Avoids the side-effects associated
	 * with the {@link #addChild(MediaResource)} method.
	 *
	 * @param child the LibraryResource to add to this node's list of children
	 * @param isAddGlobally whether to store a reference to this child in the
	 * global ID repository.
	 */
	protected synchronized void addChildInternal(StoreResource child, boolean isAddGlobally) {
		if (child.getId() != null) {
			LOGGER.debug("Node ({}) already has an ID ({}), which is overridden now. The previous parent node was: {}",
					new Object[]{child.getClass().getName(), child.getResourceId(), child.getParent()});
		}

		children.add(child);
		child.setParent(this);

		if (isAddGlobally && this.getLongId() != null) {
			//if this is not yet linked to parent, child will be added when it will.
			addGlobalRepo(child);
		}
	}

	/**
	 * GlobalRepo will set the id from db.
	 */
	private void addGlobalRepo(StoreResource resource) {
		renderer.getMediaStore().addWeakResource(resource);
		if (resource instanceof StoreContainer container) {
			for (StoreResource child : container.children) {
				addGlobalRepo(child);
			}
		}
	}

	protected void sortChildrenIfNeeded() {
		if (isChildrenSorted()) {
			StoreResourceSorter.sortResourcesByDefault(children);
		}
	}

	protected void refreshChildrenIfNeeded() {
		if (isDiscovered() && isRefreshNeeded()) {
			refreshChildren();
			notifyRefresh();
		}
	}

	private void addDynamicPls(final StoreResource child) {
		final StoreResource dynPls = renderer.getMediaStore().getDynamicPls();
		if (dynPls == child || child.getParent() == dynPls) {
			return;
		}

		if (child instanceof VirtualVideoAction) {
			// ignore these
			return;
		}

		if (dynamicPls == null) {
			dynamicPls = new LocalizedStoreContainer(renderer, "DynamicPlaylist_FolderName");
			addChildInternal(dynamicPls);
			dynamicPls.addChild(dynPls);
		}

		VirtualVideoAction vva = new VirtualVideoActionLocalized(renderer, "AddXToDynamicPlaylist", true, null, child.getDisplayName()) {
			@Override
			public boolean enable() {
				renderer.getMediaStore().getDynamicPls().add(child);
				return true;
			}
		};
		vva.setParent(this);
		dynamicPls.addChildInternal(vva);
	}

	/**
	 * Returns a string representing the container. This string is used in the
	 * UPNP ContentBrowser service.
	 *
	 * @see net.pms.store.StoreResource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @since 1.50
	 */
	protected void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns true in this case, as this is a folder.
	 *
	 * @return true
	 * @see net.pms.store.StoreResource#isFolder()
	 */
	@Override
	public boolean isFolder() {
		return true;
	}

	/**
	 * Returns zero as this is a folder (container).
	 *
	 * @return 0
	 * @see net.pms.store.StoreResource#length()
	 */
	@Override
	public long length() {
		return 0;
	}

	@Override
	public String getSystemName() {
		return getName();
	}

	/**
	 * Returns a {@link InputStream} that represents the thumbnail used.
	 *
	 * @throws IOException
	 *
	 * @see StoreResource#getThumbnailInputStream()
	 */
	@Override
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (StringUtils.isEmpty(thumbnailIcon)) {
			try {
				return super.getThumbnailInputStream();
			} catch (IOException e) {
				return null;
			}
		}
		return DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.getResourceInputStream(thumbnailIcon));
	}

	/**
	 * Returns true, as a container is always a valid item to add to another
	 * container.
	 *
	 * @see net.pms.store.StoreResource#isValid()
	 */
	@Override
	public boolean isValid() {
		return true;
	}

	public void setThumbnail(String thumbnailIcon) {
		this.thumbnailIcon = thumbnailIcon;
	}

	/**
	 * Returns the transcode folder for this resource. If UMS is configured to
	 * hide transcode folders, null is returned. If no folder exists, a new
	 * transcode folder is created. If transcode folder exists, it is returned.
	 * This method is called on the parent folder each time a child is added to
	 * that parent (via {@link addChild(LibraryResource)}.
	 *
	 * @return the transcode virtual folder
	 */
	// XXX package-private: used by VirtualFile; should be protected?
	private TranscodeVirtualFolder getTranscodeFolder() {
		if (!isTranscodeFolderAvailable()) {
			return null;
		}

		if (!renderer.getUmsConfiguration().isShowTranscodeFolder()) {
			return null;
		}

		// search for transcode folder
		for (StoreResource child : children) {
			if (child instanceof TranscodeVirtualFolder transcodeVirtualFolder) {
				return transcodeVirtualFolder;
			}
		}

		TranscodeVirtualFolder transcodeFolder = new TranscodeVirtualFolder(renderer, renderer.getUmsConfiguration());
		addChildInternal(transcodeFolder);
		return transcodeFolder;
	}

	/**
	 * Create a path of virtual folders if it doesn't already exist.
	 *
	 * @param parentPath the full virtual folder path (slash-delimited).
	 */
	protected StoreContainer getSharedContentParent(String parentPath) {
		StoreContainer result = null;
		if (parentPath != null) {
			StringTokenizer st = new StringTokenizer(parentPath, "/");
			StoreContainer currentRoot = this;
			while (st.hasMoreTokens()) {
				String folder = st.nextToken();
				result = currentRoot.searchByName(folder);
				if (result == null) {
					result = new StoreContainer(renderer, folder, "");
					currentRoot.addChild(result);
				}
				currentRoot = result;
			}
		}
		if (result == null) {
			result = this;
		}
		return result;
	}

	/**
	 * Recursive function that searches through all of the children until it
	 * finds a {@link StoreResource} that matches the name.
	 * <p>
	 * Only used by {@link net.pms.sore.MediaStore#addWebFolder(File webConf)
	 * addWebFolder(File webConf)} while parsing the web.conf file.
	 *
	 * @param name String to be compared the name to.
	 * @return Returns a {@link StoreResource} whose name matches the parameter
	 * name
	 * @see #getName()
	 */
	public StoreContainer searchByName(String name) {
		for (StoreResource child : children) {
			if (child instanceof StoreContainer virtualFolder && child.getName().equals(name)) {
				return virtualFolder;
			}
		}

		return null;
	}

	// Returns the index of the given child resource id, or -1 if not found
	public int indexOf(String objectId) {
		// Use the index id string only, omitting any trailing filename
		String resourceId = StringUtils.substringBefore(objectId, "/");
		if (resourceId != null) {
			for (int i = 0; i < children.size(); i++) {
				if (resourceId.equals(children.get(i).getResourceId())) {
					return i;
				}
			}
		}

		return -1;
	}

	/**
	 * Discover the list of children.
	 */
	public void discoverChildren() {
		sortChildrenIfNeeded();
	}

	public void discoverChildren(String str) {
		discoverChildren();
	}

	protected final synchronized void discover(boolean forced) {
		// Discover children if it hasn't been done already
		if (!isDiscovered()) {
			LOGGER.trace("Initial discovering children for container: {}", getDisplayName());
			if (renderer.getUmsConfiguration().getFolderLimit() && depthLimit()) {
				if (renderer.isPS3() || renderer.isXbox360()) {
					LOGGER.info("Depth limit potentionally hit for " + getDisplayName());
				}

				renderer.addFolderLimit(this);
			}

			discoverChildren();
			setDiscovered(true);
			notifyRefresh();
		} else {
			// if forced, then call the old 'refreshChildren' method
			LOGGER.trace("discover {} refresh forced: {}", getResourceId(), forced);
			/*
			 * if (forced && shouldRefresh(searchStr)) {
			 * doRefreshChildren(searchStr); notifyRefresh(); }
			 */
			if (forced) {
				// This seems to follow the same code path as the else below in
				// the case of VirtualFile, because
				// refreshChildren calls shouldRefresh -> isRefreshNeeded ->
				// doRefreshChildren, which is what happens below
				// (refreshChildren is not overridden in VirtualFile)
				if (refreshChildren()) {
					notifyRefresh();
				} else {
					sortChildrenIfNeeded();
				}
			} else {
				// if not, then the regular isRefreshNeeded/doRefreshChildren
				// pair.
				if (isRefreshNeeded()) {
					doRefreshChildren();
					notifyRefresh();
				} else {
					sortChildrenIfNeeded();
				}
			}
		}
	}

	public void doRefreshChildren() {
		sortChildrenIfNeeded();
	}

	public void doRefreshChildren(String search, String lang) {
		doRefreshChildren();
	}

	private boolean depthLimit() {
		StoreContainer tmp = this;
		int depth = 0;
		while (tmp != null) {
			tmp = tmp.getParent();
			depth++;
		}

		return (depth > DEPTH_WARNING_LIMIT);
	}

	/**
	 * @return true, if the container is changed, so refresh is needed. This
	 * could be called a lot of times.
	 */
	public boolean isRefreshNeeded() {
		return false;
	}

	/**
	 * This method gets called only for the browsed folder, and not for the
	 * parent folders.
	 *
	 * (And in the media scan step too).
	 *
	 * @return True when a refresh is needed, false otherwise.
	 */
	public boolean refreshChildren() {
		if (isRefreshNeeded()) {
			doRefreshChildren();
			return true;
		}

		return false;
	}

	public boolean refreshChildren(String search, String lang) {
		if (isRefreshNeeded()) {
			doRefreshChildren(search, lang);
			return true;
		}

		return false;
	}

	/**
	 * Returns true when the details of this resource have already been
	 * investigated. This helps is not doing the same work twice.
	 *
	 * @return True if discovered, false otherwise.
	 */
	public boolean isDiscovered() {
		return discovered;
	}

	/**
	 * Set to true when the details of this resource have already been
	 * investigated. This helps is not doing the same work twice.
	 *
	 * @param discovered Set to true if this resource is discovered, false
	 * otherwise.
	 * @since 1.50
	 */
	public void setDiscovered(boolean discovered) {
		this.discovered = discovered;
	}

	protected void setChildrenSorted(boolean isChildrenSorted) {
		this.isChildrenSorted = isChildrenSorted;
	}

	public boolean isChildrenSorted() {
		return isChildrenSorted;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [id=").append(getId());
		result.append(", name=").append(getName());
		result.append(", discovered=").append(isDiscovered());
		if (getChildren() != null && !getChildren().isEmpty()) {
			result.append(", children=").append(getChildren());
		}
		result.append(']');
		return result.toString();
	}

}
