/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.pms.PMS;
import net.pms.platform.PlatformUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// see https://code.google.com/p/ps3mediaserver/issues/detail?id=680
// for background/issues/discussion related to this class
public class ProcessUtil {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessUtil.class);

	/**
	 * This class is not meant to be instantiated.
	 */
	private ProcessUtil() {
	}

	// work around a Java bug
	// see: http://www.cnblogs.com/abnercai/archive/2012/12/27/2836008.html
	public static int waitFor(Process p) {
		int exit = -1;

		try {
			exit = p.waitFor();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		return exit;
	}

	/**
	 * Retrieves the process ID (PID) for the specified {@link Process}.
	 *
	 * @param process the {@link Process} for whose PID to retrieve.
	 * @return The PID or zero if the PID couldn't be retrieved.
	 */
	public static long getProcessId(@Nullable Process process) {
		if (process == null) {
			return 0;
		}
		try {
			return process.pid();
		} catch (UnsupportedOperationException e) {
			return 0;
		}
	}

	/**
	 * Checks if the process is still alive using reflection if possible.
	 *
	 * @param process the {@link Process} to check.
	 * @return {@code true} if the process is still alive, {@code false}
	 *         otherwise.
	 */
	public static boolean isProcessIsAlive(@Nullable Process process) {
		if (process == null) {
			return false;
		}
		return process.isAlive();
	}

	// destroy a process safely (kill -TERM on Unix)
	public static void destroy(final Process p) {
		if (p != null) {
			PlatformUtils.INSTANCE.destroyProcess(p);
		}
	}

	/**
	 * Converts the path of the specified {@link File} to the equivalent MS-DOS
	 * style 8.3 path using the Windows API function {@code GetShortPathNameW()}
	 * if the path contains Unicode characters.
	 *
	 * @param file the {@link File} whose path to convert.
	 * @return The resulting non-Unicode file path.
	 */
	public static String getShortFileNameIfWideChars(File file) {
		if (file == null) {
			return null;
		}
		return getShortFileNameIfWideChars(file.getPath());
	}

	/**
	 * Converts the specified file path to the equivalent MS-DOS style 8.3 path
	 * using the Windows API function {@code GetShortPathNameW()} if the path
	 * contains Unicode characters.
	 *
	 * @param name the file path to convert.
	 * @return The resulting non-Unicode file path.
	 */
	public static String getShortFileNameIfWideChars(String name) {
		return PlatformUtils.INSTANCE.getShortPathNameW(name);
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
		} catch (IOException e) {
			LOGGER.error("Error running command " + Arrays.toString(cmd), e);
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
		}
		return "";
	}

	public static String run(String... cmd) {
		int[] zeroExpected = {0};
		return run(zeroExpected, cmd);
	}

	// Whitewash any arguments not suitable to display in dbg messages
	// and make one single printable string
	public static String dbgWashCmds(String[] cmd) {
		StringBuilder sb = new StringBuilder();
		boolean prevHeader = false;
		for (String argument : cmd) {
			if (StringUtils.isNotBlank(argument)) {
				if (sb.length() > 0) {
					sb.append(" ");
				}

				// Hide sensitive information from the log
				String modifiedArgument;
				if (prevHeader) {
					modifiedArgument = argument.replaceAll("Authorization: [^\n]+\n", "Authorization: ****\n");
					prevHeader = false;
				} else {
					if (argument.contains("headers")) {
						prevHeader = true;
					}
					modifiedArgument = argument;
				}

				// Wrap arguments with spaces in double quotes to make them
				// runnable if copy-pasted
				if (modifiedArgument.contains(" ")) {
					sb.append("\"").append(modifiedArgument).append("\"");
				} else {
					sb.append(modifiedArgument);
				}
			}
		}
		return sb.toString();
	}

	// Rebooting

	public static void reboot() {
		reboot((ArrayList<String>) null, null, null);
	}

	public static void reboot(String... umsoptions) {
		reboot(null, null, null, umsoptions);
	}

	// Shutdown UMS and either reboot or run the given command (e.g. a script to
	// restart UMS)
	public static void reboot(List<String> cmd, Map<String, String> env, String startdir, String... umsOptions) {
		final boolean hasOption = umsOptions.length > 0;
		final List<String> reboot = PlatformUtils.INSTANCE.getRestartCommand(hasOption);

		if (hasOption) {
			reboot.addAll(Arrays.asList(umsOptions));
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

		final ProcessBuilder pb = new ProcessBuilder(cmd);
		if (env != null) {
			pb.environment().putAll(env);
		}
		pb.directory(new File(startdir));
		try {
			LOGGER.info("Starting: " + StringUtils.join(cmd, " "));
			LOGGER.info("In folder: " + pb.directory());
			PMS.shutdown();
			pb.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		PMS.quit();
	}

	/**
	 * Shuts down the computer.
	 * This is initiated via the Server Settings folder.
	 */
	public static void shutDownComputer() {
		String shutdownCommand = PlatformUtils.INSTANCE.getShutdownCommand();

		if (shutdownCommand != null) {
			try {
				Runtime.getRuntime().exec(shutdownCommand);
				System.exit(0);
			} catch (IOException e) {
				LOGGER.error("Error while shutting down computer: {}", e.getMessage(), e);
			}
		}
	}

}
