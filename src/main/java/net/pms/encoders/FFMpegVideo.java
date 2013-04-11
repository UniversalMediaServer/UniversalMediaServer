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
package net.pms.encoders;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.configuration.RendererConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAMediaSubtitle;
import net.pms.dlna.DLNAResource;
import net.pms.dlna.InputFile;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.formats.v2.SubtitleUtils;
import net.pms.io.OutputParams;
import net.pms.io.PipeIPCProcess;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.StreamModifier;
import net.pms.network.HTTPResource;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Pure FFmpeg video player.
 *
 * Design note:
 *
 * Helper methods that return lists of <code>String</code>s representing options are public
 * to facilitate composition e.g. a custom engine (plugin) that uses tsMuxeR for videos without
 * subtitles and FFmpeg otherwise needs to compose and call methods on both players.
 *
 * To avoid API churn, and to provide wiggle room for future functionality, all of these methods
 * take RendererConfiguration (renderer) and DLNAMediaInfo (media) parameters, even if one or
 * both of these parameters are unused.
 */
public class FFMpegVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);
	private static final String DEFAULT_QSCALE = "3";
	protected static PmsConfiguration configuration;
	
	@Deprecated
	public FFMpegVideo() {
		this(PMS.getConfiguration());
	}
	
	public FFMpegVideo(PmsConfiguration configuration) {
		FFMpegVideo.configuration = configuration;
	}

	// FIXME we have an id() accessor for this; no need for the field to be public
	@Deprecated
	public static final String ID = "ffmpegvideo";

	/**
	 * Returns a list of strings representing the rescale options for this transcode i.e. the ffmpeg -vf
	 * options used to resize a video that's too wide and/or high for the specified renderer.
	 * If the renderer has no size limits, or there's no media metadata, or the video is within the renderer's
	 * size limits, an empty list is returned.
	 *
	 * @param renderer the DLNA renderer the video is being streamed to
	 * @param media metadata for the DLNA resource which is being transcoded
	 * @return a {@link List} of <code>String</code>s representing the rescale options for this video,
	 * or an empty list if the video doesn't need to be resized.
	 */
	public List<String> getVideoFilterOptions(RendererConfiguration renderer, DLNAMediaInfo media, OutputParams params) throws IOException {
		List<String> videoFilterOptions = new ArrayList<>();
		String subsOption = null;
		String padding = null;
		String externalSubtitlesFileName;

		boolean isResolutionTooHighForRenderer = renderer.isVideoRescale() && // renderer defines a max width/height
			(media != null && media.isMediaparsed()) &&
			(
				(media.getWidth() > renderer.getMaxVideoWidth()) ||
				(media.getHeight() > renderer.getMaxVideoHeight())
			);

		if (params.sid != null && !configuration.isDisableSubtitles() && params.sid.isExternal()) {
			if (params.sid.isExternalFileUtf16()) {
				// convert UTF-16 -> UTF-8
				File convertedSubtitles = new File(configuration.getTempFolder(), "utf8_" + params.sid.getExternalFile().getName());
				FileUtil.convertFileFromUtf16ToUtf8(params.sid.getExternalFile(), convertedSubtitles);
				externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(convertedSubtitles.getAbsolutePath());
			} else {
				externalSubtitlesFileName = ProcessUtil.getShortFileNameIfWideChars(params.sid.getExternalFile().getAbsolutePath());
			}

			if (params.sid.getType() == SubtitleType.SUBRIP) {
				try  {
					externalSubtitlesFileName = SubtitleUtils.ConvertSrtToAss(externalSubtitlesFileName, params.timeseek, configuration).getAbsolutePath();
				} catch (IOException e) {
					LOGGER.debug("Converting to ASS file raised an error: {}", e.getMessage());
				}

			}
			
			if (externalSubtitlesFileName != null) {
				StringBuilder s = new StringBuilder();
				CharacterIterator it = new StringCharacterIterator(externalSubtitlesFileName);

				for (char ch = it.first(); ch != CharacterIterator.DONE; ch = it.next()) {
					switch (ch) {
						case ':':
							s.append("\\\\:");
							break;
						case '\\':
							s.append("/");
							break;
						case ']':
						case '[':
							s.append("\\");
						default:
							s.append(ch);
							break;
					}
				}

				String subsFile = s.toString();
				subsFile = subsFile.replace(",", "\\,");
				subsOption = "ass=" + subsFile;
			}
		}

		if (renderer.isKeepAspectRatio() && renderer.isRescaleByRenderer()) {
			
			if (media != null && media.isMediaparsed() && media.getHeight() != 0 &&
				(media.getWidth() / (double) media.getHeight()) >= (16 / (double) 9)) {
				padding = "pad=iw:iw/(16/9):0:(oh-ih)/2";
			} else {
				padding = "pad=ih*(16/9):ih:(ow-iw)/2:0";
			}
		}

		String rescaleSpec = null;

		if (isResolutionTooHighForRenderer || (renderer.isKeepAspectRatio() && !renderer.isRescaleByRenderer())) {
			rescaleSpec = String.format(
				// http://stackoverflow.com/a/8351875
				"scale=iw*min(%1$d/iw\\,%2$d/ih):ih*min(%1$d/iw\\,%2$d/ih),pad=%1$d:%2$d:(%1$d-iw)/2:(%2$d-ih)/2",
				renderer.getMaxVideoWidth(),
				renderer.getMaxVideoHeight()
			);
		}
		
		String overrideVF = renderer.getFFmpegVideoFilterOverride();

		if (rescaleSpec != null || padding != null || overrideVF != null || subsOption != null) {
			videoFilterOptions.add("-vf");
			StringBuilder filterParams = new StringBuilder();
			
			if (overrideVF != null) {
				filterParams.append(overrideVF);
				if (subsOption != null) {
					filterParams.append(", ");
				}
			} else {
				if (rescaleSpec != null) {
					filterParams.append(rescaleSpec);
					if (subsOption != null || padding != null) {
						filterParams.append(", ");
					}
				}

				if (padding != null && rescaleSpec == null) {
					filterParams.append(padding);
					if (subsOption != null) {
						filterParams.append(", ");
					}
				}
			}

			if (subsOption != null) {
				filterParams.append(subsOption);
			}

			videoFilterOptions.add(filterParams.toString());
		}

		return videoFilterOptions;
	}

	@Deprecated
	public List<String> getTranscodeVideoOptions(RendererConfiguration renderer, DLNAMediaInfo media, OutputParams params) {
		return getTranscodeVideoOptions(renderer, media, params, null);
	}

	/**
	 * Takes a renderer and returns a list of <code>String</code>s representing FFmpeg output options
	 * (i.e. options that define the output file's video codec, audio codec and container)
	 * compatible with the renderer's <code>TranscodeVideo</code> profile.
	 *
	 * @param renderer The {@link RendererConfiguration} instance whose <code>TranscodeVideo</code> profile is to be processed.
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params output parameters
	 * @param fileName the name of the file
	 *
	 * @return a {@link List} of <code>String</code>s representing the FFmpeg output parameters for the renderer according
	 * to its <code>TranscodeVideo</code> profile.
	 */
	public List<String> getTranscodeVideoOptions(RendererConfiguration renderer, DLNAMediaInfo media, OutputParams params, String fileName) {
		List<String> transcodeOptions = new ArrayList<>();

		if (renderer.isTranscodeToWMV()) { // WMV
			transcodeOptions.add("-c:v");
			transcodeOptions.add("wmv2");

			transcodeOptions.add("-c:a");
			transcodeOptions.add("wmav2");

			transcodeOptions.add("-f");
			transcodeOptions.add("asf");
		} else { // MPEGPSAC3, MPEGTSAC3 or H264TSAC3
			final boolean isTsMuxeRVideoEngineEnabled = configuration.getEnginesAsList(PMS.get().getRegistry()).contains(TsMuxeRVideo.ID);

			// Output audio codec
			dtsRemux = isTsMuxeRVideoEngineEnabled &&
				configuration.isDTSEmbedInPCM() &&
				params.aid != null &&
				params.aid.isDTS() &&
				!avisynth() &&
				renderer.isDTSPlayable();

			if (configuration.isRemuxAC3() && params.aid != null && params.aid.isAC3() && !avisynth() && renderer.isTranscodeToAC3()) {
				// AC-3 remux
				transcodeOptions.add("-c:a");
				transcodeOptions.add("copy");
			} else {
				if (dtsRemux) {
					// Audio is added in a separate process later
					transcodeOptions.add("-an");
				} else if (type() == Format.AUDIO) {
					// Skip
				} else {
					transcodeOptions.add("-c:a");
					transcodeOptions.add("ac3");
				}
			}

			InputFile newInput = null;
			if (fileName != null) {
				newInput = new InputFile();
				newInput.setFilename(fileName);
				newInput.setPush(params.stdin);
			}

			// Output video codec
			if (
				media.isMediaparsed() &&
				params.sid == null &&
				!avisynth() &&
				(
					(
						newInput != null &&
						media.isVideoWithinH264LevelLimits(newInput, params.mediaRenderer)
					) ||
					!params.mediaRenderer.isH264Level41Limited()
				) &&
				media.isMuxable(params.mediaRenderer) &&
				configuration.isFFmpegMuxWhenCompatible() &&
				params.mediaRenderer.isMuxH264MpegTS()
			) {
				transcodeOptions.add("-c:v");
				transcodeOptions.add("copy");
				transcodeOptions.add("-bsf");
				transcodeOptions.add("h264_mp4toannexb");
				transcodeOptions.add("-fflags");
				transcodeOptions.add("+genpts");

				videoRemux = true;
			} else if (renderer.isTranscodeToH264TSAC3()) {
				transcodeOptions.add("-c:v");
				transcodeOptions.add("libx264");
				transcodeOptions.add("-crf");
				transcodeOptions.add("20");
				transcodeOptions.add("-preset");
				transcodeOptions.add("superfast");
			} else if (!dtsRemux) {
				transcodeOptions.add("-c:v");
				transcodeOptions.add("mpeg2video");
			}

			// Output file format
			transcodeOptions.add("-f");
			if (dtsRemux) {
				if (videoRemux) {
					transcodeOptions.add("rawvideo");
				} else {
					transcodeOptions.add("mpeg2video");
				}
			} else if (renderer.isTranscodeToMPEGTSAC3() || renderer.isTranscodeToH264TSAC3() || videoRemux) { // MPEGTSAC3
				transcodeOptions.add("mpegts");
			} else { // default: MPEGPSAC3
				transcodeOptions.add("vob");
			}
		}

		return transcodeOptions;
	}

	/**
	 * Takes a renderer and metadata for the current video and returns the video bitrate spec for the current transcode according to
	 * the limits/requirements of the renderer.
	 *
	 * @param renderer a {@link RendererConfiguration} instance representing the renderer being streamed to
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @return a {@link List} of <code>String</code>s representing the video bitrate options for this transcode
	 */
	public List<String> getVideoBitrateOptions(RendererConfiguration renderer, DLNAMediaInfo media) { // media is currently unused
		List<String> videoBitrateOptions = new ArrayList<>();
		String sMaxVideoBitrate = renderer.getMaxVideoBitrate(); // currently Mbit/s
		int iMaxVideoBitrate = 0;

		if (sMaxVideoBitrate != null) {
			try {
				iMaxVideoBitrate = Integer.parseInt(sMaxVideoBitrate);
			} catch (NumberFormatException nfe) {
				LOGGER.error("Can't parse max video bitrate", nfe); // XXX this should be handled in RendererConfiguration
			}
		}

		if (iMaxVideoBitrate == 0) { // unlimited: try to preserve the bitrate
			videoBitrateOptions.add("-q:v"); // video qscale
			videoBitrateOptions.add(DEFAULT_QSCALE);
		} else { // limit the bitrate FIXME untested
			// convert megabits-per-second (as per the current option name: MaxVideoBitrateMbps) to bps
			// FIXME rather than dealing with megabit vs mebibit issues here, this should be left up to the client i.e.
			// the renderer.conf unit should be bits-per-second (and the option should be renamed: MaxVideoBitrateMbps -> MaxVideoBitrate)
			videoBitrateOptions.add("-maxrate");
			videoBitrateOptions.add("" + iMaxVideoBitrate * 1000 * 1000);
		}

		return videoBitrateOptions;
	}

	/**
	 * Takes a renderer and metadata for the current video and returns the audio bitrate spec for the current transcode according to
	 * the limits/requirements of the renderer.
	 *
	 * @param renderer a {@link RendererConfiguration} instance representing the renderer being streamed to
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @return a {@link List} of <code>String</code>s representing the audio bitrate options for this transcode
	 */
	public List<String> getAudioBitrateOptions(RendererConfiguration renderer, DLNAMediaInfo media) {
		List<String> audioBitrateOptions = new ArrayList<>();

		audioBitrateOptions.add("-q:a");
		audioBitrateOptions.add(DEFAULT_QSCALE);

		return audioBitrateOptions;
	}

	protected boolean dtsRemux;
	protected boolean ac3Remux;
	protected boolean videoRemux;

	@Override
	public int purpose() {
		return VIDEO_SIMPLEFILE_PLAYER;
	}

	@Override
	// TODO make this static so it can replace ID, instead of having both
	public String id() {
		return ID;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public boolean avisynth() {
		return false;
	}

	public String initialString() {
		String threads = "";
		if (configuration.isFfmpegMultithreading()) {
			threads = " -threads " + configuration.getNumberOfCpuCores();
		}
		return configuration.getFfmpegSettings() + threads;
	}

	@Override
	public String name() {
		return "FFmpeg";
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	// unused; return this array for backwards-compatibility
	@Deprecated
	protected String[] getDefaultArgs() {
		List<String> defaultArgsList = new ArrayList<>();

		defaultArgsList.add("-loglevel");
		defaultArgsList.add("warning");

		String[] defaultArgsArray = new String[defaultArgsList.size()];
		defaultArgsList.toArray(defaultArgsArray);

		return defaultArgsArray;
	}

	private int[] getVideoBitrateConfig(String bitrate) {
		int bitrates[] = new int[2];

		if (bitrate.contains("(") && bitrate.contains(")")) {
			bitrates[1] = Integer.parseInt(bitrate.substring(bitrate.indexOf("(") + 1, bitrate.indexOf(")")));
		}

		if (bitrate.contains("(")) {
			bitrate = bitrate.substring(0, bitrate.indexOf("(")).trim();
		}

		if (isBlank(bitrate)) {
			bitrate = "0";
		}

		bitrates[0] = (int) Double.parseDouble(bitrate);

		return bitrates;
	}

	@Override
	@Deprecated
	public String[] args() {
		return getDefaultArgs(); // unused; return this array for for backwards compatibility
	}

	// XXX hardwired to false and not referenced anywhere else in the codebase
	@Deprecated
	public boolean mplayer() {
		return false;
	}

	@Override
	public String mimeType() {
		return HTTPResource.VIDEO_TRANSCODE;
	}

	@Override
	public String executable() {
		return configuration.getFfmpegPath();
	}

	@Override
	public boolean isGPUAccelerationReady() {
		return false;
	}

	@Override
	public ProcessWrapper launchTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		int nThreads = configuration.getNumberOfCpuCores();
		List<String> cmdList = new ArrayList<>();
		RendererConfiguration renderer = params.mediaRenderer;
		setAudioAndSubs(fileName, media, params, configuration);

		boolean avisynth = avisynth();

		cmdList.add(executable());

		// Prevent FFmpeg timeout
		cmdList.add("-y");

		cmdList.add("-loglevel");
		
		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add("" + params.timeseek);
		}

		// decoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		final boolean isTsMuxeRVideoEngineEnabled = configuration.getEnginesAsList(PMS.get().getRegistry()).contains(TsMuxeRVideo.ID);

		ac3Remux = false;
		dtsRemux = false;

		if (configuration.isRemuxAC3() && params.aid != null && params.aid.isAC3() && !avisynth() && renderer.isTranscodeToAC3()) {
			// AC-3 remux takes priority
			ac3Remux = true;
		} else {
			// Now check for DTS remux and LPCM streaming
			dtsRemux = isTsMuxeRVideoEngineEnabled &&
				configuration.isDTSEmbedInPCM() &&
				params.aid != null &&
				params.aid.isDTS() &&
				!avisynth() &&
				params.mediaRenderer.isDTSPlayable();
		}

		String frameRateRatio = media.getValidFps(true);
		String frameRateNumber = media.getValidFps(false);

		// Input filename
		cmdList.add("-i");
		if (avisynth && !fileName.toLowerCase().endsWith(".iso")) {
			File avsFile = AviSynthFFmpeg.getAVSScript(fileName, params.sid, params.fromFrame, params.toFrame, frameRateRatio, frameRateNumber);
			cmdList.add(ProcessUtil.getShortFileNameIfWideChars(avsFile.getAbsolutePath()));
		} else {
			cmdList.add(fileName);

			if (media.getAudioTracksList().size() > 1) {
				// Set the video stream
				cmdList.add("-map");
				cmdList.add("0:v");

				// Set the proper audio stream
				cmdList.add("-map");
				cmdList.add("0:a:" + (media.getAudioTracksList().indexOf(params.aid)));
			}
		}

		// Encoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		if (params.timeend > 0) {
			cmdList.add("-t");
			cmdList.add("" + params.timeend);
		}

		// add video bitrate options
		// TODO: Integrate our (more comprehensive) code with this function
		// from PMS to make keeping synchronised easier.
		// Until then, leave the following line commented out.
		// cmdList.addAll(getVideoBitrateOptions(renderer, media));

		// add audio bitrate options
		// TODO: Integrate our (more comprehensive) code with this function
		// from PMS to make keeping synchronised easier.
		// Until then, leave the following line commented out.
		// cmdList.addAll(getAudioBitrateOptions(renderer, media));

		// if the source is too large for the renderer, resize it
		// and/or add subtitles to video filter
		// FFmpeg must be compiled with --enable-libass parameter
		cmdList.addAll(getVideoFilterOptions(renderer, media, params));

		int defaultMaxBitrates[] = getVideoBitrateConfig(configuration.getMaximumBitrate());
		int rendererMaxBitrates[] = new int[2];

		if (renderer.getMaxVideoBitrate() != null) {
			rendererMaxBitrates = getVideoBitrateConfig(renderer.getMaxVideoBitrate());
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if ((defaultMaxBitrates[0] == 0 && rendererMaxBitrates[0] > 0) || rendererMaxBitrates[0] < defaultMaxBitrates[0] && rendererMaxBitrates[0] > 0) {
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (params.mediaRenderer.getCBRVideoBitrate() == 0 && defaultMaxBitrates[0] > 0) {
			// Convert value from Mb to Kb
			defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];

			// Halve it since it seems to send up to 1 second of video in advance
			defaultMaxBitrates[0] = defaultMaxBitrates[0] / 2;

			int bufSize = 1835;
			// x264 uses different buffering math than MPEG-2
			if (!renderer.isTranscodeToH264TSAC3()) {
				if (media.isHDVideo()) {
					bufSize = defaultMaxBitrates[0] / 3;
				}

				if (bufSize > 7000) {
					bufSize = 7000;
				}

				if (defaultMaxBitrates[1] > 0) {
					bufSize = defaultMaxBitrates[1];
				}

				if (params.mediaRenderer.isDefaultVBVSize() && rendererMaxBitrates[1] == 0) {
					bufSize = 1835;
				}
			}

			// Make room for audio
			if (dtsRemux) {
				defaultMaxBitrates[0] = defaultMaxBitrates[0] - 1510;
			} else {
				defaultMaxBitrates[0] = defaultMaxBitrates[0] - configuration.getAudioBitrate();
			}

			// Round down to the nearest Mb
			defaultMaxBitrates[0] = defaultMaxBitrates[0] / 1000 * 1000;

			// FFmpeg uses bytes for inputs instead of kbytes like MEncoder
			bufSize = bufSize * 1000;
			defaultMaxBitrates[0] = defaultMaxBitrates[0] * 1000;

			/**
			 * Level 4.1-limited renderers like the PS3 can stutter when H.264 video exceeds
			 * this bitrate
			 */
			if (renderer.isTranscodeToH264TSAC3() || videoRemux) {
				if (
					params.mediaRenderer.isH264Level41Limited() &&
					defaultMaxBitrates[0] > 31250000
				) {
					defaultMaxBitrates[0] = 31250000;
				}
				bufSize = defaultMaxBitrates[0];
			}

			cmdList.add("-bufsize");
			cmdList.add("" + bufSize);

			cmdList.add("-maxrate");
			cmdList.add("" + defaultMaxBitrates[0]);
		}

		int channels;
		if (renderer.isTranscodeToWMV()) {
			channels = 2;
		} else if (ac3Remux) {
			channels = params.aid.getAudioProperties().getNumberOfChannels(); // AC-3 remux
		} else if (dtsRemux) {
			channels = 2;
		} else {
			channels = configuration.getAudioChannelCount(); // 5.1 max for AC-3 encoding
		}
		cmdList.add("-ac");
		cmdList.add("" + channels);

		// Audio bitrate
		if (!ac3Remux && !dtsRemux && !(type() == Format.AUDIO)) {
			cmdList.add("-ab");
			// Check if audio bitrate meets mp2 specification
			// TODO: Is this needed?
			if (!renderer.isTranscodeToMPEGPSAC3() && configuration.getAudioBitrate() <= 384) {
				cmdList.add(configuration.getAudioBitrate() + "k");
			} else {
				cmdList.add("384k");
			}
		}

		if (params.timeseek > 0) {
			cmdList.add("-copypriorss");
			cmdList.add("0");
			cmdList.add("-avoid_negative_ts");
			cmdList.add("1");
		}

		// Add MPEG-2 quality settings
		if (!renderer.isTranscodeToH264TSAC3() && !videoRemux) {
			String[] customOptions = StringUtils.split(configuration.getFfmpegSettings());
			cmdList.addAll(new ArrayList<>(Arrays.asList(customOptions)));
		}

		// Add the output options (-f, -acodec, -vcodec)
		cmdList.addAll(getTranscodeVideoOptions(renderer, media, params, fileName));

		// Add custom options
		if (StringUtils.isNotEmpty(renderer.getCustomFFmpegOptions())) {
			parseOptions(renderer.getCustomFFmpegOptions(), cmdList);
		}

		if (!dtsRemux) {
			cmdList.add("pipe:");
		}

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		cmdArray = finalizeTranscoderArgs(
			fileName,
			dlna,
			media,
			params,
			cmdArray
		);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		if (dtsRemux) {
			PipeProcess pipe;
			pipe = new PipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

			TsMuxeRVideo ts = new TsMuxeRVideo(configuration);
			File f = new File(configuration.getTempFolder(), "pms-tsmuxer.meta");
			String cmd[] = new String[]{ ts.executable(), f.getAbsolutePath(), pipe.getInputPipe() };
			pw = new ProcessWrapperImpl(cmd, params);

			PipeIPCProcess ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			cmdList.add(ffVideoPipe.getInputPipe());

			OutputParams ffparams = new OutputParams(configuration);
			ffparams.maxBufferSize = 1;
			ffparams.stdin = params.stdin;

			String[] cmdArrayDts = new String[cmdList.size()];
			cmdList.toArray(cmdArrayDts);

			cmdArrayDts = finalizeTranscoderArgs(
				fileName,
				dlna,
				media,
				params,
				cmdArrayDts
			);

			ProcessWrapperImpl ffVideo = new ProcessWrapperImpl(cmdArrayDts, ffparams);

			ProcessWrapper ff_video_pipe_process = ffVideoPipe.getPipeProcess();
			pw.attachProcess(ff_video_pipe_process);
			ff_video_pipe_process.runInNewThread();
			ffVideoPipe.deleteLater();

			pw.attachProcess(ffVideo);
			ffVideo.runInNewThread();

			PipeIPCProcess ffAudioPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegaudio01", System.currentTimeMillis() + "audioout", false, true);
			StreamModifier sm = new StreamModifier();
			sm.setPcm(false);
			sm.setDtsEmbed(dtsRemux);
			sm.setSampleFrequency(48000);
			sm.setBitsPerSample(16);
			sm.setNbChannels(channels);

			String ffmpegLPCMextract[] = new String[]{
				executable(),
				"-y",
				"-ss", "0",
				"-i", fileName,
				"-ac", "" + channels,
				"-f", "dts",
				"-c:a", "copy",
				ffAudioPipe.getInputPipe()
			};

			if (!params.mediaRenderer.isMuxDTSToMpeg()) { // No need to use the PCM trick when media renderer supports DTS
				ffAudioPipe.setModifier(sm);
			}

			if (params.stdin != null) {
				ffmpegLPCMextract[4] = "-";
			}

			if (params.timeseek > 0) {
				ffmpegLPCMextract[3] = "" + params.timeseek;
			}

			OutputParams ffaudioparams = new OutputParams(configuration);
			ffaudioparams.maxBufferSize = 1;
			ffaudioparams.stdin = params.stdin;
			ProcessWrapperImpl ffAudio = new ProcessWrapperImpl(ffmpegLPCMextract, ffaudioparams);

			params.stdin = null;
			try (PrintWriter pwMux = new PrintWriter(f)) {
				pwMux.println("MUXOPT --no-pcr-on-video-pid --no-asyncio --new-audio-pes --vbr --vbv-len=500");
				String videoType = "V_MPEG-2";

				if (videoRemux) {
					videoType = "V_MPEG4/ISO/AVC";
				}

				if (params.no_videoencode && params.forceType != null) {
					videoType = params.forceType;
				}

				String fps = "";
				if (params.forceFps != null) {
					fps = "fps=" + params.forceFps + ", ";
				}

				String audioType = "A_AC3";
				if (dtsRemux) {
					if (params.mediaRenderer.isMuxDTSToMpeg()) {
						// Renderer can play proper DTS track
						audioType = "A_DTS";
					} else {
						// DTS padded in LPCM trick
						audioType = "A_LPCM";
					}
				}

				pwMux.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + fps + "level=4.1, insertSEI, contSPS, track=1");
				pwMux.println(audioType + ", \"" + ffAudioPipe.getOutputPipe() + "\", track=2");
			}

			ProcessWrapper pipe_process = pipe.getPipeProcess();
			pw.attachProcess(pipe_process);
			pipe_process.runInNewThread();

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}

			pipe.deleteLater();
			params.input_pipes[0] = pipe;

			ProcessWrapper ff_pipe_process = ffAudioPipe.getPipeProcess();
			pw.attachProcess(ff_pipe_process);
			ff_pipe_process.runInNewThread();

			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}

			ffAudioPipe.deleteLater();
			pw.attachProcess(ffAudio);
			ffAudio.runInNewThread();
		}

		pw.runInNewThread();
		return pw;
	}

	private JCheckBox multithreading;
	private JCheckBox videoremux;

	@Override
	public JComponent config() {
		return config("NetworkTab.5");
	}

	protected JComponent config(String languageLabel) {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.setBorder(Borders.EMPTY_BORDER);
		builder.setOpaque(false);

		CellConstraints cc = new CellConstraints();

		JComponent cmp = builder.addSeparator(Messages.getString(languageLabel), cc.xyw(2, 1, 1));
		cmp = (JComponent) cmp.getComponent(0);
		cmp.setFont(cmp.getFont().deriveFont(Font.BOLD));

		multithreading = new JCheckBox(Messages.getString("MEncoderVideo.35"));
		multithreading.setContentAreaFilled(false);
		if (configuration.isFfmpegMultithreading()) {
			multithreading.setSelected(true);
		}
		multithreading.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setFfmpegMultithreading(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(multithreading, cc.xy(2, 3));

		videoremux = new JCheckBox(Messages.getString("FFmpeg.0"));
		videoremux.setContentAreaFilled(false);
		if (configuration.isFFmpegMuxWhenCompatible()) {
			videoremux.setSelected(true);
		}
		videoremux.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setFFmpegMuxWhenCompatible(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(videoremux, cc.xy(2, 5));

		return builder.getPanel();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		if (resource == null || resource.getFormat().getType() != Format.VIDEO) {
			return false;
		}

		DLNAMediaSubtitle subtitle = resource.getMediaSubtitle();

		// Check whether the subtitle actually has a language defined,
		// uninitialized DLNAMediaSubtitle objects have a null language.
		// For now supports only external subtitles
		if (
			subtitle != null && subtitle.getLang() != null &&
			subtitle.getExternalFile() == null
		) {
			return false;
		}

		Format format = resource.getFormat();

		if (format != null) {
			Format.Identifier id = format.getIdentifier();

			if (id.equals(Format.Identifier.MKV) || id.equals(Format.Identifier.MPG)) {
				return true;
			}
		}

		return false;
	}

	protected static List<String> parseOptions(String str) {
		return str == null ? null : parseOptions(str, new ArrayList<String>());
	}

	protected static List<String> parseOptions(String str, List<String> cmdList) {
		while (str.length() > 0) {
			if (str.charAt(0) == '\"') {
				int pos = str.indexOf("\"", 1);
				if (pos == -1) {
					// No ", error
					break;
				}
				String tmp = str.substring(1, pos);
				cmdList.add(tmp.trim());
				str = str.substring(pos + 1);
				continue;
			} else {
				// New arg, find space
				int pos = str.indexOf(" ");
				if (pos == -1) {
					// No space, we're done
					cmdList.add(str);
					break;
				}
				String tmp = str.substring(0, pos);
				cmdList.add(tmp.trim());
				str = str.substring(pos + 1);
				continue;
			}
		}
		return cmdList;
	}

	
}
