package net.pms.external.umsapi;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.PMS;
import net.pms.configuration.UmsConfiguration;
import net.pms.media.MediaInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class APIUtilsQueueTest {
	@BeforeAll
	public static void setup() throws Exception {
		System.setProperty(PMS.PROPERTY_RUNNING_TESTS, "true");
		PMS.setConfiguration(new UmsConfiguration(false));
	}

	@Test
	public void coalescesConcurrentRequestsUntilCompletion() throws Exception {
		MediaInfo media = new MediaInfo();
		var queued = new ConcurrentLinkedQueue<Runnable>();
		AtomicInteger runs = new AtomicInteger();
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Runnable lookup = () -> {
			runs.incrementAndGet();
			entered.countDown();
			try {
				assertTrue(release.await(5, TimeUnit.SECONDS));
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new AssertionError(ex);
			}
		};
		try (var callers = Executors.newFixedThreadPool(8)) {
			var results = new ArrayList<Future<Boolean>>();
			for (int i = 0; i < 24; i++) {
				results.add(callers.submit(() -> APIUtils.executeMetadataLookup(media, lookup, queued::add)));
			}
			int accepted = 0;
			for (Future<Boolean> result : results) {
				if (result.get(5, TimeUnit.SECONDS)) {
					accepted++;
				}
			}
			assertEquals(1, accepted);
			assertEquals(1, queued.size());
			Future<?> running = callers.submit(queued.remove());
			try {
				assertTrue(entered.await(5, TimeUnit.SECONDS));
				assertFalse(APIUtils.executeMetadataLookup(media, lookup, queued::add));
				assertTrue(APIUtils.executeMetadataLookup(new MediaInfo(), runs::incrementAndGet, Runnable::run));
			} finally {
				release.countDown();
			}
			running.get(5, TimeUnit.SECONDS);
			assertTrue(APIUtils.executeMetadataLookup(media, runs::incrementAndGet, Runnable::run));
			assertEquals(3, runs.get());
		} finally {
			release.countDown();
		}
	}

	@Test
	public void permitsRetryAfterRejectionAndFailure() {
		MediaInfo media = new MediaInfo();
		AtomicInteger runs = new AtomicInteger();
		assertThrows(RejectedExecutionException.class, () -> APIUtils.executeMetadataLookup(media,
			runs::incrementAndGet, task -> { throw new RejectedExecutionException(); }));
		assertTrue(APIUtils.executeMetadataLookup(media, runs::incrementAndGet, Runnable::run));
		assertThrows(IllegalStateException.class, () -> APIUtils.executeMetadataLookup(media,
			() -> { throw new IllegalStateException(); }, Runnable::run));
		assertTrue(APIUtils.executeMetadataLookup(media, runs::incrementAndGet, Runnable::run));
		assertEquals(2, runs.get());
	}
}
