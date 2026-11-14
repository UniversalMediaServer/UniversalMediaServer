package net.pms.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Holds one upstream connection per live stream and delivers it to all its listeners.
 */
public final class LiveStreamRelay {

	@FunctionalInterface
	public interface SourceOpener {
		Source open() throws IOException;
	}

	/**
	 * The bytes of a live stream, free of ICY blocks, and where it says what it is playing.
	 */
	public record Source(InputStream stream, Supplier<NowPlayingInfo> nowPlaying) {
	}

	// Package-visible so failure and watchdog tests do not need real-time radio delays.
	record Timing(long idleMs, long recoveryMs, long lingerMs, long retryMs, long openMs) {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(LiveStreamRelay.class);
	private static final Timing DEFAULT_TIMING = new Timing(10000, 25000, 30000, 250, 10000);
	private static final int BACKLOG_BYTES = 256 * 1024;
	private static final int LISTENER_SECONDS = 50;
	private static final int LISTENER_BYTES_MIN = 256 * 1024;
	private static final int LISTENER_BYTES_MAX = 2 * 1024 * 1024;
	private static final long RATE_MEASURE_MS = 5000;
	private static final long STARVATION_MS = 60000;
	private static final long SKIP_LOG_MS = 5000;
	private static final AtomicLong LISTENER_IDS = new AtomicLong();
	private static final Map<String, Relay> RELAYS = new ConcurrentHashMap<>();

	private LiveStreamRelay() {
	}

	/**
	 * Joins a live stream. Temporary upstream failures are recovered without ending the listener.
	 *
	 * @return NULL when the initial upstream could not be opened
	 */
	public static Listener listen(String key, SourceOpener opener) {
		return listen(key, opener, "unspecified", DEFAULT_TIMING, true);
	}

	public static Listener listen(String key, SourceOpener opener, String client) {
		return listen(key, opener, client, DEFAULT_TIMING, true);
	}

	static Listener listen(String key, SourceOpener opener, String client, Timing timing, boolean reconnect) {
		Listener[] joined = new Listener[1];
		RELAYS.compute(key, (relayKey, running) -> {
			if (running != null) {
				joined[0] = running.addListener(client);
				if (joined[0] != null) {
					return running;
				}
			}
			Source source;
			try {
				source = open(opener, timing.openMs());
			} catch (IOException e) {
				LOGGER.warn("Cannot open live stream {}: {}", displayKey(relayKey), e.getClass().getSimpleName());
				return null;
			}
			if (source == null || source.stream() == null) {
				return null;
			}
			Relay created = new Relay(relayKey, source, opener, timing, reconnect);
			joined[0] = created.addListener(client);
			created.start();
			LOGGER.debug("Opened shared stream {}", created.displayKey);
			return created;
		});
		return joined[0];
	}

	public static void closeAll() {
		for (Relay relay : List.copyOf(RELAYS.values())) {
			relay.stop();
		}
	}

	public static int getStreamCount() {
		return RELAYS.size();
	}

	private static String displayKey(String key) {
		// Station URLs carry subscription tokens. Neither logs nor thread names should expose them.
		int query = key.indexOf('?');
		return query < 0 ? key : key.substring(0, query);
	}

	private static Source open(SourceOpener opener, long timeoutMs) throws IOException {
		CompletableFuture<Source> result = new CompletableFuture<>();
		Thread worker = Thread.ofVirtual().name("live-stream-open").start(() -> {
			try {
				Source opened = opener.open();
				if (!result.complete(opened)) {
					closeAsync(opened);
				}
			} catch (Exception e) {
				result.completeExceptionally(e);
			}
		});
		boolean claimed = false;
		try {
			Source opened = result.get(timeoutMs, TimeUnit.MILLISECONDS);
			claimed = true;
			return opened;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interrupted opening live stream", e);
		} catch (TimeoutException e) {
			throw new IOException("Timed out opening live stream", e);
		} catch (ExecutionException e) {
			throw new IOException("Cannot open live stream", e.getCause());
		} finally {
			// An opener that ignores interruption must close its late result rather than leaking it.
			if (!claimed) {
				result.whenComplete((late, error) -> closeAsync(late));
				result.cancel(false);
				worker.interrupt();
			}
		}
	}

	private static void closeAsync(Source source) {
		if (source != null && source.stream() != null) {
			Thread.ofVirtual().name("live-stream-close").start(() -> {
				try {
					source.stream().close();
				} catch (IOException e) {
					LOGGER.trace("Closing live upstream failed: {}", e.getClass().getSimpleName());
				}
			});
		}
	}

	/**
	 * One HTTP request's view of a shared stream, independent of the renderer's other requests.
	 */
	public static final class Listener extends InputStream {

		private final Relay relay;
		private final long id = LISTENER_IDS.incrementAndGet();
		private final String client;
		private final Deque<byte[]> chunks = new ArrayDeque<>();
		private int queuedBytes;
		private byte[] current;
		private int position;
		private boolean ended;
		private volatile boolean closed;
		private volatile long bytesRead;
		private long skippedBytes;
		private long unreportedSkipped;
		private long lastSkipLog;
		private final long joinedAt = System.nanoTime();
		private volatile long lastRead = joinedAt;

		private Listener(Relay relay, String client) {
			this.relay = relay;
			this.client = client;
		}

		public boolean hasNowPlaying() {
			return relay.source.nowPlaying() != null;
		}

		public NowPlayingInfo getNowPlaying() {
			Supplier<NowPlayingInfo> nowPlaying = relay.source.nowPlaying();
			return nowPlaying == null ? null : nowPlaying.get();
		}

		@Override
		public int read() throws IOException {
			if (!advance()) {
				return -1;
			}
			int value = current[position++] & 0xFF;
			bytesRead++;
			lastRead = System.nanoTime();
			if (position == current.length) {
				current = null;
			}
			return value;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			Objects.checkFromIndexSize(off, len, b.length);
			if (len == 0) {
				return 0;
			}
			if (!advance()) {
				return -1;
			}
			int count = Math.min(len, current.length - position);
			System.arraycopy(current, position, b, off, count);
			position += count;
			bytesRead += count;
			lastRead = System.nanoTime();
			if (position == current.length) {
				current = null;
			}
			return count;
		}

		@Override
		public synchronized int available() {
			byte[] active = current;
			return closed ? 0 : queuedBytes + (active == null ? 0 : active.length - position);
		}

		@Override
		public void close() {
			synchronized (this) {
				if (closed) {
					return;
				}
				logDiagnostics();
				closed = true;
				chunks.clear();
				queuedBytes = 0;
				notifyAll();
			}
			// Never acquire the relay lock while holding a listener lock.
			relay.removeListener(this);
			LOGGER.debug("Closed live listener {} ({}) of {}: read={} bytes, skipped={} bytes",
				id, client, relay.displayKey, bytesRead, skippedBytes);
		}

		private synchronized boolean advance() throws IOException {
			long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(STARVATION_MS);
			while (!closed) {
				if (current != null) {
					return true;
				}
				byte[] next = chunks.pollFirst();
				if (next != null) {
					queuedBytes -= next.length;
					current = next;
					position = 0;
					return true;
				}
				if (ended) {
					return false;
				}
				long remaining = deadline - System.nanoTime();
				if (remaining <= 0) {
					throw new IOException("No data from live relay for " + STARVATION_MS + " ms");
				}
				try {
					TimeUnit.NANOSECONDS.timedWait(this, remaining);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw new IOException("Interrupted waiting for live audio", e);
				}
			}
			return false;
		}

		private synchronized void offer(byte[] chunk) {
			if (closed || ended) {
				return;
			}
			queuedBytes += chunk.length;
			chunks.addLast(chunk);
			int budget = relay.listenerBudget();
			// MP3/ADTS chunks contain whole frames, so skipping cannot split an audio frame.
			while (queuedBytes > budget && chunks.size() > 1) {
				int skipped = chunks.removeFirst().length;
				queuedBytes -= skipped;
				skippedBytes += skipped;
				unreportedSkipped += skipped;
			}
			long now = System.nanoTime();
			if (unreportedSkipped > 0 && now - lastSkipLog >= TimeUnit.MILLISECONDS.toNanos(SKIP_LOG_MS)) {
				LOGGER.debug("Live listener {} ({}) of {} fell behind: skipped={} bytes, queued={} bytes, read={} bytes",
					id, client, relay.displayKey, unreportedSkipped, queuedBytes, bytesRead);
				lastSkipLog = now;
				unreportedSkipped = 0;
			}
			notifyAll();
		}

		private synchronized void logDiagnostics() {
			Session upstream = relay.session;
			long now = System.nanoTime();
			LOGGER.debug("Live listener {} ({}) of {} state: ageMs={} read={} skipped={} queued={} budget={} lastReadMs={} ended={} closed={} upstreamBytes={} upstreamIdleMs={} recovering={} stopped={}",
				id, client, relay.displayKey, TimeUnit.NANOSECONDS.toMillis(now - joinedAt), bytesRead, skippedBytes,
				queuedBytes, relay.listenerBudget(), TimeUnit.NANOSECONDS.toMillis(now - lastRead), ended, closed,
				relay.pumpedBytes, upstream == null ? -1 : TimeUnit.NANOSECONDS.toMillis(now - upstream.lastData), upstream == null && !relay.stopped, relay.stopped);
		}

		private synchronized void endOfStream() {
			ended = true;
			notifyAll();
		}
	}

	private static final class Session {
		private final Source source;
		private volatile long lastData = System.nanoTime();
		private volatile boolean delivered;
		private FutureTask<Void> reader;

		private Session(Source source) {
			this.source = source;
		}
	}

	private static final class Relay {

		private final String key;
		private final String displayKey;
		private final SourceOpener opener;
		private final Timing timing;
		private final boolean reconnect;
		private final long startedAt = System.nanoTime();
		private volatile long pumpedBytes;
		private volatile Source source;
		private final List<Listener> listeners = new CopyOnWriteArrayList<>();
		private final Deque<byte[]> backlog = new ArrayDeque<>();
		private int backlogBytes;
		private volatile long emptySince = System.nanoTime();
		private volatile boolean stopped;
		private volatile Session session;
		private Thread supervisor;

		private Relay(String key, Source source, SourceOpener opener, Timing timing, boolean reconnect) {
			this.key = key;
			this.displayKey = displayKey(key);
			this.source = source;
			this.opener = opener;
			this.timing = timing;
			this.reconnect = reconnect;
		}

		private void start() {
			// Assign before starting so closeAll cannot miss a newly created supervisor.
			supervisor = Thread.ofVirtual().name("live-stream-relay-" + displayKey).unstarted(this::pump);
			supervisor.start();
		}

		private int listenerBudget() {
			long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
			if (elapsed < RATE_MEASURE_MS || pumpedBytes == 0) {
				return LISTENER_BYTES_MAX;
			}
			long budget = LISTENER_SECONDS * 1000L * pumpedBytes / elapsed;
			return (int) Math.clamp(budget, LISTENER_BYTES_MIN, LISTENER_BYTES_MAX);
		}

		private Listener addListener(String client) {
			synchronized (backlog) {
				if (stopped) {
					return null;
				}
				Listener listener = new Listener(this, client);
				for (byte[] chunk : backlog) {
					listener.offer(chunk);
				}
				listeners.add(listener);
				LOGGER.debug("Joined live listener {} ({}) to {}: {} listeners, backlog={} bytes",
					listener.id, client, displayKey, listeners.size(), backlogBytes);
				return listener;
			}
		}

		private void publish(Session current, byte[] chunk) {
			synchronized (backlog) {
				if (stopped || session != current) {
					return;
				}
				current.lastData = System.nanoTime();
				current.delivered = true;
				// Count and remember audio even while there are no listeners during the linger period.
				pumpedBytes += chunk.length;
				backlog.addLast(chunk);
				backlogBytes += chunk.length;
				while (backlogBytes > BACKLOG_BYTES && backlog.size() > 1) {
					backlogBytes -= backlog.removeFirst().length;
				}
				for (Listener listener : listeners) {
					listener.offer(chunk);
				}
			}
		}

		private void removeListener(Listener listener) {
			synchronized (backlog) {
				listeners.remove(listener);
				if (listeners.isEmpty()) {
					emptySince = System.nanoTime();
				}
			}
		}

		private boolean lingerExpired() {
			return listeners.isEmpty() && System.nanoTime() - emptySince >= TimeUnit.MILLISECONDS.toNanos(timing.lingerMs());
		}

		private void startSession(Source opened) {
			Session next = new Session(opened);
			next.reader = new FutureTask<>(() -> {
				LiveAudioFrameReader frames = new LiveAudioFrameReader(opened.stream());
				byte[] chunk;
				while ((chunk = frames.next()) != null) {
					publish(next, chunk);
					if (stopped || session != next) {
						break;
					}
				}
				return null;
			});
			synchronized (backlog) {
				if (stopped) {
					closeAsync(opened);
					return;
				}
				source = opened;
				session = next;
			}
			Thread.ofVirtual().name("live-stream-read-" + displayKey).start(next.reader);
		}

		private void retireSession(Session previous) {
			synchronized (backlog) {
				if (session == previous) {
					session = null;
					// Joiners must not get stale history from a different upstream connection.
					backlog.clear();
					backlogBytes = 0;
				}
			}
			previous.reader.cancel(true);
			closeAsync(previous.source);
		}

		private void pump() {
			long recoverySince = 0;
			long retryDelay = timing.retryMs();
			long nextDiagnostics = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
			try {
				startSession(source);
				while (!stopped && !lingerExpired()) {
					Session current = session;
					if (LOGGER.isDebugEnabled() && System.nanoTime() >= nextDiagnostics) {
						for (Listener listener : listeners) {
							listener.logDiagnostics();
						}
						nextDiagnostics = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
					}
					if (current == null) {
						long remaining = timing.recoveryMs() - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - recoverySince);
						if (remaining <= 0) {
							LOGGER.warn("Live upstream {} did not recover within {} ms", displayKey, timing.recoveryMs());
							break;
						}
						Thread.sleep(Math.min(retryDelay, remaining));
						if (stopped || lingerExpired()) {
							break;
						}
						remaining = timing.recoveryMs() - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - recoverySince);
						if (remaining <= 0) {
							continue;
						}
						try {
							Source opened = open(opener, Math.min(timing.openMs(), remaining));
							if (opened != null && opened.stream() != null) {
								startSession(opened);
								LOGGER.debug("Reopened live upstream {} without ending its listeners", displayKey);
							}
						} catch (IOException e) {
							LOGGER.debug("Retry opening {} failed: {}", displayKey, e.getClass().getSimpleName());
						}
						retryDelay = Math.min(retryDelay * 2, 4000);
						continue;
					}
					if (current.delivered) {
						recoverySince = 0;
						retryDelay = timing.retryMs();
					}
					boolean failed = false;
					try {
						current.reader.get(100, TimeUnit.MILLISECONDS);
						LOGGER.debug("Live upstream {} ended", displayKey);
						failed = true;
					} catch (TimeoutException e) {
						if (System.nanoTime() - current.lastData >= TimeUnit.MILLISECONDS.toNanos(timing.idleMs())) {
							LOGGER.debug("Live upstream {} stalled for {} ms", displayKey, timing.idleMs());
							failed = true;
						}
					} catch (CancellationException e) {
						if (stopped) {
							break;
						}
						failed = true;
					} catch (ExecutionException e) {
						LOGGER.debug("Live upstream {} failed: {}", displayKey, e.getCause().getClass().getSimpleName());
						failed = true;
					}
					if (failed) {
						retireSession(current);
						if (!reconnect) {
							break;
						}
						if (recoverySince == 0) {
							recoverySince = System.nanoTime();
						}
					} else if (recoverySince != 0 && System.nanoTime() - recoverySince >= TimeUnit.MILLISECONDS.toNanos(timing.recoveryMs())) {
						// Opening successfully but never delivering audio must not reset the recovery deadline.
						break;
					}
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				stop();
			}
		}

		private void stop() {
			Session previous;
			synchronized (backlog) {
				if (stopped) {
					return;
				}
				LOGGER.debug("Stopping live relay {}: listeners={} pumpedBytes={} ageMs={} lingerExpired={} interrupted={}",
					displayKey, listeners.size(), pumpedBytes, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt), lingerExpired(), Thread.currentThread().isInterrupted());
				stopped = true;
				previous = session;
				session = null;
				for (Listener listener : listeners) {
					listener.endOfStream();
				}
				listeners.clear();
				backlog.clear();
				backlogBytes = 0;
			}
			// Never acquire the map entry's lock while holding the backlog lock.
			RELAYS.remove(key, this);
			if (previous != null) {
				previous.reader.cancel(true);
				closeAsync(previous.source);
			}
			if (supervisor != null && supervisor != Thread.currentThread()) {
				supervisor.interrupt();
			}
		}
	}
}
