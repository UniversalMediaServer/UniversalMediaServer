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
package net.pms.store;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Bounds repeated probes of files whose content could not be identified. */
final class MediaParseRetry {
	private static final long RETRY_DELAY = TimeUnit.MINUTES.toNanos(5);
	private static final int MAX_ENTRIES = 1024;
	private final Map<String, Attempt> attempts = new LinkedHashMap<>();
	private final LongSupplier clock;

	MediaParseRetry() {
		this(System::nanoTime);
	}

	MediaParseRetry(LongSupplier clock) {
		this.clock = clock;
	}

	synchronized boolean acquire(File file) {
		String path = file.getAbsolutePath();
		long now = clock.getAsLong();
		long modified = file.lastModified();
		long size = file.length();
		Attempt previous = attempts.get(path);
		if (previous != null && previous.modified == modified && previous.size == size && now - previous.time < RETRY_DELAY) {
			return false;
		}
		attempts.remove(path);
		attempts.put(path, new Attempt(modified, size, now));
		if (attempts.size() > MAX_ENTRIES) {
			attempts.remove(attempts.keySet().iterator().next());
		}
		return true;
	}

	private record Attempt(long modified, long size, long time) { }
}