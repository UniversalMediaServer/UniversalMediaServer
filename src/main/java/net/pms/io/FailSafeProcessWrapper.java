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
package net.pms.io;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Interrupt the worker thread upon timeout
 */
public class FailSafeProcessWrapper extends TimerTask {

	private final ProcessWrapperImpl pw;
	private final Timer timer;
	private final long delay;
	private final Object failureLock = new Object();
	private boolean failure;

	/**
	 * @param delay delay in milliseconds before task is to be stopped.
	 */
	public FailSafeProcessWrapper(ProcessWrapperImpl pw, long delay) {
		this.pw = pw;
		this.timer = new Timer();
		this.delay = delay;
	}

	public void runInSameThread() {
		setFailure(false);
		timer.schedule(this, delay);
		pw.runInSameThread();
	}

	private void setFailure(boolean value) {
		synchronized (failureLock) {
			failure = value;
		}
	}

	public boolean hasFail() {
		synchronized (failureLock) {
			return failure;
		}
	}

	@Override
	public void run() {
		if (pw != null && pw.isAlive()) {
			setFailure(true);
			pw.stopProcess();
		}
		timer.cancel();
	}

}
