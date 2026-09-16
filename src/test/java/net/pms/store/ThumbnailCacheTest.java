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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ThumbnailCacheTest {
	private static void await(CountDownLatch latch) {
		try {
			assertTrue(latch.await(10, TimeUnit.SECONDS));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new AssertionError(e);
		}
	}

	@Test
	void cachedHitsAndTemporaryWritesCompleteWhileDatabaseIsBlocked() throws Exception {
		for (boolean writing : new boolean[] {false, true}) {
			var entered = new CountDownLatch(1);
			var release = new CountDownLatch(1);
			Object loaded = new Object();
			var cache = new ThumbnailCache<Object>(id -> {
				entered.countDown();
				await(release);
				return loaded;
			}, value -> {
				entered.countDown();
				await(release);
				return 1L;
			});
			Object hot = new Object();
			Long hotId = cache.put(hot, true);
			try (var workers = Executors.newFixedThreadPool(2)) {
				var slow = workers.submit(() -> writing ? cache.put(loaded, false) : cache.get(1L));
				try {
					await(entered);
					assertSame(hot, workers.submit(() -> cache.get(hotId)).get(5, TimeUnit.SECONDS));
					Long temporary = workers.submit(() -> cache.put(hot, true)).get(5, TimeUnit.SECONDS);
					assertNotEquals(hotId, temporary);
				} finally {
					release.countDown();
				}
				slow.get(5, TimeUnit.SECONDS);
			}
		}
	}

	@Test
	void concurrentRequestsReuseLoadedValue() throws Exception {
		AtomicInteger loads = new AtomicInteger();
		Object value = new Object();
		var cache = new ThumbnailCache<Object>(id -> { loads.incrementAndGet(); return value; }, v -> 1L);
		try (var workers = Executors.newFixedThreadPool(8)) {
			var requests = new java.util.ArrayList<java.util.concurrent.Future<Object>>();
			for (int i = 0; i < 64; i++) {
				requests.add(workers.submit(() -> cache.get(1L)));
			}
			for (var request : requests) {
				assertSame(value, request.get(5, TimeUnit.SECONDS));
			}
		}
		assertEquals(1, loads.get());
	}

	@Test
	void invalidationWaitsForLoadAndPreventsStaleRepopulation() throws Exception {
		var entered = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		AtomicInteger loads = new AtomicInteger();
		AtomicInteger active = new AtomicInteger();
		Object old = new Object();
		Object fresh = new Object();
		var cache = new ThumbnailCache<Object>(id -> {
			if (loads.incrementAndGet() == 1) {
				active.incrementAndGet();
				entered.countDown();
				await(release);
				active.decrementAndGet();
				return old;
			}
			return fresh;
		}, v -> 1L);
		try (var workers = Executors.newFixedThreadPool(2)) {
			var load = workers.submit(() -> cache.get(1L));
			try {
				await(entered);
				var resetStarted = new CountDownLatch(1);
				var reset = workers.submit(() -> {
					resetStarted.countDown();
					cache.invalidate(true, () -> assertEquals(0, active.get()));
				});
				await(resetStarted);
				assertThrows(java.util.concurrent.TimeoutException.class,
					() -> reset.get(150, TimeUnit.MILLISECONDS));
				release.countDown();
				assertSame(old, load.get(5, TimeUnit.SECONDS));
				reset.get(5, TimeUnit.SECONDS);
				assertSame(fresh, cache.get(1L));
			} finally {
				release.countDown();
			}
		}
	}

	@Test
	void nullFailuresAndTemporaryIdResetKeepCacheUsable() {
		AtomicInteger loads = new AtomicInteger();
		Object value = new Object();
		var cache = new ThumbnailCache<Object>(id -> {
			if (loads.incrementAndGet() == 1) { throw new IllegalStateException(); }
			return value;
		}, v -> null);
		assertNull(cache.get(null));
		assertNull(cache.put(null, true));
		assertNull(cache.put(value, false));
		assertThrows(IllegalStateException.class, () -> cache.get(1L));
		assertSame(value, cache.get(1L));
		assertEquals(Long.MAX_VALUE, cache.put(value, true));
		cache.invalidate(false, () -> {});
		assertEquals(Long.MAX_VALUE - 1, cache.put(value, true));
		assertThrows(IllegalStateException.class, () -> cache.invalidate(true, () -> { throw new IllegalStateException(); }));
		assertEquals(Long.MAX_VALUE, cache.put(value, true));
	}
}
