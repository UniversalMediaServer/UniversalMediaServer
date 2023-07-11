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
package net.pms.service.process;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import net.pms.util.ProcessUtil;

/**
 * This class represents a {@link Process} and its state information
 * relevant for {@link ProcessTerminator}.
 *
 * @author Nadahar
 */
@NotThreadSafe
public class ProcessInfo {

	/** The {@link Process} */
	protected final Process process;

	/** The process name used for logging and identification */
	protected final String processName;

	/** The process ID for the {@link Process} */
	protected final long pid;

	/** The termination timeout in milliseconds */
	protected long terminateTimeoutMS;

	/** The current {@link ProcessState} for the {@link Process} */
	protected ProcessState state = ProcessState.RUNNING;

	/**
	 * Creates a new instance using the specified parameters.
	 *
	 * @param process the {@link Process}.
	 * @param processName the process name used for logging and identification.
	 * @param terminateTimeoutMS the termination timeout in milliseconds.
	 */
	public ProcessInfo(@Nonnull Process process, @Nonnull String processName, long terminateTimeoutMS) {
		if (process == null) {
			throw new IllegalArgumentException("process cannot be null");
		}
		if (processName == null) {
			throw new IllegalArgumentException("processName cannot be null");
		}
		this.process = process;
		this.processName = processName;
		this.pid = ProcessUtil.getProcessId(process);
		if (this.pid == 0) {
			throw new IllegalStateException("Unable to retrieve process id");
		}
		this.terminateTimeoutMS = terminateTimeoutMS;
	}

	/**
	 * @return The {@link Process}.
	 */
	public Process getProcess() {
		return process;
	}

	/**
	 * @return The {@link Process} name.
	 */
	public String getName() {
		return processName;
	}

	/**
	 * @return The process ID.
	 */
	public long getPID() {
		return pid;
	}

	/**
	 * @return The process terminate timeout in milliseconds.
	 */
	public long getTerminateTimeoutMS() {
		return terminateTimeoutMS;
	}

	/**
	 * Sets the terminate timeout value.
	 *
	 * @param terminateTimeoutMS the terminate timeout in milliseconds.
	 */
	public void setTerminateTimeoutMS(long terminateTimeoutMS) {
		this.terminateTimeoutMS = terminateTimeoutMS;
	}

	/**
	 * @return The current {@link ProcessState}.
	 */
	public ProcessState getState() {
		return state;
	}

	/**
	 * Sets the current {@link ProcessState}.
	 *
	 * @param state the {@link ProcessState} to set.
	 */
	public void setState(ProcessState state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "ProcessInfo [Name=" + processName + ", PID=" + pid + ", State=" + state + "]";
	}
}

