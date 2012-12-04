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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to execute the process of a short running command, for example
 * "vlc -version". This in contrary to {@link ProcessWrapperImpl}, which is
 * better suited for processes that take a long time to complete
 * and that may need to be terminated half way.
 * <p>
 * Typical use: create an instance, then call {@link getResult()} to execute
 * the command and get its output.
 */
public class ProcessWrapperLiteImpl implements ProcessWrapper {
	/** Logger */
	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessWrapperLiteImpl.class);

	/** System program and arguments to execute. */
	private String[] command;

	/**
	 * Instantiates a new process wrapper based on the specified operating
	 * system program and arguments. Instantiating the wrapper does not yet
	 * execute the command. A call to {@link getResult()} will execute it.
	 *
	 * @param command The operating system program and arguments to execute.
	 */
	public ProcessWrapperLiteImpl(String[] command) {
		this.command = command;
	}

	/**
	 * Executes the process and returns the combined output and error
	 * stream as a string. If the command fails to execute, an empty
	 * string is returned.
	 * <p>
	 * Note: this method assumes the command will finish by itself. Execution
	 * time is not limited in any way and the method will wait for the command
	 * to finish before returning the result.
	 *
	 * @return The combined output and error stream of the process. Individual
	 * 			lines will get a "\n" appended.
	 */
	public String getResult() {
		StringBuilder result = new StringBuilder();

		if (command == null || command.length == 0) {
			return "";
		}

		ProcessBuilder processBuilder = new ProcessBuilder(command);

		// Combine the output and error stream
		processBuilder.redirectErrorStream(true);

		try {
			LOGGER.trace("Executing command \"" + StringUtils.join(command, " ") + "\"");
			Process process = processBuilder.start();
			InputStream is = process.getInputStream();
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;

			while ((line = br.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}

			br.close();
			isr.close();
			is.close();
			process.waitFor();
			process.destroy();
		} catch (IOException e) {
			LOGGER.debug("Error executing command \"" + StringUtils.join(command, " ")  + "\"", e);
		} catch (InterruptedException e) {
			LOGGER.debug("Interrupted while waiting for command \"" + StringUtils.join(command, " ")
					+ "\" to finish.");
		}

		return result.toString();
	}

	/**
	 * Unused interface method.
	 * 
	 * @return Always returns <code>null</code>.
	 */
	@Override
	public InputStream getInputStream(long seek) throws IOException {
		return null;
	}

	/**
	 * Unused interface method.
	 * 
	 * @return Always returns <code>null</code>.
	 */
	@Override
	public ArrayList<String> getResults() {
		return null;
	}

	/**
	 * Unused interface method.
	 * 
	 * @return Always returns <code>false</code>.
	 */
	@Override
	public boolean isDestroyed() {
		return false;
	}

	/**
	 * Unused interface method.
	 */
	@Override
	public void runInNewThread() {
	}

	/**
	 * Unused interface method.
	 * 
	 * @return Always returns <code>false</code>.
	 */
	@Override
	public boolean isReadyToStop() {
		return false;
	}

	/**
	 * Unused interface method.
	 * 
	 * @param nullable Parameter is ignored.
	 */
	@Override
	public void setReadyToStop(boolean nullable) {
	}

	/**
	 * Unused interface method.
	 */
	@Override
	public void stopProcess() {
	}
}
