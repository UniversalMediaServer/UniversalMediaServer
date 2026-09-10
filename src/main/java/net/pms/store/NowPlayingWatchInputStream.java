package net.pms.store;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Asks the supplier what the stream is playing now.
 */
public class NowPlayingWatchInputStream extends FilterInputStream {

	/** Roughly every few seconds at typical radio bitrates. */
	public static final int DEFAULT_CHECK_INTERVAL = 64 * 1024;

	private final String resourceId;
	private final Supplier<NowPlayingInfo> supplier;
	private final int checkInterval;

	private int bytesUntilCheck;

	public NowPlayingWatchInputStream(InputStream in, String resourceId, Supplier<NowPlayingInfo> supplier) {
		this(in, resourceId, supplier, DEFAULT_CHECK_INTERVAL);
	}

	public NowPlayingWatchInputStream(InputStream in, String resourceId, Supplier<NowPlayingInfo> supplier, int checkInterval) {
		super(in);
		this.resourceId = resourceId;
		this.supplier = supplier;
		this.checkInterval = checkInterval;
		// Report what is playing as soon as the stream starts, not only after the first interval.
		check();
	}

	@Override
	public int read() throws IOException {
		int b = in.read();
		if (b != -1) {
			countAndMaybeCheck(1);
		}
		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int n = in.read(b, off, len);
		if (n > 0) {
			countAndMaybeCheck(n);
		}
		return n;
	}

	private void countAndMaybeCheck(int read) {
		bytesUntilCheck -= read;
		if (bytesUntilCheck <= 0) {
			bytesUntilCheck = checkInterval;
			check();
		}
	}

	private void check() {
		bytesUntilCheck = checkInterval;
		WebStreamNowPlaying.put(resourceId, supplier.get());
	}

	@Override
	public void close() throws IOException {
		try {
			super.close();
		} finally {
			WebStreamNowPlaying.remove(resourceId);
		}
	}
}
