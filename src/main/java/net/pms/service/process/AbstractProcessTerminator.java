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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.util.ProcessUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The process terminator implementation.
 *
 * @author Nadahar
 */
public abstract class AbstractProcessTerminator extends Thread {

	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractProcessTerminator.class);

	/** The {@link TreeMap} of {@link Process}es scheduled for shutdown */
	protected final TreeMap<Long, ProcessInfo> processes = new TreeMap<>();

	/** *  The {@link ProcessManager} "owning" this {@link AbstractProcessTerminator} */
	protected final ProcessManager owner;

	/**
	 * Creates a new instance.
	 *
	 * @param owner the {@link ProcessManager} controlling this terminator thread.
	 */
	protected AbstractProcessTerminator(@Nonnull ProcessManager owner) {
		super("Process Terminator");
		if (owner == null) {
			throw new IllegalArgumentException("owner cannot be null");
		}
		this.owner = owner;
		this.setDaemon(true);
	}

	/**
	 * Gobbles (consumes) an {@link InputStream}.
	 *
	 * @param is the {@link InputStream} to gobble.
	 */
	@SuppressWarnings("checkstyle:EmptyBlock")
	protected void gobbleStream(InputStream is) {
		if (is == null) {
			return;
		}
		byte[] gobbler = new byte[1024];
		try {
			while (is.read(gobbler) != -1) {
				//do nothing
			}
		} catch (IOException e) {
			LOGGER.error("Gobbling of {} failed with: {}", is.getClass(), e.getMessage());
			LOGGER.trace("", e);
		}
	}

	protected abstract void stopPlatformProcess(@Nullable ProcessInfo processInfo) throws InterruptedException;

	/**
	 * Attempts to stop a {@link Process} by delegating to the platform
	 * dependent method unless the process is already stopped.
	 *
	 * @param processInfo the {@link ProcessInfo} referencing the
	 *            {@link Process} to stop.
	 * @throws InterruptedException If the {@link Thread} was interrupted
	 *             during the operation.
	 */
	protected void stopProcess(@Nullable ProcessInfo processInfo) throws InterruptedException {
		if (processInfo == null) {
			return;
		}
		if (ProcessUtil.isProcessIsAlive(processInfo.getProcess())) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
					"Trying to terminate process \"{}\" ({}) since its allowed run time has expired",
					processInfo.getName(),
					processInfo.getPID()
				);
			}
			stopPlatformProcess(processInfo);
		} else {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(
					"Successfully terminated process \"{}\" ({})", processInfo.getName(), processInfo.getPID()
				);
			}
			destroyProcess(processInfo.process);
		}
	}

	protected static void closeSilently(AutoCloseable autoCloseable) {
		if (autoCloseable != null) {
			try {
				autoCloseable.close();
			} catch (Exception e) {
				//do nothing
			}
		}
	}

	/**
	 * Destroys a {@link Process} after closing all the attached streams.
	 *
	 * @param process the {@link Process} to destroy.
	 */
	protected void destroyProcess(@Nullable Process process) {
		if (process == null) {
			return;
		}

		closeSilently(process.getInputStream());
		closeSilently(process.getErrorStream());
		closeSilently(process.getOutputStream());
		process.destroy();
	}

	/**
	 * Gets the next available schedule time in nanoseconds that is at least
	 * {@code delayMS} milliseconds from now.
	 *
	 * @param delayMS the minimum delay time in milliseconds.
	 * @return The next available schedule time in nanoseconds.
	 */
	protected Long getSchedule(long delayMS) {
		Long schedule = System.nanoTime() + delayMS * 1000000;
		while (processes.get(schedule) != null) {
			schedule++;
		}
		return schedule;
	}

	@Override
	public void run() {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("ProcessTerminator is starting");
		}
		try {
			work(false);
		} catch (InterruptedException e) {
			Thread.interrupted();
			owner.clearWorker(this);
			LOGGER.debug("Shutting down ProcessTerminator");
			try {
				work(true);
			} catch (InterruptedException e1) {
				LOGGER.debug(
					"ProcessTerminator interrupted while shutting down, terminating without terminating managed processes"
				);
			} catch (Throwable e1) {
				LOGGER.error(
					"Unexpected error in ProcessTerminator while shutting down, terminating without terminating managed processes",
					e.getClass().getSimpleName()
				);
			}
		} catch (Throwable e) {
			owner.clearWorker(this);
			LOGGER.error(
				"Unexpected error in ProcessTerminator, shutting down managed processes: {}",
				e.getMessage()
			);
			LOGGER.trace("", e);
			try {
				work(true);
			} catch (InterruptedException e1) {
				LOGGER.debug(
					"ProcessTerminator interrupted while shutting down, terminating without terminating managed processes"
				);
			} catch (Throwable e1) {
				LOGGER.error(
					"Unexpected error in ProcessTerminator while trying to shut down from a previous {}, " +
					"terminating without terminating managed processes",
					e.getClass().getSimpleName()
					);
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("ProcessTerminator has stopped");
		}
	}

	/**
	 * Performs the actual work in the {@link #run()} method.
	 *
	 * @param shutdown {@code true} if all managed {@link Process}es should
	 *            be scheduled for immediate shutdown and return,
	 *            {@code false} to keep running until interrupted.
	 * @throws InterruptedException If interrupted during work.
	 */
	protected void work(final boolean shutdown) throws InterruptedException {
		if (shutdown) {
			// Reschedule running processes for immediate shutdown
			ArrayList<ProcessInfo> reschedule = new ArrayList<>();
			for (Iterator<Map.Entry<Long, ProcessInfo>> iterator = processes.entrySet().iterator(); iterator.hasNext();) {
				Map.Entry<Long, ProcessInfo> entry = iterator.next();
				if (entry.getValue().getState() == ProcessState.RUNNING) {
					reschedule.add(entry.getValue());
					iterator.remove();
				}
			}
			int delay = 0;
			for (ProcessInfo processInfo : reschedule) {
				if (processInfo.getTerminateTimeoutMS() > 500) {
					processInfo.setTerminateTimeoutMS(500);
				}
				processes.put(getSchedule(delay++), processInfo);
			}
			if (LOGGER.isTraceEnabled() && !reschedule.isEmpty()) {
				LOGGER.trace(
					"ProcessTerminator rescheduled {} process{} for immediate shutdown",
					reschedule.size(),
					reschedule.size() > 1 ? "es" : "");
			}
		}

		ProcessTicket currentTicket;
		while (true) {
			if (!shutdown) {
				// Schedule incoming
				do {
					synchronized (owner.incoming) {
						currentTicket = owner.incoming.poll();
					}
					if (currentTicket != null && currentTicket.getAction() != null) {
						switch (currentTicket.getAction()) {
							case ADD -> {
								processes.put(getSchedule(currentTicket.getTimeoutMS()), new ProcessInfo(
										currentTicket.getProcess(),
										currentTicket.getName(),
										currentTicket.getTerminateTimeoutMS()
								));	if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(
											"ProcessTerminator scheduled shutdown of process \"{}\" in {} milliseconds",
											currentTicket.getName(),
											currentTicket.getTimeoutMS()
									);
								}
							}
							case REMOVE -> {
								ProcessInfo remove = null;
								for (
										Iterator<Map.Entry<Long, ProcessInfo>> iterator = processes.entrySet().iterator();
										iterator.hasNext();
										) {
									Map.Entry<Long, ProcessInfo> entry = iterator.next();
									if (entry.getValue().getProcess() == currentTicket.getProcess()) {
										remove = entry.getValue();
										iterator.remove();
									}
								}	if (LOGGER.isDebugEnabled()) {
									if (remove == null) {
										LOGGER.debug(
												"Couldn't find {} process to remove from process management",
												currentTicket.getName()
										);
									} else if (LOGGER.isTraceEnabled()) {
										LOGGER.trace(
												"ProcessTerminator unscheduled process \"{}\" ({})",
												remove.getName(),
												remove.getPID()
										);
									}
								}
							}
							case SHUTDOWN -> {
								ProcessInfo reschedule = null;
								for (
										Iterator<Map.Entry<Long, ProcessInfo>> iterator = processes.entrySet().iterator();
										iterator.hasNext();
										) {
									Map.Entry<Long, ProcessInfo> entry = iterator.next();
									if (entry.getValue().getProcess() == currentTicket.getProcess()) {
										reschedule = entry.getValue();
										iterator.remove();
									}
								}	if (reschedule != null) {
									if (currentTicket.getTerminateTimeoutMS() > 0) {
										reschedule.setTerminateTimeoutMS(Math.max(100, currentTicket.getTerminateTimeoutMS()));
									}
									processes.put(getSchedule(0), reschedule);
									if (LOGGER.isTraceEnabled()) {
										LOGGER.trace(
												"ProcessTerminator rescheduled process \"{}\" ({}) for immediate shutdown",
												reschedule.getName(),
												reschedule.getPID()
										);
									}
								} else if (LOGGER.isDebugEnabled()) {
									LOGGER.debug(
											"ProcessTerminator: No matching {} process found to reschedule for immediate shutdown"
									);
								}
							}
							default -> throw new AssertionError("Unimplemented ProcessTicketAction");
						}
					}
				} while (currentTicket != null);
			}

			//Process schedule
			while (!processes.isEmpty() && processes.firstKey() <= System.nanoTime()) {
				ProcessInfo processInfo = processes.remove(processes.firstKey());
				stopProcess(processInfo);
			}

			// Wait
			if (processes.isEmpty()) {
				if (shutdown) {
					break;
				}
				synchronized (owner.incoming) {
					if (owner.incoming.isEmpty()) {
						if (LOGGER.isTraceEnabled()) {
							LOGGER.trace("ProcessTerminator is waiting for new tickets");
						}
						owner.incoming.wait();
					}
				}
			} else {
				long waitTime = processes.firstKey() - System.nanoTime();
				if (waitTime > 0) {
					synchronized (owner.incoming) {
						if (owner.incoming.isEmpty()) {
							if (LOGGER.isTraceEnabled()) {
								if (waitTime < 1000000) {
									LOGGER.trace("ProcessTerminator is waiting {} nanoseconds", waitTime);
								} else {
									long waitTimeMS = waitTime / 1000000;
									LOGGER.trace(
										"ProcessTerminator is waiting {} millisecond{}",
										waitTimeMS,
										waitTimeMS == 1 ? "" : "s"
									);
								}
							}
							owner.incoming.wait(waitTime / 1000000, (int) (waitTime % 1000000));
						}
					}
				}
			}
		}
	}
}