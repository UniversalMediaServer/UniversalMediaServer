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
package net.pms.service.sleep;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An abstract implementation of a {@link SleepManager} sleep worker.
 *
 * @author Nadahar
 */
public abstract class AbstractSleepWorker extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSleepWorker.class);

	/**
	 * An internal state flag that indicates if a system sleep prevention
	 * action has been requested but not executed.
	 */
	protected boolean preventSleep;

	/**
	 * An internal state flag that indicates whether system sleep is
	 * currently being prevented.
	 */
	protected boolean sleepPrevented;

	/**
	 * An internal state flag that indicates if a reset sleep timer action
	 * has been requested but not executed.
	 */
	protected boolean resetSleepTimer;

	/**
	 * An internal state flag that indicates if a system sleep allow
	 * action has been requested but not executed.
	 */
	// protected boolean allowSleep;

	/**
	 * An internal timestamp for when the last sleep action request was
	 * made.
	 */
	protected long lastChange = 0;

	/** Timestamp for when the allow sleep delay will be done. */
	protected long allowSleepTimer = 0;

	/** The current {@link PreventSleepMode} */
	protected PreventSleepMode mode;
	private final SleepManager owner;

	/** Number of milliseconds to delay allowing sleep mode after preventing it */
	protected int getDelayUntilAllowSleep() {
		return 0;
	}

	/**
	 * An abstract constructor that initializes the required fields.
	 *
	 * @param threadName the name of this worker {@link Thread}.
	 * @param mode the current {@link PreventSleepMode}.
	 * @param owner the {@link SleepManager} controlling this worker thread.
	 */
	protected AbstractSleepWorker(String threadName, PreventSleepMode mode, SleepManager owner) {
		super(StringUtils.isBlank(threadName) ? "SleepWorker" : threadName);
		this.mode = mode;
		this.owner = owner;
	}

	/**
	 * Requests that this sleep worker allows the system to sleep at will.
	 */
	public synchronized void allowSleep() {
		preventSleep = false;
		lastChange = System.currentTimeMillis();
		notifyAll();
	}

	/**
	 * Requests that this sleep worker prevents the system from sleeping.
	 */
	public synchronized void preventSleep() {
		preventSleep = true;
		lastChange = System.currentTimeMillis();
		notifyAll();
	}

	/**
	 * Requests that this sleep worker reset the system's idle sleep timer.
	 */
	public synchronized void resetSleepTimer() {
		resetSleepTimer = true;
		lastChange = System.currentTimeMillis();
		notifyAll();
	}

	/**
	 * Sets the {@link PreventSleepMode} for this sleep worker to use.
	 *
	 * @param mode the {@link PreventSleepMode} to use.
	 */
	public synchronized void setMode(PreventSleepMode mode) {
		this.mode = mode;
	}

	@Override
	public synchronized void run() {
		try {
			while (true) {
				while (sleepPrevented && lastChange + 5000 > System.currentTimeMillis()) {
					/*
					 * Wait until there's been 5 seconds after a change before
					 * allowing sleep if it's already prevented. This is to
					 * avoid acting on multiple rapid changes.
					 */
					wait(Math.max(System.currentTimeMillis() - lastChange + 5001, 0));
				}
				if (preventSleep != sleepPrevented) {
					if (preventSleep) {
						doPreventSleep();
					} else {
						// this else block means we should allow sleep, or begin counting down to allow it

						if (getDelayUntilAllowSleep() > 0) {
							if (allowSleepTimer > 0) {
								// the allow sleep timer is active, now let's check whether it has elapsed

								if (System.currentTimeMillis() < allowSleepTimer) {
									// we are still waiting for the allow sleep timer to elapse
									wait(5000);
									continue;
								} else {
									// the timer has elapsed, reset it
									allowSleepTimer = 0;
								}
							} else {
								// set the allow sleep timer if it has not been set
								allowSleepTimer = System.currentTimeMillis() + getDelayUntilAllowSleep();
								continue;
							}
						}

						doAllowSleep();
					}
				} else if (resetSleepTimer) {
					if (!sleepPrevented) {
						doResetSleepTimer();
					} else {
						resetSleepTimer = false;
					}
				} else {
					wait();
				}
			}
		} catch (InterruptedException e) {
			LOGGER.debug("Shutting down sleep worker");
			doAllowSleep();
			owner.clearWorker();
		} catch (Throwable e) {
			LOGGER.error("Unexpected error in SleepManager worker thread, shutting down worker: {}", e.getMessage());
			LOGGER.trace("", e);
		}
	}

	/**
	 * Executes the system call that allows the system to sleep at will.
	 */
	protected abstract void doAllowSleep();

	/**
	 * Executes the system call that prevents the system from sleeping.
	 */
	protected abstract void doPreventSleep();

	/**
	 * Executes a system call to reset the idle sleep timer.
	 */
	protected abstract void doResetSleepTimer();
}

