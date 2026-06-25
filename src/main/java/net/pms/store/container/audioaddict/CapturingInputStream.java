package net.pms.store.container.audioaddict;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.pms.configuration.UmsConfiguration;

/**
 * Diagnostic helper: tees the first {@value #CAPTURE_BYTES} bytes read from a stream into a file
 * (aa-capture-*.bin) in the profile directory, so the actually served AudioAddict stream can be
 * inspected and compared (playlist vs radio vs event) to find what makes a renderer behave
 * differently. Diagnostic only; gated behind {@code audio_addict_capture_stream}.
 */
public class CapturingInputStream extends FilterInputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(CapturingInputStream.class);
	private static final int CAPTURE_BYTES = 64 * 1024;
	private static final AtomicInteger SEQ = new AtomicInteger();

	private OutputStream sink;
	private int remaining;

	private CapturingInputStream(InputStream in, OutputStream sink) {
		super(in);
		this.sink = sink;
		this.remaining = CAPTURE_BYTES;
	}

	/**
	 * Wraps {@code in} so the first chunk read is written to a capture file labelled with
	 * {@code label}; returns {@code in} unchanged if the file cannot be opened.
	 */
	public static InputStream wrap(InputStream in, String label) {
		try {
			String safe = label.replaceAll("[^a-zA-Z0-9._-]", "_");
			String name = "aa-capture-" + safe + "-" + SEQ.incrementAndGet() + ".bin";
			File f = new File(UmsConfiguration.getProfileDirectory(), name);
			OutputStream sink = new BufferedOutputStream(new FileOutputStream(f));
			LOGGER.info("AudioAddict capture: writing first {} bytes of '{}' to {}", CAPTURE_BYTES, label, f.getAbsolutePath());
			return new CapturingInputStream(in, sink);
		} catch (IOException e) {
			LOGGER.warn("AudioAddict capture: could not open capture file for '{}'", label, e);
			return in;
		}
	}

	@Override
	public int read() throws IOException {
		int b = super.read();
		if (b != -1 && sink != null && remaining > 0) {
			try {
				sink.write(b);
				if (--remaining == 0) {
					closeSink();
				}
			} catch (IOException e) {
				closeSink();
			}
		}
		return b;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int n = super.read(b, off, len);
		if (n > 0 && sink != null && remaining > 0) {
			int w = Math.min(n, remaining);
			try {
				sink.write(b, off, w);
				remaining -= w;
				if (remaining == 0) {
					closeSink();
				}
			} catch (IOException e) {
				closeSink();
			}
		}
		return n;
	}

	private void closeSink() {
		if (sink != null) {
			try {
				sink.flush();
				sink.close();
			} catch (IOException e) {
				// ignore
			}
			sink = null;
		}
	}

	@Override
	public void close() throws IOException {
		closeSink();
		super.close();
	}

}
