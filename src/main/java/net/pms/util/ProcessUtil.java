/*
 * Universal Media Server, for streaming any medias to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
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

package net.pms.util;

import com.sun.jna.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import net.pms.PMS;
import net.pms.io.StreamGobbler;
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
	// see: http://www.cnblogs.com/abnercai/archive/2012/12/27/2836008.html
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
			ProcessBuilder processBuilder = new ProcessBuilder("kill", "-" + signal, Integer.toString(pid));
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			// consume the error and output process streams
			StreamGobbler.consume(process.getInputStream(), true);
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
	public static String run(int[] expectedExitCodes, String... cmd) {
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
			boolean expected = false;
			if (expectedExitCodes != null) {
				for (int expectedCode : expectedExitCodes) {
					if (expectedCode == p.exitValue()) {
						expected = true;
						break;
					}
				}
			}
			if (!expected) {
				LOGGER.debug("Warning: command {} returned {}", Arrays.toString(cmd), p.exitValue());
			}
			return output.toString();
		} catch (Exception e) {
			LOGGER.error("Error running command " + Arrays.toString(cmd), e);
		}
		return "";
	}

	public static String run(String... cmd) {
		int[] zeroExpected = { 0 };
		return run(zeroExpected, cmd);
	}

	// Whitewash any arguments not suitable to display in dbg messages
	// and make one single printable string
	public static String dbgWashCmds(String[] cmd) {
		for (int i=0; i < cmd.length; i++) {
			// Wrap arguments with spaces in double quotes to make them runnable if copy-pasted
			if (cmd[i].contains(" ")) {
				cmd[i] = "\"" + cmd[i] + "\"";
			}
			// Hide sensitive information from the log
			if (cmd[i].contains("headers")) {
				cmd[i+1]= cmd[i+1].replaceAll("Authorization: [^\n]+\n", "Authorization: ****\n");
				i++;
				continue;
			}
		}
		return StringUtils.join(cmd, " ");
	}

	// Rebooting

	// Reboot UMS same as now
	public static void reboot() {
		reboot((ArrayList<String>)null, null, null);
	}

	// Reboot UMS same as now, adding these options
	public static void reboot(String... UMSOptions) {
		reboot(null, null, null, UMSOptions);
	}

	// Shutdown UMS and either reboot or run the given command (e.g. a script to restart UMS)
	public static void reboot(ArrayList<String> cmd, Map<String,String> env, String startdir, String... UMSOptions) {
		final ArrayList<String> reboot = getUMSCommand();
		if (UMSOptions.length > 0) {
			reboot.addAll(Arrays.asList(UMSOptions));
		}
		if (cmd == null) {
			// We're doing a straight reboot
			cmd = reboot;
		} else {
			// We're running a script that will eventually restart UMS
			if (env == null) {
				env = new HashMap<>();
			}
			// Tell the script how to restart UMS
			env.put("RESTART_CMD", StringUtils.join(reboot, " "));
			env.put("RESTART_DIR", System.getProperty("user.dir"));
		}
		if (startdir == null) {
			startdir = System.getProperty("user.dir");
		}

		System.out.println("starting: " + StringUtils.join(cmd, " "));

		final ProcessBuilder pb = new ProcessBuilder(cmd);
		if (env != null) {
			pb.environment().putAll(env);
		}
		pb.directory(new File(startdir));
		System.out.println("in directory: " + pb.directory());
		try {
			pb.start();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		System.exit(0);
	}

	// Reconstruct the command that started this jvm, including all options.
	// See http://stackoverflow.com/questions/4159802/how-can-i-restart-a-java-application
	//     http://stackoverflow.com/questions/1518213/read-java-jvm-startup-parameters-eg-xmx
	public static ArrayList<String> getUMSCommand() {
		ArrayList<String> reboot = new ArrayList<>();
		reboot.add(StringUtil.quoteArg(
			System.getProperty("java.home") + File.separator + "bin" + File.separator +
			((Platform.isWindows() && System.console() == null) ? "javaw" : "java")));
		for (String jvmArg : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
			reboot.add(StringUtil.quoteArg(jvmArg));
		}
		reboot.add("-cp");
		reboot.add(ManagementFactory.getRuntimeMXBean().getClassPath());
		// Could also use generic main discovery instead:
		// see http://stackoverflow.com/questions/41894/0-program-name-in-java-discover-main-class
		reboot.add(PMS.class.getName());
		return reboot;
	}
}
