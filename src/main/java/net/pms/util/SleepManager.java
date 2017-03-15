package net.pms.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.PMS;
import net.pms.configuration.PreventSleepMode;


public class SleepManager {

	private final static Logger LOGGER = LoggerFactory.getLogger(SleepManager.class);
	protected int playingCount;
	protected PreventSleepMode mode = PMS.getConfiguration().getPreventSleep();
	protected boolean sleepDisabled;

	public SleepManager() {
		if (mode == PreventSleepMode.RUNNING) {
			preventSleep();
		}
	}

	public synchronized void startPlaying() {
		if (playingCount++ == 0) {
			preventSleep();
		}
	}

	public synchronized void stopPlaying() {
		if (playingCount-- > 0) {
			if (playingCount == 0) {
				allowSleep();
			}
		} else {
			throw new IllegalStateException("Cannot reduce playing reference count as it's already zero");
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

	protected void allowSleep() {
		PMS.get().getRegistry().reenableGoToSleep();
		sleepDisabled = false;
	}

	protected void preventSleep() {
		PMS.get().getRegistry().disableGoToSleep();
		sleepDisabled = true;
	}

	protected void updateMode(PreventSleepMode mode) {
		if (mode == null) {
			throw new NullPointerException("mode cannot be null");
		}

		if (this.mode != mode) {
			this.mode = mode;
			switch (mode) {
				case NEVER:
					if (sleepDisabled) {
						allowSleep();
					}
					break;
				case PLAYBACK:
					if (playingCount > 0 && !sleepDisabled) {
						preventSleep();
					} else if (playingCount == 0 && sleepDisabled) {
						allowSleep();
					}
					break;
				case RUNNING:
					if (!sleepDisabled) {
						preventSleep();
					}
					break;
				default:
					throw new IllegalStateException("PreventSleepMode value not implemented: " + mode.getValue());
			}
		}
	}
}
