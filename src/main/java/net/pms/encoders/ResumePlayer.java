package net.pms.encoders;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.dlna.DLNAMediaInfo;
import net.pms.dlna.DLNAResource;
import net.pms.io.OutputParams;
import net.pms.io.PipeProcess;
import net.pms.io.ProcessWrapper;
import net.pms.io.ProcessWrapperImpl;
import org.codehaus.plexus.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResumePlayer extends FFMpegVideo {
	private static final Logger LOGGER = LoggerFactory.getLogger(FFMpegVideo.class);

	public ResumePlayer() {
		super(PMS.getConfiguration());
	}

	@Override
	public ProcessWrapper launchTranscode(
		String fileName,
		DLNAResource dlna,
		DLNAMediaInfo media,
		OutputParams params) throws IOException {
		List<String> cmdList = new ArrayList<String>();
		int nThreads = PMS.getConfiguration().getNumberOfCpuCores();

		cmdList.add(executable());

		// Prevent FFmpeg timeout
		cmdList.add("-y");

		cmdList.add("-loglevel");

		if (LOGGER.isTraceEnabled()) { // Set -loglevel in accordance with LOGGER setting
			cmdList.add("info"); // Could be changed to "verbose" or "debug" if "info" level is not enough
		} else {
			cmdList.add("warning");
		}

		// decoder threads
		cmdList.add("-threads");
		cmdList.add("" + nThreads);

		if (params.timeseek > 0) {
			cmdList.add("-ss");
			cmdList.add("" + params.timeseek);
		}

		cmdList.add("-i");
		cmdList.add(fileName);

		cmdList.add("-vcodec");
		cmdList.add("copy");
		cmdList.add("-acodec");
		cmdList.add("copy");

		cmdList.add("-f");
		cmdList.add(FileUtils.getExtension(fileName));

		PipeProcess pipe = new PipeProcess("resumeplayer" + System.currentTimeMillis(), params);
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
