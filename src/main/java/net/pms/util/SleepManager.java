package net.pms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.PreventSleepMode;

/**
 * This class manages OS sleep mode prevention. It must not be created until
 * {@link PMS#getConfiguration()} and {@link PMS#getRegistry()} have been
 * initialized.
 * <p>
 * This class spawns a worker thread that will do the actual calls to the OS on
 * demand. Once spawned, this thread will be kept alive until {@link #stop()} is
 * called or the worker thread is interrupted, so that all calls are made from
 * the same thread.
 *
 * @author Nadahar
 */
public class SleepManager {

	private final static Logger LOGGER = LoggerFactory.getLogger(SleepManager.class);
	protected int playingCount;
	protected PreventSleepMode mode = PMS.getConfiguration().getPreventSleep();
	protected boolean sleepPrevented;
	protected SleepWorker worker;

	public SleepManager() {
		start();
	}

	/**
	 * Registers a start playing event with the {@link SleepManager} and
	 * prevents the OS from sleeping if configured and necessary.
	 */
	public synchronized void startPlaying() {
		playingCount++;
		if (!sleepPrevented && mode == PreventSleepMode.PLAYBACK) {
			preventSleep();
		}
	}

	/**
	 * Registers a stop playing event with the {@link SleepManager} and allows
	 * the OS to sleep again if configured and necessary.
	 */
	public synchronized void stopPlaying() {
		if (playingCount == 0) {
			throw new IllegalStateException("Cannot decrease playing reference count as it's already zero");
		}
		playingCount--;
		if (playingCount == 0 && sleepPrevented && mode == PreventSleepMode.PLAYBACK) {
			allowSleep();
		}
	}

	public synchronized void postponeSleep() {
		if (mode == PreventSleepMode.PLAYBACK && !sleepPrevented) {
			resetSleepTimer();
		}
	}

	public synchronized int getPlayingCount() {
		return playingCount;
	}

	public synchronized void setMode(PreventSleepMode mode) {
		if (mode == null) {
			LOGGER.debug("Disregarding attempt to set PreventSleepMode to null");
			return;
		}

		updateMode(mode);
	}

	/**
	 * Starts the {@link SleepManager}. This will be called automatically in the
	 * constructor, and need only be called if {@link #stop()} has been called
	 * previously.
	 */
	public synchronized void start() {
		LOGGER.debug("Starting SleepManager");
		if (mode == PreventSleepMode.RUNNING || mode == PreventSleepMode.PLAYBACK && playingCount > 0) {
			preventSleep();
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

	protected void allowSleep() {
		initializeWorker();
		worker.allowSleep();
		sleepPrevented = false;
	}

	protected void preventSleep() {
		initializeWorker();
		worker.preventSleep();
		sleepPrevented = true;
	}

	protected void resetSleepTimer() {
		initializeWorker();
		worker.resetSleepTimer();
	}

	protected void initializeWorker() {
		if (worker == null) {
			worker = new SleepWorker(this);
			worker.start();
		}
	}

	protected synchronized void clearWorker() {
		worker = null;
	}

	protected void updateMode(PreventSleepMode mode) {
		if (mode == null) {
			throw new NullPointerException("mode cannot be null");
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
		}
	}

	protected static class SleepWorker extends Thread {

		private final static Logger LOGGER = LoggerFactory.getLogger(SleepWorker.class);
		protected boolean preventSleep;
		protected boolean sleepPrevented;
		protected boolean resetSleepTimer;
		protected long lastChange = 0;
		private final SleepManager owner;

		public SleepWorker(SleepManager owner) {
			super("SleepWorker");
			this.owner = owner;
		}

		public synchronized void allowSleep() {
			preventSleep = false;
			lastChange = System.currentTimeMillis();
			notify();
		}

		public synchronized void preventSleep() {
			preventSleep = true;
			lastChange = System.currentTimeMillis();
			notify();
		}

		public synchronized void resetSleepTimer() {
			resetSleepTimer = true;
			lastChange = System.currentTimeMillis();
			notify();
		}

		@Override
		public synchronized void run() {
			try {
				while (true) {
					while (sleepPrevented && lastChange + 5000 > System.currentTimeMillis()) {
						// Wait until there's been 5 seconds after a change before
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

		protected void doAllowSleep() {
			PMS.get().getRegistry().reenableGoToSleep();
			sleepPrevented = false;
		}

		protected void doPreventSleep() {
			PMS.get().getRegistry().disableGoToSleep();
			sleepPrevented = true;
		}

		protected void doResetSleepTimer() {
			PMS.get().getRegistry().resetSleepTimer();
			resetSleepTimer = false;
		}
	}
}
