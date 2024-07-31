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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.pms.configuration.FormatConfiguration;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.OutputParams;
import net.pms.io.ProcessWrapper;
import net.pms.media.MediaInfo;
import net.pms.network.HTTPResource;
import net.pms.store.StoreItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegHlsVideo extends FFMpegVideo {

	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegHlsVideo.class);

	public static final EngineId ID = StandardEngineId.FFMPEG_HLS_VIDEO;
	public static final String NAME = "FFmpeg HLS Video";

	// Not to be instantiated by anything but PlayerFactory
	FFmpegHlsVideo() {
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
			StoreItem resource,
			MediaInfo media,
			OutputParams params
	) throws IOException {
		if (!params.isHlsConfigured()) {
			LOGGER.error("No Hls configuration to transcode.");
			return null;
		}
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);
		params.setWaitBeforeStart(0);
		// Use device-specific conf
		UmsConfiguration configuration = params.getMediaRenderer().getUmsConfiguration();
		HlsHelper.HlsConfiguration hlsConfiguration = params.getHlsConfiguration();
		boolean needVideo = hlsConfiguration.video.resolutionWidth > -1;
		boolean needAudio = hlsConfiguration.audioStream > -1;
		boolean needSubtitle = hlsConfiguration.subtitle > -1;
		String filename = resource.getFileName();

		// Build the command line
		List<String> cmdList = new ArrayList<>();

		cmdList.add(getExecutable());

		// XXX squashed bug - without this, ffmpeg hangs waiting for a confirmation
		// that it can write to a file that already exists i.e. the named pipe
		cmdList.add("-y");

		setLogLevel(cmdList, configuration);

		if (needSubtitle) {
			cmdList.add("-nostdin");
		}

		// Decoding threads and GPU decoding
		setDecodingOptions(cmdList, configuration, false);

		if (params.getTimeSeek() > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.getTimeSeek());
		}

		if (params.getTimeEnd() > 0 && !needSubtitle) {
			cmdList.add("-t");
			cmdList.add(String.valueOf(params.getTimeEnd() - params.getTimeSeek()));
		}

		//don't decode stream if not needed
		if (!needVideo) {
			cmdList.add("-vn");
		}
		if (!needAudio) {
			cmdList.add("-an");
		}
		if (!needSubtitle) {
			cmdList.add("-sn");
		}
		cmdList.add("-i");
		cmdList.add(filename);

		if (needSubtitle) {
			cmdList.add("-map");
			cmdList.add("0:s:" + hlsConfiguration.subtitle);
			if (params.getTimeEnd() > 0) {
				cmdList.add("-t");
				cmdList.add(String.valueOf(params.getTimeEnd()));
			}
		} else {
			cmdList.add("-sn");
		}

		if (media.getAudioTracks().size() > 1) {
			if (needVideo) {
				cmdList.add("-map");
				cmdList.add("0:V");
			}
			if (needAudio) {
				cmdList.add("-map");
				cmdList.add("0:a:" + hlsConfiguration.audioStream);
			}
		}
		//remove data
		cmdList.add("-dn");
		cmdList.add("-copyts");

		//setup video
		if (needVideo) {
			if (hlsConfiguration.video.resolutionWidth > 0) {
				cmdList.add("-s:v");
				cmdList.add(String.valueOf(hlsConfiguration.video.resolutionWidth) + "x" + String.valueOf(hlsConfiguration.video.resolutionHeight));
			}
			cmdList.add("-c:v");
			if (!hlsConfiguration.video.videoCodec.startsWith("avc1.")) {
				LOGGER.error("Something wrong, hls codec not handled: {}", hlsConfiguration.video.videoCodec);
				return null;
			}

			String selectedTranscodeAccelerationMethod = configuration.getFFmpegGPUH264EncodingAccelerationMethod();
			cmdList.add(selectedTranscodeAccelerationMethod);
			cmdList.add("-keyint_min");
			cmdList.add("25");

			if (selectedTranscodeAccelerationMethod.startsWith("libx264")) {
				// Let x264 optimize the bitrate more for lower resolutions
				cmdList.add("-preset");
				if (hlsConfiguration.video.resolutionWidth < 842) {
					cmdList.add("superfast");
				} else {
					cmdList.add("ultrafast");
				}
			}

			/**
			 * 1.3a. For maximum compatibility, some H.264 variants SHOULD be
			 * less than or equal to High Profile, Level 4.1. 1.3b. * Profile
			 * and Level for H.264 MUST be less than or equal to High Profile,
			 * Level 5.2. 1.4. For H.264, you SHOULD use High Profile in
			 * preference to Main or Baseline Profile.
			 *
			 * @see
			 * https://developer.apple.com/documentation/http_live_streaming/http_live_streaming_hls_authoring_specification_for_apple_devices
			 */
			//set profile : baseline main high high10 high422 high444
			if (hlsConfiguration.video.videoCodec.startsWith("avc1.64")) {
				cmdList.add("-profile:v");
				cmdList.add("high");
			} else if (hlsConfiguration.video.videoCodec.startsWith("avc1.4D")) {
				//main profile
				cmdList.add("-profile:v");
				cmdList.add("main");
			} else if (hlsConfiguration.video.videoCodec.startsWith("avc1.42")) {
				//baseline profile
				cmdList.add("-profile:v");
				cmdList.add("baseline");
			}

			String hexLevel = hlsConfiguration.video.videoCodec.substring(hlsConfiguration.video.videoCodec.length() - 2);
			int level;
			try {
				level = Integer.parseInt(hexLevel, 16);
			} catch (NumberFormatException nfe) {
				level = 30;
			}
			cmdList.add("-level");
			cmdList.add(String.valueOf(level));

			cmdList.add("-pix_fmt");
			cmdList.add("yuv420p");
		} else {
			//don't encode stream if not needed
			cmdList.add("-vn");
		}
		if (needAudio) {
			//setup audio
			cmdList.add("-c:a");
			if (hlsConfiguration.audio.audioCodec.startsWith("mp4a.40.")) {
				if (!hlsConfiguration.audio.audioCodec.endsWith(".2")) {
					//LC-AAC should be 2
					//HE-AAC require libfdk_aac
					LOGGER.trace("Something wrong, falling back to aac");
				}
				cmdList.add("aac");
			} else {
				cmdList.add("ac3");
				//here should handle ac3 !!!!
			}
			if (hlsConfiguration.audio.audioChannels != 0) {
				cmdList.add("-ac");
				cmdList.add(String.valueOf(hlsConfiguration.audio.audioChannels));
			}
			if (hlsConfiguration.audio.audioBitRate > 0) {
				cmdList.add("-ab");
				cmdList.add(String.valueOf(hlsConfiguration.audio.audioBitRate));
			}
		} else {
			//don't encode stream if not needed
			cmdList.add("-an");
		}

		// Encoder threads
		setEncodingThreads(cmdList, configuration);

		cmdList.add("-f");
		if (needSubtitle && !needAudio && !needVideo) {
			cmdList.add("webvtt");
		} else {
			cmdList.add(FormatConfiguration.MPEGTS);
			cmdList.add("-skip_estimate_duration_from_pts");
			cmdList.add("1");
			cmdList.add("-use_wallclock_as_timestamps");
			cmdList.add("1");
			//transcodeOptions.add("-mpegts_flags");
			//transcodeOptions.add("latm");
			cmdList.add("-movflags");
			cmdList.add("frag_keyframe"); //frag_keyframe
		}

		return runHlsTranscodeProcess(params, cmdList);
	}

	@Override
	public boolean isCompatible(EncodingFormat encodingFormat) {
		return encodingFormat.isVideoFormat() && encodingFormat.isTranscodeToHLS();
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getMimeType() {
		return HTTPResource.HLS_TYPEMIME;
	}

}
