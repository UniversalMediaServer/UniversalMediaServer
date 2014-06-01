package net.pms.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import net.pms.PMS;
import net.pms.io.Gob;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see https://code.google.com/p/ps3mediaserver/issues/detail?id=680
// for background/issues/discussion related to this class
public class ProcessUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtil.class);

	// how long to wait in milliseconds until a kill -TERM on Unix has been deemed to fail
	private static final int TERM_TIMEOUT = 10000;

	// how long to wait in milliseconds until a kill -ALRM on Unix has been deemed to fail
	private static final int ALRM_TIMEOUT = 2000;

	// work around a Java bug
	// see: http://kylecartmell.com/?p=9
	public static int waitFor(Process p) {
		int exit = -1;

		try {
			exit = p.waitFor();
		} catch (InterruptedException e) {
			Thread.interrupted();
		}

		return exit;
	}

	// get the process ID on Unix (returns null otherwise)
	public static Integer getProcessID(Process p) {
		Integer pid = null;

		if (p != null && p.getClass().getName().equals("java.lang.UNIXProcess")) {
			try {
				Field f = p.getClass().getDeclaredField("pid");
				f.setAccessible(true);
				pid = f.getInt(p);
			} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
				LOGGER.debug("Can't determine the Unix process ID: " + e.getMessage());
			}
		}

		return pid;
	}

	// kill -9 a Unix process
	public static void kill(Integer pid) {
		kill(pid, 9);
	}

	/*
	 * FIXME: this is a hack - destroy() *should* work
	 *
	 * call chain (innermost last):
	 *
	 *     WaitBufferedInputStream.close
	 *     BufferedOutputFile.detachInputStream
	 *     ProcessWrapperImpl.stopProcess
	 *     ProcessUtil.destroy
	 *     ProcessUtil.kill
	 *
	 * my best guess is that the process's stdout/stderr streams
	 * aren't being/haven't been fully/promptly consumed.
	 * From the abovelinked article:
	 *
	 *     The Java 6 API clearly states that failure to promptly
	 *     “read the output stream of the subprocess may cause the subprocess
	 *     to block, and even deadlock.
	 *
	 * This is corroborated by the fact that destroy() works fine if the
	 * process is allowed to run to completion:
	 *
	 *     https://code.google.com/p/ps3mediaserver/issues/detail?id=680#c11
	 */
	// send a Unix process the specified signal
	public static boolean kill(Integer pid, int signal) {
		boolean killed = false;
		LOGGER.warn("Sending kill -" + signal + " to the Unix process: " + pid);
		try {
			Process process = Runtime.getRuntime().exec("kill -" + signal + " " + pid);
			// "Gob": a cryptic name for (e.g.) StreamGobbler - i.e. a stream
			// consumer that reads and discards the stream
			new Gob(process.getErrorStream()).start();
			new Gob(process.getInputStream()).start();
			int exit = waitFor(process);
			if (exit == 0) {
				killed = true;
				LOGGER.debug("Successfully sent kill -" + signal + " to the Unix process: " + pid);
			}
		} catch (IOException e) {
			LOGGER.error("Error calling: kill -" + signal + " " + pid, e);
		}

		return killed;
	}

	// destroy a process safely (kill -TERM on Unix)
	public static void destroy(final Process p) {
		if (p != null) {
			final Integer pid = getProcessID(p);

			if (pid != null) { // Unix only
				LOGGER.trace("Killing the Unix process: " + pid);
				Runnable r = new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(TERM_TIMEOUT);
						} catch (InterruptedException e) {
						}

						try {
							p.exitValue();
						} catch (IllegalThreadStateException itse) { // still running: nuke it
							// kill -14 (ALRM) works (for MEncoder) and is less dangerous than kill -9
							// so try that first 
							if (!kill(pid, 14)) {
								try {
									// This is a last resort, so let's not be too eager
									Thread.sleep(ALRM_TIMEOUT);
								} catch (InterruptedException ie) {
								}

								kill(pid, 9);
							}
						}
					}
				};

				Thread failsafe = new Thread(r, "Process Destroyer");
				failsafe.start();
			}

			p.destroy();
		}
	}

	public static String getShortFileNameIfWideChars(String name) {
		return PMS.get().getRegistry().getShortPathNameW(name);
	}

	// Run cmd and return combined stdout/stderr
	public static String run(String... cmd) {
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process p = pb.start();
			StringBuilder output;
			try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				output = new StringBuilder();
				while ((line = br.readLine()) != null) {
					output.append(line).append("\n");
				}
			}
			p.waitFor();
			if (p.exitValue() != 0) {
				LOGGER.debug("Warning: command {} returned {}", Arrays.toString(cmd), p.exitValue());
			}
			return output.toString();
		} catch (Exception e) {
			LOGGER.error("Error running command " + Arrays.toString(cmd), e);
		}
		return "";
	}

	// Whitewash any arguments not suitable to display in dbg messages
	// and make one single printable string
	public static String dbgWashCmds(String[] cmd) {
		for(int i=0; i < cmd.length; i++) {
			if(cmd[i].contains("headers")) {
				cmd[i+1]= cmd[i+1].replaceAll("Authorization: [^\n]+\n", "Authorization: ****\n");
				i++;
				continue;
			}
		}
		return StringUtils.join(cmd, " ");
	}
}
