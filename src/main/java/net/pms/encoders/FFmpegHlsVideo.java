/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import net.pms.configuration.DeviceConfiguration;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.encoders.HlsHelper.HlsConfiguration;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegHlsVideo extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegHlsVideo.class);
	public static final PlayerId ID = StandardPlayerId.FFMPEG_HLS_VIDEO;
	public static final String NAME = "FFmpeg Hls Video";

	// Not to be instantiated by anything but PlayerFactory
	FFmpegHlsVideo() {
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public PlayerId id() {
		return ID;
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_PLAYER;
	}

	@Override
	public boolean isTimeSeekable() {
		return true;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);
		params.setWaitBeforeStart(1000);
		// Use device-specific conf
		PmsConfiguration prev = configuration;
		configuration = (DeviceConfiguration) params.getMediaRenderer();
		HlsConfiguration hlsConfiguration = params.getHlsConfiguration();
		boolean needVideo = hlsConfiguration.video.resolutionWidth > -1;
		boolean needAudio = hlsConfiguration.audioStream > -1;
		boolean needSubtitle = hlsConfiguration.subtitle > -1;
		String filename = dlna.getFileName();

		// Build the command line
		List<String> cmdList = new ArrayList<>();

		cmdList.add(getExecutable());

		// XXX squashed bug - without this, ffmpeg hangs waiting for a confirmation
		// that it can write to a file that already exists i.e. the named pipe
		cmdList.add("-y");

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
			if (FFmpegLogLevels.WARNING.isMoreVerboseThan(askedLogLevel)) {
				cmdList.add("warning");
			} else {
				cmdList.add(askedLogLevel.label);
			}
			cmdList.add("-hide_banner");
		}
		if (needSubtitle) {
			cmdList.add("-nostdin");
		}
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

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

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

		if (media.getAudioTracksList().size() > 1) {
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
		//cmdList.add("-sc_threshold");
		//cmdList.add("0");

		//setup video
		if (needVideo) {
			if (hlsConfiguration.video.resolutionWidth > 0) {
				cmdList.add("-s:v");
				cmdList.add(String.valueOf(hlsConfiguration.video.resolutionWidth) + "x" + String.valueOf(hlsConfiguration.video.resolutionHeight));
			}
			cmdList.add("-c:v");
			if (hlsConfiguration.video.videoCodec.startsWith("avc1.")) {
				LOGGER.trace("Something wrong, falling back to h264");
			}
			cmdList.add("libx264");
			cmdList.add("-keyint_min");
			cmdList.add("25");
			cmdList.add("-preset");
			cmdList.add("ultrafast");
			//set fps
			if (hlsConfiguration.video.framesPerSecond > 0) {
				cmdList.add("-r");
				cmdList.add(String.valueOf(hlsConfiguration.video.framesPerSecond));
			}
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
			cmdList.add("-pix_fmt");
			cmdList.add("yuv420p");
			cmdList.add("-level");
			String hexLevel = hlsConfiguration.video.videoCodec.substring(hlsConfiguration.video.videoCodec.length() - 2);
			int level;
			try {
				level = Integer.parseInt(hexLevel, 16);
			} catch (NumberFormatException ie) {
				level = 30;
			}
			cmdList.add(String.valueOf(level));

			// https://trac.ffmpeg.org/wiki/Limiting%20the%20output%20bitrate
			if (hlsConfiguration.video.maxVideoBitRate > 0) {
				cmdList.add("-maxrate");
				cmdList.add(String.valueOf(hlsConfiguration.video.maxVideoBitRate));
				cmdList.add("-bufsize");
				cmdList.add(String.valueOf(hlsConfiguration.video.maxVideoBitRate));
			}
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
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		cmdList.add("-f");
		if (needSubtitle && !needAudio && !needVideo) {
			cmdList.add("webvtt");
		} else {
			cmdList.add("mpegts");
			cmdList.add("-skip_estimate_duration_from_pts");
			cmdList.add("1");
			cmdList.add("-use_wallclock_as_timestamps");
			cmdList.add("1");
			//transcodeOptions.add("-mpegts_flags");
			//transcodeOptions.add("latm");
			cmdList.add("-movflags");
			cmdList.add("frag_keyframe"); //frag_keyframe
		}

		// Set up the process
		// basename of the named pipe:
		String fifoName = String.format(
			"ffmpeghlsvideo_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);
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
		}

		// Launch the transcode command...
		pw.runInNewThread();
		// ...and wait briefly to allow it to start
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("Thread interrupted while waiting for transcode to start", e);
		}

		configuration = prev;
		return pw;
	}

	@Override
	public String name() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(DLNAResource resource) {
		return false;
	}
}