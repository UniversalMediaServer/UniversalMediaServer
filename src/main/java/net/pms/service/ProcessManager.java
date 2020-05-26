/*
 * Digital Media Server, for streaming digital media to UPnP AV or DLNA
 * compatible devices based on PS3 Media Server and Universal Media Server.
 * Copyright (C) 2016 Digital Media Server developers.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see http://www.gnu.org/licenses/.
 */
package net.pms.service;

import static org.apache.commons.lang3.StringUtils.isBlank;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import javax.annotation.concurrent.ThreadSafe;
import org.h2.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.Memory;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.IntByReference;
import net.pms.configuration.PlatformProgramPaths;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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

	/** Windows API {@code CTRL_C_EVENT} */
	public static final int CTRL_C_EVENT = 0;

	/** Windows API {@code CTRL_BREAK_EVENT} */
	public static final int CTRL_BREAK_EVENT = 1;

	/** The queue of incoming {@link ProcessTicket}s. */
	protected final LinkedList<ProcessTicket> incoming = new LinkedList<>();

	/** The {@link ProcessTerminator} thread */
	@GuardedBy("incoming")
	protected ProcessTerminator terminator;

	/**
	 * Creates a new instance and starts the {@link ProcessTerminator} thread.
	 */
	public ProcessManager() {
		start();
	}

	/**
	 * Starts the {@link ProcessManager}. This will be called automatically in
	 * the constructor, and need only be called if {@link #stop()} has
	 * previously been called.
	 */
	public void start() {
		synchronized (incoming) {
			if (terminator == null || !terminator.isAlive()) {
				LOGGER.debug("Starting ProcessManager");
				terminator = new ProcessTerminator(this);
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
		ProcessTerminator currentTerminator;
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
			}
		}
	}

	/**
	 * Detaches the {@link ProcessTerminator} thread unless a new has already
	 * been set.
	 *
	 * @param terminator the {@link ProcessTerminator} instance to clear.
	 */
	protected void clearWorker(ProcessTerminator terminator) {
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
			incoming.notify();
			if (terminator == null || !terminator.isAlive()) {
				LOGGER.warn(
					"ProcessManager added the following ticket while no ProcessTerminator is processing tickets: {}",
					ticket
				);
			}
		}
	}

	/**
	 * Checks if the process is still alive using reflection if possible.
	 *
	 * @param process the {@link Process} to check.
	 * @return {@code true} if the process is still alive, {@code false}
	 *         otherwise.
	 */
	@SuppressFBWarnings("REC_CATCH_EXCEPTION")
	public static boolean isProcessIsAlive(@Nullable Process process) {
		if (process == null) {
			return false;
		}
		// XXX replace with Process.isAlive() in Java 8
		try {
			Field field;
			field = process.getClass().getDeclaredField("handle");
			field.setAccessible(true);
			long handle = (long) field.get(process);
			field = process.getClass().getDeclaredField("STILL_ACTIVE");
			field.setAccessible(true);
			int stillActive = (int) field.get(process);
			Method method;
			method = process.getClass().getDeclaredMethod("getExitCodeProcess", long.class);
			method.setAccessible(true);
			int exitCode = (int) method.invoke(process, handle);
			return exitCode == stillActive;
		} catch (Exception e) {
			// Reflection failed, use the backup solution
		}
		try {
			process.exitValue();
			return false;
		} catch (IllegalThreadStateException e) {
			return true;
		}
	}

	/**
	 * Retrieves the process ID (PID) for the specified {@link Process}.
	 *
	 * @param process the {@link Process} for whose PID to retrieve.
	 * @return The PID or zero if the PID couldn't be retrieved.
	 */
	public static int getProcessId(@Nullable Process process) {
		if (process == null) {
			return 0;
		}
		try {
			Field field;
			if (Platform.isWindows()) {
				field = process.getClass().getDeclaredField("handle");
				field.setAccessible(true);
				int pid = Kernel32.INSTANCE.GetProcessId(new HANDLE(new Pointer(field.getLong(process))));
				if (pid == 0 && LOGGER.isDebugEnabled()) {
					int lastError = Kernel32.INSTANCE.GetLastError();
					LOGGER.debug("KERNEL32.getProcessId() failed with error {}", lastError);
				}
				return pid;
			}
			field = process.getClass().getDeclaredField("pid");
			field.setAccessible(true);
			return field.getInt(process);
		} catch (Exception e) {
			LOGGER.warn("Failed to get process id for process \"{}\": {}", process, e.getMessage());
			LOGGER.trace("", e);
			return 0;
		}
	}

	/**
	 * The process terminator implementation.
	 *
	 * @author Nadahar
	 */
	protected static class ProcessTerminator extends Thread {

		private static final Logger LOGGER = LoggerFactory.getLogger(ProcessTerminator.class);

		/** The {@link TreeMap} of {@link Process}es scheduled for shutdown */
		protected final TreeMap<Long, ProcessInfo> processes = new TreeMap<>();

		/** The {@link ProcessManager} "owning" this {@link ProcessTerminator} */
		protected final ProcessManager owner;

		/**
		 * Creates a new instance.
		 *
		 * @param owner the {@link ProcessManager} controlling this terminator thread.
		 */
		public ProcessTerminator(@Nonnull ProcessManager owner) {
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
				}
			} catch (IOException e) {
				LOGGER.error("Gobbling of {} failed with: {}", is.getClass(), e.getMessage());
				LOGGER.trace("", e);
			}
		}

		/**
		 * Sends {@code WM_CLOSE} to the specified Windows {@link Process}.
		 *
		 * @param processInfo the {@link ProcessInfo} referencing the
		 *            {@link Process} to send to.
		 * @return {@code true} if {@code WM_CLOSE} was sent, {@code false}
		 *         otherwise.
		 */
		protected boolean stopWindowsProcessWMClosed(@Nonnull ProcessInfo processInfo) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
					"Attempting to stop timed out process \"{}\" ({}) with WM_CLOSE",
					processInfo.getName(),
					processInfo.getPID()
				);
			}
			HANDLE hProc = Kernel32.INSTANCE.OpenProcess(
				Kernel32.SYNCHRONIZE | Kernel32.PROCESS_TERMINATE,
				false,
				processInfo.getPID()
			);
			if (hProc == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Failed to get Windows handle for process \"{}\" ({}) during WM_CLOSE",
						processInfo.getName(),
						processInfo.getPID()
					);
				}
				return false;
			}
			final Memory posted = new Memory(1);
			posted.setByte(0, (byte) 0);
			Memory dwPID = new Memory(4);
			dwPID.setInt(0, processInfo.getPID());
			User32.INSTANCE.EnumWindows(new WNDENUMPROC() {

				@Override
				public boolean callback(HWND hWnd, Pointer data) {
					IntByReference dwID = new IntByReference();
					User32.INSTANCE.GetWindowThreadProcessId(hWnd, dwID);

					if (dwID.getValue() == data.getInt(0)) {
						User32.INSTANCE.PostMessage(hWnd, User32.WM_CLOSE, new WPARAM(0), new LPARAM(0));
						posted.setByte(0, (byte) 1);
					}
					return true;
				}
			}, dwPID);
			Kernel32.INSTANCE.CloseHandle(hProc);
			if (LOGGER.isTraceEnabled()) {
				if (posted.getByte(0) > 0) {
					LOGGER.trace(
						"WM_CLOSE sent to process \"{}\" ({}) with PostMessage",
						processInfo.getName(),
						processInfo.getPID()
					);
				} else {
					LOGGER.trace(
						"Can't find any Windows belonging to process \"{}\" ({}), unable to send WM_CLOSE",
						processInfo.getName(),
						processInfo.getPID()
					);
				}
			}
			return posted.getByte(0) > 0;
		}

		/**
		 * Performs {@code TerminateProcess} on the specified Windows
		 * {@link Process}.
		 *
		 * @param processInfo the {@link ProcessInfo} referencing the
		 *            {@link Process} to terminate.
		 * @return {@code true} if {@code TerminateProcess} was executed,
		 *         {@code false} otherwise.
		 */
		protected boolean stopWindowsProcessTerminateProcess(@Nonnull ProcessInfo processInfo) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
					"Attempting to stop timed out process \"{}\" ({}) with TerminateProcess",
					processInfo.getName(),
					processInfo.getPID()
				);
			}
			HANDLE hProc = Kernel32.INSTANCE.OpenProcess(
				Kernel32.PROCESS_TERMINATE,
				false,
				processInfo.getPID()
			);
			if (hProc == null) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Failed to get Windows handle for process \"{}\" ({}) during TerminateProcess",
						processInfo.getName(),
						processInfo.getPID()
					);
				}
				return false;
			}
			boolean result = Kernel32.INSTANCE.TerminateProcess(hProc, 1);
			Kernel32.INSTANCE.CloseHandle(hProc);
			if (LOGGER.isTraceEnabled()) {
				if (result) {
					LOGGER.trace("TerminateProcess performed for process \"{}\" ({})", processInfo.getName(), processInfo.getPID());
				} else {
					LOGGER.trace("TerminateProcess failed for process \"{}\" ({})", processInfo.getName(), processInfo.getPID());
				}
			}
			return result;
		}

		/**
		 * Sends a {@code CtrlEvent} to the specified Windows {@link Process}.
		 *
		 * @param processInfo the {@link ProcessInfo} referencing the
		 *            {@link Process} to send to.
		 * @param ctrlEvent the {@code CtrlEvent} to send. Only
		 *            {@link ProcessManager#CTRL_BREAK_EVENT} and
		 *            {@link ProcessManager#CTRL_C_EVENT} are supported.
		 * @return {@code true} if a {@code CtrlEvent} was sent, {@code false}
		 *         otherwise.
		 * @throws InterruptedException If the {@link Thread} was interrupted
		 *             during the operation.
		 */
		protected boolean stopWindowsProcessCtrlEvent(@Nonnull ProcessInfo processInfo, int ctrlEvent) throws InterruptedException {
			if (PlatformProgramPaths.get().getCtrlSender() == null) {
				return false;
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
					"Attempting to stop timed out process \"{}\" ({}) with CtrlSender", processInfo.getName(), processInfo.getPID()
				);
			}
			ProcessBuilder processBuilder = new ProcessBuilder(
				PlatformProgramPaths.get().getCtrlSender().toString(),
				Integer.toString(processInfo.getPID()),
				Integer.toString(ctrlEvent)
			);
			processBuilder.redirectErrorStream(true);
			try {
				Process process = processBuilder.start();
				gobbleStream(process.getInputStream());
				int exitCode = process.waitFor();
				if (exitCode != 0) {
					if (exitCode == 1) {
						LOGGER.trace(
							"CtrlSender could not attach to PID {} for process \"{}\"",
							processInfo.getPID(),
							processInfo.getName()
						);
					} else {
						LOGGER.warn("An internal error caused CtrlSender to exit with code {}", exitCode);
					}
					return false;
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Ctrl + {} sent to process \"{}\" ({}) with CtrlSender",
						ctrlEvent == CTRL_C_EVENT ? "C" : ctrlEvent == CTRL_BREAK_EVENT ? "BREAK" : "?",
						processInfo.getName(),
						processInfo.getPID()
					);
				}
				return true;
			} catch (IOException e) {
				LOGGER.error(
					"CtrlSender for process \"{}\" ({}) failed with: {}",
					processInfo.getName(),
					processInfo.getPID(),
					e.getMessage()
				);
				LOGGER.trace("", e);
				return false;
			}
		}

		/**
		 * Performs a {@code TaskKill} on the specified Windows {@link Process}.
		 *
		 * @param processInfo the {@link ProcessInfo} referencing the
		 *            {@link Process} to {@code TaskKill}.
		 * @return {@code true} if a {@code TaskKill} was executed,
		 *         {@code false} otherwise.
		 * @throws InterruptedException If the {@link Thread} was interrupted
		 *             during the operation.
		 */
		protected boolean stopWindowsProcessTaskKill(@Nonnull ProcessInfo processInfo) throws InterruptedException {
			if (PlatformProgramPaths.get().getTaskKill() == null) {
				return false;
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace(
					"Attempting to stop timed out process \"{}\" ({}) with TaskKill",
					processInfo.getName(),
					processInfo.getPID()
				);
			}
			ProcessBuilder processBuilder = new ProcessBuilder(
				PlatformProgramPaths.get().getTaskKill().toString(),
				"/PID",
				Integer.toString(processInfo.getPID())
			);
			processBuilder.redirectErrorStream(true);
			try {
				Process process = processBuilder.start();
				gobbleStream(process.getInputStream());
				int exitCode = process.waitFor();
				if (exitCode != 0) {
					LOGGER.debug(
						"TaskKill failed for process \"{}\" ({}) with exit code {}",
						processInfo.getName(),
						processInfo.getPID(),
						exitCode
					);
					return false;
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace(
						"Taskkill performed for process \"{}\" ({})", processInfo.getName(), processInfo.getPID()
					);
				}
				return true;
			} catch (IOException e) {
				LOGGER.error(
					"TaskkKill for process \"{}\" ({}) failed with: {}",
					processInfo.getName(),
					processInfo.getPID(),
					e.getMessage()
				);
				LOGGER.trace("", e);
				return false;
			}
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
				Integer.toString(processInfo.getPID())
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

		/**
		 * Attempts to stop a Windows {@link Process} using various methods
		 * depending on the current {@link ProcessState}.
		 *
		 * @param processInfo the {@link ProcessInfo} referencing the Windows
		 *            {@link Process} to stop.
		 * @throws InterruptedException If the {@link Thread} was interrupted
		 *             during the operation.
		 */
		protected void stopWindowsProcess(@Nonnull ProcessInfo processInfo) throws InterruptedException {
			if (processInfo.getState() == ProcessState.RUNNING) {
				if (stopWindowsProcessWMClosed(processInfo)) {
					processInfo.setState(ProcessState.WM_CLOSED);
					processes.put(getSchedule(processInfo.getTerminateTimeoutMS()), processInfo);
					return;
				}
				if (stopWindowsProcessCtrlEvent(processInfo, CTRL_C_EVENT)) {
					processInfo.setState(ProcessState.CTRL_C);
					processes.put(getSchedule(processInfo.getTerminateTimeoutMS()), processInfo);
					return;
				}
			}
			if ((
					processInfo.getState() == ProcessState.RUNNING ||
					processInfo.getState() == ProcessState.WM_CLOSED ||
					processInfo.getState() == ProcessState.CTRL_C
				) &&
				PlatformProgramPaths.get().getTaskKill() != null
			) {
				if (stopWindowsProcessTaskKill(processInfo)) {
					processInfo.setState(ProcessState.TASKKILL);
					processes.put(getSchedule(processInfo.getTerminateTimeoutMS()), processInfo);
					return;
				}
			}
			if (stopWindowsProcessTerminateProcess(processInfo)) {
				processInfo.setState(ProcessState.TERMINATEPROCESS);
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
		 * Attempts to stop a POSIX {@link Process} using various methods
		 * depending on the current {@link ProcessState}.
		 *
		 * @param processInfo the {@link ProcessInfo} referencing the POSIX
		 *            {@link Process} to stop.
		 * @throws InterruptedException If the {@link Thread} was interrupted
		 *             during the operation.
		 */
		protected void stopPOSIXProcess(@Nonnull ProcessInfo processInfo) throws InterruptedException {
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
			if (isProcessIsAlive(processInfo.getProcess())) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
						"Trying to terminate process \"{}\" ({}) since its allowed run time has expired",
						processInfo.getName(),
						processInfo.getPID()
					);
				}
				if (Platform.isWindows()) {
					stopWindowsProcess(processInfo);
				} else {
					stopPOSIXProcess(processInfo);
				}
			} else {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug(
						"Successfully terminated process \"{}\" ({})", processInfo.getName(), processInfo.getPID()
					);
				}
				destroyProcess(processInfo.process);
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

			IOUtils.closeSilently(process.getInputStream());
			IOUtils.closeSilently(process.getErrorStream());
			IOUtils.closeSilently(process.getOutputStream());

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
			Long schedule = Long.valueOf(System.nanoTime() + delayMS * 1000000);
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
				for (Iterator<Entry<Long, ProcessInfo>> iterator = processes.entrySet().iterator(); iterator.hasNext();) {
					Entry<Long, ProcessInfo> entry = iterator.next();
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

			ProcessTicket currentTicket = null;
			while (true) {
				if (!shutdown) {
					// Schedule incoming
					do {
						synchronized (owner.incoming) {
							currentTicket = owner.incoming.poll();
						}
						if (currentTicket != null) {
							if (currentTicket.getAction() == ProcessTicketAction.ADD) {
								processes.put(getSchedule(currentTicket.getTimeoutMS()), new ProcessInfo(
										currentTicket.getProcess(),
										currentTicket.getName(),
										currentTicket.getTerminateTimeoutMS()
								));
								if (LOGGER.isTraceEnabled()) {
									LOGGER.trace(
										"ProcessTerminator scheduled shutdown of process \"{}\" in {} milliseconds",
										currentTicket.getName(),
										currentTicket.getTimeoutMS()
									);
								}
							} else if (currentTicket.getAction() == ProcessTicketAction.REMOVE) {
								ProcessInfo remove = null;
								for (
									Iterator<Entry<Long, ProcessInfo>> iterator = processes.entrySet().iterator();
									iterator.hasNext();
								) {
									Entry<Long, ProcessInfo> entry = iterator.next();
									if (entry.getValue().getProcess() == currentTicket.getProcess()) {
										remove = entry.getValue();
										iterator.remove();
									}
								}
								if (LOGGER.isDebugEnabled()) {
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
							} else if (currentTicket.getAction() == ProcessTicketAction.SHUTDOWN) {
								ProcessInfo reschedule = null;
								for (
									Iterator<Entry<Long, ProcessInfo>> iterator = processes.entrySet().iterator();
									iterator.hasNext();
								) {
									Entry<Long, ProcessInfo> entry = iterator.next();
									if (entry.getValue().getProcess() == currentTicket.getProcess()) {
										reschedule = entry.getValue();
										iterator.remove();
									}
								}
								if (reschedule != null) {
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
							} else {
								throw new AssertionError("Unimplemented ProcessTicketAction");
							}
						}
					} while (currentTicket != null);
				}

				//Process schedule
				while (!processes.isEmpty() && processes.firstKey().longValue() <= System.nanoTime()) {
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
					long waitTime = processes.firstKey().longValue() - System.nanoTime();
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

	/**
	 * This class represents a {@link Process} with corresponding orders to the
	 * {@link ProcessTerminator}.
	 *
	 * @author Nadahar
	 */
	@Immutable
	protected static class ProcessTicket {

		/** The ticket {@link Process} */
		protected final Process process;

		/** The ticket process name */
		protected final String processName;

		/** The ticket {@link ProcessTicketAction} */
		protected final ProcessTicketAction action;

		/** The timeout in milliseconds if applicable */
		protected final long timeoutMS;

		/** The termination timeout in milliseconds if applicable */
		protected final long terminateTimeoutMS;

		/**
		 * Creates a new ticket using the specified parameters.
		 *
		 * @param process the ticket {@link Process}.
		 * @param processName the ticket process name.
		 * @param action the {@link ProcessTicketAction}.
		 * @param timeoutMS the timeout in milliseconds if {@code action} is
		 *            {@link ProcessTicketAction#ADD}.
		 * @param terminateTimeoutMS the termination timeout in milliseconds if
		 *            {@code action} is {@link ProcessTicketAction#ADD}.
		 */
		public ProcessTicket(
			@Nonnull Process process,
			@Nonnull String processName,
			@Nonnull ProcessTicketAction action,
			long timeoutMS,
			long terminateTimeoutMS
		) {
			if (process == null) {
				throw new IllegalArgumentException("process cannot be null");
			}
			if (isBlank(processName)) {
				throw new IllegalArgumentException("processName cannot be blank");
			}
			if (action == null) {
				throw new IllegalArgumentException("action cannot be null");
			}
			this.process = process;
			this.processName = processName;
			this.action = action;
			this.timeoutMS = Math.max(0, timeoutMS);
			this.terminateTimeoutMS = Math.max(action == ProcessTicketAction.ADD ? 100 : 0, terminateTimeoutMS);
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
		 * @return The {@link ProcessTicketAction}.
		 */
		public ProcessTicketAction getAction() {
			return action;
		}

		/**
		 * @return The process timeout in milliseconds.
		 */
		public long getTimeoutMS() {
			return timeoutMS;
		}

		/**
		 * @return The process terminate timeout in milliseconds.
		 */
		public long getTerminateTimeoutMS() {
			return terminateTimeoutMS;
		}

		@Override
		public String toString() {
			return
				"ProcessTicket [Name=" + processName + ", Timeout=" + timeoutMS +
				" ms, Terminate Timeout=" + terminateTimeoutMS + " ms, Action=" + action + "]";
		}
	}

	/**
	 * This {@code enum} represents an action/command in a {@link ProcessTicket}.
	 *
	 * @author Nadahar
	 */
	protected static enum ProcessTicketAction {

		/** Add a process for management */
		ADD,

		/** Remove a process from management */
		REMOVE,

		/** Schedule a process for immediate shutdown */
		SHUTDOWN;
	}

	/**
	 * This class represents a {@link Process} and its state information
	 * relevant for {@link ProcessTerminator}.
	 *
	 * @author Nadahar
	 */
	@NotThreadSafe
	protected static class ProcessInfo {

		/** The {@link Process} */
		protected final Process process;

		/** The process name used for logging and identification */
		protected final String processName;

		/** The process ID for the {@link Process} */
		protected final int pid;

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
			this.pid = getProcessId(process);
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
		public int getPID() {
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

	/**
	 * This {@code enum} represents the states a {@link Process} can have in a
	 * {@link ProcessTerminator} context.
	 *
	 * @author Nadahar
	 */
	protected static enum ProcessState {

		/** Running; initial state */
		RUNNING,

		/** {@code WM_CLOSE} has been sent */
		WM_CLOSED,

		/** Ctrl + C has been sent */
		CTRL_C,

		/** TaskKill has been executed */
		TASKKILL,

		/** TerminateProcess has been called */
		TERMINATEPROCESS,

		/** {@link POSIXSignal#SIGTERM} has been sent */
		SIGTERM,

		/** {@link POSIXSignal#SIGALRM} has been sent */
		SIGALRM,

		/** {@link POSIXSignal#SIGKILL} has been sent */
		SIGKILL;
	}

	/**
	 * This {@code enum} represents the different POSIX signals
	 *
	 * @author Nadahar
	 */
	public static enum POSIXSignal {

		/**
		 * 1: POSIX {@code SIGHUP} - Hangup detected on controlling terminal or
		 * death of controlling process
		 */
		SIGHUP(1),

		/** 2: POSIX {@code SIGINT} - Interrupt from keyboard */
		SIGINT(2),

		/** 3: POSIX {@code SIGQUIT} - Quit from keyboard */
		SIGQUIT(3),

		/** 4: POSIX {@code SIGILL} - Illegal Instruction */
		SIGILL(4),

		/** 6: POSIX {@code SIGABRT} - Abort signal from abort() */
		SIGABRT(6),

		/** 8: POSIX {@code SIGFPE} - Floating-point exception */
		SIGFPE(8),

		/** 9: POSIX {@code SIGKILL} - Kill signal */
		SIGKILL(9),

		/** 11: POSIX {@code SIGSEGV} - Invalid memory reference */
		SIGSEGV(11),

		/**
		 * 13: POSIX {@code SIGPIPE} - Broken pipe: write to pipe with no
		 * readers
		 */
		SIGPIPE(13),

		/** 14: POSIX {@code SIGALRM} - Timer signal from alarm() */
		SIGALRM(14),

		/** 15: POSIX {@code SIGTERM} - Termination signal */
		SIGTERM(15);

		private final int value;

		private POSIXSignal(int value) {
			this.value = value;
		}

		/**
		 * @return The integer value.
		 */
		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return name() + " (" + value + ")";
		}
	}
}
