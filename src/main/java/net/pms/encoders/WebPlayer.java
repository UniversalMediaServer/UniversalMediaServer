package net.pms.encoders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

public class WebPlayer extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayer.class);
    private boolean flash;

    public WebPlayer() {
        super();
        flash = false;
    }

    public WebPlayer(boolean f) {
        this();
        flash = f;
    }


	@Override
	public ProcessWrapper launchTranscode(
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params
	) throws IOException {
		LOGGER.debug("web player wrapper called");
		params.waitbeforestart = 1000;
		final String filename = dlna.getSystemName();
		setAudioAndSubs(filename, media, params);

		String fifoName = String.format(
			"webplayer_%d_%d",
			Thread.currentThread().getId(),
			System.currentTimeMillis()
		);
		int nThreads = configuration.getNumberOfCpuCores();

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

		// XXX squashed bug - without this, ffmpeg hangs waiting for a confirmation
		// that it can write to a file that already exists i.e. the named pipe
		cmdList.add("-y");
        cmdList.add("-re");

		cmdList.add("-loglevel");

		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		// Decoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add("" + (int) params.timeseek);
		}

		cmdList.add("-i");
		cmdList.add(filename);

		//cmdList.addAll(getVideoFilterOptions(dlna, media, params));

		// Encoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

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
        if (!flash) {
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
            /*cmdList.add("-c:v");
            cmdList.add("libx264");
            cmdList.add("-ab");
            cmdList.add("56k");
            cmdList.add("-acodec");
            cmdList.add("libvo_aacenc");
            cmdList.add("-movflags");
            cmdList.add("faststart+frag_keyframe+empty_moov");
            cmdList.add("-g");
            cmdList.add("30");
            cmdList.add("-r");
            cmdList.add("25");
            cmdList.add("-f");
            cmdList.add("mp4");*/
        }
        else {
            cmdList.add("-c:v");
            cmdList.add("flv");
            cmdList.add("-ar");
            cmdList.add("44100");
            cmdList.add("-f");
            cmdList.add("flv");
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
}
