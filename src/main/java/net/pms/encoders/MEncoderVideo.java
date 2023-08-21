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
package net.pms.encoders;

import bsh.EvalError;
import bsh.Interpreter;
import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.DVDISOTitle;
import net.pms.dlna.InputFile;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.*;
import net.pms.media.MediaInfo;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.network.HTTPResource;
import net.pms.platform.PlatformUtils;
import net.pms.platform.windows.NTStatus;
import net.pms.renderers.Renderer;
import net.pms.util.*;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import static net.pms.util.AudioUtils.getLPCMChannelMappingForMencoder;
import static net.pms.util.StringUtil.quoteArg;
import org.apache.commons.lang3.ArrayUtils;
import static org.apache.commons.lang.BooleanUtils.isTrue;
import static org.apache.commons.lang3.StringUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MEncoderVideo extends Engine {
	private static final Logger LOGGER = LoggerFactory.getLogger(MEncoderVideo.class);
	public static final EngineId ID = StandardEngineId.MENCODER_VIDEO;

	/** The {@link Configuration} key for the custom MEncoder path. */
	public static final String KEY_MENCODER_PATH = "mencoder_path";

	/** The {@link Configuration} key for the MEncoder executable type. */
	public static final String KEY_MENCODER_EXECUTABLE_TYPE = "mencoder_executable_type";
	public static final String NAME = "MEncoder Video";

	private static final String REMOVE_OPTION = "---REMOVE-ME---"; // use an out-of-band option that can't be confused with a real option


	private static final String[] INVALID_CUSTOM_OPTIONS = {
		"-of",
		"-oac",
		"-ovc",
		"-mpegopts"
	};

	private static final String INVALID_CUSTOM_OPTIONS_LIST = Arrays.toString(INVALID_CUSTOM_OPTIONS);

	public static final int MENCODER_MAX_THREADS = 8;

	private String[] overriddenMainArgs;
	private boolean dtsRemux;
	private boolean encodedAudioPassthrough;
	private boolean pcm;
	private boolean ovccopy;
	private boolean ac3Remux;
	private boolean isTranscodeToMPEGTS;

	/**
	 * Whether MEncoder will transcode to H.264.
	 * Note: This will be true if the renderer has specified H.265
	 * because MEncoder does not support encoding to H.265.
	 */
	private boolean isTranscodeToH264;
	private boolean isTranscodeToAAC;
	private boolean wmv;

	/**
	 * @todo reduce the amount of translation lines, .properties files support line breaks
	 */
	public static final String DEFAULT_CODEC_CONF_SCRIPT =
		Messages.getString("MencoderConfigScript.1.HereYouCanInputSpecific") +
		Messages.getString("MencoderConfigScript.2.WarningThisShouldNot") +
		Messages.getString("MencoderConfigScript.3.SyntaxIsJavaCondition") +
		Messages.getString("MencoderConfigScript.4.AuthorizedVariables") +
		Messages.getString("MencoderConfigScript.5.SpecialOptions") +
		Messages.getString("MencoderConfigScript.6.Noass") +
		Messages.getString("MencoderConfigScript.7.Nosync") +
		Messages.getString("MencoderConfigScript.8.Quality") +
		Messages.getString("MencoderConfigScript.9.Nomux") +
		"\n" +
		"container == iso :: -nosync\n" +
		"(container == avi || container == matroska) && vcodec == mpeg4 && acodec == mp3 :: -mc 0.1\n" +
		"container == flv :: -mc 0.1\n" +
		"container == mov :: -mc 0.1\n" +
		"container == rm  :: -mc 0.1\n" +
		"container == mp4 && vcodec == h264 :: -mc 0.1\n" +
		"\n" +
		Messages.getString("MencoderConfigScript.10.YouCanPut") +
		Messages.getString("MencoderConfigScript.11.ToRemoveJudder") +
		Messages.getString("MencoderConfigScript.12.ToRemux");

	// Not to be instantiated by anything but PlayerFactory
	MEncoderVideo() {
		super(CONFIGURATION.getMEncoderPaths());
	}

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_ENGINE;
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getConfigurablePathKey() {
		return KEY_MENCODER_PATH;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_MENCODER_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isAviSynthEngine() {
		return false;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	protected String[] getDefaultArgs() {
		List<String> defaultArgsList = new ArrayList<>();

		defaultArgsList.add("-msglevel");
		defaultArgsList.add("statusline=2");

		defaultArgsList.add("-oac");
		if (ac3Remux || dtsRemux) {
			defaultArgsList.add("copy");
		} else if (pcm) {
			defaultArgsList.add("pcm");
		} else if (isTranscodeToAAC) {
			defaultArgsList.add("faac");
			defaultArgsList.add("-faacopts");
			defaultArgsList.add("br=320:mpeg=4:object=2");
		} else {
			defaultArgsList.add("lavc");
		}

		defaultArgsList.add("-of");
		if (wmv || isTranscodeToMPEGTS) {
			defaultArgsList.add("lavf");
		} else if (pcm && isAviSynthEngine()) {
			defaultArgsList.add("avi");
		} else if (pcm || dtsRemux || encodedAudioPassthrough) {
			defaultArgsList.add("rawvideo");
		} else {
			defaultArgsList.add("mpeg");
		}

		if (wmv) {
			defaultArgsList.add("-lavfopts");
			defaultArgsList.add("format=asf");
		} else if (isTranscodeToMPEGTS) {
			defaultArgsList.add("-lavfopts");
			defaultArgsList.add("format=mpegts");
		}

		if (!isTranscodeToH264) {
			defaultArgsList.add("-mpegopts");
			defaultArgsList.add("format=mpeg2:muxrate=500000:vbuf_size=1194:abuf_size=64");
		}

		defaultArgsList.add("-ovc");
		String ovc = "lavc";
		if (ovccopy) {
			ovc = "copy";
		} else if (isTranscodeToH264) {
			ovc = "x264";
		}
		defaultArgsList.add(ovc);

		String[] defaultArgsArray = new String[defaultArgsList.size()];
		defaultArgsList.toArray(defaultArgsArray);

		return defaultArgsArray;
	}

	private static String[] sanitizeArgs(String[] args) {
		List<String> sanitized = new ArrayList<>();
		int i = 0;

		while (i < args.length) {
			String name = args[i];
			String value = null;

			for (String option : INVALID_CUSTOM_OPTIONS) {
				if (option.equals(name)) {
					if ((i + 1) < args.length) {
						value = " " + args[i + 1];
						++i;
					} else {
						value = "";
					}

					LOGGER.warn(
						"Ignoring custom MEncoder option: {}{}; the following options cannot be changed: " + INVALID_CUSTOM_OPTIONS_LIST,
						name,
						value
					);

					break;
				}
			}

			if (value == null) {
				sanitized.add(args[i]);
			}

			++i;
		}

		return sanitized.toArray(String[]::new);
	}

	public String[] args() {
		String[] args;
		String[] defaultArgs = getDefaultArgs();

		if (overriddenMainArgs != null) {
			// add the sanitized custom MEncoder options.
			// not cached because they may be changed on the fly in the GUI
			args = ArrayUtils.addAll(defaultArgs, sanitizeArgs(overriddenMainArgs));
		} else {
			args = defaultArgs;
		}

		return args;
	}

	private static int[] getVideoBitrateConfig(String bitrate) {
		int[] bitrates = new int[2];

		if (bitrate.contains("(") && bitrate.contains(")")) {
			try {
				bitrates[1] = Integer.parseInt(bitrate.substring(bitrate.indexOf('(') + 1, bitrate.indexOf(')')));
			} catch (NumberFormatException e) {
				bitrates[1] = 0;
			}
		}

		if (bitrate.contains("(")) {
			bitrate = bitrate.substring(0, bitrate.indexOf('(')).trim();
		}

		if (isBlank(bitrate)) {
			bitrate = "0";
		}

		try {
			bitrates[0] = (int) Double.parseDouble(bitrate);
		} catch (NumberFormatException e) {
			bitrates[0] = 0;
		}

		return bitrates;
	}

	/**
	 * Note: This is not exact. The bitrate can go above this but it is generally pretty good.
	 *
	 * @return The maximum bitrate the video should be along with the buffer size using MEncoder vars
	 */
	private String addMaximumBitrateConstraints(String encodeSettings, MediaInfo media, String quality, Renderer renderer, String audioType) {
		// Use device-specific UMS conf
		UmsConfiguration dConfiguration = PMS.getConfiguration(renderer);
		int[] defaultMaxBitrates = getVideoBitrateConfig(dConfiguration.getMaximumBitrate());
		int[] rendererMaxBitrates = new int[2];

		if (renderer.getMaxVideoBitrate() > 0) {
			rendererMaxBitrates = getVideoBitrateConfig(Integer.toString(renderer.getMaxVideoBitrate()));
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			LOGGER.trace("Using video bitrate limit from {} configuration ({} Mb/s) because " +
				"it is lower than the general configuration bitrate limit ({} Mb/s)",
				renderer.getRendererName(),
				rendererMaxBitrates[0],
				defaultMaxBitrates[0]
			);
			defaultMaxBitrates = rendererMaxBitrates;
		} else {
			LOGGER.trace(
				"Using video bitrate limit from the general configuration ({} Mb/s)",
				defaultMaxBitrates[0]
			);
		}

		if (renderer.getCBRVideoBitrate() == 0 && !quality.contains("vrc_buf_size") && !quality.contains("vrc_maxrate") && !quality.contains("vbitrate")) {
			// Convert value from Mb to Kb
			defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];

			if (renderer.isHalveBitrate() && !dConfiguration.isAutomaticMaximumBitrate()) {
				defaultMaxBitrates[0] /= 2;
				LOGGER.trace("Halving the video bitrate limit to {} kb/s", defaultMaxBitrates[0]);
			}

			int bufSize = 1835;
			boolean bitrateLevel41Limited = false;
			boolean isXboxOneWebVideo = renderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;

			/**
			 * Although the maximum bitrate for H.264 Level 4.1 is
			 * officially 50,000 kbit/s, some 4.1-capable renderers
			 * like the PS3 stutter when video exceeds roughly 31,250
			 * kbit/s.
			 *
			 * We also apply the correct buffer size in this section.
			 */
			if ((renderer.isTranscodeToH264() || renderer.isTranscodeToH265()) && !isXboxOneWebVideo) {
				if (
					renderer.isH264Level41Limited() &&
					defaultMaxBitrates[0] > 31250
				) {
					defaultMaxBitrates[0] = 31250;
					bitrateLevel41Limited = true;
					LOGGER.trace("Adjusting the video bitrate limit to the H.264 Level 4.1-safe value of 31250 kb/s");
				}
				bufSize = defaultMaxBitrates[0];
			} else {
				if (media.isHDVideo()) {
					bufSize = defaultMaxBitrates[0] / 3;
				}

				if (bufSize > 7000) {
					bufSize = 7000;
				}

				if (defaultMaxBitrates[1] > 0) {
					bufSize = defaultMaxBitrates[1];
				}

				if (renderer.isDefaultVBVSize() && rendererMaxBitrates[1] == 0) {
					bufSize = 1835;
				}
			}

			if (!bitrateLevel41Limited) {
				// Make room for audio
				switch (audioType) {
					case "pcm":
						defaultMaxBitrates[0] -= 4600;
						break;
					case "dts":
						defaultMaxBitrates[0] -= 1510;
						break;
					case "aac":
					case "ac3":
						defaultMaxBitrates[0] -= dConfiguration.getAudioBitrate();
						break;
					default:
						break;
				}

				// Round down to the nearest Mb
				defaultMaxBitrates[0] = defaultMaxBitrates[0] / 1000 * 1000;

				LOGGER.trace(
					"Adjusting the video bitrate limit to {} kb/s to make room for audio",
					defaultMaxBitrates[0]
				);
			}

			if (isTranscodeToH264) {
				encodeSettings += ":vbv_maxrate=" + defaultMaxBitrates[0] + ":vbv_bufsize=" + bufSize;
			} else {
				encodeSettings += ":vrc_maxrate=" + defaultMaxBitrates[0] + ":vrc_buf_size=" + bufSize;
			}
		}

		return encodeSettings;
	}

	/*
	 * Collapse the multiple internal ways of saying "subtitles are disabled" into a single method
	 * which returns true if any of the following are true:
	 *
	 *     1) configuration.isDisableSubtitles()
	 *     2) params.sid == null
	 *     3) avisynth()
	 */
	private boolean isDisableSubtitles(OutputParams params) {
		return configuration.isDisableSubtitles() || (params.getSid() == null) || isAviSynthEngine();
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		// Use device-specific UMS conf
		UmsConfiguration prev = configuration;
		configuration = params.getMediaRenderer().getUmsConfiguration();
		params.manageFastStart();

		boolean avisynth = isAviSynthEngine();

		final String filename = dlna.getFileName();
		setAudioAndSubs(dlna, params);
		String externalSubtitlesFileName = null;

		if (params.getSid() != null && params.getSid().isExternal()) {
			if (params.getSid().getExternalFile() != null) {
				if (params.getSid().isExternalFileUtf16()) {
					// convert UTF-16 -> UTF-8
					File convertedSubtitles = new File(configuration.getTempFolder(), "utf8_" + params.getSid().getExternalFile().getName());
					FileUtil.convertFileFromUtf16ToUtf8(params.getSid().getExternalFile(), convertedSubtitles);
					externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(convertedSubtitles.getAbsolutePath());
				} else {
					externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(params.getSid().getExternalFile());
				}
			} else {
				LOGGER.error("External subtitles file \"{}\" is unavailable", params.getSid().getName());
			}
		}

		InputFile newInput = new InputFile();
		newInput.setFilename(filename);
		newInput.setPush(params.getStdIn());

		boolean isDVD = dlna instanceof DVDISOTitle;

		ovccopy  = false;
		pcm      = false;
		ac3Remux = false;
		dtsRemux = false;
		wmv      = false;

		int intOCW = 0;
		int intOCH = 0;

		try {
			intOCW = Integer.parseInt(configuration.getMencoderOverscanCompensationWidth());
		} catch (NumberFormatException e) {
			LOGGER.error("Cannot parse configured MEncoder overscan compensation width: \"{}\"", configuration.getMencoderOverscanCompensationWidth());
		}

		try {
			intOCH = Integer.parseInt(configuration.getMencoderOverscanCompensationHeight());
		} catch (NumberFormatException e) {
			LOGGER.error("Cannot parse configured MEncoder overscan compensation height: \"{}\"", configuration.getMencoderOverscanCompensationHeight());
		}

		/*
		 * Check if the video track and the container report different aspect ratios
		 */
		boolean aspectRatiosMatch = true;
		if (
			media.getAspectRatioContainer() != null &&
			media.getAspectRatioVideoTrack() != null &&
			!media.getAspectRatioContainer().equals(media.getAspectRatioVideoTrack())
		) {
			aspectRatiosMatch = false;
		}

		// Decide whether to defer to tsMuxeR or continue to use MEncoder
		boolean deferToTsmuxer = true;
		String prependTraceReason = "Not muxing the video stream with tsMuxeR via MEncoder because ";
		if (!configuration.isMencoderMuxWhenCompatible()) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the user setting is disabled");
		} else if (dlna.isInsideTranscodeFolder()) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the file is being played via a MEncoder entry in the TRANSCODE folder.");
		} else if (!params.getMediaRenderer().isMuxH264MpegTS()) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the renderer does not support H.264 inside MPEG-TS.");
		} else if (params.getSid() != null) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "we need to burn subtitles.");
		} else if (isDVD) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "this is a DVD track.");
		} else if (isAviSynthEngine()) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "we are using AviSynth.");
		} else if (params.getMediaRenderer().isH264Level41Limited() && !media.isVideoWithinH264LevelLimits(newInput, params.getMediaRenderer())) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the video stream is not within H.264 level limits for this renderer.");
		} else if (!media.isMuxable(params.getMediaRenderer())) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the video stream is not muxable to this renderer");
		} else if (intOCW > 0 && intOCH > 0) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "we need to transcode to apply overscan compensation.");
		} else if (!aspectRatiosMatch) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "we need to transcode to apply the correct aspect ratio.");
		} else if (!params.getMediaRenderer().isPS3() && media.isWebDl(filename, params)) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the version of tsMuxeR supported by this renderer does not support WEB-DL files.");
		} else if ("bt.601".equals(media.getMatrixCoefficients())) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the colorspace probably isn't supported by the renderer.");
		} else if ((params.getMediaRenderer().isKeepAspectRatio() || params.getMediaRenderer().isKeepAspectRatioTranscoding()) && !"16:9".equals(media.getAspectRatioContainer())) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the renderer needs us to add borders so it displays the correct aspect ratio of " + media.getAspectRatioContainer() + ".");
		} else if (!params.getMediaRenderer().isResolutionCompatibleWithRenderer(media.getWidth(), media.getHeight())) {
			deferToTsmuxer = false;
			LOGGER.trace(prependTraceReason + "the resolution is incompatible with the renderer.");
		} else if (!EngineFactory.isEngineAvailable(StandardEngineId.TSMUXER_VIDEO)) {
			deferToTsmuxer = false;
			LOGGER.warn(prependTraceReason + "the configured executable isn't available.");
		}
		if (deferToTsmuxer) {
			String[] expertOptions = getSpecificCodecOptions(
				configuration.getMencoderCodecSpecificConfig(),
				media,
				params,
				filename,
				externalSubtitlesFileName,
				configuration.isMencoderIntelligentSync(),
				false
			);

			boolean nomux = false;

			for (String s : expertOptions) {
				if (s.equals("-nomux")) {
					nomux = true;
				}
			}

			if (!nomux) {
				TsMuxeRVideo tv = (TsMuxeRVideo) EngineFactory.getEngine(StandardEngineId.TSMUXER_VIDEO, false, true);
				params.setForceFps(media.getValidFps(false));

				if (media.getCodecV() != null) {
					if (media.isH264()) {
						params.setForceType("V_MPEG4/ISO/AVC");
					} else if (media.getCodecV().startsWith("mpeg2")) {
						params.setForceType("V_MPEG-2");
					} else if (media.getCodecV().equals("vc1")) {
						params.setForceType("V_MS/VFW/WVC1");
					}
				}

				return tv.launchTranscode(dlna, media, params);
			}
		} else if (params.getSid() == null && isDVD && configuration.isMencoderRemuxMPEG2() && params.getMediaRenderer().isMpeg2Supported()) {
			String[] expertOptions = getSpecificCodecOptions(
				configuration.getMencoderCodecSpecificConfig(),
				media,
				params,
				filename,
				externalSubtitlesFileName,
				configuration.isMencoderIntelligentSync(),
				false
			);

			boolean nomux = false;

			for (String s : expertOptions) {
				if (s.equals("-nomux")) {
					nomux = true;
				}
			}

			if (!nomux) {
				ovccopy = true;
			}
		}

		isTranscodeToMPEGTS = params.getMediaRenderer().isTranscodeToMPEGTS();
		isTranscodeToH264   = params.getMediaRenderer().isTranscodeToH264() || params.getMediaRenderer().isTranscodeToH265();
		isTranscodeToAAC    = params.getMediaRenderer().isTranscodeToAAC();

		final boolean isXboxOneWebVideo = params.getMediaRenderer().isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;

		String vcodec = "mpeg2video";
		if (isTranscodeToH264) {
			vcodec = "libx264";
		} else if (
			(
				params.getMediaRenderer().isTranscodeToWMV() &&
				!params.getMediaRenderer().isXbox360()
			) ||
			isXboxOneWebVideo
		) {
			wmv = true;
			vcodec = "wmv2";
		}

		// Default: Empty string
		String rendererMencoderOptions = params.getMediaRenderer().getCustomMencoderOptions();

		/**
		 * Ignore the renderer's custom MEncoder options if a) we're streaming a DVD (i.e. via dvd://)
		 * or b) the renderer's MEncoder options contain overscan settings (those are handled
		 * separately)
		 */
		// XXX we should weed out the unused/unwanted settings and keep the rest
		// (see sanitizeArgs()) rather than ignoring the options entirely
		if (rendererMencoderOptions.contains("expand=") && isDVD) {
			rendererMencoderOptions = "";
		}

		// Default: Empty string
		String globalMencoderOptions = configuration.getMencoderCustomOptions();

		String combinedCustomOptions = defaultString(globalMencoderOptions) +
			" " +
			defaultString(rendererMencoderOptions);

		/**
		 * Disable AC3 remux for stereo tracks with 384 kbits bitrate and PS3 renderer (PS3 FW bug?)
		 *
		 * Commented out until we can find a way to detect when a video has an audio track that switches from 2 to 6 channels
		 * because MEncoder can't handle those files, which are very common these days.
		boolean ps3_and_stereo_and_384_kbits = params.aid != null &&
			(params.mediaRenderer.isPS3() && params.aid.getAudioProperties().getNumberOfChannels() == 2) &&
			(params.aid.getBitRate() > 370000 && params.aid.getBitRate() < 400000);
		 */

		final boolean isTsMuxeRVideoEngineActive = EngineFactory.isEngineActive(TsMuxeRVideo.ID);
		final boolean mencoderAC3RemuxAudioDelayBug = (params.getAid() != null) && (params.getAid().getAudioProperties().getAudioDelay() != 0) && (params.getTimeSeek() == 0);

		encodedAudioPassthrough = isTsMuxeRVideoEngineActive &&
			configuration.isEncodedAudioPassthrough() &&
			params.getMediaRenderer().isWrapEncodedAudioIntoPCM() &&
			(
				!isDVD ||
				configuration.isMencoderRemuxMPEG2()
			) &&
			params.getAid() != null &&
			params.getAid().isNonPCMEncodedAudio() &&
			!isAviSynthEngine() &&
			params.getMediaRenderer().isMuxLPCMToMpeg();

		if (
			configuration.isAudioRemuxAC3() &&
			params.getAid() != null &&
			params.getAid().isAC3() &&
			!isAviSynthEngine() &&
			params.getMediaRenderer().isTranscodeToAC3() &&
			!configuration.isMEncoderNormalizeVolume() &&
			!combinedCustomOptions.contains("acodec=") &&
			!encodedAudioPassthrough &&
			!isXboxOneWebVideo &&
			params.getAid().getAudioProperties().getNumberOfChannels() <= configuration.getAudioChannelCount()
		) {
			ac3Remux = true;
		} else {
			// Now check for DTS remux and LPCM streaming
			dtsRemux = isTsMuxeRVideoEngineActive &&
				configuration.isAudioEmbedDtsInPcm() &&
				(
					!isDVD ||
					configuration.isMencoderRemuxMPEG2()
				) &&
				params.getAid() != null &&
				params.getAid().isDTS() &&
				!isAviSynthEngine() &&
				params.getMediaRenderer().isDTSPlayable() &&
				!combinedCustomOptions.contains("acodec=");
			pcm = isTsMuxeRVideoEngineActive &&
				configuration.isAudioUsePCM() &&
				(
					!isDVD ||
					configuration.isMencoderRemuxMPEG2()
				) &&
				// Disable LPCM transcoding for MP4 container with non-H.264 video as workaround for MEncoder's A/V sync bug
				!(media.getContainer().equals("mp4") && !media.isH264()) &&
				params.getAid() != null &&
				(
					(params.getAid().isDTS() && params.getAid().getAudioProperties().getNumberOfChannels() <= 6) || // disable 7.1 DTS-HD => LPCM because of channels mapping bug
					params.getAid().isLossless() ||
					params.getAid().isTrueHD() ||
					(
						!configuration.isMencoderUsePcmForHQAudioOnly() &&
						(
							params.getAid().isAC3() ||
							params.getAid().isMP3() ||
							params.getAid().isAAC() ||
							params.getAid().isVorbis() ||
							// Disable WMA to LPCM transcoding because of mencoder's channel mapping bug
							// (see CodecUtil.getMixerOutput)
							// params.aid.isWMA() ||
							params.getAid().isMpegAudio()
						)
					)
				) &&
				params.getMediaRenderer().isLPCMPlayable() &&
				!combinedCustomOptions.contains("acodec=");
		}

		if (dtsRemux || pcm || encodedAudioPassthrough) {
			params.setLosslessAudio(true);
			params.setForceFps(media.getValidFps(false));
		}

		// MPEG-2 remux still buggy with MEncoder
		// TODO when we can still use it?
		ovccopy = false;

		if (pcm && isAviSynthEngine()) {
			params.setAvidemux(true);
		}

		String add = "";
		if (!combinedCustomOptions.contains("-lavdopts")) {
			add = " -lavdopts debug=0";
		}

		int channels;
		if (ac3Remux) {
			channels = params.getAid().getAudioProperties().getNumberOfChannels(); // AC-3 remux
		} else if (dtsRemux || encodedAudioPassthrough || (!params.getMediaRenderer().isXbox360() && wmv)) {
			channels = 2;
		} else if (
			params.getAid().getAudioProperties().getNumberOfChannels() == 8 ||
			params.getAid().isAAC()
		) {
			// MEncoder crashes when trying to downmix 7.1 AAC to 5.1 AC-3
			channels = 2;
		} else if (pcm) {
			channels = params.getAid().getAudioProperties().getNumberOfChannels();
		} else {
			/**
			 * Note: MEncoder will output 2 audio channels if the input video had 2 channels
			 * regardless of us telling it to output 6 (unlike FFmpeg which will output 6).
			 */
			channels = configuration.getAudioChannelCount(); // 5.1 max for AC-3 encoding
		}
		String channelsString = "-channels " + channels;
		if (combinedCustomOptions.contains("-channels")) {
			channelsString = "";
		}

		StringTokenizer st = new StringTokenizer(
			channelsString +
			(isNotBlank(globalMencoderOptions) ? " " + globalMencoderOptions : "") +
			(isNotBlank(rendererMencoderOptions) ? " " + rendererMencoderOptions : "") +
			add,
			" "
		);

		// XXX why does this field (which is used to populate the array returned by args(),
		// called below) store the renderer-specific (i.e. not global) MEncoder options?
		overriddenMainArgs = new String[st.countTokens()];

		int nThreads = (isDVD || filename.toLowerCase().endsWith("dvr-ms")) ?
			1 :
			configuration.getMencoderMaxThreads();

		// MEncoder loses audio/video sync if more than 4 decoder (lavdopts) threads are used.
		// Multithreading for decoding offers little performance gain anyway so it's not a big deal.
		if (nThreads > 4) {
			nThreads = 4;
		}

		boolean handleToken = false;
		int i = 0;

		while (st.hasMoreTokens()) {
			String token = st.nextToken().trim();

			if (handleToken) {
				token += ":threads=" + nThreads;

				if (configuration.getSkipLoopFilterEnabled() && !isAviSynthEngine()) {
					token += ":skiploopfilter=all";
				}

				handleToken = false;
			}

			if (token.toLowerCase().contains("lavdopts")) {
				handleToken = true;
			}

			overriddenMainArgs[i++] = token;
		}

		String vcodecString = ":vcodec=" + vcodec;
		if (combinedCustomOptions.contains("vcodec=")) {
			vcodecString = "";
		}

		if (
			(configuration.getx264ConstantRateFactor() != null && isTranscodeToH264) ||
			(configuration.getMPEG2MainSettings() != null && !isTranscodeToH264)
		) {
			// Ditlew - WDTV Live (+ other byte asking clients), CBR. This probably ought to be placed in addMaximumBitrateConstraints(..)
			int cbrBitrate = params.getMediaRenderer().getCBRVideoBitrate();
			String cbrSettings = (cbrBitrate > 0) ?
				":vrc_buf_size=5000:vrc_minrate=" + cbrBitrate + ":vrc_maxrate=" +
				cbrBitrate + ":vbitrate=" + ((cbrBitrate > 16000) ? cbrBitrate * 1000 : cbrBitrate) :
				"";

			// Set audio codec and bitrate if audio is being transcoded
			String acodec   = "";
			String abitrate = "";
			if (!ac3Remux && !dtsRemux && !isTranscodeToAAC) {
				// Set the audio codec used by Lavc
				if (!combinedCustomOptions.contains("acodec=")) {
					acodec = ":acodec=";
					if (wmv && !params.getMediaRenderer().isXbox360()) {
						acodec += "wmav2";
					} else {
						acodec = cbrSettings + acodec;
						if (params.getMediaRenderer().isTranscodeToAAC()) {
							acodec += "libfaac";
						} else if (configuration.isMencoderAc3Fixed()) {
							acodec += "ac3_fixed";
						} else {
							acodec += "ac3";
						}
					}
				}

				// Set the audio bitrate used by Lavc
				if (!combinedCustomOptions.contains("abitrate=")) {
					abitrate = ":abitrate=";
					if (wmv && !params.getMediaRenderer().isXbox360()) {
						abitrate += "448";
					} else {
						abitrate += CodecUtil.getAC3Bitrate(configuration, params.getAid());
					}
				}
			}

			// TODO : check why we defaultMaxBitrates to set maximumBitrate not used

			// Find out the maximum bandwidth we are supposed to use
			int[] defaultMaxBitrates = getVideoBitrateConfig(configuration.getMaximumBitrate());
			int[] rendererMaxBitrates = new int[2];

			if (params.getMediaRenderer().getMaxVideoBitrate() > 0) {
				rendererMaxBitrates = getVideoBitrateConfig(Integer.toString(params.getMediaRenderer().getMaxVideoBitrate()));
			}

			if ((rendererMaxBitrates[0] > 0) && (rendererMaxBitrates[0] < defaultMaxBitrates[0])) {
				LOGGER.trace(
					"Using video bitrate limit from {} configuration ({} Mb/s) because " +
					"it is lower than the general configuration bitrate limit ({} Mb/s)",
					params.getMediaRenderer().getRendererName(),
					rendererMaxBitrates[0],
					defaultMaxBitrates[0]
				);
				defaultMaxBitrates = rendererMaxBitrates;
			}

			int maximumBitrate = defaultMaxBitrates[0];

			// Set which audio codec to use
			String audioType = "ac3";
			if (dtsRemux) {
				audioType = "dts";
			} else if (pcm || encodedAudioPassthrough) {
				audioType = "pcm";
			} else if (params.getMediaRenderer().isTranscodeToAAC()) {
				audioType = "aac";
			}

			String encodeSettings = "";

			/**
			 * Fixes aspect ratios on Sony TVs
			 */
			String aspectRatioLavcopts = "autoaspect=1";
			if (
				!isDVD &&
				(
					(
						params.getMediaRenderer().isKeepAspectRatio() ||
						params.getMediaRenderer().isKeepAspectRatioTranscoding()
					) &&
					!"16:9".equals(media.getAspectRatioContainer())
				) &&
				!configuration.isMencoderScaler()
			) {
				aspectRatioLavcopts = "aspect=16/9";
			}

			if (isXboxOneWebVideo || (configuration.getMPEG2MainSettings() != null && !isTranscodeToH264)) {
				// Set MPEG-2 video quality
				String mpeg2Options = configuration.getMPEG2MainSettings();
				String mpeg2OptionsRenderer = params.getMediaRenderer().getCustomMEncoderMPEG2Options();

				// Renderer settings take priority over user settings
				if (isNotBlank(mpeg2OptionsRenderer)) {
					mpeg2Options = mpeg2OptionsRenderer;
				} else {
					// Remove comment from the value
					if (mpeg2Options.contains("/*")) {
						mpeg2Options = mpeg2Options.substring(mpeg2Options.indexOf("/*"));
					}

					// when the automatic bandwidth is used than use the proper automatic MPEG2 setting
					if (configuration.isAutomaticMaximumBitrate()) {
						mpeg2Options = params.getMediaRenderer().getAutomaticVideoQuality();
					}

					if (mpeg2Options.contains("Automatic")) {
						if (mpeg2Options.contains("Wireless")) {
							// Lower quality for 720p+ content
							if (media.getWidth() > 1280) {
								mpeg2Options = "keyint=25:vqmin=2:vqmax=7";
							} else if (media.getWidth() > 720) {
								mpeg2Options = "keyint=25:vqmin=2:vqmax=5";
							} else {
								mpeg2Options = "keyint=25:vqmin=2:vqmax=3";
							}
						} else { // set the automatic wired quality
							mpeg2Options = "keyint=5:vqscale=1:vqmin=2:vqmax=3";
						}
					}

					if (params.getMediaRenderer().isPS3()) {
						// It has been reported that non-PS3 renderers prefer keyint 5 but prefer 25 for PS3 because it lowers the average bitrate
						mpeg2Options = "keyint=25:vqscale=1:vqmin=2:vqmax=3";
					}
				}

				encodeSettings = "-lavcopts " + aspectRatioLavcopts + vcodecString + acodec + abitrate +
					":threads=" + (wmv && !params.getMediaRenderer().isXbox360() ? 1 : configuration.getMencoderMaxThreads()) +
					("".equals(mpeg2Options) ? "" : ":" + mpeg2Options);

				encodeSettings = addMaximumBitrateConstraints(encodeSettings, media, mpeg2Options, params.getMediaRenderer(), audioType);
			} else if (configuration.getx264ConstantRateFactor() != null && isTranscodeToH264) {
				// Set H.264 video quality
				String x264CRF = configuration.getx264ConstantRateFactor();
				if (configuration.isAutomaticMaximumBitrate()) {
					x264CRF = params.getMediaRenderer().getAutomaticVideoQuality();
				}

				// Remove comment from the value
				if (x264CRF.contains("/*")) {
					x264CRF = x264CRF.substring(x264CRF.indexOf("/*"));
				}

				// Determine a good quality setting based on video attributes
				if (x264CRF.contains("Automatic")) {
					if (x264CRF.contains("Wireless")) {
						x264CRF = "19";
						// Lower quality for 720p+ content
						if (media.getWidth() > 1280) {
							x264CRF = "23";
						} else if (media.getWidth() > 720) {
							x264CRF = "22";
						}
					} else {
						x264CRF = "16";

						// Lower quality for 720p+ content
						if (media.getWidth() > 720) {
							x264CRF = "19";
						}
					}
				}

				encodeSettings = "-lavcopts " + aspectRatioLavcopts + acodec + abitrate +
					":threads=" + configuration.getMencoderMaxThreads();

				encodeSettings += " -x264encopts crf=" + x264CRF + ":preset=superfast:level=31:threads=auto";

				encodeSettings = addMaximumBitrateConstraints(encodeSettings, media, "", params.getMediaRenderer(), audioType);
			}

			st = new StringTokenizer(encodeSettings, " ");

			int ii = overriddenMainArgs.length; // Old length
			overriddenMainArgs = Arrays.copyOf(overriddenMainArgs, overriddenMainArgs.length + st.countTokens());

			while (st.hasMoreTokens()) {
				overriddenMainArgs[ii++] = st.nextToken();
			}
		}

		boolean foundNoassParam = false;

		String[] expertOptions = getSpecificCodecOptions(
			configuration.getMencoderCodecSpecificConfig(),
			media,
			params,
			filename,
			externalSubtitlesFileName,
			configuration.isMencoderIntelligentSync(),
			false
		);

		if (expertOptions != null) {
			for (String s : expertOptions) {
				if (s.equals("-noass")) {
					foundNoassParam = true;
				}
			}
		}

		StringBuilder sb = new StringBuilder();
		// Set subtitles options
		if (!isDisableSubtitles(params)) {
			int subtitleMargin = 0;
			int userMargin     = 0;

			// Use ASS flag (and therefore ASS font styles) for all subtitled files except vobsub, PGS (Blu-ray Disc) and DVD
			boolean applyAssStyling = params.getSid().getType() != SubtitleType.VOBSUB &&
				params.getSid().getType() != SubtitleType.PGS &&
				configuration.isMencoderAss() &&   // GUI: enable subtitles formating
				!foundNoassParam &&                // GUI: codec specific options
				!isDVD;

			if (applyAssStyling) {
				sb.append("-ass ");

				// GUI: Override ASS subtitles style if requested (always for SRT and TX3G subtitles)
				boolean overrideAssStyle = !configuration.isUseEmbeddedSubtitlesStyle() ||
					params.getSid().getType() == SubtitleType.SUBRIP ||
					params.getSid().getType() == SubtitleType.TX3G;

				if (overrideAssStyle) {
					String assSubColor = configuration.getSubsColor().getMEncoderHexValue();
					sb.append("-ass-color ").append(assSubColor).append(" -ass-border-color 00000000 -ass-font-scale ").append(configuration.getAssScale());

					// Set subtitles font
					if (isNotBlank(configuration.getFont())) {
						/* Set font with -font option, workaround for the bug:
						 * https://github.com/Happy-Neko/ps3mediaserver/commit/52e62203ea12c40628de1869882994ce1065446a#commitcomment-990156
						 */
						sb.append(" -font ").append(quoteArg(configuration.getFont())).append(' ');
						String font = CodecUtil.isFontRegisteredInOS(configuration.getFont());
						if (font != null) {
							sb.append(" -ass-force-style FontName=").append(quoteArg(font)).append(',');
						}

					} else {
						String font = CodecUtil.getDefaultFontPath();
						if (isNotBlank(font)) {
							sb.append(" -font ").append(quoteArg(font)).append(' ');
							String fontName = CodecUtil.isFontRegisteredInOS(font);
							if (fontName != null) {
								sb.append(" -ass-force-style FontName=").append(quoteArg(fontName)).append(',');
							}

						} else {
							sb.append(" -font Arial ");
							sb.append(" -ass-force-style FontName=Arial,");
						}
					}

					/*
					 * Add to the subtitle margin if overscan compensation is being used
					 * This keeps the subtitle text inside the frame instead of in the border
					 */
					if (intOCH > 0) {
						subtitleMargin = (media.getHeight() / 100) * intOCH;
						subtitleMargin /= 2;
					}

					sb.append("Outline=").append(configuration.getAssOutline()).append(",Shadow=").append(configuration.getAssShadow());

					try {
						userMargin = Integer.parseInt(configuration.getAssMargin());
					} catch (NumberFormatException n) {
						LOGGER.debug("Could not parse SSA margin from \"" + configuration.getAssMargin() + "\"");
					}

					subtitleMargin += userMargin;

					sb.append(",MarginV=").append(subtitleMargin).append(' ');
				} else if (intOCH > 0) {
					/*
					 * Add to the subtitle margin
					 * This keeps the subtitle text inside the frame instead of in the border
					 */
					subtitleMargin = (media.getHeight() / 100) * intOCH;
					subtitleMargin /= 2;

					sb.append("-ass-force-style MarginV=").append(subtitleMargin).append(' ');
				}

				// MEncoder is not compiled with fontconfig on Mac OS X, therefore
				// use of the "-ass" option also requires the "-font" option.
				if (Platform.isMac() && !sb.toString().contains(" -font ")) {
					String font = CodecUtil.getDefaultFontPath();

					if (isNotBlank(font)) {
						sb.append("-font ").append(quoteArg(font)).append(' ');
					}
				}

				// Workaround for MPlayer #2041, remove when that bug is fixed
				if (!params.getSid().isEmbedded()) {
					sb.append("-noflip-hebrew ");
				}
			// Use PLAINTEXT formatting
			} else {
				// Set subtitles font
				if (configuration.getFont() != null && configuration.getFont().length() > 0) {
					sb.append(" -font ").append(quoteArg(configuration.getFont())).append(' ');
				} else {
					String font = CodecUtil.getDefaultFontPath();
					if (isNotBlank(font)) {
						sb.append(" -font ").append(quoteArg(font)).append(' ');
					}
				}

				sb.append(" -subfont-text-scale ").append(configuration.getMencoderNoAssScale());
				sb.append(" -subfont-outline ").append(configuration.getMencoderNoAssOutline());
				sb.append(" -subfont-blur ").append(configuration.getMencoderNoAssBlur());

				// Add to the subtitle margin if overscan compensation is being used
				// This keeps the subtitle text inside the frame instead of in the border
				if (intOCH > 0) {
					subtitleMargin = intOCH;
				}

				try {
					userMargin = Integer.parseInt(configuration.getMencoderNoAssSubPos());
				} catch (NumberFormatException n) {
					LOGGER.debug("Could not parse subpos from \"" + configuration.getMencoderNoAssSubPos() + "\"");
				}

				subtitleMargin += userMargin;

				sb.append(" -subpos ").append(100 - subtitleMargin).append(' ');
			}

			// Common subtitle options
			// MEncoder on Mac OS X is compiled without fontconfig support.
			// Appending the flag will break execution, so skip it on Mac OS X.
			if (!Platform.isMac()) {
				// Use fontconfig if enabled
				sb.append('-').append(configuration.isMencoderFontConfig() ? "" : "no").append("fontconfig ");
			}

			// Apply DVD/VOBsub subtitle quality
			if (params.getSid().getType() == SubtitleType.VOBSUB && configuration.getMencoderVobsubSubtitleQuality() != null) {
				String subtitleQuality = configuration.getMencoderVobsubSubtitleQuality();

				sb.append("-spuaa ").append(subtitleQuality).append(' ');
			}

			// External subtitles file
			if (params.getSid().isExternal()) {
				if (!params.getSid().isExternalFileUtf()) {
					String subcp = null;

					// Append -subcp option for non UTF external subtitles
					if (isNotBlank(configuration.getSubtitlesCodepage())) {
						// Manual setting
						subcp = configuration.getSubtitlesCodepage();
					} else if (isNotBlank(SubtitleUtils.getSubCpOptionForMencoder(params.getSid()))) {
						// Autodetect charset (blank mencoder_subcp config option)
						subcp = SubtitleUtils.getSubCpOptionForMencoder(params.getSid());
					}

					if (isNotBlank(subcp)) {
						sb.append("-subcp ").append(subcp).append(' ');
						if (configuration.isMencoderSubFribidi()) {
							sb.append("-fribidi-charset ").append(subcp).append(' ');
						}
					}
				}
			}
		}

		st = new StringTokenizer(sb.toString(), " ");

		int length = overriddenMainArgs.length; // Old length
		overriddenMainArgs = Arrays.copyOf(overriddenMainArgs, overriddenMainArgs.length + st.countTokens());
		boolean handleToken1 = false;

		while (st.hasMoreTokens()) {
			String s = st.nextToken();

			if (handleToken1) {
				s = "-quiet";
				handleToken1 = false;
			}

			if ((!configuration.isMencoderAss() || isDVD) && s.contains("-ass")) {
				s = "-quiet";
				handleToken1 = true;
			}

			overriddenMainArgs[length++] = s;
		}

		List<String> cmdList = new ArrayList<>();

		cmdList.add(getExecutable());

		// Choose which time to seek to
		cmdList.add("-ss");
		cmdList.add((params.getTimeSeek() > 0) ? "" + params.getTimeSeek() : "0");

		if (isDVD) {
			cmdList.add("-dvd-device");
		}

		String frameRateRatio = media.getValidFps(true);
		String frameRateNumber = media.getValidFps(false);

		// Input filename
		if (avisynth && !filename.toLowerCase().endsWith(".iso")) {
			File avsFile = AviSynthMEncoder.getAVSScript(filename, params.getSid(), params.getFromFrame(), params.getToFrame(), frameRateRatio, frameRateNumber, configuration);
			cmdList.add(ProcessUtil.getShortFileNameIfWideChars(avsFile.getAbsolutePath()));
		} else {
			if (params.getStdIn() != null) {
				cmdList.add("-");
			} else {
				if (isDVD) {
					String dvdFileName = filename.replace("\\VIDEO_TS", "");
					cmdList.add(dvdFileName);
				} else {
					cmdList.add(filename);
				}
			}
		}

		if (isDVD) {
			cmdList.add("dvd://" + media.getDvdtrack());
		}

		for (String arg : args()) {
			if (arg.contains("format=mpeg2") && media.getAspectRatioDvdIso() != null && media.getAspectRatioMencoderMpegopts(true) != null) {
				cmdList.add(arg + ":vaspect=" + media.getAspectRatioMencoderMpegopts(true));
			} else {
				cmdList.add(arg);
			}
		}

		if (!dtsRemux && !encodedAudioPassthrough && !pcm && !isAviSynthEngine() && params.getAid() != null && media.getAudioTracksList().size() > 1) {
			cmdList.add("-aid");
			boolean lavf = false; // TODO Need to add support for LAVF demuxing
			cmdList.add("" + (lavf ? params.getAid().getId() + 1 : params.getAid().getId()));
		}

		/*
		 * Handle subtitles
		 *
		 * Try to reconcile the fact that the handling of "Definitely disable subtitles" is spread out
		 * over net.pms.encoders.Player.setAudioAndSubs and here by setting both of MEncoder's "disable
		 * subs" options if any of the internal conditions for disabling subtitles are met.
		 */
		if (isDisableSubtitles(params)) {
			// Ensure that internal subtitles are not automatically loaded
			cmdList.add("-nosub");

			// Ensure that external subtitles are not automatically loaded
			cmdList.add("-noautosub");
		} else {
			// Note: isEmbedded() and isExternal() are mutually exclusive
			if (params.getSid().isEmbedded()) { // internal (embedded) subs
				// Ensure that external subtitles are not automatically loaded
				cmdList.add("-noautosub");

				// Specify which internal subtitle we want
				cmdList.add("-sid");
				cmdList.add("" + params.getSid().getId());
			} else if (externalSubtitlesFileName != null) { // external subtitles
				assert params.getSid().isExternal(); // confirm the mutual exclusion

				// Ensure that internal subtitles are not automatically loaded
				cmdList.add("-nosub");

				if (params.getSid().getType() == SubtitleType.VOBSUB) {
					cmdList.add("-vobsub");
					cmdList.add(externalSubtitlesFileName.substring(0, externalSubtitlesFileName.length() - 4));
					cmdList.add("-slang");
					cmdList.add("" + params.getSid().getLang());
				} else if (
					!params.getMediaRenderer().streamSubsForTranscodedVideo() ||
					!params.getMediaRenderer().isExternalSubtitlesFormatSupported(params.getSid(), dlna)
				) {
					// Only transcode subtitles if they aren't streamable
					cmdList.add("-sub");
					MediaSubtitle convertedSubs = dlna.getMediaSubtitle();
					if (media.is3d()) {
						if (convertedSubs != null && convertedSubs.getConvertedFile() != null) { // subs are already converted to 3D so use them
							cmdList.add(convertedSubs.getConvertedFile().getAbsolutePath().replace(",", "\\,"));
						} else if (params.getSid().getType() != SubtitleType.ASS) { // When subs are not converted and they are not in the ASS format and video is 3D then subs need conversion to 3D
							File subsFilename = SubtitleUtils.getSubtitles(dlna, media, params, configuration, SubtitleType.ASS);
							cmdList.add(subsFilename.getAbsolutePath().replace(",", "\\,"));
						}
					} else {
						cmdList.add(externalSubtitlesFileName.replace(",", "\\,")); // Commas in MEncoder separate multiple subtitle files
					}

					if (params.getSid().isExternalFileUtf()) {
						// Append -utf8 option for UTF-8 external subtitles
						cmdList.add("-utf8");
					}
				}
			}
		}

		// -ofps
		String framerate = (frameRateRatio != null) ? frameRateRatio : "24000/1001"; // where a framerate is required, use the input framerate or 24000/1001
		String ofps = framerate;

		// Optional -fps or -mc
		if (configuration.isMencoderForceFps()) {
			if (!configuration.isFix25FPSAvMismatch()) {
				cmdList.add("-fps");
				cmdList.add(framerate);
			} else if (frameRateRatio != null) { // XXX not sure why this "fix" requires the input to have a valid framerate, but that's the logic in the old (cmdArray) code
				cmdList.add("-mc");
				cmdList.add("0.005");
				ofps = "25";
			}
		}

		// Make MEncoder output framerate correspond to InterFrame
		if (isAviSynthEngine() && configuration.getAvisynthInterFrame() && !"60000/1001".equals(frameRateRatio) && !"50".equals(frameRateRatio) && !"60".equals(frameRateRatio)) {
			ofps = switch (frameRateRatio) {
				case "25" -> "50";
				case "30" -> "60";
				default -> "60000/1001";
			};
		}

		cmdList.add("-ofps");
		cmdList.add(ofps);

		if (filename.toLowerCase().endsWith(".evo")) {
			cmdList.add("-psprobe");
			cmdList.add("10000");
		}

		boolean deinterlace = configuration.isMencoderYadif();

		// Check if the media renderer supports this resolution
		boolean isResolutionTooHighForRenderer = !params.getMediaRenderer().isResolutionCompatibleWithRenderer(media.getWidth(), media.getHeight());

		// Video scaler and overscan compensation
		boolean scaleBool = false;
		if (
			isResolutionTooHighForRenderer ||
			(
				configuration.isMencoderScaler() &&
				(
					configuration.getMencoderScaleX() != 0 ||
					configuration.getMencoderScaleY() != 0
				)
			) ||
			(
				intOCW > 0 ||
				intOCH > 0
			)
		) {
			scaleBool = true;
		}

		int scaleWidth = 0;
		int scaleHeight = 0;
		String vfValue = "";
		if (media.getWidth() > 0 && media.getHeight() > 0) {
			scaleWidth = media.getWidth();
			scaleHeight = media.getHeight();
		}

		double videoAspectRatio = (double) media.getWidth() / (double) media.getHeight();
		double rendererAspectRatio = 1.777777777777778;
		if (params.getMediaRenderer().isMaximumResolutionSpecified()) {
			rendererAspectRatio = (double) params.getMediaRenderer().getMaxVideoWidth() / (double) params.getMediaRenderer().getMaxVideoHeight();
		}

		if ((deinterlace || scaleBool) && !isAviSynthEngine()) {
			StringBuilder vfValueOverscanPrepend = new StringBuilder();
			StringBuilder vfValueOverscanMiddle  = new StringBuilder();
			StringBuilder vfValueVS              = new StringBuilder();
			StringBuilder vfValueComplete        = new StringBuilder();

			String deinterlaceComma = "";

			/*
			 * Implement overscan compensation settings
			 *
			 * This feature takes into account aspect ratio,
			 * making it less blunt than the Video Scaler option
			 */
			if (intOCW > 0 || intOCH > 0) {
				int intOCWPixels = (media.getWidth()  / 100) * intOCW;
				int intOCHPixels = (media.getHeight() / 100) * intOCH;

				scaleWidth  += intOCWPixels;
				scaleHeight += intOCHPixels;

				// See if the video needs to be scaled down
				if (
					params.getMediaRenderer().isMaximumResolutionSpecified() &&
					(
						(scaleWidth > params.getMediaRenderer().getMaxVideoWidth()) ||
						(scaleHeight > params.getMediaRenderer().getMaxVideoHeight())
					)
				) {
					double overscannedAspectRatio = scaleWidth / (double) scaleHeight;

					if (overscannedAspectRatio > rendererAspectRatio) {
						// Limit video by width
						scaleWidth  = params.getMediaRenderer().getMaxVideoWidth();
						scaleHeight = (int) Math.round(params.getMediaRenderer().getMaxVideoWidth() / overscannedAspectRatio);
					} else {
						// Limit video by height
						scaleWidth  = (int) Math.round(params.getMediaRenderer().getMaxVideoHeight() * overscannedAspectRatio);
						scaleHeight = params.getMediaRenderer().getMaxVideoHeight();
					}
				}

				scaleWidth  = convertToModX(scaleWidth, 4);
				scaleHeight = convertToModX(scaleHeight, 4);

				vfValueOverscanPrepend.append("softskip,expand=-").append(intOCWPixels).append(":-").append(intOCHPixels);
				vfValueOverscanMiddle.append(",scale=").append(scaleWidth).append(':').append(scaleHeight);
			}

			/*
			 * Video Scaler and renderer-specific resolution-limiter
			 */
			if (configuration.isMencoderScaler()) {
				// Use the manual, user-controlled scaler
				if (configuration.getMencoderScaleX() != 0) {
					if (configuration.getMencoderScaleX() <= params.getMediaRenderer().getMaxVideoWidth()) {
						scaleWidth = configuration.getMencoderScaleX();
					} else {
						scaleWidth = params.getMediaRenderer().getMaxVideoWidth();
					}
				}

				if (configuration.getMencoderScaleY() != 0) {
					if (configuration.getMencoderScaleY() <= params.getMediaRenderer().getMaxVideoHeight()) {
						scaleHeight = configuration.getMencoderScaleY();
					} else {
						scaleHeight = params.getMediaRenderer().getMaxVideoHeight();
					}
				}

				scaleWidth  = convertToModX(scaleWidth, 4);
				scaleHeight = convertToModX(scaleHeight, 4);

				LOGGER.info("Setting video resolution to: " + scaleWidth + "x" + scaleHeight + ", your Video Scaler setting");

				vfValueVS.append("scale=").append(scaleWidth).append(':').append(scaleHeight);
			} else if (isResolutionTooHighForRenderer) {
				// The video resolution is too big for the renderer so we need to scale it down

				/*
				 * First we deal with some exceptions, then if they are not matched we will
				 * let the renderer limits work.
				 *
				 * This is so, for example, we can still define a maximum resolution of
				 * 1920x1080 in the renderer config file but still support 1920x1088 when
				 * it's needed, otherwise we would either resize 1088 to 1080, meaning the
				 * ugly (unused) bottom 8 pixels would be displayed, or we would limit all
				 * videos to 1088 causing the bottom 8 meaningful pixels to be cut off.
				 */
				if (media.getWidth() == 3840 && media.getHeight() <= 1080) {
					// Full-SBS
					scaleWidth  = 1920;
					scaleHeight = media.getHeight();
				} else if (media.getWidth() == 1920 && media.getHeight() == 2160) {
					// Full-OU
					scaleWidth  = 1920;
					scaleHeight = 1080;
				} else if (media.getWidth() == 1920 && media.getHeight() == 1088) {
					// SAT capture
					scaleWidth  = 1920;
					scaleHeight = 1088;
				} else {
					// Passed the exceptions, now we allow the renderer to define the limits
					if (videoAspectRatio > rendererAspectRatio) {
						scaleWidth  = params.getMediaRenderer().getMaxVideoWidth();
						scaleHeight = (int) Math.round(params.getMediaRenderer().getMaxVideoWidth() / videoAspectRatio);
					} else {
						scaleWidth  = (int) Math.round(params.getMediaRenderer().getMaxVideoHeight() * videoAspectRatio);
						scaleHeight = params.getMediaRenderer().getMaxVideoHeight();
					}
				}

				scaleWidth  = convertToModX(scaleWidth, 4);
				scaleHeight = convertToModX(scaleHeight, 4);

				LOGGER.info("Setting video resolution to: " + scaleWidth + "x" + scaleHeight + ", the maximum your renderer supports");

				vfValueVS.append("scale=").append(scaleWidth).append(':').append(scaleHeight);
			}

			// Put the string together taking into account overscan compensation and video scaler
			if (intOCW > 0 || intOCH > 0) {
				vfValueComplete.append(vfValueOverscanPrepend).append(vfValueOverscanMiddle).append(",harddup");
				LOGGER.info("Setting video resolution to: " + scaleWidth + "x" + scaleHeight + ", to fit your overscan compensation");
			} else {
				vfValueComplete.append(vfValueVS);
			}

			if (deinterlace) {
				deinterlaceComma = ",";
			}

			vfValue = (deinterlace ? "yadif" : "") + (scaleBool ? deinterlaceComma + vfValueComplete : "");
		}

		/*
		 * Make sure the video is mod4 unless the renderer has specified
		 * that it doesn't care, and make sure the aspect ratio is 16/9
		 * if the renderer needs it.
		 *
		 * The PS3 and possibly other renderers sometimes display mod2
		 * videos in black and white with diagonal strips of color.
		 *
		 * TODO: Integrate this with the other stuff so that "expand" only
		 * ever appears once in the MEncoder CMD.
		 */
		if (
			!isDVD &&
			(
				(
					(
						(scaleWidth % 4 != 0) ||
						(scaleHeight % 4 != 0)
					) &&
					!params.getMediaRenderer().isMuxNonMod4Resolution()
				) ||
				(
					(
						params.getMediaRenderer().isKeepAspectRatio() ||
						params.getMediaRenderer().isKeepAspectRatioTranscoding()
					) &&
					!"16:9".equals(media.getAspectRatioContainer())
				)
			) &&
			!configuration.isMencoderScaler()
		) {
			String vfValuePrepend = "expand=";

			if (params.getMediaRenderer().isKeepAspectRatio() || params.getMediaRenderer().isKeepAspectRatioTranscoding()) {
				String resolution = dlna.getResolutionForKeepAR(scaleWidth, scaleHeight);
				scaleWidth = Integer.parseInt(substringBefore(resolution, "x"));
				scaleHeight = Integer.parseInt(substringAfter(resolution, "x"));

				/**
				 * Now we know which resolution we want the video to be, let's see if MEncoder
				 * can be trusted to output it using only the expand filter, or if we need to
				 * be extra careful and use scale too (which is slower).
				 *
				 * For now I'm not sure exactly how MEncoder decides which resolution to
				 * output so this is some cautious math. If someone does extensive testing
				 * in the future it can be made less cautious.
				 */
				if (
					(scaleWidth + 4) > params.getMediaRenderer().getMaxVideoWidth() ||
					(scaleHeight + 4) > params.getMediaRenderer().getMaxVideoHeight()
				) {
					vfValuePrepend += "::::0:16/9,scale=" + scaleWidth + ":" + scaleHeight;
				} else {
					vfValuePrepend += "::::0:16/9:4";
				}
			} else {
				vfValuePrepend += "-" + (scaleWidth % 4) + ":-" + (scaleHeight % 4);
			}

			vfValuePrepend += ",softskip";

			if (isNotBlank(vfValue)) {
				vfValuePrepend += ",";
			}

			vfValue = vfValuePrepend + vfValue;
		}

		if (isNotBlank(vfValue)) {
			cmdList.add("-vf");
			cmdList.add(vfValue);
		}

		if (configuration.getMencoderMT() && !avisynth && !isDVD && !(media.getCodecV() != null && (media.getCodecV().startsWith("mpeg2")))) {
			cmdList.add("-lavdopts");
			cmdList.add("fast");
		}

		boolean disableMc0AndNoskip = false;

		// Process the options for this file in Transcoding Settings -> Mencoder -> Expert Settings: Codec-specific parameters
		// TODO this is better handled by a plugin with scripting support and will be removed

		// the parameters (expertOptions) are processed in 3 passes
		// 1) process expertOptions
		// 2) process cmdList
		// 3) append expertOptions to cmdList
		if (expertOptions != null && expertOptions.length > 0) {
			// remove this option (key) from the cmdList in pass 2.
			// if the boolean value is true, also remove the option's corresponding value
			Map<String, Boolean> removeCmdListOption = new HashMap<>();

			// if this option (key) is defined in cmdList, merge this string value into the
			// option's value in pass 2. the value is a string format template into which the
			// cmdList option value is injected
			Map<String, String> mergeCmdListOption = new HashMap<>();

			// merges that are performed in pass 2 are logged in this map; the key (string) is
			// the option name and the value is a boolean indicating whether the option was merged
			// or not. the map is populated after pass 1 with the options from mergeCmdListOption
			// and all values initialised to false. if an option was merged, it is not appended
			// to cmdList
			Map<String, Boolean> mergedCmdListOption = new HashMap<>();

			// pass 1: process expertOptions
			for (int l = 0; l < expertOptions.length; ++l) {
				switch (expertOptions[l]) {
					case "-noass":
						// remove -ass from cmdList in pass 2.
						// -ass won't have been added in this method (getSpecificCodecOptions
						// has been called multiple times above to check for -noass and -nomux)
						// but it may have been added via the renderer or global MEncoder options.
						// XXX: there are currently 10 other -ass options (-ass-color, -ass-border-color &c.).
						// technically, they should all be removed...
						removeCmdListOption.put("-ass", false); // false: option does not have a corresponding value
						// remove -noass from expertOptions in pass 3
						expertOptions[l] = REMOVE_OPTION;
						break;
					case "-nomux":
						expertOptions[l] = REMOVE_OPTION;
						break;
					case "-mt":
						// not an MEncoder option so remove it from exportOptions.
						// multi-threaded MEncoder is used by default, so this is obsolete (TODO: Remove it from the description)
						expertOptions[l] = REMOVE_OPTION;
						break;
					case "-ofps":
						// replace the cmdList version with the expertOptions version i.e. remove the former
						removeCmdListOption.put("-ofps", true);
						// skip (i.e. leave unchanged) the exportOptions value
						++l;
						break;
					case "-fps":
						removeCmdListOption.put("-fps", true);
						++l;
						break;
					case "-ovc":
						removeCmdListOption.put("-ovc", true);
						++l;
						break;
					case "-channels":
						removeCmdListOption.put("-channels", true);
						++l;
						break;
					case "-oac":
						removeCmdListOption.put("-oac", true);
						++l;
						break;
					case "-quality":
						// XXX like the old (cmdArray) code, this clobbers the old -lavcopts value
						String lavcopts = String.format(
							"autoaspect=1:vcodec=%s:acodec=%s:abitrate=%s:threads=%d:%s",
							vcodec,
							(configuration.isMencoderAc3Fixed() ? "ac3_fixed" : "ac3"),
							CodecUtil.getAC3Bitrate(configuration, params.getAid()),
							configuration.getMencoderMaxThreads(),
							expertOptions[l + 1]
						);

						// append bitrate-limiting options if configured
						lavcopts = addMaximumBitrateConstraints(
							lavcopts,
							media,
							lavcopts,
							params.getMediaRenderer(),
							""
						);

						// a string format with no placeholders, so the cmdList option value is ignored.
						// note: we protect "%" from being interpreted as a format by converting it to "%%",
						// which is then turned back into "%" when the format is processed
						mergeCmdListOption.put("-lavcopts", lavcopts.replace("%", "%%"));
						// remove -quality <value>
						expertOptions[l] = REMOVE_OPTION;
						expertOptions[l + 1] = REMOVE_OPTION;
						++l;
						break;
					case "-mpegopts":
						mergeCmdListOption.put("-mpegopts", "%s:" + expertOptions[l + 1].replace("%", "%%"));
						// merge if cmdList already contains -mpegopts, but don't append if it doesn't (parity with the old (cmdArray) version)
						expertOptions[l] = REMOVE_OPTION;
						expertOptions[l + 1] = REMOVE_OPTION;
						++l;
						break;
					case "-vf":
						mergeCmdListOption.put("-vf", "%s," + expertOptions[l + 1].replace("%", "%%"));
						++l;
						break;
					case "-af":
						mergeCmdListOption.put("-af", "%s," + expertOptions[l + 1].replace("%", "%%"));
						++l;
						break;
					case "-nosync":
						disableMc0AndNoskip = true;
						expertOptions[l] = REMOVE_OPTION;
						break;
					case "-mc":
						disableMc0AndNoskip = true;
						break;
					default:
						break;
				}
			}

			for (String key : mergeCmdListOption.keySet()) {
				mergedCmdListOption.put(key, false);
			}

			// pass 2: process cmdList
			List<String> transformedCmdList = new ArrayList<>();

			for (int ii = 0; ii < cmdList.size(); ++ii) {
				String option = cmdList.get(ii);

				// we remove an option by *not* adding it to transformedCmdList
				if (removeCmdListOption.containsKey(option)) {
					if (isTrue(removeCmdListOption.get(option))) { // true: remove (i.e. don't add) the corresponding value
						++ii;
					}
				} else {
					transformedCmdList.add(option);

					if (mergeCmdListOption.containsKey(option)) {
						String format = mergeCmdListOption.get(option);
						String value = String.format(format, cmdList.get(ii + 1));
						// record the fact that an expertOption value has been merged into this cmdList value
						mergedCmdListOption.put(option, true);
						transformedCmdList.add(value);
						++ii;
					}
				}
			}

			cmdList = transformedCmdList;

			// pass 3: append expertOptions to cmdList
			for (int iii = 0; iii < expertOptions.length; ++iii) {
				String option = expertOptions[iii];

				if (!option.equals(REMOVE_OPTION)) {
					if (isTrue(mergedCmdListOption.get(option))) { // true: this option and its value have already been merged into existing cmdList options
						++iii; // skip the value
					} else {
						cmdList.add(option);
					}
				}
			}
		}

		if ((pcm || dtsRemux || encodedAudioPassthrough || ac3Remux) || (configuration.isMencoderNoOutOfSync() && !disableMc0AndNoskip)) {
			if (configuration.isFix25FPSAvMismatch()) {
				cmdList.add("-mc");
				cmdList.add("0.005");
			} else if (configuration.isMencoderNoOutOfSync() && !disableMc0AndNoskip) {
				cmdList.add("-mc");
				cmdList.add("0");

				if (!params.getMediaRenderer().isDisableMencoderNoskip()) {
					cmdList.add("-noskip");
				}
			}
		}

		if (params.getTimeEnd() > 0) {
			cmdList.add("-endpos");
			cmdList.add("" + params.getTimeEnd());
		}

		// Force srate because MEncoder doesn't like anything other than 48khz for AC-3
		String rate = "" + params.getMediaRenderer().getTranscodedVideoAudioSampleRate();
		if (!pcm && !dtsRemux && !ac3Remux && !encodedAudioPassthrough) {
			cmdList.add("-af");
			String af = "lavcresample=" + rate;
			if (configuration.isMEncoderNormalizeVolume()) {
				af += ":volnorm=1";
			}
			cmdList.add(af);
			cmdList.add("-srate");
			cmdList.add(rate);
		}

		// Add a -cache option for piped media (e.g. rar/zip file entries):
		// https://code.google.com/p/ps3mediaserver/issues/detail?id=911
		if (params.getStdIn() != null) {
			cmdList.add("-cache");
			cmdList.add("8192");
		}

		IPipeProcess pipe = null;

		ProcessWrapperImpl pw;

		if (pcm || dtsRemux || encodedAudioPassthrough) {
			// Transcode video, demux audio, remux with tsMuxeR
			boolean channelsFilterPresent = false;

			for (String s : cmdList) {
				if (isNotBlank(s) && s.startsWith("channels")) {
					channelsFilterPresent = true;
					break;
				}
			}

			if (params.isAvidemux()) {
				pipe = PlatformUtils.INSTANCE.getPipeProcess("mencoder" + System.currentTimeMillis(), (pcm || dtsRemux || encodedAudioPassthrough || ac3Remux) ? null : params);
				params.getInputPipes()[0] = pipe;

				cmdList.add("-o");
				cmdList.add(pipe.getInputPipe());

				if (pcm && !channelsFilterPresent && params.getAid() != null) {
					String mixer = AudioUtils.getLPCMChannelMappingForMencoder(params.getAid());
					if (isNotBlank(mixer)) {
						cmdList.add("-af");
						cmdList.add(mixer);
					}
				}

				String[] cmdArray = new String[cmdList.size()];
				cmdList.toArray(cmdArray);
				pw = new ProcessWrapperImpl(cmdArray, params);

				IPipeProcess videoPipe = PlatformUtils.INSTANCE.getPipeProcess("videoPipe" + System.currentTimeMillis(), "out", "reconnect");
				IPipeProcess audioPipe = PlatformUtils.INSTANCE.getPipeProcess("audioPipe" + System.currentTimeMillis(), "out", "reconnect");

				ProcessWrapper videoPipeProcess = videoPipe.getPipeProcess();
				ProcessWrapper audioPipeProcess = audioPipe.getPipeProcess();

				params.getOutputPipes()[0] = videoPipe;
				params.getOutputPipes()[1] = audioPipe;

				pw.attachProcess(videoPipeProcess);
				pw.attachProcess(audioPipeProcess);
				videoPipeProcess.runInNewThread();
				audioPipeProcess.runInNewThread();
				UMSUtils.sleep(50);
				videoPipe.deleteLater();
				audioPipe.deleteLater();
			} else {
				// remove the -oac switch, otherwise the "too many video packets" errors appear again

				for (ListIterator<String> it = cmdList.listIterator(); it.hasNext();) {
					String option = it.next();

					if (option.equals("-oac")) {
						it.set("-nosound");

						if (it.hasNext()) {
							it.next();
							it.remove();
						}

						break;
					}
				}

				pipe = PlatformUtils.INSTANCE.getPipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

				TsMuxeRVideo ts = (TsMuxeRVideo) EngineFactory.getEngine(StandardEngineId.TSMUXER_VIDEO, false, true);
				File f = new File(CONFIGURATION.getTempFolder(), "ums-tsmuxer.meta");
				String[] cmd = new String[]{ts.getExecutable(), f.getAbsolutePath(), pipe.getInputPipe()};
				pw = new ProcessWrapperImpl(cmd, params);

				PipeIPCProcess ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

				cmdList.add("-o");
				cmdList.add(ffVideoPipe.getInputPipe());

				OutputParams ffparams = new OutputParams(configuration);
				ffparams.setMaxBufferSize(1);
				ffparams.setStdIn(params.getStdIn());

				String[] cmdArray = new String[cmdList.size()];
				cmdList.toArray(cmdArray);
				ProcessWrapperImpl ffVideo = new ProcessWrapperImpl(cmdArray, ffparams);

				ProcessWrapper ffVideoPipeProcess = ffVideoPipe.getPipeProcess();
				pw.attachProcess(ffVideoPipeProcess);
				ffVideoPipeProcess.runInNewThread();
				ffVideoPipe.deleteLater();

				pw.attachProcess(ffVideo);
				ffVideo.runInNewThread();

				String aid = null;
				if (media.getAudioTracksList().size() > 1 && params.getAid() != null) {
					if (media.getContainer() != null && (media.getContainer().equals(FormatConfiguration.AVI) || media.getContainer().equals(FormatConfiguration.FLV))) {
						// TODO confirm (MP4s, OGMs and MOVs already tested: first aid is 0; AVIs: first aid is 1)
						// For AVIs, FLVs and MOVs MEncoder starts audio tracks numbering from 1
						aid = "" + (params.getAid().getId() + 1);
					} else {
						// Everything else from 0
						aid = "" + params.getAid().getId();
					}
				}

				PipeIPCProcess ffAudioPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);
				StreamModifier sm = new StreamModifier();
				sm.setPcm(pcm);
				sm.setDtsEmbed(dtsRemux);
				sm.setEncodedAudioPassthrough(encodedAudioPassthrough);
				sm.setSampleFrequency(48000);
				sm.setBitsPerSample(16);

				String mixer = null;
				if (pcm && !dtsRemux && !encodedAudioPassthrough) {
					mixer = getLPCMChannelMappingForMencoder(params.getAid()); // LPCM always outputs 5.1/7.1 for multichannel tracks. Downmix with player if needed!
				}

				sm.setNbChannels(channels);

				// It seems that -really-quiet prevents MEncoder from stopping the pipe output after some time
				// -mc 0.1 makes the DTS-HD extraction work better with latest MEncoder builds, and has no impact on the regular DTS one
				// TODO: See if these notes are still true, and if so leave specific revisions/release names of the latest version tested.
				String[] ffmpegLPCMextract = new String[]{
					getExecutable(),
					"-ss", "0",
					filename,
					"-really-quiet",
					"-msglevel", "statusline=2",
					"-channels", "" + channels,
					"-ovc", "copy",
					"-of", "rawaudio",
					"-mc", (dtsRemux || encodedAudioPassthrough) ? "0.1" : "0",
					"-noskip",
					(aid == null) ? "-quiet" : "-aid", (aid == null) ? "-quiet" : aid,
					"-oac", (ac3Remux || dtsRemux || encodedAudioPassthrough) ? "copy" : "pcm",
					(isNotBlank(mixer) && !channelsFilterPresent) ? "-af" : "-quiet", (isNotBlank(mixer) && !channelsFilterPresent) ? mixer : "-quiet",
					"-srate", "48000",
					"-o", ffAudioPipe.getInputPipe()
				};

				if (!params.getMediaRenderer().isMuxDTSToMpeg()) { // No need to use the PCM trick when media renderer supports DTS
					ffAudioPipe.setModifier(sm);
				}

				if (media.getDvdtrack() > 0) {
					ffmpegLPCMextract[3] = "-dvd-device";
					ffmpegLPCMextract[4] = filename;
					ffmpegLPCMextract[5] = "dvd://" + media.getDvdtrack();
				} else if (params.getStdIn() != null) {
					ffmpegLPCMextract[3] = "-";
				}

				if (filename.toLowerCase().endsWith(".evo")) {
					ffmpegLPCMextract[4] = "-psprobe";
					ffmpegLPCMextract[5] = "1000000";
				}

				if (params.getTimeSeek() > 0) {
					ffmpegLPCMextract[2] = "" + params.getTimeSeek();
				}

				OutputParams ffaudioparams = new OutputParams(configuration);
				ffaudioparams.setMaxBufferSize(1);
				ffaudioparams.setStdIn(params.getStdIn());
				ProcessWrapperImpl ffAudio = new ProcessWrapperImpl(ffmpegLPCMextract, ffaudioparams);

				params.setStdIn(null);
				try (PrintWriter pwMux = new PrintWriter(f)) {
					pwMux.println("MUXOPT --no-pcr-on-video-pid --no-asyncio --new-audio-pes --vbr --vbv-len=500");
					String videoType = "V_MPEG-2";

					if (params.isNoVideoEncode() && params.getForceType() != null) {
						videoType = params.getForceType();
					}

					String fps = "";
					if (params.getForceFps() != null) {
						fps = "fps=" + params.getForceFps() + ", ";
					}

					String audioType;
					if (ac3Remux) {
						audioType = "A_AC3";
					} else if (dtsRemux) {
						if (params.getMediaRenderer().isMuxDTSToMpeg()) {
							// Renderer can play proper DTS track
							audioType = "A_DTS";
						} else {
							// DTS padded in LPCM trick
							audioType = "A_LPCM";
						}
					} else {
						// DTS padded in LPCM trick
						audioType = "A_LPCM";
					}

					/*
					 * MEncoder bug (confirmed with MEncoder r35003 + FFmpeg 0.11.1)
					 * Audio delay is ignored when playing from file start (-ss 0)
					 * Override with tsmuxer.meta setting
					 */
					String timeshift = "";
					if (params.getAid() != null && mencoderAC3RemuxAudioDelayBug) {
						timeshift = "timeshift=" + params.getAid().getAudioProperties().getAudioDelay() + "ms, ";
					}

					pwMux.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + fps + "level=4.1, insertSEI, contSPS, track=1");
					pwMux.println(audioType + ", \"" + ffAudioPipe.getOutputPipe() + "\", " + timeshift + "track=2");
				}

				ProcessWrapper pipeProcess = pipe.getPipeProcess();
				pw.attachProcess(pipeProcess);
				pipeProcess.runInNewThread();

				UMSUtils.sleep(50);

				pipe.deleteLater();
				params.getInputPipes()[0] = pipe;

				ProcessWrapper ffPipeProcess = ffAudioPipe.getPipeProcess();
				pw.attachProcess(ffPipeProcess);
				ffPipeProcess.runInNewThread();

				UMSUtils.sleep(50);

				ffAudioPipe.deleteLater();
				pw.attachProcess(ffAudio);
				ffAudio.runInNewThread();
			}
		} else {
			boolean directpipe = Platform.isMac() || Platform.isFreeBSD();

			if (directpipe) {
				cmdList.add("-o");
				cmdList.add("-");
				cmdList.add("-really-quiet");
				cmdList.add("-msglevel");
				cmdList.add("statusline=2");
				params.setInputPipes(new IPipeProcess[2]);
			} else {
				pipe = PlatformUtils.INSTANCE.getPipeProcess("mencoder" + System.currentTimeMillis(), (pcm || dtsRemux || encodedAudioPassthrough) ? null : params);
				params.getInputPipes()[0] = pipe;
				cmdList.add("-o");
				cmdList.add(pipe.getInputPipe());
			}

			String[] cmdArray = new String[cmdList.size()];
			cmdList.toArray(cmdArray);

			pw = new ProcessWrapperImpl(cmdArray, params);

			if (!directpipe) {
				ProcessWrapper mkfifoProcess = pipe.getPipeProcess();
				pw.attachProcess(mkfifoProcess);

				/*
				 * It can take a long time for Windows to create a named pipe (and
				 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
				 * the current thread.
				 */
				mkfifoProcess.runInSameThread();

				pipe.deleteLater();
			}
		}

		pw.runInNewThread();

		UMSUtils.sleep(100);

		configuration = prev;
		return pw;
	}

	@Override
	public String mimeType() {
		return HTTPResource.VIDEO_TRANSCODE;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	public static String[] getSpecificCodecOptions(
		String codecParam,
		MediaInfo media,
		OutputParams params,
		String filename,
		String externalSubtitlesFileName,
		boolean enable,
		boolean verifyOnly
	) {
		StringBuilder sb = new StringBuilder();
		String codecs = enable ? DEFAULT_CODEC_CONF_SCRIPT : "";
		codecs += "\n" + codecParam;
		StringTokenizer stLines = new StringTokenizer(codecs, "\n");

		try {
			Interpreter interpreter = new Interpreter();
			interpreter.setStrictJava(true);
			List<String> types = CodecUtil.getPossibleCodecs();
			int rank = 1;

			if (types != null) {
				for (String type : types) {
					int r = rank++;
					interpreter.set("" + type, r);
					String secondaryType = "dummy";

					if ("matroska".equals(type)) {
						secondaryType = "mkv";
						interpreter.set(secondaryType, r);
					} else if ("rm".equals(type)) {
						secondaryType = "rmvb";
						interpreter.set(secondaryType, r);
					} else if ("mpeg2".startsWith(type)) {
						secondaryType = "mpeg2";
						interpreter.set(secondaryType, r);
					} else if ("mpeg1video".equals(type)) {
						secondaryType = "mpeg1";
						interpreter.set(secondaryType, r);
					}

					if (media.getContainer() != null && (media.getContainer().equals(type) || media.getContainer().equals(secondaryType))) {
						interpreter.set("container", r);
					} else if (media.getCodecV() != null && (media.getCodecV().equals(type) || media.getCodecV().equals(secondaryType))) {
						interpreter.set("vcodec", r);
					} else if (params.getAid() != null && params.getAid().getCodecA() != null && params.getAid().getCodecA().equals(type)) {
						interpreter.set("acodec", r);
					}
				}
			} else {
				return null;
			}

			interpreter.set("filename", filename);
			interpreter.set("audio", params.getAid() != null);
			interpreter.set("subtitles", params.getSid() != null);
			interpreter.set("srtfile", externalSubtitlesFileName);

			if (params.getAid() != null) {
				interpreter.set("samplerate", params.getAid().getSampleRate());
			}

			String frameRateNumber = media.getValidFps(false);

			try {
				if (frameRateNumber != null) {
					interpreter.set("framerate", Double.parseDouble(frameRateNumber));
				}
			} catch (NumberFormatException e) {
				LOGGER.debug("Could not parse framerate from \"" + frameRateNumber + "\"");
			}

			interpreter.set("duration", media.getDurationInSeconds());

			if (params.getAid() != null) {
				interpreter.set("channels", params.getAid().getAudioProperties().getNumberOfChannels());
			}

			interpreter.set("height", media.getHeight());
			interpreter.set("width", media.getWidth());

			while (stLines.hasMoreTokens()) {
				String line = stLines.nextToken();

				if (!line.startsWith("#") && line.trim().length() > 0) {
					int separator = line.indexOf("::");

					if (separator > -1) {
						String key = null;

						try {
							key = line.substring(0, separator).trim();
							String value = line.substring(separator + 2).trim();

							if (value.length() > 0) {
								if (key.length() == 0) {
									key = "1 == 1";
								}

								Object result = interpreter.eval(key);

								if (result instanceof Boolean boolval && boolval) {
									sb.append(' ').append(value);
								}
							}
						} catch (EvalError e) {
							LOGGER.debug("Error while executing: " + key + " : " + e.getMessage());

							if (verifyOnly) {
								return new String[]{"@@Error while parsing: " + e.getMessage()};
							}
						}
					} else if (verifyOnly) {
						return new String[]{"@@Malformatted line: " + line};
					}
				}
			}
		} catch (EvalError e) {
			LOGGER.debug("BeanShell error: " + e.getMessage());
		}

		String completeLine = sb.toString();
		ArrayList<String> args = new ArrayList<>();
		StringTokenizer st = new StringTokenizer(completeLine, " ");

		while (st.hasMoreTokens()) {
			String arg = st.nextToken().trim();

			if (arg.length() > 0) {
				args.add(arg);
			}
		}

		String[] definitiveArgs = new String[args.size()];
		args.toArray(definitiveArgs);

		return definitiveArgs;
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		return (
			PlayerUtil.isVideo(resource, Format.Identifier.ISOVOB) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(resource, Format.Identifier.OGG)
		);
	}

	@Override
	public boolean excludeFormat(Format extension) {
		return false;
	}

	@Override
	public boolean isEngineCompatible(Renderer renderer) {
		return true;
	}

	@Override
	public @Nullable ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		executableInfo = testExecutableFile(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		final String arg = "-info:help";
		ExecutableInfoBuilder result = executableInfo.modify();
		try {
			ListProcessWrapperResult output = SimpleProcessWrapper.runProcessListOutput(
				30000,
				1000,
				executableInfo.getPath().toString(),
				arg
			);
			if (output.getError() != null) {
				result.errorType(ExecutableErrorType.GENERAL);
				result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" + output.getError().getMessage());
				result.available(Boolean.FALSE);
				LOGGER.debug("\"{} {}\" failed with error: {}", executableInfo.getPath(), arg, output.getError().getMessage());
				return result.build();
			}
			if (output.getExitCode() == 0) {
				if (!output.getOutput().isEmpty()) {
					Pattern pattern = Pattern.compile("^MEncoder\\s+(.*?)\\s+\\(C\\)", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(output.getOutput().get(0));
					if (matcher.find() && isNotBlank(matcher.group(1))) {
						result.version(new Version(matcher.group(1)));
					}
				}
				result.available(Boolean.TRUE);
			} else {
				NTStatus ntStatus = Platform.isWindows() ? NTStatus.typeOf(output.getExitCode()) : null;
				if (ntStatus != null) {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + "\n\n" + ntStatus);
				} else {
					if (output.getOutput().size() > 3 &&
						StringUtil.hasValue(output.getOutput().get(output.getOutput().size() - 1)) &&
						!StringUtil.hasValue(output.getOutput().get(output.getOutput().size() - 2)) &&
						StringUtil.hasValue(output.getOutput().get(output.getOutput().size() - 3))
					) {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(
							String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" +
							output.getOutput().get(output.getOutput().size() - 3)
						);
					} else {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + Messages.getString("UnknownError"));
					}
				}
				result.available(Boolean.FALSE);
			}
		} catch (InterruptedException e) {
			return null;
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}
}
