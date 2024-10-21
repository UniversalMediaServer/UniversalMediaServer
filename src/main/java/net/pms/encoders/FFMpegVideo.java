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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import net.pms.Messages;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.encoders.AviSynthFFmpeg.AviSynthScriptGenerationResult;
import net.pms.formats.Format;
import net.pms.formats.v2.SubtitleType;
import net.pms.io.IPipeProcess;
import net.pms.io.ListProcessWrapperResult;
import net.pms.io.OutputParams;
import net.pms.io.OutputTextLogger;
import net.pms.io.PipeIPCProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.io.SimpleProcessWrapper;
import net.pms.io.StreamModifier;
import net.pms.media.MediaInfo;
import net.pms.media.audio.MediaAudio;
import net.pms.media.subtitle.MediaSubtitle;
import net.pms.media.video.MediaVideo;
import net.pms.network.HTTPResource;
import net.pms.platform.PlatformUtils;
import net.pms.platform.windows.NTStatus;
import net.pms.renderers.OutputOverride;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.CodecUtil;
import net.pms.util.ExecutableErrorType;
import net.pms.util.ExecutableInfo;
import net.pms.util.ExecutableInfo.ExecutableInfoBuilder;
import net.pms.util.FFmpegExecutableInfo.FFmpegExecutableInfoBuilder;
import net.pms.util.InputFile;
import net.pms.util.PlayerUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.StringUtil;
import net.pms.util.SubtitleUtils;
import net.pms.util.Version;
import org.apache.commons.lang3.StringUtils;
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
 *         StoreResource resource,
 *         MediaInfo media,
 *         OutputParams params
 *     )
 */
public class FFMpegVideo extends Engine {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);
	public static final EngineId ID = StandardEngineId.FFMPEG_VIDEO;

	/** The {@link Configuration} key for the custom FFmpeg path. */
	public static final String KEY_FFMPEG_PATH = "ffmpeg_path";

	/** The {@link Configuration} key for the FFmpeg executable type. */
	public static final String KEY_FFMPEG_EXECUTABLE_TYPE = "ffmpeg_executable_type";
	public static final String NAME = "FFmpeg Video";
	private static final String DEFAULT_QSCALE = "3";

	// Not to be instantiated by anything but PlayerFactory
	FFMpegVideo() {
		super(CONFIGURATION.getFFmpegPaths());
	}

	/**
	 * Returns a list of strings representing the rescale options for this transcode i.e. the ffmpeg -vf
	 * options used to show subtitles in either SSA/ASS or picture-based format and resize a video that's too wide and/or high for the specified renderer.
	 * If the renderer has no size limits, or there's no media metadata, or the video is within the renderer's
	 * size limits, an empty list is returned.
	 *
	 * @param resource
	 * @param mediaInfo metadata for the DLNA HlsHelper which is being transcoded
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the rescale options for this video,
	 * or an empty list if the video doesn't need to be resized.
	 * @throws java.io.IOException
	 */
	public List<String> getVideoFilterOptions(StoreItem resource, MediaInfo mediaInfo, OutputParams params, boolean isConvertedTo3d) throws IOException {
		List<String> videoFilterOptions = new ArrayList<>();
		ArrayList<String> filterChain = new ArrayList<>();
		final Renderer renderer = params.getMediaRenderer();
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		MediaVideo defaultVideoTrack = mediaInfo != null ? mediaInfo.getDefaultVideoTrack() : null;

		boolean isMediaValid = mediaInfo != null && mediaInfo.isMediaParsed() && defaultVideoTrack != null && defaultVideoTrack.getHeight() != 0;
		boolean isResolutionTooHighForRenderer = isMediaValid && defaultVideoTrack != null && !renderer.isResolutionCompatibleWithRenderer(defaultVideoTrack.getWidth(), defaultVideoTrack.getHeight());

		int scaleWidth = 0;
		int scaleHeight = 0;
		if (defaultVideoTrack != null && defaultVideoTrack.getWidth() > 0 && defaultVideoTrack.getHeight() > 0) {
			scaleWidth = defaultVideoTrack.getWidth();
			scaleHeight = defaultVideoTrack.getHeight();
		}

		boolean is3D = (defaultVideoTrack != null && defaultVideoTrack.is3d() && !defaultVideoTrack.multiViewIsAnaglyph()) || isConvertedTo3d;

		// Make sure the aspect ratio is 16/9 if the renderer needs it.
		boolean keepAR = (renderer.isKeepAspectRatio() || renderer.isKeepAspectRatioTranscoding()) &&
				(defaultVideoTrack != null && !defaultVideoTrack.is3dFullSbsOrOu()) && !isConvertedTo3d &&
				(!"16:9".equals(defaultVideoTrack.getDisplayAspectRatio()));

		// Scale and pad the video if necessary
		ArrayList<String> scalePadFilterChain = new ArrayList<>();
		if (isResolutionTooHighForRenderer || (!renderer.isRescaleByRenderer() && renderer.isMaximumResolutionSpecified() && mediaInfo.getWidth() < 720)) { // Do not rescale for SD video and higher
			if (defaultVideoTrack != null && defaultVideoTrack.is3dFullSbsOrOu()) {
				scalePadFilterChain.add(String.format("[0:v]scale=%1$d:%2$d", renderer.getMaxVideoWidth(), renderer.getMaxVideoHeight()));
			} else {
				scalePadFilterChain.add(String.format("[0:v]scale=iw*min(%1$d/iw\\,%2$d/ih):ih*min(%1$d/iw\\,%2$d/ih)", renderer.getMaxVideoWidth(), renderer.getMaxVideoHeight()));
				scalePadFilterChain.add(String.format("[0:v]pad=ceil(iw/4)*4:ceil(ih/4)*4:(ow-iw)/2:(oh-ih)/2"));  // ensure height and width are divisible by 4

				if (keepAR) {
					scalePadFilterChain.add(String.format("[0:v]pad=%1$d:%2$d:(%1$d-iw)/2:(%2$d-ih)/2", renderer.getMaxVideoWidth(), renderer.getMaxVideoHeight()));
				}
			}
		} else if (keepAR && isMediaValid) {
			if ((mediaInfo.getWidth() / (double) mediaInfo.getHeight()) >= (16 / (double) 9)) {
				scalePadFilterChain.add("[0:v]pad=iw:iw/(16/9):0:(oh-ih)/2");
				scaleHeight = (int) Math.round(scaleWidth / (16 / (double) 9));
			} else {
				scalePadFilterChain.add("[0:v]pad=ih*(16/9):ih:(ow-iw)/2:0");
				scaleWidth = (int) Math.round(scaleHeight * (16 / (double) 9));
			}

			scaleWidth  = convertToModX(scaleWidth, 4);
			scaleHeight = convertToModX(scaleHeight, 4);

			// Make sure we didn't exceed the renderer's maximum resolution.
			if (
				scaleHeight > renderer.getMaxVideoHeight() ||
				scaleWidth  > renderer.getMaxVideoWidth()
			) {
				scaleHeight = renderer.getMaxVideoHeight();
				scaleWidth  = renderer.getMaxVideoWidth();
			}

			scalePadFilterChain.add("[0:v]scale=" + scaleWidth + ":" + scaleHeight);
		}
		filterChain.addAll(scalePadFilterChain);

		boolean override = true;
		if (renderer instanceof OutputOverride or) {
			override = or.addSubtitles();
		}

		if (!isDisableSubtitles(params) && override) {
			boolean isSubsManualTiming = true;
			MediaSubtitle convertedSubs = resource.getMediaSubtitle();
			StringBuilder subsFilter = new StringBuilder();
			if (params.getSid() != null && params.getSid().getType().isText()) {
				boolean isSubsASS = params.getSid().getType() == SubtitleType.ASS;
				String originalSubsFilename = null;
				if (is3D) {
					if (convertedSubs != null && convertedSubs.getConvertedFile() != null) { // subs are already converted to 3D so use them
						originalSubsFilename = convertedSubs.getConvertedFile().getAbsolutePath();
					} else if (!isSubsASS) { // When subs are not converted and they are not in the ASS format and video is 3D then subs need conversion to 3D
						File subtitlesFile = SubtitleUtils.getSubtitles(resource, mediaInfo, params, configuration, SubtitleType.ASS);
						if (subtitlesFile != null) {
							originalSubsFilename = subtitlesFile.getAbsolutePath();
						} else {
							LOGGER.error("External subtitles file \"{}\" is unavailable", params.getSid().getName());
						}
					} else {
						if (params.getSid().getExternalFile() != null) {
							originalSubsFilename = params.getSid().getExternalFile().getPath();
						} else {
							LOGGER.error("External subtitles file \"{}\" is unavailable", params.getSid().getName());
						}
					}
				} else if (params.getSid().isExternal()) {
					if (params.getSid().getExternalFile() != null) {
						if (
							!renderer.streamSubsForTranscodedVideo() ||
							!renderer.isExternalSubtitlesFormatSupported(params.getSid(), resource)
						) {
							// Only transcode subtitles if they aren't streamable
							originalSubsFilename = params.getSid().getExternalFile().getPath();
						}
					} else {
						LOGGER.error("External subtitles file \"{}\" is unavailable", params.getSid().getName());
					}
				} else {
					originalSubsFilename = resource.getFileName();
				}

				if (originalSubsFilename != null) {
					subsFilter.append("subtitles=").append(StringUtil.ffmpegEscape(originalSubsFilename));
					if (params.getSid().isEmbedded()) {
						subsFilter.append(":si=").append(params.getSid().getId());
					}

					// Set the input subtitles character encoding if not UTF-8
					if (!params.getSid().isSubsUtf8()) {
						if (StringUtils.isNotBlank(configuration.getSubtitlesCodepage())) {
							subsFilter.append(":charenc=").append(configuration.getSubtitlesCodepage());
						} else if (params.getSid().getSubCharacterSet() != null) {
							subsFilter.append(":charenc=").append(params.getSid().getSubCharacterSet());
						}
					}

					// If the FFmpeg font config is enabled than we need to add settings to the filter. TODO there could be also changed the font type. See http://ffmpeg.org/ffmpeg-filters.html#subtitles-1
					if (configuration.isFFmpegFontConfig() && !is3D && !isSubsASS) { // Do not force style for 3D videos and ASS subtitles
						subsFilter.append(":force_style=");
						subsFilter.append("'");
						String fontName = configuration.getFont();
						if (StringUtils.isNotBlank(fontName)) {
							String font = CodecUtil.isFontRegisteredInOS(fontName);
							if (font != null) {
								subsFilter.append("Fontname=").append(font);
							}
						}

						// XXX (valib) If the font size is not acceptable it could be calculated better taking in to account the original video size. Unfortunately I don't know how to do that.
						subsFilter.append(",Fontsize=").append(15 * Double.parseDouble(configuration.getAssScale()));
						subsFilter.append(",PrimaryColour=").append(configuration.getSubsColor().getASSv4StylesHexValue());
						subsFilter.append(",Outline=").append(configuration.getAssOutline());
						subsFilter.append(",Shadow=").append(configuration.getAssShadow());
						subsFilter.append(",MarginV=").append(configuration.getAssMargin());
						subsFilter.append("'");
					}
				}
			} else if (params.getSid().getType().isPicture()) {
				StringBuilder subsPictureFilter = new StringBuilder();
				if (params.getSid().isEmbedded()) {
					// Embedded
					subsPictureFilter.append("[0:v][0:s:").append(mediaInfo.getSubtitlesTracks().indexOf(params.getSid())).append("]overlay");
					isSubsManualTiming = false;
				} else if (params.getSid().getExternalFile() != null) {
					// External
					videoFilterOptions.add("-i");
					videoFilterOptions.add(params.getSid().getExternalFile().getPath());
					subsPictureFilter.append("[0:v][1:s]overlay"); // this assumes the sub file is single-language
				}
				filterChain.add(0, subsPictureFilter.toString());
			}

			if (StringUtils.isNotBlank(subsFilter)) {
				if (params.getTimeSeek() > 0 && isSubsManualTiming) {
					filterChain.add("setpts=PTS+" + params.getTimeSeek() + "/TB"); // based on https://trac.ffmpeg.org/ticket/2067
				}

				filterChain.add(subsFilter.toString());
				if (params.getTimeSeek() > 0 && isSubsManualTiming) {
					filterChain.add("setpts=PTS-STARTPTS"); // based on https://trac.ffmpeg.org/ticket/2067
				}
			}
		}

		String overrideVF = renderer.getFFmpegVideoFilterOverride();
		if (StringUtils.isNotEmpty(overrideVF)) {
			filterChain.add(overrideVF);
		}

		// Convert 3D video to the other output 3D format or to 2D using "Output3DFormat = ml" or "Output3DFormat = mr" in the renderer conf
		String stereoLayout = null;
		String renderer3DOutputFormat = null;
		if (defaultVideoTrack != null && defaultVideoTrack.get3DLayout() != null) {
			stereoLayout = defaultVideoTrack.get3DLayout().toString().toLowerCase(Locale.ROOT);
			renderer3DOutputFormat = renderer.getOutput3DFormat();
		}

		if (
			is3D &&
			stereoLayout != null &&
			StringUtils.isNotBlank(renderer3DOutputFormat) &&
			!stereoLayout.equals(renderer3DOutputFormat)
		) {
			filterChain.add("stereo3d=" + stereoLayout + ":" + renderer3DOutputFormat);
		}

		if (!filterChain.isEmpty()) {
			videoFilterOptions.add("-filter_complex");
			videoFilterOptions.add(StringUtils.join(filterChain, ","));
		}

		return videoFilterOptions;
	}

	/**
	 * Returns a list of <code>String</code>s representing ffmpeg output
	 * options (i.e. options that define the output file's video codec,
	 * audio codec and container) compatible with the renderer's
	 * <code>TranscodeVideo</code> profile.
	 *
	 * @param item
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params output parameters
	 * @param canMuxVideoWithFFmpeg
	 *
	 * @return a {@link List} of <code>String</code>s representing the FFmpeg output parameters for the renderer according
	 * to its <code>TranscodeVideo</code> profile.
	 */
	protected synchronized List<String> getVideoTranscodeOptions(StoreItem item, MediaInfo media, OutputParams params, boolean canMuxVideoWithFFmpeg) {
		List<String> transcodeOptions = new ArrayList<>();
		final String filename = item.getFileName();
		final Renderer renderer = params.getMediaRenderer();
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		String customFFmpegOptions = renderer.getCustomFFmpegOptions();
		final EncodingFormat encodingFormat = item.getTranscodingSettings().getEncodingFormat();
		if (
			(
				encodingFormat.isTranscodeToWMV() &&
				!renderer.isXbox360()
			) ||
			(
				renderer.isXboxOne() &&
				purpose() == VIDEO_WEBSTREAM_ENGINE
			)
		) { // WMV
			transcodeOptions.add("-c:v");
			transcodeOptions.add("wmv2");

			if (!customFFmpegOptions.contains("-c:a ")) {
				transcodeOptions.add("-c:a");
				transcodeOptions.add("wmav2");
			}

			transcodeOptions.add("-f");
			transcodeOptions.add("asf");
		} else { // MP4H265AC3, MPEGPSMPEG2AC3, MPEGTSMPEG2AC3, MPEGTSH264AC3 or MPEGTSH264AAC
			final boolean isTsMuxeRVideoEngineActive = EngineFactory.isEngineActive(TsMuxeRVideo.ID);

			// Output audio codec
			boolean dtsRemux = isTsMuxeRVideoEngineActive &&
				configuration.isAudioEmbedDtsInPcm() &&
				params.getAid() != null &&
				params.getAid().isDTS() &&
				!isAviSynthEngine() &&
				renderer.isDTSPlayable();

			boolean isSubtitlesAndTimeseek = !isDisableSubtitles(params) && params.getTimeSeek() > 0;

			if (
				params.getAid() != null &&
				(
					(
						configuration.isAudioRemuxAC3() &&
						params.getAid().isAC3()
					) ||
					!params.getAid().isAC3()
				) &&
				renderer.isAudioStreamTypeSupportedInTranscodingContainer(params.getAid(), encodingFormat) &&
				!isAviSynthEngine() &&
				!isSubtitlesAndTimeseek &&
				ffmpegSupportsRemuxingAudioStreamToTranscodingContainer(params.getAid(), encodingFormat.getTranscodingContainer())
			) {
				// Audio remux if the renderer supports the audio stream inside the transcoding container
				if (!customFFmpegOptions.contains("-c:a ")) {
					transcodeOptions.add("-c:a");
					transcodeOptions.add("copy");
				}
			} else {
				// log the reason for not remuxing audio
				String logPrepend = "Audio was not remuxed because ";
				if (params.getAid() == null) {
					LOGGER.trace(logPrepend + "there is no audio");
				} else {
					if (!configuration.isAudioRemuxAC3() && params.getAid().isAC3()) {
						LOGGER.trace(logPrepend + "audio is AC-3 and the user setting to remux AC-3 is disabled");
					}
					if (!renderer.isAudioStreamTypeSupportedInTranscodingContainer(params.getAid(), encodingFormat)) {
						LOGGER.trace(logPrepend + "audio stream type {} is not supported inside the container {}", params.getAid().getAudioCodec(), encodingFormat);
					}
					if (!ffmpegSupportsRemuxingAudioStreamToTranscodingContainer(params.getAid(), encodingFormat.getTranscodingContainer())) {
						LOGGER.trace(logPrepend + "FFmpeg does not support remuxing the audio stream {} to the transcoding container {}", params.getAid().getAudioCodec(), encodingFormat.getTranscodingContainer());
					}
				}
				if (isAviSynthEngine()) {
					LOGGER.trace(logPrepend + "this is AviSynth");
				}
				if (isSubtitlesAndTimeseek) {
					LOGGER.trace(logPrepend + "there are subtitles and seeking involved");
				}

				if (dtsRemux) {
					// Audio is added in a separate process later
					transcodeOptions.add("-an");
				} else if (type() == Format.AUDIO) {
					// Skip
				} else if (!customFFmpegOptions.matches(".*-(c:a|codec:a|acodec).*")) {
					if (encodingFormat.isTranscodeToAAC()) {
						transcodeOptions.add("-c:a");
						transcodeOptions.add("aac");
					} else if (!customFFmpegOptions.contains("-c:a ")) {
						transcodeOptions.add("-c:a");
						transcodeOptions.add("ac3");
					}
				}
			}

			InputFile newInput;
			if (filename != null) {
				newInput = new InputFile();
				newInput.setFilename(filename);
				newInput.setPush(params.getStdIn());
			}

			// Output video codec

			// This may be useful in the future for better muxing support
			// Set a temporary container to see if the renderer would accept this media wrapped in our transcoding container
//			String originalContainer = media.getContainer();
//			if (renderer.isTranscodeToMPEGTS()) {
//				media.setContainer("mpegts");
//			} else {
//				media.setContainer("mpegps");
//			}
//			item.setMediaInfo(media);
//			if (renderer.getFormatConfiguration().getMatchedMIMEtype(item) != null) {
//				media.setContainer(originalContainer);
//				item.setMediaInfo(media);
//				transcodeOptions.add("-c:v");
//				transcodeOptions.add("copy");

			MediaVideo defaultVideoTrack = media.getDefaultVideoTrack();
			if (defaultVideoTrack != null) {
				if (canMuxVideoWithFFmpeg) {
					if (!customFFmpegOptions.contains("-c:v")) {
						transcodeOptions.add("-c:v");
						transcodeOptions.add("copy");
					}
				} else if (encodingFormat.isTranscodeToMPEG2() && !dtsRemux) {
					if (!customFFmpegOptions.contains("-c:v")) {
						transcodeOptions.add("-c:v");
						transcodeOptions.add("mpeg2video");
					}
				} else {
					String selectedTranscodeAccelerationMethod = null;

					if (!customFFmpegOptions.contains("-c:v")) {
						transcodeOptions.add("-c:v");

						if (encodingFormat.isTranscodeToH264()) {
							selectedTranscodeAccelerationMethod = configuration.getFFmpegGPUH264EncodingAccelerationMethod();
							transcodeOptions.add(selectedTranscodeAccelerationMethod);
						} else if (encodingFormat.isTranscodeToH265()) {
							selectedTranscodeAccelerationMethod = configuration.getFFmpegGPUH265EncodingAccelerationMethod();
							transcodeOptions.add(selectedTranscodeAccelerationMethod);
						}

						if (selectedTranscodeAccelerationMethod != null && selectedTranscodeAccelerationMethod.endsWith("nvenc")) {
							transcodeOptions.add("-preset");
							transcodeOptions.add("llhp");
						}
					}

					if (selectedTranscodeAccelerationMethod == null || selectedTranscodeAccelerationMethod.startsWith("libx264")) {
						if (!customFFmpegOptions.contains("-preset")) {
							transcodeOptions.add("-preset");

							// do not use ultrafast for compatibility problems, particularly Panasonic TVs
							transcodeOptions.add("superfast");
						}
						if (!customFFmpegOptions.contains("-level")) {
							transcodeOptions.add("-level");
							transcodeOptions.add("31");
						}

						// do not use -tune zerolatency for compatibility problems, particularly Panasonic TVs
					} else if (selectedTranscodeAccelerationMethod.startsWith("libx265")) {
						if (!customFFmpegOptions.contains("-preset")) {
							transcodeOptions.add("-preset");
							transcodeOptions.add("superfast");
						}
					}

					if (defaultVideoTrack.getBitDepth() == 8 || !renderer.isVideoBitDepthSupportedForAllFiletypes(10)) {
						transcodeOptions.add("-pix_fmt");
						transcodeOptions.add("yuv420p");
					}
				}

				// this makes FFmpeg output HDR metadata, and Dolby Vision metadata if we output MP4 (only HDR if we are outputting MPEG-TS)
				if (defaultVideoTrack.getHDRFormatForRenderer() != null) {
					transcodeOptions.add("-strict");
					transcodeOptions.add("unofficial");
				}
			}

			if (!customFFmpegOptions.contains("-f")) {
				// Output file format
				transcodeOptions.add("-f");
				if (dtsRemux) {
					transcodeOptions.add("mpeg2video");
				} else if (encodingFormat.isTranscodeToMPEGTS()) {
					transcodeOptions.add(FormatConfiguration.MPEGTS);
				} else if (encodingFormat.isTranscodeToMP4()) {
					transcodeOptions.add(FormatConfiguration.MP4);

					transcodeOptions.add("-movflags");
					transcodeOptions.add("frag_keyframe+faststart+delay_moov");
				} else {
					transcodeOptions.add("vob");
				}
			}
		}

		return transcodeOptions;
	}

	/**
	 * Returns the video bitrate spec for the current transcode according
	 * to the limits/requirements of the renderer and the user's settings.
	 *
	 * @param item
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the video bitrate options for this transcode
	 */
	public List<String> getVideoBitrateOptions(StoreItem item, MediaInfo media, OutputParams params, boolean dtsRemux) {
		List<String> videoBitrateOptions = new ArrayList<>();
		boolean low = false;
		Renderer renderer = params.getMediaRenderer();
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		MediaVideo defaultVideoTrack = media.getDefaultVideoTrack();
		EncodingFormat encodingFormat = item.getTranscodingSettings().getEncodingFormat();
		int[] defaultMaxBitrates = getVideoBitrateConfig(configuration.getMaximumBitrate());
		int[] rendererMaxBitrates = new int[2];

		if (renderer.getMaxVideoBitrate() > 0) {
			rendererMaxBitrates = getVideoBitrateConfig(Integer.toString(renderer.getMaxVideoBitrate()));
		}

		// Give priority to the renderer's maximum bitrate setting over the user's setting
		if (rendererMaxBitrates[0] > 0 && rendererMaxBitrates[0] < defaultMaxBitrates[0]) {
			LOGGER.trace(
				"Using video bitrate limit from {} configuration ({} Mb/s) because " +
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

		boolean isXboxOneWebVideo = renderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;
		int maximumBitrate = defaultMaxBitrates[0];

		if (renderer.getCBRVideoBitrate() == 0 && params.getTimeEnd() == 0) {
			if (rendererMaxBitrates[0] < 0) {
				// odd special case here
				// this is -1 so we guess that 3000 kbps is good
				defaultMaxBitrates[0] = 3000;
				low = true;
			} else {
				// Convert value from Mb to Kb
				defaultMaxBitrates[0] = 1000 * defaultMaxBitrates[0];
			}

			if (renderer.isHalveBitrate() && !configuration.isAutomaticMaximumBitrate()) {
				defaultMaxBitrates[0] /= 2;
				LOGGER.trace("Halving the video bitrate limit to {} kb/s", defaultMaxBitrates[0]);
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
			if (!isXboxOneWebVideo && encodingFormat.isTranscodeToH264()) {
				if (
					renderer.getH264LevelLimit() < 4.2 &&
					defaultMaxBitrates[0] > 31250
				) {
					defaultMaxBitrates[0] = 31250;
					bitrateLevel41Limited = true;
					LOGGER.trace("Adjusting the video bitrate limit to the H.264 Level 4.1-safe value of 31250 kb/s");
				}
				bufSize = defaultMaxBitrates[0];
			} else {
				if (defaultVideoTrack != null && defaultVideoTrack.isHDVideo()) {
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
				if (dtsRemux) {
					defaultMaxBitrates[0] -= 1510;
				} else {
					defaultMaxBitrates[0] -= configuration.getAudioBitrate();
				}

				// Round down to the nearest Mb
				defaultMaxBitrates[0] = defaultMaxBitrates[0] / 1000 * 1000;
				if (low) {
					defaultMaxBitrates[0] = 3000;
				}

				LOGGER.trace(
					"Adjusting the video bitrate limit to {} kb/s to make room for audio",
					defaultMaxBitrates[0]
				);
			}

			if (defaultMaxBitrates[0] > 0) {
				// prevent x265 buffer underflow errors
				if (!encodingFormat.isTranscodeToH265()) {
					videoBitrateOptions.add("-bufsize");
					videoBitrateOptions.add(String.valueOf(bufSize) + "k");
				}
				videoBitrateOptions.add("-maxrate");
				videoBitrateOptions.add(String.valueOf(defaultMaxBitrates[0]) + "k");
			}
		}

		if (isXboxOneWebVideo || encodingFormat.isTranscodeToMPEG2()) {
			// Add MPEG-2 quality settings
			String mpeg2Options = configuration.getMPEG2MainSettingsFFmpeg();
			String mpeg2OptionsRenderer = renderer.getCustomFFmpegMPEG2Options();

			// Renderer settings take priority over user settings
			if (StringUtils.isNotBlank(mpeg2OptionsRenderer)) {
				mpeg2Options = mpeg2OptionsRenderer;
			} else if (configuration.isAutomaticMaximumBitrate()) {
				// when the automatic bandwidth is used than use the proper automatic MPEG2 setting
				mpeg2Options = renderer.getAutomaticVideoQuality();
			}

			if (mpeg2Options.contains("Automatic")) {
				if (mpeg2Options.contains("Wireless")) {
					// Lower quality for 720p+ content
					if (media.getWidth() > 1280) {
						mpeg2Options = "-g 25 -qmin 2 -qmax 7";
					} else if (media.getWidth() > 720) {
						mpeg2Options = "-g 25 -qmin 2 -qmax 5";
					} else {
						mpeg2Options = "-g 25 -qmin 2 -qmax 3";
					}
				} else { // set the automatic wired quality
					mpeg2Options = "-g 5 -q:v 1 -qmin 2 -qmax 3";
				}
			}

			if (renderer.isPS3()) {
				// It has been reported that non-PS3 renderers prefer -g 5 but prefer 25 for PS3 because it lowers the average bitrate
				mpeg2Options = "-g 25 -q:v 1 -qmin 2 -qmax 3";
			}

			String[] customOptions = StringUtils.split(mpeg2Options);
			videoBitrateOptions.addAll(new ArrayList<>(Arrays.asList(customOptions)));
		} else {
			// Add x264 quality settings
			String x264CRF = configuration.getx264ConstantRateFactor();
			if (configuration.isAutomaticMaximumBitrate()) {
				x264CRF = renderer.getAutomaticVideoQuality();
			}

			// Remove comment from the value
			if (x264CRF.contains("/*")) {
				x264CRF = x264CRF.substring(x264CRF.indexOf("/*"));
			}

			if (x264CRF.contains("Automatic")) {
				if (x264CRF.contains("Wireless") || maximumBitrate < 70) {
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
					if (media.getWidth() > 720 && !encodingFormat.isTranscodeToH265()) {
						x264CRF = "19";
					}
				}
			}
			if (StringUtils.isNotBlank(x264CRF) && !renderer.nox264()) {
				videoBitrateOptions.add("-crf");
				videoBitrateOptions.add(x264CRF);
			}
		}

		return videoBitrateOptions;
	}

	/**
	 * Returns the audio bitrate spec for the current transcode according
	 * to the limits/requirements of the renderer.
	 *
	 * @param item
	 * @param media the media metadata for the video being streamed. May contain unset/null values (e.g. for web videos).
	 * @param params
	 * @return a {@link List} of <code>String</code>s representing the audio bitrate options for this transcode
	 */
	public List<String> getAudioBitrateOptions(StoreItem item, MediaInfo media, OutputParams params) {
		Renderer renderer = params.getMediaRenderer();
		List<String> audioBitrateOptions = new ArrayList<>();

		audioBitrateOptions.add("-q:a");
		audioBitrateOptions.add(DEFAULT_QSCALE);

		audioBitrateOptions.add("-ar");
		audioBitrateOptions.add("" + renderer.getTranscodedVideoAudioSampleRate());

		return audioBitrateOptions;
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
		return KEY_FFMPEG_PATH;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_FFMPEG_EXECUTABLE_TYPE;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public boolean isAviSynthEngine() {
		return false;
	}

	public String initialString() {
		String threads = " -threads 1";
		if (CONFIGURATION.isFfmpegMultithreading()) {
			if (Runtime.getRuntime().availableProcessors() == CONFIGURATION.getNumberOfCpuCores()) {
				threads = "";
			} else {
				threads = " -threads " + CONFIGURATION.getNumberOfCpuCores();
			}
		}
		return threads;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int type() {
		return Format.VIDEO;
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

	@Override
	public String getMimeType() {
		return HTTPResource.VIDEO_TRANSCODE;
	}

	@Override
	public boolean isGPUAccelerationReady() {
		return true;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		StoreItem item,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		Renderer renderer = params.getMediaRenderer();
		// Use device-specific pms conf
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		MediaVideo defaultVideoTrack = media.getDefaultVideoTrack();
		final String filename = item.getFileName();
		final EncodingFormat encodingFormat = item.getTranscodingSettings().getEncodingFormat();
		InputFile newInput = new InputFile();
		newInput.setFilename(filename);
		newInput.setPush(params.getStdIn());

		List<String> cmdList = new ArrayList<>();
		boolean avisynth = isAviSynthEngine();
		if (params.getTimeSeek() > 0) {
			params.setWaitBeforeStart(1);
		} else if (renderer.isTranscodeFastStart()) {
			params.manageFastStart();
		} else {
			params.setWaitBeforeStart(2500);
		}

		setAudioAndSubs(item, params);
		cmdList.add(getExecutable());

		// Prevent FFmpeg timeout
		cmdList.add("-y");

		setLogLevel(cmdList, configuration);
		setDecodingOptions(cmdList, configuration, avisynth);

		final boolean isTsMuxeRVideoEngineActive = EngineFactory.isEngineActive(TsMuxeRVideo.ID);
		final boolean isXboxOneWebVideo = renderer.isXboxOne() && purpose() == VIDEO_WEBSTREAM_ENGINE;

		boolean ac3Remux = false;
		boolean dtsRemux = false;

		if (
			configuration.isAudioRemuxAC3() &&
			params.getAid() != null &&
			(
				params.getAid().isAC3() ||
				params.getAid().isEAC3()
			) &&
			!isAviSynthEngine() &&
			encodingFormat.isTranscodeToAC3() &&
			!isXboxOneWebVideo &&
			params.getAid().getNumberOfChannels() <= configuration.getAudioChannelCount()
		) {
			// AC-3 remux takes priority
			ac3Remux = true;
		} else {
			// Now check for DTS remux and LPCM streaming
			dtsRemux = isTsMuxeRVideoEngineActive &&
				configuration.isAudioEmbedDtsInPcm() &&
				params.getAid() != null &&
				params.getAid().isDTS() &&
				!isAviSynthEngine() &&
				renderer.isDTSPlayable();
		}

		String frameRateRatio = getValidFps(media.getFrameRate(), true);
		String frameRateNumber = getValidFps(media.getFrameRate(), false);

		// Set seeks
		if (params.getTimeSeek() > 0 && !avisynth) {
			cmdList.add("-ss");
			cmdList.add(String.valueOf(params.getTimeSeek()));
		}

		boolean isConvertedTo3d = false;

		// Input filename
		cmdList.add("-i");
		if (avisynth && !filename.toLowerCase().endsWith(".iso") && this instanceof AviSynthFFmpeg aviSynthFFmpeg) {
			AviSynthScriptGenerationResult aviSynthScriptGenerationResult = aviSynthFFmpeg.getAVSScript(filename, params, frameRateRatio, frameRateNumber, media);
			cmdList.add(ProcessUtil.getShortFileNameIfWideChars(aviSynthScriptGenerationResult.getAvsFile().getAbsolutePath()));
			isConvertedTo3d = aviSynthScriptGenerationResult.isConvertedTo3d();
		} else {
			if (params.getStdIn() != null) {
				cmdList.add("pipe:");
			} else {
				cmdList.add(filename);
			}
		}

		/**
		 * Defer to MEncoder for subtitles if:
		 * - MEncoder is enabled and available
		 * - The setting is enabled
		 * - There are subtitles to transcode
		 * - The file is not being played via the transcode folder
		 * - The file is not Dolby Vision, because our MEncoder implementation can't handle that yet
		 */
		if (
			EngineFactory.isEngineActive(MEncoderVideo.ID) &&
			!(renderer instanceof OutputOverride) &&
			params.getSid() != null &&
			!item.isInsideTranscodeFolder() &&
			configuration.isFFmpegDeferToMEncoderForProblematicSubtitles() &&
			params.getSid().isEmbedded() &&
			(
				params.getSid().getType().isText() ||
				params.getSid().getType() == SubtitleType.VOBSUB
			) &&
			!(defaultVideoTrack != null && defaultVideoTrack.getHDRFormatForRenderer() != null && defaultVideoTrack.getHDRFormatForRenderer().equals("dolbyvision"))
		) {
			LOGGER.debug("Switching from FFmpeg to MEncoder to transcode subtitles because the user setting is enabled.");
			MEncoderVideo mv = (MEncoderVideo) EngineFactory.getEngine(StandardEngineId.MENCODER_VIDEO, false, true);
			if (mv != null) {
				return mv.launchTranscode(item, media, params);
			}
		}

		boolean canMuxVideoWithFFmpeg = true;
		boolean canMuxVideoWithFFmpegIfTsMuxerIsNotUsed = false;
		String prependFfmpegTraceReason = "Not muxing the video stream with FFmpeg because ";
		if (!(renderer instanceof OutputOverride)) {
			if (!renderer.isVideoStreamTypeSupportedInTranscodingContainer(media, encodingFormat, null)) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "the video codec is not the same as the transcoding goal.");
			} else if (item.isInsideTranscodeFolder()) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "the file is being played via a FFmpeg entry in the TRANSCODE folder.");
			} else if (params.getSid() != null) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "we need to burn subtitles.");
			} else if (isAviSynthEngine()) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "we are using AviSynth.");
			} else if (defaultVideoTrack.isH264() && !isVideoWithinH264LevelLimits(defaultVideoTrack, renderer)) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "the video stream is not within H.264 level limits for this renderer.");
			} else if ("bt.601".equals(defaultVideoTrack.getMatrixCoefficients())) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "the colorspace probably isn't supported by the renderer.");
			} else if ((renderer.isKeepAspectRatio() || renderer.isKeepAspectRatioTranscoding()) && !"16:9".equals(defaultVideoTrack.getDisplayAspectRatio())) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "the renderer needs us to add borders so it displays the correct aspect ratio of " + defaultVideoTrack.getDisplayAspectRatio() + ".");
			} else if (!renderer.isResolutionCompatibleWithRenderer(media.getWidth(), media.getHeight())) {
				canMuxVideoWithFFmpeg = false;
				LOGGER.debug(prependFfmpegTraceReason + "the resolution is incompatible with the renderer.");
			} else if (!encodingFormat.isTranscodeToMP4H265AC3() && defaultVideoTrack.getHDRFormatForRenderer() != null && defaultVideoTrack.getHDRFormatForRenderer().equals("dolbyvision")) {
				canMuxVideoWithFFmpeg = false;
				boolean videoWouldBeCompatibleInTsContainer = renderer.getFormatConfiguration().getMatchedMIMEtype(
					"mpegts",
					defaultVideoTrack.getCodec(),
					null,
					0,
					0,
					defaultVideoTrack.getBitRate(),
					0,
					defaultVideoTrack.getWidth(),
					defaultVideoTrack.getHeight(),
					defaultVideoTrack.getBitDepth(),
					defaultVideoTrack.getHDRFormatForRenderer(),
					defaultVideoTrack.getHDRFormatCompatibilityForRenderer(),
					defaultVideoTrack.getExtras(),
					null,
					false,
					renderer
				) != null;
				if (videoWouldBeCompatibleInTsContainer) {
					canMuxVideoWithFFmpegIfTsMuxerIsNotUsed = true;
				}
				LOGGER.debug(prependFfmpegTraceReason + "the file is Dolby Vision and FFmpeg only outputs Dolby Vision metadata to MP4 containers as of FFmpeg 7.0.1 (worth re-checking periodically).");
			}
		}

		// Decide whether to defer to tsMuxeR or continue to use FFmpeg
		boolean deferToTsmuxer = !canMuxVideoWithFFmpeg;
		if (!(renderer instanceof OutputOverride) && configuration.isFFmpegMuxWithTsMuxerWhenCompatible()) {
			// Decide whether to defer to tsMuxeR or continue to use FFmpeg
			String prependTraceReason = "Not muxing the video stream with tsMuxeR via FFmpeg because ";
			if (item.isInsideTranscodeFolder()) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the file is being played via a FFmpeg entry in the TRANSCODE folder.");
			} else if (!renderer.isVideoStreamTypeSupportedInTranscodingContainer(media, encodingFormat, FormatConfiguration.MPEGTS)) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the renderer does not support {} inside MPEG-TS.", defaultVideoTrack.getCodec());
			} else if (params.getSid() != null && !(defaultVideoTrack.getHDRFormatForRenderer() != null && defaultVideoTrack.getHDRFormatForRenderer().equals("dolbyvision"))) {
				deferToTsmuxer = false;
				/**
				 * @todo here we are manually preventing hardcoding subtitles
				 * to a Dolby Vision video stream, because the colors will be
				 * wrong and unwatchable. When this FFmpegVideo engine supports
				 * handling and encoding Dolby Vision streams we should remove
				 * this condition
				 */
				LOGGER.debug(prependTraceReason + "we need to burn subtitles.");
			} else if (isAviSynthEngine()) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "we are using AviSynth.");
			} else if (defaultVideoTrack.isH264() && !isVideoWithinH264LevelLimits(defaultVideoTrack, renderer)) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the video stream is not within H.264 level limits for this renderer.");
			} else if (!isMuxable(defaultVideoTrack, renderer)) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the video stream is not muxable to this renderer");
			} else if (!defaultVideoTrack.isDisplayAspectRatioFromCodec()) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "we need to transcode to apply the correct aspect ratio.");
			} else if ("bt.601".equals(defaultVideoTrack.getMatrixCoefficients())) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the colorspace probably isn't supported by the renderer.");
			} else if ((renderer.isKeepAspectRatio() || renderer.isKeepAspectRatioTranscoding()) && !"16:9".equals(defaultVideoTrack.getDisplayAspectRatio())) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the renderer needs us to add borders so it displays the correct aspect ratio of " + defaultVideoTrack.getDisplayAspectRatio() + ".");
			} else if (!renderer.isResolutionCompatibleWithRenderer(media.getWidth(), media.getHeight())) {
				deferToTsmuxer = false;
				LOGGER.debug(prependTraceReason + "the resolution is incompatible with the renderer.");
			} else if (!EngineFactory.isEngineAvailable(StandardEngineId.TSMUXER_VIDEO)) {
				deferToTsmuxer = false;
				LOGGER.warn(prependTraceReason + "the configured executable isn't available.");
			}
			if (deferToTsmuxer) {
				TsMuxeRVideo tsMuxeRVideoInstance = (TsMuxeRVideo) EngineFactory.getEngine(StandardEngineId.TSMUXER_VIDEO, false, true);
				params.setForceFps(getValidFps(media.getFrameRate(), false));

				if (defaultVideoTrack != null && defaultVideoTrack.getCodec() != null) {
					if (defaultVideoTrack.isH264()) {
						params.setForceType("V_MPEG4/ISO/AVC");
					} else if (defaultVideoTrack.isH265()) {
						params.setForceType("V_MPEGH/ISO/HEVC");
					} else if (defaultVideoTrack.getCodec().startsWith("mpeg2")) {
						params.setForceType("V_MPEG-2");
					} else if (defaultVideoTrack.getCodec().equals("vc1")) {
						params.setForceType("V_MS/VFW/WVC1");
					}
				}

				LOGGER.debug("Deferring from FFmpeg to tsMuxeR");

				return tsMuxeRVideoInstance.launchTranscode(item, media, params);
			}
		}

		// If we got here, we are not deferring to tsMuxeR and can mux the video
		if (canMuxVideoWithFFmpegIfTsMuxerIsNotUsed) {
			canMuxVideoWithFFmpeg = true;
		}

		// Apply any video filters and associated options. These should go
		// after video input is specified and before output streams are mapped.
		List<String> videoFilterOptions = getVideoFilterOptions(item, media, params, isConvertedTo3d);
		if (!videoFilterOptions.isEmpty()) {
			cmdList.addAll(videoFilterOptions);
			canMuxVideoWithFFmpeg = false;
			LOGGER.debug(prependFfmpegTraceReason + "video filters are being applied.");
		}

		// Map the proper audio stream when there are multiple audio streams.
		// For video the FFMpeg automatically chooses the stream with the highest resolution.
		if (media.getAudioTracks().size() > 1) {
			/**
			 * Use the first video stream that is not an attached picture, video
			 * thumbnail or cover art.
			 *
			 * @see https://web.archive.org/web/20160609011350/https://ffmpeg.org/ffmpeg.html#Stream-specifiers-1
			 * @todo find a way to automatically select proper stream when media
			 *       includes multiple video streams
			 */
			cmdList.add("-map");
			cmdList.add("0:V");

			cmdList.add("-map");
			cmdList.add("0:a:" + (media.getAudioTracks().indexOf(params.getAid())));
		}

		// Now configure the output streams

		// Encoder threads
		setEncodingThreads(cmdList, configuration);

		if (params.getTimeEnd() > 0) {
			cmdList.add("-t");
			cmdList.add(String.valueOf(params.getTimeEnd()));
		}

		// Add the output options (-f, -c:a, -c:v, etc.)

		// Now that inputs and filtering are complete, see if we should
		// give the renderer the final say on the command
		boolean override = false;
		if (renderer instanceof OutputOverride outputOverride) {
			override = outputOverride.getOutputOptions(cmdList, item, this, params);
		}

		if (!override) {
			if (!canMuxVideoWithFFmpeg) {
				cmdList.addAll(getVideoBitrateOptions(item, media, params, dtsRemux));
			}

			String customFFmpegOptions = renderer.getCustomFFmpegOptions();

			// Audio bitrate
			if (!ac3Remux && !dtsRemux && (type() != Format.AUDIO)) {
				int channels = 0;
				if (
					(
						encodingFormat.isTranscodeToWMV() &&
						!renderer.isXbox360()
					) ||
					(
						renderer.isXboxOne() &&
						purpose() == VIDEO_WEBSTREAM_ENGINE
					)
				) {
					channels = 2;
				} else if (params.getAid() != null && params.getAid().getNumberOfChannels() > configuration.getAudioChannelCount()) {
					channels = configuration.getAudioChannelCount();
				}

				if (!customFFmpegOptions.contains("-ac ") && channels > 0) {
					cmdList.add("-ac");
					cmdList.add(String.valueOf(channels));
				}

				if (!customFFmpegOptions.matches(".* -(-ab|b:a) .*")) {
					cmdList.add("-ab");
					if (encodingFormat.isTranscodeToAAC()) {
						cmdList.add(Math.min(configuration.getAudioBitrate(), 320) + "k");
					} else {
						cmdList.add(String.valueOf(CodecUtil.getAC3Bitrate(configuration, params.getAid())) + "k");
					}
				}

				if (
					!customFFmpegOptions.contains("-ar ") &&
					params.getAid() != null &&
					params.getAid().getSampleRate() != renderer.getTranscodedVideoAudioSampleRate()
				) {
					cmdList.add("-ar");
					cmdList.add("" + renderer.getTranscodedVideoAudioSampleRate());
				}

				// Use high quality resampler
				// The parameters of http://forum.minimserver.com/showthread.php?tid=4181&pid=27185 are used.
				if (
					!customFFmpegOptions.contains("--resampler") &&
					params.getAid() != null &&
					params.getAid().getSampleRate() != renderer.getTranscodedVideoAudioSampleRate() &&
					configuration.isFFmpegSoX()
				) {
					cmdList.add("-resampler");
					cmdList.add("soxr");
					cmdList.add("-precision");
					cmdList.add("33");
					cmdList.add("-cheby");
					cmdList.add("1");
				}
			}

			// Add the output options (-f, -c:a, -c:v, etc.)
			cmdList.addAll(getVideoTranscodeOptions(item, media, params, canMuxVideoWithFFmpeg));

			// Add custom options
			if (StringUtils.isNotEmpty(customFFmpegOptions)) {
				parseOptions(customFFmpegOptions, cmdList);
			}
		}

		// Set up the process
		IPipeProcess pipe = null;

		if (!dtsRemux) {
			// cmdList.add("pipe:");

			// basename of the named pipe:
			String fifoName = String.format(
				"ffmpegvideo_%d_%d",
				Thread.currentThread().getId(),
				System.currentTimeMillis()
			);

			// This process wraps the command that creates the named pipe
			pipe = PlatformUtils.INSTANCE.getPipeProcess(fifoName);
			pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created

			params.getInputPipes()[0] = pipe;

			// Output file
			cmdList.add(pipe.getInputPipe());
		}

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		setOutputParsing(configuration, item, pw, false);

		if (!dtsRemux && pipe != null) {
			ProcessWrapper mkfifoProcess = pipe.getPipeProcess();

			/**
			 * It can take a long time for Windows to create a named pipe (and
			 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
			 * the current thread.
			 */
			mkfifoProcess.runInSameThread();
			pw.attachProcess(mkfifoProcess); // Clean up the mkfifo process when the transcode ends

			// Give the mkfifo process a little time
			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				LOGGER.error("Thread interrupted while waiting for named pipe to be created", e);
				Thread.currentThread().interrupt();
			}
		} else {
			pipe = PlatformUtils.INSTANCE.getPipeProcess(System.currentTimeMillis() + "tsmuxerout.ts");

			TsMuxeRVideo ts = (TsMuxeRVideo) EngineFactory.getEngine(StandardEngineId.TSMUXER_VIDEO, false, true);
			File f = new File(CONFIGURATION.getTempFolder(), "ums-tsmuxer.meta");
			String[] cmd = new String[]{ts.getExecutable(), f.getAbsolutePath(), pipe.getInputPipe()};
			pw = new ProcessWrapperImpl(cmd, params);

			PipeIPCProcess ffVideoPipe = new PipeIPCProcess(System.currentTimeMillis() + "ffmpegvideo", System.currentTimeMillis() + "videoout", false, true);

			cmdList.add(ffVideoPipe.getInputPipe());

			OutputParams ffparams = new OutputParams(configuration);
			ffparams.setMaxBufferSize(1);
			ffparams.setStdIn(params.getStdIn());

			String[] cmdArrayDts = new String[cmdList.size()];
			cmdList.toArray(cmdArrayDts);

			ProcessWrapperImpl ffVideo = new ProcessWrapperImpl(cmdArrayDts, ffparams);

			ProcessWrapper ffVideoPipeProcess = ffVideoPipe.getPipeProcess();
			pw.attachProcess(ffVideoPipeProcess);
			ffVideoPipeProcess.runInNewThread();
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

			List<String> cmdListDTS = new ArrayList<>();
			cmdListDTS.add(getExecutable());
			cmdListDTS.add("-y");
			cmdListDTS.add("-ss");

			if (params.getTimeSeek() > 0) {
				cmdListDTS.add(String.valueOf(params.getTimeSeek()));
			} else {
				cmdListDTS.add("0");
			}

			if (params.getStdIn() == null) {
				cmdListDTS.add("-i");
			} else {
				cmdListDTS.add("-");
			}
			cmdListDTS.add(filename);

			if (params.getTimeSeek() > 0) {
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

			if (!renderer.isMuxDTSToMpeg()) { // No need to use the PCM trick when media renderer supports DTS
				ffAudioPipe.setModifier(sm);
			}

			OutputParams ffaudioparams = new OutputParams(configuration);
			ffaudioparams.setMaxBufferSize(1);
			ffaudioparams.setStdIn(params.getStdIn());
			ProcessWrapperImpl ffAudio = new ProcessWrapperImpl(cmdArrayDTS, ffaudioparams);

			params.setStdIn(null);
			try (PrintWriter pwMux = new PrintWriter(f)) {
				pwMux.println("MUXOPT --no-pcr-on-video-pid --no-asyncio --new-audio-pes --vbr --vbv-len=500");
				String videoType = "V_MPEG-2";

				if (encodingFormat.isTranscodeToH264()) {
					videoType = "V_MPEG4/ISO/AVC";
				} else if (encodingFormat.isTranscodeToH265()) {
					videoType = "V_MPEGH/ISO/HEVC";
				}

				if (params.isNoVideoEncode() && params.getForceType() != null) {
					videoType = params.getForceType();
				}

				StringBuilder fps = new StringBuilder();
				fps.append("");
				if (params.getForceFps() != null) {
					fps.append("fps=").append(params.getForceFps()).append(", ");
				}

				String audioType = "A_AC3";
				if (dtsRemux) {
					if (renderer.isMuxDTSToMpeg()) {
						// Renderer can play proper DTS track
						audioType = "A_DTS";
					} else {
						// DTS padded in LPCM trick
						audioType = "A_LPCM";
					}
				}

				String videoparams;
				if (encodingFormat.isTranscodeToH264()) {
					String sei = "insertSEI";
					if (
						renderer.isPS3() &&
						isWebDl(filename, media, params)
					) {
						sei = "forceSEI";
					}
					videoparams = "level=4.1, " + sei + ", contSPS, track=1";
				} else {
					videoparams = "track=1";
				}
				pwMux.println(videoType + ", \"" + ffVideoPipe.getOutputPipe() + "\", " + fps + videoparams);
				pwMux.println(audioType + ", \"" + ffAudioPipe.getOutputPipe() + "\", track=2");
			}

			ProcessWrapper pipeProcess = pipe.getPipeProcess();
			pw.attachProcess(pipeProcess);
			pipeProcess.runInNewThread();

			try {
				wait(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			pipe.deleteLater();
			params.getInputPipes()[0] = pipe;

			ProcessWrapper ffPipeProcess = ffAudioPipe.getPipeProcess();
			pw.attachProcess(ffPipeProcess);
			ffPipeProcess.runInNewThread();

			try {
				wait(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			ffAudioPipe.deleteLater();
			pw.attachProcess(ffAudio);
			ffAudio.runInNewThread();
		}

		// Launch the transcode command...
		pw.runInNewThread();
		// ...and wait briefly to allow it to start
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for transcode to start", e.getMessage());
			LOGGER.trace("", e);
			Thread.currentThread().interrupt();
		}
		return pw;
	}

	public static void setLogLevel(List<String> cmdList, UmsConfiguration configuration) {
		cmdList.add("-loglevel");
		FFmpegLogLevels askedLogLevel = FFmpegLogLevels.valueOfLabel(configuration.getFFmpegLoggingLevel());
		if (LOGGER.isTraceEnabled()) {
			// Set -loglevel in accordance with LOGGER setting
			if (FFmpegLogLevels.INFO.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("info");
			} else {
				cmdList.add(askedLogLevel.label);
			}
		} else {
			if (FFmpegLogLevels.FATAL.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("fatal");
			} else {
				cmdList.add(askedLogLevel.label);
			}
			cmdList.add("-hide_banner");
		}
	}

	public static void setDecodingOptions(List<String> cmdList, UmsConfiguration configuration, boolean avisynth) {
		/*
		 * FFmpeg uses multithreading by default, so provided that the
		 * user has not disabled FFmpeg multithreading and has not
		 * chosen to use more or less threads than are available, do not
		 * specify how many cores to use.
		 */
		int nThreads = 1;
		if (configuration.isFfmpegMultithreading()) {
			if (Runtime.getRuntime().availableProcessors() == configuration.getNumberOfCpuCores()) {
				nThreads = 0;
			} else {
				nThreads = configuration.getNumberOfCpuCores();
			}
		}
		// Decoding threads and GPU decoding
		if (nThreads > 0 && !configuration.isGPUAcceleration()) {
			cmdList.add("-threads");
			cmdList.add(String.valueOf(nThreads));
		} else if (
			configuration.isGPUAcceleration() &&
			!avisynth &&
			!configuration.getFFmpegGPUDecodingAccelerationMethod().equals("none")
		) {
			// GPU decoding method
			if (configuration.getFFmpegGPUDecodingAccelerationMethod().trim().matches("(auto|cuda|cuvid|d3d11va|dxva2|vaapi|vdpau|videotoolbox|qsv)")) {
				cmdList.add("-hwaccel");
				cmdList.add(configuration.getFFmpegGPUDecodingAccelerationMethod().trim());
			} else {
				if (configuration.getFFmpegGPUDecodingAccelerationMethod().matches(".*-hwaccel +[a-z]+.*")) {
					String[] hwaccelOptions = StringUtils.split(configuration.getFFmpegGPUDecodingAccelerationMethod());
					cmdList.addAll(Arrays.asList(hwaccelOptions));
				} else {
					cmdList.add("-hwaccel");
					cmdList.add("auto");
				}
			}

			// GPU decoding threads
			if (configuration.getFFmpegGPUDecodingAccelerationThreadNumber().trim().matches("^[0-9]+$")) {
				if (Integer.parseInt(configuration.getFFmpegGPUDecodingAccelerationThreadNumber().trim()) > 0) {
					cmdList.add("-threads");
					cmdList.add(String.valueOf(configuration.getFFmpegGPUDecodingAccelerationThreadNumber().trim()));
				}
			} else {
				cmdList.add("-threads");
				cmdList.add("1");
			}
		}
	}

	public static void setEncodingThreads(List<String> cmdList, UmsConfiguration configuration) {
		/*
		 * FFmpeg uses multithreading by default, so provided that the
		 * user has not disabled FFmpeg multithreading and has not
		 * chosen to use more or less threads than are available, do not
		 * specify how many cores to use.
		 */
		int nThreads = 1;
		if (configuration.isFfmpegMultithreading()) {
			if (Runtime.getRuntime().availableProcessors() == configuration.getNumberOfCpuCores()) {
				nThreads = 0;
			} else {
				nThreads = configuration.getNumberOfCpuCores();
			}
		}
		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}
	}

	public static ProcessWrapperImpl runHlsTranscodeProcess(OutputParams params, List<String> cmdList) {
		// Set up the process
		// basename of the named pipe:
		String fifoName = String.format(
			"ffmpeghlsvideo_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		IPipeProcess pipe = PlatformUtils.INSTANCE.getPipeProcess(fifoName);
		pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		ProcessWrapper mkfifoProcess = pipe.getPipeProcess();

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifoProcess.runInSameThread();

		params.getInputPipes()[0] = pipe;

		// Output file
		cmdList.add(pipe.getInputPipe());

		// Convert the command list to an array
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		// Now launch FFmpeg
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(mkfifoProcess); // Clean up the mkfifo process when the transcode ends

		// Give the mkfifo process a little time
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for named pipe to be created", e);
			Thread.currentThread().interrupt();
		}

		// Launch the transcode command...
		pw.runInNewThread();
		// ...and wait briefly to allow it to start
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for transcode to start", e);
			Thread.currentThread().interrupt();
		}
		return pw;
	}

	/**
	 * A simple arg parser with basic quote comprehension
	 */
	public static List<String> parseOptions(String str) {
		return str == null ? null : parseOptions(str, new ArrayList<>());
	}

	protected static List<String> parseOptions(String str, List<String> cmdList) {
		int start;
		int pos = 0;
		int len = str.length();
		while (pos < len) {
			// New arg
			if (str.charAt(pos) == '\"') {
				start = pos + 1;
				// Find next quote. No support for escaped quotes here, and
				// -1 means no matching quote but be lax and accept the fragment anyway
				pos = str.indexOf('"', start);
			} else {
				start = pos;
				// Find next space
				pos = str.indexOf(' ', start);
			}
			if (pos == -1) {
				// We're done
				pos = len;
			}
			// Add the arg, if any
			if (pos - start > 0) {
				cmdList.add(str.substring(start, pos).trim());
			}
			pos++;
			// Advance to next non-space char
			while (pos < len && str.charAt(pos) == ' ') {
				pos++;
			}
		}
		return cmdList;
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
		UmsConfiguration configuration = params.getMediaRenderer().getUmsConfiguration();
		return configuration.isDisableSubtitles() || (params.getSid() == null) || isAviSynthEngine();
	}

	@Override
	public boolean isCompatible(StoreItem item) {
		MediaAudio audio = item.getMediaInfo().getDefaultAudioTrack();
		if (audio != null && audio.isAC4()) {
			LOGGER.trace("Ignoring file \"{}\" because audio is AC-4 and engine is FFmpeg so skip it until FFmpeg" +
				" will support it.", item.getName());
			return false;
		}
		return (
			PlayerUtil.isVideo(item, Format.Identifier.MKV) ||
			PlayerUtil.isVideo(item, Format.Identifier.MPG) ||
			PlayerUtil.isVideo(item, Format.Identifier.OGG) ||
			"m3u8".equals(item.getFormat().getMatchedExtension())
		);
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return encodingFormat.isVideoFormat() && !encodingFormat.isTranscodeToHLS();
	}

	// matches 'Duration: 00:17:17.00' but not 'Duration: N/A'
	static final Matcher RE_DURATION = Pattern.compile("Duration:\\s+([\\d:.]+),").matcher("");

	/**
	 * Set up a filter to parse ffmpeg's stderr output for info
	 * (e.g. duration) if required.
	 */
	public void setOutputParsing(UmsConfiguration configuration, final StoreItem resource, ProcessWrapperImpl pw, boolean force) {
		if (configuration.isResumeEnabled() && resource.getMediaInfo() != null) {
			long duration = force ? 0 : (long) resource.getMediaInfo().getDurationInSeconds();
			if (duration == 0 || duration == StoreItem.TRANS_SIZE) {
				OutputTextLogger ffParser = new OutputTextLogger(null) {
					@Override
					public boolean filter(String line) {
						if (RE_DURATION.reset(line).find()) {
							String d = RE_DURATION.group(1);
							LOGGER.trace("[{}] setting duration: {}", ID, d);
							resource.getMediaInfo().setDuration(StringUtil.convertStringToTime(d));
							return false; // done, stop filtering
						}
						return true; // keep filtering
					}
				};
				ffParser.setFiltered(true);
				pw.setStderrConsumer(ffParser);
			}
		}
	}

	@Override
	public boolean excludeFormat(Format extension) {
		return false;
	}

	@Override
	public ExecutableInfo testExecutable(@Nonnull ExecutableInfo executableInfo) {
		executableInfo = testExecutableFile(executableInfo);
		if (Boolean.FALSE.equals(executableInfo.getAvailable())) {
			return executableInfo;
		}
		final String arg = "-version";
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
					Pattern pattern = Pattern.compile("^ffmpeg version\\s+(.*?)\\s+Copyright", Pattern.CASE_INSENSITIVE);
					Matcher matcher = pattern.matcher(output.getOutput().get(0));
					if (matcher.find() && StringUtils.isNotBlank(matcher.group(1))) {
						result.version(new Version(matcher.group(1)));
					}
				}
				result.available(Boolean.TRUE);

				if (result instanceof FFmpegExecutableInfoBuilder fFmpegExecutableInfoBuilder) {
					List<String> protocols = FFmpegOptions.getSupportedProtocols(executableInfo.getPath());
					fFmpegExecutableInfoBuilder.protocols(protocols);
					if (protocols.isEmpty()) {
						LOGGER.warn("Couldn't parse any supported protocols for \"{}\"", executableInfo.getPath());
					} else {
						LOGGER.debug("{} supported protocols: {}", executableInfo.getPath(), protocols);
					}
				} else {
					LOGGER.error("Could not store FFmpeg supported protocols because of an internal error");
				}
			} else {
				NTStatus ntStatus = Platform.isWindows() ? NTStatus.typeOf(output.getExitCode()) : null;
				if (ntStatus != null) {
					result.errorType(ExecutableErrorType.GENERAL);
					result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + "\n\n" + ntStatus);
				} else {
					if (output.getOutput().size() > 2) {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(
							String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" +
							output.getOutput().get(output.getOutput().size() - 2) + " " +
							output.getOutput().get(output.getOutput().size() - 1)
						);
					} else if (output.getOutput().size() > 1) {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(
							String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + " \n" +
							output.getOutput().get(output.getOutput().size() - 1)
						);
					} else {
						result.errorType(ExecutableErrorType.GENERAL);
						result.errorText(String.format(Messages.getString("TranscodingEngineXNotAvailable"), this) + Messages.getString("UnknownError"));
					}
				}
				result.available(Boolean.FALSE);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return result.build();
	}

	@Override
	protected boolean isSpecificTest() {
		return false;
	}

	/**
	 * @param fileAudio
	 * @param transcodingContainer
	 * @return whether FFmpeg will error if we try to remux the audio in the container
	 */
	private boolean ffmpegSupportsRemuxingAudioStreamToTranscodingContainer(MediaAudio fileAudio, String transcodingContainer) {
		if (transcodingContainer.equals(FormatConfiguration.MPEGPS)) {
			/**
			 * FFmpeg says this for VOB (MPEG-PS):
			 * Must be one of mp1, mp2, mp3, 16-bit pcm_dvd, pcm_s16be, ac3 or dts
			 * Worth noting that MPEG-PS itself seems to support AAC audio, but FFmpeg does
			 * not support doing that.
			 *
			 * @see https://github.com/UniversalMediaServer/UniversalMediaServer/issues/4901
			 */
			return fileAudio.isMpegAudio() ||
				fileAudio.isMP3() ||
				fileAudio.isAC3() ||
				fileAudio.isDTS() ||
				fileAudio.isPCM();
		}

		// Other containers could be added to this method to make it more complete. For now just return true for them.

		return true;
	}
}
