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
package net.pms.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class BufferedOutputByteArrayImpl extends ByteArrayOutputStream implements BufferedOutputFile {

	public BufferedOutputByteArrayImpl(OutputParams params) {
		super(params.outputByteArrayStreamBufferSize > 512 ? params.outputByteArrayStreamBufferSize : 512);
	}

	public BufferedOutputByteArrayImpl(int size) {
		super(size);
	}

	/**
	 * Returns a {@link ByteArrayInputStream} using the current live buffer.
	 *
	 * @param newReadPosition the number of bytes to skip.
	 * @return The new input stream.
	 */
	@Override
	public synchronized InputStream getInputStream(long newReadPosition) {
		if (newReadPosition < 0) {
			throw new IndexOutOfBoundsException("Can't set new read position to a negative value (" + newReadPosition + ")");
		}

		int length = Math.max(count - (int) newReadPosition, 0);
		if (newReadPosition > 0 && newReadPosition >= count) {
			throw new IndexOutOfBoundsException("Can't skip to position " + newReadPosition + " since the length is " + count);
		}
		byte[] bufferCopy = new byte[length];
		System.arraycopy(buf, (int) newReadPosition, bufferCopy, 0, length);
		return new ByteArrayInputStream(bufferCopy);
	}

	/**
	 * Writes len bytes from the specified byte array starting at offset off to
	 * the {@link ByteArrayOutputStream}.
	 *
	 * @param b the byte array to write from.
	 * @param off the start offset in the data.
	 * @param len the number of bytes to write.
	 */
	@Override
	public void write(byte[] b, int off, int len) {
		super.write(b, off, len);
	}

	/**
	 * Writes the specified byte to the {@link ByteArrayOutputStream}.
	 *
	 * @param b the byte to write.
	 */
	@Override
	public void write(int b) {
		super.write(b);
	}

	/**
	 * Writes {@code b.length} bytes from the specified byte array to the
	 * {@link ByteArrayOutputStream}. The general contract for {@code write(b)}
	 * is that it should have exactly the same effect as the call
	 * {@code write(b, 0, b.length)}.
	 *
	 * @param b the byte array to write.
	 */
	@Override
	public void write(byte[] b) throws IOException {
		super.write(b);
	}

	/**
	 * @deprecated Unused method from interface.
	 * @return null
	 */
	@Deprecated
	@Override
	public WaitBufferedInputStream getCurrentInputStream() {
		return null;
	}

	/**
	 * @deprecated Unused method from interface.
	 * @return 0
	 */
	@Deprecated
	@Override
	public long getWriteCount() {
		return 0;
	}

	/**
	 * @deprecated Unused method from interface.
	 * @return 0
	 */
	@Deprecated
	@Override
	public int read(boolean firstRead, long readCount) {
		return 0;
	}

	/**
	 * @deprecated Unused method from interface.
	 * @return 0
	 */
	@Deprecated
	@Override
	public int read(boolean firstRead, long readCount, byte[] b, int off, int len) {
		return 0;
	}

	/**
	 * @deprecated Unused method from interface.
	 */
	@Deprecated
	@Override
	public void attachThread(ProcessWrapper thread) {
	}

	/**
	 * @deprecated Unused method from interface.
	 */
	@Deprecated
	@Override
	public void reset() {
	}

	/**
	 * @deprecated Unused method from interface.
	 */
	@Deprecated
	@Override
	public void removeInputStream(WaitBufferedInputStream waitBufferedInputStream) {
	}


	/**
	 * @deprecated Unused method from interface.
	 */
	@Deprecated
	@Override
	public void detachInputStream() {
	}
}
