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

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.LongFunction;

/** Weak thumbnail cache whose hits do not wait for backing-store I/O. */
final class ThumbnailCache<T> {
	private final ConcurrentHashMap<Long, WeakReference<T>> entries = new ConcurrentHashMap<>();
	// Fairness lets queued invalidation finish even under a steady stream of hits.
	private final ReentrantReadWriteLock lifecycle = new ReentrantReadWriteLock(true);
	// Preserve serialization of database reads, repairs and writes, independently of hits.
	private final Object database = new Object();
	private final AtomicLong temporaryId = new AtomicLong(Long.MAX_VALUE);
	private final LongFunction<T> loader;
	private final Function<T, Long> writer;

	ThumbnailCache(LongFunction<T> loader, Function<T, Long> writer) {
		this.loader = loader;
		this.writer = writer;
	}

	T get(Long id) {
		if (id == null) {
			return null;
		}
		lifecycle.readLock().lock();
		try {
			T value = cached(id);
			if (value != null) {
				return value;
			}
			synchronized (database) {
				value = cached(id);
				if (value == null) {
					value = loader.apply(id);
					if (value != null) {
						entries.put(id, new WeakReference<>(value));
					}
				}
				return value;
			}
		} finally {
			lifecycle.readLock().unlock();
		}
	}

	private T cached(long id) {
		WeakReference<T> reference = entries.get(id);
		return reference == null ? null : reference.get();
	}

	Long put(T value, boolean temporary) {
		if (value == null) {
			return null;
		}
		lifecycle.readLock().lock();
		try {
			if (temporary) {
				long id = temporaryId.getAndDecrement();
				entries.put(id, new WeakReference<>(value));
				return id;
			}
			synchronized (database) {
				Long id = writer.apply(value);
				if (id != null) {
					entries.put(id, new WeakReference<>(value));
				}
				return id;
			}
		} finally {
			lifecycle.readLock().unlock();
		}
	}

	void invalidate(boolean resetTemporaryIds, Runnable action) {
		lifecycle.writeLock().lock();
		try {
			entries.clear();
			if (resetTemporaryIds) {
				temporaryId.set(Long.MAX_VALUE);
			}
			action.run();
		} finally {
			lifecycle.writeLock().unlock();
		}
	}
}
