package net.pms.encoders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.formats.Format;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;

public class WebPlayer extends Player {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WebPlayer.class);
	
	public ProcessWrapper launchTranscode(
				String fileName,
				DLNAResource dlna,
				DLNAMediaInfo media,
				OutputParams params) throws IOException {
			LOGGER.debug("web player wrapper called");
			String fifoName = String.format("webplayer%d_%d", Thread.currentThread().getId(), System.currentTimeMillis());
			
			PipeProcess pipe = new PipeProcess(fifoName);

			params.input_pipes[0] = pipe;
			int nThreads = PMS.getConfiguration().getNumberOfCpuCores();

			// FFMPEG version
			String[] cmdArray = new String[] {
					PMS.getConfiguration().getFfmpegPath(),
					"-y", 
					"-loglevel", "warning",
					"-threads", "" + nThreads,
					"-i", fileName,
					"-threads",  "" + nThreads,
					"-f", "mp4",
					"-vcodec", "libx264",
					//"-acodec", "libfaac",
					"-frag_duration", "300",
					"-frag_size", "100",
					"-flags", "+aic+mv4",
					pipe.getInputPipe()
				};
			
			// MENCODER version
			
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
			
			LOGGER.debug("web cmd "+cmdArray);
			ProcessWrapperImpl pw = new ProcessWrapperImpl(cmdArray, params);
			ProcessWrapper mkfifo_process = pipe.getPipeProcess();
			pw.attachProcess(mkfifo_process);

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
			
			LOGGER.debug("return pw "+pw);
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
