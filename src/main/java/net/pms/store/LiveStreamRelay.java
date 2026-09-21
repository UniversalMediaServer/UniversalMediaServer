package net.pms.store;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(LiveStreamRelay.class.getName());

	// Read size towards the upstream.
	private static final int CHUNK_SIZE = 32 * 1024;

	private static final int BACKLOG_BYTES = 256 * 1024;

	// How far a listener may fall behind before it is dropped.
	private static final int LISTENER_SECONDS = 50;

	// max memory allocation.
	private static final int LISTENER_BYTES_MIN = 256 * 1024;
	private static final int LISTENER_BYTES_MAX = 2 * 1024 * 1024;

	// measure bytes/sec for a period of this long
	private static final long RATE_MEASURE_MS = 5000;

	// How long the upstream is kept running after the last listener left.
	private static final long LINGER_MS = 30000;

	// A listener that is served nothing for this long is at the end of a dead stream.
	private static final long STARVATION_MS = 30000;

	private static final Map<String, Relay> RELAYS = new ConcurrentHashMap<>();

	private LiveStreamRelay() {
	}

	/**
	 * Joins the shared stream, opening it when nobody is listening yet.
	 *
	 * @return NULL when the stream could not be opened
	 */
	public static Listener listen(String key, SourceOpener opener) {
		Listener[] joined = new Listener[1];
		RELAYS.compute(key, (relayKey, running) -> {
			if (running != null) {
				joined[0] = running.addListener();
				if (joined[0] != null) {
					LOGGER.debug("joining the running stream of {}, {} listeners", relayKey, running.listenerCount());
					return running;
				}
			}
			Source source;
			try {
				source = opener.open();
			} catch (IOException e) {
				LOGGER.warn("cannot open the live stream {} : {}", relayKey, e.getMessage());
				LOGGER.trace("", e);
				return null;
			}
			if (source == null || source.stream() == null) {
				return null;
			}
			Relay created = new Relay(relayKey, source);
			joined[0] = created.addListener();
			created.start();
			LOGGER.debug("opened the shared stream of {}", relayKey);
			return created;
		});
		return joined[0];
	}

	public static void closeAll() {
		for (Relay relay : List.copyOf(RELAYS.values())) {
			relay.stop();
		}
	}

	/**
	 * @return how many streams are shared at the moment
	 */
	public static int getStreamCount() {
		return RELAYS.size();
	}

	/**
	 * One renderer's view of a shared stream.
	 */
	public static final class Listener extends InputStream {

		private static final byte[] END_OF_STREAM = new byte[0];

		private final Relay relay;
		private final BlockingQueue<byte[]> chunks = new LinkedBlockingQueue<>();
		private final AtomicInteger queuedBytes = new AtomicInteger();
		private byte[] current;
		private int position;
		private volatile boolean ended;
		private volatile boolean closed;

		private Listener(Relay relay) {
			this.relay = relay;
		}

		/**
		 * @return whether the shared stream knows what it is playing
		 */
		public boolean hasNowPlaying() {
			return relay.source.nowPlaying() != null;
		}

		/**
		 * @return what the shared stream is playing, NULL when unknown
		 */
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
			if (position == current.length) {
				current = null;
			}
			return value;
		}

		@Override
		public int read(byte[] b, int off, int len) throws IOException {
			if (len == 0) {
				return 0;
			}
			if (!advance()) {
				return -1;
			}
			int count = Math.min(len, current.length - position);
			System.arraycopy(current, position, b, off, count);
			position += count;
			if (position == current.length) {
				current = null;
			}
			return count;
		}

		@Override
		public int available() {
			return closed || current == null ? 0 : current.length - position;
		}

		@Override
		public void close() {
			closed = true;
			relay.removeListener(this);
			chunks.clear();
			queuedBytes.set(0);
			chunks.offer(END_OF_STREAM);
		}

		/**
		 * Waits for the next chunk of the shared stream.
		 *
		 * @return false at the end of the stream
		 */
		private boolean advance() throws IOException {
			if (closed) {
				return false;
			}
			if (current != null) {
				return true;
			}
			long waited = 0;
			while (!closed) {
				byte[] next;
				try {
					if (ended && chunks.isEmpty()) {
						return false;
					}
					next = chunks.poll(1, TimeUnit.SECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return false;
				}
				if (closed || next == END_OF_STREAM) {
					return false;
				}
				if (next != null) {
					queuedBytes.addAndGet(-next.length);
					current = next;
					position = 0;
					return true;
				}
				if (ended) {
					// Everything that was queued is served and nothing more will come.
					return false;
				}
				waited += 1000;
				if (waited >= STARVATION_MS) {
					throw new IOException("no data from the shared stream for " + waited + " ms");
				}
			}
			return false;
		}

		private void offer(byte[] chunk) {
			if (closed || ended) {
				return;
			}
			if (queuedBytes.addAndGet(chunk.length) > relay.listenerBudget()) {
				queuedBytes.addAndGet(-chunk.length);
				LOGGER.debug("dropping a listener of {} that does not keep up", relay.key);
				relay.removeListener(this);
				// It still serves what it has queued, then ends.
				endOfStream();
				return;
			}
			chunks.add(chunk);
		}

		private void endOfStream() {
			ended = true;
			// Wake a blocked reader without discarding queued audio. If full, the
			// reader will observe ended once it has drained the queue.
			chunks.offer(END_OF_STREAM);
		}

	}

	/**
	 * The upstream connection and its listeners.
	 */
	private static final class Relay {

		private final String key;
		private final Source source;
		private final long startedAt = System.currentTimeMillis();
		private volatile long pumpedBytes;
		private final List<Listener> listeners = new CopyOnWriteArrayList<>();
		private final Deque<byte[]> backlog = new ArrayDeque<>();
		private int backlogBytes;
		private volatile long emptySince = System.currentTimeMillis();
		private volatile boolean stopped;

		private Relay(String key, Source source) {
			this.key = key;
			this.source = source;
		}

		private void start() {
			Thread.ofVirtual().name("live-stream-relay-" + key).start(this::pump);
		}

		private int listenerCount() {
			return listeners.size();
		}

		/**
		 * @return how many bytes a listener may be behind, from what this stream really delivers
		 */
		private int listenerBudget() {
			long elapsed = System.currentTimeMillis() - startedAt;
			if (elapsed < RATE_MEASURE_MS || pumpedBytes == 0) {
				// The rate is not known yet, so allow what the cap allows.
				return LISTENER_BYTES_MAX;
			}
			long budget = LISTENER_SECONDS * 1000L * pumpedBytes / elapsed;
			return (int) Math.clamp(budget, LISTENER_BYTES_MIN, LISTENER_BYTES_MAX);
		}

		private Listener addListener() {
			synchronized (backlog) {
				if (stopped) {
					return null;
				}
				Listener listener = new Listener(this);
				// Handed over as one block
				byte[] headStart = takeBacklog();
				if (headStart.length > 0) {
					listener.offer(headStart);
				}
				listeners.add(listener);
				return listener;
			}
		}

		/**
		 * @return the kept seconds of stream as one block, empty when there are none yet
		 */
		private byte[] takeBacklog() {
			synchronized (backlog) {
				byte[] all = new byte[backlogBytes];
				int at = 0;
				for (byte[] chunk : backlog) {
					System.arraycopy(chunk, 0, all, at, chunk.length);
					at += chunk.length;
				}
				return all;
			}
		}

		/**
		 * Keeps the last seconds of the stream for the next listener to join.
		 */
		private void remember(byte[] chunk) {
			synchronized (backlog) {
				backlog.addLast(chunk);
				backlogBytes += chunk.length;
				while (backlogBytes > BACKLOG_BYTES && backlog.size() > 1) {
					backlogBytes -= backlog.removeFirst().length;
				}
			}
		}

		private void removeListener(Listener listener) {
			listeners.remove(listener);
			if (listeners.isEmpty()) {
				emptySince = System.currentTimeMillis();
			}
		}

		private void pump() {
			byte[] buffer = new byte[CHUNK_SIZE];
			try {
				while (!stopped) {
					int read = source.stream().read(buffer);
					if (read == -1) {
						LOGGER.debug("the shared stream of {} ended upstream", key);
						break;
					}
					if (read == 0) {
						continue;
					}
					if (listeners.isEmpty()) {
						// Nobody listens: the bytes are dropped, but the session stays up so a renderer
						// that reconnects does not have to open a second one.
						if (System.currentTimeMillis() - emptySince > LINGER_MS) {
							LOGGER.debug("closing the shared stream of {}, no listener for {} ms", key, LINGER_MS);
							break;
						}
						continue;
					}
					pumpedBytes += read;
					byte[] chunk = Arrays.copyOf(buffer, read);
					// Joiners must see each chunk either in their backlog or in live
					// delivery, never in both and never in neither.
					synchronized (backlog) {
						if (stopped) {
							break;
						}
						remember(chunk);
						for (Listener listener : listeners) {
							listener.offer(chunk);
						}
					}
				}
			} catch (IOException e) {
				LOGGER.debug("the shared stream of {} broke : {}", key, e.getMessage());
				LOGGER.trace("", e);
			} finally {
				stop();
			}
		}

		private void stop() {
			synchronized (backlog) {
				stopped = true;
				for (Listener listener : listeners) {
					listener.endOfStream();
				}
				listeners.clear();
				backlog.clear();
				backlogBytes = 0;
			}
			// listen() holds the map entry while joining; never acquire that
			// entry's lock while holding backlog (the opposite lock order).
			RELAYS.remove(key, this);
			try {
				source.stream().close();
			} catch (IOException e) {
				LOGGER.trace("closing the upstream of {}", key, e);
			}
		}

	}
}
