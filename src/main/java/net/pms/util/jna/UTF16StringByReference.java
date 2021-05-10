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

import com.sun.jna.FromNativeContext;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;


/**
 * An implementation of a referenced {@code null}-terminated UTF-16 string.
 */
public class UTF16StringByReference extends PointerType {

	/**
	 * Creates an unallocated {@link UTF16StringByReference}.
	 */
	public UTF16StringByReference() {
		super();
	}

	/**
	 * Creates a {@link UTF16StringByReference} and allocates space for {
	 * {@code dataSize} plus the size of the {@code null} terminator.
	 *
	 * @param dataSize the size to allocate in bytes excluding the {@code null}
	 *            terminator.
	 */
	public UTF16StringByReference(long dataSize) {
		super(dataSize < 1 ? Pointer.NULL : new Memory(dataSize + 2));
	}

	/**
	 * Creates a {@link UTF16StringByReference} containing {@code value} allocated
	 * to {@code value}'s byte length in {@code UTF-16}.
	 *
	 * @param value the string content.
	 */
	public UTF16StringByReference(String value) {
		super();
		if (value != null) {
			setValue(value);
		}
	}

	/**
	 * Sets this {@link UTF16StringByReference}'s content to that of
	 * {@code value}. If there's enough space in the already allocated memory,
	 * the content will be written there. If not a new area will be allocated
	 * and the {@link Pointer} updated.
	 *
	 * @param value the new string content.
	 */
	public void setValue(String value) {
		if (value == null) {
			setPointer(Pointer.NULL);
			return;
		}
		if (getNumberOfBytes(value) > getAllocatedSize()) {
			setPointer(new Memory(getNumberOfBytes(value) + 2));
		}
		int i;
		for (i = 0; i < value.length(); i++) {
			getPointer().setChar(i * 2, value.charAt(i));
		}
		getPointer().setMemory(i * 2L, 2L, (byte) 0);
	}

	/**
	 * Gets this {@link UTF16StringByReference}'s content.
	 *
	 * @return The content as a {@link String}.
	 */
	public String getValue() {
		if (getPointer() == null) {
			return null;
		}
		int length = 0;
		while (getPointer().getChar(length) != 0) {
			length += 2;
		}
		char[] value = new char[length / 2];
		for (int i = 0; i < length; i += 2) {
			value[i / 2] = getPointer().getChar(i);
		}
		return String.valueOf(value);
	}

	/**
	 * Gets the size in bytes allocated to this {@link UTF16StringByReference}
	 * excluding byte for the {@code null} terminator.
	 *
	 * @return The allocated size in bytes or {@code -1} if unknown.
	 */
	public long getAllocatedSize() {
		if (getPointer() instanceof Memory) {
			return Math.max(((Memory) getPointer()).size() - 2, 0);
		}
		return -1;
	}

	@Override
	public Object fromNative(Object nativeValue, FromNativeContext context) {
		// Always pass along null pointer values
		if (nativeValue == null) {
			return null;
		}
		setPointer((Pointer) nativeValue);
		return this;
	}

	@Override
	public String toString() {
		if (getPointer() == null) {
			return "null";
		}
		return getValue();
	}

	/**
	 * Calculates the length in bytes of {@code string} as {@code UTF-16}.
	 *
	 * @param string the string to evaluate.
	 * @return the byte-length of {@code string}.
	 */
	public static int getNumberOfBytes(String string) {
		if (string == null) {
			return 0;
		}
		final int length = string.length();
		int byteLength = 0;
		for (int offset = 0; offset < length;) {
			int codepoint = string.codePointAt(offset);
			byteLength += Character.charCount(codepoint) * 2;
			offset += Character.charCount(codepoint);
		}
		return byteLength;
	}
}
