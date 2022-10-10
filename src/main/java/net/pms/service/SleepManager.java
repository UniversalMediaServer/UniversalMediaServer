/*
 * This file is part of Universal Media Server, based on PS3 Media Server.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.service;

import net.pms.PMS;
import net.pms.platform.PlatformUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages system sleep prevention. It must not be instantiated until
 * {@link PMS#getConfiguration()} and {@link PMS#getRegistry()} have been
 * initialized.
 * <p>
 * This class spawns a worker thread that will do the actual calls to the OS on
 * demand. Once spawned, this thread will be kept alive until {@link #stop()} is
 * called or the worker thread is interrupted. All OS calls are made from the
 * same worker thread. p> Usage is simple: Call {@link #startPlaying()} to
 * indicate to {@link SleepManager} that a playback has started, and call
 * {@link #stopPlaying()} when the playback stops. Any number of "play events"
 * can occur simultaneously, {@link SleepManager} will only allow sleep (if
 * configured to {@link PreventSleepMode#PLAYBACK} when all
 * {@link #startPlaying()} calls has been canceled with corresponding
 * {@link #stopPlaying()} calls. For situations where there are no "play events"
 * (like for images) but you still want to "extend" the time the computer stays
 * awake, call {@link #postponeSleep()}. This will simply reset the system's
 * idle sleep timer so that the timer starts from the beginning. This class is
 * thread-safe so the methods can be called form any thread.
 * <p>
 * This class uses the configured settings for sleep management, and will handle
 * the platform "automatically". Currently only Windows and macOS are supported.
 * On other operating systems no worker thread will be spawned and this class
 * will essentially do nothing.
 * <p>
 * {@link SleepManager} is designed to run as a singleton instance, two
 * instances should not be used at the same time.
 *
 * @author Nadahar
 */
public class SleepManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(SleepManager.class);

	/**
	 * A reference count for incremented and decremented with
	 * {@link #startPlaying()} and {@link #stopPlaying()}
	 */
	protected int playingCount;

	/** The cached value of {@link PmsConfiguration#getPreventSleep()} */
	protected PreventSleepMode mode = PMS.getConfiguration().getPreventSleep();

	/** An internal state flag tracking if sleep is currently prevented */
	protected boolean sleepPrevented;

	/** The attached worker thread */
	protected AbstractSleepWorker worker;

	/**
	 * Creates and starts a {@link SleepManager} instance.
	 */
	public SleepManager() {
		start();
	}

	/**
	 * Registers a start playing event with this {@link SleepManager} and
	 * prevents the system from sleeping if configured and necessary.
	 */
	public synchronized void startPlaying() {
		playingCount++;
		if (isPreventSleepSupported() && !sleepPrevented && mode == PreventSleepMode.PLAYBACK) {
			preventSleep();
		}
	}

	/**
	 * Registers a stop playing event with this {@link SleepManager} and allows
	 * the system to sleep again if configured and necessary.
	 */
	public synchronized void stopPlaying() {
		if (playingCount == 0) {
			LOGGER.error("Sleepmanager cannot decrease playing reference count as it's already zero");
			return;
		}
		playingCount--;
		if (isPreventSleepSupported() && playingCount == 0 && sleepPrevented && mode == PreventSleepMode.PLAYBACK) {
			allowSleep();
			/*
			 * Between ES_SYSTEM_REQUIRED|ES_CONTINUOUS and ES_CONTINUOUS the Windows does not go to sleep.
			 * But both do not restart the system idle timer. In worst case Windows goes
			 * to sleep short after ES_CONTINUOUS. To avoid this the system idle timer gets restarted here.
			 */
			postponeSleep();
		}
	}

	/**
	 * Resets the system sleep timer if configured and necessary.
	 */
	public synchronized void postponeSleep() {
		if (isPreventSleepSupported() && mode == PreventSleepMode.PLAYBACK && !sleepPrevented) {
			resetSleepTimer();
		}
	}

	/**
	 * @return The current "playing" reference count value.
	 */
	public synchronized int getPlayingCount() {
		return playingCount;
	}

	/**
	 * Sets a new {@link PreventSleepMode} for this {@link SleepManager}. If
	 * {@code mode} equals the current mode, no action is taken.
	 *
	 * @param mode the new {@link PreventSleepMode}.
	 */
	public synchronized void setMode(PreventSleepMode mode) {
		if (mode == null) {
			LOGGER.error("Ignoring attempt to set PreventSleepMode to null");
			return;
		}

		if (this.mode != mode) {
			this.mode = mode;
			switch (mode) {
				case NEVER -> {
					if (sleepPrevented) {
						allowSleep();
					}
				}
				case PLAYBACK -> {
					if (playingCount > 0 && !sleepPrevented) {
						preventSleep();
					} else if (playingCount == 0 && sleepPrevented) {
						allowSleep();
					}
				}
				case RUNNING -> {
					if (!sleepPrevented) {
						preventSleep();
					}
				}
				default -> throw new IllegalStateException("PreventSleepMode value not implemented: " + mode.getValue());
			}
			if (worker != null) {
				worker.setMode(mode);
			}
		}
	}

	/**
	 * Starts the {@link SleepManager}. This will be called automatically in the
	 * constructor, and need only be called if {@link #stop()} has been called
	 * previously.
	 */
	public final synchronized void start() {
		LOGGER.debug("Starting SleepManager");
		if (isPreventSleepSupported()) {
			if (mode == PreventSleepMode.RUNNING || mode == PreventSleepMode.PLAYBACK && playingCount > 0) {
				preventSleep();
			}
		} else {
			LOGGER.debug("SleepManager doesn't support current platform");
		}
	}

	/**
	 * Stops the {@link SleepManager}. This will cause its worker thread to
	 * terminate and any sleep mode prevention to be cancelled.
	 */
	public void stop() {
		LOGGER.debug("Stopping SleepManager");
		AbstractSleepWorker localWorker;
		synchronized (this) {
			localWorker = worker;
		}
		if (localWorker != null) {
			localWorker.interrupt();
			try {
				localWorker.join();
			} catch (InterruptedException e) {
				LOGGER.debug("SleepManager was interrupted while waiting for the sleep worker to terminate");
			}
		}
	}

	/**
	 * Convenience method that calls {@link #stop()} and then {@link #start()}.
	 */
	public synchronized void restart() {
		stop();
		start();
	}

	/**
	 * For internal use only, instructs the sleep worker to allow the system to
	 * sleep at will.
	 */
	protected synchronized void allowSleep() {
		initializeWorker();
		if (worker != null) {
			worker.allowSleep();
		}
		sleepPrevented = false;
	}

	/**
	 * For internal use only, instructs the sleep worker to prevent system sleep
	 * until further notice.
	 */
	protected synchronized void preventSleep() {
		initializeWorker();
		if (worker != null) {
			worker.preventSleep();
		}
		sleepPrevented = true;
	}

	/**
	 * For internal use only, instructs the sleep worker to reset the system
	 * idle timer.
	 */
	protected synchronized void resetSleepTimer() {
		initializeWorker();
		if (worker != null) {
			worker.resetSleepTimer();
		}
	}

	/**
	 * For internal use only, creates and starts a sleep worker of the correct
	 * type if it doesn't already exist.
	 */
	protected void initializeWorker() {
		if (worker == null && isPreventSleepSupported()) {
			worker = PlatformUtils.INSTANCE.getSleepWorker(this, mode);
			worker.start();
		}
	}

	/**
	 * For internal use only, detaches the worker thread.
	 */
	protected synchronized void clearWorker() {
		worker = null;
	}

	/**
	 * Checks whether system sleep prevention is supported for the current
	 * {@link PlatformUtils}.
	 *
	 * @return {@code true} if system sleep prevention is supported,
	 *         {@code false} otherwise.
	 */
	public static boolean isPreventSleepSupported() {
		return PlatformUtils.INSTANCE.isPreventSleepSupported();
	}
}
