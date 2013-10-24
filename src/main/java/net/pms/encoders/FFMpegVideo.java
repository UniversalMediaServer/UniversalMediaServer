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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
import net.pms.util.PlayerUtil;
import net.pms.util.ProcessUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Pure FFmpeg video player.
 *
 * Design note:
 *
 * Helper methods that return lists of <code>String</code>s representing
 * options are public to facilitate composition e.g. a custom engine (plugin)
 * that uses tsMuxeR for videos without subtitles and FFmpeg otherwise needs to
 * compose and call methods on both players.
 *
 * To avoid API churn, and to provide wiggle room for future functionality, all
 * of these methods take the same arguments as launchTranscode (and the same
 * first four arguments as finalizeTranscoderArgs) even if one or more of the
 * parameters are unused e.g.:
 *
 *     public List<String> getAudioBitrateOptions(
 *         String filename,
 *         DLNAResource dlna,
 *         DLNAMediaInfo media,
 *         OutputParams params
 *     )
 */
public class FFMpegVideo extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);
	private static final String DEFAULT_QSCALE = "3";
	private static final String SUB_DIR = "subs";
	
	public FFMpegVideo() {
	}
	
	@Deprecated
	public FFMpegVideo(PmsConfiguration configuration) {
		FFMpegVideo.configuration = configuration;
	}

	// FIXME we have an id() accessor for this; no need for the field to be public
	@Deprecated
	public static final String ID = "ffmpegvideo";

	/**
	 * Returns a list of strings representing the rescale options for this transcode i.e. the ffmpeg -vf
	 * options used to show subtitles in SSA/ASS format and resize a video that's too wide and/or high for the specified renderer.
	 * If the renderer has no size limits, or there's no media metadata, or the video is within the renderer's
	 * size limits, an empty list is returned.
	 *
	 * @param dlna
	 * @param media metadata for the DLNA resource which is being transcoded
	 * @param params 
	 * @return a {@link List} of <code>String</code>s representing the rescale options for this video,
	 * or an empty list if the video doesn't need to be resized.
	 * @throws java.io.IOException
	 */
	public List<String> getVideoFilterOptions(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		List<String> videoFilterOptions = new ArrayList<String>();
		StringBuilder subsOption = new StringBuilder();
		File tempSubs = null;
		final RendererConfiguration renderer = params.mediaRenderer;
		if (!isDisableSubtitles(params)) {
			tempSubs = getSubtitles(dlna, media, params);
		}

		boolean isMediaValid = media != null && media.isMediaparsed() && media.getHeight() != 0;
		boolean isResolutionTooHighForRenderer = renderer.isVideoRescale() && isMediaValid && // renderer defines a max width/height
			(
				media.getWidth() > renderer.getMaxVideoWidth() ||
				media.getHeight() > renderer.getMaxVideoHeight()
			);

		if (tempSubs != null) {
			StringBuilder s = new StringBuilder();
			CharacterIterator it = new StringCharacterIterator(tempSubs.getAbsolutePath());

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

			if (params.sid.isEmbedded() || (params.sid.isExternal() && params.sid.getType() == SubtitleType.ASS)) {
				subsOption.append("ass=");
				subsOption.append(subsFile);
			} else if (params.sid.isExternal() && params.sid.getType() == SubtitleType.SUBRIP) {
				subsOption.append("subtitles=");
				subsOption.append(subsFile);
			}

			// based on https://trac.ffmpeg.org/ticket/2067
			if (params.timeseek > 0) {
				videoFilterOptions.add("-copyts");
				videoFilterOptions.add("-copypriorss");
				videoFilterOptions.add("0");
				videoFilterOptions.add("-avoid_negative_ts");
				videoFilterOptions.add("1");
				videoFilterOptions.add("-af");
				videoFilterOptions.add("asetpts=PTS-" + (int) params.timeseek + "/TB");
				subsOption.append(",setpts=PTS-").append((int) params.timeseek).append("/TB");
			}
		}

		String rescaleOrPadding = null;

		if (isResolutionTooHighForRenderer || (renderer.isKeepAspectRatio() && !renderer.isRescaleByRenderer() && media.getWidth() < 720)) { // Do not rescale for SD video and higher
			rescaleOrPadding = String.format(
				// http://stackoverflow.com/a/8351875
				"scale=iw*min(%1$d/iw\\,%2$d/ih):ih*min(%1$d/iw\\,%2$d/ih),pad=%1$d:%2$d:(%1$d-iw)/2:(%2$d-ih)/2",
				renderer.getMaxVideoWidth(),
				renderer.getMaxVideoHeight()
			);
		} else if (renderer.isKeepAspectRatio() && isMediaValid) {
			if ((media.getWidth() / (double) media.getHeight()) >= (16 / (double) 9)) {
				rescaleOrPadding = "pad=iw:iw/(16/9):0:(oh-ih)/2";
			} else {
				rescaleOrPadding = "pad=ih*(16/9):ih:(ow-iw)/2:0";
			}
		}

		String overrideVF = renderer.getFFmpegVideoFilterOverride();

		if (rescaleOrPadding != null || overrideVF != null || isNotBlank(subsOption)) {
			videoFilterOptions.add("-vf");
			StringBuilder filterParams = new StringBuilder();

			if (overrideVF != null) {
				filterParams.append(overrideVF);
				if (isNotBlank(subsOption)) {
					filterParams.append(", ");
				}
			} else {
				if (rescaleOrPadding != null) {
					filterParams.append(rescaleOrPadding);
					if (isNotBlank(subsOption)) {
						filterParams.append(", ");
					}
				}
			}

			if (isNotBlank(subsOption)) {
				filterParams.append(subsOption);
			}

			videoFilterOptions.add(filterParams.toString());
		}

		return videoFilterOptions;
	}

	/**
	 * Returns a list of <code>String</code>s representing ffmpeg output
	 * options (i.e. options that define the output file's video codec,
	 * audio codec and container) compatible with the renderer's
	 * <code>TranscodeVideo</code> profile.
	 *
	 * @param dlna
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params output parameters
	 *
	 * @return a {@link List} of <code>String</code>s representing the FFmpeg output parameters for the renderer according
	 * to its <code>TranscodeVideo</code> profile.
	 */
	public synchronized List<String> getVideoTranscodeOptions(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) {
		List<String> transcodeOptions = new ArrayList<String>();
		final String filename = dlna.getSystemName();
		final RendererConfiguration renderer = params.mediaRenderer;

		if (renderer.isTranscodeToWMV() && !renderer.isXBOX()) { // WMV
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
				configuration.isAudioEmbedDtsInPcm() &&
				params.aid != null &&
				params.aid.isDTS() &&
				!avisynth() &&
				renderer.isDTSPlayable();

			if (configuration.isAudioRemuxAC3() && params.aid != null && params.aid.isAC3() && !avisynth() && renderer.isTranscodeToAC3()) {
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
			if (filename != null) {
				newInput = new InputFile();
				newInput.setFilename(filename);
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
	 * Returns the video bitrate spec for the current transcode according
	 * to the limits/requirements of the renderer and the user's settings.
	 *
	 * @param dlna
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the video bitrate options for this transcode
	 */
	public List<String> getVideoBitrateOptions(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) {
		List<String> videoBitrateOptions = new ArrayList<String>();

		int defaultMaxBitrates[] = getVideoBitrateConfig(configuration.getMaximumBitrate());
		int rendererMaxBitrates[] = new int[2];

		if (params.mediaRenderer.getMaxVideoBitrate() != null) {
			rendererMaxBitrates = getVideoBitrateConfig(params.mediaRenderer.getMaxVideoBitrate());
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			defaultMaxBitrates = rendererMaxBitrates;
		}

		if (params.mediaRenderer.getCBRVideoBitrate() == 0 && params.timeend == 0) {
			// Convert value from Mb to Kb
			defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];

			// Halve it since it seems to send up to 1 second of video in advance
			defaultMaxBitrates[0] = defaultMaxBitrates[0] / 2;

			int bufSize = 1835;
			if (!params.mediaRenderer.isTranscodeToH264TSAC3()) {
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
			if (params.mediaRenderer.isTranscodeToH264TSAC3() || videoRemux) {
				if (
					params.mediaRenderer.isH264Level41Limited() &&
					defaultMaxBitrates[0] > 31250000
				) {
					defaultMaxBitrates[0] = 31250000;
				}
				bufSize = defaultMaxBitrates[0];
			}

			videoBitrateOptions.add("-bufsize");
			videoBitrateOptions.add(String.valueOf(bufSize));

			videoBitrateOptions.add("-maxrate");
			videoBitrateOptions.add(String.valueOf(defaultMaxBitrates[0]));
		}

		if (!videoRemux) {
			if (!params.mediaRenderer.isTranscodeToH264TSAC3()) {
				// Add MPEG-2 quality settings
				String mpeg2Options = configuration.getMPEG2MainSettingsFFmpeg();
				String mpeg2OptionsRenderer = params.mediaRenderer.getCustomFFmpegMPEG2Options();

				// Renderer settings take priority over user settings
				if (isNotBlank(mpeg2OptionsRenderer)) {
					mpeg2Options = mpeg2OptionsRenderer;
				} else if (mpeg2Options.contains("Automatic")) {
					mpeg2Options = "-g 5 -q:v 1 -qmin 2 -qmax 3";

					// It has been reported that non-PS3 renderers prefer keyint 5 but prefer it for PS3 because it lowers the average bitrate
					if (params.mediaRenderer.isPS3()) {
						mpeg2Options = "-g 25 -q:v 1 -qmin 2 -qmax 3";
					}

					if (mpeg2Options.contains("Wireless") || defaultMaxBitrates[0] < 70) {
						// Lower quality for 720p+ content
						if (media.getWidth() > 1280) {
							mpeg2Options = "-g 25 -qmax 7 -qmin 2";
						} else if (media.getWidth() > 720) {
							mpeg2Options = "-g 25 -qmax 5 -qmin 2";
						}
					}
				}
				String[] customOptions = StringUtils.split(mpeg2Options);
				videoBitrateOptions.addAll(new ArrayList<String>(Arrays.asList(customOptions)));
			} else {
				// Add x264 quality settings
				String x264CRF = configuration.getx264ConstantRateFactor();
				if (x264CRF.contains("Automatic")) {
					x264CRF = "-crf 16";

					// Lower CRF for 720p+ content
					if (media.getWidth() > 720) {
						x264CRF = "-crf 19";
					}
				}
				String[] customOptions = StringUtils.split(x264CRF);
				videoBitrateOptions.addAll(new ArrayList<String>(Arrays.asList(customOptions)));
			}
		}

		return videoBitrateOptions;
	}

	/**
	 * Returns the audio bitrate spec for the current transcode according
	 * to the limits/requirements of the renderer.
	 *
	 * @param dlna
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the audio bitrate options for this transcode
	 */
	public List<String> getAudioBitrateOptions(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) {
		List<String> audioBitrateOptions = new ArrayList<String>();

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
		return threads;
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
		List<String> defaultArgsList = new ArrayList<String>();

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
	public synchronized ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		int nThreads = configuration.getNumberOfCpuCores();
		List<String> cmdList = new ArrayList<String>();
		RendererConfiguration renderer = params.mediaRenderer;
		final String filename = dlna.getSystemName();
		boolean avisynth = avisynth();
		if (params.timeseek > 0) {
			params.waitbeforestart = 200;
		} else {
			params.waitbeforestart = 2500;
		}

		setAudioAndSubs(filename, media, params);
		cmdList.add(executable());

		// Prevent FFmpeg timeout
		cmdList.add("-y");

		cmdList.add("-loglevel");
		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("fatal");
		}

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add(String.valueOf((int) params.timeseek));
		}

		// decoder threads
		cmdList.add("-threads");
		cmdList.add(String.valueOf(nThreads));

		final boolean isTsMuxeRVideoEngineEnabled = configuration.getEnginesAsList(PMS.get().getRegistry()).contains(TsMuxeRVideo.ID);

		ac3Remux = false;
		dtsRemux = false;

		if (configuration.isAudioRemuxAC3() && params.aid != null && params.aid.isAC3() && !avisynth() && renderer.isTranscodeToAC3()) {
			// AC-3 remux takes priority
			ac3Remux = true;
		} else {
			// Now check for DTS remux and LPCM streaming
			dtsRemux = isTsMuxeRVideoEngineEnabled &&
				configuration.isAudioEmbedDtsInPcm() &&
				params.aid != null &&
				params.aid.isDTS() &&
				!avisynth() &&
				params.mediaRenderer.isDTSPlayable();
		}

		String frameRateRatio = media.getValidFps(true);
		String frameRateNumber = media.getValidFps(false);

		// Input filename
		cmdList.add("-i");
		if (avisynth && !filename.toLowerCase().endsWith(".iso")) {
			File avsFile = AviSynthFFmpeg.getAVSScript(filename, params.sid, params.fromFrame, params.toFrame, frameRateRatio, frameRateNumber);
			cmdList.add(ProcessUtil.getShortFileNameIfWideChars(avsFile.getAbsolutePath()));
		} else {
			cmdList.add(filename);

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
		cmdList.add(String.valueOf(nThreads));

		if (params.timeend > 0) {
			cmdList.add("-t");
			cmdList.add(String.valueOf(params.timeend));
		}

		cmdList.addAll(getVideoBitrateOptions(dlna, media, params));

		// add audio bitrate options
		// TODO: Integrate our (more comprehensive) code with this function
		// from PMS to make keeping synchronised easier.
		// Until then, leave the following line commented out.
		// cmdList.addAll(getAudioBitrateOptions(dlna, media, params));

		// if the source is too large for the renderer, resize it
		// and/or add subtitles to video filter
		// FFmpeg must be compiled with --enable-libass parameter
		cmdList.addAll(getVideoFilterOptions(dlna, media, params));

		// Audio bitrate
		if (!ac3Remux && !dtsRemux && !(type() == Format.AUDIO)) {
			int channels;
			if (renderer.isTranscodeToWMV() && !renderer.isXBOX()) {
				channels = 2;
			} else if (ac3Remux) {
				channels = params.aid.getAudioProperties().getNumberOfChannels(); // AC-3 remux
			} else {
				channels = configuration.getAudioChannelCount(); // 5.1 max for AC-3 encoding
			}
			cmdList.add("-ac");
			cmdList.add(String.valueOf(channels));

			cmdList.add("-ab");
			cmdList.add(configuration.getAudioBitrate() + "k");
		}

		// Add the output options (-f, -c:a, -c:v, etc.)
		cmdList.addAll(getVideoTranscodeOptions(dlna, media, params));

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
			filename,
			dlna,
			media,
			params,
			cmdArray
		);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		if (dtsRemux) {
			PipeProcess pipe;
			pipe = new PipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

			TsMuxeRVideo ts = new TsMuxeRVideo();
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
				filename,
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
			sm.setNbChannels(2);

			List<String> cmdListDTS = new ArrayList<String>();
			cmdListDTS.add(executable());
			cmdListDTS.add("-y");
			cmdListDTS.add("-ss");

			if (params.timeseek > 0) {
				cmdListDTS.add(String.valueOf(params.timeseek));
			} else {
				cmdListDTS.add("0");
			}

			if (params.stdin == null) {
				cmdListDTS.add("-i");
			} else {
				cmdListDTS.add("-");
			}
			cmdListDTS.add(filename);

			if (params.timeseek > 0) {
				cmdListDTS.add("-copypriorss");
				cmdListDTS.add("0");
				cmdListDTS.add("-avoid_negative_ts");
				cmdListDTS.add("1");
			}

			cmdListDTS.add("-ac");
			cmdListDTS.add("2");

			cmdListDTS.add("-f");
			cmdListDTS.add("dts");

			cmdListDTS.add("-c:a");
			cmdListDTS.add("copy");

			cmdListDTS.add(ffAudioPipe.getInputPipe());

			String[] cmdArrayDTS = new String[cmdListDTS.size()];
			cmdListDTS.toArray(cmdArrayDTS);

			if (!params.mediaRenderer.isMuxDTSToMpeg()) { // No need to use the PCM trick when media renderer supports DTS
				ffAudioPipe.setModifier(sm);
			}

			OutputParams ffaudioparams = new OutputParams(configuration);
			ffaudioparams.maxBufferSize = 1;
			ffaudioparams.stdin = params.stdin;
			ProcessWrapperImpl ffAudio = new ProcessWrapperImpl(cmdArrayDTS, ffaudioparams);

			params.stdin = null;
			PrintWriter pwMux = new PrintWriter(f);
			pwMux.println("MUXOPT --no-pcr-on-video-pid --no-asyncio --new-audio-pes --vbr --vbv-len=500");
			String videoType = "V_MPEG-2";

			if (videoRemux) {
				videoType = "V_MPEG4/ISO/AVC";
			}

			if (params.no_videoencode && params.forceType != null) {
				videoType = params.forceType;
			}

			StringBuilder fps = new StringBuilder();
			fps.append("");
			if (params.forceFps != null) {
				fps.append("fps=").append(params.forceFps).append(", ");
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
			pwMux.close();

			ProcessWrapper pipe_process = pipe.getPipeProcess();
			pw.attachProcess(pipe_process);
			pipe_process.runInNewThread();

			try {
				wait(50);
			} catch (InterruptedException e) {
			}

			pipe.deleteLater();
			params.input_pipes[0] = pipe;

			ProcessWrapper ff_pipe_process = ffAudioPipe.getPipeProcess();
			pw.attachProcess(ff_pipe_process);
			ff_pipe_process.runInNewThread();

			try {
				wait(50);
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
	private JCheckBox fc;

	@Override
	public JComponent config() {
		return config("NetworkTab.5");
	}

	protected JComponent config(String languageLabel) {
		FormLayout layout = new FormLayout(
			"left:pref, 0:grow",
			"p, 3dlu, p, 3dlu, p, 3dlu, p"
		);
		PanelBuilder builder = new PanelBuilder(layout);
		builder.border(Borders.EMPTY);
		builder.opaque(false);

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

		fc = new JCheckBox(Messages.getString("MEncoderVideo.21"));
		fc.setContentAreaFilled(false);
		fc.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				configuration.setFFmpegFontConfig(e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		builder.add(fc, cc.xy(2, 7));
		fc.setSelected(configuration.isFFmpegFontConfig());

		return builder.getPanel();
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
			}
		}
		return cmdList;
	}

	/**
	 * Extracts embedded subtitles from video to file in SSA/ASS format, converts external SRT
	 * subtitles file to SSA/ASS format and applies fontconfig setting to that converted file
	 * and applies timeseeking when required.
	 *
	 * @param dlna DLNAResource
	 * @param media DLNAMediaInfo
	 * @param params Output parameters
	 * @return Converted subtitle file
	 * @throws IOException
	 */
	public File getSubtitles(DLNAResource dlna, DLNAMediaInfo media, OutputParams params) throws IOException {
		if (media == null || params.sid.getId() == -1) {
			return null;
		}

		String dir = configuration.getDataFile(SUB_DIR);
		File subsPath = new File(dir);
		if (!subsPath.exists()) {
			subsPath.mkdirs();
		}

		boolean applyFontConfig = configuration.isFFmpegFontConfig();
		boolean isEmbeddedSource = params.sid.getId() < 100;

		String filename = isEmbeddedSource ?
			dlna.getSystemName() : params.sid.getExternalFile().getAbsolutePath();

		String basename;

		long modId = new File(filename).lastModified();
		if (modId != 0) {
			// We have a real file
			basename = FilenameUtils.getBaseName(filename);
		} else {
			// It's something else, e.g. a url or psuedo-url without meaningful
			// lastmodified and (maybe) basename characteristics.
			basename = dlna.getName().replaceAll("[<>:\"\\\\/|?*+\\[\\]\n\r]", "").trim();
			modId = filename.hashCode();
		}

		File convertedSubs;
		if (applyFontConfig || isEmbeddedSource) {
			convertedSubs = new File(subsPath.getAbsolutePath() + File.separator + basename + "_ID" + params.sid.getId() + "_" + modId + ".ass");
		} else {
			convertedSubs = new File(subsPath.getAbsolutePath() + File.separator + modId + "_" + params.sid.getExternalFile().getName());
		}

		if (convertedSubs.canRead()) {
			// subs are already converted
			return convertedSubs; 
		}

		boolean isExternalAss = false;
		if (
			params.sid.getType() == SubtitleType.ASS &&
			params.sid.isExternal() &&
			!isEmbeddedSource
		) {
			isExternalAss = true;
		}

		File tempSubs;
		if (
			isExternalAss ||
			(
				!applyFontConfig &&
				!isEmbeddedSource &&
				params.sid.getType() == SubtitleType.SUBRIP
			)
		) {
			tempSubs = params.sid.getExternalFile();
		} else {
			tempSubs = convertSubsToAss(filename, media, params);
		}

		if (tempSubs == null) {
			return null;
		}

		if (!FileUtil.isFileUTF8(tempSubs)) {
			tempSubs = SubtitleUtils.applyCodepageConversion(tempSubs, convertedSubs);
		} else {
			FileUtils.copyFile(tempSubs, convertedSubs);
			tempSubs = convertedSubs;
		}

		// Now we're sure we actually have our own modifiable file
		if (applyFontConfig) {
			try {
				tempSubs = applyFontconfigToASSTempSubsFile(tempSubs, media);
			} catch (IOException e) {
				LOGGER.debug("Applying subs setting ends with error: " + e);
				return null;
			}
		}

		if (isEmbeddedSource) {
			params.sid.setExternalFile(tempSubs);
			params.sid.setType(SubtitleType.ASS);
		}

		PMS.get().addTempFile(tempSubs, 30 * 24 * 3600 * 1000);

		return tempSubs;
	}

	/**
	 * Converts external subtitles file in SRT format or extract embedded subs to default SSA/ASS format
	 * @param fileName Subtitles file in SRT format or video file with embedded subs
	 * @param media 
	 * @param params output parameters
	 * @return Converted subtitles file in SSA/ASS format
	 */
	public static File convertSubsToAss(String fileName, DLNAMediaInfo media, OutputParams params) {
		List<String> cmdList = new ArrayList<String>();
		File tempSubsFile;
		cmdList.add(configuration.getFfmpegPath());
		cmdList.add("-y");
		cmdList.add("-loglevel");
		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("fatal");
		}

		// Try to specify input encoding if we have a non utf-8 external sub
		if (params.sid.getId() == 100 && !params.sid.isExternalFileUtf8()) {
			String encoding = isNotBlank(configuration.getSubtitlesCodepage()) ?
					// Prefer the global user-specified encoding if we have one.
					// Note: likely wrong if the file isn't supplied by the user.
					configuration.getSubtitlesCodepage() :
				params.sid.getExternalFileCharacterSet() != null ?
					// Fall back on the actually detected encoding if we have it.
					// Note: accuracy isn't 100% guaranteed.
					params.sid.getExternalFileCharacterSet() :
				null; // Otherwise we're out of luck!
			if (encoding != null) {
				cmdList.add("-sub_charenc");
				cmdList.add(encoding);
			}
		}

		cmdList.add("-i");
		cmdList.add(fileName);

		if (params.sid.isEmbedded()) {
			cmdList.add("-map");
			cmdList.add("0:s:" + (media.getSubtitleTracksList().indexOf(params.sid)));
		}

		try {
			tempSubsFile = new File(configuration.getTempFolder(), FilenameUtils.getBaseName(fileName) + ".ass");
		} catch (IOException e1) {
			LOGGER.debug("Subtitles conversion finished wih error: " + e1);
			return null;
		}
		cmdList.add(tempSubsFile.getAbsolutePath());

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.runInNewThread();

		try {
			pw.join(); // Wait until the conversion is finished
		} catch (InterruptedException e) {
			LOGGER.debug("Subtitles conversion finished wih error: " + e);
			return null;
		}

		tempSubsFile.deleteOnExit();
		return tempSubsFile;
	}

	public File applyFontconfigToASSTempSubsFile(File tempSubs, DLNAMediaInfo media) throws IOException {
		File outputSubs = tempSubs;
		StringBuilder outputString = new StringBuilder();
		File temp = new File(configuration.getTempFolder(), tempSubs.getName() + ".tmp");

		File sourceFile = new File(tempSubs.toString());
		File destinationFile = new File(temp.toString());
		FileUtils.copyFile(sourceFile, destinationFile);

		BufferedWriter output;
		BufferedReader input = new BufferedReader(new FileReader(temp));
		output = new BufferedWriter(new FileWriter(outputSubs));
		String line;
		String[] format = null;
		int i;
		while (( line = input.readLine()) != null) {
			outputString.setLength(0);
			if (line.startsWith("[Script Info]")) {
				outputString.append(line).append("\n");
				output.write(outputString.toString());
				while (( line = input.readLine()) != null) {
					outputString.setLength(0);
					if (!line.isEmpty()) {
						outputString.append(line).append("\n");
						output.write(outputString.toString());
					} else  {
						outputString.append("PlayResY: ").append(media.getHeight()).append("\n");
						outputString.append("PlayResX: ").append(media.getWidth()).append("\n");
						break;
					}
				}
			}

			if (line.startsWith("Format:")) {
				format = line.split(",");
				outputString.append(line).append("\n");
				output.write(outputString.toString());
				continue;
			}

			if (line.startsWith("Style: Default")) {
				String[] params = line.split(",");

				for (i = 0; i < format.length; i++) {
					if (format[i].contains("Fontname")) {
						if (!configuration.getFont().isEmpty()) {
							params[i] = configuration.getFont();
						} else {
							params[i] = "Arial";
						}
						continue;
					}

					if (format[i].contains("Fontsize")) {
						params[i] = Integer.toString((int) ((Integer.parseInt(params[i]) * media.getHeight()/288 * Double.parseDouble(configuration.getAssScale()))));
						continue;
					}

					if (format[i].contains("PrimaryColour")) {
						String primaryColour = Integer.toHexString(configuration.getSubsColor());
						params[i] = "&H" + primaryColour.substring(6, 8) + primaryColour.substring(4, 6) + primaryColour.substring(2, 4);
						continue;
					}

					if (format[i].contains("Outline")) {
						params[i] = configuration.getAssOutline();
						continue;
					}

					if (format[i].contains("Shadow")) {
						params[i] = configuration.getAssShadow();
						continue;
					}

						if (format[i].contains("MarginV")) {
							params[i] = configuration.getAssMargin();
						}
					}
				}

				outputString.append(StringUtils.join(params, ",")).append("\n");
				output.write(outputString.toString());
				continue;
			}

			outputString.append(line).append("\n");
			output.write(outputString.toString());
		}
		input.close();
		output.flush();
		output.close();
		temp.deleteOnExit();
		return outputSubs;
	}
	
	/**
	 * Collapse the multiple internal ways of saying "subtitles are disabled" into a single method
	 * which returns true if any of the following are true:
	 *
	 *     1) configuration.isDisableSubtitles()
	 *     2) params.sid == null
	 *     3) avisynth()
	 * @param params
	 * @return 
	 */
	public boolean isDisableSubtitles(OutputParams params) {
		return configuration.isDisableSubtitles() || (params.sid == null) || avisynth();
	}

	/**
	 * {@inheritDoc}
	 * @return 
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		if (
			PlayerUtil.isVideo(resource, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(resource, Format.Identifier.MPG)
		) {
			return true;
		}

		return false;
	}
}
