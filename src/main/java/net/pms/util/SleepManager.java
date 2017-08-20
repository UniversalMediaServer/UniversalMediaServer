package net.pms.util;

import static org.apache.commons.lang3.StringUtils.isBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.jna.Platform;
import net.pms.Messages;
import net.pms.PMS;
import net.pms.configuration.PmsConfiguration;
import net.pms.io.WinUtils.Kernel32;
import net.pms.util.jna.macos.iokit.IOKitException;
import net.pms.util.jna.macos.iokit.IOKitUtils;

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
				case NEVER:
					if (sleepPrevented) {
						allowSleep();
					}
					break;
				case PLAYBACK:
					if (playingCount > 0 && !sleepPrevented) {
						preventSleep();
					} else if (playingCount == 0 && sleepPrevented) {
						allowSleep();
					}
					break;
				case RUNNING:
					if (!sleepPrevented) {
						preventSleep();
					}
					break;
				default:
					throw new IllegalStateException("PreventSleepMode value not implemented: " + mode.getValue());
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
	public synchronized void start() {
		LOGGER.debug("Starting SleepManager");
		if (Platform.isWindows() || Platform.isMac()) {
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
	public synchronized void stop() {
		LOGGER.debug("Stopping SleepManager");
		if (worker != null) {
			worker.interrupt();
			try {
				worker.join();
			} catch (InterruptedException e) {
				LOGGER.debug("SleepManager was interrupted while waiting for worker to terminate");
			}
			worker = null;
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
	protected void allowSleep() {
		initializeWorker();
		worker.allowSleep();
		sleepPrevented = false;
	}

	/**
	 * For internal use only, instructs the sleep worker to prevent system sleep
	 * until further notice.
	 */
	protected void preventSleep() {
		initializeWorker();
		worker.preventSleep();
		sleepPrevented = true;
	}

	/**
	 * For internal use only, instructs the sleep worker to reset the system
	 * idle timer.
	 */
	protected void resetSleepTimer() {
		initializeWorker();
		worker.resetSleepTimer();
	}

	/**
	 * For internal use only, creates and starts a sleep worker of the correct
	 * type if it doesn't already exist.
	 *
	 * @throws IllegalStateException If no {@link AbstractSleepWorker}
	 *             implementation is available for this {@link Platform}.
	 */
	protected void initializeWorker() {
		if (worker == null && isPreventSleepSupported()) {
			if (Platform.isWindows()) {
				worker = new WindowsSleepWorker(this, mode);
			} else if (Platform.isMac()) {
				worker = new MacOsSleepWorker(this, mode);
			} else {
				throw new IllegalStateException("Missing SleepWorker implementation for current platform");
			}
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
	 * {@link Platform}.
	 *
	 * @return {@code true} if system sleep prevention is supported,
	 *         {@code false} otherwise.
	 */
	public static boolean isPreventSleepSupported() {
		return Platform.isWindows() || Platform.isMac() && IOKitUtils.isMacOsVersionEqualOrGreater(5, 0);
	}

	/**
	 * An abstract implementation of a {@link SleepManager} sleep worker.
	 *
	 * @author Nadahar
	 */
	protected abstract static class AbstractSleepWorker extends Thread {

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
		 * An internal timestamp for when the last sleep action request was
		 * made.
		 */
		protected long lastChange = 0;

		/** The current {@link PreventSleepMode} */
		protected PreventSleepMode mode;
		private final SleepManager owner;

		/**
		 * An abstract constructor that initializes the required fields.
		 *
		 * @param threadName the name of this worker {@link Thread}.
		 * @param mode the current {@link PreventSleepMode}.
		 * @param owner the {@link SleepManager} controlling this worker thread.
		 */
		public AbstractSleepWorker(String threadName, PreventSleepMode mode, SleepManager owner) {
			super(isBlank(threadName) ? "SleepWorker" : threadName);
			this.mode = mode;
			this.owner = owner;
		}

		/**
		 * Requests that this sleep worker allows the system to sleep at will.
		 */
		public synchronized void allowSleep() {
			preventSleep = false;
			lastChange = System.currentTimeMillis();
			notify();
		}

		/**
		 * Requests that this sleep worker prevents the system from sleeping.
		 */
		public synchronized void preventSleep() {
			preventSleep = true;
			lastChange = System.currentTimeMillis();
			notify();
		}

		/**
		 * Requests that this sleep worker reset the system's idle sleep timer.
		 */
		public synchronized void resetSleepTimer() {
			resetSleepTimer = true;
			lastChange = System.currentTimeMillis();
			notify();
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
						// Wait until there's been 5 seconds after a change
						// before
						// allowing sleep if it's already prevented. This is to
						// avoid acting on multiple rapid changes.
						wait(Math.max(System.currentTimeMillis() - lastChange + 5001, 0));
					}
					if (preventSleep != sleepPrevented) {
						if (preventSleep) {
							doPreventSleep();
						} else {
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

	/**
	 * An implementation of a {@link SleepManager} sleep worker for Windows.
	 *
	 * @author Nadahar
	 */
	protected static class WindowsSleepWorker extends AbstractSleepWorker {

		private static final Logger LOGGER = LoggerFactory.getLogger(WindowsSleepWorker.class);

		/** A static reference to the Windows {@link Kernel32} instance */
		protected static final Kernel32 KERNEL32 = Kernel32.INSTANCE;

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
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_CONTINUOUS);

			sleepPrevented = false;
		}

		@Override
		protected synchronized void doPreventSleep() {
			LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED to prevent Windows from going to sleep");
			KERNEL32.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED | Kernel32.ES_CONTINUOUS);

			sleepPrevented = true;
		}

		@Override
		protected synchronized void doResetSleepTimer() {
			LOGGER.trace("Calling SetThreadExecutionState ES_SYSTEM_REQUIRED to reset the Windows sleep timer");
			Kernel32.INSTANCE.SetThreadExecutionState(Kernel32.ES_SYSTEM_REQUIRED);

			resetSleepTimer = false;
		}
	}

	/**
	 * An implementation of a {@link SleepManager} sleep worker for macOS.
	 *
	 * @author Nadahar
	 */
	protected static class MacOsSleepWorker extends AbstractSleepWorker {

		private static final Logger LOGGER = LoggerFactory.getLogger(MacOsSleepWorker.class);

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
		public MacOsSleepWorker(SleepManager owner, PreventSleepMode mode) {
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
				postponeAssertionId = IOKitUtils.resetIdleTimer(Messages.getString("SleepManager.PostponeSleepName"), postponeAssertionId);
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
				return Messages.getString("SleepManager.PreventSleepPlaybackName");
			} else if (mode == PreventSleepMode.RUNNING) {
				return Messages.getString("SleepManager.PreventSleepRunningName");
			}
			return "Universal Media Server internal error";
		}

		/**
		 * @return The relevant localized sleep prevention assertion details.
		 */
		protected synchronized String getPreventAssertionDetails() {
			if (mode == PreventSleepMode.PLAYBACK) {
				return Messages.getString("SleepManager.PreventSleepPlaybackDetails");
			} else if (mode == PreventSleepMode.RUNNING) {
				return Messages.getString("SleepManager.PreventSleepRunningDetails");
			}
			return "A bug in Universal Media Server causes this assertion to exist";
		}
	}
}
