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

import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageInfo;
import net.pms.media.MediaInfo;
import net.pms.media.MediaStatus;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.mediaserver.MediaServerRequest;
import net.pms.renderers.Renderer;
import net.pms.store.container.ChapterFileTranscodeVirtualFolder;
import net.pms.store.container.CodeEnter;
import net.pms.store.container.FileTranscodeVirtualFolder;
import net.pms.util.FullyPlayedAction;
import net.pms.util.GenericIcons;
import net.pms.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents any item that can be browsed via the UPNP ContentDirectory
 * service.
 */
public abstract class StoreResource implements Cloneable, Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(StoreResource.class);
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	protected static final int MAX_ARCHIVE_ENTRY_SIZE = 10000000;
	protected static final int MAX_ARCHIVE_SIZE_SEEK = 800000000;

	/**
	 * Maximum size of a stream, taking into account that some renderers (like
	 * the PS3) will convert this <code>long</code> to <code>int</code>.
	 * Truncating this value will still return the maximum value that an
	 * <code>int</code> can contain.
	 */
	public static final long TRANS_SIZE = Long.MAX_VALUE - Integer.MAX_VALUE - 1;

	public static final RenderingHints THUMBNAIL_HINTS = new RenderingHints(RenderingHints.KEY_RENDERING,
			RenderingHints.VALUE_RENDER_QUALITY);

	private volatile ImageInfo thumbnailImageInfo = null;

	protected final Renderer renderer;

	private boolean resolved;

	private String id;

	private StoreContainer parent;

	/**
	 * The format of this StoreResource.
	 */
	protected MediaInfo mediaInfo;
	private MediaStatus mediaStatus;

	private long lastModified;

	private boolean noName;
	private int nametruncate;
	private StoreResource first;
	private StoreResource second;

	private String fakeParentId;

	/**
	 * The last time refresh was called.
	 */
	private long lastRefreshTime;
	protected boolean isSortable = false;

	protected HashMap<String, Object> attachments = null;

	protected StoreResource(Renderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * Returns parent object, usually a folder type of resource. In the DLDI
	 * queries, the UPNP server needs to give out the parent container where the
	 * item is. The <i>parent</i> represents such a container.
	 *
	 * @return Parent object.
	 */
	public StoreContainer getParent() {
		return parent;
	}

	/**
	 * Set the parent object, usually a folder type of resource. In the DLDI
	 * queries, the UPNP server needs to give out the parent container where the
	 * item is. The <i>parent</i> represents such a container.
	 *
	 * @param parent Sets the parent object.
	 */
	public void setParent(StoreContainer parent) {
		this.parent = parent;
	}

	/**
	 * Returns the id of this resource based on the index in its parent
	 * container. Its main purpose is to be unique in the parent container.
	 *
	 * @return The id string.
	 * @since 1.50
	 */
	public String getId() {
		return id;
	}

	/**
	 * Returns the long representation of the id of this StoreResource
	 * based on the index in its parent container.
	 *
	 * @return The id integer.
	 * @since 6.4.1
	 */
	public Long getLongId() {
		try {
			return Long.valueOf(getId());
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/**
	 * Set the ID of this resource based on the index in its parent container.
	 * Its main purpose is to be unique in the parent container. The method is
	 * automatically called by addChildInternal, so most of the time it is not
	 * necessary to call it explicitly.
	 *
	 * @param id
	 * @since 1.50
	 * @see #addChildInternal(StoreResource)
	 */
	public void setId(String id) {
		this.id = id;
	}

	public String getPathId() {
		StoreResource tmp = getParent();
		ArrayList<String> res = new ArrayList<>();
		res.add(getId());
		while (tmp != null) {
			res.add(0, tmp.getId());
			tmp = tmp.getParent();
		}
		return StringUtils.join(res, '.');
	}

	/**
	 * String representing this resource ID. This string is used by the UPNP
	 * ContentDirectory service. There is no hard spec on the actual numbering
	 * except for the root container that always has to be "0". In UMS the
	 * format used is <i>number($number)+</i>. A common client that expects a
	 * different format than the one used here is the XBox360. UMS translates
	 * the XBox360 queries on the fly. For more info, check
	 * <ul>
	 * <li><a href="http://www.mperfect.net/whsUpnp360/">whsUpnp360</a></li>
	 * <li><a href=
	 * "https://code.google.com/archive/p/jems/wikis/XBox360Notes.wiki">jems -
	 * XBox360Notes.wiki</a></li>
	 * <li><a href=
	 * "https://web-beta.archive.org/web/20100501042404/http://download.microsoft.com:80/download/0/0/b/00bba048-35e6-4e5b-a3dc-36da83cbb0d1/NetCompat_WMP11.docx">NetCompat_WMP11.docx</a></li>
	 * </ul>
	 *
	 * @return The StoreResource id.
	 * @since 1.50
	 */
	public String getResourceId() {
		/*
		 * if (getId() == null) { return null; }
		 *
		 * if (parent != null) { return parent.getResourceId() + '$' + getId();
		 * } else { return getId(); }
		 */
		if (isFolder() && renderer.getUmsConfiguration().getAutoDiscover()) {
			return getPathId();
		}
		return getId();
	}

	/**
	 * @see #setId(String)
	 * @param id
	 */
	protected void setLongId(long id) {
		setId(Long.toString(id));
	}

	/**
	 *
	 * @return true, if this contain can have a transcode folder
	 */
	public boolean isTranscodeFolderAvailable() {
		return true;
	}

	/**
	 * Checks if is live subtitle folder available.
	 *
	 * @return true, if the live subtitle folder should be shown
	 */
	public boolean isLiveSubtitleFolderAvailable() {
		return true;
	}

	/**
	 * Any {@link StoreResource} needs to represent the container or item with
	 * a String.
	 *
	 * @return String to be showed in the UPNP client.
	 */
	public abstract String getName();

	public abstract String getSystemName();

	/**
	 * @return The path to the mediaInfo source.
	 */
	public String getFileName() {
		return getSystemName();
	}

	public abstract long length();

	public abstract boolean isFolder();

	public StoreResource getPrimaryResource() {
		return first;
	}

	public void setPrimaryResource(StoreResource first) {
		this.first = first;
	}

	public StoreResource getSecondaryResource() {
		return second;
	}

	public void setSecondaryResource(StoreResource second) {
		this.second = second;
	}

	public String getFakeParentId() {
		return fakeParentId;
	}

	public void setFakeParentId(String fakeParentId) {
		this.fakeParentId = fakeParentId;
	}

	/**
	 * @return the fake parent id if specified, or the real parent id
	 */
	public String getParentId() {
		if (getFakeParentId() != null) {
			return getFakeParentId();
		}
		if (parent != null) {
			return parent.getResourceId();
		}
		return "-1";
	}

	/**
	 * Update the last refresh time.
	 */
	protected void notifyRefresh() {
		lastRefreshTime = System.currentTimeMillis();
		MediaStoreIds.incrementUpdateId(getLongId());
	}

	@Override
	public void run() {
		try {
			if (first == null) {
				syncResolve();
				if (second != null) {
					second.syncResolve();
				}
			}
		} catch (Exception e) {
			LOGGER.warn("Unhandled exception while resolving {}: {}", getDisplayName(), e.getMessage());
			LOGGER.debug("", e);
		}
	}

	/**
	 * Hook to lazily initialise immutable resources e.g. ISOs, zip files
	 * &amp;c.
	 *
	 * @since 1.90.0
	 * @see #syncResolve()
	 */
	protected void resolveOnce() {
	}

	/**
	 * Resolve events are hooks that allow store resources to perform various
	 * forms of initialisation when navigated to or streamed i.e. they function
	 * as lazy constructors.
	 *
	 * This method is called by request handlers for a) requests for a stream or
	 * b) content directory browsing i.e. for potentially every request for a
	 * file or folder the renderer hasn't cached. Many resource types are
	 * immutable (e.g. playlists, zip files, DVD ISOs &amp;c.) and only need to
	 * respond to this event once. Most resource types don't "subscribe" to this
	 * event at all. This default implementation provides hooks for immutable
	 * resources and handles the event for resource types that don't care about
	 * it. The rest override this method and handle it accordingly. Currently,
	 * the only resource type that overrides it is {@link RealFile}.
	 *
	 * Note: resolving a resource once (only) doesn't prevent children being
	 * added to or removed from it (if supported). There are other mechanisms
	 * for that e.g. {@link #doRefreshChildren()} (see {@link Feed} for an
	 * example).
	 */
	public synchronized void syncResolve() {
		resolve();
	}

	/**
	 * Use {@link #syncResolve()} instead
	 */
	public void resolve() {
		if (!resolved) {
			resolveOnce();
			// if resolve() isn't overridden, this file/folder is immutable
			// (or doesn't respond to resolve events, which amounts to the
			// same thing), so don't spam it with this event again.
			resolved = true;
		}
	}

	/**
	 * Returns the "base" part of the display name or an empty {@link String} if
	 * none should be displayed. The "base" name is the name of this
	 * {@link StoreResource} without any prefix or suffix.
	 *
	 * @return The base display name or {@code ""}.
	 */
	public String getDisplayNameBase() {
		// this unescape trick is to solve the problem of a name containing
		// unicode stuff like \u005e
		// if it's done here it will fix this for all objects
		return isNoName() || getName() == null ? "" : StringEscapeUtils.unescapeJava(getName());
	}

	/**
	 * Returns the display name for the renderer.
	 *
	 * @return The display name.
	 */
	public String getDisplayName() {
		return getDisplayName(true);
	}

	/**
	 * Returns the display name for the renderer with or without additional
	 * information suffix.
	 *
	 * @param withSuffix if {@code true} additional information is added after
	 * the name itself, if {@code false} nothing is added.
	 * @return The display name.
	 */
	public String getDisplayName(boolean withSuffix) {
		StringBuilder sb = new StringBuilder();

		// Base
		if (parent instanceof ChapterFileTranscodeVirtualFolder && this instanceof StoreItem item && item.getSplitRange() != null) {
			sb.append(">> ");
			sb.append(StringUtil.convertTimeToString(item.getSplitRange().getStartOrZero(), StringUtil.DURATION_TIME_FORMAT));
		} else {
			sb.append(getDisplayNameBase());
		}

		// Suffix
		if (withSuffix) {
			String displayNamesuffix = getDisplayNameSuffix();
			if (StringUtils.isNotBlank(displayNamesuffix)) {
				if (isInsideTranscodeFolder()) {
					sb.setLength(0);
					sb.append(displayNamesuffix);
				} else {
					sb.append(" ").append(displayNamesuffix);
				}
			}
		} else if (isInsideTranscodeFolder() && !(this instanceof ChapterFileTranscodeVirtualFolder)) {
			// This matches the [No transcoding] entry in the TRANSCODE folder
			sb.setLength(0);
		}

		// Engine name for items
		if (this instanceof StoreItem item) {
			String engineName = item.getDisplayNameEngine();
			if (engineName != null) {
				if (sb.length() > 0) {
					sb.append(" ");
				}
				sb.append(engineName);
			}
		}

		// Truncate
		if (nametruncate > 0) {
			return sb.substring(0, nametruncate).trim();
		}

		return sb.toString();
	}

	public String getDisplayNameSuffix() {
		return null;
	}

	/**
	 * Returns the localized display name for the renderer.
	 *
	 * @return The localized display name.
	 */
	public String getLocalizedDisplayName(String lang) {
		return getDisplayName();
	}

	public void truncateDisplayName(String end) {
		nametruncate = getName().lastIndexOf(end);
	}

	public boolean isInsideTranscodeFolder() {
		return parent instanceof FileTranscodeVirtualFolder;
	}

	/**
	 * @param profile
	 * @return Returns an URL pointing to an image representing the item. If
	 * none is available, "thumbnail0000.png" is used.
	 */
	public String getThumbnailURL(DLNAImageProfile profile) {
		StringBuilder sb = MediaServerRequest.getServerThumbnailURL(renderer.getUUID(), encode(getResourceId()));
		if (profile != null) {
			if (DLNAImageProfile.JPEG_RES_H_V.equals(profile)) {
				sb.append("JPEG_RES").append(profile.getH()).append("x");
				sb.append(profile.getV()).append("_");
			} else {
				sb.append(profile).append("_");
			}
		}
		sb.append(encode(getName())).append(".");
		if (profile != null) {
			sb.append(profile.getDefaultExtension());
		} else {
			LOGGER.debug("Warning: Thumbnail without DLNA image profile requested, resulting URL is: \"{}\"", sb.toString());
		}

		return sb.toString();
	}

	/**
	 * @param subs
	 * @return Returns a URL for a given subtitles item. Not used for container
	 * types.
	 */
	public String getSubsURL(MediaSubtitle subs) {
		StringBuilder sb = MediaServerRequest.getServerSubtitlesURL(renderer.getUUID(), getResourceId());
		sb.append(encode(subs.getName()));
		return sb.toString();
	}

	/**
	 * Prototype function. Original comment: need to override if some thumbnail
	 * work is to be done when mediaparserv2 enabled
	 */
	public void checkThumbnail() {
		// need to override if some thumbnail work is to be done when
		// mediaparserv2 enabled
	}

	protected DLNAThumbnailInputStream getGenericThumbnailInputStreamInternal(String fallback) throws IOException {
		if (isFolder()) {
			return GenericIcons.INSTANCE.getGenericFolderIcon();
		}
		return GenericIcons.INSTANCE.getGenericIcon(this);
	}

	/**
	 * Returns the input stream for this resource's generic thumbnail, which is
	 * the first of:
	 * <li>its Format icon, if any
	 * <li>the fallback image, if any
	 * <li>the {@link GenericIcons} icon <br>
	 * <br>
	 * This is a wrapper around {@link #getGenericThumbnailInputStream0()} that
	 * stores the {@link ImageInfo} before returning the {@link InputStream}.
	 *
	 * @param fallback the fallback image, or {@code null}.
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	public final DLNAThumbnailInputStream getGenericThumbnailInputStream(String fallback) throws IOException {
		DLNAThumbnailInputStream inputStream = getGenericThumbnailInputStreamInternal(fallback);
		thumbnailImageInfo = inputStream != null ? inputStream.getImageInfo() : null;
		return inputStream;
	}

	public ImageInfo getThumbnailImageInfo() {
		return thumbnailImageInfo;
	}

	/**
	 * Returns the input stream for this resource's thumbnail (or a default
	 * image if a thumbnail can't be found). Typically overridden by a
	 * subclass.<br>
	 * <br>
	 * This is a wrapper around {@link #getThumbnailInputStream()} that stores
	 * the {@link ImageInfo} before returning the {@link InputStream}.
	 *
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	public final DLNAThumbnailInputStream fetchThumbnailInputStream() throws IOException {
		DLNAThumbnailInputStream inputStream = getThumbnailInputStream();
		thumbnailImageInfo = inputStream != null ? inputStream.getImageInfo() : null;
		return inputStream;
	}

	/**
	 * Returns the input stream for this resource's thumbnail (or a default
	 * image if a thumbnail can't be found). Typically overridden by a subclass.
	 *
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	public DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		return getGenericThumbnailInputStream(null);
	}

	/**
	 * Adds audio and subtitles "flag" filters to the specified
	 * {@link BufferedImageFilterChain} if they should be applied. If
	 * {@code filterChain} is {@code null} and a "flag" filter is added, a new
	 * {@link BufferedImageFilterChain} is created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	public BufferedImageFilterChain addFlagFilters(BufferedImageFilterChain filterChain) {
		return filterChain;
	}

	/**
	 * Prototype function.
	 *
	 * @return true if child can be added to other folder.
	 * @see #addChild(StoreResource)
	 */
	public abstract boolean isValid();

	/**
	 * Returns the {@link MediaInfo} object for this resource, containing the
	 * specifics of this resource, e.g. the duration.
	 *
	 * @return The object containing detailed information.
	 */
	public MediaInfo getMediaInfo() {
		return mediaInfo;
	}

	/**
	 * Sets the the {@link MediaInfo} object that contains all specifics for
	 * this resource.
	 *
	 * @param media The object containing detailed information.
	 * @since 1.50
	 */
	public void setMediaInfo(MediaInfo media) {
		this.mediaInfo = media;
	}

	/**
	 * Returns the {@link MediaStatus} object for this resource, containing the
	 * status of this resource, e.g. the playback count.
	 *
	 * @return The object containing status information.
	 */
	public MediaStatus getMediaStatus() {
		return mediaStatus;
	}

	/**
	 * Sets the the {@link MediaStatus} object that contains all status for this
	 * resource.
	 *
	 * @param mediaStatus The object containing status information.
	 */
	public void setMediaStatus(MediaStatus mediaStatus) {
		this.mediaStatus = mediaStatus;
	}

	/**
	 * Returns the timestamp at which this resource was last modified.
	 *
	 * @return The timestamp.
	 * @since 1.71.0
	 */
	public long getLastModified() {
		return lastModified;
	}

	/**
	 * Sets the timestamp at which this resource was last modified.
	 *
	 * @param lastModified The timestamp to set.
	 * @since 1.71.0
	 */
	protected final void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * Returns whether or not this is a nameless resource.
	 *
	 * @return True if the resource is nameless.
	 */
	public boolean isNoName() {
		return noName;
	}

	/**
	 * Sets whether or not this is a nameless resource. This is particularly
	 * useful in the virtual TRANSCODE folder for a file, where the same file is
	 * copied many times with different audio and subtitle settings. In that
	 * case the name of the file becomes irrelevant and only the settings need
	 * to be shown.
	 *
	 * @param noName Set to true if the resource is nameless.
	 * @since 1.50
	 */
	public void setNoName(boolean noName) {
		this.noName = noName;
	}

	/**
	 * Returns the default renderer configuration for this resource.
	 *
	 * @return The default renderer configuration.
	 * @since 1.50
	 */
	public Renderer getDefaultRenderer() {
		return renderer;
	}

	/**
	 * Returns the timestamp when this resource was last refreshed.
	 *
	 * @return The timestamp.
	 */
	protected long getLastRefreshTime() {
		return lastRefreshTime;
	}

	/**
	 * Sets the timestamp when this resource was last refreshed.
	 *
	 * @param lastRefreshTime The timestamp to set.
	 * @since 1.50
	 */
	protected void setLastRefreshTime(long lastRefreshTime) {
		this.lastRefreshTime = lastRefreshTime;
	}

	public boolean isSearched() {
		return false;
	}

	public void attach(String key, Object data) {
		if (attachments == null) {
			attachments = new HashMap<>();
		}

		attachments.put(key, data);
	}

	public Object getAttachment(String key) {
		return attachments == null ? null : attachments.get(key);
	}

	public boolean isURLResolved() {
		return false;
	}

	/**
	 * Determines if the media should be hidden when browsing. Currently only
	 * video files are hidden.
	 *
	 * @return The result
	 */
	public boolean isHideFullyPlayed() {
		return CONFIGURATION.getFullyPlayedAction() == FullyPlayedAction.HIDE_MEDIA &&
				mediaInfo != null &&
				mediaInfo.isVideo() &&
				mediaStatus != null &&
				mediaStatus.isFullyPlayed();
	}

	public boolean isSubSelectable() {
		return false;
	}

	/**
	 * Handle serialization.
	 *
	 * This method should be overridden by all media types that can be
	 * bookmarked, i.e. serialized to an external file. By default it just
	 * returns null which means the resource is ignored when serializing.
	 */
	public String write() {
		return null;
	}

	// Returns whether the url appears to be ours
	public static boolean isResourceUrl(String url) {
		return url != null && url.contains(MediaServerRequest.getMediaURL().toString());
	}

	// Returns the url's resourceId (i.e. index without trailing filename) if
	// any or null
	public static String parseResourceId(String url) {
		if (isResourceUrl(url)) {
			return new MediaServerRequest(url).getResourceId();
		}
		return null;
	}

	// Returns the url's objectId (i.e. index including trailing filename) if
	// any or null
	public static String parseObjectId(String url) {
		if (isResourceUrl(url)) {
			MediaServerRequest request = new MediaServerRequest(url);
			return request.getResourceId() + MediaServerRequest.PATH_SEPARATOR + request.getOptionalPath();
		}
		return null;
	}

	public boolean isRendererAllowed() {
		return true;
	}

	public StoreResource isCoded() {
		StoreResource tmp = this;
		while (tmp != null) {
			if (tmp instanceof CodeEnter) {
				return tmp;
			}

			tmp = tmp.getParent();
		}

		return null;
	}

	public boolean isCodeValid(StoreResource r) {
		StoreResource res = r.isCoded();
		if (res instanceof CodeEnter codeEnter) {
			return codeEnter.validCode(r);
		}

		// normal case no code in path code is always valid
		return true;
	}

	/**
	 * @return whether the play events (like "Started playing" and "Stopped
	 * playing" will be logged.
	 */
	public boolean isLogPlayEvents() {
		return true;
	}

	public boolean isAddToMediaLibrary() {
		return true;
	}

	/**
	 * @return whether the resource track "fully played".
	 */
	public boolean isFullyPlayedAware() {
		return false;
	}

	/**
	 * @return whether the resource is "fully played".
	 */
	public boolean isFullyPlayed() {
		return false;
	}

	public void setFullyPlayed(boolean fullyPlayed) {
		//nothing to do
	}

	/**
	 * @return whether the media should be marked as "fully played" either with
	 * text or a "fully played" overlay.
	 */
	public boolean isFullyPlayedMark() {
		return isFullyPlayedAware() && (
			CONFIGURATION.getFullyPlayedAction() == FullyPlayedAction.MARK ||
			CONFIGURATION.getFullyPlayedAction() == FullyPlayedAction.MOVE_FOLDER_AND_MARK
		) && isFullyPlayed();
	}

	public boolean isSortable() {
		return isSortable;
	}

	public void setSortable(boolean isSortable) {
		this.isSortable = isSortable;
	}

	/**
	 * Returns an InputStream associated with the fileName.
	 *
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null
	 * otherwise.
	 */
	protected InputStream getResourceInputStream(String fileName) {
		fileName = "/resources/" + fileName;
		fileName = fileName.replace("//", "/");
		ClassLoader cll = this.getClass().getClassLoader();
		InputStream is = cll.getResourceAsStream(fileName.substring(1));

		while (is == null && cll.getParent() != null) {
			cll = cll.getParent();
			is = cll.getResourceAsStream(fileName.substring(1));
		}

		return is;
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	public StoreResource clone() {
		StoreResource o = null;
		try {
			o = (StoreResource) super.clone();
			o.setId(null);

			// Make sure clones (typically #--TRANSCODE--# folder files)
			// have the option to respond to resolve events
			o.resolved = false;

			if (mediaInfo != null) {
				o.mediaInfo = mediaInfo.clone();
			}
		} catch (CloneNotSupportedException e) {
			LOGGER.error(null, e);
		}

		return o;
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [id=");
		result.append(getId());
		result.append(", name=");
		result.append(getName());
		result.append(", full path=");
		result.append(getResourceId());
		result.append(']');
		return result.toString();
	}

	/**
	 * Transforms a String to URL encoded UTF-8.
	 *
	 * @param s
	 * @return Transformed string s in UTF-8 encoding.
	 */
	protected static String encode(String s) {
		try {
			if (s == null) {
				return "";
			}
			s = s.replace('\\', '-').replace('/', '-').replace('%', '-');
			return URLEncoder.encode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			LOGGER.debug("Error while URL encoding \"{}\": {}", s, e.getMessage());
			LOGGER.trace("", e);
		}

		return "";
	}

	public String getGenre() {
		if (mediaInfo != null) {
			if (mediaInfo.isAudio()) {
				if (mediaInfo.hasAudioMetadata()) {
					return mediaInfo.getAudioMetadata().getGenre();
				}
			} else if (mediaInfo.isVideo() && mediaInfo.hasVideoMetadata() && mediaInfo.getVideoMetadata().getGenres() != null) {
				return mediaInfo.getVideoMetadata().getGenres().get(0);
			}
		}
		return null;
	}

}
