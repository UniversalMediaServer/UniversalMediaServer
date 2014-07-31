package net.pms.encoders;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.*;
import net.pms.remote.RemoteUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebPlayer extends FFMpegVideo {
	public static final int STREAM = 0;
	public static final int TRANS = 1;
	public static final int FLASH = 2;

	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayer.class);
	private int method;

	public WebPlayer() {
		super();
		method = STREAM;
	}

	public WebPlayer(int m) {
		this();
		method = m;
	}

	private void flashCmds(List<String> cmdList, DLNAMediaInfo media) {
		cmdList.add("-c:v");
		if (media.getCodecV() != null && media.getCodecV().equals("h264")) {
			cmdList.add("copy");
		} else {
			cmdList.add("flv");
			cmdList.add("-qmin");
			cmdList.add("2");
			cmdList.add("-qmax");
			cmdList.add("6");
		}
		if (media.getFirstAudioTrack() != null && media.getFirstAudioTrack().isAAC()) {
			cmdList.add("-c:a");
			cmdList.add("copy");
		} else {
			cmdList.add("-ar");
			cmdList.add("44100");
		}
		cmdList.add("-f");
		cmdList.add("flv");
	}



	private void oggCmd(List<String> cmdList) {
		cmdList.add("-c:v");
		cmdList.add("libtheora");
		cmdList.add("-qscale:v");
		cmdList.add("8");
		cmdList.add("-acodec");
		cmdList.add("libvorbis");
		cmdList.add("-qscale:a");
		cmdList.add("6");
		cmdList.add("-f");
		cmdList.add("ogg");
	}

	private void mp4Cmd(List<String> cmdList) {
		cmdList.add("-c:v");
		cmdList.add("libx264");
		cmdList.add("-preset");
		cmdList.add("ultrafast");
		cmdList.add("-tune");
		cmdList.add("zerolatency");
		cmdList.add("-profile:v");
		cmdList.add("high");
		cmdList.add("-level:v");
		cmdList.add("3.1");
		cmdList.add("-c:a");
		cmdList.add("aac");
		cmdList.add("-ab");
		cmdList.add("16k");
		cmdList.add("-ar");
		cmdList.add("44100");
		cmdList.add("-strict");
		cmdList.add("experimental");
		cmdList.add("-pix_fmt");
		cmdList.add("yuv420p");
		cmdList.add("-frag_duration");
		cmdList.add("300");
		cmdList.add("-frag_size");
		cmdList.add("100");
		cmdList.add("-flags");
		cmdList.add("+aic+mv4");
		cmdList.add("-movflags");
		cmdList.add("+faststart");
		cmdList.add("-f");
		cmdList.add("mp4");
		//cmdList.add("separate_moof+frag_keyframe+empty_moov");
	}

	private void hlsCmd(List<String> cmdList, DLNAMediaInfo media) {
		cmdList.add("-c:v");
		if (media.getCodecV() != null && media.getCodecV().equals("h264")) {
			cmdList.add("copy");
		} else {
			cmdList.add("flv");
			cmdList.add("-qmin");
			cmdList.add("2");
			cmdList.add("-qmax");
			cmdList.add("6");
		}
		if (media.getFirstAudioTrack() != null && media.getFirstAudioTrack().isAAC()) {
			cmdList.add("-c:a");
			cmdList.add("copy");
		} else {
			cmdList.add("-ar");
			cmdList.add("44100");
		}
		cmdList.add("-f");
		cmdList.add("HLS");
	}

	@Override
	public synchronized ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		LOGGER.debug("web player wrapper called");
		params.waitbeforestart = 4000;
		final String filename = dlna.getSystemName();
		setAudioAndSubs(filename, media, params);

		String fifoName = String.format(
			"webplayer_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);

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

		// This process wraps the command that creates the named pipe
		PipeProcess pipe = new PipeProcess(fifoName);
		pipe.deleteLater(); // delete the named pipe later; harmless if it isn't created
		ProcessWrapper mkfifo_process = pipe.getPipeProcess();

		/**
		 * It can take a long time for Windows to create a named pipe (and
		 * mkfifo can be slow if /tmp isn't memory-mapped), so run this in
		 * the current thread.
		 */
		mkfifo_process.runInSameThread();

		params.input_pipes[0] = pipe;

		List<String> cmdList = new ArrayList<>();
		cmdList.add(executable());

		cmdList.add("-y");
		cmdList.add("-re");

		cmdList.add("-loglevel");

		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		// Decoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add(String.valueOf(nThreads));
		}

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.timeseek);
		}

		cmdList.add("-i");
		cmdList.add(filename);

		cmdList.addAll(getVideoFilterOptions(dlna, media, params));

		// Encoder threads
		if (nThreads > 0) {
			cmdList.add("-threads");
			cmdList.add(String.valueOf(nThreads));
		}

		// FFmpeg with Theora and Vorbis inside OGG
		/*String[] cmdArray = new String[]{
		PMS.getConfiguration().getFfmpegPath(),
		"-y",
		"-loglevel", "warning",
		"-threads", "" + nThreads,
		"-i", filename,
		"-threads", "" + nThreads,
		"-c:v", "libtheora",
		"-qscale:v", "8",
		"-acodec", "libvorbis",
		"-qscale:a", "6",
		"-f", "ogg",
		pipe.getInputPipe()
		};*/
		// Add the output options (-f, -c:a, -c:v, etc.)
		if (method == TRANS) {
			if(RemoteUtil.MIME_TRANS.equals(RemoteUtil.MIME_OGG))  {
				oggCmd(cmdList);
			}
			else if (RemoteUtil.MIME_TRANS.equals(RemoteUtil.MIME_MP4)) {
				mp4Cmd(cmdList);
			}
			else if (RemoteUtil.MIME_TRANS.equals(RemoteUtil.MIME_WEBM)) {
				// nothing here   yet
			}
		} else if (method == FLASH) {
			flashCmds(cmdList, media);
		}

		// Output file
		cmdList.add(pipe.getInputPipe());

		// Convert the command list to an array
		String[] cmdArray = new String[cmdList.size()];
		cmdList.toArray(cmdArray);

		// Hook to allow plugins to customize this command line
		cmdArray = finalizeTranscoderArgs(
			filename,
			dlna,
			media,
			params,
			cmdArray
		);

		// Now launch FFmpeg
		ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
		pw.attachProcess(mkfifo_process); // Clean up the mkfifo process when the transcode ends

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

		return pw;
	}

	@Override
	public String[] args() {
		return null;
	}

	@Override
	public JComponent config() {
		return null;
	}

	@Override
	public String id() {
		return "WebPlayer";
	}

	@Override
	public boolean isCompatible(DLNAResource resource) {
		return true;
	}

	@Override
	public String mimeType() {
		return "mime/mp4";
	}

	@Override
	public String name() {
		return "WebPlayer";
	}

	@Override
	public int purpose() {
		return VIDEO_WEBSTREAM_PLAYER;
	}

	@Override
	public int type() {
		return Format.VIDEO;
	}

	@Override
	public String executable() {
		return super.executable();
	}

	private class WebProcessWrapper extends ProcessWrapperLiteImpl {
		private DLNAResource res;

		public WebProcessWrapper(DLNAResource res) {
			super(null);
			this.res = res;
		}

		@Override
		public InputStream getInputStream(long seek) throws IOException {
			InputStream fis = res.getInputStream();
			return fis;
		}
	}
}
