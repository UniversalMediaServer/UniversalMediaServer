package net.pms.configuration;

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.LibMediaInfoParser;
import net.pms.dlna.RootFolder;
import net.pms.formats.Format;
import net.pms.network.HTTPResource;
import net.pms.network.SpeedStats;
import net.pms.network.UPNPHelper;
import net.pms.util.PropertiesUtil;
import net.pms.newgui.StatusTab;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererConfiguration extends UPNPHelper.Renderer {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererConfiguration.class);
	private static ArrayList<RendererConfiguration> enabledRendererConfs;
	private static ArrayList<String> allRenderersNames = new ArrayList<>();
	private static PmsConfiguration pmsConfiguration;
	private static RendererConfiguration defaultConf;
	private static Map<InetAddress, RendererConfiguration> addressAssociation = new HashMap<>();

	private RootFolder rootFolder;
	private final PropertiesConfiguration configuration;
	private final ConfigurationReader configurationReader;
	private FormatConfiguration formatConfiguration;
	private int rank;

	public StatusTab.rendererItem statusIcon;
	public boolean loaded;
	private UPNPHelper.Player player;

	// Holds MIME type aliases
	private final Map<String, String> mimes;

	private final Map<String, String> charMap;
	private final Map<String, String> DLNAPN;

	// TextWrap parameters
	protected int line_w, line_h, indent;
	protected String inset, dots;
	protected boolean dc_date = true;

	// property values
	private static final String LPCM = "LPCM";
	private static final String MP3 = "MP3";
	private static final String WAV = "WAV";
	private static final String WMV = "WMV";

	// Old video transcoding options
	@Deprecated
	private static final String DEPRECATED_MPEGAC3 = "MPEGAC3";

	@Deprecated
	private static final String DEPRECATED_MPEGPSAC3 = "MPEGPSAC3";

	@Deprecated
	private static final String DEPRECATED_MPEGTSAC3 = "MPEGTSAC3";

	@Deprecated
	private static final String DEPRECATED_H264TSAC3 = "H264TSAC3";

	// Current video transcoding options
	private static final String MPEGTSH264AAC = "MPEGTS-H264-AAC";
	private static final String MPEGTSH264AC3 = "MPEGTS-H264-AC3";
	private static final String MPEGPSMPEG2AC3 = "MPEGPS-MPEG2-AC3";
	private static final String MPEGTSMPEG2AC3 = "MPEGTS-MPEG2-AC3";

	// property names
	private static final String ACCURATE_DLNA_ORGPN = "AccurateDLNAOrgPN";
	private static final String AUDIO = "Audio";
	private static final String AUTO_EXIF_ROTATE = "AutoExifRotate";
	private static final String BYTE_TO_TIMESEEK_REWIND_SECONDS = "ByteToTimeseekRewindSeconds"; // Ditlew
	private static final String CBR_VIDEO_BITRATE = "CBRVideoBitrate"; // Ditlew
	private static final String CHARMAP = "CharMap";
	private static final String CHUNKED_TRANSFER = "ChunkedTransfer";
	private static final String CUSTOM_FFMPEG_OPTIONS = "CustomFFmpegOptions";
	private static final String CUSTOM_MENCODER_OPTIONS = "CustomMencoderOptions";
	private static final String CUSTOM_MENCODER_MPEG2_OPTIONS = "CustomMencoderQualitySettings"; // TODO (breaking change): value should be CustomMEncoderMPEG2Options
	private static final String DEFAULT_VBV_BUFSIZE = "DefaultVBVBufSize";
	private static final String DISABLE_MENCODER_NOSKIP = "DisableMencoderNoskip";
	private static final String DLNA_LOCALIZATION_REQUIRED = "DLNALocalizationRequired";
	private static final String DLNA_ORGPN_USE = "DLNAOrgPN";
	private static final String DLNA_PN_CHANGES = "DLNAProfileChanges";
	private static final String DLNA_TREE_HACK = "CreateDLNATreeFaster";
	private static final String LIMIT_FOLDERS = "LimitFolders";
	private static final String FORCE_JPG_THUMBNAILS = "ForceJPGThumbnails"; // Sony devices require JPG thumbnails
	private static final String H264_L41_LIMITED = "H264Level41Limited";
	private static final String IMAGE = "Image";
	private static final String IGNORE_TRANSCODE_BYTE_RANGE_REQUEST = "IgnoreTranscodeByteRangeRequests";
	private static final String KEEP_ASPECT_RATIO = "KeepAspectRatio";
	private static final String MAX_VIDEO_BITRATE = "MaxVideoBitrateMbps";
	private static final String MAX_VIDEO_HEIGHT = "MaxVideoHeight";
	private static final String MAX_VIDEO_WIDTH = "MaxVideoWidth";
	private static final String MEDIAPARSERV2 = "MediaInfo";
	private static final String MEDIAPARSERV2_THUMB = "MediaParserV2_ThumbnailGeneration";
	private static final String MIME_TYPES_CHANGES = "MimeTypesChanges";
	private static final String MUX_DTS_TO_MPEG = "MuxDTSToMpeg";
	private static final String MUX_H264_WITH_MPEGTS = "MuxH264ToMpegTS";
	private static final String MUX_LPCM_TO_MPEG = "MuxLPCMToMpeg";
	private static final String MUX_NON_MOD4_RESOLUTION = "MuxNonMod4Resolution";
	private static final String OVERRIDE_FFMPEG_VF = "OverrideFFmpegVideoFilter";
	private static final String RENDERER_ICON = "RendererIcon";
	private static final String RENDERER_NAME = "RendererName";
	private static final String RESCALE_BY_RENDERER = "RescaleByRenderer";
	private static final String SEEK_BY_TIME = "SeekByTime";
	private static final String SHOW_AUDIO_METADATA = "ShowAudioMetadata";
	private static final String SHOW_DVD_TITLE_DURATION = "ShowDVDTitleDuration"; // Ditlew
	private static final String SHOW_SUB_METADATA = "ShowSubMetadata";
	private static final String STREAM_EXT = "StreamExtensions";
	private static final String SUBTITLE_HTTP_HEADER = "SubtitleHttpHeader";
	private static final String SUPPORTED = "Supported";
	private static final String SUPPORTED_SUBTITLES_FORMATS = "SupportedSubtitlesFormats";
	private static final String TEXTWRAP = "TextWrap";
	private static final String THUMBNAIL_AS_RESOURCE = "ThumbnailAsResource";
	private static final String TRANSCODE_AUDIO_441KHZ = "TranscodeAudioTo441kHz";
	private static final String TRANSCODE_AUDIO = "TranscodeAudio";
	private static final String TRANSCODE_EXT = "TranscodeExtensions";
	private static final String TRANSCODE_FAST_START = "TranscodeFastStart";
	private static final String TRANSCODE_VIDEO = "TranscodeVideo";
	private static final String TRANSCODED_SIZE = "TranscodedVideoFileSize";
	private static final String TRANSCODED_VIDEO_AUDIO_SAMPLE_RATE = "TranscodedVideoAudioSampleRate";
	private static final String USER_AGENT_ADDITIONAL_HEADER = "UserAgentAdditionalHeader";
	private static final String USER_AGENT_ADDITIONAL_SEARCH = "UserAgentAdditionalHeaderSearch";
	private static final String USER_AGENT = "UserAgentSearch";
	private static final String UPNP_DETAILS = "UpnpDetailsSearch";
	private static final String USE_CLOSED_CAPTION = "UseClosedCaption";
	private static final String USE_SAME_EXTENSION = "UseSameExtension";
	private static final String VIDEO = "Video";
	private static final String WRAP_DTS_INTO_PCM = "WrapDTSIntoPCM";
	private static final String WRAP_ENCODED_AUDIO_INTO_PCM = "WrapEncodedAudioIntoPCM";

	public static RendererConfiguration getDefaultConf() {
		return defaultConf;
	}

	/**
	 * Load all renderer configuration files and set up the default renderer.
	 *
	 * @param pmsConf
	 */
	public static void loadRendererConfigurations(PmsConfiguration pmsConf) {
		pmsConfiguration = pmsConf;
		enabledRendererConfs = new ArrayList<>();

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
			List<String> ignoredRenderers = pmsConfiguration.getIgnoredRenderers();
			for (File f : confs) {
				if (f.getName().endsWith(".conf")) {
					try {
						RendererConfiguration r = new RendererConfiguration(f);
						r.rank = rank++;
						String rendererName = r.getRendererName();
						if (!ignoredRenderers.contains(rendererName)) {
							enabledRendererConfs.add(r);
							LOGGER.info("Loaded configuration for renderer: " + rendererName);
						} else {
							LOGGER.debug("Ignored " + rendererName + " configuration");
						}
					} catch (ConfigurationException ce) {
						LOGGER.info("Error in loading configuration of: " + f.getAbsolutePath());
					}
				}
			}
		}

		if (enabledRendererConfs.size() > 0) {
			// See if a different default configuration was configured
			String rendererFallback = pmsConfiguration.getRendererDefault();

			if (StringUtils.isNotBlank(rendererFallback)) {
				RendererConfiguration fallbackConf = getRendererConfigurationByName(rendererFallback);

				if (fallbackConf != null) {
					// A valid fallback configuration was set, use it as default.
					defaultConf = fallbackConf;
				}
			}
		}
	}

	private static void loadRenderersNames() {
		File renderersDir = getRenderersDir();

		if (renderersDir != null) {
			LOGGER.info("Loading renderer names from " + renderersDir.getAbsolutePath());

			for (File f : renderersDir.listFiles()) {
				if (f.getName().endsWith(".conf")) {
					try {
						allRenderersNames.add(new RendererConfiguration(f).getRendererName());
					} catch (ConfigurationException ce) {
						LOGGER.warn("Error loading " + f.getAbsolutePath());
					}
				}
			}

			Collections.sort(allRenderersNames, String.CASE_INSENSITIVE_ORDER);
		}
	}

	private int getInt(String key, int def) {
		return configurationReader.getInt(key, def);
	}

	private long getLong(String key, int def) {
		return configurationReader.getLong(key, def);
	}

	private boolean getBoolean(String key, boolean def) {
		return configurationReader.getBoolean(key, def);
	}

	/**
	 * Return the <code>String</code> value for a given configuration key if the
	 * value is non-blank (i.e. not null, not an empty string, not all whitespace).
	 * Otherwise return the supplied default value.
	 * The value is returned with leading and trailing whitespace removed in both cases.
	 *
	 * @param key The key to look up.
	 * @param def The default value to return when no valid key value can be found.
	 * @return The value configured for the key.
	 */
	private String getString(String key, String def) {
		return configurationReader.getString(key, def);
	}

	@Deprecated
	public static ArrayList<RendererConfiguration> getAllRendererConfigurations() {
		return getEnabledRenderersConfigurations();
	}

	/**
	 * Returns the list of all renderer configurations.
	 *
	 * @return The list of all configurations.
	 */
	public static ArrayList<RendererConfiguration> getEnabledRenderersConfigurations() {
		return enabledRendererConfs;
	}

	public static Collection<RendererConfiguration> getConnectedRenderersConfigurations() {
		return addressAssociation.values();
	}

	public static boolean hasConnectedAVTransportPlayers() {
		return UPNPHelper.hasRenderer(UPNPHelper.AVT);
	}

	public static List<RendererConfiguration> getConnectedAVTransportPlayers() {
		return UPNPHelper.getRenderers(UPNPHelper.AVT);
	}

	public static boolean hasConnectedControlPlayers() {
		return UPNPHelper.hasRenderer(UPNPHelper.ANY);
	}

	public static List<RendererConfiguration> getConnectedControlPlayers() {
		return UPNPHelper.getRenderers(UPNPHelper.ANY);
	}

	public static File getRenderersDir() {
		final String[] pathList = PropertiesUtil.getProjectProperties().get("project.renderers.dir").split(",");

		for (String path : pathList) {
			if (path.trim().length() > 0) {
				File file = new File(path.trim());

				if (file.isDirectory()) {
					if (file.canRead()) {
						return file;
					} else {
						LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
					}
				}
			}
		}

		return null;
	}

	public static void resetAllRenderers() {
		for (RendererConfiguration rc : enabledRendererConfs) {
			rc.rootFolder = null;
		}
	}

	public RootFolder getRootFolder() {
		if (rootFolder == null) {
			ArrayList<String> tags = new ArrayList<String>();
			tags.add(getRendererName());
			for (InetAddress sa : addressAssociation.keySet()) {
				if (addressAssociation.get(sa) == this) {
					tags.add(sa.getHostAddress());
				}
			}

			rootFolder = new RootFolder(tags);
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
	 * @see #getRendererConfigurationBySocketAddress(InetAddress)
	 */
	public boolean associateIP(InetAddress sa) {
		if (UPNPHelper.isNonRenderer(sa)) {
			// TODO: remove it if already added unknowingly
			return false;
		}

		// FIXME: handle multiple clients with same ip properly, now newer overwrites older

		addressAssociation.put(sa, this);
		if ((pmsConfiguration.isAutomaticMaximumBitrate() || pmsConfiguration.isSpeedDbg()) &&
				!(sa.isLoopbackAddress() || sa.isAnyLocalAddress())) {
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
			SpeedStats.getInstance().getSpeedInMBits(sa, r.getRendererName());
		}
	}

	public static RendererConfiguration getRendererConfigurationBySocketAddress(InetAddress sa) {
		RendererConfiguration r = addressAssociation.get(sa);
		return (r != null && r.loaded) ? r : null;
	}

	/**
	 * Tries to find a matching renderer configuration based on a request
	 * header line with a User-Agent header. These matches are made using
	 * the "UserAgentSearch" configuration option in a renderer.conf.
	 * Returns the matched configuration or <code>null</code> if no match
	 * could be found.
	 *
	 * @param userAgentString The request header line.
	 * @return The matching renderer configuration or <code>null</code>.
	 */
	public static RendererConfiguration getRendererConfigurationByUA(String userAgentString) {
		if (pmsConfiguration.isRendererForceDefault()) {
			// Force default renderer
			LOGGER.trace("Forcing renderer match to \"" + defaultConf.getRendererName() + "\"");
			return manageRendererMatch(defaultConf);
		} else {
			// Try to find a match
			for (RendererConfiguration r : enabledRendererConfs) {
				if (r.matchUserAgent(userAgentString)) {
					return manageRendererMatch(r);
				}
			}
		}

		return null;
	}

	private static RendererConfiguration manageRendererMatch(RendererConfiguration r) {
		if (addressAssociation.values().contains(r)) {
			// FIXME: This cannot ever ever happen because of how renderer matching
			// is implemented in RequestHandler and RequestHandlerV2. The first header
			// match will associate the IP address with the renderer and from then on
			// all other requests from the same IP address will be recognized based on
			// that association. Headers will be ignored and unfortunately they happen
			// to be the only way to get here.
			LOGGER.info("Another renderer like " + r.getRendererName() + " was found!");
		}

		return r;
	}

	/**
	 * Tries to find a matching renderer configuration based on a request
	 * header line with an additional, non-User-Agent header. These matches
	 * are made based on the "UserAgentAdditionalHeader" and
	 * "UserAgentAdditionalHeaderSearch" configuration options in a
	 * renderer.conf. Returns the matched configuration or <code>null</code>
	 * if no match could be found.
	 *
	 * @param header The request header line.
	 * @return The matching renderer configuration or <code>null</code>.
	 */
	public static RendererConfiguration getRendererConfigurationByUAAHH(String header) {
		if (pmsConfiguration.isRendererForceDefault()) {
			// Force default renderer
			LOGGER.trace("Forcing renderer match to \"" + defaultConf.getRendererName() + "\"");
			return manageRendererMatch(defaultConf);
		} else {
			// Try to find a match
			for (RendererConfiguration r : enabledRendererConfs) {
				if (StringUtils.isNotBlank(r.getUserAgentAdditionalHttpHeader()) && header.startsWith(r.getUserAgentAdditionalHttpHeader())) {
					String value = header.substring(header.indexOf(':', r.getUserAgentAdditionalHttpHeader().length()) + 1);
					if (r.matchAdditionalUserAgent(value)) {
						return manageRendererMatch(r);
					}
				}
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
			if (conf.getRendererName().toLowerCase().contains(name.toLowerCase())) {
				return conf;
			}
		}

		return null;
	}

	public static RendererConfiguration getRendererConfigurationByUUID(String uuid) {
		for (RendererConfiguration conf : enabledRendererConfs) {
			if (conf.getUUID().equals(uuid)) {
				return conf;
			}
		}

		return null;
	}

	public static RendererConfiguration getRendererConfigurationByHeaders(String[] headers, InetAddress ia) {
		RendererConfiguration renderer = null;
		String uuid = null;
		for (String header : headers) {
			if (renderer == null) {
				renderer = getRendererConfigurationByHeaderLine(header, ia);
			}
			if (uuid == null && header.toUpperCase().startsWith("USN")) {
				uuid = header.substring(header.indexOf(':') + 1).split("::")[0].trim();
			}
		}
		if (uuid != null && renderer != null && renderer.getUUID() == null) {
			renderer.setUUID(uuid);
		}
		return renderer;
	}

	public static RendererConfiguration getRendererConfigurationByUPNPDetails(String details/*, InetAddress ia, String uuid*/) {
		for (RendererConfiguration r : enabledRendererConfs) {
			if (r.matchUPNPDetails(details)) {
				LOGGER.trace("Matched media renderer \"" + r.getRendererName() + "\" based on dlna details \"" + details + "\"");
				return r;
			}
		}
		return null;
	}

	public static RendererConfiguration getRendererConfigurationByHeaderLine(String header, InetAddress ia) {
		String userAgentString;
		RendererConfiguration r = null, ref = null;
		boolean isNew = false;

		if (header.toUpperCase().startsWith("USER-AGENT")) {
			userAgentString = header.substring(header.indexOf(':') + 1).trim();
			ref = getRendererConfigurationByUA(userAgentString);
		}

		if (ref == null) {
			ref = getRendererConfigurationByUAAHH(header);
		}

		if (ref != null) {
			try {
				if (addressAssociation.containsKey(ia)) {
					// Already seen, finish configuration if required
					r = addressAssociation.get(ia);
					if (! r.loaded) {
						r.init(ref.getFile());
						// update gui
						PMS.get().updateRenderer(r);
					}
				} else if (! UPNPHelper.isNonRenderer(ia)) {
					// It's brand new
					r = new RendererConfiguration(ref.getFile());
					if (r.associateIP(ia)) {
						PMS.get().setRendererFound(r);
						isNew = true;
					}
				}
			} catch (Exception e) {
			}
			if (r != null) {
				LOGGER.trace("Matched " + (isNew ? "new " : "") + "media renderer \"" + r.getRendererName() + "\" based on header \"" + header + "\"");
			}
		}

		return r;
	}

	public FormatConfiguration getFormatConfiguration() {
		return formatConfiguration;
	}

	public File getFile() {
		return getFile(false);
	}

	public File getFile(boolean force) {
		return configuration.getFile() != null ? configuration.getFile() :
			force ? new File(getRenderersDir(), getRendererName().split("\\(")[0].trim().replace(" ", "") + ".conf") : null;
	}

	public static void createNewFile(RendererConfiguration r, File file, boolean load, File ref) {
		try {
			ArrayList<String> conf = new ArrayList<String>();
			String name = r.getRendererName().split("\\(")[0].trim();
			Map<String, String> details = r.getUpnpDetails();
			String detailmatcher = details == null ? "" :
				(details.get("manufacturer") + " , " + details.get("modelName"));

			// Add the header and identifiers
			conf.add("#----------------------------------------------------------------------------");
			conf.add("# Auto-generated profile for " + name);
			conf.add("#" + (ref == null ? "" : " Based on " + ref.getName()));
			conf.add("# See PS3.conf for a description of all possible configuration options.");
			conf.add("#");
			conf.add("");
			conf.add(RENDERER_NAME + " = " + name);
			conf.add(UPNP_DETAILS + " = " + detailmatcher);
			conf.add("");
			// TODO: Set more properties automatically from UPNP info

			if (ref != null) {
				// Copy the reference file, skipping its header and identifiers
				Matcher skip = Pattern.compile(".*(" + RENDERER_ICON + "|" + RENDERER_NAME + "|" +
					UPNP_DETAILS + "|" + USER_AGENT + "|" + USER_AGENT_ADDITIONAL_HEADER + "|" +
					USER_AGENT_ADDITIONAL_SEARCH + ").*").matcher("");
				boolean header = true;
				for (String line : FileUtils.readLines(ref, Charsets.UTF_8)) {
					if (skip.reset(line).matches() ||
							(header && (line.startsWith("#") || StringUtils.isBlank(line)))) {
						continue;
					}
					header = false;
					conf.add(line);
				}
			}

			FileUtils.writeLines(file, "utf-8", conf, "\r\n");

			if (load) {
				try {
					r.init(file);
				} catch (ConfigurationException ce) {
					LOGGER.debug("Error initializing renderer configuration: " + ce);
				}
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating renderer configuration file: " + ie);
		}
	}

	public int getRank() {
		return rank;
	}

	public boolean isXBOX() {
		return getRendererName().toUpperCase().contains("XBOX");
	}

	public boolean isXBMC() {
		return getRendererName().toUpperCase().contains("XBMC");
	}

	public boolean isPS3() {
		return getRendererName().toUpperCase().contains("PLAYSTATION") || getRendererName().toUpperCase().contains("PS3");
	}

	public boolean isBRAVIA() {
		return getRendererName().toUpperCase().contains("BRAVIA");
	}

	public boolean isFDSSDP() {
		return getRendererName().toUpperCase().contains("FDSSDP");
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

	public RendererConfiguration(File f) throws ConfigurationException {
		this(f, null);
	}

	public RendererConfiguration(File f, String uuid) throws ConfigurationException {
		super(uuid);
		configuration = new PropertiesConfiguration();

		// Treat backslashes in the conf as literal while also supporting double-backslash syntax, i.e.
		// ensure that typical raw regex strings (and unescaped Windows file paths) are read correctly.
		configuration.setIOFactory(new PropertiesConfiguration.DefaultIOFactory() {
			@Override
			public PropertiesConfiguration.PropertiesReader createPropertiesReader(final Reader in, final char delimiter) {
				return new PropertiesConfiguration.PropertiesReader(in, delimiter) {
					@Override
					protected void parseProperty(final String line) {
						// Unescape any double-backslashes, then escape all backslashes before parsing
						super.parseProperty(line.replace("\\\\", "\\").replace("\\", "\\\\"));
					}
				};
			}
		});

		// false: don't log overrides (every renderer conf
		// overrides multiple settings)
		configurationReader = new ConfigurationReader(configuration, false);

		configuration.setListDelimiter((char) 0);

		mimes = new HashMap<>();
		charMap = new HashMap<>();
		DLNAPN = new HashMap<>();
		player = null;

		init(f);
	}

	public void init(File f) throws ConfigurationException {

		configuration.clear();

		if (f != null) {
			configuration.load(f);
			loaded = true;
		}

		mimes.clear();
		String mimeTypes = getString(MIME_TYPES_CHANGES, null);

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
		line_w = getIntAt(s, "width:", 0);
		if (line_w > 0) {
			line_h = getIntAt(s, "height:", 0);
			indent = getIntAt(s, "indent:", 0);
			dc_date = getIntAt(s, "date:", 1) != 0;
			int ws = getIntAt(s, "whitespace:", 9);
			int dotct = getIntAt(s, "dots:", 0);
			inset = new String(new byte[indent]).replaceAll(".", Character.toString((char) ws));
			dots = new String(new byte[dotct]).replaceAll(".", ".");
		}

		charMap.clear();
		String ch = getString(CHARMAP, null);
		if (StringUtils.isNotBlank(ch)) {
			StringTokenizer st = new StringTokenizer(ch, " ");
			String org = "";

			while (st.hasMoreTokens()) {
				String tok = st.nextToken().trim();
				if (StringUtils.isBlank(tok)) {
					continue;
				}
				tok = tok.replaceAll("###0", " ");
				if (StringUtils.isBlank(org)) {
					org = tok;
				} else {
					charMap.put(org, tok);
					org = "";
				}
			}
		}

		DLNAPN.clear();
		String DLNAPNchanges = getString(DLNA_PN_CHANGES, null);

		if (DLNAPNchanges != null) {
			LOGGER.trace("Config DLNAPNchanges: " + DLNAPNchanges);
		}

		if (StringUtils.isNotBlank(DLNAPNchanges)) {
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

		if (isMediaParserV2()) {
			formatConfiguration = new FormatConfiguration(configuration.getList(SUPPORTED));
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

	public boolean isTranscodeToAC3() {
		return isTranscodeToMPEGPSMPEG2AC3() || isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGTSH264AC3();
	}

	public boolean isTranscodeToAAC() {
		return isTranscodeToMPEGTSH264AAC();
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

	public boolean isAutoRotateBasedOnExif() {
		return getBoolean(AUTO_EXIF_ROTATE, false);
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

	public boolean isH264Level41Limited() {
		return getBoolean(H264_L41_LIMITED, false);
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
	public String getMimeType(String mimeType) {
		if (mimeType == null) {
			return null;
		}

		String matchedMimeType = null;

		if (isMediaParserV2()) {
			// Use the supported information in the configuration to determine the transcoding mime type.
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToMPEGTSH264AC3()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AC3);
				} else if (isTranscodeToMPEGTSH264AAC()) {
					matchedMimeType = getFormatConfiguration().match(FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AAC);
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
						if (isTranscodeAudioTo441()) {
							matchedMimeType += ";rate=44100;channels=2";
						} else {
							matchedMimeType += ";rate=48000;channels=2";
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

					if (isTranscodeAudioTo441()) {
						matchedMimeType += ";rate=44100;channels=2";
					} else {
						matchedMimeType += ";rate=48000;channels=2";
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

	/**
	 * Pattern match a user agent header string to the "UserAgentSearch"
	 * expression for this renderer. Will return false when the pattern is
	 * empty or when no match can be made.
	 *
	 * @param header The header containing the user agent.
	 * @return True if the pattern matches.
	 */
	public boolean matchUserAgent(String header) {
		String userAgent = getUserAgent();
		Pattern userAgentPattern;

		if (StringUtils.isNotBlank(userAgent)) {
			userAgentPattern = Pattern.compile(userAgent, Pattern.CASE_INSENSITIVE);

			return userAgentPattern.matcher(header).find();
		} else {
			return false;
		}
	}

	/**
	 * Pattern match a header string to the "UserAgentAdditionalHeaderSearch"
	 * expression for this renderer. Will return false when the pattern is
	 * empty or when no match can be made.
	 *
	 * @param header The additional header string.
	 * @return True if the pattern matches.
	 */
	public boolean matchAdditionalUserAgent(String header) {
		String userAgentAdditionalHeader = getUserAgentAdditionalHttpHeaderSearch();
		Pattern userAgentAddtionalPattern;

		if (StringUtils.isNotBlank(userAgentAdditionalHeader)) {
			userAgentAddtionalPattern = Pattern.compile(userAgentAdditionalHeader, Pattern.CASE_INSENSITIVE);

			return userAgentAddtionalPattern.matcher(header).find();
		} else {
			return false;
		}
	}

	public boolean matchUPNPDetails(String details) {
		String upnpDetails = getUpnpDetailsString();
		Pattern pattern;

		if (StringUtils.isNotBlank(upnpDetails)) {
			String p = StringUtils.join(upnpDetails.split(" , "), ".*");
			pattern = Pattern.compile(p, Pattern.CASE_INSENSITIVE);
			return pattern.matcher(details.replace("\n", " ")).find();
		} else {
			return false;
		}
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
	 * Returns the unique upnp details of this renderer as defined in the
	 * renderer configuration. Default value is "".
	 *
	 * @return The detail string.
	 */
	public String getUpnpDetailsString() {
		return getString(UPNP_DETAILS, "");
	}

	/**
	 * Returns the upnp details of this renderer as broadcast by itself, if known.
	 * Default value is null.
	 *
	 * @return The detail map.
	 */
	public Map<String, String> getUpnpDetails() {
		return UPNPHelper.getDeviceDetails(UPNPHelper.getDevice(uuid));
	}

	/**
	 * Returns the current upnp state variables of this renderer, if known. Default value is null.
	 *
	 * @return The data.
	 */
	public Map<String, String> getUPNPData() {
		return UPNPHelper.getData(uuid, instanceID);
	}

	/**
	 * Returns the upnp services of this renderer.
	 * Default value is null.
	 *
	 * @return The list of service names.
	 */
	public List<String> getUpnpServices() {
		return UPNPHelper.getServiceNames(UPNPHelper.getDevice(uuid));
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
	 * Returns the upnp instance id of this renderer, if known. Default value is null.
	 *
	 * @return The instance id.
	 */
	public String getInstanceID() {
		return instanceID;
	}

	/**
	 * Sets the upnp instance id of this renderer.
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
		return ! (uuid == null ? hasAssociatedAddress() : UPNPHelper.isActive(uuid, instanceID));
	}

	/**
	 * Returns whether this renderer is currently connected via upnp.
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
		// If we have a uuid look up the upnp device address, which is always
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
	 * Returns whether this renderer provides upnp control services.
	 *
	 * @return Whether controllable.
	 */
	public boolean isUpnpControllable() {
		return UPNPHelper.isUpnpControllable(uuid);
	}

	/**
	 * Returns a upnp player for this renderer if upnp control is supported.
	 *
	 * @return a player or null.
	 */
	public UPNPHelper.Player getPlayer() {
		if (player == null && isUpnpControllable()) {
			player = new UPNPHelper.Player(this);
		}
		return player;
	}

	/**
	 * Sets the upnp player.
	 *
	 * @param the player.
	 */
	public void setPlayer(UPNPHelper.Player player) {
		this.player = player;
	}

	@Override
	public void alert() {
		if (statusIcon != null) {
			statusIcon.icon.setGrey(! isUpnpConnected());
		}
		super.alert();
	}

	public void setStatusIcon(StatusTab.rendererItem item) {
		statusIcon = item;
	}

	/**
	 * RendererName: Determines the name that is displayed in the PMS user
	 * interface when this renderer connects. Default value is "Unknown
	 * renderer".
	 *
	 * @return The renderer name.
	 */
	public String getRendererName() {
		try {
			return UPNPHelper.getFriendlyName(uuid);
		} catch (Exception e) {
			return getString(RENDERER_NAME, Messages.getString("PMS.17"));
		}
	}

	/**
	 * Returns the icon to use for displaying this renderer in PMS as defined
	 * in the renderer configurations. Default value is "unknown.png".
	 *
	 * @return The renderer icon.
	 */
	public String getRendererIcon() {
		String icon = getString(RENDERER_ICON, "unknown.png");
		String deviceIcon = null;
		if (icon.equals("unknown.png")) {
			deviceIcon = UPNPHelper.getDeviceIcon(this, 140);
		}
		return deviceIcon == null ? icon : deviceIcon;
	}

	/**
	 * Returns the the name of an additional HTTP header whose value should
	 * be matched with the additional header search pattern. The header name
	 * must be an exact match (read: the header has to start with the exact
	 * same case sensitive string). The default value is <code>null</code>.
	 *
	 * @return The additional HTTP header name.
	 */
	public String getUserAgentAdditionalHttpHeader() {
		return getString(USER_AGENT_ADDITIONAL_HEADER, null);
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

	public String getUseSameExtension(String file) {
		String s = getString(USE_SAME_EXTENSION, null);

		if (s != null) {
			s = file + "." + s;
		} else {
			s = file;
		}

		return s;
	}

	/**
	 * Returns true if SeekByTime is set to "true" or "exclusive", false otherwise.
	 * Default value is false.
	 *
	 * @return true if the renderer supports seek-by-time, false otherwise.
	 */
	public boolean isSeekByTime() {
		return isSeekByTimeExclusive() || getBoolean(SEEK_BY_TIME, false);
	}

	/**
	 * Returns true if SeekByTime is set to "exclusive", false otherwise.
	 * Default value is false.
	 *
	 * @return true if the renderer supports seek-by-time exclusively
	 * (i.e. not in conjunction with seek-by-byte), false otherwise.
	 */
	public boolean isSeekByTimeExclusive() {
		return getString(SEEK_BY_TIME, "").equalsIgnoreCase("exclusive");
	}

	public boolean isMuxH264MpegTS() {
		boolean muxCompatible = getBoolean(MUX_H264_WITH_MPEGTS, true);
		if (isMediaParserV2()) {
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
		if (isMediaParserV2()) {
			return getFormatConfiguration().isDTSSupported();
		}

		return getBoolean(MUX_DTS_TO_MPEG, false);
	}

	public boolean isWrapDTSIntoPCM() {
		return getBoolean(WRAP_DTS_INTO_PCM, true);
	}

	public boolean isWrapEncodedAudioIntoPCM() {
		return getBoolean(WRAP_ENCODED_AUDIO_INTO_PCM, false);
	}

	public boolean isLPCMPlayable() {
		return isMuxLPCMToMpeg();
	}

	public boolean isMuxLPCMToMpeg() {
		if (isMediaParserV2()) {
			return getFormatConfiguration().isLPCMSupported();
		}

		return getBoolean(MUX_LPCM_TO_MPEG, true);
	}

	public boolean isMuxNonMod4Resolution() {
		return getBoolean(MUX_NON_MOD4_RESOLUTION, false);
	}

	public boolean isMpeg2Supported() {
		if (isMediaParserV2()) {
			return getFormatConfiguration().isMpeg2Supported();
		}

		return isPS3();
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
	 * Returns the maximum bitrate (in megabits-per-second) supported by the media renderer as defined
	 * in the renderer configuration. The default value is <code>null</code>.
	 *
	 * @return The bitrate.
	 */
	// TODO this should return an integer and the units should be bits-per-second
	public String getMaxVideoBitrate() {
		if (PMS.getConfiguration().isAutomaticMaximumBitrate()) {
			try {
				return calculatedSpeed();
			} catch (Exception e) {
				// ignore this
			}
		}
		return getString(MAX_VIDEO_BITRATE, null);
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

		String mpegSettingsArray[] = mpegSettings.split(":");

		String pairArray[];
		StringBuilder returnString = new StringBuilder();
		for (String pair : mpegSettingsArray) {
			pairArray = pair.split("=");
			switch (pairArray[0]) {
				case "keyint":
					returnString.append("-g ").append(pairArray[1]).append(" ");
					break;
				case "vqscale":
					returnString.append("-q:v ").append(pairArray[1]).append(" ");
					break;
				case "vqmin":
					returnString.append("-qmin ").append(pairArray[1]).append(" ");
					break;
				case "vqmax":
					returnString.append("-qmax ").append(pairArray[1]).append(" ");
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
	 * the renderer configuration. The default value 0 means unlimited.
	 *
	 * @return The maximum video width.
	 */
	public int getMaxVideoWidth() {
		// XXX we should require width and height to both be 0 or both be > 0
		return getInt(MAX_VIDEO_WIDTH, 0);
	}

	/**
	 * Returns the maximum video height supported by the renderer as defined
	 * in the renderer configuration. The default value 0 means unlimited.
	 *
	 * @return The maximum video height.
	 */
	public int getMaxVideoHeight() {
		// XXX we should require width and height to both be 0 or both be > 0
		return getInt(MAX_VIDEO_HEIGHT, 0);
	}

	/**
	 * Returns <code>true</code> if the renderer has a maximum supported width
	 * or height, <code>false</code> otherwise.
	 *
	 * @return boolean indicating whether the renderer may need videos to be resized.
	 */
	public boolean isVideoRescale() {
		return getMaxVideoWidth() > 0 && getMaxVideoHeight() > 0;
	}

	public boolean isDLNAOrgPNUsed() {
		return getBoolean(DLNA_ORGPN_USE, true);
	}

	public boolean isAccurateDLNAOrgPN() {
		return getBoolean(ACCURATE_DLNA_ORGPN, false);
	}

	/**
	 * Returns whether or not to use the "res" element instead of the "albumArtURI"
	 * element for thumbnails in DLNA reponses. E.g. Samsung 2012 models do not
	 * recognize the "albumArtURI" element. Default value is <code>false</code>.
	 *
	 * @return True if the "res" element should be used, false otherwise.
	 */
	public boolean getThumbNailAsResource() {
		return getBoolean(THUMBNAIL_AS_RESOURCE, false);
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
	 * custom HTTP header, or "" if no such header exists. Default value is "".
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

	public boolean isMediaParserV2() {
		return getBoolean(MEDIAPARSERV2, false) && LibMediaInfoParser.isValid();
	}

	public boolean isMediaParserV2ThumbnailGeneration() {
		return getBoolean(MEDIAPARSERV2_THUMB, false) && LibMediaInfoParser.isValid();
	}

	public boolean isForceJPGThumbnails() {
		return (getBoolean(FORCE_JPG_THUMBNAILS, false) && LibMediaInfoParser.isValid()) || isBRAVIA();
	}

	public boolean isShowAudioMetadata() {
		return getBoolean(SHOW_AUDIO_METADATA, true);
	}

	public boolean isShowSubMetadata() {
		return getBoolean(SHOW_SUB_METADATA, true);
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
	 * @param mediainfo The {@link DLNAMediaInfo} information parsed from the
	 * 				media file.
	 * @param format The {@link Format} to test compatibility for.
	 * @return True if the renderer natively supports the format, false
	 * 				otherwise.
	 */
	public boolean isCompatible(DLNAMediaInfo mediainfo, Format format) {
		// Use the configured "Supported" lines in the renderer.conf
		// to see if any of them match the MediaInfo library
		if (isMediaParserV2() && mediainfo != null && getFormatConfiguration().match(mediainfo) != null) {
			return true;
		}

		if (format != null) {
			String noTranscode = "";

			if (PMS.getConfiguration() != null) {
				noTranscode = PMS.getConfiguration().getDisableTranscodeForExtensions();
			}

			// Is the format among the ones to be streamed?
			return format.skip(noTranscode, getStreamedExtensions());
		} else {
			// Not natively supported.
			return false;
		}
	}

	public String getCustomFFmpegOptions() {
		return getString(CUSTOM_FFMPEG_OPTIONS, "");
	}

	public boolean isKeepAspectRatio() {
		return getBoolean(KEEP_ASPECT_RATIO, false);
	}

	public boolean isRescaleByRenderer() {
		return getBoolean(RESCALE_BY_RENDERER, true);
	}

	public String getFFmpegVideoFilterOverride() {
		return getString(OVERRIDE_FFMPEG_VF, null);
	}

	public static ArrayList<String> getAllRenderersNames() {
		if (allRenderersNames.isEmpty()) {
			loadRenderersNames();
		}

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
		// Wrap + tuncate
		int len = 0;
		if (line_w > 0 && (name.length() + suffix.length()) > line_w) {
			int suffix_len = dots.length() + suffix.length();
			if (line_h == 1) {
				len = line_w - suffix_len;
			} else {
				// Wrap
				int i = dlna.isFolder() ? 0 : indent;
				String newline = "\n" + (dlna.isFolder() ? "" : inset);
				name = name.substring(0, i + (Character.isWhitespace(name.charAt(i)) ? 1 : 0))
					+ WordUtils.wrap(name.substring(i) + suffix, line_w - i, newline, true);
				len = line_w * line_h;
				if (len != 0 && name.length() > len) {
					len = name.substring(0, name.length() - line_w).lastIndexOf(newline) + newline.length();
					name = name.substring(0, len) + name.substring(len, len + line_w).replace(newline, " ");
					len += (line_w - suffix_len - i);
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
			name = name.replaceAll(s, charMap.get(s));
		}

		return name;
	}

	public boolean isOmitDcDate() {
		return !dc_date;
	}

	public static int getIntAt(String s, String key, int fallback) {
		try {
			return Integer.valueOf((s + " ").split(key)[1].split("\\D")[0]);
		} catch (Exception e) {
			return fallback;
		}
	}

	public String getSupportedSubtitles() {
		return getString(SUPPORTED_SUBTITLES_FORMATS, null);
	}

	public boolean useClosedCaption() {
		return getBoolean(USE_CLOSED_CAPTION, false);
	}

	public ArrayList<String> tags() {
		if (rootFolder != null) {
			return rootFolder.getTags();
		}
		return null;
	}

	public boolean ignoreTranscodeByteRangeRequests() {
		return getBoolean(IGNORE_TRANSCODE_BYTE_RANGE_REQUEST, false);
	}

	private String calculatedSpeed() throws Exception {
		String max = getString(MAX_VIDEO_BITRATE, null);
		for (InetAddress sa : addressAssociation.keySet()) {
			if (addressAssociation.get(sa) == this) {
				Future<Integer> speed = SpeedStats.getInstance().getSpeedInMBitsStored(sa, getRendererName());
				if (max == null) {
					return String.valueOf(speed.get());
				}
				try {
					Integer i = Integer.parseInt(max);
					if (speed.get() > i && i != 0) {
						return max;
					} else {
						return String.valueOf(speed.get());
					}
				} catch (NumberFormatException e) {
					return String.valueOf(speed.get());
				}
			}
		}
		return max;
	}
}
