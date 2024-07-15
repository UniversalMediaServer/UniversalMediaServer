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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.pms.configuration.FFmpegWebFilters;
import net.pms.configuration.UmsConfiguration;
import net.pms.io.IPipeProcess;
import net.pms.io.OutputParams;
import net.pms.io.OutputTextLogger;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import net.pms.media.MediaInfo;
import net.pms.parsers.FFmpegParser;
import net.pms.platform.PlatformUtils;
import net.pms.renderers.OutputOverride;
import net.pms.renderers.Renderer;
import net.pms.store.StoreItem;
import net.pms.util.ExecutableInfo;
import net.pms.util.FFmpegExecutableInfo;
import net.pms.util.PlayerUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegWebVideo extends FFMpegVideo {

	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegWebVideo.class);
	public static final EngineId ID = StandardEngineId.FFMPEG_WEB_VIDEO;

	public static final String KEY_FFMPEG_WEB_EXECUTABLE_TYPE = "ffmpeg_web_executable_type";
	public static final String NAME = "FFmpeg Web Video";

	// Not to be instantiated by anything but PlayerFactory
	FFmpegWebVideo() {
	}

	@Override
	public EngineId getEngineId() {
		return ID;
	}

	@Override
	public String getExecutableTypeKey() {
		return KEY_FFMPEG_WEB_EXECUTABLE_TYPE;
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_ENGINE;
	}

	@Override
	public boolean isTimeSeekable() {
		return false;
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		StoreItem resource,
		MediaInfo media,
		OutputParams params
	) throws IOException {
		params.setMinBufferSize(params.getMinFileSize());
		params.setSecondReadMinSize(100000);

		Renderer renderer = params.getMediaRenderer();
		UmsConfiguration configuration = renderer.getUmsConfiguration();
		String filename = resource.getFileName();
		setAudioAndSubs(resource, params);

		// Workaround an FFmpeg bug: http://ffmpeg.org/trac/ffmpeg/ticket/998
		// Also see static init
		if (filename.startsWith("mms:")) {
			filename = "mmsh:" + filename.substring(4);
		}

		// check if we have modifier for this url
		filename = FFmpegWebFilters.getReplacements(filename);

		FFmpegOptions customOptions = new FFmpegOptions();

		// Gather custom options from various sources in ascending priority:
		// - automatic options
		List<String> opts = FFmpegWebFilters.getAutoOptions(filename);
		if (opts != null) {
			customOptions.addAll(opts);
		}
		// - (http) header options
		if (params.getHeader() != null && params.getHeader().length > 0) {
			String hdr = new String(params.getHeader(), StandardCharsets.UTF_8);
			customOptions.addAll(parseOptions(hdr));
		}
		// - attached options
		String attached = (String) resource.getAttachment(ID.toString());
		if (attached != null) {
			customOptions.addAll(parseOptions(attached));
		}
		// - renderer options
		String ffmpegOptions = renderer.getCustomFFmpegOptions();
		if (StringUtils.isNotEmpty(ffmpegOptions)) {
			customOptions.addAll(parseOptions(ffmpegOptions));
		}

		// Build the command line
		List<String> cmdList = new ArrayList<>();

		cmdList.add(getExecutable());

		// this stops FFmpeg waiting to write to a file that already exists i.e. the named pipe
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

		// Add global and input-file custom options, if any
		if (!customOptions.isEmpty()) {
			customOptions.transferGlobals(cmdList);
			customOptions.transferInputFileOptions(cmdList);
		}

		if (params.getTimeSeek() > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.getTimeSeek());
		}

		cmdList.add("-i");
		cmdList.add(filename);

		cmdList.addAll(getVideoFilterOptions(resource, media, params, false));

		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add("" + nThreads);
		}

		// Add the output options (-f, -c:a, -c:v, etc.)

		// Now that inputs and filtering are complete, see if we should
		// give the renderer the final say on the command
		boolean override = false;
		if (renderer instanceof OutputOverride outputOverride) {
			override = outputOverride.getOutputOptions(cmdList, resource, this, params);
		}

		if (!override) {
			// TODO: See if that last boolean can be set more carefully to disable unnecessary transcoding
			cmdList.addAll(getVideoTranscodeOptions(resource, media, params, false));

			// Add video bitrate options
			cmdList.addAll(getVideoBitrateOptions(resource, media, params, false));

			// Add audio bitrate options
			cmdList.addAll(getAudioBitrateOptions(resource, media, params));

			// Add any remaining custom options
			if (!customOptions.isEmpty()) {
				customOptions.transferAll(cmdList);
			}
		}

		// Set up the process

		// basename of the named pipe:
		String fifoName = String.format(
			"ffmpegwebvideo_%d_%d",
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
		parseMediaInfo(filename, resource, pw); // Better late than never
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

	@Override
	public String getName() {
		return NAME;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isCompatible(StoreItem item) {
		if (PlayerUtil.isWebVideo(item)) {
			String url = item.getFileName();

			ExecutableInfo executableInfo = programInfo.getExecutableInfo(currentExecutableType);
			if (executableInfo instanceof FFmpegExecutableInfo) {
				List<String> protocols = FFmpegOptions.getSupportedProtocols(executableInfo.getPath());
				if (!protocols.contains(url.split(":")[0])) {
					return false;
				}
			} else {
				LOGGER.warn(
					"Couldn't check {} protocol compatibility for \"{}\", reporting as not compatible",
					getClass().getSimpleName(),
					url.split(":")[0]
				);
				return false;
			}

			// FFmpeg does not natively support YouTube videos
			if (isYouTubeURL(url)) {
				return false;
			}

			return !FFmpegWebFilters.isExluded(url);
		}

		return false;
	}

	static final Matcher END_OF_HEADER = Pattern.compile("Press \\[q\\]|A-V:|At least|Invalid").matcher("");

	/**
	 * Parse media info from ffmpeg headers during playback
	 */
	public static void parseMediaInfo(String filename, final StoreItem item, final ProcessWrapperImpl pw) {
		if (item.getMediaInfo() == null) {
			item.setMediaInfo(new MediaInfo());
		} else if (item.getMediaInfo().isMediaParsed()) {
			return;
		}
		OutputTextLogger ffParser = new OutputTextLogger(null) {
			final ArrayList<String> lines = new ArrayList<>();
			final String input = filename.length() > 200 ? filename.substring(0, 199) : filename;

			@Override
			public boolean filter(String line) {
				if (END_OF_HEADER.reset(line).find()) {
					FFmpegParser.parseFFmpegInfo(item.getMediaInfo(), lines, input);
					LOGGER.trace("[{}] parsed media from headers: {}", ID, item.getMediaInfo());
					item.getParent().updateChild(item);
					return false; // done, stop filtering
				}
				lines.add(line);
				return true; // keep filtering
			}
		};
		ffParser.setFiltered(true);
		pw.setStderrConsumer(ffParser);
	}

	public static boolean isYouTubeURL(String youTubeUrl) {
		String pattern = "(?<=youtu.be/|watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";
		Pattern compiledPattern = Pattern.compile(pattern);
		Matcher matcher = compiledPattern.matcher(youTubeUrl);
		return matcher.find();
	}
}
