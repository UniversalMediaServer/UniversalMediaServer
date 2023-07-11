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
package net.pms.platform.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.pms.io.StreamGobbler;
import net.pms.platform.PlatformUtils;
import net.pms.util.FileUtil;
import net.pms.util.ProcessUtil;
import net.pms.util.UMSUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux specific platform code.
 * Only to be instantiated by {@link PlatformUtils#createInstance()}.
 */
public class LinuxUtils extends PlatformUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(LinuxUtils.class);

	// how long to wait in milliseconds until a kill -TERM on Unix has been
	// deemed to fail
	private static final int TERM_TIMEOUT = 10000;

	// how long to wait in milliseconds until a kill -ALRM on Unix has been
	// deemed to fail
	private static final int ALRM_TIMEOUT = 2000;

	@Override
	public void moveToTrash(File file) throws IOException {
		try {
			FreedesktopTrash.moveToTrash(file);
		} catch (FileUtil.InvalidFileSystemException e) {
			throw new IOException(e);
		}
	}

	@Override
	public boolean isAdmin() {
		synchronized (IS_ADMIN_LOCK) {
			if (isAdmin != null) {
				return isAdmin;
			}
			try {
				final String command = "id -Gn";
				LOGGER.trace("isAdmin: Executing \"{}\"", command);
				Process p = Runtime.getRuntime().exec(command);
				InputStream is = p.getInputStream();
				InputStreamReader isr = new InputStreamReader(is, StandardCharsets.US_ASCII);
				int exitValue;
				String exitLine;
				try (BufferedReader br = new BufferedReader(isr)) {
					p.waitFor();
					exitValue = p.exitValue();
					exitLine = br.readLine();
				}

				if (exitValue != 0 || exitLine == null || exitLine.isEmpty()) {
					LOGGER.error("Could not determine root privileges, \"{}\" ended with exit code: {}", command, exitValue);
					isAdmin = false;
					return false;
				}

				LOGGER.trace("isAdmin: \"{}\" returned {}", command, exitLine);
				if (exitLine.matches(".*\\broot\\b.*")) {
					LOGGER.trace("isAdmin: UMS has root privileges");
					isAdmin = true;
					return true;
				}

				LOGGER.trace("isAdmin: UMS does not have root privileges");
				isAdmin = false;
				return false;
			} catch (IOException e) {
				LOGGER.error(
					"An error prevented UMS from checking Linux permissions: {}",
					e.getMessage()
				);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			isAdmin = false;
			return false;
		}
	}

	@Override
	public String getDefaultFontPath() {
		// get Linux default font
		String font = getAbsolutePath("/usr/share/fonts/truetype/msttcorefonts/", "Arial.ttf");
		if (font == null) {
			font = getAbsolutePath("/usr/share/fonts/truetype/ttf-bitstream-veras/", "Vera.ttf");
		}
		if (font == null) {
			font = getAbsolutePath("/usr/share/fonts/truetype/ttf-dejavu/", "DejaVuSans.ttf");
		}
		return font;
	}

	@Override
	public String getShutdownCommand() {
		return "shutdown -h now";
	}

	// destroy a process safely (kill -TERM on Unix)
	@Override
	public void destroyProcess(final Process p) {
		if (p != null) {
			long pid = ProcessUtil.getProcessId(p);
			if (pid != 0) {
				LOGGER.trace("Killing the Unix process: " + pid);
				Runnable r = () -> {
					UMSUtils.sleep(TERM_TIMEOUT);

					try {
						p.exitValue();
					} catch (IllegalThreadStateException itse) {
						// Unix only
						// still running: nuke it
						// kill -14 (ALRM) works (for MEncoder) and is less
						// dangerous than kill -9 so try that first
						if (!kill(pid, 14)) {
							// This is a last resort, so let's not be
							// too eager
							UMSUtils.sleep(ALRM_TIMEOUT);

							kill(pid, 9);
						}
					}
				};

				Thread failsafe = new Thread(r, "Process Destroyer");
				failsafe.start();
			}
			p.destroy();
		}
	}

	/*
	 * FIXME: this is a hack - destroy() *should* work
	 *
	 * call chain (innermost last):
	 *
	 * WaitBufferedInputStream.close BufferedOutputFile.detachInputStream
	 * ProcessWrapperImpl.stopProcess ProcessUtil.destroy ProcessUtil.kill
	 *
	 * my best guess is that the process's stdout/stderr streams aren't
	 * being/haven't been fully/promptly consumed. From the abovelinked article:
	 *
	 * The Java 6 API clearly states that failure to promptly “read the output
	 * stream of the subprocess may cause the subprocess to block, and even
	 * deadlock.
	 *
	 * This is corroborated by the fact that destroy() works fine if the process
	 * is allowed to run to completion:
	 *
	 * https://code.google.com/p/ps3mediaserver/issues/detail?id=680#c11
	 */
	// send a Unix process the specified signal
	private static boolean kill(long pid, int signal) {
		boolean killed = false;
		LOGGER.warn("Sending kill -" + signal + " to the Unix process: " + pid);
		try {
			ProcessBuilder processBuilder = new ProcessBuilder("kill", "-" + signal, Long.toString(pid));
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			// consume the error and output process streams
			StreamGobbler.consume(process.getInputStream(), true);
			int exit = ProcessUtil.waitFor(process);
			if (exit == 0) {
				killed = true;
				LOGGER.debug("Successfully sent kill -" + signal + " to the Unix process: " + pid);
			}
		} catch (IOException e) {
			LOGGER.error("Error calling: kill -" + signal + " " + pid, e);
		}

		return killed;
	}

}
