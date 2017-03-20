package net.pms.io;

/*
 * Input stream wrapper with a byte limit.
 * Copyright (C) 2004 Stephen Ostermiller
 * http://ostermiller.org/contact.pl?regarding=Java+Utilities
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See COPYING.TXT for details.
 */

import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream wrapper that will read only a set number of bytes from the
 * underlying stream.
 * 
 * @author Stephen Ostermiller
 *         http://ostermiller.org/contact.pl?regarding=Java+Utilities
 * @since ostermillerutils 1.04.00
 */
public class SizeLimitInputStream extends InputStream {

	/**
	 * The input stream that is being protected. All methods should be forwarded
	 * to it, after checking the size that has been read.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	protected InputStream in;

	/**
	 * The number of bytes to read at most from this Stream. Read methods should
	 * check to ensure that bytesRead never exceeds maxBytesToRead.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	protected long maxBytesToRead = 0;

	/**
	 * The number of bytes that have been read from this stream. Read methods
	 * should check to ensure that bytesRead never exceeds maxBytesToRead.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	protected long bytesRead = 0;

	/**
	 * The number of bytes that have been read from this stream since mark() was
	 * called.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	protected long bytesReadSinceMark = 0;

	/**
	 * The number of bytes the user has request to have been marked for reset.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	protected long markReadLimitBytes = -1;

	/**
	 * Get the number of bytes actually read from this stream.
	 * 
	 * @return number of bytes that have already been taken from this stream.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	public long getBytesRead() {
		return bytesRead;
	}

	/**
	 * Get the maximum number of bytes left to read before the limit (set in the
	 * constructor) is reached.
	 * 
	 * @return The number of bytes that (at a maximum) are left to be taken from
	 *         this stream.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	public long getBytesLeft() {
		return maxBytesToRead - bytesRead;
	}

	/**
	 * Tell whether the number of bytes specified in the constructor have been
	 * read yet.
	 * 
	 * @return true iff the specified number of bytes have all been read.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	public boolean allBytesRead() {
		return getBytesLeft() == 0;
	}

	/**
	 * Get the number of total bytes (including bytes already read) that can be
	 * read from this stream (as set in the constructor).
	 * 
	 * @return Maximum bytes that can be read until the size limit runs out
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	public long getMaxBytesToRead() {
		return maxBytesToRead;
	}

	/**
	 * Create a new size limit input stream from another stream given a size
	 * limit.
	 * 
	 * @param in
	 *            The input stream.
	 * @param maxBytesToRead
	 *            the max number of bytes to allow to be read from the
	 *            underlying stream.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	public SizeLimitInputStream(InputStream in, long maxBytesToRead) {
		this.in = in;
		this.maxBytesToRead = maxBytesToRead;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws IOException {
		if (bytesRead >= maxBytesToRead) {
			return -1;
		}
		int b = in.read();
		if (b != -1) {
			bytesRead++;
			bytesReadSinceMark++;
		}
		return b;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (bytesRead >= maxBytesToRead) {
			return -1;
		}
		long bytesLeft = getBytesLeft();
		if (len > bytesLeft) {
			len = (int) bytesLeft;
		}
		int bytesJustRead = in.read(b, off, len);
		bytesRead += bytesJustRead;
		bytesReadSinceMark += bytesJustRead;
		return bytesJustRead;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long skip(long n) throws IOException {
		if (bytesRead >= maxBytesToRead) {
			return -1;
		}
		long bytesLeft = getBytesLeft();
		if (n > bytesLeft) {
			n = bytesLeft;
		}
		return in.skip(n);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int available() throws IOException {
		int available = in.available();
		long bytesLeft = getBytesLeft();
		if (available > bytesLeft) {
			available = (int) bytesLeft;
		}
		return available;
	}

	/**
	 * Close this stream and underlying streams. Calling this method may make
	 * data on the underlying stream unavailable.
	 * <p>
	 * Consider wrapping this stream in a NoCloseStream so that clients can call
	 * close() with no effect.
	 * 
	 * @since ostermillerutils 1.04.00
	 */
	@Override
	public void close() throws IOException {
		in.close();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void mark(int readlimit) {
		if (in.markSupported()) {
			markReadLimitBytes = readlimit;
			bytesReadSinceMark = 0;
			in.mark(readlimit);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void reset() throws IOException {
		if (in.markSupported() && bytesReadSinceMark <= markReadLimitBytes) {
			bytesRead -= bytesReadSinceMark;
			in.reset();
			bytesReadSinceMark = 0;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean markSupported() {
		return in.markSupported();
	}
}

