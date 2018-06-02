/*
 * Universal Media Server, for streaming any media to DLNA compatible renderers
 * based on the http://www.ps3mediaserver.org. Copyright (C) 2012 UMS
 * developers.
 *
 * This program is a free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; version 2 of the License only.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package net.pms.util.jna;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;


/**
 * An implementation of a referenced fixed size 8-bit char array where the
 * elements are placed adjacent to each other without a terminator.
 * <p>
 * Internally {@link Byte} is used since Java has no 8-bit char type, which means
 * that it is possible to set and get the array as {@code Byte[]} and perform character.
 *
 * @author Nadahar
 */
public class FixedCharArrayByReference extends FixedArrayByReference<Byte> {

	/**
	 * Creates an unallocated {@link FixedCharArrayByReference}.
	 *
	 * @param fixedSize the size of this fixed size 8-bit char array.
	 */
	public FixedCharArrayByReference(long fixedSize) {
		super(fixedSize);
	}

	/**
	 * Constructs a new {@code String} by decoding the referenced char array
	 * using the platform's default charset.
	 * <p>
	 * <b>Relying on the default charset can often lead to bugs. Use
	 * {@link #getString(Charset)} instead.</b>
	 *
	 * @return The {@link String}.
	 */
	public String getString() {
		return getString(Charset.defaultCharset());
	}

	/**
	 * Constructs a new {@code String} by decoding the referenced char array
     * using {@code charset}.
     *
     * @param charset the {@link Charset} to use for decoding.
	 * @return The {@link String}.
	 */
	public String getString(Charset charset) {
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Array to big, please read it \"manually\" using getPointer.getX");
		}
		if (getPointer() == null) {
			return null;
		}
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		CharsetDecoder decoder = charset.newDecoder();
		decoder.onMalformedInput(CodingErrorAction.REPLACE);
		decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
		decoder.replaceWith("?");
		ByteBuffer byteBuffer = ByteBuffer.wrap(getPointer().getByteArray(0, (int) size));
		CharBuffer charBuffer = CharBuffer.allocate(100);
		StringBuilder sb = new StringBuilder(100);
		CoderResult coderResult = null;
		while (coderResult == null || !coderResult.isUnderflow()) {
			coderResult = decoder.decode(byteBuffer, charBuffer, true);
			if (coderResult.isOverflow() && drainCharBuffer(charBuffer, sb)) {
				return sb.toString();
			}
		}
		if (drainCharBuffer(charBuffer, sb)) {
			return sb.toString();
		}
		coderResult = null;
		while (coderResult == null || !coderResult.isUnderflow()) {
			coderResult = decoder.flush(charBuffer);
			if (coderResult.isOverflow() && drainCharBuffer(charBuffer, sb)) {
				return sb.toString();
			}
		}
		if (drainCharBuffer(charBuffer, sb)) {
			return sb.toString();
		}
		return sb.toString();
	}

	/**
	 * Drains {@code charBuffer} and appends the result to {@code stringBuilder}
	 * while checking for null-terminator. {@code charBuffer} is compacted and
	 * ready to be filled after the operation unless a null-terminator is found.
	 *
	 * @param charBuffer the {@link Buffer} to drain.
	 * @param stringBuilder the {@link StringBuilder} to fill.
	 * @return {@code true} if a null-terminator was encountered, {@code false}
	 *         otherwise.
	 */
	protected boolean drainCharBuffer(CharBuffer charBuffer, StringBuilder stringBuilder) {
		if (charBuffer == null || stringBuilder == null) {
			throw new IllegalArgumentException("Neither argument can be null");
		}
		charBuffer.flip();
		while (charBuffer.hasRemaining()) {
			char c = charBuffer.get();
			if (c == 0) {
				return true;
			}
			stringBuilder.append(c);
		}
		charBuffer.compact();
		return false;
	}

	/**
	 * Encodes {@code content} to an array of 8-bit char using the platform's default charset,
	 * and writes this to the referenced char array. If the encoded
	 * {@code content} is bigger than the fixed array size, it will be truncated
	 * possibly in the middle of a multi-byte code point.
	 * <p>
	 * <b>Relying on the default charset can often lead to bugs. Use
	 * {@link #setString(String, Charset)} instead.</b>
	 *
	 * @param content the {@link String} content to write to the referenced
	 *            8-bit char array.
	 */
	public void setString(String content) {
		setString(content, Charset.defaultCharset());
	}

	/**
	 * Encodes {@code content} to an array of 8-bit char using {@code charset},
	 * and writes this to the referenced char array. If the encoded
	 * {@code content} is bigger than the fixed array size, it will be truncated
	 * possibly in the middle of a multi-byte code point.
	 *
	 * @param content the {@link String} content to write to the referenced
	 *            8-bit char array.
	 * @param charset the {@link Charset} to use for encoding.
	 */
	public void setString(String content, Charset charset) {
		if (content == null) {
			setPointer(Pointer.NULL);
			return;
		}
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Array to big, please write it \"manually\" using getPointer.getX");
		}
		byte[] sourceBytes = content.getBytes(charset);
		byte[] bytes = new byte[(int) size];
		System.arraycopy(sourceBytes, 0, bytes, 0, Math.min((int) size, sourceBytes.length));
		if (sourceBytes.length < size) {
			bytes[sourceBytes.length] = 0;
		}
		for (int i = 0; i < bytes.length; i++) {
			getPointer().setByte(i, bytes[i]);
		}
	}

	/**
	 * Generates and returns a byte array with the content of the referenced
	 * char array. This is a copy of the referenced memory and any changes will
	 * not be reflected in the referenced memory.
	 *
	 * @return A byte array containing the values of the referenced char array.
	 */
	public byte[] getByteArray() {
		if (size > Integer.MAX_VALUE) {
			throw new UnsupportedOperationException("Array to big, please read it \"manually\" using getPointer.setX");
		}
		return getPointer() == null ? null : getPointer().getByteArray(0, (int) size);
	}

	/**
	 * Stores the values from {@code array} allocating memory as needed.
	 *
	 * @param array the array of bytes to write to the referenced memory. The
	 *            array size must be the same as defined for this
	 *            {@link FixedCharArrayByReference}.
	 */
	public void setByteArray(byte[] array) {
		if (array == null) {
			throw new NullPointerException("array cannot be null");
		}
		if (array.length != size) {
			throw new IllegalArgumentException("array size must be " + size);
		}

		if (size < 1) {
			setPointer(Pointer.NULL);
			return;
		} else if (getPointer() == null) {
			// Allocate new memory
			setPointer(new Memory(getElementSize() * size));
		}

		getPointer().write(0, array, 0, (int) size);
	}

	@Override
	public int getElementSize() {
		return 1;
	}

	@Override
	protected Byte[] getElements() {
		Byte[] result = new Byte[(int) size];
		for (int i = 0; i < size; i++) {
			result[i] = Byte.valueOf(getPointer().getByte(i * getElementSize()));
		}
		return result;
	}

	@Override
	protected void setElements(Byte[] array) {
		for (int i = 0; i < size; i++) {
			getPointer().setByte(i * getElementSize(), array[i].byteValue());
		}
	}

	@Override
	public String toString() {
		if (getPointer() == null) {
			return "null";
		}
		if (size > Integer.MAX_VALUE) {
			return super.toString();
		}
		return getString();
	}
}
