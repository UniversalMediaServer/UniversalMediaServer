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
package net.pms.platform.windows;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import java.io.IOException;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.pms.platform.PlatformProgramPaths;
import net.pms.service.process.AbstractProcessTerminator;
import net.pms.service.process.ProcessInfo;
import net.pms.service.process.ProcessManager;
import net.pms.service.process.ProcessState;

public class WindowsProcessTerminator extends AbstractProcessTerminator {
	/** Windows API {@code CTRL_C_EVENT} */
	public static final int CTRL_C_EVENT = 0;
	/** Windows API {@code CTRL_BREAK_EVENT} */
	public static final int CTRL_BREAK_EVENT = 1;

	public WindowsProcessTerminator(@Nonnull ProcessManager owner) {
		super(owner);
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
	@Override
	public void stopPlatformProcess(@Nullable ProcessInfo processInfo) throws InterruptedException {
		if (processInfo == null) {
			return;
		}
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
			String.valueOf(PlatformProgramPaths.get().getTaskKill()),
			"/PID",
			Long.toString(processInfo.getPID())
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
			String.valueOf(PlatformProgramPaths.get().getCtrlSender()),
			Long.toString(processInfo.getPID()),
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
	 * Sends {@code WM_CLOSE} to the specified Windows {@link Process}.
	 * Equivalent to posix SIGTERM.
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
		WinNT.HANDLE hProc = com.sun.jna.platform.win32.Kernel32.INSTANCE.OpenProcess(
			com.sun.jna.platform.win32.Kernel32.SYNCHRONIZE | com.sun.jna.platform.win32.Kernel32.PROCESS_TERMINATE,
			false,
			(int) processInfo.getPID()
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
		dwPID.setInt(0, (int) processInfo.getPID());
		User32.INSTANCE.EnumWindows((WinDef.HWND hWnd, Pointer data) -> {
			IntByReference dwID = new IntByReference();
			User32.INSTANCE.GetWindowThreadProcessId(hWnd, dwID);

			if (dwID.getValue() == data.getInt(0)) {
				User32.INSTANCE.PostMessage(hWnd, User32.WM_CLOSE, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
				posted.setByte(0, (byte) 1);
			}
			return true;
		}, dwPID);
		com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(hProc);
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
	 * Equivalent to posix SIGKILL.
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
		WinNT.HANDLE hProc = com.sun.jna.platform.win32.Kernel32.INSTANCE.OpenProcess(
			com.sun.jna.platform.win32.Kernel32.PROCESS_TERMINATE,
			false,
			(int) processInfo.getPID()
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
		boolean result = com.sun.jna.platform.win32.Kernel32.INSTANCE.TerminateProcess(hProc, 1);
		com.sun.jna.platform.win32.Kernel32.INSTANCE.CloseHandle(hProc);
		if (LOGGER.isTraceEnabled()) {
			if (result) {
				LOGGER.trace("TerminateProcess performed for process \"{}\" ({})", processInfo.getName(), processInfo.getPID());
			} else {
				LOGGER.trace("TerminateProcess failed for process \"{}\" ({})", processInfo.getName(), processInfo.getPID());
			}
		}
		return result;
	}

}
