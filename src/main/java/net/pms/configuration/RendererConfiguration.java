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
package net.pms.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.formats.Format.Identifier;
import net.pms.formats.v2.AudioProperties;
import net.pms.media.audio.MediaAudio;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo.Mode3D;
import net.pms.network.HTTPResource;
import net.pms.parsers.MediaInfoParser;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.util.FileWatcher;
import net.pms.util.SortedHeaderMap;
import net.pms.util.StringUtil;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for settings specific to a Renderer model.
 */
public class RendererConfiguration extends BaseConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(RendererConfiguration.class);

	/**
	 * renderer configuration property keys.
	 */
	private static final String KEY_ACCURATE_DLNA_ORGPN = "AccurateDLNAOrgPN";
	private static final String KEY_AUDIO = "Audio";
	private static final String KEY_AUTO_PLAY_TMO = "AutoPlayTmo";
	private static final String KEY_AVISYNTH_2D_TO_3D = "AviSynth2Dto3D";
	private static final String KEY_BYTE_TO_TIMESEEK_REWIND_SECONDS = "ByteToTimeseekRewindSeconds";
	private static final String KEY_CBR_VIDEO_BITRATE = "CBRVideoBitrate";
	private static final String KEY_CHARMAP = "CharMap";
	private static final String KEY_CHUNKED_TRANSFER = "ChunkedTransfer";
	private static final String KEY_CUSTOM_FFMPEG_OPTIONS = "CustomFFmpegOptions";
	private static final String KEY_CUSTOM_MENCODER_OPTIONS = "CustomMencoderOptions";
	private static final String KEY_CUSTOM_MENCODER_MPEG2_OPTIONS = "CustomMEncoderMPEG2Options";
	private static final String KEY_DEFAULT_VBV_BUFSIZE = "DefaultVBVBufSize";
	protected static final String KEY_DEVICE_ID = "Device";
	private static final String KEY_DISABLE_MENCODER_NOSKIP = "DisableMencoderNoskip";
	private static final String KEY_DLNA_LOCALIZATION_REQUIRED = "DLNALocalizationRequired";
	private static final String KEY_DLNA_ORGPN_USE = "DLNAOrgPN";
	private static final String KEY_DLNA_PN_CHANGES = "DLNAProfileChanges";
	private static final String KEY_DLNA_TREE_HACK = "CreateDLNATreeFaster";
	private static final String KEY_HALVE_BITRATE = "HalveBitrate";
	private static final String KEY_H264_L41_LIMITED = "H264Level41Limited";
	protected static final String KEY_HLS_MULTI_VIDEO_QUALITY = "HlsMultiVideoQuality";
	protected static final String KEY_HLS_VERSION = "HlsVersion";
	private static final String KEY_IGNORE_TRANSCODE_BYTE_RANGE_REQUEST = "IgnoreTranscodeByteRangeRequests";
	private static final String KEY_IMAGE = "Image";
	private static final String KEY_KEEP_ASPECT_RATIO = "KeepAspectRatio";
	private static final String KEY_KEEP_ASPECT_RATIO_TRANSCODING = "KeepAspectRatioTranscoding";
	private static final String KEY_LIMIT_FOLDERS = "LimitFolders";
	private static final String KEY_LOADING_PRIORITY = "LoadingPriority";
	private static final String KEY_MAX_VIDEO_BITRATE = "MaxVideoBitrateMbps";
	private static final String KEY_MAX_VIDEO_HEIGHT = "MaxVideoHeight";
	private static final String KEY_MAX_VIDEO_WIDTH = "MaxVideoWidth";
	private static final String KEY_MAX_VOLUME = "MaxVolume";
	protected static final String KEY_MEDIAPARSERV2 = "MediaInfo";
	protected static final String KEY_MEDIAPARSERV2_THUMB = "MediaParserV2_ThumbnailGeneration";
	private static final String KEY_MIME_TYPES_CHANGES = "MimeTypesChanges";
	private static final String KEY_MUX_DTS_TO_MPEG = "MuxDTSToMpeg";
	private static final String KEY_MUX_H264_WITH_MPEGTS = "MuxH264ToMpegTS";
	private static final String KEY_MUX_LPCM_TO_MPEG = "MuxLPCMToMpeg";
	private static final String KEY_MUX_NON_MOD4_RESOLUTION = "MuxNonMod4Resolution";
	private static final String KEY_OFFER_SUBTITLES_BY_PROTOCOL_INFO = "OfferSubtitlesByProtocolInfo";
	private static final String KEY_OFFER_SUBTITLES_AS_SOURCE = "OfferSubtitlesAsSource";
	private static final String KEY_OUTPUT_3D_FORMAT = "Output3DFormat";
	private static final String KEY_OVERRIDE_FFMPEG_VF = "OverrideFFmpegVideoFilter";
	private static final String KEY_PREPEND_TRACK_NUMBERS = "PrependTrackNumbers";
	private static final String KEY_PUSH_METADATA = "PushMetadata";
	private static final String KEY_REMOVE_TAGS_FROM_SRT_SUBS = "RemoveTagsFromSRTSubtitles";
	private static final String KEY_RENDERER_ICON = "RendererIcon";
	private static final String KEY_RENDERER_ICON_OVERLAYS = "RendererIconOverlays";
	private static final String KEY_RENDERER_NAME = "RendererName";
	private static final String KEY_RESCALE_BY_RENDERER = "RescaleByRenderer";
	private static final String KEY_SEEK_BY_TIME = "SeekByTime";
	private static final String KEY_SEND_DATE_METADATA = "SendDateMetadata";
	private static final String KEY_SEND_DATE_METADATA_YEAR_FOR_AUDIO_TAGS = "SendDateMetadataYearForAudioTags";
	private static final String KEY_SEND_DLNA_ORG_FLAGS = "SendDLNAOrgFlags";
	private static final String KEY_SEND_FOLDER_THUMBNAILS = "SendFolderThumbnails";
	private static final String KEY_SHOW_AUDIO_METADATA = "ShowAudioMetadata";
	private static final String KEY_SHOW_DVD_TITLE_DURATION = "ShowDVDTitleDuration";
	private static final String KEY_SHOW_SUB_METADATA = "ShowSubMetadata";
	private static final String KEY_STREAM_EXT = "StreamExtensions";
	private static final String KEY_STREAM_SUBS_FOR_TRANSCODED_VIDEO = "StreamSubsForTranscodedVideo";
	private static final String KEY_SUBTITLE_HTTP_HEADER = "SubtitleHttpHeader";
	protected static final String KEY_SUPPORTED = "Supported";
	private static final String KEY_SUPPORTED_VIDEO_BIT_DEPTHS = "SupportedVideoBitDepths";
	private static final String KEY_SUPPORTED_EXTERNAL_SUBTITLES_FORMATS = "SupportedExternalSubtitlesFormats";
	private static final String KEY_SUPPORTED_INTERNAL_SUBTITLES_FORMATS = "SupportedInternalSubtitlesFormats";
	private static final String KEY_TEXTWRAP = "TextWrap";
	private static final String KEY_THUMBNAIL_PADDING = "ThumbnailPadding";
	private static final String KEY_THUMBNAILS = "Thumbnails";
	protected static final String KEY_TRANSCODE_AUDIO = "TranscodeAudio";
	private static final String KEY_TRANSCODE_AUDIO_441KHZ = "TranscodeAudioTo441kHz";
	private static final String KEY_TRANSCODE_EXT = "TranscodeExtensions";
	private static final String KEY_TRANSCODE_FAST_START = "TranscodeFastStart";
	protected static final String KEY_TRANSCODE_VIDEO = "TranscodeVideo";
	private static final String KEY_TRANSCODED_SIZE = "TranscodedVideoFileSize";
	private static final String KEY_TRANSCODED_VIDEO_AUDIO_SAMPLE_RATE = "TranscodedVideoAudioSampleRate";
	private static final String KEY_UPNP_DETAILS = "UpnpDetailsSearch";
	protected static final String KEY_UPNP_ALLOW = "UpnpAllow";
	private static final String KEY_USER_AGENT = "UserAgentSearch";
	private static final String KEY_USER_AGENT_ADDITIONAL_HEADER = "UserAgentAdditionalHeader";
	private static final String KEY_USER_AGENT_ADDITIONAL_SEARCH = "UserAgentAdditionalHeaderSearch";
	private static final String KEY_USE_CLOSED_CAPTION = "UseClosedCaption";
	private static final String KEY_USE_SAME_EXTENSION = "UseSameExtension";
	private static final String KEY_VIDEO = "Video";
	private static final String KEY_WRAP_DTS_INTO_PCM = "WrapDTSIntoPCM";
	private static final String KEY_WRAP_ENCODED_AUDIO_INTO_PCM = "WrapEncodedAudioIntoPCM";
	private static final String KEY_DISABLE_UMS_RESUME = "DisableUmsResume";
	private static final String KEY_UPNP_ENABLE_SEARCHCAPS = "UpnpSearchCapsEnabled";

	/**
	 * audio transcoding options.
	 */
	private static final String TRANSCODE_TO_LPCM = "LPCM";
	protected static final String TRANSCODE_TO_MP3 = "MP3";
	private static final String TRANSCODE_TO_WAV = "WAV";
	private static final String TRANSCODE_TO_WMV = "WMV";

	/**
	 * video transcoding options.
	 */
	private static final String MPEGTSH264AAC = "MPEGTS-H264-AAC";
	private static final String MPEGTSH264AC3 = "MPEGTS-H264-AC3";
	private static final String MPEGTSH265AAC = "MPEGTS-H265-AAC";
	private static final String MPEGTSH265AC3 = "MPEGTS-H265-AC3";
	private static final String MPEGPSMPEG2AC3 = "MPEGPS-MPEG2-AC3";
	private static final String MPEGTSMPEG2AC3 = "MPEGTS-MPEG2-AC3";
	//HLS (HTTP Live Streaming) encapsulated by MPEG-2 Transport Stream
	protected static final String HLSMPEGTSH264AAC = "HLS-MPEGTS-H264-AAC";
	private static final String HLSMPEGTSH264AC3 = "HLS-MPEGTS-H264-AC3";
	//private static final String HLSMPEGTSH264MP3 = "HLS-MPEGTS-H264-MP3";
	//private static final String HLSMPEGTSH264EAC3 = "HLS-MPEGTS-H264-EAC3";
	//HLS (HTTP Live Streaming) encapsulated by MPEG-4_Part_14
	//private static final String HLSMPEG4H264AAC = "HLS-MPEG4-H264-AAC";
	//private static final String HLSMPEG4H264AC3 = "HLS-MPEG4-H264-AC3";
	//private static final String HLSMPEG4H264MP3 = "HLS-MPEG4-H264-MP3";
	//private static final String HLSMPEG4H264EAC3 = "HLS-MPEG4-H264-EAC3";

	public static final File NOFILE = new File("NOFILE");
	public static final String UNKNOWN_ICON = "unknown.png";

	protected Matcher sortedHeaderMatcher;

	protected UmsConfiguration umsConfiguration = PMS.getConfiguration();
	protected boolean loaded = false;

	private boolean fileless = false;

	private File file;
	private FormatConfiguration formatConfiguration;
	private List<String> identifiers = null;

	// Holds MIME type aliases
	private Map<String, String> mimes;

	private Map<String, String> charMap;
	private Map<String, String> dLNAPN;

	// TextWrap parameters
	private int lineWidth;
	private int lineHeight;
	private int indent;
	private String inset;
	private String dots;

	public RendererConfiguration() {
		super(false);
	}

	public RendererConfiguration(File f) throws ConfigurationException {
		// false: don't log overrides.
		// (every renderer conf overrides multiple settings)
		super(false);
		umsConfiguration = PMS.getConfiguration();

		init(f);
	}

	public boolean isUpnpSearchCapsEnabled() {
		return getBoolean(KEY_UPNP_ENABLE_SEARCHCAPS, true);
	}

	public boolean isLoaded() {
		return loaded;
	}

	public FormatConfiguration getFormatConfiguration() {
		return formatConfiguration;
	}

	public UmsConfiguration getUmsConfiguration() {
		return umsConfiguration;
	}

	public File getFile() {
		return file;
	}

	public boolean isFileless() {
		return fileless;
	}

	public final void setFileless(boolean b) {
		fileless = b;
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

	/**
	 * @return whether this renderer is an Samsung device
	 */
	public boolean isSamsung() {
		return getConfName().toUpperCase().contains("SAMSUNG");
	}

	public int getByteToTimeseekRewindSeconds() {
		return getInt(KEY_BYTE_TO_TIMESEEK_REWIND_SECONDS, 0);
	}

	public int getCBRVideoBitrate() {
		return getInt(KEY_CBR_VIDEO_BITRATE, 0);
	}

	public boolean isShowDVDTitleDuration() {
		return getBoolean(KEY_SHOW_DVD_TITLE_DURATION, false);
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
				FileWatcher.add(new FileWatcher.Watch(getFile().getPath(), RELOADER, this));
			}
			return true;
		}
		return false;
	}

	public final void init(File f) throws ConfigurationException {
		if (!loaded) {
			configuration.clear();
			loaded = load(f);
		}

		mimes = new HashMap<>();
		String mimeTypes = getString(KEY_MIME_TYPES_CHANGES, "");

		if (StringUtils.isNotBlank(mimeTypes)) {
			StringTokenizer st = new StringTokenizer(mimeTypes, "|");

			while (st.hasMoreTokens()) {
				String mimeChange = st.nextToken().trim();
				int equals = mimeChange.indexOf('=');

				if (equals > -1) {
					String old = mimeChange.substring(0, equals).trim().toLowerCase();
					String nw = mimeChange.substring(equals + 1).trim().toLowerCase();
					mimes.put(old, nw);
				}
			}
		}

		String s = getString(KEY_TEXTWRAP, "").toLowerCase();
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
		String ch = getString(KEY_CHARMAP, null);
		if (StringUtils.isNotBlank(ch)) {
			StringTokenizer st = new StringTokenizer(ch, " ");
			String org = "";

			while (st.hasMoreTokens()) {
				String tok = st.nextToken().trim();
				if (StringUtils.isBlank(tok)) {
					continue;
				}
				tok = tok.replace("###0", " ").replace("###n", "\n").replace("###r", "\r");
				if (StringUtils.isBlank(org)) {
					org = tok;
				} else {
					charMap.put(org, tok);
					org = "";
				}
			}
		}

		dLNAPN = new HashMap<>();
		String dLNAPNchanges = getString(KEY_DLNA_PN_CHANGES, "");

		if (StringUtils.isNotBlank(dLNAPNchanges)) {
			LOGGER.trace("Config dLNAPNchanges: " + dLNAPNchanges);
			StringTokenizer st = new StringTokenizer(dLNAPNchanges, "|");
			while (st.hasMoreTokens()) {
				String dLNAPNChange = st.nextToken().trim();
				int equals = dLNAPNChange.indexOf('=');
				if (equals > -1) {
					String old = dLNAPNChange.substring(0, equals).trim().toUpperCase();
					String nw = dLNAPNChange.substring(equals + 1).trim().toUpperCase();
					dLNAPN.put(old, nw);
				}
			}
		}

		if (f == null) {
			// The default renderer supports everything!
			configuration.addProperty(KEY_MEDIAPARSERV2, true);
			configuration.addProperty(KEY_MEDIAPARSERV2_THUMB, true);
			configuration.addProperty(KEY_SUPPORTED, "f:.+");
		}

		if (isUseMediaInfo()) {
			formatConfiguration = new FormatConfiguration(configuration.getList(KEY_SUPPORTED));
		}
	}

	public void reset() {
		File f = getFile();
		try {
			LOGGER.info("Reloading renderer configuration: {}", f);
			loaded = false;
			init(f);
		} catch (ConfigurationException e) {
			LOGGER.debug("Error reloading renderer configuration {}: {}", f, e);
		}
	}

	public void resetLoaded() {
		loaded = false;
	}

	public String getDLNAPN(String old) {
		if (dLNAPN.containsKey(old)) {
			return dLNAPN.get(old);
		}

		return old;
	}

	public boolean supportsFormat(Format f) {
		return switch (f.getType()) {
			case Format.VIDEO -> isVideoSupported();
			case Format.AUDIO -> isAudioSupported();
			case Format.IMAGE -> isImageSupported();
			default -> false;
		};
	}

	public boolean isVideoSupported() {
		return getBoolean(KEY_VIDEO, true);
	}

	public boolean isAudioSupported() {
		return getBoolean(KEY_AUDIO, true);
	}

	public boolean isImageSupported() {
		return getBoolean(KEY_IMAGE, true);
	}

	public boolean isTranscodeToWMV() {
		return getVideoTranscode().equals(TRANSCODE_TO_WMV);
	}

	public boolean isTranscodeToMPEGPSMPEG2AC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGPSMPEG2AC3);
	}

	public boolean isTranscodeToMPEGTSMPEG2AC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGTSMPEG2AC3);
	}

	public boolean isTranscodeToMPEGTSH264AC3() {
		String videoTranscode = getVideoTranscode();
		return videoTranscode.equals(MPEGTSH264AC3);
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

	public boolean isTranscodeToHLSMPEGTSH264AAC() {
		return getVideoTranscode().equals(HLSMPEGTSH264AAC);
	}

	public boolean isTranscodeToHLSMPEGTSH264AC3() {
		return getVideoTranscode().equals(HLSMPEGTSH264AC3);
	}

	/**
	 * @return whether to use the HLS format for transcoded video
	 */
	public boolean isTranscodeToHLS() {
		return isTranscodeToHLSMPEGTSH264AAC() || isTranscodeToHLSMPEGTSH264AC3();
	}

	/**
	 * @return whether to use the AC-3 audio codec for transcoded video
	 */
	public boolean isTranscodeToAC3() {
		return isTranscodeToMPEGPSMPEG2AC3() || isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGTSH264AC3() || isTranscodeToMPEGTSH265AC3() || isTranscodeToHLSMPEGTSH264AC3();
	}

	/**
	 * @return whether to use the AAC audio codec for transcoded video
	 */
	public boolean isTranscodeToAAC() {
		return isTranscodeToMPEGTSH264AAC() || isTranscodeToMPEGTSH265AAC() || isTranscodeToHLSMPEGTSH264AAC();
	}

	/**
	 * @return whether to use the H.264 video codec for transcoded video
	 */
	public boolean isTranscodeToH264() {
		return isTranscodeToMPEGTSH264AAC() || isTranscodeToMPEGTSH264AC3() || isTranscodeToHLSMPEGTSH264AAC() || isTranscodeToHLSMPEGTSH264AC3();
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
		return isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGTSH264AC3() || isTranscodeToMPEGTSH264AAC() || isTranscodeToMPEGTSH265AC3() || isTranscodeToMPEGTSH265AAC() || isTranscodeToHLSMPEGTSH264AAC() || isTranscodeToHLSMPEGTSH264AC3();
	}

	/**
	 * @return whether to use the MPEG-TS container for transcoded video
	 */
	private String getTranscodingContainer() {
		String transcodingContainer = FormatConfiguration.MPEGPS;
		if (isTranscodeToMPEGTS()) {
			transcodingContainer = FormatConfiguration.MPEGTS;
		}
		return transcodingContainer;
	}

	/**
	 * @return whether to use the MPEG-2 video codec for transcoded video
	 */
	public boolean isTranscodeToMPEG2() {
		return isTranscodeToMPEGTSMPEG2AC3() || isTranscodeToMPEGPSMPEG2AC3();
	}

	public boolean isTranscodeToMP3() {
		return getAudioTranscode().equals(TRANSCODE_TO_MP3);
	}

	public boolean isTranscodeToLPCM() {
		return getAudioTranscode().equals(TRANSCODE_TO_LPCM);
	}

	public boolean isTranscodeToWAV() {
		return getAudioTranscode().equals(TRANSCODE_TO_WAV);
	}

	public boolean isTranscodeAudioTo441() {
		return getBoolean(KEY_TRANSCODE_AUDIO_441KHZ, false);
	}

	/**
	 * @return whether to transcode H.264 video if it exceeds level 4.1
	 */
	public boolean isH264Level41Limited() {
		return getBoolean(KEY_H264_L41_LIMITED, true);
	}

	public boolean isTranscodeFastStart() {
		return getBoolean(KEY_TRANSCODE_FAST_START, false);
	}

	public boolean isDLNALocalizationRequired() {
		return getBoolean(KEY_DLNA_LOCALIZATION_REQUIRED, false);
	}

	public boolean isDisableMencoderNoskip() {
		return getBoolean(KEY_DISABLE_MENCODER_NOSKIP, false);
	}

	public boolean disableUmsResume() {
		return getBoolean(KEY_DISABLE_UMS_RESUME, false);
	}

	/**
	 * This is used to determine whether transcoding engines can safely remux
	 * video streams into the transcoding container instead of re-encoding
	 * them to the same format.
	 * There is a lot of logic necessary to determine that and this is only
	 * one step in the process.
	 *
	 * @param media
	 * @return whether this renderer supports the video stream type of this
	 *         resource inside the container it wants for transcoding.
	 */
	public boolean isVideoStreamTypeSupportedInTranscodingContainer(MediaInfo media) {
		return getFormatConfiguration().getMatchedMIMEtype(getTranscodingContainer(), media.getCodecV(), null) != null;
	}

	/**
	 * This is used to determine whether transcoding engines can safely remux
	 * audio streams into the transcoding container instead of re-encoding
	 * them to the same format.
	 * There is a lot of logic necessary to determine that and this is only
	 * one step in the process.
	 *
	 * @param audio
	 * @return whether this renderer supports the audio stream type of this
	 *         resource inside the container it wants for transcoding.
	 */
	public boolean isAudioStreamTypeSupportedInTranscodingContainer(MediaAudio audio) {
		return getFormatConfiguration().getMatchedMIMEtype(getTranscodingContainer(), null, audio.getCodecA()) != null;
	}

	/**
	 * Determine the mime type specific for this renderer, given a generic mime
	 * type by resource. This translation takes into account all configured "Supported"
	 * lines and mime type aliases for this renderer.
	 *
	 * @param resource The resource with the generic mime type.
	 * @return The renderer specific mime type  for given resource. If the generic mime
	 * type given by resource is <code>null</code> this method returns <code>null</code>.
	 */
	public String getMimeType(DLNAResource resource) {
		String mimeType = resource.mimeType();
		if (mimeType == null) {
			return null;
		}

		String matchedMimeType = null;
		MediaInfo media = resource.getMedia();

		if (isUseMediaInfo()) {
			// Use the supported information in the configuration to determine the transcoding mime type.
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToHLSMPEGTSH264AC3()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS_HLS, FormatConfiguration.H264, FormatConfiguration.AC3);
				} else if (isTranscodeToHLSMPEGTSH264AAC()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS_HLS, FormatConfiguration.H264, FormatConfiguration.AAC_LC);
				} else if (isTranscodeToMPEGTSH264AC3()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AC3);
				} else if (isTranscodeToMPEGTSH264AAC()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS, FormatConfiguration.H264, FormatConfiguration.AAC_LC);
				} else if (isTranscodeToMPEGTSH265AC3()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS, FormatConfiguration.H265, FormatConfiguration.AC3);
				} else if (isTranscodeToMPEGTSH265AAC()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS, FormatConfiguration.H265, FormatConfiguration.AAC_LC);
				} else if (isTranscodeToMPEGTSMPEG2AC3()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS, FormatConfiguration.MPEG2, FormatConfiguration.AC3);
				} else if (isTranscodeToWMV()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.WMV, FormatConfiguration.WMV, FormatConfiguration.WMA);
				} else {
					// Default video transcoding mime type
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGPS, FormatConfiguration.MPEG2, FormatConfiguration.AC3);
				}
			} else if (HTTPResource.AUDIO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToWAV()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.WAV, null, null);
				} else if (isTranscodeToMP3()) {
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MP3, null, null);
				} else {
					// Default audio transcoding mime type
					matchedMimeType = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.LPCM, null, null);

					if (matchedMimeType != null) {
						if (umsConfiguration.isAudioResample()) {
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
			// No match found in the renderer config, try our defaults
			if (HTTPResource.VIDEO_TRANSCODE.equals(mimeType)) {
				if (isTranscodeToWMV()) {
					matchedMimeType = HTTPResource.WMV_TYPEMIME;
				} else if (isTranscodeToHLS()) {
					matchedMimeType = HTTPResource.HLS_TYPEMIME;
				} else if (isTranscodeToMPEGTS()) {
					// Default video transcoding mime type
					matchedMimeType = HTTPResource.MPEGTS_TYPEMIME;
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

					if (umsConfiguration.isAudioResample()) {
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
		return getString(KEY_USER_AGENT, "");
	}

	/**
	 * Returns the unique UPnP details of this renderer as defined in the
	 * renderer configuration. Default value is "".
	 *
	 * @return The detail string.
	 */
	public String getUpnpDetailsString() {
		return getString(KEY_UPNP_DETAILS, "");
	}

	public String getUpnpAllow() {
		return getString(KEY_UPNP_ALLOW, "true");
	}

	public void setUpnpAllow(String value) {
		configuration.setProperty(KEY_UPNP_ALLOW, value);
	}

	/**
	 * Returns the maximum bitrate (in megabits-per-second) supported by the
	 * media renderer as defined in the renderer configuration. The default
	 * value is 0 (unlimited).
	 *
	 * @return The bitrate.
	 */
	public int getMaxVideoBitrate() {
		return getInt(KEY_MAX_VIDEO_BITRATE, 0);
	}

	/**
	 * RendererName: Determines the name that is displayed in the UMS user
	 * interface when this renderer connects.
	 * Default value is "Unknown renderer".
	 *
	 * @return The renderer name.
	 */
	public String getRendererName() {
		return getConfName();
	}

	public String getSimpleName() {
		return StringUtils.substringBefore(getRendererName(), "(").trim();
	}

	public String getConfName() {
		return getString(KEY_RENDERER_NAME, Messages.getString("UnknownRenderer"));
	}

	/**
	 * Returns the icon to use for displaying this renderer in PMS as defined
	 * in the renderer configurations.
	 * Default value is UNKNOWN_ICON.
	 *
	 * @return The renderer icon.
	 */
	public String getRendererIcon() {
		return getString(KEY_RENDERER_ICON, UNKNOWN_ICON);
	}

	/**
	 * Returns the the text/s that is displayed on the renderer icon
	 * as defined in the renderer configurations. Default value is "".
	 *
	 * @return The renderer text.
	 */
	public String getRendererIconOverlays() {
		return getString(KEY_RENDERER_ICON_OVERLAYS, "");
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
		return getString(KEY_USER_AGENT_ADDITIONAL_HEADER, "");
	}

	/**
	 * Returns the pattern to match additional headers to as defined in the
	 * renderer configuration. Default value is "".
	 *
	 * @return The User-Agent search pattern.
	 */
	public String getUserAgentAdditionalHttpHeaderSearch() {
		return getString(KEY_USER_AGENT_ADDITIONAL_SEARCH, "");
	}

	/**
	 * May append a custom file extension to the file path.
	 * Returns the original path if the renderer didn't define an extension.
	 *
	 * @param file the original file path
	 * @return
	 */
	public String getUseSameExtension(String file) {
		String extension = getString(KEY_USE_SAME_EXTENSION, "");
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
		return isSeekByTimeExclusive() || getString(KEY_SEEK_BY_TIME, "false").equalsIgnoreCase("true");
	}

	/**
	 * Returns true if SeekByTime is set to "exclusive", false otherwise.
	 * Default value is false.
	 *
	 * @return true if the renderer supports seek-by-time exclusively
	 * (i.e. not in conjunction with seek-by-byte), false otherwise.
	 */
	public boolean isSeekByTimeExclusive() {
		return getString(KEY_SEEK_BY_TIME, "false").equalsIgnoreCase("exclusive");
	}

	/**
	 * @return {boolean} whether the renderer supports H.264 inside MPEG-TS
	 */
	public boolean isMuxH264MpegTS() {
		boolean muxCompatible = getBoolean(KEY_MUX_H264_WITH_MPEGTS, true);
		if (isUseMediaInfo()) {
			muxCompatible = getFormatConfiguration().getMatchedMIMEtype(FormatConfiguration.MPEGTS, FormatConfiguration.H264, null) != null;
		}

		if (!PlatformUtils.INSTANCE.isTsMuxeRCompatible()) {
			muxCompatible = false;
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

		return getBoolean(KEY_MUX_DTS_TO_MPEG, false);
	}

	public boolean isWrapDTSIntoPCM() {
		return getBoolean(KEY_WRAP_DTS_INTO_PCM, false);
	}

	public boolean isWrapEncodedAudioIntoPCM() {
		return getBoolean(KEY_WRAP_ENCODED_AUDIO_INTO_PCM, false);
	}

	public boolean isLPCMPlayable() {
		return isMuxLPCMToMpeg();
	}

	public boolean isMuxLPCMToMpeg() {
		if (isUseMediaInfo()) {
			return getFormatConfiguration().isLPCMSupported();
		}

		return getBoolean(KEY_MUX_LPCM_TO_MPEG, true);
	}

	public boolean isMuxNonMod4Resolution() {
		return getBoolean(KEY_MUX_NON_MOD4_RESOLUTION, false);
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
		return getBoolean(KEY_PUSH_METADATA, true);
	}

	/**
	 * Returns the codec to use for video transcoding for this renderer as
	 * defined in the renderer configuration. Default value is "MPEGPSMPEG2AC3".
	 *
	 * @return The codec name.
	 */
	public String getVideoTranscode() {
		return getString(KEY_TRANSCODE_VIDEO, MPEGPSMPEG2AC3);
	}

	/**
	 * Returns the codec to use for audio transcoding for this renderer as
	 * defined in the renderer configuration. Default value is "LPCM".
	 *
	 * @return The codec name.
	 */
	public String getAudioTranscode() {
		return getString(KEY_TRANSCODE_AUDIO, TRANSCODE_TO_LPCM);
	}

	/**
	 * Returns whether or not to use the default DVD buffer size for this
	 * renderer as defined in the renderer configuration. Default is false.
	 *
	 * @return True if the default size should be used.
	 */
	public boolean isDefaultVBVSize() {
		return getBoolean(KEY_DEFAULT_VBV_BUFSIZE, false);
	}

	/**
	 * This was originally added for the PS3 after it was observed to need
	 * a video whose maximum bitrate was under half of the network maximum.
	 *
	 * @return whether to set the maximum bitrate to half of the network max
	 */
	public boolean isHalveBitrate() {
		return getBoolean(KEY_HALVE_BITRATE, false);
	}

	/**
	 * Returns the override settings for MEncoder quality settings as
	 * defined in the renderer configuration. The default value is "".
	 *
	 * @return The MEncoder quality settings.
	 */
	public String getCustomMEncoderMPEG2Options() {
		String o = getString(KEY_CUSTOM_MENCODER_MPEG2_OPTIONS, "");
		// Backward compatibility
		if (StringUtils.isBlank(o)) {
			o = getString("CustomMencoderQualitySettings", "");
		}
		return o;
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
	 * Returns the override settings for MEncoder custom options in PMS as
	 * defined in the renderer configuration. The default value is "".
	 *
	 * @return The MEncoder custom options.
	 */
	public String getCustomMencoderOptions() {
		return getString(KEY_CUSTOM_MENCODER_OPTIONS, "");
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
		return getInt(KEY_MAX_VIDEO_WIDTH, 1920);
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
		return getInt(KEY_MAX_VIDEO_HEIGHT, 1080);
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
			LOGGER.trace("Resolution {}x{} is too high for this renderer, which supports up to {}x{}", width, height, getMaxVideoWidth(), getMaxVideoHeight());
			return false;
		}

		// Check if the resolution is too low
		if (!isRescaleByRenderer() && getMaxVideoWidth() < 720) {
			LOGGER.trace("Resolution {}x{} is too low for this renderer");
			return false;
		}

		return true;
	}

	public boolean isDLNAOrgPNUsed() {
		return getBoolean(KEY_DLNA_ORGPN_USE, true);
	}

	public boolean isSendDLNAOrgFlags() {
		return getBoolean(KEY_SEND_DLNA_ORG_FLAGS, true);
	}

	public boolean isAccurateDLNAOrgPN() {
		return getBoolean(KEY_ACCURATE_DLNA_ORGPN, false);
	}

	/**
	 * Returns the comma separated list of file extensions that are forced to
	 * be transcoded and never streamed, as defined in the renderer
	 * configuration. Default value is "".
	 *
	 * @return The file extensions.
	 */
	public String getTranscodedExtensions() {
		return getString(KEY_TRANSCODE_EXT, "");
	}

	/**
	 * Returns the comma separated list of file extensions that are forced to
	 * be streamed and never transcoded, as defined in the renderer
	 * configuration. Default value is "".
	 *
	 * @return The file extensions.
	 */
	public String getStreamedExtensions() {
		return getString(KEY_STREAM_EXT, "");
	}

	/**
	 * Returns the size to report back to the renderer when transcoding media
	 * as defined in the renderer configuration. Default value is 0.
	 *
	 * @return The size to report.
	 */
	public long getTranscodedSize() {
		return getLong(KEY_TRANSCODED_SIZE, 0);
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
		return getString(KEY_SUBTITLE_HTTP_HEADER, "");
	}

	@Override
	public String toString() {
		return getRendererName();
	}

	/**
	 * @return whether to use MediaInfo
	 */
	public boolean isUseMediaInfo() {
		return getBoolean(KEY_MEDIAPARSERV2, true) && MediaInfoParser.isValid();
	}

	public boolean isMediaInfoThumbnailGeneration() {
		return getBoolean(KEY_MEDIAPARSERV2_THUMB, false) && MediaInfoParser.isValid();
	}

	public boolean isShowAudioMetadata() {
		return getBoolean(KEY_SHOW_AUDIO_METADATA, true);
	}

	public boolean isShowSubMetadata() {
		return getBoolean(KEY_SHOW_SUB_METADATA, true);
	}

	/**
	 * Whether to send the last modified date metadata for files and
	 * folders, which can take up screen space on some renderers.
	 *
	 * @return whether to send the metadata
	 */
	public boolean isSendDateMetadata() {
		return getBoolean(KEY_SEND_DATE_METADATA, true);
	}

	/**
	 * Note: This can break browsing on some renderers, even though it is valid.
	 *
	 * @return whether to send the release year as the `dc:date` tag for audio tracks
	 */
	public boolean isSendDateMetadataYearForAudioTags() {
		return getBoolean(KEY_SEND_DATE_METADATA_YEAR_FOR_AUDIO_TAGS, false);
	}

	/**
	 * Whether to send folder thumbnails.
	 *
	 * @return whether to send folder thumbnails
	 */
	public boolean isSendFolderThumbnails() {
		return getBoolean(KEY_SEND_FOLDER_THUMBNAILS, true);
	}

	public boolean isDLNATreeHack() {
		return getBoolean(KEY_DLNA_TREE_HACK, false) && MediaInfoParser.isValid();
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
		return getBoolean(KEY_CHUNKED_TRANSFER, false);
	}

	/**
	 * Returns whether or not the renderer can handle the given format
	 * natively, based on its configuration in the renderer.conf. If it can
	 * handle a format natively, content can be streamed to the renderer. If
	 * not, content should be transcoded before sending it to the renderer.
	 *
	 * @param dlna The {@link DLNAResource} information parsed from the
	 * 				media file.
	 * @param format The {@link Format} to test compatibility for.
	 * @param configuration The {@link UmsConfiguration} to use while evaluating compatibility
	 * @return True if the renderer natively supports the format, false
	 * 				otherwise.
	 */
	public boolean isCompatible(DLNAResource dlna, Format format, UmsConfiguration configuration) {
		MediaInfo mediaInfo;
		if (dlna != null) {
			mediaInfo = dlna.getMedia();
		} else {
			mediaInfo = null;
		}

		if (configuration == null) {
			configuration = umsConfiguration;
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
		if (isUseMediaInfo() && mediaInfo != null && getFormatConfiguration().getMatchedMIMEtype(dlna, this) != null) {
			return true;
		}

		return format != null && format.skip(getStreamedExtensions());
	}

	/**
	 * Returns whether or not the renderer can handle the given format
	 * natively, based on its configuration in the renderer.conf. If it can
	 * handle a format natively, content can be streamed to the renderer. If
	 * not, content should be transcoded before sending it to the renderer.
	 *
	 * @param dlna The {@link DLNAResource} information parsed from the
	 * 				media file.
	 * @param format The {@link Format} to test compatibility for.
	 * @return True if the renderer natively supports the format, false
	 * 				otherwise.
	 */
	public boolean isCompatible(DLNAResource dlna, Format format) {
		return isCompatible(dlna, format, null);
	}

	public int getAutoPlayTmo() {
		return getInt(KEY_AUTO_PLAY_TMO, 5000);
	}

	public String getCustomFFmpegOptions() {
		return getString(KEY_CUSTOM_FFMPEG_OPTIONS, "");
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
		return getBoolean(KEY_KEEP_ASPECT_RATIO, false);
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
		return getBoolean(KEY_KEEP_ASPECT_RATIO_TRANSCODING, false);
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
		return getBoolean(KEY_RESCALE_BY_RENDERER, true);
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
		return getBoolean(KEY_PREPEND_TRACK_NUMBERS, false);
	}

	public String getFFmpegVideoFilterOverride() {
		return getString(KEY_OVERRIDE_FFMPEG_VF, "");
	}

	public int getTranscodedVideoAudioSampleRate() {
		return getInt(KEY_TRANSCODED_VIDEO_AUDIO_SAMPLE_RATE, 48000);
	}

	public boolean isLimitFolders() {
		return getBoolean(KEY_LIMIT_FOLDERS, true);
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
		if (suffix == null) {
			suffix = "";
		}

		if (lineWidth > 0 && (name.length() + suffix.length()) > lineWidth) {
			int suffixLength = dots.length() + suffix.length();
			if (lineHeight == 1) {
				len = lineWidth - suffixLength;
			} else {
				// Wrap
				int i = dlna.isFolder() ? 0 : indent;
				String newline = "\n" + (dlna.isFolder() ? "" : inset);
				name = name.substring(0, i + (i < name.length() && Character.isWhitespace(name.charAt(i)) ? 1 : 0)) +
					WordUtils.wrap(name.substring(i) + suffix, lineWidth - i, newline, true);
				len = lineWidth * lineHeight;
				if (len != 0 && name.length() > len) {
					len = name.substring(0, name.length() - lineWidth).lastIndexOf(newline) + newline.length();
					name = name.substring(0, len) + name.substring(len, len + lineWidth).replace(newline, " ");
					len += (lineWidth - suffixLength - i);
				} else {
					len = -1; // done
				}
			}
			if (len > 0) {
				// Truncate
				name = name.substring(0, len).trim() + dots;
			}
		}
		if (len > -1 && StringUtils.isNotBlank(suffix)) {
			name += " " + suffix;
		}

		// Substitute
		for (Entry<String, String> entry : charMap.entrySet()) {
			String repl = entry.getValue().replace("###e", "");
			name = name.replaceAll(entry.getKey(), repl);
		}

		return name;
	}

	/**
	 * List of the renderer supported external subtitles formats
	 * for streaming together with streaming (not transcoded) video, for all
	 * file types.
	 *
	 * @return A comma-separated list of supported text-based external subtitles formats.
	 */
	public String getExternalSubtitlesFormatsSupportedForAllFiletypes() {
		return getString(KEY_SUPPORTED_EXTERNAL_SUBTITLES_FORMATS, "");
	}

	/**
	 * List of the renderer supported embedded subtitles formats.
	 *
	 * @return A comma-separated list of supported embedded subtitles formats.
	 */
	public String getSupportedEmbeddedSubtitles() {
		return getString(KEY_SUPPORTED_INTERNAL_SUBTITLES_FORMATS, "");
	}

	public boolean useClosedCaption() {
		return getBoolean(KEY_USE_CLOSED_CAPTION, false);
	}

	public boolean offerSubtitlesAsResource() {
		return getBoolean(KEY_OFFER_SUBTITLES_AS_SOURCE, true);
	}

	public boolean offerSubtitlesByProtocolInfo() {
		return getBoolean(KEY_OFFER_SUBTITLES_BY_PROTOCOL_INFO, true);
	}

	/**
	 * Note: This can return false even when the renderer config has defined
	 * external subtitles support for individual filetypes.
	 *
	 * @return whether this renderer supports streaming external subtitles
	 * for all video formats.
	 */
	public boolean isSubtitlesStreamingSupportedForAllFiletypes() {
		return StringUtils.isNotBlank(getExternalSubtitlesFormatsSupportedForAllFiletypes());
	}

	/**
	 * Note: This can return false even when the renderer config has defined
	 * external subtitles support for individual filetypes.
	 *
	 * @param subtitlesFormat the subtitles format to check for.
	 * @return whether this renderer supports streaming this type of external
	 * subtitles for all video formats.
	 */
	public boolean isExternalSubtitlesFormatSupportedForAllFiletypes(String subtitlesFormat) {
		// First, check if this subtitles format is supported for all filetypes
		if (isSubtitlesStreamingSupportedForAllFiletypes()) {
			String[] supportedSubs = getExternalSubtitlesFormatsSupportedForAllFiletypes().split(",");
			for (String supportedSub : supportedSubs) {
				if (subtitlesFormat.equals(supportedSub.trim().toUpperCase())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Check if the given subtitle type is supported by renderer for streaming for given media.
	 *
	 * @todo this results in extra CPU use, since we probably already have
	 *       the result of getMatchedMIMEtype, so it would be better to
	 *       refactor the logic of the caller to make that function only run
	 *       once
	 * @param subtitle Subtitles for checking
	 * @param dlna
	 * @return whether the renderer specifies support for the subtitles and
	 * renderer supports subs streaming for the given media video.
	 */
	public boolean isExternalSubtitlesFormatSupported(MediaSubtitle subtitle, DLNAResource dlna) {
		if (subtitle == null || subtitle.getType() == null || dlna == null) {
			return false;
		}

		LOGGER.trace("Checking whether the external subtitles format " + subtitle.getType().toString() + " is supported by the renderer");

		return getFormatConfiguration().getMatchedMIMEtype(
			dlna.getMedia().getContainer(),
			null,
			null,
			0,
			0,
			0,
			0,
			0,
			0,
			8,
			null,
			null,
			null,
			subtitle.getType().getShortName().toUpperCase(Locale.ROOT),
			true,
			this
		) != null;
	}

	/**
	 * Note: This can return false even when the renderer config has defined
	 * external subtitles support for individual filetypes.
	 *
	 * @param subtitlesFormat the embedded subtitles format to check for.
	 * @return whether this renderer supports streaming this type of embedded
	 * subtitles for all video formats.
	 */
	public boolean isEmbeddedSubtitlesFormatSupportedForAllFiletypes(String subtitlesFormat) {
		if (isEmbeddedSubtitlesSupported()) {
			String[] supportedSubs = getSupportedEmbeddedSubtitles().split(",");
			for (String supportedSub : supportedSubs) {
				if (subtitlesFormat.equals(supportedSub.trim().toUpperCase())) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Check if the internal subtitle type is supported by renderer.
	 *
	 * @param subtitle Subtitles for checking
	 * @param dlna The dlna resource
	 * @return whether the renderer specifies support for the subtitles
	 */
	public boolean isEmbeddedSubtitlesFormatSupported(MediaSubtitle subtitle, DLNAResource dlna) {
		if (subtitle == null) {
			return false;
		}

		LOGGER.trace("Checking whether the embedded subtitles format " + (subtitle.getType().toString() != null ? subtitle.getType().toString() : "null") + " is supported by the renderer");
		return getFormatConfiguration().getMatchedMIMEtype(dlna, this) != null;
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
		String value = getString(KEY_OUTPUT_3D_FORMAT, "").toLowerCase(Locale.ROOT);
		// check if the parameter is specified correctly
		if (StringUtils.isNotBlank(value)) {
			for (Mode3D format : Mode3D.values()) {
				if (value.equals(format.toString().toLowerCase(Locale.ROOT))) {
					return value;
				}
			}

			LOGGER.debug("The output 3D format `{}` specified in the `Output3DFormat` is not implemented or incorrectly specified.", value);
		}

		return "";
	}

	/**
	 * Whether to use AviSynth 2D to 3D conversion script.
	 *
	 * @return true if 2D to 3D conversion should be done.
	 */
	public boolean getAviSynth2Dto3D() {
		return getBoolean(KEY_AVISYNTH_2D_TO_3D, false);
	}

	public boolean ignoreTranscodeByteRangeRequests() {
		return getBoolean(KEY_IGNORE_TRANSCODE_BYTE_RANGE_REQUEST, false);
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
			try {
				return sortedHeaderMatcher.reset(headers.joined()).find();
			} catch (Exception e) {
				return false;
			}
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
		return getInt(KEY_LOADING_PRIORITY, 0);
	}

	public String getSubLanguage() {
		return umsConfiguration.getSubtitlesLanguages();
	}

	public int getMaxVolume() {
		return getInt(KEY_MAX_VOLUME, 100);
	}

	public void setIdentifiers(List<String> identifiers) {
		this.identifiers = identifiers;
	}

	public List<String> getIdentifiers() {
		return identifiers;
	}

	public boolean hasDeviceId() {
		return configuration.containsKey(KEY_DEVICE_ID) || configuration.containsKey("device");
	}

	public String getDeviceId() {
		String d = getString(KEY_DEVICE_ID, "");
		if (StringUtils.isBlank(d)) {
			// Backward compatibility
			d = getString("device", "");
		}
		// Note: this might be a comma-separated list of ids
		return d;
	}

	/**
	 * Whether the renderer can display thumbnails.
	 *
	 * @return whether the renderer can display thumbnails
	 */
	public boolean isThumbnails() {
		return getBoolean(KEY_THUMBNAILS, true);
	}

	/**
	 * Whether we should add black padding to thumbnails so they are always
	 * at the same resolution, or just scale to within the limits.
	 *
	 * @return whether to add padding to thumbnails
	 */
	public boolean isThumbnailPadding() {
		return getBoolean(KEY_THUMBNAIL_PADDING, false);
	}

	/**
	 * Whether to stream subtitles even if the video is transcoded. It may work on some renderers.
	 *
	 * @return whether to stream subtitles for transcoded video
	 */
	public boolean streamSubsForTranscodedVideo() {
		return getBoolean(KEY_STREAM_SUBS_FOR_TRANSCODED_VIDEO, false);
	}

	/**
	 * List of supported video bit depths.
	 *
	 * @return a comma-separated list of supported video bit depths.
	 */
	public String getSupportedVideoBitDepths() {
		return getString(KEY_SUPPORTED_VIDEO_BIT_DEPTHS, "8");
	}

	/**
	 * Check if the given video bit depth is supported.
	 *
	 * @todo this results in extra CPU use, since we probably already have
	 *       the result of getMatchedMIMEtype, so it would be better to
	 *       refactor the logic of the caller to make that function only run
	 *       once
	 * @param dlna the resource to check
	 * @return whether the video bit depth is supported.
	 */
	public boolean isVideoBitDepthSupported(DLNAResource dlna) {
		Integer videoBitDepth = null;
		if (dlna.getMedia() != null) {
			videoBitDepth = dlna.getMedia().getVideoBitDepth();
		}

		if (videoBitDepth != null) {
			String[] supportedBitDepths = getSupportedVideoBitDepths().split(",");
			for (String supportedBitDepth : supportedBitDepths) {
				if (Objects.equals(Integer.valueOf(supportedBitDepth.trim()), videoBitDepth)) {
					return true;
				}
			}
		}

		LOGGER.trace("Checking whether the video bit depth " + (videoBitDepth != null ? videoBitDepth : "null") + " matches any 'vbd' entries in the 'Supported' lines");
		return getFormatConfiguration().getMatchedMIMEtype(dlna, this) != null;
	}

	/**
	 * Note: This can return false even when the renderer config has defined
	 * support for the bit depth for individual filetypes.
	 *
	 * @param videoBitDepth the video bit depth to check for.
	 * @return whether this renderer supports streaming this video bit depth
	 *         for all video formats.
	 */
	public boolean isVideoBitDepthSupportedForAllFiletypes(Integer videoBitDepth) {
		if (videoBitDepth != null) {
			String[] supportedBitDepths = getSupportedVideoBitDepths().split(",");
			for (String supportedBitDepth : supportedBitDepths) {
				if (Objects.equals(Integer.valueOf(supportedBitDepth.trim()), videoBitDepth)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isRemoveTagsFromSRTsubs() {
		return getBoolean(KEY_REMOVE_TAGS_FROM_SRT_SUBS, true);
	}

	public int getHlsVersion() {
		return getInt(KEY_HLS_VERSION, 3);
	}

	public boolean getHlsMultiVideoQuality() {
		return getBoolean(KEY_HLS_MULTI_VIDEO_QUALITY, false);
	}

	protected static int[] getVideoBitrateConfig(String bitrate) {
		int[] bitrates = new int[2];

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

	private static int getIntAt(String s, String key, int fallback) {
		if (StringUtils.isBlank(s) || StringUtils.isBlank(key)) {
			return fallback;
		}

		try {
			return Integer.parseInt((s + " ").split(key)[1].split("\\D")[0]);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			return fallback;
		}
	}

	/**
	 * Automatic reloading
	 */
	private static final FileWatcher.Listener RELOADER = (String filename, String event, FileWatcher.Watch watch, boolean isDir) -> {
		RendererConfiguration r = (RendererConfiguration) watch.getItem();
		if (r != null && r.getFile().equals(new File(filename))) {
			r.reset();
		}
	};

	public static List<String> getRendererLines(Renderer renderer, File refFile) {
		List<String> lines = new ArrayList<>();
		try {
			String name = renderer.getSimpleName();
			Map<String, String> details = renderer.getUpnpDetails();
			List<String> headers = renderer.getIdentifiers();
			boolean hasRef = refFile != null && refFile != NOFILE;

			// Add the header and identifiers
			lines.add("#----------------------------------------------------------------------------");
			lines.add("# Auto-generated profile for " + name);
			lines.add("#" + (refFile != null && refFile != NOFILE ? " Based on " + refFile.getName() : ""));
			lines.add("# See DefaultRenderer.conf for a description of all possible configuration options.");
			lines.add("#");
			lines.add("");
			lines.add(KEY_RENDERER_NAME + " = " + name);
			if (headers != null || details != null) {
				lines.add("");
				lines.add("# ============================================================================");
				lines.add("# This renderer has sent the following string/s:");
				if (headers != null && !headers.isEmpty()) {
					lines.add("#");
					for (String h : headers) {
						lines.add("# " + h);
					}
				}
				if (details != null) {
					details.remove("address");
					details.remove("udn");
					lines.add("#");
					lines.add("# " + details);
				}
				lines.add("# ============================================================================");
				lines.add("");
			}
			lines.add(KEY_USER_AGENT + " = ");
			if (headers != null && headers.size() > 1) {
				lines.add(KEY_USER_AGENT_ADDITIONAL_HEADER + " = ");
				lines.add(KEY_USER_AGENT_ADDITIONAL_SEARCH + " = ");
			}
			if (details != null) {
				lines.add(KEY_UPNP_DETAILS + " = " + details.get("manufacturer") + " , " + details.get("modelName"));
			}
			lines.add("");
			// TODO: Set more properties automatically from UPNP info

			if (hasRef) {
				// Copy the reference file, skipping its header and identifiers
				Matcher skip = Pattern.compile(".*(" + KEY_RENDERER_ICON + "|" + KEY_RENDERER_NAME + "|" +
					KEY_UPNP_DETAILS + "|" + KEY_USER_AGENT + "|" + KEY_USER_AGENT_ADDITIONAL_HEADER + "|" +
					KEY_USER_AGENT_ADDITIONAL_SEARCH + ").*").matcher("");
				boolean header = true;
				for (String line : FileUtils.readLines(refFile, StandardCharsets.UTF_8)) {
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
					lines.add(line);
				}
			}
		} catch (IOException ie) {
			LOGGER.debug("Error creating renderer configuration file: " + ie);
		}
		return lines;
	}

	public static List<String> getDeviceLines(Renderer renderer) {
		List<String> lines = new ArrayList<>();

		// Add the header and device id
		lines.add("#----------------------------------------------------------------------------");
		lines.add("# Custom Device profile");
		lines.add("# See DefaultRenderer.conf for descriptions of all possible renderer options");
		lines.add("# and UMS.conf for program options.");
		lines.add("");
		lines.add("# Options in this file override the default settings for the specific " + renderer.getSimpleName() + " device(s) listed below.");
		lines.add("# Specify devices by uuid (or address if no uuid), separated by commas if more than one.");
		lines.add("");
		lines.add(KEY_DEVICE_ID + " = " + renderer.getId());
		return lines;
	}

}
