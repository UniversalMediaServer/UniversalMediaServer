package net.pms.encoders;

import com.sun.jna.Platform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FFmpegScreencastVideo extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFmpegScreencastVideo.class);

	public FFmpegScreencastVideo() {
	}

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		List<String> cmdList = new ArrayList<>();

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

		cmdList.add(executable());

		// Prevent FFmpeg timeout
		cmdList.add("-y");

		cmdList.add("-loglevel");
		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		cmdList.add("-video_size");
		cmdList.add("1920x1080");

		cmdList.add("-framerate");
		cmdList.add("60000/1001");

		cmdList.add("-f");
		if (Platform.isWindows()) {
			cmdList.add("dshow");
		} else {
			cmdList.add("x11grab");
		}

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add(String.valueOf(nThreads));
		}

		cmdList.add("-i");
		if (Platform.isWindows()) {
			cmdList.add("video=\"UScreenCapture\":audio=\"Microphone\"");
		} else {
			cmdList.add(":0.0+100,200");
		}

		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add(String.valueOf(nThreads));
		}

		cmdList.add("-c:v");
		cmdList.add("mpeg2video");

		cmdList.add("-c:a");
		cmdList.add("ac3");

		if (!Platform.isWindows()) {
			cmdList.add("-f");
			cmdList.add("alsa");

			cmdList.add("-i");
			cmdList.add("pulse");
		}

		PipeProcess pipe = new PipeProcess("ffmpegscreencastvideo" + System.currentTimeMillis(), params);
		params.input_pipes[0] = pipe;
		cmdList.add(pipe.getInputPipe());

		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);

		ProcessWrapper mkfifo_process = pipe.getPipeProcess();
		pw.attachProcess(mkfifo_process);
		mkfifo_process.runInNewThread();
		try {
			Thread.sleep(50);
		} catch (InterruptedException e) {
		}
		pipe.deleteLater();

		pw.runInNewThread();
		return pw;
	}
}
