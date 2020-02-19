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

import java.awt.RenderingHints;
import java.io.*;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.PmsConfiguration.SubtitlesInfoLevel;
import net.pms.configuration.RendererConfiguration;
import net.pms.database.TableFilesStatus;
import net.pms.database.TableThumbnails;
import net.pms.dlna.DLNAImageProfile.HypotheticalResult;
import net.pms.dlna.virtual.TranscodeVirtualFolder;
import net.pms.dlna.virtual.VirtualFolder;
import net.pms.dlna.virtual.VirtualVideoAction;
import net.pms.encoders.*;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageFormat;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.SizeLimitInputStream;
import net.pms.network.HTTPResource;
import net.pms.network.UPNPControl.Renderer;
import net.pms.util.*;
import static net.pms.util.StringUtil.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.pms.external.ExternalListener;

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
	private volatile ImageInfo thumbnailImageInfo = null;
	protected PmsConfiguration configuration = PMS.getConfiguration();
//	private boolean subsAreValidForStreaming = false;

	protected static final int MAX_ARCHIVE_ENTRY_SIZE = 10000000;
	protected static final int MAX_ARCHIVE_SIZE_SEEK = 800000000;

	public static final RenderingHints THUMBNAIL_HINTS = new RenderingHints(
		RenderingHints.KEY_RENDERING,
		RenderingHints.VALUE_RENDER_QUALITY
	);

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
	protected String pathId;

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

	private boolean isEpisodeWithinSeasonFolder = false;

	/**
	 * Represents the transformation to be used to the file. If null, then
	 *
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
	 * @deprecated Use #hasExternalSubtitles()
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

	/**
	 * @deprecated Use standard getter and setter to access this field.
	 *
	 * List of children objects associated with this DLNAResource. This is only valid when the DLNAResource is of the container type.
	 */
	@Deprecated
	protected DLNAList children;
	//protected List<DLNAResource> children;

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

	private VirtualFolder dynamicPls;

	protected HashMap<String, Object> attachments = null;

	/**
	 * Used to synchronize access to {@link #hasExternalSubtitles},
	 * {@link #hasSubtitles} and {@link #isExternalSubtitlesParsed}
	 */
	private final Object subtitlesLock = new Object();

	private boolean hasExternalSubtitles;
	private boolean hasSubtitles;
	private boolean isExternalSubtitlesParsed;

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
	 *
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
	public String getId() {
		return id;
	}

	/**
	 * Returns the integer representation of the id of this resource based
	 * on the index in its parent container.
	 *
	 * @return The id integer.
	 * @since 6.4.1
	 */
	public int getIntId() {
		return Integer.parseInt(getId());
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

	public String getPathId() {
		DLNAResource tmp = getParent();
		ArrayList<String> res = new ArrayList<>();
		res.add(getId());
		while (tmp != null) {
			res.add(0, tmp.getId());
			tmp = tmp.getParent();
		}
		pathId = StringUtils.join(res, '.');
		return pathId;
	}

	/**
	 * String representing this resource ID. This string is used by the UPNP
	 * ContentDirectory service. There is no hard spec on the actual numbering
	 * except for the root container that always has to be "0". In DMS the
	 * format used is <i>number($number)+</i>. A common client that expects a
	 * different format than the one used here is the XBox360. DMS translates
	 * the XBox360 queries on the fly. For more info, check
	 * <ul>
	 * <li><a href="http://www.mperfect.net/whsUpnp360/">whsUpnp360</a></li>
	 * <li><a href="https://code.google.com/archive/p/jems/wikis/XBox360Notes.wiki">jems - XBox360Notes.wiki</a></li>
	 * <li><a href="https://web-beta.archive.org/web/20100501042404/http://download.microsoft.com:80/download/0/0/b/00bba048-35e6-4e5b-a3dc-36da83cbb0d1/NetCompat_WMP11.docx">NetCompat_WMP11.docx</a></li>
	 * </ul>
	 *
	 * @return The resource id.
	 * @since 1.50
	 */
	public String getResourceId() {
		/*if (getId() == null) {
			return null;
		}

		if (parent != null) {
			return parent.getResourceId() + '$' + getId();
		} else {
			return getId();
		}*/
		if (isFolder() && configuration.getAutoDiscover()) {
			return getPathId();
		}
		return getId();
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
	 * Checks if is live subtitle folder available.
	 *
	 * @return true, if the live subtitle folder should be shown
	 */
	public boolean isLiveSubtitleFolderAvailable() {
		return true;
	}

	/**
	 * Any {@link DLNAResource} needs to represent the container or item with a String.
	 *
	 * @return String to be showed in the UPNP client.
	 */
	public abstract String getName();

	public abstract String getSystemName();

	/**
	 * @return The path to the media source.
	 */
	public String getFileName() {
		return getSystemName();
	}

	public abstract long length();

	// Ditlew
	public long length(RendererConfiguration mediaRenderer) {
		return length();
	}

	public abstract InputStream getInputStream() throws IOException;

	public abstract boolean isFolder();

	public String getDlnaContentFeatures(RendererConfiguration mediaRenderer) {
		// TODO: Determine renderer's correct localization value
		int localizationValue = 1;
		String dlnaOrgPnFlags = getDlnaOrgPnFlags(mediaRenderer, localizationValue);
		return (dlnaOrgPnFlags != null ? (dlnaOrgPnFlags + ";") : "") + getDlnaOrgOpFlags(mediaRenderer) + ";DLNA.ORG_CI=0;DLNA.ORG_FLAGS=01700000000000000000000000000000";
	}

	public String getDlnaContentFeatures(DLNAImageProfile profile, boolean thumbnailRequest) {
		StringBuilder sb = new StringBuilder();
		if (profile != null) {
			sb.append("DLNA.ORG_PN=").append(profile);
		}
		ImageInfo thumbnailImageInfo = this.thumbnailImageInfo != null ?
			this.thumbnailImageInfo :
			getMedia() != null && getMedia().getThumb() != null ?
				getMedia().getThumb().getImageInfo() :
				null;
		ImageInfo imageInfo = thumbnailRequest ?
			thumbnailImageInfo :
			media != null ?
				media.getImageInfo() :
				null;

		if (profile != null &&
			!thumbnailRequest &&
			thumbnailImageInfo != null &&
			profile.useThumbnailSource(imageInfo, thumbnailImageInfo)
		) {
			imageInfo = thumbnailImageInfo;
		}
		if (profile != null && imageInfo != null) {
			HypotheticalResult hypotheticalResult = profile.calculateHypotheticalProperties(imageInfo);
			if (sb.length() > 0) {
				sb.append(';');
			}
			sb.append("DLNA.ORG_CI=").append(hypotheticalResult.conversionNeeded ? "1" : "0");
		}
		if (sb.length() > 0) {
			sb.append(';');
		}
		sb.append("DLNA.ORG_FLAGS=00900000000000000000000000000000");

		return sb.toString();
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
		}
		if (parent != null) {
			return parent.getResourceId();
		}
		return "-1";
	}

	public DLNAResource() {
		this.specificType = Format.UNKNOWN;
		//this.children = new ArrayList<DLNAResource>();
		this.children = new DLNAList();
		this.updateId = 1;
		resHash = 0;
	}

	public DLNAResource(int specificType) {
		this();
		this.specificType = specificType;
	}

	/**
	 * Recursive function that searches through all of the children until it finds
	 * a {@link DLNAResource} that matches the name.<p>
	 * Only used by
	 * {@link net.pms.dlna.RootFolder#addWebFolder(File webConf)
	 * addWebFolder(File webConf)} while parsing the web.conf file.
	 *
	 * @param name String to be compared the name to.
	 * @return Returns a {@link DLNAResource} whose name matches the parameter name
	 * @see #getName()
	 */
	public DLNAResource searchByName(String name) {
		for (DLNAResource child : children) {
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
	 * if this DLNAResource is a container.
	 */
	public boolean isCompatible(RendererConfiguration renderer) {
		return format == null
			|| format.isUnknown()
			|| (format.isVideo() && renderer.isVideoSupported())
			|| (format.isAudio() && renderer.isAudioSupported())
			|| (format.isImage() && renderer.isImageSupported());
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
	 * DLNAResource to add to a container type.
	 */
	public void addChild(DLNAResource child) {
		addChild(child, true, true);
	}

	public void addChild(DLNAResource child, boolean isNew) {
		addChild(child, true, true);
	}

	public void addChild(DLNAResource child, boolean isNew, boolean isAddGlobally) {
		// child may be null (spotted - via rootFolder.addChild() - in a misbehaving plugin
		if (child == null) {
			LOGGER.error("A plugin has attempted to add a null child to \"{}\"", getName());
			LOGGER.debug("Error info:", new NullPointerException("Invalid DLNA resource"));
			return;
		}

		child.parent = this;

		if (parent != null) {
			defaultRenderer = parent.getDefaultRenderer();
		}

		if (configuration.useCode() && !PMS.get().masterCodeValid()) {
			String code = PMS.get().codeDb().getCode(child);
			if (StringUtils.isNotEmpty(code)) {
				DLNAResource cobj = child.isCoded();
				if (cobj == null || !((CodeEnter) cobj).getCode().equals(code)) {
					LOGGER.debug("Resource {} is coded add code folder", child);
					CodeEnter ce = new CodeEnter(child);
					ce.parent = this;
					ce.defaultRenderer = this.getDefaultRenderer();
					ce.setCode(code);
					addChildInternal(ce, isAddGlobally);
					return;
				}
			}
		}

		try {
			if (child.isValid()) {
				if (child.format != null) {
					// Do not add unsupported media formats to the list
					if (defaultRenderer != null && !defaultRenderer.supportsFormat(child.format)) {
						LOGGER.trace("Ignoring file \"{}\" because it is not supported by renderer \"{}\"", child.getName(), defaultRenderer.getRendererName());
						children.remove(child);
						return;
					}

					// Hide watched videos depending user preference
					if (FullyPlayed.isHideFullyPlayed(child)) {
						LOGGER.trace("Ignoring video file \"{}\" because it has been watched", child.getName());
						return;
					}
				}

				LOGGER.trace("{} child \"{}\" with class \"{}\"", isNew ? "Adding new" : "Updating", child.getName(), child.getClass().getSimpleName());

				if (allChildrenAreFolders && !child.isFolder()) {
					allChildrenAreFolders = false;
				}

				child.resHash = Math.abs(child.getSystemName().hashCode() + resumeHash());

				DLNAResource resumeRes = null;

				boolean addResumeFile = false;
				ResumeObj r = ResumeObj.create(child);
				if (r != null) {
					resumeRes = child.clone();
					resumeRes.resume = r;
					resumeRes.resHash = child.resHash;
					addResumeFile = true;
				}

				if (child.format != null) {
					// Determine transcoding possibilities if either
					//    - the format is known to be transcodable
					//    - we have media info (via parserV2, playback info, or a plugin)
					if (child.format.transcodable() || child.media != null) {
						if (child.media == null) {
							child.media = new DLNAMediaInfo();
						}

						// Try to determine a player to use for transcoding.
						Player playerTranscoding = null;

						// First, try to match a player from recently played folder or based on the name of the DLNAResource
						// or its parent. If the name ends in "[unique player id]", that player
						// is preferred.
						String name = getName();

						if (configuration.isShowRecentlyPlayedFolder()) {
							playerTranscoding = child.player;
						} else {
							for (Player p : PlayerFactory.getPlayers()) {
								String end = "[" + p.id().toString() + "]";

								if (name.endsWith(end)) {
									nametruncate = name.lastIndexOf(end);
									playerTranscoding = p;
									LOGGER.trace("Selecting player based on name end");
									break;
								} else if (parent != null && parent.getName().endsWith(end)) {
									parent.nametruncate = parent.getName().lastIndexOf(end);
									playerTranscoding = p;
									LOGGER.trace("Selecting player based on parent name end");
									break;
								}
							}
						}

						// If no preferred player could be determined from the name, try to
						// match a player based on media information and format.
						if (playerTranscoding == null) {
							playerTranscoding = child.resolvePlayer(defaultRenderer);
						}
						child.setPlayer(playerTranscoding);
						child.setPreferredMimeType(defaultRenderer);

						if (resumeRes != null) {
							resumeRes.player = playerTranscoding;
							resumeRes.media_subtitle = child.media_subtitle;
						}

						if (!allChildrenAreFolders) {
							child.setDefaultRenderer(defaultRenderer);

							// Should the child be added to the #--TRANSCODE--# folder?
							if ((child.format.isVideo() || child.format.isAudio()) && child.isTranscodeFolderAvailable()) {
								// true: create (and append) the #--TRANSCODE--# folder to this
								// folder if supported/enabled and if it doesn't already exist
								VirtualFolder transcodeFolder = getTranscodeFolder(true, isAddGlobally);
								if (transcodeFolder != null) {
									VirtualFolder fileTranscodeFolder = new FileTranscodeVirtualFolder(child);
									if (parent instanceof SubSelect) {
										fileTranscodeFolder.setMediaSubtitle(child.getMediaSubtitle());
									}

									LOGGER.trace("Adding \"{}\" to transcode folder for player: \"{}\"", child.getName(), playerTranscoding);
									transcodeFolder.addChildInternal(fileTranscodeFolder, isAddGlobally);
								}
							}

							if (child.format.isVideo() && child.isSubSelectable() && !(this instanceof SubSelFile)) {
								VirtualFolder vf = getSubSelector(true);
								if (vf != null) {
									DLNAResource newChild = child.clone();
									newChild.player = playerTranscoding;
									newChild.media = child.media;
									LOGGER.trace("Adding live subtitles folder for \"{}\" with player {}", child.getName(), playerTranscoding);

									vf.addChild(new SubSelFile(newChild), true, isAddGlobally);
								}
							}

							if (configuration.isDynamicPls() &&
								!child.isFolder() &&
								defaultRenderer != null &&
								!defaultRenderer.isNoDynPlsFolder()) {
								addDynamicPls(child);
							}
						} else if (!child.format.isCompatible(child, defaultRenderer) && !child.isFolder()) {
							LOGGER.trace("Ignoring file \"{}\" because it is not compatible with renderer \"{}\"", child.getName(), defaultRenderer.getRendererName());
							children.remove(child);
						}
					}

					if (resumeRes != null && resumeRes.media != null) {
						resumeRes.media.setThumbready(false);
						resumeRes.media.setMimeType(HTTPResource.VIDEO_TRANSCODE);
					}

					/**
					 * Secondary format is currently only used to provide 24-bit FLAC to PS3 by
					 * sending it as a fake video. This can be made more reusable with a renderer
					 * config setting like Mux24BitFlacToVideo if we ever have another purpose
					 * for it, which I doubt we will have.
					 */
					if (
						child.format.getSecondaryFormat() != null &&
						child.media != null &&
						defaultRenderer != null &&
						defaultRenderer.supportsFormat(child.format.getSecondaryFormat()) &&
						defaultRenderer.isPS3()
					) {
						DLNAResource newChild = child.clone();
						newChild.setFormat(newChild.format.getSecondaryFormat());
						LOGGER.trace("Detected secondary format \"{}\" for \"{}\"", newChild.format.toString(), newChild.getName());
						newChild.first = child;
						child.second = newChild;

						if (!newChild.format.isCompatible(newChild, defaultRenderer)) {
							Player playerTranscoding = PlayerFactory.getPlayer(newChild);
							newChild.setPlayer(playerTranscoding);
							LOGGER.trace("Secondary format \"{}\" will use player \"{}\" for \"{}\"", newChild.format.toString(), player == null ? "null" : player.name(), newChild.getName());
						}

						if (child.media != null && child.media.isSecondaryFormatValid()) {
							addChild(newChild, true, isAddGlobally);
							LOGGER.trace("Adding secondary format \"{}\" for \"{}\"", newChild.format.toString(), newChild.getName());
						} else {
							LOGGER.trace("Ignoring secondary format \"{}\" for \"{}\": invalid format", newChild.format.toString(), newChild.getName());
						}
					}
				}

				if (addResumeFile && resumeRes != null) {
					resumeRes.setDefaultRenderer(child.getDefaultRenderer());
					addChildInternal(resumeRes, isAddGlobally);
				}

				if (isNew) {
					addChildInternal(child, isAddGlobally);
				}
			}
		} catch (Throwable t) {
			LOGGER.error("Error adding child: \"{}\"", child.getName(), t);

			child.parent = null;
			children.remove(child);
		}
	}

	/**
	 * Determine whether we are a candidate for streaming or transcoding to the
	 * given renderer, and return the relevant player or null as appropriate.
	 *
	 * @param renderer The target renderer
	 * @return A player if transcoding or null if streaming
	 */
	public Player resolvePlayer(RendererConfiguration renderer) {
		// Use device-specific DMS conf, if any
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(renderer);
		boolean parserV2 = media != null && renderer != null && renderer.isUseMediaInfo();
		Player resolvedPlayer = null;

		if (media == null) {
			media = new DLNAMediaInfo();
		}

		if (format == null) {
			// Shouldn't happen, this is just a desperate measure
			Format f = FormatFactory.getAssociatedFormat(getSystemName());
			setFormat(f != null ? f : FormatFactory.getAssociatedFormat(".mpg"));
		}

		// Check if we're a transcode folder item
		if (isNoName() && (getParent() instanceof FileTranscodeVirtualFolder)) {
			// Yes, leave everything as-is
			resolvedPlayer = getPlayer();
			LOGGER.trace("Selecting player {} based on transcode item settings", resolvedPlayer);
			return resolvedPlayer;
		}

		/*
		 * At this stage, we know the media is compatible with the renderer based on its
		 * "Supported" lines, and can therefore be streamed to the renderer without a
		 * player. However, other details about the media can change this, such as
		 * whether it has subtitles that match this user's language settings, so here we
		 * perform those checks.
		 */
		if (media.isVideo() && !configurationSpecificToRenderer.isDisableSubtitles()) {
			if (hasSubtitles(false)) {
				DLNAMediaAudio audio = media_audio != null ? media_audio : resolveAudioStream(renderer);
				if (media_subtitle == null) {
					media_subtitle = resolveSubtitlesStream(renderer, audio == null ? null : audio.getLang(), false);
				}
				if (media_subtitle != null) {
					if (media_subtitle.isExternal()) {
						if (renderer != null && renderer.isExternalSubtitlesFormatSupported(media_subtitle, media)) {
							LOGGER.trace("This video has external subtitles that can be streamed");
						} else {
							LOGGER.trace("This video has external subtitles that must be transcoded");
						}
					} else {
						if (renderer != null && renderer.isEmbeddedSubtitlesFormatSupported(media_subtitle)) {
							LOGGER.trace("This video has embedded subtitles that are supported");
						} else {
							LOGGER.trace("This video has embedded subtitles that must be transcoded");
						}
					}
				}
			} else {
				LOGGER.trace("This video does not have subtitles");
			}
		}

		String rendererForceExtensions = renderer == null ? null : renderer.getTranscodedExtensions();
		String rendererSkipExtensions = renderer == null ? null : renderer.getStreamedExtensions();
		String configurationForceExtensions = configurationSpecificToRenderer.getForceTranscodeForExtensions();
		String configurationSkipExtensions = configurationSpecificToRenderer.getDisableTranscodeForExtensions();

		if (configurationSpecificToRenderer.isDisableTranscoding()) {
			LOGGER.debug("Final verdict: \"{}\" will be streamed since transcoding is disabled", getName());
			return null;
		}

		// Should transcoding be skipped for this format?
		skipTranscode = format.skip(configurationSkipExtensions, rendererSkipExtensions);

		if (skipTranscode) {
			LOGGER.debug("Final verdict: \"{}\" will be streamed since it is forced by configuration", getName());
			return null;
		}

		// Should transcoding be forced for this format?
		boolean forceTranscode = format.skip(configurationForceExtensions, rendererForceExtensions);

		// Try to match a player based on media information and format.
		resolvedPlayer = PlayerFactory.getPlayer(this);

		if (resolvedPlayer != null) {
			boolean isIncompatible = false;
			String prependTranscodingReason = "File \"{}\" will not be streamed because ";
			if (forceTranscode) {
				LOGGER.debug(prependTranscodingReason + "transcoding is forced by configuration", getName());
			} else if (this instanceof DVDISOTitle) {
				forceTranscode = true;
				LOGGER.debug(prependTranscodingReason + "streaming of DVD video tracks isn't supported", getName());
			} else if (!format.isCompatible(this, renderer)) {
				isIncompatible = true;
				if (renderer == null) {
					LOGGER.debug(prependTranscodingReason + "the renderer is not recognised", getName());
				} else {
					LOGGER.debug(prependTranscodingReason + "it is not supported by the renderer {}", getName(), renderer.getRendererName());
				}
			} else if (configurationSpecificToRenderer.isEncodedAudioPassthrough()) {
				if (
					getMediaAudio() != null &&
					(
						FormatConfiguration.AC3.equals(getMediaAudio().getAudioCodec()) ||
						FormatConfiguration.DTS.equals(getMediaAudio().getAudioCodec())
					)
				) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the audio will use the encoded audio passthrough feature", getName());
				} else {
					for (DLNAMediaAudio audioTrack : media.getAudioTracksList()) {
						if (
							audioTrack != null &&
							(
								FormatConfiguration.AC3.equals(audioTrack.getAudioCodec()) ||
								FormatConfiguration.DTS.equals(audioTrack.getAudioCodec())
							)
						) {
							isIncompatible = true;
							LOGGER.debug(prependTranscodingReason + "the audio will use the encoded audio passthrough feature", getName());
							break;
						}
					}
				}
			}

			if (!forceTranscode && !isIncompatible && format.isVideo() && parserV2 && renderer != null) {
				int maxBandwidth = renderer.getMaxBandwidth();

				if (
					renderer.isKeepAspectRatio() &&
					!"16:9".equals(media.getAspectRatioContainer())
				) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the renderer needs us to add borders to change the aspect ratio from {} to 16/9.", getName(), media.getAspectRatioContainer());
				} else if (!renderer.isResolutionCompatibleWithRenderer(media.getWidth(), media.getHeight())) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the resolution is incompatible with the renderer.", getName());
				} else if (media.getBitrate() > maxBandwidth) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the bitrate ({} b/s) is too high ({} b/s).", getName(), media.getBitrate(), maxBandwidth);
				} else if (!renderer.isVideoBitDepthSupported(media.getVideoBitDepth())) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the video bit depth ({}) is not supported.", getName(), media.getVideoBitDepth());
				} else if (renderer.isH264Level41Limited() && media.isH264()) {
					if (media.getAvcLevel() != null) {
						double h264Level = 4.1;

						try {
							h264Level = Double.parseDouble(media.getAvcLevel());
						} catch (NumberFormatException e) {
							LOGGER.trace("Could not convert {} to double: {}", media.getAvcLevel(), e.getMessage());
						}

						if (h264Level > 4.1) {
							isIncompatible = true;
							LOGGER.debug(prependTranscodingReason + "the H.264 level ({}) is not supported.", getName(), h264Level);
						}
					} else {
						isIncompatible = true;
						LOGGER.debug(prependTranscodingReason + "the H.264 level is unknown.", getName());
					}
				} else if (media.is3d() && StringUtils.isNotBlank(renderer.getOutput3DFormat()) && (!media.get3DLayout().toString().toLowerCase(Locale.ROOT).equals(renderer.getOutput3DFormat()))) {
					forceTranscode = true;
					LOGGER.debug(prependTranscodingReason + "it is 3D and is forced to transcode to the format \"{}\"", getName(), renderer.getOutput3DFormat());
				}
			}

			// Transcode if:
			// 1) transcoding is forced by configuration, or
			// 2) transcoding is not prevented by configuration
			if (forceTranscode || (isIncompatible && !isSkipTranscode())) {
				if (parserV2) {
					LOGGER.debug("Final verdict: \"{}\" will be transcoded with player \"{}\" with mime type \"{}\"", getName(), resolvedPlayer.toString(), renderer != null ? renderer.getMimeType(this) : media.getMimeType());
				} else {
					LOGGER.debug("Final verdict: \"{}\" will be transcoded with player \"{}\"", getName(), resolvedPlayer.toString());
				}
			} else {
				resolvedPlayer = null;
				LOGGER.debug("Final verdict: \"{}\" will be streamed", getName());
			}
		} else {
			LOGGER.debug("Final verdict: \"{}\" will be streamed because no compatible player was found", getName());
		}
		return resolvedPlayer;
	}


	/**
	 * Set the mimetype for this resource according to the given renderer's
	 * supported preferences, if any.
	 *
	 * @param renderer The renderer
	 * @return The previous mimetype for this resource, or null
	 */
	public String setPreferredMimeType(RendererConfiguration renderer) {
		String prev = media != null ? media.getMimeType() : null;
		boolean parserV2 = media != null && renderer != null && renderer.isUseMediaInfo();
		if (parserV2 && (format == null || !format.isImage())) {
			// See which MIME type the renderer prefers in case it supports the media
			String preferred = renderer.getFormatConfiguration().match(this);
			if (preferred != null) {
				/**
				 * Use the renderer's preferred MIME type for this file.
				 */
				if (!FormatConfiguration.MIMETYPE_AUTO.equals(preferred)) {
					media.setMimeType(preferred);
				}
				LOGGER.trace("File \"{}\" will be sent with MIME type \"{}\"", getName(), preferred);
			}
		}
		return prev;
	}

	/**
	 * Return the transcode folder for this resource.
	 * If DMS is configured to hide transcode folders, null is returned.
	 * If no folder exists and the create argument is false, null is returned.
	 * If no folder exists and the create argument is true, a new transcode folder is created.
	 * This method is called on the parent folder each time a child is added to that parent
	 * (via {@link addChild(DLNAResource)}.
	 *
	 * @param create
	 * @return the transcode virtual folder
	 */
	// XXX package-private: used by MapFile; should be protected?
	TranscodeVirtualFolder getTranscodeFolder(boolean create) {
		return getTranscodeFolder(create, true);
	}

	TranscodeVirtualFolder getTranscodeFolder(boolean create, boolean isAddGlobally) {
		if (!isTranscodeFolderAvailable()) {
			return null;
		}

		if (!configuration.isShowTranscodeFolder()) {
			return null;
		}

		// search for transcode folder
		for (DLNAResource child : children) {
			if (child instanceof TranscodeVirtualFolder) {
				return (TranscodeVirtualFolder) child;
			}
		}

		if (create) {
			TranscodeVirtualFolder transcodeFolder = new TranscodeVirtualFolder(null, configuration);
			addChildInternal(transcodeFolder, isAddGlobally);
			return transcodeFolder;
		}

		return null;
	}

	public void updateChild(DLNAResource child) {
		updateChild(child, true);
	}

	/**
	 * (Re)sets the given DLNA resource as follows:
	 *    - if it's already one of our children, renew it
	 *    - or if we have another child with the same name, replace it
	 *    - otherwise add it as a new child.
	 *
	 * @param child the DLNA resource to update
	 */
	public void updateChild(DLNAResource child, boolean isAddGlobally) {
		DLNAResource found = children.contains(child) ?
			child : searchByName(child.getName());
		if (found != null) {
			if (child != found) {
				// Replace
				child.parent = this;
				PMS.getGlobalRepo().replace(found, child);
				children.set(children.indexOf(found), child);
			}
			// Renew
			addChild(child, false, isAddGlobally);
		} else {
			// Not found, it's new
			addChild(child, true, isAddGlobally);
		}
	}

	/**
	 * Adds the supplied DLNA resource in the internal list of child nodes,
	 * and sets the parent to the current node. Avoids the side-effects
	 * associated with the {@link #addChild(DLNAResource)} method.
	 *
	 * @param child the DLNA resource to add to this node's list of children
	 */
	protected synchronized void addChildInternal(DLNAResource child) {
		addChildInternal(child, true);
	}

	/**
	 * Adds the supplied DLNA resource in the internal list of child nodes,
	 * and sets the parent to the current node. Avoids the side-effects
	 * associated with the {@link #addChild(DLNAResource)} method.
	 *
	 * @param child the DLNA resource to add to this node's list of children
	 * @param isAddGlobally when a global ID is added for a DLNAResource it
	 *                      means the garbage collector can't clean up the
	 *                      memory, and sometimes we don't need the resource
	 *                      to hang around forever.
	 */
	protected synchronized void addChildInternal(DLNAResource child, boolean isAddGlobally) {
		if (child.getInternalId() != null) {
			LOGGER.debug(
				"Node ({}) already has an ID ({}), which is overridden now. The previous parent node was: {}",
				new Object[] {
					child.getClass().getName(),
					child.getResourceId(),
					child.parent
				}
			);
		}

		children.add(child);
		child.parent = this;

		if (isAddGlobally) {
			PMS.getGlobalRepo().add(child);
		}
	}

	public synchronized DLNAResource getDLNAResource(String objectId, RendererConfiguration renderer) {
		// this method returns exactly ONE (1) DLNAResource
		// it's used when someone requests playback of media. The media must
		// first have been discovered by someone first (unless it's a Temp item)

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			return Temp.get(objectId, renderer);
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		DLNAResource dlna;
		String[] ids = objectId.split("\\.");
		if (objectId.equals("0")) {
			dlna = renderer.getRootFolder();
		} else {
			// only allow the last one here
			dlna = PMS.getGlobalRepo().get(ids[ids.length - 1]);
		}

		if (dlna == null) {
			return null;
		}

		return dlna;
	}

	/**
	 * First thing it does it searches for an item matching the given objectID.
	 * If children is false, then it returns the found object as the only object in the list.
	 * TODO: (botijo) This function does a lot more than this!
	 *
	 * @param objectId ID to search for.
	 * @param children State if you want all the children in the returned list.
	 * @param start
	 * @param count
	 * @param renderer Renderer for which to do the actions.
	 * @return List of DLNAResource items.
	 * @throws IOException
	 */
	public synchronized List<DLNAResource> getDLNAResources(String objectId, boolean children, int start, int count, RendererConfiguration renderer) throws IOException {
		return getDLNAResources(objectId, children, start, count, renderer, null);
	}

	public synchronized List<DLNAResource> getDLNAResources(String objectId, boolean returnChildren, int start, int count, RendererConfiguration renderer, String searchStr) {
		ArrayList<DLNAResource> resources = new ArrayList<>();

		// Get/create/reconstruct it if it's a Temp item
		if (objectId.contains("$Temp/")) {
			List<DLNAResource> items = Temp.asList(objectId);
			return items != null ? items : resources;
		}

		// Now strip off the filename
		objectId = StringUtils.substringBefore(objectId, "/");

		DLNAResource dlna;
		String[] ids = objectId.split("\\.");
		if (objectId.equals("0")) {
			dlna = renderer.getRootFolder();
		} else {
			dlna = PMS.getGlobalRepo().get(ids[ids.length - 1]);
		}

		if (dlna == null) {
			// nothing in the cache do a traditional search
			dlna = search(ids, renderer);
			//dlna = search(objectId, count, renderer, searchStr);
		}

		if (dlna != null) {
			if (!(dlna instanceof CodeEnter) && !isCodeValid(dlna)) {
				LOGGER.debug("code is not valid any longer");
				return resources;
			}
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
						queue,
						new BasicThreadFactory("DLNAResource resolver thread %d-%d")
					);

					for (int i = start; i < start + count && i < dlna.getChildren().size(); i++) {
						final DLNAResource child = dlna.getChildren().get(i);
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
					}

					LOGGER.trace("End of analysis for " + systemName);
				}
			}
		}

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
		lastRefreshTime = System.currentTimeMillis();
		updateId += 1;
		systemUpdateId += 1;
	}

	final protected void discoverWithRenderer(RendererConfiguration renderer, int count, boolean forced, String searchStr) {
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(renderer);
		// Discover children if it hasn't been done already
		if (!isDiscovered()) {
			if (configurationSpecificToRenderer.getFolderLimit() && depthLimit()) {
				if (renderer.isPS3() || renderer.isXbox360()) {
					LOGGER.info("Depth limit potentionally hit for " + getDisplayName());
				}

				if (defaultRenderer != null) {
					defaultRenderer.addFolderLimit(this);
				}
			}

			discoverChildren(searchStr);
			boolean ready;

			if (renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
				ready = analyzeChildren(count);
			} else {
				ready = analyzeChildren(-1);
			}

			if (!renderer.isUseMediaInfo() || ready) {
				setDiscovered(true);
			}

			notifyRefresh();
		} else {
			// if forced, then call the old 'refreshChildren' method
			LOGGER.trace("discover {} refresh forced: {}", getResourceId(), forced);
			/*if (forced && shouldRefresh(searchStr)) {
				doRefreshChildren(searchStr);
				notifyRefresh();
			} */
			if (forced) {
				// This seems to follow the same code path as the else below in the case of MapFile, because
				// refreshChildren calls shouldRefresh -> isRefreshNeeded -> doRefreshChildren, which is what happens below
				// (refreshChildren is not overridden in MapFile)
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
		return isRefreshNeeded();
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
	 * Recursive function that searches for a given ID.
	 *
	 * @param searchId ID to search for.
	 * @param count
	 * @param renderer
	 * @param searchStr
	 * @return Item found, or null otherwise.
	 * @see #getId()
	 */
	public DLNAResource search(String searchId, int count, RendererConfiguration renderer, String searchStr) {
		if (id != null && searchId != null) {
			String[] indexPath = searchId.split("\\$", 2);
			if (id.equals(indexPath[0])) {
				if (indexPath.length == 1 || indexPath[1].length() == 0) {
					return this;
				}
				discoverWithRenderer(renderer, count, false, null);

				for (DLNAResource file : children) {
					DLNAResource found = file.search(indexPath[1], count, renderer, null);
					if (found != null) {
						// Make sure it's ready
						//found.resolve();
						return found;
					}
				}
			} else {
				return null;
			}
		}

		return null;
	}

	private static DLNAResource search(String[] searchIds, RendererConfiguration renderer) {
		DLNAResource dlna;
		for (String searchId : searchIds) {
			if (searchId.equals("0")) {
				dlna = renderer.getRootFolder();
			} else {
				dlna = PMS.getGlobalRepo().get(searchId);
			}

			if (dlna == null) {
				LOGGER.debug("Bad id {} found in path", searchId);
				return null;
			}

			dlna.discoverWithRenderer(renderer, 0, false, null);
		}

		return PMS.getGlobalRepo().get(searchIds[searchIds.length - 1]);
	}

	public DLNAResource search(String searchId) {
		if (id != null && searchId != null) {
			if (getResourceId().equals(searchId)) {
				return this;
			}
			for (DLNAResource file : children) {
				DLNAResource found = file.search(searchId);
				if (found != null) {
					// Make sure it's ready
					//found.resolve();
					return found;
				}
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
	 *
	 * @param count
	 * @return Returns true
	 */
	public boolean analyzeChildren(int count) {
		return true;
	}

	public boolean analyzeChildren(int count, boolean isAddGlobally) {
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
	 *
	 * @since 1.90.0
	 */
	protected void resolveFormat() {
		if (format == null) {
			format = FormatFactory.getAssociatedFormat(getSystemName());
		}

		if (format != null && format.isUnknown()) {
			format.setType(getSpecificType());
		}
	}

	/**
	 * Hook to lazily initialise immutable resources e.g. ISOs, zip files &amp;c.
	 *
	 * @since 1.90.0
	 * @see #syncResolve()
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
	public synchronized final void syncResolve() {
		resolve();
		if (media != null && media.isVideo()) {
			registerExternalSubtitles(false);
			if (this instanceof RealFile && ((RealFile) this).getFile() != null) {
				setMetadataFromFileName(((RealFile) this).getFile());
			}
		}
	}

	/**
	 * @deprecated Use {@link #syncResolve()} instead
	 */
	@Deprecated
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
	 * Returns the "engine"/player part of the display name or {@code null} if
	 * none should be displayed.
	 * Returns the display name for the default renderer.
	 *
	 * @param configuration the {@link PmsConfiguration} to use.
	 * @return The engine display name or {@code null}.
	 */
	protected String getDisplayNameEngine(PmsConfiguration configuration) {
		String engineName = null;
		if (player != null) {
			if (
				isNoName() ||
				!configuration.isHideEngineNames()
			) {
				engineName = "[" + player.name() + (isAvisynth() ? " + AviSynth]" : "]");
			}
		} else if (isNoName()) {
			engineName = Messages.getString("DLNAResource.0");
		}
		return engineName;
	}

	/**
	 * Returns the "base" part of the display name or an empty {@link String} if
	 * none should be displayed. The "base" name is the name of this
	 * {@link DLNAResource} without any prefix or suffix.
	 *
	 * @return The base display name or {@code ""}.
	 */
	protected String getDisplayNameBase() {
		// this unescape trick is to solve the problem of a name containing
		// unicode stuff like \u005e
		// if it's done here it will fix this for all objects
		return isNoName() ? "" : StringEscapeUtils.unescapeJava(getName());
	}

	/**
	 * Returns the suffix part of the display name or an empty {@link String} if
	 * none should be displayed.
	 *
	 * @param renderer the {@link RendererConfiguration} to use.
	 * @param configuration the {@link PmsConfiguration} to use.
	 * @return The display name suffix or {@code ""}.
	 */
	protected String getDisplayNameSuffix(RendererConfiguration renderer, PmsConfiguration configuration) {
		if (media == null) {
			return null;
		}
		MediaType mediaType = media.getMediaType();
		switch (mediaType) {
			case VIDEO:
				StringBuilder nameSuffixBuilder = new StringBuilder();
				boolean subsAreValidForStreaming =
					media_subtitle != null &&
					media_subtitle.isExternal() &&
					renderer != null &&
					(player == null || renderer.streamSubsForTranscodedVideo()) &&
					renderer.isExternalSubtitlesFormatSupported(media_subtitle, media);

				if (media_audio != null) {
					String audioLanguage = media_audio.getLang();
					if (audioLanguage == null || DLNAMediaLang.UND.equals(audioLanguage.toLowerCase(Locale.ROOT))) {
						audioLanguage = "";
					} else {
						audioLanguage = Iso639.getFirstName(audioLanguage);
						audioLanguage = audioLanguage == null ? "" : "/" + audioLanguage;
					}

					String audioTrackTitle = "";
					if (
						media_audio.getAudioTrackTitleFromMetadata() != null &&
						!"".equals(media_audio.getAudioTrackTitleFromMetadata()) &&
						renderer != null &&
						renderer.isShowAudioMetadata()
					) {
						audioTrackTitle = " (" + media_audio.getAudioTrackTitleFromMetadata() + ")";
					}

					if (nameSuffixBuilder.length() > 0) {
						nameSuffixBuilder.append(" ");
					}
					nameSuffixBuilder.append("{Audio: ").append(media_audio.getAudioCodec())
						.append(audioLanguage).append(audioTrackTitle).append("}");
				}

				SubtitlesInfoLevel subsInfoLevel;
				if (parent instanceof ChapterFileTranscodeVirtualFolder) {
					subsInfoLevel = SubtitlesInfoLevel.NONE;
				} else if (parent instanceof FileTranscodeVirtualFolder) {
					subsInfoLevel = SubtitlesInfoLevel.FULL;
				} else {
					subsInfoLevel = configuration.getSubtitlesInfoLevel();
				}
				if (
					media_subtitle != null &&
					media_subtitle.getId() != DLNAMediaLang.DUMMY_ID &&
					subsInfoLevel != SubtitlesInfoLevel.NONE
				) {
					if (nameSuffixBuilder.length() > 0) {
						nameSuffixBuilder.append(" ");
					}
					nameSuffixBuilder.append("{");
					String subtitleLanguage = media_subtitle.getLangFullName();
					if (subsInfoLevel == SubtitlesInfoLevel.BASIC) {
						if ("Undetermined".equals(subtitleLanguage)) {
							nameSuffixBuilder.append(Messages.getString("Generic.Unknown"));
						} else {
							nameSuffixBuilder.append(subtitleLanguage);
						}
						nameSuffixBuilder.append(" ").append(Messages.getString("Subtitles.LowerCase"));
					} else if (subsInfoLevel == SubtitlesInfoLevel.FULL) {
						if (subsAreValidForStreaming) {
							nameSuffixBuilder.append(Messages.getString("DLNAResource.3")).append(" ");
						}

						if (media_subtitle.isExternal()) {
							nameSuffixBuilder.append(Messages.getString("Subtitles.ExternalShort")).append(" ");
						} else if (media_subtitle.isEmbedded()) {
							nameSuffixBuilder.append(Messages.getString("Subtitles.InternalShort")).append(" ");
						}
						nameSuffixBuilder.append(Messages.getString("DLNAResource.2"));
						nameSuffixBuilder.append(media_subtitle.getType().getShortName()).append("/");

						if ("Undetermined".equals(subtitleLanguage)) {
							nameSuffixBuilder.append(Messages.getString("Subtitles.UnknownShort"));
						} else {
							nameSuffixBuilder.append(subtitleLanguage);
						}

						if (
							renderer != null &&
							media_subtitle.getSubtitlesTrackTitleFromMetadata() != null &&
							isNotBlank(media_subtitle.getSubtitlesTrackTitleFromMetadata()) &&
							renderer.isShowSubMetadata()
						) {
							nameSuffixBuilder
								.append(" (").append(media_subtitle.getSubtitlesTrackTitleFromMetadata()).append(")");
						}
					}
					nameSuffixBuilder.append("}");
				}
				return nameSuffixBuilder.toString();
			case AUDIO:
			case IMAGE:
			case UNKNOWN:
			default:
				return null;
			}
		}

	// Ditlew
	/**
	 * Returns the display name for the default renderer.
	 *
	 * @return The display name.
	 */
	public String getDisplayName() {
		return getDisplayName(null, true);
	}

	/**
	 * Returns the display name for the specified renderer.
	 *
	 * @param mediaRenderer the {@link RendererConfiguration} for which to adapt
	 *            the display name.
	 * @return The display name.
	 */
	public String getDisplayName(RendererConfiguration mediaRenderer) {
		return getDisplayName(mediaRenderer, true);
	}

	/**
	 * Returns the display name for the specified renderer with or without
	 * additional information suffix.
	 *
	 * @param mediaRenderer the {@link RendererConfiguration} for which to adapt
	 *            the display name.
	 * @param withSuffix if {@code true} additional information is added after
	 *            the name itself, if {@code false} nothing is added.
	 * @return The display name.
	 */
	public String getDisplayName(RendererConfiguration mediaRenderer, boolean withSuffix) {
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(mediaRenderer);
		StringBuilder sb = new StringBuilder();

		// Base
		if (parent instanceof ChapterFileTranscodeVirtualFolder && getSplitRange() != null) {
			sb.append(">> ");
			sb.append(
				convertTimeToString(getSplitRange().isStartOffsetAvailable() ?
					getSplitRange().getStart() :
					0,
				DURATION_TIME_FORMAT)
			);
		} else {
			sb.append(getDisplayNameBase());
		}

		// Suffix
		if (withSuffix) {
			String nameSuffix = getDisplayNameSuffix(mediaRenderer, configurationSpecificToRenderer);
			if (isNotBlank(nameSuffix)) {
				sb.append(" ").append(nameSuffix);
			}
		}

		// Engine name
		String engineName = getDisplayNameEngine(configurationSpecificToRenderer);
		if (engineName != null) {
			sb.append(" ").append(engineName);
		}

		// Truncate
		if (nametruncate > 0) {
			return sb.substring(0, nametruncate).trim();
		}

		return sb.toString();
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
	 * @return Returns an URL pointing to an image representing the item. If
	 * none is available, "thumbnail0000.png" is used.
	 */
	protected String getThumbnailURL(DLNAImageProfile profile) {
		StringBuilder sb = new StringBuilder(PMS.get().getServer().getURL());
		sb.append("/get/").append(getResourceId()).append("/thumbnail0000");
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
	 * @param prefix
	 * @return Returns a URL for a given media item. Not used for container types.
	 */
	public String getURL(String prefix) {
		return getURL(prefix, false);
	}

	public String getURL(String prefix, boolean useSystemName) {
		StringBuilder sb = new StringBuilder();
		sb.append(PMS.get().getServer().getURL());
		sb.append("/get/");
		sb.append(getResourceId()); //id
		sb.append('/');
		sb.append(prefix);
		sb.append(encode(useSystemName ? getSystemName() : getName()));
		return sb.toString();
	}

	/**
	 * @param subs
	 * @return Returns a URL for a given subtitles item. Not used for container types.
	 */
	protected String getSubsURL(DLNAMediaSubtitle subs) {
		StringBuilder sb = new StringBuilder();
		sb.append(PMS.get().getServer().getURL());
		sb.append("/get/");
		sb.append(getResourceId()); //id
		sb.append('/');
		sb.append("subtitle0000");
		sb.append(encode(subs.getName()));
		return sb.toString();
	}

	/**
	 * Transforms a String to URL encoded UTF-8.
	 *
	 * @param s
	 * @return Transformed string s in UTF-8 encoding.
	 */
	private static String encode(String s) {
		try {
			return URLEncoder.encode(s, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.debug("Error while URL encoding \"{}\": {}", s, e.getMessage());
			LOGGER.trace("", e);
		}

		return "";
	}

	/**
	 * @return Number of children objects. This might be used in the DLDI
	 * response, as some renderers might not have enough memory to hold the
	 * list for all children.
	 */
	public int childrenNumber() {
		if (children == null) {
			return 0;
		}

		return children.size();
	}

	/**
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#clone()
	 */
	@Override
	protected DLNAResource clone() {
		DLNAResource o = null;
		try {
			o = (DLNAResource) super.clone();
			o.setId(null);

			// Make sure clones (typically #--TRANSCODE--# folder files)
			// have the option to respond to resolve events
			o.resolved = false;

			if (media != null) {
				o.media = media.clone();
			}
		} catch (CloneNotSupportedException e) {
			LOGGER.error(null, e);
		}

		return o;
	}

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
	 *
	 * Note that seek-by-byte is the preferred option for streamed files [1] and seek-by-time is the
	 * preferred option for transcoded files.
	 *
	 * seek-by-time requires a) support by the renderer (via the SeekByTime renderer conf option)
	 * and b) support by the transcode engine.
	 *
	 * The seek-by-byte fallback doesn't work well with transcoded files [2], but it's better than
	 * disabling seeking (and pausing) altogether.
	 *
	 * @param mediaRenderer
	 * 			Media Renderer for which to represent this information.
	 * @return String representation of the DLNA.ORG_OP flags
	 */
	private String getDlnaOrgOpFlags(RendererConfiguration mediaRenderer) {
		String dlnaOrgOpFlags = "01"; // seek by byte (exclusive)

		if (mediaRenderer.isSeekByTime() && player != null && player.isTimeSeekable()) {
			/**
			 * Some renderers - e.g. the PS3 and Panasonic TVs - behave erratically when
			 * transcoding if we keep the default seek-by-byte permission on when permitting
			 * seek-by-time.
			 *
			 * It's not clear if this is a bug in the DLNA libraries of these renderers or a bug
			 * in DMS, but setting an option in the renderer conf that disables seek-by-byte when
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

		return "DLNA.ORG_OP=" + dlnaOrgOpFlags;
	}

	/**
	 * Creates the DLNA.ORG_PN to send.
	 * DLNA.ORG_PN is a string that tells the renderer what type of file to expect, like its
	 * container, framerate, codecs and resolution.
	 * Some renderers will not play a file if it has the wrong DLNA.ORG_PN string, while others
	 * are fine with any string or even nothing.
	 *
	 * @param mediaRenderer
	 * 			Media Renderer for which to represent this information.
	 * @param localizationValue
	 * @return String representation of the DLNA.ORG_PN flags
	 */
	private String getDlnaOrgPnFlags(RendererConfiguration mediaRenderer, int localizationValue) {
		// Use device-specific DMS conf, if any
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(mediaRenderer);
		String mime = getRendererMimeType(mediaRenderer);

		String dlnaOrgPnFlags = null;

		if (mediaRenderer.isDLNAOrgPNUsed() || mediaRenderer.isAccurateDLNAOrgPN()) {
			if (mediaRenderer.isPS3()) {
				if (mime.equals(DIVX_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=AVI";
				} else if (mime.equals(WMV_TYPEMIME) && media != null && media.getHeight() > 700) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=WMVHIGH_PRO";
				}
			} else {
				if (mime.equals(MPEG_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMPEG_PS_PALLocalizedValue(localizationValue);

					if (player != null) {
						// VLC Web Video (Legacy) and tsMuxeR always output MPEG-TS
						boolean isFileMPEGTS = TsMuxeRVideo.ID.equals(player.id()) || VideoLanVideoStreaming.ID.equals(player.id());

						// Check if the renderer settings make the current engine always output MPEG-TS
						if (
							!isFileMPEGTS &&
							mediaRenderer.isTranscodeToMPEGTS() &&
							(
								MEncoderVideo.ID.equals(player.id()) ||
								FFMpegVideo.ID.equals(player.id()) ||
								VLCVideo.ID.equals(player.id()) ||
								AviSynthFFmpeg.ID.equals(player.id()) ||
								AviSynthMEncoder.ID.equals(player.id())
							)
						) {
							isFileMPEGTS = true;
						}

						boolean isMuxableResult = getMedia() != null && getMedia().isMuxable(mediaRenderer);

						// If the engine is capable of automatically muxing to MPEG-TS and the setting is enabled, it might be MPEG-TS
						if (
							!isFileMPEGTS &&
							(
								(
									configurationSpecificToRenderer.isMencoderMuxWhenCompatible() &&
									MEncoderVideo.ID.equals(player.id())
								) ||
								(
									configurationSpecificToRenderer.isFFmpegMuxWithTsMuxerWhenCompatible() &&
									FFMpegVideo.ID.equals(player.id())
								)
							)
						) {
							/*
							 * Media renderer needs ORG_PN to be accurate.
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
							if (mediaRenderer.isAccurateDLNAOrgPN()) {
								if (media_subtitle == null) {
									DLNAMediaAudio audio = media_audio != null ? media_audio : resolveAudioStream(mediaRenderer);
									media_subtitle = resolveSubtitlesStream(
										mediaRenderer,
										audio == null ? null : audio.getLang(),
										false
									);
								}

								if (media_subtitle == null) {
									LOGGER.trace("We do not want a subtitle for {}", getName());
								} else {
									LOGGER.trace("We do want a subtitle for {}", getName());
								}
							}

							/**
							 * If:
							 * - There are no subtitles
							 * - This is not a DVD track
							 * - The media is muxable
							 * - The renderer accepts media muxed to MPEG-TS
							 * then the file is MPEG-TS
							 */
							if (
								media_subtitle == null &&
								!hasExternalSubtitles() &&
								media != null &&
								media.getDvdtrack() == 0 &&
								isMuxableResult &&
								mediaRenderer.isMuxH264MpegTS()
							) {
								isFileMPEGTS = true;
							}
						}

						if (isFileMPEGTS) {
							dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMPEG_TS_SD_EU_ISOLocalizedValue(localizationValue);
							if (
								media.isH264() &&
								!VideoLanVideoStreaming.ID.equals(player.id()) &&
								isMuxableResult
							) {
								dlnaOrgPnFlags = "DLNA.ORG_PN=AVC_TS_HD_24_AC3_ISO";
								if (mediaRenderer.isTranscodeToMPEGTSH264AAC()) {
									dlnaOrgPnFlags = "DLNA.ORG_PN=AVC_TS_HP_HD_AAC";
								}
							}
						}
					} else if (media != null) {
						if (media.isMpegTS()) {
							dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMPEG_TS_EULocalizedValue(localizationValue, media.isHDVideo());
							if (media.isH264()) {
								dlnaOrgPnFlags = "DLNA.ORG_PN=AVC_TS_HD_50_AC3";
								if (mediaRenderer.isTranscodeToMPEGTSH264AAC()) {
									dlnaOrgPnFlags = "DLNA.ORG_PN=AVC_TS_HP_HD_AAC";
								}
							}
						}
					}
				} else if (media != null && mime.equals("video/vnd.dlna.mpeg-tts")) {
					// patters - on Sony BDP m2ts clips aren't listed without this
					dlnaOrgPnFlags = "DLNA.ORG_PN=" + getMPEG_TS_EULocalizedValue(localizationValue, media.isHDVideo());
				} else if (media != null && mime.equals(JPEG_TYPEMIME)) {
					int width = media.getWidth();
					int height = media.getHeight();
					if (width > 1024 || height > 768) { // 1024 * 768
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_LRG";
					} else if (width > 640 || height > 480) { // 640 * 480
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_MED";
					} else if (width > 160 || height > 160) { // 160 * 160
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_SM";
					} else {
						dlnaOrgPnFlags = "DLNA.ORG_PN=JPEG_TN";
					}

				} else if (mime.equals(AUDIO_MP3_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=MP3";
				} else if (mime.substring(0, 9).equals(AUDIO_LPCM_TYPEMIME) || mime.equals(AUDIO_WAV_TYPEMIME)) {
					dlnaOrgPnFlags = "DLNA.ORG_PN=LPCM";
				}
			}

			if (dlnaOrgPnFlags != null) {
				dlnaOrgPnFlags = "DLNA.ORG_PN=" + mediaRenderer.getDLNAPN(dlnaOrgPnFlags.substring(12));
			}
		}

		return dlnaOrgPnFlags;
	}

	/**
	 * Gets the media renderer's mime type if available, returns a default mime type otherwise.
	 *
	 * @param mediaRenderer
	 * 			Media Renderer for which to represent this information.
	 * @return String representation of the mime type
	 */
	private String getRendererMimeType(RendererConfiguration mediaRenderer) {
		// FIXME: There is a flaw here. In addChild(DLNAResource) the mime type
		// is determined for the default renderer. This renderer may rewrite the
		// mime type based on its configuration. Looking up that mime type is
		// not guaranteed to return a match for another renderer.
		String mime = mediaRenderer.getMimeType(this);

		// Use our best guess if we have no valid mime type
		if (mime == null || mime.contains("/transcode")) {
			mime = HTTPResource.getDefaultMimeType(getType());
		}

		return mime;
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
	 *         {@code <container id="0$1" childCount="1" parentID="0" restricted="1">}
	 */
	public final String getDidlString(RendererConfiguration mediaRenderer) {
		// Use device-specific DMS conf, if any
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(mediaRenderer);
		StringBuilder sb = new StringBuilder();
		boolean subsAreValidForStreaming = false;
		boolean xbox360 = mediaRenderer.isXbox360();
		// Cache this as some implementations actually call the file system
		boolean isFolder = isFolder();
		if (!isFolder) {
			if (media != null && media.isVideo()) {
				if (
					!configurationSpecificToRenderer.isDisableSubtitles() &&
					(player == null || mediaRenderer.streamSubsForTranscodedVideo()) &&
					media_subtitle != null &&
					media_subtitle.isExternal() &&
					mediaRenderer.isExternalSubtitlesFormatSupported(media_subtitle, media)
				) {
					subsAreValidForStreaming = true;
					LOGGER.trace("External subtitles \"{}\" can be streamed to {}", media_subtitle.getName(), mediaRenderer);
				} else if (media_subtitle != null && LOGGER.isTraceEnabled()) {
					if (configurationSpecificToRenderer.isDisableSubtitles()) {
						LOGGER.trace("Subtitles are disabled");
					} else if (media_subtitle.isEmbedded()) {
						LOGGER.trace("Subtitles track {} cannot be streamed because it is internal/embedded", media_subtitle.getId());
					} else {
						LOGGER.trace("Subtitles \"{}\" aren't valid for streaming to {}", media_subtitle.getName(), mediaRenderer);
					}
				}
			}

			openTag(sb, "item");
		} else {
			openTag(sb, "container");
		}

		String id = getResourceId();
		if (xbox360) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual folder ids.
			id += "$";
		}

		addAttribute(sb, "id", id);
		if (isFolder) {
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

		id = getParentId();
		if (xbox360 && getFakeParentId() == null) {
			// Ensure the xbox 360 doesn't confuse our ids with its own virtual folder ids.
			id += "$";
		}

		addAttribute(sb, "parentID", id);
		addAttribute(sb, "restricted", "1");
		endTag(sb);
		final DLNAMediaAudio firstAudioTrack = media != null ? media.getFirstAudioTrack() : null;

		/*
		 * Use the track title for audio files, otherwise use the filename.
		 */
		String title;
		if (
			firstAudioTrack != null &&
			media.isAudio() &&
			StringUtils.isNotBlank(firstAudioTrack.getSongname())
		) {
			title = "";
			if (mediaRenderer != null && mediaRenderer.isPrependTrackNumbers() && firstAudioTrack.getTrack() > 0) {
				// zero pad for proper numeric sorting on all devices
				title += String.format("%03d - ", firstAudioTrack.getTrack());
			}
			title += firstAudioTrack.getSongname();
		} else if (isFolder || subsAreValidForStreaming) {
			title = getDisplayName(mediaRenderer, false);
		} else {
			title = mediaRenderer.getUseSameExtension(getDisplayName(mediaRenderer, false));
		}
		if (!mediaRenderer.isThumbnails() && this instanceof RealFile && FullyPlayed.isFullyPlayedMark(((RealFile) this).getFile())) {
			title = FullyPlayed.addFullyPlayedNamePrefix(title, this);
		}

		title = resumeStr(title);
		addXMLTagAndAttribute(
			sb,
			"dc:title",
			encodeXML(mediaRenderer.getDcTitle(title, getDisplayNameSuffix(mediaRenderer, configurationSpecificToRenderer), this))
		);
		
		if (mediaRenderer.isSamsung() && this instanceof RealFile) {
			addBookmark(sb, mediaRenderer.getDcTitle(title, getDisplayNameSuffix(mediaRenderer, configurationSpecificToRenderer), this));
		}

		if (firstAudioTrack != null) {
			if (StringUtils.isNotBlank(firstAudioTrack.getAlbum())) {
				addXMLTagAndAttribute(sb, "upnp:album", encodeXML(firstAudioTrack.getAlbum()));
			}

			//TODO maciekberry: check whether it makes sense to use Album Artist
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

		MediaType mediaType = media != null ? media.getMediaType() : MediaType.UNKNOWN;
		if (!isFolder && mediaType == MediaType.IMAGE) {
			appendImage(sb, mediaRenderer);
		} else if (!isFolder) {
			int indexCount = 1;
			if (mediaRenderer.isDLNALocalizationRequired()) {
				indexCount = getDLNALocalesCount();
			}

			for (int c = 0; c < indexCount; c++) {
				openTag(sb, "res");
				addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
				String dlnaOrgPnFlags = getDlnaOrgPnFlags(mediaRenderer, c);
				String tempString = "http-get:*:" + getRendererMimeType(mediaRenderer) + ":" + (dlnaOrgPnFlags != null ? (dlnaOrgPnFlags + ";") : "") + getDlnaOrgOpFlags(mediaRenderer);
				addAttribute(sb, "protocolInfo", tempString);
				if (subsAreValidForStreaming && mediaRenderer.offerSubtitlesByProtocolInfo() && !mediaRenderer.useClosedCaption()) {
					addAttribute(sb, "pv:subtitleFileType", media_subtitle.getType().getExtension().toUpperCase());
					addAttribute(sb, "pv:subtitleFileUri", getSubsURL(media_subtitle));
				}

				if (getFormat() != null && getFormat().isVideo() && media != null && media.isMediaparsed()) {
					if (player == null) {
						addAttribute(sb, "size", media.getSize());
					} else {
						long transcoded_size = mediaRenderer.getTranscodedSize();
						if (transcoded_size != 0) {
							addAttribute(sb, "size", transcoded_size);
						}
					}

					if (media.getDuration() != null) {
						if (getSplitRange().isEndLimitAvailable()) {
							addAttribute(sb, "duration", StringUtil.formatDLNADuration(getSplitRange().getDuration()));
						} else {
							addAttribute(sb, "duration", media.getDurationString());
						}
					}

					if (media.getResolution() != null) {
						if (player != null && mediaRenderer.isKeepAspectRatio()) {
							addAttribute(sb, "resolution", getResolutionForKeepAR(media.getWidth(), media.getHeight()));
						} else {
							addAttribute(sb, "resolution", media.getResolution());
						}

					}

					addAttribute(sb, "bitrate", media.getRealVideoBitrate());
					if (firstAudioTrack != null) {
						if (firstAudioTrack.getAudioProperties().getNumberOfChannels() > 0) {
							addAttribute(sb, "nrAudioChannels", firstAudioTrack.getAudioProperties().getNumberOfChannels());
						}

						if (firstAudioTrack.getSampleFrequency() != null) {
							addAttribute(sb, "sampleFrequency", firstAudioTrack.getSampleFrequency());
						}
					}
					if (media.getVideoBitDepth() > 0) {
						addAttribute(sb, "colorDepth", media.getVideoBitDepth());
					}
				} else if (getFormat() != null && getFormat().isImage()) {
					if (media != null && media.isMediaparsed()) {
						addAttribute(sb, "size", media.getSize());
						if (media.getResolution() != null) {
							addAttribute(sb, "resolution", media.getResolution());
						}
					} else {
						addAttribute(sb, "size", length());
					}
				} else if (getFormat() != null && getFormat().isAudio()) {
					if (media != null && media.isMediaparsed()) {
						if (media.getBitrate() > 0) {
							addAttribute(sb, "bitrate", media.getBitrate());
						}
						if (media.getDuration() != null && media.getDuration().doubleValue() != 0.0) {
							addAttribute(sb, "duration", StringUtil.formatDLNADuration(media.getDuration()));
						}

						int transcodeFrequency = -1;
						int transcodeNumberOfChannels = -1;
						if (firstAudioTrack != null) {
							if (player == null) {
								if (firstAudioTrack.getSampleFrequency() != null) {
									addAttribute(sb, "sampleFrequency", firstAudioTrack.getSampleFrequency());
								}
								if (firstAudioTrack.getAudioProperties().getNumberOfChannels() > 0) {
									addAttribute(sb, "nrAudioChannels", firstAudioTrack.getAudioProperties().getNumberOfChannels());
								}
							} else {
								if (configurationSpecificToRenderer.isAudioResample()) {
									transcodeFrequency = mediaRenderer.isTranscodeAudioTo441() ? 44100 : 48000;
									transcodeNumberOfChannels = 2;
								} else {
									transcodeFrequency = firstAudioTrack.getSampleRate();
									transcodeNumberOfChannels = firstAudioTrack.getAudioProperties().getNumberOfChannels();
								}
								if (transcodeFrequency > 0) {
									addAttribute(sb, "sampleFrequency", transcodeFrequency);
								}
								if (transcodeNumberOfChannels > 0) {
									addAttribute(sb, "nrAudioChannels", transcodeNumberOfChannels);
								}
							}
						}

						if (player == null) {
							if (media.getSize() != 0) {
								addAttribute(sb, "size", media.getSize());
							}
						} else {
							// Calculate WAV size
							if (
								firstAudioTrack != null &&
								media.getDurationInSeconds() > 0.0 &&
								transcodeFrequency > 0 &&
								transcodeNumberOfChannels > 0
							) {
								int finalSize = (int) (media.getDurationInSeconds() * transcodeFrequency * 2 * transcodeNumberOfChannels);
								LOGGER.trace("Calculated transcoded size for {}: {}", getSystemName(), finalSize);
								addAttribute(sb, "size", finalSize);
							} else if (media.getSize() > 0){
								LOGGER.trace("Could not calculate transcoded size for {}, using file size: {}", getSystemName(), media.getSize());
								addAttribute(sb, "size", media.getSize());
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
				// Add transcoded format extension to the output stream URL.
				String transcodedExtension = "";
				if (player != null && media != null) {
					// Note: Can't use instanceof below because the audio classes inherit the corresponding video class
					if (media.isVideo()) {
						if (mediaRenderer.getCustomFFmpegOptions().contains("-f avi")) {
							transcodedExtension = "_transcoded_to.avi";
						} else if (mediaRenderer.getCustomFFmpegOptions().contains("-f flv")) {
							transcodedExtension = "_transcoded_to.flv";
						} else if (mediaRenderer.getCustomFFmpegOptions().contains("-f matroska")) {
							transcodedExtension = "_transcoded_to.mkv";
						} else if (mediaRenderer.getCustomFFmpegOptions().contains("-f mov")) {
							transcodedExtension = "_transcoded_to.mov";
						} else if (mediaRenderer.getCustomFFmpegOptions().contains("-f webm")) {
							transcodedExtension = "_transcoded_to.webm";
						} else if (mediaRenderer.isTranscodeToMPEGTS()) {
							transcodedExtension = "_transcoded_to.ts";
						} else if (mediaRenderer.isTranscodeToWMV() && !xbox360) {
							transcodedExtension = "_transcoded_to.wmv";
						} else {
							transcodedExtension = "_transcoded_to.mpg";
						}
					} else if (media.isAudio()) {
						if (mediaRenderer.isTranscodeToMP3()) {
							transcodedExtension = "_transcoded_to.mp3";
						} else if (mediaRenderer.isTranscodeToWAV()) {
							transcodedExtension = "_transcoded_to.wav";
						} else {
							transcodedExtension = "_transcoded_to.pcm";
						}
					}
				}

				sb.append(getFileURL()).append(transcodedExtension);
				closeTag(sb, "res");
			}
		}

		if (subsAreValidForStreaming) {
			String subsURL = getSubsURL(media_subtitle);
			if (mediaRenderer.useClosedCaption()) {
				openTag(sb, "sec:CaptionInfoEx");
				addAttribute(sb, "sec:type", "srt");
				endTag(sb);
				sb.append(subsURL);
				closeTag(sb, "sec:CaptionInfoEx");
				LOGGER.trace("Network debugger: sec:CaptionInfoEx: sec:type=srt " + subsURL);
			} else if (mediaRenderer.offerSubtitlesAsResource()){
				openTag(sb, "res");
				String subtitlesFormat = media_subtitle.getType().getExtension();
				if (StringUtils.isBlank(subtitlesFormat)) {
					subtitlesFormat = "plain";
				}

				addAttribute(sb, "protocolInfo", "http-get:*:text/" + subtitlesFormat + ":*");
				endTag(sb);
				sb.append(subsURL);
				closeTag(sb, "res");
				LOGGER.trace("Network debugger: http-get:*:text/" + subtitlesFormat + ":*" + subsURL);
			}
		}

		if (
			mediaType != MediaType.IMAGE && (
				!isFolder ||
				mediaRenderer.isSendFolderThumbnails() ||
				this instanceof DVDISOFile
			)
		) {
			appendThumbnail(sb, mediaType, mediaRenderer);
		}

		if (getLastModified() > 0 && mediaRenderer.isSendDateMetadata()) {
			addXMLTagAndAttribute(sb, "dc:date", SDF_DATE.format(new Date(getLastModified())));
		}

		String uclass;
		if (first != null && media != null && !media.isSecondaryFormatValid()) {
			uclass = "dummy";
		} else if (isFolder) {
			uclass = "object.container.storageFolder";
			if (xbox360 && getFakeParentId() != null) {
				switch (getFakeParentId()) {
					case "7":
						uclass = "object.container.album.musicAlbum";
						break;
					case "6":
						uclass = "object.container.person.musicArtist";
						break;
					case "5":
						uclass = "object.container.genre.musicGenre";
						break;
					case "F":
						uclass = "object.container.playlistContainer";
						break;
				}
			}
		} else if (
			mediaType == MediaType.IMAGE ||
			mediaType == MediaType.UNKNOWN &&
			format != null &&
			format.isImage()
		) {
			uclass = "object.item.imageItem.photo";
		} else if (
			mediaType == MediaType.AUDIO ||
			mediaType == MediaType.UNKNOWN &&
			format != null &&
			format.isAudio()
		) {
			uclass = "object.item.audioItem.musicTrack";
		} else {
			uclass = "object.item.videoItem";
		}

		addXMLTagAndAttribute(sb, "upnp:class", uclass);
		if (isFolder) {
			closeTag(sb, "container");
		} else {
			closeTag(sb, "item");
		}

		return sb.toString();
	}
	
	private void addBookmark(StringBuilder sb, String title){
		try {
			File file = new File(getFileName());
			String path = file.getCanonicalPath();
			int bookmark = TableFilesStatus.getBookmark(path);
			LOGGER.debug("Setting bookmark for " + path + " => " + bookmark);
			addXMLTagAndAttribute(
					sb,
					"sec:dcmInfo",
					encodeXML(String.format("CREATIONDATE=0,FOLDER=%s,BM=%d",title,bookmark))
				);
		} catch(Exception e) {
			LOGGER.error("Cannot set bookmark tag for " + title, e);
		}
	}

	/**
	 * Generate and append image and thumbnail {@code res} and
	 * {@code upnp:albumArtURI} entries for the image.
	 *
	 * @param sb The {@link StringBuilder} to append the elements to.
	 * @param renderer the {@link Renderer} used for filtering or {@code null}
	 *            for no filtering.
	 */
	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	private void appendImage(StringBuilder sb, Renderer renderer) {
		/*
		 * There's no technical difference between the image itself and the
		 * thumbnail for an object.item.imageItem, they are all simply listed
		 * as <res> entries. To DMS there is a difference since the thumbnail
		 * is cached while the image itself is not. The idea here is therefore
		 * to offer any size smaller than or equal to the cached thumbnail
		 * using the cached thumbnail as the source, and offer anything bigger
		 * using the image itself as the source.
		 *
		 * If the thumbnail isn't parsed
		 * yet, we don't know the size of the thumbnail. In those situations
		 * we simply use the thumbnail for the _TN entries and the image for
		 * all others.
		 */

		ImageInfo imageInfo = media.getImageInfo();
		ImageInfo thumbnailImageInfo = this.thumbnailImageInfo != null ?
			this.thumbnailImageInfo :
			getMedia() != null && getMedia().getThumb() != null ?
				getMedia().getThumb().getImageInfo() :
				null;

		// Only include GIF elements if the source is a GIF and it's supported by the renderer.
		boolean includeGIF =
			imageInfo != null &&
			imageInfo.getFormat() == ImageFormat.GIF &&
			DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.GIF_LRG, renderer);

		// Add elements in any order, it's sorted by priority later
		List<DLNAImageResElement> resElements = new ArrayList<>();

		// Always offer JPEG_TN as per DLNA standard
		resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_TN, thumbnailImageInfo != null ? thumbnailImageInfo : imageInfo, true));
		if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_TN, renderer)) {
			resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_TN, thumbnailImageInfo != null ? thumbnailImageInfo : imageInfo, true));
		}
		if (imageInfo != null) {
			if (
				DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_RES_H_V, renderer) &&
				imageInfo.getWidth() > 0 &&
				imageInfo.getHeight() > 0
			) {
				// Offer the exact resolution as JPEG_RES_H_V
				DLNAImageProfile exactResolution =
					DLNAImageProfile.createJPEG_RES_H_V(imageInfo.getWidth(), imageInfo.getHeight());
				resElements.add(new DLNAImageResElement(
					exactResolution, imageInfo,
					exactResolution.useThumbnailSource(imageInfo, thumbnailImageInfo)
				));
			}
			// Always offer JPEG_SM for images as per DLNA standard
			resElements.add(new DLNAImageResElement(
				DLNAImageProfile.JPEG_SM, imageInfo,
				DLNAImageProfile.JPEG_SM.useThumbnailSource(imageInfo, thumbnailImageInfo)
			));
			if (!DLNAImageProfile.PNG_TN.isResolutionCorrect(imageInfo)) {
				if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_LRG, renderer)) {
					resElements.add(new DLNAImageResElement(
						DLNAImageProfile.PNG_LRG, imageInfo,
						DLNAImageProfile.PNG_LRG.useThumbnailSource(imageInfo, thumbnailImageInfo)
					));
				}
				if (includeGIF) {
					resElements.add(new DLNAImageResElement(
						DLNAImageProfile.GIF_LRG, imageInfo,
						DLNAImageProfile.GIF_LRG.useThumbnailSource(imageInfo, thumbnailImageInfo)
					));
				}
				if (!DLNAImageProfile.JPEG_SM.isResolutionCorrect(imageInfo)) {
					if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_MED, renderer)) {
						resElements.add(new DLNAImageResElement(
							DLNAImageProfile.JPEG_MED, imageInfo,
							DLNAImageProfile.JPEG_MED.useThumbnailSource(imageInfo, thumbnailImageInfo)
						));
					}
					if (!DLNAImageProfile.JPEG_MED.isResolutionCorrect(imageInfo)) {
						if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_LRG, renderer)) {
							resElements.add(new DLNAImageResElement(
								DLNAImageProfile.JPEG_LRG, imageInfo,
								DLNAImageProfile.JPEG_LRG.useThumbnailSource(imageInfo, thumbnailImageInfo)
							));
						}
					}
				}
			}
		} else {
			// This shouldn't normally be the case, parsing must have failed or
			// isn't finished yet so we just make a generic offer.

			// Always offer JPEG_SM for images as per DLNA standard
			resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_SM, null, false));
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_LRG, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_LRG, null, false));
			}
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_LRG, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_LRG, null, false));
			}
			LOGGER.debug("Warning: Image \"{}\" wasn't parsed when DIDL-Lite was generated", this.getName());
		}

		// Sort the elements by priority
		Collections.sort(resElements, DLNAImageResElement.getComparator(imageInfo != null ? imageInfo.getFormat() : ImageFormat.JPEG));

		for (DLNAImageResElement resElement : resElements) {
			addImageResource(sb, resElement);
		}

		for (DLNAImageResElement resElement : resElements) {
			// Offering AlbumArt here breaks the standard, but some renderers need it
			switch (resElement.getProfile().toInt()) {
				case DLNAImageProfile.GIF_LRG_INT:
				case DLNAImageProfile.JPEG_SM_INT:
				case DLNAImageProfile.JPEG_TN_INT:
				case DLNAImageProfile.PNG_LRG_INT:
				case DLNAImageProfile.PNG_TN_INT:
					addAlbumArt(sb, resElement.getProfile());
			}
		}
	}


	/**
	 * Generate and append the thumbnail {@code res} and
	 * {@code upnp:albumArtURI} entries for the thumbnail.
	 *
	 * @param sb the {@link StringBuilder} to append the response to.
	 * @param mediaType the {@link MediaType} of this {@link DLNAResource}.
	 * @param renderer the {@link Renderer} used for filtering or {@code null}
	 *            for no filtering.
	 */
	@SuppressFBWarnings("SF_SWITCH_NO_DEFAULT")
	private void appendThumbnail(StringBuilder sb, MediaType mediaType, Renderer renderer) {

		/*
		 * JPEG_TN = Max 160 x 160; EXIF Ver.1.x or later or JFIF 1.02; SRGB or uncalibrated
		 * JPEG_SM = Max 640 x 480; EXIF Ver.1.x or later or JFIF 1.02; SRGB or uncalibrated
		 * PNG_TN = Max 160 x 160; Greyscale 8/16 bit, Truecolor 24 bit, Indexed-color 24 bit, Greyscale with alpha 8/16 bit or Truecolor with alpha 24 bit;
		 * PNG_SM doesn't exist!
		 *
		 * The standard dictates that thumbnails for images and videos should
		 * be given as a <res> element:
		 * > If a UPnP AV MediaServer exposes a CDS object with a <upnp:class>
		 * > designation of object.item.imageItem or object.item.videoItem (or
		 * > any class derived from them), then the UPnP AV MediaServer should
		 * > provide a <res> element for the thumbnail resource. (Multiple
		 * > thumbnail <res> elements are also allowed.)
		 *
		 * It also dictates that if a <res> thumbnail is available, it HAS to
		 * be offered as JPEG_TN (although not exclusively):
		 * > If a UPnP AV MediaServer exposes thumbnail images for image or video
		 * > content, then a UPnP AV MediaServer shall provide a thumbnail that
		 * > conforms to guideline 7.1.7 (GUN 6SXDY) in IEC 62481-2:2013 media
		 * > format profile and be declared with the JPEG_TN designation in the
		 * > fourth field of the res@protocolInfo attribute.
		 * >
		 * > When thumbnails are provided, the minimal expectation is to provide
		 * > JPEG thumbnails. However, vendors can also provide additional
		 * > thumbnails using the JPEG_TN or PNG_TN profiles.
		 *
		 * For videos content additional related images can be offered:
		 * > UPnP AV MediaServers that expose a video item can include in the
		 * > <item> element zero or more <res> elements referencing companion
		 * > images that provide additional descriptive information. Examples
		 * > of companion images include larger versions of thumbnails, posters
		 * > describing a movie, and others.
		 *
		 * For audio content, and ONLY for audio content, >upnp:albumArtURI>
		 * should be used:
		 * > If a UPnP AV MediaServer exposes a CDS object with a <upnp:class>
		 * > designation of object.item.audioItem or object.container.album.musicAlbum
		 * > (or any class derived from either class), then the UPnP AV MediaServer
		 * > should provide a <upnp:albumArtURI> element to present the URI for
		 * > the album art
		 * >
		 * > Unlike image or video content, thumbnails for audio content will
		 * > preferably be presented through the <upnp:albumArtURI> element.
		 *
		 * There's a difference between a thumbnail and album art. A thumbnail
		 * is a miniature still image of visual content, since audio isn't
		 * visual the concept is invalid. Album art is an image "tied to" that
		 * audio, but doesn't represent the audio itself.
		 *
		 * The same requirement of always providing a JPEG_TN applies to
		 * <upnp:albumArtURI> although formulated somewhat vaguer:
		 * > If album art thumbnails are provided, the desired expectation is
		 * > to have JPEG thumbnails. Additional thumbnails can also be provided.
		 */

		// Images add thumbnail resources together with the image resources in appendImage()
		if (MediaType.IMAGE != mediaType) {

			ImageInfo imageInfo = thumbnailImageInfo != null ? thumbnailImageInfo :
				getMedia() != null && getMedia().getThumb() != null && getMedia().getThumb().getImageInfo() != null ?
					getMedia().getThumb().getImageInfo() : null;

			// Only include GIF elements if the source is a GIF and it's supported by the renderer.
			boolean includeGIF =
				imageInfo != null &&
				imageInfo.getFormat() == ImageFormat.GIF &&
				DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.GIF_LRG, renderer);

			// Add elements in any order, it's sorted by priority later
			List<DLNAImageResElement> resElements = new ArrayList<>();

			// Always include JPEG_TN as per DLNA standard
			resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_TN, imageInfo, true));
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_SM, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_SM, imageInfo, true));
			}
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_TN, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_TN, imageInfo, true));
			}
			if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.PNG_LRG, renderer)) {
				resElements.add(new DLNAImageResElement(DLNAImageProfile.PNG_LRG, imageInfo, true));
			}

			if (imageInfo != null) {
				if (
					DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_RES_H_V, renderer) &&
					imageInfo.getWidth() > 0 &&
					imageInfo.getHeight() > 0
				) {
					// Offer the exact resolution as JPEG_RES_H_V
					DLNAImageProfile exactResolution =
						DLNAImageProfile.createJPEG_RES_H_V(imageInfo.getWidth(), imageInfo.getHeight());
					resElements.add(new DLNAImageResElement(exactResolution, imageInfo, true));
				}
				if (includeGIF) {
					resElements.add(new DLNAImageResElement(DLNAImageProfile.GIF_LRG, imageInfo, true));
				}
				if (!DLNAImageProfile.JPEG_SM.isResolutionCorrect(imageInfo)) {
					if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_MED, renderer)) {
						resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_MED, imageInfo, true));
					}
					if (!DLNAImageProfile.JPEG_MED.isResolutionCorrect(imageInfo)) {
						if (DLNAImageResElement.isImageProfileSupported(DLNAImageProfile.JPEG_LRG, renderer)) {
							resElements.add(new DLNAImageResElement(DLNAImageProfile.JPEG_LRG, imageInfo, true));
						}
					}
				}
			}

			// Sort the elements by priority
			Collections.sort(resElements, DLNAImageResElement.getComparator(imageInfo != null ? imageInfo.getFormat() : ImageFormat.JPEG));

			for (DLNAImageResElement resElement : resElements) {
				addImageResource(sb, resElement);
			}

			for (DLNAImageResElement resElement : resElements) {
				// Offering AlbumArt for video breaks the standard, but some renderers need it
				switch (resElement.getProfile().toInt()) {
					case DLNAImageProfile.GIF_LRG_INT:
					case DLNAImageProfile.JPEG_SM_INT:
					case DLNAImageProfile.JPEG_TN_INT:
					case DLNAImageProfile.PNG_LRG_INT:
					case DLNAImageProfile.PNG_TN_INT:
						addAlbumArt(sb, resElement.getProfile());
				}
			}
		}
	}

	private void addImageResource(StringBuilder sb, DLNAImageResElement resElement) {
		if (resElement == null) {
			throw new NullPointerException("resElement cannot be null");
		}
		if (!resElement.isResolutionKnown() && DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile())) {
			throw new IllegalArgumentException("Resolution cannot be unknown for DLNAImageProfile.JPEG_RES_H_V");
		}
		String url;
		if (resElement.isThumbnail()) {
			url = getThumbnailURL(resElement.getProfile());
		} else {
			url = getURL(
				(DLNAImageProfile.JPEG_RES_H_V.equals(resElement.getProfile()) ?
					"JPEG_RES" + resElement.getWidth() + "x" + resElement.getHeight() :
					resElement.getProfile().toString()
				) + "_"
			);
		}
		if (StringUtils.isNotBlank(url)) {
			String ciFlag;
			/*
			 * Some Panasonic TV's can't handle if the thumbnails have the CI
			 * flag set to 0 while the main resource doesn't have a CI flag.
			 * DLNA dictates that a missing CI flag should be interpreted as
			 * if it were 0, so the result should be the same.
			 */
			if (resElement.getCiFlag() == null || resElement.getCiFlag() == 0) {
				ciFlag = "";
			} else {
				ciFlag = ";DLNA.ORG_CI=" + resElement.getCiFlag().toString();
			}
			openTag(sb, "res");
			if (resElement.getSize() != null && resElement.getSize() > 0) {
				addAttribute(sb, "size", resElement.getSize());
			}
			if (resElement.isResolutionKnown()) {
				addAttribute(sb, "resolution", Integer.toString(resElement.getWidth()) + "x" + Integer.toString(resElement.getHeight()));
			}

			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			addAttribute(sb,
				"protocolInfo",
				"http-get:*:" + resElement.getProfile().getMimeType() + ":DLNA.ORG_PN=" +
				resElement.getProfile() +
				ciFlag + ";DLNA.ORG_FLAGS=00900000000000000000000000000000"
			);
			endTag(sb);
			sb.append(url);
			closeTag(sb, "res");
		}
	}

	private void addAlbumArt(StringBuilder sb, DLNAImageProfile thumbnailProfile) {
		String albumArtURL = getThumbnailURL(thumbnailProfile);
		if (StringUtils.isNotBlank(albumArtURL)) {
			openTag(sb, "upnp:albumArtURI");
			addAttribute(sb, "dlna:profileID", thumbnailProfile);
			addAttribute(sb, "xmlns:dlna", "urn:schemas-dlna-org:metadata-1-0/");
			endTag(sb);
			sb.append(albumArtURL);
			closeTag(sb, "upnp:albumArtURI");
		}
	}

	private String getRequestId(String rendererId) {
		return String.format("%s|%x|%s", rendererId, hashCode(), getSystemName());
	}

	/**
	 * Plugin implementation. When this item is going to play, it will notify all the
	 * StartStopListener objects available.
	 *
	 * @param rendererId
	 * @param incomingRenderer
	 *
	 * @see StartStopListener
	 */
	public void startPlaying(final String rendererId, final RendererConfiguration incomingRenderer) {
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
							RendererConfiguration renderer;
							if (incomingRenderer == null) {
								renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(rendererIp);
							} else {
								renderer = incomingRenderer;
							}

							String rendererName = "unknown renderer";
							try {
								renderer.setPlayingRes(self);
								rendererName = renderer.getRendererName().replaceAll("\n", "");
							} catch (NullPointerException e) { }
							if (!quietPlay()) {
								LOGGER.info("Started playing " + getName() + " on your " + rendererName);
								LOGGER.debug("The full filename of which is: " + getSystemName() + " and the address of the renderer is: " + rendererId);
							}
						} catch (UnknownHostException ex) {
							LOGGER.debug("" + ex);
						}

						startTime = System.currentTimeMillis();
					}
				};

				new Thread(r, "StartPlaying Event").start();
			}
		}
	}

	/**
	 * Plugin implementation. When this item is going to stop playing, it will notify all the StartStopListener
	 * objects available.
	 *
	 * @see StartStopListener
	 */
	public void stopPlaying(final String rendererId, final RendererConfiguration incomingRenderer) {
		final DLNAResource self = this;
		final String requestId = getRequestId(rendererId);
		Runnable defer = new Runnable() {
			@Override
			public void run() {
				long start = startTime;
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
					if (start != startTime) {
						return;
					}

					Runnable r = new Runnable() {
						@Override
						public void run() {
							if (refCount == 1) {
								requestIdToRefcount.put(requestId, 0);
								InetAddress rendererIp;
								try {
									rendererIp = InetAddress.getByName(rendererId);
									RendererConfiguration renderer;
									if (incomingRenderer == null) {
										renderer = RendererConfiguration.getRendererConfigurationBySocketAddress(rendererIp);
									} else {
										renderer = incomingRenderer;
									}

									String rendererName = "unknown renderer";
									try {
										// Reset only if another item hasn't already begun playing
										if (renderer.getPlayingRes() == self) {
											renderer.setPlayingRes(null);
										}
										rendererName = renderer.getRendererName();
									} catch (NullPointerException e) { }

									if (!quietPlay()) {
										LOGGER.info("Stopped playing {} on {}", getName(), rendererName);
										LOGGER.debug(
											"The full filename of which is \"{}\" and the address of the renderer is {}",
											getSystemName(),
											rendererId
										);
									}
								} catch (UnknownHostException ex) {
									LOGGER.debug("" + ex);
								}

								internalStop();
							}
						}
					};

					new Thread(r, "StopPlaying Event").start();
				}
				if (media_subtitle instanceof DLNAMediaOpenSubtitle) {
					((DLNAMediaOpenSubtitle) media_subtitle).deleteLiveSubtitlesFile();
				}
			}
		};

		new Thread(defer, "StopPlaying Event Deferrer").start();
	}

	/**
	 * The system time when the resource was last (re)started.
	 */
	private long lastStartSystemTime;

	/**
	 * Gets the system time when the resource was last (re)started.
	 *
	 * @return The system time when the resource was last (re)started
	 */
	public double getLastStartSystemTime() {
		return lastStartSystemTime;
	}

	/**
	 * Sets the system time when the resource was last (re)started.
	 *
	 * @param startTime the system time to set
	 */
	public void setLastStartSystemTime(long startTime) {
		lastStartSystemTime = startTime;
	}

	/**
	 * The most recently requested time offset in seconds.
	 */
	private double lastStartPosition;

	/**
	 * Gets the most recently requested time offset in seconds.
	 *
	 * @return The most recently requested time offset in seconds
	 */
	public double getLastStartPosition() {
		return lastStartPosition;
	}

	/**
	 * Sets the most recently requested time offset in seconds.
	 *
	 * @param startPosition the time offset in seconds
	 */
	public void setLastStartPosition(long startPosition) {
		lastStartPosition = startPosition;
	}

	/**
	 * Returns an InputStream of this DLNAResource that starts at a given time, if possible. Very useful if video chapters are being used.
	 *
	 * @param range
	 * @param mediarenderer
	 * @return The inputstream
	 * @throws IOException
	 */
	public synchronized InputStream getInputStream(Range range, RendererConfiguration mediarenderer) throws IOException {
		// Use device-specific DMS conf, if any
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(mediarenderer);
		LOGGER.trace("Asked stream chunk : " + range + " of " + getName() + " and player " + player);

		// shagrath: small fix, regression on chapters
		boolean timeseek_auto = false;
		// Ditlew - WDTV Live
		// Ditlew - We convert byteoffset to timeoffset here. This needs the stream to be CBR!
		int cbr_video_bitrate = mediarenderer.getCBRVideoBitrate();
		long low = range.isByteRange() && range.isStartOffsetAvailable() ? range.asByteRange().getStart() : 0;
		long high = range.isByteRange() && range.isEndLimitAvailable() ? range.asByteRange().getEnd() : -1;
		Range.Time timeRange = range.createTimeRange();
		if (player != null && low > 0 && cbr_video_bitrate > 0) {
			int used_bit_rated = (int) ((cbr_video_bitrate + 256) * 1024 / (double) 8 * 1.04); // 1.04 = container overhead
			if (low > used_bit_rated) {
				timeRange.setStart(low / (double) (used_bit_rated));
				low = 0;

				// WDTV Live - if set to TS it asks multiple times and ends by
				// asking for an invalid offset which kills MEncoder
				if (timeRange.getStartOrZero() > media.getDurationInSeconds()) {
					return null;
				}

				// Should we rewind a little (in case our overhead isn't accurate enough)
				int rewind_secs = mediarenderer.getByteToTimeseekRewindSeconds();
				timeRange.rewindStart(rewind_secs);

				// shagrath:
				timeseek_auto = true;
			}
		}

		if (low > 0 && media.getBitrate() > 0) {
			lastStartPosition = (low * 8) / (double) media.getBitrate();
			LOGGER.trace("Estimating seek position from byte range:");
			LOGGER.trace("   media.getBitrate: " + media.getBitrate());
			LOGGER.trace("   low: " + low);
			LOGGER.trace("   lastStartPosition: " + lastStartPosition);
		} else {
			lastStartPosition = timeRange.getStartOrZero();
			LOGGER.trace("Setting lastStartPosition from time-seeking: " + lastStartPosition);
		}

		// Determine source of the stream
		if (player == null && !isResume()) {
			// No transcoding
			if (this instanceof IPushOutput) {
				PipedOutputStream out = new PipedOutputStream();
				InputStream fis = new PipedInputStream(out);
				((IPushOutput) this).push(out);

				if (low > 0) {
					fis.skip(low);
				}
				// http://www.ps3mediaserver.org/forum/viewtopic.php?f=11&t=12035

				lastStartSystemTime = System.currentTimeMillis();
				return wrap(fis, high, low);
			}

			 InputStream fis = getInputStream();

			if (fis != null) {
				if (low > 0) {
					fis.skip(low);
				}

				// http://www.ps3mediaserver.org/forum/viewtopic.php?f=11&t=12035
				fis = wrap(fis, high, low);
				if (timeRange.getStartOrZero() > 0 && this instanceof RealFile) {
					fis.skip(MpegUtil.getPositionForTimeInMpeg(((RealFile) this).getFile(), (int) timeRange.getStartOrZero()));
				}
			}

			lastStartSystemTime = System.currentTimeMillis();
			return fis;
		}

		// Pipe transcoding result
		OutputParams params = new OutputParams(configurationSpecificToRenderer);
		params.aid = getMediaAudio();
		params.sid = media_subtitle;
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
			if (range.isTimeRange()) {
				resume.update((Range.Time) range, this);
			}

			params.timeseek = resume.getTimeOffset() / 1000;
			if (player == null) {
				player = PlayerFactory.getPlayer(this);
			}
		}

		if (System.currentTimeMillis() - lastStartSystemTime < 500) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.error(null, e);
			}
		}

		// (Re)start transcoding process if necessary
		if (externalProcess == null || externalProcess.isDestroyed()) {
			// First playback attempt => start new transcoding process
			LOGGER.debug("Starting transcode/remux of " + getName() + " with media info: " + media);
			lastStartSystemTime = System.currentTimeMillis();
			externalProcess = player.launchTranscode(this, media, params);
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
			media != null &&
			media.isMediaparsed() &&
			media.getDurationInSeconds() > 0
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
			lastStartSystemTime = System.currentTimeMillis();
			ProcessWrapper newExternalProcess = player.launchTranscode(this, media, params);
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
		if (is == null && !externalProcess.isDestroyed()) {
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
	public static InputStream wrap(InputStream input, long high, long low) {
		if (input != null && high > low) {
			long bytes = (high - (low < 0 ? 0 : low)) + 1;
			LOGGER.trace("Using size-limiting stream (" + bytes + " bytes)");
			return new SizeLimitInputStream(input, bytes);
		}

		return input;
	}

	public String mimeType() {
		return mimeType(player);
	}

	public String mimeType(Player player) {
		if (player != null) {
			// FIXME: This cannot be right. A player like FFmpeg can output many
			// formats depending on the media and the renderer. Also, players are
			// singletons. Therefore it is impossible to have exactly one mime
			// type to return.
			return player.mimeType();
		} else if (media != null && media.isMediaparsed()) {
			return media.getMimeType();
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

	@Deprecated
	protected void checkThumbnail(InputFile inputFile) {
		checkThumbnail(inputFile, null);
	}

	/**
	 * Checks if a thumbnail exists, and, if not, generates one (if possible).
	 * Called from Request/RequestV2 in response to thumbnail requests e.g. HEAD /get/0$1$0$42$3/thumbnail0000%5BExample.mkv
	 * Calls DLNAMediaInfo.generateThumbnail, which in turn calls DLNAMediaInfo.parse.
	 *
	 * @param inputFile File to check or generate the thumbnail for.
	 * @param renderer The renderer profile
	 */
	protected void checkThumbnail(InputFile inputFile, RendererConfiguration renderer) {
		// Use device-specific DMS conf, if any
		PmsConfiguration configurationSpecificToRenderer = PMS.getConfiguration(renderer);
		if (
			media != null &&
			!media.isThumbready() &&
			configurationSpecificToRenderer.isThumbnailGenerationEnabled() &&
			(renderer == null || renderer.isThumbnails())
		) {
			Double seekPosition = (double) configurationSpecificToRenderer.getThumbnailSeekPos();
			if (isResume()) {
				Double resumePosition = resume.getTimeOffset() / 1000d;

				if (media.getDurationInSeconds() > 0 && resumePosition < media.getDurationInSeconds()) {
					seekPosition = resumePosition;
				}
			}

			media.generateThumbnail(inputFile, getFormat(), getType(), seekPosition, isResume(), renderer);
			if (!isResume() && media.getThumb() != null && configurationSpecificToRenderer.getUseCache() && inputFile.getFile() != null) {
				TableThumbnails.setThumbnail(media.getThumb(), inputFile.getFile().getAbsolutePath());
			}
		}
	}

	/**
	 * Returns the input stream for this resource's generic thumbnail,
	 * which is the first of:
	 *      <li> its Format icon, if any
	 *      <li> the fallback image, if any
	 *      <li> the {@link GenericIcons} icon
	 * <br><br>
	 * This is a wrapper around {@link #getGenericThumbnailInputStream0()} that
	 * stores the {@link ImageInfo} before returning the
	 * {@link InputStream}.
	 *
	 * @param fallback
	 *            the fallback image, or {@code null}.
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	public final DLNAThumbnailInputStream getGenericThumbnailInputStream(String fallback) throws IOException {
		DLNAThumbnailInputStream inputStream = getGenericThumbnailInputStreamInternal(fallback);
		thumbnailImageInfo = inputStream != null ? inputStream.getImageInfo() : null;
		return inputStream;
	}

	/**
	 * Returns the input stream for this resource's generic thumbnail,
	 * which is the first of:
	 *      <li> its Format icon, if any
	 *      <li> the fallback image, if any
	 *      <li> the {@link GenericIcons} icon
	 * <br><br>
	 * @param fallback
	 *            the fallback image, or {@code null}.
	 *
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	protected DLNAThumbnailInputStream getGenericThumbnailInputStreamInternal(String fallback) throws IOException {
		String thumb = fallback;
		if (format != null && format.getIcon() != null) {
			thumb = format.getIcon();
		}

		// Thumb could be:
		if (thumb != null && isCodeValid(this)) {
			// A local file
			if (new File(thumb).exists()) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(new FileInputStream(thumb));
			}

			// A jar resource
			InputStream is;
			if ((is = getResourceInputStream(thumb)) != null) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(is);
			}

			// A URL
			try {
				return DLNAThumbnailInputStream.toThumbnailInputStream(downloadAndSend(thumb, true));
			} catch (Exception e) {}
		}

		// Or none of the above
		if (isFolder()) {
			return GenericIcons.INSTANCE.getGenericFolderIcon();
		}
		return GenericIcons.INSTANCE.getGenericIcon(this);
	}

	/**
	 * Returns the input stream for this resource's thumbnail
	 * (or a default image if a thumbnail can't be found).
	 * Typically overridden by a subclass.<br>
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
	 * Returns the input stream for this resource's thumbnail
	 * (or a default image if a thumbnail can't be found).
	 * Typically overridden by a subclass.
	 *
	 * @return The {@link DLNAThumbnailInputStream}.
	 * @throws IOException
	 */
	protected DLNAThumbnailInputStream getThumbnailInputStream() throws IOException {
		if (isAvisynth()) {
			return DLNAThumbnailInputStream.toThumbnailInputStream(getResourceInputStream("/images/logo-avisynth.png"));
		}

		return getGenericThumbnailInputStream(null);
	}

	/**
	 * Adds an audio "flag" filter to the specified
	 * {@link BufferedImageFilterChain}. If {@code filterChain} is {@code null}
	 * and a "flag" filter is added, a new {@link BufferedImageFilterChain} is
	 * created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	public BufferedImageFilterChain addAudioFlagFilter(BufferedImageFilterChain filterChain) {
		String audioLanguageCode = media_audio != null ? media_audio.getLang() : null;
		if (isNotBlank(audioLanguageCode)) {
			if (filterChain == null) {
				filterChain = new BufferedImageFilterChain();
			}
			filterChain.add(new ImagesUtil.AudioFlagFilter(audioLanguageCode, THUMBNAIL_HINTS));
		}
		return filterChain;
	}

	/**
	 * Adds a subtitles "flag" filter to the specified
	 * {@link BufferedImageFilterChain}. If {@code filterChain} is {@code null}
	 * and a "flag" filter is added, a new {@link BufferedImageFilterChain} is
	 * created.
	 *
	 * @param filterChain the {@link BufferedImageFilterChain} to modify.
	 * @return The resulting {@link BufferedImageFilterChain} or {@code null}.
	 */
	public BufferedImageFilterChain addSubtitlesFlagFilter(BufferedImageFilterChain filterChain) {
		String subsLanguageCode = media_subtitle != null && media_subtitle.getId() != DLNAMediaLang.DUMMY_ID ?
			media_subtitle.getLang() :
			null;

		if (isNotBlank(subsLanguageCode)) {
			if (filterChain == null) {
				filterChain = new BufferedImageFilterChain();
			}
			filterChain.add(new ImagesUtil.SubtitlesFlagFilter(subsLanguageCode, THUMBNAIL_HINTS));
		}
		return filterChain;
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
		// Show audio and subtitles language flags in the TRANSCODE folder only for video files
		if (
			(
				parent instanceof FileTranscodeVirtualFolder ||
				parent instanceof SubSelFile
			) && (
				media_audio != null ||
				media_subtitle != null
			)
		) {
			if (
				(
					media != null &&
					media.isVideo()
				) || (
					media == null &&
					format != null &&
					format.isVideo()
				)
			) {
				filterChain = addAudioFlagFilter(filterChain);
				filterChain = addSubtitlesFlagFilter(filterChain);
			}
		} else if (
			parent instanceof TranscodeVirtualFolder &&
			this instanceof FileTranscodeVirtualFolder &&
			media_subtitle != null
		) {
			filterChain = addSubtitlesFlagFilter(filterChain);
		}
		return filterChain;
	}

	public int getType() {
		if (getFormat() != null) {
			return getFormat().getType();
		}
		return Format.UNKNOWN;
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
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append(" [id=");
		result.append(id);
		result.append(", name=");
		result.append(getName());
		result.append(", full path=");
		result.append(getResourceId());
		result.append(", format=");
		result.append(format);
		result.append(", discovered=");
		result.append(isDiscovered());
		result.append(']');
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
	 *
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
	public void setFormat(Format format) {
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
	public void setMedia(DLNAMediaInfo media) {
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
	public void setMediaSubtitle(DLNAMediaSubtitle mediaSubtitle) {
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
	 * @return Whether this is a TV episode being accessed within a season
	 *         folder in the Media Library.
	 */
	public boolean isEpisodeWithinSeasonFolder() {
		return isEpisodeWithinSeasonFolder;
	}

	/**
	 * Sets whether this is a TV episode being accessed within a season
	 * folder in the Media Library.
	 *
	 * @param isEpisodeWithinSeasonFolder
	 */
	protected void setIsEpisodeWithinSeasonFolder(boolean isEpisodeWithinSeasonFolder) {
		this.isEpisodeWithinSeasonFolder = isEpisodeWithinSeasonFolder;
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
	public void setPlayer(Player player) {
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
	 * Determines whether this resource has external subtitles.
	 *
	 * @return {@code true} if this resource has external subtitles,
	 *         {@code false} otherwise.
	 */
	public boolean hasExternalSubtitles() {
		return hasExternalSubtitles(false);
	}

	/**
	 * Determines whether this resource has external subtitles.
	 *
	 * @param forceRefresh if {@code true} forces a new scan for external
	 *            subtitles instead of relying on cached information (if it
	 *            exists).
	 * @return {@code true} if this resource has external subtitles,
	 *         {@code false} otherwise.
	 */
	public boolean hasExternalSubtitles(boolean forceRefresh) {
		if (media == null || !media.isVideo()) {
			return false;
		}
		synchronized (subtitlesLock) {
			registerExternalSubtitles(forceRefresh);
			return hasExternalSubtitles;
		}
	}

	/**
	 * Determines whether this resource has subtitles.
	 *
	 * @return {@code true} if this resource has subtitles, {@code false}
	 *         otherwise.
	 */
	public boolean hasSubtitles() {
		return hasSubtitles(false);
	}

	/**
	 * Determines whether this resource has subtitles.
	 *
	 * @param forceRefresh if {@code true} forces a new scan for external
	 *            subtitles instead of relying on cached information (if it
	 *            exists).
	 * @return {@code true} if this resource has subtitles, {@code false}
	 *         otherwise.
	 */
	public boolean hasSubtitles(boolean forceRefresh) {
		if (media == null || !media.isVideo()) {
			return false;
		}
		synchronized (subtitlesLock) {
			registerExternalSubtitles(forceRefresh);
			return hasSubtitles;
		}
	}

	/**
	 * Determines whether this resource has internal/embedded subtitles.
	 *
	 * @return {@code true} if this resource has internal/embedded subtitles,
	 *         {@code false} otherwise.
	 */
	public boolean hasEmbeddedSubtitles() {
		if (media == null || !media.isVideo()) {
			return false;
		}

		List<DLNAMediaSubtitle> subtitlesList = media.getSubtitleTracksList();
		if (subtitlesList != null) {
			for (DLNAMediaSubtitle subtitles : subtitlesList) {
				if (subtitles.isEmbedded()) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Resets the cached subtitles information. Mostly useful for cloned
	 * resources.
	 */
	public void resetSubtitlesStatus() {
		synchronized (subtitlesLock) {
			isExternalSubtitlesParsed = false;
			hasExternalSubtitles = false;
			hasSubtitles = false;
		}
	}

	/**
	 * Scans for and registers external subtitles if this is a video resource.
	 * Cached information will be used if it exists unless {@code forceRefresh}
	 * is {@code true}, in which case a new scan will always be done. This also
	 * sets the cached subtitles information for this resource after
	 * registration.
	 *
	 * @param forceRefresh if {@code true} forces a new scan for external
	 *            subtitles instead of relying on cached information (if it
	 *            exists).
	 */
	public void registerExternalSubtitles(boolean forceRefresh) {
		if (media == null || !media.isVideo()) {
			return;
		}

		synchronized (subtitlesLock) {
			if (!forceRefresh && isExternalSubtitlesParsed) {
				return;
			}

			File file = this instanceof RealFile ? ((RealFile) this).getFile() : new File(getFileName());
			if (file == null || media == null) {
				isExternalSubtitlesParsed = true;
				return;
			}

			if (!configuration.isDisableSubtitles() && configuration.isAutoloadExternalSubtitles()) {
				SubtitleUtils.registerExternalSubtitles(file, media, forceRefresh);
				// update the database if enabled
				if (configuration.getUseCache() && media.isMediaparsed() && !media.isParsing()) {
					DLNAMediaDatabase database = PMS.get().getDatabase();

					if (database != null) {
						try {
							database.insertOrUpdateData(file.getAbsolutePath(), file.lastModified(), getType(), media);
						} catch (SQLException e) {
							LOGGER.error(
								"Database error while trying to add parsed information for \"{}\" to the cache: {}",
								file,
								e.getMessage());
							if (LOGGER.isTraceEnabled()) {
								LOGGER.trace("SQL error code: {}", e.getErrorCode());
								if (
									e.getCause() instanceof SQLException &&
									((SQLException) e.getCause()).getErrorCode() != e.getErrorCode()
								) {
									LOGGER.trace("Cause SQL error code: {}", ((SQLException) e.getCause()).getErrorCode());
								}
								LOGGER.trace("", e);
							}
						}
					}
				}
			}

			List<DLNAMediaSubtitle> subtitlesList = media.getSubtitleTracksList();
			if (subtitlesList != null) {
				hasSubtitles = !subtitlesList.isEmpty();
				for (DLNAMediaSubtitle subtitles : subtitlesList) {
					if (subtitles.isExternal()) {
						hasExternalSubtitles = true;
						break;
					}
				}
			}

			isExternalSubtitlesParsed = true;
		}
	}

	/**
	 * Sets external subtitles parsed status to true and sets
	 * {@link #hasSubtitles} and {@link #hasExternalSubtitles} according to the
	 * existing {@link DLNAMediaSubtitle} instances.
	 * <p>
	 * <b>WARNING:</b> This should only be called when the subtitles tracks has
	 * been populated by an alternative source like the database. Setting this
	 * under other circumstances will break the implemented automatic parsing
	 * and caching.
	 */
	public void setExternalSubtitlesParsed() {
		if (media == null || !media.isVideo()) {
			return;
		}

		synchronized (subtitlesLock) {
			if (isExternalSubtitlesParsed) {
				return;
			}

			List<DLNAMediaSubtitle> subtitlesList = media.getSubtitleTracksList();
			hasSubtitles = !subtitlesList.isEmpty();
			for (DLNAMediaSubtitle subtitles : subtitlesList) {
				if (subtitles.isExternal()) {
					hasExternalSubtitles = true;
					break;
				}
			}
			isExternalSubtitlesParsed = true;
		}
	}

	/**
	 * This method figures out which audio track should be used based on
	 * {@link DLNAMediaInfo} metadata and configuration settings.
	 *
	 * @param renderer the {@link RendererConfiguration} from which to get the
	 *            configuration or {@code null} to use the default
	 *            configuration.
	 * @return The resolved {@link DLNAMediaAudio} or {@code null}.
	 */
	public DLNAMediaAudio resolveAudioStream(RendererConfiguration renderer) {
		if (media == null || media.getAudioTrackCount() == 0) {
			LOGGER.trace("Found no audio track");
			return null;
		}
		// Use device-specific pms conf
		PmsConfiguration deviceSpecificConfiguration = PMS.getConfiguration(renderer);

		// check for preferred audio
		DLNAMediaAudio dtsTrack = null;
		StringTokenizer st = new StringTokenizer(deviceSpecificConfiguration.getAudioLanguages(), ",");
		while (st.hasMoreTokens()) {
			String lang = st.nextToken().trim();
			LOGGER.trace(
				"Looking for an audio track with language \"{}\" for \"{}\"",
				lang,
				getName()
			);
			for (DLNAMediaAudio audio : media.getAudioTracksList()) {
				if (audio.matchCode(lang)) {
					LOGGER.trace("Matched audio track: {}", audio);
					return audio;
				}

				if (dtsTrack == null && audio.isDTS()) {
					dtsTrack = audio;
				}
			}
		}

		// preferred audio not found, take a default audio track, dts first if available
		if (dtsTrack != null) {
			LOGGER.trace("Preferring DTS audio track since no language match was found: {}", dtsTrack);
			return dtsTrack;
		}
		DLNAMediaAudio result = media.getFirstAudioTrack();
		LOGGER.trace("Using the first available audio track: {}", result);
		return result;
	}

	/**
	 * This method figures out which subtitles track should be used based on
	 * {@link DLNAMediaInfo} metadata, chosen audio language and configuration
	 * settings.
	 *
	 * @param renderer the {@link RendererConfiguration} from which to get the
	 *            configuration or {@code null} to use the default
	 *            configuration.
	 * @param audioLanguage the {@code ISO 639} language code for the chosen
	 *            audio language or {@code null} if it doesn't apply.
	 * @param forceRefresh if {@code true} forces a new scan for external
	 *            subtitles instead of relying on cached information (if it
	 *            exists).
	 * @return The resolved {@link DLNAMediaSubtitle} or {@code null}.
	 */
	public DLNAMediaSubtitle resolveSubtitlesStream(RendererConfiguration renderer, String audioLanguage, boolean forceRefresh) {
		if (media == null) {
			return null;
		}

		// Use device-specific pms conf
		PmsConfiguration deviceSpecificConfiguration = PMS.getConfiguration(renderer);
		if (deviceSpecificConfiguration.isDisableSubtitles()) {
			LOGGER.trace("Not resolving subtitles since subtitles are disabled");
			return null;
		}

		if (!hasSubtitles(forceRefresh)) {
			return null;
		}

		/*
		 * Check for external and internal subtitles matching the user's
		 * language preferences
		 */
		DLNAMediaSubtitle matchedSub = null;
		boolean useExternal = deviceSpecificConfiguration.isAutoloadExternalSubtitles();
		String audioSubLanguages = deviceSpecificConfiguration.getAudioSubLanguages();
		if (isBlank(audioLanguage) || isBlank(audioSubLanguages)) {
			// Not enough information to do a full audio/subtitles combination search, only use the preferred subtitles
			LOGGER.trace(
				"Searching for subtitles without considering audio language for \"{}\"",
				getName()
			);
			ArrayList<DLNAMediaSubtitle> candidates = new ArrayList<>();
			for (DLNAMediaSubtitle subtitles : media.getSubtitleTracksList()) {
				if (subtitles.isExternal()) {
					if (useExternal) {
						candidates.add(subtitles);
					}
				} else {
					candidates.add(subtitles);
				}
			}
			if (!candidates.isEmpty()) {
				matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, false);
				if (matchedSub != null) {
					LOGGER.trace(
						"Matched {} subtitles track for \"{}\" with unknown audio language: {}",
						matchedSub.isExternal() ? "external" : "internal",
						getName(),
						matchedSub
					);
					return matchedSub;
				}
			}
		} else {
			// Do a full audio/subtitles combination search
			StringTokenizer st = new StringTokenizer(audioSubLanguages.toLowerCase(Locale.ROOT), ";");
			while (st.hasMoreTokens()) {
				String pair = st.nextToken();
				int commaPos = pair.indexOf(',');
				if (commaPos > -1) {
					String audio = pair.substring(0, commaPos).trim();
					String sub = pair.substring(commaPos + 1).trim();
					LOGGER.trace(
						"Searching for a match for audio language \"{}\" with audio \"{}\" and subtitles \"{}\" for \"{}\"",
						audioLanguage,
						audio,
						sub,
						getName()
					);

					if ("*".equals(audio) || DLNAMediaLang.UND.equals(audio) || Iso639.isCodesMatching(audio, audioLanguage)) {
						boolean anyLanguage = "*".equals(sub) || DLNAMediaLang.UND.equals(sub);
						if ("off".equals(sub)) {
							if (deviceSpecificConfiguration.isForceExternalSubtitles()) {
								// Ignore the "off" language for external subtitles if force external subtitles is enabled
								ArrayList<DLNAMediaSubtitle> candidates = new ArrayList<>();
								for (DLNAMediaSubtitle subtitles : media.getSubtitleTracksList()) {
									if (subtitles.isExternal()) {
										candidates.add(subtitles);
									}
								}

								// If external subtitles were found, let findPrioritizedSubtitles return the right one
								if (candidates.isEmpty()) {
									LOGGER.trace(
										"Disabling subtitles because we matched \"off\" and no external" +
										"subtitles candidates were found, returning null for \"{}\"",
										getName()
									);
								} else {
									matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, true);
									LOGGER.trace(
										"Ignoring the \"off\" language because external " +
										"subtitles are enforced, returning: {} for \"{}\"",
										matchedSub,
										getName()
									);
								}

								// Return either the matched external subtitles, or null if there was no external match
								return matchedSub;
							} else {
								LOGGER.trace(
									"Not looking for non-forced subtitles since they are \"off\" for audio language \"{}\"",
									audio
								);
								break;
							}
						} else {
							ArrayList<DLNAMediaSubtitle> candidates = new ArrayList<>();
							for (DLNAMediaSubtitle subtitles : media.getSubtitleTracksList()) {
								if (anyLanguage || subtitles.matchCode(sub)) {
									if (!subtitles.isExternal()) {
										candidates.add(subtitles);
										LOGGER.trace("Adding internal subtitles candidate: {}", subtitles);
									} else if (useExternal) {
										candidates.add(subtitles);
										LOGGER.trace("Adding external subtitles candidate: {}", subtitles);
									} else {
										LOGGER.trace(
											"Ignoring external subtitles because auto loading of external subtitles is disabled: {}",
											subtitles
										);
									}
								}
							}
							if (!candidates.isEmpty()) {
								matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, !anyLanguage);
								if (matchedSub != null) {
									LOGGER.trace(
										"Matched {} subtitles track for \"{}\": {}",
										matchedSub.isExternal() ? "external" : "internal",
										getName(),
										matchedSub
									);
									return matchedSub;
								}
							}
						}
					}
				} else {
					LOGGER.warn("Ignoring invalid audio/subtitle language configuration \"{}\"", pair);
				}
			}
		}

		/*
		 * Check for external subtitles that were skipped in the above code block
		 * because they didn't match language preferences, if there wasn't already
		 * a match and the user settings specify it.
		 */
		if (deviceSpecificConfiguration.isForceExternalSubtitles()) {
			ArrayList<DLNAMediaSubtitle> candidates = new ArrayList<>();
			for (DLNAMediaSubtitle subtitles : media.getSubtitleTracksList()) {
				if (subtitles.isExternal()) {
					candidates.add(subtitles);
					LOGGER.trace("Adding external subtitles candidate without language match: {}", subtitles);
				}
			}
			if (!candidates.isEmpty()) {
				matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, true);
				LOGGER.trace(
					"Using external subtitles track that did not match language preferences for \"{}\": {}",
					getName(),
					matchedSub
				);
				return matchedSub;
			}
		}

		// Check for forced subtitles.
		String forcedTags = deviceSpecificConfiguration.getForcedSubtitleTags();
		if (isNotBlank(forcedTags)) {
			Locale locale = PMS.getLocale();
			ArrayList<String> forcedTagsList = new ArrayList<>();
			for (String forcedTag : forcedTags.split(",")) {
				if (isNotBlank(forcedTag)) {
					forcedTagsList.add(forcedTag.trim().toLowerCase(locale));
				}
			}
			ArrayList<DLNAMediaSubtitle> candidates = new ArrayList<>();
			String forcedLanguage = deviceSpecificConfiguration.getForcedSubtitleLanguage();
			boolean anyLanguage =
				isBlank(forcedLanguage) ||
				"*".equals(forcedLanguage) ||
				DLNAMediaLang.UND.equals(forcedLanguage);
			for (DLNAMediaSubtitle subtitles : media.getSubtitleTracksList()) {
				if (!useExternal && subtitles.isExternal()) {
					continue;
				}
				String title = subtitles.getSubtitlesTrackTitleFromMetadata();
				if (isNotBlank(title)) {
					title = title.toLowerCase(locale);
					for (String forcedTag : forcedTagsList) {
						if (
							title.contains(forcedTag) &&
							(
								anyLanguage ||
								Iso639.isCodesMatching(subtitles.getLang(), forcedLanguage)
							)
						) {
							candidates.add(subtitles);
							LOGGER.trace(
								"Adding {} forced subtitles candidate that matched tag \"{}\": {}",
								subtitles.isExternal() ? "external" : "internal",
								forcedTag,
								subtitles
							);
						}
					}
				}
			}
			if (!candidates.isEmpty()) {
				matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, false);
				if (matchedSub != null) {
					LOGGER.trace(
						"Using forced {} subtitles track for \"{}\": {}",
						matchedSub.isExternal() ? "external" : "internal",
						getName(),
						matchedSub
					);
					return matchedSub;
				}
			}
		}

		LOGGER.trace("Found no matching subtitle for \"{}\"", getName());
		return null;
	}

	/**
	 * Returns the updates id for this resource. When the resource needs
	 * to be refreshed, its id is updated.
	 *
	 * @return The updated id.
	 * @see #notifyRefresh()
	 */
	public int getUpdateId() {
		return updateId;
	}

	/**
	 * Sets the updated id for this resource. When the resource needs
	 * to be refreshed, its id should be updated.
	 *
	 * @param updateId The updated id value to set.
	 * @since 1.50
	 */
	protected void setUpdateId(int updateId) {
		this.updateId = updateId;
	}

	/**
	 * Returns the updates id for all resources. When all resources need
	 * to be refreshed, this id is updated.
	 *
	 * @return The system updated id.
	 * @since 1.50
	 */
	public static int getSystemUpdateId() {
		return systemUpdateId;
	}

	/**
	 * Sets the updated id for all resources. When all resources need
	 * to be refreshed, this id should be updated.
	 *
	 * @param systemUpdateId The system updated id to set.
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
	public RendererConfiguration getDefaultRenderer() {
		return defaultRenderer;
	}

	/**
	 * Sets the default renderer configuration for this resource.
	 *
	 * @param defaultRenderer The default renderer configuration to set.
	 * @since 1.50
	 */
	public void setDefaultRenderer(RendererConfiguration defaultRenderer) {
		this.defaultRenderer = defaultRenderer;
		configuration = PMS.getConfiguration(defaultRenderer);
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
		this.children = (DLNAList) children;
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

	private static final int DEPTH_WARNING_LIMIT = 7;

	private boolean depthLimit() {
		DLNAResource tmp = this;
		int depth = 0;
		while (tmp != null) {
			tmp = tmp.parent;
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
			!configuration.isShowLiveSubtitlesFolder() ||
			!isLiveSubtitleFolderAvailable()
		) {
			return null;
		}

		// Search for transcode folder
		for (DLNAResource r : children) {
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

	////////////////////////////////////////////////////
	// Resume handling
	////////////////////////////////////////////////////

	private ResumeObj resume;
	private int resHash;
	private long startTime;

	private void internalStop() {
		DLNAResource res = resumeStop();
		final RootFolder root = ((defaultRenderer != null) ? defaultRenderer.getRootFolder() : null);
		if (root != null) {
			if (res == null) {
				res = this.clone();
			} else {
				res = res.clone();
			}

			root.stopPlaying(res);
		}
	}

	public int resumeHash() {
		return resHash;
	}

	public ResumeObj getResume() {
		return resume;
	}

	public void setResume(ResumeObj r) {
		resume = r;
	}

	public boolean isResumeable() {
		if (format != null) {
			// Only resume videos
			return format.isVideo();
		}

		return true;
	}

	private DLNAResource resumeStop() {
		if (!configuration.isResumeEnabled() || !isResumeable()) {
			return null;
		}

		notifyRefresh();
		if (resume != null) {
			resume.stop(startTime, (long) (media.getDurationInSeconds() * 1000));
			if (resume.isDone()) {
				parent.getChildren().remove(this);
			} else if (getMedia() != null) {
				media.setThumbready(false);
			}
		} else {
			for (DLNAResource res : parent.getChildren()) {
				if (res.isResume() && res.getName().equals(getName())) {
					res.resume.stop(startTime, (long) (media.getDurationInSeconds() * 1000));
					if (res.resume.isDone()) {
						parent.getChildren().remove(res);
						return null;
					}

					if (res.getMedia() != null) {
						res.media.setThumbready(false);
					}

					return res;
				}
			}

			ResumeObj r = ResumeObj.store(this, startTime);
			if (r != null) {
				DLNAResource clone = this.clone();
				clone.resume = r;
				clone.resHash = resHash;
				if (clone.media != null) {
					clone.media.setThumbready(false);
				}

				clone.player = player;
				parent.addChildInternal(clone);
				return clone;
			}
		}

		return null;
	}

	public final boolean isResume() {
		return resume != null && isResumeable();
	}

	public int minPlayTime() {
		return configuration.getMinimumWatchedPlayTime();
	}

	private String resumeStr(String s) {
		if (isResume()) {
			return Messages.getString("PMS.134") + ": " + s;
		}
		return s;
	}

	public String resumeName() {
		return resumeStr(getDisplayName());
	}

	/**
	 * Handle serialization.
	 *
	 * This method should be overridden by all media types that can be
	 * bookmarked, i.e. serialized to an external file.
	 * By default it just returns null which means the resource is ignored
	 * when serializing.
	 */
	public String write() {
		return null;
	}

	private ExternalListener masterParent;

	public void setMasterParent(ExternalListener r) {
		if (masterParent == null) {
			// If master is already set ignore this
			masterParent = r;
		}
	}

	public ExternalListener getMasterParent() {
		return masterParent;
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

	// Attempts to automatically create the appropriate container for
	// the given uri. Defaults to mpeg video for indeterminate local uris.
	public static DLNAResource autoMatch(String uri, String name) {
		try {
			uri = URLDecoder.decode(uri, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			LOGGER.error("URL decoding error ", e);
		}

		boolean isweb = uri.matches("\\S+://.+");
		Format format = FormatFactory.getAssociatedFormat(isweb ? "." + FileUtil.getUrlExtension(uri) : uri);
		int type = format == null ? Format.VIDEO : format.getType();
		if (name == null) {
			name = new File(StringUtils.substringBefore(uri, "?")).getName();
		}

		DLNAResource resource = isweb ?
			type == Format.VIDEO ? new WebVideoStream(name, uri, null) :
			type == Format.AUDIO ? new WebAudioStream(name, uri, null) :
			type == Format.IMAGE ? new FeedItem(name, uri, null, null, Format.IMAGE) : null
			:
			new RealFile(new File(uri));
		if (resource != null && format == null && !isweb) {
			resource.setFormat(FormatFactory.getAssociatedFormat(".mpg"));
		}

		LOGGER.debug(resource == null ?
			("Could not auto-match " + uri) :
			("Created auto-matched container: "+ resource));
		return resource;
	}

	// A general-purpose free-floating folder
	public static class UnattachedFolder extends VirtualFolder {
		public UnattachedFolder(String name) {
			super(name, null);
			setId(name);
		}

		public DLNAResource add(DLNAResource d) {
			if (d != null) {
				addChild(d);
				d.setId(d.getId() + "$" + getId());
				return d;
			}

			return null;
		}

		public DLNAResource add(String uri, String name, RendererConfiguration r) {
			DLNAResource  d = autoMatch(uri, name);
			if (d != null) {
				// Set the auto-matched item's renderer
				d.setDefaultRenderer(r);
				// Cache our previous renderer and
				// pretend to be a parent with the same renderer
				RendererConfiguration prev = getDefaultRenderer();
				setDefaultRenderer(r);
				// Now add the item and resolve its rendering details
				add(d);
				d.syncResolve();
				// Restore our previous renderer
				setDefaultRenderer(prev);
			}

			return d;
		}

		public int getIndex(String objectId) {
			return getIndex(objectId, null);
		}

		public int getIndex(String objectId, RendererConfiguration r) {
			int index = indexOf(objectId);
			if (index == -1 && r != null) {
				index = indexOf(recreate(objectId, null, r).getResourceId());
			}

			return index;
		}

		public DLNAResource get(String objectId, RendererConfiguration r) {
			int index = getIndex(objectId, r);
			DLNAResource d = index > -1 ? getChildren().get(index) : null;
			if (d != null && r != null && ! r.equals(d.getDefaultRenderer())) {
				d.updateRendering(r);
			}

			return d;
		}

		public List<DLNAResource> asList(String objectId) {
			int index = getIndex(objectId);
			return index > -1 ? getChildren().subList(index, index + 1) : null;
		}

		// Try to recreate a lost item from a previous session
		// using its objectId's trailing uri, if any

		public DLNAResource recreate(String objectId, String name, RendererConfiguration r) {
			try {
				return add(StringUtils.substringAfter(objectId, "/"), name, r);
			} catch (Exception e) {
				return null;
			}
		}
	}

	// A temp folder for non-xmb items
	public static final UnattachedFolder Temp = new UnattachedFolder("Temp");

	// Returns whether the url appears to be ours
	public static boolean isResourceUrl(String url) {
		return url != null && url.startsWith(PMS.get().getServer().getURL() + "/get/");
	}

	// Returns the url's resourceId (i.e. index without trailing filename) if any or null
	public static String parseResourceId(String url) {
		return isResourceUrl(url) ? StringUtils.substringBetween(url + "/", "get/", "/") : null;
	}

	// Returns the url's objectId (i.e. index including trailing filename) if any or null
	public static String parseObjectId(String url) {
		return isResourceUrl(url) ? StringUtils.substringAfter(url, "get/") : null;
	}

	// Returns the DLNAResource pointed to by the uri if it exists
	// or else a new Temp item (or null)
	public static DLNAResource getValidResource(String uri, String name, RendererConfiguration renderer) {
		LOGGER.debug("Validating URI \"{}\"", uri);
		String objectId = parseObjectId(uri);
		if (objectId != null) {
			if (objectId.startsWith("Temp$")) {
				int index = Temp.indexOf(objectId);
				return index > -1 ? Temp.getChildren().get(index) : Temp.recreate(objectId, name, renderer);
			}
			if (renderer == null) {
				renderer = RendererConfiguration.getDefaultConf();
			}

			return PMS.get().getRootFolder(renderer).getDLNAResource(objectId, renderer);
		}
		return Temp.add(uri, name, renderer);
	}

	// Returns the uri if it's ours and exists or else the url of new Temp item (or null)
	public static String getValidResourceURL(String uri, String name, RendererConfiguration r) {
		if (isResourceUrl(uri)) {
			// Check existence
			return PMS.getGlobalRepo().exists(parseResourceId(uri)) ? uri : null; // TODO: attempt repair
		}
		DLNAResource d = Temp.add(uri, name, r);
		if (d != null) {
			return d.getURL("", true);
		}
		return null;
	}

	public static class Rendering {
		RendererConfiguration r;
		Player p;
		DLNAMediaSubtitle s;
		String m;
		Rendering(DLNAResource d) {
			r = d.getDefaultRenderer();
			p = d.getPlayer();
			s = d.getMediaSubtitle();
			if (d.getMedia() != null) {
				m = d.getMedia().getMimeType();
			}
		}
	}

	public Rendering updateRendering(RendererConfiguration r) {
		Rendering rendering = new Rendering(this);
		Player p = resolvePlayer(r);
		LOGGER.debug("Switching rendering context to '{} [{}]' from '{} [{}]'", r, p, rendering.r, rendering.p);
		setDefaultRenderer(r);
		setPlayer(p);
		setPreferredMimeType(r);
		return rendering;
	}

	public void updateRendering(Rendering rendering) {
		LOGGER.debug("Switching rendering context to '{} [{}]' from '{} [{}]'", rendering.r, rendering.p, getDefaultRenderer(), getPlayer());
		setDefaultRenderer(rendering.r);
		setPlayer(rendering.p);
		media_subtitle = rendering.s;
		if (media != null) {
			media.setMimeType(rendering.m);
		}
	}

	public DLNAResource isCoded() {
		DLNAResource tmp = this;
		while (tmp != null) {
			if (tmp instanceof CodeEnter) {
				return tmp;
			}

			tmp = tmp.getParent();
		}

		return null;
	}

	public boolean isCodeValid(DLNAResource r) {
		DLNAResource res = r.isCoded();
		if (res != null) {
			if (res instanceof CodeEnter) {
				return ((CodeEnter) res).validCode(r);
			}
		}

		// normal case no code in path code is always valid
		return true;
	}

	public boolean quietPlay() {
		return false;
	}

	public long getStartTime() {
		return startTime;
	}

	private void addDynamicPls(final DLNAResource child) {
		final DLNAResource dynPls = PMS.get().getDynamicPls();
		if (dynPls == child || child.getParent() == dynPls) {
			return;
		}

		if (child instanceof VirtualVideoAction) {
			// ignore these
			return;
		}

		if (dynamicPls == null) {
			dynamicPls = new VirtualFolder(Messages.getString("PMS.147"), null);
			addChildInternal(dynamicPls);
			dynamicPls.addChild(dynPls);
		}

		if (dynamicPls != null) {
			String str = Messages.getString("PluginTab.9") + " " + child.getDisplayName() + " " + Messages.getString("PMS.148");
			VirtualVideoAction vva = new VirtualVideoAction(str, true) {
				@Override
				public boolean enable() {
					PMS.get().getDynamicPls().add(child);
					return true;
				}
			};
			vva.setParent(this);
			dynamicPls.addChildInternal(vva);
		}
	}

	public String getResolutionForKeepAR(int scaleWidth, int scaleHeight) {
		double videoAspectRatio = (double) scaleWidth / (double) scaleHeight;
		double rendererAspectRatio = 1.777777777777778;
		if (videoAspectRatio > rendererAspectRatio) {
			scaleHeight = (int) Math.round(scaleWidth / rendererAspectRatio);
		} else {
			scaleWidth  = (int) Math.round(scaleHeight * rendererAspectRatio);
		}

		scaleWidth  = Player.convertToModX(scaleWidth, 4);
		scaleHeight = Player.convertToModX(scaleHeight, 4);
		return scaleWidth + "x" + scaleHeight;
	}


	/**
	 * Populates the media Title, Year, Edition, TVSeason, TVEpisodeNumber and TVEpisodeName
	 * parsed from the media file name and if enabled insert them to the database.
	 *
	 * @param file 
	 */
	private void setMetadataFromFileName(File file) {
		String[] metadataFromFilename = FileUtil.getFileNameMetadata(file.getName());
		String titleFromFilename            = metadataFromFilename[0];
		String yearFromFilename             = metadataFromFilename[1];
		String extraInformationFromFilename = metadataFromFilename[2];
		String tvSeasonFromFilename         = metadataFromFilename[3];
		String tvEpisodeNumberFromFilename  = metadataFromFilename[4];
		String tvEpisodeNameFromFilename    = metadataFromFilename[5];
		String titleFromFilenameSimplified  = FileUtil.getSimplifiedShowName(titleFromFilename);

		media.setMovieOrShowName(titleFromFilename);
		media.setSimplifiedMovieOrShowName(titleFromFilenameSimplified);
		String titleFromDatabase;
		String titleFromDatabaseSimplified;

		// Apply the metadata from the filename.
		if (isNotBlank(titleFromFilename) && isNotBlank(tvSeasonFromFilename) && isNotBlank(tvEpisodeNumberFromFilename)) {
			/**
			 * Overwrite the title from the filename if it's very similar to one we
			 * already have in our database. This is to avoid minor grammatical differences
			 * like "Word and Word" vs. "Word & Word" from creating two virtual folders.
			 */
			titleFromDatabase = PMS.get().getSimilarTVSeriesName(titleFromFilename);
			titleFromDatabaseSimplified = FileUtil.getSimplifiedShowName(titleFromDatabase);
			if (titleFromFilenameSimplified.equals(titleFromDatabaseSimplified)) {
				media.setMovieOrShowName(titleFromDatabase);
			}

			media.setTVSeason(tvSeasonFromFilename);
			media.setTVEpisodeNumber(tvEpisodeNumberFromFilename);
			if (isNotBlank(tvEpisodeNameFromFilename)) {
				media.setTVEpisodeName(tvEpisodeNameFromFilename);
			}

			media.setIsTVEpisode(true);
		}

		if (yearFromFilename != null) {
			media.setYear(yearFromFilename);
		}

		if (extraInformationFromFilename != null) {
			media.setExtraInformation(extraInformationFromFilename);
		}

		try {
			if (configuration.getUseCache()) {
				PMS.get().getDatabase().insertVideoMetadata(file.getAbsolutePath(), file.lastModified(), media);
			}
		} catch (SQLException e) {
			LOGGER.error(
				"Could not update the database with information from the filename for \"{}\": {}",
				file.getAbsolutePath(),
				e.getMessage()
			);
			LOGGER.trace("", e);
		} finally {
			/**
			 * Attempt to enhance the metadata by using OpenSubtitles if the
			 * setting is enabled.
			 */
			if (configuration.isUseInfoFromIMDb()) {
				OpenSubtitle.backgroundLookupAndAdd(file, media);
			}
		}
	}

	public boolean isAddToMediaLibrary() {
		return true;
	}
}
