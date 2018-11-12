package net.pms.configuration;

import static org.apache.commons.lang3.StringUtils.isBlank;
import com.sun.jna.Platform;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.*;
import net.pms.dlna.DLNAMediaInfo.Mode3D;
import net.pms.encoders.Player;
import net.pms.formats.Format;
import net.pms.formats.Format.Identifier;
import net.pms.formats.v2.AudioProperties;
import net.pms.io.OutputParams;
import net.pms.network.HTTPResource;
import net.pms.network.SpeedStats;
import net.pms.network.UPNPHelper;
import net.pms.newgui.StatusTab;
import net.pms.util.BasicPlayer;
import net.pms.util.FileWatcher;
import net.pms.util.FormattableColor;
import net.pms.util.InvalidArgumentException;
import net.pms.util.PropertiesUtil;
import net.pms.util.StringUtil;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererConfiguration extends UPNPHelper.Renderer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererConfiguration.class);
	protected static TreeSet<RendererConfiguration> enabledRendererConfs;
	protected static final ArrayList<String> allRenderersNames = new ArrayList<>();
	protected static PmsConfiguration _pmsConfiguration = PMS.getConfiguration();
	protected static RendererConfiguration defaultConf;
	protected static final Map<InetAddress, RendererConfiguration> addressAssociation = new HashMap<>();

	protected RootFolder rootFolder;
	protected File file;
	protected Configuration configuration;
	protected PmsConfiguration pmsConfiguration = _pmsConfiguration;
	protected ConfigurationReader configurationReader;
	protected FormatConfiguration formatConfiguration;
	protected int rank;
	protected Matcher sortedHeaderMatcher;
	protected List<String> identifiers = null;

	public StatusTab.RendererItem gui;
	public boolean loaded, fileless = false;
	protected BasicPlayer player;

	public static final File NOFILE = new File("NOFILE");

	public interface OutputOverride {
		/**
		 * Override a player's default output formatting.
		 * To be invoked by the player after input and filter options are complete.
		 *
		 * @param cmdList the command so far
		 * @param dlna the media item
		 * @param player the player
		 * @param params the output parameters
		 *
		 * @return whether the options have been finalized
		 */
		public boolean getOutputOptions(List<String> cmdList, DLNAResource dlna, Player player, OutputParams params);

		public boolean addSubtitles();
	}

	// Holds MIME type aliases
	protected Map<String, String> mimes;

	protected Map<String, String> charMap;
	protected Map<String, String> DLNAPN;

	// TextWrap parameters
	protected int lineWidth, lineHeight, indent;
	protected String inset, dots;

	// property values
	protected static final String LPCM = "LPCM";
	protected static final String MP3 = "MP3";
	protected static final String WAV = "WAV";
	protected static final String WMV = "WMV";

	// Old video transcoding options
	@Deprecated
	protected static final String DEPRECATED_MPEGAC3 = "MPEGAC3";

	@Deprecated
	protected static final String DEPRECATED_MPEGPSAC3 = "MPEGPSAC3";

	@Deprecated
	protected static final String DEPRECATED_MPEGTSAC3 = "MPEGTSAC3";

	@Deprecated
	protected static final String DEPRECATED_H264TSAC3 = "H264TSAC3";

	// Current video transcoding options
	protected static final String MPEGTSH264AAC = "MPEGTS-H264-AAC";
	protected static final String MPEGTSH264AC3 = "MPEGTS-H264-AC3";
	protected static final String MPEGTSH265AAC = "MPEGTS-H265-AAC";
	protected static final String MPEGTSH265AC3 = "MPEGTS-H265-AC3";
	protected static final String MPEGPSMPEG2AC3 = "MPEGPS-MPEG2-AC3";
	protected static final String MPEGTSMPEG2AC3 = "MPEGTS-MPEG2-AC3";

	// property names
	protected static final String ACCURATE_DLNA_ORGPN = "AccurateDLNAOrgPN";
	protected static final String AUDIO = "Audio";
	protected static final String AUTO_PLAY_TMO = "AutoPlayTmo";
	protected static final String BYTE_TO_TIMESEEK_REWIND_SECONDS = "ByteToTimeseekRewindSeconds"; // Ditlew
	protected static final String CBR_VIDEO_BITRATE = "CBRVideoBitrate"; // Ditlew
	protected static final String CHARMAP = "CharMap";
	protected static final String CHUNKED_TRANSFER = "ChunkedTransfer";
	protected static final String CUSTOM_FFMPEG_OPTIONS = "CustomFFmpegOptions";
	protected static final String CUSTOM_MENCODER_OPTIONS = "CustomMencoderOptions";
	protected static final String CUSTOM_MENCODER_MPEG2_OPTIONS = "CustomMencoderQualitySettings"; // TODO (breaking change): value should be CustomMEncoderMPEG2Options
	protected static final String DEFAULT_VBV_BUFSIZE = "DefaultVBVBufSize";
	protected static final String DEVICE_ID = "Device";
	protected static final String DISABLE_MENCODER_NOSKIP = "DisableMencoderNoskip";
	protected static final String DLNA_LOCALIZATION_REQUIRED = "DLNALocalizationRequired";
	protected static final String DLNA_ORGPN_USE = "DLNAOrgPN";
	protected static final String DLNA_PN_CHANGES = "DLNAProfileChanges";
	protected static final String DLNA_TREE_HACK = "CreateDLNATreeFaster";
	protected static final String EMBEDDED_SUBS_SUPPORTED = "InternalSubtitlesSupported";
	protected static final String HALVE_BITRATE = "HalveBitrate";
	protected static final String H264_L41_LIMITED = "H264Level41Limited";
	protected static final String IGNORE_TRANSCODE_BYTE_RANGE_REQUEST = "IgnoreTranscodeByteRangeRequests";
	protected static final String IMAGE = "Image";
	protected static final String KEEP_ASPECT_RATIO = "KeepAspectRatio";
	protected static final String KEEP_ASPECT_RATIO_TRANSCODING = "KeepAspectRatioTranscoding";
	protected static final String LIMIT_FOLDERS = "LimitFolders";
	protected static final String LOADING_PRIORITY = "LoadingPriority";
	protected static final String MAX_VIDEO_BITRATE = "MaxVideoBitrateMbps";
	protected static final String MAX_VIDEO_HEIGHT = "MaxVideoHeight";
	protected static final String MAX_VIDEO_WIDTH = "MaxVideoWidth";
	protected static final String MAX_VOLUME = "MaxVolume";
	protected static final String MEDIAPARSERV2 = "MediaInfo";
	protected static final String MEDIAPARSERV2_THUMB = "MediaParserV2_ThumbnailGeneration";
	protected static final String MIME_TYPES_CHANGES = "MimeTypesChanges";
	protected static final String MUX_DTS_TO_MPEG = "MuxDTSToMpeg";
	protected static final String MUX_H264_WITH_MPEGTS = "MuxH264ToMpegTS";
	protected static final String MUX_LPCM_TO_MPEG = "MuxLPCMToMpeg";
	protected static final String MUX_NON_MOD4_RESOLUTION = "MuxNonMod4Resolution";
	protected static final String NOT_AGGRESSIVE_BROWSING = "NotAggressiveBrowsing";
	protected static final String OFFER_SUBTITLES_BY_PROTOCOL_INFO = "OfferSubtitlesByProtocolInfo";
	protected static final String OFFER_SUBTITLES_AS_SOURCE = "OfferSubtitlesAsSource";
	protected static final String OUTPUT_3D_FORMAT = "Output3DFormat";
	protected static final String OVERRIDE_FFMPEG_VF = "OverrideFFmpegVideoFilter";
	protected static final String PREPEND_TRACK_NUMBERS = "PrependTrackNumbers";
	protected static final String PUSH_METADATA = "PushMetadata";
	protected static final String REMOVE_TAGS_FROM_SRT_SUBS = "RemoveTagsFromSRTSubtitles";
	protected static final String RENDERER_ICON = "RendererIcon";
	protected static final String RENDERER_NAME = "RendererName";
	protected static final String RESCALE_BY_RENDERER = "RescaleByRenderer";
	protected static final String SEEK_BY_TIME = "SeekByTime";
	protected static final String SEND_DATE_METADATA = "SendDateMetadata";
	protected static final String SEND_FOLDER_THUMBNAILS = "SendFolderThumbnails";
	protected static final String SHOW_AUDIO_METADATA = "ShowAudioMetadata";
	protected static final String SHOW_DVD_TITLE_DURATION = "ShowDVDTitleDuration"; // Ditlew
	protected static final String SHOW_SUB_METADATA = "ShowSubMetadata";
	protected static final String STREAM_EXT = "StreamExtensions";
	protected static final String STREAM_SUBS_FOR_TRANSCODED_VIDEO = "StreamSubsForTranscodedVideo";
	protected static final String SUBTITLE_HTTP_HEADER = "SubtitleHttpHeader";
	protected static final String SUPPORTED = "Supported";
	protected static final String SUPPORTED_VIDEO_BIT_DEPTHS = "SupportedVideoBitDepths";
	protected static final String SUPPORTED_EXTERNAL_SUBTITLES_FORMATS = "SupportedExternalSubtitlesFormats";
	protected static final String SUPPORTED_INTERNAL_SUBTITLES_FORMATS = "SupportedInternalSubtitlesFormats";
	protected static final String TEXTWRAP = "TextWrap";
	protected static final String THUMBNAIL_PADDING = "ThumbnailPadding";
	protected static final String THUMBNAILS = "Thumbnails";
	protected static final String TRANSCODE_AUDIO = "TranscodeAudio";
	protected static final String TRANSCODE_AUDIO_441KHZ = "TranscodeAudioTo441kHz";
	protected static final String TRANSCODE_EXT = "TranscodeExtensions";
	protected static final String TRANSCODE_FAST_START = "TranscodeFastStart";
	protected static final String TRANSCODE_VIDEO = "TranscodeVideo";
	protected static final String TRANSCODED_SIZE = "TranscodedVideoFileSize";
	protected static final String TRANSCODED_VIDEO_AUDIO_SAMPLE_RATE = "TranscodedVideoAudioSampleRate";
	protected static final String UPNP_DETAILS = "UpnpDetailsSearch";
	protected static final String UPNP_ALLOW = "UpnpAllow";
	protected static final String USER_AGENT = "UserAgentSearch";
	protected static final String USER_AGENT_ADDITIONAL_HEADER = "UserAgentAdditionalHeader";
	protected static final String USER_AGENT_ADDITIONAL_SEARCH = "UserAgentAdditionalHeaderSearch";
	protected static final String USE_CLOSED_CAPTION = "UseClosedCaption";
	protected static final String USE_SAME_EXTENSION = "UseSameExtension";
	protected static final String VIDEO = "Video";
	protected static final String VIDEO_FORMATS_SUPPORTING_STREAMED_EXTERNAL_SUBTITLES = "VideoFormatsSupportingStreamedExternalSubtitles";
	protected static final String WRAP_DTS_INTO_PCM = "WrapDTSIntoPCM";
	protected static final String WRAP_ENCODED_AUDIO_INTO_PCM = "WrapEncodedAudioIntoPCM";

	// Deprecated property names
	@Deprecated
	protected static final String THUMBNAIL_BG = "ThumbnailBackground";
	@Deprecated
	protected static final String THUMBNAIL_SIZE = "ThumbnailSize";

	private static int maximumBitrateTotal = 0;
	public static final String UNKNOWN_ICON = "unknown.png";

	public static RendererConfiguration getDefaultConf() {
		return defaultConf;
	}

	public ConfigurationReader getConfigurationReader() {
		return configurationReader;
	}

	/**
	 * {@link #enabledRendererConfs} doesn't normally need locking since
	 * modification is rare and {@link #loadRendererConfigurations(PmsConfiguration)}
	 * is only called during {@link PMS#init()} (To avoid any chance of a
	 * race condition proper locking should be implemented though). During
	 * build on the other hand the method is called repeatedly and it is random
	 * if a {@link ConcurrentModificationException} is thrown as a result.
	 *
	 * To avoid build problems, this is used to make sure that calls to
	 * {@link #loadRendererConfigurations(PmsConfiguration)} is serialized.
	 */
	public static final Object loadRendererConfigurationsLock = new Object();

	/**
	 * Load all renderer configuration files and set up the default renderer.
	 *
	 * @param pmsConf
	 */
	public static void loadRendererConfigurations(PmsConfiguration pmsConf) {
		synchronized(loadRendererConfigurationsLock) {
			_pmsConfiguration = pmsConf;
			enabledRendererConfs = new TreeSet<>(rendererLoadingPriorityComparator);

			try {
				defaultConf = new RendererConfiguration();
			} catch (ConfigurationException e) {
				LOGGER.debug("Caught exception", e);
			}

			File renderersDir = getRenderersDir();

			if (renderersDir != null) {
				LOGGER.info("Loading renderer configurations from " + renderersDir.getAbsolutePath());

				File[] confs = renderersDir.listFiles();
				Arrays.sort(confs);
				int rank = 1;

				List<String> selectedRenderers = pmsConf.getSelectedRenderers();
				for (File f : confs) {
					if (f.getName().endsWith(".conf")) {
						try {
							RendererConfiguration r = new RendererConfiguration(f);
							r.rank = rank++;
							String rendererName = r.getConfName();
							allRenderersNames.add(rendererName);
							String renderersGroup = null;
							if (rendererName.indexOf(' ') > 0) {
								renderersGroup = rendererName.substring(0, rendererName.indexOf(' '));
							}

							if (selectedRenderers.contains(rendererName) || selectedRenderers.contains(renderersGroup) || selectedRenderers.contains(pmsConf.ALL_RENDERERS)) {
								enabledRendererConfs.add(r);
							} else {
								LOGGER.debug("Ignored \"{}\" configuration", rendererName);
							}
						} catch (ConfigurationException ce) {
							LOGGER.info("Error in loading configuration of: " + f.getAbsolutePath());
						}
					}
				}
			}
		}

		LOGGER.info("Enabled " + enabledRendererConfs.size() + " configurations, listed in order of loading priority:");
		for (RendererConfiguration r : enabledRendererConfs) {
			LOGGER.info(":   " + r);
		}

		if (enabledRendererConfs.size() > 0) {
			// See if a different default configuration was configured
			String rendererFallback = pmsConf.getRendererDefault();

			if (StringUtils.isNotBlank(rendererFallback)) {
				RendererConfiguration fallbackConf = getRendererConfigurationByName(rendererFallback);

				if (fallbackConf != null) {
					// A valid fallback configuration was set, use it as default.
					defaultConf = fallbackConf;
				}
			}
		}
		Collections.sort(allRenderersNames, String.CASE_INSENSITIVE_ORDER);
		DeviceConfiguration.loadDeviceConfigurations(pmsConf);
	}

	public int getInt(String key, int def) {
		return configurationReader.getInt(key, def);
	}

	public long getLong(String key, long def) {
		return configurationReader.getLong(key, def);
	}

	public double getDouble(String key, double def) {
		return configurationReader.getDouble(key, def);
	}

	public boolean getBoolean(String key, boolean def) {
		return configurationReader.getBoolean(key, def);
	}

	public String getString(String key, String def) {
		return configurationReader.getNonBlankConfigurationString(key, def);
	}

	public List<String> getStringList(String key, String def) {
		List<String> result = configurationReader.getStringList(key, def);
		if (result.size() == 1 && result.get(0).equalsIgnoreCase("None")) {
			return new ArrayList<>();
		}
		return result;
	}

	public void setStringList(String key, List<String> value) {
		StringBuilder result = new StringBuilder();
		for (String element : value) {
			if (!result.toString().equals("")) {
				result.append(", ");
			}
			result.append(element);
		}
		if (result.toString().equals("")) {
			result.append("None");
		}
		configuration.setProperty(key, result.toString());
	}

	public Color getColor(String key, String defaultValue) {
		String colorString = getString(key, defaultValue);
		if (!StringUtils.isBlank(colorString)) {
			try {
				return new FormattableColor(colorString);
			} catch (InvalidArgumentException e) {
				LOGGER.error(e.getMessage());
				LOGGER.trace("", e);
			}
		}
		if (StringUtils.isBlank(defaultValue)) {
			return null;
		}
		try {
			return new FormattableColor(defaultValue);
		} catch (InvalidArgumentException e) {
			LOGGER.error("Invalid default value: {}", e.getMessage());
			LOGGER.trace("", e);
			return null;
		}
	}

	@Deprecated
	public static ArrayList<RendererConfiguration> getAllRendererConfigurations() {
		return getEnabledRenderersConfigurations();
	}

	public boolean nox264() {
		return false;
	}

	/**
	 * Returns the list of enabled renderer configurations.
	 *
	 * @return The list of enabled renderers.
	 */
	public static ArrayList<RendererConfiguration> getEnabledRenderersConfigurations() {
		return enabledRendererConfs != null ? new ArrayList(enabledRendererConfs) : null;
	}

	/**
	 * Returns the list of all connected renderer devices.
	 *
	 * @return The list of connected renderers.
	 */
	public static Collection<RendererConfiguration> getConnectedRenderersConfigurations() {
		// We need to check both UPnP and http sides to ensure a complete list
		HashSet<RendererConfiguration> renderers = new HashSet<>(UPNPHelper.getRenderers(UPNPHelper.ANY));
		renderers.addAll(addressAssociation.values());
		// Ensure any remaining secondary common-ip renderers (which are no longer in address association) are added
		renderers.addAll(PMS.get().getFoundRenderers());
		return renderers;
	}

	public static boolean hasConnectedAVTransportPlayers() {
		return UPNPHelper.hasRenderer(UPNPHelper.AVT);
	}

	public static List<RendererConfiguration> getConnectedAVTransportPlayers() {
		return UPNPHelper.getRenderers(UPNPHelper.AVT);
	}

	public static boolean hasConnectedRenderer(int type) {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if ((r.controls & type) != 0) {
				return true;
			}
		}
		return false;
	}

	public static List<RendererConfiguration> getConnectedRenderers(int type) {
		ArrayList<RendererConfiguration> renderers = new ArrayList<>();
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if (r.active && (r.controls & type) != 0) {
				renderers.add(r);
			}
		}
		return renderers;
	}

	public static boolean hasConnectedControlPlayers() {
		return hasConnectedRenderer(UPNPHelper.ANY);
	}

	public static List<RendererConfiguration> getConnectedControlPlayers() {
		return getConnectedRenderers(UPNPHelper.ANY);
	}

	/**
	 * Searches for an instance of this renderer connected at the given address.
	 *
	 * @param r the renderer.
	 * @param ia the address.
	 * @return the matching renderer or null.
	 */
	public static RendererConfiguration find(RendererConfiguration r, InetAddress ia) {
		return find(r.getConfName(), ia);
	}

	/**
	 * Searches for a renderer of this name connected at the given address.
	 *
	 * @param name the renderer name.
	 * @param ia the address.
	 * @return the matching renderer or null.
	 */
	public static RendererConfiguration find(String name, InetAddress ia) {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			if (ia.equals(r.getAddress()) && name.equals(r.getConfName())) {
				return r;
			}
		}
		return null;
	}

	public static File getRenderersDir() {
		final String[] pathList = PropertiesUtil.getProjectProperties().get("project.renderers.dir").split(",");

		for (String path : pathList) {
			if (path.trim().length() > 0) {
				File file = new File(path.trim());

				if (file.isDirectory()) {
					if (file.canRead()) {
						return file;
					}
					LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
				}
			}
		}

		return null;
	}

	public static void resetAllRenderers() {
		for (RendererConfiguration r : getConnectedRenderersConfigurations()) {
			r.rootFolder = null;
		}
		// Resetting enabledRendererConfs isn't strictly speaking necessary any more, since
		// these are now for reference only and never actually populate their root folders.
		for (RendererConfiguration r : enabledRendererConfs) {
			r.rootFolder = null;
		}
	}

	public RootFolder getRootFolder() {
		if (rootFolder == null) {
			rootFolder = new RootFolder();
			if (pmsConfiguration.getUseCache()) {
				rootFolder.discoverChildren();
			}
		}

		return rootFolder;
	}

	public void addFolderLimit(DLNAResource res) {
		if (rootFolder != null) {
			rootFolder.setFolderLim(res);
		}
	}

	public void setRootFolder(RootFolder r) {
		rootFolder = r;
	}

	/**
	 * Associate an IP address with this renderer. The association will
	 * persist between requests, allowing the renderer to be recognized
	 * by its address in later requests.
	 *
	 * @param sa The IP address to associate.
	 * @return whether the device at this address is a renderer.
	 * @see #getRendererConfigurationBySocketAddress(InetAddress)
	 */
	public boolean associateIP(InetAddress sa) {
		if (UPNPHelper.isNonRenderer(sa)) {
			// TODO: remove it if already added unknowingly
			return false;
		}

		// FIXME: handle multiple clients with same ip properly, now newer overwrites older

		RendererConfiguration prev = addressAssociation.put(sa, this);
		if (prev != null) {
			// We've displaced a previous renderer at this address, so
			// check  if it's a ghost instance that should be deleted.
			verify(prev);
		}
		resetUpnpMode();

		if (
			(
				pmsConfiguration.isAutomaticMaximumBitrate() ||
				pmsConfiguration.isSpeedDbg()
			) &&
			!(
				sa.isLoopbackAddress() ||
				sa.isAnyLocalAddress()
			)
		) {
			SpeedStats.getInstance().getSpeedInMBits(sa, getRendererName());
		}
		return true;
	}

	public static void calculateAllSpeeds() {
		for (InetAddress sa : addressAssociation.keySet()) {
			if (sa.isLoopbackAddress() || sa.isAnyLocalAddress()) {
				continue;
			}
			RendererConfiguration r = addressAssociation.get(sa);
			if (!r.isOffline()) {
				SpeedStats.getInstance().getSpeedInMBits(sa, r.getRendererName());
			}
		}
	}

	public static RendererConfiguration getRendererConfigurationBySocketAddress(InetAddress sa) {
		RendererConfiguration r = addressAssociation.get(sa);
		if (r != null) {
			LOGGER.trace("Matched media renderer \"{}\" based on address {}", r.getRendererName(), sa.getHostAddress());
		}
		return r;
	}

	/**
	 * Tries to find a matching renderer configuration based on the given collection of
	 * request headers
	 *
	 * @param headers The headers.
	 * @param ia The request's origin address.
	 * @return The matching renderer configuration or <code>null</code>
	 */
	public static RendererConfiguration getRendererConfigurationByHeaders(Collection<Map.Entry<String, String>> headers, InetAddress ia) {
		return getRendererConfigurationByHeaders(new SortedHeaderMap(headers), ia);
	}

	public static RendererConfiguration getRendererConfigurationByHeaders(SortedHeaderMap sortedHeaders, InetAddress ia) {
		RendererConfiguration r = null;
		RendererConfiguration ref = getRendererConfigurationByHeaders(sortedHeaders);
		if (ref != null) {
			boolean isNew = !addressAssociation.containsKey(ia);
			r = resolve(ia, ref);
			if (r != null) {
				LOGGER.trace(
					"Matched {}media renderer \"{}\" based on headers {}",
					isNew ? "new " : "",
					r.getRendererName(),
					sortedHeaders
				);
			}
		}
		return r;
	}

	public static RendererConfiguration getRendererConfigurationByHeaders(SortedHeaderMap sortedHeaders) {
		if (_pmsConfiguration.isRendererForceDefault()) {
			// Force default renderer
			LOGGER.debug("Forcing renderer match to \"" + defaultConf.getRendererName() + "\"");
			return defaultConf;
		}
		for (RendererConfiguration r : enabledRendererConfs) {
			if (r.match(sortedHeaders)) {
				LOGGER.debug("Matched media renderer \"" + r.getRendererName() + "\" based on headers " + sortedHeaders);
				return r;
			}
		}
		return null;
	}

	/**
	 * Tries to find a matching renderer configuration based on the name of
	 * the renderer. Returns true if the provided name is equal to or a
	 * substring of the renderer name defined in a configuration, where case
	 * does not matter.
	 *
	 * @param name The renderer name to match.
	 * @return The matching renderer configuration or <code>null</code>
	 *
	 * @since 1.50.1
	 */
	public static RendererConfiguration getRendererConfigurationByName(String name) {
		for (RendererConfiguration conf : enabledRendererConfs) {
			if (conf.getConfName().toLowerCase().contains(name.toLowerCase())) {
				return conf;
			}
		}

		return null;
	}

	public static RendererConfiguration getRendererConfigurationByUUID(String uuid) {
		for (RendererConfiguration conf : getConnectedRenderersConfigurations()) {
			if (uuid.equals(conf.getUUID())) {
				return conf;
			}
		}

		return null;
	}

	public static RendererConfiguration getRendererConfigurationByUPNPDetails(String details) {
		for (RendererConfiguration r : enabledRendererConfs) {
			if (r.matchUPNPDetails(details)) {
				LOGGER.debug("Matched media renderer \"" + r.getRendererName() + "\" based on dlna details \"" + details + "\"");
				return r;
			}
		}
		return null;
	}

	public static RendererConfiguration resolve(InetAddress ia, RendererConfiguration ref) {
		DeviceConfiguration r = null;
		boolean recognized = ref != null;
		if (!recognized) {
			ref = getDefaultConf();
		}
		try {
			if (addressAssociation.containsKey(ia)) {
				// Already seen, finish configuration if required
				r = (DeviceConfiguration) addressAssociation.get(ia);
				boolean higher = ref != null && ref.getLoadingPriority() > r.getLoadingPriority() && recognized;
				if (!r.loaded || higher) {
					LOGGER.debug("Finishing configuration for {}", r);
					if (higher) {
						LOGGER.debug("Switching to higher priority renderer: {}", ref);
					}
					r.inherit(ref);
					// update gui
					PMS.get().updateRenderer(r);
				}
			} else if (!UPNPHelper.isNonRenderer(ia)) {
				// It's brand new
				r = new DeviceConfiguration(ref, ia);
				if (r.associateIP(ia)) {
					PMS.get().setRendererFound(r);
				}
				r.active = true;
				if (r.isUpnpPostponed()) {
					r.setUpnpMode(ALLOW);
				}
			}
		} catch (ConfigurationException e) {
			LOGGER.error("Configuration error while resolving renderer: {}", e.getMessage());
			LOGGER.trace("", e);
		}
		if (!recognized) {
			// Mark it as unloaded so actual recognition can happen later if UPnP sees it.
			LOGGER.trace("Marking renderer \"{}\" at {} as unrecognized", r, ia.getHostAddress());
			if (r != null) {
				r.loaded = false;
			}
		}
		return r;
	}

	public FormatConfiguration getFormatConfiguration() {
		return formatConfiguration;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public PmsConfiguration getPmsConfiguration() {
		return pmsConfiguration;
	}

	public File getFile() {
		return file;
	}

	public String getId() {
		return uuid != null ? uuid : getAddress().toString().substring(1);
	}

	public static String getSimpleName(RendererConfiguration r) {
		return StringUtils.substringBefore(r.getRendererName(), "(").trim();
	}

	public static String getDefaultFilename(RendererConfiguration r) {
		String id = r.getId();
		return (getSimpleName(r) + "-" + (id.startsWith("uuid:") ? id.substring(5, 11) : id)).replace(" ", "") + ".conf";
	}

	public File getUsableFile() {
		File f = getFile();
		if (f == null || f.equals(NOFILE)) {
			String name = getSimpleName(this);
			f = new File(getRenderersDir(), name.equals(getSimpleName(defaultConf)) ? getDefaultFilename(this) :  (name.replace(" ", "") + ".conf"));
		}
		return f;
	}

	public static void createNewFile(RendererConfiguration r, File file, boolean load, File ref) {
		try {
			ArrayList<String> conf = new ArrayList<>();
			String name = getSimpleName(r);
			Map<String, String> details = r.getUpnpDetails();
			List<String> headers = r.getIdentifiers();
			boolean hasRef = ref != null && ref != NOFILE;

			// Add the header and identifiers
			conf.add("#----------------------------------------------------------------------------");
			conf.add("# Auto-generated profile for " + name);
			conf.add("#" + (hasRef ? " Based on " + ref.getName() : ""));
			conf.add("# See DefaultRenderer.conf for a description of all possible configuration options.");
			conf.add("#");
			conf.add("");
			conf.add(RENDERER_NAME + " = " + name);
			if (headers != null || details != null) {
				conf.add("");
				conf.add("# ============================================================================");
				conf.add("# This renderer has sent the following string/s:");
				if (headers != null && headers.size() > 0) {
					conf.add("#");
					for (String h : headers) {
						conf.add("# " + h);
					}
				}
				if (details != null) {
					details.remove("address");
					details.remove("udn");
					conf.add("#");
					conf.add("# " + details);
				}
				conf.add("# ============================================================================");
				conf.add("");
			}
			conf.add(USER_AGENT + " = ");
			if (headers != null && headers.size() > 1) {
				conf.add(USER_AGENT_ADDITIONAL_HEADER + " = ");
				conf.add(USER_AGENT_ADDITIONAL_SEARCH + " = ");
			}
			if (details != null) {
				conf.add(UPNP_DETAILS + " = " + details.get("manufacturer") + " , " + details.get("modelName"));
			}
			conf.add("");
			// TODO: Set more properties automatically from UPNP info

			if (hasRef) {
				// Copy the reference file, skipping its header and identifiers
				Matcher skip = Pattern.compile(".*(" + RENDERER_ICON + "|" + RENDERER_NAME + "|" +
					UPNP_DETAILS + "|" + USER_AGENT + "|" + USER_AGENT_ADDITIONAL_HEADER + "|" +
					USER_AGENT_ADDITIONAL_SEARCH + ").*").matcher("");
				boolean header = true;
				for (String line : FileUtils.readLines(ref, Charsets.UTF_8)) {
					if (
						skip.reset(line).matches() ||
						(
							header &&
							(
								line.startsWith("#") ||
								StringUtils.isBlank(line)
							)
						)
					) {
						continue;
					}
					header = false;
					conf.add(line);
				}
			}

			FileUtils.writeLines(file, "utf-8", conf, "\r\n");

			if (load) {
				try {
					RendererConfiguration renderer = new RendererConfiguration(file);
					enabledRendererConfs.add(renderer);
					if (r instanceof DeviceConfiguration) {
						((DeviceConfiguration)r).inherit(renderer);
					}
				} catch (ConfigurationException ce) {
					LOGGER.debug("Error initializing renderer configuration: " + ce);
				}
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating renderer configuration file: " + ie);
		}
	}

	public boolean isFileless() {
		return fileless;
	}

	public void setFileless(boolean b) {
		fileless = b;
	}

	public int getRank() {
		return rank;
	}

	/**
	 * @see #isXbox360()
	 * @deprecated
	 */
	@Deprecated
	public boolean isXBOX() {
		return isXbox360();
	}

	/**
	 * @return whether this renderer is an Xbox 360
	 */
	public boolean isXbox360() {
		return getConfName().toUpperCase().contains("XBOX 360");
	}

	/**
	 * @return whether this renderer is an Xbox One
	 */
	public boolean isXboxOne() {
		return getConfName().toUpperCase().contains("XBOX ONE");
	}

	public boolean isXBMC() {
		return getConfName().toUpperCase().contains("KODI") || getConfName().toUpperCase().contains("XBMC");
	}

	public boolean isPS3() {
		return getConfName().toUpperCase().contains("PLAYSTATION 3") || getConfName().toUpperCase().contains("PS3");
	}

	public boolean isPS4() {
		return getConfName().toUpperCase().contains("PLAYSTATION 4");
	}

	public boolean isBRAVIA() {
		return getConfName().toUpperCase().contains("BRAVIA");
	}

	public boolean isFDSSDP() {
		return getConfName().toUpperCase().contains("FDSSDP");
	}

	public boolean isLG() {
		return getConfName().toUpperCase().contains("LG ");
	}

	// Ditlew
	public int getByteToTimeseekRewindSeconds() {
		return getInt(BYTE_TO_TIMESEEK_REWIND_SECONDS, 0);
	}

	// Ditlew
	public int getCBRVideoBitrate() {
		return getInt(CBR_VIDEO_BITRATE, 0);
	}

	// Ditlew
	public boolean isShowDVDTitleDuration() {
		return getBoolean(SHOW_DVD_TITLE_DURATION, false);
	}

	public RendererConfiguration() throws ConfigurationException {
		this(null, null);
	}

	public RendererConfiguration(String uuid) throws ConfigurationException {
		this(null, uuid);
	}

	public RendererConfiguration(int ignored) {
		// Just instantiate minimally, full initialization will happen later
		configuration = createPropertiesConfiguration();
		configurationReader = new ConfigurationReader(configuration, true); // true: log
	}

	static UnicodeUnescaper unicodeUnescaper = new UnicodeUnescaper();

	public RendererConfiguration(File f) throws ConfigurationException {
		this(f, null);
	}

	public RendererConfiguration(File f, String uuid) throws ConfigurationException {
		super(uuid);

		configuration = createPropertiesConfiguration();

		// false: don't log overrides (every renderer conf
		// overrides multiple settings)
		configurationReader = new ConfigurationReader(configuration, false);
		pmsConfiguration = _pmsConfiguration;

		player = null;
		buffer = 0;

		init(f);
	}

	static StringUtil.LaxUnicodeUnescaper laxUnicodeUnescaper = new StringUtil.LaxUnicodeUnescaper();

	public static PropertiesConfiguration createPropertiesConfiguration() {
		PropertiesConfiguration conf = new PropertiesConfiguration();
		conf.setListDelimiter((char) 0);
		// Treat backslashes in the conf as literal while also supporting double-backslash syntax, i.e.
		// ensure that typical raw regex strings (and unescaped Windows file paths) are read correctly.
		conf.setIOFactory(new PropertiesConfiguration.DefaultIOFactory() {
			@Override
			public PropertiesConfiguration.PropertiesReader createPropertiesReader(final Reader in, final char delimiter) {
				return new PropertiesConfiguration.PropertiesReader(in, delimiter) {
					@Override
					protected void parseProperty(final String line) {
						// Decode any backslashed unicode escapes, e.g. '\u005c', from the
						// ISO 8859-1 (aka Latin 1) encoded java Properties file, then
						// unescape any double-backslashes, then escape all backslashes before parsing
						super.parseProperty(laxUnicodeUnescaper.translate(line).replace("\\\\", "\\").replace("\\", "\\\\"));
					}
				};
			}
		});
		return conf;
	}

	public boolean load(File f) throws ConfigurationException {
		if (f != null && !f.equals(NOFILE) && (configuration instanceof PropertiesConfiguration)) {
			((PropertiesConfiguration) configuration).load(f);

			// Set up the header matcher
			SortedHeaderMap searchMap = new SortedHeaderMap();
			searchMap.put("User-Agent", getUserAgent());
			searchMap.put(getUserAgentAdditionalHttpHeader(), getUserAgentAdditionalHttpHeaderSearch());
			String re = searchMap.toRegex();
			sortedHeaderMatcher = StringUtils.isNotBlank(re) ? Pattern.compile(re, Pattern.CASE_INSENSITIVE).matcher("") : null;

			boolean addWatch = file != f;
			file = f;
			if (addWatch) {
				FileWatcher.add(new FileWatcher.Watch(getFile().getPath(), reloader, this));
			}
			return true;
		}
		return false;
	}

	public void init(File f) throws ConfigurationException {
		rootFolder = null;
		if (!loaded) {
			configuration.clear();
			loaded = load(f);
		}

		if (isUpnpAllowed() && uuid == null) {
			String id = getDeviceId();
			if (StringUtils.isNotBlank(id) && !id.contains(",")) {
				uuid = id;
			}
		}

		mimes = new HashMap<>();
		String mimeTypes = getString(MIME_TYPES_CHANGES, "");

		if (StringUtils.isNotBlank(mimeTypes)) {
			StringTokenizer st = new StringTokenizer(mimeTypes, "|");

			while (st.hasMoreTokens()) {
				String mime_change = st.nextToken().trim();
				int equals = mime_change.indexOf('=');

				if (equals > -1) {
					String old = mime_change.substring(0, equals).trim().toLowerCase();
					String nw = mime_change.substring(equals + 1).trim().toLowerCase();
					mimes.put(old, nw);
				}
			}
		}

		String s = getString(TEXTWRAP, "").toLowerCase();
		lineWidth = getIntAt(s, "width:", 0);
		if (lineWidth > 0) {
			lineHeight = getIntAt(s, "height:", 0);
			indent = getIntAt(s, "indent:", 0);
			int whitespace = getIntAt(s, "whitespace:", 9);
			int dotCount = getIntAt(s, "dots:", 0);
			inset = StringUtil.fillString(whitespace, indent);
			dots = StringUtil.fillString(".", dotCount);
		}

		charMap = new HashMap<>();
		String ch = getString(CHARMAP, null);
		if (StringUtils.isNotBlank(ch)) {
			StringTokenizer st = new StringTokenizer(ch, " ");
			String org = "";

			while (st.hasMoreTokens()) {
				String tok = st.nextToken().trim();
				if (StringUtils.isBlank(tok)) {
					continue;
				}
				tok = tok.replaceAll("###0", " ").replaceAll("###n", "\n").replaceAll("###r", "\r");
				if (StringUtils.isBlank(org)) {
					org = tok;
				} else {
					charMap.put(org, tok);
					org = "";
				}
			}
		}

		DLNAPN = new HashMap<>();
		String DLNAPNchanges = getString(DLNA_PN_CHANGES, "");

		if (StringUtils.isNotBlank(DLNAPNchanges)) {
			LOGGER.trace("Config DLNAPNchanges: " + DLNAPNchanges);
			StringTokenizer st = new StringTokenizer(DLNAPNchanges, "|");
			while (st.hasMoreTokens()) {
				String DLNAPN_change = st.nextToken().trim();
				int equals = DLNAPN_change.indexOf('=');
				if (equals > -1) {
					String old = DLNAPN_change.substring(0, equals).trim().toUpperCase();
					String nw = DLNAPN_change.substring(equals + 1).trim().toUpperCase();
					DLNAPN.put(old, nw);
				}
			}
		}

		if (f == null) {
			// The default renderer supports everything!
			configuration.addProperty(MEDIAPARSERV2, true);
			configuration.addProperty(MEDIAPARSERV2_THUMB, true);
			configuration.addProperty(SUPPORTED, "f:.+");
		}

		if (isUseMediaInfo()) {
			formatConfiguration = new FormatConfiguration(configuration.getList(SUPPORTED));
		}
	}

	public void reset() {
		File f = getFile();
		try {
			LOGGER.info("Reloading renderer configuration: {}", f);
			loaded = false;
			init(f);
			// update gui
			for (RendererConfiguration d : DeviceConfiguration.getInheritors(this)) {
				PMS.get().updateRenderer(d);
			}
		} catch (Exception e) {
			LOGGER.debug("Error reloading renderer configuration {}: {}", f, e);
			e.printStackTrace();
		}
	}

	public String getDLNAPN(String old) {
		if (DLNAPN.containsKey(old)) {
			return DLNAPN.get(old);
		}

		return old;
	}

	public boolean supportsFormat(Format f) {
		switch (f.getType()) {
			case Format.VIDEO:
				return isVideoSupported();
			case Format.AUDIO:
				return isAudioSupported();
			case Format.IMAGE:
				return isImageSupported();
			default:
				break;
		}

		return false;
	}

	public boolean isVideoSupported() {
		return getBoolean(VIDEO, true);
	}

	public boolean isAudioSupported() {
		return getBoolean(AUDIO, true);
	}

	public boolean isImageSupported() {
		return getBoolean(IMAGE, true);
	}

	public boolean isTranscodeToWMV() {
		return getVideoTranscode().equals(WMV);
	}

	public boolean isTranscodeToMPEGPSMPEG2AC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGPSMPEG2AC3) || videoTranscode.equals(DEPRECATED_MPEGAC3) || videoTranscode.equals(DEPRECATED_MPEGPSAC3);
	}

	public boolean isTranscodeToMPEGTSMPEG2AC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGTSMPEG2AC3) || videoTranscode.equals(DEPRECATED_MPEGTSAC3);
	}

	public boolean isTranscodeToMPEGTSH264AC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGTSH264AC3) || videoTranscode.equals(DEPRECATED_H264TSAC3);
	}

	public boolean isTranscodeToMPEGTSH264AAC() {
		return getVideoTranscode().equals(MPEGTSH264AAC);
	}

	public boolean isTranscodeToMPEGTSH265AAC() {
		return getVideoTranscode().equals(MPEGTSH265AAC);
	}

	public boolean isTranscodeToMPEGTSH265AC3() {
		return getVideoTranscode().equals(MPEGTSH265AC3);
	}

	/**
	 * @return whether to use the AC-3 audio codec for transcoded video
	 */
	public boolean isTranscodeToAC3() {
		return isTranscodeToMPEGPSMPEG2AC3() || isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGTSH264AC3() || isTranscodeToMPEGTSH265AC3();
	}

	/**
	 * @return whether to use the AAC audio codec for transcoded video
	 */
	public boolean isTranscodeToAAC() {
		return isTranscodeToMPEGTSH264AAC() || isTranscodeToMPEGTSH265AAC();
	}

	/**
	 * @return whether to use the H.264 video codec for transcoded video
	 */
	public boolean isTranscodeToH264() {
		return isTranscodeToMPEGTSH264AAC() || isTranscodeToMPEGTSH264AC3();
	}

	/**
	 * @return whether to use the H.265 video codec for transcoded video
	 */
	public boolean isTranscodeToH265() {
		return isTranscodeToMPEGTSH265AAC() || isTranscodeToMPEGTSH265AC3();
	}

	/**
	 * @return whether to use the MPEG-TS container for transcoded video
	 */
	public boolean isTranscodeToMPEGTS() {
		return isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGTSH264AC3() || isTranscodeToMPEGTSH264AAC() || isTranscodeToMPEGTSH265AC3() || isTranscodeToMPEGTSH265AAC();
	}

	/**
	 * @return whether to use the MPEG-2 video codec for transcoded video
	 */
	public boolean isTranscodeToMPEG2() {
		return isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGPSMPEG2AC3();
	}

	public boolean isTranscodeToMP3() {
		return getAudioTranscode().equals(MP3);
	}

	public boolean isTranscodeToLPCM() {
		return getAudioTranscode().equals(LPCM);
	}

	public boolean isTranscodeToWAV() {
		return getAudioTranscode().equals(WAV);
	}

	public boolean isTranscodeAudioTo441() {
		return getBoolean(TRANSCODE_AUDIO_441KHZ, false);
	}

	/**
	 * @return whether to transcode H.264 video if it exceeds level 4.1
	 */
	public boolean isH264Level41Limited() {
		return getBoolean(H264_L41_LIMITED, true);
	}

	public boolean isTranscodeFastStart() {
		return getBoolean(TRANSCODE_FAST_START, false);
	}

	public boolean isDLNALocalizationRequired() {
		return getBoolean(DLNA_LOCALIZATION_REQUIRED, false);
	}

	public boolean isDisableMencoderNoskip() {
		return getBoolean(DISABLE_MENCODER_NOSKIP, false);
	}

	/**
	 * Determine the mime type specific for this renderer, given a generic mime
	 * type. This translation takes into account all configured "Supported"
	 * lines and mime type aliases for this renderer.
	 *
	 * @param mimeType
	 *            The mime type to look up. Special values are
	 *            <code>HTTPResource.VIDEO_TRANSCODE</code> and
	 *            <code>HTTPResource.AUDIO_TRANSCODE</code>, which will be
	 *            translated to the mime type of the transcoding profile
	 *            configured for this renderer.
	 * @return The mime type.
	 */
	public String getMimeType(String mimeType, DLNAMediaInfo media) {
		if (mimeType == null) {
			return null;
		}

		String matchedMimeType = null;

		if (isUseMediaInfo()) {
			// Use the supported information in the configuration to determine the transcoding mime type.
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToMPEGTSH264AC3()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AC3);
				} else if (isTranscodeToMPEGTSH264AAC()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AAC_LC);
				} else if (isTranscodeToMPEGTSH265AC3()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H265, FormatConfiguration.AC3);
				} else if (isTranscodeToMPEGTSH265AAC()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H265, FormatConfiguration.AAC_LC);
				} else if (isTranscodeToMPEGTSMPEG2AC3()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.MPEG2, FormatConfiguration.AC3);
				} else if (isTranscodeToWMV()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.WMV, FormatConfiguration.WMV, FormatConfiguration.WMA);
				} else {
					// Default video transcoding mime type
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGPS, FormatConfiguration.MPEG2, FormatConfiguration.AC3);
				}
			} else if (HTTPResource.AUDIO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToWAV()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.WAV, null, null);
				} else if (isTranscodeToMP3()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MP3, null, null);
				} else {
					// Default audio transcoding mime type
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.LPCM, null, null);

					if (matchedMimeType != null) {
						if (pmsConfiguration.isAudioResample()) {
							if (isTranscodeAudioTo441()) {
								matchedMimeType += ";rate=44100;channels=2";
							} else {
								matchedMimeType += ";rate=48000;channels=2";
							}
						} else if (media != null && media.getFirstAudioTrack() != null) {
							AudioProperties audio = media.getFirstAudioTrack().getAudioProperties();
							if (audio.getSampleFrequency() > 0) {
								matchedMimeType += ";rate=" + Integer.toString(audio.getSampleFrequency());
							}
							if (audio.getNumberOfChannels() > 0) {
								matchedMimeType += ";channels=" + Integer.toString(audio.getNumberOfChannels());
							}
						}
					}
				}
			}
		}

		if (matchedMimeType == null) {
			// No match found, try without media parser v2
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToWMV()) {
					matchedMimeType = HTTPResource.WMV_TYPEMIME;
				} else {
					// Default video transcoding mime type
					matchedMimeType = HTTPResource.MPEG_TYPEMIME;
				}
			} else if (HTTPResource.AUDIO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToWAV()) {
					matchedMimeType = HTTPResource.AUDIO_WAV_TYPEMIME;
				} else if (isTranscodeToMP3()) {
					matchedMimeType = HTTPResource.AUDIO_MP3_TYPEMIME;
				} else {
					// Default audio transcoding mime type
					matchedMimeType = HTTPResource.AUDIO_LPCM_TYPEMIME;

					if (pmsConfiguration.isAudioResample()) {
						if (isTranscodeAudioTo441()) {
							matchedMimeType += ";rate=44100;channels=2";
						} else {
							matchedMimeType += ";rate=48000;channels=2";
						}
					} else if (media != null) {
						AudioProperties audio = media.getFirstAudioTrack().getAudioProperties();
						if (audio.getSampleFrequency() > 0) {
							matchedMimeType += ";rate=" + Integer.toString(audio.getSampleFrequency());
						}
						if (audio.getNumberOfChannels() > 0) {
							matchedMimeType += ";channels=" + Integer.toString(audio.getNumberOfChannels());
						}
					}
				}
			}
		}

		if (matchedMimeType == null) {
			matchedMimeType = mimeType;
		}

		// Apply renderer specific mime type aliases
		if (mimes.containsKey(matchedMimeType)) {
			return mimes.get(matchedMimeType);
		}

		return matchedMimeType;
	}

	public boolean matchUPNPDetails(String details) {
		String upnpDetails = getUpnpDetailsString();
		Pattern pattern;

		if (StringUtils.isNotBlank(upnpDetails)) {
			String p = StringUtils.join(upnpDetails.split(" , "), ".*");
			pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
			return pattern.matcher(details.replace("\n", " ")).find();
		}
		return false;
	}

	/**
	 * Returns the pattern to match the User-Agent header to as defined in the
	 * renderer configuration. Default value is "".
	 *
	 * @return The User-Agent search pattern.
	 */
	public String getUserAgent() {
		return getString(USER_AGENT, "");
	}

	/**
	 * Returns the unique UPnP details of this renderer as defined in the
	 * renderer configuration. Default value is "".
	 *
	 * @return The detail string.
	 */
	public String getUpnpDetailsString() {
		return getString(UPNP_DETAILS, "");
	}

	/**
	 * Returns the UPnP details of this renderer as broadcast by itself, if known.
	 * Default value is null.
	 *
	 * @return The detail map.
	 */
	public Map<String, String> getUpnpDetails() {
		return UPNPHelper.getDeviceDetails(UPNPHelper.getDevice(uuid));
	}

	public boolean isUpnp() {
		return uuid != null && UPNPHelper.isUpnpDevice(uuid);
	}

	public boolean isControllable() {
		return controls != 0;
	}

	public Map<String, String> getDetails() {
		if (details == null) {
			if (isUpnp()) {
				details = UPNPHelper.getDeviceDetails(UPNPHelper.getDevice(uuid));
			} else {
				details = new LinkedHashMap<String, String>() {
					private static final long serialVersionUID = -3998102753945339020L;

					{
						put(Messages.getString("RendererPanel.10"), getRendererName());
						if (getAddress() != null) {
							put(Messages.getString("RendererPanel.11"), getAddress().getHostAddress().toString());
						}
					}
				};
			}
		}
		return details;
	}

	/**
	 * Returns the current UPnP state variables of this renderer, if known. Default value is null.
	 *
	 * @return The data.
	 */
	public Map<String, String> getUPNPData() {
		return UPNPHelper.getData(uuid, instanceID);
	}

	/**
	 * Returns the UPnP services of this renderer.
	 * Default value is null.
	 *
	 * @return The list of service names.
	 */
	public List<String> getUpnpServices() {
		return isUpnp() ? UPNPHelper.getServiceNames(UPNPHelper.getDevice(uuid)) : null;
	}

	/**
	 * Returns the uuid of this renderer, if known. Default value is null.
	 *
	 * @return The uuid.
	 */
	public String getUUID() {
		return uuid;
	}

	/**
	 * Sets the uuid of this renderer.
	 *
	 * @param uuid The uuid.
	 */
	public void setUUID(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Returns the UPnP instance id of this renderer, if known. Default value is null.
	 *
	 * @return The instance id.
	 */
	public String getInstanceID() {
		return instanceID;
	}

	/**
	 * Sets the UPnP instance id of this renderer.
	 *
	 * @param id The instance id.
	 */
	public void setInstanceID(String id) {
		instanceID = id;
	}

	/**
	 * Returns whether this renderer is known to be offline.
	 *
	 * @return Whether offline.
	 */
	public boolean isOffline() {
		return !active;
	}

	/**
	 * Returns whether this renderer is currently connected via UPnP.
	 *
	 * @return Whether connected.
	 */
	public boolean isUpnpConnected() {
		return uuid != null ? UPNPHelper.isActive(uuid, instanceID) : false;
	}

	/**
	 * Returns whether this renderer has an associated address.
	 *
	 * @return Has address.
	 */
	public boolean hasAssociatedAddress() {
		return addressAssociation.values().contains(this);
	}

	/**
	 * Returns this renderer's associated address.
	 *
	 * @return The address.
	 */
	public InetAddress getAddress() {
		// If we have a uuid look up the UPnP device address, which is always
		// correct even if another device has overwritten our association
		if (uuid != null) {
			InetAddress address = UPNPHelper.getAddress(uuid);
			if (address != null) {
				return address;
			}
		}
		// Otherwise check the address association
		for (InetAddress sa : addressAssociation.keySet()) {
			if (addressAssociation.get(sa) == this) {
				return sa;
			}
		}
		return null;
	}

	/**
	 * Returns whether this renderer provides UPnP control services.
	 *
	 * @return Whether controllable.
	 */
	public boolean isUpnpControllable() {
		return UPNPHelper.isUpnpControllable(uuid);
	}

	/**
	 * Returns a UPnP player for this renderer if UPnP control is supported.
	 *
	 * @return a player or null.
	 */
	public BasicPlayer getPlayer() {
		if (player == null) {
			player = isUpnpControllable() ? new UPNPHelper.Player((DeviceConfiguration) this) :
				new PlaybackTimer((DeviceConfiguration) this);
		}
		return player;
	}

	/**
	 * Sets the UPnP player.
	 *
	 * @param player
	 */
	public void setPlayer(UPNPHelper.Player player) {
		this.player = player;
	}

	@Override
	public void setActive(boolean b) {
		super.setActive(b);
		if (gui != null) {
			gui.icon.setGrey(!active);
		}
	}

	public void delete(int delay) {
		delete(this, delay);
	}

	public static void delete(final RendererConfiguration r, int delay) {
		r.setActive(false);
		// Using javax.swing.Timer because of gui (this works in headless mode too).
		javax.swing.Timer t = new javax.swing.Timer(delay, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				// Make sure we haven't been reactivated while asleep
				if (! r.isActive()) {
					LOGGER.debug("Deleting renderer " + r);
					if (r.gui != null) {
						r.gui.delete();
					}
					PMS.get().getFoundRenderers().remove(r);
					UPNPHelper.getInstance().removeRenderer(r);
					InetAddress ia = r.getAddress();
					if (addressAssociation.get(ia) == r) {
						addressAssociation.remove(ia);
					}
					// TODO: actually delete rootfolder, etc.
				}
			}
		});
		t.setRepeats(false);
		t.start();
	}

	public void setGuiComponents(StatusTab.RendererItem item) {
		gui = item;
	}

	public StatusTab.RendererItem getGuiComponents() {
		return gui;
	}

	/**
	 * RendererName: Determines the name that is displayed in the PMS user
	 * interface when this renderer connects. Default value is "Unknown
	 * renderer".
	 *
	 * @return The renderer name.
	 */
	public String getRendererName() {
		return (details != null && details.containsKey("friendlyName")) ? details.get("friendlyName") :
			isUpnp() ? UPNPHelper.getFriendlyName(uuid) :
			getConfName();
	}

	public String getConfName() {
		return getString(RENDERER_NAME, Messages.getString("PMS.17"));
	}

	/**
	 * Returns the icon to use for displaying this renderer in PMS as defined
	 * in the renderer configurations. Default value is UNKNOWN_ICON.
	 *
	 * @return The renderer icon.
	 */
	public String getRendererIcon() {
		String icon = getString(RENDERER_ICON, UNKNOWN_ICON);
		String deviceIcon = null;
		if (icon.equals(UNKNOWN_ICON)) {
			deviceIcon = UPNPHelper.getDeviceIcon(this, 140);
		}
		return deviceIcon == null ? icon : deviceIcon;
	}

	/**
	 * Returns the the name of an additional HTTP header whose value should
	 * be matched with the additional header search pattern. The header name
	 * must be an exact match (read: the header has to start with the exact
	 * same case sensitive string). The default value is "".
	 *
	 * @return The additional HTTP header name.
	 */
	public String getUserAgentAdditionalHttpHeader() {
		return getString(USER_AGENT_ADDITIONAL_HEADER, "");
	}

	/**
	 * Returns the pattern to match additional headers to as defined in the
	 * renderer configuration. Default value is "".
	 *
	 * @return The User-Agent search pattern.
	 */
	public String getUserAgentAdditionalHttpHeaderSearch() {
		return getString(USER_AGENT_ADDITIONAL_SEARCH, "");
	}

	/**
	 * May append a custom file extension to the file path.
	 * Returns the original path if the renderer didn't define an extension.
	 *
	 * @param file the original file path
	 * @return
	 */
	public String getUseSameExtension(String file) {
		String extension = getString(USE_SAME_EXTENSION, "");
		if (StringUtils.isNotEmpty(extension)) {
			file += "." + extension;
		}

		return file;
	}

	/**
	 * Returns true if SeekByTime is set to "true" or "exclusive", false otherwise.
	 * Default value is false.
	 *
	 * @return true if the renderer supports seek-by-time, false otherwise.
	 */
	public boolean isSeekByTime() {
		return isSeekByTimeExclusive() || getString(SEEK_BY_TIME, "false").equalsIgnoreCase("true");
	}

	/**
	 * Returns true if SeekByTime is set to "exclusive", false otherwise.
	 * Default value is false.
	 *
	 * @return true if the renderer supports seek-by-time exclusively
	 * (i.e. not in conjunction with seek-by-byte), false otherwise.
	 */
	public boolean isSeekByTimeExclusive() {
		return getString(SEEK_BY_TIME, "false").equalsIgnoreCase("exclusive");
	}

	public boolean isMuxH264MpegTS() {
		boolean muxCompatible = getBoolean(MUX_H264_WITH_MPEGTS, true);
		if (isUseMediaInfo()) {
			muxCompatible = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H264, null) != null;
		}

		if (Platform.isMac() && System.getProperty("os.version") != null && System.getProperty("os.version").contains("10.4.")) {
			muxCompatible = false; // no tsMuxeR for 10.4 (yet?)
		}

		return muxCompatible;
	}

	public boolean isDTSPlayable() {
		return isMuxDTSToMpeg() || (isWrapDTSIntoPCM() && isMuxLPCMToMpeg());
	}

	public boolean isMuxDTSToMpeg() {
		if (isUseMediaInfo()) {
			return getFormatConfiguration().isDTSSupported();
		}

		return getBoolean(MUX_DTS_TO_MPEG, false);
	}

	public boolean isWrapDTSIntoPCM() {
		return getBoolean(WRAP_DTS_INTO_PCM, false);
	}

	public boolean isWrapEncodedAudioIntoPCM() {
		return getBoolean(WRAP_ENCODED_AUDIO_INTO_PCM, false);
	}

	public boolean isLPCMPlayable() {
		return isMuxLPCMToMpeg();
	}

	public boolean isMuxLPCMToMpeg() {
		if (isUseMediaInfo()) {
			return getFormatConfiguration().isLPCMSupported();
		}

		return getBoolean(MUX_LPCM_TO_MPEG, true);
	}

	public boolean isMuxNonMod4Resolution() {
		return getBoolean(MUX_NON_MOD4_RESOLUTION, false);
	}

	public boolean isMpeg2Supported() {
		if (isUseMediaInfo()) {
			return getFormatConfiguration().isMpeg2Supported();
		}

		return isPS3();
	}

	/**
	 * Returns whether or not to include metadata when pushing uris.
	 * This is meant as a stopgap workaround for any renderer that
	 * chokes on our metadata.
	 *
	 * @return whether to include metadata.
	 */
	public boolean isPushMetadata() {
		return getBoolean(PUSH_METADATA, true);
	}

	/**
	 * Returns the codec to use for video transcoding for this renderer as
	 * defined in the renderer configuration. Default value is "MPEGPSMPEG2AC3".
	 *
	 * @return The codec name.
	 */
	public String getVideoTranscode() {
		return getString(TRANSCODE_VIDEO, MPEGPSMPEG2AC3);
	}

	/**
	 * Returns the codec to use for audio transcoding for this renderer as
	 * defined in the renderer configuration. Default value is "LPCM".
	 *
	 * @return The codec name.
	 */
	public String getAudioTranscode() {
		return getString(TRANSCODE_AUDIO, LPCM);
	}

	/**
	 * Returns whether or not to use the default DVD buffer size for this
	 * renderer as defined in the renderer configuration. Default is false.
	 *
	 * @return True if the default size should be used.
	 */
	public boolean isDefaultVBVSize() {
		return getBoolean(DEFAULT_VBV_BUFSIZE, false);
	}

	/**
	 * Returns the maximum bitrate (in megabits-per-second) supported by the
	 * media renderer as defined in the renderer configuration. The default
	 * value is "0" (unlimited).
	 *
	 * @return The bitrate.
	 */
	// TODO this should return an integer and the units should be bits-per-second
	public String getMaxVideoBitrate() {
		if (PMS.getConfiguration().isAutomaticMaximumBitrate()) {
			try {
				return calculatedSpeed();
			} catch (InterruptedException e) {
				return "0";
			} catch (ExecutionException e) {
				LOGGER.debug("Automatic maximum bitrate calculation failed with: {}", e.getCause().getMessage());
				LOGGER.trace("", e.getCause());
			}
		}
		return getString(MAX_VIDEO_BITRATE, "0");
	}

	/**
	 * This was originally added for the PS3 after it was observed to need
	 * a video whose maximum bitrate was under half of the network maximum.
	 *
	 * @return whether to set the maximum bitrate to half of the network max
	 */
	public boolean isHalveBitrate() {
		return getBoolean(HALVE_BITRATE, false);
	}

	/**
	 * Returns the maximum bitrate (in bits-per-second) as defined by
	 * whichever is lower out of the renderer setting or user setting.
	 *
	 * @return The maximum bitrate in bits-per-second.
	 */
	public int getMaxBandwidth() {
		if (maximumBitrateTotal > 0) {
			return maximumBitrateTotal;
		}

		int defaultMaxBitrates[] = getVideoBitrateConfig(PMS.getConfiguration().getMaximumBitrate());
		int rendererMaxBitrates[] = new int[2];

		String maxVideoBitrate = getMaxVideoBitrate();
		if (StringUtils.isNotEmpty(maxVideoBitrate)) {
			rendererMaxBitrates = getVideoBitrateConfig(maxVideoBitrate);
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			LOGGER.trace(
				"Using video bitrate limit from {} configuration ({} Mb/s) because " +
				"it is lower than the general configuration bitrate limit ({} Mb/s)",
				getRendererName(),
				rendererMaxBitrates[0],
				defaultMaxBitrates[0]
			);
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (isHalveBitrate()) {
			defaultMaxBitrates[0] /= 2;
		}

		maximumBitrateTotal = defaultMaxBitrates[0] * 1000000;
		return maximumBitrateTotal;
	}

	@Deprecated
	public String getCustomMencoderQualitySettings() {
		return getCustomMEncoderMPEG2Options();
	}

	/**
	 * Returns the override settings for MEncoder quality settings as
	 * defined in the renderer configuration. The default value is "".
	 *
	 * @return The MEncoder quality settings.
	 */
	public String getCustomMEncoderMPEG2Options() {
		return getString(CUSTOM_MENCODER_MPEG2_OPTIONS, "");
	}

	/**
	 * Converts the getCustomMencoderQualitySettings() from MEncoder's format to FFmpeg's.
	 *
	 * @return The FFmpeg quality settings.
	 */
	public String getCustomFFmpegMPEG2Options() {
		String mpegSettings = getCustomMEncoderMPEG2Options();
		if (StringUtils.isBlank(mpegSettings)) {
			return "";
		}

		return convertMencoderSettingToFFmpegFormat(mpegSettings);
	}

	/**
	 * Converts the MEncoder's quality settings format to FFmpeg's.
	 *
	 * @return The FFmpeg format.
	 */
	public String convertMencoderSettingToFFmpegFormat(String mpegSettings) {
		String mpegSettingsArray[] = mpegSettings.split(":");
		String pairArray[];
		StringBuilder returnString = new StringBuilder();
		for (String pair : mpegSettingsArray) {
			pairArray = pair.split("=");
			switch (pairArray[0]) {
				case "keyint":
					returnString.append("-g ").append(pairArray[1]).append(' ');
					break;
				case "vqscale":
					returnString.append("-q:v ").append(pairArray[1]).append(' ');
					break;
				case "vqmin":
					returnString.append("-qmin ").append(pairArray[1]).append(' ');
					break;
				case "vqmax":
					returnString.append("-qmax ").append(pairArray[1]).append(' ');
					break;
				default:
					break;
			}
		}

		return returnString.toString();
	}

	/**
	 * Returns the override settings for MEncoder custom options in PMS as
	 * defined in the renderer configuration. The default value is "".
	 *
	 * @return The MEncoder custom options.
	 */
	public String getCustomMencoderOptions() {
		return getString(CUSTOM_MENCODER_OPTIONS, "");
	}

	/**
	 * Returns the maximum video width supported by the renderer as defined in
	 * the renderer configuration. 0 means unlimited.
	 *
	 * @see #isMaximumResolutionSpecified()
	 *
	 * @return The maximum video width.
	 */
	public int getMaxVideoWidth() {
		return getInt(MAX_VIDEO_WIDTH, 1920);
	}

	/**
	 * Returns the maximum video height supported by the renderer as defined
	 * in the renderer configuration. 0 means unlimited.
	 *
	 * @see #isMaximumResolutionSpecified()
	 *
	 * @return The maximum video height.
	 */
	public int getMaxVideoHeight() {
		return getInt(MAX_VIDEO_HEIGHT, 1080);
	}

	/**
	 * @Deprecated use isMaximumResolutionSpecified() instead
	 */
	@Deprecated
	public boolean isVideoRescale() {
		return getMaxVideoWidth() > 0 && getMaxVideoHeight() > 0;
	}

	/**
	 * Returns <code>true</code> if the renderer has a maximum supported width
	 * and height, <code>false</code> otherwise.
	 *
	 * @return whether the renderer has specified a maximum width and height
	 */
	public boolean isMaximumResolutionSpecified() {
		return getMaxVideoWidth() > 0 && getMaxVideoHeight() > 0;
	}

	/**
	 * Whether the resolution is compatible with the renderer.
	 *
	 * @param width the media width
	 * @param height the media height
	 *
	 * @return whether the resolution is compatible with the renderer
	 */
	public boolean isResolutionCompatibleWithRenderer(int width, int height) {
		// Check if the resolution is too high
		if (
			isMaximumResolutionSpecified() &&
			(
				width > getMaxVideoWidth() ||
				(
					height > getMaxVideoHeight() &&
					!(
						getMaxVideoHeight() == 1080 &&
						height == 1088
					)
				)
			)
		) {
			return false;
		}

		// Check if the resolution is too low
		if (!isRescaleByRenderer() && getMaxVideoWidth() < 720) {
			return false;
		}

		return true;
	}

	public boolean isDLNAOrgPNUsed() {
		return getBoolean(DLNA_ORGPN_USE, true);
	}

	public boolean isAccurateDLNAOrgPN() {
		return getBoolean(ACCURATE_DLNA_ORGPN, false);
	}

	/**
	 * Returns the comma separated list of file extensions that are forced to
	 * be transcoded and never streamed, as defined in the renderer
	 * configuration. Default value is "".
	 *
	 * @return The file extensions.
	 */
	public String getTranscodedExtensions() {
		return getString(TRANSCODE_EXT, "");
	}

	/**
	 * Returns the comma separated list of file extensions that are forced to
	 * be streamed and never transcoded, as defined in the renderer
	 * configuration. Default value is "".
	 *
	 * @return The file extensions.
	 */
	public String getStreamedExtensions() {
		return getString(STREAM_EXT, "");
	}

	/**
	 * Returns the size to report back to the renderer when transcoding media
	 * as defined in the renderer configuration. Default value is 0.
	 *
	 * @return The size to report.
	 */
	public long getTranscodedSize() {
		return getLong(TRANSCODED_SIZE, 0);
	}

	/**
	 * Some devices (e.g. Samsung) recognize a custom HTTP header for retrieving
	 * the contents of a subtitles file. This method will return the name of that
	 * custom HTTP header, or "" if no such header exists. The supported external
	 * subtitles must be set by {@link #SupportedExternalSubtitlesFormats()}.
	 *
	 * Default value is "".
	 *
	 * @return The name of the custom HTTP header.
	 */
	public String getSubtitleHttpHeader() {
		return getString(SUBTITLE_HTTP_HEADER, "");
	}

	@Override
	public String toString() {
		return getRendererName();
	}

	@Deprecated
	public boolean isMediaParserV2() {
		return isUseMediaInfo();
	}

	/**
	 * @return whether to use MediaInfo
	 */
	public boolean isUseMediaInfo() {
		return getBoolean(MEDIAPARSERV2, false) && LibMediaInfoParser.isValid();
	}

	@Deprecated
	public boolean isMediaParserV2ThumbnailGeneration() {
		return isMediaInfoThumbnailGeneration();
	}

	public boolean isMediaInfoThumbnailGeneration() {
		return getBoolean(MEDIAPARSERV2_THUMB, false) && LibMediaInfoParser.isValid();
	}

	public boolean isShowAudioMetadata() {
		return getBoolean(SHOW_AUDIO_METADATA, true);
	}

	public boolean isShowSubMetadata() {
		return getBoolean(SHOW_SUB_METADATA, true);
	}

	/**
	 * Whether to send the last modified date metadata for files and
	 * folders, which can take up screen space on some renderers.
	 *
	 * @return whether to send the metadata
	 */
	public boolean isSendDateMetadata() {
		return getBoolean(SEND_DATE_METADATA, true);
	}

	/**
	 * Whether to send folder thumbnails.
	 *
	 * @return whether to send folder thumbnails
	 */
	public boolean isSendFolderThumbnails() {
		return getBoolean(SEND_FOLDER_THUMBNAILS, true);
	}

	public boolean isDLNATreeHack() {
		return getBoolean(DLNA_TREE_HACK, false) && LibMediaInfoParser.isValid();
	}

	/**
	 * Returns whether or not to omit sending a content length header when the
	 * length is unknown, as defined in the renderer configuration. Default
	 * value is false.
	 * <p>
	 * Some renderers are particular about the "Content-Length" headers in
	 * requests (e.g. Sony Blu-ray Disc players). By default, UMS will send a
	 * "Content-Length" that refers to the total media size, even if the exact
	 * length is unknown.
	 *
	 * @return True if sending the content length header should be omitted.
	 */
	public boolean isChunkedTransfer() {
		return getBoolean(CHUNKED_TRANSFER, false);
	}

	/**
	 * Returns whether or not the renderer can handle the given format
	 * natively, based on its configuration in the renderer.conf. If it can
	 * handle a format natively, content can be streamed to the renderer. If
	 * not, content should be transcoded before sending it to the renderer.
	 *
	 * @param mediaInfo The {@link DLNAMediaInfo} information parsed from the
	 * 				media file.
	 * @param format The {@link Format} to test compatibility for.
	 * @param configuration The {@link PmsConfiguration} to use while evaluating compatibility
	 * @return True if the renderer natively supports the format, false
	 * 				otherwise.
	 */
	public boolean isCompatible(DLNAMediaInfo mediaInfo, Format format, PmsConfiguration configuration) {

		if (configuration == null) {
			configuration = PMS.getConfiguration(this);
		}

		if (
			configuration != null &&
			(configuration.isDisableTranscoding() ||
			(format != null &&
			format.skip(configuration.getDisableTranscodeForExtensions())))
		) {
			return true;
		}
		// Handle images differently because of automatic image transcoding
		if (format != null && format.isImage()) {
			if (
				format.getIdentifier() == Identifier.RAW ||
				mediaInfo != null && mediaInfo.getImageInfo() != null &&
				mediaInfo.getImageInfo().getFormat() != null &&
				mediaInfo.getImageInfo().getFormat().isRaw()
			) {
				LOGGER.trace(
					"RAW ({}) images are not supported for streaming",
					mediaInfo != null && mediaInfo.getImageInfo() != null && mediaInfo.getImageInfo().getFormat() != null ?
					mediaInfo.getImageInfo().getFormat() :
					format
				);
				return false;
			}
			if (mediaInfo != null && mediaInfo.getImageInfo() != null && mediaInfo.getImageInfo().isImageIOSupported()) {
				LOGGER.trace(
					"Format \"{}\" will be subject to on-demand automatic transcoding with ImageIO",
					mediaInfo.getImageInfo().getFormat() != null ?
					mediaInfo.getImageInfo().getFormat() :
					format
				);
				return true;
			}
			LOGGER.trace("Format \"{}\" is not supported by ImageIO and will depend on a compatible transcoding engine", format);
			return false;
		}

		// Use the configured "Supported" lines in the renderer.conf
		// to see if any of them match the MediaInfo library
		if (isUseMediaInfo() && mediaInfo != null && getFormatConfiguration().match(mediaInfo) != null) {
			return true;
		}

		return format != null ? format.skip(getStreamedExtensions()) : false;
	}

	/**
	 * Returns whether or not the renderer can handle the given format
	 * natively, based on its configuration in the renderer.conf. If it can
	 * handle a format natively, content can be streamed to the renderer. If
	 * not, content should be transcoded before sending it to the renderer.
	 *
	 * @param mediainfo The {@link DLNAMediaInfo} information parsed from the
	 * 				media file.
	 * @param format The {@link Format} to test compatibility for.
	 * @return True if the renderer natively supports the format, false
	 * 				otherwise.
	 */
	public boolean isCompatible(DLNAMediaInfo mediainfo, Format format) {
		return isCompatible(mediainfo, format, null);
	}

	public int getAutoPlayTmo() {
		return getInt(AUTO_PLAY_TMO, 5000);
	}

	public String getCustomFFmpegOptions() {
		return getString(CUSTOM_FFMPEG_OPTIONS, "");
	}

	public boolean isNoDynPlsFolder() {
		return false;
	}

	/**
	 * If this is true, we will always output video at 16/9 aspect ratio to
	 * the renderer, meaning that all videos with different aspect ratios
	 * will have black bars added to the edges to make them 16/9.
	 *
	 * This addresses a bug in some renderers (like Panasonic TVs) where
	 * they stretch videos that are not 16/9.
	 *
	 * @return
	 */
	public boolean isKeepAspectRatio() {
		return getBoolean(KEEP_ASPECT_RATIO, false);
	}

	/**
	 * If this is true, we will always output transcoded video at 16/9
	 * aspect ratio to the renderer, meaning that all transcoded videos with
	 * different aspect ratios will have black bars added to the edges to
	 * make them 16/9.
	 *
	 * This addresses a bug in some renderers (like Panasonic TVs) where
	 * they stretch transcoded videos that are not 16/9.
	 *
	 * @return
	 */
	public boolean isKeepAspectRatioTranscoding() {
		return getBoolean(KEEP_ASPECT_RATIO_TRANSCODING, false);
	}

	/**
	 * If this is false, FFmpeg will upscale videos with resolutions lower
	 * than SD (720 pixels wide) to the maximum resolution your renderer
	 * supports.
	 *
	 * Changing it to false is only recommended if your renderer has
	 * poor-quality upscaling, since we will use more CPU and network
	 * bandwidth when it is false.
	 *
	 * @return
	 */
	public boolean isRescaleByRenderer() {
		return getBoolean(RESCALE_BY_RENDERER, true);
	}

	/**
	 * Whether to prepend audio track numbers to audio titles.
	 * e.g. "Stairway to Heaven" becomes "4: Stairway to Heaven".
	 *
	 * This is to provide a workaround for devices that order everything
	 * alphabetically instead of in the order we give, like Samsung devices.
	 *
	 * @return whether to prepend audio track numbers to audio titles.
	 */
	public boolean isPrependTrackNumbers() {
		return getBoolean(PREPEND_TRACK_NUMBERS, false);
	}

	public String getFFmpegVideoFilterOverride() {
		return getString(OVERRIDE_FFMPEG_VF, "");
	}

	public static ArrayList<String> getAllRenderersNames() {
		return allRenderersNames;
	}

	public int getTranscodedVideoAudioSampleRate() {
		return getInt(TRANSCODED_VIDEO_AUDIO_SAMPLE_RATE, 48000);
	}

	public boolean isLimitFolders() {
		return getBoolean(LIMIT_FOLDERS, true);
	}

	/**
	 * Perform renderer-specific name reformatting:<p>
	 * Truncating and wrapping see {@code TextWrap}<br>
	 * Character substitution see {@code CharMap}
	 *
	 * @param name Original name
	 * @param suffix Additional media information
	 * @param dlna The actual DLNA resource
	 * @return Reformatted name
	 */
	public String getDcTitle(String name, String suffix, DLNAResource dlna) {
		// Wrap + truncate
		int len = 0;
		if (lineWidth > 0 && (name.length() + suffix.length()) > lineWidth) {
			int suffix_len = dots.length() + suffix.length();
			if (lineHeight == 1) {
				len = lineWidth - suffix_len;
			} else {
				// Wrap
				int i = dlna.isFolder() ? 0 : indent;
				String newline = "\n" + (dlna.isFolder() ? "" : inset);
				name = name.substring(0, i + (i < name.length() && Character.isWhitespace(name.charAt(i)) ? 1 : 0))
					+ WordUtils.wrap(name.substring(i) + suffix, lineWidth - i, newline, true);
				len = lineWidth * lineHeight;
				if (len != 0 && name.length() > len) {
					len = name.substring(0, name.length() - lineWidth).lastIndexOf(newline) + newline.length();
					name = name.substring(0, len) + name.substring(len, len + lineWidth).replace(newline, " ");
					len += (lineWidth - suffix_len - i);
				} else {
					len = -1; // done
				}
			}
			if (len > 0) {
				// Truncate
				name = name.substring(0, len).trim() + dots;
			}
		}
		if (len > -1) {
			name += suffix;
		}

		// Substitute
		for (String s : charMap.keySet()) {
			String repl = charMap.get(s).replaceAll("###e", "");
			name = name.replaceAll(s, repl);
		}

		return name;
	}

	/**
	 * @see #isSendDateMetadata()
	 * @deprecated
	 */
	@Deprecated
	public boolean isOmitDcDate() {
		return !isSendDateMetadata();
	}

	public static int getIntAt(String s, String key, int fallback) {
		try {
			return Integer.valueOf((s + " ").split(key)[1].split("\\D")[0]);
		} catch (Exception e) {
			return fallback;
		}
	}

	/**
	 * List of the renderer supported external subtitles formats
	 * for streaming together with streaming (not transcoded) video.
	 *
	 * @return A comma-separated list of supported text-based external subtitles formats.
	 */
	public String getSupportedExternalSubtitles() {
		return getString(SUPPORTED_EXTERNAL_SUBTITLES_FORMATS, "");
	}

	/**
	 * List of video formats for which supported external subtitles formats
	 * are set for streaming together with streaming (not transcoded) video.
	 * If empty all subtitles listed in "SupportedExternalSubtitlesFormats" will be streamed.
	 * When specified only for listed video formats subtitles will be streamed.
	 *
	 * @return A comma-separated list of supported video formats listed in "Supported" section.
	 */
	public String getVideoFormatsSupportingStreamedExternalSubtitles() {
		return getString(VIDEO_FORMATS_SUPPORTING_STREAMED_EXTERNAL_SUBTITLES, "");
	}

	/**
	 * List of the renderer supported embedded subtitles formats.
	 *
	 * @return A comma-separated list of supported embedded subtitles formats.
	 */
	public String getSupportedEmbeddedSubtitles() {
		return getString(SUPPORTED_INTERNAL_SUBTITLES_FORMATS, "");
	}

	public boolean useClosedCaption() {
		return getBoolean(USE_CLOSED_CAPTION, false);
	}

	public boolean offerSubtitlesAsResource() {
		return getBoolean(OFFER_SUBTITLES_AS_SOURCE, true);
	}

	public boolean offerSubtitlesByProtocolInfo() {
		return getBoolean(OFFER_SUBTITLES_BY_PROTOCOL_INFO, true);
	}

	public boolean isSubtitlesStreamingSupported() {
		return StringUtils.isNotBlank(getSupportedExternalSubtitles());
	}

	/**
	 * Check if the given subtitle type is supported by renderer for streaming for given media.
	 *
	 * @param subtitle Subtitles for checking
	 * @param media Played media
	 *
	 * @return True if the renderer specifies support for the subtitles and
	 * renderer supports subs streaming for the given media video.
	 */
	public boolean isExternalSubtitlesFormatSupported(DLNAMediaSubtitle subtitle, DLNAMediaInfo media) {
		if (subtitle == null || media == null) {
			return false;
		}

		if (isSubtitlesStreamingSupported()) {
			String[] supportedFormats = null;
			if (StringUtils.isNotBlank(getVideoFormatsSupportingStreamedExternalSubtitles())) {
				supportedFormats = getVideoFormatsSupportingStreamedExternalSubtitles().split(",");
			}

			String[] supportedSubs = getSupportedExternalSubtitles().split(",");
			for (String supportedSub : supportedSubs) {
				if (subtitle.getType().toString().equals(supportedSub.trim().toUpperCase())) {
					if (supportedFormats != null) {
						for (String supportedFormat : supportedFormats) {
							if (media.getCodecV() != null && media.getCodecV().equals(supportedFormat.trim())) {
								return true;
							}
						}
					} else {
						return true;
					}

				}
			}
		}

		return false;
	}

	/**
	 * Check if the internal subtitle type is supported by renderer.
	 *
	 * @param subtitle Subtitles for checking
	 * @return True if the renderer specifies support for the subtitles
	 */
	public boolean isEmbeddedSubtitlesFormatSupported(DLNAMediaSubtitle subtitle) {
		if (subtitle == null) {
			return false;
		}

		if (isEmbeddedSubtitlesSupported()) {
			String[] supportedSubs = getSupportedEmbeddedSubtitles().split(",");
			for (String supportedSub : supportedSubs) {
				if (subtitle.getType().toString().equals(supportedSub.trim().toUpperCase())) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isEmbeddedSubtitlesSupported() {
		return StringUtils.isNotBlank(getSupportedEmbeddedSubtitles());
	}

	/**
	 * Get the renderer setting of the output video 3D format to which the video should be converted.
	 *
	 * @return the lowercase string of the output video 3D format or an
	 * empty string when the output format is not specified or not implemented.
	 */
	public String getOutput3DFormat() {
		String value = getString(OUTPUT_3D_FORMAT, "").toLowerCase(Locale.ROOT);
		// check if the parameter is specified correctly
		if (StringUtils.isNotBlank(value)) {
			for (Mode3D format : DLNAMediaInfo.Mode3D.values()) {
				if (value.equals(format.toString().toLowerCase(Locale.ROOT))) {
					return value;
				}
			}

			LOGGER.debug("The output 3D format `{}` specified in the `Output3DFormat` is not implemented or incorrectly specified.", value);
		}

		return "";
	}

	public boolean ignoreTranscodeByteRangeRequests() {
		return getBoolean(IGNORE_TRANSCODE_BYTE_RANGE_REQUEST, false);
	}

	public String calculatedSpeed() throws InterruptedException, ExecutionException {
		String max = getString(MAX_VIDEO_BITRATE, "");
		for (Entry<InetAddress, RendererConfiguration> entry : addressAssociation.entrySet()) {
			if (entry.getValue() == this) {
				Future<Integer> speed = SpeedStats.getInstance().getSpeedInMBitsStored(entry.getKey());
				if (speed != null) {
					if (max == null) {
						return String.valueOf(speed.get());
					}
					try {
						Integer i = Integer.parseInt(max);
						if (speed.get() > i && i > 0) {
							return max;
						}
						return String.valueOf(speed.get());
					} catch (NumberFormatException e) {
						return String.valueOf(speed.get());
					}
				}
			}
		}
		return isBlank(max) ? "0" : max;
	}

	/**
	 * A case-insensitive string comparator
	 */
	public static final Comparator<String> CaseInsensitiveComparator = new Comparator<String>() {
		@Override
		public int compare(String s1, String s2) {
			return s1.compareToIgnoreCase(s2);
		}
	};

	/**
	 * A case-insensitive key-sorted map of headers that can join its values
	 * into a combined string or regex.
	 */
	public static class SortedHeaderMap extends TreeMap<String, String> {
		private static final long serialVersionUID = -5090333053981045429L;
		String headers = null;

		public SortedHeaderMap() {
			super(CaseInsensitiveComparator);
		}

		public SortedHeaderMap(Collection<Map.Entry<String, String>> headers) {
			this();
			for (Map.Entry<String, String> h : headers) {
				put(h.getKey(), h.getValue());
			}
		}

		@Override
		public String put(String key, String value) {
			if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)) {
				headers = null; // i.e. mark as changed
				return super.put(key.trim(), value.trim());
			}
			return null;
		}

		public String put(String raw) {
			return put(StringUtils.substringBefore(raw, ":"), StringUtils.substringAfter(raw, ":"));
		}

		public String joined() {
			if (headers == null) {
				headers = StringUtils.join(values(), " ");
			}
			return headers;
		}

		public String toRegex() {
			int size = size();
			return (size > 1 ? "(" : "") + StringUtils.join(values(), ").*(") + (size > 1 ? ")" : "");
		}
	}

	/**
	 * Pattern match our combined header matcher to the given collection of sorted request
	 * headers as a whole.
	 *
	 * @param headers The headers.
	 * @return True if the pattern matches or false if no match, no headers, or no matcher.
	 */
	public boolean match(SortedHeaderMap headers) {
		if (headers != null && !headers.isEmpty() && sortedHeaderMatcher != null) {
			return sortedHeaderMatcher.reset(headers.joined()).find();
		}
		return false;
	}

	/**
	 * The loading priority of this renderer. This should be set to 1 (or greater)
	 * if this renderer config is a more specific version of one we already have.
	 *
	 * For example, we have a Panasonic TVs config that is used for all
	 * Panasonic TVs, except the ones we have specific configs for, so the
	 * specific ones have a greater priority to ensure they are used when
	 * applicable instead of the less-specific renderer config.
	 *
	 * @return The loading priority of this renderer
	 */
	public int getLoadingPriority() {
		return getInt(LOADING_PRIORITY, 0);
	}

	/**
	 * A loading priority comparator
	 */
	public static final Comparator<RendererConfiguration> rendererLoadingPriorityComparator = new Comparator<RendererConfiguration>() {
		@Override
		public int compare(RendererConfiguration r1, RendererConfiguration r2) {
			if (r1 == null || r2 == null) {
				return (r1 == null && r2 == null) ? 0 : r1 == null ? 1 : r2 == null ? -1 : 0;
			}
			int p1 = r1.getLoadingPriority();
			int p2 = r2.getLoadingPriority();
			return p1 > p2 ? -1 : p1 < p2 ? 1 : r1.getConfName().compareToIgnoreCase(r2.getConfName());
		}
	};

	private static int[] getVideoBitrateConfig(String bitrate) {
		int bitrates[] = new int[2];

		if (bitrate.contains("(") && bitrate.contains(")")) {
			bitrates[1] = Integer.parseInt(bitrate.substring(bitrate.indexOf('(') + 1, bitrate.indexOf(')')));
		}

		if (bitrate.contains("(")) {
			bitrate = bitrate.substring(0, bitrate.indexOf('(')).trim();
		}

		if (StringUtils.isBlank(bitrate)) {
			bitrate = "0";
		}

		bitrates[0] = (int) Double.parseDouble(bitrate);

		return bitrates;
	}

	/**
	 * Automatic reloading
	 */
	public static final FileWatcher.Listener reloader = new FileWatcher.Listener() {
		@Override
		public void notify(String filename, String event, FileWatcher.Watch watch, boolean isDir) {
			RendererConfiguration r = (RendererConfiguration) watch.getItem();
			if (r != null && r.getFile().equals(new File(filename))) {
				r.reset();
			}
		}
	};

	private DLNAResource playingRes;

	public DLNAResource getPlayingRes() {
		return playingRes;
	}

	public void setPlayingRes(DLNAResource dlna) {
		playingRes = dlna;
		getPlayer();
		if (dlna != null) {
			player.getState().name = dlna.getDisplayName();
			player.start();
		} else {
			player.reset();
		}
	}

	private long buffer;

	public void setBuffer(long mb) {
		buffer = mb < 0 ? 0 : mb;
		getPlayer().setBuffer(mb);
	}

	public long getBuffer() {
		return buffer;
	}

	public String getSubLanguage() {
		return pmsConfiguration.getSubtitlesLanguages();
	}

	public static class PlaybackTimer extends BasicPlayer.Minimal {
		private long duration = 0;

		public PlaybackTimer(DeviceConfiguration renderer) {
			super(renderer);
			LOGGER.debug("Created playback timer for " + renderer.getRendererName());
		}

		@Override
		public void start() {
			final DLNAResource res = renderer.getPlayingRes();
			state.name = res.getDisplayName();
			duration = 0;
			if (res.getMedia() != null) {
				duration = (long) res.getMedia().getDurationInSeconds() * 1000;
				state.duration = DurationFormatUtils.formatDuration(duration, "HH:mm:ss");
			}
			Runnable r = new Runnable() {
				@Override
				public void run() {
					state.playback = PLAYING;
					while (res == renderer.getPlayingRes()) {
						long elapsed;
						if ((long) res.getLastStartPosition() == 0) {
							elapsed = System.currentTimeMillis() - res.getStartTime();
						} else {
							elapsed = System.currentTimeMillis() - (long) res.getLastStartSystemTime();
							elapsed += (long) (res.getLastStartPosition() * 1000);
						}

						if (duration == 0 || elapsed < duration + 500) {
							// Position is valid as far as we can tell
							state.position = DurationFormatUtils.formatDuration(elapsed, "HH:mm:ss");
						} else {
							// Position is invalid, blink instead
							state.position = ("NOT_IMPLEMENTED" + (elapsed / 1000 % 2 == 0 ? "  " : "--"));
						}
						alert();
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
						}
					}
					// Reset only if another item hasn't already begun playing
					if (renderer.getPlayingRes() == null) {
						reset();
					}
				}
			};
			new Thread(r).start();
		}
	}

	public final String INFO = "info";
	public final String OK = "okay";
	public final String WARN = "warn";
	public final String ERR = "err";

	@SuppressWarnings("unused")
	public void notify(String type, String msg) {
		// Implemented by subclasses
	}

	public int getMaxVolume() {
		return getInt(MAX_VOLUME, 100);
	}

	public void setIdentifiers(List<String> identifiers) {
		this.identifiers = identifiers;
	}

	public List<String> getIdentifiers() {
		return identifiers;
	}

	public String getDeviceId() {
		String d = getString(DEVICE_ID, "");
		if (StringUtils.isBlank(d)) {
			// Backward compatibility
			d = getString("device", "");
		}
		// Note: this might be a comma-separated list of ids
		return d;
	}

	/**
	 * Upnp service startup management
	 */

	public static final int BLOCK = -2;
	public static final int POSTPONE = -1;
	public static final int NONE = 0;
	public static final int ALLOW = 1;

	protected volatile int upnpMode = NONE;

	public static int getUpnpMode(String mode) {
		if (mode != null) {
			switch (mode.trim().toLowerCase()) {
				case "false":    return BLOCK;
				case "postpone": return POSTPONE;
			}
		}
		return ALLOW;
	}

	public static String getUpnpModeString(int mode) {
		switch (mode) {
			case BLOCK:    return "blocked";
			case POSTPONE: return "postponed";
			case NONE:     return "unknown";
		}
		return "allowed";
	}

	public int getUpnpMode() {
		if (upnpMode == NONE) {
			upnpMode = getUpnpMode(getString(UPNP_ALLOW, "true"));
		}
		return upnpMode;
	}

	public String getUpnpModeString() {
		return getUpnpModeString(upnpMode);
	}

	public void resetUpnpMode() {
		setUpnpMode(getUpnpMode(getString(UPNP_ALLOW, "true")));
	}

	public void setUpnpMode(int mode) {
		if (upnpMode != mode) {
			upnpMode = mode;
			if (upnpMode == ALLOW) {
				String id = uuid != null ? uuid : DeviceConfiguration.getUuidOf(getAddress());
				if (id != null) {
					configuration.setProperty(UPNP_ALLOW, "true");
					UPNPHelper.activate(id);
				}
			}
		}
	}

	public boolean isUpnpPostponed() {
		return getUpnpMode() == POSTPONE;
	}

	public boolean isUpnpAllowed() {
		return getUpnpMode() > NONE;
	}

	public static void verify(RendererConfiguration r) {
		// FIXME: this is a very fallible, incomplete validity test for use only until
		// we find something better. The assumption is that renderers unable determine
		// their own address (i.e. non-UPnP/web renderers that have lost their spot in the
		// address association to a newer renderer at the same ip) are "invalid".
		if (r.getUpnpMode() != BLOCK && r.getAddress() == null) {
			LOGGER.debug("Purging renderer {} as invalid", r);
			r.delete(0);
		}
	}

	/**
	 * Whether the renderer can display thumbnails.
	 *
	 * @return whether the renderer can display thumbnails
	 */
	public boolean isThumbnails() {
		return getBoolean(THUMBNAILS, true);
	}

	/**
	 * Whether we should add black padding to thumbnails so they are always
	 * at the same resolution, or just scale to within the limits.
	 *
	 * @return whether to add padding to thumbnails
	 */
	public boolean isThumbnailPadding() {
		return getBoolean(THUMBNAIL_PADDING, false);
	}

	/**
	 * Whether to stream subtitles even if the video is transcoded. It may work on some renderers.
	 *
	 * @return whether to stream subtitles for transcoded video
	 */
	public boolean streamSubsForTranscodedVideo() {
		return getBoolean(STREAM_SUBS_FOR_TRANSCODED_VIDEO, false);
	}

	/**
	 * List of supported video bit depths.
	 *
	 * @return a comma-separated list of supported video bit depths.
	 */
	public String getSupportedVideoBitDepths() {
		return getString(SUPPORTED_VIDEO_BIT_DEPTHS, "8");
	}

	/**
	 * Check if the given video bit depth is supported.
	 *
	 * @param videoBitDepth The video bit depth
	 *
	 * @return whether the video bit depth is supported.
	 */
	public boolean isVideoBitDepthSupported(int videoBitDepth) {
		String[] supportedBitDepths = getSupportedVideoBitDepths().split(",");
		for (String supportedBitDepth : supportedBitDepths) {
			if (Integer.toString(videoBitDepth).equals(supportedBitDepth.trim())) {
				return true;
			}
		}

		return false;
	}

	public boolean isRemoveTagsFromSRTsubs() {
		return getBoolean(REMOVE_TAGS_FROM_SRT_SUBS, true);
	}
}
