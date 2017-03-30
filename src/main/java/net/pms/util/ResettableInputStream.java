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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import javax.imageio.stream.IIOByteBuffer;
import javax.imageio.stream.ImageInputStream;

/**
 * This class can be used as a "drop in replacement" for
 * {@link BufferedInputStream} if you want to alter the buffering behavior.
 * This will fake the behavior of a {@link BufferedInputStream} with its own
 * "mark position" while keeping the real mark at 0. This means that the start
 * position won't be invalidated just because a mark is placed later than at
 * position 0. {@code marklimit} in {@link #mark(int)} is ignored and the value
 * given in the constructor is used instead, meaning that this can be passed to
 * other methods without the danger of them invalidating the start position by
 * specifying a too small {@code marklimit}.<br>
 * <br>
 * This also offers two additional methods {@link #isFullResetAvailable()} and
 * {@link #fullReset()}. A full reset will ignore any marks set and reset the
 * read position to the start.<br>
 * <br>
 * The intended use for this class is to use an {@link InputStream} instance
 * for multiple purposes without having to read the whole stream into memory,
 * for example when parsing information early in the stream with multiple
 * parsers.
 *
 * @author Nadahar
 */
public class ResettableInputStream extends BufferedInputStream implements ImageInputStream {

	protected int overriddenMarkpos = 0;

	/**
     * Creates a {@link ResettableInputStream} and saves its argument, the
     * input stream {@code in}, for later use. An internal buffer array is
     * created and stored in {@code buf}.
     *
     * @param in the underlying input stream.
	 * @param marklimit the maximum size to buffer before the mark/start point
	 *                  gets invalidated. This overrides the parameter sent
	 *                  when calling {@link #mark(int)}.
	 */
	public ResettableInputStream(InputStream in, int marklimit) {
		super(in);
		this.marklimit = marklimit;
		markpos = 0;
	}

    /**
     * Creates a {@link ResettableInputStream} with the specified buffer
     * size, and saves its argument, the input stream {@code in}, for later
     * use.  An internal buffer array of length {@code size} is created and
     * stored in {@code buf}.
     *
     * @param in the underlying input stream.
     * @param size the initial buffer size.
	 * @param marklimit the maximum size to buffer before the mark/start point
	 *                  gets invalidated. This overrides the parameter sent
	 *                  when calling {@link #mark(int)}.
     * @exception IllegalArgumentException if {@code size} <= 0.
     */
	public ResettableInputStream(InputStream in, int size, int marklimit) {
		super(in, size);
		this.marklimit = marklimit;
		markpos = 0;
	}

    /**
     * See the general contract of {@link InputStream#mark(int)}.
     *
     * @param readlimit - ignored.
     * @see ResettableInputStream#reset()
     */
	@Override
	public synchronized void mark(int readlimit) {
		//Ignore readlimit, we adhere to the constructor limit
        overriddenMarkpos = pos;
	}

    /**
     * See the general contract of {@link InputStream#reset()}.
     * <p>
     * If {@code markpos} is {@code -1} (the start position has been
     * invalidated), an {@link IOException} is thrown. Otherwise, {@code pos}
     * is set equal to {@code markpos}.
     *
     * @exception IOException if the mark has been invalidated, or the stream
     *                        has been closed by invoking its {@link #close()}
     *                        method, or an I/O error occurs.
     * @see ResettableInputStream#mark(int)
     */
	@Override
	public synchronized void reset() throws IOException {
		super.reset();
		if (overriddenMarkpos < 0) {
			overriddenMarkpos = 0;
		}
        pos = overriddenMarkpos;
	}

	/**
	 * @return Whether a call to {@link #fullReset()} will succeed.
	 */
	public synchronized boolean isFullResetAvailable() {
		return markpos == 0;
	}

	/**
	 * Resets the stream read position to the start if possible.
	 *
	 * @throws IOException if the full reset fails.
	 */
	public synchronized void fullReset() throws IOException {
		super.reset();
		pos = 0;
	}

	@Override
	public void setByteOrder(ByteOrder byteOrder) {
		// TODO Auto-generated method stub

	}

	@Override
	public ByteOrder getByteOrder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void readBytes(IIOByteBuffer buf, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean readBoolean() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte readByte() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readUnsignedByte() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public short readShort() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readUnsignedShort() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public char readChar() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readInt() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readUnsignedInt() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readLong() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public float readFloat() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double readDouble() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String readLine() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String readUTF() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void readFully(byte[] b, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(byte[] b) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(short[] s, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(char[] c, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(int[] i, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(long[] l, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(float[] f, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void readFully(double[] d, int off, int len) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public long getStreamPosition() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getBitOffset() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setBitOffset(int bitOffset) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int readBit() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long readBits(int numBits) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long length() throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int skipBytes(int n) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long skipBytes(long n) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void seek(long pos) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void mark() {
		// TODO Auto-generated method stub

	}

	@Override
	public void flushBefore(long pos) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void flush() throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public long getFlushedPosition() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isCached() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCachedMemory() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCachedFile() {
		// TODO Auto-generated method stub
		return false;
	}
}
