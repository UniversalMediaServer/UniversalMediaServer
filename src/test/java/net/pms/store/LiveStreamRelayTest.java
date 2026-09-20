package net.pms.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LiveStreamRelayTest {
	private static final class Source extends InputStream {
		private final LinkedBlockingQueue<byte[]> data = new LinkedBlockingQueue<>();
		private final java.util.concurrent.CountDownLatch eofRead = new java.util.concurrent.CountDownLatch(1);
		@Override
		public int read() { throw new AssertionError("Relay must use bulk reads"); }
		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			try {
				byte[] next = data.take();
				if (next.length == 0) { eofRead.countDown(); return -1; }
				assertTrue(next.length <= len);
				System.arraycopy(next, 0, buffer, off, next.length);
				return next.length;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException(e);
			}
		}
		@Override
		public void close() { data.offer(new byte[0]); }
	}

	@Test
	public void sharesAudioAndPreservesUnsignedSingleByteReads() throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		try (var first = LiveStreamRelay.listen(key, () -> new LiveStreamRelay.Source(source, null));
			var second = LiveStreamRelay.listen(key, () -> { throw new AssertionError("Second upstream"); })) {
			byte[] audio = {0, 127, (byte) 128, (byte) 255, 42};
			source.data.add(audio);
			source.close();
			assertArrayEquals(audio, first.readNBytes(audio.length));
			for (byte value : audio) { assertEquals(value & 255, second.read()); }
			assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> {
				assertEquals(-1, first.read());
				assertEquals(-1, second.read());
				assertEquals(-1, second.read());
			});
		} finally { source.close(); }
	}

	@Test
	public void fullQueueDrainsBeforeEof() throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		try (var listener = LiveStreamRelay.listen(key, () -> new LiveStreamRelay.Source(source, null))) {
			byte[] expected = new byte[64];
			for (int i = 0; i < expected.length; i++) {
				expected[i] = (byte) i;
				source.data.add(new byte[]{(byte) i});
			}
			source.close();
			assertTrue(source.eofRead.await(3, TimeUnit.SECONDS));
			assertArrayEquals(expected, listener.readNBytes(expected.length));
			assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> assertEquals(-1, listener.read()));
		} finally { source.close(); }
	}

	@Test
	public void concurrentJoinersReceiveEveryByteExactlyOnce() throws Exception {
		for (int round = 0; round < 30; round++) {
			Source source = new Source();
			String key = UUID.randomUUID().toString();
			var joined = new java.util.ArrayList<LiveStreamRelay.Listener>();
			try (var first = LiveStreamRelay.listen(key, () -> new LiveStreamRelay.Source(source, null));
				var workers = Executors.newFixedThreadPool(9)) {
				var start = new java.util.concurrent.CountDownLatch(1);
				var futures = new java.util.ArrayList<java.util.concurrent.Future<LiveStreamRelay.Listener>>();
				for (int i = 0; i < 8; i++) {
					futures.add(workers.submit(() -> {
						start.await();
						return LiveStreamRelay.listen(key, () -> { throw new AssertionError("Second upstream"); });
					}));
				}
				byte[] expected = new byte[40];
				for (int i = 0; i < expected.length; i++) { expected[i] = (byte) i; }
				var producer = workers.submit(() -> {
					start.await();
					for (byte value : expected) { source.data.add(new byte[]{value}); Thread.yield(); }
					return null;
				});
				start.countDown();
				for (var future : futures) { joined.add(future.get(3, TimeUnit.SECONDS)); }
				producer.get(3, TimeUnit.SECONDS);
				source.close();
				assertArrayEquals(expected, first.readAllBytes());
				for (var listener : joined) { assertArrayEquals(expected, listener.readAllBytes()); }
			} finally {
				for (var listener : joined) { listener.close(); }
				source.close();
			}
		}
	}

	@Test
	public void upstreamEndWakesWaitingListener() throws Exception {
		assertReaderWakes(false);
	}

	@Test
	public void closingListenerWakesReaderWithoutClosingUpstream() throws Exception {
		assertReaderWakes(true);
	}

	private static void assertReaderWakes(boolean closeListener) throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		try (var listener = LiveStreamRelay.listen(key, () -> new LiveStreamRelay.Source(source, null));
			var other = LiveStreamRelay.listen(key, () -> { throw new AssertionError("Second upstream"); });
			var worker = Executors.newSingleThreadExecutor()) {
			AtomicReference<Thread> reader = new AtomicReference<>();
			var result = worker.submit(() -> { reader.set(Thread.currentThread()); return listener.read(); });
			try {
				long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
				while (reader.get() == null || reader.get().getState() != Thread.State.TIMED_WAITING) {
					assertTrue(System.nanoTime() < deadline, "Reader did not enter queue wait");
					Thread.sleep(1);
				}
				if (closeListener) { listener.close(); } else { source.close(); }
				assertEquals(-1, result.get(500, TimeUnit.MILLISECONDS));
				if (closeListener) {
					source.data.add(new byte[]{23});
					assertEquals(23, other.read());
				}
			} finally { listener.close(); source.close(); }
		} finally { source.close(); }
	}
}
