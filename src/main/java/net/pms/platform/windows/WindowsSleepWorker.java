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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
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

	private static ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	/**
	 * When this is active, it means we are in the process of
	 * counting down from 5 minutes before we let Windows 11
	 * resume its normal sleep activity. This is to work
	 * around different logic in Windows 11 vs previous versions
	 * of Windows.
	 *
	 * @see https://github.com/UniversalMediaServer/UniversalMediaServer/issues/3883
	 */
	private static ScheduledFuture futureSleep = null;

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
	protected synchronized void doAllowSleep() {
		LOGGER.trace("Calling SetThreadExecutionState ES_CONTINUOUS to allow Windows to go to sleep");
		Runnable allowSleepRunner = () -> {
			LOGGER.trace("Windows can sleep now");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);
			sleepPrevented = false;
		};

		// Windows 11 identifies itself as 10.0.0, FFS...
		if (WindowsUtils.getOSVersion().isGreaterThanOrEqualTo("10.0.0")) {
			/*
			 * Future enhancement could make this delay the same
			 * as the configured Windows sleep delay, for now 5 minutes
			 * seems better than 0.
			 */
			futureSleep = executorService.schedule(allowSleepRunner, 5, TimeUnit.MINUTES);
			LOGGER.trace("Windows will sleep in 5 minutes unless cancelled");
		} else {
			allowSleep();
		}
	}

	@Override
	protected synchronized void doPreventSleep() {
		LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED to prevent Windows from going to sleep");
		Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_CONTINUOUS);
		sleepPrevented = true;
		if (futureSleep != null) {
			futureSleep.cancel(true);
		}
	}

	@Override
	protected synchronized void doResetSleepTimer() {
		LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED to reset the Windows sleep timer");
		Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED);
		resetSleepTimer = false;
		if (futureSleep != null) {
			futureSleep.cancel(true);
		}
	}
}
