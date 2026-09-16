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
package net.pms.util;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Waits for stable files without occupying the workers that parse them. */
public final class SettledFileQueue {
	private static final Logger LOGGER = LoggerFactory.getLogger(SettledFileQueue.class);
	private final Set<String> pending = ConcurrentHashMap.newKeySet();
	private final ScheduledExecutorService scheduler;
	private final Executor workers;
	private final Predicate<File> isLocked;
	private final long intervalMillis;
	private final int maxWaits;

	public SettledFileQueue(ScheduledExecutorService scheduler, Executor workers,
			Predicate<File> isLocked, long intervalMillis, int maxWaits) {
		this.scheduler = scheduler;
		this.workers = workers;
		this.isLocked = isLocked;
		this.intervalMillis = intervalMillis;
		this.maxWaits = maxWaits;
	}

	/** Coalesces events while a file is waiting, queued or being parsed. */
	public void submit(File file, Runnable action) {
		String filename = file.getAbsolutePath();
		if (!pending.add(filename)) {
			return;
		}
		try {
			scheduler.execute(new Check(file, filename, action));
		} catch (RuntimeException | Error e) {
			pending.remove(filename);
			throw e;
		}
	}

	private final class Check implements Runnable {
		private final File file;
		private final String filename;
		private final Runnable action;
		private boolean initialized;
		private long size;
		private long modified;
		private int waits;

		private Check(File file, String filename, Runnable action) {
			this.file = file;
			this.filename = filename;
			this.action = action;
		}

		@Override
		public void run() {
			try {
				if (!file.exists()) {
					pending.remove(filename);
					return;
				}
				long currentSize = file.length();
				long currentModified = file.lastModified();
				if (!initialized || currentSize != size || currentModified != modified || isLocked.test(file)) {
					if (initialized && waits++ >= maxWaits) {
						LOGGER.debug("Giving up waiting for file {} to be fully written", filename);
						pending.remove(filename);
						return;
					}
					initialized = true;
					size = currentSize;
					modified = currentModified;
					scheduler.schedule(this, intervalMillis, TimeUnit.MILLISECONDS);
					return;
				}
				workers.execute(() -> {
					boolean release = true;
					try {
						// The file may have changed while waiting for a parser worker.
						if (file.length() == size && file.lastModified() == modified && !isLocked.test(file)) {
							action.run();
							if (file.length() == size && file.lastModified() == modified) {
								return;
							}
						}
						// Keep ownership and settle again if it changed before or during parsing.
						initialized = false;
						waits = 0;
						scheduler.execute(this);
						release = false;
					} finally {
						if (release) {
							pending.remove(filename);
						}
					}
				});
			} catch (RuntimeException | Error e) {
				pending.remove(filename);
				LOGGER.warn("Unable to process file {}", filename, e);
				throw e;
			}
		}
	}
}
