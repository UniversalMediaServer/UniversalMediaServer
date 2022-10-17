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
package net.pms.platform.mac;

import net.pms.Messages;
import net.pms.platform.mac.iokit.IOKitException;
import net.pms.platform.mac.iokit.IOKitUtils;
import net.pms.service.sleep.AbstractSleepWorker;
import net.pms.service.sleep.PreventSleepMode;
import net.pms.service.sleep.SleepManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of a {@link SleepManager} sleep worker for macOS.
 *
 * @author Nadahar
 */
public class MacSleepWorker extends AbstractSleepWorker {
	private static final Logger LOGGER = LoggerFactory.getLogger(MacSleepWorker.class);

	/**
	 * The previously returned prevent system sleep assertion id or -1 if
	 * none has been acquired.
	 */
	protected int preventAssertionId = -1;

	/**
	 * The previously returned user is active assertion id or -1 if none has
	 * been acquired.
	 */
	protected int postponeAssertionId = -1;

	/**
	 * Creates a new {@link MacOsSleepWorker} initialized with the required
	 * arguments.
	 *
	 * @param mode the current {@link PreventSleepMode}.
	 * @param owner the {@link SleepManager} controlling this worker thread.
	 */
	public MacSleepWorker(SleepManager owner, PreventSleepMode mode) {
		super("macOS Sleep Worker", mode, owner);
	}

	@Override
	protected synchronized void doAllowSleep() {
		doResetSleepTimer();
		if (preventAssertionId > 0) {
			try {
				LOGGER.trace("Calling IOKitUtils.enableGoToSleep to stop preventing macOS idle sleep");
				IOKitUtils.enableGoToSleep(preventAssertionId);
				sleepPrevented = false;
			} catch (IOKitException e) {
				preventSleep = sleepPrevented;
				LOGGER.warn("Unable to release idle sleep prevention assertion: {}", e.getMessage());
				LOGGER.trace("", e);
			}
		} else {
			LOGGER.warn("Ignoring attempt to release idle sleep prevention assertion with invalid id ", preventAssertionId);
			preventSleep = sleepPrevented;
		}
	}

	@Override
	protected synchronized void doPreventSleep() {
		try {
			LOGGER.trace("Calling IOKitUtils.disableGoToSleep() to prevent macOS idle sleep");
			preventAssertionId = IOKitUtils.disableGoToSleep(getPreventAssertionName(), getPreventAssertionDetails());
			sleepPrevented = true;
		} catch (IOKitException e) {
			LOGGER.warn("Unable to create idle sleep prevention assertion: {}", e.getMessage());
			LOGGER.trace("", e);
			preventSleep = sleepPrevented;
		}
	}

	@Override
	protected synchronized void doResetSleepTimer() {
		try {
			LOGGER.trace("Calling IOKitUtils.resetSleepTimer() to postpone macOS idle sleep");
			postponeAssertionId = IOKitUtils.resetIdleTimer(Messages.getString("UniversalMediaServerResetIdle"), postponeAssertionId);
		} catch (IOKitException e) {
			LOGGER.warn("Unable to reset idle sleep timer: {}", e.getMessage());
			LOGGER.trace("", e);
			postponeAssertionId = -1;
		}
		resetSleepTimer = false;
	}

	/**
	 * @return The relevant localized sleep prevention assertion name.
	 */
	protected synchronized String getPreventAssertionName() {
		if (mode == PreventSleepMode.PLAYBACK) {
			return Messages.getString("UniversalMediaServerPlaying");
		} else if (mode == PreventSleepMode.RUNNING) {
			return Messages.getString("UniversalMediaServerRunning");
		}
		return "Universal Media Server internal error";
	}

	/**
	 * @return The relevant localized sleep prevention assertion details.
	 */
	protected synchronized String getPreventAssertionDetails() {
		if (mode == PreventSleepMode.PLAYBACK) {
			return Messages.getString("SystemIdleSleepPreventedPlayback");
		} else if (mode == PreventSleepMode.RUNNING) {
			return Messages.getString("SystemIdleSleepPreventedRunning");
		}
		return "A bug in Universal Media Server causes this assertion to exist";
	}
}
