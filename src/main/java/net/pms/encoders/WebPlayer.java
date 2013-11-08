package net.pms.encoders;

import java.io.IOException;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import static net.pms.encoders.Player.VIDEO_WEBSTREAM_PLAYER;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebPlayer extends Player {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayer.class);

	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		LOGGER.debug("web player wrapper called");
		params.waitbeforestart = 2000;

		String fifoName = String.format(
			"ffmpegwebvideo_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);
		int nThreads = configuration.getNumberOfCpuCores();
		String fileName = dlna.getSystemName();

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

		// FFmpeg with Theora and Vorbis inside OGG
		String[] cmdArray = new String[]{
			PMS.getConfiguration().getFfmpegPath(),
			"-y",
			"-loglevel", "warning",
			"-threads", "" + nThreads,
			"-i", fileName,
			"-threads", "" + nThreads,
			"-c:v", "libtheora",
			"-qscale:v", "8",
			"-acodec", "libvorbis",
			"-qscale:a", "6",
			"-f", "ogg",
			pipe.getInputPipe()
		};

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

		/*		String[] cmdArray = new String[] {
		 PMS.getConfiguration().getMencoderPath(),
		 fileName,
		 "-quiet",
		 "-oac", "copy",
		 "-ovc", "x264",
		 "-lavcopts", "vcodec=mpeg4:threads="+nThreads,
		 "-of", "mp4",
		 "-o", pipe.getInputPipe()
		 };*/
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
		return null;
	}
}
