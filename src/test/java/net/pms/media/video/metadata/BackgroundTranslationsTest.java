package net.pms.media.video.metadata;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

public class BackgroundTranslationsTest {
	@Test
	public void deduplicatesAndPublishes() {
		Queue<Runnable> jobs = new ArrayDeque<>();
		BackgroundTranslations<String> cache = new BackgroundTranslations<>(jobs::add, () -> 0);
		for (int i = 0; i < 100; i++) {
			cache.request("CS", () -> "Cesky");
		}
		assertNull(cache.get("cs"));
		assertEquals(1, jobs.size());
		jobs.remove().run();
		assertEquals("Cesky", cache.get("cs"));
		assertEquals(1, cache.version());
		cache.request("cs", () -> fail("cached translation must be reused"));
		assertTrue(jobs.isEmpty());
	}

	@Test
	public void failuresRetryAfterCooldownAndOldResultsCannotReplaceNewData() {
		Queue<Runnable> jobs = new ArrayDeque<>();
		AtomicLong time = new AtomicLong();
		BackgroundTranslations<String> cache = new BackgroundTranslations<>(jobs::add, time::get);
		cache.request("cs", () -> { throw new IllegalStateException("offline"); });
		jobs.remove().run();
		cache.request("cs", () -> "retry");
		assertTrue(jobs.isEmpty());
		time.set(TimeUnit.MINUTES.toNanos(2));
		cache.request("cs", () -> "stale");
		cache.set(Map.of("CS", "new"));
		jobs.remove().run();
		assertEquals("new", cache.get("cs"));
	}

	@Test
	public void wholeBurstOfPublishedTranslationsSchedulesOneStoreRefresh() {
		Queue<Runnable> jobs = new ArrayDeque<>();
		var target = new TranslationStoreRefresh.Target(1L, null);
		BackgroundTranslations<String> first = new BackgroundTranslations<>(() -> target, jobs::add, () -> 0);
		BackgroundTranslations<String> second = new BackgroundTranslations<>(() -> target, jobs::add, () -> 0);
		try {
			first.request("cs", () -> "Cesky");
			first.request("de", () -> "Deutsch");
			second.request("cs", () -> "Cesky");
			assertEquals(3, jobs.size());
			// Execute all three lookups, leaving only their shared flush.
			for (int i = 0; i < 3; i++) {
				jobs.remove().run();
			}
			assertEquals("Cesky", second.get("cs"));
			assertEquals(1, jobs.size());
			jobs.remove().run();
			TranslationStoreRefresh.request(target, jobs::add);
			assertEquals(1, jobs.size(), "A completed flush must allow another batch");
		} finally {
			while (!jobs.isEmpty()) {
				jobs.remove().run();
			}
		}
	}

	@Test
	public void rejectedQueueCanRetryWithoutRunningOnCaller() {
		BackgroundTranslations<String> cache = new BackgroundTranslations<>(job -> {
			throw new RejectedExecutionException();
		}, () -> 0);
		assertDoesNotThrow(() -> cache.request("cs", () -> fail("must not run inline")));
		assertNull(cache.get("cs"));
	}

	@Test
	public void blockedLookupDoesNotBlockReaderOrDuplicateRequest() throws Exception {
		var worker = Executors.newSingleThreadExecutor();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		BackgroundTranslations<String> cache = new BackgroundTranslations<>(worker, System::nanoTime);
		try {
			cache.request("cs", () -> {
				started.countDown();
				try {
					if (!release.await(5, TimeUnit.SECONDS)) {
						throw new IllegalStateException("lookup timed out");
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException(e);
				}
				return "done";
			});
			assertTrue(started.await(5, TimeUnit.SECONDS));
			assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () -> {
				assertNull(cache.get("cs"));
				cache.request("cs", () -> fail("duplicate"));
			});
		} finally {
			release.countDown();
			worker.shutdown();
			assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
		}
		assertEquals("done", cache.get("cs"));
	}
}
