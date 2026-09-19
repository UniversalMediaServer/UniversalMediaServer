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
package net.pms.media.video.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Publishes translations without making a browsing thread wait for a lookup. */
final class BackgroundTranslations<T> {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundTranslations.class);
	private static final Executor WORKER = new ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS,
		new ArrayBlockingQueue<>(256), task -> {
			Thread thread = new Thread(task, "Localized metadata worker");
			thread.setDaemon(true);
			return thread;
		});
	private final Executor executor;
	private final LongSupplier clock;
	private final Map<String, T> values = new HashMap<>();
	private final Set<String> pending = new HashSet<>();
	private final Map<String, Long> failures = new HashMap<>();
	private long generation;
	private long version;

	BackgroundTranslations() {
		this(WORKER, System::nanoTime);
	}

	BackgroundTranslations(Executor executor, LongSupplier clock) {
		this.executor = executor;
		this.clock = clock;
	}

	private static String key(String language) {
		return language.toLowerCase(Locale.ROOT);
	}

	synchronized T get(String language) {
		return language == null ? null : values.get(key(language));
	}

	synchronized long version() {
		return version;
	}

	synchronized void set(Map<String, T> translations) {
		generation++;
		values.clear();
		pending.clear();
		failures.clear();
		if (translations != null) {
			translations.forEach((language, value) -> values.put(key(language), value));
		}
		version++;
	}

	synchronized void request(String language, Supplier<T> lookup) {
		String key = key(language);
		Long failedAt = failures.get(key);
		if (values.containsKey(key) || pending.contains(key) ||
			(failedAt != null && clock.getAsLong() - failedAt < TimeUnit.MINUTES.toNanos(1))) {
			return;
		}
		long requestedGeneration = generation;
		pending.add(key);
		try {
			executor.execute(() -> {
				T value = null;
				try {
					value = lookup.get();
				} catch (Exception e) {
					LOGGER.debug("Localized metadata lookup failed for {}", key, e);
				} finally {
					synchronized (this) {
						if (generation == requestedGeneration) {
							pending.remove(key);
							if (value != null) {
								values.put(key, value);
								version++;
							} else {
								failures.put(key, clock.getAsLong());
							}
						}
					}
				}
			});
		} catch (RejectedExecutionException e) {
			// A full queue must never run network work on the caller or block browsing.
			pending.remove(key);
		}
	}
}
