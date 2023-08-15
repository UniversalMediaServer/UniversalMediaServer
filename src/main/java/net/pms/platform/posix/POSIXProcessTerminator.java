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
package net.pms.platform.posix;

import java.io.IOException;
import java.util.Locale;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.service.process.AbstractProcessTerminator;
import net.pms.service.process.ProcessInfo;
import net.pms.service.process.ProcessManager;
import net.pms.service.process.ProcessState;

public class POSIXProcessTerminator extends AbstractProcessTerminator {

	public POSIXProcessTerminator(@Nonnull ProcessManager owner) {
		super(owner);
	}

	/**
	 * Attempts to stop a POSIX {@link Process} using various methods
	 * depending on the current {@link ProcessState}.
	 *
	 * @param processInfo the {@link ProcessInfo} referencing the POSIX
	 *            {@link Process} to stop.
	 * @throws InterruptedException If the {@link Thread} was interrupted
	 *             during the operation.
	 */
	@Override
	protected void stopPlatformProcess(@Nullable ProcessInfo processInfo) throws InterruptedException {
		if (processInfo == null) {
			return;
		}
		if (processInfo.getState() == ProcessState.RUNNING) {
			if (sendPOSIXSignal(processInfo, POSIXSignal.SIGTERM)) {
				processInfo.setState(ProcessState.SIGTERM);
				processes.put(getSchedule(processInfo.getTerminateTimeoutMS()), processInfo);
				return;
			}
		}
		if (
			processInfo.getState() == ProcessState.RUNNING ||
			processInfo.getState() == ProcessState.SIGTERM
		) {
			String nameLower = processInfo.getName().toLowerCase(Locale.ROOT);
			if ((
					nameLower.contains("mencoder") ||
					nameLower.contains("mplayer")
				) &&
				sendPOSIXSignal(processInfo, POSIXSignal.SIGALRM)
			) {
				//Special case for MPlayer/MEncoder which responds to SIGALRM
				processInfo.setState(ProcessState.SIGALRM);
				processes.put(getSchedule(processInfo.getTerminateTimeoutMS()), processInfo);
				return;
			}
		}
		if ((
			processInfo.getState() == ProcessState.RUNNING ||
			processInfo.getState() == ProcessState.SIGTERM ||
			processInfo.getState() == ProcessState.SIGALRM
			) &&
			sendPOSIXSignal(processInfo, POSIXSignal.SIGKILL)
		) {
			processInfo.setState(ProcessState.SIGKILL);
			processes.put(getSchedule(Math.max(500, processInfo.getTerminateTimeoutMS())), processInfo);
			return;
		}
		LOGGER.warn(
			"All previous attempts to terminate process \"{}\" ({}) has failed, leaving it to the JVM and hoping for the best",
			processInfo.getName(),
			processInfo.getPID()
		);
		destroyProcess(processInfo.getProcess());
	}

	/**
	 * Sends a {@code POSIX signal} to the specified POSIX {@link Process}.
	 *
	 * @param processInfo the {@link ProcessInfo} referencing the
	 *            {@link Process} to send to.
	 * @param signal the {@link POSIXSignal} to send.
	 * @return {@code true} if the signal was sent, {@code false} otherwise.
	 * @throws InterruptedException If the {@link Thread} was interrupted
	 *             during the operation.
	 */
	protected boolean sendPOSIXSignal(
		@Nonnull ProcessInfo processInfo,
		@Nullable POSIXSignal signal
	) throws InterruptedException {
		if (signal == null) {
			signal = POSIXSignal.SIGTERM;
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"Attempting to send {} to timed out process \"{}\" ({})",
				signal,
				processInfo.getName(),
				processInfo.getPID()
			);
		}
		ProcessBuilder processBuilder = new ProcessBuilder(
			"kill",
			"-" + signal.getValue(),
			Long.toString(processInfo.getPID())
		);
		processBuilder.redirectErrorStream(true);
		try {
			Process process = processBuilder.start();
			gobbleStream(process.getInputStream());
			int exitCode = process.waitFor();
			if (exitCode != 0) {
				LOGGER.debug(
					"kill -{} failed for process \"{}\" ({}) with exit code {}",
					signal.getValue(),
					processInfo.getName(),
					processInfo.getPID(),
					exitCode
				);
				return false;
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("{} sent to process \"{}\" ({})", signal, processInfo.getName(), processInfo.getPID());
			}
			return true;
		} catch (IOException e) {
			LOGGER.error(
				"kill -{} for process \"{}\" ({}) failed with: {}",
				signal.getValue(),
				processInfo.getName(),
				processInfo.getPID(),
				e.getMessage()
			);
			LOGGER.trace("", e);
			return false;
		}
	}

}
