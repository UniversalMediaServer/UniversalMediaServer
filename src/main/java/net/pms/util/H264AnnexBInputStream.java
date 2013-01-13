package net.pms.util;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class H264AnnexBInputStream extends InputStream {
	private static final Logger LOGGER = LoggerFactory.getLogger(H264AnnexBInputStream.class);
	private InputStream source;
	private int nextTarget;
	private boolean firstHeader;
	private byte header[];
	//private int remaining;

	public H264AnnexBInputStream(InputStream source, byte header[]) {
		this.source = source;
		this.header = header;
		firstHeader = true;
		nextTarget = -1;
	}

	@Override
	public int read() throws IOException {
		return -1;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		byte h[] = null;
		boolean insertHeader = false;

		if (nextTarget == -1) {
			h = getArray(4);
			if (h == null) {
				return -1;
			}
			nextTarget = 65536 * 256 * (h[0] & 0xff) + 65536 * (h[1] & 0xff) + 256 * (h[2] & 0xff) + (h[3] & 0xff);
			h = getArray(3);
			if (h == null) {
				return -1;
			}
			insertHeader = ((h[0] & 37) == 37 && (h[1] & -120) == -120);
			if (!insertHeader) {
				System.arraycopy(new byte[]{0, 0, 0, 1}, 0, b, off, 4);
				off += 4;

			}
			nextTarget = nextTarget - 3;
		}

		if (nextTarget == -1) {
			return -1;
		}

		if (insertHeader) {
			byte defHeader[] = header;
			if (!firstHeader) {
				defHeader = new byte[header.length + 1];
				System.arraycopy(header, 0, defHeader, 0, header.length);
				defHeader[defHeader.length - 1] = 1;
				defHeader[defHeader.length - 2] = 0;
			}
			if (defHeader.length < (len - off)) {
				System.arraycopy(defHeader, 0, b, off, defHeader.length);
				off += defHeader.length;
			} else {
				System.arraycopy(defHeader, 0, b, off, (len - off));
				off = len;
			}
			//LOGGER.info("header inserted / nextTarget: " + nextTarget);
			firstHeader = false;
		}

		if (h != null) {
			System.arraycopy(h, 0, b, off, 3);
			off += 3;
			//LOGGER.info("frame start inserted");
		}

		if (nextTarget < (len - off)) {

			h = getArray(nextTarget);
			if (h == null) {
				return -1;
			}
			System.arraycopy(h, 0, b, off, nextTarget);
			//LOGGER.info("Frame copied: " + nextTarget);
			off += nextTarget;

			nextTarget = -1;

		} else {

			h = getArray(len - off);
			if (h == null) {
				return -1;
			}
			System.arraycopy(h, 0, b, off, (len - off));
			//LOGGER.info("Frame copied: " + (len - off));
			nextTarget = nextTarget - (len - off);
			off = len;

		}

		return off;
	}

	private byte[] getArray(int length) throws IOException {
		if (length < 0) {
			LOGGER.trace("Negative array ?");
			return null;
		}
		byte bb[] = new byte[length];
		int n = source.read(bb);
		if (n == -1) {
			return null;
		}
		while (n < length) {
			int u = source.read(bb, n, length - n);
			if (u == -1) {
				break;
			}
			n += u;
		}
		return bb;
	}

	@Override
	public void close() throws IOException {
		super.close();
		if (source != null) {
			source.close();
		}
	}
}
