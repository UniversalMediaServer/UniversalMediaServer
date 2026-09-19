package net.pms.store;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import net.pms.media.MediaStatus;
import org.junit.jupiter.api.Test;

public class MediaStatusStoreConcurrencyTest {
	@Test
	public void concurrentReadsShareOneLoadAndOtherKeysProceed() throws Exception {
		String file = "status-test-" + UUID.randomUUID();
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var calls = new AtomicInteger();
		var expected = new MediaStatus();
		ExecutorService executor = Executors.newFixedThreadPool(3);
		try {
			var first = executor.submit(() -> MediaStatusStore.getMediaStatus(101, file, () -> {
				calls.incrementAndGet();
				started.countDown();
				try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
				catch (InterruptedException e) { throw new AssertionError(e); }
				return expected;
			}));
			assertTrue(started.await(2, TimeUnit.SECONDS));
			var second = executor.submit(() -> MediaStatusStore.getMediaStatus(101, file, () -> {
				calls.incrementAndGet(); return new MediaStatus();
			}));
			assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
			var other = new MediaStatus();
			assertSame(other, executor.submit(() -> MediaStatusStore.getMediaStatus(102, file, () -> other)).get(2, TimeUnit.SECONDS));
			assertNotNull(executor.submit(() -> MediaStatusStore.getMediaStatus(101, file + "-other", MediaStatus::new)).get(2, TimeUnit.SECONDS));
			release.countDown();
			assertSame(expected, first.get(2, TimeUnit.SECONDS));
			assertSame(expected, second.get(2, TimeUnit.SECONDS));
			assertEquals(1, calls.get());
			assertSame(expected, MediaStatusStore.getMediaStatus(101, file, () -> { throw new AssertionError("cache miss"); }));
		} finally {
			release.countDown();
			executor.shutdownNow();
			assertTrue(executor.awaitTermination(3, TimeUnit.SECONDS));
			MediaStatusStore.removeMediaEntry(file);
			MediaStatusStore.removeMediaEntry(file + "-other");
		}
	}

	@Test
	public void failedLoadCanRetryAndAbsentStatusIsCached() {
		String file = "status-test-" + UUID.randomUUID();
		try {
			assertThrows(IllegalStateException.class, () -> MediaStatusStore.getMediaStatus(101, file, () -> { throw new IllegalStateException(); }));
			var empty = MediaStatusStore.getMediaStatus(101, file, () -> null);
			assertNotNull(empty);
			assertSame(empty, MediaStatusStore.getMediaStatus(101, file, () -> { throw new AssertionError(); }));
		} finally {
			MediaStatusStore.removeMediaEntry(file);
		}
	}
}
