package net.pms.encoders;

import java.io.IOException;
import javax.swing.JComponent;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
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
		OutputParams params) throws IOException {
		LOGGER.debug("web player wrapper called");
		String fifoName = String.format("webplayer%d_%d", Thread.currentThread().getId(), System.currentTimeMillis());

		PipeProcess pipe = new PipeProcess(fifoName);
        String fileName = dlna.getSystemName();
        int nThreads = configuration.getNumberOfCpuCores();

			// FFMPEG version
			String[] cmdArray = new String[] {
					PMS.getConfiguration().getFfmpegPath(),
					"-y", 
					"-loglevel", "warning",
					"-threads", "" + nThreads,
					"-i", fileName,
					"-f", "h264",
					"-vcodec", "libx264",
					"-preset", "baseline",
					"-preset", "slow",
					"-acodec", "copy",
//					"-frag_duration", "300",
	//				"-frag_size", "100",
		//			"-flags", "+aic+mv4",
					pipe.getInputPipe()
				};
			
			LOGGER.debug("web cmd "+cmdArray.toString());
			ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
			ProcessWrapper mkfifo_process = pipe.getPipeProcess();
			pw.attachProcess(mkfifo_process);

			try {
				Thread.sleep(300);
			} catch (InterruptedException e) {
				LOGGER.error("thread interrupted while waiting for named pipe to be created", e);
			}

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


		// create the named pipe and wait briefly to allow it to be created
		mkfifo_process.runInNewThread();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("thread interrupted while waiting for named pipe to be created", e);
		}

		pipe.deleteLater();

		// launch transcode command and wait briefly to allow it to start
		pw.runInNewThread();

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			LOGGER.error("thread interrupted while waiting for transcode to start", e);
		}

		LOGGER.debug("return pw " + pw);
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
		return null;
	}
}
