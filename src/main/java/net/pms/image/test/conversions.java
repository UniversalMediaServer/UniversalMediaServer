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
public abstract class conversions implements Closeable {

	protected final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	protected Deque<Long> markByteStack = new ArrayDeque<Long>();

	protected Deque<Integer> markBitStack = new ArrayDeque<Integer>();

	protected boolean isClosed = false;

	// Length of the buffer used for readFully(type[], int, int)
	//protected static final int BYTE_BUF_LENGTH = 8192;

	/**
	 * Byte buffer used for readFully(type[], int, int). Note that this array is
	 * also used for bulk reads in readShort(), readInt(), etc, so it should be
	 * large enough to hold a primitive value (i.e. >= 8 bytes). Also note that
	 * this array is package protected, so that it can be used by
	 * ImageOutputStreamImpl in a similar manner.
	 */
	//protected byte[] byteBuf = new byte[BYTE_BUF_LENGTH];

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
	public conversions() {
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

	public void readFully(long position, byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > b.length || off + len < 0) {
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > b.length!");
		}

		while (len > 0) {
			int nbytes = read(position, b, off, len);
			if (nbytes == -1) {
				throw new EOFException();
			}
			off += nbytes;
			len -= nbytes;
		}
	}

	public void readFully(long position, byte[] b) throws IOException {
		readFully(position, b, 0, b.length);
	}

	/*
	 * public void readFully(short[] s, int off, int len) throws IOException {
	 * // Fix 4430357 - if off + len < 0, overflow occurred if (off < 0 || len <
	 * 0 || off + len > s.length || off + len < 0) { throw new
	 * IndexOutOfBoundsException
	 * ("off < 0 || len < 0 || off + len > s.length!"); }
	 *
	 * while (len > 0) { int nelts = Math.min(len, byteBuf.length/2);
	 * readFully(byteBuf, 0, nelts*2); toShorts(byteBuf, s, off, nelts); off +=
	 * nelts; len -= nelts; } }
	 */

	public void readFully(short[] s, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > s.length || off + len < 0) {
			// NullPointerException will be thrown before this if s is null.
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > s.length");
		} else if (len == 0) {
			return;
		}

		// Determine the requested length in bytes.
		int byteLen = 2 * len;
		byte[] buffer =

		while (len > 0) {
			int nelts = Math.min(len, byteBuf.length/2);
			readFully(byteBuf, 0, nelts*2);
			toShorts(byteBuf, s, off, nelts);
			off += nelts;
			len -= nelts;
		}

		// Get the mapped buffer.
		ByteBuffer byteBuffer = getMappedBuffer(byteLen);

		// Ensure enough bytes remain.
		if (byteBuffer.remaining() < byteLen) {
			throw new EOFException();
		}

		// Get the view Buffer.
		ShortBuffer viewBuffer = byteBuffer.asShortBuffer();

		// Get the shorts.
		viewBuffer.get(s, off, len);

		// Update the position.
		seek(streamPos + byteLen);
	}

	/*
	 * public void readFully(char[] c, int off, int len) throws IOException { //
	 * Fix 4430357 - if off + len < 0, overflow occurred if (off < 0 || len < 0
	 * || off + len > c.length || off + len < 0) { throw new
	 * IndexOutOfBoundsException
	 * ("off < 0 || len < 0 || off + len > c.length!"); }
	 *
	 * while (len > 0) { int nelts = Math.min(len, byteBuf.length/2);
	 * readFully(byteBuf, 0, nelts*2); toChars(byteBuf, c, off, nelts); off +=
	 * nelts; len -= nelts; } }
	 */

	public void readFully(char[] c, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > c.length) {
			// NullPointerException will be thrown before this if c is null.
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > c.length");
		} else if (len == 0) {
			return;
		}

		// Determine the requested length in bytes.
		int byteLen = 2 * len;

		// Get the mapped buffer.
		ByteBuffer byteBuffer = getMappedBuffer(byteLen);

		// Ensure enough bytes remain.
		if (byteBuffer.remaining() < byteLen) {
			throw new EOFException();
		}

		// Get the view Buffer.
		CharBuffer viewBuffer = byteBuffer.asCharBuffer();

		// Get the chars.
		viewBuffer.get(c, off, len);

		// Update the position.
		seek(streamPos + byteLen);
	}

	/*
	 * public void readFully(int[] i, int off, int len) throws IOException { //
	 * Fix 4430357 - if off + len < 0, overflow occurred if (off < 0 || len < 0
	 * || off + len > i.length || off + len < 0) { throw new
	 * IndexOutOfBoundsException
	 * ("off < 0 || len < 0 || off + len > i.length!"); }
	 *
	 * while (len > 0) { int nelts = Math.min(len, byteBuf.length/4);
	 * readFully(byteBuf, 0, nelts*4); toInts(byteBuf, i, off, nelts); off +=
	 * nelts; len -= nelts; } }
	 */

	public void readFully(int[] i, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > i.length) {
			// NullPointerException will be thrown before this if i is null.
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > i.length");
		} else if (len == 0) {
			return;
		}

		// Determine the requested length in bytes.
		int byteLen = 4 * len;

		// Get the mapped buffer.
		ByteBuffer byteBuffer = getMappedBuffer(byteLen);

		// Ensure enough bytes remain.
		if (byteBuffer.remaining() < byteLen) {
			throw new EOFException();
		}

		// Get the view Buffer.
		IntBuffer viewBuffer = byteBuffer.asIntBuffer();

		// Get the ints.
		viewBuffer.get(i, off, len);

		// Update the position.
		seek(streamPos + byteLen);
	}

	/*
	 * public void readFully(long[] l, int off, int len) throws IOException { //
	 * Fix 4430357 - if off + len < 0, overflow occurred if (off < 0 || len < 0
	 * || off + len > l.length || off + len < 0) { throw new
	 * IndexOutOfBoundsException
	 * ("off < 0 || len < 0 || off + len > l.length!"); }
	 *
	 * while (len > 0) { int nelts = Math.min(len, byteBuf.length/8);
	 * readFully(byteBuf, 0, nelts*8); toLongs(byteBuf, l, off, nelts); off +=
	 * nelts; len -= nelts; } }
	 */

	public void readFully(long[] l, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > l.length) {
			// NullPointerException will be thrown before this if l is null.
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > l.length");
		} else if (len == 0) {
			return;
		}

		// Determine the requested length in bytes.
		int byteLen = 8 * len;

		// Get the mapped buffer.
		ByteBuffer byteBuffer = getMappedBuffer(byteLen);

		// Ensure enough bytes remain.
		if (byteBuffer.remaining() < byteLen) {
			throw new EOFException();
		}

		// Get the view Buffer.
		LongBuffer viewBuffer = byteBuffer.asLongBuffer();

		// Get the longs.
		viewBuffer.get(l, off, len);

		// Update the position.
		seek(streamPos + byteLen);
	}

	/*
	 * public void readFully(float[] f, int off, int len) throws IOException {
	 * // Fix 4430357 - if off + len < 0, overflow occurred if (off < 0 || len <
	 * 0 || off + len > f.length || off + len < 0) { throw new
	 * IndexOutOfBoundsException
	 * ("off < 0 || len < 0 || off + len > f.length!"); }
	 *
	 * while (len > 0) { int nelts = Math.min(len, byteBuf.length/4);
	 * readFully(byteBuf, 0, nelts*4); toFloats(byteBuf, f, off, nelts); off +=
	 * nelts; len -= nelts; } }
	 */

	public void readFully(float[] f, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > f.length) {
			// NullPointerException will be thrown before this if f is null.
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > f.length");
		} else if (len == 0) {
			return;
		}

		// Determine the requested length in bytes.
		int byteLen = 4 * len;

		// Get the mapped buffer.
		ByteBuffer byteBuffer = getMappedBuffer(byteLen);

		// Ensure enough bytes remain.
		if (byteBuffer.remaining() < byteLen) {
			throw new EOFException();
		}

		// Get the view Buffer.
		FloatBuffer viewBuffer = byteBuffer.asFloatBuffer();

		// Get the floats.
		viewBuffer.get(f, off, len);

		// Update the position.
		seek(streamPos + byteLen);
	}

	/*
	 * public void readFully(double[] d, int off, int len) throws IOException {
	 * // Fix 4430357 - if off + len < 0, overflow occurred if (off < 0 || len <
	 * 0 || off + len > d.length || off + len < 0) { throw new
	 * IndexOutOfBoundsException
	 * ("off < 0 || len < 0 || off + len > d.length!"); }
	 *
	 * while (len > 0) { int nelts = Math.min(len, byteBuf.length/8);
	 * readFully(byteBuf, 0, nelts*8); toDoubles(byteBuf, d, off, nelts); off +=
	 * nelts; len -= nelts; } }
	 */

	public void readFully(double[] d, int off, int len) throws IOException {
		if (off < 0 || len < 0 || off + len > d.length) {
			// NullPointerException will be thrown before this if d is null.
			throw new IndexOutOfBoundsException("off < 0 || len < 0 || off + len > d.length");
		} else if (len == 0) {
			return;
		}

		// Determine the requested length in bytes.
		int byteLen = 8 * len;

		// Get the mapped buffer.
		ByteBuffer byteBuffer = getMappedBuffer(byteLen);

		// Ensure enough bytes remain.
		if (byteBuffer.remaining() < byteLen) {
			throw new EOFException();
		}

		// Get the view Buffer.
		DoubleBuffer viewBuffer = byteBuffer.asDoubleBuffer();

		// Get the doubles.
		viewBuffer.get(d, off, len);

		// Update the position.
		seek(streamPos + byteLen);
	}

	private void toShorts(byte[] b, short[] s, int off, int len) {
		int boff = 0;
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff];
				int b1 = b[boff + 1] & 0xff;
				s[off + j] = (short) ((b0 << 8) | b1);
				boff += 2;
			}
		} else {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff + 1];
				int b1 = b[boff] & 0xff;
				s[off + j] = (short) ((b0 << 8) | b1);
				boff += 2;
			}
		}
	}

	private void toChars(byte[] b, char[] c, int off, int len) {
		int boff = 0;
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff];
				int b1 = b[boff + 1] & 0xff;
				c[off + j] = (char) ((b0 << 8) | b1);
				boff += 2;
			}
		} else {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff + 1];
				int b1 = b[boff] & 0xff;
				c[off + j] = (char) ((b0 << 8) | b1);
				boff += 2;
			}
		}
	}

	private void toInts(byte[] b, int[] i, int off, int len) {
		int boff = 0;
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff];
				int b1 = b[boff + 1] & 0xff;
				int b2 = b[boff + 2] & 0xff;
				int b3 = b[boff + 3] & 0xff;
				i[off + j] = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				boff += 4;
			}
		} else {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff + 3];
				int b1 = b[boff + 2] & 0xff;
				int b2 = b[boff + 1] & 0xff;
				int b3 = b[boff] & 0xff;
				i[off + j] = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				boff += 4;
			}
		}
	}

	private void toLongs(byte[] b, long[] l, int off, int len) {
		int boff = 0;
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff];
				int b1 = b[boff + 1] & 0xff;
				int b2 = b[boff + 2] & 0xff;
				int b3 = b[boff + 3] & 0xff;
				int b4 = b[boff + 4];
				int b5 = b[boff + 5] & 0xff;
				int b6 = b[boff + 6] & 0xff;
				int b7 = b[boff + 7] & 0xff;

				int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

				l[off + j] = ((long) i0 << 32) | (i1 & 0xffffffffL);
				boff += 8;
			}
		} else {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff + 7];
				int b1 = b[boff + 6] & 0xff;
				int b2 = b[boff + 5] & 0xff;
				int b3 = b[boff + 4] & 0xff;
				int b4 = b[boff + 3];
				int b5 = b[boff + 2] & 0xff;
				int b6 = b[boff + 1] & 0xff;
				int b7 = b[boff] & 0xff;

				int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;

				l[off + j] = ((long) i0 << 32) | (i1 & 0xffffffffL);
				boff += 8;
			}
		}
	}

	private void toFloats(byte[] b, float[] f, int off, int len) {
		int boff = 0;
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff];
				int b1 = b[boff + 1] & 0xff;
				int b2 = b[boff + 2] & 0xff;
				int b3 = b[boff + 3] & 0xff;
				int i = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				f[off + j] = Float.intBitsToFloat(i);
				boff += 4;
			}
		} else {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff + 3];
				int b1 = b[boff + 2] & 0xff;
				int b2 = b[boff + 1] & 0xff;
				int b3 = b[boff + 0] & 0xff;
				int i = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				f[off + j] = Float.intBitsToFloat(i);
				boff += 4;
			}
		}
	}

	private void toDoubles(byte[] b, double[] d, int off, int len) {
		int boff = 0;
		if (byteOrder == ByteOrder.BIG_ENDIAN) {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff];
				int b1 = b[boff + 1] & 0xff;
				int b2 = b[boff + 2] & 0xff;
				int b3 = b[boff + 3] & 0xff;
				int b4 = b[boff + 4];
				int b5 = b[boff + 5] & 0xff;
				int b6 = b[boff + 6] & 0xff;
				int b7 = b[boff + 7] & 0xff;

				int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
				long l = ((long) i0 << 32) | (i1 & 0xffffffffL);

				d[off + j] = Double.longBitsToDouble(l);
				boff += 8;
			}
		} else {
			for (int j = 0; j < len; j++) {
				int b0 = b[boff + 7];
				int b1 = b[boff + 6] & 0xff;
				int b2 = b[boff + 5] & 0xff;
				int b3 = b[boff + 4] & 0xff;
				int b4 = b[boff + 3];
				int b5 = b[boff + 2] & 0xff;
				int b6 = b[boff + 1] & 0xff;
				int b7 = b[boff] & 0xff;

				int i0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
				int i1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
				long l = ((long) i0 << 32) | (i1 & 0xffffffffL);

				d[off + j] = Double.longBitsToDouble(l);
				boff += 8;
			}
		}
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
