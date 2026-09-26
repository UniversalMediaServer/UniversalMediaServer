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
package net.pms.logging;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Emits the latest TRACE message after 300 ms without another call. */
public class DebounceTraceLogger {
	private static final ScheduledThreadPoolExecutor EXECUTOR = createExecutor();
	private final Logger logger;
	private final ScheduledThreadPoolExecutor executor;
	private final long delayMillis;
	private ScheduledFuture<?> pending;
	private long generation;

	public DebounceTraceLogger() {
		this(LoggerFactory.getLogger(DebounceTraceLogger.class), EXECUTOR, 300);
	}

	DebounceTraceLogger(Logger logger, ScheduledThreadPoolExecutor executor, long delayMillis) {
		this.logger = logger;
		this.executor = executor;
		this.delayMillis = delayMillis;
	}

	private static ScheduledThreadPoolExecutor createExecutor() {
		ScheduledThreadPoolExecutor result = new ScheduledThreadPoolExecutor(1, task -> {
			Thread thread = new Thread(task, "Debounced TRACE logger");
			thread.setDaemon(true);
			return thread;
		});
		result.setRemoveOnCancelPolicy(true);
		return result;
	}

	public void log(String logMessage) {
		if (!logger.isTraceEnabled()) {
			return;
		}
		synchronized (this) {
			long requestedGeneration = ++generation;
			if (pending != null) {
				pending.cancel(false);
			}
			pending = executor.schedule(() -> {
				synchronized (this) {
					if (generation == requestedGeneration) {
						pending = null;
						logger.trace(logMessage);
					}
				}
			}, delayMillis, TimeUnit.MILLISECONDS);
		}
	}
}
