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

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.Messages;
import net.pms.configuration.UmsConfiguration;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.media.MediaInfo;
import net.pms.media.MediaLang;
import net.pms.network.HTTPResource;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.*;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// FIXME (breaking change): VLCWebVideo doesn't customize any of this, so everything should be *private*
// TODO (when transcoding to MPEG-2): handle non-MPEG-2 compatible input framerates

/**
 * Use VLC as a backend transcoder. Note that 0.x and 1.x versions are
 * unsupported (and probably will crash). Only the latest version will be
 * supported
 *
 * @author Leon Blakey <lord.quackstar@gmail.com>
 */
public class VLCVideo extends Engine {
	private static final Logger LOGGER = LoggerFactory.getLogger(VLCVideo.class);
	public static final EngineId ID = StandardEngineId.VLC_VIDEO;

	/** The {@link Configuration} key for the custom VLC path. */
	public static final String KEY_VLC_PATH = "vlc_path";

	/** The {@link Configuration} key for the VLC executable type. */
	public static final String KEY_VLC_EXECUTABLE_TYPE = "vlc_executable_type";
	public static final String NAME = "VLC Video";

	// Not to be instantiated by anything but PlayerFactory
	VLCVideo() {
		super(CONFIGURATION.getVLCPaths());
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
		return KEY_VLC_PATH;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_VLC_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public boolean isGPUAccelerationReady() {
		return true;
	}

	@Override
	public boolean isAviSynthEngine() {
		return false;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	@Override
	public String getMimeType() {
		return HTTPResource.VIDEO_TRANSCODE;
	}

	/**
	 * Pick codecs for VLC based on formats the renderer supports;
	 *
	 * @param renderer The {@link Renderer}.
	 * @return The codec configuration
	 */
	protected CodecConfig genConfig(Renderer renderer, EncodingFormat encodingFormat) {
		CodecConfig codecConfig = new CodecConfig();
		boolean isXboxOneWebVideo = renderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;

		if (
			(
				encodingFormat.isTranscodeToWMV() &&
				!renderer.isXbox360()
			) ||
			isXboxOneWebVideo
		) {
			codecConfig.videoCodec = "wmv2";
			codecConfig.audioCodec = "wma";
			codecConfig.container = "asf";
		} else {
			/**
			 * VLC does not support H.265 encoding as of VLC 2.1.5.
			 */
			if (encodingFormat.isTranscodeToH264() || encodingFormat.isTranscodeToH265()) {
				codecConfig.videoCodec = "h264";
				codecConfig.videoRemux = true;
			} else {
				codecConfig.videoCodec = "mp2v";
			}

			if (encodingFormat.isTranscodeToAC3()) {
				codecConfig.audioCodec = "a52";
			} else {
				codecConfig.audioCodec = "mp4a";
			}

			if (encodingFormat.isTranscodeToMPEGTS()) {
				codecConfig.container = "ts";
			} else {
				codecConfig.container = "ps";
			}
		}
		LOGGER.trace("Using " + codecConfig.videoCodec + ", " + codecConfig.audioCodec + ", " + codecConfig.container);

		// This has caused garbled audio, so only enable when told to
		if (renderer.getUmsConfiguration().isVlcAudioSyncEnabled()) {
			codecConfig.extraTrans.put("audio-sync", "");
		}
		return codecConfig;
	}

	protected static class CodecConfig {
		String videoCodec;
		String audioCodec;
		String container;
		HashMap<String, Object> extraTrans = new HashMap<>();
		boolean videoRemux;
	}

	protected Map<String, Object> getEncodingArgs(UmsConfiguration configuration, CodecConfig codecConfig, EncodingFormat encodingFormat, OutputParams params) {
		// See: http://www.videolan.org/doc/streaming-howto/en/ch03.html
		// See: http://wiki.videolan.org/Codec
		Map<String, Object> args = new HashMap<>();

		// Codecs to use
		args.put("vcodec", codecConfig.videoCodec);
		args.put("acodec", codecConfig.audioCodec);

		/**
		 * Bitrate in kbit/s
		 *
		 * TODO: Make this engine smarter with bitrates, see
		 * FFMpegVideo.getVideoBitrateOptions() for our best
		 * implementation of this.
		 */
		if (!codecConfig.videoRemux) {
			args.put("vb", "4096");
		}

		if (codecConfig.audioCodec.equals("mp4a")) {
			args.put("ab", Math.min(configuration.getAudioBitrate(), 320));
		} else {
			args.put("ab", configuration.getAudioBitrate());
		}

		// Video scaling
		args.put("scale", "1.0");

		boolean isXboxOneWebVideo = params.getMediaRenderer().isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;

		/**
		 * Only output 6 audio channels for codecs other than AC-3 because as of VLC
		 * 2.1.5, VLC screws up the channel mapping, making a rear channel go through
		 * a front speaker.
		 * Re-evaluate if they ever fix it.
		 */
		int channels = 2;
		if (
			!isXboxOneWebVideo &&
			params.getAid() != null &&
			params.getAid().getNumberOfChannels() > 2 &&
			configuration.getAudioChannelCount() == 6 &&
			!encodingFormat.isTranscodeToAC3()
		) {
			channels = 6;
		}
		args.put("channels", channels);

		// Static sample rate
		// TODO: Does WMA still need a sample rate of 41000 for Xbox compatibility?
		args.put("samplerate", "48000");

		// Recommended on VLC DVD encoding page
		args.put("strict-rc", null);

		// Enable multi-threading
		args.put("threads", "" + configuration.getNumberOfCpuCores());

		// Hardcode subtitles into video
		args.put("soverlay", null);

		// Add extra args
		args.putAll(codecConfig.extraTrans);

		return args;
	}

	private static int[] getVideoBitrateConfig(String bitrate) {
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

	/**
	 * Returns the video bitrate spec for the current transcode according
	 * to the limits/requirements of the renderer and the user's settings.
	 *
	 * @param dlna
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param EncodingFormat encodingFormat
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the video bitrate options for this transcode
	 */
	private List<String> getVideoBitrateOptions(UmsConfiguration configuration, MediaInfo media, EncodingFormat encodingFormat, OutputParams params) {
		List<String> videoBitrateOptions = new ArrayList<>();

		int[] defaultMaxBitrates = getVideoBitrateConfig(configuration.getMaximumBitrate());
		int[] rendererMaxBitrates = new int[2];

		boolean isXboxOneWebVideo = params.getMediaRenderer().isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;

		if (params.getMediaRenderer().getMaxVideoBitrate() > 0) {
			rendererMaxBitrates = getVideoBitrateConfig(Integer.toString(params.getMediaRenderer().getMaxVideoBitrate()));
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			LOGGER.trace(
				"Using video bitrate limit from {} configuration ({} Mb/s) because " +
				"it is lower than the general configuration bitrate limit ({} Mb/s)",
				params.getMediaRenderer().getRendererName(),
				rendererMaxBitrates[0],
				defaultMaxBitrates[0]
			);
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (params.getMediaRenderer().getCBRVideoBitrate() == 0 && params.getTimeEnd() == 0) {
			// Convert value from Mb to Kb
			defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];

			if (params.getMediaRenderer().isHalveBitrate() && !configuration.isAutomaticMaximumBitrate()) {
				defaultMaxBitrates[0] /= 2;
			}

			int bufSize = 1835;
			boolean bitrateLevel41Limited = false;

			/**
			 * Although the maximum bitrate for H.264 Level 4.1 is
			 * officially 50,000 kbit/s, some 4.1-capable renderers
			 * like the PS3 stutter when video exceeds roughly 31,250
			 * kbit/s.
			 *
			 * We also apply the correct buffer size in this section.
			 */
			if (!isXboxOneWebVideo && (encodingFormat.isTranscodeToH264() || encodingFormat.isTranscodeToH265())) {
				if (
					params.getMediaRenderer().getH264LevelLimit() < 4.2 &&
					defaultMaxBitrates[0] > 31250
				) {
					defaultMaxBitrates[0] = 31250;
					bitrateLevel41Limited = true;
				}
				bufSize = defaultMaxBitrates[0];
			} else {
				if (media.getDefaultVideoTrack() != null && media.getDefaultVideoTrack().isHDVideo()) {
					bufSize = defaultMaxBitrates[0] / 3;
				}

				if (bufSize > 7000) {
					bufSize = 7000;
				}

				if (defaultMaxBitrates[1] > 0) {
					bufSize = defaultMaxBitrates[1];
				}

				if (params.getMediaRenderer().isDefaultVBVSize() && rendererMaxBitrates[1] == 0) {
					bufSize = 1835;
				}
			}

			if (!bitrateLevel41Limited) {
				// Make room for audio
				// TODO: set correct bitrate when remuxing DTS, like in FFMpegVideo
				if (encodingFormat.isTranscodeToAAC()) {
					defaultMaxBitrates[0] -= Math.min(configuration.getAudioBitrate(), 320);
				} else {
					defaultMaxBitrates[0] -= configuration.getAudioBitrate();
				}

				// Round down to the nearest Mb
				defaultMaxBitrates[0] = defaultMaxBitrates[0] / 1000 * 1000;
			}

			videoBitrateOptions.add("--sout-x264-vbv-bufsize");
			videoBitrateOptions.add(String.valueOf(bufSize));

			videoBitrateOptions.add("--sout-x264-vbv-maxrate");
			videoBitrateOptions.add(String.valueOf(defaultMaxBitrates[0]));
		}

		if (isXboxOneWebVideo || (!encodingFormat.isTranscodeToH264() && !encodingFormat.isTranscodeToH265())) {
			// Add MPEG-2 quality settings
			String mpeg2Options = configuration.getMPEG2MainSettingsFFmpeg();
			String mpeg2OptionsRenderer = params.getMediaRenderer().getCustomFFmpegMPEG2Options();

			// Renderer settings take priority over user settings
			if (StringUtils.isNotBlank(mpeg2OptionsRenderer)) {
				mpeg2Options = mpeg2OptionsRenderer;
			} else if (configuration.isAutomaticMaximumBitrate()) {
				// when the automatic bandwidth is used than use the proper automatic MPEG2 setting
				mpeg2Options = params.getMediaRenderer().getAutomaticVideoQuality();
			}

			if (mpeg2Options.contains("Automatic")) {
				if (mpeg2Options.contains("Wireless")) {
					// Lower quality for 720p+ content
					if (media.getWidth() > 1280) {
						mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qmin 2 --sout-avcodec-qmax 7";
					} else if (media.getWidth() > 720) {
						mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qmin 2 --sout-avcodec-qmax 5";
					} else {
						mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qmin 2 --sout-avcodec-qmax 3";
					}
				} else { // set the automatic wired quality
					mpeg2Options = "--sout-x264-keyint 5 --sout-avcodec-qscale 1 --sout-avcodec-qmin 2 --sout-avcodec-qmax 3";
				}
			}

			if (params.getMediaRenderer().isPS3()) {
				// It has been reported that non-PS3 renderers prefer keyint 5 but prefer 25 for PS3 because it lowers the average bitrate
				mpeg2Options = "--sout-x264-keyint 25 --sout-avcodec-qscale 1 --sout-avcodec-qmin 2 --sout-avcodec-qmax 3";
			}

			String[] customOptions = StringUtils.split(mpeg2Options);
			videoBitrateOptions.addAll(new ArrayList<>(Arrays.asList(customOptions)));
		} else {
			// Add x264 quality settings
			String x264CRF = configuration.getx264ConstantRateFactor();
			if (configuration.isAutomaticMaximumBitrate()) {
				x264CRF = params.getMediaRenderer().getAutomaticVideoQuality();
			}

			// Remove comment from the value
			if (x264CRF.contains("/*")) {
				x264CRF = x264CRF.substring(x264CRF.indexOf("/*"));
			}

			if (x264CRF.contains("Automatic")) {
				x264CRF = "16";

				// Lower CRF for 720p+ content
				if (media.getWidth() > 720 && !encodingFormat.isTranscodeToH265()) {
					x264CRF = "19";
				}
			}
			videoBitrateOptions.add("--sout-x264-crf");
			videoBitrateOptions.add(x264CRF);
		}

		return videoBitrateOptions;
	}

	@Override
	public ProcessWrapper launchTranscode(StoreItem item, MediaInfo media, OutputParams params) throws IOException {
		// Use device-specific pms conf
		final Renderer renderer = params.getMediaRenderer();
		final UmsConfiguration configuration = renderer.getUmsConfiguration();
		final String filename = item.getFileName();
		final EncodingFormat encodingFormat = item.getTranscodingSettings().getEncodingFormat();
		boolean isWindows = Platform.isWindows();
		setAudioAndSubs(item, params);

		// Make sure we can play this
		CodecConfig config = genConfig(renderer, encodingFormat);

		IPipeProcess tsPipe = PlatformUtils.INSTANCE.getPipeProcess("VLC" + System.currentTimeMillis() + "." + config.container);
		ProcessWrapper pipeProcess = tsPipe.getPipeProcess();

		// XXX it can take a long time for Windows to create a named pipe
		// (and mkfifo can be slow if /tmp isn't memory-mapped), so start this as early as possible
		pipeProcess.runInNewThread();
		tsPipe.deleteLater();

		params.getInputPipes()[0] = tsPipe;
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);

		List<String> cmdList = new ArrayList<>();
		cmdList.add(getExecutable());
		cmdList.add("-I");
		cmdList.add("dummy");

		/**
		 * Disable hardware acceleration which is enabled by default,
		 * but for hardware acceleration, user must enable it in "VLC Preferences",
		 * until they release documentation for new functionalities introduced in 2.1.4+
		 */
		if (PlatformUtils.INSTANCE.getVlcVersion() != null) {
			Version requiredVersion = new Version("2.1.4");

			if (PlatformUtils.INSTANCE.getVlcVersion().compareTo(requiredVersion) > 0) {
				if (!configuration.isGPUAcceleration()) {
					cmdList.add("--avcodec-hw=disabled");
					LOGGER.trace("Disabled VLC's hardware acceleration.");
				}
			} else if (!configuration.isGPUAcceleration()) {
				LOGGER.debug("Version {} of VLC is too low to handle the way we disable hardware acceleration.",
					PlatformUtils.INSTANCE.getVlcVersion()
				);
			}
		}

		// Useful for the more esoteric codecs people use
		if (configuration.isVlcExperimentalCodecs()) {
			cmdList.add("--sout-avcodec-strict=-2");
		}

		// Stop the DOS box from appearing on windows
		if (isWindows) {
			cmdList.add("--dummy-quiet");
		}

		// File needs to be given before sout, otherwise vlc complains
		cmdList.add(filename);

		String disableSuffix = "track=-1";

		// Handle audio language
		if (params.getAid() != null) {
			// User specified language at the client, acknowledge it
			if (params.getAid().getLang() == null || params.getAid().getLang().equals("und")) {
				// VLC doesn't understand "und", so try to get audio track by ID
				cmdList.add("--audio-track=" + params.getAid().getId());
			} else {
				if (
					StringUtils.isBlank(params.getAid().getLang()) ||
					MediaLang.UND.equals(params.getAid().getLang()) ||
					"loc".equals(params.getAid().getLang())
				) {
					cmdList.add("--audio-track=-1");
				} else {
					cmdList.add("--audio-language=" + params.getAid().getLang());
				}
			}
		} else {
			cmdList.add("--audio-track=-1");
		}

		// Handle subtitle language
		if (params.getSid() != null) { // User specified language at the client, acknowledge it
			if (params.getSid().isExternal()) {
				if (params.getSid().getExternalFile() == null) {
					cmdList.add("--sub-" + disableSuffix);
					LOGGER.error("External subtitles file \"{}\" is unavailable", params.getSid().getName());
				} else if (
					!renderer.streamSubsForTranscodedVideo() ||
					!renderer.isExternalSubtitlesFormatSupported(params.getSid(), item)
				) {
					String externalSubtitlesFileName;

					// External subtitle file
					if (params.getSid().isExternalFileUtf16()) {
						try {
							// Convert UTF-16 -> UTF-8
							File convertedSubtitles = new File(CONFIGURATION.getTempFolder(), "utf8_" + params.getSid().getName());
							FileUtil.convertFileFromUtf16ToUtf8(params.getSid().getExternalFile(), convertedSubtitles);
							externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(convertedSubtitles.getAbsolutePath());
						} catch (IOException e) {
							LOGGER.debug("Error converting file from UTF-16 to UTF-8", e);
							externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(params.getSid().getExternalFile());
						}
					} else {
						externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(params.getSid().getExternalFile());
					}

					if (externalSubtitlesFileName != null) {
						cmdList.add("--sub-file");
						cmdList.add(externalSubtitlesFileName);
					}
				}
			} else if (params.getSid().getLang() != null && !params.getSid().getLang().equals("und")) { // Load by ID (better)
				cmdList.add("--sub-track=" + params.getSid().getId());
			} else { // VLC doesn't understand "und", but does understand a nonexistent track
				cmdList.add("--sub-" + disableSuffix);
			}
		} else {
			cmdList.add("--sub-" + disableSuffix);
		}

		// x264 options
		if (config.videoRemux) {
			cmdList.add("--sout-x264-preset");
			cmdList.add("superfast");

			/**
			 * This option prevents VLC from speeding up transcoding by disabling certain
			 * codec optimizations if the CPU is struggling to keep up.
			 * It is already disabled by default so there is no reason to specify that here,
			 * plus the option doesn't work on versions of VLC from 2.0.7 to 2.1.5.
			 */
			//cmdList.add("--no-sout-avcodec-hurry-up");

			cmdList.addAll(getVideoBitrateOptions(configuration, media, encodingFormat, params));
		}

		// Skip forward if necessary
		if (params.getTimeSeek() != 0) {
			cmdList.add("--start-time");
			cmdList.add(String.valueOf(params.getTimeSeek()));
		}

		// Generate encoding args
		String separator = "";
		StringBuilder encodingArgsBuilder = new StringBuilder();
		for (Map.Entry<String, Object> curEntry : getEncodingArgs(configuration, config, encodingFormat, params).entrySet()) {
			encodingArgsBuilder.append(separator);
			encodingArgsBuilder.append(curEntry.getKey());

			if (curEntry.getValue() != null) {
				encodingArgsBuilder.append('=');
				encodingArgsBuilder.append(curEntry.getValue());
			}

			separator = ",";
		}

		// Add our transcode options
		String transcodeSpec = String.format(
			"#transcode{%s}:standard{access=file,mux=%s,dst='%s%s'}",
			encodingArgsBuilder.toString(),
			config.container,
			(isWindows ? "\\\\" : ""),
			tsPipe.getInputPipe()
		);
		cmdList.add("--sout");
		cmdList.add(transcodeSpec);

		// Force VLC to exit when finished
		cmdList.add("vlc://quit");

		// Pass to process wrapper
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(pipeProcess);

		// TODO: Why is this here?
		UMSUtils.sleep(150);

		pw.runInNewThread();
		return pw;
	}

	@Override
	public boolean isCompatible(StoreItem item) {
		// Only handle local video - not web video or audio
		return (
			PlayerUtil.isVideo(item, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(item, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(item, Format.Identifier.OGG)
		);
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return encodingFormat.isVideoFormat() && !encodingFormat.isTranscodeToHLS();
	}

	@Override
	public boolean excludeFormat(Format extension) {
		return false;
	}

	@Override
	public @Nullable ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		executableInfo = testExecutableFile(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		ExecutableInfoBuilder result = executableInfo.modify();
		if (Platform.isWindows()) {
			if (executableInfo.getPath().isAbsolute() && executableInfo.getPath().equals(PlatformUtils.INSTANCE.getVlcPath())) {
				result.version(PlatformUtils.INSTANCE.getVlcVersion());
			}
			result.available(Boolean.TRUE);
		} else {
			final String arg = "--version";
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
						Pattern pattern = Pattern.compile("VLC version\\s+[^\\(]*\\(([^\\)]*)", Pattern.CASE_INSENSITIVE);
						Matcher matcher = pattern.matcher(output.getOutput().get(0));
						if (matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
							result.version(new Version(matcher.group(1)));
						}
					}
					result.available(Boolean.TRUE);
				} else {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("TranscodingEngineNotAvailableExitCode"), this, output.getExitCode()));
					result.available(Boolean.FALSE);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return null;
			}
		}
		Version version = result.version();
		if (version != null) {
			Version requiredVersion = new Version("2.0.2");
			if (version.compareTo(requiredVersion) <= 0) {
				result.errorType(ExecutableErrorType.GENERAL);
				result.errorText(String.format(Messages.getString("OnlyVersionXAboveSupported"), requiredVersion, this));
				result.available(Boolean.FALSE);
				LOGGER.warn(String.format(Messages.getRootString("OnlyVersionXAboveSupported"), requiredVersion, this));
			}
		} else if (result.available() != null && result.available()) {
			LOGGER.warn("Could not parse VLC version, the version might be too low (< 2.0.2)");
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}
}
