package net.pms.image.test;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.imageio.stream.IIOByteBuffer;

/**
 * An abstract class implementing the <code>ImageInputStream</code> interface.
 * This class is designed to reduce the number of methods that must be
 * implemented by subclasses.
 *
 * <p>
 * In particular, this class handles most or all of the details of byte order
 * interpretation, buffering, mark/reset, discarding, closing, and disposing.
 */
public abstract class AbstractInputSource implements Closeable {

	/**
	 * The byte order of the stream as an instance of the enumeration class
	 * <code>java.nio.ByteOrder</code>, where <code>ByteOrder.BIG_ENDIAN</code>
	 * indicates network byte order and <code>ByteOrder.LITTLE_ENDIAN</code>
	 * indicates the reverse order. By default, the value is
	 * <code>ByteOrder.BIG_ENDIAN</code>.
	 */
	protected ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;

	/**
	 * The current read position within the stream. Subclasses are responsible
	 * for keeping this value current from any method they override that alters
	 * the read position.
	 */
	protected long streamPos;

	/**
	 * The current bit offset within the stream. Subclasses are responsible for
	 * keeping this value current from any method they override that alters the
	 * bit offset.
	 */
	protected int bitOffset;

	/**
	 * Constructs an <code>ImageInputStreamImpl</code>.
	 */
	public AbstractInputSource() {
	}

	protected abstract void checkClosed() throws IOException;

	public abstract void setByteOrder(ByteOrder byteOrder);

	public abstract ByteOrder getByteOrder();

	/**
	 * Reads a single byte from the stream and returns it as an <code>int</code>
	 * between 0 and 255. If EOF is reached, <code>-1</code> is returned.
	 *
	 * <p>
	 * Subclasses must provide an implementation for this method. The subclass
	 * implementation should update the stream position before exiting.
	 *
	 * <p>
	 * The bit offset within the stream must be reset to zero before the read
	 * occurs.
	 *
	 * @return the value of the next byte in the stream, or <code>-1</code> if
	 *         EOF is reached.
	 *
	 * @exception IOException if the stream has been closed.
	 */
	public abstract int read(long position) throws IOException;

	/**
	 * A convenience method that calls <code>read(b, 0, b.length)</code>.
	 *
	 * <p>
	 * The bit offset within the stream is reset to zero before the read occurs.
	 *
	 * @return the number of bytes actually read, or <code>-1</code> to indicate
	 *         EOF.
	 *
	 * @exception NullPointerException if <code>b</code> is <code>null</code>.
	 * @exception IOException if an I/O error occurs.
	 */
	public int read(long position, byte[] b) throws IOException {
		return read(position, b, 0, b.length);
	}

	/**
	 * Reads up to <code>len</code> bytes from the stream, and stores them into
	 * <code>b</code> starting at index <code>off</code>. If no bytes can be
	 * read because the end of the stream has been reached, <code>-1</code> is
	 * returned.
	 *
	 * <p>
	 * The bit offset within the stream must be reset to zero before the read
	 * occurs.
	 *
	 * <p>
	 * Subclasses must provide an implementation for this method. The subclass
	 * implementation should update the stream position before exiting.
	 *
	 * @param b an array of bytes to be written to.
	 * @param off the starting position within <code>b</code> to write to.
	 * @param len the maximum number of bytes to read.
	 *
	 * @return the number of bytes actually read, or <code>-1</code> to indicate
	 *         EOF.
	 *
	 * @exception IndexOutOfBoundsException if <code>off</code> is negative,
	 *                <code>len</code> is negative, or <code>off +
	 * len</code> is greater than <code>b.length</code>.
	 * @exception NullPointerException if <code>b</code> is <code>null</code>.
	 * @exception IOException if an I/O error occurs.
	 */
	public abstract int read(long position, byte[] b, int off, int len) throws IOException;

	public boolean readBoolean(long position) throws IOException {
		int i = this.read(position);
		if (i < 0) {
			throw new EOFException();
		}
		return (i != 0);
	}

	public byte readByte(long position) throws IOException {
		int i = this.read(position);
		if (i < 0) {
			throw new EOFException();
		}
		return (byte) i;
	}

	public int readUnsignedByte(long position) throws IOException {
		int i = this.read(position);
		if (i < 0) {
			throw new EOFException();
		}
		return i;
	}

	public short readShort(long position) throws IOException {
		byte[] buffer = new byte[2];
		if (read(position, buffer, 0, 2) < 0) {
			throw new EOFException();
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			return (short) (((buffer[0] & 0xff) << 8) | ((buffer[1] & 0xff) << 0));
		} else {
			return (short) (((buffer[1] & 0xff) << 8) | ((buffer[0] & 0xff) << 0));
		}
	}

	public int readUnsignedShort(long position) throws IOException {
		return ((int) readShort(position)) & 0xffff;
	}

	public char readChar(long position) throws IOException {
		return (char) readShort(position);
	}

	public int readInt(long position) throws IOException {
		byte[] buffer = new byte[4];
		if (read(position, buffer, 0, 4) < 0) {
			throw new EOFException();
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			return (((buffer[0] & 0xff) << 24) | ((buffer[1] & 0xff) << 16) | ((buffer[2] & 0xff) << 8) | ((buffer[3] & 0xff) << 0));
		} else {
			return (((buffer[3] & 0xff) << 24) | ((buffer[2] & 0xff) << 16) | ((buffer[1] & 0xff) << 8) | ((buffer[0] & 0xff) << 0));
		}
	}

	public long readUnsignedInt(long position) throws IOException {
		return ((long) readInt(position)) & 0xffffffffL;
	}

	public long readLong(long position) throws IOException {
		byte[] buffer = new byte[8];
		if (read(position, buffer, 0, 8) < 0) {
			throw new EOFException();
		}

		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			return (
				((long) (buffer[0] & 0xff) << 56) |
				((long) (buffer[1] & 0xff) << 48) |
				((long) (buffer[2] & 0xff) << 40) |
				((long) (buffer[3] & 0xff) << 32) |
				((buffer[4] & 0xff) << 24) |
				((buffer[5] & 0xff) << 16) |
				((buffer[6] & 0xff) << 8) |
				((buffer[7] & 0xff) << 0)
			);
		} else {
			return (
				((long) (buffer[7] & 0xff) << 56) |
				((long) (buffer[6] & 0xff) << 48) |
				((long) (buffer[5] & 0xff) << 40) |
				((long) (buffer[4] & 0xff) << 32) |
				((buffer[3] & 0xff) << 24) |
				((buffer[2] & 0xff) << 16) |
				((buffer[1] & 0xff) << 8) |
				((buffer[0] & 0xff) << 0)
			);
		}
	}

	public float readFloat(long position) throws IOException {
		return Float.intBitsToFloat(readInt(position));
	}

	public double readDouble(long position) throws IOException {
		return Double.longBitsToDouble(readLong(position));
	}


	public long getPosition() throws IOException {
		checkClosed();
		return streamPos;
	}


	public int getBitOffset() throws IOException {
		checkClosed();
		return bitOffset;
	}

	public void setBitOffset(int bitOffset) throws IOException {
		checkClosed();
		if (bitOffset < 0 || bitOffset > 7) {
			throw new IllegalArgumentException("bitOffset must be betwwen 0 and 7!");
		}
		this.bitOffset = bitOffset;
	}

	public int readBit() throws IOException {
		checkClosed();

		// Compute final bit offset before we call read() and seek()
		int newBitOffset = (this.bitOffset + 1) & 0x7;

		int val = read();
		if (val == -1) {
			throw new EOFException();
		}

		if (newBitOffset != 0) {
			// Move byte position back if in the middle of a byte
			seek(getPosition() - 1);
			// Shift the bit to be read to the rightmost position
			val >>= 8 - newBitOffset;
		}
		this.bitOffset = newBitOffset;

		return val & 0x1;
	}

	public long readBits(int numBits) throws IOException {
		checkClosed();

		if (numBits < 0 || numBits > 64) {
			throw new IllegalArgumentException();
		}
		if (numBits == 0) {
			return 0L;
		}

		// Have to read additional bits on the left equal to the bit offset
		int bitsToRead = numBits + bitOffset;

		// Compute final bit offset before we call read() and seek()
		int newBitOffset = (this.bitOffset + numBits) & 0x7;

		// Read a byte at a time, accumulate
		long accum = 0L;
		while (bitsToRead > 0) {
			int val = read();
			if (val == -1) {
				throw new EOFException();
			}

			accum <<= 8;
			accum |= val;
			bitsToRead -= 8;
		}

		// Move byte position back if in the middle of a byte
		if (newBitOffset != 0) {
			seek(getPosition() - 1);
		}
		this.bitOffset = newBitOffset;

		// Shift away unwanted bits on the right.
		accum >>>= (-bitsToRead); // Negative of bitsToRead == extra bits read

		// Mask out unwanted bits on the left
		accum &= (-1L >>> (64 - numBits));

		return accum;
	}

	/**
	 * Returns <code>-1L</code> to indicate that the stream has unknown length.
	 * Subclasses must override this method to provide actual length
	 * information.
	 *
	 * @return -1L to indicate unknown length.
	 */
	/*
	 * public long length() { return -1L; }
	 */

	/**
	 * Returns the number of bytes currently in the <code>FileChannel</code>. If
	 * an <code>IOException</code> is encountered when querying the channel's
	 * size, -1L will be returned.
	 *
	 * @return The number of bytes in the channel -1L to indicate unknown
	 *         length.
	 */
	public long length() {
		// Initialize to value indicating unknown length.
		long length = -1L;

		// Set length to current size with respect to initial position.
		try {
			length = channel.size();
		} catch (IOException e) {
			// Default to unknown length.
		}

		return length;
	}

	/**
	 * Advances the current stream position by calling
	 * <code>seek(getStreamPosition() + n)</code>.
	 *
	 * <p>
	 * The bit offset is reset to zero.
	 *
	 * @param n the number of bytes to seek forward.
	 *
	 * @return an <code>int</code> representing the number of bytes skipped.
	 *
	 * @exception IOException if <code>getStreamPosition</code> throws an
	 *                <code>IOException</code> when computing either the
	 *                starting or ending position.
	 */
	public int skipBytes(int n) throws IOException {
		long pos = getPosition();
		seek(pos + n);
		return (int) (getPosition() - pos);
	}

	/**
	 * Advances the current stream position by calling
	 * <code>seek(getStreamPosition() + n)</code>.
	 *
	 * <p>
	 * The bit offset is reset to zero.
	 *
	 * @param n the number of bytes to seek forward.
	 *
	 * @return a <code>long</code> representing the number of bytes skipped.
	 *
	 * @exception IOException if <code>getStreamPosition</code> throws an
	 *                <code>IOException</code> when computing either the
	 *                starting or ending position.
	 */
	public long skipBytes(long n) throws IOException {
		long pos = getPosition();
		seek(pos + n);
		return getPosition() - pos;
	}

	/*
	 * public void seek(long pos) throws IOException { checkClosed();
	 *
	 * // This test also covers pos < 0 if (pos < flushedPos) { throw new
	 * IndexOutOfBoundsException("pos < flushedPos!"); }
	 *
	 * this.streamPos = pos; this.bitOffset = 0; }
	 */

	/**
	 * Invokes the superclass method and sets the position within the memory
	 * mapped buffer. A new region is mapped if necessary. The position of the
	 * source <code>FileChannel</code> is not changed, i.e.,
	 * {@link java.nio.channels.FileChannel#position(long)} is not invoked.
	 */
	public void seek(long pos) throws IOException {
		checkClosed();

		// This test also covers pos < 0
		if (pos < 0) {
			throw new IndexOutOfBoundsException("Invalid position " + pos);
		}

		this.streamPos = pos;
		this.bitOffset = 0;

		if (pos >= mappedPos && pos < mappedUpperBound) {
			// Seeking to location within mapped buffer: set buffer position.
			mappedBuffer.position((int) (pos - mappedPos));
		} else {
			// Seeking to location outside mapped buffer: get a new mapped
			// buffer at current position with maximal size.
			int len = (int) Math.min(channel.size() - pos, Integer.MAX_VALUE);
			mappedBuffer = getMappedBuffer(len);
		}
	}

	/**
	 * Pushes the current stream position onto a stack of marked positions.
	 */
	public void mark() {
		try {
			markByteStack.push(getPosition());
			markBitStack.push(getBitOffset());
		} catch (IOException e) {
		}
	}

	/**
	 * Resets the current stream byte and bit positions from the stack of marked
	 * positions.
	 *
	 * <p>
	 * An <code>IOException</code> will be thrown if the previous marked
	 * position lies in the discarded portion of the stream.
	 *
	 * @exception IOException if an I/O error occurs.
	 */
	public void reset() throws IOException {
		if (markByteStack.isEmpty()) {
			return;
		}

		long pos = ((Long) markByteStack.pop()).longValue();
		seek(pos);

		int offset = markBitStack.pop().intValue();
		setBitOffset(offset);// TODO: WTF?
	}

	/**
	 * Default implementation returns false. Subclasses should override this if
	 * they cache data.
	 */
	public boolean isCached() {
		return false;
	}

	/**
	 * Default implementation returns false. Subclasses should override this if
	 * they cache data in main memory.
	 */
	public boolean isCachedMemory() {
		return false;
	}

	/**
	 * Default implementation returns false. Subclasses should override this if
	 * they cache data in a temporary file.
	 */
	public boolean isCachedFile() {
		return false;
	}

	/**
	 * sets the internal reference to the source <code>FileChannel</code> to
	 * <code>null</code>. The source <code>FileChannel</code> is not closed.
	 *
	 * @exception IOException if an error occurs.
	 */
	public void close() throws IOException {
		// checkClosed();

		isClosed = true;
		channel = null;
	}

	/**
	 * Finalizes this object prior to garbage collection. The <code>close</code>
	 * method is called to close any open input source. This method should not
	 * be called from application code.
	 *
	 * @exception Throwable if an error occurs during superclass finalization.
	 */
	protected void finalize() throws Throwable {
		if (!isClosed) {
			try {
				close();
			} catch (IOException e) {
			}
		}
		super.finalize();
	}
}
