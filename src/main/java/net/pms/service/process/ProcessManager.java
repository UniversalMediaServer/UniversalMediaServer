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

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;
import net.pms.platform.PlatformUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to manage the shutdown of external processes if they run
 * longer than their expected run time or hangs. It uses its own thread and
 * internal scheduling to shut down managed processes once their run time
 * expires. A graceful shutdown is initially escalating to less graceful methods
 * until successful. If nothing works, the shutdown is left to the JVM with
 * {@link Process#destroy()} with its known shortcomings.
 *
 * @author Nadahar
 */
@ThreadSafe
public class ProcessManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProcessManager.class);

	/** The queue of incoming {@link ProcessTicket}s. */
	protected final LinkedList<ProcessTicket> incoming = new LinkedList<>();

	/** *  The {@link AbstractProcessTerminator} thread */
	@GuardedBy("incoming")
	protected AbstractProcessTerminator terminator;

	/**
	 * Creates a new instance and starts the {@link AbstractProcessTerminator} thread.
	 */
	public ProcessManager() {
		start();
	}

	/**
	 * Starts the {@link ProcessManager}. This will be called automatically in
	 * the constructor, and need only be called if {@link #stop()} has
	 * previously been called.
	 */
	public final void start() {
		synchronized (incoming) {
			if (terminator == null || !terminator.isAlive()) {
				LOGGER.debug("Starting ProcessManager");
				terminator = PlatformUtils.INSTANCE.getProcessTerminator(this);
				terminator.start();
			} else if (LOGGER.isDebugEnabled()) {
				LOGGER.warn("ProcessManager is already running, start attempt failed");
			}
		}
	}

	/**
	 * Stops the {@link ProcessManager}. This will cause its process terminator
	 * thread to terminate all managed processes and stop.
	 */
	public void stop() {
		AbstractProcessTerminator currentTerminator;
		synchronized (incoming) {
			currentTerminator = terminator;
			terminator = null;
		}
		if (currentTerminator != null) {
			LOGGER.debug("Stopping ProcessManager");
			currentTerminator.interrupt();
			try {
				currentTerminator.join();
			} catch (InterruptedException e) {
				LOGGER.debug("ProcessManager was interrupted while waiting for the process terminator to terminate");
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Detaches the {@link AbstractProcessTerminator} thread unless a new has already
	 * been set.
	 *
	 * @param terminator the {@link AbstractProcessTerminator} instance to clear.
	 */
	protected void clearWorker(AbstractProcessTerminator terminator) {
		synchronized (incoming) {
			if (this.terminator == terminator) {
				this.terminator = null;
			}
		}
	}

	/**
	 * Adds a {@link Process} to be managed by this {@link ProcessManager}.
	 *
	 * @param process the {@link Process} to manage.
	 * @param processName the name of the process used for
	 *            logging/identification.
	 * @param timeoutMS the timeout for this {@link Process} in milliseconds.
	 *            When this time has expired, the process will be shut down if
	 *            it isn't already finished. If it's already finished, it will
	 *            simply be removed from the schedule.
	 * @param terminateTimeoutMS the timeout for shutdown attempts in
	 *            milliseconds. This timeout is used for each shutdown attempt
	 *            before escalating to the next level. Any value below 100
	 *            milliseconds will be set to 100 milliseconds.
	 */
	public void addProcess(
		@Nonnull Process process,
		@Nonnull String processName,
		long timeoutMS,
		long terminateTimeoutMS
	) {
		addTicket(new ProcessTicket(
			process, processName,
			ProcessTicketAction.ADD,
			timeoutMS,
			terminateTimeoutMS
		));
	}

	/**
	 * Adds a {@link Process} to be managed by this {@link ProcessManager}.
	 *
	 * @param process the {@link Process} to manage.
	 * @param processName the name of the process used for
	 *            logging/identification.
	 * @param timeout the timeout for this {@link Process} in {@code timeUnit}.
	 *            When this time has expired, the process will be shut down if
	 *            it isn't already finished. If it's already finished, it will
	 *            simply be removed from the schedule.
	 * @param timeUnit the {@link TimeUnit} for {@code timeout}.
	 * @param terminateTimeoutMS the timeout for shutdown attempts in
	 *            milliseconds. This timeout is used for each shutdown attempt
	 *            before escalating to the next level. Any value below 100
	 *            milliseconds will be set to 100 milliseconds.
	 */
	public void addProcess(
		@Nonnull Process process,
		@Nonnull String processName,
		long timeout,
		@Nonnull TimeUnit timeUnit,
		long terminateTimeoutMS
	) {
		addTicket(new ProcessTicket(
			process, processName,
			ProcessTicketAction.ADD,
			timeUnit.toMillis(timeout),
			terminateTimeoutMS
		));
	}

	/**
	 * Reschedule a managed process for immediate shutdown. If {@code process}
	 * isn't found among the managed processes no action is taken.
	 *
	 * @param process the {@link Process} to shutdown.
	 * @param processName the name of the process used for
	 *            logging/identification.
	 * @param terminateTimeoutMS the timeout for shutdown attempts in
	 *            milliseconds. This timeout is used for each shutdown attempt
	 *            before escalating to the next level. Any value below 100
	 *            milliseconds will be set to 100 milliseconds.
	 */
	public void shutdownProcess(
		@Nonnull Process process,
		@Nonnull String processName,
		long terminateTimeoutMS
	) {
		addTicket(new ProcessTicket(
			process,
			processName,
			ProcessTicketAction.SHUTDOWN,
			0,
			terminateTimeoutMS
		));
	}

	/**
	 * Reschedule a managed process for immediate shutdown. If {@code process}
	 * isn't found among the managed processes no action is taken.
	 *
	 * @param process the {@link Process} to shutdown.
	 * @param processName the name of the process used for
	 *            logging/identification.
	 */
	public void shutdownProcess(
		@Nonnull Process process,
		@Nonnull String processName
	) {
		addTicket(new ProcessTicket(
			process,
			processName,
			ProcessTicketAction.SHUTDOWN,
			0,
			0
		));
	}
	/**
	 * Removes a {@link Process} from management by this {@link ProcessManager}.
	 * This will cause the reference to the {@link Process} to be released
	 * allowing for earlier GC or prevent it from being shutdown at timeout.
	 *
	 * @param process the {@link Process} to remove from management.
	 * @param processName the name of the process used for
	 *            logging/identification.
	 */
	public void removeProcess(@Nonnull Process process, @Nonnull String processName) {
		addTicket(new ProcessTicket(process, processName, ProcessTicketAction.REMOVE, 0, 0));
	}

	/**
	 * Adds a {@link ProcessTicket} to the internal queue.
	 *
	 * @param ticket the {@link ProcessTicket} to add.
	 */
	protected void addTicket(@Nonnull ProcessTicket ticket) {
		if (ticket == null) {
			throw new IllegalArgumentException("ticket cannot be null");
		}
		synchronized (incoming) {
			incoming.add(ticket);
			incoming.notifyAll();
			if (terminator == null || !terminator.isAlive()) {
				LOGGER.warn(
					"ProcessManager added the following ticket while no ProcessTerminator is processing tickets: {}",
					ticket
				);
			}
		}
	}

}
