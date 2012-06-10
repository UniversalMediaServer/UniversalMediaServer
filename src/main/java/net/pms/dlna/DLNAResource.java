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

import static net.pms.util.StringUtil.addAttribute;
import static net.pms.util.StringUtil.addXMLTagAndAttribute;
import static net.pms.util.StringUtil.closeTag;
import static net.pms.util.StringUtil.encodeXML;
import static net.pms.util.StringUtil.endTag;
import static net.pms.util.StringUtil.openTag;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.MEncoderVideo;
import net.pms.encoders.Player;
import net.pms.encoders.PlayerFactory;
import net.pms.encoders.TSMuxerVideo;
import net.pms.encoders.VideoLanVideoStreaming;
import net.pms.external.AdditionalResourceFolderListener;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.external.StartStopListener;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.SizeLimitInputStream;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.util.ImagesUtil;
import net.pms.util.Iso639;
import net.pms.util.MpegUtil;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents any item that can be browsed via the UPNP ContentDirectory service.
 *
 * TODO: Change all instance variables to private. For backwards compatibility
 * with external plugin code the variables have all been marked as deprecated
 * instead of changed to private, but this will surely change in the future.
 * When everything has been changed to private, the deprecated note can be
 * removed.
 */
public abstract class DLNAResource extends HTTPResource implements Cloneable, Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAResource.class);
	protected static final int MAX_ARCHIVE_ENTRY_SIZE = 10000000;
	protected static final int MAX_ARCHIVE_SIZE_SEEK = 800000000;
	protected static final String TRANSCODE_FOLDER = "#--TRANSCODE--#";
	private final Map<String, Integer> requestIdToRefcount = new HashMap<String, Integer>();
	private static final int STOP_PLAYING_DELAY = 4000;
	private static final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected int specificType;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String id;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected DLNAResource parent;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected Format ext;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected DLNAMediaInfo media;

	/**
	 * @deprecated Use {@link #getMediaAudio()} and {@link
	 * #setMediaAudio(DLNAMediaAudio)} to access this variable.
	 */
	@Deprecated
	protected DLNAMediaAudio media_audio;

	/**
	 * @deprecated Use {@link #getMediaSubtitle()} and {@link
	 * #setMediaSubtitle(DLNAMediaSubtitle)} to access this variable.
	 */
	@Deprecated
	protected DLNAMediaSubtitle media_subtitle;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected long lastmodified;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 *
	 * Represents the transformation to be used to the file. If null, then
	 * @see Player
	 */
	@Deprecated
	protected Player player;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected boolean discovered = false;

	private ProcessWrapper externalProcess;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected boolean srtFile;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected int updateId = 1;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	public static int systemUpdateId = 1;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected boolean noName;

	private int nametruncate;
	private DLNAResource first;
	private DLNAResource second;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 *
	 * The time range for the file containing the start and end time in seconds.
	 */
	@Deprecated
	protected Range.Time splitRange = new Range.Time();

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected int splitTrack;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected String fakeParentId;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	// Ditlew - needs this in one of the derived classes
	@Deprecated
	protected RendererConfiguration defaultRenderer;

	private String dlnaspec;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected boolean avisynth;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 */
	@Deprecated
	protected boolean skipTranscode = false;

	private boolean allChildrenAreFolders = true;
	private String flags;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 *
	 * List of children objects associated with this DLNAResource. This is only valid when the DLNAResource is of the container type.
	 */
	@Deprecated
	protected List<DLNAResource> children;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 *
	 * the id which the last child got, so the next child can get unique id with incrementing this value.
	 */
	@Deprecated
	protected int lastChildrenId;

	/**
	 * @deprecated Use standard getter and setter to access this variable.
	 *
	 * The last time when refresh is called.
	 */
	@Deprecated
	protected long lastRefreshTime;

	/**
	 * Returns parent object, usually a folder type of resource. In the DLDI
	 * queries, the UPNP server needs to give out the parent container where
	 * the item is. The <i>parent</i> represents such a container.
	 *
	 * @return Parent object.
	 */
	public DLNAResource getParent() {
		return parent;
	}

	/**
	 * Set the parent object, usually a folder type of resource. In the DLDI
	 * queries, the UPNP server needs to give out the parent container where
	 * the item is. The <i>parent</i> represents such a container.

	 * @param parent Sets the parent object.
	 */
	public void setParent(DLNAResource parent) {
		this.parent = parent;
	}

	/**
	 * Returns the id of this resource based on the index in its parent
	 * container. Its main purpose is to be unique in the parent container.
	 *
	 * @return The id string.
	 * @since 1.50
	 */
	protected String getId() {
		return id;
	}

	/**
	 * Set the id of this resource based on the index in its parent container.
	 * Its main purpose is to be unique in the parent container. The method is
	 * automatically called by addChildInternal, so most of the time it is not
	 * necessary to call it explicitly.
	 *
	 * @param id
	 * @since 1.50
	 * @see #addChildInternal(DLNAResource)
	 */
	protected void setId(String id) {
		this.id = id;
	}

	/**
	 * String representing this resource ID. This string is used by the UPNP
	 * ContentDirectory service. There is no hard spec on the actual numbering
	 * except for the root container that always has to be "0". In PMS the
	 * format used is <i>number($number)+</i>. A common client that expects a
	 * different format than the one used here is the XBox360. PMS translates
	 * the XBox360 queries on the fly. For more info, check
	 * http://www.mperfect.net/whsUpnp360/ .
	 *
	 * @return The resource id.
	 * @since 1.50
	 */
	public String getResourceId() {
		if (getId() == null) {
			return null;
		}

		if (getParent() != null) {
			return getParent().getResourceId() + '$' + getId();
		} else {
			return getId();
		}
	}

	/**
	 * @see #setId(String)
	 * @param id
	 */
	protected void setIndexId(int id) {
		setId(Integer.toString(id));
	}

	/**
	 *
	 * @return the unique id which identifies the DLNAResource relative to its parent.
	 */
	public String getInternalId() {
		return getId();
	}

	/**
	 *
	 * @return true, if this contain can have a transcode folder
	 */
	public boolean isTranscodeFolderAvailable() {
		return true;
	}

	/**Any {@link DLNAResource} needs to represent the container or item with a String.
	 * @return String to be showed in the UPNP client.
	 */
	public abstract String getName();

	public abstract String getSystemName();

	public abstract long length();

	// Ditlew
	public long length(RendererConfiguration mediaRenderer) {
		return length();
	}

	public abstract InputStream getInputStream() throws IOException;

	public abstract boolean isFolder();

	public String getDlnaContentFeatures() {
		return (dlnaspec != null ? (dlnaspec + ";") : "") + getFlags() + ";DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
	}

	public DLNAResource getPrimaryResource() {
		return first;
	}

	public DLNAResource getSecondaryResource() {
		return second;
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
		} else {
			if (getParent() != null) {
				return getParent().getResourceId();
			} else {
				return "-1";
			}
		}
	}

	public DLNAResource() {
		setSpecificType(Format.UNKNOWN);
		setChildren(new ArrayList<DLNAResource>());
		setUpdateId(1);
	}

	public DLNAResource(int specificType) {
		this();
		setSpecificType(specificType);
	}

	/** Recursive function that searchs through all of the children until it finds
	 * a {@link DLNAResource} that matches the name.<p> Only used by
	 * {@link net.pms.dlna.RootFolder#addWebFolder(File webConf)
	 * addWebFolder(File webConf)} while parsing the web.conf file.
	 * @param name String to be compared the name to.
	 * @return Returns a {@link DLNAResource} whose name matches the parameter name
	 * @see #getName()
	 */
	public DLNAResource searchByName(String name) {
		for (DLNAResource child : getChildren()) {
			if (child.getName().equals(name)) {
				return child;
			}
		}
		return null;
	}

	/**
	 * @param renderer Renderer for which to check if file is supported.
	 * @return true if the given {@link net.pms.configuration.RendererConfiguration
	 *		RendererConfiguration} can understand type of media. Also returns true
	 *		if this DLNAResource is a container.
	 */
	public boolean isCompatible(RendererConfiguration renderer) {
		return getExt() == null
			|| getExt().isUnknown()
			|| (getExt().isVideo() && renderer.isVideoSupported())
			|| (getExt().isAudio() && renderer.isAudioSupported())
			|| (getExt().isImage() && renderer.isImageSupported());
	}

	/**Adds a new DLNAResource to the child list. Only useful if this object is of the container type.<P>
	 * TODO: (botijo) check what happens with the child object. This function can and will transform the child
	 * object. If the transcode option is set, the child item is converted to a container with the real
	 * item and the transcode option folder. There is also a parser in order to get the right name and type,
	 * I suppose. Is this the right place to be doing things like these?
	 * @param child DLNAResource to add to a container type.
	 */
	public void addChild(DLNAResource child) {
		// child may be null (spotted - via rootFolder.addChild() - in a misbehaving plugin
		if (child == null) {
			LOGGER.error("Attempt to add a null child to " + getName(), new NullPointerException("Invalid DLNA resource"));
			return;
		}

		child.setParent(this);

		if (getParent() != null) {
			setDefaultRenderer(getParent().getDefaultRenderer());
		}

		try {
			if (child.isValid()) {
				LOGGER.trace("Adding " + child.getName() + " / class: " + child.getClass().getName());
				VirtualFolder vf = null;

				if (allChildrenAreFolders && !child.isFolder()) {
					allChildrenAreFolders = false;
				}
				addChildInternal(child);

				boolean forceTranscodeV2 = false;
				boolean parserV2 = child.getMedia() != null && getDefaultRenderer() != null && getDefaultRenderer().isMediaParserV2();
				if (parserV2) {
					// We already have useful info, just need to layout folders
					String mimeType = getDefaultRenderer().getFormatConfiguration().match(child.getMedia());
					if (mimeType != null) {
						// This is streamable
						child.getMedia().setMimeType(mimeType.equals(FormatConfiguration.MIMETYPE_AUTO) ? child.getMedia().getMimeType() : mimeType);
					} else {
						// This is transcodable
						forceTranscodeV2 = true;
					}
				}

				if (child.getExt() != null) {
					setSkipTranscode(child.getExt().skip(PMS.getConfiguration().getNoTranscode(), getDefaultRenderer() != null ? getDefaultRenderer().getStreamedExtensions() : null));
				}

				if (child.getExt() != null && (child.getExt().transcodable() || parserV2) && (child.getMedia() == null || parserV2)) {
					if (!parserV2) {
						child.setMedia(new DLNAMediaInfo());
					}

					// Try to determine a player to use for transcoding.
					Player pl = null;

					if (child.getExt().getProfiles() != null && child.getExt().getProfiles().size() > 0) {
						// First try to match a player based on the format profiles.
						int i = 0;

						while (pl == null && i < child.getExt().getProfiles().size()) {
							pl = PlayerFactory.getPlayer(child.getExt().getProfiles().get(i), child.getExt());
							i++;
						}

						// Next, try to match a player based on the name of the DLNAResource.
						// When a match is found it overrules the result of the first try.
						String name = getName();

						for (Class<? extends Player> clazz : child.getExt().getProfiles()) {
							for (Player p : PlayerFactory.getPlayers()) {
								if (p.getClass().equals(clazz)) {
									String end = "[" + p.id() + "]";

									if (name.endsWith(end)) {
										nametruncate = name.lastIndexOf(end);
										pl = p;
										break;
									} else if (getParent() != null && getParent().getName().endsWith(end)) {
										getParent().nametruncate = getParent().getName().lastIndexOf(end);
										pl = p;
										break;
									}
								}
							}
						}
					}

					if (pl != null && !allChildrenAreFolders) {
						boolean forceTranscode = false;
						if (child.getExt() != null) {
							forceTranscode = child.getExt().skip(PMS.getConfiguration().getForceTranscode(), getDefaultRenderer() != null ? getDefaultRenderer().getTranscodedExtensions() : null);
						}

						boolean hasEmbeddedSubs = false;

						if (child.getMedia() != null) {
							for (DLNAMediaSubtitle s : child.getMedia().getSubtitlesCodes()) {
								hasEmbeddedSubs = (hasEmbeddedSubs || s.isEmbedded());
							}
						}

						boolean hasSubsToTranscode = false;

						if (!PMS.getConfiguration().isMencoderDisableSubs()) {
							hasSubsToTranscode = (PMS.getConfiguration().getUseSubtitles() && child.isSrtFile()) || hasEmbeddedSubs;
						}

						boolean isIncompatible = false;

						if (!child.getExt().isCompatible(child.getMedia(),getDefaultRenderer())) {
							isIncompatible = true;
						}

						// Force transcoding if
						// 1- MediaInfo support detected the file was not matched with supported codec configs and no SkipTranscode extension forced by user
						// or 2- ForceTranscode extension forced by user
						// or 3- FFmpeg support and the file is not ps3 compatible (need to remove this ?) and no SkipTranscode extension forced by user
						// or 4- There's some sub files or embedded subs to deal with and no SkipTranscode extension forced by user
						if (forceTranscode || !isSkipTranscode() && (forceTranscodeV2 || isIncompatible || hasSubsToTranscode)) {
							child.setPlayer(pl);
							LOGGER.trace("Switching " + child.getName() + " to player " + pl.toString() + " for transcoding");
						}

						// Should the child be added to the transcode folder?
						if (child.getExt().isVideo() && child.isTranscodeFolderAvailable()) {
							vf = getTranscodeFolder(true);

							if (vf != null) {
								VirtualFolder fileFolder = new FileTranscodeVirtualFolder(child.getName(), null);

								DLNAResource newChild = child.clone();
								newChild.setPlayer(pl);
								newChild.setMedia(child.getMedia());
								// newChild.original = child;
								fileFolder.addChildInternal(newChild);
								LOGGER.trace("Duplicate " + child.getName() + " with player: " + pl.toString());

								vf.addChild(fileFolder);
							}
						}

						for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
							if (listener instanceof AdditionalResourceFolderListener) {
								try {
									((AdditionalResourceFolderListener) listener).addAdditionalFolder(this, child);
								} catch (Throwable t) {
									LOGGER.error(String.format("Failed to add add additional folder for listener of type=%s", listener.getClass()), t);
								}
							}
						}
					} else if (!child.getExt().isCompatible(child.getMedia(),getDefaultRenderer()) && !child.isFolder()) {
						getChildren().remove(child);
					}
				}

				if (child.getExt() != null && child.getExt().getSecondaryFormat() != null && child.getMedia() != null && getDefaultRenderer() != null && getDefaultRenderer().supportsFormat(child.getExt().getSecondaryFormat())) {
					DLNAResource newChild = child.clone();
					newChild.setExt(newChild.getExt().getSecondaryFormat());
					newChild.first = child;
					child.second = newChild;

					if (!newChild.getExt().isCompatible(newChild.getMedia(),getDefaultRenderer()) && newChild.getExt().getProfiles().size() > 0) {
						newChild.setPlayer(PlayerFactory.getPlayer(newChild.getExt().getProfiles().get(0), newChild.getExt()));
					}
					if (child.getMedia() != null && child.getMedia().isSecondaryFormatValid()) {
						addChild(newChild);
					}
				}
			}
		}catch (Throwable t) {
			LOGGER.error(String.format("Failed to add child '%s'", child.getName()), t);

			child.setParent(null);
			getChildren().remove(child);
		}
	}

	/**
	 * Return the transcode virtual folder if it's supported and allowed. If create set to true, it tries to create if not yet created.
	 * @param create
	 * @return
	 */
	TranscodeVirtualFolder getTranscodeFolder(boolean create) {
		if (!isTranscodeFolderAvailable()) {
			return null;
		}
		if (PMS.getConfiguration().getHideTranscodeEnabled()) {
			return null;
		}
		// search for transcode folder
		for (DLNAResource r : getChildren()) {
			if (r instanceof TranscodeVirtualFolder) {
				return (TranscodeVirtualFolder) r;
			}
		}
		if (create) {
			TranscodeVirtualFolder vf = new TranscodeVirtualFolder(null);
			addChildInternal(vf);
			return vf;
		}
		return null;
	}

	/**
	 * Add to the internal list of child nodes, and sets the parent to the
	 * current node.
	 *
	 * @param res
	 */
	protected synchronized void addChildInternal(DLNAResource res) {
		if (res.getInternalId() != null) {
			LOGGER.info("Node({}) already has an ID={}, which is overriden now. The previous parent node was:{}", new Object[] { res.getClass().getName(), res.getResourceId(), res.getParent()});
		}
		getChildren().add(res);
		res.setParent(this);

		setLastChildrenId(getLastChildrenId() + 1);
		res.setIndexId(getLastChildrenId());
	}

	/**
	 * First thing it does it searches for an item matching the given objectID.
	 * If children is false, then it returns the found object as the only object in the list.
	 * TODO: (botijo) This function does a lot more than this!
	 * @param objectId ID to search for.
	 * @param returnChildren State if you want all the children in the returned list.
	 * @param start
	 * @param count
	 * @param renderer Renderer for which to do the actions.
	 * @return List of DLNAResource items.
	 * @throws IOException
	 */
	public synchronized List<DLNAResource> getDLNAResources(String objectId, boolean returnChildren, int start, int count, RendererConfiguration renderer) throws IOException {
		ArrayList<DLNAResource> resources = new ArrayList<DLNAResource>();
		DLNAResource resource = search(objectId, count, renderer);

		if (resource != null) {
			resource.setDefaultRenderer(renderer);

			if (!returnChildren) {
				resources.add(resource);
				resource.refreshChildrenIfNeeded();
			} else {
				resource.discoverWithRenderer(renderer, count, true);

				if (count == 0) {
					count = resource.getChildren().size();
				}

				if (count > 0) {
					ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(count);

					int parallel_thread_number = 3;
					if (resource instanceof DVDISOFile) {
						parallel_thread_number = 1; // my dvd drive is dying wih 3 parallel threads
					}
					ThreadPoolExecutor tpe = new ThreadPoolExecutor(Math.min(count, parallel_thread_number), count, 20, TimeUnit.SECONDS, queue);

					for (int i = start; i < start + count; i++) {
						if (i < resource.getChildren().size()) {
							final DLNAResource child = resource.getChildren().get(i);
							if (child != null) {
								tpe.execute(child);
								resources.add(child);
							}
						}
					}
					try {
						tpe.shutdown();
						tpe.awaitTermination(20, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
					}
					LOGGER.trace("End of analysis");
				}
			}
		}
		return resources;
	}

	protected void refreshChildrenIfNeeded() {
		if (isDiscovered() && isRefreshNeeded()) {
			refreshChildren();
			notifyRefresh();
		}
	}

	/**
	 * update the last refresh time.
	 */
	protected void notifyRefresh() {
		setLastRefreshTime(System.currentTimeMillis());
		setUpdateId(getUpdateId() + 1);
		setSystemUpdateId(getSystemUpdateId() + 1);
	}

	final protected void discoverWithRenderer(RendererConfiguration renderer, int count, boolean forced) {
		// Discovering if not already done.
		if (!isDiscovered()) {
			discoverChildren();
			boolean ready = true;
			if (renderer.isMediaParserV2() && renderer.isDLNATreeHack()) {
				ready = analyzeChildren(count);
			} else {
				ready = analyzeChildren(-1);
			}
			if (!renderer.isMediaParserV2() || ready) {
				setDiscovered(true);
			}
			notifyRefresh();
		} else {
			// if forced, then call the old 'refreshChildren' method
			LOGGER.trace("discover {} refresh forced: {}", getResourceId(), forced);
			if (forced) {
				if (refreshChildren()) {
					notifyRefresh();
				}
			} else {
				// if not, then the regular isRefreshNeeded/doRefreshChildren pair.
				if (isRefreshNeeded()) {
					doRefreshChildren();
					notifyRefresh();
				}
			}
		}
	}

	@Override
	public void run() {
		if (first == null) {
			resolve();
			if (second != null) {
				second.resolve();
			}
		}
	}

	/**Recursive function that searches for a given ID.
	 * @param searchId ID to search for.
	 * @param renderer
	 * @param count
	 * @return Item found, or null otherwise.
	 * @see #getId()
	 *
	 */
	public DLNAResource search(String searchId, int count, RendererConfiguration renderer) {
		if (getId() != null && searchId != null) {
			String[] indexPath = searchId.split("\\$", 2);
			if (getId().equals(indexPath[0])) {
				if (indexPath.length == 1 || indexPath[1].length() == 0) {
					return this;
				} else {
					discoverWithRenderer(renderer, count, false);
					for (DLNAResource file : getChildren()) {
						DLNAResource found = file.search(indexPath[1], count, renderer);
						if (found != null) {
							return found;
						}
					}
				}
			} else {
				return null;
			}
		}
		return null;
	}

	/**
	 * TODO: (botijo) What is the intention of this function? Looks like a prototype to be overloaded.
	 */
	public void discoverChildren() {
	}

	/**
	 * TODO: (botijo) What is the intention of this function? Looks like a prototype to be overloaded.
	 * @param count
	 * @return Returns true
	 */
	public boolean analyzeChildren(int count) {
		return true;
	}

	/**
	 * Reload the list of children.
	 */
	public void doRefreshChildren() {
	}

	/**
	 * @return true, if the container is changed, so refresh is needed.
	 * This could be called a lot of times.
	 */
	public boolean isRefreshNeeded() {
		return false;
	}

	/**
	 * This method gets called only for the browsed folder, and not for the
	 * parent folders. (And in the media library scan step too). Override in
	 * plugins when you do not want to implement proper change tracking, and
	 * you do not care if the hierarchy of nodes getting invalid between.
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

	protected void checktype() {
		if (getExt() == null) {
			setExt(FormatFactory.getAssociatedExtension(getSystemName()));
		}
		if (getExt() != null && getExt().isUnknown()) {
			getExt().setType(getSpecificType());
		}
	}

	/**
	 * Determine all properties for this DLNAResource that are relevant for playback
	 * or hierarchy traversal. This can be a costly operation, so when the method is
	 * finished the property <code>resolved</code> is set to <code>true</code>.
	 */
	public void resolve() {
	}

	// Ditlew
	/**Returns the DisplayName for the default renderer.
	 * @return The display name.
	 * @see #getDisplayName(RendererConfiguration)
	 */
	public String getDisplayName() {
		return getDisplayName(null);
	}

	// Ditlew - org
	//public String getDisplayName() {
	// Ditlew
	/**Returns the DisplayName that is shown to the Renderer. Depending on the settings,
	 * extra info might be appended, like item duration.<p>
	 * This is based on {@link #getName()}.
	 * @param mediaRenderer Media Renderer for which to show information.
	 * @return String representing the item.
	 */
	public String getDisplayName(RendererConfiguration mediaRenderer) {
		String name = getName();
		if (this instanceof RealFile && PMS.getConfiguration().isHideExtensions() && !isFolder()) {
			name = FileUtil.getFileNameWithoutExtension(name);
		}
		if (getPlayer() != null) {
			if (isNoName()) {
				name = "[" + getPlayer().name() + "]";
			} else {
				// Ditlew - WDTV Live don't show durations otherwise, and this is useful for finding the main title
				if (mediaRenderer != null && mediaRenderer.isShowDVDTitleDuration() && getMedia().getDvdtrack() > 0) {
					name += " - " + getMedia().getDurationString();
				}

				if (!PMS.getConfiguration().isHideEngineNames()) {
					name += " [" + getPlayer().name() + "]";
				}
			}
		} else {
			if (isNoName()) {
				name = "[No encoding]";
			} else if (nametruncate > 0) {
				name = name.substring(0, nametruncate).trim();
			}
		}

		if (isSrtFile() && (getMediaAudio() == null && getMediaSubtitle() == null)
				&& (getPlayer() == null || getPlayer().isExternalSubtitlesSupported())) {
			name += " {External Subtitles}";
		}

		if (getMediaAudio() != null) {
			name = (getPlayer() != null ? ("[" + getPlayer().name() + "]") : "") + " {Audio: " + getMediaAudio().getAudioCodec() + "/" + getMediaAudio().getLangFullName() + ((getMediaAudio().getFlavor() != null && mediaRenderer != null && mediaRenderer.isShowAudioMetadata()) ? (" (" + getMediaAudio().getFlavor() + ")") : "") + "}";
		}

		if (getMediaSubtitle() != null && getMediaSubtitle().getId() != -1) {
			name += " {Sub: " + getMediaSubtitle().getSubType() + "/" + getMediaSubtitle().getLangFullName() + ((getMediaSubtitle().getFlavor() != null && mediaRenderer != null && mediaRenderer.isShowSubMetadata()) ? (" (" + getMediaSubtitle().getFlavor() + ")") : "") + "}";
		}

		if (isAvisynth()) {
			name = (getPlayer() != null ? ("[" + getPlayer().name()) : "") + " + AviSynth]";
		}

		if (getSplitRange().isEndLimitAvailable()) {
			name = ">> " + DLNAMediaInfo.getDurationString(getSplitRange().getStart());
		}

		return name;
	}

	/**Prototype for returning URLs.
	 * @return An empty URL
	 */
	protected String getFileURL() {
		return getURL("");
	}

	/**
	 * @return Returns a URL pointing to an image representing the item. If none is available, "thumbnail0000.png" is used.
	 */
	protected String getThumbnailURL() {
		StringBuilder sb = new StringBuilder();
		sb.append(PMS.get().getServer().getURL());
		sb.append("/images/");
		String id = null;
		if (getMediaAudio() != null) {
			id = getMediaAudio().getLang();
		}
		if (getMediaSubtitle() != null && getMediaSubtitle().getId() != -1) {
			id = getMediaSubtitle().getLang();
		}
		if ((getMediaSubtitle() != null || getMediaAudio() != null) && StringUtils.isBlank(id)) {
			id = DLNAMediaLang.UND;
		}
		if (id != null) {
			String code = Iso639.getISO639_2Code(id.toLowerCase());
			sb.append("codes/").append(code).append(".png");
			return sb.toString();
		}
		if (isAvisynth()) {
			sb.append("avisynth-logo-gears-mod.png");
			return sb.toString();
		}
		return getURL("thumbnail0000");
	}

	/**
	 * @param prefix
	 * @return Returns an URL for a given media item. Not used for container types.
	 */
	protected String getURL(String prefix) {
		StringBuilder sb = new StringBuilder();
		sb.append(PMS.get().getServer().getURL());
		sb.append("/get/");
		sb.append(getResourceId()); //id
		sb.append("/");
		sb.append(prefix);
		sb.append(encode(getName()));
		return sb.toString();
	}

	/**Transforms a String to UTF-8.
	 * @param s
	 * @return Transformed string s in UTF-8 encoding.
	 */
	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.debug("Caught exception", e);
		}
		return "";
	}

	/**
	 * @return Number of children objects. This might be used in the DLDI response, as some renderers might
	 * not have enough memory to hold the list for all children.
	 */
	public int childrenNumber() {
		if (getChildren() == null) {
			return 0;
		}
		return getChildren().size();
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */

	@Override
	protected DLNAResource clone() {
		DLNAResource o = null;
		try {
			o = (DLNAResource) super.clone();
			o.setId(null);
		} catch (CloneNotSupportedException e) {
			LOGGER.error(null, e);
		}
		return o;
	}

	public String getFlags() {
		return flags;
	}

	/**Returns a representation using DIDL response lines. It gives a complete representation of the item, with as many tags as available.
	 * Recommendations as per UPNP specification are followed where possible.
	 * @param mediaRenderer Media Renderer for which to represent this information. Useful for some hacks.
	 * @return String representing the item. An example would start like this: {@code <container id="0$1" childCount=1 parentID="0" restricted="true">}
	 */
	public final String toString(RendererConfiguration mediaRenderer) {
		StringBuilder sb = new StringBuilder();
		if (isFolder()) {
			openTag(sb, "container");
		} else {
			openTag(sb, "item");
		}

		addAttribute(sb, "id", getResourceId());

		if (isFolder()) {
			if (!isDiscovered() && childrenNumber() == 0) {
				//  When a folder has not been scanned for resources, it will automatically have zero children.
				//  Some renderers like XBMC will assume a folder is empty when encountering childCount="0" and
				//  will not display the folder. By returning childCount="1" these renderers will still display
				//  the folder. When it is opened, its children will be discovered and childrenNumber() will be
				//  set to the right value.
				addAttribute(sb, "childCount", 1);
			} else {
				addAttribute(sb, "childCount", childrenNumber());
			}
		}
		addAttribute(sb, "parentID", getParentId());
		addAttribute(sb, "restricted", "true");
		endTag(sb);

		final DLNAMediaAudio firstAudioTrack = getMedia() != null ? getMedia().getFirstAudioTrack() : null;
		if (firstAudioTrack != null && StringUtils.isNotBlank(firstAudioTrack.getSongname())) {
			addXMLTagAndAttribute(sb, "dc:title", encodeXML(firstAudioTrack.getSongname() + (getPlayer() != null && !PMS.getConfiguration().isHideEngineNames() ? (" [" + getPlayer().name() + "]") : "")));
		} else // Ditlew - org
		//addXMLTagAndAttribute(sb, "dc:title", encodeXML((isFolder()||player==null)?getDisplayName():mediaRenderer.getUseSameExtension(getDisplayName())));
		// Ditlew
		{
			addXMLTagAndAttribute(sb, "dc:title", encodeXML((isFolder() || getPlayer() == null) ? getDisplayName() : mediaRenderer.getUseSameExtension(getDisplayName(mediaRenderer))));
		}

		if (firstAudioTrack != null) {
			if (StringUtils.isNotBlank(firstAudioTrack.getAlbum())) {
				addXMLTagAndAttribute(sb, "upnp:album", encodeXML(firstAudioTrack.getAlbum()));
			}
			if (StringUtils.isNotBlank(firstAudioTrack.getArtist())) {
				addXMLTagAndAttribute(sb, "upnp:artist", encodeXML(firstAudioTrack.getArtist()));
				addXMLTagAndAttribute(sb, "dc:creator", encodeXML(firstAudioTrack.getArtist()));
			}
			if (StringUtils.isNotBlank(firstAudioTrack.getGenre())) {
				addXMLTagAndAttribute(sb, "upnp:genre", encodeXML(firstAudioTrack.getGenre()));
			}
			if (firstAudioTrack.getTrack() > 0) {
				addXMLTagAndAttribute(sb, "upnp:originalTrackNumber", "" + firstAudioTrack.getTrack());
			}
		}

		if (!isFolder()) {
			int indexCount = 1;
			if (mediaRenderer.isDLNALocalizationRequired()) {
				indexCount = getDLNALocalesCount();
			}
			for (int c = 0; c < indexCount; c++) {
				openTag(sb, "res");
				// DLNA.ORG_OP : 1er 10 = exemple: TimeSeekRange.dlna.org :npt=187.000-
				//                   01 = Range par octets
				//                   00 = pas de range, meme pas de pause possible
				flags = "DLNA.ORG_OP=01";
				if (getPlayer() != null) {
					if (getPlayer().isTimeSeekable() && mediaRenderer.isSeekByTime()) {
						if (mediaRenderer.isPS3()) // ps3 doesn't like OP=11
						{
							flags = "DLNA.ORG_OP=10";
						} else {
							flags = "DLNA.ORG_OP=11";
						}
					}
				} else {
					if (mediaRenderer.isSeekByTime() && !mediaRenderer.isPS3()) {
						flags = "DLNA.ORG_OP=11";
					}
				}
				addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");

				String mime = getRendererMimeType(mimeType(), mediaRenderer);
				if (mime == null) {
					mime = "video/mpeg";
				}
				if (mediaRenderer.isPS3()) { // XXX TO REMOVE, OR AT LEAST MAKE THIS GENERIC // whole extensions/mime-types mess to rethink anyway
					if (mime.equals("video/x-divx")) {
						dlnaspec = "DLNA.ORG_PN=AVI";
					} else if (mime.equals("video/x-ms-wmv") && getMedia() != null && getMedia().getHeight() > 700) {
						dlnaspec = "DLNA.ORG_PN=WMVHIGH_PRO";
					}
				} else {
					if (mime.equals("video/mpeg")) {
						if (getPlayer() != null) {
							// do we have some mpegts to offer ?
							boolean mpegTsMux = TSMuxerVideo.ID.equals(getPlayer().id()) || VideoLanVideoStreaming.ID.equals(getPlayer().id());
							if (!mpegTsMux) { // maybe, like the ps3, mencoder can launch tsmuxer if this a compatible H264 video
								mpegTsMux = MEncoderVideo.ID.equals(getPlayer().id()) && ((getMediaSubtitle() == null && getMedia() != null && getMedia().getDvdtrack() == 0 && getMedia().isMuxable(mediaRenderer)
									&& PMS.getConfiguration().isMencoderMuxWhenCompatible() && mediaRenderer.isMuxH264MpegTS())
									|| mediaRenderer.isTranscodeToMPEGTSAC3());
							}
							if (mpegTsMux) {
								dlnaspec = getMedia().isH264() && !VideoLanVideoStreaming.ID.equals(getPlayer().id()) && getMedia().isMuxable(mediaRenderer) ? "DLNA.ORG_PN=AVC_TS_HD_24_AC3_ISO" : "DLNA.ORG_PN=" + getMPEG_TS_SD_EU_ISOLocalizedValue(c);
							} else {
								dlnaspec = "DLNA.ORG_PN=" + getMPEG_PS_PALLocalizedValue(c);
							}
						} else if (getMedia() != null) {
							if (getMedia().isMpegTS()) {
								dlnaspec = getMedia().isH264() ? "DLNA.ORG_PN=AVC_TS_HD_50_AC3" : "DLNA.ORG_PN=" + getMPEG_TS_SD_EULocalizedValue(c);
							} else {
								dlnaspec = "DLNA.ORG_PN=" + getMPEG_PS_PALLocalizedValue(c);
							}
						} else {
							dlnaspec = "DLNA.ORG_PN=" + getMPEG_PS_PALLocalizedValue(c);
						}
					} else if (mime.equals("video/vnd.dlna.mpeg-tts")) {
						// patters - on Sony BDP m2ts clips aren't listed without this
						dlnaspec = "DLNA.ORG_PN=" + getMPEG_TS_SD_EULocalizedValue(c);
					} else if (mime.equals("image/jpeg")) {
						dlnaspec = "DLNA.ORG_PN=JPEG_LRG";
					} else if (mime.equals("audio/mpeg")) {
						dlnaspec = "DLNA.ORG_PN=MP3";
					} else if (mime.substring(0, 9).equals("audio/L16") || mime.equals("audio/wav")) {
						dlnaspec = "DLNA.ORG_PN=LPCM";
					}
				}

				if (dlnaspec != null) {
					dlnaspec = "DLNA.ORG_PN=" + mediaRenderer.getDLNAPN(dlnaspec.substring(12));
				}

				if (!mediaRenderer.isDLNAOrgPNUsed()) {
					dlnaspec = null;
				}

				addAttribute(sb, "protocolInfo", "http-get:*:" + mime + ":" + (dlnaspec != null ? (dlnaspec + ";") : "") + flags);


				if (getExt() != null && getExt().isVideo() && getMedia() != null && getMedia().isMediaparsed()) {
					if (getPlayer() == null && getMedia() != null) {
						addAttribute(sb, "size", getMedia().getSize());
					} else {
						long transcoded_size = mediaRenderer.getTranscodedSize();
						if (transcoded_size != 0) {
							addAttribute(sb, "size", transcoded_size);
						}
					}
					if (getMedia().getDuration() != null) {
						if (getSplitRange().isEndLimitAvailable()) {
							addAttribute(sb, "duration", DLNAMediaInfo.getDurationString(getSplitRange().getDuration()));
						} else {
							addAttribute(sb, "duration", getMedia().getDurationString());
						}
					}
					if (getMedia().getResolution() != null) {
						addAttribute(sb, "resolution", getMedia().getResolution());
					}
					addAttribute(sb, "bitrate", getMedia().getRealVideoBitrate());
					if (firstAudioTrack != null) {
						if (firstAudioTrack.getNrAudioChannels() > 0) {
							addAttribute(sb, "nrAudioChannels", firstAudioTrack.getNrAudioChannels());
						}
						if (firstAudioTrack.getSampleFrequency() != null) {
							addAttribute(sb, "sampleFrequency", firstAudioTrack.getSampleFrequency());
						}
					}
				} else if (getExt() != null && getExt().isImage()) {
					if (getMedia() != null && getMedia().isMediaparsed()) {
						addAttribute(sb, "size", getMedia().getSize());
						if (getMedia().getResolution() != null) {
							addAttribute(sb, "resolution", getMedia().getResolution());
						}
					} else {
						addAttribute(sb, "size", length());
					}
				} else if (getExt() != null && getExt().isAudio()) {
					if (getMedia() != null && getMedia().isMediaparsed()) {
						addAttribute(sb, "bitrate", getMedia().getBitrate());
						if (getMedia().getDuration() != null) {
							addAttribute(sb, "duration", DLNAMediaInfo.getDurationString(getMedia().getDuration()));
						}
						if (firstAudioTrack != null && firstAudioTrack.getSampleFrequency() != null) {
							addAttribute(sb, "sampleFrequency", firstAudioTrack.getSampleFrequency());
						}
						if (firstAudioTrack != null) {
							addAttribute(sb, "nrAudioChannels", firstAudioTrack.getNrAudioChannels());
						}

						if (getPlayer() == null) {
							addAttribute(sb, "size", getMedia().getSize());
						} else {
							// calcul taille wav
							if (firstAudioTrack != null) {
								int defaultFrequency = mediaRenderer.isTranscodeAudioTo441() ? 44100 : 48000;
								if (!PMS.getConfiguration().isAudioResample()) {
									try {
										// FIXME: Which exception could be thrown here?
										defaultFrequency = firstAudioTrack.getSampleRate();
									} catch (Exception e) {
										LOGGER.debug("Caught exception", e);
									}
								}
								int na = firstAudioTrack.getNrAudioChannels();
								if (na > 2) // no 5.1 dump in mplayer
								{
									na = 2;
								}
								int finalsize = (int) (getMedia().getDurationInSeconds() * defaultFrequency * 2 * na);
								LOGGER.debug("Calculated size: " + finalsize);
								addAttribute(sb, "size", finalsize);
							}
						}
					} else {
						addAttribute(sb, "size", length());
					}
				} else {
					addAttribute(sb, "size", DLNAMediaInfo.TRANS_SIZE);
					addAttribute(sb, "duration", "09:59:59");
					addAttribute(sb, "bitrate", "1000000");
				}
				endTag(sb);
				sb.append(getFileURL());
				closeTag(sb, "res");
			}
		}

		String thumbURL = getThumbnailURL();
		if (!isFolder() && (getExt() == null || (getExt() != null && thumbURL != null))) {
			openTag(sb, "upnp:albumArtURI");
			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");

			if (getThumbnailContentType().equals(PNG_TYPEMIME) && !mediaRenderer.isForceJPGThumbnails()) {
				addAttribute(sb, "dlna:profileID", "PNG_TN");
			} else {
				addAttribute(sb, "dlna:profileID", "JPEG_TN");
			}
			endTag(sb);
			sb.append(thumbURL);
			closeTag(sb, "upnp:albumArtURI");
		}

		if ((isFolder() || mediaRenderer.isForceJPGThumbnails()) && thumbURL != null) {
			openTag(sb, "res");

			if (getThumbnailContentType().equals(PNG_TYPEMIME) && !mediaRenderer.isForceJPGThumbnails()) {
				addAttribute(sb, "protocolInfo", "http-get:*:image/png:DLNA.ORG_PN=PNG_TN");
			} else {
				addAttribute(sb, "protocolInfo", "http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN");
			}
			endTag(sb);
			sb.append(thumbURL);
			closeTag(sb, "res");
		}

		if (getLastmodified() > 0) {
			addXMLTagAndAttribute(sb, "dc:date", SDF_DATE.format(new Date(getLastmodified())));
		}

		String uclass = null;
		if (first != null && getMedia() != null && !getMedia().isSecondaryFormatValid()) {
			uclass = "dummy";
		} else {
			if (isFolder()) {
				uclass = "object.container.storageFolder";
				boolean xbox = mediaRenderer.isXBOX();
				if (xbox && getFakeParentId() != null && getFakeParentId().equals("7")) {
					uclass = "object.container.album.musicAlbum";
				} else if (xbox && getFakeParentId() != null && getFakeParentId().equals("6")) {
					uclass = "object.container.person.musicArtist";
				} else if (xbox && getFakeParentId() != null && getFakeParentId().equals("5")) {
					uclass = "object.container.genre.musicGenre";
				} else if (xbox && getFakeParentId() != null && getFakeParentId().equals("F")) {
					uclass = "object.container.playlistContainer";
				}
			} else if (getExt() != null && getExt().isVideo()) {
				uclass = "object.item.videoItem";
			} else if (getExt() != null && getExt().isImage()) {
				uclass = "object.item.imageItem.photo";
			} else if (getExt() != null && getExt().isAudio()) {
				uclass = "object.item.audioItem.musicTrack";
			} else {
				uclass = "object.item.videoItem";
			}
		}
		if (uclass != null) {
			addXMLTagAndAttribute(sb, "upnp:class", uclass);
		}

		if (isFolder()) {
			closeTag(sb, "container");
		} else {
			closeTag(sb, "item");
		}
		return sb.toString();
	}

	private String getRequestId(String rendererId) {
		return String.format("%s|%x|%s", rendererId, hashCode(), getSystemName());
	}

	/**
	 * Plugin implementation. When this item is going to play, it will notify all the StartStopListener objects available.
	 * @see StartStopListener
	 */
	public void startPlaying(final String rendererId) {
		final String requestId = getRequestId(rendererId);
		synchronized (requestIdToRefcount) {
			Integer temp = requestIdToRefcount.get(requestId);
			if (temp == null) {
				temp = 0;
			}
			final Integer refCount = temp;
			requestIdToRefcount.put(requestId, refCount + 1);
			if (refCount == 0) {
				final DLNAResource self = this;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						LOGGER.info(String.format("renderer: %s, file: %s", rendererId, getSystemName()));

						for (final ExternalListener listener : ExternalFactory.getExternalListeners()) {
							if (listener instanceof StartStopListener) {
								// run these asynchronously for slow handlers (e.g. logging, scrobbling)
								Runnable fireStartStopEvent = new Runnable() {
									@Override
									public void run() {
										try {
											((StartStopListener) listener).nowPlaying(getMedia(), self);
										} catch (Throwable t) {
											LOGGER.error(String.format("Notification of startPlaying event failed for StartStopListener %s", listener.getClass()), t);
										}
									}
								};
								new Thread(fireStartStopEvent, "StartPlaying Event for " + listener.name()).start();
							}
						}
					}
				};

				new Thread(r, "StartPlaying Event").start();
			}
		}
	}

	/**
	 * Plugin implementation. When this item is going to stop playing, it will notify all the StartStopListener
	 * objects available.
	 * @see StartStopListener
	 */
	public void stopPlaying(final String rendererId) {
		final DLNAResource self = this;
		final String requestId = getRequestId(rendererId);
		Runnable defer = new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(STOP_PLAYING_DELAY);
				} catch (InterruptedException e) {
					LOGGER.error("stopPlaying sleep interrupted", e);
				}

				synchronized (requestIdToRefcount) {
					final Integer refCount = requestIdToRefcount.get(requestId);
					assert refCount != null;
					assert refCount > 0;
					requestIdToRefcount.put(requestId, refCount - 1);

					Runnable r = new Runnable() {
						@Override
						public void run() {
							if (refCount == 1) {
								LOGGER.info(String.format("renderer: %s, file: %s", rendererId, getSystemName()));
								PMS.get().getFrame().setStatusLine("");

								for (final ExternalListener listener : ExternalFactory.getExternalListeners()) {
									if (listener instanceof StartStopListener) {
										// run these asynchronously for slow handlers (e.g. logging, scrobbling)
										Runnable fireStartStopEvent = new Runnable() {
											@Override
											public void run() {
												try {
													((StartStopListener) listener).donePlaying(getMedia(), self);
												} catch (Throwable t) {
													LOGGER.error(String.format("Notification of donePlaying event failed for StartStopListener %s", listener.getClass()), t);
												}
											}
										};
										new Thread(fireStartStopEvent, "StopPlaying Event for " + listener.name()).start();
									}
								}
							}
						}
					};

					new Thread(r, "StopPlaying Event").start();
				}
			}
		};

		new Thread(defer, "StopPlaying Event Deferrer").start();
	}

	/**
	 * Returns an InputStream of this DLNAResource that starts at a given time, if possible. Very useful if video chapters are being used.
	 * @param range
	 * @param mediarenderer
	 * @return The inputstream
	 * @throws IOException
	 */
	public InputStream getInputStream(Range range, RendererConfiguration mediarenderer) throws IOException {
		LOGGER.trace("Asked stream chunk : " + range + " of " + getName() + " and player " + getPlayer());

		// shagrath: small fix, regression on chapters
		boolean timeseek_auto = false;
		// Ditlew - WDTV Live
		// Ditlew - We convert byteoffset to timeoffset here. This needs the stream to be CBR!
		int cbr_video_bitrate = mediarenderer.getCBRVideoBitrate();
		long low = range.isByteRange() && range.isStartOffsetAvailable() ? range.asByteRange().getStart() : 0;
		long high = range.isByteRange() && range.isEndLimitAvailable() ? range.asByteRange().getEnd() : -1;
		Range.Time timeRange = range.createTimeRange();

		if (getPlayer() != null && low > 0 && cbr_video_bitrate > 0) {
			int used_bit_rated = (int) ((cbr_video_bitrate + 256) * 1024 / 8 * 1.04); // 1.04 = container overhead
			if (low > used_bit_rated) {
				timeRange.setStart((double) (low / (used_bit_rated)));
				low = 0;

				// WDTV Live - if set to TS it asks multiple times and ends by
				// asking for an invalid offset which kills mencoder
				if (timeRange.getStartOrZero() > getMedia().getDurationInSeconds()) {
					return null;
				}

				// Should we rewind a little (in case our overhead isn't accurate enough)
				int rewind_secs = mediarenderer.getByteToTimeseekRewindSeconds();
				timeRange.rewindStart(rewind_secs);

				//shagrath:
				timeseek_auto = true;
			}
		}

		if (getPlayer() == null) {
			if (this instanceof IPushOutput) {
				PipedOutputStream out = new PipedOutputStream();
				InputStream fis = new PipedInputStream(out);
				((IPushOutput) this).push(out);
				if (fis != null) {
					if (low > 0) {
						fis.skip(low);
					}
					// http://www.ps3mediaserver.org/forum/viewtopic.php?f=11&t=12035
					fis = wrap(fis, high, low);
				}

				return fis;
			}

			InputStream fis = null;
			if (getExt() != null && getExt().isImage() && getMedia() != null && getMedia().getOrientation() > 1 && mediarenderer.isAutoRotateBasedOnExif()) {
				// seems it's a jpeg file with an orientation setting to take care of
				fis = ImagesUtil.getAutoRotateInputStreamImage(getInputStream(), getMedia().getOrientation());
				if (fis == null) // error, let's return the original one
				{
					fis = getInputStream();
				}
			} else {
				fis = getInputStream();
			}
			if (fis != null) {
				if (low > 0) {
					fis.skip(low);
				}

				// http://www.ps3mediaserver.org/forum/viewtopic.php?f=11&t=12035
				fis = wrap(fis, high, low);

				if (timeRange.getStartOrZero() > 0 && this instanceof RealFile) {
					fis.skip(MpegUtil.getPositionForTimeInMpeg(((RealFile) this).getFile(), (int) timeRange.getStartOrZero() ));
				}
			}
			return fis;
		} else {
			OutputParams params = new OutputParams(PMS.getConfiguration());
			params.aid = getMediaAudio();
			params.sid = getMediaSubtitle();
			params.mediaRenderer = mediarenderer;
			timeRange.limit(getSplitRange());
			params.timeseek = timeRange.getStartOrZero();
			params.timeend = timeRange.getEndOrZero();
			params.shift_scr = timeseek_auto;

			if (this instanceof IPushOutput) {
				params.stdin = (IPushOutput) this;
			}

			if (externalProcess == null || externalProcess.isDestroyed()) {
				LOGGER.info("Starting transcode/remux of " + getName());
				externalProcess = getPlayer().launchTranscode(
					getSystemName(),
					this,
					getMedia(),
					params);
				if (params.waitbeforestart > 0) {
					LOGGER.trace("Sleeping for " + params.waitbeforestart + " milliseconds");
					try {
						Thread.sleep(params.waitbeforestart);
					} catch (InterruptedException e) {
						LOGGER.error(null, e);
					}
					LOGGER.trace("Finished sleeping for " + params.waitbeforestart + " milliseconds");
				}
			} else if (params.timeseek > 0 && getMedia() != null && getMedia().isMediaparsed()
					&& getMedia().getDurationInSeconds() > 0) {
				LOGGER.debug("Requesting time seek: " + params.timeseek + " seconds");
				params.minBufferSize = 1;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						externalProcess.stopProcess();
					}
				};
				new Thread(r, "External Process Stopper").start();
				ProcessWrapper newExternalProcess = getPlayer().launchTranscode(
					getSystemName(),
					this,
					getMedia(),
					params);
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error(null, e);
				}
				if (newExternalProcess == null) {
					LOGGER.trace("External process instance is null... sounds not good");
				}
				externalProcess = newExternalProcess;
			}
			if (externalProcess == null) {
				return null;
			}
			InputStream is = null;
			int timer = 0;
			while (is == null && timer < 10) {
				is = externalProcess.getInputStream(low);
				timer++;
				if (is == null) {
					LOGGER.trace("External input stream instance is null... sounds not good, waiting 500ms");
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
					}
				}
			}

			// fail fast: don't leave a process running indefinitely if it's
			// not producing output after params.waitbeforestart milliseconds + 5 seconds
			// this cleans up lingering MEncoder web video transcode processes that hang
			// instead of exiting
			if (is == null && externalProcess != null && !externalProcess.isDestroyed()) {
				Runnable r = new Runnable() {
					@Override
					public void run() {
						LOGGER.trace("External input stream instance is null... stopping process");
						externalProcess.stopProcess();
					}
				};
				new Thread(r, "Hanging External Process Stopper").start();
			}
			return is;
		}
	}

	/**
	 * Wrap an {@link InputStream} in a {@link SizeLimitInputStream} that sets a
	 * limit to the maximum number of bytes to be read from the original input
	 * stream. The number of bytes is determined by the high and low value
	 * (bytes = high - low). If the high value is less than the low value, the
	 * input stream is not wrapped and returned as is.
	 *
	 * @param input
	 *            The input stream to wrap.
	 * @param high
	 *            The high value.
	 * @param low
	 *            The low value.
	 * @return The resulting input stream.
	 */
	private InputStream wrap(InputStream input, long high, long low) {
		if (input != null && high > low) {
			long bytes = (high - (low < 0 ? 0 : low)) + 1;
			LOGGER.trace("Using size-limiting stream (" + bytes + " bytes)");
			return new SizeLimitInputStream(input, bytes);
		}
		return input;
	}

	public String mimeType() {
		if (getPlayer() != null) {
			return getPlayer().mimeType();
		} else if (getMedia() != null && getMedia().isMediaparsed()) {
			return getMedia().getMimeType();
		} else if (getExt() != null) {
			return getExt().mimeType();
		} else {
			return getDefaultMimeType(getSpecificType());
		}
	}

	/**
	 * Prototype function. Original comment: need to override if some thumbnail work is to be done when mediaparserv2 enabled
	 */
	public void checkThumbnail() {
		// need to override if some thumbnail work is to be done when mediaparserv2 enabled
	}

	/**Checks if a thumbnail exists, and if possible, generates one.
	 * @param input InputFile to check or generate the thumbnail that is being asked for.
	 */
	protected void checkThumbnail(InputFile input) {
		if (getMedia() != null && !getMedia().isThumbready() && PMS.getConfiguration().isThumbnailGenerationEnabled()) {
			getMedia().setThumbready(true);
			getMedia().generateThumbnail(input, getExt(), getType());
			if (getMedia().getThumb() != null && PMS.getConfiguration().getUseCache() && input.getFile() != null) {
				PMS.get().getDatabase().updateThumbnail(input.getFile().getAbsolutePath(), input.getFile().lastModified(), getType(), getMedia());
			}
		}
	}

	/** Returns the input stream for this resource's thumbnail
	 * (or a default image if a thumbnail can't be found).
	 * Typically overridden by a subclass.
	 * @return The InputStream
	 * @throws IOException
	 */
	public InputStream getThumbnailInputStream() throws IOException {
		return getResourceInputStream("images/thumbnail-256.png");
	}

	public String getThumbnailContentType() {
		return HTTPResource.JPEG_TYPEMIME;
	}

	public int getType() {
		if (getExt() != null) {
			return getExt().getType();
		} else {
			return Format.UNKNOWN;
		}
	}

	/**Prototype function.
	 * @return true if child can be added to other folder.
	 * @see #addChild(DLNAResource)
	 */
	public abstract boolean isValid();

	public boolean allowScan() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " [id=" + getId() + ", name=" + getName() + ", full path=" + getResourceId() + ", ext=" + getExt() + ", discovered=" + isDiscovered() + "]";
	}

	/**
	 * Returns the specific type of resource. Valid types are defined in {@link Format}.
	 *
	 * @return The specific type
	 */
	protected int getSpecificType() {
		return specificType;
	}

	/**
	 * Set the specific type of this resource. Valid types are defined in {@link Format}.
	 * @param specificType The specific type to set.
	 */
	protected void setSpecificType(int specificType) {
		this.specificType = specificType;
	}

	/**
	 * Returns the {@link Format} of this resource, which defines its capabilities.
	 *
	 * @return The format of this resource.
	 */
	public Format getExt() {
		return ext;
	}

	/**
	 * Sets the {@link Format} of this resource, thereby defining its capabilities.
	 *
	 * @param ext The format to set.
	 */
	protected void setExt(Format ext) {
		this.ext = ext;
	}

	/**
	 * Returns the {@link DLNAMediaInfo} object for this resource, containing the
	 * specifics of this resource, e.g. the duration.
	 *
	 * @return The object containing detailed information.
	 */
	public DLNAMediaInfo getMedia() {
		return media;
	}

	/**
	 * Sets the the {@link DLNAMediaInfo} object that contains all specifics for
	 * this resource.
	 *
	 * @param media The object containing detailed information.
	 * @since 1.50
	 */
	protected void setMedia(DLNAMediaInfo media) {
		this.media = media;
	}

	/**
	 * Returns the {@link DLNAMediaAudio} object for this resource that contains
	 * the audio specifics.
	 *
	 * @return The audio object containing detailed information.
	 * @since 1.50
	 */
	protected DLNAMediaAudio getMediaAudio() {
		return media_audio;
	}

	/**
	 * Sets the {@link DLNAMediaAudio} object for this resource that contains
	 * the audio specifics.
	 *
	 * @param mediaAudio The audio object containing detailed information.
	 * @since 1.50
	 */
	protected void setMediaAudio(DLNAMediaAudio mediaAudio) {
		this.media_audio = mediaAudio;
	}

	/**
	 * Returns the {@link DLNAMediaSubtitle} object for this resource that
	 * contains the specifics for the subtitles.
	 *
	 * @return The subtitle object containing detailed information.
	 * @since 1.50
	 */
	protected DLNAMediaSubtitle getMediaSubtitle() {
		return media_subtitle;
	}

	/**
	 * Sets the {@link DLNAMediaSubtitle} object for this resource that
	 * contains the specifics for the subtitles.
	 *
	 * @param mediaSubtitle The subtitle object containing detailed information.
	 * @since 1.50
	 */
	protected void setMediaSubtitle(DLNAMediaSubtitle mediaSubtitle) {
		this.media_subtitle = mediaSubtitle;
	}

	/**
	 * Returns the timestamp at which this resource was last modified.
	 *
	 * @return The timestamp.
	 */
	public long getLastmodified() {
		return lastmodified;
	}

	/**
	 * Sets the timestamp at which this resource was last modified.
	 *
	 * @param lastmodified The timestamp to set.
	 * @since 1.50
	 */
	protected void setLastmodified(long lastmodified) {
		this.lastmodified = lastmodified;
	}

	/**
	 * Returns the {@link Player} object that is used to encode this resource
	 * for the renderer. Can be null.
	 *
	 * @return The player object.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Sets the {@link Player} object that is to be used to encode this
	 * resource for the renderer. The player object can be null.
	 *
	 * @param player The player object to set.
	 * @since 1.50
	 */
	protected void setPlayer(Player player) {
		this.player = player;
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
	 * @param discovered Set to true if this resource is discovered,
	 * 			false otherwise.
	 * @since 1.50
	 */
	protected void setDiscovered(boolean discovered) {
		this.discovered = discovered;
	}

	/**
	 * Returns true if this resource has subtitles in a file.
	 *
	 * @return the srtFile
	 * @since 1.50
	 */
	protected boolean isSrtFile() {
		return srtFile;
	}

	/**
	 * Set to true if this resource has subtitles in a file.
	 *
	 * @param srtFile the srtFile to set
	 * @since 1.50
	 */
	protected void setSrtFile(boolean srtFile) {
		this.srtFile = srtFile;
	}

	/**
	 * Returns the update counter for this resource. When the resource needs
	 * to be refreshed, its counter is updated.
	 *
	 * @return The update counter.
	 * @see #notifyRefresh()
	 */
	public int getUpdateId() {
		return updateId;
	}

	/**
	 * Sets the update counter for this resource. When the resource needs
	 * to be refreshed, its counter should be updated.
	 *
	 * @param updateId The counter value to set.
	 * @since 1.50
	 */
	protected void setUpdateId(int updateId) {
		this.updateId = updateId;
	}

	/**
	 * Returns the update counter for all resources. When all resources need
	 * to be refreshed, this counter is updated.
	 *
	 * @return The system update counter.
	 * @since 1.50
	 */
	public static int getSystemUpdateId() {
		return systemUpdateId;
	}

	/**
	 * Sets the update counter for all resources. When all resources need
	 * to be refreshed, this counter should be updated.
	 *
	 * @param systemUpdateId The system update counter to set.
	 * @since 1.50
	 */
	public static void setSystemUpdateId(int systemUpdateId) {
		DLNAResource.systemUpdateId = systemUpdateId;
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
	 * Sets whether or not this is a nameless resource.
	 *
	 * @param noName Set to true if the resource is nameless.
	 * @since 1.50
	 */
	protected void setNoName(boolean noName) {
		this.noName = noName;
	}

	/**
	 * Returns the from - to time range for this resource.
	 *
	 * @return The time range.
	 */
	public Range.Time getSplitRange() {
		return splitRange;
	}

	/**
	 * Sets the from - to time range for this resource.
	 *
	 * @param splitRange The time range to set.
	 * @since 1.50
	 */
	protected void setSplitRange(Range.Time splitRange) {
		this.splitRange = splitRange;
	}

	/**
	 * Returns the number of the track to split from this resource.
	 *
	 * @return the splitTrack
	 * @since 1.50
	 */
	protected int getSplitTrack() {
		return splitTrack;
	}

	/**
	 * Sets the number of the track from this resource to split.
	 *
	 * @param splitTrack The track number.
	 * @since 1.50
	 */
	protected void setSplitTrack(int splitTrack) {
		this.splitTrack = splitTrack;
	}

	/**
	 * Returns the default renderer configuration for this resource.
	 *
	 * @return The default renderer configuration.
	 * @since 1.50
	 */
	protected RendererConfiguration getDefaultRenderer() {
		return defaultRenderer;
	}

	/**
	 * Sets the default renderer configuration for this resource.
	 *
	 * @param defaultRenderer The default renderer configuration to set.
	 * @since 1.50
	 */
	protected void setDefaultRenderer(RendererConfiguration defaultRenderer) {
		this.defaultRenderer = defaultRenderer;
	}

	/**
	 * Returns whether or not this resource is handled by AviSynth.
	 *
	 * @return True if handled by AviSynth, otherwise false.
	 * @since 1.50
	 */
	protected boolean isAvisynth() {
		return avisynth;
	}

	/**
	 * Sets whether or not this resource is handled by AviSynth.
	 *
	 * @param avisynth Set to true if handled by Avisyth, otherwise false.
	 * @since 1.50
	 */
	protected void setAvisynth(boolean avisynth) {
		this.avisynth = avisynth;
	}

	/**
	 * Returns true if transcoding should be skipped for this resource.
	 *
	 * @return True if transcoding should be skipped, false otherwise.
	 * @since 1.50
	 */
	protected boolean isSkipTranscode() {
		return skipTranscode;
	}

	/**
	 * Set to true if transcoding should be skipped for this resource.
	 *
	 * @param skipTranscode Set to true if trancoding should be skipped, false
	 * 			otherwise.
	 * @since 1.50
	 */
	protected void setSkipTranscode(boolean skipTranscode) {
		this.skipTranscode = skipTranscode;
	}

	/**
	 * Returns the list of children for this resource.
	 *
	 * @return List of children objects.
	 */
	public List<DLNAResource> getChildren() {
		return children;
	}

	/**
	 * Sets the list of children for this resource.
	 *
	 * @param children The list of children to set.
	 * @since 1.50
	 */
	protected void setChildren(List<DLNAResource> children) {
		this.children = children;
	}

	/**
	 * Returns the id of the last child added.
	 *
	 * @return The id.
	 * @since 1.50
	 */
	protected int getLastChildrenId() {
		return lastChildrenId;
	}

	/**
	 * Sets the id of the last child added.
	 *
	 * @param lastChildrenId The id to set.
	 * @since 1.50
	 */
	protected void setLastChildrenId(int lastChildrenId) {
		this.lastChildrenId = lastChildrenId;
	}

	/**
	 * Returns the timestamp when this resource was last refreshed.
	 *
	 * @return The timestamp.
	 */
	long getLastRefreshTime() {
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
}

