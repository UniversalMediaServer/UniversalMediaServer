/*
 * PS3 Media Server, for streaming any medias to your PS3.
 * Copyright (C) 2008  A.Brochard
 *
 * This program is free software; you can redistribute it and/or
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
package net.pms.io;

import com.sun.jna.Platform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.pms.PMS;
import net.pms.encoders.AviDemuxerInputStream;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessWrapperImpl extends Thread implements ProcessWrapper {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessWrapperImpl.class);

	/** FONTCONFIG_PATH environment variable name */
	private static final String FONTCONFIG_PATH = "FONTCONFIG_PATH";

	private Process process;
	private OutputConsumer stdoutConsumer;
	private OutputConsumer stderrConsumer;
	private OutputParams params;
	private boolean destroyed;
	private String[] cmdArray;
	private boolean nullable;
	private ArrayList<ProcessWrapper> attachedProcesses;
	private BufferedOutputFile bo = null;
	private boolean keepStdout;
	private boolean keepStderr;
	private static int processCounter = 0;
	private boolean success;

	@Override
	public String toString() {
		return super.getName();
	}

	public boolean isSuccess() {
		return success;
	}

	public ProcessWrapperImpl(String cmdArray[], OutputParams params) {
		this(cmdArray, params, false, false);
	}

	public ProcessWrapperImpl(String cmdArray[], OutputParams params, boolean keepOutput) {
		this(cmdArray, params, keepOutput, keepOutput);
	}

	public ProcessWrapperImpl(String cmdArray[], OutputParams params, boolean keepStdout, boolean keepStderr) {
		super();

		// Determine a suitable thread name for this process:
		// use the command name, but remove its path first.
		String threadName = cmdArray[0];

		if (threadName.indexOf('/') >= 0) {
			threadName = threadName.substring(threadName.lastIndexOf('/') + 1);
		}

		if (threadName.indexOf('\\') >= 0) {
			threadName = threadName.substring(threadName.lastIndexOf('\\') + 1);
		}

		setName(threadName + "-" + getProcessCounter());

		File exec = new File(cmdArray[0]);

		if (exec.isFile()) {
			cmdArray[0] = exec.getAbsolutePath();
		}

		this.cmdArray = cmdArray;
		this.params = params;
		this.keepStdout = keepStdout;
		this.keepStderr = keepStderr;
		attachedProcesses = new ArrayList<>();
	}

	private synchronized int getProcessCounter() {
		return processCounter++;
	}

	public void attachProcess(ProcessWrapper process) {
		attachedProcesses.add(process);
	}

	@Override
	public void run() {
		ProcessBuilder pb = new ProcessBuilder(cmdArray);
		try {
			LOGGER.debug("Starting " + ProcessUtil.dbgWashCmds(cmdArray));

			if (params.workDir != null && params.workDir.isDirectory()) {
				pb.directory(params.workDir);
			}

			// Retrieve all environment variables of the process
			Map<String,String> environment = pb.environment();

			// The variable params.env is initialized to null in the OutputParams
			// constructor and never set to another value in PMS code. Plugins
			// might use it?
			if (params.env != null && !params.env.isEmpty()) {
				// Actual name of system path var is case-sensitive

				String sysPathKey = Platform.isWindows() ? "Path" : "PATH";
				// As is Map
				String PATH = params.env.containsKey("PATH") ? params.env.get("PATH") :
					params.env.containsKey("path") ? params.env.get("path") :
					params.env.containsKey("Path") ? params.env.get("Path") : null;
				if (PATH != null) {
					PATH += (File.pathSeparator + environment.get(sysPathKey));
				}
				environment.putAll(params.env);
				if (PATH != null) {
					environment.put(sysPathKey, PATH);
				}
			}

			// Fontconfig on Mac OS X may have problems locating fonts. As a result
			// subtitles may be rendered invisible. Force feed fontconfig the
			// FONTCONFIG_PATH environment variable to the prepackaged fontconfig
			// configuration directory that comes with UMS on Mac OS X to make
			// sure it has sensible defaults.
			if (Platform.isMac()) {
				// Do not overwrite the variable if it already exists.
				if (!environment.containsKey(FONTCONFIG_PATH)) {
					String pmsWorkingDirectory = new File("").getAbsolutePath();
					String fontconfigFontsPath = pmsWorkingDirectory + "/fonts";
					LOGGER.trace("Setting FONTCONFIG_PATH to \"" + fontconfigFontsPath + "\"");
					environment.put(FONTCONFIG_PATH, fontconfigFontsPath);
				}
			}

			// XXX A cleaner way to execute short-running commands (e.g. vlc -version)
			// is being developed. When that's done, this class can be used solely
			// for the long-running tasks i.e. transcodes. At that point, we won't need
			// separate stdout and stderr and can merge them by uncommenting the
			// following line:
			// pb.redirectErrorStream(true);
			process = pb.start();
			PMS.get().currentProcesses.add(process);

			if (stderrConsumer == null) {
				stderrConsumer = keepStderr
					? new OutputTextConsumer(process.getErrorStream(), true)
					: new OutputTextLogger(process.getErrorStream());
			} else {
				stderrConsumer.setInputStream(process.getErrorStream());
			}
			stderrConsumer.setName(getName() + "-2");
			stderrConsumer.start();
			stdoutConsumer = null;

			if (params.input_pipes[0] != null) {
				LOGGER.debug("Reading pipe: " + params.input_pipes[0].getInputPipe());
				bo = params.input_pipes[0].getDirectBuffer();
				if (bo == null || params.losslessaudio || params.lossyaudio || params.no_videoencode) {
					InputStream is = params.input_pipes[0].getInputStream();

					if (params.avidemux) {
						is = new AviDemuxerInputStream(is, params, attachedProcesses);
					}

					stdoutConsumer = new OutputBufferConsumer(is, params);
					bo = stdoutConsumer.getBuffer();
				}
				bo.attachThread(this);
				new OutputTextLogger(process.getInputStream()).start();
			} else if (params.log) {
				stdoutConsumer = keepStdout
					? new OutputTextConsumer(process.getInputStream(), true)
					: new OutputTextLogger(process.getInputStream());
			} else {
				stdoutConsumer = new OutputBufferConsumer(process.getInputStream(), params);
				bo = stdoutConsumer.getBuffer();
				bo.attachThread(this);
			}

			if (stdoutConsumer != null) {
				stdoutConsumer.setName(getName() + "-1");
				stdoutConsumer.start();
			}

			if (params.stdin != null) {
				params.stdin.push(process.getOutputStream());
			}

			Integer pid = ProcessUtil.getProcessID(process);

			if (pid != null) {
				LOGGER.debug("Unix process ID ({}): {}", cmdArray[0], pid);
			}

			ProcessUtil.waitFor(process);

			// Wait up to a second for the stderr consumer thread to finish
			try {
				if (stderrConsumer != null) {
					stderrConsumer.join(1000);
				}
			} catch (InterruptedException e) {
			}

			// wait up to a second for the stdout consumer thread to finish
			try {
				if (stdoutConsumer != null) {
					stdoutConsumer.join(1000);
				}
			} catch (InterruptedException e) { }
		} catch (IOException e) {
			LOGGER.error("Error initializing process: ", e.getMessage());
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("", e);
			}
			stopProcess();
		} finally {
			try {
				if (bo != null) {
					bo.close();
				}
			} catch (IOException ioe) {
				LOGGER.debug("Error closing buffered output file", ioe.getMessage());
			}

			if (!destroyed && !params.noexitcheck) {
				try {
					success = true;
					if (process != null && process.exitValue() != 0) {
						LOGGER.info("Process {} has a return code of {}! Maybe an error occurred... check the log file", cmdArray[0], process.exitValue());
						success = false;
					}
				} catch (IllegalThreadStateException itse) {
					LOGGER.error("Error reading process exit value", itse);
				}
			}
			if (attachedProcesses != null) {
				for (ProcessWrapper pw : attachedProcesses) {
					if (pw != null) {
						pw.stopProcess();
					}
				}
			}
			PMS.get().currentProcesses.remove(process);
		}
	}

	/**
	 * Same as {@link #start()}, merely making the intention explicit in the
	 * method name.
	 * @see #runInSameThread()
	 */
	@Override
	public void runInNewThread() {
		this.start();
	}

	/**
	 * Same as {@link #run()}, merely making the intention explicit in the
	 * method name.
	 * @see #runInNewThread()
	 */
	@Override
	public void runInSameThread() {
		this.run();
	}

	@Override
	public InputStream getInputStream(long seek) throws IOException {
		if (bo != null) {
			return bo.getInputStream(seek);
		} else if (stdoutConsumer != null && stdoutConsumer.getBuffer() != null) {
			return stdoutConsumer.getBuffer().getInputStream(seek);
		}
		return null;
	}

	public List<String> getOtherResults() {
		if (stdoutConsumer == null) {
			return null;
		}
		try {
			stdoutConsumer.join(1000);
		} catch (InterruptedException e) {
		}
		return stdoutConsumer.getResults();
	}

	@Override
	public List<String> getResults() {
		try {
			stderrConsumer.join(1000);
		} catch (InterruptedException e) {
		}
		return stderrConsumer.getResults();
	}

	@Override
	public synchronized void stopProcess() {
		if (!destroyed) {
			destroyed = true;
			if (process != null) {
				Integer pid = ProcessUtil.getProcessID(process);
				if (pid != null) {
					LOGGER.debug("Stopping Unix process " + pid + ": " + this);
				} else {
					LOGGER.debug("Stopping process: " + this);
				}
				ProcessUtil.destroy(process);
			}

			if (attachedProcesses != null) {
				for (ProcessWrapper pw : attachedProcesses) {
					if (pw != null) {
						pw.stopProcess();
					}
				}
			}
			if (stdoutConsumer != null && stdoutConsumer.getBuffer() != null) {
				stdoutConsumer.getBuffer().reset();
			}
		}
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	@Override
	public boolean isReadyToStop() {
		return nullable;
	}

	@Override
	public void setReadyToStop(boolean nullable) {
		if (nullable != this.nullable) {
			LOGGER.trace("Ready to Stop: " + nullable);
		}
		this.nullable = nullable;
	}

	// TODO: implement setStdoutConsumer() ?

	public void setStderrConsumer(OutputConsumer consumer) {
		this.stderrConsumer = consumer;
	}
}
