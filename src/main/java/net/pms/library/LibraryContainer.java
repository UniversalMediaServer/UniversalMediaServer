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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.encoders.Engine;
import net.pms.encoders.EngineFactory;
import net.pms.library.container.CodeEnter;
import net.pms.library.container.FileTranscodeVirtualFolder;
import net.pms.library.container.OpenSubtitleFolder;
import net.pms.library.container.SubSelect;
import net.pms.library.container.TranscodeVirtualFolder;
import net.pms.library.container.VirtualFolder;
import net.pms.library.item.VirtualVideoAction;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.renderers.Renderer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a container (folder). This is widely used by the UPNP
 * ContentBrowser service. Child objects are expected in this folder.
 */
public class LibraryContainer extends LibraryResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryResource.class);
	private static final int DEPTH_WARNING_LIMIT = 7;

	protected String name;
	protected String thumbnailIcon;
	private boolean allChildrenAreContainers = true;
	private boolean discovered = false;

	/**
	 * List of children objects associated with this LibraryResource. This is
	 * only valid when the LibraryResource is of the container type.
	 */
	private final List<LibraryResource> children = new ArrayList<>();

	/**
	 * The numerical ID (1-based index) assigned to the last child of this
	 * folder. The next child is assigned this ID + 1.
	 */
	private int lastChildId = 0;
	private LibraryContainer dynamicPls;

	/**
	 * Constructor for this class. The constructor does not add any child to the
	 * container. This is the only chance to set the name of this container.
	 *
	 * @param name String to be shown in the ContentBrowser service
	 * @param thumbnailIcon Represents a thumbnail to be shown. The String
	 * represents an absolute path. Use null if none is available or desired.
	 * @see #addChild(LibraryResource)
	 */
	public LibraryContainer(Renderer renderer, String name, String thumbnailIcon) {
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
	public void addChild(LibraryResource child) {
		addChild(child, true, true);
	}

	public void addChild(LibraryResource child, boolean isNew, boolean isAddGlobally) {
		// child may be null (spotted - via rootFolder.addChild() - in a
		// misbehaving plugin
		if (child == null) {
			LOGGER.error("A plugin has attempted to add a null child to \"{}\"", getName());
			LOGGER.debug("Error info:", new NullPointerException("Invalid library resource"));
			return;
		}

		child.setParent(this);
		if (isAddGlobally && renderer.getUmsConfiguration().useCode() && !PMS.get().masterCodeValid()) {
			String code = PMS.get().codeDb().getCode(child);
			if (StringUtils.isNotEmpty(code)) {
				LibraryResource cobj = child.isCoded();
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

		if (child instanceof LibraryItem libraryItem) {
			addChildItem(libraryItem, isNew, isAddGlobally);
		} else if (child instanceof LibraryContainer libraryContainer) {
			addChildContainer(libraryContainer, isNew, isAddGlobally);
		}

	}

	private void addChildItem(LibraryItem item, boolean isNew, boolean isAddGlobally) {
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

				LibraryItem resumeRes = null;

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
						Engine transcodingEngine = null;

						// First, try to match an engine from recently played
						// folder or based on the name of the LibraryResource
						// or its parent. If the name ends in "[unique engine
						// id]", that engine is preferred.
						String currentName = getName();

						if (renderer.getUmsConfiguration().isShowRecentlyPlayedFolder()) {
							transcodingEngine = item.getEngine();
						} else {
							for (Engine tEngine : EngineFactory.getEngines()) {
								String end = "[" + tEngine.getEngineId().toString() + "]";

								if (currentName.endsWith(end)) {
									truncateDisplayName(end);
									transcodingEngine = tEngine;
									LOGGER.trace("Selecting engine based on name end");
									break;
								} else if (getParent() != null && getParent().getName().endsWith(end)) {
									getParent().truncateDisplayName(end);
									transcodingEngine = tEngine;
									LOGGER.trace("Selecting engine based on parent name end");
									break;
								}
							}
						}

						// If no preferred engine could be determined from the name,
						// try to match a engine based on mediaInfo information and format.
						if (transcodingEngine == null) {
							transcodingEngine = item.resolveEngine();
						}
						item.setEngine(transcodingEngine);

						if (resumeRes != null) {
							resumeRes.setEngine(transcodingEngine);
							resumeRes.setMediaSubtitle(item.getMediaSubtitle());
						}

						if (!allChildrenAreContainers) {
							// Should the item be added to the #--TRANSCODE--# folder?
							if ((item.getFormat().isVideo() || item.getFormat().isAudio()) && item.isTranscodeFolderAvailable()) {
								TranscodeVirtualFolder transcodeFolder = getTranscodeFolder();
								if (transcodeFolder != null) {
									FileTranscodeVirtualFolder fileTranscodeFolder = new FileTranscodeVirtualFolder(renderer, item);

									LOGGER.trace("Adding \"{}\" to transcode folder for engine: \"{}\"", item.getName(),
											transcodingEngine);
									transcodeFolder.addChildInternal(fileTranscodeFolder);
								}
							}

							if (item.getFormat().isVideo() && item.isSubSelectable() && !(this instanceof OpenSubtitleFolder)) {
								LibraryContainer vf = getSubSelector(true);
								if (vf != null) {
									LibraryItem newChild = item.clone();
									newChild.setEngine(transcodingEngine);
									newChild.setMediaInfo(item.getMediaInfo());
									LOGGER.trace("Adding live subtitles folder for \"{}\" with engine {}", item.getName(),
											transcodingEngine);

									vf.addChild(new OpenSubtitleFolder(renderer, newChild));
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
						}
					}

					if (resumeRes != null && resumeRes.getMediaInfo() != null) {
						resumeRes.getMediaInfo().setThumbready(false);
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
						LibraryItem newChild = item.clone();
						newChild.setFormat(newChild.getFormat().getSecondaryFormat());
						LOGGER.trace("Detected secondary format \"{}\" for \"{}\"", newChild.getFormat().toString(), newChild.getName());
						newChild.setPrimaryResource(item);
						item.setSecondaryResource(newChild);

						if (!newChild.getFormat().isCompatible(newChild, renderer)) {
							Engine transcodingEngine = EngineFactory.getEngine(newChild);
							newChild.setEngine(transcodingEngine);
							LOGGER.trace("Secondary format \"{}\" will use engine \"{}\" for \"{}\"", newChild.getFormat().toString(),
									transcodingEngine == null ? "null" : transcodingEngine.getName(), newChild.getName());
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

	private void addChildContainer(LibraryContainer container, boolean isNew, boolean isAddGlobally) {
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
	public List<LibraryResource> getChildren() {
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
		Iterator<LibraryResource> resources = children.iterator();
		while (resources.hasNext()) {
			LibraryResource resource = resources.next();
			if (resource instanceof LibraryContainer container) {
				container.clearChildren();
			}
			resources.remove();
			renderer.getRootFolder().deleteWeakResource(resource);
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
	public void updateChild(LibraryResource child) {
		LibraryResource found = children.contains(child) ? child : searchByName(child.getName());
		if (found != null) {
			if (child != found) {
				// Replace
				child.setParent(this);
				renderer.getRootFolder().replaceWeakResource(found, child);
				children.set(children.indexOf(found), child);
			}
			// Renew
			addChild(child, false, true);
		} else {
			// Not found, it's new
			addChild(child, true, true);
		}
	}

	public void removeChild(LibraryResource child) {
		if (children.contains(child)) {
			children.remove(child);
		}
		renderer.getRootFolder().deleteWeakResource(child);
	}

	/**
	 * Adds the supplied MediaResource in the internal list of child nodes, and
	 * sets the parent to the current node. Avoids the side-effects associated
	 * with the {@link #addChild(MediaResource)} method.
	 *
	 * @param child the LibraryResource to add to this node's list of children
	 */
	protected synchronized void addChildInternal(LibraryResource child) {
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
	protected synchronized void addChildInternal(LibraryResource child, boolean isAddGlobally) {
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
	private void addGlobalRepo(LibraryResource resource) {
		renderer.getRootFolder().addWeakResource(resource);
		if (resource instanceof LibraryContainer container) {
			for (LibraryResource child : container.children) {
				addGlobalRepo(child);
			}
		}
	}

	protected void refreshChildrenIfNeeded(String search) {
		if (isDiscovered() && isRefreshNeeded()) {
			refreshChildren(search);
			notifyRefresh();
		}
	}

	private void addDynamicPls(final LibraryResource child) {
		final LibraryResource dynPls = renderer.getRootFolder().getDynamicPls();
		if (dynPls == child || child.getParent() == dynPls) {
			return;
		}

		if (child instanceof VirtualVideoAction) {
			// ignore these
			return;
		}

		if (dynamicPls == null) {
			dynamicPls = new LibraryContainer(renderer, Messages.getString("DynamicPlaylist_FolderName"), null);
			addChildInternal(dynamicPls);
			dynamicPls.addChild(dynPls);
		}

		String str = Messages.getString("Add") + " " + child.getDisplayName() + " " + Messages.getString("ToDynamicPlaylist");
		VirtualVideoAction vva = new VirtualVideoAction(renderer, str, true, null) {
			@Override
			public boolean enable() {
				renderer.getRootFolder().getDynamicPls().add(child);
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
	 * @see net.pms.library.LibraryResource#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns true in this case, as this is a folder.
	 *
	 * @return true
	 * @see net.pms.library.LibraryResource#isFolder()
	 */
	@Override
	public boolean isFolder() {
		return true;
	}

	/**
	 * Returns zero as this is a folder (container).
	 *
	 * @return 0
	 * @see net.pms.library.LibraryResource#length()
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
	 * @see LibraryResource#getThumbnailInputStream()
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
	 * @see net.pms.library.LibraryResource#isValid()
	 */
	@Override
	public boolean isValid() {
		return true;
	}

	public void setThumbnail(String thumbnailIcon) {
		this.thumbnailIcon = thumbnailIcon;
	}

	////////////////////////////////////////////////////
	// Subtitle handling
	////////////////////////////////////////////////////
	private SubSelect getSubSelector(boolean create) {
		if (renderer.getUmsConfiguration().isDisableSubtitles() || !renderer.getUmsConfiguration().isAutoloadExternalSubtitles() ||
				!renderer.getUmsConfiguration().isShowLiveSubtitlesFolder() || !isLiveSubtitleFolderAvailable()) {
			return null;
		}

		// Search for transcode folder
		for (LibraryResource r : children) {
			if (r instanceof SubSelect subSelect) {
				return subSelect;
			}
		}

		if (create) {
			SubSelect vf = new SubSelect(renderer);
			addChildInternal(vf);
			return vf;
		}

		return null;
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
		for (LibraryResource child : children) {
			if (child instanceof TranscodeVirtualFolder transcodeVirtualFolder) {
				return transcodeVirtualFolder;
			}
		}

		TranscodeVirtualFolder transcodeFolder = new TranscodeVirtualFolder(renderer, null, renderer.getUmsConfiguration());
		addChildInternal(transcodeFolder);
		return transcodeFolder;
	}

	/**
	 * Create a path of virtual folders if it doesn't already exist.
	 *
	 * @param parentPath the full virtual folder path (slash-delimited).
	 */
	protected LibraryContainer getSharedContentParent(String parentPath) {
		LibraryContainer result = null;
		if (parentPath != null) {
			StringTokenizer st = new StringTokenizer(parentPath, "/");
			LibraryContainer currentRoot = this;
			while (st.hasMoreTokens()) {
				String folder = st.nextToken();
				result = currentRoot.searchByName(folder);
				if (result == null) {
					result = new LibraryContainer(renderer, folder, "");
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
	 * finds a {@link LibraryResource} that matches the name.
	 * <p>
	 * Only used by {@link net.pms.library.RootFolder#addWebFolder(File webConf)
	 * addWebFolder(File webConf)} while parsing the web.conf file.
	 *
	 * @param name String to be compared the name to.
	 * @return Returns a {@link LibraryResource} whose name matches the
	 * parameter name
	 * @see #getName()
	 */
	public LibraryContainer searchByName(String name) {
		for (LibraryResource child : children) {
			if (child instanceof LibraryContainer virtualFolder && child.getName().equals(name)) {
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
	}

	public void discoverChildren(String str) {
		discoverChildren();
	}

	protected final void discover(int count, boolean forced, String searchStr) {
		// Discover children if it hasn't been done already
		if (!isDiscovered()) {
			if (renderer.getUmsConfiguration().getFolderLimit() && depthLimit()) {
				if (renderer.isPS3() || renderer.isXbox360()) {
					LOGGER.info("Depth limit potentionally hit for " + getDisplayName());
				}

				renderer.addFolderLimit(this);
			}

			discoverChildren(searchStr);
			boolean ready;

			if (this instanceof VirtualFolder virtualFolder) {
				if (renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
					ready = virtualFolder.analyzeChildren(count);
				} else {
					ready = virtualFolder.analyzeChildren(-1);
				}
			} else {
				ready = true;
			}

			if (!renderer.isUseMediaInfo() || ready) {
				setDiscovered(true);
			}

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
				if (refreshChildren(searchStr)) {
					notifyRefresh();
				}
			} else {
				// if not, then the regular isRefreshNeeded/doRefreshChildren
				// pair.
				if (isRefreshNeeded()) {
					doRefreshChildren(searchStr);
					notifyRefresh();
				}
			}
		}
	}

	public void doRefreshChildren() {
	}

	public void doRefreshChildren(String search) {
		doRefreshChildren();
	}

	private boolean depthLimit() {
		LibraryContainer tmp = this;
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
	 * parent folders. (And in the media library scan step too). Override in
	 * plugins when you do not want to implement proper change tracking, and you
	 * do not care if the hierarchy of nodes getting invalid between.
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

	public boolean refreshChildren(String search) {
		if (isRefreshNeeded()) {
			doRefreshChildren(search);
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