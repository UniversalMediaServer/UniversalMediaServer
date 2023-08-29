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

import java.awt.RenderingHints;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.configuration.UmsConfiguration.SubtitlesInfoLevel;
import net.pms.database.MediaDatabase;
import net.pms.database.MediaTableFiles;
import net.pms.database.MediaTableThumbnails;
import net.pms.dlna.DLNAImageProfile;
import net.pms.dlna.DLNAThumbnailInputStream;
import net.pms.encoders.Engine;
import net.pms.encoders.EngineFactory;
import net.pms.encoders.HlsHelper.HlsConfiguration;
import net.pms.formats.Format;
import net.pms.formats.FormatFactory;
import net.pms.image.BufferedImageFilterChain;
import net.pms.image.ImageInfo;
import net.pms.image.ImagesUtil;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.io.SizeLimitInputStream;
import net.pms.library.virtual.ChapterFileTranscodeVirtualFolder;
import net.pms.library.virtual.CodeEnter;
import net.pms.library.virtual.FileTranscodeVirtualFolder;
import net.pms.library.virtual.SubSelFile;
import net.pms.library.virtual.SubSelect;
import net.pms.library.virtual.TranscodeVirtualFolder;
import net.pms.library.virtual.VirtualFile;
import net.pms.library.virtual.VirtualFolder;
import net.pms.library.virtual.VirtualVideoAction;
import net.pms.media.MediaInfo;
import net.pms.media.MediaInfoStore;
import net.pms.media.MediaLang;
import net.pms.media.MediaStatus;
import net.pms.media.MediaType;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaOpenSubtitle;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.network.mediaserver.MediaServer;
import net.pms.network.mediaserver.jupnp.support.contentdirectory.UmsContentDirectoryService;
import net.pms.renderers.Renderer;
import net.pms.util.ByteRange;
import net.pms.util.FullyPlayedAction;
import net.pms.util.GenericIcons;
import net.pms.util.IPushOutput;
import net.pms.util.InputFile;
import net.pms.util.Iso639;
import net.pms.util.MpegUtil;
import net.pms.util.Range;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.TimeRange;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents any item that can be browsed via the UPNP ContentDirectory
 * service.
 */
public abstract class LibraryResource implements Cloneable, Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(LibraryResource.class);
	protected static final UmsConfiguration CONFIGURATION = PMS.getConfiguration();

	private static final int STOP_PLAYING_DELAY = 4000;
	private static final int DEPTH_WARNING_LIMIT = 7;

	protected static final int MAX_ARCHIVE_ENTRY_SIZE = 10000000;
	protected static final int MAX_ARCHIVE_SIZE_SEEK = 800000000;
	protected static final double CONTAINER_OVERHEAD = 1.04;


	/**
	 * Maximum size of a stream, taking into account that some renderers (like
	 * the PS3) will convert this <code>long</code> to <code>int</code>.
	 * Truncating this value will still return the maximum value that an
	 * <code>int</code> can contain.
	 */
	public static final long TRANS_SIZE = Long.MAX_VALUE - Integer.MAX_VALUE - 1;

	public static final RenderingHints THUMBNAIL_HINTS = new RenderingHints(RenderingHints.KEY_RENDERING,
		RenderingHints.VALUE_RENDER_QUALITY);

	private final Map<String, Integer> requestIdToRefcount = new HashMap<>();
	private volatile ImageInfo thumbnailImageInfo = null;

	protected final Renderer renderer;

	private boolean resolved;
	private int specificType;
	private String id;

	private LibraryResource parent;

	/**
	 * The format of this LibraryResource.
	 */
	private Format format;
	private MediaInfo mediaInfo;
	private MediaStatus mediaStatus;
	private MediaAudio mediaAudio;
	private MediaSubtitle mediaSubtitle;
	private long lastModified;

	private boolean isEpisodeWithinSeasonFolder = false;
	private boolean isEpisodeWithinTVSeriesFolder = false;

	/**
	 * Represents the transformation to be used to the file. If null, then
	 *
	 * @see Engine
	 */
	private Engine engine;
	private boolean discovered = false;
	private ProcessWrapper externalProcess;

	private boolean noName;
	private int nametruncate;
	private LibraryResource first;
	private LibraryResource second;

	/**
	 * The time range for the file containing the start and end time in seconds.
	 */
	private TimeRange splitRange = new TimeRange();
	private int splitTrack;
	private String fakeParentId;
	private boolean avisynth;
	private boolean skipTranscode = false;
	private boolean allChildrenAreFolders = true;

	/**
	 * List of children objects associated with this LibraryResource. This is only
	 * valid when the LibraryResource is of the container type.
	 */
	private List<LibraryResource> children;

	/**
	 * The numerical ID (1-based index) assigned to the last child of this
	 * folder. The next child is assigned this ID + 1.
	 */
	private int lastChildId = 0;

	/**
	 * The last time refresh was called.
	 */
	private long lastRefreshTime;
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

	private double lastTimeSeek = -1.0;

	/**
	 * The system time when the resource was last (re)started by a user.
	 */
	private long lastStartSystemTimeUser;

	/**
	 * The system time when the resource was last (re)started.
	 */
	private long lastStartSystemTime;

	/**
	 * The most recently requested time offset in seconds.
	 */
	private double lastStartPosition;

	////////////////////////////////////////////////////
	// Resume handling
	////////////////////////////////////////////////////
	private ResumeObj resume;
	private int resHash;
	private long startTime;

	protected LibraryResource(Renderer renderer) {
		this(renderer, Format.UNKNOWN);
	}

	protected LibraryResource(Renderer renderer, int specificType) {
		this.renderer = renderer;
		this.specificType = specificType;
		this.children = new ArrayList<>();
		resHash = 0;
	}

	/**
	 * Returns parent object, usually a folder type of resource. In the DLDI
	 * queries, the UPNP server needs to give out the parent container where the
	 * item is. The <i>parent</i> represents such a container.
	 *
	 * @return Parent object.
	 */
	public LibraryResource getParent() {
		return parent;
	}

	/**
	 * Set the parent object, usually a folder type of resource. In the DLDI
	 * queries, the UPNP server needs to give out the parent container where the
	 * item is. The <i>parent</i> represents such a container.
	 *
	 * @param parent Sets the parent object.
	 */
	public void setParent(LibraryResource parent) {
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
	 * Returns the integer representation of the id of this LibraryResource based on
 the index in its parent container.
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
	 * @see #addChildInternal(LibraryResource)
	 */
	public void setId(String id) {
		this.id = id;
	}

	public String getPathId() {
		LibraryResource tmp = getParent();
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
	 * @return The LibraryResource id.
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
	protected void setIndexId(int id) {
		setId(Integer.toString(id));
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
	 * Any {@link LibraryResource} needs to represent the container or item with a
	 * String.
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

	public abstract InputStream getInputStream() throws IOException;

	public abstract boolean isFolder();

	public LibraryResource getPrimaryResource() {
		return first;
	}

	public LibraryResource getSecondaryResource() {
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

	/**
	 * Recursive function that searches through all of the children until it
	 * finds a {@link LibraryResource} that matches the name.
	 * <p>
	 * Only used by {@link net.pms.library.RootFolder#addWebFolder(File webConf)
	 * addWebFolder(File webConf)} while parsing the web.conf file.
	 *
	 * @param name String to be compared the name to.
	 * @return Returns a {@link LibraryResource} whose name matches the parameter
	 *         name
	 * @see #getName()
	 */
	public LibraryResource searchByName(String name) {
		for (LibraryResource child : children) {
			if (child.getName().equals(name)) {
				return child;
			}
		}

		return null;
	}

	/**
	 * @return true if the {@link defaultRenderer#renderer} can understand type of mediaInfo.
         Also returns true if this LibraryResource is a container.
	 */
	public boolean isCompatible() {
		return format == null || format.isUnknown() || (format.isVideo() && renderer.isVideoSupported()) ||
			(format.isAudio() && renderer.isAudioSupported()) || (format.isImage() && renderer.isImageSupported());
	}

	/**
	 * Adds a new LibraryResource to the child list. Only useful if this object is
	 * of the container type.
	 *
	 * @param child LibraryResource to add to a container type.
	 */
	public void addChild(LibraryResource child) {
		addChild(child, true, true);
	}

	public void addChild(LibraryResource child, boolean isNew) {
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

		child.parent = this;
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

		try {
			if (child.isValid()) {
				if (isAddGlobally && child.format != null) {
					// Do not add unsupported mediaInfo formats to the list
					if (renderer != null && !renderer.supportsFormat(child.format)) {
						LOGGER.trace("Ignoring file \"{}\" because it is not supported by renderer \"{}\"", child.getName(),
							renderer.getRendererName());
						children.remove(child);
						return;
					}

					// Hide watched videos depending user preference
					if (child.isHideFullyPlayed()) {
						LOGGER.trace("Ignoring video file \"{}\" because it has been watched", child.getName());
						return;
					}
				}

				LOGGER.trace("{} child \"{}\" with class \"{}\"", isNew ? "Adding new" : "Updating", child.getName(),
					child.getClass().getSimpleName());

				if (allChildrenAreFolders && !child.isFolder()) {
					allChildrenAreFolders = false;
				}

				child.resHash = Math.abs(child.getSystemName().hashCode() + resumeHash());

				LibraryResource resumeRes = null;

				ResumeObj resumeObject = ResumeObj.create(child);
				if (
					isAddGlobally &&
					resumeObject != null &&
					!renderer.disableUmsResume() &&
					!renderer.isSamsung()
				) {
					resumeRes = child.clone();
					resumeRes.resume = resumeObject;
					resumeRes.resHash = child.resHash;
				}

				if (isAddGlobally && child.format != null) {
					// Determine transcoding possibilities if either
					// - the format is known to be transcodable
					// - we have mediaInfo info (via parserV2, playback info, or a
					// plugin)
					if (child.format.transcodable() || child.mediaInfo != null) {
						if (child.mediaInfo == null) {
							child.mediaInfo = new MediaInfo();
						}

						// Try to determine a engine to use for transcoding.
						Engine transcodingEngine = null;

						// First, try to match an engine from recently played
						// folder or based on the name of the LibraryResource
						// or its parent. If the name ends in "[unique engine
						// id]", that engine is preferred.
						String name = getName();

						if (renderer.getUmsConfiguration().isShowRecentlyPlayedFolder()) {
							transcodingEngine = child.engine;
						} else {
							for (Engine tEngine : EngineFactory.getEngines()) {
								String end = "[" + tEngine.getEngineId().toString() + "]";

								if (name.endsWith(end)) {
									nametruncate = name.lastIndexOf(end);
									transcodingEngine = tEngine;
									LOGGER.trace("Selecting engine based on name end");
									break;
								} else if (parent != null && parent.getName().endsWith(end)) {
									parent.nametruncate = parent.getName().lastIndexOf(end);
									transcodingEngine = tEngine;
									LOGGER.trace("Selecting engine based on parent name end");
									break;
								}
							}
						}

						// If no preferred engine could be determined from the name,
						// try to match a engine based on mediaInfo information and format.
						if (transcodingEngine == null) {
							transcodingEngine = child.resolveEngine();
						}
						child.setEngine(transcodingEngine);

						if (resumeRes != null) {
							resumeRes.engine = transcodingEngine;
							resumeRes.mediaSubtitle = child.mediaSubtitle;
						}

						if (!allChildrenAreFolders) {
							// Should the child be added to the #--TRANSCODE--# folder?
							if ((child.format.isVideo() || child.format.isAudio()) && child.isTranscodeFolderAvailable()) {
								VirtualFolder transcodeFolder = getTranscodeFolder();
								if (transcodeFolder != null) {
									VirtualFolder fileTranscodeFolder = new FileTranscodeVirtualFolder(renderer, child);
									if (parent instanceof SubSelect) {
										fileTranscodeFolder.setMediaSubtitle(child.getMediaSubtitle());
									}

									LOGGER.trace("Adding \"{}\" to transcode folder for engine: \"{}\"", child.getName(),
										transcodingEngine);
									transcodeFolder.addChildInternal(fileTranscodeFolder);
								}
							}

							if (child.format.isVideo() && child.isSubSelectable() && !(this instanceof SubSelFile)) {
								VirtualFolder vf = getSubSelector(true);
								if (vf != null) {
									LibraryResource newChild = child.clone();
									newChild.engine = transcodingEngine;
									newChild.mediaInfo = child.mediaInfo;
									LOGGER.trace("Adding live subtitles folder for \"{}\" with engine {}", child.getName(),
										transcodingEngine);

									vf.addChild(new SubSelFile(renderer, newChild), true);
								}
							}

							if (renderer.getUmsConfiguration().isDynamicPls() && !child.isFolder() && renderer != null &&
								!renderer.isNoDynPlsFolder()) {
								addDynamicPls(child);
							}
						} else if (!child.format.isCompatible(child, renderer) && !child.isFolder()) {
							LOGGER.trace("Ignoring file \"{}\" because it is not compatible with renderer \"{}\"", child.getName(),
								renderer.getRendererName());
							children.remove(child);
						}
					}

					if (resumeRes != null && resumeRes.mediaInfo != null) {
						resumeRes.mediaInfo.setThumbready(false);
						resumeRes.mediaInfo.setMimeType(HTTPResource.VIDEO_TRANSCODE);
					}

					/**
					 * Secondary format is currently only used to provide 24-bit
					 * FLAC to PS3 by sending it as a fake video. This can be
					 * made more reusable with a renderer config setting like
					 * Mux24BitFlacToVideo if we ever have another purpose for
					 * it, which I doubt we will have.
					 */
					if (child.format.getSecondaryFormat() != null && child.mediaInfo != null && renderer != null &&
						renderer.supportsFormat(child.format.getSecondaryFormat()) && renderer.isPS3()) {
						LibraryResource newChild = child.clone();
						newChild.setFormat(newChild.format.getSecondaryFormat());
						LOGGER.trace("Detected secondary format \"{}\" for \"{}\"", newChild.format.toString(), newChild.getName());
						newChild.first = child;
						child.second = newChild;

						if (!newChild.format.isCompatible(newChild, renderer)) {
							Engine transcodingEngine = EngineFactory.getEngine(newChild);
							newChild.setEngine(transcodingEngine);
							LOGGER.trace("Secondary format \"{}\" will use engine \"{}\" for \"{}\"", newChild.format.toString(),
								engine == null ? "null" : engine.getName(), newChild.getName());
						}

						if (child.mediaInfo != null && child.mediaInfo.isSecondaryFormatValid()) {
							addChild(newChild, true);
							LOGGER.trace("Adding secondary format \"{}\" for \"{}\"", newChild.format.toString(), newChild.getName());
						} else {
							LOGGER.trace("Ignoring secondary format \"{}\" for \"{}\": invalid format", newChild.format.toString(),
								newChild.getName());
						}
					}
				}

				if (isNew) {
					addChildInternal(child, isAddGlobally);
				}

				if (resumeRes != null) {
					addChildInternal(resumeRes);
				}
			}
		} catch (Throwable t) {
			LOGGER.debug("Error adding child {}: {}", child.getName(), t);

			child.parent = null;
			children.remove(child);
		}
	}

	/**
	 * Determine whether we are a candidate for streaming or transcoding to the
	 * renderer, and return the relevant engine or null as appropriate.
	 *
	 * @return An engine if transcoding or null if streaming
	 */
	public Engine resolveEngine() {
		// Use device-specific conf, if any
		boolean parserV2 = mediaInfo != null && renderer.isUseMediaInfo();
		Engine resolvedEngine;

		if (mediaInfo == null) {
			mediaInfo = new MediaInfo();
		}

		if (format == null) {
			// Shouldn't happen, this is just a desperate measure
			Format f = FormatFactory.getAssociatedFormat(getSystemName());
			setFormat(f != null ? f : FormatFactory.getAssociatedFormat(".mpg"));
		}

		// Check if we're a transcode folder item
		if (isInsideTranscodeFolder()) {
			// Yes, leave everything as-is
			resolvedEngine = getEngine();
			LOGGER.trace("Selecting engine {} based on transcode item settings", resolvedEngine);
			return resolvedEngine;
		}

		// Resolve subtitles stream
		if (mediaInfo.isVideo() && !renderer.getUmsConfiguration().isDisableSubtitles() && hasSubtitles(false)) {
			MediaAudio audio = mediaAudio != null ? mediaAudio : resolveAudioStream();
			if (mediaSubtitle == null) {
				mediaSubtitle = resolveSubtitlesStream(audio == null ? null : audio.getLang(), false);
			}
		}

		String rendererForceExtensions = renderer.getTranscodedExtensions();
		String rendererSkipExtensions = renderer.getStreamedExtensions();
		String configurationForceExtensions = renderer.getUmsConfiguration().getForceTranscodeForExtensions();
		String configurationSkipExtensions = renderer.getUmsConfiguration().getDisableTranscodeForExtensions();

		if (renderer.getUmsConfiguration().isDisableTranscoding()) {
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

		// Try to match an engine based on mediaInfo information and format.
		resolvedEngine = EngineFactory.getEngine(this);

		boolean isIncompatible = false;
		if (resolvedEngine != null) {
			String prependTranscodingReason = "File \"{}\" will not be streamed because ";
			if (forceTranscode) {
				LOGGER.debug(prependTranscodingReason + "transcoding is forced by configuration", getName());
			} else if (this instanceof DVDISOTitle) {
				forceTranscode = true;
				LOGGER.debug(prependTranscodingReason + "streaming of DVD video tracks isn't supported", getName());
			} else if (!format.isCompatible(this, renderer)) {
				isIncompatible = true;
				LOGGER.debug(prependTranscodingReason + "it is not supported by the renderer {}", getName(),
					renderer.getRendererName());
			} else if (renderer.getUmsConfiguration().isEncodedAudioPassthrough()) {
				if (mediaAudio != null && (FormatConfiguration.AC3.equals(mediaAudio.getAudioCodec()) ||
					FormatConfiguration.DTS.equals(mediaAudio.getAudioCodec()))) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the audio will use the encoded audio passthrough feature", getName());
				} else {
					for (MediaAudio audioTrack : mediaInfo.getAudioTracks()) {
						if (audioTrack != null && (FormatConfiguration.AC3.equals(audioTrack.getAudioCodec()) ||
							FormatConfiguration.DTS.equals(audioTrack.getAudioCodec()))) {
							isIncompatible = true;
							LOGGER.debug(prependTranscodingReason + "the audio will use the encoded audio passthrough feature", getName());
							break;
						}
					}
				}
			}

			if (!forceTranscode && !isIncompatible && format.isVideo() && parserV2 && mediaInfo.getDefaultVideoTrack() != null) {
				MediaVideo mediaVideo = mediaInfo.getDefaultVideoTrack();
				int maxBandwidth = renderer.getMaxBandwidth();

				if (renderer.isKeepAspectRatio() && !"16:9".equals(mediaVideo.getDisplayAspectRatio())) {
					isIncompatible = true;
					LOGGER.debug(
						prependTranscodingReason + "the renderer needs us to add borders to change the aspect ratio from {} to 16/9.",
						getName(), mediaVideo.getDisplayAspectRatio());
				} else if (!renderer.isResolutionCompatibleWithRenderer(mediaVideo.getWidth(), mediaVideo.getHeight())) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the resolution is incompatible with the renderer.", getName());
				} else if (mediaInfo.getBitRate() > maxBandwidth) {
					isIncompatible = true;
					LOGGER.debug(prependTranscodingReason + "the bitrate ({} b/s) is too high ({} b/s).", getName(), mediaInfo.getBitRate(),
						maxBandwidth);
				} else if (renderer.isH264Level41Limited() && mediaVideo.isH264()) {
					if (mediaVideo.getFormatLevel() != null) {
						double h264Level = 4.1;

						try {
							h264Level = Double.parseDouble(mediaVideo.getFormatLevel());
						} catch (NumberFormatException e) {
							LOGGER.trace("Could not convert {} to double: {}", mediaVideo.getFormatLevel(), e.getMessage());
						}

						if (h264Level > 4.1) {
							isIncompatible = true;
							LOGGER.debug(prependTranscodingReason + "the H.264 level ({}) is not supported.", getName(), h264Level);
						}
					} else {
						isIncompatible = true;
						LOGGER.debug(prependTranscodingReason + "the H.264 level is unknown.", getName());
					}
				} else if (mediaVideo.is3d() && StringUtils.isNotBlank(renderer.getOutput3DFormat()) &&
					(!mediaVideo.get3DLayout().toString().toLowerCase(Locale.ROOT).equals(renderer.getOutput3DFormat()))) {
					forceTranscode = true;
					LOGGER.debug(prependTranscodingReason + "it is 3D and is forced to transcode to the format \"{}\"", getName(),
						renderer.getOutput3DFormat());
				}
			}

			/*
			 * Transcode if: 1) transcoding is forced by configuration, or 2)
			 * transcoding is not prevented by configuration and is needed due
			 * to subtitles or some other renderer incompatbility
			 */
			if (forceTranscode || (isIncompatible && !isSkipTranscode())) {
				if (parserV2) {
					LOGGER.debug("Final verdict: \"{}\" will be transcoded with engine \"{}\" with mime type \"{}\"", getName(),
						resolvedEngine.toString(), renderer.getMimeType(this));
				} else {
					LOGGER.debug("Final verdict: \"{}\" will be transcoded with engine \"{}\"", getName(), resolvedEngine.toString());
				}
			} else {
				resolvedEngine = null;
				LOGGER.debug("Final verdict: \"{}\" will be streamed", getName());
			}
		} else {
			LOGGER.debug("Final verdict: \"{}\" will be streamed because no compatible engine was found", getName());
		}
		return resolvedEngine;
	}

	/**
	 * Returns the transcode folder for this resource.
	 * If UMS is configured to hide transcode folders, null is returned.
	 * If no folder exists, a new transcode folder is created.
	 * If transcode folder exists, it is returned.
	 * This method is called on the parent folder each time a child is added to
	 * that parent (via {@link addChild(LibraryResource)}.
	 *
	 * @return the transcode virtual folder
	 */
	// XXX package-private: used by VirtualFile; should be protected?
	TranscodeVirtualFolder getTranscodeFolder() {
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
	 * (Re)sets the given LibraryResource as follows: - if it's already one of our
	 * children, renew it - or if we have another child with the same name,
	 * replace it - otherwise add it as a new child.
	 *
	 * @param child the LibraryResource to update
	 */
	public void updateChild(LibraryResource child) {
		LibraryResource found = children.contains(child) ? child : searchByName(child.getName());
		if (found != null) {
			if (child != found) {
				// Replace
				child.parent = this;
				renderer.getGlobalRepo().replace(found, child);
				children.set(children.indexOf(found), child);
			}
			// Renew
			addChild(child, false, true);
		} else {
			// Not found, it's new
			addChild(child, true, true);
		}
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
	 *                      global ID repository.
	 */
	protected synchronized void addChildInternal(LibraryResource child, boolean isAddGlobally) {
		if (child.getId() != null) {
			LOGGER.debug("Node ({}) already has an ID ({}), which is overridden now. The previous parent node was: {}",
				new Object[] {child.getClass().getName(), child.getResourceId(), child.parent });
		}

		children.add(child);
		child.parent = this;

		if (isAddGlobally) {
			renderer.getGlobalRepo().add(child);
		}
	}

	protected void refreshChildrenIfNeeded(String search) {
		if (isDiscovered() && isRefreshNeeded()) {
			refreshChildren(search);
			notifyRefresh();
		}
	}

	/**
	 * Update the last refresh time.
	 */
	protected void notifyRefresh() {
		lastRefreshTime = System.currentTimeMillis();
		UmsContentDirectoryService.bumpSystemUpdateId();
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

			if (this instanceof VirtualFile virtualFile) {
				if (renderer.isUseMediaInfo() && renderer.isDLNATreeHack()) {
					ready = virtualFile.analyzeChildren(count);
				} else {
					ready = virtualFile.analyzeChildren(-1);
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
	 * Discover the list of children.
	 */
	public void discoverChildren() {
	}

	public void discoverChildren(String str) {
		discoverChildren();
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
	 * @return true, if the container is changed, so refresh is needed. This
	 *         could be called a lot of times.
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
	 * Sets the LibraryResource's {@link net.pms.formats.Format} according to its
	 * filename if it isn't set already.
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
	 * Hook to lazily initialise immutable resources e.g. ISOs, zip files
	 * &amp;c.
	 *
	 * @since 1.90.0
	 * @see #syncResolve()
	 */
	protected void resolveOnce() {
	}

	/**
	 * Resolve events are hooks that allow library resources to perform various
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
		if (mediaInfo != null && mediaInfo.isVideo()) {
			registerExternalSubtitles(false);
			if (this instanceof RealFile realfile && realfile.getFile() != null) {
				MediaInfoStore.setMetadataFromFileName(realfile.getFile(), mediaInfo);
			}
		}
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
	 * Returns the engine part of the display name or {@code null} if
	 * none should be displayed.
	 *
	 * @return The engine display name or {@code null}.
	 */
	protected String getDisplayNameEngine() {
		String engineName = null;
		if (engine != null) {
			if (isNoName() || !CONFIGURATION.isHideEngineNames()) {
				engineName = "[" + engine.getName() + (isAvisynth() ? " + AviSynth]" : "]");
			}
		} else if (isNoName()) {
			engineName = Messages.getString("NoTranscoding");
		}
		return engineName;
	}

	/**
	 * Returns the "base" part of the display name or an empty {@link String} if
	 * none should be displayed. The "base" name is the name of this
	 * {@link LibraryResource} without any prefix or suffix.
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
	 * Returns the suffix part of the display name or an empty {@link String} if
	 * none should be displayed.
	 *
	 * @return The display name suffix or {@code ""}.
	 */
	public String getDisplayNameSuffix() {
		if (mediaInfo == null) {
			return null;
		}
		MediaType mediaType = mediaInfo.getMediaType();
		switch (mediaType) {
			case VIDEO:
				StringBuilder nameSuffixBuilder = new StringBuilder();
				boolean subsAreValidForStreaming = mediaSubtitle != null && mediaSubtitle.isExternal() &&
					(engine == null || renderer.streamSubsForTranscodedVideo()) &&
					renderer.isExternalSubtitlesFormatSupported(mediaSubtitle, this);

				if (mediaAudio != null) {
					String audioLanguage = mediaAudio.getLang();
					if (audioLanguage == null || MediaLang.UND.equals(audioLanguage.toLowerCase(Locale.ROOT))) {
						audioLanguage = "";
					} else {
						audioLanguage = Iso639.getFirstName(audioLanguage);
						audioLanguage = audioLanguage == null ? "" : "/" + audioLanguage;
					}

					String audioTrackTitle = "";
					if (mediaAudio.getTitle() != null && !"".equals(mediaAudio.getTitle()) &&
						renderer.isShowAudioMetadata()) {
						audioTrackTitle = " (" + mediaAudio.getTitle() + ")";
					}

					if (nameSuffixBuilder.length() > 0) {
						nameSuffixBuilder.append(" ");
					}
					nameSuffixBuilder.append("{Audio: ").append(mediaAudio.getAudioCodec()).append(audioLanguage).append(audioTrackTitle)
						.append("}");
				}

				SubtitlesInfoLevel subsInfoLevel;
				if (parent instanceof ChapterFileTranscodeVirtualFolder) {
					subsInfoLevel = SubtitlesInfoLevel.NONE;
				} else if (isInsideTranscodeFolder()) {
					subsInfoLevel = SubtitlesInfoLevel.FULL;
				} else {
					subsInfoLevel = CONFIGURATION.getSubtitlesInfoLevel();
				}
				if (mediaSubtitle != null && mediaSubtitle.getId() != MediaLang.DUMMY_ID && subsInfoLevel != SubtitlesInfoLevel.NONE) {
					if (nameSuffixBuilder.length() > 0) {
						nameSuffixBuilder.append(" ");
					}
					nameSuffixBuilder.append("{");
					String subtitleLanguage = mediaSubtitle.getLangFullName();
					if (subsInfoLevel == SubtitlesInfoLevel.BASIC) {
						if ("Undetermined".equals(subtitleLanguage)) {
							nameSuffixBuilder.append(Messages.getString("Unknown"));
						} else {
							nameSuffixBuilder.append(subtitleLanguage);
						}
						nameSuffixBuilder.append(" ").append(Messages.getString("Subtitles_lowercase"));
					} else if (subsInfoLevel == SubtitlesInfoLevel.FULL) {
						if (subsAreValidForStreaming) {
							nameSuffixBuilder.append(Messages.getString("Stream")).append(" ");
						}

						if (mediaSubtitle.isExternal()) {
							nameSuffixBuilder.append(Messages.getString("External_abbr")).append(" ");
						} else if (mediaSubtitle.isEmbedded()) {
							nameSuffixBuilder.append(Messages.getString("Internal_abbr")).append(" ");
						}
						nameSuffixBuilder.append(Messages.getString("Sub"));
						nameSuffixBuilder.append(mediaSubtitle.getType().getShortName()).append("/");

						if ("Undetermined".equals(subtitleLanguage)) {
							nameSuffixBuilder.append(Messages.getString("Unknown_abbr"));
						} else {
							nameSuffixBuilder.append(subtitleLanguage);
						}

						if (mediaSubtitle.getTitle() != null &&
							StringUtils.isNotBlank(mediaSubtitle.getTitle()) && renderer.isShowSubMetadata()) {
							nameSuffixBuilder.append(" (").append(mediaSubtitle.getTitle()).append(")");
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

	/**
	 * Returns the display name for the renderer.
	 *
	 * @return The display name.
	 */
	public String getDisplayName() {
		return getDisplayName(true);
	}

	public boolean isInsideTranscodeFolder() {
		return parent instanceof FileTranscodeVirtualFolder;
	}

	/**
	 * Returns the display name for the renderer with or without
	 * additional information suffix.
	 *
	 * @param withSuffix if {@code true} additional information is added after
	 *            the name itself, if {@code false} nothing is added.
	 * @return The display name.
	 */
	public String getDisplayName(boolean withSuffix) {
		StringBuilder sb = new StringBuilder();

		// Base
		if (parent instanceof ChapterFileTranscodeVirtualFolder && getSplitRange() != null) {
			sb.append(">> ");
			sb.append(StringUtil.convertTimeToString(getSplitRange().getStartOrZero(), StringUtil.DURATION_TIME_FORMAT));
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

		// Engine name
		String engineName = getDisplayNameEngine();
		if (engineName != null) {
			if (sb.length() > 0) {
				sb.append(" ");
			}
			sb.append(engineName);
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
	public String getFileURL() {
		return getURL("");
	}

	/**
	 * @param profile
	 * @return Returns an URL pointing to an image representing the item. If
	 *         none is available, "thumbnail0000.png" is used.
	 */
	public String getThumbnailURL(DLNAImageProfile profile) {
		StringBuilder sb = new StringBuilder(MediaServer.getURL());
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
	 * @return Returns a URL for a given mediaInfo item. Not used for container
         types.
	 */
	public String getURL(String prefix) {
		return getURL(prefix, false);
	}

	public String getURL(String prefix, boolean useSystemName) {
		return getURL(prefix, useSystemName, true);
	}

	public String getURL(String prefix, boolean useSystemName, boolean urlEncode) {
		String uri = useSystemName ? getSystemName() : getName();
		StringBuilder sb = new StringBuilder();
		sb.append(MediaServer.getURL());
		sb.append("/get/");
		sb.append(getResourceId()); // id
		sb.append('/');
		sb.append(prefix);
		sb.append(urlEncode ? encode(uri) : uri);
		return sb.toString();
	}

	/**
	 * @param subs
	 * @return Returns a URL for a given subtitles item. Not used for container
	 *         types.
	 */
	public String getSubsURL(MediaSubtitle subs) {
		StringBuilder sb = new StringBuilder();
		sb.append(MediaServer.getURL());
		sb.append("/get/");
		sb.append(getResourceId()); // id
		sb.append('/');
		sb.append("subtitle0000");
		sb.append(encode(subs.getName()));
		return sb.toString();
	}

	/**
	 * @return Number of children objects. This might be used in the DLDI
	 *         response, as some renderers might not have enough memory to hold
	 *         the list for all children.
	 */
	public int childrenCount() {
		if (children == null) {
			return 0;
		}

		return children.size();
	}

	/**
	 * Clear all resources in children.
	 */
	public void clearChildren() {
		if (children == null) {
			return;
		}
		for (LibraryResource resource : children) {
			resource.clearChildren();
		}
		children.clear();
	}

	/**
	 * Gets the media renderer's mime type if available, returns a default mime
	 * type otherwise.
	 *
	 * @return String representation of the mime type
	 */
	public String getRendererMimeType() {
		// FIXME: There is a flaw here. In addChild(LibraryResource) the mime type
		// is determined for the default renderer. This renderer may rewrite the
		// mime type based on its configuration. Looking up that mime type is
		// not guaranteed to return a match for another renderer.
		String mime = renderer.getMimeType(this);

		// Use our best guess if we have no valid mime type
		if (mime == null || mime.contains("/transcode")) {
			mime = HTTPResource.getDefaultMimeType(getType());
		}

		return mime;
	}

	private String getRequestId(String rendererId) {
		return String.format("%s|%x|%s", rendererId, hashCode(), getSystemName());
	}

	/**
	 * Plugin implementation. When this item is going to play, it will notify
	 * all the StartStopListener objects available.
	 *
	 * @param rendererId
	 * @param incomingRenderer
	 *
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
				final LibraryResource self = this;
				Runnable r = () -> {
					String rendererName = "unknown renderer";
					try {
						renderer.setPlayingRes(self);
						rendererName = renderer.getRendererName().replaceAll("\n", "");
					} catch (NullPointerException e) {
					}
					if (isLogPlayEvents()) {
						LOGGER.info("Started playing " + getName() + " on your " + rendererName);
						LOGGER.debug(
							"The full filename of which is: " + getSystemName() + " and the address of the renderer is: " + rendererId);
					}
					startTime = System.currentTimeMillis();
				};
				new Thread(r, "StartPlaying Event").start();
			}
		}
	}

	/**
	 * Plugin implementation. When this item is going to stop playing, it will
	 * notify all the StartStopListener objects available.
	 *
	 * @see StartStopListener
	 */
	public void stopPlaying(final String rendererId) {
		final LibraryResource self = this;
		final String requestId = getRequestId(rendererId);
		Runnable defer = () -> {
			long start = startTime;
			try {
				Thread.sleep(STOP_PLAYING_DELAY);
			} catch (InterruptedException e) {
				LOGGER.error("stopPlaying sleep interrupted", e);
				Thread.currentThread().interrupt();
			}

			synchronized (requestIdToRefcount) {
				final Integer refCount = requestIdToRefcount.get(requestId);
				assert refCount != null;
				assert refCount > 0;
				requestIdToRefcount.put(requestId, refCount - 1);
				if (start != startTime) {
					return;
				}

				Runnable r = () -> {
					if (refCount == 1) {
						requestIdToRefcount.put(requestId, 0);
						String rendererName = "unknown renderer";
						try {
							// Reset only if another item hasn't already
							// begun playing
							if (renderer.getPlayingRes() == self) {
								renderer.setPlayingRes(null);
							}
							rendererName = renderer.getRendererName();
						} catch (NullPointerException e) {
						}

						if (isLogPlayEvents()) {
							LOGGER.info("Stopped playing {} on {}", getName(), rendererName);
							LOGGER.debug("The full filename of which is \"{}\" and the address of the renderer is {}", getSystemName(),
								rendererId);
						}
						internalStop();
					}
				};
				new Thread(r, "StopPlaying Event").start();
			}
			if (mediaSubtitle instanceof MediaOpenSubtitle dLNAMediaOpenSubtitle) {
				dLNAMediaOpenSubtitle.deleteLiveSubtitlesFile();
			}
		};

		new Thread(defer, "StopPlaying Event Deferrer").start();
	}

	/**
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

		double fileDuration = 0;
		if (mediaInfo != null && (mediaInfo.isAudio() || mediaInfo.isVideo())) {
			fileDuration = mediaInfo.getDurationInSeconds();
		}

		/**
		 * Do not treat this as a legitimate playback attempt if the start
		 * time was within 2 seconds of the end of the video.
		 */
		if (fileDuration < 2.0 || lastStartPosition < (fileDuration - 2.0)) {
			lastStartSystemTimeUser = startTime;
		}
	}

	/**
	 * Gets the system time when the resource was last (re)started.
	 *
	 * The system time when the resource was last (re)started by a user.
	 * This is a guess, where we disqualify certain playback requests from
	 * setting this value based on how close they were to the end, because
	 * some renderers request the last bytes of a file for processing behind
	 * the scenes, and that does not count as a real user doing it.
	 *
	 * @return The system time when the resource was last (re)started
	 *         by a user.
	 */
	public double getLastStartSystemTimeUser() {
		return lastStartSystemTimeUser;
	}

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
	 * Returns an InputStream of this LibraryResource that starts at a given time,
	 * if possible.
	 * Very useful if video chapters are being used.
	 *
	 * @param range
	 * @return The inputstream
	 * @throws IOException
	 */
	public InputStream getInputStream(Range range) throws IOException {
		return getInputStream(range, null);
	}

	/**
	 * Returns an InputStream of this LibraryResource that starts at a given time,
	 * if possible. Very useful if video chapters are being used.
	 *
	 * @param range
	 * @param hlsConfiguration
	 * @return The inputstream
	 * @throws IOException
	 */
	public synchronized InputStream getInputStream(Range range, HlsConfiguration hlsConfiguration) throws IOException {
		// Use device-specific UMS conf, if any
		LOGGER.trace("Asked stream chunk : " + range + " of " + getName() + " and engine " + engine);

		// shagrath: small fix, regression on chapters
		boolean timeseekAuto = false;
		// Ditlew - WDTV Live
		// Ditlew - We convert byteoffset to timeoffset here. This needs the
		// stream to be CBR!
		int cbrVideoBitrate = renderer.getCBRVideoBitrate();
		long low = (range instanceof ByteRange byteRange) ? byteRange.getStartOrZero() : 0;
		long high = (range instanceof ByteRange byteRange && range.isEndLimitAvailable()) ? (long) byteRange.getEnd() : -1;
		TimeRange timeRange = range.createTimeRange();
		if (engine != null && low > 0 && cbrVideoBitrate > 0) {
			int usedBitRated = (int) ((cbrVideoBitrate + 256) * 1024 / (double) 8 * CONTAINER_OVERHEAD);
			if (low > usedBitRated) {
				timeRange.setStart(low / (double) (usedBitRated));
				low = 0;

				// WDTV Live - if set to TS it asks multiple times and ends by
				// asking for an invalid offset which kills MEncoder
				if (timeRange.getStartOrZero() > mediaInfo.getDurationInSeconds()) {
					return null;
				}

				// Should we rewind a little (in case our overhead isn't
				// accurate enough)
				int rewindSecs = renderer.getByteToTimeseekRewindSeconds();
				timeRange.rewindStart(rewindSecs);

				// shagrath:
				timeseekAuto = true;
			}
		}

		if (low > 0 && mediaInfo.getBitRate() > 0) {
			lastStartPosition = (low * 8) / (double) mediaInfo.getBitRate();
			LOGGER.trace("Estimating seek position from byte range:");
			LOGGER.trace("   media.getBitrate: " + mediaInfo.getBitRate());
			LOGGER.trace("   low: " + low);
			LOGGER.trace("   lastStartPosition: " + lastStartPosition);
		} else {
			lastStartPosition = timeRange.getStartOrZero();
			LOGGER.trace("Setting lastStartPosition from time-seeking: " + lastStartPosition);
		}

		// Determine source of the stream
		if (engine == null && !isResume()) {
			// No transcoding
			if (this instanceof IPushOutput iPushOutput) {
				PipedOutputStream out = new PipedOutputStream();
				InputStream fis = new PipedInputStream(out);
				iPushOutput.push(out);

				if (low > 0) {
					fis.skip(low);
				}

				lastStartSystemTime = System.currentTimeMillis();
				return wrap(fis, high, low);
			}

			InputStream fis = getInputStream();

			if (fis != null) {
				if (low > 0) {
					fis.skip(low);
				}

				fis = wrap(fis, high, low);
				if (timeRange.getStartOrZero() > 0 && this instanceof RealFile) {
					fis.skip(MpegUtil.getPositionForTimeInMpeg(((RealFile) this).getFile(), (int) timeRange.getStartOrZero()));
				}
			}

			lastStartSystemTime = System.currentTimeMillis();
			return fis;
		}

		// Pipe transcoding result
		OutputParams params = new OutputParams(renderer.getUmsConfiguration());
		params.setAid(mediaAudio);
		params.setSid(mediaSubtitle);
		params.setMediaRenderer(renderer);
		timeRange.limit(getSplitRange());
		params.setTimeSeek(timeRange.getStartOrZero());
		params.setTimeEnd(timeRange.getEndOrZero());
		params.setShiftScr(timeseekAuto);
		params.setHlsConfiguration(hlsConfiguration);
		if (this instanceof IPushOutput iPushOutput) {
			params.setStdIn(iPushOutput);
		}

		if (resume != null) {
			if (range instanceof TimeRange tRange) {
				resume.update(tRange, this);
			}

			params.setTimeSeek(resume.getTimeOffset() / 1000);
			if (engine == null) {
				engine = EngineFactory.getEngine(this);
			}
		}

		if (System.currentTimeMillis() - lastStartSystemTime < 500) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				LOGGER.error(null, e);
				Thread.currentThread().interrupt();
			}
		}

		// (Re)start transcoding process if necessary
		if (externalProcess == null || externalProcess.isDestroyed() || hlsConfiguration != null) {
			// First playback attempt => start new transcoding process
			LOGGER.debug("Starting transcode/remux of " + getName() + " with media info: " + mediaInfo);
			lastStartSystemTime = System.currentTimeMillis();

			if (params.getTimeSeek() > 0) {
				// This must be a resume - so need to set lastTimeSeek to avoid a restart of the process
				// from a new seek request to the same resume point
				LOGGER.debug("Setting last time seek (from resume) to: " + params.getTimeSeek() + " seconds");
				lastTimeSeek = params.getTimeSeek();
			}

			externalProcess = engine.launchTranscode(this, mediaInfo, params);
			if (params.getWaitBeforeStart() > 0) {
				LOGGER.trace("Sleeping for {} milliseconds", params.getWaitBeforeStart());
				try {
					Thread.sleep(params.getWaitBeforeStart());
				} catch (InterruptedException e) {
					LOGGER.error(null, e);
					Thread.currentThread().interrupt();
				}

				LOGGER.trace("Finished sleeping for " + params.getWaitBeforeStart() + " milliseconds");
			}
		} else if (params.getTimeSeek() > 0 && mediaInfo != null && mediaInfo.isMediaParsed() && mediaInfo.getDurationInSeconds() > 0) {

			// Time seek request => stop running transcode process and start a new one
			LOGGER.debug("Requesting time seek: " + params.getTimeSeek() + " seconds");

			if (lastTimeSeek == params.getTimeSeek()) {
				LOGGER.debug("Duplicate time seek request: " + params.getTimeSeek() + " seconds, ignoring");
			} else {

				LOGGER.debug("Setting last time seek to: " + params.getTimeSeek() + " seconds");
				lastTimeSeek = params.getTimeSeek();

				params.setMinBufferSize(1);

				Runnable r = () -> {
					externalProcess.stopProcess();
				};

				new Thread(r, "External Process Stopper").start();

				lastStartSystemTime = System.currentTimeMillis();
				ProcessWrapper newExternalProcess = engine.launchTranscode(this, mediaInfo, params);

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					LOGGER.error(null, e);
					Thread.currentThread().interrupt();
				}

				if (newExternalProcess == null) {
					LOGGER.trace("External process instance is null... sounds not good");
				}

				externalProcess = newExternalProcess;
			}
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
					Thread.currentThread().interrupt();
				}
			}
		}

		// fail fast: don't leave a process running indefinitely if it's
		// not producing output after params.waitbeforestart milliseconds + 5
		// seconds
		// this cleans up lingering MEncoder web video transcode processes that
		// hang
		// instead of exiting
		if (is == null && !externalProcess.isDestroyed()) {
			Runnable r = () -> {
				LOGGER.error("External input stream instance is null... stopping process");
				externalProcess.stopProcess();
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
	 * @param input The input stream to wrap.
	 * @param high The high value.
	 * @param low The low value.
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
		return mimeType(engine);
	}

	private String mimeType(Engine engine) {
		if (engine != null) {
			// Engines like FFmpegVideo can define placeholder MIME types like
			// video/transcode to be replaced later
			return engine.mimeType();
		} else if (mediaInfo != null && mediaInfo.isMediaParsed()) {
			return getPreferredMimeType();
		} else if (getFormat() != null) {
			return getFormat().mimeType();
		} else {
			return HTTPResource.getDefaultMimeType(getSpecificType());
		}
	}

	/**
	 * Get the mimetype for this resource according to the renderer's
	 * supported preferences, if any.
	 *
	 * @return The mimetype renderer supported preferences
	 */
	private String getPreferredMimeType() {
		if (mediaInfo != null && renderer.isUseMediaInfo() && (format == null || !format.isImage())) {
			// See which MIME type the renderer prefers in case it supports the
			// mediaInfo
			String preferred = renderer.getFormatConfiguration().getMatchedMIMEtype(this, renderer);
			if (preferred != null && !FormatConfiguration.MIMETYPE_AUTO.equals(preferred)) {
				// Use the renderer's preferred MIME type for this file
				LOGGER.trace("File \"{}\" will be sent with MIME type \"{}\"", getName(), preferred);
				return preferred;
			}
		}
		return mediaInfo != null ? mediaInfo.getMimeType() : null;
	}

	/**
	 * Prototype function. Original comment: need to override if some thumbnail
	 * work is to be done when mediaparserv2 enabled
	 */
	public void checkThumbnail() {
		// need to override if some thumbnail work is to be done when
		// mediaparserv2 enabled
	}

	/**
	 * Checks if a thumbnail exists, and, if not, generates one (if possible).
	 * Called from Request/RequestV2 in response to thumbnail requests e.g. HEAD
	 * /get/0$1$0$42$3/thumbnail0000%5BExample.mkv Calls
	 * DLNAMediaInfo.generateThumbnail, which in turn calls DLNAMediaInfo.parse.
	 *
	 * @param inputFile File to check or generate the thumbnail for.
	 */
	protected void checkThumbnail(InputFile inputFile) {
		// Use device-specific conf, if any
		if (mediaInfo != null && !mediaInfo.isThumbready() && renderer.getUmsConfiguration().isThumbnailGenerationEnabled() &&
			(renderer.isThumbnails())) {
			Double seekPosition = (double) renderer.getUmsConfiguration().getThumbnailSeekPos();
			if (isResume()) {
				Double resumePosition = resume.getTimeOffset() / 1000d;

				if (mediaInfo.getDurationInSeconds() > 0 && resumePosition < mediaInfo.getDurationInSeconds()) {
					seekPosition = resumePosition;
				}
			}

			mediaInfo.generateThumbnail(inputFile, getFormat(), getType(), seekPosition);
			if (!isResume() && mediaInfo.getThumb() != null && CONFIGURATION.getUseCache() && inputFile.getFile() != null) {
				MediaTableThumbnails.setThumbnail(mediaInfo.getThumb(), inputFile.getFile().getAbsolutePath(), -1);
			}
		}
	}

	/**
	 * Returns the input stream for this resource's generic thumbnail, which is
	 * the first of:
	 * <li>its Format icon, if any
	 * <li>the fallback image, if any
	 * <li>the {@link GenericIcons} icon <br>
	 * <br>
	 *
	 * @param fallback the fallback image, or {@code null}.
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
				FileInputStream inputStream = new FileInputStream(thumb);
				return DLNAThumbnailInputStream.toThumbnailInputStream(inputStream);
			}

			// A jar resource
			InputStream is = getResourceInputStream(thumb);
			if (is != null) {
				return DLNAThumbnailInputStream.toThumbnailInputStream(is);
			}

			// A URL
			try {
				return DLNAThumbnailInputStream.toThumbnailInputStream(HTTPResource.downloadAndSend(thumb, true));
			} catch (IOException e) {
				//dowwnload fail
			}
		}

		// Or none of the above
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
		String audioLanguageCode = mediaAudio != null ? mediaAudio.getLang() : null;
		if (StringUtils.isNotBlank(audioLanguageCode)) {
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
		String subsLanguageCode = mediaSubtitle != null && mediaSubtitle.getId() != MediaLang.DUMMY_ID ? mediaSubtitle.getLang() : null;

		if (StringUtils.isNotBlank(subsLanguageCode)) {
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
		// Show audio and subtitles language flags in the TRANSCODE folder only
		// for video files
		if ((isInsideTranscodeFolder() || parent instanceof SubSelFile) &&
			(mediaAudio != null || mediaSubtitle != null)) {
			if ((mediaInfo != null && mediaInfo.isVideo()) || (mediaInfo == null && format != null && format.isVideo())) {
				filterChain = addAudioFlagFilter(filterChain);
				filterChain = addSubtitlesFlagFilter(filterChain);
			}
		} else if (parent instanceof TranscodeVirtualFolder && this instanceof FileTranscodeVirtualFolder && mediaSubtitle != null) {
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
	 * @see #addChild(LibraryResource)
	 */
	public abstract boolean isValid();

	public boolean allowScan() {
		return false;
	}

	/**
	 * Returns the specific type of resource. Valid types are defined in
	 * {@link Format}.
	 *
	 * @return The specific type
	 */
	protected int getSpecificType() {
		return specificType;
	}

	/**
	 * Set the specific type of this resource. Valid types are defined in
	 * {@link Format}.
	 *
	 * @param specificType The specific type to set.
	 */
	protected void setSpecificType(int specificType) {
		this.specificType = specificType;
	}

	/**
	 * Returns the {@link Format} of this resource, which defines its
	 * capabilities.
	 *
	 * @return The format of this resource.
	 */
	public Format getFormat() {
		return format;
	}

	/**
	 * Sets the {@link Format} of this resource, thereby defining its
	 * capabilities.
	 *
	 * @param format The format to set.
	 */
	public void setFormat(Format format) {
		this.format = format;
	}

	/**
	 * Returns the {@link MediaInfo} object for this resource, containing
	 * the specifics of this resource, e.g. the duration.
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
	 * Returns the {@link MediaStatus} object for this resource, containing
	 * the status of this resource, e.g. the playback count.
	 *
	 * @return The object containing status information.
	 */
	public MediaStatus getMediaStatus() {
		return mediaStatus;
	}

	/**
	 * Sets the the {@link MediaStatus} object that contains all status for
	 * this resource.
	 *
	 * @param mediaStatus The object containing status information.
	 */
	public void setMediaStatus(MediaStatus mediaStatus) {
		this.mediaStatus = mediaStatus;
	}

	/**
	 * Returns the {@link MediaAudio} object for this resource that contains
	 * the audio specifics. A resource can have many audio tracks, this method
	 * returns the one that should be played.
	 *
	 * @return The audio object containing detailed information.
	 * @since 1.50
	 */
	public MediaAudio getMediaAudio() {
		return mediaAudio;
	}

	/**
	 * Sets the {@link MediaAudio} object for this resource that contains
	 * the audio specifics. A resource can have many audio tracks, this method
	 * determines the one that should be played.
	 *
	 * @param mediaAudio The audio object containing detailed information.
	 * @since 1.50
	 */
	public void setMediaAudio(MediaAudio mediaAudio) {
		this.mediaAudio = mediaAudio;
	}

	/**
	 * Returns the {@link MediaSubtitle} object for this resource that
	 * contains the specifics for the subtitles. A resource can have many
	 * subtitles, this method returns the one that should be displayed.
	 *
	 * @return The subtitle object containing detailed information.
	 * @since 1.50
	 */
	public MediaSubtitle getMediaSubtitle() {
		return mediaSubtitle;
	}

	/**
	 * Sets the {@link MediaSubtitle} object for this resource that contains
	 * the specifics for the subtitles. A resource can have many subtitles, this
	 * method determines the one that should be used.
	 *
	 * @param mediaSubtitle The subtitle object containing detailed information.
	 * @since 1.50
	 */
	public void setMediaSubtitle(MediaSubtitle mediaSubtitle) {
		this.mediaSubtitle = mediaSubtitle;
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
	protected void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	/**
	 * @return Whether this is a TV episode being accessed within a season
	 *         folder in the Media Library.
	 */
	public boolean isEpisodeWithinSeasonFolder() {
		return isEpisodeWithinSeasonFolder;
	}

	/**
	 * Sets whether this is a TV episode being accessed within a season folder
	 * in the Media Library.
	 *
	 * @param isEpisodeWithinSeasonFolder
	 */
	protected void setIsEpisodeWithinSeasonFolder(boolean isEpisodeWithinSeasonFolder) {
		this.isEpisodeWithinSeasonFolder = isEpisodeWithinSeasonFolder;
	}

	/**
	 * @return Whether this is a TV episode being accessed directly inside a TV
	 *         series folder in the Media Library
	 */
	public boolean isEpisodeWithinTVSeriesFolder() {
		return isEpisodeWithinTVSeriesFolder;
	}

	/**
	 * Sets whether this is a TV episode being accessed directly inside a TV
	 * series folder in the Media Library
	 *
	 * @param isEpisodeWithinTVSeriesFolder
	 */
	protected void setIsEpisodeWithinTVSeriesFolder(boolean isEpisodeWithinTVSeriesFolder) {
		this.isEpisodeWithinTVSeriesFolder = isEpisodeWithinTVSeriesFolder;
	}

	/**
	 * Returns the {@link Engine} object that is used to encode this resource
	 * for the renderer. Can be null.
	 *
	 * @return The engine object.
	 */
	public Engine getEngine() {
		return engine;
	}

	/**
	 * Sets the {@link Engine} object that is to be used to encode this resource
	 * for the renderer. The engine object can be null.
	 *
	 * @param engine The engine object to set.
	 * @since 1.50
	 */
	public void setEngine(Engine engine) {
		this.engine = engine;
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
	 *            otherwise.
	 * @since 1.50
	 */
	public void setDiscovered(boolean discovered) {
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
		if (mediaInfo == null || !mediaInfo.isVideo()) {
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
		if (mediaInfo == null || !mediaInfo.isVideo()) {
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
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return false;
		}

		List<MediaSubtitle> subtitlesList = mediaInfo.getSubtitlesTracks();
		if (subtitlesList != null) {
			for (MediaSubtitle subtitles : subtitlesList) {
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
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return;
		}

		synchronized (subtitlesLock) {
			if (!forceRefresh && isExternalSubtitlesParsed) {
				return;
			}

			File file = this instanceof RealFile ? ((RealFile) this).getFile() : new File(getFileName());
			if (file == null || mediaInfo == null) {
				isExternalSubtitlesParsed = true;
				return;
			}

			if (!renderer.getUmsConfiguration().isDisableSubtitles() && renderer.getUmsConfiguration().isAutoloadExternalSubtitles()) {
				SubtitleUtils.searchAndAttachExternalSubtitles(file, mediaInfo, forceRefresh);
				// update the database if enabled
				if (CONFIGURATION.getUseCache() && mediaInfo.isMediaParsed() && !mediaInfo.isParsing()) {
					Connection connection = null;
					try {
						connection = MediaDatabase.getConnectionIfAvailable();
						if (connection != null) {
							//handle autocommit
							boolean currentAutoCommit = connection.getAutoCommit();
							if (currentAutoCommit) {
								connection.setAutoCommit(false);
							}
							MediaTableFiles.insertOrUpdateData(connection, file.getAbsolutePath(), file.lastModified(), getType(), mediaInfo);
							if (currentAutoCommit) {
								connection.commit();
								connection.setAutoCommit(true);
							}
						}
					} catch (SQLException e) {
						LOGGER.error("Database error while trying to add parsed information for \"{}\" to the cache: {}", file,
							e.getMessage());
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("SQL error code: {}", e.getErrorCode());
							if (e.getCause() instanceof SQLException &&
								((SQLException) e.getCause()).getErrorCode() != e.getErrorCode()) {
								LOGGER.trace("Cause SQL error code: {}", ((SQLException) e.getCause()).getErrorCode());
							}
							LOGGER.trace("", e);
						}
					} finally {
						MediaDatabase.close(connection);
					}
				}
			}

			List<MediaSubtitle> subtitlesList = mediaInfo.getSubtitlesTracks();
			if (subtitlesList != null) {
				hasSubtitles = !subtitlesList.isEmpty();
				for (MediaSubtitle subtitles : subtitlesList) {
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
	 * existing {@link MediaSubtitle} instances.
	 * <p>
	 * <b>WARNING:</b> This should only be called when the subtitles tracks has
	 * been populated by an alternative source like the database. Setting this
	 * under other circumstances will break the implemented automatic parsing
	 * and caching.
	 */
	public void setExternalSubtitlesParsed() {
		if (mediaInfo == null || !mediaInfo.isVideo()) {
			return;
		}

		synchronized (subtitlesLock) {
			if (isExternalSubtitlesParsed) {
				return;
			}

			List<MediaSubtitle> subtitlesList = mediaInfo.getSubtitlesTracks();
			hasSubtitles = !subtitlesList.isEmpty();
			for (MediaSubtitle subtitles : subtitlesList) {
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
	 * {@link MediaInfo} metadata and configuration settings.
	 *
	 * @return The resolved {@link MediaAudio} or {@code null}.
	 */
	public MediaAudio resolveAudioStream() {
		if (mediaInfo == null || mediaInfo.getAudioTrackCount() == 0) {
			LOGGER.trace("Found no audio track");
			return null;
		}

		// check for preferred audio
		MediaAudio dtsTrack = null;
		StringTokenizer st = new StringTokenizer(renderer.getUmsConfiguration().getAudioLanguages(), ",");
		while (st.hasMoreTokens()) {
			String lang = st.nextToken().trim();
			LOGGER.trace("Looking for an audio track with language \"{}\" for \"{}\"", lang, getName());
			for (MediaAudio audio : mediaInfo.getAudioTracks()) {
				if (audio.matchCode(lang)) {
					LOGGER.trace("Matched audio track: {}", audio);
					return audio;
				}

				if (dtsTrack == null && audio.isDTS()) {
					dtsTrack = audio;
				}
			}
		}

		// preferred audio not found, take a default audio track, dts first if
		// available
		if (dtsTrack != null) {
			LOGGER.trace("Preferring DTS audio track since no language match was found: {}", dtsTrack);
			return dtsTrack;
		}
		MediaAudio result = mediaInfo.getDefaultAudioTrack();
		LOGGER.trace("Using the first available audio track: {}", result);
		return result;
	}

	/**
	 * This method figures out which subtitles track should be used based on
	 * {@link MediaInfo} metadata, chosen audio language and configuration
	 * settings.
	 *
	 * @param audioLanguage the {@code ISO 639} language code for the chosen
	 *            audio language or {@code null} if it doesn't apply.
	 * @param forceRefresh if {@code true} forces a new scan for external
	 *            subtitles instead of relying on cached information (if it
	 *            exists).
	 * @return The resolved {@link MediaSubtitle} or {@code null}.
	 */
	public MediaSubtitle resolveSubtitlesStream(String audioLanguage, boolean forceRefresh) {
		if (mediaInfo == null) {
			return null;
		}

		// Use device-specific conf
		if (renderer.getUmsConfiguration().isDisableSubtitles()) {
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
		MediaSubtitle matchedSub;
		boolean useExternal = renderer.getUmsConfiguration().isAutoloadExternalSubtitles();
		boolean forceExternal = renderer.getUmsConfiguration().isForceExternalSubtitles();
		String audioSubLanguages = renderer.getUmsConfiguration().getAudioSubLanguages();

		if (forceExternal) {
			matchedSub = getHighestPriorityExternalSubtitles();
			if (matchedSub == null) {
				LOGGER.trace("No external subtitles candidates were found to force for \"{}\"", getName());
			} else {
				LOGGER.trace("Forcing external subtitles track for \"{}\": {}", getName(), matchedSub);
				return matchedSub;
			}
		}

		if (StringUtils.isBlank(audioLanguage) || StringUtils.isBlank(audioSubLanguages)) {
			// Not enough information to do a full audio/subtitles combination
			// search, only use the preferred subtitles
			LOGGER.trace("Searching for subtitles without considering audio language for \"{}\"", getName());
			ArrayList<MediaSubtitle> candidates = new ArrayList<>();
			for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
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
					LOGGER.trace("Matched {} subtitles track for \"{}\" with unknown audio language: {}",
						matchedSub.isExternal() ? "external" : "internal", getName(), matchedSub);
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
					LOGGER.trace("Searching for a match for audio language \"{}\" with audio \"{}\" and subtitles \"{}\" for \"{}\"",
						audioLanguage, audio, sub, getName());

					if ("*".equals(audio) || MediaLang.UND.equals(audio) || Iso639.isCodesMatching(audio, audioLanguage)) {
						boolean anyLanguage = "*".equals(sub) || MediaLang.UND.equals(sub);
						if ("off".equals(sub)) {
							LOGGER.trace("Not looking for non-forced subtitles since they are \"off\" for audio language \"{}\"", audio);
							break;
						} else {
							ArrayList<MediaSubtitle> candidates = new ArrayList<>();
							for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
								if (anyLanguage || subtitles.matchCode(sub)) {
									if (subtitles.isEmbedded()) {
										candidates.add(subtitles);
										LOGGER.trace("Adding internal subtitles candidate: {}", subtitles);
									} else if (useExternal) {
										candidates.add(subtitles);
										LOGGER.trace("Adding external subtitles candidate: {}", subtitles);
									} else {
										LOGGER.trace(
											"Ignoring external subtitles because auto loading of external subtitles is disabled: {}",
											subtitles);
									}
								}
							}
							if (!candidates.isEmpty()) {
								matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, !anyLanguage);
								if (matchedSub != null) {
									LOGGER.trace("Matched {} subtitles track for \"{}\": {}",
										matchedSub.isExternal() ? "external" : "internal", getName(), matchedSub);
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

		// Check for forced subtitles.
		String forcedTags = renderer.getUmsConfiguration().getForcedSubtitleTags();
		if (StringUtils.isNotBlank(forcedTags)) {
			Locale locale = PMS.getLocale();
			ArrayList<String> forcedTagsList = new ArrayList<>();
			for (String forcedTag : forcedTags.split(",")) {
				if (StringUtils.isNotBlank(forcedTag)) {
					forcedTagsList.add(forcedTag.trim().toLowerCase(locale));
				}
			}
			ArrayList<MediaSubtitle> candidates = new ArrayList<>();
			String forcedLanguage = renderer.getUmsConfiguration().getForcedSubtitleLanguage();
			boolean anyLanguage = StringUtils.isBlank(forcedLanguage) || "*".equals(forcedLanguage) || MediaLang.UND.equals(forcedLanguage);
			for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
				if (!useExternal && subtitles.isExternal()) {
					continue;
				}
				String title = subtitles.getTitle();
				if (StringUtils.isNotBlank(title)) {
					title = title.toLowerCase(locale);
					for (String forcedTag : forcedTagsList) {
						if (title.contains(forcedTag) && (anyLanguage || Iso639.isCodesMatching(subtitles.getLang(), forcedLanguage))) {
							candidates.add(subtitles);
							LOGGER.trace("Adding {} forced subtitles candidate that matched tag \"{}\": {}",
								subtitles.isExternal() ? "external" : "internal", forcedTag, subtitles);
						}
					}
				}
			}
			if (!candidates.isEmpty()) {
				matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, false);
				if (matchedSub != null) {
					LOGGER.trace("Using forced {} subtitles track for \"{}\": {}", matchedSub.isExternal() ? "external" : "internal",
						getName(), matchedSub);
					return matchedSub;
				}
			}
		}

		LOGGER.trace("Found no matching subtitle for \"{}\"", getName());
		return null;
	}

	private MediaSubtitle getHighestPriorityExternalSubtitles() {
		MediaSubtitle matchedSub = null;

		ArrayList<MediaSubtitle> candidates = new ArrayList<>();
		for (MediaSubtitle subtitles : mediaInfo.getSubtitlesTracks()) {
			if (subtitles.isExternal()) {
				candidates.add(subtitles);
			}
		}

		// If external subtitles were found, let findPrioritizedSubtitles return
		// the right one
		if (!candidates.isEmpty()) {
			matchedSub = SubtitleUtils.findPrioritizedSubtitles(candidates, renderer, true);
		}

		// Return either the matched external subtitles, or null if there was no
		// external match
		return matchedSub;
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
	 * Returns the from - to time range for this resource.
	 *
	 * @return The time range.
	 */
	public TimeRange getSplitRange() {
		return splitRange;
	}

	/**
	 * Sets the from - to time range for this resource.
	 *
	 * @param splitRange The time range to set.
	 * @since 1.50
	 */
	public void setSplitRange(TimeRange splitRange) {
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
	public Renderer getDefaultRenderer() {
		return renderer;
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
	 *            otherwise.
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
	public List<LibraryResource> getChildren() {
		return children;
	}

	/**
	 * Sets the list of children for this resource.
	 *
	 * @param children The list of children to set.
	 * @since 1.50
	 */
	protected void setChildren(List<LibraryResource> children) {
		this.children = children;
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

	private boolean depthLimit() {
		LibraryResource tmp = this;
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
		return
			CONFIGURATION.getFullyPlayedAction() == FullyPlayedAction.HIDE_MEDIA &&
			mediaInfo != null &&
			mediaInfo.isVideo() &&
			mediaStatus != null &&
			mediaStatus.isFullyPlayed();
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

	public boolean isSubSelectable() {
		return false;
	}

	////////////////////////////////////////////////////
	// Resume handling
	////////////////////////////////////////////////////

	private void internalStop() {
		LibraryResource res = resumeStop();
		final RootFolder root = ((renderer != null) ? renderer.getRootFolder() : null);
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

	private LibraryResource resumeStop() {
		if (!CONFIGURATION.isResumeEnabled() || !isResumeable()) {
			return null;
		}

		notifyRefresh();
		if (resume != null) {
			resume.stop(startTime, (long) (mediaInfo.getDurationInSeconds() * 1000));
			if (resume.isDone()) {
				parent.getChildren().remove(this);
			} else if (getMediaInfo() != null) {
				mediaInfo.setThumbready(false);
			}
		} else {
			for (LibraryResource res : parent.getChildren()) {
				if (res.isResume() && res.getName().equals(getName())) {
					res.resume.stop(startTime, (long) (mediaInfo.getDurationInSeconds() * 1000));
					if (res.resume.isDone()) {
						parent.getChildren().remove(res);
						return null;
					}

					if (res.getMediaInfo() != null) {
						res.mediaInfo.setThumbready(false);
					}

					return res;
				}
			}

			ResumeObj r = ResumeObj.store(this, startTime);
			if (r != null) {
				LibraryResource clone = this.clone();
				clone.resume = r;
				clone.resHash = resHash;
				if (clone.mediaInfo != null) {
					clone.mediaInfo.setThumbready(false);
				}

				clone.engine = engine;
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
		return CONFIGURATION.getMinimumWatchedPlayTime();
	}

	public String resumeStr(String s) {
		if (isResume()) {
			return Messages.getString("Resume") + ": " + s;
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
	 * bookmarked, i.e. serialized to an external file. By default it just
	 * returns null which means the resource is ignored when serializing.
	 */
	public String write() {
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


	// Returns whether the url appears to be ours
	public static boolean isResourceUrl(String url) {
		return url != null && url.startsWith(MediaServer.getURL() + "/get/");
	}

	// Returns the url's resourceId (i.e. index without trailing filename) if
	// any or null
	public static String parseResourceId(String url) {
		return isResourceUrl(url) ? StringUtils.substringBetween(url + "/", "get/", "/") : null;
	}

	// Returns the url's objectId (i.e. index including trailing filename) if
	// any or null
	public static String parseObjectId(String url) {
		return isResourceUrl(url) ? StringUtils.substringAfter(url, "get/") : null;
	}

	public boolean isRendererAllowed() {
		return true;
	}

	public LibraryResource isCoded() {
		LibraryResource tmp = this;
		while (tmp != null) {
			if (tmp instanceof CodeEnter) {
				return tmp;
			}

			tmp = tmp.getParent();
		}

		return null;
	}

	public boolean isCodeValid(LibraryResource r) {
		LibraryResource res = r.isCoded();
		if (res instanceof CodeEnter codeEnter) {
			return codeEnter.validCode(r);
		}

		// normal case no code in path code is always valid
		return true;
	}

	/**
	 * @return whether the play events (like "Started playing" and "Stopped
	 *         playing" will be logged.
	 */
	public boolean isLogPlayEvents() {
		return true;
	}

	public long getStartTime() {
		return startTime;
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
			dynamicPls = new VirtualFolder(renderer, Messages.getString("DynamicPlaylist_FolderName"), null);
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

	public String getResolutionForKeepAR(int scaleWidth, int scaleHeight) {
		double videoAspectRatio = (double) scaleWidth / (double) scaleHeight;
		double rendererAspectRatio = 1.777777777777778;
		if (videoAspectRatio > rendererAspectRatio) {
			scaleHeight = (int) Math.round(scaleWidth / rendererAspectRatio);
		} else {
			scaleWidth = (int) Math.round(scaleHeight * rendererAspectRatio);
		}

		scaleWidth = Engine.convertToModX(scaleWidth, 4);
		scaleHeight = Engine.convertToModX(scaleHeight, 4);
		return scaleWidth + "x" + scaleHeight;
	}


	/**
	 * Stores the file in the cache if it doesn't already exist.
	 *
	 * @param file the full path to the file.
	 * @param formatType the type constant defined in {@link Format}.
	 */
	protected void storeFileInCache(File file, int formatType) {
		if (CONFIGURATION.getUseCache() && MediaDatabase.isAvailable()) {
			Connection connection = null;
			try {
				connection = MediaDatabase.getConnectionIfAvailable();
				if (connection != null && !MediaTableFiles.isDataExists(connection, file.getAbsolutePath(), file.lastModified())) {
					//handle autocommit
					boolean currentAutoCommit = connection.getAutoCommit();
					if (currentAutoCommit) {
						connection.setAutoCommit(false);
					}
					MediaTableFiles.insertOrUpdateData(connection, file.getAbsolutePath(), file.lastModified(), formatType, null);
					if (currentAutoCommit) {
						connection.commit();
						connection.setAutoCommit(true);
					}
				}
			} catch (SQLException e) {
				LOGGER.error("Database error while trying to store \"{}\" in the cache: {}", file.getName(), e.getMessage());
				LOGGER.trace("", e);
			} finally {
				MediaDatabase.close(connection);
			}
		}
	}

	public boolean isAddToMediaLibrary() {
		return true;
	}

	/**
	 * @return whether the media should be marked as "fully played" either with
	 * text or a "fully played" overlay.
	 */
	public boolean isFullyPlayedMark() {
		return false;
	}

	/**
	 * Create a path of virtual folders if it doesn't already exist.
	 *
	 * @param parentPath the full virtual folder path (slash-delimited).
	 */
	protected LibraryResource getSharedContentParent(String parentPath) {
		LibraryResource result = null;
		if (parentPath != null) {
			StringTokenizer st = new StringTokenizer(parentPath, "/");
			LibraryResource currentRoot = this;
			while (st.hasMoreTokens()) {
				String folder = st.nextToken();
				result = currentRoot.searchByName(folder);
				if (result == null) {
					result = new VirtualFolder(renderer, folder, "");
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
	 * Returns an InputStream associated with the fileName.
	 * @param fileName TODO Absolute or relative file path.
	 * @return If found, an InputStream associated with the fileName. null otherwise.
	 */
	private InputStream getResourceInputStream(String fileName) {
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
	public LibraryResource clone() {
		LibraryResource o = null;
		try {
			o = (LibraryResource) super.clone();
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
		result.append(", format=");
		result.append(format);
		result.append(", discovered=");
		result.append(isDiscovered());
		result.append(']');
		return result.toString();
	}

	/**
	 * Transforms a String to URL encoded UTF-8.
	 *
	 * @param s
	 * @return Transformed string s in UTF-8 encoding.
	 */
	private static String encode(String s) {
		try {
			if (s == null) {
				return "";
			}
			return URLEncoder.encode(s, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			LOGGER.debug("Error while URL encoding \"{}\": {}", s, e.getMessage());
			LOGGER.trace("", e);
		}

		return "";
	}

}
