package net.pms.configuration;

import com.sun.jna.Platform;
import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.LibMediaInfoParser;
import net.pms.dlna.RootFolder;
import net.pms.formats.Format;
import net.pms.network.HTTPResource;
import net.pms.network.SpeedStats;
import net.pms.util.PropertiesUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RendererConfiguration {
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

	// Holds MIME type aliases
	private final Map<String, String> mimes;

	private final Map<String, String> DLNAPN;

	// property values
	private static final String DEPRECATED_MPEGPSAC3 = "MPEGAC3"; // XXX deprecated: old name with missing container
	private static final String LPCM = "LPCM";
	private static final String MP3 = "MP3";
	private static final String MPEGPSAC3 = "MPEGPSAC3";
	private static final String MPEGTSAC3 = "MPEGTSAC3";
	private static final String H264TSAC3 = "H264TSAC3";
	private static final String WAV = "WAV";
	private static final String WMV = "WMV";

	// property names
	private static final String AUDIO = "Audio";
	private static final String AUTO_EXIF_ROTATE = "AutoExifRotate";
	private static final String BYTE_TO_TIMESEEK_REWIND_SECONDS = "ByteToTimeseekRewindSeconds"; // Ditlew
	private static final String CBR_VIDEO_BITRATE = "CBRVideoBitrate"; // Ditlew
	private static final String CHUNKED_TRANSFER = "ChunkedTransfer";
	private static final String CUSTOM_MENCODER_OPTIONS = "CustomMencoderOptions";
	private static final String CUSTOM_MENCODER_MPEG2_OPTIONS = "CustomMencoderQualitySettings"; // TODO (breaking change): value should be CustomMEncoderMPEG2Options
	private static final String DEFAULT_VBV_BUFSIZE = "DefaultVBVBufSize";
	private static final String DLNA_LOCALIZATION_REQUIRED = "DLNALocalizationRequired";
	private static final String DLNA_ORGPN_USE = "DLNAOrgPN";
	private static final String DLNA_PN_CHANGES = "DLNAProfileChanges";
	private static final String DLNA_TREE_HACK = "CreateDLNATreeFaster";
	private static final String FORCE_JPG_THUMBNAILS = "ForceJPGThumbnails"; // Sony devices require JPG thumbnails
	private static final String H264_L41_LIMITED = "H264Level41Limited";
	private static final String IMAGE = "Image";
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
	private static final String USE_SAME_EXTENSION = "UseSameExtension";
	private static final String VIDEO = "Video";
	private static final String WRAP_DTS_INTO_PCM = "WrapDTSIntoPCM";
	private static final String CUSTOM_FFMPEG_OPTIONS = "CustomFFmpegOptions";
	private static final String OVERRIDE_VF = "OverrideVideoFilter";
	private static final String TEXTWRAP = "TextWrap";

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
			for (File f : confs) {
				if (f.getName().endsWith(".conf")) {
					try {
						RendererConfiguration r = new RendererConfiguration(f);
						r.rank = rank++;
						String rendererName = r.getRendererName();
						if (!pmsConfiguration.getIgnoredRenderers().contains(rendererName)) {
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

	protected static File getRenderersDir() {
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

	/**
	 * Associate an IP address with this renderer. The association will
	 * persist between requests, allowing the renderer to be recognized
	 * by its address in later requests.
	 * @param sa The IP address to associate.
	 * @see #getRendererConfigurationBySocketAddress(InetAddress)
	 */
	public void associateIP(InetAddress sa) {
		addressAssociation.put(sa, this);
		SpeedStats.getInstance().getSpeedInMBits(sa, getRendererName());
	}

	public static RendererConfiguration getRendererConfigurationBySocketAddress(InetAddress sa) {
		return addressAssociation.get(sa);
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
					String value = header.substring(header.indexOf(":", r.getUserAgentAdditionalHttpHeader().length()) + 1);
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

	public FormatConfiguration getFormatConfiguration() {
		return formatConfiguration;
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

	RendererConfiguration() throws ConfigurationException {
		this(null);
	}

	public RendererConfiguration(File f) throws ConfigurationException {
		configuration = new PropertiesConfiguration();

		// false: don't log overrides (every renderer conf
		// overrides multiple settings)
		configurationReader = new ConfigurationReader(configuration, false);

		configuration.setListDelimiter((char) 0);

		if (f != null) {
			configuration.load(f);
		}

		mimes = new HashMap<>();
		String mimeTypes = getString(MIME_TYPES_CHANGES, null);

		if (StringUtils.isNotBlank(mimeTypes)) {
			StringTokenizer st = new StringTokenizer(mimeTypes, "|");

			while (st.hasMoreTokens()) {
				String mime_change = st.nextToken().trim();
				int equals = mime_change.indexOf("=");

				if (equals > -1) {
					String old = mime_change.substring(0, equals).trim().toLowerCase();
					String nw = mime_change.substring(equals + 1).trim().toLowerCase();
					mimes.put(old, nw);
				}
			}
		}

		DLNAPN = new HashMap<>();
		String DLNAPNchanges = getString(DLNA_PN_CHANGES, null);

		if (DLNAPNchanges != null) {
			LOGGER.trace("Config DLNAPNchanges: " + DLNAPNchanges);
		}

		if (StringUtils.isNotBlank(DLNAPNchanges)) {
			StringTokenizer st = new StringTokenizer(DLNAPNchanges, "|");
			while (st.hasMoreTokens()) {
				String DLNAPN_change = st.nextToken().trim();
				int equals = DLNAPN_change.indexOf("=");
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
		return isTranscodeToMPEGPSAC3() || isTranscodeToMPEGTSAC3() || isTranscodeToH264TSAC3();
	}

	public boolean isTranscodeToMPEGPSAC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGPSAC3) || videoTranscode.equals(DEPRECATED_MPEGPSAC3);
	}

	public boolean isTranscodeToMPEGTSAC3() {
		return getVideoTranscode().equals(MPEGTSAC3);
	}

	public boolean isTranscodeToH264TSAC3() {
		return getVideoTranscode().equals(H264TSAC3);
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

	/**
	 * Determine the mime type specific for this renderer, given a generic mime
	 * type. This translation takes into account all configured "Supported"
	 * lines and mime type aliases for this renderer.
	 * 
	 * @param matchedMimeType
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
			// Use the supported information in the configuration to determine the mime type.
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToMPEGTSAC3()) {
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
	 * RendererName: Determines the name that is displayed in the PMS user
	 * interface when this renderer connects. Default value is "Unknown
	 * renderer".
	 *
	 * @return The renderer name.
	 */
	public String getRendererName() {
		return getString(RENDERER_NAME, Messages.getString("PMS.17"));
	}

	/**
	 * Returns the icon to use for displaying this renderer in PMS as defined
	 * in the renderer configurations. Default value is "unknown.png".
	 *
	 * @return The renderer icon.
	 */
	public String getRendererIcon() {
		return getString(RENDERER_ICON, "unknown.png");
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

	public boolean isLPCMPlayable() {
		return isMuxLPCMToMpeg();
	}

	public boolean isMuxLPCMToMpeg() {
		if (isMediaParserV2()) {
			return getFormatConfiguration().isLPCMSupported();
		}

		return getBoolean(MUX_LPCM_TO_MPEG, true);
	}

	public boolean isMpeg2Supported() {
		if (isMediaParserV2()) {
			return getFormatConfiguration().isMpeg2Supported();
		}

		return isPS3();
	}

	/**
	 * Returns the codec to use for video transcoding for this renderer as
	 * defined in the renderer configuration. Default value is "MPEGPSAC3".
	 *
	 * @return The codec name.
	 */
	public String getVideoTranscode() {
		return getString(TRANSCODE_VIDEO, MPEGPSAC3);
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
		// FIXME why is this 1920 if the default value is 0 (unlimited)?
		// XXX we should also require width and height to both be 0 or both be > 0
		return getInt(MAX_VIDEO_WIDTH, 1920);
	}

	/**
	 * Returns the maximum video height supported by the renderer as defined
	 * in the renderer configuration. The default value 0 means unlimited.
	 *
	 * @return The maximum video height.
	 */
	public int getMaxVideoHeight() {
		// FIXME why is this 1080 if the default value is 0 (unlimited)?
		// XXX we should also require width and height to both be 0 or both be > 0
		return getInt(MAX_VIDEO_HEIGHT, 1080);
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
	 * requests (e.g. Sony blu-ray players). By default, PMS will send a
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
		return getString(OVERRIDE_VF, null);
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

	public String getTextWrap() {
		return getString(TEXTWRAP, "").toLowerCase();
	}

	protected int line_w = -1, line_h, indent;
	protected String inset;
	protected boolean dc_date = true;

	public String getDcTitle(String name, DLNAResource dlna) {
		if (line_w == -1) {
			// Init text wrap settings
			String s = getTextWrap();
			line_w = getIntAt(s, "width:", 0);
			if (line_w > 0) {
				line_h = getIntAt(s, "height:", 0);
				indent = getIntAt(s, "indent:", 0);
				dc_date = getIntAt(s, "date:", 1) != 0;
				int ws = getIntAt(s, "whitespace:", 9);
				inset = new String(new byte[indent]).replaceAll(".", Character.toString((char) ws));
				LOGGER.debug("{}: TextWrap width:{} height:{} indent:{} whitespace:{} date:{}", getRendererName(), line_w, line_h, indent, ws, dc_date ? "1" : "0");
			}
		}
		// Wrap text if applicable
		if (line_w > 0 && name.length() > line_w) {
			int i = dlna.isFolder() ? 0 : indent;
			String head = name.substring(0, i + (Character.isSpace(name.charAt(i)) ? 1 : 0));
			String tail = name.substring(i);
			return head + WordUtils.wrap(tail, line_w - i, "\n" + (dlna.isFolder() ? "" : inset), true);
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
}
