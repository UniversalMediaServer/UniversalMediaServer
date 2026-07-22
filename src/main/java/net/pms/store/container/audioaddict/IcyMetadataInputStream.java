package net.pms.store.container.audioaddict;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an audio InputStream and interleaves SHOUTcast/Icecast (ICY) metadata blocks
 * every "metaInt" bytes, as requested by renderers via "Icy-MetaData: 1". Each block
 * carries "StreamTitle='...';" with the current title from "titleSupplier"; a single
 * zero byte is sent when the title has not changed (or is unknown), as the protocol requires.
 */
public class IcyMetadataInputStream extends InputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(IcyMetadataInputStream.class.getName());

	// A length byte of 0 means "no metadata in this block".
	private static final byte[] NO_METADATA = {0};
	// The length byte counts 16-byte units, so a single block holds at most 255 * 16 bytes.
	private static final int MAX_BLOCKS = 255;

	private final InputStream in;
	private final int metaInt;
	private final Supplier<String> titleSupplier;

	private int bytesUntilMeta;
	private byte[] pendingMeta;
	private int pendingMetaPos;
	private String lastTitle = "";

	public IcyMetadataInputStream(InputStream in, int metaInt, Supplier<String> titleSupplier) {
		this.in = in;
		this.metaInt = metaInt;
		this.titleSupplier = titleSupplier;
		this.bytesUntilMeta = metaInt;
		LOGGER.info("ICY: new IcyMetadataInputStream(in={}, metaInt={}, titleSupplier={})", in, metaInt, titleSupplier);
	}

	@Override
	public int read() throws IOException {
		byte[] one = new byte[1];
		int n = read(one, 0, 1);
		return n == -1 ? -1 : (one[0] & 0xFF);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (len == 0) {
			return 0;
		}
		// Emit a pending metadata block before any further audio.
		if (pendingMeta != null) {
			int n = Math.min(len, pendingMeta.length - pendingMetaPos);
			System.arraycopy(pendingMeta, pendingMetaPos, b, off, n);
			pendingMetaPos += n;
			if (pendingMetaPos == pendingMeta.length) {
				pendingMeta = null;
				pendingMetaPos = 0;
				bytesUntilMeta = metaInt;
			}
			return n;
		}
		// Reached a metadata boundary: build the next block and emit it on the next pass.
		if (bytesUntilMeta == 0) {
			pendingMeta = buildMetadataBlock();
			pendingMetaPos = 0;
			return read(b, off, len);
		}
		// Otherwise pass through audio up to the next boundary.
		int n = in.read(b, off, Math.min(len, bytesUntilMeta));
		if (n == -1) {
			return -1;
		}
		bytesUntilMeta -= n;
		return n;
	}

	private byte[] buildMetadataBlock() {
		String title = titleSupplier.get();
		// A null title means "unknown / unchanged" - keep whatever the renderer already shows.
		if (title == null || title.equals(lastTitle)) {
			LOGGER.trace("ICY: no-change metadata block (0 byte) after {} audio bytes", metaInt);
			return NO_METADATA;
		}
		lastTitle = title;
		// Single quotes would terminate the quoted value, so drop them.
		String value = "StreamTitle='" + title.replace("'", "") + "';";
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		int blocks = Math.min(MAX_BLOCKS, (bytes.length + 15) / 16);
		int length = blocks * 16;
		byte[] block = new byte[1 + length];
		block[0] = (byte) blocks;
		System.arraycopy(bytes, 0, block, 1, Math.min(bytes.length, length));
		LOGGER.debug("ICY: injected metadata block -> StreamTitle='{}' ({} payload bytes, {} x 16-byte block(s))", title, bytes.length, blocks);
		return block;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
