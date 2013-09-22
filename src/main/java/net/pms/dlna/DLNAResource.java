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

import java.io.*;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.encoders.*;
import net.pms.external.AdditionalResourceFolderListener;
import net.pms.external.ExternalFactory;
import net.pms.external.ExternalListener;
import net.pms.external.StartStopListener;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.SizeLimitInputStream;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.util.ImagesUtil;
import net.pms.util.Iso639;
import net.pms.util.MpegUtil;
import net.pms.util.OpenSubtitle;
import net.pms.util.StringUtil;
import static net.pms.util.StringUtil.*;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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
	private final Map<String, Integer> requestIdToRefcount = new HashMap<>();
	private boolean resolved;
	private static final int STOP_PLAYING_DELAY = 4000;
	private static final Logger LOGGER = LoggerFactory.getLogger(DLNAResource.class);
	private final SimpleDateFormat SDF_DATE = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
	private static final PmsConfiguration configuration = PMS.getConfiguration();

	protected static final int MAX_ARCHIVE_ENTRY_SIZE = 10000000;
	protected static final int MAX_ARCHIVE_SIZE_SEEK = 800000000;

	/**
	 * The name displayed on the renderer. Cached the first time getDisplayName(RendererConfiguration) is called.
	 */
	private String displayName;

	/**
	 * @deprecated This field will be removed. Use {@link net.pms.configuration.PmsConfiguration#getTranscodeFolderName()} instead.
	 */
	@Deprecated
	protected static final String TRANSCODE_FOLDER = Messages.getString("TranscodeVirtualFolder.0"); // localized #--TRANSCODE--#

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected int specificType;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected String id;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected DLNAResource parent;

	/**
	 * @deprecated This field will be removed. Use {@link #getFormat()} and
	 * {@link #setFormat(Format)} instead.
	 */
	@Deprecated
	protected Format ext;

	/**
	 * The format of this resource.
	 */
	private Format format;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected DLNAMediaInfo media;

	/**
	 * @deprecated Use {@link #getMediaAudio()} and {@link
	 * #setMediaAudio(DLNAMediaAudio)} to access this field.
	 */
	@Deprecated
	protected DLNAMediaAudio media_audio;

	/**
	 * @deprecated Use {@link #getMediaSubtitle()} and {@link
	 * #setMediaSubtitle(DLNAMediaSubtitle)} to access this field.
	 */
	@Deprecated
	protected DLNAMediaSubtitle media_subtitle;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected long lastmodified; // TODO make private and rename lastmodified -> lastModified

	/**
	 * Represents the transformation to be used to the file. If null, then
	 * @see Player
	 */
	private Player player;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected boolean discovered = false;

	private ProcessWrapper externalProcess;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected boolean srtFile;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected int updateId = 1;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	public static int systemUpdateId = 1;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected boolean noName;

	private int nametruncate;
	private DLNAResource first;
	private DLNAResource second;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 *
	 * The time range for the file containing the start and end time in seconds.
	 */
	@Deprecated
	protected Range.Time splitRange = new Range.Time();

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected int splitTrack;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected String fakeParentId;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	// Ditlew - needs this in one of the derived classes
	@Deprecated
	protected RendererConfiguration defaultRenderer;

	private String dlnaspec;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected boolean avisynth;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 */
	@Deprecated
	protected boolean skipTranscode = false;

	private boolean allChildrenAreFolders = true;
	private String dlnaOrgOpFlags;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 *
	 * List of children objects associated with this DLNAResource. This is only valid when the DLNAResource is of the container type.
	 */
	@Deprecated
	protected List<DLNAResource> children;

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 *
	 * The numerical ID (1-based index) assigned to the last child of this folder. The next child is assigned this ID + 1.
	 */
	// FIXME should be lastChildId
	@Deprecated
	protected int lastChildrenId = 0; // XXX make private and rename lastChildrenId -> lastChildId

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 *
	 * The last time refresh was called.
	 */
	@Deprecated
	protected long lastRefreshTime;

	private String lastSearch;

	protected HashMap<String,Object> attachments = null;

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
	 * Set the ID of this resource based on the index in its parent container.
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

	/**
	 * Any {@link DLNAResource} needs to represent the container or item with a String.
	 *
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
		return (dlnaspec != null ? (dlnaspec + ";") : "") + getDlnaOrgOpFlags() + ";DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
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
		lastSearch = null;
		resHash = 0;
	}

	public DLNAResource(int specificType) {
		this();
		setSpecificType(specificType);
	}

	/**
	 * Recursive function that searches through all of the children until it finds
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
		return getFormat() == null
			|| getFormat().isUnknown()
			|| (getFormat().isVideo() && renderer.isVideoSupported())
			|| (getFormat().isAudio() && renderer.isAudioSupported())
			|| (getFormat().isImage() && renderer.isImageSupported());
	}

	/**
	 * Adds a new DLNAResource to the child list. Only useful if this object is
	 * of the container type.
	 * <P>
	 * TODO: (botijo) check what happens with the child object. This function
	 * can and will transform the child object. If the transcode option is set,
	 * the child item is converted to a container with the real item and the
	 * transcode option folder. There is also a parser in order to get the right
	 * name and type, I suppose. Is this the right place to be doing things like
	 * these?
	 * <p>
	 * FIXME: Ideally the logic below is completely renderer-agnostic. Focus on
	 * harvesting generic data and transform it for a specific renderer as late
	 * as possible. 
	 * 
	 * @param child
	 *            DLNAResource to add to a container type.
	 */
	public void addChild(DLNAResource child) {
		// child may be null (spotted - via rootFolder.addChild() - in a misbehaving plugin
		if (child == null) {
			LOGGER.error("A plugin has attempted to add a null child to \"{}\"", getName());
			LOGGER.debug("Error info:", new NullPointerException("Invalid DLNA resource"));
			return;
		}

		child.setParent(this);

		if (getParent() != null) {
			setDefaultRenderer(getParent().getDefaultRenderer());
		}

		try {
			if (child.isValid()) {
				LOGGER.trace("Adding new child \"{}\" with class \"{}\"", child.getName(), child.getClass().getName());

				if (allChildrenAreFolders && !child.isFolder()) {
					allChildrenAreFolders = false;
				}

				child.resHash = Math.abs(child.getSystemName().hashCode() + resumeHash());

				DLNAResource resumeRes = null;

				ResumeObj r = ResumeObj.create(child);
				if (r != null) {
					resumeRes = child.clone();
					resumeRes.resume = r;
					resumeRes.resHash = child.resHash;
					addChildInternal(resumeRes);
				}

				addChildInternal(child);

				boolean parserV2 = child.getMedia() != null && getDefaultRenderer() != null && getDefaultRenderer().isMediaParserV2();
				if (parserV2) {
					// See which mime type the renderer prefers in case it supports the media
					String mimeType = getDefaultRenderer().getFormatConfiguration().match(child.getMedia());
					if (mimeType != null) {
						// Media is streamable
						if (!FormatConfiguration.MIMETYPE_AUTO.equals(mimeType)) {
							// Override with the preferred mime type of the renderer
							LOGGER.trace("Overriding detected mime type \"{}\" for file \"{}\" with renderer preferred mime type \"{}\"",
									child.getMedia().getMimeType(), child.getName(), mimeType);
							child.getMedia().setMimeType(mimeType);
						}

						LOGGER.trace("File \"{}\" can be streamed with mime type \"{}\"", child.getName(), child.getMedia().getMimeType());
					} else {
						// Media is transcodable
						LOGGER.trace("File \"{}\" can be transcoded", child.getName());
					}
				}

				if (child.getFormat() != null) {
					String configurationSkipExtensions = configuration.getDisableTranscodeForExtensions();
					String rendererSkipExtensions = null;

					if (getDefaultRenderer() != null) {
						rendererSkipExtensions = getDefaultRenderer().getStreamedExtensions();
					}

					// Should transcoding be skipped for this format?
					boolean skip = child.getFormat().skip(configurationSkipExtensions, rendererSkipExtensions);
					setSkipTranscode(skip);
					
					if (skip) {
						LOGGER.trace("File \"{}\" will be forced to skip transcoding by configuration", child.getName());
					}

					if (parserV2 || (child.getFormat().transcodable() && child.getMedia() == null)) {
						if (!parserV2) {
							child.setMedia(new DLNAMediaInfo());
						}
	
						// Try to determine a player to use for transcoding.
						Player player = null;
	
						// First, try to match a player based on the name of the DLNAResource
						// or its parent. If the name ends in "[unique player id]", that player
						// is preferred.
						String name = getName();
	
						for (Player p : PlayerFactory.getAllPlayers()) {
							String end = "[" + p.id() + "]";
	
							if (name.endsWith(end)) {
								nametruncate = name.lastIndexOf(end);
								player = p;
								LOGGER.trace("Selecting player based on name end");
								break;
							} else if (getParent() != null && getParent().getName().endsWith(end)) {
								getParent().nametruncate = getParent().getName().lastIndexOf(end);
								player = p;
								LOGGER.trace("Selecting player based on parent name end");
								break;
							}
						}
	
						// If no preferred player could be determined from the name, try to
						// match a player based on media information and format.
						if (player == null) {
							player = PlayerFactory.getPlayer(child);
						}
	
						if (player != null && !allChildrenAreFolders) {
							String configurationForceExtensions = configuration.getForceTranscodeForExtensions();
							String rendererForceExtensions = null;
							
							if (getDefaultRenderer() != null) {
								rendererForceExtensions = getDefaultRenderer().getTranscodedExtensions();
							}

							// Should transcoding be forced for this format?
							boolean forceTranscode = child.getFormat().skip(configurationForceExtensions, rendererForceExtensions);

							if (forceTranscode) {
								LOGGER.trace("File \"{}\" will be forced to be transcoded by configuration", child.getName());
							}

							boolean hasEmbeddedSubs = false;
	
							if (child.getMedia() != null) {
								for (DLNAMediaSubtitle s : child.getMedia().getSubtitleTracksList()) {
									hasEmbeddedSubs = (hasEmbeddedSubs || s.isEmbedded());
								}
							}
	
							boolean hasSubsToTranscode = false;
	
							if (!configuration.isDisableSubtitles()) {
								// FIXME: Why transcode if the renderer can handle embedded subs?
								hasSubsToTranscode = (configuration.isAutoloadExternalSubtitles() && child.isSrtFile()) || hasEmbeddedSubs;

								if (hasSubsToTranscode) {
									LOGGER.trace("File \"{}\" has subs that need transcoding", child.getName());
								}
							}
	
							boolean isIncompatible = false;
	
							if (!child.getFormat().isCompatible(child.getMedia(), getDefaultRenderer())) {
								isIncompatible = true;
								LOGGER.trace("File \"{}\" is not supported by the renderer", child.getName());
							}
	
							// Prefer transcoding over streaming if:
							// 1) the media is unsupported by the renderer, or
							// 2) there are subs to transcode
							boolean preferTranscode = isIncompatible || hasSubsToTranscode;

							// Transcode if:
							// 1) transcoding is forced by configuration, or
							// 2) transcoding is preferred and not prevented by configuration
							if (forceTranscode || (preferTranscode && !isSkipTranscode())) {
								child.setPlayer(player);

								if (parserV2) {
									LOGGER.trace("Final verdict: \"{}\" will be transcoded with player \"{}\" with mime type \"{}\"", child.getName(), player.toString(), child.getMedia().getMimeType());
								} else {
									LOGGER.trace("Final verdict: \"{}\" will be transcoded with player \"{}\"", child.getName(), player.toString());
								}
							} else {
								LOGGER.trace("Final verdict: \"{}\" will be streamed", child.getName());
							}
	
							// Should the child be added to the #--TRANSCODE--# folder?
							if ((child.getFormat().isVideo() || child.getFormat().isAudio()) && child.isTranscodeFolderAvailable()) {
								// true: create (and append) the #--TRANSCODE--# folder to this
								// folder if supported/enabled and if it doesn't already exist
								VirtualFolder transcodeFolder = getTranscodeFolder(true);
								if (transcodeFolder != null) {
									VirtualFolder fileTranscodeFolder = new FileTranscodeVirtualFolder(child.getDisplayName(), null);
	
									DLNAResource newChild = child.clone();
									newChild.setPlayer(player);
									newChild.setMedia(child.getMedia());
									fileTranscodeFolder.addChildInternal(newChild);
									LOGGER.trace("Adding \"{}\" to transcode folder for player: \"{}\"", child.getName(), player.toString());
	
									transcodeFolder.addChild(fileTranscodeFolder);
								}
							}
	
							for (ExternalListener listener : ExternalFactory.getExternalListeners()) {
								if (listener instanceof AdditionalResourceFolderListener) {
									try {
										((AdditionalResourceFolderListener) listener).addAdditionalFolder(this, child);
									} catch (Throwable t) {
										LOGGER.error("Failed to add additional folder for listener of type: \"{}\"", listener.getClass(), t);
									}
								}
							}
						} else if (!child.getFormat().isCompatible(child.getMedia(), getDefaultRenderer()) && !child.isFolder()) {
							LOGGER.trace("Ignoring file \"{}\" because it is not compatible with renderer \"{}\"", child.getName(), getDefaultRenderer().getRendererName());
							getChildren().remove(child);
						}
					}

					if (child.getFormat().getSecondaryFormat() != null &&
						child.getMedia() != null &&
						getDefaultRenderer() != null &&
						getDefaultRenderer().supportsFormat(child.getFormat().getSecondaryFormat())
					) {
						DLNAResource newChild = child.clone();
						newChild.setFormat(newChild.getFormat().getSecondaryFormat());
						LOGGER.trace("Detected secondary format \"{}\" for \"{}\"", newChild.getFormat().toString(), newChild.getName());
						newChild.first = child;
						child.second = newChild;
	
						if (!newChild.getFormat().isCompatible(newChild.getMedia(), getDefaultRenderer())) {
							Player player = PlayerFactory.getPlayer(newChild);
							newChild.setPlayer(player);
							LOGGER.trace("Secondary format \"{}\" will use player \"{}\" for \"{}\"", newChild.getFormat().toString(), child.getPlayer().name(), newChild.getName());
						}
	
						if (child.getMedia() != null && child.getMedia().isSecondaryFormatValid()) {
							addChild(newChild);
							LOGGER.trace("Adding secondary format \"{}\" for \"{}\"", newChild.getFormat().toString(), newChild.getName());
						} else {
							LOGGER.trace("Ignoring secondary format \"{}\" for \"{}\": invalid format", newChild.getFormat().toString(), newChild.getName());
						}
					}
				}
			}
		} catch (Throwable t) {
			LOGGER.error("Error adding child: \"{}\"", child.getName(), t);

			child.setParent(null);
			getChildren().remove(child);
		}
	}

	/**
	 * Return the transcode folder for this resource.
	 * If UMS is configured to hide transcode folders, null is returned.
	 * If no folder exists and the create argument is false, null is returned.
	 * If no folder exists and the create argument is true, a new transcode folder is created.
	 * This method is called on the parent frolder each time a child is added to that parent
	 * (via {@link addChild(DLNAResource)}.
	 *
	 * @param create
	 * @return the transcode virtual folder
	 */
	// XXX package-private: used by MapFile; should be protected?
	TranscodeVirtualFolder getTranscodeFolder(boolean create) {
		if (!isTranscodeFolderAvailable()) {
			return null;
		}

		if (configuration.getHideTranscodeEnabled()) {
			return null;
		}

		// search for transcode folder
		for (DLNAResource child : getChildren()) {
			if (child instanceof TranscodeVirtualFolder) {
				return (TranscodeVirtualFolder) child;
			}
		}

		if (create) {
			TranscodeVirtualFolder transcodeFolder = new TranscodeVirtualFolder(null);
			addChildInternal(transcodeFolder);
			return transcodeFolder;
		}

		return null;
	}

	/**
	 * Adds the supplied DNLA resource to the internal list of child nodes,
	 * and sets the parent to the current node. Avoids the side-effects
	 * associated with the {@link #addChild(DLNAResource)} method.
	 *
	 * @param child the DLNA resource to add to this node's list of children
	 */
	protected synchronized void addChildInternal(DLNAResource child) {
		if (child.getInternalId() != null) {
			LOGGER.info(
				"Node ({}) already has an ID ({}), which is overridden now. The previous parent node was: {}",
				new Object[] {
					child.getClass().getName(),
					child.getResourceId(),
					child.getParent()
				}
			);
		}

		getChildren().add(child);
		child.setParent(this);

		setLastChildId(getLastChildId() + 1);
		child.setIndexId(getLastChildId());
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
	public synchronized List<DLNAResource> getDLNAResources(String objectId, boolean children, int start, int count, RendererConfiguration renderer) throws IOException {
		return getDLNAResources(objectId,children,start,count,renderer,null);
	}

	public synchronized List<DLNAResource> getDLNAResources(String objectId, boolean returnChildren, int start, int count, RendererConfiguration renderer, String searchStr) throws IOException {
		ArrayList<DLNAResource> resources = new ArrayList<>();
		DLNAResource dlna = search(objectId, count, renderer, searchStr);

		if (dlna != null) {
			String systemName = dlna.getSystemName();
			dlna.setDefaultRenderer(renderer);

			if (!returnChildren) {
				resources.add(dlna);
				dlna.refreshChildrenIfNeeded(searchStr);
			} else {
				dlna.discoverWithRenderer(renderer, count, true, searchStr);

				if (count == 0) {
					count = dlna.getChildren().size();
				}

				if (count > 0) {
					ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(count);

					int nParallelThreads = 3;
					if (dlna instanceof DVDISOFile) {
						nParallelThreads = 1; // Some DVD drives die with 3 parallel threads
					}
					ThreadPoolExecutor tpe = new ThreadPoolExecutor(
						Math.min(count, nParallelThreads),
						count,
						20,
						TimeUnit.SECONDS,
						queue
					);

					for (int i = start; i < start + count; i++) {
						if (i < dlna.getChildren().size()) {
							final DLNAResource child = dlna.getChildren().get(i);

							if (child != null) {
								tpe.execute(child);
								resources.add(child);
							} else {
								LOGGER.warn("null child at index {} in {}", i, systemName);
							}
						}
					}
					try {
						tpe.shutdown();
						tpe.awaitTermination(20, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						LOGGER.error("error while shutting down thread pool executor for " + systemName, e);
					}

					LOGGER.trace("End of analysis for " + systemName);
				}
			}
		}

		lastSearch = searchStr;
		return resources;
	}

	protected void refreshChildrenIfNeeded(String search) {
		if (isDiscovered() && shouldRefresh(search)) {
			refreshChildren(search);
			notifyRefresh();
		}
	}

	/**
	 * Update the last refresh time.
	 */
	protected void notifyRefresh() {
		setLastRefreshTime(System.currentTimeMillis());
		setUpdateId(getUpdateId() + 1);
		setSystemUpdateId(getSystemUpdateId() + 1);
	}

	final protected void discoverWithRenderer(RendererConfiguration renderer, int count, boolean forced, String searchStr) {
		// Discover children if it hasn't been done already
		if (!isDiscovered()) {
			if (configuration.getFolderLimit() && depthLimit()) {
				if (renderer.getRendererName().equalsIgnoreCase("Playstation 3") || renderer.isXBOX()) {
					LOGGER.info("Depth limit potentionally hit for " + getDisplayName());
				}

				if (defaultRenderer != null) {
					defaultRenderer.addFolderLimit(this);
				}
			}

			discoverChildren(searchStr);
			boolean ready;

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
				if (refreshChildren(searchStr)) {
					notifyRefresh();
				}
			} else {
				// if not, then the regular isRefreshNeeded/doRefreshChildren pair.
				if (shouldRefresh(searchStr)) {
					doRefreshChildren(searchStr);
					notifyRefresh();
				}
			}
		}
	}

	private boolean shouldRefresh(String searchStr) {
		return (searchStr == null && lastSearch != null) || 
		(searchStr !=null && !searchStr.equals(lastSearch)) ||
		isRefreshNeeded();
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

	/**
	 * Recursive function that searches for a given ID.
	 *
	 * @param searchId ID to search for.
	 * @param renderer
	 * @param count
	 * @return Item found, or null otherwise.
	 * @see #getId()
	 */
	public DLNAResource search(String searchId, int count, RendererConfiguration renderer, String searchStr) {
		if (getId() != null && searchId != null) {
			String[] indexPath = searchId.split("\\$", 2);
			if (getId().equals(indexPath[0])) {
				if (indexPath.length == 1 || indexPath[1].length() == 0) {
					return this;
				} else {
					discoverWithRenderer(renderer, count, false, searchStr);

					for (DLNAResource file : getChildren()) {
						DLNAResource found = file.search(indexPath[1], count, renderer, searchStr);
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

	public void discoverChildren(String str) {
		discoverChildren();
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

	public void doRefreshChildren(String search) {
		doRefreshChildren();
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

	public boolean refreshChildren(String search) {
		if (shouldRefresh(search)) {
			doRefreshChildren(search);
			return true;
		}

		return false;
	}

	/**
	 * @deprecated Use {@link #resolveFormat()} instead.
	 */
	@Deprecated
	protected void checktype() {
		resolveFormat();
	}

	/**
	 * Sets the resource's {@link net.pms.formats.Format} according to its filename
	 * if it isn't set already.
	 * @since 1.90.0
	 */
	protected void resolveFormat() {
		if (getFormat() == null) {
			setFormat(FormatFactory.getAssociatedFormat(getSystemName()));
		}

		if (getFormat() != null && getFormat().isUnknown()) {
			getFormat().setType(getSpecificType());
		}
	}

	/**
	 * Hook to lazily initialise immutable resources e.g. ISOs, zip files &amp;c.
	 *
	 * @since 1.90.0
	 * @see #resolve()
	 */
	protected void resolveOnce() { }

	/**
	 * Resolve events are hooks that allow DLNA resources to perform various forms
	 * of initialisation when navigated to or streamed i.e. they function as lazy
	 * constructors.
	 *
	 * This method is called by request handlers for a) requests for a stream
	 * or b) content directory browsing i.e. for potentially every request for a file or
	 * folder the renderer hasn't cached. Many resource types are immutable (e.g. playlists,
	 * zip files, DVD ISOs &amp;c.) and only need to respond to this event once.
	 * Most resource types don't "subscribe" to this event at all. This default implementation
	 * provides hooks for immutable resources and handles the event for resource types that
	 * don't care about it. The rest override this method and handle it accordingly. Currently,
	 * the only resource type that overrides it is {@link RealFile}.
	 *
	 * Note: resolving a resource once (only) doesn't prevent children being added to or
	 * removed from it (if supported). There are other mechanisms for that e.g.
	 * {@link #doRefreshChildren()} (see {@link Feed} for an example).
	 */
	public synchronized void resolve() {
		if (!resolved) {
			resolveOnce();
			// if resolve() isn't overridden, this file/folder is immutable
			// (or doesn't respond to resolve events, which amounts to the
			// same thing), so don't spam it with this event again.
			resolved = true;
		}
	}

	// Ditlew
	/**
	 * Returns the display name for the default renderer.
	 *
	 * @return The display name.
	 * @see #getDisplayName(RendererConfiguration)
	 */
	public String getDisplayName() {
		return getDisplayName(null);
	}

	/**
	 * Returns the DisplayName that is shown to the Renderer.
	 * Extra info might be appended depending on the settings, like item duration.
	 * This is based on {@link #getName()}.
	 *
	 * @param mediaRenderer Media Renderer for which to show information.
	 * @return String representing the item.
	 */
	public String getDisplayName(RendererConfiguration mediaRenderer) {
		if (displayName != null) { // cached
			return displayName;
		}

		displayName = getName();
		String subtitleFormat;
		String subtitleLanguage;
		boolean isNamedNoEncoding = false;
		if (
			this instanceof RealFile &&
			(
				configuration.isHideExtensions() ||
				configuration.isPrettifyFilenames()
			) &&
			!isFolder()
		) {
			if (configuration.isPrettifyFilenames()) {
				displayName = FileUtil.getFileNameWithRewriting(displayName);
			} else {
				displayName = FileUtil.getFileNameWithoutExtension(displayName);
			}
		}

		if (getPlayer() != null) {
			if (isNoName()) {
				displayName = "[" + getPlayer().name() + "]";
			} else {
				// Ditlew - WDTV Live don't show durations otherwise, and this is useful for finding the main title
				if (mediaRenderer != null && mediaRenderer.isShowDVDTitleDuration() && getMedia() != null && getMedia().getDvdtrack() > 0) {
					displayName += " - " + getMedia().getDurationString();
				}

				if (!configuration.isHideEngineNames()) {
					displayName += " [" + getPlayer().name() + "]";
				}
			}
		} else {
			if (isNoName()) {
				displayName = "[No encoding]";
				isNamedNoEncoding = true;
			} else if (nametruncate > 0) {
				displayName = displayName.substring(0, nametruncate).trim();
			}
		}

		if (
			isSrtFile() &&
			!isNamedNoEncoding &&
			(
				getMediaAudio() == null &&
				getMediaSubtitle() == null
			) && 
			(
				getPlayer() == null ||
				getPlayer().isExternalSubtitlesSupported()
			) &&
			!configuration.hideSubInfo()
		) {
			displayName += " {External Subtitles}";
		}

		if (getMediaAudio() != null) {
			String audioLanguage = "/" + getMediaAudio().getLangFullName();
			if ("/Undetermined".equals(audioLanguage)) {
				audioLanguage = "";
			}

			displayName = (getPlayer() != null ? ("[" + getPlayer().name() + "]") : "") + " {Audio: " + getMediaAudio().getAudioCodec() + audioLanguage + ((getMediaAudio().getFlavor() != null && mediaRenderer != null && mediaRenderer.isShowAudioMetadata()) ? (" (" + getMediaAudio().getFlavor() + ")") : "") + "}";
		}

		if (
			getMediaSubtitle() != null && 
			getMediaSubtitle().getId() != -1 &&
			!configuration.hideSubInfo()
		) {
			subtitleFormat = getMediaSubtitle().getType().getDescription();
			if ("(Advanced) SubStation Alpha".equals(subtitleFormat)) {
				subtitleFormat = "SSA";
			}

			subtitleLanguage = "/" + getMediaSubtitle().getLangFullName();
			if ("/Undetermined".equals(subtitleLanguage)) {
				subtitleLanguage = "";
			}

			displayName += " {Sub: " + subtitleFormat + subtitleLanguage + ((getMediaSubtitle().getFlavor() != null && mediaRenderer != null && mediaRenderer.isShowSubMetadata()) ? (" (" + getMediaSubtitle().getFlavor() + ")") : "") + "}";
		}

		if (isAvisynth()) {
			displayName = (getPlayer() != null ? ("[" + getPlayer().name()) : "") + " + AviSynth]";
		}

		if (getSplitRange().isEndLimitAvailable()) {
			displayName = ">> " + StringUtil.convertTimeToString(getSplitRange().getStart(), StringUtil.DURATION_TIME_FORMAT);
		}

		return displayName;
	}

	/**
	 * Prototype for returning URLs.
	 *
	 * @return An empty URL
	 */
	protected String getFileURL() {
		return getURL("");
	}

	/**
	 * @return Returns a URL pointing to an image representing the item. If
	 * none is available, "thumbnail0000.png" is used.
	 */
	protected String getThumbnailURL() {
		return getURL("thumbnail0000");
	}

	/**
	 * @param prefix
	 * @return Returns a URL for a given media item. Not used for container types.
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

	/**
	 * Transforms a String to UTF-8.
	 *
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
	 * @return Number of children objects. This might be used in the DLDI
	 * response, as some renderers might not have enough memory to hold the
	 * list for all children.
	 */
	public int childrenNumber() {
		if (getChildren() == null) {
			return 0;
		}
		return getChildren().size();
	}
	/**
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */

	@Override
	protected DLNAResource clone() {
		DLNAResource o = null;
		try {
			o = (DLNAResource) super.clone();
			o.setId(null);

			// Clear the cached display name
			o.displayName = null;
			// Make sure clones (typically #--TRANSCODE--# folder files)
			// have the option to respond to resolve events
			o.resolved = false;
		} catch (CloneNotSupportedException e) {
			LOGGER.error(null, e);
		}

		return o;
	}

	// this shouldn't be public
	@Deprecated
	public String getFlags() {
		return getDlnaOrgOpFlags();
	}

	// permit the renderer to seek by time, bytes or both
	private String getDlnaOrgOpFlags() {
		return "DLNA.ORG_OP=" + dlnaOrgOpFlags;
	}

	/**
	 * @deprecated Use {@link #getDidlString(RendererConfiguration)} instead.
	 *
	 * @param mediaRenderer
	 * @return
	 */
	@Deprecated
	public final String toString(RendererConfiguration mediaRenderer) {
		return getDidlString(mediaRenderer);
	}

	/**
	 * Returns an XML (DIDL) representation of the DLNA node. It gives a
	 * complete representation of the item, with as many tags as available.
	 * Recommendations as per UPNP specification are followed where possible.
	 * 
	 * @param mediaRenderer
	 *            Media Renderer for which to represent this information. Useful
	 *            for some hacks.
	 * @return String representing the item. An example would start like this:
	 *         {@code <container id="0$1" childCount="1" parentID="0" restricted="true">}
	 */
	public final String getDidlString(RendererConfiguration mediaRenderer) {
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

		String wireshark = "";
		final DLNAMediaAudio firstAudioTrack = getMedia() != null ? getMedia().getFirstAudioTrack() : null;
		if (firstAudioTrack != null && isNotBlank(firstAudioTrack.getSongname())) {
			wireshark = firstAudioTrack.getSongname() + (getPlayer() != null && !configuration.isHideEngineNames() ? (" [" + getPlayer().name() + "]") : "");
			addXMLTagAndAttribute(
				sb,
				"dc:title",
				encodeXML(resumeStr(wireshark))
			);
		} else { // Ditlew - org
			// Ditlew
			wireshark = ((isFolder() || getPlayer() == null) ? getDisplayName() : mediaRenderer.getUseSameExtension(getDisplayName(mediaRenderer)));
			String tmp = (isFolder() || getPlayer() == null) ? getDisplayName() : mediaRenderer.getUseSameExtension(getDisplayName(mediaRenderer));
			addXMLTagAndAttribute(
				sb,
				"dc:title",
				encodeXML(resumeStr(tmp))
			);
		}

		if (firstAudioTrack != null) {
			if (isNotBlank(firstAudioTrack.getAlbum())) {
				addXMLTagAndAttribute(sb, "upnp:album", encodeXML(firstAudioTrack.getAlbum()));
			}

			if (isNotBlank(firstAudioTrack.getArtist())) {
				addXMLTagAndAttribute(sb, "upnp:artist", encodeXML(firstAudioTrack.getArtist()));
				addXMLTagAndAttribute(sb, "dc:creator", encodeXML(firstAudioTrack.getArtist()));
			}

			if (isNotBlank(firstAudioTrack.getGenre())) {
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

				/**
				 * DLNA.ORG_OP flags
				 *
				 * Two booleans (binary digits) which determine what transport operations the renderer is allowed to
				 * perform (in the form of HTTP request headers): the first digit allows the renderer to send
				 * TimeSeekRange.DLNA.ORG (seek by time) headers; the second allows it to send RANGE (seek by byte)
				 * headers.
				 *
				 *    00 - no seeking (or even pausing) allowed
				 *    01 - seek by byte
				 *    10 - seek by time
				 *    11 - seek by both
				 *
				 * See here for an example of how these options can be mapped to keys on the renderer's controller:
				 * http://www.ps3mediaserver.org/forum/viewtopic.php?f=2&t=2908&p=12550#p12550
				 *
				 * Note that seek-by-byte is the preferred option for streamed files [1] and seek-by-time is the
				 * preferred option for transcoded files.
				 *
				 * [1] see http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15841&p=76201#p76201
				 *
				 * seek-by-time requires a) support by the renderer (via the SeekByTime renderer conf option)
				 * and b) support by the transcode engine.
				 *
				 * The seek-by-byte fallback doesn't work well with transcoded files [2], but it's better than
				 * disabling seeking (and pausing) altogether.
				 *
				 * [2] http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=3507&p=16567#p16567 (bottom post)
				 */
				dlnaOrgOpFlags = "01"; // seek by byte (exclusive)

				if (mediaRenderer.isSeekByTime() && getPlayer() != null && getPlayer().isTimeSeekable()) {
					/**
					 * Some renderers - e.g. the PS3 and Panasonic TVs - behave erratically when
					 * transcoding if we keep the default seek-by-byte permission on when permitting
					 * seek-by-time: http://www.ps3mediaserver.org/forum/viewtopic.php?f=6&t=15841
					 *
					 * It's not clear if this is a bug in the DLNA libraries of these renderers or a bug
					 * in UMS, but setting an option in the renderer conf that disables seek-by-byte when
					 * we permit seek-by-time - e.g.:
					 *
					 *    SeekByTime = exclusive
					 *
					 * works around it.
					 */

					/**
					 * TODO (e.g. in a beta release): set seek-by-time (exclusive) here for *all* renderers:
					 * seek-by-byte isn't needed here (both the renderer and the engine support seek-by-time)
					 * and may be buggy on other renderers than the ones we currently handle.
					 *
					 * In the unlikely event that a renderer *requires* seek-by-both here, it can
					 * opt in with (e.g.):
					 *
					 *    SeekByTime = both
					 */
					if (mediaRenderer.isSeekByTimeExclusive()) {
						dlnaOrgOpFlags = "10"; // seek by time (exclusive)
					} else {
						dlnaOrgOpFlags = "11"; // seek by both
					}
				}

				addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");

				// FIXME: There is a flaw here. In addChild(DLNAResource) the mime type
				// is determined for the default renderer. This renderer may rewrite the
				// mime type based on its configuration. Looking up that mime type is
				// not guaranteed to return a match for another renderer.
				String mime = mediaRenderer.getMimeType(mimeType());

				if (mime == null) {
					// FIXME: Setting the default to "video/mpeg" leaves a lot of audio files in the cold.
					mime = "video/mpeg";
				}

				dlnaspec = null;

				if (mediaRenderer.isDLNAOrgPNUsed()) {
					if (mediaRenderer.isPS3()) {
						if (mime.equals("video/x-divx")) {
							dlnaspec = "DLNA.ORG_PN=AVI";
						} else if (mime.equals("video/x-ms-wmv") && getMedia() != null && getMedia().getHeight() > 700) {
							dlnaspec = "DLNA.ORG_PN=WMVHIGH_PRO";
						}
					} else {
						if (mime.equals("video/mpeg")) {
							dlnaspec = "DLNA.ORG_PN=" + getMPEG_PS_PALLocalizedValue(c);

							if (getPlayer() != null) {
								boolean isFileMPEGTS = TsMuxeRVideo.ID.equals(getPlayer().id()) || VideoLanVideoStreaming.ID.equals(getPlayer().id());
								boolean isMuxableResult = getMedia().isMuxable(mediaRenderer);
								boolean isBravia = mediaRenderer.isBRAVIA();
								if (
									!isFileMPEGTS &&
									(
										(
											configuration.isMencoderMuxWhenCompatible() &&
											MEncoderVideo.ID.equals(getPlayer().id())
										) ||
										(
											configuration.isFFmpegMuxWhenCompatible() &&
											FFMpegVideo.ID.equals(getPlayer().id())
										)
									)
								) {
									if (isBravia) {
										/**
										 * Sony Bravia TVs (and possibly other renderers) need ORG_PN to be accurate.
										 * If the value does not match the media, it won't play the media.
										 * Often we can lazily predict the correct value to send, but due to
										 * MEncoder needing to mux via tsMuxeR, we need to work it all out
										 * before even sending the file list to these devices.
										 * This is very time-consuming so we should a) avoid using this
										 * chunk of code whenever possible, and b) design a better system.
										 * Ideally we would just mux to MPEG-PS instead of MPEG-TS so we could
										 * know it will always be PS, but most renderers will not accept H.264
										 * inside MPEG-PS. Another option may be to always produce MPEG-TS
										 * instead and we should check if that will be OK for all renderers.
										 *
										 * This code block comes from Player.setAudioAndSubs()
										 */
										boolean finishedMatchingPreferences = false;
										OutputParams params = new OutputParams(configuration);
										if (getMedia() != null) {
											// check for preferred audio
											StringTokenizer st = new StringTokenizer(configuration.getAudioLanguages(), ",");
											while (st != null && st.hasMoreTokens()) {
												String lang = st.nextToken();
												lang = lang.trim();
												LOGGER.trace("Looking for an audio track with lang: " + lang);
												for (DLNAMediaAudio audio : getMedia().getAudioTracksList()) {
													if (audio.matchCode(lang)) {
														params.aid = audio;
														LOGGER.trace("Matched audio track: " + audio);
														st = null;
														break;
													}
												}
											}
										}

										if (params.aid == null && getMedia().getAudioTracksList().size() > 0) {
											// Take a default audio track, dts first if possible
											for (DLNAMediaAudio audio : getMedia().getAudioTracksList()) {
												if (audio.isDTS()) {
													params.aid = audio;
													LOGGER.trace("Found priority audio track with DTS: " + audio);
													break;
												}
											}

											if (params.aid == null) {
												params.aid = getMedia().getAudioTracksList().get(0);
												LOGGER.trace("Chose a default audio track: " + params.aid);
											}
										}

										String currentLang = null;
										DLNAMediaSubtitle matchedSub = null;

										if (params.aid != null) {
											currentLang = params.aid.getLang();
										}

										if (params.sid != null && params.sid.getId() == -1) {
											LOGGER.trace("Don't want subtitles!");
											params.sid = null;
											setMediaSubtitle(params.sid);
											finishedMatchingPreferences = true;
										}

										if (!finishedMatchingPreferences && params.sid != null && !StringUtils.isEmpty(params.sid.getLiveSubURL())) {
											// live subtitles
											// currently only open subtitles
											LOGGER.debug("Live subtitles " + params.sid.getLiveSubURL());
											try {
												matchedSub = params.sid;
												String file = OpenSubtitle.fetchSubs(matchedSub.getLiveSubURL(), matchedSub.getLiveSubFile());
												if (!StringUtils.isEmpty(file)) {
													matchedSub.setExternalFile(new File(file));
													params.sid = matchedSub;
													setMediaSubtitle(params.sid);
													finishedMatchingPreferences = true;
												}
											} catch (IOException e) {
											}
										}

										if (!finishedMatchingPreferences) {
											StringTokenizer st1 = new StringTokenizer(configuration.getAudioSubLanguages(), ";");

											boolean matchedEmbeddedSubtitle = false;
											while (st1.hasMoreTokens()) {
												String pair = st1.nextToken();
												if (pair.contains(",")) {
													String audio = pair.substring(0, pair.indexOf(","));
													String sub = pair.substring(pair.indexOf(",") + 1);
													audio = audio.trim();
													sub = sub.trim();
													LOGGER.trace("Searching for a match for: " + currentLang + " with " + audio + " and " + sub);

													if (Iso639.isCodesMatching(audio, currentLang) || (currentLang != null && audio.equals("*"))) {
														if (sub.equals("off")) {
															matchedSub = new DLNAMediaSubtitle();
															matchedSub.setLang("off");
														} else {
															for (DLNAMediaSubtitle present_sub : getMedia().getSubtitleTracksList()) {
																if (present_sub.matchCode(sub) || sub.equals("*")) {
																	if (present_sub.getExternalFile() != null) {
																		if (configuration.isAutoloadExternalSubtitles()) {
																			// Subtitle is external and we want external subtitles, look no further
																			matchedSub = present_sub;
																			LOGGER.trace(" Found a match: " + matchedSub);
																			break;
																		} else {
																			// Subtitle is external but we do not want external subtitles, keep searching
																			LOGGER.trace(" External subtitle ignored because of user setting: " + present_sub);
																		}
																	} else {
																		matchedSub = present_sub;
																		LOGGER.trace(" Found a match: " + matchedSub);
																		if (configuration.isAutoloadExternalSubtitles()) {
																			// Subtitle is internal and we will wait to see if an external one is available instead
																			matchedEmbeddedSubtitle = true;
																		} else {
																			// Subtitle is internal and we will use it
																			break;
																		}
																	}
																}
															}
														}

														if (matchedSub != null && !matchedEmbeddedSubtitle) {
															break;
														}
													}
												}
											}

											if (matchedSub != null && params.sid == null) {
												if (configuration.isDisableSubtitles() || (matchedSub.getLang() != null && matchedSub.getLang().equals("off"))) {
													LOGGER.trace(" Disabled the subtitles: " + matchedSub);
												} else {
													params.sid = matchedSub;
													setMediaSubtitle(params.sid);
												}
											}

											if (!configuration.isDisableSubtitles() && params.sid == null && getMedia() != null) {
												// Check for subtitles again
												File video = new File(getSystemName());
												FileUtil.isSubtitlesExists(video, getMedia(), false);

												if (configuration.isAutoloadExternalSubtitles()) {
													boolean forcedSubsFound = false;
													// Priority to external subtitles
													for (DLNAMediaSubtitle sub : getMedia().getSubtitleTracksList()) {
														if (matchedSub != null && matchedSub.getLang() != null && matchedSub.getLang().equals("off")) {
															StringTokenizer st = new StringTokenizer(configuration.getForcedSubtitleTags(), ",");

															while (sub.getFlavor() != null && st.hasMoreTokens()) {
																String forcedTags = st.nextToken();
																forcedTags = forcedTags.trim();

																if (
																	sub.getFlavor().toLowerCase().indexOf(forcedTags) > -1 &&
																	Iso639.isCodesMatching(sub.getLang(), configuration.getForcedSubtitleLanguage())
																) {
																	LOGGER.trace("Forcing preferred subtitles : " + sub.getLang() + "/" + sub.getFlavor());
																	LOGGER.trace("Forced subtitles track : " + sub);

																	if (sub.getExternalFile() != null) {
																		LOGGER.trace("Found external forced file : " + sub.getExternalFile().getAbsolutePath());
																	}
																	params.sid = sub;
																	setMediaSubtitle(params.sid);
																	forcedSubsFound = true;
																	break;
																}
															}
															if (forcedSubsFound == true) {
																break;
															}
														} else {
															LOGGER.trace("Found subtitles track: " + sub);

															if (sub.getExternalFile() != null) {
																LOGGER.trace("Found external file: " + sub.getExternalFile().getAbsolutePath());
																params.sid = sub;
																setMediaSubtitle(params.sid);
																break;
															}
														}
													}
												}
												if (
													matchedSub != null &&
													matchedSub.getLang() != null &&
													matchedSub.getLang().equals("off")
												) {
													finishedMatchingPreferences = true;
												}

												if (!finishedMatchingPreferences && params.sid == null) {
													StringTokenizer st = new StringTokenizer(configuration.getSubtitlesLanguages(), ",");
													while (st != null && st.hasMoreTokens()) {
														String lang = st.nextToken();
														lang = lang.trim();
														LOGGER.trace("Looking for a subtitle track with lang: " + lang);
														for (DLNAMediaSubtitle sub : getMedia().getSubtitleTracksList()) {
															if (
																sub.matchCode(lang) &&
																!(
																	!configuration.isAutoloadExternalSubtitles() &&
																	sub.getExternalFile() != null
																)
															) {
																params.sid = sub;
																LOGGER.trace("Matched sub track: " + params.sid);
																st = null;
																break;
															}
														}
													}
												}
											}
										}

										if (getMediaSubtitle() == null) {
											LOGGER.trace("We do not want a subtitle for " + getName());
										} else {
											LOGGER.trace("We do want a subtitle for " + getName());
										}
									}

									if (
										(
											getMediaSubtitle() == null &&
											!isSrtFile() &&
											getMedia() != null &&
											getMedia().getDvdtrack() == 0 &&
											isMuxableResult &&
											mediaRenderer.isMuxH264MpegTS()
										) ||
										mediaRenderer.isTranscodeToMPEGTSAC3()
									) {
										isFileMPEGTS = true;
									}
								}

								if (isFileMPEGTS) {
									dlnaspec = "DLNA.ORG_PN=" + getMPEG_TS_SD_EU_ISOLocalizedValue(c);
									if (
										getMedia().isH264() &&
										!VideoLanVideoStreaming.ID.equals(getPlayer().id()) &&
										isMuxableResult
									) {
										dlnaspec = "DLNA.ORG_PN=AVC_TS_HD_24_AC3_ISO";
									}
								}
							} else if (getMedia() != null) {
								if (getMedia().isMpegTS()) {
									dlnaspec = "DLNA.ORG_PN=" + getMPEG_TS_SD_EULocalizedValue(c);
									if (getMedia().isH264()) {
										dlnaspec = "DLNA.ORG_PN=AVC_TS_HD_50_AC3";
									}
								}
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
				}

				String tempString = "http-get:*:" + mime + ":" + (dlnaspec != null ? (dlnaspec + ";") : "") + getDlnaOrgOpFlags();
				wireshark = wireshark + " " + tempString;
				addAttribute(sb, "protocolInfo", tempString);

				if (getFormat() != null && getFormat().isVideo() && getMedia() != null && getMedia().isMediaparsed()) {
					if (getPlayer() == null && getMedia() != null) {
						wireshark = wireshark + " " + "size=" + getMedia().getSize();
						addAttribute(sb, "size", getMedia().getSize());
					} else {
						long transcoded_size = mediaRenderer.getTranscodedSize();
						if (transcoded_size != 0) {
							wireshark = wireshark + " " + "size=" + transcoded_size;
							addAttribute(sb, "size", transcoded_size);
						}
					}
					if (getMedia().getDuration() != null) {
						if (getSplitRange().isEndLimitAvailable()) {
							wireshark = wireshark + " " + "duration=" + StringUtil.convertTimeToString(getSplitRange().getDuration(), StringUtil.DURATION_TIME_FORMAT);
							addAttribute(sb, "duration", StringUtil.convertTimeToString(getSplitRange().getDuration(), StringUtil.DURATION_TIME_FORMAT));
						} else {
							wireshark = wireshark + " " + "duration=" + getMedia().getDurationString();
							addAttribute(sb, "duration", getMedia().getDurationString());
						}
					}
					if (getMedia().getResolution() != null) {
						addAttribute(sb, "resolution", getMedia().getResolution());
					}
					addAttribute(sb, "bitrate", getMedia().getRealVideoBitrate());
					if (firstAudioTrack != null) {
						if (firstAudioTrack.getAudioProperties().getNumberOfChannels() > 0) {
							addAttribute(sb, "nrAudioChannels", firstAudioTrack.getAudioProperties().getNumberOfChannels());
						}
						if (firstAudioTrack.getSampleFrequency() != null) {
							addAttribute(sb, "sampleFrequency", firstAudioTrack.getSampleFrequency());
						}
					}
				} else if (getFormat() != null && getFormat().isImage()) {
					if (getMedia() != null && getMedia().isMediaparsed()) {
						wireshark = wireshark + " " + "size=" + getMedia().getSize();
						addAttribute(sb, "size", getMedia().getSize());
						if (getMedia().getResolution() != null) {
							addAttribute(sb, "resolution", getMedia().getResolution());
						}
					} else {
						wireshark = wireshark + " " + "size=" + length();
						addAttribute(sb, "size", length());
					}
				} else if (getFormat() != null && getFormat().isAudio()) {
					if (getMedia() != null && getMedia().isMediaparsed()) {
						addAttribute(sb, "bitrate", getMedia().getBitrate());
						if (getMedia().getDuration() != null) {
							wireshark = wireshark + " " + "duration=" + StringUtil.convertTimeToString(getMedia().getDuration(), StringUtil.DURATION_TIME_FORMAT);
							addAttribute(sb, "duration", StringUtil.convertTimeToString(getMedia().getDuration(), StringUtil.DURATION_TIME_FORMAT));
						}
						if (firstAudioTrack != null && firstAudioTrack.getSampleFrequency() != null) {
							addAttribute(sb, "sampleFrequency", firstAudioTrack.getSampleFrequency());
						}
						if (firstAudioTrack != null) {
							addAttribute(sb, "nrAudioChannels", firstAudioTrack.getAudioProperties().getNumberOfChannels());
						}

						if (getPlayer() == null) {
							wireshark = wireshark + " " + "size=" + getMedia().getSize();
							addAttribute(sb, "size", getMedia().getSize());
						} else {
							// Calculate WAV size
							if (firstAudioTrack != null) {
								int defaultFrequency = mediaRenderer.isTranscodeAudioTo441() ? 44100 : 48000;
								if (!configuration.isAudioResample()) {
									try {
										// FIXME: Which exception could be thrown here?
										defaultFrequency = firstAudioTrack.getSampleRate();
									} catch (Exception e) {
										LOGGER.debug("Caught exception", e);
									}
								}
								int na = firstAudioTrack.getAudioProperties().getNumberOfChannels();
								if (na > 2) { // No 5.1 dump in MPlayer
									na = 2;
								}
								int finalSize = (int) (getMedia().getDurationInSeconds() * defaultFrequency * 2 * na);
								LOGGER.trace("Calculated size for " + getSystemName() + ": " + finalSize);
								wireshark = wireshark + " " + "size=" + finalSize;
								addAttribute(sb, "size", finalSize);
							}
						}
					} else {
						wireshark = wireshark + " " + "size=" + length();
						addAttribute(sb, "size", length());
					}
				} else {
					wireshark = wireshark + " " + "size=" + DLNAMediaInfo.TRANS_SIZE + " duration=" + "09:59:59";
					addAttribute(sb, "size", DLNAMediaInfo.TRANS_SIZE);
					addAttribute(sb, "duration", "09:59:59");
					addAttribute(sb, "bitrate", "1000000");
				}
				endTag(sb);
				wireshark = wireshark + " " + getFileURL();
				LOGGER.trace("Network debugger: " + wireshark);
				wireshark = "";
				sb.append(getFileURL());
				closeTag(sb, "res");
			}
		}

		appendThumbnail(mediaRenderer, sb);

		if (getLastModified() > 0) {
			addXMLTagAndAttribute(sb, "dc:date", SDF_DATE.format(new Date(getLastModified())));
		}

		String uclass;
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
			} else if (getFormat() != null && getFormat().isVideo()) {
				uclass = "object.item.videoItem";
			} else if (getFormat() != null && getFormat().isImage()) {
				uclass = "object.item.imageItem.photo";
			} else if (getFormat() != null && getFormat().isAudio()) {
				uclass = "object.item.audioItem.musicTrack";
			} else {
				uclass = "object.item.videoItem";
			}
		}
		addXMLTagAndAttribute(sb, "upnp:class", uclass);

		if (isFolder()) {
			closeTag(sb, "container");
		} else {
			closeTag(sb, "item");
		}
		return sb.toString();
	}

	/**
	 * Generate and append the response for the thumbnail based on the
	 * configuration of the renderer.
	 *
	 * @param mediaRenderer The renderer configuration.
	 * @param sb The StringBuilder to append the response to.
	 */
	private void appendThumbnail(RendererConfiguration mediaRenderer, StringBuilder sb) {
		final String thumbURL = getThumbnailURL();

		if (isNotBlank(thumbURL)) {
			if (mediaRenderer.getThumbNailAsResource()) {
				// Samsung 2012 (ES and EH) models do not recognize the "albumArtURI" element. Instead,
				// the "res" element should be used.
				// Also use "res" when faking JPEG thumbs.
				openTag(sb, "res");

				if (getThumbnailContentType().equals(PNG_TYPEMIME) && !mediaRenderer.isForceJPGThumbnails()) {
					addAttribute(sb, "protocolInfo", "http-get:*:image/png:DLNA.ORG_PN=PNG_TN");
				} else {
					addAttribute(sb, "protocolInfo", "http-get:*:image/jpeg:DLNA.ORG_PN=JPEG_TN");
				}

				endTag(sb);
				sb.append(thumbURL);
				closeTag(sb, "res");
			} else {
				// Renderers that can handle the "albumArtURI" element.
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
		}
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
						InetAddress rendererIp;
						try {
							rendererIp = InetAddress.getByName(rendererId);
							RendererConfiguration renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(rendererIp);
							String rendererName = "unknown renderer";
							try {
								rendererName = renderer.getRendererName();
							} catch (NullPointerException e) { }
							LOGGER.info("Started playing " + getName() + " on your " + rendererName);
							LOGGER.debug("The full filename of which is: " + getSystemName() + " and the address of the renderer is: " + rendererId);
						} catch (UnknownHostException ex) {
							LOGGER.debug("" + ex);
						}

						startTime = System.currentTimeMillis();

						for (final ExternalListener listener : ExternalFactory.getExternalListeners()) {
							if (listener instanceof StartStopListener) {
								// run these asynchronously for slow handlers (e.g. logging, scrobbling)
								Runnable fireStartStopEvent = new Runnable() {
									@Override
									public void run() {
										try {
											((StartStopListener) listener).nowPlaying(getMedia(), self);
										} catch (Throwable t) {
											LOGGER.error("Notification of startPlaying event failed for StartStopListener {}", listener.getClass(), t);
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
								InetAddress rendererIp;
								try {
									rendererIp = InetAddress.getByName(rendererId);
									RendererConfiguration renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(rendererIp);
									String rendererName = "unknown renderer";
									try {
										rendererName = renderer.getRendererName();
									} catch (NullPointerException e) { }
									LOGGER.info("Stopped playing " + getName() + " on your " + rendererName);
									LOGGER.debug("The full filename of which is: " + getSystemName() + " and the address of the renderer is: " + rendererId);
								} catch (UnknownHostException ex) {
									LOGGER.debug("" + ex);
								}

								// Initiate the code that figures out whether to create a resume item
								if (getMedia() != null) {
									long durSec = (long) getMedia().getDurationInSeconds();
									if (externalProcess != null && (durSec == 0 || durSec == DLNAMediaInfo.TRANS_SIZE)) {
										ProcessWrapperImpl pw = (ProcessWrapperImpl) externalProcess;
										String dur = pw.getDuration();
										if (StringUtils.isNotEmpty(dur)) {
											getMedia().setDuration(convertStringToTime(dur));
										}
									}
								}
								resumeStop();

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
													LOGGER.error("Notification of donePlaying event failed for StartStopListener {}", listener.getClass(), t);
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
				// asking for an invalid offset which kills MEncoder
				if (timeRange.getStartOrZero() > getMedia().getDurationInSeconds()) {
					return null;
				}

				// Should we rewind a little (in case our overhead isn't accurate enough)
				int rewind_secs = mediarenderer.getByteToTimeseekRewindSeconds();
				timeRange.rewindStart(rewind_secs);

				// shagrath:
				timeseek_auto = true;
			}
		}

		// Determine source of the stream
		if (getPlayer() == null && !isResume()) {
			// No transcoding
			if (this instanceof IPushOutput) {
				PipedOutputStream out = new PipedOutputStream();
				InputStream fis = new PipedInputStream(out);
				((IPushOutput) this).push(out);

				if (low > 0) {
					fis.skip(low);
				}
				// http://www.ps3mediaserver.org/forum/viewtopic.php?f=11&t=12035
				fis = wrap(fis, high, low);

				return fis;
			}

			InputStream fis;
			if (getFormat() != null && getFormat().isImage() && getMedia() != null && getMedia().getOrientation() > 1 && mediarenderer.isAutoRotateBasedOnExif()) {
				// seems it's a jpeg file with an orientation setting to take care of
				fis = ImagesUtil.getAutoRotateInputStreamImage(getInputStream(), getMedia().getOrientation());
				if (fis == null) { // error, let's return the original one
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
			// Pipe transcoding result
			OutputParams params = new OutputParams(configuration);
			params.aid = getMediaAudio();
			params.sid = getMediaSubtitle();
			params.header = getHeaders();
			params.mediaRenderer = mediarenderer;
			timeRange.limit(getSplitRange());
			params.timeseek = timeRange.getStartOrZero();
			params.timeend = timeRange.getEndOrZero();
			params.shift_scr = timeseek_auto;

			if (this instanceof IPushOutput) {
				params.stdin = (IPushOutput) this;
			}

			if (resume != null) {
				params.timeseek += (long) (resume.getTimeOffset() / 1000);
				if (getPlayer() == null) {
					setPlayer(new ResumePlayer());
				}
			}

			// (Re)start transcoding process if necessary
			if (externalProcess == null || externalProcess.isDestroyed()) {
				// First playback attempt => start new transcoding process
				LOGGER.debug("Starting transcode/remux of " + getName() + " with media info: " + getMedia().toString());

				externalProcess = getPlayer().launchTranscode(this, getMedia(), params);

				if (params.waitbeforestart > 0) {
					LOGGER.trace("Sleeping for {} milliseconds", params.waitbeforestart);
					try {
						Thread.sleep(params.waitbeforestart);
					} catch (InterruptedException e) {
						LOGGER.error(null, e);
					}
					LOGGER.trace("Finished sleeping for " + params.waitbeforestart + " milliseconds");
				}
			} else if (
				params.timeseek > 0 &&
				getMedia() != null &&
				getMedia().isMediaparsed() &&
				getMedia().getDurationInSeconds() > 0
			) {
				// Time seek request => stop running transcode process and start a new one
				LOGGER.debug("Requesting time seek: " + params.timeseek + " seconds");
				params.minBufferSize = 1;
				Runnable r = new Runnable() {
					@Override
					public void run() {
						externalProcess.stopProcess();
					}
				};
				new Thread(r, "External Process Stopper").start();
				ProcessWrapper newExternalProcess = getPlayer().launchTranscode(this, getMedia(), params);
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
					LOGGER.debug("External input stream instance is null... sounds not good, waiting 500ms");
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
						LOGGER.error("External input stream instance is null... stopping process");
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
			// FIXME: This cannot be right. A player like FFmpeg can output many
			// formats depending on the media and the renderer. Also, players are
			// singletons. Therefore it is impossible to have exactly one mime
			// type to return.
			return getPlayer().mimeType();
		} else if (getMedia() != null && getMedia().isMediaparsed()) {
			return getMedia().getMimeType();
		} else if (getFormat() != null) {
			return getFormat().mimeType();
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

	/**
	 * Checks if a thumbnail exists, and, if not, generates one (if possible).
	 * Called from Request/RequestV2 in response to thumbnail requests e.g. HEAD /get/0$1$0$42$3/thumbnail0000%5BExample.mkv
	 * Calls DLNAMediaInfo.generateThumbnail, which in turn calls DLNAMediaInfo.parse.
	 *
	 * @param inputFile File to check or generate the thumbnail for.
	 */
	protected void checkThumbnail(InputFile inputFile) {
		if (getMedia() != null && !getMedia().isThumbready() && configuration.isThumbnailGenerationEnabled()) {
			getMedia().setThumbready(true);
			getMedia().generateThumbnail(inputFile, getFormat(), getType());
			if (getMedia().getThumb() != null && configuration.getUseCache() && inputFile.getFile() != null) {
				PMS.get().getDatabase().updateThumbnail(inputFile.getFile().getAbsolutePath(), inputFile.getFile().lastModified(), getType(), getMedia());
			}
		}
	}

	/**
	 * Returns the input stream for this resource's generic thumbnail,
	 * which is the first of:
	 *          - its Format icon, if any
	 *          - the fallback image, if any
	 *          - the default video icon
	 *
	 * @param fallback
	 *            the fallback image, or null.
	 *
	 * @return The InputStream
	 * @throws IOException
	 */
	public InputStream getGenericThumbnailInputStream(String fallback) throws IOException {
		String thumb = fallback;
		if (getFormat() != null && getFormat().getIcon() != null) {
			thumb = getFormat().getIcon();
		}

		// Thumb could be:
		if (thumb != null) {
			// A local file
			if (new File(thumb).exists()) {
				return new FileInputStream(thumb);
			}

			// A jar resource
			InputStream is;
			if ((is = getResourceInputStream(thumb)) != null) {
				return is;
			}

			// A URL
			try {
				return downloadAndSend(thumb, true);
			} catch (Exception e) {}
		}

		// Or none of the above
		return getResourceInputStream("images/thumbnail-video-256.png");
	}

	/**
	 * Returns the input stream for this resource's thumbnail
	 * (or a default image if a thumbnail can't be found).
	 * Typically overridden by a subclass.
	 *
	 * @return The InputStream
	 * @throws IOException
	 */
	public InputStream getThumbnailInputStream() throws IOException {
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
			return getResourceInputStream("/images/codes/" + code + ".png");
		}

		if (isAvisynth()) {
			return getResourceInputStream("/images/logo-avisynth.png");
		}
		return getGenericThumbnailInputStream(null);
	}

	public String getThumbnailContentType() {
		return HTTPResource.JPEG_TYPEMIME;
	}

	public int getType() {
		if (getFormat() != null) {
			return getFormat().getType();
		} else {
			return Format.UNKNOWN;
		}
	}

	/**
	 * Prototype function.
	 *
	 * @return true if child can be added to other folder.
	 * @see #addChild(DLNAResource)
	 */
	public abstract boolean isValid();

	public boolean allowScan() {
		return false;
	}

	/**
	 * (non-Javadoc)
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
		result.append(", ext=");
		result.append(getFormat());
		result.append(", discovered=");
		result.append(isDiscovered());
		result.append("]");
		return result.toString();
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
	public Format getFormat() {
		return format;
	}

	/**
	 * Sets the {@link Format} of this resource, thereby defining its capabilities.
	 *
	 * @param format The format to set.
	 */
	protected void setFormat(Format format) {
		this.format = format;

		// Set deprecated variable for backwards compatibility
		ext = format;
	}
 
	/**
	 * @deprecated Use {@link #getFormat()} instead.
	 *
	 * @return The format of this resource.
	 */
	@Deprecated
	public Format getExt() {
		return getFormat();
	}

	/**
	 * @deprecated Use {@link #setFormat(Format)} instead.
	 *
	 * @param format The format to set.
	 */
	@Deprecated
	protected void setExt(Format format) {
		setFormat(format);
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
	 * the audio specifics. A resource can have many audio tracks, this method
	 * returns the one that should be played.
	 *
	 * @return The audio object containing detailed information.
	 * @since 1.50
	 */
	public DLNAMediaAudio getMediaAudio() {
		return media_audio;
	}

	/**
	 * Sets the {@link DLNAMediaAudio} object for this resource that contains
	 * the audio specifics. A resource can have many audio tracks, this method
	 * determines the one that should be played.
	 *
	 * @param mediaAudio The audio object containing detailed information.
	 * @since 1.50
	 */
	protected void setMediaAudio(DLNAMediaAudio mediaAudio) {
		this.media_audio = mediaAudio;
	}

	/**
	 * Returns the {@link DLNAMediaSubtitle} object for this resource that
	 * contains the specifics for the subtitles. A resource can have many
	 * subtitles, this method returns the one that should be displayed.
	 *
	 * @return The subtitle object containing detailed information.
	 * @since 1.50
	 */
	public DLNAMediaSubtitle getMediaSubtitle() {
		return media_subtitle;
	}

	/**
	 * Sets the {@link DLNAMediaSubtitle} object for this resource that
	 * contains the specifics for the subtitles. A resource can have many
	 * subtitles, this method determines the one that should be used.
	 *
	 * @param mediaSubtitle The subtitle object containing detailed information.
	 * @since 1.50
	 */
	protected void setMediaSubtitle(DLNAMediaSubtitle mediaSubtitle) {
		this.media_subtitle = mediaSubtitle;
	}

	/**
	 * @deprecated Use {@link #getLastModified()} instead.
	 *
	 * Returns the timestamp at which this resource was last modified.
	 *
	 * @return The timestamp.
	 */
	@Deprecated
	public long getLastmodified() {
		return getLastModified();
	}

	/**
	 * Returns the timestamp at which this resource was last modified.
	 *
	 * @return The timestamp.
	 * @since 1.71.0
	 */
	public long getLastModified() {
		return lastmodified; // TODO rename lastmodified -> lastModified
	}

	/**
	 * @deprecated Use {@link #setLastModified(long)} instead.
	 *
	 * Sets the timestamp at which this resource was last modified.
	 *
	 * @param lastModified The timestamp to set.
	 * @since 1.50
	 */
	@Deprecated
	protected void setLastmodified(long lastModified) {
		setLastModified(lastModified);
	}

	/**
	 * Sets the timestamp at which this resource was last modified.
	 *
	 * @param lastModified The timestamp to set.
	 * @since 1.71.0
	 */
	protected void setLastModified(long lastModified) {
		this.lastmodified = lastModified; // TODO rename lastmodified -> lastModified
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
	 * Sets whether or not this is a nameless resource. This is particularly
	 * useful in the virtual TRANSCODE folder for a file, where the same file
	 * is copied many times with different audio and subtitle settings. In that
	 * case the name of the file becomes irrelevant and only the settings
	 * need to be shown.
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
	 * @deprecated use {@link #getLastChildId()} instead.
	 */
	@Deprecated
	protected int getLastChildrenId() {
		return getLastChildId();
	}

	/**
	 * Returns the numerical ID of the last child added.
	 *
	 * @return The ID.
	 * @since 1.80.0
	 */
	protected int getLastChildId() {
		return lastChildrenId;
	}

	/**
	 * @deprecated use {@link #setLastChildId(int)} instead.
	 */
	@Deprecated
	protected void setLastChildrenId(int lastChildId) {
		setLastChildId(lastChildId);
	}

	/**
	 * Sets the numerical ID of the last child added.
	 *
	 * @param lastChildId The ID to set.
	 * @since 1.80.0
	 */
	protected void setLastChildId(int lastChildId) {
		this.lastChildrenId = lastChildId;
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

	private static final int DEPTH_WARNING_LIMIT=7;

	private boolean depthLimit() {
		DLNAResource tmp = this;
		int depth = 0;
		while (tmp != null) {
			tmp = tmp.getParent();
			depth++;
		}
		return (depth > DEPTH_WARNING_LIMIT);
	}

	public boolean isSearched() {
		return false;
	}

	public byte[] getHeaders() {
		return null;
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

	////////////////////////////////////////////////////
	// Subtitle handling
	////////////////////////////////////////////////////

	private SubSelect getSubSelector(boolean create) {
		if (
			configuration.isDisableSubtitles() ||
			!configuration.isAutoloadExternalSubtitles() ||
			configuration.isHideLiveSubtitlesFolder()
		) {
			return null;
		}

		// Search for transcode folder
		for (DLNAResource r : getChildren()) {
			if (r instanceof SubSelect) {
				return (SubSelect) r;
			}
		}
		if (create) {
			SubSelect vf = new SubSelect();
			addChildInternal(vf);
			return vf;
		}
		return null;
	}

	public boolean isSubSelectable() {
		return false;
	}

	private boolean liveSubs(DLNAResource r) {
		DLNAMediaSubtitle s = r.getMediaSubtitle();
		if (s != null) {
			return StringUtils.isNotEmpty(s.getLiveSubURL());
		}
		return false;
	}

	////////////////////////////////////////////////////
	// Resume handling
	////////////////////////////////////////////////////

	private ResumeObj resume;
	private int resHash;
	private long startTime;

	public int resumeHash() {
		return resHash;
	}

	public boolean isResumeable() {
		if (getFormat() != null) {
			// Only resume videos
			return getFormat().isVideo();
		}
		return true;
	}

	private void resumeStop() {
		if (!configuration.isResumeEnabled() || !isResumeable()) {
			return;
		}
		if (resume != null) {
			resume.stop(startTime, (long) (getMedia().getDurationInSeconds() * 1000));
			if (resume.isDone()) {
				getParent().getChildren().remove(this);
			}
			notifyRefresh();
		} else {
			for (DLNAResource res : getParent().getChildren()) {
				if (res.isResume() && res.getName().equals(getName())) {
					res.resume.stop(startTime, (long) (getMedia().getDurationInSeconds() * 1000));
					if (res.resume.isDone()) {
						getParent().getChildren().remove(res);
					}
					return;
				}
			}
			ResumeObj r = ResumeObj.store(this, startTime);
			if (r != null) {
				DLNAResource clone = this.clone();
				clone.resume = r;
				clone.resHash = resHash;
				clone.setMedia(getMedia());
				clone.setPlayer(getPlayer());
				getParent().addChildInternal(clone);
			}
		}
	}

	public final boolean isResume() {
		return isResumeable() && (resume != null);
	}

	public int minPlayTime() {
		return configuration.getMinPlayTime();
	}

	private String resumeStr(String s) {
		if (isResume()) {
			return Messages.getString("PMS.134") + ": " + s;
		} else {
			return s;
		}
	}
}
