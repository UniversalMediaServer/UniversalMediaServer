package net.pms.store;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a SHOUTcast/Icecast (ICY) stream.
 */
public class IcyMetadataReaderInputStream extends InputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(IcyMetadataReaderInputStream.class.getName());

	private static final String TITLE_PREFIX = "StreamTitle='";

	private final InputStream in;
	private final int metaInt;
	private final Consumer<String> titleListener;

	private volatile String streamTitle;
	private int bytesUntilMeta;

	public IcyMetadataReaderInputStream(InputStream in, int metaInt) {
		this(in, metaInt, null);
	}

	public IcyMetadataReaderInputStream(InputStream in, int metaInt, Consumer<String> titleListener) {
		this.in = in;
		this.metaInt = metaInt;
		this.titleListener = titleListener;
		this.bytesUntilMeta = metaInt;
		LOGGER.debug("ICY: reading upstream metadata every {} bytes", metaInt);
	}

	/**
	 * @return the last title the station announced, or NULL.
	 */
	public String getStreamTitle() {
		return streamTitle;
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
		if (bytesUntilMeta == 0) {
			if (!consumeMetadataBlock()) {
				return -1;
			}
			bytesUntilMeta = metaInt;
		}
		int n = in.read(b, off, Math.min(len, bytesUntilMeta));
		if (n == -1) {
			return -1;
		}
		bytesUntilMeta -= n;
		return n;
	}

	private boolean consumeMetadataBlock() throws IOException {
		int lengthByte = in.read();
		if (lengthByte == -1) {
			return false;
		}
		int length = lengthByte * 16;
		if (length == 0) {
			return true;
		}
		byte[] block;
		try {
			block = in.readNBytes(length);
		} catch (EOFException e) {
			return false;
		}
		if (block.length < length) {
			return false;
		}
		parseStreamTitle(new String(block, StandardCharsets.UTF_8));
		return true;
	}

	private void parseStreamTitle(String block) {
		int start = block.indexOf(TITLE_PREFIX);
		if (start == -1) {
			return;
		}
		start += TITLE_PREFIX.length();
		int end = block.indexOf("';", start);
		if (end == -1) {
			end = block.indexOf('\'', start);
		}
		if (end == -1) {
			return;
		}
		String title = block.substring(start, end).trim();
		if (title.isEmpty()) {
			return;
		}
		if (!title.equals(streamTitle)) {
			LOGGER.debug("ICY: upstream announced StreamTitle='{}'", title);
			streamTitle = title;
			if (titleListener != null) {
				titleListener.accept(title);
			}
			return;
		}
		streamTitle = title;
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
