package net.pms.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LiveStreamRelayTest {
	private static final class Source extends InputStream {
		private final LinkedBlockingQueue<byte[]> data = new LinkedBlockingQueue<>();
		private final AtomicInteger reads = new AtomicInteger();
		private final CountDownLatch closed = new CountDownLatch(1);
		private final java.util.concurrent.CountDownLatch eofRead = new java.util.concurrent.CountDownLatch(1);
		@Override
		public int read() { throw new AssertionError("Relay must use bulk reads"); }
		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			try {
				reads.incrementAndGet();
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
		public void close() { closed.countDown(); data.offer(new byte[0]); }
	}

	private static final LiveStreamRelay.Timing TEST_TIMING = new LiveStreamRelay.Timing(2000, 2000, 1000, 10, 500);

	private static LiveStreamRelay.Listener listenOnce(String key, LiveStreamRelay.SourceOpener opener) {
		return LiveStreamRelay.listen(key, opener, "test", TEST_TIMING, false);
	}

	@AfterEach
	public void closeRelays() {
		LiveStreamRelay.closeAll();
	}

	private static byte[] audio(int value, int length) {
		byte[] bytes = new byte[length];
		Arrays.fill(bytes, (byte) value);
		return bytes;
	}

	private static void await(java.util.function.BooleanSupplier condition) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
		while (!condition.getAsBoolean()) {
			assertTrue(System.nanoTime() < deadline, "Condition did not become true");
			Thread.sleep(5);
		}
	}

	@Test
	public void recoversUpstreamEofWithoutEndingTheIcyConnection() throws Exception {
		Source first = new Source();
		Source second = new Source();
		AtomicInteger opens = new AtomicInteger();
		try (var listener = LiveStreamRelay.listen(UUID.randomUUID().toString(),
			() -> new LiveStreamRelay.Source(opens.getAndIncrement() == 0 ? first : second,
				() -> NowPlayingInfo.ofStreamTitle(opens.get() == 1 ? "First" : "Second")),
			"icy-test", TEST_TIMING, true);
			var icy = new IcyMetadataInputStream(listener, 128, () -> listener.getNowPlaying().streamTitle)) {
			first.data.add(audio(1, 8192));
			assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertArrayEquals(audio(1, 8192), readIcyAudio(icy, 8192)));
			first.close();
			await(() -> opens.get() == 2);
			second.data.add(audio(2, 8192));
			assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertArrayEquals(audio(2, 8192), readIcyAudio(icy, 8192)));
			assertEquals("Second", listener.getNowPlaying().streamTitle);
			assertEquals(2, opens.get(), "Recovery shares the new upstream, keeping the same listener and ICY wrapper");
		} finally {
			first.close();
			second.close();
		}
	}

	private static byte[] readIcyAudio(InputStream icy, int length) throws IOException {
		java.io.ByteArrayOutputStream audio = new java.io.ByteArrayOutputStream();
		while (audio.size() < length) {
			byte[] block = icy.readNBytes(128);
			assertEquals(128, block.length);
			audio.write(block);
			int blocks = icy.read();
			assertTrue(blocks >= 0, "ICY connection must stay open");
			assertEquals(blocks * 16, icy.readNBytes(blocks * 16).length);
		}
		return audio.toByteArray();
	}

	@Test
	public void recoversAnUpstreamReadException() throws Exception {
		Source replacement = new Source();
		AtomicInteger opens = new AtomicInteger();
		InputStream failing = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("upstream reset");
			}
		};
		try (var listener = LiveStreamRelay.listen(UUID.randomUUID().toString(),
			() -> new LiveStreamRelay.Source(opens.getAndIncrement() == 0 ? failing : replacement, null),
			"test", TEST_TIMING, true)) {
			replacement.data.add(audio(7, 8192));
			assertTimeoutPreemptively(Duration.ofSeconds(3), () -> assertArrayEquals(audio(7, 8192), listener.readNBytes(8192)));
			assertEquals(2, opens.get());
		} finally {
			replacement.close();
		}
	}

	@Test
	public void silentUpstreamIsReplacedEvenWhenItsReadIgnoresInterruption() throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch closed = new CountDownLatch(1);
		InputStream stalled = new InputStream() {
			@Override
			public int read() {
				throw new AssertionError("bulk read required");
			}
			@Override
			public int read(byte[] bytes, int off, int len) {
				boolean interrupted = false;
				while (true) {
					try {
						release.await();
						break;
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
				Arrays.fill(bytes, off, off + len, (byte) 99);
				return len;
			}
			@Override
			public void close() {
				closed.countDown();
			}
		};
		Source replacement = new Source();
		AtomicInteger opens = new AtomicInteger();
		var timing = new LiveStreamRelay.Timing(100, 1500, 1000, 10, 500);
		try (var listener = LiveStreamRelay.listen(UUID.randomUUID().toString(),
			() -> new LiveStreamRelay.Source(opens.getAndIncrement() == 0 ? stalled : replacement, null),
			"test", timing, true)) {
			assertTrue(closed.await(2, TimeUnit.SECONDS), "Watchdog must close the stalled upstream");
			await(() -> opens.get() == 2);
			release.countDown();
			replacement.data.add(audio(3, 8192));
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertArrayEquals(audio(3, 8192), listener.readNBytes(8192)));
		} finally {
			release.countDown();
			replacement.close();
		}
	}

	@Test
	public void endlessEmptyReconnectsHaveABoundedRecoveryDeadline() throws Exception {
		AtomicInteger opens = new AtomicInteger();
		var timing = new LiveStreamRelay.Timing(100, 300, 1000, 10, 100);
		try (var listener = LiveStreamRelay.listen(UUID.randomUUID().toString(), () -> {
			opens.incrementAndGet();
			return new LiveStreamRelay.Source(new ByteArrayInputStream(new byte[0]), null);
		}, "test", timing, true)) {
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertEquals(-1, listener.read()));
			assertTrue(opens.get() > 1);
		}
	}

	@Test
	public void reconnectingListenerGetsFreshHistoryFromTheLingerPeriod() throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		try (var first = listenOnce(key, () -> new LiveStreamRelay.Source(source, null))) {
			source.data.add(audio(1, 8192));
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertArrayEquals(audio(1, 8192), first.readNBytes(8192)));
			first.close();
			for (int i = 0; i < 40; i++) {
				source.data.add(audio(2, 8192));
			}
			await(() -> source.reads.get() >= 42);
			try (var joined = listenOnce(key, () -> { throw new AssertionError("Rejoin must reuse upstream"); })) {
				assertArrayEquals(audio(2, 256 * 1024), joined.readNBytes(256 * 1024));
			}
		} finally {
			source.close();
		}
	}

	@Test
	public void lingerExpiresWhileTheUpstreamReadIsBlocked() throws Exception {
		Source source = new Source();
		var timing = new LiveStreamRelay.Timing(10000, 1000, 100, 10, 500);
		var listener = LiveStreamRelay.listen(UUID.randomUUID().toString(), () -> new LiveStreamRelay.Source(source, null),
			"test", timing, true);
		listener.close();
		await(() -> LiveStreamRelay.getStreamCount() == 0);
		assertTrue(source.closed.await(2, TimeUnit.SECONDS), "Blocked read must be closed when linger expires");
	}

	@Test
	public void overflowSkipsWholeMp3FramesEvenWithFragmentedUpstreamReads() throws Exception {
		java.io.ByteArrayOutputStream upstream = new java.io.ByteArrayOutputStream();
		int frames = 6000;
		for (int i = 0; i < frames; i++) {
			upstream.write(mp3Frame(i));
		}
		InputStream fragmented = new java.io.FilterInputStream(new ByteArrayInputStream(upstream.toByteArray())) {
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return in.read(b, off, Math.min(113, len));
			}
		};
		try (var listener = listenOnce(UUID.randomUUID().toString(), () -> new LiveStreamRelay.Source(fragmented, null))) {
			await(() -> LiveStreamRelay.getStreamCount() == 0);
			byte[] received = listener.readAllBytes();
			assertEquals(0, received.length % 417);
			int first = ((received[4] & 255) << 8) | (received[5] & 255);
			assertTrue(first > 0, "Overflow must trim old frames");
			for (int i = 0; i < received.length / 417; i++) {
				assertArrayEquals(mp3Frame(first + i), Arrays.copyOfRange(received, i * 417, (i + 1) * 417));
			}
			assertEquals(frames, first + received.length / 417);
		}
	}

	private static byte[] mp3Frame(int sequence) {
		byte[] frame = audio(3, 417);
		frame[0] = (byte) 0xFF;
		frame[1] = (byte) 0xFB;
		frame[2] = (byte) 0x90;
		frame[3] = 0;
		frame[4] = (byte) (sequence >>> 8);
		frame[5] = (byte) sequence;
		return frame;
	}

	@Test
	public void aTimedOutOpenerClosesItsLateSource() throws Exception {
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch lateClosed = new CountDownLatch(1);
		AtomicInteger opens = new AtomicInteger();
		InputStream late = new ByteArrayInputStream(new byte[0]) {
			@Override
			public void close() {
				lateClosed.countDown();
			}
		};
		var timing = new LiveStreamRelay.Timing(100, 250, 1000, 10, 50);
		try (var listener = LiveStreamRelay.listen(UUID.randomUUID().toString(), () -> {
			int attempt = opens.incrementAndGet();
			if (attempt == 1) {
				return new LiveStreamRelay.Source(new ByteArrayInputStream(new byte[0]), null);
			}
			if (attempt == 2) {
				boolean interrupted = false;
				while (true) {
					try {
						release.await();
						break;
					} catch (InterruptedException e) {
						interrupted = true;
					}
				}
				if (interrupted) {
					Thread.currentThread().interrupt();
				}
				return new LiveStreamRelay.Source(late, null);
			}
			throw new IOException("station unavailable");
		}, "test", timing, true)) {
			assertTimeoutPreemptively(Duration.ofSeconds(2), () -> assertEquals(-1, listener.read()));
			release.countDown();
			assertTrue(lateClosed.await(2, TimeUnit.SECONDS));
		} finally {
			release.countDown();
		}
	}

	@Test
	public void sharesAudioAndPreservesUnsignedSingleByteReads() throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		try (var first = listenOnce(key, () -> new LiveStreamRelay.Source(source, null));
			var second = listenOnce(key, () -> { throw new AssertionError("Second upstream"); })) {
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
	public void everyQueuedChunkIsServedBeforeEof() throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		try (var listener = listenOnce(key, () -> new LiveStreamRelay.Source(source, null))) {
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
	public void aListenerThatFallsBehindSkipsTowardsLiveInsteadOfEnding() throws Exception {
		Source source = new Source();
		String key = UUID.randomUUID().toString();
		int chunkSize = 32 * 1024;
		int chunks = 80;
		try (var listener = listenOnce(key, () -> new LiveStreamRelay.Source(source, null))) {
			for (int i = 0; i < chunks; i++) {
				byte[] chunk = new byte[chunkSize];
				java.util.Arrays.fill(chunk, (byte) i);
				source.data.add(chunk);
			}
			source.close();
			assertTrue(source.eofRead.await(3, TimeUnit.SECONDS));
			byte[] served = listener.readAllBytes();
			assertEquals(0, served.length % chunkSize, "Chunks are served whole");
			assertTrue(served.length < chunks * chunkSize, "The oldest audio of a listener that never reads is skipped");
			int first = served[0];
			assertTrue(first > 0, "It does not start at the beginning any more");
			for (int i = 0; i < served.length; i++) {
				assertEquals((byte) (first + i / chunkSize), served[i], "What is served is contiguous and in order");
			}
			assertEquals((byte) (chunks - 1), served[served.length - 1], "It plays on to the live edge");
			assertTimeoutPreemptively(java.time.Duration.ofMillis(500), () -> assertEquals(-1, listener.read()));
		} finally { source.close(); }
	}

	@Test
	public void concurrentJoinersReceiveEveryByteExactlyOnce() throws Exception {
		for (int round = 0; round < 30; round++) {
			Source source = new Source();
			String key = UUID.randomUUID().toString();
			var joined = new java.util.ArrayList<LiveStreamRelay.Listener>();
			try (var first = listenOnce(key, () -> new LiveStreamRelay.Source(source, null));
				var workers = Executors.newFixedThreadPool(9)) {
				var start = new java.util.concurrent.CountDownLatch(1);
				var futures = new java.util.ArrayList<java.util.concurrent.Future<LiveStreamRelay.Listener>>();
				for (int i = 0; i < 8; i++) {
					futures.add(workers.submit(() -> {
						start.await();
						return listenOnce(key, () -> { throw new AssertionError("Second upstream"); });
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
		try (var listener = listenOnce(key, () -> new LiveStreamRelay.Source(source, null));
			var other = listenOnce(key, () -> { throw new AssertionError("Second upstream"); });
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
					// Supply enough audio for the codec sniff while the upstream remains open.
					source.data.add(audio(23, 8192));
					assertEquals(23, other.read());
				}
			} finally { listener.close(); source.close(); }
		} finally { source.close(); }
	}
}
