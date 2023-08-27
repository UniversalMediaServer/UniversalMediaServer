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

import com.sun.jna.platform.win32.Kernel32;
import net.pms.service.sleep.AbstractSleepWorker;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a {@link SleepManager} sleep worker for Windows.
 */
public class WindowsSleepWorker extends AbstractSleepWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(WindowsSleepWorker.class);
	private static final int FIFTEEN_MINUTES_IN_MILLISECONDS = 15 * 60 * 1000;

	/**
	 * Creates a new {@link WindowsSleepWorker} initialized with the
	 * required arguments.
	 *
	 * @param mode the current {@link PreventSleepMode}.
	 * @param owner the {@link SleepManager} controlling this worker thread.
	 */
	public WindowsSleepWorker(SleepManager owner, PreventSleepMode mode) {
		super("Windows Sleep Worker", mode, owner);
	}

	@Override
	protected int getDelayUntilAllowSleep() {
		return WindowsUtils.isVersionThatSleepsImmediately() ? FIFTEEN_MINUTES_IN_MILLISECONDS : 0;
	}

	@Override
	protected synchronized void doAllowSleep() {
		LOGGER.trace("Calling SetThreadExecutionState ES_CONTINUOUS to allow Windows to go to sleep");
		Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
		sleepPrevented = false;
	}

	@Override
	protected synchronized void doPreventSleep() {
		LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED to prevent Windows from going to sleep");
		Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_CONTINUOUS);
		sleepPrevented = true;
	}

	@Override
	protected synchronized void doResetSleepTimer() {
		LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED to reset the Windows sleep timer");
		Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED);
		resetSleepTimer = false;
	}
}
