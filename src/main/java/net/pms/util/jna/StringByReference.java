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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import com.sun.jna.FromNativeContext;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;


/**
 * An implementation of a referenced {@code null}-terminated C string.
 */
public class StringByReference extends PointerType {

	/**
	 * Creates an unallocated {@link StringByReference}.
	 */
	public StringByReference() {
		super();
	}

	/**
	 * Creates a {@link StringByReference} and allocates space for {
	 * {@code dataSize} plus the size of the {@code null} terminator.
	 *
	 * @param dataSize the size to allocate in bytes excluding the {@code null}
	 *            terminator.
	 */
	public StringByReference(long dataSize) {
		super(dataSize < 1 ? Pointer.NULL : new Memory(dataSize + 1));
	}

	/**
	 * Creates a {@link StringByReference} containing {@code value} allocated
	 * to {@code value}'s byte length encoded with
	 * {@link Native#getDefaultStringEncoding}. The new
	 * {@link StringByReference} will be encoded with
	 * {@link Native#getDefaultStringEncoding}.
	 *
	 * @param value the string content.
	 */
	public StringByReference(String value) {
		this(value, Charset.forName(Native.getDefaultStringEncoding()));
	}

	/**
	 * Creates a {@link StringByReference} containing {@code value} allocated
	 * to {@code value}'s byte length encoded with {@code charset}.
	 *
	 * @param value the string content.
	 * @param charset the {@link Charset} to use for encoding.
	 */
	public StringByReference(String value, Charset charset) {
		super();
		if (value != null) {
			setValue(value, charset);
		}
	}

	/**
	 * Creates a {@link StringByReference} containing {@code value} allocated
	 * to {@code value}'s byte length encoded with {@code charsetName}.
	 *
	 * @param value the string content.
	 * @param charsetName the name of the {@link Charset} to use for encoding.
	 */
	public StringByReference(String value, String charsetName) {
		super();
		if (value != null) {
			setValue(value, charsetName);
		}
	}

	/**
	 * Sets this {@link StringByReference}'s content to that of {@code value}
	 * using encoding {@link Native#getDefaultStringEncoding}. If there's enough
	 * space in the already allocated memory, the content will be written there.
	 * If not a new area will be allocated and the {@link Pointer} updated.
	 *
	 * @param value the new string content.
	 */
	public void setValue(String value) {
		setValue(value, Native.getDefaultStringEncoding());
	}

	/**
	 * Sets this {@link StringByReference}'s content to that of {@code value}.
	 * If there's enough space in the already allocated memory, the content will
	 * be written there. If not a new area will be allocated and the
	 * {@link Pointer} updated.
	 *
	 * @param value the new string content.
	 * @param charset a supported {@link Charset} to use for encoding.
	 */
	public void setValue(String value, Charset charset) {
		setValue(value, charset.name());
	}

	/**
	 * Sets this {@link StringByReference}'s content to that of {@code value}.
	 * If there's enough space in the already allocated memory, the content will
	 * be written there. If not a new area will be allocated and the
	 * {@link Pointer} updated.
	 *
	 * @param value the new string content.
	 * @param charsetName a valid and supported {@link Charset} name to use for
	 *            encoding.
	 */
	public void setValue(String value, String charsetName) {
		if (value == null) {
			setPointer(Pointer.NULL);
			return;
		}
		try {
			int length = value.getBytes(charsetName).length;
			if (length > getAllocatedSize()) {
				setPointer(new Memory(length + 1));
			}
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException("Unsupported encoding: " + charsetName, e);
		}
		getPointer().setString(0, value, charsetName);
	}

	/**
	 * Gets this {@link StringByReference}'s content using
	 * {@link Native#getDefaultStringEncoding} for decoding.
	 *
	 * @return The content as a {@link String}.
	 */
	public String getValue() {
		return getValue(Native.getDefaultStringEncoding());
	}

	/**
	 * Gets this {@link StringByReference}'s content using
	 * {@code charset} for decoding.
	 *
	 * @param charset a supported {@link Charset} to use for decoding.
	 * @return The content as a {@link String}.
	 */
	public String getValue(Charset charset) {
		return getValue(charset.name());
	}

	/**
	 * Gets this {@link StringByReference}'s content using {@code charsetName}
	 * for decoding.
	 *
	 * @param charsetName a valid and supported {@link Charset} name to use for
	 *            decoding.
	 * @return The content as a {@link String}.
	 */
	public String getValue(String charsetName) {
		return getPointer() == null ? null : getPointer().getString(0, charsetName);
	}

	/**
	 * Gets the size in bytes allocated to this {@link StringByReference}
	 * excluding byte for the {@code null} terminator.
	 *
	 * @return The allocated size in bytes or {@code -1} if unknown.
	 */
	public long getAllocatedSize() {
		if (getPointer() instanceof Memory) {
			return Math.max(((Memory) getPointer()).size() - 1, 0);
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
}
