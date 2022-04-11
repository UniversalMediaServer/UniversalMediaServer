/*
 * Universal Media Server, for streaming any media to DLNA
 * compatible renderers based on the http://www.ps3mediaserver.org.
 * Copyright (C) 2012 UMS developers.
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; version 2
 * of the License only.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.pms.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * This is an {@link InputStream} implementation that stores everything that is
 * read in memory. This is useful for capturing what has been read from the
 * stream for example for logging, but will consume as much memory as has been
 * read. Use in moderation and only when needed.
 * <p>
 * The maximum size that can be read is
 * {@link ByteArrayOutputStream#MAX_ARRAY_SIZE}, which is
 * {@link Integer#MAX_VALUE} - 8. Reading past this will result in an
 * {@link OutOfMemoryError}, although available memory might be exhausted before
 * that point depending on the allocated heap size.
 *
 * @author Nadahar
 */
public class LoggableInputStream extends InputStream {

	/** Used to determine the maximum buffer size to use when skipping */
	protected static final int MAX_SKIP_BUFFER_SIZE = 2048;

	/** The {@link Charset} used to convert the byte array to a {@link String} */
	protected final Charset logCharset;

	/** The source {@link InputStream} */
	protected final InputStream inputStream;

	/** The copy of what has been read from the {@link InputStream} */
	protected final ByteArrayOutputStream logStream = new ByteArrayOutputStream();

	/** The cursor position */
	protected int position;

	/** The maximum position the cursor has been at */
	protected int readPosition;

	/** The mark position */
	protected int markPosition;

	/**
	 * Creates a new instance wrapping the specified {@link InputStream}.
	 * {@link #toString()} will be generated using
	 * {@link StandardCharsets#ISO_8859_1}.
	 *
	 * @param inputStream the {@link InputStream} to capture from.
	 */
	public LoggableInputStream(InputStream inputStream) {
		this(inputStream, null);
	}

	/**
	 * Creates a new instance wrapping the specified {@link InputStream}.
	 *
	 * @param inputStream the {@link InputStream} to capture from.
	 * @param logCharset the {@link Charset} to use when generating
	 *            {@link #toString()}.
	 */
	public LoggableInputStream(InputStream inputStream, Charset logCharset) {
		this.inputStream = inputStream;
		if (logCharset == null) {
			this.logCharset = StandardCharsets.ISO_8859_1;
		} else {
			this.logCharset = logCharset;
		}
	}

	@Override
	public int read() throws IOException {
		if (inputStream == null) {
			return -1;
		}
		int result = inputStream.read();
		if (result >= 0) {
			if (position == readPosition) {
				logStream.write(result);
				readPosition++;
			}
			position++;
		}
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (inputStream == null) {
			return -1;
		}
		int numRead = inputStream.read(b, off, len);
		if (numRead > 0) {
			if (position == readPosition) {
				logStream.write(b, off, numRead);
				readPosition += numRead;
			} else if (position + numRead > readPosition) {
				int count = position + numRead - readPosition;
				logStream.write(b, off + numRead - count, count);
				readPosition += count;
			}
			position += numRead;
		}
		return numRead;
	}

	@Override
	public int read(byte[] b) throws IOException {
		if (inputStream == null) {
			return -1;
		}
		int numRead = inputStream.read(b);
		if (numRead > 0) {
			if (position == readPosition) {
				logStream.write(b, 0, numRead);
				readPosition += numRead;
			} else if (position + numRead > readPosition) {
				int count = position + numRead - readPosition;
				logStream.write(b, numRead - count, count);
				readPosition += count;
			}
			position += numRead;
		}
		return numRead;
	}

	@Override
	public int available() throws IOException {
		if (inputStream == null) {
			return 0;
		}
		return inputStream.available();
	}

	@Override
	public long skip(long n) throws IOException {
		if (inputStream == null) {
			return 0;
		}

		long remaining = n;
		int numRead;

		if (n <= 0) {
			return 0;
		}

		while (remaining > 0 && position < readPosition) {
			int count = Math.min(readPosition - position, (int) n);
			count = (int) inputStream.skip(count);
			remaining -= count;
			position += count;
		}

		if (remaining > 0) {
			int size = (int) Math.min(MAX_SKIP_BUFFER_SIZE, remaining);
			byte[] skipBuffer = new byte[size];
			while (remaining > 0) {
				numRead = inputStream.read(skipBuffer, 0, (int) Math.min(size, remaining));
				if (numRead < 0) {
					break;
				}
				if (numRead > 0) {
					logStream.write(skipBuffer, 0, numRead);
					position += numRead;
					readPosition += numRead;
				}
				remaining -= numRead;
			}
		}

		return n - remaining;
	}

	@Override
	public synchronized void mark(int readlimit) {
		if (inputStream == null) {
			return;
		}
		inputStream.mark(readlimit);
		markPosition = position;
	}

	@Override
	public boolean markSupported() {
		if (inputStream == null) {
			return false;
		}
		return inputStream.markSupported();
	}

	@Override
	public synchronized void reset() throws IOException {
		if (inputStream == null) {
			return;
		}
		inputStream.reset();
		position = markPosition;
	}

	@Override
	public void close() throws IOException {
		if (inputStream != null) {
			inputStream.close();
		}
	}

	/**
	 * @return A copy of the bytes read this far.
	 */
	public byte[] getReadBytes() {
		return logStream.toByteArray();
	}

	@Override
	public String toString() {
		try {
			return logStream.toString(logCharset.name());
		} catch (UnsupportedEncodingException e) {
			return "UnsupportedEncodingException: " + e.getMessage();
		}
	}
}
